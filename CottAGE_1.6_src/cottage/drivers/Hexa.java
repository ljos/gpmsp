/*
CottAGE - the Arcade Generic Emulator in Java

Java driver by Gollum, Erik Duijs
*/

/****************************************************************************

HEXA

driver by Howie Cohen

Memory map (prelim)
0000 7fff ROM
8000 bfff bank switch rom space??
c000 c7ff RAM
e000 e7ff video ram
e800-efff unused RAM

read:
d001      AY8910 read
f000      ???????

write:
d000      AY8910 control
d001      AY8910 write
d008      bit0/1 = flip screen x/y
          bit 4 = ROM bank??
          bit 5 = char bank
		  other bits????????
d010      watchdog reset, or IRQ acknowledge, or both
f000      ????????

NOTES:
        Needs High score save and load (i think the high score is stored
                around 0xc709)

*************************************************************************/

package cottage.drivers;

import java.net.URL;

import jef.machine.Machine;
import jef.map.InterruptHandler;
import jef.map.WriteHandler;
import jef.sound.SoundChipEmulator;
import jef.sound.chip.AY8910;
import jef.video.GfxManager;
import jef.video.Vh_refresh;
import jef.video.Vh_start;

import cottage.mame.Driver;
import cottage.mame.MAMEConstants;
import cottage.mame.MAMEDriver;

public class Hexa extends MAMEDriver implements Driver, MAMEConstants {

AY8910	ay8910 = new AY8910 ( 	1,			  /* 1 chip */
								1500000 );	/* 1.5 MHz ???? */

cottage.vidhrdw.Hexa v = new cottage.vidhrdw.Hexa();
WriteHandler hexa_d008_w = v.hexa_d008_w();
Vh_refresh hexa_vu = (Vh_refresh)v;
Vh_start generic_vs = (Vh_start)v;
WriteHandler videoram_w = v.videoram_w();
WriteHandler	 AY8910_write_port_0_w   = ay8910.AY8910_write_port_0_w();
WriteHandler	 AY8910_control_port_0_w = ay8910.AY8910_control_port_0_w();

jef.machine.BasicMachine m = new jef.machine.BasicMachine();
InterruptHandler irq0_line_hold = m.irq0_line_hold();

private boolean readmem() {
	MR_START( 0x0000, 0x7fff, MRA_ROM );
	//MR_ADD( 0x8000, 0xbfff, MRA_BANK1 );
	MR_ADD( 0x8000, 0xbfff, MRA_RAM );
	MR_ADD( 0xc000, 0xc7ff, MRA_RAM );
	//MR_ADD( 0xd001, 0xd001, AY8910_read_port_0_r );
	MR_ADD( 0xd001, 0xd001, input_port_0_r );
	MR_ADD( 0xe000, 0xe7ff, MRA_RAM );
	return true;
}

private boolean writemem() {
	MW_START( 0x0000, 0xbfff, MWA_ROM );
	MW_ADD( 0xc000, 0xc7ff, MWA_RAM );
	MW_ADD( 0xd000, 0xd000, AY8910_control_port_0_w );
	MW_ADD( 0xd001, 0xd001, AY8910_write_port_0_w );
	MW_ADD( 0xd008, 0xd008, hexa_d008_w );
	/*MW_ADD( 0xd010, 0xd010, watchdog_reset_w );*/	/* or IRQ acknowledge, or both */
	MW_ADD( 0xe000, 0xe7ff, videoram_w, videoram, videoram_size );
	return true;
}

private boolean ipt_hexa() {
	PORT_START();	/* IN0 */
	PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP | IPF_4WAY );
	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN | IPF_4WAY );
	PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT | IPF_4WAY );
	PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_4WAY );
	PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 );
	PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_START1 );
	PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_COIN1 );
	PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_BUTTON2 );

	PORT_START();	/* DSW */
	PORT_DIPNAME( 0x03, 0x03, DEF_STR2( Coinage ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( _3C_1C ) );
	PORT_DIPSETTING(    0x01, DEF_STR2( _2C_1C ) );
	PORT_DIPSETTING(    0x03, DEF_STR2( _1C_1C ) );
	PORT_DIPSETTING(    0x02, DEF_STR2( _1C_2C ) );
	PORT_DIPNAME( 0x04, 0x00, "Naughty Pics" );
	PORT_DIPSETTING(    0x04, DEF_STR2( Off ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( On ) );
	PORT_DIPNAME( 0x08, 0x08, DEF_STR2( Flip_Screen ) );
	PORT_DIPSETTING(    0x08, DEF_STR2( Off ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( On ) );
	PORT_DIPNAME( 0x30, 0x30, "Difficulty?" );
	PORT_DIPSETTING(    0x30, "Easy?" );
	PORT_DIPSETTING(    0x20, "Medium?" );
	PORT_DIPSETTING(    0x10, "Hard?" );
	PORT_DIPSETTING(    0x00, "Hardest?" );
	PORT_DIPNAME( 0x40, 0x40, "Pobys" );
	PORT_DIPSETTING(    0x40, "2" );
	PORT_DIPSETTING(    0x00, "4" );
	PORT_DIPNAME( 0x80, 0x80, DEF_STR2( Demo_Sounds ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( Off ) );
	PORT_DIPSETTING(    0x80, DEF_STR2( On ) );
	return true;
}



int[][] charlayout =
{
	{8},{8},    /* 8 by 8 */
	{4096},   /* 4096 characters */
	{3},		/* 3 bits per pixel */
	{ 0, 4096*8*8, 2*4096*8*8 },	/* plane */
	{ 0, 1, 2, 3, 4, 5, 6, 7 },		/* x bit */
	{ 8*0, 8*1, 8*2, 8*3, 8*4, 8*5, 8*6, 8*7 }, 	/* y bit */
	{8*8}
};



private boolean gfxdecodeinfo()
{
	GDI_ADD( REGION_GFX1, 0x0000, charlayout,  0 , 32 );
	GDI_ADD( -1 ); /* end of array */
	return true;
};



public boolean mdrv_hexa() {

	/* basic machine hardware */
	MDRV_CPU_ADD(Z80, 4000000);		/* 4 MHz ??????? */
	MDRV_CPU_MEMORY(readmem(),writemem());
	MDRV_CPU_VBLANK_INT(irq0_line_hold,1);

	/* sound hardware */
	MDRV_SOUND_ADD((SoundChipEmulator)ay8910);

	MDRV_FRAMES_PER_SECOND(60);
	MDRV_VBLANK_DURATION(DEFAULT_60HZ_VBLANK_DURATION);

	/* video hardware */
	MDRV_VIDEO_ATTRIBUTES(VIDEO_TYPE_RASTER | GfxManager.VIDEO_SUPPORTS_DIRTY);
	MDRV_SCREEN_SIZE(32*8, 32*8);
	MDRV_VISIBLE_AREA(0*8, 32*8-1, 2*8, 30*8-1);
	MDRV_GFXDECODE(gfxdecodeinfo());
	MDRV_PALETTE_LENGTH(256);

	MDRV_PALETTE_INIT(v.RRRR_GGGG_BBBB());
	MDRV_VIDEO_START(generic_vs);
	MDRV_VIDEO_UPDATE(hexa_vu);

	return true;
};


/***************************************************************************

  Game driver(s)

***************************************************************************/

private boolean rom_hexa() {
	ROM_REGION( 0x18000, REGION_CPU1, 0 );		/* 64k for code + 32k for banked ROM */
	ROM_LOAD( "hexa.20",      0x00000, 0x8000, 0x98b00586 );
	ROM_LOAD( "hexa.21",      0x10000, 0x8000, 0x3d5d006c );

	ROM_REGION( 0x18000, REGION_GFX1, ROMREGION_DISPOSE );
	ROM_LOAD( "hexa.17",      0x00000, 0x8000, 0xf6911dd6 );
	ROM_LOAD( "hexa.18",      0x08000, 0x8000, 0x6e3d95d2 );
	ROM_LOAD( "hexa.19",      0x10000, 0x8000, 0xffe97a31 );

	ROM_REGION( 0x0300, REGION_PROMS, 0 );
	ROM_LOAD( "hexa.001",     0x0000, 0x0100, 0x88a055b4 );
	ROM_LOAD( "hexa.003",     0x0100, 0x0100, 0x3e9d4932 );
	ROM_LOAD( "hexa.002",     0x0200, 0x0100, 0xff15366c );
	return true;
}



private boolean init_hexa() {
	int[] RAM = memory_region(REGION_CPU1);


	/* Hexa is not protected or anything, but it keeps writing 0x3f to register */
	/* 0x07 of the AY8910, to read the input ports. This causes clicks in the */
	/* music since the output channels are continuously disabled and reenabled. */
	/* To avoid that, we just NOP out the 0x3f write. */
	RAM[0x0124] = 0x00;
	RAM[0x0125] = 0x00;
	RAM[0x0126] = 0x00;
	return true;
}

	public Machine getMachine(URL url, String name) {
		super.getMachine(url,name);
		super.setVideoEmulator(v);
		m = new jef.machine.BasicMachine();

		if (name.equals("hexa")) {
			GAME( "1986?", rom_hexa(), 0, mdrv_hexa(), ipt_hexa(), init_hexa(), ROT0, "D. R. Korea", "Hexa" );
		};

		m.init(md);
		return (Machine)m;
	}

}
