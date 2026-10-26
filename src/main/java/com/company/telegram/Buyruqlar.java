package com.company.telegram;

import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScope;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeAllGroupChats;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeAllPrivateChats;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Dorixonalar botining "/" menyusi. Har bir buyruq menyudagi tugmaning o'zi — Router uni tugma matniga
 * aylantiradi, shuning uchun tugma bilan buyruq bir xil ishlaydi. Ro'yxat bot ishga tushganda yoziladi.
 */
public final class Buyruqlar {

    private Buyruqlar() { }

    /** {buyruq, o'zbekcha tavsif, ruscha tavsif}. */
    private static final String[][] SHAXSIY = {
            {"start", "Bosh menyu", "Главное меню"},
            {"mahsulotlar", "Mahsulotlarim", "Мои товары"},
            {"qoshish", "Mahsulot qo'shish", "Добавить товар"},
            {"kirim", "Kirim (mahsulot keldi)", "Приход товара"},
            {"chiqim", "Chiqim (sotildi)", "Расход (продажа)"},
            {"bronlar", "Bronlar", "Брони"},
            {"yetkazish", "Yetkazish buyurtmalari", "Заказы с доставкой"},
            {"dorixonam", "Dorixona ma'lumotlari va yetkazish sozlamalari", "Данные аптеки и настройки доставки"},
            {"hisobot", "Ombor hisoboti", "Отчёт по складу"},
            {"obuna", "Obunam", "Моя подписка"},
            {"til", "Tilni o'zgartirish", "Сменить язык"},
            {"help", "Yordam", "Помощь"},
    };

    private static final String[][] GURUH = {
            {"help", "Bot haqida", "О боте"},
    };

    public static void ornat(AbsSender bot) {
        try {
            yoz(bot, SHAXSIY, new BotCommandScopeDefault());
            yoz(bot, SHAXSIY, new BotCommandScopeAllPrivateChats());
            yoz(bot, GURUH, new BotCommandScopeAllGroupChats());
            System.out.println("Buyruqlar menyusi yangilandi.");
        } catch (Exception e) {
            System.out.println("Buyruqlar menyusini yozib bo'lmadi: " + e.getMessage());
        }
    }

    private static void yoz(AbsSender bot, String[][] royxat, BotCommandScope scope) throws Exception {
        for (String til : new String[]{null, "ru"}) {
            List<BotCommand> buyruqlar = Arrays.stream(royxat)
                    .map(b -> new BotCommand(b[0], "ru".equals(til) ? b[2] : b[1]))
                    .toList();
            SetMyCommands set = new SetMyCommands();
            set.setCommands(buyruqlar);
            set.setScope(scope);
            if (til != null) set.setLanguageCode(til);
            bot.execute(set);
        }
    }

    /** "/Bronlar@Pharmaa2_bot" → "/bronlar"; "/" bilan boshlanmasa null. Shaxsiy chatda "@bot" qismi ahamiyatsiz. */
    public static String buyruq(String text) {
        if (text == null || !text.startsWith("/")) return null;
        String b = text.trim().split("\\s+", 2)[0];
        int at = b.indexOf('@');
        if (at >= 0) b = b.substring(0, at);
        return b.toLowerCase(Locale.ROOT);
    }
}
