/*
CottAGE - the Arcade Generic Emulator in Java

Java driver by LFE, Gollum
*/

/****************************************************************************

Tropical Angel

driver by Phil Stroffolino

****************************************************************************/
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

public class Troangel extends MAMEDriver implements Driver, MAMEConstants {

cottage.vidhrdw.Troangel v = new cottage.vidhrdw.Troangel();
Vh_start generic_vs = (Vh_start)v;
Vh_refresh troangel_vu = (Vh_refresh)v;
Vh_convert_color_proms troangel_pi = (Vh_convert_color_proms)v;
int[] troangel_scroll = v.Ftroangel_scroll;
WriteHandler videoram_w = v.videoram_w();

jef.machine.BasicMachine m = new jef.machine.BasicMachine();
InterruptHandler irq0_line_hold = m.irq0_line_hold();

private boolean troangel_readmem() {
	MR_START( 0x0000, 0x7fff, MRA_ROM );
	MR_ADD( 0x8000, 0x8fff, MRA_RAM );
	MR_ADD( 0x9000, 0x90ff, MRA_RAM );
	MR_ADD( 0xd000, 0xd000, input_port_0_r );
	MR_ADD( 0xd001, 0xd001, input_port_1_r );
	MR_ADD( 0xd002, 0xd002, input_port_2_r );
	MR_ADD( 0xd003, 0xd003, input_port_3_r );
	MR_ADD( 0xd004, 0xd004, input_port_4_r );
	MR_ADD( 0xe000, 0xe7ff, MRA_RAM );
	return true;
}

private boolean troangel_writemem() {
	MW_START( 0x0000, 0x7fff, MWA_ROM );
	MW_ADD( 0x8000, 0x87ff, videoram_w, videoram, videoram_size );
//	MW_ADD( 0x8800, 0x8fff, MWA_RAM );
	MW_ADD( 0x9000, 0x91ff, MWA_RAM, troangel_scroll );
	MW_ADD( 0xc820, 0xc8ff, MWA_RAM, spriteram, spriteram_size );
//	MW_ADD( 0xd000, 0xd000, irem_sound_cmd_w );
//	MW_ADD( 0xd001, 0xd001, troangel_flipscreen_w );	/* + coin counters */
	MW_ADD( 0xe000, 0xe7ff, MWA_RAM );
	return true;
}

private boolean ipt_troangel() {
	PORT_START();	/* IN0 */
	PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_START1 );
	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_START2 );
	/* coin input must be active for 19 frames to be consistently recognized */
	/*PORT_BIT_IMPULSE( 0x04, IP_ACTIVE_LOW, IPT_COIN3, 19 );*/
	PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_COIN3 );
	PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_COIN1 );
	PORT_BIT( 0xf0, IP_ACTIVE_LOW, IPT_UNUSED );

	PORT_START();	/* IN1 */
	PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_2WAY );
	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT | IPF_2WAY );
	PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_UNKNOWN );
	PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_UNKNOWN );
	PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_UNKNOWN );
	PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON2 );
	PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
	PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_BUTTON1 );

	PORT_START();	/* IN2 */
	PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_2WAY | IPF_COCKTAIL );
	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT | IPF_2WAY | IPF_COCKTAIL );
	PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_UNKNOWN );
	PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_UNKNOWN );
	PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_COIN2 );
	PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_COCKTAIL );
	PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
	PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_COCKTAIL );

	PORT_START();	/* DSW1 */
	PORT_DIPNAME( 0x03, 0x03, "Time" );
	PORT_DIPSETTING(    0x03, "180 160 140" );
	PORT_DIPSETTING(    0x02, "160 140 120" );
	PORT_DIPSETTING(    0x01, "140 120 100" );
	PORT_DIPSETTING(    0x00, "120 100 100" );
	PORT_DIPNAME( 0x04, 0x04, "Crash Loss Time" );
	PORT_DIPSETTING(    0x04, "5" );
	PORT_DIPSETTING(    0x00, "10" );
	PORT_DIPNAME( 0x08, 0x08, "Background Sound" );
	PORT_DIPSETTING(    0x08, "Boat Motor" );
	PORT_DIPSETTING(    0x00, "Music" );
	/* TODO: support the different settings which happen in Coin Mode 2 */
	PORT_DIPNAME( 0xf0, 0xf0, DEF_STR2( Coinage ) ); /* mapped on coin mode 1 */
	PORT_DIPSETTING(    0xa0, DEF_STR2( _6C_1C ) );
	PORT_DIPSETTING(    0xb0, DEF_STR2( _5C_1C ) );
	PORT_DIPSETTING(    0xc0, DEF_STR2( _4C_1C ) );
	PORT_DIPSETTING(    0xd0, DEF_STR2( _3C_1C ) );
	PORT_DIPSETTING(    0xe0, DEF_STR2( _2C_1C ) );
	PORT_DIPSETTING(    0xf0, DEF_STR2( _1C_1C ) );
	PORT_DIPSETTING(    0x70, DEF_STR2( _1C_2C ) );
	PORT_DIPSETTING(    0x60, DEF_STR2( _1C_3C ) );
	PORT_DIPSETTING(    0x50, DEF_STR2( _1C_4C ) );
	PORT_DIPSETTING(    0x40, DEF_STR2( _1C_5C ) );
	PORT_DIPSETTING(    0x30, DEF_STR2( _1C_6C ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( Free_Play ) );
	/* settings 0x10, 0x20, 0x80, 0x90 all give 1 Coin/1 Credit */

	PORT_START();	/* DSW2 */
	PORT_DIPNAME( 0x01, 0x01, DEF_STR2( Flip_Screen ) );
	PORT_DIPSETTING(    0x01, DEF_STR2( Off ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( On ) );
	PORT_DIPNAME( 0x02, 0x00, DEF_STR2( Cabinet ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( Upright ) );
	PORT_DIPSETTING(    0x02, DEF_STR2( Cocktail ) );
/* This activates a different coin mode. Look at the dip switch setting schematic */
	PORT_DIPNAME( 0x04, 0x04, "Coin Mode" );
	PORT_DIPSETTING(    0x04, "Mode 1" );
	PORT_DIPSETTING(    0x00, "Mode 2" );
/* TODO: the following enables an analog accelerator input read from 0xd003 */
/* however that is the DSW1 input so it must be multiplexed some way */
	PORT_DIPNAME( 0x08, 0x08, "Analog Accelarator" );
	PORT_DIPSETTING(    0x08, DEF_STR2( No ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( Yes ) );
	/* In stop mode, press 2 to stop and 1 to restart */
	PORT_BITX   ( 0x10, 0x10, IPT_DIPSWITCH_NAME | IPF_CHEAT, "Stop Mode", IP_KEY_NONE, IP_JOY_NONE );
	PORT_DIPSETTING(    0x10, DEF_STR2( Off ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( On ) );
	PORT_DIPNAME( 0x20, 0x20, DEF_STR2( Unknown ) );
	PORT_DIPSETTING(    0x20, DEF_STR2( Off ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( On ) );
	PORT_BITX(    0x40, 0x40, IPT_DIPSWITCH_NAME | IPF_CHEAT, "Invulnerability", IP_KEY_NONE, IP_JOY_NONE );
	PORT_DIPSETTING(    0x40, DEF_STR2( Off ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( On ) );
	PORT_SERVICE( 0x80, IP_ACTIVE_LOW );
	return true;
}

int[][] charlayout =
{
	{8},{8}, /* character size */
	{1024}, /* number of characters */
	{3}, /* bits per pixel */
	{ 2*1024*8*8, 1024*8*8, 0 },
	{ 0, 1, 2, 3, 4, 5, 6, 7 },
	{ 0*8, 1*8, 2*8, 3*8, 4*8, 5*8, 6*8, 7*8 },
	{8*8}	/* character offset */
};

int[][] spritelayout =
{
	{16},{32}, /* sprite size */
	{64}, /* number of sprites */
	{3}, /* bits per pixel */
	{ 2*0x4000*8, 0x4000*8, 0 },
	{ 0, 1, 2, 3, 4, 5, 6, 7,
			16*8+0, 16*8+1, 16*8+2, 16*8+3, 16*8+4, 16*8+5, 16*8+6, 16*8+7 },
	{ 0*8, 1*8, 2*8, 3*8, 4*8, 5*8, 6*8, 7*8,
			8*8, 9*8, 10*8, 11*8, 12*8, 13*8, 14*8, 15*8,
			256*64+0*8, 256*64+1*8, 256*64+2*8, 256*64+3*8, 256*64+4*8, 256*64+5*8, 256*64+6*8, 256*64+7*8,
			256*64+8*8, 256*64+9*8, 256*64+10*8, 256*64+11*8, 256*64+12*8, 256*64+13*8, 256*64+14*8, 256*64+15*8 },
	{32*8}	/* character offset */
};

private boolean troangel_gfxdecodeinfo() {
	GDI_ADD( REGION_GFX1, 0x0000, charlayout,      0, 32 );
	GDI_ADD( REGION_GFX2, 0x0000, spritelayout, 32*8, 32 );
	GDI_ADD( REGION_GFX2, 0x1000, spritelayout, 32*8, 32 );
	GDI_ADD( REGION_GFX2, 0x2000, spritelayout, 32*8, 32 );
	GDI_ADD( REGION_GFX2, 0x3000, spritelayout, 32*8, 32 );
	GDI_ADD( -1 ); /* end of array */
	return true;
};

public boolean mdrv_troangel() {
	/* basic machine hardware */
	MDRV_CPU_ADD(Z80, 3000000);	/* 3 MHz ??? */
	MDRV_CPU_MEMORY(troangel_readmem(),troangel_writemem());
	MDRV_CPU_VBLANK_INT(irq0_line_hold,1);

	MDRV_FRAMES_PER_SECOND(57);
	MDRV_VBLANK_DURATION(1790);	/* accurate frequency, measured on a Moon Patrol board, is 56.75Hz. */
				/* the Lode Runner manual (similar but different hardware) */
				/* talks about 55Hz and 1790ms vblank duration. */

	/* video hardware */
	MDRV_VIDEO_ATTRIBUTES(VIDEO_TYPE_RASTER);
	MDRV_SCREEN_SIZE(32*8, 32*8);
	MDRV_VISIBLE_AREA(1*8, 31*8-1, 1*8, 31*8-1);
	MDRV_GFXDECODE(troangel_gfxdecodeinfo());
	MDRV_PALETTE_LENGTH(32*8+16);
	MDRV_COLORTABLE_LENGTH(32*8+32*8);

	MDRV_PALETTE_INIT(troangel_pi);
	MDRV_VIDEO_START(generic_vs);
	MDRV_VIDEO_UPDATE(troangel_vu);

	/* sound hardware */
	//MDRV_IMPORT_FROM(irem_audio);
	return true;
}

private boolean rom_troangel() {
	ROM_REGION( 0x10000, REGION_CPU1, 0 );	/* main CPU */
	ROM_LOAD( "ta-a-3k",	0x0000, 0x2000, 0xf21f8196 );
	ROM_LOAD( "ta-a-3m",	0x2000, 0x2000, 0x58801e55 );
	ROM_LOAD( "ta-a-3n",	0x4000, 0x2000, 0xde3dea44 );
	ROM_LOAD( "ta-a-3q",	0x6000, 0x2000, 0xfff0fc2a );

	ROM_REGION(  0x10000 , REGION_CPU2, 0 );	/* sound CPU */
	ROM_LOAD( "ta-s-1a",	0xe000, 0x2000, 0x15a83210 );

	ROM_REGION( 0x06000, REGION_GFX1, ROMREGION_DISPOSE );
	ROM_LOAD( "ta-a-3c",	0x00000, 0x2000, 0x7ff5482f );	/* characters */
	ROM_LOAD( "ta-a-3d",	0x02000, 0x2000, 0x06eef241 );
	ROM_LOAD( "ta-a-3e",	0x04000, 0x2000, 0xe49f7ad8 );

	ROM_REGION( 0x0c000, REGION_GFX2, ROMREGION_DISPOSE );
	ROM_LOAD( "ta-b-5j",	0x00000, 0x2000, 0x86895c0c );	/* sprites */
	ROM_LOAD( "ta-b-5h",	0x02000, 0x2000, 0xf8cff29d );
	ROM_LOAD( "ta-b-5e",	0x04000, 0x2000, 0x8b21ee9a );
	ROM_LOAD( "ta-b-5d",	0x06000, 0x2000, 0xcd473d47 );
	ROM_LOAD( "ta-b-5c",	0x08000, 0x2000, 0xc19134c9 );
	ROM_LOAD( "ta-b-5a",	0x0a000, 0x2000, 0x0012792a );

	ROM_REGION( 0x0320, REGION_PROMS, 0 );
	ROM_LOAD( "ta-a-5a",	0x0000,	0x0100, 0x01de1167 ); /* chars palette low 4 bits */
	ROM_LOAD( "ta-a-5b",	0x0100,	0x0100, 0xefd11d4b ); /* chars palette high 4 bits */
	ROM_LOAD( "ta-b-1b",	0x0200, 0x0020, 0xf94911ea ); /* sprites palette */
	ROM_LOAD( "ta-b-3d",	0x0220,	0x0100, 0xed3e2aa4 ); /* sprites lookup table */
	return true;
}

public Machine getMachine(URL url, String name) {
	super.getMachine(url,name);
	super.setVideoEmulator(v);

	if (name.equals("troangel")) {
		GAME(1983, rom_troangel(),  0, mdrv_troangel(), ipt_troangel(), 0, ROT0, "Irem", "Tropical Angel" );
	}

	m.init(md);
	v.setMachine(m);

	return (Machine)m;
	}

}