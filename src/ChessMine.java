import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * ChessMine — Satranç Mayın Tarlası Modu için soyut taş sınıfı.
 * Her taş türü (Piyon, At, Fil, Kale, Vezir) bu sınıfı extend eder.
 */
public abstract class ChessMine {
    protected int row, col;
    protected String symbol;
    protected String name;
    protected static final Random random = new Random();

    public void setPosition(int r, int c) {
        this.row = r;
        this.col = c;
    }

    public ChessMine(int row, int col) {
        this.row = row;
        this.col = col;
    }

    /** Bu taşın gidebileceği tüm geçerli kareleri döndürür. */
    public abstract List<int[]> getPossibleMoves(int boardSize);

    /** Bu taşın tehdit ettiği tüm kareleri döndürür (ipucu sayıları için). */
    public abstract List<int[]> getThreatenedSquares(int boardSize);

    /** Taşı tahta üzerinde bir adım hareket ettirir. Açık karelere gitmekten kaçınır. */
    /** Taşı tahta üzerinde bir adım hareket ettirir. Sadece kapalı karelere gidebilir. */
    public void move(int boardSize, boolean[][] revealed) {
        List<int[]> moves = getPossibleMoves(boardSize);
        List<int[]> validMoves = new ArrayList<>();

        // Taşlar SADECE henüz açılmamış (kapalı) karelere gidebilir
        for (int[] m : moves) {
            if (!revealed[m[0]][m[1]]) {
                validMoves.add(m);
            }
        }

        // Gidebileceği kapalı bir kare varsa hareket et, yoksa olduğu yerde kal (köşeye sıkıştı)
        if (!validMoves.isEmpty()) {
            int[] pick = validMoves.get(random.nextInt(validMoves.size()));
            row = pick[0];
            col = pick[1];
        }
    }

    public int    getRow()    { return row; }
    public int    getCol()    { return col; }
    public String getSymbol() { return symbol; }
    public String getName()   { return name; }
}