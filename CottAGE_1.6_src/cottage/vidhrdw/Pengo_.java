/*************************************************************************

	Sega Pengo

**************************************************************************

	This file is used by the Pengo and Pac Man drivers.
	They are almost identical, the only differences being the extra gfx bank
	in Pengo, and the need to compensate for an hardware sprite positioning
	"bug" in Pac Man.

**************************************************************************/

package cottage.vidhrdw;

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

public class Pengo_ extends MAMEVideo implements VideoEmulator,Vh_refresh,Vh_start,Vh_stop,Vh_convert_color_proms {

static int gfx_bank;
static int flipscreen;
static int xoffsethack;

/*static struct rectangle spritevisiblearea =
{
	2*8, 34*8-1,
	0*8, 28*8-1
};*/

/* COTTAGE VIDEO INITIALIZATION */
public void init(MachineDriver md) {
	super.init_bis(md);
	super.init(md);
}

/***************************************************************************

  Convert the color PROMs into a more useable format.

  Pac Man has a 32x8 palette PROM and a 256x4 color lookup table PROM.

  Pengo has a 32x8 palette PROM and a 1024x4 color lookup table PROM.

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

public Vh_convert_color_proms palette_init_pacman() { return new Palette_init_pacman(); }
public class Palette_init_pacman implements Vh_convert_color_proms {
	public void palette_init()
	{
		int i;

		int cp = 0;
		for (i = 0;i < Machine_drv_total_colors;i++)
		{
			int bit0,bit1,bit2,r,g,b;

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
		for (i = 0;i < TOTAL_COLORS(0);i++)
		{
			COLOR(0,i, color_prom[cp] & 0x0f);
			COLOR(1,i, color_prom[cp++] & 0x0f);
		}
	}
}

/***************************************************************************

  Start the video hardware emulation.

***************************************************************************/

public Vh_start video_start_pacman() { return new Video_start_pacman(); }
public class Video_start_pacman implements Vh_start {
	public int vh_start()
	{
		gfx_bank = 0;
		/* In the Pac Man based games (NOT Pengo) the first two sprites must be offset */
		/* one pixel to the left to get a more correct placement */
		xoffsethack = 1;

		return 0;
	}
}

/***************************************************************************

  Draw the game screen in the given mame_bitmap.
  Do NOT call osd_update_display() from this function, it will be called by
  the main emulation engine.

***************************************************************************/

public Vh_refresh video_update_pengo() { return new Video_update_pengo(); }
public class Video_update_pengo implements Vh_refresh {
	public void video_post_update() { };
	public BitMap video_update()
	{
		//struct rectangle spriteclip = spritevisiblearea;
		int offs;

		//sect_rect(&spriteclip, cliprect);

		for (offs = videoram_size - 1; offs > 0; offs--)
		{
			if (dirtybuffer[offs])
			{
				int mx,my,sx,sy;

				dirtybuffer[offs] = false;
				mx = offs % 32;
				my = offs / 32;

				if (my < 2)
				{
					if (mx < 2 || mx >= 30) continue; /* not visible */
					sx = my + 34;
					sy = mx - 2;
				}
				else if (my >= 30)
				{
					if (mx < 2 || mx >= 30) continue; /* not visible */
					sx = my - 30;
					sy = mx - 2;
				}
				else
				{
					sx = mx + 2;
					sy = my - 2;
				}

				if (flipscreen != 0)
				{
					sx = 35 - sx;
					sy = 27 - sy;
				}

				drawgfx(tmpbitmap,Machine_gfx[gfx_bank*2],
						RAM[videoram + offs],
						RAM[colorram + offs] & 0x1f,
						flipscreen,flipscreen,
						sx*8,sy*8,
						GfxManager.TRANSPARENCY_NONE,0);
			}
		}

		copybitmap(bitmap,tmpbitmap,0,0,0,0,Machine_visible_area,GfxManager.TRANSPARENCY_NONE,0);

		/* Draw the sprites. Note that it is important to draw them exactly in this */
		/* order, to have the correct priorities. */
		for (offs = spriteram_size - 2;offs > 2*2;offs -= 2)
		{
			int sx,sy;

			sx = 272 - RAM[spriteram_2 + offs + 1];
			sy = RAM[spriteram_2 + offs] - 31;

			drawgfx(bitmap,Machine_gfx[gfx_bank*2+1],
					RAM[spriteram + offs] >> 2,
					RAM[spriteram + offs + 1] & 0x1f,
					RAM[spriteram + offs] & 1,RAM[spriteram + offs] & 2,
					sx,sy,
					GfxManager.TRANSPARENCY_COLOR,0);

			/* also plot the sprite with wraparound (tunnel in Crush Roller) */
			/*drawgfx(bitmap,Machine_gfx[gfx_bank*2+1],
					RAM[spriteram + offs] >> 2,
					RAM[spriteram + offs + 1] & 0x1f,
					RAM[spriteram + offs] & 1,RAM[spriteram + offs] & 2,
					sx - 256,sy,
					TRANSPARENCY_COLOR,0);*/
		}
		/* In the Pac Man based games (NOT Pengo) the first two sprites must be offset */
		/* one pixel to the left to get a more correct placement */
		for (offs = 2*2;offs >= 0;offs -= 2)
		{
			int sx,sy;

			sx = 272 - RAM[spriteram_2 + offs + 1];
			sy = RAM[spriteram_2 + offs] - 31;

			drawgfx(bitmap,Machine_gfx[gfx_bank*2+1],
					RAM[spriteram + offs] >> 2,
					RAM[spriteram + offs + 1] & 0x1f,
					RAM[spriteram + offs] & 1,RAM[spriteram + offs] & 2,
					sx,sy + xoffsethack,
					GfxManager.TRANSPARENCY_COLOR,0);

			/* also plot the sprite with wraparound (tunnel in Crush Roller) */
			/*drawgfx(bitmap,Machine_gfx[gfx_bank*2+1],
					RAM[spriteram + offs] >> 2,
					RAM[spriteram + offs + 1] & 0x1f,
					RAM[spriteram + offs] & 2,RAM[spriteram + offs] & 1,
					sx - 256,sy + xoffsethack,
					TRANSPARENCY_COLOR,0);*/
		}

		return bitmap;
	}
}

public WriteHandler videoram_w() { return new Videoram_w(); }
public class Videoram_w implements WriteHandler {
	public void write(int address, int value) {
		RAM[videoram + (address & 0x3ff)] = value;
		dirtybuffer[address & 0x3ff] = true;
	}
}

public WriteHandler colorram_w() { return new Colorram_w(); }
public class Colorram_w implements WriteHandler {
	public void write(int address, int value) {
		RAM[colorram + (address & 0x3ff)] = value;
		dirtybuffer[address & 0x3ff] = true;
	}
}

}
