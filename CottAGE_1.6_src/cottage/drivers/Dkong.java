/*
CottAGE - the Arcade Generic Emulator in Java

Java driver by Erik Duijs, Gollum
*/

/***************************************************************************

TODO:
- Radarscope does a check on bit 6 of 7d00 which prevent it from working.
  It's a sound status flag, maybe signaling whan a tune is finished.
  For now, we comment it out.

- radarscp_grid_color_w() is wrong, it probably isn't supposed to change
  the grid color. There are reports of the grid being constantly blue in
  the real game, the flyer confirms this.


Donkey Kong and Donkey Kong Jr. memory map (preliminary) (DKong 3 follows)

0000-3fff ROM (Donkey Kong Jr.and Donkey Kong 3: 0000-5fff)
6000-6fff RAM
6900-6a7f sprites
7000-73ff ?
7400-77ff Video RAM
8000-9fff ROM (DK3 only)



memory mapped ports:

read:
7c00      IN0
7c80      IN1
7d00      IN2 (DK3: DSW2)
7d80      DSW1

*
 * IN0 (bits NOT inverted)
 * bit 7 : ?
 * bit 6 : reset (when player 1 active)
 * bit 5 : ?
 * bit 4 : JUMP player 1
 * bit 3 : DOWN player 1
 * bit 2 : UP player 1
 * bit 1 : LEFT player 1
 * bit 0 : RIGHT player 1
 *
*
 * IN1 (bits NOT inverted)
 * bit 7 : ?
 * bit 6 : reset (when player 2 active)
 * bit 5 : ?
 * bit 4 : JUMP player 2
 * bit 3 : DOWN player 2
 * bit 2 : UP player 2
 * bit 1 : LEFT player 2
 * bit 0 : RIGHT player 2
 *
*
 * IN2 (bits NOT inverted)
 * bit 7 : COIN (IS inverted in Radarscope)
 * bit 6 : ? Radarscope does some wizardry with this bit
 * bit 5 : ?
 * bit 4 : ?
 * bit 3 : START 2
 * bit 2 : START 1
 * bit 1 : ?
 * bit 0 : ? if this is 1, the code jumps to $4000, outside the rom space
 *
*
 * DSW1 (bits NOT inverted)
 * bit 7 : COCKTAIL or UPRIGHT cabinet (1 = UPRIGHT)
 * bit 6 : \ 000 = 1 coin 1 play   001 = 2 coins 1 play  010 = 1 coin 2 plays
 * bit 5 : | 011 = 3 coins 1 play  100 = 1 coin 3 plays  101 = 4 coins 1 play
 * bit 4 : / 110 = 1 coin 4 plays  111 = 5 coins 1 play
 * bit 3 : \bonus at
 * bit 2 : / 00 = 7000  01 = 10000  10 = 15000  11 = 20000
 * bit 1 : \ 00 = 3 lives  01 = 4 lives
 * bit 0 : / 10 = 5 lives  11 = 6 lives
 *

write:
7800-7803 ?
7808      ?
7c00      Background sound/music select:
          00 - nothing
		  01 - Intro tune
		  02 - How High? (intermisson) tune
		  03 - Out of time
		  04 - Hammer
		  05 - Rivet level 2 completed (end tune)
		  06 - Hammer hit
		  07 - Standard level end
		  08 - Background 1	(first screen)
		  09 - ???
		  0A - Background 3	(springs)
		  0B - Background 2 (rivet)
		  0C - Rivet level 1 completed (end tune)
		  0D - Rivet removed
		  0E - Rivet level completed
		  0F - Gorilla roar
7c80      gfx bank select (Donkey Kong Jr. only)
7d00      digital sound trigger - walk
7d01      digital sound trigger - jump
7d02      digital sound trigger - boom (gorilla stomps foot)
7d03      digital sound trigger - coin input/spring
7d04      digital sound trigger	- gorilla fall
7d05      digital sound trigger - barrel jump/prize
7d06      ?
7d07      ?
7d80      digital sound trigger - dead
7d82      flip screen
7d83      ?
7d84      interrupt enable
7d85      0/1 toggle
7d86-7d87 palette bank selector (only bit 0 is significant: 7d86 = bit 0 7d87 = bit 1)


8035 Memory Map:

0000-07ff ROM
0800-0fff Compressed sound sample (Gorilla roar in DKong)

Read ports:
0x20   Read current tune
P2.5   Active low when jumping
T0     Select sound for jump (Normal or Barrell?)
T1     Active low when gorilla is falling

Write ports:
P1     Digital out
P2.7   External decay
P2.6   Select second ROM reading (MOVX instruction will access area 800-fff)
P2.2-0 Select the bank of 256 bytes for second ROM



Donkey Kong 3 memory map (preliminary):

RAM and read ports same as above;

write:
7d00      ?
7d80      ?
7e00      ?
7e80
7e81      char bank selector
7e82      flipscreen
7e83      ?
7e84      interrupt enable
7e85      ?
7e86-7e87 palette bank selector (only bit 0 is significant: 7e86 = bit 0 7e87 = bit 1)


I/O ports

write:
00        ?

Changes:
	Apr 7 98 Howie Cohen
	* Added samples for the climb, jump, land and walking sounds

	Jul 27 99 Chad Hendrickson
	* Added cocktail mode flipscreen

***************************************************************************/

package cottage.drivers;

import java.net.URL;

import jef.machine.Machine;
import jef.map.InitHandler;
import jef.map.InterruptHandler;
import jef.map.ReadHandler;
import jef.map.WriteHandler;
import jef.video.Vh_convert_color_proms;
import jef.video.Vh_refresh;
import jef.video.Vh_start;

import cottage.mame.MAMEDriver;

public class Dkong extends MAMEDriver {

int mcustatus = 0;

cottage.vidhrdw.Dkong v 			 = new cottage.vidhrdw.Dkong();
WriteHandler 	videoram_w 			 = v.videoram_w();
WriteHandler 	dkong_palettebank_w  = v.dkong_palettebank_w();
WriteHandler 	dkongjr_gfxbank_w    = v.dkongjr_gfxbank_w();
WriteHandler 	dkong3_gfxbank_w     = v.dkong3_gfxbank_w();
WriteHandler 	radarscp_grid_color_w= v.radarscp_grid_color_w();
WriteHandler	radarscp_grid_enable_w=v.radarscp_grid_enable_w();

Vh_start 	 			dkong_vs	 = (Vh_start)v;
Vh_refresh 	 			dkong_vu	 = (Vh_refresh)v;
Vh_refresh 	 			radarscp_vu	 = v.radarscp_vu();
Vh_convert_color_proms  dkong_pi	 = (Vh_convert_color_proms)v;
Vh_convert_color_proms	dkong3_pi	 = v.dkong3_palette_init();


jef.machine.BasicMachine m 		= new jef.machine.BasicMachine();
InterruptHandler nmi_line_pulse = m.nmi_interrupt_switched();
WriteHandler interrupt_enable_w = m.nmi_interrupt_enable();

ReadHandler dkong_in2_r		= (ReadHandler) new Dkong_in2_r();

class Dkong_in2_r implements ReadHandler {
	public int read(int address) {
		return input_port_2_r.read(address) | (mcustatus << 6);
	}
}

private boolean readmem() {
	MR_START();
	MR_ADD( 0x0000, 0x5fff, MRA_ROM );	/* DK: 0000-3fff */
	MR_ADD( 0x6000, 0x6fff, MRA_RAM );	/* including sprites RAM */
	MR_ADD( 0x7400, 0x77ff, MRA_RAM );	/* video RAM */
	MR_ADD( 0x7c00, 0x7c00, input_port_0_r );	/* IN0 */
	MR_ADD( 0x7c80, 0x7c80, input_port_1_r );	/* IN1 */
	MR_ADD( 0x7d00, 0x7d00, dkong_in2_r );	/* IN2/DSW2 */
	MR_ADD( 0x7d80, 0x7d80, input_port_3_r );	/* DSW1 */
	MR_ADD( 0x8000, 0x9fff, MRA_ROM );	/* DK3 and bootleg DKjr only */
	return true;
}

private boolean dkong3_readmem() {
	MR_START();
	MR_ADD( 0x0000, 0x5fff, MRA_ROM );	/* DK: 0000-3fff */
	MR_ADD( 0x6000, 0x6fff, MRA_RAM );	/* including sprites RAM */
	MR_ADD( 0x7400, 0x77ff, MRA_RAM );	/* video RAM */
	MR_ADD( 0x7c00, 0x7c00, input_port_0_r );	/* IN0 */
	MR_ADD( 0x7c80, 0x7c80, input_port_1_r );	/* IN1 */
	MR_ADD( 0x7d00, 0x7d00, input_port_2_r );	/* IN2/DSW2 */
	MR_ADD( 0x7d80, 0x7d80, input_port_3_r );	/* DSW1 */
	MR_ADD( 0x8000, 0x9fff, MRA_ROM );	/* DK3 and bootleg DKjr only */
	return true;
}

private boolean dkong_writemem() {
	
	MW_START();
	MW_ADD( 0x0000, 0x5fff, MWA_ROM );
	MW_ADD( 0x6000, 0x68ff, MWA_RAM );
	MW_ADD( 0x6900, 0x6a7f, MWA_RAM, spriteram, spriteram_size );
	MW_ADD( 0x6a80, 0x6fff, MWA_RAM );
	MW_ADD( 0x7000, 0x73ff, MWA_RAM );    /* ???? */
	MW_ADD( 0x7400, 0x77ff, videoram_w, videoram, videoram_size );
	MW_ADD( 0x7800, 0x7803, MWA_RAM );	/* ???? */
	MW_ADD( 0x7808, 0x7808, MWA_RAM );	/* ???? */
	//MW_ADD( 0x7c00, 0x7c00, dkong_sh_tuneselect_w );
//	MW_ADD( 0x7c80, 0x7c80,  );
	//MW_ADD( 0x7d00, 0x7d02, dkong_sh1_w );	/* walk/jump/boom sample trigger */
	//MW_ADD( 0x7d03, 0x7d03, dkong_sh_sound3_w );
	//MW_ADD( 0x7d04, 0x7d04, dkong_sh_sound4_w );
	//MW_ADD( 0x7d05, 0x7d05, dkong_sh_sound5_w );
	//MW_ADD( 0x7d80, 0x7d80, dkong_sh_w );
	MW_ADD( 0x7d81, 0x7d81, MWA_RAM );	/* ???? */
	//MW_ADD( 0x7d82, 0x7d82, dkong_flipscreen_w );
	MW_ADD( 0x7d83, 0x7d83, MWA_RAM );
	MW_ADD( 0x7d84, 0x7d84, interrupt_enable_w );
	MW_ADD( 0x7d85, 0x7d85, MWA_RAM );
	MW_ADD( 0x7d86, 0x7d87, dkong_palettebank_w );
	
	return true;
}

private boolean dkongjr_writemem() {
	MW_START();
	MW_ADD( 0x0000, 0x5fff, MWA_ROM );
	MW_ADD( 0x6000, 0x68ff, MWA_RAM );
	MW_ADD( 0x6900, 0x6a7f, MWA_RAM, spriteram, spriteram_size );
	MW_ADD( 0x6a80, 0x6fff, MWA_RAM );
	MW_ADD( 0x7400, 0x77ff, videoram_w, videoram, videoram_size );
	MW_ADD( 0x7800, 0x7803, MWA_RAM );	/* ???? */
	MW_ADD( 0x7808, 0x7808, MWA_RAM );	/* ???? */
	//MW_ADD( 0x7c00, 0x7c00, dkongjr_sh_tuneselect_w );
	MW_ADD( 0x7c80, 0x7c80, dkongjr_gfxbank_w );
	//MW_ADD( 0x7c81, 0x7c81, dkongjr_sh_test6_w );
	//MW_ADD( 0x7d00, 0x7d00, dkongjr_sh_climb_w ); /* HC - climb sound */
	//MW_ADD( 0x7d01, 0x7d01, dkongjr_sh_jump_w ); /* HC - jump */
	//MW_ADD( 0x7d02, 0x7d02, dkongjr_sh_land_w ); /* HC - climb sound */
	//MW_ADD( 0x7d03, 0x7d03, dkongjr_sh_roar_w );
	//MW_ADD( 0x7d04, 0x7d04, dkong_sh_sound4_w );
	//MW_ADD( 0x7d05, 0x7d05, dkong_sh_sound5_w );
	//MW_ADD( 0x7d06, 0x7d06, dkongjr_sh_snapjaw_w );
	//MW_ADD( 0x7d07, 0x7d07, dkongjr_sh_walk_w );	/* controls pitch of the walk/climb? */
	//MW_ADD( 0x7d80, 0x7d80, dkongjr_sh_death_w );
	//MW_ADD( 0x7d81, 0x7d81, dkongjr_sh_drop_w );   /* active when Junior is falling */
	MW_ADD( 0x7d84, 0x7d84, interrupt_enable_w );
	//MW_ADD( 0x7d82, 0x7d82, dkong_flipscreen_w );
	MW_ADD( 0x7d86, 0x7d87, dkong_palettebank_w );
	MW_ADD( 0x8000, 0x9fff, MWA_ROM );	/* bootleg DKjr only */
	return true;
}

private boolean dkong3_writemem() {
	MW_START();
	MW_ADD( 0x0000, 0x5fff, MWA_ROM );
	MW_ADD( 0x6000, 0x68ff, MWA_RAM );
	MW_ADD( 0x6900, 0x6a7f, MWA_RAM, spriteram, spriteram_size );
	MW_ADD( 0x6a80, 0x6fff, MWA_RAM );
	MW_ADD( 0x7400, 0x77ff, videoram_w, videoram, videoram_size );
	//MW_ADD( 0x7c00, 0x7c00, soundlatch_w );
	//MW_ADD( 0x7c80, 0x7c80, soundlatch2_w );
	//MW_ADD( 0x7d00, 0x7d00, soundlatch3_w );
	//MW_ADD( 0x7d80, 0x7d80, dkong3_2a03_reset_w );
	MW_ADD( 0x7e81, 0x7e81, dkong3_gfxbank_w );
	//MW_ADD( 0x7e82, 0x7e82, dkong_flipscreen_w );
	MW_ADD( 0x7e84, 0x7e84, interrupt_enable_w );
	MW_ADD( 0x7e85, 0x7e85, MWA_NOP );	/* ??? */
	MW_ADD( 0x7e86, 0x7e87, dkong_palettebank_w );
	MW_ADD( 0x8000, 0x9fff, MWA_ROM );
	return true;
}

private boolean radarscp_writemem() {
	MW_START();
	MW_ADD( 0x0000, 0x5fff, MWA_ROM );
	MW_ADD( 0x6000, 0x68ff, MWA_RAM );
	MW_ADD( 0x6900, 0x6a7f, MWA_RAM, spriteram, spriteram_size );
	MW_ADD( 0x6a80, 0x6fff, MWA_RAM );
	MW_ADD( 0x7000, 0x73ff, MWA_RAM );    /* ???? */
	MW_ADD( 0x7400, 0x77ff, videoram_w, videoram, videoram_size );
	MW_ADD( 0x7800, 0x7803, MWA_RAM );	/* ???? */
	MW_ADD( 0x7808, 0x7808, MWA_RAM );	/* ???? */
	//MW_ADD( 0x7c00, 0x7c00, dkong_sh_tuneselect_w );
	MW_ADD( 0x7c80, 0x7c80, radarscp_grid_color_w );
	//MW_ADD( 0x7d00, 0x7d02, dkong_sh1_w );	/* walk/jump/boom sample trigger */
	//MW_ADD( 0x7d03, 0x7d03, dkong_sh_sound3_w );
	//MW_ADD( 0x7d04, 0x7d04, dkong_sh_sound4_w );
	//MW_ADD( 0x7d05, 0x7d05, dkong_sh_sound5_w );
	//MW_ADD( 0x7d80, 0x7d80, dkong_sh_w );
	MW_ADD( 0x7d81, 0x7d81, radarscp_grid_enable_w );
	//MW_ADD( 0x7d82, 0x7d82, dkong_flipscreen_w );
	MW_ADD( 0x7d83, 0x7d83, MWA_RAM );
	MW_ADD( 0x7d84, 0x7d84, interrupt_enable_w );
	MW_ADD( 0x7d85, 0x7d85, MWA_RAM );
	MW_ADD( 0x7d86, 0x7d87, dkong_palettebank_w );
	return true;
}

private boolean ipt_dkong() {
	PORT_START();      /* IN0 */
	PORT_BIT( 0x01, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_4WAY );
	PORT_BIT( 0x02, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT  | IPF_4WAY );
	PORT_BIT( 0x04, IP_ACTIVE_HIGH, IPT_JOYSTICK_UP    | IPF_4WAY );
	PORT_BIT( 0x08, IP_ACTIVE_HIGH, IPT_JOYSTICK_DOWN  | IPF_4WAY );
	PORT_BIT( 0x10, IP_ACTIVE_HIGH, IPT_BUTTON1 );
	PORT_BIT( 0x20, IP_ACTIVE_HIGH, IPT_UNKNOWN );
	PORT_BIT( 0x40, IP_ACTIVE_HIGH, IPT_UNKNOWN );
	PORT_BIT( 0x80, IP_ACTIVE_HIGH, IPT_UNKNOWN );

	PORT_START();      /* IN1 */
	PORT_BIT( 0x01, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_4WAY | IPF_COCKTAIL );
	PORT_BIT( 0x02, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT  | IPF_4WAY | IPF_COCKTAIL );
	PORT_BIT( 0x04, IP_ACTIVE_HIGH, IPT_JOYSTICK_UP    | IPF_4WAY | IPF_COCKTAIL );
	PORT_BIT( 0x08, IP_ACTIVE_HIGH, IPT_JOYSTICK_DOWN  | IPF_4WAY | IPF_COCKTAIL );
	PORT_BIT( 0x10, IP_ACTIVE_HIGH, IPT_BUTTON1 | IPF_COCKTAIL );
	PORT_BIT( 0x20, IP_ACTIVE_HIGH, IPT_UNKNOWN );
	PORT_BIT( 0x40, IP_ACTIVE_HIGH, IPT_UNKNOWN );
	PORT_BIT( 0x80, IP_ACTIVE_HIGH, IPT_UNKNOWN );

	PORT_START();      /* IN2 */
	//PORT_BITX(0x01, IP_ACTIVE_HIGH, IPT_SERVICE, DEF_STR2( Service_Mode ), KEYCODE_F2, IP_JOY_NONE );
//	PORT_DIPNAME( 0x01, 0x00, DEF_STR2( Service_Mode ) );
//	PORT_DIPSETTING(    0x00, DEF_STR2( Off ) );
//	PORT_DIPSETTING(    0x01, DEF_STR2( On ) );
	PORT_BIT( 0x02, IP_ACTIVE_HIGH, IPT_UNKNOWN );
	PORT_BIT( 0x04, IP_ACTIVE_HIGH, IPT_START1 );
	PORT_BIT( 0x08, IP_ACTIVE_HIGH, IPT_START2 );
	PORT_BIT( 0x10, IP_ACTIVE_HIGH, IPT_UNKNOWN );
	PORT_BIT( 0x20, IP_ACTIVE_HIGH, IPT_UNKNOWN );
	PORT_BIT( 0x40, IP_ACTIVE_HIGH, IPT_SPECIAL );	/* status from sound cpu */
	PORT_BIT( 0x80, IP_ACTIVE_HIGH, IPT_COIN1 );

	PORT_START();     /* DSW0 */
	PORT_DIPNAME( 0x03, 0x00, DEF_STR2( Lives ) );
	PORT_DIPSETTING(    0x00, "3" );
	PORT_DIPSETTING(    0x01, "4" );
	PORT_DIPSETTING(    0x02, "5" );
	PORT_DIPSETTING(    0x03, "6" );
	PORT_DIPNAME( 0x0c, 0x00, DEF_STR2( Bonus_Life ) );
	PORT_DIPSETTING(    0x00, "7000" );
	PORT_DIPSETTING(    0x04, "10000" );
	PORT_DIPSETTING(    0x08, "15000" );
	PORT_DIPSETTING(    0x0c, "20000" );
	PORT_DIPNAME( 0x70, 0x00, DEF_STR2( Coinage ) );
	PORT_DIPSETTING(    0x70, DEF_STR2( _5C_1C ) );
	PORT_DIPSETTING(    0x50, DEF_STR2( _4C_1C ) );
	PORT_DIPSETTING(    0x30, DEF_STR2( _3C_1C ) );
	PORT_DIPSETTING(    0x10, DEF_STR2( _2C_1C ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( _1C_1C ) );
	PORT_DIPSETTING(    0x20, DEF_STR2( _1C_2C ) );
	PORT_DIPSETTING(    0x40, DEF_STR2( _1C_3C ) );
	PORT_DIPSETTING(    0x60, DEF_STR2( _1C_4C ) );
	PORT_DIPNAME( 0x80, 0x80, DEF_STR2( Cabinet ) );
	PORT_DIPSETTING(    0x80, DEF_STR2( Upright ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( Cocktail ) );
	return true;
}

private boolean ipt_dkong3() {
	PORT_START();      /* IN0 */
	PORT_BIT( 0x01, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_4WAY );
	PORT_BIT( 0x02, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT  | IPF_4WAY );
	PORT_BIT( 0x04, IP_ACTIVE_HIGH, IPT_JOYSTICK_UP    | IPF_4WAY );
	PORT_BIT( 0x08, IP_ACTIVE_HIGH, IPT_JOYSTICK_DOWN  | IPF_4WAY );
	PORT_BIT( 0x10, IP_ACTIVE_HIGH, IPT_BUTTON1 );
	PORT_BIT( 0x20, IP_ACTIVE_HIGH, IPT_START1 );
	PORT_BIT( 0x40, IP_ACTIVE_HIGH, IPT_START2 );
	PORT_BIT( 0x80, IP_ACTIVE_HIGH, IPT_COIN3 );


	PORT_START();      /* IN1 */
	PORT_BIT( 0x01, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_4WAY | IPF_COCKTAIL );
	PORT_BIT( 0x02, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT  | IPF_4WAY | IPF_COCKTAIL );
	PORT_BIT( 0x04, IP_ACTIVE_HIGH, IPT_JOYSTICK_UP    | IPF_4WAY | IPF_COCKTAIL );
	PORT_BIT( 0x08, IP_ACTIVE_HIGH, IPT_JOYSTICK_DOWN  | IPF_4WAY | IPF_COCKTAIL );
	PORT_BIT( 0x10, IP_ACTIVE_HIGH, IPT_BUTTON1 | IPF_COCKTAIL );
	PORT_BIT/*_IMPULSE*/( 0x20, IP_ACTIVE_HIGH, IPT_COIN1/*, 1*/ );
	PORT_BIT/*_IMPULSE*/( 0x40, IP_ACTIVE_HIGH, IPT_COIN2/*, 1*/ );
	PORT_BIT( 0x80, IP_ACTIVE_HIGH, IPT_UNKNOWN );

	PORT_START();      /* DSW0 */
	PORT_DIPNAME( 0x07, 0x00, DEF_STR2( Coinage ) );
	PORT_DIPSETTING(    0x02, DEF_STR2( _3C_1C ) );
	PORT_DIPSETTING(    0x04, DEF_STR2( _2C_1C ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( _1C_1C ) );
	PORT_DIPSETTING(    0x06, DEF_STR2( _1C_2C ) );
	PORT_DIPSETTING(    0x01, DEF_STR2( _1C_3C ) );
	PORT_DIPSETTING(    0x03, DEF_STR2( _1C_4C ) );
	PORT_DIPSETTING(    0x05, DEF_STR2( _1C_5C ) );
	PORT_DIPSETTING(    0x07, DEF_STR2( _1C_6C ) );
	PORT_DIPNAME( 0x08, 0x00, DEF_STR2( Unknown ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( Off ) );
	PORT_DIPSETTING(    0x08, DEF_STR2( On ) );
	PORT_DIPNAME( 0x10, 0x00, DEF_STR2( Unknown ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( Off ) );
	PORT_DIPSETTING(    0x10, DEF_STR2( On ) );
	PORT_DIPNAME( 0x20, 0x00, DEF_STR2( Unknown ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( Off ) );
	PORT_DIPSETTING(    0x20, DEF_STR2( On ) );
	//PORT_BITX(0x40, IP_ACTIVE_HIGH, IPT_SERVICE, DEF_STR2( Service_Mode ), KEYCODE_F2, IP_JOY_NONE );
	PORT_DIPNAME( 0x80, 0x00, DEF_STR2( Cabinet ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( Upright ) );
	PORT_DIPSETTING(    0x80, DEF_STR2( Cocktail ) );

	PORT_START();      /* DSW1 */
	PORT_DIPNAME( 0x03, 0x00, DEF_STR2( Lives ) );
	PORT_DIPSETTING(    0x00, "3" );
	PORT_DIPSETTING(    0x01, "4" );
	PORT_DIPSETTING(    0x02, "5" );
	PORT_DIPSETTING(    0x03, "6" );
	PORT_DIPNAME( 0x0c, 0x00, DEF_STR2( Bonus_Life ) );
	PORT_DIPSETTING(    0x00, "30000" );
	PORT_DIPSETTING(    0x04, "40000" );
	PORT_DIPSETTING(    0x08, "50000" );
	PORT_DIPSETTING(    0x0c, "None" );
	PORT_DIPNAME( 0x30, 0x00, "Additional Bonus" );
	PORT_DIPSETTING(    0x00, "30000" );
	PORT_DIPSETTING(    0x10, "40000" );
	PORT_DIPSETTING(    0x20, "50000" );
	PORT_DIPSETTING(    0x30, "None" );
	PORT_DIPNAME( 0xc0, 0x00, DEF_STR2( Difficulty ) );
	PORT_DIPSETTING(    0x00, "Easy" );
	PORT_DIPSETTING(    0x40, "Medium" );
	PORT_DIPSETTING(    0x80, "Hard" );
	PORT_DIPSETTING(    0xc0, "Hardest" );
	return true;
}


int[][] dkong_charlayout =
{
	{8},{8},	/* 8*8 characters */
	{256},	/* 256 characters */
	{2},	/* 2 bits per pixel */
	{ 0, 256*8*8 },	/* the two bitplanes are separated */
	{ 0, 1, 2, 3, 4, 5, 6, 7 },	/* pretty straightforward layout */
	{ 0*8, 1*8, 2*8, 3*8, 4*8, 5*8, 6*8, 7*8 },
	{8*8}	/* every char takes 8 consecutive bytes */
};
int[][]  dkongjr_charlayout =
{
	{8},{8},	/* 8*8 characters */
	{512},	/* 512 characters */
	{2},	/* 2 bits per pixel */
	{ 0, 512*8*8 },	/* the two bitplanes are separated */
	{ 0, 1, 2, 3, 4, 5, 6, 7 },	/* pretty straightforward layout */
	{ 0*8, 1*8, 2*8, 3*8, 4*8, 5*8, 6*8, 7*8 },
	{8*8}	/* every char takes 8 consecutive bytes */
};
int[][] dkong_spritelayout =
{
	{16},{16},	/* 16*16 sprites */
	{128},	/* 128 sprites */
	{2},	/* 2 bits per pixel */
	{ 0, 128*16*16 },	/* the two bitplanes are separated */
	{ 0, 1, 2, 3, 4, 5, 6, 7,	/* the two halves of the sprite are separated */
			64*16*16+0, 64*16*16+1, 64*16*16+2, 64*16*16+3, 64*16*16+4, 64*16*16+5, 64*16*16+6, 64*16*16+7 },
	{ 0*8, 1*8, 2*8, 3*8, 4*8, 5*8, 6*8, 7*8,
			8*8, 9*8, 10*8, 11*8, 12*8, 13*8, 14*8, 15*8 },
	{16*8}	/* every sprite takes 16 consecutive bytes */
};
int[][] dkong3_spritelayout =
{
	{16},{16},	/* 16*16 sprites */
	{256},	/* 256 sprites */
	{2},	/* 2 bits per pixel */
	{ 0, 256*16*16 },	/* the two bitplanes are separated */
	{ 0, 1, 2, 3, 4, 5, 6, 7,	/* the two halves of the sprite are separated */
			128*16*16+0, 128*16*16+1, 128*16*16+2, 128*16*16+3, 128*16*16+4, 128*16*16+5, 128*16*16+6, 128*16*16+7 },
	{ 0*8, 1*8, 2*8, 3*8, 4*8, 5*8, 6*8, 7*8,
			8*8, 9*8, 10*8, 11*8, 12*8, 13*8, 14*8, 15*8 },
	{16*8}	/* every sprite takes 16 consecutive bytes */
};

private boolean dkong_gfxdecodeinfo()
{
	GDI_ADD( REGION_GFX1, 0x0000, dkong_charlayout,   0, 64 );
	GDI_ADD( REGION_GFX2, 0x0000, dkong_spritelayout, 0, 64 );
	GDI_ADD( -1 ); /* end of array */
	return true;
};
private boolean dkongjr_gfxdecodeinfo()
{
	GDI_ADD( REGION_GFX1, 0x0000, dkongjr_charlayout,   0, 64 );
	GDI_ADD( REGION_GFX2, 0x0000, dkong_spritelayout,   0, 64 );
	GDI_ADD( -1 ); /* end of array */
	return true;
};
private boolean dkong3_gfxdecodeinfo()
{
	GDI_ADD( REGION_GFX1, 0x0000, dkongjr_charlayout,   0, 64 );
	GDI_ADD( REGION_GFX2, 0x0000, dkong3_spritelayout,  0, 64 );
	GDI_ADD( -1 ); /* end of array */
	return true;
};

public boolean mdrv_radarscp() {
	/* basic machine hardware */
	MDRV_CPU_ADD(Z80, 3072000);	/* 3.072 MHz (?) */
	MDRV_CPU_MEMORY(readmem(),radarscp_writemem());
	MDRV_CPU_VBLANK_INT(nmi_line_pulse,1);

	//MDRV_CPU_ADD(I8035,6000000/15)
	//MDRV_CPU_FLAGS(CPU_AUDIO_CPU)	/* 6MHz crystal */
	//MDRV_CPU_MEMORY(readmem_sound,writemem_sound)
	//MDRV_CPU_PORTS(readport_sound,writeport_sound)

	MDRV_FRAMES_PER_SECOND(60);
	MDRV_VBLANK_DURATION(DEFAULT_60HZ_VBLANK_DURATION);

	/* video hardware */
	MDRV_VIDEO_ATTRIBUTES(VIDEO_TYPE_RASTER);
	MDRV_SCREEN_SIZE(32*8, 32*8);
	MDRV_VISIBLE_AREA(0*8, 32*8-1, 2*8, 30*8-1);
	MDRV_GFXDECODE(dkong_gfxdecodeinfo());
	MDRV_PALETTE_LENGTH(256+2);
	MDRV_COLORTABLE_LENGTH(64*4);

	MDRV_PALETTE_INIT(dkong_pi);
	MDRV_VIDEO_START(dkong_vs);
	MDRV_VIDEO_UPDATE(radarscp_vu);

	/* sound hardware */
	//MDRV_SOUND_ADD(DAC, dkong_dac_interface)
	//MDRV_SOUND_ADD(SAMPLES, dkong_samples_interface)

	return true;
}

public boolean mdrv_dkong() {
	/* basic machine hardware */
	MDRV_CPU_ADD(Z80, 3072000);	/* 3.072 MHz (?) */
	MDRV_CPU_MEMORY(readmem(),dkong_writemem());
	MDRV_CPU_VBLANK_INT(nmi_line_pulse,1);

	//MDRV_CPU_ADD(I8035,6000000/15)
	//MDRV_CPU_FLAGS(CPU_AUDIO_CPU)	/* 6MHz crystal */
	//MDRV_CPU_MEMORY(readmem_sound,writemem_sound)
	//MDRV_CPU_PORTS(readport_sound,writeport_sound)

	MDRV_FRAMES_PER_SECOND(60);
	MDRV_VBLANK_DURATION(DEFAULT_60HZ_VBLANK_DURATION);

	/* video hardware */
	MDRV_VIDEO_ATTRIBUTES(VIDEO_TYPE_RASTER);
	MDRV_SCREEN_SIZE(32*8, 32*8);
	MDRV_VISIBLE_AREA(0*8, 32*8-1, 2*8, 30*8-1);
	MDRV_GFXDECODE(dkong_gfxdecodeinfo());
	MDRV_PALETTE_LENGTH(256);
	MDRV_COLORTABLE_LENGTH(64*4);

	MDRV_PALETTE_INIT(dkong_pi);
	MDRV_VIDEO_START(dkong_vs);
	MDRV_VIDEO_UPDATE(dkong_vu);

	/* sound hardware */
	//MDRV_SOUND_ADD(DAC, dkong_dac_interface)
	//MDRV_SOUND_ADD(SAMPLES, dkong_samples_interface)

	return true;
}

public boolean mdrv_dkongjr() {
	/* basic machine hardware */
	MDRV_CPU_ADD(Z80, 3072000);	/* 3.072 MHz (?) */
	MDRV_CPU_MEMORY(readmem(),dkongjr_writemem());
	MDRV_CPU_VBLANK_INT(nmi_line_pulse,1);

	//MDRV_CPU_ADD(I8035,6000000/15)
	//MDRV_CPU_FLAGS(CPU_AUDIO_CPU)	/* 6MHz crystal */
	//MDRV_CPU_MEMORY(readmem_sound,writemem_sound)
	//MDRV_CPU_PORTS(readport_sound,writeport_sound)

	MDRV_FRAMES_PER_SECOND(60);
	MDRV_VBLANK_DURATION(DEFAULT_60HZ_VBLANK_DURATION);

	/* video hardware */
	MDRV_VIDEO_ATTRIBUTES(VIDEO_TYPE_RASTER);
	MDRV_SCREEN_SIZE(32*8, 32*8);
	MDRV_VISIBLE_AREA(0*8, 32*8-1, 2*8, 30*8-1);
	MDRV_GFXDECODE(dkongjr_gfxdecodeinfo());
	MDRV_PALETTE_LENGTH(256);
	MDRV_COLORTABLE_LENGTH(64*4);

	MDRV_PALETTE_INIT(dkong_pi);
	MDRV_VIDEO_START(dkong_vs);
	MDRV_VIDEO_UPDATE(dkong_vu);

	/* sound hardware */
	//MDRV_SOUND_ADD(DAC, dkong_dac_interface)
	//MDRV_SOUND_ADD(SAMPLES, dkong_samples_interface)

	return true;
}

public boolean mdrv_dkong3() {
	/* basic machine hardware */
	MDRV_CPU_ADD(Z80,8000000/2);	/* 4 MHz */
	MDRV_CPU_MEMORY(dkong3_readmem(),dkong3_writemem());
	//MDRV_CPU_PORTS(0,dkong3_writeport);
	MDRV_CPU_VBLANK_INT(nmi_line_pulse,1);

	//MDRV_CPU_ADD(N2A03,N2A03_DEFAULTCLOCK);
	//MDRV_CPU_FLAGS(CPU_AUDIO_CPU);
	//MDRV_CPU_MEMORY(dkong3_sound1_readmem,dkong3_sound1_writemem);
	//MDRV_CPU_VBLANK_INT(nmi_line_pulse,1);

	//MDRV_CPU_ADD(N2A03,N2A03_DEFAULTCLOCK);
	//MDRV_CPU_FLAGS(CPU_AUDIO_CPU);
	//MDRV_CPU_MEMORY(dkong3_sound2_readmem,dkong3_sound2_writemem);
	//MDRV_CPU_VBLANK_INT(nmi_line_pulse,1);

	MDRV_FRAMES_PER_SECOND(60);
	MDRV_VBLANK_DURATION(DEFAULT_60HZ_VBLANK_DURATION);

	/* video hardware */
	MDRV_VIDEO_ATTRIBUTES(VIDEO_TYPE_RASTER);
	MDRV_SCREEN_SIZE(32*8, 32*8);
	MDRV_VISIBLE_AREA(0*8, 32*8-1, 2*8, 30*8-1);
	MDRV_GFXDECODE(dkong3_gfxdecodeinfo());
	MDRV_PALETTE_LENGTH(256);
	MDRV_COLORTABLE_LENGTH(64*4);

	MDRV_PALETTE_INIT(dkong3_pi);
	MDRV_VIDEO_START(dkong_vs);
	MDRV_VIDEO_UPDATE(dkong_vu);

	return true;
}

private boolean rom_dkong() {
	ROM_REGION( 0x10000, REGION_CPU1, 0 );	/* 64k for code */
	ROM_LOAD( "c_5et_g.bin",  0x0000, 0x1000, 0xba70b88b );
	ROM_LOAD( "c_5ct_g.bin",  0x1000, 0x1000, 0x5ec461ec );
	ROM_LOAD( "c_5bt_g.bin",  0x2000, 0x1000, 0x1c97d324 );
	ROM_LOAD( "c_5at_g.bin",  0x3000, 0x1000, 0xb9005ac0 );
	/* space for diagnostic ROM */

	//ROM_REGION( 0x1000, REGION_CPU2, 0 );	/* sound */
	//ROM_LOAD( "s_3i_b.bin",   0x0000, 0x0800, 0x45a4ed06 );
	//ROM_LOAD( "s_3j_b.bin",   0x0800, 0x0800, 0x4743fe92 );

	ROM_REGION( 0x1000, REGION_GFX1, ROMREGION_DISPOSE );
	ROM_LOAD( "v_5h_b.bin",   0x0000, 0x0800, 0x12c8c95d );
	ROM_LOAD( "v_3pt.bin",    0x0800, 0x0800, 0x15e9c5e9 );

	ROM_REGION( 0x2000, REGION_GFX2, ROMREGION_DISPOSE );
	ROM_LOAD( "l_4m_b.bin",   0x0000, 0x0800, 0x59f8054d );
	ROM_LOAD( "l_4n_b.bin",   0x0800, 0x0800, 0x672e4714 );
	ROM_LOAD( "l_4r_b.bin",   0x1000, 0x0800, 0xfeaa59ee );
	ROM_LOAD( "l_4s_b.bin",   0x1800, 0x0800, 0x20f2ef7e );

	ROM_REGION( 0x0300, REGION_PROMS, 0 );
	//ROM_LOAD( "c-2k.bpr",     0x0000, 0x0100, 0xe273ede5 ); /* palette low 4 bits (inverted) */
	//ROM_LOAD( "c-2j.bpr",     0x0100, 0x0100, 0xd6412358 ); /* palette high 4 bits (inverted) */
	//ROM_LOAD( "v-5e.bpr",     0x0200, 0x0100, 0xb869b8f5 ); /* character color codes on a per-column basis */
	ROM_LOAD( "Dkong.2k",     0x0000, 0x0100, 0x1e82d375 ); /* palette low 4 bits (inverted) */
	ROM_LOAD( "Dkong.2j",     0x0100, 0x0100, 0x2ab01dc8 ); /* palette high 4 bits (inverted) */
	ROM_LOAD( "Dkong.5f",     0x0200, 0x0100, 0x44988665 ); /* character color codes on a per-column basis */
	
/*********************************************************
I use more appropreate filenames for color PROMs.
	ROM_REGION( 0x0300, REGION_PROMS, 0 )
	ROM_LOAD( "dkong.2k",     0x0000, 0x0100, 0x1e82d375 )
	ROM_LOAD( "dkong.2j",     0x0100, 0x0100, 0x2ab01dc8 )
	ROM_LOAD( "dkong.5f",     0x0200, 0x0100, 0x44988665 )
*********************************************************/
	return true;
}

private boolean rom_dkongo() {
	ROM_REGION( 0x10000, REGION_CPU1, 0 );	/* 64k for code */
	ROM_LOAD( "c_5f_b.bin",   0x0000, 0x1000, 0x424f2b11 );	// tkg3c.5f
	ROM_LOAD( "c_5ct_g.bin",  0x1000, 0x1000, 0x5ec461ec );	// tkg3c.5g
	ROM_LOAD( "c_5h_b.bin",   0x2000, 0x1000, 0x1d28895d );	// tkg3c.5h
	ROM_LOAD( "tkg3c.5k",     0x3000, 0x1000, 0x553b89bb );
	/* space for diagnostic ROM */

	//ROM_REGION( 0x1000, REGION_CPU2, 0 );	/* sound */
	//ROM_LOAD( "s_3i_b.bin",   0x0000, 0x0800, 0x45a4ed06 );
	//ROM_LOAD( "s_3j_b.bin",   0x0800, 0x0800, 0x4743fe92 );

	ROM_REGION( 0x1000, REGION_GFX1, ROMREGION_DISPOSE );
	ROM_LOAD( "v_5h_b.bin",   0x0000, 0x0800, 0x12c8c95d );
	ROM_LOAD( "v_3pt.bin",    0x0800, 0x0800, 0x15e9c5e9 );

	ROM_REGION( 0x2000, REGION_GFX2, ROMREGION_DISPOSE );
	ROM_LOAD( "l_4m_b.bin",   0x0000, 0x0800, 0x59f8054d );
	ROM_LOAD( "l_4n_b.bin",   0x0800, 0x0800, 0x672e4714 );
	ROM_LOAD( "l_4r_b.bin",   0x1000, 0x0800, 0xfeaa59ee );
	ROM_LOAD( "l_4s_b.bin",   0x1800, 0x0800, 0x20f2ef7e );

	ROM_REGION( 0x0300, REGION_PROMS, 0 );
	ROM_LOAD( "c-2k.bpr",     0x0000, 0x0100, 0xe273ede5 ); /* palette low 4 bits (inverted) */
	ROM_LOAD( "c-2j.bpr",     0x0100, 0x0100, 0xd6412358 ); /* palette high 4 bits (inverted) */
	ROM_LOAD( "v-5e.bpr",     0x0200, 0x0100, 0xb869b8f5 ); /* character color codes on a per-column basis */

/*********************************************************
I use more appropreate filenames for color PROMs.
	ROM_REGION( 0x0300, REGION_PROMS, 0 )
	ROM_LOAD( "dkong.2k",     0x0000, 0x0100, 0x1e82d375 )
	ROM_LOAD( "dkong.2j",     0x0100, 0x0100, 0x2ab01dc8 )
	ROM_LOAD( "dkong.5f",     0x0200, 0x0100, 0x44988665 )
*********************************************************/
	return true;
}

private boolean rom_dkongjp() {
	ROM_REGION( 0x10000, REGION_CPU1, 0 );	/* 64k for code */
	ROM_LOAD( "c_5f_b.bin",   0x0000, 0x1000, 0x424f2b11 );
	ROM_LOAD( "5g.cpu",       0x1000, 0x1000, 0xd326599b );
	ROM_LOAD( "5h.cpu",       0x2000, 0x1000, 0xff31ac89 );
	ROM_LOAD( "c_5k_b.bin",   0x3000, 0x1000, 0x394d6007 );

	//ROM_REGION( 0x1000, REGION_CPU2, 0 );	/* sound */
	//ROM_LOAD( "s_3i_b.bin",   0x0000, 0x0800, 0x45a4ed06 );
	//ROM_LOAD( "s_3j_b.bin",   0x0800, 0x0800, 0x4743fe92 );

	ROM_REGION( 0x1000, REGION_GFX1, ROMREGION_DISPOSE );
	ROM_LOAD( "v_5h_b.bin",   0x0000, 0x0800, 0x12c8c95d );
	ROM_LOAD( "v_5k_b.bin",   0x0800, 0x0800, 0x3684f914 );

	ROM_REGION( 0x2000, REGION_GFX2, ROMREGION_DISPOSE );
	ROM_LOAD( "l_4m_b.bin",   0x0000, 0x0800, 0x59f8054d );
	ROM_LOAD( "l_4n_b.bin",   0x0800, 0x0800, 0x672e4714 );
	ROM_LOAD( "l_4r_b.bin",   0x1000, 0x0800, 0xfeaa59ee );
	ROM_LOAD( "l_4s_b.bin",   0x1800, 0x0800, 0x20f2ef7e );

	ROM_REGION( 0x0300, REGION_PROMS, 0 );
	ROM_LOAD( "c-2k.bpr",     0x0000, 0x0100, 0xe273ede5 ); /* palette low 4 bits (inverted) */
	ROM_LOAD( "c-2j.bpr",     0x0100, 0x0100, 0xd6412358 ); /* palette high 4 bits (inverted) */
	ROM_LOAD( "v-5e.bpr",     0x0200, 0x0100, 0xb869b8f5 ); /* character color codes on a per-column basis */

/*********************************************************
I use more appropreate filenames for color PROMs.
	ROM_REGION( 0x0300, REGION_PROMS, 0 )
	ROM_LOAD( "dkong.2k",     0x0000, 0x0100, 0x1e82d375 )
	ROM_LOAD( "dkong.2j",     0x0100, 0x0100, 0x2ab01dc8 )
	ROM_LOAD( "dkong.5f",     0x0200, 0x0100, 0x44988665 )
*********************************************************/
	return true;
}

private boolean rom_dkongjo() {
	ROM_REGION( 0x10000, REGION_CPU1, 0 );	/* 64k for code */
	ROM_LOAD( "c_5f_b.bin",   0x0000, 0x1000, 0x424f2b11 );
	ROM_LOAD( "c_5g_b.bin",   0x1000, 0x1000, 0x3b2a6635 );
	ROM_LOAD( "c_5h_b.bin",   0x2000, 0x1000, 0x1d28895d );
	ROM_LOAD( "c_5k_b.bin",   0x3000, 0x1000, 0x394d6007 );

	//ROM_REGION( 0x1000, REGION_CPU2, 0 );	/* sound */
	//ROM_LOAD( "s_3i_b.bin",   0x0000, 0x0800, 0x45a4ed06 );
	//ROM_LOAD( "s_3j_b.bin",   0x0800, 0x0800, 0x4743fe92 );

	ROM_REGION( 0x1000, REGION_GFX1, ROMREGION_DISPOSE );
	ROM_LOAD( "v_5h_b.bin",   0x0000, 0x0800, 0x12c8c95d );
	ROM_LOAD( "v_5k_b.bin",   0x0800, 0x0800, 0x3684f914 );

	ROM_REGION( 0x2000, REGION_GFX2, ROMREGION_DISPOSE );
	ROM_LOAD( "l_4m_b.bin",   0x0000, 0x0800, 0x59f8054d );
	ROM_LOAD( "l_4n_b.bin",   0x0800, 0x0800, 0x672e4714 );
	ROM_LOAD( "l_4r_b.bin",   0x1000, 0x0800, 0xfeaa59ee );
	ROM_LOAD( "l_4s_b.bin",   0x1800, 0x0800, 0x20f2ef7e );

	ROM_REGION( 0x0300, REGION_PROMS, 0 );
	ROM_LOAD( "c-2k.bpr",     0x0000, 0x0100, 0xe273ede5 ); /* palette low 4 bits (inverted) */
	ROM_LOAD( "c-2j.bpr",     0x0100, 0x0100, 0xd6412358 ); /* palette high 4 bits (inverted) */
	ROM_LOAD( "v-5e.bpr",     0x0200, 0x0100, 0xb869b8f5 ); /* character color codes on a per-column basis */

/*********************************************************
I use more appropreate filenames for color PROMs.
	ROM_REGION( 0x0300, REGION_PROMS, 0 )
	ROM_LOAD( "dkong.2k",     0x0000, 0x0100, 0x1e82d375 )
	ROM_LOAD( "dkong.2j",     0x0100, 0x0100, 0x2ab01dc8 )
	ROM_LOAD( "dkong.5f",     0x0200, 0x0100, 0x44988665 )
*********************************************************/
	return true;
}

private boolean rom_dkongjo1() {
	ROM_REGION( 0x10000, REGION_CPU1, 0 );	/* 64k for code */
	ROM_LOAD( "c_5f_b.bin",   0x0000, 0x1000, 0x424f2b11 );
	ROM_LOAD( "5g.cpu",       0x1000, 0x1000, 0xd326599b );
	ROM_LOAD( "c_5h_b.bin",   0x2000, 0x1000, 0x1d28895d );
	ROM_LOAD( "5k.bin",       0x3000, 0x1000, 0x7961599c );

	//ROM_REGION( 0x1000, REGION_CPU2, 0 );	/* sound */
	//ROM_LOAD( "s_3i_b.bin",   0x0000, 0x0800, 0x45a4ed06 );
	//ROM_LOAD( "s_3j_b.bin",   0x0800, 0x0800, 0x4743fe92 );

	ROM_REGION( 0x1000, REGION_GFX1, ROMREGION_DISPOSE );
	ROM_LOAD( "v_5h_b.bin",   0x0000, 0x0800, 0x12c8c95d );
	ROM_LOAD( "v_5k_b.bin",   0x0800, 0x0800, 0x3684f914 );

	ROM_REGION( 0x2000, REGION_GFX2, ROMREGION_DISPOSE );
	ROM_LOAD( "l_4m_b.bin",   0x0000, 0x0800, 0x59f8054d );
	ROM_LOAD( "l_4n_b.bin",   0x0800, 0x0800, 0x672e4714 );
	ROM_LOAD( "l_4r_b.bin",   0x1000, 0x0800, 0xfeaa59ee );
	ROM_LOAD( "l_4s_b.bin",   0x1800, 0x0800, 0x20f2ef7e );

	ROM_REGION( 0x0300, REGION_PROMS, 0 );
	ROM_LOAD( "c-2k.bpr",     0x0000, 0x0100, 0xe273ede5 ); /* palette low 4 bits (inverted) */
	ROM_LOAD( "c-2j.bpr",     0x0100, 0x0100, 0xd6412358 ); /* palette high 4 bits (inverted) */
	ROM_LOAD( "v-5e.bpr",     0x0200, 0x0100, 0xb869b8f5 ); /* character color codes on a per-column basis */

/*********************************************************
I use more appropreate filenames for color PROMs.
	ROM_REGION( 0x0300, REGION_PROMS, 0 )
	ROM_LOAD( "dkong.2k",     0x0000, 0x0100, 0x1e82d375 )
	ROM_LOAD( "dkong.2j",     0x0100, 0x0100, 0x2ab01dc8 )
	ROM_LOAD( "dkong.5f",     0x0200, 0x0100, 0x44988665 )
*********************************************************/
	return true;
}

private boolean rom_dkongjr() {
	ROM_REGION( 0x10000, REGION_CPU1, 0 );	/* 64k for code */
	ROM_LOAD( "dkj.5b",       0x0000, 0x1000, 0xdea28158 );
	ROM_CONTINUE(             0x3000, 0x1000 );
	ROM_LOAD( "dkj.5c",       0x2000, 0x0800, 0x6fb5faf6 );
	ROM_CONTINUE(             0x4800, 0x0800 );
	ROM_CONTINUE(             0x1000, 0x0800 );
	ROM_CONTINUE(             0x5800, 0x0800 );
	ROM_LOAD( "dkj.5e",       0x4000, 0x0800, 0xd042b6a8 );
	ROM_CONTINUE(             0x2800, 0x0800 );
	ROM_CONTINUE(             0x5000, 0x0800 );
	ROM_CONTINUE(             0x1800, 0x0800 );

	//ROM_REGION( 0x1000, REGION_CPU2, 0 );	/* sound */
	//ROM_LOAD( "c_3h.bin",       0x0000, 0x1000, 0x715da5f8 );

	ROM_REGION( 0x2000, REGION_GFX1, ROMREGION_DISPOSE );
	ROM_LOAD( "dkj.3n",       0x0000, 0x1000, 0x8d51aca9 );
	ROM_LOAD( "dkj.3p",       0x1000, 0x1000, 0x4ef64ba5 );

	ROM_REGION( 0x2000, REGION_GFX2, ROMREGION_DISPOSE );
	ROM_LOAD( "v_7c.bin",     0x0000, 0x0800, 0xdc7f4164 );
	ROM_LOAD( "v_7d.bin",     0x0800, 0x0800, 0x0ce7dcf6 );
	ROM_LOAD( "v_7e.bin",     0x1000, 0x0800, 0x24d1ff17 );
	ROM_LOAD( "v_7f.bin",     0x1800, 0x0800, 0x0f8c083f );

	ROM_REGION( 0x0300, REGION_PROMS, 0 );
	ROM_LOAD( "c-2e.bpr",  0x0000, 0x0100, 0x463dc7ad );	/* palette low 4 bits (inverted) */
	ROM_LOAD( "c-2f.bpr",  0x0100, 0x0100, 0x47ba0042 );	/* palette high 4 bits (inverted) */
	ROM_LOAD( "v-2n.bpr",  0x0200, 0x0100, 0xdbf185bf );	/* character color codes on a per-column basis */
	return true;
}

private boolean rom_dkongjrj() {
	ROM_REGION( 0x10000, REGION_CPU1, 0 );	/* 64k for code */
	ROM_LOAD( "c_5ba.bin",    0x0000, 0x1000, 0x50a015ce );
	ROM_CONTINUE(             0x3000, 0x1000 );
	ROM_LOAD( "c_5ca.bin",    0x2000, 0x0800, 0xc0a18f0d );
	ROM_CONTINUE(             0x4800, 0x0800 );
	ROM_CONTINUE(             0x1000, 0x0800 );
	ROM_CONTINUE(             0x5800, 0x0800 );
	ROM_LOAD( "c_5ea.bin",    0x4000, 0x0800, 0xa81dd00c );
	ROM_CONTINUE(             0x2800, 0x0800 );
	ROM_CONTINUE(             0x5000, 0x0800 );
	ROM_CONTINUE(             0x1800, 0x0800 );

	//ROM_REGION( 0x1000, REGION_CPU2, 0 );	/* sound */
	//ROM_LOAD( "c_3h.bin",     0x0000, 0x1000, 0x715da5f8 );

	ROM_REGION( 0x2000, REGION_GFX1, ROMREGION_DISPOSE );
	ROM_LOAD( "v_3na.bin",    0x0000, 0x1000, 0xa95c4c63 );
	ROM_LOAD( "v_3pa.bin",    0x1000, 0x1000, 0x4974ffef );

	ROM_REGION( 0x2000, REGION_GFX2, ROMREGION_DISPOSE );
	ROM_LOAD( "v_7c.bin",     0x0000, 0x0800, 0xdc7f4164 );
	ROM_LOAD( "v_7d.bin",     0x0800, 0x0800, 0x0ce7dcf6 );
	ROM_LOAD( "v_7e.bin",     0x1000, 0x0800, 0x24d1ff17 );
	ROM_LOAD( "v_7f.bin",     0x1800, 0x0800, 0x0f8c083f );

	ROM_REGION( 0x0300, REGION_PROMS, 0 );
	ROM_LOAD( "c-2e.bpr",  0x0000, 0x0100, 0x463dc7ad );	/* palette low 4 bits (inverted) */
	ROM_LOAD( "c-2f.bpr",  0x0100, 0x0100, 0x47ba0042 );	/* palette high 4 bits (inverted) */
	ROM_LOAD( "v-2n.bpr",  0x0200, 0x0100, 0xdbf185bf );	/* character color codes on a per-column basis */
	return true;
}

private boolean rom_dkngjnrj() {
	ROM_REGION( 0x10000, REGION_CPU1, 0 );	/* 64k for code */
	ROM_LOAD( "dkjp.5b",      0x0000, 0x1000, 0x7b48870b );
	ROM_CONTINUE(             0x3000, 0x1000 );
	ROM_LOAD( "dkjp.5c",      0x2000, 0x0800, 0x12391665 );
	ROM_CONTINUE(             0x4800, 0x0800 );
	ROM_CONTINUE(             0x1000, 0x0800 );
	ROM_CONTINUE(             0x5800, 0x0800 );
	ROM_LOAD( "dkjp.5e",      0x4000, 0x0800, 0x6c9f9103 );
	ROM_CONTINUE(             0x2800, 0x0800 );
	ROM_CONTINUE(             0x5000, 0x0800 );
	ROM_CONTINUE(             0x1800, 0x0800 );

	//ROM_REGION( 0x1000, REGION_CPU2, 0 );	/* sound */
	//ROM_LOAD( "c_3h.bin",       0x0000, 0x1000, 0x715da5f8 );

	ROM_REGION( 0x2000, REGION_GFX1, ROMREGION_DISPOSE );
	ROM_LOAD( "dkj.3n",       0x0000, 0x1000, 0x8d51aca9 );
	ROM_LOAD( "dkj.3p",       0x1000, 0x1000, 0x4ef64ba5 );

	ROM_REGION( 0x2000, REGION_GFX2, ROMREGION_DISPOSE );
	ROM_LOAD( "v_7c.bin",     0x0000, 0x0800, 0xdc7f4164 );
	ROM_LOAD( "v_7d.bin",     0x0800, 0x0800, 0x0ce7dcf6 );
	ROM_LOAD( "v_7e.bin",     0x1000, 0x0800, 0x24d1ff17 );
	ROM_LOAD( "v_7f.bin",     0x1800, 0x0800, 0x0f8c083f );

	ROM_REGION( 0x0300, REGION_PROMS, 0 );
	ROM_LOAD( "c-2e.bpr",  0x0000, 0x0100, 0x463dc7ad );	/* palette low 4 bits (inverted) */
	ROM_LOAD( "c-2f.bpr",  0x0100, 0x0100, 0x47ba0042 );	/* palette high 4 bits (inverted) */
	ROM_LOAD( "v-2n.bpr",  0x0200, 0x0100, 0xdbf185bf );	/* character color codes on a per-column basis */
	return true;
}

private boolean rom_dkongjrb() {
	ROM_REGION( 0x10000, REGION_CPU1, 0 );	/* 64k for code */
	ROM_LOAD( "dkjr1",        0x0000, 0x1000, 0xec7e097f );
	ROM_CONTINUE(             0x3000, 0x1000 );
	ROM_LOAD( "c_5ca.bin",    0x2000, 0x0800, 0xc0a18f0d );
	ROM_CONTINUE(             0x4800, 0x0800 );
	ROM_CONTINUE(             0x1000, 0x0800 );
	ROM_CONTINUE(             0x5800, 0x0800 );
	ROM_LOAD( "c_5ea.bin",    0x4000, 0x0800, 0xa81dd00c );
	ROM_CONTINUE(             0x2800, 0x0800 );
	ROM_CONTINUE(             0x5000, 0x0800 );
	ROM_CONTINUE(             0x1800, 0x0800 );

	//ROM_REGION( 0x1000, REGION_CPU2, 0 );	/* sound */
	//ROM_LOAD( "c_3h.bin",       0x0000, 0x1000, 0x715da5f8 );

	ROM_REGION( 0x2000, REGION_GFX1, ROMREGION_DISPOSE );
	ROM_LOAD( "v_3na.bin",    0x0000, 0x1000, 0xa95c4c63 );
	ROM_LOAD( "dkjr10",       0x1000, 0x1000, 0xadc11322 );

	ROM_REGION( 0x2000, REGION_GFX2, ROMREGION_DISPOSE );
	ROM_LOAD( "v_7c.bin",     0x0000, 0x0800, 0xdc7f4164 );
	ROM_LOAD( "v_7d.bin",     0x0800, 0x0800, 0x0ce7dcf6 );
	ROM_LOAD( "v_7e.bin",     0x1000, 0x0800, 0x24d1ff17 );
	ROM_LOAD( "v_7f.bin",     0x1800, 0x0800, 0x0f8c083f );

	ROM_REGION( 0x0300, REGION_PROMS, 0 );
	ROM_LOAD( "c-2e.bpr",  0x0000, 0x0100, 0x463dc7ad );	/* palette low 4 bits (inverted) */
	ROM_LOAD( "c-2f.bpr",  0x0100, 0x0100, 0x47ba0042 );	/* palette high 4 bits (inverted) */
	ROM_LOAD( "v-2n.bpr",  0x0200, 0x0100, 0xdbf185bf );	/* character color codes on a per-column basis */
	return true;
}

private boolean rom_dkngjnrb() {
	ROM_REGION( 0x10000, REGION_CPU1, 0 );	/* 64k for code */
	ROM_LOAD( "djr1-c.5b",    0x0000, 0x1000, 0xffe9e1a5 );
	ROM_CONTINUE(             0x3000, 0x1000 );
	ROM_LOAD( "djr1-c.5c",    0x2000, 0x0800, 0x982e30e8 );
	ROM_CONTINUE(             0x4800, 0x0800 );
	ROM_CONTINUE(             0x1000, 0x0800 );
	ROM_CONTINUE(             0x5800, 0x0800 );
	ROM_LOAD( "djr1-c.5e",    0x4000, 0x0800, 0x24c3d325 );
	ROM_CONTINUE(             0x2800, 0x0800 );
	ROM_CONTINUE(             0x5000, 0x0800 );
	ROM_CONTINUE(             0x1800, 0x0800 );
	ROM_LOAD( "djr1-c.5a",    0x8000, 0x1000, 0xbb5f5180 );

	//ROM_REGION( 0x1000, REGION_CPU2, 0 );	/* sound */
	//ROM_LOAD( "c_3h.bin",       0x0000, 0x1000, 0x715da5f8 );

	ROM_REGION( 0x2000, REGION_GFX1, ROMREGION_DISPOSE );
	ROM_LOAD( "dkj.3n",       0x0000, 0x1000, 0x8d51aca9 );
	ROM_LOAD( "dkj.3p",       0x1000, 0x1000, 0x4ef64ba5 );

	ROM_REGION( 0x2000, REGION_GFX2, ROMREGION_DISPOSE );
	ROM_LOAD( "v_7c.bin",     0x0000, 0x0800, 0xdc7f4164 );
	ROM_LOAD( "v_7d.bin",     0x0800, 0x0800, 0x0ce7dcf6 );
	ROM_LOAD( "v_7e.bin",     0x1000, 0x0800, 0x24d1ff17 );
	ROM_LOAD( "v_7f.bin",     0x1800, 0x0800, 0x0f8c083f );

	ROM_REGION( 0x0300, REGION_PROMS, 0 );
	ROM_LOAD( "c-2e.bpr",  0x0000, 0x0100, 0x463dc7ad );	/* palette low 4 bits (inverted) */
	ROM_LOAD( "c-2f.bpr",  0x0100, 0x0100, 0x47ba0042 );	/* palette high 4 bits (inverted) */
	ROM_LOAD( "v-2n.bpr",  0x0200, 0x0100, 0xdbf185bf );	/* character color codes on a per-column basis */
	return true;
}

private boolean rom_dkong3() {
	ROM_REGION( 0x10000, REGION_CPU1, 0 );	/* 64k for code */
	ROM_LOAD( "dk3c.7b",      0x0000, 0x2000, 0x38d5f38e );
	ROM_LOAD( "dk3c.7c",      0x2000, 0x2000, 0xc9134379 );
	ROM_LOAD( "dk3c.7d",      0x4000, 0x2000, 0xd22e2921 );
	ROM_LOAD( "dk3c.7e",      0x8000, 0x2000, 0x615f14b7 );

	//ROM_REGION( 0x10000, REGION_CPU2, 0 );	/* sound #1 */
	//ROM_LOAD( "dk3c.5l",      0xe000, 0x2000, 0x7ff88885 );

	//ROM_REGION( 0x10000, REGION_CPU3, 0 );	/* sound #2 */
	//ROM_LOAD( "dk3c.6h",      0xe000, 0x2000, 0x36d7200c );

	ROM_REGION( 0x2000, REGION_GFX1, ROMREGION_DISPOSE );
	ROM_LOAD( "dk3v.3n",      0x0000, 0x1000, 0x415a99c7 );
	ROM_LOAD( "dk3v.3p",      0x1000, 0x1000, 0x25744ea0 );

	ROM_REGION( 0x4000, REGION_GFX2, ROMREGION_DISPOSE );
	ROM_LOAD( "dk3v.7c",      0x0000, 0x1000, 0x8ffa1737 );
	ROM_LOAD( "dk3v.7d",      0x1000, 0x1000, 0x9ac84686 );
	ROM_LOAD( "dk3v.7e",      0x2000, 0x1000, 0x0c0af3fb );
	ROM_LOAD( "dk3v.7f",      0x3000, 0x1000, 0x55c58662 );

	ROM_REGION( 0x0300, REGION_PROMS, 0 );
	ROM_LOAD( "dkc1-c.1d",    0x0000, 0x0200, 0xdf54befc ); /* palette red & green component */
	ROM_LOAD( "dkc1-c.1c",    0x0100, 0x0200, 0x66a77f40 ); /* palette blue component */
	ROM_LOAD( "dkc1-v.2n",    0x0200, 0x0100, 0x50e33434 );	/* character color codes on a per-column basis */
	return true;
}

private boolean rom_dkong3j() {
	ROM_REGION( 0x10000, REGION_CPU1, 0 );	/* 64k for code */
	ROM_LOAD( "dk3c.7b",      0x0000, 0x2000, 0x38d5f38e );
	ROM_LOAD( "dk3c.7c",      0x2000, 0x2000, 0xc9134379 );
	ROM_LOAD( "dk3c.7d",      0x4000, 0x2000, 0xd22e2921 );
	ROM_LOAD( "dk3cj.7e",     0x8000, 0x2000, 0x25b5be23 );

	//ROM_REGION( 0x10000, REGION_CPU2, 0 );	/* sound #1 */
	//ROM_LOAD( "dk3c.5l",      0xe000, 0x2000, 0x7ff88885 );

	//ROM_REGION( 0x10000, REGION_CPU3, 0 );	/* sound #2 */
	//ROM_LOAD( "dk3c.6h",      0xe000, 0x2000, 0x36d7200c );

	ROM_REGION( 0x2000, REGION_GFX1, ROMREGION_DISPOSE );
	ROM_LOAD( "dk3v.3n",      0x0000, 0x1000, 0x415a99c7 );
	ROM_LOAD( "dk3v.3p",      0x1000, 0x1000, 0x25744ea0 );

	ROM_REGION( 0x4000, REGION_GFX2, ROMREGION_DISPOSE );
	ROM_LOAD( "dk3v.7c",      0x0000, 0x1000, 0x8ffa1737 );
	ROM_LOAD( "dk3v.7d",      0x1000, 0x1000, 0x9ac84686 );
	ROM_LOAD( "dk3v.7e",      0x2000, 0x1000, 0x0c0af3fb );
	ROM_LOAD( "dk3v.7f",      0x3000, 0x1000, 0x55c58662 );

	ROM_REGION( 0x0300, REGION_PROMS, 0 );
	ROM_LOAD( "dkc1-c.1d",    0x0000, 0x0200, 0xdf54befc ); /* palette red & green component */
	ROM_LOAD( "dkc1-c.1c",    0x0100, 0x0200, 0x66a77f40 ); /* palette blue component */
	ROM_LOAD( "dkc1-v.2n",    0x0200, 0x0100, 0x50e33434 );	/* character color codes on a per-column basis */
	return true;
}

private boolean rom_radarscp() {
	ROM_REGION( 0x10000, REGION_CPU1, 0 );	/* 64k for code */
	ROM_LOAD( "trs2c5fc",     0x0000, 0x1000, 0x40949e0d );
	ROM_LOAD( "trs2c5gc",     0x1000, 0x1000, 0xafa8c49f );
	ROM_LOAD( "trs2c5hc",     0x2000, 0x1000, 0x51b8263d );
	ROM_LOAD( "trs2c5kc",     0x3000, 0x1000, 0x1f0101f7 );
	/* space for diagnostic ROM */

	//ROM_REGION( 0x1000, REGION_CPU2, 0 );	/* sound */
	//ROM_LOAD( "trs2s3i",      0x0000, 0x0800, 0x78034f14 );
	/* socket 3J is empty */

	ROM_REGION( 0x1000, REGION_GFX1, ROMREGION_DISPOSE );
	ROM_LOAD( "trs2v3gc",     0x0000, 0x0800, 0xf095330e );
	ROM_LOAD( "trs2v3hc",     0x0800, 0x0800, 0x15a316f0 );

	ROM_REGION( 0x2000, REGION_GFX2, ROMREGION_DISPOSE );
	ROM_LOAD( "trs2v3dc",     0x0000, 0x0800, 0xe0bb0db9 );
	ROM_LOAD( "trs2v3cc",     0x0800, 0x0800, 0x6c4e7dad );
	ROM_LOAD( "trs2v3bc",     0x1000, 0x0800, 0x6fdd63f1 );
	ROM_LOAD( "trs2v3ac",     0x1800, 0x0800, 0xbbf62755 );

	ROM_REGION( 0x0800, REGION_GFX3, 0 );	/* radar/star timing table */
	ROM_LOAD( "trs2v3ec",     0x0000, 0x0800, 0x0eca8d6b );

	ROM_REGION( 0x0300, REGION_PROMS, 0 );
	ROM_LOAD( "rs2-x.xxx",    0x0000, 0x0100, 0x54609d61 ); /* palette low 4 bits (inverted); */
	ROM_LOAD( "rs2-c.xxx",    0x0100, 0x0100, 0x79a7d831 ); /* palette high 4 bits (inverted); */
	ROM_LOAD( "rs2-v.1hc",    0x0200, 0x0100, 0x1b828315 ); /* character color codes on a per-column basis */
	return true;
}

public InitHandler init_radarscp() { return new Init_radarscp(); }
public class Init_radarscp implements InitHandler {
	public void init() {
		int[] RAM = memory_region(REGION_CPU1);

		/* TODO: Radarscope does a check on bit 6 of 7d00 which prevent it from working. */
		/* It's a sound status flag, maybe signaling when a tune is finished. */
		/* For now, we comment it out. */
		RAM[0x1e9c] = 0xc3;
		RAM[0x1e9d] = 0xbd;
	}
}

	public Machine getMachine(URL url, String name) {
		super.getMachine(url,name);
		super.setVideoEmulator(v);

		if (name.equals("dkong")) {
			GAME( 1981, rom_dkong(),	     0, mdrv_dkong(), 	 ipt_dkong(),               0, ROT90, "Nintendo of America", "Donkey Kong (US set 1)" );
		} else if (name.equals("dkongo")) {
			GAME( 1981, rom_dkongo(),    "dkong", mdrv_dkong(),    ipt_dkong(),               0, ROT90, "Nintendo", "Donkey Kong (US set 2)" );
		} else if (name.equals("dkongjp")) {
			GAME( 1981, rom_dkongjp(),   "dkong", mdrv_dkong(),    ipt_dkong(),               0, ROT90, "Nintendo", "Donkey Kong (Japan set 1)" );
		} else if (name.equals("dkongjo")) {
			GAME( 1981, rom_dkongjo(),   "dkong", mdrv_dkong(),    ipt_dkong(),               0, ROT90, "Nintendo", "Donkey Kong (Japan set 2)" );
		} else if (name.equals("dkongjo1")) {
			GAME( 1981, rom_dkongjo1(),  "dkong", mdrv_dkong(),    ipt_dkong(),               0, ROT90, "Nintendo", "Donkey Kong (Japan set 3) (bad dump?)" );

		} else if (name.equals("dkongjr")) {
			GAME( 1982, rom_dkongjr(),         0, mdrv_dkongjr(),  ipt_dkong(),               0, ROT90, "Nintendo of America", "Donkey Kong Junior (US)" );
		} else if (name.equals("dkongjrj")) {
			GAME( 1982, rom_dkongjrj(),"dkongjr", mdrv_dkongjr(),  ipt_dkong(),               0, ROT90, "Nintendo", "Donkey Kong Jr. (Japan)" );
		} else if (name.equals("dkngjnrj")) {
			GAME( 1982, rom_dkngjnrj(),"dkongjr", mdrv_dkongjr(),  ipt_dkong(),               0, ROT90, "Nintendo", "Donkey Kong Junior (Japan?)" );
		} else if (name.equals("dkongjrb")) {
			GAME( 1982, rom_dkongjrb(),"dkongjr", mdrv_dkongjr(),  ipt_dkong(),               0, ROT90, "bootleg", "Donkey Kong Jr. (bootleg)" );
		} else if (name.equals("dkngjnrb")) {
			GAME( 1982, rom_dkngjnrb(),"dkongjr", mdrv_dkongjr(),  ipt_dkong(),               0, ROT90, "Nintendo of America", "Donkey Kong Junior (bootleg?)" );

		} else if (name.equals("dkong3")) {
			GAME( 1983, rom_dkong3(),          0, mdrv_dkong3(),   ipt_dkong3(),              0, ROT90, "Nintendo of America", "Donkey Kong 3 (US)" );
		} else if (name.equals("dkong3j")) {
			GAME( 1983, rom_dkong3j(),  "dkong3", mdrv_dkong3(),   ipt_dkong3(),              0, ROT90, "Nintendo", "Donkey Kong 3 (Japan)" );

		} else if (name.equals("radarscp")) {
			GAME( 1980, rom_radarscp(),        0, mdrv_radarscp(), ipt_dkong(), init_radarscp(), ROT90, "Nintendo", "Radar Scope" );
		}

		m.init(md);


		
		return (Machine)m;
	}

}
