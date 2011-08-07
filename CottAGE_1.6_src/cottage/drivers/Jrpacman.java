/*
CottAGE - the Arcade Generic Emulator in Java

Java driver by Gollum
*/

/***************************************************************************

	Bally/Midway Jr. Pac-Man

    Games supported:
		* Jr. Pac-Man
	
	Known issues:
		* none

****************************************************************************

	Jr. Pac Man memory map (preliminary)

	0000-3fff ROM
	4000-47ff Video RAM (also color RAM)
	4800-4fff RAM
	8000-dfff ROM

	memory mapped ports:

	read:
	5000      IN0
	5040      IN1
	5080      DSW1

	*
	 * IN0 (all bits are inverted)
	 * bit 7 : CREDIT
	 * bit 6 : COIN 2
	 * bit 5 : COIN 1
	 * bit 4 : RACK TEST
	 * bit 3 : DOWN player 1
	 * bit 2 : RIGHT player 1
	 * bit 1 : LEFT player 1
	 * bit 0 : UP player 1
	 *
	*
	 * IN1 (all bits are inverted)
	 * bit 7 : TABLE or UPRIGHT cabinet select (1 = UPRIGHT)
	 * bit 6 : START 2
	 * bit 5 : START 1
	 * bit 4 : TEST SWITCH
	 * bit 3 : DOWN player 2 (TABLE only)
	 * bit 2 : RIGHT player 2 (TABLE only)
	 * bit 1 : LEFT player 2 (TABLE only)
	 * bit 0 : UP player 2 (TABLE only)
	 *
	*
	 * DSW1 (all bits are inverted)
	 * bit 7 :  ?
	 * bit 6 :  difficulty level
	 *                       1 = Normal  0 = Harder
	 * bit 5 :\ bonus pac at xx000 pts
	 * bit 4 :/ 00 = 10000  01 = 15000  10 = 20000  11 = 30000
	 * bit 3 :\ nr of lives
	 * bit 2 :/ 00 = 1  01 = 2  10 = 3  11 = 5
	 * bit 1 :\ play mode
	 * bit 0 :/ 00 = free play   01 = 1 coin 1 credit
	 *          10 = 1 coin 2 credits   11 = 2 coins 1 credit
	 *

	write:
	4ff2-4ffd 6 pairs of two bytes:
	          the first byte contains the sprite image number (bits 2-7), Y flip (bit 0),
			  X flip (bit 1); the second byte the color
	5000      interrupt enable
	5001      sound enable
	5002      unused
	5003      flip screen
	5004      unused
	5005      unused
	5006      unused
	5007      coin counter
	5040-5044 sound voice 1 accumulator (nibbles) (used by the sound hardware only)
	5045      sound voice 1 waveform (nibble)
	5046-5049 sound voice 2 accumulator (nibbles) (used by the sound hardware only)
	504a      sound voice 2 waveform (nibble)
	504b-504e sound voice 3 accumulator (nibbles) (used by the sound hardware only)
	504f      sound voice 3 waveform (nibble)
	5050-5054 sound voice 1 frequency (nibbles)
	5055      sound voice 1 volume (nibble)
	5056-5059 sound voice 2 frequency (nibbles)
	505a      sound voice 2 volume (nibble)
	505b-505e sound voice 3 frequency (nibbles)
	505f      sound voice 3 volume (nibble)
	5062-506d Sprite coordinates, x/y pairs for 6 sprites
	5070      palette bank
	5071      colortable bank
	5073      background priority over sprites
	5074      char gfx bank
	5075      sprite gfx bank
	5080      scroll
	50c0      Watchdog reset

	I/O ports:
	OUT on port $0 sets the interrupt vector

***************************************************************************/

package cottage.drivers;

import java.net.URL;
import jef.map.*;
import jef.machine.*;
import jef.video.*;
import cottage.mame.*;

public class Jrpacman extends MAMEDriver implements Driver, MAMEConstants {

cottage.vidhrdw.Jrpacman v = new cottage.vidhrdw.Jrpacman();
int[] jrpacman_palettebank = v.Fjrpacman_palettebank;
int[] jrpacman_colortablebank = v.Fjrpacman_colortablebank;
int[] jrpacman_bgpriority = v.Fjrpacman_bgpriority;
int[] jrpacman_charbank = v.Fjrpacman_charbank;
int[] jrpacman_spritebank = v.Fjrpacman_spritebank;
int[] jrpacman_scroll = v.Fjrpacman_scroll;
WriteHandler jrpacman_videoram_w = v.jrpacman_videoram_w();
WriteHandler jrpacman_palettebank_w = v.jrpacman_palettebank_w();
WriteHandler jrpacman_colortablebank_w = v.jrpacman_colortablebank_w();
WriteHandler jrpacman_charbank_w = v.jrpacman_charbank_w();
Vh_start jrpacman_vs = (Vh_start)v;
Vh_refresh jrpacman_vu = (Vh_refresh)v;;
Vh_convert_color_proms jrpacman_pi = (Vh_convert_color_proms)v;

jef.machine.BasicMachine m = new jef.machine.BasicMachine();
InterruptHandler irq0_line_hold = m.irq0_line_hold();

WriteHandler interrupt_vector_w = new Interrupt_vector_w();
WriteHandler interrupt_enable_w = m.interrupt_enable();
InterruptHandler jrpacman_interrupt = new Jrpacman_interrupt();

public class Interrupt_vector_w implements WriteHandler {
	public void write(int address, int value) {
		m.cd[0].cpu.setProperty(0,value);
	}
}

static int speedcheat = 0; /* a well known hack allows to make JrPac Man run at four times */
				   /* his usual speed. When we start the emulation, we check if the */
				   /* hack can be applied, and set this flag accordingly. */

/*************************************
 *
 *	Machine init
 *
 *************************************/

public void MACHINE_INIT_jrpacman()
{
	int[] RAM = memory_region(REGION_CPU1);

	/* check if the loaded set of ROMs allows the Pac Man speed hack */
	if (RAM[0x180b] == 0xbe || RAM[0x180b] == 0x01)
		speedcheat = 1;
	else
		speedcheat = 0;
}

/*************************************
 *
 *	Interrupts
 *
 *************************************/

public class Jrpacman_interrupt implements InterruptHandler {
	public int irq() {
		int[] RAM = memory_region(REGION_CPU1);

		/* speed up cheat */
		if (speedcheat != 0)
		{
			if ((input_port_3_r.read(0) & 1) != 0)	/* check status of the fake dip switch */
			{
				/* activate the cheat */
				RAM[0x180b] = 0x01;
			}
			else
			{
				/* remove the cheat */
				RAM[0x180b] = 0xbe;
			}
		}

		return m.irq0_line_hold().irq();
	}
}

/*************************************
 *
 *	Main CPU memory handlers
 *
 *************************************/

private boolean readmem() {
	MR_START( 0x0000, 0x3fff, MRA_ROM );
	MR_ADD( 0x4000, 0x4fff, MRA_RAM );	/* including video and color RAM */
	MR_ADD( 0x5000, 0x503f, input_port_0_r );	/* IN0 */
	MR_ADD( 0x5040, 0x507f, input_port_1_r );	/* IN1 */
	MR_ADD( 0x5080, 0x50bf, input_port_2_r );	/* DSW1 */
	MR_ADD( 0x8000, 0xdfff, MRA_ROM );
	return true;
}

private boolean writemem() {
	MW_START( 0x0000, 0x3fff, MWA_ROM );
	MW_ADD( 0x4000, 0x47ff, jrpacman_videoram_w, videoram, videoram_size );
	MW_ADD( 0x4800, 0x4fef, MWA_RAM );
	MW_ADD( 0x4ff0, 0x4fff, MWA_RAM, spriteram, spriteram_size );
	MW_ADD( 0x5000, 0x5000, interrupt_enable_w );
	//MW_ADD( 0x5001, 0x5001, pengo_sound_enable_w );
	//MW_ADD( 0x5003, 0x5003, jrpacman_flipscreen_w );
	//MW_ADD( 0x5040, 0x505f, pengo_sound_w, pengo_soundregs );
	MW_ADD( 0x5060, 0x506f, MWA_RAM, spriteram_2 );
	MW_ADD( 0x5070, 0x5070, jrpacman_palettebank_w, jrpacman_palettebank );
	MW_ADD( 0x5071, 0x5071, jrpacman_colortablebank_w, jrpacman_colortablebank );
	MW_ADD( 0x5073, 0x5073, MWA_RAM, jrpacman_bgpriority );
	MW_ADD( 0x5074, 0x5074, jrpacman_charbank_w, jrpacman_charbank );
	MW_ADD( 0x5075, 0x5075, MWA_RAM, jrpacman_spritebank );
	MW_ADD( 0x5080, 0x5080, MWA_RAM, jrpacman_scroll );
	MW_ADD( 0x50c0, 0x50c0, MWA_NOP );
	MW_ADD( 0x8000, 0xdfff, MWA_ROM );
	return true;
}

/*************************************
 *
 *	Main CPU port handlers
 *
 *************************************/

private boolean writeport() {
	PW_START( 0x00, 0x00, interrupt_vector_w );
	return true;
}

/*************************************
 *
 *	Port definitions
 *
 *************************************/

private boolean ipt_jrpacman() {
	PORT_START();	/* IN0 */
	PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP | IPF_4WAY );
	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT | IPF_4WAY );
	PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_4WAY );
	PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN | IPF_4WAY );
	//PORT_BITX(    0x10, 0x10, IPT_DIPSWITCH_NAME | IPF_CHEAT, "Rack Test", KEYCODE_F1, IP_JOY_NONE );
	PORT_DIPNAME( 0x10, 0x10, "Rack Test" );
	PORT_DIPSETTING(    0x10, DEF_STR2( Off ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( On ) );
	PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_COIN1 );
	PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_COIN2 );
	PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_COIN3 );

	PORT_START();	/* IN1 */
	PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP | IPF_4WAY | IPF_COCKTAIL );
	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT | IPF_4WAY | IPF_COCKTAIL );
	PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_4WAY | IPF_COCKTAIL );
	PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN | IPF_4WAY | IPF_COCKTAIL );
	PORT_SERVICE( 0x10, IP_ACTIVE_LOW );
	PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_START1 );
	PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_START2 );
	PORT_DIPNAME( 0x80, 0x80, DEF_STR2( Cabinet ) );
	PORT_DIPSETTING(    0x80, DEF_STR2( Upright ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( Cocktail ) );

	PORT_START();	/* DSW0 */
	PORT_DIPNAME( 0x03, 0x01, DEF_STR2( Coinage ) );
	PORT_DIPSETTING(    0x03, DEF_STR2( _2C_1C ) );
	PORT_DIPSETTING(    0x01, DEF_STR2( _1C_1C ) );
	PORT_DIPSETTING(    0x02, DEF_STR2( _1C_2C ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( Free_Play ) );
	PORT_DIPNAME( 0x0c, 0x08, DEF_STR2( Lives ) );
	PORT_DIPSETTING(    0x00, "1" );
	PORT_DIPSETTING(    0x04, "2" );
	PORT_DIPSETTING(    0x08, "3" );
	PORT_DIPSETTING(    0x0c, "5" );
	PORT_DIPNAME( 0x30, 0x00, DEF_STR2( Bonus_Life ) );
	PORT_DIPSETTING(    0x00, "10000" );
	PORT_DIPSETTING(    0x10, "15000" );
	PORT_DIPSETTING(    0x20, "20000" );
	PORT_DIPSETTING(    0x30, "30000" );
	PORT_DIPNAME( 0x40, 0x40, DEF_STR2( Difficulty ) );
	PORT_DIPSETTING(    0x40, "Normal" );
	PORT_DIPSETTING(    0x00, "Hard" );
	PORT_DIPNAME( 0x80, 0x80, DEF_STR2( Unknown ) );
	PORT_DIPSETTING(    0x80, DEF_STR2( Off ) );
	PORT_DIPSETTING(    0x00, DEF_STR2( On ) );

	//PORT_START();	/* FAKE */
	/* This fake input port is used to get the status of the fire button */
	/* and activate the speedup cheat if it is. */
	//PORT_BITX(    0x01, 0x00, IPT_DIPSWITCH_NAME | IPF_CHEAT, "Speedup Cheat", KEYCODE_LCONTROL, JOYCODE_1_BUTTON1 )
	//PORT_DIPSETTING(    0x00, DEF_STR2( Off ) )
	//PORT_DIPSETTING(    0x01, DEF_STR2( On ) )
	return true;
}

/*************************************
 *
 *	Graphics layouts
 *
 *************************************/

int[][] charlayout =
{
    {8},{8},
    {RGN_FRAC(1,1)},
    {2},
    { 0, 4 },
    { 8*8+0, 8*8+1, 8*8+2, 8*8+3, 0, 1, 2, 3 },
    { 0*8, 1*8, 2*8, 3*8, 4*8, 5*8, 6*8, 7*8 },
    {16*8}
};

int[][] spritelayout =
{
	{16},{16},
	{RGN_FRAC(1,1)},
	{2},
	{ 0, 4 },
	{ 8*8, 8*8+1, 8*8+2, 8*8+3, 16*8+0, 16*8+1, 16*8+2, 16*8+3,
			24*8+0, 24*8+1, 24*8+2, 24*8+3, 0, 1, 2, 3 },
	{ 0*8, 1*8, 2*8, 3*8, 4*8, 5*8, 6*8, 7*8,
			32*8, 33*8, 34*8, 35*8, 36*8, 37*8, 38*8, 39*8 },
	{64*8}
};

private boolean gfxdecodeinfo()
{
	GDI_ADD( REGION_GFX1, 0, charlayout,   0, 128 );
	GDI_ADD( REGION_GFX2, 0, spritelayout, 0, 128 );
	GDI_ADD( -1 ); /* end of array */
	return true;
};

/*************************************
 *
 *	Sound interfaces
 *
 *************************************/

/*************************************
 *
 *	Machine drivers
 *
 *************************************/

public boolean mdrv_jrpacman() {

	/* basic machine hardware */
	MDRV_CPU_ADD(Z80, 18432000/6);	/* 3.072 MHz */
	MDRV_CPU_MEMORY(readmem(),writemem());
	MDRV_CPU_PORTS(0,writeport());
	MDRV_CPU_VBLANK_INT(jrpacman_interrupt,1);

	MDRV_FRAMES_PER_SECOND(60.606060);
	MDRV_VBLANK_DURATION(DEFAULT_REAL_60HZ_VBLANK_DURATION);
	
	//MDRV_MACHINE_INIT(jrpacman);
	
	/* video hardware */
	MDRV_VIDEO_ATTRIBUTES(VIDEO_TYPE_RASTER);
	MDRV_SCREEN_SIZE(36*8, 28*8);
	MDRV_VISIBLE_AREA(0*8, 36*8-1, 0*8, 28*8-1);
	MDRV_GFXDECODE(gfxdecodeinfo());
	MDRV_PALETTE_LENGTH(32);
	MDRV_COLORTABLE_LENGTH(128*4);

	MDRV_PALETTE_INIT(jrpacman_pi);
	MDRV_VIDEO_START(jrpacman_vs);
	MDRV_VIDEO_UPDATE(jrpacman_vu);

	/* sound hardware */
	//MDRV_SOUND_ADD(NAMCO, namco_interface);
	return true;
}

/*************************************
 *
 *	ROM definitions
 *
 *************************************/

private boolean rom_jrpacman() {
	ROM_REGION( 0x10000, REGION_CPU1, 0 );	/* 64k for code */
	ROM_LOAD( "jrp8d.bin",    0x0000, 0x2000, 0xe3fa972e );
	ROM_LOAD( "jrp8e.bin",    0x2000, 0x2000, 0xec889e94 );
	ROM_LOAD( "jrp8h.bin",    0x8000, 0x2000, 0x35f1fc6e );
	ROM_LOAD( "jrp8j.bin",    0xa000, 0x2000, 0x9737099e );
	ROM_LOAD( "jrp8k.bin",    0xc000, 0x2000, 0x5252dd97 );

	ROM_REGION( 0x2000, REGION_GFX1, ROMREGION_DISPOSE );
	ROM_LOAD( "jrp2c.bin",    0x0000, 0x2000, 0x0527ff9b );

	ROM_REGION( 0x2000, REGION_GFX2, ROMREGION_DISPOSE );
	ROM_LOAD( "jrp2e.bin",    0x0000, 0x2000, 0x73477193 );

	ROM_REGION( 0x0300, REGION_PROMS, 0 );
	ROM_LOAD( "jrprom.9e",    0x0000, 0x0100, 0x029d35c4 ); /* palette low bits */
	ROM_LOAD( "jrprom.9f",    0x0100, 0x0100, 0xeee34a79 ); /* palette high bits */
	ROM_LOAD( "jrprom.9p",    0x0200, 0x0100, 0x9f6ea9d8 ); /* color lookup table */

	//ROM_REGION( 0x0200, REGION_SOUND1, 0 );	/* sound prom */
	//ROM_LOAD( "jrprom.7p",    0x0000, 0x0100, 0xa9cc86bf );
	//ROM_LOAD( "jrprom.5s",    0x0100, 0x0100, 0x77245b66 );	/* timing - not used */
	return true;
}

/*************************************
 *
 *	Driver initialization
 *
 *************************************/

public InitHandler init_jrpacman() { return new Init_jrpacman(); }
public class Init_jrpacman implements InitHandler {
	public void init() {
	/* The encryption PALs garble bits 0, 2 and 7 of the ROMs. The encryption */
	/* scheme is complex (basically it's a state machine) and can only be */
	/* faithfully emulated at run time. To avoid the performance hit that would */
	/* cause, here we have a table of the values which must be XORed with */
	/* each memory region to obtain the decrypted bytes. */
	/* Decryption table provided by David Caldwell (david@indigita.com) */
	/* For an accurate reproduction of the encryption, see jrcrypt.c */
	int table[][] =
	{
		{ 0x00C1, 0x00 },{ 0x0002, 0x80 },{ 0x0004, 0x00 },{ 0x0006, 0x80 },
		{ 0x0003, 0x00 },{ 0x0002, 0x80 },{ 0x0009, 0x00 },{ 0x0004, 0x80 },
		{ 0x9968, 0x00 },{ 0x0001, 0x80 },{ 0x0002, 0x00 },{ 0x0001, 0x80 },
		{ 0x0009, 0x00 },{ 0x0002, 0x80 },{ 0x0009, 0x00 },{ 0x0001, 0x80 },
		{ 0x00AF, 0x00 },{ 0x000E, 0x04 },{ 0x0002, 0x00 },{ 0x0004, 0x04 },
		{ 0x001E, 0x00 },{ 0x0001, 0x80 },{ 0x0002, 0x00 },{ 0x0001, 0x80 },
		{ 0x0002, 0x00 },{ 0x0002, 0x80 },{ 0x0009, 0x00 },{ 0x0002, 0x80 },
		{ 0x0009, 0x00 },{ 0x0002, 0x80 },{ 0x0083, 0x00 },{ 0x0001, 0x04 },
		{ 0x0001, 0x01 },{ 0x0001, 0x00 },{ 0x0002, 0x05 },{ 0x0001, 0x00 },
		{ 0x0003, 0x04 },{ 0x0003, 0x01 },{ 0x0002, 0x00 },{ 0x0001, 0x04 },
		{ 0x0003, 0x01 },{ 0x0003, 0x00 },{ 0x0003, 0x04 },{ 0x0001, 0x01 },
		{ 0x002E, 0x00 },{ 0x0078, 0x01 },{ 0x0001, 0x04 },{ 0x0001, 0x05 },
		{ 0x0001, 0x00 },{ 0x0001, 0x01 },{ 0x0001, 0x04 },{ 0x0002, 0x00 },
		{ 0x0001, 0x01 },{ 0x0001, 0x04 },{ 0x0002, 0x00 },{ 0x0001, 0x01 },
		{ 0x0001, 0x04 },{ 0x0002, 0x00 },{ 0x0001, 0x01 },{ 0x0001, 0x04 },
		{ 0x0001, 0x05 },{ 0x0001, 0x00 },{ 0x0001, 0x01 },{ 0x0001, 0x04 },
		{ 0x0002, 0x00 },{ 0x0001, 0x01 },{ 0x0001, 0x04 },{ 0x0002, 0x00 },
		{ 0x0001, 0x01 },{ 0x0001, 0x04 },{ 0x0001, 0x05 },{ 0x0001, 0x00 },
		{ 0x01B0, 0x01 },{ 0x0001, 0x00 },{ 0x0002, 0x01 },{ 0x00AD, 0x00 },
		{ 0x0031, 0x01 },{ 0x005C, 0x00 },{ 0x0005, 0x01 },{ 0x604E, 0x00 },
	    { 0,0 }
	};
	int i,j,A;
	int[] RAM = memory_region(REGION_CPU1);


	A = 0;
	i = 0;
	while (table[i][1]!=0)
	{
		for (j = 0;j < table[i][1];j++)
		{
			RAM[A] ^= table[i][0];
			A++;
		}
		i++;
	}
	}
}

/*************************************
 *
 *	Game drivers
 *
 *************************************/

public Machine getMachine(URL url, String name) {
	super.getMachine(url,name);
	super.setVideoEmulator(v);
	m = new jef.machine.BasicMachine();

	if (name.equals("jrpacman"))
		GAME( 1983, rom_jrpacman(), 0, mdrv_jrpacman(), ipt_jrpacman(), init_jrpacman(), ROT90, "Bally Midway", "Jr. Pac-Man" );

	m.init(md);
	return (Machine)m;
}

}