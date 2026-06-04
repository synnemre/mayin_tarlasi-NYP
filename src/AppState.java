import javafx.animation.Animation;
import javafx.animation.Timeline;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.util.HashSet;
import java.util.Set;

/**
 * AppState — Shared mutable application state.
 *
 * A single instance of this class is created in MinesweeperApp.start()
 * and passed to every module (MenuView, GameSceneBuilder, GameController).
 * No module holds the primary copy of these fields; they all read/write
 * through this object, so there is only one source of truth.
 *
 * Fields are package-visible (no getter boilerplate) because all modules
 * live in the same unnamed package, exactly like the original monolith.
 */
public class AppState {

    // ── Window / scene ─────────────────────────────────────────────────────────
    Stage pencere;
    Scene rootScene;
    StackPane rootWrapper;

    // ── Mode flags ─────────────────────────────────────────────────────────────
    boolean leblebModu = false;
    boolean satranModu = false;
    boolean karanlikTema = true;
    
    // Ses Kontrolleri
    public double sesHacmi = 0.5; // Efekt sesleri (0.0 - 1.0)
    public double muzikHacmi = 0.3; // Arka plan müzik sesi (0.0 - 1.0)
    public boolean muzikSessiz = false; // Müzik tamamen kapalı mı?

    // ── Active board modes (at most one is non-null at a time) ─────────────────
    KlasikBoardMode klasikBoardMode;
    LeblebiBoardMode leblebiBoardMode;
    ChessBoardMode chessBoardMode;

    // ── Level progression (Leblebi only) ───────────────────────────────────────
    int mevcutSeviye = 1;
    int toplamLeblebPuani = 0;
    int kaliciAltin = 0;
    int toplamKargaKullanim = 0;
    int toplamIlacKullanim = 0;

    // ── Grid dimensions / mine count ───────────────────────────────────────────
    int satirSayisi, sutunSayisi, mayinSayisi;

    // ── Grid UI ────────────────────────────────────────────────────────────────
    Button[][] dugmeler;
    /** Dirty-cell tracking: avoids unnecessary button redraws. */
    byte[][] hucreDurum;
    int yerlestirilenIsaret;

    /** Cells that are showing the "snake warning" overlay in Leblebi mode. */
    Set<Integer> yilanHucreleri = new HashSet<>();

    // ── Top-level layout containers ────────────────────────────────────────────
    BorderPane kokDuzen;
    GridPane izgaraDuzen;
    StackPane anaSahneKoku;
    StackPane merkezIcerikKutusu;
    Scene sahne; // alias → always equal to rootScene while in-game

    // ── Market / görev side-panels (Leblebi only) ──────────────────────────────
    VBox marketPanel;
    VBox gorevPanel;
    HBox canIkonKutusu;

    // ── HUD labels & buttons ───────────────────────────────────────────────────
    Label maynSayaciEtiketi;
    Label zamanlayiciEtiketi;
    Label durumEtiketi;
    Label canEtiketi;
    Label puanEtiketi;
    Label altinEtiketi;
    Button sifirlaBtn;

    // ── Konuşma balonu (speech bubble, Leblebi only) ───────────────────────────
    StackPane konusmaBalonuPanel;
    Label konusmaBalonuLabel;
    Animation aktifBalonAnimasyonu;

    // ── Timer ──────────────────────────────────────────────────────────────────
    Timeline zamanlayici;

    // ── Pause ──────────────────────────────────────────────────────────────────
    boolean oyunDuraksatildi = false;
    /** The pause button in the HUD — kept here so GameController can update its graphic. */
    Button duraklatBtn;

    // ── Screen-shake animation ─────────────────────────────────────────────────
    Timeline aktifSarsinti;

    // ── Easter egg ─────────────────────────────────────────────────────────────
    static final String EASTER_EGG_KOD = "1837837";
    StringBuilder basiliKodBuffer = new StringBuilder();
    boolean leblebAcildi = false;




    // ── Score de-dup guard (chess mode) ───────────────────────────────────────
    boolean satranSkorKaydedildi = false;
    /** Prevents the game-over popup from being shown twice. */
    boolean popupGosterildi = false;

    // ── Constant ───────────────────────────────────────────────────────────────
    static final int KLASIK_CAN = 3;
}
