/*
 * Created on 19-jul-2005
 * 
 * To change the template for this generated file go to Window - Preferences -
 * Java - Code Generation - Code and Comments
 */
package jef.util;

/**
 * @author Erik Duijs
 * 
 * To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Generation - Code and Comments
 */
public class Statistics {

	public static int frames_per_second;
	public static long pixels_per_second;
	public static long pixels_err_per_second;

	private static int frameNr;
	private static long frameTimeStart;
	private static long pixels, pixelsErr;
	
	public static void frame() {
		if (System.currentTimeMillis() - frameTimeStart > 1000) {
			frameTimeStart = System.currentTimeMillis();
			frames_per_second = frameNr;
			pixels_per_second = pixels;
			pixels_err_per_second = pixelsErr;
			//System.out.println("FPS:" + frameNr);
			frameNr = 0;
			pixels = 0;
			pixelsErr = 0;
		} else {
			frameNr++;
		}
	}
	
	public static void pixels(int number) {
		pixels += number;
	}
	public static void pixelsErr(int number) {
		pixelsErr += number;
	}
}
