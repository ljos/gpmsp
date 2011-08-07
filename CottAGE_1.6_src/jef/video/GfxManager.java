package jef.video;

/**
 * This class manages a cache of tiles.
 */
public class GfxManager implements VideoConstants {

	
	protected int[] GFX;
	protected GfxDecodeInfo gdi;
	protected int w;
	protected int h;
	protected static final int bit[] = {1,2,4,8,16,32,64,128};

	protected int		tileTotal;
	protected int		tileColors;
	protected int		tileMameOff;
	protected int[]		palette;
	protected int[]		colorLookUp;		// color lookup table
	protected boolean[]	colorChanged;		// colorcode changed
	protected BitMap[]	tileImg;			// images of the tiles
	protected boolean[] valid;				// validity of the tiles
	protected int[]		tileColor;
	protected int		flip;
	protected int		rot;
	protected boolean	colorsChanged;
	protected int		offset;

	protected int		videoFlags;
	protected boolean	videoModifiesPalette;
	protected boolean	videoSupportsDirty;

	protected boolean	owTrans = false;	// overwrite transparency

	public final void init( GfxDecodeInfo gdi, int[] palette, int[] colorLookUp,
							int flip, int rot, int videoFlags) {

		this.videoFlags		= videoFlags;
		this.videoModifiesPalette = (videoFlags & VIDEO_MODIFIES_PALETTE) != 0;
		this.videoSupportsDirty = (videoFlags & VIDEO_SUPPORTS_DIRTY) != 0;

		this.gdi			= gdi;
		this.tileTotal		= gdi.gfx.total;
		this.tileImg		= new BitMap[tileTotal];
		this.tileColor 		= new int[tileTotal];
		this.offset			= gdi.offset;
		this.colorLookUp	= colorLookUp;
		this.tileColors		= 1 << gdi.gfx.planes;
		this.palette		= palette;
		this.colorChanged	= new boolean[colorLookUp.length];
		this.valid			= new boolean[gdi.gfx.total];
		this.flip			= flip;
		this.rot			= rot;
		this.GFX			= gdi.mem;
		this.w				= gdi.gfx.w -1;
		this.h				= gdi.gfx.h -1;

		for(int n=0; n<=w; n++) {	// calculate tileMameOff (which is a hack BTW)	FIX THIS
			if((gdi.gfx.offsX[n]&7) > tileMameOff) { tileMameOff = gdi.gfx.offsX[n]&7; }
		}
	}
	
	public void setTransparencyOverwrite(boolean b) {
		this.owTrans = b;
	}
	
	public boolean getTransparencyOverwrite() {
		return this.owTrans;
	}

/**
 * Returns a tile image
 */
	public BitMap getTile(int tile, int color, int trans, int transcol) {
		tile = tile % tileTotal;

		if (tileColor[tile] != color) {
			tileColor[tile] = color;
			valid[tile] = false;
		}

		try {
			if (!valid[tile] || colorChanged[color]) {
				if (tileImg[tile] == null) {
					tileImg[tile] = new BitMapImpl(this.w+1, this.h+1);
				}
				tileImg[tile] = decode(tile, color, tileImg[tile], this.rot, this.flip, trans, transcol);
				valid[tile] = true;
			}
		} catch (Exception e) {
			System.out.println("tile " + tile + " , color " + color);
		}
		return tileImg[tile];
	}

	/**
	 * Draw a tile to the given graphics object
	 */
	public void drawTile(BitMap target, int tile, int color,
			boolean flipx, boolean flipy,
			int x, int y, int transparency, int transcolor) {

		switch(rot) {
			case GfxManager.ROT0: getTile(tile,color,transparency,transcolor).toBitMap(target, x, y, flipx, flipy, owTrans); break;
			case GfxManager.ROT90: getTile(tile,color,transparency,transcolor).toBitMap(target, (target.getWidth() - w - 1) - y, x, flipx, flipy, owTrans); break;
			case GfxManager.ROT180: getTile(tile,color,transparency,transcolor).toBitMap(target, (target.getWidth() - w - 1) - x, (target.getHeight() - h - 1) - y, flipx, flipy, owTrans); break;
			case GfxManager.ROT270: getTile(tile,color,transparency,transcolor).toBitMap(target, y, (target.getHeight() - h - 1) - x, flipx, flipy, owTrans); break;
		}
	}


/**
 * Notify me if something changed the color table.
 * This marks the correspondent colorcode as changed.
 */
	public void changeColorLookUp(int color, int argb) {
		if (colorLookUp[color] != argb) {
			colorLookUp[color] = argb;
			colorChanged[color >> gdi.gfx.planes] = true;
			colorsChanged = true;
		}
	}

/**
 * Notify me if something changed the palette.
 * This marks the correspondent colorcode as changed.
 */
	public void changePalette(int paletteIndex, int argb) {
		palette[paletteIndex] = argb;
		try {
			colorChanged[(paletteIndex - gdi.colorOffset) >> gdi.gfx.planes] = true;  // TO DO: this is not
															  						  // correct if the machine
															  						  // has a color table too.
			colorsChanged = true;
		} catch (Exception e) {
			// The color that is changed is not used by the layer that is using this
			// GfxManager. We don't check, we just catch.
		}
	}

/**
 * Returns true if the given colorcode has changed.
 * Can be handy to update the dirty rectangles buffer.
 */
	public boolean colorCodeHasChanged(int color) {
		return this.colorChanged[color];
	}

/**
 * Resets all 'changed' flags.
 * Call refresh() at last in a frame.
 */
	public boolean refresh() {
		if (colorsChanged) {
			for (int c=0; c<colorChanged.length; c++) {
				if (colorChanged[c]) {
					colorChanged[c] = false;
					for (int n = 0; n < gdi.gfx.total; n++) {
						valid[n] = (tileColor[n] == c) && (tileImg[n] != null) ;
					}
				}
			}
			colorsChanged = false;
			return true;
		}
		return false;
	}

/**
 * Signals the GfxManager to let it re-render all tiles.
 */
 	public boolean flush() {
		for (int c=0; c<colorChanged.length; c++) {
			this.colorChanged[c] = true;
		}
		colorsChanged = true;
		return true;
	}

/**
 * Decode a tile
 */
	protected final BitMap decode(int tile, int col, BitMap tileImg, int rot, int flip, int trans, int transcol) {
		int byteOffset, bitOffset, realXOffset;
		int colorGroup = col*tileColors;
		int charAddress= offset + (tile  % gdi.gfx.total) * (gdi.gfx.bytes >> 3);
		int indexSelect = (rot<<2)|flip;
		int colorIndex = 0;
		int index = 0;
		int cc = 0;

		for (int y=0; y<=h; y++) {
			for (int x=0; x<=w; x++) {
				byteOffset= (gdi.gfx.offsX[x])&0xfffff8;	// offset within the character in bytes of this pixel
				bitOffset = (gdi.gfx.offsX[x])&7;	// offset within the character in bits of this pixel
				realXOffset=byteOffset+(tileMameOff-bitOffset);
				colorIndex=0;

				try {
					for (int p=0; p < gdi.gfx.planes; p++) {	// This loop assembles the colorindex
						byteOffset = (realXOffset + gdi.gfx.offsY[y] + gdi.gfx.offsPlane[p]) / 8;
						bitOffset  = (realXOffset + gdi.gfx.offsY[y] + gdi.gfx.offsPlane[p]) & 7;
						if ( (GFX[charAddress+byteOffset] & bit[bitOffset]) != 0) {
							colorIndex |= 1<<p;		// if the read bit is set, set the correct bit in the colorindex
						}
					}

					cc = colorLookUp[colorGroup + colorIndex];

					if (trans == TRANSPARENCY_NONE) {
						cc = palette[cc];
					} else if ((trans == TRANSPARENCY_PEN && colorIndex == transcol) || (trans == TRANSPARENCY_COLOR && cc == transcol)) {
						cc = -1;
					} else {
						cc = palette[cc];
					}

					switch(indexSelect) {
						case 0:  tileImg.setPixelFast(x, y, cc); break;
						case 4:  tileImg.setPixelFast(h - y, x, cc); break;
						case 8:  tileImg.setPixelFast(w - x, h - y, cc); break;
						case 12: tileImg.setPixelFast(y, w - x, cc); break;
					}
				} catch (Exception e) {}
			}
		}
		return tileImg;
	}
}