import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD — RED phase.
 * LeblebiBoardMode henüz yok, bu testler kasıtlı olarak FAIL edecek.
 */
class LeblebiBoardModeTest {

    private LeblebiBoardMode mod;

    @BeforeEach
    void setup() {
        // 5x5 tarla, 3 solucan, 60 saniye süre, 3 can
        mod = new LeblebiBoardMode(5, 5, 3, 60, 3);
    }

    // ── 1. Başlangıç durumu ───────────────────────────────────────────────────

    @Test
    void baslangictaCanSayisi3OlmaliTest() {
        assertEquals(3, mod.getCanSayisi(), "Başlangıçta 3 can olmalı");
    }

    @Test
    void baslangictaKalanSure60OlmaliTest() {
        assertEquals(60, mod.getKalanSure(), "Başlangıçta 60 saniye olmalı");
    }

    @Test
    void baslangictaOyunBitmemisOlmaliTest() {
        assertFalse(mod.isOyunBitti(), "Başlangıçta oyun bitmemiş olmalı");
    }

    @Test
    void baslangictaKazanilmamisOlmaliTest() {
        assertFalse(mod.isKazanildi(), "Başlangıçta kazanılmamış olmalı");
    }

    // ── 2. Can sistemi ────────────────────────────────────────────────────────

    @Test
    void solucanaBastirildikcanAzalirTest() {
        int oncekiCan = mod.getCanSayisi();
        mod.solucanaBastir();
        assertEquals(oncekiCan - 1, mod.getCanSayisi(), "Solucana basınca can azalmalı");
    }

    @Test
    void ucCanGidincaOyunBiterTest() {
        mod.solucanaBastir();
        mod.solucanaBastir();
        mod.solucanaBastir();
        assertTrue(mod.isOyunBitti(), "3 can gidince oyun bitmeli");
    }

    @Test
    void canSifirAltinaDusmemeliTest() {
        mod.solucanaBastir();
        mod.solucanaBastir();
        mod.solucanaBastir();
        mod.solucanaBastir(); // 4. basış — can zaten 0
        assertEquals(0, mod.getCanSayisi(), "Can 0'ın altına düşmemeli");
    }

    // ── 3. Süre sistemi ───────────────────────────────────────────────────────

    @Test
    void sureBitinceOyunBiterTest() {
        mod.sureyiGuncelle(60); // 60 saniye geçti
        assertTrue(mod.isOyunBitti(), "Süre bitince oyun bitmeli");
    }

    @Test
    void kalanSureHicbirZamanNegatifOlmamazTest() {
        mod.sureyiGuncelle(999);
        assertEquals(0, mod.getKalanSure(), "Kalan süre negatif olmamalı");
    }

    @Test
    void sureGecmesiOyunBitmemiskenCalismaliTest() {
        mod.sureyiGuncelle(30);
        assertEquals(30, mod.getKalanSure(), "30 saniye geçince 30 kalmalı");
        assertFalse(mod.isOyunBitti(), "30 saniye geçince oyun bitmemeli");
    }

    // ── 4. Kazanma koşulu ─────────────────────────────────────────────────────

    @Test
    void tumSolucanlarIsaretlenipSureDolarkenKazanilamaz() {
        // Süre dolduğunda canlar yerinde olsa bile kaybedilir
        mod.sureyiGuncelle(60);
        assertFalse(mod.isKazanildi(), "Süre dolunca kazanılmamalı");
    }

    @Test
    void sifirlaIncaKalanSureVeCanSifirlaniTest() {
        mod.solucanaBastir();
        mod.sureyiGuncelle(20);
        mod.sifirla();
        assertEquals(3, mod.getCanSayisi(), "Sıfırlamada can 3 olmalı");
        assertEquals(60, mod.getKalanSure(), "Sıfırlamada süre 60 olmalı");
        assertFalse(mod.isOyunBitti(), "Sıfırlamada oyun bitmemiş olmalı");
    }

    // ── 5. Board delegasyonu ─────────────────────────────────────────────────

    @Test
    void getTahtaNoullOlmamazTest() {
        assertNotNull(mod.getTahta(), "getTahta() null döndürmemeli");
    }

    @Test
    void tarlaBoyutuDogruOlmaliTest() {
        Board tahta = mod.getTahta();
        // 5x5 tarla — köşe hücresi erişilebilir olmalı
        assertNotNull(tahta.getHucre(0, 0));
        assertNotNull(tahta.getHucre(4, 4));
    }
}
