package com.company.model;

import java.time.LocalDate;

/** Dorixona: nomi, manzili va obuna muddati. Egasi tasdiqlangach egasiTelegramId to'ladi. */
public class Dorixona {

    private long id;
    private String nomi;
    private String viloyat;
    private String tuman;
    private String manzil;
    private String telefon;
    private LocalDate obunaBoshlanish;
    private LocalDate obunaTugash;
    private String holat;
    private Long egasiTelegramId;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getNomi() { return nomi; }
    public void setNomi(String nomi) { this.nomi = nomi; }

    public String getViloyat() { return viloyat; }
    public void setViloyat(String viloyat) { this.viloyat = viloyat; }

    public String getTuman() { return tuman; }
    public void setTuman(String tuman) { this.tuman = tuman; }

    public String getManzil() { return manzil; }
    public void setManzil(String manzil) { this.manzil = manzil; }

    public String getTelefon() { return telefon; }
    public void setTelefon(String telefon) { this.telefon = telefon; }

    public LocalDate getObunaBoshlanish() { return obunaBoshlanish; }
    public void setObunaBoshlanish(LocalDate obunaBoshlanish) { this.obunaBoshlanish = obunaBoshlanish; }

    public LocalDate getObunaTugash() { return obunaTugash; }
    public void setObunaTugash(LocalDate obunaTugash) { this.obunaTugash = obunaTugash; }

    public String getHolat() { return holat; }
    public void setHolat(String holat) { this.holat = holat; }

    public Long getEgasiTelegramId() { return egasiTelegramId; }
    public void setEgasiTelegramId(Long egasiTelegramId) { this.egasiTelegramId = egasiTelegramId; }

    /** Obuna hali amal qiladimi (tugash sanasi belgilanmagan bo'lsa cheklovsiz deb qaraladi). */
    public boolean obunaFaol() {
        if (obunaTugash == null) return !"nofaol".equalsIgnoreCase(holat);
        return !obunaTugash.isBefore(LocalDate.now()) && !"nofaol".equalsIgnoreCase(holat);
    }

    /** Obuna tugashiga necha kun qolgani (tugagan bo'lsa manfiy). */
    public long qolganKun() {
        if (obunaTugash == null) return Long.MAX_VALUE;
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), obunaTugash);
    }

    @Override
    public String toString() { return nomi; }
}
