/*
CottAGE - the Arcade Generic Emulator in Java

Java driver by Gollum
*/

/***************************************************************************

	Namco PuckMan

    driver by Nicola Salmoria and many others

    java driver by Gollum and Erik Duijs

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

import jef.machine.Machine;
import jef.map.InitHandler;
import jef.map.InterruptHandler;
import jef.map.WriteHandler;
import jef.video.Vh_convert_color_proms;
import jef.video.Vh_refresh;
import jef.video.Vh_start;

import cottage.mame.Driver;
import cottage.mame.MAMEConstants;
import cottage.mame.MAMEDriver;

public class Pacman_ extends MAMEDriver implements Driver, MAMEConstants {

cottage.vidhrdw.Pengo_ v = new cottage.vidhrdw.Pengo_();
Vh_start pacman_vs = v.video_start_pacman();
Vh_refresh pengo_vu = v.video_update_pengo();
Vh_convert_color_proms pacman_pi = v.palette_init_pacman();
WriteHandler videoram_w = v.videoram_w();
WriteHandler colorram_w = v.colorram_w();

jef.machine.BasicMachine m = new jef.machine.BasicMachine();
InterruptHandler irq0_line_hold = m.irq0_line_hold();

WriteHandler interrupt_vector_w = new Interrupt_vector_w();
WriteHandler interrupt_enable_w = m.interrupt_enable();
InterruptHandler pacman_interrupt = new Pacman_interrupt();
InterruptHandler pacplus_interrupt = new Pacplus_interrupt();
WriteHandler pacman_leds_w = new Pacman_leds_w();
WriteHandler pacman_coin_counter_w = new Pacman_coin_counter_w();

cottage.machine.Pacplus_ pacplus = new cottage.machine.Pacplus_(this);
cottage.machine.Jumpshot_ jumpshot = new cottage.machine.Jumpshot_(this);

public class Interrupt_vector_w implements WriteHandler {
	public void write(int address, int value) {
		m.cd[0].cpu.setProperty(0,value);
	}
}

static int speedcheat = 0;	/* a well known hack allows to make Pac Man run at four times */
							/* his usual speed. When we start the emulation, we check if the */
							/* hack can be applied, and set this flag accordingly. */

/*************************************
 *
 *	Machine init
 *
 *************************************/

public void MACHINE_INIT_pacman()
{
	int[] RAM = memory_region(REGION_CPU1);

	/* check if the loaded set of ROMs allows the Pac Man speed hack */
	if ((RAM[0x180b] == 0xbe && RAM[0x1ffd] == 0x00) ||
			(RAM[0x180b] == 0x01 && RAM[0x1ffd] == 0xbd))
		speedcheat = 1;
	else
		speedcheat = 0;
}

public void MACHINE_INIT_pacplus()
{
	int[] RAM = memory_region(REGION_CPU1);

	/* check if the loaded set of ROMs allows the Pac Man speed hack */
	if ((RAM[0x182d] == 0xbe && RAM[0x1ffd] == 0xff) ||
			(RAM[0x182d] == 0x01 && RAM[0x1ffd] == 0xbc))
		speedcheat = 1;
	else
		speedcheat = 0;
}

/*************************************
 *
 *	Interrupts
 *
 *************************************/

public class Pacman_interrupt implements InterruptHandler {
	public int irq() {
		int[] RAM = memory_region(REGION_CPU1);

		/* speed up cheat */
		if (speedcheat != 0)
		{
			if ((input_port_4_r.read(0) & 1) != 0)	/* check status of the fake dip switch */
			{
				/* activate the cheat */
				RAM[0x180b] = 0x01;
				RAM[0x1ffd] = 0xbd;
			}
			else
			{
				/* remove the cheat */
				RAM[0x180b] = 0xbe;
				RAM[0x1ffd] = 0x00;
			}
		}

		return m.irq0_line_hold().irq();
	}
}

public class Pacplus_interrupt implements InterruptHandler {
	public int irq() {
		int[] RAM = memory_region(REGION_CPU1);

		/* speed up cheat */
		if (speedcheat != 0)
		{
			if ((input_port_4_r.read(0) & 1) != 0)	/* check status of the fake dip switch */
			{
				/* activate the cheat */
				RAM[0x182d] = 0x01;
				RAM[0x1ffd] = 0xbc;
			}
			else
			{
				/* remove the cheat */
				RAM[0x182d] = 0xbe;
				RAM[0x1ffd] = 0xff;
			}
		}

		return m.irq0_line_hold().irq();
	}
}

public class Mspacman_interrupt implements InterruptHandler {
	public int irq() {
		int[] RAM = memory_region(REGION_CPU1);

		/* speed up cheat */
		if (speedcheat != 0)
		{
			if ((input_port_4_r.read(0) & 1) != 0)	/* check status of the fake dip switch */
			{
				/* activate the cheat */
				RAM[0x1180b] = 0x01;
				RAM[0x11ffd] = 0xbd;
			}
			else
			{
				/* remove the cheat */
				RAM[0x1180b] = 0xbe;
				RAM[0x11ffd] = 0x00;
			}
		}

		return m.irq0_line_hold().irq();
	}
}

/*************************************
 *
 *	LEDs/coin counters
 *
 *************************************/

public class Pacman_leds_w implements WriteHandler {
	public void write(int offset, int data) {
		//set_led_status(offset,data & 1);
	}
}


public class Pacman_coin_counter_w implements WriteHandler {
	public void write(int offset, int data) {
		//coin_counter_w(offset,data & 1);
	}
}

public class Pacman_coin_lockout_global_w implements WriteHandler {
	public void write(int offset, int data) {
		//coin_lockout_global_w(~data & 0x01);
	}
}

/*************************************
 *
 *	Main CPU memory handlers
 *
 *************************************/

private boolean readmem() {
	MR_START( 0x0000, 0x3fff, MRA_ROM );
	MR_ADD( 0x4000, 0x47ff, MRA_RAM );	/* video and color RAM */
	MR_ADD( 0x4c00, 0x4fff, MRA_RAM );	/* including sprite codes at 4ff0-4fff */
	MR_ADD( 0x5000, 0x503f, input_port_0_r );	/* IN0 */
	MR_ADD( 0x5040, 0x507f, input_port_1_r );	/* IN1 */
	MR_ADD( 0x5080, 0x50bf, input_port_2_r );	/* DSW1 */
	MR_ADD( 0x50c0, 0x50ff, input_port_3_r );	/* DSW2 */
	MR_ADD( 0x8000, 0xbfff, MRA_ROM );	/* Ms. Pac-Man / Ponpoko only */
	return true;
}

private boolean writemem() {
	MW_START( 0x0000, 0x3fff, MWA_ROM );
	MW_ADD( 0x4000, 0x43ff, videoram_w, videoram, videoram_size );
	MW_ADD( 0x4400, 0x47ff, colorram_w, colorram );
	MW_ADD( 0x4c00, 0x4fef, MWA_RAM );
	MW_ADD( 0x4ff0, 0x4fff, MWA_RAM, spriteram, spriteram_size );
	MW_ADD( 0x5000, 0x5000, interrupt_enable_w );
	//MW_ADD( 0x5001, 0x5001, pengo_sound_enable_w );
	MW_ADD( 0x5002, 0x5002, MWA_NOP );
	//MW_ADD( 0x5003, 0x5003, pengo_flipscreen_w );
 	MW_ADD( 0x5004, 0x5005, pacman_leds_w );
// 	MW_ADD( 0x5006, 0x5006, pacman_coin_lockout_global_w );	this breaks many games
 	MW_ADD( 0x5007, 0x5007, pacman_coin_counter_w );
	//MW_ADD( 0x5040, 0x505f, pengo_sound_w, pengo_soundregs );
	MW_ADD( 0x5060, 0x506f, MWA_RAM, spriteram_2 );
	//MW_ADD( 0x50c0, 0x50c0, watchdog_reset_w );
	MW_ADD( 0x8000, 0xbfff, MWA_ROM );	/* Ms. Pac-Man / Ponpoko only */
	MW_ADD( 0xc000, 0xc3ff, videoram_w ); /* mirror address for video ram, */
	MW_ADD( 0xc400, 0xc7ef, colorram_w ); /* used to display HIGH SCORE and CREDITS */
	MW_ADD( 0xffff, 0xffff, MWA_NOP );	/* Eyes writes to this location to simplify code */
	return true;
}

private boolean mspacman_readmem() {
	MR_START( 0x0000, 0x3fff, MRA_BANK1 );
	MR_ADD( 0x4000, 0x47ff, MRA_RAM );	/* video and color RAM */
	MR_ADD( 0x4c00, 0x4fff, MRA_RAM );	/* including sprite codes at 4ff0-4fff */
	MR_ADD( 0x5000, 0x503f, input_port_0_r );	/* IN0 */
	MR_ADD( 0x5040, 0x507f, input_port_1_r );	/* IN1 */
	MR_ADD( 0x5080, 0x50bf, input_port_2_r );	/* DSW1 */
	MR_ADD( 0x50c0, 0x50ff, input_port_3_r );	/* DSW2 */
	MR_ADD( 0x8000, 0xbfff, MRA_BANK1 );
	return true;
}

/*************************************
 *
 *	Main CPU port handlers
 *
 *************************************/

private boolean writeport() {
	PW_START( 0x00, 0x00, interrupt_vector_w );	/* Pac-Man only */
	return true;
}

/*************************************
 *
 *	Port definitions
 *
 *************************************/

private boolean ipt_pacman() {
	PORT_START();	/* IN0 */
	PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_4WAY );
	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_4WAY );
	PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_4WAY );
	PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_4WAY );
	//PORT_BITX(    0x10, 0x10, IPT_DIPSWITCH_NAME | IPF_CHEAT, "Rack Test", KEYCODE_F1, IP_JOY_NONE );
	PORT_DIPNAME( 0x10, 0x10, "Rack Test" );
	PORT_DIPSETTING(    0x10, DEF_STR2( Off ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( On ) );
	PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_COIN1 );
	PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_COIN2 );
	PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_SERVICE1 );

	PORT_START();	/* IN1 */
	PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_4WAY | IPF_COCKTAIL );
	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_4WAY | IPF_COCKTAIL );
	PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_4WAY | IPF_COCKTAIL );
	PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_4WAY | IPF_COCKTAIL );
	PORT_SERVICE( 0x10, IP_ACTIVE_LOW );
	PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_START1 );
	PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_START2 );
	PORT_DIPNAME(0x80, 0x80, DEF_STR2( Cabinet ) );
	PORT_DIPSETTING(   0x80, DEF_STR2( Upright ) );
	PORT_DIPSETTING(   0x00, DEF_STR2( Cocktail ) );

	PORT_START();	/* DSW 1 */
	PORT_DIPNAME( 0x03, 0x01, DEF_STR2( Coinage ) );
	PORT_DIPSETTING(    0x03, DEF_STR2( _2C_1C ) );
	PORT_DIPSETTING(    0x01, DEF_STR2( _1C_1C ) );
	PORT_DIPSETTING(    0x02, DEF_STR2( _1C_2C ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( Free_Play ) );
	PORT_DIPNAME( 0x0c, 0x08, DEF_STR2( Lives ) );
	PORT_DIPSETTING(    0x00, "1" );
	PORT_DIPSETTING(    0x04, "2" );
	PORT_DIPSETTING(    0x08, "3" );
	PORT_DIPSETTING(    0x0c, "5" );
	PORT_DIPNAME( 0x30, 0x00, DEF_STR2( Bonus_Life ) );
	PORT_DIPSETTING(    0x00, "10000" );
	PORT_DIPSETTING(    0x10, "15000" );
	PORT_DIPSETTING(    0x20, "20000" );
	PORT_DIPSETTING(    0x30, "None" );
	PORT_DIPNAME( 0x40, 0x40, DEF_STR2( Difficulty ) );
	PORT_DIPSETTING(    0x40, "Normal" );
	PORT_DIPSETTING(    0x00, "Hard" );
	PORT_DIPNAME( 0x80, 0x80, "Ghost Names" );
	PORT_DIPSETTING(    0x80, "Normal" );
	PORT_DIPSETTING(    0x00, "Alternate" );

	PORT_START();	/* DSW 2 */
	PORT_BIT( 0xff, IP_ACTIVE_HIGH, IPT_UNUSED );

	//PORT_START();	/* FAKE */
	/* This fake input port is used to get the status of the fire button */
	/* and activate the speedup cheat if it is. */
	//PORT_BITX(    0x01, 0x00, IPT_DIPSWITCH_NAME | IPF_CHEAT, "Speedup Cheat", KEYCODE_LCONTROL, JOYCODE_1_BUTTON1 );
	//PORT_DIPSETTING(    0x00, DEF_STR2( Off ) );
	//PORT_DIPSETTING(    0x01, DEF_STR2( On ) );
	return true;
}

/* Ms. Pac-Man input ports are identical to Pac-Man, the only difference is */
/* the missing Ghost Names dip switch. */
private boolean ipt_mspacman() {
	PORT_START();	/* IN0 */
	PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_4WAY );
	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_4WAY );
	PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_4WAY );
	PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_4WAY );
	//PORT_BITX(    0x10, 0x10, IPT_DIPSWITCH_NAME | IPF_CHEAT, "Rack Test", KEYCODE_F1, IP_JOY_NONE );
	PORT_DIPNAME( 0x10, 0x10, "Rack Test" );
	PORT_DIPSETTING(    0x10, DEF_STR2( Off ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( On ) );
	PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_COIN1 );
	PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_COIN2 );
	PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_SERVICE1 );

	PORT_START();	/* IN1 */
	PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_4WAY | IPF_COCKTAIL );
	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_4WAY | IPF_COCKTAIL );
	PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_4WAY | IPF_COCKTAIL );
	PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_4WAY | IPF_COCKTAIL );
	PORT_SERVICE( 0x10, IP_ACTIVE_LOW );
	PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_START1 );
	PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_START2 );
	PORT_DIPNAME( 0x80, 0x80, DEF_STR2( Cabinet ) );
	PORT_DIPSETTING(    0x80, DEF_STR2( Upright ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( Cocktail ) );

	PORT_START();	/* DSW 1 */
	PORT_DIPNAME( 0x03, 0x01, DEF_STR2( Coinage ) );
	PORT_DIPSETTING(    0x03, DEF_STR2( _2C_1C ) );
	PORT_DIPSETTING(    0x01, DEF_STR2( _1C_1C ) );
	PORT_DIPSETTING(    0x02, DEF_STR2( _1C_2C ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( Free_Play ) );
	PORT_DIPNAME( 0x0c, 0x08, DEF_STR2( Lives ) );
	PORT_DIPSETTING(    0x00, "1" );
	PORT_DIPSETTING(    0x04, "2" );
	PORT_DIPSETTING(    0x08, "3" );
	PORT_DIPSETTING(    0x0c, "5" );
	PORT_DIPNAME( 0x30, 0x00, DEF_STR2( Bonus_Life ) );
	PORT_DIPSETTING(    0x00, "10000" );
	PORT_DIPSETTING(    0x10, "15000" );
	PORT_DIPSETTING(    0x20, "20000" );
	PORT_DIPSETTING(    0x30, "None" );
	PORT_DIPNAME( 0x40, 0x40, DEF_STR2( Difficulty ) );
	PORT_DIPSETTING(    0x40, "Normal" );
	PORT_DIPSETTING(    0x00, "Hard" );
	PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNUSED );

	PORT_START();	/* DSW 2 */
	PORT_BIT( 0xff, IP_ACTIVE_HIGH, IPT_UNUSED );

	//PORT_START();	/* FAKE */
	/* This fake input port is used to get the status of the fire button */
	/* and activate the speedup cheat if it is. */
	//PORT_BITX(    0x01, 0x00, IPT_DIPSWITCH_NAME | IPF_CHEAT, "Speedup Cheat", KEYCODE_LCONTROL, JOYCODE_1_BUTTON1 );
	//PORT_DIPSETTING(    0x00, DEF_STR2( Off ) );
	//PORT_DIPSETTING(    0x01, DEF_STR2( On ) );
	return true;
}

/* Same as 'mspacman', but no fake input port */
private boolean ipt_mspacpls() {
	PORT_START();	/* IN0 */
	PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_4WAY );
	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_4WAY );
	PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_4WAY );
	PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_4WAY );
	//PORT_BITX(    0x10, 0x10, IPT_DIPSWITCH_NAME | IPF_CHEAT, "Rack Test", KEYCODE_F1, IP_JOY_NONE );
	PORT_DIPNAME( 0x10, 0x10, "Rack Test" );
	PORT_DIPSETTING(    0x10, DEF_STR2( Off ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( On ) );
	PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_COIN1 );
	PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_COIN2 );
	PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_SERVICE1 );

	PORT_START();	/* IN1 */
	PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_4WAY | IPF_COCKTAIL );
	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_4WAY | IPF_COCKTAIL );
	PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_4WAY | IPF_COCKTAIL );
	PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_4WAY | IPF_COCKTAIL );
	PORT_SERVICE( 0x10, IP_ACTIVE_LOW );
	PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_START1 );	// Also invincibility when playing
	PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_START2 );	// Also speed-up when playing
	PORT_DIPNAME( 0x80, 0x80, DEF_STR2( Cabinet ) );
	PORT_DIPSETTING(    0x80, DEF_STR2( Upright ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( Cocktail ) );

	PORT_START();	/* DSW 1 */
	PORT_DIPNAME( 0x03, 0x01, DEF_STR2( Coinage ) );
	PORT_DIPSETTING(    0x03, DEF_STR2( _2C_1C ) );
	PORT_DIPSETTING(    0x01, DEF_STR2( _1C_1C ) );
	PORT_DIPSETTING(    0x02, DEF_STR2( _1C_2C ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( Free_Play ) );
	PORT_DIPNAME( 0x0c, 0x08, DEF_STR2( Lives ) );
	PORT_DIPSETTING(    0x00, "1" );
	PORT_DIPSETTING(    0x04, "2" );
	PORT_DIPSETTING(    0x08, "3" );
	PORT_DIPSETTING(    0x0c, "5" );
	PORT_DIPNAME( 0x30, 0x00, DEF_STR2( Bonus_Life ) );
	PORT_DIPSETTING(    0x00, "10000" );
	PORT_DIPSETTING(    0x10, "15000" );
	PORT_DIPSETTING(    0x20, "20000" );
	PORT_DIPSETTING(    0x30, "None" );
	PORT_DIPNAME( 0x40, 0x40, DEF_STR2( Difficulty ) );
	PORT_DIPSETTING(    0x40, "Normal" );
	PORT_DIPSETTING(    0x00, "Hard" );
	PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNUSED );

	PORT_START();	/* DSW 2 */
	PORT_BIT( 0xff, IP_ACTIVE_HIGH, IPT_UNUSED );
	return true;
}

private boolean ipt_maketrax() {
	PORT_START();	/* IN0 */
	PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_4WAY );
	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_4WAY );
	PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_4WAY );
	PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_4WAY );
	PORT_DIPNAME( 0x10, 0x00, DEF_STR2( Cabinet ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( Upright ) );
	PORT_DIPSETTING(    0x10, DEF_STR2( Cocktail ) );
	PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_COIN1 );
	PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_COIN2 );
	PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_SERVICE1 );

	PORT_START();	/* IN1 */
	PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_4WAY | IPF_COCKTAIL );
	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_4WAY | IPF_COCKTAIL );
	PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_4WAY | IPF_COCKTAIL );
	PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_4WAY | IPF_COCKTAIL );
	PORT_BIT( 0x10, IP_ACTIVE_HIGH, IPT_UNUSED );  /* Protection */
	PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_START1 );
	PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_START2 );
	PORT_BIT( 0x80, IP_ACTIVE_HIGH, IPT_UNUSED );  /* Protection */

	PORT_START();	/* DSW 1 */
	PORT_DIPNAME( 0x03, 0x01, DEF_STR2( Coinage ) );
	PORT_DIPSETTING(    0x03, DEF_STR2( _2C_1C ) );
	PORT_DIPSETTING(    0x01, DEF_STR2( _1C_1C ) );
	PORT_DIPSETTING(    0x02, DEF_STR2( _1C_2C ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( Free_Play ) );
	PORT_DIPNAME( 0x0c, 0x00, DEF_STR2( Lives ) );
	PORT_DIPSETTING(    0x00, "3" );
	PORT_DIPSETTING(    0x04, "4" );
	PORT_DIPSETTING(    0x08, "5" );
	PORT_DIPSETTING(    0x0c, "6" );
	PORT_DIPNAME( 0x10, 0x10, "First Pattern" );
	PORT_DIPSETTING(    0x10, "Easy" );
	PORT_DIPSETTING(    0x00, "Hard" );
	PORT_DIPNAME( 0x20, 0x20, "Teleport Holes" );
	PORT_DIPSETTING(    0x20, DEF_STR2( Off ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( On ) );
 	PORT_BIT( 0xc0, IP_ACTIVE_HIGH, IPT_UNUSED );  /* Protection */

	PORT_START();	/* DSW 2 */
	PORT_BIT( 0xff, IP_ACTIVE_HIGH, IPT_UNUSED );
	return true;
}

private boolean ipt_mbrush() {
	PORT_START();	/* IN0 */
	PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_4WAY );
	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_4WAY );
	PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_4WAY );
	PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_4WAY );
	PORT_DIPNAME( 0x10, 0x00, DEF_STR2( Cabinet ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( Upright ) );
	PORT_DIPSETTING(    0x10, DEF_STR2( Cocktail ) );
	PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_COIN1 );
	PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_COIN2 );
	PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_SERVICE1 );

	PORT_START();	/* IN1 */
	PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_4WAY | IPF_COCKTAIL );
	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_4WAY | IPF_COCKTAIL );
	PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_4WAY | IPF_COCKTAIL );
	PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_4WAY | IPF_COCKTAIL );
	PORT_BIT( 0x10, IP_ACTIVE_HIGH, IPT_UNUSED );  /* Protection in Make Trax */
	PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_START1 );
	PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_START2 );
	PORT_BIT( 0x80, IP_ACTIVE_HIGH, IPT_UNUSED );  /* Protection in Make Trax */

	PORT_START();	/* DSW 1 */
	PORT_DIPNAME( 0x03, 0x01, DEF_STR2( Coinage ) );
	PORT_DIPSETTING(    0x03, DEF_STR2( _2C_1C ) );
	PORT_DIPSETTING(    0x01, DEF_STR2( _1C_1C ) );
	PORT_DIPSETTING(    0x02, DEF_STR2( _1C_2C ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( Free_Play ) );
	PORT_DIPNAME( 0x0c, 0x08, DEF_STR2( Lives ) );
	PORT_DIPSETTING(    0x00, "1" );
	PORT_DIPSETTING(    0x04, "2" );
	PORT_DIPSETTING(    0x08, "3" );
	PORT_DIPSETTING(    0x0c, "4" );
	PORT_DIPNAME( 0x10, 0x10, DEF_STR2( Unknown ) );
	PORT_DIPSETTING(    0x10, DEF_STR2( Off ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( On ) );
	PORT_DIPNAME( 0x20, 0x20, DEF_STR2( Unknown ) );
	PORT_DIPSETTING(    0x20, DEF_STR2( Off ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( On ) );
	PORT_BIT( 0xc0, IP_ACTIVE_HIGH, IPT_UNUSED );  /* Protection in Make Trax */

	PORT_START();	/* DSW 2 */
	PORT_BIT( 0xff, IP_ACTIVE_HIGH, IPT_UNUSED );
	return true;
}

private boolean ipt_paintrlr() {
	PORT_START();	/* IN0 */
	PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_4WAY );
	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_4WAY );
	PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_4WAY );
	PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_4WAY );
	PORT_DIPNAME( 0x10, 0x00, DEF_STR2( Cabinet ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( Upright ) );
	PORT_DIPSETTING(    0x10, DEF_STR2( Cocktail ) );
	PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_COIN1 );
	PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_COIN2 );
	PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_SERVICE1 );

	PORT_START();	/* IN1 */
	PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_4WAY | IPF_COCKTAIL );
	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_4WAY | IPF_COCKTAIL );
	PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_4WAY | IPF_COCKTAIL );
	PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_4WAY | IPF_COCKTAIL );
	PORT_BIT( 0x10, IP_ACTIVE_HIGH, IPT_UNUSED );  /* Protection in Make Trax */
	PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_START1 );
	PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_START2 );
	PORT_BIT( 0x80, IP_ACTIVE_HIGH, IPT_UNUSED );  /* Protection in Make Trax */

	PORT_START();	/* DSW 1 */
	PORT_DIPNAME( 0x03, 0x01, DEF_STR2( Coinage ) );
	PORT_DIPSETTING(    0x03, DEF_STR2( _2C_1C ) );
	PORT_DIPSETTING(    0x01, DEF_STR2( _1C_1C ) );
	PORT_DIPSETTING(    0x02, DEF_STR2( _1C_2C ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( Free_Play ) );
	PORT_DIPNAME( 0x0c, 0x00, DEF_STR2( Lives ) );
	PORT_DIPSETTING(    0x00, "3" );
	PORT_DIPSETTING(    0x04, "4" );
	PORT_DIPSETTING(    0x08, "5" );
	PORT_DIPSETTING(    0x0c, "6" );
	PORT_DIPNAME( 0x10, 0x10, DEF_STR2( Unknown ) );
	PORT_DIPSETTING(    0x10, DEF_STR2( Off ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( On ) );
	PORT_DIPNAME( 0x20, 0x20, DEF_STR2( Unknown ) );
	PORT_DIPSETTING(    0x20, DEF_STR2( Off ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( On ) );
	PORT_BIT( 0xc0, IP_ACTIVE_HIGH, IPT_UNUSED );  /* Protection in Make Trax */

	PORT_START();	/* DSW 2 */
	PORT_BIT( 0xff, IP_ACTIVE_HIGH, IPT_UNUSED );
	return true;
}

private boolean ipt_ponpoko() {
	PORT_START();	/* IN0 */
	PORT_BIT( 0x01, IP_ACTIVE_HIGH, IPT_JOYSTICK_UP    | IPF_8WAY );
	PORT_BIT( 0x02, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT  | IPF_8WAY );
	PORT_BIT( 0x04, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_8WAY );
	PORT_BIT( 0x08, IP_ACTIVE_HIGH, IPT_JOYSTICK_DOWN  | IPF_8WAY );
	PORT_BIT( 0x10, IP_ACTIVE_HIGH, IPT_BUTTON1 );
	PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_COIN1 );
	PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_COIN2 );
	PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_SERVICE1 );

	/* The 2nd player controls are used even in upright mode */
	PORT_START();	/* IN1 */
	PORT_BIT( 0x01, IP_ACTIVE_HIGH, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_PLAYER2 );
	PORT_BIT( 0x02, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_PLAYER2 );
	PORT_BIT( 0x04, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_PLAYER2 );
	PORT_BIT( 0x08, IP_ACTIVE_HIGH, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_PLAYER2 );
	PORT_BIT( 0x10, IP_ACTIVE_HIGH, IPT_BUTTON1 | IPF_PLAYER2 );
	PORT_BIT( 0x20, IP_ACTIVE_HIGH, IPT_START1 );
	PORT_BIT( 0x40, IP_ACTIVE_HIGH, IPT_START2 );
	PORT_BIT( 0x80, IP_ACTIVE_HIGH, IPT_UNUSED );

	PORT_START();	/* DSW 1 */
	PORT_DIPNAME( 0x03, 0x01, DEF_STR2( Bonus_Life ) );
	PORT_DIPSETTING(    0x01, "10000" );
	PORT_DIPSETTING(    0x02, "30000" );
	PORT_DIPSETTING(    0x03, "50000" );
	PORT_DIPSETTING(    0x00, "None" );
	PORT_DIPNAME( 0x0c, 0x00, DEF_STR2( Unknown ) );
	PORT_DIPSETTING(    0x00, "0" );
	PORT_DIPSETTING(    0x04, "1" );
	PORT_DIPSETTING(    0x08, "2" );
	PORT_DIPSETTING(    0x0c, "3" );
	PORT_DIPNAME( 0x30, 0x20, DEF_STR2( Lives ) );
	PORT_DIPSETTING(    0x00, "2" );
	PORT_DIPSETTING(    0x10, "3" );
	PORT_DIPSETTING(    0x20, "4" );
	PORT_DIPSETTING(    0x30, "5" );
	PORT_DIPNAME( 0x40, 0x40, DEF_STR2( Cabinet ) );
	PORT_DIPSETTING(    0x40, DEF_STR2( Upright ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( Cocktail ) );
	PORT_DIPNAME( 0x80, 0x80, DEF_STR2( Unknown ) );
	PORT_DIPSETTING(    0x80, DEF_STR2( Off ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( On ) );

	PORT_START();	/* DSW 2 */
	PORT_DIPNAME( 0x0f, 0x01, DEF_STR2( Coinage ) );
	PORT_DIPSETTING(    0x04, "A 3/1 B 3/1" );
	PORT_DIPSETTING(    0x0e, "A 3/1 B 1/2" );
	PORT_DIPSETTING(    0x0f, "A 3/1 B 1/4" );
	PORT_DIPSETTING(    0x02, "A 2/1 B 2/1" );
	PORT_DIPSETTING(    0x0d, "A 2/1 B 1/1" );
	PORT_DIPSETTING(    0x07, "A 2/1 B 1/3" );
	PORT_DIPSETTING(    0x0b, "A 2/1 B 1/5" );
	PORT_DIPSETTING(    0x0c, "A 2/1 B 1/6" );
	PORT_DIPSETTING(    0x01, "A 1/1 B 1/1" );
	PORT_DIPSETTING(    0x06, "A 1/1 B 4/5" );
	PORT_DIPSETTING(    0x05, "A 1/1 B 2/3" );
	PORT_DIPSETTING(    0x0a, "A 1/1 B 1/3" );
	PORT_DIPSETTING(    0x08, "A 1/1 B 1/5" );
	PORT_DIPSETTING(    0x09, "A 1/1 B 1/6" );
	PORT_DIPSETTING(    0x03, "A 1/2 B 1/2" );
	PORT_DIPSETTING(    0x00, DEF_STR2( Free_Play ) );
	PORT_DIPNAME( 0x10, 0x10, DEF_STR2( Unknown ) );  /* Most likely unused */
	PORT_DIPSETTING(    0x10, DEF_STR2( Off ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( On ) );
	PORT_DIPNAME( 0x20, 0x20, DEF_STR2( Unknown ) );  /* Most likely unused */
	PORT_DIPSETTING(    0x20, DEF_STR2( Off ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( On ) );
	PORT_DIPNAME( 0x40, 0x00, DEF_STR2( Demo_Sounds ) );
	PORT_DIPSETTING(    0x40, DEF_STR2( Off ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( On ) );
	PORT_DIPNAME( 0x80, 0x80, DEF_STR2( Unknown ) );  /* Most likely unused */
	PORT_DIPSETTING(    0x80, DEF_STR2( Off ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( On ) );
	return true;
}

private boolean ipt_eyes() {
	PORT_START();  /* IN0 */
	PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_4WAY );
	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_4WAY );
	PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_4WAY );
	PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_4WAY );
	PORT_SERVICE( 0x10, IP_ACTIVE_LOW );
	PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_COIN1 );
	PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_TILT );
	PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_COIN2 );

	PORT_START();	/* IN1 */
	PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_4WAY | IPF_COCKTAIL );
	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_4WAY | IPF_COCKTAIL );
	PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_4WAY | IPF_COCKTAIL );
	PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_4WAY | IPF_COCKTAIL );
	PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 );
	PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_START1 );
	PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_START2 );
	PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_COCKTAIL );

	PORT_START();	/* DSW 1 */
	PORT_DIPNAME( 0x03, 0x03, DEF_STR2( Coinage ) );
	PORT_DIPSETTING(    0x01, DEF_STR2( _2C_1C ) );
	PORT_DIPSETTING(    0x03, DEF_STR2( _1C_1C ) );
	PORT_DIPSETTING(    0x02, DEF_STR2( _1C_2C ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( Free_Play ) );
	PORT_DIPNAME( 0x0c, 0x08, DEF_STR2( Lives ) );
	PORT_DIPSETTING(    0x0c, "2" );
	PORT_DIPSETTING(    0x08, "3" );
	PORT_DIPSETTING(    0x04, "4" );
	PORT_DIPSETTING(    0x00, "5" );
	PORT_DIPNAME( 0x30, 0x30, DEF_STR2( Bonus_Life ) );
	PORT_DIPSETTING(    0x30, "50000" );
	PORT_DIPSETTING(    0x20, "75000" );
	PORT_DIPSETTING(    0x10, "100000" );
	PORT_DIPSETTING(    0x00, "125000" );
	PORT_DIPNAME( 0x40, 0x40, DEF_STR2( Cabinet ) );
	PORT_DIPSETTING(    0x40, DEF_STR2( Upright ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( Cocktail ) );
	PORT_DIPNAME( 0x80, 0x80, DEF_STR2( Unknown ) );  /* Not accessed */
	PORT_DIPSETTING(    0x80, DEF_STR2( Off ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( On ) );

	PORT_START();	/* DSW 2 */
	PORT_BIT( 0xff, IP_ACTIVE_HIGH, IPT_UNUSED );
	return true;
}

private boolean ipt_mrtnt() {
	PORT_START();  /* IN0 */
	PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_4WAY );
	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_4WAY );
	PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_4WAY );
	PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_4WAY );
	PORT_SERVICE( 0x10, IP_ACTIVE_LOW );
	PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_COIN1 );
	PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_TILT );
	PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_COIN2 );

	PORT_START();	/* IN1 */
	PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_4WAY | IPF_COCKTAIL );
	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_4WAY | IPF_COCKTAIL );
	PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_4WAY | IPF_COCKTAIL );
	PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_4WAY | IPF_COCKTAIL );
	PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 );
	PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_START1 );
	PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_START2 );
	PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_COCKTAIL );

	PORT_START();	/* DSW 1 */
	PORT_DIPNAME( 0x03, 0x03, DEF_STR2( Coinage ) );
	PORT_DIPSETTING(    0x01, DEF_STR2( _2C_1C ) );
	PORT_DIPSETTING(    0x03, DEF_STR2( _1C_1C ) );
	PORT_DIPSETTING(    0x02, DEF_STR2( _1C_2C ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( Free_Play ) );
	PORT_DIPNAME( 0x0c, 0x08, DEF_STR2( Lives ) );
	PORT_DIPSETTING(    0x0c, "2" );
	PORT_DIPSETTING(    0x08, "3" );
	PORT_DIPSETTING(    0x04, "4" );
	PORT_DIPSETTING(    0x00, "5" );
	PORT_DIPNAME( 0x30, 0x30, DEF_STR2( Bonus_Life ) );
	PORT_DIPSETTING(    0x30, "75000" );
	PORT_DIPSETTING(    0x20, "100000" );
	PORT_DIPSETTING(    0x10, "125000" );
	PORT_DIPSETTING(    0x00, "150000" );
	PORT_DIPNAME( 0x40, 0x40, DEF_STR2( Cabinet ) );
	PORT_DIPSETTING(    0x40, DEF_STR2( Upright ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( Cocktail ) );
	PORT_DIPNAME( 0x80, 0x80, DEF_STR2( Unknown ) );
	PORT_DIPSETTING(    0x80, DEF_STR2( Off ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( On ) );

	PORT_START();	/* DSW 2 */
	PORT_BIT( 0xff, IP_ACTIVE_HIGH, IPT_UNUSED );
	return true;
}

private boolean ipt_lizwiz() {
	PORT_START();	/* IN0 */
	PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY );
	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY );
	PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY );
	PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY );
	PORT_SERVICE( 0x10, IP_ACTIVE_LOW );
	PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_COIN1 );
	PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_TILT );
	PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_COIN2 );

	PORT_START();	/* IN1 */
	PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_PLAYER2 );
	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_PLAYER2 );
	PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_PLAYER2 );
	PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_PLAYER2 );
	PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 );
	PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_START1 );
	PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_START2 );
	PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER2 );

	PORT_START();	/* DSW 1 */
	PORT_DIPNAME( 0x03, 0x03, DEF_STR2( Coinage ) );
	PORT_DIPSETTING(    0x01, DEF_STR2( _2C_1C ) );
	PORT_DIPSETTING(    0x03, DEF_STR2( _1C_1C ) );
	PORT_DIPSETTING(    0x02, DEF_STR2( _1C_2C ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( Free_Play ) );
	PORT_DIPNAME( 0x0c, 0x08, DEF_STR2( Lives ) );
	PORT_DIPSETTING(    0x0c, "2" );
	PORT_DIPSETTING(    0x08, "3" );
	PORT_DIPSETTING(    0x04, "4" );
	PORT_DIPSETTING(    0x00, "5" );
	PORT_DIPNAME( 0x30, 0x30, DEF_STR2( Bonus_Life ) );
	PORT_DIPSETTING(    0x30, "75000" );
	PORT_DIPSETTING(    0x20, "100000" );
	PORT_DIPSETTING(    0x10, "125000" );
	PORT_DIPSETTING(    0x00, "150000" );
	PORT_DIPNAME( 0x40, 0x40, DEF_STR2( Difficulty ) );
	PORT_DIPSETTING(    0x40, "Normal" );
	PORT_DIPSETTING(    0x00, "Hard" );
	PORT_DIPNAME( 0x80, 0x80, DEF_STR2( Unknown ) );
	PORT_DIPSETTING(    0x80, DEF_STR2( Off ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( On ) );

	PORT_START();	/* DSW 2 */
	PORT_BIT( 0xff, IP_ACTIVE_HIGH, IPT_UNUSED );
	return true;
}

private boolean ipt_jumpshot() {
	PORT_START(); /* IN0 */
	PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY );
	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY );
	PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY );
	PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY );
	PORT_DIPNAME( 0x10, 0x10, DEF_STR2( Unknown ) );
	PORT_DIPSETTING(    0x10, DEF_STR2( Off ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( On ) );
	PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_COIN1 );
	PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_COIN2 );
	PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_SERVICE1 );

	PORT_START(); /* IN1 */
	PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_PLAYER2 );
	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_PLAYER2 );
	PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_PLAYER2 );
	PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_PLAYER2 );
	PORT_SERVICE( 0x10, IP_ACTIVE_LOW );
	PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON1 );
	PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER2  );
	PORT_DIPNAME( 0x80, 0x80, DEF_STR2( Unknown ) );
	PORT_DIPSETTING(    0x80, DEF_STR2( Off ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( On ) );

	PORT_START(); /* DSW 1 */
	PORT_DIPNAME( 0x03, 0x01, "Time"  );
//	PORT_DIPSETTING(    0x00,  "2 Minutes"  );
	PORT_DIPSETTING(    0x02,  "2 Minutes" );
	PORT_DIPSETTING(    0x03,  "3 Minutes" );
	PORT_DIPSETTING(    0x01,  "4 Minutes"  );
	PORT_DIPNAME( 0x04, 0x04, DEF_STR2( Unknown ) );
	PORT_DIPSETTING(    0x04, DEF_STR2( Off ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( On ) );
	PORT_DIPNAME( 0x08, 0x08, DEF_STR2( Unknown ) );
	PORT_DIPSETTING(    0x08, DEF_STR2( Off ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( On ) );
	PORT_DIPNAME( 0x10, 0x10, DEF_STR2( Free_Play ) );
	PORT_DIPSETTING(    0x10, DEF_STR2( Off ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( On ) );
	PORT_DIPNAME( 0x20, 0x00, "2 Players Game" );
	PORT_DIPSETTING(    0x20, "1 Credit" );
	PORT_DIPSETTING(    0x00, "2 Credits" );
	PORT_DIPNAME( 0x40, 0x40, DEF_STR2( Unknown ) );
	PORT_DIPSETTING(    0x40, DEF_STR2( Off ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( On ) );
	PORT_DIPNAME( 0x80, 0x80, DEF_STR2( Unknown ) );
	PORT_DIPSETTING(    0x80, DEF_STR2( Off) );
	PORT_DIPSETTING(    0x00, DEF_STR2( On ) );

	PORT_START(); /* DSW 2 */
	PORT_BIT( 0xff, IP_ACTIVE_HIGH, IPT_UNUSED );
	return true;
}

/*************************************
 *
 *	Graphics layouts
 *
 *************************************/

int[][] tilelayout =
{
	{8},{8},	/* 8*8 characters */
    {256},    /* 256 characters */
    {2},  /* 2 bits per pixel */
    { 0, 4 },   /* the two bitplanes for 4 pixels are packed into one byte */
    { 8*8+0, 8*8+1, 8*8+2, 8*8+3, 0, 1, 2, 3 }, /* bits are packed in groups of four */
    { 0*8, 1*8, 2*8, 3*8, 4*8, 5*8, 6*8, 7*8 },
    {16*8}    /* every char takes 16 bytes */
};

int[][] spritelayout =
{
	{16},{16},	/* 16*16 sprites */
	{64},	/* 64 sprites */
	{2},	/* 2 bits per pixel */
	{ 0, 4 },	/* the two bitplanes for 4 pixels are packed into one byte */
	{ 8*8, 8*8+1, 8*8+2, 8*8+3, 16*8+0, 16*8+1, 16*8+2, 16*8+3,
			24*8+0, 24*8+1, 24*8+2, 24*8+3, 0, 1, 2, 3 },
	{ 0*8, 1*8, 2*8, 3*8, 4*8, 5*8, 6*8, 7*8,
			32*8, 33*8, 34*8, 35*8, 36*8, 37*8, 38*8, 39*8 },
	{64*8}	/* every sprite takes 64 bytes */
};

private boolean gfxdecodeinfo()
{
	GDI_ADD( REGION_GFX1, 0, tilelayout,   0, 32 );
	GDI_ADD( REGION_GFX2, 0, spritelayout, 0, 32 );
	GDI_ADD( -1 ); /* end of array */
	return true;
};

/*************************************
 *
 *	Sound interfaces
 *
 *************************************/

/*************************************
 *
 *	Machine drivers
 *
 *************************************/

public boolean mdrv_pacman() {

	/* basic machine hardware */
	MDRV_CPU_ADD_TAG("main", Z80, 18432000/6);
	MDRV_CPU_MEMORY(readmem(),writemem());
	MDRV_CPU_PORTS(0,writeport());
	MDRV_CPU_VBLANK_INT(pacman_interrupt,1);

	MDRV_FRAMES_PER_SECOND(60.606060);
	MDRV_VBLANK_DURATION(DEFAULT_REAL_60HZ_VBLANK_DURATION);
	//MDRV_MACHINE_INIT(pacman);

	/* video hardware */
	MDRV_VIDEO_ATTRIBUTES(VIDEO_TYPE_RASTER);
	MDRV_SCREEN_SIZE(36*8, 28*8);
	MDRV_VISIBLE_AREA(0*8, 36*8-1, 0*8, 28*8-1);
	MDRV_GFXDECODE(gfxdecodeinfo());
	MDRV_PALETTE_LENGTH(16);
	MDRV_COLORTABLE_LENGTH(4*32);

	MDRV_PALETTE_INIT(pacman_pi);
	MDRV_VIDEO_START(pacman_vs);
	MDRV_VIDEO_UPDATE(pengo_vu);

	/* sound hardware */
	//MDRV_SOUND_ADD_TAG("namco", NAMCO, namco_interface);
	return true;
}

public boolean mdrv_pacplus() {

	/* basic machine hardware */
	MDRV_IMPORT_FROM(mdrv_pacman());

	MDRV_CPU_MODIFY("main");
	MDRV_CPU_VBLANK_INT(pacplus_interrupt,1);

	//MDRV_MACHINE_INIT(pacplus);
	return true;
}

public boolean mdrv_mspacpls() {

	/* basic machine hardware */
	MDRV_IMPORT_FROM(mdrv_pacman());

	MDRV_CPU_MODIFY("main");
	MDRV_CPU_VBLANK_INT(irq0_line_hold,1);

	//MDRV_MACHINE_INIT(NULL);
	return true;
}

/*************************************
 *
 *	ROM definitions
 *
 *************************************/

private boolean rom_puckman() {
	ROM_REGION( 0x10000, REGION_CPU1, 0 );	/* 64k for code */
	ROM_LOAD( "namcopac.6e",  0x0000, 0x1000, 0xfee263b3 );
	ROM_LOAD( "namcopac.6f",  0x1000, 0x1000, 0x39d1fc83 );
	ROM_LOAD( "namcopac.6h",  0x2000, 0x1000, 0x02083b03 );
	ROM_LOAD( "namcopac.6j",  0x3000, 0x1000, 0x7a36fe55 );

	ROM_REGION( 0x1000, REGION_GFX1, ROMREGION_DISPOSE );
	ROM_LOAD( "pacman.5e",    0x0000, 0x1000, 0x0c944964 );

	ROM_REGION( 0x1000, REGION_GFX2, ROMREGION_DISPOSE );
	ROM_LOAD( "pacman.5f",    0x0000, 0x1000, 0x958fedf9 );

	ROM_REGION( 0x0120, REGION_PROMS, 0 );
	ROM_LOAD( "82s123.7f",    0x0000, 0x0020, 0x2fc650bd );
	ROM_LOAD( "82s126.4a",    0x0020, 0x0100, 0x3eb3a8e4 );

	//ROM_REGION( 0x0200, REGION_SOUND1, 0 );	/* sound PROMs */
	//ROM_LOAD( "82s126.1m",    0x0000, 0x0100, 0xa9cc86bf );
	//ROM_LOAD( "82s126.3m",    0x0100, 0x0100, 0x77245b66 );	/* timing - not used */
	return true;
}

private boolean rom_puckmod() {
	ROM_REGION( 0x10000, REGION_CPU1, 0 );	/* 64k for code */
	ROM_LOAD( "namcopac.6e",  0x0000, 0x1000, 0xfee263b3 );
	ROM_LOAD( "namcopac.6f",  0x1000, 0x1000, 0x39d1fc83 );
	ROM_LOAD( "namcopac.6h",  0x2000, 0x1000, 0x02083b03 );
	ROM_LOAD( "npacmod.6j",   0x3000, 0x1000, 0x7d98d5f5 );

	ROM_REGION( 0x1000, REGION_GFX1, ROMREGION_DISPOSE );
	ROM_LOAD( "pacman.5e",    0x0000, 0x1000, 0x0c944964 );

	ROM_REGION( 0x1000, REGION_GFX2, ROMREGION_DISPOSE );
	ROM_LOAD( "pacman.5f",    0x0000, 0x1000, 0x958fedf9 );

	ROM_REGION( 0x0120, REGION_PROMS, 0 );
	ROM_LOAD( "82s123.7f",    0x0000, 0x0020, 0x2fc650bd );
	ROM_LOAD( "82s126.4a",    0x0020, 0x0100, 0x3eb3a8e4 );

	//ROM_REGION( 0x0200, REGION_SOUND1, 0 );	/* sound PROMs */
	//ROM_LOAD( "82s126.1m",    0x0000, 0x0100, 0xa9cc86bf );
	//ROM_LOAD( "82s126.3m",    0x0100, 0x0100, 0x77245b66 );	/* timing - not used */
	return true;
}

private boolean rom_puckmana() {
	ROM_REGION( 0x10000, REGION_CPU1, 0 );	/* 64k for code */
	ROM_LOAD( "pacman.6e",    0x0000, 0x1000, 0xc1e6ab10 );
	ROM_LOAD( "pacman.6f",    0x1000, 0x1000, 0x1a6fb2d4 );
	ROM_LOAD( "pacman.6h",    0x2000, 0x1000, 0xbcdd1beb );
	ROM_LOAD( "prg7",         0x3000, 0x0800, 0xb6289b26 );
	ROM_LOAD( "prg8",         0x3800, 0x0800, 0x17a88c13 );

	ROM_REGION( 0x1000, REGION_GFX1, ROMREGION_DISPOSE );
	ROM_LOAD( "chg1",         0x0000, 0x0800, 0x2066a0b7 );
	ROM_LOAD( "chg2",         0x0800, 0x0800, 0x3591b89d );

	ROM_REGION( 0x1000, REGION_GFX2, ROMREGION_DISPOSE );
	ROM_LOAD( "pacman.5f",    0x0000, 0x1000, 0x958fedf9 );

	ROM_REGION( 0x0120, REGION_PROMS, 0 );
	ROM_LOAD( "82s123.7f",    0x0000, 0x0020, 0x2fc650bd );
	ROM_LOAD( "82s126.4a",    0x0020, 0x0100, 0x3eb3a8e4 );

	//ROM_REGION( 0x0200, REGION_SOUND1, 0 );	/* sound PROMs */
	//ROM_LOAD( "82s126.1m",    0x0000, 0x0100, 0xa9cc86bf );
	//ROM_LOAD( "82s126.3m",    0x0100, 0x0100, 0x77245b66 );	/* timing - not used */
	return true;
}

private boolean rom_pacman() {
	ROM_REGION( 0x10000, REGION_CPU1, 0 );	/* 64k for code */
	ROM_LOAD( "pacman.6e",    0x0000, 0x1000, 0xc1e6ab10 );
	ROM_LOAD( "pacman.6f",    0x1000, 0x1000, 0x1a6fb2d4 );
	ROM_LOAD( "pacman.6h",    0x2000, 0x1000, 0xbcdd1beb );
	ROM_LOAD( "pacman.6j",    0x3000, 0x1000, 0x817d94e3 );

	ROM_REGION( 0x1000, REGION_GFX1, ROMREGION_DISPOSE );
	ROM_LOAD( "pacman.5e",    0x0000, 0x1000, 0x0c944964 );

	ROM_REGION( 0x1000, REGION_GFX2, ROMREGION_DISPOSE );
	ROM_LOAD( "pacman.5f",    0x0000, 0x1000, 0x958fedf9 );

	ROM_REGION( 0x0120, REGION_PROMS, 0 );
	ROM_LOAD( "82s123.7f",    0x0000, 0x0020, 0x2fc650bd );
	ROM_LOAD( "82s126.4a",    0x0020, 0x0100, 0x3eb3a8e4 );

	//ROM_REGION( 0x0200, REGION_SOUND1, 0 );	/* sound PROMs */
	//ROM_LOAD( "82s126.1m",    0x0000, 0x0100, 0xa9cc86bf );
	//ROM_LOAD( "82s126.3m",    0x0100, 0x0100, 0x77245b66 );	/* timing - not used */
	return true;
}

private boolean rom_pacmod() {
	ROM_REGION( 0x10000, REGION_CPU1, 0 );	/* 64k for code */
	ROM_LOAD( "pacmanh.6e",   0x0000, 0x1000, 0x3b2ec270 );
	ROM_LOAD( "pacman.6f",    0x1000, 0x1000, 0x1a6fb2d4 );
	ROM_LOAD( "pacmanh.6h",   0x2000, 0x1000, 0x18811780 );
	ROM_LOAD( "pacmanh.6j",   0x3000, 0x1000, 0x5c96a733 );

	ROM_REGION( 0x1000, REGION_GFX1, ROMREGION_DISPOSE );
	ROM_LOAD( "pacmanh.5e",   0x0000, 0x1000, 0x299fb17a );

	ROM_REGION( 0x1000, REGION_GFX2, ROMREGION_DISPOSE );
	ROM_LOAD( "pacman.5f",    0x0000, 0x1000, 0x958fedf9 );

	ROM_REGION( 0x0120, REGION_PROMS, 0 );
	ROM_LOAD( "82s123.7f",    0x0000, 0x0020, 0x2fc650bd );
	ROM_LOAD( "82s126.4a",    0x0020, 0x0100, 0x3eb3a8e4 );

	//ROM_REGION( 0x0200, REGION_SOUND1, 0 );	/* sound PROMs */
	//ROM_LOAD( "82s126.1m",    0x0000, 0x0100, 0xa9cc86bf );
	//ROM_LOAD( "82s126.3m",    0x0100, 0x0100, 0x77245b66 );	/* timing - not used */
	return true;
}

private boolean rom_hangly() {
	ROM_REGION( 0x10000, REGION_CPU1, 0 );	/* 64k for code */
	ROM_LOAD( "hangly.6e",    0x0000, 0x1000, 0x5fe8610a );
	ROM_LOAD( "hangly.6f",    0x1000, 0x1000, 0x73726586 );
	ROM_LOAD( "hangly.6h",    0x2000, 0x1000, 0x4e7ef99f );
	ROM_LOAD( "hangly.6j",    0x3000, 0x1000, 0x7f4147e6 );

	ROM_REGION( 0x1000, REGION_GFX1, ROMREGION_DISPOSE );
	ROM_LOAD( "pacman.5e",    0x0000, 0x1000, 0x0c944964 );

	ROM_REGION( 0x1000, REGION_GFX2, ROMREGION_DISPOSE );
	ROM_LOAD( "pacman.5f",    0x0000, 0x1000, 0x958fedf9 );

	ROM_REGION( 0x0120, REGION_PROMS, 0 );
	ROM_LOAD( "82s123.7f",    0x0000, 0x0020, 0x2fc650bd );
	ROM_LOAD( "82s126.4a",    0x0020, 0x0100, 0x3eb3a8e4 );

	//ROM_REGION( 0x0200, REGION_SOUND1, 0 );	/* sound PROMs */
	//ROM_LOAD( "82s126.1m",    0x0000, 0x0100, 0xa9cc86bf );
	//ROM_LOAD( "82s126.3m",    0x0100, 0x0100, 0x77245b66 );	/* timing - not used */
	return true;
}

private boolean rom_hangly2() {
	ROM_REGION( 0x10000, REGION_CPU1, 0 );	/* 64k for code */
	ROM_LOAD( "hangly.6e",    0x0000, 0x1000, 0x5fe8610a );
	ROM_LOAD( "hangly2.6f",   0x1000, 0x0800, 0x5ba228bb );
	ROM_LOAD( "hangly2.6m",   0x1800, 0x0800, 0xbaf5461e );
	ROM_LOAD( "hangly.6h",    0x2000, 0x1000, 0x4e7ef99f );
	ROM_LOAD( "hangly2.6j",   0x3000, 0x0800, 0x51305374 );
	ROM_LOAD( "hangly2.6p",   0x3800, 0x0800, 0x427c9d4d );

	ROM_REGION( 0x1000, REGION_GFX1, ROMREGION_DISPOSE );
	ROM_LOAD( "pacmanh.5e",   0x0000, 0x1000, 0x299fb17a );

	ROM_REGION( 0x1000, REGION_GFX2, ROMREGION_DISPOSE );
	ROM_LOAD( "pacman.5f",    0x0000, 0x1000, 0x958fedf9 );

	ROM_REGION( 0x0120, REGION_PROMS, 0 );
	ROM_LOAD( "82s123.7f",    0x0000, 0x0020, 0x2fc650bd );
	ROM_LOAD( "82s126.4a",    0x0020, 0x0100, 0x3eb3a8e4 );

	//ROM_REGION( 0x0200, REGION_SOUND1, 0 );	/* sound PROMs */
	//ROM_LOAD( "82s126.1m",    0x0000, 0x0100, 0xa9cc86bf );
	//ROM_LOAD( "82s126.3m",    0x0100, 0x0100, 0x77245b66 );	/* timing - not used */
	return true;
}

private boolean rom_newpuckx() {
	ROM_REGION( 0x10000, REGION_CPU1, 0 );	/* 64k for code */
	ROM_LOAD( "puckman.6e",   0x0000, 0x1000, 0xa8ae23c5 );
	ROM_LOAD( "pacman.6f",    0x1000, 0x1000, 0x1a6fb2d4 );
	ROM_LOAD( "puckman.6h",   0x2000, 0x1000, 0x197443f8 );
	ROM_LOAD( "puckman.6j",   0x3000, 0x1000, 0x2e64a3ba );

	ROM_REGION( 0x1000, REGION_GFX1, ROMREGION_DISPOSE );
	ROM_LOAD( "pacman.5e",    0x0000, 0x1000, 0x0c944964 );

	ROM_REGION( 0x1000, REGION_GFX2, ROMREGION_DISPOSE );
	ROM_LOAD( "pacman.5f",    0x0000, 0x1000, 0x958fedf9 );

	ROM_REGION( 0x0120, REGION_PROMS, 0 );
	ROM_LOAD( "82s123.7f",    0x0000, 0x0020, 0x2fc650bd );
	ROM_LOAD( "82s126.4a",    0x0020, 0x0100, 0x3eb3a8e4 );

	//ROM_REGION( 0x0200, REGION_SOUND1, 0 );	/* sound PROMs */
	//ROM_LOAD( "82s126.1m",    0x0000, 0x0100, 0xa9cc86bf );
	//ROM_LOAD( "82s126.3m",    0x0100, 0x0100, 0x77245b66 );	/* timing - not used */
	return true;
}

private boolean rom_pacheart() {
	ROM_REGION( 0x10000, REGION_CPU1, 0 );     /* 64k for code */
	ROM_LOAD( "pacheart.pg1", 0x0000, 0x0800, 0xd844b679 );
	ROM_LOAD( "pacheart.pg2", 0x0800, 0x0800, 0xb9152a38 );
	ROM_LOAD( "pacheart.pg3", 0x1000, 0x0800, 0x7d177853 );
	ROM_LOAD( "pacheart.pg4", 0x1800, 0x0800, 0x842d6574 );
	ROM_LOAD( "pacheart.pg5", 0x2000, 0x0800, 0x9045a44c );
	ROM_LOAD( "pacheart.pg6", 0x2800, 0x0800, 0x888f3c3e );
	ROM_LOAD( "pacheart.pg7", 0x3000, 0x0800, 0xf5265c10 );
	ROM_LOAD( "pacheart.pg8", 0x3800, 0x0800, 0x1a21a381 );

	ROM_REGION( 0x1000, REGION_GFX1, ROMREGION_DISPOSE );
	ROM_LOAD( "pacheart.ch1", 0x0000, 0x0800, 0xc62bbabf );
	ROM_LOAD( "chg2",         0x0800, 0x0800, 0x3591b89d );

	ROM_REGION( 0x1000, REGION_GFX2, ROMREGION_DISPOSE );
	ROM_LOAD( "pacheart.ch3", 0x0000, 0x0800, 0xca8c184c );
	ROM_LOAD( "pacheart.ch4", 0x0800, 0x0800, 0x1b1d9096 );

	ROM_REGION( 0x0120, REGION_PROMS, 0 );
	ROM_LOAD( "82s123.7f",    0x0000, 0x0020, 0x2fc650bd );
	ROM_LOAD( "82s126.4a",    0x0020, 0x0100, 0x3eb3a8e4 );

	//ROM_REGION( 0x0200, REGION_SOUND1, 0 );	/* sound PROMs */
	//ROM_LOAD( "82s126.1m",    0x0000, 0x0100, 0xa9cc86bf );
	//ROM_LOAD( "82s126.3m",    0x0100, 0x0100, 0x77245b66 );  /* timing - not used */
	return true;
}

private boolean rom_piranha() {
	ROM_REGION( 0x10000, REGION_CPU1, 0 );	/* 64k for code */
	ROM_LOAD( "pr1.cpu",      0x0000, 0x1000, 0xbc5ad024 );
	ROM_LOAD( "pacman.6f",    0x1000, 0x1000, 0x1a6fb2d4 );
	ROM_LOAD( "pr3.cpu",      0x2000, 0x1000, 0x473c379d );
	ROM_LOAD( "pr4.cpu",      0x3000, 0x1000, 0x63fbf895 );

	ROM_REGION( 0x1000, REGION_GFX1, ROMREGION_DISPOSE );
	ROM_LOAD( "pr5.cpu",      0x0000, 0x0800, 0x3fc4030c );
	ROM_LOAD( "pr7.cpu",      0x0800, 0x0800, 0x30b9a010 );

	ROM_REGION( 0x1000, REGION_GFX2, ROMREGION_DISPOSE );
	ROM_LOAD( "pr6.cpu",      0x0000, 0x0800, 0xf3e9c9d5 );
	ROM_LOAD( "pr8.cpu",      0x0800, 0x0800, 0x133d720d );

	ROM_REGION( 0x0120, REGION_PROMS, 0 );
	ROM_LOAD( "82s123.7f",    0x0000, 0x0020, 0x2fc650bd );
	ROM_LOAD( "82s126.4a",    0x0020, 0x0100, 0x3eb3a8e4 );

	//ROM_REGION( 0x0200, REGION_SOUND1, 0 );	/* sound PROMs */
	//ROM_LOAD( "82s126.1m",    0x0000, 0x0100, 0xa9cc86bf );
	//ROM_LOAD( "82s126.3m",    0x0100, 0x0100, 0x77245b66 );	/* timing - not used */
	return true;
}

private boolean rom_pacplus() {
	ROM_REGION( 0x10000, REGION_CPU1, 0 );	/* 64k for code */
	ROM_LOAD( "pacplus.6e",   0x0000, 0x1000, 0xd611ef68 );
	ROM_LOAD( "pacplus.6f",   0x1000, 0x1000, 0xc7207556 );
	ROM_LOAD( "pacplus.6h",   0x2000, 0x1000, 0xae379430 );
	ROM_LOAD( "pacplus.6j",   0x3000, 0x1000, 0x5a6dff7b );

	ROM_REGION( 0x1000, REGION_GFX1, ROMREGION_DISPOSE );
	ROM_LOAD( "pacplus.5e",   0x0000, 0x1000, 0x022c35da );

	ROM_REGION( 0x1000, REGION_GFX2, ROMREGION_DISPOSE );
	ROM_LOAD( "pacplus.5f",   0x0000, 0x1000, 0x4de65cdd );

	ROM_REGION( 0x0120, REGION_PROMS, 0 );
	ROM_LOAD( "pacplus.7f",   0x0000, 0x0020, 0x063dd53a );
	ROM_LOAD( "pacplus.4a",   0x0020, 0x0100, 0xe271a166 );

	//ROM_REGION( 0x0200, REGION_SOUND1, 0 );	/* sound PROMs */
	//ROM_LOAD( "82s126.1m",    0x0000, 0x0100, 0xa9cc86bf );
	//ROM_LOAD( "82s126.3m",    0x0100, 0x0100, 0x77245b66 );	/* timing - not used */
	return true;
}

private boolean rom_mspacmab() {
	ROM_REGION( 0x10000, REGION_CPU1, 0 );	/* 64k for code */
	ROM_LOAD( "boot1",        0x0000, 0x1000, 0xd16b31b7 );
	ROM_LOAD( "boot2",        0x1000, 0x1000, 0x0d32de5e );
	ROM_LOAD( "boot3",        0x2000, 0x1000, 0x1821ee0b );
	ROM_LOAD( "boot4",        0x3000, 0x1000, 0x165a9dd8 );
	ROM_LOAD( "boot5",        0x8000, 0x1000, 0x8c3e6de6 );
	ROM_LOAD( "boot6",        0x9000, 0x1000, 0x368cb165 );

	ROM_REGION( 0x1000, REGION_GFX1, ROMREGION_DISPOSE );
	ROM_LOAD( "5e",           0x0000, 0x1000, 0x5c281d01 );

	ROM_REGION( 0x1000, REGION_GFX2, ROMREGION_DISPOSE );
	ROM_LOAD( "5f",           0x0000, 0x1000, 0x615af909 );

	ROM_REGION( 0x0120, REGION_PROMS, 0 );
	ROM_LOAD( "82s123.7f",    0x0000, 0x0020, 0x2fc650bd );
	ROM_LOAD( "82s126.4a",    0x0020, 0x0100, 0x3eb3a8e4 );

	//ROM_REGION( 0x0200, REGION_SOUND1, 0 );	/* sound PROMs */
	//ROM_LOAD( "82s126.1m",    0x0000, 0x0100, 0xa9cc86bf );
	//ROM_LOAD( "82s126.3m",    0x0100, 0x0100, 0x77245b66 );	/* timing - not used */
	return true;
}

private boolean rom_mspacpls() {
	ROM_REGION( 0x10000, REGION_CPU1, 0 );	/* 64k for code */
	ROM_LOAD( "boot1",        0x0000, 0x1000, 0xd16b31b7 );
	ROM_LOAD( "mspacatk.2",   0x1000, 0x1000, 0x0af09d31 );
	ROM_LOAD( "boot3",        0x2000, 0x1000, 0x1821ee0b );
	ROM_LOAD( "boot4",        0x3000, 0x1000, 0x165a9dd8 );
	ROM_LOAD( "mspacatk.5",   0x8000, 0x1000, 0xe6e06954 );
	ROM_LOAD( "mspacatk.6",   0x9000, 0x1000, 0x3b5db308 );

	ROM_REGION( 0x1000, REGION_GFX1, ROMREGION_DISPOSE );
	ROM_LOAD( "5e",           0x0000, 0x1000, 0x5c281d01 );

	ROM_REGION( 0x1000, REGION_GFX2, ROMREGION_DISPOSE );
	ROM_LOAD( "5f",           0x0000, 0x1000, 0x615af909 );

	ROM_REGION( 0x0120, REGION_PROMS, 0 );
	ROM_LOAD( "82s123.7f",    0x0000, 0x0020, 0x2fc650bd );
	ROM_LOAD( "82s126.4a",    0x0020, 0x0100, 0x3eb3a8e4 );

	//ROM_REGION( 0x0200, REGION_SOUND1, 0 );	/* sound PROMs */
	//ROM_LOAD( "82s126.1m",    0x0000, 0x0100, 0xa9cc86bf );
	//ROM_LOAD( "82s126.3m",    0x0100, 0x0100, 0x77245b66 );	/* timing - not used */
	return true;
}

private boolean rom_pacgal() {
	ROM_REGION( 0x10000, REGION_CPU1, 0 );	/* 64k for code */
	ROM_LOAD( "boot1",        0x0000, 0x1000, 0xd16b31b7 );
	ROM_LOAD( "boot2",        0x1000, 0x1000, 0x0d32de5e );
	ROM_LOAD( "pacman.7fh",   0x2000, 0x1000, 0x513f4d5c );
	ROM_LOAD( "pacman.7hj",   0x3000, 0x1000, 0x70694c8e );
	ROM_LOAD( "boot5",        0x8000, 0x1000, 0x8c3e6de6 );
	ROM_LOAD( "boot6",        0x9000, 0x1000, 0x368cb165 );

	ROM_REGION( 0x1000, REGION_GFX1, ROMREGION_DISPOSE );
	ROM_LOAD( "5e",           0x0000, 0x1000, 0x5c281d01 );

	ROM_REGION( 0x1000, REGION_GFX2, ROMREGION_DISPOSE );
	ROM_LOAD( "pacman.5ef",   0x0000, 0x0800, 0x65a3ee71 );
	ROM_LOAD( "pacman.5hj",   0x0800, 0x0800, 0x50c7477d );

	ROM_REGION( 0x0120, REGION_PROMS, 0 );
	ROM_LOAD( "82s123.7f",    0x0000, 0x0020, 0x2fc650bd );
	ROM_LOAD( "82s129.4a",    0x0020, 0x0100, 0x63efb927 );

	//ROM_REGION( 0x0200, REGION_SOUND1, 0 );	/* sound PROMs */
	//ROM_LOAD( "82s126.1m",    0x0000, 0x0100, 0xa9cc86bf );
	//ROM_LOAD( "82s126.3m",    0x0100, 0x0100, 0x77245b66 );	/* timing - not used */
	return true;
}

private boolean rom_crush2() {
	ROM_REGION( 0x10000, REGION_CPU1, 0 );	/* 64k for code */
	ROM_LOAD( "tp1",          0x0000, 0x0800, 0xf276592e );
	ROM_LOAD( "tp5a",         0x0800, 0x0800, 0x3d302abe );
	ROM_LOAD( "tp2",          0x1000, 0x0800, 0x25f42e70 );
	ROM_LOAD( "tp6",          0x1800, 0x0800, 0x98279cbe );
	ROM_LOAD( "tp3",          0x2000, 0x0800, 0x8377b4cb );
	ROM_LOAD( "tp7",          0x2800, 0x0800, 0xd8e76c8c );
	ROM_LOAD( "tp4",          0x3000, 0x0800, 0x90b28fa3 );
	ROM_LOAD( "tp8",          0x3800, 0x0800, 0x10854e1b );

	ROM_REGION( 0x1000, REGION_GFX1, ROMREGION_DISPOSE );
	ROM_LOAD( "tpa",          0x0000, 0x0800, 0xc7617198 );
	ROM_LOAD( "tpc",          0x0800, 0x0800, 0xe129d76a );

	ROM_REGION( 0x1000, REGION_GFX2, ROMREGION_DISPOSE );
	ROM_LOAD( "tpb",          0x0000, 0x0800, 0xd1899f05 );
	ROM_LOAD( "tpd",          0x0800, 0x0800, 0xd35d1caf );

	ROM_REGION( 0x0120, REGION_PROMS, 0 );
	ROM_LOAD( "82s123.7f",    0x0000, 0x0020, 0x2fc650bd );
	ROM_LOAD( "2s140.4a",     0x0020, 0x0100, 0x63efb927 );

	//ROM_REGION( 0x0200, REGION_SOUND1, 0 );	/* sound PROMs */
	//ROM_LOAD( "82s126.1m",    0x0000, 0x0100, 0xa9cc86bf );
	//ROM_LOAD( "82s126.3m",    0x0100, 0x0100, 0x77245b66 );	/* timing - not used */
	return true;
}

private boolean rom_crush3() {
	ROM_REGION( 0x10000, REGION_CPU1, 0 );	/* 64k for code */
	ROM_LOAD( "unkmol.4e",    0x0000, 0x0800, 0x49150ddf );
	ROM_LOAD( "unkmol.6e",    0x0800, 0x0800, 0x21f47e17 );
	ROM_LOAD( "unkmol.4f",    0x1000, 0x0800, 0x9b6dd592 );
	ROM_LOAD( "unkmol.6f",    0x1800, 0x0800, 0x755c1452 );
	ROM_LOAD( "unkmol.4h",    0x2000, 0x0800, 0xed30a312 );
	ROM_LOAD( "unkmol.6h",    0x2800, 0x0800, 0xfe4bb0eb );
	ROM_LOAD( "unkmol.4j",    0x3000, 0x0800, 0x072b91c9 );
	ROM_LOAD( "unkmol.6j",    0x3800, 0x0800, 0x66fba07d );

	ROM_REGION( 0x1000, REGION_GFX1, ROMREGION_DISPOSE );
	ROM_LOAD( "unkmol.5e",    0x0000, 0x0800, 0x338880a0 );
	ROM_LOAD( "unkmol.5h",    0x0800, 0x0800, 0x4ce9c81f );

	ROM_REGION( 0x1000, REGION_GFX2, ROMREGION_DISPOSE );
	ROM_LOAD( "unkmol.5f",    0x0000, 0x0800, 0x752e3780 );
	ROM_LOAD( "unkmol.5j",    0x0800, 0x0800, 0x6e00d2ac );

	ROM_REGION( 0x0120, REGION_PROMS, 0 );
	ROM_LOAD( "82s123.7f",    0x0000, 0x0020, 0x2fc650bd );
	ROM_LOAD( "2s140.4a",     0x0020, 0x0100, 0x63efb927 );

	//ROM_REGION( 0x0200, REGION_SOUND1, 0 );	/* sound PROMs */
	//ROM_LOAD( "82s126.1m",    0x0000, 0x0100, 0xa9cc86bf );
	//ROM_LOAD( "82s126.3m",    0x0100, 0x0100, 0x77245b66 );	/* timing - not used */
	return true;
}

private boolean rom_mbrush() {
	ROM_REGION( 0x10000, REGION_CPU1, 0 );	/* 64k for code */
	ROM_LOAD( "mbrush.6e",    0x0000, 0x1000, 0x750fbff7 );
	ROM_LOAD( "mbrush.6f",    0x1000, 0x1000, 0x27eb4299 );
	ROM_LOAD( "mbrush.6h",    0x2000, 0x1000, 0xd297108e );
	ROM_LOAD( "mbrush.6j",    0x3000, 0x1000, 0x6fd719d0 );

	ROM_REGION( 0x1000, REGION_GFX1, ROMREGION_DISPOSE );
	ROM_LOAD( "tpa",          0x0000, 0x0800, 0xc7617198 );
	ROM_LOAD( "mbrush.5h",    0x0800, 0x0800, 0xc15b6967 );

	ROM_REGION( 0x1000, REGION_GFX2, ROMREGION_DISPOSE );
	ROM_LOAD( "mbrush.5f",    0x0000, 0x0800, 0xd5bc5cb8 );  /* copyright sign was removed */
	ROM_LOAD( "tpd",          0x0800, 0x0800, 0xd35d1caf );

	ROM_REGION( 0x0120, REGION_PROMS, 0 );
	ROM_LOAD( "82s123.7f",    0x0000, 0x0020, 0x2fc650bd );
	ROM_LOAD( "2s140.4a",     0x0020, 0x0100, 0x63efb927 );

	//ROM_REGION( 0x0200, REGION_SOUND1, 0 );	/* sound PROMs */
	//ROM_LOAD( "82s126.1m",    0x0000, 0x0100, 0xa9cc86bf );
	//ROM_LOAD( "82s126.3m",    0x0100, 0x0100, 0x77245b66 );	/* timing - not used */
	return true;
}

private boolean rom_paintrlr() {
	ROM_REGION( 0x10000, REGION_CPU1, 0 );	/* 64k for code */
	ROM_LOAD( "paintrlr.1",   0x0000, 0x0800, 0x556d20b5 );
	ROM_LOAD( "paintrlr.5",   0x0800, 0x0800, 0x4598a965 );
	ROM_LOAD( "paintrlr.2",   0x1000, 0x0800, 0x2da29c81 );
	ROM_LOAD( "paintrlr.6",   0x1800, 0x0800, 0x1f561c54 );
	ROM_LOAD( "paintrlr.3",   0x2000, 0x0800, 0xe695b785 );
	ROM_LOAD( "paintrlr.7",   0x2800, 0x0800, 0x00e6eec0 );
	ROM_LOAD( "paintrlr.4",   0x3000, 0x0800, 0x0fd5884b );
	ROM_LOAD( "paintrlr.8",   0x3800, 0x0800, 0x4900114a );

	ROM_REGION( 0x1000, REGION_GFX1, ROMREGION_DISPOSE );
	ROM_LOAD( "tpa",          0x0000, 0x0800, 0xc7617198 );
	ROM_LOAD( "mbrush.5h",    0x0800, 0x0800, 0xc15b6967 );

	ROM_REGION( 0x1000, REGION_GFX2, ROMREGION_DISPOSE );
	ROM_LOAD( "mbrush.5f",    0x0000, 0x0800, 0xd5bc5cb8 );  /* copyright sign was removed */
	ROM_LOAD( "tpd",          0x0800, 0x0800, 0xd35d1caf );

	ROM_REGION( 0x0120, REGION_PROMS, 0 );
	ROM_LOAD( "82s123.7f",    0x0000, 0x0020, 0x2fc650bd );
	ROM_LOAD( "2s140.4a",     0x0020, 0x0100, 0x63efb927 );

	//ROM_REGION( 0x0200, REGION_SOUND1, 0 );	/* sound PROMs */
	//ROM_LOAD( "82s126.1m",    0x0000, 0x0100, 0xa9cc86bf );
	//ROM_LOAD( "82s126.3m",    0x0100, 0x0100, 0x77245b66 );	/* timing - not used */
	return true;
}

private boolean rom_ponpoko() {
	ROM_REGION( 0x10000, REGION_CPU1, 0 );	/* 64k for code */
	ROM_LOAD( "ppokoj1.bin",  0x0000, 0x1000, 0xffa3c004 );
	ROM_LOAD( "ppokoj2.bin",  0x1000, 0x1000, 0x4a496866 );
	ROM_LOAD( "ppokoj3.bin",  0x2000, 0x1000, 0x17da6ca3 );
	ROM_LOAD( "ppokoj4.bin",  0x3000, 0x1000, 0x9d39a565 );
	ROM_LOAD( "ppoko5.bin",   0x8000, 0x1000, 0x54ca3d7d );
	ROM_LOAD( "ppoko6.bin",   0x9000, 0x1000, 0x3055c7e0 );
	ROM_LOAD( "ppoko7.bin",   0xa000, 0x1000, 0x3cbe47ca );
	ROM_LOAD( "ppokoj8.bin",  0xb000, 0x1000, 0x04b63fc6 );

	ROM_REGION( 0x1000, REGION_GFX1, ROMREGION_DISPOSE );
	ROM_LOAD( "ppoko9.bin",   0x0000, 0x1000, 0xb73e1a06 );

	ROM_REGION( 0x1000, REGION_GFX2, ROMREGION_DISPOSE );
	ROM_LOAD( "ppoko10.bin",  0x0000, 0x1000, 0x62069b5d );

	ROM_REGION( 0x0120, REGION_PROMS, 0 );
	ROM_LOAD( "82s123.7f",    0x0000, 0x0020, 0x2fc650bd );
	ROM_LOAD( "82s126.4a",    0x0020, 0x0100, 0x3eb3a8e4 );

	//ROM_REGION( 0x0200, REGION_SOUND1, 0 );	/* sound PROMs */
	//ROM_LOAD( "82s126.1m",    0x0000, 0x0100, 0xa9cc86bf );
	//ROM_LOAD( "82s126.3m",    0x0100, 0x0100, 0x77245b66 );	/* timing - not used */
	return true;
}

private boolean rom_ponpokov() {
	ROM_REGION( 0x10000, REGION_CPU1, 0 );	/* 64k for code */
	ROM_LOAD( "ppoko1.bin",   0x0000, 0x1000, 0x49077667 );
	ROM_LOAD( "ppoko2.bin",   0x1000, 0x1000, 0x5101781a );
	ROM_LOAD( "ppoko3.bin",   0x2000, 0x1000, 0xd790ed22 );
	ROM_LOAD( "ppoko4.bin",   0x3000, 0x1000, 0x4e449069 );
	ROM_LOAD( "ppoko5.bin",   0x8000, 0x1000, 0x54ca3d7d );
	ROM_LOAD( "ppoko6.bin",   0x9000, 0x1000, 0x3055c7e0 );
	ROM_LOAD( "ppoko7.bin",   0xa000, 0x1000, 0x3cbe47ca );
	ROM_LOAD( "ppoko8.bin",   0xb000, 0x1000, 0xb39be27d );

	ROM_REGION( 0x1000, REGION_GFX1, ROMREGION_DISPOSE );
	ROM_LOAD( "ppoko9.bin",   0x0000, 0x1000, 0xb73e1a06 );

	ROM_REGION( 0x1000, REGION_GFX2, ROMREGION_DISPOSE );
	ROM_LOAD( "ppoko10.bin",  0x0000, 0x1000, 0x62069b5d );

	ROM_REGION( 0x0120, REGION_PROMS, 0 );
	ROM_LOAD( "82s123.7f",    0x0000, 0x0020, 0x2fc650bd );
	ROM_LOAD( "82s126.4a",    0x0020, 0x0100, 0x3eb3a8e4 );

	//ROM_REGION( 0x0200, REGION_SOUND1, 0 );	/* sound PROMs */
	//ROM_LOAD( "82s126.1m",    0x0000, 0x0100, 0xa9cc86bf );
	//ROM_LOAD( "82s126.3m",    0x0100, 0x0100, 0x77245b66 );	/* timing - not used */
	return true;
}

private boolean rom_eyes() {
	ROM_REGION( 0x10000, REGION_CPU1, 0 );	/* 64k for code */
	ROM_LOAD( "d7",           0x0000, 0x1000, 0x3b09ac89 );
	ROM_LOAD( "e7",           0x1000, 0x1000, 0x97096855 );
	ROM_LOAD( "f7",           0x2000, 0x1000, 0x731e294e );
	ROM_LOAD( "h7",           0x3000, 0x1000, 0x22f7a719 );

	ROM_REGION( 0x1000, REGION_GFX1, ROMREGION_DISPOSE );
	ROM_LOAD( "d5",           0x0000, 0x1000, 0xd6af0030 );

	ROM_REGION( 0x1000, REGION_GFX2, ROMREGION_DISPOSE );
	ROM_LOAD( "e5",           0x0000, 0x1000, 0xa42b5201 );

	ROM_REGION( 0x0120, REGION_PROMS, 0 );
	ROM_LOAD( "82s123.7f",    0x0000, 0x0020, 0x2fc650bd );
	ROM_LOAD( "82s129.4a",    0x0020, 0x0100, 0xd8d78829 );

	//ROM_REGION( 0x0200, REGION_SOUND1, 0 );	/* sound PROMs */
	//ROM_LOAD( "82s126.1m",    0x0000, 0x0100, 0xa9cc86bf );
	//ROM_LOAD( "82s126.3m",    0x0100, 0x0100, 0x77245b66 );	/* timing - not used */
	return true;
}

private boolean rom_eyes2() {
	ROM_REGION( 0x10000, REGION_CPU1, 0 );	/* 64k for code */
	ROM_LOAD( "g38201.7d",    0x0000, 0x1000, 0x2cda7185 );
	ROM_LOAD( "g38202.7e",    0x1000, 0x1000, 0xb9fe4f59 );
	ROM_LOAD( "g38203.7f",    0x2000, 0x1000, 0xd618ba66 );
	ROM_LOAD( "g38204.7h",    0x3000, 0x1000, 0xcf038276 );

	ROM_REGION( 0x1000, REGION_GFX1, ROMREGION_DISPOSE );
	ROM_LOAD( "g38205.5d",    0x0000, 0x1000, 0x03b1b4c7 );  /* this one has a (c) sign */

	ROM_REGION( 0x1000, REGION_GFX2, ROMREGION_DISPOSE );
	ROM_LOAD( "e5",           0x0000, 0x1000, 0xa42b5201 );

	ROM_REGION( 0x0120, REGION_PROMS, 0 );
	ROM_LOAD( "82s123.7f",    0x0000, 0x0020, 0x2fc650bd );
	ROM_LOAD( "82s129.4a",    0x0020, 0x0100, 0xd8d78829 );

	//ROM_REGION( 0x0200, REGION_SOUND1, 0 );	/* sound PROMs */
	//ROM_LOAD( "82s126.1m",    0x0000, 0x0100, 0xa9cc86bf );
	//ROM_LOAD( "82s126.3m",    0x0100, 0x0100, 0x77245b66 );	/* timing - not used */
	return true;
}

private boolean rom_mrtnt() {
	ROM_REGION( 0x10000, REGION_CPU1, 0 );	/* 64k for code */
	ROM_LOAD( "tnt.1",        0x0000, 0x1000, 0x0e836586 );
	ROM_LOAD( "tnt.2",        0x1000, 0x1000, 0x779c4c5b );
	ROM_LOAD( "tnt.3",        0x2000, 0x1000, 0xad6fc688 );
	ROM_LOAD( "tnt.4",        0x3000, 0x1000, 0xd77557b3 );

	ROM_REGION( 0x1000, REGION_GFX1, ROMREGION_DISPOSE );
	ROM_LOAD( "tnt.5",        0x0000, 0x1000, 0x3038cc0e );

	ROM_REGION( 0x1000, REGION_GFX2, ROMREGION_DISPOSE );
	ROM_LOAD( "tnt.6",        0x0000, 0x1000, 0x97634d8b );

	ROM_REGION( 0x0120, REGION_PROMS, 0 );
	ROM_LOAD( "mrtnt08.bin",  0x0000, 0x0020, 0x00000000 );
	ROM_LOAD( "mrtnt04.bin",  0x0020, 0x0100, 0x00000000 );

	//ROM_REGION( 0x0200, REGION_SOUND1, 0 );	/* sound PROMs */
	//ROM_LOAD( "82s126.1m",    0x0000, 0x0100, 0xa9cc86bf );
	//ROM_LOAD( "82s126.3m"  ,  0x0100, 0x0100, 0x77245b66 );	/* timing - not used */
	return true;
}

private boolean rom_lizwiz() {
	ROM_REGION( 0x10000, REGION_CPU1, 0 );	/* 64k for code */
	ROM_LOAD( "6e.cpu",       0x0000, 0x1000, 0x32bc1990 );
	ROM_LOAD( "6f.cpu",       0x1000, 0x1000, 0xef24b414 );
	ROM_LOAD( "6h.cpu",       0x2000, 0x1000, 0x30bed83d );
	ROM_LOAD( "6j.cpu",       0x3000, 0x1000, 0xdd09baeb );
	ROM_LOAD( "wiza",         0x8000, 0x1000, 0xf6dea3a6 );
	ROM_LOAD( "wizb",         0x9000, 0x1000, 0xf27fb5a8 );

	ROM_REGION( 0x1000, REGION_GFX1, ROMREGION_DISPOSE );
	ROM_LOAD( "5e.cpu",       0x0000, 0x1000, 0x45059e73 );

	ROM_REGION( 0x1000, REGION_GFX2, ROMREGION_DISPOSE );
	ROM_LOAD( "5f.cpu",       0x0000, 0x1000, 0xd2469717 );

	ROM_REGION( 0x0120, REGION_PROMS, 0 );
	ROM_LOAD( "7f.cpu",       0x0000, 0x0020, 0x7549a947 );
	ROM_LOAD( "4a.cpu",       0x0020, 0x0100, 0x5fdca536 );

	//ROM_REGION( 0x0200, REGION_SOUND1, 0 );	/* sound PROMs */
	//ROM_LOAD( "82s126.1m",    0x0000, 0x0100, 0xa9cc86bf );
	//ROM_LOAD( "82s126.3m"  ,  0x0100, 0x0100, 0x77245b66 );	/* timing - not used */
	return true;
}

private boolean rom_jumpshot() {
	ROM_REGION( 0x10000, REGION_CPU1, 0 );	/* 64k for code */
	ROM_LOAD( "6e",           0x0000, 0x1000, 0xf00def9a );
	ROM_LOAD( "6f",           0x1000, 0x1000, 0xf70deae2 );
	ROM_LOAD( "6h",           0x2000, 0x1000, 0x894d6f68 );
	ROM_LOAD( "6j",           0x3000, 0x1000, 0xf15a108a );

	ROM_REGION( 0x1000, REGION_GFX1, ROMREGION_DISPOSE );
	ROM_LOAD( "5e",           0x0000, 0x1000, 0xd9fa90f5 );

	ROM_REGION( 0x1000, REGION_GFX2, ROMREGION_DISPOSE );
	ROM_LOAD( "5f",           0x0000, 0x1000, 0x2ec711c1 );

	ROM_REGION( 0x0120, REGION_PROMS, 0 );
	ROM_LOAD( "prom.7f",      0x0000, 0x0020, 0x872b42f3 );
	ROM_LOAD( "prom.4a",      0x0020, 0x0100, 0x0399f39f );

	//ROM_REGION( 0x0200, REGION_SOUND1, 0 );	/* sound PROMs */
	//ROM_LOAD( "82s126.1m",    0x0000, 0x0100, 0xa9cc86bf );
	//ROM_LOAD( "82s126.3m",    0x0100, 0x0100, 0x77245b66 );	/* timing - not used */
	return true;
}

/*************************************
 *
 *	Driver initialization
 *
 *************************************/

public InitHandler init_ponpoko() { return new Init_ponpoko(); }
public class Init_ponpoko implements InitHandler {
	public void init() {
		int i, j;
		int[] RAM;
		int temp;

		/* The gfx data is swapped wrt the other Pac-Man hardware games. */
		/* Here we revert it to the usual format. */

		/* Characters */
		RAM = memory_region(REGION_GFX1);
		for (i = 0;i < memory_region_length(REGION_GFX1);i += 0x10)
		{
			for (j = 0; j < 8; j++)
			{
				temp          = RAM[i+j+0x08];
				RAM[i+j+0x08] = RAM[i+j+0x00];
				RAM[i+j+0x00] = temp;
			}
		}

		/* Sprites */
		RAM = memory_region(REGION_GFX2);
		for (i = 0;i < memory_region_length(REGION_GFX2);i += 0x20)
		{
			for (j = 0; j < 8; j++)
			{
				temp          = RAM[i+j+0x18];
				RAM[i+j+0x18] = RAM[i+j+0x10];
				RAM[i+j+0x10] = RAM[i+j+0x08];
				RAM[i+j+0x08] = RAM[i+j+0x00];
				RAM[i+j+0x00] = temp;
			}
		}
	}
}

private void eyes_decode(int[] data, int ofs)
{
	int j;
	int[] swapbuffer = new int[8];

	for (j = 0; j < 8; j++)
	{
		swapbuffer[j] = data[(j >> 2) + (j & 2) + ((j & 1) << 2) + ofs];
	}

	for (j = 0; j < 8; j++)
	{
		int ch = swapbuffer[j];

		data[j + ofs] = (ch & 0x80) | ((ch & 0x10) << 2) |
					 (ch & 0x20) | ((ch & 0x40) >> 2) | (ch & 0x0f);
	}
}

public InitHandler init_eyes() { return new Init_eyes(); }
public class Init_eyes implements InitHandler {
	public void init() {
		int i;
		int[] RAM;

		/* CPU ROMs */

		/* Data lines D3 and D5 swapped */
		RAM = memory_region(REGION_CPU1);
		for (i = 0; i < 0x4000; i++)
		{
			RAM[i] =  (RAM[i] & 0xc0) | ((RAM[i] & 0x08) << 2) |
					  (RAM[i] & 0x10) | ((RAM[i] & 0x20) >> 2) | (RAM[i] & 0x07);
		}


		/* Graphics ROMs */

		/* Data lines D4 and D6 and address lines A0 and A2 are swapped */
		RAM = memory_region(REGION_GFX1);
		for (i = 0;i < memory_region_length(REGION_GFX1);i += 8)
			eyes_decode(RAM,i);
		RAM = memory_region(REGION_GFX2);
		for (i = 0;i < memory_region_length(REGION_GFX2);i += 8)
			eyes_decode(RAM,i);
	}
}

public InitHandler init_pacplus() { return new Init_pacplus(); }
public class Init_pacplus implements InitHandler {
	public void init() {
		pacplus.pacplus_decode();
	}
}

public InitHandler init_jumpshot() { return new Init_jumpshot(); }
public class Init_jumpshot implements InitHandler {
	public void init() {
		jumpshot.jumpshot_decode();
	}
}

/*************************************
 *
 *	Game drivers
 *
 *************************************/

public Machine getMachine(URL url, String name) {
	super.getMachine(url,name);
	super.setVideoEmulator(v);
	m = new jef.machine.BasicMachine();

	/*				rom       parent    machine   inp       init */
	if (name.equals("puckman")) {
		GAME( 1980, rom_puckman(),           0,   mdrv_pacman(),   ipt_pacman(),               0, ROT90, "Namco", "PuckMan (Japan set 1)" );
	} else if (name.equals("puckmana")) {
		GAME( 1980, rom_puckmana(),  "puckman",   mdrv_pacman(),   ipt_pacman(),               0, ROT90, "Namco", "PuckMan (Japan set 2)" );
	} else if (name.equals("pacman")) {
		GAME( 1980, rom_pacman(),    "puckman",   mdrv_pacman(),   ipt_pacman(),               0, ROT90, "[Namco] (Midway license)", "Pac-Man (Midway)" );
	} else if (name.equals("puckmod")) {
		GAME( 1981, rom_puckmod(),   "puckman",   mdrv_pacman(),   ipt_pacman(),               0, ROT90, "Namco", "PuckMan (harder?)" );
	} else if (name.equals("pacmod")) {
		GAME( 1981, rom_pacmod(),    "puckman",   mdrv_pacman(),   ipt_pacman(),               0, ROT90, "[Namco] (Midway license)", "Pac-Man (Midway, harder)" );
	} else if (name.equals("hangly")) {
		GAME( 1981, rom_hangly(),    "puckman",   mdrv_pacman(),   ipt_pacman(),               0, ROT90, "hack", "Hangly-Man (set 1)" );
	} else if (name.equals("hangly2")) {
		GAME( 1981, rom_hangly2(),   "puckman",   mdrv_pacman(),   ipt_pacman(),               0, ROT90, "hack", "Hangly-Man (set 2)" );
	} else if (name.equals("newpuckx")) {
		GAME( 1980, rom_newpuckx(),  "puckman",   mdrv_pacman(),   ipt_pacman(),               0, ROT90, "hack", "New Puck-X" );
	} else if (name.equals("pacheart")) {
		GAME( 1981, rom_pacheart(),  "puckman",   mdrv_pacman(),   ipt_pacman(),               0, ROT90, "hack", "Pac-Man (Hearts)" );
	} else if (name.equals("piranha")) {
		GAME( 1981, rom_piranha(),   "puckman",   mdrv_pacman(), ipt_mspacman(),               0, ROT90, "hack", "Piranha" );
	} else if (name.equals("pacplus")) {
		GAME( 1982, rom_pacplus(),           0,  mdrv_pacplus(),   ipt_pacman(),  init_pacplus(), ROT90, "[Namco] (Midway license)", "Pac-Man Plus" );
	} else if (name.equals("mspacmab")) {
		GAME( 1981, rom_mspacmab(), "mspacman",   mdrv_pacman(), ipt_mspacman(),               0, ROT90, "bootleg", "Ms. Pac-Man (bootleg)" );
	} else if (name.equals("mspacpls")) {
		GAME( 1981, rom_mspacpls(), "mspacman", mdrv_mspacpls(), ipt_mspacpls(),               0, ROT90, "hack", "Ms. Pac-Man Plus" );
	} else if (name.equals("pacgal")) {
		GAME( 1981, rom_pacgal(),   "mspacman",   mdrv_pacman(), ipt_mspacman(),               0, ROT90, "hack", "Pac-Gal" );
	} else if (name.equals("crush2")) {
		GAME( 1981, rom_crush2(),      "crush",   mdrv_pacman(), ipt_maketrax(),               0, ROT90, "Kural Esco Electric", "Crush Roller (Kural Esco - bootleg?)" );
	} else if (name.equals("crush3")) {
		GAME( 1981, rom_crush3(),      "crush",   mdrv_pacman(), ipt_maketrax(),     init_eyes(), ROT90, "Kural Electric", "Crush Roller (Kural - bootleg?)" );
	} else if (name.equals("mbrush")) {
		GAME( 1981, rom_mbrush(),      "crush",   mdrv_pacman(),   ipt_mbrush(),               0, ROT90, "bootleg", "Magic Brush" );
	} else if (name.equals("paintrlr")) {
		GAME( 1981, rom_paintrlr(),    "crush",   mdrv_pacman(), ipt_paintrlr(),               0, ROT90, "bootleg", "Paint Roller" );
	} else if (name.equals("ponpoko")) {
		GAME( 1982, rom_ponpoko(),           0,   mdrv_pacman(),  ipt_ponpoko(),  init_ponpoko(),  ROT0, "Sigma Ent. Inc.", "Ponpoko" );
	} else if (name.equals("ponpokov")) {
		GAME( 1982, rom_ponpokov(),  "ponpoko",   mdrv_pacman(),  ipt_ponpoko(),  init_ponpoko(),  ROT0, "Sigma Ent. Inc. (Venture Line license)", "Ponpoko (Venture Line)" );
	} else if (name.equals("eyes")) {
		GAME( 1982, rom_eyes(),              0,   mdrv_pacman(),     ipt_eyes(),     init_eyes(), ROT90, "Digitrex Techstar (Rock-ola license)", "Eyes (Digitrex Techstar)" );
	} else if (name.equals("eyes2")) {
		GAME( 1982, rom_eyes2(),        "eyes",   mdrv_pacman(),     ipt_eyes(),     init_eyes(), ROT90, "Techstar Inc. (Rock-ola license)", "Eyes (Techstar Inc.)" );
	} else if (name.equals("mrtnt")) {
		GAME( 1983, rom_mrtnt(),             0,   mdrv_pacman(),    ipt_mrtnt(),     init_eyes(), ROT90, "Telko", "Mr. TNT" );
	} else if (name.equals("lizwiz")) {
		GAME( 1985, rom_lizwiz(),            0,   mdrv_pacman(),   ipt_lizwiz(),               0, ROT90, "Techstar (Sunn license)", "Lizard Wizard" );
	} else if (name.equals("jumpshot")) {
		GAME( 1985, rom_jumpshot(),          0,   mdrv_pacman(), ipt_jumpshot(), init_jumpshot(), ROT90, "Bally Midway", "Jump Shot" );
	}

	m.init(md);
	return (Machine)m;
}

}