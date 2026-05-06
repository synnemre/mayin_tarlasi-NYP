import java.util.Set;
import java.util.HashSet;
import java.security.SecureRandom;

public class Board {
    private final int satirSayisi;
    private final int sutunSayisi;
    private int toplamMayin;
    private final Cell[][] izgara;
    private boolean oyunBitti = false;
    private boolean ilkTiklama = true;

    // Performance: track how many safe cells are still closed
    private int kapaliGuvenliHucre;

    public Board(int satirSayisi, int sutunSayisi, int toplamMayin) {
        this.satirSayisi = satirSayisi;
        this.sutunSayisi = sutunSayisi;
        this.toplamMayin = toplamMayin;
        this.izgara = new Cell[satirSayisi][sutunSayisi];
        izgarayiBaslat();
        // All cells are safe until mines are placed; recalculated after first click
        this.kapaliGuvenliHucre = satirSayisi * sutunSayisi;
    }

    private void izgarayiBaslat() {
        for (int s = 0; s < satirSayisi; s++)
            for (int u = 0; u < sutunSayisi; u++)
                izgara[s][u] = new Cell();
    }

    /**
     * Returns the standard 3x3 safe zone around the first click.
     * No mine can be placed in this area, so the clicked cell is always
     * a 0-neighbour cell and the flood-fill produces a reasonable opening —
     * not an enormous cascade across the whole board.
     *
     * This matches the behaviour of the original Windows Minesweeper and
     * virtually all modern implementations.
     */
    private Set<Integer> guvenliBolgeHesapla(int satir, int sutun) {
        Set<Integer> guvenli = new HashSet<>();
        for (int ds = -1; ds <= 1; ds++)
            for (int du = -1; du <= 1; du++) {
                int ys = satir + ds, yu = sutun + du;
                if (sinirIcindeMi(ys, yu))
                    guvenli.add(ys * sutunSayisi + yu);
            }
        return guvenli;
    }

    private void mayinlariYerlestir(Set<Integer> guvenliBolge) {
        int yerlestirilen = 0;
        SecureRandom rastgele = new SecureRandom();
        while (yerlestirilen < toplamMayin) {
            int s = rastgele.nextInt(satirSayisi);
            int u = rastgele.nextInt(sutunSayisi);
            if (!izgara[s][u].isMayinMi() && !guvenliBolge.contains(s * sutunSayisi + u)) {
                izgara[s][u].setMayin(true);
                yerlestirilen++;
            }
        }
        // After placing mines, recalculate closed safe cell count
        kapaliGuvenliHucre = satirSayisi * sutunSayisi - toplamMayin;
    }

    public void altinLeblebileriYerlestir(int adet) {
        java.util.List<Cell> guvenliHucreler = new java.util.ArrayList<>();
        for (int s = 0; s < satirSayisi; s++) {
            for (int u = 0; u < sutunSayisi; u++) {
                if (!izgara[s][u].isMayinMi()) {
                    guvenliHucreler.add(izgara[s][u]);
                }
            }
        }
        java.util.Collections.shuffle(guvenliHucreler, new SecureRandom());
        for (int i = 0; i < Math.min(adet, guvenliHucreler.size()); i++) {
            guvenliHucreler.get(i).setAltinLeblebiMi(true);
        }
    }

    private void komsuMayinlariHesapla() {
        for (int s = 0; s < satirSayisi; s++)
            for (int u = 0; u < sutunSayisi; u++)
                if (!izgara[s][u].isMayinMi())
                    izgara[s][u].setKomsuMayinSayisi(etrafindakiMayinlariSay(s, u));
    }

    private int etrafindakiMayinlariSay(int satir, int sutun) {
        int sayi = 0;
        for (int ds = -1; ds <= 1; ds++)
            for (int du = -1; du <= 1; du++) {
                if (ds == 0 && du == 0) continue;
                int ys = satir + ds, yu = sutun + du;
                if (sinirIcindeMi(ys, yu) && izgara[ys][yu].isMayinMi()) sayi++;
            }
        return sayi;
    }

    private void ilkTiklamaKontrol(int satir, int sutun) {
        if (ilkTiklama) {
            ilkTiklama = false;
            mayinlariYerlestir(guvenliBolgeHesapla(satir, sutun));
            komsuMayinlariHesapla();
        }
    }

    /**
     * Opens a cell. Returns true if a mine was hit.
     * When useLives=true the board does NOT set oyunBitti on mine hit —
     * the caller (LeblebiBoardMode) manages game-over via the lives system.
     */
    public boolean ac(int satir, int sutun, boolean useLives) {
        if (oyunBitti) return false;
        if (!sinirIcindeMi(satir, sutun)) return false;

        ilkTiklamaKontrol(satir, sutun);

        Cell hucre = izgara[satir][sutun];
        if (hucre.isAcildiMi() || hucre.isIsaretlendi()) return false;
        hucre.ac();

        if (hucre.isMayinMi()) {
            if (!useLives) {
                // Classic mode: mine hit ends the game immediately
                oyunBitti = true;
                tumMayinlariGoster();
            }
            // In lives mode: mine is revealed visually but game continues;
            // caller must remove the mine and refresh to keep playing
            return true;
        }

        // Track opened safe cells — single place; flood-fill below uses this same path
        kapaliGuvenliHucre = Math.max(0, kapaliGuvenliHucre - 1);

        if (hucre.getKomsuMayinSayisi() > 0) return false;

        // Flood-fill for empty cells
        for (int ds = -1; ds <= 1; ds++)
            for (int du = -1; du <= 1; du++) {
                if (ds == 0 && du == 0) continue;
                ac(satir + ds, sutun + du, useLives);
            }
        return false;
    }

    /**
     * FIX (bug): Renamed from ac(int, int) to acKlasik(int, int) to make it
     * explicit that this shorthand is for classic (no-lives) mode only.
     * Using the old name in lives mode would silently end the game on a mine hit.
     * Classic mode callers use this; LeblebiBoardMode always calls ac(s, u, true).
     */
    public void acKlasik(int satir, int sutun) {
        ac(satir, sutun, false);
    }

    /**
     * Leblebi moduna özel: tek hücre açar, flood-fill YAPMAZ.
     * Böylece boş bölgelerde yüz hücre birden açılmaz;
     * oyuncu her hücreyi bizzat tıklamak zorunda kalır.
     * Returns true if a mine was hit (aynı ac() sözleşmesi).
     */
    public boolean acTekHucre(int satir, int sutun, boolean useLives) {
        if (oyunBitti) return false;
        if (!sinirIcindeMi(satir, sutun)) return false;

        ilkTiklamaKontrol(satir, sutun);

        Cell hucre = izgara[satir][sutun];
        if (hucre.isAcildiMi() || hucre.isIsaretlendi()) return false;
        hucre.ac();

        if (hucre.isMayinMi()) {
            if (!useLives) {
                oyunBitti = true;
                tumMayinlariGoster();
            }
            return true;
        }

        kapaliGuvenliHucre = Math.max(0, kapaliGuvenliHucre - 1);
        // flood-fill YOK — sadece bu hücre açılır
        return false;
    }

    /**
     * Called by the lives system after a mine hit:
     * removes the mine from the board so play can continue,
     * recalculates neighbour counts, and re-opens the cell safely.
     * Also redistributes mines so the removed mine reappears elsewhere
     * (prevents accidental instant-win after hitting a mine).
     */
    public void mineHitRecover(int satir, int sutun) {
        if (!sinirIcindeMi(satir, sutun)) return;
        Cell hucre = izgara[satir][sutun];
        if (hucre.isMayinMi()) {
            hucre.setMayin(false);
            toplamMayin--;
            // Close the cell so it can be safely re-opened
            hucre.kapat();
            kapaliGuvenliHucre++; // BUG 1 FIX: mine kaldırıldı, hücre güvenli+kapalı oldu, sayaç artırılmalı.

            // Redistribute: place 1 new mine somewhere that is closed, not this cell, not a mine
            Set<Integer> guvenli = new HashSet<>();
            guvenli.add(satir * sutunSayisi + sutun);
            // also exclude already-opened cells
            for (int s = 0; s < satirSayisi; s++)
                for (int u = 0; u < sutunSayisi; u++)
                    if (izgara[s][u].isAcildiMi()) guvenli.add(s * sutunSayisi + u);

            java.util.List<int[]> candidates = new java.util.ArrayList<>();
            for (int s = 0; s < satirSayisi; s++)
                for (int u = 0; u < sutunSayisi; u++)
                    if (!izgara[s][u].isMayinMi() && !guvenli.contains(s * sutunSayisi + u))
                        candidates.add(new int[]{s, u});

            if (!candidates.isEmpty()) {
                int[] c = candidates.get(new SecureRandom().nextInt(candidates.size()));
                izgara[c[0]][c[1]].setMayin(true);
                toplamMayin++;
                kapaliGuvenliHucre--; // The new mine replaced a safe closed cell.
            }
            // Note: if no candidate found (board nearly complete), toplamMayin stays decremented.
            // kapaliGuvenliHucre is correctly incremented because the hit cell was just made safe.

            komsuMayinlariHesapla();
            // Open it again — now it's safe
            ac(satir, sutun, true);
        }
    }

    /**
     * Zirai İlaç: destroys worms in a 3x3 area and safely opens all cells.
     * Returns number of mines destroyed.
     *
     * FIX (quality): The 3×3 cells are now opened via Board.ac() instead of
     * directly calling Cell.ac() and manually adjusting kapaliGuvenliHucre.
     * This means all decrement logic lives in one place (Board.ac), so future
     * changes to the open path cannot accidentally desync the counter.
     */
    public int zirayiIlacUygula(int satir, int sutun, int cap) {
        if (!sinirIcindeMi(satir, sutun)) return 0;

        ilkTiklamaKontrol(satir, sutun);

        int yokEdilenSolucan = 0;

        java.util.List<int[]> etkiAlani = new java.util.ArrayList<>();
        if (cap == 100) {
            // Level 3: Cross clear
            for (int s = 0; s < satirSayisi; s++) etkiAlani.add(new int[]{s, sutun});
            for (int u = 0; u < sutunSayisi; u++) {
                if (u != sutun) etkiAlani.add(new int[]{satir, u});
            }
        } else {
            // Level 1 (cap=1: 3x3) or Level 2 (cap=2: 5x5)
            for (int ds = -cap; ds <= cap; ds++) {
                for (int du = -cap; du <= cap; du++) {
                    etkiAlani.add(new int[]{satir + ds, sutun + du});
                }
            }
        }

        // 1. Remove mines in the area
        for (int[] pos : etkiAlani) {
            int ys = pos[0], yu = pos[1];
            if (sinirIcindeMi(ys, yu) && izgara[ys][yu].isMayinMi()) {
                izgara[ys][yu].setMayin(false);
                toplamMayin--;
                yokEdilenSolucan++;
                kapaliGuvenliHucre++; // We destroyed a mine, so it became a safe closed cell.
            }
        }

        // 2. Recalculate neighbour counts for the whole board
        komsuMayinlariHesapla();

        // 3. Open all cells in the area via Board.ac()
        for (int[] pos : etkiAlani) {
            int ys = pos[0], yu = pos[1];
            if (sinirIcindeMi(ys, yu))
                ac(ys, yu, true);
        }

        return yokEdilenSolucan;
    }

    /**
     * Karga Upgrade (Level 1: 1 random, Level 2: 3 random, Level 3: closest)
     */
    public java.util.List<int[]> kargaGorus(int level, int lastR, int lastC) {
        java.util.List<int[]> mayinlar = new java.util.ArrayList<>();
        for (int s = 0; s < satirSayisi; s++) {
            for (int u = 0; u < sutunSayisi; u++) {
                if (izgara[s][u].isMayinMi() && !izgara[s][u].isAcildiMi()) {
                    mayinlar.add(new int[]{s, u});
                }
            }
        }
        if (mayinlar.isEmpty()) return mayinlar;

        java.util.List<int[]> sonuc = new java.util.ArrayList<>();
        if (level == 3) {
            // En yakın olanı bul
            int[] enYakin = mayinlar.get(0);
            double minMesafe = Double.MAX_VALUE;
            for (int[] m : mayinlar) {
                double mesafe = Math.pow(m[0] - lastR, 2) + Math.pow(m[1] - lastC, 2);
                if (mesafe < minMesafe) {
                    minMesafe = mesafe;
                    enYakin = m;
                }
            }
            sonuc.add(enYakin);
            return sonuc;
        }

        // Level 1 (1 tane) veya Level 2 (3 tane)
        java.util.Collections.shuffle(mayinlar, new SecureRandom());
        int adet = (level == 2) ? 3 : 1;
        for (int i = 0; i < Math.min(adet, mayinlar.size()); i++) {
            sonuc.add(mayinlar.get(i));
        }
        return sonuc;
    }

    // FIX (minor): Removed the one-line tumMayinlariAc() wrapper — callers now
    // call tumMayinlariGoster() directly, eliminating pointless indirection.

    /** Reveals all mines (used when classic game ends or lives run out). */
    public void tumMayinlariGoster() {
        for (int s = 0; s < satirSayisi; s++)
            for (int u = 0; u < sutunSayisi; u++)
                if (izgara[s][u].isMayinMi()) izgara[s][u].ac();
    }

    public boolean sinirIcindeMi(int s, int u) {
        return s >= 0 && s < satirSayisi && u >= 0 && u < sutunSayisi;
    }

    public Cell    getHucre(int s, int u) { return izgara[s][u]; }
    public boolean isOyunBitti()          { return oyunBitti; }
    public boolean isIlkTiklama()         { return ilkTiklama; }
    public int     getSatirSayisi()       { return satirSayisi; }
    public int     getSutunSayisi()       { return sutunSayisi; }
    public int     getToplamMayin()       { return toplamMayin; }

    public boolean kazanildiMi() {
        // Fast path: use counter instead of scanning every cell
        return !oyunBitti && kapaliGuvenliHucre == 0;
    }

    // English aliases
    public void    revealClassic(int s, int u) { acKlasik(s, u); }
    public boolean isGameOver()                { return isOyunBitti(); }
    public boolean isWon()                     { return kazanildiMi(); }
    public Cell    getCell(int s, int u)       { return getHucre(s, u); }
}
