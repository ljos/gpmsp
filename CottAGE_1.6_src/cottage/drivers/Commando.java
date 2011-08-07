/*
 * Created on 24-jun-2005
 * 
 * To change the template for this generated file go to Window - Preferences -
 * Java - Code Generation - Code and Comments
 */
package cottage.drivers;

import java.net.URL;

import jef.cpu.Cpu;
import jef.cpu.Z80;
import jef.cpuboard.CpuDriver;
import jef.machine.DecryptingMachine;
import jef.machine.Machine;
import jef.machine.MachineDriver;
import jef.map.InputPort;
import jef.map.InterruptHandler;
import jef.map.MemoryReadAddress;
import jef.map.MemoryWriteAddress;
import jef.map.ReadHandler;
import jef.map.WriteHandler;
import jef.sound.SoundChipEmulator;
import jef.sound.chip.YM2203;
import jef.video.Eof_callback;
import jef.video.GfxDecodeInfo;
import jef.video.GfxLayout;
import jef.video.Vh_convert_color_proms;
import jef.video.Vh_refresh;
import jef.video.Vh_start;
import jef.video.Vh_stop;
import cottage.mame.MAMEDriver;

/**
 * @author Erik Duijs
 * 
 * To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Generation - Code and Comments
 */
public class Commando extends MAMEDriver {

	Z80 cpu1 = new Z80();
	Z80 cpu2 = new Z80();

	int[] REGION_CPU1 = new int[0x20000];
	int[] REGION_CPU2 = new int[0x20000];
	int[] REGION_GFX1 = new int[0x4000];
	int[] REGION_GFX2 = new int[0x18000];
	int[] REGION_GFX3 = new int[0x18000];
	int[] REGION_PROMS = new int[0x600];

	YM2203 ym = new YM2203(2, 1500000, null, null);

	DecryptingMachine m = new DecryptingMachine();
	InterruptHandler commando_interrupt = new CommandoInterrupt();

	cottage.vidhrdw.Commando v = new cottage.vidhrdw.Commando();
	WriteHandler videoram_w = v.videoram_w(REGION_CPU1, v);
	WriteHandler colorram_w = videoram_w;
	WriteHandler commando_bgvideoram_w = v.videoram_w(REGION_CPU1, v);
	WriteHandler commando_bgcolorram_w = commando_bgvideoram_w;
	Eof_callback noCallback = (Eof_callback) v;
	Vh_refresh commando_vh_screenrefresh = (Vh_refresh) v;
	Vh_start commando_vh_start = (Vh_start) v;
	Vh_stop commando_vh_stop = (Vh_stop) v;
	Vh_convert_color_proms commando_vh_convert_color_prom = (Vh_convert_color_proms) v;

	InputPort[] in = new InputPort[5];

	ReadHandler input_port_0_r;
	ReadHandler input_port_1_r;
	ReadHandler input_port_2_r;
	ReadHandler input_port_3_r;
	ReadHandler input_port_4_r;
	ReadHandler input_port_5_r;

	public class CommandoInterrupt implements InterruptHandler {
		public int irq() {
			cpu1.setProperty(Cpu.PROPERTY_Z80_IRQ_VECTOR, 0x10);
			return Cpu.INTERRUPT_TYPE_IRQ;
		}

	}

	private MemoryReadAddress readmem() {
		MemoryReadAddress mra = new MemoryReadAddress(REGION_CPU1);
		mra.setMR(0x0000, 0xbfff, MRA_ROM);
		mra.set(0xc000, 0xc000, input_port_0_r);
		mra.set(0xc001, 0xc001, input_port_1_r);
		mra.set(0xc002, 0xc002, input_port_2_r);
		mra.set(0xc003, 0xc003, input_port_3_r);
		mra.set(0xc004, 0xc004, input_port_4_r);
		mra.setMR(0xd000, 0xffff, MRA_RAM);
		return mra;
	}

	private MemoryWriteAddress writemem() {
		MemoryWriteAddress mwa = new MemoryWriteAddress(REGION_CPU1);
		mwa.setMW(0x0000, 0xbfff, MWA_ROM);
		mwa.set(0xc800, 0xc800, soundlatch_w);
		mwa.setMW(0xc804, 0xc804, MWA_RAM); //commando_c804_w
		mwa.setMW(0xc808, 0xc80b, MWA_RAM); // scroll Y,X
		mwa.set(0xd000, 0xd3ff, videoram_w);
		mwa.set(0xd400, 0xd7ff, colorram_w);
		mwa.set(0xd800, 0xdbff, commando_bgvideoram_w);
		mwa.set(0xdc00, 0xdfff, commando_bgcolorram_w);
		mwa.setMW(0xe000, 0xfdff, MWA_RAM);
		mwa.setMW(0xfe00, 0xffff, MWA_RAM); // sprites

		return mwa;
	}

	private MemoryReadAddress sound_readmem() {
		MemoryReadAddress mra = new MemoryReadAddress(REGION_CPU2);
		mra.setMR(0x0000, 0x3fff, MRA_ROM);
		mra.setMR(0x4000, 0x47ff, MRA_RAM);
		mra.set(0x6000, 0x6000, soundlatch_r);
		return mra;
	};

	private MemoryWriteAddress sound_writemem() {
		MemoryWriteAddress mwa = new MemoryWriteAddress(REGION_CPU2);
		mwa.setMW(0x0000, 0x3fff, MWA_ROM);
		mwa.setMW(0x4000, 0x47ff, MWA_RAM);
		mwa.set(0x8000, 0x8000, ym.ym2203_control_port_0_w());
		mwa.set(0x8001, 0x8001, ym.ym2203_write_port_0_w());
		mwa.set(0x8002, 0x8002, ym.ym2203_control_port_1_w());
		mwa.set(0x8003, 0x8003, ym.ym2203_write_port_1_w());
		return mwa;
	}

	private InputPort[] ipt_commando() {
		/* IN0 */
		in[0].setBit(0x01, IP_ACTIVE_LOW, IPT_START1);
		in[0].setBit(0x02, IP_ACTIVE_LOW, IPT_START2);
		in[0].setBit(0x04, IP_ACTIVE_LOW, IPT_UNKNOWN); /* probably unused */
		in[0].setBit(0x08, IP_ACTIVE_LOW, IPT_UNKNOWN); /* probably unused */
		in[0].setBit(0x10, IP_ACTIVE_LOW, IPT_UNKNOWN); /* probably unused */
		in[0].setBit(0x20, IP_ACTIVE_LOW, IPT_UNKNOWN);
		in[0].setBit(0x40, IP_ACTIVE_LOW, IPT_COIN1);
		in[0].setBit(0x80, IP_ACTIVE_LOW, IPT_COIN2);

		/* IN1 */
		in[1].setBit(0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY);
		in[1].setBit(0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT | IPF_8WAY);
		in[1].setBit(0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN | IPF_8WAY);
		in[1].setBit(0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_UP | IPF_8WAY);
		in[1].setBit(0x10, IP_ACTIVE_LOW, IPT_BUTTON1);
		in[1].setBit(0x20, IP_ACTIVE_LOW, IPT_BUTTON2);
		in[1].setBit(0x40, IP_ACTIVE_LOW, IPT_UNKNOWN); /* probably unused */
		in[1].setBit(0x80, IP_ACTIVE_LOW, IPT_UNKNOWN); /* probably unused */

		/* IN2 */
		in[2].setBit(0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_COCKTAIL);
		in[2].setBit(0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT | IPF_8WAY | IPF_COCKTAIL);
		in[2].setBit(0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN | IPF_8WAY | IPF_COCKTAIL);
		in[2].setBit(0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_UP | IPF_8WAY | IPF_COCKTAIL);
		in[2].setBit(0x10, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_COCKTAIL);
		in[2].setBit(0x20, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_COCKTAIL);
		in[2].setBit(0x40, IP_ACTIVE_LOW, IPT_UNKNOWN); /* probably unused */
		in[2].setBit(0x80, IP_ACTIVE_LOW, IPT_UNKNOWN); /* probably unused */

		/* DSW0 */
		in[3].setDipName(0x03, 0x03, "Starting Stage");
		in[3].setDipSetting(0x00, "1");
		in[3].setDipSetting(0x01, "3");
		in[3].setDipSetting(0x02, "5");
		in[3].setDipSetting(0x07, "7");
		in[3].setDipName(0x0c, 0x0c, DEF_STR[Lives]);
		in[3].setDipSetting(0x04, "2");
		in[3].setDipSetting(0x0c, "3");
		in[3].setDipSetting(0x08, "4");
		in[3].setDipSetting(0x00, "5");
		in[3].setDipName(0x30, 0x30, DEF_STR[Coin_A]);
		in[3].setDipSetting(0x00, DEF_STR[_4C_1C]);
		in[3].setDipSetting(0x20, DEF_STR[_3C_1C]);
		in[3].setDipSetting(0x10, DEF_STR[_2C_1C]);
		in[3].setDipSetting(0x30, DEF_STR[_1C_1C]);
		in[3].setDipName(0xc0, 0xc0, DEF_STR[Coin_B]);
		in[3].setDipSetting(0x00, DEF_STR[_2C_1C]);
		in[3].setDipSetting(0xc0, DEF_STR[_1C_1C]);
		in[3].setDipSetting(0x40, DEF_STR[_1C_2C]);
		in[3].setDipSetting(0x80, DEF_STR[_1C_3C]);

		/* DSW1 */
		in[4].setDipName(0x07, 0x07, DEF_STR[Bonus_Life]);
		in[4].setDipSetting(0x04, "40000 50000");
		in[4].setDipSetting(0x07, "10000 500000");
		in[4].setDipSetting(0x03, "10000 600000");
		in[4].setDipSetting(0x05, "20000 600000");
		in[4].setDipSetting(0x01, "20000 700000");
		in[4].setDipSetting(0x06, "30000 700000");
		in[4].setDipSetting(0x02, "30000 800000");
		in[4].setDipSetting(0x00, "None");
		in[4].setDipName(0x08, 0x08, DEF_STR[Demo_Sounds]);
		in[4].setDipSetting(0x00, DEF_STR[Off]);
		in[4].setDipSetting(0x08, DEF_STR[On]);
		in[4].setDipName(0x10, 0x10, DEF_STR[Difficulty]);
		in[4].setDipSetting(0x10, "Normal");
		in[4].setDipSetting(0x00, "Difficult");
		in[4].setDipName(0x20, 0x00, DEF_STR[Flip_Screen]);
		in[4].setDipSetting(0x00, DEF_STR[Off]);
		in[4].setDipSetting(0x20, DEF_STR[On]);
		in[4].setDipName(0xc0, 0x00, DEF_STR[Cabinet]);
		in[4].setDipSetting(0x00, "Upright One Player");
		in[4].setDipSetting(0x40, "Upright Two Players");
		in[4].setDipSetting(0xc0, DEF_STR[Cocktail]);

		return in;
	}

	public Machine getMachine(URL url, String name) {
		super.getMachine(url, name);
		super.setVideoEmulator(v);

		in[0] = new InputPort();
		in[1] = new InputPort();
		in[2] = new InputPort();
		in[3] = new InputPort();
		in[4] = new InputPort();

		input_port_0_r = (ReadHandler) in[0];
		input_port_1_r = (ReadHandler) in[1];
		input_port_2_r = (ReadHandler) in[2];
		input_port_3_r = (ReadHandler) in[3];
		input_port_4_r = (ReadHandler) in[4];

		v.setRegions(REGION_CPU1, REGION_PROMS);

		if (name.equals("commando")) {
			this.md = machine_driver_commando();
			GAME(
				1987,
				rom_commando(),
				ipt_commando(),
				v.commando(),
				ROT270,
				"Capcom",
				"Commando (World)");
			decode();
		}

		m.init(md);
		return (Machine) m;
	}

	/**
	 *  
	 */
	private void decode() {
		System.out.print("Decrypting ROMs...");
		int A;
		int diff = 0x10000;

		REGION_CPU1[0x10000] = REGION_CPU1[0]; /*
											    * the first opcode is not
											    * encrypted
											    */
		for (A = 1; A < 0xc000; A++) {
			int src;

			src = REGION_CPU1[A];
			REGION_CPU1[A + diff] = src ^ (src & 0xee) ^ ((src & 0xe0) >> 4) ^ ((src & 0x0e) << 4);
			//System.out.println(src + "->" + REGION_CPU1[A+diff]);
		}

		for (int i = 0; i < 0x4000; i++) {
			REGION_CPU2[i + diff] = REGION_CPU2[i];
		}

		System.out.println("OK!");

	}

	/**
	 * @return
	 */
	private boolean rom_commando() {
		romLoader.setZip("commando");

		romLoader.setMemory(REGION_CPU1);
		romLoader.loadROM("m09_cm04.bin", 0x0000, 0x8000, 0x8438b694);
		romLoader.loadROM("m08_cm03.bin", 0x8000, 0x4000, 0x35486542);

		romLoader.setMemory(REGION_CPU2);
		romLoader.loadROM("f09_cm02.bin", 0x0000, 0x4000, 0xf9cc4a74);

		romLoader.setMemory(REGION_GFX1);
		romLoader.loadROM("d05_vt01.bin", 0x00000, 0x4000, 0x505726e0); /* characters */

		romLoader.setMemory(REGION_GFX2);
		romLoader.loadROM("a05_vt11.bin", 0x00000, 0x4000, 0x7b2e1b48); /* tiles */
		romLoader.loadROM("a06_vt12.bin", 0x04000, 0x4000, 0x81b417d3);
		romLoader.loadROM("a07_vt13.bin", 0x08000, 0x4000, 0x5612dbd2);
		romLoader.loadROM("a08_vt14.bin", 0x0c000, 0x4000, 0x2b2dee36);
		romLoader.loadROM("a09_vt15.bin", 0x10000, 0x4000, 0xde70babf);
		romLoader.loadROM("a10_vt16.bin", 0x14000, 0x4000, 0x14178237);

		romLoader.setMemory(REGION_GFX3);
		romLoader.loadROM("e07_vt05.bin", 0x00000, 0x4000, 0x79f16e3d); /* sprites */
		romLoader.loadROM("e08_vt06.bin", 0x04000, 0x4000, 0x26fee521);
		romLoader.loadROM("e09_vt07.bin", 0x08000, 0x4000, 0xca88bdfd);
		romLoader.loadROM("h07_vt08.bin", 0x0c000, 0x4000, 0x2019c883);
		romLoader.loadROM("h08_vt09.bin", 0x10000, 0x4000, 0x98703982);
		romLoader.loadROM("h09_vt10.bin", 0x14000, 0x4000, 0xf069d2f8);

		romLoader.setMemory(REGION_PROMS); /* PROMs */
		romLoader.loadROM("01d_vtb1.bin", 0x0000, 0x0100, 0x3aba15a1); /* red */
		romLoader.loadROM("02d_vtb2.bin", 0x0100, 0x0100, 0x88865754); /* green */
		romLoader.loadROM("03d_vtb3.bin", 0x0200, 0x0100, 0x4c14c3f6); /* blue */
		romLoader.loadROM("01h_vtb4.bin", 0x0300, 0x0100, 0xb388c246);
		/* palette selector (not used) */
		romLoader.loadROM("06l_vtb5.bin", 0x0400, 0x0100, 0x712ac508);
		/* interrupt timing (not used) */
		romLoader.loadROM("06e_vtb6.bin", 0x0500, 0x0100, 0x0eaf5158);
		/* video timing (not used) */

		romLoader.loadZip(base_URL);

		return true;
	}

	/**
	 * @return
	 */
	private MachineDriver machine_driver_commando() {
		CpuDriver[] cpuDriver = new CpuDriver[2];
		SoundChipEmulator[] soundChip = new SoundChipEmulator[1];

		cpuDriver[0] = new CpuDriver((Cpu) cpu1, 4000000, /* 4 Mhz */
		readmem(), writemem(), null, null, commando_interrupt, 1);
		cpuDriver[1] = new CpuDriver((Cpu) cpu2, 3000000, /* 4 Mhz */
		sound_readmem(), sound_writemem(), null, null, m.interrupt(), 4);
		cpuDriver[1].isAudioCpu = true;

		soundChip[0] = (SoundChipEmulator) ym;

		int[] visibleArea = { 0 * 8, 32 * 8 - 1, 2 * 8, 30 * 8 - 1 };

		return new MachineDriver(cpuDriver, 60, 1000, 100, NOP,

		//video;
		32 * 8,
			32 * 8,
			visibleArea,
			gfxdecodeinfo(),
			256,
			16 * 4 + 4 * 16 + 16 * 8,
			commando_vh_convert_color_prom,
			VIDEO_TYPE_RASTER | VIDEO_UPDATE_AFTER_VBLANK,
			noCallback,
			commando_vh_start,
			commando_vh_stop,
			commando_vh_screenrefresh,
			soundChip);
	}

	private GfxLayout charlayout() {

		int[] pOffs = { 4, 0 };
		int[] xOffs = { 0, 1, 2, 3, 8 + 0, 8 + 1, 8 + 2, 8 + 3 };
		int[] yOffs = { 0 * 16, 1 * 16, 2 * 16, 3 * 16, 4 * 16, 5 * 16, 6 * 16, 7 * 16 };

		return new GfxLayout(8, 8, /* 8*8 characters */
		1024, /* 2048 characters */
		2, /* 2 bits per pixel */
		pOffs, xOffs, yOffs, 16 * 8 /* every char takes 16 consecutive bytes */
		);
	}

	private GfxLayout spritelayout() {

		//int[] pOffs = { 768*64*8+4, 768*64*8+0, 5, 0 };
		int[] pOffs = { 4, 0, 768 * 64 * 8 + 4, 768 * 64 * 8 + 0 };
		int[] xOffs =
			{
				0,
				1,
				2,
				3,
				8 + 0,
				8 + 1,
				8 + 2,
				8 + 3,
				32 * 8 + 0,
				32 * 8 + 1,
				32 * 8 + 2,
				32 * 8 + 3,
				33 * 8 + 0,
				33 * 8 + 1,
				33 * 8 + 2,
				33 * 8 + 3 };
		int[] yOffs =
			{
				0 * 16,
				1 * 16,
				2 * 16,
				3 * 16,
				4 * 16,
				5 * 16,
				6 * 16,
				7 * 16,
				8 * 16,
				9 * 16,
				10 * 16,
				11 * 16,
				12 * 16,
				13 * 16,
				14 * 16,
				15 * 16 };

		return new GfxLayout(16, 16, /* 16*8 sprites */
		768, /* 256 sprites */
		4, /* 4 bits per pixel */
		pOffs, xOffs, yOffs, 64 * 8 /* every char takes 64 consecutive bytes */
		);
	}

	private GfxLayout tilelayout() {

		//int[] pOffs = { 0, 1024*32*8, 2*1024*32*8 };
		int[] pOffs = { 2 * 1024 * 32 * 8, 1024 * 32 * 8, 0 };
		int[] xOffs =
			{
				0,
				1,
				2,
				3,
				4,
				5,
				6,
				7,
				16 * 8 + 0,
				16 * 8 + 1,
				16 * 8 + 2,
				16 * 8 + 3,
				16 * 8 + 4,
				16 * 8 + 5,
				16 * 8 + 6,
				16 * 8 + 7 };
		int[] yOffs =
			{
				0 * 8,
				1 * 8,
				2 * 8,
				3 * 8,
				4 * 8,
				5 * 8,
				6 * 8,
				7 * 8,
				8 * 8,
				9 * 8,
				10 * 8,
				11 * 8,
				12 * 8,
				13 * 8,
				14 * 8,
				15 * 8 };

		return new GfxLayout(16, 16, /* 16*8 sprites */
		1024, /* 256 sprites */
		3, /* 4 bits per pixel */
		pOffs, xOffs, yOffs, 32 * 8 /* every char takes 32 consecutive bytes */
		);
	}

	/**
	 * @return
	 */
	private GfxDecodeInfo[] gfxdecodeinfo() {
		GfxDecodeInfo gdi[] = new GfxDecodeInfo[3];
		gdi[0] = new GfxDecodeInfo(REGION_GFX1, 0, charlayout(), 192, 16); /*
																		    * colors
																		    * 192-255
																		    */
		gdi[1] = new GfxDecodeInfo(REGION_GFX2, 0, tilelayout(), 0, 16); /*
																		  * colors
																		  * 0-127
																		  */
		gdi[2] = new GfxDecodeInfo(REGION_GFX3, 0, spritelayout(), 128, 4); /*
																			 * colors
																			 * 128-191
																			 */
		return gdi;
	}

}
