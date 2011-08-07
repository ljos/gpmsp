/*
 * CottAGE - the Arcade Generic Emulator in Java
 * 
 * Java driver by Erik Duijs, Gollum
 */

/*******************************************************************************
 * 
 * Black Tiger
 * 
 * Driver provided by Paul Leaman
 * 
 * Thanks to Ishmair for providing information about the screen layout on level 3.
 * 
 * Notes: - sprites/tile priority is a guess. I didn't find a PROM that would
 * simply translate to the scheme I implemented.
 *  
 ******************************************************************************/

package cottage.drivers;

import java.net.URL;

import jef.cpu.Cpu;
import jef.cpu.Z80;
import jef.cpuboard.CpuDriver;
import jef.machine.Machine;
import jef.machine.MachineDriver;
import jef.map.IOReadPort;
import jef.map.IOWritePort;
import jef.map.InputPort;
import jef.map.InterruptHandler;
import jef.map.MemoryReadAddress;
import jef.map.MemoryWriteAddress;
import jef.map.ReadHandler;
import jef.map.WriteHandler;
import jef.sound.SoundChipEmulator;
import jef.sound.chip.YM2203;
import jef.sound.chip.fm.FMIRQHandler;
import jef.video.Eof_callback;
import jef.video.GfxDecodeInfo;
import jef.video.GfxLayout;
import jef.video.GfxManager;
import jef.video.Vh_convert_color_proms;
import jef.video.Vh_refresh;
import jef.video.Vh_start;
import jef.video.Vh_stop;

import cottage.mame.MAMEDriver;

public class Blktiger extends MAMEDriver {

	Z80 cpu1 = new Z80();
	Z80 cpu2 = new Z80();
	int[] REGION_CPU1 = new int[0x50000];
	int[] REGION_CPU2 = new int[0x10000];
	int[] REGION_GFX1 = new int[0x08000];
	int[] REGION_GFX2 = new int[0x40000];
	int[] REGION_GFX3 = new int[0x40000];
	int[] REGION_PROMS = new int[0x400];

	int bankaddress = 0;

	InputPort[] in = new InputPort[6];

	ReadHandler input_port_0_r;
	ReadHandler input_port_1_r;
	ReadHandler input_port_2_r;
	ReadHandler input_port_3_r;
	ReadHandler input_port_4_r;
	ReadHandler input_port_5_r;

	cottage.vidhrdw.Blktiger v = new cottage.vidhrdw.Blktiger();
	WriteHandler videoram_w = v.videoram_w(REGION_CPU1, v);
	WriteHandler colorram_w = videoram_w;
	Eof_callback noCallback = (Eof_callback) v;
	Vh_refresh blktiger_vh_screenrefresh = (Vh_refresh) v;
	Vh_start blktiger_vh_start = (Vh_start) v;
	Vh_stop blktiger_vh_stop = (Vh_stop) v;
	Vh_convert_color_proms blktiger_vh_convert_color_prom = (Vh_convert_color_proms) v;

	WriteHandler paletteram_xxxxBBBBRRRRGGGG_split1_w = v.paletteram_w(REGION_CPU1, v);
	WriteHandler paletteram_xxxxBBBBRRRRGGGG_split2_w = v.paletteram2_w(REGION_CPU1, v);
	WriteHandler blktiger_scrollx_w = v.blktiger_scrollx_w(v);
	WriteHandler blktiger_scrolly_w = v.blktiger_scrolly_w(v);
	WriteHandler blktiger_scrollbank_w = v.blktiger_scrollbank_w(v);
	WriteHandler blktiger_screen_layout_w = v.blktiger_screen_layout_w(v);
	WriteHandler blktiger_background_w = v.blktiger_background_w(REGION_CPU1, v);
	ReadHandler blktiger_background_r = v.blktiger_background_r(REGION_CPU1, v);

	ReadHandler MRA_BANK1 = (ReadHandler) new Bankread();
	ReadHandler blktiger_protection_r = (ReadHandler) new Blktiger_protection_r();
	WriteHandler blktiger_bankswitch_w = (WriteHandler) new Blktiger_bankswitch_w();

	WriteHandler highScore;

	jef.machine.BasicMachine m = new jef.machine.BasicMachine();
	InterruptHandler interrupt = m.interrupt();
	FMIRQHandler sndIrq = new SndIrq();
	
	YM2203 ym = new YM2203(2, 3579545, null, sndIrq);
	
	public class SndIrq implements FMIRQHandler {
		
		int curState = 0;
		/* (non-Javadoc)
		 * @see jef.sound.chip.fm.FMIRQHandler#irq(int, int)
		 */
		public void irq(int numChip, int irqState) {
			//if (curState == 0 && irqState == 1) {
				//System.out.println("IRQ");
				cpu2.irq();
			//}

			curState = irqState;
			
		}

		
	}

	/* this is a protection check. The game crashes (thru a jump to 0x8000) */
	/* if a read from this address doesn't return the value it expects. */
	public class Blktiger_protection_r implements ReadHandler {
		public int read(int offset) {
			return cpu1.D;
		}
	}

	public class Blktiger_bankswitch_w implements WriteHandler {
		public void write(int offset, int data) {
			bankaddress = 0x10000 + (data & 0x0f) * 0x4000;
		}
	}

	public class Bankread implements ReadHandler {
		public int read(int offset) {
			return REGION_CPU1[bankaddress + (offset - 0x8000)];
		}
	}

	public MachineDriver machine_driver_blktiger() {
		CpuDriver[] cpuDriver = new CpuDriver[1];
		SoundChipEmulator[] soundChip = new SoundChipEmulator[1];
		
		cpuDriver[0] = new CpuDriver((Cpu) cpu1, 4000000, /* 4 Mhz */
		readmem(), writemem(), readport(), writeport(), interrupt, 1);
		//cpuDriver[1] = new CpuDriver((Cpu) cpu2, 3000000, /* 3 Mhz */
		//		sound_readmem(), sound_writemem(), readport(), writeport(), interrupt, 0);
		
		soundChip[0] = (SoundChipEmulator) ym;
		
		int[] visibleArea = { 0 * 8, 32 * 8 - 1, 2 * 8, 30 * 8 - 1 };

		return new MachineDriver(cpuDriver, 60, 1500, 1000, NOP,

		//video;
		32 * 8,
			32 * 8,
			visibleArea,
			gfxdecodeinfo(),
			1024,
			1024,
			blktiger_vh_convert_color_prom,
			VIDEO_TYPE_RASTER
				| GfxManager.VIDEO_MODIFIES_PALETTE
				| GfxManager.VIDEO_UPDATE_AFTER_VBLANK,
			noCallback,
			blktiger_vh_start,
			blktiger_vh_stop,
			blktiger_vh_screenrefresh,
			soundChip);
	}

	private MemoryReadAddress readmem() {
		MemoryReadAddress mra = new MemoryReadAddress(REGION_CPU1);
		mra.setMR(0x0000, 0x7fff, MRA_ROM);
		mra.set(0x8000, 0xbfff, MRA_BANK1);
		mra.set(0xc000, 0xcfff, blktiger_background_r);
		mra.setMR(0xd000, 0xffff, MRA_RAM);
		return mra;
	}

	private MemoryWriteAddress writemem() {
		MemoryWriteAddress mwa = new MemoryWriteAddress(REGION_CPU1);
		mwa.setMW(0x0000, 0xbfff, MWA_ROM);
		mwa.set(0xc000, 0xcfff, blktiger_background_w);
		mwa.set(0xd000, 0xd3ff, videoram_w);
		mwa.set(0xd400, 0xd7ff, colorram_w);
		mwa.set(0xd800, 0xdbff, paletteram_xxxxBBBBRRRRGGGG_split1_w);
		mwa.set(0xdc00, 0xdfff, paletteram_xxxxBBBBRRRRGGGG_split2_w);
		mwa.setMW(0xe000, 0xfdff, MWA_RAM);
		mwa.setMW(0xfe00, 0xffff, MWA_RAM);

		return mwa;
	}
	private MemoryReadAddress sound_readmem() {
		MemoryReadAddress mra = new MemoryReadAddress(REGION_CPU2);
		mra.setMR(0x0000, 0x7fff, MRA_ROM);
		mra.setMR(0xc000, 0xc7ff, MRA_RAM);
		mra.set(0xc800, 0xc800, soundlatch_r);
		mra.set(0xe000, 0xe000, ym.ym2203_status_port_0_r());
		mra.set(0xe001, 0xe001, ym.ym2203_read_port_0_r());
		mra.set(0xe002, 0xe002, ym.ym2203_status_port_1_r());
		mra.set(0xe003, 0xe003, ym.ym2203_read_port_1_r());
		return mra;
	}

	private MemoryWriteAddress sound_writemem() {
		MemoryWriteAddress mwa = new MemoryWriteAddress(REGION_CPU2);
		mwa.setMW(0x0000, 0x7fff, MWA_ROM);
		mwa.setMW(0xc000, 0xc7ff, MWA_RAM);
		mwa.set(0xe000, 0xe000, ym.ym2203_control_port_0_w());
		mwa.set(0xe001, 0xe001, ym.ym2203_write_port_0_w());
		mwa.set(0xe002, 0xe002, ym.ym2203_control_port_1_w());
		mwa.set(0xe003, 0xe003, ym.ym2203_write_port_1_w());
		return mwa;
	}

	private IOReadPort readport() {
		IOReadPort ior = new IOReadPort();
		ior.set(0x00, 0x00, input_port_0_r);
		ior.set(0x01, 0x01, input_port_1_r);
		ior.set(0x02, 0x02, input_port_2_r);
		ior.set(0x03, 0x03, input_port_3_r);
		ior.set(0x04, 0x04, input_port_4_r);
		ior.set(0x05, 0x05, input_port_5_r);
		ior.set(0x07, 0x07, blktiger_protection_r);
		return ior;
	}

	private IOWritePort writeport() {
		IOWritePort iow = new IOWritePort();
		iow.set( 0x00, 0x00, soundlatch_w );
		iow.set(0x01, 0x01, blktiger_bankswitch_w);
		//iow.set( 0x04, 0x04, blktiger_video_control_w );
		//iow.set( 0x06, 0x06, watchdog_reset_w );
		//iow.set( 0x07, 0x07, IOWP_NOP ); /* Software protection (7) */
		iow.set(0x08, 0x09, blktiger_scrollx_w);
		iow.set(0x0a, 0x0b, blktiger_scrolly_w);
		//iow.set( 0x0c, 0x0c, blktiger_video_enable_w );
		iow.set(0x0d, 0x0d, blktiger_scrollbank_w);
		iow.set(0x0e, 0x0e, blktiger_screen_layout_w);
		return iow;
	}

	private InputPort[] ipt_blktiger() {
		/* IN0 */
		in[0].setBit(0x01, IP_ACTIVE_LOW, IPT_START1);
		in[0].setBit(0x02, IP_ACTIVE_LOW, IPT_START2);
		in[0].setBit(0x04, IP_ACTIVE_LOW, IPT_UNKNOWN); /* probably unused */
		in[0].setBit(0x08, IP_ACTIVE_LOW, IPT_UNKNOWN); /* probably unused */
		in[0].setBit(0x10, IP_ACTIVE_LOW, IPT_UNKNOWN); /* probably unused */
		in[0].setBit(0x20, IP_ACTIVE_LOW, IPT_COIN3);
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
		in[3].setDipName(0x07, 0x07, DEF_STR[Coin_A]);
		in[3].setDipSetting(0x00, DEF_STR[_4C_1C]);
		in[3].setDipSetting(0x01, DEF_STR[_3C_1C]);
		in[3].setDipSetting(0x02, DEF_STR[_2C_1C]);
		in[3].setDipSetting(0x07, DEF_STR[_1C_1C]);
		in[3].setDipSetting(0x06, DEF_STR[_1C_2C]);
		in[3].setDipSetting(0x05, DEF_STR[_1C_3C]);
		in[3].setDipSetting(0x04, DEF_STR[_1C_4C]);
		in[3].setDipSetting(0x03, DEF_STR[_1C_5C]);
		in[3].setDipName(0x38, 0x38, DEF_STR[Coin_B]);
		in[3].setDipSetting(0x00, DEF_STR[_4C_1C]);
		in[3].setDipSetting(0x08, DEF_STR[_3C_1C]);
		in[3].setDipSetting(0x10, DEF_STR[_2C_1C]);
		in[3].setDipSetting(0x38, DEF_STR[_1C_1C]);
		in[3].setDipSetting(0x30, DEF_STR[_1C_2C]);
		in[3].setDipSetting(0x28, DEF_STR[_1C_3C]);
		in[3].setDipSetting(0x20, DEF_STR[_1C_4C]);
		in[3].setDipSetting(0x18, DEF_STR[_1C_5C]);
		in[3].setDipName(0x40, 0x40, DEF_STR[Flip_Screen]);
		in[3].setDipSetting(0x40, DEF_STR[Off]);
		in[3].setDipSetting(0x00, DEF_STR[On]);
		in[3].setService(0x80, IP_ACTIVE_LOW);

		/* DSW1 */
		in[4].setDipName(0x03, 0x03, DEF_STR[Lives]);
		in[4].setDipSetting(0x02, "2");
		in[4].setDipSetting(0x03, "3");
		in[4].setDipSetting(0x01, "5");
		in[4].setDipSetting(0x00, "7");
		in[4].setDipName(0x1c, 0x1c, DEF_STR[Difficulty]);
		in[4].setDipSetting(0x1c, "1 (Easiest);");
		in[4].setDipSetting(0x18, "2");
		in[4].setDipSetting(0x14, "3");
		in[4].setDipSetting(0x10, "4");
		in[4].setDipSetting(0x0c, "5 (Normal);");
		in[4].setDipSetting(0x08, "6");
		in[4].setDipSetting(0x04, "7");
		in[4].setDipSetting(0x00, "8 (Hardest);");
		in[4].setDipName(0x20, 0x20, DEF_STR[Demo_Sounds]);
		in[4].setDipSetting(0x00, DEF_STR[Off]);
		in[4].setDipSetting(0x20, DEF_STR[On]);
		in[4].setDipName(0x40, 0x40, "Allow Continue");
		in[4].setDipSetting(0x00, DEF_STR[No]);
		in[4].setDipSetting(0x40, DEF_STR[Yes]);
		in[4].setDipName(0x80, 0x00, DEF_STR[Cabinet]);
		in[4].setDipSetting(0x00, DEF_STR[Upright]);
		in[4].setDipSetting(0x80, DEF_STR[Cocktail]);

		in[5].setDipName(0x01, 0x01, "Freeze"); /* could be VBLANK */
		in[5].setDipSetting(0x01, DEF_STR[Off]);
		in[5].setDipSetting(0x00, DEF_STR[On]);
		return in;
	}

	private GfxLayout charlayout() {

		int[] pOffs = { 4, 0 };
		int[] xOffs = { 0, 1, 2, 3, 8 + 0, 8 + 1, 8 + 2, 8 + 3 };
		int[] yOffs = { 0 * 16, 1 * 16, 2 * 16, 3 * 16, 4 * 16, 5 * 16, 6 * 16, 7 * 16 };

		return new GfxLayout(8, 8, /* 8*8 characters */
		2048, /* 2048 characters */
		2, /* 2 bits per pixel */
		pOffs, xOffs, yOffs, 16 * 8 /* every char takes 16 consecutive bytes */
		);
	}

	private GfxLayout spritelayout() {

		int[] pOffs = { 4, 0, 0x20000 * 8 + 4, 0x20000 * 8 + 0 };
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
		2048, /* 256 sprites */
		4, /* 4 bits per pixel */
		pOffs, xOffs, yOffs, 32 * 16 /* every char takes 64 consecutive bytes */
		);
	}

	private GfxDecodeInfo[] gfxdecodeinfo() {
		GfxDecodeInfo gdi[] = new GfxDecodeInfo[3];
		gdi[0] = new GfxDecodeInfo(REGION_GFX1, 0, charlayout(), 0x300, 32);
		/* colors 0x300-0x37f */
		gdi[1] = new GfxDecodeInfo(REGION_GFX2, 0, spritelayout(), 0x000, 16);
		/* colors 0x000-0x0ff */
		gdi[2] = new GfxDecodeInfo(REGION_GFX3, 0, spritelayout(), 0x200, 8);
		/* colors 0x200-0x27f */
		return gdi;
	}

	private boolean rom_blktiger() {
		romLoader.setZip("blktiger");

		romLoader.setMemory(REGION_CPU1);
		romLoader.loadROM("blktiger.5e", 0x00000, 0x08000, 0xa8f98f22); /* CODE */
		romLoader.loadROM("blktiger.6e", 0x10000, 0x10000, 0x7bef96e8); /* 0+1 */
		romLoader.loadROM("blktiger.8e", 0x20000, 0x10000, 0x4089e157); /* 2+3 */
		romLoader.loadROM("blktiger.9e", 0x30000, 0x10000, 0xed6af6ec); /* 4+5 */
		romLoader.loadROM("blktiger.10e", 0x40000, 0x10000, 0xae59b72e); /* 6+7 */

		romLoader.setMemory(REGION_CPU2);
		romLoader.loadROM("blktiger.1l", 0x0000, 0x8000, 0x2cf54274);

		romLoader.setMemory(REGION_GFX1);
		romLoader.loadROM("blktiger.2n", 0x00000, 0x08000, 0x70175d78); /* characters */

		romLoader.setMemory(REGION_GFX2);
		romLoader.loadROM("blktiger.5b", 0x00000, 0x10000, 0xc4524993); /* tiles */
		romLoader.loadROM("blktiger.4b", 0x10000, 0x10000, 0x7932c86f);
		romLoader.loadROM("blktiger.9b", 0x20000, 0x10000, 0xdc49593a);
		romLoader.loadROM("blktiger.8b", 0x30000, 0x10000, 0x7ed7a122);

		romLoader.setMemory(REGION_GFX3);
		romLoader.loadROM("blktiger.5a", 0x00000, 0x10000, 0xe2f17438); /* sprites */
		romLoader.loadROM("blktiger.4a", 0x10000, 0x10000, 0x5fccbd27);
		romLoader.loadROM("blktiger.9a", 0x20000, 0x10000, 0xfc33ccc6);
		romLoader.loadROM("blktiger.8a", 0x30000, 0x10000, 0xf449de01);

		//romLoader.setMemory( REGION_PROMS); /* PROMs (function unknown) */
		//romLoader.loadROM( "mb7114e.8j", 0x0000, 0x0100, 0x29b459e5 );
		//romLoader.loadROM( "mb7114e.9j", 0x0100, 0x0100, 0x8b741e66 );
		//romLoader.loadROM( "mb7114e.11k", 0x0200, 0x0100, 0x27201c75 );
		//romLoader.loadROM( "mb7114e.11l", 0x0300, 0x0100, 0xe5490b68 );

		romLoader.loadZip(base_URL);

		return true;
	}

	private boolean rom_bktigerb() {
		romLoader.setZip("blktiger");

		romLoader.setMemory(REGION_CPU1);
		romLoader.loadROM("blktiger.6e", 0x10000, 0x10000, 0x7bef96e8); /* 0+1 */
		romLoader.loadROM("blktiger.9e", 0x30000, 0x10000, 0xed6af6ec); /* 4+5 */
		romLoader.loadROM("blktiger.10e", 0x40000, 0x10000, 0xae59b72e); /* 6+7 */

		//romLoader.setMemory( REGION_CPU2 );
		//romLoader.loadROM( "blktiger.1l", 0x0000, 0x8000, 0x2cf54274 );

		romLoader.setMemory(REGION_GFX1);
		romLoader.loadROM("blktiger.2n", 0x00000, 0x08000, 0x70175d78); /* characters */

		romLoader.setMemory(REGION_GFX2);
		romLoader.loadROM("blktiger.5b", 0x00000, 0x10000, 0xc4524993); /* tiles */
		romLoader.loadROM("blktiger.4b", 0x10000, 0x10000, 0x7932c86f);
		romLoader.loadROM("blktiger.9b", 0x20000, 0x10000, 0xdc49593a);
		romLoader.loadROM("blktiger.8b", 0x30000, 0x10000, 0x7ed7a122);

		romLoader.setMemory(REGION_GFX3);
		romLoader.loadROM("blktiger.5a", 0x00000, 0x10000, 0xe2f17438); /* sprites */
		romLoader.loadROM("blktiger.4a", 0x10000, 0x10000, 0x5fccbd27);
		romLoader.loadROM("blktiger.9a", 0x20000, 0x10000, 0xfc33ccc6);
		romLoader.loadROM("blktiger.8a", 0x30000, 0x10000, 0xf449de01);

		//romLoader.setMemory( REGION_PROMS); /* PROMs (function unknown) */
		//romLoader.loadROM( "mb7114e.8j", 0x0000, 0x0100, 0x29b459e5 );
		//romLoader.loadROM( "mb7114e.9j", 0x0100, 0x0100, 0x8b741e66 );
		//romLoader.loadROM( "mb7114e.11k", 0x0200, 0x0100, 0x27201c75 );
		//romLoader.loadROM( "mb7114e.11l", 0x0300, 0x0100, 0xe5490b68 );

		romLoader.loadZip(base_URL);

		romLoader.setZip("bktigerb");

		romLoader.setMemory(REGION_CPU1);
		romLoader.loadROM("btiger1.f6", 0x00000, 0x08000, 0x9d8464e8); /* CODE */
		romLoader.loadROM("btiger3.j6", 0x20000, 0x10000, 0x52c56ed1); /* 2+3 */

		romLoader.loadZip(base_URL);

		return true;
	}

	private boolean rom_blkdrgon() {
		romLoader.setZip("blktiger");

		//romLoader.setMemory( REGION_CPU2 );
		//romLoader.loadROM( "blktiger.1l", 0x0000, 0x8000, 0x2cf54274 );

		romLoader.setMemory(REGION_GFX3);
		romLoader.loadROM("blktiger.5a", 0x00000, 0x10000, 0xe2f17438); /* sprites */
		romLoader.loadROM("blktiger.4a", 0x10000, 0x10000, 0x5fccbd27);
		romLoader.loadROM("blktiger.9a", 0x20000, 0x10000, 0xfc33ccc6);
		romLoader.loadROM("blktiger.8a", 0x30000, 0x10000, 0xf449de01);

		//romLoader.setMemory( REGION_PROMS); /* PROMs (function unknown) */
		//romLoader.loadROM( "mb7114e.8j", 0x0000, 0x0100, 0x29b459e5 );
		//romLoader.loadROM( "mb7114e.9j", 0x0100, 0x0100, 0x8b741e66 );
		//romLoader.loadROM( "mb7114e.11k", 0x0200, 0x0100, 0x27201c75 );
		//romLoader.loadROM( "mb7114e.11l", 0x0300, 0x0100, 0xe5490b68 );

		romLoader.loadZip(base_URL);

		romLoader.setZip("blkdrgon");

		romLoader.setMemory(REGION_CPU1);
		romLoader.loadROM("blkdrgon.5e", 0x00000, 0x08000, 0x27ccdfbc); /* CODE */
		romLoader.loadROM("blkdrgon.6e", 0x10000, 0x10000, 0x7d39c26f); /* 0+1 */
		romLoader.loadROM("blkdrgon.8e", 0x20000, 0x10000, 0xd1bf3757); /* 2+3 */
		romLoader.loadROM("blkdrgon.9e", 0x30000, 0x10000, 0x4d1d6680); /* 4+5 */
		romLoader.loadROM("blkdrgon.10e", 0x40000, 0x10000, 0xc8d0c45e); /* 6+7 */

		romLoader.setMemory(REGION_GFX1);
		romLoader.loadROM("blkdrgon.2n", 0x00000, 0x08000, 0x3821ab29); /* characters */

		romLoader.setMemory(REGION_GFX2);
		romLoader.loadROM("blkdrgon.5b", 0x00000, 0x10000, 0x22d0a4b0); /* tiles */
		romLoader.loadROM("blkdrgon.4b", 0x10000, 0x10000, 0xc8b5fc52);
		romLoader.loadROM("blkdrgon.9b", 0x20000, 0x10000, 0x9498c378);
		romLoader.loadROM("blkdrgon.8b", 0x30000, 0x10000, 0x5b0df8ce);

		romLoader.loadZip(base_URL);

		return true;
	}

	private boolean rom_blkdrgnb() {
		romLoader.setZip("blktiger");

		//romLoader.setMemory( REGION_CPU2 );
		//romLoader.loadROM( "blktiger.1l", 0x0000, 0x8000, 0x2cf54274 )

		romLoader.setMemory(REGION_GFX3);
		romLoader.loadROM("blktiger.5a", 0x00000, 0x10000, 0xe2f17438); /* sprites */
		romLoader.loadROM("blktiger.4a", 0x10000, 0x10000, 0x5fccbd27);
		romLoader.loadROM("blktiger.9a", 0x20000, 0x10000, 0xfc33ccc6);
		romLoader.loadROM("blktiger.8a", 0x30000, 0x10000, 0xf449de01);

		//romLoader.setMemory( REGION_PROMS); /* PROMs (function unknown) */
		//romLoader.loadROM( "mb7114e.8j", 0x0000, 0x0100, 0x29b459e5 );
		//romLoader.loadROM( "mb7114e.9j", 0x0100, 0x0100, 0x8b741e66 );
		//romLoader.loadROM( "mb7114e.11k", 0x0200, 0x0100, 0x27201c75 );
		//romLoader.loadROM( "mb7114e.11l", 0x0300, 0x0100, 0xe5490b68 );

		romLoader.loadZip(base_URL);

		romLoader.setZip("blkdrgnb");

		romLoader.setMemory(REGION_CPU1);
		romLoader.loadROM("j1-5e", 0x00000, 0x08000, 0x97e84412); /* CODE */
		romLoader.loadROM("blkdrgon.6e", 0x10000, 0x10000, 0x7d39c26f); /* 0+1 */
		romLoader.loadROM("j3-8e", 0x20000, 0x10000, 0xf4cd0f39); /* 2+3 */
		romLoader.loadROM("blkdrgon.9e", 0x30000, 0x10000, 0x4d1d6680); /* 4+5 */
		romLoader.loadROM("blkdrgon.10e", 0x40000, 0x10000, 0xc8d0c45e); /* 6+7 */

		romLoader.setMemory(REGION_GFX1);
		romLoader.loadROM("j15-2n", 0x00000, 0x08000, 0x852ad2b7); /* characters */

		romLoader.setMemory(REGION_GFX2);
		romLoader.loadROM("blkdrgon.5b", 0x00000, 0x10000, 0x22d0a4b0); /* tiles */
		romLoader.loadROM("j11-4b", 0x10000, 0x10000, 0x053ab15c);
		romLoader.loadROM("blkdrgon.9b", 0x20000, 0x10000, 0x9498c378);
		romLoader.loadROM("j13-8b", 0x30000, 0x10000, 0x663d5afa);

		romLoader.loadZip(base_URL);

		return true;
	}

	public Machine getMachine(URL url, String name) {
		super.getMachine(url, name);
		super.setVideoEmulator(v);

		in[0] = new InputPort();
		in[1] = new InputPort();
		in[2] = new InputPort();
		in[3] = new InputPort();
		in[4] = new InputPort();
		in[5] = new InputPort();

		input_port_0_r = (ReadHandler) in[0];
		input_port_1_r = (ReadHandler) in[1];
		input_port_2_r = (ReadHandler) in[2];
		input_port_3_r = (ReadHandler) in[3];
		input_port_4_r = (ReadHandler) in[4];
		input_port_5_r = (ReadHandler) in[5];

		if (name.equals("blktiger")) {
			this.md = machine_driver_blktiger();
			GAME(1987, rom_blktiger(), ipt_blktiger(), v.blktiger(), ROT0, "Capcom", "Black Tiger");
		} else if (name.equals("bktigerb")) {
			this.md = machine_driver_blktiger();
			GAME(
				1987,
				rom_bktigerb(),
				"blktiger",
				ipt_blktiger(),
				v.blktiger(),
				ROT0,
				"bootleg",
				"Black Tiger (bootleg)");
		} else if (name.equals("blkdrgon")) {
			this.md = machine_driver_blktiger();
			GAME(
				1987,
				rom_blkdrgon(),
				"blktiger",
				ipt_blktiger(),
				v.blktiger(),
				ROT0,
				"Capcom",
				"Black Dragon");
		} else if (name.equals("blkdrgnb")) {
			this.md = machine_driver_blktiger();
			GAME(
				1987,
				rom_blkdrgnb(),
				"blktiger",
				ipt_blktiger(),
				v.blktiger(),
				ROT0,
				"bootleg",
				"Black Dragon (bootleg)");
		}

		m.init(md);
		v.setRegions(REGION_CPU1);
		return (Machine) m;
	}

}