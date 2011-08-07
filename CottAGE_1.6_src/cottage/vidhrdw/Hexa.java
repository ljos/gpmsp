/***************************************************************************

  vidhrdw.c

  Functions to emulate the video hardware of the machine.

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

public class Hexa extends MAMEVideo implements VideoEmulator,Vh_refresh,Vh_start,Vh_stop,Vh_convert_color_proms {


static int charbank;
static int flipx,flipy;

	/* COTTAGE VIDEO INITIALIZATION */
	public void init(MachineDriver md) {
		super.init_bis(md);
		super.init(md);
	}



public class Hexa_d008_w implements WriteHandler {
	public void write(int address, int data) {

		int i;

		int bankaddress;


		/* bit 0 = flipx (or y?) */
		if (flipx != (data & 0x01))
		{
			flipx = data & 0x01;
			for(i=0; i<videoram_size; i++)
				dirtybuffer[i] = true;
		}

		/* bit 1 = flipy (or x?) */
		if (flipy != (data & 0x02))
		{
			flipy = data & 0x02;
			for(i=0; i<videoram_size; i++)
				dirtybuffer[i] = true;
		}

		/* bit 2 - 3 unknown */

		/* bit 4 could be the ROM bank selector for 8000-bfff (not sure) */
		bankaddress = 0x10000 + ((data & 0x10) >> 4) * 0x4000;
//		cpu_setbank(1,&RAM[bankaddress]);

		/* bit 5 = char bank */
		if (charbank != ((data & 0x20) >> 5))
		{
			charbank = (data & 0x20) >> 5;
			for(i=0; i<videoram_size; i++)
				dirtybuffer[i] = true;
		}

		/* bit 6 - 7 unknown */
	}
}

public WriteHandler hexa_d008_w() { return new Hexa_d008_w(); }



/***************************************************************************

  Draw the game screen in the given mame_bitmap.
  Do NOT call osd_update_display() from this function, it will be called by
  the main emulation engine.

***************************************************************************/
public BitMap video_update()
{
	int offs;


	/* for every character in the Video RAM, check if it has been modified */
	/* since last time and update it accordingly. */
	for (offs = videoram_size - 2;offs >= 0;offs -= 2)
	{
		if (dirtybuffer[offs] || dirtybuffer[offs + 1])
		{
			int sx,sy;


			dirtybuffer[offs] = false;
			dirtybuffer[offs + 1] = false;

			sx = (offs/2) % 32;
			sy = (offs/2) / 32;
			if (flipx!=0) sx = 31 - sx;
			if (flipy!=0) sy = 31 - sy;

			drawgfx(tmpbitmap,Machine_gfx[0],
					RAM[videoram + offs + 1] + ((RAM[videoram + offs] & 0x07) << 8) + (charbank << 11),
					(RAM[videoram + offs] & 0xf8) >> 3,
					flipx,flipy,
					8*sx,8*sy,
					Machine_visible_area,GfxManager.TRANSPARENCY_NONE,0);
		}
	}

	copybitmap(bitmap,tmpbitmap,0,0,0,0,Machine_visible_area,GfxManager.TRANSPARENCY_NONE,0);
	return bitmap;
}

}