/***************************************************************************

  vidhrdw.c

  Functions to emulate the video hardware of the machine.

***************************************************************************/

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

public class _1942 extends MAMEVideo implements VideoEmulator,Vh_refresh,Vh_start,Vh_stop,Vh_convert_color_proms {

public int[] Fc1942_fgvideoram = {0};
public int[] Fc1942_bgvideoram = {0};
static int c1942_fgvideoram, c1942_bgvideoram;

static int c1942_palette_bank;
//static struct tilemap *fg_tilemap, *bg_tilemap;

	/* COTTAGE VIDEO INITIALIZATION */
	public void init(MachineDriver md) {
		super.init_bis(md);
		super.init(md);
		c1942_fgvideoram = Fc1942_fgvideoram[0];
		c1942_bgvideoram = Fc1942_bgvideoram[0];
	}


/***************************************************************************

  Convert the color PROMs into a more useable format.

  1942 has three 256x4 palette PROMs (one per gun) and three 256x4 lookup
  table PROMs (one for characters, one for sprites, one for background tiles).
  The palette PROMs are connected to the RGB output this way:

  bit 3 -- 220 ohm resistor  -- RED/GREEN/BLUE
        -- 470 ohm resistor  -- RED/GREEN/BLUE
        -- 1  kohm resistor  -- RED/GREEN/BLUE
  bit 0 -- 2.2kohm resistor  -- RED/GREEN/BLUE

***************************************************************************/
public void palette_init() {
	int i;
	int cptr = 0;

	for (i = 0;i < total_colors;i++)
	{
		int bit0,bit1,bit2,bit3,r,g,b;

		/* red component */
		bit0 = (color_prom[i] >> 0) & 0x01;
		bit1 = (color_prom[i] >> 1) & 0x01;
		bit2 = (color_prom[i] >> 2) & 0x01;
		bit3 = (color_prom[i] >> 3) & 0x01;
		r = 0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3;
		/* green component */
		bit0 = (color_prom[i + total_colors] >> 0) & 0x01;
		bit1 = (color_prom[i + total_colors] >> 1) & 0x01;
		bit2 = (color_prom[i + total_colors] >> 2) & 0x01;
		bit3 = (color_prom[i + total_colors] >> 3) & 0x01;
		g = 0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3;
		/* blue component */
		bit0 = (color_prom[i + 2*total_colors] >> 0) & 0x01;
		bit1 = (color_prom[i + 2*total_colors] >> 1) & 0x01;
		bit2 = (color_prom[i + 2*total_colors] >> 2) & 0x01;
		bit3 = (color_prom[i + 2*total_colors] >> 3) & 0x01;
		b = 0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3;

		palette_set_color(i,r,g,b);
	}

	cptr += 3*total_colors;
	/* color_prom now points to the beginning of the lookup table */

	/* characters use colors 128-143 */
	for (i = 0;i < TOTAL_COLORS(0);i++)
		COLOR(0,i,color_prom[cptr++] + 128);

	/* background tiles use colors 0-63 in four banks */
	for (i = 0;i < TOTAL_COLORS(1)/4;i++)
	{
		COLOR(1,i, color_prom[cptr]);
		COLOR(1,i+32*8, color_prom[cptr] + 16);
		COLOR(1,i+2*32*8, color_prom[cptr] + 32);
		COLOR(1,i+3*32*8, color_prom[cptr] + 48);
		cptr++;
	}

	/* sprites use colors 64-79 */
	for (i = 0;i < TOTAL_COLORS(2);i++)
		COLOR(2,i, color_prom[cptr++] + 64);
}


/***************************************************************************

  Callbacks for the TileMap code

***************************************************************************/

/*static void get_fg_tile_info(int tile_index)
{
	int code, color;

	code = c1942_fgvideoram[tile_index];
	color = c1942_fgvideoram[tile_index + 0x400];
	SET_TILE_INFO(
			0,
			code + ((color & 0x80) << 1),
			color & 0x3f,
			0)
}*/

/*static void get_bg_tile_info(int tile_index)
{
	int code, color;

	tile_index = (tile_index & 0x0f) | ((tile_index & 0x01f0) << 1);

	code = c1942_bgvideoram[tile_index];
	color = c1942_bgvideoram[tile_index + 0x10];
	SET_TILE_INFO(
			1,
			code + ((color & 0x80) << 1),
			(color & 0x1f) + (0x20 * c1942_palette_bank),
			TILE_FLIPYX((color & 0x60) >> 5))
}*/


/***************************************************************************

  Start the video hardware emulation.

***************************************************************************/
public int vh_start()
{
/*	fg_tilemap = tilemap_create(get_fg_tile_info,tilemap_scan_rows,TILEMAP_TRANSPARENT, 8, 8,32,32);
	bg_tilemap = tilemap_create(get_bg_tile_info,tilemap_scan_cols,TILEMAP_OPAQUE,     16,16,32,16);

	if (!fg_tilemap || !bg_tilemap)
		return 1;

	tilemap_set_transparent_pen(fg_tilemap,0);
*/

	tmpbitmap = new BitMapImpl(256,512);

	return 0;
}


/***************************************************************************

  Memory handlers

***************************************************************************/

public WriteHandler c1942_fgvideoram_w() { return new C1942_fgvideoram_w(); }
class C1942_fgvideoram_w implements WriteHandler {
	public void write(int address, int data)
	{
		RAM[address] = data;
	}
}

/*WRITE_HANDLER( c1942_fgvideoram_w )
{
	c1942_fgvideoram[offset] = data;
	tilemap_mark_tile_dirty(fg_tilemap,offset & 0x3ff);
}*/

public WriteHandler c1942_bgvideoram_w() { return new C1942_bgvideoram_w(); }
class C1942_bgvideoram_w implements WriteHandler {
	public void write(int address, int data)
	{
		RAM[address] = data;
	}
}

/*WRITE_HANDLER( c1942_bgvideoram_w )
{
	c1942_bgvideoram[offset] = data;
	tilemap_mark_tile_dirty(bg_tilemap,(offset & 0x0f) | ((offset >> 1) & 0x01f0));
}*/

public WriteHandler c1942_palette_bank_w() { return new C1942_palette_bank_w(); }
class C1942_palette_bank_w implements WriteHandler {
	public void write(int address, int data)
	{
		if (c1942_palette_bank != data)
			c1942_palette_bank = data;
	}
}

/*
WRITE_HANDLER( c1942_palette_bank_w )
{
	if (c1942_palette_bank != data)
	{
		c1942_palette_bank = data;
		tilemap_mark_all_tiles_dirty(bg_tilemap);
	}
}*/

static int scroll[] = new int[2];

public WriteHandler c1942_scroll_w() { return new C1942_scroll_w(); }
class C1942_scroll_w implements WriteHandler {
	public void write(int address, int data)
	{
		scroll[address & 0x1] = data;
	}
}

/*WRITE_HANDLER( c1942_scroll_w )
{
	static unsigned char scroll[2];

	scroll[offset] = data;
	tilemap_set_scrollx(bg_tilemap,0,scroll[0] | (scroll[1] << 8));
}*/


/*WRITE_HANDLER( c1942_c804_w )
{*/
	/* bit 7: flip screen
       bit 4: cpu B reset
	   bit 0: coin counter */

/*	coin_counter_w(0,data & 0x01);

	cpu_set_reset_line(1,(data & 0x10) ? ASSERT_LINE : CLEAR_LINE);

	flip_screen_set(data & 0x80);
}*/


/***************************************************************************

  Display refresh

***************************************************************************/

public void draw_sprites()
{
	int offs;


	for (offs = spriteram_size - 4;offs >= 0;offs -= 4)
	{
		int i,code,col,sx,sy,dir;


		code = (RAM[spriteram + offs] & 0x7f) + 4*(RAM[spriteram + offs + 1] & 0x20)
				+ 2*(RAM[spriteram + offs] & 0x80);
		col = RAM[spriteram + offs + 1] & 0x0f;
		sx = RAM[spriteram + offs + 3] - 0x10 * (RAM[spriteram + offs + 1] & 0x10);
		sy = RAM[spriteram + offs + 2];
		dir = 1;
/*		if (flip_screen)
		{
			sx = 240 - sx;
			sy = 240 - sy;
			dir = -1;
		}*/

		/* handle double / quadruple height */
		i = (RAM[spriteram + offs + 1] & 0xc0) >> 6;
		if (i == 2) i = 3;

		do
		{
			drawgfx(bitmap,Machine_gfx[2],
					code + i,col,
					0,0,
					sx,sy + 16 * i * dir,
					GfxManager.TRANSPARENCY_PEN,15);

			i--;
		} while (i >= 0);
	}


}

public BitMap video_update()
{
	int offs=0;

	/* for every character in the Video RAM, check if it has been modified */
	/* since last time and update it accordingly. */
	for (offs = 0x200 - 1;offs >= 0;offs--)
	{
		int sx,sy,flipx,flipy;

		sx = offs % 16;
		sy = offs / 16;

		int tile_index = (offs & 0x0f) | ((offs & 0x01f0) << 1);

		int code = RAM[c1942_bgvideoram + tile_index];
		int color = RAM[c1942_bgvideoram + tile_index + 0x10];

		flipx = color & 0x20;
		flipy = color & 0x40;

		drawgfx(tmpbitmap,1,
				code + ((color & 0x80) << 1),
				(color & 0x1f) + (0x20 * c1942_palette_bank),
				flipx,flipy,
				16*sy,16*sx,
				GfxManager.TRANSPARENCY_NONE,0);
	}

	int x,y;
	int ofssrc, ofsdst;
	int[] srcpix;
	int[] dstpix;

	srcpix = tmpbitmap.getPixels();
	dstpix = bitmap.getPixels();

	int scrollx = 256 - ((scroll[0] | (scroll[1] << 8)))&511;

//	System.out.println("SCROLLX="+scrollx);

	if (scrollx < 0) scrollx += 512;

	ofsdst = 0;

	if ((scrollx + 256) <= 512) {

		ofssrc = scrollx * 256;
		// No wrap
		for(y=0; y<256; y++) {
			for(x=0; x<224; x++) {
				dstpix[ofsdst++] = srcpix[ofssrc++];
			}
			ofssrc+=32;
		}

	} else {

		ofssrc = scrollx * 256;

		// Wrap
		for(y=0; y<(512-scrollx); y++) {
			for(x=0; x<224; x++) {
				dstpix[ofsdst++] = srcpix[ofssrc++];
			}
			ofssrc+=32;
		}

		ofssrc = 0;
		for(y=0; y<(256-(512-scrollx)); y++) {
			for(x=0; x<224; x++) {
				dstpix[ofsdst++] = srcpix[ofssrc++];
			}
			ofssrc+=32;
		}

	}

/*
	ofssrc = 0;
	for(y=0; y<256; y++) {
		for(x=0; x<224; x++) {
			dstpix[ofsdst++] = srcpix[ofssrc++];
		}
		ofssrc+=32;
	}
*/

//	scrolling[0] = scroll[0] | (scroll[1] << 8);
//	copyscrollbitmap(bitmap, tmpbitmap, 1, scrolling, 0, 0, visible_area(), TRANSPARENCY_NONE, 0);

//	tilemap_draw(bitmap,cliprect,bg_tilemap,0,0);
//	fillbitmap(bitmap,0,visible_area());
	draw_sprites();
//	tilemap_draw(bitmap,cliprect,fg_tilemap,0,0);

	/* for every character in the Video RAM, check if it has been modified */
	/* since last time and update it accordingly. */
	for (offs = 0x400 - 1;offs >= 0;offs--)
	{
		int sx,sy;

		sx = offs % 32;
		sy = offs / 32;

		int code = RAM[c1942_fgvideoram + offs];
		int color = RAM[c1942_fgvideoram + offs + 0x400];

		drawgfx(bitmap,0,
				code + ((color & 0x80) << 1),
				color & 0x3f,
				0,0,
				8*sx,8*sy,
				GfxManager.TRANSPARENCY_PEN,0);
	}

	return bitmap;
}

}