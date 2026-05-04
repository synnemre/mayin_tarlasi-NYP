/**
 * KlasikBoardMode — Klasik Mayın Tarlası Modu
 *
 * Board mantığını delege eden klasik mod katmanı.
 * LeblebiBoardMode ile paralel tasarım:
 *   - Zamanlayıcı (yukarı sayar veya geri sayar)
 *   - Skor hesaplama (zamanlı mod için)
 *   - Oyun durumu yönetimi (kazanma / kaybetme)
 *
 * Bu sınıf tamamen UI-bağımsızdır; JavaFX importu içermez.
 * MinesweeperApp yalnızca getter'lar ve hucreAc() / isaretKoy() üzerinden
 * etkileşime geçer; zamanlayıcı tikleri sureyiGuncelle(int) ile iletilir.
 */
public class KlasikBoardMode {

    // ── Sabit ─────────────────────────────────────────────────────────────────

    /** Preset zorluk tanımı (sadece KlasikBoardMode tarafından kullanılır). */
    public record KlasikAyar(
            String etiket,
            int    satir,
            int    sutun,
            int    mayin,
            boolean geriSayim,
            int    sure
    ) {}

    public static final KlasikAyar[] PRESETLER = {
        new KlasikAyar("😊 Kolay",             8,  8,  8,  false, 0),
        new KlasikAyar("😊⏱ Kolay (Zamanlı)", 8,  8,  8,  true,  120),
        new KlasikAyar("😐 Orta",              10, 10, 15, false, 0),
        new KlasikAyar("😐⏱ Orta (Zamanlı)",  10, 10, 15, true,  180),
        new KlasikAyar("😈 Zor",               16, 16, 40, false, 0),
        new KlasikAyar("😈⏱ Zor (Zamanlı)",   16, 16, 40, true,  300),
    };

    // ── Alan değişkenleri ─────────────────────────────────────────────────────

    private final Board tahta;

    /** true → geri sayım (süre sınırlı); false → kronometre (yukarı sayar). */
    private final boolean geriSayim;

    /**
     * Başlangıç süresi (saniye). Yalnızca geriSayim=true olduğunda anlamlıdır.
     * Sıfırlamada bu değerden geri yüklenir.
     */
    private final int baslangicSuresi;

    /**
     * geriSayim=true  → kalan süreyi tutar (sıfıra iner).
     * geriSayim=false → geçen süreyi tutar (sıfırdan çıkar).
     */
    private int suruclukSure;

    private boolean oyunBitti  = false;
    private boolean kazanildi  = false;

    /**
     * Süre dolduğu için mi bitti?
     * Hem klasikGeriSayim hem de oyunBitti true ise true olur.
     */
    private boolean sureDoldu = false;

    // ── Skor koruma bayrağı ───────────────────────────────────────────────────

    /**
     * Zamanlı modda skor yalnızca bir kez kaydedilmelidir.
     * Bu bayrağı UI katmanı kullanır; burada tutulması taşmayı önler.
     */
    private boolean skorKaydedildi = false;

    // ── Yapıcı ────────────────────────────────────────────────────────────────

    /**
     * @param satirSayisi   Izgara satır sayısı
     * @param sutunSayisi   Izgara sütun sayısı
     * @param mayinSayisi   Toplam mayın sayısı
     * @param geriSayim     true → geri sayım; false → kronometre
     * @param sureSaniye    Geri sayım süresi (geriSayim=false ise görmezden gelinir)
     */
    public KlasikBoardMode(int satirSayisi, int sutunSayisi,
                            int mayinSayisi,
                            boolean geriSayim, int sureSaniye) {
        this.tahta          = new Board(satirSayisi, sutunSayisi, mayinSayisi);
        this.geriSayim      = geriSayim;
        this.baslangicSuresi = geriSayim ? sureSaniye : 0;
        this.suruclukSure   = geriSayim ? sureSaniye : 0;
    }

    // ── Hücre açma ────────────────────────────────────────────────────────────

    /**
     * Bir hücreyi klasik kurallara göre açar.
     *
     * @return true → mayına basıldı ve oyun bitti;
     *         false → güvenli açış ya da zaten açık/işaretli
     */
    public boolean hucreAc(int satir, int sutun) {
        if (oyunBitti || kazanildi) return false;

        boolean mineHit = tahta.ac(satir, sutun, /*useLives=*/false);

        if (mineHit) {
            // Board.ac() klasik modda oyunBitti=true'ya çeker ve tüm mayınları gösterir.
            oyunBitti = true;
            kazanildi = false;
            return true;
        }

        // Kazanma kontrolü
        kazanmaKontrol();
        return false;
    }

    /**
     * Hücrenin işaretini (bayrağını) değiştirir.
     * Oyun bitmişse veya hücre açıksa işlem yapılmaz.
     *
     * @return Yeni işaret durumu (true=işaretlendi, false=işaret kaldırıldı)
     *         veya hücre zaten açıksa/oyun bitmişse null.
     */
    public Boolean isaretKoy(int satir, int sutun) {
        if (oyunBitti || kazanildi) return null;
        Cell hucre = tahta.getHucre(satir, sutun);
        if (hucre.isAcildiMi()) return null;
        hucre.isaretiDegistir();
        return hucre.isIsaretlendi();
    }

    // ── Süre sistemi ──────────────────────────────────────────────────────────

    /**
     * Her saniye çağrılır; geçen/kalan süreyi bir adım günceller.
     *
     * @param gecenSaniye Geçen saniye miktarı (genellikle 1)
     * @return true → bu tick'te süre doldu ve oyun bitti (sadece geri sayım modunda);
     *         false → oyun devam ediyor veya zaten bitmiş
     */
    public boolean sureyiGuncelle(int gecenSaniye) {
        if (oyunBitti || kazanildi) return false;

        if (geriSayim) {
            suruclukSure = Math.max(0, suruclukSure - gecenSaniye);
            if (suruclukSure <= 0) {
                suruclukSure = 0;
                sureDoldu  = true;
                oyunBitti  = true;
                kazanildi  = false;
                tahta.tumMayinlariGoster();
                return true; // süre bu tick'te doldu
            }
        } else {
            suruclukSure += gecenSaniye;
        }
        return false;
    }

    // ── Kazanma kontrolü ─────────────────────────────────────────────────────

    /**
     * Tahtanın tam temizlenip temizlenmediğini kontrol eder.
     * hucreAc() içinden otomatik çağrılır; UI katmanı da çağırabilir.
     *
     * @return true → kazanıldı
     */
    public boolean kazanmaKontrol() {
        if (oyunBitti || suruclukSure == 0 && geriSayim) return false;
        if (tahta.kazanildiMi()) {
            oyunBitti = true;
            kazanildi = true;
        }
        return kazanildi;
    }

    // ── Skor hesaplama ────────────────────────────────────────────────────────

    /**
     * Zamanlı mod skor formülü:
     *   base         = kalanSure × 10
     *   hızBonusu    = max(0, (sureSiniri/3 - gecenSure) × 5)   [erken bitirme ödülü]
     *   zorCarpani   = (mayinSayisi / toplamHucre) × 200         [yüksek yoğunluk = daha fazla puan]
     *   toplam       = base + hızBonusu + zorCarpani
     *
     * geriSayim=false olduğunda 0 döner (puanlanmayan mod).
     */
    public int skorHesapla() {
        if (!geriSayim) return 0;
        int toplamHucre = tahta.getSatirSayisi() * tahta.getSutunSayisi();
        int kalanSure   = suruclukSure;                            // geri sayım → kalan
        int gecenSure   = baslangicSuresi - kalanSure;
        double yogunluk = (double) tahta.getToplamMayin() / toplamHucre;
        int base        = kalanSure * 10;
        int hizBonusu   = Math.max(0, (baslangicSuresi / 3 - gecenSure) * 5);
        int zorCarpan   = (int) (yogunluk * 200);
        return base + hizBonusu + zorCarpan;
    }

    // ── Getter'lar ────────────────────────────────────────────────────────────

    /** Oyun tahtasına erişim (UI katmanı hücre durumlarını okumak için kullanır). */
    public Board   getTahta()           { return tahta; }

    /** Oyun bitti mi? (kaybedildi veya kazanıldı veya süre doldu). */
    public boolean isOyunBitti()        { return oyunBitti; }

    /** Kazanıldı mı? */
    public boolean isKazanildi()        { return kazanildi; }

    /** Süre dolduğu için mi oyun bitti? (sadece geri sayım modunda true olabilir). */
    public boolean isSureDoldu()        { return sureDoldu; }

    /** Zamanlayıcı modu: true → geri sayım; false → kronometre. */
    public boolean isGeriSayim()        { return geriSayim; }

    /**
     * Anlık süre değeri:
     *   geriSayim=true  → kalan süre (saniye)
     *   geriSayim=false → geçen süre (saniye)
     */
    public int     getSuruclukSure()    { return suruclukSure; }

    /** Başlangıç süresi (saniye). geriSayim=false ise 0. */
    public int     getBaslangicSuresi() { return baslangicSuresi; }

    /** Skor zaten kaydedildi mi? (çift kayıt önlemek için UI katmanı kontrol eder). */
    public boolean isSkorKaydedildi()   { return skorKaydedildi; }

    /** Skor kaydedildi bayrağını ayarlar. */
    public void    setSkorKaydedildi(boolean deger) { this.skorKaydedildi = deger; }

    // ── Yardımcı ─────────────────────────────────────────────────────────────

    /**
     * Oyun aktif mi? (bitmemiş ve kazanılmamışsa true)
     * UI katmanı tıklama olaylarını reddetmek için kullanır.
     */
    public boolean isOyunAktif() {
        return !oyunBitti && !kazanildi;
    }

    /**
     * Preset listesinden bir eşleşme arar ve etiketi döner.
     * Eşleşme bulunamazsa boş string döner.
     */
    public String presetEtiketiBul() {
        for (KlasikAyar p : PRESETLER) {
            if (p.geriSayim()
                    && p.satir() == tahta.getSatirSayisi()
                    && p.sutun() == tahta.getSutunSayisi()
                    && p.mayin() == tahta.getToplamMayin()
                    && p.sure()  == baslangicSuresi) {
                return p.etiket();
            }
        }
        return "";
    }
}
