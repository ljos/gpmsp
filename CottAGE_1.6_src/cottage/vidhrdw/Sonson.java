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

public class Sonson extends MAMEVideo implements VideoEmulator,Vh_refresh,Vh_start,Vh_stop,Vh_convert_color_proms {

public int[] Fsonson_scrollx = {0};
static int sonson_scrollx;

	/* COTTAGE VIDEO INITIALIZATION */
	public void init(MachineDriver md) {
		super.init_bis(md);
		super.init(md);
		sonson_scrollx = Fsonson_scrollx[0];
	}

/***************************************************************************

  Convert the color PROMs into a more useable format.

  Son Son has two 32x8 palette PROMs and two 256x4 lookup table PROMs (one
  for characters, one for sprites).
  The palette PROMs are connected to the RGB output this way:

  I don't know the exact values of the resistors between the PROMs and the
  RGB output. I assumed these values (the same as Commando)
  bit 7 -- 220 ohm resistor  -- GREEN
        -- 470 ohm resistor  -- GREEN
        -- 1  kohm resistor  -- GREEN
        -- 2.2kohm resistor  -- GREEN
        -- 220 ohm resistor  -- BLUE
        -- 470 ohm resistor  -- BLUE
        -- 1  kohm resistor  -- BLUE
  bit 0 -- 2.2kohm resistor  -- BLUE

  bit 7 -- unused
        -- unused
        -- unused
        -- unused
        -- 220 ohm resistor  -- RED
        -- 470 ohm resistor  -- RED
        -- 1  kohm resistor  -- RED
  bit 0 -- 2.2kohm resistor  -- RED

***************************************************************************/
public void palette_init()
{
	int i;

	int cp=0;
	for (i = 0;i < Machine_drv_total_colors;i++)
	{
		int bit0,bit1,bit2,bit3,r,g,b;

		/* red component */
		bit0 = (color_prom[i + Machine_drv_total_colors] >> 0) & 0x01;
		bit1 = (color_prom[i + Machine_drv_total_colors] >> 1) & 0x01;
		bit2 = (color_prom[i + Machine_drv_total_colors] >> 2) & 0x01;
		bit3 = (color_prom[i + Machine_drv_total_colors] >> 3) & 0x01;
		r = 0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3;
		/* green component */
		bit0 = (color_prom[i] >> 4) & 0x01;
		bit1 = (color_prom[i] >> 5) & 0x01;
		bit2 = (color_prom[i] >> 6) & 0x01;
		bit3 = (color_prom[i] >> 7) & 0x01;
		g = 0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3;
		/* blue component */
		bit0 = (color_prom[i] >> 0) & 0x01;
		bit1 = (color_prom[i] >> 1) & 0x01;
		bit2 = (color_prom[i] >> 2) & 0x01;
		bit3 = (color_prom[i] >> 3) & 0x01;
		b = 0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3;

		palette_set_color(i,r,g,b);
	}

	cp += 2*Machine_drv_total_colors;
	/* color_prom now points to the beginning of the lookup table */

	/* characters use colors 0-15 */
	for (i = 0;i < TOTAL_COLORS(0);i++)
		COLOR(0,i, color_prom[cp++] & 0x0f);

	/* sprites use colors 16-31 */
	for (i = 0;i < TOTAL_COLORS(1);i++)
		COLOR(1,i, (color_prom[cp++] & 0x0f) + 0x10);
}


public int vh_start()
{
	tmpbitmap = new BitMapImpl(256,256);
	return 0;
}


/***************************************************************************

  Draw the game screen in the given mame_bitmap.
  Do NOT call osd_update_display() from this function, it will be called by
  the main emulation engine.

***************************************************************************/
public BitMap video_update() {
	int offs;

	/* for every character in the Video RAM, check if it has been modified */
	/* since last time and update it accordingly. */
	for (offs = videoram_size - 1;offs >= 0;offs--)
	{
		if (dirtybuffer[offs])
		{
			int sx,sy;

			dirtybuffer[offs] = false;

			sx = offs % 32;
			sy = offs / 32;

			drawgfx(tmpbitmap,Machine_gfx[0],
					RAM[videoram+offs] + 256 * (RAM[colorram+offs] & 3),
					RAM[colorram+offs] >> 2,
					0,0,
					8*sx,8*sy,
					0,GfxManager.TRANSPARENCY_NONE,0);
		}
	}

	/* copy the background graphics */
	{
		int i;
		int scroll[]=new int[32];

		for (i = 0;i < 5;i++)
			scroll[i] = 0;
		for (i = 5;i < 32;i++)
			scroll[i] = -(RAM[sonson_scrollx]);

		copyscrollbitmap(bitmap,tmpbitmap,32,scroll,0,0,visible,GfxManager.TRANSPARENCY_NONE,0);
	}


	/* draw the sprites */
	for (offs = spriteram_size - 4;offs >= 0;offs -= 4)
	{
		drawgfx(bitmap,Machine_gfx[1],
				RAM[spriteram+offs + 2] + ((RAM[spriteram+offs + 1] & 0x20) << 3),
				RAM[spriteram+offs + 1] & 0x1f,
				~RAM[spriteram+offs + 1] & 0x40,~RAM[spriteram+offs + 1] & 0x80,
				RAM[spriteram+offs + 3],RAM[spriteram+offs + 0],
				Machine_visible_area,GfxManager.TRANSPARENCY_PEN,0);
	}
	return bitmap;
}

}