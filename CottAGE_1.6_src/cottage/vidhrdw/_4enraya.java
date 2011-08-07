/***************************************************************************

  vidhrdw.c

  Functions to emulate the video hardware of the machine.

***************************************************************************/

package cottage.vidhrdw;

import cottage.mame.MAMEVideo;
import jef.map.*;
import jef.machine.*;
import jef.video.*;

public class _4enraya extends MAMEVideo implements VideoEmulator,Vh_refresh,Vh_start,Vh_stop,Vh_convert_color_proms {

static boolean dirty[] = new boolean[0x400];

static BitMap fgbitmap;

//static struct tilemap *tilemap;

	/* COTTAGE VIDEO INITIALIZATION */
	public void init(MachineDriver md) {
		super.init_bis(md);
		super.init(md);
	}

public WriteHandler fenraya_videoram_w() { return new Fenraya_videoram_w(); }
class Fenraya_videoram_w implements WriteHandler {
	public void write(int offset, int data)
	{
		RAM[videoram+((offset-videoram)&0x3ff)*2]=data;
		RAM[videoram+((offset-videoram)&0x3ff)*2+1]=((offset-videoram)&0xc00)>>10;
		dirty[offset&0x3ff] = true;		
	}
}

/*static void get_tile_info(int tile_index)
{
	int code = videoram[tile_index*2]+(videoram[tile_index*2+1]<<8);
	SET_TILE_INFO(
		0,
		code,
		0,
		0)
}*/

/*VIDEO_START( 4enraya )
{
	tilemap = tilemap_create( get_tile_info,tilemap_scan_rows,TILEMAP_OPAQUE,8,8,32,32 );
	return video_start_generic();
}

VIDEO_UPDATE( 4enraya)
{
	tilemap_draw(bitmap,cliprect,tilemap, 0,0);
}*/

public BitMap video_update()
{
	int offs;

	/* for every character in the Video RAM, check if it has been modified */
	/* since last time and update it accordingly. */
	for (offs = 0x400 - 1;offs >= 0;offs--)
	{
		if (dirty[offs]) {

			dirty[offs] = false;

			int sx,sy,flipx,flipy;

			sx = offs % 32;
			sy = offs / 32;

			flipx = 0;
			flipy = 0;

			int code = RAM[videoram+offs*2]+(RAM[videoram+offs*2+1]<<8);

			drawgfx(bitmap,0,
					code + ((color & 0xc0) << 2),
					0,
					flipx,flipy,
					8*sx,8*sy,
					GfxManager.TRANSPARENCY_PEN,3);
		}
	}

	return bitmap;
}

}