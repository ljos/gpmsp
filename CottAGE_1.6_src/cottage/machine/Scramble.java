package cottage.machine;

import jef.cpu.Z80;
import jef.cpuboard.CpuBoard;
import jef.cpuboard.FastCpuBoard;
import jef.machine.BasicMachine;
import jef.machine.Machine;
import jef.map.ReadHandler;

public class Scramble extends BasicMachine implements Machine {

	public CpuBoard createCpuBoard(int id) {
		return new FastCpuBoard();
	}

	public ReadHandler scramblb_protection_1_r(Z80 c) { return new Scramblb_protection_1_r(c); }
	public ReadHandler scramblb_protection_2_r(Z80 c) { return new Scramblb_protection_2_r(c); }

	class Scramblb_protection_1_r implements ReadHandler {
		Z80 cpu;
		public Scramblb_protection_1_r(Z80 cpu) {
			this.cpu = cpu;
		}
		public int read(int offset) {
			switch (cpu.PC)
			{
			case 0x01da: return 0x80;
			case 0x01e4: return 0x00;
			default:
				System.out.println(Integer.toHexString(cpu.PC)+": read protection 1\n");
				return 0;
			}
		}
	}

	class Scramblb_protection_2_r implements ReadHandler {
		Z80 cpu;
		public Scramblb_protection_2_r(Z80 cpu) {
			this.cpu = cpu;
		}
		public int read(int offset) {
			switch (cpu.PC)
			{
			case 0x01ca: return 0x90;
			default:
				System.out.println(Integer.toHexString(cpu.PC)+": read protection 2\n");
				return 0;
			}
		}
	}

}