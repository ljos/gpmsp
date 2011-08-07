package cottage.machine;

import jef.cpuboard.CpuBoard;
import jef.cpuboard.FastCpuBoard;
import jef.machine.BasicMachine;
import jef.machine.Machine;
import jef.map.InterruptHandler;
import jef.map.WriteHandler;

public class Gyruss extends BasicMachine implements Machine {

	public static boolean irqEnabled = false;

	public InterruptHandler nmi_interrupt(cottage.machine.Gyruss m)	{	return new NMI_interrupt(m); }
	public WriteHandler interrupt_enable_w(cottage.machine.Gyruss m) {	return new Interrupt_enable_w(m); }

	public CpuBoard createCpuBoard(int id) {
		return new FastCpuBoard();
	}

	public class NMI_interrupt implements InterruptHandler {
		cottage.machine.Gyruss m;

		public NMI_interrupt(cottage.machine.Gyruss m) {
			this.m = m;
		}

		public int irq() {
			if (m.irqEnabled) {
				return jef.cpu.Cpu.INTERRUPT_TYPE_NMI;	// nmi
			} else {
				return jef.cpu.Cpu.INTERRUPT_TYPE_IGNORE;	// ignore interrupt
			}
		}
	}

	public class Interrupt_enable_w implements WriteHandler {
		cottage.machine.Gyruss m;

		public Interrupt_enable_w(cottage.machine.Gyruss m) {
			this.m = m;
		}

		public void write(int address, int value) {
			m.irqEnabled = (value != 0);
		}
	}
}