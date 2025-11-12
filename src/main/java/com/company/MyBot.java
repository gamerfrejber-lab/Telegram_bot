package com.company;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class MyBot extends TelegramLongPollingBot {

    private final MainController mainController = new MainController();

    @Override
    public void onUpdateReceived(Update update) {

        if (update.hasMessage()) {




            Message message = update.getMessage();
            Long chatId = message.getChatId();

            if (message.hasText()) {

                SendMessage sendMessage =   mainController.messageHandler( message );
                send( sendMessage );

            } else if (message.hasPhoto()) {

            }

        } else if (update.hasCallbackQuery()) {

        }

    }

    @Override
    public String getBotUsername() {
        return "@Anvarovich_02_bot";
    }

    @Override
    public String getBotToken() {
        return "8598681301:AAE6sON6nZWAe8gl25PnnEygF-yQQAVQ3UM";
    }




    public void send(SendMessage sendMessage) {
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }



}
