package cottage.machine;

import jef.machine.BasicMachine;
import jef.machine.Machine;
import jef.map.InterruptHandler;
import jef.map.ReadHandler;
import jef.map.WriteHandler;
import jef.video.BitMap;

public class Galaga extends BasicMachine implements Machine {

	static boolean interrupt_enable_1 = false;
	static boolean interrupt_enable_2 = false;
	static boolean interrupt_enable_3 = false;

	static int customio_command = 0;
	static int mode = 0;
	static int credits = 0;
	static int coinpercred = 0;
	static int credpercoin = 0;
	static int[] customio = new int[0x10];
	static int coininserted = 0;
	static boolean nmi_timer = false;
	static boolean halt23 = false;

	public int[] galaga_sharedram;
	cottage.vidhrdw.Galaga video;
	cottage.drivers.Galaga driver;


	public ReadHandler		galaga_sharedram_r() 	 	  { return new Galaga_sharedram_r(); }
	public ReadHandler  	galaga_dsw_r() 			 	  { return new Galaga_dsw_r(); }
	public ReadHandler  	galaga_customio_data_r() 	  { return new Galaga_customio_data_r(); }
	public ReadHandler  	galaga_customio_r() 	 	  { return new Galaga_customio_r(); }
	public WriteHandler 	galaga_customio_data_w() 	  { return new Galaga_customio_data_w(); }
	public WriteHandler 	galaga_sharedram_w()	 	  { return new Galaga_sharedram_w(); }
	public WriteHandler 	galaga_customio_w() 	 	  { return new Galaga_customio_w(); }
	public WriteHandler 	galaga_halt_w() 	 	 	  { return new Galaga_halt_w(); }
	public WriteHandler 	galaga_interrupt_enable_1_w() { return new Galaga_interrupt_enable_1_w(); }
	public WriteHandler 	galaga_interrupt_enable_2_w() { return new Galaga_interrupt_enable_2_w(); }
	public WriteHandler 	galaga_interrupt_enable_3_w() { return new Galaga_interrupt_enable_3_w(); }
	public WriteHandler 	hiscore(WriteHandler wh) {	return new HiScore(wh); }
	public InterruptHandler galaga_interrupt_1() 		  { return new Galaga_interrupt_1(); }
	public InterruptHandler galaga_interrupt_2() 		  { return new Galaga_interrupt_2(); }
	public InterruptHandler galaga_interrupt_3() 		  { return new Galaga_interrupt_3(); }

	public void setRefs(int sharedram[], cottage.drivers.Galaga driver, cottage.vidhrdw.Galaga video) {
		this.galaga_sharedram = sharedram;
		this.driver = driver;
		this.video = video;
	}

	public BitMap refresh(boolean render) {
        
        if (render) 
            backBuffer = getDisplay();
        
        for (int s = 0; s < 99; s++) {

			cd[0].cpu.exec(526);

			if (!halt23) {
				cd[1].cpu.exec(526);
				cd[2].cpu.exec(526);
			}

			if (s == 50 || s == 0) {
				if (nmi_timer) {
					cd[0].cpu.interrupt(jef.cpu.Cpu.INTERRUPT_TYPE_NMI, true);
				}
				if (interrupt_enable_3 && s == 50) {
					cd[2].cpu.interrupt(jef.cpu.Cpu.INTERRUPT_TYPE_NMI, true);
				}
			}
		}
		if (interrupt_enable_1) {
			cd[0].cpu.interrupt(jef.cpu.Cpu.INTERRUPT_TYPE_IRQ, true);
		}
		if (interrupt_enable_2) {
			cd[1].cpu.interrupt(jef.cpu.Cpu.INTERRUPT_TYPE_IRQ, true);
		}
		if (interrupt_enable_3) {
			cd[2].cpu.interrupt(jef.cpu.Cpu.INTERRUPT_TYPE_NMI, true);
		}
		updateInput();

		// UPDATE SOUND
		se.update();
		video.galaga_vh_interrupt();	/* update the background stars position */
		//highScoreHandler.update();
        
        return backBuffer;
	}

	public class Galaga_sharedram_r implements ReadHandler {
		public int read(int address) {
			return galaga_sharedram[address];
		}
	}

	public class Galaga_sharedram_w implements WriteHandler {
		public void write(int address, int data) {
			if (address < 0x8800) {		/* write to video RAM */
				video.dirtybuffer[address & 0x3ff] = true;
			}
			galaga_sharedram[address] = data;
		}
	}

	public class Galaga_dsw_r implements ReadHandler {
		public int read(int offset) {
			offset -= 0x6800;

			int bit0,bit1;

			bit0 = (driver.input_port_0_r.read(0) >> offset) & 1;
			bit1 = (driver.input_port_1_r.read(0) >> offset) & 1;

			int r = bit0 | (bit1 << 1);

			//System.out.println("Galaga_dsw_r: " + offset + " - " + r);

			return r;
		}
	}

 	public final int readinputport(int port) {
		int retval;
		switch (port) {
			case 0: return 255;
			case 1: return 255;
			case 2:
				retval = driver.input_port_2_r.read(0);
				md.input[2].write(md.input[2].read(0) | 0x10);	// fake impulse event -> REMOVE WHEN ADDED TO InputPort
				return retval;
			case 3:
				retval = driver.input_port_2_r.read(0);
				md.input[2].write(md.input[2].read(0) | 0x10);	// fake impulse event -> REMOVE WHEN ADDED TO InputPort
				return retval;
			case 4:
				retval = driver.input_port_4_r.read(0);	// fake impulse event -> REMOVE WHEN ADDED TO InputPort
				md.input[4].write(255);
				return retval;
			default: return 255;
		}
	}
/***************************************************************************

 Emulate the custom IO chip.

***************************************************************************/

	public class Galaga_customio_data_w implements WriteHandler {
		public void write(int offset, int data) {
			offset -= 0x7000;
			customio[offset] = data;

			//System.out.println("Galaga_customio_data_w: " + offset + " - " + data);

			switch (customio_command) {
				case 0xa8:
					//if (offset == 3 && data == 0x20)	/* total hack */
						//sample_start(0,0,0);
					break;

				case 0xe1:
					if (offset == 7) {
						coinpercred = customio[1];
						credpercoin = customio[2];
					}
					break;
			}
		}
	}

	public class Galaga_customio_data_r implements ReadHandler {
		public int read(int offset) {

			offset -= 0x7000;

			//System.out.print("Galaga_customio_data_r " + Integer.toHexString(offset));

			switch (customio_command)
			{
				case 0x71:	/* read input */
				case 0xb1:	/* only issued after 0xe1 (go into credit mode) */
					if (offset == 0)
					{
						if (mode != 0)	/* switch mode */
						{
							/* bit 7 is the service switch */
							//System.out.println(" - "+ readinputport(4));
							return readinputport(4);
						}
						else	/* credits mode: return number of credits in BCD format */
						{
							int in;

							in = readinputport(4);

							/* check if the user inserted a coin */
							if (coinpercred > 0)
							{
								if ((in & 0x70) != 0x70 && credits < 99)
								{
									md.input[4].write(md.input[4].read(0) | 0x70);	// fake impulse event -> REMOVE WHEN ADDED TO InputPort
									coininserted++;
									if (coininserted >= coinpercred)
									{
										credits += credpercoin;
										coininserted = 0;
									}
								}
							}
							else credits = 100;	/* free play */


							/* check for 1 player start button */
							if ((in & 0x04) == 0)
								if (credits >= 1) credits--;

							/* check for 2 players start button */
							if ((in & 0x08) == 0)
								if (credits >= 2) credits -= 2;

							//System.out.println(" - "+ ((credits / 10) * 16 + credits % 10));
							return (credits / 10) * 16 + credits % 10;
						}
					}
					else if (offset == 1) {
						//System.out.println(" - "+ readinputport(2));
						return readinputport(2);	/* player 1 input */
					}
					else if (offset == 2) {
						//System.out.println(" - "+ readinputport(3));
						return readinputport(3);	/* player 2 input */
					}

					break;
			}

			return 255;
		}
	}

	public class Galaga_customio_r implements ReadHandler {
		public int read(int address) {
			//System.out.println("customio_command: " + customio_command);
			return customio_command;
		}
	}

	public class Galaga_customio_w implements WriteHandler {
		public void write(int address, int data) {
			//if (data != 0x10 && data != 0x71)
			//	logerror("%04x: custom IO command %02x\n",activecpu_get_pc(),data);

			customio_command = data;

			switch (data)
			{
				case 0x10:
					//timer_adjust(nmi_timer, TIME_NEVER, 0, 0);
					nmi_timer = false;
					//System.out.println("nmi_timer: " + nmi_timer);
					return;

				case 0xa1:	/* go into switch mode */
					mode = 1;
					//System.out.println("mode = 1");
					break;

				case 0xe1:	/* go into credit mode */
					credits = 0;	/* this is a good time to reset the credits counter */
					mode = 0;
					//System.out.println("mode = 0, credits = 0");
					break;
			}
			nmi_timer = true;

			//System.out.println("nmi_timer: " + nmi_timer);
			//timer_adjust(nmi_timer, TIME_IN_USEC(50), 0, TIME_IN_USEC(50));

		}
	}

	public class Galaga_halt_w implements WriteHandler {
		public void write(int address, int data) {
			//System.out.println("Galaga_halt_w : " + data);
			if ( (data & 1) != 0 )
			{
				halt23 = false;
				driver.cpu2.state_HALT = false;
				driver.cpu3.state_HALT = false;
			}
			else if (data == 0)
			{
				halt23 = true;
				driver.cpu2.reset();
				driver.cpu3.reset();
				driver.cpu2.state_HALT = true;
				driver.cpu3.state_HALT = true;
			}
		}
	}

	public class Galaga_interrupt_1 implements InterruptHandler {
		public int irq() {
			video.galaga_vh_interrupt();	/* update the background stars position */

			if (interrupt_enable_1)
				return jef.cpu.Cpu.INTERRUPT_TYPE_IRQ; // IRQ;

			return jef.cpu.Cpu.INTERRUPT_TYPE_IGNORE;
		}
	}

	public class Galaga_interrupt_enable_1_w implements WriteHandler {
		public void write(int address, int data) {
			interrupt_enable_1 = (data & 1) != 0;
			//System.out.println("interrupt_enable_1: " + interrupt_enable_1);
		}
	}

	public class Galaga_interrupt_2 implements InterruptHandler {
		public int irq() {
			if (interrupt_enable_2)
				return jef.cpu.Cpu.INTERRUPT_TYPE_IRQ; // IRQ;

			return jef.cpu.Cpu.INTERRUPT_TYPE_IGNORE;
		}
	}

	public class Galaga_interrupt_enable_2_w implements WriteHandler {
		public void write(int address, int data) {
			interrupt_enable_2 = (data & 1) != 0;
		}
	}


	public class Galaga_interrupt_3 implements InterruptHandler {
		public int irq() {
			if (interrupt_enable_3)
				return jef.cpu.Cpu.INTERRUPT_TYPE_NMI; // NMI;

			return jef.cpu.Cpu.INTERRUPT_TYPE_IGNORE;
		}
	}

	public class Galaga_interrupt_enable_3_w implements WriteHandler {
		public void write(int address, int data) {
			interrupt_enable_3 = (data & 1) == 0;
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
		private static final int OFFSET_SCORE = 0x43f7;
		
		/** Offset in memory of the lsb of the high score */
		private static final int OFFSET_HIGH_SCORE = 0x83ED;
		
		long counter = 0;
		
		WriteHandler vupdate;
		/**
		 * Constructor
		 * 
		 * @param hsClient
		 * @param mem
		 */
		public HiScore(WriteHandler wh) {
			this.vupdate = wh;
		}
		
		/**
		 * @see jef.map.WriteHandler#write(int, int)
		 */
		public void write(int address, int value) {
			if (galaga_sharedram[address] != value) {
				galaga_sharedram[address] = value;
				if (counter >= 446) { // a delay preventing uploading bogus score
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
			final int BLANK_CHAR = 0x24;
			
			long score = 0;
			
			// calculate the score
			for (int i = 0; i < 7; i++) {
				int c = galaga_sharedram[offset + i];
				if (c == 0x00 || c == BLANK_CHAR) c = ZERO_CHAR;
				c -= ZERO_CHAR;
				score += (c * Math.pow(10, i));
			}

			if (score == 20000 || score > 9999999) score = 0;
			
			return score;
		}
	}
	
	
}