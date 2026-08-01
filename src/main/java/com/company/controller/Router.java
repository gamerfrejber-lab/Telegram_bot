package com.company.controller;

import com.company.admin.AdminPanel;
import com.company.flow.Session;
import com.company.menu.Keyboards;
import com.company.model.Bron;
import com.company.model.Dori;
import com.company.model.Dorixona;
import com.company.model.Soov;
import com.company.repository.BronRepository;
import com.company.repository.DrugRepository;
import com.company.repository.OwnerRepository;
import com.company.repository.PharmacyRepository;
import com.company.repository.SoovRepository;
import com.company.repository.StockRepository;
import com.company.telegram.Sender;
import com.company.text.Texts;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Contact;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;

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
            sessionQadami(chatId, userId, lang, session, text);
            return;
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
        if (AdminPanel.isAdmin(userId)) {
            sender.text(chatId, adminBosh(lang), Keyboards.adminMenu(lang));
            return;
        }
        Dorixona meniki = pharmacies.egasiBoyicha(userId);
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
        if (AdminPanel.isAdmin(userId)) return Keyboards.adminMenu(lang);
        return pharmacies.egasiBoyicha(userId) != null ? Keyboards.ownerMenu(lang) : Keyboards.guestMenu(lang);
    }

    // ————————————————— Admin bo'limi —————————————————

    private String adminBosh(String lang) {
        int kutilmoqda = soovlar.kutilayotganlar().size();
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
        if (Keyboards.isAdminStats(text)) {
            sender.text(chatId, statistika(lang), Keyboards.adminMenu(lang));
            return true;
        }
        return false;
    }

    private void dorixonalarRoyxati(long chatId, String lang) {
        List<Dorixona> all = pharmacies.getAll();
        if (all.isEmpty()) {
            sender.text(chatId, Texts.ru(lang) ? "Аптек пока нет." : "Hali dorixona yo'q.", Keyboards.adminMenu(lang));
            return;
        }
        StringBuilder sb = new StringBuilder(Texts.ru(lang)
                ? "📋 <b>Аптеки (" + all.size() + ")</b>\n\n" : "📋 <b>Dorixonalar (" + all.size() + ")</b>\n\n");
        int chiqarildi = 0;
        for (Dorixona d : all) {
            if (chiqarildi >= 30) break;
            sb.append("🏥 <b>").append(esc(d.getNomi())).append("</b>\n");
            if (d.getManzil() != null && !d.getManzil().isBlank()) sb.append("   📍 ").append(esc(d.getManzil())).append('\n');
            sb.append("   ").append(obunaMatni(d, lang)).append('\n');
            sb.append("   ").append(d.getEgasiTelegramId() == null
                    ? (Texts.ru(lang) ? "👤 владелец не подключён" : "👤 egasi ulanmagan")
                    : (Texts.ru(lang) ? "👤 владелец: " : "👤 egasi: ") + d.getEgasiTelegramId()).append('\n');
            sb.append("   🆔 ").append(d.getId()).append("\n\n");
            chiqarildi++;
        }
        if (all.size() > chiqarildi) {
            sb.append(Texts.ru(lang) ? "… и ещё " : "… va yana ").append(all.size() - chiqarildi);
        }
        sender.text(chatId, sb.toString(), Keyboards.adminMenu(lang));
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

    private String statistika(String lang) {
        List<Dorixona> all = pharmacies.getAll();
        long ulangan = all.stream().filter(d -> d.getEgasiTelegramId() != null).count();
        long faol = all.stream().filter(Dorixona::obunaFaol).count();
        boolean ru = Texts.ru(lang);
        return (ru ? "📊 <b>Статистика</b>\n\n" : "📊 <b>Statistika</b>\n\n")
                + (ru ? "🏥 Всего аптек: <b>" : "🏥 Jami dorixonalar: <b>") + all.size() + "</b>\n"
                + (ru ? "👤 Подключено владельцев: <b>" : "👤 Egasi ulangan: <b>") + ulangan + "</b>\n"
                + (ru ? "✅ Активных подписок: <b>" : "✅ Faol obunalar: <b>") + faol + "</b>\n"
                + (ru ? "📨 Заявок в ожидании: <b>" : "📨 Kutilayotgan arizalar: <b>") + soovlar.kutilayotganlar().size() + "</b>\n"
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

        boolean kabinetTugmasi = Keyboards.isProducts(text) || Keyboards.isAddProduct(text)
                || Keyboards.isStockIn(text) || Keyboards.isStockOut(text)
                || Keyboards.isBrons(text) || Keyboards.isReport(text) || Keyboards.isSubscription(text);
        if (!kabinetTugmasi) return false;

        // Obuna tugagan bo'lsa faqat obuna bo'limi ochiq qoladi.
        if (!meniki.obunaFaol() && !Keyboards.isSubscription(text)) {
            sender.text(chatId, Texts.t(lang, "subscriptionExpired"), Keyboards.ownerMenu(lang));
            return true;
        }

        if (Keyboards.isProducts(text)) {
            mahsulotlar(chatId, meniki, lang);
            return true;
        }
        if (Keyboards.isAddProduct(text)) {
            Session session = new Session(Session.Turi.MAHSULOT_QOSHISH);
            session.setDorixonaId(meniki.getId());
            sessions.put(userId, session);
            sender.text(chatId, Texts.ru(lang)
                    ? "📦 <b>Название товара:</b>\n\nНапример: Парацетамол, Бинт стерильный"
                    : "📦 <b>Mahsulot nomi:</b>\n\nMasalan: Paracetamol, Bint steril", Keyboards.cancelMenu(lang));
            return true;
        }
        if (Keyboards.isStockIn(text) || Keyboards.isStockOut(text)) {
            boolean kirim = Keyboards.isStockIn(text);
            List<Dori> royxat = drugs.dorixonaniki(meniki.getId());
            if (royxat.isEmpty()) {
                sender.text(chatId, Texts.ru(lang)
                        ? "❌ Сначала добавьте товар." : "❌ Avval mahsulot qo'shing.", Keyboards.ownerMenu(lang));
                return true;
            }
            Session session = new Session(kirim ? Session.Turi.KIRIM : Session.Turi.CHIQIM);
            session.setDorixonaId(meniki.getId());
            sessions.put(userId, session);
            sender.text(chatId, kirim
                            ? (Texts.ru(lang) ? "📥 <b>Что поступило? Выберите товар:</b>" : "📥 <b>Nima keldi? Mahsulotni tanlang:</b>")
                            : (Texts.ru(lang) ? "📤 <b>Что продано? Выберите товар:</b>" : "📤 <b>Nima sotildi? Mahsulotni tanlang:</b>"),
                    Keyboards.listMenu(lang, royxat.stream().map(Dori::getNomi).toList()));
            return true;
        }
        if (Keyboards.isBrons(text)) {
            bronlarRoyxati(chatId, meniki, lang);
            return true;
        }
        if (Keyboards.isReport(text)) {
            hisobot(chatId, meniki, lang);
            return true;
        }
        if (Keyboards.isSubscription(text)) {
            sender.text(chatId, obunaTafsiloti(meniki, lang), Keyboards.ownerMenu(lang));
            return true;
        }
        return false;
    }

    private void mahsulotlar(long chatId, Dorixona d, String lang) {
        List<Dori> royxat = drugs.dorixonaniki(d.getId());
        boolean ru = Texts.ru(lang);
        if (royxat.isEmpty()) {
            sender.text(chatId, ru ? "📦 Товаров пока нет. Нажмите «Добавить товар»."
                    : "📦 Hali mahsulot yo'q. «Mahsulot qo'shish» tugmasini bosing.", Keyboards.ownerMenu(lang));
            return;
        }
        StringBuilder sb = new StringBuilder(ru
                ? "📦 <b>Мои товары (" + royxat.size() + ")</b>\n\n"
                : "📦 <b>Mahsulotlarim (" + royxat.size() + ")</b>\n\n");
        int chiqarildi = 0;
        for (Dori dori : royxat) {
            if (chiqarildi >= 40) break;
            sb.append("• <b>").append(esc(dori.getNomi())).append("</b> — ")
              .append(son(dori.getNarx())).append(ru ? " сум" : " so'm").append('\n')
              .append("   ").append(qoldiqMatni(dori, ru)).append('\n');
            chiqarildi++;
        }
        if (royxat.size() > chiqarildi) {
            sb.append(ru ? "\n… и ещё " : "\n… va yana ").append(royxat.size() - chiqarildi);
        }
        sender.text(chatId, sb.toString(), Keyboards.ownerMenu(lang));
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
        sender.text(chatId, matn, Keyboards.ownerMenu(lang));
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
            sender.text(chatId, ru ? "🔔 Активных броней нет." : "🔔 Faol bron yo'q.", Keyboards.ownerMenu(lang));
            return;
        }
        sender.text(chatId, (ru ? "🔔 <b>Активные брони: " : "🔔 <b>Faol bronlar: ") + royxat.size() + "</b>",
                Keyboards.ownerMenu(lang));
        for (Bron bron : royxat) {
            sender.text(chatId, bronMatni(bron, lang),
                    Keyboards.bronActions(bron.getId(), Bron.TAYYOR.equals(bron.getHolat())));
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
                    Keyboards.ownerMenu(lang));
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
                    Keyboards.claimDecision(soov.getId()));
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
                session.setQadam(1);
                sender.text(chatId, ru ? "💵 <b>Цена (сум):</b>" : "💵 <b>Narxi (so'm):</b>", Keyboards.cancelMenu(lang));
            }
            case 1 -> {
                Double narx = narxOqi(text);
                if (narx == null) {
                    sender.text(chatId, ru ? "❌ Неверная цена. Введите число, например 12000."
                            : "❌ Narx noto'g'ri. Raqam kiriting, masalan 12000.", Keyboards.cancelMenu(lang));
                    return;
                }
                session.setNarx(narx);
                session.setQadam(2);
                sender.text(chatId, ru
                        ? "🔢 <b>Сколько штук есть сейчас?</b>\n\nЭто будет первым приходом. Если не ведёте учёт — «Пропустить»."
                        : "🔢 <b>Hozir nechta bor?</b>\n\nBu birinchi kirim bo'ladi. Hisob yuritmasangiz — «O'tkazib yuborish».",
                        Keyboards.skipCancelMenu(lang));
            }
            case 2 -> {
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
                        Keyboards.ownerMenu(lang));
            }
            default -> sessions.remove(userId);
        }
    }

    private void omborQadami(long chatId, long userId, String lang, Session session, String text) {
        boolean ru = Texts.ru(lang);
        boolean kirim = session.getTuri() == Session.Turi.KIRIM;

        if (session.getQadam() == 0) {
            Dori dori = drugs.nomiBoyicha(session.getDorixonaId(), text);
            if (dori == null) {
                List<Dori> royxat = drugs.dorixonaniki(session.getDorixonaId());
                sender.text(chatId, ru ? "❌ Товар не найден. Выберите из списка."
                                : "❌ Mahsulot topilmadi. Ro'yxatdan tanlang.",
                        Keyboards.listMenu(lang, royxat.stream().map(Dori::getNomi).toList()));
                return;
            }
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
            return;
        }

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
                Keyboards.ownerMenu(lang));
    }

    // ————————————————— Inline tugmalar —————————————————

    public String handleCallback(CallbackQuery query) {
        String data = query.getData();
        long userId = query.getFrom().getId();
        long chatId = query.getMessage().getChatId();
        String lang = owners.til(userId);
        if (data == null) return null;

        String[] parts = data.split(":");
        if (parts.length < 3) return null;
        long id = Long.parseLong(parts[2]);

        if ("soov".equals(parts[0])) {
            if (!AdminPanel.isAdmin(userId)) return "Ruxsat yo'q";
            return arizaQarori(chatId, id, "ok".equals(parts[1]));
        }
        if ("bron".equals(parts[0])) {
            return bronQarori(chatId, userId, lang, id, parts[1]);
        }
        return null;
    }

    private String arizaQarori(long adminChatId, long soovId, boolean tasdiq) {
        Soov soov = soovlar.getById(soovId);
        if (soov == null) return "Ariza topilmadi";
        if (!Soov.KUTILMOQDA.equals(soov.getHolat())) return "Bu ariza allaqachon hal qilingan";

        if (!soovlar.hal(soovId, tasdiq ? Soov.TASDIQLANGAN : Soov.RAD, null)) {
            return "Bu ariza allaqachon hal qilingan";
        }

        // Egasiga javobni bu yerda emas, kuzatuvchi yetkazadi (hal() "aytilmagan" deb
        // belgilab qo'ydi). Shunda ariza mijozlar botida hal qilinsa ham egasi bir xil
        // xabar oladi va xabar ikki marta ketmaydi.
        if (!tasdiq) {
            sender.text(adminChatId, "❌ Ariza №" + soovId + " rad etildi.");
            return "Rad etildi";
        }

        if (!pharmacies.egasiniBiriktir(soov.getDorixonaId(), soov.getTelegramId())) {
            sender.text(adminChatId, "⚠️ Bu dorixona allaqachon boshqa egaga biriktirilgan.");
            return "Dorixona allaqachon band";
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
                    Keyboards.ownerMenu(lang));
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

        if (!bronlar.holatniOzgartir(bronId, meniki.getId(), yangiHolat)) return "O'zgartirib bo'lmadi";

        // Mahsulot berilganda ombordan chiqim qilinadi — qoldiq o'z-o'zidan to'g'ri qoladi.
        if (Bron.BERILDI.equals(yangiHolat)) {
            stock.chiqim(bron.getDoriId(), bron.getSoni(), "bron №" + bronId);
        }

        boolean ru = Texts.ru(lang);
        String javob = switch (yangiHolat) {
            case Bron.TAYYOR -> ru ? "✅ Бронь подтверждена" : "✅ Bron tasdiqlandi";
            case Bron.BERILDI -> ru ? "📦 Отмечено как выдано" : "📦 Berildi deb belgilandi";
            default -> ru ? "❌ Бронь отменена" : "❌ Bron bekor qilindi";
        };
        sender.text(chatId, javob + " (№" + bronId + ")", Keyboards.ownerMenu(lang));
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
        try {
            int value = Integer.parseInt(cleaned);
            return value >= 0 ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
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
                Keyboards.bronActions(bron.getId(), false));
        return true;
    }
}
