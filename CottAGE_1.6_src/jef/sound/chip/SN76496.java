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
 * The SN76496 sound chip emulator is translated from the SN76496 emulator in MAME.
 */
public class SN76496 extends SoundChip implements SoundChipEmulator {

	private int SN76496_FREQ;

	private static final int MAX_OUTPUT = 0x7fff;
	private static final int STEP = 0x10000;

      /* noise feedback for periodic noise mode */

	/* noise feedback for white noise mode (verified on real SN76489 by John Kortink) */
	private static final int FB_WNOISE = 0x14002;	/* (16bits) bit16 = bit0(out) ^ bit2 ^ bit15 */

	/* noise feedback for periodic noise mode */
	//private static final int FB_PNOISE = 0x10000; /* 16bit rorate */
	private static final int FB_PNOISE = 0x08000;	/* 15bit rotate   /* JH 981127 - fixes Do Run Run */

	/*
	0x08000 is definitely wrong. The Master System conversion of Marble Madness
	uses periodic noise as a baseline. With a 15-bit rotate, the bassline is
	out of tune.
	The 16-bit rotate has been confirmed against a real PAL Sega Master System 2.
	Hope that helps the System E stuff, more news on the PSG as and when!
	*/

	/* noise generator start preset (for periodic noise) */
	private static final int NG_PRESET = 0x0f35;

	private int gain = 16;
//	private int Channel;
//	private int SampleRate;
	private int UpdateStep;
	private int[] VolTable = new int[16];	// volume table
	private int[] Register = new int[8];	// registers
	private int LastRegister;				// last register written
	private int[] Volume = new int[4];		// volume of voice 0-2 and noise
	private int RNG;						// noise generator
	private int NoiseFB;					// noise feedback mask
	private int[] Period = new int[4];
	private int[] Count = new int[4];
	private int[] Output = new int[4];
	int vol[] = new int[4];
	
//	private boolean changed = false;

/**
 * Constructor
 */
    public SN76496( int clockFreq ) {
		this.SN76496_FREQ = clockFreq;
    }

/**
 * Initialize the sound.
 */
	public void init(boolean useJavaxSound, int sampRate, int buflen, int fps) {

		super.init(useJavaxSound, sampRate, buflen, fps);
		int i;
		float out;

		/* the base clock for the tone generators is the chip clock divided by 16; */
		/* for the noise generator, it is clock / 256. */
		/* Here we calculate the number of steps which happen during one sample */
		/* at the given sample rate. No. of events = sample rate / (clock/16). */
		/* STEP is a multiplier used to turn the fraction into a fixed point */
		/* number. */
		UpdateStep = (int)((STEP * sampRate * 16) / (SN76496_FREQ / 16));

		for (i = 0;i < 4;i++) {
			Volume[i] = 0;
		}

		LastRegister = 0;
		for (i = 0;i < 8;i+=2) {
			Register[i] = 0;
			Register[i + 1] = 0x0f;	/* volume = 0 */
		}

		for (i = 0;i < 4;i++) {
			Output[i] = 0;
			Period[i] = Count[i] = UpdateStep;
		}

		RNG = NG_PRESET;
		Output[3] = RNG & 1;

		gain &= 0xff;

		/* increase max output basing on gain (0.2 dB per step) */
		out = (float)(MAX_OUTPUT / 3);
		while (gain-- > 0) {
			out = (float)(out * 1.023292992);	/* = (10 ^ (0.2/20)) */
		}

		/* build volume table (2dB per step) */
		for (i = 0;i < 15;i++) {
			/* limit volume to avoid clipping */
			if (out > (MAX_OUTPUT / 3)) {
				VolTable[i] = (int)(MAX_OUTPUT / 3);
			}
			else {
				VolTable[i] = (int)out;
			}

			out /= 1.258925412;	/* = 10 ^ (2/20) = 2dB */
		}
		VolTable[15] = 0;

	}

    public void writeBuffer() {
    	
			clearBuffer();
	
	
			int i;
			int length;
			int out;
			int left;
			int nextevent;
	
			length = super.getBufferLength();
			/* If the volume is 0, increase the counter */
			for (i = 0;i < 4;i++) {
				if (Volume[i] == 0) {
					/* note that I do count += length, NOT count = length + 1. You might think */
					/* it's the same since the volume is 0, but doing the latter could cause */
					/* interferencies when the program is rapidly modulating the volume. */
					if (Count[i] <= (length*STEP)) {
						Count[i] += length*STEP;
					}
				}
			}
			
			for (int b = 0; b < length ; b++) {
	
				/* vol[] keeps track of how long each square wave stays */
				/* in the 1 position during the sample period. */
				vol[0] = vol[1] = vol[2] = vol[3] = 0;
				
				for (i = 0; i < 3; i++) {
					if (Output[i] != 0) {
						vol[i] += Count[i];
					}
					Count[i] -= STEP;
					/* Period[i] is the half period of the square wave. Here, in each */
					/* loop I add Period[i] twice, so that at the end of the loop the */
					/* square wave is in the same status (0 or 1) it was at the start. */
					/* vol[i] is also incremented by Period[i], since the wave has been 1 */
					/* exactly half of the time, regardless of the initial position. */
					/* If we exit the loop in the middle, Output[i] has to be inverted */
					/* and vol[i] incremented only if the exit status of the square */
					/* wave is 1. */
					//System.out.println("*******WHILE");
					while (Count[i] <= 0) {
						Count[i] += Period[i];
						if (Count[i] > 0)
						{
							Output[i] ^= 1;
							if (Output[i] != 0) {
								vol[i] += Period[i];
							}
							break;
						}
						Count[i] += Period[i];
						vol[i] += Period[i];
					}
					if (Output[i] != 0) {
						vol[i] -= Count[i];
					}
				}
	
				left = STEP;
				//System.out.println("********DO");
				do {
					if (Count[3] < left) {
						nextevent = Count[3];
						//System.out.println("nextevent = " + nextevent + " , left = " + Integer.toHexString(left));
					} else {
						nextevent = left;
					}
					if (Output[3] != 0) {
						vol[3] += Count[3];
					}
					Count[3] -= nextevent;
					if (Count[3] <= 0) {
						if ((RNG & 1) == 1) {
							RNG ^= NoiseFB;
						}
						RNG >>= 1;
						Output[3] = RNG & 1;
						Count[3] += Period[3];
						if (Output[3] != 0) {
							vol[3] += Period[3];
						}
					}
					if (Output[3] != 0) {
						vol[3] -= Count[3];
					}
	
					left -= nextevent;
				} while (left > 0);
	
				out = vol[0] * Volume[0] + vol[1] * Volume[1] +
						vol[2] * Volume[2] + vol[3] * Volume[3];
	
				if (out > (MAX_OUTPUT * STEP)) {
					out = MAX_OUTPUT * STEP;
				}
				writeLinBuffer(b, (int) (out / STEP));
			}
	}


	public WriteHandler sn76496_command_w() { return new SN76496_command_w(); }
	public class SN76496_command_w implements WriteHandler {
		public void write(int address, int data) {
			command_w(data);
		}
	}

	public void command_w(int data) {
		if ((data & 0x80)!=0)
		{
			//System.out.println("SN76496 command_w:" + Integer.toHexString(data));
			int r = (data & 0x70) >> 4;
			int c = (int)(r/2);
			int n = 0;

			LastRegister = r;
			Register[r] = (Register[r] & 0x3f0) | (data & 0x0f);
			switch (r)
			{
				case 0:	/* tone 0 : frequency */
				case 2:	/* tone 1 : frequency */
				case 4:	/* tone 2 : frequency */
					Period[c] = UpdateStep * Register[r];
					if (Period[c] == 0) {
						Period[c] = UpdateStep;
					}
					if (r == 4) {
						/* update noise shift frequency */
						if ((Register[6] & 0x03) == 0x03) {
							Period[3] = 2 * Period[2];
						}
					}
					break;
				case 1:	/* tone 0 : volume */
				case 3:	/* tone 1 : volume */
				case 5:	/* tone 2 : volume */
				case 7:	/* noise  : volume */
					Volume[c] = VolTable[data & 0x0f];
					break;
				case 6:	/* noise  : frequency, mode */
					n = Register[6];
					NoiseFB = ( (n & 4) !=0 ) ? FB_WNOISE : FB_PNOISE;
					n &= 3;
					/* N/512,N/1024,N/2048,Tone #3 output */
					if (n==3) {
						Period[3] = 2 * Period[2];
					} else {
						Period[3] = UpdateStep << (5+n);
					}

					/* reset noise shifter */
					RNG = NG_PRESET;
					Output[3] = RNG & 1;
					break;
			}
		} else {
			int r = LastRegister;
			int c = (int)(r/2);

			switch (r)
			{
				case 0:	/* tone 0 : frequency */
				case 2:	/* tone 1 : frequency */
				case 4:	/* tone 2 : frequency */
					Register[r] = (Register[r] & 0x0f) | ((data & 0x3f) << 4);
					Period[c] = UpdateStep * Register[r];
					if (Period[c] == 0) {
						Period[c] = UpdateStep;
					}
					if (r == 4)
					{
						/* update noise shift frequency */
						if ((Register[6] & 0x03) == 0x03)
							Period[3] = 2 * Period[2];
					}
					break;
			}
		}
	}
}


