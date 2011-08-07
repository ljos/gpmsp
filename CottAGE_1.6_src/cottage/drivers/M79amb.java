/*
CottAGE - the Arcade Generic Emulator in Java

Java driver by Gollum
*/

/* Ramtek M79 Ambush */

package cottage.drivers;

import java.net.URL;

import jef.machine.Machine;
import jef.map.InitHandler;
import jef.map.InterruptHandler;
import jef.map.ReadHandler;
import jef.map.WriteHandler;
import jef.video.GfxManager;
import jef.video.Vh_convert_color_proms;
import jef.video.Vh_refresh;
import cottage.mame.Driver;
import cottage.mame.MAMEConstants;
import cottage.mame.MAMEDriver;

public class M79amb extends MAMEDriver implements Driver, MAMEConstants {

/*
 * in
 * 8000 DIP SW
 * 8002 D0=VBlank
 * 8004
 * 8005
 *
 * out
 * 8000
 * 8001 Mask Sel
 * 8002
 * 8003 D0=SelfTest LED
 *
 */

cottage.vidhrdw.M79amb v = new cottage.vidhrdw.M79amb();
WriteHandler ramtek_videoram_w = v.ramtek_videoram_w();
WriteHandler ramtek_mask_w = v.ramtek_mask_w();
Vh_convert_color_proms m79amb_pi = (Vh_convert_color_proms)v;
Vh_refresh generic_bitmapped_vu = (Vh_refresh)v;
//Vh_start generic_bitmapped_vs = (Vh_start)v;

jef.machine.BasicMachine m = new jef.machine.BasicMachine();
InterruptHandler irq0_line_hold = m.irq0_line_hold();

/*
 * since these functions aren't used anywhere else, i've made them
 * static, and included them here
 */
static int ControllerTable[] = {
    0  , 1  , 3  , 2  , 6  , 7  , 5  , 4  ,
    12 , 13 , 15 , 14 , 10 , 11 , 9  , 8  ,
    24 , 25 , 27 , 26 , 30 , 31 , 29 , 28 ,
    20 , 21 , 23 , 22 , 18 , 19 , 17 , 16
};

public ReadHandler gray5bit_controller0_r() { return new Gray5bit_controller0_r(); }
public class Gray5bit_controller0_r implements ReadHandler {
	public int read(int address) {
		return (input_port_2_r.read(0) & 0xe0) | (~ControllerTable[input_port_2_r.read(0) & 0x1f] & 0x1f);
	}
}

public ReadHandler gray5bit_controller1_r() { return new Gray5bit_controller1_r(); }
public class Gray5bit_controller1_r implements ReadHandler {
	public int read(int address) {
		return (input_port_3_r.read(0) & 0xe0) | (~ControllerTable[input_port_3_r.read(0) & 0x1f] & 0x1f);
	}
}

private boolean readmem() {
	MR_START( 0x0000, 0x1fff, MRA_ROM );
	MR_ADD( 0x4000, 0x63ff, MRA_RAM );
	MR_ADD( 0x8000, 0x8000, input_port_0_r);
	MR_ADD( 0x8002, 0x8002, input_port_1_r);
	MR_ADD( 0x8004, 0x8004, gray5bit_controller0_r());
	MR_ADD( 0x8005, 0x8005, gray5bit_controller1_r());
	MR_ADD( 0xC000, 0xC07f, MRA_RAM);			/* ?? */
	MR_ADD( 0xC200, 0xC27f, MRA_RAM);			/* ?? */
	return true;
}

private boolean writemem() {
	MW_START( 0x0000, 0x1fff, MWA_ROM );
	MW_ADD( 0x4000, 0x43ff, MWA_RAM );
    	MW_ADD( 0x4400, 0x5fff, ramtek_videoram_w, videoram );
    	MW_ADD( 0x6000, 0x63ff, MWA_RAM );		/* ?? */
	MW_ADD( 0x8001, 0x8001, ramtek_mask_w);
	//MW_ADD( 0x8000, 0x8000, sound_w );
	//MW_ADD( 0x8002, 0x8003, sound_w );
	MW_ADD( 0xC000, 0xC07f, MWA_RAM);			/* ?? */
	MW_ADD( 0xC200, 0xC27f, MWA_RAM);			/* ?? */
	return true;
}


private boolean ipt_m79amb() {
	PORT_START();      /* 8000 */
	PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_UNKNOWN );	/* dip switch */
	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_UNUSED );
	PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_UNUSED );
	PORT_BIT( 0x08, IP_ACTIVE_LOW,  IPT_UNUSED );
	PORT_BIT( 0x10, IP_ACTIVE_HIGH, IPT_UNUSED );
	PORT_BIT( 0x20, IP_ACTIVE_HIGH, IPT_UNUSED );
	PORT_BIT( 0x40, IP_ACTIVE_HIGH, IPT_UNUSED );
	PORT_BIT( 0x80, IP_ACTIVE_HIGH, IPT_UNUSED );

	PORT_START();      /* 8002 */
//	PORT_BIT( 0x01, IP_ACTIVE_LOW,  IPT_VBLANK );
	PORT_BIT( 0x02, IP_ACTIVE_LOW,  IPT_START1 );
	PORT_BIT( 0x04, IP_ACTIVE_LOW,  IPT_COIN1  );
	PORT_BIT( 0x08, IP_ACTIVE_LOW,  IPT_TILT   );
	PORT_BIT( 0x10, IP_ACTIVE_LOW,  IPT_UNUSED );
	PORT_BIT( 0x20, IP_ACTIVE_LOW,  IPT_UNUSED );
	PORT_BIT( 0x40, IP_ACTIVE_LOW,  IPT_UNUSED );
	PORT_BIT( 0x80, IP_ACTIVE_LOW,  IPT_UNUSED );

	PORT_START();		/* 8004 */
	PORT_ANALOG( 0x1f, 0x10, IPT_PADDLE, 25, 10, 0, 0x1f);
	PORT_BIT( 0x20, IP_ACTIVE_LOW,  IPT_BUTTON1 );
	PORT_BIT( 0x40, IP_ACTIVE_LOW,  IPT_UNKNOWN );
	PORT_BIT( 0x80, IP_ACTIVE_LOW,  IPT_UNKNOWN );

	PORT_START();      /* 8005 */
	PORT_ANALOG( 0x1f, 0x10, IPT_PADDLE | IPF_PLAYER2, 25, 10, 0, 0x1f);
	PORT_BIT( 0x20, IP_ACTIVE_LOW,  IPT_BUTTON2 );
	PORT_BIT( 0x40, IP_ACTIVE_LOW,  IPT_UNKNOWN );
	PORT_BIT( 0x80, IP_ACTIVE_LOW,  IPT_UNKNOWN );
	return true;
}

public InitHandler init_m79amb() { return new Init_m79amb(); }
public class Init_m79amb implements InitHandler {
	public void init() {
		int[] rom = memory_region(REGION_CPU1);
		int i;

		/* PROM data is active low */
	 	for (i = 0;i < 0x2000;i++)
			rom[i] = (~rom[i])&0xFF;	/* JAVA FIXES */
	}
}

public boolean mdrv_m79amb() {

	/* basic machine hardware */
	MDRV_CPU_ADD(I8080, 1996800);
	MDRV_CPU_MEMORY(readmem(),writemem());
	MDRV_CPU_VBLANK_INT(irq0_line_hold,1);

	MDRV_FRAMES_PER_SECOND(60);
	MDRV_VBLANK_DURATION(DEFAULT_60HZ_VBLANK_DURATION);
//	MDRV_VBLANK_DURATION(DEFAULT_REAL_60HZ_VBLANK_DURATION);

	/* video hardware */
	MDRV_VIDEO_ATTRIBUTES(VIDEO_TYPE_RASTER|GfxManager.VIDEO_SUPPORTS_DIRTY);
	MDRV_SCREEN_SIZE(32*8, 28*8);
	MDRV_VISIBLE_AREA(0*8, 32*8-1, 0*8, 28*8-1);
	MDRV_PALETTE_LENGTH(7);

	MDRV_PALETTE_INIT(m79amb_pi);
//	MDRV_VIDEO_START(generic_bitmapped_vs);
	MDRV_VIDEO_UPDATE(generic_bitmapped_vu);

	/* sound hardware */
	return true;
}



private boolean rom_m79amb() {
	ROM_REGION( 0x10000, REGION_CPU1, 0 );     /* 64k for code */
	ROM_LOAD( "m79.10t",      0x0000, 0x0200, 0xccf30b1e );
	ROM_LOAD( "m79.9t",       0x0200, 0x0200, 0xdaf807dd );
	ROM_LOAD( "m79.8t",       0x0400, 0x0200, 0x79fafa02 );
	ROM_LOAD( "m79.7t",       0x0600, 0x0200, 0x06f511f8 );
	ROM_LOAD( "m79.6t",       0x0800, 0x0200, 0x24634390 );
	ROM_LOAD( "m79.5t",       0x0a00, 0x0200, 0x95252aa6 );
	ROM_LOAD( "m79.4t",       0x0c00, 0x0200, 0x54cffb0f );
	ROM_LOAD( "m79.3ta",      0x0e00, 0x0200, 0x27db5ede );
	ROM_LOAD( "m79.10u",      0x1000, 0x0200, 0xe41d13d2 );
	ROM_LOAD( "m79.9u",       0x1200, 0x0200, 0xe35f5616 );
	ROM_LOAD( "m79.8u",       0x1400, 0x0200, 0x14eafd7c );
	ROM_LOAD( "m79.7u",       0x1600, 0x0200, 0xb9864f25 );
	ROM_LOAD( "m79.6u",       0x1800, 0x0200, 0xdd25197f );
	ROM_LOAD( "m79.5u",       0x1a00, 0x0200, 0x251545e2 );
	ROM_LOAD( "m79.4u",       0x1c00, 0x0200, 0xb5f55c75 );
	ROM_LOAD( "m79.3u",       0x1e00, 0x0200, 0xe968691a );
	return true;
}


	public Machine getMachine(URL url, String name) {
		super.getMachine(url,name);
		super.setVideoEmulator(v);
		m = new jef.machine.BasicMachine();

		if (name.equals("m79amb")) {
			GAME( 1977, rom_m79amb(), 0, mdrv_m79amb(), ipt_m79amb(), init_m79amb(), ROT0, "Ramtek", "M79 Ambush" );
			init_m79amb();
		}

		m.init(md);
		return (Machine)m;
	}

}


