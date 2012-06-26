/*
 * Created on Aug 24, 2005
 * by edy
 */
package jef.map;

/**
 * @author edy
 * 
 *         To change the template for this generated type comment go to Window -
 *         Preferences - Java - Code Generation - Code and Comments
 */
public interface ReadMap extends ReadHandler {
	/* Memory Read handler IDs */
	public static final int MRA_RAM = 0;

	public static final int MRA_ROM = 1;

	public static final int MRA_NOP = 2;

	public static final int MRA_BANK1 = 3;

	public static final int MRA_BANK2 = 4;

	public static final int MRA_BANK3 = 5;

	public static final int MRA_BANK4 = 6;

	public static final int MRA_BANK5 = 7;

	public static final int MRA_BANK6 = 8;

	public static final int MRA_BANK7 = 9;

	public static final int MRA_BANK8 = 10;

	public abstract void setBankAddress(int b, int address);

	public abstract void set(int from, int until, ReadHandler memRead);

	public abstract void setMR(int from, int until, int readerType);

	public abstract int getSize();

	public abstract int[] getMemory();

}