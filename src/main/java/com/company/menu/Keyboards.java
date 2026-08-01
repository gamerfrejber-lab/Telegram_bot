package com.company.menu;

import com.company.model.Dorixona;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

public final class Keyboards {

    public static final String LANG_UZ = "🇺🇿 Uzb";
    public static final String LANG_RU = "🇷🇺 Rus";

    public static final String HELP_UZ = "🆘 Yordam";
    public static final String HELP_RU = "🆘 Помощь";
    public static final String BACK_UZ = "⬅️ Orqaga";
    public static final String BACK_RU = "⬅️ Назад";
    public static final String CANCEL_UZ = "❌ Bekor qilish";
    public static final String CANCEL_RU = "❌ Отменить";
    public static final String SKIP_UZ = "⏭ O'tkazib yuborish";
    public static final String SKIP_RU = "⏭ Пропустить";
    public static final String SEND_PHONE_UZ = "📱 Raqamni yuborish";
    public static final String SEND_PHONE_RU = "📱 Отправить номер";

    public static final String CLAIM_UZ = "🏥 Dorixonamni ulash";
    public static final String CLAIM_RU = "🏥 Подключить мою аптеку";

    public static final String PRODUCTS_UZ = "📦 Mahsulotlarim";
    public static final String PRODUCTS_RU = "📦 Мои товары";
    public static final String ADD_PRODUCT_UZ = "➕ Mahsulot qo'shish";
    public static final String ADD_PRODUCT_RU = "➕ Добавить товар";
    public static final String STOCK_IN_UZ = "📥 Kirim";
    public static final String STOCK_IN_RU = "📥 Приход";
    public static final String STOCK_OUT_UZ = "📤 Chiqim";
    public static final String STOCK_OUT_RU = "📤 Расход";
    public static final String BRONS_UZ = "🔔 Bronlar";
    public static final String BRONS_RU = "🔔 Брони";
    public static final String REPORT_UZ = "📈 Hisobot";
    public static final String REPORT_RU = "📈 Отчёт";
    public static final String SUBSCRIPTION_UZ = "🗓 Obunam";
    public static final String SUBSCRIPTION_RU = "🗓 Моя подписка";

    public static final String ADMIN_ADD_PHARMACY_UZ = "➕ Dorixona qo'shish";
    public static final String ADMIN_ADD_PHARMACY_RU = "➕ Добавить аптеку";
    public static final String ADMIN_PHARMACIES_UZ = "📋 Dorixonalar";
    public static final String ADMIN_PHARMACIES_RU = "📋 Аптеки";
    public static final String ADMIN_CLAIMS_UZ = "📨 Arizalar";
    public static final String ADMIN_CLAIMS_RU = "📨 Заявки";
    public static final String ADMIN_STATS_UZ = "📊 Statistika";
    public static final String ADMIN_STATS_RU = "📊 Статистика";

    private Keyboards() { }

    public static boolean isRu(String lang) { return "ru".equals(lang); }

    // ——— Menyular ———

    /** Hali dorixonasi ulanmagan foydalanuvchi uchun. */
    public static ReplyKeyboard guestMenu(String lang) {
        List<KeyboardRow> rows = new ArrayList<>();
        rows.add(languageRow());
        rows.add(row(claim(lang)));
        rows.add(row(help(lang)));
        return markup(rows);
    }

    /** Tasdiqlangan dorixona egasi uchun ish kabineti. */
    public static ReplyKeyboard ownerMenu(String lang) {
        List<KeyboardRow> rows = new ArrayList<>();
        rows.add(languageRow());
        rows.add(row(products(lang), brons(lang)));
        rows.add(row(addProduct(lang), report(lang)));
        rows.add(row(stockIn(lang), stockOut(lang)));
        rows.add(row(subscription(lang), help(lang)));
        return markup(rows);
    }

    public static ReplyKeyboard adminMenu(String lang) {
        List<KeyboardRow> rows = new ArrayList<>();
        rows.add(languageRow());
        rows.add(row(adminAddPharmacy(lang), adminPharmacies(lang)));
        rows.add(row(adminClaims(lang), adminStats(lang)));
        rows.add(row(help(lang)));
        return markup(rows);
    }

    public static ReplyKeyboard cancelMenu(String lang) {
        List<KeyboardRow> rows = new ArrayList<>();
        rows.add(row(cancel(lang)));
        return markup(rows);
    }

    public static ReplyKeyboard skipCancelMenu(String lang) {
        List<KeyboardRow> rows = new ArrayList<>();
        rows.add(row(skip(lang)));
        rows.add(row(cancel(lang)));
        return markup(rows);
    }

    public static ReplyKeyboard phoneRequest(String lang) {
        KeyboardButton phone = new KeyboardButton(sendPhone(lang));
        phone.setRequestContact(true);
        KeyboardRow phoneRow = new KeyboardRow();
        phoneRow.add(phone);
        List<KeyboardRow> rows = new ArrayList<>();
        rows.add(phoneRow);
        rows.add(row(cancel(lang)));
        return markup(rows);
    }

    /** Obuna muddati tanlovi: 1, 3, 6, 9, 12 oy. */
    public static ReplyKeyboard monthsMenu(String lang) {
        boolean ru = isRu(lang);
        String oy = ru ? " мес." : " oy";
        List<KeyboardRow> rows = new ArrayList<>();
        rows.add(row("1" + oy, "3" + oy, "6" + oy));
        rows.add(row("9" + oy, "12" + oy));
        rows.add(row(cancel(lang)));
        return markup(rows);
    }

    /** Tanlangan matndan oy sonini o'qiydi (mos kelmasa 0). */
    public static int months(String text) {
        if (text == null) return 0;
        String digits = text.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) return 0;
        int value = Integer.parseInt(digits);
        return switch (value) {
            case 1, 3, 6, 9, 12 -> value;
            default -> 0;
        };
    }

    public static ReplyKeyboard listMenu(String lang, List<String> items) {
        List<KeyboardRow> rows = new ArrayList<>();
        for (String item : items) rows.add(row(item));
        rows.add(row(cancel(lang)));
        return markup(rows);
    }

    public static ReplyKeyboard pharmacyMenu(String lang, List<Dorixona> pharmacies) {
        List<KeyboardRow> rows = new ArrayList<>();
        for (Dorixona pharmacy : pharmacies) rows.add(row(pharmacy.getNomi()));
        rows.add(row(cancel(lang)));
        return markup(rows);
    }

    // ——— Inline tugmalar ———

    /** Admin uchun ariza qarori: tasdiqlash yoki rad etish. */
    public static InlineKeyboardMarkup claimDecision(long soovId) {
        InlineKeyboardButton ok = new InlineKeyboardButton("✅ Tasdiqlash");
        ok.setCallbackData("soov:ok:" + soovId);
        InlineKeyboardButton no = new InlineKeyboardButton("❌ Rad etish");
        no.setCallbackData("soov:no:" + soovId);
        return inline(List.of(List.of(ok, no)));
    }

    /** Dorixona egasi uchun bron qarori. */
    public static InlineKeyboardMarkup bronActions(long bronId, boolean tayyorlangan) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        if (!tayyorlangan) {
            InlineKeyboardButton ok = new InlineKeyboardButton("✅ Tayyorladim");
            ok.setCallbackData("bron:tayyor:" + bronId);
            InlineKeyboardButton no = new InlineKeyboardButton("❌ Yo'q / bekor");
            no.setCallbackData("bron:bekor:" + bronId);
            rows.add(List.of(ok, no));
        } else {
            InlineKeyboardButton given = new InlineKeyboardButton("📦 Berildi");
            given.setCallbackData("bron:berildi:" + bronId);
            InlineKeyboardButton no = new InlineKeyboardButton("❌ Bekor");
            no.setCallbackData("bron:bekor:" + bronId);
            rows.add(List.of(given, no));
        }
        return inline(rows);
    }

    private static InlineKeyboardMarkup inline(List<List<InlineKeyboardButton>> rows) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(rows);
        return markup;
    }

    // ——— Tugma matnlarini tanish ———

    public static boolean isUzbek(String text) { return LANG_UZ.equals(text) || "Uzb".equals(text); }
    public static boolean isRussian(String text) { return LANG_RU.equals(text) || "Rus".equals(text); }
    public static boolean isHelp(String text) { return eq(text, HELP_UZ, HELP_RU, "Yordam", "Помощь"); }
    public static boolean isBack(String text) { return eq(text, BACK_UZ, BACK_RU, "Orqaga", "Назад"); }
    public static boolean isCancel(String text) { return eq(text, CANCEL_UZ, CANCEL_RU, "Bekor qilish", "Отменить"); }
    public static boolean isSkip(String text) { return eq(text, SKIP_UZ, SKIP_RU, "O'tkazib yuborish", "Пропустить"); }
    public static boolean isClaim(String text) { return eq(text, CLAIM_UZ, CLAIM_RU); }
    public static boolean isProducts(String text) { return eq(text, PRODUCTS_UZ, PRODUCTS_RU); }
    public static boolean isAddProduct(String text) { return eq(text, ADD_PRODUCT_UZ, ADD_PRODUCT_RU); }
    public static boolean isStockIn(String text) { return eq(text, STOCK_IN_UZ, STOCK_IN_RU); }
    public static boolean isStockOut(String text) { return eq(text, STOCK_OUT_UZ, STOCK_OUT_RU); }
    public static boolean isBrons(String text) { return eq(text, BRONS_UZ, BRONS_RU) || startsWith(text, BRONS_UZ, BRONS_RU); }
    public static boolean isReport(String text) { return eq(text, REPORT_UZ, REPORT_RU); }
    public static boolean isSubscription(String text) { return eq(text, SUBSCRIPTION_UZ, SUBSCRIPTION_RU); }
    public static boolean isAdminAddPharmacy(String text) { return eq(text, ADMIN_ADD_PHARMACY_UZ, ADMIN_ADD_PHARMACY_RU); }
    public static boolean isAdminPharmacies(String text) { return eq(text, ADMIN_PHARMACIES_UZ, ADMIN_PHARMACIES_RU); }
    public static boolean isAdminClaims(String text) { return eq(text, ADMIN_CLAIMS_UZ, ADMIN_CLAIMS_RU) || startsWith(text, ADMIN_CLAIMS_UZ, ADMIN_CLAIMS_RU); }
    public static boolean isAdminStats(String text) { return eq(text, ADMIN_STATS_UZ, ADMIN_STATS_RU); }

    // ——— Tilga mos matnlar ———

    public static String help(String lang) { return isRu(lang) ? HELP_RU : HELP_UZ; }
    public static String back(String lang) { return isRu(lang) ? BACK_RU : BACK_UZ; }
    public static String cancel(String lang) { return isRu(lang) ? CANCEL_RU : CANCEL_UZ; }
    public static String skip(String lang) { return isRu(lang) ? SKIP_RU : SKIP_UZ; }
    public static String sendPhone(String lang) { return isRu(lang) ? SEND_PHONE_RU : SEND_PHONE_UZ; }
    public static String claim(String lang) { return isRu(lang) ? CLAIM_RU : CLAIM_UZ; }
    public static String products(String lang) { return isRu(lang) ? PRODUCTS_RU : PRODUCTS_UZ; }
    public static String addProduct(String lang) { return isRu(lang) ? ADD_PRODUCT_RU : ADD_PRODUCT_UZ; }
    public static String stockIn(String lang) { return isRu(lang) ? STOCK_IN_RU : STOCK_IN_UZ; }
    public static String stockOut(String lang) { return isRu(lang) ? STOCK_OUT_RU : STOCK_OUT_UZ; }
    public static String brons(String lang) { return isRu(lang) ? BRONS_RU : BRONS_UZ; }
    public static String report(String lang) { return isRu(lang) ? REPORT_RU : REPORT_UZ; }
    public static String subscription(String lang) { return isRu(lang) ? SUBSCRIPTION_RU : SUBSCRIPTION_UZ; }
    public static String adminAddPharmacy(String lang) { return isRu(lang) ? ADMIN_ADD_PHARMACY_RU : ADMIN_ADD_PHARMACY_UZ; }
    public static String adminPharmacies(String lang) { return isRu(lang) ? ADMIN_PHARMACIES_RU : ADMIN_PHARMACIES_UZ; }
    public static String adminClaims(String lang) { return isRu(lang) ? ADMIN_CLAIMS_RU : ADMIN_CLAIMS_UZ; }
    public static String adminStats(String lang) { return isRu(lang) ? ADMIN_STATS_RU : ADMIN_STATS_UZ; }

    private static boolean eq(String text, String... values) {
        for (String value : values) if (value.equals(text)) return true;
        return false;
    }

    /** "🔔 Bronlar (3)" kabi son qo'shilgan tugmalarni ham tanish uchun. */
    private static boolean startsWith(String text, String... values) {
        if (text == null) return false;
        for (String value : values) if (text.startsWith(value)) return true;
        return false;
    }

    private static KeyboardRow languageRow() { return row(LANG_UZ, LANG_RU); }

    private static KeyboardRow row(String... values) {
        KeyboardRow row = new KeyboardRow();
        for (String value : values) row.add(value);
        return row;
    }

    private static ReplyKeyboard markup(List<KeyboardRow> rows) {
        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setResizeKeyboard(true);
        markup.setKeyboard(rows);
        return markup;
    }
}
