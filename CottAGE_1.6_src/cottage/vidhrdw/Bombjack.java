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

public class Bombjack
	extends MAMEVideo
	implements VideoEmulator, Vh_refresh, Vh_start, Vh_stop, Vh_convert_color_proms {

	BitMap charLayer;
	BitMap bgLayer;

	boolean bgChanged = false;

	public int background_image = 0;

	int[] REGION_CPU;
	int[] REGION_GFX4;

	boolean[] dirtybuffer = new boolean[0x400];

	public void init(MachineDriver md) {
		super.init(md);
		charLayer = new BitMapImpl(backBuffer.getWidth(), backBuffer.getHeight());
		bgLayer = new BitMapImpl(backBuffer.getWidth(), backBuffer.getHeight());
		this.REGION_CPU = md.REGIONS[0];
		this.REGION_GFX4 = md.REGIONS[11];
	}

	public void palette_init() {
	}

	public BitMap video_update() {
		int offs, base, sx, sy, flipx, flipy, tile, dindex;

		base = 0x200 * (background_image & 0x07);

		for (int n = 0; n < 0x100; n++) {
			tile = REGION_GFX4[base + n];
			int color = REGION_GFX4[base + n + 0x100] & 15;
			if ((background_image & 0x10) == 0) {
				tile = 255;
				color = 0;
			}
			if (bgChanged || gfxMan[1].colorCodeHasChanged(color)) {
				sx = n % 16;
				sy = n / 16;

				// The following makes sure the correspondent foreground
				// tiles will be updated as well.
				// This is needed because the background is only blitted
				// to the backbuffer when the foreground is drawn.
				dindex = sx * 2 + sy * 2;
				dirtybuffer[dindex] = true;
				dirtybuffer[dindex + 1] = true;
				dindex += 32;
				dirtybuffer[dindex] = true;
				dirtybuffer[dindex + 1] = true;

				flipy = REGION_GFX4[base + n + 0x100] & 0x80;
				drawgfx(
					bgLayer,
					1,
					tile,
					color,
					false,
					flipy != 0,
					16 * sx,
					16 * sy,
					GfxManager.TRANSPARENCY_NONE,
					0);
			}
		}

		/* for every character in the Video RAM, check if it has been modified */
		/* since last time and update it accordingly. */
		for (offs = 0x400 - 1; offs >= 0; offs--) {

			sx = (offs % 32) * 8;
			sy = (offs / 32) * 8;

			if (dirtybuffer[offs]
				|| gfxMan[0].colorCodeHasChanged(REGION_CPU[0x9400 + offs] & 0x0f)
				|| bgChanged) {
				bgLayer.toBitMap(charLayer, 232 - sy, sx, 232 - sy, sx, 8, 8);

				drawgfx(
					charLayer,
					0,
					REGION_CPU[0x9000 + offs] + 16 * (REGION_CPU[0x9400 + offs] & 0x10),
					REGION_CPU[0x9400 + offs] & 0x0f,
					false,
					false,
					sx,
					sy,
					GfxManager.TRANSPARENCY_PEN,
					0);

				dirtybuffer[offs] = false;
			}
		}

		/* copy the character mapped graphics */
		charLayer.toPixels(pixels);

		bgChanged = false;

		/* Draw the sprites. */
		for (offs = 0x60 - 4; offs >= 0; offs -= 4) {

			/*
			 * abbbbbbb cdefgggg hhhhhhhh iiiiiiii
			 * 
			 * a use big sprites (32x32 instead of 16x16) bbbbbbb sprite code c
			 * x flip d y flip (used only in death sequence?) e ? (set when big
			 * sprites are selected) f ? (set only when the bonus (B)
			 * materializes?) gggg color hhhhhhhh x position iiiiiiii y
			 * position
			 */

			sx = REGION_CPU[0x9820 + offs + 3];
			if ((REGION_CPU[0x9820 + offs] & 0x80) != 0)
				sy = 177 - REGION_CPU[0x9820 + offs + 2];
			else
				sy = 225 - REGION_CPU[0x9820 + offs + 2];
			flipx = REGION_CPU[0x9820 + offs + 1] & 0x40;
			flipy = REGION_CPU[0x9820 + offs + 1] & 0x80;

			sy += ((REGION_CPU[0x9820 + offs] & 0x80) != 0) ? 48 : 16;

			drawgfx(
				backBuffer,
				((REGION_CPU[0x9820 + offs] & 0x80) != 0) ? 3 : 2,
				REGION_CPU[0x9820 + offs] & 0x7f,
				REGION_CPU[0x9820 + offs + 1] & 0x0f,
				flipx != 0,
				flipy != 0,
				sx,
				sy,
				GfxManager.TRANSPARENCY_PEN,
				0);
		}

		return bitmap;
	}

	public boolean bombjack() {
		return true;
	}

	public class Paletteram_xxxxBBBBGGGGRRRR_w implements WriteHandler {
		public void write(int address, int value) {
			if (REGION_CPU[address] != value) {
				REGION_CPU[address] = value;
				int argb;
				int offset = ((address & 0xfffe) - 0x9c00) / 2;
				int color =
					(REGION_CPU[address & 0xfffe] << 8) | REGION_CPU[1 + (address & 0xfffe)];

				argb = ((color & 0x000f) << 4) | ((color & 0x0f00) << 12) | ((color & 0xf000) << 0);

				gfxMan[0].changePalette(offset, argb);
				gfxMan[1].changePalette(offset, argb);
				gfxMan[2].changePalette(offset, argb);
				gfxMan[3].changePalette(offset, argb);
			}
		}
	}

	public WriteHandler bombjack_paletteram_xxxxBBBBGGGGRRRR_w() {
		return new Paletteram_xxxxBBBBGGGGRRRR_w();
	}

	public class Videoram_w implements WriteHandler {
		public void write(int address, int data) {
			REGION_CPU[address] = data;
			dirtybuffer[address & 0x3ff] = true;
		}
	}

	public WriteHandler bombjack_videoram_w() {
		return new Videoram_w();
	}

	public class Background_w implements WriteHandler {
		public void write(int address, int data) {
			REGION_CPU[address] = data;
			if (background_image != data) {
				for (int n = 0; n < 0x400; n++) {
					dirtybuffer[n] = true;
				}
				background_image = data;
				bgChanged = true;
			}
		}
	}

	public WriteHandler bombjack_background_w() {
		return new Background_w();
	}
}