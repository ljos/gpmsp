package cottage.vidhrdw;

import jef.machine.MachineDriver;
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

public class Blktiger
	extends MAMEVideo
	implements VideoEmulator, Vh_refresh, Vh_start, Vh_stop, Vh_convert_color_proms {

	BitMap charLayer;
	BitMap tmpbitmap2;
	BitMap tmpbitmap3;

	int[] REGION_CPU;
	int[] scroll_ram = new int[4 * 0x1000];

	int[] blktiger_scrolly = new int[2];
	int[] blktiger_scrollx = new int[2];

	boolean chon = true;
	boolean objon = true;
	boolean bgon = true;

	int blktiger_scroll_bank = 0;
	int screen_layout = 0;

	boolean[] dirtybuffer = new boolean[0x400];
	boolean[] dirtybuffer2 = new boolean[4 * 0x1000];

	public WriteHandler paletteram_w(int[] mem, cottage.vidhrdw.Blktiger video) {
		return new Paletteram_w(mem, video);
	}
	public WriteHandler paletteram2_w(int[] mem, cottage.vidhrdw.Blktiger video) {
		return new Paletteram2_w(mem, video);
	}
	public WriteHandler videoram_w(int[] mem, cottage.vidhrdw.Blktiger video) {
		return new Videoram_w(mem, video);
	}
	public WriteHandler blktiger_background_w(int[] mem, cottage.vidhrdw.Blktiger video) {
		return new Blktiger_background_w(mem, video);
	}
	public WriteHandler blktiger_scrolly_w(cottage.vidhrdw.Blktiger video) {
		return new Blktiger_scrolly_w(video);
	}
	public WriteHandler blktiger_scrollx_w(cottage.vidhrdw.Blktiger video) {
		return new Blktiger_scrollx_w(video);
	}
	public WriteHandler blktiger_scrollbank_w(cottage.vidhrdw.Blktiger video) {
		return new Blktiger_scrollbank_w(video);
	}
	public WriteHandler blktiger_screen_layout_w(cottage.vidhrdw.Blktiger video) {
		return new Blktiger_screen_layout_w(video);
	}
	public ReadHandler blktiger_background_r(int[] mem, cottage.vidhrdw.Blktiger video) {
		return new Blktiger_background_r(mem, video);
	}

	public void init(MachineDriver md) {
		super.init(md);
		charLayer = new BitMapImpl(backBuffer.getWidth(), backBuffer.getHeight());
		tmpbitmap2 = new BitMapImpl(8 * 256, 4 * 256);
		tmpbitmap3 = new BitMapImpl(4 * 256, 8 * 256, tmpbitmap2.getPixels());
		gfxMan[0].setTransparencyOverwrite(true);
	}

	public void setRegions(int[] mem) {
		this.REGION_CPU = mem;
	}

	public BitMap video_update() {
		int offs, sx, sy;

		if (bgon) {
			/*
			 * Draw the tiles.
			 * 
			 * This method may look unnecessarily complex. Only tiles that are
			 * likely to be visible are drawn. The rest are kept dirty until
			 * they become visible.
			 * 
			 * The reason for this is that on level 3, the palette changes a
			 * lot if the whole virtual screen is checked and redrawn then the
			 * game will slow down to a crawl.
			 */

			if (screen_layout != 0) {
				// 8x4 screen
				int offsetbase;
				int scrollx, scrolly, y;
				scrollx = ((blktiger_scrollx[0] >> 4) + 16 * blktiger_scrollx[1]);
				scrolly = ((blktiger_scrolly[0] >> 4) + 16 * blktiger_scrolly[1]);

				for (sy = 0; sy < 18; sy++) {
					y = (scrolly + sy) & (16 * 4 - 1);
					offsetbase = ((y & 0xf0) << 8) + 32 * (y & 0x0f);
					for (sx = 0; sx < 18; sx++) {
						int colour, attr, code, x;
						x = (scrollx + sx) & (16 * 8 - 1);
						offs = offsetbase + ((x & 0xf0) << 5) + 2 * (x & 0x0f);
						attr = scroll_ram[offs + 1];
						colour = (attr & 0x78) >> 3;

						if (dirtybuffer2[offs]
							|| dirtybuffer2[offs + 1]
							|| gfxMan[1].colorCodeHasChanged(colour)) {
							code = scroll_ram[offs];
							code += 256 * (attr & 0x07);

							dirtybuffer2[offs] = dirtybuffer2[offs + 1] = false;

							drawgfx(
								tmpbitmap2,
								1,
								code,
								colour,
								(attr & 0x80) != 0,
								false,
								x * 16,
								y * 16,
								GfxManager.TRANSPARENCY_NONE,
								0);
						}
					}
				}

				// copy the background graphics
				scrollx = (blktiger_scrollx[0] + 256 * blktiger_scrollx[1]);
				// & (tmpbitmap2.getWidth() - 257);
				scrolly = (blktiger_scrolly[0] + 256 * blktiger_scrolly[1]);
				// & (tmpbitmap2.getHeight() - 257);
				copyscrollbitmap(
					backBuffer,
					tmpbitmap2,
					1,
					scrollx,
					1,
					scrolly,
					GfxManager.TRANSPARENCY_NONE,
					0);
				//tmpbitmap2.toBitMap(backBuffer,0,0,scrollx,scrolly,256,224);
			} else {
				// 4x8 screen
				int offsetbase;
				int scrollx, scrolly, y;
				scrollx = ((blktiger_scrollx[0] >> 4) + 16 * blktiger_scrollx[1]);
				scrolly = ((blktiger_scrolly[0] >> 4) + 16 * blktiger_scrolly[1]);

				for (sy = 0; sy < 18; sy++) {
					y = (scrolly + sy) & (16 * 8 - 1);
					offsetbase = ((y & 0xf0) << 7) + 32 * (y & 0x0f);
					for (sx = 0; sx < 18; sx++) {
						int colour, attr, code, x;
						x = (scrollx + sx) & (16 * 4 - 1);
						offs = offsetbase + ((x & 0xf0) << 5) + 2 * (x & 0x0f);
						attr = scroll_ram[offs + 1];
						colour = (attr & 0x78) >> 3;

						if (dirtybuffer2[offs]
							|| dirtybuffer2[offs + 1]
							|| gfxMan[1].colorCodeHasChanged(colour)) {

							code = scroll_ram[offs];
							code += 256 * (attr & 0x07);

							dirtybuffer2[offs] = dirtybuffer2[offs + 1] = false;

							drawgfx(
								tmpbitmap3,
								1,
								code,
								colour,
								(attr & 0x80) != 0,
								false,
								x * 16,
								y * 16,
								GfxManager.TRANSPARENCY_NONE,
								0);
						}
					}
				}

				// copy the background graphics
				scrollx = (blktiger_scrollx[0] + 256 * blktiger_scrollx[1]);
				// & (tmpbitmap2.getWidth() - 257);
				scrolly = (blktiger_scrolly[0] + 256 * blktiger_scrolly[1]);
				// & (tmpbitmap2.getHeight() - 257);
				copyscrollbitmap(
					backBuffer,
					tmpbitmap3,
					1,
					scrollx,
					1,
					scrolly,
					GfxManager.TRANSPARENCY_NONE,
					0);
				//tmpbitmap3.toBitMap(backBuffer,0,0,scrollx,scrolly,256,224);
			}
		}

		if (objon) {
			/* Draw the sprites. */
			for (offs = 0x200 - 4; offs >= 0; offs -= 4) {
				/*
				 * SPRITES ===== Attribute 0x80 Code MSB 0x40 Code MSB 0x20
				 * Code MSB 0x10 X MSB 0x08 X flip 0x04 Colour 0x02 Colour 0x01
				 * Colour
				 */

				int code, colour;

				code = REGION_CPU[0xfe00 + offs];
				code += (((int) (REGION_CPU[0xfe00 + offs + 1] & 0xe0)) << 3);
				colour = REGION_CPU[0xfe00 + offs + 1] & 0x07;

				sy = REGION_CPU[0xfe00 + offs + 2];
				sx = REGION_CPU[0xfe00 + offs + 3] - ((REGION_CPU[0xfe00 + offs + 1] & 0x10) << 4);

				drawgfx(
					backBuffer,
					2,
					code,
					colour,
					(REGION_CPU[0xfe00 + offs + 1] & 0x08) != 0,
					false,
					sx,
					sy,
					GfxManager.TRANSPARENCY_PEN,
					15);
			}
		}

		if (chon) {
			/*
			 * draw the frontmost playfield. They are characters, but draw them
			 * as sprites
			 */
			for (offs = 0x3ff; offs >= 0; offs--) {
				if (dirtybuffer[offs]) {
					dirtybuffer[offs] = false;
					sx = offs % 32;
					sy = offs / 32;

					drawgfx(
						charLayer,
						0,
						REGION_CPU[0xd000 + offs] + ((REGION_CPU[0xd400 + offs] & 0xe0) << 3),
						REGION_CPU[0xd400 + offs] & 0x1f,
						false,
						false,
						8 * sx,
						8 * sy,
						GfxManager.TRANSPARENCY_PEN,
						3);
				}
			}
		}

		//charLayer.toPixels(pixels);
		charLayer.toBitMap(backBuffer, 0, 0, 0, 0, 256, 224);

		return bitmap;
	}

	public class Videoram_w implements WriteHandler {
		int mem[];
		cottage.vidhrdw.Blktiger video;

		public Videoram_w(int[] mem, cottage.vidhrdw.Blktiger video) {
			this.mem = mem;
			this.video = video;
		}

		public void write(int address, int value) {
			mem[address] = value;
			video.dirtybuffer[address & 0x3ff] = true;
		}
	}

	public class Blktiger_background_w implements WriteHandler {
		int mem[];
		cottage.vidhrdw.Blktiger video;

		public Blktiger_background_w(int[] mem, cottage.vidhrdw.Blktiger video) {
			this.mem = mem;
			this.video = video;
		}

		public void write(int address, int value) {
			int offs = (address & 0xfff) + video.blktiger_scroll_bank;
			scroll_ram[offs] = value;
			video.dirtybuffer2[offs] = true;
		}
	}

	public class Blktiger_background_r implements ReadHandler {
		int mem[];
		cottage.vidhrdw.Blktiger video;

		public Blktiger_background_r(int[] mem, cottage.vidhrdw.Blktiger video) {
			this.mem = mem;
			this.video = video;
		}

		public int read(int address) {
			int offs = (address & 0xfff) + video.blktiger_scroll_bank;
			return scroll_ram[offs];
		}
	}

	public class Blktiger_scrolly_w implements WriteHandler {
		cottage.vidhrdw.Blktiger video;
		public Blktiger_scrolly_w(cottage.vidhrdw.Blktiger video) {
			this.video = video;
		}

		public void write(int address, int value) {
			video.blktiger_scrolly[address & 1] = value;
		}
	}

	public class Blktiger_scrollx_w implements WriteHandler {
		cottage.vidhrdw.Blktiger video;
		public Blktiger_scrollx_w(cottage.vidhrdw.Blktiger video) {
			this.video = video;
		}

		public void write(int address, int value) {
			video.blktiger_scrollx[address & 1] = value;
		}
	}

	public class Blktiger_scrollbank_w implements WriteHandler {
		cottage.vidhrdw.Blktiger video;
		public Blktiger_scrollbank_w(cottage.vidhrdw.Blktiger video) {
			this.video = video;
		}

		public void write(int address, int value) {
			video.blktiger_scroll_bank = (value & 0x03) * 0x1000;
		}
	}

	public class Blktiger_screen_layout_w implements WriteHandler {
		cottage.vidhrdw.Blktiger video;
		public Blktiger_screen_layout_w(cottage.vidhrdw.Blktiger video) {
			this.video = video;
		}

		public void write(int address, int value) {
			video.screen_layout = value;
		}
	}

	public class Paletteram_w implements WriteHandler {
		int mem[];
		cottage.vidhrdw.Blktiger video;

		public Paletteram_w(int[] mem, cottage.vidhrdw.Blktiger video) {
			this.mem = mem;
			this.video = video;
		}

		public void write(int address, int value) {
			if (mem[address] != value) {
				mem[address] = value;
				int argb;
				int offset = (address - 0xd800);
				int color = (mem[address] << 8) | mem[address + 0x400];

				argb = ((color & 0x000f) << 4) | ((color & 0x0f00) << 4) | ((color & 0xf000) << 8);

				video.gfxMan[0].changePalette(offset, argb);
				video.gfxMan[1].changePalette(offset, argb);
				video.gfxMan[2].changePalette(offset, argb);
			}
		}
	}

	public class Paletteram2_w implements WriteHandler {
		int mem[];
		cottage.vidhrdw.Blktiger video;

		public Paletteram2_w(int[] mem, cottage.vidhrdw.Blktiger video) {
			this.mem = mem;
			this.video = video;
		}

		public void write(int address, int value) {
			if (mem[address] != value) {
				mem[address] = value;
				int argb;
				int offset = (address - 0xdc00);
				int color = (mem[address - 0x400] << 8) | mem[address];

				argb = ((color & 0x000f) << 4) | ((color & 0x0f00) << 4) | ((color & 0xf000) << 8);

				video.gfxMan[0].changePalette(offset, argb);
				video.gfxMan[1].changePalette(offset, argb);
				video.gfxMan[2].changePalette(offset, argb);
			}
		}
	}

	public boolean blktiger() {
		return true;
	}

}