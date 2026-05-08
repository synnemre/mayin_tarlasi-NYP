import java.util.ArrayList;
import java.util.List;
/** ♞ At � L �eklinde hareket eder. */
class KnightMine extends ChessMine {
    public KnightMine(int row, int col) { super(row, col); symbol = "♞"; name = "At"; }

    @Override
    public List<int[]> getPossibleMoves(int boardSize) {
        List<int[]> moves = new ArrayList<>();
        int[][] off = {{2,1},{2,-1},{-2,1},{-2,-1},{1,2},{1,-2},{-1,2},{-1,-2}};
        for (int[] o : off) {
            int nr = row + o[0], nc = col + o[1];
            if (nr >= 0 && nr < boardSize && nc >= 0 && nc < boardSize)
                moves.add(new int[]{nr, nc});
        }
        return moves;
    }

    @Override
    public List<int[]> getThreatenedSquares(int boardSize) { return getPossibleMoves(boardSize); }
}
