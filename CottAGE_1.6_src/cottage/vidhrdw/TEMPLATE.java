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

public class TEMPLATE extends MAMEVideo implements VideoEmulator,Vh_refresh,Vh_start,Vh_stop,Vh_convert_color_proms {

	BitMap charLayer;

	int[] REGION_PROMS;
	int[] REGION_CPU;

	boolean[] dirtybuffer = new boolean[0x400];

	public void init(MachineDriver md) {
		super.init(md);
		charLayer = new BitMapImpl(backBuffer.getWidth(), backBuffer.getHeight());
	}

	public void setRegions(int[] proms, int[] mem) {
		this.REGION_PROMS = proms;
		this.REGION_CPU	  = mem;
	}


	public BitMap video_update() {
		int offs;

		/* for every character in the Video RAM, check if it has been modified */
		/* since last time and update it accordingly. */
		for (offs = 0x3ff;offs >= 0;offs--) {
			if (dirtybuffer[offs]) {
				int sx,sy,flipx,flipy;


				dirtybuffer[offs] = false;

				sx = offs % 32;
				sy = offs / 32;
				flipx = REGION_CPU[0x8000 + offs] & 0x40;
				flipy = REGION_CPU[0x8000 + offs] & 0x80;

				drawgfx(charLayer, 0,
						REGION_CPU[0x8400 + offs] + 8 * (REGION_CPU[0x8000 + offs] & 0x20),
						REGION_CPU[0x8000 + offs] & 0x0f,
						flipx != 0,flipy != 0,
						8*sx,8*sy,
						GfxManager.TRANSPARENCY_NONE, 0);
			}
		}

		charLayer.toPixels(pixels);


		return bitmap;
	}


	public boolean template() {
		return true;
	}

	public void vh_convert_color_proms() {
		System.out.println("Converting color proms...");
		int i;
		int pointer = 0;

		for (i = 0;i < total_colors;i++) {
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
			bit0 = 0;
			bit1 = (REGION_PROMS[pointer] >> 6) & 0x01;
			bit2 = (REGION_PROMS[pointer] >> 7) & 0x01;
			blue = 0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2;

			palette_set_color(pointer++, red, green, blue);
		}


		/* sprites */
		for (i = 0;i < TOTAL_COLORS(1);i++)
			COLOR(1, i, REGION_PROMS[pointer++] & 0x0f);

		/* characters */
		for (i = 0;i < TOTAL_COLORS(0);i++)
			COLOR(0, i, (REGION_PROMS[pointer++] & 0x0f) + 0x10);
	}


	public class Videoram_w implements WriteHandler {
		int			mem[];
		cottage.vidhrdw.TEMPLATE	video;

		public Videoram_w(int[] mem, cottage.vidhrdw.TEMPLATE video) {
			this.mem	= mem;
			this.video	= video;
		}

		public void write(int address, int value) {
			mem[address] = value;
			//video.writeVRAM(0, address, value);
		}
	}

	public WriteHandler videoram_w(int[] mem, cottage.vidhrdw.TEMPLATE video) {
		return new Videoram_w(mem, video);
	}
}