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
import java.util.Optional;

/**
 * MinesweeperApp — Pure UI layer.
 *
 * All game logic lives in KlasikBoardMode and LeblebiBoardMode.
 * This class is responsible only for:
 *   - Rendering the menu, grid, HUD, market panel, and score table
 *   - Routing user input (clicks, key presses) to the active board mode
 *   - Running the JavaFX Timeline and forwarding ticks via sureyiGuncelle()
 *   - Playing sound effects and managing themes
 */
public class MinesweeperApp extends Application {

    // ── Classic mode lives constant ───────────────────────────────────────────
    private static final int KLASIK_CAN = 3;

    // ── Theme colours ─────────────────────────────────────────────────────────
    private static final String KT_ARKAPLAN  = "#1e1e2e";
    private static final String KT_ACILMAMIS = "#454158";
    private static final String KT_ACILMIS   = "#11111b";
    private static final String KT_ISARETLI  = "#524f6e";
    private static final String KT_CERCEVE   = "#6c7086";
    private static final String KT_YAZI      = "#cdd6f4";
    private static final String KT_YAZI_SOLUK= "#6c7086";
    private static final String KT_UST_BAR   = "#181825";

    private static final String AT_ARKAPLAN  = "#e8eaf0";
    private static final String AT_ACILMAMIS = "#c0c8d8";
    private static final String AT_ACILMIS   = "#f4f4f4";
    private static final String AT_ISARETLI  = "#b0b8cc";
    private static final String AT_CERCEVE   = "#9aa0b0";
    private static final String AT_YAZI      = "#1e1e2e";
    private static final String AT_YAZI_SOLUK= "#9aa0b0";
    private static final String AT_UST_BAR   = "#d0d4de";

    private static final String LB_ARKAPLAN     = "#3d2800";
    private static final String LB_ACILMAMIS    = "#c89a2a";
    private static final String LB_ACILMIS      = "#7a4e1a";
    private static final String LB_ISARETLI     = "#e8b84b";
    private static final String LB_SOLUCAN_RENK = "#4a7c2f";
    private static final String LB_CERCEVE      = "#a07020";
    private static final String LB_UST_BAR      = "#5c3a00";
    private static final String LB_KARGA_RENK   = "#c0392b";

    private static final String[] KT_SAYI_RENK = {
        "", "#89b4fa","#a6e3a1","#f38ba8","#74c7ec","#fab387","#89dceb","#b4befe","#cdd6f4"};
    private static final String[] AT_SAYI_RENK = {
        "", "#1565c0","#2e7d32","#c62828","#0277bd","#e65100","#00838f","#6a1b9a","#37474f"};
    private static final String[] LB_SAYI_RENK = {
        "", "#e8c27a","#6dbe45","#d95f3b","#5ba3d6","#d4a050","#40d4b0","#c080e0","#f0c060"};

    // ── Application state ─────────────────────────────────────────────────────
    private Stage pencere;
    private Scene anaSahne;
    private Scene menuSahne;

    /** Active board mode objects — at most one is non-null at any time. */
    private KlasikBoardMode  klasikBoardMode;
    private LeblebiBoardMode leblebiBoardMode;

    private boolean leblebModu   = false;
    private boolean karanlikTema = true;

    // Level system (Leblebi only)
    private int mevcutSeviye     = 1;
    private int toplamLeblebPuani = 0;

    // Grid
    private Button[][] dugmeler;
    private int satirSayisi, sutunSayisi, mayinSayisi;

    // Dirty-cell tracking: only repaint cells whose state changed
    // State encoding: 0=closed, 1=flagged, 2=opened-safe, 3=opened-mine, 4=karga-highlight, 5=yilan-Y
    private byte[][] hucreDurum;

    // Cells the player stepped on a snake (shown as 🐍 Y-marker)
    private java.util.Set<Integer> yilanHucreleri = new java.util.HashSet<>();

    // Flag counter (purely UI — does not belong in board mode)
    private int yerlestirilenIsaret;

    // UI components
    private Label      maynSayaciEtiketi;
    private Label      zamanlayiciEtiketi;
    private Label      durumEtiketi;
    private Label      canEtiketi;
    private Label      puanEtiketi;
    private Button     sifirlaBtn;
    private Timeline   zamanlayici;
    private BorderPane kokDuzen;
    private GridPane   izgaraDuzen;
    private Scene      sahne;
    private VBox       marketPanel;

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
    private static final String ASSET_SOLUCAN = "assets/solucan.png";
    private static final String ASSET_LEBLEBI = "assets/leblebi.png";
    private static final String ASSET_BAYRAK  = "assets/bayrak.png";
    private static final String ASSET_MAYIN   = "assets/mayin.png";

    // Pre-loaded image cache
    private Image imgSolucan;
    private Image imgLeblebi;
    private Image imgBayrak;
    private Image imgMayin;

    // Font
    private static final String FONT_YOL = "assets/fonts/GameFont.ttf";

    // =========================================================================
    //  start()
    // =========================================================================

    @Override
    public void start(Stage pencere) {
        this.pencere = pencere;
        menuGoster();
        pencere.setTitle("Mayın Tarlası");
        pencere.setMinWidth(500);
        pencere.setMinHeight(400);
        pencere.setResizable(true);
        pencere.show();

        Thread yukleyici = new Thread(() -> {
            sesFxYukle();
            assetleriOnYukle();
        }, "asset-loader");
        yukleyici.setDaemon(true);
        yukleyici.start();
    }

    // =========================================================================
    //  Assets & Sound
    // =========================================================================

    private void sesFxYukle() {
        sesKazma   = sesFxYukleGuveli("sounds/kazma.mp3");
        sesPatlama = sesFxYukleGuveli("sounds/patlama.mp3");
        sesButon   = sesFxYukleGuveli("sounds/buton.mp3");
        sesMarket  = sesFxYukleGuveli("sounds/market.mp3");
        sesKazan   = sesFxYukleGuveli("sounds/kazan.mp3");
    }

    private void assetleriOnYukle() {
        imgSolucan = assetImgYukle(ASSET_SOLUCAN, 20);
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

    private ImageView cachedView(Image img) {
        return img != null ? new ImageView(img) : null;
    }

    private AudioClip sesFxYukleGuveli(String yol) {
        try {
            java.net.URL url = getClass().getResource(yol);
            if (url == null) url = new java.io.File(yol).toURI().toURL();
            return new AudioClip(url.toString());
        } catch (Exception e) { return null; }
    }

    private void sesCal(AudioClip klip) {
        if (klip != null) klip.play();
    }

    // =========================================================================
    //  Menu
    // =========================================================================

    private void menuGoster() {
        VBox kok = new VBox(24);
        kok.setAlignment(Pos.CENTER);
        kok.setPadding(new Insets(60));
        kok.setStyle("-fx-background-color: " + KT_ARKAPLAN + ";");

        Label baslik = new Label("💣 MAYIN TARLASI");
        baslik.setStyle(
            "-fx-font-size: 36px; -fx-font-weight: bold;" +
            "-fx-text-fill: #cdd6f4; -fx-effect: dropshadow(gaussian,#89b4fa,12,0.4,0,0);"
        );
        baslik.setMaxWidth(Double.MAX_VALUE);
        baslik.setAlignment(javafx.geometry.Pos.CENTER);

        Button klasikBtn = menuButonOlustur("⛏  Klasik Mayın Tarlası Oyna", "#89b4fa", "#1e1e2e");
        klasikBtn.setOnAction(e -> {
            sesCal(sesButon);
            leblebModu = false;
            klasikOyunuBaslat();
        });

        Button skorBtn = menuButonOlustur("🏆  Skor Tablosu", "#a6e3a1", "#1e1e2e");
        skorBtn.setOnAction(e -> {
            sesCal(sesButon);
            skorTablosunuGoster();
        });

        Button leblebBtn = menuButonOlustur("🫘  Mehmet Emmi'nin Leblebi Tarlası", "#c89a2a", "#3d2800");
        leblebBtn.setVisible(leblebAcildi);
        leblebBtn.setManaged(leblebAcildi);
        leblebBtn.setId("leblebBtn");
        leblebBtn.setOnAction(e -> {
            sesCal(sesButon);
            leblebModu = true;
            mevcutSeviye = 1;
            toplamLeblebPuani = 0;
            leblebOyunuBaslat();
        });

        Label easterEggEtiketi = new Label(leblebAcildi ? "🫘 Mehmet Emmi'nin Leblebi Tarlası Modu Açık!" : "");
        easterEggEtiketi.setId("easterEggEtiket");
        easterEggEtiketi.setStyle("-fx-font-size: 13px; -fx-text-fill: #c89a2a; -fx-font-weight: bold;");

        kok.getChildren().addAll(baslik, klasikBtn, skorBtn, leblebBtn, easterEggEtiketi);

        menuSahne = new Scene(kok, 600, 500);
        menuSahne.setOnKeyPressed(olay -> {
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

        globalCssUygula(menuSahne);
        pencere.setScene(menuSahne);
    }

    private Button menuButonOlustur(String metin, String arkaplan, String yaziRengi) {
        Button btn = new Button(metin);
        btn.setPrefWidth(320);
        btn.setPrefHeight(50);
        btn.setStyle(
            "-fx-background-color: " + arkaplan + ";" +
            "-fx-text-fill: " + yaziRengi + ";" +
            "-fx-font-size: 15px; -fx-font-weight: bold;" +
            "-fx-background-radius: 10; -fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 8, 0, 0, 2);"
        );
        btn.setOnMouseEntered(e -> btn.setOpacity(0.85));
        btn.setOnMouseExited(e  -> btn.setOpacity(1.0));
        return btn;
    }

    // ── Easter Egg ────────────────────────────────────────────────────────────

    private void easterEggTetikle(VBox kok, Button leblebBtn, Label etiket) {
        TranslateTransition sarsinti = new TranslateTransition(Duration.millis(60), kok);
        sarsinti.setByX(12);
        sarsinti.setCycleCount(8);
        sarsinti.setAutoReverse(true);

        Timeline flash = new Timeline(
            new KeyFrame(Duration.millis(0),   e -> kok.setStyle("-fx-background-color: #5c3a00;")),
            new KeyFrame(Duration.millis(150),  e -> kok.setStyle("-fx-background-color: #c89a2a;")),
            new KeyFrame(Duration.millis(300),  e -> kok.setStyle("-fx-background-color: #3d2800;")),
            new KeyFrame(Duration.millis(450),  e -> kok.setStyle("-fx-background-color: " + KT_ARKAPLAN + ";"))
        );

        sarsinti.play();
        flash.play();

        leblebBtn.setVisible(true);
        leblebBtn.setManaged(true);
        etiket.setText("🫘 Mehmet Emmi'nin Leblebi Tarlası Modu Açıldı!");

        Alert uyari = new Alert(Alert.AlertType.INFORMATION);
        uyari.setTitle("Gizli Mod!");
        uyari.setHeaderText("🫘 Leblebi Tarlası Modu Açıldı!");
        uyari.setContentText(
            "Mehmet Emmi çok mutlu!\n\n" +
            "Leblebi Tarlası modunu keşfettin.\n" +
            "Yılanları temizle, leblebi kazan, marketten güç al!"
        );
        dialogStilUygula(uyari, true);
        uyari.showAndWait();
    }

    // =========================================================================
    //  Classic game — setup dialog
    // =========================================================================

    private void klasikOyunuBaslat() {
        klasikAyarDialogGoster();
    }

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
        presetGrid.setHgap(8);
        presetGrid.setVgap(8);

        Spinner<Integer> satirSpinner = yapSpinner(7, 30, 10);
        Spinner<Integer> sutunSpinner = yapSpinner(7, 30, 10);
        int minMayinSayisi = (int) Math.floor(satirSpinner.getValueFactory().getValue()*sutunSpinner.getValueFactory().getValue()*0.15);
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
        yukariBtn.setToggleGroup(timerGrup);
        geriBtn.setToggleGroup(timerGrup);
        yukariBtn.setSelected(true);
        yukariBtn.setStyle("-fx-text-fill:" + yazi + ";");
        geriBtn.setStyle("-fx-text-fill:" + yazi + ";");
        Spinner<Integer> sureSpinner = yapSpinner(10, 3600, 180);
        sureSpinner.setDisable(true);
        geriBtn.selectedProperty().addListener((obs, eski, yeni) -> sureSpinner.setDisable(!yeni));

        // Preset buttons — all defined in KlasikBoardMode
        for (int i = 0; i < KlasikBoardMode.PRESETLER.length; i++) {
            KlasikBoardMode.KlasikAyar p = KlasikBoardMode.PRESETLER[i];
            Button pb = new Button(p.etiket());
            pb.setPrefWidth(200);
            pb.setStyle(
                "-fx-background-color:#313244;-fx-text-fill:" + yazi + ";" +
                "-fx-background-radius:8;-fx-border-radius:8;" +
                "-fx-border-color:" + ayrac + ";-fx-cursor:hand;-fx-font-size:12px;"
            );
            pb.setOnMouseEntered(e -> pb.setStyle(pb.getStyle().replace("#313244","#45475a")));
            pb.setOnMouseExited(e  -> pb.setStyle(pb.getStyle().replace("#45475a","#313244")));
            pb.setOnAction(e -> {
                satirSpinner.getValueFactory().setValue(p.satir());
                sutunSpinner.getValueFactory().setValue(p.sutun());
                mayinSpinner.getValueFactory().setValue(p.mayin());
                if (p.geriSayim()) {
                    geriBtn.setSelected(true);
                    sureSpinner.getValueFactory().setValue(p.sure());
                } else {
                    yukariBtn.setSelected(true);
                }
            });
            presetGrid.add(pb, i % 2, i / 2);
        }

        Label ozelBaslik = new Label("Ya da Özel Ayarla");
        ozelBaslik.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:" + vurgu + ";");

        Label satirLbl = etiketOlustur("Satır sayısı:", yazi);
        Label sutunLbl = etiketOlustur("Sütun sayısı:", yazi);
        Label mayinLbl = etiketOlustur("Mayın sayısı:", yazi);
        Label sureLbl  = etiketOlustur("Süre (saniye):", yazi);

        Runnable mayinKlamp = () -> {
            int maks = Math.max(1, satirSpinner.getValue() * sutunSpinner.getValue() - 9);
            ((SpinnerValueFactory.IntegerSpinnerValueFactory) mayinSpinner.getValueFactory()).setMax(maks);
        };
        satirSpinner.valueProperty().addListener((o, e, n) -> mayinKlamp.run());
        sutunSpinner.valueProperty().addListener((o, e, n) -> mayinKlamp.run());

        GridPane ozelGrid = new GridPane();
        ozelGrid.setHgap(12);
        ozelGrid.setVgap(10);
        ozelGrid.addRow(0, satirLbl, satirSpinner);
        ozelGrid.addRow(1, sutunLbl, sutunSpinner);
        ozelGrid.addRow(2, mayinLbl, mayinSpinner);

        Label timerBaslik = new Label("Zamanlayıcı Modu");
        timerBaslik.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:" + vurgu + ";");

        GridPane timerGrid = new GridPane();
        timerGrid.setHgap(12);
        timerGrid.setVgap(8);
        timerGrid.addRow(0, yukariBtn);
        timerGrid.addRow(1, geriBtn);
        timerGrid.addRow(2, sureLbl, sureSpinner);

        for (Spinner<?> sp : new Spinner<?>[]{ satirSpinner, sutunSpinner, mayinSpinner, sureSpinner })
            sp.setStyle("-fx-background-color:" + girdiArka + ";-fx-text-fill:" + yazi + ";");

        Button oynaBtn = new Button("▶  Oyna!");
        oynaBtn.setPrefWidth(180);
        oynaBtn.setPrefHeight(44);
        oynaBtn.setStyle(
            "-fx-background-color:#89b4fa;-fx-text-fill:#1e1e2e;" +
            "-fx-font-size:15px;-fx-font-weight:bold;" +
            "-fx-background-radius:10;-fx-cursor:hand;"
        );
        oynaBtn.setOnAction(e -> {
            satirSayisi = satirSpinner.getValue();
            sutunSayisi = sutunSpinner.getValue();
            mayinSayisi = Math.min(mayinSpinner.getValue(), satirSayisi * sutunSayisi - 9);
            boolean geri = geriBtn.isSelected();
            int sure     = geri ? sureSpinner.getValue() : 0;

            leblebModu       = false;
            leblebiBoardMode = null;
            klasikBoardMode  = new KlasikBoardMode(satirSayisi, sutunSayisi, mayinSayisi, geri, sure);

            dialog.close();
            oyunSahnesiniBaSlat(false);
        });

        Separator sep1 = new Separator(); sep1.setStyle("-fx-background-color:" + ayrac + ";");
        Separator sep2 = new Separator(); sep2.setStyle("-fx-background-color:" + ayrac + ";");

        VBox kok = new VBox(14,
            presetBaslik, presetGrid,
            sep1,
            ozelBaslik, ozelGrid,
            sep2,
            timerBaslik, timerGrid,
            oynaBtn
        );
        kok.setPadding(new Insets(24));
        kok.setAlignment(Pos.CENTER_LEFT);
        kok.setStyle("-fx-background-color:" + arka + ";");

        Scene dialogSahne = new Scene(kok);
        globalCssUygula(dialogSahne);
        dialog.setScene(dialogSahne);
        dialog.showAndWait();
    }

    // ── Spinner / Label helpers ───────────────────────────────────────────────

    private Spinner<Integer> yapSpinner(int min, int max, int baslangic) {
        Spinner<Integer> sp = new Spinner<>(min, max, Math.max(min, Math.min(max, baslangic)));
        sp.setEditable(true);
        sp.setPrefWidth(100);
        return sp;
    }

    private Label etiketOlustur(String metin, String renk) {
        Label l = new Label(metin);
        l.setStyle("-fx-text-fill:" + renk + ";-fx-font-size:13px;");
        return l;
    }

    // =========================================================================
    //  Leblebi game — start
    // =========================================================================

    private void leblebOyunuBaslat() {
        yilanHucreleri.clear();
        Seviye seviye = Seviye.getSeviye(mevcutSeviye);
        satirSayisi  = seviye.getSatirSayisi();
        sutunSayisi  = seviye.getSutunSayisi();
        mayinSayisi  = seviye.getSolucanSayisi();
        klasikBoardMode  = null;
        leblebiBoardMode = new LeblebiBoardMode(
            satirSayisi, sutunSayisi, mayinSayisi,
            seviye.getSureSaniye(), KLASIK_CAN
        );
        oyunSahnesiniBaSlat(true);
    }

    // =========================================================================
    //  Game scene construction
    // =========================================================================

    private void oyunSahnesiniBaSlat(boolean leblebi) {
        kokDuzen = new BorderPane();
        kokDuzen.setPadding(new Insets(12));

        ustBariOlustur();
        izgarayiOlustur();

        if (leblebi) {
            marketPanelOlustur();
            kokDuzen.setRight(marketPanel);
            marketPanelGuncelle();
        }

        temayiUygula();
        zamanlayiciBaslat();
        arayuzuGuncelle();

        double genislik  = leblebi ? 900 : 620;
        double yukseklik = leblebi
            ? Math.max(680, satirSayisi * 50 + 200)
            : Math.max(600, satirSayisi * 50 + 160);

        sahne = new Scene(kokDuzen, genislik, yukseklik);
        sahne.widthProperty().addListener((g, e, y)  -> hucreBoyutlariniGuncelle());
        sahne.heightProperty().addListener((g, e, y) -> hucreBoyutlariniGuncelle());
        globalCssUygula(sahne);

        pencere.setScene(sahne);
        hucreBoyutlariniGuncelle();
    }

    // ── HUD bar ───────────────────────────────────────────────────────────────

    private void ustBariOlustur() {
        String hudLabelStil = "-fx-font-size: 19px; -fx-font-weight: bold; -fx-padding: 4 12 4 12;" +
                              "-fx-background-radius: 8;";
        String hudPuanStil  = "-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #f0c040;" +
                              "-fx-padding: 4 12 4 12; -fx-background-radius: 8;";

        maynSayaciEtiketi = new Label();
        maynSayaciEtiketi.setStyle(hudLabelStil);

        zamanlayiciEtiketi = new Label();
        zamanlayiciEtiketi.setStyle(hudLabelStil);

        canEtiketi = new Label("");
        canEtiketi.setStyle(hudLabelStil + "-fx-text-fill: #ff6b6b;");

        puanEtiketi = new Label("");
        puanEtiketi.setStyle(hudPuanStil);

        durumEtiketi = new Label("");
        durumEtiketi.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");

        sifirlaBtn = new Button(leblebModu ? "🌾" : "😊");
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
        temaBtn.setVisible(!leblebModu);
        temaBtn.setManaged(!leblebModu);
        temaBtn.setOnAction(o -> {
            karanlikTema = !karanlikTema;
            temaBtn.setText(karanlikTema ? "☀" : "★");
            temayiUygula();
            tumHucreleriYenidenCiz();
            hucreBoyutlariniGuncelle();
        });

        HBox ustBar = new HBox(8,
            menuBtn, maynSayaciEtiketi, canEtiketi, sifirlaBtn,
            zamanlayiciEtiketi, puanEtiketi, durumEtiketi, temaBtn
        );
        ustBar.setAlignment(Pos.CENTER_LEFT);
        ustBar.setPadding(new Insets(6, 8, 10, 8));
        kokDuzen.setTop(ustBar);

        guncelleUstBar();
    }

    private void guncelleUstBar() {
        String mayinSimge = leblebModu ? "🐍 " : "💣 ";
        maynSayaciEtiketi.setText(mayinSimge + (mayinSayisi - yerlestirilenIsaret));

        if (leblebModu && leblebiBoardMode != null) {
            zamanlayiciEtiketi.setText("⏳ " + leblebiBoardMode.getKalanSure() + "s");
            canEtiketi.setText("❤ x" + leblebiBoardMode.getCanSayisi());
            puanEtiketi.setText("🫘 " + leblebiBoardMode.getLeblebPuani() + " puan");
        } else if (klasikBoardMode != null && klasikBoardMode.isGeriSayim()) {
            zamanlayiciEtiketi.setText("⏳ " + klasikBoardMode.getSuruclukSure() + "s");
            zamanlayiciEtiketi.setStyle(
                "-fx-font-size: 19px; -fx-font-weight: bold; -fx-padding: 4 12 4 12;" +
                "-fx-background-radius: 8; -fx-text-fill: #cdd6f4;");
            canEtiketi.setText("");
            puanEtiketi.setText("");
        } else {
            zamanlayiciEtiketi.setText("⏱ 0s");
            canEtiketi.setText("");
            puanEtiketi.setText("");
        }
    }

    private String butonTarzi() {
        return "-fx-font-size: 13px; -fx-padding: 4 10 4 10;" +
               "-fx-cursor: hand; -fx-border-radius: 6; -fx-background-radius: 6;";
    }

    // ── Grid ──────────────────────────────────────────────────────────────────

    private void izgarayiOlustur() {
        izgaraDuzen = new GridPane();
        izgaraDuzen.setHgap(3);
        izgaraDuzen.setVgap(3);
        izgaraDuzen.setAlignment(Pos.CENTER);

        dugmeler = new Button[satirSayisi][sutunSayisi];
        hucreDurum = new byte[satirSayisi][sutunSayisi];
        for (byte[] row : hucreDurum) java.util.Arrays.fill(row, (byte) -1);
        yerlestirilenIsaret = 0;

        for (int s = 0; s < satirSayisi; s++) {
            for (int u = 0; u < sutunSayisi; u++) {
                Button btn = new Button();
                btn.setPrefSize(48, 48);
                btn.setMinSize(28, 28);

                int sr = s, su = u;
                btn.setOnMouseClicked(olay -> {
                    if (oyunAktifDegil()) return;
                    if (olay.getButton() == MouseButton.PRIMARY) {
                        hucreAc(sr, su);
                    } else if (olay.getButton() == MouseButton.SECONDARY) {
                        isaretKoy(sr, su);
                    }
                    arayuzuGuncelle();
                    hucreBoyutlariniGuncelle();
                    if (marketPanel != null) marketPanelGuncelle();
                });

                btn.setOnMouseEntered(olay -> {
                    if (!btn.isDisabled() && !oyunAktifDegil()) {
                        Board tahta = aktifTahta();
                        Cell hh = tahta.getHucre(sr, su);
                        if (!hh.isAcildiMi() && !hh.isIsaretlendi())
                            btn.setStyle(acilmamisHucreHoverTarzi());
                    }
                });
                btn.setOnMouseExited(olay -> {
                    if (!btn.isDisabled()) {
                        Board tahta = aktifTahta();
                        Cell hh = tahta.getHucre(sr, su);
                        if (!hh.isAcildiMi() && !hh.isIsaretlendi())
                            btn.setStyle(acilmamisHucreTarzi());
                    }
                });

                dugmeler[s][u] = btn;
                izgaraDuzen.add(btn, u, s);
            }
        }

        StackPane merkez = new StackPane(izgaraDuzen);
        merkez.setAlignment(Pos.CENTER);
        VBox.setVgrow(merkez, Priority.ALWAYS);
        kokDuzen.setCenter(merkez);
    }

    // ── Market panel ──────────────────────────────────────────────────────────

    private void marketPanelOlustur() {
        Seviye seviye = Seviye.getSeviye(mevcutSeviye);

        marketPanel = new VBox(10);
        marketPanel.setPadding(new Insets(14));
        marketPanel.setPrefWidth(200);
        marketPanel.setStyle(
            "-fx-background-color: #4a2e00; -fx-background-radius: 14;" +
            "-fx-border-color: #c89a2a; -fx-border-width: 2; -fx-border-radius: 14;" +
            "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.5),12,0,0,4);"
        );

        Label baslik = new Label("🛒  Market");
        baslik.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #f5e6b0;" +
                        "-fx-padding: 0 0 4 0;");

        Label seviyeEtiketi = new Label("Seviye " + mevcutSeviye + ": " + seviye.getIsim());
        seviyeEtiketi.setStyle("-fx-font-size: 11px; -fx-text-fill: #c89a2a;");
        seviyeEtiketi.setWrapText(true);

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #c89a2a; -fx-opacity: 0.5;");

        Button kargaBtn = marketKartButonOlustur("🐦", "Karga",         "15 🫘");
        Button saatBtn  = marketKartButonOlustur("⏰", "Emmi'nin Saati", "20 🫘");
        Button ilacBtn  = marketKartButonOlustur("🧪", "Zirai İlaç",    "30 🫘");
        Button kalpBtn  = marketKartButonOlustur("❤️", "Ekstra Kalp",   "50 🫘");

        kargaBtn.setId("kargaBtn");
        saatBtn.setId("saatBtn");
        ilacBtn.setId("ilacBtn");
        kalpBtn.setId("kalpBtn");

        kargaBtn.setOnAction(e -> {
            if (leblebiBoardMode == null || oyunAktifDegil()) return;
            int[] konum = leblebiBoardMode.kargaKullan();
            if (konum == null) {
                bilgilendirmeGoster("Karga", "Yetersiz puan veya yılan bulunamadı!");
            } else {
                sesCal(sesMarket);
                arayuzuGuncelle();
                hucreBoyutlariniGuncelle();
                marketPanelGuncelle();
            }
        });

        saatBtn.setOnAction(e -> {
            if (leblebiBoardMode == null || oyunAktifDegil()) return;
            if (leblebiBoardMode.emmininSaatiniKullan()) {
                sesCal(sesMarket);
                marketPanelGuncelle();
                zamanlayiciEtiketi.setText("⏳ " + leblebiBoardMode.getKalanSure() + "s");
                puanEtiketi.setText("🫘 " + leblebiBoardMode.getLeblebPuani() + " puan");
            } else {
                bilgilendirmeGoster("Emmi'nin Saati", "Yetersiz puan! (20 puan gerekli)");
            }
        });

        ilacBtn.setOnAction(e -> {
            if (leblebiBoardMode == null || oyunAktifDegil()) return;
            if (leblebiBoardMode.isZirayiIlacAktif()) {
                bilgilendirmeGoster("Zirai İlaç", "İlaç zaten aktif! Bir hücreye tıkla.");
                return;
            }
            if (leblebiBoardMode.zirayiIlacAktiflesir()) {
                sesCal(sesMarket);
                bilgilendirmeGoster("Zirai İlaç", "Aktif! Şimdi bir hücreye sol tıkla — 3x3 alan güvenlice açılacak.");
                marketPanelGuncelle();
                puanEtiketi.setText("🫘 " + leblebiBoardMode.getLeblebPuani() + " puan");
                ilacBtn.setStyle(ilacBtn.getStyle() + "-fx-border-color: #e74c3c; -fx-border-width: 2;");
            } else {
                bilgilendirmeGoster("Zirai İlaç", "Yetersiz puan! (30 puan gerekli)");
            }
        });

        kalpBtn.setOnAction(e -> {
            if (leblebiBoardMode == null || oyunAktifDegil()) return;
            if (leblebiBoardMode.ekstraKalpAl()) {
                sesCal(sesMarket);
                canEtiketi.setText("❤ x" + leblebiBoardMode.getCanSayisi());
                marketPanelGuncelle();
                puanEtiketi.setText("🫘 " + leblebiBoardMode.getLeblebPuani() + " puan");
            } else {
                bilgilendirmeGoster("Ekstra Kalp", "Yetersiz puan! (50 puan gerekli)");
            }
        });

        GridPane kartlar = new GridPane();
        kartlar.setHgap(8);
        kartlar.setVgap(8);
        kartlar.add(kargaBtn, 0, 0);
        kartlar.add(saatBtn,  1, 0);
        kartlar.add(ilacBtn,  0, 1);
        kartlar.add(kalpBtn,  1, 1);
        GridPane.setHgrow(kargaBtn, Priority.ALWAYS);
        GridPane.setHgrow(saatBtn,  Priority.ALWAYS);
        GridPane.setHgrow(ilacBtn,  Priority.ALWAYS);
        GridPane.setHgrow(kalpBtn,  Priority.ALWAYS);

        marketPanel.getChildren().addAll(baslik, seviyeEtiketi, sep, kartlar);
    }

    private static final String MKT_BTN_NORMAL =
        "-fx-background-color: #6b3e00; -fx-text-fill: #f5e6b0;" +
        "-fx-background-radius: 10; -fx-border-radius: 10;" +
        "-fx-border-color: #a07020; -fx-border-width: 1.5;" +
        "-fx-cursor: hand; -fx-alignment: center;";
    private static final String MKT_BTN_HOVER =
        "-fx-background-color: #8a5500; -fx-text-fill: #fff8e0;" +
        "-fx-background-radius: 10; -fx-border-radius: 10;" +
        "-fx-border-color: #f0c040; -fx-border-width: 2;" +
        "-fx-cursor: hand; -fx-alignment: center;" +
        "-fx-effect: dropshadow(gaussian,#f0c040,8,0.4,0,0);";

    private Button marketKartButonOlustur(String ikon, String isim, String fiyat) {
        Label ikonLabel  = new Label(ikon);
        ikonLabel.setStyle("-fx-font-size: 28px;");
        Label isimLabel  = new Label(isim);
        isimLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #d4b070; -fx-font-weight: bold;");
        isimLabel.setWrapText(true);
        isimLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        Label fiyatLabel = new Label(fiyat);
        fiyatLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #f0c040; -fx-font-weight: bold;");

        VBox kutu = new VBox(2, ikonLabel, isimLabel, fiyatLabel);
        kutu.setAlignment(Pos.CENTER);
        kutu.setMaxWidth(Double.MAX_VALUE);

        Button btn = new Button();
        btn.setGraphic(kutu);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setPrefHeight(90);
        btn.setStyle(MKT_BTN_NORMAL);
        btn.setOnMouseEntered(e -> { if (!btn.isDisabled()) btn.setStyle(MKT_BTN_HOVER); });
        btn.setOnMouseExited(e  -> btn.setStyle(MKT_BTN_NORMAL));
        return btn;
    }

    private void marketPanelGuncelle() {
        if (marketPanel == null || leblebiBoardMode == null) return;
        int puan = leblebiBoardMode.getLeblebPuani();

        java.util.function.Consumer<javafx.scene.Node> guncelleBtn = dugum -> {
            if (dugum instanceof Button btn) {
                String id = btn.getId();
                if (id == null) return;
                boolean aktif = switch (id) {
                    case "kargaBtn" -> puan >= 15;
                    case "saatBtn"  -> puan >= 20;
                    case "ilacBtn"  -> puan >= 30;
                    case "kalpBtn"  -> puan >= 50;
                    default -> true;
                };
                btn.setDisable(!aktif);
                btn.setOpacity(aktif ? 1.0 : 0.5);
                if (aktif) btn.setStyle(MKT_BTN_NORMAL);
            }
        };

        marketPanel.getChildren().forEach(dugum -> {
            guncelleBtn.accept(dugum);
            if (dugum instanceof GridPane gp)
                gp.getChildren().forEach(guncelleBtn::accept);
        });
    }

    // =========================================================================
    //  Input handling
    // =========================================================================

    /** Returns false if a click should be accepted. */
    private boolean oyunAktifDegil() {
        if (leblebModu && leblebiBoardMode != null)
            return leblebiBoardMode.isOyunBitti() || leblebiBoardMode.isKazanildi();
        return klasikBoardMode == null || !klasikBoardMode.isOyunAktif();
    }

    private void hucreAc(int s, int u) {
        if (leblebModu && leblebiBoardMode != null) {
            // ── Leblebi mode ──────────────────────────────────────────────────
            if (leblebiBoardMode.isZirayiIlacAktif()) {
                Board tahta = leblebiBoardMode.getTahta();
                int oncekiAcik = acikHucreSay(tahta);
                tahta.zirayiIlacUygula(s, u);
                int kazanilanPuan = acikHucreSay(tahta) - oncekiAcik;
                if (kazanilanPuan > 0) {
                    leblebiBoardMode.puanEkle(kazanilanPuan);
                    puanEtiketi.setText("🫘 " + leblebiBoardMode.getLeblebPuani() + " puan");
                }
                leblebiBoardMode.zirayiIlacKullanildi();
                sesCal(sesKazma);
                return;
            }

            Board tahta = leblebiBoardMode.getTahta();
            int oncekiAcik = acikHucreSay(tahta);
            boolean mineHit = leblebiBoardMode.hucreAc(s, u);

            if (mineHit) {
                sesCal(sesPatlama);
                canEtiketi.setText("❤ x" + leblebiBoardMode.getCanSayisi());
                if (!leblebiBoardMode.isOyunBitti()) {
                    zamanlayici.pause();
                    yilanUyarisiGoster(s, u);
                }
            } else {
                sesCal(sesKazma);
                int kazanilan = acikHucreSay(tahta) - oncekiAcik;
                if (kazanilan > 0) {
                    leblebiBoardMode.puanEkle(kazanilan);
                    puanEtiketi.setText("🫘 " + leblebiBoardMode.getLeblebPuani() + " puan");
                }
            }

        } else if (klasikBoardMode != null) {
            // ── Classic mode ──────────────────────────────────────────────────
            Board tahta = klasikBoardMode.getTahta();
            boolean solucanMiydi = tahta.getHucre(s, u).isMayinMi();
            klasikBoardMode.hucreAc(s, u);
            sesCal(solucanMiydi ? sesPatlama : sesKazma);
        }
    }

    private void isaretKoy(int s, int u) {
        Board tahta = aktifTahta();
        if (tahta == null) return;
        Cell hucre = tahta.getHucre(s, u);
        if (hucre.isAcildiMi()) return;

        boolean isaretliydi = hucre.isIsaretlendi();
        if (!isaretliydi && yerlestirilenIsaret >= mayinSayisi) return;

        hucre.isaretiDegistir();
        yerlestirilenIsaret += isaretliydi ? -1 : 1;
        yerlestirilenIsaret  = Math.max(0, yerlestirilenIsaret);

        String simge = leblebModu ? "🐍 " : "💣 ";
        maynSayaciEtiketi.setText(simge + (mayinSayisi - yerlestirilenIsaret));
    }

    /** Returns the Board for whichever mode is currently active. */
    private Board aktifTahta() {
        if (leblebModu && leblebiBoardMode != null) return leblebiBoardMode.getTahta();
        if (klasikBoardMode != null)                return klasikBoardMode.getTahta();
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
    //  Timer
    // =========================================================================

    private void zamanlayiciBaslat() {
        if (zamanlayici != null) zamanlayici.stop();

        zamanlayici = new Timeline(new KeyFrame(Duration.seconds(1), olay -> {
            if (leblebModu && leblebiBoardMode != null) {
                // ── Leblebi tick ──────────────────────────────────────────────
                leblebiBoardMode.sureyiGuncelle(1);
                int kalan = leblebiBoardMode.getKalanSure();
                zamanlayiciEtiketi.setText("⏳ " + kalan + "s");
                boolean flash = (kalan % 2 == 0);
                zamanlayiciEtiketi.setStyle(
                    "-fx-font-size: 19px; -fx-font-weight: bold; -fx-padding: 4 12 4 12;" +
                    "-fx-background-radius: 8; -fx-text-fill: " +
                    (kalan <= 10 ? (flash ? "#e74c3c" : "#ffffff") : "#cdd6f4") + ";");
                if (leblebiBoardMode.isOyunBitti()) {
                    zamanlayici.stop();
                    arayuzuGuncelle();
                }

            } else if (klasikBoardMode != null) {
                if (klasikBoardMode.isGeriSayim()) {
                    // ── Classic countdown tick ────────────────────────────────
                    boolean timesUp = klasikBoardMode.sureyiGuncelle(1);
                    int kalan = klasikBoardMode.getSuruclukSure();
                    zamanlayiciEtiketi.setText("⏳ " + kalan + "s");
                    boolean flash = (kalan % 2 == 0);
                    zamanlayiciEtiketi.setStyle(
                        "-fx-font-size: 19px; -fx-font-weight: bold; -fx-padding: 4 12 4 12;" +
                        "-fx-background-radius: 8; -fx-text-fill: " +
                        (kalan <= 10 ? (flash ? "#e74c3c" : "#ffffff") : "#cdd6f4") + ";");
                    if (timesUp) {
                        zamanlayici.stop();
                        durumEtiketi.setText("⏰ Süre Doldu! Tüm Mayınlar Patladı!");
                        durumEtiketi.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;" +
                            "-fx-text-fill: #f38ba8;");
                        sifirlaBtn.setText("😵");
                        sesCal(sesPatlama);
                        arayuzuGuncelle();
                        if (dugmeler != null)
                            for (Button[] satir : dugmeler)
                                for (Button btn : satir) btn.setDisable(true);
                    }
                } else {
                    // ── Classic stopwatch tick ────────────────────────────────
                    klasikBoardMode.sureyiGuncelle(1);
                    zamanlayiciEtiketi.setText("⏱ " + klasikBoardMode.getSuruclukSure() + "s");
                }
            }
        }));
        zamanlayici.setCycleCount(Animation.INDEFINITE);
        zamanlayici.play();
    }

    // =========================================================================
    //  Reset
    // =========================================================================

    private void oyunuSifirla() {
        if (zamanlayici != null) zamanlayici.stop();
        yerlestirilenIsaret = 0;
        yilanHucreleri.clear();
        durumEtiketi.setText("");

        String hudLabelStil = "-fx-font-size: 19px; -fx-font-weight: bold; -fx-padding: 4 12 4 12;" +
                              "-fx-background-radius: 8;";
        String hudPuanStil  = "-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #f0c040;" +
                              "-fx-padding: 4 12 4 12; -fx-background-radius: 8;";
        zamanlayiciEtiketi.setStyle(hudLabelStil);
        puanEtiketi.setStyle(hudPuanStil);
        canEtiketi.setStyle(hudLabelStil + "-fx-text-fill: #ff6b6b;");
        durumEtiketi.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");
        sifirlaBtn.setText(leblebModu ? "🌾" : "😊");

        if (leblebModu) {
            leblebiBoardMode = new LeblebiBoardMode(
                satirSayisi, sutunSayisi, mayinSayisi,
                Seviye.getSeviye(mevcutSeviye).getSureSaniye(), KLASIK_CAN
            );
        } else {
            // Reconstruct KlasikBoardMode with the same settings
            klasikBoardMode = new KlasikBoardMode(
                satirSayisi, sutunSayisi, mayinSayisi,
                klasikBoardMode.isGeriSayim(),
                klasikBoardMode.getBaslangicSuresi()
            );
        }

        guncelleUstBar();
        izgarayiOlustur();

        if (leblebModu) {
            marketPanelOlustur();
            kokDuzen.setRight(marketPanel);
            marketPanelGuncelle();
        } else {
            kokDuzen.setRight(null);
        }

        temayiUygula();
        zamanlayiciBaslat();
        arayuzuGuncelle();
        hucreBoyutlariniGuncelle();
    }

    // =========================================================================
    //  UI update loop
    // =========================================================================

    private void arayuzuGuncelle() {
        Board tahta = aktifTahta();
        if (tahta == null || dugmeler == null) return;

        String[] sayiRenk = leblebModu ? LB_SAYI_RENK
                          : (karanlikTema ? KT_SAYI_RENK : AT_SAYI_RENK);

        int[] kargaKonum = (leblebModu && leblebiBoardMode != null)
            ? leblebiBoardMode.getKargaGosterilenMayin() : null;

        for (int s = 0; s < satirSayisi; s++) {
            for (int u = 0; u < sutunSayisi; u++) {
                Cell    hucre     = tahta.getHucre(s, u);
                boolean kargaHedef = kargaKonum != null
                    && kargaKonum[0] == s && kargaKonum[1] == u
                    && !hucre.isAcildiMi();
                boolean yilanY    = leblebModu && yilanHucreleri.contains(s * sutunSayisi + u);

                byte nyDurum;
                if      (yilanY)                                    nyDurum = 5;
                else if (hucre.isAcildiMi() && hucre.isMayinMi())  nyDurum = 3;
                else if (hucre.isAcildiMi())                        nyDurum = (byte)(10 + hucre.getKomsuMayinSayisi());
                else if (hucre.isIsaretlendi())                     nyDurum = 1;
                else if (kargaHedef)                                nyDurum = 4;
                else                                                nyDurum = 0;

                if (nyDurum == hucreDurum[s][u]) continue;
                hucreDurum[s][u] = nyDurum;

                Button btn = dugmeler[s][u];

                if (nyDurum == 3) {
                    String mineEmoji = leblebModu ? "🐍" : "💣";
                    Label mineL = new Label(mineEmoji);
                    mineL.setStyle("-fx-font-size:20px;");
                    btn.setText(""); btn.setGraphic(mineL);
                    btn.setStyle(mayinHucreTarzi());
                    btn.setDisable(true);
                } else if (nyDurum >= 10) {
                    btn.setGraphic(null);
                    int k = hucre.getKomsuMayinSayisi();
                    btn.setText(k == 0 ? "" : String.valueOf(k));
                    btn.setStyle(acilmisHucreTarzi(k, sayiRenk));
                    btn.setDisable(true);
                } else if (nyDurum == 1) {
                    Label flagL = new Label("🚩");
                    flagL.setStyle("-fx-font-size:20px;");
                    btn.setText(""); btn.setGraphic(flagL);
                    btn.setStyle(isaretliHucreTarzi());
                    btn.setDisable(false);
                } else if (nyDurum == 4) {
                    btn.setGraphic(null); btn.setText("");
                    btn.setStyle(acilmamisHucreTarzi() +
                        "-fx-border-color: " + LB_KARGA_RENK + "; -fx-border-width: 3;");
                    btn.setDisable(false);
                } else if (nyDurum == 5) {
                    Label snakeL = new Label("🐍");
                    snakeL.setStyle("-fx-font-size:20px;");
                    btn.setText(""); btn.setGraphic(snakeL);
                    btn.setStyle(
                        "-fx-background-color: #8B0000;" +
                        "-fx-border-color: #ff4444; -fx-border-width: 2;" +
                        "-fx-background-radius: 3; -fx-border-radius: 3; -fx-padding: 0;"
                    );
                    btn.setDisable(false);
                } else {
                    btn.setGraphic(null); btn.setText("");
                    btn.setStyle(acilmamisHucreTarzi());
                    btn.setDisable(false);
                }
            }
        }

        kontrolEt();
    }

    // ── Win / loss checks ─────────────────────────────────────────────────────

    private void kontrolEt() {
        if (leblebModu && leblebiBoardMode != null) {
            leblebiBoardMode.kazanmaKontrol();

            if (leblebiBoardMode.isKazanildi()) {
                zamanlayici.stop();
                sesCal(sesKazan);
                sifirlaBtn.setText("🎉");
                durumEtiketi.setText("🫘 Tüm yılanlar bulundu!");
                durumEtiketi.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #c89a2a;");
                toplamLeblebPuani += leblebiBoardMode.getLeblebPuani();
                javafx.application.Platform.runLater(this::seviyeGecisiniGoster);

            } else if (leblebiBoardMode.isOyunBitti()) {
                zamanlayici.stop();
                sifirlaBtn.setText("🪱");
                String msg = leblebiBoardMode.getKalanSure() <= 0
                    ? "⏰ Süre doldu! Mehmet Emmi üzüldü..."
                    : "💀 Canların bitti! Yılanlar kazandı!";
                durumEtiketi.setText(msg);
                durumEtiketi.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #e74c3c;");
                javafx.application.Platform.runLater(this::oyunSonuPopupuGoster);
            }

        } else if (klasikBoardMode != null) {
            if (klasikBoardMode.isKazanildi()) {
                zamanlayici.stop();
                sesCal(sesKazan);
                sifirlaBtn.setText("😎");
                String winMsg = klasikBoardMode.isGeriSayim()
                    ? "★ Kazandınız! (" + klasikBoardMode.getSuruclukSure() + "s kaldı)"
                    : "★ Kazandınız! (" + klasikBoardMode.getSuruclukSure() + "s)";
                durumEtiketi.setText(winMsg);
                durumEtiketi.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;" +
                    "-fx-text-fill: " + (karanlikTema ? "#a6e3a1" : "#2e7d32") + ";");
                if (klasikBoardMode.isGeriSayim())
                    javafx.application.Platform.runLater(this::klasikZamanliSkorKaydet);

            } else if (klasikBoardMode.isOyunBitti() && !klasikBoardMode.isSureDoldu()) {
                // Mine hit (countdown handles its own UI in the timer tick)
                zamanlayici.stop();
                sesCal(sesPatlama);
                sifirlaBtn.setText("😵");
                durumEtiketi.setText("✖ Oyun Bitti!");
                durumEtiketi.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;" +
                    "-fx-text-fill: " + (karanlikTema ? "#f38ba8" : "#c62828") + ";");
            }
        }
    }

    // ── Level transition (Leblebi) ────────────────────────────────────────────

    private void seviyeGecisiniGoster() {
        boolean sonSeviye = Seviye.sonSeviyeMi(mevcutSeviye);
        Seviye  seviye    = Seviye.getSeviye(mevcutSeviye);

        Alert dialog = new Alert(Alert.AlertType.INFORMATION);
        dialog.setTitle("Seviye Tamamlandı!");
        dialog.getButtonTypes().clear();

        String baslik, icerik;
        if (sonSeviye) {
            baslik = "🏆 TÜM BÖLÜMLER TAMAMLANDI!";
            icerik = String.format(
                "Mehmet Emmi son derece memnun!\n\n" +
                "Toplam Leblebi Puanı: %d\n" +
                "Kalan Süre: %d saniye\n\n" +
                "İsminizi skor tablosuna ekleyelim mi?",
                toplamLeblebPuani, leblebiBoardMode.getKalanSure()
            );
            dialog.getButtonTypes().addAll(
                new ButtonType("🏅 Skoru Kaydet", ButtonBar.ButtonData.OK_DONE),
                new ButtonType("Menüye Dön",       ButtonBar.ButtonData.CANCEL_CLOSE)
            );
        } else {
            String sonrakiIsim = Seviye.getSeviye(mevcutSeviye + 1).getIsim();
            baslik = "✅ Seviye " + mevcutSeviye + " Tamamlandı!";
            icerik = String.format(
                "%s temizlendi!\n\n" +
                "Bu bölümde kazanılan puan: %d\n" +
                "Toplam puan: %d\n\n" +
                "Sıradaki bölüm: %s",
                seviye.getIsim(), leblebiBoardMode.getLeblebPuani(),
                toplamLeblebPuani, sonrakiIsim
            );
            dialog.getButtonTypes().addAll(
                new ButtonType("▶ Sonraki Bölüm", ButtonBar.ButtonData.OK_DONE),
                new ButtonType("Menüye Dön",       ButtonBar.ButtonData.CANCEL_CLOSE)
            );
        }

        dialog.setHeaderText(baslik);
        dialog.setContentText(icerik);
        dialogStilUygula(dialog, true);

        dialog.showAndWait().ifPresent(cevap -> {
            if (cevap.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                if (sonSeviye) oyunSonuPopupuGoster();
                else { mevcutSeviye++; leblebOyunuBaslat(); }
            } else {
                menuGoster();
            }
        });
    }

    // ── Game-over / score popups ──────────────────────────────────────────────

    private boolean popupGosterildi = false;

    private void oyunSonuPopupuGoster() {
        if (popupGosterildi) return;
        popupGosterildi = true;

        int finalSkor = (leblebiBoardMode != null)
            ? toplamLeblebPuani * 10 + leblebiBoardMode.getKalanSure() * 5
            : (klasikBoardMode != null ? klasikBoardMode.getSuruclukSure() : 0);

        TextInputDialog isimDialog = new TextInputDialog("Tarımcı");
        isimDialog.setTitle("Skor Tablosu");
        isimDialog.setHeaderText("🏆 Oyun Bitti!");
        isimDialog.setContentText(String.format("Skor: %d\nİsminizi girin:", finalSkor));
        dialogStilUygula(isimDialog, false);

        isimDialog.showAndWait().ifPresent(isim -> {
            if (!isim.isBlank())
                SkorTablosu.kaydet(isim.trim(), finalSkor, mevcutSeviye);
        });

        popupGosterildi = false;
        menuGoster();
    }

    private void klasikZamanliSkorKaydet() {
        if (klasikBoardMode == null || klasikBoardMode.isSkorKaydedildi()) return;
        klasikBoardMode.setSkorKaydedildi(true);

        int    skor       = klasikBoardMode.skorHesapla();
        int    kalanSure  = klasikBoardMode.getSuruclukSure();
        int    gecenSure  = klasikBoardMode.getBaslangicSuresi() - kalanSure;
        double yogunluk   = (double) mayinSayisi / (satirSayisi * sutunSayisi) * 100;
        String presetEtiket = klasikBoardMode.presetEtiketiBul();
        String seviyeGoster = presetEtiket.isBlank()
            ? satirSayisi + "×" + sutunSayisi + ", " + mayinSayisi + " mayın"
            : presetEtiket;

        TextInputDialog dlg = new TextInputDialog("Oyuncu");
        dlg.setTitle("Skor Tablosu");
        dlg.setHeaderText("⏱ Tebrikler! Zamanlı Modu Kazandınız!");
        dlg.setContentText(String.format(
            "Zorluk: %s%n" +
            "Izgara: %d×%d  |  Mayın: %d  |  Süre limiti: %ds%n" +
            "Geçen süre: %ds  |  Kalan süre: %ds%n" +
            "Mayın yoğunluğu: %.0f%%%n%n" +
            "🏆 Skor: %d%n%nİsminizi girin:",
            seviyeGoster,
            satirSayisi, sutunSayisi, mayinSayisi, klasikBoardMode.getBaslangicSuresi(),
            gecenSure, kalanSure, yogunluk, skor
        ));
        dialogStilUygula(dlg, false);

        dlg.showAndWait().ifPresent(isim -> {
            if (!isim.isBlank())
                SkorTablosu.kaydet(isim.trim(), skor, 0, SkorTablosu.MOD_ZAMANLI,
                    satirSayisi, sutunSayisi, mayinSayisi, klasikBoardMode.getBaslangicSuresi());
        });

        klasikBoardMode.setSkorKaydedildi(false);
    }

    // ── Yılan warning dialog ──────────────────────────────────────────────────

    private void yilanUyarisiGoster(int satir, int sutun) {
        yilanHucreleri.add(satir * sutunSayisi + sutun);

        Alert uyari = new Alert(Alert.AlertType.WARNING);
        uyari.setTitle("Yılan!");
        uyari.setHeaderText("🐍 Yılana Bastın!");
        uyari.setContentText(
            "Dikkatli ol! Yılan seni ısırdı.\n" +
            "Canın gitti! ❤ x" + leblebiBoardMode.getCanSayisi() + " kaldı.\n\n" +
            "O hücre işaretlendi (🐍)."
        );
        dialogStilUygula(uyari, true);
        Optional<ButtonType> result = uyari.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) zamanlayici.play();
    }

    // =========================================================================
    //  Score table
    // =========================================================================

    private void skorTablosunuGoster() {
        Stage pencere2 = new Stage();
        pencere2.setTitle("Skor Tablosu");
        pencere2.initModality(Modality.APPLICATION_MODAL);
        pencere2.initOwner(pencere);

        String arka = "#1e1e2e", tabloBg = "#181825";

        TabPane sekmeler = new TabPane();
        sekmeler.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        sekmeler.setStyle("-fx-background-color:" + arka + ";-fx-tab-min-width:160px;");

        Tab zamanlıTab = new Tab("⏱ Klasik (Zamanlı)");
        zamanlıTab.setContent(zamanlıSkorTabloIcerik(
            SkorTablosu.yukle(SkorTablosu.MOD_ZAMANLI), arka, tabloBg, pencere2
        ));
        sekmeler.getTabs().add(zamanlıTab);

        if (leblebAcildi) {
            Tab leblebTab = new Tab("🫘 Leblebi Tarlası");
            leblebTab.setContent(skorTabloIcerik(
                SkorTablosu.yukle(SkorTablosu.MOD_LEBLEBI),
                new String[]{"#", "İsim", "Skor", "Seviye", "Tarih"},
                (g, i) -> new String[]{
                    (i + 1) + ".",
                    g.isim() == null || g.isim().isBlank() ? "—" : g.isim(),
                    String.valueOf(g.skor()),
                    g.seviye() <= 0 ? "—" : "Seviye " + g.seviye(),
                    g.tarih() == null || g.tarih().isBlank() ? "—" : g.tarih()
                },
                "Henüz kayıt yok.\nLeblebi Tarlası'nı oyna ve adını yazdır!",
                arka, tabloBg
            ));
            sekmeler.getTabs().add(leblebTab);
        }

        Button kapat = new Button("Kapat");
        kapat.setStyle(butonTarzi() + "-fx-background-color:#313244;-fx-text-fill:#cdd6f4;");
        kapat.setOnAction(e -> pencere2.close());

        Label ipucu = new Label("💡 Bir satıra tıklayarak ayrıntıları görebilirsin");
        ipucu.setStyle("-fx-text-fill:#6c7086;-fx-font-size:11px;");

        VBox kok = new VBox(8, sekmeler, ipucu, kapat);
        kok.setPadding(new Insets(16));
        kok.setAlignment(Pos.TOP_CENTER);
        kok.setStyle("-fx-background-color:" + arka + ";");
        VBox.setVgrow(sekmeler, Priority.ALWAYS);

        Scene s = new Scene(kok, 640, 520);
        s.setFill(javafx.scene.paint.Color.web(arka));
        globalCssUygula(s);
        pencere2.setScene(s);
        pencere2.showAndWait();
    }

    private javafx.scene.Node zamanlıSkorTabloIcerik(
            java.util.List<SkorTablosu.SkorGirisi> liste,
            String arka, String tabloBg, Stage owner) {

        GridPane tablo = new GridPane();
        tablo.setHgap(18);
        tablo.setVgap(8);
        tablo.setStyle("-fx-background-color:" + tabloBg + ";-fx-padding:16;-fx-background-radius:10;");
        tablo.setAlignment(Pos.CENTER);

        String[] basliklar = {"#", "İsim", "Skor", "Zorluk", "Tarih"};
        for (int i = 0; i < basliklar.length; i++) {
            Label lbl = new Label(basliklar[i]);
            lbl.setStyle("-fx-font-weight:bold;-fx-text-fill:#89b4fa;-fx-font-size:13px;");
            tablo.add(lbl, i, 0);
        }

        if (liste.isEmpty()) {
            Label bos = new Label("Henüz kayıt yok.\nZamanlı modda oyna ve adını yazdır!");
            bos.setStyle("-fx-text-fill:#6c7086;-fx-font-size:13px;");
            bos.setWrapText(true);
            tablo.add(bos, 0, 1, basliklar.length, 1);
        } else {
            for (int i = 0; i < Math.min(liste.size(), 20); i++) {
                SkorTablosu.SkorGirisi g = liste.get(i);
                String renk = i == 0 ? "#f1c40f" : i == 1 ? "#95a5a6" : i == 2 ? "#e67e22" : "#cdd6f4";
                String zorlukEtiketi = presetEtiketiAl(g);
                String[] degerler = {
                    (i + 1) + ".",
                    g.isim() == null || g.isim().isBlank() ? "—" : g.isim(),
                    String.valueOf(g.skor()),
                    zorlukEtiketi,
                    g.tarih() == null || g.tarih().isBlank() ? "—" : g.tarih()
                };
                final int rowIdx = i;
                final SkorTablosu.SkorGirisi girdi = g;
                for (int j = 0; j < degerler.length; j++) {
                    Label lbl = new Label(degerler[j]);
                    lbl.setStyle("-fx-text-fill:" + renk + ";-fx-font-size:12px;-fx-cursor:hand;");
                    lbl.setOnMouseEntered(e -> lbl.setStyle(lbl.getStyle().replace("-fx-cursor:hand;",
                        "-fx-cursor:hand;-fx-underline:true;")));
                    lbl.setOnMouseExited(e -> lbl.setStyle(lbl.getStyle().replace("-fx-underline:true;", "")));
                    lbl.setOnMouseClicked(e -> zamanlıDetayGoster(girdi, rowIdx, owner));
                    tablo.add(lbl, j, i + 1);
                }
            }
        }
        return scrollSar(tablo, arka);
    }

    /** Returns a human-readable difficulty label for a score entry. */
    private String presetEtiketiAl(SkorTablosu.SkorGirisi g) {
        if (g.satirSayisi() > 0) {
            for (KlasikBoardMode.KlasikAyar p : KlasikBoardMode.PRESETLER) {
                if (p.geriSayim()
                        && p.satir() == g.satirSayisi()
                        && p.sutun() == g.sutunSayisi()
                        && p.mayin() == g.mayinSayisi()
                        && p.sure()  == g.sureSiniri()) {
                    String e = p.etiket();
                    int spaceIdx = e.indexOf(' ');
                    return spaceIdx >= 0 ? e.substring(spaceIdx + 1) : e;
                }
            }
            double yogunluk = (double) g.mayinSayisi() / (g.satirSayisi() * g.sutunSayisi()) * 100;
            return String.format("%d×%d / %d💣 / %.0f%%",
                g.satirSayisi(), g.sutunSayisi(), g.mayinSayisi(), yogunluk);
        }
        return "—";
    }

    private void zamanlıDetayGoster(SkorTablosu.SkorGirisi g, int sira, Stage owner) {
        Alert dlg = new Alert(Alert.AlertType.INFORMATION);
        dlg.initOwner(owner);
        dlg.setTitle("Skor Detayı");
        dlg.setHeaderText(String.format("#%d — %s", sira + 1, g.isim()));

        String gridBilgi;
        if (g.satirSayisi() > 0) {
            int    toplamHucre = g.satirSayisi() * g.sutunSayisi();
            double yogunluk    = (double) g.mayinSayisi() / toplamHucre * 100;
            gridBilgi = String.format(
                "Izgara       : %d satır × %d sütun (%d hücre)%n" +
                "Mayın sayısı : %d  (yoğunluk: %.1f%%)%n" +
                "Süre limiti  : %d saniye%n%n" +
                "🏆 Skor      : %d%n" +
                "📅 Tarih     : %s",
                g.satirSayisi(), g.sutunSayisi(), toplamHucre,
                g.mayinSayisi(), yogunluk, g.sureSiniri(),
                g.skor(),
                g.tarih() == null || g.tarih().isBlank() ? "—" : g.tarih()
            );
        } else {
            gridBilgi = String.format(
                "🏆 Skor  : %d%n📅 Tarih : %s%n%n" +
                "(Bu kayıt eski sürümde oluşturuldu — ızgara bilgisi mevcut değil)",
                g.skor(),
                g.tarih() == null || g.tarih().isBlank() ? "—" : g.tarih()
            );
        }

        dlg.setContentText(gridBilgi);
        dialogStilUygula(dlg, false);
        dlg.showAndWait();
    }

    private javafx.scene.Node skorTabloIcerik(
            java.util.List<SkorTablosu.SkorGirisi> liste,
            String[] basliklar,
            java.util.function.BiFunction<SkorTablosu.SkorGirisi, Integer, String[]> satirUret,
            String bosMsg,
            String arka, String tabloBg) {

        GridPane tablo = new GridPane();
        tablo.setHgap(22);
        tablo.setVgap(8);
        tablo.setStyle("-fx-background-color:" + tabloBg + ";-fx-padding:16;-fx-background-radius:10;");
        tablo.setAlignment(Pos.CENTER);

        for (int i = 0; i < basliklar.length; i++) {
            Label lbl = new Label(basliklar[i]);
            lbl.setStyle("-fx-font-weight:bold;-fx-text-fill:#89b4fa;-fx-font-size:13px;");
            tablo.add(lbl, i, 0);
        }

        if (liste.isEmpty()) {
            Label bos = new Label(bosMsg);
            bos.setStyle("-fx-text-fill:#6c7086;-fx-font-size:13px;");
            bos.setWrapText(true);
            tablo.add(bos, 0, 1, basliklar.length, 1);
        } else {
            for (int i = 0; i < Math.min(liste.size(), 20); i++) {
                SkorTablosu.SkorGirisi g = liste.get(i);
                String[] degerler = satirUret.apply(g, i);
                String renk = i == 0 ? "#f1c40f" : i == 1 ? "#95a5a6" : i == 2 ? "#e67e22" : "#cdd6f4";
                for (int j = 0; j < degerler.length; j++) {
                    Label lbl = new Label(degerler[j]);
                    lbl.setStyle("-fx-text-fill:" + renk + ";-fx-font-size:12px;");
                    tablo.add(lbl, j, i + 1);
                }
            }
        }
        return scrollSar(tablo, arka);
    }

    /** Wraps a node in a styled ScrollPane + VBox. */
    private javafx.scene.Node scrollSar(javafx.scene.Node icerik, String arka) {
        ScrollPane scroll = new ScrollPane(icerik);
        scroll.setStyle("-fx-background-color:" + arka + ";-fx-background:" + arka + ";");
        scroll.setFitToWidth(true);
        scroll.viewportBoundsProperty().addListener((obs, ov, nv) -> {
            javafx.scene.Node vp = scroll.lookup(".viewport");
            if (vp != null) vp.setStyle("-fx-background-color:" + arka + ";");
        });
        VBox wrapper = new VBox(scroll);
        wrapper.setStyle("-fx-background-color:" + arka + ";-fx-padding:12;");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return wrapper;
    }

    // =========================================================================
    //  Cell sizing & styles
    // =========================================================================

    private void hucreBoyutlariniGuncelle() {
        Board tahta = aktifTahta();
        if (dugmeler == null || tahta == null) return;
        double marketGenislik = (leblebModu && marketPanel != null) ? 210 : 0;
        double kullanilabilirG = sahne.getWidth()  - 52 - marketGenislik;
        double kullanilabilirY = sahne.getHeight() - 140;
        double hg  = Math.floor(kullanilabilirG / sutunSayisi);
        double hy  = Math.floor(kullanilabilirY / satirSayisi);
        double boy = Math.max(28, Math.min(hg, hy));
        double yaz = Math.max(9, boy * 0.28);

        String[] sayiRenk = leblebModu ? LB_SAYI_RENK
                          : (karanlikTema ? KT_SAYI_RENK : AT_SAYI_RENK);

        for (int s = 0; s < satirSayisi; s++)
            for (int u = 0; u < sutunSayisi; u++) {
                Button btn = dugmeler[s][u];
                btn.setPrefSize(boy, boy);
                btn.setMinSize(boy, boy);
                btn.setMaxSize(boy, boy);
                Cell h = tahta.getHucre(s, u);
                if (h.isAcildiMi() && !h.isMayinMi()) {
                    int k = h.getKomsuMayinSayisi();
                    btn.setStyle(acilmisHucreTarzi(k, sayiRenk, yaz));
                }
            }
    }

    private void tumHucreleriYenidenCiz() {
        if (hucreDurum != null)
            for (byte[] row : hucreDurum) java.util.Arrays.fill(row, (byte) -1);
        arayuzuGuncelle();
    }

    private void temayiUygula() {
        String arka  = leblebModu ? LB_ARKAPLAN  : (karanlikTema ? KT_ARKAPLAN  : AT_ARKAPLAN);
        String ustBr = leblebModu ? LB_UST_BAR   : (karanlikTema ? KT_UST_BAR   : AT_UST_BAR);
        String yazi  = leblebModu ? "#f5e6b0"    : (karanlikTema ? KT_YAZI      : AT_YAZI);
        String cerc  = leblebModu ? LB_CERCEVE   : (karanlikTema ? KT_CERCEVE   : AT_CERCEVE);

        kokDuzen.setStyle("-fx-background-color:" + arka + ";");

        if (kokDuzen.getTop() instanceof HBox bar) {
            bar.setStyle(
                "-fx-background-color:" + ustBr + ";" +
                "-fx-background-radius:10;-fx-padding:10 16 10 16;" +
                "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.35),8,0,0,3);"
            );
            bar.getChildren().forEach(n -> {
                if (n instanceof Label l)
                    l.setStyle(l.getStyle() + "-fx-text-fill:" + yazi + ";");
                else if (n instanceof Button b)
                    b.setStyle(b.getStyle() +
                        "-fx-background-color:" + (karanlikTema && !leblebModu ? "#313244" : (leblebModu ? "#7a5200" : "#c8cdd8")) + ";" +
                        "-fx-text-fill:" + yazi + ";-fx-border-color:" + cerc + ";"
                    );
            });
        }

        if (izgaraDuzen != null)
            izgaraDuzen.setStyle("-fx-background-color:" + cerc + ";-fx-padding:2;");
    }

    // ── Cell style helpers ────────────────────────────────────────────────────

    private String acilmamisHucreTarzi() {
        if (leblebModu)
            return "-fx-background-color:" + LB_ACILMAMIS + ";" +
                   "-fx-border-color: #e8c55a #7a5500 #7a5500 #e8c55a;" +
                   "-fx-border-width:2;-fx-background-radius:3;-fx-border-radius:3;" +
                   "-fx-padding:0;-fx-cursor:hand;";
        String bg = karanlikTema ? KT_ACILMAMIS : AT_ACILMAMIS;
        String br = karanlikTema ? KT_CERCEVE   : AT_CERCEVE;
        return "-fx-background-color:" + bg + ";-fx-border-color:" + br + ";" +
               "-fx-border-width:1;-fx-background-radius:3;-fx-border-radius:3;" +
               "-fx-padding:0;-fx-cursor:hand;";
    }

    private String acilmamisHucreHoverTarzi() {
        if (leblebModu)
            return "-fx-background-color:#d9aa3a;" +
                   "-fx-border-color: #f0d070 #8a6510 #8a6510 #f0d070;" +
                   "-fx-border-width:2;-fx-background-radius:3;-fx-border-radius:3;" +
                   "-fx-padding:0;-fx-cursor:hand;";
        String bg = karanlikTema ? "#5a5770" : "#d0d8e8";
        String br = karanlikTema ? KT_CERCEVE : AT_CERCEVE;
        return "-fx-background-color:" + bg + ";-fx-border-color:" + br + ";" +
               "-fx-border-width:1;-fx-background-radius:3;-fx-border-radius:3;" +
               "-fx-padding:0;-fx-cursor:hand;";
    }

    private String acilmisHucreTarzi(int k, String[] sr) { return acilmisHucreTarzi(k, sr, 14); }

    private String acilmisHucreTarzi(int k, String[] sr, double yaz) {
        String bg, br, fgSoluk;
        if (leblebModu) {
            bg      = LB_ACILMIS;
            br      = "#5c3200";
            fgSoluk = "#c8a060";
        } else {
            bg      = karanlikTema ? KT_ACILMIS   : AT_ACILMIS;
            br      = karanlikTema ? KT_CERCEVE   : AT_CERCEVE;
            fgSoluk = karanlikTema ? KT_YAZI_SOLUK: AT_YAZI_SOLUK;
        }
        String fg = (k > 0 && k <= 8) ? sr[k] : fgSoluk;
        return "-fx-background-color:" + bg + ";" +
               "-fx-border-color:" + br + ";-fx-border-width:1;" +
               "-fx-background-radius:3;-fx-border-radius:3;-fx-padding:0;" +
               "-fx-text-fill:" + fg + ";-fx-font-weight:bold;-fx-font-size:" + yaz + "px;";
    }

    private String isaretliHucreTarzi() {
        String bg = leblebModu ? LB_ISARETLI  : (karanlikTema ? KT_ISARETLI  : AT_ISARETLI);
        String br = leblebModu ? LB_CERCEVE   : (karanlikTema ? KT_CERCEVE   : AT_CERCEVE);
        String fg = leblebModu ? "#3d2800"    : (karanlikTema ? "#f38ba8"    : "#c62828");
        return "-fx-background-color:" + bg + ";-fx-border-color:" + br + ";" +
               "-fx-border-width:1;-fx-background-radius:3;-fx-border-radius:3;" +
               "-fx-padding:0;-fx-text-fill:" + fg + ";" +
               "-fx-font-weight:bold;-fx-font-size:14px;-fx-cursor:hand;";
    }

    private String mayinHucreTarzi() {
        String bg = leblebModu ? LB_SOLUCAN_RENK : "#8B0000";
        return "-fx-background-color:" + bg + ";-fx-border-width:1;" +
               "-fx-background-radius:3;-fx-border-radius:3;-fx-padding:0;" +
               "-fx-text-fill:#ffffff;-fx-font-weight:bold;-fx-font-size:16px;";
    }

    // =========================================================================
    //  Dialog styling
    // =========================================================================

    private void bilgilendirmeGoster(String baslik, String mesaj) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(baslik);
        a.setHeaderText(null);
        a.setContentText(mesaj);
        dialogStilUygula(a, false);
        a.showAndWait();
    }

    private void dialogStilUygula(Alert alert, boolean leblebi) {
        dialogStilUygulaTemel(alert.getDialogPane(), leblebi);
    }

    private void dialogStilUygula(TextInputDialog dialog, boolean leblebi) {
        dialogStilUygulaTemel(dialog.getDialogPane(), leblebi);
    }

    private void dialogStilUygulaTemel(javafx.scene.control.DialogPane pane, boolean leblebi) {
        String bg  = leblebi ? "#3d2800" : "#1e1e2e";
        String fg  = leblebi ? "#f5e6b0" : "#cdd6f4";
        String hbg = leblebi ? "#5c3a00" : "#181825";

        pane.setStyle(
            "-fx-background-color: " + bg + ";" +
            "-fx-font-size: 14px;" +
            "-fx-text-fill: " + fg + ";"
        );

        Runnable stilUygula = () -> {
            for (javafx.scene.Node n : pane.lookupAll(".label")) {
                if (n instanceof javafx.scene.control.Label lbl) {
                    String mevcut = lbl.getStyle() == null ? "" : lbl.getStyle();
                    if (!mevcut.contains("-fx-text-fill"))
                        lbl.setStyle(mevcut + "-fx-text-fill:" + fg + ";-fx-font-size:14px;");
                }
            }
            javafx.scene.Node headerNode = pane.lookup(".header-panel");
            if (headerNode != null) {
                headerNode.setStyle("-fx-background-color:" + hbg + ";");
                javafx.scene.Node headerLabel = pane.lookup(".header-panel .label");
                if (headerLabel instanceof javafx.scene.control.Label lbl)
                    lbl.setStyle("-fx-text-fill:" + fg + ";-fx-font-size:15px;-fx-font-weight:bold;");
            }
            javafx.scene.Node tf = pane.lookup(".text-field");
            if (tf instanceof javafx.scene.control.TextField field)
                field.setStyle("-fx-background-color:" + hbg + ";-fx-text-fill:" + fg + ";-fx-font-size:13px;");
            pane.getButtonTypes().forEach(bt -> {
                javafx.scene.Node btn = pane.lookupButton(bt);
                if (btn != null)
                    btn.setStyle(
                        "-fx-background-color:" + (leblebi ? "#7a5200" : "#313244") + ";" +
                        "-fx-text-fill:" + fg + ";" +
                        "-fx-font-size:13px;-fx-background-radius:6;-fx-padding:6 14 6 14;"
                    );
            });
        };

        stilUygula.run();
        javafx.application.Platform.runLater(stilUygula);
    }

    // =========================================================================
    //  Global CSS / font
    // =========================================================================

    private void globalCssUygula(Scene hedefSahne) {
        boolean fontYuklendi = false;
        try {
            java.net.URL fontUrl = getClass().getResource(FONT_YOL);
            if (fontUrl == null) fontUrl = new java.io.File(FONT_YOL).toURI().toURL();
            javafx.scene.text.Font yuklenenFont =
                javafx.scene.text.Font.loadFont(fontUrl.toExternalForm(), 14);
            fontYuklendi = (yuklenenFont != null);
        } catch (Exception ignored) {}

        String fontFamily = fontYuklendi ? "GameFont" : "System";
        String css =
            ".root { -fx-font-family: '" + fontFamily + "'; }" +
            ".label { -fx-font-family: '" + fontFamily + "'; }" +
            ".button { -fx-font-family: '" + fontFamily + "'; }" +
            ".text-field { -fx-font-family: '" + fontFamily + "'; }" +
            ".text-area { -fx-font-family: '" + fontFamily + "'; }" +
            ".dialog-pane .label { -fx-font-family: '" + fontFamily + "'; }" +
            ".dialog-pane .button { -fx-font-family: '" + fontFamily + "'; }";

        String encoded = java.util.Base64.getEncoder()
            .encodeToString(css.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        hedefSahne.getStylesheets().clear();
        hedefSahne.getStylesheets().add("data:text/css;base64," + encoded);
    }

    public static void main(String[] args) { launch(); }
}
