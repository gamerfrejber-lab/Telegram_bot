package com.company.telegram;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;

/**
 * Xabar yuborish imkoniyati. Boshqaruv mantiqi (Router) shu interfeys orqali ishlaydi,
 * shuning uchun u Telegram kutubxonasiga bevosita bog'lanmaydi va sinash osonroq bo'ladi.
 */
public interface Sender {

    void text(long chatId, String html);

    void text(long chatId, String html, ReplyKeyboard keyboard);

    void inlineText(long chatId, String html, InlineKeyboardMarkup markup);

    /**
     * Inline tugmali xabarni joyida yangilaydi (sahifalash, kartani ochish). Xabar juda eski
     * bo'lsa yoki tahrirlab bo'lmasa, yangi xabar yuboriladi.
     */
    void editInline(long chatId, int messageId, String html, InlineKeyboardMarkup markup);

    /** Rasm yuboradi (file_id orqali — rasm Telegram serverida allaqachon bor). */
    void photo(long chatId, String fileId, String caption, ReplyKeyboard keyboard);

    /**
     * Rasmni Telegram'dan yuklab oladi. Baytlar bazaga saqlanadi, chunki file_id faqat
     * uni olgan botda ishlaydi — mijozlar boti o'sha id bilan rasmni ko'rsata olmaydi.
     * Yuklab bo'lmasa null qaytadi.
     */
    byte[] download(String fileId);
}
