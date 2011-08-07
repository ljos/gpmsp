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

public class Pooyan extends MAMEVideo implements VideoEmulator,Vh_refresh,Vh_start,Vh_stop,Vh_convert_color_proms {

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

  Convert the color PROMs into a more useable format.

  Pooyan has one 32x8 palette PROM and two 256x4 lookup table PROMs
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

	/* color_prom now points to the beginning of the char lookup table */

	/* sprites */
	for (i = 0;i < TOTAL_COLORS(1);i++)
		COLOR(1,i,color_prom[cp++] & 0x0f);

	/* characters */
	for (i = 0;i < TOTAL_COLORS(0);i++)
		COLOR(0,i,(color_prom[cp++] & 0x0f) + 0x10);
}

public WriteHandler pooyan_flipscreen_w() { return new Pooyan_flipscreen_w(); }
class Pooyan_flipscreen_w implements WriteHandler {
	public void write(int address, int data)
	{
		if (flipscreen != (data & 1))
		{
			flipscreen = data & 1;
			memset(dirtybuffer,1,videoram_size);
		}
	}
}


/***************************************************************************

  Draw the game screen in the given mame_bitmap.
  Do NOT call osd_update_display() from this function, it will be called by
  the main emulation engine.

***************************************************************************/
public BitMap video_update()
{
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
			flipy = RAM[colorram + offs] & 0x80;
			if (flipscreen != 0)
			{
				sx = 31 - sx;
				sy = 31 - sy;
				flipx = ~flipx;
				flipy = ~flipy;
			}

			drawgfx(tmpbitmap,Machine_gfx[0],
					RAM[videoram + offs] + 8 * (RAM[colorram + offs] & 0x20),
					RAM[colorram + offs] & 0x0f,
					flipx,flipy,
					8*sx,8*sy,
					GfxManager.TRANSPARENCY_NONE,0);
		}
	}


	/* copy the character mapped graphics */
	copybitmap(bitmap,tmpbitmap,0,0,0,0,Machine_visible_area,GfxManager.TRANSPARENCY_NONE,0);


	/* Draw the sprites. */
	for (offs = 0;offs < spriteram_size;offs += 2)
	{
		/* TRANSPARENCY_COLOR is needed for the scores */
		drawgfx(bitmap,Machine_gfx[1],
				RAM[spriteram + offs + 1],
				RAM[spriteram_2 + offs] & 0x0f,
				RAM[spriteram_2 + offs] & 0x40,~RAM[spriteram_2 + offs] & 0x80,
				240-RAM[spriteram + offs],RAM[spriteram_2 + offs + 1],
				Machine_visible_area,GfxManager.TRANSPARENCY_COLOR,0);
	}
	return bitmap;
}

}