package com.company.model;

import java.time.LocalDateTime;

/**
 * Egalik arizasi: dorixona egasi "bu dorixona meniki" deb da'vo qiladi va dalil yuboradi.
 *
 * Dalil uch qatlamdan iborat, chunki bittasining o'zi ishonchli emas:
 *  1) Telegram tasdiqlagan telefon raqami — tugma orqali ulashiladi, qo'lda yozib bo'lmaydi;
 *  2) litsenziya/guvohnoma surati — dorixona rasman mavjudligini ko'rsatadi;
 *  3) bir martalik kod yozilgan qog'oz bilan dorixonaning jonli surati — bu eng muhimi:
 *     internetdan olingan yoki eski rasm ish bermaydi, chunki kodni ariza berilgan
 *     paytda bot o'zi beradi va u aynan shu suratda ko'rinishi kerak.
 */
public class Soov {

    public static final String KUTILMOQDA = "KUTILMOQDA";
    public static final String TASDIQLANGAN = "TASDIQLANGAN";
    public static final String RAD = "RAD";

    private long id;
    private long dorixonaId;
    private long telegramId;
    private String ism;
    private String username;
    private String telefon;
    private String litsenziyaFileId;
    private String jonliRasmFileId;
    /** Rasm baytlari — file_id faqat uni olgan botda ishlagani uchun bazada saqlanadi. */
    private byte[] litsenziyaRasm;
    private byte[] jonliRasm;
    private String tekshiruvKodi;
    private String holat = KUTILMOQDA;
    private String adminIzoh;
    private LocalDateTime sana;

    private String dorixonaNomi;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getDorixonaId() { return dorixonaId; }
    public void setDorixonaId(long dorixonaId) { this.dorixonaId = dorixonaId; }

    public long getTelegramId() { return telegramId; }
    public void setTelegramId(long telegramId) { this.telegramId = telegramId; }

    public String getIsm() { return ism; }
    public void setIsm(String ism) { this.ism = ism; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getTelefon() { return telefon; }
    public void setTelefon(String telefon) { this.telefon = telefon; }

    public String getLitsenziyaFileId() { return litsenziyaFileId; }
    public void setLitsenziyaFileId(String litsenziyaFileId) { this.litsenziyaFileId = litsenziyaFileId; }

    public String getJonliRasmFileId() { return jonliRasmFileId; }
    public void setJonliRasmFileId(String jonliRasmFileId) { this.jonliRasmFileId = jonliRasmFileId; }

    public byte[] getLitsenziyaRasm() { return litsenziyaRasm; }
    public void setLitsenziyaRasm(byte[] litsenziyaRasm) { this.litsenziyaRasm = litsenziyaRasm; }

    public byte[] getJonliRasm() { return jonliRasm; }
    public void setJonliRasm(byte[] jonliRasm) { this.jonliRasm = jonliRasm; }

    public String getTekshiruvKodi() { return tekshiruvKodi; }
    public void setTekshiruvKodi(String tekshiruvKodi) { this.tekshiruvKodi = tekshiruvKodi; }

    public String getHolat() { return holat; }
    public void setHolat(String holat) { this.holat = holat; }

    public String getAdminIzoh() { return adminIzoh; }
    public void setAdminIzoh(String adminIzoh) { this.adminIzoh = adminIzoh; }

    public LocalDateTime getSana() { return sana; }
    public void setSana(LocalDateTime sana) { this.sana = sana; }

    public String getDorixonaNomi() { return dorixonaNomi; }
    public void setDorixonaNomi(String dorixonaNomi) { this.dorixonaNomi = dorixonaNomi; }
}
