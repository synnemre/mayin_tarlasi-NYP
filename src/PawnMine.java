import java.util.List;
import java.util.ArrayList;

/** ♟ Piyon � yaln�zca a�a�� hareket eder, �apraz� tehdit eder. */
class PawnMine extends ChessMine {
    public PawnMine(int row, int col) { super(row, col); symbol = "♟"; name = "Piyon"; }

    @Override
    public List<int[]> getPossibleMoves(int boardSize) {
        List<int[]> moves = new ArrayList<>();
        if (row + 1 < boardSize) moves.add(new int[]{row + 1, col});
        return moves;
    }

    @Override
    public List<int[]> getThreatenedSquares(int boardSize) {
        List<int[]> t = new ArrayList<>();
        if (row + 1 < boardSize && col - 1 >= 0)       t.add(new int[]{row + 1, col - 1});
        if (row + 1 < boardSize && col + 1 < boardSize) t.add(new int[]{row + 1, col + 1});
        return t;
    }
}
