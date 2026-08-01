package com.company.repository;

import com.company.db.Database;
import com.company.model.Soov;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/** Egalik arizalari: yuborish, ko'rish, tasdiqlash va rad etish. */
public class SoovRepository {

    private static final String SELECT =
            "SELECT s.id, s.dorixona_id, s.telegram_id, s.ism, s.username, s.telefon, "
            + "s.litsenziya_file_id, s.jonli_rasm_file_id, s.litsenziya_rasm, s.jonli_rasm, "
            + "s.tekshiruv_kodi, s.holat, s.admin_izoh, s.sana, "
            + "p.nomi AS dorixona_nomi "
            + "FROM dorixona_soov s JOIN dorixona p ON p.id = s.dorixona_id";

    public long add(Soov soov) {
        // adminga_xabar = FALSE: ariza yangi, ikkala botdagi adminga hali aytilmagan.
        String sql = "INSERT INTO dorixona_soov (dorixona_id, telegram_id, ism, username, telefon, "
                + "litsenziya_file_id, jonli_rasm_file_id, litsenziya_rasm, jonli_rasm, "
                + "tekshiruv_kodi, holat, adminga_xabar, egaga_xabar, sana) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, FALSE, TRUE, NOW())";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, soov.getDorixonaId());
            stmt.setLong(2, soov.getTelegramId());
            stmt.setString(3, soov.getIsm());
            stmt.setString(4, soov.getUsername());
            stmt.setString(5, soov.getTelefon());
            stmt.setString(6, soov.getLitsenziyaFileId());
            stmt.setString(7, soov.getJonliRasmFileId());
            stmt.setBytes(8, soov.getLitsenziyaRasm());
            stmt.setBytes(9, soov.getJonliRasm());
            stmt.setString(10, soov.getTekshiruvKodi());
            stmt.setString(11, Soov.KUTILMOQDA);
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
            return 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Arizani yozib bo'lmadi", e);
        }
    }

    public Soov getById(long id) {
        return bitta(SELECT + " WHERE s.id = ?", id);
    }

    /** Shu foydalanuvchining ko'rib chiqilmagan arizasi (bo'lmasa null). */
    public Soov kutilayotgani(long telegramId) {
        return bitta(SELECT + " WHERE s.telegram_id = ? AND s.holat = '" + Soov.KUTILMOQDA + "' "
                + "ORDER BY s.id DESC LIMIT 1", telegramId);
    }

    public List<Soov> kutilayotganlar() {
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     SELECT + " WHERE s.holat = '" + Soov.KUTILMOQDA + "' ORDER BY s.id")) {
            return read(stmt);
        } catch (SQLException e) {
            throw new IllegalStateException("Arizalarni o'qib bo'lmadi", e);
        }
    }

    /**
     * Arizani hal qiladi. Faqat hali ko'rib chiqilmagan ariza o'zgaradi — shuning uchun
     * ikki admin bir vaqtda bossa ham ikki marta bajarilib ketmaydi (false qaytadi).
     */
    public boolean hal(long soovId, String holat, String izoh) {
        // egaga_xabar = FALSE: qaror chiqdi, endi dorixona egasiga aytilishi kerak.
        // adminga_xabar = TRUE: ariza hal bo'ldi, boshqa adminni bezovta qilmaymiz.
        String sql = "UPDATE dorixona_soov SET holat = ?, admin_izoh = ?, "
                + "egaga_xabar = FALSE, adminga_xabar = TRUE "
                + "WHERE id = ? AND holat = '" + Soov.KUTILMOQDA + "'";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, holat);
            stmt.setString(2, izoh);
            stmt.setLong(3, soovId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Arizani hal qilib bo'lmadi", e);
        }
    }

    /**
     * Qarori chiqqan, lekin dorixona egasiga hali aytilmagan arizalar.
     * Ariza qaysi botda hal qilinganidan qat'i nazar, egasi shu botda bo'lgani uchun
     * javobni doim shu bot yetkazadi.
     */
    public List<Soov> egagaAytilmaganlar() {
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     SELECT + " WHERE s.egaga_xabar = FALSE AND s.holat <> '" + Soov.KUTILMOQDA + "' "
                     + "ORDER BY s.id LIMIT 20")) {
            return read(stmt);
        } catch (SQLException e) {
            throw new IllegalStateException("Arizalarni o'qib bo'lmadi", e);
        }
    }

    /** "Egasiga aytildi" deb belgilaydi — bir xabar ikki marta ketmasligi uchun. */
    public boolean egagaAytildi(long soovId) {
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE dorixona_soov SET egaga_xabar = TRUE WHERE id = ? AND egaga_xabar = FALSE")) {
            stmt.setLong(1, soovId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Arizani belgilab bo'lmadi", e);
        }
    }

    private Soov bitta(String sql, long param) {
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, param);
            List<Soov> found = read(stmt);
            return found.isEmpty() ? null : found.get(0);
        } catch (SQLException e) {
            throw new IllegalStateException("Arizani o'qib bo'lmadi", e);
        }
    }

    private List<Soov> read(PreparedStatement stmt) throws SQLException {
        try (ResultSet rs = stmt.executeQuery()) {
            List<Soov> list = new ArrayList<>();
            while (rs.next()) {
                Soov s = new Soov();
                s.setId(rs.getLong("id"));
                s.setDorixonaId(rs.getLong("dorixona_id"));
                s.setTelegramId(rs.getLong("telegram_id"));
                s.setIsm(rs.getString("ism"));
                s.setUsername(rs.getString("username"));
                s.setTelefon(rs.getString("telefon"));
                s.setLitsenziyaFileId(rs.getString("litsenziya_file_id"));
                s.setJonliRasmFileId(rs.getString("jonli_rasm_file_id"));
                s.setLitsenziyaRasm(rs.getBytes("litsenziya_rasm"));
                s.setJonliRasm(rs.getBytes("jonli_rasm"));
                s.setTekshiruvKodi(rs.getString("tekshiruv_kodi"));
                s.setHolat(rs.getString("holat"));
                s.setAdminIzoh(rs.getString("admin_izoh"));
                Timestamp sana = rs.getTimestamp("sana");
                if (sana != null) s.setSana(sana.toLocalDateTime());
                s.setDorixonaNomi(rs.getString("dorixona_nomi"));
                list.add(s);
            }
            return list;
        }
    }
}
