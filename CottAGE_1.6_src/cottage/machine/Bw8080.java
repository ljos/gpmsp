package cottage.machine;

import jef.machine.BasicMachine;
import jef.machine.Machine;
import jef.map.InterruptHandler;
import jef.map.ReadHandler;
import jef.map.WriteHandler;
import jef.sound.SamplePlayer;

public class Bw8080 extends BasicMachine implements Machine {

	static SamplePlayer samples = new SamplePlayer();

	static int shift_amount = 0;
	static int shift_data1 = 0;
	static int shift_data2 = 0;

	static int count = 0;

	static {
		try {
			samples.loadSound("/0.wav");
			samples.loadSound("/1.wav");
			samples.loadSound("/2.wav");
			samples.loadSound("/3.wav");
			samples.loadSound("/4.wav");
			samples.loadSound("/5.wav");
			samples.loadSound("/6.wav");
			samples.loadSound("/7.wav");
			samples.loadSound("/8.wav");
			samples.loadSound("/9.wav");
		} catch (Exception e) {
		}
	}

	//public CpuBoard createCpuBoard(int id) {
	//	return new BasicCpuBoard();
	//}

	public static int SHIFT() {
		return (((((shift_data1 << 8) | shift_data2) << (shift_amount & 0x07)) >> 8) & 0xff);
	}

	public int bw_interrupt() {
		count++;

		if ((count & 1) == 1)
			return jef.cpu.Cpu.INTERRUPT_TYPE_IRQ; /* IRQ */
		else
			return jef.cpu.Cpu.INTERRUPT_TYPE_NMI; /* NMI */
	}

	public class Invaders_interrupt implements InterruptHandler {
		cottage.machine.Bw8080 b;

		public Invaders_interrupt(cottage.machine.Bw8080 b) {
			this.b = b;
		}

		public int irq() {
			return b.bw_interrupt();
		}
	}

	public class Invaders_shift_amount_w implements WriteHandler {
		cottage.machine.Bw8080 b;
		public Invaders_shift_amount_w(cottage.machine.Bw8080 b) {
			this.b = b;
		}

		public void write(int port, int data) {
			b.shift_amount = data;
		}
	}

	public class Invaders_sh_port3_w implements WriteHandler {
		cottage.machine.Bw8080 b;
		int Sound = 0;
		public Invaders_sh_port3_w(cottage.machine.Bw8080 b) {
			this.b = b;
		}

		public void write(int port, int data) {
			try {

				if ((data & 0x01) != 0 && (~Sound & 0x01) != 0)
					samples.playSound("/0.wav");

				//if ((~data & 0x01) != 0 && (Sound & 0x01) != 0)
				//	sample_stop (0);

				if ((data & 0x02) != 0 && (~Sound & 0x02) != 0)
					samples.playSound("/1.wav");

				if ((data & 0x04) != 0 && (~Sound & 0x04) != 0)
					samples.playSound("/2.wav");

				//if ((~data & 0x04) != 0 && (Sound & 0x04) != 0)
				//	sample_stop (2);

				if ((data & 0x08) != 0 && ~(Sound & 0x08) != 0)
					samples.playSound("/3.wav");

				if ((data & 0x10) != 0 && ~(Sound & 0x10) != 0)
					samples.playSound("/9.wav");

				//invaders_screen_red_w(data & 0x04);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				System.setErr(System.out);
				e.printStackTrace();
			}

			Sound = data;
		}
	}

	public class Invaders_shift_data_w implements WriteHandler {
		cottage.machine.Bw8080 b;
		public Invaders_shift_data_w(cottage.machine.Bw8080 b) {
			this.b = b;
		}

		public void write(int port, int data) {
			b.shift_data2 = b.shift_data1;
			b.shift_data1 = data;
		}
	}

	public class Invaders_sh_port5_w implements WriteHandler {
		cottage.machine.Bw8080 b;
		int Sound = 0;
		public Invaders_sh_port5_w(cottage.machine.Bw8080 b) {
			this.b = b;
		}

		public void write(int port, int data) {

			try {
				if ((data & 0x01) != 0 && (~Sound & 0x01) != 0)
					samples.playSound("/4.wav"); /* Fleet 1 */

				if ((data & 0x02) != 0 && (~Sound & 0x02) != 0)
					samples.playSound("/5.wav"); /* Fleet 2 */

				if ((data & 0x04) != 0 && (~Sound & 0x04) != 0)
					samples.playSound("/6.wav"); /* Fleet 3 */

				if ((data & 0x08) != 0 && (~Sound & 0x08) != 0)
					samples.playSound("/7.wav"); /* Fleet 4 */

				if ((data & 0x10) != 0 && (~Sound & 0x10) != 0)
					samples.playSound("/8.wav"); /* Saucer Hit */
			} catch (Exception e) {
				// TODO Auto-generated catch block
				System.setErr(System.out);
				e.printStackTrace();
			}

			//invaders_flipscreen_w(data & 0x20);

			Sound = data;
		}
	}

	public class Invaders_shift_data_r implements ReadHandler {
		cottage.machine.Bw8080 b;
		public Invaders_shift_data_r(cottage.machine.Bw8080 b) {
			this.b = b;
		}

		public int read(int port) {
			return b.SHIFT();
		}
	}

	public class Boothill_shift_data_r implements ReadHandler {
		cottage.machine.Bw8080 b;
		public Boothill_shift_data_r(cottage.machine.Bw8080 b) {
			this.b = b;
		}

		public int read(int port) {
			if (shift_amount < 0x10)
				return b.SHIFT();
			else {
				int ret = SHIFT();

				ret =
					((ret & 0x01) << 7)
						| ((ret & 0x02) << 5)
						| ((ret & 0x04) << 3)
						| ((ret & 0x08) << 1)
						| ((ret & 0x10) >> 1)
						| ((ret & 0x20) >> 3)
						| ((ret & 0x40) >> 5)
						| ((ret & 0x80) >> 7);

				return ret;
			}
		}
	}

	public class Invaders_shift_data_rev_r implements ReadHandler {
		cottage.machine.Bw8080 b;

		public Invaders_shift_data_rev_r(cottage.machine.Bw8080 b) {
			this.b = b;
		}

		public int read(int port) {
			int ret = SHIFT();

			ret =
				((ret & 0x01) << 7)
					| ((ret & 0x02) << 5)
					| ((ret & 0x04) << 3)
					| ((ret & 0x08) << 1)
					| ((ret & 0x10) >> 1)
					| ((ret & 0x20) >> 3)
					| ((ret & 0x40) >> 5)
					| ((ret & 0x80) >> 7);

			return ret;
		}
	}

	/////////////////////////////////////////////////////////////////////
	//																   //
	// Functions to interface the inner classes with the outside world //
	//																   //
	/////////////////////////////////////////////////////////////////////

	public WriteHandler c8080bw_shift_amount_w(cottage.machine.Bw8080 b) {
		return new Invaders_shift_amount_w(b);
	}

	public WriteHandler c8080bw_shift_data_w(cottage.machine.Bw8080 b) {
		return new Invaders_shift_data_w(b);
	}

	public WriteHandler invaders_sh_port3_w(cottage.machine.Bw8080 b) {
		return new Invaders_sh_port3_w(b);
	}

	public WriteHandler invaders_sh_port5_w(cottage.machine.Bw8080 b) {
		return new Invaders_sh_port5_w(b);
	}

	public ReadHandler c8080bw_shift_data_r(cottage.machine.Bw8080 b) {
		return new Invaders_shift_data_r(b);
	}

	public ReadHandler invaders_shift_data_rev_r(cottage.machine.Bw8080 b) {
		return new Invaders_shift_data_rev_r(b);
	}

	public ReadHandler boothill_shift_data_r(cottage.machine.Bw8080 b) {
		return new Boothill_shift_data_r(b);
	}

	public InterruptHandler invaders_interrupt(cottage.machine.Bw8080 b) {
		return new Invaders_interrupt(b);
	}

}