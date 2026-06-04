import javafx.animation.*;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import java.security.SecureRandom;

/**
 * GameController — All in-game logic that was previously inlined in MinesweeperApp.
 *
 * Responsibilities:
 *   - hucreAc / isaretKoy       : cell open / flag (all three modes)
 *   - zamanlayiciBaslat/Dur     : timer lifecycle
 *   - oyunuSifirla              : full game reset
 *   - kontrolEt / satranKontrolEt : win-loss detection
 *   - seviyeGecisiniGoster      : Leblebi level-transition overlay
 *   - oyunSonuPopupuGoster      : end-game score dialog
 *   - klasikZamanliSkorKaydet   : timed-classic score dialog
 *   - satranSkorKaydet          : chess score dialog
 *   - Screen-shake / flash animations
 *
 * It is constructed once in MinesweeperApp and wired to GameSceneBuilder
 * via setCallbacks().  It never creates UI itself — all rendering goes
 * through GameSceneBuilder.
 */
public class GameController {

    private static final SecureRandom RNG = new SecureRandom();
    private static final int KLASIK_CAN = 3;

    // ── Collaborators ─────────────────────────────────────────────────────────
    private final AppState         s;
    private final AppAssets        assets;
    private final AppTheme         theme;
    private final GameSceneBuilder builder;

    /** Called when the controller wants to show the menu. */
    private Runnable onMenuGoster;

    // ── Score de-dup guards ───────────────────────────────────────────────────
    private boolean satranSkorKaydedildi = false;

    // =========================================================================
    // Construction & wiring
    // =========================================================================

    public GameController(AppState state, AppAssets assets,
                          AppTheme theme, GameSceneBuilder builder) {
        this.s       = state;
        this.assets  = assets;
        this.theme   = theme;
        this.builder = builder;
    }

    public void setOnMenuGoster(Runnable r) { this.onMenuGoster = r; }

    /**
     * Wire this controller's callbacks into the scene builder so that every
     * button click routes through here.
     */
    public void registerCallbacks() {
        builder.setCallbacks(
                this::hucreAc,
                this::isaretKoy,
                this::oyunuSifirla,
                () -> { if (onMenuGoster != null) onMenuGoster.run(); },
                () -> { theme.applyCss(s.sahne); builder.temayiUygula(); arayuzuGuncelle(); },
                this::duraklatToggle
        );
        // Space bar pauses / resumes from anywhere in the game scene
        s.rootScene.setOnKeyPressed(ev -> {
            if (ev.getCode() == javafx.scene.input.KeyCode.SPACE) {
                duraklatToggle();
                ev.consume();
            }
        });
    }

    // =========================================================================
    // Pause / Resume
    // =========================================================================

    public void duraklatToggle() {
        // Cannot pause if the game isn't running
        boolean oyunCalisiyor;
        if (s.satranModu && s.chessBoardMode != null)
            oyunCalisiyor = s.chessBoardMode.isOyunAktif();
        else if (s.leblebModu && s.leblebiBoardMode != null)
            oyunCalisiyor = !s.leblebiBoardMode.isOyunBitti() && !s.leblebiBoardMode.isKazanildi();
        else
            oyunCalisiyor = s.klasikBoardMode != null && s.klasikBoardMode.isOyunAktif();

        if (!oyunCalisiyor && !s.oyunDuraksatildi) return;

        s.oyunDuraksatildi = !s.oyunDuraksatildi;

        if (s.oyunDuraksatildi) {
            if (s.zamanlayici != null) s.zamanlayici.pause();
            if (s.aktifBalonAnimasyonu != null) s.aktifBalonAnimasyonu.pause();
            builder.duraklatilaOverlayGoster();
            if (s.duraklatBtn != null) s.duraklatBtn.setText("▶");
        } else {
            if (s.zamanlayici != null) s.zamanlayici.play();
            if (s.aktifBalonAnimasyonu != null) s.aktifBalonAnimasyonu.play();
            builder.duraklatilaOverlayKaldir();
            if (s.duraklatBtn != null) s.duraklatBtn.setText("⏸");
        }
    }

    // =========================================================================
    // Cell interactions
    // =========================================================================

    public void hucreAc(int sr, int su) {
        // ── Chess mode ────────────────────────────────────────────────────────
        if (s.satranModu && s.chessBoardMode != null) {
            // Block left-click on flagged cells
            if (s.chessBoardMode.isFlagged(sr, su)) return;
            boolean mineHit = s.chessBoardMode.hucreAc(sr, su);
            if (mineHit) {
                assets.play(assets.sesPatlama);
                ekranSarsintisi();
                if (s.merkezIcerikKutusu != null) kirmiziFlasBas(s.merkezIcerikKutusu);
            } else {
                assets.playKazma();
            }
            s.maynSayaciEtiketi.setText("♟ " + s.chessBoardMode.getMineCount());
            arayuzuGuncelle();
            return;
        }

        // ── Leblebi mode ──────────────────────────────────────────────────────
        if (s.leblebModu && s.leblebiBoardMode != null) {
            // Block left-click on flagged cells (ilaç spray is exempt — it should clear flags)
            if (!s.leblebiBoardMode.isZirayiIlacAktif()
                    && s.leblebiBoardMode.getTahta().getHucre(sr, su).isIsaretlendi()) return;
            if (s.leblebiBoardMode.isZirayiIlacAktif()) {
                s.leblebiBoardMode.hucreAc(sr, su);
                refreshScoreLabels();
                s.leblebiBoardMode.kazanmaKontrol();
                assets.playKazma();
                arayuzuGuncelle();
                return;
            }
            boolean mineHit = s.leblebiBoardMode.hucreAc(sr, su);
            if (mineHit) {
                if (s.leblebiBoardMode.isOyunBitti()) {
                    assets.play(assets.sesYilanBitis != null ? assets.sesYilanBitis : assets.sesPatlama);
                } else {
                    assets.play(assets.sesYilan != null ? assets.sesYilan : assets.sesPatlama);
                }
                builder.canSayisiGuncelle(s.leblebiBoardMode.getCanSayisi());
                ekranSarsintisi();
                if (s.merkezIcerikKutusu != null) kirmiziFlasBas(s.merkezIcerikKutusu);
                builder.canIkonunuKir(s.leblebiBoardMode.getCanSayisi());
                if (!s.leblebiBoardMode.isOyunBitti()) {
                    s.zamanlayici.pause();
                    yilanUyarisiGoster(sr, su);
                } else {
                    s.leblebiBoardMode.diyalogTetikle(LeblebiBoardMode.DiyalogTetikleyici.KAYBETME);
                    builder.diyalogGoster(s.leblebiBoardMode.getAktifDiyalog());
                }
            } else {
                assets.playKazma();
                refreshScoreLabels();
            }
            arayuzuGuncelle();
            return;
        }

        // ── Classic mode ──────────────────────────────────────────────────────
        if (s.klasikBoardMode != null) {
            Board tahta = s.klasikBoardMode.getTahta();
            var hucre = tahta.getHucre(sr, su);
            // Block left-click on flagged cells — also prevents the false sesPatlama
            // that fired when wasMine was read before the no-op hucreAc returned
            if (hucre.isIsaretlendi()) return;
            boolean wasMine = hucre.isMayinMi();
            s.klasikBoardMode.hucreAc(sr, su);
            if (wasMine) assets.play(assets.sesPatlama); else assets.playKazma();
            arayuzuGuncelle();
        }
    }

    public void isaretKoy(int sr, int su) {
        if (s.satranModu && s.chessBoardMode != null) {
            s.chessBoardMode.isaretKoy(sr, su);
            arayuzuGuncelle();
            return;
        }
        Board tahta = aktifTahta();
        if (tahta == null) return;
        var hucre = tahta.getHucre(sr, su);
        if (hucre.isAcildiMi()) return;
        boolean isaretliydi = hucre.isIsaretlendi();
        if (!isaretliydi && s.yerlestirilenIsaret >= s.mayinSayisi) return;
        hucre.isaretiDegistir();
        s.yerlestirilenIsaret += isaretliydi ? -1 : 1;
        s.yerlestirilenIsaret = Math.max(0, s.yerlestirilenIsaret);
        // FIX: if this cell was karga-highlighted, clear the highlight so the
        // flag renders correctly instead of staying stuck on ⚠
        if (s.leblebModu && s.leblebiBoardMode != null && !isaretliydi) {
            var kargaList = s.leblebiBoardMode.getKargaGosterilenMayinlar();
            for (int[] k : kargaList) {
                if (k[0] == sr && k[1] == su) {
                    if (s.dugmeler != null) builder.stopKargaAnim(s.dugmeler[sr][su]);
                    s.hucreDurum[sr][su] = -1; // force redraw as flagged
                    break;
                }
            }
        }
        String simge = s.leblebModu ? "🐍 " : "💣 ";
        s.maynSayaciEtiketi.setText(simge + (s.mayinSayisi - s.yerlestirilenIsaret));
        arayuzuGuncelle();
    }

    // =========================================================================
    // UI refresh — delegates to GameSceneBuilder
    // =========================================================================

    public void arayuzuGuncelle() {
        builder.guncelleUstBar();
        renderGrid();
        kontrolEt();
        if (s.leblebModu) builder.gorevPanelGuncelle();
        builder.hucreBoyutlariniGuncelle();
        if (s.marketPanel != null) builder.marketPanelGuncelle();
    }

    // ── Grid rendering ────────────────────────────────────────────────────────

    private void renderGrid() {
        if (s.dugmeler == null) return;

        if (s.satranModu && s.chessBoardMode != null) {
            for (int r = 0; r < ChessBoardMode.BOARD_SIZE; r++)
                for (int c = 0; c < ChessBoardMode.BOARD_SIZE; c++)
                    updateChessCell(r, c);
            return;
        }

        Board tahta = aktifTahta();
        if (tahta == null) return;

        String[] sayiRenk = theme.sayiRenkleri(s);

        // Build O(1)-lookup set for karga targets instead of scanning the list per cell.
        java.util.Set<Integer> kargaSet = java.util.Collections.emptySet();
        if (s.leblebModu && s.leblebiBoardMode != null) {
            var kargaKonumlar = s.leblebiBoardMode.getKargaGosterilenMayinlar();
            if (!kargaKonumlar.isEmpty()) {
                kargaSet = new java.util.HashSet<>();
                for (int[] k : kargaKonumlar) kargaSet.add(k[0] * s.sutunSayisi + k[1]);
            }
        }
        final java.util.Set<Integer> kargaSetFinal = kargaSet;

        for (int r = 0; r < s.satirSayisi; r++) {
            for (int c = 0; c < s.sutunSayisi; c++) {
                var hucre = tahta.getHucre(r, c);

                boolean kargaHedef = !kargaSetFinal.isEmpty()
                        && !hucre.isAcildiMi()
                        && kargaSetFinal.contains(r * s.sutunSayisi + c);

                boolean yilanY = s.leblebModu && s.yilanHucreleri.contains(r * s.sutunSayisi + c);

                byte nyDurum;
                if      (yilanY)                               nyDurum = 5;
                else if (hucre.isAcildiMi() && hucre.isMayinMi()) nyDurum = 3;
                else if (hucre.isAcildiMi() && hucre.isGoldenLeblebi()) nyDurum = 20;
                else if (hucre.isAcildiMi())  nyDurum = (byte)(10 + hucre.getKomsuMayinSayisi());
                else if (hucre.isIsaretlendi()) nyDurum = 1;
                else if (kargaHedef)            nyDurum = 4;
                else                            nyDurum = 0;

                if (nyDurum == s.hucreDurum[r][c]) continue;
                byte eskiDurum = s.hucreDurum[r][c];
                s.hucreDurum[r][c] = nyDurum;
                var btn = s.dugmeler[r][c];

                switch (nyDurum) {
                    case 3 -> {
                        if (s.leblebModu) {
                            btn.setText(""); btn.setGraphic(builder.imgGraphicFill(assets.imgYilan, "🐍", 20, btn));
                        } else {
                            btn.setText(""); btn.setGraphic(builder.imgGraphicFill(assets.imgMayin, "💣", 20, btn));
                        }
                        btn.setStyle(theme.mayinHucreTarzi(s)); btn.setDisable(true);
                        if (eskiDurum != 3) boom(btn, 300);
                    }
                    case 20 -> {
                        // Golden Leblebi — shows neighbor count with golden styling
                        int k = hucre.getKomsuMayinSayisi();
                        btn.setText(k == 0 ? "" : String.valueOf(k));
                        btn.setGraphic(k == 0 ? builder.imgGraphicFill(assets.imgAltinLeblebi, "🌟", 18, btn) : null);

                        String goldenStyle =
                            "-fx-background-color: linear-gradient(to bottom right, #ffd700, #ff8c00);" +
                            "-fx-border-color: #daa520; -fx-border-width: 2.5;" +
                            "-fx-background-radius: 5; -fx-border-radius: 5;" +
                            "-fx-text-fill: #2a1500; -fx-font-weight: bold; -fx-font-size: " + builder.getLastYaz() + "px;";

                        btn.setStyle(goldenStyle);
                        btn.setDisable(true);

                        if (eskiDurum != 20 && s.leblebModu && s.leblebiBoardMode != null) {
                            s.leblebiBoardMode.altinLeblebiBulundu();
                            s.leblebiBoardMode.hucrePuaniEkle(9);
                            refreshScoreLabels();
                            altinLeblebiAnimasyonu(btn);
                        }
                    }
                    default -> {
                        if (nyDurum >= 10) {
                            btn.setGraphic(null);
                            int k = hucre.getKomsuMayinSayisi();
                            btn.setText(k == 0 ? "" : String.valueOf(k));
                            btn.setStyle(theme.acilmisHucreTarzi(s, k, sayiRenk, builder.getLastYaz()));
                            btn.setDisable(true);
                            if (eskiDurum < 10) { var st = new ScaleTransition(Duration.millis(150), btn); st.setFromX(0.85); st.setFromY(0.85); st.setToX(1); st.setToY(1); st.play(); }
                        } else if (nyDurum == 1) {
                            btn.setText(""); btn.setGraphic(builder.imgGraphic(assets.imgBayrak, "🚩", 18));
                            btn.setStyle(theme.isaretliHucreTarzi(s)); btn.setDisable(false);
                        } else if (nyDurum == 4) {
                            builder.stopKargaAnim(btn); // FIX: stop any previous pulse before starting a new one
                            btn.setText(""); btn.setGraphic(builder.emojiLabel("⚠", 16));
                            btn.setStyle(theme.acilmamisHucreTarzi(s) +
                                         "-fx-border-color:" + AppTheme.LB_KARGA_RENK + ";-fx-border-width:2.5;");
                            btn.setDisable(false);
                            var ft = new FadeTransition(Duration.millis(600), btn);
                            ft.setFromValue(0.5); ft.setToValue(1.0);
                            ft.setCycleCount(Animation.INDEFINITE); ft.setAutoReverse(true);
                            btn.setUserData(ft);
                            ft.play();
                        } else if (nyDurum == 5) {
                            btn.setText(""); btn.setGraphic(builder.imgGraphicFill(assets.imgYilan, "🐍", 20, btn));
                            btn.setStyle("-fx-background-color:#8B0000;-fx-border-color:#ff4444;-fx-border-width:2;" +
                                         "-fx-background-radius:4;-fx-border-radius:4;-fx-padding:0;");
                            btn.setDisable(false);
                        } else {
                            btn.setGraphic(null); btn.setText("");
                            btn.setStyle(theme.acilmamisHucreTarzi(s, r, c)); btn.setDisable(false);
                        }
                    }
                }
            }
        }
    }

    // ── Chess cell rendering ──────────────────────────────────────────────────

    private void updateChessCell(int r, int c) {
        var btn      = s.dugmeler[r][c];
        boolean rev  = s.chessBoardMode.isRevealed(r, c);
        boolean flag = s.chessBoardMode.isFlagged(r, c);
        boolean mine = s.chessBoardMode.isMine(r, c);
        int threat   = s.chessBoardMode.getThreat(r, c);

        byte nyDurum;
        if      (rev && mine) nyDurum = 3;
        else if (rev)         nyDurum = (byte)(10 + Math.min(threat, 8));
        else if (flag)        nyDurum = 1;
        else                  nyDurum = (byte)((r + c) % 2 == 0 ? 0 : 7);

        if (nyDurum == s.hucreDurum[r][c]) return;
        byte eskiDurum = s.hucreDurum[r][c];
        s.hucreDurum[r][c] = nyDurum;

        if (rev && mine) {
            ChessMine mine2 = s.chessBoardMode.getMine(r, c);
            var sym = builder.emojiLabel(mine2 != null ? mine2.getSymbol() : "💥", 38);
            btn.setText(""); btn.setGraphic(sym);
            btn.setStyle("-fx-background-color:#c0392b;-fx-background-radius:7;-fx-border-radius:7;");
            btn.setDisable(true);
            if (eskiDurum != 3) boom(btn, 400);
            btn.setScaleX(1.0); btn.setScaleY(1.0);
        } else if (rev) {
            btn.setGraphic(null);
            btn.setText(threat == 0 ? "" : String.valueOf(threat));
            String fg = (threat > 0 && threat <= 8) ? AppTheme.CH_SAYI_RENK[threat] : "#4a6a8a";
            btn.setStyle("-fx-background-color:" + AppTheme.CH_ACILMIS + ";" +
                         "-fx-border-color:" + AppTheme.CH_CERCEVE + ";-fx-border-width:1;" +
                         "-fx-background-radius:4;-fx-border-radius:4;" +
                         "-fx-text-fill:" + fg + ";-fx-font-weight:bold;-fx-font-size:" + builder.getLastYaz() + "px;");
            btn.setDisable(true);
            if (eskiDurum <= 0 || eskiDurum == 7) {
                var ft = new FadeTransition(Duration.millis(180), btn);
                ft.setFromValue(0.3); ft.setToValue(1.0); ft.play();
            }
        } else if (flag) {
            btn.setText(""); btn.setGraphic(builder.imgGraphic(assets.imgBayrak, "🚩", 20));
            btn.setStyle("-fx-background-color:" + AppTheme.CH_ISARETLI + ";" +
                         "-fx-border-color:#8060a0;-fx-border-width:1;" +
                         "-fx-background-radius:4;-fx-border-radius:4;");
            btn.setDisable(false);
        } else {
            btn.setGraphic(null); btn.setText("");
            boolean light = (r + c) % 2 == 0;
            btn.setStyle("-fx-background-color:" + (light ? AppTheme.CH_ACILMAMIS_LIGHT : AppTheme.CH_ACILMAMIS_DARK) + ";" +
                         "-fx-border-color:" + AppTheme.CH_CERCEVE + ";-fx-border-width:1;" +
                         "-fx-background-radius:4;-fx-border-radius:4;-fx-cursor:hand;");
            btn.setDisable(false);
        }
    }

    // =========================================================================
    // Win / loss detection
    // =========================================================================

    private void kontrolEt() {
        if (s.satranModu) { satranKontrolEt(); return; }

        if (s.leblebModu && s.leblebiBoardMode != null) {
            s.leblebiBoardMode.kazanmaKontrol();
            if (s.leblebiBoardMode.isKazanildi() && !s.popupGosterildi) {
                s.zamanlayici.stop(); assets.stopBgm(); assets.play(assets.sesKazan); setSifirlaBtnGraphic(assets.imgResetBtn, "🎉");
                s.durumEtiketi.setText("🫘 Tüm yılanlar bulundu!");
                s.durumEtiketi.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#c89a2a;");
                s.toplamLeblebPuani += s.leblebiBoardMode.getLeblebPuani();
                s.leblebiBoardMode.diyalogTetikle(LeblebiBoardMode.DiyalogTetikleyici.KAZANMA);
                builder.diyalogGoster(s.leblebiBoardMode.getAktifDiyalog());
                konfetiAnimasyonu();
                s.popupGosterildi = true;
                javafx.application.Platform.runLater(this::seviyeGecisiniGoster);
            } else if (s.leblebiBoardMode.isOyunBitti() && !s.leblebiBoardMode.isKazanildi() && !s.popupGosterildi) {
                s.zamanlayici.stop(); assets.stopBgm(); setSifirlaBtnGraphic(assets.imgOyunBitti, "🪱");
                String msg = s.leblebiBoardMode.getKalanSure() <= 0
                        ? "⏰ Süre doldu! Mehmet Emmi üzüldü..." : "💀 Canların bitti! Yılanlar kazandı!";
                s.durumEtiketi.setText(msg);
                s.durumEtiketi.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#e74c3c;");
                s.leblebiBoardMode.diyalogTetikle(LeblebiBoardMode.DiyalogTetikleyici.KAYBETME);
                builder.diyalogGoster(s.leblebiBoardMode.getAktifDiyalog());
                s.popupGosterildi = true;
                javafx.application.Platform.runLater(this::oyunSonuPopupuGoster);
            }
            return;
        }

        if (s.klasikBoardMode != null) {
            if (s.klasikBoardMode.isKazanildi() && !s.popupGosterildi) {
                s.popupGosterildi = true;
                s.zamanlayici.stop(); assets.stopBgm(); assets.play(assets.sesKazan); setSifirlaBtnGraphic(assets.imgResetBtn, "😎");
                String win = s.klasikBoardMode.isGeriSayim()
                        ? "★ Kazandınız! (" + s.klasikBoardMode.getSuruclukSure() + "s kaldı)"
                        : "★ Kazandınız! (" + s.klasikBoardMode.getSuruclukSure() + "s)";
                s.durumEtiketi.setText(win);
                s.durumEtiketi.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:" +
                        (s.karanlikTema ? "#a6e3a1" : "#2e7d32") + ";");
                konfetiAnimasyonu();
                if (s.klasikBoardMode.isGeriSayim())
                    javafx.application.Platform.runLater(this::klasikZamanliSkorKaydet);
            } else if (s.klasikBoardMode.isOyunBitti() && !s.klasikBoardMode.isSureDoldu()
                       && !s.klasikBoardMode.isKazanildi() && !s.popupGosterildi) {
                s.popupGosterildi = true;
                s.zamanlayici.stop(); assets.stopBgm(); assets.play(assets.sesPatlama); setSifirlaBtnGraphic(assets.imgOyunBitti, "😵");
                s.durumEtiketi.setText("✖ Oyun Bitti!");
                s.durumEtiketi.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:" +
                        (s.karanlikTema ? "#f38ba8" : "#c62828") + ";");
            }
        }
    }

    private void satranKontrolEt() {
        if (s.chessBoardMode == null) return;
        s.chessBoardMode.kazanmaKontrol();

        if (s.chessBoardMode.isKazanildi() && !s.satranSkorKaydedildi) {
            s.zamanlayici.stop(); assets.stopBgm(); assets.play(assets.sesKazan); setSifirlaBtnGraphic(assets.imgResetBtn, "🏆");
            s.durumEtiketi.setText("♟ Tüm güvenli kareler açıldı! Kazandın!");
            s.durumEtiketi.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:#f0c040;");
            konfetiAnimasyonu();
            javafx.application.Platform.runLater(this::satranSkorKaydet);
        } else if (s.chessBoardMode.isOyunBitti() && !s.chessBoardMode.isSureDoldu()
                   && !s.chessBoardMode.isKazanildi() && !s.popupGosterildi) {
            s.popupGosterildi = true;
            s.zamanlayici.stop(); assets.stopBgm(); assets.play(assets.sesPatlama); setSifirlaBtnGraphic(assets.imgKafatasi, "💀");
            s.durumEtiketi.setText("💥 Bir satranç taşına bastın!");
            s.durumEtiketi.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:#e74c3c;");
        }
    }

    // =========================================================================
    // Timer
    // =========================================================================

    public void zamanlayiciBaslat() {
        if (s.zamanlayici != null) s.zamanlayici.stop();
        s.zamanlayici = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(Duration.seconds(1), e -> timerTick()));
        s.zamanlayici.setCycleCount(Animation.INDEFINITE);
        s.zamanlayici.play();
    }

    private void timerTick() {
        // Chess
        if (s.satranModu && s.chessBoardMode != null) {
            boolean timesUp = s.chessBoardMode.sureyiGuncelle(1);
            int kalan = s.chessBoardMode.getKalanSure();
            s.zamanlayiciEtiketi.setGraphic(builder.imgGraphic(assets.imgKumSaati, "⏳", 24));
            s.zamanlayiciEtiketi.setText(" " + kalan + "s");
            s.zamanlayiciEtiketi.setStyle(timerLabelStil(kalan));
            if (timesUp) {
                s.zamanlayici.stop(); assets.stopBgm(); setSifirlaBtnGraphic(assets.imgKafatasi, "💀");
                s.durumEtiketi.setText("⏰ Süre Doldu! Taşlar tarlayı işgal etti!");
                s.durumEtiketi.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:#e74c3c;");
                assets.play(assets.sesPatlama);
            }
            arayuzuGuncelle();
            return;
        }
        // Leblebi
        if (s.leblebModu && s.leblebiBoardMode != null) {
            s.leblebiBoardMode.sureyiGuncelle(1);
            int kalan = s.leblebiBoardMode.getKalanSure();
            s.zamanlayiciEtiketi.setGraphic(builder.imgGraphic(assets.imgKumSaati, "⏳", 24));
            s.zamanlayiciEtiketi.setText(" " + kalan + "s");
            s.zamanlayiciEtiketi.setStyle(timerLabelStil(kalan));
            if (s.leblebiBoardMode.isOyunBitti() && !s.leblebiBoardMode.isKazanildi()) {
                s.zamanlayici.stop(); assets.stopBgm(); setSifirlaBtnGraphic(assets.imgOyunBitti, "😵");
                s.leblebiBoardMode.diyalogTetikle(LeblebiBoardMode.DiyalogTetikleyici.KAYBETME);
                builder.diyalogGoster(s.leblebiBoardMode.getAktifDiyalog());
                arayuzuGuncelle();
            }
            return;
        }
        // Classic
        if (s.klasikBoardMode != null) {
            if (s.klasikBoardMode.isGeriSayim()) {
                boolean timesUp = s.klasikBoardMode.sureyiGuncelle(1);
                int kalan = s.klasikBoardMode.getSuruclukSure();
                s.zamanlayiciEtiketi.setGraphic(builder.imgGraphic(assets.imgKumSaati, "⏳", 24));
                s.zamanlayiciEtiketi.setText(" " + kalan + "s");
                s.zamanlayiciEtiketi.setStyle(timerLabelStil(kalan));
                if (timesUp) {
                    s.zamanlayici.stop(); assets.stopBgm();
                    s.durumEtiketi.setText("⏰ Süre Doldu! Tüm Mayınlar Patladı!");
                    s.durumEtiketi.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:#f38ba8;");
                    setSifirlaBtnGraphic(assets.imgOyunBitti, "😵"); assets.play(assets.sesPatlama);
                    arayuzuGuncelle();
                    if (s.dugmeler != null)
                        for (var row : s.dugmeler) for (var btn : row) btn.setDisable(true);
                }
            } else {
                s.klasikBoardMode.sureyiGuncelle(1);
                s.zamanlayiciEtiketi.setGraphic(builder.imgGraphic(assets.imgAlarm2, "⏱", 24));
                s.zamanlayiciEtiketi.setText(" " + s.klasikBoardMode.getSuruclukSure() + "s");
            }
        }
    }

    private String timerLabelStil(int kalan) {
        boolean flash = (kalan % 2 == 0);
        String fg = kalan <= 10 ? (flash ? "#e74c3c" : "#ffffff") : "#cdd6f4";
        return "-fx-font-size:19px;-fx-font-weight:bold;-fx-padding:4 12 4 12;-fx-background-radius:8;-fx-text-fill:" + fg + ";";
    }

    // =========================================================================
    // Reset
    // =========================================================================

    public void oyunuSifirla() {
        if (s.zamanlayici != null) s.zamanlayici.stop();
        if (s.aktifBalonAnimasyonu != null) { s.aktifBalonAnimasyonu.stop(); s.aktifBalonAnimasyonu = null; }
        if (s.konusmaBalonuPanel != null) s.konusmaBalonuPanel.setVisible(false);

        s.yerlestirilenIsaret = 0;
        s.yilanHucreleri.clear();
        s.durumEtiketi.setText("");
        s.popupGosterildi = false;
        s.satranSkorKaydedildi = false;
        satranSkorKaydedildi = false;
        // Clear any active pause
        s.oyunDuraksatildi = false;
        builder.duraklatilaOverlayKaldir();
        if (s.duraklatBtn != null) s.duraklatBtn.setText("⏸");

        String hudL = "-fx-font-size:19px;-fx-font-weight:bold;-fx-padding:4 12 4 12;-fx-background-radius:8;";
        String hudP = "-fx-font-size:18px;-fx-font-weight:bold;-fx-text-fill:#f0c040;-fx-padding:4 12 4 12;-fx-background-radius:8;";
        s.zamanlayiciEtiketi.setStyle(hudL);
        s.puanEtiketi.setStyle(hudP);
        if (s.altinEtiketi != null) s.altinEtiketi.setStyle(hudP);
        s.canEtiketi.setStyle(hudL + "-fx-text-fill:#ff6b6b;");
        s.durumEtiketi.setStyle("-fx-font-size:15px;-fx-font-weight:bold;");
        if (s.leblebModu) {
            setSifirlaBtnGraphic(assets.imgLeblebi, "🌾");
        } else if (s.satranModu) {
            setSifirlaBtnGraphic(assets.imgMutlu, "♟");
        } else {
            setSifirlaBtnGraphic(assets.imgMutlu, "😊");
        }

        if (s.satranModu && s.chessBoardMode != null) {
            s.chessBoardMode = new ChessBoardMode(s.chessBoardMode.getDifficulty(), s.chessBoardMode.getBaslangicSuresi());
            s.mayinSayisi = s.chessBoardMode.getMineCount();
        } else if (s.leblebModu) {
            int harcanan = s.leblebiBoardMode != null ? s.leblebiBoardMode.getHarcananAltin() : 0;
            s.kaliciAltin = Math.max(0, s.kaliciAltin - harcanan);
            s.leblebiBoardMode = new LeblebiBoardMode(s.satirSayisi, s.sutunSayisi, s.mayinSayisi,
                    Seviye.getSeviye(s.mevcutSeviye).getSureSaniye(), KLASIK_CAN,
                    s.kaliciAltin, s.toplamKargaKullanim, s.toplamIlacKullanim);
        } else if (s.klasikBoardMode != null) {
            s.klasikBoardMode = new KlasikBoardMode(s.satirSayisi, s.sutunSayisi, s.mayinSayisi,
                    s.klasikBoardMode.isGeriSayim(), s.klasikBoardMode.getBaslangicSuresi());
        }

        builder.guncelleUstBar();
        builder.izgarayiOlustur();

        if (s.leblebModu) {
            builder.marketPanelOlustur();
            s.kokDuzen.setRight(s.marketPanel);
            builder.marketPanelGuncelle();
            builder.canSayisiGuncelle(s.leblebiBoardMode.getCanSayisi());
        } else if (s.satranModu) {
            builder.satranBilgiBariOlustur();
            s.kokDuzen.setRight(null); s.kokDuzen.setLeft(null);
        } else {
            s.kokDuzen.setRight(null);
        }

        builder.temayiUygula();
        zamanlayiciBaslat();
        // FIX: restart the BGM after reset — stopBgm() was called on game-over
        // so without this the music stays silent for the new game
        String bgmMod = s.satranModu ? "satranc" : s.leblebModu ? "leblebi" : "klasik";
        assets.playBgm(bgmMod);
        arayuzuGuncelle();
        builder.hucreBoyutlariniGuncelle();
    }

    // =========================================================================
    // Score dialogs
    // =========================================================================

    private void satranSkorKaydet() {
        if (satranSkorKaydedildi || s.chessBoardMode == null) return;
        satranSkorKaydedildi = true;
        s.satranSkorKaydedildi = true;

        int skor = s.chessBoardMode.skorHesapla();
        String zorlukAdi = switch (s.chessBoardMode.getDifficulty()) {
            case 1 -> "Kolay"; case 2 -> "Orta"; default -> "Zor";
        };
        var dlg = new TextInputDialog("Oyuncu");
        dlg.setTitle("Satranç Modu Skoru");
        dlg.setHeaderText("♟ Tebrikler! Satranç Mayın Tarlasını Tamamladın!");
        dlg.setContentText(String.format("Zorluk: %s  |  Kalan süre: %ds%n🏆 Skor: %d%n%nİsminizi girin:",
                zorlukAdi, s.chessBoardMode.getKalanSure(), skor));
        theme.styleDialog(dlg, false);
        dlg.showAndWait().ifPresent(isim -> {
            if (!isim.isBlank())
                SkorTablosu.kaydet(isim.trim(), skor, s.chessBoardMode.getDifficulty(), "satranç",
                        ChessBoardMode.BOARD_SIZE, ChessBoardMode.BOARD_SIZE,
                        s.chessBoardMode.getMineCount(), s.chessBoardMode.getBaslangicSuresi());
        });
        satranSkorKaydedildi = false;
        s.satranSkorKaydedildi = false;
    }

    private void klasikZamanliSkorKaydet() {
        if (s.klasikBoardMode == null || s.klasikBoardMode.isSkorKaydedildi()) return;
        s.klasikBoardMode.setSkorKaydedildi(true);

        int skor      = s.klasikBoardMode.skorHesapla();
        int kalan     = s.klasikBoardMode.getSuruclukSure();
        int gecen     = s.klasikBoardMode.getBaslangicSuresi() - kalan;
        double yog    = (double) s.mayinSayisi / (s.satirSayisi * s.sutunSayisi) * 100;
        String preset = s.klasikBoardMode.presetEtiketiBul();
        String seviyeGoster = preset.isBlank()
                ? s.satirSayisi + "×" + s.sutunSayisi + ", " + s.mayinSayisi + " mayın" : preset;

        var dlg = new TextInputDialog("Oyuncu");
        dlg.setTitle("Skor Tablosu");
        dlg.setHeaderText("⏱ Tebrikler! Zamanlı Modu Kazandınız!");
        dlg.setContentText(String.format(
                "Zorluk: %s%nIzgara: %d×%d  |  Mayın: %d  |  Süre limiti: %ds%n" +
                "Geçen süre: %ds  |  Kalan süre: %ds%nMayın yoğunluğu: %.0f%%%n%n🏆 Skor: %d%n%nİsminizi girin:",
                seviyeGoster, s.satirSayisi, s.sutunSayisi, s.mayinSayisi,
                s.klasikBoardMode.getBaslangicSuresi(), gecen, kalan, yog, skor));
        theme.styleDialog(dlg, false);
        dlg.showAndWait().ifPresent(isim -> {
            if (!isim.isBlank())
                SkorTablosu.kaydet(isim.trim(), skor, 0, SkorTablosu.MOD_ZAMANLI,
                        s.satirSayisi, s.sutunSayisi, s.mayinSayisi, s.klasikBoardMode.getBaslangicSuresi());
        });
        s.klasikBoardMode.setSkorKaydedildi(false);
    }

    private void oyunSonuPopupuGoster() {
        int finalSkor = (s.leblebiBoardMode != null)
                ? s.toplamLeblebPuani * 10 + s.leblebiBoardMode.getKalanSure() * 5
                : (s.klasikBoardMode != null ? s.klasikBoardMode.getSuruclukSure() : 0);
        var dlg = new TextInputDialog("Tarımcı");
        dlg.setTitle("Skor Tablosu"); dlg.setHeaderText("🏆 Oyun Bitti!");
        dlg.setContentText(String.format("Skor: %d\nİsminizi girin:", finalSkor));
        theme.styleDialog(dlg, false);
        dlg.showAndWait().ifPresent(isim -> {
            if (!isim.isBlank())
                SkorTablosu.kaydet(isim.trim(), finalSkor, s.mevcutSeviye);
        });
        s.popupGosterildi = false;
        if (onMenuGoster != null) onMenuGoster.run();
    }

    // =========================================================================
    // Level transition overlay (Leblebi)
    // =========================================================================

    private void seviyeGecisiniGoster() {
        boolean sonSeviye = Seviye.sonSeviyeMi(s.mevcutSeviye);
        var rapor = s.leblebiBoardMode.hasatRaporuOlustur();

        var overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.75);");

        var fis = new VBox(15);
        fis.setMaxWidth(400); fis.setMaxHeight(VBox.USE_PREF_SIZE);
        fis.setPadding(new Insets(25));
        fis.setStyle("-fx-background-color:#fcf8e3;-fx-border-color:#d4b070;-fx-border-width:3;" +
                     "-fx-border-radius:8;-fx-background-radius:8;" +
                     "-fx-effect:dropshadow(three-pass-box,rgba(0,0,0,0.6),15,0,0,5);");
        fis.setAlignment(Pos.TOP_CENTER);

        var baslikLbl = new Label(sonSeviye ? "🏆 OYUN TAMAMLANDI" : "✅ HASAT RAPORU");
        baslikLbl.setStyle("-fx-font-size:24px;-fx-font-weight:bold;-fx-text-fill:#5c4033;");
        var altLbl = new Label(sonSeviye ? "Mehmet Emmi çok mutlu!" : "Seviye " + s.mevcutSeviye + " Başarıyla Bitti!");
        altLbl.setStyle("-fx-font-size:14px;-fx-text-fill:#8b5a2b;-fx-font-style:italic;");

        java.util.function.BiFunction<String,String,HBox> satirOlustur = (sol, sag) -> {
            var sl = new Label(sol); var sr = new Label(sag); sr.setStyle("-fx-font-weight:bold;");
            var r  = new Region(); HBox.setHgrow(r, Priority.ALWAYS);
            return new HBox(sl, r, sr);
        };

        var detaylar = new VBox(8);
        detaylar.getChildren().addAll(
                satirOlustur.apply("Zaman Bonusu:", "+" + rapor.surePuani()),
                satirOlustur.apply("Açılan Hücreler (" + rapor.acilanHucreSayisi() + "):", "+" + rapor.hucrePuani()),
                satirOlustur.apply("Yok Edilen Yılanlar (" + rapor.yokEdilenYilan() + "):", "+" + rapor.yilanPuani()));
        if (rapor.altinLeblebiBulundu() > 0)
            detaylar.getChildren().add(
                    satirOlustur.apply("Altın Leblebi (" + rapor.altinLeblebiBulundu() + "):", "+" + (rapor.altinLeblebiBulundu() * 10)));

        var ekstralar = new VBox(10); ekstralar.setAlignment(Pos.CENTER_LEFT);
        if (!rapor.tamamlananGorevler().isEmpty()) {
            var gb = new Label("📜 Tamamlanan Görevler");
            gb.setStyle("-fx-font-weight:bold;-fx-text-fill:#2e7d32;");
            ekstralar.getChildren().add(gb);
            for (var g : rapor.tamamlananGorevler()) {
                var gl = new Label("✔ " + g.aciklama); gl.setWrapText(true);
                gl.setStyle("-fx-text-fill:#388e3c;-fx-font-size:13px;");
                ekstralar.getChildren().add(gl);
            }
        }

        var toplamLbl = new Label("TOPLAM KAZANÇ: " + s.leblebiBoardMode.getLeblebPuani() + " Puan");
        toplamLbl.setStyle("-fx-font-size:18px;-fx-font-weight:bold;-fx-text-fill:#d84315;-fx-padding:10 0 0 0;");
        var emmiLbl = new Label("💬 " + rapor.emmiYorumu());
        emmiLbl.setWrapText(true);
        emmiLbl.setStyle("-fx-font-size:14px;-fx-font-style:italic;-fx-text-fill:#5d4037;" +
                         "-fx-background-color:#d7ccc8;-fx-padding:10;-fx-background-radius:5;");

        var butonlar = new HBox(15); butonlar.setAlignment(Pos.CENTER); butonlar.setPadding(new Insets(15,0,0,0));
        var btnSonraki = new Button(sonSeviye ? "🏅 Skoru Kaydet" : "▶ Sonraki Bölüm");
        btnSonraki.setStyle("-fx-background-color:#4caf50;-fx-text-fill:white;-fx-font-size:16px;" +
                            "-fx-font-weight:bold;-fx-padding:8 20;-fx-cursor:hand;-fx-background-radius:5;");
        btnSonraki.setOnAction(e -> {
            if (s.anaSahneKoku != null) s.anaSahneKoku.getChildren().remove(overlay);
            s.popupGosterildi = false;
            if (sonSeviye) { oyunSonuPopupuGoster(); }
            else {
                s.kaliciAltin        = s.leblebiBoardMode.getAltin();
                s.toplamKargaKullanim = s.leblebiBoardMode.getKargaKullanimToplami();
                s.toplamIlacKullanim  = s.leblebiBoardMode.getIlacKullanimToplami();
                s.mevcutSeviye++;
                leblebOyunuBaslat();
            }
        });
        var btnMenu = new Button("Menüye Dön");
        btnMenu.setStyle("-fx-background-color:#e0e0e0;-fx-text-fill:#424242;-fx-font-size:14px;" +
                         "-fx-padding:8 15;-fx-cursor:hand;-fx-background-radius:5;");
        btnMenu.setOnAction(e -> {
            if (s.anaSahneKoku != null) s.anaSahneKoku.getChildren().remove(overlay);
            s.popupGosterildi = false;
            if (onMenuGoster != null) onMenuGoster.run();
        });
        butonlar.getChildren().addAll(btnMenu, btnSonraki);

        fis.getChildren().addAll(baslikLbl, altLbl, new Separator(), detaylar, new Separator());
        if (!ekstralar.getChildren().isEmpty()) {
            fis.getChildren().add(ekstralar); fis.getChildren().add(new Separator());
        }
        fis.getChildren().addAll(toplamLbl, emmiLbl, butonlar);
        overlay.getChildren().add(fis);

        fis.setTranslateY(-50); fis.setOpacity(0);
        var tt = new TranslateTransition(Duration.millis(500), fis); tt.setToY(0);
        var ft = new FadeTransition(Duration.millis(500), fis);      ft.setToValue(1.0);
        new ParallelTransition(tt, ft).play();

        if (s.anaSahneKoku != null) s.anaSahneKoku.getChildren().add(overlay);
        else s.kokDuzen.setCenter(overlay);
    }

    // =========================================================================
    // Leblebi level start — called by seviyeGecisiniGoster & reset
    // =========================================================================

    public void leblebOyunuBaslat() {
        s.yilanHucreleri.clear();
        s.popupGosterildi = false;
        s.oyunDuraksatildi = false;
        if (s.duraklatBtn != null) s.duraklatBtn.setText("⏸");
        Seviye seviye = Seviye.getSeviye(s.mevcutSeviye);
        s.satirSayisi = seviye.getSatirSayisi();
        s.sutunSayisi = seviye.getSutunSayisi();
        s.mayinSayisi = seviye.getYilanSayisi();
        s.klasikBoardMode = null; s.chessBoardMode = null;
        s.leblebiBoardMode = new LeblebiBoardMode(
                s.satirSayisi, s.sutunSayisi, s.mayinSayisi,
                seviye.getSureSaniye(), KLASIK_CAN,
                s.kaliciAltin, s.toplamKargaKullanim, s.toplamIlacKullanim);
        builder.oyunSahnesiniBaSlat(true, false);
        registerCallbacks();
        assets.playBgm("leblebi");
        zamanlayiciBaslat();
        arayuzuGuncelle();
        s.leblebiBoardMode.diyalogTetikle(LeblebiBoardMode.DiyalogTetikleyici.LEVEL_BASI);
        javafx.application.Platform.runLater(() -> builder.diyalogGoster(s.leblebiBoardMode.getAktifDiyalog()));
    }

    // =========================================================================
    // Snake warning overlay (Leblebi)
    // =========================================================================

    private void yilanUyarisiGoster(int satir, int sutun) {
        s.yilanHucreleri.add(satir * s.sutunSayisi + sutun);
        if (s.merkezIcerikKutusu != null) {
            var flash = new Rectangle();
            flash.widthProperty().bind(s.merkezIcerikKutusu.widthProperty());
            flash.heightProperty().bind(s.merkezIcerikKutusu.heightProperty());
            flash.setFill(javafx.scene.paint.Color.RED); flash.setOpacity(0.4);
            flash.setMouseTransparent(true);
            s.merkezIcerikKutusu.getChildren().add(flash);
            var ft = new FadeTransition(Duration.millis(300), flash); ft.setToValue(0);
            ft.setOnFinished(e -> {
                flash.widthProperty().unbind(); flash.heightProperty().unbind();
                s.merkezIcerikKutusu.getChildren().remove(flash);
            });
            ft.play();
        }
        ekranSarsintisi();
        if (s.leblebiBoardMode != null) {
            s.leblebiBoardMode.diyalogTetikle(LeblebiBoardMode.DiyalogTetikleyici.CAN_KAYBI);
            builder.diyalogGoster(s.leblebiBoardMode.getAktifDiyalog());
        }
        s.zamanlayici.play();
    }

    // =========================================================================
    // Animations
    // =========================================================================

    private void ekranSarsintisi() {
        if (s.merkezIcerikKutusu == null) return;
        if (s.aktifSarsinti != null) { s.aktifSarsinti.stop(); s.merkezIcerikKutusu.setScaleX(1.0); }
        s.aktifSarsinti = new javafx.animation.Timeline(
                kv(0,   s.merkezIcerikKutusu, 1.00),
                kv(40,  s.merkezIcerikKutusu, 0.97),
                kv(80,  s.merkezIcerikKutusu, 1.03),
                kv(120, s.merkezIcerikKutusu, 0.97),
                kv(160, s.merkezIcerikKutusu, 1.03),
                kv(200, s.merkezIcerikKutusu, 0.98),
                kv(240, s.merkezIcerikKutusu, 1.02),
                kv(280, s.merkezIcerikKutusu, 1.00));
        s.aktifSarsinti.setOnFinished(e -> { s.merkezIcerikKutusu.setScaleX(1.0); s.aktifSarsinti = null; });
        s.aktifSarsinti.play();
    }

    private javafx.animation.KeyFrame kv(int ms, StackPane node, double val) {
        return new javafx.animation.KeyFrame(Duration.millis(ms),
                new javafx.animation.KeyValue(node.scaleXProperty(), val));
    }

    private void kirmiziFlasBas(Pane kokPanel) {
        var overlay = new Rectangle();
        overlay.setFill(javafx.scene.paint.Color.web("#e74c3c")); overlay.setOpacity(0);
        overlay.setMouseTransparent(true);
        overlay.widthProperty().bind(kokPanel.widthProperty());
        overlay.heightProperty().bind(kokPanel.heightProperty());
        kokPanel.getChildren().add(overlay);
        var flash = new javafx.animation.Timeline(
                kfO(0,   overlay, 0.0),
                kfO(120, overlay, 0.5),
                kfO(240, overlay, 0.0),
                kfO(360, overlay, 0.5),
                kfO(480, overlay, 0.0),
                kfO(600, overlay, 0.5),
                kfO(720, overlay, 0.0));
        flash.setOnFinished(e -> {
            overlay.widthProperty().unbind(); overlay.heightProperty().unbind();
            kokPanel.getChildren().remove(overlay);
        });
        flash.play();
    }

    private javafx.animation.KeyFrame kfO(int ms, Rectangle r, double op) {
        return new javafx.animation.KeyFrame(Duration.millis(ms),
                new javafx.animation.KeyValue(r.opacityProperty(), op));
    }

    private void boom(javafx.scene.control.Button btn, int ms) {
        var st = new ScaleTransition(Duration.millis(ms), btn);
        st.setFromX(0.5); st.setFromY(0.5); st.setToX(1.0); st.setToY(1.0);
        st.play();

        var merkez = s.merkezIcerikKutusu;
        if (merkez == null) return;

        // Ekranda devasa bir patlama emojisi belirip kaybolsun
        Label patlamaEfekti = new Label(s.leblebModu ? "🐍" : "💥");
        if (s.leblebModu && assets.imgYilan != null) {
            javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(assets.imgYilan);
            iv.setFitWidth(80); iv.setFitHeight(80); iv.setPreserveRatio(true); iv.setSmooth(true);
            patlamaEfekti.setText("");
            patlamaEfekti.setGraphic(iv);
        }
        patlamaEfekti.setStyle("-fx-font-size: 80px; -fx-effect: dropshadow(gaussian,rgba(255,0,0,0.8),20,0,0,0);");
        patlamaEfekti.setMouseTransparent(true);

        var bb = btn.localToScene(btn.getBoundsInLocal());
        var sb = merkez.sceneToLocal(bb);
        patlamaEfekti.setTranslateX(sb.getMinX() - merkez.getWidth() / 2 + btn.getWidth() / 2);
        patlamaEfekti.setTranslateY(sb.getMinY() - merkez.getHeight() / 2 + btn.getHeight() / 2);

        merkez.getChildren().add(patlamaEfekti);

        ScaleTransition st2 = new ScaleTransition(Duration.millis(500), patlamaEfekti);
        st2.setFromX(0.1); st2.setFromY(0.1); st2.setToX(2.5); st2.setToY(2.5);
        FadeTransition ft = new FadeTransition(Duration.millis(500), patlamaEfekti);
        ft.setFromValue(1.0); ft.setToValue(0.0);

        ParallelTransition pt = new ParallelTransition(st2, ft);
        pt.setOnFinished(e -> merkez.getChildren().remove(patlamaEfekti));
        pt.play();
    }

    public void devPatlamaTest() {
        var merkez = s.merkezIcerikKutusu;
        if (merkez == null) return;

        assets.play(assets.sesPatlama);
        ekranSarsintisi();
        kirmiziFlasBas(merkez);

        Label patlamaEfekti = new Label("💥");
        patlamaEfekti.setStyle("-fx-font-size: 150px; -fx-effect: dropshadow(gaussian,rgba(255,0,0,1.0),40,0,0,0);");
        patlamaEfekti.setMouseTransparent(true);
        merkez.getChildren().add(patlamaEfekti);

        ScaleTransition st = new ScaleTransition(Duration.millis(600), patlamaEfekti);
        st.setFromX(0.1); st.setFromY(0.1); st.setToX(3.0); st.setToY(3.0);
        FadeTransition ft = new FadeTransition(Duration.millis(600), patlamaEfekti);
        ft.setFromValue(1.0); ft.setToValue(0.0);

        ParallelTransition pt = new ParallelTransition(st, ft);
        pt.setOnFinished(e -> merkez.getChildren().remove(patlamaEfekti));
        pt.play();
    }

    public void devAltinTest() {
        var merkez = s.merkezIcerikKutusu;
        if (merkez == null) return;
        var lbl = builder.emojiLabel("+10 🌟", 20);
        lbl.setStyle("-fx-font-size:40px;-fx-font-weight:bold;-fx-text-fill:#ffd700;" +
                     "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.5),4,0,0,2);");
        lbl.setTranslateX(0);
        lbl.setTranslateY(0);
        merkez.getChildren().add(lbl);
        var tt = new TranslateTransition(Duration.millis(1000), lbl); tt.setByY(-100);
        var ft = new FadeTransition(Duration.millis(1000), lbl);
        ft.setFromValue(1.0); ft.setToValue(0.0);
        var pt = new ParallelTransition(tt, ft);
        pt.setOnFinished(e -> merkez.getChildren().remove(lbl));
        pt.play();
    }

    public void konfetiAnimasyonu() {
        var merkez = s.merkezIcerikKutusu;
        if (merkez == null) return;

        String[] renkler = {"#f38ba8", "#a6e3a1", "#f9e2af", "#89b4fa", "#cba6f7", "#fab387"};
        String[] emojiler = {"🎉", "✨", "🎊", "⭐"};

        for (int i = 0; i < 40; i++) {
            Label konfeti = new Label(emojiler[RNG.nextInt(emojiler.length)]);
            konfeti.setStyle("-fx-font-size: 24px; -fx-text-fill: " + renkler[RNG.nextInt(renkler.length)] + ";");
            konfeti.setMouseTransparent(true);

            // Başlangıç noktası (merkezin alt kısmı)
            konfeti.setTranslateX(0);
            konfeti.setTranslateY(merkez.getHeight() / 2);
            merkez.getChildren().add(konfeti);

            // Rastgele fırlama
            double hiziX = (RNG.nextDouble() - 0.5) * 600;
            double hiziY = -400 - RNG.nextDouble() * 300;

            TranslateTransition tt = new TranslateTransition(Duration.seconds(2 + RNG.nextDouble()), konfeti);
            tt.setByX(hiziX);
            tt.setByY(hiziY);
            tt.setInterpolator(Interpolator.SPLINE(0.25, 0.1, 0.25, 1)); // Yerçekimi hissi

            RotateTransition rt = new RotateTransition(Duration.seconds(2 + RNG.nextDouble()), konfeti);
            rt.setByAngle(360 + RNG.nextInt(720));

            FadeTransition ft = new FadeTransition(Duration.seconds(1), konfeti);
            ft.setDelay(Duration.seconds(1 + RNG.nextDouble()));
            ft.setFromValue(1.0);
            ft.setToValue(0.0);

            ParallelTransition pt = new ParallelTransition(tt, rt, ft);
            pt.setOnFinished(e -> merkez.getChildren().remove(konfeti));
            pt.play();
        }
    }

    private void altinLeblebiAnimasyonu(javafx.scene.control.Button btn) {
        var merkez = s.merkezIcerikKutusu;
        if (merkez == null) return;
        var lbl = builder.emojiLabel("+10 🌟", 20);
        lbl.setStyle("-fx-font-size:20px;-fx-font-weight:bold;-fx-text-fill:#ffd700;" +
                     "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.5),4,0,0,2);");
        var bb = btn.localToScene(btn.getBoundsInLocal());
        var sb = merkez.sceneToLocal(bb);
        lbl.setTranslateX(sb.getMinX() - merkez.getWidth() / 2 + btn.getWidth() / 2);
        lbl.setTranslateY(sb.getMinY() - merkez.getHeight() / 2);
        merkez.getChildren().add(lbl);
        var tt = new TranslateTransition(Duration.millis(800), lbl); tt.setByY(-40);
        var ft = new FadeTransition(Duration.millis(800), lbl);
        ft.setFromValue(1.0); ft.setToValue(0.0);
        var pt = new ParallelTransition(tt, ft);
        pt.setOnFinished(e -> merkez.getChildren().remove(lbl));
        pt.play();
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private Board aktifTahta() {
        if (s.leblebModu  && s.leblebiBoardMode != null) return s.leblebiBoardMode.getTahta();
        if (s.klasikBoardMode != null) return s.klasikBoardMode.getTahta();
        return null;
    }

    private void refreshScoreLabels() {
        if (s.leblebiBoardMode == null) return;
        s.puanEtiketi.setText(s.leblebiBoardMode.getLeblebPuani() + " puan");
        s.altinEtiketi.setText(s.leblebiBoardMode.getAltin() + " altın");
    }

    private void setSifirlaBtnGraphic(javafx.scene.image.Image img, String fallbackEmoji) {
        if (s.sifirlaBtn != null) {
            s.sifirlaBtn.setText("");
            s.sifirlaBtn.setGraphic(builder.imgGraphic(img, fallbackEmoji, 26));
        }
    }
}
