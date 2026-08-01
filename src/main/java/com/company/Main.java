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
            // Sozlamalarni bazaga ulanishdan oldin tekshiramiz: xato token bilan
            // ishga tushib, keyin tushunarsiz joyda yiqilishdan ko'ra darhol aytgan yaxshi.
            com.company.config.Config.botToken();

            Database.ensureSchema();
            System.out.println("Baza tayyor.");

            PharmacyBot bot = new PharmacyBot();
            TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
            api.registerBot(bot);
            System.out.println("Dorixonalar boti ishga tushdi: @" + bot.getBotUsername());

            new BronNotifier(bot.router()).start();

            // Render bepul xizmati uxlab qolmasligi uchun tashqi so'rovlarga "OK" javob beradi.
            HealthServer.start();
        } catch (IllegalStateException e) {
            // Sozlama xatosi — uzun stack trace o'rniga aniq tushuntirish ko'rsatamiz.
            System.err.println();
            System.err.println("❌ SOZLAMA XATOSI");
            System.err.println(e.getMessage());
            System.err.println();
            System.exit(1);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
