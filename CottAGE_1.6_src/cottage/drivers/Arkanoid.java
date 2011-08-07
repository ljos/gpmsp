/*
CottAGE - the Arcade Generic Emulator in Java

Java driver by Gollum
*/

/***************************************************************************

	Arkanoid driver (Preliminary)

	Japanese version support cocktail mode (DSW #7), the others don't.

	Here are the versions we have:

	arkanoid	World version, probably an earlier revision
	arknoidu	USA version, probably a later revision; There has been code
			    inserted, NOT patched, so I don't think it's a bootleg
				The 68705 code for this one was not available; I made it up from
				the World version changing the level data pointer table.
    arkatour    Tournament version
				The 68705 code for this one was not available; I made it up from
				the World version changing the level data pointer table.
	arknoidj	Japanese version with level selector.
				The 68705 code for this one was not available; I made it up from
				the World version changing the level data pointer table.
	arkbl2		Bootleg of the early Japanese version.
				The only difference is that the warning text has been replaced
				by "WAIT"
				ROM	E2.6F should be identical to the real Japanese one.
				(It only differs in the country byte from A75_11.ROM)
				This version works fine with the real MCU ROM
	arkatayt	Another bootleg of the early Japanese one, more heavily modified
	arkblock	Another bootleg of the early Japanese one, more heavily modified
	arkbloc2	Another bootleg
	arkbl3   	Another bootleg of the early Japanese one, more heavily modified
	paddle2   	Another bootleg of the early Japanese one, more heavily modified
	arkangc		Game Corporation bootleg with level selector


	Most if not all Arkanoid sets have a bug in their game code. It occurs on the
	final level where the player has to dodge falling objects. The bug resides in
	the collision detection routine which sometimes reads from unmapped addresses
	above $F000. For these addresses it is vital to read zero values, or else the
	player will die for no reason.


***************************************************************************/

package cottage.drivers;

import java.net.URL;

import jef.machine.Machine;
import jef.map.InterruptHandler;
import jef.map.ReadHandler;
import jef.map.WriteHandler;
import jef.sound.chip.AY8910;
import jef.video.Vh_refresh;
import jef.video.Vh_start;

import cottage.mame.MAMEDriver;

public class Arkanoid extends MAMEDriver {
	
	AY8910	ay8910 = new AY8910 ( 	1, 1500000 );	/* 1.5 MHz ???? */

cottage.vidhrdw.Arkanoid v = new cottage.vidhrdw.Arkanoid();
WriteHandler arkanoid_d008_w = v.arkanoid_d008_w();
Vh_start generic_vs = (Vh_start)v;
Vh_refresh arkanoid_vu = (Vh_refresh)v;
WriteHandler videoram_w = v.videoram_w();

jef.machine.BasicMachine m = new jef.machine.BasicMachine();
InterruptHandler irq0_line_hold = m.irq0_line_hold();
ReadHandler arkanoid_input_2_r = new Arkanoid_input_2_r();

public class Arkanoid_input_2_r implements ReadHandler {
	public int read(int offset) {
		if (v.arkanoid_paddle_select != 0)
		{
			return input_port_3_r.read(offset);
		}
		else
		{
			return input_port_2_r.read(offset);
		}
	}
}

private boolean boot_readmem() {
	MR_START( 0x0000, 0xbfff, MRA_ROM );
	MR_ADD( 0xc000, 0xc7ff, MRA_RAM );
	//MR_ADD( 0xd001, 0xd001, ay8910.AY8910_read_port_0_r() );
	MR_ADD( 0xd001, 0xd001, input_port_4_r );
	MR_ADD( 0xd00c, 0xd00c, input_port_0_r );
	MR_ADD( 0xd010, 0xd010, input_port_1_r );
	MR_ADD( 0xd018, 0xd018, arkanoid_input_2_r );
	MR_ADD( 0xe000, 0xefff, MRA_RAM );
	MR_ADD( 0xf000, 0xffff, MRA_NOP );	/* fixes instant death in final level */
	return true;
}

private boolean boot_writemem() {
	MW_START( 0x0000, 0xbfff, MWA_ROM );
	MW_ADD( 0xc000, 0xc7ff, MWA_RAM );
	MW_ADD( 0xd000, 0xd000, ay8910.AY8910_control_port_0_w() );
	MW_ADD( 0xd001, 0xd001, ay8910.AY8910_write_port_0_w() );
	MW_ADD( 0xd008, 0xd008, arkanoid_d008_w );	/* gfx bank, flip screen etc. */
	//MW_ADD( 0xd010, 0xd010, watchdog_reset_w );
	MW_ADD( 0xd018, 0xd018, MWA_NOP );
	MW_ADD( 0xe000, 0xe7ff, videoram_w, videoram, videoram_size );
	MW_ADD( 0xe800, 0xe83f, MWA_RAM, spriteram, spriteram_size );
	MW_ADD( 0xe840, 0xefff, MWA_RAM );
	return true;
}

/* These are the input ports of the real Japanese ROM set                        */
/* 'Block' uses the these ones as well.	The Tayto bootleg is different			 */
/*  in coinage and # of lives.                    								 */

private boolean ipt_arknoidj() {
	PORT_START();	/* IN0 */
	PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_START1 );
	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_START2 );
	PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_SERVICE1 );
	PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_TILT );
	PORT_BIT( 0x10, IP_ACTIVE_HIGH, IPT_COIN1 );
	PORT_BIT( 0x20, IP_ACTIVE_HIGH, IPT_COIN2 );
	PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_SPECIAL );	/* input from the 68705, some bootlegs need it to be 1 */
	PORT_BIT( 0x80, IP_ACTIVE_HIGH, IPT_SPECIAL );	/* input from the 68705 */

	PORT_START();	/* IN1 */
	PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_BUTTON1 );
	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_UNKNOWN );
	PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_COCKTAIL );
	PORT_BIT( 0xf8, IP_ACTIVE_LOW, IPT_UNKNOWN );

	PORT_START();      /* IN2 - spinner (multiplexed for player 1 and 2) */
	PORT_ANALOG( 0xff, 0x00, IPT_DIAL, 30, 15, 0, 0);

	PORT_START();      /* IN3 - spinner Player 2  */
	PORT_ANALOG( 0xff, 0x00, IPT_DIAL | IPF_COCKTAIL, 30, 15, 0, 0);

	PORT_START();	/* DSW1 */
	PORT_DIPNAME( 0x01, 0x00, "Allow Continue" );
	PORT_DIPSETTING(    0x01, DEF_STR2( No ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( Yes ) );
	PORT_DIPNAME( 0x02, 0x02, DEF_STR2( Flip_Screen ) );
	PORT_DIPSETTING(    0x02, DEF_STR2( Off ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( On ) );
	PORT_SERVICE( 0x04, IP_ACTIVE_LOW );
	PORT_DIPNAME( 0x08, 0x08, DEF_STR2( Difficulty ) );
	PORT_DIPSETTING(    0x08, "Easy" );
	PORT_DIPSETTING(    0x00, "Hard" );
	PORT_DIPNAME( 0x10, 0x10, DEF_STR2( Bonus_Life ) );
	PORT_DIPSETTING(    0x10, "20K, 60K and every 60K" );
	PORT_DIPSETTING(    0x00, "20K only" );
	PORT_DIPNAME( 0x20, 0x20, DEF_STR2( Lives ) );
	PORT_DIPSETTING(    0x20, "3" );
	PORT_DIPSETTING(    0x00, "5" );
	PORT_DIPNAME( 0x40, 0x40, DEF_STR2( Coinage ) );
	PORT_DIPSETTING(    0x40, DEF_STR2( _1C_1C ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( _1C_2C ) );
	PORT_DIPNAME( 0x80, 0x00, DEF_STR2( Cabinet ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( Upright ) );
	PORT_DIPSETTING(    0x80, DEF_STR2( Cocktail ) );
	return true;
}

/* Is the same as arkanoij, but the Coinage,
  Lives and Bonus_Life dips are different */
private boolean ipt_arkatayt() {
	PORT_START();	/* IN0 */
	PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_START1 );
	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_START2 );
	PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_SERVICE1 );
	PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_TILT );
	PORT_BIT( 0x10, IP_ACTIVE_HIGH, IPT_COIN1 );
	PORT_BIT( 0x20, IP_ACTIVE_HIGH, IPT_COIN2 );
	PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_SPECIAL );	/* input from the 68705, some bootlegs need it to be 1 */
	PORT_BIT( 0x80, IP_ACTIVE_HIGH, IPT_SPECIAL );	/* input from the 68705 */

	PORT_START();	/* IN1 */
	PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_BUTTON1 );
	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_UNKNOWN );
	PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_COCKTAIL );
	PORT_BIT( 0xf8, IP_ACTIVE_LOW, IPT_UNKNOWN );

	PORT_START();      /* IN2 - spinner (multiplexed for player 1 and 2) */
	PORT_ANALOG( 0xff, 0x00, IPT_DIAL, 30, 15, 0, 0);

	PORT_START();      /* IN3 - spinner Player 2  */
	PORT_ANALOG( 0xff, 0x00, IPT_DIAL | IPF_COCKTAIL, 30, 15, 0, 0);

	PORT_START();	/* DSW1 */
	PORT_DIPNAME( 0x01, 0x00, "Allow Continue" );
	PORT_DIPSETTING(    0x01, DEF_STR2( No ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( Yes ) );
	PORT_DIPNAME( 0x02, 0x02, DEF_STR2( Flip_Screen ) );
	PORT_DIPSETTING(    0x02, DEF_STR2( Off ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( On ) );
	PORT_SERVICE( 0x04, IP_ACTIVE_LOW );
	PORT_DIPNAME( 0x08, 0x08, DEF_STR2( Difficulty ) );
	PORT_DIPSETTING(    0x08, "Easy" );
	PORT_DIPSETTING(    0x00, "Hard" );
	PORT_DIPNAME( 0x10, 0x10, DEF_STR2( Bonus_Life ) );
	PORT_DIPSETTING(    0x10, "60K, 100K and every 60K" );
	PORT_DIPSETTING(    0x00, "60K only" );
	PORT_DIPNAME( 0x20, 0x00, DEF_STR2( Lives ) );
	PORT_DIPSETTING(    0x20, "2" );
	PORT_DIPSETTING(    0x00, "3" );
	PORT_DIPNAME( 0x40, 0x40, DEF_STR2( Coinage ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( _2C_1C ) );
	PORT_DIPSETTING(    0x40, DEF_STR2( _1C_1C ) );
	PORT_DIPNAME( 0x80, 0x00, DEF_STR2( Cabinet ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( Upright ) );
	PORT_DIPSETTING(    0x80, DEF_STR2( Cocktail ) );
	return true;
}

int[][] charlayout =
{
	{8},{8},	/* 8*8 characters */
	{4096},	/* 4096 characters */
	{3},	/* 3 bits per pixel */
	{ 0, 4096*8*8, 2*4096*8*8 },	/* the two bitplanes are separated */
	{ 0, 1, 2, 3, 4, 5, 6, 7 },
	{ 0*8, 1*8, 2*8, 3*8, 4*8, 5*8, 6*8, 7*8 },
	{8*8}	/* every char takes 8 consecutive bytes */
};

private boolean gfxdecodeinfo()
{
	GDI_ADD( REGION_GFX1, 0, charlayout,  0, 64 );
	/* sprites use the same characters above, but are 16x8 */
	GDI_ADD( -1 ); /* end of array */
	return true;
}

public boolean mdrv_bootleg() {
	/* basic machine hardware */
	MDRV_CPU_ADD(Z80, 6000000);	/* 6 MHz ?? */
	MDRV_CPU_MEMORY(boot_readmem(),boot_writemem());
	MDRV_CPU_VBLANK_INT(irq0_line_hold,1);

	/* sound hardware */
	MDRV_SOUND_ADD(ay8910);
	
	MDRV_FRAMES_PER_SECOND(60);
	MDRV_VBLANK_DURATION(DEFAULT_60HZ_VBLANK_DURATION);

	/* video hardware */
	MDRV_VIDEO_ATTRIBUTES(VIDEO_TYPE_RASTER);
	MDRV_SCREEN_SIZE(32*8, 32*8);
	MDRV_VISIBLE_AREA(0*8, 32*8-1, 2*8, 30*8-1);
	MDRV_GFXDECODE(gfxdecodeinfo());
	MDRV_PALETTE_LENGTH(512);
	MDRV_COLORTABLE_LENGTH(512);

	MDRV_PALETTE_INIT(v.RRRR_GGGG_BBBB());
	MDRV_VIDEO_START(generic_vs);
	MDRV_VIDEO_UPDATE(arkanoid_vu);

	return true;
}

/***************************************************************************

  Game driver(s)

***************************************************************************/

private boolean rom_arkatayt() {
	ROM_REGION( 0x10000, REGION_CPU1, 0 );	/* 64k for code */
	ROM_LOAD( "arkanoid.1",   0x0000, 0x8000, 0x6e0a2b6f );
	ROM_LOAD( "arkanoid.2",   0x8000, 0x8000, 0x5a97dd56 );

	ROM_REGION( 0x18000, REGION_GFX1, ROMREGION_DISPOSE );
	ROM_LOAD( "a75_03.rom",   0x00000, 0x8000, 0x038b74ba );
	ROM_LOAD( "a75_04.rom",   0x08000, 0x8000, 0x71fae199 );
	ROM_LOAD( "a75_05.rom",   0x10000, 0x8000, 0xc76374e2 );

	ROM_REGION( 0x0600, REGION_PROMS, 0 );
	ROM_LOAD( "07.bpr",       0x0000, 0x0200, 0x0af8b289 );	/* red component */
	ROM_LOAD( "08.bpr",       0x0200, 0x0200, 0xabb002fb );	/* green component */
	ROM_LOAD( "09.bpr",       0x0400, 0x0200, 0xa7c6c277 );	/* blue component */
	return true;
}

private boolean rom_arkbloc2() {
	ROM_REGION( 0x10000, REGION_CPU1, 0 );	/* 64k for code */
	ROM_LOAD( "ark-6.bin",    0x0000, 0x8000, 0x0be015de );
	ROM_LOAD( "arkgc.2",      0x8000, 0x8000, 0x9f0d4754 );

	ROM_REGION( 0x18000, REGION_GFX1, ROMREGION_DISPOSE );
	ROM_LOAD( "a75_03.rom",   0x00000, 0x8000, 0x038b74ba );
	ROM_LOAD( "a75_04.rom",   0x08000, 0x8000, 0x71fae199 );
	ROM_LOAD( "a75_05.rom",   0x10000, 0x8000, 0xc76374e2 );

	ROM_REGION( 0x0600, REGION_PROMS, 0 );
	ROM_LOAD( "07.bpr",       0x0000, 0x0200, 0x0af8b289 );	/* red component */
	ROM_LOAD( "08.bpr",       0x0200, 0x0200, 0xabb002fb );	/* green component */
	ROM_LOAD( "09.bpr",       0x0400, 0x0200, 0xa7c6c277 );	/* blue component */
	return true;
}

private boolean rom_arkangc() {
	ROM_REGION( 0x10000, REGION_CPU1, 0 );	/* 64k for code */
	ROM_LOAD( "arkgc.1",      0x0000, 0x8000, 0xc54232e6 );
	ROM_LOAD( "arkgc.2",      0x8000, 0x8000, 0x9f0d4754 );

	ROM_REGION( 0x18000, REGION_GFX1, ROMREGION_DISPOSE );
	ROM_LOAD( "a75_03.rom",   0x00000, 0x8000, 0x038b74ba );
	ROM_LOAD( "a75_04.rom",   0x08000, 0x8000, 0x71fae199 );
	ROM_LOAD( "a75_05.rom",   0x10000, 0x8000, 0xc76374e2 );

	ROM_REGION( 0x0600, REGION_PROMS, 0 );
	ROM_LOAD( "07.bpr",       0x0000, 0x0200, 0x0af8b289 );	/* red component */
	ROM_LOAD( "08.bpr",       0x0200, 0x0200, 0xabb002fb );	/* green component */
	ROM_LOAD( "09.bpr",       0x0400, 0x0200, 0xa7c6c277 );	/* blue component */
	return true;
}

	public Machine getMachine(URL url, String name) {
		super.getMachine(url,name);
		super.setVideoEmulator(v);
		m = new jef.machine.BasicMachine();

		if (name.equals("arkatayt")) {
			GAME( 1986, rom_arkatayt(), "arkanoid", mdrv_bootleg(),  ipt_arknoidj(), 0, ROT90, "bootleg", "Arkanoid (Tayto bootleg, Japanese)" );
		} else if (name.equals("arkbloc2")) {
			GAME( 1986, rom_arkbloc2(), "arkanoid", mdrv_bootleg(),  ipt_arknoidj(), 0, ROT90, "bootleg", "Block (Game Corporation bootleg)" );
		} else if (name.equals("arkangc")) {
			GAME( 1986, rom_arkangc(),  "arkanoid", mdrv_bootleg(),  ipt_arknoidj(), 0, ROT90, "bootleg", "Arkanoid (Game Corporation bootleg)" );
		}

		m.init(md);
		return (Machine)m;
	}

}

