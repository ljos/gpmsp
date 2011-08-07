/***************************************************************************

  vidhrdw.c

  Functions to emulate the video hardware of the machine.

***************************************************************************/

package cottage.vidhrdw;

import jef.machine.MachineDriver;
import jef.video.BitMap;
import jef.video.GfxManager;
import jef.video.Vh_convert_color_proms;
import jef.video.Vh_refresh;
import jef.video.Vh_start;
import jef.video.Vh_stop;
import jef.video.VideoEmulator;

import cottage.mame.MAMEVideo;

public class Bublbobl extends MAMEVideo implements VideoEmulator,Vh_refresh,Vh_start,Vh_stop,Vh_convert_color_proms {

public int[] Fbublbobl_objectram = {0};
public int[] Fbublbobl_objectram_size = {0};
static int bublbobl_objectram, bublbobl_objectram_size;
public int bublbobl_video_enable;

	/* COTTAGE VIDEO INITIALIZATION */
	public void init(MachineDriver md) {
		super.init_bis(md);
		super.init(md);
		bublbobl_objectram = Fbublbobl_objectram[0];
		bublbobl_objectram_size = Fbublbobl_objectram_size[0];
	}

/***************************************************************************

  Draw the game screen in the given mame_bitmap.
  Do NOT call osd_update_display() from this function, it will be called by
  the main emulation engine.

***************************************************************************/
public BitMap video_update()
{
	int offs;
	int sx,sy,xc,yc;
	int gfx_num,gfx_attr,gfx_offs;
	int prom_line;

	/* Bubble Bobble doesn't have a real video RAM. All graphics (characters */
	/* and sprites) are stored in the same memory region, and information on */
	/* the background character columns is stored in the area dd00-dd3f */

	/* This clears & redraws the entire screen each pass */
	fillbitmap(bitmap,Machine_pens[255],visible_area());

	if (bublbobl_video_enable == 0) return bitmap;

	sx = 0;

	for (offs = 0;offs < bublbobl_objectram_size;offs += 4)
    {
		/* skip empty sprites */
		/* this is dword aligned so the UINT32 * cast shouldn't give problems */
		/* on any architecture */
		if ((RAM[bublbobl_objectram+offs] == 0) &&
			(RAM[bublbobl_objectram+offs+1] == 0) &&
			(RAM[bublbobl_objectram+offs+2] == 0) &&
			(RAM[bublbobl_objectram+offs+3] == 0))
			continue;

		gfx_num = RAM[bublbobl_objectram + offs + 1];
		gfx_attr = RAM[bublbobl_objectram + offs + 3];
		prom_line = 0x80 + ((gfx_num & 0xe0) >> 1);

		gfx_offs = ((gfx_num & 0x1f) * 0x80);
		if ((gfx_num & 0xa0) == 0xa0)
			gfx_offs |= 0x1000;

		sy = -RAM[bublbobl_objectram + offs + 0];

		for (yc = 0;yc < 32;yc++)
		{
			if ((PROM[prom_line + yc/2] & 0x08) != 0)	continue;	/* NEXT */

			if ((PROM[prom_line + yc/2] & 0x04) == 0)	/* next column */
			{
				sx = RAM[bublbobl_objectram + offs + 2];
				if ((gfx_attr & 0x40) != 0) sx -= 256;
			}

			for (xc = 0;xc < 2;xc++)
			{
				int goffs,code,color,flipx,flipy,x,y;

				goffs = gfx_offs + xc * 0x40 + (yc & 7) * 0x02 +
						(PROM[prom_line + yc/2] & 0x03) * 0x10;
				code = RAM[videoram + goffs] + 256 * (RAM[videoram + goffs + 1] & 0x03) + 1024 * (gfx_attr & 0x0f);
				color = (RAM[videoram + goffs + 1] & 0x3c) >> 2;
				flipx = RAM[videoram + goffs + 1] & 0x40;
				flipy = RAM[videoram + goffs + 1] & 0x80;
				x = sx + xc * 8;
				y = (sy + yc * 8) & 0xff;

				/*if (flip_screen)
				{
					x = 248 - x;
					y = 248 - y;
					flipx = !flipx;
					flipy = !flipy;
				}*/

				drawgfx(bitmap,Machine_gfx[0],
						code,
						color,
						flipx,flipy,
						x,y,
						Machine_visible_area,GfxManager.TRANSPARENCY_PEN,15);
			}
		}

		sx += 16;
	}
	return bitmap;
}

}