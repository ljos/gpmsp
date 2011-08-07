package cottage.machine;

import jef.machine.BasicMachine;
import jef.machine.Machine;
import jef.map.InterruptHandler;
import jef.map.WriteHandler;
import jef.video.BitMap;

public class Yiear extends BasicMachine implements Machine {

	public static boolean irqEnabled = false;
	public static boolean nmiEnabled = false;

	public InterruptHandler irq_interrupt()  { return new IRQ_interrupt(); }
	public InterruptHandler yiear_nmi_interrupt()  { return new Yiear_nmi_interrupt(); }
	public WriteHandler interrupt_enable_w() { return new Interrupt_enable_w(); }
	public WriteHandler nmi_enable_w() { return new NMI_enable_w(); }

	public BitMap refresh(boolean render) {

        if (render) 
            backBuffer = getDisplay();

        for (int slice = 0; slice < 500; slice++) {	// each frame is divided in slices

			currentSlice = slice;

			cd[0].cpu.exec(cd[0].frq / 500 / 60);
			
			//if (nmiEnabled) cb[0].interrupt(jef.cpu.Cpu.INTERRUPT_TYPE_NMI,true);
		}

		// UPDATE SOUND
		se.update();

		// UPDATE INPUT (for Impulse Events)
		updateInput();
		
		if (irqEnabled) cb[0].interrupt(0,true);
		
		//highScoreHandler.update();
        
        return backBuffer;
	}

	public class IRQ_interrupt implements InterruptHandler {

		public int irq() {
			//if (irqEnabled) {
				return jef.cpu.Cpu.INTERRUPT_TYPE_IRQ;	// irq
			//} else {
			//	return jef.cpu.Cpu.INTERRUPT_TYPE_IGNORE;	// ignore interrupt
			//}
		}
	}

	public class Interrupt_enable_w implements WriteHandler {
		public void write(int address, int value) {
			irqEnabled = (value != 0);
		}
	}
	public class NMI_enable_w implements WriteHandler {
		public void write(int address, int value) {
			nmiEnabled = (value != 0);
		}
	}

	public class Yiear_nmi_interrupt implements InterruptHandler {

		public int irq() {
			//if (irqEnabled) {
			return nmiEnabled ? jef.cpu.Cpu.INTERRUPT_TYPE_NMI : jef.cpu.Cpu.INTERRUPT_TYPE_IGNORE;
			//} else {
			//	return jef.cpu.Cpu.INTERRUPT_TYPE_IGNORE;	// ignore interrupt
			//}
		}
	}
	

}