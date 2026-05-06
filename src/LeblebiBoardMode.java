import java.security.SecureRandom;
import java.util.EnumMap;
import java.util.Map;

/**
 * LeblebiBoardMode — Mehmet Emmi'nin Leblebi Tarlası
 *
 * Board mantığını delege eden mod katmanı.
 * Mayin → Yılan, Bayrak → Çubuk
 * - Geri sayım timer
 * - Can sistemi  (mine hits cost a life; game only ends at 0 lives)
 * - Leblebi Puanı (her açılan güvenli hücre +1)
 * - Market item entegrasyonu (Karga, Emmi'nin Saati, Zirai İlaç, Ekstra Kalp)
 * - Diyalog sistemi (Mehmet Emmi konuşma balonları)
 * - Hasat raporu (level sonu istatistik verisi)
 */
public class LeblebiBoardMode {

    // ── Diyalog sistemi ───────────────────────────────────────────────────────

    /** Her oyun olayına karşılık gelen diyalog tetikleyicileri. */
    public enum DiyalogTetikleyici {
        LEVEL_BASI, CAN_KAYBI, KAZANMA, KAYBETME, ITEM_KULLANIMI
    }

    private static final Map<DiyalogTetikleyici, String[]> DIYALOG_HAVUZU;
    static {
        DIYALOG_HAVUZU = new EnumMap<>(DiyalogTetikleyici.class);
        DIYALOG_HAVUZU.put(DiyalogTetikleyici.LEVEL_BASI, new String[]{
            "Haydi evladım! Tarlamı yılanlardan temizle!",
            "Bu yılanlar leblebi tarlama girdi, defet şunları!",
            "Dikkat et, yılanlar sinsi olur. Acele etme!",
            "Bismillah! Bu tarla senin elinde, yılan bırakma!"
        });
        DIYALOG_HAVUZU.put(DiyalogTetikleyici.CAN_KAYBI, new String[]{
            "Eyvah! Yılana bastın evladım!",
            "Dur dur, bak nereye basıyorsun!",
            "O yılan seni bekliyordu, dikkat et!",
            "Canın gitti ama vazgeçme, hadi devam!"
        });
        DIYALOG_HAVUZU.put(DiyalogTetikleyici.KAZANMA, new String[]{
            "Aferin! Tarla yılanlardan temizlendi!",
            "İşte bu! Mehmet Emmi gurur duyuyor!",
            "Helal olsun, yılanların hepsini buldun!",
            "Bravo! Bu tarla artık güvende, çok iyisin!"
        });
        DIYALOG_HAVUZU.put(DiyalogTetikleyici.KAYBETME, new String[]{
            "Ah evladım, yılanlar kazandı bu sefer.",
            "Üzme kendini, bir dahaki sefere daha dikkatli ol.",
            "Tarla yılanlara kaldı... bir daha dene!",
            "Bu yılanlar çok sinsi çıktı, hakkını yediler."
        });
        DIYALOG_HAVUZU.put(DiyalogTetikleyici.ITEM_KULLANIMI, new String[]{
            "Güzel hamle! İlaç yılanları ezer!",
            "Karga geldi, yılanları görür o!",
            "Saatim biraz tutmuyor ama devam et!",
            "Para harcadın ama değer, hadi bakalım!"
        });
    }

    private static final SecureRandom RASGELESEC = new SecureRandom();

    private String               aktifDiyalog      = null;
    private DiyalogTetikleyici   aktifTetikleyici  = null;

    private final Board tahta;
    private final int   baslangicSuresi;
    private final int   baslangicCan;

    private int     kalanSure;
    private int     canSayisi;
    private int     leblebPuani;
    private int     altin;
    private boolean oyunBitti;
    private boolean kazanildi;
    
    // Kalıcı eşya kullanımları (Seviye hesabı için)
    private int     kargaKullanimToplami;
    private int     ilacKullanimToplami;

    // Görev Sistemi
    public enum GorevTipi { HIZLI_BITIR, KARGA_YOK, COKLU_HUCRE, YILAN_AVCISI }
    public static class Gorev {
        public final GorevTipi tip;
        public final String aciklama;
        public final int puanOdulu;
        public final int altinOdulu;
        public boolean tamamlandi = false;
        public Gorev(GorevTipi tip, String aciklama, int puanOdulu, int altinOdulu) {
            this.tip = tip; this.aciklama = aciklama; this.puanOdulu = puanOdulu; this.altinOdulu = altinOdulu;
        }
    }
    private final java.util.List<Gorev> aktifGorevler = new java.util.ArrayList<>();

    // Zirai İlaç aktif mi? (bir sonraki tıklamaya uygulanacak)
    private boolean zirayiIlacAktif = false;

    // Karga: gösterilen yılan koordinatları (boş = pasif)
    private java.util.List<int[]> kargaGosterilenMayinlar = new java.util.ArrayList<>();
    
    // Son tıklanan konum (Karga Level 3 için)
    private int sonTiklananSatir = -1;
    private int sonTiklananSutun = -1;

    // ── Hasat raporu sayaçları ───────────────────────────────────────────────
    private int acilanHucreSayisi   = 0;  // doğrudan tıklama ile açılan güvenli hücre
    private int hucrePuaniKazanildi = 0;  // yalnızca hücre açılışından kazanilan puan (market hariç)
    private int yokEdilenYilan      = 0;  // zirai ilaçla yok edilen yılan
    private int altinLeblebiBulundu = 0;  // bulunan altın leblebi sayısı
    private int kullanilanKarga     = 0;
    private int kullanilanSaat      = 0;
    private int kullanilanIlac      = 0;
    private int kullanilanKalp      = 0;

    public LeblebiBoardMode(int satirSayisi, int sutunSayisi,
                             int solucanSayisi, int sureSaniye, int baslangicCan,
                             int baslangicAltin, int toplamKargaKullanim, int toplamIlacKullanim) {
        this.tahta           = new Board(satirSayisi, sutunSayisi, solucanSayisi);
        this.baslangicSuresi = sureSaniye;
        this.baslangicCan    = baslangicCan;
        this.kalanSure       = sureSaniye;
        this.canSayisi       = baslangicCan;
        this.leblebPuani     = 0;
        this.altin           = baslangicAltin;
        this.kargaKullanimToplami = toplamKargaKullanim;
        this.ilacKullanimToplami  = toplamIlacKullanim;
        this.oyunBitti       = false;
        this.kazanildi       = false;
        
        gorevleriBelirle();
    }
    
    private void gorevleriBelirle() {
        java.util.List<Gorev> havuz = new java.util.ArrayList<>();
        havuz.add(new Gorev(GorevTipi.COKLU_HUCRE, "10 Hücre Aç", 50, 5));
        havuz.add(new Gorev(GorevTipi.YILAN_AVCISI, "İlaçla 3 Yılan Avla", 80, 8));
        havuz.add(new Gorev(GorevTipi.KARGA_YOK, "Karga Kullanma", 100, 10));
        havuz.add(new Gorev(GorevTipi.HIZLI_BITIR, "Sürenin Yarısında Bitir", 150, 15));
        java.util.Collections.shuffle(havuz, RASGELESEC);
        for(int i=0; i<Math.min(3, havuz.size()); i++) {
            aktifGorevler.add(havuz.get(i));
        }
    }

    // ── Hücre açma (lives-aware) ─────────────────────────────────────────────

    /**
     * Main entry point for opening a cell.
     * Returns true if a mine was hit (caller can trigger visual/sound feedback).
     * The board does NOT end the game on mine hit; instead a life is deducted here
     * and the mine is removed so the player can continue from the same cell.
     *
     * FIX (quality): kazanmaKontrol() is now called here so the win state is always
     * updated after every open — callers no longer need to remember to do it.
     */
    public boolean hucreAc(int satir, int sutun) {
        if (oyunBitti || kazanildi) return false;

        sonTiklananSatir = satir;
        sonTiklananSutun = sutun;

        // BUG 3 FIX: zirayiIlacAktif=true iken bu metot çağrılırsa
        // ilaç otomatik uygulanır ve devre dışı kalır; UI'a bırakılmaz.
        if (zirayiIlacAktif) {
            int cap = (getIlacLevel() == 1) ? 1 : ((getIlacLevel() == 2) ? 2 : 100);
            int yokEdilen = tahta.zirayiIlacUygula(satir, sutun, cap);
            yokEdilenYilan += yokEdilen;
            zirayiIlacKullanildi();
            kazanmaKontrol();
            return false;
        }

        boolean ilkTi = tahta.isIlkTiklama();
        boolean mineHit = tahta.ac(satir, sutun, /*useLives=*/true);

        if (ilkTi) {
            // İlk tıklamada mayınlar yerleştikten sonra altın leblebileri yerleştir
            int toplamGuvenli = (tahta.getSatirSayisi() * tahta.getSutunSayisi()) - tahta.getToplamMayin();
            int altinAdet = Math.max(1, Math.min(5, (int)(toplamGuvenli * 0.04)));
            tahta.altinLeblebileriYerlestir(altinAdet);
        }

        if (mineHit) {
            // Deduct a life
            canSayisi--;
            if (canSayisi <= 0) {
                canSayisi = 0;
                oyunBitti = true;
                kazanildi = false;
                // Reveal all remaining mines so the player can see them
                tahta.tumMayinlariGoster();
            } else {
                // Recover: remove mine, reopen cell safely so play continues
                tahta.mineHitRecover(satir, sutun);
            }
        } else {
            // Clear karga highlight once the player opens any safe cell
            kargayiTemizle();
            // Güvenli hücre açıldı: puan + hasat sayıcı
            puanEkle(1);
            acilanHucreSayisi++;
        }

        // Always check win condition after a successful open
        kazanmaKontrol();

        return mineHit;
    }

    // ── Süre sistemi ─────────────────────────────────────────────────────────

    /**
     * Updates the countdown timer by the given number of elapsed seconds.
     *
     * FIX (quality): Now returns true if the timer hit zero this tick (game just
     * ended due to timeout), so callers can react immediately without polling
     * isOyunBitti() separately. Returning false means nothing changed.
     */
    public boolean sureyiGuncelle(int gecenSaniye) {
        if (oyunBitti || kazanildi) return false;
        kalanSure = Math.max(0, kalanSure - gecenSaniye);
        if (kalanSure <= 0) {
            oyunBitti = true;
            kazanildi = false;
            return true;  // game just ended — notify caller
        }
        return false;
    }

    // ── Altın Sistemi ────────────────────────────────────────────────────────

    public void altinEkle(int miktar) {
        altin += miktar;
    }

    public boolean altinHarca(int miktar) {
        if (altin >= miktar) {
            altin -= miktar;
            return true;
        }
        return false;
    }

    public int getAltin() {
        return altin;
    }

    // ── Puan sistemi ─────────────────────────────────────────────────────────

    public void puanEkle(int miktar) {
        leblebPuani += miktar;
    }

    /**
     * Hücre açılışından kazanilan puanı ekle.
     * Her 10 puanda 1 altın kazandırır.
     */
    public void hucrePuaniEkle(int miktar) {
        int onceki = hucrePuaniKazanildi;
        leblebPuani         += miktar;
        hucrePuaniKazanildi += miktar;
        
        int kazanilanAltin = (hucrePuaniKazanildi / 10) - (onceki / 10);
        if (kazanilanAltin > 0) {
            altinEkle(kazanilanAltin);
        }
    }

    // ── Market Aksiyonları ve Item Upgrade'leri ──────────────────────────────

    public int getKargaLevel() {
        if (kargaKullanimToplami < 2) return 1;
        if (kargaKullanimToplami < 4) return 2;
        return 3;
    }
    
    public int getIlacLevel() {
        if (ilacKullanimToplami < 2) return 1;
        if (ilacKullanimToplami < 4) return 2;
        return 3;
    }

    /**
     * Karga (20 Altın): Seviyeye göre yılan(lar) gösterir.
     */
    public java.util.List<int[]> kargaKullan() {
        if (!altinHarca(20)) return null;
        java.util.List<int[]> konumlar = tahta.kargaGorus(getKargaLevel(), sonTiklananSatir, sonTiklananSutun);
        if (konumlar == null || konumlar.isEmpty()) {
            altinEkle(20); // mayın yoksa iade
            return null;
        }
        kargaGosterilenMayinlar.addAll(konumlar);
        kullanilanKarga++;
        kargaKullanimToplami++;
        return konumlar;
    }

    public void kargayiTemizle() {
        kargaGosterilenMayinlar.clear();
    }

    /**
     * Emmi'nin Saati (30 Altın): +30 seconds.
     */
    public boolean emmininSaatiniKullan() {
        if (!altinHarca(30)) return false;
        kalanSure += 30;
        kullanilanSaat++;
        return true;
    }

    /**
     * Zirai İlaç (50 Altın): arms the spray for the next click.
     */
    public boolean zirayiIlacAktiflesir() {
        if (!altinHarca(50)) return false;
        zirayiIlacAktif = true;
        kullanilanIlac++;
        ilacKullanimToplami++;
        return true;
    }

    public void zirayiIlacKullanildi() {
        zirayiIlacAktif = false;
    }

    /**
     * Ekstra Kalp (100 Altın): +1 life.
     */
    public boolean ekstraKalpAl() {
        if (!altinHarca(100)) return false;
        canSayisi++;
        kullanilanKalp++;
        return true;
    }

    // ── Kazanma kontrolü ────────────────────────────────────────────────────

    /**
     * Checks whether the board is fully cleared and sets the win flag if so.
     * Called internally by hucreAc() after every safe open; may also be called
     * externally (e.g. from the UI's arayuzuGuncelle loop) without side effects.
     */
    public boolean kazanmaKontrol() {
        if (oyunBitti) return false;
        if (tahta.kazanildiMi()) {
            oyunBitti = true;
            kazanildi = true;
            
            // Kazanıldığında görevleri (Missions) kontrol et
            for (Gorev g : aktifGorevler) {
                if (g.tip == GorevTipi.COKLU_HUCRE && acilanHucreSayisi >= 10) g.tamamlandi = true;
                if (g.tip == GorevTipi.YILAN_AVCISI && yokEdilenYilan >= 3) g.tamamlandi = true;
                if (g.tip == GorevTipi.KARGA_YOK && kullanilanKarga == 0) g.tamamlandi = true;
                if (g.tip == GorevTipi.HIZLI_BITIR && kalanSure >= (baslangicSuresi / 2)) g.tamamlandi = true;
                
                if (g.tamamlandi) {
                    puanEkle(g.puanOdulu);
                    altinEkle(g.altinOdulu);
                }
            }
        }
        return kazanildi;
    }

    // ── Hasat raporu ──────────────────────────────────────────────────────────

    /**
     * Level sonu hasat raporu — saf veri, UI bağımsız.
     *
     * @param kalanSure      hesaplama anındaki kalan süre (saniye)
     * @param surePuani      kalanSure * 5
     * @param acilanHucre    oyuncunun tıkladığı güvenli hücre sayısı
     * @param hucrePuani     acilanHucre başına 1 puan (flood-fill dahil leblebPuani)
     * @param yokEdilenYilan zirai ilaçla yok edilen yılan sayısı
     * @param yilanPuani     yokEdilenYilan * 10
     * @param kullanilanKarga karga kullanım sayısı
     * @param kullanilanSaat  saat kullanım sayısı
     * @param kullanilanIlac  ilaç kullanım sayısı
     * @param kullanilanKalp  kalp kullanım sayısı
     * @param toplamPuan      bu levelin toplam puanı
     * @param emmiYorumu      Mehmet Emmi'nin puana göre yorumu
     */
    public record HasatRaporu(
        int    kalanSure,
        int    surePuani,
        int    acilanHucreSayisi,
        int    hucrePuani,
        int    yokEdilenYilan,
        int    yilanPuani,
        int    altinLeblebiBulundu,
        int    kullanilanKarga,
        int    kullanilanSaat,
        int    kullanilanIlac,
        int    kullanilanKalp,
        int    toplamPuan,
        String emmiYorumu,
        java.util.List<Gorev> tamamlananGorevler,
        java.util.List<String> gizliBasarimlar
    ) {}

    /**
     * Mevcut oyun durumuna göre HasatRaporu oluşturur.
     * Oyun bitmeden de çağrılabilir (anlık istatistik).
     */
    public HasatRaporu hasatRaporuOlustur() {
        int sp   = kalanSure * 5;
        int hp   = hucrePuaniKazanildi;
        int yp   = yokEdilenYilan * 10;
        int ap   = altinLeblebiBulundu * 10;
        int top  = sp + hp + yp + ap;

        java.util.List<Gorev> tGorevler = new java.util.ArrayList<>();
        for (Gorev g : aktifGorevler) {
            if (g.tamamlandi) tGorevler.add(g);
        }

        java.util.List<String> basarimlar = new java.util.ArrayList<>();
        if (kullanilanKarga == 0 && kullanilanIlac == 0 && kullanilanSaat == 0 && kullanilanKalp == 0 && kazanildi) {
            basarimlar.add("Körü Körüne");
        }
        if (kalanSure <= 30 && kazanildi) {
            basarimlar.add("Aceleci Emmi");
        }
        if (baslangicCan - canSayisi >= 3 && kazanildi) {
            basarimlar.add("Sağlam Yürek");
        }
        if (top >= 200) {
            basarimlar.add("Leblebi Ustası");
        }

        String yorum;
        if (top >= 1000) yorum = "Leblebi üstadı! Mehmet Emmi'nin gözleri doldu!";
        else if (top >= 700) yorum = "Mükemmel! Tarla el gibi pırıl pırıl!";
        else if (top >= 400) yorum = "Güzel iş evladım, devam et böyle!";
        else if (top >= 200) yorum = "Fena değil, ama daha çok çalışmak lazım.";
        else                 yorum = "Yılanlar çok uğraştırdı bizi bu sefer...";

        return new HasatRaporu(
            kalanSure, sp,
            acilanHucreSayisi, hp,
            yokEdilenYilan, yp,
            altinLeblebiBulundu,
            kullanilanKarga, kullanilanSaat, kullanilanIlac, kullanilanKalp,
            top, yorum, tGorevler, basarimlar
        );
    }

    // ── Skor hesaplama (eski API — hasat raporu ile örtüşecek şekilde güncellendi) ──

    /**
     * Final score for this level: leblebPuani * 10 + kalanSure * 5
     */
    public int finalSkoruHesapla() {
        return leblebPuani * 10 + kalanSure * 5;
    }

    // ── Diyalog API'si ────────────────────────────────────────────────────────

    /**
     * Verilen tetikleyici için havuzdan rastgele bir diyalog seçer.
     * Sonuç getAktifDiyalog() ile okunabilir.
     */
    public void diyalogTetikle(DiyalogTetikleyici tetikleyici) {
        String[] secenekler = DIYALOG_HAVUZU.get(tetikleyici);
        if (secenekler == null || secenekler.length == 0) return;
        aktifTetikleyici = tetikleyici;
        aktifDiyalog     = secenekler[RASGELESEC.nextInt(secenekler.length)];
    }

    /** Son tetiklenen diyalog metnini döner; yoksa null. */
    public String getAktifDiyalog() { return aktifDiyalog; }

    /** Aktif diyalogu temizler (UI animasyonu bittikten sonra çağrılmalı). */
    public void diyaloguTemizle() {
        aktifDiyalog     = null;
        aktifTetikleyici = null;
    }

    // ── Getter'lar ───────────────────────────────────────────────────────────

    public int                   getCanSayisi()              { return canSayisi; }
    public int                   getKalanSure()              { return kalanSure; }
    public int                   getLeblebPuani()            { return leblebPuani; }
    public boolean               isOyunBitti()               { return oyunBitti; }
    public boolean               isKazanildi()               { return kazanildi; }
    public Board                 getTahta()                  { return tahta; }
    public boolean               isZirayiIlacAktif()         { return zirayiIlacAktif; }
    public java.util.List<int[]> getKargaGosterilenMayinlar(){ return kargaGosterilenMayinlar; }
    public DiyalogTetikleyici    getAktifTetikleyici()       { return aktifTetikleyici; }
    public int                   getKargaKullanimToplami()   { return kargaKullanimToplami; }
    public int                   getIlacKullanimToplami()    { return ilacKullanimToplami; }
    public java.util.List<Gorev> getAktifGorevler()          { return aktifGorevler; }
    
    public void altinLeblebiBulundu() {
        altinLeblebiBulundu++;
    }
}
