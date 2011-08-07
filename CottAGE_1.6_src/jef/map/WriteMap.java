/*
 * Created on Aug 24, 2005
 * by edy
 */
package jef.map;

/**
 * @author edy
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public interface WriteMap extends WriteHandler {
    /* Memory Write handler IDs */
    public static final int MWA_RAM = 0;

    public static final int MWA_ROM = 1;

    public static final int MWA_NOP = 2;

    public static final int MWA_BANK1 = 3;

    public static final int MWA_BANK2 = 4;

    public static final int MWA_BANK3 = 5;

    public static final int MWA_BANK4 = 6;

    public static final int MWA_BANK5 = 7;

    public static final int MWA_BANK6 = 8;

    public static final int MWA_BANK7 = 9;

    public static final int MWA_BANK8 = 10;

    public abstract void set(int from, int until, WriteHandler memWrite);

    public abstract void setMW(int from, int until, int type);

    public abstract int getSize();
    
}