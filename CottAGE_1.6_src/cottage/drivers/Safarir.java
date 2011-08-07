/*
CottAGE - the Arcade Generic Emulator in Java

Java driver by Gollum
*/

/****************************************************************************

Safari Rally by SNK/Taito

Driver by Zsolt Vasvari


This hardware is a precursor to Phoenix.

----------------------------------

CPU board

76477        18MHz

              8080

Video board


 RL07  2114
       2114
       2114
       2114
       2114           RL01 RL02
       2114           RL03 RL04
       2114           RL05 RL06
 RL08  2114

11MHz

----------------------------------

TODO:

- SN76477 sound

****************************************************************************/

package cottage.drivers;

import java.net.URL;

import jef.machine.Machine;
import jef.map.ReadHandler;
import jef.map.WriteHandler;
import jef.video.Vh_convert_color_proms;
import jef.video.Vh_refresh;

import cottage.mame.Driver;
import cottage.mame.MAMEConstants;
import cottage.mame.MAMEDriver;

public class Safarir extends MAMEDriver implements Driver, MAMEConstants {

cottage.vidhrdw.Safarir v = new cottage.vidhrdw.Safarir();
Vh_refresh safarir_vu = (Vh_refresh)v;
Vh_convert_color_proms safarir_pi = (Vh_convert_color_proms)v;
WriteHandler safarir_scroll_w = v.safarir_scroll_w();
WriteHandler safarir_ram_w = v.safarir_ram_w();
ReadHandler safarir_ram_r = v.safarir_ram_r();
WriteHandler safarir_ram_bank_w = v.safarir_ram_bank_w();
int[] safarir_ram1 = v.Fsafarir_ram1;
int[] safarir_ram2 = v.Fsafarir_ram2;
int[] safarir_ram_size = v.Fsafarir_ram_size;

jef.machine.BasicMachine m = new jef.machine.BasicMachine();


private boolean readmem() {
	MR_START( 0x0000, 0x17ff, MRA_ROM );
	MR_ADD( 0x2000, 0x27ff, safarir_ram_r );
	MR_ADD( 0x3800, 0x38ff, input_port_0_r );
	MR_ADD( 0x3c00, 0x3cff, input_port_1_r );
	return true;
}

private boolean writemem() {
	MW_START( 0x0000, 0x17ff, MWA_ROM );
	MW_ADD( 0x2000, 0x27ff, safarir_ram_w, safarir_ram1, safarir_ram_size );
	MW_ADD( 0x2800, 0x28ff, safarir_ram_bank_w );
	MW_ADD( 0x2c00, 0x2cff, safarir_scroll_w );
	MW_ADD( 0x3000, 0x30ff, MWA_NOP );	/* goes to SN76477 */

	MW_ADD( 0x8000, 0x87ff, MWA_NOP, safarir_ram2 );	/* only here to initialize pointer */
	return true;
}


private boolean ipt_safarir() {
	PORT_START();	/* IN0 */
	PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_COIN1 );
	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_START1 );
	PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_START2 );
	PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_UNKNOWN );
	PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 );
	PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_2WAY );
	PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_2WAY );
	PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );

	PORT_START();	/* DSW0 */
	PORT_DIPNAME( 0x03, 0x00, DEF_STR2( Lives ) );
	PORT_DIPSETTING(    0x00, "3" );
	PORT_DIPSETTING(    0x01, "4" );
	PORT_DIPSETTING(    0x02, "5" );
	PORT_DIPSETTING(    0x03, "6" );
	PORT_DIPNAME( 0x0c, 0x04, "Acceleration Rate" );
	PORT_DIPSETTING(    0x00, "Slowest" );
	PORT_DIPSETTING(    0x04, "Slow" );
	PORT_DIPSETTING(    0x08, "Fast" );
	PORT_DIPSETTING(    0x0c, "Fastest" );
	PORT_DIPNAME( 0x10, 0x00, DEF_STR2( Unknown ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( Off ) );
	PORT_DIPSETTING(    0x10, DEF_STR2( On ) );
	PORT_DIPNAME( 0x60, 0x00, DEF_STR2( Bonus_Life ) );
	PORT_DIPSETTING(    0x00, "3000" );
	PORT_DIPSETTING(    0x20, "5000" );
	PORT_DIPSETTING(    0x40, "7000" );
	PORT_DIPSETTING(    0x60, "9000" );
	PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_VBLANK );
	return true;
}


int[][] charlayout =
{
	{8},{8},	/* 8*8 chars */
	{128},	/* 128 characters */
	{1},		/* 1 bit per pixel */
	{ 0 },
	{ 7, 6, 5, 4, 3, 2, 1, 0 },
	{ 0*8, 1*8, 2*8, 3*8, 4*8, 5*8, 6*8, 7*8 },
	{8*8}	/* every char takes 8 consecutive bytes */
};

private boolean gfxdecodeinfo() {
	GDI_ADD( REGION_GFX1, 0, charlayout, 0, 2 );
	GDI_ADD( REGION_GFX2, 0, charlayout, 0, 2 );
	GDI_ADD( -1 ); /* end of array */
	return true;
}

public boolean mdrv_safarir() {

	/* basic machine hardware */
	MDRV_CPU_ADD(I8080, 3072000);	/* 3 MHz ? */
	MDRV_CPU_MEMORY(readmem(),writemem());

	MDRV_FRAMES_PER_SECOND(60);
	MDRV_VBLANK_DURATION(DEFAULT_REAL_60HZ_VBLANK_DURATION);

	/* video hardware */
	MDRV_VIDEO_ATTRIBUTES(VIDEO_TYPE_RASTER);
	MDRV_SCREEN_SIZE(32*8, 32*8);
	MDRV_VISIBLE_AREA(0*8, 30*8-1, 0*8, 28*8-1);
	MDRV_GFXDECODE(gfxdecodeinfo());
	MDRV_PALETTE_LENGTH(3);
	MDRV_COLORTABLE_LENGTH(2*2);

	MDRV_PALETTE_INIT(safarir_pi);
	MDRV_VIDEO_UPDATE(safarir_vu);

	/* sound hardware */
	return true;
}

/***************************************************************************

  Game driver(s)

***************************************************************************/

private boolean rom_safarir() {
	ROM_REGION( 0x10000, REGION_CPU1, 0 );     /* 64k for main CPU */
	ROM_LOAD( "rl01",		0x0000, 0x0400, 0xcf7703c9 );
	ROM_LOAD( "rl02",		0x0400, 0x0400, 0x1013ecd3 );
	ROM_LOAD( "rl03",		0x0800, 0x0400, 0x84545894 );
	ROM_LOAD( "rl04",		0x0c00, 0x0400, 0x5dd12f96 );
	ROM_LOAD( "rl05",		0x1000, 0x0400, 0x935ed469 );
	ROM_LOAD( "rl06",		0x1400, 0x0400, 0x24c1cd42 );

	ROM_REGION( 0x0400, REGION_GFX1, ROMREGION_DISPOSE );
	ROM_LOAD( "rl08",		0x0000, 0x0400, 0xd6a50aac );

	ROM_REGION( 0x0400, REGION_GFX2, ROMREGION_DISPOSE );
	ROM_LOAD( "rl07",		0x0000, 0x0400, 0xba525203 );
	return true;
}

	public Machine getMachine(URL url, String name) {
		super.getMachine(url,name);
		super.setVideoEmulator(v);

		if (name.equals("safarir")) {
			GAME("19??", rom_safarir(),  0, mdrv_safarir(), ipt_safarir(), 0, ROT90, "SNK", "Safari Rally" );
		}

		m.init(md);

		return (Machine)m;
	}

}
