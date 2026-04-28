/**
 * LeblebiBoardMode — Mehmet Emmi'nin Leblebi Tarlası
 *
 * Normal mayın tarlasının üstüne eklenen mod katmanı.
 * Mayin → Solucan 🪱, Bayrak → Çubuk 🥢
 * - Geri sayım timer (başlangıçta verilen saniye)
 * - 3 can sistemi (solucan patladığında can azalır, 0'da oyun biter)
 * - Board mantığını delege eder, UI buna bağlanır
 */
public class LeblebiBoardMode {

    private final Board tahta;
    private final int baslangicSuresi;
    private final int baslangicCan;

    private int kalanSure;
    private int canSayisi;
    private boolean oyunBitti;
    private boolean kazanildi;

    /**
     * @param satirSayisi  Tarla satır sayısı
     * @param sutunSayisi  Tarla sütun sayısı
     * @param solucanSayisi Gizlenmiş solucan (mayin) sayısı
     * @param sureSaniye   Başlangıç geri sayım süresi (saniye)
     * @param canSayisi    Başlangıç can sayısı
     */
    public LeblebiBoardMode(int satirSayisi, int sutunSayisi,
                             int solucanSayisi, int sureSaniye, int canSayisi) {
        this.tahta         = new Board(satirSayisi, sutunSayisi, solucanSayisi);
        this.baslangicSuresi = sureSaniye;
        this.baslangicCan  = canSayisi;
        this.kalanSure     = sureSaniye;
        this.canSayisi     = canSayisi;
        this.oyunBitti     = false;
        this.kazanildi     = false;
    }

    // ── Can sistemi ──────────────────────────────────────────────────────────

    /**
     * Oyuncu bir solucan hücresine bastığında çağrılır.
     * Can azaltır, 0'a düşünce oyunu bitirir.
     */
    public void solucanaBastir() {
        if (oyunBitti) return;
        if (canSayisi > 0) canSayisi--;
        if (canSayisi <= 0) {
            oyunBitti = true;
            kazanildi = false;
        }
    }

    // ── Süre sistemi ─────────────────────────────────────────────────────────

    /**
     * Her saniye UI tarafından çağrılır; geçen saniyeyi kalan süreden düşer.
     * Süre 0'a düşünce oyun kaybedilir.
     *
     * @param gecenSaniye Ne kadar süre geçti
     */
    public void sureyiGuncelle(int gecenSaniye) {
        if (oyunBitti) return;
        kalanSure = Math.max(0, kalanSure - gecenSaniye);
        if (kalanSure <= 0 && !kazanildi) {
            oyunBitti = true;
            kazanildi = false;
        }
    }

    // ── Kazanma kontrolü ────────────────────────────────────────────────────

    /**
     * Tüm solucanlar işaretlenip tüm güvenli hücreler açıldığında
     * ve süre henüz bitmemişse true döner.
     */
    public boolean kazanmaKontrol() {
        if (oyunBitti || kalanSure <= 0) return false;
        boolean boardKazanildi = tahta.kazanildiMi();
        if (boardKazanildi) {
            oyunBitti = true;
            kazanildi = true;
        }
        return boardKazanildi;
    }

    // ── Sıfırlama ────────────────────────────────────────────────────────────

    /**
     * Modu başa alır (yeni Board oluşturmaz, mevcut tahtayı sıfırlar değil —
     * UI yeni mod nesnesi oluşturur; burada sadece sayaçlar sıfırlanır).
     */
    public void sifirla() {
        this.kalanSure = baslangicSuresi;
        this.canSayisi = baslangicCan;
        this.oyunBitti = false;
        this.kazanildi = false;
    }

    // ── Getter'lar ───────────────────────────────────────────────────────────

    public int  getCanSayisi()  { return canSayisi; }
    public int  getKalanSure()  { return kalanSure; }
    public boolean isOyunBitti() { return oyunBitti; }
    public boolean isKazanildi() { return kazanildi; }
    public Board getTahta()      { return tahta; }
}
