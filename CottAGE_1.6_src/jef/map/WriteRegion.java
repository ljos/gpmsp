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
public class WriteRegion {

	public int from, until;
	public WriteHandler handler;

	/**
	 * @param from
	 * @param until
	 * @param handler
	 */
	public WriteRegion(int from, int until, WriteHandler handler) {
		super();
		this.from = from;
		this.until = until;
		this.handler = handler;
	}
}
