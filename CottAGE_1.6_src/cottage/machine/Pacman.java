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
	public WriteHandler hiscore(int[] mem, WriteHandler wh) {				return new HiScore(mem,wh); }
	
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
			System.out.println("Decrypting roms...");
			m.theglob_decrypt_rom_8(mem);
			m.theglob_decrypt_rom_9(mem);
			m.theglob_decrypt_rom_A(mem);
			m.theglob_decrypt_rom_B(mem);
		}
	}



	public void theglob_decrypt_rom_8(int[] RAM)
	{
		int oldbyte,inverted_oldbyte,newbyte;
		int mem;

		for (mem=0;mem<0x4000;mem++)
		{
			oldbyte = RAM[mem];
			inverted_oldbyte = ~oldbyte;

			/*	Note: D2 is inverted and connected to D1, D5 is inverted and
				connected to D0.  The other six data bits are converted by a
				PAL10H8 driven by the counter. */
			newbyte = 0;

			/* Direct inversion */
			newbyte  = (inverted_oldbyte & 0x04) >> 1;
			newbyte |= (inverted_oldbyte & 0x20) >> 5;
			/* PAL */
			newbyte |= (oldbyte & 0x01) << 5;
			newbyte |= (oldbyte & 0x02) << 1;
			newbyte |= (inverted_oldbyte & 0x08) << 4;
			newbyte |= (inverted_oldbyte & 0x10) >> 1;
			newbyte |= (inverted_oldbyte & 0x40) >> 2;
			newbyte |= (inverted_oldbyte & 0x80) >> 1;

			RAM[mem + 0x10000] = newbyte;
		}
	}


	public void theglob_decrypt_rom_9(int[] RAM)
	{
		int oldbyte,inverted_oldbyte,newbyte;
		int mem;

		for (mem=0;mem<0x4000;mem++)
		{
			oldbyte = RAM[mem];
			inverted_oldbyte = ~oldbyte;

			/*	Note: D2 is inverted and connected to D1, D5 is inverted and
				connected to D0.  The other six data bits are converted by a
				PAL10H8 driven by the counter. */
			newbyte = 0;

			/* Direct inversion */
			newbyte  = (inverted_oldbyte & 0x04) >> 1;
			newbyte |= (inverted_oldbyte & 0x20) >> 5;
			/* PAL */
			newbyte |= (oldbyte & 0x01) << 5;
			newbyte |= (inverted_oldbyte & 0x02) << 6;
			newbyte |= (oldbyte & 0x08) << 1;
			newbyte |= (inverted_oldbyte & 0x10) >> 1;
			newbyte |= (inverted_oldbyte & 0x40) >> 4;
			newbyte |= (inverted_oldbyte & 0x80) >> 1;

			RAM[mem + 0x14000] = newbyte;
		}
	}

	public void theglob_decrypt_rom_A(int[] RAM)
	{
		int oldbyte,inverted_oldbyte,newbyte;
		int mem;

		for (mem=0;mem<0x4000;mem++)
		{
			oldbyte = RAM[mem];
			inverted_oldbyte = ~oldbyte;

			/*	Note: D2 is inverted and connected to D1, D5 is inverted and
				connected to D0.  The other six data bits are converted by a
				PAL10H8 driven by the counter. */
			newbyte = 0;

			/* Direct inversion */
			newbyte  = (inverted_oldbyte & 0x04) >> 1;
			newbyte |= (inverted_oldbyte & 0x20) >> 5;
			/* PAL */
			newbyte |= (inverted_oldbyte & 0x01) << 6;
			newbyte |= (oldbyte & 0x02) << 1;
			newbyte |= (inverted_oldbyte & 0x08) << 4;
			newbyte |= (inverted_oldbyte & 0x10) << 1;
			newbyte |= (inverted_oldbyte & 0x40) >> 2;
			newbyte |= (oldbyte & 0x80) >> 4;

			RAM[mem + 0x18000] = newbyte;
		}
	}

	public void theglob_decrypt_rom_B(int[] RAM)
	{
		int oldbyte,inverted_oldbyte,newbyte;
		int mem;

		for (mem=0;mem<0x4000;mem++)
		{
			oldbyte = RAM[mem];
			inverted_oldbyte = ~oldbyte;

			/*	Note: D2 is inverted and connected to D1, D5 is inverted and
				connected to D0.  The other six data bits are converted by a
				PAL10H8 driven by the counter. */
			newbyte = 0;

			/* Direct inversion */
			newbyte  = (inverted_oldbyte & 0x04) >> 1;
			newbyte |= (inverted_oldbyte & 0x20) >> 5;
			/* PAL */
			newbyte |= (inverted_oldbyte & 0x01) << 6;
			newbyte |= (inverted_oldbyte & 0x02) << 6;
			newbyte |= (oldbyte & 0x08) << 1;
			newbyte |= (inverted_oldbyte & 0x10) << 1;
			newbyte |= (inverted_oldbyte & 0x40) >> 4;
			newbyte |= (oldbyte & 0x80) >> 4;

			RAM[mem + 0x1C000] = newbyte;
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
	
	/**
	 * This ReadHandler should be put in a location in the memory map
	 * as a 'breakpoint' at game-over.
	 * It takes the score and submits it to the high score server.  
	 * 
	 * Created on Apr 14, 2003
	 * 
	 * @author Erik Duijs
	 */
	public class HiScore implements WriteHandler {
		
		/** Offset in memory of the lsb of the score */
//		private static final int OFFSET_SCORE = 0x43f7;
		
		/** Offset in memory of the lsb of the high score */
		private static final int OFFSET_HIGH_SCORE = 0x43ed;
		
		long counter = 0;
		
		private int[] mem;
		WriteHandler vupdate;
		/**
		 * Constructor
		 * 
		 * @param hsClient
		 * @param mem
		 */
		public HiScore(int[] mem, WriteHandler wh) {
			this.mem = mem;
			this.vupdate = wh;
		}
		
		/**
		 * @see jef.map.WriteHandler#write(int, int)
		 */
		@Override
		public void write(int address, int value) {
			if (mem[address] != value) {
				mem[address] = value;
				if (counter > 250) { // a delay preventing uploading bogus score
					setHighScore(getScore(OFFSET_HIGH_SCORE));
				} else {
					counter++;
				}
				vupdate.write(address, value);
			}
		}
		
		/**
		 * Calculate the score from the emulated memory contents.
		 * 
		 * @return long The score
		 */
		private long getScore(int offset) {
			final int ZERO_CHAR = 0x00;
			final int BLANK_CHAR = 0x40;
			
			long score = 0;
			
			// calculate the score
			for (int i = 0; i < 7; i++) {
				int c = mem[offset + i];
				if (c == 0x00 || c == BLANK_CHAR) c = ZERO_CHAR;
				c -= ZERO_CHAR;
				score += (c * Math.pow(10, i));
			}
			
			if (score > 9999999) score = 0;
			
			return score;
		}
	}
}