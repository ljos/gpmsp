/*
CottAGE - the Arcade Generic Emulator in Java

Java driver by Gollum
*/

/*

News

from the program ROM:
PROGRAMED BY KWANG-HO CHO
COPYRIGHT(C) 1993
ALL RIGHTS RESERVED BY POBY
Hi-tel ID:poby:


driver by David Haywood

Notes:
- The gfx data cointains pictures for both women and girls, however only the women
  seem to be used. Different ROM set, probably (there's a table at 0x253 containing
  the numbers of the pictures to use).
*/

package cottage.drivers;

import java.net.URL;

import jef.machine.Machine;
import jef.map.InterruptHandler;
import jef.map.WriteHandler;
import jef.video.Vh_refresh;
import jef.video.Vh_start;

import cottage.mame.Driver;
import cottage.mame.MAMEConstants;
import cottage.mame.MAMEDriver;

public class News extends MAMEDriver implements Driver, MAMEConstants {

cottage.vidhrdw.News v = new cottage.vidhrdw.News();
int[] news_fgram = v.Fnews_fgram;
int[] news_bgram = v.Fnews_bgram;
WriteHandler news_fgram_w = v.news_fgram_w();
WriteHandler news_bgram_w = v.news_bgram_w();
WriteHandler news_bgpic_w = v.news_bgpic_w();
Vh_start news_vs = (Vh_start)v;
Vh_refresh news_vu = (Vh_refresh)v;
WriteHandler paletteram_xxxxRRRRGGGGBBBB_swap_w = v.paletteram_xxxxRRRRGGGGBBBB_swap_w();

jef.machine.BasicMachine m = new jef.machine.BasicMachine();
InterruptHandler irq0_line_hold = m.irq0_line_hold();

private boolean readmem() {
	MR_START( 0x0000, 0x7fff, MRA_ROM );
	MR_ADD( 0x8000, 0x8fff, MRA_RAM );
	MR_ADD( 0xc000, 0xc000, input_port_0_r );
	MR_ADD( 0xc001, 0xc001, input_port_1_r );
	//MR_ADD( 0xc002, 0xc002, OKIM6295_status_0_r );
	MR_ADD( 0xe000, 0xffff, MRA_RAM );
	return true;
}

private boolean writemem() {
	MW_START( 0x0000, 0x7fff, MWA_ROM );	/* 4000-7fff is written to during startup, probably leftover code */
	MW_ADD( 0x8000, 0x87ff, news_fgram_w, news_fgram );
	MW_ADD( 0x8800, 0x8fff, news_bgram_w, news_bgram );
	MW_ADD( 0x9000, 0x91ff, MWA_RAM );
	MW_ADD( 0x9000, 0x91ff, paletteram_xxxxRRRRGGGGBBBB_swap_w, paletteram );
	//MW_ADD( 0xc002, 0xc002, OKIM6295_data_0_w ); /* ?? */
	MW_ADD( 0xc003, 0xc003, news_bgpic_w );
	MW_ADD( 0xe000, 0xffff, MWA_RAM );
	return true;
}

private boolean ipt_news() {
	PORT_START();	/* DSW */
	PORT_DIPNAME( 0x03, 0x03, DEF_STR2( Coinage ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( _3C_1C ) );
	PORT_DIPSETTING(    0x01, DEF_STR2( _2C_1C ) );
	PORT_DIPSETTING(    0x03, DEF_STR2( _1C_1C ) );
	PORT_DIPSETTING(    0x02, DEF_STR2( _1C_2C ) );
	PORT_DIPNAME( 0x0c, 0x0c, DEF_STR2( Difficulty ) );
	PORT_DIPSETTING(    0x0c, "Easy" );
	PORT_DIPSETTING(    0x08, "Medium" );
	PORT_DIPSETTING(    0x04, "Hard" );
	PORT_DIPSETTING(    0x00, "Hardest" );
	PORT_DIPNAME( 0x10, 0x10, "Helps" );
	PORT_DIPSETTING(    0x10, "1" );
	PORT_DIPSETTING(    0x00, "2" );
	PORT_DIPNAME( 0x20, 0x00, "Copyright" );
	PORT_DIPSETTING(    0x00, "Poby" );
	PORT_DIPSETTING(    0x20, "Virus" );
	PORT_DIPNAME( 0x40, 0x40, DEF_STR2( Unused ) );
	PORT_DIPSETTING(    0x40, DEF_STR2( Off ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( On ) );
	PORT_DIPNAME( 0x80, 0x80, DEF_STR2( Unused ) );
	PORT_DIPSETTING(    0x80, DEF_STR2( Off ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( On ) );

	PORT_START();
	PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_COIN1 );
	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_START1 );
	PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY );
	PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY );
	PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY );
	PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY );
	PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_BUTTON1 );
	PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_BUTTON2 );
	return true;
}

int[][] tiles8x8_layout =
{
	{8},{8},
	{RGN_FRAC(1,1)},
	{4},
	{ 0, 1, 2, 3 },
	{ 0, 4, 8, 12, 16, 20, 24, 28 },
	{ 0*32, 1*32, 2*32, 3*32, 4*32, 5*32, 6*32, 7*32 },
	{32*8}
};

private boolean gfxdecodeinfo()
{
	GDI_ADD( REGION_GFX1, 0, tiles8x8_layout, 0, 16 );
	GDI_ADD( -1 );
	return true;
};

public boolean mdrv_news() {
	/* basic machine hardware */
	MDRV_CPU_ADD(Z80,8000000);		 /* ? MHz */
	MDRV_CPU_MEMORY(readmem(),writemem());
	MDRV_CPU_VBLANK_INT(irq0_line_hold,1);

	MDRV_FRAMES_PER_SECOND(60);
	MDRV_VBLANK_DURATION(DEFAULT_60HZ_VBLANK_DURATION);

	/* video hardware */
	MDRV_VIDEO_ATTRIBUTES(VIDEO_TYPE_RASTER);
	MDRV_SCREEN_SIZE(256, 256);
	MDRV_VISIBLE_AREA(0, 256-1, 16, 256-16-1);
	MDRV_GFXDECODE(gfxdecodeinfo());
	MDRV_PALETTE_LENGTH(0x100);

	MDRV_VIDEO_START(news_vs);
	MDRV_VIDEO_UPDATE(news_vu);

	/* sound hardware */
	//MDRV_SOUND_ADD(OKIM6295, okim6295_interface)
	return true;
}

private boolean rom_news() {
	ROM_REGION( 0x10000, REGION_CPU1, 0 );
	ROM_LOAD( "virus.4", 0x00000, 0x08000, BADCRC( 0xaa005dfb ) ); /* The Original was too short, I padded it with 0xFF */

	ROM_REGION( 0x80000, REGION_GFX1, 0 );
	ROM_LOAD16_BYTE( "virus.2", 0x00000, 0x40000, 0xb5af58d8 );
	ROM_LOAD16_BYTE( "virus.3", 0x00001, 0x40000, 0xa4b1c175 );

	//ROM_REGION( 0x40000, REGION_SOUND1, 0 );
	//ROM_LOAD( "virus.1", 0x00000, 0x40000, 0x41f5935a );

	return true;
}

	public Machine getMachine(URL url, String name) {
		super.getMachine(url,name);
		super.setVideoEmulator(v);
		m = new jef.machine.BasicMachine();

		if (name.equals("news")) {
			GAME( 1993, rom_news(), 0, mdrv_news(), ipt_news(), 0, ROT0, "Poby / Virus", "News" );
		}

		m.init(md);
		return (Machine)m;
	}

}
