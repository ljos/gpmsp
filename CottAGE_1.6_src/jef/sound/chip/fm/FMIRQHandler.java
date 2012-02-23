/*
 * Created on 14-aug-2005
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package jef.sound.chip.fm;

/* FM_IRQHHANDLER : IRQ level changing sense     */
/* int n       = chip number                     */
/* int irq     = IRQ level 0=OFF,1=ON            */
public interface FMIRQHandler {
	
	public void irq(int numChip, int irqState);
}
