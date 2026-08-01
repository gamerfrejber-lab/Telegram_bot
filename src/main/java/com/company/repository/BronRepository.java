package com.company.repository;

import com.company.db.Database;
import com.company.model.Bron;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Bronlar. Bu jadval ikki bot uchun umumiy pochta qutisi vazifasini bajaradi:
 * mijozlar boti bron yozadi, dorixonalar boti uni o'qib egasiga yetkazadi va
 * javobni yana shu jadvalga yozadi. Shuning uchun botlar bir-biriga to'g'ridan-to'g'ri
 * ulanmaydi — biri o'chib qolsa ham xabar yo'qolmaydi, keyin yetkaziladi.
 */
public class BronRepository {

    private static final String SELECT =
            "SELECT b.id, b.dori_id, b.dorixona_id, b.mijoz_telegram_id, b.mijoz_ismi, b.mijoz_telefon, "
            + "b.soni, b.kod, b.holat, b.sana, d.nomi AS dori_nomi, d.narx AS dori_narx, "
            + "p.nomi AS dorixona_nomi "
            + "FROM bron b JOIN dori d ON d.id = b.dori_id JOIN dorixona p ON p.id = b.dorixona_id";

    /** Dorixona egasiga hali yetkazilmagan yangi bronlar. */
    public List<Bron> yetkazilmaganlar() {
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     SELECT + " WHERE b.egaga_xabar = FALSE ORDER BY b.id LIMIT 20")) {
            return read(stmt);
        } catch (SQLException e) {
            throw new IllegalStateException("Bronlarni o'qib bo'lmadi", e);
        }
    }

    /**
     * Bronni "egasiga yetkazildi" deb belgilaydi. Faqat hali belgilanmagani o'zgaradi,
     * shuning uchun bir xabar ikki marta yuborilib ketmaydi.
     */
    public boolean egagaYetkazildi(long bronId) {
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE bron SET egaga_xabar = TRUE WHERE id = ? AND egaga_xabar = FALSE")) {
            stmt.setLong(1, bronId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Bronni belgilab bo'lmadi", e);
        }
    }

    public Bron getById(long id) {
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT + " WHERE b.id = ?")) {
            stmt.setLong(1, id);
            List<Bron> found = read(stmt);
            return found.isEmpty() ? null : found.get(0);
        } catch (SQLException e) {
            throw new IllegalStateException("Bronni o'qib bo'lmadi", e);
        }
    }

    /** Dorixonaning bronlari — eng yangisidan boshlab. */
    public List<Bron> dorixonaniki(long dorixonaId, boolean faqatFaol) {
        String sql = SELECT + " WHERE b.dorixona_id = ?"
                + (faqatFaol ? " AND b.holat IN ('" + Bron.YANGI + "', '" + Bron.TAYYOR + "')" : "")
                + " ORDER BY b.id DESC LIMIT 30";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, dorixonaId);
            return read(stmt);
        } catch (SQLException e) {
            throw new IllegalStateException("Bronlarni o'qib bo'lmadi", e);
        }
    }

    /**
     * Bron holatini o'zgartiradi. Mijozga xabar berish uchun mijozga_xabar bayrog'i
     * tushiriladi — mijozlar boti buni ko'rib, mijozga o'zgarishni aytadi.
     * Faqat shu dorixonaning broni o'zgaradi (birovnikiga tegib bo'lmaydi).
     */
    public boolean holatniOzgartir(long bronId, long dorixonaId, String yangiHolat) {
        String sql = "UPDATE bron SET holat = ?, mijozga_xabar = FALSE WHERE id = ? AND dorixona_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, yangiHolat);
            stmt.setLong(2, bronId);
            stmt.setLong(3, dorixonaId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Bron holatini o'zgartirib bo'lmadi", e);
        }
    }

    /** Dorixonaning kutilayotgan (javob berilmagan) bronlari soni. */
    public int yangilarSoni(long dorixonaId) {
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT COUNT(*) FROM bron WHERE dorixona_id = ? AND holat = '" + Bron.YANGI + "'")) {
            stmt.setLong(1, dorixonaId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Bronlar sonini o'qib bo'lmadi", e);
        }
    }

    private List<Bron> read(PreparedStatement stmt) throws SQLException {
        try (ResultSet rs = stmt.executeQuery()) {
            List<Bron> list = new ArrayList<>();
            while (rs.next()) {
                Bron b = new Bron();
                b.setId(rs.getLong("id"));
                b.setDoriId(rs.getLong("dori_id"));
                b.setDorixonaId(rs.getLong("dorixona_id"));
                long mijoz = rs.getLong("mijoz_telegram_id");
                if (!rs.wasNull()) b.setMijozTelegramId(mijoz);
                b.setMijozIsmi(rs.getString("mijoz_ismi"));
                b.setMijozTelefon(rs.getString("mijoz_telefon"));
                b.setSoni(rs.getInt("soni"));
                b.setKod(rs.getString("kod"));
                b.setHolat(rs.getString("holat"));
                Timestamp sana = rs.getTimestamp("sana");
                if (sana != null) b.setSana(sana.toLocalDateTime());
                b.setDoriNomi(rs.getString("dori_nomi"));
                b.setNarx(rs.getDouble("dori_narx"));
                b.setDorixonaNomi(rs.getString("dorixona_nomi"));
                list.add(b);
            }
            return list;
        }
    }
}
