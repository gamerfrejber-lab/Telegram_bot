package com.company.model;

import java.time.LocalDateTime;

/**
 * Bron: mijoz dorini oldindan band qiladi, dorixona uni ajratib qo'yadi.
 * Mijoz dorixonaga borib olib ketish kodini aytadi.
 */
public class Bron {

    /** Yangi bron — dorixona hali javob bermagan. */
    public static final String YANGI = "YANGI";
    /** Dorixona tasdiqladi, mahsulot ajratib qo'yildi. */
    public static final String TAYYOR = "TAYYOR";
    /** Mijoz kelib olib ketdi. */
    public static final String BERILDI = "BERILDI";
    /** Bekor qilindi (mahsulot yo'q yoki mijoz kelmadi). */
    public static final String BEKOR = "BEKOR";

    private long id;
    private long doriId;
    private long dorixonaId;
    private Long mijozTelegramId;
    private String mijozIsmi;
    private String mijozTelefon;
    private int soni;
    private String kod;
    private String holat = YANGI;
    private LocalDateTime sana;

    // Ko'rsatish uchun qo'shimcha ma'lumot (bazadagi bron jadvalida saqlanmaydi).
    private String doriNomi;
    private String dorixonaNomi;
    private double narx;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getDoriId() { return doriId; }
    public void setDoriId(long doriId) { this.doriId = doriId; }

    public long getDorixonaId() { return dorixonaId; }
    public void setDorixonaId(long dorixonaId) { this.dorixonaId = dorixonaId; }

    public Long getMijozTelegramId() { return mijozTelegramId; }
    public void setMijozTelegramId(Long mijozTelegramId) { this.mijozTelegramId = mijozTelegramId; }

    public String getMijozIsmi() { return mijozIsmi; }
    public void setMijozIsmi(String mijozIsmi) { this.mijozIsmi = mijozIsmi; }

    public String getMijozTelefon() { return mijozTelefon; }
    public void setMijozTelefon(String mijozTelefon) { this.mijozTelefon = mijozTelefon; }

    public int getSoni() { return soni; }
    public void setSoni(int soni) { this.soni = soni; }

    public String getKod() { return kod; }
    public void setKod(String kod) { this.kod = kod; }

    public String getHolat() { return holat; }
    public void setHolat(String holat) { this.holat = holat; }

    public LocalDateTime getSana() { return sana; }
    public void setSana(LocalDateTime sana) { this.sana = sana; }

    public String getDoriNomi() { return doriNomi; }
    public void setDoriNomi(String doriNomi) { this.doriNomi = doriNomi; }

    public String getDorixonaNomi() { return dorixonaNomi; }
    public void setDorixonaNomi(String dorixonaNomi) { this.dorixonaNomi = dorixonaNomi; }

    public double getNarx() { return narx; }
    public void setNarx(double narx) { this.narx = narx; }

    /** Holatni odam o'qiy oladigan ko'rinishda (belgisi bilan). */
    public String holatMatni(boolean ru) {
        return switch (holat) {
            case TAYYOR -> ru ? "🟢 Готов к выдаче" : "🟢 Tayyor, olib ketsangiz bo'ladi";
            case BERILDI -> ru ? "✅ Выдан" : "✅ Berildi";
            case BEKOR -> ru ? "❌ Отменён" : "❌ Bekor qilindi";
            default -> ru ? "🕐 Ожидает подтверждения" : "🕐 Dorixona javobi kutilmoqda";
        };
    }
}
