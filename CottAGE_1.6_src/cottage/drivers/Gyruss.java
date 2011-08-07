/*
CottAGE - the Arcade Generic Emulator in Java

Java driver by Erik Duijs
*/

/***************************************************************************

Gyruss memory map (preliminary)

Main processor memory map.
0000-5fff ROM (6000-7fff diagnostics)
8000-83ff Color RAM
8400-87ff Video RAM
9000-a7ff RAM
a000-a17f \ sprites
a200-a27f /

memory mapped ports:

read:
c080      IN0
c0a0      IN1
c0c0      IN2
c0e0      DSW0
c000      DSW1
c100      DSW2

write:
a000-a1ff  Odd frame spriteram
a200-a3ff  Even frame spriteram
a700       Frame odd or even?
a701       Semaphore system:  tells 6809 to draw queued sprites
a702       Semaphore system:  tells 6809 to queue sprites
c000       watchdog reset
c080       trigger interrupt on audio CPU
c100       command for the audio CPU
c180       interrupt enable
c185       flip screen

interrupts:
standard NMI at 0x66


SOUND BOARD:
0000-3fff  Audio ROM (4000-5fff diagnostics)
6000-63ff  Audio RAM
8000       Read Sound Command

I/O:

Gyruss has 5 PSGs:
1)  Control: 0x00    Read: 0x01    Write: 0x02
2)  Control: 0x04    Read: 0x05    Write: 0x06
3)  Control: 0x08    Read: 0x09    Write: 0x0a
4)  Control: 0x0c    Read: 0x0d    Write: 0x0e
5)  Control: 0x10    Read: 0x11    Write: 0x12

and 1 SFX channel controlled by an 8039:
1)  SoundOn: 0x14    SoundData: 0x18

***************************************************************************/

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
import jef.video.Eof_callback;
import jef.video.GfxDecodeInfo;
import jef.video.GfxLayout;
import jef.video.GfxManager;
import jef.video.Vh_convert_color_proms;
import jef.video.Vh_refresh;
import jef.video.Vh_start;
import jef.video.Vh_stop;

import cottage.mame.Driver;
import cottage.mame.MAMEConstants;
import cottage.mame.MAMEDriver;

public class Gyruss extends MAMEDriver implements Driver,

													  MAMEConstants {


	int[] memCpu1 = new int[0x10000];
	int[] REGION_CPU4 = new int[0x20000];
	int[] REGION_GFX1 = new int[0x2000];
	int[] REGION_GFX2 = new int[0x8000];
	int[] REGION_PROMS = new int[0x220];


	InputPort[] in = new InputPort[6];

	ReadHandler		input_port_0_r;
	ReadHandler		input_port_1_r;
	ReadHandler		input_port_2_r;
	ReadHandler		input_port_3_r;
	ReadHandler		input_port_4_r;
	ReadHandler		input_port_5_r;

	cottage.vidhrdw.Gyruss 	v								= new cottage.vidhrdw.Gyruss();
	WriteHandler 				videoram_w						= v.videoram_w(memCpu1, v);
	WriteHandler				colorram_w						= videoram_w;
	WriteHandler 				gyruss_spritebank				= v.gyruss_spritebank_w(v);
	WriteHandler 				gyruss_queuereg_w				= v.gyruss_queuereg_w(v);
	Eof_callback				noCallback						= (Eof_callback)v;
	Vh_refresh 					gyruss_vh_screenrefresh 		= (Vh_refresh)v;
	Vh_start					generic_vh_start				= (Vh_start)v;
	Vh_stop						generic_vh_stop					= (Vh_stop)v;
	Vh_convert_color_proms 		gyruss_vh_convert_color_prom	= (Vh_convert_color_proms)v;

	cottage.machine.Gyruss m				= new cottage.machine.Gyruss();
	InterruptHandler nmi_interrupt				= m.nmi_interrupt(m);
	WriteHandler	 interrupt_enable_w			= m.interrupt_enable_w(m);

	public Machine getMachine(URL url, String name) {
		super.getMachine(url,name);
		super.setVideoEmulator(v);

		in[0] = new InputPort();
		in[1] = new InputPort();
		in[2] = new InputPort();
		in[3] = new InputPort();
		in[4] = new InputPort();
		in[5] = new InputPort();

		input_port_0_r = (ReadHandler)in[0];
		input_port_1_r = (ReadHandler)in[1];
		input_port_2_r = (ReadHandler)in[2];
		input_port_3_r = (ReadHandler)in[3];
		input_port_4_r = (ReadHandler)in[4];
		input_port_5_r = (ReadHandler)in[5];


		if (name.equals("gyruss")) {
			this.md = machine_driver_gyruss();
			GAME(1983, rom_gyruss(), ipt_gyruss(), v.gyruss(), ROT90, "Konami", "Gyruss (Konami)" );
		}

		v.setRegions(REGION_PROMS, memCpu1, REGION_CPU4);

		m.init(md);
		return (Machine)m;
	}



	public MachineDriver machine_driver_gyruss() {
		CpuDriver[] cpuDriver = new CpuDriver[1];

		cpuDriver[0] = new CpuDriver( (Cpu) new Z80(),
										3072000,        /* 3.072 Mhz (?) */
										readmem(), writemem(), readport(), writeport(),
										nmi_interrupt, 1 );

		int[] visibleArea = { 0*8, 32*8-1, 2*8, 30*8-1 };

		return new MachineDriver
		(
			cpuDriver,

			60, DEFAULT_60HZ_VBLANK_DURATION,
			1,
			NOP,

			//video;
			32*8, 32*8, visibleArea,
			gfxdecodeinfo(),
			32,16*4+16*16,
			gyruss_vh_convert_color_prom,

			VIDEO_TYPE_RASTER|GfxManager.VIDEO_SUPPORTS_DIRTY,

			noCallback,
			generic_vh_start,
			generic_vh_stop,
			gyruss_vh_screenrefresh,
			noSound

		);
	}

	private MemoryReadAddress readmem() {
		MemoryReadAddress mra = new MemoryReadAddress(memCpu1);
		mra.setMR( 0x0000, 0x7fff, MRA_ROM );
		mra.setMR( 0x8000, 0x87ff, MRA_RAM );
		mra.setMR( 0x9000, 0x9fff, MRA_RAM );
		mra.setMR( 0xa000, 0xa7ff, MRA_RAM );
		mra.set( 0xc000, 0xc000, input_port_4_r );	/* DSW1 */
		mra.set( 0xc080, 0xc080, input_port_0_r );	/* IN0 */
		mra.set( 0xc0a0, 0xc0a0, input_port_1_r );	/* IN1 */
		mra.set( 0xc0c0, 0xc0c0, input_port_2_r );	/* IN2 */
		mra.set( 0xc0e0, 0xc0e0, input_port_3_r );	/* DSW0 */
		mra.set( 0xc100, 0xc100, input_port_5_r );	/* DSW2 */
		return mra;
	}


	private MemoryWriteAddress writemem() {
		MemoryWriteAddress mwa = new MemoryWriteAddress(memCpu1);
		mwa.setMW( 0x0000, 0x7fff, MWA_ROM );
		mwa.set( 0x8000, 0x83ff, colorram_w );
		mwa.set( 0x8400, 0x87ff, videoram_w );
		mwa.setMW( 0x9000, 0x9fff, MWA_RAM );
		mwa.setMW( 0xa000, 0xa17f, MWA_RAM );     /* odd frame spriteram */
		mwa.setMW( 0xa200, 0xa37f, MWA_RAM );		/* even frame spriteram */
		mwa.set( 0xa700, 0xa700, gyruss_spritebank );
		mwa.setMW( 0xa701, 0xa701, MWA_NOP );     /* semaphore system   */
		mwa.set( 0xa702, 0xa702, gyruss_queuereg_w );     /* semaphore system   */
		mwa.setMW( 0xa7fc, 0xa7fc, MWA_RAM ); // &gyruss_6809_drawplanet
		mwa.setMW( 0xa7fd, 0xa7fd, MWA_RAM ); // &gyruss_6809_drawship
		mwa.setMW( 0xc000, 0xc000, MWA_NOP );		/* watchdog reset */
		//mwa.set( 0xc080, 0xc080, gyruss_sh_irqtrigger_w );
		//mwa.set( 0xc100, 0xc100, soundlatch_w );/* command to soundb */
		mwa.set( 0xc180, 0xc180, interrupt_enable_w );	/* NMI enable	*/
		//mwa.set( 0xc185, 0xc185, gyruss_flipscreen_w );

		return mwa;
	}

	private IOReadPort readport() {
		IOReadPort ior = new IOReadPort();
		return ior;
	}

	private IOWritePort writeport() {
		IOWritePort	iow = new IOWritePort();
		return iow;
	}

	private InputPort[] ipt_gyruss() {
		/* IN0 */
		in[0].setBit( 0x01, IP_ACTIVE_LOW, IPT_COIN1 );
		in[0].setBit( 0x02, IP_ACTIVE_LOW, IPT_COIN2 );
		in[0].setBit( 0x04, IP_ACTIVE_LOW, IPT_COIN3 );
		in[0].setBit( 0x08, IP_ACTIVE_LOW, IPT_START1 );
		in[0].setBit( 0x10, IP_ACTIVE_LOW, IPT_START2 );
		in[0].setBit( 0xe0, IP_ACTIVE_LOW, IPT_UNUSED );

		/* IN1 */
		in[1].setBit( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT | IPF_2WAY );
		in[1].setBit( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_2WAY );
		in[1].setBit( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_UP | IPF_2WAY );
		in[1].setBit( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN | IPF_2WAY );
		in[1].setBit( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 );
		in[1].setBit( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );	/* 1p shoot 2 - unused */
		in[1].setBit( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );	/* 2p shoot 3 - unused */
		in[1].setBit( 0x80, IP_ACTIVE_LOW, IPT_UNUSED );

		/* IN2 */
		in[2].setBit( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT | IPF_2WAY | IPF_COCKTAIL );
		in[2].setBit( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_2WAY | IPF_COCKTAIL );
		in[2].setBit( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_UP | IPF_2WAY | IPF_COCKTAIL );
		in[2].setBit( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN | IPF_2WAY | IPF_COCKTAIL );
		in[2].setBit( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_COCKTAIL );
		in[2].setBit( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );	/* 2p shoot 2 - unused */
		in[2].setBit( 0xc0, IP_ACTIVE_LOW, IPT_UNUSED );

		/* DSW0 */
		in[3].setDipName( 0xf0, 0xf0, DEF_STR[ Coin_B ] );
		in[3].setDipSetting(    0x20, DEF_STR[ _4C_1C ] );
		in[3].setDipSetting(    0x50, DEF_STR[ _3C_1C ] );
		in[3].setDipSetting(    0x80, DEF_STR[ _2C_1C ] );
		//in[3].setDipSetting(    0x40, DEF_STR[ _3C_2C ] );
		//in[3].setDipSetting(    0x10, DEF_STR[ _4C_3C ] );
		in[3].setDipSetting(    0xf0, DEF_STR[ _1C_1C ] );
		//in[3].setDipSetting(    0x30, DEF_STR[ _3C_4C ] );
		//in[3].setDipSetting(    0x70, DEF_STR[ _2C_3C ] );
		in[3].setDipSetting(    0xe0, DEF_STR[ _1C_2C ] );
		//in[3].setDipSetting(    0x60, DEF_STR[ _2C_5C ] );
		in[3].setDipSetting(    0xd0, DEF_STR[ _1C_3C ] );
		in[3].setDipSetting(    0xc0, DEF_STR[ _1C_4C ] );
		in[3].setDipSetting(    0xb0, DEF_STR[ _1C_5C ] );
		in[3].setDipSetting(    0xa0, DEF_STR[ _1C_6C ] );
		in[3].setDipSetting(    0x90, DEF_STR[ _1C_7C ] );
		in[3].setDipSetting(    0x00, DEF_STR[ Free_Play ] );
		in[3].setDipName( 0x0f, 0x0f, DEF_STR[ Coin_A ] );
		in[3].setDipSetting(    0x02, DEF_STR[ _4C_1C ] );
		in[3].setDipSetting(    0x05, DEF_STR[ _3C_1C ] );
		in[3].setDipSetting(    0x08, DEF_STR[ _2C_1C ] );
		//in[3].setDipSetting(    0x04, DEF_STR[ _3C_2C ] );
		//in[3].setDipSetting(    0x01, DEF_STR[ _4C_3C ] );
		in[3].setDipSetting(    0x0f, DEF_STR[ _1C_1C ] );
		//in[3].setDipSetting(    0x03, DEF_STR[ _3C_4C ] );
		//in[3].setDipSetting(    0x07, DEF_STR[ _2C_3C ] );
		in[3].setDipSetting(    0x0e, DEF_STR[ _1C_2C ] );
		//in[3].setDipSetting(    0x06, DEF_STR[ _2C_5C ] );
		in[3].setDipSetting(    0x0d, DEF_STR[ _1C_3C ] );
		in[3].setDipSetting(    0x0c, DEF_STR[ _1C_4C ] );
		in[3].setDipSetting(    0x0b, DEF_STR[ _1C_5C ] );
		in[3].setDipSetting(    0x0a, DEF_STR[ _1C_6C ] );
		in[3].setDipSetting(    0x09, DEF_STR[ _1C_7C ] );
		in[3].setDipSetting(    0x00, DEF_STR[ Free_Play ] );

		/* DSW1 */
		in[4].setDipName( 0x03, 0x03, DEF_STR[ Lives ] );
		in[4].setDipSetting(    0x03, "3" );
		in[4].setDipSetting(    0x02, "4" );
		in[4].setDipSetting(    0x01, "5" );
		//in[4].setBitX( 0,       0x00, IPT_DIPSWITCH_SETTING | IPF_CHEAT, "255", IP_KEY_NONE, IP_JOY_NONE );
		in[4].setDipName( 0x04, 0x00, DEF_STR[ Cabinet ] );
		in[4].setDipSetting(    0x00, DEF_STR[ Upright ] );
		in[4].setDipSetting(    0x04, DEF_STR[ Cocktail ] );
		in[4].setDipName( 0x08, 0x08, DEF_STR[ Bonus_Life ] );
		in[4].setDipSetting(    0x08, "30000 60000" );
		in[4].setDipSetting(    0x00, "40000 70000" );
		in[4].setDipName( 0x70, 0x70, DEF_STR[ Difficulty ] );
		in[4].setDipSetting(    0x70, "1 (Easiest)" );
		in[4].setDipSetting(    0x60, "2" );
		in[4].setDipSetting(    0x50, "3" );
		in[4].setDipSetting(    0x40, "4" );
		in[4].setDipSetting(    0x30, "5 (Average)" );
		in[4].setDipSetting(    0x20, "6" );
		in[4].setDipSetting(    0x10, "7" );
		in[4].setDipSetting(    0x00, "8 (Hardest)" );
		in[4].setDipName( 0x80, 0x00, DEF_STR[ Demo_Sounds ] );
		in[4].setDipSetting(    0x80, DEF_STR[ Off ] );
		in[4].setDipSetting(    0x00, DEF_STR[ On ] );

		/* DSW2 */
		in[5].setDipName( 0x01, 0x00, "Demo Music" );
		in[5].setDipSetting(    0x01, DEF_STR[ Off ] );
		in[5].setDipSetting(    0x00, DEF_STR[ On ] );
		/* other bits probably unused */
		return in;
	}

	private GfxLayout charlayout() {

		int[] pOffs = { 4, 0 };
		int[] xOffs = { 0, 1, 2, 3, 8*8+0,8*8+1,8*8+2,8*8+3 };
		int[] yOffs = { 0*8, 1*8, 2*8, 3*8, 4*8, 5*8, 6*8, 7*8 };

		return new GfxLayout(
			8,8,	/* 8*8 characters */
			512,	/* 512 characters */
			2,		/* 2 bits per pixel */
			pOffs,
			xOffs,
			yOffs,
			16*8	/* every char takes 16 consecutive bytes */
		);
	}

	private GfxLayout spritelayout1() {

		int[] pOffs = { 4, 0, 0x4000*8+4, 0x4000*8+0 };
		int[] xOffs = { 0, 1, 2, 3,  8*8, 8*8+1, 8*8+2, 8*8+3 };
		int[] yOffs = { 0*8, 1*8, 2*8, 3*8, 4*8, 5*8, 6*8, 7*8, 32*8, 33*8, 34*8, 35*8, 36*8, 37*8, 38*8, 39*8 };

		return new GfxLayout(
			8,16,	/* 16*8 sprites */
			256,	/* 256 sprites */
			4,	/* 4 bits per pixel */
			pOffs,
			xOffs,
			yOffs,
			64*8	/* every char takes 64 consecutive bytes */
		);
	}

	private GfxLayout spritelayout2() {

		int[] pOffs = { 4, 0, 0x4000*8+4, 0x4000*8+0 };
		int[] xOffs = { 0, 1, 2, 3,  8*8, 8*8+1, 8*8+2, 8*8+3, 16*8+0, 16*8+1, 16*8+2, 16*8+3,  24*8, 24*8+1, 24*8+2, 24*8+3 };
		int[] yOffs = { 0*8, 1*8, 2*8, 3*8, 4*8, 5*8, 6*8, 7*8, 32*8, 33*8, 34*8, 35*8, 36*8, 37*8, 38*8, 39*8 };

		return new GfxLayout(
			16,16,	/* 16*16 sprites */
			256,	/* 256 sprites */
			4,	/* 4 bits per pixel */
			pOffs,
			xOffs,
			yOffs,
			64*8	/* every char takes 64 consecutive bytes */
		);
	}

	private GfxDecodeInfo[] gfxdecodeinfo() {
		GfxDecodeInfo gdi[] = new GfxDecodeInfo[4];
		gdi[0] = new GfxDecodeInfo( REGION_GFX1, 0x0000, charlayout(), 	  0,	16 );
		gdi[1] = new GfxDecodeInfo( REGION_GFX2, 0x0000, spritelayout2(), 16*4, 16 );	/* upper half */
		gdi[2] = new GfxDecodeInfo( REGION_GFX2, 0x0010, spritelayout2(), 16*4, 16 );	/* lower half */
		gdi[3] = new GfxDecodeInfo( REGION_GFX2, 0x0000, spritelayout2(), 16*4, 16 );
		return gdi;
	}

	private boolean rom_gyruss() {
		romLoader.setMemory( memCpu1 );
		romLoader.setZip( "gyruss" );
		romLoader.loadROM( "gyrussk.1",    0x0000, 0x2000, 0xc673b43d );
		romLoader.loadROM( "gyrussk.2",    0x2000, 0x2000, 0xa4ec03e4 );
		romLoader.loadROM( "gyrussk.3",    0x4000, 0x2000, 0x27454a98 );

		romLoader.setMemory( REGION_CPU4 );
		romLoader.loadROM( "gyrussk.9",    0xe000, 0x2000, 0x822bf27e );

		romLoader.setMemory( REGION_GFX1 );
		romLoader.loadROM( "gyrussk.4",    0x0000, 0x2000, 0x27d8329b );

		romLoader.setMemory( REGION_GFX2 );
		romLoader.loadROM( "gyrussk.6",    0x0000, 0x2000, 0xc949db10 );
		romLoader.loadROM( "gyrussk.5",    0x2000, 0x2000, 0x4f22411a );
		romLoader.loadROM( "gyrussk.8",    0x4000, 0x2000, 0x47cd1fbc );
		romLoader.loadROM( "gyrussk.7",    0x6000, 0x2000, 0x8e8d388c );

		romLoader.setMemory( REGION_PROMS );
		romLoader.loadROM( "gyrussk.pr3",  0x0000, 0x0020, 0x98782db3 );	/* palette */
		romLoader.loadROM( "gyrussk.pr1",  0x0020, 0x0100, 0x7ed057de );	/* sprite lookup table */
		romLoader.loadROM( "gyrussk.pr2",  0x0120, 0x0100, 0xde823a81 );	/* character lookup table */

		romLoader.loadZip(base_URL);
		return true;
	}

}