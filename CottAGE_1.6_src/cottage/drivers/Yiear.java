/*
CottAGE - the Arcade Generic Emulator in Java

Java driver by Erik Duijs
*/

/***************************************************************************

	Yie Ar Kung-Fu memory map (preliminary)
	enrique.sanchez@cs.us.es

CPU:    Motorola 6809

Normal 6809 IRQs must be generated each video frame (60 fps).
The 6809 NMI is used for sound timing.


0000	  	R	VLM5030 status ???
4000	 	 W  control port
					bit 0 - flip screen
					bit 1 - NMI enable
					bit 2 - IRQ enable
					bit 3 - coin counter A
					bit 4 - coin counter B
4800	 	 W	sound latch write
4900	 	 W  copy sound latch to SN76496
4a00	 	 W  VLM5030 control
4b00	 	 W  VLM5030 data
4c00		R   DSW #0
4d00		R   DSW #1
4e00		R   IN #0
4e01		R   IN #1
4e02		R   IN #2
4e03		R   DSW #2
4f00	 	 W  watchdog
5000-502f	 W  sprite RAM 1 (18 sprites)
					byte 0 - bit 0 - sprite code MSB
							 bit 6 - flip X
							 bit 7 - flip Y
					byte 1 - Y position
5030-53ff	RW  RAM
5400-542f    W  sprite RAM 2
					byte 0 - X position
					byte 1 - sprite code LSB
5430-57ff	RW  RAM
5800-5fff	RW  video RAM
					byte 0 - bit 4 - character code MSB
							 bit 6 - flip Y
							 bit 7 - flip X
					byte 1 - character code LSB
8000-ffff	R   ROM


***************************************************************************/

package cottage.drivers;

import java.net.URL;

import jef.machine.Machine;
import jef.map.InterruptHandler;
import jef.map.WriteHandler;
import jef.sound.chip.SN76496;
import jef.video.GfxManager;
import jef.video.Vh_convert_color_proms;
import jef.video.Vh_refresh;
import jef.video.Vh_start;

import cottage.mame.Driver;
import cottage.mame.MAMEConstants;
import cottage.mame.MAMEDriver;

public class Yiear extends MAMEDriver implements Driver, MAMEConstants {

	cottage.vidhrdw.Yiear v	= new cottage.vidhrdw.Yiear();
	WriteHandler videoram_w = v.videoram_w();
	Vh_convert_color_proms yiear_pi = (Vh_convert_color_proms)v;
	Vh_start generic_vs = (Vh_start)v;
	Vh_refresh yiear_vu = (Vh_refresh)v;
	
	SN76496 sn = new SN76496(18432000/12);

	cottage.machine.Yiear m	= new cottage.machine.Yiear();
	InterruptHandler interrupt = m.irq_interrupt();
	InterruptHandler yiear_nmi_interrupt = m.yiear_nmi_interrupt();
	WriteHandler interrupt_enable_w = m.interrupt_enable_w();
	WriteHandler nmi_enable_w = m.nmi_enable_w();
	WriteHandler yiear_control_w = new Yiear_control_w();

	private boolean readmem() {
		MR_START();
		//MR_ADD( 0x0000, 0x0000, yiear_speech_r );
		MR_ADD( 0x4c00, 0x4c00, input_port_3_r );
		MR_ADD( 0x4d00, 0x4d00, input_port_4_r );
		MR_ADD( 0x4e00, 0x4e00, input_port_0_r );
		MR_ADD( 0x4e01, 0x4e01, input_port_1_r );
		MR_ADD( 0x4e02, 0x4e02, input_port_2_r );
		MR_ADD( 0x4e03, 0x4e03, input_port_5_r );
		MR_ADD( 0x5000, 0x5fff, MRA_RAM );
		MR_ADD( 0x8000, 0xffff, MRA_ROM );
		return true;
	}

	private boolean writemem() {
		MW_START( 0x4000, 0x4000, yiear_control_w );
		MW_ADD( 0x4800, 0x4800, konami_SN76496_latch_w() );
		MW_ADD( 0x4900, 0x4900, konami_SN76496_0_w() );
		//MW_ADD( 0x4a00, 0x4a00, yiear_VLM5030_control_w );
		//MW_ADD( 0x4b00, 0x4b00, VLM5030_data_w );
		//MW_ADD( 0x4f00, 0x4f00, watchdog_reset_w );
		MW_ADD( 0x5000, 0x502f, MWA_RAM, spriteram, spriteram_size );
		MW_ADD( 0x5030, 0x53ff, MWA_RAM );
		MW_ADD( 0x5400, 0x542f, MWA_RAM, spriteram_2 );
		MW_ADD( 0x5430, 0x57ff, MWA_RAM );
		MW_ADD( 0x5800, 0x5fff, videoram_w, videoram, videoram_size );
		MW_ADD( 0x8000, 0xffff, MWA_ROM );
		return true;
	}

	private boolean ipt_yiear() {
		PORT_START();	/* IN0 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_COIN1 );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_COIN2 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_COIN3 );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_START1 );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_START2 );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );

		PORT_START();	/* IN1 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT | IPF_8WAY );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_UP | IPF_8WAY );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN | IPF_8WAY );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON2 );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_BUTTON3 );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );

		PORT_START();	/* IN2 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT | IPF_8WAY | IPF_COCKTAIL );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_COCKTAIL );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_UP | IPF_8WAY | IPF_COCKTAIL );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN | IPF_8WAY | IPF_COCKTAIL );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_COCKTAIL );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_COCKTAIL );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_COCKTAIL );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );

		PORT_START();	/* DSW0 */
		PORT_DIPNAME( 0x03, 0x01, DEF_STR2( Lives ) );
		PORT_DIPSETTING(    0x03, "1" );
		PORT_DIPSETTING(    0x02, "2" );
		PORT_DIPSETTING(    0x01, "3" );
		PORT_DIPSETTING(    0x00, "5" );
		PORT_DIPNAME( 0x04, 0x00, DEF_STR2( Cabinet ) );
		PORT_DIPSETTING(    0x00, DEF_STR2( Upright ) );
		PORT_DIPSETTING(    0x04, DEF_STR2( Cocktail ) );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR2( Bonus_Life ) );
		PORT_DIPSETTING(    0x08, "30000 80000" );
		PORT_DIPSETTING(    0x00, "40000 90000" );
		PORT_DIPNAME( 0x30, 0x10, DEF_STR2( Difficulty ) );
		PORT_DIPSETTING(    0x30, "Easy" );
		PORT_DIPSETTING(    0x10, "Normal" );
		PORT_DIPSETTING(    0x20, "Difficult" );
		PORT_DIPSETTING(    0x00, "Very Difficult" );
		PORT_DIPNAME( 0x40, 0x40, DEF_STR2( Unused ) );
		PORT_DIPSETTING(    0x40, DEF_STR2( Off ) );
		PORT_DIPSETTING(    0x00, DEF_STR2( On ) );
		PORT_DIPNAME( 0x80, 0x00, DEF_STR2( Demo_Sounds ) );
		PORT_DIPSETTING(    0x80, DEF_STR2( Off ) );
		PORT_DIPSETTING(    0x00, DEF_STR2( On ) );

		PORT_START();	/* DSW1 */
		PORT_DIPNAME( 0x01, 0x01, DEF_STR2( Flip_Screen ) );
		PORT_DIPSETTING(    0x01, DEF_STR2( Off ) );
		PORT_DIPSETTING(    0x00, DEF_STR2( On ) );
		PORT_DIPNAME( 0x02, 0x02, "Number of Controllers" );
		PORT_DIPSETTING(    0x02, "1" );
		PORT_DIPSETTING(    0x00, "2" );
		PORT_SERVICE( 0x04, IP_ACTIVE_LOW );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR2( Unused ) );
		PORT_DIPSETTING(    0x08, DEF_STR2( Off ) );
		PORT_DIPSETTING(    0x00, DEF_STR2( On ) );
		PORT_BIT( 0xf0, IP_ACTIVE_LOW, IPT_UNUSED );

		PORT_START();	/* DSW2 */
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
	//	PORT_DIPSETTING(    0x00, "Invalid" );
		return true;
	}

	int[][] charlayout =
	{
	{8},{8},
	{RGN_FRAC(1,2)},
	{4},
	{ RGN_FRAC(1,2)+4, RGN_FRAC(1,2)+0, 4, 0 }, //CottAGE
	{ 0, 1, 2, 3, 8*8+0, 8*8+1, 8*8+2, 8*8+3 },
	{ 0*8, 1*8, 2*8, 3*8, 4*8, 5*8, 6*8, 7*8 },
	{16*8}
	};

	int[][] spritelayout =
	{
	{16},{16},
	{RGN_FRAC(1,2)},
	{4},
	{ RGN_FRAC(1,2)+4, RGN_FRAC(1,2)+0, 4, 0 }, //CottAGE
	{ 0*8*8+0, 0*8*8+1, 0*8*8+2, 0*8*8+3, 1*8*8+0, 1*8*8+1, 1*8*8+2, 1*8*8+3,
	  2*8*8+0, 2*8*8+1, 2*8*8+2, 2*8*8+3, 3*8*8+0, 3*8*8+1, 3*8*8+2, 3*8*8+3 },
	{  0*8,  1*8,  2*8,  3*8,  4*8,  5*8,  6*8,  7*8,
	  32*8, 33*8, 34*8, 35*8, 36*8, 37*8, 38*8, 39*8 },
	{64*8}
	};

	private boolean gfxdecodeinfo() {
		GDI_ADD( REGION_GFX1, 0, charlayout,   16, 1 );
		GDI_ADD( REGION_GFX2, 0, spritelayout,  0, 1 );
		GDI_ADD( -1 ); /* end of array */
		return true;
	}

	class Yiear_control_w implements WriteHandler {
		public void write(int address, int data) {
			/* bit 0 flips screen */
			//flip_screen_set(data & 1);

			/* bit 1 is NMI enable */
			nmi_enable_w.write(0, data & 0x02);

			/* bit 2 is IRQ enable */
			interrupt_enable_w.write(0, data & 0x04);

			/* bits 3 and 4 are coin counters */
			//coin_counter_w(0, (data >> 3) & 0x01);
			//coin_counter_w(1, (data >> 4) & 0x01);
		}
	}
	
	static int SN76496_latch = 0;
	public WriteHandler konami_SN76496_latch_w() { return new Konami_SN76496_latch_w(); }
	public class Konami_SN76496_latch_w implements WriteHandler {
		public void write(int address, int data) {
			SN76496_latch = data;
		}
	}	
	public WriteHandler konami_SN76496_0_w() { return new Konami_SN76496_0_w(); }
	public class Konami_SN76496_0_w implements WriteHandler {
		public void write(int address, int data) {
			sn.command_w(SN76496_latch);
		}
	}

	public boolean mdrv_yiear() {

		/* basic machine hardware */
		MDRV_CPU_ADD(M6809,18432000/16);	/* ???? */
		MDRV_CPU_MEMORY(readmem(),writemem());
		MDRV_CPU_VBLANK_INT(interrupt,1);	/* vblank */
		//MDRV_CPU_(yiear_nmi_interrupt,500);	/* music tempo (correct frequency unknown) */
		
		/* sound hardware */
		MDRV_SOUND_ADD(sn);
		
		MDRV_FRAMES_PER_SECOND(60);
		MDRV_VBLANK_DURATION(DEFAULT_60HZ_VBLANK_DURATION);

		/* video hardware */
		MDRV_VIDEO_ATTRIBUTES(VIDEO_TYPE_RASTER|GfxManager.VIDEO_SUPPORTS_DIRTY);
		MDRV_SCREEN_SIZE(32*8, 32*8);
		MDRV_VISIBLE_AREA(0*8, 32*8-1, 2*8, 30*8-1);
		MDRV_GFXDECODE(gfxdecodeinfo());
		MDRV_PALETTE_LENGTH(32);
		MDRV_COLORTABLE_LENGTH(32);

		MDRV_PALETTE_INIT(yiear_pi);
		MDRV_VIDEO_START(generic_vs);
		MDRV_VIDEO_UPDATE(yiear_vu);

		//MDRV_SOUND_ADD(VLM5030, vlm5030_interface);
		return true;
	}

	private boolean rom_yiear() {
		ROM_REGION( 0x10000, REGION_CPU1, 0 );	/* 64k for code */
		ROM_LOAD( "i08.10d",      0x08000, 0x4000, 0xe2d7458b );
		ROM_LOAD( "i07.8d",       0x0c000, 0x4000, 0x7db7442e );

		ROM_REGION( 0x04000, REGION_GFX1, ROMREGION_DISPOSE );
		ROM_LOAD( "g16_1.bin",    0x00000, 0x2000, 0xb68fd91d );
		ROM_LOAD( "g15_2.bin",    0x02000, 0x2000, 0xd9b167c6 );

		ROM_REGION( 0x10000, REGION_GFX2, ROMREGION_DISPOSE );
		ROM_LOAD( "g04_5.bin",    0x00000, 0x4000, 0x45109b29 );
		ROM_LOAD( "g03_6.bin",    0x04000, 0x4000, 0x1d650790 );
		ROM_LOAD( "g06_3.bin",    0x08000, 0x4000, 0xe6aa945b );
		ROM_LOAD( "g05_4.bin",    0x0c000, 0x4000, 0xcc187c22 );

		ROM_REGION( 0x0020, REGION_PROMS, 0 );
		ROM_LOAD( "yiear.clr",    0x00000, 0x0020, 0xc283d71f );

		//ROM_REGION( 0x2000, REGION_SOUND1, 0 )	/* 8k for the VLM5030 data */
		//ROM_LOAD( "a12_9.bin",    0x00000, 0x2000, 0xf75a1539 );
		return true;
	}

	private boolean rom_yiear2() {
		ROM_REGION( 0x10000, REGION_CPU1, 0 );	/* 64k for code */
		ROM_LOAD( "d12_8.bin",    0x08000, 0x4000, 0x49ecd9dd );
		ROM_LOAD( "d14_7.bin",    0x0c000, 0x4000, 0xbc2e1208 );

		ROM_REGION( 0x04000, REGION_GFX1, ROMREGION_DISPOSE );
		ROM_LOAD( "g16_1.bin",    0x00000, 0x2000, 0xb68fd91d );
		ROM_LOAD( "g15_2.bin",    0x02000, 0x2000, 0xd9b167c6 );

		ROM_REGION( 0x10000, REGION_GFX2, ROMREGION_DISPOSE );
		ROM_LOAD( "g04_5.bin",    0x00000, 0x4000, 0x45109b29 );
		ROM_LOAD( "g03_6.bin",    0x04000, 0x4000, 0x1d650790 );
		ROM_LOAD( "g06_3.bin",    0x08000, 0x4000, 0xe6aa945b );
		ROM_LOAD( "g05_4.bin",    0x0c000, 0x4000, 0xcc187c22 );

		ROM_REGION( 0x0020, REGION_PROMS, 0 );
		ROM_LOAD( "yiear.clr",    0x00000, 0x0020, 0xc283d71f );

		//ROM_REGION( 0x2000, REGION_SOUND1, 0 );	/* 8k for the VLM5030 data */
		//ROM_LOAD( "a12_9.bin",    0x00000, 0x2000, 0xf75a1539 );
		return true;
	}

	public Machine getMachine(URL url, String name) {
		super.getMachine(url,name);
		super.setVideoEmulator(v);

		if (name.equals("yiear")) {
			GAME(1985, rom_yiear(),  0,       mdrv_yiear(), ipt_yiear(), 0, ROT0, "Konami", "Yie Ar Kung-Fu (set 1)" );
		} else if (name.equals("yiear2")) {
			GAME(1985, rom_yiear2(), "yiear", mdrv_yiear(), ipt_yiear(), 0, ROT0, "Konami", "Yie Ar Kung-Fu (set 2)" );
		}

		m.init(md);
		return (Machine)m;
	}

}