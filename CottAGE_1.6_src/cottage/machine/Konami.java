package cottage.machine;

import jef.cpuboard.BasicCpuBoard;
import jef.cpuboard.BasicDecryptingCpuBoard;
import jef.cpuboard.CpuBoard;
import jef.machine.BasicMachine;
import jef.machine.Machine;

public class Konami extends BasicMachine implements Machine {

	int [] rom;

	public CpuBoard createCpuBoard(int id) {
		if (id == 0) return new BasicDecryptingCpuBoard();
		else return new BasicCpuBoard();
	}

	int decodebyte( int opcode, int address )
	{
	/*
	>
	> CPU_D7 = (EPROM_D7 & ~ADDRESS_1) | (~EPROM_D7 & ADDRESS_1)  >
	> CPU_D6 = EPROM_D6
	>
	> CPU_D5 = (EPROM_D5 & ADDRESS_1) | (~EPROM_D5 & ~ADDRESS_1) >
	> CPU_D4 = EPROM_D4
	>
	> CPU_D3 = (EPROM_D3 & ~ADDRESS_3) | (~EPROM_D3 & ADDRESS_3) >
	> CPU_D2 = EPROM_D2
	>
	> CPU_D1 = (EPROM_D1 & ADDRESS_3) | (~EPROM_D1 & ~ADDRESS_3) >
	> CPU_D0 = EPROM_D0
	>
	*/
		int xormask;


		xormask = 0;
		if ((address & 0x02)!=0) xormask |= 0x80;
		else xormask |= 0x20;
		if ((address & 0x08)!=0) xormask |= 0x08;
		else xormask |= 0x02;

		return opcode ^ xormask;
	}



	void decode(int cpu)
	{
		int diff = rom.length / 2;
		int A;


		//memory_set_opcode_base(cpu,rom+diff);

		for (A = 0;A < diff;A++)
		{
			rom[A+diff] = decodebyte(rom[A],A);
		}
	}

	public void konami1_decode(int[] rom)
	{
		this.rom = rom;
		decode(0);
	}

	public void konami1_decode_cpu2(int[] rom)
	{
		this.rom = rom;
		decode(1);
	}
}