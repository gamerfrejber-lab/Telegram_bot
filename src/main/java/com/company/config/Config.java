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

    /**
     * Bot tokenini oladi va ko'rinishini tekshiradi.
     *
     * Tekshiruv bejiz emas: token o'rniga tasodifan boshqa matn (masalan fayl yo'li yoki
     * izoh) yozib qo'yilsa, Telegram kutubxonasi uni manzilga qo'shib yuboradi va
     * "Illegal character in path" degan tushunarsiz xato beradi. Shuning uchun xatoni
     * shu yerda, aniq tushuntirish bilan ushlaymiz.
     */
    public static String botToken() {
        String token = require("BOT_TOKEN").trim();
        if (!token.matches("\\d{6,}:[A-Za-z0-9_-]{30,}")) {
            String korinish = token.length() <= 8 ? token : token.substring(0, 8) + "…";
            throw new IllegalStateException(
                    "BOT_TOKEN noto'g'ri ko'rinishda: \"" + korinish + "\"\n"
                    + "Token \"123456789:AAG...\" ko'rinishida bo'lishi kerak — raqamlar, ikki nuqta, keyin uzun harf-raqamlar.\n"
                    + "Fayl yo'li yoki izoh matni emas, aynan @BotFather bergan tokenning o'zi yozilishi kerak.");
        }
        return token;
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
