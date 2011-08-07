/*
CottAGE - the Arcade Generic Emulator in Java

Java driver by Erik Duijs, Gollum
*/

/***************************************************************************

Green Beret memory map (preliminary)

gberetb is a bootleg hacked to run on different hardware.

driver by Nicola Salmoria


0000-bfff ROM
c000-c7ff Color RAM
c800-cfff Video RAM
d000-d0c0 Sprites (bank 0)
d100-d1c0 Sprites (bank 1)
d200-dfff RAM
e000-e01f ZRAM1 line scroll registers
e020-e03f ZRAM2 bit 8 of line scroll registers

read:
f200      DSW1
          bit 0-1 lives
          bit 2   cocktail/upright cabinet (0 = upright)
          bit 3-4 bonus
          bit 5-6 difficulty
          bit 7   demo sounds
f400      DSW2
          bit 0 = screen flip
          bit 1 = single/dual upright controls
f600      DSW0
          bit 0-1-2-3 coins per play Coin1
          bit 4-5-6-7 coins per play Coin2
f601      IN1 player 2 controls
f602      IN0 player 1 controls
f603      IN2
          bit 0-1-2 coin  bit 3 1 player start  bit 4 2 players start

write:
e040      ?
e041      ?
e042      ?
e043      bit 3 = sprite RAM bank select; other bits = ?
e044      bit 0 = nmi enable, bit 3 = flip screen, other bits = ?
f000      ?
f200      SN76496 command
f400      SN76496 trigger (write command to f200, then write to this location
          to cause the chip to read it)
f600      watchdog reset (?)

interrupts:
The game uses both IRQ (mode 1) and NMI.


TODO:
gberetb:
- cocktail mode
mrgoemon:
- flickering rogue sprites
- it resets during the first boot sequence, but works afterwards

***************************************************************************/
package cottage.drivers;

import java.net.URL;

import jef.machine.Machine;
import jef.map.InterruptHandler;
import jef.map.WriteHandler;
import jef.sound.SoundChipEmulator;
import jef.sound.chip.SN76496;
import jef.video.Vh_convert_color_proms;
import jef.video.Vh_refresh;
import jef.video.Vh_start;

import cottage.mame.Driver;
import cottage.mame.MAMEConstants;
import cottage.mame.MAMEDriver;


public class Gberet extends MAMEDriver implements Driver, MAMEConstants {

cottage.vidhrdw.Gberet v = new cottage.vidhrdw.Gberet();

SN76496 sn = new SN76496(18432000/12);

WriteHandler videoram_w	= v.videoram_w();
WriteHandler colorram_w	= v.colorram_w();
Vh_start gberet_vs = (Vh_start)v;
Vh_refresh gberet_vu = (Vh_refresh)v;
Vh_convert_color_proms gberet_pi = (Vh_convert_color_proms)v;

cottage.machine.Gberet m = new cottage.machine.Gberet();
InterruptHandler gberet_interrupt = m.gberet_interrupt();
WriteHandler gberet_e044_w = m.gberet_e044_w();
//WriteHandler sound_command_w = (WriteHandler) new Gberet_snd_w();
//WriteHandler SN76496_0_w = (WriteHandler) new SN76496_write();
WriteHandler SN76496_0_w = sn.sn76496_command_w();

WriteHandler mrgoemon_bankswitch_w = new Mrgoemon_bankswitch_w();

int sound_command = 0;

private boolean readmem() {
	MR_START();
	MR_ADD( 0x0000, 0xffff, MRA_RAM ); /* CottAGE fixes - Unmapped memory */
	MR_ADD( 0x0000, 0xbfff, MRA_ROM );
	MR_ADD( 0xc000, 0xe03f, MRA_RAM );
	MR_ADD( 0xf200, 0xf200, input_port_4_r );	/* DSW1 */
	MR_ADD( 0xf400, 0xf400, input_port_5_r );	/* DSW2 */
	MR_ADD( 0xf600, 0xf600, input_port_3_r );	/* DSW0 */
	MR_ADD( 0xf601, 0xf601, input_port_1_r );	/* IN1 */
	MR_ADD( 0xf602, 0xf602, input_port_0_r );	/* IN0 */
	MR_ADD( 0xf603, 0xf603, input_port_2_r );	/* IN2 */
	MR_ADD( 0xf800, 0xf800, MRA_NOP );	/* gberetb only - IRQ acknowledge */
	return true;
}

private boolean writemem() {
	MW_START();
	MW_ADD( 0x0000, 0xffff, MWA_RAM ); /* CottAGE fixes - Unmapped memory */
	MW_ADD( 0x0000, 0xbfff, MWA_ROM );
	MW_ADD( 0xc000, 0xc7ff, colorram_w, colorram );
	MW_ADD( 0xc800, 0xcfff, videoram_w, videoram, videoram_size );
	MW_ADD( 0xd000, 0xd0bf, MWA_RAM, spriteram_2 );
	MW_ADD( 0xd100, 0xd1bf, MWA_RAM, spriteram, spriteram_size );
	MW_ADD( 0xd200, 0xdfff, MWA_RAM );
	MW_ADD( 0xe000, 0xe03f, MWA_RAM );  // , gberet_scrollram
	MW_ADD( 0xe043, 0xe043, MWA_RAM );	// , gberet_spritebank
	MW_ADD( 0xe044, 0xe044, gberet_e044_w );
	//MW_ADD( 0xf000, 0xf000, gberet_coincounter_w );
	MW_ADD( 0xf200, 0xf200, MWA_NOP );	/* Loads the snd command into the snd latch */
	MW_ADD( 0xf400, 0xf400, SN76496_0_w );		/* This address triggers the SN chip to read the data port. */
//	MW_ADD( 0xf600, 0xf600, MWA_NOP );
	return true;
}

private boolean mrgoemon_readmem() {
	MR_START();
	MR_ADD( 0x0000, 0xffff, MRA_RAM ); /* CottAGE fixes - Unmapped memory */
	MR_ADD( 0x0000, 0xbfff, MRA_ROM );
	MR_ADD( 0xc000, 0xe03f, MRA_RAM );
	MR_ADD( 0xf200, 0xf200, input_port_4_r );	/* DSW1 */
	MR_ADD( 0xf400, 0xf400, input_port_5_r );	/* DSW2 */
	MR_ADD( 0xf600, 0xf600, input_port_3_r );	/* DSW0 */
	MR_ADD( 0xf601, 0xf601, input_port_1_r );	/* IN1 */
	MR_ADD( 0xf602, 0xf602, input_port_0_r );	/* IN0 */
	MR_ADD( 0xf603, 0xf603, input_port_2_r );	/* IN2 */
	MR_ADD( 0xf800, 0xffff, MRA_BANK1 );
	return true;
}

private boolean mrgoemon_writemem() {
	MW_START();
	MW_ADD( 0x0000, 0xffff, MWA_RAM ); /* CottAGE fixes - Unmapped memory */
	MW_ADD( 0x0000, 0xbfff, MWA_ROM );
	MW_ADD( 0xc000, 0xc7ff, colorram_w, colorram );
	MW_ADD( 0xc800, 0xcfff, videoram_w, videoram, videoram_size );
	MW_ADD( 0xd000, 0xd0bf, MWA_RAM, spriteram_2 );
	MW_ADD( 0xd100, 0xd1bf, MWA_RAM, spriteram, spriteram_size );
	MW_ADD( 0xd200, 0xdfff, MWA_RAM );
	MW_ADD( 0xe000, 0xe03f, MWA_RAM );  // , gberet_scrollram
	MW_ADD( 0xe043, 0xe043, MWA_RAM );  // , gberet_spritebank
	MW_ADD( 0xe044, 0xe044, gberet_e044_w );
	MW_ADD( 0xf000, 0xf000, mrgoemon_bankswitch_w );	/* + coin counters */
	MW_ADD( 0xf200, 0xf200, MWA_NOP );		/* Loads the snd command into the snd latch */
	MW_ADD( 0xf400, 0xf400, SN76496_0_w );	/* This address triggers the SN chip to read the data port. */
	MW_ADD( 0xf800, 0xffff, MWA_ROM );
	return true;
}

private boolean ipt_gberet() {
	PORT_START();	/* IN0 */
	PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY );
	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY );
	PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY );
	PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY );
	PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 );
	PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON2 );
	PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
	PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );

	PORT_START();	/* IN1 */
	PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_COCKTAIL );
	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_COCKTAIL );
	PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_COCKTAIL );
	PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_COCKTAIL );
	PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_COCKTAIL );
	PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_COCKTAIL );
	PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
	PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );

	PORT_START();	/* IN2 */
	PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_COIN1 );
	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_COIN2 );
	PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_COIN3 );
	PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_START1 );
	PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_START2 );
	PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );
	PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
	PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );

	PORT_START();	/* DSW0 */
	PORT_DIPNAME( 0x0f, 0x0f, DEF_STR2( Coin_A ) );
	PORT_DIPSETTING(    0x02, DEF_STR2( _4C_1C ) );
	PORT_DIPSETTING(    0x05, DEF_STR2( _3C_1C ) );
	PORT_DIPSETTING(    0x08, DEF_STR2( _2C_1C ) );
	PORT_DIPSETTING(    0x04, DEF_STR2( _3C_2C ) );
	PORT_DIPSETTING(    0x01, DEF_STR2( _4C_3C ) );
	PORT_DIPSETTING(    0x0f, DEF_STR2( _1C_1C ) );
	PORT_DIPSETTING(    0x03, DEF_STR2( _3C_4C ) );
	PORT_DIPSETTING(    0x07, DEF_STR2( _2C_3C ) );
	PORT_DIPSETTING(    0x0e, DEF_STR2( _1C_2C ) );
	PORT_DIPSETTING(    0x06, DEF_STR2( _2C_5C ) );
	PORT_DIPSETTING(    0x0d, DEF_STR2( _1C_3C ) );
	PORT_DIPSETTING(    0x0c, DEF_STR2( _1C_4C ) );
	PORT_DIPSETTING(    0x0b, DEF_STR2( _1C_5C ) );
	PORT_DIPSETTING(    0x0a, DEF_STR2( _1C_6C ) );
	PORT_DIPSETTING(    0x09, DEF_STR2( _1C_7C ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( Free_Play ) );
	PORT_DIPNAME( 0xf0, 0xf0, DEF_STR2( Coin_B ) );
	PORT_DIPSETTING(    0x20, DEF_STR2( _4C_1C ) );
	PORT_DIPSETTING(    0x50, DEF_STR2( _3C_1C ) );
	PORT_DIPSETTING(    0x80, DEF_STR2( _2C_1C ) );
	PORT_DIPSETTING(    0x40, DEF_STR2( _3C_2C ) );
	PORT_DIPSETTING(    0x10, DEF_STR2( _4C_3C ) );
	PORT_DIPSETTING(    0xf0, DEF_STR2( _1C_1C ) );
	PORT_DIPSETTING(    0x30, DEF_STR2( _3C_4C ) );
	PORT_DIPSETTING(    0x70, DEF_STR2( _2C_3C ) );
	PORT_DIPSETTING(    0xe0, DEF_STR2( _1C_2C ) );
	PORT_DIPSETTING(    0x60, DEF_STR2( _2C_5C ) );
	PORT_DIPSETTING(    0xd0, DEF_STR2( _1C_3C ) );
	PORT_DIPSETTING(    0xc0, DEF_STR2( _1C_4C ) );
	PORT_DIPSETTING(    0xb0, DEF_STR2( _1C_5C ) );
	PORT_DIPSETTING(    0xa0, DEF_STR2( _1C_6C ) );
	PORT_DIPSETTING(    0x90, DEF_STR2( _1C_7C ) );
	/* 0x00 is invalid */

	PORT_START();	/* DSW1 */
	PORT_DIPNAME( 0x03, 0x02, DEF_STR2( Lives ) );
	PORT_DIPSETTING(    0x03, "2" );
	PORT_DIPSETTING(    0x02, "3" );
	PORT_DIPSETTING(    0x01, "5" );
	PORT_DIPSETTING(    0x00, "7" );
	PORT_DIPNAME( 0x04, 0x00, DEF_STR2( Cabinet ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( Upright ) );
	PORT_DIPSETTING(    0x04, DEF_STR2( Cocktail ) );
	PORT_DIPNAME( 0x18, 0x18, DEF_STR2( Bonus_Life ) );
	PORT_DIPSETTING(    0x18, "30000 70000" );
	PORT_DIPSETTING(    0x10, "40000 80000" );
	PORT_DIPSETTING(    0x08, "50000 100000" );
	PORT_DIPSETTING(    0x00, "50000 200000" );
	PORT_DIPNAME( 0x60, 0x60, DEF_STR2( Difficulty ) );
	PORT_DIPSETTING(    0x60, "Easy" );
	PORT_DIPSETTING(    0x40, "Medium" );
	PORT_DIPSETTING(    0x20, "Hard" );
	PORT_DIPSETTING(    0x00, "Hardest" );
	PORT_DIPNAME( 0x80, 0x00, DEF_STR2( Demo_Sounds ) );
	PORT_DIPSETTING(    0x80, DEF_STR2( Off ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( On ) );

	PORT_START();	/* DSW2 */
	PORT_DIPNAME( 0x01, 0x01, DEF_STR2( Flip_Screen ) );
	PORT_DIPSETTING(    0x01, DEF_STR2( Off ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( On ) );
	PORT_DIPNAME( 0x02, 0x02, "Controls" );
	PORT_DIPSETTING(    0x02, "Single" );
	PORT_DIPSETTING(    0x00, "Dual" );
	PORT_DIPNAME( 0x04, 0x04, DEF_STR2 ( Unknown ) );
	PORT_DIPSETTING(    0x04, DEF_STR2( Off ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( On ) );
	PORT_DIPNAME( 0x08, 0x08, DEF_STR2 ( Unknown ) );
	PORT_DIPSETTING(    0x08, DEF_STR2( Off ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( On ) );
	PORT_BIT( 0xf0, IP_ACTIVE_LOW, IPT_UNUSED );
	return true;
}

private boolean ipt_mrgoemon() {
	PORT_START();	/* IN0 */
	PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_4WAY );
	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_4WAY );
	PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_4WAY );
	PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_4WAY );
	PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 );
	PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON2 );
	PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
	PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );

	PORT_START();	/* IN1 */
	PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_4WAY | IPF_COCKTAIL );
	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_4WAY | IPF_COCKTAIL );
	PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_4WAY | IPF_COCKTAIL );
	PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_4WAY | IPF_COCKTAIL );
	PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_COCKTAIL );
	PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_COCKTAIL );
	PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
	PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );

	PORT_START();	/* IN2 */
	PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_COIN1 );
	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_COIN2 );
	PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_COIN3 );
	PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_START1 );
	PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_START2 );
	PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );
	PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
	PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );

	PORT_START();	/* DSW0 */
	PORT_DIPNAME( 0x0f, 0x0f, DEF_STR2( Coin_A ) );
	PORT_DIPSETTING(    0x02, DEF_STR2( _4C_1C ) );
	PORT_DIPSETTING(    0x05, DEF_STR2( _3C_1C ) );
	PORT_DIPSETTING(    0x08, DEF_STR2( _2C_1C ) );
	PORT_DIPSETTING(    0x04, DEF_STR2( _3C_2C ) );
	PORT_DIPSETTING(    0x01, DEF_STR2( _4C_3C ) );
	PORT_DIPSETTING(    0x0f, DEF_STR2( _1C_1C ) );
	PORT_DIPSETTING(    0x03, DEF_STR2( _3C_4C ) );
	PORT_DIPSETTING(    0x07, DEF_STR2( _2C_3C ) );
	PORT_DIPSETTING(    0x0e, DEF_STR2( _1C_2C ) );
	PORT_DIPSETTING(    0x06, DEF_STR2( _2C_5C ) );
	PORT_DIPSETTING(    0x0d, DEF_STR2( _1C_3C ) );
	PORT_DIPSETTING(    0x0c, DEF_STR2( _1C_4C ) );
	PORT_DIPSETTING(    0x0b, DEF_STR2( _1C_5C ) );
	PORT_DIPSETTING(    0x0a, DEF_STR2( _1C_6C ) );
	PORT_DIPSETTING(    0x09, DEF_STR2( _1C_7C ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( Free_Play ) );
	PORT_DIPNAME( 0xf0, 0xf0, DEF_STR2( Coin_B ) );
	PORT_DIPSETTING(    0x20, DEF_STR2( _4C_1C ) );
	PORT_DIPSETTING(    0x50, DEF_STR2( _3C_1C ) );
	PORT_DIPSETTING(    0x80, DEF_STR2( _2C_1C ) );
	PORT_DIPSETTING(    0x40, DEF_STR2( _3C_2C ) );
	PORT_DIPSETTING(    0x10, DEF_STR2( _4C_3C ) );
	PORT_DIPSETTING(    0xf0, DEF_STR2( _1C_1C ) );
	PORT_DIPSETTING(    0x30, DEF_STR2( _3C_4C ) );
	PORT_DIPSETTING(    0x70, DEF_STR2( _2C_3C ) );
	PORT_DIPSETTING(    0xe0, DEF_STR2( _1C_2C ) );
	PORT_DIPSETTING(    0x60, DEF_STR2( _2C_5C ) );
	PORT_DIPSETTING(    0xd0, DEF_STR2( _1C_3C ) );
	PORT_DIPSETTING(    0xc0, DEF_STR2( _1C_4C ) );
	PORT_DIPSETTING(    0xb0, DEF_STR2( _1C_5C ) );
	PORT_DIPSETTING(    0xa0, DEF_STR2( _1C_6C ) );
	PORT_DIPSETTING(    0x90, DEF_STR2( _1C_7C ) );
	/* 0x00 is invalid */

	PORT_START();	/* DSW1 */
	PORT_DIPNAME( 0x03, 0x02, DEF_STR2( Lives ) );
	PORT_DIPSETTING(    0x03, "2" );
	PORT_DIPSETTING(    0x02, "3" );
	PORT_DIPSETTING(    0x01, "5" );
	PORT_DIPSETTING(    0x00, "7" );
	PORT_DIPNAME( 0x04, 0x00, DEF_STR2( Cabinet ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( Upright ) );
	PORT_DIPSETTING(    0x04, DEF_STR2( Cocktail ) );
	PORT_DIPNAME( 0x18, 0x18, DEF_STR2( Bonus_Life ) );
	PORT_DIPSETTING(    0x18, "20000 and every 60000" );
	PORT_DIPSETTING(    0x10, "30000 and every 70000" );
	PORT_DIPSETTING(    0x08, "40000 and every 80000" );
	PORT_DIPSETTING(    0x00, "50000 and every 90000" );
	PORT_DIPNAME( 0x60, 0x60, DEF_STR2( Difficulty ) );
	PORT_DIPSETTING(    0x60, "Easy" );
	PORT_DIPSETTING(    0x40, "Medium" );
	PORT_DIPSETTING(    0x20, "Hard" );
	PORT_DIPSETTING(    0x00, "Hardest" );
	PORT_DIPNAME( 0x80, 0x00, DEF_STR2( Demo_Sounds ) );
	PORT_DIPSETTING(    0x80, DEF_STR2( Off ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( On ) );

	PORT_START();	/* DSW2 */
	PORT_DIPNAME( 0x01, 0x01, DEF_STR2( Flip_Screen ) );
	PORT_DIPSETTING(    0x01, DEF_STR2( Off ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( On ) );
	PORT_DIPNAME( 0x02, 0x02, "Controls" );
	PORT_DIPSETTING(    0x02, "Single" );
	PORT_DIPSETTING(    0x00, "Dual" );
	PORT_SERVICE( 0x04, IP_ACTIVE_LOW );
	PORT_DIPNAME( 0x08, 0x08, DEF_STR2( Unknown ) );
	PORT_DIPSETTING(    0x08, DEF_STR2( Off ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( On ) );
	PORT_BIT( 0xf0, IP_ACTIVE_LOW, IPT_UNUSED );
	return true;
}

int[][] charlayout =
{
	{8},{8},	/* 8*8 characters */
	{512},	/* 512 characters */
	{4},	/* 4 bits per pixel */
	{ 0, 1, 2, 3 },	/* the four bitplanes are packed in one nibble */
	{ 0*4, 1*4, 2*4, 3*4, 4*4, 5*4, 6*4, 7*4 },
	{ 0*32, 1*32, 2*32, 3*32, 4*32, 5*32, 6*32, 7*32 },
	{32*8}	/* every char takes 8 consecutive bytes */
};
int[][] spritelayout =
{
	{16},{16},	/* 16*16 sprites */
	{512},	/* 512 sprites */
	{4},	/* 4 bits per pixel */
	{ 0, 1, 2, 3 },	/* the four bitplanes are packed in one nibble */
	{ 0*4, 1*4, 2*4, 3*4, 4*4, 5*4, 6*4, 7*4,
		32*8+0*4, 32*8+1*4, 32*8+2*4, 32*8+3*4, 32*8+4*4, 32*8+5*4, 32*8+6*4, 32*8+7*4 },
	{ 0*32, 1*32, 2*32, 3*32, 4*32, 5*32, 6*32, 7*32,
		64*8+0*32, 64*8+1*32, 64*8+2*32, 64*8+3*32, 64*8+4*32, 64*8+5*32, 64*8+6*32, 64*8+7*32 },
	{128*8}	/* every sprite takes 128 consecutive bytes */
};

private boolean gfxdecodeinfo()
{
	GDI_ADD( REGION_GFX1, 0, charlayout, 	   0, 16 );
	GDI_ADD( REGION_GFX2, 0, spritelayout, 16*16, 16 );
	GDI_ADD( -1 );
	return true;
};

class Gberet_snd_w implements WriteHandler {
	public void write(int address, int data) {
		sound_command = data;
	}
}

class SN76496_write implements WriteHandler {
	public void write(int address, int data) {
		sn.command_w(sound_command);
	}
}

class Mrgoemon_bankswitch_w implements WriteHandler {
	public void write(int address, int data) {
		int[] RAM = memory_region(REGION_CPU1);
		int offs;

		/* bits 0/1 = coin counters */
		//coin_counter_w(0,data & 1);
		//coin_counter_w(1,data & 2);

		/* bits 5-7 = ROM bank select */
		offs = 0x10000 + ((data & 0xe0) >> 5) * 0x800;
		cpu_setbank(1,offs);
	}
}

public boolean mdrv_gberet() {
	/* basic machine hardware */
	MDRV_CPU_ADD(Z80,18432000/6);	/* X1S (generated by a custom IC); */
	MDRV_CPU_MEMORY(readmem(),writemem());
	MDRV_CPU_VBLANK_INT(gberet_interrupt,32);	/* 1 IRQ + 16 NMI hgenerated by a custom IC); */

	/* sound hardware */
	MDRV_SOUND_ADD((SoundChipEmulator)sn);

	MDRV_FRAMES_PER_SECOND(30);
	MDRV_VBLANK_DURATION(DEFAULT_30HZ_VBLANK_DURATION);

	/* video hardware */
	MDRV_VIDEO_ATTRIBUTES(VIDEO_TYPE_RASTER);
	MDRV_SCREEN_SIZE(32*8, 32*8);
	MDRV_VISIBLE_AREA(1*8, 31*8-1, 2*8, 30*8-1);
	MDRV_GFXDECODE(gfxdecodeinfo());
	MDRV_PALETTE_LENGTH(32);
	MDRV_COLORTABLE_LENGTH(2*16*16);

	MDRV_PALETTE_INIT(gberet_pi);
	MDRV_VIDEO_START(gberet_vs);
	MDRV_VIDEO_UPDATE(gberet_vu);
	return true;
}

public boolean mdrv_mrgoemon() {
	/* basic machine hardware */
	MDRV_CPU_ADD(Z80,18432000/6);	/* X1S (generated by a custom IC) */
	MDRV_CPU_MEMORY(mrgoemon_readmem(),mrgoemon_writemem());
	MDRV_CPU_VBLANK_INT(gberet_interrupt,16);	/* 1 IRQ + 8 NMI */

	/* sound hardware */
	MDRV_SOUND_ADD((SoundChipEmulator)sn);

	MDRV_FRAMES_PER_SECOND(60);
	MDRV_VBLANK_DURATION(DEFAULT_60HZ_VBLANK_DURATION);

	/* video hardware */
	MDRV_VIDEO_ATTRIBUTES(VIDEO_TYPE_RASTER);
	MDRV_SCREEN_SIZE(32*8, 32*8);
	MDRV_VISIBLE_AREA(1*8, 31*8-1, 2*8, 30*8-1);
	MDRV_GFXDECODE(gfxdecodeinfo());
	MDRV_PALETTE_LENGTH(32);
	MDRV_COLORTABLE_LENGTH(2*16*16);

	MDRV_PALETTE_INIT(gberet_pi);
	MDRV_VIDEO_START(gberet_vs);
	MDRV_VIDEO_UPDATE(gberet_vu);
	return true;
}

/***************************************************************************

  Game driver(s)

***************************************************************************/
private boolean rom_gberet() {
	ROM_REGION( 0x10000, REGION_CPU1, 0 );	/* 64k for code */
	ROM_LOAD( "c10_l03.bin",  0x0000, 0x4000, 0xae29e4ff );
	ROM_LOAD( "c08_l02.bin",  0x4000, 0x4000, 0x240836a5 );
	ROM_LOAD( "c07_l01.bin",  0x8000, 0x4000, 0x41fa3e1f );

	ROM_REGION( 0x04000, REGION_GFX1, ROMREGION_DISPOSE );
	ROM_LOAD( "f03_l07.bin",  0x00000, 0x4000, 0x4da7bd1b );

	ROM_REGION( 0x10000, REGION_GFX2, ROMREGION_DISPOSE );
	ROM_LOAD( "e05_l06.bin",  0x00000, 0x4000, 0x0f1cb0ca );
	ROM_LOAD( "e04_l05.bin",  0x04000, 0x4000, 0x523a8b66 );
	ROM_LOAD( "f04_l08.bin",  0x08000, 0x4000, 0x883933a4 );
	ROM_LOAD( "e03_l04.bin",  0x0c000, 0x4000, 0xccecda4c );

	ROM_REGION( 0x0220, REGION_PROMS, 0 );
	ROM_LOAD( "577h09",       0x0000, 0x0020, 0xc15e7c80 ); /* palette */
	ROM_LOAD( "577h10",       0x0020, 0x0100, 0xe9de1e53 ); /* sprites */
	ROM_LOAD( "577h11",       0x0120, 0x0100, 0x2a1a992b ); /* characters */
	return true;
}

private boolean rom_rushatck() {
	ROM_REGION( 0x10000, REGION_CPU1, 0 );	/* 64k for code */
	ROM_LOAD( "rush_h03.10c", 0x0000, 0x4000, 0x4d276b52 );
	ROM_LOAD( "rush_h02.8c",  0x4000, 0x4000, 0xb5802806 );
	ROM_LOAD( "rush_h01.7c",  0x8000, 0x4000, 0xda7c8f3d );

	ROM_REGION( 0x04000, REGION_GFX1, ROMREGION_DISPOSE );
	ROM_LOAD( "rush_h07.3f",  0x00000, 0x4000, 0x03f9815f );

	ROM_REGION( 0x10000, REGION_GFX2, ROMREGION_DISPOSE );
	ROM_LOAD( "e05_l06.bin",  0x00000, 0x4000, 0x0f1cb0ca );
	ROM_LOAD( "rush_h05.4e",  0x04000, 0x4000, 0x9d028e8f );
	ROM_LOAD( "f04_l08.bin",  0x08000, 0x4000, 0x883933a4 );
	ROM_LOAD( "e03_l04.bin",  0x0c000, 0x4000, 0xccecda4c );

	ROM_REGION( 0x0220, REGION_PROMS, 0 );
	ROM_LOAD( "577h09",       0x0000, 0x0020, 0xc15e7c80 ); /* palette */
	ROM_LOAD( "577h10",       0x0020, 0x0100, 0xe9de1e53 ); /* sprites */
	ROM_LOAD( "577h11",       0x0120, 0x0100, 0x2a1a992b ); /* characters */
	return true;
}

private boolean rom_mrgoemon() {
	ROM_REGION( 0x14000, REGION_CPU1, 0 );	/* 64k for code + banked ROM */
	ROM_LOAD( "621d01.10c",   0x00000, 0x8000, 0xb2219c56 );
	ROM_LOAD( "621d02.12c",   0x08000, 0x4000, 0xc3337a97 );
	ROM_CONTINUE(             0x10000, 0x4000 );

	ROM_REGION( 0x04000, REGION_GFX1, ROMREGION_DISPOSE );
	ROM_LOAD( "621a05.6d",   0x00000, 0x4000, 0xf0a6dfc5 );

	ROM_REGION( 0x10000, REGION_GFX2, ROMREGION_DISPOSE );
	ROM_LOAD( "621d03.4d",   0x00000, 0x8000, 0x66f2b973 );
	ROM_LOAD( "621d04.5d",   0x08000, 0x8000, 0x47df6301 );

	ROM_REGION( 0x0220, REGION_PROMS, 0 );
	ROM_LOAD( "621a06.5f",    0x0000, 0x0020, 0x7c90de5f ); /* palette */
	ROM_LOAD( "621a07.6f",    0x0020, 0x0100, 0x3980acdc ); /* sprites */
	ROM_LOAD( "621a08.7f",    0x0120, 0x0100, 0x2fb244dd ); /* characters */
	return true;
}

	public Machine getMachine(URL url, String name) {
		super.getMachine(url,name);
		super.setVideoEmulator(v);

		if (name.equals("gberet")) {
			GAME( 1985, rom_gberet(),          0, mdrv_gberet(),   ipt_gberet(),   0, ROT0, "Konami", "Green Beret" );
		} else if (name.equals("rushatck")) {
			GAME( 1985, rom_rushatck(), "gberet", mdrv_gberet(),   ipt_gberet(),   0, ROT0, "Konami", "Rush'n Attack" );
		} else if (name.equals("mrgoemon")) {
			GAME( 1986, rom_mrgoemon(),        0, mdrv_mrgoemon(), ipt_mrgoemon(), 0, ROT0, "Konami", "Mr. Goemon (Japan)" );
		}

		m.init(md);
		return (Machine)m;
	}

}
