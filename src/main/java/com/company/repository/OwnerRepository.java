package com.company.repository;

import com.company.db.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Botdan foydalanuvchilar (dorixona egalari): ism, username, telefon va til tanlovi. */
public class OwnerRepository {

    /** Foydalanuvchini yozadi yoki mavjudini yangilaydi. */
    public void saqla(long telegramId, String ism, String username) {
        String sql = "INSERT INTO dorixona_foydalanuvchi (telegram_id, ism, username, sana) "
                + "VALUES (?, ?, ?, NOW()) "
                + "ON CONFLICT (telegram_id) DO UPDATE SET ism = EXCLUDED.ism, username = EXCLUDED.username";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, telegramId);
            stmt.setString(2, ism);
            stmt.setString(3, username);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Foydalanuvchini saqlab bo'lmadi: " + e.getMessage());
        }
    }

    public void telefonniSaqla(long telegramId, String telefon) {
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE dorixona_foydalanuvchi SET telefon = ? WHERE telegram_id = ?")) {
            stmt.setString(1, telefon);
            stmt.setLong(2, telegramId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Telefonni saqlab bo'lmadi: " + e.getMessage());
        }
    }

    public void tilniSaqla(long telegramId, String til) {
        String sql = "INSERT INTO dorixona_foydalanuvchi (telegram_id, til, sana) VALUES (?, ?, NOW()) "
                + "ON CONFLICT (telegram_id) DO UPDATE SET til = EXCLUDED.til";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, telegramId);
            stmt.setString(2, til);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Tilni saqlab bo'lmadi: " + e.getMessage());
        }
    }

    public String til(long telegramId) {
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT til FROM dorixona_foydalanuvchi WHERE telegram_id = ?")) {
            stmt.setLong(1, telegramId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String til = rs.getString("til");
                    if (til != null && !til.isBlank()) return til;
                }
            }
        } catch (SQLException e) {
            System.out.println("Tilni o'qib bo'lmadi: " + e.getMessage());
        }
        return "uz";
    }

    public int soni() {
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM dorixona_foydalanuvchi");
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            return 0;
        }
    }
}
