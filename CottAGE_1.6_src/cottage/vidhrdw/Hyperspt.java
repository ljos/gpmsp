/***************************************************************************

  vidhrdw.c

  Functions to emulate the video hardware of the machine.

***************************************************************************/
package cottage.vidhrdw;

import jef.machine.MachineDriver;
import jef.video.BitMap;
import jef.video.BitMapImpl;
import jef.video.GfxManager;
import jef.video.Vh_convert_color_proms;
import jef.video.Vh_refresh;
import jef.video.Vh_start;
import jef.video.Vh_stop;
import jef.video.VideoEmulator;

import cottage.mame.MAMEVideo;

public class Hyperspt extends MAMEVideo implements VideoEmulator,
												 Vh_refresh,
												 Vh_start,
												 Vh_stop,
												 Vh_convert_color_proms {

int flipscreen;
int hyperspt_scroll = 0x10C0;

public void init(MachineDriver md) {
	super.init_bis(md);
	super.init(md);
}


/***************************************************************************

  Convert the color PROMs into a more useable format.

  Hyper Sports has one 32x8 palette PROM and two 256x4 lookup table PROMs
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
		COLOR(0,i, (color_prom[cptr++] & 0x0f) + 0x10);
}

public int vh_start() {
	/* Hyper Sports has a virtual screen twice as large as the visible screen */
	tmpbitmap = new BitMapImpl(32 * 8 * 2, 32 * 8);

	return 0;
}

public Vh_start roadf_vs() { return new Roadf_vs(); }
class Roadf_vs implements Vh_start {
	public int vh_start() {
		tmpbitmap = new BitMapImpl(32 * 8, 32 * 8 * 2);

		return 0;
	}
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

			sx = offs % 64;
			sy = offs / 64;
			flipx = RAM[colorram + offs] & 0x10;
			flipy = RAM[colorram + offs] & 0x20;

			drawgfx(tmpbitmap,0,
					RAM[videoram + offs] + ((RAM[colorram + offs] & 0x80) << 1) + ((RAM[colorram + offs] & 0x40) << 3),
					RAM[colorram + offs] & 0x0f,
					flipx,flipy,
					8*sx,8*sy,
					0,GfxManager.TRANSPARENCY_NONE,0);
		}
	}


	/* copy the temporary bitmap to the screen */
	{
		int[] scroll = new int[32];


		for (offs = 0;offs < 32;offs++)
			scroll[offs] = -(RAM[hyperspt_scroll + 2*offs] + 256 * (RAM[hyperspt_scroll + 2*offs+1] & 1));


		copyscrollbitmap(bitmap,tmpbitmap,32,scroll,0,0,visible_area(),GfxManager.TRANSPARENCY_NONE,0);
		//tmpbitmap.toBitMap(bitmap,0,0);

	}


	/* Draw the sprites. */
	for (offs = spriteram_size - 4;offs >= 0;offs -= 4)
	{
		int sx,sy,flipx,flipy;


		sx = RAM[spriteram + offs + 3];
		sy = 240 - RAM[spriteram + offs + 1];
		flipx = ~RAM[spriteram + offs] & 0x40;
		flipy = RAM[spriteram + offs] & 0x80;

		/* Note that this adjustement must be done AFTER handling flipscreen, thus */
		/* proving that this is a hardware related "feature" */
		sy += 1;

		drawgfx(bitmap,1,
				RAM[spriteram + offs + 2] + 8 * (RAM[spriteram + offs] & 0x20),
				RAM[spriteram + offs] & 0x0f,
				flipx,flipy,
				sx,sy,
				GfxManager.TRANSPARENCY_COLOR,0);

		/* redraw with wraparound */
		drawgfx(bitmap,1,
				RAM[spriteram + offs + 2] + 8 * (RAM[spriteram + offs] & 0x20),
				RAM[spriteram + offs] & 0x0f,
				flipx,flipy,
				sx-256,sy,
				GfxManager.TRANSPARENCY_COLOR,0);
	}
	return bitmap;
}

/* Only difference with Hyper Sports is the way tiles are selected (1536 tiles */
/* instad of 1024). Plus, it has 256 sprites instead of 512. */
public Vh_refresh roadf_vu() { return new Roadf_vu(); }
class Roadf_vu implements Vh_refresh {
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

				sx = 63 - (offs % 64);
				sy = 31 - (offs / 64);
				flipx = (~RAM[colorram + offs]) & 0x10;
				flipy = 1;

				drawgfx(tmpbitmap,0,
						RAM[videoram + offs] + ((RAM[colorram + offs] & 0x80) << 1) + ((RAM[colorram + offs] & 0x60) << 4),
						RAM[colorram + offs] & 0x0f,
						flipx,flipy,
						8*sx,8*sy,
						GfxManager.TRANSPARENCY_NONE,0);
			}
		}


		/* copy the temporary bitmap to the screen */
		{
			int[] scroll = new int[32];


			for (offs = 0;offs < 32;offs++)
				scroll[offs] = -(RAM[hyperspt_scroll + 2*offs] + 256 * (RAM[hyperspt_scroll + 2*offs+1] & 1));


			copyscrollbitmap(bitmap,tmpbitmap,32,scroll,0,0,visible_area(),GfxManager.TRANSPARENCY_NONE,0);
			//tmpbitmap.toBitMap(bitmap,0,-256);

		}


		/* Draw the sprites. */
		for (offs = spriteram_size - 4;offs >= 0;offs -= 4)
		{
			int sx,sy,flipx,flipy;


			sx = RAM[spriteram + offs + 3];
			sy = RAM[spriteram + offs + 1];
			flipx = ~RAM[spriteram + offs] & 0x40;
			flipy = ~RAM[spriteram + offs] & 0x80;

			/* Note that this adjustement must be done AFTER handling flipscreen, thus */
			/* proving that this is a hardware related "feature" */
			sy += 1;

			drawgfx(bitmap,1,
					RAM[spriteram + offs + 2] + 8 * (RAM[spriteram + offs] & 0x20),
					RAM[spriteram + offs] & 0x0f,
					flipx,flipy,
					sx,sy,
					GfxManager.TRANSPARENCY_COLOR,0);

			/* redraw with wraparound */
			drawgfx(bitmap,1,
					RAM[spriteram + offs + 2] + 8 * (RAM[spriteram + offs] & 0x20),
					RAM[spriteram + offs] & 0x0f,
					flipx,flipy,
					sx-256,sy,
					GfxManager.TRANSPARENCY_COLOR,0);
		}
		return bitmap;
	}
	public void video_post_update() {}
}

}