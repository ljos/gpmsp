package cottage.machine;

import jef.cpuboard.CpuBoard;
import jef.cpuboard.FastCpuBoard;
import jef.machine.BasicMachine;
import jef.machine.Machine;
import jef.map.InterruptHandler;
import jef.map.WriteHandler;

public class Bombjack extends BasicMachine implements Machine {

	public boolean irqEnabled = false;

	public CpuBoard createCpuBoard(int id) {
		return new FastCpuBoard();
	}

	public class NMI_interrupt implements InterruptHandler {
		public int irq() {
			if (irqEnabled)
				return jef.cpu.Cpu.INTERRUPT_TYPE_NMI;	// nmi
			else
				return jef.cpu.Cpu.INTERRUPT_TYPE_IGNORE;	// ignore interrupt
		}
	}

	public InterruptHandler nmi_interrupt() { return new NMI_interrupt(); }

	public class Interrupt_enable_w implements WriteHandler {
		public void write(int address, int value) {
			irqEnabled = (value != 0);
		}
	}

	public WriteHandler interrupt_enable_w() { return new Interrupt_enable_w(); }
}