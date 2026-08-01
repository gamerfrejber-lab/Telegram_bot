package com.company.repository;

import com.company.db.Database;
import com.company.model.Dori;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Dorixonaning mahsulotlari. Qoldiq alohida ustunda saqlanmaydi — u ombor
 * harakatlaridan (kirim minus chiqim) hisoblanadi, shuning uchun hisobot bilan
 * qoldiq hech qachon bir-biriga zid bo'lib qolmaydi.
 */
public class DrugRepository {

    private static final String SELECT =
            "SELECT d.id, d.nomi, d.nomi_ru, d.ishlab_chiqaruvchi, d.narx, d.mavjud, d.dorixona_id, "
            + "COALESCE((SELECT SUM(CASE WHEN h.turi = 'KIRIM' THEN h.soni ELSE -h.soni END) "
            + "          FROM ombor_harakat h WHERE h.dori_id = d.id), 0) AS qoldiq, "
            + "COALESCE((SELECT SUM(h.soni) FROM ombor_harakat h "
            + "          WHERE h.dori_id = d.id AND h.turi = 'KIRIM'), 0) AS kelgan "
            + "FROM dori d";

    public List<Dori> dorixonaniki(long dorixonaId) {
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT + " WHERE d.dorixona_id = ? ORDER BY d.nomi")) {
            stmt.setLong(1, dorixonaId);
            return read(stmt);
        } catch (SQLException e) {
            throw new IllegalStateException("Mahsulotlarni o'qib bo'lmadi", e);
        }
    }

    public Dori getById(long id) {
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT + " WHERE d.id = ?")) {
            stmt.setLong(1, id);
            List<Dori> found = read(stmt);
            return found.isEmpty() ? null : found.get(0);
        } catch (SQLException e) {
            throw new IllegalStateException("Mahsulotni o'qib bo'lmadi", e);
        }
    }

    /** Dorixonadagi mahsulotni nomi bo'yicha topadi (katta-kichik harf muhim emas). */
    public Dori nomiBoyicha(long dorixonaId, String nomi) {
        if (nomi == null || nomi.isBlank()) return null;
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     SELECT + " WHERE d.dorixona_id = ? AND LOWER(d.nomi) = LOWER(?) ORDER BY d.id LIMIT 1")) {
            stmt.setLong(1, dorixonaId);
            stmt.setString(2, nomi.trim());
            List<Dori> found = read(stmt);
            return found.isEmpty() ? null : found.get(0);
        } catch (SQLException e) {
            throw new IllegalStateException("Mahsulotni topib bo'lmadi", e);
        }
    }

    public long add(Dori dori) {
        String sql = "INSERT INTO dori (nomi, nomi_ru, ishlab_chiqaruvchi, narx, mavjud, dorixona_id) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, dori.getNomi());
            stmt.setString(2, dori.getNomiRu());
            stmt.setString(3, dori.getIshlabChiqaruvchi());
            stmt.setDouble(4, dori.getNarx());
            stmt.setBoolean(5, dori.isMavjud());
            stmt.setLong(6, dori.getDorixonaId());
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
            return 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Mahsulot qo'shib bo'lmadi", e);
        }
    }

    /** Narxni yangilaydi — faqat shu dorixonaga tegishli mahsulotda. */
    public boolean narxniYangila(long doriId, long dorixonaId, double narx) {
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE dori SET narx = ? WHERE id = ? AND dorixona_id = ?")) {
            stmt.setDouble(1, narx);
            stmt.setLong(2, doriId);
            stmt.setLong(3, dorixonaId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Narxni yangilab bo'lmadi", e);
        }
    }

    /** O'chiradi — faqat shu dorixonaga tegishli bo'lsa (birovnikini o'chirib bo'lmaydi). */
    public boolean ochir(long doriId, long dorixonaId) {
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "DELETE FROM dori WHERE id = ? AND dorixona_id = ?")) {
            stmt.setLong(1, doriId);
            stmt.setLong(2, dorixonaId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Mahsulotni o'chirib bo'lmadi", e);
        }
    }

    private List<Dori> read(PreparedStatement stmt) throws SQLException {
        try (ResultSet rs = stmt.executeQuery()) {
            List<Dori> list = new ArrayList<>();
            while (rs.next()) {
                Dori d = new Dori();
                d.setId(rs.getLong("id"));
                d.setNomi(rs.getString("nomi"));
                d.setNomiRu(rs.getString("nomi_ru"));
                d.setIshlabChiqaruvchi(rs.getString("ishlab_chiqaruvchi"));
                d.setNarx(rs.getDouble("narx"));
                d.setMavjud(rs.getBoolean("mavjud"));
                d.setDorixonaId(rs.getLong("dorixona_id"));
                d.setQoldiq(rs.getInt("qoldiq"));
                d.setKelgan(rs.getInt("kelgan"));
                list.add(d);
            }
            return list;
        }
    }
}
