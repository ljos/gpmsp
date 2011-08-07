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

public class Galaga extends MAMEVideo implements VideoEmulator,
													Vh_refresh,
													Vh_start,
													Vh_stop,
													Vh_convert_color_proms{

BitMap charLayer;

private static final int spriteram		= 0x8b80;
private static final int spriteram_2	= 0x9380;
private static final int spriteram_3	= 0x9b80;
private static final int spriteram_size = 0x80;

private static final int MAX_STARS = 250;
private static final int STARS_COLOR_BASE = 32;

private static final int galaga_starcontrol = 0xa000;
private static int stars_scroll = 0;

class Star {
	public int x = 0;
	public int y = 0;
	public int col = 0;
	public int set = 0;
}

Star[] stars = new Star[MAX_STARS];
static int total_stars;

int[] REGION_PROMS;
int[] REGION_CPU;

	public boolean[] dirtybuffer = new boolean[0x400];

	public void init(MachineDriver md) {
		super.init(md);
		charLayer = new BitMapImpl(backBuffer.getWidth(), backBuffer.getHeight());
	}

	public void setRefs(int[] mem, int[] proms) {
		this.REGION_PROMS = proms;
		this.REGION_CPU	  = mem;
	}

	public boolean galaga() {
		return true;
	}

	public void galaga_vh_interrupt() {
		/* this function is called by galaga_interrupt_1() */
		int s0,s1,s2;
		int[] speeds = { 2, 3, 4, 0, -4, -3, -2, 0 };


		s0 = REGION_CPU[galaga_starcontrol + 0] & 1;
		s1 = REGION_CPU[galaga_starcontrol + 1] & 1;
		s2 = REGION_CPU[galaga_starcontrol + 2] & 1;

		stars_scroll -= speeds[s0 + s1*2 + s2*4];
	}

	public int vh_start() {
		int generator;
		int x,y;
		int set = 0;

		/* precalculate the star background */
		/* this comes from the Galaxian hardware, Galaga is probably different */
		total_stars = 0;
		generator = 0;

		for (y = 0;y <= 255;y++)
		{
			for (x = 511;x >= 0;x--)
			{
				int bit1,bit2;


				generator <<= 1;
				bit1 = (~generator >> 17) & 1;
				bit2 = (generator >> 5) & 1;

				if ((bit1 ^ bit2) != 0) generator |= 1;

				if ( (((~generator >> 16) & 1) != 0) && (generator & 0xff) == 0xff)
				{
					int color;

					color = (~(generator >> 8)) & 0x3f;
					if (color != 0 && total_stars < MAX_STARS)
					{
						stars[total_stars] = new Star();
						stars[total_stars].x = x;
						stars[total_stars].y = y;
						stars[total_stars].col = Machine_pens[color + STARS_COLOR_BASE];
						stars[total_stars].set = set;
						if (++set > 3)
							set = 0;

						total_stars++;
					}
				}
			}
		}

		return 0;
	}

	public BitMap video_update() {
		int offs;

		/* for every character in the Video RAM, check if it has been modified */
		/* since last time and update it accordingly. */
		for (offs = 0x400 - 1;offs >= 0;offs--) {
			if (dirtybuffer[offs]) {
				int sx,sy,mx,my;

				dirtybuffer[offs] = false;

			/* Even if Galaga's screen is 28x36, the memory layout is 32x32. We therefore */
			/* have to convert the memory coordinates into screen coordinates. */
			/* Note that 32*32 = 1024, while 28*36 = 1008: therefore 16 bytes of Video RAM */
			/* don't map to a screen position. We don't check that here, however: range */
			/* checking is performed by drawgfx(). */

				mx = offs % 32;
				my = offs / 32;

				if (my <= 1) {
					sx = my + 34;
					sy = mx - 2;
				} else if (my >= 30) {
					sx = my - 30;
					sy = mx - 2;
				} else {
					sx = mx + 2;
					sy = my - 2;
				}

				drawgfx(charLayer,0,
						REGION_CPU[0x8000 + offs]&127,
						REGION_CPU[0x8400 + offs]&31,
						false,false,
						sx*8,sy*8,
						GfxManager.TRANSPARENCY_NONE,0);
			}
		}

		charLayer.toPixels(pixels);

		/* Draw the sprites. */
		for (offs = 0;offs < spriteram_size;offs += 2)
		{
			if ((REGION_CPU[spriteram_3 + offs + 1] & 2) == 0)
			{
				int code,color,flipx,flipy,sx,sy,sfa,sfb;


				code = REGION_CPU[spriteram + offs];
				color = REGION_CPU[spriteram + offs + 1] & 31;
				flipy = REGION_CPU[spriteram_3 + offs] & 2;
				flipx = REGION_CPU[spriteram_3 + offs] & 1;
				sx = (REGION_CPU[spriteram_2 + offs + 1] & 0xff) - 40 + 0x100*(REGION_CPU[spriteram_3 + offs + 1] & 1);
				sy = 28*8 - (REGION_CPU[spriteram_2 + offs] & 0xff);
				sfb = 0;
				sfa = 16;

				/* this constraint fell out of the old, pre-rotated code automatically */
				/* we need to explicitly add it because of the way we flip Y */
				if (sy <= -16)
					continue;

				if ((REGION_CPU[spriteram_3 + offs] & 0x0c) == 0x0c)		/* double width, double height */
				{
					drawgfx(bitmap,1,
							code+1,color,flipx,flipy,sx+sfa,sy-sfa,
							GfxManager.TRANSPARENCY_COLOR,0);
					drawgfx(bitmap,1,
							code+3,color,flipx,flipy,sx+sfa,sy-sfb,
							GfxManager.TRANSPARENCY_COLOR,0);

					drawgfx(bitmap,1,
							code,color,flipx,flipy,sx+sfb,sy-sfa,
							GfxManager.TRANSPARENCY_COLOR,0);
					drawgfx(bitmap,1,
							code+2,color,flipx,flipy,sx+sfb,sy-sfb,
							GfxManager.TRANSPARENCY_COLOR,0);
				}
				else if ((REGION_CPU[spriteram_3 + offs] & 8) != 0)	/* double width */
				{
					drawgfx(bitmap,1,
							code,color,flipx,flipy,sx,sy-sfa,
							GfxManager.TRANSPARENCY_COLOR,0);
					drawgfx(bitmap,1,
							code+2,color,flipx,flipy,sx,sy-sfb,
							GfxManager.TRANSPARENCY_COLOR,0);
				}
				else if ((REGION_CPU[spriteram_3 + offs] & 4) != 0)	/* double height */
				{
					drawgfx(bitmap,1,
							code+1,color,flipx,flipy,sx+sfa,sy,
							GfxManager.TRANSPARENCY_COLOR,0);
					drawgfx(bitmap,1,
							code,color,flipx,flipy,sx+sfb,sy,
							GfxManager.TRANSPARENCY_COLOR,0);
				}
				else	/* normal */
					drawgfx(bitmap,1,
							code,color,flipx,flipy,sx,sy,
							GfxManager.TRANSPARENCY_COLOR,0);
			}
		}

		/* draw the stars */
		if ((REGION_CPU[galaga_starcontrol + 5] & 1) != 0)
		{
			int bpen;


			bpen = Machine_pens[0];
			for (offs = 0;offs < total_stars;offs++)
			{
				int x,y;
				int set;
				int[][] starset = {{0,3},{0,1},{2,3},{2,1}};

				set = ((REGION_CPU[galaga_starcontrol + 4] << 1) | REGION_CPU[galaga_starcontrol + 3]) & 3;
				if ((stars[offs].set == starset[set][0]) ||
					(stars[offs].set == starset[set][1]))
				{
					x = ((stars[offs].x + stars_scroll) & 511) / 2 + 16;
					y = (stars[offs].y + (stars_scroll + stars[offs].x) / 512) & 255;

					if (y >= visible[2] &&
						y <= visible[3])
					{
						if (read_pixel(bitmap, y, x) == bpen)
							plot_pixel(bitmap, y, x, stars[offs].col);
					}
				}
			}
		}

		return bitmap;
	}


/***************************************************************************

  Convert the color PROMs into a more useable format.

  Galaga has one 32x8 palette PROM and two 256x4 color lookup table PROMs
  (one for characters, one for sprites). Only the first 128 bytes of the
  lookup tables seem to be used.
  The palette PROM is connected to the RGB output this way:

  bit 7 -- 220 ohm resistor  -- BLUE
        -- 470 ohm resistor  -- BLUE
        -- 220 ohm resistor  -- GREEN
        -- 470 ohm resistor  -- GREEN
        -- 1  kohm resistor  -- GREEN
        -- 220 ohm resistor  -- RED
        -- 470 ohm resistor  -- RED
  bit 0 -- 1  kohm resistor  -- RED

***************************************************************************/
	public void palette_init() {
		int i;
		for (i = 0;i < 32;i++) {
			int bit0,bit1,bit2,red,green,blue;


			/* red component */
			bit0 = (REGION_PROMS[31-i] >> 0) & 0x01;
			bit1 = (REGION_PROMS[31-i] >> 1) & 0x01;
			bit2 = (REGION_PROMS[31-i] >> 2) & 0x01;
			red = 0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2;
			/* green component */
			bit0 = (REGION_PROMS[31-i] >> 3) & 0x01;
			bit1 = (REGION_PROMS[31-i] >> 4) & 0x01;
			bit2 = (REGION_PROMS[31-i] >> 5) & 0x01;
			green = 0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2;
			/* blue component */
			bit0 = 0;
			bit1 = (REGION_PROMS[31-i] >> 6) & 0x01;
			bit2 = (REGION_PROMS[31-i] >> 7) & 0x01;
			blue = 0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2;

			palette_set_color(i, red, green, blue);
		}

		int color_prom = 32;

		/* characters */
		for (i = 0;i < TOTAL_COLORS(0);i++)
			COLOR(0,i, 15 - (REGION_PROMS[color_prom++] & 0x0f));

		color_prom += 128;

		/* sprites */
		for (i = 0;i < TOTAL_COLORS(1);i++) {
			if (i % 4 == 0) COLOR(1,i,0);	/* preserve transparency */
			else COLOR(1,i,15 - ((REGION_PROMS[color_prom] & 0x0f)) + 0x10);

			color_prom++;
		}

		color_prom += 128;


		/* now the stars */
		for (i = 32;i < 32 + 64;i++)
		{
			int bits,r,g,b;
			int[] map = { 0x00, 0x88, 0xcc, 0xff };
//
			bits = ((i-32) >> 0) & 0x03;
			r = map[bits];
			bits = ((i-32) >> 2) & 0x03;
			g = map[bits];
			bits = ((i-32) >> 4) & 0x03;
			b = map[bits];
			palette_set_color(i,r,g,b);
		}
	}

}