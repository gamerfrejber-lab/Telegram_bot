package com.company.model;

/** Dorixona sotadigan mahsulot (dori yoki tibbiy buyum) va uning ombordagi qoldig'i. */
public class Dori {

    private long id;
    private String nomi;
    private String nomiRu;
    private String ishlabChiqaruvchi;
    private double narx;
    private boolean mavjud = true;
    private long dorixonaId;
    private int qoldiq;
    private int kelgan;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getNomi() { return nomi; }
    public void setNomi(String nomi) { this.nomi = nomi; }

    public String getNomiRu() { return nomiRu; }
    public void setNomiRu(String nomiRu) { this.nomiRu = nomiRu; }

    public String getIshlabChiqaruvchi() { return ishlabChiqaruvchi; }
    public void setIshlabChiqaruvchi(String ishlabChiqaruvchi) { this.ishlabChiqaruvchi = ishlabChiqaruvchi; }

    public double getNarx() { return narx; }
    public void setNarx(double narx) { this.narx = narx; }

    public boolean isMavjud() { return mavjud; }
    public void setMavjud(boolean mavjud) { this.mavjud = mavjud; }

    public long getDorixonaId() { return dorixonaId; }
    public void setDorixonaId(long dorixonaId) { this.dorixonaId = dorixonaId; }

    public int getQoldiq() { return qoldiq; }
    public void setQoldiq(int qoldiq) { this.qoldiq = qoldiq; }

    public int getKelgan() { return kelgan; }
    public void setKelgan(int kelgan) { this.kelgan = kelgan; }

    /**
     * Ombor hisobi yuritilyaptimi. Hech qanday kirim yozilmagan bo'lsa qoldiq 0 chiqadi,
     * lekin bu "tugagan" degani emas — shunchaki hisob yuritilmagan.
     */
    public boolean hisobYuritiladi() { return kelgan > 0; }

    @Override
    public String toString() { return nomi; }
}
