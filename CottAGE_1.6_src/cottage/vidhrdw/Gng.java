/***************************************************************************

  vidhrdw.c

  Functions to emulate the video hardware of the machine.

***************************************************************************/

package cottage.vidhrdw;

import cottage.mame.MAMEVideo;
import jef.map.*;
import jef.machine.*;
import jef.video.*;

public class Gng extends MAMEVideo implements VideoEmulator,Vh_refresh,Vh_start,Vh_stop,Vh_convert_color_proms,Eof_callback {

public int[] Fgng_fgvideoram = {0};
public int[] Fgng_bgvideoram = {0};
static int gng_fgvideoram, gng_bgvideoram;

static boolean fgdirty[] = new boolean[0x400];
static boolean bgdirty[] = new boolean[0x400];

static BitMap fgbitmap;

//static struct tilemap *fg_tilemap, *bg_tilemap;

	/* COTTAGE VIDEO INITIALIZATION */
	public void init(MachineDriver md) {
		super.init_bis(md);
		super.init(md);
		gng_fgvideoram = Fgng_fgvideoram[0];
		gng_bgvideoram = Fgng_bgvideoram[0];
	}

/***************************************************************************

  Callbacks for the TileMap code

***************************************************************************/

/*static void get_fg_tile_info(int tile_index)
{
	unsigned char attr = gng_fgvideoram[tile_index + 0x400];
	SET_TILE_INFO(
			0,
			gng_fgvideoram[tile_index] + ((attr & 0xc0) << 2),
			attr & 0x0f,
			TILE_FLIPYX((attr & 0x30) >> 4))
}

static void get_bg_tile_info(int tile_index)
{
	unsigned char attr = gng_bgvideoram[tile_index + 0x400];
	SET_TILE_INFO(
			1,
			gng_bgvideoram[tile_index] + ((attr & 0xc0) << 2),
			attr & 0x07,
			TILE_FLIPYX((attr & 0x30) >> 4) | TILE_SPLIT((attr & 0x08) >> 3))
}*/

/***************************************************************************

  Start the video hardware emulation.

***************************************************************************/
public int vh_start()
{
/*	fg_tilemap = tilemap_create(get_fg_tile_info,tilemap_scan_rows,TILEMAP_TRANSPARENT,8,8,32,32);
	bg_tilemap = tilemap_create(get_bg_tile_info,tilemap_scan_cols,TILEMAP_SPLIT,    16,16,32,32);

	if (!fg_tilemap || !bg_tilemap)
		return 1;

	tilemap_set_transparent_pen(fg_tilemap,3);

	tilemap_set_transmask(bg_tilemap,0,0xff,0x00);
	tilemap_set_transmask(bg_tilemap,1,0x41,0xbe);

	return 0;
*/

	fgbitmap = new BitMapImpl(256,256);

	tmpbitmap = new BitMapImpl(512,512);

	gfxMan[0].setTransparencyOverwrite(true);

	return 0;
}


/***************************************************************************

  Memory handlers

***************************************************************************/

public WriteHandler gng_fgvideoram_w() { return new Gng_fgvideoram_w(); }
class Gng_fgvideoram_w implements WriteHandler {
	public void write(int address, int data)
	{
		RAM[address] = data;
		fgdirty[address & 0x3ff] = true;		
	}
}

public WriteHandler gng_bgvideoram_w() { return new Gng_bgvideoram_w(); }
class Gng_bgvideoram_w implements WriteHandler {
	public void write(int address, int data)
	{
		RAM[address] = data;
		bgdirty[address & 0x3ff] = true;		
	}
}

static int scrollx[] = new int[2];

public WriteHandler gng_bgscrollx_w() { return new Gng_bgscrollx_w(); }
class Gng_bgscrollx_w implements WriteHandler {
	public void write(int address, int data)
	{
		scrollx[address & 0x1] = data;
	}
}

static int scrolly[] = new int[2];

public WriteHandler gng_bgscrolly_w() { return new Gng_bgscrolly_w(); }
class Gng_bgscrolly_w implements WriteHandler {
	public void write(int address, int data)
	{
		scrolly[address & 0x1] = data;
	}
}

/***************************************************************************

  Display refresh

***************************************************************************/

public void draw_sprites()
{
	int gfx = Machine_gfx[2];
	int offs;

	for (offs = 0x200 - 4;offs >= 0;offs -= 4)
	{
		int attributes = RAM[0x1e00 + offs+1];
		int sx = RAM[0x1e00 + offs + 3] - 0x100 * (attributes & 0x01);
		int sy = RAM[0x1e00 + offs + 2];
		int flipx = attributes & 0x04;
		int flipy = attributes & 0x08;

/*		if (flip_screen)
		{
			sx = 240 - sx;
			sy = 240 - sy;
			flipx = !flipx;
			flipy = !flipy;
		}*/

		drawgfx(bitmap,gfx,
				RAM[0x1e00 + offs] + ((attributes<<2) & 0x300),
				(attributes >> 4) & 3,
				flipx,flipy,
				sx,sy,
				GfxManager.TRANSPARENCY_PEN,15);
	}
}

public BitMap video_update()
{
	int offs;

	/* for every character in the Video RAM, check if it has been modified */
	/* since last time and update it accordingly. */
	for (offs = 0x400 - 1;offs >= 0;offs--)
	{
		int	color = RAM[gng_bgvideoram + offs + 0x400];

		if (bgdirty[offs] || gfxMan[1].colorCodeHasChanged(color&0x07)) {

			bgdirty[offs] = false;

			int sx,sy,flipx,flipy;

			sx = offs / 32;
			sy = offs % 32;

			int code = RAM[gng_bgvideoram + offs];

			flipx = color & 0x10;
			flipy = color & 0x20;

			//if (((color & 0x08) >> 3)==0)
			drawgfx(tmpbitmap,1,
					code + ((color & 0xc0) << 2),
					color & 0x07,
					flipx,flipy,
					16*sx,16*sy,
					GfxManager.TRANSPARENCY_NONE,0);
		}
	}

	int x,y;
	int ofssrc, ofsdst;
	int[] srcpix;
	int[] dstpix;

	srcpix = tmpbitmap.getPixels();
	dstpix = bitmap.getPixels();

	int scroll = ((scrollx[0] | (scrollx[1] << 8)))&511;

//	System.out.println("SCROLLX="+scrollx);

	ofsdst = 0;

	if ((scroll + 256) <= 512) {

		ofssrc = scroll;
		// No wrap
		for(y=0; y<224; y++) {
			for(x=0; x<256; x++) {
				dstpix[ofsdst++] = srcpix[ofssrc++];
			}
			ofssrc+=256;
		}

	} else {

		ofssrc = scroll;
		ofsdst = 0;

		// Wrap
		for(y=0; y<224; y++) {
			for(x=0; x<(512-scroll); x++) {
				dstpix[ofsdst++] = srcpix[ofssrc++];
			}
			ofssrc+=scroll;
			ofsdst+=256-(512-scroll);
		}

		ofssrc = 0;
		ofsdst = (512-scroll);

		for(y=0; y<224; y++) {
			for(x=0; x<(256-(512-scroll)); x++) {
				dstpix[ofsdst++] = srcpix[ofssrc++];
			}
			ofssrc+=512-(256-(512-scroll));
			ofsdst+=(512-scroll);
		}

	}

	draw_sprites();

	/* for every character in the Video RAM, check if it has been modified */
	/* since last time and update it accordingly. */
/*	for (offs = 0x400 - 1;offs >= 0;offs--)
	{
		int sx,sy,flipx,flipy;

		sx = offs / 32;
		sy = offs % 32;

		int code = RAM[gng_bgvideoram + offs];
		int color = RAM[gng_bgvideoram + offs + 0x400];

		flipx = color & 0x10;
		flipy = color & 0x20;

		if (((color & 0x08) >> 3)==1)
		drawgfx(bitmap,1,
				code + ((color & 0xc0) << 2),
				color & 0x07,
				flipx,flipy,
				16*sx,16*sy,
				TRANSPARENCY_NONE,0);
	}*

	/* for every character in the Video RAM, check if it has been modified */
	/* since last time and update it accordingly. */
	for (offs = 0x400 - 1;offs >= 0;offs--)
	{
		int color = RAM[gng_fgvideoram + offs + 0x400];

		//if (fgdirty[offs] || gfxMan[0].colorCodeHasChanged(color&0x0f)) {

			fgdirty[offs] = false;

			int sx,sy,flipx,flipy;

			sx = offs % 32;
			sy = offs / 32;

			flipx = color & 0x10;
			flipy = color & 0x20;

			int code = RAM[gng_fgvideoram + offs];

			drawgfx(fgbitmap,0,
					code + ((color & 0xc0) << 2),
					color & 0x0f,
					flipx,flipy,
					8*sx,8*sy,
					GfxManager.TRANSPARENCY_PEN,3);
		//}
	}

	int c;

	srcpix = fgbitmap.getPixels();
	dstpix = bitmap.getPixels();

	ofsdst = 0;
	ofssrc = 0;
	// No wrap
	for(y=0; y<224; y++) {
		for(x=0; x<256; x++) {
			c = srcpix[ofssrc];
			if (c != -1)
				dstpix[ofsdst] = c;
			ofsdst++;
			ofssrc++;
		}
	}

	return bitmap;
}

public void eof_callback()
{
	buffer_spriteram_w(0,0);
}

}