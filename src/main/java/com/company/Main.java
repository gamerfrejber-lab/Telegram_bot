package com.company;

import com.company.db.Database;
import com.company.service.BronNotifier;
import com.company.telegram.HealthServer;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

/** Dorixonalar boti — ishga tushirish nuqtasi. */
public class Main {

    public static void main(String[] args) {
        try {
            Database.ensureSchema();
            System.out.println("Baza tayyor.");

            PharmacyBot bot = new PharmacyBot();
            TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
            api.registerBot(bot);
            System.out.println("Dorixonalar boti ishga tushdi: @" + bot.getBotUsername());

            new BronNotifier(bot.router()).start();

            // Render bepul xizmati uxlab qolmasligi uchun tashqi so'rovlarga "OK" javob beradi.
            HealthServer.start();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
