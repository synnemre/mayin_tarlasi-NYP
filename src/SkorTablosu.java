import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Skor tablosunu scores.json dosyasında saklar ve okur.
 *
 * "mod" alanı:
 *   "leblebi"  — Mehmet Emmi'nin Leblebi Tarlası
 *   "zamanlı"  — Klasik zamanlı mod (geri sayım)
 *
 * v2: satir, sutun, mayin, sureSiniri eklendi (zamanlı mod detayları için)
 *
 * FIX (bug): JSON parser now correctly handles string values that contain
 * commas or escaped quotes (e.g. player names like 'Ali, Veli' or 'O\"Neil').
 * The previous split-on-},{ approach corrupted records with such values.
 *
 * FIX (quality): Dates now use Europe/Istanbul timezone explicitly instead of
 * the JVM default, which may differ on servers or CI machines.
 */
public class SkorTablosu {

    public static final String DOSYA         = "scores.json";
    public static final String MOD_LEBLEBI   = "leblebi";
    public static final String MOD_ZAMANLI   = "zamanlı";

    private static final java.time.ZoneId ISTANBUL = java.time.ZoneId.of("Europe/Istanbul");

    public record SkorGirisi(
            String isim,
            int    skor,
            int    seviye,
            String tarih,
            String mod,
            int    satirSayisi,
            int    sutunSayisi,
            int    mayinSayisi,
            int    sureSiniri
    ) implements Comparable<SkorGirisi> {

        public SkorGirisi(String isim, int skor, int seviye, String tarih) {
            this(isim, skor, seviye, tarih, MOD_LEBLEBI, 0, 0, 0, 0);
        }
        public SkorGirisi(String isim, int skor, int seviye, String tarih, String mod) {
            this(isim, skor, seviye, tarih, mod, 0, 0, 0, 0);
        }
        @Override
        public int compareTo(SkorGirisi diger) {
            return Integer.compare(diger.skor, this.skor);
        }
    }

    public static void kaydet(String isim, int skor, int seviye, String mod,
                              int satir, int sutun, int mayin, int sureSiniri) {
        List<SkorGirisi> liste = tumunuYukle();
        String tarih = java.time.LocalDate.now(ISTANBUL).toString();
        liste.add(new SkorGirisi(isim, skor, seviye, tarih, mod, satir, sutun, mayin, sureSiniri));
        Collections.sort(liste);
        yaz(liste);
    }

    public static void kaydet(String isim, int skor, int seviye, String mod) {
        kaydet(isim, skor, seviye, mod, 0, 0, 0, 0);
    }

    public static void kaydet(String isim, int skor, int seviye) {
        kaydet(isim, skor, seviye, MOD_LEBLEBI);
    }

    public static List<SkorGirisi> yukle(String mod) {
        List<SkorGirisi> filtreli = new ArrayList<>();
        for (SkorGirisi g : tumunuYukle())
            if (mod.equals(g.mod())) filtreli.add(g);
        return filtreli;
    }

    public static List<SkorGirisi> yukle() {
        return tumunuYukle();
    }

    private static List<SkorGirisi> tumunuYukle() {
        List<SkorGirisi> liste = new ArrayList<>();
        Path dosya = Path.of(DOSYA);
        if (!Files.exists(dosya)) return liste;
        try {
            String icerik = Files.readString(dosya).trim();
            if (icerik.isEmpty() || icerik.equals("[]")) return liste;

            // FIX: Parse JSON objects properly rather than splitting on },{ which
            // breaks when a string value contains a comma or an escaped quote.
            List<String> nesneler = jsonNesneleriniCikart(icerik);

            for (String nesne : nesneler) {
                String isim      = jsonStringDeger(nesne, "isim");
                String tarih     = jsonStringDeger(nesne, "tarih");
                String modStr    = jsonStringDeger(nesne, "mod");
                if (modStr == null || modStr.isBlank()) modStr = MOD_LEBLEBI;
                int skor2        = parseIntSafe(jsonSayiDeger(nesne, "skor"));
                int seviye2      = parseIntSafe(jsonSayiDeger(nesne, "seviye"));
                int satirSayisi2 = parseIntSafe(jsonSayiDeger(nesne, "satir"));
                int sutunSayisi2 = parseIntSafe(jsonSayiDeger(nesne, "sutun"));
                int mayinSayisi2 = parseIntSafe(jsonSayiDeger(nesne, "mayin"));
                int sureSiniri2  = parseIntSafe(jsonSayiDeger(nesne, "sure"));
                liste.add(new SkorGirisi(isim, skor2, seviye2, tarih, modStr,
                    satirSayisi2, sutunSayisi2, mayinSayisi2, sureSiniri2));
            }
            Collections.sort(liste);
        } catch (Exception e) {
            System.err.println("Skor dosyası okunurken hata: " + e.getMessage());
        }
        return liste;
    }

    /**
     * Splits a JSON array string into individual raw object strings by counting
     * brace depth, so commas inside string values never cause incorrect splits.
     */
    private static List<String> jsonNesneleriniCikart(String json) {
        List<String> sonuc = new ArrayList<>();
        int derinlik = 0;
        boolean stringIcinde = false;
        boolean kacisChar = false;
        int baslangic = -1;

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);

            if (kacisChar) {
                kacisChar = false;
                continue;
            }
            if (c == '\\' && stringIcinde) {
                kacisChar = true;
                continue;
            }
            if (c == '"') {
                stringIcinde = !stringIcinde;
                continue;
            }
            if (stringIcinde) continue;

            if (c == '{') {
                if (derinlik == 0) baslangic = i;
                derinlik++;
            } else if (c == '}') {
                derinlik--;
                if (derinlik == 0 && baslangic >= 0) {
                    sonuc.add(json.substring(baslangic, i + 1));
                    baslangic = -1;
                }
            }
        }
        return sonuc;
    }

    /**
     * Extracts a JSON string value for the given key, correctly handling
     * escape sequences (e.g. \" inside the value).
     * Returns an empty string if the key is not found.
     */
    private static String jsonStringDeger(String nesne, String anahtar) {
        String aranan = "\"" + anahtar + "\":\"";
        int bas = nesne.indexOf(aranan);
        if (bas < 0) return "";
        bas += aranan.length();

        StringBuilder sb = new StringBuilder();
        boolean kacis = false;
        for (int i = bas; i < nesne.length(); i++) {
            char c = nesne.charAt(i);
            if (kacis) {
                // Only unescape sequences we write: \" and \\
                if (c == '"') sb.append('"');
                else if (c == '\\') sb.append('\\');
                else { sb.append('\\'); sb.append(c); }
                kacis = false;
            } else if (c == '\\') {
                kacis = true;
            } else if (c == '"') {
                break; // end of string value
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Extracts a numeric (non-string) JSON value for the given key.
     * Returns empty string if not found.
     */
    private static String jsonSayiDeger(String nesne, String anahtar) {
        String aranan = "\"" + anahtar + "\":";
        int bas = nesne.indexOf(aranan);
        if (bas < 0) return "";
        bas += aranan.length();
        // Skip past any opening quote (shouldn't happen for numbers, but be safe)
        if (bas < nesne.length() && nesne.charAt(bas) == '"') return "";
        int son = bas;
        while (son < nesne.length()
                && (Character.isDigit(nesne.charAt(son)) || nesne.charAt(son) == '-'))
            son++;
        return nesne.substring(bas, son);
    }

    private static void yaz(List<SkorGirisi> liste) {
        StringBuilder sb = new StringBuilder("[\n");
        for (int i = 0; i < liste.size(); i++) {
            SkorGirisi g = liste.get(i);
            String m = (g.mod() == null || g.mod().isBlank()) ? MOD_LEBLEBI : g.mod();
            // Escape backslashes first, then double-quotes, to avoid double-escaping
            String isimKacisli = g.isim().replace("\\", "\\\\").replace("\"", "\\\"");
            sb.append("  {\"isim\":\"").append(isimKacisli)
              .append("\",\"skor\":").append(g.skor())
              .append(",\"seviye\":").append(g.seviye())
              .append(",\"tarih\":\"").append(g.tarih())
              .append("\",\"mod\":\"").append(m)
              .append("\",\"satir\":").append(g.satirSayisi())
              .append(",\"sutun\":").append(g.sutunSayisi())
              .append(",\"mayin\":").append(g.mayinSayisi())
              .append(",\"sure\":").append(g.sureSiniri())
              .append("}");
            if (i < liste.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("]");
        try {
            Files.writeString(Path.of(DOSYA), sb.toString());
        } catch (IOException e) {
            System.err.println("Skor kaydedilemedi: " + e.getMessage());
        }
    }

    private static int parseIntSafe(String s) {
        if (s == null || s.isBlank()) return 0;
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return 0; }
    }
}
