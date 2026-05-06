public class Cell {
    private boolean mayinMi;
    private boolean acildiMi;
    private boolean isaretlendi;
    private int komsuMayinSayisi;
    private boolean altinLeblebiMi;

    /**
     * Returned by ac() so callers can distinguish between
     * a successful open, a flag-blocked open, and a no-op on an already-open cell.
     */
    public enum OpenResult { OPENED, BLOCKED_BY_FLAG, ALREADY_OPEN }

    public Cell() {
        this.mayinMi = false;
        this.acildiMi = false;
        this.isaretlendi = false;
        this.komsuMayinSayisi = 0;
        this.altinLeblebiMi = false;
    }

    public boolean isMayinMi()          { return mayinMi; }
    public boolean isAcildiMi()         { return acildiMi; }
    public boolean isIsaretlendi()      { return isaretlendi; }
    public int     getKomsuMayinSayisi(){ return komsuMayinSayisi; }
    public boolean isAltinLeblebiMi()   { return altinLeblebiMi; }

    public void setMayin(boolean mayinMi)            { this.mayinMi = mayinMi; }
    public void setKomsuMayinSayisi(int sayi)        { this.komsuMayinSayisi = sayi; }
    public void setAltinLeblebiMi(boolean altinLeblebiMi) { this.altinLeblebiMi = altinLeblebiMi; }

    /**
     * Attempts to open the cell.
     * Returns OPENED on success, BLOCKED_BY_FLAG if flagged, ALREADY_OPEN if already revealed.
     * Callers that don't need the result can safely ignore the return value.
     */
    public OpenResult ac() {
        if (acildiMi)    return OpenResult.ALREADY_OPEN;
        if (isaretlendi) return OpenResult.BLOCKED_BY_FLAG;
        this.acildiMi = true;
        return OpenResult.OPENED;
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
    public boolean   isMine()          { return mayinMi; }
    public boolean   isRevealed()      { return acildiMi; }
    public boolean   isFlagged()       { return isaretlendi; }
    public int       getNeighborMines(){ return komsuMayinSayisi; }
    public OpenResult reveal()         { return ac(); }
    public void      toggleFlag()      { isaretiDegistir(); }
    public boolean   isEmpty()         { return bosHucreMi(); }
    public boolean   isGoldenLeblebi() { return altinLeblebiMi; }
}
