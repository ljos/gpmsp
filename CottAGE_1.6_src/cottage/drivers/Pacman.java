/*
CottAGE - the Arcade Generic Emulator in Java

Java driver by Erik Duijs
*/

/***************************************************************************

	Namco PuckMan

    driver by Nicola Salmoria and many others

    Games supported:
		* PuckMan
		* Pac-Man Plus
		* Ms. Pac-Man
		* Crush Roller
		* Ponpoko
		* Eyes
		* Mr. TNT
		* Lizard Wizard
		* The Glob
		* Dream Shopper
		* Van Van Car
		* Ali Baba and 40 Thieves
		* Jump Shot
		* Shoot the Bull

	Known issues:
		* mystery items in Ali Baba don't work correctly because of protection

****************************************************************************

	Pac-Man memory map (preliminary)

	0000-3fff ROM
	4000-43ff Video RAM
	4400-47ff Color RAM
	4c00-4fff RAM
	8000-9fff ROM (Ms Pac-Man and Ponpoko only)
	a000-bfff ROM (Ponpoko only)

	memory mapped ports:

	read:
	5000      IN0
	5040      IN1
	5080      DSW 1
	50c0	  DSW 2 (Ponpoko only)
	see the input_ports definition below for details on the input bits

	write:
	4ff0-4fff 8 pairs of two bytes:
	          the first byte contains the sprite image number (bits 2-7), Y flip (bit 0),
			  X flip (bit 1); the second byte the color
	5000      interrupt enable
	5001      sound enable
	5002      ????
	5003      flip screen
	5004      1 player start lamp
	5005      2 players start lamp
	5006      coin lockout
	5007      coin counter
	5040-5044 sound voice 1 accumulator (nibbles) (used by the sound hardware only)
	5045      sound voice 1 waveform (nibble)
	5046-5049 sound voice 2 accumulator (nibbles) (used by the sound hardware only)
	504a      sound voice 2 waveform (nibble)
	504b-504e sound voice 3 accumulator (nibbles) (used by the sound hardware only)
	504f      sound voice 3 waveform (nibble)
	5050-5054 sound voice 1 frequency (nibbles)
	5055      sound voice 1 volume (nibble)
	5056-5059 sound voice 2 frequency (nibbles)
	505a      sound voice 2 volume (nibble)
	505b-505e sound voice 3 frequency (nibbles)
	505f      sound voice 3 volume (nibble)
	5060-506f Sprite coordinates, x/y pairs for 8 sprites
	50c0      Watchdog reset

	I/O ports:
	OUT on port $0 sets the interrupt vector


****************************************************************************

	Make Trax protection description:

	Make Trax has a "Special" chip that it uses for copy protection.
	The following chart shows when reads and writes may occur:

	AAAAAAAA AAAAAAAA
	11111100 00000000  <- address bits
	54321098 76543210
	xxx1xxxx 01xxxxxx - read data bits 4 and 7
	xxx1xxxx 10xxxxxx - read data bits 6 and 7
	xxx1xxxx 11xxxxxx - read data bits 0 through 5

	xxx1xxxx 00xxx100 - write to Special
	xxx1xxxx 00xxx101 - write to Special
	xxx1xxxx 00xxx110 - write to Special
	xxx1xxxx 00xxx111 - write to Special

	In practical terms, it reads from Special when it reads from
	location $5040-$50FF.  Note that these locations overlap our
	inputs and Dip Switches.  Yuk.

	I don't bother trapping the writes right now, because I don't
	know how to interpret them.  However, comparing against Crush
	Roller gives most of the values necessary on the reads.

	Instead of always reading from $5040, $5080, and $50C0, the Make
	Trax programmers chose to read from a wide variety of locations,
	probably to make debugging easier.  To us, it means that for the most
	part we can just assign a specific value to return for each address and
	we'll be OK.  This falls apart for the following addresses:  $50C0, $508E,
	$5090, and $5080.  These addresses should return multiple values.  The other
	ugly thing happening is in the ROMs at $3AE5.  It keeps checking for
	different values of $50C0 and $5080, and weird things happen if it gets
	the wrong values.  The only way I've found around these is to patch the
	ROMs using the same patches Crush Roller uses.  The only thing to watch
	with this is that changing the ROMs will break the beginning checksum.
	That's why we use the rom opcode decode function to do our patches.

	Incidentally, there are extremely few differences between Crush Roller
	and Make Trax.  About 98% of the differences appear to be either unused
	bytes, the name of the game, or code related to the protection.  I've
	only spotted two or three actual differences in the games, and they all
	seem minor.

	If anybody cares, here's a list of disassembled addresses for every
	read and write to the Special chip (not all of the reads are
	specifically for checking the Special bits, some are for checking
	player inputs and Dip Switches):

	Writes: $0084, $012F, $0178, $023C, $0C4C, $1426, $1802, $1817,
		$280C, $2C2E, $2E22, $3205, $3AB7, $3ACC, $3F3D, $3F40,
		$3F4E, $3F5E
	Reads:  $01C8, $01D2, $0260, $030E, $040E, $0416, $046E, $0474,
		$0560, $0568, $05B0, $05B8, $096D, $0972, $0981, $0C27,
		$0C2C, $0F0A, $10B8, $10BE, $111F, $1127, $1156, $115E,
		$11E3, $11E8, $18B7, $18BC, $18CA, $1973, $197A, $1BE7,
		$1C06, $1C9F, $1CAA, $1D79, $213D, $2142, $2389, $238F,
		$2AAE, $2BF4, $2E0A, $39D5, $39DA, $3AE2, $3AEA, $3EE0,
		$3EE9, $3F07, $3F0D

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
import jef.map.MemoryReadAddressMap;
import jef.map.MemoryWriteAddress;
import jef.map.MemoryWriteAddressMap;
import jef.map.ReadHandler;
import jef.map.ReadMap;
import jef.map.VoidFunction;
import jef.map.WriteHandler;
import jef.map.WriteMap;
import jef.sound.SoundChipEmulator;
import jef.sound.chip.AY8910;
import jef.sound.chip.Namco;
import jef.video.Eof_callback;
import jef.video.GfxDecodeInfo;
import jef.video.GfxLayout;
import jef.video.Vh_convert_color_proms;
import jef.video.Vh_refresh;
import jef.video.Vh_start;
import jef.video.Vh_stop;

import cottage.mame.Driver;
import cottage.mame.MAMEConstants;
import cottage.mame.MAMEDriver;

public class Pacman extends MAMEDriver implements Driver, MAMEConstants {


	int[] REGION_CPU1 = new int[0x20000];
	int[] REGION_GFX1 = new int[0x2000];
	int[] REGION_GFX2 = new int[0x2000];
	int[] REGION_PROMS = new int[0x120];
	//int[] REGION_SOUND = new int[0x100];

    int[] sound_prom = {
		0x07,0x09,0x0A,0x0B,0x0C,0x0D,0x0D,0x0E,0x0E,0x0E,0x0D,0x0D,0x0C,0x0B,0x0A,0x09,
		0x07,0x05,0x04,0x03,0x02,0x01,0x01,0x00,0x00,0x00,0x01,0x01,0x02,0x03,0x04,0x05,
		0x07,0x0C,0x0E,0x0E,0x0D,0x0B,0x09,0x0A,0x0B,0x0B,0x0A,0x09,0x06,0x04,0x03,0x05,
		0x07,0x09,0x0B,0x0A,0x08,0x05,0x04,0x03,0x03,0x04,0x05,0x03,0x01,0x00,0x00,0x02,
		0x07,0x0A,0x0C,0x0D,0x0E,0x0D,0x0C,0x0A,0x07,0x04,0x02,0x01,0x00,0x01,0x02,0x04,
		0x07,0x0B,0x0D,0x0E,0x0D,0x0B,0x07,0x03,0x01,0x00,0x01,0x03,0x07,0x0E,0x07,0x00,
		0x07,0x0D,0x0B,0x08,0x0B,0x0D,0x09,0x06,0x0B,0x0E,0x0C,0x07,0x09,0x0A,0x06,0x02,
		0x07,0x0C,0x08,0x04,0x05,0x07,0x02,0x00,0x03,0x08,0x05,0x01,0x03,0x06,0x03,0x01,
		0x00,0x08,0x0F,0x07,0x01,0x08,0x0E,0x07,0x02,0x08,0x0D,0x07,0x03,0x08,0x0C,0x07,
		0x04,0x08,0x0B,0x07,0x05,0x08,0x0A,0x07,0x06,0x08,0x09,0x07,0x07,0x08,0x08,0x07,
		0x07,0x08,0x06,0x09,0x05,0x0A,0x04,0x0B,0x03,0x0C,0x02,0x0D,0x01,0x0E,0x00,0x0F,
		0x00,0x0F,0x01,0x0E,0x02,0x0D,0x03,0x0C,0x04,0x0B,0x05,0x0A,0x06,0x09,0x07,0x08,
		0x00,0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,0x0A,0x0B,0x0C,0x0D,0x0E,0x0F,
		0x0F,0x0E,0x0D,0x0C,0x0B,0x0A,0x09,0x08,0x07,0x06,0x05,0x04,0x03,0x02,0x01,0x00,
		0x00,0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,0x0A,0x0B,0x0C,0x0D,0x0E,0x0F,
		0x00,0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,0x0A,0x0B,0x0C,0x0D,0x0E,0x0F
	};

    Namco	namco = new Namco ( REGION_CPU1,
								3,
								3072000/32,
								sound_prom );

	AY8910	ay8910 = new AY8910 ( 	1,			  /* 1 chip */
									14318000/8 ); /* 1.78975 MHz ??? */

	InputPort[] in = new InputPort[4];

	ReadHandler		input_port_0_r;
	ReadHandler		input_port_1_r;
	ReadHandler		input_port_2_r;
	ReadHandler		input_port_3_r;

	cottage.vidhrdw.Pacman 	    v						= new cottage.vidhrdw.Pacman();
	WriteHandler 				videoram_w				= v.videoram_w(REGION_CPU1, v);
	WriteHandler				colorram_w				= videoram_w;
	Eof_callback				noCallback				= (Eof_callback)v;
	Vh_refresh 					pengo_vh_screenrefresh 	= (Vh_refresh)v;
	Vh_start					pacman_vh_start			= (Vh_start)v;
	Vh_stop						generic_vh_stop			= (Vh_stop)v;
	Vh_convert_color_proms 		pacman_vh_convert_color_prom = (Vh_convert_color_proms)v;

	cottage.machine.Pacman m				= new cottage.machine.Pacman();
	//WriteHandler 	 hiscore				= m.hiscore(REGION_CPU1, videoram_w);
	InterruptHandler pacman_interrupt		= m.pacman_interrupt(m);
	InterruptHandler nmi_interrupt			= m.pacman_nmi_interrupt(m);
	WriteHandler	 interrupt_enable_w		= m.interrupt_enable_w(m);
	WriteHandler	 interrupt_vector_w		= m.interrupt_vector_w(m);
	WriteHandler	 pengo_sound_w			= namco.pengo_sound_w(0x5040);
	WriteHandler	 AY8910_write_port_0_w  = ay8910.AY8910_write_port_0_w();
	WriteHandler	 AY8910_control_port_0_w= ay8910.AY8910_control_port_0_w();
	ReadHandler		 theglob_decrypt_rom	= m.theglob_decrypt_rom(m);
	ReadHandler		 MRA_BANK1				= m.MRA_BANK1(m,REGION_CPU1);
	VoidFunction	 theglob_init_machine	= m.theglob_init_machine(m,REGION_CPU1);


	public Machine getMachine(URL url, String name) {
		super.getMachine(url,name);
		super.setVideoEmulator(v);

		in[0] = new InputPort();
		in[1] = new InputPort();
		in[2] = new InputPort();
		in[3] = new InputPort();

		input_port_0_r = (ReadHandler)in[0];
		input_port_1_r = (ReadHandler)in[1];
		input_port_2_r = (ReadHandler)in[2];
		input_port_3_r = (ReadHandler)in[3];


		if (name.equals("mspacman")) {
			this.md = machine_driver_pacman();
			GAME(1981, rom_mspacman(), 	ipt_mspacman(), v.pacman(), ROT90, "Bootleg", "Ms. Pac-Man" );
		}

		v.setRegions(REGION_PROMS, REGION_CPU1);

		m.init(md);
		return (Machine)m;
	}


/*************************************
 *
 *	Main CPU memory handlers
 *
 *************************************/

	private ReadMap readmem() {
        ReadMap mra = new MemoryReadAddressMap(REGION_CPU1);
		mra.setMR( 0x0000, 0x3fff, MRA_ROM );
		mra.setMR( 0x4000, 0x47ff, MRA_RAM );	/* video and color RAM */
		mra.setMR( 0x4800, 0x4fff, MRA_RAM );	/* including sprite codes at 4ff0-4fff */
		mra.set( 0x5000, 0x503f, input_port_0_r );	/* IN0 */
		mra.set( 0x5040, 0x507f, input_port_1_r );	/* IN1 */
		mra.set( 0x5080, 0x50bf, input_port_2_r );	/* DSW1 */
		mra.set( 0x50c0, 0x50ff, input_port_3_r );	/* DSW2 */
		mra.setMR( 0x8000, 0xbfff, MRA_ROM );	/* Ms. Pac-Man / Ponpoko only */
		return mra;
	}

	private WriteMap writemem() {
        WriteMap mwa = new MemoryWriteAddressMap(REGION_CPU1);
		mwa.setMW( 0x0000, 0x3fff, MWA_ROM );
		mwa.set( 0x4000, 0x43ff, videoram_w );
		//mwa.set( 0x43ed, 0x43f3, hiscore);
		mwa.set( 0x4400, 0x47ff, colorram_w );
		mwa.setMW( 0x4800, 0x4fef, MWA_RAM );
		mwa.setMW( 0x4ff0, 0x4fff, MWA_RAM );
		mwa.set( 0x5000, 0x5000, interrupt_enable_w );
	//	mwa.set( 0x5001, 0x5001, pengo_sound_enable_w };
		mwa.setMW( 0x5002, 0x5002, MWA_NOP );
	//	mwa.set( 0x5003, 0x5003, pengo_flipscreen_w };
	//	mwa.set( 0x5004, 0x5005, pacman_leds_w };
	// 	mwa.set( 0x5006, 0x5006, pacman_coin_lockout_global_w };	this breaks many games
	//	mwa.set( 0x5007, 0x5007, pacman_coin_counter_w };
		mwa.set( 0x5040, 0x505f, pengo_sound_w );
		mwa.setMW( 0x5060, 0x506f, MWA_RAM );
	//	mwa.set( 0x50c0, 0x50c0, watchdog_reset_w };
		mwa.setMW( 0x8000, 0xbfff, MWA_ROM );	/* Ms. Pac-Man / Ponpoko only */
		mwa.set( 0xc000, 0xc3ff, videoram_w ); /* mirror address for video ram, */
		mwa.set( 0xc400, 0xc7ef, colorram_w ); /* used to display HIGH SCORE and CREDITS */
		mwa.setMW( 0xffff, 0xffff, MWA_NOP );	/* Eyes writes to this location to simplify code */
		return mwa;
	}

	private ReadMap theglob_readmem() {
		MemoryReadAddress mra = new MemoryReadAddress(REGION_CPU1);
		mra.set( 0x0000, 0x3fff, MRA_BANK1 );
		mra.setMR( 0x4000, 0x47ff, MRA_RAM );	/* video and color RAM */
		mra.setMR( 0x4800, 0x4fff, MRA_RAM );	/* including sprite codes at 4ff0-4fff */
		mra.set( 0x5000, 0x503f, input_port_0_r );	/* IN0 */
		mra.set( 0x5040, 0x507f, input_port_1_r );	/* IN1 */
		mra.set( 0x5080, 0x50bf, input_port_2_r );	/* DSW1 */
		mra.set( 0x50c0, 0x50ff, input_port_3_r );	/* DSW2 */
		return mra;
	}

	private ReadMap vanvan_readmem() {
		MemoryReadAddress mra = new MemoryReadAddress(REGION_CPU1);
		mra.setMR( 0x0000, 0x3fff, MRA_ROM );
		mra.setMR( 0x4000, 0x47ff, MRA_RAM );	/* video and color RAM */
		mra.setMR( 0x4800, 0x4fff, MRA_RAM );	/* including sprite codes at 4ff0-4fff */
		mra.set( 0x5000, 0x5000, input_port_0_r );	/* IN0 */
		mra.set( 0x5040, 0x5040, input_port_1_r );	/* IN1 */
		mra.set( 0x5080, 0x5080, input_port_2_r );	/* DSW1 */
		mra.set( 0x50c0, 0x50c0, input_port_3_r );	/* DSW2 */
		mra.setMR( 0x8000, 0x8fff, MRA_ROM );
		return mra;
	}


	private WriteMap vanvan_writemem() {
		MemoryWriteAddress mwa = new MemoryWriteAddress(REGION_CPU1);
		mwa.setMW( 0x0000, 0x3fff, MWA_ROM );
		mwa.set( 0x4000, 0x43ff, videoram_w );
		mwa.set( 0x4400, 0x47ff, colorram_w );
		mwa.setMW( 0x4800, 0x4fef, MWA_RAM );
		mwa.setMW( 0x4ff0, 0x4fff, MWA_RAM );
		mwa.set( 0x5000, 0x5000, interrupt_enable_w );
		//mwa.set( 0x5001, 0x5001, vanvan_bgcolor_w );
		//mwa.set( 0x5003, 0x5003, pengo_flipscreen_w );
		mwa.setMW( 0x5005, 0x5006, MWA_NOP );	/* always written together with 5001 */
		//mwa.set( 0x5007, 0x5007, pacman_coin_counter_w );
		mwa.setMW( 0x5060, 0x506f, MWA_RAM );
		mwa.setMW( 0x5080, 0x5080, MWA_NOP );	/* ??? toggled before reading 5000 */
		//mwa.set( 0x50c0, 0x50c0, watchdog_reset_w );
		mwa.setMW( 0x8000, 0x8fff, MWA_ROM );
		mwa.setMW( 0xb800, 0xb87f, MWA_NOP );	/* probably a leftover from development: the Sanritsu version */
											/* writes the color lookup table here, while the Karateko version */
											/* writes garbage. */
		return mwa;
	}

/*************************************
 *
 *	Main CPU port handlers
 *
 *************************************/

	private IOReadPort readport() {
		IOReadPort ior = new IOReadPort();
		return ior;
	}

	private IOReadPort theglob_readport() {
		IOReadPort ior = new IOReadPort();
		ior.set( 0x00, 0xff, theglob_decrypt_rom );	/* Switch protection logic */
		return ior;
	}

	private IOWritePort writeport() {
		IOWritePort	iow = new IOWritePort();
		iow.set( 0x00, 0x00, interrupt_vector_w );	/* Pac-Man only */
		return iow;
	}

	private IOWritePort vanvan_writeport() {
		IOWritePort	iow = new IOWritePort();
		//iow.set( 0x01, 0x01, SN76496_0_w );
		//iow.set( 0x02, 0x02, SN76496_1_w );
		return iow;
	}

	private IOWritePort dremshpr_writeport() {
		IOWritePort	iow = new IOWritePort();
		iow.set( 0x06, 0x06, AY8910_write_port_0_w );
		iow.set( 0x07, 0x07, AY8910_control_port_0_w );
		return iow;
	}

/*************************************
 *
 *	Port definitions
 *
 *************************************/

	private InputPort[] ipt_pacman() {
		/* IN0 */
		in[0].setBit( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_4WAY );
		in[0].setBit( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_4WAY );
		in[0].setBit( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_4WAY );
		in[0].setBit( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_4WAY );
		//in[0].setBitX(    0x10, 0x10, IPT_DIPSWITCH_NAME | IPF_CHEAT, "Rack Test", KEYCODE_F1, IP_JOY_NONE )
		in[0].setDipName( 0x10, 0x10, "Rack Test" );
		in[0].setDipSetting(    0x10, DEF_STR[ Off ] );
		in[0].setDipSetting(    0x00, DEF_STR[ On ] );
		in[0].setBit( 0x20, IP_ACTIVE_LOW, IPT_COIN1 );
		in[0].setBit( 0x40, IP_ACTIVE_LOW, IPT_COIN2 );
		in[0].setBit( 0x80, IP_ACTIVE_LOW, IPT_COIN3 );

		/* IN1 */
		in[1].setBit( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_4WAY | IPF_COCKTAIL );
		in[1].setBit( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_4WAY | IPF_COCKTAIL );
		in[1].setBit( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_4WAY | IPF_COCKTAIL );
		in[1].setBit( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_4WAY | IPF_COCKTAIL );
		in[1].setService( 0x10, IP_ACTIVE_LOW );
		in[1].setBit( 0x20, IP_ACTIVE_LOW, IPT_START1 );
		in[1].setBit( 0x40, IP_ACTIVE_LOW, IPT_START2 );
		in[1].setDipName(0x80, 0x80, DEF_STR[ Cabinet ] );
		in[1].setDipSetting(   0x80, DEF_STR[ Upright ] );
		in[1].setDipSetting(   0x00, DEF_STR[ Cocktail ] );

		/* DSW 1 */
		in[2].setDipName( 0x03, 0x01, DEF_STR[ Coinage ] );
		in[2].setDipSetting(    0x03, DEF_STR[ _2C_1C ] );
		in[2].setDipSetting(    0x01, DEF_STR[ _1C_1C ] );
		in[2].setDipSetting(    0x02, DEF_STR[ _1C_2C ] );
		in[2].setDipSetting(    0x00, DEF_STR[ Free_Play ] );
		in[2].setDipName( 0x0c, 0x08, DEF_STR[ Lives ] );
		in[2].setDipSetting(    0x00, "1" );
		in[2].setDipSetting(    0x04, "2" );
		in[2].setDipSetting(    0x08, "3" );
		in[2].setDipSetting(    0x0c, "5" );
		in[2].setDipName( 0x30, 0x00, DEF_STR[ Bonus_Life ] );
		in[2].setDipSetting(    0x00, "10000" );
		in[2].setDipSetting(    0x10, "15000" );
		in[2].setDipSetting(    0x20, "20000" );
		in[2].setDipSetting(    0x30, "None" );
		in[2].setDipName( 0x40, 0x40, DEF_STR[ Difficulty ] );
		in[2].setDipSetting(    0x40, "Normal" );
		in[2].setDipSetting(    0x00, "Hard" );
		in[2].setDipName( 0x80, 0x80, "Ghost Names" );
		in[2].setDipSetting(    0x80, "Normal" );
		in[2].setDipSetting(    0x00, "Alternate" );

		/* DSW 2 */
		in[3].setBit( 0xff, IP_ACTIVE_HIGH, IPT_UNUSED );

		/* FAKE */
		/* This fake input port is used to get the status of the fire button */
		/* and activate the speedup cheat if it is. */
		//in[0].setBitX(    0x01, 0x00, IPT_DIPSWITCH_NAME | IPF_CHEAT, "Speedup Cheat", KEYCODE_LCONTROL, JOYCODE_1_BUTTON1 )
		//in[0].setDipSetting(    0x00, DEF_STR[ Off ] )
		//in[0].setDipSetting(    0x01, DEF_STR[ On ] )
		return in;
	}

	private InputPort[] ipt_mspacman() {
		/* IN0 */
		in[0].setBit( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_4WAY );
		in[0].setBit( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_4WAY );
		in[0].setBit( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_4WAY );
		in[0].setBit( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_4WAY );
		//in[0].setBitX(    0x10, 0x10, IPT_DIPSWITCH_NAME | IPF_CHEAT, "Rack Test", KEYCODE_F1, IP_JOY_NONE );
		in[0].setDipName( 0x10, 0x10, "Rack Test" );
		in[0].setDipSetting(    0x10, DEF_STR[ Off ] );
		in[0].setDipSetting(    0x00, DEF_STR[ On ] );
		in[0].setBit( 0x20, IP_ACTIVE_LOW, IPT_COIN1 );
		in[0].setBit( 0x40, IP_ACTIVE_LOW, IPT_COIN2 );
		in[0].setBit( 0x80, IP_ACTIVE_LOW, IPT_COIN3 );

		/* IN1 */
		in[1].setBit( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_4WAY | IPF_COCKTAIL );
		in[1].setBit( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_4WAY | IPF_COCKTAIL );
		in[1].setBit( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_4WAY | IPF_COCKTAIL );
		in[1].setBit( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_4WAY | IPF_COCKTAIL );
		in[1].setService( 0x10, IP_ACTIVE_LOW );
		in[1].setBit( 0x20, IP_ACTIVE_LOW, IPT_START1 );
		in[1].setBit( 0x40, IP_ACTIVE_LOW, IPT_START2 );
		in[1].setDipName( 0x80, 0x80, DEF_STR[ Cabinet ] );
		in[1].setDipSetting(    0x80, DEF_STR[ Upright ] );
		in[1].setDipSetting(    0x00, DEF_STR[ Cocktail ] );

		/* DSW 1 */
		in[2].setDipName( 0x03, 0x01, DEF_STR[ Coinage ] );
		in[2].setDipSetting(    0x03, DEF_STR[ _2C_1C ] );
		in[2].setDipSetting(    0x01, DEF_STR[ _1C_1C ] );
		in[2].setDipSetting(    0x02, DEF_STR[ _1C_2C ] );
		in[2].setDipSetting(    0x00, DEF_STR[ Free_Play ] );
		in[2].setDipName( 0x0c, 0x08, DEF_STR[ Lives ] );
		in[2].setDipSetting(    0x00, "1" );
		in[2].setDipSetting(    0x04, "2" );
		in[2].setDipSetting(    0x08, "3" );
		in[2].setDipSetting(    0x0c, "5" );
		in[2].setDipName( 0x30, 0x00, DEF_STR[ Bonus_Life ] );
		in[2].setDipSetting(    0x00, "10000" );
		in[2].setDipSetting(    0x10, "15000" );
		in[2].setDipSetting(    0x20, "20000" );
		in[2].setDipSetting(    0x30, "None" );
		in[2].setDipName( 0x40, 0x40, DEF_STR[ Difficulty ] );
		in[2].setDipSetting(    0x40, "Normal" );
		in[2].setDipSetting(    0x00, "Hard" );
		in[2].setBit( 0x80, IP_ACTIVE_LOW, IPT_UNUSED );

		/* DSW 2 */
		in[3].setBit( 0xff, IP_ACTIVE_HIGH, IPT_UNUSED );

		/* FAKE */
		/* This fake input port is used to get the status of the fire button */
		/* and activate the speedup cheat if it is. */
		//in[0].setBitX(    0x01, 0x00, IPT_DIPSWITCH_NAME | IPF_CHEAT, "Speedup Cheat", KEYCODE_LCONTROL, JOYCODE_1_BUTTON1 );
		//in[0].setDipSetting(    0x00, DEF_STR[ Off ] );
		//in[0].setDipSetting(    0x01, DEF_STR[ On ] );
		return in;
	}

	private InputPort[] ipt_mrtnt() {
		/* IN0 */
		in[0].setBit( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_4WAY );
		in[0].setBit( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_4WAY );
		in[0].setBit( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_4WAY );
		in[0].setBit( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_4WAY );
		in[0].setService( 0x10, IP_ACTIVE_LOW );
		in[0].setBit( 0x20, IP_ACTIVE_LOW, IPT_COIN1 );
		in[0].setBit( 0x40, IP_ACTIVE_LOW, IPT_TILT );
		in[0].setBit( 0x80, IP_ACTIVE_LOW, IPT_COIN2 );

		/* IN1 */
		in[1].setBit( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_4WAY | IPF_COCKTAIL );
		in[1].setBit( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_4WAY | IPF_COCKTAIL );
		in[1].setBit( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_4WAY | IPF_COCKTAIL );
		in[1].setBit( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_4WAY | IPF_COCKTAIL );
		in[1].setBit( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 );
		in[1].setBit( 0x20, IP_ACTIVE_LOW, IPT_START1 );
		in[1].setBit( 0x40, IP_ACTIVE_LOW, IPT_START2 );
		in[1].setBit( 0x80, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_COCKTAIL );

		/* DSW 1 */
		in[2].setDipName( 0x03, 0x03, DEF_STR[ Coinage ] );
		in[2].setDipSetting(    0x01, DEF_STR[ _2C_1C ] );
		in[2].setDipSetting(    0x03, DEF_STR[ _1C_1C ] );
		in[2].setDipSetting(    0x02, DEF_STR[ _1C_2C ] );
		in[2].setDipSetting(    0x00, DEF_STR[ Free_Play ] );
		in[2].setDipName( 0x0c, 0x08, DEF_STR[ Lives ] );
		in[2].setDipSetting(    0x0c, "2" );
		in[2].setDipSetting(    0x08, "3" );
		in[2].setDipSetting(    0x04, "4" );
		in[2].setDipSetting(    0x00, "5" );
		in[2].setDipName( 0x30, 0x30, DEF_STR[ Bonus_Life ] );
		in[2].setDipSetting(    0x30, "75000" );
		in[2].setDipSetting(    0x20, "100000" );
		in[2].setDipSetting(    0x10, "125000" );
		in[2].setDipSetting(    0x00, "150000" );
		in[2].setDipName( 0x40, 0x40, DEF_STR[ Cabinet ] );
		in[2].setDipSetting(    0x40, DEF_STR[ Upright ] );
		in[2].setDipSetting(    0x00, DEF_STR[ Cocktail ] );
		in[2].setDipName( 0x80, 0x80, DEF_STR[ Unknown ] );
		in[2].setDipSetting(    0x80, DEF_STR[ Off ] );
		in[2].setDipSetting(    0x00, DEF_STR[ On ] );

		/* DSW 2 */
		in[3].setBit( 0xff, IP_ACTIVE_HIGH, IPT_UNUSED );
		return in;
	}

	private InputPort[] ipt_lizwiz() {
		/* IN0 */
		in[0].setBit( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY );
		in[0].setBit( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY );
		in[0].setBit( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY );
		in[0].setBit( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY );
		in[0].setService( 0x10, IP_ACTIVE_LOW );
		in[0].setBit( 0x20, IP_ACTIVE_LOW, IPT_COIN1 );
		in[0].setBit( 0x40, IP_ACTIVE_LOW, IPT_TILT );
		in[0].setBit( 0x80, IP_ACTIVE_LOW, IPT_COIN2 );

		/* IN1 */
		in[1].setBit( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_PLAYER2 );
		in[1].setBit( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_PLAYER2 );
		in[1].setBit( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_PLAYER2 );
		in[1].setBit( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_PLAYER2 );
		in[1].setBit( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 );
		in[1].setBit( 0x20, IP_ACTIVE_LOW, IPT_START1 );
		in[1].setBit( 0x40, IP_ACTIVE_LOW, IPT_START2 );
		in[1].setBit( 0x80, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER2 );

		/* DSW 1 */
		in[2].setDipName( 0x03, 0x03, DEF_STR[ Coinage ] );
		in[2].setDipSetting(    0x01, DEF_STR[ _2C_1C ] );
		in[2].setDipSetting(    0x03, DEF_STR[ _1C_1C ] );
		in[2].setDipSetting(    0x02, DEF_STR[ _1C_2C ] );
		in[2].setDipSetting(    0x00, DEF_STR[ Free_Play ] );
		in[2].setDipName( 0x0c, 0x08, DEF_STR[ Lives ] );
		in[2].setDipSetting(    0x0c, "2" );
		in[2].setDipSetting(    0x08, "3" );
		in[2].setDipSetting(    0x04, "4" );
		in[2].setDipSetting(    0x00, "5" );
		in[2].setDipName( 0x30, 0x30, DEF_STR[ Bonus_Life ] );
		in[2].setDipSetting(    0x30, "75000" );
		in[2].setDipSetting(    0x20, "100000" );
		in[2].setDipSetting(    0x10, "125000" );
		in[2].setDipSetting(    0x00, "150000" );
		in[2].setDipName( 0x40, 0x40, DEF_STR[ Difficulty ] );
		in[2].setDipSetting(    0x40, "Normal" );
		in[2].setDipSetting(    0x00, "Hard" );
		in[2].setDipName( 0x80, 0x80, DEF_STR[ Unknown ] );
		in[2].setDipSetting(    0x80, DEF_STR[ Off ] );
		in[2].setDipSetting(    0x00, DEF_STR[ On ] );

		/* DSW 2 */
		in[3].setBit( 0xff, IP_ACTIVE_HIGH, IPT_UNUSED );
		return in;
	}

	private InputPort[] ipt_eyes() {
		/* IN0 */
		in[0].setBit( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_4WAY );
		in[0].setBit( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_4WAY );
		in[0].setBit( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_4WAY );
		in[0].setBit( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_4WAY );
		in[0].setService( 0x10, IP_ACTIVE_LOW );
		in[0].setBit( 0x20, IP_ACTIVE_LOW, IPT_COIN1 );
		in[0].setBit( 0x40, IP_ACTIVE_LOW, IPT_TILT );
		in[0].setBit( 0x80, IP_ACTIVE_LOW, IPT_COIN2 );

		/* IN1 */
		in[1].setBit( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_4WAY | IPF_COCKTAIL );
		in[1].setBit( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_4WAY | IPF_COCKTAIL );
		in[1].setBit( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_4WAY | IPF_COCKTAIL );
		in[1].setBit( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_4WAY | IPF_COCKTAIL );
		in[1].setBit( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 );
		in[1].setBit( 0x20, IP_ACTIVE_LOW, IPT_START1 );
		in[1].setBit( 0x40, IP_ACTIVE_LOW, IPT_START2 );
		//in[1].setBit( 0x80, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_COCKTAIL );

		/* DSW 1 */
		in[2].setDipName( 0x03, 0x03, DEF_STR[ Coinage ] );
		in[2].setDipSetting(    0x01, DEF_STR[ _2C_1C ] );
		in[2].setDipSetting(    0x03, DEF_STR[ _1C_1C ] );
		in[2].setDipSetting(    0x02, DEF_STR[ _1C_2C ] );
		in[2].setDipSetting(    0x00, DEF_STR[ Free_Play ] );
		in[2].setDipName( 0x0c, 0x08, DEF_STR[ Lives ] );
		in[2].setDipSetting(    0x0c, "2" );
		in[2].setDipSetting(    0x08, "3" );
		in[2].setDipSetting(    0x04, "4" );
		in[2].setDipSetting(    0x00, "5" );
		in[2].setDipName( 0x30, 0x30, DEF_STR[ Bonus_Life ] );
		in[2].setDipSetting(    0x30, "50000" );
		in[2].setDipSetting(    0x20, "75000" );
		in[2].setDipSetting(    0x10, "100000" );
		in[2].setDipSetting(    0x00, "125000" );
		in[2].setDipName( 0x40, 0x40, DEF_STR[ Cabinet ] );
		in[2].setDipSetting(    0x40, DEF_STR[ Upright ] );
		in[2].setDipSetting(    0x00, DEF_STR[ Cocktail ] );
		in[2].setDipName( 0x80, 0x80, DEF_STR[ Unknown ] );  /* Not accessed */
		in[2].setDipSetting(    0x80, DEF_STR[ Off ] );
		in[2].setDipSetting(    0x00, DEF_STR[ On ] );

		/* DSW 2 */
		in[3].setBit( 0xff, IP_ACTIVE_HIGH, IPT_UNUSED );
		return in;
	}

	private InputPort[] ipt_ponpoko() {
		/* IN0 */
		in[0].setBit( 0x01, IP_ACTIVE_HIGH, IPT_JOYSTICK_UP    | IPF_8WAY );
		in[0].setBit( 0x02, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT  | IPF_8WAY );
		in[0].setBit( 0x04, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_8WAY );
		in[0].setBit( 0x08, IP_ACTIVE_HIGH, IPT_JOYSTICK_DOWN  | IPF_8WAY );
		in[0].setBit( 0x10, IP_ACTIVE_HIGH, IPT_BUTTON1 );
		in[0].setBit( 0x20, IP_ACTIVE_LOW, IPT_COIN1 );
		in[0].setBit( 0x40, IP_ACTIVE_LOW, IPT_COIN2 );
		in[0].setBit( 0x80, IP_ACTIVE_LOW, IPT_COIN3 );

		/* The 2nd player controls are used even in upright mode */
		/* IN1 */
		in[1].setBit( 0x01, IP_ACTIVE_HIGH, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_PLAYER2 );
		in[1].setBit( 0x02, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_PLAYER2 );
		in[1].setBit( 0x04, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_PLAYER2 );
		in[1].setBit( 0x08, IP_ACTIVE_HIGH, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_PLAYER2 );
		in[1].setBit( 0x10, IP_ACTIVE_HIGH, IPT_BUTTON1 | IPF_PLAYER2 );
		in[1].setBit( 0x20, IP_ACTIVE_HIGH, IPT_START1 );
		in[1].setBit( 0x40, IP_ACTIVE_HIGH, IPT_START2 );
		in[1].setBit( 0x80, IP_ACTIVE_HIGH, IPT_UNUSED );

		/* DSW 1 */
		in[2].setDipName( 0x03, 0x01, DEF_STR[ Bonus_Life ] );
		in[2].setDipSetting(    0x01, "10000" );
		in[2].setDipSetting(    0x02, "30000" );
		in[2].setDipSetting(    0x03, "50000" );
		in[2].setDipSetting(    0x00, "None" );
		in[2].setDipName( 0x0c, 0x00, DEF_STR[ Unknown ] );
		in[2].setDipSetting(    0x00, "0" );
		in[2].setDipSetting(    0x04, "1" );
		in[2].setDipSetting(    0x08, "2" );
		in[2].setDipSetting(    0x0c, "3" );
		in[2].setDipName( 0x30, 0x20, DEF_STR[ Lives ] );
		in[2].setDipSetting(    0x00, "2" );
		in[2].setDipSetting(    0x10, "3" );
		in[2].setDipSetting(    0x20, "4" );
		in[2].setDipSetting(    0x30, "5" );
		in[2].setDipName( 0x40, 0x40, DEF_STR[ Cabinet ] );
		in[2].setDipSetting(    0x40, DEF_STR[ Upright ] );
		in[2].setDipSetting(    0x00, DEF_STR[ Cocktail ] );
		in[2].setDipName( 0x80, 0x80, DEF_STR[ Unknown ] );
		in[2].setDipSetting(    0x80, DEF_STR[ Off ] );
		in[2].setDipSetting(    0x00, DEF_STR[ On ] );

		/* DSW 2 */
		in[3].setDipName( 0x0f, 0x01, DEF_STR[ Coinage ] );
		in[3].setDipSetting(    0x04, "A 3/1 B 3/1" );
		in[3].setDipSetting(    0x0e, "A 3/1 B 1/2" );
		in[3].setDipSetting(    0x0f, "A 3/1 B 1/4" );
		in[3].setDipSetting(    0x02, "A 2/1 B 2/1" );
		in[3].setDipSetting(    0x0d, "A 2/1 B 1/1" );
		in[3].setDipSetting(    0x07, "A 2/1 B 1/3" );
		in[3].setDipSetting(    0x0b, "A 2/1 B 1/5" );
		in[3].setDipSetting(    0x0c, "A 2/1 B 1/6" );
		in[3].setDipSetting(    0x01, "A 1/1 B 1/1" );
		in[3].setDipSetting(    0x06, "A 1/1 B 4/5" );
		in[3].setDipSetting(    0x05, "A 1/1 B 2/3" );
		in[3].setDipSetting(    0x0a, "A 1/1 B 1/3" );
		in[3].setDipSetting(    0x08, "A 1/1 B 1/5" );
		in[3].setDipSetting(    0x09, "A 1/1 B 1/6" );
		in[3].setDipSetting(    0x03, "A 1/2 B 1/2" );
		in[3].setDipSetting(    0x00, DEF_STR[ Free_Play ] );
		in[3].setDipName( 0x10, 0x10, DEF_STR[ Unknown ] );  /* Most likely unused */
		in[3].setDipSetting(    0x10, DEF_STR[ Off ] );
		in[3].setDipSetting(    0x00, DEF_STR[ On ] );
		in[3].setDipName( 0x20, 0x20, DEF_STR[ Unknown ] );  /* Most likely unused */
		in[3].setDipSetting(    0x20, DEF_STR[ Off ] );
		in[3].setDipSetting(    0x00, DEF_STR[ On ] );
		in[3].setDipName( 0x40, 0x00, DEF_STR[ Demo_Sounds ] );
		in[3].setDipSetting(    0x40, DEF_STR[ Off ] );
		in[3].setDipSetting(    0x00, DEF_STR[ On ] );
		in[3].setDipName( 0x80, 0x80, DEF_STR[ Unknown ] );  /* Most likely unused */
		in[3].setDipSetting(    0x80, DEF_STR[ Off ] );
		in[3].setDipSetting(    0x00, DEF_STR[ On ] );
		return in;
	}

	private InputPort[] ipt_theglob() {
		/* IN0 */
		in[0].setBit( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_4WAY );
		in[0].setBit( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_4WAY );
		in[0].setBit( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_4WAY );
		in[0].setBit( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_4WAY );
		in[0].setBit( 0x10, IP_ACTIVE_LOW, IPT_UNKNOWN );
		in[0].setBit( 0x20, IP_ACTIVE_LOW, IPT_COIN1 );
		in[0].setBit( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		in[0].setBit( 0x80, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_COCKTAIL );

		/* IN1 */
		in[1].setBit( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_4WAY | IPF_COCKTAIL );
		in[1].setBit( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_4WAY | IPF_COCKTAIL );
		in[1].setBit( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_4WAY | IPF_COCKTAIL );
		in[1].setBit( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_4WAY | IPF_COCKTAIL );
		in[1].setBit( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_COCKTAIL );
		in[1].setBit( 0x20, IP_ACTIVE_LOW, IPT_START1 );
		in[1].setBit( 0x20, IP_ACTIVE_LOW, IPT_BUTTON1 );
		in[1].setBit( 0x40, IP_ACTIVE_LOW, IPT_START2 );
		in[1].setBit( 0x40, IP_ACTIVE_LOW, IPT_BUTTON2 );
		in[1].setDipName( 0x80, 0x80, DEF_STR[ Cabinet ] );
		in[1].setDipSetting(    0x80, DEF_STR[ Upright ] );
		in[1].setDipSetting(    0x00, DEF_STR[ Cocktail ] );

		/* DSW 1 */
		in[2].setDipName( 0x03, 0x03, DEF_STR[ Lives ] );
		in[2].setDipSetting(    0x03, "3" );
		in[2].setDipSetting(    0x02, "4" );
		in[2].setDipSetting(    0x01, "5" );
		in[2].setDipSetting(    0x00, "6" );
		in[2].setDipName( 0x1c, 0x1c, DEF_STR[ Difficulty ] );
		in[2].setDipSetting(    0x1c, "Easiest" );
		in[2].setDipSetting(    0x18, "Very Easy" );
		in[2].setDipSetting(    0x14, "Easy" );
		in[2].setDipSetting(    0x10, "Normal" );
		in[2].setDipSetting(    0x0c, "Difficult" );
		in[2].setDipSetting(    0x08, "Very Difficult" );
		in[2].setDipSetting(    0x04, "Very Hard" );
		in[2].setDipSetting(    0x00, "Hardest" );
		in[2].setDipName( 0x20, 0x00, DEF_STR[ Demo_Sounds ] );
		in[2].setDipSetting(    0x20, DEF_STR[ Off ] );
		in[2].setDipSetting(    0x00, DEF_STR[ On ] );
		in[2].setDipName( 0x40, 0x40, DEF_STR[ Unknown ] );
		in[2].setDipSetting(    0x40, DEF_STR[ Off ] );
		in[2].setDipSetting(    0x00, DEF_STR[ On ] );
		in[2].setDipName( 0x80, 0x80, DEF_STR[ Unknown ] );
		in[2].setDipSetting(    0x80, DEF_STR[ Off ] );
		in[2].setDipSetting(    0x00, DEF_STR[ On ] );

		/* DSW 2 */
		in[3].setBit( 0xff, IP_ACTIVE_HIGH, IPT_UNUSED );
		return in;
	}

	private InputPort[] ipt_vanvan() {
		/* IN0 */
		in[0].setBit( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_4WAY );
		in[0].setBit( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_4WAY );
		in[0].setBit( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_4WAY );
		in[0].setBit( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_4WAY );
		in[0].setBit( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 );
		in[0].setBit( 0x20, IP_ACTIVE_LOW, IPT_COIN1 );
		in[0].setBit( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		in[0].setBit( 0x80, IP_ACTIVE_LOW, IPT_COIN2 );

		/* The 2nd player controls are used even in upright mode */
		/* IN1 */
		in[1].setBit( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_4WAY | IPF_COCKTAIL );
		in[1].setBit( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_4WAY | IPF_COCKTAIL );
		in[1].setBit( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_4WAY | IPF_COCKTAIL );
		in[1].setBit( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_4WAY | IPF_COCKTAIL );
		in[1].setBit( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_COCKTAIL );
		in[1].setBit( 0x20, IP_ACTIVE_LOW, IPT_START1 );
		in[1].setBit( 0x40, IP_ACTIVE_LOW, IPT_START2 );
		in[1].setBit( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );

		/* DSW 1 */
		in[2].setDipName( 0x01, 0x00, DEF_STR[ Cabinet ] );
		in[2].setDipSetting(    0x00, DEF_STR[ Upright ] );
		in[2].setDipSetting(    0x01, DEF_STR[ Cocktail ] );
		in[2].setDipName( 0x02, 0x02, DEF_STR[ Flip_Screen ] );
		in[2].setDipSetting(    0x02, DEF_STR[ Off ] );
		in[2].setDipSetting(    0x00, DEF_STR[ On ] );
		in[2].setDipName( 0x04, 0x04, DEF_STR[ Unknown ] );
		in[2].setDipSetting(    0x04, DEF_STR[ Off ] );
		in[2].setDipSetting(    0x00, DEF_STR[ On ] );
		in[2].setDipName( 0x08, 0x08, DEF_STR[ Unknown ] );
		in[2].setDipSetting(    0x08, DEF_STR[ Off ] );
		in[2].setDipSetting(    0x00, DEF_STR[ On ] );
		in[2].setDipName( 0x30, 0x30, DEF_STR[ Lives ] );
		in[2].setDipSetting(    0x30, "3" );
		in[2].setDipSetting(    0x20, "4" );
		in[2].setDipSetting(    0x10, "5" );
		in[2].setDipSetting(    0x00, "6" );
		in[2].setDipName( 0x40, 0x40, DEF_STR[ Coin_A ] );
		in[2].setDipSetting(    0x00, DEF_STR[ _2C_1C ] );
		in[2].setDipSetting(    0x40, DEF_STR[ _1C_1C ] );
		in[2].setDipName( 0x80, 0x80, DEF_STR[ Coin_B ] );
		in[2].setDipSetting(    0x80, DEF_STR[ _1C_2C ] );
		in[2].setDipSetting(    0x00, DEF_STR[ _1C_3C ] );

		/* DSW 2 */
		in[3].setDipName( 0x01, 0x00, DEF_STR[ Unknown ] );
		in[3].setDipSetting(    0x00, DEF_STR[ Off ] );
		in[3].setDipSetting(    0x01, DEF_STR[ On ] );
		//in[0].setBitX(    0x02, 0x00, IPT_DIPSWITCH_NAME | IPF_CHEAT, "Invulnerability", KEYCODE_F1, IP_JOY_NONE );
		in[3].setDipSetting(    0x00, DEF_STR[ Off ] );
		in[3].setDipSetting(    0x02, DEF_STR[ On ] );
		in[3].setDipName( 0x04, 0x00, DEF_STR[ Unknown ] );
		in[3].setDipSetting(    0x00, DEF_STR[ Off ] );
		in[3].setDipSetting(    0x04, DEF_STR[ On ] );
		in[3].setDipName( 0x08, 0x00, DEF_STR[ Unknown ] );
		in[3].setDipSetting(    0x00, DEF_STR[ Off ] );
		in[3].setDipSetting(    0x08, DEF_STR[ On ] );
		in[3].setDipName( 0x10, 0x00, DEF_STR[ Unknown ] );
		in[3].setDipSetting(    0x00, DEF_STR[ Off ] );
		in[3].setDipSetting(    0x10, DEF_STR[ On ] );
		in[3].setDipName( 0x20, 0x00, DEF_STR[ Unknown ] );
		in[3].setDipSetting(    0x00, DEF_STR[ Off ] );
		in[3].setDipSetting(    0x20, DEF_STR[ On ] );
		in[3].setDipName( 0x40, 0x00, DEF_STR[ Unknown ] );
		in[3].setDipSetting(    0x00, DEF_STR[ Off ] );
		in[3].setDipSetting(    0x40, DEF_STR[ On ] );
		in[3].setDipName( 0x80, 0x00, DEF_STR[ Unknown ] );
		in[3].setDipSetting(    0x00, DEF_STR[ Off ] );
		in[3].setDipSetting(    0x80, DEF_STR[ On ] );
		return in;
	}

	private InputPort[] ipt_dremshpr() {
		/* IN0 */
		in[0].setBit( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_4WAY );
		in[0].setBit( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_4WAY );
		in[0].setBit( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_4WAY );
		in[0].setBit( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_4WAY );
		in[0].setBit( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 );
		in[0].setBit( 0x20, IP_ACTIVE_LOW, IPT_COIN1 );
		in[0].setBit( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		in[0].setBit( 0x80, IP_ACTIVE_LOW, IPT_COIN2 );

			/* IN1 */
		in[1].setBit( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_4WAY | IPF_COCKTAIL );
		in[1].setBit( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_4WAY | IPF_COCKTAIL );
		in[1].setBit( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_4WAY | IPF_COCKTAIL );
		in[1].setBit( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_4WAY | IPF_COCKTAIL );
		in[1].setBit( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_COCKTAIL );
		in[1].setBit( 0x20, IP_ACTIVE_LOW, IPT_START1 );
		in[1].setBit( 0x40, IP_ACTIVE_LOW, IPT_START2 );
		in[1].setBit( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );

			/* DSW _1 */
		in[2].setDipName( 0x01, 0x01, DEF_STR[ Cabinet ] );
		in[2].setDipSetting(    0x01, DEF_STR[ Upright ] );
		in[2].setDipSetting(    0x00, DEF_STR[ Cocktail ] );
		in[2].setDipName( 0x02, 0x02, DEF_STR[ Flip_Screen ] );
		in[2].setDipSetting(    0x02, DEF_STR[ Off ] );
		in[2].setDipSetting(    0x00, DEF_STR[ On ] );
		in[2].setDipName( 0x0c, 0x0c, DEF_STR[ Bonus_Life ] );
		in[2].setDipSetting(    0x08, "30000" );
		in[2].setDipSetting(    0x04, "50000" );
		in[2].setDipSetting(    0x00, "70000" );
		in[2].setDipSetting(    0x0c, "None" );
		in[2].setDipName( 0x30, 0x30, DEF_STR[ Lives ] );
		in[2].setDipSetting(    0x30, "3" );
		in[2].setDipSetting(    0x20, "4" );
		in[2].setDipSetting(    0x10, "5" );
		in[2].setDipSetting(    0x00, "6" );
		in[2].setDipName( 0xc0, 0xc0, DEF_STR[ Coinage ] );
		in[2].setDipSetting(    0x00, DEF_STR[ _2C_1C ] );
		in[2].setDipSetting(    0xc0, DEF_STR[ _1C_1C ] );
		in[2].setDipSetting(    0x80, DEF_STR[ _1C_2C ] );
		in[2].setDipSetting(    0x40, DEF_STR[ _1C_3C ] );

			/* DSW _2 */
	  //in[3].setBitX(    0x01, 0x00, IPT_DIPSWITCH_NAME | IPF_CHEAT, "Invulnerability", IP_KEY_NONE, IP_JOY_NONE )
	  //in[3].setDipSetting(    0x00, DEF_STR[ Off ] );		/* turning this on crashes puts the */
	  //in[3].setDipSetting(    0x01, DEF_STR[ On ] );       /* emulated machine in an infinite loop once in a while */
	//	in[3].setDipName( 0xff, 0x00, DEF_STR[ Unused ] );
		in[3].setBit( 0xfe, IP_ACTIVE_LOW, IPT_UNUSED );
		return in;
	}

	private GfxLayout charlayout() {

		int[] pOffs = { 0, 4 };
		int[] xOffs = { 8*8+0, 8*8+1, 8*8+2, 8*8+3, 0, 1, 2, 3 };
		int[] yOffs = { 0*8, 1*8, 2*8, 3*8, 4*8, 5*8, 6*8, 7*8 };

		return new GfxLayout(
			8,8,	/* 8*8 characters */
			256,	/* 512 characters */
			2,		/* 2 bits per pixel */
			pOffs,
			xOffs,
			yOffs,
			16*8	/* every char takes 16 consecutive bytes */
		);
	}

	private GfxLayout spritelayout() {

		int[] pOffs = { 0, 4 };
		int[] xOffs = { 8*8, 8*8+1, 8*8+2, 8*8+3, 16*8+0, 16*8+1, 16*8+2, 16*8+3,
			24*8+0, 24*8+1, 24*8+2, 24*8+3, 0, 1, 2, 3 };
		int[] yOffs = { 0*8, 1*8, 2*8, 3*8, 4*8, 5*8, 6*8, 7*8,
			32*8, 33*8, 34*8, 35*8, 36*8, 37*8, 38*8, 39*8 };

		return new GfxLayout(
			16,16,	/* 16*8 sprites */
			64,	/* 256 sprites */
			2,	/* 4 bits per pixel */
			pOffs,
			xOffs,
			yOffs,
			64*8	/* every char takes 64 consecutive bytes */
		);
	}


	private GfxDecodeInfo[] gfxdecodeinfo() {
		GfxDecodeInfo gdi[] = new GfxDecodeInfo[2];
		gdi[0] = new GfxDecodeInfo( REGION_GFX1, 0x0000, charlayout(),   0, 32 );
		gdi[1] = new GfxDecodeInfo( REGION_GFX2, 0x0000, spritelayout(), 0, 32 );
		return gdi;
	}

	public MachineDriver machine_driver_pacman() {

		CpuDriver[] cpuDriver = new CpuDriver[1];
		SoundChipEmulator[] soundChip = new SoundChipEmulator[1];

		cpuDriver[0] = new CpuDriver( (Cpu) new Z80(),
										18432000/6,	/* 3.072 Mhz */
										readmem(), writemem(), readport(), writeport(),
										pacman_interrupt, 1 );

		soundChip[0] = (SoundChipEmulator)namco;

		int[] visibleArea = { 0*8, 36*8-1, 0*8, 28*8-1 };

		return new MachineDriver
		(
			cpuDriver,

			60, 2500,
			1,
			NOP,

			//video;
			36*8, 28*8, visibleArea,
			gfxdecodeinfo(),
			16, 4*32,
			pacman_vh_convert_color_prom,

			VIDEO_TYPE_RASTER | VIDEO_SUPPORTS_DIRTY,

			noCallback,
			pacman_vh_start,
			generic_vh_stop,
			pengo_vh_screenrefresh,

			soundChip
		);
	}

	public MachineDriver machine_driver_theglob() {
		CpuDriver[] cpuDriver = new CpuDriver[1];
		SoundChipEmulator[] soundChip = new SoundChipEmulator[1];

		cpuDriver[0] = new CpuDriver( (Cpu) new Z80(),
										18432000/6,	/* 3.072 Mhz */
										theglob_readmem(), writemem(), theglob_readport(), writeport(),
										pacman_interrupt, 1 );

		soundChip[0] = (SoundChipEmulator)namco;

		int[] visibleArea = { 0*8, 36*8-1, 0*8, 28*8-1 };

		return new MachineDriver
		(
			cpuDriver,

			60, 2500,
			1,
			theglob_init_machine,

			//video;
			36*8, 28*8, visibleArea,
			gfxdecodeinfo(),
			16, 4*32,
			pacman_vh_convert_color_prom,

			VIDEO_TYPE_RASTER | VIDEO_SUPPORTS_DIRTY,

			noCallback,
			pacman_vh_start,
			generic_vh_stop,
			pengo_vh_screenrefresh,

			soundChip
		);
	}

	public MachineDriver machine_driver_vanvan() {
		CpuDriver[] cpuDriver = new CpuDriver[1];

		cpuDriver[0] = new CpuDriver( (Cpu) new Z80(),
										18432000/6,	/* 3.072 Mhz */
										vanvan_readmem(), vanvan_writemem(), readport(), vanvan_writeport(),
										nmi_interrupt, 1 );


		int[] visibleArea = { 0*8, 36*8-1, 0*8, 28*8-1 };

		return new MachineDriver
		(
			cpuDriver,

			60, 2500,
			1,
			NOP,

			//video;
			36*8, 28*8, visibleArea,
			gfxdecodeinfo(),
			16, 4*32,
			pacman_vh_convert_color_prom,

			VIDEO_TYPE_RASTER | VIDEO_SUPPORTS_DIRTY,

			noCallback,
			pacman_vh_start,
			generic_vh_stop,
			pengo_vh_screenrefresh,

			noSound
		);
	}
	public MachineDriver machine_driver_dremshpr() {
		CpuDriver[] cpuDriver = new CpuDriver[1];
		SoundChipEmulator[] soundChip = new SoundChipEmulator[1];

		cpuDriver[0] = new CpuDriver( (Cpu) new Z80(),
										18432000/6,	/* 3.072 Mhz */
										readmem(), writemem(), readport(), dremshpr_writeport(),
										nmi_interrupt, 1 );

		soundChip[0] = (SoundChipEmulator)ay8910;

		int[] visibleArea = { 0*8, 36*8-1, 0*8, 28*8-1 };

		return new MachineDriver
		(
			cpuDriver,

			60, 2500,
			1,
			NOP,

			//video;
			36*8, 28*8, visibleArea,
			gfxdecodeinfo(),
			16, 4*32,
			pacman_vh_convert_color_prom,

			VIDEO_TYPE_RASTER | VIDEO_SUPPORTS_DIRTY,

			noCallback,
			pacman_vh_start,
			generic_vh_stop,
			pengo_vh_screenrefresh,

			soundChip
		);
	}

	private boolean rom_pacman() {
		romLoader.setZip( "pacman" );

		romLoader.setMemory( REGION_CPU1 );
		romLoader.loadROM( "namcopac.6e",  0x0000, 0x1000, 0xfee263b3 );
		romLoader.loadROM( "namcopac.6f",  0x1000, 0x1000, 0x39d1fc83 );
		romLoader.loadROM( "namcopac.6h",  0x2000, 0x1000, 0x02083b03 );
		romLoader.loadROM( "namcopac.6j",  0x3000, 0x1000, 0x7a36fe55 );

		romLoader.setMemory( REGION_GFX1 );
		romLoader.loadROM( "pacman.5e",    0x0000, 0x1000, 0x0c944964 );

		romLoader.setMemory( REGION_GFX2 );
		romLoader.loadROM( "pacman.5f",    0x0000, 0x1000, 0x958fedf9 );

		romLoader.setMemory( REGION_PROMS );
		romLoader.loadROM( "82s123.7f",    0x0000, 0x0020, 0x2fc650bd );
		romLoader.loadROM( "82s126.4a",    0x0020, 0x0100, 0x3eb3a8e4 );

		//romLoader.setMemory( REGION_SOUND );	/* sound PROMs */
		//romLoader.loadROM( "82s126.1m",    0x0000, 0x0100, 0xa9cc86bf );
		//romLoader.loadROM( "82s126.3m",    0x0100, 0x0100, 0x77245b66 );	/* timing - not used */

		romLoader.loadZip(base_URL);

		return true;
	}

	private boolean rom_mspacman() {
		romLoader.setZip( "mspacman" );

		romLoader.setMemory( REGION_CPU1 );
		romLoader.loadROM( "boot1",        0x0000, 0x1000, 0xd16b31b7 );
		romLoader.loadROM( "boot2",        0x1000, 0x1000, 0x0d32de5e );
		romLoader.loadROM( "boot3",        0x2000, 0x1000, 0x1821ee0b );
		romLoader.loadROM( "boot4",        0x3000, 0x1000, 0x165a9dd8 );
		romLoader.loadROM( "boot5",        0x8000, 0x1000, 0x8c3e6de6 );
		romLoader.loadROM( "boot6",        0x9000, 0x1000, 0x368cb165 );

		romLoader.setMemory( REGION_GFX1 );
		romLoader.loadROM( "5e",           0x0000, 0x1000, 0x5c281d01 );

		romLoader.setMemory( REGION_GFX2 );
		romLoader.loadROM( "5f",           0x0000, 0x1000, 0x615af909 );

		romLoader.setMemory( REGION_PROMS );
		romLoader.loadROM( "82s123.7f",    0x0000, 0x0020, 0x2fc650bd );
		romLoader.loadROM( "82s126.4a",    0x0020, 0x0100, 0x3eb3a8e4 );

		//romLoader.setMemory( REGION_SOUND );	/* sound PROMs */
		//romLoader.loadROM( "82s126.1m",    0x0000, 0x0100, 0xa9cc86bf );
		//romLoader.loadROM( "82s126.3m",    0x0100, 0x0100, 0x77245b66 )	/* timing - not used */

		romLoader.loadZip(base_URL);

		return true;
	}

	private boolean rom_mrtnt() {
		romLoader.setZip( "mrtnt" );

		romLoader.setMemory( REGION_CPU1 );
		romLoader.loadROM( "tnt.1",        0x0000, 0x1000, 0x0e836586 );
		romLoader.loadROM( "tnt.2",        0x1000, 0x1000, 0x779c4c5b );
		romLoader.loadROM( "tnt.3",        0x2000, 0x1000, 0xad6fc688 );
		romLoader.loadROM( "tnt.4",        0x3000, 0x1000, 0xd77557b3 );

		romLoader.setMemory( REGION_GFX1 );
		romLoader.loadROM( "tnt.5",        0x0000, 0x1000, 0x3038cc0e );

		romLoader.setMemory( REGION_GFX2 );
		romLoader.loadROM( "tnt.6",        0x0000, 0x1000, 0x97634d8b );

		romLoader.setMemory( REGION_PROMS );
		romLoader.loadROM( "mrtnt08.bin",  0x0000, 0x0020, 0x00000000 );
		romLoader.loadROM( "mrtnt04.bin",  0x0020, 0x0100, 0x00000000 );

		//romLoader.setMemory( REGION_SOUND );	/* sound PROMs */
		//romLoader.loadROM( "82s126.1m",    0x0000, 0x0100, 0xa9cc86bf );
		//romLoader.loadROM( "82s126.3m"  ,  0x0100, 0x0100, 0x77245b66 )	/* timing - not used */

		romLoader.loadZip(base_URL);

		return true;
	}

	private boolean rom_lizwiz() {
		romLoader.setZip( "lizwiz" );

		romLoader.setMemory( REGION_CPU1 );
		romLoader.loadROM( "6e.cpu",       0x0000, 0x1000, 0x32bc1990 );
		romLoader.loadROM( "6f.cpu",       0x1000, 0x1000, 0xef24b414 );
		romLoader.loadROM( "6h.cpu",       0x2000, 0x1000, 0x30bed83d );
		romLoader.loadROM( "6j.cpu",       0x3000, 0x1000, 0xdd09baeb );
		romLoader.loadROM( "wiza",         0x8000, 0x1000, 0xf6dea3a6 );
		romLoader.loadROM( "wizb",         0x9000, 0x1000, 0xf27fb5a8 );

		romLoader.setMemory( REGION_GFX1 );
		romLoader.loadROM( "5e.cpu",       0x0000, 0x1000, 0x45059e73 );

		romLoader.setMemory( REGION_GFX2 );
		romLoader.loadROM( "5f.cpu",       0x0000, 0x1000, 0xd2469717 );

		romLoader.setMemory( REGION_PROMS );
		romLoader.loadROM( "7f.cpu",       0x0000, 0x0020, 0x7549a947 );
		romLoader.loadROM( "4a.cpu",       0x0020, 0x0100, 0x5fdca536 );

		//romLoader.setMemory( REGION_SOUND );	/* sound PROMs */
		//romLoader.loadROM( "82s126.1m",    0x0000, 0x0100, 0xa9cc86bf );
		//romLoader.loadROM( "82s126.3m"  ,  0x0100, 0x0100, 0x77245b66 )	/* timing - not used */

		romLoader.loadZip(base_URL);

		return true;
	}

	private boolean rom_ponpoko() {
		romLoader.setZip( "ponpoko" );

		romLoader.setMemory( REGION_CPU1 );
		romLoader.loadROM( "ppokoj1.bin",  0x0000, 0x1000, 0xffa3c004 );
		romLoader.loadROM( "ppokoj2.bin",  0x1000, 0x1000, 0x4a496866 );
		romLoader.loadROM( "ppokoj3.bin",  0x2000, 0x1000, 0x17da6ca3 );
		romLoader.loadROM( "ppokoj4.bin",  0x3000, 0x1000, 0x9d39a565 );
		romLoader.loadROM( "ppoko5.bin",   0x8000, 0x1000, 0x54ca3d7d );
		romLoader.loadROM( "ppoko6.bin",   0x9000, 0x1000, 0x3055c7e0 );
		romLoader.loadROM( "ppoko7.bin",   0xa000, 0x1000, 0x3cbe47ca );
		romLoader.loadROM( "ppokoj8.bin",  0xb000, 0x1000, 0x04b63fc6 );

		romLoader.setMemory( REGION_GFX1 );
		romLoader.loadROM( "ppoko9.bin",   0x0000, 0x1000, 0xb73e1a06 );

		romLoader.setMemory( REGION_GFX2 );
		romLoader.loadROM( "ppoko10.bin",  0x0000, 0x1000, 0x62069b5d );

		romLoader.setMemory( REGION_PROMS );
		romLoader.loadROM( "82s123.7f",    0x0000, 0x0020, 0x2fc650bd );
		romLoader.loadROM( "82s126.4a",    0x0020, 0x0100, 0x3eb3a8e4 );

		//romLoader.setMemory( REGION_SOUND );	/* sound PROMs */
		//romLoader.loadROM( "82s126.1m",    0x0000, 0x0100, 0xa9cc86bf );
		//romLoader.loadROM( "82s126.3m",    0x0100, 0x0100, 0x77245b66 )	/* timing - not used */

		romLoader.loadZip(base_URL);

		return true;
	}

	private boolean rom_eyes() {
		romLoader.setZip( "eyes" );

		romLoader.setMemory( REGION_CPU1 );
		romLoader.loadROM( "d7",           0x0000, 0x1000, 0x3b09ac89 );
		romLoader.loadROM( "e7",           0x1000, 0x1000, 0x97096855 );
		romLoader.loadROM( "f7",           0x2000, 0x1000, 0x731e294e );
		romLoader.loadROM( "h7",           0x3000, 0x1000, 0x22f7a719 );

		romLoader.setMemory( REGION_GFX1 );
		romLoader.loadROM( "d5",           0x0000, 0x1000, 0xd6af0030 );

		romLoader.setMemory( REGION_GFX2 );
		romLoader.loadROM( "e5",           0x0000, 0x1000, 0xa42b5201 );

		romLoader.setMemory( REGION_PROMS );
		romLoader.loadROM( "82s123.7f",    0x0000, 0x0020, 0x2fc650bd );
		romLoader.loadROM( "82s129.4a",    0x0020, 0x0100, 0xd8d78829 );
		romLoader.loadZip(base_URL);


		//romLoader.setMemory( REGION_SOUND );	/* sound PROMs */
		//romLoader.loadROM( "82s126.1m",    0x0000, 0x0100, 0xa9cc86bf );
		//romLoader.loadROM( "82s126.3m",    0x0100, 0x0100, 0x77245b66 )	/* timing - not used */

		romLoader.loadZip(base_URL);

		return true;
	}

	private boolean rom_theglob() {
		romLoader.setZip( "theglob" );

		romLoader.setMemory( REGION_CPU1 );
		romLoader.loadROM( "glob.u2",      0x0000, 0x2000, 0x829d0bea );
		romLoader.loadROM( "glob.u3",      0x2000, 0x2000, 0x31de6628 );

		romLoader.setMemory( REGION_GFX1 );
		romLoader.loadROM( "glob.5e",      0x0000, 0x1000, 0x53688260 );

		romLoader.setMemory( REGION_GFX2 );
		romLoader.loadROM( "glob.5f",      0x0000, 0x1000, 0x051f59c7 );

		romLoader.setMemory( REGION_PROMS );
		romLoader.loadROM( "glob.7f",      0x0000, 0x0020, 0x1f617527 );
		romLoader.loadROM( "glob.4a",      0x0020, 0x0100, 0x28faa769 );

		//romLoader.setMemory( REGION_SOUND );	/* sound PROMs */
		//romLoader.loadROM( "82s126.1m",    0x0000, 0x0100, 0xa9cc86bf );
		//romLoader.loadROM( "82s126.3m"  ,  0x0100, 0x0100, 0x77245b66 )	/* timing - not used */

		romLoader.loadZip(base_URL);

		return true;
	}

	private boolean rom_vanvan() {
		romLoader.setZip( "vanvan" );

		romLoader.setMemory( REGION_CPU1 );
		romLoader.loadROM( "van-1.50",     0x0000, 0x1000, 0xcf1b2df0 );
		romLoader.loadROM( "van-2.51",     0x1000, 0x1000, 0xdf58e1cb );
		romLoader.loadROM( "van-3.52",     0x2000, 0x1000, 0x15571e24 );
		romLoader.loadROM( "van-4.53",     0x3000, 0x1000, 0xb724cbe0 );
		romLoader.loadROM( "van-5.39",     0x8000, 0x1000, 0xdb67414c );

		romLoader.setMemory( REGION_GFX1 );
		romLoader.loadROM( "van-20.18",    0x0000, 0x1000, 0x60efbe66 );

		romLoader.setMemory( REGION_GFX2 );
		romLoader.loadROM( "van-21.19",    0x0000, 0x1000, 0x5dd53723 );

		romLoader.setMemory( REGION_PROMS );
		romLoader.loadROM( "6331-1.6",     0x0000, 0x0020, 0xce1d9503 );
		romLoader.loadROM( "6301-1.37",    0x0020, 0x0100, 0x4b803d9f );

		romLoader.loadZip(base_URL);

		return true;
	}

	private boolean rom_dremshpr() {
		romLoader.setZip( "dremshpr" );

		romLoader.setMemory( REGION_CPU1 );
		romLoader.loadROM( "red_1.50",	   0x0000, 0x1000, 0x830c6361 );
		romLoader.loadROM( "red_2.51",     0x1000, 0x1000, 0xd22551cc );
		romLoader.loadROM( "red_3.52",     0x2000, 0x1000, 0x0713a34a );
		romLoader.loadROM( "red_4.53",     0x3000, 0x1000, 0xf38bcaaa );
		romLoader.loadROM( "red_5.39",     0x8000, 0x1000, 0x6a382267 );
		romLoader.loadROM( "red_6.40",     0x9000, 0x1000, 0x4cf8b121 );
		romLoader.loadROM( "red_7.41",     0xa000, 0x1000, 0xbd4fc4ba );

		romLoader.setMemory( REGION_GFX1 );
		romLoader.loadROM( "red-20.18",    0x0000, 0x1000, 0x2d6698dc );

		romLoader.setMemory( REGION_GFX2 );
		romLoader.loadROM( "red-21.19",    0x0000, 0x1000, 0x38c9ce9b );

		romLoader.setMemory( REGION_PROMS );
		romLoader.loadROM( "6331-1.6",     0x0000, 0x0020, 0xce1d9503 );
		romLoader.loadROM( "6301-1.37",    0x0020, 0x0100, 0x39d6fb5c );

		romLoader.loadZip(base_URL);

		return true;
	}

	private void init_ponpoko()
	{
		int i, j;
		int temp;
		int[] ram;

		/* The gfx data is swapped wrt the other Pac-Man hardware games. */
		/* Here we revert it to the usual format. */

		/* Characters */
		ram = REGION_GFX1;
		for (i = 0;i < REGION_GFX1.length;i += 0x10)
		{
			for (j = 0; j < 8; j++)
			{
				temp          = ram[i+j+0x08];
				ram[i+j+0x08] = ram[i+j+0x00];
				ram[i+j+0x00] = temp;
			}
		}

		/* Sprites */
		ram = REGION_GFX2;
		for (i = 0;i < REGION_GFX2.length;i += 0x20)
		{
			for (j = 0; j < 8; j++)
			{
				temp          = ram[i+j+0x18];
				ram[i+j+0x18] = ram[i+j+0x10];
				ram[i+j+0x10] = ram[i+j+0x08];
				ram[i+j+0x08] = ram[i+j+0x00];
				ram[i+j+0x00] = temp;
			}
		}
	}

	private void eyes_decode(int i, int[] ram)
	{
		int j;
		int[] swapbuffer = new int[8];

		for (j = 0; j < 8; j++)
		{
			swapbuffer[j] = ram[i + (j >> 2) + (j & 2) + ((j & 1) << 2)];
		}

		for (j = 0; j < 8; j++)
		{
			int ch = swapbuffer[j];

			ram[i +j] = (ch & 0x80) | ((ch & 0x10) << 2) |
						 (ch & 0x20) | ((ch & 0x40) >> 2) | (ch & 0x0f);
		}
	}

	private void init_eyes()
	{
		int i;
		int[] ram;

		/* CPU ROMs */

		/* Data lines D3 and D5 swapped */
		ram = REGION_CPU1;
		for (i = 0; i < 0x4000; i++)
		{
			ram[i] =  (ram[i] & 0xc0) | ((ram[i] & 0x08) << 2) |
					  (ram[i] & 0x10) | ((ram[i] & 0x20) >> 2) | (ram[i] & 0x07);
		}


		/* Graphics ROMs */

		/* Data lines D4 and D6 and address lines A0 and A2 are swapped */
		ram = REGION_GFX1;
		for (i = 0;i < REGION_GFX1.length;i += 8)
			eyes_decode(i,ram);
		ram = REGION_GFX2;
		for (i = 0;i < REGION_GFX2.length;i += 8)
			eyes_decode(i,ram);
	}
}