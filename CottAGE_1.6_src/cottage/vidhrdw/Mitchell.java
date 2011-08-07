/***************************************************************************

 Pang Video Hardware

***************************************************************************/

package cottage.vidhrdw;

import cottage.mame.MAMEVideo;
import jef.map.*;
import jef.machine.*;
import jef.video.*;

public class Mitchell
	extends MAMEVideo
	implements VideoEmulator, Vh_refresh, Vh_start, Vh_stop, Vh_convert_color_proms {

	/* Globals */
	BitMap tmpbitmap;
	int pang_videoram_size = 0x1000;
	private int[] pang_videoram = new int[0x10000];
	private int[] pang_colorram = new int[0x10000];
	private int[] pang_objram = new int[0x10000]; /* Sprite RAM */
	static int videoram, colorram, objram;
	static int video_bank;
	static int paletteram_bank;
	int flipscreen;

	/* Private */
	//static struct tilemap *bg_tilemap;

	/***************************************************************************
	 * 
	 * Start the video hardware emulation.
	 *  
	 **************************************************************************/

	public void init(MachineDriver md) {
		super.init_bis(md);
		super.init(md);
		/*
		 * pang_objram=null; paletteram=null;
		 * 
		 * bg_tilemap =
		 * tilemap_create(get_tile_info,tilemap_scan_rows,TILEMAP_TRANSPARENT,8,8,64,32);
		 * 
		 * if (bg_tilemap==null) return 1;
		 * tilemap_set_transparent_pen(bg_tilemap,15); // OBJ RAM
		 * pang_objram=auto_malloc(pang_videoram_size); if (pang_objram==null)
		 * return 1; memset(pang_objram, 0, pang_videoram_size);
		 *  // Palette RAM // Machine->drv->total_colors); if
		 * (paletteram==null) return 1; Machine->drv->total_colors);
		 *  
		 */
	}

	/***************************************************************************
	 * 
	 * Callbacks for the TileMap code
	 *  
	 **************************************************************************/

	/*
	 * static void get_tile_info(int tile_index) { unsigned char attr =
	 * pang_colorram[tile_index]; tile_index] + (pang_videoram[2*tile_index+1]
	 * << 8); SET_TILE_INFO( 0, code, attr & 0x7f, (attr & 0x80) ? TILE_FLIPX : 0)
	 */

	/***************************************************************************
	 * 
	 * Memory handlers
	 *  
	 **************************************************************************/

	/***************************************************************************
	 * OBJ / CHAR RAM HANDLERS (BANK 0 = CHAR, BANK 1=OBJ)
	 **************************************************************************/

	public WriteHandler pang_video_bank_w() {
		return new Pang_video_bank_w();
	}
	class Pang_video_bank_w implements WriteHandler {
		public void write(int address, int data) {
			/*
			 * Bank handler (sets base pointers for video write) (doesn't apply
			 * to mgakuen)
			 */
			video_bank = data;
		}
	}

	public WriteHandler mgakuen_videoram_w() {
		return new Mgakuen_videoram_w();
	}
	class Mgakuen_videoram_w implements WriteHandler {
		public void write(int offset, int data) {
			if (pang_videoram[offset] != data) {
				pang_videoram[offset] = data;
				//		tilemap_mark_tile_dirty(bg_tilemap,offset/2);
			}
		}
	}

	public ReadHandler mgakuen_videoram_r() {
		return new Mgakuen_videoram_r();
	}
	class Mgakuen_videoram_r implements ReadHandler {
		public int read(int address) {
			return pang_videoram[address];
		}
	}

	public WriteHandler mgakuen_objram_w() {
		return new Mgakuen_objram_w();
	}
	class Mgakuen_objram_w implements WriteHandler {
		public void write(int offset, int data) {
			pang_objram[offset] = data;
		}
	}

	public ReadHandler mgakuen_objram_r() {
		return new Mgakuen_objram_r();
	}
	class Mgakuen_objram_r implements ReadHandler {
		public int read(int address) {
			return pang_objram[address];
		}
	}

	public WriteHandler pang_videoram_w() {
		return new Pang_videoram_w();
	}
	class Pang_videoram_w implements WriteHandler {
		public void write(int offset, int data) {
			if (video_bank != 0)
				mgakuen_objram_w().write(offset, data);
			else
				mgakuen_videoram_w().write(offset, data);
		}
	}

	public ReadHandler pang_videoram_r() {
		return new Pang_videoram_r();
	}
	class Pang_videoram_r implements ReadHandler {
		public int read(int offset) {
			if (video_bank != 0)
				return mgakuen_objram_r().read(offset);
			else
				return mgakuen_videoram_r().read(offset);
		}
	}

	/***************************************************************************
	 * COLOUR RAM
	 **************************************************************************/

	public WriteHandler pang_colorram_w() {
		return new Pang_colorram_w();
	}
	class Pang_colorram_w implements WriteHandler {
		public void write(int offset, int data) {
			if (pang_colorram[offset] != data) {
				pang_colorram[offset] = data;
				//		tilemap_mark_tile_dirty(bg_tilemap,offset);
			}
		}
	}

	public ReadHandler pang_colorram_r() {
		return new Pang_colorram_r();
	}
	class Pang_colorram_r implements ReadHandler {
		public int read(int offset) {
			return pang_colorram[offset];
		}
	}

	/***************************************************************************
	 * PALETTE HANDLERS (COLOURS: BANK 0 = 0x00-0x3f BANK 1=0x40-0xff)
	 **************************************************************************/

	public WriteHandler pang_gfxctrl_w() {
		return new Pang_gfxctrl_w();
	}
	class Pang_gfxctrl_w implements WriteHandler {
		public void write(int address, int data) {
			/* bit 0 is unknown (used, maybe back color enable?) */

			/* bit 1 is coin counter */
			//	coin_counter_w(0,data & 2);

			/* bit 2 is flip screen */
			if (flipscreen != (data & 0x04)) {
				flipscreen = data & 0x04;
				//		tilemap_set_flip(ALL_TILEMAPS,flipscreen ? (TILEMAP_FLIPY |
				// TILEMAP_FLIPX) : 0);
			}

			/*
			 * bit 3 is unknown (used, e.g. marukin pulses it on the title
			 * screen)
			 */

			/* bit 4 selects OKI M6295 bank */
			//	OKIM6295_set_bank_base(0, (data & 0x10) ? 0x40000 : 0x00000);

			/* bit 5 is palette RAM bank selector (doesn't apply to mgakuen) */
			paletteram_bank = data & 0x20;

			/*
			 * bits 6 and 7 are unknown, used in several places. At first I
			 * thought
			 */
			/*
			 * they were bg and sprites enable, but this screws up spang
			 * (screen flickers
			 */
			/*
			 * every time you pop a bubble). However, not using them as enable
			 * bits screws
			 */
			/*
			 * up marukin - you can see partially built up screens during
			 * attract mode.
			 */
		}
	}

	public WriteHandler pang_paletteram_w() {
		return new Pang_paletteram_w();
	}

	class Pang_paletteram_w implements WriteHandler {
		public void write(int offset, int data) {
			/*
			 * if (paletteram_bank!=0) paletteram_xxxxRRRRGGGGBBBB_w(offset +
			 * 0x800,data);
			 */
		}
	}

	public ReadHandler pang_paletteram_r() {
		return new Pang_paletteram_r();
	}
	class Pang_paletteram_r implements ReadHandler {
		public int read(int offset) {
			return 0;
			/*
			 * if (paletteram_bank!=0) return paletteram_r(offset + 0x800);
			 */
		}
	}

	public WriteHandler mgakuen_paletteram_w() {
		return new Mgakuen_paletteram_w();
	}
	class Mgakuen_paletteram_w implements WriteHandler {
		public void write(int offset, int data) {
			//	paletteram_xxxxRRRRGGGGBBBB_w(offset,data);
		}
	}

	public ReadHandler mgakuen_paletteram_r() {
		return new Mgakuen_paletteram_r();
	}
	class Mgakuen_paletteram_r implements ReadHandler {
		public int read(int offset) {
			return 0;
			//	return paletteram_r(offset);
		}
	}

	/***************************************************************************
	 * 
	 * Display refresh
	 *  
	 **************************************************************************/

	void draw_sprites(/* BitMap bitmap,const struct rectangle *cliprect */
	) {
		int offs, sx, sy;

		// the last entry is not a sprite, we skip it otherwise spang shows a
		// bubble
		// moving diagonally across the screen
		for (offs = 0xd000 + 0x1000 - 0x40; offs >= 0xd000; offs -= 0x20) {
			int code = pang_objram[offs];
			int attr = pang_objram[offs + 1];
			int color = attr & 0x0f;
			sx = pang_objram[offs + 3] + ((attr & 0x10) << 4);
			sy = ((pang_objram[offs + 2] + 8) & 0xff) - 8;
			code += (attr & 0xe0) << 3;
			if (flipscreen != 0) {
				sx = 496 - sx;
				sy = 240 - sy;
			}
			drawgfx(
				bitmap,
				1,
				code,
				color,
				flipscreen,
				flipscreen,
				sx,
				sy,
				0,
				GfxManager.TRANSPARENCY_PEN,
				15);
		}
	}

	public int vh_start() {

		/*
		 * OBJ RAM
		 */
		//pang_objram = new int[pang_videoram_size];
		if (pang_objram == null)
			return 1;
		//memset(pang_objram, 0, pang_videoram_size);

		tmpbitmap = new BitMapImpl(384, 240);
		return 0;
	}

	public BitMap video_update() {
		/*
		 * fillbitmap(bitmap,Machine->pens[0],cliprect);
		 */
		draw_tiles();
		draw_sprites(/* bitmap,cliprect */
		);
		//System.out.println("appel");
		return bitmap;
	}

	/**
	 * 
	 */
	private void draw_tiles() {
		for (int i = 0xd000; i < 0xe000; i++) {
			int offs = i - 0xd000;
			int sx = ((offs/2) % 64) * 8;
			int sy = ((offs/2) / 64) * 8;
			int code = pang_videoram[i];
			int color = pang_colorram[i] & 7;
			
			drawgfx(
					bitmap,
					1,
					code,
					color,
					0,
					0,
					sx,
					sy,
					0,
					GfxManager.TRANSPARENCY_PEN,
					15);
			
		}
		
	}
}
