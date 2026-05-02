import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Skor tablosunu scores.json dosyasında saklar ve okur.
 * JSON kütüphanesi gerekmeden elle parse eder (minimal bağımlılık).
 */
public class SkorTablosu {

    public static final String DOSYA = "scores.json";

    public record SkorGirisi(String isim, int skor, int seviye, String tarih)
            implements Comparable<SkorGirisi> {
        @Override
        public int compareTo(SkorGirisi diger) {
            return Integer.compare(diger.skor, this.skor); // Yüksek skor üstte
        }
    }

    // ── Kaydet ───────────────────────────────────────────────────────────────

    public static void kaydet(String isim, int skor, int seviye) {
        List<SkorGirisi> liste = yukle();
        String tarih = java.time.LocalDate.now().toString();
        liste.add(new SkorGirisi(isim, skor, seviye, tarih));
        Collections.sort(liste);
        yaz(liste);
    }

    // ── Yükle ────────────────────────────────────────────────────────────────

    public static List<SkorGirisi> yukle() {
        List<SkorGirisi> liste = new ArrayList<>();
        Path dosya = Path.of(DOSYA);
        if (!Files.exists(dosya)) return liste;
        try {
            String icerik = Files.readString(dosya);
            // Elle JSON parse: her satır bir nesne
            // Format: [{"isim":"...","skor":123,"seviye":2,"tarih":"..."},...]
            String temiz = icerik.trim();
            if (temiz.isEmpty() || temiz.equals("[]")) return liste;
            // [ ... ] dışındakileri at
            if (temiz.startsWith("[")) temiz = temiz.substring(1);
            if (temiz.endsWith("]"))   temiz = temiz.substring(0, temiz.length() - 1);
            // Her {} bloğunu ayır
            String[] parcalar = temiz.split("\\},\\s*\\{");
            for (String p : parcalar) {
                try {
                    p = p.replace("{", "").replace("}", "").trim();
                    String isim  = degerCek(p, "isim");
                    String tarih = degerCek(p, "tarih");
                    String skorStr  = degerCek(p, "skor");
                    String sevStr   = degerCek(p, "seviye");
                    if (isim.isBlank() || skorStr.isBlank()) continue; // bozuk satırı atla
                    int skor2   = Integer.parseInt(skorStr.trim());
                    int seviye2 = sevStr.isBlank() ? 0 : Integer.parseInt(sevStr.trim());
                    liste.add(new SkorGirisi(isim, skor2, seviye2, tarih));
                } catch (Exception ex) {
                    System.err.println("Satır parse hatası, atlanıyor: " + p);
                }
            }
            Collections.sort(liste);
        } catch (Exception e) {
            System.err.println("Skor dosyası okunurken hata: " + e.getMessage());
        }
        return liste;
    }

    // ── Yaz ──────────────────────────────────────────────────────────────────

    private static void yaz(List<SkorGirisi> liste) {
        StringBuilder sb = new StringBuilder("[\n");
        for (int i = 0; i < liste.size(); i++) {
            SkorGirisi g = liste.get(i);
            sb.append("  {\"isim\":\"").append(g.isim().replace("\"","\\\""))
              .append("\",\"skor\":").append(g.skor())
              .append(",\"seviye\":").append(g.seviye())
              .append(",\"tarih\":\"").append(g.tarih()).append("\"}");
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

    // ── Yardımcı ─────────────────────────────────────────────────────────────

    private static String degerCek(String json, String anahtar) {
        // "anahtar":"deger" veya "anahtar":123
        String aranan = "\"" + anahtar + "\":";
        int bas = json.indexOf(aranan);
        if (bas < 0) return "";
        bas += aranan.length();
        char ilk = json.charAt(bas);
        if (ilk == '"') {
            int son = json.indexOf('"', bas + 1);
            return json.substring(bas + 1, son);
        } else {
            int son = bas;
            while (son < json.length() && (Character.isDigit(json.charAt(son)) || json.charAt(son) == '-'))
                son++;
            return json.substring(bas, son);
        }
    }
}
