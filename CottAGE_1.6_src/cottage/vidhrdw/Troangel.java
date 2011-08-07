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

public class Troangel extends MAMEVideo implements VideoEmulator,Vh_refresh,Vh_start,Vh_stop,Vh_convert_color_proms {

	public int[] Ftroangel_scroll = {0};
	static int troangel_scroll;
	static int flipscreen;

	BasicMachine m;

	public void setMachine(BasicMachine ma)
	{
		m = ma;
	}

	public void init(MachineDriver md) {
		super.init_bis(md);
		super.init(md);
		troangel_scroll = Ftroangel_scroll[0];
		tmpbitmap = new BitMapImpl(256,256);
	}

/***************************************************************************

  Convert the color PROMs into a more useable format.

  Tropical Angel has two 256x4 character palette PROMs, one 32x8 sprite
  palette PROM, and one 256x4 sprite color lookup table PROM.

  I don't know for sure how the palette PROMs are connected to the RGB
  output, but it's probably something like this; note that RED and BLUE
  are swapped wrt the usual configuration.

  bit 7 -- 220 ohm resistor  -- RED
        -- 470 ohm resistor  -- RED
        -- 220 ohm resistor  -- GREEN
        -- 470 ohm resistor  -- GREEN
        -- 1  kohm resistor  -- GREEN
        -- 220 ohm resistor  -- BLUE
        -- 470 ohm resistor  -- BLUE
  bit 0 -- 1  kohm resistor  -- BLUE

***************************************************************************/
public void palette_init()
{
	int i;
	int cp = 0;

	/* character palette */
	for (i = 0;i < 256;i++)
	{
		int bit0,bit1,bit2,r,g,b;

		/* red component */
		bit0 = 0;
		bit1 = (color_prom[cp+256] >> 2) & 0x01;
		bit2 = (color_prom[cp+256] >> 3) & 0x01;
		r = 0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2;
		/* green component */
		bit0 = (color_prom[cp+0] >> 3) & 0x01;
		bit1 = (color_prom[cp+256] >> 0) & 0x01;
		bit2 = (color_prom[cp+256] >> 1) & 0x01;
		g = 0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2;
		/* blue component */
		bit0 = (color_prom[cp+0] >> 0) & 0x01;
		bit1 = (color_prom[cp+0] >> 1) & 0x01;
		bit2 = (color_prom[cp+0] >> 2) & 0x01;
		b = 0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2;

		palette_set_color(i,r,g,b);
		COLOR(0,i, i);
		cp++;
	}

	cp += 256;
	/* color_prom now points to the beginning of the sprite palette */

	/* sprite palette */
	for (i = 0;i < 16;i++)
	{
		int bit0,bit1,bit2,r,g,b;

		/* red component */
		bit0 = 0;
		bit1 = (color_prom[cp] >> 6) & 0x01;
		bit2 = (color_prom[cp] >> 7) & 0x01;
		r = 0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2;
		/* green component */
		bit0 = (color_prom[cp] >> 3) & 0x01;
		bit1 = (color_prom[cp] >> 4) & 0x01;
		bit2 = (color_prom[cp] >> 5) & 0x01;
		g = 0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2;
		/* blue component */
		bit0 = (color_prom[cp] >> 0) & 0x01;
		bit1 = (color_prom[cp] >> 1) & 0x01;
		bit2 = (color_prom[cp] >> 2) & 0x01;
		b = 0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2;

		palette_set_color(i+256,r,g,b);
		cp++;
	}

	cp += 16;
	/* color_prom now points to the beginning of the sprite lookup table */

	/* sprite lookup table */
	for (i = 0;i < TOTAL_COLORS(1);i++)
	{
		COLOR(1,i, 256 + (~color_prom[cp] & 0x0f));
		COLOR(2,i, 256 + (~color_prom[cp] & 0x0f));
		COLOR(3,i, 256 + (~color_prom[cp] & 0x0f));
		COLOR(4,i, 256 + (~color_prom[cp] & 0x0f));
		cp++;
	}
}

public WriteHandler troangel_flipscreen_w() { return new Troangel_flipscreen_w(); }
class Troangel_flipscreen_w implements WriteHandler {
	public void write(int address, int data)
	{
	}
}

private void draw_background()
{
	int offs;

	for (offs = videoram_size - 2;offs >= 0;offs -= 2)
	{
		if (dirtybuffer[offs] || dirtybuffer[offs+1])
		{
			int sx,sy,code,attr,flipx;

			dirtybuffer[offs] = dirtybuffer[offs+1] = false;

			sx = (offs/2) % 32;
			sy = (offs/2) / 32;

			attr = RAM[videoram + offs];
			code = RAM[videoram + offs+1] + ((attr & 0xc0) << 2);
			flipx = attr & 0x20;

			if (flipscreen != 0)
			{
				sx = 31 - sx;
				sy = 31 - sy;
				flipx = ~flipx;
			}

			drawgfx(tmpbitmap,Machine_gfx[0],
				code,
				attr & 0x1f,
				flipx,flipscreen,
				8*sx,8*sy,
				0,GfxManager.TRANSPARENCY_NONE,0);
		}
	}

	int xscroll[] = new int[256];

	/* fixed */
	for (offs = 0;offs < 64;offs++) xscroll[offs] = 0;

	/* scroll (wraps around) */
	for (offs = 64;offs < 128;offs++) xscroll[offs] = -RAM[troangel_scroll+64];

	/* linescroll (no wrap) */
	for (offs = 128;offs < 256;offs++) xscroll[offs] = -RAM[troangel_scroll+offs];

	copyscrollbitmap(bitmap,tmpbitmap,256,xscroll,0,0,visible,GfxManager.TRANSPARENCY_NONE,0);
}

private void draw_sprites()
{
	int offs;

	for (offs = spriteram_size-4;offs >= 0;offs -= 4)
	{
		int attributes = RAM[spriteram+offs+1];
		int sx = RAM[spriteram+offs+3];
		int sy = ((224-RAM[spriteram+offs+0]-32)&0xff)+32;
		int code = RAM[spriteram+offs+2];
		int color = attributes&0x1f;
		int flipy = attributes&0x80;
		int flipx = attributes&0x40;

		int tile_number = code & 0x3f;

		int bank = 0;
		if( (code&0x80) != 0 ) bank += 1;
		if( (attributes&0x20) != 0 ) bank += 2;

		if (flipscreen != 0)
		{
			sx = 240 - sx;
			sy = 224 - sy;
			flipx = ~flipx;
			flipy = ~flipy;
		}

		drawgfx(bitmap,Machine_gfx[1+bank],
			tile_number,
			color,
			flipx,flipy,
			sx,sy,
			Machine_visible_area,GfxManager.TRANSPARENCY_PEN,0);
	}
}

public BitMap video_update() {
    draw_background();
    draw_sprites();
    return bitmap;
}

}