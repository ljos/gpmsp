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
 * MemoryWriteAddress.java */
public class MemoryWriteAddress implements WriteMap {

//	private int			size;
	public WriteHandler 	writeMap[];
	private UndefinedWrite  defwrite  = new UndefinedWrite();
	private WriteHandler	RAM;
	private WriteHandler	ROM;
	public int[]		mem;
	static final boolean debug = false;

	public MEMWriteBanked 	BANKS[] = new MEMWriteBanked[8];
	
	public MemoryWriteAddress(int[] mem) {
		this.mem = mem;
		this.RAM	  = new RAMwrite();
		this.ROM	  = new ROMwrite();
		this.writeMap = new WriteHandler[mem.length];
		set(0, mem.length-1, defwrite);
	}

	public MemoryWriteAddress(int size) {
	//	this.size = size;
		this.writeMap = new WriteHandler[size];
		set(0, size-1, defwrite);
	}

	@Override
	public void set(int from, int until, WriteHandler memWrite) {
		for (int i = from; i <= until; i++) {
			this.writeMap[i] = memWrite;
		}
	}

	@Override
	public void setMW(int from, int until, int type) {
		WriteHandler wh = null; //(type == 0) ? RAM : ROM;

		switch (type) {
		case MWA_RAM:
			wh = RAM;
			break;
		case MWA_ROM:
		case MWA_NOP:
			wh = ROM;
			break;
		case MWA_BANK1:
		case MWA_BANK2:
		case MWA_BANK3:
		case MWA_BANK4:
		case MWA_BANK5:
		case MWA_BANK6:
		case MWA_BANK7:
		case MWA_BANK8:
			wh = BANKS[type - MWA_BANK1] = new MEMWriteBanked(from);
			break;
		}
		for (int i = from; i <= until; i++) {
			this.writeMap[i] = wh;
		}
	}

	@Override
	public int getSize() {
		return mem.length;
	}

	/*public WriteHandler[] get() {
		return writeMap;
	}*/
    
    @Override
	public void write(int address, int data) {
        writeMap[address].write(address, data);
    }

	public class UndefinedWrite implements WriteHandler {
		@Override
		public void write(int address, int value) {
			if (debug) System.out.println("Undefined Write at " + Integer.toHexString(address) + ", value : " + Integer.toHexString(value));
		}
	}

	public class RAMwrite implements WriteHandler {
		@Override
		public void write(int address, int value) {
			mem[address] = value;
		}
	}

	public class ROMwrite implements WriteHandler {
		@Override
		public void write(int address, int value) {}
	}
	
	public class MEMWriteBanked implements WriteHandler {
		int startArea;
		int bank_address;

		public MEMWriteBanked(int startArea) {
			this.startArea = startArea;
		}

		public void setBankAdr(int adr) {
			this.bank_address = adr - this.startArea;
		}
		@Override
		public void write(int address, int value) {
			mem[address + this.bank_address] = value;
		}
	}
	
}