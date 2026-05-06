/**
 * Leblebi Modu seviye tanımları.
 *
 * FIX (quality): Fields are now private final with public getters.
 * Although the fields were already declared final (preventing reassignment),
 * keeping them public exposes implementation details and breaks encapsulation.
 * Callers that previously accessed seviye.isim now use seviye.getIsim(), etc.
 */
public class Seviye {

    private final int    numara;
    private final String isim;
    private final int    satirSayisi;
    private final int    sutunSayisi;
    private final int    solucanSayisi;
    private final int    sureSaniye;

    public Seviye(int numara, String isim, int satirSayisi, int sutunSayisi,
                  int solucanSayisi, int sureSaniye) {
        this.numara        = numara;
        this.isim          = isim;
        this.satirSayisi   = satirSayisi;
        this.sutunSayisi   = sutunSayisi;
        this.solucanSayisi = solucanSayisi;
        this.sureSaniye    = sureSaniye;
    }

    public int    getNumara()        { return numara; }
    public String getIsim()          { return isim; }
    public int    getSatirSayisi()   { return satirSayisi; }
    public int    getSutunSayisi()   { return sutunSayisi; }
    public int    getSolucanSayisi() { return solucanSayisi; }
    public int    getSureSaniye()    { return sureSaniye; }

    // ── Seviye listesi ───────────────────────────────────────────────────────

    public static final Seviye[] SEVIYELER = {
        new Seviye(1, "Evin Arka Bahçesi",   8,  8,  9, 120),
        new Seviye(2, "Ana Tarla",           10, 10, 15, 180),
        new Seviye(3, "Bereketli Topraklar", 15, 15, 35, 240),
    };

    public static Seviye getSeviye(int numara) {
        for (Seviye s : SEVIYELER)
            if (s.numara == numara) return s;
        return SEVIYELER[0];
    }

    public static boolean sonSeviyeMi(int numara) {
        return numara >= SEVIYELER.length;
    }
}
