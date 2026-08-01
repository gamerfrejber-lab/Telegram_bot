package com.company.admin;

import com.company.config.Config;

import java.util.HashSet;
import java.util.Set;

/**
 * Bot egasi (admin) — obunani sotadigan va dorixona egaligini tasdiqlaydigan shaxs.
 * ADMIN_IDS sozlamasi orqali bir nechta admin belgilanishi mumkin (vergul bilan).
 */
public final class AdminPanel {

    private static final Set<Long> ADMIN_IDS = parse(Config.get("ADMIN_IDS", "8243546845"));

    private AdminPanel() { }

    public static boolean isAdmin(long telegramId) {
        return ADMIN_IDS.contains(telegramId);
    }

    public static Set<Long> all() {
        return ADMIN_IDS;
    }

    private static Set<Long> parse(String value) {
        Set<Long> ids = new HashSet<>();
        for (String part : value.split(",")) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) continue;
            try {
                ids.add(Long.parseLong(trimmed));
            } catch (NumberFormatException e) {
                System.out.println("ADMIN_IDS ichida noto'g'ri qiymat: " + trimmed);
            }
        }
        return ids;
    }
}
