package jef.video;

public class GfxLayout {

	/** Width */
	public int w;
	/** Height */
	public int h;
	/** Number of tiles */
	public int total;
	/** Number planes */
	public int planes;
	/** bit plane offsets */
	public int offsPlane[];
	/** bit x pixel offsets */
	public int offsX[];
	/** bit y pixel offsets */
	public int offsY[];
	/** The graphic takes this amount of consecutive bytes */
	public int bytes;

	public GfxLayout(int w, int h, int total, int planes, int[] offsPlane,
			int[] offsX, int[] offsY, int bytes) {
		this.w = w;
		this.h = h;
		this.total = total;
		this.planes = planes;
		this.offsPlane = offsPlane;
		this.offsX = offsX;
		this.offsY = offsY;
		this.bytes = bytes;
	}
}