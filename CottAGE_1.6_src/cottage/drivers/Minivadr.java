/*
CottAGE - the Arcade Generic Emulator in Java

Java driver by Gollum
*/

/***************************************************************************

Minivader (Space Invaders's mini game)
(c)1990 Taito Corporation

Driver by Takahiro Nogi (nogi@kt.rim.or.jp) 1999/12/19 -

This is a test board sold together with the cabinet (as required by law in
Japan). It has no sound.

***************************************************************************/

package cottage.drivers;

import java.net.URL;

import jef.machine.Machine;
import jef.map.InterruptHandler;
import jef.map.WriteHandler;
import jef.video.GfxManager;
import jef.video.Vh_convert_color_proms;
import jef.video.Vh_refresh;
import jef.video.Vh_start;

import cottage.mame.Driver;
import cottage.mame.MAMEConstants;
import cottage.mame.MAMEDriver;

public class Minivadr extends MAMEDriver implements Driver, MAMEConstants {

	cottage.vidhrdw.Minivadr v = new cottage.vidhrdw.Minivadr();
	WriteHandler minivadr_videoram_w = v.minivadr_videoram_w();
	Vh_refresh minivadr_vu = (Vh_refresh)v;
	Vh_convert_color_proms minivadr_pi = (Vh_convert_color_proms)v;
	Vh_start generic_vs = (Vh_start)v;

	jef.machine.BasicMachine m = new jef.machine.BasicMachine();
	InterruptHandler irq0_line_hold = m.irq0_line_hold();

	private boolean readmem() {
		MR_START( 0x0000, 0x1fff, MRA_ROM );
		MR_ADD( 0xa000, 0xbfff, MRA_RAM );
		MR_ADD( 0xe008, 0xe008, input_port_0_r );
		return true;
	}

	private boolean writemem() {
		MW_START( 0x0000, 0x1fff, MWA_ROM );
		MW_ADD( 0xa000, 0xbfff, minivadr_videoram_w, videoram, videoram_size );
		MW_ADD( 0xe008, 0xe008, MWA_NOP );		// ???
		return true;
	}


	private boolean ipt_minivadr() {
		PORT_START();
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_BUTTON1 );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_COIN1 );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
		return true;
	}


	public boolean mdrv_minivadr() {

		/* basic machine hardware */
		MDRV_CPU_ADD(Z80,24000000 / 6);		 /* 4 MHz ? */
		MDRV_CPU_MEMORY(readmem(),writemem());
		MDRV_CPU_VBLANK_INT(irq0_line_hold,1);

		MDRV_FRAMES_PER_SECOND(60);
		MDRV_VBLANK_DURATION(DEFAULT_60HZ_VBLANK_DURATION);

		/* video hardware */
		MDRV_VIDEO_ATTRIBUTES(VIDEO_TYPE_RASTER | GfxManager.VIDEO_SUPPORTS_DIRTY);
		MDRV_SCREEN_SIZE(256, 256);
		MDRV_VISIBLE_AREA(0, 256-1, 16, 240-1);
		MDRV_PALETTE_LENGTH(2);

		MDRV_PALETTE_INIT(minivadr_pi);
		MDRV_VIDEO_START(generic_vs);
		MDRV_VIDEO_UPDATE(minivadr_vu);

		/* sound hardware */
		return true;
	}


/***************************************************************************

  Game driver(s)

***************************************************************************/

	private boolean rom_minivadr() {
		ROM_REGION( 0x10000, REGION_CPU1, 0 );	/* 64k for code */
		ROM_LOAD( "d26-01.bin",	0x0000, 0x2000, 0xa96c823d );
		return true;
	}


	public Machine getMachine(URL url, String name) {
		super.getMachine(url,name);
		super.setVideoEmulator(v);

		if (name.equals("minivadr")) {
			GAME( 1990, rom_minivadr(), 0, mdrv_minivadr(), ipt_minivadr(), 0, ROT0, "Taito Corporation", "Minivader" );
		}

		m.init(md);
		return (Machine)m;
	}

}