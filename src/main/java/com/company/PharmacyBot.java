package com.company;

import com.company.config.Config;
import com.company.controller.Router;
import com.company.telegram.Sender;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;

/** Dorixonalar boti: Telegram bilan bog'lanish qatlami. Butun mantiq Router ichida. */
public class PharmacyBot extends TelegramLongPollingBot implements Sender {

    private final Router router = new Router(this);

    /** Bron kuzatuvchisi shu orqali egasiga xabar yuboradi. */
    public Router router() {
        return router;
    }

    @Override
    public String getBotUsername() {
        return Config.get("BOT_USERNAME", "Anvarovich_02_bot");
    }

    @Override
    public String getBotToken() {
        return Config.botToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasCallbackQuery()) {
                String javob = router.handleCallback(update.getCallbackQuery());
                AnswerCallbackQuery answer = new AnswerCallbackQuery();
                answer.setCallbackQueryId(update.getCallbackQuery().getId());
                if (javob != null && !javob.isBlank()) answer.setText(javob);
                execute(answer);
                return;
            }
            if (update.hasMessage()) {
                router.handleMessage(update.getMessage());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void text(long chatId, String html) {
        text(chatId, html, null);
    }

    @Override
    public void text(long chatId, String html, ReplyKeyboard keyboard) {
        SendMessage message = new SendMessage();
        message.setChatId(Long.toString(chatId));
        message.setText(html);
        message.setParseMode("HTML");
        message.disableWebPagePreview();
        if (keyboard != null) message.setReplyMarkup(keyboard);
        try {
            execute(message);
        } catch (Exception e) {
            System.out.println("Xabar yuborilmadi (chat " + chatId + "): " + e.getMessage());
        }
    }

    @Override
    public void photo(long chatId, String fileId, String caption, ReplyKeyboard keyboard) {
        SendPhoto photo = new SendPhoto();
        photo.setChatId(Long.toString(chatId));
        photo.setPhoto(new InputFile(fileId));
        if (caption != null) {
            photo.setCaption(caption);
            photo.setParseMode("HTML");
        }
        if (keyboard != null) photo.setReplyMarkup(keyboard);
        try {
            execute(photo);
        } catch (Exception e) {
            System.out.println("Rasm yuborilmadi (chat " + chatId + "): " + e.getMessage());
        }
    }
}
