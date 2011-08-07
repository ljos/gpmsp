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

public class Gberet extends MAMEVideo implements VideoEmulator,Vh_refresh,Vh_start,Vh_stop,Vh_convert_color_proms {

int flipscreen;
int gberet_scroll;
int gberet_spritebank;

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

	for (i = 0;i < TOTAL_COLORS(1);i++)
	{
		if ((color_prom[cptr] & 0x0f) != 0) COLOR(1,i, color_prom[cptr] & 0x0f);
		else COLOR(1,i, 0);
		cptr++;
	}

	for (i = 0;i < TOTAL_COLORS(0);i++)
	{
		COLOR(0,i, (color_prom[cptr++] & 0x0f) + 0x10);
	}
}

public int vh_start()
{
	/* Green Beret has a virtual screen twice as large as the visible screen */
	tmpbitmap = new BitMapImpl(2 * 256, 256);
	gberet_scroll = 0xe000;
	gberet_spritebank = 0xe043;

	return 0;
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
			//if (flipscreen)
			//{
			//	sx = 63 - sx;
			//	sy = 31 - sy;
			//	flipx = !flipx;
			//	flipy = !flipy;
			//}

			drawgfx(tmpbitmap,0,
					RAM[videoram + offs] + 4 * (RAM[colorram + offs] & 0x40),
					RAM[colorram + offs] & 0x0f,
					flipx,flipy,
					8*sx,8*sy,
					0,GfxManager.TRANSPARENCY_NONE,0);
		}
	}


	/* copy the temporary bitmap to the screen */
	{
		int[] scroll = new int[32];


		//if (flipscreen)
		//{
		//	for (offs = 0;offs < 32;offs++)
		//		scroll[31-offs] = 256 + (RAM[gberet_scroll + offs] + 256 * RAM[gberet_scroll + offs + 32]);
		//}
		//else
		//{
			for (offs = 0;offs < 32;offs++)
				scroll[offs] = -(RAM[gberet_scroll + offs] + 256 * RAM[gberet_scroll + offs + 32]);
		//}

		copyscrollbitmap(bitmap,tmpbitmap,32,scroll,0,0,visible,GfxManager.TRANSPARENCY_NONE,0);
	}


	/* Draw the sprites. */
	{
		int sr;

		if ((RAM[gberet_spritebank] & 0x08) != 0)
			sr = spriteram_2;
		else sr = spriteram;

		for (offs = 0;offs < spriteram_size;offs += 4)
		{
			if (RAM[sr + offs+3] != 0)
			{
				int sx,sy,flipx,flipy;

				sx = RAM[sr + offs+2] - 2*(RAM[sr + offs+1] & 0x80);
				sy = RAM[sr + offs+3];
				flipx = RAM[sr + offs+1] & 0x10;
				flipy = RAM[sr + offs+1] & 0x20;

				//if (flipscreen)
				//{
				//	sx = 240 - sx;
				//	sy = 240 - sy;
				//	flipx = !flipx;
				//	flipy = !flipy;
				//}

				drawgfx(bitmap,Machine_gfx[1],
						RAM[sr + offs+0] + ((RAM[sr + offs+1] & 0x40) << 2),
						RAM[sr + offs+1] & 0x0f,
						flipx,flipy,
						sx,sy,
						GfxManager.TRANSPARENCY_COLOR,0);
			}
		}
	}
	return bitmap;
}

}