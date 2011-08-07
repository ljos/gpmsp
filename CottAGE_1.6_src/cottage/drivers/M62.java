/*
 CottAGE - the Arcade Generic Emulator in Java

 Java driver by Gollum
 */

/****************************************************************************

 Irem "M62" system

 TODO:
 - Kid Niki is missing the drums. There is an analog section in the sound board.

 Notes:
 - I believe that both kungfum bootlegs are derived from an Irem original which we
 don't have (prototype/early revision?). They say "kanfu master" instead of
 "kung-fu master" on the introduction screen.



 The following information is gathered from Kung Fu Master; the board was most
 likely modified for other games (or, not all the games in this driver are
 really M62).

 The M62 board can be set up for different configurations through the use of
 jumpers.

 A board:
 J1: \
 J2: / ROM or RAM at 0x4000
 J3: sound prg ROM size, 2764 or 27128
 J4: send output C of the secondy AY-3-8910 to SOUND IO instead of SOUND. Is
 this to have it amplified more?
 J5: enable a tristate on accesses to the range a000-bfff (must not be done
 when there is ROM at this address)
 J6:
 J7: main prg ROM type, 2764 or 27128

 B board:
 J1: selects whether bit 4 of obj color code selects or not high priority over tiles
 J2: selects whether bit 4 of obj color code goes to A7 of obj color PROMS
 J3: I'm not sure about this. It involves A8 of sprite ram.
 J4: pixels per scanline, 256 or 384. There's also a PROM @ 6F that controls
 video timing and how long a scanline is.
 J5: output Horizontal Sync or Composite Sync
 J6: ??? where is this ???
 J7: \ main xtal, 18.432 MHz (for low resolution games?) or
 J8: / 24 MHz (for mid resolution games?)
 J9: obj ROM type, 2764 or 27128

 G board:
 JP1: \
 JP2: | Tiles with color code >= the value set here have priority over sprites
 JP3: |
 JP4: /

 **************************************************************************/

package cottage.drivers;

import java.net.URL;

import jef.machine.Machine;
import jef.map.InterruptHandler;
import jef.map.ReadHandler;
import jef.map.WriteHandler;
import jef.video.Vh_convert_color_proms;
import jef.video.Vh_refresh;
import jef.video.Vh_start;

import cottage.mame.Driver;
import cottage.mame.MAMEConstants;
import cottage.mame.MAMEDriver;

public class M62 extends MAMEDriver implements Driver, MAMEConstants {

    cottage.vidhrdw.M62 v = new cottage.vidhrdw.M62();

    Vh_convert_color_proms irem_pi = (Vh_convert_color_proms) v;

    Vh_start ldrun_vs = (Vh_start) v;

    Vh_refresh ldrun_vu = (Vh_refresh) v;

    WriteHandler irem_flipscreen_w = v.irem_flipscreen_w();

    WriteHandler videoram_w = v.videoram_w();

    Vh_start kungfum_vs = v.kungfum_vs();

    jef.machine.BasicMachine m = new jef.machine.BasicMachine();

    InterruptHandler irq0_line_hold = m.irq0_line_hold();
    
    ReadHandler ldrun3_prot_5_r = new Ldrun3_prot_5_r();
    ReadHandler ldrun3_prot_7_r = new Ldrun3_prot_7_r();

    private int ldrun2_bankswap;

    public class Ldrun2_bankswitch_r implements ReadHandler {
        public int read(int offset) {
            if (ldrun2_bankswap != 0) {
                // unsigned char *RAM = memory_region(REGION_CPU1);

                ldrun2_bankswap--;

                /* swap to bank #1 on second read */
                if (ldrun2_bankswap == 0)
                    cpu_setbank(1, 0x12000);
            }
            return 0;
        }
    }

    public class Ldrun2_bankswitch_w implements WriteHandler {
        int[] bankcontrol = new int[2];

        int[] banks = { 0, 0, 0, 0, 0, 1, 0, 1, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1 };

        public void write(int offset, int data) {
            offset -= 0x80;
            int bankaddress;
            // RAM = memory_region(REGION_CPU1);

            bankcontrol[offset] = data;
            if (offset == 0) {
                if (data < 1 || data > 30) {
                    // if (errorlog) fprintf(errorlog,"unknown bank select
                    // %02x\n",data);
                    return;
                }
                bankaddress = 0x10000 + (banks[data - 1] * 0x2000);
                cpu_setbank(1, bankaddress);
            } else {
                if (bankcontrol[0] == 0x01 && data == 0x0d)
                    /* special case for service mode */
                    ldrun2_bankswap = 2;
                else
                    ldrun2_bankswap = 0;
            }
        }
    }

    /* Lode Runner 3 has, it seems, a poor man's protection consisting of a PAL */
    /* (I think; it's included in the ROM set) which is read at certain times, */
    /* and the game crashes if ti doesn't match the expected values. */
    public class Ldrun3_prot_5_r implements ReadHandler {
        public int read(int offset) {
            return 5;
        }
    }

    public class Ldrun3_prot_7_r implements ReadHandler {
        public int read(int offset) {
            return 7;
        }
    }

    private boolean ldrun_readmem() {
        MR_START(0x0000, 0x7fff, MRA_ROM);
        MR_ADD(0xd000, 0xefff, MRA_RAM);
        return true;
    }

    private boolean ldrun_writemem() {
        MW_START(0x0000, 0x7fff, MWA_ROM);
        MW_ADD(0xc000, 0xc0ff, MWA_RAM, spriteram, spriteram_size);
        MW_ADD(0xd000, 0xdfff, videoram_w, videoram, videoram_size);
        MW_ADD(0xe000, 0xefff, MWA_RAM);
        return true;
    }

    private boolean ldrun2_readmem() {
        MR_START(0x0000, 0x7fff, MRA_ROM);
        MR_ADD(0x8000, 0x9fff, MRA_BANK1);
        MR_ADD(0xd000, 0xefff, MRA_RAM);
        return true;
    }

    private boolean ldrun2_writemem() {
        MW_START(0x0000, 0x9fff, MWA_ROM);
        MW_ADD(0xc000, 0xc0ff, MWA_RAM, spriteram, spriteram_size);
        MW_ADD(0xd000, 0xdfff, videoram_w, videoram, videoram_size);
        MW_ADD(0xe000, 0xefff, MWA_RAM);
        return true;
    }

    private boolean ldrun3_readmem() {
        MR_START(0x0000, 0xbfff, MRA_ROM);
        MR_ADD(0xc800, 0xc800, ldrun3_prot_5_r);
        MR_ADD(0xcc00, 0xcc00, ldrun3_prot_5_r);
        MR_ADD(0xcfff, 0xcfff, ldrun3_prot_7_r);
        MR_ADD(0xd000, 0xefff, MRA_RAM);
        return true;
    }

    private boolean ldrun3_writemem() {
        MW_START(0x0000, 0xbfff, MWA_ROM);
        MW_ADD(0xc000, 0xc0ff, MWA_RAM, spriteram, spriteram_size);
        MW_ADD(0xd000, 0xdfff, videoram_w, videoram, videoram_size);
        MW_ADD(0xe000, 0xefff, MWA_RAM);
        return true;
    }

    private boolean kungfum_writemem() {
        MW_START(0x0000, 0x7fff, MWA_ROM);
        MW_ADD(0xa000, 0xa000, v.kungfum_scroll_low_w());
        MW_ADD(0xb000, 0xb000, v.kungfum_scroll_high_w());
        MW_ADD(0xc000, 0xc0ff, MWA_RAM, spriteram, spriteram_size);
        /* Kung Fu Master is the only game in this driver to have separated (but */
        /*
         * contiguous) videoram and colorram. They are interleaved in all the
         * others.
         */
        MW_ADD(0xd000, 0xdfff, videoram_w, videoram, videoram_size);
        MW_ADD(0xe000, 0xefff, MWA_RAM);
        return true;
    };

    private boolean ldrun_readport() {
        PR_START(0x00, 0x00, input_port_0_r); /* coin */
        PR_ADD(0x01, 0x01, input_port_1_r); /* player 1 control */
        PR_ADD(0x02, 0x02, input_port_2_r); /* player 2 control */
        PR_ADD(0x03, 0x03, input_port_3_r); /* DSW 1 */
        PR_ADD(0x04, 0x04, input_port_4_r); /* DSW 2 */
        return true;
    }

    private boolean ldrun_writeport() {
        // PW_START( 0x00, 0x00, irem_sound_cmd_w );
        PW_START(0x01, 0x01, irem_flipscreen_w); /* + coin counters */
        return true;
    }

    private boolean ldrun2_readport() {
        PR_START(0x00, 0x00, input_port_0_r); /* coin */
        PR_ADD(0x01, 0x01, input_port_1_r); /* player 1 control */
        PR_ADD(0x02, 0x02, input_port_2_r); /* player 2 control */
        PR_ADD(0x03, 0x03, input_port_3_r); /* DSW 1 */
        PR_ADD(0x04, 0x04, input_port_4_r); /* DSW 2 */
        PR_ADD(0x80, 0x80, new Ldrun2_bankswitch_r());
        return true;
    }

    private boolean ldrun2_writeport() {
        // PW_START( 0x00, 0x00, irem_sound_cmd_w );
        PW_START(0x01, 0x01, irem_flipscreen_w); /* + coin counters */
        PW_ADD(0x80, 0x80, new Ldrun2_bankswitch_w());
        return true;
    }

    private boolean ldrun3_writeport() {
        // PW_START( 0x00, 0x00, irem_sound_cmd_w );
        PW_START(0x01, 0x01, irem_flipscreen_w); /* + coin counters */
        PW_ADD(0x80, 0x80, v.ldrun3_vscroll_w());
        return true;
    }

    void IN0_PORT() {
        /*
         * Start 1 & 2 also restarts and freezes the game with stop mode on and
         * are used in test mode to enter and esc the various tests
         */
        PORT_BIT(0x01, IP_ACTIVE_LOW, IPT_START1);
        PORT_BIT(0x02, IP_ACTIVE_LOW, IPT_START2);
        /*
         * service coin must be active for 19 frames to be consistently
         * recognized
         */
        // PORT_BIT_IMPULSE( 0x04, IP_ACTIVE_LOW, IPT_SERVICE1, 19 );
        PORT_BIT(0x04, IP_ACTIVE_LOW, IPT_SERVICE1);
        PORT_BIT(0x08, IP_ACTIVE_LOW, IPT_COIN1);
        PORT_BIT(0xf0, IP_ACTIVE_LOW, IPT_UNUSED);
    }

    void IN1_PORT() {
        PORT_BIT(0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY);
        PORT_BIT(0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT | IPF_8WAY);
        PORT_BIT(0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN | IPF_8WAY);
        PORT_BIT(0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_UP | IPF_8WAY);
        PORT_BIT(0x10, IP_ACTIVE_LOW, IPT_UNKNOWN); /* probably unused */
        PORT_BIT(0x20, IP_ACTIVE_LOW, IPT_BUTTON2);
        PORT_BIT(0x40, IP_ACTIVE_LOW, IPT_UNKNOWN); /* probably unused */
        PORT_BIT(0x80, IP_ACTIVE_LOW, IPT_BUTTON1);
    }

    void IN2_PORT() {
        PORT_BIT(0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_COCKTAIL);
        PORT_BIT(0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT | IPF_8WAY | IPF_COCKTAIL);
        PORT_BIT(0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN | IPF_8WAY | IPF_COCKTAIL);
        PORT_BIT(0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_UP | IPF_8WAY | IPF_COCKTAIL);
        PORT_BIT(0x10, IP_ACTIVE_LOW, IPT_COIN2);
        PORT_BIT(0x20, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_COCKTAIL);
        PORT_BIT(0x40, IP_ACTIVE_LOW, IPT_UNKNOWN); /* probably unused */
        PORT_BIT(0x80, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_COCKTAIL);
    }

    void COINAGE_DSW() {
        /* TODO: support the different settings which happen in Coin Mode 2 */
        PORT_DIPNAME(0xf0, 0xf0, DEF_STR2(Coinage)); /* mapped on coin mode 1 */
        PORT_DIPSETTING(0x90, DEF_STR2(_7C_1C));
        PORT_DIPSETTING(0xa0, DEF_STR2(_6C_1C));
        PORT_DIPSETTING(0xb0, DEF_STR2(_5C_1C));
        PORT_DIPSETTING(0xc0, DEF_STR2(_4C_1C));
        PORT_DIPSETTING(0xd0, DEF_STR2(_3C_1C));
        PORT_DIPSETTING(0xe0, DEF_STR2(_2C_1C));
        PORT_DIPSETTING(0xf0, DEF_STR2(_1C_1C));
        PORT_DIPSETTING(0x70, DEF_STR2(_1C_2C));
        PORT_DIPSETTING(0x60, DEF_STR2(_1C_3C));
        PORT_DIPSETTING(0x50, DEF_STR2(_1C_4C));
        PORT_DIPSETTING(0x40, DEF_STR2(_1C_5C));
        PORT_DIPSETTING(0x30, DEF_STR2(_1C_6C));
        PORT_DIPSETTING(0x20, DEF_STR2(_1C_7C));
        PORT_DIPSETTING(0x10, DEF_STR2(_1C_8C));
        PORT_DIPSETTING(0x00, DEF_STR2(Free_Play));
        /* setting 0x80 give 1 Coin/1 Credit */
    }

    private boolean ipt_ldrun() {
        PORT_START(); /* IN0 */
        IN0_PORT();

        PORT_START(); /* IN1 */
        IN1_PORT();

        PORT_START(); /* IN2 */
        IN2_PORT();

        PORT_START(); /* DSW1 */
        PORT_DIPNAME(0x03, 0x03, "Timer");
        PORT_DIPSETTING(0x03, "Slow");
        PORT_DIPSETTING(0x02, "Medium");
        PORT_DIPSETTING(0x01, "Fast");
        PORT_DIPSETTING(0x00, "Fastest");
        PORT_DIPNAME(0x0c, 0x0c, DEF_STR2(Lives));
        PORT_DIPSETTING(0x08, "2");
        PORT_DIPSETTING(0x0c, "3");
        PORT_DIPSETTING(0x04, "4");
        PORT_DIPSETTING(0x00, "5");
        COINAGE_DSW();

        PORT_START(); /* DSW2 */
        PORT_DIPNAME(0x01, 0x01, DEF_STR2(Flip_Screen));
        PORT_DIPSETTING(0x01, DEF_STR2(Off));
        PORT_DIPSETTING(0x00, DEF_STR2(On));
        PORT_DIPNAME(0x02, 0x00, DEF_STR2(Cabinet));
        PORT_DIPSETTING(0x00, DEF_STR2(Upright));
        PORT_DIPSETTING(0x02, DEF_STR2(Cocktail));
        /*
         * This activates a different coin mode. Look at the dip switch setting
         * schematic
         */
        PORT_DIPNAME(0x04, 0x04, "Coin Mode");
        PORT_DIPSETTING(0x04, "Mode 1");
        PORT_DIPSETTING(0x00, "Mode 2");
        PORT_DIPNAME(0x08, 0x08, DEF_STR2(Unknown));
        PORT_DIPSETTING(0x08, DEF_STR2(Off));
        PORT_DIPSETTING(0x00, DEF_STR2(On));
        /* In stop mode, press 2 to stop and 1 to restart */
        PORT_BITX(0x10, 0x10, IPT_DIPSWITCH_NAME | IPF_CHEAT, "Stop Mode", IP_KEY_NONE, IP_JOY_NONE);
        PORT_DIPSETTING(0x10, DEF_STR2(Off));
        PORT_DIPSETTING(0x00, DEF_STR2(On));
        /* In level selection mode, press 1 to select and 2 to restart */
        PORT_BITX(0x20, 0x20, IPT_DIPSWITCH_NAME | IPF_CHEAT, "Level Selection Mode", IP_KEY_NONE, IP_JOY_NONE);
        PORT_DIPSETTING(0x20, DEF_STR2(Off));
        PORT_DIPSETTING(0x00, DEF_STR2(On));
        PORT_BITX(0x40, 0x40, IPT_DIPSWITCH_NAME | IPF_CHEAT, "Invulnerability", IP_KEY_NONE, IP_JOY_NONE);
        PORT_DIPSETTING(0x40, DEF_STR2(Off));
        PORT_DIPSETTING(0x00, DEF_STR2(On));
        PORT_SERVICE(0x80, IP_ACTIVE_LOW);
        return true;
    }

    int[][] tilelayout_1024 = { { 8 }, { 8 }, /* 8*8 characters */
    { 1024 }, /* 1024 characters */
    { 3 }, /* 3 bits per pixel */
    { 0, 1024 * 8 * 8, 2 * 1024 * 8 * 8 }, { 0, 1, 2, 3, 4, 5, 6, 7 },
            { 0 * 8, 1 * 8, 2 * 8, 3 * 8, 4 * 8, 5 * 8, 6 * 8, 7 * 8 }, { 8 * 8 } /*
                                                                                     * every
                                                                                     * char
                                                                                     * takes
                                                                                     * 8
                                                                                     * consecutive
                                                                                     * bytes
                                                                                     */
    };

    int[][] tilelayout_2048 = { { 8 }, { 8 }, /* 8*8 characters */
    { 2048 }, /* 1024 characters */
    { 3 }, /* 3 bits per pixel */
    { 0, 2048 * 8 * 8, 2 * 2048 * 8 * 8 }, { 0, 1, 2, 3, 4, 5, 6, 7 },
            { 0 * 8, 1 * 8, 2 * 8, 3 * 8, 4 * 8, 5 * 8, 6 * 8, 7 * 8 }, { 8 * 8 } /*
                                                                                     * every
                                                                                     * char
                                                                                     * takes
                                                                                     * 8
                                                                                     * consecutive
                                                                                     * bytes
                                                                                     */
    };

    int[][] spritelayout_256 = {
            { 16 },
            { 16 },
            { RGN_FRAC(1, 3) },
            { 3 },
            { RGN_FRAC(0, 3), RGN_FRAC(1, 3), RGN_FRAC(2, 3) },
            { 0, 1, 2, 3, 4, 5, 6, 7, 16 * 8 + 0, 16 * 8 + 1, 16 * 8 + 2, 16 * 8 + 3, 16 * 8 + 4, 16 * 8 + 5,
                    16 * 8 + 6, 16 * 8 + 7 },
            { 0 * 8, 1 * 8, 2 * 8, 3 * 8, 4 * 8, 5 * 8, 6 * 8, 7 * 8, 8 * 8, 9 * 8, 10 * 8, 11 * 8, 12 * 8, 13 * 8,
                    14 * 8, 15 * 8 }, { 32 * 8 } };

    int[][] spritelayout_512 = {
            { 16 },
            { 16 },
            { 512 },
            { 3 },
            { 0, 512 * 32 * 8, 2 * 512 * 32 * 8 },
            { 0, 1, 2, 3, 4, 5, 6, 7, 16 * 8 + 0, 16 * 8 + 1, 16 * 8 + 2, 16 * 8 + 3, 16 * 8 + 4, 16 * 8 + 5,
                    16 * 8 + 6, 16 * 8 + 7 },
            { 0 * 8, 1 * 8, 2 * 8, 3 * 8, 4 * 8, 5 * 8, 6 * 8, 7 * 8, 8 * 8, 9 * 8, 10 * 8, 11 * 8, 12 * 8, 13 * 8,
                    14 * 8, 15 * 8 }, { 32 * 8 } };

    int[][] spritelayout_1024 = {
            { 16 },
            { 16 },
            { 1024 },
            { 3 },
            { 0, 1024 * 32 * 8, 2 * 1024 * 32 * 8 },
            { 0, 1, 2, 3, 4, 5, 6, 7, 16 * 8 + 0, 16 * 8 + 1, 16 * 8 + 2, 16 * 8 + 3, 16 * 8 + 4, 16 * 8 + 5,
                    16 * 8 + 6, 16 * 8 + 7 },
            { 0 * 8, 1 * 8, 2 * 8, 3 * 8, 4 * 8, 5 * 8, 6 * 8, 7 * 8, 8 * 8, 9 * 8, 10 * 8, 11 * 8, 12 * 8, 13 * 8,
                    14 * 8, 15 * 8 }, { 32 * 8 } };

    private boolean ldrun_gfxdecodeinfo() {
        GDI_ADD(REGION_GFX1, 0, tilelayout_1024, 0, 32); /* use colors 0-255 */
        GDI_ADD(REGION_GFX2, 0, spritelayout_256, 256, 32); /*
                                                             * use colors
                                                             * 256-511
                                                             */
        GDI_ADD(-1); /* end of array */
        return true;
    };

    private boolean ldrun2_gfxdecodeinfo() {
        GDI_ADD(REGION_GFX1, 0, tilelayout_1024, 0, 32); /* use colors 0-255 */
        GDI_ADD(REGION_GFX2, 0, spritelayout_512, 256, 32); /*
                                                             * use colors
                                                             * 256-511
                                                             */
        GDI_ADD(-1); /* end of array */
        return true;
    };

    private boolean ldrun3_gfxdecodeinfo() {
        GDI_ADD(REGION_GFX1, 0, tilelayout_2048, 0, 32); /* use colors 0-255 */
        GDI_ADD(REGION_GFX2, 0, spritelayout_512, 256, 32); /*
                                                             * use colors
                                                             * 256-511
                                                             */
        GDI_ADD(-1); /* end of array */
        return true;
    };

    private boolean ldrun4_gfxdecodeinfo() {
        GDI_ADD(REGION_GFX1, 0, tilelayout_2048, 0, 32); /* use colors 0-255 */
        GDI_ADD(REGION_GFX2, 0, spritelayout_1024, 256, 32); /*
                                                                 * use colors
                                                                 * 256-511
                                                                 */
        GDI_ADD(-1); /* end of array */
        return true;
    };

    private boolean kungfum_gfxdecodeinfo() {
        GDI_ADD(REGION_GFX1, 0, tilelayout_1024, 0, 32); /* use colors 0-255 */
        GDI_ADD(REGION_GFX2, 0, spritelayout_1024, 32 * 8, 32); /*
                                                                 * use colors
                                                                 * 256-511
                                                                 */
        GDI_ADD(-1); /* end of array */
        return true;
    };

    /**
     * @return
     */
    private boolean mdrv_kungfum() {
        /* basic machine hardware */
        MDRV_CPU_ADD_TAG("main", Z80, 24000000 / 6);
        MDRV_CPU_MEMORY(ldrun_readmem(), kungfum_writemem());
        MDRV_CPU_PORTS(ldrun_readport(), ldrun_writeport());
        MDRV_CPU_VBLANK_INT(irq0_line_hold, 1);

        MDRV_FRAMES_PER_SECOND(60);
        MDRV_VBLANK_DURATION(1790); /*
                                     * frames per second and vblank duration
                                     * from the Lode Runner manual
                                     */

        /* video hardware */
        MDRV_VIDEO_ATTRIBUTES(VIDEO_TYPE_RASTER);
        MDRV_SCREEN_SIZE(64 * 8, 32 * 8);
        MDRV_VISIBLE_AREA(16 * 8, (64 - 16) * 8 - 1, 0 * 8, 32 * 8 - 1);
        MDRV_GFXDECODE(kungfum_gfxdecodeinfo());
        MDRV_PALETTE_LENGTH(512);

        MDRV_PALETTE_INIT(irem_pi);
        MDRV_VIDEO_START(kungfum_vs);
        MDRV_VIDEO_UPDATE(v.kungfum_update());

        /* sound hardware */
        // MDRV_IMPORT_FROM(irem_audio);
        return true;
    }

    public boolean mdrv_ldrun() {

        /* basic machine hardware */
        MDRV_CPU_ADD_TAG("main", Z80, 24000000 / 6);
        MDRV_CPU_MEMORY(ldrun_readmem(), ldrun_writemem());
        MDRV_CPU_PORTS(ldrun_readport(), ldrun_writeport());
        MDRV_CPU_VBLANK_INT(irq0_line_hold, 1);

        MDRV_FRAMES_PER_SECOND(55);
        MDRV_VBLANK_DURATION(1790); /*
                                     * frames per second and vblank duration
                                     * from the Lode Runner manual
                                     */

        /* video hardware */
        MDRV_VIDEO_ATTRIBUTES(VIDEO_TYPE_RASTER);
        MDRV_SCREEN_SIZE(64 * 8, 32 * 8);
        MDRV_VISIBLE_AREA((64 * 8 - 384) / 2, 64 * 8 - (64 * 8 - 384) / 2 - 1, 0 * 8, 32 * 8 - 1);
        MDRV_GFXDECODE(ldrun_gfxdecodeinfo());
        MDRV_PALETTE_LENGTH(512);

        MDRV_PALETTE_INIT(irem_pi);
        MDRV_VIDEO_START(ldrun_vs);
        MDRV_VIDEO_UPDATE(ldrun_vu);

        /* sound hardware */
        // MDRV_IMPORT_FROM(irem_audio);
        return true;
    }

    public boolean mdrv_ldrun2() {

        /* basic machine hardware */
        MDRV_CPU_ADD_TAG("main", Z80, 24000000 / 6);
        MDRV_CPU_MEMORY(ldrun2_readmem(), ldrun2_writemem());
        MDRV_CPU_PORTS(ldrun2_readport(), ldrun2_writeport());
        MDRV_CPU_VBLANK_INT(irq0_line_hold, 1);

        MDRV_FRAMES_PER_SECOND(55);
        MDRV_VBLANK_DURATION(1790); /*
                                     * frames per second and vblank duration
                                     * from the Lode Runner manual
                                     */

        /* video hardware */
        MDRV_VIDEO_ATTRIBUTES(VIDEO_TYPE_RASTER);
        MDRV_SCREEN_SIZE(64 * 8, 32 * 8);
        MDRV_VISIBLE_AREA((64 * 8 - 384) / 2, 64 * 8 - (64 * 8 - 384) / 2 - 1, 0 * 8, 32 * 8 - 1);
        MDRV_GFXDECODE(ldrun2_gfxdecodeinfo());
        MDRV_PALETTE_LENGTH(512);

        MDRV_PALETTE_INIT(irem_pi);
        MDRV_VIDEO_START(ldrun_vs);
        MDRV_VIDEO_UPDATE(ldrun_vu);

        /* sound hardware */
        // MDRV_IMPORT_FROM(irem_audio);
        return true;
    }

    public boolean mdrv_ldrun3() {

        /* basic machine hardware */
        MDRV_CPU_ADD_TAG("main", Z80, 24000000 / 6);
        MDRV_CPU_MEMORY(ldrun3_readmem(), ldrun3_writemem());
        MDRV_CPU_PORTS(ldrun_readport(), ldrun3_writeport());
        MDRV_CPU_VBLANK_INT(irq0_line_hold, 1);

        MDRV_FRAMES_PER_SECOND(55);
        MDRV_VBLANK_DURATION(1790); /*
                                     * frames per second and vblank duration
                                     * from the Lode Runner manual
                                     */

        /* video hardware */
        MDRV_VIDEO_ATTRIBUTES(VIDEO_TYPE_RASTER);
        MDRV_SCREEN_SIZE(64 * 8, 32 * 8);
        MDRV_VISIBLE_AREA((64 * 8 - 384) / 2, 64 * 8 - (64 * 8 - 384) / 2 - 1, 0 * 8, 32 * 8 - 1);
        MDRV_GFXDECODE(ldrun3_gfxdecodeinfo());
        MDRV_PALETTE_LENGTH(512);

        MDRV_PALETTE_INIT(irem_pi);
        MDRV_VIDEO_START(ldrun_vs);
        MDRV_VIDEO_UPDATE(ldrun_vu);

        /* sound hardware */
        // MDRV_IMPORT_FROM(irem_audio);
        return true;
    }

    public boolean mdrv_ldrun4() {

        /* basic machine hardware */
        MDRV_CPU_ADD_TAG("main", Z80, 24000000 / 6);
        MDRV_CPU_MEMORY(ldrun_readmem(), ldrun_writemem());
        MDRV_CPU_PORTS(ldrun_readport(), ldrun_writeport());
        MDRV_CPU_VBLANK_INT(irq0_line_hold, 1);

        MDRV_FRAMES_PER_SECOND(55);
        MDRV_VBLANK_DURATION(1790); /*
                                     * frames per second and vblank duration
                                     * from the Lode Runner manual
                                     */

        /* video hardware */
        MDRV_VIDEO_ATTRIBUTES(VIDEO_TYPE_RASTER);
        MDRV_SCREEN_SIZE(64 * 8, 32 * 8);
        MDRV_VISIBLE_AREA((64 * 8 - 384) / 2, 64 * 8 - (64 * 8 - 384) / 2 - 1, 0 * 8, 32 * 8 - 1);
        MDRV_GFXDECODE(ldrun4_gfxdecodeinfo());
        MDRV_PALETTE_LENGTH(512);

        MDRV_PALETTE_INIT(irem_pi);
        MDRV_VIDEO_START(ldrun_vs);
        MDRV_VIDEO_UPDATE(ldrun_vu);

        /* sound hardware */
        // MDRV_IMPORT_FROM(irem_audio);
        return true;
    }

    /***************************************************************************
     * 
     * Game driver(s)
     * 
     **************************************************************************/
    private boolean rom_kungfum() {
        ROM_REGION(0x10000, REGION_CPU1, 0); /* 64k for code */
        ROM_LOAD("a-4e-c.bin", 0x0000, 0x4000, 0xb6e2d083);
        ROM_LOAD("a-4d-c.bin", 0x4000, 0x4000, 0x7532918e);

        ROM_REGION(0x10000, REGION_CPU2, 0); /* 64k for the audio CPU (6803); */
        ROM_LOAD("a-3e-.bin", 0xa000, 0x2000, 0x58e87ab0); /*
                                                             * samples (ADPCM
                                                             * 4-bit);
                                                             */
        ROM_LOAD("a-3f-.bin", 0xc000, 0x2000, 0xc81e31ea); /*
                                                             * samples (ADPCM
                                                             * 4-bit);
                                                             */
        ROM_LOAD("a-3h-.bin", 0xe000, 0x2000, 0xd99fb995);

        ROM_REGION(0x06000, REGION_GFX1, ROMREGION_DISPOSE);
        ROM_LOAD("g-4c-a.bin", 0x00000, 0x2000, 0x6b2cc9c8); /* characters */
        ROM_LOAD("g-4d-a.bin", 0x02000, 0x2000, 0xc648f558);
        ROM_LOAD("g-4e-a.bin", 0x04000, 0x2000, 0xfbe9276e);

        ROM_REGION(0x18000, REGION_GFX2, ROMREGION_DISPOSE);
        ROM_LOAD("b-4k-.bin", 0x00000, 0x2000, 0x16fb5150); /* sprites */
        ROM_LOAD("b-4f-.bin", 0x02000, 0x2000, 0x67745a33);
        ROM_LOAD("b-4l-.bin", 0x04000, 0x2000, 0xbd1c2261);
        ROM_LOAD("b-4h-.bin", 0x06000, 0x2000, 0x8ac5ed3a);
        ROM_LOAD("b-3n-.bin", 0x08000, 0x2000, 0x28a213aa);
        ROM_LOAD("b-4n-.bin", 0x0a000, 0x2000, 0xd5228df3);
        ROM_LOAD("b-4m-.bin", 0x0c000, 0x2000, 0xb16de4f2);
        ROM_LOAD("b-3m-.bin", 0x0e000, 0x2000, 0xeba0d66b);
        ROM_LOAD("b-4c-.bin", 0x10000, 0x2000, 0x01298885);
        ROM_LOAD("b-4e-.bin", 0x12000, 0x2000, 0xc77b87d4);
        ROM_LOAD("b-4d-.bin", 0x14000, 0x2000, 0x6a70615f);
        ROM_LOAD("b-4a-.bin", 0x16000, 0x2000, 0x6189d626);

        ROM_REGION(0x0720, REGION_PROMS, 0);
        ROM_LOAD("g-1j-.bin", 0x0000, 0x0100, 0x668e6bca); 
        ROM_LOAD("b-1m-.bin", 0x0100, 0x0100, 0x76c05a9c); 
        ROM_LOAD("g-1f-.bin", 0x0200, 0x0100, 0x964b6495); 
        ROM_LOAD("b-1n-.bin", 0x0300, 0x0100, 0x23f06b99); 
        ROM_LOAD("g-1h-.bin", 0x0400, 0x0100, 0x550563e1); 
        ROM_LOAD("b-1l-.bin", 0x0500, 0x0100, 0x35e45021); 
        ROM_LOAD("b-5f-.bin", 0x0600, 0x0020, 0x7a601c3d); 
        /* sprites. Used at run time! */
        ROM_LOAD("b-6f-.bin", 0x0620, 0x0100, 0x82c20d12); 
        return true;
    }

    private boolean rom_ldrun() {
        ROM_REGION(0x10000, REGION_CPU1, 0); /* 64k for code */
        ROM_LOAD("lr-a-4e", 0x0000, 0x2000, 0x5d7e2a4d);
        ROM_LOAD("lr-a-4d", 0x2000, 0x2000, 0x96f20473);
        ROM_LOAD("lr-a-4b", 0x4000, 0x2000, 0xb041c4a9);
        ROM_LOAD("lr-a-4a", 0x6000, 0x2000, 0x645e42aa);

        ROM_REGION(0x10000, REGION_CPU2, 0); /* 64k for the audio CPU (6803) */
        ROM_LOAD("lr-a-3f", 0xc000, 0x2000, 0x7a96accd);
        ROM_LOAD("lr-a-3h", 0xe000, 0x2000, 0x3f7f3939);

        ROM_REGION(0x6000, REGION_GFX1, ROMREGION_DISPOSE);
        ROM_LOAD("lr-e-2d", 0x0000, 0x2000, 0x24f9b58d); /* characters */
        ROM_LOAD("lr-e-2j", 0x2000, 0x2000, 0x43175e08);
        ROM_LOAD("lr-e-2f", 0x4000, 0x2000, 0xe0317124);

        ROM_REGION(0x6000, REGION_GFX2, ROMREGION_DISPOSE);
        ROM_LOAD("lr-b-4k", 0x0000, 0x2000, 0x8141403e); /* sprites */
        ROM_LOAD("lr-b-3n", 0x2000, 0x2000, 0x55154154);
        ROM_LOAD("lr-b-4c", 0x4000, 0x2000, 0x924e34d0);

        ROM_REGION(0x0720, REGION_PROMS, 0);
        ROM_LOAD("lr-e-3m", 0x0000, 0x0100, 0x53040416); 
        ROM_LOAD("lr-b-1m", 0x0100, 0x0100, 0x4bae1c25); 
        ROM_LOAD("lr-e-3l", 0x0200, 0x0100, 0x67786037); 
        ROM_LOAD("lr-b-1n", 0x0300, 0x0100, 0x9cd3db94); 
        ROM_LOAD("lr-e-3n", 0x0400, 0x0100, 0x5b716837); 
        ROM_LOAD("lr-b-1l", 0x0500, 0x0100, 0x08d8cf9a); 
        ROM_LOAD("lr-b-5p", 0x0600, 0x0020, 0xe01f69e2); 
        /* sprites. Used at run time! */
        ROM_LOAD("lr-b-6f", 0x0620, 0x0100, 0x34d88d3c); 
        return true;
    }

    private boolean rom_ldrun2() {
        ROM_REGION(0x14000, REGION_CPU1, 0); /* 64k for code + 16k for banks */
        ROM_LOAD("lr2-a-4e.a", 0x00000, 0x2000, 0x22313327);
        ROM_LOAD("lr2-a-4d", 0x02000, 0x2000, 0xef645179);
        ROM_LOAD("lr2-a-4a.a", 0x04000, 0x2000, 0xb11ddf59);
        ROM_LOAD("lr2-a-4a", 0x06000, 0x2000, 0x470cc8a1);
        ROM_LOAD("lr2-h-1c.a", 0x10000, 0x2000, 0x7ebcadbc); /*
                                                                 * banked at
                                                                 * 8000-9fff
                                                                 */
        ROM_LOAD("lr2-h-1d.a", 0x12000, 0x2000, 0x64cbb7f9); /*
                                                                 * banked at
                                                                 * 8000-9fff
                                                                 */

        ROM_REGION(0x10000, REGION_CPU2, 0); /* 64k for the audio CPU (6803); */
        ROM_LOAD("lr2-a-3e", 0xa000, 0x2000, 0x853f3898);
        ROM_LOAD("lr2-a-3f", 0xc000, 0x2000, 0x7a96accd);
        ROM_LOAD("lr2-a-3h", 0xe000, 0x2000, 0x2a0e83ca);

        ROM_REGION(0x6000, REGION_GFX1, ROMREGION_DISPOSE);
        ROM_LOAD("lr2-h-1e", 0x00000, 0x2000, 0x9d63a8ff); /* characters */
        ROM_LOAD("lr2-h-1j", 0x02000, 0x2000, 0x40332bbd);
        ROM_LOAD("lr2-h-1h", 0x04000, 0x2000, 0x9404727d);

        ROM_REGION(0xc000, REGION_GFX2, ROMREGION_DISPOSE);
        ROM_LOAD("lr2-b-4k", 0x00000, 0x2000, 0x79909871); /* sprites */
        ROM_LOAD("lr2-b-4f", 0x02000, 0x2000, 0x06ba1ef4);
        ROM_LOAD("lr2-b-3n", 0x04000, 0x2000, 0x3cc5893f);
        ROM_LOAD("lr2-b-4n", 0x06000, 0x2000, 0x49c12f42);
        ROM_LOAD("lr2-b-4c", 0x08000, 0x2000, 0xfbe6d24c);
        ROM_LOAD("lr2-b-4e", 0x0a000, 0x2000, 0x75172d1f);

        ROM_REGION(0x0720, REGION_PROMS, 0);
        ROM_LOAD("lr2-h-3m", 0x0000, 0x0100, 0x2c5d834b); 
        ROM_LOAD("lr2-b-1m", 0x0100, 0x0100, 0x4ec9bb3d); 
        ROM_LOAD("lr2-h-3l", 0x0200, 0x0100, 0x3ae69aca); 
        ROM_LOAD("lr2-b-1n", 0x0300, 0x0100, 0x1daf1fa4); 
        ROM_LOAD("lr2-h-3n", 0x0400, 0x0100, 0x2b28aec5); 
        ROM_LOAD("lr2-b-1l", 0x0500, 0x0100, 0xc8fb708a); 
        ROM_LOAD("lr2-b-5p", 0x0600, 0x0020, 0xe01f69e2); 
        /* sprites. Used at run time! */
        ROM_LOAD("lr2-b-6f", 0x0620, 0x0100, 0x34d88d3c); 
        return true;
    }

    private boolean rom_ldrun3() {
        ROM_REGION( 0x10000, REGION_CPU1, 0 ) ;  /* 64k for code */
        ROM_LOAD( "lr3a4eb.bin",  0x0000, 0x4000, 0x09affc47 );
        ROM_LOAD( "lr3a4db.bin",  0x4000, 0x4000, 0x23a02178 );
        ROM_LOAD( "lr3a4bb.bin",  0x8000, 0x4000, 0x3d501a1a );

        ROM_REGION( 0x10000, REGION_CPU2, 0 );   /* 64k for the audio CPU (6803) */
        ROM_LOAD( "lr3-a-3d",     0x8000, 0x4000, 0x28be68cd );
        ROM_LOAD( "lr3-a-3f",     0xc000, 0x4000, 0xcb7186b7 );

        ROM_REGION( 0xc000, REGION_GFX1, ROMREGION_DISPOSE );
        ROM_LOAD( "lr3-n-2a",     0x00000, 0x4000, 0xf9b74dee );   /* characters */
        ROM_LOAD( "lr3-n-2c",     0x04000, 0x4000, 0xfef707ba );
        ROM_LOAD( "lr3-n-2b",     0x08000, 0x4000, 0xaf3d27b9 );

        ROM_REGION( 0x18000, REGION_GFX2, ROMREGION_DISPOSE );
        ROM_LOAD( "lr3b4kb.bin",  0x00000, 0x4000, 0x21ecd8c5 );   /* sprites */
        ROM_LOAD( "snxb4fb.bin",  0x04000, 0x4000, 0xed719d7b );
        ROM_LOAD( "lr3b3nb.bin",  0x08000, 0x4000, 0xda8cffab );
        ROM_LOAD( "snxb4nb.bin",  0x0c000, 0x4000, 0xdc675003 );
        ROM_LOAD( "snxb4cb.bin",  0x10000, 0x4000, 0x585aa244 );
        ROM_LOAD( "snxb4eb.bin",  0x14000, 0x4000, 0x2d3b69dd );

        ROM_REGION( 0x0820, REGION_PROMS, 0 );
        ROM_LOAD( "lr3-n-2l",     0x0000, 0x0100, 0xe880b86b ); /* character palette red component */
        ROM_LOAD( "lr3-b-1m",     0x0100, 0x0100, 0xf02d7167 ); /* sprite palette red component */
        ROM_LOAD( "lr3-n-2k",     0x0200, 0x0100, 0x047ee051 ); /* character palette green component */
        ROM_LOAD( "lr3-b-1n",     0x0300, 0x0100, 0x9e37f181 ); /* sprite palette green component */
        ROM_LOAD( "lr3-n-2m",     0x0400, 0x0100, 0x69ad8678 ); /* character palette blue component */
        ROM_LOAD( "lr3-b-1l",     0x0500, 0x0100, 0x5b11c41d ); /* sprite palette blue component */
        ROM_LOAD( "lr3-b-5p",     0x0600, 0x0020, 0xe01f69e2 );    /* sprite height, one entry per 32 */
                                                                /* sprites. Used at run time! */
        ROM_LOAD( "lr3-n-4f",     0x0620, 0x0100, 0xdf674be9 );    /* unknown */
        ROM_LOAD( "lr3-b-6f",     0x0720, 0x0100, 0x34d88d3c );    /* video timing - common to the other games */

        return true;
    }

    private boolean rom_ldrun4() {
        ROM_REGION(0x18000, REGION_CPU1, 0); /*
                                                 * 64k for code + 32k for banked
                                                 * ROM
                                                 */
        ROM_LOAD("lr4-a-4e", 0x00000, 0x4000, 0x5383e9bf);
        ROM_LOAD("lr4-a-4d.c", 0x04000, 0x4000, 0x298afa36);
        ROM_LOAD("lr4-v-4k", 0x10000, 0x8000, 0x8b248abd); /*
                                                             * banked at
                                                             * 8000-bfff
                                                             */

        ROM_REGION(0x10000, REGION_CPU2, 0); /* 64k for the audio CPU (6803); */
        ROM_LOAD("lr4-a-3d", 0x8000, 0x4000, 0x86c6d445);
        ROM_LOAD("lr4-a-3f", 0xc000, 0x4000, 0x097c6c0a);

        ROM_REGION(0xc000, REGION_GFX1, ROMREGION_DISPOSE);
        ROM_LOAD("lr4-v-2b", 0x00000, 0x4000, 0x4118e60a); /* characters */
        ROM_LOAD("lr4-v-2d", 0x04000, 0x4000, 0x542bb5b5);
        ROM_LOAD("lr4-v-2c", 0x08000, 0x4000, 0xc765266c);

        ROM_REGION(0x18000, REGION_GFX2, ROMREGION_DISPOSE);
        ROM_LOAD("lr4-b-4k", 0x00000, 0x4000, 0xe7fe620c); /* sprites */
        ROM_LOAD("lr4-b-4f", 0x04000, 0x4000, 0x6f0403db);
        ROM_LOAD("lr4-b-3n", 0x08000, 0x4000, 0xad1fba1b);
        ROM_LOAD("lr4-b-4n", 0x0c000, 0x4000, 0x0e568fab);
        ROM_LOAD("lr4-b-4c", 0x10000, 0x4000, 0x82c53669);
        ROM_LOAD("lr4-b-4e", 0x14000, 0x4000, 0x767a1352);

        ROM_REGION(0x0820, REGION_PROMS, 0);
        ROM_LOAD("lr4-v-1m", 0x0000, 0x0100, 0xfe51bf1d); 
        ROM_LOAD("lr4-b-1m", 0x0100, 0x0100, 0x5d8d17d0); 
        ROM_LOAD("lr4-v-1n", 0x0200, 0x0100, 0xda0658e5); 
        ROM_LOAD("lr4-b-1n", 0x0300, 0x0100, 0xda1129d2); 
        ROM_LOAD("lr4-v-1p", 0x0400, 0x0100, 0x0df23ebe); 
        ROM_LOAD("lr4-b-1l", 0x0500, 0x0100, 0x0d89b692); 
        ROM_LOAD("lr4-b-5p", 0x0600, 0x0020, 0xe01f69e2); 
        /* sprites. Used at run time! */
        ROM_LOAD("lr4-v-4h", 0x0620, 0x0100, 0xdf674be9); /* unknown */
        ROM_LOAD("lr4-b-6f", 0x0720, 0x0100, 0x34d88d3c); 
        return true;
    }

    public Machine getMachine(URL url, String name) {
        super.getMachine(url, name);
        super.setVideoEmulator(v);
        m = new jef.machine.BasicMachine();

        if (name.equals("ldrun")) {
            GAME(1984, rom_ldrun(), 0, mdrv_ldrun(), ipt_ldrun(), 0, ROT0, "Irem (licensed from Broderbund)",
                    "Lode Runner (set 1)");
        } else if (name.equals("ldrun2")) {
            GAME(1984, rom_ldrun2(), 0, mdrv_ldrun2(), ipt_ldrun(), 0, ROT0, "Irem (licensed from Broderbund)",
                    "Lode Runner 2");
        } else if (name.equals("ldrun3")) {
            GAME(1984, rom_ldrun3(), 0, mdrv_ldrun3(), ipt_ldrun(), 0, ROT0, "Irem (licensed from Broderbund)",
                    "Lode Runner 3");
        } else if (name.equals("ldrun4")) {
            GAME(1984, rom_ldrun4(), 0, mdrv_ldrun4(), ipt_ldrun(), 0, ROT0, "Irem (licensed from Broderbund)",
                    "Lode Runner 4");
        } else if (name.equals("kungfum")) {
            GAME(1984, rom_kungfum(), 0, mdrv_kungfum(), ipt_ldrun(), 0, ROT0, "Irem", "Kung Fu Master");
        }

        m.init(md);
        return (Machine) m;
    }

}
