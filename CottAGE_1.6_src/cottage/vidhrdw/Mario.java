/***************************************************************************

  vidhrdw.c

  Functions to emulate the video hardware of the machine.

***************************************************************************/

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

public class Mario extends MAMEVideo implements VideoEmulator,Vh_refresh,Vh_start,Vh_stop,Vh_convert_color_proms {

static int gfx_bank,palette_bank;

public int[] Fmario_scrolly = {0};
static int mario_scrolly;

	/* COTTAGE VIDEO INITIALIZATION */
	public void init(MachineDriver md) {
		super.init_bis(md);
		super.init(md);
		mario_scrolly = Fmario_scrolly[0];
	}

/***************************************************************************

  Convert the color PROMs into a more useable format.

  Mario Bros. has a 512x8 palette PROM; interstingly, bytes 0-255 contain an
  inverted palette, as other Nintendo games like Donkey Kong, while bytes
  256-511 contain a non inverted palette. This was probably done to allow
  connection to both the special Nintendo and a standard monitor.
  The palette PROM is connected to the RGB output this way:

  bit 7 -- 220 ohm resistor -- inverter  -- RED
        -- 470 ohm resistor -- inverter  -- RED
        -- 1  kohm resistor -- inverter  -- RED
        -- 220 ohm resistor -- inverter  -- GREEN
        -- 470 ohm resistor -- inverter  -- GREEN
        -- 1  kohm resistor -- inverter  -- GREEN
        -- 220 ohm resistor -- inverter  -- BLUE
  bit 0 -- 470 ohm resistor -- inverter  -- BLUE

***************************************************************************/
public void palette_init() {
	int i;

	for (i = 0;i < Machine_drv_total_colors;i++)
	{
		int bit0,bit1,bit2,r,g,b;

		/* red component */
		bit0 = (color_prom[i] >> 5) & 1;
		bit1 = (color_prom[i] >> 6) & 1;
		bit2 = (color_prom[i] >> 7) & 1;
		r = 255 - (0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2);
		/* green component */
		bit0 = (color_prom[i] >> 2) & 1;
		bit1 = (color_prom[i] >> 3) & 1;
		bit2 = (color_prom[i] >> 4) & 1;
		g = 255 - (0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2);
		/* blue component */
		bit0 = (color_prom[i] >> 0) & 1;
		bit1 = (color_prom[i] >> 1) & 1;
		b = 255 - (0x55 * bit0 + 0xaa * bit1);

		palette_set_color(i,r,g,b);
	}

	/* characters use the same palette as sprites, however characters */
	/* use only colors 64-127 and 192-255. */
	for (i = 0;i < 8;i++)
	{
		COLOR(0,4*i, 8*i + 64);
		COLOR(0,4*i+1, 8*i+1 + 64);
		COLOR(0,4*i+2, 8*i+2 + 64);
		COLOR(0,4*i+3, 8*i+3 + 64);
	}
	for (i = 0;i < 8;i++)
	{
		COLOR(0,4*i+8*4, 8*i + 192);
		COLOR(0,4*i+8*4+1, 8*i+1 + 192);
		COLOR(0,4*i+8*4+2, 8*i+2 + 192);
		COLOR(0,4*i+8*4+3, 8*i+3 + 192);
	}

	/* sprites */
	for (i = 0;i < TOTAL_COLORS(1);i++)
		COLOR(1,i, i);
}

public WriteHandler mario_gfxbank_w() { return new Mario_gfxbank_w(); }
public class Mario_gfxbank_w implements WriteHandler {
	public void write(int address, int data) {
		if (gfx_bank != (data & 1))
		{
			for(int i=0; i<videoram_size; i++)
				dirtybuffer[i] = true;
			gfx_bank = data & 1;
		}
	}
}

public WriteHandler mario_palettebank_w() { return new Mario_palettebank_w(); }
public class Mario_palettebank_w implements WriteHandler {
	public void write(int address, int data) {
		if (palette_bank != (data & 1))
		{
			for(int i=0; i<videoram_size; i++)
				dirtybuffer[i] = true;
			palette_bank = data & 1;
		}
	}
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
			
			sy -= 2;

			drawgfx(tmpbitmap,Machine_gfx[0],
					RAM[videoram + offs] + 256 * gfx_bank,
					(RAM[videoram + offs] >> 5) + 8 * palette_bank,
					0,0,
					8*sx,8*sy,
					0,GfxManager.TRANSPARENCY_NONE,0);
		}
	}

	copybitmap(bitmap,tmpbitmap,0,0,0,0,Machine_visible_area,GfxManager.TRANSPARENCY_NONE,0);

	/* copy the temporary bitmap to the screen */
	{
		//int[] scrolly = {0};

		/* I'm not positive the scroll direction is right */
		//scrolly[0] = -mario_scrolly - 17;
		//copyscrollbitmap(bitmap,tmpbitmap,0,0,1,scrolly,visible_area(),TRANSPARENCY_NONE,0);
	}

	/* Draw the sprites. */
	for (offs = 0;offs < spriteram_size;offs += 4)
	{
		if (RAM[spriteram + offs] != 0)
		{
			drawgfx(bitmap,Machine_gfx[1],
					RAM[spriteram + offs + 2],
					(RAM[spriteram + offs + 1] & 0x0f) + 16 * palette_bank,
					RAM[spriteram + offs + 1] & 0x80,RAM[spriteram + offs + 1] & 0x40,
					RAM[spriteram + offs + 3] - 8,240 - RAM[spriteram + offs] + 8,
					Machine_visible_area,GfxManager.TRANSPARENCY_PEN,0);
		}
	}
	return bitmap;
}

}