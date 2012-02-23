/*
 * Created on 14-aug-2005
 * 
 * To change the template for this generated file go to Window - Preferences -
 * Java - Code Generation - Code and Comments
 */
package jef.sound.chip.fm;

/**
 * @author Erik Duijs
 * 
 * To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Generation - Code and Comments
 */
public class FM3Slot {
	/* OPN 3slot struct */
	public long[] fc = new long[3];  // fnum3,blk3 :calculated
	public int[] fn_h = new int[3];  // freq3 latch
	public int[] kcode = new int[3]; // key code
}
