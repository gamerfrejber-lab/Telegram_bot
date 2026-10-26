package com.company.repository;

import com.company.db.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Dorixona egasining kabineti: dorixona ma'lumotlari (nomi, manzil, telefon, ish vaqti, xaritadagi joyi)
 * va yetkazish sozlamalari. Saytdagi "Ma'lumotlar" va "Yetkazish → Sozlamalar" bilan bir xil ustunlar va
 * qoidalar — bot yoki saytda o'zgartirilgani ikkalasida ham darhol ko'rinadi.
 */
public class KabinetRepository {

    /** Faqat shu ustunlarni o'zgartirish mumkin (ustun nomi so'rovga to'g'ridan-to'g'ri qo'shiladi). */
    private static final Set<String> USTUNLAR = Set.of(
            "nomi", "manzil", "telefon", "ish_boshlanishi", "ish_tugashi",
            "yetkazish_yoqilgan", "yetkazish_narxi", "yetkazish_km_narxi", "yetkazish_bepul_chegara",
            "yetkazish_min_summa", "yetkazish_radius_km", "yetkazish_naqd", "karta_raqami");

    /** Dorixona ma'lumotlari va yetkazish sozlamalari (sayt bilan bir xil standart qiymatlar). */
    public Map<String, Object> malumot(long dorixonaId) {
        String sql = """
                SELECT nomi, manzil, telefon, ish_boshlanishi, ish_tugashi, latitude, longitude, karta_raqami,
                       COALESCE(yetkazish_yoqilgan, FALSE) AS yetkazish_yoqilgan,
                       COALESCE(yetkazish_narxi, 0) AS yetkazish_narxi,
                       COALESCE(yetkazish_km_narxi, 0) AS yetkazish_km_narxi,
                       COALESCE(yetkazish_bepul_chegara, 0) AS yetkazish_bepul_chegara,
                       COALESCE(yetkazish_min_summa, 0) AS yetkazish_min_summa,
                       COALESCE(yetkazish_radius_km, 5) AS yetkazish_radius_km,
                       COALESCE(yetkazish_naqd, TRUE) AS yetkazish_naqd
                FROM dorixona WHERE id = ?
                """;
        try (Connection conn = Database.getConnection(); PreparedStatement st = conn.prepareStatement(sql)) {
            st.setLong(1, dorixonaId);
            try (ResultSet rs = st.executeQuery()) {
                if (!rs.next()) return null;
                Map<String, Object> m = new LinkedHashMap<>();
                ResultSetMetaData md = rs.getMetaData();
                for (int i = 1; i <= md.getColumnCount(); i++) m.put(md.getColumnLabel(i), rs.getObject(i));
                return m;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Dorixona ma'lumotlarini o'qib bo'lmadi", e);
        }
    }

    public void yoz(long dorixonaId, String ustun, Object qiymat) {
        if (!USTUNLAR.contains(ustun)) throw new IllegalArgumentException("Bu maydonni o'zgartirib bo'lmaydi: " + ustun);
        try (Connection conn = Database.getConnection();
             PreparedStatement st = conn.prepareStatement("UPDATE dorixona SET " + ustun + " = ? WHERE id = ?")) {
            st.setObject(1, qiymat);
            st.setLong(2, dorixonaId);
            st.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Dorixona ma'lumotini saqlab bo'lmadi", e);
        }
    }

    public void ishVaqti(long dorixonaId, String boshlanishi, String tugashi) {
        try (Connection conn = Database.getConnection();
             PreparedStatement st = conn.prepareStatement("UPDATE dorixona SET ish_boshlanishi = ?, ish_tugashi = ? WHERE id = ?")) {
            st.setString(1, boshlanishi);
            st.setString(2, tugashi);
            st.setLong(3, dorixonaId);
            st.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Ish vaqtini saqlab bo'lmadi", e);
        }
    }

    public void joy(long dorixonaId, double latitude, double longitude) {
        try (Connection conn = Database.getConnection();
             PreparedStatement st = conn.prepareStatement("UPDATE dorixona SET latitude = ?, longitude = ? WHERE id = ?")) {
            st.setDouble(1, latitude);
            st.setDouble(2, longitude);
            st.setLong(3, dorixonaId);
            st.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Xaritadagi joyni saqlab bo'lmadi", e);
        }
    }
}
