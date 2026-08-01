package com.company.flow;

/**
 * Ko'p bosqichli suhbat holati (dorixona qo'shish, egalik arizasi, mahsulot qo'shish,
 * kirim/chiqim). Har bir foydalanuvchi uchun bittadan saqlanadi — shuning uchun
 * bir vaqtda ikki jarayon aralashib ketmaydi.
 */
public class Session {

    public enum Turi {
        DORIXONA_QOSHISH,
        OBUNANI_UZAYTIRISH,
        EGALIK_ARIZASI,
        MAHSULOT_QOSHISH,
        KIRIM,
        CHIQIM
    }

    private final Turi turi;
    private int qadam;

    private String nomi;
    private String nomiRu;
    private String manzil;
    private String telefon;
    private String ishlabChiqaruvchi;
    private double narx;

    private long dorixonaId;
    private String dorixonaNomi;
    private long doriId;
    private String doriNomi;

    private String litsenziyaFileId;
    private String tekshiruvKodi;

    public Session(Turi turi) {
        this.turi = turi;
    }

    public Turi getTuri() { return turi; }

    public int getQadam() { return qadam; }
    public void setQadam(int qadam) { this.qadam = qadam; }
    public void keyingiQadam() { this.qadam++; }

    public String getNomi() { return nomi; }
    public void setNomi(String nomi) { this.nomi = nomi; }

    public String getNomiRu() { return nomiRu; }
    public void setNomiRu(String nomiRu) { this.nomiRu = nomiRu; }

    public String getManzil() { return manzil; }
    public void setManzil(String manzil) { this.manzil = manzil; }

    public String getTelefon() { return telefon; }
    public void setTelefon(String telefon) { this.telefon = telefon; }

    public String getIshlabChiqaruvchi() { return ishlabChiqaruvchi; }
    public void setIshlabChiqaruvchi(String ishlabChiqaruvchi) { this.ishlabChiqaruvchi = ishlabChiqaruvchi; }

    public double getNarx() { return narx; }
    public void setNarx(double narx) { this.narx = narx; }

    public long getDorixonaId() { return dorixonaId; }
    public void setDorixonaId(long dorixonaId) { this.dorixonaId = dorixonaId; }

    public String getDorixonaNomi() { return dorixonaNomi; }
    public void setDorixonaNomi(String dorixonaNomi) { this.dorixonaNomi = dorixonaNomi; }

    public long getDoriId() { return doriId; }
    public void setDoriId(long doriId) { this.doriId = doriId; }

    public String getDoriNomi() { return doriNomi; }
    public void setDoriNomi(String doriNomi) { this.doriNomi = doriNomi; }

    public String getLitsenziyaFileId() { return litsenziyaFileId; }
    public void setLitsenziyaFileId(String litsenziyaFileId) { this.litsenziyaFileId = litsenziyaFileId; }

    public String getTekshiruvKodi() { return tekshiruvKodi; }
    public void setTekshiruvKodi(String tekshiruvKodi) { this.tekshiruvKodi = tekshiruvKodi; }
}
