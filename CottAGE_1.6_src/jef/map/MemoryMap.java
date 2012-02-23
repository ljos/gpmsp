/*
 * Created on Sep 13, 2005
 * by edy
 */
package jef.map;

/**
 * @author edy
 * 
 *         To change the template for this generated type comment go to Window -
 *         Preferences - Java - Code Generation - Code and Comments
 */
public class MemoryMap implements ReadMap, WriteMap {

	int[] rom;

	int nrOfReadRegions;
	int nrOfWriteRegions;

	int banks = 4;

	/**
	 * @param rom
	 *            The rom memory, read from ROM files
	 */
	public MemoryMap(int[] rom) {
		this.rom = rom;
		nrOfReadRegions = 0;
	}

	public class MEMRead implements ReadHandler {
		public int[] memory;

		public MEMRead(int[] memory) {
			this.memory = memory;
		}

		@Override
		public int read(int offset) {
			return memory[offset];
		}

	}

	public class MEMWrite implements WriteHandler {
		public int[] memory;

		public MEMWrite(int[] memory) {
			this.memory = memory;
		}

		@Override
		public void write(int offset, int data) {
			memory[offset] = data;
		}
	}

	public class ROMwrite implements WriteHandler {
		@Override
		public void write(int address, int value) {
		}
	}

	public class MEMWriteBanked implements WriteHandler {
		public int bank_address;
		public int[] memory;

		public MEMWriteBanked(int[] memory) {
			this.memory = memory;
		}

		public void setBankAdr(int adr) {
			this.bank_address = adr;
		}

		@Override
		public void write(int offset, int value) {
			memory[offset + this.bank_address] = value;
		}
	}

	public class MEMReadBanked implements ReadHandler {
		public int bank_address;
		public int[] memory;

		public MEMReadBanked(int[] memory) {
			this.memory = memory;
		}

		public void setBankAdr(int adr) {
			this.bank_address = adr;
		}

		@Override
		public int read(int offset) {
			return memory[offset + this.bank_address];
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jef.map.ReadMap#setBankAddress(int, int)
	 */
	@Override
	public void setBankAddress(int b, int address) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jef.map.ReadMap#getSize()
	 */
	@Override
	public int getSize() {
		// TODO Auto-generated method stub
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jef.map.ReadMap#getMemory()
	 */
	@Override
	public int[] getMemory() {
		return rom;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jef.map.ReadMap#set(int, int, int)
	 */
	@Override
	public void setMR(int from, int until, int readerType) {
		ReadHandler rh;
		int[] memory;
		switch (readerType) {
		case MRA_RAM:
		case MRA_NOP:
			memory = new int[1 + until - from];
			rh = new MEMRead(memory);
			set(from, until, rh);

			break;
		case MRA_ROM:
			rh = new MEMRead(rom);
			set(from, until, rh);

			break;
		case MRA_BANK1:
		case MRA_BANK2:
		case MRA_BANK3:
		case MRA_BANK4:
		case MRA_BANK5:
		case MRA_BANK6:
		case MRA_BANK7:
		case MRA_BANK8:
			memory = new int[(1 + until - from) * banks];
			rh = new MEMReadBanked(memory);
			set(from, until, rh);

			break;
		default:
			throw new RuntimeException("MemoryMap: Unknown Reader Type "
					+ readerType);

		}
	}
}