package jef.video;

/**
 * @author Erik Duijs
 */
public interface BitMap {

	/** Scale without filter */
	public static final int SCALE_MODE_NORMAL = 0;

	/** Scale with scanlines and filtering */
	public static final int SCALE_MODE_TV = 1;

	/** Scale with scale2x enhancer */
	public static final int SCALE_MODE_SCALE2X = 2;

	/**
	 * Sets the internal pixels array.
	 * 
	 * @param pixels
	 */
	public void setPixels(int[] pixels);

	/**
	 * Gets the internal pixels array.
	 * 
	 * @return int[]
	 */
	public int[] getPixels();

	/**
	 * Width of the BitMap.
	 * 
	 * @return int
	 */
	public int getWidth();

	/**
	 * Height of the BitMap.
	 * 
	 * @return int
	 */
	public int getHeight();

	/**
	 * Change one pixel.
	 * 
	 * @param x
	 * @param y
	 * @param c
	 */
	public void setPixel(int x, int y, int c);

	/**
	 * Set one pixel without doing bounds checking.
	 * 
	 * @param x
	 * @param y
	 * @param c
	 */
	public void setPixelFast(int x, int y, int c);

	/**
	 * Get one pixel.
	 * 
	 * @param x
	 * @param y
	 * @return int
	 */
	public int getPixel(int x, int y);

	/**
	 * Fast blitting to a pixels array, assuming they are equal in size.
	 * 
	 * @param target
	 */
	public void toPixels(int[] target);

	/**
	 * Blit to another bitmap.
	 * 
	 * @param bm
	 * @param x
	 * @param y
	 */
	public void toBitMap(BitMap bm, int x, int y);

	/**
	 * Blit to another bitmap with support for X/Y flipping.
	 * 
	 * @param bm
	 * @param x
	 * @param y
	 * @param fx
	 * @param fy
	 */
	public void toBitMap(BitMap bm, int x, int y, boolean fx, boolean fy);

	/**
	 * Blit to another bitmap with support for X/Y flipping and transparent
	 * layer.
	 * 
	 * @param bm
	 * @param x
	 * @param y
	 * @param fx
	 * @param fy
	 * @param overwriteTransparency
	 */
	public void toBitMap(BitMap bm, int x, int y, boolean fx, boolean fy,
			boolean overwriteTransparency);

	/**
	 * Blit to another bitmap.
	 * 
	 * @param bm
	 * @param x
	 * @param y
	 * @param sx
	 * @param sy
	 * @param sw
	 * @param sh
	 */
	public void toBitMap(BitMap bm, int x, int y, int sx, int sy, int sw, int sh);

	/**
	 * Blit to another bitmap.
	 * 
	 * @param bm
	 * @param x
	 * @param y
	 * @param sx
	 * @param sy
	 * @param sw
	 * @param sh
	 * @param transp
	 */
	public void toBitMap(BitMap bm, int x, int y, int sx, int sy, int sw,
			int sh, int transp);

	/**
	 * XY scrolling playfield. Destination BitMap is the virtual screen. This
	 * BitMap is the playfield.
	 * 
	 * @param dest
	 * @param xScroll
	 * @param yScroll
	 * @param transparency
	 * @param transcolor
	 */
	public void toBitMapScrollXY(BitMap dest, int xScroll, int yScroll,
			int transparency, int transcolor);

	/**
	 * Copy a row of this BitMap to another and wrap it if necessary.
	 */
	public void toBitMapScrollXRow(BitMap dest, int xScroll, int ySrc,
			int srcHeight, int yDst);

	/**
	 * Copy a column of this BitMap to another and wrap it if necessary.
	 */
	public void toBitMapScrollYCol(BitMap dest, int yScroll, int xSrc,
			int srcWidth, int xDst);

	/**
	 * Returns a scaled version of this BitMap rendered with the specified
	 * rendering mode.
	 * 
	 * @param scale
	 * @param mode
	 * @return BitMap
	 */
	public BitMap getScaledBitMap(int scale, int scaleMode);
}