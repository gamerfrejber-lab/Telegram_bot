package com.company.db;

import com.company.config.Config;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Umumiy Postgres bazasiga ulanish. Bu bot mijozlar boti va veb-sayt bilan
 * ayni bir bazadan foydalanadi — shuning uchun dorixona bu yerda mahsulot qo'shsa,
 * u darhol mijozlar botida va saytda ko'rinadi, mijoz bron qilsa esa buyurtma
 * shu yerdagi dorixona egasiga tushadi.
 */
public final class Database {

    private static volatile HikariDataSource pool;

    private Database() { }

    /**
     * Puldan ulanish beradi. Avval har bir so'rov uchun yangi ulanish ochilardi — bitta xabarga
     * javob berishda bazaga 4-6 marta qayta ulanish kerak bo'lib, masofadagi Postgres bilan
     * bot sezilarli sekin ishlardi. close() ulanishni yopmaydi, pulga qaytaradi.
     */
    public static Connection getConnection() throws SQLException {
        HikariDataSource ds = pool;
        if (ds == null) {
            synchronized (Database.class) {
                ds = pool;
                if (ds == null) {
                    HikariConfig config = new HikariConfig();
                    config.setJdbcUrl(Config.require("DB_URL"));
                    String user = Config.get("DB_USER", null);
                    String password = Config.get("DB_PASSWORD", null);
                    if (user != null) config.setUsername(user);
                    if (password != null) config.setPassword(password);
                    // Bepul Postgres tariflarida ulanishlar soni cheklangan, bot uchun bir nechtasi yetadi.
                    config.setMaximumPoolSize(Integer.parseInt(Config.get("DB_POOL_SIZE", "4")));
                    config.setMinimumIdle(1);
                    config.setPoolName("dorixona-bot");
                    ds = new HikariDataSource(config);
                    pool = ds;
                }
            }
        }
        return ds.getConnection();
    }

    /**
     * Shu bot uchun kerakli jadval va ustunlarni yaratadi. Mavjud jadvallarga tegmaydi —
     * faqat yetishmayotganini qo'shadi, shuning uchun mijozlar botining ma'lumotlari buzilmaydi.
     */
    public static void ensureSchema() {
        String ddl = """
                -- Asosiy jadvallar odatda mijozlar boti tomonidan yaratilgan bo'ladi. Bu bot
                -- birinchi ishga tushsa ham to'xtab qolmasligi uchun ular ham tekshiriladi
                -- (mavjud bo'lsa hech narsa o'zgarmaydi).
                CREATE TABLE IF NOT EXISTS dorixona (
                    id BIGSERIAL PRIMARY KEY,
                    nomi VARCHAR(255) NOT NULL,
                    viloyat VARCHAR(255),
                    tuman VARCHAR(255),
                    manzil VARCHAR(500),
                    latitude DOUBLE PRECISION,
                    longitude DOUBLE PRECISION,
                    telefon VARCHAR(50),
                    shartnoma_boshlanish DATE,
                    shartnoma_tugash DATE,
                    holat VARCHAR(20) DEFAULT 'faol'
                );

                CREATE TABLE IF NOT EXISTS dori (
                    id BIGSERIAL PRIMARY KEY,
                    nomi VARCHAR(255) NOT NULL,
                    nomi_ru VARCHAR(255),
                    ishlab_chiqaruvchi VARCHAR(255),
                    narx DOUBLE PRECISION NOT NULL,
                    mavjud BOOLEAN DEFAULT TRUE,
                    dorixona_id BIGINT REFERENCES dorixona(id) ON DELETE CASCADE
                );

                CREATE TABLE IF NOT EXISTS ombor_harakat (
                    id BIGSERIAL PRIMARY KEY,
                    dori_id BIGINT NOT NULL REFERENCES dori(id) ON DELETE CASCADE,
                    turi VARCHAR(10) NOT NULL,
                    soni INTEGER NOT NULL,
                    narx DOUBLE PRECISION,
                    izoh VARCHAR(500),
                    sana TIMESTAMP DEFAULT NOW()
                );

                CREATE INDEX IF NOT EXISTS ombor_harakat_dori_idx ON ombor_harakat (dori_id);

                -- Dorixonaning egasi: tasdiqlangandan keyin shu Telegram hisobiga biriktiriladi.
                ALTER TABLE dorixona ADD COLUMN IF NOT EXISTS egasi_telegram_id BIGINT;

                -- Kategoriya belgilarini sayt yozadi (masalan ,antibiotiklar,retseptli,) va bot ularni faqat oqiydi.
                ALTER TABLE dori ADD COLUMN IF NOT EXISTS kategoriya VARCHAR(120);

                -- Shu botdan foydalanuvchilar (dorixona egalari). Til tanlovi bot qayta
                -- ishga tushganda ham saqlanib qolishi uchun kerak.
                CREATE TABLE IF NOT EXISTS dorixona_foydalanuvchi (
                    telegram_id BIGINT PRIMARY KEY,
                    ism VARCHAR(255),
                    username VARCHAR(255),
                    telefon VARCHAR(50),
                    til VARCHAR(5) DEFAULT 'uz',
                    sana TIMESTAMP DEFAULT NOW()
                );

                -- Egalik arizasi: dorixona egasi o'z dorixonasini "meniki" deb da'vo qiladi va
                -- dalil yuboradi. Admin tasdiqlagandan keyingina panel ochiladi.
                CREATE TABLE IF NOT EXISTS dorixona_soov (
                    id BIGSERIAL PRIMARY KEY,
                    dorixona_id BIGINT NOT NULL REFERENCES dorixona(id) ON DELETE CASCADE,
                    telegram_id BIGINT NOT NULL,
                    ism VARCHAR(255),
                    username VARCHAR(255),
                    telefon VARCHAR(50),
                    litsenziya_file_id VARCHAR(255),
                    jonli_rasm_file_id VARCHAR(255),
                    tekshiruv_kodi VARCHAR(10),
                    holat VARCHAR(20) DEFAULT 'KUTILMOQDA',
                    admin_izoh VARCHAR(500),
                    sana TIMESTAMP DEFAULT NOW()
                );

                CREATE INDEX IF NOT EXISTS dorixona_soov_holat_idx ON dorixona_soov (holat);

                -- Dalil suratlari bazada baytlar sifatida saqlanadi. Telegram bergan file_id
                -- faqat uni yuklagan botda ishlaydi, shuning uchun mijozlar boti o'sha id
                -- bilan rasmni ko'rsata olmasdi — endi ikkala bot ham ko'rsata oladi.
                ALTER TABLE dorixona_soov ADD COLUMN IF NOT EXISTS litsenziya_rasm BYTEA;
                ALTER TABLE dorixona_soov ADD COLUMN IF NOT EXISTS jonli_rasm BYTEA;

                -- Xabar navbati: ariza kelganda ikkala botdagi adminga, qaror chiqqanda
                -- esa dorixona egasiga xabar berilishi kerak. Botlar bir-biriga
                -- to'g'ridan-to'g'ri ulanmagani uchun shu bayroqlar orqali gaplashadi.
                ALTER TABLE dorixona_soov ADD COLUMN IF NOT EXISTS adminga_xabar BOOLEAN DEFAULT TRUE;
                ALTER TABLE dorixona_soov ADD COLUMN IF NOT EXISTS egaga_xabar BOOLEAN DEFAULT TRUE;

                -- Bron: mijoz dorini oldindan band qiladi, dorixona egasiga xabar boradi.
                CREATE TABLE IF NOT EXISTS bron (
                    id BIGSERIAL PRIMARY KEY,
                    dori_id BIGINT NOT NULL REFERENCES dori(id) ON DELETE CASCADE,
                    dorixona_id BIGINT NOT NULL REFERENCES dorixona(id) ON DELETE CASCADE,
                    mijoz_telegram_id BIGINT,
                    mijoz_ismi VARCHAR(255),
                    mijoz_telefon VARCHAR(50),
                    soni INTEGER NOT NULL DEFAULT 1,
                    kod VARCHAR(10),
                    holat VARCHAR(20) DEFAULT 'YANGI',
                    egaga_xabar BOOLEAN DEFAULT FALSE,
                    mijozga_xabar BOOLEAN DEFAULT TRUE,
                    sana TIMESTAMP DEFAULT NOW()
                );

                CREATE INDEX IF NOT EXISTS bron_dorixona_idx ON bron (dorixona_id);
                CREATE INDEX IF NOT EXISTS bron_egaga_xabar_idx ON bron (egaga_xabar);
                CREATE INDEX IF NOT EXISTS bron_mijozga_xabar_idx ON bron (mijozga_xabar);

                -- Yetkazib berish: mijoz saytda buyurtma beradi, dorini dorixona o'zi yetkazadi.
                -- Tuzilma saytdagi YetkazishService bilan bir xil (qaysi biri birinchi ishga tushsa, o'sha yaratadi).
                CREATE TABLE IF NOT EXISTS yetkazish (
                    id BIGSERIAL PRIMARY KEY,
                    dorixona_id BIGINT NOT NULL,
                    mijoz_telegram_id BIGINT,
                    mijoz_ismi VARCHAR(200),
                    mijoz_telefon VARCHAR(40),
                    manzil VARCHAR(500) NOT NULL,
                    izoh VARCHAR(500),
                    latitude DOUBLE PRECISION,
                    longitude DOUBLE PRECISION,
                    masofa_km DOUBLE PRECISION,
                    dori_summa DOUBLE PRECISION NOT NULL DEFAULT 0,
                    yetkazish_narxi DOUBLE PRECISION NOT NULL DEFAULT 0,
                    tolov_usuli VARCHAR(20) NOT NULL DEFAULT 'NAQD',
                    tolov_holati VARCHAR(20),
                    tolov_cheki BYTEA,
                    holat VARCHAR(20) NOT NULL DEFAULT 'YANGI',
                    bekor_sababi VARCHAR(300),
                    kuryer_telefon VARCHAR(40),
                    taxminiy_daqiqa INTEGER,
                    kod VARCHAR(10),
                    egaga_xabar BOOLEAN NOT NULL DEFAULT FALSE,
                    egaga_hodisa VARCHAR(20),
                    mijozga_xabar BOOLEAN NOT NULL DEFAULT TRUE,
                    mijoz_hodisa VARCHAR(20),
                    chiqim_yozildi BOOLEAN NOT NULL DEFAULT FALSE,
                    sana TIMESTAMP NOT NULL DEFAULT NOW(),
                    yangilandi TIMESTAMP
                );

                CREATE TABLE IF NOT EXISTS yetkazish_qator (
                    id BIGSERIAL PRIMARY KEY,
                    yetkazish_id BIGINT NOT NULL REFERENCES yetkazish(id) ON DELETE CASCADE,
                    dori_id BIGINT,
                    nomi VARCHAR(500) NOT NULL,
                    soni INTEGER NOT NULL,
                    tur VARCHAR(10) NOT NULL DEFAULT 'dona',
                    narx DOUBLE PRECISION NOT NULL DEFAULT 0
                );

                CREATE INDEX IF NOT EXISTS yetkazish_qator_idx ON yetkazish_qator (yetkazish_id);
                """;
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            for (String statement : ddl.split(";")) {
                if (!statement.isBlank()) stmt.execute(statement);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Bazani sozlab bo'lmadi (jadval yaratish xatosi)", e);
        }
    }
}
