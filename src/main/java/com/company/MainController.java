package com.company;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainController {

    private final UserRepository userRepository = new UserRepository();
    private final Map<Long, com.company.UserEntity> map = new HashMap<>();

    private final Long ADMIN_ID = 8050814208L;


    public SendMessage messageHandler(Message message) {
        String text = message.getText();
        Long id = message.getChatId();

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(id);

        com.company.UserEntity entity = map.get(id);

        if (id.equals(ADMIN_ID)) {
            System.out.println(id + " if ga kirdi");
            if (text.equals("/start")) {
                sendMessage.setText("👑Salom Suxrob Admin panelizga xush kelibsiz!");
                sendMessage.setReplyMarkup(getAminKeyboard());
                return sendMessage;
            } else if (text.equals("Foydalanuvchilar ro'yxati")) {
                List<com.company.UserEntity> list = userRepository.getAllUsers();
                if (list.isEmpty()) {
                    sendMessage.setText("Hozircha foydalanuvchi yo‘q 😴");
                } else {
                    StringBuilder builder = new StringBuilder("📋 Foydalanuvchilar ro‘yxati:\n\n");
                    for (com.company.UserEntity u : list) {
                        builder.append("👤 Ism: ").append(u.getFullName()).append("\n")
                                .append("🎂 Yosh: ").append(u.getAge()).append("\n")
                                .append("📞 Tel: ").append(u.getPhone()).append("\n")
                                .append("────────────────────\n");
                    }
                    sendMessage.setText(builder.toString());
                }
                sendMessage.setReplyMarkup(getAminKeyboard());
                return sendMessage;
            }
        }

        if (text.equals("/start")) {
            User user = message.getFrom();
            sendMessage.setText("Assalomu alaykum " + user.getFirstName() + "!\nIsmingizni kiriting: ");
            com.company.UserEntity newUser = new com.company.UserEntity();
            newUser.setId(id);
            newUser.setUserSteps(UserSteps.NAME);
            map.put(id, newUser);
        } else if (entity != null) {
            return registration(message, entity);
        } else {
            sendMessage.setText("❗️Iltimos /start buyrug‘idan boshlang.");
        }
        return sendMessage;
    }



    private SendMessage registration(Message message, com.company.UserEntity entity) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId());

        if (entity.getUserSteps().equals(UserSteps.NAME)) {
            entity.setFullName(message.getText());
            entity.setUserSteps(UserSteps.AGE);
            map.put(message.getChatId(), entity);
            sendMessage.setText("Yoshingizni kiriting: ");
        } else if (entity.getUserSteps().equals(UserSteps.AGE)) {
            entity.setAge(Integer.parseInt(message.getText()));
            entity.setUserSteps(UserSteps.PHONE);
            map.put(message.getChatId(), entity);
            sendMessage.setText("Telefon raqamingizni kiriting:");
        } else if (entity.getUserSteps().equals(UserSteps.PHONE)) {
            entity.setPhone(message.getText());
            userRepository.saveUser(entity);
            map.remove(message.getChatId());
            sendMessage.setText("✅ Ro‘yxatdan o‘tish yakunlandi!\nRahmat 😊");
            sendMessage.setText("Ro'yxatdan o'tdingiz ✅");

        }

        return sendMessage;
    }

    private ReplyKeyboard getAminKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);

        List<KeyboardRow> rows = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("Foydalanuvchilar ro'yxati"));

        rows.add(row1);
        keyboardMarkup.setKeyboard(rows);
        return keyboardMarkup;
    }

}
