/*
CottAGE - the Arcade Generic Emulator in Java

Java driver by Erik Duijs, Gollum
*/

package cottage.drivers;

import java.net.URL;

import jef.machine.Machine;
import jef.map.InterruptHandler;
import jef.map.WriteHandler;
import jef.sound.chip.SN76496;
import jef.video.Vh_convert_color_proms;
import jef.video.Vh_refresh;
import jef.video.Vh_start;

import cottage.mame.MAMEDriver;

/***************************************************************************

Based on drivers from Juno First emulator by Chris Hardy (chrish@kcbbs.gen.nz)

***************************************************************************/
public class Circusc extends MAMEDriver {

cottage.vidhrdw.Circusc v			= new cottage.vidhrdw.Circusc();
SN76496 				sn			= new SN76496(18432000/12);
WriteHandler 			videoram_w	= v.videoram_w();
WriteHandler 			colorram_w	= v.colorram_w();
Vh_start				circusc_vs  = (Vh_start)v;
Vh_refresh 				circusc_vu	= (Vh_refresh)v;
Vh_convert_color_proms  circusc_pi	= (Vh_convert_color_proms)v;
//WriteHandler soundlatch_w	  		= (WriteHandler) new SoundLatch_w();
//WriteHandler SN76496_0_w	 	 	= (WriteHandler) new SN76496_write();

cottage.machine.Konami m 		= new cottage.machine.Konami();
InterruptHandler irq0_line_hold = m.irq0_line_hold();
WriteHandler interrupt_enable_w = m.interrupt_enable();

private boolean readmem() {
	MR_START();
	MR_ADD( 0x1000, 0x1000, input_port_0_r ); /* IO Coin */
	MR_ADD( 0x1001, 0x1001, input_port_1_r ); /* P1 IO */
	MR_ADD( 0x1002, 0x1002, input_port_2_r ); /* P2 IO */
	MR_ADD( 0x1400, 0x1400, input_port_3_r ); /* DIP 1 */
	MR_ADD( 0x1800, 0x1800, input_port_4_r ); /* DIP 2 */
	MR_ADD( 0x2000, 0x39ff, MRA_RAM );
	MR_ADD( 0x3a00, 0x3fff, MRA_RAM ); /* Unmapped memory */
	MR_ADD( 0x6000, 0xffff, MRA_ROM );
	return true;
}

private boolean writemem() {
	MW_START();
	//MW_ADD( 0x0000, 0x0000, circusc_flipscreen_w );
	MW_ADD( 0x0001, 0x0001, interrupt_enable_w );
	//MW_ADD( 0x0003, 0x0004, circusc_coin_counter_w );  /* Coin counters */
	MW_ADD( 0x0005, 0x0005, MWA_RAM ); //, circusc_spritebank
	//MW_ADD( 0x0400, 0x0400, watchdog_reset_w );
	//MW_ADD( 0x0800, 0x0800, soundlatch_w );
	//MW_ADD( 0x0c00, 0x0c00, circusc_sh_irqtrigger_w );  /* cause interrupt on audio CPU */
	MW_ADD( 0x1c00, 0x1c00, MWA_RAM );	//, circusc_scroll
	MW_ADD( 0x2000, 0x2fff, MWA_RAM );
	MW_ADD( 0x3000, 0x33ff, colorram_w, colorram );
	MW_ADD( 0x3400, 0x37ff, videoram_w, videoram, videoram_size );
	MW_ADD( 0x3800, 0x38ff, MWA_RAM, spriteram_2 );
	MW_ADD( 0x3900, 0x39ff, MWA_RAM, spriteram, spriteram_size );
	MW_ADD( 0x3a00, 0x3fff, MWA_RAM );
	MW_ADD( 0x6000, 0xffff, MWA_ROM );
	return true;
}

private boolean ipt_circusc() {
	PORT_START();      /* IN0 */
	PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_COIN1 );
	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_COIN2 );
	PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_SERVICE1 );
	PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_START1 );
	PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_START2 );
	PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );
	PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
	PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );

	PORT_START();      /* IN1 */
	PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_2WAY );
	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_2WAY );
	PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_UNKNOWN );
	PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_UNKNOWN );
	PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 );
	PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );
	PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
	PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );

	PORT_START();      /* IN2 */
	PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_2WAY | IPF_COCKTAIL );
	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_2WAY | IPF_COCKTAIL );
	PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_UNKNOWN );
	PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_UNKNOWN );
	PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_COCKTAIL );
	PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );
	PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
	PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
	PORT_START();      /* DSW0 */

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
	PORT_DIPSETTING(    0x00, DEF_STR2( Free_Play ) );

	PORT_START();      /* DSW1 */
	PORT_DIPNAME( 0x03, 0x03, DEF_STR2( Lives ) );
	PORT_DIPSETTING(    0x03, "3" );
	PORT_DIPSETTING(    0x02, "4" );
	PORT_DIPSETTING(    0x01, "5" );
	PORT_DIPSETTING(    0x00, "7" );
	PORT_DIPNAME( 0x04, 0x00, DEF_STR2( Cabinet ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( Upright ) );
	PORT_DIPSETTING(    0x04, DEF_STR2( Cocktail ) );
	PORT_DIPNAME( 0x08, 0x08, DEF_STR2( Bonus_Life ) );
	PORT_DIPSETTING(    0x08, "20000 70000" );
	PORT_DIPSETTING(    0x00, "30000 80000" );
	PORT_DIPNAME( 0x10, 0x10, DEF_STR2( Unknown ) );
	PORT_DIPSETTING(    0x10, DEF_STR2( Off ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( On ) );
	PORT_DIPNAME( 0x60, 0x40, DEF_STR2( Difficulty ) );
	PORT_DIPSETTING(    0x60, "Easy" );
	PORT_DIPSETTING(    0x40, "Normal" );
	PORT_DIPSETTING(    0x20, "Hard" );
	PORT_DIPSETTING(    0x00, "Hardest" );
	PORT_DIPNAME( 0x80, 0x00, DEF_STR2( Demo_Sounds ) );
	PORT_DIPSETTING(    0x80, DEF_STR2( Off ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( On ) );
	return true;
}

int[][] circusc_charlayout =
{
	{8},{8},	/* 8*8 sprites */
	{512},	/* 1024 characters */
	{4},	/* 4 bits per pixel */
	{ 0, 1, 2, 3 },
	{ 0*4, 1*4, 2*4, 3*4, 4*4, 5*4, 6*4, 7*4  },
	{ 0*32, 1*32, 2*32, 3*32, 4*32, 5*32, 6*32, 7*32 },
	{ 32*8 }	/* every sprite takes 64 consecutive bytes */
};
int[][] circusc_spritelayout =
{
	{16},{16},  /* 16*16 sprites */
	{384},    /* 384 sprites */
	{4},      /* 4 bits per pixel */
	{ 0, 1, 2, 3 },        /* the bitplanes are packed */
	{ 0*4, 1*4, 2*4, 3*4, 4*4, 5*4, 6*4, 7*4,
			8*4, 9*4, 10*4, 11*4, 12*4, 13*4, 14*4, 15*4 },
	{ 0*4*16, 1*4*16, 2*4*16, 3*4*16, 4*4*16, 5*4*16, 6*4*16, 7*4*16,
			8*4*16, 9*4*16, 10*4*16, 11*4*16, 12*4*16, 13*4*16, 14*4*16, 15*4*16 },
	{ 32*4*8 }    /* every sprite takes 128 consecutive bytes */
};

private boolean gfxdecodeinfo()
{
	GDI_ADD( REGION_GFX1, 0, circusc_charlayout, 	   0, 16 );
	GDI_ADD( REGION_GFX2, 0, circusc_spritelayout, 16*16, 16 );
	GDI_ADD( -1 );
	return true;
};

public boolean mdrv_circusc() {
	/* basic machine hardware */
	MDRV_CPU_ADD(M6809, 2048000);        /* 2 MHz */
	MDRV_CPU_MEMORY(readmem(),writemem());
	MDRV_CPU_VBLANK_INT(irq0_line_hold,1);

	//MDRV_CPU_ADD(Z80,14318180/4);
	//MDRV_CPU_FLAGS(CPU_AUDIO_CPU);     /* Z80 Clock is derived from a 14.31818 MHz crystal */
	//MDRV_CPU_MEMORY(sound_readmem,sound_writemem);

	/* sound hardware */
	//MDRV_SOUND_ADD((SoundChipEmulator)sn);
	//MDRV_SOUND_ADD(DAC, dac_interface);

	MDRV_FRAMES_PER_SECOND(60);
	MDRV_VBLANK_DURATION(DEFAULT_60HZ_VBLANK_DURATION);

	/* video hardware */
	MDRV_VIDEO_ATTRIBUTES(VIDEO_TYPE_RASTER);
	MDRV_SCREEN_SIZE(32*8, 32*8);
	MDRV_VISIBLE_AREA(0*8, 32*8-1, 2*8, 30*8-1);
	MDRV_GFXDECODE(gfxdecodeinfo());
	MDRV_PALETTE_LENGTH(32);
	MDRV_COLORTABLE_LENGTH(16*16+16*16);

	MDRV_PALETTE_INIT(circusc_pi);
	MDRV_VIDEO_START(circusc_vs);
	MDRV_VIDEO_UPDATE(circusc_vu);

	return true;
}

/***************************************************************************

  Game driver(s)

***************************************************************************/
private boolean rom_circusc() {
	ROM_REGION( 2*0x10000, REGION_CPU1, 0 );	 /* 64k for code + 64k for decrypted opcodes */
	ROM_LOAD( "s05",          0x6000, 0x2000, 0x48feafcf );
	ROM_LOAD( "q04",          0x8000, 0x2000, 0xc283b887 );
	ROM_LOAD( "q03",          0xa000, 0x2000, 0xe90c0e86 );
	ROM_LOAD( "q02",          0xc000, 0x2000, 0x4d847dc6 );
	ROM_LOAD( "q01",          0xe000, 0x2000, 0x18c20adf );

	//ROM_REGION( 0x10000, REGION_CPU2, 0 );	/* 64k for the audio CPU */
	//ROM_LOAD( "cd05_l14.bin", 0x0000, 0x2000, 0x607df0fb );
	//ROM_LOAD( "cd07_l15.bin", 0x2000, 0x2000, 0xa6ad30e1 );

	ROM_REGION( 0x04000, REGION_GFX1, ROMREGION_DISPOSE );
	ROM_LOAD( "a04_j12.bin",  0x0000, 0x2000, 0x56e5b408 );
	ROM_LOAD( "a05_k13.bin",  0x2000, 0x2000, 0x5aca0193 );

	ROM_REGION( 0x0c000, REGION_GFX2, ROMREGION_DISPOSE );
	ROM_LOAD( "e11_j06.bin",  0x0000, 0x2000, 0xdf0405c6 );
	ROM_LOAD( "e12_j07.bin",  0x2000, 0x2000, 0x23dfe3a6 );
	ROM_LOAD( "e13_j08.bin",  0x4000, 0x2000, 0x3ba95390 );
	ROM_LOAD( "e14_j09.bin",  0x6000, 0x2000, 0xa9fba85a );
	ROM_LOAD( "e15_j10.bin",  0x8000, 0x2000, 0x0532347e );
	ROM_LOAD( "e16_j11.bin",  0xa000, 0x2000, 0xe1725d24 );

	ROM_REGION( 0x0220, REGION_PROMS, 0 );
	ROM_LOAD( "a02_j18.bin",  0x0000, 0x020, 0x10dd4eaa ); /* palette */
	ROM_LOAD( "c10_j16.bin",  0x0020, 0x100, 0xc244f2aa ); /* character lookup table */
	ROM_LOAD( "b07_j17.bin",  0x0120, 0x100, 0x13989357 ); /* sprite lookup table */
	return true;
}

private boolean rom_circusc2() {
	ROM_REGION( 2*0x10000, REGION_CPU1, 0 );     /* 64k for code + 64k for decrypted opcodes */
	ROM_LOAD( "h03_r05.bin",  0x6000, 0x2000, 0xed52c60f );
	ROM_LOAD( "h04_n04.bin",  0x8000, 0x2000, 0xfcc99e33 );
	ROM_LOAD( "h05_n03.bin",  0xa000, 0x2000, 0x5ef5b3b5 );
	ROM_LOAD( "h06_n02.bin",  0xc000, 0x2000, 0xa5a5e796 );
	ROM_LOAD( "h07_n01.bin",  0xe000, 0x2000, 0x70d26721 );

	//ROM_REGION( 0x10000, REGION_CPU2, 0 );     /* 64k for the audio CPU */
	//ROM_LOAD( "cd05_l14.bin", 0x0000, 0x2000, 0x607df0fb );
	//ROM_LOAD( "cd07_l15.bin", 0x2000, 0x2000, 0xa6ad30e1 );

	ROM_REGION( 0x04000, REGION_GFX1, ROMREGION_DISPOSE );
	ROM_LOAD( "a04_j12.bin",  0x0000, 0x2000, 0x56e5b408 );
	ROM_LOAD( "a05_k13.bin",  0x2000, 0x2000, 0x5aca0193 );

	ROM_REGION( 0x0c000, REGION_GFX2, ROMREGION_DISPOSE );
	ROM_LOAD( "e11_j06.bin",  0x0000, 0x2000, 0xdf0405c6 );
	ROM_LOAD( "e12_j07.bin",  0x2000, 0x2000, 0x23dfe3a6 );
	ROM_LOAD( "e13_j08.bin",  0x4000, 0x2000, 0x3ba95390 );
	ROM_LOAD( "e14_j09.bin",  0x6000, 0x2000, 0xa9fba85a );
	ROM_LOAD( "e15_j10.bin",  0x8000, 0x2000, 0x0532347e );
	ROM_LOAD( "e16_j11.bin",  0xa000, 0x2000, 0xe1725d24 );

	ROM_REGION( 0x0220, REGION_PROMS, 0 );
	ROM_LOAD( "a02_j18.bin",  0x0000, 0x020, 0x10dd4eaa ); /* palette */
	ROM_LOAD( "c10_j16.bin",  0x0020, 0x100, 0xc244f2aa ); /* character lookup table */
	ROM_LOAD( "b07_j17.bin",  0x0120, 0x100, 0x13989357 ); /* sprite lookup table */
	return true;
}

private boolean rom_circuscc() {
	ROM_REGION( 2*0x10000, REGION_CPU1, 0 );     /* 64k for code + 64k for decrypted opcodes */
	ROM_LOAD( "cc_u05.h3",    0x6000, 0x2000, 0x964c035a );
	ROM_LOAD( "p04",          0x8000, 0x2000, 0xdd0c0ee7 );
	ROM_LOAD( "p03",          0xa000, 0x2000, 0x190247af );
	ROM_LOAD( "p02",          0xc000, 0x2000, 0x7e63725e );
	ROM_LOAD( "p01",          0xe000, 0x2000, 0xeedaa5b2 );

	//ROM_REGION( 0x10000, REGION_CPU2, 0 );     /* 64k for the audio CPU */
	//ROM_LOAD( "cd05_l14.bin", 0x0000, 0x2000, 0x607df0fb );
	//ROM_LOAD( "cd07_l15.bin", 0x2000, 0x2000, 0xa6ad30e1 );

	ROM_REGION( 0x04000, REGION_GFX1, ROMREGION_DISPOSE );
	ROM_LOAD( "a04_j12.bin",  0x0000, 0x2000, 0x56e5b408 );
	ROM_LOAD( "a05_k13.bin",  0x2000, 0x2000, 0x5aca0193 );

	ROM_REGION( 0x0c000, REGION_GFX2, ROMREGION_DISPOSE );
	ROM_LOAD( "e11_j06.bin",  0x0000, 0x2000, 0xdf0405c6 );
	ROM_LOAD( "e12_j07.bin",  0x2000, 0x2000, 0x23dfe3a6 );
	ROM_LOAD( "e13_j08.bin",  0x4000, 0x2000, 0x3ba95390 );
	ROM_LOAD( "e14_j09.bin",  0x6000, 0x2000, 0xa9fba85a );
	ROM_LOAD( "e15_j10.bin",  0x8000, 0x2000, 0x0532347e );
	ROM_LOAD( "e16_j11.bin",  0xa000, 0x2000, 0xe1725d24 );

	ROM_REGION( 0x0220, REGION_PROMS, 0 );
	ROM_LOAD( "a02_j18.bin",  0x0000, 0x020, 0x10dd4eaa ); /* palette */
	ROM_LOAD( "c10_j16.bin",  0x0020, 0x100, 0xc244f2aa ); /* character lookup table */
	ROM_LOAD( "b07_j17.bin",  0x0120, 0x100, 0x13989357 ); /* sprite lookup table */
	return true;
}

private boolean rom_circusce() {
	ROM_REGION( 2*0x10000, REGION_CPU1, 0 );     /* 64k for code + 64k for decrypted opcodes */
	ROM_LOAD( "p05",          0x6000, 0x2000, 0x7ca74494 );
	ROM_LOAD( "p04",          0x8000, 0x2000, 0xdd0c0ee7 );
	ROM_LOAD( "p03",          0xa000, 0x2000, 0x190247af );
	ROM_LOAD( "p02",          0xc000, 0x2000, 0x7e63725e );
	ROM_LOAD( "p01",          0xe000, 0x2000, 0xeedaa5b2 );

	//ROM_REGION( 0x10000, REGION_CPU2, 0 );     /* 64k for the audio CPU */
	//ROM_LOAD( "cd05_l14.bin", 0x0000, 0x2000, 0x607df0fb );
	//ROM_LOAD( "cd07_l15.bin", 0x2000, 0x2000, 0xa6ad30e1 );

	ROM_REGION( 0x04000, REGION_GFX1, ROMREGION_DISPOSE );
	ROM_LOAD( "a04_j12.bin",  0x0000, 0x2000, 0x56e5b408 );
	ROM_LOAD( "a05_k13.bin",  0x2000, 0x2000, 0x5aca0193 );

	ROM_REGION( 0x0c000, REGION_GFX2, ROMREGION_DISPOSE );
	ROM_LOAD( "e11_j06.bin",  0x0000, 0x2000, 0xdf0405c6 );
	ROM_LOAD( "e12_j07.bin",  0x2000, 0x2000, 0x23dfe3a6 );
	ROM_LOAD( "e13_j08.bin",  0x4000, 0x2000, 0x3ba95390 );
	ROM_LOAD( "e14_j09.bin",  0x6000, 0x2000, 0xa9fba85a );
	ROM_LOAD( "e15_j10.bin",  0x8000, 0x2000, 0x0532347e );
	ROM_LOAD( "e16_j11.bin",  0xa000, 0x2000, 0xe1725d24 );

	ROM_REGION( 0x0220, REGION_PROMS, 0 );
	ROM_LOAD( "a02_j18.bin",  0x0000, 0x020, 0x10dd4eaa ); /* palette */
	ROM_LOAD( "c10_j16.bin",  0x0020, 0x100, 0xc244f2aa ); /* character lookup table */
	ROM_LOAD( "b07_j17.bin",  0x0120, 0x100, 0x13989357 ); /* sprite lookup table */
	return true;
}


private boolean init_circusc() {
	m.konami1_decode(memory_region(0));
	return true;
}


	public Machine getMachine(URL url, String name) {
		super.getMachine(url,name);
		super.setVideoEmulator(v);

		if (name.equals("circusc")) {
			GAME( 1984, rom_circusc(),          0, mdrv_circusc(), ipt_circusc(), 0/*init_circusc()*/, ROT90, "Konami", "Circus Charlie" );
		} else if (name.equals("circusc2")) {
			GAME( 1984, rom_circusc2(), "circusc", mdrv_circusc(), ipt_circusc(), 0/*init_circusc()*/, ROT90, "Konami", "Circus Charlie (no level select)" );
		} else if (name.equals("circuscc")) {
			GAME( 1984, rom_circuscc(), "circusc", mdrv_circusc(), ipt_circusc(), 0/*init_circusc()*/, ROT90, "Konami (Centuri licence)", "Circus Charlie (Centuri)" );
		} else if (name.equals("circusce")) {
			GAME( 1984, rom_circusce(), "circusc", mdrv_circusc(), ipt_circusc(), 0/*init_circusc()*/, ROT90, "Konami (Centuri licence)", "Circus Charlie (Centuri, earlier)" );
		}

		m.init(md);

		// In the GAME(..) simulated macro, the init_circusc is executed before the roms
		// are actually loaded so the ROM patch doesn't have effect.
		// This hacks the ROM patch in there.
		init_circusc();

		return (Machine)m;
	}

}

