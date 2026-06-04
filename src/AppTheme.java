import javafx.scene.Scene;

/**
 * AppTheme — All colour constants and CSS/JavaFX style helpers.
 *
 * One instance is held in MinesweeperApp and passed (or accessed statically
 * for the constants) by the other modules.  The instance methods that need
 * to know the current theme/mode receive the AppState they need.
 */
public class AppTheme {

    // ── Font paths ─────────────────────────────────────────────────────────────
    static final String FONT_YOL       = "assets/fonts/GameFont.ttf";
    static final String FONT_EMOJI_YOL = "assets/fonts/NotoEmoji-Regular.ttf";

    // ── Dark theme — Midnight Indigo ───────────────────────────────────────────
    static final String KT_ARKAPLAN   = "#12111f";
    static final String KT_ACILMAMIS  = "#5c5b8a";
    static final String KT_ACILMIS    = "#1a192e";
    static final String KT_ISARETLI   = "#6a5acd";
    static final String KT_CERCEVE    = "#7b748a";
    static final String KT_YAZI       = "#e2dcf8";
    static final String KT_YAZI_SOLUK = "#a099b0";
    static final String KT_UST_BAR    = "#0e0d1c";

    // ── Light theme — Soft Lavender ────────────────────────────────────────────
    static final String AT_ARKAPLAN   = "#e0e4f0";
    static final String AT_ACILMAMIS  = "#7d86a1";
    static final String AT_ACILMIS    = "#fcfdff";
    static final String AT_ISARETLI   = "#7a82ab";
    static final String AT_CERCEVE    = "#5c647a";
    static final String AT_YAZI       = "#1a1830";
    static final String AT_YAZI_SOLUK = "#717892";
    static final String AT_UST_BAR    = "#cbd1e6";

    // ── Leblebi theme — Golden Harvest ─────────────────────────────────────────
    static final String LB_ARKAPLAN   = "#2e1a00";
    static final String LB_ACILMAMIS  = "#c8922a";
    static final String LB_ACILMIS    = "#6b3e10";
    static final String LB_ISARETLI   = "#e8b840";
    static final String LB_YILAN_RENK = "#3a6e25";
    static final String LB_CERCEVE    = "#8c6018";
    static final String LB_UST_BAR    = "linear-gradient(to bottom, #6b3e10, #3a1e00)";
    static final String LB_KARGA_RENK = "#c0392b";

    // ── Chess theme — Ivory & Obsidian ─────────────────────────────────────────
    static final String CH_ARKAPLAN        = "#0a1628";
    static final String CH_ACILMAMIS_LIGHT = "#f0ead8";
    static final String CH_ACILMAMIS_DARK  = "#2c1f0e";
    static final String CH_ACILMIS         = "#0d2e54";
    static final String CH_CERCEVE         = "#1e3a5f";
    static final String CH_UST_BAR         = "#060e1e";
    static final String CH_ISARETLI        = "#3d1a5a";

    // ── Number colours ─────────────────────────────────────────────────────────
    static final String[] KT_SAYI_RENK = {
            "", "#7eb8ff", "#98e0a0", "#ff8080", "#60c8ec",
            "#ffaa70", "#70d8eb", "#c0a8ff", "#e2dcf8" };
    static final String[] AT_SAYI_RENK = {
            "", "#1a56b0", "#276830", "#b82020", "#0060a0",
            "#c85000", "#007880", "#5a1090", "#304050" };
    static final String[] LB_SAYI_RENK = {
            "", "#e8c27a", "#6dbe45", "#d95f3b", "#5ba3d6",
            "#d4a050", "#40d4b0", "#c080e0", "#f0c060" };
    static final String[] CH_SAYI_RENK = {
            "", "#5bc8f8", "#7eca80", "#f07070", "#d090e0",
            "#ffc060", "#40d8e8", "#f080b8", "#a8b8c8" };

    // ── Market button constants ────────────────────────────────────────────────
    static final String MKT_BTN_NORMAL =
            "-fx-background-color: #6b3e00; -fx-text-fill: #f5e6b0;" +
            "-fx-background-radius: 10; -fx-border-radius: 10;" +
            "-fx-border-color: #a07020; -fx-border-width: 1.5;" +
            "-fx-cursor: hand; -fx-alignment: center;";
    static final String MKT_BTN_HOVER  =
            "-fx-background-color: #8a5500; -fx-text-fill: #fff8e0;" +
            "-fx-background-radius: 10; -fx-border-radius: 10;" +
            "-fx-border-color: #f0c040; -fx-border-width: 2;" +
            "-fx-cursor: hand; -fx-alignment: center;" +
            "-fx-effect: dropshadow(gaussian,#f0c040,8,0.4,0,0);";

    // =========================================================================
    // Style cache — avoids rebuilding identical strings on every cell render
    // =========================================================================

    // Key: encoded as (leblebModu ? 2 : satranModu ? 1 : karanlikTema ? 0 : 3)
    private int      cachedModeKey        = -1;
    private String   cachedAcilmamis      = null;  // (r+c) parity-independent variant
    private String   cachedAcilmamisLight = null;  // chess light square
    private String   cachedAcilmamisDark  = null;  // chess dark square
    private String   cachedIsaretli       = null;
    private String   cachedMayin          = null;
    private String[] cachedAcilmis        = null;  // [0..8] neighbour count variants

    /** Call whenever the theme or mode changes to force cache rebuild on next access. */
    public void invalidateCache() {
        cachedModeKey = -1;
    }

    private int modeKey(AppState s) {
        if (s.leblebModu) return 2;
        if (s.satranModu) return 1;
        return s.karanlikTema ? 0 : 3;
    }

    private void ensureCache(AppState s) {
        int key = modeKey(s);
        if (key == cachedModeKey) return;
        cachedModeKey = key;

        // Closed cell
        if (s.satranModu) {
            cachedAcilmamisLight = "-fx-background-color:" + CH_ACILMAMIS_LIGHT +
                    ";-fx-border-color:" + CH_CERCEVE +
                    ";-fx-border-width:1;-fx-background-radius:4;-fx-border-radius:4;" +
                    "-fx-padding:0;-fx-cursor:hand;";
            cachedAcilmamisDark = "-fx-background-color:" + CH_ACILMAMIS_DARK +
                    ";-fx-border-color:" + CH_CERCEVE +
                    ";-fx-border-width:1;-fx-background-radius:4;-fx-border-radius:4;" +
                    "-fx-padding:0;-fx-cursor:hand;";
            cachedAcilmamis = cachedAcilmamisLight;
        } else if (s.leblebModu) {
            cachedAcilmamis = "-fx-background-color:" + LB_ACILMAMIS +
                    ";-fx-border-color: #c89a20 #7a5500 #7a5500 #c89a20;-fx-border-width:1.5;" +
                    "-fx-background-radius:4;-fx-border-radius:4;-fx-padding:0;-fx-cursor:hand;";
        } else {
            String bg = s.karanlikTema ? KT_ACILMAMIS : AT_ACILMAMIS;
            String br = s.karanlikTema ? KT_CERCEVE   : AT_CERCEVE;
            cachedAcilmamis = "-fx-background-color:" + bg + ";-fx-border-color:" + br +
                    ";-fx-border-width:1;-fx-background-radius:3;-fx-border-radius:3;" +
                    "-fx-padding:0;-fx-cursor:hand;";
        }

        // Flagged cell
        String isaBg = s.satranModu ? CH_ISARETLI : (s.leblebModu ? LB_ISARETLI :
                        (s.karanlikTema ? KT_ISARETLI : AT_ISARETLI));
        String isaBr = s.satranModu ? "#8060a0"   : (s.leblebModu ? LB_CERCEVE  :
                        (s.karanlikTema ? KT_CERCEVE  : AT_CERCEVE));
        String isaFg = s.satranModu ? "#cdd6f4"   : (s.leblebModu ? "#3d2800"   :
                        (s.karanlikTema ? "#f38ba8"   : "#c62828"));
        cachedIsaretli = "-fx-background-color:" + isaBg + ";-fx-border-color:" + isaBr +
                ";-fx-border-width:1;-fx-background-radius:3;-fx-border-radius:3;" +
                "-fx-padding:0;-fx-text-fill:" + isaFg +
                ";-fx-font-weight:bold;-fx-font-size:14px;-fx-cursor:hand;";

        // Mine cell
        if (s.leblebModu) {
            cachedMayin = "-fx-background-color:" + LB_YILAN_RENK + ";" +
                    "-fx-border-color: #4a8e35; -fx-border-width:1.5;" +
                    "-fx-background-radius:4; -fx-border-radius:4;" +
                    "-fx-padding:0; -fx-text-fill:#ffffff;" +
                    "-fx-font-weight:bold; -fx-font-size:16px;";
        } else {
            cachedMayin = "-fx-background-color:#8B0000;-fx-border-width:1;" +
                    "-fx-background-radius:3;-fx-border-radius:3;-fx-padding:0;" +
                    "-fx-text-fill:#ffffff;-fx-font-weight:bold;-fx-font-size:16px;";
        }

        // Opened cells [0..8] at default font size 14px
        String[] sr = sayiRenkleri(s);
        String openBg, openBr, openFgSoluk;
        if (s.satranModu) {
            openBg = CH_ACILMIS; openBr = CH_CERCEVE; openFgSoluk = "#4a6a8a";
        } else if (s.leblebModu) {
            openBg = LB_ACILMIS; openBr = "#5c3200"; openFgSoluk = "#c8a060";
        } else {
            openBg      = s.karanlikTema ? KT_ACILMIS    : AT_ACILMIS;
            openBr      = s.karanlikTema ? KT_CERCEVE    : AT_CERCEVE;
            openFgSoluk = s.karanlikTema ? KT_YAZI_SOLUK : AT_YAZI_SOLUK;
        }
        cachedAcilmis = new String[9];
        for (int k = 0; k <= 8; k++) {
            String fg = (k > 0) ? sr[k] : openFgSoluk;
            cachedAcilmis[k] = "-fx-background-color:" + openBg + ";-fx-border-color:" + openBr +
                    ";-fx-border-width:1;-fx-background-radius:4;-fx-border-radius:4;" +
                    "-fx-padding:0;-fx-text-fill:" + fg +
                    ";-fx-font-weight:bold;-fx-font-size:14px;";
        }
    }

    // =========================================================================
    // Instance helpers — need AppState context
    // =========================================================================

    /** Generic button style used in the HUD bar. */
    public String butonTarzi() {
        return "-fx-font-size: 13px; -fx-padding: 4 10 4 10;" +
               "-fx-cursor: hand; -fx-border-radius: 6; -fx-background-radius: 6;";
    }

    // ── Cell styles ────────────────────────────────────────────────────────────

    public String acilmamisHucreTarzi(AppState s) {
        return acilmamisHucreTarzi(s, -1, -1);
    }

    public String acilmamisHucreTarzi(AppState s, int r, int c) {
        ensureCache(s);
        if (s.satranModu) {
            boolean isLight = (r < 0 || c < 0) || (r + c) % 2 == 0;
            return isLight ? cachedAcilmamisLight : cachedAcilmamisDark;
        }
        return cachedAcilmamis;
    }

    public String acilmamisHucreHoverTarzi(AppState s) {
        return acilmamisHucreHoverTarzi(s, -1, -1);
    }

    public String acilmamisHucreHoverTarzi(AppState s, int r, int c) {
        if (s.satranModu)
            return "-fx-background-color:#e8c84a;-fx-border-color:#c8a800;-fx-border-width:2;" +
                   "-fx-background-radius:4;-fx-border-radius:4;-fx-padding:0;-fx-cursor:hand;";
        if (s.leblebModu)
            return "-fx-background-color:#d9aa3a;" +
                   "-fx-border-color: #f0d070 #8a6510 #8a6510 #f0d070;-fx-border-width:2;" +
                   "-fx-background-radius:4;-fx-border-radius:4;-fx-padding:0;-fx-cursor:hand;";
        String bg = s.karanlikTema ? "#454366" : "#98a1ba";
        String br = s.karanlikTema ? KT_CERCEVE : AT_CERCEVE;
        return "-fx-background-color:" + bg + ";-fx-border-color:" + br +
               ";-fx-border-width:1;-fx-background-radius:3;-fx-border-radius:3;" +
               "-fx-padding:0;-fx-cursor:hand;";
    }

    public String acilmisHucreTarzi(AppState s, int k, String[] sr) {
        ensureCache(s);
        return cachedAcilmis[Math.max(0, Math.min(k, 8))];
    }

    public String acilmisHucreTarzi(AppState s, int k, String[] sr, double yaz) {
        // Hot path (resize): if font size is default 14px, use the cache directly
        if (yaz == 14.0) {
            ensureCache(s);
            return cachedAcilmis[Math.max(0, Math.min(k, 8))];
        }
        // Non-default font size (e.g. small boards): build string once
        String bg, br, fgSoluk;
        if (s.satranModu) {
            bg = CH_ACILMIS; br = CH_CERCEVE; fgSoluk = "#4a6a8a";
        } else if (s.leblebModu) {
            bg = LB_ACILMIS; br = "#5c3200"; fgSoluk = "#c8a060";
        } else {
            bg      = s.karanlikTema ? KT_ACILMIS    : AT_ACILMIS;
            br      = s.karanlikTema ? KT_CERCEVE    : AT_CERCEVE;
            fgSoluk = s.karanlikTema ? KT_YAZI_SOLUK : AT_YAZI_SOLUK;
        }
        String fg = (k > 0 && k <= 8) ? sr[k] : fgSoluk;
        return "-fx-background-color:" + bg + ";-fx-border-color:" + br +
               ";-fx-border-width:1;-fx-background-radius:4;-fx-border-radius:4;" +
               "-fx-padding:0;-fx-text-fill:" + fg +
               ";-fx-font-weight:bold;-fx-font-size:" + yaz + "px;";
    }

    public String isaretliHucreTarzi(AppState s) {
        ensureCache(s);
        return cachedIsaretli;
    }

    public String mayinHucreTarzi(AppState s) {
        ensureCache(s);
        return cachedMayin;
    }

    public String ilacHoverMerkezTarzi() {
        return "-fx-background-color: #b8e068;" +
               "-fx-border-color: #4caf50 #2e7d32 #2e7d32 #4caf50;" +
               "-fx-border-width: 2.5; -fx-background-radius: 4; -fx-border-radius: 4;" +
               "-fx-padding: 0; -fx-cursor: crosshair;" +
               "-fx-effect: dropshadow(gaussian,#4caf50,8,0.5,0,0);";
    }

    public String ilacHoverKenarTarzi() {
        return "-fx-background-color: #d4edaa;" +
               "-fx-border-color: #81c784 #388e3c #388e3c #81c784;" +
               "-fx-border-width: 1.5; -fx-background-radius: 3; -fx-border-radius: 3;" +
               "-fx-padding: 0; -fx-cursor: crosshair;";
    }

    // ── Global CSS (custom font injection) ────────────────────────────────────

    public void applyCss(Scene scene) {
        boolean fontLoaded = false;
        try {
            java.net.URL url = getClass().getResource(FONT_YOL);
            if (url == null) url = new java.io.File(FONT_YOL).toURI().toURL();
            javafx.scene.text.Font f = javafx.scene.text.Font.loadFont(url.toExternalForm(), 14);
            fontLoaded = (f != null);
        } catch (Exception ignored) {}

        // Load Noto Emoji so supplementary-plane codepoints render on Windows.
        // JavaFX Font.loadFont registers the font globally; the CSS font-family
        // stack below then makes it a fallback for every node in the scene.
        boolean emojiLoaded = false;
        try {
            java.net.URL url = getClass().getResource(FONT_EMOJI_YOL);
            if (url == null) url = new java.io.File(FONT_EMOJI_YOL).toURI().toURL();
            javafx.scene.text.Font f = javafx.scene.text.Font.loadFont(url.toExternalForm(), 14);
            emojiLoaded = (f != null);
        } catch (Exception ignored) {}

        String fam   = fontLoaded  ? "GameFont"   : "System";
        // Build font-family stack: game font first, Noto Emoji as fallback.
        // JavaFX respects multi-font stacks in -fx-font-family since JDK 17+.
        String stack = emojiLoaded
                ? "'" + fam + "', 'Noto Emoji'"
                : "'" + fam + "'";
        String css = ".root{-fx-font-family:" + stack + ";}" +
                     ".label{-fx-font-family:" + stack + ";}" +
                     ".button{-fx-font-family:" + stack + ";}" +
                     ".text-field{-fx-font-family:" + stack + ";}" +
                     ".text-area{-fx-font-family:" + stack + ";}" +
                     ".dialog-pane .label{-fx-font-family:" + stack + ";}" +
                     ".dialog-pane .button{-fx-font-family:" + stack + ";}" +
                     /* Spinner arrow buttons — dark background */
                     ".spinner .increment-arrow-button{-fx-background-color:#313244;}" +
                     ".spinner .decrement-arrow-button{-fx-background-color:#313244;}" +
                     ".spinner .increment-arrow-button .increment-arrow{-fx-background-color:#cdd6f4;}" +
                     ".spinner .decrement-arrow-button .decrement-arrow{-fx-background-color:#cdd6f4;}" +
                     ".spinner .increment-arrow-button:hover{-fx-background-color:#45475a;}" +
                     ".spinner .decrement-arrow-button:hover{-fx-background-color:#45475a;}" +
                     /* Separator — dark line instead of white */
                     ".separator .line{-fx-border-color:#313244;-fx-border-width:1 0 0 0;}" +
                     ".separator{-fx-background-color:transparent;}" +
                     /* ScrollPane — remove white edges */
                     ".scroll-pane{-fx-background-color:transparent;-fx-background:transparent;}" +
                     ".scroll-pane .viewport{-fx-background-color:transparent;}" +
                     ".scroll-pane .scroll-bar .thumb{-fx-background-color:#45475a;-fx-background-radius:4;}" +
                     ".scroll-pane .scroll-bar .track{-fx-background-color:#1e1e2e;}" +
                     ".scroll-pane .scroll-bar .increment-button,.scroll-pane .scroll-bar .decrement-button{-fx-background-color:transparent;}" +
                     ".scroll-pane .scroll-bar .increment-arrow,.scroll-pane .scroll-bar .decrement-arrow{-fx-background-color:transparent;-fx-shape:null;-fx-padding:0;}" +
                     /* TabPane — dark header */
                     ".tab-pane .tab-header-background{-fx-background-color:#181825;}" +
                     ".tab-pane .tab{-fx-background-color:#313244;-fx-background-radius:6 6 0 0;}" +
                     ".tab-pane .tab:selected{-fx-background-color:#45475a;}" +
                     ".tab-pane .tab .tab-label{-fx-text-fill:#cdd6f4;}" +
                     ".tab-pane .tab-header-area{-fx-padding:4 0 0 4;}" +
                     ".tab-pane .tab-content-area{-fx-background-color:#1e1e2e;}" +
                     /* RadioButton — dark dot background */
                     ".radio-button .radio{-fx-background-color:#313244;-fx-border-color:#585b70;-fx-border-radius:50;-fx-background-radius:50;}" +
                     ".radio-button:selected .radio{-fx-background-color:#313244;-fx-border-color:#89b4fa;}" +
                     ".radio-button:selected .radio .dot{-fx-background-color:#89b4fa;-fx-background-radius:50;}" +
                     ".radio-button .radio .dot{-fx-background-color:transparent;-fx-background-radius:50;}";
        String encoded = java.util.Base64.getEncoder()
                .encodeToString(css.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        scene.getStylesheets().clear();
        scene.getStylesheets().add("data:text/css;base64," + encoded);
    }

    // ── Dialog styling ─────────────────────────────────────────────────────────

    public void styleDialog(javafx.scene.control.Alert alert, boolean leblebi) {
        applyDialogStyle(alert.getDialogPane(), leblebi);
    }

    public void styleDialog(javafx.scene.control.TextInputDialog dialog, boolean leblebi) {
        applyDialogStyle(dialog.getDialogPane(), leblebi);
    }

    private void applyDialogStyle(javafx.scene.control.DialogPane pane, boolean leblebi) {
        String bg  = leblebi ? "#3d2800" : "#1e1e2e";
        String fg  = leblebi ? "#f5e6b0" : "#cdd6f4";
        String hbg = leblebi ? "#5c3a00" : "#181825";
        pane.setStyle("-fx-background-color:" + bg +
                      ";-fx-font-size:14px;-fx-text-fill:" + fg + ";");
        Runnable apply = () -> {
            for (javafx.scene.Node n : pane.lookupAll(".label"))
                if (n instanceof javafx.scene.control.Label l) {
                    String cur = l.getStyle() == null ? "" : l.getStyle();
                    if (!cur.contains("-fx-text-fill"))
                        l.setStyle(cur + "-fx-text-fill:" + fg + ";-fx-font-size:14px;");
                }
            javafx.scene.Node h = pane.lookup(".header-panel");
            if (h != null) {
                h.setStyle("-fx-background-color:" + hbg + ";");
                javafx.scene.Node hl = pane.lookup(".header-panel .label");
                if (hl instanceof javafx.scene.control.Label lbl)
                    lbl.setStyle("-fx-text-fill:" + fg + ";-fx-font-size:15px;-fx-font-weight:bold;");
            }
            javafx.scene.Node tf = pane.lookup(".text-field");
            if (tf instanceof javafx.scene.control.TextField field)
                field.setStyle("-fx-background-color:" + hbg + ";-fx-text-fill:" + fg + ";-fx-font-size:13px;");
            pane.getButtonTypes().forEach(bt -> {
                javafx.scene.Node btn = pane.lookupButton(bt);
                if (btn != null)
                    btn.setStyle("-fx-background-color:" + (leblebi ? "#7a5200" : "#313244") +
                                 ";-fx-text-fill:" + fg + ";-fx-font-size:13px;" +
                                 "-fx-background-radius:6;-fx-padding:6 14 6 14;");
            });
        };
        apply.run();
        javafx.application.Platform.runLater(apply);
    }

    // ── Convenience ───────────────────────────────────────────────────────────

    /** Returns the number-colour array for the current mode/theme. */
    public String[] sayiRenkleri(AppState s) {
        if (s.satranModu) return CH_SAYI_RENK;
        if (s.leblebModu) return LB_SAYI_RENK;
        return s.karanlikTema ? KT_SAYI_RENK : AT_SAYI_RENK;
    }
}
