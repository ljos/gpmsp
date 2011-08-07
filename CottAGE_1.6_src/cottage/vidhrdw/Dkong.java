package cottage.vidhrdw;

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

public class Dkong extends MAMEVideo implements VideoEmulator,
												 Vh_refresh,
												 Vh_start,
												 Vh_stop,
												 Vh_convert_color_proms {


int gfx_bank = 0;
int palette_bank = 0;
int grid_on = 0;
int color_codes;

boolean global_changed = false;
BitMap charLayer;


public void init(MachineDriver md) {
	super.init_bis(md);
	super.init(md);
	charLayer = new BitMapImpl(backBuffer.getWidth(), backBuffer.getHeight());
}


/***************************************************************************

  Convert the color PROMs into a more useable format.

  Donkey Kong has two 256x4 palette PROMs and one 256x4 PROM which contains
  the color codes to use for characters on a per row/column basis (groups of
  of 4 characters in the same column - actually row, since the display is
  rotated)
  The palette PROMs are connected to the RGB output this way:

  bit 3 -- 220 ohm resistor -- inverter  -- RED
        -- 470 ohm resistor -- inverter  -- RED
        -- 1  kohm resistor -- inverter  -- RED
  bit 0 -- 220 ohm resistor -- inverter  -- GREEN
  bit 3 -- 470 ohm resistor -- inverter  -- GREEN
        -- 1  kohm resistor -- inverter  -- GREEN
        -- 220 ohm resistor -- inverter  -- BLUE
  bit 0 -- 470 ohm resistor -- inverter  -- BLUE

***************************************************************************/
public void palette_init() { // dkong
	int i;
	int cpointer = 0;

	for (i = 0;i < 256;i++) {

		int bit0,bit1,bit2,r,g,b;

		/* red component */
		bit0 = (color_prom[256 + cpointer] >> 1) & 1;
		bit1 = (color_prom[256 + cpointer] >> 2) & 1;
		bit2 = (color_prom[256 + cpointer] >> 3) & 1;
		r = 255 - (0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2);
		/* green component */
		bit0 = (color_prom[0 + cpointer] >> 2) & 1;
		bit1 = (color_prom[0 + cpointer] >> 3) & 1;
		bit2 = (color_prom[256 + cpointer] >> 0) & 1;
		g = 255 - (0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2);
		/* blue component */
		bit0 = (color_prom[0 + cpointer] >> 0) & 1;
		bit1 = (color_prom[0 + cpointer] >> 1) & 1;
		b = 255 - (0x55 * bit0 + 0xaa * bit1);

		palette_set_color(i,r,g,b);
		cpointer++;
	}

	cpointer += 256;
	/* color_prom now points to the beginning of the character color codes */
	color_codes = cpointer;	/* we'll need it later */
}

/***************************************************************************

  Convert the color PROMs into a more useable format.

  Donkey Kong 3 has two 512x8 palette PROMs and one 256x4 PROM which contains
  the color codes to use for characters on a per row/column basis (groups of
  of 4 characters in the same column - actually row, since the display is
  rotated)
  Interstingly, bytes 0-255 of the palette PROMs contain an inverted palette,
  as other Nintendo games like Donkey Kong, while bytes 256-511 contain a non
  inverted palette. This was probably done to allow connection to both the
  special Nintendo and a standard monitor.
  I don't know the exact values of the resistors between the PROMs and the
  RGB output, but they are probably the usual:

  bit 7 -- 220 ohm resistor -- inverter  -- RED
        -- 470 ohm resistor -- inverter  -- RED
        -- 1  kohm resistor -- inverter  -- RED
        -- 2.2kohm resistor -- inverter  -- RED
        -- 220 ohm resistor -- inverter  -- GREEN
        -- 470 ohm resistor -- inverter  -- GREEN
        -- 1  kohm resistor -- inverter  -- GREEN
  bit 0 -- 2.2kohm resistor -- inverter  -- GREEN

  bit 3 -- 220 ohm resistor -- inverter  -- BLUE
        -- 470 ohm resistor -- inverter  -- BLUE
        -- 1  kohm resistor -- inverter  -- BLUE
  bit 0 -- 2.2kohm resistor -- inverter  -- BLUE

***************************************************************************/
public Vh_convert_color_proms dkong3_palette_init() { return new Dkong3_palette_init(); }
public class Dkong3_palette_init implements Vh_convert_color_proms
{
	public void palette_init() {
		int i;
		int cpointer = 0;

		for (i = 0;i < 256;i++)
		{
			int bit0,bit1,bit2,bit3,r,g,b;


			/* red component */
			bit0 = (color_prom[0 + cpointer] >> 4) & 0x01;
			bit1 = (color_prom[0 + cpointer] >> 5) & 0x01;
			bit2 = (color_prom[0 + cpointer] >> 6) & 0x01;
			bit3 = (color_prom[0 + cpointer] >> 7) & 0x01;
			r = 255 - (0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3);
			/* green component */
			bit0 = (color_prom[0 + cpointer] >> 0) & 0x01;
			bit1 = (color_prom[0 + cpointer] >> 1) & 0x01;
			bit2 = (color_prom[0 + cpointer] >> 2) & 0x01;
			bit3 = (color_prom[0 + cpointer] >> 3) & 0x01;
			g = 255 - (0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3);
			/* blue component */
			bit0 = (color_prom[256 + cpointer] >> 0) & 0x01;
			bit1 = (color_prom[256 + cpointer] >> 1) & 0x01;
			bit2 = (color_prom[256 + cpointer] >> 2) & 0x01;
			bit3 = (color_prom[256 + cpointer] >> 3) & 0x01;
			b = 255 - (0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3);

			palette_set_color(i,r,g,b);
			cpointer++;
		}

		cpointer += 256;
		/* color_prom now points to the beginning of the character color codes */
		color_codes = cpointer;	/* we'll need it later */
	}
}


public int vh_start() { // dkong
	gfx_bank = 0;
	palette_bank = 0;

	return 0;
}

public WriteHandler radarscp_grid_color_w() { return new Radarscp_grid_color_w(); }
public class Radarscp_grid_color_w implements WriteHandler {
	public void write(int address, int data) {
		int r,g,b;

		r = ((~data >> 0) & 0x01) * 0xff;
		g = ((~data >> 1) & 0x01) * 0xff;
		b = ((~data >> 2) & 0x01) * 0xff;
	//	palette_set_color(257,r,g,b);
		palette_set_color(257,0x00,0x00,0xff);
	}
}

public WriteHandler radarscp_grid_enable_w() { return new Radarscp_grid_enable_w(); }
public class Radarscp_grid_enable_w implements WriteHandler {
	public void write(int address, int data) {
		grid_on = data & 1;
	}
}

public WriteHandler dkongjr_gfxbank_w() { return new Dkongjr_gfxbank_w(); }
public class Dkongjr_gfxbank_w implements WriteHandler {
	public void write(int address, int data) {
		global_changed = true;
		gfx_bank = data & 1;
	}
}

public WriteHandler dkong3_gfxbank_w() { return new Dkong3_gfxbank_w(); }
public class Dkong3_gfxbank_w implements WriteHandler {
	public void write(int address, int data) {
		global_changed = true;
		gfx_bank = ~data & 1;
	}
}

public WriteHandler dkong_palettebank_w() { return new Dkong_palettebank_w(); }
public class Dkong_palettebank_w implements WriteHandler {
	public void write(int address, int data) {
		int offset = address & 1;
		int newbank;

		newbank = palette_bank;
		if ((data & 1)!=0)
			newbank |= 1 << offset;
		else
			newbank &= ~(1 << offset);

		palette_bank = newbank;
		global_changed = true;
	}
}


/***************************************************************************

  Draw the game screen in the given mame_bitmap.
  Do NOT call osd_update_display() from this function, it will be called by
  the main emulation engine.

***************************************************************************/
private void draw_tiles()
{
	int offs;

	/* for every character in the Video RAM, check if it has been modified */
	/* since last time and update it accordingly. */
	for (offs = videoram_size - 1;offs >= 0;offs--) {
		if (dirtybuffer[offs]) {
			int sx,sy;
			int charcode,color;

			dirtybuffer[offs] = false;

			sx = offs % 32;
			sy = offs / 32;

			charcode = RAM[videoram + offs] + 256 * gfx_bank;
			/* retrieve the character color from the PROM */
			color = (color_prom[color_codes + (offs % 32) + 32 * (offs / 32 / 4)] & 0x0f) + 0x10 * palette_bank;

			//if (flip_screen)
			//{
			//	sx = 31 - sx;
			//	sy = 31 - sy;
			//}

			drawgfx(charLayer,0,
					charcode,color,
					false,false,
					8*sx,8*sy,
					GfxManager.TRANSPARENCY_NONE,0);
		}
	}


	/* copy the character mapped graphics */
	charLayer.toPixels(pixels);
}

private void draw_sprites()
{
	int offs;

	/* Draw the sprites. */
	for (offs = 0;offs < spriteram_size;offs += 4)
	{
		if (RAM[spriteram + offs] != 0)
		{
			/* RAM[spriteram + offs + 2] & 0x40 is used by Donkey Kong 3 only */
			/* RAM[spriteram + offs + 2] & 0x30 don't seem to be used (they are */
			/* probably not part of the color code, since Mario Bros, which */
			/* has similar hardware, uses a memory mapped port to change */
			/* palette bank, so it's limited to 16 color codes) */

			int x,y;

			x = RAM[spriteram + offs + 3] - 8;
			y = 240 - RAM[spriteram + offs] + 7;

			drawgfx(backBuffer,1,
					(RAM[spriteram + offs + 1] & 0x7f) + 2 * (RAM[spriteram + offs + 2] & 0x40),
					(RAM[spriteram + offs + 2] & 0x0f) + 16 * palette_bank,
					(RAM[spriteram + offs + 2] & 0x80),(RAM[spriteram + offs + 1] & 0x80),
					x,y,
					GfxManager.TRANSPARENCY_PEN,0);

			/* draw with wrap around - this fixes the 'beheading' bug */
			drawgfx(backBuffer,1,
					(RAM[spriteram + offs + 1] & 0x7f) + 2 * (RAM[spriteram + offs + 2] & 0x40),
					(RAM[spriteram + offs + 2] & 0x0f) + 16 * palette_bank,
					(RAM[spriteram + offs + 2] & 0x80),(RAM[spriteram + offs + 1] & 0x80),
					x+256,y,
					GfxManager.TRANSPARENCY_PEN,0);
		}
	}
}

private void draw_grid()
{
	int[] table = GFX_REGIONS[2];
	int x,y,counter;

	counter = 0x400;

	x = 0; //Machine->visible_area.min_x;
	y = 0; //Machine->visible_area.min_y;
	while (y <= 223)
	{
		x = 4 * (table[counter&0x7ff] & 0x7f);
		if (x >= 0 && x <= 255)
		{
			if ((table[counter&0x7ff] & 0x80) != 0)	// star
			{
				if ((int)(Math.random()*2) == 1)	// noise coming from sound board
					pixels[y + x * 224] = palette_get_color(256);
			}
			else if (grid_on != 0)			// radar
				pixels[y + x * 224] = palette_get_color(257);
		}

		counter++;

		if (x >= 4 * (table[counter&0x7ff] & 0x7f))
			y++;
	}
}


public BitMap video_update() { // dkong
	if (global_changed) {
		for (int i = 0; i < dirtybuffer.length; i++) {
			dirtybuffer[i] = true;
		}
		global_changed = false;
	}
	draw_tiles();
	draw_sprites();
	return bitmap;
}

public Vh_refresh radarscp_vu() { return new Radarscp_vu(); }
public class Radarscp_vu implements Vh_refresh {
	public BitMap video_update() {
		palette_set_color(256,0xff,0x00,0x00);	/* stars */

		if (global_changed) {
			for (int i = 0; i < dirtybuffer.length; i++) {
				dirtybuffer[i] = true;
			}
			global_changed = false;
		}

		draw_tiles();
		draw_grid();
		draw_sprites();
		return bitmap;
	}
	public void video_post_update() {}
}

}