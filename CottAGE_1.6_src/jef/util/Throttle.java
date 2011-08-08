/*

Java Emulation Framework

This library contains a framework for creating emulation software.

Copyright (C) 2002 Erik Duijs (erikduijs@yahoo.com)

Contributors:
- Julien Freilat
- Arnon Goncalves Cardoso
- S.C. Wong
- Romain Tisserand
- David Raingeard


This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

*/

package jef.util;

/**
 * Provides the speed throttling functionality based
 * on System.currentTimeMillis() and Thread.sleep().
 * Heavily based on code by Arnon Cardoso.
 * 
 * @author Erik Duijs, Arnon Goncalves Cardoso
 */
public class Throttle {

	/** throttle, frameskip constants **/
    static final int MAX_THROTTLE_STEP = 5;

	/** throttle, frameskip constants **/
    static final int MIN_THROTTLE_STEP = 1;

	/** throttle, frameskip constants **/
    static final int DEFAULT_THROTTLE_STEP = 1;

	/** throttle, frameskip constants **/
    static final int DEFAULT_TARGET_FPS = 60;

	/** throttle, frameskip constants **/
    static final int FRAMES_UNTIL_THROTTLE_RECALC = 40;

	/** throttle, frameskip constants **/
    static final int MAX_FRAME_SKIP = 5;

	/** throttle, frameskip constants **/
    static final float MAX_FPS_DEVIATION = 0.10f;

	/** throttle, frameskip constants **/
    static final boolean TRY_ALT_SKIP_CALC = true;

	/** Throttle is enabled by default */
    static boolean throttle;

	/** Auto Frame Skip */
    static boolean autoFS;

    /** Frames Per Second */
    static long fps;
    
    /** Sum Frames Per Second for measuring avg FPS */
    static long sumFPS = 0;
    
    /** Average FPS */
    static float avgFPS = 0f;

    /** Amount of sleep time in ms. */
    static long sleep;

	/** The FPS that needs to be throttled to */
    static int targetFPS;

    /** How fast the throttle changes sleep time */
    static int throttleStep;

    /** Minimum FPS */
    static int minFPS;

    /** Maximum FPS */
    static int maxFPS;

    /** Minimum sleeptime to have effect on the JVM */
    static long minimumSleep;

    /** Frame Skip */
    static int fskip;

    /** Amount of ms. of one frame */
    static int frameDuration;

    /** Time in ms. */
    static long t;

    /** Time in ms. */
    static long tempT;

    /** Framenumber relative to last recalc */
    static int frameNumber;
    
    /** Count recalc for measuring avg fps */
    static long recalcCount = 0;

    /** The thread to throttle */
    static Thread thread;

    /**
     * Initialize the throttle.
     */
    public static void init(int _fps, Thread _thread) {

		targetFPS = _fps;
		thread = _thread;
		throttle = true;
		autoFS = true;
		fskip = 0;
		throttleStep = DEFAULT_THROTTLE_STEP;
		frameNumber = 0;
		recalcCount = 0;
		sumFPS = 0;
		avgFPS = 0f;
		fps = 0;

        // This part is weird... Its intention is to set the minimumSleep variable, which is the
        // minimum amount of milliseconds that the throttle may slow down 1 frame.
        // Why? Because, depending on the VM and/or OS (not sure yet), there is a minimum value
        // for Thread.Sleep(ms) to have an effect.
        // When running M$ JRE on Win2k, this value is incredibly high (11 ms.),
        // Running SUN JRE on Win2k, it's better (5 ms.).
        // It would be logical to look for vm.vendor, but this is not allowed in an applet :o(
        // Now, I look for os.name (which is allowed) and make use of the fact that the M$ VM
        // thinks that Windows 2000 is NT, where Sun seems to be better at distinguishing between
        // M$'s OS-es (!).
        // This mega dirty hack works on win2k, but probably not on NT.
        // ....This whole throttling in java gives me a headache....
        String osname = System.getProperty("os.name");
        minimumSleep = osname.endsWith("NT") ? 11 : 5;
        sleep = minimumSleep;
        throttleStep = DEFAULT_THROTTLE_STEP;
        minFPS = targetFPS - (int)((float)(targetFPS * MAX_FPS_DEVIATION));
        if(minFPS == targetFPS) {
            minFPS = targetFPS - 1;
        }
        maxFPS = targetFPS + (int)((float)(targetFPS * MAX_FPS_DEVIATION));
        if(maxFPS == targetFPS) {
            maxFPS = targetFPS + 1;
        }

		frameDuration = 1000/targetFPS;
        fps = (long)targetFPS;
        t = System.currentTimeMillis();
	}
    
    public static void init(int _fps) {

		targetFPS = _fps;
		throttle = true;
		autoFS = true;
		fskip = 0;
		throttleStep = DEFAULT_THROTTLE_STEP;
		frameNumber = 0;
		recalcCount = 0;
		sumFPS = 0;
		avgFPS = 0f;
		fps = 0;

        // This part is weird... Its intention is to set the minimumSleep variable, which is the
        // minimum amount of milliseconds that the throttle may slow down 1 frame.
        // Why? Because, depending on the VM and/or OS (not sure yet), there is a minimum value
        // for Thread.Sleep(ms) to have an effect.
        // When running M$ JRE on Win2k, this value is incredibly high (11 ms.),
        // Running SUN JRE on Win2k, it's better (5 ms.).
        // It would be logical to look for vm.vendor, but this is not allowed in an applet :o(
        // Now, I look for os.name (which is allowed) and make use of the fact that the M$ VM
        // thinks that Windows 2000 is NT, where Sun seems to be better at distinguishing between
        // M$'s OS-es (!).
        // This mega dirty hack works on win2k, but probably not on NT.
        // ....This whole throttling in java gives me a headache....
        String osname = System.getProperty("os.name");
        minimumSleep = osname.endsWith("NT") ? 11 : 5;
        sleep = minimumSleep;
        throttleStep = DEFAULT_THROTTLE_STEP;
        minFPS = targetFPS - (int)((float)(targetFPS * MAX_FPS_DEVIATION));
        if(minFPS == targetFPS) {
            minFPS = targetFPS - 1;
        }
        maxFPS = targetFPS + (int)((float)(targetFPS * MAX_FPS_DEVIATION));
        if(maxFPS == targetFPS) {
            maxFPS = targetFPS + 1;
        }

		frameDuration = 1000/targetFPS;
        fps = (long)targetFPS;
        t = System.currentTimeMillis();
	}

	/**
	 * Call this method each frame.
	 * Here the actual throttling takes place.
	 */
	public static void throttle() {
		// Try slow down to the machine's original speed
		if(throttle & ((frameNumber % throttleStep) == 0)) {
			try {
				Thread.sleep( sleep );
			} catch(Exception e) {
			}
        }

        if (frameNumber < FRAMES_UNTIL_THROTTLE_RECALC) {
        	frameNumber++;
		} else {
			frameNumber = 0;
			recalcTiming();
		}
	}

	/**
	 * Get current sleep time in ms.
	 */
	public static long getSleep() {
		return sleep;
	}

	/**
	 * Returns true if a frame needs to be skipped.
	 */
	public static boolean skipFrame() {
		return !((fskip==0)||((frameNumber%(fskip+1))==0));
	}

	/**
	 * Enable/disable automatic frame skip.
	 */
	public static void enableAutoFrameSkip(boolean enable) {
		autoFS = enable;
	}

	/**
	 * Returns true if automatic frame skip is enabled.
	 */
	public static boolean isAutoFrameSkip() {
		return autoFS;
	}

	/**
	 * Enable throttle.
	 */
	public static void enable(boolean enable) {
		throttle = enable;
	}

	/**
	 * Returns false if throttling is not enabled.
	 */
	public static boolean isEnabled() {
		return throttle;
	}

	/**
	 * Set the amount of frames to skip.
	 */
	public static void setFrameSkip(int skip) {
		fskip = skip;
	}

	/**
	 * Get current amount of frames to be skipped.
	 */
	public static int getFrameSkip() {
		return fskip;
	}

	/**
	 * Get current FPS.
	 */
	public static int getFPS() {
		return (int)fps;
	}

	/**
	 * Get the target FPS.
	 * 
	 * @return int
	 */
	public static int getTargetFPS() {
		return targetFPS;
	}
	
	/**
	 * Get the average FPS
	 * 	 * @return float	 */
	public static float getAverageFPS() {
		return avgFPS;
	}

	/**
	 * Called after FRAMES_UNTIL_THROTTLE_RECALC is reached.
	 * Here the sleep time and auto frame skip is re-evaluated.
	 */
	private static void recalcTiming() {
		tempT = System.currentTimeMillis();
		fps = 1000 / ((tempT - t) / FRAMES_UNTIL_THROTTLE_RECALC);
		t = tempT;
		
		recalcCount = recalcCount + 1;
		sumFPS = sumFPS + fps;
		
		avgFPS = (float)( (int) ((sumFPS * 100) / recalcCount)) / 100;

		if(throttle) {
			if(fps < minFPS) {
				if(sleep > minimumSleep) {
					if(TRY_ALT_SKIP_CALC) {
						sleep-=(targetFPS-fps)/(targetFPS-minFPS);
					}
					else{
						sleep--;
					}
				}
				if(sleep <= minimumSleep) {
					throttleStep++;
					if(throttleStep > MAX_THROTTLE_STEP) {
						throttleStep = MAX_THROTTLE_STEP;
						if(autoFS) {
							throttleStep = MIN_THROTTLE_STEP;
							fskip++;
							if(fskip > MAX_FRAME_SKIP) {
								fskip = MAX_FRAME_SKIP;
								throttleStep = MAX_THROTTLE_STEP;
							}
						}
					} else {
						if(TRY_ALT_SKIP_CALC) {
							sleep += minimumSleep;
						}
						else {
							sleep = (1000 / targetFPS - 1);
						}
					}
				}
			} else if(fps > maxFPS) {
				if(TRY_ALT_SKIP_CALC) {
					sleep+=(fps-targetFPS)/(maxFPS-targetFPS);
				}
				else {
					sleep++;
				}
				if(sleep > (1000 / targetFPS - 1)) {
					throttleStep--;
					if(throttleStep < MIN_THROTTLE_STEP) {
						throttleStep = MIN_THROTTLE_STEP;
						if(autoFS) {
							throttleStep = MAX_THROTTLE_STEP;
							fskip--;
							if(fskip < 0) {
								fskip = 0;
								throttleStep = MIN_THROTTLE_STEP;
							}
						}
					} else {
						if(TRY_ALT_SKIP_CALC) {
							sleep -= minimumSleep;
							if(sleep < minimumSleep) {
								sleep = minimumSleep;
							}
						}
						else {
							sleep = minimumSleep;
						}
					}
				}
			}
		}
	}
}