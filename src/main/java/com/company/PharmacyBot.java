package com.company;

import com.company.config.Config;
import com.company.controller.Router;
import com.company.telegram.Sender;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
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
                String javob;
                try {
                    javob = router.handleCallback(update.getCallbackQuery());
                } catch (Exception e) {
                    // Xato bo'lsa ham tugmaga javob beramiz — aks holda u "yuklanmoqda"da qotib qoladi.
                    e.printStackTrace();
                    javob = "⚠️ Xatolik yuz berdi, qaytadan urinib ko'ring.";
                }
                AnswerCallbackQuery answer = new AnswerCallbackQuery();
                answer.setCallbackQueryId(update.getCallbackQuery().getId());
                if (javob != null && !javob.isBlank()) answer.setText(javob.length() > 190 ? javob.substring(0, 190) : javob);
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
    public void inlineText(long chatId, String html, InlineKeyboardMarkup markup) {
        SendMessage message = new SendMessage();
        message.setChatId(Long.toString(chatId));
        message.setText(html);
        message.setParseMode("HTML");
        message.disableWebPagePreview();
        if (markup != null) message.setReplyMarkup(markup);
        try {
            execute(message);
        } catch (Exception e) {
            System.out.println("Xabar yuborilmadi (chat " + chatId + "): " + e.getMessage());
        }
    }

    @Override
    public void editInline(long chatId, int messageId, String html, InlineKeyboardMarkup markup) {
        EditMessageText edit = new EditMessageText();
        edit.setChatId(Long.toString(chatId));
        edit.setMessageId(messageId);
        edit.setText(html);
        edit.setParseMode("HTML");
        edit.disableWebPagePreview();
        if (markup != null) edit.setReplyMarkup(markup);
        try {
            execute(edit);
        } catch (Exception e) {
            // Matn o'zgarmagan bo'lsa Telegram xato qaytaradi — bu holda hech narsa qilmaymiz.
            if (String.valueOf(e.getMessage()).contains("not modified")) return;
            inlineText(chatId, html, markup);
        }
    }

    @Override
    public byte[] download(String fileId) {
        try {
            GetFile getFile = new GetFile();
            getFile.setFileId(fileId);
            String path = execute(getFile).getFilePath();
            try (java.io.InputStream in = downloadFileAsStream(path)) {
                return in.readAllBytes();
            }
        } catch (Exception e) {
            System.out.println("Rasmni yuklab bo'lmadi (" + fileId + "): " + e.getMessage());
            return null;
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

    @Override
    public void photoBytes(long chatId, byte[] data, String caption) {
        if (data == null || data.length == 0) return;
        SendPhoto photo = new SendPhoto();
        photo.setChatId(Long.toString(chatId));
        photo.setPhoto(new InputFile(new java.io.ByteArrayInputStream(data), "chek.jpg"));
        if (caption != null) {
            photo.setCaption(caption);
            photo.setParseMode("HTML");
        }
        try {
            execute(photo);
        } catch (Exception e) {
            System.out.println("Chek rasmi yuborilmadi (chat " + chatId + "): " + e.getMessage());
        }
    }
}
