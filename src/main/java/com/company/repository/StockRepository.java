package com.company.repository;

import com.company.db.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Ombor hisobi: kirim (mahsulot keldi) va chiqim (sotildi) alohida yozib boriladi.
 * Qoldiq saqlanmaydi — u har doim kirim minus chiqim sifatida hisoblanadi.
 */
public class StockRepository {

    public static final String KIRIM = "KIRIM";
    public static final String CHIQIM = "CHIQIM";

    private static final String QOLDIQ_EXPR =
            "COALESCE((SELECT SUM(CASE WHEN turi = 'KIRIM' THEN soni ELSE -soni END) "
            + "FROM ombor_harakat WHERE dori_id = ?), 0)";

    public int kirim(long doriId, int soni, String izoh) {
        if (soni <= 0) return 0;
        String sql = "INSERT INTO ombor_harakat (dori_id, turi, soni, izoh, sana) VALUES (?, ?, ?, ?, NOW())";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, doriId);
            stmt.setString(2, KIRIM);
            stmt.setInt(3, soni);
            stmt.setString(4, izoh);
            return stmt.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Kirimni yozib bo'lmadi", e);
        }
    }

    /**
     * Chiqim yozadi. Qoldiqdan ko'p sotishga yo'l qo'yilmaydi: tekshiruv va yozuv bitta
     * SQL buyrug'ida bajarilgani uchun bir vaqtda kelgan ikki sotuv ham qoldiqni
     * manfiyga tushira olmaydi. Qoldiq yetmasa 0 qaytadi.
     */
    public int chiqim(long doriId, int soni, String izoh) {
        if (soni <= 0) return 0;
        String sql = "INSERT INTO ombor_harakat (dori_id, turi, soni, izoh, sana) "
                + "SELECT ?, ?, ?, ?, NOW() WHERE " + QOLDIQ_EXPR + " >= ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, doriId);
            stmt.setString(2, CHIQIM);
            stmt.setInt(3, soni);
            stmt.setString(4, izoh);
            stmt.setLong(5, doriId);
            stmt.setInt(6, soni);
            return stmt.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Chiqimni yozib bo'lmadi", e);
        }
    }

    public int qoldiq(long doriId) {
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT " + QOLDIQ_EXPR + " AS qoldiq")) {
            stmt.setLong(1, doriId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt("qoldiq") : 0;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Qoldiqni o'qib bo'lmadi", e);
        }
    }

    /** Dorixona bo'yicha jami: [mahsulot turi, kelgan, sotilgan, qoldiq, tushum]. */
    public double[] hisobot(long dorixonaId) {
        String sql = """
                SELECT COUNT(DISTINCT d.id) AS turlar,
                       COALESCE(SUM(CASE WHEN h.turi = 'KIRIM'  THEN h.soni ELSE 0 END), 0) AS kelgan,
                       COALESCE(SUM(CASE WHEN h.turi = 'CHIQIM' THEN h.soni ELSE 0 END), 0) AS sotilgan,
                       COALESCE(SUM(CASE WHEN h.turi = 'CHIQIM'
                                         THEN h.soni * COALESCE(h.narx, d.narx) ELSE 0 END), 0) AS tushum
                FROM dori d
                LEFT JOIN ombor_harakat h ON h.dori_id = d.id
                WHERE d.dorixona_id = ?
                """;
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, dorixonaId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) return new double[] { 0, 0, 0, 0, 0 };
                double kelgan = rs.getDouble("kelgan");
                double sotilgan = rs.getDouble("sotilgan");
                return new double[] {
                        rs.getInt("turlar"), kelgan, sotilgan, kelgan - sotilgan, rs.getDouble("tushum")
                };
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Hisobotni tayyorlab bo'lmadi", e);
        }
    }
}
