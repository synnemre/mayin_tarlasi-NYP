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
 */
public class SkorTablosu {

    public static final String DOSYA = "scores.json";
    public static final String MOD_LEBLEBI = "leblebi";
    public static final String MOD_ZAMANLI = "zamanlı";

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
        String tarih = java.time.LocalDate.now().toString();
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
            String icerik = Files.readString(dosya);
            String temiz = icerik.trim();
            if (temiz.isEmpty() || temiz.equals("[]")) return liste;
            if (temiz.startsWith("[")) temiz = temiz.substring(1);
            if (temiz.endsWith("]"))   temiz = temiz.substring(0, temiz.length() - 1);
            String[] parcalar = temiz.split("\\},\\s*\\{");
            for (String p : parcalar) {
                p = p.replace("{", "").replace("}", "").trim();
                String isim   = degerCek(p, "isim");
                String tarih  = degerCek(p, "tarih");
                String modStr = degerCek(p, "mod");
                if (modStr == null || modStr.isBlank()) modStr = MOD_LEBLEBI;
                int skor2        = parseIntSafe(degerCek(p, "skor"));
                int seviye2      = parseIntSafe(degerCek(p, "seviye"));
                int satirSayisi2 = parseIntSafe(degerCek(p, "satir"));
                int sutunSayisi2 = parseIntSafe(degerCek(p, "sutun"));
                int mayinSayisi2 = parseIntSafe(degerCek(p, "mayin"));
                int sureSiniri2  = parseIntSafe(degerCek(p, "sure"));
                liste.add(new SkorGirisi(isim, skor2, seviye2, tarih, modStr,
                    satirSayisi2, sutunSayisi2, mayinSayisi2, sureSiniri2));
            }
            Collections.sort(liste);
        } catch (Exception e) {
            System.err.println("Skor dosyası okunurken hata: " + e.getMessage());
        }
        return liste;
    }

    private static void yaz(List<SkorGirisi> liste) {
        StringBuilder sb = new StringBuilder("[\n");
        for (int i = 0; i < liste.size(); i++) {
            SkorGirisi g = liste.get(i);
            String m = (g.mod() == null || g.mod().isBlank()) ? MOD_LEBLEBI : g.mod();
            sb.append("  {\"isim\":\"").append(g.isim().replace("\"","\\\""))
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

    private static String degerCek(String json, String anahtar) {
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

    private static int parseIntSafe(String s) {
        if (s == null || s.isBlank()) return 0;
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return 0; }
    }
}
