package com.company.service;

import com.company.controller.Router;
import com.company.model.Bron;
import com.company.repository.BronRepository;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Bron jadvalini kuzatib turadi va yangi bronlarni dorixona egasiga yetkazadi.
 *
 * Ikki bot bir-biriga to'g'ridan-to'g'ri ulanmaydi: mijozlar boti bronni shu jadvalga
 * yozadi, bu yerdagi kuzatuvchi esa uni o'qib egasiga yuboradi. Shuning uchun bironta
 * bot vaqtincha o'chib qolsa ham xabar yo'qolmaydi — u qayta ishga tushganda yetkaziladi.
 */
public class BronNotifier {

    private static final int INTERVAL_SEKUND = 15;

    private final Router router;
    private final BronRepository bronlar = new BronRepository();
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread thread = new Thread(r, "bron-notifier");
                thread.setDaemon(true);
                return thread;
            });

    public BronNotifier(Router router) {
        this.router = router;
    }

    public void start() {
        scheduler.scheduleWithFixedDelay(this::tekshir, 5, INTERVAL_SEKUND, TimeUnit.SECONDS);
        System.out.println("Bron kuzatuvchisi ishga tushdi (har " + INTERVAL_SEKUND + " sekundda).");
    }

    private void tekshir() {
        try {
            List<Bron> yangilar = bronlar.yetkazilmaganlar();
            for (Bron bron : yangilar) {
                // Avval yetkazildi deb belgilaymiz: agar shu payt xato chiqsa ham
                // bitta bron ikki marta yuborilib ketmasligi muhimroq.
                if (!bronlar.egagaYetkazildi(bron.getId())) continue;
                if (!router.bronniEgasigaYetkaz(bron)) {
                    System.out.println("Bron №" + bron.getId() + ": dorixona egasi hali ulanmagan.");
                }
            }
        } catch (Exception e) {
            // Baza vaqtincha yetib bormasa ham kuzatuvchi to'xtab qolmasligi kerak.
            System.out.println("Bron kuzatuvchisida xato: " + e.getMessage());
        }
    }
}
