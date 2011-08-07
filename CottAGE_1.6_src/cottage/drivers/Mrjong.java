/*
CottAGE - the Arcade Generic Emulator in Java

Java driver by LFE, Gollum
*/

/***************************************************************************

Mr.Jong
(c)1983 Kiwako (This game is distributed by Sanritsu.)

Crazy Blocks
(c)1983 Kiwako/ECI

Driver by Takahiro Nogi (nogi@kt.rim.or.jp) 2000/03/20 -

***************************************************************************/
package cottage.drivers;

import java.net.URL;

import jef.machine.Machine;
import jef.map.InterruptHandler;
import jef.map.ReadHandler;
import jef.map.WriteHandler;
import jef.video.Vh_convert_color_proms;
import jef.video.Vh_refresh;
import jef.video.Vh_start;

import cottage.mame.Driver;
import cottage.mame.MAMEConstants;
import cottage.mame.MAMEDriver;

public class Mrjong extends MAMEDriver implements Driver, MAMEConstants {

cottage.vidhrdw.Mrjong v = new cottage.vidhrdw.Mrjong();
Vh_start generic_vs = (Vh_start)v;
Vh_refresh mrjong_vu = (Vh_refresh)v;
Vh_convert_color_proms mrjong_pi = (Vh_convert_color_proms)v;
WriteHandler videoram_w = v.videoram_w();
WriteHandler colorram_w = v.colorram_w();

jef.machine.BasicMachine m = new jef.machine.BasicMachine();
InterruptHandler nmi_line_pulse = m.nmi_interrupt_switched();

WriteHandler io_0x00_w = new Io_0x00_w();
ReadHandler io_0x03_r = new Io_0x03_r();

private boolean readmem() {
	MR_START( 0x0000, 0x7fff, MRA_ROM );
	MR_ADD( 0x8000, 0x87ff, MRA_RAM );
	MR_ADD( 0xa000, 0xa7ff, MRA_RAM );
	MR_ADD( 0xe000, 0xe3ff, videoram_r );
	MR_ADD( 0xe400, 0xe7ff, colorram_r );
	return true;
}

private boolean writemem() {
	MW_START( 0x0000, 0x7fff, MWA_ROM );
	MW_ADD( 0x8000, 0x87ff, MWA_RAM );
	MW_ADD( 0xa000, 0xa7ff, MWA_RAM );
	MW_ADD( 0xe000, 0xe3ff, videoram_w, videoram, videoram_size );
	MW_ADD( 0xe400, 0xe7ff, colorram_w, colorram );
	MW_ADD( 0xe000, 0xe03f, MWA_RAM, spriteram, spriteram_size ); /* here to initialize the pointer */
	return true;
}

class Io_0x00_w implements WriteHandler {
	public void write(int address, int data) {
		//mrjong_flipscreen_w(0, ((data & 0x04) > 2));
	}
}

class Io_0x03_r implements ReadHandler {
	public int read(int address) {
		return 0x00;
	}
}

private boolean readport() {
	PR_START( 0x00, 0x00, input_port_0_r );
	PR_ADD( 0x01, 0x01, input_port_1_r );
	PR_ADD( 0x02, 0x02, input_port_2_r );
	PR_ADD( 0x03, 0x03, io_0x03_r );
	return true;
}

private boolean writeport() {
	PW_START( 0x00, 0x00, io_0x00_w );
	//PW_ADD( 0x01, 0x01, SN76496_0_w );
	//PW_ADD( 0x02, 0x02, SN76496_1_w );
	return true;
}

private boolean ipt_mrjong() {
	PORT_START();	/* IN0 */
	PORT_BIT( 0x01, IP_ACTIVE_HIGH, IPT_JOYSTICK_UP    | IPF_4WAY | IPF_PLAYER2 );
	PORT_BIT( 0x02, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT  | IPF_4WAY | IPF_PLAYER2 );
	PORT_BIT( 0x04, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_4WAY | IPF_PLAYER2 );
	PORT_BIT( 0x08, IP_ACTIVE_HIGH, IPT_JOYSTICK_DOWN  | IPF_4WAY | IPF_PLAYER2 );
	PORT_BIT( 0x10, IP_ACTIVE_HIGH, IPT_BUTTON1 | IPF_PLAYER2 );
	PORT_BIT( 0x20, IP_ACTIVE_HIGH, IPT_START1 );
	PORT_BIT( 0x40, IP_ACTIVE_HIGH, IPT_START2 );
	PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );		// ????

	PORT_START();	/* IN1 */
	PORT_BIT( 0x01, IP_ACTIVE_HIGH, IPT_JOYSTICK_UP    | IPF_4WAY | IPF_PLAYER1 );
	PORT_BIT( 0x02, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT  | IPF_4WAY | IPF_PLAYER1 );
	PORT_BIT( 0x04, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_4WAY | IPF_PLAYER1 );
	PORT_BIT( 0x08, IP_ACTIVE_HIGH, IPT_JOYSTICK_DOWN  | IPF_4WAY | IPF_PLAYER1 );
	PORT_BIT( 0x10, IP_ACTIVE_HIGH, IPT_BUTTON1 | IPF_PLAYER1 );
	PORT_BIT( 0x20, IP_ACTIVE_HIGH, IPT_COIN1 );
	PORT_BIT( 0x40, IP_ACTIVE_HIGH, IPT_COIN2 );
	PORT_BIT( 0x80, IP_ACTIVE_HIGH, IPT_UNKNOWN );

	PORT_START();	/* DSW1 */
	PORT_DIPNAME( 0x01, 0x01, DEF_STR2( Cabinet ) );
	PORT_DIPSETTING(    0x01, DEF_STR2( Upright ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( Cocktail ) );
	PORT_DIPNAME( 0x02, 0x00, DEF_STR2( Flip_Screen ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( Off ) );
	PORT_DIPSETTING(    0x02, DEF_STR2( On ) );
	PORT_DIPNAME( 0x04, 0x00, DEF_STR2( Bonus_Life ) );
	PORT_DIPSETTING(    0x00, "30k");
	PORT_DIPSETTING(    0x04, "50k");
	PORT_DIPNAME( 0x08, 0x00, DEF_STR2( Difficulty ) );
	PORT_DIPSETTING(    0x00, "Normal");
	PORT_DIPSETTING(    0x08, "Hard");
	PORT_DIPNAME( 0x30, 0x00, DEF_STR2( Lives ) );
	PORT_DIPSETTING(    0x00, "3");
	PORT_DIPSETTING(    0x10, "4");
	PORT_DIPSETTING(    0x20, "5");
	PORT_DIPSETTING(    0x30, "6");
	PORT_DIPNAME( 0xc0, 0x00, DEF_STR2( Coinage ) );
	PORT_DIPSETTING(    0xc0, DEF_STR2( _2C_1C ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( _1C_1C ) );
	PORT_DIPSETTING(    0x40, DEF_STR2( _1C_2C ) );
	PORT_DIPSETTING(    0x80, DEF_STR2( _1C_3C ) );
	return true;
}

int[][] tilelayout =
{
	{8}, {8},				/* 8*8 characters */
	{512},				/* 512 characters */
	{2},				/* 2 bits per pixel */
	{ 512*8*8, 0 },			/* the two bitplanes are separated */
	{ 0, 1, 2, 3, 4, 5, 6, 7 },	/* pretty straightforward layout */
	{ 7*8, 6*8, 5*8, 4*8, 3*8, 2*8, 1*8, 0*8 },
	{8*8}				/* every char takes 8 consecutive bytes */
};

int[][] spritelayout =
{
	{16}, {16},				/* 16*16 sprites */
	{128},				/* 128 sprites */
	{2},				/* 2 bits per pixel */
	{ 128*16*16, 0 },		/* the bitplanes are separated */
	{ 8*8+0, 8*8+1, 8*8+2, 8*8+3, 8*8+4, 8*8+5, 8*8+6, 8*8+7,	/* pretty straightforward layout */
			0, 1, 2, 3, 4, 5, 6, 7 },
	{ 23*8, 22*8, 21*8, 20*8, 19*8, 18*8, 17*8, 16*8,
			7*8, 6*8, 5*8, 4*8, 3*8, 2*8, 1*8, 0*8 },
	{32*8}				/* every sprite takes 32 consecutive bytes */
};

private boolean gfxdecodeinfo() {
	GDI_ADD( REGION_GFX1, 0x0000, tilelayout,      0, 32 );
	GDI_ADD( REGION_GFX1, 0x0000, spritelayout,    0, 32 );
	GDI_ADD( -1 );
	return true;
};

public boolean mdrv_mrjong() {
	/* basic machine hardware */
	MDRV_CPU_ADD(Z80,15468000/6);	/* 2.578 MHz?? */
	MDRV_CPU_MEMORY(readmem(),writemem());
	MDRV_CPU_PORTS(readport(),writeport());
	MDRV_CPU_VBLANK_INT(nmi_line_pulse,1);

	MDRV_FRAMES_PER_SECOND(60);
	MDRV_VBLANK_DURATION(DEFAULT_60HZ_VBLANK_DURATION);

	/* video hardware */
	MDRV_VIDEO_ATTRIBUTES(VIDEO_TYPE_RASTER);
	MDRV_SCREEN_SIZE(32*8, 32*8);
	MDRV_VISIBLE_AREA(0*8, 30*8-1, 2*8, 30*8-1);
	MDRV_GFXDECODE(gfxdecodeinfo());
	MDRV_PALETTE_LENGTH(16);
	MDRV_COLORTABLE_LENGTH(4*32);

	MDRV_PALETTE_INIT(mrjong_pi);
	MDRV_VIDEO_START(generic_vs);
	MDRV_VIDEO_UPDATE(mrjong_vu);

	/* sound hardware */
	//MDRV_SOUND_ADD(SN76496, sn76496_interface)
	return true;
}

/***************************************************************************

  Game driver(s)

***************************************************************************/

private boolean rom_mrjong() {
	ROM_REGION( 0x10000, REGION_CPU1, 0 );	/* code */
	ROM_LOAD( "mj00", 0x0000, 0x2000, 0xd211aed3 );
	ROM_LOAD( "mj01", 0x2000, 0x2000, 0x49a9ca7e );
	ROM_LOAD( "mj02", 0x4000, 0x2000, 0x4b50ae6a );
	ROM_LOAD( "mj03", 0x6000, 0x2000, 0x2c375a17 );

	ROM_REGION( 0x2000, REGION_GFX1, 0 );	/* gfx */
	ROM_LOAD( "mj21", 0x0000, 0x1000, 0x1ea99dab );
	ROM_LOAD( "mj20", 0x1000, 0x1000, 0x7eb1d381 );

	ROM_REGION( 0x0120, REGION_PROMS, 0 );	/* color */
	ROM_LOAD( "mj61", 0x0000, 0x0020, 0xa85e9b27 );
	ROM_LOAD( "mj60", 0x0020, 0x0100, 0xdd2b304f );
	return true;
}

private boolean rom_crazyblk() {
	ROM_REGION( 0x10000, REGION_CPU1, 0 );	/* code */
	ROM_LOAD( "c1.a6", 0x0000, 0x2000, 0xe2a211a2 );
	ROM_LOAD( "c2.a7", 0x2000, 0x2000, 0x75070978 );
	ROM_LOAD( "c3.a7", 0x4000, 0x2000, 0x696ca502 );
	ROM_LOAD( "c4.a8", 0x6000, 0x2000, 0xc7f5a247 );

	ROM_REGION( 0x2000, REGION_GFX1, 0 );	/* gfx */
	ROM_LOAD( "c6.h5", 0x0000, 0x1000, 0x2b2af794 );
	ROM_LOAD( "c5.h4", 0x1000, 0x1000, 0x98d13915 );

	ROM_REGION( 0x0120, REGION_PROMS, 0 );	/* color */
	ROM_LOAD( "clr.j7", 0x0000, 0x0020, 0xee1cf1d5 );
	ROM_LOAD( "clr.g5", 0x0020, 0x0100, 0xbcb1e2e3 );
	return true;
}

public Machine getMachine(URL url, String name) {
	super.getMachine(url,name);
	super.setVideoEmulator(v);

	if (name.equals("mrjong")) {
		GAME(1983, rom_mrjong(),           0, mdrv_mrjong(), ipt_mrjong(), 0, ROT90, "Kiwako", "Mr. Jong (Japan)" );
	} else if (name.equals("crazyblk")) {
		GAME(1983, rom_crazyblk(),  "mrjong", mdrv_mrjong(), ipt_mrjong(), 0, ROT90, "Kiwako (ECI license)", "Crazy Blocks" );
	}

	m.init(md);
	v.setMachine(m);

	return (Machine)m;
	}

}

