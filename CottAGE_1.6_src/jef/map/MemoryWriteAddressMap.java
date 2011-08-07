/*
 * Created on Aug 25, 2005
 * by edy
 */
package jef.map;

import java.util.HashMap;
import java.util.Map;




/**
 * @author edy
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class MemoryWriteAddressMap implements WriteMap {

    private int[] mem;
    private Map<Integer, WriteHandler> map = new HashMap<Integer, WriteHandler>();
    private WriteHandler    RAM = new RAMwrite();
    private WriteHandler    ROM = new ROMwrite();
    private MEMWriteBanked   BANKS[] = new MEMWriteBanked[8];
    
    /**
     * @param region_cpu1
     */
    public MemoryWriteAddressMap(int[] region_cpu1) {
        this.mem = region_cpu1;
    }
    /* (non-Javadoc)
     * @see jef.map.WriteMap#set(int, int, jef.map.WriteHandler)
     */
    public void set(int from, int until, WriteHandler memWrite) {
        for (int i = from; i <= until; i++) {
            Integer key = new Integer(i);
            map.put(key, memWrite);
        }
        
    }
    /* (non-Javadoc)
     * @see jef.map.WriteMap#set(int, int, int)
     */
    public void setMW(int from, int until, int type) {
        for (int i = from; i <= until; i++) {
            switch (type) {
                case MWA_ROM:
                case MWA_NOP:
                    map.put(new Integer(i), ROM);
                    break;
                case MWA_BANK1:
                case MWA_BANK2:
                case MWA_BANK3:
                case MWA_BANK4:
                case MWA_BANK5:
                case MWA_BANK6:
                case MWA_BANK7:
                case MWA_BANK8:
                    WriteHandler wh = BANKS[type - MWA_BANK1] = new MEMWriteBanked(from);
                    map.put(new Integer(i), wh);
                    break;
            }
        }
        
    }
    /* (non-Javadoc)
     * @see jef.map.WriteMap#getSize()
     */
    public int getSize() {
        // TODO Auto-generated method stub
        return 0;
    }
    /* (non-Javadoc)
     * @see jef.map.WriteHandler#write(int, int)
     */
    public void write(int address, int data) {
        WriteHandler wh = (WriteHandler) map.get(new Integer(address));
        if (wh == null) RAM.write(address, data);
        else {
            wh.write(address, data);
        }
        
    }
    
    public class RAMwrite implements WriteHandler {
        public void write(int address, int value) {
            mem[address] = value;
        }
    }

    public class ROMwrite implements WriteHandler {
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
        public void write(int address, int value) {
            mem[address + this.bank_address] = value;
        }
    }

}
