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

public class Pingpong extends MAMEVideo implements VideoEmulator,Vh_refresh,Vh_start,Vh_stop,Vh_convert_color_proms {

	/* COTTAGE VIDEO INITIALIZATION */
	public void init(MachineDriver md) {
		super.init_bis(md);
		super.init(md);
	}

/* This is strange; it's unlikely that the sprites actually have a hardware */
/* clipping region, but I haven't found another way to have them masked by */
/* the characters at the top and bottom of the screen. */
/*static struct rectangle spritevisiblearea =
{
	0*8, 32*8-1,
	4*8, 29*8-1
};*/



/***************************************************************************

  Convert the color PROMs into a more useable format.

  Ping Pong has a 32 bytes palette PROM and two 256 bytes color lookup table
  PROMs (one for sprites, one for characters).
  I don't know for sure how the palette PROM is connected to the RGB output,
  but it's probably the usual:

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


	/* color_prom now points to the beginning of the char lookup table */

	/* sprites */
	for (i = 0;i < TOTAL_COLORS(1);i++)
	{
		int code;
		int bit0,bit1,bit2,bit3;

		/* the bits of the color code are in reverse order - 0123 instead of 3210 */
		code = color_prom[cptr++] & 0x0f;
		bit0 = (code >> 0) & 1;
		bit1 = (code >> 1) & 1;
		bit2 = (code >> 2) & 1;
		bit3 = (code >> 3) & 1;
		code = (bit0 << 3) | (bit1 << 2) | (bit2 << 1) | (bit3 << 0);
		COLOR(1,i, code);
	}

	/* characters */
	for (i = 0;i < TOTAL_COLORS(0);i++)
		COLOR(0,i, (color_prom[cptr++] & 0x0f) + 0x10);
}



/***************************************************************************

  Draw the game screen in the given mame_bitmap.

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
			int sx,sy,flipx,flipy,tchar,color;


			sx = offs % 32;
			sy = offs / 32;

			dirtybuffer[offs] = false;

			flipx = RAM[colorram + offs] & 0x40;
			flipy = RAM[colorram + offs] & 0x80;
			color = RAM[colorram + offs] & 0x1F;
			tchar = (RAM[videoram + offs] + ((RAM[colorram + offs] & 0x20)<<3));

			drawgfx(tmpbitmap,Machine_gfx[0],
					tchar,
					color,
					flipx,flipy,
					8 * sx,8 * sy,
					GfxManager.TRANSPARENCY_NONE,0);
		}
	}


	/* copy the character mapped graphics */
	copybitmap(bitmap,tmpbitmap,0,0,0,0,Machine_visible_area,GfxManager.TRANSPARENCY_NONE,0);


	/* Draw the sprites. Note that it is important to draw them exactly in this */
	/* order, to have the correct priorities. */
	for (offs = spriteram_size - 4;offs >= 0;offs -= 4)
	{
		int sx,sy,flipx,flipy,color,schar;


		sx = RAM[spriteram + offs + 3];
		sy = 241 - RAM[spriteram + offs + 1];

		flipx = RAM[spriteram + offs] & 0x40;
		flipy = RAM[spriteram + offs] & 0x80;
		color = RAM[spriteram + offs] & 0x1F;
		schar = RAM[spriteram + offs + 2] & 0x7F;

		drawgfx(bitmap,Machine_gfx[1],
				schar,
				color,
				flipx,flipy,
				sx,sy,
				GfxManager.TRANSPARENCY_COLOR,0);
	}

	return bitmap;
}

}