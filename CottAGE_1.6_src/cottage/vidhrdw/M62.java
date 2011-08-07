/***************************************************************************

 Video Hardware for Irem Games:
 Battle Road, Lode Runner, Kid Niki, Spelunker

 Tile/sprite priority system (for the Kung Fu Master M62 board):
 - Tiles with color code >= N (where N is set by jumpers) have priority over
 sprites. Only bits 1-4 of the color code are used, bit 0 is ignored.

 - Two jumpers select whether bit 5 of the sprite color code should be used
 to index the high address pin of the color PROMs, or to select high
 priority over tiles (or both, but is this used by any game?)

 ***************************************************************************/

package cottage.vidhrdw;

import jef.machine.MachineDriver;
import jef.map.ReadHandler;
import jef.map.WriteHandler;
import jef.video.BitMap;
import jef.video.BitMapImpl;
import jef.video.GfxManager;
import jef.video.Vh_convert_color_proms;
import jef.video.Vh_refresh;
import jef.video.Vh_start;
import jef.video.Vh_stop;
import jef.video.VideoEmulator;

import cottage.mame.MAMEVideo;

public class M62 extends MAMEVideo implements VideoEmulator, Vh_refresh, Vh_start, Vh_stop, Vh_convert_color_proms {

    static int flipscreen;

    static int sprite_height_prom;

    static int irem_background_hscroll;

    static int irem_background_vscroll;

    int scrollx[] = new int[32];

    ReadHandler input_port_4_r;

    /* COTTAGE VIDEO INITIALIZATION */
    public void init(MachineDriver md) {
        super.init_bis(md);
        super.init(md);
        input_port_4_r = md.input[4];
    }

    /***************************************************************************
     * 
     * Convert the color PROMs into a more useable format.
     * 
     * Kung Fu Master has a six 256x4 palette PROMs (one per gun; three for
     * characters, three for sprites). I don't know the exact values of the
     * resistors between the RAM and the RGB output. I assumed these values (the
     * same as Commando)
     * 
     * bit 3 -- 220 ohm resistor -- RED/GREEN/BLUE -- 470 ohm resistor --
     * RED/GREEN/BLUE -- 1 kohm resistor -- RED/GREEN/BLUE bit 0 -- 2.2kohm
     * resistor -- RED/GREEN/BLUE
     * 
     **************************************************************************/
    public void palette_init() {
        int i;

        int cp = 0;
        for (i = 0; i < Machine_drv_total_colors; i++) {
            int bit0, bit1, bit2, bit3, r, g, b;

            /* red component */
            bit0 = (color_prom[i] >> 0) & 0x01;
            bit1 = (color_prom[i] >> 1) & 0x01;
            bit2 = (color_prom[i] >> 2) & 0x01;
            bit3 = (color_prom[i] >> 3) & 0x01;
            r = 0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3;
            /* green component */
            bit0 = (color_prom[i + Machine_drv_total_colors] >> 0) & 0x01;
            bit1 = (color_prom[i + Machine_drv_total_colors] >> 1) & 0x01;
            bit2 = (color_prom[i + Machine_drv_total_colors] >> 2) & 0x01;
            bit3 = (color_prom[i + Machine_drv_total_colors] >> 3) & 0x01;
            g = 0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3;
            /* blue component */
            bit0 = (color_prom[i + 2 * Machine_drv_total_colors] >> 0) & 0x01;
            bit1 = (color_prom[i + 2 * Machine_drv_total_colors] >> 1) & 0x01;
            bit2 = (color_prom[i + 2 * Machine_drv_total_colors] >> 2) & 0x01;
            bit3 = (color_prom[i + 2 * Machine_drv_total_colors] >> 3) & 0x01;
            b = 0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3;

            palette_set_color(i, r, g, b);

            cp++;
        }

        cp += 2 * Machine_drv_total_colors;
        /* color_prom now points to the beginning of the sprite height table */

    }

    public int vh_start() {
        irem_background_hscroll = 0;
        irem_background_vscroll = 0;
        tmpbitmap = new BitMapImpl(384, 256);
        return 0;
    }

    public Vh_start kungfum_vs() {
        return new KunfuMStart();
    }

    public class KunfuMStart implements Vh_start {

        /*
         * (non-Javadoc)
         * 
         * @see jef.video.Vh_start#vh_start()
         */
        public int vh_start() {
            irem_background_hscroll = 0;
            irem_background_vscroll = 0;
            tmpbitmap = new BitMapImpl(512, 256);
            return 0;
        }

    }

    public WriteHandler ldrun3_vscroll_w() { return new Ldrun3_vscroll_w(); }
    public class Ldrun3_vscroll_w implements WriteHandler {

        public void write(int offset, int data) {
            irem_background_vscroll = data;
        }
    }

    public WriteHandler ldrun4_vscroll_w() { return new Ldrun4_vscroll_w(); }
    public class Ldrun4_vscroll_w implements WriteHandler {
        public void write(int offset, int data) {
            irem_background_hscroll_w(offset ^ 1, data);
        }
    }

    void irem_background_hscroll_w(int offset, int data) {
        switch (offset & 1) {
            case 0:
                irem_background_hscroll = (irem_background_hscroll & 0xff00) | data;
                break;

            case 1:
                irem_background_hscroll = (irem_background_hscroll & 0xff) | (data << 8);
                break;
        }
        // System.out.println(irem_background_hscroll + " - " + data);
    }

    void kungfum_scroll_low_w(int offset, int data) {
        irem_background_hscroll_w(0, data);
    }

    void kungfum_scroll_high_w(int offset, int data) {
        irem_background_hscroll_w(1, data);
    }

    public WriteHandler kungfum_scroll_low_w() {
        return new Kungfum_scroll_low_w();
    }

    public WriteHandler kungfum_scroll_high_w() {
        return new Kungfum_scroll_high_w();
    }

    public class Kungfum_scroll_low_w implements WriteHandler {
        public void write(int address, int data) {
            kungfum_scroll_low_w(address, data);
        }
    }

    public class Kungfum_scroll_high_w implements WriteHandler {
        public void write(int address, int data) {
            kungfum_scroll_high_w(address, data);
        }
    }

    public WriteHandler irem_flipscreen_w() {
        return new Irem_flipscreen_w();
    }

    public class Irem_flipscreen_w implements WriteHandler {
        public void write(int address, int data) {
            /* screen flip is handled both by software and hardware */
            data ^= ~input_port_4_r.read(4) & 1;

            if (flipscreen != (data & 1)) {
                flipscreen = data & 1;
                memset(dirtybuffer, 1, videoram_size);
            }

            // coin_counter_w(0,data & 2);
            // coin_counter_w(1,data & 4);
        }
    }

    private void draw_priority_sprites(int prioritylayer) {
        int offs;

        for (offs = 0; offs < spriteram_size; offs += 8) {
            int i, incr, code, col, flipx, flipy, sx, sy;

            if (prioritylayer == 0 || (prioritylayer != 0 && (RAM[spriteram + offs] & 0x10) != 0)) {
                code = RAM[spriteram + offs + 4] + ((RAM[spriteram + offs + 5] & 0x07) << 8);
                col = RAM[spriteram + offs + 0] & 0x0f;
                sx = 256 * (RAM[spriteram + offs + 7] & 1) + RAM[spriteram + offs + 6];
                sy = 256 + 128 - 15 - (256 * (RAM[spriteram + offs + 3] & 1) + RAM[spriteram + offs + 2]);
                flipx = RAM[spriteram + offs + 5] & 0x40;
                flipy = RAM[spriteram + offs + 5] & 0x80;

                i = color_prom[0x600 + ((code >> 5) & 0x1f)];
                // System.out.println("code " + code + " - " + i);
                if (i == 1) /* double height */
                {
                    code &= ~1;
                    sy -= 16;
                } else if (i == 2) /* quadruple height */
                {
                    i = 3;
                    code &= ~3;
                    sy -= 3 * 16;
                }

                // if (flipscreen!=0)
                // {
                // sx = 496 - sx;
                // sy = 242 - i*16 - sy; /* sprites are slightly misplaced by
                // the hardware */
                // flipx = ~flipx;
                // flipy = ~flipy;
                // }

                if (flipy != 0) {
                    incr = -1;
                    code += i;
                } else
                    incr = 1;

                do {
                    drawgfx(bitmap, Machine_gfx[1], code + i * incr, col, flipx, flipy, sx, sy + 16 * i,
                            Machine_visible_area, GfxManager.TRANSPARENCY_PEN, 0);

                    i--;
                } while (i >= 0);
            }
        }
    }

    void kungfum_draw_background() {
        int offs, i;

        /* for every character in the Video RAM, check if it has been modified */
        /* since last time and update it accordingly. */
        for (offs = videoram_size / 2 - 1; offs >= 0; offs--) {
            if (dirtybuffer[offs] || dirtybuffer[offs + 0x800]) {
                int sx, sy, flipx, flipy;

                dirtybuffer[offs] = dirtybuffer[offs + 0x800] = false;

                sx = offs % 64;
                sy = offs / 64;
                flipx = RAM[videoram + offs + 0x800] & 0x20;
                flipy = 0;
                // if (flipscreen)
                // {
                // sx = 63 - sx;
                // sy = 31 - sy;
                // flipx = !flipx;
                // flipy = !flipy;
                // }

                drawgfx(tmpbitmap, Machine_gfx[0], RAM[videoram + offs] + 4 * (RAM[videoram + offs + 0x800] & 0xc0),
                        RAM[videoram + offs + 0x800] & 0x1f, flipx, flipy, 8 * sx, 8 * sy, 0, TRANSPARENCY_NONE, 0);
            }
        }

        /* copy the temporary bitmap to the screen */
        {

            {
                for (i = 0; i < 6; i++)
                    scrollx[i] = 0;
                for (i = 6; i < 32; i++)
                    scrollx[i] = -irem_background_hscroll;
            }

            copyscrollbitmap(bitmap, tmpbitmap, 32, scrollx, 0, 0, getMachineDriver().getVisibleArea(),
                    TRANSPARENCY_NONE, 0);
        }
    }

    private void ldrun_draw_background(int prioritylayer) {
        int offs;

        /* for every character in the Video RAM, check if it has been modified */
        /* since last time and update it accordingly. */
        for (offs = videoram_size - 2; offs >= 0; offs -= 2) {
            if ((dirtybuffer[offs] || dirtybuffer[offs + 1])
                    && !(prioritylayer == 0 && (RAM[videoram + offs + 1] & 0x04) != 0)) {
                int sx, sy, flipx;

                dirtybuffer[offs] = false;
                dirtybuffer[offs + 1] = false;

                sx = (offs / 2) % 64;
                sy = (offs / 2) / 64;
                flipx = RAM[videoram + offs + 1] & 0x20;

                if (flipscreen != 0) {
                    sx = 63 - sx;
                    sy = 31 - sy;
                    flipx = ~flipx;
                }

                drawgfx(tmpbitmap, Machine_gfx[0], RAM[videoram + offs] + ((RAM[videoram + offs + 1] & 0xc0) << 2),
                        RAM[videoram + offs + 1] & 0x1f, flipx, flipscreen, 8 * sx, 8 * sy, 1,
                        GfxManager.TRANSPARENCY_NONE, 0);
            }
        }

        {
            int scrolly; /* ldrun3 only */

            //if (flipscreen != 0)
            //    scrolly[0] = irem_background_vscroll;
            //else
                scrolly = -irem_background_vscroll;

            if (prioritylayer != 0) {
                tmpbitmap.toBitMap(bitmap, 0, 0, 0, 0, 384, 256, 0);
                //copyscrollbitmap(bitmap,tmpbitmap,0,0,1,scrolly,TRANSPARENCY_PEN,Machine_pens[0]);
                // copyscrollbitmap(bitmap,tmpbitmap,0,0,1,scrolly,&Machine->visible_area,TRANSPARENCY_PEN,Machine->pens[0]);
            } else {
                tmpbitmap.toBitMap(bitmap, 0, 0, 0, 0, 384, 256, -1);
                //copyscrollbitmap(bitmap,tmpbitmap,0,0,1,scrolly,TRANSPARENCY_NONE,0);
                // copybitmap(bitmap,tmpbitmap,0,0,1,scrolly,Machine_visible_area,TRANSPARENCY_NONE,0);
            }
        }
    }

    public BitMap video_update() {
        ldrun_draw_background(0);
        draw_priority_sprites(0);
        ldrun_draw_background(1);
        draw_priority_sprites(1);
        return bitmap;
    }

    public Vh_refresh kungfum_update() {
        return new Kungf_update(this);
    }

    public class Kungf_update implements Vh_refresh {

        MAMEVideo v;

        public Kungf_update(M62 m62) {
            v = m62;
        }

        /*
         * (non-Javadoc)
         * 
         * @see jef.video.Vh_refresh#video_update()
         */
        public BitMap video_update() {
            kungfum_draw_background();
            draw_priority_sprites(0);
            draw_priority_sprites(1);
            return bitmap;
        }

        /*
         * (non-Javadoc)
         * 
         * @see jef.video.Vh_refresh#video_post_update()
         */
        public void video_post_update() {
            v.video_post_update();
        }

    }

}