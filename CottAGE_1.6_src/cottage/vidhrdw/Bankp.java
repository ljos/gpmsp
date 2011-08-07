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

public class Bankp extends MAMEVideo implements VideoEmulator,Vh_refresh,Vh_start,Vh_stop,Vh_convert_color_proms {

public int[] Fbankp_videoram2 = {0};
public int[] Fbankp_colorram2 = {0};
int bankp_videoram2;
int bankp_colorram2;
static boolean[] dirtybuffer2;
static BitMap tmpbitmap2;
static int scroll_x;
static int flipscreen;
static int priority;

BasicMachine m;

	public void setMachine(BasicMachine ma)
	{
		m = ma;
	}

	public void init(MachineDriver md) {
		super.init_bis(md);
		super.init(md);
		bankp_videoram2 = Fbankp_videoram2[0];
		bankp_colorram2 = Fbankp_colorram2[0];
	}

/***************************************************************************

  Convert the color PROMs into a more useable format.

  Bank Panic has a 32x8 palette PROM (I'm not sure whether the second 16
  bytes are used - they contain the same colors as the first 16 with only
  one different) and two 256x4 lookup table PROMs (one for charset #1, one
  for charset #2 - only the first 128 nibbles seem to be used).

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

	/* charset #1 lookup table */
	for (i = 0;i < TOTAL_COLORS(0);i++)
		COLOR(0,i,color_prom[p++] & 0x0f);

	p += 128;	/* skip the bottom half of the PROM - seems to be not used */

	/* charset #2 lookup table */
	for (i = 0;i < TOTAL_COLORS(1);i++)
		COLOR(1,i,color_prom[p++] & 0x0f);

	/* the bottom half of the PROM seems to be not used */
}


/***************************************************************************

  Start the video hardware emulation.

***************************************************************************/
public int vh_start()
{
/*	if (video_start_generic() != 0)
		return 1;*/

	if ((dirtybuffer2 = bauto_malloc(videoram_size)) == null)
		return 1;

	memset(dirtybuffer2,1,videoram_size);

/*	if ((tmpbitmap2 = auto_bitmap_alloc(Machine->drv->screen_width,Machine->drv->screen_height)) == 0)
		return 1;*/

	tmpbitmap = new BitMapImpl(256,256);
	tmpbitmap2 = new BitMapImpl(224,224);

	return 0;
}


public WriteHandler bankp_scroll_w() { return new Bankp_scroll_w(); }
class Bankp_scroll_w implements WriteHandler {
	public void write(int address, int data)
	{
		scroll_x = data;
	}
}


public WriteHandler bankp_videoram2_w() { return new Bankp_videoram2_w(); }
class Bankp_videoram2_w implements WriteHandler {
	public void write(int address, int data)
	{
		if (RAM[address] != data)
		{
			dirtybuffer2[address - bankp_videoram2] = true;

			RAM[address] = data;
		}
	}
}


public WriteHandler bankp_colorram2_w() { return new Bankp_colorram2_w(); }
class Bankp_colorram2_w implements WriteHandler {
	public void write(int address, int data)
	{
		if (RAM[address] != data)
		{
			dirtybuffer2[address - bankp_colorram2] = true;

			RAM[address] = data;
		}
	}
}


public WriteHandler bankp_out_w() { return new Bankp_out_w(); }
class Bankp_out_w implements WriteHandler {
	public void write(int address, int data)
	{
		/* bits 0-1 are playfield priority */
		/* TODO: understand how this works, currently the only thing I do is */
		/* invert the layer order when priority == 2 */
		priority = data & 0x03;

		/* bits 2-3 unknown (2 is used) */

		/* bit 4 controls NMI */
		m.nmi_interrupt_enabled = (((data & 0x10)>>4) != 0);
		//interrupt_enable_w(0,(data & 0x10)>>4);

		/* bit 5 controls screen flip */
		if ((data & 0x20) != flipscreen)
		{
			flipscreen = data & 0x20;
			memset(dirtybuffer,1,videoram_size);
			memset(dirtybuffer2,1,videoram_size);
		}

		/* bits 6-7 unknown */
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
			int sx,sy,flipx;

			dirtybuffer[offs] = false;

			sx = offs % 32;
			sy = offs / 32;
			flipx = RAM[colorram + offs] & 0x04;
			if (flipscreen != 0)
			{
				sx = 31 - sx;
				sy = 31 - sy;
				flipx = ~flipx;
			}

			drawgfx(tmpbitmap,Machine_gfx[0],
					RAM[videoram + offs] + 256 * ((RAM[colorram + offs] & 3) >> 0),
					RAM[colorram + offs] >> 3,
					flipx,flipscreen,
					8*sx,8*sy,
					0,GfxManager.TRANSPARENCY_NONE,0);
		}

		if (dirtybuffer2[offs])
		{
			int sx,sy,flipx;

			dirtybuffer2[offs] = false;

			sx = offs % 32;
			sy = offs / 32;
			flipx = RAM[bankp_colorram2 + offs] & 0x08;
			if (flipscreen != 0)
			{
				sx = 31 - sx;
				sy = 31 - sy;
				flipx = ~flipx;
			}

			drawgfx(tmpbitmap2,Machine_gfx[1],
					RAM[bankp_videoram2 + offs] + 256 * (RAM[bankp_colorram2 + offs] & 0x07),
					RAM[bankp_colorram2 + offs] >> 4,
					flipx,flipscreen,
					8*sx,8*sy,
					Machine_visible_area,GfxManager.TRANSPARENCY_NONE,0);
		}
	}

	/* copy the temporary bitmaps to the screen */
	{
		int scroll[] = new int[1];

		scroll[0] = -scroll_x;

/*		if (priority == 2)
		{
			copyscrollbitmap(bitmap,tmpbitmap,1,scroll,0,0,visible,TRANSPARENCY_NONE,0);
			copybitmap(bitmap,tmpbitmap2,0,0,0,0,Machine_visible_area,TRANSPARENCY_COLOR,0);
		}
		else
		{
			copybitmap(bitmap,tmpbitmap2,0,0,0,0,Machine_visible_area,TRANSPARENCY_NONE,0);
			copyscrollbitmap(bitmap,tmpbitmap,1,scroll,0,0,visible,TRANSPARENCY_COLOR,0);
		}*/
/*		for(int j=0;j<224;j++)
			for(int i=0;i<224;i++)
				bitmap.pixels[224*j+i] = tmpbitmap.pixels[256*j+i];*/
		copybitmap(bitmap,tmpbitmap2,0,0,0,0,Machine_visible_area,GfxManager.TRANSPARENCY_NONE,0);
		copyscrollbitmap(bitmap,tmpbitmap,1,scroll,0,0,visible,GfxManager.TRANSPARENCY_COLOR,0);
//		copybitmap(bitmap,tmpbitmap,0,0,0,0,Machine_visible_area,TRANSPARENCY_NONE,0);
	}
	return bitmap;
}

}