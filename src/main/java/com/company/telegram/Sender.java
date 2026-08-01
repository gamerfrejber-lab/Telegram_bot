package com.company.telegram;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;

/**
 * Xabar yuborish imkoniyati. Boshqaruv mantiqi (Router) shu interfeys orqali ishlaydi,
 * shuning uchun u Telegram kutubxonasiga bevosita bog'lanmaydi va sinash osonroq bo'ladi.
 */
public interface Sender {

    void text(long chatId, String html);

    void text(long chatId, String html, ReplyKeyboard keyboard);

    /** Rasm yuboradi (file_id orqali — rasm Telegram serverida allaqachon bor). */
    void photo(long chatId, String fileId, String caption, ReplyKeyboard keyboard);
}
