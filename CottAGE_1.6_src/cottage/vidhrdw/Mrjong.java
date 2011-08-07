/***************************************************************************

  vidhrdw.c

  Functions to emulate the video hardware of the machine.

***************************************************************************/
package cottage.vidhrdw;

import jef.machine.BasicMachine;
import jef.machine.MachineDriver;
import jef.map.WriteHandler;
import jef.video.BitMap;
import jef.video.GfxManager;
import jef.video.Vh_convert_color_proms;
import jef.video.Vh_refresh;
import jef.video.Vh_start;
import jef.video.Vh_stop;
import jef.video.VideoEmulator;

import cottage.mame.MAMEVideo;

public class Mrjong extends MAMEVideo implements VideoEmulator,Vh_refresh,Vh_start,Vh_stop,Vh_convert_color_proms {

	static int flipscreen;
	BasicMachine m;

	public void setMachine(BasicMachine ma)
	{
		m = ma;
	}

	public void init(MachineDriver md) {
		super.init_bis(md);
		super.init(md);
	}

/***************************************************************************

  Convert the color PROMs. (from vidhrdw/penco.c)

***************************************************************************/
public void palette_init()
{
	int i;

    	int cp = 0;
	for (i = 0; i < Machine_drv_total_colors; i++)
	{
		int bit0, bit1, bit2, r, g, b;

		/* red component */
		bit0 = (color_prom[cp] >> 0) & 0x01;
		bit1 = (color_prom[cp] >> 1) & 0x01;
		bit2 = (color_prom[cp] >> 2) & 0x01;
		r = 0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2;
		/* green component */
		bit0 = (color_prom[cp] >> 3) & 0x01;
		bit1 = (color_prom[cp] >> 4) & 0x01;
		bit2 = (color_prom[cp] >> 5) & 0x01;
		g = 0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2;
		/* blue component */
		bit0 = 0;
		bit1 = (color_prom[cp] >> 6) & 0x01;
		bit2 = (color_prom[cp] >> 7) & 0x01;
		b = 0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2;

		palette_set_color(i,r,g,b);
		cp++;
	}

	cp += 0x10;
	/* color_prom now points to the beginning of the lookup table */

	/* character lookup table */
	/* sprites use the same color lookup table as characters */
	for (i = 0; i < TOTAL_COLORS(0); i++)
	{
		COLOR(0, i, color_prom[cp] & 0x0f);
		COLOR(1, i, color_prom[cp++] & 0x0f);
	}
}

public WriteHandler mrjong_flipscreen_w() { return new Mrjong_flipscreen_w(); }
class Mrjong_flipscreen_w implements WriteHandler {
	public void write(int address, int data)
	{
	}
}

public BitMap video_update()
{
	int offs;

	/* Draw the tiles. */
	for (offs = (videoram_size - 1); offs > 0; offs--)
	{
		if (dirtybuffer[offs])
		{
			int tile;
			int color;
			int sx, sy;
			int flipx, flipy;

			dirtybuffer[offs] = false;

			tile = RAM[videoram + offs] | ((RAM[colorram + offs] & 0x20) << 3);
			flipx = (RAM[colorram + offs] & 0x40) >> 6;
			flipy = (RAM[colorram + offs] & 0x80) >> 7;
			color = RAM[colorram + offs] & 0x1f;

			sx = 31 - (offs % 32);
			sy = 31 - (offs / 32);

			if (flipscreen != 0)
			{
				sx = 31 - sx;
				sy = 31 - sy;
				flipx = ~flipx;
				flipy = ~flipy;
			}

			drawgfx(tmpbitmap, Machine_gfx[0],
					tile,
					color,
					flipx, flipy,
					8*sx, 8*sy,
					GfxManager.TRANSPARENCY_NONE, 0);
		}
	}
	copybitmap(bitmap, tmpbitmap, 0, 0, 0, 0, Machine_visible_area, GfxManager.TRANSPARENCY_NONE, 0);

	/* Draw the sprites. */
	for (offs = (spriteram_size - 4); offs >= 0; offs -= 4)
	{
		int sprt;
		int color;
		int sx, sy;
		int flipx, flipy;

		sprt = (((RAM[spriteram + offs + 1] >> 2) & 0x3f) | ((RAM[spriteram + offs + 3] & 0x20) << 1));
		flipx = (RAM[spriteram + offs + 1] & 0x01) >> 0;
		flipy = (RAM[spriteram + offs + 1] & 0x02) >> 1;
		color = (RAM[spriteram + offs + 3] & 0x1f);

		sx = 224 - RAM[spriteram + offs + 2];
		sy = RAM[spriteram + offs + 0];
		if (flipscreen != 0)
		{
			sx = 208 - sx;
			sy = 240 - sy;
			flipx = ~flipx;
			flipy = ~flipy;
		}

		drawgfx(bitmap, Machine_gfx[1],
				sprt,
				color,
				flipx, flipy,
				sx, sy,
				Machine_visible_area, GfxManager.TRANSPARENCY_PEN, 0);
	}

	return bitmap;
}

}