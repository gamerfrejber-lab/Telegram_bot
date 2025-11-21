package com.company;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class MyBot extends TelegramLongPollingBot {

    private final MainController controller = new MainController(this);

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasMessage()) {
                Message msg = update.getMessage();
                controller.handleMessage(msg);
            } else if (update.hasCallbackQuery()) {
                CallbackQuery cq = update.getCallbackQuery();
                AnswerCallbackQuery answer = new AnswerCallbackQuery();
                answer.setCallbackQueryId(cq.getId());
                try {
                    execute(answer);
                } catch (TelegramApiException ignored) {}
                controller.handleCallback(cq);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // yuborish va Message qaytarish
    public Message sendMsg(SendMessage sm) {
        try {
            return execute(sm);
        } catch (TelegramApiException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void deleteMessage(Long chatId, Integer messageId) {
        try {
            execute(new DeleteMessage(chatId.toString(), messageId));
        } catch (TelegramApiException e) {
            e.printStackTrace();
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
