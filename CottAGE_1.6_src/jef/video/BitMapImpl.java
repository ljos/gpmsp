package jef.video;

/**
 * @author Erik Duijs
 * 
 *         BitMap represents a bit mapped graphic.
 */
public final class BitMapImpl implements BitMap {
	BitMapImpl scaled;
	int[] pixels;
	final int w;
	final int h;
	final int w1;
	final int h1;

	/**
	 * Create an opaque BitMap of a given size.
	 * 
	 * @param w
	 * @param h
	 */
	public BitMapImpl(int w, int h) {
		this.w = w;
		this.h = h;
		this.w1 = w - 1;
		this.h1 = h - 1;
		pixels = new int[w * h];
	}

	/**
	 * Create a BitMap of a given size with the option of transparency.
	 * 
	 * @param w
	 * @param h
	 * @param transparent
	 */
	public BitMapImpl(int w, int h, boolean transparent) {
		this.w = w;
		this.h = h;
		this.w1 = w - 1;
		this.h1 = h - 1;
		pixels = new int[w * h];
		if (transparent) {
			for (int i = 0; i < pixels.length; i++) {
				pixels[i] = -1;
			}
		}
	}

	/**
	 * Create a BitMap with the pixels in the array passed as argument.
	 * 
	 * @param w
	 * @param h
	 * @param pixels
	 */
	public BitMapImpl(int w, int h, int[] pixels) {
		this.w = w;
		this.h = h;
		this.w1 = w - 1;
		this.h1 = h - 1;
		this.pixels = pixels;
	}

	/**
	 * Sets the internal pixels array.
	 * 
	 * @param pixels
	 */
	@Override
	public final void setPixels(int[] pixels) {
		this.pixels = pixels;
	}

	/**
	 * Gets the internal pixels array.
	 * 
	 * @return int[]
	 */
	@Override
	public final int[] getPixels() {
		return pixels;
	}

	/**
	 * Width of the BitMap.
	 * 
	 * @return int
	 */
	@Override
	public final int getWidth() {
		return w;
	}

	/**
	 * Height of the BitMap.
	 * 
	 * @return int
	 */
	@Override
	public final int getHeight() {
		return h;
	}

	/**
	 * Change one pixel.
	 * 
	 * @param x
	 * @param y
	 * @param c
	 */
	@Override
	public final void setPixel(int x, int y, int c) {
		if (x >= 0 && x < w && y >= 0 && y < h) {
			pixels[x + y * w] = c;
			// Statistics.pixels(1);
		} else {
			// Statistics.pixelsErr(1);
		}
	}

	/**
	 * Set one pixel without doing bounds checking.
	 * 
	 * @param x
	 * @param y
	 * @param c
	 */
	@Override
	public final void setPixelFast(int x, int y, int c) {
		pixels[x + y * w] = c;
		// Statistics.pixels(1);
	}

	/**
	 * Get one pixel.
	 * 
	 * @param x
	 * @param y
	 * @return int
	 */
	@Override
	public final int getPixel(int x, int y) {
		if (x >= 0 && x < w && y >= 0 && y < h)
			return pixels[x + y * w];
		else
			return -1;
	}

	/**
	 * Fast blitting to a pixels array, assuming they are equal in size.
	 * 
	 * @param target
	 */
	@Override
	public final void toPixels(int[] target) {
		System.arraycopy(pixels, 0, target, 0, target.length);
		// Statistics.pixels(target.length);
	}

	/**
	 * Blit to another bitmap.
	 * 
	 * @param bm
	 * @param x
	 * @param y
	 */
	@Override
	public final void toBitMap(BitMap bm, int x, int y) {

		int c;
		int ofs;

		ofs = 0;
		for (int iy = 0; iy < h; iy++) {
			for (int ix = 0; ix < w; ix++) {
				c = pixels[ofs++];
				if (c >= 0)
					bm.setPixel(x + ix, y, c);
			}
			y++;
		}
	}

	/**
	 * Blit to another bitmap with support for X/Y flipping.
	 * 
	 * @param bm
	 * @param x
	 * @param y
	 * @param fx
	 * @param fy
	 */
	@Override
	public final void toBitMap(BitMap bm, int x, int y, boolean fx, boolean fy) {

		int c, iix, iiy;

		for (int iy = 0; iy < h; iy++) {
			iiy = fy ? (h1 - iy) * w : iy * w;
			for (int ix = 0; ix < w; ix++) {
				iix = fx ? w1 - ix : ix;
				c = pixels[iiy + iix];
				if (c >= 0)
					bm.setPixel(x + ix, y, c);
			}
			y++;
		}
	}

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
	@Override
	public final void toBitMap(BitMap bm, int x, int y, boolean fx, boolean fy,
			boolean overwriteTransparency) {

		int c, iix, iiy;

		int maxx = w;
		if ((maxx + x) > bm.getWidth()) {
			maxx = bm.getWidth() - x;
		}
		int maxy = h;
		if ((maxy + y) > bm.getHeight()) {
			maxy = bm.getHeight() - y;
		}

		int yd = y;

		if (overwriteTransparency) {
			for (int iy = 0; iy < maxy; iy++) {
				iiy = fy ? (h1 - iy) * w : iy * w;
				for (int ix = 0; ix < maxx; ix++) {
					iix = fx ? w1 - ix : ix;
					c = pixels[iiy + iix];
					bm.setPixel(x + ix, yd, c);
				}
				yd++;
			}
		} else {
			for (int iy = 0; iy < maxy; iy++) {
				iiy = fy ? (h1 - iy) * w : iy * w;
				for (int ix = 0; ix < maxx; ix++) {
					iix = fx ? w1 - ix : ix;
					c = pixels[iiy + iix];
					if (c >= 0)
						bm.setPixel(x + ix, yd, c);
				}
				yd++;
			}
		}
	}

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
	@Override
	public final void toBitMap(BitMap bm, int x, int y, int sx, int sy, int sw,
			int sh) {

		int c;
		int l = pixels.length;
		int i = (sy * w) + sx;

		for (int iy = 0; iy < sh; iy++) {
			int ii = i;
			for (int ix = 0; ix < sw; ix++) {
				if (ii >= 0 && ii < l) {
					c = pixels[ii++];
					if (c >= 0)
						bm.setPixel(x + ix, y + iy, c);
				}
			}
			i += w;
		}
	}

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
	@Override
	public final void toBitMap(BitMap bm, int x, int y, int sx, int sy, int sw,
			int sh, int transp) {

		int c;
		int l = pixels.length;
		int i = (sy * w) + sx;

		if (transp == -1) {

			for (int iy = 0; iy < sh; iy++) {
				int ii = i;
				for (int ix = 0; ix < sw; ix++) {
					if (ii >= 0 && ii < l) {
						c = pixels[ii++];
						if (c >= 0)
							bm.setPixel(x + ix, y + iy, c);
					}
				}
				i += w;
			}

		} else {

			for (int iy = 0; iy < sh; iy++) {
				int ii = i;
				for (int ix = 0; ix < sw; ix++) {
					if (ii >= 0 && ii < l) {
						c = pixels[ii++];
						if (c != transp)
							bm.setPixel(x + ix, y + iy, c);
					}
				}
				i += w;
			}

		}
	}

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
	@Override
	public final void toBitMapScrollXY(BitMap dest, int xScroll, int yScroll,
			int transparency, int transcolor) {

		int sw = this.w;
		int sh = this.h;
		int dw = dest.getWidth();
		int dh = dest.getHeight();

		xScroll %= sw;
		yScroll %= sh;

		if (((xScroll + dw) <= sw) && ((yScroll + dh) <= sh)) {
			// no wrap
			this.toBitMap(dest, 0, 0, xScroll, yScroll, dw, dh);
		} else if (((xScroll + dw) > sw) && ((yScroll + dh) <= sh)) {
			// horizontal wrap
			this.toBitMap(dest, 0, 0, xScroll, yScroll, sw - xScroll, dh);
			this.toBitMap(dest, sw - xScroll, 0, xScroll + (sw - xScroll),
					yScroll, dw - (sw - xScroll), dh);
		} else if (((xScroll + dw) <= sw) && ((yScroll + dh) > sh)) {
			// vertical wrap
			this.toBitMap(dest, 0, 0, xScroll, yScroll, dw, dh
					- ((yScroll + dh) - sh));
			this.toBitMap(dest, 0, dh - ((yScroll + dh) - sh), xScroll, 0, dw,
					(yScroll + dh) - sh);
		} else {
			// horizontal/vertical wrap
			this.toBitMap(dest, sw - xScroll, sh - yScroll, 0, 0,
					(xScroll + dw) - sw, (yScroll + dh) - sh);
			this.toBitMap(dest, 0, sh - yScroll, xScroll, 0, sw - xScroll,
					(yScroll + dh) - sh);
			this.toBitMap(dest, sw - xScroll, 0, 0, yScroll,
					(xScroll + w) - sw, sh - yScroll);
			this.toBitMap(dest, 0, 0, xScroll, yScroll, sw - xScroll, sh
					- yScroll);
		}
	}

	/**
	 * Copy a row of this BitMap to another and wrap it if necessary.
	 */
	@Override
	public final void toBitMapScrollXRow(BitMap dest, int xScroll, int ySrc,
			int srcHeight, int yDst) {
		// TO DO
	}

	/**
	 * Copy a column of this BitMap to another and wrap it if necessary.
	 */
	@Override
	public final void toBitMapScrollYCol(BitMap dest, int yScroll, int xSrc,
			int srcWidth, int xDst) {
		// TO DO
	}

	/**
	 * Returns a scaled version of this BitMap rendered with the specified
	 * rendering mode.
	 * 
	 * @param scale
	 * @param mode
	 * @return BitMap
	 */
	@Override
	public final BitMap getScaledBitMap(int scale, int renderMode) {
		// switch (scale) {
		// case 0:
		// System.err.println("ERROR getting scaled BitMap: scale factor 0.");
		// break;
		// case 1:
		// return this;
		// default:
		if (scaled == null)
			scaled = new BitMapImpl(w << 1, h << 1);
		processScaledImage(scale, SCALE_MODE_TV);
		// break;
		// }
		return scaled;
	}

	protected void processScaledImage(int scale, int mode) {
		int dx = w;
		int dy = h;
		int srcofs = 0;
		int dstofs = 0;
		int[] dpixels = scaled.getPixels();

		switch (mode) {
		case SCALE_MODE_NORMAL:
			break;

		case SCALE_MODE_TV: // TV Mode (scanlines + alpha-blending)
			for (int y = 0; y < dy; y++) {
				for (int x = 0; x < dx - 1; x++) {
					int p1 = pixels[srcofs];
					int p2 = blendColors(p1, pixels[srcofs + 1]);
					int i1 = dstofs + (x << 1);
					int i2 = i1 + scaled.w;
					dpixels[i1] = p1;
					dpixels[i1 + 1] = p1;
					dpixels[i2] = (p1 >> 1) & 0x7F7F7F;
					dpixels[i2 + 1] = (p2 >> 1) & 0x7F7F7F;
					srcofs++;
				}
				srcofs++;
				dstofs += (scaled.w << 1);
			}
			break;

		case SCALE_MODE_SCALE2X: // Scale2x algorithm (based on AdvanceMAME
									// Scale2x)
			srcofs = dx + 1;
			dstofs = (scaled.w << 1);

			int i1,
			i2;

			for (int y = 1; y < dy - 1; y++) {
				for (int x = 1; x < dx - 1; x++) {
					int E0, E1, E2, E3;
					int B, D, E, F, H;

					B = pixels[srcofs - dx];
					D = pixels[srcofs - 1];
					E = pixels[srcofs];
					F = pixels[srcofs + 1];
					H = pixels[srcofs + dx];
					E0 = D == B && B != F && D != H ? D : E;
					E1 = B == F && B != D && F != H ? F : E;
					E2 = D == H && D != B && H != F ? D : E;
					E3 = H == F && D != H && B != F ? F : E;

					i1 = dstofs + (x << 1);
					i2 = i1 + scaled.w;

					dpixels[i1] = E0;
					dpixels[i1 + 1] = E1;
					dpixels[i2] = E2;
					dpixels[i2 + 1] = E3;

					srcofs++;
				}
				srcofs += 2;
				dstofs += (scaled.w << 1);
			}
			break;
		}
	}

	protected final int blendColors(int col1, int col2) {
		int r1, g1, b1;
		int r2, g2, b2;

		b1 = col1 & 0xFF;
		g1 = (col1 >> 8) & 0xFF;
		r1 = col1 >> 16;

		b2 = col2 & 0xFF;
		g2 = (col2 >> 8) & 0xFF;
		r2 = col2 >> 16;

		return (((r1 + r2) >> 1) << 16) | (((g1 + g2) >> 1) << 8)
				| ((b1 + b2) >> 1);
	}

	protected final int dimColor(int col) {
		return (col >> 1) & 0x7F7F7F;
	}
}