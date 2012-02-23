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

import sun.audio.AudioData;
import sun.audio.AudioPlayer;
import sun.audio.ContinuousAudioDataStream;

/**
 * @author Erik Duijs
 * 
 *         LoFiSound.java
 */
public class LoFiSound {

	AudioPlayer ap;
	ContinuousAudioDataStream cads;
	AudioData ad;

	int[] linBuffer;
	byte[] muBuffer;

	public void init(int lb[], byte mb[]) {
		this.ap = AudioPlayer.player;
		this.linBuffer = lb;
		this.muBuffer = mb;
		this.ad = new AudioData(muBuffer);
		this.cads = new ContinuousAudioDataStream(ad);
		this.ap.start(cads);
	}

	/**
	 * Disable sound
	 */
	public void disable() {
		ap.stop(cads);
		cads = null;
		ad = null;
	}

	/**
	 * This should fix the hanging sound problem.
	 */
	@Override
	public void finalize() {
		disable();
		try {
			super.finalize();
		} catch (Throwable e) {
		}
	}

	/************************************************************************/
	/* linToMu : performs PCM to mu-law conversion */
	/* originally for Sparcstation 1. */
	/*                                                                      */
	/* Original C code : */
	/*                                                                      */
	/* Copyright 1989 by Rich Gopstein and Harris Corporation */
	/*                                                                      */
	/* Permission to use, copy, modify, and distribute this software */
	/* and its documentation for any purpose and without fee is */
	/* hereby granted, provided that the above copyright notice */
	/* appears in all copies and that both that copyright notice and */
	/* this permission notice appear in supporting documentation, and */
	/* that the name of Rich Gopstein and Harris Corporation not be */
	/* used in advertising or publicity pertaining to distribution */
	/* of the software without specific, written prior permission. */
	/* Rich Gopstein and Harris Corporation make no representations */
	/* about the suitability of this software for any purpose. It */
	/* provided "as is" without express or implied warranty. */
	/*                                                                      */
	/* Translated to Java by Neural Semantics sprl */
	/* http://www.neuralsemantics.com/ */
	/************************************************************************/
	protected static byte linToMu(int lin) {

		// Compensate for the lesser dynamics of Mu-Law.
		// (14bit compared to 16bit).
		lin /= 4;

		int mask;
		if (lin < 0) {
			lin = -lin;
			mask = 0x7F;
		} else {
			mask = 0xFF;
		}
		if (lin < 32)
			lin = 0xF0 | 15 - (lin / 2);
		else if (lin < 96)
			lin = 0xE0 | 15 - (lin - 32) / 4;
		else if (lin < 224)
			lin = 0xD0 | 15 - (lin - 96) / 8;
		else if (lin < 480)
			lin = 0xC0 | 15 - (lin - 224) / 16;
		else if (lin < 992)
			lin = 0xB0 | 15 - (lin - 480) / 32;
		else if (lin < 2016)
			lin = 0xA0 | 15 - (lin - 992) / 64;
		else if (lin < 4064)
			lin = 0x90 | 15 - (lin - 2016) / 128;
		else if (lin < 8160)
			lin = 0x80 | 15 - (lin - 4064) / 256;
		else
			lin = 0x80;
		return (byte) (mask & lin);
	}

	/**
	 * Converts 16 bit linear buffer to 8 bit mu-law buffer. Needed for
	 * sun.audio implementation.
	 */
	protected void convertBufferToMuLaw() {
		for (int i = 0; i < linBuffer.length; i++) {
			muBuffer[i] = linToMu(linBuffer[i]);
		}
	}
}