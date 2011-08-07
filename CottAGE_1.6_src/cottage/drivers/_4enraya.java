/***************************************************************************

Driver by Tomasz Slanina  dox@space.pl

***************************************************************************

RAM :
	1 x GM76c28-10 (6116) RAM
	3 x 2114  - VRAM (only 10 bits are used )

ROM:
  27256 + 27128 for code/data
  3x2764 for gfx

PROM:
 82S123 32x8
 Used for system control
 	(d0 - connected to ROM5 /CS , d1 - ROM4 /CS, d2 - RAM /CS , d3 - to some logic(gfx control), and Z80 WAIT )

Memory Map :
  0x0000 - 0xbfff - ROM
  0xc000 - 0xcfff - RAM
  0xd000 - 0xdfff - VRAM mirrored write,
  		tilemap offset = address & 0x3ff
  		tile number =  bits 0-7 = data, bits 8,9  = address bits 10,11

Video :
	No scrolling , no sprites.
	32x32 Tilemap stored in VRAM (10 bits/tile (tile numebr 0-1023))

	3 gfx ROMS
	ROM1 - R component (ROM ->(parallel in) shift register 74166 (serial out) -> jamma output
	ROM2 - B component
	ROM3 - G component

	Default MAME color palette is used

Sound :
 AY 3 8910

 sound_control :

  bit 0 - BC1
  bit 1 - BC2
  bit 2 - BDIR

  bits 3-7 - not connected

***************************************************************************/

package cottage.drivers;

import java.net.URL;
import jef.map.*;
import jef.machine.*;
import jef.video.*;
import cottage.mame.*;

public class _4enraya extends MAMEDriver implements Driver, MAMEConstants {

cottage.vidhrdw._4enraya v = new cottage.vidhrdw._4enraya();
Vh_start _4enraya_vs = (Vh_start)v;
Vh_refresh _4enraya_vu = (Vh_refresh)v;

WriteHandler fenraya_videoram_w = v.fenraya_videoram_w();

jef.machine.BasicMachine m = new jef.machine.BasicMachine();
InterruptHandler irq0_line_hold = m.irq0_line_hold();

static int soundlatch;

private boolean readmem() {
	MR_START( 0x0000, 0xbfff, MRA_ROM );
	MR_ADD( 0xc000, 0xcfff, MRA_RAM );
	MR_ADD( 0xd000, 0xffff, MRA_NOP );
	return true;
}

private boolean writemem() {
	MW_START( 0x0000, 0xbfff, MWA_ROM );
	MW_ADD( 0xc000, 0xcfff, MWA_RAM );
	MW_ADD( 0xd000, 0xdfff, fenraya_videoram_w, videoram, videoram_size );
	return true;
}

private boolean readport() {
	PR_START( 0x00, 0x00, input_port_0_r );
	PR_ADD( 0x01, 0x01, input_port_1_r );
	PR_ADD( 0x02, 0x02, input_port_2_r );
	return true;
}

private boolean ipt_4enraya() {
	PORT_START();
	PORT_DIPNAME( 0x01, 0x01, DEF_STR2( Difficulty ) );
	PORT_DIPSETTING(    0x01, "Easy" );
	PORT_DIPSETTING(    0x00, "Hard" );
	PORT_DIPNAME( 0x02, 0x00, DEF_STR2( Demo_Sounds ) );
	PORT_DIPSETTING(    0x02, DEF_STR2( Off ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( On ) );
	PORT_DIPNAME( 0x04, 0x04, "Pieces" );
	PORT_DIPSETTING(    0x04, "30" );
	PORT_DIPSETTING(    0x00, "16" );
	PORT_DIPNAME( 0x08, 0x08, "Speed" );
	PORT_DIPSETTING(    0x08, "Slow" );
	PORT_DIPSETTING(    0x00, "Fast" );
	PORT_DIPNAME( 0x30, 0x30, DEF_STR2( Coin_B ) );
	PORT_DIPSETTING(    0x30, DEF_STR2( _1C_1C ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( _2C_3C ) );
	PORT_DIPSETTING(    0x10, DEF_STR2( _1C_3C ) );
	PORT_DIPSETTING(    0x20, DEF_STR2( _1C_4C ) );
	PORT_DIPNAME( 0xc0, 0xc0, DEF_STR2( Coin_A ) );
	PORT_DIPSETTING(    0x40, DEF_STR2( _2C_1C ) );
	PORT_DIPSETTING(    0xc0, DEF_STR2( _1C_1C ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( _2C_3C ) );
	PORT_DIPSETTING(    0x80, DEF_STR2( _1C_2C ) );

	PORT_START();
	PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_2WAY | IPF_PLAYER2 );
	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_2WAY | IPF_PLAYER1 );
	PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER1 );				// "drop" ("down")
	PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_2WAY | IPF_PLAYER2 );
	PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER2 );				// "drop" ("down")
	PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_2WAY | IPF_PLAYER1 );
	PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER1 );				// "fire" ("shot")
	PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER2 );				// "fire" ("shot")

	PORT_START();
	PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_UNUSED );
	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_COIN1 );
	PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_UNUSED );
	PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_UNUSED );
	PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_UNUSED );
	PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_START2 );
	PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_START1 );
	PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_COIN2 );
	return true;
}

int[][] charlayout =
{
	{8},{8},	/* 8*8 characters */
	{1024},	/* 1024 characters */
	{3},	/* 3 bits per pixel */
	{ 0*1024*8*8, 2*1024*8*8, 1*1024*8*8 },	/* the bitplanes are separated */
	{ 0, 1, 2, 3, 4, 5, 6, 7 },
	{ 0*8, 1*8, 2*8, 3*8, 4*8, 5*8, 6*8, 7*8 },
	{8*8}	/* every char takes 8 consecutive bytes */
};

private boolean gfxdecodeinfo()
{
	GDI_ADD( REGION_GFX1, 0, charlayout,     0, 8 );
	GDI_ADD( -1 ); /* end of array */
	return true;
};

public boolean mdrv_4enraya() {

	/* basic machine hardware */
	MDRV_CPU_ADD(Z80,8000000/2);
	MDRV_CPU_MEMORY(readmem(),writemem());
	MDRV_CPU_PORTS(readport(),0);
	MDRV_CPU_VBLANK_INT(irq0_line_hold,4);

	MDRV_FRAMES_PER_SECOND(60);
	MDRV_VBLANK_DURATION(DEFAULT_60HZ_VBLANK_DURATION);

	/* video hardware */
	MDRV_VIDEO_ATTRIBUTES(VIDEO_TYPE_RASTER);
	MDRV_SCREEN_SIZE(32*8, 32*8);
	MDRV_VISIBLE_AREA(0*8, 32*8-1, 2*8, 30*8-1);
	MDRV_GFXDECODE(gfxdecodeinfo());
	MDRV_PALETTE_LENGTH(512);

	MDRV_VIDEO_START(_4enraya_vs);
	MDRV_VIDEO_UPDATE(_4enraya_vu);

	/* sound hardware */
	//MDRV_SOUND_ADD(AY8910, ay8910_interface);
	return true;
}

/***************************************************************************

  Game driver(s)

***************************************************************************/

private boolean rom_4enraya() {
	ROM_REGION( 0x10000, REGION_CPU1, 0 );
	ROM_LOAD( "5.bin",   0x0000, 0x8000, 0xcf1cd151 );
	ROM_LOAD( "4.bin",   0x8000, 0x4000, 0xf9ec1be7 );

	ROM_REGION( 0x20000, REGION_GFX1, ROMREGION_DISPOSE );
	ROM_LOAD( "1.bin",   0x0000, 0x2000, 0x87f92552 );
	ROM_LOAD( "2.bin",   0x2000, 0x2000, 0x2b0a3793 );
	ROM_LOAD( "3.bin",   0x4000, 0x2000, 0xf6940836 );

	ROM_REGION( 0x0020,  REGION_PROMS, 0 );
	ROM_LOAD( "1.bpr",   0x0000, 0x0020, 0x0dcbd2352 );	/* not used */
	return true;
}

	public Machine getMachine(URL url, String name) {
		super.getMachine(url,name);
		m = new jef.machine.BasicMachine();
		super.setVideoEmulator(v);

		if (name.equals("4enraya"))
			GAME( 1990, rom_4enraya(),  0,       mdrv_4enraya(),  ipt_4enraya(),  0, ROT0, "IDSA", "4 En Raya" );

		m.init(md);
		return (Machine)m;
	}

}