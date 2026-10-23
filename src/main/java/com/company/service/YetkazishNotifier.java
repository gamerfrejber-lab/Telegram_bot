package com.company.service;

import com.company.controller.Router;
import com.company.model.Yetkazish;
import com.company.repository.YetkazishRepository;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Yetkazish jadvalini kuzatadi: saytdan kelgan yangi buyurtma, to'lov cheki yoki mijoz
 * bekor qilgani haqida dorixona egasiga xabar beradi (BronNotifier bilan bir xil usul).
 */
public class YetkazishNotifier {

    private static final int INTERVAL_SEKUND = 15;

    private final Router router;
    private final YetkazishRepository yetkazishlar = new YetkazishRepository();
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread thread = new Thread(r, "yetkazish-notifier");
                thread.setDaemon(true);
                return thread;
            });

    public YetkazishNotifier(Router router) {
        this.router = router;
    }

    public void start() {
        scheduler.scheduleWithFixedDelay(this::tekshir, 7, INTERVAL_SEKUND, TimeUnit.SECONDS);
        System.out.println("Yetkazish kuzatuvchisi ishga tushdi (har " + INTERVAL_SEKUND + " sekundda).");
    }

    private void tekshir() {
        try {
            for (Yetkazish y : yetkazishlar.yetkazilmaganlar()) {
                // Avval belgilaymiz: xato chiqsa ham bitta xabar ikki marta ketmasligi muhimroq.
                if (!yetkazishlar.egagaAytildi(y.getId())) continue;
                router.yetkazishniEgasigaYetkaz(y);
            }
        } catch (Exception e) {
            // Jadval hali yaratilmagan bo'lsa ham (sayt birinchi marta ishga tushmagan) kuzatuvchi to'xtamaydi.
            System.out.println("Yetkazish kuzatuvchisida xato: " + e.getMessage());
        }
    }
}
