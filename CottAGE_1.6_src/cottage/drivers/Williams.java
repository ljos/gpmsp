/*
 * Created on 22-jun-2005
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package cottage.drivers;

import jef.map.MemoryReadAddress;
import jef.map.MemoryWriteAddress;
import jef.map.WriteHandler;
import cottage.mame.Driver;
import cottage.mame.MAMEConstants;
import cottage.mame.MAMEDriver;

/**
 * @author Erik Duijs
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class Williams extends MAMEDriver implements Driver, MAMEConstants {

	int[] REGION_CPU1 = new int[0x14000];
	
	WriteHandler williams_videoram_w, defender_bank_select_w;
	
	/*************************************
	 *
	 *	Defender memory handlers
	 *
	 *************************************/
	private MemoryReadAddress defender_readmem() {
		MemoryReadAddress mra = new MemoryReadAddress(REGION_CPU1);
		mra.setMR( 0x0000, 0x97ff, MRA_BANK1 );
		mra.setMR( 0x9800, 0xbfff, MRA_RAM );
		mra.setMR( 0xc000, 0xcfff, MRA_BANK2 );
		mra.setMR( 0xd000, 0xffff, MRA_ROM );
		return mra;
	}


	private MemoryWriteAddress defender_writemem() {
		MemoryWriteAddress mwa = new MemoryWriteAddress(REGION_CPU1);
		mwa.set( 0x0000, 0x97ff, williams_videoram_w );
		mwa.setMW( 0x9800, 0xbfff, MWA_RAM );
		mwa.setMW( 0xc000, 0xcfff, MWA_BANK2 );
		mwa.setMW( 0xc000, 0xc00f, MWA_RAM );
		mwa.set( 0xd000, 0xdfff, defender_bank_select_w );
		mwa.setMW( 0xe000, 0xffff, MWA_ROM );

		return mwa;
	}
	
	/*************************************
	 *
	 *	Port definitions
	 *
	 *************************************/

	/*INPUT_PORTS_START( defender )
	PORT_START      // IN0
	PORT_BITX(0x01, IP_ACTIVE_HIGH, IPT_BUTTON1, "Fire", IP_KEY_DEFAULT, IP_JOY_DEFAULT )
	PORT_BITX(0x02, IP_ACTIVE_HIGH, IPT_BUTTON2, "Thrust", IP_KEY_DEFAULT, IP_JOY_DEFAULT )
	PORT_BITX(0x04, IP_ACTIVE_HIGH, IPT_BUTTON3, "Smart Bomb", IP_KEY_DEFAULT, IP_JOY_DEFAULT )
	PORT_BITX(0x08, IP_ACTIVE_HIGH, IPT_BUTTON4, "Hyperspace", IP_KEY_DEFAULT, IP_JOY_DEFAULT )
	PORT_BIT( 0x10, IP_ACTIVE_HIGH, IPT_START2 )
	PORT_BIT( 0x20, IP_ACTIVE_HIGH, IPT_START1 )
	PORT_BITX(0x40, IP_ACTIVE_HIGH, IPT_BUTTON6, "Reverse", IP_KEY_DEFAULT, IP_JOY_DEFAULT )
	PORT_BIT( 0x80, IP_ACTIVE_HIGH, IPT_JOYSTICK_DOWN | IPF_8WAY )

	PORT_START      // IN1
	PORT_BIT( 0x01, IP_ACTIVE_HIGH, IPT_JOYSTICK_UP | IPF_8WAY )
	PORT_BIT( 0xfe, IP_ACTIVE_HIGH, IPT_UNKNOWN )

	PORT_START      // IN2
	PORT_BITX(0x01, IP_ACTIVE_HIGH, 0, "Auto Up", KEYCODE_F1, IP_JOY_NONE )
	PORT_BITX(0x02, IP_ACTIVE_HIGH, 0, "Advance", KEYCODE_F2, IP_JOY_NONE )
	PORT_BIT( 0x04, IP_ACTIVE_HIGH, IPT_COIN3 )
	PORT_BITX(0x08, IP_ACTIVE_HIGH, 0, "High Score Reset", KEYCODE_7, IP_JOY_NONE )
	PORT_BIT( 0x10, IP_ACTIVE_HIGH, IPT_COIN1 )
	PORT_BIT( 0x20, IP_ACTIVE_HIGH, IPT_COIN2 )
	PORT_BIT( 0x40, IP_ACTIVE_HIGH, IPT_TILT )

	PORT_START      // IN3 - fake port for better joystick control 
	// This fake port is handled via defender_input_port_1 
	PORT_BIT( 0x02, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_CHEAT )
	PORT_BIT( 0x40, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT | IPF_8WAY | IPF_CHEAT )
	INPUT_PORTS_END
	
	/*************************************
	 *
	 *	ROM definitions
	 *
	 *************************************/

	/*ROM_START( defender )
	ROM_REGION( 0x14000, REGION_CPU1 )
	ROM_LOAD( "defend.1",     0x0d000, 0x0800, 0xc3e52d7e )
	ROM_LOAD( "defend.4",     0x0d800, 0x0800, 0x9a72348b )
	ROM_LOAD( "defend.2",     0x0e000, 0x1000, 0x89b75984 )
	ROM_LOAD( "defend.3",     0x0f000, 0x1000, 0x94f51e9b )
	// bank 0 is the place for CMOS ram 
	ROM_LOAD( "defend.9",     0x10000, 0x0800, 0x6870e8a5 )
	ROM_LOAD( "defend.12",    0x10800, 0x0800, 0xf1f88938 )
	ROM_LOAD( "defend.8",     0x11000, 0x0800, 0xb649e306 )
	ROM_LOAD( "defend.11",    0x11800, 0x0800, 0x9deaf6d9 )
	ROM_LOAD( "defend.7",     0x12000, 0x0800, 0x339e092e )
	ROM_LOAD( "defend.10",    0x12800, 0x0800, 0xa543b167 )
	ROM_RELOAD(               0x13800, 0x0800 )
	ROM_LOAD( "defend.6",     0x13000, 0x0800, 0x65f4efd1 )

	//ROM_REGION( 0x10000, REGION_CPU2 )     /* 64k for the sound CPU */
	//ROM_LOAD( "defend.snd",   0xf800, 0x0800, 0xfefd5b48 )
	//ROM_END

	
	/*************************************
	 *
	 *	Machine driver
	 *
	 *************************************/

	/*static struct MachineDriver machine_driver_defender =
		{
			// basic machine hardware 
			{
				{
					CPU_M6809,
					1000000,
					defender_readmem,defender_writemem,0,0,
					ignore_interrupt,1
				},
				{
					CPU_M6808 | CPU_AUDIO_CPU,
					3579000/4,
					sound_readmem,sound_writemem,0,0,
					ignore_interrupt,1
				}
			},
			60, DEFAULT_60HZ_VBLANK_DURATION,
			1,
			defender_init_machine,

			// video hardware 
			304, 256,
			{ 6, 298-1, 7, 247-1 },
			0,
			16,16,
			0,

			VIDEO_TYPE_RASTER | VIDEO_MODIFIES_PALETTE | VIDEO_SUPPORTS_DIRTY,
			0,
			williams_vh_start,
			williams_vh_stop,
			williams_vh_screenrefresh,

			// sound hardware 
			0,0,0,0,
			{
				{
					SOUND_DAC,
					&dac_interface
				}
			},

			nvram_handler
	};*/
	/*************************************
	 *
	 *	Driver initialization
	 *
	 *************************************/

	/*static void init_defender(void)
	{
		static const UINT32 bank[8] = { 0x0c000, 0x10000, 0x11000, 0x12000, 0x0c000, 0x0c000, 0x0c000, 0x13000 };
		defender_bank_list = bank;

		// CMOS configuration 
		CONFIGURE_CMOS(0xc400, 0x100);

		// PIA configuration 
		CONFIGURE_PIAS(defender_pia_0_intf, williams_pia_1_intf, williams_snd_pia_intf);
	}
	
	GAME( 1980, defender, 0,        defender, defender, defender, ROT0,   "Williams", "Defender (Red label)" )
	*/
}
