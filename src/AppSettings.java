import java.io.*;
import java.nio.file.*;

/**
 * AppSettings — Persists user preferences to settings.json.
 *
 * Saved fields:
 *   - karanlikTema   (boolean) dark / light theme
 *   - sesHacmi       (double)  SFX volume  0.0–1.0
 *   - muzikHacmi     (double)  BGM volume  0.0–1.0
 *   - muzikSessiz    (boolean) BGM muted
 *   - leblebAcildi   (boolean) easter-egg unlocked (persists across sessions)
 *
 * Format is minimal hand-written JSON so we have no dependency on any library.
 * The same brace-counting parser pattern used in SkorTablosu is reused here.
 */
public class AppSettings {

    private static final String DOSYA = "settings.json";

    // ── Load ──────────────────────────────────────────────────────────────────

    /** Reads saved settings into AppState.  Safe to call even if the file is absent. */
    public static void yukle(AppState s) {
        Path p = Path.of(DOSYA);
        if (!Files.exists(p)) return;
        try {
            String json = Files.readString(p).trim();
            s.karanlikTema = parseBool(json, "karanlikTema",  s.karanlikTema);
            s.sesHacmi     = parseDouble(json, "sesHacmi",    s.sesHacmi);
            s.muzikHacmi   = parseDouble(json, "muzikHacmi",  s.muzikHacmi);
            s.muzikSessiz  = parseBool(json, "muzikSessiz",   s.muzikSessiz);
            s.leblebAcildi = parseBool(json, "leblebAcildi",  s.leblebAcildi);
        } catch (Exception e) {
            System.err.println("[AppSettings] Yüklenemedi: " + e.getMessage());
        }
    }

    // ── Save ──────────────────────────────────────────────────────────────────

    /** Writes current AppState preferences to disk.  Silently swallows IO errors. */
    public static void kaydet(AppState s) {
        String json = "{\n" +
            "  \"karanlikTema\": "  + s.karanlikTema  + ",\n" +
            "  \"sesHacmi\": "      + String.format("%.3f", s.sesHacmi)    + ",\n" +
            "  \"muzikHacmi\": "    + String.format("%.3f", s.muzikHacmi)  + ",\n" +
            "  \"muzikSessiz\": "   + s.muzikSessiz   + ",\n" +
            "  \"leblebAcildi\": "  + s.leblebAcildi  + "\n" +
            "}\n";
        try {
            Files.writeString(Path.of(DOSYA), json);
        } catch (IOException e) {
            System.err.println("[AppSettings] Kaydedilemedi: " + e.getMessage());
        }
    }

    // ── Parsers ───────────────────────────────────────────────────────────────

    private static boolean parseBool(String json, String key, boolean fallback) {
        String val = rawValue(json, key);
        if (val == null) return fallback;
        return "true".equalsIgnoreCase(val.trim());
    }

    private static double parseDouble(String json, String key, double fallback) {
        String val = rawValue(json, key);
        if (val == null) return fallback;
        try { return Double.parseDouble(val.trim()); } catch (NumberFormatException e) { return fallback; }
    }

    /** Returns the raw (unquoted) value string for a JSON key, or null if absent. */
    private static String rawValue(String json, String key) {
        String marker = "\"" + key + "\":";
        int idx = json.indexOf(marker);
        if (idx < 0) return null;
        int start = idx + marker.length();
        // skip whitespace
        while (start < json.length() && json.charAt(start) == ' ') start++;
        // find end: comma, newline, or closing brace
        int end = start;
        while (end < json.length()) {
            char c = json.charAt(end);
            if (c == ',' || c == '\n' || c == '}') break;
            end++;
        }
        return json.substring(start, end).trim();
    }
}
