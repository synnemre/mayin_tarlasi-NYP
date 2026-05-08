import java.util.ArrayList;
import java.util.List;
/** ♜ Kale � d�z y�nlerde kayar. */
class RookMine extends ChessMine {
    public RookMine(int row, int col) { super(row, col); symbol = "♜"; name = "Kale"; }

    @Override
    public List<int[]> getPossibleMoves(int boardSize) {
        List<int[]> moves = new ArrayList<>();
        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
        for (int[] d : dirs) {
            int nr = row + d[0], nc = col + d[1];
            if (nr >= 0 && nr < boardSize && nc >= 0 && nc < boardSize)
                moves.add(new int[]{nr, nc});
        }
        return moves;
    }

    @Override
    public List<int[]> getThreatenedSquares(int boardSize) {
        List<int[]> t = new ArrayList<>();
        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
        for (int[] d : dirs) {
            int nr = row + d[0], nc = col + d[1];
            while (nr >= 0 && nr < boardSize && nc >= 0 && nc < boardSize) {
                t.add(new int[]{nr, nc}); nr += d[0]; nc += d[1];
            }
        }
        return t;
    }
}
