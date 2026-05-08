import javafx.animation.*;
import javafx.application.Application;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.effect.*;
import javafx.scene.image.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.media.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.stage.*;
import javafx.util.Duration;

import java.util.*;

/**
 * MinesweeperApp — Pure UI layer.
 *
 * Modlar:
 *  - KlasikBoardMode  : Klasik mayın tarlası (kronometre veya geri sayım)
 *  - LeblebiBoardMode : Mehmet Emmi'nin Leblebi Tarlası (easter egg)
 *  - ChessBoardMode   : ♟ Satranç Mayın Tarlası (YENİ)
 */
public class MinesweeperApp extends Application {

    // ── Classic mode lives constant ───────────────────────────────────────────
    private static final int KLASIK_CAN = 3;

    // ── Theme colours ─────────────────────────────────────────────────────────
    // Karanlık tema — Midnight Indigo
    private static final String KT_ARKAPLAN  = "#12111f";
    private static final String KT_ACILMAMIS = "#3b3756";
    private static final String KT_ACILMIS   = "#0a0918";
    private static final String KT_ISARETLI  = "#4e3d7a";
    private static final String KT_CERCEVE   = "#5a5478";
    private static final String KT_YAZI      = "#e2dcf8";
    private static final String KT_YAZI_SOLUK= "#7b748a";
    private static final String KT_UST_BAR   = "#0e0d1c";

    // Açık tema — Soft Lavender
    private static final String AT_ARKAPLAN  = "#eef0f8";
    private static final String AT_ACILMAMIS = "#b8bcd8";
    private static final String AT_ACILMIS   = "#f5f5fc";
    private static final String AT_ISARETLI  = "#a8aed0";
    private static final String AT_CERCEVE   = "#8890b0";
    private static final String AT_YAZI      = "#1a1830";
    private static final String AT_YAZI_SOLUK= "#8890b0";
    private static final String AT_UST_BAR   = "#d4d8ee";

    // Leblebi tema — Golden Harvest
    private static final String LB_ARKAPLAN  = "#2e1a00";
    private static final String LB_ACILMAMIS = "#c8922a";
    private static final String LB_ACILMIS   = "#6b3e10";
    private static final String LB_ISARETLI  = "#e8b840";
    private static final String LB_YILAN_RENK= "#3a6e25";
    private static final String LB_CERCEVE   = "#8c6018";
    private static final String LB_UST_BAR   = "linear-gradient(to bottom, #6b3e10, #3a1e00)";
    private static final String LB_KARGA_RENK= "#c0392b";

    // ── Satranç modu renkleri — Ivory & Obsidian ─────────────────────────────
    private static final String CH_ARKAPLAN  = "#0a1628";
    private static final String CH_ACILMAMIS_LIGHT = "#f0ead8";  // Fildişi
    private static final String CH_ACILMAMIS_DARK  = "#2c1f0e";  // Koyu kahve
    private static final String CH_ACILMIS   = "#0d2e54";
    private static final String CH_CERCEVE   = "#1e3a5f";
    private static final String CH_UST_BAR   = "#060e1e";
    private static final String CH_ISARETLI  = "#3d1a5a";

    private static final String[] KT_SAYI_RENK = {
            "", "#7eb8ff", "#98e0a0", "#ff8080", "#60c8ec",
            "#ffaa70", "#70d8eb", "#c0a8ff", "#e2dcf8" };
    private static final String[] AT_SAYI_RENK = {
            "", "#1a56b0", "#276830", "#b82020", "#0060a0",
            "#c85000", "#007880", "#5a1090", "#304050" };
    private static final String[] LB_SAYI_RENK = {
            "", "#e8c27a", "#6dbe45", "#d95f3b", "#5ba3d6",
            "#d4a050", "#40d4b0", "#c080e0", "#f0c060" };
    private static final String[] CH_SAYI_RENK = {
            "", "#5bc8f8", "#7eca80", "#f07070", "#d090e0",
            "#ffc060", "#40d8e8", "#f080b8", "#a8b8c8" };

    // ── Application state ─────────────────────────────────────────────────────
    private Stage     pencere;
    private Scene     anaSahne;
    private Scene     menuSahne;

    // ── Single persistent scene — swapping root avoids any window resize ──────
    private Scene     rootScene;
    private StackPane rootWrapper;

    private KlasikBoardMode   klasikBoardMode;
    private LeblebiBoardMode  leblebiBoardMode;
    private ChessBoardMode    chessBoardMode;   // ← YENİ

    private boolean leblebModu  = false;
    private boolean satranModu  = false;         // ← YENİ
    private boolean karanlikTema = true;

    // Level system (Leblebi only)
    private int mevcutSeviye       = 1;
    private int toplamLeblebPuani  = 0;
    private int kaliciAltin        = 0;
    private int toplamKargaKullanim= 0;
    private int toplamIlacKullanim = 0;

    // Grid
    private Button[][] dugmeler;
    private int satirSayisi, sutunSayisi, mayinSayisi;

    // Dirty-cell tracking
    private byte[][] hucreDurum;

    private java.util.Set<Integer> yilanHucreleri = new java.util.HashSet<>();

    private Timeline  aktifSarsinti;
    private StackPane anaSahneKoku;
    private StackPane merkezIcerikKutusu;

    private int yerlestirilenIsaret;

    // UI components
    private Label  maynSayaciEtiketi;
    private Label  zamanlayiciEtiketi;
    private Label  durumEtiketi;
    private Label  canEtiketi;
    private Label  puanEtiketi;
    private Label  altinEtiketi;
    private Button sifirlaBtn;
    private Timeline  zamanlayici;
    private BorderPane kokDuzen;
    private GridPane   izgaraDuzen;
    private Scene      sahne;
    private VBox  marketPanel;
    private HBox  canIkonKutusu;
    private VBox  gorevPanel;

    // Gece modu
    private javafx.scene.canvas.Canvas geceKanvasi;
    private double fenerX = -1000, fenerY = -1000;
    private double fenerBaseYaricap = 165;
    private double fenerPulseOffset = 0;
    private Timeline nefesAnimasyonu;

    // Konuşma balonu
    private StackPane konusmaBalonuPanel;
    private Label     konusmaBalonuLabel;
    private Animation aktifBalonAnimasyonu;

    // Easter Egg
    private static final String EASTER_EGG_KOD = "1837837";
    private StringBuilder basiliKodBuffer = new StringBuilder();
    private boolean leblebAcildi = false;

    // Sound
    private AudioClip sesKazma;
    private AudioClip sesPatlama;
    private AudioClip sesButon;
    private AudioClip sesMarket;
    private AudioClip sesKazan;

    // Asset paths
    private static final String ASSET_YILAN  = "assets/solucan.png";
    private static final String ASSET_LEBLEBI= "assets/leblebi.png";
    private static final String ASSET_BAYRAK = "assets/bayrak.png";
    private static final String ASSET_MAYIN  = "assets/mayin.png";

    private Image imgYilan;
    private Image imgLeblebi;
    private Image imgBayrak;
    private Image imgMayin;

    private static final String FONT_YOL = "assets/fonts/GameFont.ttf";

    // =========================================================================
    // start()
    // =========================================================================

    @Override
    public void start(Stage pencere) {
        this.pencere = pencere;
        rootWrapper = new StackPane();
        rootScene   = new Scene(rootWrapper, 660, 580);
        pencere.setScene(rootScene);
        pencere.setTitle("Mayın Tarlası");
        pencere.setMinWidth(560);
        pencere.setMinHeight(500);
        pencere.setResizable(true);
        pencere.centerOnScreen();
        pencere.show();
        menuGoster();

        Thread yukleyici = new Thread(() -> {
            sesFxYukle();
            assetleriOnYukle();
        }, "asset-loader");
        yukleyici.setDaemon(true);
        yukleyici.start();
    }

    // =========================================================================
    // Assets & Sound
    // =========================================================================

    private void sesFxYukle() {
        sesKazma   = sesFxYukleGuveli("sounds/kazma.mp3");
        sesPatlama = sesFxYukleGuveli("sounds/patlama.mp3");
        sesButon   = sesFxYukleGuveli("sounds/buton.mp3");
        sesMarket  = sesFxYukleGuveli("sounds/market.mp3");
        sesKazan   = sesFxYukleGuveli("sounds/kazan.mp3");
    }

    private void assetleriOnYukle() {
        imgYilan   = assetImgYukle(ASSET_YILAN,   20);
        imgLeblebi = assetImgYukle(ASSET_LEBLEBI, 20);
        imgBayrak  = assetImgYukle(ASSET_BAYRAK,  18);
        imgMayin   = assetImgYukle(ASSET_MAYIN,   20);
    }

    private Image assetImgYukle(String yol, double boyut) {
        try {
            java.net.URL url = getClass().getResource(yol);
            if (url == null) url = new java.io.File(yol).toURI().toURL();
            return new Image(url.toString(), boyut, boyut, true, true);
        } catch (Exception e) { return null; }
    }

    private AudioClip sesFxYukleGuveli(String yol) {
        try {
            java.net.URL url = getClass().getResource(yol);
            if (url == null) url = new java.io.File(yol).toURI().toURL();
            return new AudioClip(url.toString());
        } catch (Exception e) { return null; }
    }

    private void sesCal(AudioClip klip) { if (klip != null) klip.play(); }

    // =========================================================================
    // Menu
    // =========================================================================

    private void menuGoster() {
        // ── Arka plan ve gradient ──────────────────────────────────────────────
        VBox kok = new VBox(0);
        kok.setAlignment(Pos.CENTER);
        kok.setStyle(
            "-fx-background-color: linear-gradient(to bottom, #0a0918 0%, #12111f 60%, #0e0d1c 100%);");

        // ── Başlık (Constantine benzeri büyük yazı) ────────────────────────────
        Label baslik = new Label("MAYIN TARLASI");
        baslik.setStyle(
            "-fx-font-family: 'Constantine', 'Palatino Linotype', 'Book Antiqua', serif;" +
            "-fx-font-size: 52px; -fx-font-weight: bold;" +
            "-fx-text-fill: linear-gradient(to bottom, #e8dff8, #a090d0);" +
            "-fx-effect: dropshadow(gaussian, #7060c0, 20, 0.5, 0, 3);");
        baslik.setMaxWidth(Double.MAX_VALUE);
        baslik.setAlignment(javafx.geometry.Pos.CENTER);
        baslik.setPadding(new Insets(48, 0, 8, 0));


        // ── 3 üst kare buton ──────────────────────────────────────────────────
        Button klasikBtn  = kareMenuButon("⛏", "Klasik\nMayın Tarlası", "#7eb8ff", "#1a1838");
        Button satranBtn  = kareMenuButon("♟", "Satranç\nMayın Tarlası", "#f0c040", "#1a1608");
        Button skorBtn    = kareMenuButon("🏆", "Skor\nTablosu",        "#7eda90", "#081808");

        klasikBtn.setOnAction(e -> { sesCal(sesButon); leblebModu = false; satranModu = false; klasikOyunuBaslat(); });
        satranBtn.setOnAction(e -> { sesCal(sesButon); leblebModu = false; satranModu = true;  satranOyunuBaslat(); });
        skorBtn.setOnAction(e   -> { sesCal(sesButon); skorTablosunuGoster(); });


        // ── Alt merkez kare buton (Leblebi — gizli) ───────────────────────────
        Button leblebBtn = kareMenuButon("🫘", "Mehmet Emmi'nin\nLeblebi Tarlası", "#e8b840", "#251200");
        leblebBtn.setVisible(leblebAcildi);
        leblebBtn.setManaged(leblebAcildi);
        leblebBtn.setId("leblebBtn");
        leblebBtn.setOnAction(e -> {
            sesCal(sesButon);
            leblebModu = true; satranModu = false;
            mevcutSeviye = 1; toplamLeblebPuani = 0; kaliciAltin = 0;
            toplamKargaKullanim = 0; toplamIlacKullanim = 0;
            fenerBaseYaricap = 165;
            leblebOyunuBaslat();
        });
        HBox ustSira = new HBox(20, klasikBtn, satranBtn, leblebBtn);
        ustSira.setAlignment(Pos.CENTER);
        ustSira.setPadding(new Insets(0, 40, 16, 40));

        HBox altSira = new HBox(skorBtn);
        altSira.setAlignment(Pos.CENTER);
        altSira.setPadding(new Insets(0, 40, 40, 40));

        Label easterEggEtiketi = new Label(leblebAcildi ? "🫘 Mehmet Emmi'nin Leblebi Tarlası Modu Açık!" : "");
        easterEggEtiketi.setId("easterEggEtiket");
        easterEggEtiketi.setStyle("-fx-font-size: 13px; -fx-text-fill: #c89a2a; -fx-font-weight: bold;");
        easterEggEtiketi.setAlignment(Pos.CENTER);
        easterEggEtiketi.setMaxWidth(Double.MAX_VALUE);

        kok.getChildren().addAll(baslik, ustSira, altSira, easterEggEtiketi);

        StackPane kokDuzenleyici = new StackPane(kok);
        kokDuzenleyici.setId("menuRoot");

        menuSahne = rootScene;
        rootScene.setOnKeyPressed(olay -> {
            if (leblebAcildi) return;
            String k = olay.getText();
            if (!k.isEmpty() && "1234567890".contains(k)) {
                basiliKodBuffer.append(k);
                if (basiliKodBuffer.length() > EASTER_EGG_KOD.length())
                    basiliKodBuffer.delete(0, basiliKodBuffer.length() - EASTER_EGG_KOD.length());
                if (basiliKodBuffer.toString().equals(EASTER_EGG_KOD)) {
                    leblebAcildi = true;
                    basiliKodBuffer.setLength(0);
                    easterEggTetikle(kok, leblebBtn, easterEggEtiketi);
                }
            }
        });

        globalCssUygula(rootScene);
        rootWrapper.getChildren().setAll(kokDuzenleyici);
    }

    /** Kare menü butonu — ikon + iki satır yazı */
    private Button kareMenuButon(String ikon, String etiket, String vurguRenk, String yazıArka) {
        Label ikonLbl = new Label(ikon);
        ikonLbl.setStyle("-fx-font-size: 36px;");

        Label etiketLbl = new Label(etiket);
        etiketLbl.setStyle(
            "-fx-font-size: 13px; -fx-font-weight: bold;" +
            "-fx-text-fill: " + vurguRenk + ";" +
            "-fx-text-alignment: center;");
        etiketLbl.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        etiketLbl.setWrapText(true);

        VBox kutu = new VBox(8, ikonLbl, etiketLbl);
        kutu.setAlignment(Pos.CENTER);

        Button btn = new Button();
        btn.setGraphic(kutu);
        btn.setPrefSize(170, 150);
        btn.setMinSize(150, 130);
        btn.setStyle(
            "-fx-background-color: linear-gradient(to bottom right, #1e1c34, #16142a);" +
            "-fx-border-color: " + vurguRenk + ";" +
            "-fx-border-width: 2;" +
            "-fx-background-radius: 16; -fx-border-radius: 16;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 10, 0, 0, 4);");

        ScaleTransition stIn  = new ScaleTransition(Duration.millis(140), btn); stIn.setToX(1.06);  stIn.setToY(1.06);
        ScaleTransition stOut = new ScaleTransition(Duration.millis(140), btn); stOut.setToX(1.0); stOut.setToY(1.0);
        btn.setOnMouseEntered(e -> {
            btn.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #2a2848, #1e1c38);" +
                "-fx-border-color: " + vurguRenk + ";" +
                "-fx-border-width: 2.5;" +
                "-fx-background-radius: 16; -fx-border-radius: 16;" +
                "-fx-cursor: hand;" +
                "-fx-effect: dropshadow(gaussian, " + vurguRenk + ", 16, 0.4, 0, 0);");
            stOut.stop(); stIn.play();
        });
        btn.setOnMouseExited(e -> {
            btn.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #1e1c34, #16142a);" +
                "-fx-border-color: " + vurguRenk + ";" +
                "-fx-border-width: 2;" +
                "-fx-background-radius: 16; -fx-border-radius: 16;" +
                "-fx-cursor: hand;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 10, 0, 0, 4);");
            stIn.stop(); stOut.play();
        });
        return btn;
    }

       // ── Easter Egg ────────────────────────────────────────────────────────────

    private void easterEggTetikle(VBox kok, Button leblebBtn, Label etiket) {
        TranslateTransition sarsinti = new TranslateTransition(Duration.millis(60), kok);
        sarsinti.setByX(12); sarsinti.setCycleCount(8); sarsinti.setAutoReverse(true);

        Timeline flash = new Timeline(
                new KeyFrame(Duration.millis(0),   e -> kok.setStyle("-fx-background-color: #5c3a00;")),
                new KeyFrame(Duration.millis(150),  e -> kok.setStyle("-fx-background-color: #c89a2a;")),
                new KeyFrame(Duration.millis(300),  e -> kok.setStyle("-fx-background-color: #3d2800;")),
                new KeyFrame(Duration.millis(450),  e -> kok.setStyle("-fx-background-color: " + KT_ARKAPLAN + ";")));

        sarsinti.play(); flash.play();
        leblebBtn.setVisible(true); leblebBtn.setManaged(true);
        etiket.setText("🫘 Mehmet Emmi'nin Leblebi Tarlası Modu Açıldı!");

        if (kok.getParent() instanceof StackPane) {
            StackPane root = (StackPane) kok.getParent();
            VBox overlay = new VBox(20);
            overlay.setAlignment(Pos.CENTER);
            overlay.setStyle(
                    "-fx-background-color: radial-gradient(center 50% 50%, radius 70%, rgba(200, 154, 42, 0.9), rgba(61, 40, 0, 0.95));");

            Label unlem = new Label("🌟 ALTIN TARLA AÇILDI! 🌟");
            unlem.setStyle(
                    "-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #fffbe6; -fx-effect: dropshadow(gaussian, #c89a2a, 15, 0.5, 0, 0);");

            Label msg = new Label(
                    "Burası bereketli topraklardır evlat.\nBurada leblebi eker, yılan biçersin.\nAltın leblebiyi bulursan yaşadıın!");
            msg.setStyle("-fx-font-size: 16px; -fx-text-fill: #f5e6b0; -fx-text-alignment: center;");
            msg.setWrapText(true);
            msg.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

            Button tamamBtn = new Button("Anladım Emmi");
            tamamBtn.setStyle(
                    "-fx-background-color: #f0c040; -fx-text-fill: #3d2800; -fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 10 20; -fx-cursor: hand;");

            overlay.getChildren().addAll(unlem, msg, tamamBtn);
            root.getChildren().add(overlay);

            ScaleTransition st = new ScaleTransition(Duration.millis(500), overlay);
            st.setFromX(0.5); st.setFromY(0.5); st.setToX(1.0); st.setToY(1.0);
            st.setInterpolator(javafx.animation.Interpolator.EASE_OUT);
            FadeTransition ft = new FadeTransition(Duration.millis(500), overlay);
            ft.setFromValue(0); ft.setToValue(1);
            new ParallelTransition(st, ft).play();

            tamamBtn.setOnAction(e -> {
                FadeTransition out = new FadeTransition(Duration.millis(300), overlay);
                out.setToValue(0);
                out.setOnFinished(ev -> root.getChildren().remove(overlay));
                out.play();
            });
            tamamBtn.requestFocus();
        }
    }

    // =========================================================================
    // Classic game — setup dialog
    // =========================================================================

    private void klasikOyunuBaslat() { klasikAyarDialogGoster(); }

    private void klasikAyarDialogGoster() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(pencere);
        dialog.setTitle("Klasik Oyun Ayarları");
        dialog.setResizable(false);

        String arka = "#1e1e2e", yazi = "#cdd6f4", vurgu = "#89b4fa",
               girdiArka = "#181825", ayrac = "#313244";

        Label presetBaslik = new Label("Hazır Zorluklar");
        presetBaslik.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:" + vurgu + ";");

        GridPane presetGrid = new GridPane();
        presetGrid.setHgap(8); presetGrid.setVgap(8);

        Spinner<Integer> satirSpinner = yapSpinner(7, 30, 10);
        Spinner<Integer> sutunSpinner = yapSpinner(7, 30, 10);
        int minMayinSayisi = (int) Math.floor(
                satirSpinner.getValueFactory().getValue() * sutunSpinner.getValueFactory().getValue() * 0.15);
        Spinner<Integer> mayinSpinner = yapSpinner(minMayinSayisi, 500, 15);
        satirSpinner.getValueFactory().valueProperty().addListener((obs, eski, yeni) -> {
            int yeniMin = (int) Math.floor(yeni * sutunSpinner.getValueFactory().getValue() * 0.15);
            ((SpinnerValueFactory.IntegerSpinnerValueFactory) mayinSpinner.getValueFactory()).setMin(yeniMin);
            mayinSpinner.getValueFactory().setValue(yeniMin);
        });
        sutunSpinner.getValueFactory().valueProperty().addListener((obs, eski, yeni) -> {
            int yeniMin = (int) Math.floor(satirSpinner.getValueFactory().getValue() * yeni * 0.15);
            ((SpinnerValueFactory.IntegerSpinnerValueFactory) mayinSpinner.getValueFactory()).setMin(yeniMin);
            mayinSpinner.getValueFactory().setValue(yeniMin);
        });

        ToggleGroup timerGrup = new ToggleGroup();
        RadioButton yukariBtn = new RadioButton("⬆ Yukarı sayar (kronometre)");
        RadioButton geriBtn   = new RadioButton("⬇ Geri sayar (süre sınırlı)");
        yukariBtn.setToggleGroup(timerGrup); geriBtn.setToggleGroup(timerGrup);
        yukariBtn.setSelected(true);
        yukariBtn.setStyle("-fx-text-fill:" + yazi + ";");
        geriBtn.setStyle("-fx-text-fill:" + yazi + ";");
        Spinner<Integer> sureSpinner = yapSpinner(10, 3600, 180);
        sureSpinner.setDisable(true);
        geriBtn.selectedProperty().addListener((obs, eski, yeni) -> sureSpinner.setDisable(!yeni));

        for (int i = 0; i < KlasikBoardMode.PRESETLER.length; i++) {
            KlasikBoardMode.KlasikAyar p = KlasikBoardMode.PRESETLER[i];
            Button pb = new Button(p.etiket());
            pb.setPrefWidth(200);
            pb.setStyle("-fx-background-color:#313244;-fx-text-fill:" + yazi + ";" +
                        "-fx-background-radius:8;-fx-border-radius:8;" +
                        "-fx-border-color:" + ayrac + ";-fx-cursor:hand;-fx-font-size:12px;");
            pb.setOnMouseEntered(e -> pb.setStyle(pb.getStyle().replace("#313244", "#45475a")));
            pb.setOnMouseExited(e  -> pb.setStyle(pb.getStyle().replace("#45475a", "#313244")));
            pb.setOnAction(e -> {
                satirSpinner.getValueFactory().setValue(p.satir());
                sutunSpinner.getValueFactory().setValue(p.sutun());
                mayinSpinner.getValueFactory().setValue(p.mayin());
                if (p.geriSayim()) { geriBtn.setSelected(true); sureSpinner.getValueFactory().setValue(p.sure()); }
                else yukariBtn.setSelected(true);
            });
            presetGrid.add(pb, i % 2, i / 2);
        }

        Label ozelBaslik = new Label("Ya da Özel Ayarla");
        ozelBaslik.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:" + vurgu + ";");

        Runnable mayinKlamp = () -> {
            int maks = Math.max(1, satirSpinner.getValue() * sutunSpinner.getValue() - 9);
            ((SpinnerValueFactory.IntegerSpinnerValueFactory) mayinSpinner.getValueFactory()).setMax(maks);
        };
        satirSpinner.valueProperty().addListener((o, e, n) -> mayinKlamp.run());
        sutunSpinner.valueProperty().addListener((o, e, n) -> mayinKlamp.run());

        GridPane ozelGrid = new GridPane();
        ozelGrid.setHgap(12); ozelGrid.setVgap(10);
        ozelGrid.addRow(0, etiketOlustur("Satır sayısı:", yazi), satirSpinner);
        ozelGrid.addRow(1, etiketOlustur("Sütun sayısı:", yazi), sutunSpinner);
        ozelGrid.addRow(2, etiketOlustur("Mayın sayısı:", yazi), mayinSpinner);

        Label timerBaslik = new Label("Zamanlayıcı Modu");
        timerBaslik.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:" + vurgu + ";");

        GridPane timerGrid = new GridPane();
        timerGrid.setHgap(12); timerGrid.setVgap(8);
        timerGrid.addRow(0, yukariBtn);
        timerGrid.addRow(1, geriBtn);
        timerGrid.addRow(2, etiketOlustur("Süre (saniye):", yazi), sureSpinner);

        for (Spinner<?> sp : new Spinner<?>[]{ satirSpinner, sutunSpinner, mayinSpinner, sureSpinner })
            sp.setStyle("-fx-background-color:" + girdiArka + ";-fx-text-fill:" + yazi + ";");

        Button oynaBtn = new Button("▶  Oyna!");
        oynaBtn.setPrefWidth(180); oynaBtn.setPrefHeight(44);
        oynaBtn.setStyle("-fx-background-color:#89b4fa;-fx-text-fill:#1e1e2e;" +
                         "-fx-font-size:15px;-fx-font-weight:bold;-fx-background-radius:10;-fx-cursor:hand;");
        oynaBtn.setOnAction(e -> {
            satirSayisi = satirSpinner.getValue();
            sutunSayisi = sutunSpinner.getValue();
            mayinSayisi = Math.min(mayinSpinner.getValue(), satirSayisi * sutunSayisi - 9);
            boolean geri = geriBtn.isSelected();
            int sure = geri ? sureSpinner.getValue() : 0;
            leblebModu = false; satranModu = false; leblebiBoardMode = null; chessBoardMode = null;
            klasikBoardMode = new KlasikBoardMode(satirSayisi, sutunSayisi, mayinSayisi, geri, sure);
            dialog.close();
            oyunSahnesiniBaSlat(false, false);
        });

        Separator sep1 = new Separator(); sep1.setStyle("-fx-background-color:" + ayrac + ";");
        Separator sep2 = new Separator(); sep2.setStyle("-fx-background-color:" + ayrac + ";");

        VBox kok = new VBox(14, presetBaslik, presetGrid, sep1, ozelBaslik, ozelGrid, sep2,
                timerBaslik, timerGrid, oynaBtn);
        kok.setPadding(new Insets(24)); kok.setAlignment(Pos.CENTER_LEFT);
        kok.setStyle("-fx-background-color:" + arka + ";");

        Scene dialogSahne = new Scene(kok);
        globalCssUygula(dialogSahne);
        dialog.setScene(dialogSahne);
        dialog.showAndWait();
    }

    // =========================================================================
    // ── YENİ: Satranç Modu ───────────────────────────────────────────────────
    // =========================================================================

    private void satranOyunuBaslat() {
        satranAyarDialogGoster();
    }

    private void satranAyarDialogGoster() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(pencere);
        dialog.setTitle("♟ Satranç Mayın Tarlası");
        dialog.setResizable(true);

        String arka = CH_ARKAPLAN, yazi = "#d0cce8", vurgu = "#f0c040", panel = "#0f2240";

        // ── Başlık ─────────────────────────────────────────────────────────────
        Label baslik = new Label("♟  SATRANÇ MAYIN TARLASI");
        baslik.setStyle(
            "-fx-font-family: 'Constantine', 'Palatino Linotype', serif;" +
            "-fx-font-size:22px; -fx-font-weight:bold; -fx-text-fill:" + vurgu + ";" +
            "-fx-effect: dropshadow(gaussian,#c09000,10,0.4,0,2);");

        Label altBaslik = new Label("Her hamlenin ardında taşlar hareket eder. Dikkatli ol!");
        altBaslik.setStyle("-fx-font-size:12px; -fx-text-fill:#7a90b0; -fx-font-style:italic;");

        Separator sep0 = new Separator(); sep0.setStyle("-fx-background-color:#1e3a5f;");

        // ── Nasıl Oynanır paneli ───────────────────────────────────────────────
        VBox nasılPane = new VBox(8);
        nasılPane.setPadding(new Insets(12));
        nasılPane.setStyle(
            "-fx-background-color:" + panel + ";" +
            "-fx-border-color:#1e3a5f; -fx-border-radius:10; -fx-background-radius:10;");

        Label nasılBaslik = new Label("📖  Nasıl Oynanır?");
        nasılBaslik.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:" + vurgu + ";");

        String[][] kurallar = {
            {"🖱 Sol Tık",    "Kareyi açar. Taşa basarsan oyun biter!"},
            {"🚩 Sağ Tık",    "Şüpheli kareye bayrak diker / kaldırır."},
            {"🔢 Rakamlar",   "O kareyi kaç taş tehdit ettiğini gösterir."},
            {"⚡ Dinamik",    "Her güvenli açış sonrası tüm taşlar hareket eder!"},
            {"🏆 Kazanmak",   "Tüm taşsız kareleri aç, bayraklar gerekmez."},
            {"⏳ Süre",       "Süre dolmadan tahtayı temizlemelisin."},
            {"🎯 Strateji",   "Sayıları dikkatlice okuyarak tehlikeli bölgeleri tespit et."},
        };

        GridPane kuralGrid = new GridPane();
        kuralGrid.setHgap(10); kuralGrid.setVgap(6);
        for (int i = 0; i < kurallar.length; i++) {
            Label kl = new Label(kurallar[i][0]);
            kl.setStyle("-fx-font-size:12px; -fx-font-weight:bold; -fx-text-fill:#f0c040; -fx-min-width:90;");
            Label vl = new Label(kurallar[i][1]);
            vl.setStyle("-fx-font-size:12px; -fx-text-fill:" + yazi + ";");
            vl.setWrapText(true);
            kuralGrid.add(kl, 0, i);
            kuralGrid.add(vl, 1, i);
        }
        nasılPane.getChildren().addAll(nasılBaslik, kuralGrid);

        // ── Taş hareketleri ───────────────────────────────────────────────────
        VBox tasPane = new VBox(8);
        tasPane.setPadding(new Insets(12));
        tasPane.setStyle(
            "-fx-background-color:" + panel + ";" +
            "-fx-border-color:#1e3a5f; -fx-border-radius:10; -fx-background-radius:10;");

        Label tasBaslik = new Label("♟  Taş Hareketleri");
        tasBaslik.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:" + vurgu + ";");

        Object[][] taslar = {
            {"♟ Piyon",  "1 adım aşağı hareket eder (yön: güney).",                "#a8d8a8"},
            {"♞ At",     "L şeklinde atlar (2+1), engelleri aşar.",                "#a8c8f8"},
            {"♝ Fil",    "Çapraz yönde istediği kadar kayar.",                     "#d8b8f8"},
            {"♜ Kale",   "Yatay veya dikey istediği kadar kayar.",                 "#f8d8a8"},
            {"♛ Vezir",  "Her yönde (çapraz + düz) istediği kadar kayar.",        "#f8a8a8"},
        };

        GridPane tasGrid = new GridPane();
        tasGrid.setHgap(10); tasGrid.setVgap(6);
        for (int i = 0; i < taslar.length; i++) {
            Label sym  = new Label((String)taslar[i][0]);
            sym.setStyle("-fx-font-size:14px; -fx-text-fill:" + taslar[i][2] + "; -fx-min-width:55; -fx-font-weight:bold;");
            Label desc = new Label((String)taslar[i][1]);
            desc.setStyle("-fx-font-size:12px; -fx-text-fill:" + yazi + ";");
            desc.setWrapText(true);
            tasGrid.add(sym, 0, i);
            tasGrid.add(desc, 1, i);
        }
        tasPane.getChildren().addAll(tasBaslik, tasGrid);

        // ── Zorluk seçimi ────────────────────────────────────────────────────
        VBox zorlukPane = new VBox(8);
        zorlukPane.setPadding(new Insets(12));
        zorlukPane.setStyle(
            "-fx-background-color:" + panel + ";" +
            "-fx-border-color:#1e3a5f; -fx-border-radius:10; -fx-background-radius:10;");

        Label zorlukBaslik = new Label("⚙  Zorluk Seç");
        zorlukBaslik.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:" + vurgu + ";");

        ToggleGroup zorlukGrup = new ToggleGroup();
        VBox zorluklar = new VBox(6);
        String[] zorlukAciklamalar = {
            "Piyon ve At — Yavaş hareket, başlangıç için ideal.",
            "Fil ve Kale — Kayarak hareket, orta güçlük.",
            "Sadece Vezir — Her yönde kayar, en yüksek tehlike!"
        };
        ChessBoardMode.ChessAyar[] presets = ChessBoardMode.PRESETLER;
        for (int i = 0; i < presets.length; i++) {
            ChessBoardMode.ChessAyar p = presets[i];
            RadioButton rb = new RadioButton(p.etiket() + "  (" + p.sureSaniye() + "s)");
            rb.setToggleGroup(zorlukGrup);
            rb.setUserData(p);
            rb.setStyle("-fx-text-fill:" + yazi + "; -fx-font-size:13px; -fx-font-weight:bold;");
            if (p.zorluk() == 1) rb.setSelected(true);
            Label acDesc = new Label("    " + zorlukAciklamalar[i]);
            acDesc.setStyle("-fx-font-size:11px; -fx-text-fill:#5a7090;");
            zorluklar.getChildren().addAll(rb, acDesc);
        }
        zorlukPane.getChildren().addAll(zorlukBaslik, zorluklar);

        // ── Oyna butonu ───────────────────────────────────────────────────────
        Button oynaBtn = new Button("▶  Oyunu Başlat!");
        oynaBtn.setPrefWidth(220); oynaBtn.setPrefHeight(46);
        oynaBtn.setStyle(
            "-fx-background-color: linear-gradient(to right, #c09000, #f0c040);" +
            "-fx-text-fill:#0a0918; -fx-font-size:15px; -fx-font-weight:bold;" +
            "-fx-background-radius:12; -fx-cursor:hand;" +
            "-fx-effect: dropshadow(gaussian,#f0c04088,12,0.4,0,2);");
        oynaBtn.setOnMouseEntered(e -> oynaBtn.setOpacity(0.88));
        oynaBtn.setOnMouseExited(e  -> oynaBtn.setOpacity(1.0));
        oynaBtn.setOnAction(e -> {
            ChessBoardMode.ChessAyar secilen = (ChessBoardMode.ChessAyar)
                    ((RadioButton) zorlukGrup.getSelectedToggle()).getUserData();
            klasikBoardMode  = null;
            leblebiBoardMode = null;
            satranModu       = true;
            chessBoardMode   = new ChessBoardMode(secilen.zorluk(), secilen.sureSaniye());
            satirSayisi = ChessBoardMode.BOARD_SIZE;
            sutunSayisi = ChessBoardMode.BOARD_SIZE;
            mayinSayisi = chessBoardMode.getMineCount();
            dialog.close();
            oyunSahnesiniBaSlat(false, true);
        });

        HBox butonKutu = new HBox(oynaBtn);
        butonKutu.setAlignment(Pos.CENTER);
        butonKutu.setPadding(new Insets(8, 0, 0, 0));

        VBox kok = new VBox(12,
            baslik, altBaslik, sep0,
            nasılPane, tasPane, zorlukPane,
            butonKutu);
        kok.setPadding(new Insets(22));
        kok.setAlignment(Pos.TOP_LEFT);
        kok.setStyle("-fx-background-color:" + arka + ";");
        kok.setMinWidth(440);

        ScrollPane scroll = new ScrollPane(kok);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color:" + arka + "; -fx-background:" + arka + ";");

        Scene dialogSahne = new Scene(scroll, 460, 640);
        globalCssUygula(dialogSahne);
        dialog.setScene(dialogSahne);
        dialog.showAndWait();
    }

    // =========================================================================
    // Leblebi game — start
    // =========================================================================

    private void leblebOyunuBaslat() {
        yilanHucreleri.clear();
        Seviye seviye = Seviye.getSeviye(mevcutSeviye);
        satirSayisi = seviye.getSatirSayisi();
        sutunSayisi = seviye.getSutunSayisi();
        mayinSayisi = seviye.getSolucanSayisi();
        klasikBoardMode = null; chessBoardMode = null;
        leblebiBoardMode = new LeblebiBoardMode(
                satirSayisi, sutunSayisi, mayinSayisi,
                seviye.getSureSaniye(), KLASIK_CAN,
                kaliciAltin, toplamKargaKullanim, toplamIlacKullanim);
        oyunSahnesiniBaSlat(true, false);
        leblebiBoardMode.diyalogTetikle(LeblebiBoardMode.DiyalogTetikleyici.LEVEL_BASI);
        javafx.application.Platform.runLater(() -> diyalogGoster(leblebiBoardMode.getAktifDiyalog()));
    }

    // =========================================================================
    // Game scene construction
    // =========================================================================

    /**
     * @param leblebi  true → leblebi modu (market + görev paneli)
     * @param satran   true → satranç modu (sade ama bilgi barı var)
     */
    private void oyunSahnesiniBaSlat(boolean leblebi, boolean satran) {
        kokDuzen = new BorderPane();
        kokDuzen.setPadding(new Insets(12));

        ustBariOlustur();
        izgarayiOlustur();

        if (leblebi) {
            marketPanelOlustur();
            gorevPanelOlustur();
            kokDuzen.setRight(marketPanel);
            kokDuzen.setLeft(gorevPanel);
            if (leblebModu) { marketPanelGuncelle(); gorevPanelGuncelle(); }
        }

        if (satran) {
            // Satranç modunda alt açıklama barı
            satranBilgiBariOlustur();
        }

        temayiUygula();
        zamanlayiciBaslat();
        arayuzuGuncelle();

        double tahminiHucreBoyu = 35;
        double merkezG = sutunSayisi * tahminiHucreBoyu;
        double merkezY = satirSayisi * tahminiHucreBoyu;
        double genislik  = leblebi ? Math.max(900, merkezG + 480) : Math.max(680, merkezG + 80);
        double yukseklik = leblebi ? Math.max(700, merkezY + 200) : Math.max(680, merkezY + 200);

        anaSahneKoku = new StackPane(kokDuzen);
        anaSahneKoku.setStyle("-fx-background-color: transparent;");

        sahne = rootScene;
        // Remove any previously registered resize listeners before adding new ones
        rootScene.widthProperty().removeListener(this::onSceneResize);
        rootScene.heightProperty().removeListener(this::onSceneResize);
        rootScene.widthProperty().addListener(this::onSceneResize);
        rootScene.heightProperty().addListener(this::onSceneResize);
        globalCssUygula(sahne);

        if (leblebi) {
            konusmaBalonuLabel = new Label();
            konusmaBalonuLabel.setWrapText(true);
            konusmaBalonuLabel.setMaxWidth(280);
            konusmaBalonuLabel.setStyle("-fx-background-color: transparent;" +
                    "-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #3d2800;");

            Label emmiIkonu = new Label("👴 Mehmet Emmi:");
            emmiIkonu.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #7a5200; -fx-padding: 0 0 4 0;");

            VBox balonKutusu = new VBox(0, emmiIkonu, konusmaBalonuLabel);
            balonKutusu.setStyle(
                    "-fx-background-color: linear-gradient(to bottom right, #fffdf0, #ffeaa7);" +
                    "-fx-border-color: #c89a2a; -fx-border-width: 2.5; -fx-border-radius: 14;" +
                    "-fx-background-radius: 14; -fx-padding: 12 16 12 16;" +
                    "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.45),12,0,0,6);");
            balonKutusu.setMaxWidth(300);

            konusmaBalonuPanel = new StackPane(balonKutusu);
            konusmaBalonuPanel.setVisible(false);
            konusmaBalonuPanel.setOpacity(0);
            konusmaBalonuPanel.setMouseTransparent(true);
            konusmaBalonuPanel.setMaxSize(300, 120);
            StackPane.setAlignment(konusmaBalonuPanel, Pos.BOTTOM_LEFT);
            StackPane.setMargin(konusmaBalonuPanel, new Insets(0, 0, 18, 14));
            anaSahneKoku.getChildren().add(konusmaBalonuPanel);
        } else {
            konusmaBalonuPanel = null;
            konusmaBalonuLabel = null;
        }
        globalCssUygula(rootScene);
        rootScene.setOnKeyPressed(null);
        rootWrapper.getChildren().setAll(anaSahneKoku);
        hucreBoyutlariniGuncelle();
    }

    // ── Satranç bilgi barı (alt panel) ───────────────────────────────────────

    private void satranBilgiBariOlustur() {
        HBox bar = new HBox(20);
        bar.setAlignment(Pos.CENTER);
        bar.setPadding(new Insets(10, 8, 6, 8));
        bar.setStyle("-fx-background-color: " + CH_UST_BAR + "; -fx-background-radius: 8;");

        String[] bilgiler = {
            "Sol tık → Aç",
            "Sağ tık → Bayrak",
            "Her hamlede taşlar hareket eder!",
            "♟Piyon  ♞At  ♝Fil  ♜Kale  ♛Vezir"
        };
        for (String b : bilgiler) {
            Label l = new Label(b);
            l.setStyle("-fx-font-size: 12px; -fx-text-fill: #95a5a6;");
            bar.getChildren().add(l);
            if (!b.equals(bilgiler[bilgiler.length - 1])) {
                Label sep = new Label("|");
                sep.setStyle("-fx-text-fill: #2c3e50;");
                bar.getChildren().add(sep);
            }
        }

        kokDuzen.setBottom(bar);
    }

    // ── HUD bar ───────────────────────────────────────────────────────────────

    private void ustBariOlustur() {
        String hudLabelStil = "-fx-font-size: 19px; -fx-font-weight: bold; -fx-padding: 4 12 4 12;-fx-background-radius: 8;";
        String hudPuanStil  = "-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #f0c040;-fx-padding: 4 12 4 12; -fx-background-radius: 8;";

        maynSayaciEtiketi = new Label(); maynSayaciEtiketi.setStyle(hudLabelStil);
        zamanlayiciEtiketi= new Label(); zamanlayiciEtiketi.setStyle(hudLabelStil);
        canEtiketi        = new Label(""); canEtiketi.setStyle(hudLabelStil + "-fx-text-fill: #ff6b6b;");
        puanEtiketi       = new Label(""); puanEtiketi.setStyle(hudPuanStil);
        altinEtiketi      = new Label(""); altinEtiketi.setStyle(hudPuanStil);
        durumEtiketi      = new Label(""); durumEtiketi.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");

        sifirlaBtn = new Button(leblebModu ? "🌾" : satranModu ? "♟" : "😊");
        sifirlaBtn.setStyle(butonTarzi() + "-fx-font-size: 18px; -fx-padding: 6 14 6 14;");
        sifirlaBtn.setOnAction(o -> { sesCal(sesButon); oyunuSifirla(); });

        Button menuBtn = new Button("← Menü");
        menuBtn.setStyle(butonTarzi());
        menuBtn.setOnAction(o -> {
            sesCal(sesButon);
            if (zamanlayici != null) zamanlayici.stop();
            menuGoster();
        });

        Button temaBtn = new Button(karanlikTema ? "☀" : "★");
        temaBtn.setStyle(butonTarzi());
        temaBtn.setVisible(!leblebModu && !satranModu);
        temaBtn.setManaged(!leblebModu && !satranModu);
        temaBtn.setOnAction(o -> {
            karanlikTema = !karanlikTema;
            temaBtn.setText(karanlikTema ? "☀" : "★");
            temayiUygula(); tumHucreleriYenidenCiz(); hucreBoyutlariniGuncelle();
        });

        HBox ustBar = new HBox(8, menuBtn, maynSayaciEtiketi, canEtiketi, sifirlaBtn,
                zamanlayiciEtiketi, puanEtiketi, altinEtiketi, durumEtiketi, temaBtn);
        ustBar.setAlignment(Pos.CENTER_LEFT);
        ustBar.setPadding(new Insets(6, 8, 10, 8));
        kokDuzen.setTop(ustBar);

        guncelleUstBar();
    }

    private void guncelleUstBar() {
        if (satranModu && chessBoardMode != null) {
            maynSayaciEtiketi.setText("♟ " + chessBoardMode.getMineCount());
            zamanlayiciEtiketi.setText("⏳ " + chessBoardMode.getKalanSure() + "s");
            canEtiketi.setText(""); canEtiketi.setVisible(false); canEtiketi.setManaged(false);
            puanEtiketi.setText(""); altinEtiketi.setText("");
            return;
        }

        String mayinSimge = leblebModu ? "🐍 " : "💣 ";
        maynSayaciEtiketi.setText(mayinSimge + (mayinSayisi - yerlestirilenIsaret));

        if (leblebModu && leblebiBoardMode != null) {
            zamanlayiciEtiketi.setText("⏳ " + leblebiBoardMode.getKalanSure() + "s");
            canEtiketi.setText(""); canEtiketi.setVisible(false); canEtiketi.setManaged(false);
            puanEtiketi.setText("🫘 " + leblebiBoardMode.getLeblebPuani() + " puan");
        } else if (klasikBoardMode != null && klasikBoardMode.isGeriSayim()) {
            zamanlayiciEtiketi.setText("⏳ " + klasikBoardMode.getSuruclukSure() + "s");
            zamanlayiciEtiketi.setStyle("-fx-font-size: 19px; -fx-font-weight: bold; -fx-padding: 4 12 4 12;-fx-background-radius: 8; -fx-text-fill: #cdd6f4;");
            canEtiketi.setText(""); canEtiketi.setVisible(true); canEtiketi.setManaged(true);
            puanEtiketi.setText(""); altinEtiketi.setText("");
        } else {
            zamanlayiciEtiketi.setText("⏱ 0s");
            canEtiketi.setText(""); canEtiketi.setVisible(true); canEtiketi.setManaged(true);
            puanEtiketi.setText(""); altinEtiketi.setText("");
        }
    }

    private String butonTarzi() {
        return "-fx-font-size: 13px; -fx-padding: 4 10 4 10;" +
               "-fx-cursor: hand; -fx-border-radius: 6; -fx-background-radius: 6;";
    }

    // ── Grid ──────────────────────────────────────────────────────────────────

    private void izgarayiOlustur() {
        izgaraDuzen = new GridPane();
        izgaraDuzen.setHgap(3); izgaraDuzen.setVgap(3);
        izgaraDuzen.setAlignment(Pos.CENTER);

        dugmeler   = new Button[satirSayisi][sutunSayisi];
        hucreDurum = new byte[satirSayisi][sutunSayisi];
        for (byte[] row : hucreDurum) java.util.Arrays.fill(row, (byte) -1);
        yerlestirilenIsaret = 0;

        for (int s = 0; s < satirSayisi; s++) {
            for (int u = 0; u < sutunSayisi; u++) {
                Button btn = new Button();
                btn.setPrefSize(48, 48); btn.setMinSize(28, 28);

                int sr = s, su = u;
                btn.setOnMouseClicked(olay -> {
                    if (oyunAktifDegil()) return;
                    if (olay.getButton() == MouseButton.PRIMARY)        hucreAc(sr, su);
                    else if (olay.getButton() == MouseButton.SECONDARY) isaretKoy(sr, su);
                    arayuzuGuncelle();
                    hucreBoyutlariniGuncelle();
                    if (marketPanel != null) marketPanelGuncelle();
                });

                btn.setOnMouseEntered(olay -> {
                    if (!btn.isDisabled() && !oyunAktifDegil()) {
                        if (leblebModu && leblebiBoardMode != null && leblebiBoardMode.isZirayiIlacAktif()) {
                            ilacHoverUygula(sr, su);
                        } else {
                            boolean kapali = satranModu
                                    ? (!chessBoardMode.isRevealed(sr, su) && !chessBoardMode.isFlagged(sr, su))
                                    : (!aktifTahtaHucre(sr, su).isAcildiMi() && !aktifTahtaHucre(sr, su).isIsaretlendi());
                            if (kapali) btn.setStyle(acilmamisHucreHoverTarzi(sr, su));
                        }
                    }
                });
                btn.setOnMouseExited(olay -> {
                    if (!btn.isDisabled()) {
                        if (leblebModu && leblebiBoardMode != null && leblebiBoardMode.isZirayiIlacAktif()) {
                            ilacHoverTemizle();
                        } else {
                            boolean kapali = satranModu
                                    ? (!chessBoardMode.isRevealed(sr, su) && !chessBoardMode.isFlagged(sr, su))
                                    : (!aktifTahtaHucre(sr, su).isAcildiMi() && !aktifTahtaHucre(sr, su).isIsaretlendi());
                            if (kapali) btn.setStyle(acilmamisHucreTarzi(sr, su));
                        }
                    }
                });

                dugmeler[s][u] = btn;
                izgaraDuzen.add(btn, u, s);
            }
        }

        if (canIkonKutusu == null) {
            canIkonKutusu = new HBox(6);
            canIkonKutusu.setAlignment(Pos.CENTER);
            canIkonKutusu.setPickOnBounds(false);
        }

        merkezIcerikKutusu = new StackPane(izgaraDuzen);
        merkezIcerikKutusu.setAlignment(Pos.CENTER);

        StackPane merkez = new StackPane(merkezIcerikKutusu);
        merkez.setAlignment(Pos.CENTER);
        VBox.setVgrow(merkez, Priority.ALWAYS);
        kokDuzen.setCenter(merkez);

        boolean geceModu = leblebModu && (mevcutSeviye % 4 == 3);
        if (geceModu) {
            geceKanvasi = new javafx.scene.canvas.Canvas();
            geceKanvasi.setMouseTransparent(true);
            geceKanvasi.widthProperty().bind(merkez.widthProperty());
            geceKanvasi.heightProperty().bind(merkez.heightProperty());
            merkez.getChildren().add(geceKanvasi);

            merkez.setOnMouseMoved(e -> { fenerX = e.getX(); fenerY = e.getY(); geceKanvasiniCiz(); });
            merkez.setOnMouseExited(e -> { fenerX = -1000; fenerY = -1000; geceKanvasiniCiz(); });

            nefesAnimasyonu = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(this.fenerPulseOffsetProperty(), 0)),
                    new KeyFrame(Duration.seconds(2), new KeyValue(this.fenerPulseOffsetProperty(), 30)));
            nefesAnimasyonu.setAutoReverse(true);
            nefesAnimasyonu.setCycleCount(Animation.INDEFINITE);
            nefesAnimasyonu.play();

            geceKanvasi.widthProperty().addListener(o -> geceKanvasiniCiz());
            geceKanvasi.heightProperty().addListener(o -> geceKanvasiniCiz());
        }

        if (leblebModu && leblebiBoardMode != null) canSayisiGuncelle(leblebiBoardMode.getCanSayisi());
    }

    // Yardımcı: satranç modunda cell erişimi yok, dummy döner
    private Cell aktifTahtaHucre(int s, int u) {
        Board t = aktifTahta();
        return t != null ? t.getHucre(s, u) : new Cell();
    }

    private javafx.beans.property.DoubleProperty fenerPulseOffsetProp;
    private javafx.beans.property.DoubleProperty fenerPulseOffsetProperty() {
        if (fenerPulseOffsetProp == null) {
            fenerPulseOffsetProp = new javafx.beans.property.SimpleDoubleProperty(this, "fenerPulseOffset", fenerPulseOffset) {
                @Override protected void invalidated() { fenerPulseOffset = get(); geceKanvasiniCiz(); }
            };
        }
        return fenerPulseOffsetProp;
    }

    private void geceKanvasiniCiz() {
        if (geceKanvasi == null) return;
        double w = geceKanvasi.getWidth(), h = geceKanvasi.getHeight();
        if (w <= 0 || h <= 0) return;
        javafx.scene.canvas.GraphicsContext gc = geceKanvasi.getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);
        boolean mouseOnBoard = fenerX >= 0 && fenerY >= 0 && (leblebiBoardMode != null && !leblebiBoardMode.isOyunBitti());
        double ambientAlpha = mouseOnBoard ? 0.82 : 0.50;
        gc.setFill(javafx.scene.paint.Color.rgb(10, 10, 15, ambientAlpha));
        gc.fillRect(0, 0, w, h);
        if (mouseOnBoard) {
            double r = fenerBaseYaricap + fenerPulseOffset;
            javafx.scene.paint.RadialGradient rg = new javafx.scene.paint.RadialGradient(
                    0, 0, fenerX, fenerY, r, false, javafx.scene.paint.CycleMethod.NO_CYCLE,
                    new javafx.scene.paint.Stop(0.00, javafx.scene.paint.Color.TRANSPARENT),
                    new javafx.scene.paint.Stop(0.70, javafx.scene.paint.Color.rgb(10, 10, 15, 0.20)),
                    new javafx.scene.paint.Stop(1.00, javafx.scene.paint.Color.rgb(10, 10, 15, 0.82)));
            gc.setGlobalBlendMode(javafx.scene.effect.BlendMode.SRC_ATOP);
            gc.setFill(rg); gc.fillOval(fenerX - r, fenerY - r, r * 2, r * 2);
            gc.setGlobalBlendMode(javafx.scene.effect.BlendMode.SRC_OVER);
        }
    }

    // ── Market panel ──────────────────────────────────────────────────────────

    private void marketPanelOlustur() {
        marketPanel = new VBox(15);
        marketPanel.setPadding(new Insets(15));
        marketPanel.setStyle("-fx-background-color: rgba(61,40,0,0.6); -fx-background-radius: 12;");
        marketPanel.setPrefWidth(240);

        Label baslik = new Label("🚜 Mehmet Emmi'nin Dükkanı");
        baslik.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #f0c040;");
        Label seviyeEtiketi = new Label("Bölüm: " + mevcutSeviye);
        seviyeEtiketi.setStyle("-fx-font-size: 14px; -fx-text-fill: #d4b070;");
        Separator sep = new Separator(); sep.setStyle("-fx-background-color: #c89a2a; -fx-opacity: 0.5;");

        Button kargaBtn = marketKartButonOlustur("🐦", "Karga\n(" + (leblebiBoardMode != null ? leblebiBoardMode.getKargaLevel() : 1) + ". Seviye)", "20 💰");
        Button saatBtn  = marketKartButonOlustur("⏰", "Emmi'nin Saati", "30 💰");
        Button ilacBtn  = marketKartButonOlustur("🧪", "Zirai İlaç\n(" + (leblebiBoardMode != null ? leblebiBoardMode.getIlacLevel() : 1) + ". Seviye)", "50 💰");
        Button kalpBtn  = marketKartButonOlustur("❤️", "Ekstra Kalp", "100 💰");

        kargaBtn.setId("kargaBtn"); saatBtn.setId("saatBtn"); ilacBtn.setId("ilacBtn"); kalpBtn.setId("kalpBtn");

        kargaBtn.setOnAction(o -> {
            if (leblebiBoardMode.kargaKullan() != null) {
                sesCal(sesMarket); marketPanelGuncelle();
                puanEtiketi.setText("🏆 " + leblebiBoardMode.getLeblebPuani() + " puan");
                altinEtiketi.setText("💰 " + leblebiBoardMode.getAltin() + " altın");
                arayuzuGuncelle();
                leblebiBoardMode.diyalogTetikle(LeblebiBoardMode.DiyalogTetikleyici.ITEM_KULLANIMI);
                diyalogGoster(leblebiBoardMode.getAktifDiyalog());
            } else { diyalogGoster("Burada yılan yok evlat, paranı cebinde tut."); }
        });
        saatBtn.setOnAction(o -> {
            if (leblebiBoardMode.emmininSaatiniKullan()) {
                sesCal(sesMarket); marketPanelGuncelle();
                zamanlayiciEtiketi.setText("⏳ " + leblebiBoardMode.getKalanSure() + "s");
                puanEtiketi.setText("🏆 " + leblebiBoardMode.getLeblebPuani() + " puan");
                altinEtiketi.setText("💰 " + leblebiBoardMode.getAltin() + " altın");
                leblebiBoardMode.diyalogTetikle(LeblebiBoardMode.DiyalogTetikleyici.ITEM_KULLANIMI);
                diyalogGoster(leblebiBoardMode.getAktifDiyalog());
            } else { diyalogGoster("Yeterli paran yok! Saat 30 altın."); }
        });
        ilacBtn.setOnAction(o -> {
            if (leblebiBoardMode.zirayiIlacAktiflesir()) {
                sesCal(sesMarket); marketPanelGuncelle();
                puanEtiketi.setText("🏆 " + leblebiBoardMode.getLeblebPuani() + " puan");
                altinEtiketi.setText("💰 " + leblebiBoardMode.getAltin() + " altın");
                ilacBtn.setStyle(ilacBtn.getStyle() + "-fx-border-color: #e74c3c; -fx-border-width: 2;");
                leblebiBoardMode.diyalogTetikle(LeblebiBoardMode.DiyalogTetikleyici.ITEM_KULLANIMI);
                diyalogGoster(leblebiBoardMode.getAktifDiyalog());
            } else { diyalogGoster("İlaç pahalı evlat, 50 altın lazım."); }
        });
        kalpBtn.setOnAction(o -> {
            if (leblebiBoardMode.ekstraKalpAl()) {
                sesCal(sesMarket); canSayisiGuncelle(leblebiBoardMode.getCanSayisi()); marketPanelGuncelle();
                puanEtiketi.setText("🏆 " + leblebiBoardMode.getLeblebPuani() + " puan");
                altinEtiketi.setText("💰 " + leblebiBoardMode.getAltin() + " altın");
                leblebiBoardMode.diyalogTetikle(LeblebiBoardMode.DiyalogTetikleyici.ITEM_KULLANIMI);
                diyalogGoster(leblebiBoardMode.getAktifDiyalog());
            } else { diyalogGoster("Can almak o kadar ucuz değil, 100 altın ister."); }
        });

        boolean geceModu = (mevcutSeviye % 4 == 3);
        Button fenerBtn = null;
        if (geceModu) {
            fenerBtn = marketKartButonOlustur("🎕", "Fener Genışlet", "15 💰");
            fenerBtn.setId("fenerBtn");
            fenerBtn.setOnAction(o -> {
                if (leblebiBoardMode != null && leblebiBoardMode.altinHarca(15)) {
                    fenerBaseYaricap = Math.min(350, fenerBaseYaricap + 40);
                    geceKanvasiniCiz();
                    altinEtiketi.setText("💰 " + leblebiBoardMode.getAltin() + " altın");
                    marketPanelGuncelle();
                    diyalogGoster("Fırlıyalım feneri, tarlayı aydınlatalım!");
                } else { diyalogGoster("Fener için 15 altın gerekli evladım."); }
            });
        }

        GridPane kartlar = new GridPane();
        kartlar.setHgap(8); kartlar.setVgap(8);
        kartlar.add(kargaBtn, 0, 0); kartlar.add(saatBtn, 1, 0);
        kartlar.add(ilacBtn,  0, 1); kartlar.add(kalpBtn, 1, 1);
        if (fenerBtn != null) { kartlar.add(fenerBtn, 0, 2); GridPane.setColumnSpan(fenerBtn, 2); }
        GridPane.setHgrow(kargaBtn, Priority.ALWAYS); GridPane.setHgrow(saatBtn, Priority.ALWAYS);
        GridPane.setHgrow(ilacBtn,  Priority.ALWAYS); GridPane.setHgrow(kalpBtn, Priority.ALWAYS);

        marketPanel.getChildren().addAll(baslik, seviyeEtiketi, sep, kartlar);
    }

    private static final String MKT_BTN_NORMAL =
            "-fx-background-color: #6b3e00; -fx-text-fill: #f5e6b0;" +
            "-fx-background-radius: 10; -fx-border-radius: 10;" +
            "-fx-border-color: #a07020; -fx-border-width: 1.5;" +
            "-fx-cursor: hand; -fx-alignment: center;";
    private static final String MKT_BTN_HOVER  =
            "-fx-background-color: #8a5500; -fx-text-fill: #fff8e0;" +
            "-fx-background-radius: 10; -fx-border-radius: 10;" +
            "-fx-border-color: #f0c040; -fx-border-width: 2;" +
            "-fx-cursor: hand; -fx-alignment: center;" +
            "-fx-effect: dropshadow(gaussian,#f0c040,8,0.4,0,0);";

    private Button marketKartButonOlustur(String ikon, String isim, String fiyat) {
        Label ikonLabel  = new Label(ikon);  ikonLabel.setStyle("-fx-font-size: 28px;");
        Label isimLabel  = new Label(isim);  isimLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #d4b070; -fx-font-weight: bold;"); isimLabel.setWrapText(true); isimLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        Label fiyatLabel = new Label(fiyat); fiyatLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #f0c040; -fx-font-weight: bold;");

        VBox kutu = new VBox(2, ikonLabel, isimLabel, fiyatLabel);
        kutu.setAlignment(Pos.CENTER); kutu.setMaxWidth(Double.MAX_VALUE);

        Button btn = new Button();
        btn.setGraphic(kutu); btn.setMaxWidth(Double.MAX_VALUE); btn.setPrefHeight(90);
        btn.setStyle(MKT_BTN_NORMAL);

        ScaleTransition stIn  = new ScaleTransition(Duration.millis(150), btn); stIn.setToX(1.05);  stIn.setToY(1.05);
        ScaleTransition stOut = new ScaleTransition(Duration.millis(150), btn); stOut.setToX(1.0); stOut.setToY(1.0);

        btn.setOnMouseEntered(e -> { if (!btn.isDisabled()) { btn.setStyle(MKT_BTN_HOVER); stOut.stop(); stIn.play(); }});
        btn.setOnMouseExited(e  -> { btn.setStyle(MKT_BTN_NORMAL); stIn.stop(); stOut.play(); });
        return btn;
    }

    private void marketPanelGuncelle() {
        if (marketPanel == null || leblebiBoardMode == null) return;
        int altin = leblebiBoardMode.getAltin();
        java.util.function.Consumer<javafx.scene.Node> guncelleBtn = dugum -> {
            if (dugum instanceof Button btn) {
                String id = btn.getId(); if (id == null) return;
                boolean aktif = switch (id) {
                    case "kargaBtn" -> altin >= 20; case "saatBtn" -> altin >= 30;
                    case "ilacBtn"  -> altin >= 50; case "kalpBtn" -> altin >= 100;
                    default -> true;
                };
                btn.setDisable(!aktif); btn.setOpacity(aktif ? 1.0 : 0.5);
                if (aktif) btn.setStyle(MKT_BTN_NORMAL);
            }
        };
        marketPanel.getChildren().forEach(dugum -> {
            guncelleBtn.accept(dugum);
            if (dugum instanceof GridPane gp) gp.getChildren().forEach(guncelleBtn::accept);
        });
    }

    // ── Görev paneli ──────────────────────────────────────────────────────────

    private void gorevPanelOlustur() {
        gorevPanel = new VBox(15);
        gorevPanel.setPadding(new Insets(15));
        gorevPanel.setStyle("-fx-background-color: #2a1c02; -fx-border-color: #6b3e00; -fx-border-width: 0 2 0 0; -fx-min-width: 220px;");
        Label baslik = new Label("📋 Emmi'nin İstekleri");
        baslik.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #f5e6b0;");
        gorevPanel.getChildren().add(baslik);
    }

    private void gorevPanelGuncelle() {
        if (gorevPanel == null || leblebiBoardMode == null) return;
        gorevPanel.getChildren().clear();
        Label baslik = new Label("📋 Emmi'nin İstekleri");
        baslik.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #f5e6b0;");
        gorevPanel.getChildren().add(baslik);
        for (LeblebiBoardMode.Gorev g : leblebiBoardMode.getAktifGorevler()) {
            VBox kart = new VBox(5); kart.setPadding(new Insets(10));
            String bg = g.tamamlandi ? "-fx-background-color: #3b5025;" : "-fx-background-color: #3d2800;";
            String border = g.tamamlandi ? "-fx-border-color: #6dbe45;" : "-fx-border-color: #6b3e00;";
            kart.setStyle(bg + border + " -fx-border-radius: 8; -fx-background-radius: 8;");
            Label desc = new Label(g.aciklama); desc.setWrapText(true);
            desc.setStyle("-fx-font-size: 14px; -fx-text-fill: " + (g.tamamlandi ? "#a6e3a1" : "#e8c55a") + "; -fx-font-weight: bold;");
            Label odul = new Label("Ödül: " + g.puanOdulu + " Puan, " + g.altinOdulu + " Altın");
            odul.setStyle("-fx-font-size: 12px; -fx-text-fill: #a0a0a0;");
            if (g.tamamlandi) {
                Label tik = new Label("✔ Tamamlandı"); tik.setStyle("-fx-font-size: 12px; -fx-text-fill: #6dbe45; -fx-font-weight: bold;");
                kart.getChildren().addAll(desc, tik);
            } else { kart.getChildren().addAll(desc, odul); }
            gorevPanel.getChildren().add(kart);
        }
    }

    // =========================================================================
    // Input handling
    // =========================================================================

    private boolean oyunAktifDegil() {
        if (satranModu && chessBoardMode != null)
            return !chessBoardMode.isOyunAktif();
        if (leblebModu && leblebiBoardMode != null)
            return leblebiBoardMode.isOyunBitti() || leblebiBoardMode.isKazanildi();
        return klasikBoardMode == null || !klasikBoardMode.isOyunAktif();
    }

    private void hucreAc(int s, int u) {
        // ── Satranç Modu ──────────────────────────────────────────────────────
        if (satranModu && chessBoardMode != null) {
            boolean mineHit = chessBoardMode.hucreAc(s, u);
            if (mineHit) {
                sesCal(sesPatlama);
                ekranSarsintisi();
                if (merkezIcerikKutusu != null) kirmiziFlasBas(merkezIcerikKutusu);
            } else {
                sesCal(sesKazma);
            }
            // Mayın sayısını güncelle
            maynSayaciEtiketi.setText("♟ " + chessBoardMode.getMineCount());
            return;
        }

        // ── Leblebi Modu ──────────────────────────────────────────────────────
        if (leblebModu && leblebiBoardMode != null) {
            if (leblebiBoardMode.isZirayiIlacAktif()) {
                Board tahta = leblebiBoardMode.getTahta();
                int oncekiAcik = acikHucreSay(tahta);
                leblebiBoardMode.hucreAc(s, u);
                int kazanilanPuan = acikHucreSay(tahta) - oncekiAcik;
                if (kazanilanPuan > 0) {
                    leblebiBoardMode.hucrePuaniEkle(kazanilanPuan);
                    puanEtiketi.setText("🏆 " + leblebiBoardMode.getLeblebPuani() + " puan");
                    altinEtiketi.setText("💰 " + leblebiBoardMode.getAltin() + " altın");
                }
                leblebiBoardMode.kazanmaKontrol();
                sesCal(sesKazma);
                return;
            }
            Board tahta = leblebiBoardMode.getTahta();
            int oncekiAcik = acikHucreSay(tahta);
            boolean mineHit = leblebiBoardMode.hucreAc(s, u);
            if (mineHit) {
                sesCal(sesPatlama);
                canSayisiGuncelle(leblebiBoardMode.getCanSayisi());
                ekranSarsintisi();
                if (merkezIcerikKutusu != null) kirmiziFlasBas(merkezIcerikKutusu);
                canIkonunuKir(leblebiBoardMode.getCanSayisi());
                leblebiBoardMode.diyalogTetikle(LeblebiBoardMode.DiyalogTetikleyici.CAN_KAYBI);
                diyalogGoster(leblebiBoardMode.getAktifDiyalog());
                if (!leblebiBoardMode.isOyunBitti()) { zamanlayici.pause(); yilanUyarisiGoster(s, u); }
            } else {
                sesCal(sesKazma);
                int kazanilan = acikHucreSay(tahta) - oncekiAcik;
                if (kazanilan > 0) {
                    leblebiBoardMode.hucrePuaniEkle(kazanilan);
                    puanEtiketi.setText("🏆 " + leblebiBoardMode.getLeblebPuani() + " puan");
                    altinEtiketi.setText("💰 " + leblebiBoardMode.getAltin() + " altın");
                }
            }
            return;
        }

        // ── Klasik Mod ────────────────────────────────────────────────────────
        if (klasikBoardMode != null) {
            Board tahta = klasikBoardMode.getTahta();
            boolean solucanMiydi = tahta.getHucre(s, u).isMayinMi();
            klasikBoardMode.hucreAc(s, u);
            sesCal(solucanMiydi ? sesPatlama : sesKazma);
        }
    }

    private void isaretKoy(int s, int u) {
        if (satranModu && chessBoardMode != null) {
            chessBoardMode.isaretKoy(s, u);
            return;
        }
        Board tahta = aktifTahta();
        if (tahta == null) return;
        Cell hucre = tahta.getHucre(s, u);
        if (hucre.isAcildiMi()) return;
        boolean isaretliydi = hucre.isIsaretlendi();
        if (!isaretliydi && yerlestirilenIsaret >= mayinSayisi) return;
        hucre.isaretiDegistir();
        yerlestirilenIsaret += isaretliydi ? -1 : 1;
        yerlestirilenIsaret = Math.max(0, yerlestirilenIsaret);
        String simge = leblebModu ? "🐍 " : "💣 ";
        maynSayaciEtiketi.setText(simge + (mayinSayisi - yerlestirilenIsaret));
    }

    private Board aktifTahta() {
        if (leblebModu && leblebiBoardMode != null) return leblebiBoardMode.getTahta();
        if (klasikBoardMode != null) return klasikBoardMode.getTahta();
        return null;
    }

    private int acikHucreSay(Board tahta) {
        int sayi = 0;
        for (int s = 0; s < satirSayisi; s++)
            for (int u = 0; u < sutunSayisi; u++) {
                Cell h = tahta.getHucre(s, u);
                if (h.isAcildiMi() && !h.isMayinMi()) sayi++;
            }
        return sayi;
    }

    // =========================================================================
    // Timer
    // =========================================================================

    private void zamanlayiciBaslat() {
        if (zamanlayici != null) zamanlayici.stop();
        zamanlayici = new Timeline(new KeyFrame(Duration.seconds(1), olay -> {

            // ── Satranç Modu tick ─────────────────────────────────────────────
            if (satranModu && chessBoardMode != null) {
                boolean timesUp = chessBoardMode.sureyiGuncelle(1);
                int kalan = chessBoardMode.getKalanSure();
                zamanlayiciEtiketi.setText("⏳ " + kalan + "s");
                boolean flash = (kalan % 2 == 0);
                zamanlayiciEtiketi.setStyle(
                        "-fx-font-size: 19px; -fx-font-weight: bold; -fx-padding: 4 12 4 12;" +
                        "-fx-background-radius: 8; -fx-text-fill: " +
                        (kalan <= 10 ? (flash ? "#e74c3c" : "#ffffff") : "#f0c040") + ";");
                if (timesUp) {
                    zamanlayici.stop(); sifirlaBtn.setText("💀");
                    durumEtiketi.setText("⏰ Süre Doldu! Taşlar tarlayı işgal etti!");
                    durumEtiketi.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #e74c3c;");
                    sesCal(sesPatlama); arayuzuGuncelle();
                }
                arayuzuGuncelle(); // taşlar hareket edebilir, refresh şart
                return;
            }

            // ── Leblebi tick ──────────────────────────────────────────────────
            if (leblebModu && leblebiBoardMode != null) {
                leblebiBoardMode.sureyiGuncelle(1);
                int kalan = leblebiBoardMode.getKalanSure();
                zamanlayiciEtiketi.setText("⏳ " + kalan + "s");
                boolean flash = (kalan % 2 == 0);
                zamanlayiciEtiketi.setStyle("-fx-font-size: 19px; -fx-font-weight: bold; -fx-padding: 4 12 4 12;" +
                        "-fx-background-radius: 8; -fx-text-fill: " +
                        (kalan <= 10 ? (flash ? "#e74c3c" : "#ffffff") : "#cdd6f4") + ";");
                if (leblebiBoardMode.isOyunBitti()) {
                    zamanlayici.stop(); sifirlaBtn.setText("😵");
                    leblebiBoardMode.diyalogTetikle(LeblebiBoardMode.DiyalogTetikleyici.KAYBETME);
                    diyalogGoster(leblebiBoardMode.getAktifDiyalog()); arayuzuGuncelle();
                }
                return;
            }

            // ── Klasik tick ───────────────────────────────────────────────────
            if (klasikBoardMode != null) {
                if (klasikBoardMode.isGeriSayim()) {
                    boolean timesUp = klasikBoardMode.sureyiGuncelle(1);
                    int kalan = klasikBoardMode.getSuruclukSure();
                    zamanlayiciEtiketi.setText("⏳ " + kalan + "s");
                    boolean flash = (kalan % 2 == 0);
                    zamanlayiciEtiketi.setStyle("-fx-font-size: 19px; -fx-font-weight: bold; -fx-padding: 4 12 4 12;" +
                            "-fx-background-radius: 8; -fx-text-fill: " +
                            (kalan <= 10 ? (flash ? "#e74c3c" : "#ffffff") : "#cdd6f4") + ";");
                    if (timesUp) {
                        zamanlayici.stop();
                        durumEtiketi.setText("⏰ Süre Doldu! Tüm Mayınlar Patladı!");
                        durumEtiketi.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;-fx-text-fill: #f38ba8;");
                        sifirlaBtn.setText("😵"); sesCal(sesPatlama); arayuzuGuncelle();
                        if (dugmeler != null) for (Button[] satir : dugmeler) for (Button btn : satir) btn.setDisable(true);
                    }
                } else {
                    klasikBoardMode.sureyiGuncelle(1);
                    zamanlayiciEtiketi.setText("⏱ " + klasikBoardMode.getSuruclukSure() + "s");
                }
            }
        }));
        zamanlayici.setCycleCount(Animation.INDEFINITE);
        zamanlayici.play();
    }

    // =========================================================================
    // Reset
    // =========================================================================

    private void oyunuSifirla() {
        if (zamanlayici != null) zamanlayici.stop();
        if (aktifBalonAnimasyonu != null) { aktifBalonAnimasyonu.stop(); aktifBalonAnimasyonu = null; }
        if (konusmaBalonuPanel != null) konusmaBalonuPanel.setVisible(false);
        yerlestirilenIsaret = 0; yilanHucreleri.clear(); durumEtiketi.setText("");

        String hudLabelStil = "-fx-font-size: 19px; -fx-font-weight: bold; -fx-padding: 4 12 4 12;-fx-background-radius: 8;";
        String hudPuanStil  = "-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #f0c040;-fx-padding: 4 12 4 12; -fx-background-radius: 8;";
        zamanlayiciEtiketi.setStyle(hudLabelStil); puanEtiketi.setStyle(hudPuanStil);
        if (altinEtiketi != null) altinEtiketi.setStyle(hudPuanStil);
        canEtiketi.setStyle(hudLabelStil + "-fx-text-fill: #ff6b6b;");
        durumEtiketi.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");
        sifirlaBtn.setText(leblebModu ? "🌾" : satranModu ? "♟" : "😊");

        if (satranModu && chessBoardMode != null) {
            chessBoardMode = new ChessBoardMode(chessBoardMode.getDifficulty(), chessBoardMode.getBaslangicSuresi());
            mayinSayisi = chessBoardMode.getMineCount();
        } else if (leblebModu) {
            leblebiBoardMode = new LeblebiBoardMode(satirSayisi, sutunSayisi, mayinSayisi,
                    Seviye.getSeviye(mevcutSeviye).getSureSaniye(), KLASIK_CAN,
                    kaliciAltin, toplamKargaKullanim, toplamIlacKullanim);
        } else {
            klasikBoardMode = new KlasikBoardMode(satirSayisi, sutunSayisi, mayinSayisi,
                    klasikBoardMode.isGeriSayim(), klasikBoardMode.getBaslangicSuresi());
        }

        guncelleUstBar(); izgarayiOlustur();

        if (leblebModu) {
            marketPanelOlustur(); kokDuzen.setRight(marketPanel);
            marketPanelGuncelle(); canSayisiGuncelle(leblebiBoardMode.getCanSayisi());
        } else if (satranModu) {
            satranBilgiBariOlustur();
            kokDuzen.setRight(null); kokDuzen.setLeft(null);
        } else {
            kokDuzen.setRight(null);
        }

        temayiUygula(); zamanlayiciBaslat(); arayuzuGuncelle(); hucreBoyutlariniGuncelle();
    }

    // =========================================================================
    // UI update loop
    // =========================================================================

    private void arayuzuGuncelle() {
        if (dugmeler == null) return;

        // ── Satranç Modu render ───────────────────────────────────────────────
        if (satranModu && chessBoardMode != null) {
            for (int s = 0; s < ChessBoardMode.BOARD_SIZE; s++) {
                for (int u = 0; u < ChessBoardMode.BOARD_SIZE; u++) {
                    updateChessCell(s, u);
                }
            }
            satranKontrolEt();
            return;
        }

        // ── Leblebi / Klasik render ───────────────────────────────────────────
        Board tahta = aktifTahta();
        if (tahta == null) return;

        String[] sayiRenk = leblebModu ? LB_SAYI_RENK : (karanlikTema ? KT_SAYI_RENK : AT_SAYI_RENK);
        java.util.List<int[]> kargaKonumlar = (leblebModu && leblebiBoardMode != null)
                ? leblebiBoardMode.getKargaGosterilenMayinlar() : null;

        for (int s = 0; s < satirSayisi; s++) {
            for (int u = 0; u < sutunSayisi; u++) {
                Cell hucre = tahta.getHucre(s, u);

                boolean kargaHedef = false;
                if (kargaKonumlar != null && !hucre.isAcildiMi())
                    for (int[] k : kargaKonumlar) if (k[0] == s && k[1] == u) { kargaHedef = true; break; }

                boolean yilanY = leblebModu && yilanHucreleri.contains(s * sutunSayisi + u);

                byte nyDurum;
                if (yilanY) nyDurum = 5;
                else if (hucre.isAcildiMi() && hucre.isMayinMi()) nyDurum = 3;
                else if (hucre.isAcildiMi() && hucre.isGoldenLeblebi()) nyDurum = 20;
                else if (hucre.isAcildiMi()) nyDurum = (byte)(10 + hucre.getKomsuMayinSayisi());
                else if (hucre.isIsaretlendi()) nyDurum = 1;
                else if (kargaHedef) nyDurum = 4;
                else nyDurum = 0;

                if (nyDurum == hucreDurum[s][u]) continue;
                byte eskiDurum = hucreDurum[s][u];
                hucreDurum[s][u] = nyDurum;
                Button btn = dugmeler[s][u];

                if (nyDurum == 3) {
                    String mineEmoji = leblebModu ? "🐍" : "💣";
                    Label mineL = new Label(mineEmoji); mineL.setStyle("-fx-font-size:20px;");
                    btn.setText(""); btn.setGraphic(mineL); btn.setStyle(mayinHucreTarzi()); btn.setDisable(true);
                } else if (nyDurum >= 10) {
                    btn.setGraphic(null);
                    int k = hucre.getKomsuMayinSayisi();
                    btn.setText(k == 0 ? "" : String.valueOf(k)); btn.setStyle(acilmisHucreTarzi(k, sayiRenk)); btn.setDisable(true);
                    if (eskiDurum < 10) { ScaleTransition st = new ScaleTransition(Duration.millis(150), btn); st.setFromX(0.8); st.setFromY(0.8); st.setToX(1.0); st.setToY(1.0); st.play(); }
                } else if (nyDurum == 20) {
                    btn.setGraphic(null); btn.setText("🌟");
                    btn.setStyle("-fx-background-color: #ffd700;-fx-text-fill: #b8860b;-fx-font-size: 18px;-fx-font-weight: bold;-fx-border-color: #daa520;-fx-border-width: 2;-fx-background-radius: 3;-fx-border-radius: 3;");
                    btn.setDisable(true);
                    if (eskiDurum != 20 && leblebModu && leblebiBoardMode != null) {
                        leblebiBoardMode.altinLeblebiBulundu(); leblebiBoardMode.hucrePuaniEkle(9);
                        puanEtiketi.setText("🏆 " + leblebiBoardMode.getLeblebPuani() + " puan");
                        altinEtiketi.setText("💰 " + leblebiBoardMode.getAltin() + " altın");
                        altinLeblebiAnimasyonu(btn);
                    }
                } else if (nyDurum == 1) {
                    Label flagL = new Label("🚩"); flagL.setStyle("-fx-font-size:20px;");
                    btn.setText(""); btn.setGraphic(flagL); btn.setStyle(isaretliHucreTarzi()); btn.setDisable(false);
                } else if (nyDurum == 4) {
                    btn.setGraphic(null); btn.setText("");
                    btn.setStyle(acilmamisHucreTarzi() + "-fx-border-color: " + LB_KARGA_RENK + "; -fx-border-width: 3;"); btn.setDisable(false);
                    FadeTransition ft = new FadeTransition(Duration.millis(800), btn); ft.setFromValue(0.5); ft.setToValue(1.0); ft.setCycleCount(Animation.INDEFINITE); ft.setAutoReverse(true); ft.play();
                } else if (nyDurum == 5) {
                    Label snakeL = new Label("🐍"); snakeL.setStyle("-fx-font-size:20px;");
                    btn.setText(""); btn.setGraphic(snakeL);
                    btn.setStyle("-fx-background-color: #8B0000;-fx-border-color: #ff4444; -fx-border-width: 2;-fx-background-radius: 3; -fx-border-radius: 3; -fx-padding: 0;"); btn.setDisable(false);
                } else {
                    btn.setGraphic(null); btn.setText(""); btn.setStyle(acilmamisHucreTarzi()); btn.setDisable(false);
                }
            }
        }
        kontrolEt();
        if (leblebModu) gorevPanelGuncelle();
    }

    // ── Satranç hücresi render ────────────────────────────────────────────────

    private void updateChessCell(int r, int c) {
        Button btn = dugmeler[r][c];
        boolean revealed = chessBoardMode.isRevealed(r, c);
        boolean flagged  = chessBoardMode.isFlagged(r, c);
        boolean isMine   = chessBoardMode.isMine(r, c);
        int threat        = chessBoardMode.getThreat(r, c);

        // Encode state as byte for dirty check
        byte nyDurum;
        if      (revealed && isMine)   nyDurum = 3;
        else if (revealed)             nyDurum = (byte)(10 + Math.min(threat, 8));
        else if (flagged)              nyDurum = 1;
        else                           nyDurum = (byte)((r + c) % 2 == 0 ? 0 : 7); // iki renk

        if (nyDurum == hucreDurum[r][c]) return;
        byte eskiDurum = hucreDurum[r][c];
        hucreDurum[r][c] = nyDurum;

        if (revealed && isMine) {
            // Patlayan taş
            ChessMine mine = chessBoardMode.getMine(r, c);
            Label sym = new Label(mine != null ? mine.getSymbol() : "💥");
            sym.setStyle("-fx-font-size: 38px;");
            btn.setText(""); btn.setGraphic(sym);
            btn.setStyle("-fx-background-color: #c0392b; -fx-background-radius: 7; -fx-border-radius: 7;");
            btn.setDisable(true);
            if (eskiDurum != 3) {
                ScaleTransition boom = new ScaleTransition(Duration.millis(400), btn);
                boom.setFromX(0.5); boom.setFromY(0.5); boom.setToX(1.0); boom.setToY(1.0);
                boom.play();
            }
            btn.setScaleX(1.0);
            btn.setScaleX(1.0);
        } else if (revealed) {
            btn.setGraphic(null);
            btn.setText(threat == 0 ? "" : String.valueOf(threat));
            String fg = (threat > 0 && threat <= 8) ? CH_SAYI_RENK[threat] : "#4a6a8a";
            btn.setStyle("-fx-background-color: " + CH_ACILMIS + ";" +
                         "-fx-border-color: " + CH_CERCEVE + "; -fx-border-width: 1;" +
                         "-fx-background-radius: 4; -fx-border-radius: 4;" +
                         "-fx-text-fill: " + fg + "; -fx-font-weight: bold; -fx-font-size: 14px;");
            btn.setDisable(true);
            if (eskiDurum < 0 || eskiDurum == 0 || eskiDurum == 7) {
                FadeTransition ft = new FadeTransition(Duration.millis(180), btn);
                ft.setFromValue(0.3); ft.setToValue(1.0); ft.play();
            }
        } else if (flagged) {
            Label flag = new Label("🚩"); flag.setStyle("-fx-font-size: 20px;");
            btn.setText(""); btn.setGraphic(flag);
            btn.setStyle("-fx-background-color: " + CH_ISARETLI + ";" +
                         "-fx-border-color: #8060a0; -fx-border-width: 1;" +
                         "-fx-background-radius: 4; -fx-border-radius: 4;");
            btn.setDisable(false);
        } else {
            // Kapalı kare — satranç tahtası deseni
            btn.setGraphic(null); btn.setText("");
            boolean isLight = (r + c) % 2 == 0;
            String bg = isLight ? CH_ACILMAMIS_LIGHT : CH_ACILMAMIS_DARK;
            btn.setStyle("-fx-background-color: " + bg + ";" +
                         "-fx-border-color: " + CH_CERCEVE + "; -fx-border-width: 1;" +
                         "-fx-background-radius: 4; -fx-border-radius: 4; -fx-cursor: hand;");
            btn.setDisable(false);
        }
    }

    // ── Satranç Modu kazanma / kaybetme kontrolü ──────────────────────────────

    private void satranKontrolEt() {
        if (chessBoardMode == null) return;
        chessBoardMode.kazanmaKontrol();

        if (chessBoardMode.isKazanildi()) {
            zamanlayici.stop(); sesCal(sesKazan); sifirlaBtn.setText("🏆");
            durumEtiketi.setText("♟ Tüm güvenli kareler açıldı! Kazandın!");
            durumEtiketi.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #f0c040;");
            // Skor kaydet
            javafx.application.Platform.runLater(this::satranSkorKaydet);

        } else if (chessBoardMode.isOyunBitti() && !chessBoardMode.isSureDoldu()) {
            zamanlayici.stop(); sesCal(sesPatlama); sifirlaBtn.setText("💀");
            durumEtiketi.setText("💥 Bir satranç taşına bastın!");
            durumEtiketi.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #e74c3c;");
        }
    }

    // ── Satranç skor kayıt ────────────────────────────────────────────────────

    private boolean satranSkorKaydedildi = false;

    private void satranSkorKaydet() {
        if (satranSkorKaydedildi || chessBoardMode == null) return;
        satranSkorKaydedildi = true;

        int skor = chessBoardMode.skorHesapla();
        String zorlukAdi = switch (chessBoardMode.getDifficulty()) {
            case 1 -> "Kolay";
            case 2 -> "Orta";
            default -> "Zor";
        };

        TextInputDialog dlg = new TextInputDialog("Oyuncu");
        dlg.setTitle("Satranç Modu Skoru");
        dlg.setHeaderText("♟ Tebrikler! Satranç Mayın Tarlasını Tamamladın!");
        dlg.setContentText(String.format(
                "Zorluk: %s  |  Kalan süre: %ds%n🏆 Skor: %d%n%nİsminizi girin:",
                zorlukAdi, chessBoardMode.getKalanSure(), skor));
        dialogStilUygula(dlg, false);

        dlg.showAndWait().ifPresent(isim -> {
            if (!isim.isBlank())
                SkorTablosu.kaydet(isim.trim(), skor, chessBoardMode.getDifficulty(), "satranç",
                        ChessBoardMode.BOARD_SIZE, ChessBoardMode.BOARD_SIZE, chessBoardMode.getMineCount(), chessBoardMode.getBaslangicSuresi());
        });
        satranSkorKaydedildi = false;
    }

    // ── Animations ────────────────────────────────────────────────────────────

    private StackPane getMerkezStackPane() { return merkezIcerikKutusu; }

    private void altinLeblebiAnimasyonu(Button btn) {
        StackPane merkez = getMerkezStackPane();
        if (merkez == null) return;
        Label artiOn = new Label("+10 🌟");
        artiOn.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #ffd700; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 4, 0, 0, 2);");
        Bounds bBounds = btn.localToScene(btn.getBoundsInLocal());
        Bounds sBounds = merkez.sceneToLocal(bBounds);
        artiOn.setTranslateX(sBounds.getMinX() - merkez.getWidth() / 2 + btn.getWidth() / 2);
        artiOn.setTranslateY(sBounds.getMinY() - merkez.getHeight() / 2);
        merkez.getChildren().add(artiOn);
        TranslateTransition tt = new TranslateTransition(Duration.millis(800), artiOn); tt.setByY(-40);
        FadeTransition ft = new FadeTransition(Duration.millis(800), artiOn); ft.setFromValue(1.0); ft.setToValue(0.0);
        ParallelTransition pt = new ParallelTransition(tt, ft);
        pt.setOnFinished(e -> merkez.getChildren().remove(artiOn)); pt.play();
    }

    // ── Win / loss checks ─────────────────────────────────────────────────────

    private void kontrolEt() {
        if (satranModu) { satranKontrolEt(); return; }

        if (leblebModu && leblebiBoardMode != null) {
            leblebiBoardMode.kazanmaKontrol();
            if (leblebiBoardMode.isKazanildi()) {
                zamanlayici.stop(); sesCal(sesKazan); sifirlaBtn.setText("🎉");
                durumEtiketi.setText("🫘 Tüm yılanlar bulundu!");
                durumEtiketi.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #c89a2a;");
                toplamLeblebPuani += leblebiBoardMode.getLeblebPuani();
                leblebiBoardMode.diyalogTetikle(LeblebiBoardMode.DiyalogTetikleyici.KAZANMA);
                diyalogGoster(leblebiBoardMode.getAktifDiyalog());
                javafx.application.Platform.runLater(this::seviyeGecisiniGoster);
            } else if (leblebiBoardMode.isOyunBitti()) {
                zamanlayici.stop(); sifirlaBtn.setText("🪱");
                String msg = leblebiBoardMode.getKalanSure() <= 0 ? "⏰ Süre doldu! Mehmet Emmi üzüldü..." : "💀 Canların bitti! Yılanlar kazandı!";
                durumEtiketi.setText(msg);
                durumEtiketi.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #e74c3c;");
                leblebiBoardMode.diyalogTetikle(LeblebiBoardMode.DiyalogTetikleyici.KAYBETME);
                diyalogGoster(leblebiBoardMode.getAktifDiyalog());
                javafx.application.Platform.runLater(this::oyunSonuPopupuGoster);
            }
        } else if (klasikBoardMode != null) {
            if (klasikBoardMode.isKazanildi()) {
                zamanlayici.stop(); sesCal(sesKazan); sifirlaBtn.setText("😎");
                String winMsg = klasikBoardMode.isGeriSayim()
                        ? "★ Kazandınız! (" + klasikBoardMode.getSuruclukSure() + "s kaldı)"
                        : "★ Kazandınız! (" + klasikBoardMode.getSuruclukSure() + "s)";
                durumEtiketi.setText(winMsg);
                durumEtiketi.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;-fx-text-fill: " + (karanlikTema ? "#a6e3a1" : "#2e7d32") + ";");
                if (klasikBoardMode.isGeriSayim())
                    javafx.application.Platform.runLater(this::klasikZamanliSkorKaydet);
            } else if (klasikBoardMode.isOyunBitti() && !klasikBoardMode.isSureDoldu()) {
                zamanlayici.stop(); sesCal(sesPatlama); sifirlaBtn.setText("😵");
                durumEtiketi.setText("✖ Oyun Bitti!");
                durumEtiketi.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;-fx-text-fill: " + (karanlikTema ? "#f38ba8" : "#c62828") + ";");
            }
        }
    }

    // ── Level transition ──────────────────────────────────────────────────────

    private void seviyeGecisiniGoster() {
        boolean sonSeviye = Seviye.sonSeviyeMi(mevcutSeviye);
        LeblebiBoardMode.HasatRaporu rapor = leblebiBoardMode.hasatRaporuOlustur();

        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.75);");

        VBox fis = new VBox(15); fis.setMaxWidth(400); fis.setMaxHeight(VBox.USE_PREF_SIZE);
        fis.setPadding(new Insets(25));
        fis.setStyle("-fx-background-color: #fcf8e3;-fx-border-color: #d4b070; -fx-border-width: 3; -fx-border-radius: 8;-fx-background-radius: 8;-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.6), 15, 0, 0, 5);");
        fis.setAlignment(Pos.TOP_CENTER);

        Label baslikLabel = new Label(sonSeviye ? "🏆 OYUN TAMAMLANDI" : "✅ HASAT RAPORU");
        baslikLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #5c4033;");
        Label altBaslik = new Label(sonSeviye ? "Mehmet Emmi çok mutlu!" : "Seviye " + mevcutSeviye + " Başarıyla Bitti!");
        altBaslik.setStyle("-fx-font-size: 14px; -fx-text-fill: #8b5a2b; -fx-font-style: italic;");

        java.util.function.BiFunction<String, String, HBox> satirOlustur = (sol, sag) -> {
            Label sl = new Label(sol); Label sgl = new Label(sag); sgl.setStyle("-fx-font-weight: bold;");
            Region bosluk = new Region(); HBox.setHgrow(bosluk, Priority.ALWAYS);
            return new HBox(sl, bosluk, sgl);
        };

        VBox detaylar = new VBox(8);
        detaylar.getChildren().addAll(
                satirOlustur.apply("Zaman Bonusu:", "+" + rapor.surePuani()),
                satirOlustur.apply("Açılan Hücreler (" + rapor.acilanHucreSayisi() + "):", "+" + rapor.hucrePuani()),
                satirOlustur.apply("Yok Edilen Yılanlar (" + rapor.yokEdilenYilan() + "):", "+" + rapor.yilanPuani()));
        if (rapor.altinLeblebiBulundu() > 0)
            detaylar.getChildren().add(satirOlustur.apply("Altın Leblebi (" + rapor.altinLeblebiBulundu() + "):", "+" + (rapor.altinLeblebiBulundu() * 10)));

        VBox ekstralar = new VBox(10); ekstralar.setAlignment(Pos.CENTER_LEFT);
        if (!rapor.tamamlananGorevler().isEmpty()) {
            Label gBaslik = new Label("📜 Tamamlanan Görevler"); gBaslik.setStyle("-fx-font-weight: bold; -fx-text-fill: #2e7d32;");
            ekstralar.getChildren().add(gBaslik);
            for (LeblebiBoardMode.Gorev g : rapor.tamamlananGorevler()) {
                Label gl = new Label("✔ " + g.aciklama); gl.setWrapText(true); gl.setStyle("-fx-text-fill: #388e3c; -fx-font-size: 13px;");
                ekstralar.getChildren().add(gl);
            }
        }

        Label toplamLabel = new Label("TOPLAM KAZANÇ: " + leblebiBoardMode.getLeblebPuani() + " Puan");
        toplamLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #d84315; -fx-padding: 10 0 0 0;");
        Label emmiYorumu = new Label("💬 " + rapor.emmiYorumu());
        emmiYorumu.setWrapText(true);
        emmiYorumu.setStyle("-fx-font-size: 14px; -fx-font-style: italic; -fx-text-fill: #5d4037; -fx-background-color: #d7ccc8; -fx-padding: 10; -fx-background-radius: 5;");

        HBox butonlar = new HBox(15); butonlar.setAlignment(Pos.CENTER); butonlar.setPadding(new Insets(15, 0, 0, 0));
        Button btnSonraki = new Button(sonSeviye ? "🏅 Skoru Kaydet" : "▶ Sonraki Bölüm");
        btnSonraki.setStyle("-fx-background-color: #4caf50; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 8 20; -fx-cursor: hand; -fx-background-radius: 5;");
        btnSonraki.setOnAction(e -> {
            if (anaSahneKoku != null) anaSahneKoku.getChildren().remove(overlay);
            if (sonSeviye) oyunSonuPopupuGoster();
            else {
                if (leblebiBoardMode != null) { kaliciAltin = leblebiBoardMode.getAltin(); toplamKargaKullanim = leblebiBoardMode.getKargaKullanimToplami(); toplamIlacKullanim = leblebiBoardMode.getIlacKullanimToplami(); }
                mevcutSeviye++; leblebOyunuBaslat();
            }
        });
        Button btnMenu = new Button("Menüye Dön");
        btnMenu.setStyle("-fx-background-color: #e0e0e0; -fx-text-fill: #424242; -fx-font-size: 14px; -fx-padding: 8 15; -fx-cursor: hand; -fx-background-radius: 5;");
        btnMenu.setOnAction(e -> { if (anaSahneKoku != null) anaSahneKoku.getChildren().remove(overlay); menuGoster(); });
        butonlar.getChildren().addAll(btnMenu, btnSonraki);

        fis.getChildren().addAll(baslikLabel, altBaslik, new Separator(), detaylar, new Separator());
        if (!ekstralar.getChildren().isEmpty()) { fis.getChildren().add(ekstralar); fis.getChildren().add(new Separator()); }
        fis.getChildren().addAll(toplamLabel, emmiYorumu, butonlar);
        overlay.getChildren().add(fis);

        fis.setTranslateY(-50); fis.setOpacity(0);
        TranslateTransition tt = new TranslateTransition(Duration.millis(500), fis); tt.setToY(0);
        FadeTransition ft = new FadeTransition(Duration.millis(500), fis); ft.setToValue(1.0);
        ParallelTransition pt = new ParallelTransition(tt, ft);

        if (anaSahneKoku != null) { anaSahneKoku.getChildren().add(overlay); pt.play(); }
        else { kokDuzen.setCenter(overlay); pt.play(); }
    }

    private boolean popupGosterildi = false;

    private void oyunSonuPopupuGoster() {
        if (popupGosterildi) return;
        popupGosterildi = true;
        int finalSkor = (leblebiBoardMode != null)
                ? toplamLeblebPuani * 10 + leblebiBoardMode.getKalanSure() * 5
                : (klasikBoardMode != null ? klasikBoardMode.getSuruclukSure() : 0);
        TextInputDialog isimDialog = new TextInputDialog("Tarımcı");
        isimDialog.setTitle("Skor Tablosu"); isimDialog.setHeaderText("🏆 Oyun Bitti!");
        isimDialog.setContentText(String.format("Skor: %d\nİsminizi girin:", finalSkor));
        dialogStilUygula(isimDialog, false);
        isimDialog.showAndWait().ifPresent(isim -> { if (!isim.isBlank()) SkorTablosu.kaydet(isim.trim(), finalSkor, mevcutSeviye); });
        popupGosterildi = false; menuGoster();
    }

    private void klasikZamanliSkorKaydet() {
        if (klasikBoardMode == null || klasikBoardMode.isSkorKaydedildi()) return;
        klasikBoardMode.setSkorKaydedildi(true);
        int skor = klasikBoardMode.skorHesapla();
        int kalanSure = klasikBoardMode.getSuruclukSure();
        int gecenSure = klasikBoardMode.getBaslangicSuresi() - kalanSure;
        double yogunluk = (double) mayinSayisi / (satirSayisi * sutunSayisi) * 100;
        String presetEtiket = klasikBoardMode.presetEtiketiBul();
        String seviyeGoster = presetEtiket.isBlank() ? satirSayisi + "×" + sutunSayisi + ", " + mayinSayisi + " mayın" : presetEtiket;
        TextInputDialog dlg = new TextInputDialog("Oyuncu");
        dlg.setTitle("Skor Tablosu"); dlg.setHeaderText("⏱ Tebrikler! Zamanlı Modu Kazandınız!");
        dlg.setContentText(String.format("Zorluk: %s%nIzgara: %d×%d  |  Mayın: %d  |  Süre limiti: %ds%nGeçen süre: %ds  |  Kalan süre: %ds%nMayın yoğunluğu: %.0f%%%n%n🏆 Skor: %d%n%nİsminizi girin:",
                seviyeGoster, satirSayisi, sutunSayisi, mayinSayisi, klasikBoardMode.getBaslangicSuresi(), gecenSure, kalanSure, yogunluk, skor));
        dialogStilUygula(dlg, false);
        dlg.showAndWait().ifPresent(isim -> { if (!isim.isBlank()) SkorTablosu.kaydet(isim.trim(), skor, 0, SkorTablosu.MOD_ZAMANLI, satirSayisi, sutunSayisi, mayinSayisi, klasikBoardMode.getBaslangicSuresi()); });
        klasikBoardMode.setSkorKaydedildi(false);
    }

    private void yilanUyarisiGoster(int satir, int sutun) {
        yilanHucreleri.add(satir * sutunSayisi + sutun);
        if (merkezIcerikKutusu != null) {
            javafx.scene.shape.Rectangle flash = new javafx.scene.shape.Rectangle();
            flash.widthProperty().bind(merkezIcerikKutusu.widthProperty()); flash.heightProperty().bind(merkezIcerikKutusu.heightProperty());
            flash.setFill(javafx.scene.paint.Color.RED); flash.setOpacity(0.4); flash.setMouseTransparent(true);
            merkezIcerikKutusu.getChildren().add(flash);
            FadeTransition ft = new FadeTransition(Duration.millis(300), flash); ft.setToValue(0);
            ft.setOnFinished(e -> { flash.widthProperty().unbind(); flash.heightProperty().unbind(); merkezIcerikKutusu.getChildren().remove(flash); });
            ft.play();
        }
        ekranSarsintisi();
        if (leblebiBoardMode != null) { leblebiBoardMode.diyalogTetikle(LeblebiBoardMode.DiyalogTetikleyici.CAN_KAYBI); diyalogGoster(leblebiBoardMode.getAktifDiyalog()); }
        zamanlayici.play();
    }

    // =========================================================================
    // Score table
    // =========================================================================

    private void skorTablosunuGoster() {
        Stage pencere2 = new Stage();
        pencere2.setTitle("Skor Tablosu"); pencere2.initModality(Modality.APPLICATION_MODAL); pencere2.initOwner(pencere);
        String arka = "#1e1e2e", tabloBg = "#181825";
        TabPane sekmeler = new TabPane(); sekmeler.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        sekmeler.setStyle("-fx-background-color:" + arka + ";-fx-tab-min-width:140px;");

        Tab zamanlıTab = new Tab("⏱ Klasik (Zamanlı)");
        zamanlıTab.setContent(zamanlıSkorTabloIcerik(SkorTablosu.yukle(SkorTablosu.MOD_ZAMANLI), arka, tabloBg, pencere2));
        sekmeler.getTabs().add(zamanlıTab);

        // ── Satranç Modu Skor Sekmesi ─────────────────────────────────────────
        Tab satranTab = new Tab("♟ Satranç Modu");
        satranTab.setContent(skorTabloIcerik(
                SkorTablosu.yukle("satranç"),
                new String[]{"#", "İsim", "Skor", "Zorluk", "Tarih"},
                (g, i) -> new String[]{
                        (i + 1) + ".",
                        g.isim() == null || g.isim().isBlank() ? "—" : g.isim(),
                        String.valueOf(g.skor()),
                        (g.seviye() == 1 ? "Kolay" : g.seviye() == 2 ? "Orta" : g.seviye() == 3 ? "Zor" : "—"),
                        g.tarih() == null || g.tarih().isBlank() ? "—" : g.tarih()
                },
                "Henüz kayıt yok.\nSatranç modunu oyna ve adını yazdır!",
                arka, tabloBg));
        sekmeler.getTabs().add(satranTab);

        if (leblebAcildi) {
            Tab leblebTab = new Tab("🫘 Leblebi Tarlası");
            leblebTab.setContent(skorTabloIcerik(SkorTablosu.yukle(SkorTablosu.MOD_LEBLEBI),
                    new String[]{"#", "İsim", "Skor", "Seviye", "Tarih"},
                    (g, i) -> new String[]{(i + 1) + ".", g.isim() == null || g.isim().isBlank() ? "—" : g.isim(), String.valueOf(g.skor()), g.seviye() <= 0 ? "—" : "Seviye " + g.seviye(), g.tarih() == null || g.tarih().isBlank() ? "—" : g.tarih()},
                    "Henüz kayıt yok.\nLeblebi Tarlası'nı oyna ve adını yazdır!", arka, tabloBg));
            sekmeler.getTabs().add(leblebTab);
        }

        Button kapat = new Button("Kapat"); kapat.setStyle(butonTarzi() + "-fx-background-color:#313244;-fx-text-fill:#cdd6f4;"); kapat.setOnAction(e -> pencere2.close());
        VBox kok = new VBox(8, sekmeler, kapat); kok.setPadding(new Insets(16)); kok.setAlignment(Pos.TOP_CENTER);
        kok.setStyle("-fx-background-color:" + arka + ";"); VBox.setVgrow(sekmeler, Priority.ALWAYS);
        Scene s = new Scene(kok, 640, 520); s.setFill(javafx.scene.paint.Color.web(arka)); globalCssUygula(s);
        pencere2.setScene(s); pencere2.showAndWait();
    }

    private javafx.scene.Node zamanlıSkorTabloIcerik(java.util.List<SkorTablosu.SkorGirisi> liste, String arka, String tabloBg, Stage owner) {
        GridPane tablo = new GridPane(); tablo.setHgap(18); tablo.setVgap(8);
        tablo.setStyle("-fx-background-color:" + tabloBg + ";-fx-padding:16;-fx-background-radius:10;"); tablo.setAlignment(Pos.CENTER);
        String[] basliklar = {"#", "İsim", "Skor", "Zorluk", "Tarih"};
        for (int i = 0; i < basliklar.length; i++) { Label lbl = new Label(basliklar[i]); lbl.setStyle("-fx-font-weight:bold;-fx-text-fill:#89b4fa;-fx-font-size:13px;"); tablo.add(lbl, i, 0); }
        if (liste.isEmpty()) { Label bos = new Label("Henüz kayıt yok.\nZamanlı modda oyna ve adını yazdır!"); bos.setStyle("-fx-text-fill:#6c7086;-fx-font-size:13px;"); bos.setWrapText(true); tablo.add(bos, 0, 1, basliklar.length, 1); }
        else { for (int i = 0; i < Math.min(liste.size(), 20); i++) { SkorTablosu.SkorGirisi g = liste.get(i); String renk = i == 0 ? "#f1c40f" : i == 1 ? "#95a5a6" : i == 2 ? "#e67e22" : "#cdd6f4"; String[] degerler = {(i+1)+".", g.isim()==null||g.isim().isBlank()?"—":g.isim(), String.valueOf(g.skor()), presetEtiketiAl(g), g.tarih()==null||g.tarih().isBlank()?"—":g.tarih()}; for (int j = 0; j < degerler.length; j++) { Label lbl = new Label(degerler[j]); lbl.setStyle("-fx-text-fill:"+renk+";-fx-font-size:12px;"); tablo.add(lbl, j, i+1); } } }
        return scrollSar(tablo, arka);
    }

    private String presetEtiketiAl(SkorTablosu.SkorGirisi g) {
        if (g.satirSayisi() > 0) { for (KlasikBoardMode.KlasikAyar p : KlasikBoardMode.PRESETLER) { if (p.geriSayim() && p.satir()==g.satirSayisi() && p.sutun()==g.sutunSayisi() && p.mayin()==g.mayinSayisi() && p.sure()==g.sureSiniri()) { String e = p.etiket(); int spaceIdx = e.indexOf(' '); return spaceIdx >= 0 ? e.substring(spaceIdx + 1) : e; } } double yogunluk = (double) g.mayinSayisi() / (g.satirSayisi() * g.sutunSayisi()) * 100; return String.format("%d×%d / %d💣 / %.0f%%", g.satirSayisi(), g.sutunSayisi(), g.mayinSayisi(), yogunluk); } return "—";
    }

    private javafx.scene.Node skorTabloIcerik(java.util.List<SkorTablosu.SkorGirisi> liste, String[] basliklar,
            java.util.function.BiFunction<SkorTablosu.SkorGirisi, Integer, String[]> satirUret, String bosMsg, String arka, String tabloBg) {
        GridPane tablo = new GridPane(); tablo.setHgap(22); tablo.setVgap(8);
        tablo.setStyle("-fx-background-color:" + tabloBg + ";-fx-padding:16;-fx-background-radius:10;"); tablo.setAlignment(Pos.CENTER);
        for (int i = 0; i < basliklar.length; i++) { Label lbl = new Label(basliklar[i]); lbl.setStyle("-fx-font-weight:bold;-fx-text-fill:#89b4fa;-fx-font-size:13px;"); tablo.add(lbl, i, 0); }
        if (liste.isEmpty()) { Label bos = new Label(bosMsg); bos.setStyle("-fx-text-fill:#6c7086;-fx-font-size:13px;"); bos.setWrapText(true); tablo.add(bos, 0, 1, basliklar.length, 1); }
        else { for (int i = 0; i < Math.min(liste.size(), 20); i++) { SkorTablosu.SkorGirisi g = liste.get(i); String[] degerler = satirUret.apply(g, i); String renk = i==0?"#f1c40f":i==1?"#95a5a6":i==2?"#e67e22":"#cdd6f4"; for (int j = 0; j < degerler.length; j++) { Label lbl = new Label(degerler[j]); lbl.setStyle("-fx-text-fill:"+renk+";-fx-font-size:12px;"); tablo.add(lbl, j, i+1); } } }
        return scrollSar(tablo, arka);
    }

    private javafx.scene.Node scrollSar(javafx.scene.Node icerik, String arka) {
        ScrollPane scroll = new ScrollPane(icerik); scroll.setStyle("-fx-background-color:"+arka+";-fx-background:"+arka+";"); scroll.setFitToWidth(true);
        VBox wrapper = new VBox(scroll); wrapper.setStyle("-fx-background-color:"+arka+";-fx-padding:12;"); VBox.setVgrow(scroll, Priority.ALWAYS); return wrapper;
    }

    // =========================================================================
    // Cell sizing & styles
    // =========================================================================

    private void hucreBoyutlariniGuncelle() {
        if (dugmeler == null) return;

        double yanPanellerG = 0;
        if (leblebModu) { if (marketPanel != null) yanPanellerG += 240; if (gorevPanel != null) yanPanellerG += 220; }

        double kullanilabilirG = sahne.getWidth() - 52 - yanPanellerG;
        double kullanilabilirY = sahne.getHeight() - (satranModu ? 180 : 140);
        double hg = Math.floor(kullanilabilirG / sutunSayisi);
        double hy = Math.floor(kullanilabilirY / satirSayisi);
        double boy = Math.max(28, Math.min(hg, hy));
        // Satranç modunda minimum biraz daha büyük
        if (satranModu) boy = Math.max(boy, 40);
        double yaz = Math.max(9, boy * 0.28);

        String[] sayiRenk = satranModu ? CH_SAYI_RENK : (leblebModu ? LB_SAYI_RENK : (karanlikTema ? KT_SAYI_RENK : AT_SAYI_RENK));

        for (int s = 0; s < satirSayisi; s++) {
            for (int u = 0; u < sutunSayisi; u++) {
                Button btn = dugmeler[s][u];
                btn.setMinSize(boy, boy); btn.setPrefSize(boy, boy); btn.setMaxSize(boy, boy);

                if (satranModu && chessBoardMode != null && chessBoardMode.isRevealed(s, u) && !chessBoardMode.isMine(s, u)) {
                    int threat = chessBoardMode.getThreat(s, u);
                    String fg = (threat > 0 && threat <= 8) ? CH_SAYI_RENK[threat] : "#4a6a8a";
                    btn.setStyle("-fx-background-color: " + CH_ACILMIS + ";" +
                                 "-fx-border-color: " + CH_CERCEVE + "; -fx-border-width: 1;" +
                                 "-fx-background-radius: 4; -fx-border-radius: 4;" +
                                 "-fx-text-fill: " + fg + "; -fx-font-weight: bold; -fx-font-size: " + yaz + "px;");
                } else if (!satranModu) {
                    Board tahta = aktifTahta();
                    if (tahta != null) {
                        Cell h = tahta.getHucre(s, u);
                        if (h.isAcildiMi() && !h.isMayinMi()) {
                            int k = h.getKomsuMayinSayisi();
                            btn.setStyle(acilmisHucreTarzi(k, sayiRenk, yaz));
                        }
                    }
                }
            }
        }
    }

    private void onSceneResize(javafx.beans.value.ObservableValue<?> obs, Object o, Object n) {
        hucreBoyutlariniGuncelle();
    }

    private void tumHucreleriYenidenCiz() {
        if (hucreDurum != null) for (byte[] row : hucreDurum) java.util.Arrays.fill(row, (byte) -1);
        arayuzuGuncelle();
    }

    private void temayiUygula() {
        String arka  = satranModu ? CH_ARKAPLAN  : (leblebModu ? LB_ARKAPLAN  : (karanlikTema ? KT_ARKAPLAN  : AT_ARKAPLAN));
        String ustBr = satranModu ? CH_UST_BAR   : (leblebModu ? LB_UST_BAR   : (karanlikTema ? KT_UST_BAR   : AT_UST_BAR));
        String yazi  = satranModu ? "#cdd6f4"    : (leblebModu ? "#f5e6b0"    : (karanlikTema ? KT_YAZI      : AT_YAZI));
        String cerc  = satranModu ? CH_CERCEVE   : (leblebModu ? LB_CERCEVE   : (karanlikTema ? KT_CERCEVE   : AT_CERCEVE));

        kokDuzen.setStyle("-fx-background-color:" + arka + ";");
        if (kokDuzen.getTop() instanceof HBox bar) {
            bar.setStyle("-fx-background-color:" + ustBr + ";-fx-background-radius:10;-fx-padding:10 16 10 16;-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.35),8,0,0,3);");
            bar.getChildren().forEach(n -> {
                if (n instanceof Label l) l.setStyle(l.getStyle() + "-fx-text-fill:" + yazi + ";");
                else if (n instanceof Button b) b.setStyle(b.getStyle() +
                        "-fx-background-color:" + (satranModu ? "#0d2a3e" : (karanlikTema && !leblebModu ? "#313244" : (leblebModu ? "#7a5200" : "#c8cdd8"))) + ";-fx-text-fill:" + yazi + ";-fx-border-color:" + cerc + ";");
            });
        }
        if (izgaraDuzen != null) izgaraDuzen.setStyle("-fx-background-color:" + cerc + ";-fx-padding:2;");
    }

    // ── Cell style helpers ────────────────────────────────────────────────────

    // Konumsuz overload — chess dışı modlar için (pozisyon bağımsız)
    private String acilmamisHucreTarzi() {
        return acilmamisHucreTarzi(-1, -1);
    }

    // Konumlu — satranç modunda doğru rengi (açık/koyu kare) döndürür
    private String acilmamisHucreTarzi(int r, int c) {
        if (satranModu) {
            boolean isLight = (r < 0 || c < 0) || (r + c) % 2 == 0;
            String bg = isLight ? CH_ACILMAMIS_LIGHT : CH_ACILMAMIS_DARK;
            return "-fx-background-color:" + bg + ";-fx-border-color:" + CH_CERCEVE + ";-fx-border-width:1;-fx-background-radius:4;-fx-border-radius:4;-fx-padding:0;-fx-cursor:hand;";
        }
        if (leblebModu) return "-fx-background-color:" + LB_ACILMAMIS + ";-fx-border-color: #e8c55a #7a5500 #7a5500 #e8c55a;-fx-border-width:2;-fx-background-radius:3;-fx-border-radius:3;-fx-padding:0;-fx-cursor:hand;";
        String bg = karanlikTema ? KT_ACILMAMIS : AT_ACILMAMIS; String br = karanlikTema ? KT_CERCEVE : AT_CERCEVE;
        return "-fx-background-color:" + bg + ";-fx-border-color:" + br + ";-fx-border-width:1;-fx-background-radius:3;-fx-border-radius:3;-fx-padding:0;-fx-cursor:hand;";
    }

    // Hover — konumsuz overload
    private String acilmamisHucreHoverTarzi() {
        return acilmamisHucreHoverTarzi(-1, -1);
    }

    // Hover — konumlu (satranç modunda sarı highlight)
    private String acilmamisHucreHoverTarzi(int r, int c) {
        if (satranModu) return "-fx-background-color:#e8c84a;-fx-border-color:#c8a800;-fx-border-width:2;-fx-background-radius:4;-fx-border-radius:4;-fx-padding:0;-fx-cursor:hand;";
        if (leblebModu) return "-fx-background-color:#d9aa3a;-fx-border-color: #f0d070 #8a6510 #8a6510 #f0d070;-fx-border-width:2;-fx-background-radius:3;-fx-border-radius:3;-fx-padding:0;-fx-cursor:hand;";
        String bg = karanlikTema ? "#524f70" : "#c8d0e8"; String br = karanlikTema ? KT_CERCEVE : AT_CERCEVE;
        return "-fx-background-color:" + bg + ";-fx-border-color:" + br + ";-fx-border-width:1;-fx-background-radius:3;-fx-border-radius:3;-fx-padding:0;-fx-cursor:hand;";
    }

    private String acilmisHucreTarzi(int k, String[] sr) { return acilmisHucreTarzi(k, sr, 14); }
    private String acilmisHucreTarzi(int k, String[] sr, double yaz) {
        String bg, br, fgSoluk;
        if (satranModu) { bg = CH_ACILMIS; br = CH_CERCEVE; fgSoluk = "#4a6a8a"; }
        else if (leblebModu) { bg = LB_ACILMIS; br = "#5c3200"; fgSoluk = "#c8a060"; }
        else { bg = karanlikTema ? KT_ACILMIS : AT_ACILMIS; br = karanlikTema ? KT_CERCEVE : AT_CERCEVE; fgSoluk = karanlikTema ? KT_YAZI_SOLUK : AT_YAZI_SOLUK; }
        String fg = (k > 0 && k <= 8) ? sr[k] : fgSoluk;
        return "-fx-background-color:" + bg + ";-fx-border-color:" + br + ";-fx-border-width:1;-fx-background-radius:3;-fx-border-radius:3;-fx-padding:0;-fx-text-fill:" + fg + ";-fx-font-weight:bold;-fx-font-size:" + yaz + "px;";
    }

    private String isaretliHucreTarzi() {
        String bg = satranModu ? CH_ISARETLI : (leblebModu ? LB_ISARETLI : (karanlikTema ? KT_ISARETLI : AT_ISARETLI));
        String br = satranModu ? "#8060a0"   : (leblebModu ? LB_CERCEVE  : (karanlikTema ? KT_CERCEVE  : AT_CERCEVE));
        String fg = satranModu ? "#cdd6f4"   : (leblebModu ? "#3d2800"   : (karanlikTema ? "#f38ba8"   : "#c62828"));
        return "-fx-background-color:" + bg + ";-fx-border-color:" + br + ";-fx-border-width:1;-fx-background-radius:3;-fx-border-radius:3;-fx-padding:0;-fx-text-fill:" + fg + ";-fx-font-weight:bold;-fx-font-size:14px;-fx-cursor:hand;";
    }

    private String mayinHucreTarzi() {
        String bg = leblebModu ? LB_YILAN_RENK : "#8B0000";
        return "-fx-background-color:" + bg + ";-fx-border-width:1;-fx-background-radius:3;-fx-border-radius:3;-fx-padding:0;-fx-text-fill:#ffffff;-fx-font-weight:bold;-fx-font-size:16px;";
    }

    // =========================================================================
    // Dialog styling
    // =========================================================================

    private void dialogStilUygula(Alert alert, boolean leblebi) { dialogStilUygulaTemel(alert.getDialogPane(), leblebi); }
    private void dialogStilUygula(TextInputDialog dialog, boolean leblebi) { dialogStilUygulaTemel(dialog.getDialogPane(), leblebi); }

    private void dialogStilUygulaTemel(javafx.scene.control.DialogPane pane, boolean leblebi) {
        String bg = leblebi ? "#3d2800" : "#1e1e2e"; String fg = leblebi ? "#f5e6b0" : "#cdd6f4"; String hbg = leblebi ? "#5c3a00" : "#181825";
        pane.setStyle("-fx-background-color: " + bg + ";-fx-font-size: 14px;-fx-text-fill: " + fg + ";");
        Runnable stilUygula = () -> {
            for (javafx.scene.Node n : pane.lookupAll(".label")) if (n instanceof javafx.scene.control.Label lbl) { String mev = lbl.getStyle()==null?"":lbl.getStyle(); if (!mev.contains("-fx-text-fill")) lbl.setStyle(mev+"-fx-text-fill:"+fg+";-fx-font-size:14px;"); }
            javafx.scene.Node hNode = pane.lookup(".header-panel"); if (hNode != null) { hNode.setStyle("-fx-background-color:"+hbg+";"); javafx.scene.Node hLabel = pane.lookup(".header-panel .label"); if (hLabel instanceof javafx.scene.control.Label lbl) lbl.setStyle("-fx-text-fill:"+fg+";-fx-font-size:15px;-fx-font-weight:bold;"); }
            javafx.scene.Node tf = pane.lookup(".text-field"); if (tf instanceof javafx.scene.control.TextField field) field.setStyle("-fx-background-color:"+hbg+";-fx-text-fill:"+fg+";-fx-font-size:13px;");
            pane.getButtonTypes().forEach(bt -> { javafx.scene.Node btn = pane.lookupButton(bt); if (btn != null) btn.setStyle("-fx-background-color:"+(leblebi?"#7a5200":"#313244")+";-fx-text-fill:"+fg+";-fx-font-size:13px;-fx-background-radius:6;-fx-padding:6 14 6 14;"); });
        };
        stilUygula.run(); javafx.application.Platform.runLater(stilUygula);
    }

    // =========================================================================
    // Global CSS / font
    // =========================================================================

    private void globalCssUygula(Scene hedefSahne) {
        boolean fontYuklendi = false;
        try {
            java.net.URL fontUrl = getClass().getResource(FONT_YOL);
            if (fontUrl == null) fontUrl = new java.io.File(FONT_YOL).toURI().toURL();
            javafx.scene.text.Font yuklenenFont = javafx.scene.text.Font.loadFont(fontUrl.toExternalForm(), 14);
            fontYuklendi = (yuklenenFont != null);
        } catch (Exception ignored) {}
        String fontFamily = fontYuklendi ? "GameFont" : "System";
        String css = ".root{-fx-font-family:'"+fontFamily+"';}.label{-fx-font-family:'"+fontFamily+"';}.button{-fx-font-family:'"+fontFamily+"';}.text-field{-fx-font-family:'"+fontFamily+"';}.text-area{-fx-font-family:'"+fontFamily+"';}.dialog-pane .label{-fx-font-family:'"+fontFamily+"';}.dialog-pane .button{-fx-font-family:'"+fontFamily+"';}";
        String encoded = java.util.Base64.getEncoder().encodeToString(css.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        hedefSahne.getStylesheets().clear();
        hedefSahne.getStylesheets().add("data:text/css;base64," + encoded);
    }

    public static void main(String[] args) { launch(); }

    // =========================================================================
    // Can kaybı animasyonları
    // =========================================================================

    private void kirmiziFlasBas(Pane kokPanel) {
        Rectangle overlay = new Rectangle(); overlay.setFill(javafx.scene.paint.Color.web("#e74c3c")); overlay.setOpacity(0); overlay.setMouseTransparent(true);
        overlay.widthProperty().bind(kokPanel.widthProperty()); overlay.heightProperty().bind(kokPanel.heightProperty());
        kokPanel.getChildren().add(overlay);
        Timeline flash = new Timeline(
                new KeyFrame(Duration.ZERO, new javafx.animation.KeyValue(overlay.opacityProperty(), 0.0)),
                new KeyFrame(Duration.millis(120), new javafx.animation.KeyValue(overlay.opacityProperty(), 0.5)),
                new KeyFrame(Duration.millis(240), new javafx.animation.KeyValue(overlay.opacityProperty(), 0.0)),
                new KeyFrame(Duration.millis(360), new javafx.animation.KeyValue(overlay.opacityProperty(), 0.5)),
                new KeyFrame(Duration.millis(480), new javafx.animation.KeyValue(overlay.opacityProperty(), 0.0)),
                new KeyFrame(Duration.millis(600), new javafx.animation.KeyValue(overlay.opacityProperty(), 0.5)),
                new KeyFrame(Duration.millis(720), new javafx.animation.KeyValue(overlay.opacityProperty(), 0.0)));
        flash.setOnFinished(e -> kokPanel.getChildren().remove(overlay)); flash.play();
    }

    private void ekranSarsintisi() {
        if (merkezIcerikKutusu == null) return;
        if (aktifSarsinti != null) { aktifSarsinti.stop(); merkezIcerikKutusu.setScaleX(1.0); }
        aktifSarsinti = new Timeline(
                new KeyFrame(Duration.ZERO,        new javafx.animation.KeyValue(merkezIcerikKutusu.scaleXProperty(), 1.0)),
                new KeyFrame(Duration.millis(40),  new javafx.animation.KeyValue(merkezIcerikKutusu.scaleXProperty(), 0.97)),
                new KeyFrame(Duration.millis(80),  new javafx.animation.KeyValue(merkezIcerikKutusu.scaleXProperty(), 1.03)),
                new KeyFrame(Duration.millis(120), new javafx.animation.KeyValue(merkezIcerikKutusu.scaleXProperty(), 0.97)),
                new KeyFrame(Duration.millis(160), new javafx.animation.KeyValue(merkezIcerikKutusu.scaleXProperty(), 1.03)),
                new KeyFrame(Duration.millis(200), new javafx.animation.KeyValue(merkezIcerikKutusu.scaleXProperty(), 0.98)),
                new KeyFrame(Duration.millis(240), new javafx.animation.KeyValue(merkezIcerikKutusu.scaleXProperty(), 1.02)),
                new KeyFrame(Duration.millis(280), new javafx.animation.KeyValue(merkezIcerikKutusu.scaleXProperty(), 1.0)));
        aktifSarsinti.setOnFinished(e -> { merkezIcerikKutusu.setScaleX(1.0); aktifSarsinti = null; }); aktifSarsinti.play();
    }

    private void canIkonunuKir(int kalanCan) {
        if (canIkonKutusu == null) return;
        int kirIndex = kalanCan;
        if (kirIndex < 0 || kirIndex >= canIkonKutusu.getChildren().size()) return;
        javafx.scene.Node hedefIkon = canIkonKutusu.getChildren().get(kirIndex);
        ScaleTransition buyuKucul = new ScaleTransition(Duration.millis(400), hedefIkon); buyuKucul.setFromX(1.0); buyuKucul.setToX(1.4); buyuKucul.setFromY(1.0); buyuKucul.setToY(1.4); buyuKucul.setAutoReverse(true); buyuKucul.setCycleCount(2);
        FadeTransition soluklas = new FadeTransition(Duration.millis(400), hedefIkon); soluklas.setFromValue(1.0); soluklas.setToValue(0.25);
        ParallelTransition kirAnimasyon = new ParallelTransition(buyuKucul, soluklas);
        kirAnimasyon.setOnFinished(e -> { hedefIkon.setOpacity(0.2); if (hedefIkon instanceof Label lbl) lbl.setText("💀"); }); kirAnimasyon.play();
    }

    private void canSayisiGuncelle(int canSayisi) {
        if (canIkonKutusu == null) return;
        canIkonKutusu.getChildren().clear();
        for (int i = 0; i < canSayisi; i++) { Label ikon = new Label("❤"); ikon.setStyle("-fx-font-size: 18px; -fx-text-fill: #ff6b6b;"); canIkonKutusu.getChildren().add(ikon); }
        if (kokDuzen != null && kokDuzen.getTop() instanceof HBox bar) {
            if (!bar.getChildren().contains(canIkonKutusu)) { int idx = bar.getChildren().indexOf(canEtiketi); if (idx >= 0) bar.getChildren().add(idx + 1, canIkonKutusu); else bar.getChildren().add(canIkonKutusu); }
        }
    }

    // =========================================================================
    // Konuşma balonu
    // =========================================================================

    private void diyalogGoster(String metin) {
        if (konusmaBalonuPanel == null || konusmaBalonuLabel == null) return;
        if (metin == null || metin.isBlank()) return;
        if (aktifBalonAnimasyonu != null) { aktifBalonAnimasyonu.stop(); aktifBalonAnimasyonu = null; }
        konusmaBalonuLabel.setText(metin);
        konusmaBalonuPanel.setVisible(true); konusmaBalonuPanel.toFront(); konusmaBalonuPanel.setOpacity(0);
        FadeTransition belir = new FadeTransition(Duration.millis(300), konusmaBalonuPanel); belir.setFromValue(0.0); belir.setToValue(1.0);
        PauseTransition bekle = new PauseTransition(Duration.seconds(3));
        FadeTransition kaybol = new FadeTransition(Duration.millis(500), konusmaBalonuPanel); kaybol.setFromValue(1.0); kaybol.setToValue(0.0);
        kaybol.setOnFinished(e -> { konusmaBalonuPanel.setVisible(false); if (leblebiBoardMode != null) leblebiBoardMode.diyaloguTemizle(); aktifBalonAnimasyonu = null; });
        SequentialTransition dizi = new SequentialTransition(belir, bekle, kaybol);
        aktifBalonAnimasyonu = dizi; dizi.play();
    }

    // =========================================================================
    // Zirai İlaç hover preview
    // =========================================================================

    private void ilacHoverUygula(int merkezS, int merkezU) {
        if (dugmeler == null || aktifTahta() == null || leblebiBoardMode == null) return;
        Board tahta = aktifTahta();
        int cap = (leblebiBoardMode.getIlacLevel() == 1) ? 1 : ((leblebiBoardMode.getIlacLevel() == 2) ? 2 : 100);
        java.util.List<int[]> etkiAlani = new java.util.ArrayList<>();
        if (cap == 100) { for (int s = 0; s < satirSayisi; s++) etkiAlani.add(new int[]{s, merkezU}); for (int u = 0; u < sutunSayisi; u++) { if (u != merkezU) etkiAlani.add(new int[]{merkezS, u}); } }
        else { for (int ds = -cap; ds <= cap; ds++) for (int du = -cap; du <= cap; du++) etkiAlani.add(new int[]{merkezS + ds, merkezU + du}); }
        for (int[] pos : etkiAlani) {
            int ys = pos[0], yu = pos[1]; if (ys < 0 || ys >= satirSayisi || yu < 0 || yu >= sutunSayisi) continue;
            Button hedef = dugmeler[ys][yu]; if (hedef == null || hedef.isDisabled()) continue;
            Cell hh = tahta.getHucre(ys, yu); if (hh.isAcildiMi()) continue;
            boolean merkez = (ys == merkezS && yu == merkezU);
            hedef.setStyle(merkez ? ilacHoverMerkezTarzi() : ilacHoverKenarTarzi()); hedef.setCursor(javafx.scene.Cursor.CROSSHAIR);
        }
    }

    private void ilacHoverTemizle() {
        if (dugmeler == null || aktifTahta() == null) return;
        Board tahta = aktifTahta();
        for (int s = 0; s < satirSayisi; s++) for (int u = 0; u < sutunSayisi; u++) {
            Button btn = dugmeler[s][u]; if (btn == null || btn.isDisabled()) continue;
            Cell hh = tahta.getHucre(s, u); if (!hh.isAcildiMi() && !hh.isIsaretlendi()) btn.setStyle(acilmamisHucreTarzi());
            btn.setCursor(javafx.scene.Cursor.DEFAULT);
        }
    }

    private String ilacHoverMerkezTarzi() { return "-fx-background-color: #b8e068;-fx-border-color: #4caf50 #2e7d32 #2e7d32 #4caf50;-fx-border-width: 2.5; -fx-background-radius: 4; -fx-border-radius: 4;-fx-padding: 0; -fx-cursor: crosshair;-fx-effect: dropshadow(gaussian,#4caf50,8,0.5,0,0);"; }
    private String ilacHoverKenarTarzi()  { return "-fx-background-color: #d4edaa;-fx-border-color: #81c784 #388e3c #388e3c #81c784;-fx-border-width: 1.5; -fx-background-radius: 3; -fx-border-radius: 3;-fx-padding: 0; -fx-cursor: crosshair;"; }

    // ── Spinner / Label helpers ───────────────────────────────────────────────
    private Spinner<Integer> yapSpinner(int min, int max, int baslangic) { Spinner<Integer> sp = new Spinner<>(min, max, Math.max(min, Math.min(max, baslangic))); sp.setEditable(true); sp.setPrefWidth(100); return sp; }
    private Label etiketOlustur(String metin, String renk) { Label l = new Label(metin); l.setStyle("-fx-text-fill:" + renk + ";-fx-font-size:13px;"); return l; }
}
