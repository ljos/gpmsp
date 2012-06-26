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
 * @author Erik Duijs
 * 
 *         Static class with configuration parameters.
 */
public class Config {

	/** Enable debugger */
	public static final boolean DEBUG_ENABLED = false;

	/** Sampling freq used for sound playback using javax.sound */
	public static int SOUND_SAMPLING_FREQ = 22050;

	/** Buffer size used for sound playback using javax.sound */
	public static int SOUND_BUFFER_SIZE = 4096;
}