package utils;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.*;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;

public class KeyboardUtil {

    public static ReplyKeyboardMarkup getUserKeyboard() {
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);
        List<KeyboardRow> rows = new ArrayList<>();

        KeyboardRow r1 = new KeyboardRow();
        r1.add(new KeyboardButton("👤 Profilim"));
        r1.add(new KeyboardButton("📚 Faktlar"));

        KeyboardRow r2 = new KeyboardRow();
        r2.add(new KeyboardButton("🗓 Kun tartibi"));
        r2.add(new KeyboardButton("🎯 Son topish"));

        rows.add(r1);
        rows.add(r2);
        keyboard.setKeyboard(rows);
        return keyboard;
    }

    public static ReplyKeyboardMarkup getGuestKeyboard() {
        ReplyKeyboardMarkup kb = new ReplyKeyboardMarkup();
        kb.setResizeKeyboard(true);
        KeyboardRow row = new KeyboardRow();
        row.add(new KeyboardButton("/register"));
        kb.setKeyboard(List.of(row));
        return kb;
    }

    public static ReplyKeyboardMarkup getAdminKeyboard() {
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);
        List<KeyboardRow> rows = new ArrayList<>();
        KeyboardRow r1 = new KeyboardRow();
        r1.add(new KeyboardButton("📋 Foydalanuvchilar"));
        r1.add(new KeyboardButton("📊 Statistika"));
        KeyboardRow r2 = new KeyboardRow();
        r2.add(new KeyboardButton("📢 Xabar yuborish"));
        r2.add(new KeyboardButton("/start"));
        rows.add(r1); rows.add(r2);
        keyboard.setKeyboard(rows);
        return keyboard;
    }

    public static ReplyKeyboardMarkup getScheduleMenu() {
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);
        KeyboardRow r1 = new KeyboardRow();
        r1.add(new KeyboardButton("➕ Reja qo'shish"));
        r1.add(new KeyboardButton("📋 Rejalarim"));
        keyboard.setKeyboard(List.of(r1));
        return keyboard;
    }
}
