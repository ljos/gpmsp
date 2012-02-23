/*
CottAGE - the Arcade Generic Emulator in Java

Java driver by Erik Duijs
*/

/***************************************************************************

	Namco PuckMan

    driver by Nicola Salmoria and many others

    Games supported:
		* Ms. Pac-Man

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

import jef.cpu.Z80;
import jef.cpuboard.CpuDriver;
import jef.machine.Machine;
import jef.machine.MachineDriver;
import jef.map.IOReadPort;
import jef.map.IOWritePort;
import jef.map.InputPort;
import jef.map.InterruptHandler;
import jef.map.MemoryReadAddressMap;
import jef.map.MemoryWriteAddressMap;
import jef.map.ReadHandler;
import jef.map.ReadMap;
import jef.map.VoidFunction;
import jef.map.WriteHandler;
import jef.map.WriteMap;
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

	InputPort[] in = new InputPort[4];

	ReadHandler		input_port_0_r;
	ReadHandler		input_port_1_r;
	ReadHandler		input_port_2_r;
	ReadHandler		input_port_3_r;

	cottage.vidhrdw.Pacman 	    v						= new cottage.vidhrdw.Pacman();
	WriteHandler 				videoram_w				= v.videoram_w(REGION_CPU1, v);
	WriteHandler				colorram_w				= videoram_w;
	Eof_callback				noCallback				= v;
	Vh_refresh 					pengo_vh_screenrefresh 	= v;
	Vh_start					pacman_vh_start			= v;
	Vh_stop						generic_vh_stop			= v;
	Vh_convert_color_proms 		pacman_vh_convert_color_prom = v;

	cottage.machine.Pacman m				= new cottage.machine.Pacman();
	InterruptHandler pacman_interrupt		= m.pacman_interrupt(m);
	InterruptHandler nmi_interrupt			= m.pacman_nmi_interrupt(m);
	WriteHandler	 interrupt_enable_w		= m.interrupt_enable_w(m);
	WriteHandler	 interrupt_vector_w		= m.interrupt_vector_w(m);

	ReadHandler		 theglob_decrypt_rom	= m.theglob_decrypt_rom(m);
	ReadHandler		 MRA_BANK1				= m.MRA_BANK1(m,REGION_CPU1);
	VoidFunction	 theglob_init_machine	= m.theglob_init_machine(m,REGION_CPU1);


	@Override
	public Machine getMachine(URL url, String name) {
		super.getMachine(url,name);
		super.setVideoEmulator(v);

		in[0] = new InputPort();
		in[1] = new InputPort();
		in[2] = new InputPort();
		in[3] = new InputPort();

		input_port_0_r = in[0];
		input_port_1_r = in[1];
		input_port_2_r = in[2];
		input_port_3_r = in[3];


		if (name.equals("mspacman")) {
			this.md = machine_driver_pacman();
			GAME(1981, rom_mspacman(), 	ipt_mspacman(), v.pacman(), ROT90, "Bootleg", "Ms. Pac-Man" );
		}

		v.setRegions(REGION_PROMS, REGION_CPU1);

		m.init(md);
		return m;
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
		mwa.set( 0x4400, 0x47ff, colorram_w );
		mwa.setMW( 0x4800, 0x4fef, MWA_RAM );
		mwa.setMW( 0x4ff0, 0x4fff, MWA_RAM );
		mwa.set( 0x5000, 0x5000, interrupt_enable_w );
		mwa.setMW( 0x5002, 0x5002, MWA_NOP );
		mwa.setMW( 0x5060, 0x506f, MWA_RAM );
		mwa.setMW( 0x8000, 0xbfff, MWA_ROM );	/* Ms. Pac-Man / Ponpoko only */
		mwa.set( 0xc000, 0xc3ff, videoram_w ); /* mirror address for video ram, */
		mwa.set( 0xc400, 0xc7ef, colorram_w ); /* used to display HIGH SCORE and CREDITS */
		mwa.setMW( 0xffff, 0xffff, MWA_NOP );	/* Eyes writes to this location to simplify code */
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

	private IOWritePort writeport() {
		IOWritePort	iow = new IOWritePort();
		iow.set( 0x00, 0x00, interrupt_vector_w );	/* Pac-Man only */
		return iow;
	}

/*************************************
 *
 *	Port definitions
 *
 *************************************/
	private InputPort[] ipt_mspacman() {
		/* IN0 */
		in[0].setBit( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_4WAY );
		in[0].setBit( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_4WAY );
		in[0].setBit( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_4WAY );
		in[0].setBit( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_4WAY );
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
		
		cpuDriver[0] = new CpuDriver( new Z80(),
										18432000/6,	/* 3.072 Mhz */
										readmem(), writemem(), readport(), writeport(),
										pacman_interrupt, 1 );


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
			pengo_vh_screenrefresh
		);
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

		romLoader.loadZip(base_URL);

		return true;
	}
}