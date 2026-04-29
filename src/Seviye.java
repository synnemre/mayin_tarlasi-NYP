/**
 * Leblebi Modu seviye tanımları.
 */
public class Seviye {

    public final int numara;
    public final String isim;
    public final int satirSayisi;
    public final int sutunSayisi;
    public final int solucanSayisi;
    public final int sureSaniye;

    public Seviye(int numara, String isim, int satirSayisi, int sutunSayisi,
                  int solucanSayisi, int sureSaniye) {
        this.numara       = numara;
        this.isim         = isim;
        this.satirSayisi  = satirSayisi;
        this.sutunSayisi  = sutunSayisi;
        this.solucanSayisi = solucanSayisi;
        this.sureSaniye   = sureSaniye;
    }

    // ── Seviye listesi ───────────────────────────────────────────────────────

    public static final Seviye[] SEVIYELER = {
        new Seviye(1, "Evin Arka Bahçesi",  8,  8,  5,  90),
        new Seviye(2, "Ana Tarla",          10, 10, 11, 120),
        new Seviye(3, "Bereketli Topraklar",15, 15, 25, 180),
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
