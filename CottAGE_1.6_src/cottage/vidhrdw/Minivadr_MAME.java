/***************************************************************************

Minivader (Space Invaders's mini game)
(c)1990 Taito Corporation

Driver by Takahiro Nogi (nogi@kt.rim.or.jp) 1999/12/19 -

***************************************************************************/

package cottage.vidhrdw;

import jef.machine.MachineDriver;
import jef.map.WriteHandler;
import jef.video.BitMap;
import jef.video.GfxManager;
import jef.video.Vh_convert_color_proms;
import jef.video.Vh_refresh;
import jef.video.Vh_start;
import jef.video.Vh_stop;
import jef.video.VideoEmulator;

import cottage.mame.MAMEVideo;

public class Minivadr_MAME extends MAMEVideo implements VideoEmulator, Vh_refresh, Vh_start, Vh_stop, Vh_convert_color_proms {

	public void init(MachineDriver md) {
		super.init_bis(md);
		super.init(md);
	}



/*******************************************************************

	Palette Setting.

*******************************************************************/

	public void palette_init() {
		palette_set_color(0,0x00,0x00,0x00);
		palette_set_color(1,0xff,0xff,0xff);
	}


/*******************************************************************

	Draw Pixel.

*******************************************************************/
	public class Minivadr_videoram_w implements WriteHandler {
		public void write(int offset, int data) {

			int i;
			int x, y;
			int color;


			RAM[offset] = data;

			offset -= videoram;

			x = (offset % 32) * 8;
			y = (offset / 32);

			if (x >= Machine_visible_area_min_x &&
					x <= Machine_visible_area_max_x &&
					y >= Machine_visible_area_min_y &&
					y <= Machine_visible_area_max_y)
			{
				for (i = 0; i < 8; i++)
				{
					color = Machine_pens[((data >> i) & 0x01)];

					plot_pixel(tmpbitmap, x + (7 - i), y-16, color);
				}
			}
		}
	}

	public WriteHandler minivadr_videoram_w() { return new Minivadr_videoram_w();	}


	public BitMap video_update() {
		copybitmap(bitmap,tmpbitmap,0,0,0,0,Machine_visible_area,GfxManager.TRANSPARENCY_NONE,0);
		return bitmap;
	}

}