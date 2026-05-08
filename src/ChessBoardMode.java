import java.security.SecureRandom;
import java.util.*;

/**
 * ChessBoardMode — Satranç Mayın Tarlası Modu
 *
 * KlasikBoardMode ve LeblebiBoardMode ile paralel tasarım.
 * Board sınıfını KULLANMAZ — kendi 9×9 tahtasını yönetir.
 * Sebep: Board sınıfı statik mayın yerleşimi için tasarlandı;
 * bu modda taşlar her hamleden sonra hareket eder.
 *
 * UI katmanı (MinesweeperApp) sadece şu metodları çağırır:
 *   hucreAc(r, c)       → boolean (taşa basıldı mı?)
 *   isaretKoy(r, c)     → void
 *   sureyiGuncelle(int) → boolean (süre doldu mu?)
 *   kazanmaKontrol()    → boolean
 *   getter'lar
 */
public class ChessBoardMode {

    // ── Sabitler ──────────────────────────────────────────────────────────────

    public static final int BOARD_SIZE = 9;

    public record ChessAyar(
            String etiket,
            int    zorluk,   // 1=Kolay 2=Orta 3=Zor
            int    sureSaniye
    ) {}

    public static final ChessAyar[] PRESETLER = {
        new ChessAyar("♟ Kolay  (Piyon + At)",        1, 180),
        new ChessAyar("♝ Orta   (+ Fil)",             2, 240),
        new ChessAyar("♛ Zor    (+ Kale + Vezir)",    3, 300),
    };

    // ── Tahta durumu ──────────────────────────────────────────────────────────

    private final boolean[][]     revealed;   // açılan kareler
    private final boolean[][]     flagged;    // bayraklı kareler
    private       int[][]         threatCount; // kaç taş tehdit ediyor
    private       ChessMine[][]   mineBoard;  // o karede hangi taş
    private       List<ChessMine> mines;

    private final int difficulty;
    private final int baslangicSuresi;
    private       int kalanSure;

    private boolean oyunBitti  = false;
    private boolean kazanildi  = false;
    private boolean sureDoldu  = false;
    private boolean ilkTiklama = true;

    private int toplamGuvenliHucre;
    private int acilanGuvenliHucre;

    // Son hamle koordinatları (hasar karesini UI'a bildirmek için)
    private int sonHamleR = -1, sonHamleC = -1;

    // ── Yapıcı ────────────────────────────────────────────────────────────────

    public ChessBoardMode(int difficulty, int sureSaniye) {
        this.difficulty       = difficulty;
        this.baslangicSuresi  = sureSaniye;
        this.kalanSure        = sureSaniye;
        this.revealed         = new boolean[BOARD_SIZE][BOARD_SIZE];
        this.flagged          = new boolean[BOARD_SIZE][BOARD_SIZE];
        this.threatCount      = new int[BOARD_SIZE][BOARD_SIZE];
        this.mineBoard        = new ChessMine[BOARD_SIZE][BOARD_SIZE];
        this.mines            = new ArrayList<>();
    }

    // ── Yerleştirme (ilk tıklamadan sonra) ───────────────────────────────────

    private void taslariYerlestir(int ilkR, int ilkC) {
        int count = getMineCount();
        Set<String> used = new HashSet<>();
        // İlk tıklanan ve komşuları güvenli bölge
        for (int dr = -1; dr <= 1; dr++)
            for (int dc = -1; dc <= 1; dc++) {
                int nr = ilkR + dr, nc = ilkC + dc;
                if (inBounds(nr, nc)) used.add(nr + "," + nc);
            }

        SecureRandom rnd = new SecureRandom();
        while (mines.size() < count) {
            int r = rnd.nextInt(BOARD_SIZE);
            int c = rnd.nextInt(BOARD_SIZE);
            String key = r + "," + c;
            if (!used.contains(key)) {
                used.add(key);
                ChessMine mine = createMine(r, c);
                mines.add(mine);
                mineBoard[r][c] = mine;
            }
        }
        toplamGuvenliHucre = BOARD_SIZE * BOARD_SIZE - mines.size();
        updateThreatCount();
    }

    public int getMineCount() {
        return switch (difficulty) {
            case 1 -> 8;
            case 2 -> 6;
            default -> 4;
        };
    }

    private ChessMine createMine(int r, int c) {
        SecureRandom rnd = new SecureRandom();
        int type = switch (difficulty) {
            case 1 -> rnd.nextInt(2);       // Piyon, At
            case 2 -> rnd.nextInt(3);       // Piyon, At, Fil
            default -> rnd.nextInt( 5);      // Hepsi
        };
        return switch (type) {
            case 0 -> new PawnMine(r, c);
            case 1 -> new KnightMine(r, c);
            case 2 -> new BishopMine(r, c);
            case 3 -> new RookMine(r, c);
            default -> new QueenMine(r, c);
        };
    }

    // ── Tehdit sayısı güncellemesi ────────────────────────────────────────────

    private void updateThreatCount() {
        for (int[] row : threatCount) Arrays.fill(row, 0);
        for (ChessMine mine : mines) {
            for (int[] sq : mine.getThreatenedSquares(BOARD_SIZE)) {
                threatCount[sq[0]][sq[1]]++;
            }
        }
    }

    // ── Hücre açma ────────────────────────────────────────────────────────────

    /**
     * Bir kareyi açar.
     * @return true → taşa basıldı (oyun bitti); false → güvenli açış
     */
    public boolean hucreAc(int r, int c) {
        if (oyunBitti || kazanildi) return false;
        if (!inBounds(r, c) || revealed[r][c] || flagged[r][c]) return false;

        if (ilkTiklama) {
            ilkTiklama = false;
            taslariYerlestir(r, c);
        }

        // Taş var mı?
        if (mineBoard[r][c] != null) {
            revealed[r][c] = true;
            oyunBitti = true;
            sonHamleR = r; sonHamleC = c;
            // Tüm taşları göster
            for (ChessMine mine : mines) revealed[mine.getRow()][mine.getCol()] = true;
            return true;
        }

        // Güvenli açış
        openSafe(r, c);

        // Taşları hareket ettir
        moveMines();
        updateThreatCount();

        // Kazanma kontrolü
        kazanmaKontrol();
        return false;
    }

    private void openSafe(int r, int c) {
        if (!inBounds(r, c) || revealed[r][c] || flagged[r][c] || mineBoard[r][c] != null) return;
        revealed[r][c] = true;
        acilanGuvenliHucre++;
        // Flood-fill: tehdit 0 ise komşuları da aç
        if (threatCount[r][c] == 0) {
            for (int dr = -1; dr <= 1; dr++)
                for (int dc = -1; dc <= 1; dc++)
                    if (!(dr == 0 && dc == 0)) openSafe(r + dr, c + dc);
        }
    }

    // ── Taş hareketi ──────────────────────────────────────────────────────────

    private void moveMines() {
        // mineBoard temizle
        for (ChessMine[] row : mineBoard) Arrays.fill(row, null);

        // Her taşı hareket ettir
        for (ChessMine mine : mines) mine.move(BOARD_SIZE, revealed);

        // Çakışma: aynı kareye gelen taşları çıkar
        Set<String> occupied = new HashSet<>();
        List<ChessMine> surviving = new ArrayList<>();
        for (ChessMine mine : mines) {
            String key = mine.getRow() + "," + mine.getCol();
            if (!occupied.contains(key)) {
                occupied.add(key);
                surviving.add(mine);
                mineBoard[mine.getRow()][mine.getCol()] = mine;
            }
        }
        mines = surviving;
        toplamGuvenliHucre = BOARD_SIZE * BOARD_SIZE - mines.size();
    }

    // ── Bayrak ────────────────────────────────────────────────────────────────

    public void isaretKoy(int r, int c) {
        if (!oyunBitti && !kazanildi && !revealed[r][c])
            flagged[r][c] = !flagged[r][c];
    }

    // ── Süre ──────────────────────────────────────────────────────────────────

    /**
     * Her saniye çağrılır.
     * @return true → bu tick'te süre doldu
     */
    public boolean sureyiGuncelle(int gecen) {
        if (oyunBitti || kazanildi) return false;
        kalanSure = Math.max(0, kalanSure - gecen);
        if (kalanSure <= 0) {
            sureDoldu = true;
            oyunBitti = true;
            for (ChessMine mine : mines) revealed[mine.getRow()][mine.getCol()] = true;
            return true;
        }
        return false;
    }

    // ── Kazanma ───────────────────────────────────────────────────────────────

    public boolean kazanmaKontrol() {
        if (oyunBitti) return false;
        if (!ilkTiklama && acilanGuvenliHucre >= toplamGuvenliHucre) {
            kazanildi = true;
            oyunBitti = true;
        }
        return kazanildi;
    }

    // ── Skor ──────────────────────────────────────────────────────────────────

    public int skorHesapla() {
        int base     = kalanSure * 10;
        int hizBonus = Math.max(0, (baslangicSuresi / 3 - (baslangicSuresi - kalanSure)) * 5);
        int zorBonus = difficulty * 50;
        return base + hizBonus + zorBonus;
    }

    // ── Yardımcılar ───────────────────────────────────────────────────────────

    private boolean inBounds(int r, int c) {
        return r >= 0 && r < BOARD_SIZE && c >= 0 && c < BOARD_SIZE;
    }

    // ── Getter'lar ────────────────────────────────────────────────────────────

    public boolean      isRevealed(int r, int c)   { return revealed[r][c]; }
    public boolean      isFlagged(int r, int c)    { return flagged[r][c]; }
    public boolean      isMine(int r, int c)       { return mineBoard[r][c] != null; }
    public ChessMine    getMine(int r, int c)      { return mineBoard[r][c]; }
    public int          getThreat(int r, int c)    { return threatCount[r][c]; }
    public boolean      isOyunBitti()              { return oyunBitti; }
    public boolean      isKazanildi()              { return kazanildi; }
    public boolean      isSureDoldu()              { return sureDoldu; }
    public boolean      isOyunAktif()              { return !oyunBitti && !kazanildi; }
    public int          getKalanSure()             { return kalanSure; }
    public int          getBaslangicSuresi()       { return baslangicSuresi; }
    public int          getDifficulty()            { return difficulty; }
    public List<ChessMine> getMines()              { return mines; }
    public int          getSonHamleR()             { return sonHamleR; }
    public int          getSonHamleC()             { return sonHamleC; }
    public boolean      isIlkTiklama()             { return ilkTiklama; }
}
