package com.company;

import com.company.db.Database;
import com.company.service.BronNotifier;
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
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
