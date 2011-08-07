package cottage.vidhrdw;

import jef.machine.MachineDriver;
import jef.map.InterruptHandler;
import jef.map.ReadHandler;
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

public class Galaxian extends MAMEVideo implements  VideoEmulator,
													Vh_refresh,
													Vh_start,
													Vh_stop,
													Vh_convert_color_proms {

public WriteHandler videoram_w() 				{ return new Videoram_w(); }
public WriteHandler galaxian_attributes_w() 	{ return new Galaxian_attributes_w(); }
public WriteHandler galaxian_stars_w() 			{ return new Galaxian_stars_w(); }
public WriteHandler interrupt_enable_w() 		{ return new Interrupt_enable_w(); }
public WriteHandler pisces_gfxbank_w() 			{ return new Pisces_gfxbank_w(); }
public ReadHandler  videoram_r()  				{ return new Videoram_r(); }
public InterruptHandler galaxian_vh_interrupt() { return new Galaxian_vh_interrupt(); }
public InterruptHandler scramble_vh_interrupt() { return new Scramble_vh_interrupt(); }

BitMap charLayer;

int[] REGION_PROMS;
int[] REGION_CPU;

boolean[] dirtybuffer = new boolean[0x400];

boolean irqEnabled = false;
int color_mask;

int base_video;
int base_attributes;
int base_bullets;
int base_sprites;

int pisces_gfxbank = 0;

/* star circuit */
boolean	stars_on = false;
int		stars_scroll = 0;
int		stars_type = 0;
int		total_stars = 0;
int		generator = 0;
int		blink_count = 0;
int		stars_blink = 0;
int[] star_x	= new int[MAX_STARS];
int[] star_y	= new int[MAX_STARS];
int[] star_code	= new int[MAX_STARS];
static final int MAX_STARS = 250;
static final int map[] = { 0x00, 0x88, 0xcc, 0xff };

Draw draw_bullets;
boolean do_draw_bullets = true;

	/* bullet drawing functions */
	interface Draw {
		public void draw(int offs, int x, int y);
	}

	/* char/sprite modifier functions */
	interface Modifier {
		public int modify(int c);
	}

	class galaxian_draw_bullets implements Draw {
		public void draw(int offs, int x, int y) {
			int i;

			for (i = 0; i < 4; i++) {
				x--;

				if (x >= 0 && x <= 255) {
					int color;

					/* yellow missile, white shells (this is the terminology on the schematics) */
					color = ((offs == 7*4) ? 0x00ffff00 : 0x00ffffff);

					backBuffer.setPixel(239 - y, x, color);
				}
			}
		}
	}

	class scramble_draw_bullets implements Draw {
		public void draw(int offs, int x, int y) {
			x = x - 6;

			if (x >= 0 && x <= 255) {
				/* yellow bullets */
				backBuffer.setPixel(239 - y, x, 0x00ffff00);
			}
		}
	}

	public void init(MachineDriver md) {
		super.init(md);
		charLayer = new BitMapImpl(backBuffer.getWidth(), backBuffer.getHeight());
	}

	public void setRegions(int[] proms, int[] mem) {
		this.REGION_PROMS = proms;
		this.REGION_CPU	  = mem;
	}

	public boolean galaxian() {
		stars_type = 0;
		base_video = 0x5000;
		base_attributes = 0x5800;
		base_bullets = 0x5860;
		base_sprites = 0x5840;
		draw_bullets = (Draw) new galaxian_draw_bullets();
		return true;
	}

	public boolean scramble() {
		stars_type = 1;
		base_video = 0x4800;
		base_attributes = 0x5000;
		base_bullets = 0x5060;
		base_sprites = 0x5040;
		draw_bullets = (Draw) new scramble_draw_bullets();
		return true;
	}


	public BitMap video_update() {
		int x,y;
		int offs;
		int transparency;

		/* draw the characters */
		for (x = 0; x < 32; x++) {
			int sx,scroll;
			int color;


			scroll = REGION_CPU[base_attributes + (x << 1)];
			color  = REGION_CPU[base_attributes + (x << 1) | 1] & color_mask;

			//if (modify_color)
			//{
			//	modify_color(&color);
			//}

			//if (modify_ypos)
			//{
			//	modify_ypos(&scroll);
			//}


			sx = 8 * x;

			for (y = 0; y < 32; y++) {
				int sy;
				int code;


				sy = ((8 * y) - scroll) & 0xff;

				code = REGION_CPU[base_video + (y << 5) | x];

				//if (modify_charcode)
				//{
				//	modify_charcode(&code, x);
				//}

				drawgfx(backBuffer,0,
						code,color,
						false,false,
						sx,sy,
						GfxManager.TRANSPARENCY_NONE, 0);
			}
		}

		/* draw the bullets */
		if (do_draw_bullets) {
			for (offs = 0;offs < 0x20; offs += 4) {
				int sx,sy;

				sy = 255 - REGION_CPU[base_bullets + offs + 1];
				sx = 255 - REGION_CPU[base_bullets + offs + 3];

				//if (sy < Machine->visible_area.min_y ||
				//	sy > Machine->visible_area.max_y)
				//	continue;

				//if (flip_screen_y)  sy = 255 - sy;

				draw_bullets.draw(offs, sx, sy);
			}
		}

		/* draw the sprites */
		for (offs = 0x20 - 4;offs >= 0;offs -= 4) {
			int sx,sy;
			int flipx,flipy,code,color;


			sx = REGION_CPU[base_sprites + offs + 3] + 1;	/* the existence of +1 is supported by a LOT of games */
			sy = REGION_CPU[base_sprites + offs];			/* Anteater, Mariner, for example */
			flipx = REGION_CPU[base_sprites + offs + 1] & 0x40;
			flipy = REGION_CPU[base_sprites + offs + 1] & 0x80;
			code = REGION_CPU[base_sprites + offs + 1] & 0x3f;
			color = REGION_CPU[base_sprites + offs + 2] & color_mask;

			//if (modify_spritecode)
			//{
			//	modify_spritecode(&code, &flipx, &flipy, offs);
			//}

			//if (modify_color)
			//{
			//	modify_color(&color);
			//}

			//if (modify_ypos)
			//{
			//	modify_ypos(&sy);
			//}

			sy = 240 - sy;


			/* In at least Amidar Turtles, sprites #0, #1 and #2 need to be moved */
			/* down (left) one pixel to be positioned correctly. */
			/* Note that the adjustment must be done AFTER handling flipscreen, thus */
			/* proving that this is a hardware related "feature" */

			if (offs < 3*4)  sy++;


			drawgfx(backBuffer,1,
					code,color,
					flipx!=0,flipy!=0,
					sx,sy,
					GfxManager.TRANSPARENCY_PEN,0);
		}

		drawStars();

		return bitmap;
	}

	public void palette_init() {
		int i;

		color_mask = (super.color_granularity(0) == 4) ? 7 : 3;
		int pointer = 0;

		/* first, the character/sprite palette */
		for (i = 0;i < 32;i++) {
			int bit0,bit1,bit2,red,green,blue;

			/* red component */
			bit0 = (REGION_PROMS[pointer] >> 0) & 0x01;
			bit1 = (REGION_PROMS[pointer] >> 1) & 0x01;
			bit2 = (REGION_PROMS[pointer] >> 2) & 0x01;
			red = 0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2;
			/* green component */
			bit0 = (REGION_PROMS[pointer] >> 3) & 0x01;
			bit1 = (REGION_PROMS[pointer] >> 4) & 0x01;
			bit2 = (REGION_PROMS[pointer] >> 5) & 0x01;
			green = 0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2;
			/* blue component */
			bit0 = (REGION_PROMS[pointer] >> 6) & 0x01;
			bit1 = (REGION_PROMS[pointer] >> 7) & 0x01;
			blue = 0x4f * bit0 + 0xa8 * bit1;

			palette_set_color(pointer++, red, green, blue);
		}

		/* characters and sprites use the same palette */
		for (i = 0;i < super.TOTAL_COLORS(0);i++) {
			/* 00 is always mapped to pen 0 */
			if ((i & super.color_granularity(0) - 1) == 0)  COLOR(0,i,0);
		}

		/* now the stars */
		for (i = 0;i < 64;i++) {
			int bits;

			bits = (i >> 0) & 0x03;
			palette_set_color(pointer, map[bits] << 16);
			//super.palette[pointer] = map[bits] << 16;;
			bits = (i >> 2) & 0x03;
			palette_set_color(pointer, map[bits] << 8);
			//super.palette[pointer] = map[bits] << 8;;
			bits = (i >> 4) & 0x03;
			palette_set_color(pointer, map[bits]);
			//super.palette[pointer++] = map[bits];
		}
		/* precalculate the star background */

		total_stars = 0;
		generator = 0;

		for (int y = 255;y >= 0;y--)
		{
			for (int x = 511;x >= 0;x--)
			{
				int bit1,bit2;

				generator <<= 1;
				bit1 = (~generator >> 17) & 1;
				bit2 = (generator >> 5) & 1;

				if ( (bit1 ^ bit2) != 0) generator |= 1;

				if ( (((~generator >> 16) & 1) != 0) && (generator & 0xff) == 0xff)
				{
					int color;

					color = (~(generator >> 8)) & 0x3f;
					if ( (color != 0) && total_stars < MAX_STARS)
					{
						star_x[total_stars] = x;
						star_y[total_stars] = y;
						star_code[total_stars] = color;

						total_stars++;
					}
				}
			}
		}
	}

	private void plot_star(int y, int x, int code) {
		if ((backBuffer.getPixel(x,y) & 0xffffff) == 0) {
			backBuffer.setPixel(x,y,palette_get_color(code));
		}
	}

	public void drawStars() {
		if (stars_on) {
			switch (stars_type) {
			case -1: /* no stars */
				break;

			case 0:	/* Galaxian stars */
			case 3:	/* Mariner stars */
				for (int offs = 0;offs < total_stars;offs++)
				{
					int x,y;


					x = ((star_x[offs] + stars_scroll) % 512) / 2;
					y = (star_y[offs] + (stars_scroll + star_x[offs]) / 512) % 256;

					if (y >= 16 && y <= 239)
					{
						/* No stars below row (column) 64, between rows 176 and 215 or
						   between 224 and 247 */
						if ((stars_type == 3) &&
							((x < 64) ||
							((x >= 176) && (x < 216)) ||
							((x >= 224) && (x < 248)))) continue;

						if ( ((y & 1) ^ ((x >> 4) & 1)) != 0)
						{
							plot_star(x, y, star_code[offs]);
						}
					}
				}
				break;

			case 1:	/* Scramble stars */
			case 2:	/* Rescue stars */
				for (int offs = 0;offs < total_stars;offs++)
				{
					int x,y;


					x = star_x[offs] / 2;
					y = star_y[offs];

					if (y >= 16 && y <= 239)
					{
						if ((stars_type != 2 || x < 128) &&	// draw only half screen in Rescue
						    ((y & 1) ^ ((x >> 4) & 1)) != 0)
						{
							// Determine when to skip plotting
							//switch (stars_blink)
							switch (stars_blink)
							{
							case 0:
								if ((star_code[offs] & 1) == 0)  continue;
								break;
							case 1:
								if ((star_code[offs] & 4) == 0)  continue;
								break;
							case 2:
								if ((star_x[offs] & 4) == 0)  continue;
								break;
							case 3:
								// Always plot
								break;
							}
							plot_star(x, y, star_code[offs]);
						}
					}
				}
				break;
			}
		}
	}

	public class Videoram_w implements WriteHandler {
		public void write(int address, int value) {
			REGION_CPU[address] = value;
			dirtybuffer[address & 0x3ff] = true;
		}
	}

	public class Galaxian_attributes_w implements WriteHandler {
		public void write(int address,int data) {
			int offset = address - base_attributes;
			if ((address & 1) != 0 && REGION_CPU[address] != data) {
				for (int i = offset / 2; i < 0x400; i += 32) dirtybuffer[i] = true;
			}
			REGION_CPU[address] = data;
		}
	}

	public class Galaxian_stars_w implements WriteHandler {
		public void write(int address,int data) {
			stars_on = (data != 0);
			stars_scroll = 0;
		}
	}

	public class Interrupt_enable_w implements WriteHandler {
		public void write(int address, int value) {
			irqEnabled = (value != 0);
		}
	}

	public class Pisces_gfxbank_w implements WriteHandler {
		public void write(int address, int value) {
			pisces_gfxbank = value & 1;
		}
	}

	public class Videoram_r implements ReadHandler {
		public int read(int address) {
			return REGION_CPU[address - 0x400];
		}
	}

	public class Galaxian_vh_interrupt implements InterruptHandler {
		public int irq() {
			stars_scroll++;
			return irqEnabled? 1 : -1;
		}
	}

	public class Scramble_vh_interrupt implements InterruptHandler {
		public int irq() {
			blink_count++;
			if (blink_count >= 45) {
				blink_count = 0;
				stars_blink = (stars_blink + 1) & 3;
			}
			return irqEnabled? 1 : -1;
		}
	}
}