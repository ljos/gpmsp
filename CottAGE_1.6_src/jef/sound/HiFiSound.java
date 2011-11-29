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

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

/**
 * @author Erik Duijs
 * 
 *         HiFiSound.java
 * 
 *         Only supported using JRE 1.3 or newer.
 */

public final class HiFiSound/* extends Thread */{

	protected SourceDataLine m_line;

	int samfreq;
	int LINE_BUF_SIZE;
	int WRITE_BUF_SIZE;

	boolean running;

	SoundChipEmulator sc;

	/**
	 * Constructor.
	 */
	public HiFiSound(SoundChipEmulator sc, int LINE_BUFSIZE,
			int write_buf_size, int SAMPLE_FREQUENCY) {

		this.sc = sc;
		this.LINE_BUF_SIZE = LINE_BUFSIZE;
		this.WRITE_BUF_SIZE = write_buf_size;
		this.samfreq = SAMPLE_FREQUENCY;
	}

	/**
	 * Shut down sound system.
	 */
	public void terminate() {
		if (m_line != null) {
			m_line.stop();
			m_line.close();
			m_line = null;
			running = false;
		}
	}

	/**
	 * This should fix the hanging sound problem.
	 */
	@Override
	public void finalize() {
		terminate();
		try {
			super.finalize();
		} catch (Throwable e) {
		}
	}

	/**
	 * Initialize.
	 */
	public void init() {
		AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
				samfreq, 16, 1, 2, samfreq, true);

		DataLine.Info info = new DataLine.Info(SourceDataLine.class, format,
				LINE_BUF_SIZE);

		if (!AudioSystem.isLineSupported(info)) {
			return;
		}

		try {
			m_line = (SourceDataLine) AudioSystem.getLine(info);
			m_line.open(format);
		} catch (LineUnavailableException lue) {
		//	System.err.println("Unavailable data line");
			return;
		}

		m_line.start();

		running = true;
	}

	/**
 * 
 */
	public void update() {
		sc.writeBuffer();
		if (m_line.available() > (m_line.getBufferSize() - WRITE_BUF_SIZE * 16)) {
			m_line.write(sc.getByteStream(), 0, WRITE_BUF_SIZE * 2);
		}
	}
}
