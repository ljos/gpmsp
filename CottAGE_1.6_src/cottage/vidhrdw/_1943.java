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

public class _1943 extends MAMEVideo implements VideoEmulator,
												 Vh_refresh,
												 Vh_start,
												 Vh_stop,
												 Vh_convert_color_proms {

int c1943_scrollx = 0xd802;
int c1943_scrolly = 0xd800;
int c1943_bgscrolly = 0xd803;
int gunsmoke_bg_scrollx = 0xd802;
int gunsmoke_bg_scrolly = 0xd800;
public int chon = 0;
public int objon = 0;
public int sc1on = 0;
public int sc2on = 0;
public int bgon = 0;
public int sprite3bank = 0;
int flipscreen;


BitMap sc2bitmap;
BitMap sc1bitmap;
int[][][] sc2map = new int[9][8][2];
int[][][] sc1map = new int[9][9][2];


public void init(MachineDriver md) {
	super.init_bis(md);
	super.init(md);
}

/***************************************************************************

  Convert the color PROMs into a more useable format.

  1943 has three 256x4 palette PROMs (one per gun) and a lot ;-) of 256x4
  lookup table PROMs.
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

		bit0 = (color_prom[i] >> 0) & 0x01;
		bit1 = (color_prom[i] >> 1) & 0x01;
		bit2 = (color_prom[i] >> 2) & 0x01;
		bit3 = (color_prom[i] >> 3) & 0x01;
		r = 0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3;
		bit0 = (color_prom[i + total_colors] >> 0) & 0x01;
		bit1 = (color_prom[i + total_colors] >> 1) & 0x01;
		bit2 = (color_prom[i + total_colors] >> 2) & 0x01;
		bit3 = (color_prom[i + total_colors] >> 3) & 0x01;
		g = 0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3;
		bit0 = (color_prom[i + 2*total_colors] >> 0) & 0x01;
		bit1 = (color_prom[i + 2*total_colors] >> 1) & 0x01;
		bit2 = (color_prom[i + 2*total_colors] >> 2) & 0x01;
		bit3 = (color_prom[i + 2*total_colors] >> 3) & 0x01;
		b = 0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3;

		palette_set_color(i,r,g,b);
	}

	cptr += 3*total_colors;
	/* color_prom now points to the beginning of the lookup table */

	/* characters use colors 64-79 */
	for (i = 0;i < TOTAL_COLORS(0);i++)
		COLOR(0,i,color_prom[cptr++] + 64);
	cptr += 128;	/* skip the bottom half of the PROM - not used */

	/* foreground tiles use colors 0-63 */
	for (i = 0;i < TOTAL_COLORS(1);i++)
	{
		/* color 0 MUST map to pen 0 in order for transparency to work */
		if (i % color_granularity(1) == 0)
			COLOR(1,i,0);
		else
			COLOR(1,i, color_prom[0 + cptr] + 16 * (color_prom[256 + cptr] & 0x03));
		cptr++;
	}
	cptr += TOTAL_COLORS(1);

	/* background tiles use colors 0-63 */
	for (i = 0;i < TOTAL_COLORS(2);i++)
	{
		COLOR(2,i,color_prom[0 + cptr] + 16 * (color_prom[256 + cptr] & 0x03));
		cptr++;
	}
	cptr += TOTAL_COLORS(2);

	/* sprites use colors 128-255 */
	/* bit 3 of BMPROM.07 selects priority over the background, but we handle */
	/* it differently for speed reasons */
	for (i = 0;i < TOTAL_COLORS(3);i++)
	{
		COLOR(3,i,color_prom[0 + cptr] + 16 * (color_prom[256 + cptr] & 0x07) + 128);
		cptr++;
	}
	cptr += TOTAL_COLORS(3);
}

public Vh_convert_color_proms gunsmoke_pi() { return new Gunsmoke_pi(); }
public class Gunsmoke_pi implements Vh_convert_color_proms {
	public void palette_init() {
		int i;
		int cptr = 0;

		for (i = 0;i < total_colors;i++)
		{
			int bit0,bit1,bit2,bit3,r,g,b;

			bit0 = (color_prom[i] >> 0) & 0x01;
			bit1 = (color_prom[i] >> 1) & 0x01;
			bit2 = (color_prom[i] >> 2) & 0x01;
			bit3 = (color_prom[i] >> 3) & 0x01;
			r = 0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3;
			bit0 = (color_prom[i + total_colors] >> 0) & 0x01;
			bit1 = (color_prom[i + total_colors] >> 1) & 0x01;
			bit2 = (color_prom[i + total_colors] >> 2) & 0x01;
			bit3 = (color_prom[i + total_colors] >> 3) & 0x01;
			g = 0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3;
			bit0 = (color_prom[i + 2*total_colors] >> 0) & 0x01;
			bit1 = (color_prom[i + 2*total_colors] >> 1) & 0x01;
			bit2 = (color_prom[i + 2*total_colors] >> 2) & 0x01;
			bit3 = (color_prom[i + 2*total_colors] >> 3) & 0x01;
			b = 0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3;

			palette_set_color(i,r,g,b);
		}

		cptr += 3*total_colors;
		/* color_prom now points to the beginning of the lookup table */

		/* characters use colors 64-79 */
		for (i = 0;i < TOTAL_COLORS(0);i++)
			COLOR(0,i,color_prom[cptr++] + 64);
		cptr += 128;	/* skip the bottom half of the PROM - not used */

		/* background tiles use colors 0-63 */
		for (i = 0;i < TOTAL_COLORS(1);i++)
		{
			COLOR(1,i,color_prom[0 + cptr] + 16 * (color_prom[256 + cptr] & 0x03));
			cptr++;
		}
		cptr += TOTAL_COLORS(1);

		/* sprites use colors 128-255 */
		/* bit 3 of BMPROM.07 selects priority over the background, but we handle */
		/* it differently for speed reasons */
		for (i = 0;i < TOTAL_COLORS(2);i++)
		{
			COLOR(2,i,color_prom[0 + cptr] + 16 * (color_prom[256 + cptr] & 0x07) + 128);
			cptr++;
		}
		cptr += TOTAL_COLORS(2);
	}
}


public int vh_start() {
	sc2bitmap = new BitMapImpl(8*32, 9*32);
	sc1bitmap = new BitMapImpl(9*32, 9*32, true);
	gfxMan[1].setTransparencyOverwrite(true); // transparent layer
	return 0;
}

public Vh_start gunsmoke_vs() { return new Gunsmoke_vs(); }
public class Gunsmoke_vs implements Vh_start {
	public int vh_start() {
		sc1bitmap = new BitMapImpl(9*32, 9*32, true);
		return 0;
	}
}

public BitMap video_update() {
	int offs,sx,sy;
	int bg_scrolly, bg_scrollx;
	int p;
	int top,left,xscroll,yscroll;

	if (sc2on != 0)
	{
		p = 0x8000;
		bg_scrolly = RAM[c1943_bgscrolly] + 256 * RAM[c1943_bgscrolly + 1];
		offs = 16 * ((bg_scrolly>>5)+8);

		//top = 8 - (bg_scrolly>>5) % 9;
		top = 0;

		bg_scrolly&=0x1f;

		for (sy = 0;sy <9;sy++)
		{
			int ty = (sy + top) % 9;

			offs &= 0x7fff; // Enforce limits (for top of scroll)

			for (sx = 0;sx < 8;sx++)
			{
				int[] map = sc2map[ty][sx];
				int tile, attr, offset;
				offset=offs+2*sx;

				tile=GFX_REGIONS[4][p + offset];
				attr=GFX_REGIONS[4][p + offset + 1];

				if (tile != map[0] || attr != map[1])
				{
					map[0] = tile;
					map[1] = attr;
					drawgfx(sc2bitmap,2,
							tile,
							(attr & 0x3c) >> 2,
							attr&0x40, attr&0x80,
							(8-ty)*32, sx*32,
							GfxManager.TRANSPARENCY_NONE,0);
				}
			}
			offs-=0x10;
		}

		xscroll = 287-bg_scrolly;
		yscroll = 0;
		copyscrollbitmap(bitmap,sc2bitmap,
			1,xscroll,
			1,yscroll,
			GfxManager.TRANSPARENCY_NONE,0);
	}
	else fillbitmap(bitmap,get_black_pen(),cliprect);


	if (objon != 0)
	{
		// Draw the sprites which don't have priority over the foreground.
		for (offs = spriteram_size - 32;offs >= 0;offs -= 32)
		{
			int color;


			color = RAM[spriteram + offs + 1] & 0x0f;
			if (color == 0x0a || color == 0x0b)	// the priority is actually selected by
												// bit 3 of BMPROM.07
			{
				sx = RAM[spriteram + offs + 3] - ((RAM[spriteram + offs + 1] & 0x10) << 4);
				sy = RAM[spriteram + offs + 2];

				drawgfx(bitmap,3,
						RAM[spriteram + offs] + ((RAM[spriteram + offs + 1] & 0xe0) << 3),
						color,
						false,false,
						sx,sy,
						GfxManager.TRANSPARENCY_PEN,0);
			}
		}
	}


// TODO: support flipscreen
	if (sc1on != 0)
	{
		bg_scrolly = RAM[c1943_scrolly + 0] + 256 * RAM[c1943_scrolly + 1];
		bg_scrollx = RAM[c1943_scrollx + 0];
		offs = 16 * ((bg_scrolly>>5)+8)+2*(bg_scrollx>>5) ;
		if ((bg_scrollx & 0x80) != 0) offs -= 0x10;

		//top = 8 - (bg_scrolly>>5) % 9;
		top = 0;
		//left = (bg_scrollx>>5) % 9;
		left = 0;

		bg_scrolly&=0x1f;
		bg_scrollx&=0x1f;

		for (sy = 0;sy <9;sy++)
		{
			int ty = (sy + top) % 9;
			offs &= 0x7fff; // Enforce limits (for top of scroll)

			for (sx = 0;sx < 9;sx++)
			{
				int tile, attr, offset;
				int tx = (sx + left) % 9;
				int[] map = sc1map[ty][tx];
				offset=offs+(sx*2);

				tile=GFX_REGIONS[4][offset];
				attr=GFX_REGIONS[4][offset+1];

				if (tile != map[0] || attr != map[1])
				{
					map[0] = tile;
					map[1] = attr;
					tile+=256*(attr&0x01);
					drawgfx(sc1bitmap,1,
							tile,
							(attr & 0x3c) >> 2,
							attr & 0x40,attr & 0x80,
							(8-ty)*32, tx*32,
							GfxManager.TRANSPARENCY_COLOR,0);
				}
			}
			offs-=0x10;
		}

		xscroll = 255-bg_scrolly;
		yscroll = bg_scrollx;
		copyscrollbitmap(bitmap,sc1bitmap,
			1,xscroll,
			1,yscroll,
			GfxManager.TRANSPARENCY_COLOR,0);
	}


	if (objon != 0)
	{
		// Draw the sprites which have priority over the foreground.
		for (offs = spriteram_size - 32;offs >= 0;offs -= 32)
		{
			int color;


			color = RAM[spriteram + offs + 1] & 0x0f;
			if (color != 0x0a && color != 0x0b)	// the priority is actually selected by
												// bit 3 of BMPROM.07
			{
				sx = RAM[spriteram + offs + 3] - ((RAM[spriteram + offs + 1] & 0x10) << 4);
				sy = RAM[spriteram + offs + 2];

				drawgfx(bitmap,3,
						RAM[spriteram + offs] + ((RAM[spriteram + offs + 1] & 0xe0) << 3),
						color,
						false,false,
						sx,sy,
						GfxManager.TRANSPARENCY_PEN,0);
			}
		}
	}


	if (chon != 0)
	{
		// draw the frontmost playfield. They are characters, but draw them as sprites
		for (offs = videoram_size - 1;offs >= 0;offs--)
		{
			sx = offs % 32;
			sy = offs / 32;

			drawgfx(bitmap,0,
					RAM[videoram + offs] + ((RAM[colorram + offs] & 0xe0) << 3),
					RAM[colorram + offs] & 0x1f,
					false,false,
					8*sx,8*sy,
					GfxManager.TRANSPARENCY_COLOR,79);
		}
	}
	return bitmap;
}

public Vh_refresh gunsmoke_vu() { return new Gunsmoke_vu(); }
public class Gunsmoke_vu implements Vh_refresh {
	public BitMap video_update() {
		int offs,sx,sy;
		int bg_scrolly, bg_scrollx;
		int p;
		int top,left,xscroll,yscroll;

		if (bgon != 0)
		{
			bg_scrolly = RAM[gunsmoke_bg_scrolly + 0] + 256 * RAM[gunsmoke_bg_scrolly + 1];
			bg_scrollx = RAM[gunsmoke_bg_scrollx + 0];
			offs = 16 * ((bg_scrolly>>5)+8)+2*(bg_scrollx>>5) ;
			if ((bg_scrollx & 0x80) != 0) offs -= 0x10;

			//top = 8 - (bg_scrolly>>5) % 9;
			top = 0;
			//left = (bg_scrollx>>5) % 9;
			left = 0;

			bg_scrolly&=0x1f;
			bg_scrollx&=0x1f;

			for (sy = 0;sy <9;sy++)
			{
				int ty = (sy + top) % 9;
				offs &= 0x7fff; // Enforce limits (for top of scroll)

				for (sx = 0;sx < 9;sx++)
				{
					int tile, attr, offset;
					int tx = (sx + left) % 9;
					int[] map = sc1map[ty][tx];
					offset=offs+(sx*2);

					tile=GFX_REGIONS[3][offset];
					attr=GFX_REGIONS[3][offset+1];

					if (tile != map[0] || attr != map[1])
					{
						map[0] = tile;
						map[1] = attr;
						tile+=256*(attr&0x01);
						drawgfx(sc1bitmap,1,
								tile,
								(attr & 0x3c) >> 2,
								attr & 0x40,attr & 0x80,
								(8-ty)*32, tx*32,
								GfxManager.TRANSPARENCY_NONE,0);
					}
				}
				offs-=0x10;
			}

			xscroll = 255-bg_scrolly;
			yscroll = bg_scrollx;
			copyscrollbitmap(bitmap,sc1bitmap,
				1,xscroll,
				1,yscroll,
				GfxManager.TRANSPARENCY_NONE,0);
		} else fillbitmap(bitmap,get_black_pen(),cliprect);

		if (objon != 0)
		{
			/* Draw the sprites. */
			for (offs = spriteram_size - 32;offs >= 0;offs -= 32)
			{
				int bank,flipx,flipy;

				bank = (RAM[spriteram + offs + 1] & 0xc0) >> 6;
				if (bank == 3) bank += sprite3bank;

				sx = RAM[spriteram + offs + 3] - ((RAM[spriteram + offs + 1] & 0x20) << 3);
				sy = RAM[spriteram + offs + 2];
				flipx = 0;
				flipy = RAM[spriteram + offs + 1] & 0x10;

				drawgfx(bitmap,2,
						RAM[spriteram + offs] + 256 * bank,
						RAM[spriteram + offs + 1] & 0x0f,
						flipx,flipy,
						sx,sy,
						GfxManager.TRANSPARENCY_PEN,0);
			}
		}

		if (chon != 0)
		{
			// draw the frontmost playfield. They are characters, but draw them as sprites
			for (offs = videoram_size - 1;offs >= 0;offs--)
			{
				sx = offs % 32;
				sy = offs / 32;

				drawgfx(bitmap,0,
						RAM[videoram + offs] + ((RAM[colorram + offs] & 0xc0) << 2),
						RAM[colorram + offs] & 0x1f,
						true,true,
						8*sx,8*sy,
						GfxManager.TRANSPARENCY_COLOR,79);
			}
		}
		return bitmap;
	}
	public void video_post_update() {}
}

}