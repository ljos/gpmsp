package cottage.mame;

import jef.machine.*;
import jef.map.*;
import jef.video.*;

/**
 * Helper class for porting MAME /vidhrdw/* classes to CottAGE
 */
public class MAMEVideo implements VideoEmulator,Vh_convert_color_proms,Eof_callback,Vh_start,Vh_stop,Vh_refresh {

	public int[] pixels;
	public BitMap backBuffer;
	public int width;
	public int height;
	public int[] visible;
	protected int[] cliprect;
	public GfxDecodeInfo[] gdi;
	public GfxManager[] gfxMan;
	public int total_colors;
	public int color;
	public int rot;

	public int vX;
	public int vY;

	private int[] palette;
	protected int[][] color_table;

	protected boolean videoModifiesPalette;

	public BitMap bitmap;
	public BitMap tmpbitmap;

	public int Machine_drv_total_colors;
	public int[] Machine_pens;
	public int[] Machine_gfx;
	public int Machine_visible_area;
	public int Machine_visible_area_min_x;
	public int Machine_visible_area_max_x;
	public int Machine_visible_area_min_y;
	public int Machine_visible_area_max_y;

	/* RAM addresses */
	public static int videoram;
	public static int colorram;
	public static int spriteram;
	public static int spriteram_2;
	public static int spriteram_3;
	public static int paletteram;
	public static int paletteram_2;

	/* RAM sizes */
	public static int videoram_size;
	public static int spriteram_size;
	public static int spriteram_2_size;
	public static int spriteram_3_size;

	public boolean[] dirtybuffer;

	protected int[] RAM;
	protected int[] PROM;
	protected int[][] GFX_REGIONS;
	protected int[] color_prom;

	public static final int TILEMAP_OPAQUE = 0x00;
	public static final int TILEMAP_TRANSPARENT = 0x01;
	/* buffered RAM */
	public int[] buffered_spriteram;
	
	public static final int tilemap_scan_rows = 0;

	static int curgfx;
	static int curcode;
	static int curcolor;
	static int curflags;
	private MachineDriver md;
	
	protected int[] palette_used_colors;
	protected static final int PALETTE_COLOR_UNUSED	=0;	/* This color is not needed for this frame */
	protected static final int PALETTE_COLOR_VISIBLE	=1;	/* This color is currently visible */
	protected static final int PALETTE_COLOR_CACHED	=2;	/* This color is cached in temporary bitmaps */
	/* palette_recalc() will try to use always the same pens for the used colors; */
	/* if it is forced to rearrange the pens, it will return TRUE to signal the */
	/* driver that it must refresh the display. */
	protected static final int PALETTE_COLOR_TRANSPARENT_FLAG	=4;	/* All colors using this attribute will be */
	/* mapped to the same pen, and no other colors will be mapped to that pen. */
	/* This way, transparencies can be handled by copybitmap(). */

	/* backwards compatibility */
	protected static final int PALETTE_COLOR_USED			=(PALETTE_COLOR_VISIBLE|PALETTE_COLOR_CACHED);
	protected static final int PALETTE_COLOR_TRANSPARENT	=(PALETTE_COLOR_TRANSPARENT_FLAG|PALETTE_COLOR_USED);
	
	protected MachineDriver getMachineDriver() {
		return md;
	}

	/* C Facilities */

	public final boolean[] bauto_malloc(int size) {
		return new boolean[size];
	}

	public final void memset(boolean[] buffer, int value, int size) {
		boolean bval = (value == 0) ? false : true;
		for(int i=0; i<size; i++) {
			buffer[i] = bval;
		}
	}
	
	protected void memset(int[] array, int v, int s) {
		for (int i = 0; i < s; i++) {
			array[i] = v;
		}
	}
    
    protected void memcpy(int[] dst, int[] src, int nbytes) {
        System.arraycopy(src,0,dst,0,nbytes);        
    }
	
	protected int[] malloc(int size) {
		return new int[size];
	}

	/* Tilemap stuff */
	/* - memory offset not handled yet */
	/* - only 4-bit planes supported */
	/* - no flags supported */
	/* - no dirty supported */

	public void SET_TILE_INFO(int gfx, int code, int color, int flags) {
		curgfx = gfx;
		curcode = code;
		curcolor = color;
		curflags = flags;
	}

	public TileMap tilemap_create(Get_tile_info tile_get_info,int get_memory_offset, int type, int tile_width, int tile_height, int num_cols, int num_rows) {
		return new TileMap(tile_get_info,get_memory_offset,type,tile_width,tile_height,num_cols,num_rows);
	}

	public void tilemap_set_transparent_pen(TileMap tilemap, int pen) {
		tilemap.pen = pen;
	}

	public void tilemap_mark_tile_dirty(TileMap tilemap, int memory_offset) {
	}

	public void tilemap_mark_all_tiles_dirty(TileMap tilemap) {
	}

	public void tilemap_draw(BitMap dest, int[] cliprect, TileMap tilemap, int flags, int priority) {
		int i,j,k,l;
		int tile_index;
		int tile_data;
		int tile_code;
		int tile_color;
		int[] GFX;
		int nbtiles_per_row = tilemap.cols;
		int tile_width = tilemap.width;
		int tile_height = tilemap.height;
		boolean transparent = ((tilemap.type & TILEMAP_TRANSPARENT)!=0);
		int yofs;
		int xofs;
		int ofs;
		int tdata;
		int pen = 0;
		int row_start,row_end;
		int col_start,col_end;

		if (transparent)
			pen = tilemap.pen;

		row_start = visible[2]/tile_height;
		row_end = (visible[3]+1)/tile_height;

		col_start = visible[0]/tile_width;
		col_end = (visible[1]+1)/tile_width;

		/* Draw tilemap */

		yofs = 0;
		for(j=row_start; j<row_end; j++) {
			xofs = 0;
			for(i=col_start; i<col_end; i++) {
				tile_index = nbtiles_per_row*j+i;
				tilemap.tile_info.get_tile_info(tile_index);

				GFX = GFX_REGIONS[curgfx];

				tile_code = curcode * (gdi[curgfx].gfx.bytes >> 3) ;
				tile_color = curcolor << gdi[curgfx].gfx.planes;

				ofs = yofs + xofs;

				if (transparent) {
					for(k=0; k<tile_height; k++) {
						for(l=0; l<tile_width; ) {
							tile_data = GFX[tile_code++];
							tdata = ((tile_data>>4)&0x0F);
							if (tdata!=pen) pixels[ofs + l] = palette[tile_color + tdata]; l++;
							tdata = (tile_data&0x0F);
							if (tdata!=pen) pixels[ofs + l] = palette[tile_color + tdata]; l++;
						}
						ofs += width;
					}
				} else {
					for(k=0; k<tile_height; k++) {
						for(l=0; l<tile_width; ) {
							tile_data = GFX[tile_code++];
							pixels[ofs + l] = palette[tile_color + ((tile_data>>4)&0x0F)]; l++;
							pixels[ofs + l] = palette[tile_color + (tile_data&0x0F)]; l++;
						}
						ofs += width;
					}
				}
				xofs += tile_width;
			}
			yofs += width * tile_height;
		}
	}

	public ReadHandler paletteram_r() { return new Paletteram_r(); }
	public class Paletteram_r implements ReadHandler {
		public int read(int offset) {
			return RAM[offset];
		}
	}

	public WriteHandler videoram_w() { return new Videoram_w(); }
	public class Videoram_w implements WriteHandler {
		public void write(int offset, int data) {
			if (RAM[offset] != data)
			{
				dirtybuffer[offset - videoram] = true;
				RAM[offset] = data;
			}
		}
	}

	public WriteHandler colorram_w() { return new Colorram_w(); }
	public class Colorram_w implements WriteHandler {
		public void write(int offset, int data) {
			if (RAM[offset] != data)
			{
				dirtybuffer[offset - colorram] = true;
				RAM[offset] = data;
			}
		}
	}

	/***************************************************************************

	  This assumes the commonly used resistor values:

	  bit 3 -- 220 ohm resistor  -- RED/GREEN/BLUE
      	  -- 470 ohm resistor  -- RED/GREEN/BLUE
	        -- 1  kohm resistor  -- RED/GREEN/BLUE
	  bit 0 -- 2.2kohm resistor  -- RED/GREEN/BLUE

	***************************************************************************/
	public Vh_convert_color_proms RRRR_GGGG_BBBB() { return new RRRR_GGGG_BBBB_pi(); }
	public class RRRR_GGGG_BBBB_pi implements Vh_convert_color_proms {
		public void palette_init() {
			int i;

			System.out.println("Machine_drv_total_colors " + Machine_drv_total_colors);

			for (i = 0;i < Machine_drv_total_colors;i++)
			{
				int bit0,bit1,bit2,bit3,r,g,b;


				/* red component */
				bit0 = (color_prom[i] >> 0) & 0x01;
				bit1 = (color_prom[i] >> 1) & 0x01;
				bit2 = (color_prom[i] >> 2) & 0x01;
				bit3 = (color_prom[i] >> 3) & 0x01;
				r = 0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3;
				/* green component */
				bit0 = (color_prom[i + Machine_drv_total_colors] >> 0) & 0x01;
				bit1 = (color_prom[i + Machine_drv_total_colors] >> 1) & 0x01;
				bit2 = (color_prom[i + Machine_drv_total_colors] >> 2) & 0x01;
				bit3 = (color_prom[i + Machine_drv_total_colors] >> 3) & 0x01;
				g = 0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3;
				/* blue component */
				bit0 = (color_prom[i + 2*Machine_drv_total_colors] >> 0) & 0x01;
				bit1 = (color_prom[i + 2*Machine_drv_total_colors] >> 1) & 0x01;
				bit2 = (color_prom[i + 2*Machine_drv_total_colors] >> 2) & 0x01;
				bit3 = (color_prom[i + 2*Machine_drv_total_colors] >> 3) & 0x01;
				b = 0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3;

				palette_set_color(i,r,g,b);
			}
		}
	}
	
	public Vh_convert_color_proms BBBB_GGGG_RRRR() { return new BBBB_GGGG_RRRR_pi(); }
	public class BBBB_GGGG_RRRR_pi implements Vh_convert_color_proms {
		public void palette_init() {
			int i;


			for (i = 0;i < Machine_drv_total_colors;i++)
			{
				int bit0,bit1,bit2,bit3,r,g,b;


				/* red component */
				bit0 = (color_prom[i] >> 0) & 0x01;
				bit1 = (color_prom[i] >> 1) & 0x01;
				bit2 = (color_prom[i] >> 2) & 0x01;
				bit3 = (color_prom[i] >> 3) & 0x01;
				r = 0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3;
				/* green component */
				bit0 = (color_prom[i + Machine_drv_total_colors] >> 0) & 0x01;
				bit1 = (color_prom[i + Machine_drv_total_colors] >> 1) & 0x01;
				bit2 = (color_prom[i + Machine_drv_total_colors] >> 2) & 0x01;
				bit3 = (color_prom[i + Machine_drv_total_colors] >> 3) & 0x01;
				g = 0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3;
				/* blue component */
				bit0 = (color_prom[i + 2*Machine_drv_total_colors] >> 0) & 0x01;
				bit1 = (color_prom[i + 2*Machine_drv_total_colors] >> 1) & 0x01;
				bit2 = (color_prom[i + 2*Machine_drv_total_colors] >> 2) & 0x01;
				bit3 = (color_prom[i + 2*Machine_drv_total_colors] >> 3) & 0x01;
				b = 0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3;

				palette_set_color(i,r,g,b);
			}
		}
	}
	
	public WriteHandler paletteram_RRRRGGGGBBBBxxxx_split1_w() { return new Paletteram_RRRRGGGGBBBBxxxx_split1_w(); }
	public class Paletteram_RRRRGGGGBBBBxxxx_split1_w implements WriteHandler {
		public void write(int address, int data) {
			RAM[address] = data;
			changecolor_RRRRGGGGBBBBxxxx(address - paletteram, RAM[address] | (RAM[address-paletteram+paletteram_2] << 8));
		}
	}

	public WriteHandler paletteram_RRRRGGGGBBBBxxxx_split2_w() { return new Paletteram_RRRRGGGGBBBBxxxx_split2_w(); }
	public class Paletteram_RRRRGGGGBBBBxxxx_split2_w implements WriteHandler {
		public void write(int address, int data) {
			RAM[address] = data;
			changecolor_RRRRGGGGBBBBxxxx(address - paletteram_2, RAM[address-paletteram_2+paletteram] | (RAM[address] << 8));
		}
	}

	public void changecolor_xBBBBBGGGGGRRRRR(int color,int data)
	{
		int r,g,b;

		r = (data >>  0) & 0x1f;
		g = (data >>  5) & 0x1f;
		b = (data >> 10) & 0x1f;

		r = (r << 3);
		g = (g << 3);
		b = (b << 3);
		palette_set_color(color,r,g,b);
	}
	
	
	
	public void changecolor_xxxxRRRRGGGGBBBB(int color,int data)
	{
		int r,g,b;

		r = (data >> 8) & 0x0f;
		g = (data >> 4) & 0x0f;
		b = (data >> 0) & 0x0f;

		r = (r << 4) | r;
		g = (g << 4) | g;
		b = (b << 4) | b;

		palette_set_color(color,r,g,b);
	}

	public WriteHandler paletteram_xxxxRRRRGGGGBBBB_swap_w() { return new Paletteram_xxxxRRRRGGGGBBBB_swap_w(); }
	public class Paletteram_xxxxRRRRGGGGBBBB_swap_w implements WriteHandler {
		public void write(int address, int data) {
			RAM[address] = data;
			changecolor_xxxxRRRRGGGGBBBB((address - paletteram)>>1, RAM[address | 1] | (RAM[address & ~1] << 8));
		}
	}

	public void changecolor_RRRRGGGGBBBBxxxx(int color,int data)
	{
		int r,g,b;

		r = (data >> 12) & 0x0f;
		g = (data >>  8) & 0x0f;
		b = (data >>  4) & 0x0f;

		r = (r << 4) | r;
		g = (g << 4) | g;
		b = (b << 4) | b;
		palette_set_color(color,r,g,b);
	}

	public WriteHandler paletteram_RRRRGGGGBBBBxxxx_swap_w() { return new Paletteram_RRRRGGGGBBBBxxxx_swap_w(); }
	public class Paletteram_RRRRGGGGBBBBxxxx_swap_w implements WriteHandler {
		public void write(int address, int data) {
			RAM[address] = data;
			changecolor_RRRRGGGGBBBBxxxx((address - paletteram)>>1, RAM[address | 1] | (RAM[address & ~1] << 8));
		}
	}

	public void  init_bis(MachineDriver md) {
		this.md = md;
		/* Init generic stuff */
		RAM = md.REGIONS[0];
		PROM = md.REGIONS[16];

		GFX_REGIONS = new int[8][];
		GFX_REGIONS[0] = md.REGIONS[8];
		GFX_REGIONS[1] = md.REGIONS[9];
		GFX_REGIONS[2] = md.REGIONS[10];
		GFX_REGIONS[3] = md.REGIONS[11];
		GFX_REGIONS[4] = md.REGIONS[12];
		GFX_REGIONS[5] = md.REGIONS[13];
		GFX_REGIONS[6] = md.REGIONS[14];
		GFX_REGIONS[7] = md.REGIONS[15];

		color_prom = md.REGIONS[16];

		/* Update RAM addresses */
		//videoram     = md.videoram;
		//colorram     = md.colorram;
		//spriteram    = md.spriteram;
		//spriteram_2  = md.spriteram_2;
		//spriteram_3  = md.spriteram_3;
		//paletteram   = md.paletteram;
		//paletteram_2 = md.paletteram_2;

		/* Update RAM sizes */
		//videoram_size    = md.videoram_size;
		//spriteram_size   = md.spriteram_size;
		//spriteram_2_size = md.spriteram_2_size;
		//spriteram_3_size = md.spriteram_3_size;

		/* Initialize dirty buffer */
		dirtybuffer = new boolean[videoram_size];
		for(int j=0; j<videoram_size; j++)
			dirtybuffer[j] = true;

		/* Initialize fake constants */
		this.Machine_drv_total_colors = md.pal;
		this.Machine_gfx = new int[8]; for(int i=0; i<8; i++) this.Machine_gfx[i] = i;
		this.Machine_visible_area = 1;

		this.Machine_visible_area_min_x = md.visible[0];
		this.Machine_visible_area_max_x = md.visible[1];
		this.Machine_visible_area_min_y = md.visible[2];
		this.Machine_visible_area_max_y = md.visible[3];
	}

	public void  init(MachineDriver md) {
		this.md = md;
		this.width = md.w;
		this.height = md.h;
		this.visible = md.visible;
		this.cliprect = md.visible;
		this.gdi = md.gfx;
		this.total_colors = md.pal;
		this.palette = new int[total_colors];
		palette_used_colors = new int[(1+1+1+3+1) * md.pal];

		this.Machine_pens = palette;

		this.color = md.col;
		this.rot = md.ROT;
		this.pixels = new int[((visible[1]+1) - visible[0]) * ((visible[3]+1) - visible[2])];
		this.videoModifiesPalette = (md.videoFlags & GfxManager.VIDEO_MODIFIES_PALETTE) != 0;
		if ((md.videoFlags & VideoEmulator.VIDEO_BUFFERS_SPRITERAM) != 0)
			this.buffered_spriteram = new int[spriteram_size];
		
		this.vX =  - visible[0];
		this.vY =  - visible[2];

		switch (rot) {
			case GfxManager.ROT0:
				vX =  - (visible[0]);
				vY =  - (visible[2]);
			case GfxManager.ROT180:
				backBuffer = new BitMapImpl(((visible[1]+1) - visible[0]),((visible[3]+1) - visible[2]), pixels);
				break;
			case GfxManager.ROT90:
				vX =  - (visible[0]);
				vY =  - (visible[2]);
			case GfxManager.ROT270:
				backBuffer = new BitMapImpl(((visible[3]+1) - visible[2]),((visible[1]+1) - visible[0]), pixels);
				break;
		}

		bitmap = backBuffer;
		tmpbitmap = new BitMapImpl(backBuffer.getWidth(), backBuffer.getHeight());

		try {
			this.gfxMan = new GfxManager[gdi.length];
			this.color_table = new int[gdi.length][];
			for (int i = 0; i < gdi.length; i++) {
				color_table[i] = new int[gdi[i].numberOfColors << gdi[i].gfx.planes];
				// Set up a default lookup table
				for (int n = 0; n < color_table[i].length; n++) {
					color_table[i][n] = n + gdi[i].colorOffset;
				}
				gfxMan[i] = new GfxManager();
				gfxMan[i].init(gdi[i], palette, color_table[i], 0, rot, md.videoFlags);
			}
		} catch (Exception e) {}
	}

	public void palette_set_color(int c, int r, int g, int b) {
		//System.out.println(c + " = rgb " + r + "," + g + "," + b);
		int rgb =  r<<16 | g<<8 | b;
		if (palette_get_color(c) != rgb) palette_set_color(c, rgb);
	}
	
	public void palette_set_color(int c, int rgb) {
		palette[c] = rgb;
		if (videoModifiesPalette) {
			for (int i = 0; i < gfxMan.length; i++) {
				gfxMan[i].changePalette(c, rgb);
			}
		}		
	}
	
	public void palette_change_color(int c, int r, int g, int b) {
		palette_set_color(c,r,g,b);
	}
	
	public int palette_get_color(int c) {
		return palette[c];
	}

	public void palette_init() {
		//System.out.println("No color proms to decode...");
	}

	public void eof_callback() {
		//
	}

	public int vh_start() {
		return 0;
	}

	public void vh_stop() {
		//
	}

	public BitMap video_update() {
		return bitmap;
	}

	public void video_post_update() {
		if (videoModifiesPalette) {
			for (int i = 0; i < gfxMan.length; i++) {
				gfxMan[i].refresh();
			}
		}
	}

/**
 * This method mimics MAME's TOTAL_COLORS macro.
 */
	protected int TOTAL_COLORS(int i) {
		return gdi[i].numberOfColors << gdi[i].gfx.planes;
	}

/**
 * This method mimics MAME's COLOR macro.
 */
	protected void COLOR(int p, int i, int col) {
		color_table[p][i] = col;
	}

/**
 * Returns the color granularity of a layer
 */
	protected int color_granularity(int i) {
		return 1 << gdi[i].gfx.planes;
	}

/**
 * This method mimics MAME's drawgfx function.
 */
	protected void drawgfx( BitMap target, int type, int tile, int color,
							boolean flipx, boolean flipy, int x, int y,
							int transparency, int transcolor ) {

		switch (rot) {
			case GfxManager.ROT0:
				gfxMan[type].drawTile(target, tile, color, flipx, flipy, x + vX, y + vY, transparency, transcolor); break;
			case GfxManager.ROT90:
				gfxMan[type].drawTile(target, tile, color, flipy, flipx, x + vX, y + vY, transparency, transcolor); break;
			case GfxManager.ROT180:
				gfxMan[type].drawTile(target, tile, color, flipx, flipy, x + vX, y + vY, transparency, transcolor); break;
			case GfxManager.ROT270:
				gfxMan[type].drawTile(target, tile, color, flipy, flipx, x + vX, y + vY, transparency, transcolor); break;
		}

		//if (y < 0) System.out.println(y + " + " + vY);
	}

	protected void drawgfx( BitMap target, int type, int tile, int color,
							int flipx, int flipy, int x, int y,
							int transparency, int transcolor ) {
		switch (rot) {
			case GfxManager.ROT0:
				gfxMan[type].drawTile(target, tile, color, (flipx!=0), (flipy!=0), x + vX, y + vY, transparency, transcolor); break;
			case GfxManager.ROT90:
				gfxMan[type].drawTile(target, tile, color, (flipy!=0), (flipx!=0), x + vX, y + vY, transparency, transcolor); break;
			case GfxManager.ROT180:
				gfxMan[type].drawTile(target, tile, color, (flipx!=0), (flipy!=0), x + vX, y + vY, transparency, transcolor); break;
			case GfxManager.ROT270:
				gfxMan[type].drawTile(target, tile, color, (flipy!=0), (flipx!=0), x + vX, y + vY, transparency, transcolor); break;
		}
	}

	protected void drawgfx( BitMap target, int type, int tile, int color,
							int flipx, int flipy, int x, int y,
							int area, int transparency, int transcolor ) {
		if (area == 1) {
			x += vX;
			y += vY;
		}
		switch (rot) {
			case GfxManager.ROT0:
				gfxMan[type].drawTile(target, tile, color, (flipx!=0), (flipy!=0), x, y, transparency, transcolor); break;
			case GfxManager.ROT90:
				gfxMan[type].drawTile(target, tile, color, (flipy!=0), (flipx!=0), x, y, transparency, transcolor); break;
			case GfxManager.ROT180:
				gfxMan[type].drawTile(target, tile, color, (flipx!=0), (flipy!=0), x, y, transparency, transcolor); break;
			case GfxManager.ROT270:
				gfxMan[type].drawTile(target, tile, color, (flipy!=0), (flipx!=0), x, y, transparency, transcolor); break;
		}
	}

	/**
	 * Draw a tile to the given graphics object
	 */
	/*public void drawTile(BitMap target, int tile, int color,
			boolean flipx, boolean flipy,
			int x, int y, int transparency, int transcolor) {

		switch(rot) {
		case GfxManager.ROT0: getTile(tile,color,transparency,transcolor).toBitMap(target, x, y, flipx, flipy, owTrans); break;
		case GfxManager.ROT90: getTile(tile,color,transparency,transcolor).toBitMap(target, (target.getWidth() - w - 1) - y, x, flipx, flipy, owTrans); break;
		case GfxManager.ROT180: getTile(tile,color,transparency,transcolor).toBitMap(target, (target.getWidth() - w - 1) - x, (target.getHeight() - h - 1) - y, flipx, flipy, owTrans); break;
		case GfxManager.ROT270: getTile(tile,color,transparency,transcolor).toBitMap(target, y, (target.getHeight() - h - 1) - x, flipx, flipy, owTrans); break;
		}
	}*/
	
	protected void plot_pixel(BitMap dest, int x, int y, int color) {
		dest.setPixelFast(x, y, color);
	}

	protected int read_pixel(BitMap bm, int x, int y) {
		return bm.getPixel(x,y);
	}

	protected void copybitmap(BitMap dest, BitMap src,
								int rows, int rowscroll, int cols, int colscroll,
								int area, int transparency, int transcolor) {
		src.toPixels(pixels);
	}

	protected int get_black_pen() {
		return 0;
	}

	protected int[] visible_area() {
		return visible;
	}

	protected void fillbitmap(BitMap dest, int rgb, int[] area) {
		try {
			for (int i = 0; ; i++) pixels[i] = rgb;
		} catch (Exception e) {};
	}
    protected void fillbitmap(BitMap dest, int rgb, int s) {
        try {
            for (int i = 0; ; i++) pixels[i] = rgb;
        } catch (Exception e) {};
    }

	public void buffer_spriteram_w(int offset, int data)
	{
		int ofs=spriteram;
		for(int i=0; i<spriteram_size; i++)
			buffered_spriteram[i] = RAM[ofs++];
	}

/**
 * This method mimics MAME's copyscrollbitmap function.
 */
	protected void copyscrollbitmap(BitMap dest, BitMap src,
									int rows, int rowscroll, int cols, int colscroll,
									int transparency, int transcolor) {

		if (rows == 1 && cols == 1) {
			int sw,sh;
			int w,h;
			/* XY scrolling playfield */
			if ( (rot & 1) == 0) { // VideoEmulator.ROT0 or VideoEmulator.ROT180
				sw = src.getWidth();
				sh = src.getHeight();
				rowscroll &= (sw - 1);
				colscroll &= (sh - 1);
				w = dest.getWidth();
				h = dest.getHeight();
			} else { // VideoEmulator.ROT90 or VideoEmulator.ROT270
				sh = src.getWidth();
				sw = src.getHeight();
				int r = rowscroll;
				int c = colscroll;
				colscroll = r & (sw - 1);
				rowscroll = c & (sh - 1);
				w = dest.getWidth();
				h = dest.getHeight();
			}


			if ( ((rowscroll + w) <= sw) && ((colscroll + h) <= sh)) {
				// no wrap
				src.toBitMap(dest,0,0,rowscroll,colscroll,w,h);
			} else if ( ((rowscroll + w) > sw) && ((colscroll + h) <= sh)) {
				// horizontal wrap
				src.toBitMap(dest,0,0,rowscroll,colscroll,sw - rowscroll,h);
				src.toBitMap(dest,sw - rowscroll,0,rowscroll + (sw - rowscroll),colscroll,w - (sw - rowscroll),h);
			} else if ( ((rowscroll + w) <= sw) && ((colscroll + h) > sh)) {
				// vertical wrap
				src.toBitMap(dest,0,0, rowscroll,colscroll, w,h-((colscroll+h)-sh));
				src.toBitMap(dest,0,h-((colscroll+h)-sh), rowscroll,0,w,(colscroll + h) - sh);
			} else {
				// horizontal/vertical wrap
				src.toBitMap(dest,sw-rowscroll,sh-colscroll, 0,0, (rowscroll+w)-sw,(colscroll+h)-sh);
				src.toBitMap(dest,0,sh-colscroll,rowscroll,  0,sw-rowscroll, (colscroll+h)-sh);
				src.toBitMap(dest,sw-rowscroll,0, 0,colscroll,(rowscroll+w)-sw, sh-colscroll);
				src.toBitMap(dest,0,0,rowscroll, colscroll, sw-rowscroll, sh-colscroll);
			}
		}
	}

	protected final void copyscrollbitmap(BitMap target, BitMap src, int rows, int[] scroll, int cols, int colscroll, int[] clip, int transparency, int transcolor) {
		if (cols == 0 && colscroll == 0) {

			int sw,sh,tw,th,rowheight;
			int tt;

			sw = src.getWidth();
			sh = src.getHeight();

			if ( (rot & 1) == 0) {
				if (transparency == GfxManager.TRANSPARENCY_COLOR)
					tt = transcolor;
				else
					tt = -1;

				tw = target.getWidth()  + (clip[0] << 1); // we assume now that the visible area is in the middle.
				th = target.getHeight()/* + (clip[2] << 1)*/; // we assume now that the visible area is in the middle.
				rowheight = sh / rows;

				int ydst,ysrc;
				ydst = 0;
				ysrc = clip[2];

				int i=0;
				if (clip[2] >= rowheight) {
					i = clip[2] / rowheight;
				}

				for (; i < rows; i++) {
//					if (y >= 0 && y < target.getHeight()) {

						int x = -scroll[i] + clip[0];
						if (x < 0) x += sw;
						if ((x + target.getWidth()) <= sw ) {
							// No wrap
							src.toBitMap(target, 0, ydst, x, ysrc, target.getWidth(), rowheight,tt);
						} else {
							src.toBitMap(target, 0, ydst, x, ysrc, sw - x, rowheight,tt);
							src.toBitMap(target, sw - x, ydst, 0, ysrc, target.getWidth() - (sw - x), rowheight,tt);
						}
//					}
					ysrc += rowheight;
					ydst += rowheight;
				}
			} else {
				tw = target.getWidth()  + (clip[2] << 1); // we assume now that the visible area is in the middle.
				th = target.getHeight() + (clip[0] << 1); // we assume now that the visible area is in the middle.
				rowheight = tw / rows;
				for (int i = 0; i < rows; i++) {
					int y  = (i * rowheight);
					int yt = y - (clip[2] << 1);
					if (yt >= 0 && yt < target.getWidth()) {
						int x = -scroll[i-2] + th; // don't know about that -2 part but it seems to work
						if (x < 0) x += sh;
						if ((x + target.getHeight()) <= sh ) {
							// No wrap
							src.toBitMap(target, yt, 0,      y, x, rowheight, target.getHeight());
						} else {
							src.toBitMap(target, yt, 0,      y, x, rowheight, sh - x);
							src.toBitMap(target, yt, sh - x, y, 0, rowheight, target.getHeight() - (sh - x));
						}
					}
				}

			}
		}
	}

	protected final void copyscrollbitmap(BitMap target, BitMap src, int cols, int colscroll, int rows, int[] scroll, int[] clip, int transparency, int transcolor) {
		if (cols == 0 && colscroll == 0) {

			int sw,sh,tw,th,rowheight;

			sw = src.getWidth();
			sh = src.getHeight();

			if ( (rot & 1) == 1) {
				tw = target.getWidth()  + (clip[0] << 1); // we assume now that the visible area is in the middle.
				th = target.getHeight()/* + (clip[2] << 1)*/; // we assume now that the visible area is in the middle.
				rowheight = th / rows;
				for (int i = 0; i < rows; i++) {
					int y = (i * rowheight)/* - clip[2]*/;
					if (y >= 0 && y < target.getHeight()) {
						int x = -scroll[i] + clip[2];
						if (x < 0) x += sw;
						if ((x + target.getWidth()) <= sw ) {
							// No wrap
							src.toBitMap(target, 0, y, x, y, target.getWidth(), rowheight);
						} else {
							src.toBitMap(target, 0, y, x, y, sw - x, rowheight);
							src.toBitMap(target, sw - x, y, 0, y, target.getWidth() - (sw - x), rowheight);
						}
					}
				}
			} else {
				tw = target.getWidth()  + (clip[2] << 1); // we assume now that the visible area is in the middle.
				th = target.getHeight() + (clip[0] << 1); // we assume now that the visible area is in the middle.
				rowheight = tw / rows;
				for (int i = 0; i < rows; i++) {
					int y  = (i * rowheight);
					int yt = y - (clip[2] << 1);
					if (yt >= 0 && yt < target.getWidth()) {
						int x = -scroll[i-2] + th; // don't know about that -2 part but it seems to work
						if (x < 0) x += sh;
						if ((x + target.getHeight()) <= sh ) {
							// No wrap
							src.toBitMap(target, yt, 0,      y, x, rowheight, target.getHeight());
						} else {
							src.toBitMap(target, yt, 0,      y, x, rowheight, sh - x);
							src.toBitMap(target, yt, sh - x, y, 0, rowheight, target.getHeight() - (sh - x));
						}
					}
				}
			}
		}
	}

}