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


package jef.sound.chip;

import jef.sound.SoundChip;
import jef.sound.SoundChipEmulator;
import jef.map.WriteHandler;

/**
 * @author Erik Duijs
 * 
 * Namco.java
 * 
 * Namco is an emulator of the Namco Sound System chip.
 * It is based on the namco sound system emulator as found in MAME.
 */
public class Namco extends SoundChip implements SoundChipEmulator {

	static final int WAV_LENGTH = 32;

	class SoundChannel {
		int frequency;
		int volume;
		int wav;
	}

    float freqDivider;
    int clockFreq;
    int numChannels;
    int mem[];
    SoundChannel channel[];
	float pwavPointer[];

/** PROM wave data */
    int prom[];
/**
 * Constructor
 */
    public Namco( int mem[],
     			  int numChannels,
     			  int clockFreq,
     			  int prom[]) {
		this.prom = prom;
        this.mem = mem;
		this.numChannels = numChannels;
		this.channel = new SoundChannel[numChannels];
		this.clockFreq = clockFreq;
		this.pwavPointer = new float[numChannels];
		for (int c = 0; c < numChannels; c++) channel[c] = new SoundChannel();
    }

/**
 * Initialize the sound.
 */
	public void init(boolean useJavaxSound, int sampRate, int buflen, int fps) {
		this.freqDivider = clockFreq / sampRate;
		freqDivider = freqDivider * ((float)sampRate / 8000.0f);
		super.init(useJavaxSound, sampRate, buflen, fps);
	}

    public void writeBuffer() {

		clearBuffer();

		// First, generate a linear buffer
		for(int curChannel = 0; curChannel < numChannels; curChannel++) {

			int frq = channel[curChannel].frequency;
			int amp = channel[curChannel].volume << 8;
			int wav = channel[curChannel].wav;

			frq /= freqDivider;

			// update the buffer with data from the current channel
			if(frq > 0 && amp > 0) {
				float step = ((float)frq * (float)WAV_LENGTH) / super.getSampFreq();
				for(int i = 0; i < super.getBufferLength(); i++) {
					int pwav = (prom[WAV_LENGTH * wav + (int)pwavPointer[curChannel]] & 0x0f) - 8;
					writeLinBuffer(i, readLinBuffer(i) + pwav * amp);
					pwavPointer[curChannel] = (pwavPointer[curChannel] + step) % WAV_LENGTH;
				}
			}
		}

		/* Mixer */
		for(int j = 0; j < super.getBufferLength(); j++) {
			writeLinBuffer(j, readLinBuffer(j) / numChannels);
		}
	}


// The pengo interface
	public WriteHandler pengo_sound_w(int base) { return new Pengo_sound_w(base); }
	public class Pengo_sound_w implements WriteHandler {

		int base;

		public Pengo_sound_w(int base) {
			this.base = base;
		}

		public void write(int address, int value) {

			if (mem[address] != (value & 15)) {
				mem[address] = value & 15;

				for (int c = 0; c < numChannels; c++) {

					int addr = c * 5 + base;

					int freq = mem[0x14 + addr];
					freq = freq << 4 | mem[0x13 + addr];
					freq = freq << 4 | mem[0x12 + addr];
					freq = freq << 4 | mem[0x11 + addr];

					if(c == 0) // the 1st voice has extra frequency bits
						freq = freq << 4 | mem[0x10 + addr];
					else
						freq <<= 4;

					channel[c].frequency = freq;
					channel[c].volume = mem[0x15 + addr] & 15;
					channel[c].wav = mem[0x05 + addr] & 7;
				}
			}
		}
	}
}
