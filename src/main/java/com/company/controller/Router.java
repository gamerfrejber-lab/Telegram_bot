package com.company.controller;

import com.company.admin.AdminPanel;
import com.company.flow.Session;
import com.company.menu.Keyboards;
import com.company.model.Bron;
import com.company.model.Dori;
import com.company.model.Dorixona;
import com.company.model.Soov;
import com.company.model.Yetkazish;
import com.company.repository.BronRepository;
import com.company.repository.DrugRepository;
import com.company.repository.OwnerRepository;
import com.company.repository.PharmacyRepository;
import com.company.repository.SoovRepository;
import com.company.repository.StockRepository;
import com.company.repository.YetkazishRepository;
import com.company.telegram.Sender;
import com.company.text.Texts;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Contact;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;

import java.io.ByteArrayInputStream;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Botning butun mantiqi: admin bo'limi, egalik arizasi va dorixona kabineti. */
public class Router {

    private static final DateTimeFormatter SANA = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final Sender sender;
    private final PharmacyRepository pharmacies = new PharmacyRepository();
    private final DrugRepository drugs = new DrugRepository();
    private final StockRepository stock = new StockRepository();
    private final SoovRepository soovlar = new SoovRepository();
    private final BronRepository bronlar = new BronRepository();
    private final YetkazishRepository yetkazishlar = new YetkazishRepository();
    private final OwnerRepository owners = new OwnerRepository();

    private final Map<Long, Session> sessions = new ConcurrentHashMap<>();

    public Router(Sender sender) {
        this.sender = sender;
    }

    // ————————————————— Xabarlarni yo'naltirish —————————————————

    public void handleMessage(Message message) {
        long chatId = message.getChatId();
        User from = message.getFrom();
        long userId = from == null ? chatId : from.getId();
        String text = message.hasText() ? message.getText().trim() : "";
        String lang = owners.til(userId);

        if (from != null) {
            owners.saqla(userId, fullName(from), from.getUserName());
        }

        // Til almashtirish har qanday bosqichda ishlaydi.
        if (Keyboards.isUzbek(text) || Keyboards.isRussian(text)) {
            String yangi = Keyboards.isRussian(text) ? "ru" : "uz";
            owners.tilniSaqla(userId, yangi);
            sender.text(chatId, Texts.t(yangi, "languageChanged"), menu(userId, yangi));
            return;
        }

        Session session = sessions.get(userId);

        // Kontakt va rasm faqat egalik arizasi jarayonida kutiladi.
        if (message.hasContact() && session != null && session.getTuri() == Session.Turi.EGALIK_ARIZASI) {
            arizaKontakt(chatId, userId, lang, session, message.getContact(), from);
            return;
        }
        if (message.hasPhoto() && session != null && session.getTuri() == Session.Turi.EGALIK_ARIZASI) {
            arizaRasm(chatId, userId, lang, session, message.getPhoto(), from);
            return;
        }
        if (message.hasDocument() && session != null
                && session.getTuri() == Session.Turi.MAHSULOT_QOSHISH && session.isFayldan()) {
            fayldanImport(chatId, userId, lang, session, message.getDocument().getFileId());
            return;
        }

        if (Keyboards.isCancel(text)) {
            sessions.remove(userId);
            sender.text(chatId, Texts.t(lang, "cancelled"), menu(userId, lang));
            return;
        }
        if (Keyboards.isHelp(text)) {
            sender.text(chatId, Texts.t(lang, "help"), menu(userId, lang));
            return;
        }
        if ("/start".equals(text)) {
            sessions.remove(userId);
            boshSahifa(chatId, userId, lang);
            return;
        }

        if (session != null) {
            if (menyuTugmasiBosildi(text, userId)) {
                sessions.remove(userId);
            } else {
                sessionQadami(chatId, userId, lang, session, text);
                return;
            }
        }

        if (AdminPanel.isAdmin(userId) && adminTugmasi(chatId, userId, lang, text)) return;
        if (egaTugmasi(chatId, userId, lang, text)) return;

        if (Keyboards.isClaim(text)) {
            arizaBoshla(chatId, userId, lang);
            return;
        }

        boshSahifa(chatId, userId, lang);
    }

    private void boshSahifa(long chatId, long userId, String lang) {
        Dorixona meniki = pharmacies.egasiBoyicha(userId);
        if (AdminPanel.isAdmin(userId)) {
            sender.text(chatId, adminBosh(lang) + (meniki == null ? "" : "\n\n" + kabinetSarlavha(meniki, lang)),
                    Keyboards.adminMenu(lang, meniki != null));
            return;
        }
        if (meniki != null) {
            sender.text(chatId, kabinetSarlavha(meniki, lang), Keyboards.ownerMenu(lang));
            return;
        }
        Soov kutilayotgan = soovlar.kutilayotgani(userId);
        if (kutilayotgan != null) {
            sender.text(chatId, Texts.ru(lang)
                    ? "🕐 Ваша заявка на аптеку «" + esc(kutilayotgan.getDorixonaNomi())
                      + "» отправлена админу и ожидает проверки.\n\nМы сообщим сразу после решения."
                    : "🕐 «" + esc(kutilayotgan.getDorixonaNomi())
                      + "» dorixonasi bo'yicha arizangiz adminga yuborildi va tekshirilmoqda.\n\n"
                      + "Qaror chiqishi bilan xabar beramiz.",
                    Keyboards.guestMenu(lang));
            return;
        }
        sender.text(chatId, Texts.t(lang, "welcomeGuest"), Keyboards.guestMenu(lang));
    }

    private ReplyKeyboard menu(long userId, String lang) {
        boolean egasi = pharmacies.egasiBoyicha(userId) != null;
        if (AdminPanel.isAdmin(userId)) return Keyboards.adminMenu(lang, egasi);
        return egasi ? Keyboards.ownerMenu(lang) : Keyboards.guestMenu(lang);
    }

    /** Dorixona egasiga ko'rsatiladigan menyu (admin bo'lsa admin tugmalari ham qoladi). */
    private ReplyKeyboard egaMenyusi(long userId, String lang) {
        return AdminPanel.isAdmin(userId) ? Keyboards.adminMenu(lang, true) : Keyboards.ownerMenu(lang);
    }

    private boolean menyuTugmasiBosildi(String text, long userId) {
        if (Keyboards.isProducts(text) || Keyboards.isAddProduct(text) || Keyboards.isDeleteProduct(text)
                || Keyboards.isStockIn(text) || Keyboards.isStockOut(text)
                || Keyboards.isBrons(text) || Keyboards.isDeliveries(text) || Keyboards.isReport(text)
                || Keyboards.isSubscription(text) || Keyboards.isClaim(text)) return true;
        if (AdminPanel.isAdmin(userId) && (Keyboards.isAdminAddPharmacy(text)
                || Keyboards.isAdminPharmacies(text) || Keyboards.isAdminClaims(text)
                || Keyboards.isAdminOrders(text) || Keyboards.isAdminStats(text))) return true;
        return false;
    }

    // ————————————————— Admin bo'limi —————————————————

    private String adminBosh(String lang) {
        int kutilmoqda = soovlar.kutilayotganlarSoni();
        return Texts.t(lang, "adminPanel") + "\n\n"
                + (Texts.ru(lang) ? "🏥 Аптек: <b>" : "🏥 Dorixonalar: <b>") + pharmacies.soni() + "</b>\n"
                + (Texts.ru(lang) ? "📨 Заявок на проверку: <b>" : "📨 Tekshirilmagan arizalar: <b>") + kutilmoqda + "</b>";
    }

    private boolean adminTugmasi(long chatId, long userId, String lang, String text) {
        if (Keyboards.isAdminAddPharmacy(text)) {
            Session session = new Session(Session.Turi.DORIXONA_QOSHISH);
            sessions.put(userId, session);
            sender.text(chatId, Texts.ru(lang)
                    ? "🏥 <b>Название аптеки:</b>" : "🏥 <b>Dorixona nomi:</b>", Keyboards.cancelMenu(lang));
            return true;
        }
        if (Keyboards.isAdminPharmacies(text)) {
            dorixonalarRoyxati(chatId, lang);
            return true;
        }
        if (Keyboards.isAdminClaims(text)) {
            arizalarRoyxati(chatId, lang);
            return true;
        }
        if (Keyboards.isAdminOrders(text)) {
            adminBuyurtmalar(chatId, lang);
            return true;
        }
        if (Keyboards.isAdminStats(text)) {
            sender.text(chatId, statistika(lang), Keyboards.adminMenu(lang));
            return true;
        }
        return false;
    }

    private void dorixonalarRoyxati(long chatId, String lang) {
        List<Dorixona> all = pharmacies.getAll();
        boolean ru = Texts.ru(lang);
        if (all.isEmpty()) {
            sender.text(chatId, ru ? "Аптек пока нет." : "Hali dorixona yo'q.", Keyboards.adminMenu(lang));
            return;
        }
        sender.text(chatId, ru
                ? "📋 <b>Аптеки (" + all.size() + ")</b>"
                : "📋 <b>Dorixonalar (" + all.size() + ")</b>", Keyboards.adminMenu(lang));

        int chiqarildi = 0;
        for (Dorixona d : all) {
            if (chiqarildi >= 15) break;
            StringBuilder sb = new StringBuilder();
            sb.append("🏥 <b>").append(esc(d.getNomi())).append("</b>\n");
            if (d.getManzil() != null && !d.getManzil().isBlank()) sb.append("📍 ").append(esc(d.getManzil())).append('\n');
            sb.append(obunaMatni(d, lang)).append('\n');
            sb.append(d.getEgasiTelegramId() == null
                    ? (ru ? "👤 владелец не подключён" : "👤 egasi ulanmagan")
                    : (ru ? "👤 владелец: " : "👤 egasi: ") + d.getEgasiTelegramId()).append('\n');
            sb.append("🆔 ").append(d.getId());
            sender.text(chatId, sb.toString(), Keyboards.pharmacyActions(d.getId(), lang));
            chiqarildi++;
        }
        if (all.size() > chiqarildi) {
            sender.text(chatId, (ru ? "… и ещё " : "… va yana ") + (all.size() - chiqarildi));
        }
    }

    private void arizalarRoyxati(long chatId, String lang) {
        List<Soov> kutilmoqda = soovlar.kutilayotganlar();
        if (kutilmoqda.isEmpty()) {
            sender.text(chatId, Texts.ru(lang) ? "📨 Новых заявок нет." : "📨 Yangi ariza yo'q.", Keyboards.adminMenu(lang));
            return;
        }
        sender.text(chatId, (Texts.ru(lang) ? "📨 <b>Заявок на проверку: " : "📨 <b>Tekshirilmagan arizalar: ")
                + kutilmoqda.size() + "</b>", Keyboards.adminMenu(lang));
        for (Soov soov : kutilmoqda) {
            arizaniAdminga(chatId, soov, lang);
        }
    }

    private void adminBuyurtmalar(long chatId, String lang) {
        List<Bron> all = bronlar.barchasi();
        boolean ru = Texts.ru(lang);
        if (all.isEmpty()) {
            sender.text(chatId, ru ? "🔔 Активных заказов нет." : "🔔 Faol buyurtma yo'q.", Keyboards.adminMenu(lang));
            return;
        }
        sender.text(chatId, (ru ? "🔔 <b>Активные заказы: " : "🔔 <b>Faol buyurtmalar: ") + all.size() + "</b>",
                Keyboards.adminMenu(lang));
        for (Bron bron : all) {
            sender.text(chatId, adminBronMatni(bron, lang));
        }
    }

    private String adminBronMatni(Bron bron, String lang) {
        boolean ru = Texts.ru(lang);
        StringBuilder sb = new StringBuilder();
        sb.append(ru ? "🔔 <b>Заказ №" : "🔔 <b>Buyurtma №").append(bron.getId()).append("</b>\n");
        sb.append("🏥 <b>").append(esc(bron.getDorixonaNomi())).append("</b>\n");
        sb.append("💊 ").append(esc(bron.getDoriNomi())).append('\n');
        sb.append(ru ? "🔢 " : "🔢 ").append(bron.getSoni()).append(ru ? " шт. × " : " ta × ");
        sb.append(son(bron.getNarx())).append(ru ? " сум\n" : " so'm\n");
        if (bron.getMijozIsmi() != null && !bron.getMijozIsmi().isBlank()) {
            sb.append(ru ? "👤 " : "👤 ").append(esc(bron.getMijozIsmi())).append('\n');
        }
        sb.append(bron.holatMatni(ru));
        return sb.toString();
    }

    private String statistika(String lang) {
        List<Dorixona> all = pharmacies.getAll();
        long ulangan = all.stream().filter(d -> d.getEgasiTelegramId() != null).count();
        long faol = all.stream().filter(Dorixona::obunaFaol).count();
        boolean ru = Texts.ru(lang);
        return (ru ? "📊 <b>Статистика</b>\n\n" : "📊 <b>Statistika</b>\n\n")
                + (ru ? "🏥 Всего аптек: <b>" : "🏥 Jami dorixonalar: <b>") + all.size() + "</b>\n"
                + (ru ? "👤 Подключено владельцев: <b>" : "👤 Egasi ulangan: <b>") + ulangan + "</b>\n"
                + (ru ? "✅ Активных подписок: <b>" : "✅ Faol obunalar: <b>") + faol + "</b>\n"
                + (ru ? "📨 Заявок в ожидании: <b>" : "📨 Kutilayotgan arizalar: <b>") + soovlar.kutilayotganlarSoni() + "</b>\n"
                + (ru ? "👥 Пользователей бота: <b>" : "👥 Bot foydalanuvchilari: <b>") + owners.soni() + "</b>";
    }

    // ————————————————— Dorixona egasining kabineti —————————————————

    private String kabinetSarlavha(Dorixona d, String lang) {
        boolean ru = Texts.ru(lang);
        int yangiBron = bronlar.yangilarSoni(d.getId());
        StringBuilder sb = new StringBuilder();
        sb.append(ru ? "🏥 <b>Кабинет аптеки</b>\n\n" : "🏥 <b>Dorixona kabineti</b>\n\n");
        sb.append("<b>").append(esc(d.getNomi())).append("</b>\n");
        sb.append(obunaMatni(d, lang)).append('\n');
        if (yangiBron > 0) {
            sb.append(ru ? "\n🔔 <b>Новых броней: " : "\n🔔 <b>Yangi bronlar: ").append(yangiBron).append("</b>");
        }
        return sb.toString();
    }

    private boolean egaTugmasi(long chatId, long userId, String lang, String text) {
        Dorixona meniki = pharmacies.egasiBoyicha(userId);
        if (meniki == null) return false;

        boolean kabinetTugmasi = Keyboards.isProducts(text) || Keyboards.isAddProduct(text) || Keyboards.isDeleteProduct(text)
                || Keyboards.isStockIn(text) || Keyboards.isStockOut(text)
                || Keyboards.isBrons(text) || Keyboards.isDeliveries(text) || Keyboards.isReport(text)
                || Keyboards.isSubscription(text);
        if (!kabinetTugmasi) return false;

        // Obuna tugagan bo'lsa faqat obuna bo'limi ochiq qoladi.
        if (!meniki.obunaFaol() && !Keyboards.isSubscription(text)) {
            sender.text(chatId, Texts.t(lang, "subscriptionExpired"), egaMenyusi(chatId, lang));
            return true;
        }

        if (Keyboards.isProducts(text)) {
            mahsulotlar(chatId, meniki, lang);
            return true;
        }
        if (Keyboards.isDeleteProduct(text)) {
            if (drugs.dorixonaniki(meniki.getId()).isEmpty()) {
                sender.text(chatId, Texts.ru(lang) ? "📦 Товаров пока нет." : "📦 Hali mahsulot yo'q.", egaMenyusi(chatId, lang));
                return true;
            }
            Session session = new Session(Session.Turi.OCHIRISH);
            session.setDorixonaId(meniki.getId());
            sessions.put(userId, session);
            ochirishRoyxati(chatId, lang, session, null);
            return true;
        }
        if (Keyboards.isAddProduct(text)) {
            Session session = new Session(Session.Turi.MAHSULOT_QOSHISH);
            session.setDorixonaId(meniki.getId());
            sessions.put(userId, session);
            sender.text(chatId, Texts.ru(lang)
                    ? "📦 <b>Как добавить товар?</b>\n\nВыберите способ:"
                    : "📦 <b>Mahsulot qo'shish usuli?</b>\n\nUsulni tanlang:", Keyboards.addMethodMenu(lang));
            return true;
        }
        if (Keyboards.isStockIn(text) || Keyboards.isStockOut(text)) {
            boolean kirim = Keyboards.isStockIn(text);
            List<Dori> royxat = drugs.dorixonaniki(meniki.getId());
            if (royxat.isEmpty()) {
                sender.text(chatId, Texts.ru(lang)
                        ? "❌ Сначала добавьте товар." : "❌ Avval mahsulot qo'shing.", egaMenyusi(chatId, lang));
                return true;
            }
            Session session = new Session(kirim ? Session.Turi.KIRIM : Session.Turi.CHIQIM);
            session.setDorixonaId(meniki.getId());
            sessions.put(userId, session);
            // Mahsulotlar inline tugmalarda: yuzlab mahsulotni oddiy klaviaturaga joylasa,
            // Telegram xabarni rad etardi. Nomini yozib yuborsa ham bo'ladi.
            sender.text(chatId, kirim
                            ? (Texts.ru(lang) ? "📥 <b>Приход</b>\nВыберите товар ниже или напишите его название."
                                              : "📥 <b>Kirim</b>\nQuyidan mahsulotni tanlang yoki nomini yozing.")
                            : (Texts.ru(lang) ? "📤 <b>Продажа</b>\nВыберите товар ниже или напишите его название."
                                              : "📤 <b>Sotuv</b>\nQuyidan mahsulotni tanlang yoki nomini yozing."),
                    Keyboards.cancelMenu(lang));
            sender.inlineText(chatId, kirim
                            ? (Texts.ru(lang) ? "📥 <b>Что поступило?</b>" : "📥 <b>Nima keldi?</b>")
                            : (Texts.ru(lang) ? "📤 <b>Что продано?</b>" : "📤 <b>Nima sotildi?</b>"),
                    omborTanlov(royxat, kirim, 0));
            return true;
        }
        if (Keyboards.isBrons(text)) {
            bronlarRoyxati(chatId, meniki, lang);
            return true;
        }
        if (Keyboards.isDeliveries(text)) {
            yetkazishlarRoyxati(chatId, meniki, lang);
            return true;
        }
        if (Keyboards.isReport(text)) {
            hisobot(chatId, meniki, lang);
            return true;
        }
        if (Keyboards.isSubscription(text)) {
            sender.text(chatId, obunaTafsiloti(meniki, lang), egaMenyusi(chatId, lang));
            return true;
        }
        return false;
    }

    private static final int SAHIFA = 8;

    /**
     * "Mahsulotlarim": bitta xabar, har bir mahsulot — alohida inline tugma, pastda sahifalash.
     * Avval har bir mahsulot alohida xabar bo'lib kelardi va faqat birinchi 30 tasi ko'rinardi —
     * fayldan ko'p mahsulot import qilinsa, qolganlarini ochib ham, o'chirib ham bo'lmasdi.
     */
    private void mahsulotlar(long chatId, Dorixona d, String lang) {
        boolean ru = Texts.ru(lang);
        if (drugs.dorixonaniki(d.getId()).isEmpty()) {
            sender.text(chatId, ru ? "📦 Товаров пока нет. Нажмите «Добавить товар»."
                    : "📦 Hali mahsulot yo'q. «Mahsulot qo'shish» tugmasini bosing.", egaMenyusi(chatId, lang));
            return;
        }
        Object[] view = mahsulotlarSahifasi(d, 0, lang);
        sender.inlineText(chatId, (String) view[0], (InlineKeyboardMarkup) view[1]);
    }

    /** Mahsulotlar ro'yxatining bitta sahifasi: [matn, tugmalar]. */
    private Object[] mahsulotlarSahifasi(Dorixona d, int page, String lang) {
        boolean ru = Texts.ru(lang);
        List<Dori> royxat = drugs.dorixonaniki(d.getId());
        int pages = Math.max(1, (royxat.size() + SAHIFA - 1) / SAHIFA);
        page = Math.max(0, Math.min(page, pages - 1));
        List<String[]> items = new ArrayList<>();
        for (Dori dori : royxat.subList(Math.min(royxat.size(), page * SAHIFA), Math.min(royxat.size(), (page + 1) * SAHIFA))) {
            String belgi = !dori.hisobYuritiladi() ? "💊" : dori.getQoldiq() <= 0 ? "🔴" : "🟢";
            String label = belgi + " " + qisqa(dori.getNomi(), 38) + " — " + son(dori.getNarx()) + (ru ? " сум" : " so'm")
                    + (dori.hisobYuritiladi() ? " · " + dori.getQoldiq() + (ru ? " шт." : " ta") : "");
            items.add(new String[]{label, "dori:kor:" + dori.getId() + ":" + page});
        }
        String matn = (ru ? "📦 <b>Мои товары</b> — " : "📦 <b>Mahsulotlarim</b> — ") + royxat.size() + (ru ? " шт." : " ta")
                + (pages > 1 ? (ru ? "  (стр. " : "  (") + (page + 1) + "/" + pages + (ru ? ")" : "-sahifa)") : "") + "\n\n"
                + (ru ? "🟢 есть · 🔴 закончился · 💊 учёт не ведётся\n\n👇 Нажмите на товар, чтобы открыть или удалить."
                      : "🟢 bor · 🔴 tugagan · 💊 hisob yuritilmagan\n\n👇 Ochish yoki o'chirish uchun mahsulotni bosing.");
        return new Object[]{matn, Keyboards.inlineList(items, page, pages, "dori:sahifa:")};
    }

    private String mahsulotKartasi(Dori dori, boolean ru) {
        return "📦 <b>" + esc(dori.getNomi()) + "</b>\n\n"
                + "💵 " + son(dori.getNarx()) + (ru ? " сум" : " so'm") + "\n"
                + qoldiqMatni(dori, ru)
                + (dori.hisobYuritiladi() ? "\n" + (ru ? "📥 Всего поступило: " : "📥 Jami kelgan: ") + dori.getKelgan() + (ru ? " шт." : " ta") : "");
    }

    private String qoldiqMatni(Dori dori, boolean ru) {
        if (!dori.hisobYuritiladi()) return ru ? "📦 учёт не ведётся" : "📦 hisob yuritilmagan";
        if (dori.getQoldiq() <= 0) return ru ? "🔴 закончился" : "🔴 tugagan";
        return (ru ? "📊 остаток: " : "📊 qoldiq: ") + dori.getQoldiq() + (ru ? " шт." : " ta");
    }

    private void hisobot(long chatId, Dorixona d, String lang) {
        double[] h = stock.hisobot(d.getId());
        boolean ru = Texts.ru(lang);
        String matn = (ru ? "📈 <b>Отчёт по складу</b>\n" : "📈 <b>Ombor hisoboti</b>\n")
                + "<b>" + esc(d.getNomi()) + "</b>\n\n"
                + (ru ? "Наименований: <b>" : "Mahsulot turi: <b>") + (long) h[0] + (ru ? "</b>\n" : "</b> xil\n")
                + (ru ? "📥 Поступило: <b>" : "📥 Kelgan: <b>") + (long) h[1] + (ru ? "</b> шт.\n" : "</b> ta\n")
                + (ru ? "📤 Продано: <b>" : "📤 Sotilgan: <b>") + (long) h[2] + (ru ? "</b> шт.\n" : "</b> ta\n")
                + (ru ? "📊 Остаток: <b>" : "📊 Qoldiq: <b>") + (long) h[3] + (ru ? "</b> шт.\n" : "</b> ta\n")
                + (ru ? "💰 Выручка: <b>" : "💰 Tushum: <b>") + son(h[4]) + (ru ? "</b> сум" : "</b> so'm");
        sender.text(chatId, matn, egaMenyusi(chatId, lang));
    }

    private String obunaMatni(Dorixona d, String lang) {
        boolean ru = Texts.ru(lang);
        if (d.getObunaTugash() == null) {
            return ru ? "🗓 Подписка: без срока" : "🗓 Obuna: muddatsiz";
        }
        long kun = d.qolganKun();
        if (kun < 0) return ru ? "⏳ Подписка истекла " + d.getObunaTugash().format(SANA)
                : "⏳ Obuna tugagan: " + d.getObunaTugash().format(SANA);
        return (ru ? "🗓 Подписка до " : "🗓 Obuna: ") + d.getObunaTugash().format(SANA)
                + (ru ? " (осталось " + kun + " дн.)" : " gacha (" + kun + " kun qoldi)");
    }

    private String obunaTafsiloti(Dorixona d, String lang) {
        boolean ru = Texts.ru(lang);
        return (ru ? "🗓 <b>Моя подписка</b>\n\n" : "🗓 <b>Obunam</b>\n\n")
                + "🏥 <b>" + esc(d.getNomi()) + "</b>\n"
                + (d.getManzil() == null || d.getManzil().isBlank() ? "" : "📍 " + esc(d.getManzil()) + "\n")
                + obunaMatni(d, lang) + "\n\n"
                + (ru ? "Для продления обратитесь к админу: @Anvarovich_2bot"
                      : "Uzaytirish uchun adminga murojaat qiling: @Anvarovich_2bot");
    }

    private void bronlarRoyxati(long chatId, Dorixona d, String lang) {
        List<Bron> royxat = bronlar.dorixonaniki(d.getId(), true);
        boolean ru = Texts.ru(lang);
        if (royxat.isEmpty()) {
            sender.text(chatId, ru ? "🔔 Активных броней нет." : "🔔 Faol bron yo'q.", egaMenyusi(chatId, lang));
            return;
        }
        sender.text(chatId, (ru ? "🔔 <b>Активные брони: " : "🔔 <b>Faol bronlar: ") + royxat.size() + "</b>",
                egaMenyusi(chatId, lang));
        for (Bron bron : royxat) {
            sender.text(chatId, bronMatni(bron, lang),
                    Keyboards.bronActions(bron.getId(), Bron.TAYYOR.equals(bron.getHolat()), lang));
        }
    }

    private String bronMatni(Bron bron, String lang) {
        boolean ru = Texts.ru(lang);
        StringBuilder sb = new StringBuilder();
        sb.append(ru ? "🔔 <b>Бронь №" : "🔔 <b>Bron №").append(bron.getId()).append("</b>\n\n");
        sb.append("💊 <b>").append(esc(bron.getDoriNomi())).append("</b>\n");
        sb.append(ru ? "🔢 Количество: <b>" : "🔢 Soni: <b>").append(bron.getSoni()).append(ru ? "</b> шт.\n" : "</b> ta\n");
        sb.append(ru ? "💵 Цена: " : "💵 Narxi: ").append(son(bron.getNarx())).append(ru ? " сум\n" : " so'm\n");
        if (bron.getMijozIsmi() != null && !bron.getMijozIsmi().isBlank()) {
            sb.append(ru ? "👤 Покупатель: " : "👤 Xaridor: ").append(esc(bron.getMijozIsmi())).append('\n');
        }
        if (bron.getMijozTelefon() != null && !bron.getMijozTelefon().isBlank()) {
            sb.append("☎️ ").append(esc(bron.getMijozTelefon())).append('\n');
        }
        sb.append(ru ? "🔑 Код выдачи: <code>" : "🔑 Olib ketish kodi: <code>").append(esc(bron.getKod())).append("</code>\n");
        sb.append(bron.holatMatni(ru));
        return sb.toString();
    }

    // ————————————————— Egalik arizasi —————————————————

    private void arizaBoshla(long chatId, long userId, String lang) {
        boolean ru = Texts.ru(lang);
        if (pharmacies.egasiBoyicha(userId) != null) {
            sender.text(chatId, ru ? "✅ У вас уже подключена аптека." : "✅ Sizda dorixona allaqachon ulangan.",
                    egaMenyusi(chatId, lang));
            return;
        }
        if (soovlar.kutilayotgani(userId) != null) {
            sender.text(chatId, ru ? "🕐 Ваша заявка уже отправлена и ожидает проверки."
                    : "🕐 Arizangiz allaqachon yuborilgan va tekshirilmoqda.", Keyboards.guestMenu(lang));
            return;
        }
        List<Dorixona> bosh = pharmacies.egasizlar();
        if (bosh.isEmpty()) {
            sender.text(chatId, ru
                    ? "❌ Сейчас нет аптек, ожидающих подключения.\n\nСначала оформите подписку у админа: @Anvarovich_2bot"
                    : "❌ Hozir ulanishni kutayotgan dorixona yo'q.\n\nAvval admindan obuna rasmiylashtiring: @Anvarovich_2bot",
                    Keyboards.guestMenu(lang));
            return;
        }

        Session session = new Session(Session.Turi.EGALIK_ARIZASI);
        sessions.put(userId, session);
        sender.text(chatId, ru
                ? "🏥 <b>Найдите свою аптеку</b>\n\nВыберите из списка ниже или напишите название — я поищу."
                : "🏥 <b>Dorixonangizni toping</b>\n\nQuyidagi ro'yxatdan tanlang yoki nomini yozing — qidirib beraman.",
                Keyboards.pharmacyMenu(lang, bosh.size() > 20 ? bosh.subList(0, 20) : bosh));
    }

    private void arizaQadami(long chatId, long userId, String lang, Session session, String text) {
        boolean ru = Texts.ru(lang);
        if (session.getQadam() != 0) {
            // Qolgan bosqichlar matn emas, kontakt yoki rasm kutadi.
            sender.text(chatId, ru ? "👇 Пожалуйста, используйте кнопку ниже."
                    : "👇 Iltimos, pastdagi tugmadan foydalaning.", Keyboards.cancelMenu(lang));
            return;
        }

        List<Dorixona> topilgan = pharmacies.qidir(text).stream()
                .filter(d -> d.getEgasiTelegramId() == null)
                .toList();
        Dorixona aniq = topilgan.stream()
                .filter(d -> d.getNomi().equalsIgnoreCase(text))
                .findFirst()
                .orElse(topilgan.size() == 1 ? topilgan.get(0) : null);

        if (aniq == null) {
            if (topilgan.isEmpty()) {
                sender.text(chatId, ru
                        ? "❌ Такая аптека не найдена среди ожидающих подключения.\n\nПроверьте название или обратитесь к админу: @Anvarovich_2bot"
                        : "❌ Bunday dorixona ulanishni kutayotganlar orasida topilmadi.\n\nNomini tekshiring yoki adminga murojaat qiling: @Anvarovich_2bot",
                        Keyboards.cancelMenu(lang));
                return;
            }
            sender.text(chatId, ru ? "🔎 Найдено несколько. Выберите свою:" : "🔎 Bir nechta topildi. O'zingiznikini tanlang:",
                    Keyboards.pharmacyMenu(lang, topilgan));
            return;
        }

        session.setDorixonaId(aniq.getId());
        session.setDorixonaNomi(aniq.getNomi());
        session.setTekshiruvKodi(kodYarat());
        session.setQadam(1);

        sender.text(chatId, (ru
                ? "🏥 Аптека: <b>" + esc(aniq.getNomi()) + "</b>\n\n"
                  + "Теперь подтвердим, что аптека действительно ваша. Три шага:\n"
                  + "1️⃣ номер телефона\n2️⃣ фото лицензии\n3️⃣ живое фото аптеки с кодом\n\n"
                  + "<b>Шаг 1.</b> Отправьте свой номер кнопкой ниже."
                : "🏥 Dorixona: <b>" + esc(aniq.getNomi()) + "</b>\n\n"
                  + "Endi dorixona haqiqatan sizniki ekanini tasdiqlaymiz. Uch qadam:\n"
                  + "1️⃣ telefon raqami\n2️⃣ litsenziya surati\n3️⃣ kod bilan dorixonaning jonli surati\n\n"
                  + "<b>1-qadam.</b> Raqamingizni pastdagi tugma orqali yuboring."),
                Keyboards.phoneRequest(lang));
    }

    private void arizaKontakt(long chatId, long userId, String lang, Session session, Contact contact, User from) {
        boolean ru = Texts.ru(lang);
        if (session.getQadam() != 1) return;

        // Faqat o'z raqamini yuborishi mumkin — boshqa odamning kontaktini uzatib bo'lmaydi.
        if (contact.getUserId() != null && from != null && !contact.getUserId().equals(from.getId())) {
            sender.text(chatId, ru ? "⚠️ Отправьте, пожалуйста, свой собственный номер."
                    : "⚠️ Iltimos, o'zingizning raqamingizni yuboring.", Keyboards.phoneRequest(lang));
            return;
        }

        session.setTelefon(contact.getPhoneNumber());
        owners.telefonniSaqla(userId, contact.getPhoneNumber());
        session.setQadam(2);

        sender.text(chatId, ru
                ? "✅ Номер принят.\n\n<b>Шаг 2.</b> Отправьте <b>фото лицензии или свидетельства</b> аптеки одним снимком."
                : "✅ Raqam qabul qilindi.\n\n<b>2-qadam.</b> Dorixonaning <b>litsenziyasi yoki guvohnomasi</b> suratini bitta rasm qilib yuboring.",
                Keyboards.cancelMenu(lang));
    }

    private void arizaRasm(long chatId, long userId, String lang, Session session, List<PhotoSize> photos, User from) {
        boolean ru = Texts.ru(lang);
        String fileId = engKatta(photos);
        if (fileId == null) return;

        if (session.getQadam() == 2) {
            session.setLitsenziyaFileId(fileId);
            session.setQadam(3);
            sender.text(chatId, ru
                    ? "✅ Лицензия принята.\n\n<b>Шаг 3 — самый важный.</b>\n\n"
                      + "Напишите на листе бумаги код:\n\n<code>" + session.getTekshiruvKodi() + "</code>\n\n"
                      + "и сфотографируйте <b>вывеску или прилавок вашей аптеки вместе с этим листом</b>.\n\n"
                      + "Так мы видим, что вы действительно находитесь в аптеке прямо сейчас — "
                      + "старое фото или картинка из интернета не подойдёт."
                    : "✅ Litsenziya qabul qilindi.\n\n<b>3-qadam — eng muhimi.</b>\n\n"
                      + "Qog'ozga shu kodni yozing:\n\n<code>" + session.getTekshiruvKodi() + "</code>\n\n"
                      + "va <b>dorixonangizning peshtaxtasi yoki peshlavhasini o'sha qog'oz bilan birga</b> suratga oling.\n\n"
                      + "Shunda siz haqiqatan hozir dorixonada turganingiz ko'rinadi — "
                      + "eski surat yoki internetdagi rasm ish bermaydi.",
                    Keyboards.cancelMenu(lang));
            return;
        }

        if (session.getQadam() != 3) return;

        Soov soov = new Soov();
        soov.setDorixonaId(session.getDorixonaId());
        soov.setTelegramId(userId);
        soov.setIsm(from == null ? null : fullName(from));
        soov.setUsername(from == null ? null : from.getUserName());
        soov.setTelefon(session.getTelefon());
        soov.setLitsenziyaFileId(session.getLitsenziyaFileId());
        soov.setJonliRasmFileId(fileId);
        // Rasmlarni baytlar bilan saqlaymiz: file_id faqat shu botda ishlaydi, mijozlar
        // boti esa o'sha id bilan dalil suratlarini ko'rsata olmaydi.
        soov.setLitsenziyaRasm(sender.download(session.getLitsenziyaFileId()));
        soov.setJonliRasm(sender.download(fileId));
        soov.setTekshiruvKodi(session.getTekshiruvKodi());
        long soovId = soovlar.add(soov);
        soov.setId(soovId);
        soov.setDorixonaNomi(session.getDorixonaNomi());
        sessions.remove(userId);

        sender.text(chatId, ru
                ? "📨 <b>Заявка отправлена!</b>\n\nАдмин проверит её и вы получите ответ здесь же.\n\n"
                  + "🏥 Аптека: <b>" + esc(session.getDorixonaNomi()) + "</b>"
                : "📨 <b>Ariza yuborildi!</b>\n\nAdmin uni tekshiradi va javobni shu yerda olasiz.\n\n"
                  + "🏥 Dorixona: <b>" + esc(session.getDorixonaNomi()) + "</b>",
                Keyboards.guestMenu(lang));

        for (Long adminId : AdminPanel.all()) {
            arizaniAdminga(adminId, soov, "uz");
        }
    }

    /** Arizani admin ko'radigan ko'rinishda yuboradi: ma'lumot + ikkala dalil surati + tugmalar. */
    private void arizaniAdminga(long adminChatId, Soov soov, String lang) {
        String matn = "📨 <b>Egalik arizasi №" + soov.getId() + "</b>\n\n"
                + "🏥 Dorixona: <b>" + esc(soov.getDorixonaNomi()) + "</b>\n"
                + "👤 Ism: " + esc(valueOr(soov.getIsm(), "-")) + "\n"
                + "🔗 Username: " + (soov.getUsername() == null ? "-" : "@" + esc(soov.getUsername())) + "\n"
                + "🆔 Telegram ID: <code>" + soov.getTelegramId() + "</code>\n"
                + "☎️ Telefon: <code>" + esc(valueOr(soov.getTelefon(), "-")) + "</code>\n"
                + "🔑 Tekshiruv kodi: <code>" + esc(valueOr(soov.getTekshiruvKodi(), "-")) + "</code>\n\n"
                + "Quyidagi ikkinchi suratda aynan shu kod yozilgan qog'oz ko'rinishi kerak.";
        sender.text(adminChatId, matn);
        if (soov.getLitsenziyaFileId() != null) {
            sender.photo(adminChatId, soov.getLitsenziyaFileId(), "📄 Litsenziya / guvohnoma", null);
        }
        if (soov.getJonliRasmFileId() != null) {
            sender.photo(adminChatId, soov.getJonliRasmFileId(),
                    "📷 Jonli surat — kod: <b>" + esc(valueOr(soov.getTekshiruvKodi(), "-")) + "</b>",
                    Keyboards.claimDecision(soov.getId(), lang));
        }
    }

    // ————————————————— Ko'p bosqichli jarayonlar —————————————————

    private void sessionQadami(long chatId, long userId, String lang, Session session, String text) {
        switch (session.getTuri()) {
            case EGALIK_ARIZASI -> arizaQadami(chatId, userId, lang, session, text);
            case DORIXONA_QOSHISH -> dorixonaQoshishQadami(chatId, userId, lang, session, text);
            case OBUNANI_UZAYTIRISH -> obunaUzaytirishQadami(chatId, userId, lang, session, text);
            case MAHSULOT_QOSHISH -> mahsulotQoshishQadami(chatId, userId, lang, session, text);
            case KIRIM, CHIQIM -> omborQadami(chatId, userId, lang, session, text);
            case OCHIRISH -> ochirishQadami(chatId, userId, lang, session, text);
        }
    }

    private void dorixonaQoshishQadami(long chatId, long userId, String lang, Session session, String text) {
        boolean ru = Texts.ru(lang);
        switch (session.getQadam()) {
            case 0 -> {
                if (text.isBlank()) {
                    sender.text(chatId, Texts.t(lang, "sendText"), Keyboards.cancelMenu(lang));
                    return;
                }
                session.setNomi(text);
                session.setQadam(1);
                sender.text(chatId, ru ? "📍 <b>Адрес аптеки:</b>" : "📍 <b>Dorixona manzili:</b>",
                        Keyboards.skipCancelMenu(lang));
            }
            case 1 -> {
                if (!Keyboards.isSkip(text)) session.setManzil(text);
                session.setQadam(2);
                sender.text(chatId, ru ? "☎️ <b>Телефон аптеки:</b>" : "☎️ <b>Dorixona telefoni:</b>",
                        Keyboards.skipCancelMenu(lang));
            }
            case 2 -> {
                if (!Keyboards.isSkip(text)) session.setTelefon(text);
                session.setQadam(3);
                sender.text(chatId, ru ? "🗓 <b>Срок подписки:</b>" : "🗓 <b>Obuna muddati:</b>",
                        Keyboards.monthsMenu(lang));
            }
            case 3 -> {
                int oylar = Keyboards.months(text);
                if (oylar == 0) {
                    sender.text(chatId, ru ? "❓ Выберите срок кнопкой: 1, 3, 6, 9 или 12 месяцев."
                            : "❓ Muddatni tugma orqali tanlang: 1, 3, 6, 9 yoki 12 oy.", Keyboards.monthsMenu(lang));
                    return;
                }
                Dorixona d = new Dorixona();
                d.setNomi(session.getNomi());
                d.setManzil(session.getManzil());
                d.setTelefon(session.getTelefon());
                d.setObunaBoshlanish(LocalDate.now());
                d.setObunaTugash(LocalDate.now().plusMonths(oylar));
                d.setHolat("faol");
                long id = pharmacies.add(d);
                sessions.remove(userId);

                sender.text(chatId, (ru ? "✅ <b>Аптека добавлена!</b>\n\n" : "✅ <b>Dorixona qo'shildi!</b>\n\n")
                        + "🏥 <b>" + esc(d.getNomi()) + "</b>\n"
                        + (d.getManzil() == null ? "" : "📍 " + esc(d.getManzil()) + "\n")
                        + (d.getTelefon() == null ? "" : "☎️ " + esc(d.getTelefon()) + "\n")
                        + "🗓 " + (ru ? "Подписка до " : "Obuna ") + d.getObunaTugash().format(SANA)
                        + (ru ? "" : " gacha") + " (" + oylar + (ru ? " мес.)" : " oy)") + "\n"
                        + "🆔 " + id + "\n\n"
                        + (ru ? "Теперь владелец может подключить её в боте кнопкой «Подключить мою аптеку»."
                              : "Endi egasi botda «Dorixonamni ulash» tugmasi orqali uni ulashi mumkin."),
                        Keyboards.adminMenu(lang));
            }
            default -> sessions.remove(userId);
        }
    }

    private void obunaUzaytirishQadami(long chatId, long userId, String lang, Session session, String text) {
        boolean ru = Texts.ru(lang);
        int oylar = Keyboards.months(text);
        if (oylar == 0) {
            sender.text(chatId, ru ? "❓ Выберите срок: 1, 3, 6, 9 или 12 месяцев."
                    : "❓ Muddatni tanlang: 1, 3, 6, 9 yoki 12 oy.", Keyboards.monthsMenu(lang));
            return;
        }
        LocalDate yangi = pharmacies.obunaniUzaytir(session.getDorixonaId(), oylar);
        sessions.remove(userId);
        if (yangi == null) {
            sender.text(chatId, ru ? "❌ Аптека не найдена." : "❌ Dorixona topilmadi.", Keyboards.adminMenu(lang));
            return;
        }
        sender.text(chatId, (ru ? "✅ Подписка продлена до " : "✅ Obuna ") + yangi.format(SANA)
                + (ru ? "" : " gacha uzaytirildi"), Keyboards.adminMenu(lang));

        Dorixona d = pharmacies.getById(session.getDorixonaId());
        if (d != null && d.getEgasiTelegramId() != null) {
            String egaTili = owners.til(d.getEgasiTelegramId());
            sender.text(d.getEgasiTelegramId(), Texts.ru(egaTili)
                    ? "🎉 Ваша подписка продлена до <b>" + yangi.format(SANA) + "</b>."
                    : "🎉 Obunangiz <b>" + yangi.format(SANA) + "</b> gacha uzaytirildi.",
                    Keyboards.ownerMenu(egaTili));
        }
    }

    private void mahsulotQoshishQadami(long chatId, long userId, String lang, Session session, String text) {
        boolean ru = Texts.ru(lang);
        switch (session.getQadam()) {
            case 0 -> {
                if (Keyboards.isAddManually(text)) {
                    session.setFayldan(false);
                    session.setQadam(1);
                    sender.text(chatId, ru
                            ? "📦 <b>Название товара:</b>\n\nНапример: Парацетамол, Бинт стерильный"
                            : "📦 <b>Mahsulot nomi:</b>\n\nMasalan: Paratsetamol, Bint steril", Keyboards.cancelMenu(lang));
                } else if (Keyboards.isAddFromFile(text)) {
                    session.setFayldan(true);
                    session.setQadam(1);
                    sender.text(chatId, ru
                            ? "📄 <b>Отправьте файл</b> (.xlsx, .csv или .txt)\n\n"
                              + "Формат: 3 столбца — <b>Название, Цена, Количество</b>\n\n"
                              + "Пример (CSV):\n<code>Парацетамол;12000;50\nБинт стерильный;8000;30</code>"
                            : "📄 <b>Fayl yuboring</b> (.xlsx, .csv yoki .txt)\n\n"
                              + "Format: 3 ta ustun — <b>Nomi, Narxi, Soni</b>\n\n"
                              + "Misol (CSV):\n<code>Paratsetamol;12000;50\nBint steril;8000;30</code>",
                            Keyboards.cancelMenu(lang));
                } else {
                    sender.text(chatId, ru ? "⬇️ Выберите способ из меню ниже."
                            : "⬇️ Quyidagi menyudan usulni tanlang.", Keyboards.addMethodMenu(lang));
                }
            }
            case 1 -> {
                if (session.isFayldan()) {
                    sender.text(chatId, ru ? "📄 Отправьте файл (.xlsx, .csv или .txt)."
                            : "📄 Fayl yuboring (.xlsx, .csv yoki .txt).", Keyboards.cancelMenu(lang));
                    return;
                }
                if (text.isBlank()) {
                    sender.text(chatId, Texts.t(lang, "sendText"), Keyboards.cancelMenu(lang));
                    return;
                }
                if (drugs.nomiBoyicha(session.getDorixonaId(), text) != null) {
                    sender.text(chatId, ru ? "⚠️ Такой товар уже есть. Введите другое название."
                            : "⚠️ Bunday mahsulot allaqachon bor. Boshqa nom kiriting.", Keyboards.cancelMenu(lang));
                    return;
                }
                session.setNomi(text);
                session.setQadam(2);
                sender.text(chatId, ru ? "💵 <b>Цена (сум):</b>" : "💵 <b>Narxi (so'm):</b>", Keyboards.cancelMenu(lang));
            }
            case 2 -> {
                Double narx = narxOqi(text);
                if (narx == null) {
                    sender.text(chatId, ru ? "❌ Неверная цена. Введите число, например 12000."
                            : "❌ Narx noto'g'ri. Raqam kiriting, masalan 12000.", Keyboards.cancelMenu(lang));
                    return;
                }
                session.setNarx(narx);
                session.setQadam(3);
                sender.text(chatId, ru
                        ? "🔢 <b>Сколько штук есть сейчас?</b>\n\nЭто будет первым приходом. Если не ведёте учёт — «Пропустить»."
                        : "🔢 <b>Hozir nechta bor?</b>\n\nBu birinchi kirim bo'ladi. Hisob yuritmasangiz — «O'tkazib yuborish».",
                        Keyboards.skipCancelMenu(lang));
            }
            case 3 -> {
                Integer soni = Keyboards.isSkip(text) ? 0 : sonOqi(text);
                if (soni == null) {
                    sender.text(chatId, ru ? "❌ Неверное количество. Введите целое число."
                            : "❌ Son noto'g'ri. Butun raqam kiriting.", Keyboards.skipCancelMenu(lang));
                    return;
                }
                Dori dori = new Dori();
                dori.setNomi(session.getNomi());
                dori.setNarx(session.getNarx());
                dori.setDorixonaId(session.getDorixonaId());
                dori.setMavjud(true);
                long doriId = drugs.add(dori);
                if (soni > 0) stock.kirim(doriId, soni, "dorixona boti: birinchi kirim");
                sessions.remove(userId);

                sender.text(chatId, (ru ? "✅ <b>Товар добавлен!</b>\n\n" : "✅ <b>Mahsulot qo'shildi!</b>\n\n")
                        + "📦 <b>" + esc(dori.getNomi()) + "</b>\n"
                        + "💵 " + son(dori.getNarx()) + (ru ? " сум\n" : " so'm\n")
                        + (soni > 0 ? (ru ? "📊 Остаток: <b>" : "📊 Qoldiq: <b>") + soni + (ru ? "</b> шт.\n" : "</b> ta\n") : "")
                        + "\n" + (ru ? "Товар уже виден покупателям в боте поиска и на сайте."
                                     : "Mahsulot xaridorlarga qidiruv botida va saytda darhol ko'rinadi."),
                        egaMenyusi(chatId, lang));
            }
            default -> sessions.remove(userId);
        }
    }

    private void fayldanImport(long chatId, long userId, String lang, Session session, String fileId) {
        boolean ru = Texts.ru(lang);
        byte[] data = sender.download(fileId);
        if (data == null) {
            sender.text(chatId, ru ? "❌ Не удалось загрузить файл. Попробуйте ещё раз."
                    : "❌ Faylni yuklab bo'lmadi. Qaytadan urinib ko'ring.", Keyboards.cancelMenu(lang));
            return;
        }

        List<String[]> rows;
        try {
            rows = faylniOqi(data);
        } catch (Exception e) {
            sender.text(chatId, ru ? "❌ Файл не распознан. Отправьте .txt, .csv или .xlsx файл."
                    : "❌ Fayl tanilmadi. .txt, .csv yoki .xlsx fayl yuboring.", Keyboards.cancelMenu(lang));
            return;
        }

        // Dorixonada bor nomlar bir marta o'qiladi: faylni qayta yuborsa ham dublikat qo'shilmaydi.
        java.util.Set<String> mavjud = drugs.nomlar(session.getDorixonaId());
        int added = 0, skipped = 0, dublikat = 0;
        StringBuilder result = new StringBuilder();
        for (String[] parts : rows) {
            if (parts.length < 2) { skipped++; continue; }
            String nomi = tozaNom(parts[0]);
            if (nomi.isEmpty()) { skipped++; continue; }
            String nomiLow = nomi.toLowerCase();
            if (nomiLow.startsWith("nom") || nomiLow.startsWith("назв") || nomiLow.startsWith("наим") || nomiLow.equals("name")) continue;
            Double narx = narxOqi(parts[1].trim());
            if (narx == null) { skipped++; continue; }
            int soni = 0;
            if (parts.length >= 3) {
                Integer s = sonOqi(parts[2].trim());
                if (s != null) soni = s;
            }
            if (!mavjud.add(nomiLow)) { dublikat++; continue; }
            Dori dori = new Dori();
            dori.setNomi(nomi);
            dori.setNarx(narx);
            dori.setDorixonaId(session.getDorixonaId());
            dori.setMavjud(true);
            long doriId = drugs.add(dori);
            if (soni > 0) stock.kirim(doriId, soni, "dorixona boti: fayldan import");
            added++;
            // Telegram xabari 4096 belgidan oshmasin — to'liq ro'yxat pastdagi tugmalarda.
            if (added <= 40 && result.length() < 3000) {
                result.append(added).append(". ").append(esc(qisqa(nomi, 50)))
                        .append(" — <b>").append(son(narx)).append(ru ? " сум" : " so'm").append("</b>");
                if (soni > 0) result.append(" · ").append(soni).append(ru ? " шт." : " ta");
                result.append('\n');
            }
        }
        sessions.remove(userId);
        if (added > 40 || (added > 0 && result.length() >= 3000)) {
            long korsatilgan = result.chars().filter(c -> c == '\n').count();
            result.append(ru ? "… и ещё <b>" : "… va yana <b>").append(added - korsatilgan).append(ru ? "</b> шт.\n" : "</b> ta\n");
        }
        if (added == 0) {
            sender.text(chatId, ru
                    ? "❌ Ни один товар не добавлен.\n\n"
                      + (dublikat > 0 ? "Уже были в аптеке: " + dublikat + "\n" : "")
                      + (skipped > 0 ? "Пропущено строк (ошибка формата): " + skipped + "\n" : "")
                      + "\nФормат:\n<code>Название;Цена;Количество</code>"
                    : "❌ Birorta ham mahsulot qo'shilmadi.\n\n"
                      + (dublikat > 0 ? "Dorixonada allaqachon bor edi: " + dublikat + "\n" : "")
                      + (skipped > 0 ? "O'tkazilgan qatorlar (format xatosi): " + skipped + "\n" : "")
                      + "\nFormat:\n<code>Nomi;Narxi;Soni</code>",
                    egaMenyusi(chatId, lang));
            return;
        }
        sender.text(chatId, (ru ? "✅ <b>Импорт завершён</b>\n\n➕ Добавлено: <b>" : "✅ <b>Import yakunlandi</b>\n\n➕ Qo'shildi: <b>")
                        + added + "</b>"
                        + (dublikat > 0 ? (ru ? "\n♻️ Уже были: <b>" : "\n♻️ Allaqachon bor edi: <b>") + dublikat + "</b>" : "")
                        + (skipped > 0 ? (ru ? "\n⚠️ Пропущено: <b>" : "\n⚠️ O'tkazildi: <b>") + skipped + "</b>" : "")
                        + (ru ? "\n\n💊 <b>Добавленные товары:</b>\n" : "\n\n💊 <b>Qo'shilgan mahsulotlar:</b>\n") + result,
                egaMenyusi(chatId, lang));
        // Har bir mahsulot — alohida tugma: bosib ochish yoki o'chirish mumkin.
        Dorixona d = pharmacies.getById(session.getDorixonaId());
        if (d != null) mahsulotlar(chatId, d, lang);
    }

    private List<String[]> faylniOqi(byte[] data) throws Exception {
        // Excel (.xlsx/.xls) fayl yoki matnli fayl (.csv/.txt) ekanligini aniqlash
        if (data.length >= 4 && data[0] == 0x50 && data[1] == 0x4B) {
            // ZIP/XLSX format
            return excelOqi(data);
        }
        if (data.length >= 8 && data[0] == (byte) 0xD0 && data[1] == (byte) 0xCF) {
            // OLE2/XLS format
            return excelOqi(data);
        }
        // Matnli fayl (CSV/TXT)
        String content = new String(data, java.nio.charset.StandardCharsets.UTF_8);
        String[] lines = content.split("\\r?\\n");
        List<String[]> rows = new ArrayList<>();
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            // Ajratuvchi: «;» yoki tab bo'lsa o'sha, aks holda vergul. Avval hammasi birga
            // ishlatilardi — "12,5" kabi narx yoki nomidagi vergul qatorni buzib yuborardi.
            String delim = line.contains(";") ? ";" : line.contains("\t") ? "\t" : ",";
            rows.add(line.split(delim, -1));
        }
        return rows;
    }

    private List<String[]> excelOqi(byte[] data) throws Exception {
        List<String[]> rows = new ArrayList<>();
        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(data))) {
            Sheet sheet = wb.getSheetAt(0);
            for (Row row : sheet) {
                String[] cells = new String[Math.max(3, row.getLastCellNum())];
                for (int i = 0; i < cells.length; i++) {
                    Cell cell = row.getCell(i);
                    cells[i] = cell == null ? "" : cellText(cell);
                }
                boolean empty = true;
                for (String c : cells) if (c != null && !c.isBlank()) { empty = false; break; }
                if (!empty) rows.add(cells);
            }
        }
        return rows;
    }

    private String cellText(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                double v = cell.getNumericCellValue();
                yield v == Math.floor(v) ? String.valueOf((long) v) : String.valueOf(v);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> switch (cell.getCachedFormulaResultType()) {
                // Formula natijasi ham oddiy son kabi: "50.0" emas, "50" bo'lishi kerak.
                case NUMERIC -> {
                    double v = cell.getNumericCellValue();
                    yield v == Math.floor(v) ? String.valueOf((long) v) : String.valueOf(v);
                }
                case STRING -> cell.getStringCellValue().trim();
                case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
                default -> "";
            };
            default -> "";
        };
    }

    private void omborQadami(long chatId, long userId, String lang, Session session, String text) {
        boolean ru = Texts.ru(lang);
        boolean kirim = session.getTuri() == Session.Turi.KIRIM;

        if (session.getQadam() == 0) {
            Dori dori = drugs.nomiBoyicha(session.getDorixonaId(), text);
            if (dori == null) {
                String q = text.toLowerCase();
                List<Dori> topilgan = drugs.dorixonaniki(session.getDorixonaId()).stream()
                        .filter(d -> d.getNomi() != null && d.getNomi().toLowerCase().contains(q)).toList();
                if (topilgan.size() == 1) {
                    dori = topilgan.get(0);
                } else {
                    sender.inlineText(chatId, topilgan.isEmpty()
                                    ? (ru ? "❌ Товар не найден. Выберите из списка:" : "❌ Mahsulot topilmadi. Ro'yxatdan tanlang:")
                                    : (ru ? "🔎 Найдено несколько. Выберите:" : "🔎 Bir nechta topildi. Tanlang:"),
                            omborTanlov(topilgan.isEmpty() ? drugs.dorixonaniki(session.getDorixonaId()) : topilgan, kirim, 0));
                    return;
                }
            }
            omborMahsulotTanlandi(chatId, lang, session, dori);
            return;
        }
        omborSoni(chatId, userId, lang, session, text);
    }

    // ————————————————— Mahsulot o'chirish (pastki klaviatura) —————————————————

    private static final int OCHIRISH_SAHIFA = 30;

    private void ochirishQadami(long chatId, long userId, String lang, Session session, String text) {
        boolean ru = Texts.ru(lang);
        if (session.getQadam() == 1) {
            if (Keyboards.isYesDelete(text)) {
                boolean ochdi = drugs.ochir(session.getDoriId(), session.getDorixonaId());
                session.setQadam(0);
                String natija = ochdi
                        ? (ru ? "🗑 «" + esc(session.getDoriNomi()) + "» удалён." : "🗑 «" + esc(session.getDoriNomi()) + "» o'chirildi.")
                        : (ru ? "❌ Не удалось удалить (возможно, уже удалён)." : "❌ O'chirib bo'lmadi (balki allaqachon o'chirilgan).");
                if (drugs.dorixonaniki(session.getDorixonaId()).isEmpty()) {
                    sessions.remove(userId);
                    sender.text(chatId, natija + "\n\n" + (ru ? "📦 Товаров больше нет." : "📦 Boshqa mahsulot qolmadi."), egaMenyusi(chatId, lang));
                    return;
                }
                ochirishRoyxati(chatId, lang, session, natija);
                return;
            }
            session.setQadam(0);
            ochirishRoyxati(chatId, lang, session, Keyboards.isNo(text) ? Texts.t(lang, "cancelled") : null);
            return;
        }

        if (Keyboards.isNextPage(text)) { session.setSahifa(session.getSahifa() + 1); ochirishRoyxati(chatId, lang, session, null); return; }
        if (Keyboards.isPrevPage(text)) { session.setSahifa(session.getSahifa() - 1); ochirishRoyxati(chatId, lang, session, null); return; }
        if (Keyboards.isPageLabel(text) || text.isBlank()) { ochirishRoyxati(chatId, lang, session, null); return; }
        if (Keyboards.isShowAll(text)) {
            session.setFilter(null);
            session.setSahifa(0);
            ochirishRoyxati(chatId, lang, session, null);
            return;
        }

        Dori dori = drugs.nomiBoyicha(session.getDorixonaId(), text);
        if (dori != null) {
            session.setDoriId(dori.getId());
            session.setDoriNomi(dori.getNomi());
            session.setQadam(1);
            sender.text(chatId, (ru ? "🗑 <b>Удалить товар?</b>\n\n" : "🗑 <b>Mahsulotni o'chirasizmi?</b>\n\n")
                            + mahsulotKartasi(dori, ru) + "\n\n"
                            + (ru ? "⚠️ Его складские записи и брони тоже удалятся."
                                  : "⚠️ Uning ombor yozuvlari va bronlari ham o'chadi."),
                    Keyboards.confirmDeleteMenu(lang));
            return;
        }

        // Nomi to'liq mos kelmasa — qidiruv.
        String q = text.toLowerCase();
        boolean bor = drugs.dorixonaniki(session.getDorixonaId()).stream()
                .anyMatch(d -> d.getNomi() != null && d.getNomi().toLowerCase().contains(q));
        if (!bor) {
            ochirishRoyxati(chatId, lang, session, ru ? "🔎 По запросу «<b>" + esc(text) + "</b>» ничего не найдено."
                                                      : "🔎 «<b>" + esc(text) + "</b>» bo'yicha hech narsa topilmadi.");
            return;
        }
        session.setFilter(text);
        session.setSahifa(0);
        ochirishRoyxati(chatId, lang, session, null);
    }

    /** O'chirish uchun mahsulotlar — pastki klaviaturada, har biri alohida tugma, sahifalab. */
    private void ochirishRoyxati(long chatId, String lang, Session session, String oldMatn) {
        boolean ru = Texts.ru(lang);
        String f = session.getFilter() == null ? null : session.getFilter().toLowerCase();
        List<String> nomlar = drugs.dorixonaniki(session.getDorixonaId()).stream()
                .map(Dori::getNomi)
                .filter(n -> n != null && (f == null || n.toLowerCase().contains(f)))
                .toList();
        int pages = Math.max(1, (nomlar.size() + OCHIRISH_SAHIFA - 1) / OCHIRISH_SAHIFA);
        int page = Math.max(0, Math.min(session.getSahifa(), pages - 1));
        session.setSahifa(page);
        List<String> sahifa = nomlar.subList(page * OCHIRISH_SAHIFA, Math.min(nomlar.size(), (page + 1) * OCHIRISH_SAHIFA));
        String matn = (oldMatn == null ? "" : oldMatn + "\n\n")
                + (session.getFilter() == null
                    ? (ru ? "🗑 <b>Удаление товара</b>\n\nВсего: <b>" : "🗑 <b>Mahsulot o'chirish</b>\n\nJami: <b>") + nomlar.size() + (ru ? "</b> шт." : "</b> ta")
                    : "🔎 «<b>" + esc(session.getFilter()) + "</b>» — " + nomlar.size() + (ru ? " шт." : " ta"))
                + (pages > 1 ? (ru ? "  ·  стр. " : "  ·  ") + (page + 1) + "/" + pages + (ru ? "" : "-sahifa") : "")
                + "\n\n" + (ru ? "👇 Выберите товар кнопкой ниже или напишите название — я найду."
                               : "👇 Pastdagi tugmalardan mahsulotni tanlang yoki nomini yozing — qidirib beraman.");
        sender.text(chatId, matn, Keyboards.pagedMenu(lang, sahifa, page, pages, session.getFilter() != null));
    }

    /** Kirim/chiqim uchun mahsulot tanlov tugmalari (sahifali). */
    private InlineKeyboardMarkup omborTanlov(List<Dori> royxat, boolean kirim, int page) {
        int pages = Math.max(1, (royxat.size() + SAHIFA - 1) / SAHIFA);
        page = Math.max(0, Math.min(page, pages - 1));
        List<String[]> items = new ArrayList<>();
        for (Dori d : royxat.subList(Math.min(royxat.size(), page * SAHIFA), Math.min(royxat.size(), (page + 1) * SAHIFA))) {
            items.add(new String[]{"📦 " + qisqa(d.getNomi(), 40) + (d.hisobYuritiladi() ? " · " + d.getQoldiq() : ""),
                    "om:" + (kirim ? "k" : "c") + ":" + d.getId()});
        }
        return Keyboards.inlineList(items, page, pages, "om:" + (kirim ? "pk" : "pc") + ":");
    }

    private void omborMahsulotTanlandi(long chatId, String lang, Session session, Dori dori) {
        boolean ru = Texts.ru(lang);
        boolean kirim = session.getTuri() == Session.Turi.KIRIM;
        session.setDoriId(dori.getId());
        session.setDoriNomi(dori.getNomi());
        session.setQadam(1);
        sender.text(chatId, (kirim
                            ? (ru ? "📥 <b>Сколько поступило?</b>" : "📥 <b>Nechta keldi?</b>")
                            : (ru ? "📤 <b>Сколько продано?</b>" : "📤 <b>Nechta sotildi?</b>"))
                            + "\n\n📦 " + esc(dori.getNomi()) + "\n"
                            + (ru ? "Сейчас на складе: <b>" : "Hozir omborda: <b>") + dori.getQoldiq()
                            + (ru ? "</b> шт." : "</b> ta"),
                    Keyboards.cancelMenu(lang));
    }

    private void omborSoni(long chatId, long userId, String lang, Session session, String text) {
        boolean ru = Texts.ru(lang);
        boolean kirim = session.getTuri() == Session.Turi.KIRIM;
        Integer soni = sonOqi(text);
        if (soni == null || soni <= 0) {
            sender.text(chatId, ru ? "❌ Неверное количество. Введите целое число, например 50."
                    : "❌ Son noto'g'ri. Butun raqam kiriting, masalan 50.", Keyboards.cancelMenu(lang));
            return;
        }

        int yozildi = kirim
                ? stock.kirim(session.getDoriId(), soni, "dorixona boti")
                : stock.chiqim(session.getDoriId(), soni, "dorixona boti");
        if (yozildi == 0) {
            int qoldiq = stock.qoldiq(session.getDoriId());
            sender.text(chatId, (ru ? "⚠️ На складе недостаточно. Остаток: <b>" : "⚠️ Omborda yetarli emas. Qoldiq: <b>")
                    + qoldiq + (ru ? "</b> шт.\nВведите другое количество:" : "</b> ta\nBoshqa son kiriting:"),
                    Keyboards.cancelMenu(lang));
            return;
        }

        int qoldiq = stock.qoldiq(session.getDoriId());
        sessions.remove(userId);
        sender.text(chatId, (kirim
                        ? (ru ? "✅ <b>Приход записан!</b>\n\n" : "✅ <b>Kirim yozildi!</b>\n\n")
                        : (ru ? "✅ <b>Продажа записана!</b>\n\n" : "✅ <b>Sotuv yozildi!</b>\n\n"))
                        + "📦 " + esc(session.getDoriNomi()) + "\n"
                        + (kirim ? "📥 " : "📤 ") + soni + (ru ? " шт.\n" : " ta\n")
                        + (ru ? "📊 Остаток: <b>" : "📊 Qoldiq: <b>") + qoldiq + (ru ? "</b> шт." : "</b> ta"),
                egaMenyusi(chatId, lang));
    }

    // ————————————————— Inline tugmalar —————————————————

    public String handleCallback(CallbackQuery query) {
        String data = query.getData();
        long userId = query.getFrom().getId();
        long chatId = query.getMessage().getChatId();
        String lang = owners.til(userId);
        if (data == null) return null;

        String[] parts = data.split(":");
        if (parts.length < 3 || "noop".equals(parts[0])) return null;
        long id;
        try {
            id = Long.parseLong(parts[2]);
        } catch (NumberFormatException e) {
            return null;
        }
        Integer messageId = query.getMessage() == null ? null : query.getMessage().getMessageId();

        if ("soov".equals(parts[0])) {
            if (!AdminPanel.isAdmin(userId)) return "Ruxsat yo'q";
            return arizaQarori(chatId, id, "ok".equals(parts[1]));
        }
        if ("dx".equals(parts[0])) {
            if (!AdminPanel.isAdmin(userId)) return "Ruxsat yo'q";
            return dorixonaAmali(chatId, userId, lang, id, parts[1]);
        }
        if ("bron".equals(parts[0])) {
            return bronQarori(chatId, userId, lang, id, parts[1]);
        }
        if ("yetk".equals(parts[0])) {
            return yetkazishQarori(chatId, userId, lang, id, parts[1]);
        }
        if ("dori".equals(parts[0])) {
            int page = 0;
            if (parts.length >= 4) {
                try { page = Integer.parseInt(parts[3]); } catch (NumberFormatException ignored) { }
            }
            return doriAmali(chatId, messageId, userId, lang, id, parts[1], page);
        }
        if ("om".equals(parts[0])) {
            return omborTugmasi(chatId, messageId, userId, lang, parts[1], id);
        }
        return null;
    }

    /** Kirim/chiqim: mahsulot tanlandi yoki sahifa almashtirildi. */
    private String omborTugmasi(long chatId, Integer messageId, long userId, String lang, String amal, long id) {
        boolean ru = Texts.ru(lang);
        Session session = sessions.get(userId);
        boolean kirim = amal.equals("k") || amal.equals("pk");
        Session.Turi kerak = kirim ? Session.Turi.KIRIM : Session.Turi.CHIQIM;
        if (session == null || session.getTuri() != kerak) {
            return ru ? "Нажмите «Приход» или «Расход» ещё раз" : "«Kirim» yoki «Chiqim»ni qaytadan bosing";
        }
        if (amal.startsWith("p")) {
            if (messageId != null) {
                sender.editInline(chatId, messageId, kirim
                                ? (ru ? "📥 <b>Что поступило?</b>" : "📥 <b>Nima keldi?</b>")
                                : (ru ? "📤 <b>Что продано?</b>" : "📤 <b>Nima sotildi?</b>"),
                        omborTanlov(drugs.dorixonaniki(session.getDorixonaId()), kirim, (int) id));
            }
            return null;
        }
        Dori dori = drugs.getById(id);
        if (dori == null || dori.getDorixonaId() != session.getDorixonaId()) {
            return ru ? "Товар не найден" : "Mahsulot topilmadi";
        }
        omborMahsulotTanlandi(chatId, lang, session, dori);
        return null;
    }

    /** Mahsulot tugmalari: sahifa, kartani ochish, o'chirishni so'rash va tasdiqlash. */
    private String doriAmali(long chatId, Integer messageId, long userId, String lang, long id, String amal, int page) {
        boolean ru = Texts.ru(lang);
        Dorixona d = pharmacies.egasiBoyicha(userId);
        if (d == null) return ru ? "Аптека не найдена" : "Dorixona topilmadi";

        if ("sahifa".equals(amal)) {
            Object[] view = mahsulotlarSahifasi(d, (int) id, lang);
            korsat(chatId, messageId, (String) view[0], (InlineKeyboardMarkup) view[1]);
            return null;
        }

        Dori dori = drugs.getById(id);
        if (dori == null || dori.getDorixonaId() != d.getId()) {
            return ru ? "Товар не найден (возможно, уже удалён)" : "Mahsulot topilmadi (balki allaqachon o'chirilgan)";
        }
        switch (amal) {
            case "kor" -> {
                korsat(chatId, messageId, mahsulotKartasi(dori, ru), Keyboards.productCard(dori.getId(), page, lang));
                return null;
            }
            case "ochir" -> {
                korsat(chatId, messageId, (ru ? "🗑 <b>Удалить товар?</b>\n\n" : "🗑 <b>Mahsulotni o'chirasizmi?</b>\n\n")
                                + mahsulotKartasi(dori, ru) + "\n\n"
                                + (ru ? "⚠️ Его складские записи и брони тоже удалятся."
                                      : "⚠️ Uning ombor yozuvlari va bronlari ham o'chadi."),
                        Keyboards.confirmDelete("dori:ha:" + dori.getId() + ":" + page, "dori:kor:" + dori.getId() + ":" + page, lang));
                return null;
            }
            case "ha" -> {
                if (!drugs.ochir(dori.getId(), d.getId())) return ru ? "Не удалось удалить" : "O'chirib bo'lmadi";
                Object[] view = mahsulotlarSahifasi(d, page, lang);
                korsat(chatId, messageId, (ru ? "🗑 «" + esc(dori.getNomi()) + "» удалён.\n\n"
                                              : "🗑 «" + esc(dori.getNomi()) + "» o'chirildi.\n\n") + view[0],
                        (InlineKeyboardMarkup) view[1]);
                return ru ? "Удалён" : "O'chirildi";
            }
            default -> {
                return null;
            }
        }
    }

    /** Inline xabarni joyida yangilaydi, iloji bo'lmasa yangisini yuboradi. */
    private void korsat(long chatId, Integer messageId, String html, InlineKeyboardMarkup markup) {
        if (messageId != null) sender.editInline(chatId, messageId, html, markup);
        else sender.inlineText(chatId, html, markup);
    }

    private String dorixonaAmali(long chatId, long userId, String lang, long dorixonaId, String amal) {
        boolean ru = Texts.ru(lang);
        if ("uzaytir".equals(amal)) {
            Dorixona d = pharmacies.getById(dorixonaId);
            if (d == null) return ru ? "Аптека не найдена" : "Dorixona topilmadi";
            Session session = new Session(Session.Turi.OBUNANI_UZAYTIRISH);
            session.setDorixonaId(dorixonaId);
            session.setDorixonaNomi(d.getNomi());
            sessions.put(userId, session);
            sender.text(chatId, (ru
                    ? "🔄 <b>Продление подписки</b>\n\n🏥 " + esc(d.getNomi()) + "\n" + obunaMatni(d, lang)
                      + "\n\nВыберите срок продления:"
                    : "🔄 <b>Obunani uzaytirish</b>\n\n🏥 " + esc(d.getNomi()) + "\n" + obunaMatni(d, lang)
                      + "\n\nUzaytirish muddatini tanlang:"),
                    Keyboards.monthsMenu(lang));
            return ru ? "Выберите срок" : "Muddatni tanlang";
        }
        if ("ochir".equals(amal)) {
            // Bitta bosish bilan dorixona butunlay o'chib ketmasin — avval so'raymiz.
            Dorixona d = pharmacies.getById(dorixonaId);
            if (d == null) return ru ? "Аптека не найдена" : "Dorixona topilmadi";
            sender.inlineText(chatId, (ru ? "🗑 <b>Удалить аптеку?</b>\n\n🏥 " : "🗑 <b>Dorixonani o'chirasizmi?</b>\n\n🏥 ")
                    + esc(d.getNomi()) + "\n\n"
                    + (ru ? "⚠️ Вместе с ней удалятся все её товары, брони и заявки. Отменить нельзя."
                          : "⚠️ U bilan birga barcha mahsulotlari, bronlari va arizalari ham o'chadi. Qaytarib bo'lmaydi."),
                    Keyboards.confirmDelete("dx:ochirha:" + dorixonaId, "dx:yoq:" + dorixonaId, lang));
            return ru ? "Подтвердите удаление" : "O'chirishni tasdiqlang";
        }
        if ("yoq".equals(amal)) {
            sender.text(chatId, Texts.t(lang, "cancelled"), Keyboards.adminMenu(lang, pharmacies.egasiBoyicha(userId) != null));
            return ru ? "Отменено" : "Bekor qilindi";
        }
        if ("ochirha".equals(amal)) {
            Dorixona d = pharmacies.getById(dorixonaId);
            if (d == null) return ru ? "Аптека уже удалена" : "Dorixona allaqachon o'chirilgan";
            pharmacies.ochir(dorixonaId);
            sender.text(chatId, (ru ? "🗑 Аптека «" : "🗑 «") + esc(d.getNomi())
                    + (ru ? "» удалена." : "» o'chirildi."), Keyboards.adminMenu(lang));
            return ru ? "Удалена" : "O'chirildi";
        }
        return null;
    }

    private String arizaQarori(long adminChatId, long soovId, boolean tasdiq) {
        Soov soov = soovlar.getById(soovId);
        if (soov == null) return "Ariza topilmadi";
        if (!Soov.KUTILMOQDA.equals(soov.getHolat())) return "Bu ariza allaqachon hal qilingan";

        // Egasiga javobni bu yerda emas, kuzatuvchi yetkazadi (hal() "aytilmagan" deb
        // belgilab qo'yadi). Shunda ariza mijozlar botida hal qilinsa ham egasi bir xil
        // xabar oladi va xabar ikki marta ketmaydi.
        if (!tasdiq) {
            if (!soovlar.hal(soovId, Soov.RAD, null)) return "Bu ariza allaqachon hal qilingan";
            sender.text(adminChatId, "❌ Ariza №" + soovId + " rad etildi.");
            return "Rad etildi";
        }

        // Avval dorixonani biriktiramiz, keyin arizani "tasdiqlandi" qilamiz. Teskari tartibda
        // dorixona band bo'lib chiqsa, ariza tasdiqlangan holda qolib, egaga "tasdiqlandi"
        // xabari ketardi — aslida esa dorixona unga ulanmagan bo'lardi.
        if (!pharmacies.egasiniBiriktir(soov.getDorixonaId(), soov.getTelegramId())) {
            soovlar.hal(soovId, Soov.RAD, "Dorixona allaqachon boshqa egaga biriktirilgan");
            sender.text(adminChatId, "⚠️ Bu dorixona allaqachon boshqa egaga biriktirilgan — ariza rad etildi.");
            return "Dorixona allaqachon band";
        }
        if (!soovlar.hal(soovId, Soov.TASDIQLANGAN, null)) {
            // Shu orada boshqa admin hal qilib ulgurgan — biriktirishni qaytaramiz.
            pharmacies.egasiniBoshat(soov.getDorixonaId(), soov.getTelegramId());
            return "Bu ariza allaqachon hal qilingan";
        }

        sender.text(adminChatId, "✅ Ariza №" + soovId + " tasdiqlandi.\n\n🏥 "
                + esc(soov.getDorixonaNomi()) + " → " + soov.getTelegramId());
        return "Tasdiqlandi";
    }

    /**
     * Ariza bo'yicha qarorni dorixona egasiga yetkazadi (SoovNotifier chaqiradi).
     * Ariza qaysi botda hal qilinganidan qat'i nazar, egasi shu botda bo'lgani uchun
     * javobni doim shu bot yuboradi.
     */
    public void qarorniEgasigaYetkaz(Soov soov) {
        String lang = owners.til(soov.getTelegramId());
        boolean ru = Texts.ru(lang);

        if (Soov.TASDIQLANGAN.equals(soov.getHolat())) {
            Dorixona d = pharmacies.getById(soov.getDorixonaId());
            sender.text(soov.getTelegramId(), (ru
                    ? "🎉 <b>Заявка одобрена!</b>\n\nАптека <b>" + esc(soov.getDorixonaNomi())
                      + "</b> подключена к вашему аккаунту.\n\nТеперь вам доступен кабинет: товары, склад и брони."
                    : "🎉 <b>Ariza tasdiqlandi!</b>\n\n<b>" + esc(soov.getDorixonaNomi())
                      + "</b> dorixonasi hisobingizga ulandi.\n\nEndi kabinet ochiq: mahsulotlar, ombor va bronlar.")
                    + (d == null ? "" : "\n\n" + obunaMatni(d, lang)),
                    egaMenyusi(soov.getTelegramId(), lang));
            return;
        }

        if (soov.getAdminIzoh() != null && !soov.getAdminIzoh().isBlank()) {
            sender.text(soov.getTelegramId(), (ru ? "❌ <b>Заявка отклонена.</b>\n\nПричина: " : "❌ <b>Ariza rad etildi.</b>\n\nSabab: ")
                    + esc(soov.getAdminIzoh()) + (ru ? "\n\nСвяжитесь с админом: @Anvarovich_2bot" : "\n\nAdmin bilan bog'laning: @Anvarovich_2bot"),
                    Keyboards.guestMenu(lang));
            return;
        }
        sender.text(soov.getTelegramId(), ru
                ? "❌ <b>Заявка отклонена.</b>\n\nВозможно, документы нечитаемы или код на фото не совпал.\n"
                  + "Вы можете подать заявку заново или связаться с админом: @Anvarovich_2bot"
                : "❌ <b>Ariza rad etildi.</b>\n\nEhtimol hujjatlar o'qilmadi yoki suratdagi kod mos kelmadi.\n"
                  + "Qaytadan ariza berishingiz yoki admin bilan bog'lanishingiz mumkin: @Anvarovich_2bot",
                Keyboards.guestMenu(lang));
    }

    private String bronQarori(long chatId, long userId, String lang, long bronId, String amal) {
        Dorixona meniki = pharmacies.egasiBoyicha(userId);
        if (meniki == null) return "Sizda dorixona ulanmagan";

        Bron bron = bronlar.getById(bronId);
        if (bron == null) return "Bron topilmadi";
        if (bron.getDorixonaId() != meniki.getId()) return "Bu bron sizniki emas";

        String yangiHolat = switch (amal) {
            case "tayyor" -> Bron.TAYYOR;
            case "berildi" -> Bron.BERILDI;
            case "bekor" -> Bron.BEKOR;
            default -> null;
        };
        if (yangiHolat == null) return null;

        boolean ru = Texts.ru(lang);
        if (!bronlar.holatniOzgartir(bronId, meniki.getId(), yangiHolat)) {
            // Eski xabardagi tugma qayta bosildi — holat allaqachon o'zgargan.
            Bron hozir = bronlar.getById(bronId);
            return (ru ? "Бронь уже: " : "Bron allaqachon: ") + (hozir == null ? "-" : hozir.holatMatni(ru));
        }

        // Mahsulot berilganda ombordan chiqim qilinadi — qoldiq o'z-o'zidan to'g'ri qoladi.
        String ogohlantirish = "";
        if (Bron.BERILDI.equals(yangiHolat)) {
            Dori dori = drugs.getById(bron.getDoriId());
            if (stock.chiqim(bron.getDoriId(), bron.getSoni(), "bron №" + bronId) == 0
                    && dori != null && dori.hisobYuritiladi()) {
                ogohlantirish = ru
                        ? "\n\n⚠️ На складе недостаточно остатка — расход не записан. Проверьте приход."
                        : "\n\n⚠️ Omborda qoldiq yetmadi — chiqim yozilmadi. Kirimni tekshiring.";
            }
        }

        String javob = switch (yangiHolat) {
            case Bron.TAYYOR -> ru ? "✅ Бронь подтверждена" : "✅ Bron tasdiqlandi";
            case Bron.BERILDI -> ru ? "📦 Отмечено как выдано" : "📦 Berildi deb belgilandi";
            default -> ru ? "❌ Бронь отменена" : "❌ Bron bekor qilindi";
        };
        sender.text(chatId, javob + " (№" + bronId + ")" + ogohlantirish, egaMenyusi(chatId, lang));
        return javob;
    }

    // ————————————————— Yordamchilar —————————————————

    private String kodYarat() {
        return String.valueOf(100000 + RANDOM.nextInt(900000));
    }

    private String engKatta(List<PhotoSize> photos) {
        if (photos == null || photos.isEmpty()) return null;
        return photos.stream()
                .max(Comparator.comparingInt(p -> p.getWidth() * p.getHeight()))
                .map(PhotoSize::getFileId)
                .orElse(null);
    }

    private Double narxOqi(String text) {
        if (text == null) return null;
        String cleaned = text.trim().replace(" ", "").replace(" ", "").replace(",", ".");
        try {
            double value = Double.parseDouble(cleaned);
            return value >= 0 ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer sonOqi(String text) {
        if (text == null) return null;
        String cleaned = text.trim().replace(" ", "").replace(" ", "");
        // Excel'dan "50.0" yoki "50,0" kelishi mumkin — butun son bo'lsa qabul qilamiz.
        cleaned = cleaned.replaceAll("[.,]0+$", "");
        try {
            int value = Integer.parseInt(cleaned);
            return value >= 0 ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Fayldan kelgan nomni tartibga keltiradi: ortiqcha bo'shliqlar va qo'shtirnoqlar olib
     * tashlanadi, butunlay KATTA harfda yozilgan nom odatiy ko'rinishga keltiriladi
     * ("PARATSETAMOL 500MG" -> "Paratsetamol 500mg").
     */
    private String tozaNom(String raw) {
        if (raw == null) return "";
        String s = raw.replace('\u00A0', ' ').replaceAll("\\s+", " ").trim();
        while (s.length() >= 2 && (s.startsWith("\"") || s.startsWith("«")) && (s.endsWith("\"") || s.endsWith("»"))) {
            s = s.substring(1, s.length() - 1).trim();
        }
        boolean harf = false, kichik = false;
        for (char c : s.toCharArray()) {
            if (Character.isLetter(c)) {
                harf = true;
                if (Character.isLowerCase(c)) { kichik = true; break; }
            }
        }
        if (harf && !kichik && s.length() > 4) {
            String lower = s.toLowerCase();
            int i = 0;
            while (i < lower.length() && !Character.isLetter(lower.charAt(i))) i++;
            s = i < lower.length() ? lower.substring(0, i) + Character.toUpperCase(lower.charAt(i)) + lower.substring(i + 1) : lower;
        }
        return s;
    }

    private String qisqa(String value, int max) {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, max - 1) + "…";
    }

    /** Katta sonni o'qishga qulay ko'rinishda: 5400000 -> "5 400 000". */
    private String son(double value) {
        long rounded = Math.round(value);
        String digits = Long.toString(Math.abs(rounded));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < digits.length(); i++) {
            if (i > 0 && (digits.length() - i) % 3 == 0) sb.append(' ');
            sb.append(digits.charAt(i));
        }
        return (rounded < 0 ? "-" : "") + sb;
    }

    private String fullName(User user) {
        String name = valueOr(user.getFirstName(), "") + " " + valueOr(user.getLastName(), "");
        return name.trim();
    }

    private String valueOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    /** HTML rejimida yuborilgani uchun maxsus belgilarni ekranlaymiz. */
    private String esc(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /**
     * Yangi bronni dorixona egasiga yetkazadi (BronNotifier chaqiradi).
     * Egasi hali ulanmagan bo'lsa false qaytadi — xabar keyinroq, egasi ulangach yuboriladi.
     */
    public boolean bronniEgasigaYetkaz(Bron bron) {
        Dorixona d = pharmacies.getById(bron.getDorixonaId());
        if (d == null || d.getEgasiTelegramId() == null) return false;
        String lang = owners.til(d.getEgasiTelegramId());
        sender.text(d.getEgasiTelegramId(),
                (Texts.ru(lang) ? "🔔 <b>Новая бронь!</b>\n\n" : "🔔 <b>Yangi bron!</b>\n\n") + bronMatni(bron, lang),
                Keyboards.bronActions(bron.getId(), false, lang));
        return true;
    }

    // ————————————————— Yetkazib berish —————————————————

    /** Saytdan kelgan hodisa (yangi buyurtma, to'lov cheki, mijoz bekor qildi) haqida egasiga xabar. */
    public boolean yetkazishniEgasigaYetkaz(Yetkazish y) {
        Dorixona d = pharmacies.getById(y.getDorixonaId());
        if (d == null || d.getEgasiTelegramId() == null) return false;
        long egasi = d.getEgasiTelegramId();
        String lang = owners.til(egasi);
        boolean ru = Texts.ru(lang);
        String hodisa = y.getEgagaHodisa() == null ? "YANGI" : y.getEgagaHodisa();
        switch (hodisa) {
            case "CHEK" -> {
                byte[] chek = yetkazishlar.chek(y.getId());
                sender.photoBytes(egasi, chek, (ru ? "💳 <b>Чек оплаты</b> — заказ №" : "💳 <b>To'lov cheki</b> — buyurtma №")
                        + y.getId() + (ru ? "\nСумма: " : "\nSumma: ") + son(y.jami()) + (ru ? " сум" : " so'm"));
                sender.inlineText(egasi, yetkazishMatni(y, lang),
                        Keyboards.yetkazishActions(y.getId(), y.getHolat(), "CHEK_YUBORILDI".equals(y.getTolovHolati()), lang));
            }
            case "BEKOR_MIJOZ" -> sender.text(egasi, (ru ? "❌ <b>Клиент отменил заказ №" : "❌ <b>Mijoz buyurtmani bekor qildi: №")
                    + y.getId() + "</b>", egaMenyusi(egasi, lang));
            default -> sender.inlineText(egasi,
                    (ru ? "🚚 <b>Новый заказ на доставку!</b>\n\n" : "🚚 <b>Yangi yetkazish buyurtmasi!</b>\n\n") + yetkazishMatni(y, lang),
                    Keyboards.yetkazishActions(y.getId(), y.getHolat(), "CHEK_YUBORILDI".equals(y.getTolovHolati()), lang));
        }
        return true;
    }

    private void yetkazishlarRoyxati(long chatId, Dorixona d, String lang) {
        boolean ru = Texts.ru(lang);
        List<Yetkazish> royxat;
        try {
            royxat = yetkazishlar.faollari(d.getId());
        } catch (Exception e) {
            royxat = List.of(); // jadval hali yaratilmagan
        }
        if (royxat.isEmpty()) {
            sender.text(chatId, ru
                    ? "🚚 Активных заказов на доставку нет.\n\nВключить доставку и задать цену можно на сайте: doriqidiruv.com/dorixonachi → «Доставка»."
                    : "🚚 Faol yetkazish buyurtmasi yo'q.\n\nYetkazishni yoqish va narxini belgilash: doriqidiruv.com/dorixonachi → «Yetkazish».",
                    egaMenyusi(chatId, lang));
            return;
        }
        sender.text(chatId, (ru ? "🚚 <b>Активные доставки: " : "🚚 <b>Faol yetkazishlar: ") + royxat.size() + "</b>",
                egaMenyusi(chatId, lang));
        for (Yetkazish y : royxat) {
            sender.inlineText(chatId, yetkazishMatni(y, lang),
                    Keyboards.yetkazishActions(y.getId(), y.getHolat(), "CHEK_YUBORILDI".equals(y.getTolovHolati()), lang));
        }
    }

    private String yetkazishMatni(Yetkazish y, String lang) {
        boolean ru = Texts.ru(lang);
        StringBuilder sb = new StringBuilder();
        sb.append(ru ? "🧾 <b>Заказ №" : "🧾 <b>Buyurtma №").append(y.getId()).append("</b>\n\n");
        for (Yetkazish.Qator q : y.getQatorlar()) {
            sb.append("💊 ").append(esc(q.nomi())).append(" — ").append(q.soni())
              .append("pachka".equals(q.tur()) ? (ru ? " уп." : " pachka") : (ru ? " шт." : " ta"))
              .append(" × ").append(son(q.narx())).append('\n');
        }
        sb.append(ru ? "\n💵 Лекарства: " : "\n💵 Dorilar: ").append(son(y.getDoriSumma()))
          .append(ru ? " + доставка " : " + yetkazish ").append(son(y.getYetkazishNarxi()))
          .append(ru ? " = <b>" : " = <b>").append(son(y.jami())).append(ru ? " сум</b>\n" : " so'm</b>\n");
        sb.append(y.kartaga()
                ? (ru ? "💳 Предоплата на карту: " : "💳 Kartaga oldindan: ") + tolovHolati(y.getTolovHolati(), ru) + "\n"
                : (ru ? "💵 Оплата при получении\n" : "💵 To'lov yetkazilganda\n"));
        sb.append("\n📍 ").append(esc(y.getManzil()));
        if (y.getIzoh() != null && !y.getIzoh().isBlank()) sb.append(" (").append(esc(y.getIzoh())).append(')');
        if (y.getMasofaKm() != null) sb.append(" · ~").append(y.getMasofaKm()).append(" km");
        sb.append('\n');
        if (y.getLatitude() != null && y.getLongitude() != null) {
            sb.append("🗺 <a href=\"https://yandex.uz/maps/?pt=").append(y.getLongitude()).append(',').append(y.getLatitude())
              .append("&z=17&l=map\">").append(ru ? "Открыть на карте" : "Xaritada ochish").append("</a>\n");
        }
        if (y.getMijozIsmi() != null && !y.getMijozIsmi().isBlank()) sb.append("👤 ").append(esc(y.getMijozIsmi())).append('\n');
        if (y.getMijozTelefon() != null && !y.getMijozTelefon().isBlank()) sb.append("☎️ ").append(esc(y.getMijozTelefon())).append('\n');
        if (y.getTaxminiyDaqiqa() != null && Yetkazish.QABUL.equals(y.getHolat())) {
            sb.append(ru ? "⏱ Обещано: ~" : "⏱ Va'da: ~").append(y.getTaxminiyDaqiqa()).append(ru ? " мин\n" : " daqiqa\n");
        }
        sb.append('\n').append(y.holatMatni(ru));
        if (Yetkazish.YANGI.equals(y.getHolat())) {
            sb.append(ru ? "\n\nКурьера можно вызвать в Yandex Go (раздел «Доставка»): забрать — аптека, доставить — адрес выше."
                         : "\n\nKuryerni Yandex Go'dan chaqirish mumkin (\"Yetkazish\" bo'limi): olish — dorixona, topshirish — yuqoridagi manzil.");
        }
        return sb.toString();
    }

    private String tolovHolati(String holat, boolean ru) {
        if ("TASDIQLANDI".equals(holat)) return ru ? "✅ подтверждена" : "✅ tasdiqlangan";
        if ("CHEK_YUBORILDI".equals(holat)) return ru ? "🧾 чек пришёл — проверьте" : "🧾 chek keldi — tekshiring";
        return ru ? "⏳ ожидается" : "⏳ kutilmoqda";
    }

    private String yetkazishQarori(long chatId, long userId, String lang, long id, String amal) {
        Dorixona meniki = pharmacies.egasiBoyicha(userId);
        if (meniki == null) return "Sizda dorixona ulanmagan";
        Yetkazish y = yetkazishlar.getById(id);
        if (y == null) return "Buyurtma topilmadi";
        if (y.getDorixonaId() != meniki.getId()) return "Bu buyurtma sizniki emas";
        boolean ru = Texts.ru(lang);

        if ("tolov".equals(amal)) {
            if (!yetkazishlar.tolovTasdiqla(id, meniki.getId())) return ru ? "Уже подтверждено" : "Allaqachon tasdiqlangan";
            Yetkazish hozir = yetkazishlar.getById(id);
            sender.inlineText(chatId, (ru ? "💳 Оплата подтверждена\n\n" : "💳 To'lov tasdiqlandi\n\n") + yetkazishMatni(hozir, lang),
                    Keyboards.yetkazishActions(id, hozir.getHolat(), false, lang));
            return ru ? "💳 Оплата подтверждена" : "💳 To'lov tasdiqlandi";
        }

        String yangi;
        Integer daqiqa = null;
        switch (amal) {
            case "q30" -> { yangi = Yetkazish.QABUL; daqiqa = 30; }
            case "q60" -> { yangi = Yetkazish.QABUL; daqiqa = 60; }
            case "yol" -> yangi = Yetkazish.YOLDA;
            case "ok" -> yangi = Yetkazish.YETKAZILDI;
            case "bekor" -> yangi = Yetkazish.BEKOR;
            default -> { return null; }
        }
        if (!yetkazishlar.holatniOzgartir(id, meniki.getId(), yangi, daqiqa, null)) {
            Yetkazish hozir = yetkazishlar.getById(id);
            return (ru ? "Заказ уже: " : "Buyurtma allaqachon: ") + (hozir == null ? "-" : hozir.holatMatni(ru));
        }

        // Yetkazilganda ombor hisobi yuritilayotgan mahsulotlar uchun chiqim yoziladi (bir marta).
        String ogohlantirish = "";
        if (Yetkazish.YETKAZILDI.equals(yangi) && yetkazishlar.chiqimniBelgila(id)) {
            for (Yetkazish.Qator q : y.getQatorlar()) {
                if (q.doriId() == null) continue;
                Dori dori = drugs.getById(q.doriId());
                if (dori != null && dori.hisobYuritiladi() && stock.chiqim(q.doriId(), q.soni(), "yetkazish №" + id) == 0) {
                    ogohlantirish = ru ? "\n\n⚠️ Не хватило остатка на складе — часть расхода не записана."
                                       : "\n\n⚠️ Omborda qoldiq yetmadi — chiqimning bir qismi yozilmadi.";
                }
            }
        }

        Yetkazish hozir = yetkazishlar.getById(id);
        String javob = switch (yangi) {
            case Yetkazish.QABUL -> ru ? "✅ Заказ принят" : "✅ Buyurtma qabul qilindi";
            case Yetkazish.YOLDA -> ru ? "🚚 Курьер в пути" : "🚚 Kuryer yo'lda";
            case Yetkazish.YETKAZILDI -> ru ? "📦 Доставлен" : "📦 Yetkazildi";
            default -> ru ? "❌ Заказ отменён" : "❌ Buyurtma bekor qilindi";
        };
        sender.inlineText(chatId, javob + " (№" + id + ")" + ogohlantirish + "\n\n" + yetkazishMatni(hozir, lang),
                Keyboards.yetkazishActions(id, hozir.getHolat(), "CHEK_YUBORILDI".equals(hozir.getTolovHolati()), lang));
        return javob;
    }
}
