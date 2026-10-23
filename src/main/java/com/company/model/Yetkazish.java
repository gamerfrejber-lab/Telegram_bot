package com.company.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Yetkazib berish buyurtmasi. Mijoz saytda buyurtma beradi, dorixona dorini o'zi yetkazadi
 * (o'z kuryeri yoki Yandex Go orqali). Jadval sayt bilan umumiy (yetkazish, yetkazish_qator).
 */
public class Yetkazish {

    public static final String YANGI = "YANGI";
    public static final String QABUL = "QABUL";
    public static final String YOLDA = "YOLDA";
    public static final String YETKAZILDI = "YETKAZILDI";
    public static final String BEKOR = "BEKOR";

    /** Bitta mahsulot qatori. */
    public record Qator(Long doriId, String nomi, int soni, String tur, double narx) { }

    private long id;
    private long dorixonaId;
    private String dorixonaNomi;
    private Long mijozTelegramId;
    private String mijozIsmi;
    private String mijozTelefon;
    private String manzil;
    private String izoh;
    private Double latitude;
    private Double longitude;
    private Double masofaKm;
    private double doriSumma;
    private double yetkazishNarxi;
    private String tolovUsuli;
    private String tolovHolati;
    private String holat = YANGI;
    private String bekorSababi;
    private String kuryerTelefon;
    private Integer taxminiyDaqiqa;
    private String kod;
    private String egagaHodisa;
    private LocalDateTime sana;
    private final List<Qator> qatorlar = new ArrayList<>();

    public double jami() { return doriSumma + yetkazishNarxi; }
    public boolean kartaga() { return "KARTA".equals(tolovUsuli); }
    public boolean faol() { return YANGI.equals(holat) || QABUL.equals(holat) || YOLDA.equals(holat); }

    public String holatMatni(boolean ru) {
        return switch (holat) {
            case YANGI -> ru ? "🆕 Новый — ждёт вашего ответа" : "🆕 Yangi — javobingizni kutmoqda";
            case QABUL -> ru ? "✅ Принят — готовится" : "✅ Qabul qilingan — tayyorlanmoqda";
            case YOLDA -> ru ? "🚚 Курьер в пути" : "🚚 Kuryer yo'lda";
            case YETKAZILDI -> ru ? "📦 Доставлен" : "📦 Yetkazildi";
            case BEKOR -> ru ? "❌ Отменён" : "❌ Bekor qilingan";
            default -> holat;
        };
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getDorixonaId() { return dorixonaId; }
    public void setDorixonaId(long dorixonaId) { this.dorixonaId = dorixonaId; }
    public String getDorixonaNomi() { return dorixonaNomi; }
    public void setDorixonaNomi(String dorixonaNomi) { this.dorixonaNomi = dorixonaNomi; }
    public Long getMijozTelegramId() { return mijozTelegramId; }
    public void setMijozTelegramId(Long mijozTelegramId) { this.mijozTelegramId = mijozTelegramId; }
    public String getMijozIsmi() { return mijozIsmi; }
    public void setMijozIsmi(String mijozIsmi) { this.mijozIsmi = mijozIsmi; }
    public String getMijozTelefon() { return mijozTelefon; }
    public void setMijozTelefon(String mijozTelefon) { this.mijozTelefon = mijozTelefon; }
    public String getManzil() { return manzil; }
    public void setManzil(String manzil) { this.manzil = manzil; }
    public String getIzoh() { return izoh; }
    public void setIzoh(String izoh) { this.izoh = izoh; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public Double getMasofaKm() { return masofaKm; }
    public void setMasofaKm(Double masofaKm) { this.masofaKm = masofaKm; }
    public double getDoriSumma() { return doriSumma; }
    public void setDoriSumma(double doriSumma) { this.doriSumma = doriSumma; }
    public double getYetkazishNarxi() { return yetkazishNarxi; }
    public void setYetkazishNarxi(double yetkazishNarxi) { this.yetkazishNarxi = yetkazishNarxi; }
    public String getTolovUsuli() { return tolovUsuli; }
    public void setTolovUsuli(String tolovUsuli) { this.tolovUsuli = tolovUsuli; }
    public String getTolovHolati() { return tolovHolati; }
    public void setTolovHolati(String tolovHolati) { this.tolovHolati = tolovHolati; }
    public String getHolat() { return holat; }
    public void setHolat(String holat) { this.holat = holat; }
    public String getBekorSababi() { return bekorSababi; }
    public void setBekorSababi(String bekorSababi) { this.bekorSababi = bekorSababi; }
    public String getKuryerTelefon() { return kuryerTelefon; }
    public void setKuryerTelefon(String kuryerTelefon) { this.kuryerTelefon = kuryerTelefon; }
    public Integer getTaxminiyDaqiqa() { return taxminiyDaqiqa; }
    public void setTaxminiyDaqiqa(Integer taxminiyDaqiqa) { this.taxminiyDaqiqa = taxminiyDaqiqa; }
    public String getKod() { return kod; }
    public void setKod(String kod) { this.kod = kod; }
    public String getEgagaHodisa() { return egagaHodisa; }
    public void setEgagaHodisa(String egagaHodisa) { this.egagaHodisa = egagaHodisa; }
    public LocalDateTime getSana() { return sana; }
    public void setSana(LocalDateTime sana) { this.sana = sana; }
    public List<Qator> getQatorlar() { return qatorlar; }
}
