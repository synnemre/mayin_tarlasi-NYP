import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

public class MinesweeperApp extends Application {

    // ── Oyun sabitleri ────────────────────────────────────────────────────────
    private static final int SATIR_SAYISI  = 10;
    private static final int SUTUN_SAYISI  = 10;
    private static final int MAYIN_SAYISI  = 11;

    // ── Leblebi Modu Sabitleri ────────────────────────────────────────────────
    private static final int  LB_SURE_SANIYE = 90;
    private static final int  LB_CAN         = 3;
    private static final String LB_SOLUCAN   = "🪱";
    private static final String LB_CUBUK     = "🥢";

    // ── Oyun durumu ───────────────────────────────────────────────────────────
    private Board tahta;
    private LeblebiBoardMode leblebiBoardMode;
    private boolean leblebModu = false;
    private Button[][] dugmeler;
    private int yerlestirilenIsaret;
    private int gecenSaniye;

    // ── Arayüz bileşenleri ────────────────────────────────────────────────────
    private Label maynSayaciEtiketi;
    private Label zamanlayiciEtiketi;
    private Label durumEtiketi;
    private Label canEtiketi;
    private Button sifirlaBtn;
    private Button temaBtn;
    private Button leblebBtn;
    private Timeline zamanlayici;
    private boolean karanlikTema = true;
    private BorderPane kokDuzen;
    private GridPane izgaraDuzen;
    private Scene sahne;

    // ── Karanlık Tema ─────────────────────────────────────────────────────────
    private static final String KT_ARKAPLAN  = "#1e1e2e";
    private static final String KT_ACILMAMIS = "#454158";
    private static final String KT_ACILMIS   = "#11111b";
    private static final String KT_ISARETLI  = "#524f6e";
    private static final String KT_MAYIN     = "#f38ba8";
    private static final String KT_CERCEVE   = "#6c7086";
    private static final String KT_YAZI      = "#cdd6f4";
    private static final String KT_YAZI_SOLUK= "#6c7086";
    private static final String KT_UST_BAR   = "#181825";

    // ── Aydınlık Tema ─────────────────────────────────────────────────────────
    private static final String AT_ARKAPLAN  = "#e8eaf0";
    private static final String AT_ACILMAMIS = "#c0c8d8";
    private static final String AT_ACILMIS   = "#f4f4f4";
    private static final String AT_ISARETLI  = "#b0b8cc";
    private static final String AT_MAYIN     = "#e57373";
    private static final String AT_CERCEVE   = "#9aa0b0";
    private static final String AT_YAZI      = "#1e1e2e";
    private static final String AT_YAZI_SOLUK= "#9aa0b0";
    private static final String AT_UST_BAR   = "#d0d4de";

    // ── Leblebi Tema ──────────────────────────────────────────────────────────
    private static final String LB_ARKAPLAN  = "#3d2800";
    private static final String LB_ACILMAMIS = "#c89a2a";
    private static final String LB_ACILMIS   = "#f5e6b0";
    private static final String LB_ISARETLI  = "#e8b84b";
    private static final String LB_SOLUCAN_RENK = "#4a7c2f";
    private static final String LB_CERCEVE   = "#a07020";
    private static final String LB_YAZI      = "#3d2800";
    private static final String LB_YAZI_SOLUK= "#a07020";
    private static final String LB_UST_BAR   = "#5c3a00";

    // ── Sayı renkleri ─────────────────────────────────────────────────────────
    private static final String[] KARANLIK_SAYI_RENKLERI = {
        "", "#89b4fa", "#a6e3a1", "#f38ba8",
        "#74c7ec", "#fab387", "#89dceb", "#b4befe", "#cdd6f4"
    };
    private static final String[] AYDINLIK_SAYI_RENKLERI = {
        "", "#1565c0", "#2e7d32", "#c62828",
        "#0277bd", "#e65100", "#00838f", "#6a1b9a", "#37474f"
    };
    private static final String[] LEBLEBI_SAYI_RENKLERI = {
        "", "#5a3e00", "#2e7d32", "#b22222",
        "#1a5276", "#7d3c00", "#1a6b4a", "#5b2c6f", "#3d2800"
    };

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void start(Stage pencere) {
        kokDuzen = new BorderPane();
        kokDuzen.setPadding(new Insets(16));

        ustBariOlustur();
        izgarayiOlustur();
        temayiUygula();
        zamanlayiciBaslat();
        arayuzuGuncelle();

        this.sahne = new Scene(kokDuzen, 600, 700);

        this.sahne.widthProperty().addListener((g, e, y) -> hucreBoyutlariniGuncelle());
        this.sahne.heightProperty().addListener((g, e, y) -> hucreBoyutlariniGuncelle());

        pencere.setScene(this.sahne);
        pencere.setTitle("Mayın Tarlası");
        pencere.setResizable(true);
        pencere.show();
        hucreBoyutlariniGuncelle();
    }

    // ── Üst Bar ───────────────────────────────────────────────────────────────

    private void ustBariOlustur() {
        maynSayaciEtiketi = new Label("💣 " + MAYIN_SAYISI);
        maynSayaciEtiketi.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        sifirlaBtn = new Button("😊");
        sifirlaBtn.setStyle(
            "-fx-font-size: 18px; -fx-padding: 4 12 4 12;" +
            "-fx-cursor: hand; -fx-border-radius: 6; -fx-background-radius: 6;"
        );
        sifirlaBtn.setOnAction(o -> oyunuSifirla());

        zamanlayiciEtiketi = new Label("⏱ 0s");
        zamanlayiciEtiketi.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        canEtiketi = new Label("");
        canEtiketi.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        durumEtiketi = new Label("");
        durumEtiketi.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        temaBtn = new Button(karanlikTema ? "☀ Aydınlık" : "★ Karanlık");
        temaBtn.setStyle(butonTarzi());
        temaBtn.setOnAction(o -> temaDegistir());

        leblebBtn = new Button("🫘 Leblebi Modu");
        leblebBtn.setStyle(butonTarzi() + "-fx-background-color: #c89a2a; -fx-text-fill: #3d2800;");
        leblebBtn.setOnAction(o -> leblebModunuAktiflestir());

        HBox ustBar = new HBox(maynSayaciEtiketi, canEtiketi, sifirlaBtn,
                               zamanlayiciEtiketi, durumEtiketi, temaBtn, leblebBtn);
        ustBar.setAlignment(Pos.CENTER);
        ustBar.setSpacing(10);
        ustBar.setPadding(new Insets(0, 0, 10, 0));
        kokDuzen.setTop(ustBar);
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
        GridPane.setHgrow(izgaraDuzen, Priority.ALWAYS);
        GridPane.setVgrow(izgaraDuzen, Priority.ALWAYS);

        dugmeler = new Button[SATIR_SAYISI][SUTUN_SAYISI];

        if (leblebModu) {
            leblebiBoardMode = new LeblebiBoardMode(SATIR_SAYISI, SUTUN_SAYISI,
                                                     MAYIN_SAYISI, LB_SURE_SANIYE, LB_CAN);
            tahta = leblebiBoardMode.getTahta();
        } else {
            tahta = new Board(SATIR_SAYISI, SUTUN_SAYISI, MAYIN_SAYISI);
            leblebiBoardMode = null;
        }

        yerlestirilenIsaret = 0;
        canEtiketi.setText(leblebModu ? "❤ ".repeat(LB_CAN) : "");

        for (int s = 0; s < SATIR_SAYISI; s++) {
            for (int u = 0; u < SUTUN_SAYISI; u++) {
                Button btn = new Button();
                btn.setPrefSize(52, 52);
                btn.setMinSize(32, 32);

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

    private boolean oyunAktifDegil() {
        if (leblebModu && leblebiBoardMode != null) {
            return leblebiBoardMode.isOyunBitti() || leblebiBoardMode.isKazanildi();
        }
        return tahta.isOyunBitti() || tahta.kazanildiMi();
    }

    private void hucreAc(int s, int u) {
        boolean solucanMiydi = tahta.getHucre(s, u).isMayinMi();
        tahta.ac(s, u);

        if (leblebModu && leblebiBoardMode != null) {
            // Eğer solucan patladıysa — can azalt ama oyun Board'a bitmiş görünür
            // Board'un "oyunBitti" yerine canı biz yönetiyoruz
            // Board'u tek solucan için yeniden başlat (devam edeceğiz)
            if (solucanMiydi && !leblebiBoardMode.isOyunBitti()) {
                leblebiBoardMode.solucanaBastir();
                canEtiketi.setText("❤ ".repeat(Math.max(0, leblebiBoardMode.getCanSayisi())));
            }
        }
    }

    private void isaretKoy(int s, int u) {
        boolean isaretliydi = tahta.getHucre(s, u).isIsaretlendi();
        tahta.getHucre(s, u).isaretiBegistir();
        yerlestirilenIsaret += isaretliydi ? -1 : 1;
        String simge = leblebModu ? LB_SOLUCAN + " " : "💣 ";
        maynSayaciEtiketi.setText(simge + (MAYIN_SAYISI - yerlestirilenIsaret));
    }

    // ── Leblebi Modu Aktivasyonu ──────────────────────────────────────────────

    private void leblebModunuAktiflestir() {
        leblebModu = !leblebModu;
        leblebBtn.setText(leblebModu ? "💣 Normal Mod" : "🫘 Leblebi Modu");

        if (leblebModu) {
            leblebBtn.setStyle(butonTarzi() +
                "-fx-background-color: #5a3e00; -fx-text-fill: #f5e6b0;");
        } else {
            leblebBtn.setStyle(butonTarzi() +
                "-fx-background-color: #c89a2a; -fx-text-fill: #3d2800;");
        }

        oyunuSifirla();
    }

    // ── Tema ──────────────────────────────────────────────────────────────────

    private void temaDegistir() {
        karanlikTema = !karanlikTema;
        temaBtn.setText(karanlikTema ? "☀ Aydınlık" : "★ Karanlık");
        temayiUygula();
        arayuzuGuncelle();
        hucreBoyutlariniGuncelle();
    }

    private void temayiUygula() {
        String arkaplan  = leblebModu ? LB_ARKAPLAN  : (karanlikTema ? KT_ARKAPLAN  : AT_ARKAPLAN);
        String ustBarRng = leblebModu ? LB_UST_BAR   : (karanlikTema ? KT_UST_BAR   : AT_UST_BAR);
        String yaziRengi = leblebModu ? "#f5e6b0"    : (karanlikTema ? KT_YAZI      : AT_YAZI);
        String cerceveRg = leblebModu ? LB_CERCEVE   : (karanlikTema ? KT_CERCEVE   : AT_CERCEVE);

        kokDuzen.setStyle("-fx-background-color: " + arkaplan + ";");

        if (kokDuzen.getTop() instanceof HBox ustBar) {
            ustBar.setStyle(
                "-fx-background-color: " + ustBarRng + ";" +
                "-fx-background-radius: 8; -fx-padding: 8 12 8 12;"
            );
            ustBar.getChildren().forEach(dugum -> {
                if (dugum instanceof Label etiket) {
                    etiket.setStyle(etiket.getStyle() + "-fx-text-fill: " + yaziRengi + ";");
                } else if (dugum instanceof Button btn && btn != leblebBtn) {
                    btn.setStyle(btn.getStyle() +
                        "-fx-background-color: " + (karanlikTema && !leblebModu ? "#313244" : (leblebModu ? "#7a5200" : "#c8cdd8")) + ";" +
                        "-fx-text-fill: " + yaziRengi + ";" +
                        "-fx-border-color: " + cerceveRg + ";"
                    );
                }
            });
        }

        if (izgaraDuzen != null) {
            izgaraDuzen.setStyle("-fx-background-color: " + cerceveRg + "; -fx-padding: 2;");
        }
    }

    // ── Zamanlayıcı ───────────────────────────────────────────────────────────

    private void zamanlayiciBaslat() {
        gecenSaniye = 0;
        if (zamanlayici != null) zamanlayici.stop();
        zamanlayici = new Timeline(new KeyFrame(Duration.seconds(1), olay -> {
            gecenSaniye++;
            if (leblebModu && leblebiBoardMode != null) {
                leblebiBoardMode.sureyiGuncelle(1);
                int kalan = leblebiBoardMode.getKalanSure();
                zamanlayiciEtiketi.setText("⏳ " + kalan + "s");

                // Renk uyarısı: süre azalınca kırmızıya dön
                if (kalan <= 15) {
                    zamanlayiciEtiketi.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;" +
                                                "-fx-text-fill: #e74c3c;");
                }
                if (leblebiBoardMode.isOyunBitti()) {
                    zamanlayici.stop();
                    arayuzuGuncelle();
                }
            } else {
                zamanlayiciEtiketi.setText("⏱ " + gecenSaniye + "s");
            }
        }));
        zamanlayici.setCycleCount(Animation.INDEFINITE);
        zamanlayici.play();
    }

    // ── Oyunu sıfırla ─────────────────────────────────────────────────────────

    private void oyunuSifirla() {
        if (zamanlayici != null) zamanlayici.stop();
        yerlestirilenIsaret = 0;
        String simge = leblebModu ? LB_SOLUCAN + " " : "💣 ";
        maynSayaciEtiketi.setText(simge + MAYIN_SAYISI);
        zamanlayiciEtiketi.setText(leblebModu ? "⏳ " + LB_SURE_SANIYE + "s" : "⏱ 0s");
        zamanlayiciEtiketi.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        sifirlaBtn.setText(leblebModu ? "🌾" : "😊");
        durumEtiketi.setText("");
        izgarayiOlustur();
        temayiUygula();
        zamanlayiciBaslat();
        arayuzuGuncelle();
        hucreBoyutlariniGuncelle();
    }

    // ── Arayüz güncelleme ─────────────────────────────────────────────────────

    private void arayuzuGuncelle() {
        if (tahta == null || dugmeler == null) return;
        String[] sayiRenkleri = leblebModu ? LEBLEBI_SAYI_RENKLERI
                              : (karanlikTema ? KARANLIK_SAYI_RENKLERI : AYDINLIK_SAYI_RENKLERI);

        for (int s = 0; s < SATIR_SAYISI; s++) {
            for (int u = 0; u < SUTUN_SAYISI; u++) {
                Cell hucre = tahta.getHucre(s, u);
                Button btn = dugmeler[s][u];

                if (hucre.isAcildiMi()) {
                    if (hucre.isMayinMi()) {
                        btn.setText(leblebModu ? LB_SOLUCAN : "X");
                        btn.setStyle(mayinHucreTarzi());
                    } else {
                        int komsular = hucre.getKomsuMayinSayisi();
                        btn.setText(komsular == 0 ? "" : String.valueOf(komsular));
                        btn.setStyle(acilmisHucreTarzi(komsular, sayiRenkleri));
                    }
                    btn.setDisable(true);
                } else if (hucre.isIsaretlendi()) {
                    btn.setText(leblebModu ? LB_CUBUK : "F");
                    btn.setStyle(isaretliHucreTarzi());
                    btn.setDisable(false);
                } else {
                    btn.setText("");
                    btn.setStyle(acilmamisHucreTarzi());
                    btn.setDisable(false);
                }
            }
        }

        // ── Kazanma/Kaybetme durumları ─────────────────────────────────────
        if (leblebModu && leblebiBoardMode != null) {
            if (leblebiBoardMode.isOyunBitti() && !leblebiBoardMode.isKazanildi()) {
                zamanlayici.stop();
                sifirlaBtn.setText("🪱");
                boolean sureBitti = leblebiBoardMode.getKalanSure() <= 0 &&
                                    leblebiBoardMode.getCanSayisi() > 0;
                String msg = sureBitti ? "⏰ Süre doldu! Mehmet Emmi üzüldü..."
                                       : "💀 Canların bitti! Solucanlar kazandı!";
                durumEtiketi.setText(msg);
                durumEtiketi.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;" +
                                      "-fx-text-fill: #e74c3c;");
            } else {
                leblebiBoardMode.kazanmaKontrol();
                if (leblebiBoardMode.isKazanildi()) {
                    zamanlayici.stop();
                    sifirlaBtn.setText("🎉");
                    durumEtiketi.setText("🫘 Mehmet Emmi çok mutlu! Tüm solucanlar bulundu!");
                    durumEtiketi.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;" +
                                          "-fx-text-fill: #c89a2a;");
                }
            }
        } else {
            if (tahta.isOyunBitti()) {
                zamanlayici.stop();
                sifirlaBtn.setText("😵");
                durumEtiketi.setText("✖ Oyun Bitti!");
                durumEtiketi.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;" +
                        "-fx-text-fill: " + (karanlikTema ? "#f38ba8" : "#c62828") + ";");
            } else if (tahta.kazanildiMi()) {
                zamanlayici.stop();
                sifirlaBtn.setText("😎");
                durumEtiketi.setText("★ Kazandınız!");
                durumEtiketi.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;" +
                        "-fx-text-fill: " + (karanlikTema ? "#a6e3a1" : "#2e7d32") + ";");
            }
        }
    }

    // ── Hücre boyut güncelleme ────────────────────────────────────────────────

    private void hucreBoyutlariniGuncelle() {
        if (dugmeler == null) return;
        double kullanilabilirG = sahne.getWidth()  - 52;
        double kullanilabilirY = sahne.getHeight() - 130;
        double hg = Math.floor(kullanilabilirG / SUTUN_SAYISI);
        double hy = Math.floor(kullanilabilirY / SATIR_SAYISI);
        double boyut = Math.max(32, Math.min(hg, hy));
        double yazi  = Math.max(10, boyut * 0.28);
        String[] sayiRenkleri = leblebModu ? LEBLEBI_SAYI_RENKLERI
                              : (karanlikTema ? KARANLIK_SAYI_RENKLERI : AYDINLIK_SAYI_RENKLERI);

        for (int s = 0; s < SATIR_SAYISI; s++) {
            for (int u = 0; u < SUTUN_SAYISI; u++) {
                Button btn = dugmeler[s][u];
                btn.setPrefSize(boyut, boyut);
                btn.setMinSize(boyut, boyut);
                btn.setMaxSize(boyut, boyut);
                Cell hucre = tahta.getHucre(s, u);
                if (hucre.isAcildiMi() && !hucre.isMayinMi()) {
                    int k = hucre.getKomsuMayinSayisi();
                    btn.setStyle(acilmisHucreTarzi(k, sayiRenkleri, yazi));
                } else if (!hucre.isAcildiMi() && !hucre.isIsaretlendi()) {
                    btn.setStyle(acilmamisHucreTarzi());
                }
            }
        }
    }

    // ── Hücre stilleri ───────────────────────────────────────────────────────

    private String acilmamisHucreTarzi() {
        String bg = leblebModu ? LB_ACILMAMIS : (karanlikTema ? KT_ACILMAMIS : AT_ACILMAMIS);
        String br = leblebModu ? LB_CERCEVE   : (karanlikTema ? KT_CERCEVE   : AT_CERCEVE);
        return "-fx-background-color: " + bg + ";" +
               "-fx-border-color: " + br + ";" +
               "-fx-border-width: 1; -fx-background-radius: 3;" +
               "-fx-border-radius: 3; -fx-padding: 0; -fx-cursor: hand;";
    }

    private String acilmisHucreTarzi(int k, String[] sr) { return acilmisHucreTarzi(k, sr, 14); }

    private String acilmisHucreTarzi(int k, String[] sr, double yazi) {
        String bg  = leblebModu ? LB_ACILMIS   : (karanlikTema ? KT_ACILMIS   : AT_ACILMIS);
        String br  = leblebModu ? LB_CERCEVE   : (karanlikTema ? KT_CERCEVE   : AT_CERCEVE);
        String fg  = (k > 0 && k <= 8) ? sr[k]
                   : (leblebModu ? LB_YAZI_SOLUK : (karanlikTema ? KT_YAZI_SOLUK : AT_YAZI_SOLUK));
        return "-fx-background-color: " + bg + ";" +
               "-fx-border-color: " + br + ";" +
               "-fx-border-width: 1; -fx-background-radius: 3;" +
               "-fx-border-radius: 3; -fx-padding: 0;" +
               "-fx-text-fill: " + fg + ";" +
               "-fx-font-weight: bold; -fx-font-size: " + yazi + "px;";
    }

    private String isaretliHucreTarzi() {
        String bg = leblebModu ? LB_ISARETLI  : (karanlikTema ? KT_ISARETLI  : AT_ISARETLI);
        String br = leblebModu ? LB_CERCEVE   : (karanlikTema ? KT_CERCEVE   : AT_CERCEVE);
        String fg = leblebModu ? "#3d2800"    : (karanlikTema ? "#f38ba8"    : "#c62828");
        return "-fx-background-color: " + bg + ";" +
               "-fx-border-color: " + br + ";" +
               "-fx-border-width: 1; -fx-background-radius: 3;" +
               "-fx-border-radius: 3; -fx-padding: 0;" +
               "-fx-text-fill: " + fg + ";" +
               "-fx-font-weight: bold; -fx-font-size: 14px; -fx-cursor: hand;";
    }

    private String mayinHucreTarzi() {
        String bg = leblebModu ? LB_SOLUCAN_RENK : (karanlikTema ? KT_MAYIN : AT_MAYIN);
        String fg = leblebModu ? "#f5e6b0"        : (karanlikTema ? "#1e1e2e" : "#ffffff");
        return "-fx-background-color: " + bg + ";" +
               "-fx-border-width: 1; -fx-background-radius: 3;" +
               "-fx-border-radius: 3; -fx-padding: 0;" +
               "-fx-text-fill: " + fg + ";" +
               "-fx-font-weight: bold; -fx-font-size: 14px;";
    }

    public static void main(String[] args) { launch(); }
}
