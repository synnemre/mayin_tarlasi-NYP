import javafx.animation.*;
import javafx.application.Application;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
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
 * MinesweeperApp — Mayın Tarlası + Mehmet Emmi'nin Leblebi Tarlası
 *
 * Özellikler:
 *   1. Ana Menü + Easter Egg (1837837 klavye kombinasyonu)
 *   2. ImageView altyapısı + ses efektleri (AudioClip)
 *   3. Market paneli (Karga, Emmi'nin Saati, Zirai İlaç, Ekstra Kalp)
 *   4. Bölüm/Level sistemi (3 seviye)
 *   5. Skor tablosu (scores.json)
 */
public class MinesweeperApp extends Application {

    // ── Sabit boyut (klasik mod) ──────────────────────────────────────────────
    private static final int KLASIK_SATIR  = 10;
    private static final int KLASIK_SUTUN  = 10;
    private static final int KLASIK_MAYIN  = 11;
    private static final int KLASIK_CAN    = 3;

    // ── Tema renkleri ─────────────────────────────────────────────────────────
    private static final String KT_ARKAPLAN  = "#1e1e2e";
    private static final String KT_ACILMAMIS = "#454158";
    private static final String KT_ACILMIS   = "#11111b";
    private static final String KT_ISARETLI  = "#524f6e";
    private static final String KT_MAYIN     = "#f38ba8";
    private static final String KT_CERCEVE   = "#6c7086";
    private static final String KT_YAZI      = "#cdd6f4";
    private static final String KT_YAZI_SOLUK= "#6c7086";
    private static final String KT_UST_BAR   = "#181825";

    private static final String AT_ARKAPLAN  = "#e8eaf0";
    private static final String AT_ACILMAMIS = "#c0c8d8";
    private static final String AT_ACILMIS   = "#f4f4f4";
    private static final String AT_ISARETLI  = "#b0b8cc";
    private static final String AT_MAYIN     = "#e57373";
    private static final String AT_CERCEVE   = "#9aa0b0";
    private static final String AT_YAZI      = "#1e1e2e";
    private static final String AT_YAZI_SOLUK= "#9aa0b0";
    private static final String AT_UST_BAR   = "#d0d4de";

    private static final String LB_ARKAPLAN     = "#3d2800";
    private static final String LB_ACILMAMIS    = "#c89a2a";
    private static final String LB_ACILMIS      = "#f5e6b0";
    private static final String LB_ISARETLI     = "#e8b84b";
    private static final String LB_SOLUCAN_RENK = "#4a7c2f";
    private static final String LB_CERCEVE      = "#a07020";
    private static final String LB_YAZI         = "#3d2800";
    private static final String LB_YAZI_SOLUK   = "#a07020";
    private static final String LB_UST_BAR      = "#5c3a00";
    private static final String LB_KARGA_RENK   = "#c0392b"; // Karga göstergesi

    private static final String[] KT_SAYI_RENK = {
        "", "#89b4fa","#a6e3a1","#f38ba8","#74c7ec","#fab387","#89dceb","#b4befe","#cdd6f4"};
    private static final String[] AT_SAYI_RENK = {
        "", "#1565c0","#2e7d32","#c62828","#0277bd","#e65100","#00838f","#6a1b9a","#37474f"};
    private static final String[] LB_SAYI_RENK = {
        "", "#5a3e00","#2e7d32","#b22222","#1a5276","#7d3c00","#1a6b4a","#5b2c6f","#3d2800"};

    // ── Uygulama durumu ───────────────────────────────────────────────────────
    private Stage pencere;
    private Scene anaSahne;
    private Scene menuSahne;

    private Board tahta;
    private LeblebiBoardMode leblebiBoardMode;
    private boolean leblebModu   = false;
    private boolean karanlikTema = true;

    // Seviye sistemi
    private int mevcutSeviye = 1;
    private int toplamLeblebPuani = 0; // Tüm seviyelerde biriken puan

    // Izgara
    private Button[][] dugmeler;
    private int satirSayisi, sutunSayisi, mayinSayisi;

    // Dirty-cell tracking: only repaint cells whose state changed
    // State encoding: 0=closed, 1=flagged, 2=opened-safe, 3=opened-mine, 4=karga-highlight
    private byte[][] hucreDurum;

    // Yılan basılan hücreler (Y harfi gösterilecek)
    private java.util.Set<Integer> yilanHucreleri = new java.util.HashSet<>();

    // UI Bileşenleri
    private Label maynSayaciEtiketi;
    private Label zamanlayiciEtiketi;
    private Label durumEtiketi;
    private Label canEtiketi;
    private Label puanEtiketi;
    private Button sifirlaBtn;
    private Timeline zamanlayici;
    private int yerlestirilenIsaret;
    private BorderPane kokDuzen;
    private GridPane  izgaraDuzen;
    private Scene     sahne;
    private VBox      marketPanel;

    // Easter Egg
    private static final String EASTER_EGG_KOD = "1837837";
    private StringBuilder basiliKodBuffer = new StringBuilder();
    private boolean leblebAcildi = false; // persists across menu visits

    // Ses altyapısı
    private AudioClip sesKazma;
    private AudioClip sesPatlama;
    private AudioClip sesButon;
    private AudioClip sesMarket;
    private AudioClip sesKazan;

    // Asset yolları
    private static final String ASSET_SOLUCAN  = "assets/solucan.png";
    private static final String ASSET_LEBLEBI  = "assets/leblebi.png";
    private static final String ASSET_BAYRAK   = "assets/bayrak.png";
    private static final String ASSET_MAYIN    = "assets/mayin.png";

    // Pre-loaded image cache — loaded once, reused everywhere
    private Image imgSolucan;
    private Image imgLeblebi;
    private Image imgBayrak;
    private Image imgMayin;

    // =========================================================================
    //  start()
    // =========================================================================

    @Override
    public void start(Stage pencere) {
        this.pencere = pencere;

        // Show the menu immediately — don't block the UI thread on I/O
        menuGoster();
        pencere.setTitle("Mayın Tarlası");
        pencere.setMinWidth(500);
        pencere.setMinHeight(400);
        pencere.setResizable(true);
        pencere.show();

        // Load sounds and images on a background thread so startup is instant.
        // Both are safe to construct off-thread; we only *play* or *display* them
        // on the FX thread later, which is fine.
        Thread yukleyici = new Thread(() -> {
            sesFxYukle();
            assetleriOnYukle();
        }, "asset-loader");
        yukleyici.setDaemon(true); // won't block JVM shutdown
        yukleyici.start();
    }

    // =========================================================================
    //  SES FX
    // =========================================================================

    private void sesFxYukle() {
        sesKazma  = sesFxYukleGuveli("sounds/kazma.mp3");
        sesPatlama= sesFxYukleGuveli("sounds/patlama.mp3");
        sesButon  = sesFxYukleGuveli("sounds/buton.mp3");
        sesMarket = sesFxYukleGuveli("sounds/market.mp3");
        sesKazan  = sesFxYukleGuveli("sounds/kazan.mp3");
    }

    private void assetleriOnYukle() {
        imgSolucan = assetImgYukle(ASSET_SOLUCAN, 20);
        imgLeblebi = assetImgYukle(ASSET_LEBLEBI, 20);
        imgBayrak  = assetImgYukle(ASSET_BAYRAK,  18);
        imgMayin   = assetImgYukle(ASSET_MAYIN,   20);
    }

    /** Loads an image once and returns it (no ImageView). Returns null if missing. */
    private Image assetImgYukle(String yol, double boyut) {
        try {
            java.net.URL url = getClass().getResource(yol);
            if (url == null) url = new java.io.File(yol).toURI().toURL();
            return new Image(url.toString(), boyut, boyut, true, true);
        } catch (Exception e) {
            return null;
        }
    }

    /** Creates a fresh ImageView from a cached Image (cheap — no disk I/O). */
    private ImageView cachedView(Image img) {
        return img != null ? new ImageView(img) : null;
    }

    private AudioClip sesFxYukleGuveli(String yol) {
        try {
            java.net.URL url = getClass().getResource(yol);
            if (url == null) url = new java.io.File(yol).toURI().toURL();
            return new AudioClip(url.toString());
        } catch (Exception e) {
            return null; // Ses dosyası yoksa sessizce devam et
        }
    }

    private void sesCal(AudioClip klip) {
        if (klip != null) klip.play();
    }

    // =========================================================================
    //  MENÜ
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

        Button skorBtn = menuButonOlustur("🏆  En İyi Tarımcılar", "#a6e3a1", "#1e1e2e");
        skorBtn.setOnAction(e -> {
            sesCal(sesButon);
            skorTablosunuGoster();
        });

        // Leblebi modu butonu — Easter Egg ile açılır, bir kez açıldı mı kalır
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

        // Easter Egg ipucu etiketi
        Label easterEggEtiketi = new Label(leblebAcildi ? "🫘 Mehmet Emmi'nin Leblebi Tarlası Modu Açık!" : "");
        easterEggEtiketi.setId("easterEggEtiket");
        easterEggEtiketi.setStyle("-fx-font-size: 13px; -fx-text-fill: #c89a2a; -fx-font-weight: bold;");

        kok.getChildren().addAll(baslik, klasikBtn, skorBtn, leblebBtn, easterEggEtiketi);

        menuSahne = new Scene(kok, 600, 500);

        // ── Easter Egg Klavye Dinleyicisi ──────────────────────────────────────
        menuSahne.setOnKeyPressed(olay -> {
            if (leblebAcildi) return; // already unlocked — ignore further input
            String k = olay.getText();
            if (!k.isEmpty() && "1234567890".contains(k)) {
                basiliKodBuffer.append(k);
                if (basiliKodBuffer.length() > EASTER_EGG_KOD.length())
                    basiliKodBuffer.delete(0, basiliKodBuffer.length() - EASTER_EGG_KOD.length());

                if (basiliKodBuffer.toString().equals(EASTER_EGG_KOD)) {
                    leblebAcildi = true;
                    basiliKodBuffer.setLength(0); // clear buffer — won't match again anyway
                    easterEggTetikle(kok, leblebBtn, easterEggEtiketi);
                }
            }
        });

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
        // Hover efekti
        btn.setOnMouseEntered(e -> btn.setOpacity(0.85));
        btn.setOnMouseExited(e -> btn.setOpacity(1.0));
        return btn;
    }

    // ── Easter Egg ────────────────────────────────────────────────────────────

    private void easterEggTetikle(VBox kok, Button leblebBtn, Label etiket) {
        // Sarsıntı animasyonu
        TranslateTransition sarsinti = new TranslateTransition(Duration.millis(60), kok);
        sarsinti.setByX(12);
        sarsinti.setCycleCount(8);
        sarsinti.setAutoReverse(true);

        // Renk flash
        Timeline flash = new Timeline(
            new KeyFrame(Duration.millis(0),   e -> kok.setStyle("-fx-background-color: #5c3a00;")),
            new KeyFrame(Duration.millis(150),  e -> kok.setStyle("-fx-background-color: #c89a2a;")),
            new KeyFrame(Duration.millis(300),  e -> kok.setStyle("-fx-background-color: #3d2800;")),
            new KeyFrame(Duration.millis(450),  e -> kok.setStyle("-fx-background-color: " + KT_ARKAPLAN + ";"))
        );

        sarsinti.play();
        flash.play();

        // Butonu göster
        leblebBtn.setVisible(true);
        leblebBtn.setManaged(true);

        // Uyarı mesajı
        etiket.setText("🫘 Mehmet Emmi'nin Leblebi Tarlası Modu Açıldı!");

        // Popup uyarısı
        Alert uyari = new Alert(Alert.AlertType.INFORMATION);
        uyari.setTitle("Gizli Mod!");
        uyari.setHeaderText("🫘 Leblebi Tarlası Modu Açıldı!");
        uyari.setContentText(
            "Mehmet Emmi çok mutlu!\n\n" +
            "Leblebi Tarlası modunu keşfettin.\n" +
            "Yılanları temizle, leblebi kazan, marketten güç al!"
        );
        uyari.getDialogPane().setStyle("-fx-background-color: #3d2800; -fx-font-size: 13px;");
        dialogStilUygula(uyari, true);
        uyari.showAndWait();
    }

    // =========================================================================
    //  KLASİK OYUN
    // =========================================================================

    private void klasikOyunuBaslat() {
        satirSayisi  = KLASIK_SATIR;
        sutunSayisi  = KLASIK_SUTUN;
        mayinSayisi  = KLASIK_MAYIN;
        leblebModu   = false;
        leblebiBoardMode = null;
        tahta = new Board(satirSayisi, sutunSayisi, mayinSayisi);
        oyunSahnesiniBaSlat(false);
    }

    // =========================================================================
    //  LEBLEBİ OYUNU
    // =========================================================================

    private void leblebOyunuBaslat() {
        yilanHucreleri.clear();
        Seviye seviye = Seviye.getSeviye(mevcutSeviye);
        satirSayisi  = seviye.satirSayisi;
        sutunSayisi  = seviye.sutunSayisi;
        mayinSayisi  = seviye.solucanSayisi;
        leblebiBoardMode = new LeblebiBoardMode(
            satirSayisi, sutunSayisi, mayinSayisi,
            seviye.sureSaniye, KLASIK_CAN
        );
        tahta = leblebiBoardMode.getTahta();
        oyunSahnesiniBaSlat(true);
    }

    // =========================================================================
    //  OYUN SAHNESİ KURULUMU
    // =========================================================================

    private void oyunSahnesiniBaSlat(boolean leblebi) {
        kokDuzen = new BorderPane();
        kokDuzen.setPadding(new Insets(12));

        ustBariOlustur();
        izgarayiOlustur();

        if (leblebi) {
            marketPanelOlustur();
            kokDuzen.setRight(marketPanel);
        }

        temayiUygula();
        zamanlayiciBaslat();
        arayuzuGuncelle();

        double genislik = leblebi ? 900 : 620;
        double yukseklik = leblebi
            ? Math.max(680, satirSayisi * 50 + 200)
            : Math.max(600, satirSayisi * 50 + 160);

        sahne = new Scene(kokDuzen, genislik, yukseklik);
        sahne.widthProperty().addListener((g, e, y) -> hucreBoyutlariniGuncelle());
        sahne.heightProperty().addListener((g, e, y) -> hucreBoyutlariniGuncelle());

        pencere.setScene(sahne);
        hucreBoyutlariniGuncelle();
    }

    // ── Üst Bar ───────────────────────────────────────────────────────────────

    private void ustBariOlustur() {
        maynSayaciEtiketi = new Label();
        maynSayaciEtiketi.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");

        zamanlayiciEtiketi = new Label();
        zamanlayiciEtiketi.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");

        canEtiketi = new Label("");
        canEtiketi.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");

        puanEtiketi = new Label("");
        puanEtiketi.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #c89a2a;");

        durumEtiketi = new Label("");
        durumEtiketi.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");

        sifirlaBtn = new Button(leblebModu ? "🌾" : "😊");
        sifirlaBtn.setStyle(butonTarzi());
        sifirlaBtn.setOnAction(o -> {
            sesCal(sesButon);
            oyunuSifirla();
        });

        Button menuBtn = new Button("← Menü");
        menuBtn.setStyle(butonTarzi());
        menuBtn.setOnAction(o -> {
            sesCal(sesButon);
            if (zamanlayici != null) zamanlayici.stop();
            menuGoster();
        });

        Button temaBtn = new Button(karanlikTema ? "☀" : "★");
        temaBtn.setStyle(butonTarzi());
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
        ustBar.setPadding(new Insets(0, 0, 10, 0));
        kokDuzen.setTop(ustBar);

        // Etiket içeriklerini ilk değerle doldur
        guncelleUstBar();
    }

    private void guncelleUstBar() {
        String mayinSimge = leblebModu ? "🪱 " : "💣 ";
        maynSayaciEtiketi.setText(mayinSimge + (mayinSayisi - yerlestirilenIsaret));
        if (leblebModu && leblebiBoardMode != null) {
            zamanlayiciEtiketi.setText("⏳ " + leblebiBoardMode.getKalanSure() + "s");
            canEtiketi.setText("❤ x" + leblebiBoardMode.getCanSayisi());
            puanEtiketi.setText("🫘 " + leblebiBoardMode.getLeblebPuani() + " puan");
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

    // ── Izgara ────────────────────────────────────────────────────────────────

    private void izgarayiOlustur() {
        izgaraDuzen = new GridPane();
        izgaraDuzen.setHgap(2);
        izgaraDuzen.setVgap(2);
        izgaraDuzen.setAlignment(Pos.CENTER);

        dugmeler = new Button[satirSayisi][sutunSayisi];
        hucreDurum = new byte[satirSayisi][sutunSayisi];
        // Fill with -1 (impossible state) so the first arayuzuGuncelle() paints every cell
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

                dugmeler[s][u] = btn;
                izgaraDuzen.add(btn, u, s);
            }
        }

        StackPane merkez = new StackPane(izgaraDuzen);
        merkez.setAlignment(Pos.CENTER);
        VBox.setVgrow(merkez, Priority.ALWAYS);
        kokDuzen.setCenter(merkez);
    }

    // ── Market Paneli ─────────────────────────────────────────────────────────

    private void marketPanelOlustur() {
        Seviye seviye = Seviye.getSeviye(mevcutSeviye);

        marketPanel = new VBox(12);
        marketPanel.setPadding(new Insets(12));
        marketPanel.setPrefWidth(190);
        marketPanel.setStyle(
            "-fx-background-color: #5c3a00; -fx-background-radius: 10;" +
            "-fx-border-color: #a07020; -fx-border-width: 1.5; -fx-border-radius: 10;"
        );

        Label baslik = new Label("🛒 Market");
        baslik.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #f5e6b0;");

        Label seviyeEtiketi = new Label("Seviye " + mevcutSeviye + ": " + seviye.isim);
        seviyeEtiketi.setStyle("-fx-font-size: 11px; -fx-text-fill: #c89a2a;");
        seviyeEtiketi.setWrapText(true);

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #a07020;");

        // Market ürünleri
        Button kargaBtn  = marketButonOlustur("🐦 Karga",       "Rastgele yılan göster",  "15 puan");
        Button saatBtn   = marketButonOlustur("⌚ Emmi'nin Saati","+ 30 saniye",              "20 puan");
        Button ilacBtn   = marketButonOlustur("🧪 Zirai İlaç",  "3x3 güvenli açar",          "30 puan");
        Button kalpBtn   = marketButonOlustur("💖 Ekstra Kalp", "+1 can",                    "50 puan");

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

        marketPanel.getChildren().addAll(baslik, seviyeEtiketi, sep, kargaBtn, saatBtn, ilacBtn, kalpBtn);
    }

    private Button marketButonOlustur(String isim, String aciklama, String fiyat) {
        String metin = isim + "\n" + aciklama + "\n(" + fiyat + ")";
        Button btn = new Button(metin);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setStyle(
            "-fx-background-color: #7a5200; -fx-text-fill: #f5e6b0;" +
            "-fx-font-size: 12px; -fx-padding: 8 10 8 10;" +
            "-fx-background-radius: 8; -fx-cursor: hand;" +
            "-fx-border-color: #a07020; -fx-border-width: 1; -fx-border-radius: 8;"
        );
        btn.setOnMouseEntered(e -> btn.setOpacity(0.8));
        btn.setOnMouseExited(e -> btn.setOpacity(1.0));
        return btn;
    }

    private void marketPanelGuncelle() {
        if (marketPanel == null || leblebiBoardMode == null) return;
        int puan = leblebiBoardMode.getLeblebPuani();
        marketPanel.getChildren().forEach(dugum -> {
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
            }
        });
    }

    // ── Hücre Açma / İşaretleme ───────────────────────────────────────────────

    private boolean oyunAktifDegil() {
        if (leblebModu && leblebiBoardMode != null)
            return leblebiBoardMode.isOyunBitti() || leblebiBoardMode.isKazanildi();
        return tahta == null || tahta.isOyunBitti() || tahta.kazanildiMi();
    }

    private void hucreAc(int s, int u) {
        if (tahta == null) return;

        if (leblebModu && leblebiBoardMode != null) {
            // ── Leblebi modu ──────────────────────────────────────────────────

            // Zirai İlaç aktifse — normal açma yerine ilaç uygula
            if (leblebiBoardMode.isZirayiIlacAktif()) {
                int oncekiAcik = acikHucreSay();
                tahta.zirayiIlacUygula(s, u);
                int sonrakiAcik = acikHucreSay();
                int kazanilanPuan = sonrakiAcik - oncekiAcik;
                if (kazanilanPuan > 0) {
                    leblebiBoardMode.puanEkle(kazanilanPuan);
                    puanEtiketi.setText("🫘 " + leblebiBoardMode.getLeblebPuani() + " puan");
                }
                leblebiBoardMode.zirayiIlacKullanildi();
                sesCal(sesKazma);
                return;
            }

            // Normal açma — LeblebiBoardMode handles lives internally
            int oncekiAcik = acikHucreSay();
            boolean mineHit = leblebiBoardMode.hucreAc(s, u);

            if (mineHit) {
                sesCal(sesPatlama);
                canEtiketi.setText("❤ x" + leblebiBoardMode.getCanSayisi());
                // Yılan uyarı ekranı — sadece can varsa (oyun bitmemişse)
                if (!leblebiBoardMode.isOyunBitti()) {
                    yilanUyarisiGoster(s, u);
                }
            } else {
                sesCal(sesKazma);
                int kazanilan = acikHucreSay() - oncekiAcik;
                if (kazanilan > 0) {
                    leblebiBoardMode.puanEkle(kazanilan);
                    puanEtiketi.setText("🫘 " + leblebiBoardMode.getLeblebPuani() + " puan");
                }
            }

        } else {
            // ── Klasik mod ────────────────────────────────────────────────────
            boolean solucanMiydi = tahta.getHucre(s, u).isMayinMi();
            tahta.ac(s, u);
            sesCal(solucanMiydi ? sesPatlama : sesKazma);
        }
    }

    /** Toplam açık (mayın olmayan) hücre sayısını döner. */
    private int acikHucreSay() {
        int sayi = 0;
        for (int s = 0; s < satirSayisi; s++)
            for (int u = 0; u < sutunSayisi; u++) {
                Cell h = tahta.getHucre(s, u);
                if (h.isAcildiMi() && !h.isMayinMi()) sayi++;
            }
        return sayi;
    }

    private void isaretKoy(int s, int u) {
        if (tahta == null) return;
        boolean isaretliydi = tahta.getHucre(s, u).isIsaretlendi();
        tahta.getHucre(s, u).isaretiBegistir();
        yerlestirilenIsaret += isaretliydi ? -1 : 1;
        maynSayaciEtiketi.setText((leblebModu ? "🪱 " : "💣 ") + (mayinSayisi - yerlestirilenIsaret));
    }

    // ── Zamanlayıcı ───────────────────────────────────────────────────────────

    private int gecenSaniyeKlasik = 0;

    private void zamanlayiciBaslat() {
        gecenSaniyeKlasik = 0;
        if (zamanlayici != null) zamanlayici.stop();
        zamanlayici = new Timeline(new KeyFrame(Duration.seconds(1), olay -> {
            if (leblebModu && leblebiBoardMode != null) {
                leblebiBoardMode.sureyiGuncelle(1);
                int kalan = leblebiBoardMode.getKalanSure();
                zamanlayiciEtiketi.setText("⏳ " + kalan + "s");
                if (kalan <= 15)
                    zamanlayiciEtiketi.setStyle(
                        "-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #e74c3c;");
                if (leblebiBoardMode.isOyunBitti()) {
                    zamanlayici.stop();
                    arayuzuGuncelle();
                }
            } else {
                gecenSaniyeKlasik++;
                zamanlayiciEtiketi.setText("⏱ " + gecenSaniyeKlasik + "s");
            }
        }));
        zamanlayici.setCycleCount(Animation.INDEFINITE);
        zamanlayici.play();
    }

    // ── Oyunu Sıfırla ─────────────────────────────────────────────────────────

    private void oyunuSifirla() {
        if (zamanlayici != null) zamanlayici.stop();
        yerlestirilenIsaret = 0;
        yilanHucreleri.clear();
        durumEtiketi.setText("");
        zamanlayiciEtiketi.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");

        if (leblebModu) {
            leblebiBoardMode = new LeblebiBoardMode(
                satirSayisi, sutunSayisi, mayinSayisi,
                Seviye.getSeviye(mevcutSeviye).sureSaniye, KLASIK_CAN
            );
            tahta = leblebiBoardMode.getTahta();
        } else {
            tahta = new Board(satirSayisi, sutunSayisi, mayinSayisi);
            leblebiBoardMode = null;
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

    // ── Arayüz Güncelleme ─────────────────────────────────────────────────────

    private void arayuzuGuncelle() {
        if (tahta == null || dugmeler == null) return;
        String[] sayiRenk = leblebModu ? LB_SAYI_RENK
                          : (karanlikTema ? KT_SAYI_RENK : AT_SAYI_RENK);

        int[] kargaKonum = (leblebModu && leblebiBoardMode != null)
            ? leblebiBoardMode.getKargaGosterilenMayin() : null;

        for (int s = 0; s < satirSayisi; s++) {
            for (int u = 0; u < sutunSayisi; u++) {
                Cell hucre = tahta.getHucre(s, u);
                boolean kargaHedef = (kargaKonum != null
                    && kargaKonum[0] == s && kargaKonum[1] == u
                    && !hucre.isAcildiMi());

                // Encode state into a byte that captures every visually distinct condition.
                // open-safe: 10 + neighbourCount (10–18), open-mine: 3, flagged: 1,
                // karga: 4, yilan-Y: 5, closed: 0.  Values never collide.
                boolean yilanY = leblebModu && yilanHucreleri.contains(s * sutunSayisi + u);
                byte nyDurum;
                if      (yilanY)                                       nyDurum = 5;
                else if (hucre.isAcildiMi() && hucre.isMayinMi())     nyDurum = 3;
                else if (hucre.isAcildiMi())                           nyDurum = (byte)(10 + hucre.getKomsuMayinSayisi());
                else if (hucre.isIsaretlendi())                        nyDurum = 1;
                else if (kargaHedef)                                   nyDurum = 4;
                else                                                   nyDurum = 0;

                if (nyDurum == hucreDurum[s][u]) continue; // nothing changed — skip
                hucreDurum[s][u] = nyDurum;

                Button btn = dugmeler[s][u];

                if (nyDurum == 3) { // opened mine (oyun bitti, tüm mayınlar açıldı)
                    btn.setText(leblebModu ? "🐍" : "X");
                    btn.setGraphic(leblebModu ? null : cachedView(imgMayin));
                    btn.setStyle(mayinHucreTarzi());
                    btn.setDisable(true);
                } else if (nyDurum >= 10) { // opened safe (10 + neighbourCount)
                    btn.setGraphic(null);
                    int k = hucre.getKomsuMayinSayisi();
                    btn.setText(k == 0 ? "" : String.valueOf(k));
                    btn.setStyle(acilmisHucreTarzi(k, sayiRenk));
                    btn.setDisable(true);
                } else if (nyDurum == 1) { // flagged
                    // Always show emoji fallback — image may still be loading in background
                    ImageView bayrakView = cachedView(imgBayrak);
                    if (bayrakView != null) {
                        btn.setGraphic(bayrakView);
                        btn.setText("");
                    } else {
                        btn.setGraphic(null);
                        btn.setText(leblebModu ? "⚑" : "🚩");
                    }
                    btn.setStyle(isaretliHucreTarzi());
                    btn.setDisable(false);
                } else if (nyDurum == 4) { // karga highlight
                    btn.setGraphic(null);
                    btn.setText("");
                    btn.setStyle(acilmamisHucreTarzi() +
                        "-fx-border-color: " + LB_KARGA_RENK + "; -fx-border-width: 3;");
                    btn.setDisable(false);
                } else if (nyDurum == 5) { // yılan basılan hücre — Y göster
                    btn.setGraphic(null);
                    btn.setText("Y");
                    btn.setStyle(
                        "-fx-background-color: #8B0000;" +
                        "-fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-font-size: 14px;" +
                        "-fx-border-color: #ff4444; -fx-border-width: 2;" +
                        "-fx-background-radius: 3; -fx-border-radius: 3; -fx-padding: 0;"
                    );
                    btn.setDisable(true);
                } else { // closed normal
                    btn.setGraphic(null);
                    btn.setText("");
                    btn.setStyle(acilmamisHucreTarzi());
                    btn.setDisable(false);
                }
            }
        }

        kontrolEt();
    }

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
                // Seviye geçiş ekranı geciktirme
                javafx.application.Platform.runLater(this::seviyeGecisiniGoster);

            } else if (leblebiBoardMode.isOyunBitti()) {
                zamanlayici.stop();
                sifirlaBtn.setText("🪱");
                boolean sureBitti = leblebiBoardMode.getKalanSure() <= 0;
                String msg = sureBitti ? "⏰ Süre doldu! Mehmet Emmi üzüldü..."
                                       : "💀 Canların bitti! Yılanlar kazandı!";
                durumEtiketi.setText(msg);
                durumEtiketi.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #e74c3c;");
                javafx.application.Platform.runLater(this::oyunSonuPopupuGoster);
            }

        } else if (tahta != null) {
            if (tahta.isOyunBitti()) {
                zamanlayici.stop();
                sesCal(sesPatlama);
                sifirlaBtn.setText("😵");
                durumEtiketi.setText("✖ Oyun Bitti!");
                durumEtiketi.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;" +
                    "-fx-text-fill: " + (karanlikTema ? "#f38ba8" : "#c62828") + ";");
            } else if (tahta.kazanildiMi()) {
                zamanlayici.stop();
                sesCal(sesKazan);
                sifirlaBtn.setText("😎");
                durumEtiketi.setText("★ Kazandınız! (" + gecenSaniyeKlasik + "s)");
                durumEtiketi.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;" +
                    "-fx-text-fill: " + (karanlikTema ? "#a6e3a1" : "#2e7d32") + ";");
            }
        }
    }

    // ── Seviye Geçiş Ekranı ───────────────────────────────────────────────────

    private void seviyeGecisiniGoster() {
        boolean sonSeviye = Seviye.sonSeviyeMi(mevcutSeviye);
        Seviye seviye = Seviye.getSeviye(mevcutSeviye);

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
                new ButtonType("Menüye Dön", ButtonBar.ButtonData.CANCEL_CLOSE)
            );
        } else {
            String sonrakiIsim = Seviye.getSeviye(mevcutSeviye + 1).isim;
            baslik = "✅ Seviye " + mevcutSeviye + " Tamamlandı!";
            icerik = String.format(
                "%s temizlendi!\n\n" +
                "Bu bölümde kazanılan puan: %d\n" +
                "Toplam puan: %d\n\n" +
                "Sıradaki bölüm: %s",
                seviye.isim, leblebiBoardMode.getLeblebPuani(),
                toplamLeblebPuani, sonrakiIsim
            );
            dialog.getButtonTypes().addAll(
                new ButtonType("▶ Sonraki Bölüm", ButtonBar.ButtonData.OK_DONE),
                new ButtonType("Menüye Dön", ButtonBar.ButtonData.CANCEL_CLOSE)
            );
        }

        dialog.setHeaderText(baslik);
        dialog.setContentText(icerik);
        dialog.getDialogPane().setStyle(
            "-fx-background-color: #3d2800; -fx-font-size: 13px; -fx-text-fill: #f5e6b0;");
        dialogStilUygula(dialog, true);

        dialog.showAndWait().ifPresent(cevap -> {
            if (cevap.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                if (sonSeviye) {
                    oyunSonuPopupuGoster();
                } else {
                    mevcutSeviye++;
                    leblebOyunuBaslat();
                }
            } else {
                menuGoster();
            }
        });
    }

    // ── Oyun Sonu Popup ───────────────────────────────────────────────────────

    // ── Yılan Uyarı Ekranı ────────────────────────────────────────────────────

    private void yilanUyarisiGoster(int satir, int sutun) {
        // O hücreyi Y olarak işaretle (görsel)
        yilanHucreleri.add(satir * sutunSayisi + sutun);

        Alert uyari = new Alert(Alert.AlertType.WARNING);
        uyari.setTitle("Yılan!");
        uyari.setHeaderText("🐍 Yılana Bastın!");
        uyari.setContentText(
            "Dikkatli ol! Yılan seni ısırdı.\n" +
            "Canın gitti! ❤ x" + leblebiBoardMode.getCanSayisi() + " kaldı.\n\n" +
            "O hücre işaretlendi (Y)."
        );
        dialogStilUygula(uyari, true);
        uyari.showAndWait();
    }

    private boolean popupGosterildi = false;

    private void oyunSonuPopupuGoster() {
        if (popupGosterildi) return;
        popupGosterildi = true;

        // For leblebi mode: toplamLeblebPuani has all level points summed (current level
        // was added in kontrolEt() before calling this popup). Convert to final score.
        int finalSkor;
        if (leblebiBoardMode != null) {
            finalSkor = toplamLeblebPuani * 10 + leblebiBoardMode.getKalanSure() * 5;
        } else {
            finalSkor = gecenSaniyeKlasik;
        }

        TextInputDialog isimDialog = new TextInputDialog("Tarımcı");
        isimDialog.setTitle("Skor Tablosu");
        isimDialog.setHeaderText("🏆 Oyun Bitti!");
        isimDialog.setContentText(
            String.format("Skor: %d\nİsminizi girin:", finalSkor)
        );
        isimDialog.getDialogPane().setStyle("-fx-background-color: #1e1e2e; -fx-font-size: 13px;");
        dialogStilUygula(isimDialog, false);

        isimDialog.showAndWait().ifPresent(isim -> {
            if (!isim.isBlank()) {
                SkorTablosu.kaydet(isim.trim(), finalSkor, mevcutSeviye);
            }
        });

        popupGosterildi = false;
        menuGoster();
    }

    // ── Skor Tablosu ─────────────────────────────────────────────────────────

    private void skorTablosunuGoster() {
        Stage skorPenceresi = new Stage();
        skorPenceresi.setTitle("🏆 En İyi Tarımcılar");
        skorPenceresi.initModality(Modality.APPLICATION_MODAL);

        VBox kok = new VBox(16);
        kok.setPadding(new Insets(24));
        kok.setStyle("-fx-background-color: #1e1e2e;");
        kok.setAlignment(Pos.TOP_CENTER);

        Label baslik = new Label("🏆 En İyi Tarımcılar");
        baslik.setStyle(
            "-fx-font-size: 24px; -fx-font-weight: bold;" +
            "-fx-text-fill: #c89a2a;"
        );

        java.util.List<SkorTablosu.SkorGirisi> liste = SkorTablosu.yukle();

        GridPane tablo = new GridPane();
        tablo.setHgap(20);
        tablo.setVgap(8);
        tablo.setStyle("-fx-background-color: #181825; -fx-padding: 16; -fx-background-radius: 10;");
        tablo.setAlignment(Pos.CENTER);

        // Başlık satırı
        String[] basliklar = {"#", "İsim", "Skor", "Seviye", "Tarih"};
        for (int i = 0; i < basliklar.length; i++) {
            Label lbl = new Label(basliklar[i]);
            lbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #89b4fa; -fx-font-size: 13px;");
            tablo.add(lbl, i, 0);
        }

        if (liste.isEmpty()) {
            Label bos = new Label("Henüz kayıt yok. Oyna ve adını yazdır!");
            bos.setStyle("-fx-text-fill: #6c7086; -fx-font-size: 13px;");
            tablo.add(bos, 0, 1, 5, 1);
        } else {
            for (int i = 0; i < Math.min(liste.size(), 20); i++) {
                SkorTablosu.SkorGirisi g = liste.get(i);
                String isimGoster  = (g.isim()  == null || g.isim().isBlank())  ? "—" : g.isim();
                String tarihGoster = (g.tarih() == null || g.tarih().isBlank()) ? "—" : g.tarih();
                String seviyeGoster = g.seviye() <= 0 ? "Klasik" : "Seviye " + g.seviye();
                String[] degerler = {
                    (i + 1) + ".",
                    isimGoster,
                    String.valueOf(g.skor()),
                    seviyeGoster,
                    tarihGoster
                };
                String renk = i == 0 ? "#f1c40f" : i == 1 ? "#95a5a6" : i == 2 ? "#e67e22" : "#cdd6f4";
                for (int j = 0; j < degerler.length; j++) {
                    Label lbl = new Label(degerler[j]);
                    lbl.setStyle("-fx-text-fill: " + renk + "; -fx-font-size: 12px;");
                    tablo.add(lbl, j, i + 1);
                }
            }
        }

        Button kapat = new Button("Kapat");
        kapat.setStyle(butonTarzi() + "-fx-background-color: #313244; -fx-text-fill: #cdd6f4;");
        kapat.setOnAction(e -> skorPenceresi.close());

        ScrollPane scroll = new ScrollPane(tablo);
        scroll.setStyle("-fx-background-color: transparent;");
        scroll.setFitToWidth(true);

        kok.getChildren().addAll(baslik, scroll, kapat);

        Scene sahne2 = new Scene(kok, 560, 480);
        skorPenceresi.setScene(sahne2);
        skorPenceresi.showAndWait();
    }

    // ── Hücre Boyut Güncelleme ────────────────────────────────────────────────

    private void hucreBoyutlariniGuncelle() {
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

    // ── Hücre Stilleri ───────────────────────────────────────────────────────

    private String acilmamisHucreTarzi() {
        String bg = leblebModu ? LB_ACILMAMIS : (karanlikTema ? KT_ACILMAMIS : AT_ACILMAMIS);
        String br = leblebModu ? LB_CERCEVE   : (karanlikTema ? KT_CERCEVE   : AT_CERCEVE);
        return "-fx-background-color:" + bg + ";" +
               "-fx-border-color:" + br + ";" +
               "-fx-border-width:1;-fx-background-radius:3;-fx-border-radius:3;" +
               "-fx-padding:0;-fx-cursor:hand;";
    }

    private String acilmisHucreTarzi(int k, String[] sr) { return acilmisHucreTarzi(k, sr, 14); }

    private String acilmisHucreTarzi(int k, String[] sr, double yaz) {
        String bg = leblebModu ? LB_ACILMIS   : (karanlikTema ? KT_ACILMIS   : AT_ACILMIS);
        String br = leblebModu ? LB_CERCEVE   : (karanlikTema ? KT_CERCEVE   : AT_CERCEVE);
        String fg = (k > 0 && k <= 8) ? sr[k]
                  : (leblebModu ? LB_YAZI_SOLUK : (karanlikTema ? KT_YAZI_SOLUK : AT_YAZI_SOLUK));
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
        String bg = leblebModu ? LB_SOLUCAN_RENK : (karanlikTema ? KT_MAYIN : AT_MAYIN);
        String fg = leblebModu ? "#f5e6b0"        : (karanlikTema ? "#1e1e2e" : "#ffffff");
        return "-fx-background-color:" + bg + ";-fx-border-width:1;" +
               "-fx-background-radius:3;-fx-border-radius:3;-fx-padding:0;" +
               "-fx-text-fill:" + fg + ";-fx-font-weight:bold;-fx-font-size:14px;";
    }

    // ── Tema ─────────────────────────────────────────────────────────────────

    /** Forces a full repaint on the next arayuzuGuncelle() call by clearing the state cache. */
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
                "-fx-background-radius:8;-fx-padding:8 12 8 12;"
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

    // ── Yardımcı ─────────────────────────────────────────────────────────────

    private void bilgilendirmeGoster(String baslik, String mesaj) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(baslik);
        a.setHeaderText(null);
        a.setContentText(mesaj);
        dialogStilUygula(a, false);
        a.showAndWait();
    }

    /**
     * Tüm Alert/Dialog pencerelerine okunabilir stil uygular.
     * leblebi=true ise kahverengi tema, false ise koyu tema kullanılır.
     */
    private void dialogStilUygula(Alert alert, boolean leblebi) {
        dialogStilUygulaTemel(alert.getDialogPane(), leblebi);
    }

    private void dialogStilUygula(TextInputDialog dialog, boolean leblebi) {
        dialogStilUygulaTemel(dialog.getDialogPane(), leblebi);
    }

    private void dialogStilUygulaTemel(javafx.scene.control.DialogPane pane, boolean leblebi) {
        String bg   = leblebi ? "#3d2800" : "#1e1e2e";
        String fg   = leblebi ? "#f5e6b0" : "#cdd6f4";
        String hbg  = leblebi ? "#5c3a00" : "#181825";

        pane.setStyle(
            "-fx-background-color: " + bg + ";" +
            "-fx-font-size: 14px;" +
            "-fx-text-fill: " + fg + ";"
        );

        javafx.application.Platform.runLater(() -> {
            javafx.scene.Node contentNode = pane.lookup(".content.label");
            if (contentNode instanceof javafx.scene.control.Label lbl)
                lbl.setStyle("-fx-text-fill: " + fg + "; -fx-font-size: 14px;");

            javafx.scene.Node headerNode = pane.lookup(".header-panel");
            if (headerNode != null) {
                headerNode.setStyle("-fx-background-color: " + hbg + ";");
                javafx.scene.Node headerLabel = pane.lookup(".header-panel .label");
                if (headerLabel instanceof javafx.scene.control.Label lbl)
                    lbl.setStyle("-fx-text-fill: " + fg + "; -fx-font-size: 15px; -fx-font-weight: bold;");
            }

            // TextField varsa (TextInputDialog)
            javafx.scene.Node tf = pane.lookup(".text-field");
            if (tf instanceof javafx.scene.control.TextField field)
                field.setStyle("-fx-background-color: " + hbg + "; -fx-text-fill: " + fg + "; -fx-font-size: 13px;");

            // Butonları düzelt
            pane.getButtonTypes().forEach(bt -> {
                javafx.scene.Node btn = pane.lookupButton(bt);
                if (btn != null)
                    btn.setStyle(
                        "-fx-background-color: " + (leblebi ? "#7a5200" : "#313244") + ";" +
                        "-fx-text-fill: " + fg + ";" +
                        "-fx-font-size: 13px; -fx-background-radius: 6; -fx-padding: 6 14 6 14;"
                    );
            });
        });
    }

    public static void main(String[] args) { launch(); }
}
