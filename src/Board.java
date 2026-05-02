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

    private Set<Integer> guvenliiBolgeHesapla(int satir, int sutun) {
        Set<Integer> guvenli = new HashSet<>();
        guvenli.add(satir * sutunSayisi + sutun);
        return guvenli;
    }

    private void mayinlariYerlestir(Set<Integer> guvenliiBolge) {
        int yerlestirilen = 0;
        SecureRandom rastgele = new SecureRandom();
        while (yerlestirilen < toplamMayin) {
            int s = rastgele.nextInt(satirSayisi);
            int u = rastgele.nextInt(sutunSayisi);
            if (!izgara[s][u].isMayinMi() && !guvenliiBolge.contains(s * sutunSayisi + u)) {
                izgara[s][u].setMayin(true);
                yerlestirilen++;
            }
        }
        // After placing mines, recalculate closed safe cell count
        kapaliGuvenliHucre = satirSayisi * sutunSayisi - toplamMayin;
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
            mayinlariYerlestir(guvenliiBolgeHesapla(satir, sutun));
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
                tumMayinlariAc();
            }
            // In lives mode: mine is revealed visually but game continues;
            // caller must remove the mine and refresh to keep playing
            return true;
        }

        // Track opened safe cells
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

    /** Classic mode shorthand */
    public void ac(int satir, int sutun) {
        ac(satir, sutun, false);
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
            }
            // Note: if no candidate found (board nearly complete), toplamMayin stays decremented.
            // kapaliGuvenliHucre is still > 0 because the hit cell was just reopened via ac(),
            // so kazanildiMi() won't fire spuriously.

            komsuMayinlariHesapla();
            // Open it again — now it's safe
            ac(satir, sutun, true);
        }
    }

    /**
     * Zirai İlaç: destroys worms in a 3x3 area and safely opens all cells.
     * Returns number of mines destroyed.
     */
    public int zirayiIlacUygula(int satir, int sutun) {
        if (!sinirIcindeMi(satir, sutun)) return 0;

        ilkTiklamaKontrol(satir, sutun);

        int yokEdilenSolucan = 0;

        // 1. Remove mines in the 3x3 area
        for (int ds = -1; ds <= 1; ds++)
            for (int du = -1; du <= 1; du++) {
                int ys = satir + ds, yu = sutun + du;
                if (sinirIcindeMi(ys, yu) && izgara[ys][yu].isMayinMi()) {
                    izgara[ys][yu].setMayin(false);
                    toplamMayin--;
                    yokEdilenSolucan++;
                }
            }

        // 2. Recalculate neighbour counts for the whole board (fast for small grids)
        komsuMayinlariHesapla();

        // 3. Open all cells in the 3x3 area
        for (int ds = -1; ds <= 1; ds++)
            for (int du = -1; du <= 1; du++) {
                int ys = satir + ds, yu = sutun + du;
                if (sinirIcindeMi(ys, yu)) {
                    Cell h = izgara[ys][yu];
                    if (!h.isAcildiMi() && !h.isIsaretlendi()) {
                        h.ac();
                        kapaliGuvenliHucre = Math.max(0, kapaliGuvenliHucre - 1);
                        if (h.getKomsuMayinSayisi() == 0) {
                            for (int ds2 = -1; ds2 <= 1; ds2++)
                                for (int du2 = -1; du2 <= 1; du2++)
                                    if (!(ds2 == 0 && du2 == 0))
                                        ac(ys + ds2, yu + du2, true);
                        }
                    }
                }
            }

        return yokEdilenSolucan;
    }

    /**
     * Karga: returns a random unrevealed mine's coordinates, or null.
     */
    public int[] rastgeleMayinBul() {
        java.util.List<int[]> mayinlar = new java.util.ArrayList<>();
        for (int s = 0; s < satirSayisi; s++)
            for (int u = 0; u < sutunSayisi; u++)
                if (izgara[s][u].isMayinMi() && !izgara[s][u].isAcildiMi())
                    mayinlar.add(new int[]{s, u});
        if (mayinlar.isEmpty()) return null;
        return mayinlar.get(new SecureRandom().nextInt(mayinlar.size()));
    }

    private void tumMayinlariAc() {
        tumMayinlariGoster();
    }

    /** Reveals all mines (used when classic game ends or lives run out). */
    public void tumMayinlariGoster() {
        for (int s = 0; s < satirSayisi; s++)
            for (int u = 0; u < sutunSayisi; u++)
                if (izgara[s][u].isMayinMi()) izgara[s][u].ac();
    }

    public boolean sinirIcindeMi(int s, int u) {
        return s >= 0 && s < satirSayisi && u >= 0 && u < sutunSayisi;
    }

    public Cell getHucre(int s, int u) { return izgara[s][u]; }
    public boolean isOyunBitti()       { return oyunBitti; }
    public int getSatirSayisi()        { return satirSayisi; }
    public int getSutunSayisi()        { return sutunSayisi; }
    public int getToplamMayin()        { return toplamMayin; }

    public boolean kazanildiMi() {
        // Fast path: use counter instead of scanning every cell
        return !oyunBitti && kapaliGuvenliHucre == 0;
    }

    // English aliases
    public void    reveal(int s, int u)  { ac(s, u); }
    public boolean isGameOver()          { return isOyunBitti(); }
    public boolean isWon()               { return kazanildiMi(); }
    public Cell    getCell(int s, int u) { return getHucre(s, u); }
}
