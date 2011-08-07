/***************************************************************************

  vidhrdw.c

  Functions to emulate the video hardware of the machine.

***************************************************************************/
package cottage.vidhrdw;

import jef.machine.BasicMachine;
import jef.machine.MachineDriver;
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

public class Rallyx extends MAMEVideo implements VideoEmulator, Vh_refresh,Vh_start,Vh_stop,Vh_convert_color_proms {

public int[] Frallyx_videoram2 = {0};
public int[] Frallyx_colorram2 = {0};
int rallyx_videoram2;
int rallyx_colorram2;

public int[] Frallyx_radarx = {0};
public int[] Frallyx_radary = {0};
public int[] Frallyx_radarattr = {0};
int rallyx_radarx;
int rallyx_radary;
int rallyx_radarattr;

public int[] Frallyx_scrollx = {0};
public int[] Frallyx_scrolly = {0};
int rallyx_scrollx;
int rallyx_scrolly;

static boolean[] dirtybuffer2;
static BitMap tmpbitmap1;
static int flip_screen;
public int[] Frallyx_radarram_size = {0};
static int rallyx_radarram_size;

BasicMachine m;


public void setMachine(BasicMachine ma)
{
	m = ma;
}

public void init(MachineDriver md) {
	super.init_bis(md);
	super.init(md);
	rallyx_videoram2 = Frallyx_videoram2[0];
	rallyx_colorram2 = Frallyx_colorram2[0];
	rallyx_radarx = Frallyx_radarx[0];
	rallyx_radary = Frallyx_radary[0];
	rallyx_radarattr = Frallyx_radarattr[0];
	rallyx_scrollx = Frallyx_scrollx[0];
	rallyx_scrolly = Frallyx_scrolly[0];
	rallyx_radarram_size = Frallyx_radarram_size[0];
}


int[][] radarvisiblearea =
{
	{28*8}, {36*8-1},
	{0*8}, {28*8-1}
};

int[][] radarvisibleareaflip =
{
	{0*8}, {8*8-1},
	{0*8}, {28*8-1}
};

/***************************************************************************

  Convert the color PROMs into a more useable format.

  Rally X has one 32x8 palette PROM and one 256x4 color lookup table PROM.
  The palette PROM is connected to the RGB output this way:

  bit 7 -- 220 ohm resistor  -- BLUE
        -- 470 ohm resistor  -- BLUE
        -- 220 ohm resistor  -- GREEN
        -- 470 ohm resistor  -- GREEN
        -- 1  kohm resistor  -- GREEN
        -- 220 ohm resistor  -- RED
        -- 470 ohm resistor  -- RED
  bit 0 -- 1  kohm resistor  -- RED

  In Rally-X there is a 1 kohm pull-down on B only, in Locomotion the
  1 kohm pull-down is an all three RGB outputs.

***************************************************************************/
public void palette_init_rallyx()
{
	int i;


    int p = 0;
	for (i = 0;i < Machine_drv_total_colors;i++)
	{
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

	/* color_prom now points to the beginning of the lookup table */

	/* character lookup table */
	/* sprites use the same color lookup table as characters */
	/* characters use colors 0-15 */
	for (i = 0;i < TOTAL_COLORS(0);i++) {
		COLOR(0,i, color_prom[p] & 0x0f);
		COLOR(1,i, color_prom[p++] & 0x0f);
	}

	/* radar dots lookup table */
	/* they use colors 16-19 */
	for (i = 0;i < 4;i++)
		COLOR(2,i, 16 + i);
}

public void palette_init_locomotn()
{
	int i;

    int p = 0;
	for (i = 0;i < Machine_drv_total_colors;i++)
	{
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
		bit0 = (color_prom[p] >> 6) & 0x01;
		bit1 = (color_prom[p] >> 7) & 0x01;
		b = 0x50 * bit0 + 0xab * bit1;

		palette_set_color(i,r,g,b);

		p++;
	}

	/* color_prom now points to the beginning of the lookup table */

	/* character lookup table */
	/* sprites use the same color lookup table as characters */
	/* characters use colors 0-15 */
	for (i = 0;i < TOTAL_COLORS(0);i++)
		COLOR(0,i, color_prom[p++] & 0x0f);

	/* radar dots lookup table */
	/* they use colors 16-19 */
	for (i = 0;i < 4;i++)
		COLOR(2,i, 16 + i);
}

/***************************************************************************

  Start the video hardware emulation.

***************************************************************************/
public int vh_start()
{
/*	if (video_start_generic() != 0)
		return 1;
*/
	if ((dirtybuffer2 = bauto_malloc(videoram_size)) == null)
		return 1;

	memset(dirtybuffer2,1,videoram_size);

/*	if ((tmpbitmap1 = auto_bitmap_alloc(32*8,32*8)) == 0)
		return 1;*/

    tmpbitmap1 = new BitMapImpl(32*8,32*8);

	return 0;
}


public WriteHandler rallyx_videoram2_w() { return new Rallyx_videoram2_w(); }
class Rallyx_videoram2_w implements WriteHandler {
	public void write(int address, int data)
	{
		if (RAM[address] != data)
		{
			dirtybuffer2[address - rallyx_videoram2] = true;

			RAM[address] = data;
		}
	}
}

public WriteHandler rallyx_colorram2_w() { return new Rallyx_colorram2_w(); }
class Rallyx_colorram2_w implements WriteHandler {
	public void write(int address, int data)
	{
		if (RAM[address] != data)
		{
			dirtybuffer2[address - rallyx_colorram2] = true;

			RAM[address] = data;
		}
	}
}

public WriteHandler rallyx_flipscreen_w() { return new Rallyx_flipscreen_w(); }
class Rallyx_flipscreen_w implements WriteHandler {
	public void write(int address, int data)
	{
/*		if (flip_screen != (data & 1))
		{
			flip_screen_set(data & 1);
			memset(dirtybuffer,1,videoram_size);
			memset(dirtybuffer2,1,videoram_size);
		}*/
	}
}



/***************************************************************************

  Draw the game screen in the given mame_bitmap.
  Do NOT call osd_update_display() from this function, it will be called by
  the main emulation engine.

***************************************************************************/
public BitMap video_update_rallyx()
{
	int offs,sx,sy;
	int scrollx,scrolly;
    int displacement = 1;


	if (flip_screen != 0)
	{
		scrollx = (RAM[rallyx_scrollx] - displacement) + 32;
		scrolly = (RAM[rallyx_scrolly] + 16) - 32;
	}
	else
	{
		scrollx = -(RAM[rallyx_scrollx] - 3*displacement);
		scrolly = -(RAM[rallyx_scrolly] + 16);
	}


	/* draw the below sprite priority characters */
	for (offs = videoram_size - 1;offs >= 0;offs--)
	{
		if ((RAM[rallyx_colorram2 + offs] & 0x20) != 0)  continue;

		if (dirtybuffer2[offs])
		{
			int flipx,flipy;


			dirtybuffer2[offs] = false;

			sx = offs % 32;
			sy = offs / 32;
			flipx = ~RAM[rallyx_colorram2 + offs] & 0x40;
			flipy = RAM[rallyx_colorram2 + offs] & 0x80;
			if (flip_screen != 0)
			{
				sx = 31 - sx;
				sy = 31 - sy;
				flipx = ~flipx;
				flipy = ~flipy;
			}

			drawgfx(tmpbitmap1,Machine_gfx[0],
					RAM[rallyx_videoram2 + offs],
					RAM[rallyx_colorram2 + offs] & 0x3f,
					flipx,flipy,
					8*sx,8*sy,
					0,GfxManager.TRANSPARENCY_NONE,0);
		}
	}

	/* update radar */
	for (offs = videoram_size - 1;offs >= 0;offs--)
	{
		if (dirtybuffer[offs])
		{
			int flipx,flipy;


			dirtybuffer[offs] = false;

			sx = (offs % 32) ^ 4;
			sy = offs / 32 - 2;
			flipx = ~RAM[colorram + offs] & 0x40;
			flipy = RAM[colorram + offs] & 0x80;
			if (flip_screen != 0)
			{
				sx = 7 - sx;
				sy = 27 - sy;
				flipx = ~flipx;
				flipy = ~flipy;
			}

			drawgfx(tmpbitmap,Machine_gfx[0],
					RAM[videoram + offs],
					RAM[colorram + offs] & 0x3f,
					flipx,flipy,
					8*sx,8*sy,
					0,GfxManager.TRANSPARENCY_NONE,0);
		}
	}


	/* copy the temporary bitmap to the screen */
	copyscrollbitmap(bitmap,tmpbitmap1,1,scrollx,1,scrolly,GfxManager.TRANSPARENCY_NONE,0);


	/* draw the sprites */
	for (offs = 0;offs < spriteram_size;offs += 2)
	{
		sx = RAM[spriteram + offs + 1] + ((RAM[spriteram_2 + offs + 1] & 0x80) << 1) - displacement;
		sy = 225 - RAM[spriteram_2 + offs] - displacement;

		drawgfx(bitmap,Machine_gfx[1],
				(RAM[spriteram + offs] & 0xfc) >> 2,
				RAM[spriteram_2 + offs + 1] & 0x3f,
				RAM[spriteram + offs] & 1,RAM[spriteram + offs] & 2,
				sx,sy,
				Machine_visible_area,GfxManager.TRANSPARENCY_COLOR,0);
	}


	/* draw the above sprite priority characters */
	for (offs = videoram_size - 1;offs >= 0;offs--)
	{
		int flipx,flipy;


		if (~(RAM[rallyx_colorram2 + offs] & 0x20) != 0)  continue;

		sx = offs % 32;
		sy = offs / 32;
		flipx = ~RAM[rallyx_colorram2 + offs] & 0x40;
		flipy = RAM[rallyx_colorram2 + offs] & 0x80;
		if (flip_screen != 0 )
		{
			sx = 31 - sx;
			sy = 31 - sy;
			flipx = ~flipx;
			flipy = ~flipy;
		}

		drawgfx(bitmap,Machine_gfx[0],
				RAM[rallyx_videoram2 + offs],
				RAM[rallyx_colorram2 + offs] & 0x3f,
				flipx,flipy,
				(8*sx + scrollx) & 0xff,(8*sy + scrolly) & 0xff,
				0,GfxManager.TRANSPARENCY_NONE,0);
		drawgfx(bitmap,Machine_gfx[0],
				RAM[rallyx_videoram2 + offs],
				RAM[rallyx_colorram2 + offs] & 0x3f,
				flipx,flipy,
				((8*sx + scrollx) & 0xff) - 256,(8*sy + scrolly) & 0xff,
				0,GfxManager.TRANSPARENCY_NONE,0);
	}


	/* radar */
/*	if (flip_screen != 0)
		copybitmap(bitmap,tmpbitmap,0,0,0,0,1,GfxManager.TRANSPARENCY_NONE,0);
	else
		copybitmap(bitmap,tmpbitmap,0,0,28*8,0,1,GfxManager.TRANSPARENCY_NONE,0);
*/

	/* draw the cars on the radar */
	for (offs = 0; offs < rallyx_radarram_size;offs++)
	{
		int x,y;

		x = RAM[rallyx_radarx + offs] + ((~RAM[rallyx_radarattr + offs] & 0x01) << 8);
		y = 237 - RAM[rallyx_radary + offs];
		if (flip_screen != 0 ) x -= 3;

		drawgfx(bitmap,Machine_gfx[2],
				((RAM[rallyx_radarattr + offs] & 0x0e) >> 1) ^ 0x07,
				0,
				0,0,
				x,y,
				Machine_visible_area,GfxManager.TRANSPARENCY_PEN,3);
	}
	return bitmap;
}

public BitMap video_update_jungler()
{

	int offs,sx,sy;
	int scrollx,scrolly;
    int displacement = 0;


	if (flip_screen != 0)
	{
		scrollx = (RAM[rallyx_scrollx] - displacement) + 32;
		scrolly = (RAM[rallyx_scrolly] + 16) - 32;
	}
	else
	{
		scrollx = -(RAM[rallyx_scrollx] - 3*displacement);
		scrolly = -(RAM[rallyx_scrolly] + 16);
	}


	for (offs = videoram_size - 1;offs >= 0;offs--)
	{
		if (dirtybuffer2[offs])
		{
			int flipx,flipy;


			dirtybuffer2[offs] = false;

			sx = offs % 32;
			sy = offs / 32;
			flipx = ~RAM[rallyx_colorram2 + offs] & 0x40;
			flipy = RAM[rallyx_colorram2 + offs] & 0x80;
			if (flip_screen != 0 )
			{
				sx = 31 - sx;
				sy = 31 - sy;
				flipx = ~flipx;
				flipy = ~flipy;
			}

			drawgfx(tmpbitmap1,Machine_gfx[0],
					RAM[rallyx_videoram2 + offs],
					RAM[rallyx_colorram2 + offs] & 0x3f,
					flipx,flipy,
					8*sx,8*sy,
					0,GfxManager.TRANSPARENCY_NONE,0);
		}
	}

	/* update radar */
	for (offs = videoram_size - 1;offs >= 0;offs--)
	{
		if (dirtybuffer[offs])
		{
			int flipx,flipy;


			dirtybuffer[offs] = false;

			sx = (offs % 32) ^ 4;
			sy = offs / 32 - 2;
			flipx = ~RAM[colorram + offs] & 0x40;
			flipy = RAM[colorram + offs] & 0x80;
			if (flip_screen != 0)
			{
				sx = 7 - sx;
				sy = 27 - sy;
				flipx = ~flipx;
				flipy = ~flipy;
			}

			drawgfx(tmpbitmap,Machine_gfx[0],
					RAM[videoram + offs],
					RAM[colorram + offs] & 0x3f,
					flipx,flipy,
					8*sx,8*sy,
					1,GfxManager.TRANSPARENCY_NONE,0);
		}
	}


	/* copy the temporary bitmap to the screen */
	copyscrollbitmap(bitmap,tmpbitmap1,1,scrollx,1,scrolly,GfxManager.TRANSPARENCY_NONE,0);


	/* draw the sprites */
	for (offs = 0;offs < spriteram_size;offs += 2)
	{
		sx = RAM[spriteram + offs + 1] + ((RAM[spriteram_2 + offs + 1] & 0x80) << 1) - displacement;
		sy = 225 - RAM[spriteram_2 + offs] - displacement;

		drawgfx(bitmap,Machine_gfx[1],
				(RAM[spriteram + offs] & 0xfc) >> 2,
				RAM[spriteram_2 + offs + 1] & 0x3f,
				RAM[spriteram + offs] & 1,RAM[spriteram + offs] & 2,
				sx,sy,
				Machine_visible_area,GfxManager.TRANSPARENCY_COLOR,0);
	}


	/* radar */
	if (flip_screen != 0)
		copybitmap(bitmap,tmpbitmap,0,0,0,0,1,GfxManager.TRANSPARENCY_NONE,0);
	else
		copybitmap(bitmap,tmpbitmap,0,0,28*8,0,1,GfxManager.TRANSPARENCY_NONE,0);


	/* draw the cars on the radar */
	for (offs = 0; offs < rallyx_radarram_size;offs++)
	{
		int x,y;

		x = RAM[rallyx_radarx + offs] + ((~RAM[rallyx_radarattr + offs] & 0x08) << 5);
		y = 237 - RAM[rallyx_radary + offs];

		drawgfx(bitmap,Machine_gfx[2],
				(RAM[rallyx_radarattr + offs] & 0x07) ^ 0x07,
				0,
				0,0,
				x,y,
				Machine_visible_area,GfxManager.TRANSPARENCY_PEN,0);
	}
	return bitmap;
}



public BitMap video_update_locomotn()
{
	int offs,sx,sy;
   int displacement = 0;


	/* for every character in the Video RAM, check if it has been modified */
	/* since last time and update it accordingly. */
	for (offs = videoram_size - 1;offs >= 0;offs--)
	{
		if (dirtybuffer2[offs])
		{
			int flipx,flipy;


			dirtybuffer2[offs] = false;

			sx = offs % 32;
			sy = offs / 32;
			/* not a mistake, one bit selects both  flips */
			flipx = RAM[rallyx_colorram2 + offs] & 0x80;
			flipy = RAM[rallyx_colorram2 + offs] & 0x80;
			if (flip_screen != 0)
			{
				sx = 31 - sx;
				sy = 31 - sy;
				flipx = ~flipx;
				flipy = ~flipy;
			}

			drawgfx(tmpbitmap1,Machine_gfx[0],
					(RAM[rallyx_videoram2 + offs]&0x7f) + 2*(RAM[rallyx_colorram2 + offs]&0x40) + 2*(RAM[rallyx_videoram2 + offs]&0x80),
					RAM[rallyx_colorram2 + offs] & 0x3f,
					flipx,flipy,
					8*sx,8*sy,
					0,GfxManager.TRANSPARENCY_NONE,0);
		}
	}

	/* update radar */
	for (offs = videoram_size - 1;offs >= 0;offs--)
	{
		if (dirtybuffer[offs])
		{
			int flipx,flipy;


			dirtybuffer[offs] = false;

			sx = (offs % 32) ^ 4;
			sy = offs / 32 - 2;
			/* not a mistake, one bit selects both  flips */
			flipx = RAM[colorram + offs] & 0x80;
			flipy = RAM[colorram + offs] & 0x80;
			if (flip_screen != 0)
			{
				sx = 7 - sx;
				sy = 27 - sy;
				flipx = ~flipx;
				flipy = ~flipy;
			}

			drawgfx(tmpbitmap,Machine_gfx[0],
					(RAM[videoram + offs]&0x7f) + 2*(RAM[colorram + offs]&0x40) + 2*(RAM[videoram + offs]&0x80),
					RAM[colorram + offs] & 0x3f,
					flipx,flipy,
					8*sx,8*sy,
					1,GfxManager.TRANSPARENCY_NONE,0);
		}
	}


	/* copy the temporary bitmap to the screen */
	{
		int scrollx,scrolly;


		if (flip_screen != 0)
		{
			scrollx = (RAM[rallyx_scrollx]) + 32;
			scrolly = (RAM[rallyx_scrolly] + 16) - 32;
		}
		else
		{
			scrollx = -(RAM[rallyx_scrollx]);
			scrolly = -(RAM[rallyx_scrolly] + 16);
		}

		copyscrollbitmap(bitmap,tmpbitmap1,1,scrollx,1,scrolly,GfxManager.TRANSPARENCY_NONE,0);
	}


	/* radar */
	if (flip_screen != 0)
		copybitmap(bitmap,tmpbitmap,0,0,0,0,1,GfxManager.TRANSPARENCY_NONE,0);
	else
		copybitmap(bitmap,tmpbitmap,0,0,28*8,0,1,GfxManager.TRANSPARENCY_NONE,0);


	/* draw the sprites */
	for (offs = 0;offs < spriteram_size;offs += 2)
	{
		sx = RAM[spriteram + offs + 1] + ((RAM[spriteram_2 + offs + 1] & 0x80) << 1) - displacement;
		sy = 225 - RAM[spriteram_2 + offs] - displacement;

		/* handle reduced visible area in some games */
		//if (flip_screen!=0 && Machine_drv_default_visible_area.max_x == 32*8-1) sx += 32;

		drawgfx(bitmap,Machine_gfx[1],
				((RAM[spriteram + offs] & 0x7c) >> 2) + 0x20*(RAM[spriteram + offs] & 0x01) + ((RAM[spriteram + offs] & 0x80) >> 1),
				RAM[spriteram_2 + offs + 1] & 0x3f,
				RAM[spriteram + offs] & 2,RAM[spriteram + offs] & 2,
				sx,sy,
				Machine_visible_area,GfxManager.TRANSPARENCY_COLOR,0);
	}


	/* draw the cars on the radar */
	for (offs = 0; offs < rallyx_radarram_size;offs++)
	{
		int x,y;

		x = RAM[rallyx_radarx + offs] + ((~RAM[rallyx_radarattr + offs] & 0x08) << 5);
		y = 237 - RAM[rallyx_radary + offs];
		if (flip_screen!=0) x -= 3;

		/* handle reduced visible area in some games */
		//if (flip_screen!=0 && Machine_drv_default_visible_area.max_x == 32*8-1) x += 32;

		drawgfx(bitmap,Machine_gfx[2],
				(RAM[rallyx_radarattr + (offs & 0x0f)] & 0x07) ^ 0x07,
				0,
				0,0,
				x,y,
				Machine_visible_area,GfxManager.TRANSPARENCY_PEN,3);
	}
	return bitmap;
}



public BitMap video_update_commsega()
{
	int offs,sx,sy;


	/* for every character in the Video RAM, check if it has been modified */
	/* since last time and update it accordingly. */
	for (offs = videoram_size - 1;offs >= 0;offs--)
	{
		if (dirtybuffer2[offs])
		{
			int flipx,flipy;


			dirtybuffer2[offs] = false;

			sx = offs % 32;
			sy = offs / 32;
			/* not a mistake, one bit selects both  flips */
			flipx = RAM[rallyx_colorram2 + offs] & 0x80;
			flipy = RAM[rallyx_colorram2 + offs] & 0x80;
			if (flip_screen != 0)
			{
				sx = 31 - sx;
				sy = 31 - sy;
				flipx = ~flipx;
				flipy = ~flipy;
			}

			drawgfx(tmpbitmap1,Machine_gfx[0],
					(RAM[rallyx_videoram2 + offs]&0x7f) + 2*(RAM[rallyx_colorram2 + offs]&0x40) + 2*(RAM[rallyx_videoram2 + offs]&0x80),
					RAM[rallyx_colorram2 + offs] & 0x3f,
					flipx,flipy,
					8*sx,8*sy,
					0,GfxManager.TRANSPARENCY_NONE,0);
		}
	}

	/* update radar */
	for (offs = videoram_size - 1;offs >= 0;offs--)
	{
		if (dirtybuffer[offs])
		{
			int flipx,flipy;


			dirtybuffer[offs] = false;

			sx = (offs % 32) ^ 4;
			sy = offs / 32 - 2;
			/* not a mistake, one bit selects both  flips */
			flipx = RAM[colorram + offs] & 0x80;
			flipy = RAM[colorram + offs] & 0x80;
			if (flip_screen != 0)
			{
				sx = 7 - sx;
				sy = 27 - sy;
				flipx = ~flipx;
				flipy = ~flipy;
			}

			drawgfx(tmpbitmap,Machine_gfx[0],
					(RAM[videoram + offs]&0x7f) + 2*(RAM[colorram + offs]&0x40) + 2*(RAM[videoram + offs]&0x80),
					RAM[colorram + offs] & 0x3f,
					flipx,flipy,
					8*sx,8*sy,
					1,GfxManager.TRANSPARENCY_NONE,0);
		}
	}


	/* copy the temporary bitmap to the screen */
		int scrollx,scrolly;


		if (flip_screen != 0)
		{
			scrollx = (RAM[rallyx_scrollx]) + 32;
			scrolly = (RAM[rallyx_scrolly]+ 16) - 32;
		}
		else
		{
			scrollx = -(RAM[rallyx_scrollx]);
			scrolly = -(RAM[rallyx_scrolly] + 16);
		}

		copyscrollbitmap(bitmap,tmpbitmap1,1,scrollx,1,scrolly,GfxManager.TRANSPARENCY_NONE,0);


	/* radar */
	if (flip_screen != 0)
		copybitmap(bitmap,tmpbitmap,0,0,0,0,1,GfxManager.TRANSPARENCY_NONE,0);
	else
		copybitmap(bitmap,tmpbitmap,0,0,28*8,0,1,GfxManager.TRANSPARENCY_NONE,0);


	/* draw the sprites */
	for (offs = 0;offs < spriteram_size;offs += 2)
	{
		int flipx,flipy;


		sx = RAM[spriteram + offs + 1] - 1;
		sy = 224 - RAM[spriteram_2 + offs];
if (flip_screen!=0) sx += 32;
		flipx = ~RAM[spriteram + offs] & 1;
		flipy = ~RAM[spriteram + offs] & 2;
		if (flip_screen != 0)
		{
			flipx = ~flipx;
			flipy = ~flipy;
		}

		if ((RAM[spriteram + offs] & 0x01) != 0)	/* ??? */
			drawgfx(bitmap,Machine_gfx[1],
					((RAM[spriteram + offs] & 0x7c) >> 2) + 0x20*(RAM[spriteram + offs] & 0x01) + ((RAM[spriteram + offs] & 0x80) >> 1),
					RAM[spriteram_2 + offs + 1] & 0x3f,
					flipx,flipy,
					sx,sy,
					Machine_visible_area,GfxManager.TRANSPARENCY_COLOR,0);
	}


	/* draw the cars on the radar */
	for (offs = 0; offs < rallyx_radarram_size;offs++)
	{
		int x,y;


		/* it looks like the addresses used are
		   a000-a003  a004-a00f
		   8020-8023  8034-803f
		   8820-8823  8834-883f
		   so 8024-8033 and 8824-8833 are not used
		*/

		x = RAM[rallyx_radarx + offs] + ((~RAM[rallyx_radarattr + (offs & 0x0f)] & 0x08) << 5);
		if (flip_screen!=0) x += 32;
		y = 237 - RAM[rallyx_radary + offs];


		drawgfx(bitmap,Machine_gfx[2],
				(RAM[rallyx_radarattr + (offs & 0x0f)] & 0x07) ^ 0x07,
				0,
				0,0,
				x,y,
				Machine_visible_area,GfxManager.TRANSPARENCY_PEN,3);
	}
	return bitmap;
}

}
