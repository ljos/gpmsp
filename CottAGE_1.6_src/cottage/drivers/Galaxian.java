/*
CottAGE - the Arcade Generic Emulator in Java

Java driver by Erik Duijs
*/

/***************************************************************************

Galaxian/Moon Cresta memory map.

Compiled from information provided by friends and Uncles on RGVAC.

Add 0x4000 to all addresses except for the ROM for Moon Cresta.

            AAAAAA
            111111AAAAAAAAAA     DDDDDDDD   Schem   function
HEX         5432109876543210 R/W 76543210   name

0000-3FFF                                           Game ROM
4000-47FF											Working ram
5000-57FF   01010AAAAAAAAAAA R/W DDDDDDDD   !Vram   Character ram
5800-583F   01011AAAAAAAAAAA R/W DDDDDDDD   !OBJRAM Screen attributes
5840-585F   01011AAAAAAAAAAA R/W DDDDDDDD   !OBJRAM Sprites
5860-5FFF   01011AAAAAAAAAAA R/W DDDDDDDD   !OBJRAM Bullets

6000        0110000000000000 R   -------D   !SW0    coin1
6000        0110000000000000 R   ------D-   !SW0    coin2
6000        0110000000000000 R   -----D--   !SW0    p1 left
6000        0110000000000000 R   ----D---   !SW0    p1 right
6000        0110000000000000 R   ---D----   !SW0    p1shoot
6000        0110000000000000 R   --D-----   !SW0    table ??
6000        0110000000000000 R   -D------   !SW0    test
6000        0110000000000000 R   D-------   !SW0    service

6000        0110000000000001 W   -------D   !DRIVER lamp 1 ??
6001        0110000000000001 W   -------D   !DRIVER lamp 2 ??
6002        0110000000000010 W   -------D   !DRIVER lamp 3 ??
6003        0110000000000011 W   -------D   !DRIVER coin control
6004        0110000000000100 W   -------D   !DRIVER Background lfo freq bit0
6005        0110000000000101 W   -------D   !DRIVER Background lfo freq bit1
6006        0110000000000110 W   -------D   !DRIVER Background lfo freq bit2
6007        0110000000000111 W   -------D   !DRIVER Background lfo freq bit3

6800        0110100000000000 R   -------D   !SW1    1p start
6800        0110100000000000 R   ------D-   !SW1    2p start
6800        0110100000000000 R   -----D--   !SW1    p2 left
6800        0110100000000000 R   ----D---   !SW1    p2 right
6800        0110100000000000 R   ---D----   !SW1    p2 shoot
6800        0110100000000000 R   --D-----   !SW1    no used
6800        0110100000000000 R   -D------   !SW1    dip sw1
6800        0110100000000000 R   D-------   !SW1    dip sw2

6800        0110100000000000 W   -------D   !SOUND  reset background F1
                                                    (1=reset ?)
6801        0110100000000001 W   -------D   !SOUND  reset background F2
6802        0110100000000010 W   -------D   !SOUND  reset background F3
6803        0110100000000011 W   -------D   !SOUND  Noise on/off
6804        0110100000000100 W   -------D   !SOUND  not used
6805        0110100000000101 W   -------D   !SOUND  shoot on/off
6806        0110100000000110 W   -------D   !SOUND  Vol of f1
6807        0110100000000111 W   -------D   !SOUND  Vol of f2

7000        0111000000000000 R   -------D   !DIPSW  dip sw 3
7000        0111000000000000 R   ------D-   !DIPSW  dip sw 4
7000        0111000000000000 R   -----D--   !DIPSW  dip sw 5
7000        0111000000000000 R   ----D---   !DIPSW  dip s2 6

7001/B000/1 0111000000000001 W   -------D   9Nregen NMIon
7004        0111000000000100 W   -------D   9Nregen stars on
7006        0111000000000110 W   -------D   9Nregen hflip
7007        0111000000000111 W   -------D   9Nregen vflip

Note: 9n reg,other bits  used on moon cresta for extra graphics rom control.

7800        0111100000000000 R   --------   !wdr    watchdog reset
7800        0111100000000000 W   DDDDDDDD   !pitch  Sound Fx base frequency


Notes:
-----

- The only code difference between 'galaxian' and 'galmidw' is that the
  'BONUS SHIP' text is printed on a different line.

Main clock: XTAL = 18.432 MHz
Z80 Clock: XTAL/6 = 3.072 MHz
Horizontal video frequency: HSYNC = XTAL/3/192/2 = 16 kHz
Video frequency: VSYNC = HSYNC/132/2 = 60.606060 Hz
VBlank duration: 1/VSYNC * (20/132) = 2500 us


TODO:
----

- Problems with Galaxian based on the observation of a real machine:

  - Starfield is incorrect.  The speed and flashing frequency is fine, but the
    stars appear in different positions.
  - Background humming is incorrect.  It's faster on a real machine
  - Explosion sound is much softer.  Filter involved?

- $4800-4bff in Streaking/Ghost Muncher

- Need valid color prom for Fantazia. Current one is slightly damaged.


Jump Bug memory map (preliminary)

0000-3fff ROM
4000-47ff RAM
4800-4bff Video RAM
4c00-4fff mirror address for video RAM
5000-50ff Object RAM
  5000-503f  screen attributes
  5040-505f  sprites
  5060-507f  bullets?
  5080-50ff  unused?
8000-a7ff ROM

read:
6000      IN0
6800      IN1
7000      IN2

write:
5800      8910 write port
5900      8910 control port
6002-6006 gfx bank select - see vidhrdw/jumpbug.c for details
7001      interrupt enable
7002      coin counter ????
7003      ?
7004      stars on
7005      ?
7006      screen vertical flip
7007      screen horizontal flip
7800      ?


Moon Cresta versions supported:
------------------------------

mooncrst    Nichibutsu - later revision with better demo mode and
						 text for docking. Encrypted. No ROM/RAM check
mooncrsa    Nichibutsu - older revision with better demo mode and
						 text for docking. Encrypted. No ROM/RAM check
mooncrs2    Nichibutsu - probably first revision (no patches) and ROM/RAM check code.
                         This came from a bootleg board, with the logos erased
						 from the graphics
mooncrsg    Gremlin    - same docking text as mooncrst
mooncrsb    bootleg of mooncrs2. ROM/RAM check erased.


Notes about 'azurian' :
-----------------------

  bit 6 of IN1 is linked with bit 2 of IN2 (check code at 0x05b3) to set difficulty :

	bit 6  bit 2	contents of
	 IN1 	 IN2		  0x40f4   			consequences			difficulty

	 OFF 	 OFF		     2     		aliens move 2 frames out of 3		easy
	 ON  	 OFF		     4     		aliens move 4 frames out of 5		hard
	 OFF 	 ON 		     3     		aliens move 3 frames out of 4		normal
	 ON  	 ON 		     5     		aliens move 5 frames out of 6		very hard

  aliens movements is handled by routine at 0x1d59 :

    - alien 1 moves when 0x4044 != 0 else contents of 0x40f4 is stored at 0x4044
    - alien 2 moves when 0x4054 != 0 else contents of 0x40f4 is stored at 0x4054
    - alien 3 moves when 0x4064 != 0 else contents of 0x40f4 is stored at 0x4064


Notes about 'smooncrs' :
------------------------

  Due to code at 0x2b1c and 0x3306, the game ALWAYS checks the inputs for player 1
  (even for player 2 when "Cabinet" Dip Switch is set to "Cocktail")


Notes about 'scorpng' :
-----------------------

  As the START buttons are also the buttons for player 1, how should I map them ?
  I've coded this the same way as in 'checkman', but I'm not sure this is correct.

  I can't tell if it's a bug, but if you reset the game when the screen is flipped,
  the screens remains flipped (the "flip screen" routine doesn't seem to be called) !



Notes about 'frogg' :
---------------------

  If bit 5 of IN0 or bit 5 of IN1 is HIGH, something strange occurs (check code
  at 0x3580) : each time you press START2 a counter at 0x47da is incremented.
  When this counter reaches 0x2f, each next time you press START2, it acts as if
  you had pressed COIN2, so credits are added !
  Bit 5 of IN0 is tested if "Cabinet" Dip Switch is set to "Upright" and
  bit 5 of IN1 is tested if "Cabinet" Dip Switch is set to "Cocktail".



TO DO :
-------

  - smooncrs : fix read/writes at/to unmapped memory (when player 2, "cocktail" mode)
               fix the ?#! bug with "bullets" (when player 2, "cocktail" mode)
  - zigzag   : full Dip Switches and Inputs
  - zigzag2  : full Dip Switches and Inputs
  - jumpbug  : full Dip Switches and Inputs
  - jumpbugb : full Dip Switches and Inputs
  - levers   : full Dip Switches and Inputs
  - kingball : full Dip Switches and Inputs
  - kingbalj : full Dip Switches and Inputs
  - frogg    : fix read/writes at/to unmapped/wrong memory
  - scprpng  : fix read/writes at/to unmapped/wrong memory



***************************************************************************/

package cottage.drivers;

import java.net.URL;

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
import jef.video.Vh_convert_color_proms;
import jef.video.Vh_refresh;
import jef.video.Vh_start;
import jef.video.Vh_stop;

import cottage.mame.Driver;
import cottage.mame.MAMEConstants;
import cottage.mame.MAMEDriver;

public class Galaxian extends MAMEDriver implements Driver,MAMEConstants {

	int[] REGION_CPU1 = new int[0x10000];
	int[] REGION_GFX1 = new int[0x1000];
	int[] REGION_PROMS = new int[0x0020];

	jef.cpu.Z80 z80 = new jef.cpu.Z80();

	InputPort[] in = new InputPort[3];

	ReadHandler		input_port_0_r;
	ReadHandler		input_port_1_r;
	ReadHandler		input_port_2_r;

	cottage.vidhrdw.Galaxian 	v							= new cottage.vidhrdw.Galaxian();
	WriteHandler 				videoram_w					= v.videoram_w();
	WriteHandler 				galaxian_attributes_w		= v.galaxian_attributes_w();
	WriteHandler 				galaxian_stars_w			= v.galaxian_stars_w();
	WriteHandler 				interrupt_enable_w			= v.interrupt_enable_w();
	WriteHandler 				pisces_gfxbank_w			= v.pisces_gfxbank_w();
	ReadHandler					videoram_r					= v.videoram_r();
	Eof_callback				noCallback					= (Eof_callback)v;
	Vh_refresh 					galaxian_vh_screenrefresh 	= (Vh_refresh)v;
	Vh_start					galaxian_vh_start			= (Vh_start)v;
	Vh_stop						galaxian_vh_stop			= (Vh_stop)v;
	Vh_convert_color_proms 		galaxian_vh_convert_color_prom	= (Vh_convert_color_proms)v;
	InterruptHandler 			galaxian_vh_interrupt		= v.galaxian_vh_interrupt();
	InterruptHandler 			scramble_vh_interrupt		= v.scramble_vh_interrupt();

	jef.machine.BasicMachine m = new jef.machine.BasicMachine();
	cottage.machine.Scramble sm = new cottage.machine.Scramble();

	ReadHandler	scramblb_protection_1_r = (ReadHandler)sm.scramblb_protection_1_r(z80);
	ReadHandler	scramblb_protection_2_r = (ReadHandler)sm.scramblb_protection_2_r(z80);


	public MachineDriver machine_driver_galaxian() {
		CpuDriver[] cpuDriver = new CpuDriver[1];

		cpuDriver[0] = new CpuDriver(   z80,
										3072000,        /* 3.072 Mhz (?) */
										galaxian_readmem(), galaxian_writemem(), readport(), writeport(),
										galaxian_vh_interrupt, 1 );

		int[] visibleArea = { 0*8, 32*8-1, 2*8, 30*8-1 };

		return new MachineDriver
		(
			cpuDriver,

			60, 2500,
			1,
			NOP,

			//video;
			32*8, 32*8, visibleArea,
			galaxian_gfxdecodeinfo(),
			32+64+1,8*4+2*2+128*1,	/* 32 for the characters, 64 for the stars, 1 for background */
			galaxian_vh_convert_color_prom,

			VIDEO_TYPE_RASTER,

			noCallback,
			galaxian_vh_start,
			galaxian_vh_stop,
			galaxian_vh_screenrefresh,

			noSound

		);
	}

	public MachineDriver machine_driver_scramblb() {
		CpuDriver[] cpuDriver = new CpuDriver[1];

		cpuDriver[0] = new CpuDriver(   z80,
										3072000,        /* 3.072 Mhz (?) */
										scramblb_readmem(), scramblb_writemem(), readport(), writeport(),
										scramble_vh_interrupt, 1 );

		int[] visibleArea = { 0*8, 32*8-1, 2*8, 30*8-1 };

		return new MachineDriver
		(
			cpuDriver,

			60, 2500,
			1,
			NOP,

			//video;
			32*8, 32*8, visibleArea,
			galaxian_gfxdecodeinfo(),
			32+64+1,8*4+2*2+128*1,	/* 32 for the characters, 64 for the stars, 1 for background */
			galaxian_vh_convert_color_prom,

			VIDEO_TYPE_RASTER,

			noCallback,
			galaxian_vh_start,
			galaxian_vh_stop,
			galaxian_vh_screenrefresh,

			noSound

		);
	}

	private MemoryReadAddress galaxian_readmem() {
		MemoryReadAddress mra = new MemoryReadAddress(REGION_CPU1);
		mra.setMR( 0x0000, 0x3fff, MRA_ROM );	/* not all games use all the space */
		mra.setMR( 0x4000, 0x47ff, MRA_RAM );
		mra.setMR( 0x5000, 0x53ff, MRA_RAM );	/* video RAM */
		mra.set( 0x5400, 0x57ff, videoram_r );	/* video RAM mirror */
		mra.setMR( 0x5800, 0x5fff, MRA_RAM );	/* screen attributes, sprites, bullets */
		mra.set( 0x6000, 0x6000, input_port_0_r );	/* IN0 */
		mra.set( 0x6800, 0x6800, input_port_1_r );	/* IN1 */
		mra.set( 0x7000, 0x7000, input_port_2_r );	/* DSW */
		//mra.set( 0x7800, 0x7800, watchdog_reset_r );
		return mra;
	}


	private MemoryWriteAddress galaxian_writemem() {
		MemoryWriteAddress mwa = new MemoryWriteAddress(REGION_CPU1);
		mwa.setMW( 0x0000, 0x3fff, MWA_ROM );	/* not all games use all the space */
		mwa.setMW( 0x4000, 0x47ff, MWA_RAM );
		mwa.set( 0x5000, 0x53ff, videoram_w );
		mwa.set( 0x5800, 0x583f, galaxian_attributes_w );
		mwa.setMW( 0x5840, 0x585f, MWA_RAM );
		mwa.setMW( 0x5860, 0x587f, MWA_RAM );
		mwa.setMW( 0x5880, 0x58ff, MWA_RAM );
		//mwa.set( 0x6000, 0x6001, osd_led_w );
		//mwa.set( 0x6004, 0x6007, galaxian_lfo_freq_w );
		//mwa.set( 0x6800, 0x6802, galaxian_background_enable_w );
		//mwa.set( 0x6803, 0x6803, galaxian_noise_enable_w );
		//mwa.set( 0x6805, 0x6805, galaxian_shoot_enable_w );
		//mwa.set( 0x6806, 0x6807, galaxian_vol_w );
		mwa.set( 0x7001, 0x7001, interrupt_enable_w );
		mwa.set( 0x7004, 0x7004, galaxian_stars_w );
		//mwa.set( 0x7006, 0x7006, galaxian_flipx_w );
		//mwa.set( 0x7007, 0x7007, galaxian_flipy_w );
		//mwa.set( 0x7800, 0x7800, galaxian_pitch_w );
		return mwa;
	}

	private MemoryWriteAddress warofbug_writemem() {
		MemoryWriteAddress mwa = galaxian_writemem();
		mwa.set( 0x6002, 0x6002, pisces_gfxbank_w );
		return mwa;
	}

	private MemoryReadAddress scramblb_readmem() {
		MemoryReadAddress mra = new MemoryReadAddress(REGION_CPU1);
		mra.setMR( 0x0000, 0x3fff, MRA_ROM );
		mra.setMR( 0x4000, 0x4bff, MRA_RAM );	/* RAM and video RAM */
		mra.setMR( 0x5000, 0x507f, MRA_RAM );	/* screen attributes, sprites, bullets */
		mra.set( 0x6000, 0x6000, input_port_0_r );	/* IN0 */
		mra.set( 0x6800, 0x6800, input_port_1_r );	/* IN1 */
		mra.set( 0x7000, 0x7000, input_port_2_r );	/* IN2 */
		//mra.set( 0x7800, 0x7800, watchdog_reset_r );
		mra.set( 0x8102, 0x8102, scramblb_protection_1_r );
		mra.set( 0x8202, 0x8202, scramblb_protection_2_r );
		return mra;
	}

	private MemoryWriteAddress scramblb_writemem() {
		MemoryWriteAddress mwa = new MemoryWriteAddress(REGION_CPU1);
		mwa.setMW( 0x0000, 0x3fff, MWA_ROM );
		mwa.setMW( 0x4000, 0x47ff, MWA_RAM );
		mwa.set( 0x4800, 0x4bff, videoram_w );
		mwa.set( 0x5000, 0x503f, galaxian_attributes_w );
		mwa.setMW( 0x5040, 0x505f, MWA_RAM ); //, &spriteram, &spriteram_size
		mwa.setMW( 0x5060, 0x507f, MWA_RAM ); //, &galaxian_bulletsram, &galaxian_bulletsram_size
		mwa.setMW( 0x5080, 0x50ff, MWA_RAM );
		//mwa.set( 0x6000, 0x6001, MWA_NOP );  /* sound triggers */
		//mwa.set( 0x6004, 0x6007, galaxian_lfo_freq_w );
		//mwa.set( 0x6800, 0x6802, galaxian_background_enable_w );
		//mwa.set( 0x6803, 0x6803, galaxian_noise_enable_w );
		//mwa.set( 0x6805, 0x6805, galaxian_shoot_enable_w );
		//mwa.set( 0x6806, 0x6807, galaxian_vol_w );
		mwa.set( 0x7001, 0x7001, interrupt_enable_w );
		//mwa.set( 0x7002, 0x7002, coin_counter_w );
		//mwa.set( 0x7003, 0x7003, scramble_background_w );
		mwa.set( 0x7004, 0x7004, galaxian_stars_w );
		//mwa.set( 0x7006, 0x7006, galaxian_flipx_w );
		//mwa.set( 0x7007, 0x7007, galaxian_flipy_w );
		//mwa.set( 0x7800, 0x7800, galaxian_pitch_w );
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

	private InputPort[] ipt_galaxian() {
		      /* IN0 */
		in[0].setBit( 0x01, IP_ACTIVE_HIGH, IPT_COIN1 );
		in[0].setBit( 0x02, IP_ACTIVE_HIGH, IPT_COIN2 );
		in[0].setBit( 0x04, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT  | IPF_2WAY );
		in[0].setBit( 0x08, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_2WAY );
		in[0].setBit( 0x10, IP_ACTIVE_HIGH, IPT_BUTTON1 );
		in[0].setDipName( 0x20, 0x00, DEF_STR[ Cabinet ] );
		in[0].setDipSetting(    0x00, DEF_STR[ Upright ] );
		in[0].setDipSetting(    0x20, DEF_STR[ Cocktail ] );
		in[0].setService( 0x40, IP_ACTIVE_HIGH );
		in[0].setBit( 0x80, IP_ACTIVE_HIGH, IPT_COIN3 );

			  /* IN1 */
		in[1].setBit( 0x01, IP_ACTIVE_HIGH, IPT_START1 );
		in[1].setBit( 0x02, IP_ACTIVE_HIGH, IPT_START2 );
		in[1].setBit( 0x04, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT  | IPF_2WAY | IPF_COCKTAIL );
		in[1].setBit( 0x08, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_2WAY | IPF_COCKTAIL );
		in[1].setBit( 0x10, IP_ACTIVE_HIGH, IPT_BUTTON1 | IPF_COCKTAIL );
		in[1].setBit( 0x20, IP_ACTIVE_HIGH, IPT_UNKNOWN );	/* probably unused */
		in[1].setDipName( 0xc0, 0x00, DEF_STR[ Coinage ] );
		in[1].setDipSetting(    0x40, DEF_STR[ _2C_1C ] );
		in[1].setDipSetting(    0x00, DEF_STR[ _1C_1C ] );
		in[1].setDipSetting(    0x80, DEF_STR[ _1C_2C ] );
		in[1].setDipSetting(    0xc0, DEF_STR[ Free_Play ] );

			  /* DSW0 */
		in[2].setDipName( 0x03, 0x00, DEF_STR[ Bonus_Life ] );
		in[2].setDipSetting(    0x00, "7000" );
		in[2].setDipSetting(    0x01, "10000" );
		in[2].setDipSetting(    0x02, "12000" );
		in[2].setDipSetting(    0x03, "20000" );
		in[2].setDipName( 0x04, 0x04, DEF_STR[ Lives ] );
		in[2].setDipSetting(    0x00, "2" );
		in[2].setDipSetting(    0x04, "3" );
		in[2].setDipName( 0x08, 0x00, DEF_STR[ Unknown ] );
		in[2].setDipSetting(    0x00, DEF_STR[ Off ] );
		in[2].setDipSetting(    0x08, DEF_STR[ On ] );
		in[2].setBit( 0xf0, IP_ACTIVE_HIGH, IPT_UNUSED );
		return in;
	}

	private InputPort[] ipt_superg() {
		      /* IN0 */
		in[0].setBit( 0x01, IP_ACTIVE_HIGH, IPT_COIN1 );
		in[0].setBit( 0x02, IP_ACTIVE_HIGH, IPT_COIN2 );
		in[0].setBit( 0x04, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT  | IPF_2WAY );
		in[0].setBit( 0x08, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_2WAY );
		in[0].setBit( 0x10, IP_ACTIVE_HIGH, IPT_BUTTON1 );
		in[0].setDipName( 0x20, 0x00, DEF_STR[ Cabinet ] );
		in[0].setDipSetting(    0x00, DEF_STR[ Upright ] );
		in[0].setDipSetting(    0x20, DEF_STR[ Cocktail ] );
		in[0].setService( 0x40, IP_ACTIVE_HIGH );
		//in[0].setBit( 0x80, IP_ACTIVE_HIGH, IPT_Service1 );

		      /* IN1 */
		in[1].setBit( 0x01, IP_ACTIVE_HIGH, IPT_START1 );
		in[1].setBit( 0x02, IP_ACTIVE_HIGH, IPT_START2 );
		in[1].setBit( 0x04, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT  | IPF_2WAY | IPF_COCKTAIL );
		in[1].setBit( 0x08, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_2WAY | IPF_COCKTAIL );
		in[1].setBit( 0x10, IP_ACTIVE_HIGH, IPT_BUTTON1 | IPF_COCKTAIL );
		in[1].setBit( 0x20, IP_ACTIVE_HIGH, IPT_UNUSED );
		in[1].setDipName( 0xc0, 0x00, DEF_STR[ Coinage ] );
		in[1].setDipSetting(    0x40, DEF_STR[ _2C_1C ] );
		in[1].setDipSetting(    0x00, DEF_STR[ _1C_1C ] );
		in[1].setDipSetting(    0x80, DEF_STR[ _1C_2C ] );
		in[1].setDipSetting(    0xc0, DEF_STR[ Free_Play ] );

		      /* DSW0 */
		in[2].setDipName( 0x03, 0x01, DEF_STR[ Bonus_Life ] );
		in[2].setDipSetting(    0x01, "4000" );
		in[2].setDipSetting(    0x02, "5000" );
		in[2].setDipSetting(    0x03, "7000" );
		in[2].setDipSetting(    0x00, "None" );
		in[2].setDipName( 0x04, 0x00, DEF_STR[ Lives ] );
		in[2].setDipSetting(    0x00, "3" );
		in[2].setDipSetting(    0x04, "5" );
		in[2].setDipName( 0x08, 0x00, "Unused SW 0-3" );
		in[2].setDipSetting(    0x00, DEF_STR[ Off ] );
		in[2].setDipSetting(    0x08, DEF_STR[ On ] );
		in[2].setBit( 0xf0, IP_ACTIVE_HIGH, IPT_UNUSED );
		return in;
	}

	private InputPort[] ipt_scramblb() {
			/* IN0 */
		in[0].setBit( 0x01, IP_ACTIVE_HIGH, IPT_COIN1 );
		in[0].setBit( 0x02, IP_ACTIVE_HIGH, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_COCKTAIL );
		in[0].setBit( 0x04, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT  | IPF_8WAY );
		in[0].setBit( 0x08, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_8WAY );
		in[0].setBit( 0x10, IP_ACTIVE_HIGH, IPT_BUTTON1 );
		in[0].setBit( 0x20, IP_ACTIVE_HIGH, IPT_BUTTON2 );
		in[0].setBit( 0x40, IP_ACTIVE_HIGH, IPT_JOYSTICK_DOWN | IPF_8WAY );
		in[0].setBit( 0x80, IP_ACTIVE_HIGH, IPT_JOYSTICK_UP   | IPF_8WAY );

			/* IN1 */
		in[1].setBit( 0x01, IP_ACTIVE_HIGH, IPT_START1 );
		in[1].setBit( 0x02, IP_ACTIVE_HIGH, IPT_START2 );
		in[1].setBit( 0x04, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_COCKTAIL );
		in[1].setBit( 0x08, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_COCKTAIL );
		in[1].setBit( 0x10, IP_ACTIVE_HIGH, IPT_BUTTON1 | IPF_COCKTAIL );
		in[1].setBit( 0x20, IP_ACTIVE_HIGH, IPT_BUTTON2 | IPF_COCKTAIL );
		in[1].setDipName( 0x40, 0x00, DEF_STR[ Cabinet ] );
		in[1].setDipSetting(    0x00, DEF_STR[ Upright ] );
		in[1].setDipSetting(    0x40, DEF_STR[ Cocktail ] );
		in[1].setBit( 0x80, IP_ACTIVE_HIGH, IPT_JOYSTICK_UP | IPF_8WAY | IPF_COCKTAIL );

			/* IN2 */
		in[2].setDipName( 0x03, 0x00, DEF_STR[ Coinage ] );
		in[2].setDipSetting(    0x00, DEF_STR[ _1C_1C ] );
		in[2].setDipSetting(    0x00, DEF_STR[ _1C_2C ] );
		in[2].setDipSetting(    0x00, DEF_STR[ _1C_3C ] );
		in[2].setDipSetting(    0x00, DEF_STR[ _1C_4C ] );
		in[2].setDipName( 0x0c, 0x00, DEF_STR[ Lives ] );
		in[2].setDipSetting(    0x00, "3" );
		in[2].setDipSetting(    0x04, "4" );
		in[2].setDipSetting(    0x08, "5" );
		//in[2].setBitX( 0,       0x0c, IPT_DIPSWITCH_SETTING | IPF_CHEAT, "255", IP_KEY_NONE, IP_JOY_NONE );
		in[2].setDipName( 0x10, 0x00, DEF_STR[ Unknown ] );   /* probably unused */
		in[2].setDipSetting(    0x00, DEF_STR[ Off ] );
		in[2].setDipSetting(    0x10, DEF_STR[ On ] );
		in[2].setDipName( 0x20, 0x00, DEF_STR[ Unknown ] );   /* probably unused */
		in[2].setDipSetting(    0x00, DEF_STR[ Off ] );
		in[2].setDipSetting(    0x20, DEF_STR[ On ] );
		in[2].setDipName( 0x40, 0x00, DEF_STR[ Unknown ] );   /* probably unused */
		in[2].setDipSetting(    0x00, DEF_STR[ Off ] );
		in[2].setDipSetting(    0x40, DEF_STR[ On ] );
		in[2].setDipName( 0x80, 0x00, DEF_STR[ Unknown ] );   /* probably unused */
		in[2].setDipSetting(    0x00, DEF_STR[ Off ] );
		in[2].setDipSetting(    0x80, DEF_STR[ On ] );
		return in;
	}

	private InputPort[] ipt_warofbug() {
			  /* IN0 */
		in[0].setBit( 0x01, IP_ACTIVE_HIGH, IPT_COIN1 );
		in[0].setBit( 0x02, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x04, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT  | IPF_8WAY );
		in[0].setBit( 0x08, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_8WAY );
		in[0].setBit( 0x10, IP_ACTIVE_HIGH, IPT_BUTTON1 );
		in[0].setBit( 0x20, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x40, IP_ACTIVE_HIGH, IPT_JOYSTICK_DOWN | IPF_8WAY );
		in[0].setBit( 0x80, IP_ACTIVE_HIGH, IPT_JOYSTICK_UP   | IPF_8WAY );

			  /* IN1 */
		in[1].setBit( 0x01, IP_ACTIVE_HIGH, IPT_START1 );
		in[1].setBit( 0x02, IP_ACTIVE_HIGH, IPT_START2 );
		in[1].setBit( 0x04, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[1].setBit( 0x08, IP_ACTIVE_HIGH, IPT_COIN2 );
		in[1].setBit( 0x10, IP_ACTIVE_HIGH, IPT_COIN3 );
		in[1].setBit( 0x20, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[1].setDipName( 0xc0, 0x00, DEF_STR[ Coinage ] );
		in[1].setDipSetting(    0x40, DEF_STR[ _2C_1C ] );
		in[1].setDipSetting(    0x00, DEF_STR[ _1C_1C ] );
		in[1].setDipSetting(    0xc0, DEF_STR[ Free_Play ] );
	/* 0x80 gives 2 Coins/1 Credit */

			  /* DSW0 */
		in[2].setDipName( 0x03, 0x02, DEF_STR[ Lives ] );
		in[2].setDipSetting(    0x00, "1" );
		in[2].setDipSetting(    0x01, "2" );
		in[2].setDipSetting(    0x02, "3" );
		in[2].setDipSetting(    0x03, "4" );
		in[2].setDipName( 0x04, 0x00, DEF_STR[ Unknown ] );
		in[2].setDipSetting(    0x00, DEF_STR[ Off ] );
		in[2].setDipSetting(    0x04, DEF_STR[ On ] );
		in[2].setDipName( 0x08, 0x08, DEF_STR[ Bonus_Life ] );
		in[2].setDipSetting(    0x08, "500000" );
		in[2].setDipSetting(    0x00, "750000" );
		in[2].setBit( 0xf0, IP_ACTIVE_HIGH, IPT_UNUSED );
		return in;
	}

	private GfxLayout galaxian_charlayout() {

		int[] pOffs = { 256*8*8, 0 };
		int[] xOffs = { 0, 1, 2, 3, 4, 5, 6, 7 };
		int[] yOffs = { 0*8, 1*8, 2*8, 3*8, 4*8, 5*8, 6*8, 7*8 };

		return new GfxLayout(
			8,8,	/* 8*8 characters */
			256,	/* 512 characters */
			2,		/* 2 bits per pixel */
			pOffs,
			xOffs,
			yOffs,
			8*8	/* every char takes 16 consecutive bytes */
		);
	}

	private GfxLayout galaxian_spritelayout() {

		int[] pOffs = { 0, 256*8*8 };
		int[] xOffs = { 0, 1, 2, 3, 4, 5, 6, 7,
						8*8+0, 8*8+1, 8*8+2, 8*8+3, 8*8+4, 8*8+5, 8*8+6, 8*8+7 };
		int[] yOffs = { 0*8, 1*8, 2*8, 3*8, 4*8, 5*8, 6*8, 7*8,
						16*8, 17*8, 18*8, 19*8, 20*8, 21*8, 22*8, 23*8 };

		return new GfxLayout(
			16,16,	/* 8*8 sprites */
			64,	/* 64 sprites */
			2,	/* 4 bits per pixel */
			pOffs,
			xOffs,
			yOffs,
			32*8	/* every char takes 32 consecutive bytes */
		);
	}

	private GfxDecodeInfo[] galaxian_gfxdecodeinfo() {
		GfxDecodeInfo gdi[] = new GfxDecodeInfo[2];
		gdi[0] = new GfxDecodeInfo( REGION_GFX1, 0x0000, galaxian_charlayout(),    0, 8 );
		gdi[1] = new GfxDecodeInfo( REGION_GFX1, 0x0000, galaxian_spritelayout(),  0, 8 );
		return gdi;
	}

	private boolean rom_galaxian() {
		romLoader.setZip( "galaxian" );

		romLoader.setMemory( REGION_CPU1 );
		romLoader.loadROM( "galmidw.u",    0x0000, 0x0800, 0x745e2d61 );
		romLoader.loadROM( "galmidw.v",    0x0800, 0x0800, 0x9c999a40 );
		romLoader.loadROM( "galmidw.w",    0x1000, 0x0800, 0xb5894925 );
		romLoader.loadROM( "galmidw.y",    0x1800, 0x0800, 0x6b3ca10b );
		romLoader.loadROM( "7l",           0x2000, 0x0800, 0x1b933207 );

		romLoader.setMemory( REGION_GFX1 );
		romLoader.loadROM( "1h.bin",       0x0000, 0x0800, 0x39fb43a4 );
		romLoader.loadROM( "1k.bin",       0x0800, 0x0800, 0x7e3f56a2 );

		romLoader.setMemory( REGION_PROMS );
		romLoader.loadROM( "6l.bpr", 0x0000, 0x0020, 0xc3ac9467 );

		romLoader.loadZip(base_URL);

		return true;
	}

	private boolean rom_galaxiaj() {
		romLoader.setZip( "galaxiaj" );

		romLoader.setMemory( REGION_CPU1 );
		romLoader.loadROM( "7f.bin",       0x0000, 0x1000, 0x4335b1de );
		romLoader.loadROM( "7j.bin",       0x1000, 0x1000, 0x4e6f66a1 );
		romLoader.loadROM( "7l.bin",       0x2000, 0x0800, 0x5341d75a );

		romLoader.setMemory( REGION_GFX1 );
		romLoader.loadROM( "1h.bin",       0x0000, 0x0800, 0x39fb43a4 );
		romLoader.loadROM( "1k.bin",       0x0800, 0x0800, 0x7e3f56a2 );

		romLoader.setMemory( REGION_PROMS );
		romLoader.loadROM( "6l.bpr",       0x0000, 0x0020, 0xc3ac9467 );

		romLoader.loadZip(base_URL);

		return true;
	}

	private boolean rom_scramblb() {
		romLoader.setZip( "scramblb" );

		romLoader.setMemory( REGION_CPU1 );
		romLoader.loadROM( "scramble.1k",  0x0000, 0x0800, 0x9e025c4a );
		romLoader.loadROM( "scramble.2k",  0x0800, 0x0800, 0x306f783e );
		romLoader.loadROM( "scramble.3k",  0x1000, 0x0800, 0x0500b701 );
		romLoader.loadROM( "scramble.4k",  0x1800, 0x0800, 0xdd380a22 );
		romLoader.loadROM( "scramble.5k",  0x2000, 0x0800, 0xdf0b9648 );
		romLoader.loadROM( "scramble.1j",  0x2800, 0x0800, 0xb8c07b3c );
		romLoader.loadROM( "scramble.2j",  0x3000, 0x0800, 0x88ac07a0 );
		romLoader.loadROM( "scramble.3j",  0x3800, 0x0800, 0xc67d57ca );

		//romLoader.loadZip(base_URL);

		//romLoader.setZip( "scramble" );

		romLoader.setMemory( REGION_GFX1 );
		romLoader.loadROM( "5f.k",         0x0000, 0x0800, 0x4708845b );
		romLoader.loadROM( "5h.k",         0x0800, 0x0800, 0x11fd2887 );

		romLoader.setMemory( REGION_PROMS );
		romLoader.loadROM( "82s123.6e",    0x0000, 0x0020, 0x4e3caeab );

		romLoader.loadZip(base_URL);

		return true;
	}

	private boolean rom_warofbug() {
		romLoader.setZip( "warofbug" );

		romLoader.setMemory( REGION_CPU1 );
		romLoader.loadROM( "warofbug.u",   0x0000, 0x0800, 0xb8dfb7e3 );
		romLoader.loadROM( "warofbug.v",   0x0800, 0x0800, 0xfd8854e0 );
		romLoader.loadROM( "warofbug.w",   0x1000, 0x0800, 0x4495aa14 );
		romLoader.loadROM( "warofbug.y",   0x1800, 0x0800, 0xc14a541f );
		romLoader.loadROM( "warofbug.z",   0x2000, 0x0800, 0xc167fe55 );

		romLoader.setMemory( REGION_GFX1 );
		romLoader.loadROM( "warofbug.1k",  0x0000, 0x0800, 0x8100fa85 );
		romLoader.loadROM( "warofbug.1j",  0x0800, 0x0800, 0xd1220ae9 );

		romLoader.setMemory( REGION_PROMS );
		romLoader.loadROM( "warofbug.clr", 0x0000, 0x0020, 0x8688e64b );

		romLoader.loadZip(base_URL);

		return true;
	}

	public Machine getMachine(URL url, String name) {
		super.getMachine(url,name);
		super.setVideoEmulator(v);

		in[0] = new InputPort();
		in[1] = new InputPort();
		in[2] = new InputPort();

		input_port_0_r = (ReadHandler)in[0];
		input_port_1_r = (ReadHandler)in[1];
		input_port_2_r = (ReadHandler)in[2];


		if (name.equals("galaxian")) {
			this.md = machine_driver_galaxian();
			GAME(1979, rom_galaxian(), ipt_galaxian(), 	v.galaxian(), ROT90, "Namco", "Galaxian (Namco set 1)" );

		} else if (name.equals("galaxiaj")) {
			this.md = machine_driver_galaxian();
			GAME(1979, rom_galaxiaj(), "galaxian", ipt_superg(), 	v.galaxian(), ROT90, "Namco", "Galaxian (Namco set 2)" );

		} else if (name.equals("scramblb")) {
			this.md = machine_driver_scramblb();
			GAME(1981, rom_scramblb(), ipt_scramblb(), 	v.scramble(), ROT90, "bootleg", "Scramble (bootleg on Galaxian hardware)" );

		} else if (name.equals("warofbug")) {
			this.md = machine_driver_galaxian();
			GAME(1981, rom_warofbug(), ipt_warofbug(), 	v.galaxian(), ROT90, "Armenia", "War of the Bugs or Monsterous Manouvers in a Mushroom Maze" );

		}


		v.setRegions(REGION_PROMS, REGION_CPU1);

		m.init(md);
		return (Machine)m;
	}

}