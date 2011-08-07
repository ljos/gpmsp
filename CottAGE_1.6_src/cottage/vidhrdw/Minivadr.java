/***************************************************************************

Minivader (Space Invaders's mini game)
(c)1990 Taito Corporation

Driver by Takahiro Nogi (nogi@kt.rim.or.jp) 1999/12/19 -

***************************************************************************/

/* COTTAGE OPTIMIZED VERSION */

package cottage.vidhrdw;

import jef.machine.MachineDriver;
import jef.map.WriteHandler;
import jef.video.Vh_convert_color_proms;
import jef.video.Vh_refresh;
import jef.video.Vh_start;
import jef.video.Vh_stop;
import jef.video.VideoEmulator;

import cottage.mame.MAMEVideo;

public class Minivadr extends MAMEVideo implements VideoEmulator, Vh_refresh, Vh_start, Vh_stop, Vh_convert_color_proms {

	int[]	pal = new int[2];
	int[][] gfx = new int[256][8];
	int[] REGION_CPU;

	public void init(MachineDriver md) {
		super.init(md);
		this.REGION_CPU = md.REGIONS[0];
	}

	public void palette_init() {
		pal[0] = 0x000000;
		pal[1] = 0xffffff;

		for (int i = 0; i < 256; i ++) {
			gfx[i][7] = pal[ i >> 7];
			gfx[i][6] = pal[(i &  64) >> 6];
			gfx[i][5] = pal[(i &  32) >> 5];
			gfx[i][4] = pal[(i &  16) >> 4];
			gfx[i][3] = pal[(i &   8) >> 3];
			gfx[i][2] = pal[(i &   4) >> 2];
			gfx[i][1] = pal[(i &   2) >> 1];
			gfx[i][0] = pal[(i &   1)];
		}
	}

	public class Minivadr_videoram_w implements WriteHandler {
		public void write(int address, int value) {
			int[] gfxval;

			REGION_CPU[address] = value;

			address -= 0xa000;

			int y = (address >> 5);

			if (y >= 16 && y <= 239) {
				int x = (address & 31) << 3;
				gfxval = gfx[value];
				int ofs = (y << 8) - (16<<8) + x + 7;
				pixels[ofs--] = gfxval[0];
				pixels[ofs--] = gfxval[1];
				pixels[ofs--] = gfxval[2];
				pixels[ofs--] = gfxval[3];
				pixels[ofs--] = gfxval[4];
				pixels[ofs--] = gfxval[5];
				pixels[ofs--] = gfxval[6];
				pixels[ofs]   = gfxval[7];
			}
		}
	}

	public WriteHandler minivadr_videoram_w() { return new Minivadr_videoram_w();	}
}