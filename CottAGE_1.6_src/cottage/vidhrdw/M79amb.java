/***************************************************************************

  vidhrdw.c

  Functions to emulate the video hardware of the machine.

***************************************************************************/

package cottage.vidhrdw;

import jef.machine.MachineDriver;
import jef.map.WriteHandler;
import jef.video.BitMap;
import jef.video.Vh_convert_color_proms;
import jef.video.Vh_refresh;
import jef.video.Vh_start;
import jef.video.Vh_stop;
import jef.video.VideoEmulator;

import cottage.mame.MAMEVideo;

public class M79amb extends MAMEVideo implements VideoEmulator,Vh_refresh,Vh_start,Vh_stop,Vh_convert_color_proms {

	/* COTTAGE VIDEO INITIALIZATION */
	public void init(MachineDriver md) {
		super.init_bis(md);
		super.init(md);
	}



/* palette colors (see drivers/8080bw.c) */
static int BLACK = 0;
static int WHITE = 1;


static int mask = 0;

public void palette_init() {
	palette_set_color(0,0x00,0x00,0x00); /* BLACK */
	palette_set_color(1,0xff,0xff,0xff); /* WHITE */
	palette_set_color(2,0xff,0x20,0x20); /* RED */
	palette_set_color(3,0x20,0xff,0x20); /* GREEN */
	palette_set_color(4,0xff,0xff,0x20); /* YELLOW */
	palette_set_color(5,0x20,0xff,0xff); /* CYAN */
	palette_set_color(6,0xff,0x20,0xff); /* PURPLE */
}


public class Ramtek_mask_w implements WriteHandler {
	public void write(int address, int data) {
		mask = data;
	}
}

public WriteHandler ramtek_mask_w() { return new Ramtek_mask_w(); }

public class Ramtek_videoram_w implements WriteHandler {
	public void write(int address, int data) {
		data = data & ~mask;

		if (RAM[address] != data)
		{
			int i,x,y;

			RAM[address] = data;

			int offset = address - videoram;

			y = offset / 32;
			x = 8 * (offset % 32);

			for (i = 0; i < 8; i++)
			{
				//plot_pixel2(Machine->scrbitmap, tmpbitmap, x, y, Machine->pens[((data & 0x80)!=0) ? WHITE : BLACK]);
				plot_pixel(bitmap, x, y, Machine_pens[((data & 0x80)!=0) ? WHITE : BLACK]);

				x++;
				data <<= 1;
			}
		}
	}
}

public WriteHandler ramtek_videoram_w() { return new Ramtek_videoram_w(); }


public BitMap video_update() {
	return bitmap;
}

}