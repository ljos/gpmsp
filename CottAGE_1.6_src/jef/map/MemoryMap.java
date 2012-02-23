/*
 * Created on Sep 13, 2005
 * by edy
 */
package jef.map;


/**
 * @author edy
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class MemoryMap implements ReadMap, WriteMap {
    
    int[] rom;
    ReadRegion[] readRegions = new ReadRegion[30];
    WriteRegion[] writeRegions = new WriteRegion[30];
    
    int nrOfReadRegions;
    int nrOfWriteRegions;
    
    int banks = 4;
    
    
    
    
    /**
     * @param rom The rom memory, read from ROM files
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
		public void write(int address, int value) {}
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

    /* (non-Javadoc)
     * @see jef.map.ReadMap#setBankAddress(int, int)
     */
    @Override
	public void setBankAddress(int b, int address) {
        // TODO Auto-generated method stub
        
    }









    /* (non-Javadoc)
     * @see jef.map.ReadMap#getSize()
     */
    @Override
	public int getSize() {
        // TODO Auto-generated method stub
        return 0;
    }


    /* (non-Javadoc)
     * @see jef.map.ReadMap#getMemory()
     */
    @Override
	public int[] getMemory() {
        return rom;
    }



    /* (non-Javadoc)
     * @see jef.map.ReadHandler#read(int)
     */
    @Override
	public int read(int address) {
        for (int i = 0; i < nrOfReadRegions; i++) {
            ReadRegion r = readRegions[i];
            //System.out.println(Integer.toHexString(r.from) + "-" + Integer.toHexString(r.until));
            if (address >= r.from && address <= r.until) {
                return r.handler.read(address-r.from);
            }
        }
        //System.out.println("Unknown read at " + Integer.toHexString(address));
        return 0;
    }


    /* (non-Javadoc)
     * @see jef.map.WriteHandler#write(int, int)
     */
    @Override
	public void write(int address, int data) {
        for (int i = 0; i < nrOfWriteRegions; i++) {
            WriteRegion w = writeRegions[i];
            if (address >= w.from && address <= w.until) {
                w.handler.write(address-w.from, data);
                return; 
            }
        }        
    }


    /**
     * Only use for NON-RAM/ROM handlers
     * @see jef.map.ReadMap#set(int, int, jef.map.ReadHandler)
     */
    @Override
	public void set(int from, int until, ReadHandler handler) {
        readRegions[nrOfReadRegions++] = new ReadRegion(from, until, handler);
    }

    /**
     * Only use for NON_RAM/ROM handlers
     * @see jef.map.WriteMap#set(int, int, jef.map.WriteHandler)
     */
    @Override
	public void set(int from, int until, WriteHandler handler) {
        writeRegions[nrOfWriteRegions++] = new WriteRegion(from, until, handler);
    }

    /* (non-Javadoc)
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
                readRegions[nrOfReadRegions-1].memory = memory;
                break;
            case MRA_ROM:
                rh = new MEMRead(rom);
                set(from, until, rh);
                readRegions[nrOfReadRegions-1].memory = rom;
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
                readRegions[nrOfReadRegions-1].memory = memory;
                break;
            default:
                throw new RuntimeException("MemoryMap: Unknown Reader Type " + readerType);
                
        }
        
    }



    /* (non-Javadoc)
     * @see jef.map.WriteMap#setMW(int, int, int)
     */
    @Override
	public void setMW(int from, int until, int type) {
        WriteHandler wh;
        int[] mem;
        switch (type) {
            case MWA_RAM:
                mem = findMemory(from, until);
                wh = new MEMWrite(mem);
                set(from, until, wh);
                break;
            case MWA_ROM:
            case MWA_NOP:
                set(from, until, new ROMwrite());
                break;
            case MWA_BANK1:
            case MWA_BANK2:
            case MWA_BANK3:
            case MWA_BANK4:
            case MWA_BANK5:
            case MWA_BANK6:
            case MWA_BANK7:
            case MWA_BANK8:
                mem = findMemory(from, until);
                wh = new MEMWriteBanked(mem);
                set(from, until, wh);
                break;

            default:
                throw new RuntimeException("MemoryMap: Unknown Writer Type " + type);
                
        }
    }
    
    public int[] findMemory(int from, int until) {
        for (int i = 0; i < nrOfReadRegions; i++) {
            ReadRegion rr = readRegions[i];
            if (rr.from == from && rr.until == until) {
                if (rr.memory == null) {
                    throw new RuntimeException("Memory for write region 0x" + Integer.toHexString(from) + "-0x" + Integer.toHexString(until) + " is null");
                }
                return rr.memory;
            }
        }
        throw new RuntimeException("Could not find memory for write region 0x" + Integer.toHexString(from) + "-0x" + Integer.toHexString(until));
    }

}