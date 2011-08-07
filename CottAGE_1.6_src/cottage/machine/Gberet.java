package cottage.machine;

import jef.machine.BasicMachine;
import jef.machine.Machine;
import jef.map.InterruptHandler;
import jef.map.WriteHandler;

public class Gberet extends BasicMachine implements Machine {

	public static boolean interruptenable = false;

	public InterruptHandler gberet_interrupt()  { return new Gberet_interrupt(); }
	public WriteHandler gberet_e044_w() { return new Gberet_e044_w(); }

	class Gberet_e044_w implements WriteHandler
	{
		public void write(int address, int data) {
			/* bit 0 enables interrupts */
			interruptenable = (data & 1) != 0;

			/* bit 3 flips screen */
			//if (flipscreen != (data & 0x08))
			//{
			//	flipscreen = data & 0x08;
			//	memset(dirtybuffer,1,videoram_size);
			//}

			/* don't know about the other bits */
		}
	}



	class Gberet_interrupt implements InterruptHandler
	{
		public int irq() {
			if (getCurrentSlice() == 0) return jef.cpu.Cpu.INTERRUPT_TYPE_IRQ;
			else if ((getCurrentSlice() % 2) != 0) {
				if (interruptenable) return jef.cpu.Cpu.INTERRUPT_TYPE_NMI;
			}

			return jef.cpu.Cpu.INTERRUPT_TYPE_IGNORE;
		}
	}


}