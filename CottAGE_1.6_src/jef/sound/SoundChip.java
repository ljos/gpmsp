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
 * SoundChip.java */
public abstract class SoundChip implements SoundChipEmulator {

    public int SUN_BUFSIZE = 4096; // 400
    public int LINE_BUFSIZE = 4096;
    public int JAVAX_BUFSIZE = 128;
	public int SAMPLE_FREQUENCY = 22050;

    protected int linBuffer[];
	protected byte muBuffer[];
	protected byte linByteStream[];
	protected boolean enabled = true;
	protected boolean useJavaxSound;

	private LoFiSound lofi;	// sun.audio implementation
	private HiFiSound hifi; // javax.sound implementation
	private int fps;

/**
 * Initialize the sound.
 */
	@Override
	public void init(boolean useJavaxSound, int freq, int linbuflen, int fps) {

		this.LINE_BUFSIZE = linbuflen;
		this.useJavaxSound = useJavaxSound;
		this.fps = fps;
		
		if (useJavaxSound) {
			this.SAMPLE_FREQUENCY = freq;
			
			this.JAVAX_BUFSIZE = (SAMPLE_FREQUENCY / fps);
			this.linBuffer = new int[JAVAX_BUFSIZE];
			this.linByteStream = new byte[JAVAX_BUFSIZE * 2];
			this.hifi = new HiFiSound(this, LINE_BUFSIZE, JAVAX_BUFSIZE, SAMPLE_FREQUENCY);
			hifi.init();
		} else {
       		this.linBuffer = new int[SUN_BUFSIZE];
			this.muBuffer = new byte[SUN_BUFSIZE];
			this.SAMPLE_FREQUENCY = 8000;
			this.lofi = new LoFiSound();
			lofi.init(linBuffer, muBuffer);
		}


	//	System.out.println("SoundChip " + this.toString() + " initialized.");
	}

/**
 * Enable sound
 */
	@Override
	public void enable() {
		if (enabled == false) {
			enabled = true;
			init(useJavaxSound, SAMPLE_FREQUENCY, JAVAX_BUFSIZE, fps);
		}
	}

/**
 * Disable sound
 */
	@Override
	public void disable() {
		if (enabled == true) {
			enabled = false;
			if (!useJavaxSound) lofi.disable();
			else hifi.terminate();
		}
	}

/**
 * Updates the sun.audio soundbuffers.
 */
    @Override
	public void update() {

		// only update if the registers have changed
		if (/*soundHasChanged &&*/ enabled) {

			// write buffer with sound data
			//writeBuffer();

			// converts the linear buffer to 8-bit mu-law
			if (!useJavaxSound) {
				writeBuffer();
				lofi.convertBufferToMuLaw();
			} else {
				hifi.update();
			}

		}
    }


    @Override
	public abstract void writeBuffer();

	public int getSampFreq() {
		return SAMPLE_FREQUENCY;
	}
	public int getBufferLength() {
		return linBuffer.length;
	}

/**
 * Convert linear buffer to a byte stream and return it.
 * Needed for javax.sound implementation.
 */
	@Override
	public byte[] getByteStream() {
	  	for(int i = 0; i < linBuffer.length; i++) {
			linByteStream[i<<1] = (byte)(linBuffer[i] >> 8);
			linByteStream[(i<<1) + 1] = (byte)(linBuffer[i] & 255);
		}
		return linByteStream;
	}

	protected void writeLinBuffer(int index, int value) {
		linBuffer[index] = value;
	}

	protected int readLinBuffer(int index) {
		return linBuffer[index];
	}

  	public void clearBuffer() {
	  	for(int i = 0; i < linBuffer.length; i++) {
		  	linBuffer[i] = 0;
	  	}
  	}
}