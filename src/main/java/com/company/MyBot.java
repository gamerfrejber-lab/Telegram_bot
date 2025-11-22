package com.company;


import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * Asosiy Bot klassi. Faoliyatni CommandRouter ga yo'naltiradi.
 */

public class MyBot extends TelegramLongPollingBot {



    @Override
    public void onUpdateReceived(Update update) {

    }

    // Yuborishdan keyin Message qaytarish uchun oson util
    public Message sendMessageAndReturn(org.telegram.telegrambots.meta.api.methods.send.SendMessage sm) {
        try {
            return execute(sm);
        } catch (TelegramApiException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public String getBotUsername() {
        return "@Anvarovich_02_bot";
    }

    @Override
    public String getBotToken() {

        String t = System.getenv("8598681301:AAE6sON6nZWAe8gl25PnnEygF-yQQAVQ3UM");
        return t != null ? t : "8598681301:AAE6sON6nZWAe8gl25PnnEygF-yQQAVQ3UM";
    }
}
