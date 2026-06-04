import javafx.scene.image.Image;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

/**
 * AppAssets — Loads and exposes all media assets (images + sounds).
 *
 * All audio now uses MediaPlayer instead of AudioClip.
 * Reason: AudioClip relies on native audio paths that are unreliable on
 * Windows (driver-dependent MP3 decoding, thread-safety issues).
 * MediaPlayer uses JavaFX's own bundled GStreamer pipeline on every platform
 * and is consistently reliable cross-platform — no codec pack needed.
 *
 * Trade-off: MediaPlayer has slightly higher latency on first play (~50 ms)
 * but this is imperceptible for game SFX. For sounds that may overlap (rapid
 * cell clicks) we keep a small round-robin pool for sesKazma.
 *
 * IMPORTANT: MediaPlayer objects MUST be created on the JavaFX Application
 * Thread. All SFX and BGM creation therefore happens inside Platform.runLater,
 * same as BGM was already doing.
 */
public class AppAssets {

    // ── Asset paths ────────────────────────────────────────────────────────────
    private static final String ASSET_BAYRAK        = "assets/bayrak.png";
    private static final String ASSET_MAYIN         = "assets/mayin.png";
    private static final String ASSET_KALP          = "assets/kalp.png";
    private static final String ASSET_LEBLEBI       = "assets/leblebi.png";
    private static final String ASSET_ALTIN_LEBLEBI = "assets/altin_leblebi.png";
    private static final String ASSET_YILAN         = "assets/yilan.png";
    private static final String ASSET_ALARM         = "assets/alarm.png";
    private static final String ASSET_ALARM2        = "assets/alarm2.png";
    private static final String ASSET_ALTIN         = "assets/altin.png";
    private static final String ASSET_CARPI         = "assets/carpi.png";
    private static final String ASSET_EKSTRA_CAN    = "assets/ekstra_can.png";
    private static final String ASSET_GUNES         = "assets/gunes.png";
    private static final String ASSET_ILAC          = "assets/ilac.png";
    private static final String ASSET_INFO          = "assets/info.png";
    private static final String ASSET_KAFATASI      = "assets/kafatasi.png";
    private static final String ASSET_KARGA         = "assets/karga.png";
    private static final String ASSET_KUM_SAATI     = "assets/kum_saati.png";
    private static final String ASSET_KUPA          = "assets/kupa.png";
    private static final String ASSET_MUTLU         = "assets/mutlu.png";
    private static final String ASSET_OFF_BTN       = "assets/off_btn.png";
    private static final String ASSET_OK            = "assets/ok.png";
    private static final String ASSET_OYUN_BITTI    = "assets/oyun_bitti.png";
    private static final String ASSET_RESET_BTN     = "assets/reset_btn.png";
    private static final String ASSET_SES_AYARI     = "assets/ses_ayari.png";
    private static final String ASSET_SES_KAPA      = "assets/ses_kapa.png";
    private static final String ASSET_TIK           = "assets/tik.png";
    private static final String ASSET_YILDIZ        = "assets/yildiz.png";
    private static final String ASSET_YILDIZ2       = "assets/yildiz2.png";

    // ── Images ─────────────────────────────────────────────────────────────────
    Image imgBayrak;
    Image imgMayin;
    Image imgKalp;
    Image imgLeblebi;
    Image imgAltinLeblebi;
    Image imgYilan;
    Image imgAlarm;
    Image imgAlarm2;
    Image imgAltin;
    Image imgCarpi;
    Image imgEkstraCan;
    Image imgGunes;
    Image imgIlac;
    Image imgInfo;
    Image imgKafatasi;
    Image imgKarga;
    Image imgKumSaati;
    Image imgKupa;
    Image imgMutlu;
    Image imgOffBtn;
    Image imgOk;
    Image imgOyunBitti;
    Image imgResetBtn;
    Image imgSesAyari;
    Image imgSesKapa;
    Image imgTik;
    Image imgYildiz;
    Image imgYildiz2;

    // ── Sound effects (MediaPlayer) ────────────────────────────────────────────
    // sesKazma fires on every cell click — pool of 4 so rapid clicks overlap
    // without cutting each other off. Call playKazma() instead of play(sesKazma).
    private static final int KAZMA_POOL_SIZE = 4;
    private MediaPlayer[]    sesKazmaPool;
    private int              kazmaIdx = 0;

    MediaPlayer sesPatlama;
    MediaPlayer sesButon;
    MediaPlayer sesKazan;
    MediaPlayer sesBasla;

    // Market / Leblebi sounds
    MediaPlayer sesMarket;
    MediaPlayer sesMarketKarga;
    MediaPlayer sesMarketSaat;
    MediaPlayer sesMarketIlac;
    MediaPlayer sesMarketKalp;
    MediaPlayer sesYilan;
    MediaPlayer sesYilanBitis;
    MediaPlayer sesCiftlik;

    // ── Background Music ───────────────────────────────────────────────────────
    MediaPlayer bgmKlasik;
    MediaPlayer bgmSatranc;
    MediaPlayer bgmLeblebi;
    private MediaPlayer aktifBgm;
    private String      bekleyenBgm = null;

    // ── Volume reference ───────────────────────────────────────────────────────
    private AppState appState;

    /** Sets the AppState reference so play() can read sesHacmi / muzikHacmi. */
    public void setAppState(AppState state) {
        this.appState = state;
    }

    // =========================================================================
    // Public API
    // =========================================================================

    /**
     * Kicks off asset loading.
     * Images are loaded on a background thread (they are thread-safe).
     * All MediaPlayer objects are created on the JavaFX Application Thread
     * via Platform.runLater — this is a hard JavaFX requirement.
     */
    public void loadAsync() {
        Thread t = new Thread(() -> {
            imgBayrak       = loadImage(ASSET_BAYRAK,        18);
            imgMayin        = loadImage(ASSET_MAYIN,         20);
            imgKalp         = loadImage(ASSET_KALP,          20);
            imgLeblebi      = loadImage(ASSET_LEBLEBI,       128);
            imgAltinLeblebi = loadImage(ASSET_ALTIN_LEBLEBI, 128);
            imgYilan        = loadImage(ASSET_YILAN,         128);
            imgAlarm        = loadImage(ASSET_ALARM,         20);
            imgAlarm2       = loadImage(ASSET_ALARM2,        20);
            imgAltin        = loadImage(ASSET_ALTIN,         20);
            imgCarpi        = loadImage(ASSET_CARPI,         20);
            imgEkstraCan    = loadImage(ASSET_EKSTRA_CAN,    20);
            imgGunes        = loadImage(ASSET_GUNES,         20);
            imgIlac         = loadImage(ASSET_ILAC,          20);
            imgInfo         = loadImage(ASSET_INFO,          20);
            imgKafatasi     = loadImage(ASSET_KAFATASI,      20);
            imgKarga        = loadImage(ASSET_KARGA,         20);
            imgKumSaati     = loadImage(ASSET_KUM_SAATI,     20);
            imgKupa         = loadImage(ASSET_KUPA,          20);
            imgMutlu        = loadImage(ASSET_MUTLU,         20);
            imgOffBtn       = loadImage(ASSET_OFF_BTN,       20);
            imgOk           = loadImage(ASSET_OK,            20);
            imgOyunBitti    = loadImage(ASSET_OYUN_BITTI,    20);
            imgResetBtn     = loadImage(ASSET_RESET_BTN,     20);
            imgSesAyari     = loadImage(ASSET_SES_AYARI,     20);
            imgSesKapa      = loadImage(ASSET_SES_KAPA,      20);
            imgTik          = loadImage(ASSET_TIK,           20);
            imgYildiz       = loadImage(ASSET_YILDIZ,        20);
            imgYildiz2      = loadImage(ASSET_YILDIZ2,       20);

            // MediaPlayer MUST be constructed on the JavaFX Application Thread
            javafx.application.Platform.runLater(() -> {
                sesKazmaPool = new MediaPlayer[KAZMA_POOL_SIZE];
                for (int i = 0; i < KAZMA_POOL_SIZE; i++)
                    sesKazmaPool[i] = loadSfx("sounds/kazma.mp3");

                sesPatlama     = loadSfx("sounds/patlama.mp3");
                sesButon       = loadSfx("sounds/buton.mp3");
                sesMarket      = loadSfx("sounds/market.mp3");
                sesKazan       = loadSfx("sounds/kazan.mp3");
                sesBasla       = loadSfx("sounds/sesBasla.mp3");
                sesMarketKarga = loadSfx("sounds/karga.mp3");
                sesMarketSaat  = loadSfx("sounds/saat.mp3");
                sesMarketIlac  = loadSfx("sounds/ilac.mp3");
                sesMarketKalp  = loadSfx("sounds/kalp.mp3");
                sesYilan       = loadSfx("sounds/yilan.mp3");
                sesYilanBitis  = loadSfx("sounds/yilan_bitis.mp3");
                sesCiftlik     = loadSfx("sounds/ciftlik.mp3");

                bgmKlasik  = loadBgm("sounds/bgm_klasik.mp3");
                bgmSatranc = loadBgm("sounds/bgm_satranc.mp3");
                bgmLeblebi = loadBgm("sounds/bgm_leblebi.mp3");

                if (bekleyenBgm != null) {
                    playBgm(bekleyenBgm);
                    bekleyenBgm = null;
                }
            });
        }, "asset-loader");
        t.setDaemon(true);
        t.start();
    }

    // ── SFX play helpers ──────────────────────────────────────────────────────

    /**
     * Plays a one-shot SFX at the current global SFX volume.
     * Seeks back to the start first so rapid re-triggers always play from
     * the beginning rather than being silently ignored mid-play.
     */
    public void play(MediaPlayer player) {
        if (player == null) return;
        double vol = (appState != null) ? appState.sesHacmi : 0.7;
        player.setVolume(clamp(vol));
        player.seek(player.getStartTime());
        player.play();
    }

    /**
     * Plays at a fraction of the global SFX volume.
     * e.g. play(clip, 0.5) = half the current global volume.
     */
    public void play(MediaPlayer player, double carpan) {
        if (player == null) return;
        double vol = (appState != null) ? appState.sesHacmi * carpan : 0.7 * carpan;
        player.setVolume(clamp(vol));
        player.seek(player.getStartTime());
        player.play();
    }

    /**
     * Plays the dig/kazma sound via the round-robin pool so overlapping rapid
     * clicks each get their own MediaPlayer instance and don't cut each other off.
     * Use this everywhere instead of play(assets.sesKazma).
     */
    public void playKazma() {
        if (sesKazmaPool == null) return;
        play(sesKazmaPool[kazmaIdx]);
        kazmaIdx = (kazmaIdx + 1) % KAZMA_POOL_SIZE;
    }

    public void stop(MediaPlayer player) {
        if (player != null) player.stop();
    }

    // ── BGM API ────────────────────────────────────────────────────────────────

    public void playBgm(String mod) {
        if (bgmKlasik == null) {
            bekleyenBgm = mod;
            return;
        }
        if (aktifBgm != null) aktifBgm.stop();

        aktifBgm = switch (mod) {
            case "klasik"  -> bgmKlasik;
            case "satranc" -> bgmSatranc;
            case "leblebi" -> bgmLeblebi;
            default        -> null;
        };

        if (aktifBgm != null) {
            updateBgmVolume();
            aktifBgm.play();
        }
    }

    public void stopBgm() {
        if (aktifBgm != null) {
            aktifBgm.stop();
            aktifBgm = null;
        }
    }

    public void updateBgmVolume() {
        if (appState == null) return;
        double v = appState.muzikSessiz ? 0.0 : appState.muzikHacmi;
        setVol(bgmKlasik,  v);
        setVol(bgmSatranc, v);
        setVol(bgmLeblebi, v);
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private static double clamp(double v) { return Math.min(1.0, Math.max(0.0, v)); }

    private static void setVol(MediaPlayer p, double v) {
        if (p != null) p.setVolume(v);
    }

    private Image loadImage(String yol, double boyut) {
        try {
            java.net.URL url = getClass().getResource(yol);
            if (url == null)
                url = java.nio.file.Paths.get(yol).toAbsolutePath().toUri().toURL();
            return new Image(url.toString(), boyut, boyut, true, true, true);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Creates a single-play MediaPlayer for a sound effect.
     * Must be called on the JavaFX Application Thread.
     */
    private MediaPlayer loadSfx(String yol) {
        String uri = resolveUri(yol);
        if (uri == null) {
            System.err.println("[AppAssets] Ses dosyası bulunamadı: " + yol);
            return null;
        }
        try {
            MediaPlayer p = new MediaPlayer(new Media(uri));
            p.setCycleCount(1);
            if (appState != null) p.setVolume(appState.sesHacmi);
            return p;
        } catch (Exception e) {
            System.err.println("[AppAssets] SFX yüklenemedi (" + yol + "): " + e.getMessage());
            return null;
        }
    }

    /**
     * Creates a looping MediaPlayer for background music.
     * Must be called on the JavaFX Application Thread.
     */
    private MediaPlayer loadBgm(String yol) {
        String uri = resolveUri(yol);
        if (uri == null) {
            System.err.println("[AppAssets] BGM bulunamadı: " + yol);
            return null;
        }
        try {
            MediaPlayer p = new MediaPlayer(new Media(uri));
            p.setCycleCount(MediaPlayer.INDEFINITE);
            if (appState != null)
                p.setVolume(appState.muzikSessiz ? 0.0 : appState.muzikHacmi);
            return p;
        } catch (Exception e) {
            System.err.println("[AppAssets] BGM yüklenemedi (" + yol + "): " + e.getMessage());
            return null;
        }
    }

    /**
     * Tries multiple lookup strategies to resolve a relative path to a URI string.
     * Returns null if the file cannot be located anywhere.
     */
    private String resolveUri(String yol) {
        try {
            // 1. Classpath relative to this class
            java.net.URL url = getClass().getResource(yol);
            if (url != null) return url.toExternalForm();

            // 2. Absolute classpath root
            url = getClass().getResource("/" + yol);
            if (url != null) return url.toExternalForm();

            // 3. Relative to the JAR / class output directory
            java.net.URL classLoc = getClass().getProtectionDomain()
                                               .getCodeSource().getLocation();
            if (classLoc != null) {
                java.io.File base = new java.io.File(classLoc.toURI());
                java.io.File f = new java.io.File(base, yol);
                if (f.exists()) return f.toURI().toString();
            }

            // 4. Working directory
            java.io.File f = new java.io.File(yol);
            if (f.exists()) return f.toURI().toString();

            // 5. src/ subdirectory (common IDE run layout)
            f = new java.io.File("src", yol);
            if (f.exists()) return f.toURI().toString();

        } catch (Exception e) {
            System.err.println("[AppAssets] URI çözümleme hatası (" + yol + "): " + e.getMessage());
        }
        return null;
    }
}
