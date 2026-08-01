package com.company.service;

import com.company.controller.Router;
import com.company.model.Soov;
import com.company.repository.SoovRepository;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Egalik arizasi bo'yicha qarorni dorixona egasiga yetkazadi.
 *
 * Admin arizani ikkala botdan ham hal qilishi mumkin, lekin dorixona egasi faqat shu
 * botda bo'ladi — shuning uchun javobni doim shu bot yuboradi. Botlar bir-biriga
 * to'g'ridan-to'g'ri ulanmaydi: mijozlar boti qarorni bazaga yozadi, shu kuzatuvchi esa
 * uni ko'rib egasiga aytadi. Biri o'chib qolsa ham xabar yo'qolmaydi.
 */
public class SoovNotifier {

    private static final int INTERVAL_SEKUND = 15;

    private final Router router;
    private final SoovRepository soovlar = new SoovRepository();
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread thread = new Thread(r, "soov-notifier");
                thread.setDaemon(true);
                return thread;
            });

    public SoovNotifier(Router router) {
        this.router = router;
    }

    public void start() {
        scheduler.scheduleWithFixedDelay(this::tekshir, 8, INTERVAL_SEKUND, TimeUnit.SECONDS);
        System.out.println("Ariza kuzatuvchisi ishga tushdi (har " + INTERVAL_SEKUND + " sekundda).");
    }

    private void tekshir() {
        try {
            List<Soov> qarorlar = soovlar.egagaAytilmaganlar();
            for (Soov soov : qarorlar) {
                // Avval "aytildi" deb belgilaymiz: xato chiqsa ham bitta xabar
                // ikki marta yuborilib ketmasligi muhimroq.
                if (!soovlar.egagaAytildi(soov.getId())) continue;
                router.qarorniEgasigaYetkaz(soov);
            }
        } catch (Exception e) {
            // Baza vaqtincha yetib bormasa ham kuzatuvchi to'xtab qolmasligi kerak.
            System.out.println("Ariza kuzatuvchisida xato: " + e.getMessage());
        }
    }
}
