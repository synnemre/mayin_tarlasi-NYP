import javafx.animation.*;
import javafx.application.Application;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import javafx.util.Duration;

/**
 * MinesweeperApp — Application entry point and module wiring.
 *
 * This class is intentionally thin. All game logic lives in GameController,
 * all UI construction lives in GameSceneBuilder / MenuView, all shared state
 * lives in AppState, and all assets/themes live in AppAssets / AppTheme.
 *
 * Responsibilities here:
 * - Create one instance of every module and keep them alive.
 * - Wire callbacks between modules (menu → game, game → menu, etc.).
 * - Build the single persistent Scene / Stage and hand it to every module.
 * - Implement the score-table window (read-only query, no game logic).
 */
public class MinesweeperApp extends Application {

    // ── Modules ───────────────────────────────────────────────────────────────
    private AppState s;
    private AppAssets assets;
    private AppTheme theme;
    private GameSceneBuilder builder;
    private GameController controller;
    private MenuView menu;

    // =========================================================================
    // start()
    // =========================================================================

    @Override
    public void start(Stage pencere) {
        // 1. Shared state
        s = new AppState();
        s.pencere = pencere;
        s.rootWrapper = new StackPane();
        s.rootScene = new Scene(s.rootWrapper, 660, 580);

        // 2. Assets (background load)
        assets = new AppAssets();
        assets.setAppState(s);
        assets.loadAsync();

        // 3. Theme
        theme = new AppTheme();

        // 3b. Restore persisted preferences (volume, theme, easter egg unlock).
        // Must come after AppTheme is constructed so invalidateCache() is safe,
        // and before any UI is built so the correct theme is used from the start.
        AppSettings.yukle(s);
        theme.invalidateCache();

        // 4. Scene builder
        builder = new GameSceneBuilder(s, assets, theme);

        // 5. Game controller
        controller = new GameController(s, assets, theme, builder);
        controller.setOnMenuGoster(this::menuGoster);

        // 5.5 Global Easter Eggs
        s.rootScene.setOnKeyTyped(e -> {
            String ch = e.getCharacter().toLowerCase();
            if (ch.isEmpty() || "0123456789".contains(ch)) return; // rakamlar MenuView KeyPressed'de işleniyor
            s.basiliKodBuffer.append(ch);
            if (s.basiliKodBuffer.length() > 20)
                s.basiliKodBuffer.delete(0, 1);

            String b = s.basiliKodBuffer.toString();
            if (b.endsWith("patlama")) {
                controller.devPatlamaTest();
                s.basiliKodBuffer.setLength(0);
            } else if (b.endsWith("konfeti")) {
                controller.konfetiAnimasyonu();
                s.basiliKodBuffer.setLength(0);
            } else if (b.endsWith("altin")) {
                controller.devAltinTest();
                s.basiliKodBuffer.setLength(0);
            }
        });

        // 6. Menu view
        menu = new MenuView(s, assets, theme);
        menu.setCallbacks(
                this::startKlasik,
                this::startSatran,
                this::startLeblebi,
                this::skorTablosunuGoster);

        // 7. Stage
        pencere.setScene(s.rootScene);
        pencere.setTitle("Mayın Tarlası");
        pencere.setMinWidth(560);
        pencere.setMinHeight(500);
        pencere.setResizable(true);
        pencere.setMaximized(true);
        pencere.centerOnScreen();
        pencere.setOnCloseRequest(e -> AppSettings.kaydet(s));
        pencere.show();

        // 8. Show main menu
        menuGoster();
    }

    // =========================================================================
    // Menu
    // =========================================================================

    private void menuGoster() {
        // Stop any running timer before returning to menu
        if (s.zamanlayici != null)
            s.zamanlayici.stop();
        // Stop any in-flight speech bubble animation — its onFinished lambda holds a
        // reference to konusmaBalonuPanel which menu.show() is about to detach/null.
        if (s.aktifBalonAnimasyonu != null) { s.aktifBalonAnimasyonu.stop(); s.aktifBalonAnimasyonu = null; }
        if (s.konusmaBalonuPanel  != null) { s.konusmaBalonuPanel.setVisible(false); }
        // Clear pause so the menu is always unblocked
        s.oyunDuraksatildi = false;
        // Persist settings every time we land on the menu
        AppSettings.kaydet(s);
        assets.playBgm("klasik");
        menu.show();
    }

    // =========================================================================
    // Game starters (called by MenuView callbacks after dialog confirmed)
    // =========================================================================

    /** Classic mode — KlasikBoardMode already set on AppState by MenuView. */
    private void startKlasik() {
        s.leblebModu = false;
        s.satranModu = false;
        s.leblebiBoardMode = null;
        s.chessBoardMode = null;
        // klasikBoardMode already constructed in MenuView.showKlasikDialog()
        s.popupGosterildi = false;
        s.satranSkorKaydedildi = false;

        builder.oyunSahnesiniBaSlat(false, false);
        controller.registerCallbacks();
        controller.zamanlayiciBaslat();
        controller.arayuzuGuncelle();
        assets.playBgm("klasik");
    }

    /** Chess mode — ChessBoardMode already set on AppState by MenuView. */
    private void startSatran() {
        s.leblebModu = false;
        s.satranModu = true;
        s.leblebiBoardMode = null;
        s.klasikBoardMode = null;
        s.popupGosterildi = false;
        s.satranSkorKaydedildi = false;

        builder.oyunSahnesiniBaSlat(false, true);
        controller.registerCallbacks();
        controller.zamanlayiciBaslat();
        controller.arayuzuGuncelle();
        assets.playBgm("satranc");
    }

    /** Leblebi mode — delegates fully to GameController which manages levels. */
    private void startLeblebi() {
        s.leblebModu = true;
        s.satranModu = false;
        s.klasikBoardMode = null;
        s.chessBoardMode = null;
        // kaliciAltin / mevcutSeviye / toplamKargaKullanim etc. already reset
        // inside MenuView.showLeblebDialog() before this callback fires.
        assets.playBgm("leblebi");
        controller.leblebOyunuBaslat();
    }

    // =========================================================================
    // Score table
    // =========================================================================

    private void skorTablosunuGoster() {
        Stage pencere2 = new Stage();
        pencere2.setTitle("Skor Tablosu");
        pencere2.initModality(Modality.APPLICATION_MODAL);
        pencere2.initOwner(s.pencere);

        String arka = "#1e1e2e", tabloBg = "#181825";

        TabPane sekmeler = new TabPane();
        sekmeler.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        sekmeler.setStyle("-fx-background-color:" + arka + ";-fx-tab-min-width:140px;");

        // Classic timed
        var zamanlıTab = new Tab("⏱ Klasik (Zamanlı)");
        zamanlıTab.setContent(zamanlıSkorTabloIcerik(
                SkorTablosu.yukle(SkorTablosu.MOD_ZAMANLI), arka, tabloBg));
        sekmeler.getTabs().add(zamanlıTab);

        // Chess
        var satranTab = new Tab("♟ Satranç Modu");
        satranTab.setContent(skorTabloIcerik(
                SkorTablosu.yukle("satranç"),
                new String[] { "#", "İsim", "Skor", "Zorluk", "Tarih" },
                (g, i) -> new String[] {
                        (i + 1) + ".",
                        blank(g.isim()),
                        String.valueOf(g.skor()),
                        switch (g.seviye()) {
                            case 1 -> "Kolay";
                            case 2 -> "Orta";
                            case 3 -> "Zor";
                            default -> "—";
                        },
                        blank(g.tarih())
                },
                "Henüz kayıt yok.\nSatranç modunu oyna ve adını yazdır!",
                arka, tabloBg));
        sekmeler.getTabs().add(satranTab);

        // Leblebi (easter egg)
        if (s.leblebAcildi) {
            var leblebTab = new Tab("🫘 Leblebi Tarlası");
            leblebTab.setContent(skorTabloIcerik(
                    SkorTablosu.yukle(SkorTablosu.MOD_LEBLEBI),
                    new String[] { "#", "İsim", "Skor", "Seviye", "Tarih" },
                    (g, i) -> new String[] {
                            (i + 1) + ".",
                            blank(g.isim()),
                            String.valueOf(g.skor()),
                            g.seviye() <= 0 ? "—" : "Seviye " + g.seviye(),
                            blank(g.tarih())
                    },
                    "Henüz kayıt yok.\nLeblebi Tarlası'nı oyna ve adını yazdır!",
                    arka, tabloBg));
            sekmeler.getTabs().add(leblebTab);
        }

        var kapat = new Button("Kapat");
        kapat.setStyle("-fx-font-size:13px;-fx-padding:4 10 4 10;-fx-cursor:hand;" +
                "-fx-border-radius:6;-fx-background-radius:6;" +
                "-fx-background-color:#313244;-fx-text-fill:#cdd6f4;");
        kapat.setOnAction(e -> pencere2.close());

        var root = new VBox(8, sekmeler, kapat);
        root.setPadding(new Insets(16));
        root.setAlignment(Pos.TOP_CENTER);
        root.setStyle("-fx-background-color:" + arka + ";");
        VBox.setVgrow(sekmeler, Priority.ALWAYS);

        var sc = new Scene(root, 640, 520);
        sc.setFill(javafx.scene.paint.Color.web(arka));
        theme.applyCss(sc);
        pencere2.setScene(sc);
        pencere2.showAndWait();
    }

    // ── Score-table builders ──────────────────────────────────────────────────

    private javafx.scene.Node zamanlıSkorTabloIcerik(
            java.util.List<SkorTablosu.SkorGirisi> liste, String arka, String tabloBg) {

        return skorTabloIcerik(liste,
                new String[] { "#", "İsim", "Skor", "Zorluk", "Tarih" },
                (g, i) -> new String[] {
                        (i + 1) + ".",
                        blank(g.isim()),
                        String.valueOf(g.skor()),
                        presetEtiketiAl(g),
                        blank(g.tarih())
                },
                "Henüz kayıt yok.\nZamanlı modda oyna ve adını yazdır!",
                arka, tabloBg);
    }

    private javafx.scene.Node skorTabloIcerik(
            java.util.List<SkorTablosu.SkorGirisi> liste,
            String[] basliklar,
            java.util.function.BiFunction<SkorTablosu.SkorGirisi, Integer, String[]> satirUret,
            String bosMsg, String arka, String tabloBg) {

        var tablo = new GridPane();
        tablo.setHgap(18);
        tablo.setVgap(8);
        tablo.setStyle("-fx-background-color:" + tabloBg + ";-fx-padding:16;-fx-background-radius:10;");
        tablo.setAlignment(Pos.CENTER);

        for (int i = 0; i < basliklar.length; i++) {
            var lbl = new Label(basliklar[i]);
            lbl.setStyle("-fx-font-weight:bold;-fx-text-fill:#89b4fa;-fx-font-size:13px;");
            tablo.add(lbl, i, 0);
        }

        if (liste.isEmpty()) {
            var bos = new Label(bosMsg);
            bos.setStyle("-fx-text-fill:#6c7086;-fx-font-size:13px;");
            bos.setWrapText(true);
            tablo.add(bos, 0, 1, basliklar.length, 1);
        } else {
            for (int i = 0; i < Math.min(liste.size(), 20); i++) {
                String renk = i == 0 ? "#f1c40f" : i == 1 ? "#95a5a6" : i == 2 ? "#e67e22" : "#cdd6f4";
                String[] vals = satirUret.apply(liste.get(i), i);
                for (int j = 0; j < vals.length; j++) {
                    var lbl = new Label(vals[j]);
                    lbl.setStyle("-fx-text-fill:" + renk + ";-fx-font-size:12px;");
                    tablo.add(lbl, j, i + 1);
                }
            }
        }

        var scroll = new ScrollPane(tablo);
        scroll.setStyle("-fx-background-color:" + arka + ";-fx-background:" + arka + ";");
        scroll.setFitToWidth(true);
        var wrap = new VBox(scroll);
        wrap.setStyle("-fx-background-color:" + arka + ";-fx-padding:12;");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return wrap;
    }

    private String presetEtiketiAl(SkorTablosu.SkorGirisi g) {
        if (g.satirSayisi() > 0) {
            for (var p : KlasikBoardMode.PRESETLER) {
                if (p.geriSayim()
                        && p.satir() == g.satirSayisi() && p.sutun() == g.sutunSayisi()
                        && p.mayin() == g.mayinSayisi() && p.sure() == g.sureSiniri()) {
                    String e = p.etiket();
                    int sp = e.indexOf(' ');
                    return sp >= 0 ? e.substring(sp + 1) : e;
                }
            }
            double yog = (double) g.mayinSayisi() / (g.satirSayisi() * g.sutunSayisi()) * 100;
            return String.format("%d×%d / %d💣 / %.0f%%",
                    g.satirSayisi(), g.sutunSayisi(), g.mayinSayisi(), yog);
        }
        return "—";
    }

    // ── Tiny helper ──────────────────────────────────────────────────────────

    private static String blank(String v) {
        return (v == null || v.isBlank()) ? "—" : v;
    }

    // =========================================================================
    // Entry point
    // =========================================================================

    public static void main(String[] args) {
        launch();
    }
}
