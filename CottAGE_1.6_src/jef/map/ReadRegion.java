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
public class ReadRegion {
    
    public int from, until;
    public ReadHandler handler;
    
    // null if not a memory region
    public int[] memory;

    /**
     * @param from
     * @param until
     * @param handler
     */
    public ReadRegion(int from, int until, ReadHandler handler) {
        super();
        this.from = from;
        this.until = until;
        this.handler = handler;
    }
    
}
