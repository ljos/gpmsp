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

package jef.machine;

import jef.video.BitMap;

/**
 * @author Erik Duijs
 * 
 *         Machine.java
 * 
 *         A class implementing Machine is the main emulator class.
 */
public interface Machine {

	/**
	 * Initialze the Machine
	 */
	public void init(MachineDriver md);

	/**
	 * Do everything needed for 1 frame.
	 * 
	 * @param render
	 *            TODO
	 * @return TODO
	 */
	public BitMap refresh(boolean render);

	/**
	 * Returns a BitMap representing the emulated display.
	 */
	// public BitMap getDisplay();

	/**
	 * Reset the machine (hard or soft).
	 */
	public void reset(boolean hard);

	/**
	 * Handle key press events.
	 */
	public void keyPress(int keyCode);

	/**
	 * Handle key release events.
	 */
	public void keyRelease(int keyCode);

	public void writeInput(int data);

	/**
	 * Returns a property value
	 */
	public int getProperty(int property);

	/**
	 * Get the progress within a frame. 0.0 is the start of the frame, 1.1 the
	 * end.
	 * 
	 * @return the progress
	 */
	public double getProgress();

	public void setHighScoreSupported(boolean b);

	public boolean isHighScoreSupported();

	public void setHighScore(long score);

	public long getHighScore();

	public final int FPS = 0;
	public static final int ROT = 3;
	public static final int WIDTH = 1;
	public static final int HEIGHT = 2;
}