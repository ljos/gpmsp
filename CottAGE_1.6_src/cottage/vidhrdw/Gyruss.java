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

public class Gyruss extends MAMEVideo implements 	VideoEmulator,
													Vh_refresh,
													Vh_start,
													Vh_stop,
													Vh_convert_color_proms{

	int[] REGION_PROMS;
	int[] REGION_CPU;
	int[] REGION_CPU4;

	boolean[] dirtybuffer = new boolean[0x400];

	BitMap charLayer;

	int gyruss_spritebank = 0;

	public void init(MachineDriver md) {
		super.init(md);
		charLayer = new BitMapImpl(backBuffer.getWidth(), backBuffer.getHeight());
	}

	public void setRegions(int[] proms, int[] mem, int[] cpu4) {
		this.REGION_PROMS = proms;
		this.REGION_CPU	  = mem;
		this.REGION_CPU4  = cpu4;
	}

	public int SprTrans(int u) {
		int YTABLE_START = 0xe000;
		int SINTABLE_START = 0xe400;
		int COSTABLE_START = 0xe600;

		int ro;
		int theta2;
		int table;


		ro = REGION_CPU4[YTABLE_START + REGION_CPU[u]];
		theta2 = 2 * REGION_CPU[u+3];

		/* cosine table */
		//table = &memory_region(REGION_CPU4)[COSTABLE_START];
		table = COSTABLE_START;

		//REGION_CPU[u] = (table[theta2+1] * ro) >> 8;
		REGION_CPU[u] = (REGION_CPU4[table+theta2+1] * ro) >> 8;

		if (REGION_CPU[u] >= 0x80)
		{
			REGION_CPU[u+3] = 0;
			return 0;
		}
		if (REGION_CPU4[table + theta2] != 0)	/* negative */
		{
			if (REGION_CPU[u] >= 0x78)	/* avoid wraparound from top to bottom of screen */
			{
				REGION_CPU[u+3] = 0;
				return 0;
			}
			REGION_CPU[u] = (-REGION_CPU[u])&0xff;
		}

		/* sine table */
		//table = &memory_region(REGION_CPU4)[SINTABLE_START];
		table = SINTABLE_START;

		//REGION_CPU[u+3] = (table[theta2+1] * ro) >> 8;
		REGION_CPU[u+3] = (REGION_CPU4[table+theta2+1] * ro) >> 8;

		if (REGION_CPU[u+3] >= 0x80)
		{
			REGION_CPU[u+3] = 0;
			return 0;
		}
		if (REGION_CPU4[table+theta2] != 0)	/* negative */
			REGION_CPU[u+3] = (-REGION_CPU[u+3])&0xff;


		/* convert from logical coordinates to screen coordinates */
		if ( (REGION_CPU[u+2] & 0x10) != 0)
			REGION_CPU[u] += 0x78;
		else
			REGION_CPU[u] += 0x7C;

		REGION_CPU[u+3] += 0x78;


		return 1;	/* queue this sprite */
	}

/* this macro queues 'nq' sprites at address 'u', into queue at 'q' */
//#define SPR(n) ((Sprites*)&sr[4*(n)])

	public void queuereg_w (int data)
	{
		if (data == 1)
		{
			int n;
			int sr;


			/* Gyruss hardware stores alternatively sprites at position
			   0xa000 and 0xa200.  0xA700 tells which one is used.
			*/

			if (gyruss_spritebank == 0)
				sr = 0xa000;
			else sr = 0xa200;


			/* #0-#3 - ship */

			/* #4-#23 */
			if (REGION_CPU[0xa7fc] != 0)	/* planet is on screen */
			{
				//SprTrans(REGION_CPU[sr + 4 * 4] );	/* #4 - polar coordinates - ship */
				SprTrans(sr + 4 * 4 );	/* #4 - polar coordinates - ship */

				REGION_CPU[sr + 4 * 5 + 3] = 0;	/* #5 - unused */

				/* #6-#23 - planet */
			}
			else
			{
				for (n = 4;n < 24;n += 2)	/* 10 double height sprites in polar coordinates - enemies flying */
				{
					//SprTrans(REGION_CPU[sr + 4 * n]);
					SprTrans( sr + 4 * n );

					REGION_CPU[sr + 4 * (n + 1) + 3] = 0;
				}
			}


			/* #24-#59 */
			for (n = 24;n < 60;n++)	/* 36 sprites in polar coordinates - enemies at center of screen */
				SprTrans( sr + 4 * n );


			/* #60-#63 - unused */


			/* #64-#77 */
			if (REGION_CPU[0xa7fd] == 0)
			{
				for (n = 64;n < 78;n++)	/* 14 sprites in polar coordinates - bullets */
					SprTrans( sr + 4 * n );
			}
			/* else 14 sprites - player ship being formed */


			/* #78-#93 - stars */
			for (n = 78;n < 86;n++)
			{
				if (SprTrans( sr + 4 * n ) != 0)
				{
					// make a mirror copy
					REGION_CPU[sr + 4 * (n + 8) + 3] = (REGION_CPU[sr + 4 * n + 3] - 4)&0xff;
					REGION_CPU[sr + 4 * (n + 8) + 0] = (REGION_CPU[sr + 4 * n] + 4)&0xff;
				}
				else
					REGION_CPU[sr + 4 * (n + 8) + 3] = 0;
			}
		}
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

	/*
	   offs+0 :  Ypos
	   offs+1 :  Sprite number
	   offs+2 :  Attribute in the form HF-VF-BK-DH-p3-p2-p1-p0
				 where  HF is horizontal flip
						VF is vertical flip
						BK is for bank select
						DH is for double height (if set sprite is 16*16, else is 16*8)
						px is palette weight
	   offs+3 :  Xpos
	*/

	/* Draw the sprites. Note that it is important to draw them exactly in this */
	/* order, to have the correct priorities. */
		int sr;


		if (gyruss_spritebank == 0)
			sr = 0xa000; // spriteram
		else sr = 0xa200; // spriteram_2


		for (offs = 0x180 - 8;offs >= 0;offs -= 8)
		{
			/*if ((REGION_CPU[sr + 2 + offs] & 0x10) != 0)	// double height
			{
				if (REGION_CPU[sr + offs + 0] != 0)
					drawgfx(backBuffer,3,
							REGION_CPU[sr + offs + 1]/2 + 4*(REGION_CPU[sr + offs + 2] & 0x20),
							REGION_CPU[sr + offs + 2] & 0x0f,
							!((REGION_CPU[sr + offs + 2] & 0x40) != 0), (REGION_CPU[sr + offs + 2] & 0x80) != 0,
							REGION_CPU[sr + offs + 0],240-REGION_CPU[sr + offs + 3]+1,
							TRANSPARENCY_PEN,0);
			}
			else	// single height
			{
				if (REGION_CPU[sr + offs + 0] != 0)
					drawgfx(backBuffer,1 + (REGION_CPU[sr + offs + 1] & 1),
							REGION_CPU[sr + offs + 1]/2 + 4*(REGION_CPU[sr + offs + 2] & 0x20),
							REGION_CPU[sr + offs + 2] & 0x0f,
							!((REGION_CPU[sr + offs + 2] & 0x40) != 0), (REGION_CPU[sr + offs + 2] & 0x80) != 0,
							REGION_CPU[sr + offs + 0],240-REGION_CPU[sr + offs + 3]+1,
							TRANSPARENCY_PEN,0);

				if (REGION_CPU[sr + offs + 4] != 0)
					drawgfx(backBuffer,1 + (REGION_CPU[sr + offs + 5] & 1),
							REGION_CPU[sr + offs + 5]/2 + 4*(REGION_CPU[sr + offs + 6] & 0x20),
							REGION_CPU[sr + offs + 6] & 0x0f,
							!((REGION_CPU[sr + offs + 2] & 0x40) != 0), (REGION_CPU[sr + offs + 2] & 0x80) != 0,
							REGION_CPU[sr + offs + 4],240-REGION_CPU[sr + offs + 7]+1,
							TRANSPARENCY_PEN,0);
			}/**/
			if (REGION_CPU[sr + offs + 0] != 0)
				drawgfx(backBuffer,1 + (REGION_CPU[sr + offs + 1] & 1),
						REGION_CPU[sr + offs + 1]/2 + 4*(REGION_CPU[sr + offs + 2] & 0x20),
						REGION_CPU[sr + offs + 2] & 0x0f,
						!((REGION_CPU[sr + offs + 2] & 0x40) != 0), (REGION_CPU[sr + offs + 2] & 0x80) != 0,
						REGION_CPU[sr + offs + 0],241-REGION_CPU[sr + offs + 3],
						GfxManager.TRANSPARENCY_PEN,0);/**/

		}

		return bitmap;
	}

	public void writeVRAM(int layer, int address, int value) {
		dirtybuffer[address & 0x3ff] = true;
	}


	public boolean gyruss() {
		return true;
	}

	public void palette_init() {
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
		for (i = 0;i < TOTAL_COLORS(1);i++) {
			COLOR(1, i, REGION_PROMS[pointer] & 0x0f);
			COLOR(2, i, REGION_PROMS[pointer] & 0x0f);
			COLOR(3, i, REGION_PROMS[pointer++] & 0x0f);
		}

		/* characters */
		for (i = 0;i < TOTAL_COLORS(0);i++)
			COLOR(0, i, (REGION_PROMS[pointer++] & 0x0f) + 0x10);
	}

	public WriteHandler gyruss_queuereg_w(cottage.vidhrdw.Gyruss v) {
		return new Gyruss_queuereg_w(v);
	}

	public class Gyruss_queuereg_w implements WriteHandler {

		cottage.vidhrdw.Gyruss	video;

		public Gyruss_queuereg_w(cottage.vidhrdw.Gyruss video) {
			this.video	= video;
		}

		public void write(int address, int value) {
			video.queuereg_w(value);
		}
	}

	public WriteHandler gyruss_spritebank_w(cottage.vidhrdw.Gyruss v) {
		return new Gyruss_spritebank_w(v);
	}

	public class Gyruss_spritebank_w implements WriteHandler {

		cottage.vidhrdw.Gyruss	video;

		public Gyruss_spritebank_w(cottage.vidhrdw.Gyruss video) {
			this.video	= video;
		}

		public void write(int address, int value) {
			video.gyruss_spritebank = video.REGION_CPU[address] = value;
		}
	}


	public class Videoram_w implements WriteHandler {
		int			mem[];
		cottage.vidhrdw.Gyruss	video;

		public Videoram_w(int[] mem, cottage.vidhrdw.Gyruss video) {
			this.mem	= mem;
			this.video	= video;
		}

		public void write(int address, int value) {
			mem[address] = value;
			video.writeVRAM(0, address, value);
		}
	}

	public WriteHandler videoram_w(int[] mem, cottage.vidhrdw.Gyruss video) {
		return new Videoram_w(mem, video);
	}
}