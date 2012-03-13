package cottage.machine;

import jef.cpuboard.BasicCpuBoard;
import jef.cpuboard.CpuBoard;
import jef.cpuboard.FastCpuBoard;
import jef.machine.BasicMachine;
import jef.machine.Machine;
import jef.map.InterruptHandler;
import jef.map.ReadHandler;
import jef.map.VoidFunction;
import jef.map.WriteHandler;

public class Pacman extends BasicMachine implements Machine {

	public static boolean irqEnabled = false;
	public int readOffset = 0x18000;
	public static boolean fastBoard = true;

	public InterruptHandler pacman_nmi_interrupt(cottage.machine.Pacman m)	{	return new Pacman_NMI_interrupt(m); }
	public InterruptHandler pacman_interrupt(cottage.machine.Pacman m)	{	return new Pacman_interrupt(m); }
	public WriteHandler interrupt_enable_w(cottage.machine.Pacman m) {		return new Interrupt_enable_w(m); }
	public WriteHandler interrupt_vector_w(cottage.machine.Pacman m) {		return new Interrupt_vector_w(m); }
	public ReadHandler theglob_decrypt_rom(cottage.machine.Pacman m) {		return new Theglob_decrypt_rom(m); }
	public ReadHandler	MRA_BANK1(cottage.machine.Pacman m, int[] mem)	{	return new mra_bank1(m,mem); }
	public VoidFunction theglob_init_machine(cottage.machine.Pacman m, int[] mem) { return new init(m,mem); }
	
	@Override
	public CpuBoard createCpuBoard(int id) {
		if (fastBoard)
			return new FastCpuBoard();
		else
			return new BasicCpuBoard();
	}

	public class init implements VoidFunction {
		cottage.machine.Pacman m;
		int[] mem;

		public init(cottage.machine.Pacman m, int[] mem) {
			this.m = m;
			this.mem = mem;
		}

		@Override
		public void exec() {
		}
	}

	public class mra_bank1 implements ReadHandler {
		cottage.machine.Pacman m;
		int[] mem;

		public mra_bank1(cottage.machine.Pacman m, int[] mem) {
			this.m = m;
			this.mem = mem;
		}

		@Override
		public int read(int address) {
			return mem[m.readOffset + address];
		}
	}

	public class Theglob_decrypt_rom implements ReadHandler {
		cottage.machine.Pacman m;
		int counter = 0xa;

		public Theglob_decrypt_rom(cottage.machine.Pacman m) {
			this.m = m;
		}

		@Override
		public int read(int offset) {

			if ( (offset & 0x01) != 0) {
				counter = counter - 1;
				if (counter < 0)
					counter = 0x0F;
			} else {
				counter = (counter + 1) & 0x0F;
			}

			switch(counter) {
				case 0x08:	m.readOffset = 0x10000;	break;
				case 0x09:	m.readOffset = 0x14000;	break;
				case 0x0A:	m.readOffset = 0x18000;	break;
				case 0x0B:	m.readOffset = 0x1C000;	break;
			}
			return 0;
		}
	}


	public class Pacman_NMI_interrupt implements InterruptHandler {
		cottage.machine.Pacman m;

		public Pacman_NMI_interrupt(cottage.machine.Pacman m) {
			this.m = m;
		}

		@Override
		public int irq() {
			if (Pacman.irqEnabled) {
				return jef.cpu.Cpu.INTERRUPT_TYPE_NMI;	// nmi
			} else {
				return jef.cpu.Cpu.INTERRUPT_TYPE_IGNORE;	// ignore interrupt
			}
		}
	}

	public class Pacman_interrupt implements InterruptHandler {
		cottage.machine.Pacman m;

		public Pacman_interrupt(cottage.machine.Pacman m) {
			this.m = m;
		}

		@Override
		public int irq() {
			if (Pacman.irqEnabled) {
				return jef.cpu.Cpu.INTERRUPT_TYPE_IRQ;	// irq
			} else {
				return jef.cpu.Cpu.INTERRUPT_TYPE_IGNORE;	// ignore interrupt
			}
		}
	}

	public class Interrupt_enable_w implements WriteHandler {
		cottage.machine.Pacman m;

		public Interrupt_enable_w(cottage.machine.Pacman m) {
			this.m = m;
		}

		@Override
		public void write(int address, int value) {
			Pacman.irqEnabled = (value != 0);
		}
	}

	public class Interrupt_vector_w implements WriteHandler {
		cottage.machine.Pacman m;

		public Interrupt_vector_w(cottage.machine.Pacman m) {
			this.m = m;
		}

		@Override
		public void write(int address, int value) {
			m.cd[0].cpu.setProperty(0,value);
		}
	}
}