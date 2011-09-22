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
import jef.map.ReadHandler;
import jef.map.WriteHandler;

/**
 * @author Erik Duijs
 * 
 * AY8910.java
 *
 * This code is translated from:
 *
 * ay8910.c
 * 
 * as found in MAME
 *
 * Emulation of the AY-3-8910 / YM2149 sound chip.
 *
 * Based on various code snippets by Ville Hallik, Michael Cuddy,
 * Tatsuyuki Satoh, Fabrice Frances, Nicola Salmoria.
 **/
public class AY8910 extends SoundChip implements SoundChipEmulator {

	static final int MAX_OUTPUT = 0x1fff;
	static final int MAX_8910 = 5;
	static final int STEP = 0x1000;

	int ay8910_index_ym;
	int num = 0;
	static int ym_num = 0;

	class AY8910Context {
		public int Channel;
		public int SampleRate;

		public int PortAread;
		public int PortBread;
		public int PortAwrite;
		public int PortBwrite;

		int register_latch;
		int[] Regs = new int[16];
		int lastEnable;
		int UpdateStep;
		int PeriodA,PeriodB,PeriodC,PeriodN,PeriodE;
		int CountA,CountB,CountC,CountN,CountE;
		int VolA,VolB,VolC,VolE;
		int EnvelopeA,EnvelopeB,EnvelopeC;
		int OutputA,OutputB,OutputC,OutputN;
		int CountEnv;
		int Hold,Alternate,Attack,Holding;
		int RNG;
		int[] VolTable = new int[32];
	};

	/* register id's */
	static final int  AY_AFINE	= 0;
	static final int  AY_ACOARSE	= 1;
	static final int  AY_BFINE	= 2;
	static final int  AY_BCOARSE	= 3;
	static final int  AY_CFINE	= 4;
	static final int  AY_CCOARSE	= 5;
	static final int  AY_NOISEPER = 6;
	static final int  AY_ENABLE	= 7;
	static final int  AY_AVOL		= 8;
	static final int  AY_BVOL		= 9;
	static final int  AY_CVOL		= 10;
	static final int  AY_EFINE	= 11;
	static final int  AY_ECOARSE	= 12;
	static final int  AY_ESHAPE	= 13;

	static final int  AY_PORTA	= 14;
	static final int  AY_PORTB	= 15;


	static AY8910Context[] ay = new AY8910Context[MAX_8910];		/* array of PSG's */

	static int	baseClock;

/**
 * Constructor
 */
	public AY8910(int numChips, int clock) {
		this.num = numChips;
		AY8910.baseClock = clock;
	}

/**
 * Initialize the sound.
 */
	@Override
	public void init(boolean useJavaxSound, int sampRate, int buflen, int fps) {
		super.init(useJavaxSound, sampRate, buflen, fps);

		for (int chip = 0; chip < num; chip++) {
			AY8910_init("AY-3-8910", chip, baseClock, 50, super.getSampFreq(), 0, 0, 0, 0);
			build_mixer_table(chip);
		}
	}

	public void _AYWriteReg(int n, int r, int v) {

		AY8910Context psg = ay[n];
		int old;


		psg.Regs[r] = v;

		/* A note about the period of tones, noise and envelope: for speed reasons,*/
		/* we count down from the period to 0, but careful studies of the chip     */
		/* output prove that it instead counts up from 0 until the counter becomes */
		/* greater or equal to the period. This is an important difference when the*/
		/* program is rapidly changing the period to modulate the sound.           */
		/* To compensate for the difference, when the period is changed we adjust  */
		/* our internal counter.                                                   */
		/* Also, note that period = 0 is the same as period = 1. This is mentioned */
		/* in the YM2203 data sheets. However, this does NOT apply to the Envelope */
		/* period. In that case, period = 0 is half as period = 1. */
		switch( r )
		{
		case AY_AFINE:
		case AY_ACOARSE:
			psg.Regs[AY_ACOARSE] &= 0x0f;
			old = psg.PeriodA;
			psg.PeriodA = (psg.Regs[AY_AFINE] + 256 * psg.Regs[AY_ACOARSE]) * psg.UpdateStep;
			if (psg.PeriodA == 0) psg.PeriodA = psg.UpdateStep;
			psg.CountA += psg.PeriodA - old;
			if (psg.CountA <= 0) psg.CountA = 1;
			break;
		case AY_BFINE:
		case AY_BCOARSE:
			psg.Regs[AY_BCOARSE] &= 0x0f;
			old = psg.PeriodB;
			psg.PeriodB = (psg.Regs[AY_BFINE] + 256 * psg.Regs[AY_BCOARSE]) * psg.UpdateStep;
			if (psg.PeriodB == 0) psg.PeriodB = psg.UpdateStep;
			psg.CountB += psg.PeriodB - old;
			if (psg.CountB <= 0) psg.CountB = 1;
			break;
		case AY_CFINE:
		case AY_CCOARSE:
			psg.Regs[AY_CCOARSE] &= 0x0f;
			old = psg.PeriodC;
			psg.PeriodC = (psg.Regs[AY_CFINE] + 256 * psg.Regs[AY_CCOARSE]) * psg.UpdateStep;
			if (psg.PeriodC == 0) psg.PeriodC = psg.UpdateStep;
			psg.CountC += psg.PeriodC - old;
			if (psg.CountC <= 0) psg.CountC = 1;
			break;
		case AY_NOISEPER:
			psg.Regs[AY_NOISEPER] &= 0x1f;
			old = psg.PeriodN;
			psg.PeriodN = psg.Regs[AY_NOISEPER] * psg.UpdateStep;
			if (psg.PeriodN == 0) psg.PeriodN = psg.UpdateStep;
			psg.CountN += psg.PeriodN - old;
			if (psg.CountN <= 0) psg.CountN = 1;
			break;
		case AY_ENABLE:
			if ((psg.lastEnable == -1) ||
				((psg.lastEnable & 0x40) != (psg.Regs[AY_ENABLE] & 0x40)))
			{
		//**************************************************
				/* write out 0xff if port set to input */
				//if (psg.PortAwrite != null)
				//	psg.PortAwrite(0, (psg.Regs[AY_ENABLE] & 0x40) ? psg.Regs[AY_PORTA] : 0xff);
		//**************************************************
			}

			if ((psg.lastEnable == -1) ||
				((psg.lastEnable & 0x80) != (psg.Regs[AY_ENABLE] & 0x80)))
			{
		//**************************************************
				/* write out 0xff if port set to input */
				//if (psg.PortBwrite != null)
				//	(*psg.PortBwrite)(0, (psg.Regs[AY_ENABLE] & 0x80) ? psg.Regs[AY_PORTB] : 0xff);
		//**************************************************
			}

			psg.lastEnable = psg.Regs[AY_ENABLE];
			break;
		case AY_AVOL:
			psg.Regs[AY_AVOL] &= 0x1f;
			psg.EnvelopeA = psg.Regs[AY_AVOL] & 0x10;
			psg.VolA = psg.EnvelopeA != 0 ? psg.VolE : psg.VolTable[psg.Regs[AY_AVOL] != 0 ? psg.Regs[AY_AVOL]*2+1 : 0];
			break;
		case AY_BVOL:
			psg.Regs[AY_BVOL] &= 0x1f;
			psg.EnvelopeB = psg.Regs[AY_BVOL] & 0x10;
			psg.VolB = psg.EnvelopeB!= 0 ? psg.VolE : psg.VolTable[psg.Regs[AY_BVOL] != 0 ? psg.Regs[AY_BVOL]*2+1 : 0];
			break;
		case AY_CVOL:
			psg.Regs[AY_CVOL] &= 0x1f;
			psg.EnvelopeC = psg.Regs[AY_CVOL] & 0x10;
			psg.VolC = psg.EnvelopeC!= 0 ? psg.VolE : psg.VolTable[psg.Regs[AY_CVOL] != 0 ? psg.Regs[AY_CVOL]*2+1 : 0];
			break;
		case AY_EFINE:
		case AY_ECOARSE:
			old = psg.PeriodE;
			psg.PeriodE = ((psg.Regs[AY_EFINE] + 256 * psg.Regs[AY_ECOARSE])) * psg.UpdateStep;
			if (psg.PeriodE == 0) psg.PeriodE = psg.UpdateStep / 2;
			psg.CountE += psg.PeriodE - old;
			if (psg.CountE <= 0) psg.CountE = 1;
			break;
		case AY_ESHAPE:
			/* envelope shapes:
			C AtAlH
			0 0 x x  \___

			0 1 x x  /___

			1 0 0 0  \\\\

			1 0 0 1  \___

			1 0 1 0  \/\/
					  ___
			1 0 1 1  \

			1 1 0 0  ////
					  ___
			1 1 0 1  /

			1 1 1 0  /\/\

			1 1 1 1  /___

			The envelope counter on the AY-3-8910 has 16 steps. On the YM2149 it
			has twice the steps, happening twice as fast. Since the end result is
			just a smoother curve, we always use the YM2149 behaviour.
			*/
			psg.Regs[AY_ESHAPE] &= 0x0f;
			psg.Attack = ((psg.Regs[AY_ESHAPE] & 0x04) != 0) ? 0x1f : 0x00;
			if ((psg.Regs[AY_ESHAPE] & 0x08) == 0)
			{
				/* if Continue = 0, map the shape to the equivalent one which has Continue = 1 */
				psg.Hold = 1;
				psg.Alternate = psg.Attack;
			}
			else
			{
				psg.Hold = psg.Regs[AY_ESHAPE] & 0x01;
				psg.Alternate = psg.Regs[AY_ESHAPE] & 0x02;
			}
			psg.CountE = psg.PeriodE;
			psg.CountEnv = 0x1f;
			psg.Holding = 0;
			psg.VolE = psg.VolTable[psg.CountEnv ^ psg.Attack];
			if (psg.EnvelopeA != 0) psg.VolA = psg.VolE;
			if (psg.EnvelopeB != 0) psg.VolB = psg.VolE;
			if (psg.EnvelopeC != 0) psg.VolC = psg.VolE;
			break;
		case AY_PORTA:
			if ((psg.Regs[AY_ENABLE] & 0x40) != 0)
			{
				//if (psg.PortAwrite)
				//	(*psg.PortAwrite)(0, psg.Regs[AY_PORTA]);
				//else
					//logerror("PC %04x: warning - write %02x to 8910 #%d Port A\n",activecpu_get_pc(),psg.Regs[AY_PORTA],n);
			}
			//else
			//{
			//	logerror("warning: write to 8910 #%d Port A set as input - ignored\n",n);
			//}
			break;
		case AY_PORTB:
			if ((psg.Regs[AY_ENABLE] & 0x80) != 0)
			{
				//if (psg.PortBwrite)
				//	(*psg.PortBwrite)(0, psg.Regs[AY_PORTB]);
				//else
					//logerror("PC %04x: warning - write %02x to 8910 #%d Port B\n",activecpu_get_pc(),psg.Regs[AY_PORTB],n);
			}
			//else
			//{
				//logerror("warning: write to 8910 #%d Port B set as input - ignored\n",n);
			//}
			break;
		}
	}


	/* write a register on AY8910 chip number 'n' */
	public void AYWriteReg(int chip, int r, int v)
	{
		AY8910Context psg = ay[chip];


		if (r > 15) return;
		if (r < 14)
		{
			if (r == AY_ESHAPE || psg.Regs[r] != v)
			{
				/* update the output buffer before changing the register */
				//AY8910_sh_start(psg.Channel,0);
			}
		}

		_AYWriteReg(chip,r,v);
	}



	public int AYReadReg(int n, int r)
	{
		AY8910Context psg = ay[n];


		if (r > 15) return 0;

		switch (r)
		{
		case AY_PORTA:
			if ((psg.Regs[AY_ENABLE] & 0x40) != 0)
				System.out.println("warning: read from 8910 #" + n + " Port A set as output");
			else if (psg.PortAread != 0) psg.Regs[AY_PORTA] = psg.PortAread;
			//else logerror("PC %04x: warning - read 8910 #%d Port A\n",activecpu_get_pc(),n);
			break;
		case AY_PORTB:
			if ((psg.Regs[AY_ENABLE] & 0x80) != 0)
				System.out.println("warning: read from 8910 #" + n + " Port B set as output");
			else if (psg.PortBread != 0) psg.Regs[AY_PORTB] = psg.PortBread;
			//else logerror("PC %04x: warning - read 8910 #%d Port B\n",activecpu_get_pc(),n);
			break;
		}
		return psg.Regs[r];
	}


	public void AY8910Write(int chip,int a,int data)
	{
		AY8910Context psg = ay[chip];

		if ((a & 1) != 0)
		{	/* Data port */
			AYWriteReg(chip,psg.register_latch,data);
		}
		else
		{	/* Register port */
			psg.register_latch = data & 0x0f;
		}
	}

	public int AY8910Read(int chip)
	{
		AY8910Context psg = ay[chip];

		return AYReadReg(chip,psg.register_latch);
	}


    @Override
	public void writeBuffer() {

		clearBuffer();

		for (int chip = 0; chip < num; chip++) {
			AY8910Update(chip);
		}
	}

	public void AY8910Update(int chip)
	{
		AY8910Context psg = ay[chip];
		int pbuf1 = 0;
		int outn;

		int length = super.getBufferLength();


		/* The 8910 has three outputs, each output is the mix of one of the three */
		/* tone generators and of the (single) noise generator. The two are mixed */
		/* BEFORE going into the DAC. The formula to mix each channel is: */
		/* (ToneOn | ToneDisable) & (NoiseOn | NoiseDisable). */
		/* Note that this means that if both tone and noise are disabled, the output */
		/* is 1, not 0, and can be modulated changing the volume. */


		/* If the channels are disabled, set their output to 1, and increase the */
		/* counter, if necessary, so they will not be inverted during this update. */
		/* Setting the output to 1 is necessary because a disabled channel is locked */
		/* into the ON state (see above); and it has no effect if the volume is 0. */
		/* If the volume is 0, increase the counter, but don't touch the output. */
		if ((psg.Regs[AY_ENABLE] & 0x01) != 0)
		{
			if (psg.CountA <= length*STEP) psg.CountA += length*STEP;
			psg.OutputA = 1;
		}
		else if (psg.Regs[AY_AVOL] == 0)
		{
			/* note that I do count += length, NOT count = length + 1. You might think */
			/* it's the same since the volume is 0, but doing the latter could cause */
			/* interferencies when the program is rapidly modulating the volume. */
			if (psg.CountA <= length*STEP) psg.CountA += length*STEP;
		}
		if ((psg.Regs[AY_ENABLE] & 0x02) != 0)
		{
			if (psg.CountB <= length*STEP) psg.CountB += length*STEP;
			psg.OutputB = 1;
		}
		else if (psg.Regs[AY_BVOL] == 0)
		{
			if (psg.CountB <= length*STEP) psg.CountB += length*STEP;
		}
		if ((psg.Regs[AY_ENABLE] & 0x04) != 0)
		{
			if (psg.CountC <= length*STEP) psg.CountC += length*STEP;
			psg.OutputC = 1;
		}
		else if (psg.Regs[AY_CVOL] == 0)
		{
			if (psg.CountC <= length*STEP) psg.CountC += length*STEP;
		}

		/* for the noise channel we must not touch OutputN - it's also not necessary */
		/* since we use outn. */
		if ((psg.Regs[AY_ENABLE] & 0x38) == 0x38) {
			if (psg.CountN <= length*STEP) {
				psg.CountN += length*STEP;
			}
		}

		outn = (psg.OutputN | psg.Regs[AY_ENABLE]);

		//System.out.println("AYUpdate length:" + length + ", psg.CountN:" + psg.CountN);

		/* buffering loop */
		while (length != 0)
		{
			int vola,volb,volc;
			int left;


			/* vola, volb and volc keep track of how long each square wave stays */
			/* in the 1 position during the sample period. */
			vola = volb = volc = 0;

			left = STEP;
			do
			{
				int nextevent;


				if (psg.CountN < left) nextevent = psg.CountN;
				else nextevent = left;

				//System.out.println("nextevent:" +nextevent);
				if ((outn & 0x08) != 0)
				{
					if (psg.OutputA != 0) vola += psg.CountA;
					psg.CountA -= nextevent;
					/* PeriodA is the half period of the square wave. Here, in each */
					/* loop I add PeriodA twice, so that at the end of the loop the */
					/* square wave is in the same status (0 or 1) it was at the start. */
					/* vola is also incremented by PeriodA, since the wave has been 1 */
					/* exactly half of the time, regardless of the initial position. */
					/* If we exit the loop in the middle, OutputA has to be inverted */
					/* and vola incremented only if the exit status of the square */
					/* wave is 1. */
					//System.out.println("while (psg.CountA <= 0)");
					while (psg.CountA <= 0)
					{
						psg.CountA += psg.PeriodA;
						
						if (psg.CountA > 0)
						{
							psg.OutputA ^= 1;
							if (psg.OutputA != 0) vola += psg.PeriodA;
							break;
						}
						psg.CountA += psg.PeriodA;
						vola += psg.PeriodA;
					}
					if (psg.OutputA != 0) vola -= psg.CountA;
				}
				else
				{
					psg.CountA -= nextevent;
					//System.out.println("while (psg.CountA <= 0)");
					while (psg.CountA <= 0)
					{
						psg.CountA += psg.PeriodA;
						if (psg.CountA > 0)
						{
							psg.OutputA ^= 1;
							break;
						}
						psg.CountA += psg.PeriodA;
					}
				}

				if ((outn & 0x10) != 0)
				{
					if (psg.OutputB != 0) volb += psg.CountB;
					psg.CountB -= nextevent;
					//System.out.println("while (psg.CountB <= 0)");
					while (psg.CountB <= 0)
					{
						psg.CountB += psg.PeriodB;
						if (psg.CountB > 0)
						{
							psg.OutputB ^= 1;
							if (psg.OutputB != 0) volb += psg.PeriodB;
							break;
						}
						psg.CountB += psg.PeriodB;
						volb += psg.PeriodB;
					}
					if (psg.OutputB != 0) volb -= psg.CountB;
				}
				else
				{
					psg.CountB -= nextevent;
					//System.out.println("while (psg.CountB <= 0)");
					while (psg.CountB <= 0)
					{
						psg.CountB += psg.PeriodB;
						if (psg.CountB > 0)
						{
							psg.OutputB ^= 1;
							break;
						}
						psg.CountB += psg.PeriodB;
					}
				}

				if ((outn & 0x20) != 0)
				{
					if (psg.OutputC != 0) volc += psg.CountC;
					psg.CountC -= nextevent;
					//System.out.println("while (psg.CountC <= 0) 1");
					while (psg.CountC <= 0)
					{
						psg.CountC += psg.PeriodC;
						if (psg.CountC > 0)
						{
							psg.OutputC ^= 1;
							if (psg.OutputC != 0) volc += psg.PeriodC;
							break;
						}
						psg.CountC += psg.PeriodC;
						volc += psg.PeriodC;
					}
					if (psg.OutputC != 0) volc -= psg.CountC;
				}
				else
				{
					psg.CountC -= nextevent;
					//System.out.println("while (psg.CountC <= 0) 2 psg.PeriodC:" + psg.PeriodC);
					while (psg.CountC <= 0)
					{
						psg.CountC += psg.PeriodC;
						if (psg.CountC > 0)
						{
							psg.OutputC ^= 1;
							break;
						}
						psg.CountC += psg.PeriodC;
					}
				}

				psg.CountN -= nextevent;
				if (psg.CountN <= 0)
				{
					/* Is noise output going to change? */
					if (((psg.RNG + 1) & 2) != 0)	/* (bit0^bit1)? */
					{
						psg.OutputN = ~psg.OutputN;
						outn = (psg.OutputN | psg.Regs[AY_ENABLE]);
					}

					/* The Random Number Generator of the 8910 is a 17-bit shift */
					/* register. The input to the shift register is bit0 XOR bit2 */
					/* (bit0 is the output). */

					/* The following is a fast way to compute bit17 = bit0^bit2. */
					/* Instead of doing all the logic operations, we only check */
					/* bit0, relying on the fact that after two shifts of the */
					/* register, what now is bit2 will become bit0, and will */
					/* invert, if necessary, bit15, which previously was bit17. */
					if ((psg.RNG & 1) != 0) psg.RNG ^= 0x28000;
					psg.RNG >>= 1;
					psg.CountN += psg.PeriodN;
				}

				left -= nextevent;
			} while (left > 0);

			/* update envelope */
			if (psg.Holding == 0)
			{
				psg.CountE -= STEP;
				if (psg.CountE <= 0)
				{
					do
					{
						psg.CountEnv--;
						psg.CountE += psg.PeriodE;
					} while (psg.CountE <= 0);

					/* check envelope current position */
					if (psg.CountEnv < 0)
					{
						if (psg.Hold != 0)
						{
							if (psg.Alternate != 0)
								psg.Attack ^= 0x1f;
							psg.Holding = 1;
							psg.CountEnv = 0;
						}
						else
						{
							/* if CountEnv has looped an odd number of times (usually 1), */
							/* invert the output. */
							if (psg.Alternate != 0 && (psg.CountEnv & 0x20) != 0)
								psg.Attack ^= 0x1f;

							psg.CountEnv &= 0x1f;
						}
					}

					psg.VolE = psg.VolTable[psg.CountEnv ^ psg.Attack];
					/* reload volume */
					if (psg.EnvelopeA != 0) psg.VolA = psg.VolE;
					if (psg.EnvelopeB != 0) psg.VolB = psg.VolE;
					if (psg.EnvelopeC != 0) psg.VolC = psg.VolE;
				}
			}

			int a1 = (vola * psg.VolA) / STEP;
			int a2 = (volb * psg.VolB) / STEP;
			int a3 = (volc * psg.VolC) / STEP;

			int at = (a1 + a2 + a3);

			writeLinBuffer(pbuf1, readLinBuffer(pbuf1) + at);
			pbuf1++;

			length--;
		}
	}


	public void AY8910_set_clock(int chip,int clock)
	{
		AY8910Context psg = ay[chip];

		/* the step clock for the tone and noise generators is the chip clock    */
		/* divided by 8; for the envelope generator of the AY-3-8910, it is half */
		/* that much (clock/16), but the envelope of the YM2149 goes twice as    */
		/* fast, therefore again clock/8.                                        */
		/* Here we calculate the number of steps which happen during one sample  */
		/* at the given sample rate. No. of events = sample rate / (clock/8).    */
		/* STEP is a multiplier used to turn the fraction into a fixed point     */
		/* number.*/
		psg.UpdateStep = (STEP * super.getSampFreq() * 8) / clock;
		//System.out.println("psg.UpdateStep:" + psg.UpdateStep);
	}

	public void build_mixer_table(int chip)
	{
		AY8910Context psg = ay[chip];
		int i;
		double out;


		/* calculate the volume->voltage conversion table */
		/* The AY-3-8910 has 16 levels, in a logarithmic scale (3dB per step) */
		/* The YM2149 still has 16 levels for the tone generators, but 32 for */
		/* the envelope generator (1.5dB per step). */
		out = MAX_OUTPUT / 3;
		for (i = 31;i > 0;i--)
		{
			psg.VolTable[i] = (int)(out + 0.5);	/* round to nearest */

			out /= 1.188502227;	/* = 10 ^ (1.5/20) = 1.5dB */
		}
		psg.VolTable[0] = 0;
	}



	public void AY8910_reset(int chip)
	{
		int i;
		AY8910Context psg = ay[chip];

		psg.register_latch = 0;
		psg.RNG = 1;
		psg.OutputA = 0;
		psg.OutputB = 0;
		psg.OutputC = 0;
		psg.OutputN = 0xff;
		psg.lastEnable = -1;	/* force a write */
		for (i = 0;i < AY_PORTA;i++)
			_AYWriteReg(chip,i,0);	/* AYWriteReg() uses the timer system; we cannot */
									/* call it at this time because the timer system */
									/* has not been initialized. */
	}

	public void AY8910_sh_reset()
	{
		int i;

		for (i = 0;i < num + ym_num;i++)
			AY8910_reset(i);
	}

	public int AY8910_init(String chip_name,int chip,
			int clock, int volume, int samprate,
			int portAread, int portBread,
			int portAwrite, int portBwrite)
	{
		System.out.println("AY8910_init " + chip);
		int i;
		ay[chip] = new AY8910Context();
		AY8910Context psg = ay[chip];
		int[] vol = new int[3];


		//memset(PSG,0,sizeof(AY8910Context));
		psg.PortAread = portAread;
		psg.PortBread = portBread;
		psg.PortAwrite = portAwrite;
		psg.PortBwrite = portBwrite;
		
		for (i = 0;i < 3;i++)
		{
			vol[i] = volume;
			//name[i] = buf[i];
			//sprintf(buf[i],"%s #%d Ch %c",chip_name,chip,'A'+i);
		}
		//psg.Channel = stream_init_multi(3,name,vol,sample_rate,chip,AY8910Update);

		//if (psg.Channel == -1)
		//	return 1;

		AY8910_set_clock(chip,clock);
		psg.PeriodA = psg.UpdateStep;
		psg.PeriodB = psg.UpdateStep;
		psg.PeriodC = psg.UpdateStep;
		psg.PeriodE = psg.UpdateStep;
		psg.PeriodN = psg.UpdateStep;
		

		return 0;
	}

	public ReadHandler ay8910_read_port_0_r() { return new AY8910_read_port_r(0); }
	public ReadHandler ay8910_read_port_1_r() { return new AY8910_read_port_r(1); }
	public ReadHandler ay8910_read_port_2_r() { return new AY8910_read_port_r(2); }
	public ReadHandler ay8910_read_port_3_r() { return new AY8910_read_port_r(3); }
	public ReadHandler ay8910_read_port_4_r() { return new AY8910_read_port_r(4); }
	class AY8910_read_port_r implements ReadHandler {
		int context;
		public AY8910_read_port_r(int context) {
			this.context = context;
		}
		@Override
		public int read(int address) {
			return AY8910Read(context);
		}
	}

	public WriteHandler AY8910_control_port_0_w() { return new AY8910_control_port_w(0); }
	public WriteHandler AY8910_control_port_1_w() { return new AY8910_control_port_w(1); }
	public WriteHandler AY8910_control_port_2_w() { return new AY8910_control_port_w(2); }
	public WriteHandler AY8910_control_port_3_w() { return new AY8910_control_port_w(3); }
	public WriteHandler AY8910_control_port_4_w() { return new AY8910_control_port_w(4); }
	class AY8910_control_port_w implements WriteHandler {
		int context;
		public AY8910_control_port_w(int context) {
			this.context = context;
		}
		@Override
		public void write (int address, int data) {
			//System.out.println("AY8910_control_port_w " + context + " , " + data);
			AY8910Write(context,0,data);
		}
	}

	public WriteHandler AY8910_write_port_0_w() { return new AY8910_write_port_w(0); }
	public WriteHandler AY8910_write_port_1_w() { return new AY8910_write_port_w(1); }
	public WriteHandler AY8910_write_port_2_w() { return new AY8910_write_port_w(2); }
	public WriteHandler AY8910_write_port_3_w() { return new AY8910_write_port_w(3); }
	public WriteHandler AY8910_write_port_4_w() { return new AY8910_write_port_w(4); }
	class AY8910_write_port_w implements WriteHandler {
		int context;
		public AY8910_write_port_w(int context) {
			this.context = context;
		}
		@Override
		public void write (int address, int data) {
			//System.out.println("AY8910_write_port_w " + context + " , " + data);
			AY8910Write(context,1,data);
		}
	}

	/* AY8910 interface */
	/*
	READ16_HANDLER( AY8910_read_port_0_lsb_r ) { return AY8910Read(0); }
	READ16_HANDLER( AY8910_read_port_1_lsb_r ) { return AY8910Read(1); }
	READ16_HANDLER( AY8910_read_port_2_lsb_r ) { return AY8910Read(2); }
	READ16_HANDLER( AY8910_read_port_3_lsb_r ) { return AY8910Read(3); }
	READ16_HANDLER( AY8910_read_port_4_lsb_r ) { return AY8910Read(4); }
	READ16_HANDLER( AY8910_read_port_0_msb_r ) { return AY8910Read(0) << 8; }
	READ16_HANDLER( AY8910_read_port_1_msb_r ) { return AY8910Read(1) << 8; }
	READ16_HANDLER( AY8910_read_port_2_msb_r ) { return AY8910Read(2) << 8; }
	READ16_HANDLER( AY8910_read_port_3_msb_r ) { return AY8910Read(3) << 8; }
	READ16_HANDLER( AY8910_read_port_4_msb_r ) { return AY8910Read(4) << 8; }

	WRITE16_HANDLER( AY8910_control_port_0_lsb_w ) { if (ACCESSING_LSB) AY8910Write(0,0,data & 0xff); }
	WRITE16_HANDLER( AY8910_control_port_1_lsb_w ) { if (ACCESSING_LSB) AY8910Write(1,0,data & 0xff); }
	WRITE16_HANDLER( AY8910_control_port_2_lsb_w ) { if (ACCESSING_LSB) AY8910Write(2,0,data & 0xff); }
	WRITE16_HANDLER( AY8910_control_port_3_lsb_w ) { if (ACCESSING_LSB) AY8910Write(3,0,data & 0xff); }
	WRITE16_HANDLER( AY8910_control_port_4_lsb_w ) { if (ACCESSING_LSB) AY8910Write(4,0,data & 0xff); }
	WRITE16_HANDLER( AY8910_control_port_0_msb_w ) { if (ACCESSING_MSB) AY8910Write(0,0,data >> 8); }
	WRITE16_HANDLER( AY8910_control_port_1_msb_w ) { if (ACCESSING_MSB) AY8910Write(1,0,data >> 8); }
	WRITE16_HANDLER( AY8910_control_port_2_msb_w ) { if (ACCESSING_MSB) AY8910Write(2,0,data >> 8); }
	WRITE16_HANDLER( AY8910_control_port_3_msb_w ) { if (ACCESSING_MSB) AY8910Write(3,0,data >> 8); }
	WRITE16_HANDLER( AY8910_control_port_4_msb_w ) { if (ACCESSING_MSB) AY8910Write(4,0,data >> 8); }

	WRITE16_HANDLER( AY8910_write_port_0_lsb_w ) { if (ACCESSING_LSB) AY8910Write(0,1,data & 0xff); }
	WRITE16_HANDLER( AY8910_write_port_1_lsb_w ) { if (ACCESSING_LSB) AY8910Write(1,1,data & 0xff); }
	WRITE16_HANDLER( AY8910_write_port_2_lsb_w ) { if (ACCESSING_LSB) AY8910Write(2,1,data & 0xff); }
	WRITE16_HANDLER( AY8910_write_port_3_lsb_w ) { if (ACCESSING_LSB) AY8910Write(3,1,data & 0xff); }
	WRITE16_HANDLER( AY8910_write_port_4_lsb_w ) { if (ACCESSING_LSB) AY8910Write(4,1,data & 0xff); }
	WRITE16_HANDLER( AY8910_write_port_0_msb_w ) { if (ACCESSING_MSB) AY8910Write(0,1,data >> 8); }
	WRITE16_HANDLER( AY8910_write_port_1_msb_w ) { if (ACCESSING_MSB) AY8910Write(1,1,data >> 8); }
	WRITE16_HANDLER( AY8910_write_port_2_msb_w ) { if (ACCESSING_MSB) AY8910Write(2,1,data >> 8); }
	WRITE16_HANDLER( AY8910_write_port_3_msb_w ) { if (ACCESSING_MSB) AY8910Write(3,1,data >> 8); }
	WRITE16_HANDLER( AY8910_write_port_4_msb_w ) { if (ACCESSING_MSB) AY8910Write(4,1,data >> 8); }*/

}
