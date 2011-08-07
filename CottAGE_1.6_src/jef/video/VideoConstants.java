/*
 * Created on 5-jul-2005
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package jef.video;

/**
 * @author Erik Duijs
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public interface VideoConstants {
	public static final int ROT0 = 0;
	public static final int ROT90 = 1;
	public static final int ROT180 = 2;
	public static final int ROT270 = 3;
	public static final int FLIPX = 8;
	public static final int FLIPY = 16;

	public static final int TRANSPARENCY_NONE = -1;
	public static final int TRANSPARENCY_PEN = 1;
	public static final int TRANSPARENCY_COLOR = 2;

	public static final int VIDEO_TYPE_RASTER	= 1;
	public static final int VIDEO_SUPPORTS_DIRTY	= 2;
	public static final int VIDEO_MODIFIES_PALETTE	= 4;
	public static final int VIDEO_UPDATE_AFTER_VBLANK = 8;
	public static final int VIDEO_BUFFERS_SPRITERAM = 16;
	
}
