package com.company.repository;

import com.company.db.Database;
import com.company.model.Dorixona;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PharmacyRepository {

    private static final String SELECT =
            "SELECT id, nomi, viloyat, tuman, manzil, telefon, shartnoma_boshlanish, shartnoma_tugash, "
            + "holat, egasi_telegram_id FROM dorixona";

    public List<Dorixona> getAll() {
        return query(SELECT + " ORDER BY nomi");
    }

    /** Hali hech kimga biriktirilmagan dorixonalar — egasi shulardan o'zinikini tanlaydi. */
    public List<Dorixona> egasizlar() {
        return query(SELECT + " WHERE egasi_telegram_id IS NULL ORDER BY nomi");
    }

    /** Nomi bo'yicha qidiradi (qism-matn, katta-kichik harf muhim emas). */
    public List<Dorixona> qidir(String nom) {
        if (nom == null || nom.isBlank()) return new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT + " WHERE nomi ILIKE ? ORDER BY nomi LIMIT 30")) {
            stmt.setString(1, "%" + nom.trim() + "%");
            return read(stmt);
        } catch (SQLException e) {
            throw new IllegalStateException("Dorixonalarni qidirib bo'lmadi", e);
        }
    }

    public Dorixona getById(long id) {
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT + " WHERE id = ?")) {
            stmt.setLong(1, id);
            List<Dorixona> found = read(stmt);
            return found.isEmpty() ? null : found.get(0);
        } catch (SQLException e) {
            throw new IllegalStateException("Dorixonani o'qib bo'lmadi", e);
        }
    }

    /** Shu Telegram hisobiga biriktirilgan dorixona (bo'lmasa null). */
    public Dorixona egasiBoyicha(long telegramId) {
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT + " WHERE egasi_telegram_id = ? LIMIT 1")) {
            stmt.setLong(1, telegramId);
            List<Dorixona> found = read(stmt);
            return found.isEmpty() ? null : found.get(0);
        } catch (SQLException e) {
            throw new IllegalStateException("Dorixonani o'qib bo'lmadi", e);
        }
    }

    /** Yangi dorixona qo'shadi va yaratilgan id'ni qaytaradi. */
    public long add(Dorixona dorixona) {
        String sql = "INSERT INTO dorixona (nomi, viloyat, tuman, manzil, telefon, "
                + "shartnoma_boshlanish, shartnoma_tugash, holat) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, dorixona.getNomi());
            stmt.setString(2, dorixona.getViloyat());
            stmt.setString(3, dorixona.getTuman());
            stmt.setString(4, dorixona.getManzil());
            stmt.setString(5, dorixona.getTelefon());
            setDate(stmt, 6, dorixona.getObunaBoshlanish());
            setDate(stmt, 7, dorixona.getObunaTugash());
            stmt.setString(8, dorixona.getHolat() == null ? "faol" : dorixona.getHolat());
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
            return 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Dorixona qo'shib bo'lmadi", e);
        }
    }

    /** Dorixonani egasiga biriktiradi. Boshqa birov band qilib ulgurgan bo'lsa false qaytadi. */
    public boolean egasiniBiriktir(long dorixonaId, long telegramId) {
        String sql = "UPDATE dorixona SET egasi_telegram_id = ? WHERE id = ? AND egasi_telegram_id IS NULL";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, telegramId);
            stmt.setLong(2, dorixonaId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Egani biriktirib bo'lmadi", e);
        }
    }

    /** Obunani berilgan oy soniga uzaytiradi (tugagan bo'lsa bugundan boshlab). */
    public LocalDate obunaniUzaytir(long dorixonaId, int oylar) {
        Dorixona dorixona = getById(dorixonaId);
        if (dorixona == null) return null;
        LocalDate boshlanish = dorixona.getObunaTugash() == null || dorixona.getObunaTugash().isBefore(LocalDate.now())
                ? LocalDate.now()
                : dorixona.getObunaTugash();
        LocalDate yangiTugash = boshlanish.plusMonths(oylar);

        String sql = "UPDATE dorixona SET shartnoma_tugash = ?, holat = 'faol', "
                + "shartnoma_boshlanish = COALESCE(shartnoma_boshlanish, ?) WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(yangiTugash));
            stmt.setDate(2, Date.valueOf(LocalDate.now()));
            stmt.setLong(3, dorixonaId);
            stmt.executeUpdate();
            return yangiTugash;
        } catch (SQLException e) {
            throw new IllegalStateException("Obunani uzaytirib bo'lmadi", e);
        }
    }

    public boolean ochir(long id) {
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM dorixona WHERE id = ?")) {
            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Dorixonani o'chirib bo'lmadi", e);
        }
    }

    public int soni() {
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM dorixona");
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Dorixonalar sonini o'qib bo'lmadi", e);
        }
    }

    private List<Dorixona> query(String sql) {
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            return read(stmt);
        } catch (SQLException e) {
            throw new IllegalStateException("Dorixonalarni o'qib bo'lmadi", e);
        }
    }

    private List<Dorixona> read(PreparedStatement stmt) throws SQLException {
        try (ResultSet rs = stmt.executeQuery()) {
            List<Dorixona> list = new ArrayList<>();
            while (rs.next()) list.add(map(rs));
            return list;
        }
    }

    private Dorixona map(ResultSet rs) throws SQLException {
        Dorixona d = new Dorixona();
        d.setId(rs.getLong("id"));
        d.setNomi(rs.getString("nomi"));
        d.setViloyat(rs.getString("viloyat"));
        d.setTuman(rs.getString("tuman"));
        d.setManzil(rs.getString("manzil"));
        d.setTelefon(rs.getString("telefon"));
        Date boshlanish = rs.getDate("shartnoma_boshlanish");
        Date tugash = rs.getDate("shartnoma_tugash");
        if (boshlanish != null) d.setObunaBoshlanish(boshlanish.toLocalDate());
        if (tugash != null) d.setObunaTugash(tugash.toLocalDate());
        d.setHolat(rs.getString("holat"));
        long egasi = rs.getLong("egasi_telegram_id");
        if (!rs.wasNull()) d.setEgasiTelegramId(egasi);
        return d;
    }

    private void setDate(PreparedStatement stmt, int index, LocalDate value) throws SQLException {
        if (value == null) stmt.setNull(index, java.sql.Types.DATE);
        else stmt.setDate(index, Date.valueOf(value));
    }
}
