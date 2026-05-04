/**
 * LeblebiBoardMode — Mehmet Emmi'nin Leblebi Tarlası
 *
 * Board mantığını delege eden mod katmanı.
 * Mayin → Solucan, Bayrak → Çubuk
 * - Geri sayım timer
 * - Can sistemi  (mine hits cost a life; game only ends at 0 lives)
 * - Leblebi Puanı (her açılan güvenli hücre +1)
 * - Market item entegrasyonu (Karga, Emmi'nin Saati, Zirai İlaç, Ekstra Kalp)
 */
public class LeblebiBoardMode {

    private final Board tahta;
    private final int   baslangicSuresi;
    private final int   baslangicCan;

    private int     kalanSure;
    private int     canSayisi;
    private int     leblebPuani;
    private boolean oyunBitti;
    private boolean kazanildi;

    // Zirai İlaç aktif mi? (bir sonraki tıklamaya uygulanacak)
    private boolean zirayiIlacAktif = false;

    // Karga: gösterilen solucanın koordinatı (null = pasif)
    private int[] kargaGosterilenMayin = null;

    public LeblebiBoardMode(int satirSayisi, int sutunSayisi,
                             int solucanSayisi, int sureSaniye, int baslangicCan) {
        this.tahta           = new Board(satirSayisi, sutunSayisi, solucanSayisi);
        this.baslangicSuresi = sureSaniye;
        this.baslangicCan    = baslangicCan;
        this.kalanSure       = sureSaniye;
        this.canSayisi       = baslangicCan;
        this.leblebPuani     = 0;
        this.oyunBitti       = false;
        this.kazanildi       = false;
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

        boolean mineHit = tahta.ac(satir, sutun, /*useLives=*/true);

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

    // ── Puan sistemi ─────────────────────────────────────────────────────────

    public void puanEkle(int miktar) {
        leblebPuani += miktar;
    }

    // ── Market Aksiyonları ───────────────────────────────────────────────────

    /**
     * Karga (15 puan): returns coordinates of a random mine.
     */
    public int[] kargaKullan() {
        if (leblebPuani < 15) return null;
        int[] konum = tahta.rastgeleMayinBul();
        if (konum == null) return null;
        leblebPuani -= 15;
        kargaGosterilenMayin = konum;
        return konum;
    }

    public void kargayiTemizle() {
        kargaGosterilenMayin = null;
    }

    /**
     * Emmi'nin Saati (20 puan): +30 seconds.
     */
    public boolean emmininSaatiniKullan() {
        if (leblebPuani < 20) return false;
        leblebPuani -= 20;
        kalanSure += 30;
        return true;
    }

    /**
     * Zirai İlaç (30 puan): arms the spray for the next click.
     */
    public boolean zirayiIlacAktiflesir() {
        if (leblebPuani < 30) return false;
        leblebPuani -= 30;
        zirayiIlacAktif = true;
        return true;
    }

    public void zirayiIlacKullanildi() {
        zirayiIlacAktif = false;
    }

    /**
     * Ekstra Kalp (50 puan): +1 life.
     */
    public boolean ekstraKalpAl() {
        if (leblebPuani < 50) return false;
        leblebPuani -= 50;
        canSayisi++;
        return true;
    }

    // ── Kazanma kontrolü ────────────────────────────────────────────────────

    /**
     * Checks whether the board is fully cleared and sets the win flag if so.
     * Called internally by hucreAc() after every safe open; may also be called
     * externally (e.g. from the UI's arayuzuGuncelle loop) without side effects.
     */
    public boolean kazanmaKontrol() {
        if (oyunBitti || kalanSure <= 0) return false;
        if (tahta.kazanildiMi()) {
            oyunBitti = true;
            kazanildi = true;
        }
        return kazanildi;
    }

    // ── Skor hesaplama ───────────────────────────────────────────────────────

    /**
     * Final score for this level: leblebPuani * 10 + kalanSure * 5
     */
    public int finalSkoruHesapla() {
        return leblebPuani * 10 + kalanSure * 5;
    }

    // ── Getter'lar ───────────────────────────────────────────────────────────

    public int     getCanSayisi()              { return canSayisi; }
    public int     getKalanSure()              { return kalanSure; }
    public int     getLeblebPuani()            { return leblebPuani; }
    public boolean isOyunBitti()               { return oyunBitti; }
    public boolean isKazanildi()               { return kazanildi; }
    public Board   getTahta()                  { return tahta; }
    public boolean isZirayiIlacAktif()         { return zirayiIlacAktif; }
    public int[]   getKargaGosterilenMayin()   { return kargaGosterilenMayin; }
}
