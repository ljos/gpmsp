package cottage.vidhrdw;

import jef.map.WriteHandler;
import jef.video.BitMap;
import jef.video.GfxManager;
import jef.video.Vh_convert_color_proms;
import jef.video.VideoEmulator;

import cottage.mame.MAMEVideo;

public class Bw8080 extends MAMEVideo implements VideoEmulator {

	int[]	REGION_PROMS;
	int[]	pal = new int[2];
	int[][] gfx = new int[256][8];

	int[][]	overlay = new int[256 * 256][2];

	int		screen_red_enabled = 0;
	int		color_map_select = 0;
	int		screen_red = 0;


	static final int BLACK			= 0x000000;
	static final int RED			= 0xff2020;
	static final int GREEN 			= 0x20ff20;
	static final int YELLOW			= 0xffff20;
	static final int WHITE			= 0xffffff;
	static final int CYAN			= 0x20ffff;
	static final int PURPLE			= 0xff20ff;

	static final int ORANGE			= 0xff9020;
	static final int YELLOW_GREEN	= 0x90ff20;
	static final int GREEN_CYAN		= 0x20ff90;

	public void setRegions(int[] proms) {
		this.REGION_PROMS = proms;
	}

	public int vh_start() {
		pal[0] = 0x000000;
		pal[1] = 0xffffff;

		for (int i = 0; i < 256; i ++) {
			gfx[i][7] = pal[ i >> 7];
			gfx[i][6] = pal[(i &  64) >> 6];
			gfx[i][5] = pal[(i &  32) >> 5];
			gfx[i][4] = pal[(i &  16) >> 4];
			gfx[i][3] = pal[(i &   8) >> 3];
			gfx[i][2] = pal[(i &   4) >> 2];
			gfx[i][1] = pal[(i &   2) >> 1];
			gfx[i][0] = pal[(i &   1)];
		}

		return 0;
	}

	public void invadpt2_vh_convert_color_prom() {
		int i, r, g, b;
		int ptr = 0;


		for (i = 0;i < total_colors;i++)
		{
			/* this bit arrangment is a little unusual but are confirmed by screen shots */

			r = 0xff * ((i >> 0) & 1);
			g = 0xff * ((i >> 2) & 1);
			b = 0xff * ((i >> 1) & 1);
			palette_set_color(ptr++, r, g, b);
		}
	}

	public void invadpt2_videoram_w(int offset,int data) {
		offset -= 0x2400;
		int i,x,y,ix;
		int col;

		y = offset / 32;
		x = 8 * (offset % 32);

		/* 32 x 32 colormap */
		if (screen_red == 0)
			col = REGION_PROMS[(color_map_select != 0 ? 0x400 : 0 ) + (((y+32)/8)*32) + (x/8)] & 7;
		else
			col = 1;	/* red */

		for (i = 0; i < 8; i++) {
			ix = (x + i) * 224 + y;
			pixels[ix] = (data & 0x01) != 0 ? col : 0;
			data >>= 1;
		}
	}


	public void schaser_videoram_w (int offset,int data) {
	//	int i,x,y,fore_color,back_color;
//
	//	y = offset / 32;
	//	x = 8 * (offset % 32);
//
	//	back_color = 2;	/* blue */
	//	fore_color = mem[0xa400 + (offset & 0x1f1f)] & 0x07;
//
	//	for (i = 0; i < 8; i++)
	//	{
	//		if (data & 0x01)
	//			plot_pixel_p (x, y, fore_color);
	//		else
	//			plot_pixel_p (x, y, back_color);
//
	//		x ++;
	//		data >>= 1;
	//	}
	}

	public void schaser_colorram_w (int offset,int data) {
	//	int i;

		/* redraw region with (possibly) changed color */
	//	for (i = 0; i < 8; i++, offset += 0x20) {
	//		schaser_videoram_w(offset, videoram[offset]);
	//	}
	}

	public BitMap vh_refresh() {
		return bitmap;
	}

	public void writeVRAM(int layer, int address, int value) {
		int i;
		if (rot == GfxManager.ROT0) {

			i = (address - 0x2400) * 8;
			System.arraycopy(gfx[value],0,pixels,i,8);

		} else if (rot == GfxManager.ROT270) {
			int x, y;
			i = 0x1bff - (address - 0x2400);
			x = (i & 0x1f) * 8;
			y = 223 - (i >> 5);

			for (int n = 0; n < 8; n++) {
				i = (x + n) * 224 + y;
				pixels[i] = overlay[i][gfx[value][7 - n] & 1];
			}
		} else {

			int x, y;
			i = (address - 0x2400);
			x = (i & 0x1f) * 8;
			y = 223 - (i >> 5);
			i = x * 224 + y;
			pixels[i] = gfx[value][0];
			pixels[i + 224] = gfx[value][1];
			pixels[i + 448] = gfx[value][2];
			pixels[i + 672] = gfx[value][3];
			pixels[i + 896] = gfx[value][4];
			pixels[i + 1120] = gfx[value][5];
			pixels[i + 1344] = gfx[value][6];
			pixels[i + 1568] = gfx[value][7];
		}
	}

	private void invaders_overlay() {
		artwork_element(   0, 255,   0, 255, WHITE,  0xff);
		artwork_element(  16,  71,   0, 255, GREEN,  0xff);
		artwork_element(   0,  15,  16, 133, GREEN,  0xff);
		artwork_element( 192, 223,   0, 255, RED,    0xff);
	}

	private void invdpt2m_overlay() {
		artwork_element(   0, 255,   0, 255, WHITE,  0xff);
		artwork_element(  16,  71,   0, 255, GREEN,  0xff);
		artwork_element(   0,  15,  16, 133, GREEN,  0xff);
		artwork_element(  72, 191,   0, 255, YELLOW, 0xff);
		artwork_element( 192, 223,   0, 255, RED,    0xff);
	}

	private void invrvnge_overlay() {
		artwork_element(   0, 255,   0, 255, WHITE,  0xff);
		artwork_element(   0,  71,   0, 255, GREEN,  0xff);
		artwork_element( 192, 223,   0, 255, RED,    0xff);
	}

	private void invad2ct_overlay() {
		artwork_element(   0,  24,   0, 255, YELLOW,       0xff);
		artwork_element(  25,  47,   0, 255, YELLOW_GREEN, 0xff);
		artwork_element(  48,  70,   0, 255, GREEN_CYAN,   0xff);
		artwork_element(  71, 116,   0, 255, CYAN,         0xff);
		artwork_element( 117, 139,   0, 255, GREEN_CYAN,   0xff);
		artwork_element( 140, 162,   0, 255, GREEN,        0xff);
		artwork_element( 163, 185,   0, 255, YELLOW_GREEN, 0xff);
		artwork_element( 186, 208,   0, 255, YELLOW,       0xff);
		artwork_element( 209, 231,   0, 255, ORANGE,       0xff);
		artwork_element( 232, 255,   0, 255, RED,          0xff);
	}

	public boolean bw8080() {
		artwork_element(   0, 255,   0, 255, WHITE,  0xff);
		color_map_select = 0;
		return true;
	}

	public boolean invaders() {
		bw8080();
		invaders_overlay();
		return true;
	}

	public boolean invadpt2() {
		bw8080();
		screen_red_enabled = 1;
		return true;
	}

	public boolean invdpt2m() {
		bw8080();
		invdpt2m_overlay();
		return true;
	}

	public boolean invrvnge() {
		bw8080();
		invrvnge_overlay();
		return true;
	}

	public boolean invad2ct() {
		bw8080();
		invad2ct_overlay();
		return true;
	}

	private void artwork_element(int y, int h, int x, int w, int c, int dontknowMaybeAlpha) {
		int col = ((c & 255) << 16) | c & 0xff00 | ((c & 0xff0000) >> 16);
		if (w > 223) w = 223;
		for (int iy = y; iy <= h; iy++) {
			for (int ix = x; ix <= w; ix++) {
				overlay[(255 - iy) * 224 + ix][1] = col;
				if (col != WHITE) overlay[(255 - iy) * 224 + ix][0] = col & 0x0f0f0f;
			}
		}
	}

//
	public class Invadpt2_videoram_w implements WriteHandler {
		int			mem[];
		cottage.vidhrdw.Bw8080	video;

		public Invadpt2_videoram_w(int[] mem, cottage.vidhrdw.Bw8080 video) {
			this.mem	= mem;
			this.video	= video;
		}

		public void write(int address, int value) {
			mem[address] = value;
			video.invadpt2_videoram_w(address, value);
		}
	}

	public WriteHandler invadpt2_videoram_w(int[] mem, cottage.vidhrdw.Bw8080 video) {
		return new Invadpt2_videoram_w(mem, video);
	}

//
	public class Invaders_videoram_w implements WriteHandler {
		int			mem[];
		cottage.mame.MAMEVideo video;

		public Invaders_videoram_w(int[] mem, cottage.mame.MAMEVideo video) {
			this.mem	= mem;
			this.video	= video;
		}

		public void write(int address, int value) {
			mem[address] = value;
			((cottage.vidhrdw.Bw8080)video).writeVRAM(0, address, value);
		}
	}

	public WriteHandler c8080bw_videoram_w(int[] mem, cottage.mame.MAMEVideo video) {
		return new Invaders_videoram_w(mem, video);
	}

//
	public class Schaser_colorram_w implements WriteHandler {
		int			mem[];
		cottage.vidhrdw.Bw8080	video;

		public Schaser_colorram_w(int[] mem, cottage.vidhrdw.Bw8080 video) {
			this.mem	= mem;
			this.video	= video;
		}

		public void write(int address, int value) {
			int offset = address - 0xa400;
			offset &= 0x1f1f;

			mem[0xa400 + offset] = value;

			video.schaser_colorram_w(address, value);
		}
	}

	public WriteHandler schaser_colorram_w(int[] mem, cottage.vidhrdw.Bw8080 video) {
		return new Schaser_colorram_w(mem, video);
	}

//
	public class Invadpt2_vh_convert_color_prom implements Vh_convert_color_proms {
		cottage.vidhrdw.Bw8080	video;

		public Invadpt2_vh_convert_color_prom(cottage.vidhrdw.Bw8080 video) {
			this.video	= video;
		}

		public void palette_init() {
			video.invadpt2_vh_convert_color_prom();
		}
	}

	public Vh_convert_color_proms invadpt2_vh_convert_color_prom(cottage.vidhrdw.Bw8080 video) {
		return new Invadpt2_vh_convert_color_prom(video);
	}
}