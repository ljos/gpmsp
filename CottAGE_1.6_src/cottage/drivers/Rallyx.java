/*
CottAGE - the Arcade Generic Emulator in Java

Java driver by LFE, Gollum
*/

/***************************************************************************

Rally X memory map (preliminary)

driver by Nicola Salmoria


0000-3fff ROM
8000-83ff Radar video RAM + other
8400-87ff video RAM
8800-8bff Radar color RAM + other
8c00-8fff color RAM
9800-9fff RAM

memory mapped ports:

read:
a000	  IN0
a080	  IN1
a100	  DSW1

write:
8014-801f sprites - 6 pairs: code (including flipping) and X position
8814-881f sprites - 6 pairs: Y position and color
8034-880c radar car indicators x position
8834-883c radar car indicators y position
a004-a00c radar car indicators color and x position MSB
a080	  watchdog reset
a105	  sound voice 1 waveform (nibble)
a111-a113 sound voice 1 frequency (nibble)
a115	  sound voice 1 volume (nibble)
a10a	  sound voice 2 waveform (nibble)
a116-a118 sound voice 2 frequency (nibble)
a11a	  sound voice 2 volume (nibble)
a10f	  sound voice 3 waveform (nibble)
a11b-a11d sound voice 3 frequency (nibble)
a11f	  sound voice 3 volume (nibble)
a130	  virtual screen X scroll position
a140	  virtual screen Y scroll position
a170	  ? this is written to A LOT of times every frame
a180	  explosion sound trigger
a181	  interrupt enable
a182	  ?
a183	  flip screen
a184	  1 player start lamp
a185	  2 players start lamp
a186	  coin lockout
a187	  coin counter

I/O ports:
OUT on port $0 sets the interrupt vector/instruction (the game uses both
IM 2 and IM 0)

***************************************************************************/

package cottage.drivers;

import java.net.URL;

import jef.machine.Machine;
import jef.map.InterruptHandler;
import jef.map.WriteHandler;
import jef.video.Vh_convert_color_proms;
import jef.video.Vh_refresh;
import jef.video.Vh_start;

import cottage.mame.Driver;
import cottage.mame.MAMEConstants;
import cottage.mame.MAMEDriver;

public class Rallyx extends MAMEDriver implements Driver, MAMEConstants {

cottage.vidhrdw.Rallyx v = new cottage.vidhrdw.Rallyx();
Vh_start rallyx_vs = (Vh_start)v;
Vh_refresh rallyx_vu = (Vh_refresh)v;
Vh_convert_color_proms rallyx_pi = (Vh_convert_color_proms)v;
WriteHandler colorram_w = v.colorram_w();
WriteHandler videoram_w = v.videoram_w();

int[] rallyx_videoram2 = v.Frallyx_videoram2;
int[] rallyx_colorram2 = v.Frallyx_colorram2;
int[] rallyx_radarx = v.Frallyx_radarx;
int[] rallyx_radary = v.Frallyx_radary;
int[] rallyx_radarattr = v.Frallyx_radarattr;
int[] rallyx_radarram_size = v.Frallyx_radarram_size;
int[] rallyx_scrollx = v.Frallyx_scrollx;
int[] rallyx_scrolly = v.Frallyx_scrolly;

WriteHandler rallyx_videoram2_w = v.rallyx_videoram2_w();
WriteHandler rallyx_colorram2_w = v.rallyx_colorram2_w();
WriteHandler rallyx_flipscreen_w = v.rallyx_flipscreen_w();

WriteHandler rallyx_coin_lockout_w 	= (WriteHandler) new Rallyx_coin_lockout_w();
WriteHandler rallyx_coin_counter_w 	= (WriteHandler) new Rallyx_coin_counter_w();
WriteHandler rallyx_leds_w 	= (WriteHandler) new Rallyx_leds_w();
WriteHandler Rallyx_play_sound_w = (WriteHandler) new Rallyx_play_sound_w();


jef.machine.BasicMachine m = new jef.machine.BasicMachine();
InterruptHandler irq0_line_hold = m.irq0_line_hold();
WriteHandler interrupt_enable_w = m.interrupt_enable();

WriteHandler interrupt_vector_w = new Interrupt_vector_w();

public class Interrupt_vector_w implements WriteHandler {
	public void write(int address, int value) {
		m.cd[0].cpu.setProperty(0,value);
	}
}

	public class Rallyx_coin_lockout_w implements WriteHandler {
		public void write(int offset,int data) { 
			//coin_lockout_w(offset, data ^ 1);
		}
	}

	public class Rallyx_coin_counter_w implements WriteHandler {
		public void write(int offset,int data) { 
			//coin_counter_w(offset, data);
		}
	}

	public class Rallyx_leds_w implements WriteHandler {
		public void write(int offset,int data) { 
			//set_led_status(offset,data & 1);
		}
	}

	public class Rallyx_play_sound_w implements WriteHandler {
		public void write(int offset,int data) { 
			/*
			int last;

			if (data == 0 && last != 0)
				sample_start(0,0,0);

			last = data;
			*/
		}
	}

private boolean readmem() {
	MR_START( 0x0000, 0x3fff, MRA_ROM );
	MR_ADD( 0x8000, 0x8fff, MRA_RAM );
	MR_ADD( 0x9800, 0x9fff, MRA_RAM );
	MR_ADD( 0xa000, 0xa000, input_port_0_r ); /* IN0 */
	MR_ADD( 0xa080, 0xa080, input_port_1_r ); /* IN1 */
	MR_ADD( 0xa100, 0xa100, input_port_2_r ); /* DSW1 */
	return true;
}


private boolean writemem() {
	MW_START( 0x0000, 0x3fff, MWA_ROM );
	MW_ADD( 0x8000, 0x83ff, videoram_w, videoram, videoram_size );
	MW_ADD( 0x8400, 0x87ff, rallyx_videoram2_w, rallyx_videoram2 );
	MW_ADD( 0x8800, 0x8bff, colorram_w, colorram );
	MW_ADD( 0x8c00, 0x8fff, rallyx_colorram2_w, rallyx_colorram2 );
	MW_ADD( 0x9800, 0x9fff, MWA_RAM );
	MW_ADD( 0xa004, 0xa00f, MWA_RAM, rallyx_radarattr );
	//MW_ADD( 0xa080, 0xa080, watchdog_reset_w );
	//MW_ADD( 0xa100, 0xa11f, pengo_sound_w, pengo_soundregs );
	MW_ADD( 0xa130, 0xa130, MWA_RAM, rallyx_scrollx );
	MW_ADD( 0xa140, 0xa140, MWA_RAM, rallyx_scrolly );
	//MW_ADD( 0xa170, 0xa170, MWA_NOP );	/* ????? */
	//MW_ADD( 0xa180, 0xa180, rallyx_play_sound_w );
	MW_ADD( 0xa181, 0xa181, interrupt_enable_w );
	MW_ADD( 0xa183, 0xa183, rallyx_flipscreen_w );
	MW_ADD( 0xa184, 0xa185, rallyx_leds_w );
	MW_ADD( 0xa186, 0xa186, rallyx_coin_lockout_w );
	MW_ADD( 0xa187, 0xa187, rallyx_coin_counter_w );
	MW_ADD( 0x8014, 0x801f, MWA_RAM, spriteram, spriteram_size );	/* these are here just to initialize */
	MW_ADD( 0x8814, 0x881f, MWA_RAM, spriteram_2 );	/* the pointers. */
	MW_ADD( 0x8034, 0x803f, MWA_RAM, rallyx_radarx, rallyx_radarram_size ); /* ditto */
	MW_ADD( 0x8834, 0x883f, MWA_RAM, rallyx_radary );
	return true;
}

private boolean writeport() {
	PW_START( 0, 0, interrupt_vector_w );
	return true;
}

private boolean ipt_rallyx() {
	PORT_START();		/* IN0 */
	PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_COIN3 );
	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_BUTTON1 );
	PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT | IPF_4WAY );
	PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT |IPF_4WAY );
	PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN | IPF_4WAY );
	PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_JOYSTICK_UP | IPF_4WAY );
	PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_START1 );
	PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_COIN1 );

	PORT_START();		/* IN1 */
	PORT_DIPNAME( 0x01, 0x01, DEF_STR2( Cabinet ) );
	PORT_DIPSETTING(	0x01, DEF_STR2( Upright ) );
	PORT_DIPSETTING(	0x00, DEF_STR2( Cocktail ) );
	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_COCKTAIL );
	PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT | IPF_4WAY | IPF_COCKTAIL );
	PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_4WAY | IPF_COCKTAIL );
	PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN | IPF_4WAY | IPF_COCKTAIL );
	PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_JOYSTICK_UP | IPF_4WAY | IPF_COCKTAIL );
	PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_START2 );
	PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_COIN2 );

	PORT_START();		/* DSW0 */
	PORT_SERVICE( 0x01, IP_ACTIVE_LOW );
	/* TODO: the bonus score depends on the number of lives */
	PORT_DIPNAME( 0x06, 0x02, DEF_STR2( Bonus_Life ) );
	PORT_DIPSETTING(	0x02, "A" );
	PORT_DIPSETTING(	0x04, "B" );
	PORT_DIPSETTING(	0x06, "C" );
	PORT_DIPSETTING(	0x00, "None" );
	PORT_DIPNAME( 0x38, 0x08, DEF_STR2( Difficulty ) );
	PORT_DIPSETTING(	0x10, "1 Car, Medium" );
	PORT_DIPSETTING(	0x28, "1 Car, Hard" );
	PORT_DIPSETTING(	0x00, "2 Cars, Easy" );
	PORT_DIPSETTING(	0x18, "2 Cars, Medium" );
	PORT_DIPSETTING(	0x30, "2 Cars, Hard" );
	PORT_DIPSETTING(	0x08, "3 Cars, Easy" );
	PORT_DIPSETTING(	0x20, "3 Cars, Medium" );
	PORT_DIPSETTING(	0x38, "3 Cars, Hard" );
	PORT_DIPNAME( 0xc0, 0xc0, DEF_STR2( Coinage ) );
	PORT_DIPSETTING(	0x40, DEF_STR2( _2C_1C ) );
	PORT_DIPSETTING(	0xc0, DEF_STR2( _1C_1C ) );
	PORT_DIPSETTING(	0x80, DEF_STR2( _1C_2C ) );
	PORT_DIPSETTING(	0x00, DEF_STR2( Free_Play ) );
	return true;
}

private boolean ipt_nrallyx() {
	PORT_START();		/* IN0 */
	PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_COIN3 );
	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_BUTTON1 );
	PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT | IPF_4WAY );
	PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT |IPF_4WAY );
	PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN | IPF_4WAY );
	PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_JOYSTICK_UP | IPF_4WAY );
	PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_START1 );
	PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_COIN1 );

	PORT_START();		/* IN1 */
	PORT_DIPNAME( 0x01, 0x01, DEF_STR2( Cabinet ) );
	PORT_DIPSETTING(	0x01, DEF_STR2( Upright ) );
	PORT_DIPSETTING(	0x00, DEF_STR2( Cocktail ) );
	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_COCKTAIL );
	PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT | IPF_4WAY | IPF_COCKTAIL );
	PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_4WAY | IPF_COCKTAIL );
	PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN | IPF_4WAY | IPF_COCKTAIL );
	PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_JOYSTICK_UP | IPF_4WAY | IPF_COCKTAIL );
	PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_START2 );
	PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_COIN2 );

	PORT_START();		/* DSW0 */
	PORT_SERVICE( 0x01, IP_ACTIVE_LOW );
	/* TODO: the bonus score depends on the number of lives */
	PORT_DIPNAME( 0x06, 0x02, DEF_STR2( Bonus_Life ) );
	PORT_DIPSETTING(	0x02, "A" );
	PORT_DIPSETTING(	0x04, "B" );
	PORT_DIPSETTING(	0x06, "C" );
	PORT_DIPSETTING(	0x00, "None" );
	PORT_DIPNAME( 0x38, 0x00, DEF_STR2( Difficulty ) );
	PORT_DIPSETTING(	0x10, "1 Car, Medium" );
	PORT_DIPSETTING(	0x28, "1 Car, Hard" );
	PORT_DIPSETTING(	0x18, "2 Cars, Medium" );
	PORT_DIPSETTING(	0x30, "2 Cars, Hard" );
	PORT_DIPSETTING(	0x00, "3 Cars, Easy" );
	PORT_DIPSETTING(	0x20, "3 Cars, Medium" );
	PORT_DIPSETTING(	0x38, "3 Cars, Hard" );
	PORT_DIPSETTING(	0x08, "4 Cars, Easy" );
	PORT_DIPNAME( 0xc0, 0xc0, DEF_STR2( Coinage ) );
	PORT_DIPSETTING(	0x40, DEF_STR2( _2C_1C ) );
	PORT_DIPSETTING(	0xc0, DEF_STR2( _1C_1C ) );
	PORT_DIPSETTING(	0x80, DEF_STR2( _1C_2C ) );
	PORT_DIPSETTING(	0x00, DEF_STR2( Free_Play ) );
	return true;
}

int[][] charlayout =
{
	{8},{8},	/* 8*8 characters */
	{256},	/* 256 characters */
	{2},	/* 2 bits per pixel */
	{ 0, 4 },	/* the two bitplanes for 4 pixels are packed into one byte */
	{ 8*8+0, 8*8+1, 8*8+2, 8*8+3, 0, 1, 2, 3 }, /* bits are packed in groups of four */
	{ 0*8, 1*8, 2*8, 3*8, 4*8, 5*8, 6*8, 7*8 },
	{16*8}	/* every char takes 16 bytes */
};

int[][] spritelayout =
{
	{16},{16},	/* 16*16 sprites */
	{64}, /* 64 sprites */
	{2},	/* 2 bits per pixel */
	{ 0, 4 },	/* the two bitplanes for 4 pixels are packed into one byte */
	{ 8*8+0, 8*8+1, 8*8+2, 8*8+3, 16*8+0, 16*8+1, 16*8+2, 16*8+3,	/* bits are packed in groups of four */
			 24*8+0, 24*8+1, 24*8+2, 24*8+3, 0, 1, 2, 3 },
	{ 0*8, 1*8, 2*8, 3*8, 4*8, 5*8, 6*8, 7*8,
			32*8, 33*8, 34*8, 35*8, 36*8, 37*8, 38*8, 39*8 },
	{64*8}	/* every sprite takes 64 bytes */
};

int[][] dotlayout =
{
	{4},{4},	/* 4*4 characters */
	{8},	/* 8 characters */
	{2},	/* 2 bits per pixel */
	{ 6, 7 },
	{ 0*8, 1*8, 2*8, 3*8 },
	{ 0*32, 1*32, 2*32, 3*32 },
	{16*8}	/* every char takes 16 consecutive bytes */
};



private boolean gfxdecodeinfo() {
	GDI_ADD( REGION_GFX1, 0, charlayout,	  0, 64 );
	GDI_ADD( REGION_GFX1, 0, spritelayout,	  0, 64 );
	GDI_ADD( REGION_GFX2, 0, dotlayout,	   64*4,  1 );
	GDI_ADD( -1 ); /* end of array */
	return true;
};

public boolean mdrv_rallyx() {
	/* basic machine hardware */
	MDRV_CPU_ADD(Z80, 3072000);	/* 3.072 MHz ? */
	MDRV_CPU_MEMORY(readmem(),writemem());
	MDRV_CPU_PORTS(0,writeport());
	MDRV_CPU_VBLANK_INT(irq0_line_hold,1);

	MDRV_FRAMES_PER_SECOND(60.606060);
	MDRV_VBLANK_DURATION(DEFAULT_60HZ_VBLANK_DURATION);

	/* video hardware */
	MDRV_VIDEO_ATTRIBUTES(VIDEO_TYPE_RASTER);
	MDRV_SCREEN_SIZE(36*8, 28*8);
	MDRV_VISIBLE_AREA(0*8, 36*8-1, 0*8, 28*8-1);
	MDRV_GFXDECODE(gfxdecodeinfo());
	MDRV_PALETTE_LENGTH(32);
	MDRV_COLORTABLE_LENGTH(64*4+4);

	MDRV_PALETTE_INIT(rallyx_pi);
	MDRV_VIDEO_START(rallyx_vs);
	MDRV_VIDEO_UPDATE(rallyx_vu);

	/* sound hardware */
//	MDRV_SOUND_ADD(NAMCO, namco_interface);
//	MDRV_SOUND_ADD(SAMPLES, samples_interface);
    return true;
}

/***************************************************************************

  Game driver(s)

***************************************************************************/

private boolean rom_rallyx() {
	ROM_REGION( 0x10000, REGION_CPU1, 0 );	/* 64k for code */
	ROM_LOAD( "1b",           0x0000, 0x1000, 0x5882700d );
	ROM_LOAD( "rallyxn.1e",   0x1000, 0x1000, 0xed1eba2b );
	ROM_LOAD( "rallyxn.1h",   0x2000, 0x1000, 0x4f98dd1c );
	ROM_LOAD( "rallyxn.1k",   0x3000, 0x1000, 0x9aacccf0 );

	ROM_REGION( 0x1000, REGION_GFX1, ROMREGION_DISPOSE );
	ROM_LOAD( "8e",           0x0000, 0x1000, 0x277c1de5 );

	ROM_REGION( 0x0100, REGION_GFX2, ROMREGION_DISPOSE );
	ROM_LOAD( "im5623.8m",    0x0000, 0x0100, 0x3c16f62c );  /* dots */

	ROM_REGION( 0x0120, REGION_PROMS, 0 );
	ROM_LOAD( "m3-7603.11n",  0x0000, 0x0020, 0xc7865434 );
	ROM_LOAD( "im5623.8p",    0x0020, 0x0100, 0x834d4fda );

	ROM_REGION( 0x0200, REGION_SOUND1, 0 ); /* sound proms */
	ROM_LOAD( "im5623.3p",    0x0000, 0x0100, 0x4bad7017 );
	ROM_LOAD( "im5623.2m",    0x0100, 0x0100, 0x77245b66 ) ; /* timing - not used */
	return true;
}

private boolean rom_rallyxm() {
	ROM_REGION( 0x10000, REGION_CPU1, 0 );	/* 64k for code */
	ROM_LOAD( "1b",           0x0000, 0x1000, 0x5882700d );
	ROM_LOAD( "1e",           0x1000, 0x1000, 0x786585ec );
	ROM_LOAD( "1h",           0x2000, 0x1000, 0x110d7dcd );
	ROM_LOAD( "1k",           0x3000, 0x1000, 0x473ab447 );

	ROM_REGION( 0x1000, REGION_GFX1, ROMREGION_DISPOSE );
	ROM_LOAD( "8e",           0x0000, 0x1000, 0x277c1de5 );

	ROM_REGION( 0x0100, REGION_GFX2, ROMREGION_DISPOSE );
	ROM_LOAD( "im5623.8m",    0x0000, 0x0100, 0x3c16f62c );  /* dots */

	ROM_REGION( 0x0120, REGION_PROMS, 0 );
	ROM_LOAD( "m3-7603.11n",  0x0000, 0x0020, 0xc7865434 );
	ROM_LOAD( "im5623.8p",    0x0020, 0x0100, 0x834d4fda );

	ROM_REGION( 0x0200, REGION_SOUND1, 0 ); /* sound proms */
	ROM_LOAD( "im5623.3p",    0x0000, 0x0100, 0x4bad7017 );
	ROM_LOAD( "im5623.2m",    0x0100, 0x0100, 0x77245b66 );  /* timing - not used */
	return true;
}

private boolean rom_nrallyx() {
	ROM_REGION( 0x10000, REGION_CPU1, 0 );	/* 64k for code */
	ROM_LOAD( "nrallyx.1b",   0x0000, 0x1000, 0x9404c8d6 );
	ROM_LOAD( "nrallyx.1e",   0x1000, 0x1000, 0xac01bf3f );
	ROM_LOAD( "nrallyx.1h",   0x2000, 0x1000, 0xaeba29b5 );
	ROM_LOAD( "nrallyx.1k",   0x3000, 0x1000, 0x78f17da7 );

	ROM_REGION( 0x1000, REGION_GFX1, ROMREGION_DISPOSE );
	ROM_LOAD( "nrallyx.8e",   0x0000, 0x1000, 0xca7a174a );

	ROM_REGION( 0x0100, REGION_GFX2, ROMREGION_DISPOSE );
	ROM_LOAD( "im5623.8m",    0x0000, 0x0100, 0x3c16f62c );    /* dots */

	ROM_REGION( 0x0120, REGION_PROMS, 0 );
	ROM_LOAD( "nrallyx.pr1",  0x0000, 0x0020, 0xa0a49017 );
	ROM_LOAD( "nrallyx.pr2",  0x0020, 0x0100, 0xb2b7ca15 );

	ROM_REGION( 0x0200, REGION_SOUND1, 0 ); /* sound proms */
	ROM_LOAD( "nrallyx.spr",  0x0000, 0x0100, 0xb75c4e87 );
	ROM_LOAD( "im5623.2m",    0x0100, 0x0100, 0x77245b66 );  /* timing - not used */
	return true;
}

public Machine getMachine(URL url, String name) {
	super.getMachine(url,name);
	super.setVideoEmulator(v);
	if (name.equals("rallyx")) {
		GAME(1980, rom_rallyx(),  0, mdrv_rallyx(), ipt_rallyx(), 0, ROT0, "Namco", "Rally X" );
	}

	m.init(md);

	return (Machine)m;
}

}





