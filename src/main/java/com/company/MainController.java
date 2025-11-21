package com.company;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.*;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class MainController {

    private final MyBot bot;
    private final Map<Long, UserEntity> sessions = new HashMap<>(); // vaqtinchalik ro'yxatdan o'tish
    private final Map<Long, GameState> userGameState = new HashMap<>();
    private final AtomicInteger requestCounter = new AtomicInteger(1);


    public MainController(MyBot bot) {
        this.bot = bot;
    }

    public void handleMessage(Message message) {
        Long chatId = message.getChatId();
        String text = message.hasText() ? message.getText().trim() : "";

        // REGISTER flow
        UserEntity session = sessions.get(chatId);
        if (session != null && session.getUserSteps() != null) {
            // ro'yxat, userSteps bo'yicha davom etamiz
            handleRegistration(message, session);
            return;
        }

        // Agar foydalanuvchi menu tanlagan bo'lsa
        if (text.equals("/start")) {
            sendMainMenu(chatId);
            return;
        }

        // Admin menyusi tugmalarida
        if (chatId.equals(DataBase.ADMIN_ID)) {
            handleAdminCommands(message);
            return;
        }

        // Oddiy foydalanuvchi menyu tugmalari
        switch (text) {
            case "🎭 Profilim" -> sendProfile(chatId);
            case "📚 Tasodifiy fakt" -> sendRandomFact(chatId);
            case "🎯 Son topish" -> startGuessNumber(chatId);
            case "❌ X - O o‘yini" -> startTicTacToe(chatId);
            case "🔎 Ma'lumot so‘rash" -> promptRequestName(chatId, message.getFrom());
            default -> {
                GameState gs = userGameState.getOrDefault(chatId, GameState.NONE);
                if (gs == GameState.GUESS_NUMBER) {
                    processGuessNumber(chatId, text);
                } else if (gs == GameState.TIC_TAC_TOE) {
                    processTicTacToeMove(chatId, text);
                } else {

                    if (!DataBase.users.containsKey(chatId)) {

                        SendMessage sm = new SendMessage(chatId.toString(),
                                "Salom! /start yozib boshlang yoki /register buyrug'ini yuboring.");
                        bot.sendMsg(sm);
                    } else {
                        SendMessage sm = new SendMessage(chatId.toString(),
                                "Noto'g'ri buyruq. Menudan tanlang.");
                        bot.sendMsg(sm);
                    }
                }
            }
        }
    }

    private void handleRegistration(Message message, UserEntity session) {
        Long chatId = message.getChatId();
        SendMessage sm;
        switch (session.getUserSteps()) {
            case NAME -> {
                session.setFullName(message.getText().trim());
                session.setUserSteps(UserSteps.AGE);
                sm = new SendMessage(chatId.toString(), "Yoshingizni kiriting:");
                bot.sendMsg(sm);
            }
            case AGE -> {
                try {
                    int age = Integer.parseInt(message.getText().trim());
                    session.setAge(age);
                    session.setUserSteps(UserSteps.PHONE);
                    sm = new SendMessage(chatId.toString(), "Telefon raqamingizni kiriting (masalan: +99890...):");
                    bot.sendMsg(sm);
                } catch (NumberFormatException e) {
                    bot.sendMsg(new SendMessage(chatId.toString(), "Iltimos faqat raqam kiriting."));
                }
            }
            case PHONE -> {
                session.setPhone(message.getText().trim());
                session.setRegisteredAt(LocalDateTime.now());
                DataBase.users.put(chatId, session); // saqlaymiz
                sessions.remove(chatId);
                bot.sendMsg(new SendMessage(chatId.toString(), "✅ Ro'yxatdan o'tdingiz! Endi menyudan foydalaning."));
            }
        }
    }


    private void handleAdminCommands(Message message) {
        Long chatId = message.getChatId();
        String text = message.getText().trim();

        if (text.equals("/start")) {
            SendMessage sm = new SendMessage(chatId.toString(), "👑 Admin panel");
            sm.setReplyMarkup(getAdminKeyboard());
            bot.sendMsg(sm);
            return;
        }

        if (text.equals("📋 Foydalanuvchilar ro'yxati")) {

            if (bot != null) {
                // MainController doesn't hold last message id - DataBase holds it
                if (DataBase.lastAdminMsgId != null) bot.deleteMessage(chatId, DataBase.lastAdminMsgId);
            }

            SendMessage sm = new SendMessage(chatId.toString(), buildUsersListText());
            Message sent = bot.sendMsg(sm);
            if (sent != null) DataBase.lastAdminMsgId = sent.getMessageId();
            return;
        }

        if (text.equals("📨 So'rovlar")) {
            StringBuilder sb = new StringBuilder("📨 So'rovlar:\n\n");
            if (DataBase.requests.isEmpty()) sb.append("So'rovlar mavjud emas.");
            else {
                for (Map.Entry<Integer, javax.ws.rs.core.Request> e : DataBase.requests.entrySet()) {
                    Request r = (Request) e.getValue();
                    sb.append("ID: ").append(e.getKey()).append("\n");
                    sb.append("So‘ragan: ").append(r.getRequesterName()).append(" (").append(r.getRequesterId()).append(")\n");
                    sb.append("Kim haqida: ").append(r.getTargetName()).append("\n");
                    sb.append("--------------------\n");
                }
            }
            bot.sendMsg(new SendMessage(chatId.toString(), sb.toString()));
            return;
        }

        if (text.equals("📢 Broadcast")) {
            bot.sendMsg(new SendMessage(chatId.toString(), "Broadcast xabar yozing (men barcha userlarga yuboraman):"));

            DataBase.broadcastPendingAdmin = chatId;
            return;
        }


        if (DataBase.broadcastPendingAdmin != null && DataBase.broadcastPendingAdmin.equals(chatId)) {
            String btext = message.getText();
            for (Long uid : DataBase.users.keySet()) {
                bot.sendMsg(new SendMessage(uid.toString(), "📢 Admin xabari:\n\n" + btext));
            }
            DataBase.broadcastPendingAdmin = null;
            bot.sendMsg(new SendMessage(chatId.toString(), "Broadcast yuborildi."));
            return;
        }


    }


    private void sendMainMenu(Long chatId) {
        SendMessage sm = new SendMessage(chatId.toString(), "Menuga xush kelibsiz! Tanlang:");
        sm.setReplyMarkup(getUserKeyboard());
        bot.sendMsg(sm);
    }

    private ReplyKeyboardMarkup getUserKeyboard() {
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);
        List<KeyboardRow> rows = new ArrayList<>();

        KeyboardRow r1 = new KeyboardRow();
        r1.add(new KeyboardButton("🎭 Profilim"));
        r1.add(new KeyboardButton("📚 Tasodifiy fakt"));

        KeyboardRow r2 = new KeyboardRow();
        r2.add(new KeyboardButton("🎯 Son topish"));
        r2.add(new KeyboardButton("❌ X - O o‘yini"));

        KeyboardRow r3 = new KeyboardRow();
        r3.add(new KeyboardButton("🔎 Ma'lumot so‘rash"));
        r3.add(new KeyboardButton("/start"));

        rows.add(r1);
        rows.add(r2);
        rows.add(r3);

        keyboard.setKeyboard(rows);
        return keyboard;
    }

    private ReplyKeyboardMarkup getAdminKeyboard() {
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);
        List<KeyboardRow> rows = new ArrayList<>();

        KeyboardRow r1 = new KeyboardRow();
        r1.add(new KeyboardButton("📋 Foydalanuvchilar ro'yxati"));
        r1.add(new KeyboardButton("📨 So'rovlar"));

        KeyboardRow r2 = new KeyboardRow();
        r2.add(new KeyboardButton("📢 Broadcast"));
        r2.add(new KeyboardButton("/start"));

        rows.add(r1);
        rows.add(r2);

        keyboard.setKeyboard(rows);
        return keyboard;
    }

    // PROFILE
    private void sendProfile(Long chatId) {
        UserEntity u = DataBase.users.get(chatId);
        if (u == null) {
            sessions.put(chatId, new UserEntity(chatId));
            sessions.get(chatId).setUserSteps(UserSteps.NAME);
            bot.sendMsg(new SendMessage(chatId.toString(), "Siz ro'yxatdan o'tmagansiz. Iltimos ismingizni kiriting:"));
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("👤 Profil:\n\n");
        sb.append("Ism: ").append(u.getFullName()).append("\n");
        sb.append("Yosh: ").append(u.getAge()).append("\n");
        sb.append("Tel: ").append(u.getPhone()).append("\n");
        sb.append("Ro'yxatdan o'tgan: ").append(u.getRegisteredAt()).append("\n");
        bot.sendMsg(new SendMessage(chatId.toString(), sb.toString()));
    }

    // FACT
    private void sendRandomFact(Long chatId) {
        String fact = FactService.getRandomFact();
        bot.sendMsg(new SendMessage(chatId.toString(), fact));
    }

    // GUESS NUMBER
    private void startGuessNumber(Long chatId) {
        int n = new Random().nextInt(10) + 1;
        DataBase.randomNumbers.put(chatId, n);
        userGameState.put(chatId, GameState.GUESS_NUMBER);
        bot.sendMsg(new SendMessage(chatId.toString(), "Men 1–10 gacha son o'yladim. Topganingda yoz (faqat bitta raqam):"));
    }

    private void processGuessNumber(Long chatId, String text) {
        try {
            int g = Integer.parseInt(text.trim());
            Integer secret = DataBase.randomNumbers.get(chatId);
            if (secret == null) {
                bot.sendMsg(new SendMessage(chatId.toString(), "O'yinni boshlang: 🎯 Son topish"));
                userGameState.put(chatId, GameState.NONE);
                return;
            }
            if (g == secret) {
                bot.sendMsg(new SendMessage(chatId.toString(), "🎉 Tabriklayman! To'g'ri son: " + secret));
                DataBase.randomNumbers.remove(chatId);
                userGameState.put(chatId, GameState.NONE);
            } else {
                bot.sendMsg(new SendMessage(chatId.toString(), "❌ Noto'g'ri. Yana urinib ko'ring."));
            }
        } catch (NumberFormatException e) {
            bot.sendMsg(new SendMessage(chatId.toString(), "Iltimos faqat raqam yozing."));
        }
    }

    // TIC TAC TOE
    private void startTicTacToe(Long chatId) {
        TicTacToeGame game = new TicTacToeGame();
        DataBase.ticTacToe.put(chatId, game);
        userGameState.put(chatId, GameState.TIC_TAC_TOE);
        bot.sendMsg(new SendMessage(chatId.toString(), "X - O o'yini boshlandi!\nTaxtani ko'rish uchun:"));
        bot.sendMsg(new SendMessage(chatId.toString(), game.getBoard()));
        bot.sendMsg(new SendMessage(chatId.toString(), "Harakat: satr va ustun (0 0 dan 2 2 gacha), masalan: `1 1`"));
    }

    private void processTicTacToeMove(Long chatId, String text) {
        if (!text.matches("[0-2]\\s[0-2]")) {
            bot.sendMsg(new SendMessage(chatId.toString(), "Format: satr ustun (masalan: 1 1)"));
            return;
        }
        String[] parts = text.split("\\s");
        int r = Integer.parseInt(parts[0]);
        int c = Integer.parseInt(parts[1]);
        TicTacToeGame game = DataBase.ticTacToe.get(chatId);
        if (game == null) {
            bot.sendMsg(new SendMessage(chatId.toString(), "O'yinni boshlang: ❌ X - O o‘yini"));
            return;
        }
        if (!game.makeUserMove(r, c)) {
            bot.sendMsg(new SendMessage(chatId.toString(), "Bu joy band. Boshqa joyni tanlang."));
            return;
        }
        if (game.checkWin('X')) {
            bot.sendMsg(new SendMessage(chatId.toString(), game.getBoard()));
            bot.sendMsg(new SendMessage(chatId.toString(), "🎉 Siz yutdingiz!"));
            DataBase.ticTacToe.remove(chatId);
            userGameState.put(chatId, GameState.NONE);
            return;
        }
        game.botMove();
        bot.sendMsg(new SendMessage(chatId.toString(), game.getBoard()));
        if (game.checkWin('O')) {
            bot.sendMsg(new SendMessage(chatId.toString(), "😢 Bot yutdi."));
            DataBase.ticTacToe.remove(chatId);
            userGameState.put(chatId, GameState.NONE);
            return;
        }
        if (game.isDraw()) {
            bot.sendMsg(new SendMessage(chatId.toString(), "⚖ Durang!"));
            DataBase.ticTacToe.remove(chatId);
            userGameState.put(chatId, GameState.NONE);
        }
    }


    private void promptRequestName(Long chatId, User requester) {

        if (!DataBase.users.containsKey(chatId)) {
            sessions.put(chatId, new UserEntity(chatId));
            sessions.get(chatId).setUserSteps(UserSteps.NAME);
            bot.sendMsg(new SendMessage(chatId.toString(), "Siz avval ro'yxatdan o'tishingiz kerak. Ismingizni kiriting:"));
            return;
        }

        sessions.put(chatId, new UserEntity(chatId));
        sessions.get(chatId).setUserSteps(UserSteps.PHONE); // biz bu state ni vaqtincha "awaiting request name" deb ishlatamiz
        bot.sendMsg(new SendMessage(chatId.toString(), "Ma'lumot so'ramoqchi bo'lgan odamning to'liq ismini kiriting:"));
    }

    // Callback (admin approve/reject)
    public void handleCallback(CallbackQuery cq) {
        String data = cq.getData(); // format: action:requestId
        Long adminChatId = cq.getFrom().getId();
        if (!adminChatId.equals(DataBase.ADMIN_ID)) {

            bot.sendMsg(new SendMessage(adminChatId.toString(), "Sizda ruxsat yo'q."));
            return;
        }
        if (data == null || !data.contains(":")) return;
        String[] parts = data.split(":");
        String action = parts[0];
        int reqId = Integer.parseInt(parts[1]);
        Request req = (Request) DataBase.requests.get(reqId);
        if (req == null) {
            bot.sendMsg(new SendMessage(adminChatId.toString(), "So'rov topilmadi."));
            return;
        }
        if (action.equals("approve")) {

            Optional<UserEntity> target = DataBase.users.values().stream()
                    .filter(u -> u.getFullName().equalsIgnoreCase(req.getTargetName()))
                    .findFirst();
            if (target.isPresent()) {
                UserEntity t = target.get();
                StringBuilder sb = new StringBuilder();
                sb.append("👤 Ma'lumot (ruxsat bilan):\n");
                sb.append("Ism: ").append(t.getFullName()).append("\n");
                sb.append("Yosh: ").append(t.getAge()).append("\n");
                sb.append("Tel: ").append(t.getPhone()).append("\n");
                bot.sendMsg(new SendMessage(req.getRequesterId().toString(), sb.toString()));
                bot.sendMsg(new SendMessage(adminChatId.toString(), "Ma'lumot yuborildi."));
            } else {
                bot.sendMsg(new SendMessage(adminChatId.toString(), "Bunday ism bilan foydalanuvchi topilmadi."));
                bot.sendMsg(new SendMessage(req.getRequesterId().toString(), "Kechirasiz, ma'lumot topilmadi."));
            }
            DataBase.requests.remove(reqId);
        } else if (action.equals("reject")) {
            bot.sendMsg(new SendMessage(req.getRequesterId().toString(), "Sizning so'rovingiz rad etildi."));
            bot.sendMsg(new SendMessage(adminChatId.toString(), "So'rov rad etildi."));
            DataBase.requests.remove(reqId);
        }
    }

    public void createRequest(Long requesterId, String targetName, String requesterName) {
        int id = requestCounter.getAndIncrement();
        Request r = new Request(id, requesterId, requesterName, targetName);
        DataBase.requests.put(id, r);

        SendMessage sm = new SendMessage(DataBase.ADMIN_ID.toString(), "❗ Yangi so'rov №" + id +
                "\nSo'ragan: " + requesterName + " (" + requesterId + ")\nKim haqida: " + targetName +
                "\nRuxsat berasizmi?");
        InlineKeyboardMarkup ik = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton approve = new InlineKeyboardButton();
        approve.setText("✅ Ruxsat berish");
        approve.setCallbackData("approve:" + id);
        InlineKeyboardButton reject = new InlineKeyboardButton();
        reject.setText("❌ Rad etish");
        reject.setCallbackData("reject:" + id);
        row.add(approve);
        row.add(reject);
        rows.add(row);
        ik.setKeyboard(rows);
        sm.setReplyMarkup(ik);
        bot.sendMsg(sm);
    }


    private void checkPendingRequest(Message message) {
        Long chatId = message.getChatId();
        UserEntity s = sessions.get(chatId);
        if (s != null && s.getUserSteps() == UserSteps.PHONE && DataBase.users.containsKey(chatId)) {
            // treat message as request target name
            String targetName = message.getText().trim();
            createRequest(chatId, targetName, DataBase.users.get(chatId).getFullName());
            sessions.remove(chatId);
            bot.sendMsg(new SendMessage(chatId.toString(), "So'rovingiz adminga yuborildi. Javob kuting."));
        }
    }

    private String buildUsersListText() {
        if (DataBase.users.isEmpty()) return "Hozircha foydalanuvchi yo'q.";
        StringBuilder sb = new StringBuilder("📋 Foydalanuvchilar:\n\n");
        for (UserEntity u : DataBase.users.values()) {
            sb.append("👤 ").append(u.getFullName()).append("\n");
            sb.append("🎂 Yosh: ").append(u.getAge()).append("\n");
            sb.append("📞 Tel: ").append(u.getPhone()).append("\n");
            sb.append("--------------------\n");
        }
        return sb.toString();
    }
}
