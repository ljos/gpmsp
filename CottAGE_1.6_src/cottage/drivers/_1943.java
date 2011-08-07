/*
CottAGE - the Arcade Generic Emulator in Java

Java driver by Erik Duijs
*/

/*
TODO: 1943 is almost identical to GunSmoke (one more scrolling playfield). We
      should merge the two drivers.
*/

/***************************************************************************

  GUNSMOKE
  ========

  Driver provided by Paul Leaman


Stephh's notes (based on the games Z80 code and some tests) :

0) all games

  - There is some code that allows you to select your starting level
    (at 0x08dc in 'gunsmoka' and at 0x08d2 in the other sets).
    To do so, once the game has booted (after the "notice" screen),
    turn the "service" mode Dip Switch ON, and change Dip Switches
    DSW 1-0 to 1-3 (which are used by coinage). You can also set
    GUNSMOKE_HACK to 1 and change the fake "Starting Level" Dip Switch.
  - About the ingame bug at the end of level 2 : enemy's energy
    (stored at 0xf790) is in fact not infinite, but it turns back to
    0xff, so when it reaches 0 again, the boss is dead.


1) 'gunsmoke'

  - World version.
    You can enter 3 chars for your initials.


2) 'gunsmokj'

  - Japan version (but English text though).
    You can enter 8 chars for your initials.


3) 'gunsmoku'

  - US version licenced to Romstar.
    You can enter 3 chars for your initials.


4) 'gunsmoku'

  - US version licenced to Romstar.
    You can enter 3 chars for your initials.
  - This is probably a later version of the game because some code
    has been added for the "Lives" Dip Switch that replaces the
    "Demonstation" one (so demonstration is always OFF).
  - Other changes :
      * Year is 1986 instead of 1985.
      * High score is 110000 instead of 100000.
      * Levels 3 and 6 are swapped.


***************************************************************************/

package cottage.drivers;

import java.net.URL;

import jef.machine.*;
import jef.map.*;
import jef.sound.chip.YM2203;
import jef.video.*;

import cottage.mame.*;

public class _1943 extends MAMEDriver {

jef.cpu.Z80 z80 = new jef.cpu.Z80();
jef.cpu.Z80 z80_2 = new jef.cpu.Z80();

cottage.vidhrdw._1943 v = new cottage.vidhrdw._1943();
WriteHandler videoram_w = v.videoram_w();
WriteHandler colorram_w = v.colorram_w();
Vh_start c1943_vs	= (Vh_start)v;
Vh_refresh c1943_vu = (Vh_refresh)v;
Vh_convert_color_proms c1943_pi = (Vh_convert_color_proms)v;
Vh_start gunsmoke_vs = (Vh_start)v.gunsmoke_vs();
Vh_refresh gunsmoke_vu = (Vh_refresh)v.gunsmoke_vu();
Vh_convert_color_proms gunsmoke_pi = (Vh_convert_color_proms)v.gunsmoke_pi();

jef.machine.BasicMachine m = new jef.machine.BasicMachine();
InterruptHandler irq0_line_hold = m.irq0_line_hold();

ReadHandler	 c1943_protection_r = (ReadHandler) new C1943_protection_r();
WriteHandler c1943_c804_w = (WriteHandler) new C1943_c804_w();
WriteHandler c1943_d806_w = (WriteHandler) new C1943_d806_w();

ReadHandler	 gunsmoke_unknown_r = (ReadHandler) new Gunsmoke_unknown_r();
WriteHandler gunsmoke_c804_w = (WriteHandler) new Gunsmoke_c804_w();
WriteHandler gunsmoke_d806_w = (WriteHandler) new Gunsmoke_d806_w();

int bankaddress;
private YM2203 ym = new YM2203(2, 1500000, null, null);

/* this is a protection check. The game crashes (thru a jump to 0x8000) */
/* if a read from this address doesn't return the value it expects. */
public class C1943_protection_r implements ReadHandler {
	public int read(int address) {
		return z80.B;
	}
}

public class C1943_c804_w implements WriteHandler {
	public void write(int address, int data) {

		/* bits 0 and 1 are coin counters */
		//coin_counter_w(0,data & 1);
		//coin_counter_w(1,data & 2);

		/* bits 2, 3 and 4 select the ROM bank */
		bankaddress = 0x10000 + (data & 0x1c) * 0x1000;

		cpu_setbank(1,bankaddress);

		/* bit 5 resets the sound CPU - we ignore it */

		/* bit 6 flips screen */
		//if (flipscreen != (data & 0x40))
		//{
		//	flipscreen = data & 0x40;
	//		memset(dirtybuffer,1,c1942_backgroundram_size);
		//}

		/* bit 7 enables characters */
		v.chon = data & 0x80;
	}
}

public class C1943_d806_w implements WriteHandler {
	public void write(int address, int data) {
		/* bit 4 enables bg 1 */
		v.sc1on = data & 0x10;

		/* bit 5 enables bg 2 */
		v.sc2on = data & 0x20;

		/* bit 6 enables sprites */
		v.objon = data & 0x40;
	}
}

public class Gunsmoke_unknown_r implements ReadHandler {
	public int read(int address) {
		/*
		The routine at 0x0e69 tries to read data starting at 0xc4c9.
		If this value is zero, it interprets the next two bytes as a
		jump address.

		This was resulting in a reboot which happens at the end of level 3
		if you go too far to the right of the screen when fighting the level boss.

		A non-zero for the first byte seems to be harmless  (although it may not be
		the correct behaviour).

		This could be some devious protection or it could be a bug in the
		arcade game.  It's hard to tell without pulling the code apart.
		*/
		return 0;
	}
}

public class Gunsmoke_c804_w implements WriteHandler {
	public void write(int address, int data) {
		int bankaddress;

		/* bits 0 and 1 are for coin counters */
		//coin_counter_w(1,data & 1);
		//coin_counter_w(0,data & 2);

		/* bits 2 and 3 select the ROM bank */
		bankaddress = 0x10000 + (data & 0x0c) * 0x1000;
		cpu_setbank(1,bankaddress);

		/* bit 5 resets the sound CPU? - we ignore it */

		/* bit 6 flips screen */
		//flip_screen_set(data & 0x40);

		/* bit 7 enables characters? */
		v.chon = data & 0x80;
	}
}

public class Gunsmoke_d806_w implements WriteHandler {
	public void write(int address, int data) {
		/* bits 0-2 select the sprite 3 bank */
		v.sprite3bank = data & 0x07;

		/* bit 4 enables bg 1? */
		v.bgon = data & 0x10;

		/* bit 5 enables sprites? */
		v.objon = data & 0x20;
	}
}

private boolean readmem() {
	MR_START();
	MR_ADD( 0x0000, 0x7fff, MRA_ROM );
	MR_ADD( 0x8000, 0xbfff, MRA_BANK1 );
	MR_ADD( 0xd000, 0xd7ff, MRA_RAM );
	MR_ADD( 0xc000, 0xc000, input_port_0_r );
	MR_ADD( 0xc001, 0xc001, input_port_1_r );
	MR_ADD( 0xc002, 0xc002, input_port_2_r );
	MR_ADD( 0xc003, 0xc003, input_port_3_r );
	MR_ADD( 0xc004, 0xc004, input_port_4_r );
	MR_ADD( 0xc007, 0xc007, c1943_protection_r );
	MR_ADD( 0xe000, 0xffff, MRA_RAM );
	return true;
}

private boolean writemem() {
	MW_START();
	MW_ADD( 0x0000, 0xbfff, MWA_ROM );
	MW_ADD( 0xc800, 0xc800, soundlatch_w );
	MW_ADD( 0xc804, 0xc804, c1943_c804_w );	/* ROM bank switch, screen flip */
	//MW_ADD( 0xc806, 0xc806, watchdog_reset_w );
	MW_ADD( 0xc807, 0xc807, MWA_NOP ); 	/* protection chip write (we don't emulate it) */
	MW_ADD( 0xd000, 0xd3ff, videoram_w, videoram, videoram_size );
	MW_ADD( 0xd400, 0xd7ff, colorram_w, colorram );
	MW_ADD( 0xd800, 0xd801, MWA_RAM ); //, c1943_scrolly
	MW_ADD( 0xd802, 0xd802, MWA_RAM ); //, c1943_scrollx
	MW_ADD( 0xd803, 0xd804, MWA_RAM ); //, c1943_bgscrolly
	MW_ADD( 0xd806, 0xd806, c1943_d806_w );	/* sprites, bg1, bg2 enable */
	MW_ADD( 0xe000, 0xefff, MWA_RAM );
	MW_ADD( 0xf000, 0xffff, MWA_RAM, spriteram, spriteram_size );
	return true;
}

private boolean gunsmoke_readmem() {
	MR_START();
	MR_ADD( 0x0000, 0x7fff, MRA_ROM );
	MR_ADD( 0x8000, 0xbfff, MRA_BANK1 );
	MR_ADD( 0xc000, 0xc000, input_port_0_r );
	MR_ADD( 0xc001, 0xc001, input_port_1_r );
	MR_ADD( 0xc002, 0xc002, input_port_2_r );
	MR_ADD( 0xc003, 0xc003, input_port_3_r );
	MR_ADD( 0xc004, 0xc004, input_port_4_r );
    MR_ADD( 0xc4c9, 0xc4cb, gunsmoke_unknown_r );
    MR_ADD( 0xd000, 0xd3ff, videoram_r );
    MR_ADD( 0xd400, 0xd7ff, colorram_r );
	MR_ADD( 0xe000, 0xffff, MRA_RAM ); /* Work + sprite RAM */
	return true;
}

private boolean gunsmoke_writemem() {
	MW_START();
	MW_ADD( 0x0000, 0xbfff, MWA_ROM );
	MW_ADD( 0xc800, 0xc800, soundlatch_w );
	MW_ADD( 0xc804, 0xc804, gunsmoke_c804_w );	/* ROM bank switch, screen flip */
	MW_ADD( 0xc806, 0xc806, MWA_NOP ); /* Watchdog ?? */
	MW_ADD( 0xd000, 0xd3ff, videoram_w, videoram, videoram_size );
	MW_ADD( 0xd400, 0xd7ff, colorram_w, colorram );
	MW_ADD( 0xd800, 0xd801, MWA_RAM );	// , &gunsmoke_bg_scrolly
	MW_ADD( 0xd802, 0xd802, MWA_RAM );	// , &gunsmoke_bg_scrollx
	MW_ADD( 0xd806, 0xd806, gunsmoke_d806_w );	/* sprites and bg enable */
	MW_ADD( 0xe000, 0xefff, MWA_RAM );
	MW_ADD( 0xf000, 0xffff, MWA_RAM, spriteram, spriteram_size );
	return true;
}

private boolean sound_readmem() {
	MR_START();
	MR_ADD(0x0000, 0x7fff, MRA_ROM);
	MR_ADD(0xc000, 0xc7ff, MRA_RAM);
	MR_ADD(0xc800, 0xc800, soundlatch_r);
	return true;
};

private boolean sound_writemem() {
	MW_START();
	MW_ADD(0x0000, 0x7fff, MWA_ROM);
	MW_ADD(0xc000, 0xc7ff, MWA_RAM);
	MW_ADD(0xe000, 0xe000, ym.ym2203_control_port_0_w());
	MW_ADD(0xe001, 0xe001, ym.ym2203_write_port_0_w());
	MW_ADD(0xe002, 0xe002, ym.ym2203_control_port_1_w());
	MW_ADD(0xe003, 0xe003, ym.ym2203_write_port_1_w());
	return true;
}
private boolean ipt_1943() {
	PORT_START();	/* IN0 */
	PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_START1 );
	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_START2 );
	PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_UNUSED );
	PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_UNKNOWN );	/* actually, this is VBLANK */
	PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_UNKNOWN );	/* probably unused */
	PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );	/* probably unused */
	PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_COIN1 );
	PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_COIN2 );

	PORT_START();	/* IN1 */
	PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY );
	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT | IPF_8WAY );
	PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN | IPF_8WAY );
	PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_UP | IPF_8WAY );
	PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 );
	PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON2 );
	PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );	/* Button 3, probably unused */
	PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNUSED );

	PORT_START();	/* IN2 */
	PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_PLAYER2 );
	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT | IPF_8WAY | IPF_PLAYER2 );
	PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN | IPF_8WAY | IPF_PLAYER2 );
	PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_UP | IPF_8WAY | IPF_PLAYER2 );
	PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER2 );
	PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER2 );
	PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );	/* Button 3, probably unused */
	PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNUSED );

	PORT_START();	/* DSW0 */
	PORT_DIPNAME( 0x0f, 0x0f, DEF_STR2( Difficulty ) );
	PORT_DIPSETTING(    0x0f, "1 (Easiest)" );
	PORT_DIPSETTING(    0x0e, "2" );
	PORT_DIPSETTING(    0x0d, "3" );
	PORT_DIPSETTING(    0x0c, "4" );
	PORT_DIPSETTING(    0x0b, "5" );
	PORT_DIPSETTING(    0x0a, "6" );
	PORT_DIPSETTING(    0x09, "7" );
	PORT_DIPSETTING(    0x08, "8" );
	PORT_DIPSETTING(    0x07, "9" );
	PORT_DIPSETTING(    0x06, "10" );
	PORT_DIPSETTING(    0x05, "11" );
	PORT_DIPSETTING(    0x04, "12" );
	PORT_DIPSETTING(    0x03, "13" );
	PORT_DIPSETTING(    0x02, "14" );
	PORT_DIPSETTING(    0x01, "15" );
	PORT_DIPSETTING(    0x00, "16 (Hardest)" );
	PORT_DIPNAME( 0x10, 0x10, "2 Players Game" );
	PORT_DIPSETTING(    0x00, "1 Credit" );
	PORT_DIPSETTING(    0x10, "2 Credits" );
	PORT_DIPNAME( 0x20, 0x20, DEF_STR2( Flip_Screen ) );
	PORT_DIPSETTING(    0x20, DEF_STR2( Off ));
	PORT_DIPSETTING(    0x00, DEF_STR2( On ));
	PORT_DIPNAME( 0x40, 0x40, "Freeze" );
	PORT_DIPSETTING(    0x40, DEF_STR2( Off ));
	PORT_DIPSETTING(    0x00, DEF_STR2( On ) );
	PORT_SERVICE( 0x80, IP_ACTIVE_LOW );

	PORT_START();      /* DSW1 */
	PORT_DIPNAME( 0x07, 0x07, DEF_STR2( Coin_A ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( _4C_1C ));
	PORT_DIPSETTING(    0x01, DEF_STR2( _3C_1C ) );
	PORT_DIPSETTING(    0x02, DEF_STR2( _2C_1C ) );
	PORT_DIPSETTING(    0x07, DEF_STR2( _1C_1C ) );
	PORT_DIPSETTING(    0x06, DEF_STR2( _1C_2C ) );
	PORT_DIPSETTING(    0x05, DEF_STR2( _1C_3C ) );
	PORT_DIPSETTING(    0x04, DEF_STR2( _1C_4C ) );
	PORT_DIPSETTING(    0x03, DEF_STR2( _1C_5C ) );
	PORT_DIPNAME( 0x38, 0x38, DEF_STR2( Coin_B ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( _4C_1C ));
	PORT_DIPSETTING(    0x08, DEF_STR2( _3C_1C ) );
	PORT_DIPSETTING(    0x10, DEF_STR2( _2C_1C ) );
	PORT_DIPSETTING(    0x38, DEF_STR2( _1C_1C ) );
	PORT_DIPSETTING(    0x30, DEF_STR2( _1C_2C ) );
	PORT_DIPSETTING(    0x28, DEF_STR2( _1C_3C ) );
	PORT_DIPSETTING(    0x20, DEF_STR2( _1C_4C ) );
	PORT_DIPSETTING(    0x18, DEF_STR2( _1C_5C ) );
	PORT_DIPNAME( 0x40, 0x40, "Allow Continue" );
	PORT_DIPSETTING(    0x00, DEF_STR2( No ) );
	PORT_DIPSETTING(    0x40, DEF_STR2( Yes ) );
	PORT_DIPNAME( 0x80, 0x80, DEF_STR2( Demo_Sounds ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( Off ) );
	PORT_DIPSETTING(    0x80, DEF_STR2( On ) );
	return true;
}

private boolean ipt_gunsmoke() {
	PORT_START();	/* IN0 */
	PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_START1 );
	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_START2 );
	PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_UNKNOWN );	/* probably unused */
	PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_UNKNOWN );	/* probably unused */
	PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_COIN3 );
	PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );	/* probably unused */
	PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_COIN2 );
	PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_COIN1 );

	PORT_START();	/* IN1 */
	PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY );
	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT | IPF_8WAY );
	PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN | IPF_8WAY );
	PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_UP | IPF_8WAY );
	PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 );
	PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON2 );
	PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_BUTTON3 );
	PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );	/* probably unused */

	PORT_START();	/* IN2 */
	PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_COCKTAIL );
	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT | IPF_8WAY | IPF_COCKTAIL );
	PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN | IPF_8WAY | IPF_COCKTAIL );
	PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_UP | IPF_8WAY | IPF_COCKTAIL );
	PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_COCKTAIL );
	PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_COCKTAIL );
	PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_COCKTAIL );
	PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );	/* probably unused */

	PORT_START();	/* DSW0 */
	PORT_DIPNAME( 0x03, 0x03, DEF_STR2( Bonus_Life ) );
	PORT_DIPSETTING(    0x03, "30k, 100k & every 100k");
	PORT_DIPSETTING(    0x02, "30k, 80k & every 80k" );
	PORT_DIPSETTING(    0x01, "30k & 100K only");
	PORT_DIPSETTING(    0x00, "30k, 100k & every 150k" );
	PORT_DIPNAME( 0x04, 0x04, "Demonstration" );
	PORT_DIPSETTING(    0x00, DEF_STR2( Off ));
	PORT_DIPSETTING(    0x04, DEF_STR2( On ) );
	PORT_DIPNAME( 0x08, 0x00, DEF_STR2( Cabinet ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( Upright ));
	PORT_DIPSETTING(    0x08, DEF_STR2( Cocktail ));
	PORT_DIPNAME( 0x30, 0x30, DEF_STR2( Difficulty ) );
	PORT_DIPSETTING(    0x20, "Easy" );
	PORT_DIPSETTING(    0x30, "Normal" );
	PORT_DIPSETTING(    0x10, "Difficult" );
	PORT_DIPSETTING(    0x00, "Very Difficult" );
	PORT_DIPNAME( 0x40, 0x40, "Freeze" );
	PORT_DIPSETTING(    0x40, DEF_STR2( Off ));
	PORT_DIPSETTING(    0x00, DEF_STR2( On ) );
	PORT_SERVICE( 0x80, IP_ACTIVE_LOW );

	PORT_START();      /* DSW1 */
	PORT_DIPNAME( 0x07, 0x07, DEF_STR2( Coin_B ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( _4C_1C ));
	PORT_DIPSETTING(    0x01, DEF_STR2( _3C_1C ) );
	PORT_DIPSETTING(    0x02, DEF_STR2( _2C_1C ) );
	PORT_DIPSETTING(    0x07, DEF_STR2( _1C_1C ) );
	PORT_DIPSETTING(    0x06, DEF_STR2( _1C_2C ) );
	PORT_DIPSETTING(    0x05, DEF_STR2( _1C_3C ) );
	PORT_DIPSETTING(    0x04, DEF_STR2( _1C_4C ) );
	PORT_DIPSETTING(    0x03, DEF_STR2( _1C_6C ) );
	PORT_DIPNAME( 0x38, 0x38, DEF_STR2( Coin_A ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( _4C_1C ));
	PORT_DIPSETTING(    0x08, DEF_STR2( _3C_1C ) );
	PORT_DIPSETTING(    0x10, DEF_STR2( _2C_1C ) );
	PORT_DIPSETTING(    0x38, DEF_STR2( _1C_1C ) );
	PORT_DIPSETTING(    0x30, DEF_STR2( _1C_2C ) );
	PORT_DIPSETTING(    0x28, DEF_STR2( _1C_3C ) );
	PORT_DIPSETTING(    0x20, DEF_STR2( _1C_4C ) );
	PORT_DIPSETTING(    0x18, DEF_STR2( _1C_6C ) );
	PORT_DIPNAME( 0x40, 0x40, "Allow Continue" );
	PORT_DIPSETTING(    0x00, DEF_STR2( No ));
	PORT_DIPSETTING(    0x40, DEF_STR2( Yes ));
	PORT_DIPNAME( 0x80, 0x80, DEF_STR2( Demo_Sounds ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( Off ) );
	PORT_DIPSETTING(    0x80, DEF_STR2( On ));
	return true;
}


int[][] charlayout =
{
	{8},{8},	/* 8*8 characters */
	{2048},	/* 2048 characters */
	{2},	/* 2 bits per pixel */
	{ 4, 0 },
	{ 0, 1, 2, 3, 8+0, 8+1, 8+2, 8+3 },
	{ 0*16, 1*16, 2*16, 3*16, 4*16, 5*16, 6*16, 7*16 },
	{16*8}	/* every char takes 16 consecutive bytes */
};
int[][] gunsmoke_charlayout =
{
	{8},{8},	/* 8*8 characters */
	{1024},	/* 1024 characters */
	{2},	/* 2 bits per pixel */
	{ 4, 0 },
	{ 0, 1, 2, 3, 8+0, 8+1, 8+2, 8+3 },
	{ 0*16, 1*16, 2*16, 3*16, 4*16, 5*16, 6*16, 7*16 },
	{16*8}	/* every char takes 16 consecutive bytes */
};
int[][] spritelayout =
{
	{16},{16},	/* 16*16 sprites */
	{2048},	/* 2048 sprites */
	{4},	/* 4 bits per pixel */
	{ 4, 0, 2048*64*8+4, 2048*64*8+0 },
	{ 0, 1, 2, 3, 8+0, 8+1, 8+2, 8+3,
			32*8+0, 32*8+1, 32*8+2, 32*8+3, 33*8+0, 33*8+1, 33*8+2, 33*8+3 },
	{ 0*16, 1*16, 2*16, 3*16, 4*16, 5*16, 6*16, 7*16,
			8*16, 9*16, 10*16, 11*16, 12*16, 13*16, 14*16, 15*16 },
	{64*8}	/* every sprite takes 64 consecutive bytes */
};
int[][] fgtilelayout =
{
	{32},{32},  /* 32*32 tiles */
	{512},    /* 512 tiles */
	{4},      /* 4 bits per pixel */
	{ 4, 0, 512*256*8+4, 512*256*8+0 },
	{ 0, 1, 2, 3, 8+0, 8+1, 8+2, 8+3,
			64*8+0, 64*8+1, 64*8+2, 64*8+3, 65*8+0, 65*8+1, 65*8+2, 65*8+3,
			128*8+0, 128*8+1, 128*8+2, 128*8+3, 129*8+0, 129*8+1, 129*8+2, 129*8+3,
			192*8+0, 192*8+1, 192*8+2, 192*8+3, 193*8+0, 193*8+1, 193*8+2, 193*8+3 },
	{ 0*16, 1*16, 2*16, 3*16, 4*16, 5*16, 6*16, 7*16,
			8*16, 9*16, 10*16, 11*16, 12*16, 13*16, 14*16, 15*16,
			16*16, 17*16, 18*16, 19*16, 20*16, 21*16, 22*16, 23*16,
			24*16, 25*16, 26*16, 27*16, 28*16, 29*16, 30*16, 31*16 },
	{256*8}	/* every tile takes 256 consecutive bytes */
};
int[][] bgtilelayout =
{
	{32},{32},  /* 32*32 tiles */
	{128},    /* 128 tiles */
	{4},      /* 4 bits per pixel */
	{ 4, 0, 128*256*8+4, 128*256*8+0 },
	{ 0, 1, 2, 3, 8+0, 8+1, 8+2, 8+3,
			64*8+0, 64*8+1, 64*8+2, 64*8+3, 65*8+0, 65*8+1, 65*8+2, 65*8+3,
			128*8+0, 128*8+1, 128*8+2, 128*8+3, 129*8+0, 129*8+1, 129*8+2, 129*8+3,
			192*8+0, 192*8+1, 192*8+2, 192*8+3, 193*8+0, 193*8+1, 193*8+2, 193*8+3 },
	{ 0*16, 1*16, 2*16, 3*16, 4*16, 5*16, 6*16, 7*16,
			8*16, 9*16, 10*16, 11*16, 12*16, 13*16, 14*16, 15*16,
			16*16, 17*16, 18*16, 19*16, 20*16, 21*16, 22*16, 23*16,
			24*16, 25*16, 26*16, 27*16, 28*16, 29*16, 30*16, 31*16 },
	{256*8}	/* every tile takes 256 consecutive bytes */
};



private boolean gfxdecodeinfo() {
	GDI_ADD( REGION_GFX1, 0, charlayout,                  0, 32 );
	GDI_ADD( REGION_GFX2, 0, fgtilelayout,             32*4, 16 );
	GDI_ADD( REGION_GFX3, 0, bgtilelayout,       32*4+16*16, 16 );
	GDI_ADD( REGION_GFX4, 0, spritelayout, 32*4+16*16+16*16, 16 );
	GDI_ADD( -1 ); /* end of array */
	return true;
};

private boolean gunsmoke_gfxdecodeinfo() {
	GDI_ADD( REGION_GFX1, 0, gunsmoke_charlayout,   0, 32 );
	GDI_ADD( REGION_GFX2, 0, fgtilelayout,       32*4, 16 ); /* Tiles */
	GDI_ADD( REGION_GFX3, 0, spritelayout, 32*4+16*16, 16 ); /* Sprites */
	GDI_ADD( -1 ); /* end of array */
	return true;
};

public boolean mdrv_1943() {
	/* basic machine hardware */
	MDRV_CPU_ADD(z80, 6000000);	/* 6 MHz */
	MDRV_CPU_MEMORY(readmem(),writemem());
	MDRV_CPU_VBLANK_INT(irq0_line_hold,1);

	MDRV_CPU_ADD(z80_2, 3000000);
	MDRV_CPU_FLAGS(CPU_AUDIO_CPU);	/* 3 MHz */
	MDRV_CPU_MEMORY(sound_readmem(),sound_writemem());
	MDRV_CPU_VBLANK_INT(irq0_line_hold,4);

	/* sound hardware */
	MDRV_SOUND_ADD(ym);
	
	MDRV_FRAMES_PER_SECOND(60);
	MDRV_VBLANK_DURATION(DEFAULT_60HZ_VBLANK_DURATION);

	/* video hardware */
	MDRV_VIDEO_ATTRIBUTES(VIDEO_TYPE_RASTER);
	MDRV_SCREEN_SIZE(32*8, 32*8);
	MDRV_VISIBLE_AREA(0*8, 32*8-1, 2*8, 30*8-1);
	MDRV_GFXDECODE(gfxdecodeinfo());
	MDRV_PALETTE_LENGTH(256);
	MDRV_COLORTABLE_LENGTH(32*4+16*16+16*16+16*16);

	MDRV_PALETTE_INIT(c1943_pi);
	MDRV_VIDEO_START(c1943_vs);
	MDRV_VIDEO_UPDATE(c1943_vu);

	return true;
}

public boolean mdrv_gunsmoke() {
	/* basic machine hardware */
	MDRV_CPU_ADD(Z80, 4000000);        /* 4 MHz (?); */
	MDRV_CPU_MEMORY(gunsmoke_readmem(),gunsmoke_writemem());
	MDRV_CPU_VBLANK_INT(irq0_line_hold,1);

	MDRV_CPU_ADD(z80_2, 3000000);
	MDRV_CPU_FLAGS(CPU_AUDIO_CPU);	/* 3 MHz */
	MDRV_CPU_MEMORY(sound_readmem(),sound_writemem());
	MDRV_CPU_VBLANK_INT(irq0_line_hold,4);
	
	/* sound hardware */
	MDRV_SOUND_ADD(ym);
	
	MDRV_FRAMES_PER_SECOND(60);
	MDRV_VBLANK_DURATION(DEFAULT_60HZ_VBLANK_DURATION);

	/* video hardware */
	MDRV_VIDEO_ATTRIBUTES(VIDEO_TYPE_RASTER);
	MDRV_SCREEN_SIZE(32*8, 32*8);
	MDRV_VISIBLE_AREA(0*8, 32*8-1, 2*8, 30*8-1);
	MDRV_GFXDECODE(gunsmoke_gfxdecodeinfo());
	MDRV_PALETTE_LENGTH(256);
	MDRV_COLORTABLE_LENGTH(32*4+16*16+16*16);

	MDRV_PALETTE_INIT(gunsmoke_pi);
	MDRV_VIDEO_START(gunsmoke_vs);
	MDRV_VIDEO_UPDATE(gunsmoke_vu);

	/* sound hardware */
	//MDRV_SOUND_ADD(YM2203, ym2203_interface);
	return true;
}

private boolean rom_1943() {
	ROM_REGION( 0x30000, REGION_CPU1, 0 );	/* 64k for code + 128k for the banked ROMs images */
	ROM_LOAD( "1943.01",      0x00000, 0x08000, 0xc686cc5c );
	ROM_LOAD( "1943.02",      0x10000, 0x10000, 0xd8880a41 );
	ROM_LOAD( "1943.03",      0x20000, 0x10000, 0x3f0ee26c );

	ROM_REGION( 0x10000, REGION_CPU2, 0 );	/* 64k for the audio CPU */
	ROM_LOAD( "1943.05",      0x00000, 0x8000, 0xee2bd2d7 );

	ROM_REGION( 0x8000, REGION_GFX1, ROMREGION_DISPOSE );
	ROM_LOAD( "1943.04",      0x00000, 0x8000, 0x46cb9d3d );	/* characters */

	ROM_REGION( 0x40000, REGION_GFX2, ROMREGION_DISPOSE );
	ROM_LOAD( "1943.15",      0x00000, 0x8000, 0x6b1a0443 );	/* bg tiles */
	ROM_LOAD( "1943.16",      0x08000, 0x8000, 0x23c908c2 );
	ROM_LOAD( "1943.17",      0x10000, 0x8000, 0x46bcdd07 );
	ROM_LOAD( "1943.18",      0x18000, 0x8000, 0xe6ae7ba0 );
	ROM_LOAD( "1943.19",      0x20000, 0x8000, 0x868ababc );
	ROM_LOAD( "1943.20",      0x28000, 0x8000, 0x0917e5d4 );
	ROM_LOAD( "1943.21",      0x30000, 0x8000, 0x9bfb0d89 );
	ROM_LOAD( "1943.22",      0x38000, 0x8000, 0x04f3c274 );

	ROM_REGION( 0x10000, REGION_GFX3, ROMREGION_DISPOSE );
	ROM_LOAD( "1943.24",      0x00000, 0x8000, 0x11134036 );	/* fg tiles */
	ROM_LOAD( "1943.25",      0x08000, 0x8000, 0x092cf9c1 );

	ROM_REGION( 0x40000, REGION_GFX4, ROMREGION_DISPOSE );
	ROM_LOAD( "1943.06",      0x00000, 0x8000, 0x97acc8af );	/* sprites */
	ROM_LOAD( "1943.07",      0x08000, 0x8000, 0xd78f7197 );
	ROM_LOAD( "1943.08",      0x10000, 0x8000, 0x1a626608 );
	ROM_LOAD( "1943.09",      0x18000, 0x8000, 0x92408400 );
	ROM_LOAD( "1943.10",      0x20000, 0x8000, 0x8438a44a );
	ROM_LOAD( "1943.11",      0x28000, 0x8000, 0x6c69351d );
	ROM_LOAD( "1943.12",      0x30000, 0x8000, 0x5e7efdb7 );
	ROM_LOAD( "1943.13",      0x38000, 0x8000, 0x1143829a );

	ROM_REGION( 0x10000, REGION_GFX5, 0 );	/* tilemaps */
	ROM_LOAD( "1943.14",      0x0000, 0x8000, 0x4d3c6401 );	/* front background */
	ROM_LOAD( "1943.23",      0x8000, 0x8000, 0xa52aecbd );	/* back background */

	ROM_REGION( 0x0c00, REGION_PROMS, 0 );
	ROM_LOAD( "bmprom.01",    0x0000, 0x0100, 0x74421f18 );	/* red component */
	ROM_LOAD( "bmprom.02",    0x0100, 0x0100, 0xac27541f );	/* green component */
	ROM_LOAD( "bmprom.03",    0x0200, 0x0100, 0x251fb6ff );	/* blue component */
	ROM_LOAD( "bmprom.05",    0x0300, 0x0100, 0x206713d0 );	/* char lookup table */
	ROM_LOAD( "bmprom.10",    0x0400, 0x0100, 0x33c2491c );	/* foreground lookup table */
	ROM_LOAD( "bmprom.09",    0x0500, 0x0100, 0xaeea4af7 );	/* foreground palette bank */
	ROM_LOAD( "bmprom.12",    0x0600, 0x0100, 0xc18aa136 );	/* background lookup table */
	ROM_LOAD( "bmprom.11",    0x0700, 0x0100, 0x405aae37 );	/* background palette bank */
	ROM_LOAD( "bmprom.08",    0x0800, 0x0100, 0xc2010a9e );	/* sprite lookup table */
	ROM_LOAD( "bmprom.07",    0x0900, 0x0100, 0xb56f30c3 );	/* sprite palette bank */
	ROM_LOAD( "bmprom.04",    0x0a00, 0x0100, 0x91a8a2e1 );	/* priority encoder / palette selector (not used); */
	ROM_LOAD( "bmprom.06",    0x0b00, 0x0100, 0x0eaf5158 );	/* video timing (not used); */
	return true;
}

private boolean rom_1943j() {
	ROM_REGION( 0x30000, REGION_CPU1, 0 );	/* 64k for code + 128k for the banked ROMs images */
	ROM_LOAD( "1943jap.001",  0x00000, 0x08000, 0xf6935937 );
	ROM_LOAD( "1943jap.002",  0x10000, 0x10000, 0xaf971575 );
	ROM_LOAD( "1943jap.003",  0x20000, 0x10000, 0x300ec713 );

	ROM_REGION( 0x10000, REGION_CPU2, 0 );	/* 64k for the audio CPU */
	ROM_LOAD( "1943.05",      0x00000, 0x8000, 0xee2bd2d7 );

	ROM_REGION( 0x8000, REGION_GFX1, ROMREGION_DISPOSE );
	ROM_LOAD( "1943.04",      0x00000, 0x8000, 0x46cb9d3d );	/* characters */

	ROM_REGION( 0x40000, REGION_GFX2, ROMREGION_DISPOSE );
	ROM_LOAD( "1943.15",      0x00000, 0x8000, 0x6b1a0443 );	/* bg tiles */
	ROM_LOAD( "1943.16",      0x08000, 0x8000, 0x23c908c2 );
	ROM_LOAD( "1943.17",      0x10000, 0x8000, 0x46bcdd07 );
	ROM_LOAD( "1943.18",      0x18000, 0x8000, 0xe6ae7ba0 );
	ROM_LOAD( "1943.19",      0x20000, 0x8000, 0x868ababc );
	ROM_LOAD( "1943.20",      0x28000, 0x8000, 0x0917e5d4 );
	ROM_LOAD( "1943.21",      0x30000, 0x8000, 0x9bfb0d89 );
	ROM_LOAD( "1943.22",      0x38000, 0x8000, 0x04f3c274 );

	ROM_REGION( 0x10000, REGION_GFX3, ROMREGION_DISPOSE );
	ROM_LOAD( "1943.24",      0x00000, 0x8000, 0x11134036 );	/* fg tiles */
	ROM_LOAD( "1943.25",      0x08000, 0x8000, 0x092cf9c1 );

	ROM_REGION( 0x40000, REGION_GFX4, ROMREGION_DISPOSE );
	ROM_LOAD( "1943.06",      0x00000, 0x8000, 0x97acc8af );	/* sprites */
	ROM_LOAD( "1943.07",      0x08000, 0x8000, 0xd78f7197 );
	ROM_LOAD( "1943.08",      0x10000, 0x8000, 0x1a626608 );
	ROM_LOAD( "1943.09",      0x18000, 0x8000, 0x92408400 );
	ROM_LOAD( "1943.10",      0x20000, 0x8000, 0x8438a44a );
	ROM_LOAD( "1943.11",      0x28000, 0x8000, 0x6c69351d );
	ROM_LOAD( "1943.12",      0x30000, 0x8000, 0x5e7efdb7 );
	ROM_LOAD( "1943.13",      0x38000, 0x8000, 0x1143829a );

	ROM_REGION( 0x10000, REGION_GFX5, 0 );	/* tilemaps */
	ROM_LOAD( "1943.14",      0x0000, 0x8000, 0x4d3c6401 );	/* front background */
	ROM_LOAD( "1943.23",      0x8000, 0x8000, 0xa52aecbd );	/* back background */

	ROM_REGION( 0x0c00, REGION_PROMS, 0 );
	ROM_LOAD( "bmprom.01",    0x0000, 0x0100, 0x74421f18 );	/* red component */
	ROM_LOAD( "bmprom.02",    0x0100, 0x0100, 0xac27541f );	/* green component */
	ROM_LOAD( "bmprom.03",    0x0200, 0x0100, 0x251fb6ff );	/* blue component */
	ROM_LOAD( "bmprom.05",    0x0300, 0x0100, 0x206713d0 );	/* char lookup table */
	ROM_LOAD( "bmprom.10",    0x0400, 0x0100, 0x33c2491c );	/* foreground lookup table */
	ROM_LOAD( "bmprom.09",    0x0500, 0x0100, 0xaeea4af7 );	/* foreground palette bank */
	ROM_LOAD( "bmprom.12",    0x0600, 0x0100, 0xc18aa136 );	/* background lookup table */
	ROM_LOAD( "bmprom.11",    0x0700, 0x0100, 0x405aae37 );	/* background palette bank */
	ROM_LOAD( "bmprom.08",    0x0800, 0x0100, 0xc2010a9e );	/* sprite lookup table */
	ROM_LOAD( "bmprom.07",    0x0900, 0x0100, 0xb56f30c3 );	/* sprite palette bank */
	ROM_LOAD( "bmprom.04",    0x0a00, 0x0100, 0x91a8a2e1 );	/* priority encoder / palette selector (not used); */
	ROM_LOAD( "bmprom.06",    0x0b00, 0x0100, 0x0eaf5158 );	/* video timing (not used); */
	return true;
}

private boolean rom_1943kai() {
	ROM_REGION( 0x30000, REGION_CPU1, 0 );	/* 64k for code + 128k for the banked ROMs images */
	ROM_LOAD( "1943kai.01",   0x00000, 0x08000, 0x7d2211db );
	ROM_LOAD( "1943kai.02",   0x10000, 0x10000, 0x2ebbc8c5 );
	ROM_LOAD( "1943kai.03",   0x20000, 0x10000, 0x475a6ac5 );

	ROM_REGION( 0x10000, REGION_CPU2, 0 );	/* 64k for the audio CPU */
	ROM_LOAD( "1943kai.05",   0x00000, 0x8000, 0x25f37957 );

	ROM_REGION( 0x8000, REGION_GFX1, ROMREGION_DISPOSE );
	ROM_LOAD( "1943kai.04",   0x00000, 0x8000, 0x884a8692 );	/* characters */

	ROM_REGION( 0x40000, REGION_GFX2, ROMREGION_DISPOSE );
	ROM_LOAD( "1943kai.15",   0x00000, 0x8000, 0x6b1a0443 );	/* bg tiles */
	ROM_LOAD( "1943kai.16",   0x08000, 0x8000, 0x9416fe0d );
	ROM_LOAD( "1943kai.17",   0x10000, 0x8000, 0x3d5acab9 );
	ROM_LOAD( "1943kai.18",   0x18000, 0x8000, 0x7b62da1d );
	ROM_LOAD( "1943kai.19",   0x20000, 0x8000, 0x868ababc );
	ROM_LOAD( "1943kai.20",   0x28000, 0x8000, 0xb90364c1 );
	ROM_LOAD( "1943kai.21",   0x30000, 0x8000, 0x8c7fe74a );
	ROM_LOAD( "1943kai.22",   0x38000, 0x8000, 0xd5ef8a0e );

	ROM_REGION( 0x10000, REGION_GFX3, ROMREGION_DISPOSE );
	ROM_LOAD( "1943kai.24",   0x00000, 0x8000, 0xbf186ef2 );	/* fg tiles */
	ROM_LOAD( "1943kai.25",   0x08000, 0x8000, 0xa755faf1 );

	ROM_REGION( 0x40000, REGION_GFX4, ROMREGION_DISPOSE );
	ROM_LOAD( "1943kai.06",   0x00000, 0x8000, 0x5f7e38b3 );	/* sprites */
	ROM_LOAD( "1943kai.07",   0x08000, 0x8000, 0xff3751fd );
	ROM_LOAD( "1943kai.08",   0x10000, 0x8000, 0x159d51bd );
	ROM_LOAD( "1943kai.09",   0x18000, 0x8000, 0x8683e3d2 );
	ROM_LOAD( "1943kai.10",   0x20000, 0x8000, 0x1e0d9571 );
	ROM_LOAD( "1943kai.11",   0x28000, 0x8000, 0xf1fc5ee1 );
	ROM_LOAD( "1943kai.12",   0x30000, 0x8000, 0x0f50c001 );
	ROM_LOAD( "1943kai.13",   0x38000, 0x8000, 0xfd1acf8e );

	ROM_REGION( 0x10000, REGION_GFX5, 0 );	/* tilemaps */
	ROM_LOAD( "1943kai.14",   0x0000, 0x8000, 0xcf0f5a53 );	/* front background */
	ROM_LOAD( "1943kai.23",   0x8000, 0x8000, 0x17f77ef9 );	/* back background */

	ROM_REGION( 0x0c00, REGION_PROMS, 0 );
	ROM_LOAD( "bmk01.bin",    0x0000, 0x0100, 0xe001ea33 );	/* red component */
	ROM_LOAD( "bmk02.bin",    0x0100, 0x0100, 0xaf34d91a );	/* green component */
	ROM_LOAD( "bmk03.bin",    0x0200, 0x0100, 0x43e9f6ef );	/* blue component */
	ROM_LOAD( "bmk05.bin",    0x0300, 0x0100, 0x41878934 );	/* char lookup table */
	ROM_LOAD( "bmk10.bin",    0x0400, 0x0100, 0xde44b748 );	/* foreground lookup table */
	ROM_LOAD( "bmk09.bin",    0x0500, 0x0100, 0x59ea57c0 );	/* foreground palette bank */
	ROM_LOAD( "bmk12.bin",    0x0600, 0x0100, 0x8765f8b0 );	/* background lookup table */
	ROM_LOAD( "bmk11.bin",    0x0700, 0x0100, 0x87a8854e );	/* background palette bank */
	ROM_LOAD( "bmk08.bin",    0x0800, 0x0100, 0xdad17e2d );	/* sprite lookup table */
	ROM_LOAD( "bmk07.bin",    0x0900, 0x0100, 0x76307f8d );	/* sprite palette bank */
	ROM_LOAD( "bmprom.04",    0x0a00, 0x0100, 0x91a8a2e1 );	/* priority encoder / palette selector (not used); */
	ROM_LOAD( "bmprom.06",    0x0b00, 0x0100, 0x0eaf5158 );	/* video timing (not used); */
	return true;
}

private boolean rom_gunsmoke() {
	ROM_REGION( 0x20000, REGION_CPU1, 0 );     /* 2*64k for code */
	ROM_LOAD( "09n_gs03.bin", 0x00000, 0x8000, 0x40a06cef ); /* Code 0000-7fff */
	ROM_LOAD( "10n_gs04.bin", 0x10000, 0x8000, 0x8d4b423f ); /* Paged code */
	ROM_LOAD( "12n_gs05.bin", 0x18000, 0x8000, 0x2b5667fb ); /* Paged code */

	ROM_REGION( 0x10000, REGION_CPU2, 0 );	/* 64k for the audio CPU */
	ROM_LOAD( "14h_gs02.bin", 0x00000, 0x8000, 0xcd7a2c38 );

	ROM_REGION( 0x04000, REGION_GFX1, ROMREGION_DISPOSE );
	ROM_LOAD( "11f_gs01.bin", 0x00000, 0x4000, 0xb61ece9b ); /* Characters */

	ROM_REGION( 0x40000, REGION_GFX2, ROMREGION_DISPOSE );
	ROM_LOAD( "06c_gs13.bin", 0x00000, 0x8000, 0xf6769fc5 ); /* 32x32 tiles planes 2-3 */
	ROM_LOAD( "05c_gs12.bin", 0x08000, 0x8000, 0xd997b78c );
	ROM_LOAD( "04c_gs11.bin", 0x10000, 0x8000, 0x125ba58e );
	ROM_LOAD( "02c_gs10.bin", 0x18000, 0x8000, 0xf469c13c );
	ROM_LOAD( "06a_gs09.bin", 0x20000, 0x8000, 0x539f182d ); /* 32x32 tiles planes 0-1 */
	ROM_LOAD( "05a_gs08.bin", 0x28000, 0x8000, 0xe87e526d );
	ROM_LOAD( "04a_gs07.bin", 0x30000, 0x8000, 0x4382c0d2 );
	ROM_LOAD( "02a_gs06.bin", 0x38000, 0x8000, 0x4cafe7a6 );

	ROM_REGION( 0x40000, REGION_GFX3, ROMREGION_DISPOSE );
	ROM_LOAD( "06n_gs22.bin", 0x00000, 0x8000, 0xdc9c508c ); /* Sprites planes 2-3 */
	ROM_LOAD( "04n_gs21.bin", 0x08000, 0x8000, 0x68883749 ); /* Sprites planes 2-3 */
	ROM_LOAD( "03n_gs20.bin", 0x10000, 0x8000, 0x0be932ed ); /* Sprites planes 2-3 */
	ROM_LOAD( "01n_gs19.bin", 0x18000, 0x8000, 0x63072f93 ); /* Sprites planes 2-3 */
	ROM_LOAD( "06l_gs18.bin", 0x20000, 0x8000, 0xf69a3c7c ); /* Sprites planes 0-1 */
	ROM_LOAD( "04l_gs17.bin", 0x28000, 0x8000, 0x4e98562a ); /* Sprites planes 0-1 */
	ROM_LOAD( "03l_gs16.bin", 0x30000, 0x8000, 0x0d99c3b3 ); /* Sprites planes 0-1 */
	ROM_LOAD( "01l_gs15.bin", 0x38000, 0x8000, 0x7f14270e ); /* Sprites planes 0-1 */

	ROM_REGION( 0x8000, REGION_GFX4, 0 );	/* background tilemaps */
	ROM_LOAD( "11c_gs14.bin", 0x00000, 0x8000, 0x0af4f7eb );

	ROM_REGION( 0x0a00, REGION_PROMS, 0 );
	ROM_LOAD( "03b_g-01.bin", 0x0000, 0x0100, 0x02f55589 );	/* red component */
	ROM_LOAD( "04b_g-02.bin", 0x0100, 0x0100, 0xe1e36dd9 );	/* green component */
	ROM_LOAD( "05b_g-03.bin", 0x0200, 0x0100, 0x989399c0 );	/* blue component */
	ROM_LOAD( "09d_g-04.bin", 0x0300, 0x0100, 0x906612b5 );	/* char lookup table */
	ROM_LOAD( "14a_g-06.bin", 0x0400, 0x0100, 0x4a9da18b );	/* tile lookup table */
	ROM_LOAD( "15a_g-07.bin", 0x0500, 0x0100, 0xcb9394fc );	/* tile palette bank */
	ROM_LOAD( "09f_g-09.bin", 0x0600, 0x0100, 0x3cee181e );	/* sprite lookup table */
	ROM_LOAD( "08f_g-08.bin", 0x0700, 0x0100, 0xef91cdd2 );	/* sprite palette bank */
	ROM_LOAD( "02j_g-10.bin", 0x0800, 0x0100, 0x0eaf5158 );	/* video timing (not used); */
	ROM_LOAD( "01f_g-05.bin", 0x0900, 0x0100, 0x25c90c2a );	/* priority? (not used); */
	return true;
}

private boolean rom_gunsmokj() {
	ROM_REGION( 0x20000, REGION_CPU1, 0 );     /* 2*64k for code */
	ROM_LOAD( "gs03_9n.rom",  0x00000, 0x8000, 0xb56b5df6 ); /* Code 0000-7fff */
	ROM_LOAD( "10n_gs04.bin", 0x10000, 0x8000, 0x8d4b423f ); /* Paged code */
	ROM_LOAD( "12n_gs05.bin", 0x18000, 0x8000, 0x2b5667fb ); /* Paged code */

	ROM_REGION( 0x10000, REGION_CPU2, 0 );	/* 64k for the audio CPU */
	ROM_LOAD( "14h_gs02.bin", 0x00000, 0x8000, 0xcd7a2c38 );

	ROM_REGION( 0x04000, REGION_GFX1, ROMREGION_DISPOSE );
	ROM_LOAD( "11f_gs01.bin", 0x00000, 0x4000, 0xb61ece9b ); /* Characters */

	ROM_REGION( 0x40000, REGION_GFX2, ROMREGION_DISPOSE );
	ROM_LOAD( "06c_gs13.bin", 0x00000, 0x8000, 0xf6769fc5 ); /* 32x32 tiles planes 2-3 */
	ROM_LOAD( "05c_gs12.bin", 0x08000, 0x8000, 0xd997b78c );
	ROM_LOAD( "04c_gs11.bin", 0x10000, 0x8000, 0x125ba58e );
	ROM_LOAD( "02c_gs10.bin", 0x18000, 0x8000, 0xf469c13c );
	ROM_LOAD( "06a_gs09.bin", 0x20000, 0x8000, 0x539f182d ); /* 32x32 tiles planes 0-1 */
	ROM_LOAD( "05a_gs08.bin", 0x28000, 0x8000, 0xe87e526d );
	ROM_LOAD( "04a_gs07.bin", 0x30000, 0x8000, 0x4382c0d2 );
	ROM_LOAD( "02a_gs06.bin", 0x38000, 0x8000, 0x4cafe7a6 );

	ROM_REGION( 0x40000, REGION_GFX3, ROMREGION_DISPOSE );
	ROM_LOAD( "06n_gs22.bin", 0x00000, 0x8000, 0xdc9c508c ); /* Sprites planes 2-3 */
	ROM_LOAD( "04n_gs21.bin", 0x08000, 0x8000, 0x68883749 ); /* Sprites planes 2-3 */
	ROM_LOAD( "03n_gs20.bin", 0x10000, 0x8000, 0x0be932ed ); /* Sprites planes 2-3 */
	ROM_LOAD( "01n_gs19.bin", 0x18000, 0x8000, 0x63072f93 ); /* Sprites planes 2-3 */
	ROM_LOAD( "06l_gs18.bin", 0x20000, 0x8000, 0xf69a3c7c ); /* Sprites planes 0-1 */
	ROM_LOAD( "04l_gs17.bin", 0x28000, 0x8000, 0x4e98562a ); /* Sprites planes 0-1 */
	ROM_LOAD( "03l_gs16.bin", 0x30000, 0x8000, 0x0d99c3b3 ); /* Sprites planes 0-1 */
	ROM_LOAD( "01l_gs15.bin", 0x38000, 0x8000, 0x7f14270e ); /* Sprites planes 0-1 */

	ROM_REGION( 0x8000, REGION_GFX4, 0 );	/* background tilemaps */
	ROM_LOAD( "11c_gs14.bin", 0x00000, 0x8000, 0x0af4f7eb );

	ROM_REGION( 0x0a00, REGION_PROMS, 0 );
	ROM_LOAD( "03b_g-01.bin", 0x0000, 0x0100, 0x02f55589 );	/* red component */
	ROM_LOAD( "04b_g-02.bin", 0x0100, 0x0100, 0xe1e36dd9 );	/* green component */
	ROM_LOAD( "05b_g-03.bin", 0x0200, 0x0100, 0x989399c0 );	/* blue component */
	ROM_LOAD( "09d_g-04.bin", 0x0300, 0x0100, 0x906612b5 );	/* char lookup table */
	ROM_LOAD( "14a_g-06.bin", 0x0400, 0x0100, 0x4a9da18b );	/* tile lookup table */
	ROM_LOAD( "15a_g-07.bin", 0x0500, 0x0100, 0xcb9394fc );	/* tile palette bank */
	ROM_LOAD( "09f_g-09.bin", 0x0600, 0x0100, 0x3cee181e );	/* sprite lookup table */
	ROM_LOAD( "08f_g-08.bin", 0x0700, 0x0100, 0xef91cdd2 );	/* sprite palette bank */
	ROM_LOAD( "02j_g-10.bin", 0x0800, 0x0100, 0x0eaf5158 );	/* video timing (not used) */
	ROM_LOAD( "01f_g-05.bin", 0x0900, 0x0100, 0x25c90c2a );	/* priority? (not used) */
	return true;
}

private boolean rom_gunsmoku() {
	ROM_REGION( 0x20000, REGION_CPU1, 0 );     /* 2*64k for code */
	ROM_LOAD( "9n_gs03.bin",  0x00000, 0x8000, 0x592f211b ); /* Code 0000-7fff */
	ROM_LOAD( "10n_gs04.bin", 0x10000, 0x8000, 0x8d4b423f ); /* Paged code */
	ROM_LOAD( "12n_gs05.bin", 0x18000, 0x8000, 0x2b5667fb ); /* Paged code */

	ROM_REGION( 0x10000, REGION_CPU2, 0 );	/* 64k for the audio CPU */
	ROM_LOAD( "14h_gs02.bin", 0x00000, 0x8000, 0xcd7a2c38 );

	ROM_REGION( 0x04000, REGION_GFX1, ROMREGION_DISPOSE );
	ROM_LOAD( "11f_gs01.bin", 0x00000, 0x4000, 0xb61ece9b ); /* Characters */

	ROM_REGION( 0x40000, REGION_GFX2, ROMREGION_DISPOSE );
	ROM_LOAD( "06c_gs13.bin", 0x00000, 0x8000, 0xf6769fc5 ); /* 32x32 tiles planes 2-3 */
	ROM_LOAD( "05c_gs12.bin", 0x08000, 0x8000, 0xd997b78c );
	ROM_LOAD( "04c_gs11.bin", 0x10000, 0x8000, 0x125ba58e );
	ROM_LOAD( "02c_gs10.bin", 0x18000, 0x8000, 0xf469c13c );
	ROM_LOAD( "06a_gs09.bin", 0x20000, 0x8000, 0x539f182d ); /* 32x32 tiles planes 0-1 */
	ROM_LOAD( "05a_gs08.bin", 0x28000, 0x8000, 0xe87e526d );
	ROM_LOAD( "04a_gs07.bin", 0x30000, 0x8000, 0x4382c0d2 );
	ROM_LOAD( "02a_gs06.bin", 0x38000, 0x8000, 0x4cafe7a6 );

	ROM_REGION( 0x40000, REGION_GFX3, ROMREGION_DISPOSE );
	ROM_LOAD( "06n_gs22.bin", 0x00000, 0x8000, 0xdc9c508c ); /* Sprites planes 2-3 */
	ROM_LOAD( "04n_gs21.bin", 0x08000, 0x8000, 0x68883749 ); /* Sprites planes 2-3 */
	ROM_LOAD( "03n_gs20.bin", 0x10000, 0x8000, 0x0be932ed ); /* Sprites planes 2-3 */
	ROM_LOAD( "01n_gs19.bin", 0x18000, 0x8000, 0x63072f93 ); /* Sprites planes 2-3 */
	ROM_LOAD( "06l_gs18.bin", 0x20000, 0x8000, 0xf69a3c7c ); /* Sprites planes 0-1 */
	ROM_LOAD( "04l_gs17.bin", 0x28000, 0x8000, 0x4e98562a ); /* Sprites planes 0-1 */
	ROM_LOAD( "03l_gs16.bin", 0x30000, 0x8000, 0x0d99c3b3 ); /* Sprites planes 0-1 */
	ROM_LOAD( "01l_gs15.bin", 0x38000, 0x8000, 0x7f14270e ); /* Sprites planes 0-1 */

	ROM_REGION( 0x8000, REGION_GFX4, 0 );	/* background tilemaps */
	ROM_LOAD( "11c_gs14.bin", 0x00000, 0x8000, 0x0af4f7eb );

	ROM_REGION( 0x0a00, REGION_PROMS, 0 );
	ROM_LOAD( "03b_g-01.bin", 0x0000, 0x0100, 0x02f55589 );	/* red component */
	ROM_LOAD( "04b_g-02.bin", 0x0100, 0x0100, 0xe1e36dd9 );	/* green component */
	ROM_LOAD( "05b_g-03.bin", 0x0200, 0x0100, 0x989399c0 );	/* blue component */
	ROM_LOAD( "09d_g-04.bin", 0x0300, 0x0100, 0x906612b5 );	/* char lookup table */
	ROM_LOAD( "14a_g-06.bin", 0x0400, 0x0100, 0x4a9da18b );	/* tile lookup table */
	ROM_LOAD( "15a_g-07.bin", 0x0500, 0x0100, 0xcb9394fc );	/* tile palette bank */
	ROM_LOAD( "09f_g-09.bin", 0x0600, 0x0100, 0x3cee181e );	/* sprite lookup table */
	ROM_LOAD( "08f_g-08.bin", 0x0700, 0x0100, 0xef91cdd2 );	/* sprite palette bank */
	ROM_LOAD( "02j_g-10.bin", 0x0800, 0x0100, 0x0eaf5158 );	/* video timing (not used) */
	ROM_LOAD( "01f_g-05.bin", 0x0900, 0x0100, 0x25c90c2a );	/* priority? (not used) */
	return true;
}

private boolean rom_gunsmoka() {
	ROM_REGION( 0x20000, REGION_CPU1, 0 );     /* 2*64k for code */
	ROM_LOAD( "gs03.9n",      0x00000, 0x8000, 0x51dc3f76 ); /* Code 0000-7fff */
	ROM_LOAD( "gs04.10n",     0x10000, 0x8000, 0x5ecf31b8 ); /* Paged code */
	ROM_LOAD( "gs05.12n",     0x18000, 0x8000, 0x1c9aca13 ); /* Paged code */

	ROM_REGION( 0x10000, REGION_CPU2, 0 );	/* 64k for the audio CPU */
	ROM_LOAD( "14h_gs02.bin", 0x00000, 0x8000, 0xcd7a2c38 );

	ROM_REGION( 0x04000, REGION_GFX1, ROMREGION_DISPOSE );
	ROM_LOAD( "11f_gs01.bin", 0x00000, 0x4000, 0xb61ece9b ); /* Characters */

	ROM_REGION( 0x40000, REGION_GFX2, ROMREGION_DISPOSE );
	ROM_LOAD( "06c_gs13.bin", 0x00000, 0x8000, 0xf6769fc5 ); /* 32x32 tiles planes 2-3 */
	ROM_LOAD( "05c_gs12.bin", 0x08000, 0x8000, 0xd997b78c );
	ROM_LOAD( "04c_gs11.bin", 0x10000, 0x8000, 0x125ba58e );
	ROM_LOAD( "02c_gs10.bin", 0x18000, 0x8000, 0xf469c13c );
	ROM_LOAD( "06a_gs09.bin", 0x20000, 0x8000, 0x539f182d ); /* 32x32 tiles planes 0-1 */
	ROM_LOAD( "05a_gs08.bin", 0x28000, 0x8000, 0xe87e526d );
	ROM_LOAD( "04a_gs07.bin", 0x30000, 0x8000, 0x4382c0d2 );
	ROM_LOAD( "02a_gs06.bin", 0x38000, 0x8000, 0x4cafe7a6 );

	ROM_REGION( 0x40000, REGION_GFX3, ROMREGION_DISPOSE );
	ROM_LOAD( "06n_gs22.bin", 0x00000, 0x8000, 0xdc9c508c ); /* Sprites planes 2-3 */
	ROM_LOAD( "04n_gs21.bin", 0x08000, 0x8000, 0x68883749 ); /* Sprites planes 2-3 */
	ROM_LOAD( "03n_gs20.bin", 0x10000, 0x8000, 0x0be932ed ); /* Sprites planes 2-3 */
	ROM_LOAD( "01n_gs19.bin", 0x18000, 0x8000, 0x63072f93 ); /* Sprites planes 2-3 */
	ROM_LOAD( "06l_gs18.bin", 0x20000, 0x8000, 0xf69a3c7c ); /* Sprites planes 0-1 */
	ROM_LOAD( "04l_gs17.bin", 0x28000, 0x8000, 0x4e98562a ); /* Sprites planes 0-1 */
	ROM_LOAD( "03l_gs16.bin", 0x30000, 0x8000, 0x0d99c3b3 ); /* Sprites planes 0-1 */
	ROM_LOAD( "01l_gs15.bin", 0x38000, 0x8000, 0x7f14270e ); /* Sprites planes 0-1 */

	ROM_REGION( 0x8000, REGION_GFX4, 0 );	/* background tilemaps */
	ROM_LOAD( "11c_gs14.bin", 0x00000, 0x8000, 0x0af4f7eb );

	ROM_REGION( 0x0a00, REGION_PROMS, 0 );
	ROM_LOAD( "03b_g-01.bin", 0x0000, 0x0100, 0x02f55589 );	/* red component */
	ROM_LOAD( "04b_g-02.bin", 0x0100, 0x0100, 0xe1e36dd9 );	/* green component */
	ROM_LOAD( "05b_g-03.bin", 0x0200, 0x0100, 0x989399c0 );	/* blue component */
	ROM_LOAD( "09d_g-04.bin", 0x0300, 0x0100, 0x906612b5 );	/* char lookup table */
	ROM_LOAD( "14a_g-06.bin", 0x0400, 0x0100, 0x4a9da18b );	/* tile lookup table */
	ROM_LOAD( "15a_g-07.bin", 0x0500, 0x0100, 0xcb9394fc );	/* tile palette bank */
	ROM_LOAD( "09f_g-09.bin", 0x0600, 0x0100, 0x3cee181e );	/* sprite lookup table */
	ROM_LOAD( "08f_g-08.bin", 0x0700, 0x0100, 0xef91cdd2 );	/* sprite palette bank */
	ROM_LOAD( "02j_g-10.bin", 0x0800, 0x0100, 0x0eaf5158 );	/* video timing (not used) */
	ROM_LOAD( "01f_g-05.bin", 0x0900, 0x0100, 0x25c90c2a );	/* priority? (not used) */
	return true;
}


public Machine getMachine(URL url, String name) {
	super.getMachine(url,name);
	super.setVideoEmulator(v);

	if (name.equals("1943")) {
		GAME( 1987, rom_1943(), 		    0, mdrv_1943(), 	 ipt_1943(), 	 0, ROT270, "Capcom", "1943 - The Battle of Midway (US)" );
	} else if (name.equals("1943j")) {
		GAME( 1987, rom_1943j(), 	   "1943", mdrv_1943(), 	 ipt_1943(), 	 0, ROT270, "Capcom", "1943 - The Battle of Midway (Japan)" );
	} else if (name.equals("1943kai")) {
		GAME( 1987, rom_1943kai(), 		    0, mdrv_1943(), 	 ipt_1943(), 	 0, ROT270, "Capcom", "1943 Kai" );
		
	} else if (name.equals("gunsmoke")) {
		GAME( 1985, rom_gunsmoke(), 	    0, mdrv_gunsmoke(), ipt_gunsmoke(), 0, ROT270, "Capcom", "Gun.Smoke (World)" );
	} else if (name.equals("gunsmokj")) {
		GAME( 1985, rom_gunsmokj(),"gunsmoke", mdrv_gunsmoke(), ipt_gunsmoke(), 0, ROT270, "Capcom", "Gun.Smoke (Japan)" );
	} else if (name.equals("gunsmoku")) {
		GAME( 1985, rom_gunsmoku(),"gunsmoke", mdrv_gunsmoke(), ipt_gunsmoke(), 0, ROT270, "Capcom (Romstar license)", "Gun.Smoke (US set 1)" );
	} else if (name.equals("gunsmoka")) {
		GAME( 1985, rom_gunsmoka(),"gunsmoke", mdrv_gunsmoke(), ipt_gunsmoke(), 0, ROT270, "Capcom (Romstar license)", "Gun.Smoke (US set 2)" );
	}

	m.init(md);

	return (Machine)m;
}

}