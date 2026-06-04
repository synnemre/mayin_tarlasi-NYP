import javafx.animation.*;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import javafx.util.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * MenuView — Builds and displays the main menu and all game-setup dialogs.
 *
 * Callbacks back into the main app are supplied as Runnable/lambda parameters
 * so this class has zero dependency on MinesweeperApp itself.
 */
public class MenuView {

    private final AppState s;
    private final AppAssets assets;
    private final AppTheme  theme;

    /** Called when the player clicks "Klasik" and confirms settings. */
    private Runnable onStartKlasik;
    /** Called when the player clicks "Satranç" and confirms settings. */
    private Runnable onStartSatran;
    /** Called when the player clicks "Leblebi" and confirms settings. */
    private Runnable onStartLeblebi;
    /** Called when the player clicks "Skor Tablosu". */
    private Runnable onShowSkor;

    public MenuView(AppState state, AppAssets assets, AppTheme theme) {
        this.s      = state;
        this.assets = assets;
        this.theme  = theme;
    }

    public void setCallbacks(Runnable onStartKlasik, Runnable onStartSatran,
                              Runnable onStartLeblebi, Runnable onShowSkor) {
        this.onStartKlasik  = onStartKlasik;
        this.onStartSatran  = onStartSatran;
        this.onStartLeblebi = onStartLeblebi;
        this.onShowSkor     = onShowSkor;
    }

    // =========================================================================
    // Main menu
    // =========================================================================

    public void show() {
        BorderPane kok = new BorderPane();
        kok.setStyle("-fx-background-color: linear-gradient(to bottom right, #141c36 0%, #182040 50%, #122038 100%);");

        javafx.scene.canvas.Canvas gridCanvas = new javafx.scene.canvas.Canvas();
        gridCanvas.setMouseTransparent(true);
        gridCanvas.setOpacity(0.08);
        Runnable gridCiz = () -> {
            double w = gridCanvas.getWidth(), h = gridCanvas.getHeight();
            if (w <= 0 || h <= 0) return;
            var gc = gridCanvas.getGraphicsContext2D();
            gc.clearRect(0, 0, w, h);
            gc.setStroke(javafx.scene.paint.Color.web("#8098ff"));
            gc.setLineWidth(0.5);
            for (double x = 0; x < w; x += 60) { gc.beginPath(); gc.moveTo(x, 0); gc.lineTo(x, h); gc.stroke(); }
            for (double y = 0; y < h; y += 60) { gc.beginPath(); gc.moveTo(0, y); gc.lineTo(w, y); gc.stroke(); }
        };
        StackPane arkaplanKutu = new StackPane(gridCanvas);
        arkaplanKutu.setMouseTransparent(true);

        VBox icerik = new VBox(0);
        icerik.setAlignment(Pos.CENTER);

        // ── Title ─────────────────────────────────────────────────────────────
        VBox baslikBolge = new VBox(0);
        baslikBolge.setAlignment(Pos.CENTER);
        baslikBolge.setPadding(new Insets(52, 0, 28, 0));

        HBox ustCizgi = new HBox(); ustCizgi.setMaxWidth(320); ustCizgi.setMinHeight(1);
        ustCizgi.setStyle("-fx-background-color: linear-gradient(to right, transparent, #6060c0, transparent);");

        Label mayinLabel = new Label("M A Y I N");
        mayinLabel.setStyle("-fx-font-family: 'Courier New', monospace;" +
                "-fx-font-size: 56px; -fx-font-weight: bold; -fx-letter-spacing: 10;" +
                "-fx-text-fill: #e8e0ff;" +
                "-fx-effect: dropshadow(gaussian, #5040a0, 24, 0.5, 0, 4);");

        HBox altCizgi = new HBox(); altCizgi.setMaxWidth(420); altCizgi.setMinHeight(1);
        altCizgi.setStyle("-fx-background-color: linear-gradient(to right, transparent, #6060c0, transparent);");

        Label tarlasi = new Label("— T A R L A S I —");
        tarlasi.setStyle("-fx-font-family: 'Courier New', monospace;" +
                "-fx-font-size: 18px; -fx-letter-spacing: 6;" +
                "-fx-text-fill: #8878c8; -fx-padding: 4 0 0 0;");

        Label kose1 = new Label("1");
        kose1.setStyle("-fx-font-size: 60px; -fx-text-fill: #2a3060; -fx-font-weight: bold;");
        Label kose2 = new Label("2");
        kose2.setStyle("-fx-font-size: 60px; -fx-text-fill: #738c88; -fx-font-weight: bold;");

        HBox baslikSatir = new HBox(0, kose1, new Region(), mayinLabel, new Region(), kose2);
        HBox.setHgrow(baslikSatir.getChildren().get(1), Priority.ALWAYS);
        HBox.setHgrow(baslikSatir.getChildren().get(3), Priority.ALWAYS);
        baslikSatir.setAlignment(Pos.CENTER);
        baslikSatir.setMaxWidth(Double.MAX_VALUE);
        baslikSatir.setPadding(new Insets(0, 40, 0, 40));
        baslikBolge.getChildren().addAll(ustCizgi, baslikSatir, tarlasi, altCizgi);

        // ── Mode buttons ──────────────────────────────────────────────────────
        Button klasikBtn = menuCard("⚑", "KLASİK",        "MAYIN TARLASI",   "#4a7bff", "#1a1e3a");
        Button ozelBtn   = menuCard("♟", "SATRANÇ",       "TARLASI",         "#9060e0", "#1e1530");
        Button skorBtn   = menuCard("⌖", "SKOR",          "TABLOSU",         "#22aa55", "#0a1e12");

        klasikBtn.setOnAction(e -> { assets.play(assets.sesButon); s.leblebModu = false; s.satranModu = false; showKlasikDialog(); });
        ozelBtn  .setOnAction(e -> { assets.play(assets.sesButon); s.leblebModu = false; s.satranModu = true;  showSatranDialog(); });
        skorBtn  .setOnAction(e -> { assets.play(assets.sesButon); if (onShowSkor != null) onShowSkor.run(); });

        Button leblebBtn = menuCardWithImage(assets.imgAltinLeblebi, "🫘", "Mehmet Emmi'nin", "LEBLEBİ TARLASI", "#e8b840", "#251800");
        leblebBtn.setId("leblebBtn");
        leblebBtn.setVisible(s.leblebAcildi);
        leblebBtn.setManaged(s.leblebAcildi);
        leblebBtn.setOnAction(e -> { assets.play(assets.sesButon); s.leblebModu = true; s.satranModu = false; showLeblebDialog(); });

        // If the image loaded after this method ran (async), swap the fallback emoji for the real graphic
        if (assets.imgAltinLeblebi == null) {
            javafx.animation.PauseTransition bekle = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(1));
            bekle.setOnFinished(ev -> {
                if (assets.imgAltinLeblebi != null && leblebBtn.getGraphic() instanceof VBox vb && !vb.getChildren().isEmpty()) {
                    javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(assets.imgAltinLeblebi);
                    iv.setFitWidth(52); iv.setFitHeight(52); iv.setPreserveRatio(true); iv.setSmooth(true);
                    iv.setStyle("-fx-effect: dropshadow(gaussian, #e8b840, 14, 0.5, 0, 0);");
                    vb.getChildren().set(0, iv);
                }
            });
            bekle.play();
        }
        leblebBtn.setOnAction(e -> { assets.play(assets.sesButon); s.leblebModu = true; s.satranModu = false; showLeblebDialog(); });

        HBox kartlar = new HBox(16, klasikBtn, ozelBtn, leblebBtn);
        kartlar.setAlignment(Pos.CENTER);
        kartlar.setPadding(new Insets(0, 48, 14, 48));

        HBox skorSatir = new HBox(skorBtn);
        skorSatir.setAlignment(Pos.CENTER);
        skorSatir.setPadding(new Insets(0, 48, 0, 48));

        Label easterEggEtiketi = new Label(s.leblebAcildi ? "🫘 Mehmet Emmi'nin Leblebi Tarlası Modu Açık!" : "");
        easterEggEtiketi.setId("easterEggEtiket");
        easterEggEtiketi.setStyle("-fx-font-size: 12px; -fx-text-fill: #c89a2a; -fx-font-weight: bold; -fx-padding: 6 0 0 0;");
        easterEggEtiketi.setAlignment(Pos.CENTER);
        easterEggEtiketi.setMaxWidth(Double.MAX_VALUE);

        icerik.getChildren().addAll(baslikBolge, kartlar, skorSatir, easterEggEtiketi);

        // ── Bottom bar ────────────────────────────────────────────────────────
        HBox altBar = new HBox(12);
        altBar.setAlignment(Pos.CENTER);
        altBar.setPadding(new Insets(10, 20, 12, 20));
        altBar.setStyle("-fx-background-color: rgba(10,14,30,0.85); -fx-border-color: #1a2040; -fx-border-width: 1 0 0 0;");

        Button nasılBtn = buildHowToPlayBtn();
        nasılBtn.setOnAction(e -> { assets.play(assets.sesButon); showHowToPlay(); });

        Label muzikLabel = new Label("Müzik: ");
        muzikLabel.setStyle("-fx-text-fill: #8890b0;");

        Button sesKapaBtn = new Button();
        sesKapaBtn.setGraphic(mkImgLabel(assets.imgSesAyari, "🔊", 18));
        sesKapaBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
        sesKapaBtn.setOnAction(e -> {
            s.muzikSessiz = !s.muzikSessiz;
            sesKapaBtn.setGraphic(s.muzikSessiz
                ? mkImgLabel(assets.imgSesKapa, "🔇", 18)
                : mkImgLabel(assets.imgSesAyari, "🔊", 18));
            assets.updateBgmVolume();
            AppSettings.kaydet(s);
        });

        Slider sesSlider = new Slider(0, 1, s.muzikHacmi);
        sesSlider.setPrefWidth(80);
        sesSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            s.muzikHacmi = newVal.doubleValue();
            if (!s.muzikSessiz) assets.updateBgmVolume();
            AppSettings.kaydet(s);
        });

        HBox muzikKontrolleri = new HBox(5, muzikLabel, sesSlider, sesKapaBtn);
        muzikKontrolleri.setAlignment(Pos.CENTER);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        altBar.getChildren().addAll(nasılBtn, spacer, muzikKontrolleri);

        javafx.scene.Node infoIkonNode = mkImgLabel(assets.imgInfo, "ℹ", 16);
        // Rotating hints — one nudges towards the easter egg code
        String[] ipuclari = {
            "Mayın Tarlası'na hoş geldin! Dikkatli oyna.",
            "İpucu: Bazı tarlaların sırları vardır...",
            "Rakamlar komşu tehlikelerin sayısını gösterir.",
            "Sağ tık ile şüpheli kareleri işaretleyebilirsin.",
            "Mehmet Emmi'nin tarlasını keşfetmek için doğru kodu bul.",
            "Bazı sayılar kapı açar, bazı sayılar sır saklar...",
            "Satranç modunda her hamlen taşları harekete geçirir!",
        };
        // Pick hint based on second of the minute so it rotates without extra state
        int ipucuIdx = (int)(System.currentTimeMillis() / 8000) % ipuclari.length;
        Label bilgiYazi = new Label("💡 " + ipuclari[ipucuIdx]);
        bilgiYazi.setStyle("-fx-font-size: 13px; -fx-text-fill: #6878a0;");
        HBox bilgiKutu = new HBox(8, bilgiYazi);
        bilgiKutu.setAlignment(Pos.CENTER);
        bilgiKutu.setPadding(new Insets(6, 16, 6, 16));
        bilgiKutu.setStyle("-fx-background-color: rgba(20,28,60,0.7); -fx-background-radius: 20;");
        HBox.setHgrow(bilgiKutu, Priority.ALWAYS);

        Button cikisBtn = buildExitBtn();

        altBar.getChildren().addAll(bilgiKutu, cikisBtn);

        StackPane merkezStack = new StackPane(arkaplanKutu, icerik);
        StackPane.setAlignment(icerik, Pos.CENTER);
        kok.setCenter(merkezStack);
        kok.setBottom(altBar);

        arkaplanKutu.widthProperty() .addListener((o, ov, nv) -> { gridCanvas.setWidth(nv.doubleValue());  gridCiz.run(); });
        arkaplanKutu.heightProperty().addListener((o, ov, nv) -> { gridCanvas.setHeight(nv.doubleValue()); gridCiz.run(); });

        StackPane kokDuzenleyici = new StackPane(kok);
        kokDuzenleyici.setId("menuRoot");

        // Easter egg key listener
        s.rootScene.setOnKeyPressed(olay -> {
            if (s.leblebAcildi) return;
            String k = olay.getText();
            if (!k.isEmpty() && "1234567890".contains(k)) {
                s.basiliKodBuffer.append(k);
                if (s.basiliKodBuffer.length() > AppState.EASTER_EGG_KOD.length())
                    s.basiliKodBuffer.delete(0, s.basiliKodBuffer.length() - AppState.EASTER_EGG_KOD.length());
                if (s.basiliKodBuffer.toString().equals(AppState.EASTER_EGG_KOD)) {
                    s.leblebAcildi = true;
                    s.basiliKodBuffer.setLength(0);
                    assets.play(assets.sesCiftlik);
                    AppSettings.kaydet(s);
                    showEasterEggPopup(kokDuzenleyici, leblebBtn, easterEggEtiketi);
                }
            }
        });

        theme.applyCss(s.rootScene);
        s.rootWrapper.getChildren().setAll(kokDuzenleyici);
    }

    // =========================================================================
    // Setup dialogs
    // =========================================================================

    /** Klasik mod ayar penceresi. */
    public void showKlasikDialog() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(s.pencere);
        dialog.setTitle("Klasik Oyun Ayarları");
        dialog.setResizable(false);

        String arka = "#1e1e2e", yazi = "#cdd6f4", vurgu = "#89b4fa",
               girdiArka = "#181825", ayrac = "#313244";

        Label presetBaslik = new Label("Hazır Zorluklar");
        presetBaslik.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:" + vurgu + ";");

        GridPane presetGrid = new GridPane();
        presetGrid.setHgap(8); presetGrid.setVgap(8);

        Spinner<Integer> satirSp = makeSpinner(7, 30, 10);
        Spinner<Integer> sutunSp = makeSpinner(7, 30, 10);
        int minMayin = (int) Math.floor(satirSp.getValue() * sutunSp.getValue() * 0.15);
        Spinner<Integer> mayinSp = makeSpinner(minMayin, 500, 15);

        satirSp.getValueFactory().valueProperty().addListener((obs, eski, yeni) -> {
            int nm = (int) Math.floor(yeni * sutunSp.getValue() * 0.15);
            ((SpinnerValueFactory.IntegerSpinnerValueFactory) mayinSp.getValueFactory()).setMin(nm);
            mayinSp.getValueFactory().setValue(nm);
        });
        sutunSp.getValueFactory().valueProperty().addListener((obs, eski, yeni) -> {
            int nm = (int) Math.floor(satirSp.getValue() * yeni * 0.15);
            ((SpinnerValueFactory.IntegerSpinnerValueFactory) mayinSp.getValueFactory()).setMin(nm);
            mayinSp.getValueFactory().setValue(nm);
        });

        ToggleGroup timerGrup = new ToggleGroup();
        RadioButton yukariBtn = new RadioButton("⬆ Yukarı sayar (kronometre)");
        RadioButton geriBtn   = new RadioButton("⬇ Geri sayar (süre sınırlı)");
        yukariBtn.setToggleGroup(timerGrup); geriBtn.setToggleGroup(timerGrup);
        yukariBtn.setSelected(true);
        yukariBtn.setStyle("-fx-text-fill:" + yazi + ";");
        geriBtn  .setStyle("-fx-text-fill:" + yazi + ";");
        Spinner<Integer> sureSp = makeSpinner(10, 3600, 180);
        sureSp.setDisable(true);
        geriBtn.selectedProperty().addListener((obs, eski, yeni) -> sureSp.setDisable(!yeni));

        for (int i = 0; i < KlasikBoardMode.PRESETLER.length; i++) {
            KlasikBoardMode.KlasikAyar p = KlasikBoardMode.PRESETLER[i];
            Button pb = new Button(p.etiket()); pb.setPrefWidth(200);
            pb.setStyle("-fx-background-color:#313244;-fx-text-fill:" + yazi + ";" +
                        "-fx-background-radius:8;-fx-border-radius:8;" +
                        "-fx-border-color:" + ayrac + ";-fx-cursor:hand;-fx-font-size:12px;");
            pb.setOnMouseEntered(e -> pb.setStyle(pb.getStyle().replace("#313244", "#45475a")));
            pb.setOnMouseExited(e  -> pb.setStyle(pb.getStyle().replace("#45475a", "#313244")));
            pb.setOnAction(e -> {
                assets.play(assets.sesButon);
                satirSp.getValueFactory().setValue(p.satir());
                sutunSp.getValueFactory().setValue(p.sutun());
                mayinSp.getValueFactory().setValue(p.mayin());
                if (p.geriSayim()) { geriBtn.setSelected(true); sureSp.getValueFactory().setValue(p.sure()); }
                else yukariBtn.setSelected(true);
            });
            presetGrid.add(pb, i % 2, i / 2);
        }

        Label ozelBaslik = new Label("Ya da Özel Ayarla");
        ozelBaslik.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:" + vurgu + ";");

        Runnable mayinKlamp = () -> {
            int maks = Math.max(1, satirSp.getValue() * sutunSp.getValue() - 9);
            ((SpinnerValueFactory.IntegerSpinnerValueFactory) mayinSp.getValueFactory()).setMax(maks);
        };
        satirSp.valueProperty().addListener((o, e, n) -> mayinKlamp.run());
        sutunSp.valueProperty().addListener((o, e, n) -> mayinKlamp.run());

        GridPane ozelGrid = new GridPane();
        ozelGrid.setHgap(12); ozelGrid.setVgap(10);
        ozelGrid.addRow(0, labelFor("Satır sayısı:", yazi), satirSp);
        ozelGrid.addRow(1, labelFor("Sütun sayısı:", yazi), sutunSp);
        ozelGrid.addRow(2, labelFor("Mayın sayısı:", yazi), mayinSp);

        Label timerBaslik = new Label("Zamanlayıcı Modu");
        timerBaslik.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:" + vurgu + ";");
        GridPane timerGrid = new GridPane(); timerGrid.setHgap(12); timerGrid.setVgap(8);
        timerGrid.addRow(0, yukariBtn);
        timerGrid.addRow(1, geriBtn);
        timerGrid.addRow(2, labelFor("Süre (saniye):", yazi), sureSp);

        for (Spinner<?> sp : new Spinner<?>[]{ satirSp, sutunSp, mayinSp, sureSp }) {
            sp.setStyle("-fx-background-color:" + girdiArka + ";");
            sp.getEditor().setStyle("-fx-background-color:" + girdiArka + ";-fx-text-fill:" + yazi + ";");
        }

        Button oynaBtn = new Button("▶  Oyna!"); oynaBtn.setPrefWidth(180); oynaBtn.setPrefHeight(44);
        oynaBtn.setStyle("-fx-background-color:#89b4fa;-fx-text-fill:#1e1e2e;" +
                "-fx-font-size:15px;-fx-font-weight:bold;-fx-background-radius:10;-fx-cursor:hand;");
        oynaBtn.setOnAction(e -> {
            s.satirSayisi = satirSp.getValue();
            s.sutunSayisi = sutunSp.getValue();
            s.mayinSayisi = Math.min(mayinSp.getValue(), s.satirSayisi * s.sutunSayisi - 9);
            boolean geri = geriBtn.isSelected();
            int sure = geri ? sureSp.getValue() : 0;
            s.leblebModu = false; s.satranModu = false;
            s.leblebiBoardMode = null; s.chessBoardMode = null;
            s.klasikBoardMode  = new KlasikBoardMode(s.satirSayisi, s.sutunSayisi, s.mayinSayisi, geri, sure);
            assets.play(assets.sesBasla);
            dialog.close();
            if (onStartKlasik != null) onStartKlasik.run();
        });

        Separator sep1 = sep(ayrac), sep2 = sep(ayrac);
        VBox root = new VBox(14, presetBaslik, presetGrid, sep1, ozelBaslik, ozelGrid,
                sep2, timerBaslik, timerGrid, oynaBtn);
        root.setPadding(new Insets(24)); root.setAlignment(Pos.CENTER_LEFT);
        root.setStyle("-fx-background-color:" + arka + ";");
        Scene sc = new Scene(root); theme.applyCss(sc); dialog.setScene(sc); dialog.showAndWait();
    }

    /** Satranç mod ayar penceresi. */
    public void showSatranDialog() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(s.pencere);
        dialog.setTitle("♟ Satranç Mayın Tarlası");
        dialog.setResizable(true);

        String arka = AppTheme.CH_ARKAPLAN, yazi = "#d0cce8", vurgu = "#f0c040", panel = "#0f2240";

        Label baslik = new Label("♟  SATRANÇ MAYIN TARLASI");
        baslik.setStyle("-fx-font-family: 'Constantine', 'Palatino Linotype', serif;" +
                "-fx-font-size:22px; -fx-font-weight:bold; -fx-text-fill:" + vurgu + ";" +
                "-fx-effect: dropshadow(gaussian,#c09000,10,0.4,0,2);");
        Label altBaslik = new Label("Her hamlenin ardında taşlar hareket eder. Dikkatli ol!");
        altBaslik.setStyle("-fx-font-size:12px; -fx-text-fill:#7a90b0; -fx-font-style:italic;");
        Separator sep0 = sep("#1e3a5f");

        // How-to panel
        VBox nasılPane = infoPane(panel, "#1e3a5f");
        Label nasılBaslik = new Label("📖  Nasıl Oynanır?");
        nasılBaslik.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:" + vurgu + ";");
        String[][] kurallar = {
            {"🖱 Sol Tık",  "Kareyi açar. Taşa basarsan oyun biter!"},
            {"🚩 Sağ Tık",  "Şüpheli kareye bayrak diker / kaldırır."},
            {"🔢 Rakamlar", "O kareyi kaç taş tehdit ettiğini gösterir."},
            {"⚡ Dinamik",  "Her güvenli açış sonrası tüm taşlar hareket eder!"},
            {"🏆 Kazanmak", "Tüm taşsız kareleri aç, bayraklar gerekmez."},
            {"⏳ Süre",     "Süre dolmadan tahtayı temizlemelisin."},
            {"🎯 Strateji", "Sayıları dikkatlice okuyarak tehlikeli bölgeleri tespit et."},
        };
        nasılPane.getChildren().addAll(nasılBaslik, buildRuleGrid(kurallar, "#f0c040", yazi));

        // Piece movement panel
        VBox tasPane = infoPane(panel, "#1e3a5f");
        Label tasBaslik = new Label("♟  Taş Hareketleri");
        tasBaslik.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:" + vurgu + ";");
        Object[][] taslar = {
            {"♟ Piyon", "1 adım aşağı hareket eder (yön: güney).",         "#a8d8a8"},
            {"♞ At",    "L şeklinde atlar (2+1), engelleri aşar.",          "#a8c8f8"},
            {"♝ Fil",   "Çapraz yönde istediği kadar kayar.",               "#d8b8f8"},
            {"♜ Kale",  "Yatay veya dikey istediği kadar kayar.",           "#f8d8a8"},
            {"♛ Vezir", "Her yönde (çapraz + düz) istediği kadar kayar.",   "#f8a8a8"},
        };
        GridPane tasGrid = new GridPane(); tasGrid.setHgap(10); tasGrid.setVgap(6);
        for (int i = 0; i < taslar.length; i++) {
            Label sym  = new Label((String)taslar[i][0]);
            sym.setStyle("-fx-font-size:14px; -fx-text-fill:" + taslar[i][2] + "; -fx-min-width:55; -fx-font-weight:bold;");
            Label desc = new Label((String)taslar[i][1]);
            desc.setStyle("-fx-font-size:12px; -fx-text-fill:" + yazi + ";"); desc.setWrapText(true);
            tasGrid.add(sym, 0, i); tasGrid.add(desc, 1, i);
        }
        tasPane.getChildren().addAll(tasBaslik, tasGrid);

        // Difficulty panel
        VBox zorlukPane = infoPane(panel, "#1e3a5f");
        Label zorlukBaslik = new Label("⚙  Zorluk Seç");
        zorlukBaslik.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:" + vurgu + ";");
        ToggleGroup zorlukGrup = new ToggleGroup();
        VBox zorluklar = new VBox(6);
        String[] zorlukAciklamalar = {
            "Piyon ve At — Yavaş hareket, başlangıç için ideal.",
            "Fil ve Kale — Kayarak hareket, orta güçlük.",
            "Sadece Vezir — Her yönde kayar, en yüksek tehlike!"
        };
        for (int i = 0; i < ChessBoardMode.PRESETLER.length; i++) {
            ChessBoardMode.ChessAyar p = ChessBoardMode.PRESETLER[i];
            RadioButton rb = new RadioButton(p.etiket() + "  (" + p.sureSaniye() + "s)");
            rb.setToggleGroup(zorlukGrup); rb.setUserData(p);
            rb.setStyle("-fx-text-fill:" + yazi + "; -fx-font-size:13px; -fx-font-weight:bold;");
            if (p.zorluk() == 1) rb.setSelected(true);
            Label acDesc = new Label("    " + zorlukAciklamalar[i]);
            acDesc.setStyle("-fx-font-size:11px; -fx-text-fill:#5a7090;");
            zorluklar.getChildren().addAll(rb, acDesc);
        }
        zorlukPane.getChildren().addAll(zorlukBaslik, zorluklar);

        Button oynaBtn = goldButton("▶  Oyunu Başlat!");
        oynaBtn.setOnAction(e -> {
            ChessBoardMode.ChessAyar secilen =
                    (ChessBoardMode.ChessAyar)((RadioButton)zorlukGrup.getSelectedToggle()).getUserData();
            s.klasikBoardMode  = null; s.leblebiBoardMode = null;
            s.satranModu       = true;
            s.chessBoardMode   = new ChessBoardMode(secilen.zorluk(), secilen.sureSaniye());
            s.satirSayisi      = ChessBoardMode.BOARD_SIZE;
            s.sutunSayisi      = ChessBoardMode.BOARD_SIZE;
            s.mayinSayisi      = s.chessBoardMode.getMineCount();
            assets.play(assets.sesBasla);
            dialog.close();
            if (onStartSatran != null) onStartSatran.run();
        });

        HBox btnKutu = new HBox(oynaBtn); btnKutu.setAlignment(Pos.CENTER); btnKutu.setPadding(new Insets(8,0,0,0));
        VBox root = new VBox(12, baslik, altBaslik, sep0, nasılPane, tasPane, zorlukPane, btnKutu);
        root.setPadding(new Insets(22)); root.setAlignment(Pos.TOP_LEFT);
        root.setStyle("-fx-background-color:" + arka + ";"); root.setMinWidth(440);
        ScrollPane scroll = scrollWrap(root, arka);
        Scene sc = new Scene(scroll, 460, 640); theme.applyCss(sc);
        dialog.setScene(sc); dialog.showAndWait();
    }

    /** Leblebi mod ayar penceresi. */
    public void showLeblebDialog() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(s.pencere);
        dialog.setTitle("🫘 Mehmet Emmi'nin Leblebi Tarlası");
        dialog.setResizable(true);

        String arka = "#1a0e00", yazi = "#f5e6b0", vurgu = "#f0c040", panel = "#2a1800";

        Label baslik = new Label("🫘  MEHMET EMMİ'NİN LEBLEBİ TARLASI");
        baslik.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill:" + vurgu + ";" +
                "-fx-effect: dropshadow(gaussian, #c89a2a, 10, 0.4, 0, 2);");
        Label altBaslik = new Label("Bereketli topraklarda yılanlarla savaş, leblebi topla!");
        altBaslik.setStyle("-fx-font-size: 12px; -fx-text-fill: #a08040; -fx-font-style: italic;");
        Separator sep0 = sep("#6b3e00");

        // How-to
        VBox nasilPane = infoPane(panel, "#6b3e00");
        Label nasilBaslik = new Label("📖  Nasıl Oynanır?");
        nasilBaslik.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill:" + vurgu + ";");
        String[][] kurallar = {
            {"🖱 Sol Tık",     "Hücreyi açar. Yılana basarsan can kaybedersin!"},
            {"🚩 Sağ Tık",     "Şüpheli kareye bayrak diker / kaldırır."},
            {"🔢 Rakamlar",    "Komşu yılan sayısını gösterir."},
            {"❤️ Can Sistemi", "3 can ile başlarsın. Can bitince oyun biter."},
            {"⏳ Süre",        "Her seviyenin bir süre limiti var."},
            {"🏆 Kazanmak",    "Tüm yılansız kareleri aç!"},
        };
        nasilPane.getChildren().addAll(nasilBaslik, buildRuleGrid(kurallar, vurgu, yazi));

        // Market items
        VBox marketPane = infoPane(panel, "#6b3e00");
        Label marketBaslik = new Label("🛒  Mehmet Emmi'nin Dükkanı");
        marketBaslik.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill:" + vurgu + ";");
        Object[][] esyalar = {
            {"🐦 Karga",         "Gizli yılanları gösterir. Kullandıkça seviyesi artar!", "20 💰", "#a8d8a8"},
            {"⏰ Emmi'nin Saati","Süreye +30 saniye ekler.",                               "30 💰", "#a8c8f8"},
            {"🧪 Zirai İlaç",   "Tıkladığın bölgedeki yılanları yok eder!",              "50 💰", "#d8b8f8"},
            {"❤️ Ekstra Kalp",  "+1 can verir.",                                          "100 💰","#f8a8a8"},
        };
        GridPane esyaGrid = new GridPane(); esyaGrid.setHgap(10); esyaGrid.setVgap(6);
        for (int i = 0; i < esyalar.length; i++) {
            Label sym  = new Label((String) esyalar[i][0]);
            sym.setStyle("-fx-font-size: 13px; -fx-text-fill:" + esyalar[i][3] + "; -fx-min-width: 120; -fx-font-weight: bold;");
            Label desc = new Label((String) esyalar[i][1]);
            desc.setStyle("-fx-font-size: 12px; -fx-text-fill:" + yazi + ";"); desc.setWrapText(true);
            Label fiyat = new Label((String) esyalar[i][2]);
            fiyat.setStyle("-fx-font-size: 12px; -fx-text-fill:" + vurgu + "; -fx-font-weight: bold;");
            esyaGrid.add(sym, 0, i); esyaGrid.add(desc, 1, i); esyaGrid.add(fiyat, 2, i);
        }
        marketPane.getChildren().addAll(marketBaslik, esyaGrid);

        // Missions & golden leblebi
        VBox gorevPane = infoPane(panel, "#6b3e00");
        Label gorevBaslik = new Label("📋  Görevler & 🌟 Altın Leblebi");
        gorevBaslik.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill:" + vurgu + ";");
        Label gorevInfo = new Label(
            "Her seviyede 3 rastgele görev verilir. Tamamlayarak bonus puan ve altın kazan!\n\n" +
            "🌟 Altın Leblebi: Tarlada gizli altın leblebiler vardır. Bulduğunda +10 puan ve ekstra altın!");
        gorevInfo.setStyle("-fx-font-size: 12px; -fx-text-fill:" + yazi + ";"); gorevInfo.setWrapText(true);
        gorevPane.getChildren().addAll(gorevBaslik, gorevInfo);

        // Levels
        VBox seviyePane = infoPane(panel, "#6b3e00");
        Label seviyeBaslik = new Label("🗺  Seviyeler");
        seviyeBaslik.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill:" + vurgu + ";");
        VBox seviyeListesi = new VBox(4);
        for (Seviye sv : Seviye.SEVIYELER) {
            Label sl = new Label(String.format("Bölüm %d: %s  (%d×%d, %d yılan, %ds)",
                    sv.getNumara(), sv.getIsim(), sv.getSatirSayisi(), sv.getSutunSayisi(),
                    sv.getYilanSayisi(), sv.getSureSaniye()));
            sl.setStyle("-fx-font-size: 12px; -fx-text-fill:" + yazi + ";");
            seviyeListesi.getChildren().add(sl);
        }
        seviyePane.getChildren().addAll(seviyeBaslik, seviyeListesi);

        Button oynaBtn = goldButton("🌾  Tarlaya Gir!");
        oynaBtn.setOnAction(e -> {
            assets.stop(assets.sesCiftlik);
            s.mevcutSeviye = 1; s.toplamLeblebPuani = 0; s.kaliciAltin = 0;
            s.toplamKargaKullanim = 0; s.toplamIlacKullanim = 0;
            assets.play(assets.sesBasla);
            dialog.close();
            if (onStartLeblebi != null) onStartLeblebi.run();
        });
        HBox btnKutu = new HBox(oynaBtn); btnKutu.setAlignment(Pos.CENTER); btnKutu.setPadding(new Insets(8,0,0,0));

        VBox root = new VBox(12, baslik, altBaslik, sep0, nasilPane, marketPane, gorevPane, seviyePane, btnKutu);
        root.setPadding(new Insets(22)); root.setAlignment(Pos.TOP_LEFT);
        root.setStyle("-fx-background-color:" + arka + ";"); root.setMinWidth(440);
        ScrollPane scroll = scrollWrap(root, arka);
        Scene sc = new Scene(scroll, 480, 640); theme.applyCss(sc);
        dialog.setScene(sc); dialog.showAndWait();
    }

    // =========================================================================
    // How to play dialog
    // =========================================================================

    private void showHowToPlay() {
        Stage d = new Stage();
        d.initModality(Modality.APPLICATION_MODAL); d.initOwner(s.pencere);
        d.setTitle("Nasıl Oynanır?"); d.setResizable(false);
        String arka = "#12111f", yazi = "#c0b8e0", vurgu = "#7eb8ff";
        VBox panel = new VBox(12); panel.setPadding(new Insets(24));
        panel.setStyle("-fx-background-color:" + arka + ";");
        Label baslik = new Label("Mayın Tarlası — Nasıl Oynanır?");
        baslik.setStyle("-fx-font-size:16px; -fx-font-weight:bold; -fx-text-fill:" + vurgu + ";");
        List<String[]> liste = new ArrayList<>();
        liste.add(new String[]{"Sol Tık",  "Hücreyi açar."});
        liste.add(new String[]{"Sağ Tık",  "Bayrak diker / kaldırır."});
        liste.add(new String[]{"Sayılar",  "Komşu mayın / taş sayısını gösterir."});
        liste.add(new String[]{"Klasik",   "Tüm mayınsız kareleri açarak kazan."});
        liste.add(new String[]{"Satranç",  "Taşlar her hamlede hareket eder!"});
        if (s.leblebAcildi)
            liste.add(new String[]{"Leblebi", "Can sistemi + market ile oyna (gizli mod)."});
        GridPane grid = new GridPane(); grid.setHgap(12); grid.setVgap(8);
        for (int i = 0; i < liste.size(); i++) {
            Label k = new Label(liste.get(i)[0]);
            k.setStyle("-fx-font-size:13px; -fx-font-weight:bold; -fx-text-fill:" + vurgu + "; -fx-min-width:80;");
            Label v = new Label(liste.get(i)[1]);
            v.setStyle("-fx-font-size:13px; -fx-text-fill:" + yazi + ";");
            grid.add(k, 0, i); grid.add(v, 1, i);
        }
        Button kapat = new Button("Tamam");
        kapat.setStyle("-fx-background-color:#7eb8ff; -fx-text-fill:#12111f; -fx-font-weight:bold; " +
                       "-fx-background-radius:8; -fx-cursor:hand; -fx-padding:8 20;");
        kapat.setOnAction(e -> d.close());
        HBox btnBox = new HBox(kapat); btnBox.setAlignment(Pos.CENTER);
        panel.getChildren().addAll(baslik, new Separator(), grid, btnBox);
        Scene sc = new Scene(panel); theme.applyCss(sc); d.setScene(sc); d.showAndWait();
    }

    // =========================================================================
    // Easter egg popup
    // =========================================================================

    private void showEasterEggPopup(StackPane root, Button leblebBtn, Label etiket) {
        leblebBtn.setOpacity(0); leblebBtn.setVisible(true); leblebBtn.setManaged(true);

        VBox kutu = new VBox(14); kutu.setAlignment(Pos.CENTER);
        kutu.setPadding(new Insets(28, 36, 22, 36));
        kutu.setMaxWidth(420); kutu.setMaxHeight(VBox.USE_PREF_SIZE);
        kutu.setStyle("-fx-background-color: linear-gradient(to bottom, #3d2800, #1a0e00);" +
                "-fx-border-color: #e8b840; -fx-border-width: 2; -fx-border-radius: 16; -fx-background-radius: 16;");

        Label baslik = new Label("🫘  Mehmet Emmi'nin Leblebi Tarlası Açıldı!  🫘");
        baslik.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #ffd700;" +
                "-fx-effect: dropshadow(gaussian, #c89a2a, 12, 0.5, 0, 0);");
        baslik.setWrapText(true); baslik.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        Label altYazi = new Label("Gizli mod açıldı! Seni neler bekliyor...");
        altYazi.setStyle("-fx-font-size: 12px; -fx-text-fill: #a08040; -fx-font-style: italic;");

        String etiketStil = "-fx-font-size: 10px; -fx-text-fill: #c8a060; -fx-font-weight: bold; -fx-padding: 2 0 0 0;";
        javafx.util.Callback<String[], VBox> ikonOlustur = (args) -> {
            Label ikon = new Label(args[0]);
            ikon.setStyle("-fx-font-size: 28px; -fx-text-fill: " + args[2] + ";" +
                    "-fx-effect: dropshadow(gaussian, " + args[2] + ", 8, 0.4, 0, 0);");
            Label lbl = new Label(args[1]); lbl.setStyle(etiketStil);
            VBox v = new VBox(4, ikon, lbl); v.setAlignment(Pos.CENTER); return v;
        };
        HBox ikonlar = new HBox(20,
            ikonOlustur.call(new String[]{"❤️", "Can Sistemi", "#ff4060"}),
            ikonOlustur.call(new String[]{"🛒", "Eşyalar",     "#60c0ff"}),
            ikonOlustur.call(new String[]{"📋", "Görevler",    "#80e080"}),
            ikonOlustur.call(new String[]{"🌟", "Altın Leblebi","#ffd700"}),
            ikonOlustur.call(new String[]{"🏅", "Başarımlar",  "#e090ff"}));
        ikonlar.setAlignment(Pos.CENTER);

        Label ipucu = new Label("Detaylar için menüden Leblebi Tarlası'na tıkla!");
        ipucu.setStyle("-fx-font-size: 11px; -fx-text-fill: #8a6a30;");

        Button tamamBtn = new Button("Anladım Emmi");
        tamamBtn.setStyle("-fx-background-color: linear-gradient(to right, #c09000, #f0c040);" +
                "-fx-text-fill: #2a1500; -fx-font-size: 14px; -fx-font-weight: bold;" +
                "-fx-background-radius: 10; -fx-padding: 8 24; -fx-cursor: hand;");
        tamamBtn.setOnMouseEntered(e -> tamamBtn.setOpacity(0.85));
        tamamBtn.setOnMouseExited(e  -> tamamBtn.setOpacity(1.0));
        kutu.getChildren().addAll(baslik, altYazi, new Separator(), ikonlar, ipucu, tamamBtn);

        StackPane wrapper = new StackPane(kutu);
        wrapper.setStyle("-fx-background-color: rgba(0,0,0,0.6);");
        wrapper.setOpacity(0);
        root.getChildren().add(wrapper);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(350), wrapper);
        fadeIn.setFromValue(0); fadeIn.setToValue(1); fadeIn.play();

        tamamBtn.setOnAction(e -> {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(300), wrapper);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(ev -> {
                root.getChildren().remove(wrapper);
                etiket.setText("🫘 Mehmet Emmi'nin Leblebi Tarlası Modu Açık!");
                FadeTransition btnFade = new FadeTransition(Duration.millis(500), leblebBtn);
                btnFade.setFromValue(0); btnFade.setToValue(1);
                btnFade.setInterpolator(Interpolator.EASE_BOTH);
                btnFade.play();
            });
            fadeOut.play();
        });
        tamamBtn.requestFocus();
    }

    // =========================================================================
    // Private builders
    // =========================================================================

    private Button menuCard(String sembol, String baslik, String altBaslik, String vurguRenk, String arkaRenk) {
        Label sembolLbl = new Label(sembol);
        sembolLbl.setStyle("-fx-font-size: 38px; -fx-text-fill: " + vurguRenk + ";" +
                "-fx-effect: dropshadow(gaussian, " + vurguRenk + ", 12, 0.4, 0, 0);");
        Label baslikLbl = new Label(baslik);
        baslikLbl.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-letter-spacing: 2;" +
                "-fx-text-fill: #d0cce8; -fx-text-alignment: center;");
        baslikLbl.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        Label altBaslikLbl = new Label(altBaslik);
        altBaslikLbl.setStyle("-fx-font-size: 11px; -fx-letter-spacing: 1;" +
                "-fx-text-fill: " + vurguRenk + "; -fx-text-alignment: center;");
        altBaslikLbl.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        VBox kutu = new VBox(8, sembolLbl, baslikLbl, altBaslikLbl); kutu.setAlignment(Pos.CENTER);

        Button btn = new Button(); btn.setGraphic(kutu);
        btn.setPrefSize(220, 185); btn.setMinSize(190, 160);

        String normal =
            "-fx-background-color: linear-gradient(to bottom right, " + arkaRenk + ", #0e0c20);" +
            "-fx-border-color: #252840 #252840 " + vurguRenk + " #252840; -fx-border-width: 1 1 3 1;" +
            "-fx-background-radius: 14; -fx-border-radius: 14; -fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.7), 12, 0, 0, 5);";
        String hover =
            "-fx-background-color: linear-gradient(to bottom right, " + arkaRenk + "cc, #12102acc);" +
            "-fx-border-color: " + vurguRenk + " " + vurguRenk + " " + vurguRenk + " " + vurguRenk + ";" +
            "-fx-border-width: 1; -fx-background-radius: 14; -fx-border-radius: 14; -fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, " + vurguRenk + ", 20, 0.35, 0, 0);";

        btn.setStyle(normal);
        ScaleTransition stIn  = new ScaleTransition(Duration.millis(140), btn); stIn.setToX(1.05); stIn.setToY(1.05);
        ScaleTransition stOut = new ScaleTransition(Duration.millis(140), btn); stOut.setToX(1.0);  stOut.setToY(1.0);
        btn.setOnMouseEntered(e -> { btn.setStyle(hover);  stOut.stop(); stIn.play(); });
        btn.setOnMouseExited(e  -> { btn.setStyle(normal); stIn.stop();  stOut.play(); });
        return btn;
    }

    /**
     * Like menuCard but uses a real Image for the icon (with emoji fallback).
     * Used for the Leblebi button so altin_leblebi.png shows instead of 🫘.
     */
    private Button menuCardWithImage(javafx.scene.image.Image img, String fallbackEmoji,
                                     String baslik, String altBaslik,
                                     String vurguRenk, String arkaRenk) {
        javafx.scene.Node ikonNode;
        if (img != null) {
            javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(img);
            iv.setFitWidth(52); iv.setFitHeight(52);
            iv.setPreserveRatio(true); iv.setSmooth(true);
            // Golden glow effect to match the card's accent colour
            iv.setStyle("-fx-effect: dropshadow(gaussian, " + vurguRenk + ", 14, 0.5, 0, 0);");
            ikonNode = iv;
        } else {
            Label lbl = new Label(fallbackEmoji);
            lbl.setStyle("-fx-font-size: 38px; -fx-text-fill: " + vurguRenk + ";" +
                    "-fx-effect: dropshadow(gaussian, " + vurguRenk + ", 12, 0.4, 0, 0);");
            ikonNode = lbl;
        }
        Label baslikLbl = new Label(baslik);
        baslikLbl.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-letter-spacing: 2;" +
                "-fx-text-fill: #d0cce8; -fx-text-alignment: center;");
        baslikLbl.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        Label altBaslikLbl = new Label(altBaslik);
        altBaslikLbl.setStyle("-fx-font-size: 11px; -fx-letter-spacing: 1;" +
                "-fx-text-fill: " + vurguRenk + "; -fx-text-alignment: center;");
        altBaslikLbl.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        VBox kutu = new VBox(8, ikonNode, baslikLbl, altBaslikLbl); kutu.setAlignment(Pos.CENTER);

        Button btn = new Button(); btn.setGraphic(kutu);
        btn.setPrefSize(220, 185); btn.setMinSize(190, 160);

        String normal =
            "-fx-background-color: linear-gradient(to bottom right, " + arkaRenk + ", #0e0c20);" +
            "-fx-border-color: #252840 #252840 " + vurguRenk + " #252840; -fx-border-width: 1 1 3 1;" +
            "-fx-background-radius: 14; -fx-border-radius: 14; -fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.7), 12, 0, 0, 5);";
        String hover =
            "-fx-background-color: linear-gradient(to bottom right, " + arkaRenk + "cc, #12102acc);" +
            "-fx-border-color: " + vurguRenk + " " + vurguRenk + " " + vurguRenk + " " + vurguRenk + ";" +
            "-fx-border-width: 1; -fx-background-radius: 14; -fx-border-radius: 14; -fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, " + vurguRenk + ", 20, 0.35, 0, 0);";

        btn.setStyle(normal);
        ScaleTransition stIn  = new ScaleTransition(Duration.millis(140), btn); stIn.setToX(1.05); stIn.setToY(1.05);
        ScaleTransition stOut = new ScaleTransition(Duration.millis(140), btn); stOut.setToX(1.0);  stOut.setToY(1.0);
        btn.setOnMouseEntered(e -> { btn.setStyle(hover);  stOut.stop(); stIn.play(); });
        btn.setOnMouseExited(e  -> { btn.setStyle(normal); stIn.stop();  stOut.play(); });
        return btn;
    }

    private Button buildHowToPlayBtn() {
        Button btn = new Button();
        Label ikon = new Label("?");
        ikon.setStyle("-fx-font-size: 13px; -fx-text-fill: #8888aa; -fx-background-color: #1e2040;" +
                "-fx-background-radius: 50%; -fx-min-width: 22; -fx-min-height: 22; -fx-alignment: center;");
        Label yazi = new Label("Nasıl Oynanır?"); yazi.setStyle("-fx-font-size: 13px; -fx-text-fill: #8888aa;");
        HBox kutu = new HBox(6, ikon, yazi); kutu.setAlignment(Pos.CENTER_LEFT);
        btn.setGraphic(kutu);
        btn.setStyle("-fx-background-color: transparent; -fx-border-color: #2a3060; -fx-border-width: 1;" +
                "-fx-border-radius: 20; -fx-background-radius: 20; -fx-cursor: hand; -fx-padding: 6 16;");
        btn.setOnMouseEntered(e -> btn.setStyle(btn.getStyle().replace("transparent", "#1a2040")));
        btn.setOnMouseExited(e  -> btn.setStyle(btn.getStyle().replace("#1a2040", "transparent")));
        return btn;
    }

    private Button buildExitBtn() {
        Button btn = new Button();
        javafx.scene.Node ikon = mkImgLabel(assets.imgOffBtn, "⏻", 18);
        Label yazi = new Label("Çıkış"); yazi.setStyle("-fx-font-size: 13px; -fx-text-fill: #8888aa;");
        HBox kutu = new HBox(6, ikon, yazi); kutu.setAlignment(Pos.CENTER_RIGHT);
        btn.setGraphic(kutu);
        btn.setStyle("-fx-background-color: transparent; -fx-border-color: #2a3060; -fx-border-width: 1;" +
                "-fx-border-radius: 20; -fx-background-radius: 20; -fx-cursor: hand; -fx-padding: 6 16;");
        btn.setOnMouseEntered(e -> btn.setStyle(btn.getStyle().replace("transparent", "#1a2040")));
        btn.setOnMouseExited(e  -> btn.setStyle(btn.getStyle().replace("#1a2040", "transparent")));
        btn.setOnAction(e -> javafx.application.Platform.exit());
        return btn;
    }

    /** Helper: returns an ImageView if image is non-null, otherwise a styled emoji Label. */
    private javafx.scene.Node mkImgLabel(javafx.scene.image.Image img, String fallbackEmoji, double size) {
        if (img != null) {
            javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(img);
            iv.setFitWidth(size); iv.setFitHeight(size);
            iv.setPreserveRatio(true); iv.setSmooth(true);
            return iv;
        }
        Label lbl = new Label(fallbackEmoji);
        lbl.setStyle("-fx-font-size: " + (int)size + "px; -fx-text-fill: #8888aa;");
        return lbl;
    }

    private GridPane buildRuleGrid(String[][] rules, String keyColor, String valColor) {
        GridPane grid = new GridPane(); grid.setHgap(10); grid.setVgap(6);
        for (int i = 0; i < rules.length; i++) {
            Label k = new Label(rules[i][0]);
            k.setStyle("-fx-font-size:12px; -fx-font-weight:bold; -fx-text-fill:" + keyColor + "; -fx-min-width:100;");
            Label v = new Label(rules[i][1]);
            v.setStyle("-fx-font-size:12px; -fx-text-fill:" + valColor + ";"); v.setWrapText(true);
            grid.add(k, 0, i); grid.add(v, 1, i);
        }
        return grid;
    }

    private VBox infoPane(String bg, String border) {
        VBox p = new VBox(8); p.setPadding(new Insets(12));
        p.setStyle("-fx-background-color:" + bg + "; -fx-border-color:" + border +
                "; -fx-border-radius:10; -fx-background-radius:10;");
        return p;
    }

    private Button goldButton(String text) {
        Button btn = new Button(text); btn.setPrefWidth(220); btn.setPrefHeight(46);
        btn.setStyle("-fx-background-color: linear-gradient(to right, #c09000, #f0c040);" +
                "-fx-text-fill:#0a0918; -fx-font-size:15px; -fx-font-weight:bold;" +
                "-fx-background-radius:12; -fx-cursor:hand;" +
                "-fx-effect: dropshadow(gaussian,#f0c04088,12,0.4,0,2);");
        btn.setOnMouseEntered(e -> btn.setOpacity(0.88));
        btn.setOnMouseExited(e  -> btn.setOpacity(1.0));
        return btn;
    }

    private Separator sep(String color) {
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color:" + color + ";");
        return sep;
    }

    private ScrollPane scrollWrap(javafx.scene.Node content, String arka) {
        ScrollPane sc = new ScrollPane(content); sc.setFitToWidth(true);
        sc.setStyle("-fx-background-color:" + arka + "; -fx-background:" + arka + ";");
        return sc;
    }

    private Spinner<Integer> makeSpinner(int min, int max, int val) {
        Spinner<Integer> sp = new Spinner<>(min, max, Math.max(min, Math.min(max, val)));
        sp.setEditable(true); sp.setPrefWidth(100); return sp;
    }

    private Label labelFor(String text, String color) {
        Label l = new Label(text); l.setStyle("-fx-text-fill:" + color + ";-fx-font-size:13px;"); return l;
    }
}
