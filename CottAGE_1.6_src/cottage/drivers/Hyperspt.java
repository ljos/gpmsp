/*
CottAGE - the Arcade Generic Emulator in Java

Java driver by Erik Duijs
*/

package cottage.drivers;

import java.net.URL;

import jef.cpu.Cpu;
import jef.machine.Machine;
import jef.map.InterruptHandler;
import jef.map.WriteHandler;
import jef.sound.SoundChipEmulator;
import jef.sound.chip.SN76496;
import jef.video.Vh_convert_color_proms;
import jef.video.Vh_refresh;
import jef.video.Vh_start;

import cottage.mame.MAMEDriver;

/***************************************************************************

Based on drivers from Juno First emulator by Chris Hardy (chrish@kcbbs.gen.nz)

***************************************************************************/
public class Hyperspt extends MAMEDriver {

SN76496 sn1 = new SN76496(14318180/8);

cottage.vidhrdw.Hyperspt v			= new cottage.vidhrdw.Hyperspt();
WriteHandler 			videoram_w	= v.videoram_w();
WriteHandler 			colorram_w	= v.colorram_w();
WriteHandler			konami_SN76496_latch_w 	= new Konami_SN76496_latch_w();
WriteHandler			konami_SN76496_0_w 		= new Konami_SN76496_0_w();
WriteHandler			konami_sh_irqtrigger_w  = new Konami_sh_irqtrigger_w();
Vh_start				hyperspt_vs = (Vh_start)v;
Vh_start				roadf_vs	= v.roadf_vs();
Vh_refresh 				hyperspt_vu	= (Vh_refresh)v;
Vh_refresh 				roadf_vu	= v.roadf_vu();
Vh_convert_color_proms  hyperspt_pi	= (Vh_convert_color_proms)v;

cottage.machine.Konami m 		= new cottage.machine.Konami();
InterruptHandler irq0_line_hold = m.irq0_line_hold();
WriteHandler interrupt_enable_w = m.interrupt_enable();

private boolean hyperspt_readmem() {
	MR_START();
	MR_ADD( 0x1000, 0x10ff, MRA_RAM );
	MR_ADD( 0x1600, 0x1600, input_port_4_r ); /* DIP 2 */
	MR_ADD( 0x1680, 0x1680, input_port_0_r ); /* IO Coin */
	MR_ADD( 0x1681, 0x1681, input_port_1_r ); /* P1 IO */
//	MR_ADD( 0x1681, 0x1681, konami_IN1_r ); /* P1 IO and handle fake button for cheating */
	MR_ADD( 0x1682, 0x1682, input_port_2_r ); /* P2 IO */
	MR_ADD( 0x1683, 0x1683, input_port_3_r ); /* DIP 1 */
	MR_ADD( 0x2000, 0x3fff, MRA_RAM );
	MR_ADD( 0x4000, 0xffff, MRA_ROM );
	return true;
}

private boolean roadf_readmem() {
	MR_START();
	MR_ADD( 0x1000, 0x10ff, MRA_RAM );
	MR_ADD( 0x1600, 0x1600, input_port_4_r ); /* DIP 2 */
	MR_ADD( 0x1680, 0x1680, input_port_0_r ); /* IO Coin */
	MR_ADD( 0x1681, 0x1681, input_port_1_r ); /* P1 IO */
	MR_ADD( 0x1682, 0x1682, input_port_2_r ); /* P2 IO */
	MR_ADD( 0x1683, 0x1683, input_port_3_r ); /* DIP 1 */
	MR_ADD( 0x2000, 0x3fff, MRA_RAM );
	MR_ADD( 0x4000, 0xffff, MRA_ROM );
	return true;
}

private boolean writemem() {
	MW_START();
	MW_ADD( 0x1000, 0x10bf, MWA_RAM, spriteram, spriteram_size );
	MW_ADD( 0x10C0, 0x10ff, MWA_RAM );	/* , hyperspt_scroll Scroll amount */
	//MW_ADD( 0x1400, 0x1400, watchdog_reset_w );
	//MW_ADD( 0x1480, 0x1480, hyperspt_flipscreen_w );
	MW_ADD( 0x1481, 0x1481, konami_sh_irqtrigger_w );  /* cause interrupt on audio CPU */
	//MW_ADD( 0x1483, 0x1484, hyperspt_coin_counter_w );
	MW_ADD( 0x1487, 0x1487, interrupt_enable_w );  /* Interrupt enable */
	MW_ADD( 0x1500, 0x1500, soundlatch_w );
	MW_ADD( 0x2000, 0x27ff, videoram_w, videoram, videoram_size );
	MW_ADD( 0x2800, 0x2fff, colorram_w, colorram );
	MW_ADD( 0x3000, 0x37ff, MWA_RAM );
	MW_ADD( 0x3800, 0x3fff, MWA_RAM ); //, nvram, nvram_size
	MW_ADD( 0x4000, 0xffff, MWA_ROM );
	return true;
}

private boolean sound_writemem() {
	MW_START();
	MW_ADD( 0x0000, 0x3fff, MWA_ROM );
	MW_ADD( 0x4000, 0x4fff, MWA_RAM );
	//MW_ADD( 0xa000, 0xa000, VLM5030_data_w ); /* speech data */
	//MW_ADD( 0xc000, 0xdfff, hyperspt_sound_w );     /* speech and output controll */
	//MW_ADD( 0xe000, 0xe000, DAC_data_w );
	MW_ADD( 0xe001, 0xe001, konami_SN76496_latch_w );  /* Loads the snd command into the snd latch */
	MW_ADD( 0xe002, 0xe002, konami_SN76496_0_w );      /* This address triggers the SN chip to read the data port. */
	return true;
}

private boolean sound_readmem() {
	MR_START();
	MR_ADD( 0x0000, 0x3fff, MRA_ROM );
	MR_ADD( 0x4000, 0x4fff, MRA_RAM );
	MR_ADD( 0x6000, 0x6000, soundlatch_r );
	//MR_ADD( 0x8000, 0x8000, hyperspt_sh_timer_r );
	return true;
}

int SN76496_latch = 0;

class Konami_SN76496_latch_w implements WriteHandler {
	public void write(int address, int data) {
		System.out.println("Konami_SN76496_latch_w : " + data);
		SN76496_latch = data;
	}
}
class Konami_SN76496_0_w implements WriteHandler {
	WriteHandler sn = sn1.sn76496_command_w();
	public void write(int address, int data) {
		System.out.println("Konami_SN76496_0_w : " + SN76496_latch);
		sn1.command_w(SN76496_latch);
	}
}

class Konami_sh_irqtrigger_w implements WriteHandler {
	int last;

	public void write(int address, int data) {
		//System.out.println("Konami_sh_irqtrigger_w : " + data);
		if (last == 0 && data != 0)
		{
			/* setting bit 0 low then high triggers IRQ on the sound CPU */
			//cpu_cause_interrupt(1,0xff);
			cpu_set_irq_line(1, Cpu.INTERRUPT_TYPE_IRQ, 1);
		}

		last = data;
	}
	
}

private boolean ipt_hyperspt() {
	PORT_START();		/* IN0 */
	PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_COIN1 );
	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_COIN2 );
	PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_COIN3 );
	PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_START1 );
	PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_START2 );
	PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_COIN4 );
	PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
	PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );

	PORT_START();		/* IN1 */
	PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER1 );
	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER1 );
	PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER1 );
	PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_START3 );
	PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER2 );
	PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER2 );
	PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER2 );
	PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
	/* Fake button to press buttons 1 and 3 impossibly fast. Handle via konami_IN1_r */
	//PORT_BITX(0x80, IP_ACTIVE_LOW, IPT_BUTTON4 | IPF_CHEAT | IPF_PLAYER1, "Run Like Hell Cheat", IP_KEY_DEFAULT, IP_JOY_DEFAULT )

	PORT_START();		/* IN2 */
	PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER3 /*| IPF_COCKTAIL*/ );
	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER3 /*| IPF_COCKTAIL*/ );
	PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER3 /*| IPF_COCKTAIL*/ );
	PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_START4 );
	PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER4 /*| IPF_COCKTAIL*/ );
	PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER4 /*| IPF_COCKTAIL*/ );
	PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER4 /*| IPF_COCKTAIL*/ );
	PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );

	PORT_START();		/* DSW0 */
	PORT_DIPNAME( 0x0f, 0x0f, DEF_STR2( Coin_A ) );
	PORT_DIPSETTING(	0x02, DEF_STR2( _4C_1C ) );
	PORT_DIPSETTING(	0x05, DEF_STR2( _3C_1C ) );
	PORT_DIPSETTING(	0x08, DEF_STR2( _2C_1C ) );
	PORT_DIPSETTING(	0x04, DEF_STR2( _3C_2C ) );
	PORT_DIPSETTING(	0x01, DEF_STR2( _4C_3C ) );
	PORT_DIPSETTING(	0x0f, DEF_STR2( _1C_1C ) );
	PORT_DIPSETTING(	0x03, DEF_STR2( _3C_4C ) );
	PORT_DIPSETTING(	0x07, DEF_STR2( _2C_3C ) );
	PORT_DIPSETTING(	0x0e, DEF_STR2( _1C_2C ) );
	PORT_DIPSETTING(	0x06, DEF_STR2( _2C_5C ) );
	PORT_DIPSETTING(	0x0d, DEF_STR2( _1C_3C ) );
	PORT_DIPSETTING(	0x0c, DEF_STR2( _1C_4C ) );
	PORT_DIPSETTING(	0x0b, DEF_STR2( _1C_5C ) );
	PORT_DIPSETTING(	0x0a, DEF_STR2( _1C_6C ) );
	PORT_DIPSETTING(	0x09, DEF_STR2( _1C_7C ) );
	PORT_DIPSETTING(	0x00, DEF_STR2( Free_Play ) );
	PORT_DIPNAME( 0xf0, 0xf0, DEF_STR2( Coin_B ) );
	PORT_DIPSETTING(	0x20, DEF_STR2( _4C_1C ) );
	PORT_DIPSETTING(	0x50, DEF_STR2( _3C_1C ) );
	PORT_DIPSETTING(	0x80, DEF_STR2( _2C_1C ) );
	PORT_DIPSETTING(	0x40, DEF_STR2( _3C_2C ) );
	PORT_DIPSETTING(	0x10, DEF_STR2( _4C_3C ) );
	PORT_DIPSETTING(	0xf0, DEF_STR2( _1C_1C ) );
	PORT_DIPSETTING(	0x30, DEF_STR2( _3C_4C ) );
	PORT_DIPSETTING(	0x70, DEF_STR2( _2C_3C ) );
	PORT_DIPSETTING(	0xe0, DEF_STR2( _1C_2C ) );
	PORT_DIPSETTING(	0x60, DEF_STR2( _2C_5C ) );
	PORT_DIPSETTING(	0xd0, DEF_STR2( _1C_3C ) );
	PORT_DIPSETTING(	0xc0, DEF_STR2( _1C_4C ) );
	PORT_DIPSETTING(	0xb0, DEF_STR2( _1C_5C ) );
	PORT_DIPSETTING(	0xa0, DEF_STR2( _1C_6C ) );
	PORT_DIPSETTING(	0x90, DEF_STR2( _1C_7C ) );
	PORT_DIPSETTING(	0x00, "Disabled" );
/* 0x00 disables Coin 2. It still accepts coins and makes the sound, but
   it doesn't give you any credit */

	PORT_START();		/* DSW1 */
	PORT_DIPNAME( 0x01, 0x00, "After Last Event" );
	PORT_DIPSETTING(	0x01, "Game Over" );
	PORT_DIPSETTING(	0x00, "Game Continues" );
	PORT_DIPNAME( 0x02, 0x00, DEF_STR2( Cabinet ) );
	PORT_DIPSETTING(	0x00, DEF_STR2( Upright ) );
	PORT_DIPSETTING(	0x02, DEF_STR2( Cocktail ) );
	PORT_DIPNAME( 0x04, 0x00, DEF_STR2( Demo_Sounds ) );
	PORT_DIPSETTING(	0x04, DEF_STR2( Off ) );
	PORT_DIPSETTING(	0x00, DEF_STR2( On ) );
	PORT_DIPNAME( 0x08, 0x08, "World Records" );
	PORT_DIPSETTING(	0x08, "Don't Erase" );
	PORT_DIPSETTING(	0x00, "Erase on Reset" );
	PORT_DIPNAME( 0xf0, 0xf0, DEF_STR2( Difficulty ) );
	PORT_DIPSETTING(	0xf0, "Easy 1" );
	PORT_DIPSETTING(	0xe0, "Easy 2" );
	PORT_DIPSETTING(	0xd0, "Easy 3" );
	PORT_DIPSETTING(	0xc0, "Easy 4" );
	PORT_DIPSETTING(	0xb0, "Normal 1" );
	PORT_DIPSETTING(	0xa0, "Normal 2" );
	PORT_DIPSETTING(	0x90, "Normal 3" );
	PORT_DIPSETTING(	0x80, "Normal 4" );
	PORT_DIPSETTING(	0x70, "Normal 5" );
	PORT_DIPSETTING(	0x60, "Normal 6" );
	PORT_DIPSETTING(	0x50, "Normal 7" );
	PORT_DIPSETTING(	0x40, "Normal 8" );
	PORT_DIPSETTING(	0x30, "Difficult 1" );
	PORT_DIPSETTING(	0x20, "Difficult 2" );
	PORT_DIPSETTING(	0x10, "Difficult 3" );
	PORT_DIPSETTING(	0x00, "Difficult 4" );
	return true;
}

private boolean ipt_roadf() {
	PORT_START();		/* IN0 */
	PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_COIN1 );
	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_COIN2 );
	PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_COIN3 );
	PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_START1 );
	PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_START2 );
	PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_COIN4 );
	PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
	PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );

	PORT_START();		/* IN1 */
	PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT | IPF_8WAY );
	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY );
	PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_UP | IPF_8WAY );
	PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN | IPF_8WAY );
	PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 );
	PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON2 );
	PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
	PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );

	PORT_START();		/* IN2 */
	PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT | IPF_8WAY | IPF_COCKTAIL );
	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_COCKTAIL );
	PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_UP | IPF_8WAY | IPF_COCKTAIL );
	PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN | IPF_8WAY | IPF_COCKTAIL );
	PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_COCKTAIL );
	PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_COCKTAIL );
	PORT_BIT( 0x40, IP_ACTIVE_HIGH, IPT_UNKNOWN );	/* the game doesn't boot if this is 1 */
	PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );

	PORT_START();		/* DSW0 */
	PORT_DIPNAME( 0x0f, 0x0f, DEF_STR2( Coin_A ) );
	PORT_DIPSETTING(	0x02, DEF_STR2( _4C_1C ) );
	PORT_DIPSETTING(	0x05, DEF_STR2( _3C_1C ) );
	PORT_DIPSETTING(	0x08, DEF_STR2( _2C_1C ) );
	PORT_DIPSETTING(	0x04, DEF_STR2( _3C_2C ) );
	PORT_DIPSETTING(	0x01, DEF_STR2( _4C_3C ) );
	PORT_DIPSETTING(	0x0f, DEF_STR2( _1C_1C ) );
	PORT_DIPSETTING(	0x03, DEF_STR2( _3C_4C ) );
	PORT_DIPSETTING(	0x07, DEF_STR2( _2C_3C ) );
	PORT_DIPSETTING(	0x0e, DEF_STR2( _1C_2C ) );
	PORT_DIPSETTING(	0x06, DEF_STR2( _2C_5C ) );
	PORT_DIPSETTING(	0x0d, DEF_STR2( _1C_3C ) );
	PORT_DIPSETTING(	0x0c, DEF_STR2( _1C_4C ) );
	PORT_DIPSETTING(	0x0b, DEF_STR2( _1C_5C ) );
	PORT_DIPSETTING(	0x0a, DEF_STR2( _1C_6C ) );
	PORT_DIPSETTING(	0x09, DEF_STR2( _1C_7C ) );
	PORT_DIPSETTING(	0x00, DEF_STR2( Free_Play ) );
	PORT_DIPNAME( 0xf0, 0xf0, DEF_STR2( Coin_B ) );
	PORT_DIPSETTING(	0x20, DEF_STR2( _4C_1C ) );
	PORT_DIPSETTING(	0x50, DEF_STR2( _3C_1C ) );
	PORT_DIPSETTING(	0x80, DEF_STR2( _2C_1C ) );
	PORT_DIPSETTING(	0x40, DEF_STR2( _3C_2C ) );
	PORT_DIPSETTING(	0x10, DEF_STR2( _4C_3C ) );
	PORT_DIPSETTING(	0xf0, DEF_STR2( _1C_1C ) );
	PORT_DIPSETTING(	0x30, DEF_STR2( _3C_4C ) );
	PORT_DIPSETTING(	0x70, DEF_STR2( _2C_3C ) );
	PORT_DIPSETTING(	0xe0, DEF_STR2( _1C_2C ) );
	PORT_DIPSETTING(	0x60, DEF_STR2( _2C_5C ) );
	PORT_DIPSETTING(	0xd0, DEF_STR2( _1C_3C ) );
	PORT_DIPSETTING(	0xc0, DEF_STR2( _1C_4C ) );
	PORT_DIPSETTING(	0xb0, DEF_STR2( _1C_5C ) );
	PORT_DIPSETTING(	0xa0, DEF_STR2( _1C_6C ) );
	PORT_DIPSETTING(	0x90, DEF_STR2( _1C_7C ) );
	PORT_DIPSETTING(	0x00, "Disabled" );
/* 0x00 disables Coin 2. It still accepts coins and makes the sound, but
   it doesn't give you any credit */

	PORT_START();		/* DSW1 */
	PORT_DIPNAME( 0x01, 0x00, "Allow Continue" );
	PORT_DIPSETTING(	0x01, DEF_STR2( No ) );
	PORT_DIPSETTING(	0x00, DEF_STR2( Yes ) );
	PORT_DIPNAME( 0x06, 0x06, "Number of Opponents" );
	PORT_DIPSETTING(	0x06, "Easy" );
	PORT_DIPSETTING(	0x04, "Medium" );
	PORT_DIPSETTING(	0x02, "Hard" );
	PORT_DIPSETTING(	0x00, "Hardest" );
	PORT_DIPNAME( 0x08, 0x08, "Speed of Opponents" );
	PORT_DIPSETTING(	0x08, "Easy" );
	PORT_DIPSETTING(	0x00, "Difficult" );
	PORT_DIPNAME( 0x30, 0x30, "Fuel Consumption" );
	PORT_DIPSETTING(	0x30, "Easy" );
	PORT_DIPSETTING(	0x20, "Medium" );
	PORT_DIPSETTING(	0x10, "Hard" );
	PORT_DIPSETTING(	0x00, "Hardest" );
	PORT_DIPNAME( 0x40, 0x00, DEF_STR2( Cabinet ) );
	PORT_DIPSETTING(	0x00, DEF_STR2( Upright ) );
	PORT_DIPSETTING(	0x40, DEF_STR2( Cocktail ) );
	PORT_DIPNAME( 0x80, 0x00, DEF_STR2( Demo_Sounds ) );
	PORT_DIPSETTING(	0x80, DEF_STR2( Off ) );
	PORT_DIPSETTING(	0x00, DEF_STR2( On ) );
	return true;
}

int[][] hyperspt_charlayout =
{
	{8},{8},	/* 8*8 sprites */
	{1024},	/* 1024 characters */
	{4},	/* 4 bits per pixel */
	{ 4, 0, 0x4000*8+4, 0x4000*8+0},
	{ 0, 1, 2, 3, 8*8+0, 8*8+1, 8*8+2, 8*8+3 },
	{ 0*8, 1*8, 2*8, 3*8, 4*8, 5*8, 6*8, 7*8 },
	{16*8}	/* every sprite takes 64 consecutive bytes */
};
int[][] hyperspt_spritelayout =
{
	{16},{16},	/* 16*16 sprites */
	{512},	/* 512 sprites */
	{4},	/* 4 bits per pixel */
	{ 4, 0, 0x8000*8+4, 0x8000*8+0 },
	{ 0, 1, 2, 3, 8*8+0, 8*8+1, 8*8+2, 8*8+3,
			16*8+0, 16*8+1, 16*8+2, 16*8+3, 24*8+0, 24*8+1, 24*8+2, 24*8+3 },
	{ 0*8, 1*8, 2*8, 3*8, 4*8, 5*8, 6*8, 7*8 ,
		32*8, 33*8, 34*8, 35*8, 36*8, 37*8, 38*8, 39*8, },
	{64*8}	/* every sprite takes 64 consecutive bytes */
};

private boolean hyperspt_gfxdecodeinfo()
{
	GDI_ADD( REGION_GFX1, 0, hyperspt_charlayout, 	  0, 16 );
	GDI_ADD( REGION_GFX2, 0, hyperspt_spritelayout, 16*16, 16 );
	GDI_ADD( -1 );
	return true;
};

int[][] roadf_charlayout =
{
	{8},{8},	/* 8*8 sprites */
	{1536},	/* 1536 characters */
	{4},	/* 4 bits per pixel */
	{ 4, 0, 0x6000*8+4, 0x6000*8+0},
	{ 0, 1, 2, 3, 8*8+0, 8*8+1, 8*8+2, 8*8+3 },
	{ 0*8, 1*8, 2*8, 3*8, 4*8, 5*8, 6*8, 7*8 },
	{16*8}	/* every sprite takes 64 consecutive bytes */
};
int[][] roadf_spritelayout =
{
	{16},{16},	/* 16*16 sprites */
	{256},	/* 256 sprites */
	{4},	/* 4 bits per pixel */
	{ 4, 0, 0x4000*8+4, 0x4000*8+0 },
	{ 0, 1, 2, 3, 8*8+0, 8*8+1, 8*8+2, 8*8+3,
			16*8+0, 16*8+1, 16*8+2, 16*8+3, 24*8+0, 24*8+1, 24*8+2, 24*8+3 },
	{ 0*8, 1*8, 2*8, 3*8, 4*8, 5*8, 6*8, 7*8 ,
		32*8, 33*8, 34*8, 35*8, 36*8, 37*8, 38*8, 39*8, },
	{64*8}	/* every sprite takes 64 consecutive bytes */
};

private boolean roadf_gfxdecodeinfo()
{
	GDI_ADD( REGION_GFX1, 0, roadf_charlayout, 	  0, 16 );
	GDI_ADD( REGION_GFX2, 0, roadf_spritelayout, 16*16, 16 );
	GDI_ADD( -1 );
	return true;
};

public boolean mdrv_hyperspt() {
	/* basic machine hardware */
	MDRV_CPU_ADD(M6809, 2048000);		/* 1.400 MHz ??? */
	MDRV_CPU_MEMORY(hyperspt_readmem(),writemem());
	MDRV_CPU_VBLANK_INT(irq0_line_hold,1);

	//MDRV_CPU_ADD(Z80,14318180/4);
	//MDRV_CPU_FLAGS(CPU_AUDIO_CPU); /* Z80 Clock is derived from a 14.31818 MHz crystal */
	//MDRV_CPU_MEMORY(sound_readmem,sound_writemem);

	MDRV_FRAMES_PER_SECOND(60);
	MDRV_VBLANK_DURATION(DEFAULT_60HZ_VBLANK_DURATION);

	//MDRV_NVRAM_HANDLER(hyperspt);

	/* video hardware */
	MDRV_VIDEO_ATTRIBUTES(VIDEO_TYPE_RASTER);
	MDRV_SCREEN_SIZE(32*8, 32*8);
	MDRV_VISIBLE_AREA(0*8, 32*8-1, 2*8, 30*8-1);
	MDRV_GFXDECODE(hyperspt_gfxdecodeinfo());
	MDRV_PALETTE_LENGTH(32);
	MDRV_COLORTABLE_LENGTH(16*16+16*16);

	MDRV_PALETTE_INIT(hyperspt_pi);
	MDRV_VIDEO_START(hyperspt_vs);
	MDRV_VIDEO_UPDATE(hyperspt_vu);

	/* sound hardware */
	//MDRV_SOUND_ADD(DAC, konami_dac_interface);
	//MDRV_SOUND_ADD(SN76496, konami_sn76496_interface);
	//MDRV_SOUND_ADD(VLM5030, hyperspt_vlm5030_interface);
	return true;
}

public boolean mdrv_roadf() {
	/* basic machine hardware */
	MDRV_CPU_ADD(M6809, 2048000);		/* 1.400 MHz ??? */
	MDRV_CPU_MEMORY(roadf_readmem(),writemem());
	MDRV_CPU_VBLANK_INT(irq0_line_hold,1);

	MDRV_CPU_ADD(Z80,14318180/4);
	MDRV_CPU_FLAGS(CPU_AUDIO_CPU); /* Z80 Clock is derived from a 14.31818 MHz crystal */
	MDRV_CPU_MEMORY(sound_readmem(),sound_writemem());
	
	/* sound hardware */
	//MDRV_SOUND_ADD(DAC, konami_dac_interface);
	MDRV_SOUND_ADD((SoundChipEmulator)sn1);
	//MDRV_SOUND_ADD(VLM5030, konami_vlm5030_interface);
	
	MDRV_INTERLEAVE(100);	/* 100 CPU slices per frame - a high value to ensure proper */
	
	MDRV_FRAMES_PER_SECOND(60);
	MDRV_VBLANK_DURATION(DEFAULT_60HZ_VBLANK_DURATION);

	/* video hardware */
	MDRV_VIDEO_ATTRIBUTES(VIDEO_TYPE_RASTER);
	MDRV_SCREEN_SIZE(32*8, 32*8);
	MDRV_VISIBLE_AREA(0*8, 32*8-1, 2*8, 30*8-1);
	MDRV_GFXDECODE(roadf_gfxdecodeinfo());
	MDRV_PALETTE_LENGTH(32);
	MDRV_COLORTABLE_LENGTH(16*16+16*16);

	MDRV_PALETTE_INIT(hyperspt_pi);
	MDRV_VIDEO_START(roadf_vs);
	MDRV_VIDEO_UPDATE(roadf_vu);


	return true;
}

private boolean rom_hyperspt() {
	ROM_REGION( 2*0x10000, REGION_CPU1, 0 );	 /* 64k for code + 64k for decrypted opcodes */
	ROM_LOAD( "c01",          0x4000, 0x2000, 0x0c720eeb );
	ROM_LOAD( "c02",          0x6000, 0x2000, 0x560258e0 );
	ROM_LOAD( "c03",          0x8000, 0x2000, 0x9b01c7e6 );
	ROM_LOAD( "c04",          0xa000, 0x2000, 0x10d7e9a2 );
	ROM_LOAD( "c05",          0xc000, 0x2000, 0xb105a8cd );
	ROM_LOAD( "c06",          0xe000, 0x2000, 0x1a34a849 );

	//ROM_REGION( 0x10000, REGION_CPU2, 0 );	/* 64k for the audio CPU */
	//ROM_LOAD( "c10",          0x0000, 0x2000, 0x3dc1a6ff );
	//ROM_LOAD( "c09",          0x2000, 0x2000, 0x9b525c3e );

	ROM_REGION( 0x08000, REGION_GFX1, ROMREGION_DISPOSE );
	ROM_LOAD( "c26",          0x00000, 0x2000, 0xa6897eac );
	ROM_LOAD( "c24",          0x02000, 0x2000, 0x5fb230c0 );
	ROM_LOAD( "c22",          0x04000, 0x2000, 0xed9271a0 );
	ROM_LOAD( "c20",          0x06000, 0x2000, 0x183f4324 );

	ROM_REGION( 0x10000, REGION_GFX2, ROMREGION_DISPOSE );
	ROM_LOAD( "c14",          0x00000, 0x2000, 0xc72d63be );
	ROM_LOAD( "c13",          0x02000, 0x2000, 0x76565608 );
	ROM_LOAD( "c12",          0x04000, 0x2000, 0x74d2cc69 );
	ROM_LOAD( "c11",          0x06000, 0x2000, 0x66cbcb4d );
	ROM_LOAD( "c18",          0x08000, 0x2000, 0xed25e669 );
	ROM_LOAD( "c17",          0x0a000, 0x2000, 0xb145b39f );
	ROM_LOAD( "c16",          0x0c000, 0x2000, 0xd7ff9f2b );
	ROM_LOAD( "c15",          0x0e000, 0x2000, 0xf3d454e6 );

	ROM_REGION( 0x0220, REGION_PROMS, 0 );
	ROM_LOAD( "c03_c27.bin",  0x0000, 0x0020, 0xbc8a5956 );
	ROM_LOAD( "j12_c28.bin",  0x0020, 0x0100, 0x2c891d59 );
	ROM_LOAD( "a09_c29.bin",  0x0120, 0x0100, 0x811a3f3f );

	//ROM_REGION( 0x10000, REGION_SOUND1, 0 );	/*	64k for speech rom	  */
	//ROM_LOAD( "c08",          0x0000, 0x2000, 0xe8f8ea78 );
	return true;
}

private boolean rom_hpolym84() {
	ROM_REGION( 2*0x10000, REGION_CPU1, 0 );	 /* 64k for code + 64k for decrypted opcodes */
	ROM_LOAD( "c01",          0x4000, 0x2000, 0x0c720eeb );
	ROM_LOAD( "c02",          0x6000, 0x2000, 0x560258e0 );
	ROM_LOAD( "c03",          0x8000, 0x2000, 0x9b01c7e6 );
	ROM_LOAD( "330e04.bin",   0xa000, 0x2000, 0x9c5e2934 );
	ROM_LOAD( "c05",          0xc000, 0x2000, 0xb105a8cd );
	ROM_LOAD( "c06",          0xe000, 0x2000, 0x1a34a849 );

	//ROM_REGION( 0x10000, REGION_CPU2, 0 );	/* 64k for the audio CPU */
	//ROM_LOAD( "c10",          0x0000, 0x2000, 0x3dc1a6ff );
	//ROM_LOAD( "c09",          0x2000, 0x2000, 0x9b525c3e );

	ROM_REGION( 0x08000, REGION_GFX1, ROMREGION_DISPOSE );
	ROM_LOAD( "c26",          0x00000, 0x2000, 0xa6897eac );
	ROM_LOAD( "330e24.bin",   0x02000, 0x2000, 0xf9bbfe1d );
	ROM_LOAD( "c22",          0x04000, 0x2000, 0xed9271a0 );
	ROM_LOAD( "330e20.bin",   0x06000, 0x2000, 0x29969b92 );

	ROM_REGION( 0x10000, REGION_GFX2, ROMREGION_DISPOSE );
	ROM_LOAD( "c14",          0x00000, 0x2000, 0xc72d63be );
	ROM_LOAD( "c13",          0x02000, 0x2000, 0x76565608 );
	ROM_LOAD( "c12",          0x04000, 0x2000, 0x74d2cc69 );
	ROM_LOAD( "c11",          0x06000, 0x2000, 0x66cbcb4d );
	ROM_LOAD( "c18",          0x08000, 0x2000, 0xed25e669 );
	ROM_LOAD( "c17",          0x0a000, 0x2000, 0xb145b39f );
	ROM_LOAD( "c16",          0x0c000, 0x2000, 0xd7ff9f2b );
	ROM_LOAD( "c15",          0x0e000, 0x2000, 0xf3d454e6 );

	ROM_REGION( 0x0220, REGION_PROMS, 0 );
	ROM_LOAD( "c03_c27.bin",  0x0000, 0x0020, 0xbc8a5956 );
	ROM_LOAD( "j12_c28.bin",  0x0020, 0x0100, 0x2c891d59 );
	ROM_LOAD( "a09_c29.bin",  0x0120, 0x0100, 0x811a3f3f );

	//ROM_REGION( 0x10000, REGION_SOUND1, 0 );	/*	64k for speech rom	  */
	//ROM_LOAD( "c08",          0x0000, 0x2000, 0xe8f8ea78 );
	return true;
}

private boolean rom_roadf() {
	ROM_REGION( 2*0x10000, REGION_CPU1, 0 );	 /* 64k for code + 64k for decrypted opcodes */
	ROM_LOAD( "g05_g01.bin",  0x4000, 0x2000, 0xe2492a06 );
	ROM_LOAD( "g07_f02.bin",  0x6000, 0x2000, 0x0bf75165 );
	ROM_LOAD( "g09_g03.bin",  0x8000, 0x2000, 0xdde401f8 );
	ROM_LOAD( "g11_f04.bin",  0xA000, 0x2000, 0xb1283c77 );
	ROM_LOAD( "g13_f05.bin",  0xC000, 0x2000, 0x0ad4d796 );
	ROM_LOAD( "g15_f06.bin",  0xE000, 0x2000, 0xfa42e0ed );

	ROM_REGION( 0x10000, REGION_CPU2, 0 );	/* 64k for the audio CPU */
	ROM_LOAD( "a17_d10.bin",  0x0000, 0x2000, 0xc33c927e );

	ROM_REGION( 0x0c000, REGION_GFX1, ROMREGION_DISPOSE );
	ROM_LOAD( "a14_e26.bin",  0x00000, 0x4000, 0xf5c738e2 );
	ROM_LOAD( "a12_d24.bin",  0x04000, 0x2000, 0x2d82c930 );
	ROM_LOAD( "c14_e22.bin",  0x06000, 0x4000, 0xfbcfbeb9 );
	ROM_LOAD( "c12_d20.bin",  0x0a000, 0x2000, 0x5e0cf994 );

	ROM_REGION( 0x08000, REGION_GFX2, ROMREGION_DISPOSE );
	ROM_LOAD( "j19_e14.bin",  0x00000, 0x4000, 0x16d2bcff );
	ROM_LOAD( "g19_e18.bin",  0x04000, 0x4000, 0x490685ff );

	ROM_REGION( 0x0220, REGION_PROMS, 0 );
	ROM_LOAD( "c03_c27.bin",  0x0000, 0x0020, 0x45d5e352 );
	ROM_LOAD( "j12_c28.bin",  0x0020, 0x0100, 0x2955e01f );
	ROM_LOAD( "a09_c29.bin",  0x0120, 0x0100, 0x5b3b5f2a );
	return true;
}

private boolean rom_roadf2() {
	ROM_REGION( 2*0x10000, REGION_CPU1, 0 );	 /* 64k for code + 64k for decrypted opcodes */
	ROM_LOAD( "5g",           0x4000, 0x2000, 0xd8070d30 );
	ROM_LOAD( "6g",           0x6000, 0x2000, 0x8b661672 );
	ROM_LOAD( "8g",           0x8000, 0x2000, 0x714929e8 );
	ROM_LOAD( "11g",          0xA000, 0x2000, 0x0f2c6b94 );
	ROM_LOAD( "g13_f05.bin",  0xC000, 0x2000, 0x0ad4d796 );
	ROM_LOAD( "g15_f06.bin",  0xE000, 0x2000, 0xfa42e0ed );

	ROM_REGION( 0x10000, REGION_CPU2, 0 );	/* 64k for the audio CPU */
	ROM_LOAD( "a17_d10.bin",  0x0000, 0x2000, 0xc33c927e );

	ROM_REGION( 0x0c000, REGION_GFX1, ROMREGION_DISPOSE );
	ROM_LOAD( "a14_e26.bin",  0x00000, 0x4000, 0xf5c738e2 );
	ROM_LOAD( "a12_d24.bin",  0x04000, 0x2000, 0x2d82c930 );
	ROM_LOAD( "c14_e22.bin",  0x06000, 0x4000, 0xfbcfbeb9 );
	ROM_LOAD( "c12_d20.bin",  0x0a000, 0x2000, 0x5e0cf994 );

	ROM_REGION( 0x08000, REGION_GFX2, ROMREGION_DISPOSE );
	ROM_LOAD( "j19_e14.bin",  0x00000, 0x4000, 0x16d2bcff );
	ROM_LOAD( "g19_e18.bin",  0x04000, 0x4000, 0x490685ff );

	ROM_REGION( 0x0220, REGION_PROMS, 0 );
	ROM_LOAD( "c03_c27.bin",  0x0000, 0x0020, 0x45d5e352 );
	ROM_LOAD( "j12_c28.bin",  0x0020, 0x0100, 0x2955e01f );
	ROM_LOAD( "a09_c29.bin",  0x0120, 0x0100, 0x5b3b5f2a );
	return true;
}

private boolean init_hyperspt() {
	m.konami1_decode(memory_region(0));
	return true;
}


	public Machine getMachine(URL url, String name) {
		super.getMachine(url,name);
		super.setVideoEmulator(v);

		if (name.equals("hyperspt")) {
			GAME( 1984, rom_hyperspt(), 0,        mdrv_hyperspt(), ipt_hyperspt(), 0/*init_hyperspt()*/, ROT0, "Konami (Centuri license)", "Hyper Sports" );
		} else if (name.equals("hpolym84")) {
			GAME( 1984, rom_hpolym84(),"hyperspt",mdrv_hyperspt(), ipt_hyperspt(), 0/*init_hyperspt()*/, ROT0, "Konami", "Hyper Olympics '84" );
		} else if (name.equals("roadf")) {
			GAME( 1984, rom_roadf(),	0,		  mdrv_roadf(),    ipt_roadf(),    0/*init_hyperspt()*/, ROT90, "Konami", "Road Fighter (set 1)" );
		} else if (name.equals("roadf2")) {
			GAME( 1984, rom_roadf2(),"roadf",	  mdrv_roadf(),    ipt_roadf(),    0/*init_hyperspt()*/, ROT90, "Konami", "Road Fighter (set 2)" );
		}

		m.init(md);

		// In the GAME(..) simulated macro, the init_hyperspt is executed before the roms
		// are actually loaded so the ROM patch doesn't have effect.
		// This hacks the ROM patch in there.
		init_hyperspt();

		return (Machine)m;
	}

}
