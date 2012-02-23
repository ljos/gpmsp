/*

Java Emulation Framework

This library contains a framework for creating emulation software.

Copyright (C) 2002 Erik Duijs (erikduijs@yahoo.com)

Contributors:
- Julien Freilat
- Arnon Goncalves Cardoso
- S.C. Wong
- Romain Tisserand
- David Raingeard


This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

*/

package jef.map;

/**
 * @author Erik Duijs
 * 
 * MemoryReadAddress.java */
public class MemoryReadAddress implements ReadMap {

//	private int			size;
	private ReadHandler	readMap[];
	private UndefinedRead   defread  = new UndefinedRead();

	public ReadHandler	RAM;
	public MEMreadBanked 	BANKS[] = new MEMreadBanked[8];

	public int[]		mem;
	static final boolean debug = false;
    
 //   private int opcodeOffset = 0;

	public MemoryReadAddress(int[] mem) {
		this.mem = mem;
		this.readMap = new ReadHandler[mem.length];
		this.RAM = new MEMread();
		set(0, mem.length - 1, defread);
	}

	@Override
	public void setBankAddress(int bank, int address) {
		BANKS[bank-1].setBankAdr(address); // the 1st bank is 1, not 0
	}

	@Override
	public void set(int from, int until, ReadHandler memRead) {
		for (int i = from; i <= until; i++) {
			this.readMap[i] = memRead;
		}
	}

	@Override
	public void setMR(int from, int until, int readerType) {
		ReadHandler rh = null;

		switch (readerType) {
			case MRA_RAM:
			case MRA_ROM:
			case MRA_NOP:
				rh = RAM;
				break;
			case MRA_BANK1:
			case MRA_BANK2:
			case MRA_BANK3:
			case MRA_BANK4:
			case MRA_BANK5:
			case MRA_BANK6:
			case MRA_BANK7:
			case MRA_BANK8:
				rh = BANKS[readerType - MRA_BANK1] = new MEMreadBanked(from);
				break;
		}

		for (int i = from; i <= until; i++) {
			this.readMap[i] = rh;
		}
	}

	@Override
	public int getSize() {
		return mem.length;
	}
    
    @Override
	public int read(int address) {
        return readMap[address].read(address);
    }

	//public ReadHandler[] get() {
	//	return readMap;
	//}

	public class UndefinedRead implements ReadHandler {
		@Override
		public int read(int address) {
			if (debug) System.out.println("Undefined Read at " + Integer.toHexString(address));
			return 0;
		}
	}

	public class MEMread implements ReadHandler {
		@Override
		public int read(int address) {
			return mem[address];
		}
	}

	public class MEMreadBanked implements ReadHandler {
		int startArea;
		int bank_address;

		public MEMreadBanked(int startArea) {
			this.startArea = startArea;
		}

		@Override
		public int read(int address) {
			return mem[address + this.bank_address];
		}

		public void setBankAdr(int adr) {
			this.bank_address = adr - this.startArea;
		}
	}

    /* (non-Javadoc)
     * @see jef.map.ReadMap#getMemory()
     */
    @Override
	public int[] getMemory() {
        return mem;
    }


}