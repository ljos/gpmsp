/*
 * Created on Aug 29, 2004
 * 
 * To change the template for this generated file go to Window - Preferences -
 * Java - Code Generation - Code and Comments
 */
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

/**
 * @author Erik Duijs
 * 
 * To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Generation - Code and Comments
 */
public class Foodf
	extends MAMEVideo
	implements VideoEmulator, Vh_refresh, Vh_start, Vh_stop, Vh_convert_color_proms {

	int[] REGION_CPU;
	boolean[] dirtybuffer = new boolean[0x400];
	private BitMapImpl charLayer;

	private int READ_WORD(int a) {
		a &= 0xfffffe;
		return (REGION_CPU[a++] << 8) | REGION_CPU[a];
	}
	private void WRITE_WORD(int a, int value) {
		a &= 0xfffffe;
		REGION_CPU[a++] = value >> 8;
		REGION_CPU[a] = value & 0xff;
	}
	private int COMBINE_WORD(int addr, int val2) {
		if ((addr & 1) != 0) {
			return (REGION_CPU[addr & 0xfffffe] << 8) | val2;
		} else {
			return (val2 << 8) | (REGION_CPU[addr + 1]);
		}
	}

	/**
	 * @return
	 */
	public boolean foodf() {
		return true;
	}
	public void init(MachineDriver md) {
		super.init(md);
		charLayer = new BitMapImpl(backBuffer.getWidth(), backBuffer.getHeight());
	}
	/**
	 * @param region_cpu1
	 * @param region_cpu12
	 */
	public void setRegions(int[] region_cpu) {
		REGION_CPU = region_cpu;
	}

	public class Paletteram_w implements WriteHandler {

		public void write(int address, int value) {
			foodf_paletteram_w(address, value);
		}
	}

	public WriteHandler paletteram_w() {
		return new Paletteram_w();
	}
	public void palette_init() {
		System.out.println("Init palette");
		int i;
		//		#define TOTAL_COLORS(gfxn) (Machine->gfx[gfxn]->total_colors *
		// Machine->gfx[gfxn]->color_granularity)
		//		#define COLOR(gfxn,offs)
		// (colortable[Machine->drv->gfxdecodeinfo[gfxn].color_codes_start +
		// offs])

		for (i = 0; i < 256; i++) {
			int r = ((i & 1) >> 0) * 0xff;
			int g = ((i & 2) >> 1) * 0xff;
			int b = ((i & 4) >> 2) * 0xff;
			palette_set_color(i, r, g, b);
		}

		/* characters and sprites use the same palette */
		for (i = 0; i < TOTAL_COLORS(0); i++)
			COLOR(0, i, i);
	}

	/*
	 * palette RAM read/write handlers
	 */
	void foodf_paletteram_w(int addr, int data) {
		//int oldword = READ_WORD(addr);
		//int newword = COMBINE_WORD(addr, data);
		//System.out.println("paletteram_w(" + Integer.toHexString(addr) + "," + Integer.toHexString(oldword) + ", " + Integer.toHexString(newword));
		//if (REGION_CPU[addr] != data) {
			int bit0, bit1, bit2;
			int r, g, b;
	
			REGION_CPU[addr] = data;
			int newword = READ_WORD(addr);
	
			/* only the bottom 8 bits are used */
			/* red component */
			bit0 = (newword >> 0) & 0x01;
			bit1 = (newword >> 1) & 0x01;
			bit2 = (newword >> 2) & 0x01;
			r = 0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2;
			/* green component */
			bit0 = (newword >> 3) & 0x01;
			bit1 = (newword >> 4) & 0x01;
			bit2 = (newword >> 5) & 0x01;
			g = 0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2;
			/* blue component */
			bit0 = 0;
			bit1 = (newword >> 6) & 0x01;
			bit2 = (newword >> 7) & 0x01;
			b = 0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2;
	
			//palette_change_color(offset / 2, r, g, b);
			palette_set_color((addr - 0x950000) >> 1, r, g, b);
		//}
	}

	public class Videoram_w implements WriteHandler {
		public void write(int address, int value) {
			REGION_CPU[address] = value;
			dirtybuffer[(address & 0x7ff) >> 1] = true;
		}
	}
	public WriteHandler videoram_w() {
		return new Videoram_w();
	}

	private String formatHex(int value, int len) {
		String s = Integer.toHexString(value);
		while (s.length() < len) {
			s = "0" + s;
		}
		return s.toUpperCase();
	}
	public BitMap video_update() {
		int offs;

		/* for every character in the Video RAM, check if it has been modified */
		/* since last time and update it accordingly. */
		for (offs = 0x800 - 2; offs >= 0; offs -= 2) {
			int data = (REGION_CPU[0x800000 + offs] << 8) | REGION_CPU[0x800001 + offs];
			//System.out.print(formatHex(data, 4) + ", ");
			int color = (data >> 8) & 0x3f;

			if (dirtybuffer[offs >> 1]) {
				int pict = (data & 0xff) | ((data >> 7) & 0x100);
				int sx, sy;

				dirtybuffer[offs / 2] = false;

				sx = ((offs / 2) / 32 + 1) % 32;
				sy = (offs / 2) % 32;

				drawgfx(
					charLayer,
					0,
					pict,
					color,
					0,
					0,
					8 * sx,
					8 * sy,
					0,
					GfxManager.TRANSPARENCY_NONE,
					0);
			}
		}
		//System.out.println();
		charLayer.toPixels(pixels);

		/* walk the motion object list. */
		for (offs = 0; offs < 0x1000; offs += 4)
		{
			int data1 = READ_WORD (offs + 0x01c000);
			int data2 = READ_WORD (offs + 2 + 0x01c000);

			int pict = data1 & 0xff;
			int color = (data1 >> 8) & 0x1f;
			int xpos = (data2 >> 8) & 0xff;
			int ypos = (0xff - data2 - 16) & 0xff;
			int hflip = (data1 >> 15) & 1;
			int vflip = (data1 >> 14) & 1;

			drawgfx(bitmap,1,
					pict,
					color,
					hflip,vflip,
					xpos,ypos,
					GfxManager.TRANSPARENCY_PEN,0);

			/* draw again with wraparound (needed to get the end of level animation right) */
			drawgfx(bitmap,1,
					pict,
					color,
					hflip,vflip,
					xpos-256,ypos,
					GfxManager.TRANSPARENCY_PEN,0);
		}		return bitmap;
	}
}
