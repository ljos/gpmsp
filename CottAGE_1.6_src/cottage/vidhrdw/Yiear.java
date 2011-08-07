/***************************************************************************

  yiear.c

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

public class Yiear extends MAMEVideo implements VideoEmulator,Vh_refresh,Vh_start,Vh_stop,Vh_convert_color_proms {

	public void init(MachineDriver md) {
		super.init_bis(md);
		super.init(md);
	}

/***************************************************************************

  Convert the color PROMs into a more useable format.

  Yie Ar Kung-Fu has one 32x8 palette PROM, connected to the RGB output this
  way:

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


		int p = 0;
		for (i = 0;i < Machine_drv_total_colors;i++) {
			int bit0,bit1,bit2,r,g,b;

			/* red component */
			bit0 = (color_prom[p] >> 0) & 0x01;
			bit1 = (color_prom[p] >> 1) & 0x01;
			bit2 = (color_prom[p] >> 2) & 0x01;
			r = 0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2;

			/* green component */
			bit0 = (color_prom[p] >> 3) & 0x01;
			bit1 = (color_prom[p] >> 4) & 0x01;
			bit2 = (color_prom[p] >> 5) & 0x01;
			g = 0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2;

			/* blue component */
			bit0 = 0;
			bit1 = (color_prom[p] >> 6) & 0x01;
			bit2 = (color_prom[p] >> 7) & 0x01;
			b = 0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2;

			palette_set_color(i,r,g,b);
			p++;
		}
	}

	public BitMap video_update() {
		int offs;

		/* for every character in the Video RAM, check if it has been modified */
		/* since last time and update it accordingly. */
		for (offs = videoram_size - 2;offs >= 0;offs -= 2)
		{
			if (dirtybuffer[offs] || dirtybuffer[offs + 1])
			{
				int sx,sy,flipx,flipy;


				dirtybuffer[offs] = dirtybuffer[offs + 1] = false;

				sx = (offs/2) % 32;
				sy = (offs/2) / 32;
				flipx = RAM[videoram + offs] & 0x80;
				flipy = RAM[videoram + offs] & 0x40;

				drawgfx(tmpbitmap,Machine_gfx[0],
					RAM[videoram + offs + 1] | ((RAM[videoram + offs] & 0x10) << 4),
					0,
					flipx,flipy,
					8*sx,8*sy,
					1,GfxManager.TRANSPARENCY_NONE,0);
			}
		}


		/* copy the temporary bitmap to the screen */
		copybitmap(bitmap,tmpbitmap,0,0,0,0,Machine_visible_area,GfxManager.TRANSPARENCY_NONE,0);

		/* draw sprites */
		for (offs = spriteram_size - 2;offs >= 0;offs -= 2)
		{
			int sx,sy,flipx,flipy;


			sy    =  240 - RAM[spriteram + offs + 1];
			sx    =  RAM[spriteram_2 + offs];
			flipx = ~RAM[spriteram + offs] & 0x40;
			flipy =  RAM[spriteram + offs] & 0x80;

			if (offs < 0x26)
			{
				sy++;	/* fix title screen & garbage at the bottom of the screen */
			}

			drawgfx(bitmap,Machine_gfx[1],
				RAM[spriteram_2 + offs + 1] + 256 * (RAM[spriteram + offs] & 1),
				0,
				flipx,flipy,
				sx,sy,
				Machine_visible_area,GfxManager.TRANSPARENCY_PEN,0);
		}

		return bitmap;
	}
}