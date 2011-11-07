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

package jef.sound;

/**
 * @author Erik Duijs
 * 
 * This class represents the collection of emulated sound chips of an
 * emulated machine.
 * It is responsible for choosing the best sound implementation, initialization
 * and updating of all sound chip emulators.
 *
 * Written June 2002 by Erik Duijs as a component of JEF (Java Emulation Framework).
 */
public class SoundEmulation
{
	/** The soundchip emulators being used */
	SoundChipEmulator[] sc;

	/** True if the JRE supports javax.sound */
	boolean useJavaxSound;

	/** The sampling rate */
	int samplingRate;

	int bufferLength;


/**
 * Initialize the sound chip emulators and decide whether to use sun.audio (bad)
 * or javax.sound (better).
 *
 * @param sc[] 			The array of soundchips
 * @param playbackFreq 	The samplefreq being used for playback (will be always 8000 for sun.audio)
 */
	public void init(SoundChipEmulator sc[], int samplingRate, int bufferLength, int fps) {
		this.sc = sc;
		this.samplingRate = samplingRate;
		this.bufferLength = bufferLength;

		// Check if the JRE supports javax.sound (java 1.3 and above)
		this.useJavaxSound = !(System.getProperty("java.version").startsWith("1.0")
							|| System.getProperty("java.version").startsWith("1.1")
							|| System.getProperty("java.version").startsWith("1.2"));
		
		for (int i = 0; i < this.sc.length; i++) {
			this.sc[i].init(useJavaxSound, samplingRate, bufferLength, fps);
		}
	}

/**
 * Update audiostreams of all sound chip emulators
 * Update only when using sun.audio
 * When a javax.sound implementation is used, the
 * stream doesn't need to be updated here because
 * it runs in its own thread and is updated
 * constantly instead of once a frame
 */
	public void update() {
		if (sc == null /*|| useJavaxSound*/) return;
		for (int i = 0; i < this.sc.length; i++) {
			this.sc[i].update();
		}
	}

/**
 * Returns the playback sampling rate
 */
 	public int getSamplingRate() {
		return this.samplingRate;
	}
}