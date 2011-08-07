/*
CottAGE - the Arcade Generic Emulator in Java

Java driver by Gollum
*/

/***************************************************************************

Dottori Kun (Head On's mini game)
(c)1990 SEGA

Driver by Takahiro Nogi (nogi@kt.rim.or.jp) 1999/12/15 -


CPU   : Z-80 (4MHz)
SOUND : (none)

14479.MPR  ; PRG (FIRST VER)
14479A.MPR ; PRG (NEW VER)

* This game is only for the test of cabinet
* BackRaster = WHITE on the FIRST version.
* BackRaster = BLACK on the NEW version.
* On the NEW version, push COIN-SW as TEST MODE.
* 0000-3FFF:ROM 8000-85FF:VRAM(128x96) 8600-87FF:WORK-RAM

***************************************************************************/

package cottage.drivers;

import java.net.URL;

import jef.machine.Machine;
import jef.map.InterruptHandler;
import jef.map.WriteHandler;
import jef.video.GfxManager;
import jef.video.Vh_refresh;
import jef.video.Vh_start;

import cottage.mame.Driver;
import cottage.mame.MAMEConstants;
import cottage.mame.MAMEDriver;

public class Dotrikun extends MAMEDriver implements Driver, MAMEConstants {

	cottage.vidhrdw.Dotrikun v = new cottage.vidhrdw.Dotrikun();
	WriteHandler dotrikun_videoram_w = v.dotrikun_videoram_w();
	Vh_start generic_vs = (Vh_start)v;
	Vh_refresh dotrikun_vu = (Vh_refresh)v;
	WriteHandler dotrikun_color_w = v.dotrikun_color_w();

	jef.machine.BasicMachine m = new jef.machine.BasicMachine();
	InterruptHandler irq0_line_hold = m.irq0_line_hold();


	private boolean readmem() {
		MR_START( 0x0000, 0x3fff, MRA_ROM );
		MR_ADD( 0x8000, 0x87ff, MRA_RAM );
		return true;
	}

	private boolean writemem() {
		MW_START( 0x0000, 0x3fff, MWA_ROM );
		MW_ADD( 0x8000, 0x87ff, dotrikun_videoram_w, videoram, videoram_size );
		return true;
	}

	private boolean readport() {
		PR_START( 0x00, 0x00, input_port_0_r );
		return true;
	}

	private boolean writeport() {
		PW_START( 0x00, 0x00, dotrikun_color_w );
		return true;
	}


	private boolean ipt_dotrikun() {
		PORT_START();
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON2 );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_START1 );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_COIN1 );
		return true;
	}


	public boolean mdrv_dotrikun() {

		/* basic machine hardware */
		MDRV_CPU_ADD(Z80, 4000000);		 /* 4 MHz */
		MDRV_CPU_MEMORY(readmem(),writemem());
		MDRV_CPU_PORTS(readport(),writeport());
		MDRV_CPU_VBLANK_INT(irq0_line_hold,1);

		MDRV_FRAMES_PER_SECOND(60);
		MDRV_VBLANK_DURATION(DEFAULT_60HZ_VBLANK_DURATION);

		/* video hardware */
		MDRV_VIDEO_ATTRIBUTES(VIDEO_TYPE_RASTER | GfxManager.VIDEO_SUPPORTS_DIRTY);
		MDRV_SCREEN_SIZE(256, 256);
		MDRV_VISIBLE_AREA(0, 256-1, 0, 192-1);
		MDRV_PALETTE_LENGTH(2);

		MDRV_VIDEO_START(generic_vs);
		MDRV_VIDEO_UPDATE(dotrikun_vu);

		/* sound hardware */
		return true;
	}


/***************************************************************************

  Game driver(s)

***************************************************************************/

	private boolean rom_dotrikun() {
		ROM_REGION( 0x10000, REGION_CPU1, 0 );	/* 64k for code */
		ROM_LOAD( "14479a.mpr",	0x0000, 0x4000, 0xb77a50db );
		return true;
	}

	private boolean rom_dotriku2() {
		ROM_REGION( 0x10000, REGION_CPU1, 0 );	/* 64k for code */
		ROM_LOAD( "14479.mpr",	0x0000, 0x4000, 0xa6aa7fa5 );
		return true;
	}


	public Machine getMachine(URL url, String name) {
		super.getMachine(url,name);
		super.setVideoEmulator(v);

		if (name.equals("dotrikun")) {
			GAME( 1990, rom_dotrikun(),          0, mdrv_dotrikun(), ipt_dotrikun(), 0, ROT0, "Sega", "Dottori Kun (new version)" );
		} else if (name.equals("dotriku2")) {
			GAME( 1990, rom_dotriku2(), "dotrikun", mdrv_dotrikun(), ipt_dotrikun(), 0, ROT0, "Sega", "Dottori Kun (old version)" );
		}

		m.init(md);
		return (Machine)m;
	}

}