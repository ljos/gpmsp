/*
CottAGE - the Arcade Generic Emulator in Java

Java driver by Gollum, Erik Duijs
*/

/***************************************************************************
Bubble Bobble / Tokio

Main clock: XTAL = 24 MHz
Horizontal video frequency: HSYNC = XTAL/4/384 = 15.625 kHz
Video frequency: VSYNC = HSYNC/264 = 59.185606 Hz
VBlank duration: 1/VSYNC * (40/264) = 2560 us

***************************************************************************

Bubble Bobble ROM info

CPU Board
---------
           | Taito  |Romstar | ?????  |Romstar |
           |        |        |missing |mode sel|
17  CU1    | A78-01 |   ->   |   ->   |   ->   |   protection mcu
49  PAL1   | A78-02 |   ->   |   ->   |   ->   |   address decoder
43  PAL2   | A78-03 |   ->   |   ->   |   ->   |   address decoder
12  PAL3   | A78-04 |   ->   |   ->   |   ->   |   address decoder
53  empty  |        |        |        |        |   main prg
52  ROM1   | A78-05 | A78-21 | A78-22 | A78-24 |   main prg
51  ROM2   | A78-06 |   ->   | A78-23 | A78-25 |   main prg
46  ROM4   | A78-07 |   ->   |   ->   |   ->   |   sound prg
37  ROM3   | A78-08 |   ->   |   ->   |   ->   |   sub prg

Video Board
-----------
12  ROM1   | A78-09 |   ->   |   ->   |   ->   |   gfx
13  ROM2   | A78-10 |   ->   |   ->   |   ->   |   gfx
14  ROM3   | A78-11 |   ->   |   ->   |   ->   |   gfx
15  ROM4   | A78-12 |   ->   |   ->   |   ->   |   gfx
16  ROM5   | A78-13 |   ->   |   ->   |   ->   |   gfx
17  ROM6   | A78-14 |   ->   |   ->   |   ->   |   gfx
18  empty  |        |        |        |        |   gfx
19  empty  |        |        |        |        |   gfx
30  ROM7   | A78-15 |   ->   |   ->   |   ->   |   gfx
31  ROM8   | A78-16 |   ->   |   ->   |   ->   |   gfx
32  ROM9   | A78-17 |   ->   |   ->   |   ->   |   gfx
33  ROM10  | A78-18 |   ->   |   ->   |   ->   |   gfx
34  ROM11  | A78-19 |   ->   |   ->   |   ->   |   gfx
35  ROM12  | A78-20 |   ->   |   ->   |   ->   |   gfx
36  empty  |        |        |        |        |   gfx
37  empty  |        |        |        |        |   gfx
41  ROM13  | A71-25 |   ->   |   ->   |   ->   |   video timing


Bobble Bobble memory map

driver by Chris Moore

CPU #1
0000-bfff ROM (8000-bfff is banked)
c000-dcff Graphic RAM. This contains pointers to the video RAM columns and
          to the sprites are contained in Object RAM.
dd00-dfff Object RAM (groups of four bytes: X position, code [offset in the
          Graphic RAM], Y position, gfx bank)
CPU #2
0000-7fff ROM

CPU #1 AND #2
e000-f7fe RAM
f800-f9ff Palette RAM
fc01-fdff RAM

read:
ff00      DSWA
ff01      DSWB
ff02      IN0
ff03      IN1


Service mode works only if the language switch is set to Japanese.

- The protection feature which randomizes the EXTEND letters in the original
  version is not emulated properly.

***************************************************************************

Tokio memory map

CPU 1
0000-bfff ROM (8000-bfff is banked)
c000-dcff Graphic RAM. This contains pointers to the video RAM columns and
          to the sprites contained in Object RAM.
dd00-dfff Object RAM (groups of four bytes: X position, code [offset in the
          Graphic RAM], Y position, gfx bank)
e000-f7ff RAM (Shared)
f800-f9ff Palette RAM

fa03 - DSW0
fa04 - DSW1
fa05 - Coins
fa06 - ControlsConfig Player 1
fa07 - ControlsConfig Player 1

CPU 2
0000-7fff ROM
8000-97ff RAM (Shared)

CPU 3
0000-7fff ROM
8000-8fff RAM


  Here goes a list of known deficiencies of our drivers:

  - The bootleg romset is functional. The original one hangs at
    the title screen (protection).

  - Sound support is probably incomplete. There are a couple of unknown
    accesses done by the CPU, including to the YM2203 I/O ports. At the
	very least, there should be some filters.

  - "fake-r" routine make the "original" roms to restart the game after
    some seconds.

    Well, we know very little about the 0xFE00 address. It could be
    some watchdog or a synchronization timer.

    I remember scanning the main CPU code to find how it was
    used on the bootleg set. Then I just figured out a constant value
    that made the game run (it hang if just set unhandled, that is,
    returning zero).

    Maybe the solution is to patch the bootleg ROMs to skip some tests
    at this location (I remember some of them being in the
    initialization routine of the main CPU).

                       Marcelo de G. Malheiros <malheiro@dca.fee.unicamp.br>
                                                                   1998.9.25

***************************************************************************/

package cottage.drivers;

import java.net.URL;

import jef.cpu.Cpu;
import jef.machine.Machine;
import jef.map.InitHandler;
import jef.map.InterruptHandler;
import jef.map.ReadHandler;
import jef.map.WriteHandler;
import jef.sound.chip.YM2203;
import jef.sound.chip.fm.FMIRQHandler;
import jef.video.GfxManager;
import jef.video.Vh_refresh;

import cottage.mame.MAMEDriver;

public class Bublbobl extends MAMEDriver {

cottage.vidhrdw.Bublbobl v = new cottage.vidhrdw.Bublbobl();
int[] bublbobl_objectram = v.Fbublbobl_objectram;
int[] bublbobl_objectram_size = v.Fbublbobl_objectram_size;
Vh_refresh bublbobl_vu = (Vh_refresh)v;

jef.machine.BasicMachine m = new jef.machine.BasicMachine();
InterruptHandler irq0_line_hold = m.irq0_line_hold();
int bublbobl_sharedram1[] = new int[0x1800];
int bublbobl_sharedram2[] = new int[0x300];

public ReadHandler bublbobl_sharedram1_r(int ofs) { return new Bublbobl_sharedram1_r(ofs); };
public ReadHandler bublbobl_sharedram2_r(int ofs) { return new Bublbobl_sharedram2_r(ofs); };
public WriteHandler bublbobl_sharedram1_w(int ofs) { return new Bublbobl_sharedram1_w(ofs); };
public WriteHandler bublbobl_sharedram2_w(int ofs) { return new Bublbobl_sharedram2_w(ofs); };
WriteHandler bublbobl_bankswitch_w = (WriteHandler) new Bublbobl_bankswitch_w();
WriteHandler tokio_bankswitch_w = (WriteHandler) new Tokio_bankswitch_w();
WriteHandler tokio_videoctrl_w = (WriteHandler) new Tokio_videoctrl_w();
WriteHandler bublbobl_nmitrigger_w = (WriteHandler) new Bublbobl_nmitrigger_w();
ReadHandler tokio_fake_r = (ReadHandler) new Tokio_fake_r();

YM2203 ym = new YM2203(1, 3000000, null, new SndIrq());

public class SndIrq implements FMIRQHandler {
	
	int curState = 0;
	/* (non-Javadoc)
	 * @see jef.sound.chip.fm.FMIRQHandler#irq(int, int)
	 */
	public void irq(int numChip, int irqState) {
		//if (curState == 0 && irqState == 1) {
		//System.out.println("IRQ");
		//cpu_set_irq_line(2,0,irq ? ASSERT_LINE : CLEAR_LINE);
		m.cb[2].interrupt(Cpu.INTERRUPT_TYPE_NMI,true);
		//}

		curState = irqState;	
	}
}

boolean sound_nmi_enable,pending_nmi;

void nmi_callback(int param)
{
	if (sound_nmi_enable) m.cb[2].interrupt(Cpu.INTERRUPT_TYPE_NMI,true);
	else pending_nmi = true;
}

public class Bublbobl_sound_command_w implements WriteHandler {
	public void write(int offset,int data)
{
	soundlatch_w(offset,data);
	//timer_set(TIME_NOW,data,nmi_callback);
	nmi_callback(data);
	
}
}

public class Bublbobl_sh_nmi_disable_w implements WriteHandler {
	public void write(int offset,int data)
{
	sound_nmi_enable = false;
}
}

public class Bublbobl_sh_nmi_enable_w implements WriteHandler {
	public void write(int offset,int data)
{
	sound_nmi_enable = true;
	if (pending_nmi)	/* probably wrong but commands go lost otherwise */
	{
		cpu_cause_interrupt(2,Cpu.INTERRUPT_TYPE_NMI);
		pending_nmi = false;
	}
}
}

public class Bublbobl_sharedram1_r implements ReadHandler {
	public int startofs = 0;
	public Bublbobl_sharedram1_r(int ofs) { startofs = ofs; }
	public int read(int offset) {
		return bublbobl_sharedram1[offset-startofs];
	}
}

public class Bublbobl_sharedram2_r implements ReadHandler {
	public int startofs = 0;
	public Bublbobl_sharedram2_r(int ofs) { startofs = ofs; }
	public int read(int offset) {
		return bublbobl_sharedram2[offset-startofs];
	}
}

public class Bublbobl_sharedram1_w implements WriteHandler {
	public int startofs = 0;
	public Bublbobl_sharedram1_w(int ofs) { startofs = ofs; }
	public void write(int offset, int data) {
		bublbobl_sharedram1[offset-startofs] = data;
	}
}

public class Bublbobl_sharedram2_w implements WriteHandler {
	public int startofs = 0;
	public Bublbobl_sharedram2_w(int ofs) { startofs = ofs; }
	public void write(int offset, int data) {
		bublbobl_sharedram2[offset-startofs] = data;
	}
}

public class Bublbobl_bankswitch_w implements WriteHandler {
	public void write(int offset, int data) {
		int[] ROM = memory_region(REGION_CPU1);

		/* bits 0-2 select ROM bank */
		cpu_setbank(1,0x10000 + 0x4000 * ((data ^ 4) & 7));

		/* bit 3 n.c. */

		/* bit 4 resets second Z80 */

		/* bit 5 resets mcu */

		/* bit 6 enables display */
		v.bublbobl_video_enable = data & 0x40;

		/* bit 7 flips screen */
		//flip_screen_set(data & 0x80);
	}
}

public class Tokio_bankswitch_w implements WriteHandler {
	public void write(int offset, int data) {
		int[] ROM = memory_region(REGION_CPU1);

		/* bits 0-2 select ROM bank */
		cpu_setbank(1,0x10000 + 0x4000 * (data & 7));

		/* bits 3-7 unknown */
	}
}

public class Tokio_videoctrl_w implements WriteHandler {
	public void write(int offset, int data) {
		/* bit 7 flips screen */
		//flip_screen_set(data & 0x80);

		/* other bits unknown */
	}
}

public class Bublbobl_nmitrigger_w implements WriteHandler {
	public void write(int offset, int data) {
		m.cb[1].interrupt(1,true);
//		cpu_set_irq_line(1,IRQ_LINE_NMI,PULSE_LINE);
	}
}

public class Tokio_fake_r implements ReadHandler {
	public int read(int offset) {
		return 0xbf; /* ad-hoc value set to pass initial testing */
	}
}

private boolean boblbobl_readmem() {
	MR_START( 0x0000, 0x7fff, MRA_ROM );
	MR_ADD( 0x8000, 0xbfff, MRA_BANK1 );
	MR_ADD( 0xc000, 0xdfff, MRA_RAM );
	MR_ADD( 0xe000, 0xf7ff, bublbobl_sharedram1_r(0xe000) );
	MR_ADD( 0xf800, 0xf9ff, v.paletteram_r() );
	MR_ADD( 0xfc00, 0xfeff, bublbobl_sharedram2_r(0xfc00) );
	MR_ADD( 0xff00, 0xff00, input_port_0_r );
	MR_ADD( 0xff01, 0xff01, input_port_1_r );
	MR_ADD( 0xff02, 0xff02, input_port_2_r );
	MR_ADD( 0xff03, 0xff03, input_port_3_r );
	return true;
}

private boolean boblbobl_writemem() {
	MW_START( 0x0000, 0xbfff, MWA_ROM );
	MW_ADD( 0xc000, 0xdcff, MWA_RAM, videoram, videoram_size );
	MW_ADD( 0xdd00, 0xdfff, MWA_RAM, bublbobl_objectram, bublbobl_objectram_size );
	MW_ADD( 0xe000, 0xf7ff, bublbobl_sharedram1_w(0xe000)/*, bublbobl_sharedram1*/ );
	MW_ADD( 0xf800, 0xf9ff, v.paletteram_RRRRGGGGBBBBxxxx_swap_w(), paletteram );
	//MW_ADD( 0xfa00, 0xfa00, new Bublbobl_sound_command_w() );
	MW_ADD( 0xfa80, 0xfa80, MWA_NOP );
	MW_ADD( 0xfb00, 0xfb00, bublbobl_nmitrigger_w );	/* not used by Bubble Bobble, only by Tokio */
	MW_ADD( 0xfb40, 0xfb40, bublbobl_bankswitch_w );
	MW_ADD( 0xfc00, 0xfeff, bublbobl_sharedram2_w(0xfc00)/*, bublbobl_sharedram2*/ );
	return true;
}

private boolean bublbobl_readmem2() {
	MR_START( 0x0000, 0x7fff, MRA_ROM );
	MR_ADD( 0xe000, 0xf7ff, bublbobl_sharedram1_r(0xe000) );
	MR_ADD( 0xf800, 0xf9ff, v.paletteram_r() );
	MR_ADD( 0xfc00, 0xfeff, bublbobl_sharedram2_r(0xfc00)/*, bublbobl_sharedram2*/ );
	return true;
}

private boolean bublbobl_writemem2() {
	MW_START( 0x0000, 0x7fff, MWA_ROM );
	MW_ADD( 0xe000, 0xf7ff, bublbobl_sharedram1_w(0xe000) );
	MW_ADD( 0xf800, 0xf9ff, v.paletteram_RRRRGGGGBBBBxxxx_swap_w(), paletteram );
	MW_ADD( 0xfc00, 0xfeff, bublbobl_sharedram2_w(0xfc00)/*, bublbobl_sharedram2*/ );
	return true;
}

private boolean sound_readmem() {
	MR_START( 0x0000, 0x7fff, MRA_ROM );
	MR_ADD( 0x8000, 0x8fff, MRA_RAM );
	MR_ADD( 0x9000, 0x9000, ym.ym2203_status_port_0_r() );
	MR_ADD( 0x9001, 0x9001, ym.ym2203_read_port_0_r() );
	//MR_ADD( 0xa000, 0xa000, ym.ym3526_status_port_0_r() );
	MR_ADD( 0xb000, 0xb000, soundlatch_r );
	MR_ADD( 0xb001, 0xb001, MRA_NOP );	/* ??? */
	MR_ADD( 0xe000, 0xefff, MRA_ROM );	/* space for diagnostic ROM? */
	return true;
}

private boolean sound_writemem() {
	MW_START( 0x0000, 0x7fff, MWA_ROM );
	MW_ADD( 0x8000, 0x8fff, MWA_RAM );
	MW_ADD( 0x9000, 0x9000, ym.ym2203_control_port_0_w() );
	MW_ADD( 0x9001, 0x9001, ym.ym2203_write_port_0_w() );
	//MW_ADD( 0xa000, 0xa000, YM3526_control_port_0_w );
	//MW_ADD( 0xa001, 0xa001, YM3526_write_port_0_w );
	MW_ADD( 0xb000, 0xb000, MWA_NOP );	/* ??? */
	MW_ADD( 0xb001, 0xb001, new Bublbobl_sh_nmi_enable_w() );
	MW_ADD( 0xb002, 0xb002, new Bublbobl_sh_nmi_disable_w() );
	MW_ADD( 0xe000, 0xefff, MWA_ROM );	/* space for diagnostic ROM? */
	return true;
}

private boolean tokio_sound_readmem() {
	MR_START( 0x0000, 0x7fff, MRA_ROM );
	MR_ADD( 0x8000, 0x8fff, MRA_RAM );
	MR_ADD( 0x9000, 0x9000, soundlatch_r );
//	MR_ADD( 0x9800, 0x9800, MRA_NOP );	/* ??? */
	MR_ADD( 0xb000, 0xb000, ym.ym2203_status_port_0_r() );
	MR_ADD( 0xb001, 0xb001, ym.ym2203_read_port_0_r() );
	MR_ADD( 0xe000, 0xefff, MRA_ROM );	/* space for diagnostic ROM? */
	return true;
};

private boolean tokio_sound_writemem() {
	MW_START( 0x0000, 0x7fff, MWA_ROM );
	MW_ADD( 0x8000, 0x8fff, MWA_RAM );
//	MW_ADD( 0x9000, 0x9000, MWA_NOP );	/* ??? */
	MW_ADD( 0xa000, 0xa000, new Bublbobl_sh_nmi_disable_w() );
	MW_ADD( 0xa800, 0xa800, new Bublbobl_sh_nmi_enable_w() );
	MW_ADD( 0xb000, 0xb000, ym.ym2203_control_port_0_w() );
	MW_ADD( 0xb001, 0xb001, ym.ym2203_write_port_0_w() );
	MW_ADD( 0xe000, 0xefff, MWA_ROM );	/* space for diagnostic ROM? */
	return true;
};

private boolean tokio_readmem() {
	MR_START( 0x0000, 0x7fff, MRA_ROM );
	MR_ADD( 0x8000, 0xbfff, MRA_BANK1 );
	MR_ADD( 0xc000, 0xdfff, MRA_RAM );
	MR_ADD( 0xe000, 0xf7ff, bublbobl_sharedram1_r(0xe000) );
	MR_ADD( 0xf800, 0xf9ff, v.paletteram_r() );
	MR_ADD( 0xfa03, 0xfa03, input_port_0_r );
	MR_ADD( 0xfa04, 0xfa04, input_port_1_r );
	MR_ADD( 0xfa05, 0xfa05, input_port_2_r );
	MR_ADD( 0xfa06, 0xfa06, input_port_3_r );
	MR_ADD( 0xfa07, 0xfa07, input_port_4_r );
	MR_ADD( 0xfa08, 0xfdff, MRA_RAM ); /* CottAGE : UNMAPPED IN MAME */
	MR_ADD( 0xfe00, 0xfe00, tokio_fake_r );
	MR_ADD( 0xfe01, 0xffff, MRA_RAM ); /* CottAGE : UNMAPPED IN MAME */
	return true;
}

private boolean tokio_writemem() {
	MW_START( 0x0000, 0xbfff, MWA_ROM );
	MW_ADD( 0xc000, 0xdcff, MWA_RAM, videoram, videoram_size );
	MW_ADD( 0xdd00, 0xdfff, MWA_RAM, bublbobl_objectram, bublbobl_objectram_size );
	MW_ADD( 0xe000, 0xf7ff, bublbobl_sharedram1_w(0xe000)/*, bublbobl_sharedram1*/ );
	MW_ADD( 0xf800, 0xf9ff, v.paletteram_RRRRGGGGBBBBxxxx_swap_w(), paletteram );
	MW_ADD( 0xfa00, 0xfa00, MWA_NOP );
	MW_ADD( 0xfa01, 0xfa7f, MWA_RAM ); /* CottAGE : UNMAPPED IN MAME */
	MW_ADD( 0xfa80, 0xfa80, tokio_bankswitch_w );
	MW_ADD( 0xfa81, 0xfaff, MWA_RAM ); /* CottAGE : UNMAPPED IN MAME */
	MW_ADD( 0xfb00, 0xfb00, tokio_videoctrl_w );
	MW_ADD( 0xfb01, 0xfb7f, MWA_RAM ); /* CottAGE : UNMAPPED IN MAME */
	MW_ADD( 0xfb80, 0xfb80, bublbobl_nmitrigger_w );
	MW_ADD( 0xfb81, 0xfbff, MWA_RAM ); /* CottAGE : UNMAPPED IN MAME */
	//MW_ADD( 0xfc00, 0xfc00, bublbobl_sound_command_w );
	MW_ADD( 0xfc00, 0xfdff, MWA_RAM ); /* CottAGE : UNMAPPED IN MAME */
	MW_ADD( 0xfe00, 0xfe00, MWA_NOP ); /* ??? */
	MW_ADD( 0xfe01, 0xffff, MWA_RAM ); /* CottAGE : UNMAPPED IN MAME */
	return true;
}

private boolean tokio_readmem2() {
	MR_START( 0x0000, 0x7fff, MRA_ROM );
	MR_ADD( 0x8000, 0x97ff, bublbobl_sharedram1_r(0x8000) );
	return true;
}

private boolean tokio_writemem2() {
	MW_START( 0x0000, 0x7fff, MWA_ROM );
	MW_ADD( 0x8000, 0x97ff, bublbobl_sharedram1_w(0x8000) );
	return true;
}

private boolean ipt_boblbobl() {
	PORT_START();      /* DSW0 */
	PORT_DIPNAME( 0x01, 0x00, "Language" );
	PORT_DIPSETTING(    0x00, "English" );
	PORT_DIPSETTING(    0x01, "Japanese" );
	PORT_DIPNAME( 0x02, 0x02, DEF_STR2( Flip_Screen ) );
	PORT_DIPSETTING(    0x02, DEF_STR2( Off ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( On ) );
	PORT_SERVICE( 0x04, IP_ACTIVE_LOW );
	PORT_DIPNAME( 0x08, 0x08, DEF_STR2( Demo_Sounds ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( Off ) );
	PORT_DIPSETTING(    0x08, DEF_STR2( On ) );
	PORT_DIPNAME( 0x30, 0x30, DEF_STR2( Coin_A ) );
	PORT_DIPSETTING(    0x10, DEF_STR2( _2C_1C ) );
	PORT_DIPSETTING(    0x30, DEF_STR2( _1C_1C ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( _2C_3C ) );
	PORT_DIPSETTING(    0x20, DEF_STR2( _1C_2C ) );
	PORT_DIPNAME( 0xc0, 0xc0, DEF_STR2( Coin_B ) );
	PORT_DIPSETTING(    0x40, DEF_STR2( _2C_1C ) );
	PORT_DIPSETTING(    0xc0, DEF_STR2( _1C_1C ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( _2C_3C ) );
	PORT_DIPSETTING(    0x80, DEF_STR2( _1C_2C ) );

	PORT_START();      /* DSW1 */
	PORT_DIPNAME( 0x03, 0x03, DEF_STR2( Difficulty ) );
	PORT_DIPSETTING(    0x02, "Easy" );
	PORT_DIPSETTING(    0x03, "Medium" );
	PORT_DIPSETTING(    0x01, "Hard" );
	PORT_DIPSETTING(    0x00, "Hardest" );
	PORT_DIPNAME( 0x0c, 0x0c, DEF_STR2( Bonus_Life ) );
	PORT_DIPSETTING(    0x08, "20000 80000" );
	PORT_DIPSETTING(    0x0c, "30000 100000" );
	PORT_DIPSETTING(    0x04, "40000 200000" );
	PORT_DIPSETTING(    0x00, "50000 250000" );
	PORT_DIPNAME( 0x30, 0x30, DEF_STR2( Lives ) );
	PORT_DIPSETTING(    0x10, "1" );
	PORT_DIPSETTING(    0x00, "2" );
	PORT_DIPSETTING(    0x30, "3" );
	PORT_DIPSETTING(    0x20, "5" );
	PORT_DIPNAME( 0xc0, 0x00, "Monster Speed" );
	PORT_DIPSETTING(    0x00, "Normal" );
	PORT_DIPSETTING(    0x40, "Medium" );
	PORT_DIPSETTING(    0x80, "High" );
	PORT_DIPSETTING(    0xc0, "Very High" );

	PORT_START();      /* IN0 */
	PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT | IPF_2WAY );
	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_2WAY );
	PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_COIN2 );
	PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_COIN1 );
	PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON2 );
	PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON1 );
	PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_START1 );
	PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );

	PORT_START();      /* IN1 */
	PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT | IPF_2WAY | IPF_PLAYER2 );
	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_2WAY | IPF_PLAYER2 );
	PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_TILT ); /* ?????*/
	PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_SERVICE1 );
	PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER2 );
	PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER2 );
	PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_START2 );
	PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
	return true;
}

private boolean ipt_sboblbob() {
	PORT_START();      /* DSW0 */
	PORT_DIPNAME( 0x01, 0x00, "Game" );
	PORT_DIPSETTING(    0x01, "Bobble Bobble" );
	PORT_DIPSETTING(    0x00, "Super Bobble Bobble" );
	PORT_DIPNAME( 0x02, 0x02, DEF_STR2( Flip_Screen ) );
	PORT_DIPSETTING(    0x02, DEF_STR2( Off ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( On ) );
	PORT_SERVICE( 0x04, IP_ACTIVE_LOW );
	PORT_DIPNAME( 0x08, 0x08, DEF_STR2( Demo_Sounds ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( Off ) );
	PORT_DIPSETTING(    0x08, DEF_STR2( On ) );
	PORT_DIPNAME( 0x30, 0x30, DEF_STR2( Coin_A ) );
	PORT_DIPSETTING(    0x10, DEF_STR2( _2C_1C ) );
	PORT_DIPSETTING(    0x30, DEF_STR2( _1C_1C ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( _2C_3C ) );
	PORT_DIPSETTING(    0x20, DEF_STR2( _1C_2C ) );
	PORT_DIPNAME( 0xc0, 0xc0, DEF_STR2( Coin_B ) );
	PORT_DIPSETTING(    0x40, DEF_STR2( _2C_1C ) );
	PORT_DIPSETTING(    0xc0, DEF_STR2( _1C_1C ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( _2C_3C ) );
	PORT_DIPSETTING(    0x80, DEF_STR2( _1C_2C ) );

	PORT_START();      /* DSW1 */
	PORT_DIPNAME( 0x03, 0x03, DEF_STR2( Difficulty ) );
	PORT_DIPSETTING(    0x02, "Easy" );
	PORT_DIPSETTING(    0x03, "Medium" );
	PORT_DIPSETTING(    0x01, "Hard" );
	PORT_DIPSETTING(    0x00, "Hardest" );
	PORT_DIPNAME( 0x0c, 0x0c, DEF_STR2( Bonus_Life ) );
	PORT_DIPSETTING(    0x08, "20000 80000" );
	PORT_DIPSETTING(    0x0c, "30000 100000" );
	PORT_DIPSETTING(    0x04, "40000 200000" );
	PORT_DIPSETTING(    0x00, "50000 250000" );
	PORT_DIPNAME( 0x30, 0x30, DEF_STR2( Lives ) );
	PORT_DIPSETTING(    0x10, "1" );
	PORT_DIPSETTING(    0x00, "2" );
	PORT_DIPSETTING(    0x30, "3" );
	PORT_BITX( 0,       0x20, IPT_DIPSWITCH_SETTING | IPF_CHEAT, "100", IP_KEY_NONE, IP_JOY_NONE );
	PORT_DIPNAME( 0xc0, 0x00, "Monster Speed" );
	PORT_DIPSETTING(    0x00, "Normal" );
	PORT_DIPSETTING(    0x40, "Medium" );
	PORT_DIPSETTING(    0x80, "High" );
	PORT_DIPSETTING(    0xc0, "Very High" );

	PORT_START();      /* IN0 */
	PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT | IPF_2WAY );
	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_2WAY );
	PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_COIN2 );
	PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_COIN1 );
	PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON2 );
	PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON1 );
	PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_START1 );
	PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );

	PORT_START();      /* IN1 */
	PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT | IPF_2WAY | IPF_PLAYER2 );
	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_2WAY | IPF_PLAYER2 );
	PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_TILT ); /* ?????*/
	PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_SERVICE1 );
	PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER2 );
	PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER2 );
	PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_START2 );
	PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
	return true;
}

private boolean ipt_tokio() {
	PORT_START();      /* DSW0 */
	PORT_DIPNAME( 0x01, 0x00, DEF_STR2( Cabinet ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( Upright ) );
	PORT_DIPSETTING(    0x01, DEF_STR2( Cocktail ) );
	PORT_DIPNAME( 0x02, 0x02, DEF_STR2( Flip_Screen ) );
	PORT_DIPSETTING(    0x02, DEF_STR2( Off ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( On ) );
	PORT_SERVICE( 0x04, IP_ACTIVE_LOW );
	PORT_DIPNAME( 0x08, 0x08, DEF_STR2( Demo_Sounds ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( Off ) );
	PORT_DIPSETTING(    0x08, DEF_STR2( On ) );
	PORT_DIPNAME( 0x30, 0x30, DEF_STR2( Coin_A ) );
	PORT_DIPSETTING(    0x10, DEF_STR2( _2C_1C ) );
	PORT_DIPSETTING(    0x30, DEF_STR2( _1C_1C ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( _2C_3C ) );
	PORT_DIPSETTING(    0x20, DEF_STR2( _1C_2C ) );
	PORT_DIPNAME( 0xc0, 0xc0, DEF_STR2( Coin_B ) );
	PORT_DIPSETTING(    0x40, DEF_STR2( _2C_1C ) );
	PORT_DIPSETTING(    0xc0, DEF_STR2( _1C_1C ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( _2C_3C ) );
	PORT_DIPSETTING(    0x80, DEF_STR2( _1C_2C ) );

	PORT_START();      /* DSW1 */
	PORT_DIPNAME( 0x03, 0x02, DEF_STR2( Difficulty ) );
	PORT_DIPSETTING(    0x03, "Easy" );
	PORT_DIPSETTING(    0x02, "Medium" );
	PORT_DIPSETTING(    0x01, "Hard" );
	PORT_DIPSETTING(    0x00, "Hardest" );
	PORT_DIPNAME( 0x0c, 0x08, DEF_STR2( Bonus_Life ) );
	PORT_DIPSETTING(    0x0C, "100000 400000" );
	PORT_DIPSETTING(    0x08, "200000 400000" );
	PORT_DIPSETTING(    0x04, "300000 400000" );
	PORT_DIPSETTING(    0x00, "400000 400000" );
	PORT_DIPNAME( 0x30, 0x30, DEF_STR2( Lives ) );
	PORT_DIPSETTING(    0x30, "3" );
	PORT_DIPSETTING(    0x20, "4" );
	PORT_DIPSETTING(    0x10, "5" );
	PORT_BITX( 0,       0x00, IPT_DIPSWITCH_SETTING | IPF_CHEAT, "99", IP_KEY_NONE, IP_JOY_NONE );
	PORT_DIPNAME( 0x40, 0x40, DEF_STR2( Unknown ) );
	PORT_DIPSETTING(    0x40, DEF_STR2( Off ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( On ) );
	PORT_DIPNAME( 0x80, 0x00, "Language" );
	PORT_DIPSETTING(    0x00, "English" );
	PORT_DIPSETTING(    0x80, "Japanese" );

	PORT_START();      /* IN0 */
	PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_TILT );
	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_SERVICE1 );
	PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_COIN1 );
	PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_COIN2 );
	PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_UNKNOWN );
	PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );
	PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
	PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );

	PORT_START();      /* IN1 */
	PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT | IPF_2WAY );
	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_2WAY );
	PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN | IPF_2WAY );
	PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_UP | IPF_2WAY );
	PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON2 );
	PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON1 );
	PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_START1 );
	PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );

	PORT_START();      /* IN2 */
	PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT | IPF_2WAY | IPF_COCKTAIL );
	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_2WAY | IPF_COCKTAIL );
	PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN | IPF_2WAY | IPF_COCKTAIL );
	PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_UP | IPF_2WAY | IPF_COCKTAIL );
	PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER2 );
	PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER2 );
	PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_START2 );
	PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
	return true;
}

int[][] charlayout =
{
	{8},{8},	/* the characters are 8x8 pixels */
	{256*8*8},	/* 256 chars per bank * 8 banks per ROM pair * 8 ROM pairs */
	{4},	/* 4 bits per pixel */
	{ 8*0x8000*8+0, 8*0x8000*8+4, 0, 4 },
	{ 3, 2, 1, 0, 8+3, 8+2, 8+1, 8+0 },
	{ 0*16, 1*16, 2*16, 3*16, 4*16, 5*16, 6*16, 7*16 },
	{16*8}	/* every char takes 16 bytes in two ROMs */
};

private boolean gfxdecodeinfo()
{
	/* read all graphics into one big graphics region */
	GDI_ADD( REGION_GFX1, 0x00000, charlayout, 0, 16 );
	GDI_ADD( -1 );	/* end of array */
	return true;
};

static final int MAIN_XTAL = 24000000;

public boolean mdrv_boblbobl() {

	/* basic machine hardware */
	MDRV_CPU_ADD(Z80, MAIN_XTAL/4);	/* 6 MHz */
	MDRV_CPU_MEMORY(boblbobl_readmem(),boblbobl_writemem());
	MDRV_CPU_VBLANK_INT(irq0_line_hold,1);	/* interrupt mode 1, unlike Bubble Bobble */

	MDRV_CPU_ADD(Z80, MAIN_XTAL/4);	/* 6 MHz */
	MDRV_CPU_MEMORY(bublbobl_readmem2(),bublbobl_writemem2());
	MDRV_CPU_VBLANK_INT(irq0_line_hold,1);

	//MDRV_CPU_ADD(Z80, MAIN_XTAL/2);
	//MDRV_CPU_FLAGS(CPU_AUDIO_CPU);	/* 3 MHz */
	//MDRV_CPU_MEMORY(sound_readmem(),sound_writemem());
								/* IRQs are triggered by the YM2203 */
	/* sound hardware */
	//MDRV_SOUND_ADD(ym);
	//MDRV_SOUND_ADD(YM3526, ym3526_interface)
	
	MDRV_FRAMES_PER_SECOND(60);
	MDRV_VBLANK_DURATION(DEFAULT_60HZ_VBLANK_DURATION);
	MDRV_INTERLEAVE(100);	/* 100 CPU slices per frame - an high value to ensure proper */
							/* synchronization of the CPUs */

	/* video hardware */
	MDRV_VIDEO_ATTRIBUTES(VIDEO_TYPE_RASTER | GfxManager.VIDEO_MODIFIES_PALETTE);
	MDRV_SCREEN_SIZE(32*8, 32*8);
	MDRV_VISIBLE_AREA(0, 32*8-1, 2*8, 30*8-1);
	MDRV_GFXDECODE(gfxdecodeinfo());
	MDRV_PALETTE_LENGTH(256);

	MDRV_VIDEO_UPDATE(bublbobl_vu);

	return true;
}

public boolean mdrv_tokio() {

	/* basic machine hardware */
	MDRV_CPU_ADD(Z80, MAIN_XTAL/4);	/* 6 MHz */
	MDRV_CPU_MEMORY(tokio_readmem(),tokio_writemem());
	MDRV_CPU_VBLANK_INT(irq0_line_hold,1);

	MDRV_CPU_ADD(Z80, MAIN_XTAL/4);	/* 6 MHz */
	MDRV_CPU_MEMORY(tokio_readmem2(),tokio_writemem2());
	MDRV_CPU_VBLANK_INT(irq0_line_hold,1);

	//MDRV_CPU_ADD(Z80, MAIN_XTAL/8);
	//MDRV_CPU_FLAGS(CPU_AUDIO_CPU);	/* 3 MHz */
	//MDRV_CPU_MEMORY(tokio_sound_readmem(),tokio_sound_writemem());
						/* NMIs are triggered by the main CPU */
						/* IRQs are triggered by the YM2203 */

	/* sound hardware */
	//MDRV_SOUND_ADD(ym);
	
	MDRV_FRAMES_PER_SECOND(60);
	MDRV_VBLANK_DURATION(DEFAULT_60HZ_VBLANK_DURATION); /* frames/second, vblank duration */
	MDRV_INTERLEAVE(100);	/* 100 CPU slices per frame - an high value to ensure proper */
							/* synchronization of the CPUs */

	/* video hardware */
	MDRV_VIDEO_ATTRIBUTES(VIDEO_TYPE_RASTER);
	MDRV_SCREEN_SIZE(32*8, 32*8);
	MDRV_VISIBLE_AREA(0, 32*8-1, 2*8, 30*8-1);
	MDRV_GFXDECODE(gfxdecodeinfo());
	MDRV_PALETTE_LENGTH(256);

	MDRV_VIDEO_UPDATE(bublbobl_vu);

	return true;
}

/***************************************************************************

  Game driver(s)

***************************************************************************/

private boolean rom_boblbobl() {
	
	ROM_REGION( 0x30000, REGION_CPU1, 0 );
	ROM_LOAD( "bb3",          0x00000, 0x08000, 0x01f81936 );
    /* ROMs banked at 8000-bfff */
	ROM_LOAD( "bb5",          0x10000, 0x08000, 0x13118eb1 );
	ROM_LOAD( "bb4",          0x18000, 0x08000, 0xafda99d8 );
	/* 20000-2ffff empty */

	ROM_REGION( 0x10000, REGION_CPU2, 0 );	/* 64k for the second CPU */
	ROM_LOAD( "a78-08.37",    0x0000, 0x08000, 0xae11a07b );

	ROM_REGION( 0x10000, REGION_CPU3, 0 );	/* 64k for the third CPU */
	ROM_LOAD( "a78-07.46",    0x0000, 0x08000, 0x4f9a26e8 );

	ROM_REGION( 0x80000, REGION_GFX1, ROMREGION_DISPOSE | ROMREGION_INVERT );
	ROM_LOAD( "a78-09.12",    0x00000, 0x8000, 0x20358c22 );    /* 1st plane */
	ROM_LOAD( "a78-10.13",    0x08000, 0x8000, 0x930168a9 );
	ROM_LOAD( "a78-11.14",    0x10000, 0x8000, 0x9773e512 );
	ROM_LOAD( "a78-12.15",    0x18000, 0x8000, 0xd045549b );
	ROM_LOAD( "a78-13.16",    0x20000, 0x8000, 0xd0af35c5 );
	ROM_LOAD( "a78-14.17",    0x28000, 0x8000, 0x7b5369a8 );
	/* 0x30000-0x3ffff empty */
	ROM_LOAD( "a78-15.30",    0x40000, 0x8000, 0x6b61a413 );   /* 2nd plane */
	ROM_LOAD( "a78-16.31",    0x48000, 0x8000, 0xb5492d97 );
	ROM_LOAD( "a78-17.32",    0x50000, 0x8000, 0xd69762d5 );
	ROM_LOAD( "a78-18.33",    0x58000, 0x8000, 0x9f243b68 );
	ROM_LOAD( "a78-19.34",    0x60000, 0x8000, 0x66e9438c );
	ROM_LOAD( "a78-20.35",    0x68000, 0x8000, 0x9ef863ad );
	/* 0x70000-0x7ffff empty */
	ROM_REGION( 0x0100, REGION_PROMS, 0 );
	ROM_LOAD( "a71-25.41",    0x0000, 0x0100, 0x2d0f8545 );	/* video timing */
	
	return true;
}

private boolean rom_sboblbob() {
	ROM_REGION( 0x30000, REGION_CPU1, 0 );
	ROM_LOAD( "bbb-3.rom",    0x00000, 0x08000, 0xf304152a );
    /* ROMs banked at 8000-bfff */
	ROM_LOAD( "bb5",          0x10000, 0x08000, 0x13118eb1 );
	ROM_LOAD( "bbb-4.rom",    0x18000, 0x08000, 0x94c75591 );
	/* 20000-2ffff empty */

	ROM_REGION( 0x10000, REGION_CPU2, 0 );	/* 64k for the second CPU */
	ROM_LOAD( "a78-08.37",    0x0000, 0x08000, 0xae11a07b );

	//ROM_REGION( 0x10000, REGION_CPU3, 0 );	/* 64k for the third CPU */
	//ROM_LOAD( "a78-07.46",    0x0000, 0x08000, 0x4f9a26e8 );

	ROM_REGION( 0x80000, REGION_GFX1, ROMREGION_DISPOSE | ROMREGION_INVERT );
	ROM_LOAD( "a78-09.12",    0x00000, 0x8000, 0x20358c22 );    /* 1st plane */
	ROM_LOAD( "a78-10.13",    0x08000, 0x8000, 0x930168a9 );
	ROM_LOAD( "a78-11.14",    0x10000, 0x8000, 0x9773e512 );
	ROM_LOAD( "a78-12.15",    0x18000, 0x8000, 0xd045549b );
	ROM_LOAD( "a78-13.16",    0x20000, 0x8000, 0xd0af35c5 );
	ROM_LOAD( "a78-14.17",    0x28000, 0x8000, 0x7b5369a8 );
	/* 0x30000-0x3ffff empty */
	ROM_LOAD( "a78-15.30",    0x40000, 0x8000, 0x6b61a413 );    /* 2nd plane */
	ROM_LOAD( "a78-16.31",    0x48000, 0x8000, 0xb5492d97 );
	ROM_LOAD( "a78-17.32",    0x50000, 0x8000, 0xd69762d5 );
	ROM_LOAD( "a78-18.33",    0x58000, 0x8000, 0x9f243b68 );
	ROM_LOAD( "a78-19.34",    0x60000, 0x8000, 0x66e9438c );
	ROM_LOAD( "a78-20.35",    0x68000, 0x8000, 0x9ef863ad );
	/* 0x70000-0x7ffff empty */

	ROM_REGION( 0x0100, REGION_PROMS, 0 );
	ROM_LOAD( "a71-25.41",    0x0000, 0x0100, 0x2d0f8545 );	/* video timing */
	return true;
}

private boolean rom_tokiob() {
	ROM_REGION( 0x30000, REGION_CPU1, 0 ); /* main CPU */
	ROM_LOAD( "2",            0x00000, 0x8000, 0xf583b1ef );
    /* ROMs banked at 8000-bfff */
	ROM_LOAD( "3",            0x10000, 0x8000, 0x69dacf44 );
	ROM_LOAD( "a71-04.256",   0x18000, 0x8000, 0xa0a4ce0e );
	ROM_LOAD( "a71-05.256",   0x20000, 0x8000, 0x6da0b945 );
	ROM_LOAD( "6",            0x28000, 0x8000, 0x1490e95b );

	ROM_REGION( 0x10000, REGION_CPU2, 0 );	/* video CPU */
	ROM_LOAD( "a71-01.256",   0x00000, 0x8000, 0x0867c707 );

	ROM_REGION( 0x10000, REGION_CPU3, 0 );	/* audio CPU */
	ROM_LOAD( "a71-07.256",   0x0000, 0x08000, 0xf298cc7b );

	ROM_REGION( 0x80000, REGION_GFX1, ROMREGION_DISPOSE | ROMREGION_INVERT );
	ROM_LOAD( "a71-08.256",   0x00000, 0x8000, 0x0439ab13 );    /* 1st plane */
	ROM_LOAD( "a71-09.256",   0x08000, 0x8000, 0xedb3d2ff );
	ROM_LOAD( "a71-10.256",   0x10000, 0x8000, 0x69f0888c );
	ROM_LOAD( "a71-11.256",   0x18000, 0x8000, 0x4ae07c31 );
	ROM_LOAD( "a71-12.256",   0x20000, 0x8000, 0x3f6bd706 );
	ROM_LOAD( "a71-13.256",   0x28000, 0x8000, 0xf2c92aaa );
	ROM_LOAD( "a71-14.256",   0x30000, 0x8000, 0xc574b7b2 );
	ROM_LOAD( "a71-15.256",   0x38000, 0x8000, 0x12d87e7f );
	ROM_LOAD( "a71-16.256",   0x40000, 0x8000, 0x0bce35b6 );    /* 2nd plane */
	ROM_LOAD( "a71-17.256",   0x48000, 0x8000, 0xdeda6387 );
	ROM_LOAD( "a71-18.256",   0x50000, 0x8000, 0x330cd9d7 );
	ROM_LOAD( "a71-19.256",   0x58000, 0x8000, 0xfc4b29e0 );
	ROM_LOAD( "a71-20.256",   0x60000, 0x8000, 0x65acb265 );
	ROM_LOAD( "a71-21.256",   0x68000, 0x8000, 0x33cde9b2 );
	ROM_LOAD( "a71-22.256",   0x70000, 0x8000, 0xfb98eac0 );
	ROM_LOAD( "a71-23.256",   0x78000, 0x8000, 0x30bd46ad );

	ROM_REGION( 0x0100, REGION_PROMS, 0 );
	ROM_LOAD( "a71-25.bin",   0x0000, 0x0100, 0x2d0f8545 );	/* video timing */
	return true;
}

public InitHandler init_bublbobl() { return new Init_bublbobl(); }
public class Init_bublbobl implements InitHandler {
	public void init() {
		int[] ROM = memory_region(REGION_CPU1);

		/* in Bubble Bobble, bank 0 has code falling from 7fff to 8000, */
		/* so I have to copy it there because bank switching wouldn't catch it */
		for(int j=0; j<0x4000; j++)
			ROM[j+0x08000] = ROM[j+0x10000];
	}
}

public InitHandler init_boblbobl() { return new Init_boblbobl(); }
public class Init_boblbobl implements InitHandler {
	public void MOD_PAGE(int page,int addr,int data) {
		memory_region(REGION_CPU1)[addr-0x8000+0x10000+0x4000*page] = data;
	}

	public void init() {
    /* these shouldn't be necessary, surely - this is a bootleg ROM
     * with the protection removed - so what are all these JP's to
     * 0xa288 doing?  and why does the emulator fail the ROM checks?
     */

	MOD_PAGE(3,0x9a71,0x00); MOD_PAGE(3,0x9a72,0x00); MOD_PAGE(3,0x9a73,0x00);
	MOD_PAGE(3,0xa4af,0x00); MOD_PAGE(3,0xa4b0,0x00); MOD_PAGE(3,0xa4b1,0x00);
	MOD_PAGE(3,0xa55d,0x00); MOD_PAGE(3,0xa55e,0x00); MOD_PAGE(3,0xa55f,0x00);
	MOD_PAGE(3,0xb561,0x00); MOD_PAGE(3,0xb562,0x00); MOD_PAGE(3,0xb563,0x00);

	init_bublbobl().init();
	}
}

public InitHandler init_tokio() { return new Init_tokio(); }
public class Init_tokio implements InitHandler {
	public void init() {
		/* preemptively enable video, the bit is not mapped for this game and */
		/* I don't know if it even has it. */
		v.bublbobl_video_enable = 1;
	}
}

	public Machine getMachine(URL url, String name) {
		super.getMachine(url,name);
		super.setVideoEmulator(v);
		m = new jef.machine.BasicMachine();

		if (name.equals("boblbobl")) {
			GAME( 1986, rom_boblbobl(), "bublbobl", mdrv_boblbobl(), ipt_boblbobl(), init_boblbobl(), ROT0,  "bootleg", "Bobble Bobble" );
		} else if (name.equals("sboblbob")) {
			GAME( 1986, rom_sboblbob(), "bublbobl", mdrv_boblbobl(), ipt_sboblbob(), init_bublbobl(), ROT0,  "bootleg", "Super Bobble Bobble" );
		} else if (name.equals("tokiob")) {
			GAME( 1986, rom_tokiob(),   "tokio",    mdrv_tokio(),    ipt_tokio(),    init_tokio(),    ROT90, "bootleg", "Tokio / Scramble Formation (bootleg)" );
		}

		m.init(md);
		return (Machine)m;
	}

}