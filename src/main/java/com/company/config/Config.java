package com.company.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Sozlamalarni o'qiydi: avval environment variable (Render kabi serverlarda shunday beriladi),
 * topilmasa loyiha ildizidagi bot.properties fayli. Maxfiy qiymatlar (token, baza paroli)
 * hech qachon kodga yozilmaydi va bot.properties git'ga tushmaydi.
 */
public final class Config {

    private static Properties fileProperties;

    private Config() { }

    public static String get(String key, String defaultValue) {
        String value = System.getenv(key);
        if (value != null && !value.isBlank()) return value.trim();

        Properties properties = load();
        if (properties != null) {
            value = properties.getProperty(key);
            if (value != null && !value.isBlank()) return value.trim();
        }
        return defaultValue;
    }

    /** Majburiy sozlama: topilmasa aniq xato bilan to'xtaydi, jimgina noto'g'ri ishlamaydi. */
    public static String require(String key) {
        String value = get(key, null);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(key + " sozlanmagan — environment variable yoki bot.properties'ga yozing.");
        }
        return value;
    }

    private static synchronized Properties load() {
        if (fileProperties != null) return fileProperties;
        try (FileInputStream input = new FileInputStream("bot.properties")) {
            Properties properties = new Properties();
            properties.load(input);
            fileProperties = properties;
        } catch (IOException e) {
            fileProperties = new Properties();
        }
        return fileProperties;
    }
}
