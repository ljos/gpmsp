/*
 * CottAGE - the Arcade Generic Emulator in Java
 * 
 * Java driver by Gollum
 */

/*******************************************************************************
 * 
 * Mario Bros memory map (preliminary):
 * 
 * driver by Mirko Buffoni
 * 
 * 
 * 0000-5fff ROM 6000-6fff RAM 7000-73ff ? 7400-77ff Video RAM f000-ffff ROM
 * 
 * read: 7c00 IN0 7c80 IN1 7f80 DSW
 * 
 * 
 * IN0 (bits NOT inverted) bit 7 : TEST bit 6 : START 2 bit 5 : START 1 bit 4 :
 * JUMP player 1 bit 3 : ? DOWN player 1 ? bit 2 : ? UP player 1 ? bit 1 : LEFT
 * player 1 bit 0 : RIGHT player 1
 * 
 * 
 * IN1 (bits NOT inverted) bit 7 : ? bit 6 : COIN 2 bit 5 : COIN 1 bit 4 : JUMP
 * player 2 bit 3 : ? DOWN player 2 ? bit 2 : ? UP player 2 ? bit 1 : LEFT
 * player 2 bit 0 : RIGHT player 2
 * 
 * 
 * DSW (bits NOT inverted) bit 7 : \ difficulty bit 6 : / 00 = easy 01 = medium
 * 10 = hard 11 = hardest bit 5 : \ bonus bit 4 : / 00 = 20000 01 = 30000 10 =
 * 40000 11 = none bit 3 : \ coins per play bit 2 : / bit 1 : \ 00 = 3 lives 01 =
 * 4 lives bit 0 : / 10 = 5 lives 11 = 6 lives
 * 
 * 
 * write: 7d00 vertical scroll (pow) 7d80 ? 7e00 sound 7e80-7e82 ? 7e83 sprite
 * palette bank select 7e84 interrupt enable 7e85 ? 7f00-7f07 sound triggers
 * 
 * 
 * I/O ports
 * 
 * write: 00 ?
 *  
 ******************************************************************************/

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

public class Mario extends MAMEDriver implements Driver, MAMEConstants {

	cottage.vidhrdw.Mario v = new cottage.vidhrdw.Mario();

	int[] mario_scrolly = v.Fmario_scrolly;

	WriteHandler mario_gfxbank_w = v.mario_gfxbank_w();
	WriteHandler mario_palettebank_w = v.mario_palettebank_w();
	Vh_start generic_vs = (Vh_start) v;
	Vh_convert_color_proms mario_pi = (Vh_convert_color_proms) v;
	Vh_refresh mario_vu = (Vh_refresh) v;
	WriteHandler videoram_w = v.videoram_w();

	jef.machine.BasicMachine m = new jef.machine.BasicMachine();
	InterruptHandler nmi_line_pulse = m.nmi_interrupt_switched();
	WriteHandler interrupt_enable_w = m.nmi_interrupt_enable();

	private boolean readmem() {
		MR_START(0x0000, 0x5fff, MRA_ROM);
		MR_ADD(0x6000, 0x6fff, MRA_RAM);
		MR_ADD(0x7400, 0x77ff, MRA_RAM); /* video RAM */
		MR_ADD(0x7c00, 0x7c00, input_port_0_r); /* IN0 */
		MR_ADD(0x7c80, 0x7c80, input_port_1_r); /* IN1 */
		MR_ADD(0x7f80, 0x7f80, input_port_2_r); /* DSW */
		MR_ADD(0xf000, 0xffff, MRA_ROM);
		return true;
	}

	private boolean writemem() {
		MW_START(0x0000, 0x5fff, MWA_ROM);
		MW_ADD(0x6000, 0x68ff, MWA_RAM);
		MW_ADD(0x6a80, 0x6fff, MWA_RAM);
		MW_ADD(0x6900, 0x6a7f, MWA_RAM, spriteram, spriteram_size);
		MW_ADD(0x7400, 0x77ff, videoram_w, videoram, videoram_size);
		//MW_ADD( 0x7c00, 0x7c00, mario_sh1_w ); /* Mario run sample */
		//MW_ADD( 0x7c80, 0x7c80, mario_sh2_w ); /* Luigi run sample */
		MW_ADD(0x7d00, 0x7d00, MWA_RAM, mario_scrolly);
		MW_ADD(0x7e80, 0x7e80, mario_gfxbank_w);
		MW_ADD(0x7e83, 0x7e83, mario_palettebank_w);
		MW_ADD(0x7e84, 0x7e84, interrupt_enable_w);
		//MW_ADD( 0x7f00, 0x7f00, mario_sh_w ); /* death */
		//MW_ADD( 0x7f01, 0x7f01, mario_sh_getcoin_w );
		//MW_ADD( 0x7f03, 0x7f03, mario_sh_crab_w );
		//MW_ADD( 0x7f04, 0x7f04, mario_sh_turtle_w );
		//MW_ADD( 0x7f05, 0x7f05, mario_sh_fly_w );
		//MW_ADD( 0x7f00, 0x7f07, mario_sh3_w ); /* Misc discrete samples */
		//MW_ADD( 0x7e00, 0x7e00, mario_sh_tuneselect_w );
		MW_ADD(0x7000, 0x73ff, MWA_NOP); /* ??? */
		//	MW_ADD( 0x7e85, 0x7e85, MWA_RAM ); /* Sets alternative 1 and 0 */
		MW_ADD(0xf000, 0xffff, MWA_ROM);
		return true;
	}

	private boolean masao_writemem() {
		MW_START(0x0000, 0x5fff, MWA_ROM);
		MW_ADD(0x6000, 0x68ff, MWA_RAM);
		MW_ADD(0x6a80, 0x6fff, MWA_RAM);
		MW_ADD(0x6900, 0x6a7f, MWA_RAM, spriteram, spriteram_size);
		MW_ADD(0x7400, 0x77ff, videoram_w, videoram, videoram_size);
		MW_ADD(0x7d00, 0x7d00, MWA_RAM, mario_scrolly);
		//MW_ADD( 0x7e00, 0x7e00, soundlatch_w );
		MW_ADD(0x7e80, 0x7e80, mario_gfxbank_w);
		MW_ADD(0x7e83, 0x7e83, mario_palettebank_w);
		MW_ADD(0x7e84, 0x7e84, interrupt_enable_w);
		MW_ADD(0x7000, 0x73ff, MWA_NOP); /* ??? */
		//MW_ADD( 0x7f00, 0x7f00, masao_sh_irqtrigger_w );
		MW_ADD(0xf000, 0xffff, MWA_ROM);
		return true;
	}

	private boolean mario_writeport() {
		//PW_START( 0x00, 0x00, IOWP_NOP ); /* unknown... is this a trigger?
		// */
		return true;
	}

	private boolean ipt_mario() {
		PORT_START(); /* IN0 */
		PORT_BIT(0x01, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_2WAY);
		PORT_BIT(0x02, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT | IPF_2WAY);
		PORT_BIT(0x04, IP_ACTIVE_HIGH, IPT_UNKNOWN);
		PORT_BIT(0x08, IP_ACTIVE_HIGH, IPT_UNKNOWN);
		PORT_BIT(0x10, IP_ACTIVE_HIGH, IPT_BUTTON1);
		PORT_BIT(0x20, IP_ACTIVE_HIGH, IPT_START1);
		PORT_BIT(0x40, IP_ACTIVE_HIGH, IPT_START2);
		PORT_BITX(
			0x80,
			IP_ACTIVE_HIGH,
			IPT_SERVICE,
			DEF_STR2(Service_Mode),
			KEYCODE_F2,
			IP_JOY_NONE);

		PORT_START(); /* IN1 */
		PORT_BIT(0x01, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_2WAY | IPF_PLAYER2);
		PORT_BIT(0x02, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT | IPF_2WAY | IPF_PLAYER2);
		PORT_BIT(0x04, IP_ACTIVE_HIGH, IPT_UNKNOWN);
		PORT_BIT(0x08, IP_ACTIVE_HIGH, IPT_UNKNOWN);
		PORT_BIT(0x10, IP_ACTIVE_HIGH, IPT_BUTTON1 | IPF_PLAYER2);
		PORT_BIT(0x20, IP_ACTIVE_HIGH, IPT_COIN2);
		PORT_BIT(0x40, IP_ACTIVE_HIGH, IPT_COIN1);
		PORT_BIT(0x80, IP_ACTIVE_HIGH, IPT_UNKNOWN);

		PORT_START(); /* DSW0 */
		PORT_DIPNAME(0x03, 0x00, DEF_STR2(Lives));
		PORT_DIPSETTING(0x00, "3");
		PORT_DIPSETTING(0x01, "4");
		PORT_DIPSETTING(0x02, "5");
		PORT_DIPSETTING(0x03, "6");
		PORT_DIPNAME(0x0c, 0x00, DEF_STR2(Coinage));
		PORT_DIPSETTING(0x04, DEF_STR2(_2C_1C));
		PORT_DIPSETTING(0x00, DEF_STR2(_1C_1C));
		PORT_DIPSETTING(0x08, DEF_STR2(_1C_2C));
		PORT_DIPSETTING(0x0c, DEF_STR2(_1C_3C));
		PORT_DIPNAME(0x30, 0x00, DEF_STR2(Bonus_Life));
		PORT_DIPSETTING(0x00, "20000");
		PORT_DIPSETTING(0x10, "30000");
		PORT_DIPSETTING(0x20, "40000");
		PORT_DIPSETTING(0x30, "None");
		PORT_DIPNAME(0xc0, 0x00, DEF_STR2(Difficulty));
		PORT_DIPSETTING(0x00, "Easy");
		PORT_DIPSETTING(0x40, "Medium");
		PORT_DIPSETTING(0x80, "Hard");
		PORT_DIPSETTING(0xc0, "Hardest");
		return true;
	}

	private boolean ipt_mariojp() {
		PORT_START(); /* IN0 */
		PORT_BIT(0x01, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_2WAY);
		PORT_BIT(0x02, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT | IPF_2WAY);
		PORT_BIT(0x04, IP_ACTIVE_HIGH, IPT_UNKNOWN);
		PORT_BIT(0x08, IP_ACTIVE_HIGH, IPT_UNKNOWN);
		PORT_BIT(0x10, IP_ACTIVE_HIGH, IPT_BUTTON1);
		PORT_BIT(0x20, IP_ACTIVE_HIGH, IPT_START1);
		PORT_BIT(0x40, IP_ACTIVE_HIGH, IPT_START2);
		PORT_BITX(
			0x80,
			IP_ACTIVE_HIGH,
			IPT_SERVICE,
			DEF_STR2(Service_Mode),
			KEYCODE_F2,
			IP_JOY_NONE);

		PORT_START(); /* IN1 */
		PORT_BIT(0x01, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_2WAY | IPF_PLAYER2);
		PORT_BIT(0x02, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT | IPF_2WAY | IPF_PLAYER2);
		PORT_BIT(0x04, IP_ACTIVE_HIGH, IPT_UNKNOWN);
		PORT_BIT(0x08, IP_ACTIVE_HIGH, IPT_UNKNOWN);
		PORT_BIT(0x10, IP_ACTIVE_HIGH, IPT_BUTTON1 | IPF_PLAYER2);
		PORT_BIT(0x20, IP_ACTIVE_HIGH, IPT_COIN1);
		PORT_BIT(0x40, IP_ACTIVE_HIGH, IPT_COIN2);
		/* doesn't work in game, but does in service mode */
		PORT_BIT(0x80, IP_ACTIVE_HIGH, IPT_UNKNOWN);

		PORT_START(); /* DSW0 */
		PORT_DIPNAME(0x03, 0x00, DEF_STR2(Lives));
		PORT_DIPSETTING(0x00, "3");
		PORT_DIPSETTING(0x01, "4");
		PORT_DIPSETTING(0x02, "5");
		PORT_DIPSETTING(0x03, "6");
		PORT_DIPNAME(0x1c, 0x00, DEF_STR2(Coinage));
		PORT_DIPSETTING(0x08, DEF_STR2(_3C_1C));
		PORT_DIPSETTING(0x10, DEF_STR2(_2C_1C));
		PORT_DIPSETTING(0x00, DEF_STR2(_1C_1C));
		PORT_DIPSETTING(0x18, DEF_STR2(_1C_2C));
		PORT_DIPSETTING(0x04, DEF_STR2(_1C_3C));
		PORT_DIPSETTING(0x0c, DEF_STR2(_1C_4C));
		PORT_DIPSETTING(0x14, DEF_STR2(_1C_5C));
		PORT_DIPSETTING(0x1c, DEF_STR2(_1C_6C));
		PORT_DIPNAME(0x20, 0x20, "2 Players Game");
		PORT_DIPSETTING(0x00, "1 Credit");
		PORT_DIPSETTING(0x20, "2 Credits");
		PORT_DIPNAME(0xc0, 0x00, DEF_STR2(Bonus_Life));
		PORT_DIPSETTING(0x00, "20000");
		PORT_DIPSETTING(0x40, "30000");
		PORT_DIPSETTING(0x80, "40000");
		PORT_DIPSETTING(0xc0, "None");
		return true;
	}

	int[][] charlayout = { { 8 }, {
			8 }, /* 8*8 characters */ {
			512 }, /* 512 characters */ {
			2 }, /* 2 bits per pixel */ {
			0, 512 * 8 * 8 }, /* the bitplanes are separated */ {
			0, 1, 2, 3, 4, 5, 6, 7 }, /* pretty straightforward layout */ {
			0 * 8, 1 * 8, 2 * 8, 3 * 8, 4 * 8, 5 * 8, 6 * 8, 7 * 8 }, {
			8 * 8 } /* every char takes 8 consecutive bytes */
	};

	int[][] spritelayout = { { 16 }, {
			16 }, /* 16*16 sprites */ {
			256 }, /* 256 sprites */ {
			3 }, /* 3 bits per pixel */ {
			0, 256 * 16 * 16, 2 * 256 * 16 * 16 }, /*
												    * the bitplanes are
												    * separated
												    */ {
			0, 1, 2, 3, 4, 5, 6, 7, /*
									 * the two halves of the sprite are
									 * separated
									 */
			256 * 16 * 8 + 0,
				256 * 16 * 8 + 1,
				256 * 16 * 8 + 2,
				256 * 16 * 8 + 3,
				256 * 16 * 8 + 4,
				256 * 16 * 8 + 5,
				256 * 16 * 8 + 6,
				256 * 16 * 8 + 7 },
				{
			0 * 8,
				1 * 8,
				2 * 8,
				3 * 8,
				4 * 8,
				5 * 8,
				6 * 8,
				7 * 8,
				8 * 8,
				9 * 8,
				10 * 8,
				11 * 8,
				12 * 8,
				13 * 8,
				14 * 8,
				15 * 8 },
				{
			16 * 8 } /* every sprite takes 16 consecutive bytes */
	};

	private boolean gfxdecodeinfo() {
		GDI_ADD(REGION_GFX1, 0, charlayout, 0, 16);
		GDI_ADD(REGION_GFX2, 0, spritelayout, 16 * 4, 32);
		GDI_ADD(-1); /* end of array */
		return true;
	}

	public boolean mdrv_mario() {

		/* basic machine hardware */
		MDRV_CPU_ADD(Z80, 3072000); /* 3.072 MHz (?) */
		MDRV_CPU_MEMORY(readmem(), writemem());
		//MDRV_CPU_PORTS(0,mario_writeport());
		MDRV_CPU_VBLANK_INT(nmi_line_pulse, 1);

		//MDRV_CPU_ADD(I8039, 730000);
		//MDRV_CPU_FLAGS(CPU_AUDIO_CPU); /* 730 kHz */
		//MDRV_CPU_MEMORY(readmem_sound(),writemem_sound());
		//MDRV_CPU_PORTS(readport_sound(),writeport_sound());

		MDRV_FRAMES_PER_SECOND(60);
		MDRV_VBLANK_DURATION(DEFAULT_60HZ_VBLANK_DURATION);

		/* video hardware */
		MDRV_VIDEO_ATTRIBUTES(VIDEO_TYPE_RASTER);
		MDRV_SCREEN_SIZE(32 * 8, 32 * 8);
		MDRV_VISIBLE_AREA(0 * 8, 32 * 8 - 1, 2 * 8, 30 * 8 - 1);
		MDRV_GFXDECODE(gfxdecodeinfo());
		MDRV_PALETTE_LENGTH(256);
		MDRV_COLORTABLE_LENGTH(16 * 4 + 32 * 8);

		MDRV_PALETTE_INIT(mario_pi);
		MDRV_VIDEO_START(generic_vs);
		MDRV_VIDEO_UPDATE(mario_vu);

		/* sound hardware */
		//MDRV_SOUND_ADD(DAC, dac_interface);
		//MDRV_SOUND_ADD(SAMPLES, samples_interface);
		return true;
	}

	public boolean mdrv_masao() {

		/* basic machine hardware */
		MDRV_CPU_ADD(Z80, 4000000); /* 4.000 MHz (?) */
		MDRV_CPU_MEMORY(readmem(), masao_writemem());
		MDRV_CPU_VBLANK_INT(nmi_line_pulse, 1);

		//MDRV_CPU_ADD(Z80,24576000/16);
		//MDRV_CPU_FLAGS(CPU_AUDIO_CPU); /* ???? */
		//MDRV_CPU_MEMORY(masao_sound_readmem(),masao_sound_writemem());

		MDRV_FRAMES_PER_SECOND(60);
		MDRV_VBLANK_DURATION(DEFAULT_60HZ_VBLANK_DURATION);

		/* video hardware */
		MDRV_VIDEO_ATTRIBUTES(VIDEO_TYPE_RASTER);
		MDRV_SCREEN_SIZE(32 * 8, 32 * 8);
		MDRV_VISIBLE_AREA(0 * 8, 32 * 8 - 1, 2 * 8, 30 * 8 - 1);
		MDRV_GFXDECODE(gfxdecodeinfo());
		MDRV_PALETTE_LENGTH(256);
		MDRV_COLORTABLE_LENGTH(16 * 4 + 32 * 8);

		MDRV_PALETTE_INIT(mario_pi);
		MDRV_VIDEO_START(generic_vs);
		MDRV_VIDEO_UPDATE(mario_vu);

		/* sound hardware */
		//MDRV_SOUND_ADD(AY8910, ay8910_interface);
		return true;
	}

	/***************************************************************************
	 * 
	 * Game driver(s)
	 *  
	 **************************************************************************/

	private boolean rom_mario() {
		ROM_REGION(0x10000, REGION_CPU1, 0); /* 64k for code */
		ROM_LOAD("mario.7f", 0x0000, 0x2000, 0xc0c6e014);
		ROM_LOAD("mario.7e", 0x2000, 0x2000, 0x116b3856);
		ROM_LOAD("mario.7d", 0x4000, 0x2000, 0xdcceb6c1);
		ROM_LOAD("mario.7c", 0xf000, 0x1000, 0x4a63d96b);

		ROM_REGION(0x1000, REGION_CPU2, 0); /* sound */
		ROM_LOAD("tma1c-a.6k", 0x0000, 0x1000, 0x06b9ff85);

		ROM_REGION(0x2000, REGION_GFX1, ROMREGION_DISPOSE);
		ROM_LOAD("mario.3f", 0x0000, 0x1000, 0x28b0c42c);
		ROM_LOAD("mario.3j", 0x1000, 0x1000, 0x0c8cc04d);

		ROM_REGION(0x6000, REGION_GFX2, ROMREGION_DISPOSE);
		ROM_LOAD("mario.7m", 0x0000, 0x1000, 0x22b7372e);
		ROM_LOAD("mario.7n", 0x1000, 0x1000, 0x4f3a1f47);
		ROM_LOAD("mario.7p", 0x2000, 0x1000, 0x56be6ccd);
		ROM_LOAD("mario.7s", 0x3000, 0x1000, 0x56f1d613);
		ROM_LOAD("mario.7t", 0x4000, 0x1000, 0x641f0008);
		ROM_LOAD("mario.7u", 0x5000, 0x1000, 0x7baf5309);

		ROM_REGION(0x0200, REGION_PROMS, 0);
		ROM_LOAD("mario.4p", 0x0000, 0x0200, 0xafc9bd41);
		return true;
	}

	private boolean rom_mariojp() {
		ROM_REGION(0x10000, REGION_CPU1, 0); /* 64k for code */
		ROM_LOAD("tma1c-a1.7f", 0x0000, 0x2000, 0xb64b6330);
		ROM_LOAD("tma1c-a2.7e", 0x2000, 0x2000, 0x290c4977);
		ROM_LOAD("tma1c-a1.7d", 0x4000, 0x2000, 0xf8575f31);
		ROM_LOAD("tma1c-a2.7c", 0xf000, 0x1000, 0xa3c11e9e);

		ROM_REGION(0x1000, REGION_CPU2, 0); /* sound */
		ROM_LOAD("tma1c-a.6k", 0x0000, 0x1000, 0x06b9ff85);

		ROM_REGION(0x2000, REGION_GFX1, ROMREGION_DISPOSE);
		ROM_LOAD("tma1v-a.3f", 0x0000, 0x1000, 0xadf49ee0);
		ROM_LOAD("tma1v-a.3j", 0x1000, 0x1000, 0xa5318f2d);

		ROM_REGION(0x6000, REGION_GFX2, ROMREGION_DISPOSE);
		ROM_LOAD("tma1v-a.7m", 0x0000, 0x1000, 0x186762f8);
		ROM_LOAD("tma1v-a.7n", 0x1000, 0x1000, 0xe0e08bba);
		ROM_LOAD("tma1v-a.7p", 0x2000, 0x1000, 0x7b27c8c1);
		ROM_LOAD("tma1v-a.7s", 0x3000, 0x1000, 0x912ba80a);
		ROM_LOAD("tma1v-a.7t", 0x4000, 0x1000, 0x5cbb92a5);
		ROM_LOAD("tma1v-a.7u", 0x5000, 0x1000, 0x13afb9ed);

		ROM_REGION(0x0200, REGION_PROMS, 0);
		ROM_LOAD("mario.4p", 0x0000, 0x0200, 0xafc9bd41);
		return true;
	}

	private boolean rom_masao() {
		ROM_REGION(0x10000, REGION_CPU1, 0); /* 64k for code */
		ROM_LOAD("masao-4.rom", 0x0000, 0x2000, 0x07a75745);
		ROM_LOAD("masao-3.rom", 0x2000, 0x2000, 0x55c629b6);
		ROM_LOAD("masao-2.rom", 0x4000, 0x2000, 0x42e85240);
		ROM_LOAD("masao-1.rom", 0xf000, 0x1000, 0xb2817af9);

		ROM_REGION(0x10000, REGION_CPU2, 0); /* 64k for sound */
		ROM_LOAD("masao-5.rom", 0x0000, 0x1000, 0xbd437198);

		ROM_REGION(0x2000, REGION_GFX1, ROMREGION_DISPOSE);
		ROM_LOAD("masao-6.rom", 0x0000, 0x1000, 0x1c9e0be2);
		ROM_LOAD("masao-7.rom", 0x1000, 0x1000, 0x747c1349);

		ROM_REGION(0x6000, REGION_GFX2, ROMREGION_DISPOSE);
		ROM_LOAD("tma1v-a.7m", 0x0000, 0x1000, 0x186762f8);
		ROM_LOAD("masao-9.rom", 0x1000, 0x1000, 0x50be3918);
		ROM_LOAD("mario.7p", 0x2000, 0x1000, 0x56be6ccd);
		ROM_LOAD("tma1v-a.7s", 0x3000, 0x1000, 0x912ba80a);
		ROM_LOAD("tma1v-a.7t", 0x4000, 0x1000, 0x5cbb92a5);
		ROM_LOAD("tma1v-a.7u", 0x5000, 0x1000, 0x13afb9ed);

		ROM_REGION(0x0200, REGION_PROMS, 0);
		ROM_LOAD("mario.4p", 0x0000, 0x0200, 0xafc9bd41);
		return true;
	}

	public Machine getMachine(URL url, String name) {
		super.getMachine(url, name);
		super.setVideoEmulator(v);
		m = new jef.machine.BasicMachine();

		if (name.equals("mario")) {
			GAME(
				1983,
				rom_mario(),
				0,
				mdrv_mario(),
				ipt_mario(),
				0,
				ROT180,
				"Nintendo of America",
				"Mario Bros. (US)");
		} else if (name.equals("mariojp")) {
			GAME(
				1983,
				rom_mariojp(),
				"mario",
				mdrv_mario(),
				ipt_mariojp(),
				0,
				ROT180,
				"Nintendo",
				"Mario Bros. (Japan)");
		} else if (name.equals("masao")) {
			GAME(
				1983,
				rom_masao(),
				"mario",
				mdrv_masao(),
				ipt_mario(),
				0,
				ROT180,
				"bootleg",
				"Masao");
		}

		m.init(md);

		return (Machine) m;
	}

}
