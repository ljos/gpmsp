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

public class Pacman extends MAMEVideo implements VideoEmulator,
													Vh_refresh,
													Vh_start,
													Vh_stop,
													Vh_convert_color_proms{

	BitMap charLayer;
	int xoffsethack = 1;

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
	
	public int[] getREGION_CPU() {
		return REGION_CPU;
	}

	public boolean pacman() {
		return true;
	}

	public BitMap video_update() {
		int offs;

		for (offs = 0x400 - 1; offs > 0; offs--)
		{
			if (dirtybuffer[offs])
			{
				int mx,my,sx,sy;

				dirtybuffer[offs] = false;
				mx = offs % 32;
				my = offs / 32;

				if (my < 2)
				{
					if (mx < 2 || mx >= 30) continue; /* not visible */
					sx = my + 34;
					sy = mx - 2;
				}
				else if (my >= 30)
				{
					if (mx < 2 || mx >= 30) continue; /* not visible */
					sx = my - 30;
					sy = mx - 2;
				}
				else
				{
					sx = mx + 2;
					sy = my - 2;
				}

				//if (flipscreen)
				//{
				//	sx = 35 - sx;
				//	sy = 27 - sy;
				//}

				//sy += 2;


				drawgfx(charLayer,0,
						REGION_CPU[0x4000 + offs],
						REGION_CPU[0x4400 + offs] & 0x1f,
						false,false,
						sx*8,sy*8,
						GfxManager.TRANSPARENCY_NONE,0);
			}
		}

		charLayer.toPixels(pixels);

		/* Draw the sprites. Note that it is important to draw them exactly in this */
		/* order, to have the correct priorities. */
		for (offs = 16 - 2;offs > 2*2;offs -= 2)
		{
			int sx,sy;


			sx = 272 - REGION_CPU[0x5060 + offs + 1];
			sy = REGION_CPU[0x5060 + offs] - 31;

			drawgfx(backBuffer,1,
					REGION_CPU[0x4ff0 + offs] >> 2,
					REGION_CPU[0x4ff0 + offs + 1] & 0x1f,
					(REGION_CPU[0x4ff0 + offs] & 1) != 0, (REGION_CPU[0x4ff0 + offs] & 2) != 0,
					sx,sy,
					GfxManager.TRANSPARENCY_COLOR,0);

			/* also plot the sprite with wraparound (tunnel in Crush Roller) */
			//drawgfx(backBuffer,1,
			//		REGION_CPU[0x4ff0 + offs] >> 2,
			//		REGION_CPU[0x4ff0 + offs + 1] & 0x1f,
			//		(REGION_CPU[0x4ff0 + offs] & 1) != 0, (REGION_CPU[0x4ff0 + offs] & 2) != 0,
			//		sx - 256,sy,
			//		TRANSPARENCY_COLOR,0);
		}

		/* In the Pac Man based games (NOT Pengo) the first two sprites must be offset */
		/* one pixel to the left to get a more correct placement */
		for (offs = 2*2;offs >= 0;offs -= 2)
		{
			int sx,sy;

			sx = 272 - REGION_CPU[0x5060 + offs + 1];
			sy = REGION_CPU[0x5060 + offs] - 31;


			drawgfx(backBuffer,1,
					REGION_CPU[0x4ff0 + offs] >> 2,
					REGION_CPU[0x4ff0 + offs + 1] & 0x1f,
					(REGION_CPU[0x4ff0 + offs] & 1) != 0, (REGION_CPU[0x4ff0 + offs] & 2) != 0,
					sx,sy + xoffsethack,
					GfxManager.TRANSPARENCY_COLOR,0);

			/* also plot the sprite with wraparound (tunnel in Crush Roller) */
			//drawgfx(backBuffer,1,
			//		REGION_CPU[0x4ff0 + offs] >> 2,
			//		REGION_CPU[0x4ff0 + offs + 1] & 0x1f,
			//		(REGION_CPU[0x4ff0 + offs] & 1) != 0, (REGION_CPU[0x4ff0 + offs] & 2) != 0,
			//		sx - 256,sy + xoffsethack,
			//		TRANSPARENCY_COLOR,0);
		}
		return bitmap;
	}

	public void palette_init() {
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

			//palette[pointer++] = red<<16 | green<<8 | blue;
			palette_set_color(pointer++, red, green, blue);
		}

		pointer += 0x10;

		/* character lookup table */
		/* sprites use the same color lookup table as characters */
		for (i = 0;i < TOTAL_COLORS(1);i++) {
			COLOR(0,i, REGION_PROMS[pointer  ] & 0x0f);
			COLOR(1,i, REGION_PROMS[pointer++] & 0x0f);
		}
	}


	public class Videoram_w implements WriteHandler {
		int			mem[];
		cottage.vidhrdw.Pacman	video;

		public Videoram_w(int[] mem, cottage.vidhrdw.Pacman video) {
			this.mem	= mem;
			this.video	= video;
		}

		public void write(int address, int value) {
			mem[0x4000 + (address & 0x7ff)] = value;
			video.dirtybuffer[address & 0x3ff] = true;
		}
	}

	public WriteHandler videoram_w(int[] mem, cottage.vidhrdw.Pacman video) {
		return new Videoram_w(mem, video);
	}
}