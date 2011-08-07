/*
CottAGE - the Arcade Generic Emulator in Java

Java driver by Erik Duijs, Gollum
*/

/****************************************************************************/
/*                                                                          */
/*  Bw8080.java                                                             */
/*                                                                          */
/*  Michael Strutts, Nicola Salmoria, Tormod Tjaberg, Mirko Buffoni         */
/*  Lee Taylor, Valerio Verrando, Marco Cassili, Zsolt Vasvari and others   */
/*                                                                          */
/*                                                                          */
/*  Notes:                                                                  */
/*  -----                                                                   */
/*                                                                          */
/*  - "The Amazing Maze Game" on title screen, but manual, flyer,           */
/*    cabinet side art all call it just "Amazing Maze"                      */
/*                                                                          */
/*  - Desert Gun is also known as Road Runner                               */
/*                                                                          */
/*  - Space Invaders Deluxe still says Space Invaders Part II,              */
/*    because according to KLOV, Midway was only allowed to make minor      */
/*    modifications of the Taito code.  Read all about it here:             */
/*    http://www.klov.com/S/Space_Invaders_Deluxe.html                      */
/*                                                                          */
/*                                                                          */
/*  To Do:                                                                  */
/*  -----                                                                   */
/*                                                                          */
/*  - 4 Player Bowling has an offscreen display that show how many points   */
/*    the player will be rewarded for hitting the dot in the Flash game.    */
/*                                                                          */
/*  - Space Invaders Deluxe: overlay                                        */
/*                                                                          */
/*  - Space Encounters: 'trench' circuit                                    */
/*                                                                          */
/*  - Phantom II: verify clouds                                             */
/*                                                                          */
/*  - Helifire: analog wave and star background                             */
/*                                                                          */
/*  - Sheriff: overlay/color PROM                                           */
/*                                                                          */
/*                                                                          */
/*  Games confirmed not use an overlay (pure black and white):              */
/*  ---------------------------------------------------------               */
/*                                                                          */
/*  - 4 Player Bowling                                                      */
/*                                                                          */
/****************************************************************************/
/*                                                                          */
/* Change Log                                                               */
/*                                                                          */
/* 26 May 2001 - Following were renamed                                     */
/* galxwars -> galxwart - Galaxy Wars (c)1979 Taito, possible bootleg       */
/* spaceatt -> spaceat2 - Space Attack Part II                              */
/*                                                                          */
/* 26 May 2001 - Following were added                                       */
/* galxwars - Galaxy Wars (set 1) (c)1979 Universal                         */
/* galxwar2 - Galaxy Wars (set 2) (c)1979 Universal                         */
/* jspectr2 - Jatre Specter (set 2) (c)1979 Jatre                           */
/* ozmawar2 - Ozma Wars (set 2) (c)1979 SNK, on Taito 3 Colour Invaders BD  */
/* spaceatt - Space Attack (c)1978 Video Game GMBH                          */
/* sstrangr - Space Stranger (c)1978 Yachiyo Electronics, Ltd.              */
/*                                                                          */
/* 26 May 2001 - galxwars input port changed slightly so the new sets work  */
/*                                                                          */
/* ------------------------------------------------------------------------ */
/*                                                                          */
/* 30 July 2001 - sstrngr2 Added (c)1979 Yachiyo, Colour version of Space   */
/*                Stranger, board has Stranger 2 written on it              */
/****************************************************************************/

package cottage.drivers;

import java.net.URL;

import jef.cpu.Cpu;
import jef.cpu.I8080;
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
import jef.video.GfxManager;
import jef.video.Vh_convert_color_proms;
import jef.video.Vh_refresh;
import jef.video.Vh_start;
import jef.video.Vh_stop;

import cottage.mame.MAMEDriver;

public class Bw8080 extends MAMEDriver {


	int[] REGION_PROMS = new int[0x800];
	int[] memCpu1 = new int[0x10000];

	InputPort[] in = new InputPort[4];

	ReadHandler		input_port_0_r;
	ReadHandler		input_port_1_r;
	ReadHandler		input_port_2_r;
	ReadHandler		input_port_3_r;

	cottage.vidhrdw.Bw8080 v					= new cottage.vidhrdw.Bw8080();
	WriteHandler 	c8080bw_videoram_w				= v.c8080bw_videoram_w(memCpu1, v);
	WriteHandler 	invadpt2_videoram_w				= v.invadpt2_videoram_w(memCpu1, v);
	WriteHandler 	schaser_colorram_w				= v.schaser_colorram_w(memCpu1, v);
	Vh_refresh 		invaders_vh_screenrefresh 		= (Vh_refresh)v;
	Vh_start		invaders_vh_start				= (Vh_start)v;
	Vh_stop			invaders_vh_stop				= (Vh_stop)v;
	Eof_callback	noCallback						= (Eof_callback)v;
	Vh_convert_color_proms	invadpt2_vh_convert_color_prom	= v.invadpt2_vh_convert_color_prom(v);
	Vh_convert_color_proms	noConvertProms			= (Vh_convert_color_proms)v;

	cottage.machine.Bw8080 m					= new cottage.machine.Bw8080();
	WriteHandler 	c8080bw_shift_amount_w			= m.c8080bw_shift_amount_w(m);
	WriteHandler 	c8080bw_shift_data_w			= m.c8080bw_shift_data_w(m);
	WriteHandler 	invaders_sh_port3_w				= m.invaders_sh_port3_w(m);
	WriteHandler 	invaders_sh_port5_w				= m.invaders_sh_port5_w(m);
	ReadHandler	 	c8080bw_shift_data_r			= m.c8080bw_shift_data_r(m);
	ReadHandler	 	invaders_shift_data_rev_r		= m.invaders_shift_data_rev_r(m);
	ReadHandler	 	boothill_shift_data_r			= m.boothill_shift_data_r(m);
	InterruptHandler invaders_interrupt				= m.invaders_interrupt(m);

	/* Common Bw8080 functions */

	private MemoryReadAddress c8080bw_readmem() {
		MemoryReadAddress mra = new MemoryReadAddress(memCpu1);
		mra.setMR( 0x0000, 0x1fff, MRA_ROM );
		mra.setMR( 0x2000, 0x3fff, MRA_RAM );
		mra.setMR( 0x4000, 0x63ff, MRA_ROM );
		return mra;
	}

	private MemoryWriteAddress c8080bw_writemem() {
		MemoryWriteAddress mwa = new MemoryWriteAddress(memCpu1);
		mwa.setMW( 0x0000, 0x1fff, MWA_ROM );
		mwa.setMW( 0x2000, 0x23ff, MWA_RAM );
		mwa.set( 0x2400, 0x3fff, c8080bw_videoram_w );
		mwa.setMW( 0x4000, 0x63ff, MWA_ROM );
		return mwa;
	}

	private IOReadPort c8080bw_readport() {
		IOReadPort ior = new IOReadPort();
		ior.set( 0x00, 0x00, input_port_0_r );
		ior.set( 0x01, 0x01, input_port_1_r );
		ior.set( 0x02, 0x02, input_port_2_r );
		ior.set( 0x03, 0x03, c8080bw_shift_data_r );
		return ior;
	}

	private IOWritePort writeport_1_2() {
		IOWritePort	iow = new IOWritePort();
		iow.set( 0x01,	0x01, c8080bw_shift_amount_w );
		iow.set( 0x02,	0x02, c8080bw_shift_data_w );
		return iow;
	}

	private IOWritePort writeport_2_4() {
		IOWritePort	iow = new IOWritePort();
		iow.set( 0x02,	0x02, c8080bw_shift_amount_w );
		iow.set( 0x03,	0x03, invaders_sh_port3_w );
		iow.set( 0x04,	0x04, c8080bw_shift_data_w );
		iow.set( 0x05,	0x05, invaders_sh_port5_w );
		return iow;
	}

	private IOWritePort writeport_4_3() {
		IOWritePort	iow = new IOWritePort();
		iow.set( 0x03,	0x03, c8080bw_shift_data_w );
		iow.set( 0x04,	0x04, c8080bw_shift_amount_w );
		return iow;
	}


/*******************************************************/
/*                                                     */
/* Midway "Space Invaders"                             */
/*                                                     */
/*******************************************************/

	private InputPort[] ipt_invaders() {

		/* IN0 */
		in[0].setBit( 0x01, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x02, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x04, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x08, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x10, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x20, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x40, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x80, IP_ACTIVE_HIGH, IPT_UNKNOWN );

		/* IN1 */
		in[1].setBit( 0x01, IP_ACTIVE_LOW,  IPT_COIN1 );
		in[1].setBit( 0x02, IP_ACTIVE_HIGH, IPT_START2 );
		in[1].setBit( 0x04, IP_ACTIVE_HIGH, IPT_START1 );
		in[1].setBit( 0x08, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[1].setBit( 0x10, IP_ACTIVE_HIGH, IPT_BUTTON1 );
		in[1].setBit( 0x20, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT  | IPF_2WAY );
		in[1].setBit( 0x40, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_2WAY );
		in[1].setBit( 0x80, IP_ACTIVE_LOW,  IPT_UNKNOWN );	/* must be ACTIVE_HIGH Super Invaders */

		/* DSW0 */
		in[2].setDipName( 0x03, 0x00, DEF_STR[ Lives ] );
		in[2].setDipSetting(    0x00, "3" );
		in[2].setDipSetting(    0x01, "4" );
		in[2].setDipSetting(    0x02, "5" );
		in[2].setDipSetting(    0x03, "6" );
		in[2].setBit( 0x04, IP_ACTIVE_HIGH, IPT_TILT );
		in[2].setDipName( 0x08, 0x00, DEF_STR[ Bonus_Life ] );
		in[2].setDipSetting(    0x08, "1000" );
		in[2].setDipSetting(    0x00, "1500" );
		in[2].setBit( 0x10, IP_ACTIVE_HIGH, IPT_BUTTON1 | IPF_PLAYER2 );
		in[2].setBit( 0x20, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT  | IPF_2WAY | IPF_PLAYER2 );
		in[2].setBit( 0x40, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_2WAY | IPF_PLAYER2 );
		in[2].setDipName( 0x80, 0x00, "Coin Info" );
		in[2].setDipSetting(    0x80, DEF_STR[ Off ] );
		in[2].setDipSetting(    0x00, DEF_STR[ On ] );

		/* TODO: Cocktail mode support */
		return in;
	}

	public MachineDriver machine_driver_invaders() {
		CpuDriver[] cpuDriver = new CpuDriver[1];

		cpuDriver[0] = new CpuDriver( (Cpu) new I8080(), 2000000,        /* 2 MHz? */
										c8080bw_readmem(), c8080bw_writemem(),
										c8080bw_readport(), writeport_2_4(),
										invaders_interrupt, 2 );    /* two interrupts per frame */

		int[] visibleArea = { 0*8, 32*8-1, 0*8, 28*8-1 };

		return new MachineDriver
		(
			cpuDriver,

			60, DEFAULT_60HZ_VBLANK_DURATION,
			1,
			NOP,

			32*8, 32*8,	visibleArea,

			NO_GFX_DECODE_INFO,
			256, 0,

			noConvertProms,

			VIDEO_TYPE_RASTER | GfxManager.VIDEO_SUPPORTS_DIRTY,

			noCallback,
			invaders_vh_start,
			invaders_vh_stop,
			invaders_vh_screenrefresh,
			noSound

		);
	}


/*******************************************************/
/*                                                     */
/* Space Invaders TV Version (Taito)                   */
/*                                                     */
/*LT 24-12-1998                                        */
/*******************************************************/

/* same as Invaders with a test mode switch */

	private InputPort[] ipt_sitv() {

		/* TEST MODE */
		//PORT_SERVICE( 0x01, IP_ACTIVE_LOW )
		in[0].setBit( 0x02, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x04, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x08, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x10, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x20, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x40, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x80, IP_ACTIVE_HIGH, IPT_UNKNOWN );

		/* IN1 */
		in[1].setBit( 0x01, IP_ACTIVE_LOW,  IPT_COIN1 );
		in[1].setBit( 0x02, IP_ACTIVE_HIGH, IPT_START2 );
		in[1].setBit( 0x04, IP_ACTIVE_HIGH, IPT_START1 );
		in[1].setBit( 0x08, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[1].setBit( 0x10, IP_ACTIVE_HIGH, IPT_BUTTON1 );
		in[1].setBit( 0x20, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT  | IPF_2WAY );
		in[1].setBit( 0x40, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_2WAY );
		in[1].setBit( 0x80, IP_ACTIVE_LOW,  IPT_UNKNOWN );

		/* DSW0 */
		in[2].setDipName( 0x03, 0x00, DEF_STR[ Lives ] );
		in[2].setDipSetting(    0x00, "3" );
		in[2].setDipSetting(    0x01, "4" );
		in[2].setDipSetting(    0x02, "5" );
		in[2].setDipSetting(    0x03, "6" );
		in[2].setBit( 0x04, IP_ACTIVE_HIGH, IPT_TILT );
		in[2].setDipName( 0x08, 0x00, DEF_STR[ Bonus_Life ] );
		in[2].setDipSetting(    0x08, "1000" );
		in[2].setDipSetting(    0x00, "1500" );
		in[2].setBit( 0x10, IP_ACTIVE_HIGH, IPT_BUTTON1 | IPF_PLAYER2 );
		in[2].setBit( 0x20, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT  | IPF_2WAY | IPF_PLAYER2 );
		in[2].setBit( 0x40, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_2WAY | IPF_PLAYER2 );
		in[2].setDipName( 0x80, 0x00, "Coin Info" );
		in[2].setDipSetting(    0x80, DEF_STR[ Off ] );
		in[2].setDipSetting(    0x00, DEF_STR[ On ] );

		/* Dummy port for cocktail mode */
		//in[3].setDipName( 0x01, 0x00, DEF_STR[ Cabinet ] );
		//in[3].setDipSetting(    0x00, DEF_STR[ Upright ] );
		//in[3].setDipSetting(    0x01, DEF_STR[ Cocktail ] );
		return in;
	}


/*******************************************************/
/*                                                     */
/* Midway "Space Invaders Part II"                     */
/*                                                     */
/*******************************************************/

	private MemoryWriteAddress invadpt2_writemem() {
		MemoryWriteAddress mwa = new MemoryWriteAddress(memCpu1);
		mwa.setMW( 0x0000, 0x1fff, MWA_ROM );
		mwa.setMW( 0x2000, 0x23ff, MWA_RAM );
		mwa.set( 0x2400, 0x3fff, invadpt2_videoram_w );
		mwa.setMW( 0x4000, 0x5fff, MWA_ROM );
		return mwa;
	}

	private InputPort[] ipt_invadpt2() {
		/* IN0 */
		in[0].setBit( 0x01, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x02, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x04, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x08, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x10, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x20, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x40, IP_ACTIVE_HIGH, IPT_UNKNOWN );	/* otherwise high score entry ends right away */
		in[0].setBit( 0x80, IP_ACTIVE_HIGH, IPT_UNKNOWN );

		/* IN1 */
		in[1].setBit( 0x01, IP_ACTIVE_LOW,  IPT_COIN1 );
		in[1].setBit( 0x02, IP_ACTIVE_HIGH, IPT_START2 );
		in[1].setBit( 0x04, IP_ACTIVE_HIGH, IPT_START1 );
		in[1].setBit( 0x08, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[1].setBit( 0x10, IP_ACTIVE_HIGH, IPT_BUTTON1 );
		in[1].setBit( 0x20, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT  | IPF_2WAY );
		in[1].setBit( 0x40, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_2WAY );
		in[1].setBit( 0x80, IP_ACTIVE_LOW,  IPT_UNKNOWN );

		/* DSW0 */
		in[2].setDipName( 0x01, 0x00, DEF_STR[ Lives ] );
		in[2].setDipSetting(    0x00, "3" );
		in[2].setDipSetting(    0x01, "4" );
		in[2].setDipName( 0x02, 0x00, DEF_STR[ Unknown ] );
		in[2].setDipSetting(    0x00, DEF_STR[ Off ] );
		in[2].setDipSetting(    0x02, DEF_STR[ On ] );
		in[2].setBit( 0x04, IP_ACTIVE_HIGH, IPT_TILT );
		in[2].setDipName( 0x08, 0x00, "Preset Mode" );
		in[2].setDipSetting(    0x00, DEF_STR[ Off ] );
		in[2].setDipSetting(    0x08, DEF_STR[ On ] );
		in[2].setBit( 0x10, IP_ACTIVE_HIGH, IPT_BUTTON1 | IPF_PLAYER2 );
		in[2].setBit( 0x20, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT  | IPF_2WAY | IPF_PLAYER2 );
		in[2].setBit( 0x40, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_2WAY | IPF_PLAYER2 );
		in[2].setDipName( 0x80, 0x00, "Coin Info" );
		in[2].setDipSetting(    0x80, DEF_STR[ Off ] );
		in[2].setDipSetting(    0x00, DEF_STR[ On ] );

		/* Dummy port for cocktail mode */
		//in[0].setDipName( 0x01, 0x00, DEF_STR[ Cabinet ] );
		//in[0].setDipSetting(    0x00, DEF_STR[ Upright ] );
		//in[0].setDipSetting(    0x01, DEF_STR[ Cocktail ] );
		return in;
	}


/* same as regular invaders, but with a color board added */

	public MachineDriver machine_driver_invadpt2() {
		CpuDriver[] cpuDriver = new CpuDriver[1];

		cpuDriver[0] = new CpuDriver( (Cpu) new I8080(), 2000000,        /* 2 Mhz? */
										c8080bw_readmem(), c8080bw_writemem(),
										c8080bw_readport(), writeport_2_4(),
										invaders_interrupt, 2 );    /* two interrupts per frame */

		int[] visibleArea = { 0*8, 32*8-1, 0*8, 28*8-1 };

		return new MachineDriver
		(
			cpuDriver,

			60, DEFAULT_60HZ_VBLANK_DURATION,
			1,
			NOP,

			32*8, 32*8, visibleArea,
			NO_GFX_DECODE_INFO,
			8, 0,
			invadpt2_vh_convert_color_prom,

			VIDEO_TYPE_RASTER | GfxManager.VIDEO_SUPPORTS_DIRTY,

			noCallback,
			invaders_vh_start,
			invaders_vh_stop,
			invaders_vh_screenrefresh,
			noSound

		);
	}


/*******************************************************/
/*                                                     */
/* ?????? "Super Earth Invasion"                       */
/*                                                     */
/*******************************************************/

	private InputPort[] ipt_earthinv() {
		/* IN0 */
		in[0].setBit( 0x01, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x02, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x04, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x08, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x10, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x20, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x40, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x80, IP_ACTIVE_HIGH, IPT_UNKNOWN );

		/* IN1 */
		in[1].setBit( 0x01, IP_ACTIVE_LOW,  IPT_COIN1 );
		in[1].setBit( 0x02, IP_ACTIVE_HIGH, IPT_START2 );
		in[1].setBit( 0x04, IP_ACTIVE_HIGH, IPT_START1 );
		in[1].setBit( 0x08, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[1].setBit( 0x10, IP_ACTIVE_HIGH, IPT_BUTTON1 );
		in[1].setBit( 0x20, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT  | IPF_2WAY );
		in[1].setBit( 0x40, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_2WAY );
		in[1].setBit( 0x80, IP_ACTIVE_LOW,  IPT_UNKNOWN );

		/* DSW0 */
		in[2].setDipName( 0x01, 0x01, DEF_STR[ Lives ] );
		in[2].setDipSetting(    0x00, "2" );
		in[2].setDipSetting(    0x01, "3" );
		in[2].setDipName( 0x02, 0x02, "Pence Coinage" );
		in[2].setDipSetting(    0x02, DEF_STR[ Off ] );
		in[2].setDipSetting(    0x00, DEF_STR[ On ] );
		in[2].setBit( 0x04, IP_ACTIVE_HIGH, IPT_COIN2 ); /* Pence Coin */
		in[2].setDipName( 0x08, 0x08, DEF_STR[ Unknown ] ); /* Not bonus */
		in[2].setDipSetting(    0x00, DEF_STR[ Off ] );
		in[2].setDipSetting(    0x08, DEF_STR[ On ] );
		in[2].setBit( 0x10, IP_ACTIVE_HIGH, IPT_BUTTON1 | IPF_PLAYER2 );
		in[2].setBit( 0x20, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT  | IPF_2WAY | IPF_PLAYER2 );
		in[2].setBit( 0x40, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_2WAY | IPF_PLAYER2 );
		in[2].setDipName( 0x80, 0x80, DEF_STR[ Coinage ] );
		in[2].setDipSetting(    0x00, "2C/1C 50p/3C (+ Bonus Life)" );
		in[2].setDipSetting(    0x80, "1C/1C 50p/5C" );

		/* Dummy port for cocktail mode */
		in[3].setDipName( 0x01, 0x00, DEF_STR[ Cabinet ] );
		in[3].setDipSetting(    0x00, DEF_STR[ Upright ] );
		in[3].setDipSetting(    0x01, DEF_STR[ Cocktail ] );
		return in;
	}


/*******************************************************/
/*                                                     */
/* ?????? "Space Attack II"                            */
/*                                                     */
/*******************************************************/

	private InputPort[] ipt_spaceatt() {
		/* IN0 */
		in[0].setBit( 0x01, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x02, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x04, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x08, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x10, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x20, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x40, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x80, IP_ACTIVE_HIGH, IPT_UNKNOWN );

		/* IN1 */
		in[1].setBit( 0x01, IP_ACTIVE_HIGH, IPT_COIN1 );
		in[1].setBit( 0x02, IP_ACTIVE_HIGH, IPT_START2 );
		in[1].setBit( 0x04, IP_ACTIVE_HIGH, IPT_START1 );
		in[1].setBit( 0x08, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[1].setBit( 0x10, IP_ACTIVE_HIGH, IPT_BUTTON1 );
		in[1].setBit( 0x20, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT  | IPF_2WAY );
		in[1].setBit( 0x40, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_2WAY );
		in[1].setBit( 0x80, IP_ACTIVE_HIGH, IPT_UNKNOWN );

		/* DSW0 */
		in[2].setDipName( 0x03, 0x00, DEF_STR[ Lives ] );
		in[2].setDipSetting(    0x00, "3" );
		in[2].setDipSetting(    0x01, "4" );
		in[2].setDipSetting(    0x02, "5" );
		in[2].setDipSetting(    0x03, "6" );
		in[2].setBit( 0x04, IP_ACTIVE_HIGH, IPT_TILT );
		in[2].setDipName( 0x08, 0x00, DEF_STR[ Bonus_Life ] );
		in[2].setDipSetting(    0x08, "1000" );
		in[2].setDipSetting(    0x00, "1500" );
		in[2].setDipName( 0x10, 0x00, DEF_STR[ Unknown ] );
		in[2].setDipSetting(    0x10, DEF_STR[ Off ] );
		in[2].setDipSetting(    0x00, DEF_STR[ On ] );
		in[2].setDipName( 0x20, 0x00, DEF_STR[ Unknown ] );
		in[2].setDipSetting(    0x20, DEF_STR[ Off ] );
		in[2].setDipSetting(    0x00, DEF_STR[ On ] );
		in[2].setDipName( 0x40, 0x00, DEF_STR[ Unknown ] );
		in[2].setDipSetting(    0x40, DEF_STR[ Off ] );
		in[2].setDipSetting(    0x00, DEF_STR[ On ] );
		in[2].setDipName( 0x80, 0x00, DEF_STR[ Coinage ] );
		in[2].setDipSetting(    0x80, DEF_STR[ _2C_1C ] );
		in[2].setDipSetting(    0x00, DEF_STR[ _1C_1C ] );

		/* Dummy port for cocktail mode (not used) */
		in[3].setBit( 0xff, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		return in;
	}


/*******************************************************/
/*                                                     */
/* Zenitone Microsec "Invaders Revenge"                */
/*                                                     */
/*******************************************************/

	private InputPort[] ipt_invrvnge() {
		/* IN0 */
		in[0].setBit( 0x01, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x02, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x04, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x08, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x10, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x20, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x40, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x80, IP_ACTIVE_HIGH, IPT_UNKNOWN );

		/* IN1 */
		in[1].setBit( 0x01, IP_ACTIVE_LOW,  IPT_COIN1 );
		in[1].setBit( 0x02, IP_ACTIVE_HIGH, IPT_START2 );
		in[1].setBit( 0x04, IP_ACTIVE_HIGH, IPT_START1 );
		in[1].setBit( 0x08, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[1].setBit( 0x10, IP_ACTIVE_HIGH, IPT_BUTTON1 );
		in[1].setBit( 0x20, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT  | IPF_2WAY );
		in[1].setBit( 0x40, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_2WAY );
		in[1].setBit( 0x80, IP_ACTIVE_LOW,  IPT_UNKNOWN );

		/* DSW0 */
		in[2].setDipName( 0x03, 0x00, DEF_STR[ Lives ] );
		in[2].setDipSetting(    0x00, "3" );
		in[2].setDipSetting(    0x01, "4" );
		in[2].setDipSetting(    0x02, "5" );
		in[2].setDipSetting(    0x03, "6" );
		in[2].setBit( 0x04, IP_ACTIVE_HIGH, IPT_TILT );
		in[2].setDipName( 0x08, 0x00, DEF_STR[ Unknown ] );
		in[2].setDipSetting(    0x08, DEF_STR[ Off ] );
		in[2].setDipSetting(    0x00, DEF_STR[ On ] );
		in[2].setBit( 0x10, IP_ACTIVE_HIGH, IPT_BUTTON1 | IPF_PLAYER2 );
		in[2].setBit( 0x20, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT  | IPF_2WAY | IPF_PLAYER2 );
		in[2].setBit( 0x40, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_2WAY | IPF_PLAYER2 );
		in[2].setDipName( 0x80, 0x00, DEF_STR[ Coinage ] );
		in[2].setDipSetting(    0x80, DEF_STR[ _2C_1C ] );
		in[2].setDipSetting(    0x00, DEF_STR[ _1C_1C ] );

		/* Dummy port for cocktail mode */
		in[3].setDipName( 0x01, 0x00, DEF_STR[ Cabinet ] );
		in[3].setDipSetting(    0x00, DEF_STR[ Upright ] );
		in[3].setDipSetting(    0x01, DEF_STR[ Cocktail ] );
		return in;
	}


/*******************************************************/
/*                                                     */
/* Emag "Super Invaders"                               */
/*                                                     */
/*******************************************************/

	private InputPort[] ipt_sinvemag() {
		/* IN0 */
		in[0].setBit( 0x01, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x02, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x04, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x08, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x10, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x20, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x40, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x80, IP_ACTIVE_HIGH, IPT_UNKNOWN );

		/* IN1 */
		in[1].setBit( 0x01, IP_ACTIVE_LOW,  IPT_COIN1 );
		in[1].setBit( 0x02, IP_ACTIVE_HIGH, IPT_START2 );
		in[1].setBit( 0x04, IP_ACTIVE_HIGH, IPT_START1 );
		in[1].setBit( 0x08, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[1].setBit( 0x10, IP_ACTIVE_HIGH, IPT_BUTTON1 );
		in[1].setBit( 0x20, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT  | IPF_2WAY );
		in[1].setBit( 0x40, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_2WAY );
		in[1].setBit( 0x80, IP_ACTIVE_LOW,  IPT_UNKNOWN );

		/* DSW0 */
		in[2].setDipName( 0x03, 0x00, DEF_STR[ Lives ] );
		in[2].setDipSetting(    0x00, "3" );
		in[2].setDipSetting(    0x01, "4" );
		in[2].setDipSetting(    0x02, "5" );
		in[2].setDipSetting(    0x03, "6" );
		in[2].setDipName( 0x04, 0x00, DEF_STR[ Unknown ] );
		in[2].setDipSetting(    0x04, DEF_STR[ Off ] );
		in[2].setDipSetting(    0x00, DEF_STR[ On ] );
		in[2].setDipName( 0x08, 0x00, DEF_STR[ Bonus_Life ] );
		in[2].setDipSetting(    0x08, "1000" );
		in[2].setDipSetting(    0x00, "1500" );
		in[2].setBit( 0x10, IP_ACTIVE_HIGH, IPT_BUTTON1 | IPF_PLAYER2 );
		in[2].setBit( 0x20, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT  | IPF_2WAY | IPF_PLAYER2 );
		in[2].setBit( 0x40, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_2WAY | IPF_PLAYER2 );
		in[2].setDipName( 0x80, 0x00, "Coin Info" );
		in[2].setDipSetting(    0x80, DEF_STR[ Off ] );
		in[2].setDipSetting(    0x00, DEF_STR[ On ] );

		/* Dummy port for cocktail mode */
		in[3].setDipName( 0x01, 0x00, DEF_STR[ Cabinet ] );
		in[3].setDipSetting(    0x00, DEF_STR[ Upright ] );
		in[3].setDipSetting(    0x01, DEF_STR[ Cocktail ] );
		return in;
	}


/*******************************************************/
/*                                                     */
/* Jatre Specter (Taito?)                              */
/*                                                     */
/*******************************************************/

	private InputPort[] ipt_jspecter() {
		/* IN0 */
		in[0].setBit( 0x01, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x02, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x04, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x08, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x10, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x20, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x40, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x80, IP_ACTIVE_HIGH, IPT_UNKNOWN );

		/* IN1 */
		in[1].setBit( 0x01, IP_ACTIVE_LOW,  IPT_COIN1 );
		in[1].setBit( 0x02, IP_ACTIVE_HIGH, IPT_START2 );
		in[1].setBit( 0x04, IP_ACTIVE_HIGH, IPT_START1 );
		in[1].setBit( 0x08, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[1].setBit( 0x10, IP_ACTIVE_HIGH, IPT_BUTTON1 );
		in[1].setBit( 0x20, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT  | IPF_2WAY );
		in[1].setBit( 0x40, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_2WAY );
		in[1].setDipName( 0x80, 0x00, DEF_STR[ Difficulty ] );
		in[1].setDipSetting(    0x80, "Easy" );
		in[1].setDipSetting(    0x00, "Hard" );

		/* DSW0 */
		in[2].setDipName( 0x03, 0x00, DEF_STR[ Lives ] );
		in[2].setDipSetting(    0x00, "3" );
		in[2].setDipSetting(    0x01, "4" );
		in[2].setDipSetting(    0x02, "5" );
		in[2].setDipSetting(    0x03, "6" );
		in[2].setDipName( 0x04, 0x00, DEF_STR[ Unknown ] );
		in[2].setDipSetting(    0x04, DEF_STR[ Off ] );
		in[2].setDipSetting(    0x00, DEF_STR[ On ] );
		in[2].setDipName( 0x08, 0x00, DEF_STR[ Bonus_Life ] );
		in[2].setDipSetting(    0x08, "1000" );
		in[2].setDipSetting(    0x00, "1500" );
		in[2].setBit( 0x10, IP_ACTIVE_HIGH, IPT_BUTTON1 | IPF_PLAYER2 );
		in[2].setBit( 0x20, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT  | IPF_2WAY | IPF_PLAYER2 );
		in[2].setBit( 0x40, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_2WAY | IPF_PLAYER2 );
		in[2].setDipName( 0x80, 0x00, "Coin Info" );
		in[2].setDipSetting(    0x80, DEF_STR[ Off ] );
		in[2].setDipSetting(    0x00, DEF_STR[ On ] );

		/* Dummy port for cocktail mode */
		in[3].setDipName( 0x01, 0x00, DEF_STR[ Cabinet ] );
		in[3].setDipSetting(    0x00, DEF_STR[ Upright ] );
		in[3].setDipSetting(    0x01, DEF_STR[ Cocktail ] );
		return in;
	}


	private InputPort[] ipt_spceking() {
		/* IN0 */
		in[0].setBit( 0x01, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x02, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x04, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x08, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x10, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x20, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x40, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x80, IP_ACTIVE_HIGH, IPT_UNKNOWN );

		/* IN1 */
		in[1].setBit( 0x01, IP_ACTIVE_LOW,  IPT_COIN1 );
		in[1].setBit( 0x02, IP_ACTIVE_HIGH, IPT_START2 );
		in[1].setBit( 0x04, IP_ACTIVE_HIGH, IPT_START1 );
		in[1].setBit( 0x08, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[1].setBit( 0x10, IP_ACTIVE_HIGH, IPT_BUTTON1 );
		in[1].setBit( 0x20, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT  | IPF_2WAY );
		in[1].setBit( 0x40, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_2WAY );
		in[1].setBit( 0x80, IP_ACTIVE_LOW,  IPT_UNKNOWN );

		/* DSW0 */
		in[2].setDipName( 0x03, 0x00, DEF_STR[ Lives ] );
		in[2].setDipSetting(    0x00, "3" );
		in[2].setDipSetting(    0x01, "4" );
		in[2].setDipSetting(    0x02, "5" );
		in[2].setDipSetting(    0x03, "6" );
		in[2].setDipName( 0x04, 0x00, DEF_STR[ Unknown ] );
		in[2].setDipSetting(    0x04, DEF_STR[ Off ] );
		in[2].setDipSetting(    0x00, DEF_STR[ On ] );
		in[2].setDipName( 0x08, 0x00, "High Score Preset Mode" );
		in[2].setDipSetting(    0x00, DEF_STR[ Off ] );
		in[2].setDipSetting(    0x08, DEF_STR[ On ] );
		in[2].setBit( 0x10, IP_ACTIVE_HIGH, IPT_BUTTON1 | IPF_PLAYER2 );
		in[2].setBit( 0x20, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT  | IPF_2WAY | IPF_PLAYER2 );
		in[2].setBit( 0x40, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_2WAY | IPF_PLAYER2 );
		in[2].setDipName( 0x80, 0x00, "Coin Info" );
		in[2].setDipSetting(    0x80, DEF_STR[ Off ] );
		in[2].setDipSetting(    0x00, DEF_STR[ On ] );

		/* Dummy port for cocktail mode */
		in[3].setDipName( 0x01, 0x00, DEF_STR[ Cabinet ] );
		in[3].setDipSetting(    0x00, DEF_STR[ Upright ] );
		in[3].setDipSetting(    0x01, DEF_STR[ Cocktail ] );
		return in;
	}


/*******************************************************/
/*                                                     */
/* Midway "Space Invaders II Cocktail"                 */
/*                                                     */
/*******************************************************/

	private IOWritePort invad2ct_writeport() {
		IOWritePort	iow = new IOWritePort();
		//iow.set( 0x01, 0x01, invad2ct_sh_port1_w );
		iow.set( 0x02, 0x02, c8080bw_shift_amount_w );
		//iow.set( 0x03, 0x03, invaders_sh_port3_w );
		iow.set( 0x04, 0x04, c8080bw_shift_data_w );
		//iow.set( 0x05, 0x05, invaders_sh_port5_w );
		//iow.set( 0x07, 0x07, invad2ct_sh_port7_w );
		return iow;
	}

	private InputPort[] ipt_invad2ct() {

		in[0].setService(0x01, IP_ACTIVE_LOW); 			  /* dip 8 */
		in[0].setBit( 0x02, IP_ACTIVE_HIGH, IPT_UNKNOWN ); /* tied to pull-down */
		in[0].setBit( 0x04, IP_ACTIVE_LOW, IPT_UNKNOWN );  /* tied to pull-up */
		in[0].setBit( 0x08, IP_ACTIVE_HIGH, IPT_UNKNOWN ); /* tied to pull-down */
		in[0].setBit( 0x10, IP_ACTIVE_LOW, IPT_UNKNOWN );  /* tied to pull-up */
		in[0].setBit( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );  /* tied to pull-up */
		in[0].setBit( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );  /* labelled reset but tied to pull-up */
		in[0].setBit( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );  /* tied to pull-up */


		in[1].setBit( 0x01, IP_ACTIVE_LOW,  IPT_COIN1 );
		in[1].setBit( 0x02, IP_ACTIVE_HIGH, IPT_START2 );
		in[1].setBit( 0x04, IP_ACTIVE_HIGH, IPT_START1 );
		in[1].setBit( 0x08, IP_ACTIVE_HIGH, IPT_UNKNOWN ); /* tied to pull-down */
		in[1].setBit( 0x10, IP_ACTIVE_HIGH, IPT_BUTTON1 );
		in[1].setBit( 0x20, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT  | IPF_2WAY );
		in[1].setBit( 0x40, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT | IPF_2WAY );
		in[1].setBit( 0x80, IP_ACTIVE_LOW,  IPT_UNKNOWN );  /* tied to pull-up */

		in[2].setDipName( 0x03, 0x00, DEF_STR[ Coinage ] ); /* dips 4 & 3 */
		in[2].setDipSetting(    0x02, DEF_STR[ _2C_1C ] );
		in[2].setDipSetting(    0x03, DEF_STR[ _2C_2C ] );
		in[2].setDipSetting(    0x00, DEF_STR[ _1C_1C ] );
		in[2].setDipSetting(    0x01, DEF_STR[ _1C_2C ] );
		in[2].setBit( 0x04, IP_ACTIVE_LOW, IPT_UNKNOWN );   /* tied to pull-up */
		in[2].setDipName( 0x08, 0x08, DEF_STR[ Unknown ] ); /* dip 2 */
		in[2].setDipSetting(    0x08, DEF_STR[ Off ] );
		in[2].setDipSetting(    0x00, DEF_STR[ On ] );
		in[2].setBit( 0x10, IP_ACTIVE_HIGH, IPT_BUTTON1 | IPF_COCKTAIL );
		in[2].setBit( 0x20, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT  | IPF_2WAY | IPF_COCKTAIL );
		in[2].setBit( 0x40, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_2WAY | IPF_COCKTAIL );
		in[2].setDipName( 0x80, 0x80, DEF_STR[ Bonus_Life ] ); /* dip 1 */
		in[2].setDipSetting(    0x80, "1500" );
		in[2].setDipSetting(    0x00, "2000" );

		/* Dummy port for cocktail mode (not used) */
		in[3].setBit( 0xff, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		return in;
	}

	public MachineDriver machine_driver_invad2ct() {
		CpuDriver[] cpuDriver = new CpuDriver[1];

		cpuDriver[0] = new CpuDriver( (Cpu) new I8080(),
										1996800,        /* 2 Mhz? */
										c8080bw_readmem(), c8080bw_writemem(), c8080bw_readport(), invad2ct_writeport(),
										invaders_interrupt, 2 );

		int[] visibleArea = { 0*8, 32*8-1, 0*8, 28*8-1 };

		return new MachineDriver
		(
			cpuDriver,

			60, DEFAULT_60HZ_VBLANK_DURATION,
			1,
			NOP,

			32*8, 32*8, visibleArea,
			NO_GFX_DECODE_INFO,
			256, 0,

			noConvertProms,

			VIDEO_TYPE_RASTER | GfxManager.VIDEO_SUPPORTS_DIRTY,

			noCallback,
			invaders_vh_start,
			invaders_vh_stop,
			invaders_vh_screenrefresh,
			noSound

		);
	}

/*******************************************************/
/*                                                     */
/* Taito "Space Laser"                                 */
/*                                                     */
/*******************************************************/

	private InputPort[] ipt_spclaser() {
			  /* IN0 */
		in[0].setBit( 0x01, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x02, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x04, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x08, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x10, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x20, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x40, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x80, IP_ACTIVE_HIGH, IPT_UNKNOWN );

			  /* IN1 */
		in[1].setBit( 0x01, IP_ACTIVE_HIGH, IPT_COIN1 );
		in[1].setBit( 0x02, IP_ACTIVE_HIGH, IPT_START2 );
		in[1].setBit( 0x04, IP_ACTIVE_HIGH, IPT_START1 );
		in[1].setBit( 0x08, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[1].setBit( 0x10, IP_ACTIVE_HIGH, IPT_BUTTON1 );
		in[1].setBit( 0x20, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT  | IPF_2WAY );
		in[1].setBit( 0x40, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_2WAY );
		in[1].setBit( 0x80, IP_ACTIVE_HIGH, IPT_UNKNOWN );

			  /* DSW0 */
		in[2].setDipName( 0x01, 0x00, DEF_STR[ Lives ] );
		in[2].setDipSetting(    0x00, "3" );
		in[2].setDipSetting(    0x01, "4" );
		in[2].setDipName( 0x02, 0x00, DEF_STR[ Unknown ] );
		in[2].setDipSetting(    0x00, DEF_STR[ Off ] );
		in[2].setDipSetting(    0x02, DEF_STR[ On ] );
		in[2].setDipName( 0x04, 0x00, DEF_STR[ Unknown ] );
		in[2].setDipSetting(    0x04, DEF_STR[ Off ] );
		in[2].setDipSetting(    0x00, DEF_STR[ On ] );
		in[2].setDipName( 0x08, 0x00, "Preset Mode" );
		in[2].setDipSetting(    0x00, DEF_STR[ Off ] );
		in[2].setDipSetting(    0x08, DEF_STR[ On ] );
		in[2].setBit( 0x10, IP_ACTIVE_HIGH, IPT_BUTTON1 | IPF_PLAYER2 );
		in[2].setBit( 0x20, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT  | IPF_2WAY | IPF_PLAYER2 );
		in[2].setBit( 0x40, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_2WAY | IPF_PLAYER2 );
		in[2].setDipName( 0x80, 0x00, DEF_STR[ Unknown ] );
		in[2].setDipSetting(    0x80, DEF_STR[ Off ] );
		in[2].setDipSetting(    0x00, DEF_STR[ On ] );

				/* Dummy port for cocktail mode (not used) */
		in[3].setBit( 0xff, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		return in;
	}


/*******************************************************/
/*                                                     */
/* Space War Part 3                                    */
/*                                                     */
/* Added 21/11/1999 By LT                              */
/* Thanks to Peter Fyfe for machine info               */
/*******************************************************/

	private InputPort[] ipt_spacewr3() {
		/* IN0 */
		in[0].setBit( 0x01, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x02, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x04, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x08, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x10, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x20, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x40, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x80, IP_ACTIVE_HIGH, IPT_UNKNOWN );

		/* IN1 */
		in[1].setBit( 0x01, IP_ACTIVE_LOW,  IPT_COIN1 );
		in[1].setBit( 0x02, IP_ACTIVE_HIGH, IPT_START2 );
		in[1].setBit( 0x04, IP_ACTIVE_HIGH, IPT_START1 );
		in[1].setBit( 0x08, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[1].setBit( 0x10, IP_ACTIVE_HIGH, IPT_BUTTON1 );
		in[1].setBit( 0x20, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT  | IPF_2WAY );
		in[1].setBit( 0x40, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_2WAY );
		in[1].setBit( 0x80, IP_ACTIVE_LOW,  IPT_UNKNOWN );

		/* DSW0 */
		in[2].setDipName( 0x01, 0x00, DEF_STR[ Lives ] );
		in[2].setDipSetting(    0x00, "3" );
		in[2].setDipSetting(    0x01, "4" );
		in[2].setBit( 0x02, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[2].setDipName( 0x04, 0x00, DEF_STR[ Unknown ] );
		in[2].setDipSetting(    0x04, DEF_STR[ Off ] );
		in[2].setDipSetting(    0x00, DEF_STR[ On ] );
		in[2].setDipName( 0x08, 0x00, DEF_STR[ Bonus_Life ] );
		in[2].setDipSetting(    0x08, "1000" );
		in[2].setDipSetting(    0x00, "1500" );
		in[2].setBit( 0x10, IP_ACTIVE_HIGH, IPT_BUTTON1 | IPF_PLAYER2 );
		in[2].setBit( 0x20, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT  | IPF_2WAY | IPF_PLAYER2 );
		in[2].setBit( 0x40, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_2WAY | IPF_PLAYER2 );
		in[2].setDipName( 0x80, 0x00, "Coin Info" );
		in[2].setDipSetting(    0x80, DEF_STR[ Off ] );
		in[2].setDipSetting(    0x00, DEF_STR[ On ] );

		/* Dummy port for cocktail mode */
		in[3].setDipName( 0x01, 0x00, DEF_STR[ Cabinet ] );
		in[3].setDipSetting(    0x00, DEF_STR[ Upright ] );
		in[3].setDipSetting(    0x01, DEF_STR[ Cocktail ] );
		return in;
	}

/*******************************************************/
/*                                                     */
/* Taito "Galaxy Wars"                                 */
/*                                                     */
/*******************************************************/

	private InputPort[] ipt_galxwars() {
			  /* IN0 */
		in[0].setBit( 0x01, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x02, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x04, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x08, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x10, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x20, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x40, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x80, IP_ACTIVE_HIGH, IPT_UNKNOWN );

			  /* IN1 */
		in[1].setBit( 0x01, IP_ACTIVE_LOW,  IPT_COIN1 );
		in[1].setBit( 0x02, IP_ACTIVE_HIGH, IPT_START2 );
		in[1].setBit( 0x04, IP_ACTIVE_HIGH, IPT_START1 );
		in[1].setBit( 0x08, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[1].setBit( 0x10, IP_ACTIVE_HIGH, IPT_BUTTON1 );
		in[1].setBit( 0x20, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT  | IPF_2WAY );
		in[1].setBit( 0x40, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_2WAY );
		in[1].setBit( 0x80, IP_ACTIVE_LOW,  IPT_UNKNOWN );

			  /* DSW0 */
		in[2].setDipName( 0x03, 0x01, DEF_STR[ Lives ] );
		in[2].setDipSetting(    0x00, "2" );
		in[2].setDipSetting(    0x01, "3" );
		in[2].setDipSetting(    0x02, "4" );
		in[2].setDipSetting(    0x03, "5" );
		in[2].setDipName( 0x04, 0x00, DEF_STR[ Unknown ] );
		in[2].setDipSetting(    0x04, DEF_STR[ Off ] );
		in[2].setDipSetting(    0x00, DEF_STR[ On ] );
		in[2].setDipName( 0x08, 0x00, DEF_STR[ Bonus_Life ] );
		in[2].setDipSetting(    0x00, "3000" );
		in[2].setDipSetting(    0x08, "5000" );
		in[2].setBit( 0x10, IP_ACTIVE_HIGH, IPT_BUTTON1 | IPF_PLAYER2 );
		in[2].setBit( 0x20, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT | IPF_PLAYER2 );
		in[2].setBit( 0x40, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_PLAYER2 );
		in[2].setDipName( 0x80, 0x00, DEF_STR[ Coinage ] );
		in[2].setDipSetting(    0x80, DEF_STR[ _2C_1C ] );
		in[2].setDipSetting(    0x00, DEF_STR[ _1C_1C ] );

				/* Dummy port for cocktail mode */
		in[3].setDipName( 0x01, 0x00, DEF_STR[ Cabinet ] );
		in[3].setDipSetting(    0x00, DEF_STR[ Upright ] );
		in[3].setDipSetting(    0x01, DEF_STR[ Cocktail ] );
		return in;
	}

/*******************************************************/
/*                                                     */
/* Taito "Lunar Rescue"                                */
/*                                                     */
/*******************************************************/

	private InputPort[] ipt_lrescue() {
			  /* IN0 */
		in[0].setBit( 0x01, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x02, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x04, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x08, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x10, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x20, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x40, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x80, IP_ACTIVE_HIGH, IPT_UNKNOWN );

			  /* IN1 */
		in[1].setBit( 0x01, IP_ACTIVE_LOW,  IPT_COIN1 );
		in[1].setBit( 0x02, IP_ACTIVE_HIGH, IPT_START2 );
		in[1].setBit( 0x04, IP_ACTIVE_HIGH, IPT_START1 );
		in[1].setBit( 0x08, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[1].setBit( 0x10, IP_ACTIVE_HIGH, IPT_BUTTON1 );
		in[1].setBit( 0x20, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT  | IPF_2WAY );
		in[1].setBit( 0x40, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_2WAY );
		in[1].setBit( 0x80, IP_ACTIVE_LOW,  IPT_UNKNOWN );

			  /* DSW0 */
		in[2].setDipName( 0x03, 0x00, DEF_STR[ Lives ] );
		in[2].setDipSetting(    0x00, "3" );
		in[2].setDipSetting(    0x01, "4" );
		in[2].setDipSetting(    0x02, "5" );
		in[2].setDipSetting(    0x03, "6" );
		in[2].setBit( 0x04, IP_ACTIVE_HIGH, IPT_TILT );
		in[2].setDipName( 0x08, 0x00, DEF_STR[ Unknown ] );
		in[2].setDipSetting(    0x08, DEF_STR[ Off ] );
		in[2].setDipSetting(    0x00, DEF_STR[ On ] );
		in[2].setBit( 0x10, IP_ACTIVE_HIGH, IPT_BUTTON1 | IPF_PLAYER2 );
		in[2].setBit( 0x20, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT  | IPF_2WAY | IPF_PLAYER2 );
		in[2].setBit( 0x40, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_2WAY | IPF_PLAYER2 );
		in[2].setDipName( 0x80, 0x00, DEF_STR[ Unknown ] );
		in[2].setDipSetting(    0x80, DEF_STR[ Off ] );
		in[2].setDipSetting(    0x00, DEF_STR[ On ] );

				/* Dummy port for cocktail mode */
		in[3].setDipName( 0x01, 0x00, DEF_STR[ Cabinet ] );
		in[3].setDipSetting(    0x00, DEF_STR[ Upright ] );
		in[3].setDipSetting(    0x01, DEF_STR[ Cocktail ] );
		return in;
	}

/*******************************************************/
/*                                                     */
/* Universal "Cosmic Monsters"                         */
/*                                                     */
/*******************************************************/

	private InputPort[] ipt_cosmicmo() {
		/* IN0 */
		in[0].setBit( 0x01, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x02, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x04, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x08, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x10, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x20, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x40, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x80, IP_ACTIVE_HIGH, IPT_UNKNOWN );

		/* IN1 */
		in[1].setBit( 0x01, IP_ACTIVE_LOW,  IPT_COIN1 );
		in[1].setBit( 0x02, IP_ACTIVE_HIGH, IPT_START2 );
		in[1].setBit( 0x04, IP_ACTIVE_HIGH, IPT_START1 );
		in[1].setBit( 0x08, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[1].setBit( 0x10, IP_ACTIVE_HIGH, IPT_BUTTON1 );
		in[1].setBit( 0x20, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT  | IPF_2WAY );
		in[1].setBit( 0x40, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_2WAY );
		in[1].setBit( 0x80, IP_ACTIVE_LOW,  IPT_UNKNOWN );

		/* DSW0 */
		in[2].setDipName( 0x03, 0x01, DEF_STR[ Lives ] );
		in[2].setDipSetting(    0x00, "2" );
		in[2].setDipSetting(    0x01, "3" );
		in[2].setDipSetting(    0x02, "4" );
		in[2].setDipSetting(    0x03, "5" );
		in[2].setDipName( 0x04, 0x00, DEF_STR[ Unknown ] );
		in[2].setDipSetting(    0x04, DEF_STR[ Off ] );
		in[2].setDipSetting(    0x00, DEF_STR[ On ] );
		in[2].setDipName( 0x08, 0x00, DEF_STR[ Unknown ] );
		in[2].setDipSetting(    0x08, DEF_STR[ Off ] );
		in[2].setDipSetting(    0x00, DEF_STR[ On ] );
		in[2].setBit( 0x10, IP_ACTIVE_HIGH, IPT_BUTTON1 | IPF_PLAYER2 );
		in[2].setBit( 0x20, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT  | IPF_2WAY | IPF_PLAYER2 );
		in[2].setBit( 0x40, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_2WAY | IPF_PLAYER2 );
		in[2].setDipName( 0x80, 0x00, DEF_STR[ Coinage ] );
		in[2].setDipSetting(    0x00, DEF_STR[ _1C_1C ] );
		in[2].setDipSetting(    0x80, DEF_STR[ _1C_2C ] );

		/* Dummy port for cocktail mode */
		in[3].setDipName( 0x01, 0x00, DEF_STR[ Cabinet ] );
		in[3].setDipSetting(    0x00, DEF_STR[ Upright ] );
		in[3].setDipSetting(    0x01, DEF_STR[ Cocktail ] );
		return in;
	}


/*******************************************************/
/*                                                     */
/* Nichibutsu "Rolling Crash"                          */
/*                                                     */
/*******************************************************/

	private MemoryReadAddress rollingc_readmem() {
		MemoryReadAddress mra = new MemoryReadAddress(memCpu1);
		mra.setMR( 0x0000, 0x1fff, MRA_ROM );
		mra.setMR( 0x2000, 0x3fff, MRA_RAM );
	//  mra.set( 0x2000, 0x2002, MRA_RAM );
	//  mra.set( 0x2003, 0x2003, hack );
		mra.setMR( 0x4000, 0x5fff, MRA_ROM );
		mra.setMR( 0xa400, 0xbfff, MRA_RAM );
		mra.setMR( 0xe400, 0xffff, MRA_RAM );
		return mra;
	}

	private MemoryWriteAddress rollingc_writemem() {
		MemoryWriteAddress mwa = new MemoryWriteAddress(memCpu1);
		mwa.setMW( 0x0000, 0x1fff, MWA_ROM );
		mwa.setMW( 0x2000, 0x23ff, MWA_RAM );
		mwa.set( 0x2400, 0x3fff, c8080bw_videoram_w );
		mwa.setMW( 0x4000, 0x5fff, MWA_ROM );
		mwa.set( 0xa400, 0xbfff, schaser_colorram_w );
		mwa.setMW( 0xe400, 0xffff, MWA_RAM );
		return mwa;
	}

	private InputPort[] ipt_rollingc() {
		      /* IN0 */
		in[0].setBit( 0x01, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT ); /* Game Select */
		in[0].setBit( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT ); /* Game Select */
		in[0].setBit( 0x08, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x10, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x20, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x40, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x80, IP_ACTIVE_HIGH, IPT_UNKNOWN );

		      /* IN1 */
		in[1].setBit( 0x01, IP_ACTIVE_HIGH, IPT_COIN1 );
		in[1].setBit( 0x02, IP_ACTIVE_HIGH, IPT_START2 );
		in[1].setBit( 0x04, IP_ACTIVE_HIGH, IPT_START1 );
		in[1].setBit( 0x08, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[1].setBit( 0x10, IP_ACTIVE_HIGH, IPT_BUTTON1 );
		in[1].setBit( 0x20, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT  | IPF_2WAY );
		in[1].setBit( 0x40, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_2WAY );
		in[1].setBit( 0x80, IP_ACTIVE_HIGH, IPT_UNKNOWN );

		      /* DSW1 */
		in[2].setDipName( 0x03, 0x00, DEF_STR[ Lives ] );
		in[2].setDipSetting(    0x00, "3" );
		in[2].setDipSetting(    0x01, "4" );
		in[2].setDipSetting(    0x02, "5" );
		in[2].setDipSetting(    0x03, "6" );
		in[2].setBit( 0x04, IP_ACTIVE_HIGH, IPT_TILT );
		in[2].setDipName( 0x08, 0x00, DEF_STR[ Unknown ] );
		in[2].setDipSetting(    0x08, DEF_STR[ Off ] );
		in[2].setDipSetting(    0x00, DEF_STR[ On ] );
		in[2].setBit( 0x10, IP_ACTIVE_HIGH, IPT_BUTTON1 | IPF_PLAYER2 );
		in[2].setBit( 0x20, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT  | IPF_2WAY | IPF_PLAYER2 );
		in[2].setBit( 0x40, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_2WAY | IPF_PLAYER2 );
		in[2].setDipName( 0x80, 0x00, "Coin Info" );
		in[2].setDipSetting(    0x80, DEF_STR[ Off ] );
		in[2].setDipSetting(    0x00, DEF_STR[ On ] );

				/* Dummy port for cocktail mode */
		in[3].setDipName( 0x01, 0x00, DEF_STR[ Cabinet ] );
		in[3].setDipSetting(    0x00, DEF_STR[ Upright ] );
		in[3].setDipSetting(    0x01, DEF_STR[ Cocktail ] );

		return in;
	}

	public MachineDriver machine_driver_rollingc() {
		CpuDriver[] cpuDriver = new CpuDriver[1];

		cpuDriver[0] = new CpuDriver( (Cpu) new I8080(),
										1996800,        /* 2 Mhz? */
										rollingc_readmem(), rollingc_writemem(), c8080bw_readport(), invad2ct_writeport(),
										invaders_interrupt, 2 );

		int[] visibleArea = { 0*8, 32*8-1, 0*8, 28*8-1 };

		return new MachineDriver
		(
			cpuDriver,

			60, DEFAULT_60HZ_VBLANK_DURATION,
			1,
			NOP,

			32*8, 32*8, visibleArea,
			NO_GFX_DECODE_INFO,
			256, 0,
			invadpt2_vh_convert_color_prom,

			VIDEO_TYPE_RASTER | GfxManager.VIDEO_SUPPORTS_DIRTY,

			noCallback,
			invaders_vh_start,
			invaders_vh_stop,
			invaders_vh_screenrefresh,
			noSound

		);
	}


/*******************************************************/
/*                                                     */
/* Midway "280 ZZZAP"                                  */
/*                                                     */
/*******************************************************/

	private InputPort[] ipt_280zzzap() {
		/* IN0 */
		in[0].setBit( 0x01, IP_ACTIVE_HIGH, IPT_UNKNOWN ); /* 4 bit accel */
		in[0].setBit( 0x02, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x04, IP_ACTIVE_HIGH, IPT_BUTTON1 ); /* Crude approximation using 2 buttons */
		in[0].setBit( 0x08, IP_ACTIVE_HIGH, IPT_BUTTON2 );
		in[0].setBit( 0x10, IP_ACTIVE_HIGH, IPT_BUTTON3 | IPF_TOGGLE ); /* shift */
		in[0].setBit( 0x20, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x40, IP_ACTIVE_LOW,  IPT_COIN1 );
		in[0].setBit( 0x80, IP_ACTIVE_LOW,  IPT_START1 );

		/* IN1 - Steering Wheel */
		in[1].setAnalog( 0xff, 0x7f, IPT_PADDLE | IPF_REVERSE, 100, 10, 0x01, 0xfe);

		/* IN2 Dips & Coins */
		in[2].setDipName( 0x03, 0x00, DEF_STR[ Coinage ] );
		in[2].setDipSetting(    0x01, DEF_STR[ _2C_1C ] );
		in[2].setDipSetting(    0x00, DEF_STR[ _1C_1C ] );
		in[2].setDipSetting(    0x03, DEF_STR[ _2C_3C ] );
		in[2].setDipSetting(    0x02, DEF_STR[ _1C_3C ] );
		in[2].setDipName( 0x0c, 0x00, "Time" );
		in[2].setDipSetting(    0x0c, "60" );
		in[2].setDipSetting(    0x00, "80" );
		in[2].setDipSetting(    0x08, "99" );
		in[2].setDipSetting(    0x04, "Test Mode" );
		in[2].setDipName( 0x30, 0x00, "Extended Time" );
		in[2].setDipSetting(    0x00, "Score >= 2.5" );
		in[2].setDipSetting(    0x10, "Score >= 2" );
		in[2].setDipSetting(    0x20, "None" );
		/* 0x30 same as 0x20 */
		in[2].setDipName( 0xc0, 0x00, "Language");
		in[2].setDipSetting(    0x00, "English" );
		in[2].setDipSetting(    0x40, "German" );
		in[2].setDipSetting(    0x80, "French" );
		in[2].setDipSetting(    0xc0, "Spanish" );
		return in;
	}

	public MachineDriver machine_driver_280zzzap() {
		CpuDriver[] cpuDriver = new CpuDriver[1];

		cpuDriver[0] = new CpuDriver( (Cpu) new I8080(),
										2000000,        /* 2 Mhz? */
										c8080bw_readmem(), c8080bw_writemem(), c8080bw_readport(), writeport_4_3(),
										invaders_interrupt, 2 );

		int[] visibleArea = { 0*8, 32*8-1, 0*8, 28*8-1 };

		return new MachineDriver
		(
			cpuDriver,

			60, DEFAULT_60HZ_VBLANK_DURATION,
			1,
			NOP,

			//video;	// not in MAME
			32*8, 32*8, visibleArea,
			NO_GFX_DECODE_INFO,
			256, 0,

			noConvertProms,

			VIDEO_TYPE_RASTER | GfxManager.VIDEO_SUPPORTS_DIRTY,

			noCallback,
			invaders_vh_start,
			invaders_vh_stop,
			invaders_vh_screenrefresh,
			noSound

		);
	}


/*******************************************************/
/*                                                     */
/* Midway "Laguna Racer"                               */
/*                                                     */
/*******************************************************/

	private InputPort[] ipt_lagunar() {
		      /* IN0 */
		in[0].setBit( 0x01, IP_ACTIVE_HIGH, IPT_UNKNOWN ); /* 4 bit accel */
		in[0].setBit( 0x02, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x04, IP_ACTIVE_HIGH, IPT_BUTTON1 ); /* Crude approximation using 2 buttons */
		in[0].setBit( 0x08, IP_ACTIVE_HIGH, IPT_BUTTON2 );
		in[0].setBit( 0x10, IP_ACTIVE_HIGH, IPT_BUTTON4 | IPF_TOGGLE ); /* shift */
		in[0].setBit( 0x20, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[0].setBit( 0x40, IP_ACTIVE_LOW,  IPT_COIN1 );
		in[0].setBit( 0x80, IP_ACTIVE_LOW,  IPT_START1 );

		      /* IN1 - Steering Wheel */
		//PORT_ANALOG( 0xff, 0x7f, IPT_PADDLE | IPF_REVERSE, 100, 10, 0x01, 0xfe);

		      /* IN2 Dips & Coins */
		in[2].setDipName( 0x03, 0x00, DEF_STR[ Coinage ] );
		in[2].setDipSetting(    0x02, DEF_STR[ _2C_1C ] );
		in[2].setDipSetting(    0x00, DEF_STR[ _1C_1C ] );
		in[2].setDipSetting(    0x03, DEF_STR[ _2C_3C ] );
		in[2].setDipSetting(    0x01, DEF_STR[ _1C_2C ] );
		in[2].setDipName( 0x0c, 0x0c, "Time" );
		in[2].setDipSetting(    0x00, "45" );
		in[2].setDipSetting(    0x04, "60" );
		in[2].setDipSetting(    0x08, "75" );
		in[2].setDipSetting(    0x0c, "90" );
		in[2].setDipName( 0x30, 0x00, "Extended Time" );
		in[2].setDipSetting(    0x00, "350" );
		in[2].setDipSetting(    0x10, "400" );
		in[2].setDipSetting(    0x20, "450" );
		in[2].setDipSetting(    0x30, "500" );
		in[2].setDipName( 0xc0, 0x00, "Test Modes");
		in[2].setDipSetting(    0x00, "Play Mode" );
		in[2].setDipSetting(    0x40, "RAM/ROM" );
		in[2].setDipSetting(    0x80, "Steering" );
		in[2].setDipSetting(    0xc0, "No Extended Play" );
		return in;
	}


/*******************************************************/
/*                                                     */
/* Midway "Amazing Maze"                               */
/*                                                     */
/*******************************************************/

	private InputPort[] ipt_maze() {
		      /* IN0 */
		in[0].setBit( 0x01, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT  | IPF_PLAYER1 );
		in[0].setBit( 0x02, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_PLAYER1 );
		in[0].setBit( 0x04, IP_ACTIVE_HIGH, IPT_JOYSTICK_DOWN  | IPF_PLAYER1 );
		in[0].setBit( 0x08, IP_ACTIVE_HIGH, IPT_JOYSTICK_UP    | IPF_PLAYER1 );
		in[0].setBit( 0x10, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT  | IPF_PLAYER2 );
		in[0].setBit( 0x20, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_PLAYER2 );
		in[0].setBit( 0x40, IP_ACTIVE_HIGH, IPT_JOYSTICK_DOWN  | IPF_PLAYER2 );
		in[0].setBit( 0x80, IP_ACTIVE_HIGH, IPT_JOYSTICK_UP    | IPF_PLAYER2 );

		      /* IN1 */
		in[1].setBit( 0x01, IP_ACTIVE_HIGH, IPT_START1 );
		in[1].setBit( 0x02, IP_ACTIVE_HIGH, IPT_START2 );
		in[1].setBit( 0x04, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[1].setBit( 0x08, IP_ACTIVE_HIGH, IPT_COIN1  );
		in[1].setBit( 0x10, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[1].setBit( 0x20, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		in[1].setBit( 0x40, IP_ACTIVE_HIGH, IPT_UNKNOWN);
		in[1].setService( 0x80, IP_ACTIVE_HIGH );

		      /* DSW0 - Never read (?); */
		in[2].setDipName( 0x01, 0x00, DEF_STR[ Unknown ] );
		in[2].setDipSetting(    0x00, DEF_STR[ Off ] );
		in[2].setDipSetting(    0x01, DEF_STR[ On ] );
		in[2].setDipName( 0x02, 0x00, DEF_STR[ Unknown ] );
		in[2].setDipSetting(    0x00, DEF_STR[ Off ] );
		in[2].setDipSetting(    0x02, DEF_STR[ On ] );
		in[2].setDipName( 0x04, 0x00, DEF_STR[ Unknown ] );
		in[2].setDipSetting(    0x00, DEF_STR[ Off ] );
		in[2].setDipSetting(    0x04, DEF_STR[ On ] );
		in[2].setDipName( 0x08, 0x00, DEF_STR[ Unknown ] );
		in[2].setDipSetting(    0x00, DEF_STR[ Off ] );
		in[2].setDipSetting(    0x08, DEF_STR[ On ] );
		in[2].setDipName( 0x10, 0x00, DEF_STR[ Unknown ] );
		in[2].setDipSetting(    0x00, DEF_STR[ Off ] );
		in[2].setDipSetting(    0x10, DEF_STR[ On ] );
		in[2].setDipName( 0x20, 0x00, DEF_STR[ Unknown ] );
		in[2].setDipSetting(    0x00, DEF_STR[ Off ] );
		in[2].setDipSetting(    0x20, DEF_STR[ On ] );
		in[2].setDipName( 0x40, 0x00, DEF_STR[ Unknown ] );
		in[2].setDipSetting(    0x00, DEF_STR[ Off ] );
		in[2].setDipSetting(    0x40, DEF_STR[ On ] );
		in[2].setDipName( 0x80, 0x00, DEF_STR[ Unknown ] );
		in[2].setDipSetting(    0x00, DEF_STR[ Off ] );
		in[2].setDipSetting(    0x80, DEF_STR[ On ] );
		return in;
	}

/*******************************************************/
/*                                                     */
/* Midway "Checkmate"                                  */
/*                                                     */
/*******************************************************/

	private IOReadPort checkmat_readport() {
		IOReadPort ior = new IOReadPort();
		ior.set( 0x00, 0x00, input_port_0_r );
		ior.set( 0x01, 0x01, input_port_1_r );
		ior.set( 0x02, 0x02, input_port_2_r );
		ior.set( 0x03, 0x03, input_port_3_r );
		return ior;
	}

	private IOWritePort checkmat_writeport() {
		return new IOWritePort();
	}


	private InputPort[] ipt_checkmat() {
		      /* IN0  */
		in[0].setBit( 0x01, IP_ACTIVE_HIGH, IPT_JOYSTICK_UP    | IPF_PLAYER1 );
		in[0].setBit( 0x02, IP_ACTIVE_HIGH, IPT_JOYSTICK_DOWN  | IPF_PLAYER1 );
		in[0].setBit( 0x04, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT  | IPF_PLAYER1 );
		in[0].setBit( 0x08, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_PLAYER1 );
		in[0].setBit( 0x10, IP_ACTIVE_HIGH, IPT_JOYSTICK_UP    | IPF_PLAYER2 );
		in[0].setBit( 0x20, IP_ACTIVE_HIGH, IPT_JOYSTICK_DOWN  | IPF_PLAYER2 );
		in[0].setBit( 0x40, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT  | IPF_PLAYER2 );
		in[0].setBit( 0x80, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_PLAYER2 );

		      /* IN1  */
		//in[1].setBit( 0x01, IP_ACTIVE_HIGH, IPT_JOYSTICK_UP    | IPF_PLAYER3 );
		//in[1].setBit( 0x02, IP_ACTIVE_HIGH, IPT_JOYSTICK_DOWN  | IPF_PLAYER3 );
		//in[1].setBit( 0x04, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT  | IPF_PLAYER3 );
		//in[1].setBit( 0x08, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_PLAYER3 );
		//in[1].setBit( 0x10, IP_ACTIVE_HIGH, IPT_JOYSTICK_UP    | IPF_PLAYER4 );
		//in[1].setBit( 0x20, IP_ACTIVE_HIGH, IPT_JOYSTICK_DOWN  | IPF_PLAYER4 );
		//in[1].setBit( 0x40, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT  | IPF_PLAYER4 );
		//in[1].setBit( 0x80, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_PLAYER4 );

		      /* IN2 Dips & Coins */
		in[2].setDipName( 0x01, 0x00, DEF_STR[ Coinage ] );
		in[2].setDipSetting(    0x00, "1 Coin/1 or 2 Playera" );
		in[2].setDipSetting(    0x01, "1 Coin/1 to 4 Players" );
		in[2].setDipName( 0x02, 0x00, DEF_STR[ Unknown ] );
		in[2].setDipSetting(    0x02, DEF_STR[ Off ] );
		in[2].setDipSetting(    0x00, DEF_STR[ On ] );
		in[2].setDipName( 0x0c, 0x00, "Rounds" );
		in[2].setDipSetting(    0x00, "2" );
		in[2].setDipSetting(    0x04, "3" );
		in[2].setDipSetting(    0x08, "4" );
		in[2].setDipSetting(    0x0c, "5" );
		in[2].setDipName( 0x10, 0x00, DEF_STR[ Unknown ] );
		in[2].setDipSetting(    0x10, DEF_STR[ Off ] );
		in[2].setDipSetting(    0x00, DEF_STR[ On ] );
		in[2].setDipName( 0x60, 0x00, "Language?" );
		in[2].setDipSetting(    0x00, "English?" );
		in[2].setDipSetting(    0x20, "German?" );
		in[2].setDipSetting(    0x40, "French?" );
		in[2].setDipSetting(    0x60, "Spanish?" );
		in[2].setService( 0x80, IP_ACTIVE_HIGH );

		       /* IN3  */
		in[3].setBit( 0x01, IP_ACTIVE_HIGH, IPT_START1 );
		in[3].setBit( 0x02, IP_ACTIVE_HIGH, IPT_START2 );
		in[3].setBit( 0x04, IP_ACTIVE_HIGH, IPT_START3 );
		//in[3].setBit( 0x08, IP_ACTIVE_HIGH, IPT_START4 );
		in[3].setBit( 0x10, IP_ACTIVE_HIGH, IPT_UNUSED );
		in[3].setBit( 0x20, IP_ACTIVE_HIGH, IPT_UNUSED );
		in[3].setBit( 0x40, IP_ACTIVE_HIGH, IPT_UNUSED );
		in[3].setBit( 0x80, IP_ACTIVE_HIGH, IPT_COIN1 );
		return in;
	}

	public MachineDriver machine_driver_checkmat() {
		CpuDriver[] cpuDriver = new CpuDriver[1];

		cpuDriver[0] = new CpuDriver( (Cpu) new I8080(),
										2000000,        /* 2 Mhz? */
										c8080bw_readmem(), c8080bw_writemem(), checkmat_readport(), checkmat_writeport(),
										invaders_interrupt, 2 );

		int[] visibleArea = { 0*8, 32*8-1, 0*8, 28*8-1 };

		return new MachineDriver
		(
			cpuDriver,

			60, DEFAULT_60HZ_VBLANK_DURATION,
			1,
			NOP,

			32*8, 32*8, visibleArea,
			NO_GFX_DECODE_INFO,
			256, 0,

			noConvertProms,

			VIDEO_TYPE_RASTER | GfxManager.VIDEO_SUPPORTS_DIRTY,

			noCallback,
			invaders_vh_start,
			invaders_vh_stop,
			invaders_vh_screenrefresh,
			noSound

		);
	}


/*******************************************************/
/*                                                     */
/* Midway "Boot Hill"                                  */
/*                                                     */
/*******************************************************/

	private IOReadPort boothill_readport() {
		IOReadPort ior = new IOReadPort();
		ior.set( 0x00, 0x00, input_port_0_r );
		ior.set( 0x01, 0x01, input_port_1_r );
		ior.set( 0x02, 0x02, input_port_2_r );
		ior.set( 0x03, 0x03, boothill_shift_data_r );
		return ior;
	}

	private IOWritePort boothill_writeport() {
		IOWritePort	iow = new IOWritePort();
		iow.set( 0x01,	0x01, c8080bw_shift_amount_w );
		iow.set( 0x02,	0x02, c8080bw_shift_data_w );
		//{ 0x03, 0x03, boothill_sh_port3_w },
		//{ 0x05, 0x05, boothill_sh_port5_w },
		return iow;
	}

	private InputPort[] ipt_boothill() {
		/* Gun position uses bits 4-6, handled using fake paddles */
		      /* IN0 - Player 2 */
		in[0].setBit( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_PLAYER2 );
		in[0].setBit( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_PLAYER2 );        /* Move Man */
		in[0].setBit( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_PLAYER2 );
		in[0].setBit( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_PLAYER2 );
		in[0].setBit( 0x80, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER2 ); /* Fire */

		      /* IN1 - Player 1 */
		in[1].setBit( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY );
		in[1].setBit( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY ); /* Move Man */
		in[1].setBit( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY );
		in[1].setBit( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY );
		in[1].setBit( 0x80, IP_ACTIVE_LOW, IPT_BUTTON1 ); /* Fire */

		      /* IN2 Dips & Coins */
		in[2].setDipName( 0x03, 0x00, DEF_STR[ Coinage ] );
		in[2].setDipSetting(    0x02, DEF_STR[ _2C_1C ] );
		in[2].setDipSetting(    0x00, DEF_STR[ _1C_1C ] );
		in[2].setDipSetting(    0x01, DEF_STR[ _1C_2C ] );
	//	in[2].setDipSetting(    0x03, DEF_STR[ 1C_2C ] );
		in[2].setDipName( 0x0c, 0x00, "Time" );
		in[2].setDipSetting(    0x00, "64" );
		in[2].setDipSetting(    0x04, "74" );
		in[2].setDipSetting(    0x08, "84" );
		in[2].setDipSetting(    0x0C, "94" );
		in[2].setService( 0x10, IP_ACTIVE_HIGH );
		in[2].setBit( 0x20, IP_ACTIVE_LOW, IPT_START1 );
		in[2].setBit( 0x40, IP_ACTIVE_LOW, IPT_COIN1 );
		in[2].setBit( 0x80, IP_ACTIVE_LOW, IPT_START2 );

		/* Player 2 Gun */
		//PORT_ANALOGX( 0xff, 0x00, IPT_PADDLE | IPF_PLAYER2, 50, 10, 1, 255, IP_KEY_NONE, IP_KEY_NONE, IP_JOY_NONE, IP_JOY_NONE );

		/* Player 1 Gun */
		//PORT_ANALOGX( 0xff, 0x00, IPT_PADDLE, 50, 10, 1, 255, KEYCODE_Z, KEYCODE_A, IP_JOY_NONE, IP_JOY_NONE );
		return in;
	}

	public MachineDriver machine_driver_boothill() {
		CpuDriver[] cpuDriver = new CpuDriver[1];

		cpuDriver[0] = new CpuDriver( (Cpu) new I8080(), 2000000,        /* 2 Mhz? */
										c8080bw_readmem(), c8080bw_writemem(),
										boothill_readport(), writeport_1_2(),
										invaders_interrupt, 2 );

		int[] visibleArea = { 0*8, 32*8-1, 0*8, 28*8-1 };

		return new MachineDriver
		(
			cpuDriver,

			60, DEFAULT_60HZ_VBLANK_DURATION,
			1,
			NOP,

			32*8, 32*8, visibleArea,
			NO_GFX_DECODE_INFO,
			256, 0,

			noConvertProms,

			VIDEO_TYPE_RASTER | GfxManager.VIDEO_SUPPORTS_DIRTY,

			noCallback,
			invaders_vh_start,
			invaders_vh_stop,
			invaders_vh_screenrefresh,
			noSound

		);
	}


	private boolean rom_invaders() {
		romLoader.setZip( "invaders" );

		romLoader.setMemory(memCpu1);
		romLoader.loadROM( "invaders.h", 0x0000, 0x0800, 0x734f5ad8 );
		romLoader.loadROM( "invaders.g", 0x0800, 0x0800, 0x6bfaca4a );
		romLoader.loadROM( "invaders.f", 0x1000, 0x0800, 0x0ccead96 );
		romLoader.loadROM( "invaders.e", 0x1800, 0x0800, 0x14e538b0 );

		romLoader.loadZip(base_URL);

		return true;
	}

	private boolean rom_earthinv() {
		romLoader.setZip( "earthinv" );

		romLoader.setMemory(memCpu1);
		romLoader.loadROM( "earthinv.h",   0x0000, 0x0800, 0x58a750c8 );
		romLoader.loadROM( "earthinv.g",   0x0800, 0x0800, 0xb91742f1 );
		romLoader.loadROM( "earthinv.f",   0x1000, 0x0800, 0x4acbbc60 );
		romLoader.loadROM( "earthinv.e",   0x1800, 0x0800, 0xdf397b12 );

		romLoader.loadZip(base_URL);

		return true;
	}

	private boolean rom_spaceatt() {
		romLoader.setZip( "spaceatt" );

		romLoader.setMemory(memCpu1);
		romLoader.loadROM( "h",            0x0000, 0x0400, 0xd0c32d72 );
		romLoader.loadROM( "sv02.bin",     0x0400, 0x0400, 0x0e159534 );
		romLoader.loadROM( "f",            0x0800, 0x0400, 0x483e651e );
		romLoader.loadROM( "c",            0x1400, 0x0400, 0x1293b826 );
		romLoader.loadROM( "b",            0x1800, 0x0400, 0x6fc782aa );
		romLoader.loadROM( "a",            0x1c00, 0x0400, 0x211ac4a3 );

		romLoader.loadZip(base_URL);

		return true;
	}

	private boolean rom_spaceat2() {
		romLoader.setZip( "spaceat2" );

		romLoader.setMemory(memCpu1);
		romLoader.loadROM( "spaceatt.h",   0x0000, 0x0800, 0xa31d0756 );
		romLoader.loadROM( "spaceatt.g",   0x0800, 0x0800, 0xf41241f7 );
		romLoader.loadROM( "spaceatt.f",   0x1000, 0x0800, 0x4c060223 );
		romLoader.loadROM( "spaceatt.e",   0x1800, 0x0800, 0x7cf6f604 );

		romLoader.loadZip(base_URL);

		return true;
	}

	private boolean rom_sinvzen() {
		romLoader.setZip( "sinvzen" );

		romLoader.setMemory(memCpu1);
		romLoader.loadROM( "1.bin",        0x0000, 0x0400, 0x9b0da779 );
		romLoader.loadROM( "2.bin",        0x0400, 0x0400, 0x9858ccab );
		romLoader.loadROM( "3.bin",        0x0800, 0x0400, 0xa1cc38b5 );
		romLoader.loadROM( "4.bin",        0x0c00, 0x0400, 0x1f2db7a8 );
		romLoader.loadROM( "5.bin",        0x1000, 0x0400, 0x9b505fcd );
		romLoader.loadROM( "6.bin",        0x1400, 0x0400, 0xde0ca0ae );
		romLoader.loadROM( "7.bin",        0x1800, 0x0400, 0x25a296f6 );
		romLoader.loadROM( "8.bin",        0x1c00, 0x0400, 0xf4bc4a98 );

		romLoader.loadZip(base_URL);

		return true;
	}

	private boolean rom_sinvemag() {
		romLoader.setZip( "sinvemag" );

		romLoader.setMemory(memCpu1);
		romLoader.loadROM( "sv0h.bin",     0x0000, 0x0400, 0x86bb8cb6 );
		romLoader.loadROM( "emag_si.b",    0x0400, 0x0400, 0xfebe6d1a );
		romLoader.loadROM( "emag_si.c",    0x0800, 0x0400, 0xaafb24f7 );
		romLoader.loadROM( "emag_si.d",    0x1400, 0x0400, 0x68c4b9da );
		romLoader.loadROM( "emag_si.e",    0x1800, 0x0400, 0xc4e80586 );
		romLoader.loadROM( "emag_si.f",    0x1c00, 0x0400, 0x077f5ef2 );

		romLoader.loadZip(base_URL);

		return true;
	}

	private boolean rom_alieninv() {
		romLoader.setZip( "alieninv" );

		romLoader.setMemory(memCpu1);
		romLoader.loadROM( "1h.bin",       0x0000, 0x0800, 0xc46df7f4 );
		romLoader.loadROM( "1g.bin",       0x0800, 0x0800, 0x4b1112d6 );
		romLoader.loadROM( "1f.bin",       0x1000, 0x0800, 0Xadca18a5 );
		romLoader.loadROM( "1e.bin",       0x1800, 0x0800, 0x0449CB52 );

		romLoader.loadZip(base_URL);

		return true;
	}

	private boolean rom_sitv() {
		romLoader.setZip( "sitv" );

		romLoader.setMemory(memCpu1);
		romLoader.loadROM( "tv0h.s1",      0x0000, 0x0800, 0xfef18aad );
		romLoader.loadROM( "tv02.rp1",     0x0800, 0x0800, 0x3c759a90 );
		romLoader.loadROM( "tv03.n1",      0x1000, 0x0800, 0x0ad3657f );
		romLoader.loadROM( "tv04.m1",      0x1800, 0x0800, 0xcd2c67f6 );

		romLoader.loadZip(base_URL);

		return true;
	}

	private boolean rom_sicv() {
		romLoader.setZip( "sicv" );

		romLoader.setMemory(memCpu1);
		romLoader.loadROM( "cv17.bin",     0x0000, 0x0800, 0x3dfbe9e6 );
		romLoader.loadROM( "cv18.bin",     0x0800, 0x0800, 0xbc3c82bf );
		romLoader.loadROM( "cv19.bin",     0x1000, 0x0800, 0xd202b41c );
		romLoader.loadROM( "cv20.bin",     0x1800, 0x0800, 0xc74ee7b6 );

		romLoader.setMemory(REGION_PROMS); /* color maps player 1/player 2 */
		romLoader.loadROM( "cv01_1.bin",   0x0000, 0x0400, 0xaac24f34 );
		romLoader.loadROM( "cv02_2.bin",   0x0400, 0x0400, 0x2bdf83a0 );

		romLoader.loadZip(base_URL);

		return true;
	}

	private boolean rom_sisv() {
		romLoader.setZip( "invaders" );

		romLoader.setMemory(memCpu1);
		romLoader.loadROM( "invaders.g",   0x0800, 0x0800, 0x6bfaca4a );
		romLoader.loadROM( "invaders.f",   0x1000, 0x0800, 0x0ccead96 );

		romLoader.loadZip(base_URL);

		romLoader.setZip( "sisv" );

		romLoader.setMemory(memCpu1);
		romLoader.loadROM( "sv0h.bin",     0x0000, 0x0400, 0x86bb8cb6 );
		romLoader.loadROM( "sv02.bin",     0x0400, 0x0400, 0x0e159534 );
		romLoader.loadROM( "tv04.m1",      0x1800, 0x0800, 0xcd2c67f6 );

		romLoader.setMemory(REGION_PROMS); /* color maps player 1/player 2 */
		romLoader.loadROM( "cv01_1.bin",   0x0000, 0x0400, 0xaac24f34 );
		romLoader.loadROM( "cv02_2.bin",   0x0400, 0x0400, 0x2bdf83a0 );

		romLoader.loadZip(base_URL);

		return true;
	}

	private boolean rom_sisv2() {
		romLoader.setZip( "invaders" );

		romLoader.setMemory(memCpu1);
		romLoader.loadROM( "invaders.f",   0x1000, 0x0800, 0x0ccead96 );

		romLoader.loadZip(base_URL);

		romLoader.setZip( "sisv2" );

		romLoader.setMemory(memCpu1);
		romLoader.loadROM( "sv0h.bin",     0x0000, 0x0400, 0x86bb8cb6 );
		romLoader.loadROM( "emag_si.b",    0x0400, 0x0400, 0xfebe6d1a );
		romLoader.loadROM( "sv12",         0x0800, 0x0400, 0xa08e7202 );
		romLoader.loadROM( "sv13",         0x1800, 0x0400, 0xa9011634 );
		romLoader.loadROM( "sv14",         0x1c00, 0x0400, 0x58730370 );

		romLoader.setMemory(REGION_PROMS); /* color maps player 1/player 2 */
		romLoader.loadROM( "cv01_1.bin",   0x0000, 0x0400, 0xaac24f34 );
		romLoader.loadROM( "cv02_2.bin",   0x0400, 0x0400, 0x2bdf83a0 );

		romLoader.loadZip(base_URL);

		return true;
	}

	private boolean rom_spceking() {
		romLoader.setZip( "invaders" );

		romLoader.setMemory(memCpu1);
		romLoader.loadROM( "invaders.h",   0x0000, 0x0800, 0x734f5ad8 );

		romLoader.loadZip(base_URL);

		romLoader.setZip( "spceking" );

		romLoader.setMemory(memCpu1);
		romLoader.loadROM( "spcekng2",     0x0800, 0x0800, 0x96dcdd42 );
		romLoader.loadROM( "spcekng3",     0x1000, 0x0800, 0x95fc96ad );
		romLoader.loadROM( "spcekng4",     0x1800, 0x0800, 0x54170ada );

		romLoader.loadZip(base_URL);

		return true;
	}

	private boolean rom_spcewars() {
		romLoader.setZip( "spcewars" );

		romLoader.setMemory(memCpu1);
		romLoader.loadROM( "sanritsu.1",   0x0000, 0x0400, 0xca331679 );
		romLoader.loadROM( "sanritsu.2",   0x0400, 0x0400, 0x48dc791c );
		romLoader.loadROM( "ic35.bin",     0x0800, 0x0800, 0x40c2d55b );
		romLoader.loadROM( "sanritsu.5",   0x1000, 0x0400, 0x77475431 );
		romLoader.loadROM( "sanritsu.6",   0x1400, 0x0400, 0x392ef82c );
		romLoader.loadROM( "sanritsu.7",   0x1800, 0x0400, 0xb3a93df8 );
		romLoader.loadROM( "sanritsu.8",   0x1c00, 0x0400, 0x64fdc3e1 );
		romLoader.loadROM( "sanritsu.9",   0x4000, 0x0400, 0xb2f29601 );

		romLoader.loadZip(base_URL);

		return true;
	}

	private boolean rom_spacewr3() {
		romLoader.setZip( "spacewr3" );

		romLoader.setMemory(memCpu1);
		romLoader.loadROM( "ic36.bin",     0x0000, 0x0800, 0x9e30f88a );
		romLoader.loadROM( "ic35.bin",     0x0800, 0x0800, 0x40c2d55b );
		romLoader.loadROM( "ic34.bin",     0x1000, 0x0800, 0xb435f021 );
		romLoader.loadROM( "ic33.bin",     0x1800, 0x0800, 0xcbdc6fe8 );
		romLoader.loadROM( "ic32.bin",     0x4000, 0x0800, 0x1e5a753c );

		romLoader.loadZip(base_URL);

		return true;
	}

	private boolean rom_invaderl() {
		romLoader.setZip( "invaderl" );

		romLoader.setMemory(memCpu1);
		romLoader.loadROM( "c01",          0x0000, 0x0400, 0x499f253a );
		romLoader.loadROM( "c02",          0x0400, 0x0400, 0x2d0b2e1f );
		romLoader.loadROM( "c03",          0x0800, 0x0400, 0x03033dc2 );
		romLoader.loadROM( "c07",          0x1000, 0x0400, 0x5a7bbf1f );
		romLoader.loadROM( "c04",          0x1400, 0x0400, 0x455b1fa7 );
		romLoader.loadROM( "c05",          0x1800, 0x0400, 0x40cbef75 );
		romLoader.loadROM( "sv06.bin",     0x1c00, 0x0400, 0x2c68e0b4 );

		romLoader.loadZip(base_URL);

		return true;
	}

	private boolean rom_jspecter() {
		romLoader.setZip( "jspecter" );

		romLoader.setMemory(memCpu1);
		romLoader.loadROM( "3305.u6",      0x0000, 0x1000, 0xab211a4f );
		romLoader.loadROM( "3306.u7",      0x1400, 0x1000, 0x0df142a7 );

		romLoader.loadZip(base_URL);

		return true;
	}

	private boolean rom_jspectr2() {
		romLoader.setZip( "jspectr2" );

		romLoader.setMemory(memCpu1);
		romLoader.loadROM( "unksi.b2",     0x0000, 0x1000, 0x0584b6c4 );
		romLoader.loadROM( "unksi.a2",     0x1400, 0x1000, 0x58095955 );

		romLoader.loadZip(base_URL);

		return true;
	}

	private boolean rom_280zzzap() {
		romLoader.setZip( "280zzzap" );

		romLoader.setMemory(memCpu1);
		romLoader.loadROM( "zzzaph", 0x0000, 0x0400, 0x1fa86e1c );
		romLoader.loadROM( "zzzapg", 0x0400, 0x0400, 0x9639bc6b );
		romLoader.loadROM( "zzzapf", 0x0800, 0x0400, 0xadc6ede1 );
		romLoader.loadROM( "zzzape", 0x0c00, 0x0400, 0x472493d6 );
		romLoader.loadROM( "zzzapd", 0x1000, 0x0400, 0x4c240ee1 );
		romLoader.loadROM( "zzzapc", 0x1400, 0x0400, 0x6e85aeaf );

		romLoader.loadZip(base_URL);

		return true;
	}

	private boolean rom_invadpt2() {
		romLoader.setZip( "invadpt2" );

		romLoader.setMemory( memCpu1 );
		romLoader.loadROM( "pv.01",        0x0000, 0x0800, 0x7288a511 );
		romLoader.loadROM( "pv.02",        0x0800, 0x0800, 0x097dd8d5 );
		romLoader.loadROM( "pv.03",        0x1000, 0x0800, 0x1766337e );
		romLoader.loadROM( "pv.04",        0x1800, 0x0800, 0x8f0e62e0 );
		romLoader.loadROM( "pv.05",        0x4000, 0x0800, 0x19b505e9 );

		romLoader.setMemory(REGION_PROMS); /* color maps player 1/player 2 */
		romLoader.loadROM( "pv06_1.bin",   0x0000, 0x0400, 0xa732810b );
		romLoader.loadROM( "pv07_2.bin",   0x0400, 0x0400, 0x2c5b91cb );

		romLoader.loadZip(base_URL);

		return true;
	}

	private boolean rom_invrvnge() {
		romLoader.setZip( "invrvnge" );

		romLoader.setMemory( memCpu1 );
		romLoader.loadROM( "invrvnge.h",   0x0000, 0x0800, 0xaca41bbb );
		romLoader.loadROM( "invrvnge.g",   0x0800, 0x0800, 0xcfe89dad );
		romLoader.loadROM( "invrvnge.f",   0x1000, 0x0800, 0xe350de2c );
		romLoader.loadROM( "invrvnge.e",   0x1800, 0x0800, 0x1ec8dfc8 );

		romLoader.loadZip(base_URL);

		return true;
	}

	private boolean rom_invrvnga() {
		romLoader.setZip( "invrvnga" );

		romLoader.setMemory( memCpu1 );
		romLoader.loadROM( "5m.bin",       0x0000, 0x0800, 0xb145cb71 );
		romLoader.loadROM( "5n.bin",       0x0800, 0x0800, 0x660e8af3 );
		romLoader.loadROM( "5p.bin",       0x1000, 0x0800, 0x6ec5a9ad );
		romLoader.loadROM( "5r.bin",       0x1800, 0x0800, 0x74516811 );

		romLoader.loadZip(base_URL);

		return true;
	}

	private boolean rom_cosmicmo() {
		romLoader.setZip( "cosmicmo" );

		romLoader.setMemory( memCpu1 );
		romLoader.loadROM( "cosmicmo.1",   0x0000, 0x0400, 0xd6e4e5da );
		romLoader.loadROM( "cosmicmo.2",   0x0400, 0x0400, 0x8f7988e6 );
		romLoader.loadROM( "cosmicmo.3",   0x0800, 0x0400, 0x2d2e9dc8 );
		romLoader.loadROM( "cosmicmo.4",   0x0c00, 0x0400, 0x26cae456 );
		romLoader.loadROM( "cosmicmo.5",   0x4000, 0x0400, 0xb13f228e );
		romLoader.loadROM( "cosmicmo.6",   0x4400, 0x0400, 0x4ae1b9c4 );
		romLoader.loadROM( "cosmicmo.7",   0x4800, 0x0400, 0x6a13b15b );

		romLoader.loadZip(base_URL);

		return true;
	}

	private boolean rom_superinv() {
		romLoader.setZip( "superinv" );

		romLoader.setMemory( memCpu1 );
		romLoader.loadROM( "00",           0x0000, 0x0400, 0x7a9b4485 );
		romLoader.loadROM( "01",           0x0400, 0x0400, 0x7c86620d );
		romLoader.loadROM( "02",           0x0800, 0x0400, 0xccaf38f6 );
		romLoader.loadROM( "03",           0x1400, 0x0400, 0x8ec9eae2 );
		romLoader.loadROM( "04",           0x1800, 0x0400, 0x68719b30 );
		romLoader.loadROM( "05",           0x1c00, 0x0400, 0x8abe2466 );

		romLoader.loadZip(base_URL);

		return true;
	}

	private boolean rom_lagunar() {
		romLoader.setZip( "lagunar" );

		romLoader.setMemory( memCpu1 );
		romLoader.loadROM( "lagunar.h",    0x0000, 0x0800, 0x0cd5a280 );
		romLoader.loadROM( "lagunar.g",    0x0800, 0x0800, 0x824cd6f5 );
		romLoader.loadROM( "lagunar.f",    0x1000, 0x0800, 0x62692ca7 );
		romLoader.loadROM( "lagunar.e",    0x1800, 0x0800, 0x20e098ed );

		romLoader.loadZip(base_URL);

		return true;
	}

	private boolean rom_maze() {
		romLoader.setZip( "maze" );

		romLoader.setMemory( memCpu1 );
		romLoader.loadROM( "invaders.h",   0x0000, 0x0800, 0xf2860cff );
		romLoader.loadROM( "invaders.g",   0x0800, 0x0800, 0x65fad839 );

		romLoader.loadZip(base_URL);

		return true;
	}

	private boolean rom_checkmat () {
		romLoader.setZip( "checkmat" );

		romLoader.setMemory( memCpu1 );
		romLoader.loadROM( "checkmat.h",   0x0000, 0x0400, 0x3481a6d1 );
		romLoader.loadROM( "checkmat.g",   0x0400, 0x0400, 0xdf5fa551 );
		romLoader.loadROM( "checkmat.f",   0x0800, 0x0400, 0x25586406 );
		romLoader.loadROM( "checkmat.e",   0x0c00, 0x0400, 0x59330d84 );

		romLoader.loadZip(base_URL);

		return true;
	}

	private boolean rom_boothill () {
		romLoader.setZip( "boothill" );

		romLoader.setMemory( memCpu1 );
		romLoader.loadROM( "romh.cpu",     0x0000, 0x0800, 0x1615d077 );
		romLoader.loadROM( "romg.cpu",     0x0800, 0x0800, 0x65a90420 );
		romLoader.loadROM( "romf.cpu",     0x1000, 0x0800, 0x3fdafd79 );
		romLoader.loadROM( "rome.cpu",     0x1800, 0x0800, 0x374529f4 );

		romLoader.loadZip(base_URL);

		return true;
	}

	public Machine getMachine(URL url, String name) {
		super.getMachine(url,name);

		in[0] = new InputPort();
		in[1] = new InputPort();
		in[2] = new InputPort();
		in[3] = new InputPort();

		input_port_0_r = (ReadHandler)in[0];
		input_port_1_r = (ReadHandler)in[1];
		input_port_2_r = (ReadHandler)in[2];
		input_port_3_r = (ReadHandler)in[3];

		super.setVideoEmulator(v);

	/* Midway games */

		if (name.equals("280zzzap")) {
			md = machine_driver_280zzzap();
			/* 610 */
			GAME(1976, rom_280zzzap(), ipt_280zzzap(), v.bw8080(),   ROT0,   "Midway", "Datsun 280 Zzzap" );
		} else if (name.equals("maze")) {
			md = machine_driver_invaders();
			/* 611 */
			GAME(1976, rom_maze(), ipt_maze(),  	   v.bw8080(),   ROT0,   "Midway", "Amazing Maze" );
		} else if (name.equals("boothill")) {
			md = machine_driver_boothill();
			/* 612 */
			GAME(1977, rom_boothill(), ipt_boothill(), v.bw8080(),   ROT0,   "Midway", "Boot Hill" );
		} else if (name.equals("checkmat")) {
			md = machine_driver_checkmat();
			/* 615 */
			GAME(1977, rom_checkmat(), ipt_checkmat(), v.bw8080(),   ROT0,   "Midway", "Checkmate" );
		} else if (name.equals("lagunar")) {
			md = machine_driver_280zzzap();
			/* 622 */
			GAME(1977, rom_lagunar(), ipt_lagunar(),   v.bw8080(),   ROT90,  "Midway", "Laguna Racer" );
		} else if (name.equals("invaders")) {
			md = machine_driver_invaders();
			/* 739 */
			GAME(1978, rom_invaders(), ipt_invaders(), v.invaders(), ROT270, "Midway", "Space Invaders" );

/* Taito games */

		} else if (name.equals("sitv")) {
			md = machine_driver_invaders();
			GAME(1978, rom_sitv(),    "invaders", ipt_sitv(),     v.invaders(), ROT270, "Taito", "Space Invaders (TV Version)" );
		} else if (name.equals("sicv")) {
			md = machine_driver_invaders();
			GAME(1979, rom_sicv(),    "invaders", ipt_invaders(), v.invadpt2(), ROT270, "Taito", "Space Invaders (CV Version)" );
		} else if (name.equals("sisv")) {
			md = machine_driver_invaders();
			GAME(1978, rom_sisv(),    "invaders", ipt_invaders(), v.invadpt2(), ROT270, "Taito", "Space Invaders (SV Version)" );
		} else if (name.equals("sisv2")) {
			md = machine_driver_invaders();
			GAME(1978, rom_sisv2(),   "invaders", ipt_invaders(), v.invadpt2(), ROT270, "Taito", "Space Invaders (SV Version 2)" );
		} else if (name.equals("invadpt2")) {
			md = machine_driver_invadpt2();
			GAME(1980, rom_invadpt2(),"invaders", ipt_invadpt2(), v.invadpt2(), ROT270, "Taito", "Space Invaders Part II (Taito)" );

/* Misc. manufacturers */

		} else if (name.equals("earthinv")) {
			md = machine_driver_invaders();
			GAME(1980, rom_earthinv(),"invaders", ipt_earthinv(), v.invaders(), ROT270, "bootleg", "Super Earth Invasion" );
		} else if (name.equals("spaceatt")) {
			md = machine_driver_invaders();
			GAME(1978, rom_spaceatt(),"invaders", ipt_invaders(), v.invaders(), ROT270, "Video Games GMBH", "Space Attack" );
		} else if (name.equals("spaceat2")) {
			md = machine_driver_invaders();
			GAME(1980, rom_spaceat2(),"invaders", ipt_spaceatt(), v.invaders(), ROT270, "Zenitone-Microsec Ltd", "Space Attack II" );
		} else if (name.equals("sinvzen")) {
			md = machine_driver_invaders();
			GAME(   0, rom_sinvzen(), "invaders", ipt_spaceatt(), v.invaders(), ROT270, "Zenitone-Microsec Ltd", "Super Invaders (Zenitone-Microsec)" );
		} else if (name.equals("sinvemag")) {
			md = machine_driver_invaders();
			GAME(   0, rom_sinvemag(),"invaders", ipt_sinvemag(), v.invaders(), ROT270, "bootleg", "Super Invaders (EMAG)" );
		} else if (name.equals("alieninv")) {
			md = machine_driver_invaders();
			GAME(   0, rom_alieninv(),"invaders", ipt_earthinv(), v.invaders(), ROT270, "bootleg", "Alien Invasion Part II" );
		} else if (name.equals("spceking")) {
			md = machine_driver_invaders();
			GAME(1978, rom_spceking(),"invaders", ipt_spceking(), v.invaders(), ROT270, "Leijac (Konami)", "Space King" );
		} else if (name.equals("spcewars")) {
			md = machine_driver_invaders();
			GAME(1978, rom_spcewars(),"invaders", ipt_invadpt2(), v.invaders(), ROT270, "Sanritsu", "Space War (Sanritsu)" );
		} else if (name.equals("spacewr3")) {
			md = machine_driver_invaders();
			GAME(1978, rom_spacewr3(),"invaders", ipt_spacewr3(), v.invaders(), ROT270, "bootleg", "Space War Part 3" );
		} else if (name.equals("invaderl")) {
			md = machine_driver_invaders();
			GAME(1978, rom_invaderl(),"invaders", ipt_invaders(), v.invaders(), ROT270, "bootleg", "Space Invaders (Logitec)" );
		} else if (name.equals("jspecter")) {
			md = machine_driver_invaders();
			GAME(1979, rom_jspecter(),"invaders", ipt_jspecter(), v.invaders(), ROT270, "Jatre", "Jatre Specter (set 1)" );
		} else if (name.equals("jspectr2")) {
			md = machine_driver_invaders();
			GAME(1979, rom_jspectr2(),"invaders", ipt_jspecter(), v.invaders(), ROT270, "Jatre", "Jatre Specter (set 2)" );
		} else if (name.equals("cosmicmo")) {
			md = machine_driver_invaders();
			GAME(1979, rom_cosmicmo(),"invaders", ipt_cosmicmo(), v.invaders(), ROT270, "Universal", "Cosmic Monsters" );
		} else if (name.equals("superinv")) {
			md = machine_driver_invaders();
			GAME(   0, rom_superinv(),"invaders", ipt_invaders(), v.invaders(), ROT270, "bootleg", "Super Invaders" );
		} else if (name.equals("invrvnge")) {
			md = machine_driver_invaders();
			GAME(   0, rom_invrvnge(),"invaders", ipt_invrvnge(), v.invrvnge(), ROT270, "Zenitone Microsec", "Invader's Revenge" );
		} else if (name.equals("invrvnga")) {
			md = machine_driver_invaders();
			GAME(   0, rom_invrvnga(),"invaders", ipt_invrvnge(), v.invrvnge(), ROT270, "Zenitone Microsec (Dutchford license)", "Invader's Revenge (Dutchford)" );
		}

		v.setRegions(REGION_PROMS);
		m.init(md);
		return (Machine)m;
	}
}