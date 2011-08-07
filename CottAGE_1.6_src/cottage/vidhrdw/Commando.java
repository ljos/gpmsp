/*
 * Created on 24-jun-2005
 * 
 * To change the template for this generated file go to Window - Preferences -
 * Java - Code Generation - Code and Comments
 */
package cottage.vidhrdw;

import cottage.mame.MAMEVideo;
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

/**
 * @author Erik Duijs
 * 
 * To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Generation - Code and Comments
 */
public class Commando
	extends MAMEVideo
	implements VideoEmulator, Vh_refresh, Vh_start, Vh_stop, Vh_convert_color_proms {

	boolean[] dirtybuffer = new boolean[0x400];
	boolean[] dirtybuffer2 = new boolean[4 * 0x1000];
	private int[] REGION_CPU, REGION_PROMS;

	int commando_bgvideoram_size = 0x400;
	int videoram_size = 0x400;
	int spriteram_size = 0x180;
	BitMap bitmap;
	BitMap tmpbitmap2;
	BitMap charLayer;

	public void init(MachineDriver md) {
		super.init(md);
		charLayer = new BitMapImpl(backBuffer.getWidth(), backBuffer.getHeight());
		bitmap = new BitMapImpl(backBuffer.getWidth(), backBuffer.getHeight());
		tmpbitmap2 = new BitMapImpl(backBuffer.getWidth() * 2, backBuffer.getHeight() * 2);
		gfxMan[0].setTransparencyOverwrite(true);
	}

	/***************************************************************************
	 * 
	 * Draw the game screen in the given osd_bitmap. Do NOT call
	 * osd_update_display() from this function, it will be called by the main
	 * emulation engine.
	 *  
	 **************************************************************************/
	public BitMap video_update() {
		int offs;

		for (offs = commando_bgvideoram_size - 1; offs >= 0; offs--) {
			if (dirtybuffer[offs]) {
				int sx, sy;
				boolean flipx, flipy;

				dirtybuffer[offs] = false;

				sx = offs / 32;
				sy = offs % 32;
				flipx = (REGION_CPU[offs + 0xdc00] & 0x10) != 0;
				flipy = (REGION_CPU[offs + 0xdc00] & 0x20) != 0;
				/*
				 * if (flipscreen) {
				 */
				//sx = 31 - sx;
				//sy = 31 - sy;
				//flipx = !flipx;
				//flipy = !flipy;
				// */

				drawgfx(
					tmpbitmap2,
					1,
					REGION_CPU[offs + 0xd800] + 4 * (REGION_CPU[offs + 0xdc00] & 0xc0),
					REGION_CPU[offs + 0xdc00] & 0x0f,
					flipx,
					flipy,
					16 * sx,
					16 * sy,
					GfxManager.TRANSPARENCY_NONE,
					0);
			}
		}

		/* copy the background graphics */
		int scrollY = REGION_CPU[0xc808] + (256 * (REGION_CPU[0xc809] & 15));
		int scrollX = REGION_CPU[0xc80a] + (256 * (REGION_CPU[0xc80b] & 15));

		if (scrollY <= 256) {
			tmpbitmap2.toBitMap(bitmap, 0, 0, scrollX, 256 - scrollY, 224, 256);
		} else {
			tmpbitmap2.toBitMap(bitmap, 0, 0, scrollX, 768 - scrollY, 224, scrollY - 256);
			tmpbitmap2.toBitMap(bitmap, 0, scrollY - 256, scrollX, 0, 224, 256 - (scrollY - 256));
		}

		/*
		 * Draw the sprites. Note that it is important to draw them exactly in
		 * this
		 */
		/* order, to have the correct priorities. */
		for (offs = spriteram_size - 4; offs >= 0; offs -= 4) {
			int sx, sy, bank;
			boolean flipx, flipy;

			/* bit 1 of [offs+1] is not used */

			sx = REGION_CPU[offs + 3 + 0xfe00] - 0x100 * (REGION_CPU[offs + 1 + 0xfe00] & 0x01);
			sy = REGION_CPU[offs + 2 + 0xfe00];
			flipx = (REGION_CPU[offs + 1 + 0xfe00] & 0x04) != 0;
			flipy = (REGION_CPU[offs + 1 + 0xfe00] & 0x08) != 0;
			bank = (REGION_CPU[offs + 1 + 0xfe00] & 0xc0) >> 6;

			//if (flipscreen) {
			//	sx = 240 - sx;
			//	sy = 240 - sy;
			//	flipx = !flipx;
			//	flipy = !flipy;
			//}*/

			if (bank < 3)
				drawgfx(
					bitmap,
					2,
					REGION_CPU[offs + 0xfe00] + 256 * bank,
					((REGION_CPU[offs + 1 + 0xfe00] & 0x30) >> 4),
					flipx,
					flipy,
					sx,
					sy,
					GfxManager.TRANSPARENCY_PEN,
					15);
		}

		/*
		 * draw the frontmost playfield. They are characters, but draw them as
		 * sprites
		 */
		for (offs = videoram_size - 1; offs >= 0; offs--) {
			int sx, sy;
			boolean flipx, flipy;

			sx = offs % 32;
			sy = offs / 32;
			flipx = (REGION_CPU[offs + 0xd400] & 0x10) != 0;
			flipy = (REGION_CPU[offs + 0xd400] & 0x20) != 0;

			//if (flipscreen) {
			//	sx = 31 - sx;
			//	sy = 31 - sy;
			//	flipx = !flipx;
			//	flipy = !flipy;
			//}

			drawgfx(
				charLayer,
				0,
				REGION_CPU[offs + 0xd000] + 4 * (REGION_CPU[offs + 0xd400] & 0xc0),
				REGION_CPU[offs + 0xd400] & 0x0f,
				flipx,
				flipy,
				8 * sx,
				8 * sy,
				GfxManager.TRANSPARENCY_PEN,
				3);
		}
		charLayer.toBitMap(bitmap, 0, 0, 0, 0, 256, 224);
		/*
		 * for (int i = 0; i < bitmap.getHeight(); i++) { for (int ii = 0; ii
		 * < bitmap.getWidth(); ii++) { if (bitmap.getPixel(i,ii)> 0) {
		 * System.out.println(bitmap.getPixel(i,ii)); } }
		 */
		return bitmap;
	}
	public void palette_init() {
		System.out.println("palette_init" + REGION_PROMS.length);
		int i;

		for (i = 0; i < 256; i++) {
			int bit0, bit1, bit2, bit3, r, g, b;

			/* red component */
			bit0 = (REGION_PROMS[i] >> 0) & 0x01;
			bit1 = (REGION_PROMS[i] >> 1) & 0x01;
			bit2 = (REGION_PROMS[i] >> 2) & 0x01;
			bit3 = (REGION_PROMS[i] >> 3) & 0x01;
			r = 0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3;
			/* green component */
			bit0 = (REGION_PROMS[i + 256] >> 0) & 0x01;
			bit1 = (REGION_PROMS[i + 256] >> 1) & 0x01;
			bit2 = (REGION_PROMS[i + 256] >> 2) & 0x01;
			bit3 = (REGION_PROMS[i + 256] >> 3) & 0x01;
			g = 0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3;
			/* blue component */
			bit0 = (REGION_PROMS[i + 2 * 256] >> 0) & 0x01;
			bit1 = (REGION_PROMS[i + 2 * 256] >> 1) & 0x01;
			bit2 = (REGION_PROMS[i + 2 * 256] >> 2) & 0x01;
			bit3 = (REGION_PROMS[i + 2 * 256] >> 3) & 0x01;
			b = 0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3;

			palette_set_color(i, r, g, b);
		}
	}

	public WriteHandler videoram_w(int[] region_cpu1, Commando v) {
		return new Videoram_w(region_cpu1, v);
	}
	public WriteHandler videoram_w2(int[] region_cpu1, Commando v) {
		return new Videoram_w2(region_cpu1, v);
	}
	public class Videoram_w implements WriteHandler {
		int mem[];
		cottage.vidhrdw.Commando video;

		public Videoram_w(int[] mem, cottage.vidhrdw.Commando video) {
			this.mem = mem;
			this.video = video;
		}

		public void write(int address, int value) {
			//if (value != 0) System.out.println(value);
			mem[address] = value;
			video.dirtybuffer[address & 0x3ff] = true;
		}
	}
	public class Videoram_w2 implements WriteHandler {
		int mem[];
		cottage.vidhrdw.Commando video;

		public Videoram_w2(int[] mem, cottage.vidhrdw.Commando video) {
			this.mem = mem;
			this.video = video;
		}

		public void write(int address, int value) {
			//if (value != 0) System.out.println(value);
			mem[address] = value;
			video.dirtybuffer2[address & 0x3ff] = true;
		}
	}
	/**
	 * @return
	 */
	public boolean commando() {
		return true;
	}

	public void setRegions(int[] mem, int[] proms) {
		this.REGION_CPU = mem;
		this.REGION_PROMS = proms;
	}
}
