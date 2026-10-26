package com.company.repository;

import com.company.db.Database;
import com.company.model.Yetkazish;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Yetkazish buyurtmalari. Bron kabi umumiy "pochta qutisi": sayt buyurtmani yozadi
 * (egaga_xabar = FALSE, egaga_hodisa = YANGI | CHEK | BEKOR_MIJOZ), bu bot egasiga yetkazadi;
 * dorixona javobi mijozlar botiga mijozga_xabar = FALSE + mijoz_hodisa orqali boradi.
 */
public class YetkazishRepository {

    private static final String SELECT =
            "SELECT y.id, y.dorixona_id, y.mijoz_telegram_id, y.mijoz_ismi, y.mijoz_telefon, y.manzil, y.izoh, "
            + "y.latitude, y.longitude, y.masofa_km, y.dori_summa, y.yetkazish_narxi, y.tolov_usuli, y.tolov_holati, "
            + "y.holat, y.bekor_sababi, y.kuryer_telefon, y.taxminiy_daqiqa, y.kod, y.egaga_hodisa, y.sana, "
            + "p.nomi AS dorixona_nomi FROM yetkazish y JOIN dorixona p ON p.id = y.dorixona_id";

    /** Egasiga hali aytilmagan hodisalar (egasi ulangan dorixonalarniki). */
    public List<Yetkazish> yetkazilmaganlar() {
        return royxat(SELECT + " WHERE y.egaga_xabar = FALSE AND p.egasi_telegram_id IS NOT NULL ORDER BY y.id LIMIT 20");
    }

    /** "Egasiga aytildi" — faqat hali belgilanmagani; bir hodisa ikki marta yuborilmaydi. */
    public boolean egagaAytildi(long id) {
        return update("UPDATE yetkazish SET egaga_xabar = TRUE WHERE id = ? AND egaga_xabar = FALSE", id) > 0;
    }

    public Yetkazish getById(long id) {
        List<Yetkazish> r = royxat(SELECT + " WHERE y.id = ?", id);
        return r.isEmpty() ? null : r.get(0);
    }

    /** Dorixonaning faol buyurtmalari (yangi, qabul qilingan, yo'lda). */
    public List<Yetkazish> faollari(long dorixonaId) {
        return royxat(SELECT + " WHERE y.dorixona_id = ? AND y.holat IN ('YANGI', 'QABUL', 'YOLDA') ORDER BY y.id DESC LIMIT 20",
                dorixonaId);
    }

    /**
     * Holatni o'zgartiradi (faqat ruxsat etilgan o'tishlar, faqat shu dorixonaniki). Mijozga
     * mijozlar boti orqali xabar boradi. Eski xabardagi tugma qayta bosilsa false qaytadi.
     */
    public boolean holatniOzgartir(long id, long dorixonaId, String yangi, Integer daqiqa, String sabab) {
        return holatniOzgartir(id, dorixonaId, yangi, daqiqa, sabab, null);
    }

    /** kuryerTelefon — "Yo'lga chiqdi"da mijozga yuboriladi (saytdagi kabi; null bo'lsa o'zgarmaydi). */
    public boolean holatniOzgartir(long id, long dorixonaId, String yangi, Integer daqiqa, String sabab, String kuryerTelefon) {
        String oldingi = switch (yangi) {
            case Yetkazish.QABUL -> "'YANGI'";
            case Yetkazish.YOLDA -> "'QABUL'";
            case Yetkazish.YETKAZILDI -> "'QABUL', 'YOLDA'";
            case Yetkazish.BEKOR -> "'YANGI', 'QABUL', 'YOLDA'";
            default -> null;
        };
        if (oldingi == null) return false;
        String sql = "UPDATE yetkazish SET holat = ?, mijozga_xabar = FALSE, mijoz_hodisa = ?, yangilandi = NOW(), "
                + "taxminiy_daqiqa = COALESCE(?, taxminiy_daqiqa), bekor_sababi = COALESCE(?, bekor_sababi), "
                + "kuryer_telefon = COALESCE(?, kuryer_telefon) "
                + "WHERE id = ? AND dorixona_id = ? AND holat IN (" + oldingi + ")";
        try (Connection conn = Database.getConnection(); PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, yangi);
            st.setString(2, yangi);
            if (daqiqa == null) st.setNull(3, java.sql.Types.INTEGER); else st.setInt(3, daqiqa);
            st.setString(4, Yetkazish.BEKOR.equals(yangi) ? (sabab == null ? "Dorixona buyurtmani bajara olmadi" : sabab) : null);
            st.setString(5, kuryerTelefon == null || kuryerTelefon.isBlank() ? null : kuryerTelefon.trim());
            st.setLong(6, id);
            st.setLong(7, dorixonaId);
            return st.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Yetkazish holatini o'zgartirib bo'lmadi", e);
        }
    }

    /** Kartaga to'lov tekshirildi — mijozga "to'lov tasdiqlandi" boradi. */
    public boolean tolovTasdiqla(long id, long dorixonaId) {
        return update("UPDATE yetkazish SET tolov_holati = 'TASDIQLANDI', mijozga_xabar = FALSE, mijoz_hodisa = 'TOLOV', "
                + "yangilandi = NOW() WHERE id = ? AND dorixona_id = ? AND tolov_usuli = 'KARTA' "
                + "AND tolov_holati = 'CHEK_YUBORILDI'", id, dorixonaId) > 0;
    }

    /** Ombor chiqimi bir marta yozilishi uchun: true — hali yozilmagan edi, endi belgilandi. */
    public boolean chiqimniBelgila(long id) {
        return update("UPDATE yetkazish SET chiqim_yozildi = TRUE WHERE id = ? AND chiqim_yozildi = FALSE", id) > 0;
    }

    public byte[] chek(long id) {
        try (Connection conn = Database.getConnection();
             PreparedStatement st = conn.prepareStatement("SELECT tolov_cheki FROM yetkazish WHERE id = ?")) {
            st.setLong(1, id);
            try (ResultSet rs = st.executeQuery()) {
                return rs.next() ? rs.getBytes(1) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Chekni o'qib bo'lmadi", e);
        }
    }

    // ——————————————————————————————————————————

    private int update(String sql, long... args) {
        try (Connection conn = Database.getConnection(); PreparedStatement st = conn.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) st.setLong(i + 1, args[i]);
            return st.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Yetkazishni yangilab bo'lmadi", e);
        }
    }

    private List<Yetkazish> royxat(String sql, long... args) {
        try (Connection conn = Database.getConnection(); PreparedStatement st = conn.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) st.setLong(i + 1, args[i]);
            List<Yetkazish> list = new ArrayList<>();
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) list.add(oqi(rs));
            }
            for (Yetkazish y : list) qatorlar(conn, y);
            return list;
        } catch (SQLException e) {
            throw new IllegalStateException("Yetkazish buyurtmalarini o'qib bo'lmadi", e);
        }
    }

    private void qatorlar(Connection conn, Yetkazish y) throws SQLException {
        try (PreparedStatement st = conn.prepareStatement(
                "SELECT dori_id, nomi, soni, tur, narx FROM yetkazish_qator WHERE yetkazish_id = ? ORDER BY id")) {
            st.setLong(1, y.getId());
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    long doriId = rs.getLong("dori_id");
                    Long dori = rs.wasNull() ? null : doriId;
                    y.getQatorlar().add(new Yetkazish.Qator(dori, rs.getString("nomi"),
                            rs.getInt("soni"), rs.getString("tur"), rs.getDouble("narx")));
                }
            }
        }
    }

    private Yetkazish oqi(ResultSet rs) throws SQLException {
        Yetkazish y = new Yetkazish();
        y.setId(rs.getLong("id"));
        y.setDorixonaId(rs.getLong("dorixona_id"));
        long mijoz = rs.getLong("mijoz_telegram_id");
        if (!rs.wasNull()) y.setMijozTelegramId(mijoz);
        y.setMijozIsmi(rs.getString("mijoz_ismi"));
        y.setMijozTelefon(rs.getString("mijoz_telefon"));
        y.setManzil(rs.getString("manzil"));
        y.setIzoh(rs.getString("izoh"));
        double lat = rs.getDouble("latitude");
        if (!rs.wasNull()) y.setLatitude(lat);
        double lon = rs.getDouble("longitude");
        if (!rs.wasNull()) y.setLongitude(lon);
        double km = rs.getDouble("masofa_km");
        if (!rs.wasNull()) y.setMasofaKm(km);
        y.setDoriSumma(rs.getDouble("dori_summa"));
        y.setYetkazishNarxi(rs.getDouble("yetkazish_narxi"));
        y.setTolovUsuli(rs.getString("tolov_usuli"));
        y.setTolovHolati(rs.getString("tolov_holati"));
        y.setHolat(rs.getString("holat"));
        y.setBekorSababi(rs.getString("bekor_sababi"));
        y.setKuryerTelefon(rs.getString("kuryer_telefon"));
        int daq = rs.getInt("taxminiy_daqiqa");
        if (!rs.wasNull()) y.setTaxminiyDaqiqa(daq);
        y.setKod(rs.getString("kod"));
        y.setEgagaHodisa(rs.getString("egaga_hodisa"));
        Timestamp sana = rs.getTimestamp("sana");
        if (sana != null) y.setSana(sana.toLocalDateTime());
        y.setDorixonaNomi(rs.getString("dorixona_nomi"));
        return y;
    }
}
