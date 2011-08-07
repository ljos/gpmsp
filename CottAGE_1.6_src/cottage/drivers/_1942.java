/*
 * CottAGE - the Arcade Generic Emulator in Java
 * 
 * Java driver by Gollum
 */

/*******************************************************************************
 * 
 * 1942
 * 
 * driver by Paul Leaman
 * 
 * MAIN CPU:
 * 
 * 0000-bfff ROM (8000-bfff banked) cc00-cc7f Sprites d000-d3ff Video RAM
 * d400-d7ff Color RAM d800-dbff Background RAM (groups of 32 bytes, 16 code,
 * 16 color/attribute) e000-efff RAM
 * 
 * read: c000 IN0 c001 IN1 c002 IN2 c003 DSW0 c004 DSW1
 * 
 * write: c800 command for the audio CPU c802-c803 background scroll c804 bit 7:
 * flip screen bit 4: cpu B reset bit 0: coin counter c805 background palette
 * bank selector c806 bit 0-1 ROM bank selector 00=1-N5.BIN 01=1-N6.BIN
 * 10=1-N7.BIN
 * 
 * 
 * 
 * SOUND CPU:
 * 
 * 0000-3fff ROM 4000-47ff RAM 6000 command from the main CPU 8000 8910 #1
 * control 8001 8910 #1 write c000 8910 #2 control c001 8910 #2 write
 * 
 * 
 * 
 * Game runs in interrupt mode 0 (the devices supply the interrupt number).
 * 
 * Two interrupts must be triggered per refresh for the game to function
 * correctly.
 * 
 * 0x10 is the video retrace. This controls the speed of the game and generally
 * drives the code. This must be triggerd for each video retrace. 0x08 is the
 * sound card service interupt. The game uses this to throw sounds at the sound
 * CPU.
 *  
 ******************************************************************************/

package cottage.drivers;

import java.net.URL;

import jef.machine.Machine;
import jef.map.InterruptHandler;
import jef.map.WriteHandler;
import jef.sound.chip.AY8910;
import jef.video.Vh_convert_color_proms;
import jef.video.Vh_refresh;
import jef.video.Vh_start;

import cottage.mame.MAMEDriver;

public class _1942 extends MAMEDriver {

	cottage.vidhrdw._1942 v = new cottage.vidhrdw._1942();
	int[] c1942_fgvideoram = v.Fc1942_fgvideoram;
	int[] c1942_bgvideoram = v.Fc1942_bgvideoram;
	Vh_start _1942_vs = (Vh_start) v;
	Vh_convert_color_proms _1942_pi = (Vh_convert_color_proms) v;
	WriteHandler c1942_fgvideoram_w = v.c1942_fgvideoram_w();
	WriteHandler c1942_bgvideoram_w = v.c1942_bgvideoram_w();
	WriteHandler c1942_scroll_w = v.c1942_scroll_w();
	//WriteHandler c1942_c804_w = v.c1942_c804_w();
	WriteHandler c1942_palette_bank_w = v.c1942_palette_bank_w();
	Vh_refresh _1942_vu = (Vh_refresh) v;

	jef.machine.BasicMachine m = new jef.machine.BasicMachine();
	InterruptHandler irq0_line_hold = m.irq0_line_hold();

	AY8910 ay = new AY8910(2, 1500000);

	WriteHandler c1942_bankswitch_w() {
		return new C1942_bankswitch_w();
	}
	InterruptHandler c1942_interrupt() {
		return new C1942_interrupt();
	}

	public class C1942_bankswitch_w implements WriteHandler {
		public void write(int address, int data) {
			int bankaddress;
			int[] RAM = memory_region(REGION_CPU1);

			bankaddress = 0x10000 + (data & 0x03) * 0x4000;
			cpu_setbank(1, bankaddress);
		}
	}

	public class C1942_interrupt implements InterruptHandler {
		public int irq() {
			if (m.getCurrentSlice() != 0) {
				m.cb[0].getCpu().setProperty(0, 0x08); /* RST 08h */
				return 0;
			} else {
				m.cb[0].getCpu().setProperty(0, 0x10); /* RST 10h - vblank */
				return 0;
			}
		}
	}

	private boolean readmem() {
		MR_START(0x0000, 0x7fff, MRA_ROM);
		MR_ADD(0x8000, 0xbfff, MRA_BANK1);
		MR_ADD(0xc000, 0xc000, input_port_0_r); /* IN0 */
		MR_ADD(0xc001, 0xc001, input_port_1_r); /* IN1 */
		MR_ADD(0xc002, 0xc002, input_port_2_r); /* IN2 */
		MR_ADD(0xc003, 0xc003, input_port_3_r); /* DSW0 */
		MR_ADD(0xc004, 0xc004, input_port_4_r); /* DSW1 */
		MR_ADD(0xd000, 0xdbff, MRA_RAM);
		MR_ADD(0xe000, 0xefff, MRA_RAM);
		return true;
	}

	private boolean writemem() {

		MW_START(0x0000, 0xffff, MWA_RAM);
		MW_ADD(0x0000, 0xbfff, MWA_ROM);
		MW_ADD(0xc800, 0xc800, soundlatch_w);
		MW_ADD(0xc802, 0xc803, c1942_scroll_w);
		//MW_ADD( 0xc804, 0xc804, c1942_c804_w );
		MW_ADD(0xc805, 0xc805, c1942_palette_bank_w);
		MW_ADD(0xc806, 0xc806, c1942_bankswitch_w());
		MW_ADD(0xcc00, 0xcc7f, MWA_RAM, spriteram, spriteram_size);
		MW_ADD(0xd000, 0xd7ff, c1942_fgvideoram_w, c1942_fgvideoram);
		MW_ADD(0xd800, 0xdbff, c1942_bgvideoram_w, c1942_bgvideoram);
		MW_ADD(0xe000, 0xefff, MWA_RAM);
		return true;
	}

	private boolean sound_readmem() {
		MR_START(0x0000, 0x3fff, MRA_ROM);
		MR_ADD(0x4000, 0x47ff, MRA_RAM);
		MR_ADD(0x6000, 0x6000, soundlatch_r);
		return true;
	}

	private boolean sound_writemem() {
		MW_START(0x0000, 0x3fff, MWA_ROM);
		MW_ADD(0x4000, 0x47ff, MWA_RAM);
		MW_ADD(0x8000, 0x8000, ay.AY8910_control_port_0_w());
		MW_ADD(0x8001, 0x8001, ay.AY8910_write_port_0_w());
		MW_ADD(0xc000, 0xc000, ay.AY8910_control_port_1_w());
		MW_ADD(0xc001, 0xc001, ay.AY8910_write_port_1_w());
		return true;
	}

	private boolean ipt_1942() {
		PORT_START(); /* IN0 */
		PORT_BIT(0x01, IP_ACTIVE_LOW, IPT_START1);
		PORT_BIT(0x02, IP_ACTIVE_LOW, IPT_START2);
		PORT_BIT(0x04, IP_ACTIVE_LOW, IPT_UNKNOWN); /* probably unused */
		PORT_BIT(0x08, IP_ACTIVE_LOW, IPT_UNKNOWN); /* probably unused */
		PORT_BIT(0x10, IP_ACTIVE_LOW, IPT_SERVICE1);
		PORT_BIT(0x20, IP_ACTIVE_LOW, IPT_UNKNOWN); /* probably unused */
		PORT_BIT(0x40, IP_ACTIVE_LOW, IPT_COIN2);
		PORT_BIT(0x80, IP_ACTIVE_LOW, IPT_COIN1);

		PORT_START(); /* IN1 */
		PORT_BIT(0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY);
		PORT_BIT(0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT | IPF_8WAY);
		PORT_BIT(0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN | IPF_8WAY);
		PORT_BIT(0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_UP | IPF_8WAY);
		PORT_BIT(0x10, IP_ACTIVE_LOW, IPT_BUTTON1);
		PORT_BIT(0x20, IP_ACTIVE_LOW, IPT_BUTTON2);
		PORT_BIT(0x40, IP_ACTIVE_LOW, IPT_UNKNOWN); /* probably unused */
		PORT_BIT(0x80, IP_ACTIVE_LOW, IPT_UNKNOWN); /* probably unused */

		PORT_START(); /* IN2 */
		PORT_BIT(0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_COCKTAIL);
		PORT_BIT(0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT | IPF_8WAY | IPF_COCKTAIL);
		PORT_BIT(0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN | IPF_8WAY | IPF_COCKTAIL);
		PORT_BIT(0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_UP | IPF_8WAY | IPF_COCKTAIL);
		PORT_BIT(0x10, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_COCKTAIL);
		PORT_BIT(0x20, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_COCKTAIL);
		PORT_BIT(0x40, IP_ACTIVE_LOW, IPT_UNKNOWN); /* probably unused */
		PORT_BIT(0x80, IP_ACTIVE_LOW, IPT_UNKNOWN); /* probably unused */

		PORT_START(); /* DSW0 */
		PORT_DIPNAME(0x07, 0x07, DEF_STR2(Coin_A));
		PORT_DIPSETTING(0x01, DEF_STR2(_4C_1C));
		PORT_DIPSETTING(0x02, DEF_STR2(_3C_1C));
		PORT_DIPSETTING(0x04, DEF_STR2(_2C_1C));
		PORT_DIPSETTING(0x07, DEF_STR2(_1C_1C));
		PORT_DIPSETTING(0x03, DEF_STR2(_2C_3C));
		PORT_DIPSETTING(0x06, DEF_STR2(_1C_2C));
		PORT_DIPSETTING(0x05, DEF_STR2(_1C_4C));
		PORT_DIPSETTING(0x00, DEF_STR2(Free_Play));
		PORT_DIPNAME(0x08, 0x00, DEF_STR2(Cabinet));
		PORT_DIPSETTING(0x00, DEF_STR2(Upright));
		PORT_DIPSETTING(0x08, DEF_STR2(Cocktail));
		PORT_DIPNAME(0x30, 0x30, DEF_STR2(Bonus_Life));
		PORT_DIPSETTING(0x30, "20000 80000");
		PORT_DIPSETTING(0x20, "20000 100000");
		PORT_DIPSETTING(0x10, "30000 80000");
		PORT_DIPSETTING(0x00, "30000 100000");
		PORT_DIPNAME(0xc0, 0xc0, DEF_STR2(Lives));
		PORT_DIPSETTING(0x80, "1");
		PORT_DIPSETTING(0x40, "2");
		PORT_DIPSETTING(0xc0, "3");
		PORT_DIPSETTING(0x00, "5");

		PORT_START(); /* DSW1 */
		PORT_DIPNAME(0x07, 0x07, DEF_STR2(Coin_B));
		PORT_DIPSETTING(0x01, DEF_STR2(_4C_1C));
		PORT_DIPSETTING(0x02, DEF_STR2(_3C_1C));
		PORT_DIPSETTING(0x04, DEF_STR2(_2C_1C));
		PORT_DIPSETTING(0x07, DEF_STR2(_1C_1C));
		PORT_DIPSETTING(0x03, DEF_STR2(_2C_3C));
		PORT_DIPSETTING(0x06, DEF_STR2(_1C_2C));
		PORT_DIPSETTING(0x05, DEF_STR2(_1C_4C));
		PORT_DIPSETTING(0x00, DEF_STR2(Free_Play));
		PORT_SERVICE(0x08, IP_ACTIVE_LOW);
		PORT_DIPNAME(0x10, 0x10, DEF_STR2(Flip_Screen));
		PORT_DIPSETTING(0x10, DEF_STR2(Off));
		PORT_DIPSETTING(0x00, DEF_STR2(On));
		PORT_DIPNAME(0x60, 0x60, DEF_STR2(Difficulty));
		PORT_DIPSETTING(0x40, "Easy");
		PORT_DIPSETTING(0x60, "Normal");
		PORT_DIPSETTING(0x20, "Hard");
		PORT_DIPSETTING(0x00, "Hardest");
		PORT_DIPNAME(0x80, 0x80, "Freeze");
		PORT_DIPSETTING(0x80, DEF_STR2(Off));
		PORT_DIPSETTING(0x00, DEF_STR2(On));
		return true;
	}

	int[][] charlayout = { { 8 }, {
			8 }, {
			RGN_FRAC(1, 1)
			}, {
			2 }, {
			4, 0 }, {
			0, 1, 2, 3, 8 + 0, 8 + 1, 8 + 2, 8 + 3 }, {
			0 * 16, 1 * 16, 2 * 16, 3 * 16, 4 * 16, 5 * 16, 6 * 16, 7 * 16 }, {
			16 * 8 }
	};

	int[][] tilelayout = { { 16 }, {
			16 }, {
			RGN_FRAC(1, 3)
			}, {
			3 }, {
			RGN_FRAC(2, 3), RGN_FRAC(1, 3), RGN_FRAC(0, 3)
			}, {
			0,
				1,
				2,
				3,
				4,
				5,
				6,
				7,
				16 * 8 + 0,
				16 * 8 + 1,
				16 * 8 + 2,
				16 * 8 + 3,
				16 * 8 + 4,
				16 * 8 + 5,
				16 * 8 + 6,
				16 * 8 + 7 },
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
			32 * 8 }
	};

	int[][] spritelayout = { { 16 }, {
			16 }, {
			RGN_FRAC(1, 2)
			}, {
			4 }, {
			4, 0, RGN_FRAC(1, 2) + 4, RGN_FRAC(1, 2) + 0 }, {
			0,
				1,
				2,
				3,
				8 + 0,
				8 + 1,
				8 + 2,
				8 + 3,
				16 * 16 + 0,
				16 * 16 + 1,
				16 * 16 + 2,
				16 * 16 + 3,
				16 * 16 + 8 + 0,
				16 * 16 + 8 + 1,
				16 * 16 + 8 + 2,
				16 * 16 + 8 + 3 },
				{
			0 * 16,
				1 * 16,
				2 * 16,
				3 * 16,
				4 * 16,
				5 * 16,
				6 * 16,
				7 * 16,
				8 * 16,
				9 * 16,
				10 * 16,
				11 * 16,
				12 * 16,
				13 * 16,
				14 * 16,
				15 * 16 },
				{
			64 * 8 }
	};

	private boolean gfxdecodeinfo() {
		GDI_ADD(REGION_GFX1, 0, charlayout, 0, 64);
		GDI_ADD(REGION_GFX2, 0, tilelayout, 64 * 4, 4 * 32);
		GDI_ADD(REGION_GFX3, 0, spritelayout, 64 * 4 + 4 * 32 * 8, 16);
		GDI_ADD(-1); /* end of array */
		return true;
	};

	public boolean mdrv_1942() {

		/* basic machine hardware */
		MDRV_CPU_ADD(Z80, 4000000); /* 4 MHz (?) */
		MDRV_CPU_MEMORY(readmem(), writemem());
		MDRV_CPU_VBLANK_INT(c1942_interrupt(), 2);

		MDRV_CPU_ADD(Z80, 3000000);
		MDRV_CPU_FLAGS(CPU_AUDIO_CPU); /* 3 MHz ??? */
		MDRV_CPU_MEMORY(sound_readmem(), sound_writemem());
		MDRV_CPU_VBLANK_INT(irq0_line_hold, 4);

		/* sound hardware */
		MDRV_SOUND_ADD(ay);

		MDRV_FRAMES_PER_SECOND(60);
		MDRV_VBLANK_DURATION(DEFAULT_60HZ_VBLANK_DURATION);
		MDRV_INTERLEAVE(100);

		/* video hardware */
		MDRV_VIDEO_ATTRIBUTES(VIDEO_TYPE_RASTER);
		MDRV_SCREEN_SIZE(32 * 8, 32 * 8);
		MDRV_VISIBLE_AREA(0 * 8, 32 * 8 - 1, 2 * 8, 30 * 8 - 1);
		MDRV_GFXDECODE(gfxdecodeinfo());
		MDRV_PALETTE_LENGTH(256);
		MDRV_COLORTABLE_LENGTH(64 * 4 + 4 * 32 * 8 + 16 * 16);

		MDRV_PALETTE_INIT(_1942_pi);
		MDRV_VIDEO_START(_1942_vs);
		MDRV_VIDEO_UPDATE(_1942_vu);
		return true;
	}

	/***************************************************************************
	 * 
	 * Game driver(s)
	 *  
	 **************************************************************************/

	private boolean rom_1942() {
		ROM_REGION(0x1c000, REGION_CPU1, 0);
		/*
		 * 64k for code + 3*16k for the banked ROMs images
		 */
		ROM_LOAD("1-n3a.bin", 0x00000, 0x4000, 0x40201bab);
		ROM_LOAD("1-n4.bin", 0x04000, 0x4000, 0xa60ac644);
		ROM_LOAD("1-n5.bin", 0x10000, 0x4000, 0x835f7b24);
		ROM_LOAD("1-n6.bin", 0x14000, 0x2000, 0x821c6481);
		ROM_LOAD("1-n7.bin", 0x18000, 0x4000, 0x5df525e1);

		ROM_REGION( 0x10000, REGION_CPU2, 0 ); /* 64k for the audio CPU */
		ROM_LOAD( "1-c11.bin", 0x0000, 0x4000, 0xbd87f06b );

		ROM_REGION(0x2000, REGION_GFX1, ROMREGION_DISPOSE);
		ROM_LOAD("1-f2.bin", 0x0000, 0x2000, 0x6ebca191); /* characters */

		ROM_REGION(0xc000, REGION_GFX2, ROMREGION_DISPOSE);
		ROM_LOAD("2-a1.bin", 0x0000, 0x2000, 0x3884d9eb); /* tiles */
		ROM_LOAD("2-a2.bin", 0x2000, 0x2000, 0x999cf6e0);
		ROM_LOAD("2-a3.bin", 0x4000, 0x2000, 0x8edb273a);
		ROM_LOAD("2-a4.bin", 0x6000, 0x2000, 0x3a2726c3);
		ROM_LOAD("2-a5.bin", 0x8000, 0x2000, 0x1bd3d8bb);
		ROM_LOAD("2-a6.bin", 0xa000, 0x2000, 0x658f02c4);

		ROM_REGION(0x10000, REGION_GFX3, ROMREGION_DISPOSE);
		ROM_LOAD("2-l1.bin", 0x00000, 0x4000, 0x2528bec6); /* sprites */
		ROM_LOAD("2-l2.bin", 0x04000, 0x4000, 0xf89287aa);
		ROM_LOAD("2-n1.bin", 0x08000, 0x4000, 0x024418f8);
		ROM_LOAD("2-n2.bin", 0x0c000, 0x4000, 0xe2c7e489);

		ROM_REGION(0x0a00, REGION_PROMS, 0);
		ROM_LOAD("08e_sb-5.bin", 0x0000, 0x0100, 0x93ab8153); /* red component */
		ROM_LOAD("09e_sb-6.bin", 0x0100, 0x0100, 0x8ab44f7d);
		/*
		 * green component
		 */
		ROM_LOAD("10e_sb-7.bin", 0x0200, 0x0100, 0xf4ade9a4); /* blue component */
		ROM_LOAD("f01_sb-0.bin", 0x0300, 0x0100, 0x6047d91b);
		/*
		 * char lookup table
		 */
		ROM_LOAD("06d_sb-4.bin", 0x0400, 0x0100, 0x4858968d);
		/*
		 * tile lookup table
		 */
		ROM_LOAD("03k_sb-8.bin", 0x0500, 0x0100, 0xf6fad943);
		/*
		 * sprite lookup table
		 */
		ROM_LOAD("01d_sb-2.bin", 0x0600, 0x0100, 0x8bb8b3df);
		/* tile palette selector? (not used) */
		ROM_LOAD("02d_sb-3.bin", 0x0700, 0x0100, 0x3b0c99af);
		/* tile palette selector? (not used) */
		ROM_LOAD("k06_sb-1.bin", 0x0800, 0x0100, 0x712ac508);
		/*
		 * interrupt timing (not used)
		 */
		ROM_LOAD("01m_sb-9.bin", 0x0900, 0x0100, 0x4921635c);
		/*
		 * video timing? (not used)
		 */
		return true;
	}

	private boolean rom_1942a() {
		ROM_REGION(0x1c000, REGION_CPU1, 0);
		/*
		 * 64k for code + 3*16k for the banked ROMs images
		 */
		ROM_LOAD("1-n3.bin", 0x00000, 0x4000, 0x612975f2);
		ROM_LOAD("1-n4.bin", 0x04000, 0x4000, 0xa60ac644);
		ROM_LOAD("1-n5.bin", 0x10000, 0x4000, 0x835f7b24);
		ROM_LOAD("1-n6.bin", 0x14000, 0x2000, 0x821c6481);
		ROM_LOAD("1-n7.bin", 0x18000, 0x4000, 0x5df525e1);

		ROM_REGION(0x10000, REGION_CPU2, 0); /* 64k for the audio CPU */
		ROM_LOAD("1-c11.bin", 0x0000, 0x4000, 0xbd87f06b);

		ROM_REGION(0x2000, REGION_GFX1, ROMREGION_DISPOSE);
		ROM_LOAD("1-f2.bin", 0x0000, 0x2000, 0x6ebca191); /* characters */

		ROM_REGION(0xc000, REGION_GFX2, ROMREGION_DISPOSE);
		ROM_LOAD("2-a1.bin", 0x0000, 0x2000, 0x3884d9eb); /* tiles */
		ROM_LOAD("2-a2.bin", 0x2000, 0x2000, 0x999cf6e0);
		ROM_LOAD("2-a3.bin", 0x4000, 0x2000, 0x8edb273a);
		ROM_LOAD("2-a4.bin", 0x6000, 0x2000, 0x3a2726c3);
		ROM_LOAD("2-a5.bin", 0x8000, 0x2000, 0x1bd3d8bb);
		ROM_LOAD("2-a6.bin", 0xa000, 0x2000, 0x658f02c4);

		ROM_REGION(0x10000, REGION_GFX3, ROMREGION_DISPOSE);
		ROM_LOAD("2-l1.bin", 0x00000, 0x4000, 0x2528bec6); /* sprites */
		ROM_LOAD("2-l2.bin", 0x04000, 0x4000, 0xf89287aa);
		ROM_LOAD("2-n1.bin", 0x08000, 0x4000, 0x024418f8);
		ROM_LOAD("2-n2.bin", 0x0c000, 0x4000, 0xe2c7e489);

		ROM_REGION(0x0a00, REGION_PROMS, 0);
		ROM_LOAD("08e_sb-5.bin", 0x0000, 0x0100, 0x93ab8153); /* red component */
		ROM_LOAD("09e_sb-6.bin", 0x0100, 0x0100, 0x8ab44f7d);
		/*
		 * green component
		 */
		ROM_LOAD("10e_sb-7.bin", 0x0200, 0x0100, 0xf4ade9a4); /* blue component */
		ROM_LOAD("f01_sb-0.bin", 0x0300, 0x0100, 0x6047d91b);
		/*
		 * char lookup table
		 */
		ROM_LOAD("06d_sb-4.bin", 0x0400, 0x0100, 0x4858968d);
		/*
		 * tile lookup table
		 */
		ROM_LOAD("03k_sb-8.bin", 0x0500, 0x0100, 0xf6fad943);
		/*
		 * sprite lookup table
		 */
		ROM_LOAD("01d_sb-2.bin", 0x0600, 0x0100, 0x8bb8b3df);
		/* tile palette selector? (not used) */
		ROM_LOAD("02d_sb-3.bin", 0x0700, 0x0100, 0x3b0c99af);
		/* tile palette selector? (not used) */
		ROM_LOAD("k06_sb-1.bin", 0x0800, 0x0100, 0x712ac508);
		/*
		 * interrupt timing (not used)
		 */
		ROM_LOAD("01m_sb-9.bin", 0x0900, 0x0100, 0x4921635c);
		/*
		 * video timing? (not used)
		 */
		return true;
	}

	private boolean rom_1942b() {
		ROM_REGION(0x1c000, REGION_CPU1, 0);
		/*
		 * 64k for code + 3*16k for the banked ROMs images
		 */
		ROM_LOAD("srb-03.n3", 0x00000, 0x4000, 0xd9dafcc3);
		ROM_LOAD("srb-04.n4", 0x04000, 0x4000, 0xda0cf924);
		ROM_LOAD("srb-05.n5", 0x10000, 0x4000, 0xd102911c);
		ROM_LOAD("srb-06.n6", 0x14000, 0x2000, 0x466f8248);
		ROM_LOAD("srb-07.n7", 0x18000, 0x4000, 0x0d31038c);

		//ROM_REGION( 0x10000, REGION_CPU2, 0 ); /* 64k for the audio CPU */
		//ROM_LOAD( "1-c11.bin", 0x0000, 0x4000, 0xbd87f06b );

		ROM_REGION(0x2000, REGION_GFX1, ROMREGION_DISPOSE);
		ROM_LOAD("1-f2.bin", 0x0000, 0x2000, 0x6ebca191); /* characters */

		ROM_REGION(0xc000, REGION_GFX2, ROMREGION_DISPOSE);
		ROM_LOAD("2-a1.bin", 0x0000, 0x2000, 0x3884d9eb); /* tiles */
		ROM_LOAD("2-a2.bin", 0x2000, 0x2000, 0x999cf6e0);
		ROM_LOAD("2-a3.bin", 0x4000, 0x2000, 0x8edb273a);
		ROM_LOAD("2-a4.bin", 0x6000, 0x2000, 0x3a2726c3);
		ROM_LOAD("2-a5.bin", 0x8000, 0x2000, 0x1bd3d8bb);
		ROM_LOAD("2-a6.bin", 0xa000, 0x2000, 0x658f02c4);

		ROM_REGION(0x10000, REGION_GFX3, ROMREGION_DISPOSE);
		ROM_LOAD("2-l1.bin", 0x00000, 0x4000, 0x2528bec6); /* sprites */
		ROM_LOAD("2-l2.bin", 0x04000, 0x4000, 0xf89287aa);
		ROM_LOAD("2-n1.bin", 0x08000, 0x4000, 0x024418f8);
		ROM_LOAD("2-n2.bin", 0x0c000, 0x4000, 0xe2c7e489);

		ROM_REGION(0x0a00, REGION_PROMS, 0);
		ROM_LOAD("08e_sb-5.bin", 0x0000, 0x0100, 0x93ab8153); /* red component */
		ROM_LOAD("09e_sb-6.bin", 0x0100, 0x0100, 0x8ab44f7d);
		/*
		 * green component
		 */
		ROM_LOAD("10e_sb-7.bin", 0x0200, 0x0100, 0xf4ade9a4); /* blue component */
		ROM_LOAD("f01_sb-0.bin", 0x0300, 0x0100, 0x6047d91b);
		/*
		 * char lookup table
		 */
		ROM_LOAD("06d_sb-4.bin", 0x0400, 0x0100, 0x4858968d);
		/*
		 * tile lookup table
		 */
		ROM_LOAD("03k_sb-8.bin", 0x0500, 0x0100, 0xf6fad943);
		/*
		 * sprite lookup table
		 */
		ROM_LOAD("01d_sb-2.bin", 0x0600, 0x0100, 0x8bb8b3df);
		/* tile palette selector? (not used) */
		ROM_LOAD("02d_sb-3.bin", 0x0700, 0x0100, 0x3b0c99af);
		/* tile palette selector? (not used) */
		ROM_LOAD("k06_sb-1.bin", 0x0800, 0x0100, 0x712ac508);
		/*
		 * interrupt timing (not used)
		 */
		ROM_LOAD("01m_sb-9.bin", 0x0900, 0x0100, 0x4921635c);
		/*
		 * video timing? (not used)
		 */
		return true;
	}

	public Machine getMachine(URL url, String name) {
		super.getMachine(url, name);
		m = new jef.machine.BasicMachine();
		super.setVideoEmulator(v);

		if (name.equals("1942")) {
			GAME(1984, rom_1942(), 0, mdrv_1942(), ipt_1942(), 0, ROT270, "Capcom", "1942 (set 1)");
		} else if (name.equals("1942a")) {
			GAME(
				1984,
				rom_1942a(),
				"1942",
				mdrv_1942(),
				ipt_1942(),
				0,
				ROT270,
				"Capcom",
				"1942 (set 2)");
		} else if (name.equals("1942b")) {
			GAME(
				1984,
				rom_1942b(),
				"1942",
				mdrv_1942(),
				ipt_1942(),
				0,
				ROT270,
				"Capcom",
				"1942 (set 3)");
		}

		m.init(md);

		return (Machine) m;
	}

}
