/*
 * CottAGE - the Arcade Generic Emulator in Java
 * 
 * Java driver by Romain Tisserand
 */

/*******************************************************************************
 * 
 * "Mitchell hardware". Actually used mostly by Capcom.
 * 
 * All games run on the same hardware except mgakuen, which runs on an earlier
 * version, without RAM banking, not encrypted (standard Z80) and without
 * EEPROM.
 * 
 * Other games that might run on this hardware: "Chi-toitsu"(YUGA 1988)-Another
 * version of"Mahjong Gakuen" "MIRAGE -Youjyu mahjong den-"(MITCHELL 1994)
 * 
 * Notes: - Super Pang has a protection which involves copying code stored in
 * the EEPROM to RAM and execute it from there. The first time the game is run,
 * you have to keep the player 1 start button pressed until the title screen
 * appears. This forces the game to initialize the EEPROM, otherwise it will
 * not work. This is simulated with a kluge in input_r.
 * 
 * TODO: - understand what bits 0 and 3 of input port 0x05 are - ball speed is
 * erratic in Block Block. It was not like this at one point. This is probably
 * related to interrupts and maybe to the above bits.
 *  
 ******************************************************************************/

package cottage.drivers;

import java.net.URL;

import jef.machine.Machine;
import jef.map.InitHandler;
import jef.map.InterruptHandler;
import jef.map.ReadHandler;
import jef.map.WriteHandler;
import jef.video.Vh_refresh;
import jef.video.Vh_start;
import cottage.mame.Driver;
import cottage.mame.MAMEConstants;
import cottage.mame.MAMEDriver;

public class Mitchell extends MAMEDriver implements Driver, MAMEConstants {

	cottage.vidhrdw.Mitchell v = new cottage.vidhrdw.Mitchell();
	Vh_start generic_vs = (Vh_start) v;
	Vh_refresh vh_pang = (Vh_refresh) v;
	cottage.machine.Kabuki m = new cottage.machine.Kabuki(this);
	InterruptHandler irq0_line_hold = m.irq0_line_hold();

	ReadHandler pang_colorram_r = v.pang_colorram_r();
	ReadHandler pang_paletteram_r = v.pang_paletteram_r();
	ReadHandler pang_videoram_r = v.pang_videoram_r();
	WriteHandler pang_colorram_w = v.pang_colorram_w();
	WriteHandler pang_paletteram_w = v.pang_paletteram_w();
	WriteHandler pang_videoram_w = v.pang_videoram_w();

	ReadHandler mgakuen_paletteram_r = v.mgakuen_paletteram_r();
	ReadHandler mgakuen_videoram_r = v.mgakuen_videoram_r();
	ReadHandler mgakuen_objram_r = v.mgakuen_objram_r();
	WriteHandler mgakuen_paletteram_w = v.mgakuen_paletteram_w();
	WriteHandler mgakuen_videoram_w = v.mgakuen_videoram_w();
	WriteHandler mgakuen_objram_w = v.mgakuen_objram_w();

	/*
	 * WriteHandler mgakuen_paletteram_w; WriteHandler mgakuen_videoram_w;
	 * WriteHandler mgakuen_objram_w; WriteHandler pang_video_bank_w;
	 * WriteHandler pang_videoram_w; WriteHandler pang_colorram_w; WriteHandler
	 * pang_gfxctrl_w; WriteHandler pang_paletteram_w;
	 * 
	 * ReadHandler mgakuen_paletteram_r; ReadHandler mgakuen_videoram_r;
	 * ReadHandler mgakuen_objram_r; ReadHandler pang_videoram_r; ReadHandler
	 * pang_colorram_r;
	 */


	int pang_videoram_size;

	public WriteHandler pang_bankswitch_w() {
		return new Pang_bankswitch_w();
	}

	class Pang_bankswitch_w implements WriteHandler {
		public void write(int address, int data) {
			int bankaddress;
			int[] RAM = memory_region(REGION_CPU1);

			bankaddress = 0x10000 + (data & 0x0f) * 0x4000;
			cpu_setbank(1,bankaddress);
		}
	}

	/***************************************************************************
	 * EEPROM
	 **************************************************************************/
	static int[] nvram;
	static int nvram_size;
	static int init_eeprom_count;

	/*
	 * static struct EEPROM_interface eeprom_interface = { 6, // address bits
	 * 16, // data bits "0110" // read command "0101", // write command "0111" //
	 * erase command };
	 * 
	 * 
	 * static NVRAM_HANDLER( mitchell ) { if (read_or_write) {
	 * EEPROM_save(file); // EEPROM if (nvram_size) // Super Pang, Block Block
	 * osd_fwrite(file,nvram,nvram_size); // NVRAM } else {
	 * EEPROM_init(&eeprom_interface);
	 * 
	 * if (file) { init_eeprom_count = 0; EEPROM_load(file); // EEPROM if
	 * (nvram_size) // Super Pang, Block Block
	 * osd_fread(file,nvram,nvram_size); // NVRAM } else init_eeprom_count =
	 * 1000; // for Super Pang }
	 */

	ReadHandler pang_port5_r = (ReadHandler) new Pang_port5_r();
	class Pang_port5_r implements ReadHandler {
		public int read(int address) {
			int bit = 0;
			/*
			 * extern const struct GameDriver driver_mgakuen2; bit =
			 * EEPROM_read_bit() << 7; // bits 0 and (sometimes) 3 are checked
			 * in the interrupt handler. // Maybe they are vblank related, but
			 * I'm not sure. // bit 3 is checked before updating the palette so
			 * it really seems to be vblank. // Many games require two
			 * interrupts per frame and for these bits to toggle, // otherwise
			 * music doesn't work. if (cpu_getiloops() & 1) bit |= 0x01; else
			 * bit |= 0x08; if (m.gamedrv == driver_mgakuen2) // hack... music
			 * doesn't work otherwise
			 */
			//            return (input_port_0_r(0) & 0x76) | bit;
			return 0;
		}
	}

	/*
	 * static WRITE_HANDLER( eeprom_cs_w ) { EEPROM_set_cs_line(data ?
	 * CLEAR_LINE : ASSERT_LINE); }
	 * 
	 * static WRITE_HANDLER( eeprom_clock_w ) { EEPROM_set_clock_line(data ?
	 * CLEAR_LINE : ASSERT_LINE); }
	 * 
	 * static WRITE_HANDLER( eeprom_serial_w ) { EEPROM_write_bit(data);
	 */

	/***************************************************************************
	 * 
	 * ControlsConfig handling
	 *  
	 **************************************************************************/

	static int dial[] = new int[2];
	static int dial_selected;
	static int dir[] = new int[2];

	ReadHandler block_input_r = (ReadHandler) new Block_input_r();
	class Block_input_r implements ReadHandler {
		public int read(int address) {

			if ((dial_selected) != 0) {
				int delta;
				delta = (m.readinputport(4 + address) - dial[address]) & 0xff;
				if ((delta & 0x80) != 0) {
					delta = (-delta) & 0xff;
					if ((dir[address]) != 0) {
						// don't report movement on a direction change,
						// otherwise it will stutter
						dir[address] = 0;
						delta = 0;
					}
				} else if (delta > 0) {
					if (dir[address] == 0) {
						// don't report movement on a direction change,
						// otherwise it will stutter
						dir[address] = 1;
						delta = 0;
					}
				}
				if (delta > 0x3f)
					delta = 0x3f;
				return delta << 2;
			} else {
				int res;
				res = m.readinputport(2 + address) & 0xf7;
				if ((dir[address]) != 0)
					res |= 0x08;
				return res;
			}
		}
	}

	static int input_type;

	ReadHandler input_r = (ReadHandler) new Input_r();
	class Input_r implements ReadHandler {
		public int read(int offset) {
			switch (input_type) {
				case 1 : // Mahjong games
					if ((offset) != 0)
						return mahjong_input_r.read(offset - 1);
					else
						return m.readinputport(1);
				case 2 : // Block Block - dial control
					if ((offset) != 0)
						return block_input_r.read(offset - 1);
					else
						return m.readinputport(1);
				case 3 : // Super Pang - simulate START 1 press to initialize
					// EEPROM
					if (offset != 0 || init_eeprom_count == 0)
						return m.readinputport(1 + offset);
					else {
						init_eeprom_count--;
						return m.readinputport(1) & ~0x08;
					}
				case 0 :
				default :
					return m.readinputport(1 + offset);
			}
		}
	}

	WriteHandler block_dial_control_w = (WriteHandler) new Block_dial_control_w();
	class Block_dial_control_w implements WriteHandler {
		public void write(int address, int data) {
			if (data == 0x08) {
				// reset the dial counters
				dial[0] = m.readinputport(4);
				dial[1] = m.readinputport(5);
			} else if (data == 0x80)
				dial_selected = 0;
			else
				dial_selected = 1;
		}
	}

	static int keymatrix;

	ReadHandler mahjong_input_r = (ReadHandler) new Mahjong_input_r();
	class Mahjong_input_r implements ReadHandler {
		public int read(int offset) {
			int i;

			for (i = 0; i < 5; i++)
				if ((keymatrix & (0x80 >> i)) != 0)
					return m.readinputport(2 + 5 * offset + i);

			return 0xff;
		}
	}

	WriteHandler mahjong_input_select_w = (WriteHandler) new Mahjong_input_select_w();
	class Mahjong_input_select_w implements WriteHandler {
		public void write(int address, int data) {
			keymatrix = data;
		}
	}

	WriteHandler input_w = (WriteHandler) new Input_w();
	class Input_w implements WriteHandler {
		public void write(int offset, int data) {
			switch (input_type) {
				case 1 :
					mahjong_input_select_w.write(offset, data);
					break;
				case 2 :
					block_dial_control_w.write(offset, data);
					break;
				case 0 :
				default :
					break;
			}
		}
	}

	/***************************************************************************
	 * 
	 * Memory handlers
	 *  
	 **************************************************************************/

	private boolean mgakuen_readmem() {
		MR_START(0x0000, 0x7fff, MRA_ROM);
		MR_ADD(0x8000, 0xbfff, MRA_BANK1);
		MR_ADD(0xc000, 0xc7ff, mgakuen_paletteram_r); /* palette RAM */
		MR_ADD(0xc800, 0xcfff, pang_colorram_r); /* Attribute RAM */
		MR_ADD(0xd000, 0xdfff, mgakuen_videoram_r); /* char RAM */
		MR_ADD(0xe000, 0xefff, MRA_RAM); /* Work RAM */
		MR_ADD(0xf000, 0xffff, mgakuen_objram_r); /* OBJ RAM */
		return true;
	}

	private boolean mgakuen_writemem() {
		MW_START(0x0000, 0xbfff, MWA_ROM);
		MW_ADD(0xc000, 0xc7ff, mgakuen_paletteram_w);
		MW_ADD(0xc800, 0xcfff, pang_colorram_w);
		MW_ADD(0xd000, 0xdfff, mgakuen_videoram_w); //,
		// pang_videoram_size);
		MW_ADD(0xe000, 0xefff, MWA_RAM /* ROM */
		);
		MW_ADD(0xf000, 0xffff, mgakuen_objram_w); /* OBJ RAM */
		return true;
	}

	private boolean readmem() {
		MR_START(0x0000, 0x7fff, MRA_ROM);
		MR_ADD(0x8000, 0xbfff, MRA_BANK1);
		MR_ADD(0xc000, 0xc7ff, pang_paletteram_r); /* Banked palette RAM */
		MR_ADD(0xc800, 0xcfff, pang_colorram_r); /* Attribute RAM */
		MR_ADD(0xd000, 0xdfff, pang_videoram_r); /* Banked char / OBJ RAM */
		MR_ADD(0xe000, 0xffff, MRA_RAM); /* Work RAM */
		return true;
	}

	private boolean writemem() {
		MW_START(0x0000, 0xbfff, MWA_ROM);
		MW_ADD(0xc000, 0xc7ff, pang_paletteram_w);
		MW_ADD(0xc800, 0xcfff, pang_colorram_w);
		MW_ADD(0xd000, 0xdfff, pang_videoram_w); //,
		// pang_videoram_size);
		MW_ADD(0xe000, 0xffff, MWA_RAM /* ROM */
		);
		return true;
	}

	private boolean readport() {
		PR_START(0x00, 0x02, input_r);
		/*
		 * Super Pang needs a kludge to initialize EEPROM.
		 */
		//         PR_ADD( 0x03, 0x03, input_port_12_r() ); /* mgakuen only */
		//         PR_ADD(0x04, 0x04, input_port_13_r() ); /* mgakuen only */
		PR_ADD(0x05, 0x05, pang_port5_r);
		return true;
	}

	private boolean writeport() {
		PW_START(0x00, 0x00, v.pang_gfxctrl_w());
		/* Palette bank, layer enable, coin counters, more */
		PW_ADD(0x01, 0x01, input_w);
		PW_ADD(0x02, 0x02, pang_bankswitch_w()); /* Code bank register */
		//         PW_ADD(0x03, 0x03, YM2413_data_port_0_w );
		//         PW_ADD(0x04, 0x04, YM2413_register_port_0_w );
		//         PW_ADD(0x05, 0x05, OKIM6295_data_0_w );
		//         PW_ADD(0x06, 0x06, MWA_NOP() ); /* watchdog? irq ack? */
		PW_ADD(0x07, 0x07, v.pang_video_bank_w()); /* Video RAM bank register */
		//         PW_ADD(0x08, 0x08, eeprom_cs_w() );
		//       PW_ADD(0x10, 0x10, eeprom_clock_w() );
		//         PW_ADD(0x18, 0x18, eeprom_serial_w() );
		return true;
	}

	private boolean ipt_mgakuen() {
		PORT_START(); /* DSW */
		PORT_BIT(0x01, IP_ACTIVE_LOW, IPT_UNKNOWN);
		/*
		 * USED - handled in port5_r
		 */
		PORT_BIT(0x02, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x04, IP_ACTIVE_LOW, IPT_UNKNOWN); /* unused? */
		PORT_BIT(0x08, IP_ACTIVE_LOW, IPT_UNKNOWN);
		/*
		 * USED - handled in port5_r
		 */
		PORT_BIT(0x70, IP_ACTIVE_LOW, IPT_UNKNOWN); /* unused? */
		PORT_BIT(0x80, IP_ACTIVE_LOW, IPT_UNKNOWN); /* data from EEPROM */

		PORT_START(); /* IN0 */
		PORT_BIT(0x01, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x02, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x04, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x08, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x10, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x20, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x40, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x80, IP_ACTIVE_LOW, IPT_COIN1);

		PORT_START(); /* IN1 */
		PORT_BIT(0x01, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x02, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x04, IP_ACTIVE_LOW, IPT_START1);
		//         PORT_BITX(0x08, IP_ACTIVE_LOW, 0, "P1 Kan",KEYCODE_LCONTROL,
		// IP_JOY_NONE );
		//         PORT_BITX(0x10, IP_ACTIVE_LOW, 0, "P1 M", KEYCODE_M, IP_JOY_NONE );
		//         PORT_BITX(0x20, IP_ACTIVE_LOW, 0, "P1 I", KEYCODE_I, IP_JOY_NONE );
		//         PORT_BITX(0x40, IP_ACTIVE_LOW, 0, "P1 E", KEYCODE_E, IP_JOY_NONE );
		//         PORT_BITX(0x80, IP_ACTIVE_LOW, 0, "P1 A", KEYCODE_A, IP_JOY_NONE );

		PORT_START(); /* IN1 */
		PORT_BIT(0x01, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x02, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x04, IP_ACTIVE_LOW, IPT_UNKNOWN);
		//         PORT_BITX(0x08, IP_ACTIVE_LOW, 0, "P1 Reach", KEYCODE_LSHIFT,
		// IP_JOY_NONE );
		//         PORT_BITX(0x10, IP_ACTIVE_LOW, 0, "P1 N", KEYCODE_N, IP_JOY_NONE );
		//         PORT_BITX(0x20, IP_ACTIVE_LOW, 0, "P1 J", KEYCODE_J, IP_JOY_NONE );
		//         PORT_BITX(0x40, IP_ACTIVE_LOW, 0, "P1 F", KEYCODE_F, IP_JOY_NONE );
		//         PORT_BITX(0x80, IP_ACTIVE_LOW, 0, "P1 B", KEYCODE_B, IP_JOY_NONE );

		PORT_START(); /* IN1 */
		PORT_BIT(0x01, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x02, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x04, IP_ACTIVE_LOW, IPT_UNKNOWN);
		//         PORT_BITX(0x08, IP_ACTIVE_LOW, 0, "P1 Ron", KEYCODE_Z, IP_JOY_NONE
		// );
		//         PORT_BITX(0x10, IP_ACTIVE_LOW, 0, "P1 Chi", KEYCODE_SPACE,
		// IP_JOY_NONE );
		//         PORT_BITX(0x20, IP_ACTIVE_LOW, 0, "P1 K", KEYCODE_K, IP_JOY_NONE );
		//         PORT_BITX(0x40, IP_ACTIVE_LOW, 0, "P1 G", KEYCODE_G, IP_JOY_NONE );
		//         PORT_BITX(0x80, IP_ACTIVE_LOW, 0, "P1 C", KEYCODE_C, IP_JOY_NONE );

		PORT_START(); /* IN1 */
		PORT_BIT(0x01, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x02, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x04, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x08, IP_ACTIVE_LOW, IPT_UNKNOWN);
		//         PORT_BITX(0x10, IP_ACTIVE_LOW, 0, "P1 Pon", KEYCODE_LALT,
		// IP_JOY_NONE );
		//         PORT_BITX(0x20, IP_ACTIVE_LOW, 0, "P1 L", KEYCODE_L, IP_JOY_NONE );
		//         PORT_BITX(0x40, IP_ACTIVE_LOW, 0, "P1 H", KEYCODE_H, IP_JOY_NONE );
		//         PORT_BITX(0x80, IP_ACTIVE_LOW, 0, "P1 D", KEYCODE_D, IP_JOY_NONE );

		PORT_START(); /* IN1 */
		PORT_BIT(0x01, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x02, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x04, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x08, IP_ACTIVE_LOW, IPT_UNKNOWN);
		//         PORT_BITX(0x10, IP_ACTIVE_LOW, 0, "P1 Flip", KEYCODE_X, IP_JOY_NONE
		// );
		PORT_BIT(0x20, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x40, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x80, IP_ACTIVE_LOW, IPT_UNKNOWN);

		PORT_START(); /* IN2 */
		PORT_BIT(0x01, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x02, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x04, IP_ACTIVE_LOW, IPT_START2);
		//         PORT_BITX(0x08, IP_ACTIVE_LOW, 0, "P2 Kan", KEYCODE_LCONTROL,
		// IP_JOY_NONE );
		//         PORT_BITX(0x10, IP_ACTIVE_LOW, 0, "P2 M", KEYCODE_M, IP_JOY_NONE );
		//         PORT_BITX(0x20, IP_ACTIVE_LOW, 0, "P2 I", KEYCODE_I, IP_JOY_NONE );
		//         PORT_BITX(0x40, IP_ACTIVE_LOW, 0, "P2 E", KEYCODE_E, IP_JOY_NONE );
		//         PORT_BITX(0x80, IP_ACTIVE_LOW, 0, "P2 A", KEYCODE_A, IP_JOY_NONE );

		PORT_START(); /* IN2 */
		PORT_BIT(0x01, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x02, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x04, IP_ACTIVE_LOW, IPT_UNKNOWN);
		//         PORT_BITX(0x08, IP_ACTIVE_LOW, 0, "P2 Reach", KEYCODE_LSHIFT,
		// IP_JOY_NONE );
		//         PORT_BITX(0x10, IP_ACTIVE_LOW, 0, "P2 N", KEYCODE_N, IP_JOY_NONE );
		//         PORT_BITX(0x20, IP_ACTIVE_LOW, 0, "P2 J", KEYCODE_J, IP_JOY_NONE );
		//         PORT_BITX(0x40, IP_ACTIVE_LOW, 0, "P2 F", KEYCODE_F, IP_JOY_NONE );
		//         PORT_BITX(0x80, IP_ACTIVE_LOW, 0, "P2 B", KEYCODE_B, IP_JOY_NONE );

		PORT_START(); /* IN2 */
		PORT_BIT(0x01, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x02, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x04, IP_ACTIVE_LOW, IPT_UNKNOWN);
		//         PORT_BITX(0x08, IP_ACTIVE_LOW, 0, "P2 Ron", KEYCODE_Z, IP_JOY_NONE
		// );
		//         PORT_BITX(0x10, IP_ACTIVE_LOW, 0, "P2 Chi", KEYCODE_SPACE,
		// IP_JOY_NONE );
		//         PORT_BITX(0x20, IP_ACTIVE_LOW, 0, "P2 K", KEYCODE_K, IP_JOY_NONE );
		//         PORT_BITX(0x40, IP_ACTIVE_LOW, 0, "P2 G", KEYCODE_G, IP_JOY_NONE );
		//         PORT_BITX(0x80, IP_ACTIVE_LOW, 0, "P2 C", KEYCODE_C, IP_JOY_NONE );

		PORT_START(); /* IN2 */
		PORT_BIT(0x01, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x02, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x04, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x08, IP_ACTIVE_LOW, IPT_UNKNOWN);
		//         PORT_BITX(0x10, IP_ACTIVE_LOW, 0, "P2 Pon", KEYCODE_LALT,
		// IP_JOY_NONE );
		//         PORT_BITX(0x20, IP_ACTIVE_LOW, 0, "P2 L", KEYCODE_L, IP_JOY_NONE );
		//         PORT_BITX(0x40, IP_ACTIVE_LOW, 0, "P2 H", KEYCODE_H, IP_JOY_NONE );
		//         PORT_BITX(0x80, IP_ACTIVE_LOW, 0, "P2 D", KEYCODE_D, IP_JOY_NONE );

		PORT_START(); /* IN2 */
		PORT_BIT(0x01, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x02, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x04, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x08, IP_ACTIVE_LOW, IPT_UNKNOWN);
		//         PORT_BITX(0x10, IP_ACTIVE_LOW, 0, "P2 Flip", KEYCODE_X, IP_JOY_NONE
		// );
		PORT_BIT(0x20, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x40, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x80, IP_ACTIVE_LOW, IPT_UNKNOWN);

		PORT_START(); /* DSW1 */
		PORT_DIPNAME(0x07, 0x07, "Coinage");
		PORT_DIPSETTING(0x00, "4C_1C");
		PORT_DIPSETTING(0x01, "3C_1C");
		PORT_DIPSETTING(0x02, "2C_1C");
		PORT_DIPSETTING(0x07, "1C_1C");
		PORT_DIPSETTING(0x06, "1C_2C");
		PORT_DIPSETTING(0x05, "1C_3C");
		PORT_DIPSETTING(0x04, "1C_4C");
		PORT_DIPSETTING(0x03, "1C_6C");
		PORT_DIPNAME(0x08, 0x08, "Rules");
		PORT_DIPSETTING(0x08, "Kantou");
		PORT_DIPSETTING(0x00, "Kansai");
		PORT_DIPNAME(0x10, 0x00, "Harness Type");
		PORT_DIPSETTING(0x10, "Generic");
		PORT_DIPSETTING(0x00, "Royal Mahjong");
		PORT_DIPNAME(0x20, 0x20, "Flip_Screen");
		PORT_DIPSETTING(0x20, "Off");
		PORT_DIPSETTING(0x00, "On");
		PORT_DIPNAME(0x40, 0x40, "Unknown");
		PORT_DIPSETTING(0x40, "Off");
		PORT_DIPSETTING(0x00, "On");
		PORT_SERVICE(0x80, IP_ACTIVE_LOW);

		PORT_START(); /* DSW2 */
		PORT_DIPNAME(0x03, 0x03, "Player 1 Skill");
		PORT_DIPSETTING(0x03, "Weak");
		PORT_DIPSETTING(0x02, "Normal");
		PORT_DIPSETTING(0x01, "Strong");
		PORT_DIPSETTING(0x00, "Very Strong");
		PORT_DIPNAME(0x0c, 0x0c, "Player 1 Skill");
		PORT_DIPSETTING(0x0c, "Weak");
		PORT_DIPSETTING(0x08, "Normal");
		PORT_DIPSETTING(0x04, "Strong");
		PORT_DIPSETTING(0x00, "Very Strong");
		PORT_DIPNAME(0x10, 0x00, "Music");
		PORT_DIPSETTING(0x10, "Off");
		PORT_DIPSETTING(0x00, "On");
		PORT_DIPNAME(0x20, 0x00, "Demo_Sounds");
		PORT_DIPSETTING(0x20, "Off");
		PORT_DIPSETTING(0x00, "On");
		PORT_DIPNAME(0x40, 0x00, "Help Mode");
		PORT_DIPSETTING(0x40, "Off");
		PORT_DIPSETTING(0x00, "On");
		PORT_DIPNAME(0x80, 0x80, "Unknown");
		PORT_DIPSETTING(0x80, "Off");
		PORT_DIPSETTING(0x00, "On");
		return true;
	}

	private boolean ipt_marukin() {
		PORT_START(); /* DSW */
		PORT_BIT(0x01, IP_ACTIVE_LOW, IPT_UNKNOWN);
		/*
		 * USED - handled in port5_r
		 */
		PORT_BITX(0x02, 0x02, IPT_SERVICE, DEF_STR2(Service_Mode), KEYCODE_F2, IP_JOY_NONE);
		PORT_BIT(0x04, IP_ACTIVE_LOW, IPT_UNKNOWN); /* unused? */
		PORT_BIT(0x08, IP_ACTIVE_LOW, IPT_UNKNOWN);
		/*
		 * USED - handled in port5_r
		 */
		PORT_BIT(0x70, IP_ACTIVE_LOW, IPT_UNKNOWN); /* unused? */
		PORT_BIT(0x80, IP_ACTIVE_LOW, IPT_UNKNOWN); /* data from EEPROM */

		PORT_START(); /* IN0 */
		PORT_BIT(0x01, IP_ACTIVE_LOW, IPT_SERVICE);
		/*
		 * same as the service mode farther down
		 */
		PORT_BIT(0x02, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x04, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x08, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x10, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x20, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x40, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x80, IP_ACTIVE_LOW, IPT_COIN1);

		PORT_START(); /* IN1 */
		PORT_BIT(0x01, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x02, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x04, IP_ACTIVE_LOW, IPT_START1);
		//         PORT_BITX(0x08, IP_ACTIVE_LOW, 0, "P1 Kan", KEYCODE_LCONTROL,
		// IP_JOY_NONE );
		//         PORT_BITX(0x10, IP_ACTIVE_LOW, 0, "P1 M", KEYCODE_M, IP_JOY_NONE );
		//         PORT_BITX(0x20, IP_ACTIVE_LOW, 0, "P1 I", KEYCODE_I, IP_JOY_NONE );
		//         PORT_BITX(0x40, IP_ACTIVE_LOW, 0, "P1 E", KEYCODE_E, IP_JOY_NONE );
		//         PORT_BITX(0x80, IP_ACTIVE_LOW, 0, "P1 A", KEYCODE_A, IP_JOY_NONE );

		PORT_START(); /* IN1 */
		PORT_BIT(0x01, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x02, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x04, IP_ACTIVE_LOW, IPT_UNKNOWN);
		//         PORT_BITX(0x08, IP_ACTIVE_LOW, 0, "P1 Reach", KEYCODE_LSHIFT,
		// IP_JOY_NONE );
		//         PORT_BITX(0x10, IP_ACTIVE_LOW, 0, "P1 N", KEYCODE_N, IP_JOY_NONE );
		//         PORT_BITX(0x20, IP_ACTIVE_LOW, 0, "P1 J", KEYCODE_J, IP_JOY_NONE );
		//         PORT_BITX(0x40, IP_ACTIVE_LOW, 0, "P1 F", KEYCODE_F, IP_JOY_NONE );
		//         PORT_BITX(0x80, IP_ACTIVE_LOW, 0, "P1 B", KEYCODE_B, IP_JOY_NONE );

		PORT_START(); /* IN1 */
		PORT_BIT(0x01, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x02, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x04, IP_ACTIVE_LOW, IPT_UNKNOWN);
		//         PORT_BITX(0x08, IP_ACTIVE_LOW, 0, "P1 Ron", KEYCODE_Z, IP_JOY_NONE
		// );
		//         PORT_BITX(0x10, IP_ACTIVE_LOW, 0, "P1 Chi", KEYCODE_SPACE,
		// IP_JOY_NONE );
		//         PORT_BITX(0x20, IP_ACTIVE_LOW, 0, "P1 K", KEYCODE_K, IP_JOY_NONE );
		//         PORT_BITX(0x40, IP_ACTIVE_LOW, 0, "P1 G", KEYCODE_G, IP_JOY_NONE );
		//         PORT_BITX(0x80, IP_ACTIVE_LOW, 0, "P1 C", KEYCODE_C, IP_JOY_NONE );

		PORT_START(); /* IN1 */
		PORT_BIT(0x01, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x02, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x04, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x08, IP_ACTIVE_LOW, IPT_UNKNOWN);
		//         PORT_BITX(0x10, IP_ACTIVE_LOW, 0, "P1 Pon", KEYCODE_LALT,
		// IP_JOY_NONE );
		//         PORT_BITX(0x20, IP_ACTIVE_LOW, 0, "P1 L", KEYCODE_L, IP_JOY_NONE );
		//         PORT_BITX(0x40, IP_ACTIVE_LOW, 0, "P1 H", KEYCODE_H, IP_JOY_NONE );
		//         PORT_BITX(0x80, IP_ACTIVE_LOW, 0, "P1 D", KEYCODE_D, IP_JOY_NONE );

		PORT_START(); /* IN1 */
		PORT_BIT(0x01, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x02, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x04, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x08, IP_ACTIVE_LOW, IPT_UNKNOWN);
		//         PORT_BITX(0x10, IP_ACTIVE_LOW, 0, "P1 Flip", KEYCODE_X, IP_JOY_NONE
		// );
		PORT_BIT(0x20, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x40, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x80, IP_ACTIVE_LOW, IPT_UNKNOWN);

		PORT_START(); /* IN2 */
		PORT_BIT(0x01, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x02, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x04, IP_ACTIVE_LOW, IPT_START2);
		//         PORT_BITX(0x08, IP_ACTIVE_LOW, 0, "P2 Kan", KEYCODE_LCONTROL,
		// IP_JOY_NONE );
		//         PORT_BITX(0x10, IP_ACTIVE_LOW, 0, "P2 M", KEYCODE_M, IP_JOY_NONE );
		//         PORT_BITX(0x20, IP_ACTIVE_LOW, 0, "P2 I", KEYCODE_I, IP_JOY_NONE );
		//         PORT_BITX(0x40, IP_ACTIVE_LOW, 0, "P2 E", KEYCODE_E, IP_JOY_NONE );
		//         PORT_BITX(0x80, IP_ACTIVE_LOW, 0, "P2 A", KEYCODE_A, IP_JOY_NONE );

		PORT_START(); /* IN2 */
		PORT_BIT(0x01, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x02, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x04, IP_ACTIVE_LOW, IPT_UNKNOWN);
		//         PORT_BITX(0x08, IP_ACTIVE_LOW, 0, "P2 Reach", KEYCODE_LSHIFT,
		// IP_JOY_NONE );
		//         PORT_BITX(0x10, IP_ACTIVE_LOW, 0, "P2 N", KEYCODE_N, IP_JOY_NONE );
		//         PORT_BITX(0x20, IP_ACTIVE_LOW, 0, "P2 J", KEYCODE_J, IP_JOY_NONE );
		//         PORT_BITX(0x40, IP_ACTIVE_LOW, 0, "P2 F", KEYCODE_F, IP_JOY_NONE );
		//         PORT_BITX(0x80, IP_ACTIVE_LOW, 0, "P2 B", KEYCODE_B, IP_JOY_NONE );

		PORT_START(); /* IN2 */
		PORT_BIT(0x01, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x02, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x04, IP_ACTIVE_LOW, IPT_UNKNOWN);
		//         PORT_BITX(0x08, IP_ACTIVE_LOW, 0, "P2 Ron", KEYCODE_Z, IP_JOY_NONE
		// );
		//         PORT_BITX(0x10, IP_ACTIVE_LOW, 0, "P2 Chi", KEYCODE_SPACE,
		// IP_JOY_NONE );
		//         PORT_BITX(0x20, IP_ACTIVE_LOW, 0, "P2 K", KEYCODE_K, IP_JOY_NONE );
		//         PORT_BITX(0x40, IP_ACTIVE_LOW, 0, "P2 G", KEYCODE_G, IP_JOY_NONE );
		//         PORT_BITX(0x80, IP_ACTIVE_LOW, 0, "P2 C", KEYCODE_C, IP_JOY_NONE );

		PORT_START(); /* IN2 */
		PORT_BIT(0x01, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x02, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x04, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x08, IP_ACTIVE_LOW, IPT_UNKNOWN);
		//         PORT_BITX(0x10, IP_ACTIVE_LOW, 0, "P2 Pon", KEYCODE_LALT,
		// IP_JOY_NONE );
		//         PORT_BITX(0x20, IP_ACTIVE_LOW, 0, "P2 L", KEYCODE_L, IP_JOY_NONE );
		//         PORT_BITX(0x40, IP_ACTIVE_LOW, 0, "P2 H", KEYCODE_H, IP_JOY_NONE );
		//         PORT_BITX(0x80, IP_ACTIVE_LOW, 0, "P2 D", KEYCODE_D, IP_JOY_NONE );

		PORT_START(); /* IN2 */
		PORT_BIT(0x01, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x02, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x04, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x08, IP_ACTIVE_LOW, IPT_UNKNOWN);
		//         PORT_BITX(0x10, IP_ACTIVE_LOW, 0, "P2 Flip", KEYCODE_X, IP_JOY_NONE
		// );
		PORT_BIT(0x20, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x40, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x80, IP_ACTIVE_LOW, IPT_UNKNOWN);
		return true;
	}

	private boolean ipt_pkladies() {
		PORT_START(); /* DSW */
		PORT_BIT(0x01, IP_ACTIVE_LOW, IPT_UNKNOWN);
		/*
		 * USED - handled in port5_r
		 */
		//         PORT_BITX(0x02, 0x02, IPT_SERVICE, DEF_STR2( Service_Mode ),
		// KEYCODE_F2, IP_JOY_NONE );
		PORT_BIT(0x04, IP_ACTIVE_LOW, IPT_UNKNOWN); /* unused? */
		PORT_BIT(0x08, IP_ACTIVE_LOW, IPT_UNKNOWN);
		/*
		 * USED - handled in port5_r
		 */
		PORT_BIT(0x70, IP_ACTIVE_LOW, IPT_UNKNOWN); /* unused? */
		PORT_BIT(0x80, IP_ACTIVE_LOW, IPT_UNKNOWN); /* data from EEPROM */

		PORT_START(); /* IN0 */
		PORT_BIT(0x01, IP_ACTIVE_LOW, IPT_SERVICE);
		/*
		 * same as the service mode farther down
		 */
		PORT_BIT(0x02, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x04, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x08, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x10, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x20, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x40, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x80, IP_ACTIVE_LOW, IPT_COIN1);

		PORT_START(); /* IN1 */
		PORT_BIT(0x01, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x02, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x04, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x08, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x10, IP_ACTIVE_LOW, IPT_UNKNOWN);
		//         PORT_BITX(0x20, IP_ACTIVE_LOW, 0, "P1 Deal", KEYCODE_LCONTROL,
		// IP_JOY_NONE );
		//         PORT_BITX(0x40, IP_ACTIVE_LOW, 0, "P1 E", KEYCODE_E, IP_JOY_NONE );
		//         PORT_BITX(0x80, IP_ACTIVE_LOW, 0, "P1 A", KEYCODE_A, IP_JOY_NONE );

		PORT_START(); /* IN1 */
		PORT_BIT(0x01, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x02, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x04, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x08, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x10, IP_ACTIVE_LOW, IPT_UNKNOWN);
		//         PORT_BITX(0x20, IP_ACTIVE_LOW, 0, "P1 Cancel", KEYCODE_LALT,
		// IP_JOY_NONE );
		PORT_BIT(0x40, IP_ACTIVE_LOW, IPT_UNKNOWN);
		//         PORT_BITX(0x80, IP_ACTIVE_LOW, 0, "P1 B", KEYCODE_B, IP_JOY_NONE );

		PORT_START(); /* IN1 */
		PORT_BIT(0x01, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x02, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x04, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x08, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x10, IP_ACTIVE_LOW, IPT_UNKNOWN);
		//         PORT_BITX(0x20, IP_ACTIVE_LOW, 0, "P1 Flip", KEYCODE_SPACE,
		// IP_JOY_NONE );
		PORT_BIT(0x40, IP_ACTIVE_LOW, IPT_UNKNOWN);
		//         PORT_BITX(0x80, IP_ACTIVE_LOW, 0, "P1 C", KEYCODE_C, IP_JOY_NONE );

		PORT_START(); /* IN1 */
		PORT_BIT(0x01, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x02, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x04, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x08, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x08, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x10, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x20, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x40, IP_ACTIVE_LOW, IPT_UNKNOWN);
		//         PORT_BITX(0x80, IP_ACTIVE_LOW, 0, "P1 D", KEYCODE_D, IP_JOY_NONE );

		PORT_START(); /* IN1 */
		PORT_BIT(0x01, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x02, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x04, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x08, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x10, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x20, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x40, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x80, IP_ACTIVE_LOW, IPT_UNKNOWN);

		PORT_START(); /* IN2 */
		PORT_BIT(0x01, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x02, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x04, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x08, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x10, IP_ACTIVE_LOW, IPT_UNKNOWN);
		//         PORT_BITX(0x20, IP_ACTIVE_LOW, 0, "P2 Deal", KEYCODE_LCONTROL,
		// IP_JOY_NONE );
		//         PORT_BITX(0x40, IP_ACTIVE_LOW, 0, "P2 E", KEYCODE_E, IP_JOY_NONE );
		//         PORT_BITX(0x80, IP_ACTIVE_LOW, 0, "P2 A", KEYCODE_A, IP_JOY_NONE );

		PORT_START(); /* IN2 */
		PORT_BIT(0x01, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x02, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x04, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x08, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x10, IP_ACTIVE_LOW, IPT_UNKNOWN);
		//         PORT_BITX(0x20, IP_ACTIVE_LOW, 0, "P2 Cancel", KEYCODE_LALT,
		// IP_JOY_NONE );
		PORT_BIT(0x40, IP_ACTIVE_LOW, IPT_UNKNOWN);
		//         PORT_BITX(0x80, IP_ACTIVE_LOW, 0, "P2 B", KEYCODE_B, IP_JOY_NONE );

		PORT_START(); /* IN2 */
		PORT_BIT(0x01, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x02, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x04, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x08, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x10, IP_ACTIVE_LOW, IPT_UNKNOWN);
		//         PORT_BITX(0x20, IP_ACTIVE_LOW, 0, "P2 Flip", KEYCODE_SPACE,
		// IP_JOY_NONE );
		PORT_BIT(0x40, IP_ACTIVE_LOW, IPT_UNKNOWN);
		//         PORT_BITX(0x80, IP_ACTIVE_LOW, 0, "P2 C", KEYCODE_C, IP_JOY_NONE );

		PORT_START(); /* IN2 */
		PORT_BIT(0x01, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x02, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x04, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x08, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x08, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x10, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x20, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x40, IP_ACTIVE_LOW, IPT_UNKNOWN);
		//         PORT_BITX(0x80, IP_ACTIVE_LOW, 0, "P2 D", KEYCODE_D, IP_JOY_NONE );

		PORT_START(); /* IN2 */
		PORT_BIT(0x01, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x02, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x04, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x08, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x10, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x20, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x40, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x80, IP_ACTIVE_LOW, IPT_UNKNOWN);
		return true;
	}

	private boolean ipt_pang() {
		PORT_START(); /* DSW */
		PORT_BIT(0x01, IP_ACTIVE_LOW, IPT_UNKNOWN);
		/*
		 * USED - handled in port5_r
		 */
		//         PORT_BITX(0x02, 0x02, IPT_SERVICE, DEF_STR( Service_Mode ),
		// KEYCODE_F2, IP_JOY_NONE );
		PORT_BIT(0x04, IP_ACTIVE_LOW, IPT_UNKNOWN); /* unused? */
		PORT_BIT(0x08, IP_ACTIVE_LOW, IPT_UNKNOWN);
		/*
		 * USED - handled in port5_r
		 */
		PORT_BIT(0x70, IP_ACTIVE_LOW, IPT_UNKNOWN); /* unused? */
		PORT_BIT(0x80, IP_ACTIVE_LOW, IPT_UNKNOWN); /* data from EEPROM */

		PORT_START(); /* IN0 */
		PORT_BIT(0x01, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x02, IP_ACTIVE_LOW, IPT_START2);
		PORT_BIT(0x04, IP_ACTIVE_LOW, IPT_UNKNOWN); /* probably unused */
		PORT_BIT(0x08, IP_ACTIVE_LOW, IPT_START1);
		PORT_BIT(0x10, IP_ACTIVE_LOW, IPT_UNKNOWN); /* probably unused */
		PORT_BIT(0x20, IP_ACTIVE_LOW, IPT_UNKNOWN); /* probably unused */
		PORT_BIT(0x40, IP_ACTIVE_LOW, IPT_COIN2);
		PORT_BIT(0x80, IP_ACTIVE_LOW, IPT_COIN1);

		PORT_START(); /* IN1 */
		PORT_BIT(0x01, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x02, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x04, IP_ACTIVE_LOW, IPT_BUTTON2);
		PORT_BIT(0x08, IP_ACTIVE_LOW, IPT_BUTTON1);
		PORT_BIT(0x10, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY);
		PORT_BIT(0x20, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT | IPF_8WAY);
		PORT_BIT(0x40, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN | IPF_8WAY);
		PORT_BIT(0x80, IP_ACTIVE_LOW, IPT_JOYSTICK_UP | IPF_8WAY);

		PORT_START(); /* IN2 */
		PORT_BIT(0x01, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x02, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x04, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER2);
		PORT_BIT(0x08, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER2);
		PORT_BIT(0x10, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_PLAYER2);
		PORT_BIT(0x20, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT | IPF_8WAY | IPF_PLAYER2);
		PORT_BIT(0x40, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN | IPF_8WAY | IPF_PLAYER2);
		PORT_BIT(0x80, IP_ACTIVE_LOW, IPT_JOYSTICK_UP | IPF_8WAY | IPF_PLAYER2);
		return true;
	}

	public boolean ipt_qtono1() {
		PORT_START(); /* DSW */
		PORT_BIT(0x01, IP_ACTIVE_LOW, IPT_UNKNOWN);
		/*
		 * USED - handled in port5_r
		 */
		//         PORT_BITX(0x02, 0x02, IPT_SERVICE, DEF_STR( Service_Mode ),
		// KEYCODE_F2, IP_JOY_NONE );
		PORT_BIT(0x04, IP_ACTIVE_LOW, IPT_UNKNOWN); /* unused? */
		PORT_BIT(0x08, IP_ACTIVE_LOW, IPT_UNKNOWN);
		/*
		 * USED - handled in port5_r
		 */
		PORT_BIT(0x70, IP_ACTIVE_LOW, IPT_UNKNOWN); /* unused? */
		PORT_BIT(0x80, IP_ACTIVE_LOW, IPT_UNKNOWN); /* data from EEPROM */

		PORT_START(); /* IN0 */
		PORT_BIT(0x01, IP_ACTIVE_LOW, IPT_SERVICE);
		/*
		 * same as the service mode farther down
		 */
		PORT_BIT(0x02, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x04, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x08, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x10, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x20, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x40, IP_ACTIVE_LOW, IPT_COIN2);
		PORT_BIT(0x80, IP_ACTIVE_LOW, IPT_COIN1);

		PORT_START(); /* IN1 */
		PORT_BIT(0x01, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x02, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x04, IP_ACTIVE_LOW, IPT_START1);
		PORT_BIT(0x08, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x10, IP_ACTIVE_LOW, IPT_BUTTON4);
		PORT_BIT(0x20, IP_ACTIVE_LOW, IPT_BUTTON3);
		PORT_BIT(0x40, IP_ACTIVE_LOW, IPT_BUTTON2);
		PORT_BIT(0x80, IP_ACTIVE_LOW, IPT_BUTTON1);

		PORT_START(); /* IN2 */
		PORT_BIT(0x01, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x02, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x04, IP_ACTIVE_LOW, IPT_START2);
		PORT_BIT(0x08, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x10, IP_ACTIVE_LOW, IPT_BUTTON4 | IPF_PLAYER2);
		PORT_BIT(0x20, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER2);
		PORT_BIT(0x40, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER2);
		PORT_BIT(0x80, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER2);
		return true;
	}

	private boolean ipt_block() {
		PORT_START(); /* DSW */
		PORT_BIT(0x01, IP_ACTIVE_LOW, IPT_UNKNOWN);
		/*
		 * USED - handled in port5_r
		 */
		//         PORT_BITX(0x02, 0x02, IPT_SERVICE, DEF_STR( Service_Mode ),
		// KEYCODE_F2, IP_JOY_NONE );
		PORT_BIT(0x04, IP_ACTIVE_LOW, IPT_UNKNOWN); /* unused? */
		PORT_BIT(0x08, IP_ACTIVE_LOW, IPT_UNKNOWN);
		/*
		 * USED - handled in port5_r
		 */
		PORT_BIT(0x70, IP_ACTIVE_LOW, IPT_UNKNOWN); /* unused? */
		PORT_BIT(0x80, IP_ACTIVE_LOW, IPT_UNKNOWN); /* data from EEPROM */

		PORT_START(); /* IN0 */
		PORT_BIT(0x01, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x02, IP_ACTIVE_LOW, IPT_START2);
		PORT_BIT(0x04, IP_ACTIVE_LOW, IPT_UNKNOWN); /* probably unused */
		PORT_BIT(0x08, IP_ACTIVE_LOW, IPT_START1);
		PORT_BIT(0x10, IP_ACTIVE_LOW, IPT_UNKNOWN); /* probably unused */
		PORT_BIT(0x20, IP_ACTIVE_LOW, IPT_UNKNOWN); /* probably unused */
		PORT_BIT(0x40, IP_ACTIVE_LOW, IPT_COIN2);
		PORT_BIT(0x80, IP_ACTIVE_LOW, IPT_COIN1);

		PORT_START(); /* IN1 */
		PORT_BIT(0x01, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x02, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x04, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x08, IP_ACTIVE_LOW, IPT_SPECIAL); /* dial direction */
		PORT_BIT(0x10, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT);
		PORT_BIT(0x20, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT);
		PORT_BIT(0x40, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x80, IP_ACTIVE_LOW, IPT_BUTTON1);

		PORT_START(); /* IN2 */
		PORT_BIT(0x01, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x02, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x04, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x08, IP_ACTIVE_LOW, IPT_SPECIAL); /* dial direction */
		PORT_BIT(0x10, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_PLAYER2);
		PORT_BIT(0x20, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT | IPF_PLAYER2);
		PORT_BIT(0x40, IP_ACTIVE_LOW, IPT_UNKNOWN);
		PORT_BIT(0x80, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER2);

		PORT_START(); /* DIAL1 */
		PORT_ANALOG(0xff, 0x00, IPT_DIAL, 50, 20, 0, 0);

		PORT_START(); /* DIAL2 */
		PORT_ANALOG(0xff, 0x00, IPT_DIAL | IPF_PLAYER2, 50, 20, 0, 0);
		return true;
	}

	int[][] charlayout = { { 8 }, {
			8 }, // 8*8 characters
		{
			32768 }, // 32768 characters
		{
			4 }, // 4 bits per pixel
		{
			32768 * 16 * 8 + 4, 32768 * 16 * 8 + 0, 4, 0 }, {
			0, 1, 2, 3, 8 + 0, 8 + 1, 8 + 2, 8 + 3 }, {
			0 * 16, 1 * 16, 2 * 16, 3 * 16, 4 * 16, 5 * 16, 6 * 16, 7 * 16 }, {
			16 * 8 } // every char takes 16 consecutive bytes
	};

	int[][] marukin_charlayout = { { 8 }, {
			8 }, // 8*8 characters
		{
			65536 }, // 65536 characters
		{
			4 }, // 4 bits per pixel
		{
			3 * 4, 2 * 4, 1 * 4, 0 * 4 }, {
			0, 1, 2, 3, 16 + 0, 16 + 1, 16 + 2, 16 + 3 }, {
			0 * 32, 1 * 32, 2 * 32, 3 * 32, 4 * 32, 5 * 32, 6 * 32, 7 * 32 }, {
			32 * 8 } // every char takes 32 consecutive bytes
	};

	int[][] spritelayout = { { 16 }, {
			16 }, // 16*16 sprites
		{
			2048 }, // 2048 sprites
		{
			4 }, // 4 bits per pixel
		{
			2048 * 64 * 8 + 4, 2048 * 64 * 8 + 0, 4, 0 }, {
			0,
				1,
				2,
				3,
				8 + 0,
				8 + 1,
				8 + 2,
				8 + 3,
				32 * 8 + 0,
				32 * 8 + 1,
				32 * 8 + 2,
				32 * 8 + 3,
				33 * 8 + 0,
				33 * 8 + 1,
				33 * 8 + 2,
				33 * 8 + 3 },
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
			64 * 8 } // every sprite takes 64 consecutive bytes
	};

	private boolean mgakuen_gfxdecodeinfo() {
		GDI_ADD(REGION_GFX1, 0, marukin_charlayout, 0, 64); /* colors 0-1023 */
		GDI_ADD(REGION_GFX2, 0, spritelayout, 0, 16); /* colors 0- 255 */
		GDI_ADD(-1); /* end of array */
		return true;
	}

	private boolean marukin_gfxdecodeinfo() {
		GDI_ADD(REGION_GFX1, 0, marukin_charlayout, 0, 128); /* colors 0-2047 */
		GDI_ADD(REGION_GFX2, 0, spritelayout, 0, 16); /* colors 0- 255 */
		GDI_ADD(-1); /* end of array */
		return true;
	};

	private boolean gfxdecodeinfo() {
		GDI_ADD(REGION_GFX1, 0, charlayout, 0, 128); /* colors 0-2047 */
		GDI_ADD(REGION_GFX2, 0, spritelayout, 0, 16); /* colors 0- 255 */
		GDI_ADD(-1); /* end of array */
		return true;
	};

	/*
	 * static struct YM2413interface ym2413_interface = { 1, // 1 chip 3579545, //
	 * ??? { 100 }, // Volume
	 */

	/*
	 * static struct OKIM6295interface okim6295_interface = { 1, // 1 chip {
	 * 8000 }, // 8000Hz ??? { REGION_SOUND1 }, // memory region 2 { 50 }
	 */

	public boolean mdrv_mgakuen() {
		/* basic machine hardware */
		MDRV_CPU_ADD(Z80, 6000000); /* ??? */
		MDRV_CPU_MEMORY(mgakuen_readmem(), mgakuen_writemem());
		MDRV_CPU_PORTS(readport(), writeport());
		MDRV_CPU_VBLANK_INT(irq0_line_hold, 2);
		/* ??? one extra irq seems to be needed for music (see input5_r) */

		MDRV_FRAMES_PER_SECOND(60);
		MDRV_VBLANK_DURATION(DEFAULT_60HZ_VBLANK_DURATION);

		/* video hardware */
		MDRV_VIDEO_ATTRIBUTES(VIDEO_TYPE_RASTER);
		MDRV_SCREEN_SIZE(64 * 8, 32 * 8);
		MDRV_VISIBLE_AREA(8 * 8, (64 - 8) * 8 - 1, 1 * 8, 31 * 8 - 1);
		MDRV_GFXDECODE(mgakuen_gfxdecodeinfo());
		MDRV_PALETTE_LENGTH(1024); /* less colors than the others */

		MDRV_VIDEO_START(generic_vs);
		MDRV_VIDEO_UPDATE(vh_pang);

		/* sound hardware */
		//	MDRV_SOUND_ADD(OKIM6295, okim6295_interface)
		//	MDRV_SOUND_ADD(YM2413, ym2413_interface)
		return true;
	}

	public boolean mdrv_pang() {
		/* basic machine hardware */
		MDRV_CPU_ADD(Z80, 8000000); /* Super Pang says 8MHZ ORIGINAL BOARD */
		MDRV_CPU_MEMORY(readmem(), writemem());
		MDRV_CPU_PORTS(readport(), writeport());
		MDRV_CPU_VBLANK_INT(irq0_line_hold, 2);
		/* ??? one extra irq seems to be needed for music (see input5_r) */

		MDRV_FRAMES_PER_SECOND(60);
		MDRV_VBLANK_DURATION(DEFAULT_60HZ_VBLANK_DURATION);

		//         MDRV_NVRAM_HANDLER(mitchell);

		/* video hardware */
		MDRV_VIDEO_ATTRIBUTES(VIDEO_TYPE_RASTER);
		MDRV_SCREEN_SIZE(64 * 8, 32 * 8);
		MDRV_VISIBLE_AREA(8 * 8, (64 - 8) * 8 - 1, 1 * 8, 31 * 8 - 1);
		MDRV_GFXDECODE(gfxdecodeinfo());
		MDRV_PALETTE_LENGTH(2048);

		MDRV_VIDEO_START(generic_vs);
		MDRV_VIDEO_UPDATE(vh_pang);

		/* sound hardware */
		//	MDRV_SOUND_ADD(OKIM6295, okim6295_interface)
		//	MDRV_SOUND_ADD(YM2413, ym2413_interface)
		return true;
	}

	public boolean mdrv_marukin() {
		/* basic machine hardware */
		MDRV_CPU_ADD(Z80, 8000000); /* Super Pang says 8MHZ ORIGINAL BOARD */
		MDRV_CPU_MEMORY(readmem(), writemem());
		MDRV_CPU_PORTS(readport(), writeport());
		MDRV_CPU_VBLANK_INT(irq0_line_hold, 2);
		/* ??? one extra irq seems to be needed for music (see input5_r) */

		MDRV_FRAMES_PER_SECOND(60);
		MDRV_VBLANK_DURATION(DEFAULT_60HZ_VBLANK_DURATION);

		//         MDRV_NVRAM_HANDLER(mitchell);

		/* video hardware */
		MDRV_VIDEO_ATTRIBUTES(VIDEO_TYPE_RASTER);
		MDRV_SCREEN_SIZE(64 * 8, 32 * 8);
		MDRV_VISIBLE_AREA(8 * 8, (64 - 8) * 8 - 1, 1 * 8, 31 * 8 - 1);
		MDRV_GFXDECODE(marukin_gfxdecodeinfo());
		MDRV_PALETTE_LENGTH(2048);

		MDRV_VIDEO_START(generic_vs);
		MDRV_VIDEO_UPDATE(vh_pang);

		/* sound hardware */
		//	MDRV_SOUND_ADD(OKIM6295, okim6295_interface)
		//	MDRV_SOUND_ADD(YM2413, ym2413_interface)
		return true;
	}

	private boolean rom_mgakuen() {
		ROM_REGION(0x30000, REGION_CPU1, 0); /* 192k for code */
		ROM_LOAD("mg-1.1j", 0x00000, 0x08000, 0xbf02ea6b);
		ROM_LOAD("mg-2.1l", 0x10000, 0x20000, 0x64141b0c);

		ROM_REGION(0x200000, REGION_GFX1, ROMREGION_DISPOSE);
		ROM_LOAD("mg-1.13h", 0x000000, 0x80000, 0xfd6a0805); /* chars */
		ROM_LOAD("mg-2.14h", 0x080000, 0x80000, 0xe26e871e);
		ROM_LOAD("mg-3.16h", 0x100000, 0x80000, 0xdd781d9a);
		ROM_LOAD("mg-4.17h", 0x180000, 0x80000, 0x97afcc79);

		ROM_REGION(0x040000, REGION_GFX2, ROMREGION_DISPOSE);
		ROM_LOAD("mg-6.4l", 0x000000, 0x20000, 0x34594e62); /* sprites */
		ROM_LOAD("mg-7.6l", 0x020000, 0x20000, 0xf304c806);

		//         ROM_REGION( 0x80000, REGION_SOUND1, 0 ); /* OKIM */
		//         ROM_LOAD( "mg-5.1c", 0x00000, 0x80000, 0x170332f1 ); /* banked */
		return true;
	}

	private boolean rom_mgakuen2() {
		ROM_REGION(2 * 0x50000, REGION_CPU1, 0);
		/*
		 * 320k for code + 320k for decrypted opcodes
		 */
		ROM_LOAD("mg2-xf.1j", 0x00000, 0x08000, 0xc8165d2d);
		ROM_LOAD("mg2-y.1l", 0x10000, 0x20000, 0x75bbcc14);
		ROM_LOAD("mg2-z.3l", 0x30000, 0x20000, 0xbfdba961);

		ROM_REGION(0x200000, REGION_GFX1, ROMREGION_DISPOSE);
		ROM_LOAD("mg2-a.13h", 0x000000, 0x80000, 0x31a0c55e); /* chars */
		ROM_LOAD("mg2-b.14h", 0x080000, 0x80000, 0xc18488fa);
		ROM_LOAD("mg2-c.16h", 0x100000, 0x80000, 0x9425b364);
		ROM_LOAD("mg2-d.17h", 0x180000, 0x80000, 0x6cc9eeba);

		ROM_REGION(0x040000, REGION_GFX2, ROMREGION_DISPOSE);
		ROM_LOAD("mg2-f.4l", 0x000000, 0x20000, 0x3172c9fe); /* sprites */
		ROM_LOAD("mg2-g.6l", 0x020000, 0x20000, 0x19b8b61c);

		//         ROM_REGION( 0x80000, REGION_SOUND1, 0 ); /* OKIM */
		//         ROM_LOAD( "mg2-e.1c", 0x00000, 0x80000, 0x70fd0809 ); /* banked */
		return true;
	}

	public boolean rom_pkladies() {
		ROM_REGION(2 * 0x20000, REGION_CPU1, 0);
		/*
		 * 128k for code + 128k for decrypted opcodes
		 */
		ROM_LOAD("pko-prg1.14f", 0x00000, 0x08000, 0x86585a94);
		ROM_LOAD("pko-prg2.15f", 0x10000, 0x10000, 0x86cbe82d);

		ROM_REGION(0x200000, REGION_GFX1, ROMREGION_DISPOSE);
		ROM_LOAD16_BYTE("pko-001.8h", 0x000000, 0x80000, 0x1ead5d9b); /* chars */
		ROM_LOAD16_BYTE("pko-003.8j", 0x000001, 0x80000, 0x339ab4e6);
		ROM_LOAD16_BYTE("pko-002.9h", 0x100000, 0x80000, 0x1cf02586);
		ROM_LOAD16_BYTE("pko-004.9j", 0x100001, 0x80000, 0x09ccb442);

		ROM_REGION(0x040000, REGION_GFX2, ROMREGION_DISPOSE);
		ROM_LOAD("pko-chr1.2j", 0x000000, 0x20000, 0x31ce33cd); /* sprites */
		ROM_LOAD("pko-chr2.3j", 0x020000, 0x20000, 0xad7e055f);

		//         ROM_REGION( 0x80000, REGION_SOUND1, 0 ); /* OKIM */
		//         ROM_LOAD( "pko-voi1.2d", 0x00000, 0x20000, 0x07e0f531 );
		//         ROM_LOAD( "pko-voi2.3d", 0x20000, 0x20000, 0x18398bf6 );
		return true;
	}

	public boolean rom_pkladiel() {
		ROM_REGION(2 * 0x30000, REGION_CPU1, 0);
		/*
		 * 128k for code + 128k for decrypted opcodes
		 */
		ROM_LOAD("pk05.14f", 0x00000, 0x08000, 0xea1740a6);
		ROM_LOAD("pk06.15f", 0x10000, 0x20000, 0x3078ff5e);
		/* larger than pkladies - 2nd half unused? */

		ROM_REGION(0x200000, REGION_GFX1, ROMREGION_DISPOSE);
		ROM_LOAD16_BYTE("pko-001.8h", 0x000000, 0x80000, 0x1ead5d9b); /* chars */
		ROM_LOAD16_BYTE("pko-003.8j", 0x000001, 0x80000, 0x339ab4e6);
		ROM_LOAD16_BYTE("pko-002.9h", 0x100000, 0x80000, 0x1cf02586);
		ROM_LOAD16_BYTE("pko-004.9j", 0x100001, 0x80000, 0x09ccb442);

		ROM_REGION(0x040000, REGION_GFX2, ROMREGION_DISPOSE);
		ROM_LOAD("pko-chr1.2j", 0x000000, 0x20000, 0x31ce33cd); /* sprites */
		ROM_LOAD("pko-chr2.3j", 0x020000, 0x20000, 0xad7e055f);

		//         ROM_REGION( 0x80000, REGION_SOUND1, 0 ); /* OKIM */
		//         ROM_LOAD( "pko-voi1.2d", 0x00000, 0x20000, 0x07e0f531 );
		//         ROM_LOAD( "pko-voi2.3d", 0x20000, 0x20000, 0x18398bf6 );
		return true;
	}

	public boolean rom_dokaben() {
		ROM_REGION(2 * 0x50000, REGION_CPU1, 0);
		/*
		 * 320k for code + 320k for decrypted opcodes
		 */
		ROM_LOAD("db06.11h", 0x00000, 0x08000, 0x413e0886);
		ROM_LOAD("db07.13h", 0x10000, 0x20000, 0x8bdcf49e);
		ROM_LOAD("db08.14h", 0x30000, 0x20000, 0x1643bdd9);

		//         ROM_REGION( 0x100000, REGION_GFX1, ROMREGION_DISPOSE );
		//         ROM_LOAD( "db02.1e", 0x000000, 0x20000, 0x9aa8470c ); /* chars */
		//         ROM_LOAD( "db03.2e", 0x020000, 0x20000, 0x3324e43d );
		/* 40000-7ffff empty */
		ROM_LOAD("db04.1g", 0x080000, 0x20000, 0xc0c5b6c2);
		ROM_LOAD("db05.2g", 0x0a0000, 0x20000, 0xd2ab25f2);
		/* c0000-fffff empty */

		ROM_REGION(0x040000, REGION_GFX2, ROMREGION_DISPOSE);
		ROM_LOAD("db10.2k", 0x000000, 0x20000, 0x9e70f7ae); /* sprites */
		ROM_LOAD("db09.1k", 0x020000, 0x20000, 0x2d9263f7);

		//         ROM_REGION( 0x80000, REGION_SOUND1, 0 ); /* OKIM */
		//         ROM_LOAD( "db01.1d", 0x00000, 0x20000, 0x62fa6b81 );
		return true;
	}

	public boolean rom_pang() {
		ROM_REGION(2 * 0x30000, REGION_CPU1, 0);
		/*
		 * 192k for code + 192k for decrypted opcodes
		 */
		ROM_LOAD("pang6.bin", 0x00000, 0x08000, 0x68be52cd);
		ROM_LOAD("pang7.bin", 0x10000, 0x20000, 0x4a2e70f6);

		ROM_REGION(0x100000, REGION_GFX1, ROMREGION_DISPOSE);
		ROM_LOAD("pang_09.bin", 0x000000, 0x20000, 0x3a5883f5); /* chars */
		ROM_LOAD("bb3.bin", 0x020000, 0x20000, 0x79a8ed08);
		/* 40000-7ffff empty */
		ROM_LOAD("pang_11.bin", 0x080000, 0x20000, 0x166a16ae);
		ROM_LOAD("bb5.bin", 0x0a0000, 0x20000, 0x2fb3db6c);
		/* c0000-fffff empty */

		ROM_REGION(0x040000, REGION_GFX2, ROMREGION_DISPOSE);
		ROM_LOAD("bb10.bin", 0x000000, 0x20000, 0xfdba4f6e); /* sprites */
		ROM_LOAD("bb9.bin", 0x020000, 0x20000, 0x39f47a63);

		//         ROM_REGION( 0x80000, REGION_SOUND1, 0 ); /* OKIM */
		//         ROM_LOAD( "bb1.bin", 0x00000, 0x20000, 0xc52e5b8e );
		return true;
	}

	public boolean rom_pangb() {
		ROM_REGION(2 * 0x30000, REGION_CPU1, 0);
		/*
		 * 192k for code + 192k for decrypted opcodes
		 */
		ROM_LOAD("pang_04.bin", 0x30000, 0x08000, 0xf68f88a5);
		/*
		 * Decrypted opcode + data
		 */
		ROM_CONTINUE(0x00000, 0x08000);
		ROM_LOAD("pang_02.bin", 0x40000, 0x20000, 0x3f15bb61);
		/*
		 * Decrypted op codes
		 */
		ROM_LOAD("pang_03.bin", 0x10000, 0x20000, 0x0c8477ae);
		/*
		 * Decrypted data
		 */

		ROM_REGION(0x100000, REGION_GFX1, ROMREGION_DISPOSE);
		ROM_LOAD("pang_09.bin", 0x000000, 0x20000, 0x3a5883f5); /* chars */
		ROM_LOAD("bb3.bin", 0x020000, 0x20000, 0x79a8ed08);
		/* 40000-7ffff empty */
		ROM_LOAD("pang_11.bin", 0x080000, 0x20000, 0x166a16ae);
		ROM_LOAD("bb5.bin", 0x0a0000, 0x20000, 0x2fb3db6c);
		/* c0000-fffff empty */

		ROM_REGION(0x040000, REGION_GFX2, ROMREGION_DISPOSE);
		ROM_LOAD("bb10.bin", 0x000000, 0x20000, 0xfdba4f6e); /* sprites */
		ROM_LOAD("bb9.bin", 0x020000, 0x20000, 0x39f47a63);

		//         ROM_REGION( 0x80000, REGION_SOUND1, 0 ); /* OKIM */
		//         ROM_LOAD( "bb1.bin", 0x00000, 0x20000, 0xc52e5b8e );
		return true;
	}

	public boolean rom_bbros() {
		ROM_REGION(2 * 0x30000, REGION_CPU1, 0);
		/*
		 * 192k for code + 192k for decrypted opcodes
		 */
		ROM_LOAD("bb6.bin", 0x00000, 0x08000, 0xa3041ca4);
		ROM_LOAD("bb7.bin", 0x10000, 0x20000, 0x09231c68);

		ROM_REGION(0x100000, REGION_GFX1, ROMREGION_DISPOSE);
		ROM_LOAD("bb2.bin", 0x000000, 0x20000, 0x62f29992); /* chars */
		ROM_LOAD("bb3.bin", 0x020000, 0x20000, 0x79a8ed08);
		/* 40000-7ffff empty */
		ROM_LOAD("bb4.bin", 0x080000, 0x20000, 0xf705aa89);
		ROM_LOAD("bb5.bin", 0x0a0000, 0x20000, 0x2fb3db6c);
		/* c0000-fffff empty */

		ROM_REGION(0x040000, REGION_GFX2, ROMREGION_DISPOSE);
		ROM_LOAD("bb10.bin", 0x000000, 0x20000, 0xfdba4f6e); /* sprites */
		ROM_LOAD("bb9.bin", 0x020000, 0x20000, 0x39f47a63);

		//         ROM_REGION( 0x80000, REGION_SOUND1, 0 ); /* OKIM */
		//         ROM_LOAD( "bb1.bin", 0x00000, 0x20000, 0xc52e5b8e );
		return true;
	}

	public boolean rom_pompingw() {
		ROM_REGION(2 * 0x30000, REGION_CPU1, 0);
		/*
		 * 192k for code + 192k for decrypted opcodes
		 */
		ROM_LOAD("pwj_06.11h", 0x00000, 0x08000, 0x4a0a6426);
		ROM_LOAD("pwj_07.13h", 0x10000, 0x20000, 0xa9402420);

		ROM_REGION(0x100000, REGION_GFX1, ROMREGION_DISPOSE);
		ROM_LOAD("pw_02.1e", 0x000000, 0x20000, 0x4b5992e4); /* chars */
		ROM_LOAD("bb3.bin", 0x020000, 0x20000, 0x79a8ed08);
		/* 40000-7ffff empty */
		ROM_LOAD("pwj_04.1g", 0x080000, 0x20000, 0x01e49081);
		ROM_LOAD("bb5.bin", 0x0a0000, 0x20000, 0x2fb3db6c);
		/* c0000-fffff empty */

		ROM_REGION(0x040000, REGION_GFX2, ROMREGION_DISPOSE);
		ROM_LOAD("bb10.bin", 0x000000, 0x20000, 0xfdba4f6e); /* sprites */
		ROM_LOAD("bb9.bin", 0x020000, 0x20000, 0x39f47a63);

		//         ROM_REGION( 0x80000, REGION_SOUND1, 0 ); /* OKIM */
		//         ROM_LOAD( "bb1.bin", 0x00000, 0x20000, 0xc52e5b8e );
		return true;
	}

	public boolean rom_cworld() {
		ROM_REGION(2 * 0x50000, REGION_CPU1, 0);
		/*
		 * 320k for code + 320k for decrypted opcodes
		 */
		ROM_LOAD("cw05.bin", 0x00000, 0x08000, 0xd3c1723d);
		ROM_LOAD("cw06.bin", 0x10000, 0x20000, 0xd71ed4a3);
		ROM_LOAD("cw07.bin", 0x30000, 0x20000, 0xd419ce08);

		ROM_REGION(0x100000, REGION_GFX1, ROMREGION_DISPOSE);
		ROM_LOAD("cw08.bin", 0x000000, 0x20000, 0x6c80da3c); /* chars */
		ROM_LOAD("cw09.bin", 0x020000, 0x20000, 0x7607da71);
		ROM_LOAD("cw10.bin", 0x040000, 0x20000, 0x6f0e639f);
		ROM_LOAD("cw11.bin", 0x060000, 0x20000, 0x130bd7c0);
		ROM_LOAD("cw18.bin", 0x080000, 0x20000, 0xbe6ee0c9);
		ROM_LOAD("cw19.bin", 0x0a0000, 0x20000, 0x51fc5532);
		ROM_LOAD("cw20.bin", 0x0c0000, 0x20000, 0x58381d58);
		ROM_LOAD("cw21.bin", 0x0e0000, 0x20000, 0x910cc753);

		ROM_REGION(0x040000, REGION_GFX2, ROMREGION_DISPOSE);
		ROM_LOAD("cw16.bin", 0x000000, 0x20000, 0xf90217d1); /* sprites */
		ROM_LOAD("cw17.bin", 0x020000, 0x20000, 0xc953c702);

		//         ROM_REGION( 0x80000, REGION_SOUND1, 0 ); /* OKIM */
		//         ROM_LOAD( "cw01.bin", 0x00000, 0x20000, 0xf4368f5b );
		return true;
	}

	public boolean rom_hatena() {
		ROM_REGION(2 * 0x50000, REGION_CPU1, 0);
		/*
		 * 320k for code + 320k for decrypted opcodes
		 */
		ROM_LOAD("q2-05.rom", 0x00000, 0x08000, 0x66c9e1da);
		ROM_LOAD("q2-06.rom", 0x10000, 0x20000, 0x5fc39916);
		ROM_LOAD("q2-07.rom", 0x30000, 0x20000, 0xec6d5e5e);

		ROM_REGION(0x100000, REGION_GFX1, ROMREGION_DISPOSE);
		ROM_LOAD("q2-08.rom", 0x000000, 0x20000, 0x6c80da3c); /* chars */
		ROM_LOAD("q2-09.rom", 0x020000, 0x20000, 0xabe3e15c);
		ROM_LOAD("q2-10.rom", 0x040000, 0x20000, 0x6963450d);
		ROM_LOAD("q2-11.rom", 0x060000, 0x20000, 0x1e319fa2);
		ROM_LOAD("q2-18.rom", 0x080000, 0x20000, 0xbe6ee0c9);
		ROM_LOAD("q2-19.rom", 0x0a0000, 0x20000, 0x70300445);
		ROM_LOAD("q2-20.rom", 0x0c0000, 0x20000, 0x21a6ff42);
		ROM_LOAD("q2-21.rom", 0x0e0000, 0x20000, 0x076280c9);

		ROM_REGION(0x040000, REGION_GFX2, ROMREGION_DISPOSE);
		ROM_LOAD("q2-16.rom", 0x000000, 0x20000, 0xec19b2f0); /* sprites */
		ROM_LOAD("q2-17.rom", 0x020000, 0x20000, 0xecd69d92);

		//         ROM_REGION( 0x80000, REGION_SOUND1, 0 ); /* OKIM */
		//         ROM_LOAD( "q2-01.rom", 0x00000, 0x20000, 0x149e7a89 );
		return true;
	}

	public boolean rom_spang() {
		ROM_REGION(2 * 0x50000, REGION_CPU1, 0);
		/*
		 * 320k for code + 320k for decrypted opcodes
		 */
		ROM_LOAD("spe_06.rom", 0x00000, 0x08000, 0x1af106fb);
		ROM_LOAD("spe_07.rom", 0x10000, 0x20000, 0x208b5f54);
		ROM_LOAD("spe_08.rom", 0x30000, 0x20000, 0x2bc03ade);

		ROM_REGION(0x100000, REGION_GFX1, ROMREGION_DISPOSE);
		ROM_LOAD("spe_02.rom", 0x000000, 0x20000, 0x63c9dfd2); /* chars */
		ROM_LOAD("03.f2", 0x020000, 0x20000, 0x3ae28bc1);
		/* 40000-7ffff empty */
		ROM_LOAD("spe_04.rom", 0x080000, 0x20000, 0x9d7b225b);
		ROM_LOAD("05.g2", 0x0a0000, 0x20000, 0x4a060884);
		/* c0000-fffff empty */

		ROM_REGION(0x040000, REGION_GFX2, ROMREGION_DISPOSE);
		ROM_LOAD("spe_10.rom", 0x000000, 0x20000, 0xeedd0ade); /* sprites */
		ROM_LOAD("spe_09.rom", 0x020000, 0x20000, 0x04b41b75);

		//         ROM_REGION( 0x80000, REGION_SOUND1, 0 ); /* OKIM */
		//         ROM_LOAD( "spe_01.rom", 0x00000, 0x20000, 0x2d19c133 );
		return true;
	}

	public boolean rom_sbbros() {
		ROM_REGION(2 * 0x50000, REGION_CPU1, 0);
		/*
		 * 320k for code + 320k for decrypted opcodes
		 */
		ROM_LOAD("06.j12", 0x00000, 0x08000, 0x292eee6a);
		ROM_LOAD("07.j13", 0x10000, 0x20000, 0xf46b698d);
		ROM_LOAD("08.j14", 0x30000, 0x20000, 0xa75e7fbe);

		ROM_REGION(0x100000, REGION_GFX1, ROMREGION_DISPOSE);
		ROM_LOAD("02.f1", 0x000000, 0x20000, 0x0c22ffc6); /* chars */
		ROM_LOAD("03.f2", 0x020000, 0x20000, 0x3ae28bc1);
		/* 40000-7ffff empty */
		ROM_LOAD("04.g2", 0x080000, 0x20000, 0xbb3dee5b);
		ROM_LOAD("05.g2", 0x0a0000, 0x20000, 0x4a060884);
		/* c0000-fffff empty */

		ROM_REGION(0x040000, REGION_GFX2, ROMREGION_DISPOSE);
		ROM_LOAD("10.l2", 0x000000, 0x20000, 0xd6675d8f); /* sprites */
		ROM_LOAD("09.l1", 0x020000, 0x20000, 0x8f678bc8);

		//         ROM_REGION( 0x80000, REGION_SOUND1, 0 ); /* OKIM */
		//         ROM_LOAD( "01.d1", 0x00000, 0x20000, 0xb96ea126 );
		return true;
	}

	public boolean rom_marukin() {
		ROM_REGION(2 * 0x30000, REGION_CPU1, 0);
		/*
		 * 192k for code + 192k for decrypted opcodes
		 */
		ROM_LOAD("mg3-01.9d", 0x00000, 0x08000, 0x04357973);
		ROM_LOAD("mg3-02.10d", 0x10000, 0x20000, 0x50d08da0);

		ROM_REGION(0x200000, REGION_GFX1, ROMREGION_DISPOSE);
		ROM_LOAD("mg3-a.3k", 0x000000, 0x80000, 0x420f1de7); /* chars */
		ROM_LOAD("mg3-b.4k", 0x080000, 0x80000, 0xd8de13fa);
		ROM_LOAD("mg3-c.6k", 0x100000, 0x80000, 0xfbeb66e8);
		ROM_LOAD("mg3-d.7k", 0x180000, 0x80000, 0x8f6bd831);

		ROM_REGION(0x040000, REGION_GFX2, ROMREGION_DISPOSE);
		ROM_LOAD("mg3-05.2g", 0x000000, 0x20000, 0x7a738d2d); /* sprites */
		ROM_LOAD("mg3-04.1g", 0x020000, 0x20000, 0x56f30515);

		//         ROM_REGION( 0x80000, REGION_SOUND1, 0 ); /* OKIM */
		//         ROM_LOAD( "mg3-e.1d", 0x00000, 0x80000, 0x106c2fa9 ); /* banked */
		return true;
	}

	private boolean rom_qtonol() {
		ROM_REGION(2 * 0x50000, REGION_CPU1, 0);
		/*
		 * 320k for code + 320k for decrypted opcodes
		 */
		ROM_LOAD("q3-05.rom", 0x00000, 0x08000, 0x1dd0a344);
		ROM_LOAD("q3-06.rom", 0x10000, 0x20000, 0xbd6a2110);
		ROM_LOAD("q3-07.rom", 0x30000, 0x20000, 0x61e53c4f);

		ROM_REGION(0x100000, REGION_GFX1, ROMREGION_DISPOSE);
		ROM_LOAD("q3-08.rom", 0x000000, 0x20000, 0x1533b978); /* chars */
		ROM_LOAD("q3-09.rom", 0x020000, 0x20000, 0xa32db2f2);
		ROM_LOAD("q3-10.rom", 0x040000, 0x20000, 0xed681aa8);
		ROM_LOAD("q3-11.rom", 0x060000, 0x20000, 0x38b2fd10);
		ROM_LOAD("q3-18.rom", 0x080000, 0x20000, 0x9e4292ac);
		ROM_LOAD("q3-19.rom", 0x0a0000, 0x20000, 0xb7f6d40f);
		ROM_LOAD("q3-20.rom", 0x0c0000, 0x20000, 0x6cd7f38d);
		ROM_LOAD("q3-21.rom", 0x0e0000, 0x20000, 0xb4aa6b4b);

		ROM_REGION(0x040000, REGION_GFX2, ROMREGION_DISPOSE);
		ROM_LOAD("q3-16.rom", 0x000000, 0x20000, 0x863d6836); /* sprites */
		ROM_LOAD("q3-17.rom", 0x020000, 0x20000, 0x459bf59c);

		//         ROM_REGION( 0x80000, REGION_SOUND1, 0 ); /* OKIM */
		//         ROM_LOAD( "q3-01.rom", 0x00000, 0x20000, 0x6c1be591 );
		return true;
	}

	public boolean rom_qsangoku() {
		ROM_REGION(2 * 0x50000, REGION_CPU1, 0);
		/*
		 * 320k for code + 320k for decrypted opcodes
		 */
		ROM_LOAD("q4-05c.rom", 0x00000, 0x08000, 0xe1d010b4);
		ROM_LOAD("q4-06.rom", 0x10000, 0x20000, 0xa0301849);
		ROM_LOAD("q4-07.rom", 0x30000, 0x20000, 0x2941ef5b);

		ROM_REGION(0x100000, REGION_GFX1, ROMREGION_DISPOSE);
		ROM_LOAD("q4-08.rom", 0x000000, 0x20000, 0xdc84c6cb); /* chars */
		ROM_LOAD("q4-09.rom", 0x020000, 0x20000, 0xcbb6234c);
		ROM_LOAD("q4-10.rom", 0x040000, 0x20000, 0xc20a27a8);
		ROM_LOAD("q4-11.rom", 0x060000, 0x20000, 0x4ff66aed);
		ROM_LOAD("q4-18.rom", 0x080000, 0x20000, 0xca3acea5);
		ROM_LOAD("q4-19.rom", 0x0a0000, 0x20000, 0x1fd92b7d);
		ROM_LOAD("q4-20.rom", 0x0c0000, 0x20000, 0xb02dc6a1);
		ROM_LOAD("q4-21.rom", 0x0e0000, 0x20000, 0x432b1dc1);

		ROM_REGION(0x040000, REGION_GFX2, ROMREGION_DISPOSE);
		ROM_LOAD("q4-16.rom", 0x000000, 0x20000, 0x77342320); /* sprites */
		ROM_LOAD("q4-17.rom", 0x020000, 0x20000, 0x1275c436);

		ROM_REGION(0x80000, REGION_SOUND1, 0); /* OKIM */
		ROM_LOAD("q4-01.rom", 0x00000, 0x20000, 0x5d0d07d8);
		return true;
	}

	public boolean rom_block() {
		ROM_REGION(2 * 0x50000, REGION_CPU1, 0);
		/*
		 * 320k for code + 320k for decrypted opcodes
		 */
		ROM_LOAD("ble_05.bin", 0x00000, 0x08000, 0xfa2a4536);
		ROM_LOAD("ble_06.bin", 0x10000, 0x20000, 0x58a77402);
		ROM_LOAD("ble_07.rom", 0x30000, 0x20000, 0x1d114f13);

		ROM_REGION(0x100000, REGION_GFX1, ROMREGION_DISPOSE);
		ROM_LOAD("bl_08.rom", 0x000000, 0x20000, 0xaa0f4ff1); /* chars */
		ROM_CONTINUE(0x040000, 0x20000); /* chars */

		ROM_LOAD("bl_09.rom", 0x020000, 0x20000, 0x6fa8c186);
		ROM_CONTINUE(0x060000, 0x20000);

		ROM_LOAD("bl_18.rom", 0x080000, 0x20000, 0xc0acafaf);
		ROM_CONTINUE(0x0c0000, 0x20000);

		ROM_LOAD("bl_19.rom", 0x0a0000, 0x20000, 0x1ae942f5);
		ROM_CONTINUE(0x0e0000, 0x20000);

		ROM_REGION(0x040000, REGION_GFX2, ROMREGION_DISPOSE);
		ROM_LOAD("bl_16.rom", 0x000000, 0x20000, 0xfadcaff7); /* sprites */
		ROM_LOAD("bl_17.rom", 0x020000, 0x20000, 0x5f8cab42);

		ROM_REGION(0x80000, REGION_SOUND1, 0); /* OKIM */
		ROM_LOAD("bl_01.rom", 0x00000, 0x20000, 0xc2ec2abb);
		return true;
	}

	public boolean rom_blocka() {
		ROM_REGION(2 * 0x50000, REGION_CPU1, 0);
		/*
		 * 320k for code + 320k for decrypted opcodes
		 */
		ROM_LOAD("ble_05.rom", 0x00000, 0x08000, 0xc12e7f4c);
		ROM_LOAD("ble_06.rom", 0x10000, 0x20000, 0xcdb13d55);
		ROM_LOAD("ble_07.rom", 0x30000, 0x20000, 0x1d114f13);

		ROM_REGION(0x100000, REGION_GFX1, ROMREGION_DISPOSE);
		ROM_LOAD("bl_08.rom", 0x000000, 0x20000, 0xaa0f4ff1); /* chars */
		ROM_CONTINUE(0x040000, 0x20000); /* chars */

		ROM_LOAD("bl_09.rom", 0x020000, 0x20000, 0x6fa8c186);
		ROM_CONTINUE(0x060000, 0x20000);

		ROM_LOAD("bl_18.rom", 0x080000, 0x20000, 0xc0acafaf);
		ROM_CONTINUE(0x0c0000, 0x20000);

		ROM_LOAD("bl_19.rom", 0x0a0000, 0x20000, 0x1ae942f5);
		ROM_CONTINUE(0x0e0000, 0x20000);

		ROM_REGION(0x040000, REGION_GFX2, ROMREGION_DISPOSE);
		ROM_LOAD("bl_16.rom", 0x000000, 0x20000, 0xfadcaff7); /* sprites */
		ROM_LOAD("bl_17.rom", 0x020000, 0x20000, 0x5f8cab42);

		ROM_REGION(0x80000, REGION_SOUND1, 0); /* OKIM */
		ROM_LOAD("bl_01.rom", 0x00000, 0x20000, 0xc2ec2abb);
		return true;
	}

	public boolean rom_blockj() {
		ROM_REGION(2 * 0x50000, REGION_CPU1, 0);
		/*
		 * 320k for code + 320k for decrypted opcodes
		 */
		ROM_LOAD("blj_05.rom", 0x00000, 0x08000, 0x3b55969a);
		ROM_LOAD("ble_06.rom", 0x10000, 0x20000, 0xcdb13d55);
		ROM_LOAD("blj_07.rom", 0x30000, 0x20000, 0x1723883c);

		ROM_REGION(0x100000, REGION_GFX1, ROMREGION_DISPOSE);
		ROM_LOAD("bl_08.rom", 0x000000, 0x20000, 0xaa0f4ff1); /* chars */
		ROM_LOAD("bl_08.rom", 0x040000, 0x20000, 0xaa0f4ff1); /* chars */
		ROM_LOAD("bl_09.rom", 0x020000, 0x20000, 0x6fa8c186);
		ROM_LOAD("bl_09.rom", 0x060000, 0x20000, 0x6fa8c186);
		ROM_LOAD("bl_18.rom", 0x080000, 0x20000, 0xc0acafaf);
		ROM_LOAD("bl_18.rom", 0x0c0000, 0x20000, 0xc0acafaf);
		ROM_LOAD("bl_19.rom", 0x0a0000, 0x20000, 0x1ae942f5);
		ROM_LOAD("bl_19.rom", 0x0e0000, 0x20000, 0x1ae942f5);

		ROM_REGION(0x040000, REGION_GFX2, ROMREGION_DISPOSE);
		ROM_LOAD("bl_16.rom", 0x000000, 0x20000, 0xfadcaff7); /* sprites */
		ROM_LOAD("bl_17.rom", 0x020000, 0x20000, 0x5f8cab42);

		ROM_REGION(0x80000, REGION_SOUND1, 0); /* OKIM */
		ROM_LOAD("bl_01.rom", 0x00000, 0x20000, 0xc2ec2abb);
		return true;
	}

	public boolean rom_blockbl() {
		ROM_REGION(2 * 0x50000, REGION_CPU1, 0);
		/*
		 * 320k for code + 320k for decrypted opcodes
		 */
		ROM_LOAD("m7.l6", 0x50000, 0x08000, 0x3b576fd9);
		/*
		 * Decrypted opcode + data
		 */
		ROM_CONTINUE(0x00000, 0x08000);
		ROM_LOAD("m5.l3", 0x60000, 0x20000, 0x7c988bb7);
		/*
		 * Decrypted opcode + data
		 */
		ROM_CONTINUE(0x10000, 0x20000);
		ROM_LOAD("m6.l5", 0x30000, 0x20000, 0x5768d8eb); /* Decrypted data */

		ROM_REGION(0x100000, REGION_GFX1, ROMREGION_DISPOSE);
		ROM_LOAD("m12.o10", 0x000000, 0x20000, 0x963154d9); /* chars */
		ROM_LOAD("m12.o10", 0x040000, 0x20000, 0x963154d9); /* chars */
		ROM_LOAD("m13.o14", 0x020000, 0x20000, 0x069480bb);
		ROM_LOAD("m13.o14", 0x060000, 0x20000, 0x069480bb);
		ROM_LOAD("m4.j17", 0x080000, 0x20000, 0x9e3b6f4f);
		ROM_LOAD("m4.j17", 0x0c0000, 0x20000, 0x9e3b6f4f);
		ROM_LOAD("m3.j20", 0x0a0000, 0x20000, 0x629d58fe);
		ROM_LOAD("m3.j20", 0x0e0000, 0x20000, 0x629d58fe);

		ROM_REGION(0x040000, REGION_GFX2, ROMREGION_DISPOSE);
		ROM_LOAD("m11.o7", 0x000000, 0x10000, 0x255180a5); /* sprites */
		ROM_LOAD("m10.o5", 0x010000, 0x10000, 0x3201c088);
		ROM_LOAD("m9.o3", 0x020000, 0x10000, 0x29357fe4);
		ROM_LOAD("m8.o2", 0x030000, 0x10000, 0xabd665d1);

		ROM_REGION(0x80000, REGION_SOUND1, 0); /* OKIM */
		ROM_LOAD("bl_01.rom", 0x00000, 0x20000, 0xc2ec2abb);
		return true;
	}

	void bootleg_decode() {
		int[] rom = memory_region(MAMEConstants.REGION_CPU1);
		int diff = memory_region_length(MAMEConstants.REGION_CPU1) / 2;

		//	memory_set_opcode_base(0,rom+diff);
	}

	public InitHandler init_dokaben() {
		return new Init_dokaben();
	}
	public class Init_dokaben implements InitHandler {
		public void init() {

			input_type = 0;
			nvram_size = 0;
			m.mgakuen2_decode();
		}
	}

	public InitHandler init_pang() {
		return new Init_pang();
	}
	public class Init_pang implements InitHandler {
		public void init() {
			input_type = 0;
			nvram_size = 0;
			//m.pang_decode();
		}
	}

	public InitHandler init_pangb() {
		return new Init_pangb();
	}
	public class Init_pangb implements InitHandler {
		public void init() {
			input_type = 0;
			nvram_size = 0;
			bootleg_decode();
		}
	}

	public InitHandler init_cworld() {
		return new Init_cworld();
	}
	public class Init_cworld implements InitHandler {
		public void init() {
			input_type = 0;
			nvram_size = 0;
			m.cworld_decode();
		}
	}

	public InitHandler init_hatena() {
		return new Init_hatena();
	}
	public class Init_hatena implements InitHandler {
		public void init() {
			input_type = 0;
			nvram_size = 0;
			m.hatena_decode();
		}
	}

	public InitHandler init_spang() {
		return new Init_spang();
	}
	public class Init_spang implements InitHandler {
		public void init() {
			input_type = 3;
			nvram_size = 0x80;
			//   nvram = &memory_region(REGION_CPU1)[0xe000]; // NVRAM
			m.spang_decode();
		}
	}

	public InitHandler init_sbbros() {
		return new Init_sbbros();
	}
	public class Init_sbbros implements InitHandler {
		public void init() {
			input_type = 3;
			nvram_size = 0x80;
			//   nvram = &memory_region(REGION_CPU1)[0xe000]; // NVRAM
			m.sbbros_decode();
		}
	}

	public InitHandler init_qtono1() {
		return new Init_qtono1();
	}
	public class Init_qtono1 implements InitHandler {
		public void init() {
			input_type = 0;
			nvram_size = 0;
			m.qtono1_decode();
		}
	}

	public InitHandler init_qsangoku() {
		return new Init_qsangoku();
	}
	public class Init_qsangoku implements InitHandler {
		public void init() {
			input_type = 0;
			nvram_size = 0;
			m.qsangoku_decode();
		}
	}

	public InitHandler init_mgakuen() {
		return new Init_mgakuen();
	}
	public class Init_mgakuen implements InitHandler {
		public void init() {
			input_type = 1;
		}
	}

	public InitHandler init_mgakuen2() {
		return new Init_mgakuen2();
	}
	public class Init_mgakuen2 implements InitHandler {
		public void init() {
			input_type = 1;
			nvram_size = 0;
			m.mgakuen2_decode();
		}
	}

	public InitHandler init_marukin() {
		return new Init_marukin();
	}
	public class Init_marukin implements InitHandler {
		public void init() {
			input_type = 1;
			nvram_size = 0;
			m.marukin_decode();
		}
	}

	public InitHandler init_block() {
		return new Init_block();
	}
	public class Init_block implements InitHandler {
		public void init() {
			input_type = 2;
			nvram_size = 0x80;
			//   nvram = &memory_region(REGION_CPU1)[0xff80]; // NVRAM
			m.block_decode();
		}
	}

	public InitHandler init_blockbl() {
		return new Init_blockbl();
	}
	public class Init_blockbl implements InitHandler {
		public void init() {
			input_type = 2;
			nvram_size = 0x80;
			//   nvram = &memory_region(REGION_CPU1)[0xff80]; // NVRAM
			bootleg_decode();
		}
	}
	public Machine getMachine(URL url, String name) {

		super.getMachine(url, name);
		super.setVideoEmulator(v);
		
		//v.pang_objram = pang_o

		if (name.equals("mgakuen"))
			GAME(
				1988,
				rom_mgakuen(),
				0,
				mdrv_mgakuen(),
				ipt_mgakuen(),
				init_mgakuen(),
				ROT0,
				"Yuga",
				"Mahjong Gakuen");
		else if (name.equals("mgakuen2"))
			GAME(
				1989,
				rom_mgakuen2(),
				0,
				mdrv_marukin(),
				ipt_marukin(),
				init_mgakuen2(),
				ROT0,
				"Face",
				"Mahjong Gakuen 2 Gakuen-chou no Fukushuu");
		else if (name.equals("pkladies"))
			GAME(
				1989,
				rom_pkladies(),
				0,
				mdrv_marukin(),
				ipt_pkladies(),
				init_mgakuen2(),
				ROT0,
				"Mitchell",
				"Poker Ladies");
		else if (name.equals("pkladiel"))
			GAME(
				1989,
				rom_pkladiel(),
				"pkladies",
				mdrv_marukin(),
				ipt_pkladies(),
				init_mgakuen2(),
				ROT0,
				"Leprechaun",
				"Poker Ladies (Leprechaun)");
		else if (name.equals("dokaben"))
			GAME(
				1989,
				rom_dokaben(),
				0,
				mdrv_pang(),
				ipt_pang(),
				init_dokaben(),
				ROT0,
				"Capcom",
				"Dokaben (Japan)");
		else if (name.equals("pang")) {
			GAME(
				1989,
				rom_pang(),
				0,
				mdrv_pang(),
				ipt_pang(),
				init_pang(),
				ROT0,
				"Mitchell",
				"Pang (World)");
			m.pang_decode();
		} else if (name.equals("pangb"))
			GAME(
				1989,
				rom_pangb(),
				"pang",
				mdrv_pang(),
				ipt_pang(),
				init_pangb(),
				ROT0,
				"bootleg",
				"Pang (bootleg)");
		else if (name.equals("bbros"))
			GAME(
				1989,
				rom_bbros(),
				"pang",
				mdrv_pang(),
				ipt_pang(),
				init_pang(),
				ROT0,
				"Capcom",
				"Buster Bros (US)");
		else if (name.equals("pompingw"))
			GAME(
				1989,
				rom_pompingw(),
				"pang",
				mdrv_pang(),
				ipt_pang(),
				init_pang(),
				ROT0,
				"Mitchell",
				"Pomping World (Japan)");
		else if (name.equals("cworld"))
			GAME(
				1989,
				rom_cworld(),
				0,
				mdrv_pang(),
				ipt_qtono1(),
				init_cworld(),
				ROT0,
				"Capcom",
				"Capcom World (Japan)");
		else if (name.equals("hatena"))
			GAME(
				1990,
				rom_hatena(),
				0,
				mdrv_pang(),
				ipt_qtono1(),
				init_hatena(),
				ROT0,
				"Capcom",
				"Adventure Quiz 2 Hatena Hatena no Dai-Bouken (Japan)");
		else if (name.equals("spang"))
			GAME(
				1990,
				rom_spang(),
				0,
				mdrv_pang(),
				ipt_pang(),
				init_spang(),
				ROT0,
				"Mitchell",
				"Super Pang (World)");
		else if (name.equals("sbbros"))
			GAME(
				1990,
				rom_sbbros(),
				"spang",
				mdrv_pang(),
				ipt_pang(),
				init_sbbros(),
				ROT0,
				"Mitchell + Capcom",
				"Super Buster Bros (US)");
		else if (name.equals("marukin"))
			GAME(
				1990,
				rom_marukin(),
				0,
				mdrv_marukin(),
				ipt_marukin(),
				init_marukin(),
				ROT0,
				"Yuga",
				"Super Marukin-Ban");
		else if (name.equals("qtonol"))
			GAME(
				1991,
				rom_qtonol(),
				0,
				mdrv_pang(),
				ipt_qtono1(),
				init_qtono1(),
				ROT0,
				"Capcom",
				"Quiz Tonosama no Yabou (Japan)");
		else if (name.equals("qsangoku"))
			GAME(
				1991,
				rom_qsangoku(),
				0,
				mdrv_pang(),
				ipt_qtono1(),
				init_qsangoku(),
				ROT0,
				"Capcom",
				"Quiz Sangokushi (Japan)");
		else if (name.equals("block"))
			GAME(
				1991,
				rom_block(),
				0,
				mdrv_pang(),
				ipt_block(),
				init_block(),
				ROT270,
				"Capcom",
				"Block Block (World 911106 Joystick)");
		else if (name.equals("blocka"))
			GAME(
				1991,
				rom_blocka(),
				"block",
				mdrv_pang(),
				ipt_block(),
				init_block(),
				ROT270,
				"Capcom",
				"Block Block (World 910910)");
		else if (name.equals("blockj"))
			GAME(
				1991,
				rom_blockj(),
				"block",
				mdrv_pang(),
				ipt_block(),
				init_block(),
				ROT270,
				"Capcom",
				"Block Block (Japan 910910)");
		else if (name.equals("blockbl"))
			GAME(
				1991,
				rom_blockbl(),
				"block",
				mdrv_pang(),
				ipt_block(),
				init_blockbl(),
				ROT270,
				"bootleg",
				"Block Block (bootleg)");

		m.init(md);
		return (Machine) m;
	}

}