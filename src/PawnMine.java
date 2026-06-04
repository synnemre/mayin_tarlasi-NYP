import java.util.List;
import java.util.ArrayList;

/** ♟ Piyon — yalnızca aşağı hareket eder, çaprazı tehdit eder. */
class PawnMine extends ChessMine {
    public PawnMine(int row, int col) {
        super(row, col);
        symbol = "♟";
        name = "Piyon";
    }

    @Override
    public List<int[]> getPossibleMoves(int boardSize) {
        List<int[]> moves = new ArrayList<>();

        // YENİ KURAL: Piyonun son satıra (boardSize - 1) inmesini engelliyoruz.
        // Bu yüzden sınırımızı "boardSize - 1" olarak belirledik.
        // Piyon en fazla sondan bir önceki satırda duracak.
        if (row + 1 < boardSize - 1) {
            moves.add(new int[]{row + 1, col});
        }

        return moves;
    }

    @Override
    public List<int[]> getThreatenedSquares(int boardSize) {
        List<int[]> t = new ArrayList<>();

        // Tehdit hesaplamasında bir değişiklik yok, son satıra kadar tehdit edebilir.
        if (row + 1 < boardSize && col - 1 >= 0) {
            t.add(new int[]{row + 1, col - 1});
        }
        if (row + 1 < boardSize && col + 1 < boardSize) {
            t.add(new int[]{row + 1, col + 1});
        }

        return t;
    }
}