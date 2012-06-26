package jef.video;

public class GfxDecodeInfo {

	/** memory with the gfx data */
	public int[] mem;

	public int offset;

	/** decode layout info */
	public GfxLayout gfx;

	public int colorOffset;

	public int numberOfColors;

	public boolean IS_FRAC(int offset) {
		return ((offset & 0x80000000) != 0);
	}

	public int FRAC_NUM(int offset) {
		return (offset >> 27) & 0x0F;
	}

	public int FRAC_DEN(int offset) {
		return (offset >> 23) & 0x0F;
	}

	public int FRAC_OFFSET(int offset) {
		return (offset & 0x007FFFFF);
	}

	public GfxDecodeInfo(int[] mem, int offset, GfxLayout gfx, int colorOffset,
			int numberOfColors) {
		this.mem = mem;
		this.offset = offset;
		this.gfx = gfx;
		this.colorOffset = colorOffset;
		this.numberOfColors = numberOfColors;
	}

	public GfxDecodeInfo(int[] mem, int offset, int[][] gfx, int colorOffset,
			int numberOfColors) {
		this.mem = mem;
		this.offset = offset;
		int num;
		int den;
		int ofs;

		if (IS_FRAC(gfx[2][0])) {
			num = FRAC_NUM(gfx[2][0]);
			den = FRAC_DEN(gfx[2][0]);
			gfx[2][0] = (8 * mem.length * num) / (den * gfx[7][0]);

			/*
			 * if (gfx[2][0]>512) {
			 * System.out.println("NUM="+num+" DEN="+den+" GFX="+gfx[2][0]);
			 * System.exit(0); }
			 */
		}

		for (int i = 0; i < gfx[4].length; i++) {
			if (IS_FRAC(gfx[4][i])) {
				num = FRAC_NUM(gfx[4][i]);
				den = FRAC_DEN(gfx[4][i]);
				ofs = FRAC_OFFSET(gfx[4][i]);
				gfx[4][i] = (8 * mem.length * num) / den + ofs;
			}
		}

		this.gfx = new GfxLayout(gfx[0][0], gfx[1][0], gfx[2][0], gfx[3][0],
				gfx[4], gfx[5], gfx[6], gfx[7][0]);
		this.colorOffset = colorOffset;
		this.numberOfColors = numberOfColors;
	}
}