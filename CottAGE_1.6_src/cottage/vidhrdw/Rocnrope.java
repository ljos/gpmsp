/***************************************************************************

  vidhrdw.c

  Functions to emulate the video hardware of the machine.

***************************************************************************/
package cottage.vidhrdw;

import jef.machine.MachineDriver;
import jef.video.BitMap;
import jef.video.GfxManager;
import jef.video.Vh_convert_color_proms;
import jef.video.Vh_refresh;
import jef.video.Vh_start;
import jef.video.Vh_stop;
import jef.video.VideoEmulator;

import cottage.mame.MAMEVideo;

public class Rocnrope extends MAMEVideo implements VideoEmulator,
												 Vh_refresh,
												 Vh_start,
												 Vh_stop,
												 Vh_convert_color_proms {

int flipscreen;

public void init(MachineDriver md) {
	super.init_bis(md);
	super.init(md);
}

/***************************************************************************

  Convert the color PROMs into a more useable format.

  Roc'n Rope has one 32x8 palette PROM and two 256x4 lookup table PROMs
  (one for characters, one for sprites).
  The palette PROM is connected to the RGB output this way:

  bit 7 -- 220 ohm resistor  -- BLUE
        -- 470 ohm resistor  -- BLUE
        -- 220 ohm resistor  -- GREEN
        -- 470 ohm resistor  -- GREEN
        -- 1  kohm resistor  -- GREEN
        -- 220 ohm resistor  -- RED
        -- 470 ohm resistor  -- RED
  bit 0 -- 1  kohm resistor  -- RED

***************************************************************************/

public void palette_init() {

	int i;
	int cptr = 0;

	for (i = 0;i < total_colors;i++)
	{
		int bit0,bit1,bit2,r,g,b;

		/* red component */
		bit0 = (color_prom[i] >> 0) & 0x01;
		bit1 = (color_prom[i] >> 1) & 0x01;
		bit2 = (color_prom[i] >> 2) & 0x01;
		r = 0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2;
		/* green component */
		bit0 = (color_prom[i] >> 3) & 0x01;
		bit1 = (color_prom[i] >> 4) & 0x01;
		bit2 = (color_prom[i] >> 5) & 0x01;
		g = 0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2;
		/* blue component */
		bit0 = 0;
		bit1 = (color_prom[i] >> 6) & 0x01;
		bit2 = (color_prom[i] >> 7) & 0x01;
		b = 0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2;

		palette_set_color(i,r,g,b);
		cptr++;
	}

	/* color_prom now points to the beginning of the lookup table */

	/* sprites */
	for (i = 0;i < TOTAL_COLORS(1);i++)
		COLOR(1,i, color_prom[cptr++] & 0x0f);

	/* characters */
	for (i = 0;i < TOTAL_COLORS(0);i++)
		COLOR(0,i, color_prom[cptr++] & 0x0f);
}


public BitMap video_update() {
	int offs;


	/* for every character in the Video RAM, check if it has been modified */
	/* since last time and update it accordingly. */
	for (offs = videoram_size - 1;offs >= 0;offs--)
	{
		if (dirtybuffer[offs])
		{
			int sx,sy,flipx,flipy;


			dirtybuffer[offs] = false;

			sx = offs % 32;
			sy = offs / 32;
			flipx = RAM[colorram + offs] & 0x40;
			flipy = RAM[colorram + offs] & 0x20;

			drawgfx(tmpbitmap,0,
					RAM[videoram + offs] + 2 * (RAM[colorram + offs] & 0x80),
					RAM[colorram + offs] & 0x0f,
					flipx,flipy,
					8*sx,8*sy,
					GfxManager.TRANSPARENCY_NONE,0);
		}
	}


	/* copy the temporary bitmap to the screen */
	tmpbitmap.toPixels(pixels);


	/* Draw the sprites. */
	for (offs = spriteram_size - 2;offs >= 0;offs -= 2)
	{
		drawgfx(bitmap,1,
				RAM[spriteram + offs + 1],
				RAM[spriteram_2 + offs] & 0x0f,
				RAM[spriteram_2 + offs] & 0x40,~RAM[spriteram_2 + offs] & 0x80,
				240-RAM[spriteram + offs],RAM[spriteram_2 + offs + 1],
				GfxManager.TRANSPARENCY_COLOR,0);
	}

	return bitmap;
}


}