/***************************************************************************

	Bally/Midway Jr. Pac-Man

***************************************************************************/

package cottage.vidhrdw;

import cottage.mame.MAMEVideo;
import jef.map.*;
import jef.machine.*;
import jef.video.*;

public class Jrpacman extends MAMEVideo implements VideoEmulator,Vh_refresh,Vh_start,Vh_stop,Vh_convert_color_proms {

public int[] Fjrpacman_scroll = {0};
public int[] Fjrpacman_bgpriority = {0};
public int[] Fjrpacman_charbank = {0};
public int[] Fjrpacman_spritebank = {0};
public int[] Fjrpacman_palettebank = {0};
public int[] Fjrpacman_colortablebank = {0};
static int jrpacman_scroll, jrpacman_bgpriority;
static int jrpacman_charbank, jrpacman_spritebank;
static int jrpacman_palettebank, jrpacman_colortablebank;
static int flipscreen;


/* COTTAGE VIDEO INITIALIZATION */
public void init(MachineDriver md) {
	super.init_bis(md);
	super.init(md);
	jrpacman_scroll = Fjrpacman_scroll[0];
	jrpacman_bgpriority = Fjrpacman_bgpriority[0];
	jrpacman_charbank = Fjrpacman_charbank[0];
	jrpacman_spritebank = Fjrpacman_spritebank[0];
	jrpacman_palettebank = Fjrpacman_palettebank[0];
	jrpacman_colortablebank = Fjrpacman_colortablebank[0];
}

/***************************************************************************

	Convert the color PROMs into a more useable format.

	Jr. Pac Man has two 256x4 palette PROMs (the three msb of the address are
	grounded, so the effective colors are only 32) and one 256x4 color lookup
	table PROM.
	The palette PROMs are connected to the RGB output this way:

	bit 3 -- 220 ohm resistor  -- BLUE
	   -- 470 ohm resistor  -- BLUE
	   -- 220 ohm resistor  -- GREEN
	bit 0 -- 470 ohm resistor  -- GREEN

	bit 3 -- 1  kohm resistor  -- GREEN
	   -- 220 ohm resistor  -- RED
	   -- 470 ohm resistor  -- RED
	bit 0 -- 1  kohm resistor  -- RED

***************************************************************************/

public void palette_init()
{
	int i;
	int cp=0;

	for (i = 0;i < 32;i++)
	{
		int bit0,bit1,bit2,r,g,b;


		bit0 = (color_prom[i] >> 0) & 0x01;
		bit1 = (color_prom[i] >> 1) & 0x01;
		bit2 = (color_prom[i] >> 2) & 0x01;
		r = 0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2;
		bit0 = (color_prom[i] >> 3) & 0x01;
		bit1 = (color_prom[i+256] >> 0) & 0x01;
		bit2 = (color_prom[i+256] >> 1) & 0x01;
		g = 0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2;
		bit0 = 0;
		bit1 = (color_prom[i+256] >> 2) & 0x01;
		bit2 = (color_prom[i+256] >> 3) & 0x01;
		b = 0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2;
		palette_set_color(i,r,g,b);
	}

	cp += 2*256;

	for (i = 0;i < 64*4;i++)
	{
		/* chars */
		//colortable[i] = color_prom[i];
		color_table[0][i] = color_prom[cp];

		/* sprites */
		//if (color_prom[i]) colortable[i + 64*4] = color_prom[i] + 0x10;
		//else colortable[i + 64*4] = 0;
		if (color_prom[cp]!=0) color_table[1][i + 64*4] = color_prom[cp] + 0x10;
		else color_table[1][i + 64*4] = 0;
		cp++;
	}
}

/***************************************************************************

  Start the video hardware emulation.

***************************************************************************/
public int vh_start()
{
	return 0;
}

public WriteHandler jrpacman_videoram_w() { return new Jrpacman_videoram_w(); }
public class Jrpacman_videoram_w implements WriteHandler {
	public void write(int offset, int data) {
		if (RAM[offset] != data)
		{
			RAM[offset] = data;
            System.out.println(data);
		}
	}
}

public WriteHandler jrpacman_palettebank_w() { return new Jrpacman_palettebank_w(); }
public class Jrpacman_palettebank_w implements WriteHandler {
	public void write(int offset, int data) {
		if (jrpacman_palettebank != data)
		{
			jrpacman_palettebank = data;
			//memset(dirtybuffer,1,videoram_size);
		}
	}
}

public WriteHandler jrpacman_colortablebank_w() { return new Jrpacman_colortablebank_w(); }
public class Jrpacman_colortablebank_w implements WriteHandler {
	public void write(int offset, int data) {
		if (jrpacman_colortablebank != data)
		{
			jrpacman_colortablebank = data;
			//memset(dirtybuffer,1,videoram_size);
		}
	}
}

public WriteHandler jrpacman_charbank_w() { return new Jrpacman_charbank_w(); }
public class Jrpacman_charbank_w implements WriteHandler {
	public void write(int offset, int data) {
		if (jrpacman_charbank != data)
		{
			jrpacman_charbank = data;
			//memset(dirtybuffer,1,videoram_size);
		}
	}
}

/***************************************************************************

  Draw the game screen in the given mame_bitmap.
  Do NOT call osd_update_display() from this function, it will be called by
  the main emulation engine.

***************************************************************************/
int scrolly[] = new int[36];

public BitMap video_update()
{
	int i,offs;


    /* for every character in the Video RAM, check if it has been modified */
    /* since last time and update it accordingly. */
    for (offs = videoram_size - 1;offs >= 0;offs--)
    {
        //if (dirtybuffer[offs])
        //{
            int mx,my;

//
        //    dirtybuffer[offs] = 0;

            /* Jr. Pac Man's screen layout is quite awkward */
            mx = offs % 32;
            my = offs / 32;
            
            //System.out.println(videoram + "+" + offs + " -> " + RAM[videoram + offs]);

            if (my >= 2 && my < 60)
            {
                int sx, sy;

                if (my < 56)
                {
                    sy = my;
                    sx = mx+2;
                    //if (flipscreen)
                    //{
                    //    sx = 35 - sx;
                    //    sy = 55 - sy;
                    //}

                    drawgfx(tmpbitmap,0,
                            RAM[videoram + offs] + 256 * jrpacman_charbank,
                        /* color is set line by line */
                            (RAM[videoram + mx] & 0x1f) + 0x20 * (jrpacman_colortablebank & 1)
                                    + 0x40 * (jrpacman_palettebank & 1),
                            flipscreen,flipscreen,
                            8*sx,8*sy,
                            0,TRANSPARENCY_NONE,0);
                }
                else
                {
                    if (my >= 58)
                    {
                        sy = mx - 2;
                        sx = my - 58;
                        //if (flipscreen)
                        //{
                        //    sx = 35 - sx;
                        //    sy = 55 - sy;
                        //}

                        drawgfx(tmpbitmap,0,
                                RAM[videoram + offs],
                                (RAM[videoram + offs + 4*32] & 0x1f) + 0x20 * (jrpacman_colortablebank & 1)
                                        + 0x40 * (jrpacman_palettebank & 1),
                                flipscreen,flipscreen,
                                8*sx,8*sy,
                                0,TRANSPARENCY_NONE,0);
                    }
                    else
                    {
                        sy = mx - 2;
                        sx = my - 22;
                        //if (flipscreen)
                        //{
                        //    sx = 35 - sx;
                        //    sy = 55 - sy;
                        //}

                        drawgfx(tmpbitmap,0,
                                RAM[videoram + offs] + 0x100 * (jrpacman_charbank & 1),
                                (RAM[videoram + offs + 4*32] & 0x1f) + 0x20 * (jrpacman_colortablebank & 1)
                                        + 0x40 * (jrpacman_palettebank & 1),
                                flipscreen,flipscreen,
                                8*sx,8*sy,
                                0,TRANSPARENCY_NONE,0);
                    }
                }
            }
        }
    //}


    /* copy the temporary bitmap to the screen */


        for (i = 0;i < 2;i++)
            scrolly[i] = 0;
        for (i = 2;i < 34;i++)
            scrolly[i] = -jrpacman_scroll - 16;
        for (i = 34;i < 36;i++)
            scrolly[i] = 0;

        //if (flipscreen)
        //{
        //    for (i = 0;i < 36;i++)
        //        scrolly[i] = 224 - scrolly[i];
        //}

        //copyscrollbitmap(bitmap,tmpbitmap,0,0,36,scrolly,super.getMachineDriver().getVisibleArea(),TRANSPARENCY_NONE,0);
        bitmap.setPixels(tmpbitmap.getPixels());

	/* Draw the sprites. Note that it is important to draw them exactly in this */
	/* order, to have the correct priorities. */
	for (offs = spriteram_size - 2;offs > 2*2;offs -= 2)
	{
		drawgfx(bitmap,1,
				(RAM[spriteram+offs] >> 2) + 0x40 * (jrpacman_spritebank & 1),
				(RAM[spriteram+offs + 1] & 0x1f) + 0x20 * (jrpacman_colortablebank & 1)
						+ 0x40 * (jrpacman_palettebank & 1),
				RAM[spriteram+offs] & 1,RAM[spriteram+offs] & 2,
				272 - RAM[spriteram_2+offs + 1],RAM[spriteram_2+offs]-31,
				GfxManager.TRANSPARENCY_COLOR,0);
	}
	/* the first two sprites must be offset one pixel to the left */
	for (offs = 2*2;offs > 0;offs -= 2)
	{
		drawgfx(bitmap,1,
				(RAM[spriteram+offs] >> 2) + 0x40 * (jrpacman_spritebank & 1),
				(RAM[spriteram+offs + 1] & 0x1f) + 0x20 * (jrpacman_colortablebank & 1)
						+ 0x40 * (jrpacman_palettebank & 1),
				RAM[spriteram+offs] & 1,RAM[spriteram+offs] & 2,
				272 - RAM[spriteram_2+offs + 1],RAM[spriteram_2+offs]-30,
				GfxManager.TRANSPARENCY_COLOR,0);
	}

	return bitmap;
}

}