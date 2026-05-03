public class Cell {
    private boolean mayinMi;
    private boolean acildiMi;
    private boolean isaretlendi;
    private int komsuMayinSayisi;

    public Cell() {
        this.mayinMi = false;
        this.acildiMi = false;
        this.isaretlendi = false;
        this.komsuMayinSayisi = 0;
    }

    public boolean isMayinMi()          { return mayinMi; }
    public boolean isAcildiMi()         { return acildiMi; }
    public boolean isIsaretlendi()      { return isaretlendi; }
    public int     getKomsuMayinSayisi(){ return komsuMayinSayisi; }

    public void setMayin(boolean mayinMi)            { this.mayinMi = mayinMi; }
    public void setKomsuMayinSayisi(int sayi)        { this.komsuMayinSayisi = sayi; }

    public void ac() {
        if (!isaretlendi) this.acildiMi = true;
    }

    /** Closes a previously opened cell (used by the lives-recovery path). */
    public void kapat() {
        this.acildiMi = false;
    }

    public void isaretiDegistir() {
        if (!acildiMi) this.isaretlendi = !this.isaretlendi;
    }

    public boolean bosHucreMi() { return komsuMayinSayisi == 0 && !mayinMi; }

    // English aliases
    public boolean isMine()         { return mayinMi; }
    public boolean isRevealed()     { return acildiMi; }
    public boolean isFlagged()      { return isaretlendi; }
    public int     getNeighborMines(){ return komsuMayinSayisi; }
    public void    reveal()         { ac(); }
    public void    toggleFlag()     { isaretiDegistir(); }
    public boolean isEmpty()        { return bosHucreMi(); }
}
