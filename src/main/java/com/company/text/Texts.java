package com.company.text;

/** Botning barcha matnlari — o'zbekcha va ruscha. */
public final class Texts {

    private Texts() { }

    public static boolean ru(String lang) { return "ru".equals(lang); }

    public static String t(String lang, String key) {
        boolean ru = ru(lang);
        return switch (key) {
            case "languageChanged" -> ru ? "🇷🇺 Язык изменён на русский." : "🇺🇿 Til o'zbekchaga o'zgartirildi.";

            case "welcomeGuest" -> ru
                    ? """
                      👋 <b>Добро пожаловать!</b>

                      Это <b>рабочий кабинет аптеки</b>. Здесь аптека ведёт свои товары, \
                      остатки и принимает брони от покупателей.

                      🏥 Если вы владелец аптеки и оформили подписку — нажмите \
                      «Подключить мою аптеку».
                      """
                    : """
                      👋 <b>Xush kelibsiz!</b>

                      Bu — <b>dorixona ish kabineti</b>. Bu yerda dorixona o'z mahsulotlarini, \
                      qoldiqlarini yuritadi va xaridorlardan bron qabul qiladi.

                      🏥 Agar siz dorixona egasi bo'lsangiz va obuna rasmiylashtirgan bo'lsangiz — \
                      «Dorixonamni ulash» tugmasini bosing.
                      """;

            case "help" -> ru
                    ? """
                      🆘 <b>Раздел помощи</b>

                      🏥 <b>Подключить мою аптеку</b> — привязать вашу аптеку к этому аккаунту. \
                      Нужны: номер телефона, фото лицензии и живое фото аптеки с одноразовым кодом.
                      📦 <b>Мои товары</b> — список товаров и остатки.
                      ➕ <b>Добавить товар</b> — новое лекарство или медизделие.
                      📥 <b>Приход</b> / 📤 <b>Расход</b> — учёт склада.
                      🔔 <b>Брони</b> — заказы покупателей: подтвердить, выдать или отменить.
                      📈 <b>Отчёт</b> — сколько поступило, продано, осталось.

                      👨‍💻 Админ: @Anvarovich_2bot
                      ❓ Если у вас есть вопросы, можете обратиться к админу.
                      """
                    : """
                      🆘 <b>Yordam bo'limi</b>

                      🏥 <b>Dorixonamni ulash</b> — dorixonangizni shu hisobga biriktirish. \
                      Kerak bo'ladi: telefon raqami, litsenziya surati va bir martalik kod bilan \
                      dorixonaning jonli surati.
                      📦 <b>Mahsulotlarim</b> — mahsulotlar ro'yxati va qoldiqlar.
                      ➕ <b>Mahsulot qo'shish</b> — yangi dori yoki tibbiy buyum.
                      📥 <b>Kirim</b> / 📤 <b>Chiqim</b> — ombor hisobi.
                      🔔 <b>Bronlar</b> — xaridorlar buyurtmasi: tasdiqlash, berish yoki bekor qilish.
                      📈 <b>Hisobot</b> — qancha keldi, qancha sotildi, qancha qoldi.

                      👨‍💻 Admin: @Anvarovich_2bot
                      ❓ Qandaydir savolingiz bo'lsa, adminga murojaat qilishingiz mumkin.
                      """;

            case "chooseMenu" -> ru ? "📋 Выберите из меню:" : "📋 Menyudan tanlang:";
            case "adminPanel" -> ru ? "👑 <b>Админ панель</b>" : "👑 <b>Admin panel</b>";
            case "cancelled" -> ru ? "❌ Отменено." : "❌ Bekor qilindi.";
            case "sendText" -> ru ? "✍️ Пожалуйста, отправьте текст." : "✍️ Iltimos, matn yuboring.";
            case "notOwner" -> ru
                    ? "🏥 Сначала подключите свою аптеку."
                    : "🏥 Avval dorixonangizni ulang.";
            case "subscriptionExpired" -> ru
                    ? "⏳ <b>Срок подписки истёк.</b>\n\nЧтобы продолжить работу, продлите подписку у админа: @Anvarovich_2bot"
                    : "⏳ <b>Obuna muddati tugagan.</b>\n\nIshni davom ettirish uchun admindan obunani uzaytiring: @Anvarovich_2bot";
            default -> "";
        };
    }
}
