/*
CottAGE - the Arcade Generic Emulator in Java

Java driver by Gollum, Erik Duijs
*/

package cottage.drivers;

import java.net.URL;

import jef.machine.Machine;
import jef.map.InterruptHandler;
import jef.map.WriteHandler;
import jef.sound.SoundChipEmulator;
import jef.sound.chip.SN76496;
import jef.video.Vh_convert_color_proms;
import jef.video.Vh_refresh;
import jef.video.Vh_start;

import cottage.mame.Driver;
import cottage.mame.MAMEConstants;
import cottage.mame.MAMEDriver;

public class Pingpong extends MAMEDriver implements Driver, MAMEConstants {

cottage.vidhrdw.Pingpong v = new cottage.vidhrdw.Pingpong();

SN76496 sn = new SN76496(18432000/12);

Vh_start generic_vs = (Vh_start)v;
Vh_refresh pingpong_vu = (Vh_refresh)v;
Vh_convert_color_proms pingpong_pi = (Vh_convert_color_proms)v;
WriteHandler colorram_w = (WriteHandler)v.colorram_w();
WriteHandler videoram_w = (WriteHandler)v.videoram_w();
WriteHandler soundlatch_w	  = (WriteHandler) new SoundLatch_w();
WriteHandler SN76496_0_w	 	  = (WriteHandler) new SN76496_write();

jef.machine.BasicMachine m = new jef.machine.BasicMachine();
InterruptHandler irq0_line_hold = m.irq0_line_hold();

static int intenable;
static int sound_command = 0;

WriteHandler coin_w = (WriteHandler) new Coin_w();
InterruptHandler pingpong_interrupt = (InterruptHandler) new Pingpong_interrupt();

public class Coin_w implements WriteHandler {
	public void write(int offset, int data) {
		/* bit 2 = irq enable, bit 3 = nmi enable */
		intenable = data & 0x0c;

		/* bit 0/1 = coin counters */
		//coin_counter_w(0,data & 1);
		//coin_counter_w(1,data & 2);

		/* other bits unknown */
	}
}

class SoundLatch_w implements WriteHandler {
	public void write(int address, int data) {
		sound_command = data;
	}
}

class SN76496_write implements WriteHandler {
	public void write(int address, int data) {
		sn.command_w(sound_command);
	}
}

public class Pingpong_interrupt implements InterruptHandler {
	public int irq() {
		if (m.getCurrentSlice() == 0)
		{
			if ((intenable & 0x04) != 0) return 0;
		}
		else if ((m.getCurrentSlice() % 2) != 0)
		{
			if ((intenable & 0x08) != 0) return 1;
		}

		return -1; // ignore interrupt
	}
}

private boolean readmem() {
	MR_START( 0x0000, 0x7fff, MRA_ROM );
	MR_ADD( 0x8000, 0x87ff, MRA_RAM );
	MR_ADD( 0x9000, 0x97ff, MRA_RAM );
	MR_ADD( 0xa800, 0xa800, input_port_0_r );
	MR_ADD( 0xa880, 0xa880, input_port_1_r );
	MR_ADD( 0xa900, 0xa900, input_port_2_r );
	MR_ADD( 0xa980, 0xa980, input_port_3_r );
	return true;
}

private boolean writemem() {
	MW_START( 0x0000, 0x7fff, MWA_ROM );
	MW_ADD( 0x8000, 0x83ff, colorram_w, colorram );
	MW_ADD( 0x8400, 0x87ff, videoram_w, videoram, videoram_size );
	MW_ADD( 0x9000, 0x9002, MWA_RAM );
	MW_ADD( 0x9003, 0x9052, MWA_RAM, spriteram, spriteram_size );
	MW_ADD( 0x9053, 0x97ff, MWA_RAM );
	MW_ADD( 0xa000, 0xa000, coin_w );	/* coin counters + irq enables */
	MW_ADD( 0xa200, 0xa200, soundlatch_w );		/* SN76496 data latch */
	MW_ADD( 0xa400, 0xa400, SN76496_0_w );	/* trigger read */
	//MW_ADD( 0xa600, 0xa600, watchdog_reset_w );
	return true;
}

private boolean ipt_pingpong() {
	PORT_START();	/* IN0 */
	PORT_DIPNAME( 0x01, 0x01, DEF_STR2( Unused ) );
	PORT_DIPSETTING(    0x01, DEF_STR2( Off ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( On ) );
	PORT_DIPNAME( 0x02, 0x02, DEF_STR2( Unused ) );
	PORT_DIPSETTING(    0x02, DEF_STR2( Off ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( On ) );
	PORT_SERVICE( 0x04, IP_ACTIVE_LOW );
	PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_START2 );
	PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_START1 );
	PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_SERVICE1 );
	PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_COIN2 );
	PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_COIN1 );

	PORT_START();	/* IN1 */
	PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER2 );
	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT | IPF_2WAY | IPF_PLAYER2 );
	PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT| IPF_2WAY | IPF_PLAYER2 );
	PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER2 );
	PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON2 );
	PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_2WAY );
	PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_2WAY );
	PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_BUTTON1 );

	PORT_START();	/* DSW1 */
	PORT_DIPNAME( 0x0F, 0x0F, DEF_STR2( Coin_B ) );
	PORT_DIPSETTING(    0x04, DEF_STR2( _4C_1C ) );
	PORT_DIPSETTING(    0x0A, DEF_STR2( _3C_1C ) );
	PORT_DIPSETTING(    0x01, DEF_STR2( _2C_1C ) );
	PORT_DIPSETTING(    0x02, DEF_STR2( _3C_2C ) );
	PORT_DIPSETTING(    0x08, DEF_STR2( _4C_3C ) );
	PORT_DIPSETTING(    0x0F, DEF_STR2( _1C_1C ) );
	PORT_DIPSETTING(    0x0C, DEF_STR2( _3C_4C ) );
	PORT_DIPSETTING(    0x0E, DEF_STR2( _2C_3C ) );
	PORT_DIPSETTING(    0x07, DEF_STR2( _1C_2C ) );
	PORT_DIPSETTING(    0x06, DEF_STR2( _2C_5C ) );
	PORT_DIPSETTING(    0x0B, DEF_STR2( _1C_3C ) );
	PORT_DIPSETTING(    0x03, DEF_STR2( _1C_4C ) );
	PORT_DIPSETTING(    0x0D, DEF_STR2( _1C_5C ) );
	PORT_DIPSETTING(    0x05, DEF_STR2( _1C_6C ) );
	PORT_DIPSETTING(    0x09, DEF_STR2( _1C_7C ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( Free_Play ) );
	PORT_DIPNAME( 0xF0, 0xF0, DEF_STR2( Coin_A ) );
	PORT_DIPSETTING(    0x40, DEF_STR2( _4C_1C ) );
	PORT_DIPSETTING(    0xA0, DEF_STR2( _3C_1C ) );
	PORT_DIPSETTING(    0x10, DEF_STR2( _2C_1C ) );
	PORT_DIPSETTING(    0x20, DEF_STR2( _3C_2C ) );
	PORT_DIPSETTING(    0x80, DEF_STR2( _4C_3C ) );
	PORT_DIPSETTING(    0xF0, DEF_STR2( _1C_1C ) );
	PORT_DIPSETTING(    0xC0, DEF_STR2( _3C_4C ) );
	PORT_DIPSETTING(    0xE0, DEF_STR2( _2C_3C ) );
	PORT_DIPSETTING(    0x70, DEF_STR2( _1C_2C ) );
	PORT_DIPSETTING(    0x60, DEF_STR2( _2C_5C ) );
	PORT_DIPSETTING(    0xB0, DEF_STR2( _1C_3C ) );
	PORT_DIPSETTING(    0x30, DEF_STR2( _1C_4C ) );
	PORT_DIPSETTING(    0xD0, DEF_STR2( _1C_5C ) );
	PORT_DIPSETTING(    0x50, DEF_STR2( _1C_6C ) );
	PORT_DIPSETTING(    0x90, DEF_STR2( _1C_7C ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( Free_Play ) );

	PORT_START();	/* DSW2 */
	PORT_DIPNAME( 0x01, 0x00, DEF_STR2( Demo_Sounds ) );
	PORT_DIPSETTING(    0x01, DEF_STR2( Off ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( On ) );
	PORT_DIPNAME( 0x06, 0x06, DEF_STR2( Difficulty ) );
	PORT_DIPSETTING(    0x06, "Easy" );
	PORT_DIPSETTING(    0x02, "Normal" );
	PORT_DIPSETTING(    0x04, "Difficult" );
	PORT_DIPSETTING(    0x00, "Very Difficult" );
	PORT_DIPNAME( 0x08, 0x08, DEF_STR2( Unused ) );
	PORT_DIPSETTING(    0x08, DEF_STR2( Off ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( On ) );
	PORT_DIPNAME( 0x10, 0x10, DEF_STR2( Unused ) );
	PORT_DIPSETTING(    0x10, DEF_STR2( Off ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( On ) );
	PORT_DIPNAME( 0x20, 0x20, DEF_STR2( Unused ) );
	PORT_DIPSETTING(    0x20, DEF_STR2( Off ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( On ) );
	PORT_DIPNAME( 0x40, 0x40, DEF_STR2( Unused ) );
	PORT_DIPSETTING(    0x40, DEF_STR2( Off ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( On ) );
	PORT_DIPNAME( 0x80, 0x80, DEF_STR2( Unused ) );
	PORT_DIPSETTING(    0x80, DEF_STR2( Off ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( On ) );
	return true;
}

int[][] charlayout =
{
	{8},{8},		/* 8*8 characters */
	{512},		/* 512 characters */
	{2},		/* 2 bits per pixel */
	{ 4, 0 },	/* the bitplanes are packed in one nibble */
	{ 3, 2, 1, 0, 8*8+3, 8*8+2, 8*8+1, 8*8+0 },	/* x bit */
	{ 0*8, 1*8, 2*8, 3*8, 4*8, 5*8, 6*8, 7*8   },     /* y bit */
	{16*8}	/* every char takes 16 consecutive bytes */
};

int[][] spritelayout =
{
	{16},{16},		/* 16*16 sprites */
	{128},		/* 128 sprites */
	{2},		/* 2 bits per pixel */
	{ 4, 0 },	/* the bitplanes are packed in one nibble */
	{ 12*16+3,12*16+2,12*16+1,12*16+0,
	   8*16+3, 8*16+2, 8*16+1, 8*16+0,
	   4*16+3, 4*16+2, 4*16+1, 4*16+0,
	        3,      2,      1,      0 },			/* x bit */
	{  0*8,  1*8,  2*8,  3*8,  4*8,  5*8,  6*8,  7*8,
	  32*8, 33*8, 34*8, 35*8, 36*8, 37*8, 38*8, 39*8  },    /* y bit */
	{64*8}	/* every char takes 64 consecutive bytes */
};

private boolean gfxdecodeinfo()
{
	GDI_ADD( REGION_GFX1, 0, charlayout,         0, 64 );
	GDI_ADD( REGION_GFX2, 0, spritelayout,    64*4, 64 );
	GDI_ADD( -1 ); /* end of array */
	return true;
};

public boolean mdrv_pingpong() {

	/* basic machine hardware */
	MDRV_CPU_ADD(Z80,18432000/6);		/* 3.072 MHz (probably) */
	MDRV_CPU_MEMORY(readmem(),writemem());
	MDRV_CPU_VBLANK_INT(pingpong_interrupt,16);	/* 1 IRQ + 8 NMI */

	/* sound hardware */
	MDRV_SOUND_ADD((SoundChipEmulator)sn);

	MDRV_FRAMES_PER_SECOND(60);
	MDRV_VBLANK_DURATION(DEFAULT_60HZ_VBLANK_DURATION);

	/* video hardware */
	MDRV_VIDEO_ATTRIBUTES(VIDEO_TYPE_RASTER);
	MDRV_SCREEN_SIZE(32*8, 32*8);
	MDRV_VISIBLE_AREA(0*8, 32*8-1, 2*8, 30*8-1);
	MDRV_GFXDECODE(gfxdecodeinfo());
	MDRV_PALETTE_LENGTH(32);
	MDRV_COLORTABLE_LENGTH(64*4+64*4);

	MDRV_PALETTE_INIT(pingpong_pi);
	MDRV_VIDEO_START(generic_vs);
	MDRV_VIDEO_UPDATE(pingpong_vu);

	return true;
}

/***************************************************************************

  Game driver(s)

***************************************************************************/

private boolean rom_pingpong() {
	ROM_REGION( 0x10000, REGION_CPU1, 0 );	/* 64k for code */
	ROM_LOAD( "pp_e04.rom",   0x0000, 0x4000, 0x18552f8f );
	ROM_LOAD( "pp_e03.rom",   0x4000, 0x4000, 0xae5f01e8 );

	ROM_REGION( 0x2000, REGION_GFX1, ROMREGION_DISPOSE );
	ROM_LOAD( "pp_e01.rom",   0x0000, 0x2000, 0xd1d6f090 );

	ROM_REGION( 0x2000, REGION_GFX2, ROMREGION_DISPOSE );
	ROM_LOAD( "pp_e02.rom",   0x0000, 0x2000, 0x33c687e0 );

	ROM_REGION( 0x0220, REGION_PROMS, 0 );
	ROM_LOAD( "pingpong.3j",  0x0000, 0x0020, 0x3e04f06e ); /* palette (this might be bad) */
	ROM_LOAD( "pingpong.11j", 0x0020, 0x0100, 0x09d96b08 ); /* sprites */
	ROM_LOAD( "pingpong.5h",  0x0120, 0x0100, 0x8456046a ); /* characters */
	return true;
}

	public Machine getMachine(URL url, String name) {
		super.getMachine(url,name);
		super.setVideoEmulator(v);
		m = new jef.machine.BasicMachine();

		if (name.equals("pingpong")) {
			GAME( 1985, rom_pingpong(), 0, mdrv_pingpong(), ipt_pingpong(), 0, ROT0, "Konami", "Ping Pong" );
		}

		m.init(md);
		return (Machine)m;
	}

}