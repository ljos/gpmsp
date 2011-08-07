/*
CottAGE - the Arcade Generic Emulator in Java

Java driver by
*/

package cottage.drivers;

import java.net.URL;

import jef.machine.Machine;
import jef.map.InterruptHandler;
import jef.video.Vh_refresh;
import jef.video.Vh_start;

import cottage.mame.Driver;
import cottage.mame.MAMEConstants;
import cottage.mame.MAMEDriver;

public class Berzerk extends MAMEDriver implements Driver, MAMEConstants {

cottage.vidhrdw.TEMPLATE v = new cottage.vidhrdw.TEMPLATE();
Vh_start template_vs = (Vh_start)v;
Vh_refresh template_vu = (Vh_refresh)v;

jef.machine.BasicMachine m = new jef.machine.BasicMachine();
InterruptHandler irq0_line_hold = m.irq0_line_hold();

int berzerk_videoram_w = MWA_RAM;
int berzerk_magicram_w = MWA_RAM;
int berzerk_colorram_w = MWA_RAM;

private boolean readmem() {
	MR_START( 0x0000, 0x07ff, MRA_ROM );
	MR_ADD( 0x0800, 0x09ff, MRA_RAM );
    MR_ADD( 0x1000, 0x3fff, MRA_ROM );
    MR_ADD( 0x4000, 0x87ff, MRA_RAM );
	return true;
}

private boolean writemem() {
    MW_START( 0x0000, 0x07ff, MWA_ROM );
	MW_ADD( 0x0800, 0x09ff, MWA_RAM );
    MW_ADD( 0x1000, 0x3fff, MWA_ROM );
    MW_ADD( 0x4000, 0x5fff, berzerk_videoram_w );
    MW_ADD( 0x6000, 0x7fff, berzerk_magicram_w );
    MW_ADD( 0x8000, 0x87ff, berzerk_colorram_w );
	return true;
}

private boolean ipt_template() {
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

public boolean mdrv_template() {
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

	MDRV_VIDEO_START(template_vs);
	MDRV_VIDEO_UPDATE(template_vu);

	/* sound hardware */
	//MDRV_SOUND_ADD(OKIM6295, okim6295_interface)

	return true;
}

private boolean rom_template() {
	ROM_REGION( 0x10000, REGION_CPU1, 0 );
	ROM_LOAD( "virus.4", 0x00000, 0x08000, BADCRC( 0xaa005dfb ) ); /* The Original was too short, I padded it with 0xFF */

	ROM_REGION( 0x80000, REGION_GFX1, 0 );
	ROM_LOAD( "virus.2", 0x0000, 0x4000, 0xb5af58d8 );

	return true;
}

	public Machine getMachine(URL url, String name) {
		super.getMachine(url,name);
		super.setVideoEmulator(v);

		if (name.equals("template")) {
			GAME( 1993, rom_template(), 0, mdrv_template(), ipt_template(), 0, ROT0, "Poby / Virus", "TEMPLATE" );
		}

		m.init(md);
		return (Machine)m;
	}

}
