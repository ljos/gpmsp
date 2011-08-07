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

public class Solomon
	extends MAMEVideo
	implements VideoEmulator, Vh_refresh, Vh_start, Vh_stop, Vh_convert_color_proms {

	BitMap charLayer;

	int fn = 0;

	int[] REGION_CPU;

	boolean[] dirtybuffer = new boolean[0x400];

	public void init(MachineDriver md) {
		super.init(md);
		charLayer = new BitMapImpl(backBuffer.getWidth(), backBuffer.getHeight());
		this.REGION_CPU = md.REGIONS[0];
	}

	public void palette_init() {
	}

	public BitMap video_update() {
		int color, chr, colorBg, chrBg, attr;

		/* for every character in the Video RAM, check if it has been modified */
		/* since last time and update it accordingly. */
		for (int offset = 0x3ff; offset >= 0; offset--) {

			attr = REGION_CPU[0xd800 + offset];
			colorBg = ((attr & 0x70) >> 4);
			color = (REGION_CPU[0xd000 + offset] & 0x70) >> 4;

			if (dirtybuffer[offset]
				|| gfxMan[0].colorCodeHasChanged(color)
				|| gfxMan[1].colorCodeHasChanged(colorBg)) {
				int sx, sy, flipx, flipy;

				dirtybuffer[offset] = false;

				sx = offset % 32;
				sy = offset / 32;
				flipx = (attr & 0x80);
				flipy = (attr & 0x08);

				drawgfx(
					charLayer,
					1,
					(REGION_CPU[0xdc00 + offset] | ((REGION_CPU[0xd800 + offset] & 7) << 8))
						& 0x07ff,
					colorBg,
					flipx != 0,
					flipy != 0,
					8 * sx,
					8 * sy,
					GfxManager.TRANSPARENCY_NONE,
					0);
				drawgfx(
					charLayer,
					0,
					(REGION_CPU[0xd400 + offset] | ((REGION_CPU[0xd000 + offset] & 7) << 8))
						& 0x07ff,
					color,
					false,
					false,
					8 * sx,
					8 * sy,
					GfxManager.TRANSPARENCY_PEN,
					0);
			}
		}

		charLayer.toPixels(pixels);

		/* draw sprites */
		for (int offs = 0xe080 - 4; offs >= 0xe000; offs -= 4) {
			int sx, sy, flipx, flipy;

			sx = REGION_CPU[offs + 3];
			sy = 241 - REGION_CPU[offs + 2];
			flipx = REGION_CPU[offs + 1] & 0x40;
			flipy = REGION_CPU[offs + 1] & 0x80;

			drawgfx(
				backBuffer,
				2,
				REGION_CPU[offs] + 16 * (REGION_CPU[offs + 1] & 0x10),
				(REGION_CPU[offs + 1] & 0x0e) >> 1,
				flipx != 0,
				flipy != 0,
				sx,
				sy,
				GfxManager.TRANSPARENCY_PEN,
				0);
		}

		return bitmap;
	}

	public class Paletteram_w implements WriteHandler {
		public void write(int address, int value) {
			REGION_CPU[address] = value;
			int argb;
			int offset = ((address & 0xfffe) - 0xe400) / 2;
			int color = (REGION_CPU[address & 0xfffe] << 8) | REGION_CPU[1 + (address & 0xfffe)];

			argb = ((color & 0x000f) << 4) | ((color & 0x0f00) << 12) | ((color & 0xf000) << 0);

			gfxMan[0].changePalette(offset, argb);
			gfxMan[1].changePalette(offset, argb);
			gfxMan[2].changePalette(offset, argb);
		}
	}

	public WriteHandler paletteram_w() {
		return new Paletteram_w();
	}

	public class Videoram_w implements WriteHandler {
		public void write(int address, int value) {
			REGION_CPU[address] = value;
			dirtybuffer[address & 0x3ff] = true;
		}
	}

	public WriteHandler videoram_w() {
		return new Videoram_w();
	}
}