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
public class MemoryReadAddressMap implements ReadMap {
    
    int[] mem;
    Map<Integer, ReadHandler> map = new HashMap<Integer, ReadHandler>(0x100);
    
    MEMreadBanked    BANKS[] = new MEMreadBanked[8];
    ReadHandler memRead = new MEMRead();
    ReadHandler[] handlers = { memRead, memRead, memRead };

    /**
     * @param mem
     */
    public MemoryReadAddressMap(int[] mem) {
        this.mem = mem;
    }
    
    
    class MEMRead implements ReadHandler {

        @Override
		public int read(int address) {
            return mem[address];
        }
    }

    
    class MEMreadBanked implements ReadHandler {
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
     * @see jef.map.ReadMap#setBankAddress(int, int)
     */
    @Override
	public void setBankAddress(int b, int address) {
        BANKS[b-1].setBankAdr(address); // the 1st bank is 1, not 0
    }

    /* (non-Javadoc)
     * @see jef.map.ReadMap#set(int, int, jef.map.ReadHandler)
     */
    @Override
	public void set(int from, int until, ReadHandler memRead) {
        for (int i = from; i <= until; i++) {
            //Value key = new Value(i);
            Integer key = new Integer(i);
            map.put(key, memRead);
        }
        
    }

    /* (non-Javadoc)
     * @see jef.map.ReadMap#set(int, int, int)
     */
    @Override
	public void setMR(int from, int until, int readerType) {
        for (int i = from; i <= until; i++) {
            //map.put(key, handlers[readerType]);
        }
        
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
     * @see jef.map.ReadHandler#read(int)
     */
    @Override
	public int read(int address) {
        ReadHandler rh = map.get(new Integer(address));
        if (rh == null) {
        	return memRead.read(address);
        } else {
            //System.out.println(rh);
            return rh.read(address);
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