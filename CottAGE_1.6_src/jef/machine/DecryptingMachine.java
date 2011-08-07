/*
 * Created on 24-jun-2005
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package jef.machine;

import jef.cpuboard.BasicDecryptingCpuBoard;
import jef.cpuboard.CpuBoard;

/**
 * @author Erik Duijs
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class DecryptingMachine extends BasicMachine {
	
	private int offset;
	
	public DecryptingMachine() {
		super();
		this.offset = 0x10000;
	}
	
	public DecryptingMachine(int offset) {
		super();
		this.offset = offset;
	}

	public CpuBoard createCpuBoard(int id) {
		return new BasicDecryptingCpuBoard(offset);
	}
}
