/*
CottAGE - the Arcade Generic Emulator in Java

Java driver by Gollum, Erik Duijs
*/

/***************************************************************************

Bank Panic memory map (preliminary)
Similar to Appoooh

driver by Nicola Salmoria

0000-dfff ROM
e000-e7ff RAM
f000-f3ff Video RAM #1
f400-f7ff Color RAM #1
f800-fbff Video RAM #2
fc00-ffff Color RAM #2

I/O
read:
00  IN0
01  IN1
02  IN2
04  DSW

write:
00  SN76496 #1
01  SN76496 #2
02  SN76496 #3
05  horizontal scroll
07  bit 0-1 = at least one of these two controls the playfield priority
    bit 2-3 = ?
    bit 4 = NMI enable
    bit 5 = flip screen
    bit 6-7 = ?

***************************************************************************/

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

import cottage.mame.MAMEDriver;

public class Bankp extends MAMEDriver {

cottage.vidhrdw.Bankp v = new cottage.vidhrdw.Bankp();
SN76496 sn0	= new SN76496(3867120);	/* ?? the main oscillator is 15.46848 MHz */
SN76496 sn1	= new SN76496(3867120);	/* ?? the main oscillator is 15.46848 MHz */
SN76496 sn2	= new SN76496(3867120);	/* ?? the main oscillator is 15.46848 MHz */
WriteHandler SN76496_0_w = sn0.sn76496_command_w();
WriteHandler SN76496_1_w = sn1.sn76496_command_w();
WriteHandler SN76496_2_w = sn2.sn76496_command_w();
Vh_start bankp_vs = (Vh_start)v;
Vh_refresh bankp_vu = (Vh_refresh)v;
Vh_convert_color_proms bankp_pi = (Vh_convert_color_proms)v;
int[] bankp_videoram2 = v.Fbankp_videoram2;
int[] bankp_colorram2 = v.Fbankp_colorram2;
WriteHandler bankp_videoram2_w = v.bankp_videoram2_w();
WriteHandler bankp_colorram2_w = v.bankp_colorram2_w();
WriteHandler bankp_scroll_w = v.bankp_scroll_w();
WriteHandler bankp_out_w = v.bankp_out_w();
WriteHandler videoram_w = v.videoram_w();
WriteHandler colorram_w = v.colorram_w();

jef.machine.BasicMachine m = new jef.machine.BasicMachine();
InterruptHandler nmi_line_pulse = m.nmi_interrupt_switched();

private boolean readmem() {
	MR_START( 0x0000, 0xdfff, MRA_ROM );
	MR_ADD( 0xe000, 0xe7ff, MRA_RAM );
	MR_ADD( 0xf000, 0xffff, MRA_RAM );
	return true;
}

private boolean writemem() {
	MW_START( 0x0000, 0xdfff, MWA_ROM );
	MW_ADD( 0xe000, 0xe7ff, MWA_RAM );
	MW_ADD( 0xf000, 0xf3ff, videoram_w, videoram, videoram_size );
	MW_ADD( 0xf400, 0xf7ff, colorram_w, colorram );
	MW_ADD( 0xf800, 0xfbff, bankp_videoram2_w, bankp_videoram2 );
	MW_ADD( 0xfc00, 0xffff, bankp_colorram2_w, bankp_colorram2 );
	return true;
}

private boolean readport() {
	PR_START( 0x00, 0x00, input_port_0_r );	/* IN0 */
	PR_ADD( 0x01, 0x01, input_port_1_r );	/* IN1 */
	PR_ADD( 0x02, 0x02, input_port_2_r );	/* IN2 */
	PR_ADD( 0x04, 0x04, input_port_3_r );	/* DSW */
	return true;
}

private boolean writeport() {
	PW_START( 0x00, 0x00, SN76496_0_w );
	PW_ADD( 0x01, 0x01, SN76496_1_w );
	PW_ADD( 0x02, 0x02, SN76496_2_w );
	PW_ADD( 0x05, 0x05, bankp_scroll_w );
	PW_ADD( 0x07, 0x07, bankp_out_w );
	return true;
}


private boolean ipt_bankp() {
	PORT_START();	/* IN0 */
	PORT_BIT( 0x01, IP_ACTIVE_HIGH, IPT_UNKNOWN );	/* probably unused */
	PORT_BIT( 0x02, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_2WAY );
	PORT_BIT( 0x04, IP_ACTIVE_HIGH, IPT_UNKNOWN );	/* probably unused */
	PORT_BIT( 0x08, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT | IPF_2WAY );
	PORT_BIT( 0x10, IP_ACTIVE_HIGH, IPT_BUTTON1 );
	PORT_BIT( 0x20, IP_ACTIVE_HIGH, IPT_COIN1 );
	PORT_BIT( 0x40, IP_ACTIVE_HIGH, IPT_COIN2 );
	PORT_BIT( 0x80, IP_ACTIVE_HIGH, IPT_BUTTON2 );

	PORT_START();	/* IN1 */
	PORT_BIT( 0x01, IP_ACTIVE_HIGH, IPT_UNKNOWN );	/* probably unused */
	PORT_BIT( 0x02, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_2WAY | IPF_COCKTAIL );
	PORT_BIT( 0x04, IP_ACTIVE_HIGH, IPT_UNKNOWN );	/* probably unused */
	PORT_BIT( 0x08, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT | IPF_2WAY | IPF_COCKTAIL );
	PORT_BIT( 0x10, IP_ACTIVE_HIGH, IPT_BUTTON1 | IPF_COCKTAIL );
	PORT_BIT( 0x20, IP_ACTIVE_HIGH, IPT_START1 );
	PORT_BIT( 0x40, IP_ACTIVE_HIGH, IPT_START2 );
	PORT_BIT( 0x80, IP_ACTIVE_HIGH, IPT_BUTTON2 | IPF_COCKTAIL );

	PORT_START();	/* IN2 */
	PORT_BIT( 0x01, IP_ACTIVE_HIGH, IPT_BUTTON3 );
	PORT_BIT( 0x02, IP_ACTIVE_HIGH, IPT_BUTTON3 | IPF_COCKTAIL );
	PORT_BIT( 0x04, IP_ACTIVE_HIGH, IPT_COIN3 );
	PORT_BIT( 0xf8, IP_ACTIVE_HIGH, IPT_UNKNOWN );	/* probably unused */

	PORT_START();	/* DSW */
	PORT_DIPNAME( 0x03, 0x00, "Coin A/B" );
	PORT_DIPSETTING(    0x03, DEF_STR2( _3C_1C ) );
	PORT_DIPSETTING(    0x02, DEF_STR2( _2C_1C ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( _1C_1C ) );
	PORT_DIPSETTING(    0x01, DEF_STR2( _1C_2C ) );
	PORT_DIPNAME( 0x04, 0x00, "Coin C" );
	PORT_DIPSETTING(    0x04, DEF_STR2( _2C_1C ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( _1C_1C ) );
	PORT_DIPNAME( 0x08, 0x00, DEF_STR2( Lives ) );
	PORT_DIPSETTING(    0x00, "3" );
	PORT_DIPSETTING(    0x08, "4" );
	PORT_DIPNAME( 0x10, 0x00, DEF_STR2( Bonus_Life ) );
	PORT_DIPSETTING(    0x00, "70K 200K 500K ..." );
	PORT_DIPSETTING(    0x10, "100K 400K 800K ..." );
	PORT_DIPNAME( 0x20, 0x00, DEF_STR2( Difficulty ) );
	PORT_DIPSETTING(    0x00, "Easy" );
	PORT_DIPSETTING(    0x20, "Hard" );
	PORT_DIPNAME( 0x40, 0x40, DEF_STR2( Demo_Sounds ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( Off ) );
	PORT_DIPSETTING(    0x40, DEF_STR2( On ) );
	PORT_DIPNAME( 0x80, 0x80, DEF_STR2( Cabinet ) );
	PORT_DIPSETTING(    0x80, DEF_STR2( Upright ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( Cocktail ) );
	return true;
}


int[][] charlayout =
{
	{8},{8},	/* 8*8 characters */
	{1024},	/* 1024 characters */
	{2},	/* 2 bits per pixel */
	{ 0, 4 },	/* the bitplanes are packed in one byte */
	{ 8*8+3, 8*8+2, 8*8+1, 8*8+0, 3, 2, 1, 0 },
	{ 0*8, 1*8, 2*8, 3*8, 4*8, 5*8, 6*8, 7*8 },
	{16*8}	/* every char takes 8 consecutive bytes */
};
int[][] charlayout2 =
{
	{8},{8},	/* 8*8 characters */
	{2048},	/* 2048 characters */
	{3},	/* 3 bits per pixel */
	{ 2*2048*8*8, 2048*8*8, 0 },	/* the bitplanes are separated */
	{ 7, 6, 5, 4, 3, 2, 1, 0 },
	{ 0*8, 1*8, 2*8, 3*8, 4*8, 5*8, 6*8, 7*8 },
	{8*8}	/* every char takes 8 consecutive bytes */
};

private boolean gfxdecodeinfo() {
	GDI_ADD( REGION_GFX1, 0, charlayout,      0, 32 );
	GDI_ADD( REGION_GFX2, 0, charlayout2,  32*4, 16 );
	GDI_ADD( -1 ); /* end of array */
	return true;
};


public boolean mdrv_bankp() {

	/* basic machine hardware */
	MDRV_CPU_ADD(Z80, 3867120);	/* ?? the main oscillator is 15.46848 MHz */
	MDRV_CPU_MEMORY(readmem(),writemem());
	MDRV_CPU_PORTS(readport(),writeport());
	MDRV_CPU_VBLANK_INT(nmi_line_pulse,1);

	/* sound hardware */
	MDRV_SOUND_ADD((SoundChipEmulator)sn0);
	MDRV_SOUND_ADD((SoundChipEmulator)sn1);
	MDRV_SOUND_ADD((SoundChipEmulator)sn2);

	MDRV_FRAMES_PER_SECOND(60);
	MDRV_VBLANK_DURATION(DEFAULT_60HZ_VBLANK_DURATION);

	/* video hardware */
	MDRV_VIDEO_ATTRIBUTES(VIDEO_TYPE_RASTER);
	MDRV_SCREEN_SIZE(32*8, 32*8);
	MDRV_VISIBLE_AREA(3*8, 31*8-1, 2*8, 30*8-1);
	MDRV_GFXDECODE(gfxdecodeinfo());
	MDRV_PALETTE_LENGTH(32);
	MDRV_COLORTABLE_LENGTH(32*4+16*8);

	MDRV_PALETTE_INIT(bankp_pi);
	MDRV_VIDEO_START(bankp_vs);
	MDRV_VIDEO_UPDATE(bankp_vu);


	return true;
}



/***************************************************************************

  Game driver(s)

***************************************************************************/

private boolean rom_bankp() {
	ROM_REGION( 0x10000, REGION_CPU1, 0 );	/* 64k for code */
	ROM_LOAD( "epr6175.bin",  0x0000, 0x4000, 0x044552b8 );
	ROM_LOAD( "epr6174.bin",  0x4000, 0x4000, 0xd29b1598 );
	ROM_LOAD( "epr6173.bin",  0x8000, 0x4000, 0xb8405d38 );
	ROM_LOAD( "epr6176.bin",  0xc000, 0x2000, 0xc98ac200 );

	ROM_REGION( 0x04000, REGION_GFX1, ROMREGION_DISPOSE );
	ROM_LOAD( "epr6165.bin",  0x0000, 0x2000, 0xaef34a93 );	/* playfield #1 chars */
	ROM_LOAD( "epr6166.bin",  0x2000, 0x2000, 0xca13cb11 );

	ROM_REGION( 0x0c000, REGION_GFX2, ROMREGION_DISPOSE );
	ROM_LOAD( "epr6172.bin",  0x0000, 0x2000, 0xc4c4878b );	/* playfield #2 chars */
	ROM_LOAD( "epr6171.bin",  0x2000, 0x2000, 0xa18165a1 );
	ROM_LOAD( "epr6170.bin",  0x4000, 0x2000, 0xb58aa8fa );
	ROM_LOAD( "epr6169.bin",  0x6000, 0x2000, 0x1aa37fce );
	ROM_LOAD( "epr6168.bin",  0x8000, 0x2000, 0x05f3a867 );
	ROM_LOAD( "epr6167.bin",  0xa000, 0x2000, 0x3fa337e1 );

	ROM_REGION( 0x0220, REGION_PROMS, 0 );
	ROM_LOAD( "pr6177.clr",   0x0000, 0x020, 0xeb70c5ae ); 	/* palette */
	ROM_LOAD( "pr6178.clr",   0x0020, 0x100, 0x0acca001 );	/* charset #1 lookup table */
	ROM_LOAD( "pr6179.clr",   0x0120, 0x100, 0xe53bafdb ); 	/* charset #2 lookup table */
	return true;
}


	public Machine getMachine(URL url, String name) {
		super.getMachine(url,name);
		super.setVideoEmulator(v);

		if (name.equals("bankp")) {
			GAME(1984, rom_bankp(),  0, mdrv_bankp(), ipt_bankp(), 0, ROT0, "Sega", "Bank Panic" );
		}

		m.init(md);

		v.setMachine(m);

		return (Machine)m;
	}

}



