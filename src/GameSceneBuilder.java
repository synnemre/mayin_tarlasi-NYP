import javafx.animation.*;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.util.Duration;
import java.security.SecureRandom;

/**
 * GameSceneBuilder — Constructs the in-game UI.
 *
 * Responsibilities:
 *   - oyunSahnesiniBaSlat()  : top-level scene assembly
 *   - ustBariOlustur()       : HUD (mine counter, timer, score, buttons)
 *   - izgarayiOlustur()      : Button grid + mouse-event wiring
 *   - marketPanelOlustur/Guncelle : Leblebi shop
 *   - gorevPanelOlustur/Guncelle  : Leblebi missions
 *   - satranBilgiBariOlustur : Chess hint bar
 *   - temayiUygula()         : theme colours
 *   - hucreBoyutlariniGuncelle() : responsive cell sizing
 *
 * It does NOT contain game logic; all click actions delegate immediately to
 * the GameController callbacks set via setCallbacks().
 */
public class GameSceneBuilder {

    private static final SecureRandom RNG = new SecureRandom();

    private final AppState  s;
    private final AppAssets assets;
    private final AppTheme  theme;

    // ── Callbacks supplied by GameController ──────────────────────────────────
    /** Called on left-click. */
    private java.util.function.BiConsumer<Integer, Integer> onHucreAc;
    /** Called on right-click. */
    private java.util.function.BiConsumer<Integer, Integer> onIsaretKoy;
    /** Called on reset button. */
    private Runnable onSifirla;
    /** Called on menu button. */
    private Runnable onMenu;
    /** Called on theme toggle. */
    private Runnable onTema;
    /** Called on pause/resume button. */
    private Runnable onDuraksat;

    public GameSceneBuilder(AppState state, AppAssets assets, AppTheme theme) {
        this.s      = state;
        this.assets = assets;
        this.theme  = theme;
    }

    public void setCallbacks(
            java.util.function.BiConsumer<Integer,Integer> onHucreAc,
            java.util.function.BiConsumer<Integer,Integer> onIsaretKoy,
            Runnable onSifirla,
            Runnable onMenu,
            Runnable onTema,
            Runnable onDuraksat) {
        this.onHucreAc   = onHucreAc;
        this.onIsaretKoy = onIsaretKoy;
        this.onSifirla   = onSifirla;
        this.onMenu      = onMenu;
        this.onTema      = onTema;
        this.onDuraksat  = onDuraksat;
    }

    // =========================================================================
    // Scene assembly
    // =========================================================================

    /**
     * Builds and installs the full game scene into the shared root wrapper.
     *
     * @param leblebi true → Leblebi mode (market + görev panels, speech bubble)
     * @param satran  true → Chess mode (hint bar at bottom)
     */
    public void oyunSahnesiniBaSlat(boolean leblebi, boolean satran) {
        s.kokDuzen = new BorderPane();
        s.kokDuzen.setPadding(new Insets(12));

        ustBariOlustur();
        izgarayiOlustur();

        if (leblebi) {
            marketPanelOlustur();
            gorevPanelOlustur();
            s.kokDuzen.setRight(s.marketPanel);
            s.kokDuzen.setLeft(s.gorevPanel);
            if (s.leblebModu) { marketPanelGuncelle(); gorevPanelGuncelle(); }
        }

        if (satran) satranBilgiBariOlustur();

        temayiUygula();

        s.anaSahneKoku = new StackPane(s.kokDuzen);
        s.anaSahneKoku.setStyle("-fx-background-color: transparent;");

        s.sahne = s.rootScene;
        s.rootScene.widthProperty() .removeListener(this::onResize);
        s.rootScene.heightProperty().removeListener(this::onResize);
        s.rootScene.widthProperty() .addListener(this::onResize);
        s.rootScene.heightProperty().addListener(this::onResize);
        theme.applyCss(s.sahne);

        if (leblebi) {
            // Speech-bubble overlay
            s.konusmaBalonuLabel = new Label();
            s.konusmaBalonuLabel.setWrapText(true);
            s.konusmaBalonuLabel.setMaxWidth(480);
            s.konusmaBalonuLabel.setStyle("-fx-background-color: transparent;" +
                    "-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #1a0e00;");

            Label emmiIkonu = new Label("👴 Mehmet Emmi:");
            emmiIkonu.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; " +
                    "-fx-text-fill: #3d1a00; -fx-padding: 0 0 4 0;");

            VBox balonKutusu = new VBox(0, emmiIkonu, s.konusmaBalonuLabel);
            balonKutusu.setStyle(
                    "-fx-background-color: linear-gradient(to bottom right, #fffdf0, #ffeaa7);" +
                    "-fx-border-color: #c89a2a; -fx-border-width: 2.5; -fx-border-radius: 14;" +
                    "-fx-background-radius: 14; -fx-padding: 12 16 12 16;" +
                    "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.45),12,0,0,6);");
            balonKutusu.setMaxWidth(500);

            s.konusmaBalonuPanel = new StackPane(balonKutusu);
            s.konusmaBalonuPanel.setVisible(false);
            s.konusmaBalonuPanel.setOpacity(0);
            s.konusmaBalonuPanel.setMouseTransparent(true);
            s.konusmaBalonuPanel.setMaxSize(500, 200);
            StackPane.setAlignment(s.konusmaBalonuPanel, Pos.BOTTOM_RIGHT);
            StackPane.setMargin(s.konusmaBalonuPanel, new Insets(0, 0, 18, 14));
            s.anaSahneKoku.getChildren().add(s.konusmaBalonuPanel);
        } else {
            s.konusmaBalonuPanel = null;
            s.konusmaBalonuLabel = null;
        }

        theme.applyCss(s.rootScene);
        s.rootWrapper.getChildren().setAll(s.anaSahneKoku);
        hucreBoyutlariniGuncelle();
    }

    // =========================================================================
    // HUD bar
    // =========================================================================

    public void ustBariOlustur() {
        String hudL = "-fx-font-size: 19px; -fx-font-weight: bold; -fx-padding: 4 12 4 12;-fx-background-radius: 8;";
        String hudP = "-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #f0c040;-fx-padding: 4 12 4 12; -fx-background-radius: 8;";

        s.maynSayaciEtiketi  = new Label(); s.maynSayaciEtiketi.setStyle(hudL);
        s.zamanlayiciEtiketi = new Label(); s.zamanlayiciEtiketi.setStyle(hudL);
        s.canEtiketi         = new Label(""); s.canEtiketi.setStyle(hudL + "-fx-text-fill: #ff6b6b;");
        s.puanEtiketi        = new Label(""); s.puanEtiketi.setStyle(hudP);
        s.puanEtiketi.setGraphic(imgGraphic(assets.imgKupa, null, 24));
        s.altinEtiketi       = new Label(""); s.altinEtiketi.setStyle(hudP);
        s.altinEtiketi.setGraphic(imgGraphic(assets.imgAltin, null, 24));
        s.durumEtiketi       = new Label(""); s.durumEtiketi.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");

        s.sifirlaBtn = new Button();
        if (s.leblebModu) {
            s.sifirlaBtn.setGraphic(imgGraphic(assets.imgLeblebi, "🌾", 26));
        } else if (s.satranModu) {
            s.sifirlaBtn.setGraphic(imgGraphic(assets.imgMutlu, "♟", 26));
        } else {
            s.sifirlaBtn.setGraphic(imgGraphic(assets.imgMutlu, "😊", 26));
        }
        s.sifirlaBtn.setStyle(theme.butonTarzi() + "-fx-font-size: 18px; -fx-padding: 6 14 6 14;");
        s.sifirlaBtn.setOnAction(o -> { assets.play(assets.sesButon); if (onSifirla != null) onSifirla.run(); });

        Button menuBtn = new Button("← Menü");
        menuBtn.setStyle(theme.butonTarzi());
        menuBtn.setOnAction(o -> { assets.play(assets.sesButon); if (onMenu != null) onMenu.run(); });

        Button temaBtn = new Button();
        // Dark theme → show sun (switch to light); light theme → show star (switch to dark)
        temaBtn.setGraphic(imgGraphic(s.karanlikTema ? assets.imgGunes : assets.imgYildiz,
                                      s.karanlikTema ? "☀" : "★", 22));
        temaBtn.setStyle(theme.butonTarzi());
        temaBtn.setVisible(!s.leblebModu && !s.satranModu);
        temaBtn.setManaged(!s.leblebModu && !s.satranModu);
        temaBtn.setOnAction(o -> {
            s.karanlikTema = !s.karanlikTema;
            theme.invalidateCache();
            temaBtn.setGraphic(imgGraphic(s.karanlikTema ? assets.imgGunes : assets.imgYildiz,
                                          s.karanlikTema ? "☀" : "★", 22));
            // Force full cell repaint — dirty-tracking skips cells whose state
            // code hasn't changed, so we must flush it whenever styles change.
            if (s.hucreDurum != null)
                for (byte[] row : s.hucreDurum) java.util.Arrays.fill(row, (byte) -1);
            if (onTema != null) onTema.run();
        });

        HBox ustBar = new HBox(8, menuBtn, s.maynSayaciEtiketi, s.canEtiketi, s.sifirlaBtn,
                s.zamanlayiciEtiketi, s.puanEtiketi, s.altinEtiketi, s.durumEtiketi, temaBtn);

        // Pause button — right-aligned via a growing spacer
        Region spacerHud = new Region();
        HBox.setHgrow(spacerHud, Priority.ALWAYS);
        s.duraklatBtn = new Button("⏸");
        s.duraklatBtn.setStyle(theme.butonTarzi() + "-fx-font-size:16px;-fx-padding:6 12 6 12;");
        s.duraklatBtn.setOnAction(o -> { assets.play(assets.sesButon); if (onDuraksat != null) onDuraksat.run(); });
        ustBar.getChildren().addAll(spacerHud, s.duraklatBtn);

        ustBar.setAlignment(Pos.CENTER_LEFT);
        ustBar.setPadding(new Insets(6, 8, 10, 8));
        s.kokDuzen.setTop(ustBar);

        guncelleUstBar();
    }

    public void guncelleUstBar() {
        if (s.satranModu && s.chessBoardMode != null) {
            s.maynSayaciEtiketi.setGraphic(imgGraphic(assets.imgCarpi, "♟", 24));
            s.maynSayaciEtiketi.setText(" " + s.chessBoardMode.getMineCount());
            s.zamanlayiciEtiketi.setGraphic(imgGraphic(assets.imgKumSaati, "⏳", 24));
            s.zamanlayiciEtiketi.setText(" " + s.chessBoardMode.getKalanSure() + "s");
            s.canEtiketi.setText(""); s.canEtiketi.setVisible(false); s.canEtiketi.setManaged(false);
            s.puanEtiketi.setText(""); s.altinEtiketi.setText("");
            s.puanEtiketi.setVisible(false); s.puanEtiketi.setManaged(false);
            s.altinEtiketi.setVisible(false); s.altinEtiketi.setManaged(false);
            return;
        }

        s.maynSayaciEtiketi.setGraphic(imgGraphic(s.leblebModu ? assets.imgYilan : assets.imgMayin, s.leblebModu ? "🐍" : "💣", 24));
        s.maynSayaciEtiketi.setText(" " + (s.mayinSayisi - s.yerlestirilenIsaret));

        if (s.leblebModu && s.leblebiBoardMode != null) {
            s.zamanlayiciEtiketi.setGraphic(imgGraphic(assets.imgKumSaati, "⏳", 24));
            s.zamanlayiciEtiketi.setText(" " + s.leblebiBoardMode.getKalanSure() + "s");
            s.canEtiketi.setText(""); s.canEtiketi.setVisible(false); s.canEtiketi.setManaged(false);
            s.puanEtiketi.setText(s.leblebiBoardMode.getLeblebPuani() + " puan");
            s.altinEtiketi.setText(s.leblebiBoardMode.getAltin() + " altın");
            s.puanEtiketi.setVisible(true); s.puanEtiketi.setManaged(true);
            s.altinEtiketi.setVisible(true); s.altinEtiketi.setManaged(true);
        } else if (s.klasikBoardMode != null && s.klasikBoardMode.isGeriSayim()) {
            s.zamanlayiciEtiketi.setGraphic(imgGraphic(assets.imgKumSaati, "⏳", 24));
            s.zamanlayiciEtiketi.setText(" " + s.klasikBoardMode.getSuruclukSure() + "s");
            s.zamanlayiciEtiketi.setStyle("-fx-font-size: 19px; -fx-font-weight: bold; " +
                    "-fx-padding: 4 12 4 12;-fx-background-radius: 8; -fx-text-fill: #cdd6f4;");
            s.canEtiketi.setText(""); s.canEtiketi.setVisible(true); s.canEtiketi.setManaged(true);
            s.puanEtiketi.setText(""); s.altinEtiketi.setText("");
            s.puanEtiketi.setVisible(false); s.puanEtiketi.setManaged(false);
            s.altinEtiketi.setVisible(false); s.altinEtiketi.setManaged(false);
        } else {
            s.zamanlayiciEtiketi.setGraphic(imgGraphic(assets.imgAlarm, "⏱", 24));
            String zaman = (s.klasikBoardMode != null)
                    ? " " + s.klasikBoardMode.getSuruclukSure() + "s"
                    : " 0s";
            s.zamanlayiciEtiketi.setText(zaman);
            s.canEtiketi.setText(""); s.canEtiketi.setVisible(true); s.canEtiketi.setManaged(true);
            s.puanEtiketi.setText(""); s.altinEtiketi.setText("");
            s.puanEtiketi.setVisible(false); s.puanEtiketi.setManaged(false);
            s.altinEtiketi.setVisible(false); s.altinEtiketi.setManaged(false);
        }
    }

    // =========================================================================
    // Grid
    // =========================================================================

    public void izgarayiOlustur() {
        s.izgaraDuzen = new GridPane();
        s.izgaraDuzen.setHgap(3); s.izgaraDuzen.setVgap(3);
        s.izgaraDuzen.setAlignment(Pos.CENTER);

        s.dugmeler         = new Button[s.satirSayisi][s.sutunSayisi];
        s.hucreDurum       = new byte[s.satirSayisi][s.sutunSayisi];
        for (byte[] row : s.hucreDurum) java.util.Arrays.fill(row, (byte) -1);
        lastBoy = -1; // force resize pass on first render of new grid
        gorevTamamlandiRendered.clear(); // force gorev panel full rebuild
        s.yerlestirilenIsaret = 0;

        for (int r = 0; r < s.satirSayisi; r++) {
            for (int c = 0; c < s.sutunSayisi; c++) {
                Button btn = new Button();
                btn.setPrefSize(48, 48); btn.setMinSize(28, 28);
                final int sr = r, su = c;

                btn.setOnMouseClicked(ev -> {
                    if (oyunAktifDegil()) return;
                    if (ev.getButton() == MouseButton.PRIMARY  && onHucreAc   != null) onHucreAc  .accept(sr, su);
                    if (ev.getButton() == MouseButton.SECONDARY && onIsaretKoy != null) onIsaretKoy.accept(sr, su);
                });

                btn.setOnMouseEntered(ev -> {
                    if (btn.isDisabled() || oyunAktifDegil()) return;
                    if (s.leblebModu && s.leblebiBoardMode != null && s.leblebiBoardMode.isZirayiIlacAktif()) {
                        ilacHoverUygula(sr, su);
                    } else {
                        if (isCellClosed(sr, su)) btn.setStyle(theme.acilmamisHucreHoverTarzi(s, sr, su));
                    }
                });
                btn.setOnMouseExited(ev -> {
                    if (btn.isDisabled()) return;
                    if (s.leblebModu && s.leblebiBoardMode != null && s.leblebiBoardMode.isZirayiIlacAktif()) {
                        ilacHoverTemizle();
                    } else {
                        if (isCellClosed(sr, su)) btn.setStyle(theme.acilmamisHucreTarzi(s, sr, su));
                    }
                });

                s.dugmeler[r][c] = btn;
                s.izgaraDuzen.add(btn, c, r);
            }
        }

        if (s.canIkonKutusu == null) {
            s.canIkonKutusu = new HBox(6);
            s.canIkonKutusu.setAlignment(Pos.CENTER);
            s.canIkonKutusu.setPickOnBounds(false);
        }

        s.merkezIcerikKutusu = new StackPane(s.izgaraDuzen);
        s.merkezIcerikKutusu.setAlignment(Pos.CENTER);
        StackPane merkez = new StackPane(s.merkezIcerikKutusu);
        merkez.setAlignment(Pos.CENTER);
        VBox.setVgrow(merkez, Priority.ALWAYS);
        s.kokDuzen.setCenter(merkez);

        if (s.leblebModu && s.leblebiBoardMode != null)
            canSayisiGuncelle(s.leblebiBoardMode.getCanSayisi());
    }

    // =========================================================================
    // Market panel (Leblebi)
    // =========================================================================

    public void marketPanelOlustur() {
        s.marketPanel = new VBox(15);
        s.marketPanel.setPadding(new Insets(15));
        s.marketPanel.setStyle("-fx-background-color: rgba(61,40,0,0.6); -fx-background-radius: 12;");
        s.marketPanel.setPrefWidth(240);

        Label baslik = new Label("🚜 Mehmet Emmi'nin Dükkanı");
        baslik.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #f0c040;");
        Label seviyeEtiketi = new Label("Bölüm: " + s.mevcutSeviye);
        seviyeEtiketi.setStyle("-fx-font-size: 14px; -fx-text-fill: #d4b070;");
        Separator sep = new Separator(); sep.setStyle("-fx-background-color: #c89a2a; -fx-opacity: 0.5;");

        int kLvl = s.leblebiBoardMode != null ? s.leblebiBoardMode.getKargaLevel() : 1;
        int iLvl = s.leblebiBoardMode != null ? s.leblebiBoardMode.getIlacLevel()  : 1;
        Button kargaBtn = marketBtn(assets.imgKarga,    "🐦", "Karga\n(" + kLvl + ". Seviye)", "20 💰");
        Button saatBtn  = marketBtn(assets.imgKumSaati, "⏰", "Emmi'nin Saati",                "30 💰");
        Button ilacBtn  = marketBtn(assets.imgIlac,     "🧪", "Zirai İlaç\n(" + iLvl + ". Seviye)", "50 💰");
        Button kalpBtn  = marketBtn(assets.imgEkstraCan,"❤️", "Ekstra Kalp",                  "100 💰");
        kargaBtn.setId("kargaBtn"); saatBtn.setId("saatBtn");
        ilacBtn.setId("ilacBtn");   kalpBtn.setId("kalpBtn");

        kargaBtn.setOnAction(o -> {
            LeblebiBoardMode.KargaSonuc sonuc = s.leblebiBoardMode.kargaKullan();
            switch (sonuc) {
                case BASARILI -> {
                    assets.play(assets.sesMarketKarga != null ? assets.sesMarketKarga : assets.sesMarket);
                    marketPanelGuncelle();
                    refreshScoreLabels();
                    kargaHiglightGuncelle(); // FIX: render ⚠ cells immediately
                    int rnd = RNG.nextInt(2);
                    s.leblebiBoardMode.diyalogTetikle(rnd == 1
                            ? LeblebiBoardMode.DiyalogTetikleyici.KARGA_KULLANIMI
                            : LeblebiBoardMode.DiyalogTetikleyici.ITEM_KULLANIMI);
                    diyalogGoster(s.leblebiBoardMode.getAktifDiyalog());
                }
                case MAYIN_YOK      -> triggerDialog("Burada yılan yok evlat, paranı cebinde tut.");
                case YETERSIZ_ALTIN -> triggerDialog("Karga için 20 altın lazım, paran yetmez.");
            }
        });
        saatBtn.setOnAction(o -> {
            if (s.leblebiBoardMode.emmininSaatiniKullan()) {
                assets.play(assets.sesMarketSaat != null ? assets.sesMarketSaat : assets.sesMarket); marketPanelGuncelle();
                s.zamanlayiciEtiketi.setGraphic(imgGraphic(assets.imgKumSaati, "⏳", 24));
                s.zamanlayiciEtiketi.setText(" " + s.leblebiBoardMode.getKalanSure() + "s");
                refreshScoreLabels();
                int rnd = RNG.nextInt(2);
                s.leblebiBoardMode.diyalogTetikle(rnd == 1
                        ? LeblebiBoardMode.DiyalogTetikleyici.SAAT_KULLANIMI
                        : LeblebiBoardMode.DiyalogTetikleyici.ITEM_KULLANIMI);
            } else { triggerDialog("Yeterli paran yok! Saat 30 altın."); }
        });
        ilacBtn.setOnAction(o -> {
            if (s.leblebiBoardMode.zirayiIlacAktiflesir()) {
                assets.play(assets.sesMarketIlac != null ? assets.sesMarketIlac : assets.sesMarket); marketPanelGuncelle();
                refreshScoreLabels();
                ilacBtn.setStyle(ilacBtn.getStyle() + "-fx-border-color: #e74c3c; -fx-border-width: 2;");
                int rnd = RNG.nextInt(2);
                s.leblebiBoardMode.diyalogTetikle(rnd == 1
                        ? LeblebiBoardMode.DiyalogTetikleyici.ILAC_KULLANIMI
                        : LeblebiBoardMode.DiyalogTetikleyici.ITEM_KULLANIMI);
            } else { triggerDialog("İlaç pahalı evlat, 50 altın lazım."); }
        });
        kalpBtn.setOnAction(o -> {
            if (s.leblebiBoardMode.ekstraKalpAl()) {
                assets.play(assets.sesMarketKalp != null ? assets.sesMarketKalp : assets.sesMarket);
                canSayisiGuncelle(s.leblebiBoardMode.getCanSayisi());
                marketPanelGuncelle(); refreshScoreLabels();
                int rnd = RNG.nextInt(2);
                s.leblebiBoardMode.diyalogTetikle(rnd == 1
                        ? LeblebiBoardMode.DiyalogTetikleyici.EKSTRA_KALP
                        : LeblebiBoardMode.DiyalogTetikleyici.ITEM_KULLANIMI);
            } else { triggerDialog("Can almak o kadar ucuz değil, 100 altın ister."); }
        });

        GridPane kartlar = new GridPane(); kartlar.setHgap(8); kartlar.setVgap(8);
        kartlar.add(kargaBtn, 0, 0); kartlar.add(saatBtn, 1, 0);
        kartlar.add(ilacBtn,  0, 1); kartlar.add(kalpBtn, 1, 1);
        GridPane.setHgrow(kargaBtn, Priority.ALWAYS); GridPane.setHgrow(saatBtn, Priority.ALWAYS);
        GridPane.setHgrow(ilacBtn,  Priority.ALWAYS); GridPane.setHgrow(kalpBtn, Priority.ALWAYS);

        s.marketPanel.getChildren().addAll(baslik, seviyeEtiketi, sep, kartlar);
    }

    public void marketPanelGuncelle() {
        if (s.marketPanel == null || s.leblebiBoardMode == null) return;
        int altin = s.leblebiBoardMode.getAltin();
        java.util.function.Consumer<javafx.scene.Node> upd = dugum -> {
            if (dugum instanceof Button btn) {
                String id = btn.getId(); if (id == null) return;
                boolean aktif = switch (id) {
                    case "kargaBtn" -> altin >= 20; case "saatBtn" -> altin >= 30;
                    case "ilacBtn"  -> altin >= 50; case "kalpBtn" -> altin >= 100;
                    default -> true;
                };
                btn.setDisable(!aktif); btn.setOpacity(aktif ? 1.0 : 0.5);
                if (aktif) btn.setStyle(AppTheme.MKT_BTN_NORMAL);
            }
        };
        s.marketPanel.getChildren().forEach(n -> {
            upd.accept(n);
            if (n instanceof GridPane gp) gp.getChildren().forEach(upd::accept);
        });
    }

    // =========================================================================
    // Görev panel (Leblebi)
    // =========================================================================

    public void gorevPanelOlustur() {
        s.gorevPanel = new VBox(15); s.gorevPanel.setPadding(new Insets(15));
        s.gorevPanel.setStyle("-fx-background-color: #2a1c02; -fx-border-color: #6b3e00; " +
                "-fx-border-width: 0 2 0 0; -fx-min-width: 220px;");
        Label baslik = new Label("📋 Emmi'nin İstekleri");
        baslik.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #f5e6b0;");
        s.gorevPanel.getChildren().add(baslik);
    }

    // Track which tasks were already rendered as completed to avoid redundant DOM rebuilds.
    private final java.util.Set<Integer> gorevTamamlandiRendered = new java.util.HashSet<>();

    public void gorevPanelGuncelle() {
        if (s.gorevPanel == null || s.leblebiBoardMode == null) return;
        java.util.List<LeblebiBoardMode.Gorev> gorevler = s.leblebiBoardMode.getAktifGorevler();

        // First call: panel has only the header label — build the task cards.
        // Subsequent calls: only update cards whose completion state just flipped.
        boolean ilkYapim = s.gorevPanel.getChildren().size() <= 1;

        if (ilkYapim) {
            for (int i = 0; i < gorevler.size(); i++) {
                LeblebiBoardMode.Gorev g = gorevler.get(i);
                VBox kart = gorevKartOlustur(g);
                kart.setId("gorev_" + i);
                s.gorevPanel.getChildren().add(kart);
            }
        } else {
            // Only re-render cards whose tamamlandi flag changed since last render.
            for (int i = 0; i < gorevler.size(); i++) {
                LeblebiBoardMode.Gorev g = gorevler.get(i);
                boolean wasRendered = gorevTamamlandiRendered.contains(i);
                if (g.tamamlandi == wasRendered) continue; // no change
                // Find existing card by id and replace it in-place.
                String id = "gorev_" + i;
                for (int j = 1; j < s.gorevPanel.getChildren().size(); j++) {
                    if (id.equals(s.gorevPanel.getChildren().get(j).getId())) {
                        VBox nyKart = gorevKartOlustur(g);
                        nyKart.setId(id);
                        s.gorevPanel.getChildren().set(j, nyKart);
                        break;
                    }
                }
            }
        }
        // Sync rendered-state tracker.
        gorevTamamlandiRendered.clear();
        for (int i = 0; i < gorevler.size(); i++) {
            if (gorevler.get(i).tamamlandi) gorevTamamlandiRendered.add(i);
        }
    }

    private VBox gorevKartOlustur(LeblebiBoardMode.Gorev g) {
        VBox kart = new VBox(5); kart.setPadding(new Insets(10));
        String bg     = g.tamamlandi ? "-fx-background-color: #3b5025;" : "-fx-background-color: #3d2800;";
        String border = g.tamamlandi ? "-fx-border-color: #6dbe45;"     : "-fx-border-color: #6b3e00;";
        kart.setStyle(bg + border + " -fx-border-radius: 8; -fx-background-radius: 8;");
        Label desc = new Label(g.aciklama); desc.setWrapText(true);
        desc.setStyle("-fx-font-size: 14px; -fx-text-fill: " +
                (g.tamamlandi ? "#a6e3a1" : "#e8c55a") + "; -fx-font-weight: bold;");
        if (g.tamamlandi) {
            javafx.scene.Node tikNode = imgGraphic(assets.imgTik, null, 20);
            Label tikYazi = new Label(" Tamamlandı");
            tikYazi.setStyle("-fx-font-size: 12px; -fx-text-fill: #6dbe45; -fx-font-weight: bold;");
            HBox tikSatir = new HBox(4, tikNode != null ? tikNode : new Label("✔"), tikYazi);
            tikSatir.setAlignment(Pos.CENTER_LEFT);
            kart.getChildren().addAll(desc, tikSatir);
        } else {
            javafx.scene.Node yildizNode = imgGraphic(assets.imgYildiz, null, 18);
            javafx.scene.Node altinNode  = imgGraphic(assets.imgAltin,  null, 18);
            Label odulYazi = new Label((yildizNode == null ? "🏆 " : "") + g.puanOdulu
                    + " Puan,  " + (altinNode == null ? "💰 " : "") + g.altinOdulu + " Altın");
            odulYazi.setStyle("-fx-font-size: 12px; -fx-text-fill: #a0a0a0;");
            HBox odulSatir = new HBox(4);
            if (yildizNode != null) odulSatir.getChildren().add(yildizNode);
            odulSatir.getChildren().add(odulYazi);
            if (altinNode  != null) odulSatir.getChildren().add(altinNode);
            odulSatir.setAlignment(Pos.CENTER_LEFT);
            kart.getChildren().addAll(desc, odulSatir);
        }
        return kart;
    }

    // =========================================================================
    // Chess hint bar (bottom)
    // =========================================================================

    public void satranBilgiBariOlustur() {
        HBox bar = new HBox(20); bar.setAlignment(Pos.CENTER);
        bar.setPadding(new Insets(10, 8, 6, 8));
        bar.setStyle("-fx-background-color: " + AppTheme.CH_UST_BAR + "; -fx-background-radius: 8;");
        String[] bilgiler = {
            "Sol tık → Aç", "Sağ tık → Bayrak",
            "Her hamlede taşlar hareket eder!",
            "♟Piyon  ♞At  ♝Fil  ♜Kale  ♛Vezir"
        };
        for (int i = 0; i < bilgiler.length; i++) {
            Label l = new Label(bilgiler[i]); l.setStyle("-fx-font-size: 12px; -fx-text-fill: #95a5a6;");
            bar.getChildren().add(l);
            if (i < bilgiler.length - 1) {
                Label sep = new Label("|"); sep.setStyle("-fx-text-fill: #2c3e50;");
                bar.getChildren().add(sep);
            }
        }
        s.kokDuzen.setBottom(bar);
    }

    // =========================================================================
    // Theme
    // =========================================================================

    public void temayiUygula() {
        String arka  = s.satranModu ? AppTheme.CH_ARKAPLAN  : (s.leblebModu ? AppTheme.LB_ARKAPLAN  : (s.karanlikTema ? AppTheme.KT_ARKAPLAN  : AppTheme.AT_ARKAPLAN));
        String ustBr = s.satranModu ? AppTheme.CH_UST_BAR   : (s.leblebModu ? AppTheme.LB_UST_BAR   : (s.karanlikTema ? AppTheme.KT_UST_BAR   : AppTheme.AT_UST_BAR));
        String yazi  = s.satranModu ? "#cdd6f4"             : (s.leblebModu ? "#f5e6b0"             : (s.karanlikTema ? AppTheme.KT_YAZI       : AppTheme.AT_YAZI));
        String cerc  = s.satranModu ? AppTheme.CH_CERCEVE   : (s.leblebModu ? AppTheme.LB_CERCEVE   : (s.karanlikTema ? AppTheme.KT_CERCEVE    : AppTheme.AT_CERCEVE));

        s.kokDuzen.setStyle("-fx-background-color:" + arka + ";");
        if (s.kokDuzen.getTop() instanceof HBox bar) {
            bar.setStyle("-fx-background-color:" + ustBr +
                    ";-fx-background-radius:10;-fx-padding:10 16 10 16;" +
                    "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.35),8,0,0,3);");
            bar.getChildren().forEach(n -> {
                if (n instanceof Label l)
                    l.setStyle(l.getStyle() + "-fx-text-fill:" + yazi + ";");
                else if (n instanceof Button b) {
                    String btnBg = s.satranModu ? "#0d2a3e"
                            : (s.karanlikTema && !s.leblebModu ? "#313244"
                            : (s.leblebModu ? "#7a5200" : "#c8cdd8"));
                    b.setStyle(b.getStyle() + "-fx-background-color:" + btnBg +
                               ";-fx-text-fill:" + yazi + ";-fx-border-color:" + cerc + ";");
                }
            });
        }
        if (s.izgaraDuzen != null)
            s.izgaraDuzen.setStyle("-fx-background-color:" + cerc + ";-fx-padding:2;");
    }

    // =========================================================================
    // Responsive cell sizing
    // =========================================================================

        // Last computed cell size — skip the full O(N²) resize pass when nothing changed.
        private double lastBoy = -1;
        private double lastYaz = 14;

        public double getLastYaz() { return lastYaz; }

        public void hucreBoyutlariniGuncelle() {
        if (s.dugmeler == null) return;
        double yanG = 0;
        if (s.leblebModu) {
            if (s.marketPanel != null) yanG += 240;
            if (s.gorevPanel != null) yanG += 220;
        }
        double kullanG = s.sahne.getWidth() - 52 - yanG;
        double kullanY = s.sahne.getHeight() - (s.satranModu ? 180 : 140);
        double hg = Math.floor(kullanG / s.sutunSayisi);
        double hy = Math.floor(kullanY / s.satirSayisi);
        double boy = Math.max(28, Math.min(hg, hy));
        if (s.satranModu) boy = Math.max(boy, 40);

        double yaz = Math.max(13, boy * 0.32);

        // Skip the full loop when cell size hasn't changed (every cell click triggers this).
        if (boy == lastBoy) return;
        lastBoy = boy;
        lastYaz = yaz;
        String[] sayiRenk = theme.sayiRenkleri(s);

        // Hoist board lookup outside the loop — aktifTahta() is called N² times otherwise.
        Board tahta = s.satranModu ? null : aktifTahta();

        for (int r = 0; r < s.satirSayisi; r++) {
            for (int c = 0; c < s.sutunSayisi; c++) {
                Button btn = s.dugmeler[r][c];
                btn.setMinSize(boy, boy);
                btn.setPrefSize(boy, boy);
                btn.setMaxSize(boy, boy);

                if (s.satranModu && s.chessBoardMode != null
                        && s.chessBoardMode.isRevealed(r, c) && !s.chessBoardMode.isMine(r, c)) {
                    int threat = s.chessBoardMode.getThreat(r, c);
                    String fg = (threat > 0 && threat <= 8) ? AppTheme.CH_SAYI_RENK[threat] : "#4a6a8a";
                    btn.setStyle("-fx-background-color:" + AppTheme.CH_ACILMIS + ";-fx-border-color:" +
                            AppTheme.CH_CERCEVE + ";-fx-border-width:1;-fx-background-radius:4;" +
                            "-fx-border-radius:4;-fx-text-fill:" + fg +
                            ";-fx-font-weight:bold;-fx-font-size:" + yaz + "px;");
                }
                else if (tahta != null) {
                    Cell h = tahta.getHucre(r, c);
                    if (h.isAcildiMi() && !h.isMayinMi()) {
                        int k = h.getKomsuMayinSayisi();

                        if (h.isGoldenLeblebi()) {
                            btn.setText(k == 0 ? "" : String.valueOf(k));
                            btn.setGraphic(k == 0 ? imgGraphicFill(assets.imgAltinLeblebi, "🌟", 18, btn) : null);
                            btn.setStyle(
                                "-fx-background-color: linear-gradient(to bottom right, #ffd700, #ff8c00);" +
                                "-fx-border-color: #daa520; -fx-border-width: 2.5;" +
                                "-fx-background-radius: 5; -fx-border-radius: 5;" +
                                "-fx-text-fill: #2a1500; -fx-font-weight: bold; -fx-font-size:" + yaz + "px;"
                            );
                        } else {
                            btn.setStyle(theme.acilmisHucreTarzi(s, k, sayiRenk, yaz));
                        }
                    }
                }
            }
        }
    }

    // =========================================================================
    // Can (lives) UI
    // =========================================================================

    public void canSayisiGuncelle(int canSayisi) {
        if (s.canIkonKutusu == null) return;
        s.canIkonKutusu.getChildren().clear();
        for (int i = 0; i < canSayisi; i++) {
            // Prefer real image, fallback to emoji
            s.canIkonKutusu.getChildren().add(
                imgGraphic(assets.imgKalp, "❤", 22)
            );
        }
        if (s.kokDuzen != null && s.kokDuzen.getTop() instanceof HBox bar) {
            if (!bar.getChildren().contains(s.canIkonKutusu)) {
                int idx = bar.getChildren().indexOf(s.canEtiketi);
                if (idx >= 0) bar.getChildren().add(idx + 1, s.canIkonKutusu);
                else bar.getChildren().add(s.canIkonKutusu);
            }
        }
    }

    public void canIkonunuKir(int kalanCan) {
        if (s.canIkonKutusu == null) return;
        int kirIndex = kalanCan;
        if (kirIndex < 0 || kirIndex >= s.canIkonKutusu.getChildren().size()) return;
        javafx.scene.Node hedef = s.canIkonKutusu.getChildren().get(kirIndex);
        ScaleTransition buyuKucul = new ScaleTransition(Duration.millis(400), hedef);
        buyuKucul.setFromX(1.0); buyuKucul.setToX(1.4); buyuKucul.setFromY(1.0); buyuKucul.setToY(1.4);
        buyuKucul.setAutoReverse(true); buyuKucul.setCycleCount(2);
        FadeTransition soluklas = new FadeTransition(Duration.millis(400), hedef);
        soluklas.setFromValue(1.0); soluklas.setToValue(0.25);
        ParallelTransition anim = new ParallelTransition(buyuKucul, soluklas);
        anim.setOnFinished(e -> {
            hedef.setOpacity(0.2);
            if (hedef instanceof Label lbl) {
                lbl.setText("");
                lbl.setGraphic(imgGraphic(assets.imgKafatasi, "💀", 22));
            }
        });
        anim.play();
    }

    // =========================================================================
    // Speech bubble (Leblebi)
    // =========================================================================

    public void diyalogGoster(String metin) {
        if (s.konusmaBalonuPanel == null || s.konusmaBalonuLabel == null) return;
        if (metin == null || metin.isBlank()) return;
        if (s.aktifBalonAnimasyonu != null) { s.aktifBalonAnimasyonu.stop(); s.aktifBalonAnimasyonu = null; }
        s.konusmaBalonuLabel.setText(metin);
        s.konusmaBalonuPanel.setVisible(true);
        s.konusmaBalonuPanel.toFront();
        s.konusmaBalonuPanel.setOpacity(0);
        FadeTransition belir = new FadeTransition(Duration.millis(300), s.konusmaBalonuPanel);
        belir.setFromValue(0); belir.setToValue(1);
        PauseTransition bekle = new PauseTransition(Duration.seconds(5));
        FadeTransition kaybol = new FadeTransition(Duration.millis(500), s.konusmaBalonuPanel);
        kaybol.setFromValue(1); kaybol.setToValue(0);
        kaybol.setOnFinished(e -> {
            s.konusmaBalonuPanel.setVisible(false);
            if (s.leblebiBoardMode != null) s.leblebiBoardMode.diyaloguTemizle();
            s.aktifBalonAnimasyonu = null;
        });
        SequentialTransition dizi = new SequentialTransition(belir, bekle, kaybol);
        s.aktifBalonAnimasyonu = dizi;
        dizi.play();
    }

    // =========================================================================
    // Zirai İlaç hover preview
    // =========================================================================

    public void ilacHoverUygula(int merkezS, int merkezU) {
        if (s.dugmeler == null || aktifTahta() == null || s.leblebiBoardMode == null) return;
        Board tahta = aktifTahta();
        int cap = (s.leblebiBoardMode.getIlacLevel() == 1) ? 1
                : (s.leblebiBoardMode.getIlacLevel() == 2) ? 2 : 100;
        java.util.List<int[]> alan = new java.util.ArrayList<>();
        if (cap == 100) {
            for (int r = 0; r < s.satirSayisi; r++) alan.add(new int[]{r, merkezU});
            for (int c = 0; c < s.sutunSayisi;  c++) { if (c != merkezU) alan.add(new int[]{merkezS, c}); }
        } else {
            for (int dr = -cap; dr <= cap; dr++) for (int dc = -cap; dc <= cap; dc++)
                alan.add(new int[]{merkezS + dr, merkezU + dc});
        }
        for (int[] pos : alan) {
            int r = pos[0], c = pos[1];
            if (r < 0 || r >= s.satirSayisi || c < 0 || c >= s.sutunSayisi) continue;
            Button btn = s.dugmeler[r][c]; if (btn == null || btn.isDisabled()) continue;
            if (tahta.getHucre(r, c).isAcildiMi()) continue;
            btn.setStyle((r == merkezS && c == merkezU) ? theme.ilacHoverMerkezTarzi() : theme.ilacHoverKenarTarzi());
            btn.setCursor(javafx.scene.Cursor.CROSSHAIR);
        }
    }

    public void ilacHoverTemizle() {
        if (s.dugmeler == null || aktifTahta() == null) return;
        Board tahta = aktifTahta();
        for (int r = 0; r < s.satirSayisi; r++) for (int c = 0; c < s.sutunSayisi; c++) {
            Button btn = s.dugmeler[r][c]; if (btn == null || btn.isDisabled()) continue;
            Cell h = tahta.getHucre(r, c);
            if (!h.isAcildiMi() && !h.isIsaretlendi()) btn.setStyle(theme.acilmamisHucreTarzi(s));
            else if (h.isIsaretlendi()) btn.setStyle(theme.isaretliHucreTarzi(s));
            btn.setCursor(javafx.scene.Cursor.DEFAULT);
        }
    }

    // =========================================================================
    // Shared graphics helpers
    // =========================================================================

    public javafx.scene.Node imgGraphic(Image img, String fallbackEmoji, double size) {
        if (img != null) {
            ImageView iv = new ImageView(img);
            iv.setFitWidth(size); iv.setFitHeight(size);
            iv.setPreserveRatio(true); iv.setSmooth(true);
            return iv;
        }
        if (fallbackEmoji == null) return null;
        return emojiLabel(fallbackEmoji, size);
    }

    /**
     * Like imgGraphic but binds the ImageView's dimensions to the button's
     * actual size minus a small padding, so the graphic always fills the cell.
     * Falls back to an emoji label at fallbackSize if img is null.
     */
    public javafx.scene.Node imgGraphicFill(Image img, String fallbackEmoji,
                                            double fallbackSize,
                                            javafx.scene.control.Button btn) {
        if (img != null) {
            ImageView iv = new ImageView(img);
            iv.setPreserveRatio(true); iv.setSmooth(true);
            // Use 65 % of the button's current size — large enough to read clearly,
            // small enough to leave a comfortable border gap.
            double size = btn.getPrefWidth() * 0.55;
            iv.setFitWidth(size); iv.setFitHeight(size);
            return iv;
        }
        if (fallbackEmoji == null) return null;
        return emojiLabel(fallbackEmoji, fallbackSize);
    }
    public Label emojiLabel(String emoji, double size) {
        Label lbl = new Label(emoji);
        String emojiStack = "'Noto Emoji', 'Segoe UI Emoji', 'Apple Color Emoji', System";
        lbl.setStyle("-fx-font-size:" + (int)size + "px; -fx-font-family:" + emojiStack + ";");
        return lbl;
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private void refreshScoreLabels() {
        if (s.leblebiBoardMode == null) return;
        s.puanEtiketi.setText(s.leblebiBoardMode.getLeblebPuani() + " puan");
        s.altinEtiketi.setText(s.leblebiBoardMode.getAltin() + " altın");
    }

    /**
     * Repaints only the cells currently marked as karga targets so the ⚠
     * highlight appears immediately after the crow button is pressed.
     *
     * FIX: hucreDurum is set to 4 (the canonical karga state), not -1, so the
     * next renderGrid pass knows the cell is already in state 4 and skips it
     * rather than overwriting the highlight with the plain closed style.
     *
     * FIX: Any existing FadeTransition stored in the button's userData is stopped
     * before a new one is started, preventing animation accumulation across
     * repeated crow uses on the same cell.
     */
    private void kargaHiglightGuncelle() {
        if (s.leblebiBoardMode == null || s.dugmeler == null) return;
        Board tahta = aktifTahta();
        if (tahta == null) return;
        for (int[] k : s.leblebiBoardMode.getKargaGosterilenMayinlar()) {
            int r = k[0], c = k[1];
            if (r < 0 || r >= s.satirSayisi || c < 0 || c >= s.sutunSayisi) continue;
            Cell h = tahta.getHucre(r, c);
            if (h.isAcildiMi() || h.isIsaretlendi()) continue;
            s.hucreDurum[r][c] = 4; // FIX: correct state, not -1
            Button btn = s.dugmeler[r][c];
            // FIX: stop previous animation before starting a new one
            stopKargaAnim(btn);
            btn.setText(""); btn.setGraphic(emojiLabel("⚠", 16));
            btn.setStyle(theme.acilmamisHucreTarzi(s) +
                         "-fx-border-color:" + AppTheme.LB_KARGA_RENK + ";-fx-border-width:2.5;");
            btn.setDisable(false);
            var ft = new FadeTransition(Duration.millis(600), btn);
            ft.setFromValue(0.5); ft.setToValue(1.0);
            ft.setCycleCount(Animation.INDEFINITE); ft.setAutoReverse(true);
            btn.setUserData(ft); // store so we can stop it later
            ft.play();
        }
        guncelleUstBar();
    }

    /** Stops and clears a karga pulse animation stored in the button's userData. */
    public void stopKargaAnim(Button btn) {
        if (btn.getUserData() instanceof FadeTransition ft) {
            ft.stop();
            btn.setOpacity(1.0);
            btn.setUserData(null);
        }
    }

    /**
     * Used by market buttons to show a dialog when they have no result to trigger.
     * The actual dialog display goes through the speech-bubble panel; if the panel
     * is ready we show it, otherwise we fall back to doing nothing (the text will
     * be set properly by the next regular diyalogTetikle call anyway).
     */
    private void triggerDialog(String text) {
        diyalogGoster(text);
    }

    private Board aktifTahta() {
        if (s.leblebModu && s.leblebiBoardMode != null) return s.leblebiBoardMode.getTahta();
        if (s.klasikBoardMode != null) return s.klasikBoardMode.getTahta();
        return null;
    }

    private boolean oyunAktifDegil() {
        if (s.oyunDuraksatildi) return true;
        if (s.satranModu   && s.chessBoardMode   != null) return !s.chessBoardMode.isOyunAktif();
        if (s.leblebModu   && s.leblebiBoardMode  != null)
            return s.leblebiBoardMode.isOyunBitti() || s.leblebiBoardMode.isKazanildi();
        return s.klasikBoardMode == null || !s.klasikBoardMode.isOyunAktif();
    }

    private boolean isCellClosed(int r, int c) {
        if (s.satranModu && s.chessBoardMode != null)
            return !s.chessBoardMode.isRevealed(r, c) && !s.chessBoardMode.isFlagged(r, c);
        Board t = aktifTahta();
        return t != null && !t.getHucre(r, c).isAcildiMi() && !t.getHucre(r, c).isIsaretlendi();
    }

    /**
     * Creates a market button with a real image icon (and an emoji fallback).
     */
    private Button marketBtn(Image img, String fallbackEmoji, String isim, String fiyat) {
        javafx.scene.Node ikonNode;
        if (img != null) {
            ImageView iv = new ImageView(img);
            iv.setFitWidth(36); iv.setFitHeight(36);
            iv.setPreserveRatio(true); iv.setSmooth(true);
            ikonNode = iv;
        } else {
            Label lbl = new Label(fallbackEmoji); lbl.setStyle("-fx-font-size: 28px;");
            ikonNode = lbl;
        }
        Label isimLabel  = new Label(isim);
        isimLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #d4b070; -fx-font-weight: bold;");
        isimLabel.setWrapText(true);
        isimLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        Label fiyatLabel = new Label(fiyat); fiyatLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #f0c040; -fx-font-weight: bold;");
        VBox kutu = new VBox(2, ikonNode, isimLabel, fiyatLabel);
        kutu.setAlignment(Pos.CENTER); kutu.setMaxWidth(Double.MAX_VALUE);
        Button btn = new Button(); btn.setGraphic(kutu);
        btn.setMaxWidth(Double.MAX_VALUE); btn.setPrefHeight(90);
        btn.setStyle(AppTheme.MKT_BTN_NORMAL);
        ScaleTransition stIn  = new ScaleTransition(Duration.millis(150), btn); stIn.setToX(1.05); stIn.setToY(1.05);
        ScaleTransition stOut = new ScaleTransition(Duration.millis(150), btn); stOut.setToX(1.0);  stOut.setToY(1.0);
        btn.setOnMouseEntered(e -> { if (!btn.isDisabled()) { btn.setStyle(AppTheme.MKT_BTN_HOVER); stOut.stop(); stIn.play(); }});
        btn.setOnMouseExited(e  -> { btn.setStyle(AppTheme.MKT_BTN_NORMAL); stIn.stop(); stOut.play(); });
        return btn;
    }

    /** Legacy overload kept for any callers that only have an emoji (no image). */
    private Button marketBtn(String ikon, String isim, String fiyat) {
        return marketBtn(null, ikon, isim, fiyat);
    }

    private void onResize(javafx.beans.value.ObservableValue<?> obs, Object o, Object n) {
        hucreBoyutlariniGuncelle();
    }

    // =========================================================================
    // Pause overlay
    // =========================================================================

    private static final String PAUSE_OVERLAY_ID = "pauseOverlay";

    public void duraklatilaOverlayGoster() {
        if (s.anaSahneKoku == null) return;
        boolean zatenVar = s.anaSahneKoku.getChildren().stream()
                .anyMatch(n -> PAUSE_OVERLAY_ID.equals(n.getId()));
        if (zatenVar) return;

        StackPane overlay = new StackPane();
        overlay.setId(PAUSE_OVERLAY_ID);
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.55);");
        overlay.setMouseTransparent(true);

        VBox kutu = new VBox(12);
        kutu.setAlignment(Pos.CENTER);
        kutu.setMaxWidth(320);
        kutu.setMaxHeight(VBox.USE_PREF_SIZE);
        kutu.setPadding(new Insets(28, 40, 28, 40));

        String bg     = s.satranModu ? "#0a1628" : s.leblebModu ? "#1a0e00" : "#12111f";
        String accent = s.satranModu ? "#f0c040" : s.leblebModu ? "#e8b840" : "#89b4fa";
        kutu.setStyle("-fx-background-color:" + bg + ";" +
                "-fx-border-color:" + accent + ";-fx-border-width:2.5;" +
                "-fx-border-radius:12;-fx-background-radius:12;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.7),20,0,0,6);");

        Label pauseIcon = new Label("⏸");
        pauseIcon.setStyle("-fx-font-size:48px;-fx-text-fill:" + accent + ";");
        Label pauseText = new Label("DURAKLATILDI");
        pauseText.setStyle("-fx-font-size:22px;-fx-font-weight:bold;-fx-text-fill:" + accent + ";-fx-letter-spacing:3;");
        Label hint = new Label("Devam etmek için Space tuşuna bas");
        hint.setStyle("-fx-font-size:12px;-fx-text-fill:#6878a0;");
        hint.setWrapText(true);
        hint.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        kutu.getChildren().addAll(pauseIcon, pauseText, hint);
        overlay.getChildren().add(kutu);

        overlay.setOpacity(0);
        s.anaSahneKoku.getChildren().add(overlay);
        FadeTransition ft = new FadeTransition(Duration.millis(200), overlay);
        ft.setToValue(1.0);
        ft.play();
    }

    public void duraklatilaOverlayKaldir() {
        if (s.anaSahneKoku == null) return;
        s.anaSahneKoku.getChildren().removeIf(n -> PAUSE_OVERLAY_ID.equals(n.getId()));
    }
}
