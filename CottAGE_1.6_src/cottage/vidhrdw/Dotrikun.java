/***************************************************************************

Dottori Kun (Head On's mini game)
(c)1990 SEGA

Driver by Takahiro Nogi (nogi@kt.rim.or.jp) 1999/12/15 -

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

public class Dotrikun extends MAMEVideo implements VideoEmulator, Vh_refresh, Vh_start, Vh_stop, Vh_convert_color_proms {

//	int[] oldpal = new int[2];
	int[]	pal = new int[2];
	int[] REGION_CPU;

	WriteHandler videoram_w;

	public void init(MachineDriver md) {
		super.init(md);
		this.REGION_CPU = md.REGIONS[0];
		pal[0] = 0x000000;
		pal[1] = 0xffffff;
//		oldpal[0] = -1;
//		oldpal[1] = -1;
	}

	/*******************************************************************

		Palette Setting.

	*******************************************************************/
	public class Dotrikun_color_w implements WriteHandler {
		public void write(int address, int data) {
			int r, g, b;

			r = (((data & 0x08)!=0) ? 0xff : 0x00);
			g = (((data & 0x10)!=0) ? 0xff : 0x00);
			b = (((data & 0x20)!=0) ? 0xff : 0x00);
			//palette_set_color(0, r, g, b);		// BG color
			pal[0] = (r<<16)|(g<<8)|b;

			r = (((data & 0x01)!=0) ? 0xff : 0x00);
			g = (((data & 0x02)!=0) ? 0xff : 0x00);
			b = (((data & 0x04)!=0) ? 0xff : 0x00);
			//palette_set_color(1, r, g, b);		// DOT color
			pal[1] = (r<<16)|(g<<8)|b;

			/* CottAGE fixes for background color */
//			if ((oldpal[0]!=pal[0]) || (oldpal[1]!=pal[1]))
				for(int i=0; i<0x2000; i++)
					videoram_w.write(0x8000+i,REGION_CPU[0x8000+i]);
//			oldpal[0] = pal[0];
//			oldpal[1] = pal[1];
		}
	}

	public WriteHandler dotrikun_color_w() { return new Dotrikun_color_w();	}

	public BitMap video_update() {
		return bitmap;
	}

	public class Dotrikun_videoram_w implements WriteHandler {
		public void write(int offset, int data) {
			REGION_CPU[offset] = data;

			offset -= 0x8000;

			int x = 2 * ((offset & 15) << 3);
			int y = 2 * (offset >> 4);

			if (x >= 0 && x <= 255 && y >= 0 && y <= 191)
				for (int i = 0; i < 8; i++) {
					int color = pal[(data>>i)&1];
					pixels[256*y + x + 2*(7-i)] = color;
					pixels[256*y + x + 2*(7-i)+1] = color;
					pixels[256*(y+1) + x + 2*(7-i)] = color;
					pixels[256*(y+1) + x + 2*(7-i)+1] = color;
				}
		}
	}

	public WriteHandler dotrikun_videoram_w() { videoram_w = new Dotrikun_videoram_w();	return videoram_w; }
}