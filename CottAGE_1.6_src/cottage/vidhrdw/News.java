package cottage.vidhrdw;

import jef.machine.MachineDriver;
import jef.map.WriteHandler;
import jef.video.BitMap;
import jef.video.Get_tile_info;
import jef.video.TileMap;
import jef.video.Vh_convert_color_proms;
import jef.video.Vh_refresh;
import jef.video.Vh_start;
import jef.video.Vh_stop;
import jef.video.VideoEmulator;

import cottage.mame.MAMEVideo;

public class News extends MAMEVideo implements VideoEmulator,Vh_refresh,Vh_start,Vh_stop,Vh_convert_color_proms {


public int[] Fnews_fgram = {0};
public int[] Fnews_bgram = {0};
static int news_fgram, news_bgram;

static int bgpic;
static TileMap fg_tilemap, bg_tilemap;



	/* COTTAGE VIDEO INITIALIZATION */
	public void init(MachineDriver md) {
		super.init_bis(md);
		super.init(md);
		news_fgram = Fnews_fgram[0];
		news_bgram = Fnews_bgram[0];
	}

/***************************************************************************

  Callbacks for the TileMap code

***************************************************************************/

public class Get_fg_tile_info implements Get_tile_info {
	public void get_tile_info(int tile_index) {
		int code = (RAM[news_fgram + tile_index*2] << 8) | RAM[news_fgram + tile_index*2+1];
		SET_TILE_INFO(
			0,
			code & 0x0fff,
			(code & 0xf000) >> 12,
			0);
	}
}

public Get_tile_info get_fg_tile_info() { return new Get_fg_tile_info(); }

public class Get_bg_tile_info implements Get_tile_info {
	public void get_tile_info(int tile_index) {
		int code = (RAM[news_bgram + tile_index*2] << 8) | RAM[news_bgram + tile_index*2+1];
		int color = (code & 0xf000) >> 12;

		code &= 0x0fff;
		if ((code & 0x0e00) == 0x0e00) code = (code & 0x1ff) | (bgpic << 9);

		SET_TILE_INFO(
			0,
			code,
			color,
			0);
	}
}

public Get_tile_info get_bg_tile_info() { return new Get_bg_tile_info(); }



/***************************************************************************

  Start the video hardware emulation.

***************************************************************************/

public int vh_start()
{
	fg_tilemap = tilemap_create(get_fg_tile_info(),tilemap_scan_rows,TILEMAP_TRANSPARENT,8,8,32, 32);
	tilemap_set_transparent_pen(fg_tilemap,0);

	bg_tilemap = tilemap_create(get_bg_tile_info(),tilemap_scan_rows,TILEMAP_OPAQUE,8,8,32, 32);

	return 0;
}



/***************************************************************************

  Memory handlers

***************************************************************************/

public class News_fgram_w implements WriteHandler {
	public void write(int address, int data) {
		RAM[address] = data;
	}
}

public WriteHandler news_fgram_w() { return new News_fgram_w(); }

public class News_bgram_w implements WriteHandler {
	public void write(int address, int data) {
		RAM[address] = data;
	}
}

public WriteHandler news_bgram_w() { return new News_bgram_w(); }

public class News_bgpic_w implements WriteHandler {
	public void write(int address, int data) {
		if (bgpic != data)
		{
			bgpic = data;
		}
	}
}

public WriteHandler news_bgpic_w() { return new News_bgpic_w(); }



/***************************************************************************

  Display refresh

***************************************************************************/

public BitMap video_update() {
	tilemap_draw(bitmap,cliprect,bg_tilemap,0,0);
	tilemap_draw(bitmap,cliprect,fg_tilemap,0,0);
	return bitmap;
}

}