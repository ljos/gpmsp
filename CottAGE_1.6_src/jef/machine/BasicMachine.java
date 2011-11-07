/*
 * 
 * Java Emulation Framework
 * 
 * This library contains a framework for creating emulation software.
 * 
 * Copyright (C) 2002 Erik Duijs (erikduijs@yahoo.com)
 * 
 * Contributors: - Julien Freilat - Arnon Goncalves Cardoso - S.C. Wong -
 * Romain Tisserand - David Raingeard
 * 
 * 
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or (at your
 * option) any later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *  
 */

package jef.machine;

import jef.cpuboard.BasicCpuBoard;
import jef.cpuboard.CpuBoard;
import jef.cpuboard.CpuDriver;
import jef.map.InterruptHandler;
import jef.map.WriteHandler;
import jef.sound.SoundEmulation;
import jef.video.BitMap;

/**
 * @author Erik Duijs
 * 
 * BasicMachine.java
 * 
 * BasicMachine is a reference implementation of the Machine interface and is
 * the main emulation class. Subclass if more specific functions are needed.
 */
public class BasicMachine implements Machine {

	/** The machine's parts */
	public MachineDriver md;

	/** The CpuBoard's properties */
	public CpuDriver[] cd;

	/** as defined in the CpuDrivers which are defined in the MachineDriver */
	public CpuBoard[] cb;

	/** manages all sound emulation */
	public SoundEmulation se;

	/** how many timeslices one frames has to be divided in */
	protected int slicesPerFrame;

	protected int currentSlice = 0;

	/** defines if sound is to be emulated */
	protected boolean soundEnabled = true;

	/** non maskable interrupts can be enabled/disabled on machine level */
	public boolean nmi_interrupt_enabled = true;

	/** 'normal' interrupts can be enabled/disabled on machine level */
	public boolean interrupt_enabled = true;

	private boolean highScoreSupported;

	private long highScore;

	protected BitMap backBuffer;

	public BasicMachine() {
	}

	/**
	 * Initialize the machine
	 */
	@Override
	public void init(MachineDriver md) {

		md.mach = this;

		this.md = md;

		/*
		 * do not initialize emulation as we only need information about the
		 * driver
		 */
		if (md.info)
			return;

		this.cd = md.cpuDriver;
		this.cb = new CpuBoard[cd.length];

		slicesPerFrame = md.spf;

		// ----------------------------------------
		// Initialize CPUs
		// ----------------------------------------
		for (int c = 0; c < cd.length; c++) {

			// We need to divide a frame into timeslices
			// Choose the greatest common divider
			if (slicesPerFrame < cd[c].ipf)
				this.slicesPerFrame = cd[c].ipf;

			// Initialize the CpuBoards
			this.cb[c] = createCpuBoard(c);
			cb[c].init(cd[c]);

			// Tag the CPUs
			cd[c].cpu.setTag("CPU #" + Integer.toString(c));

			// Reset all CPUs
			cd[c].cpu.reset();
		}

		if (slicesPerFrame == 0)
			slicesPerFrame = 1;

		// ----------------------------------------
		// Initialize Sound emulation
		// ----------------------------------------
		se = new SoundEmulation();
		if (md.soundChips != null) {
			se.init(
				md.soundChips,
				jef.util.Config.SOUND_SAMPLING_FREQ,
				jef.util.Config.SOUND_BUFFER_SIZE,
				md.fps);
		}

		// ----------------------------------------
		// Initialize Video emulation
		// ----------------------------------------
		md.ve.init(md);
		if (md.initProms != null) {
			md.initProms.palette_init();
		}
		if (md.vh_start != null) {
			md.vh_start.vh_start();
		}

		// ----------------------------------------
		// Initialize Machine
		// ----------------------------------------
		md.init.exec();
	}

	/**
	 * Enables or disables sound
	 */
	@Override
	public void setSound(boolean enable) {
		this.soundEnabled = enable;
		if (md.soundChips != null) {
			for (int c = 0; c < md.soundChips.length; c++) {
				if (soundEnabled)
					md.soundChips[c].enable();
				else
					md.soundChips[c].disable();
			}
		}
	}

	/**
	 * Set high score support.
	 */
	@Override
	public void setHighScoreSupported(boolean b) {
		this.highScoreSupported = b;
	}
	
	/**
	 * Check if high scores are supported.
	 */
	@Override
	public boolean isHighScoreSupported() {
		return this.highScoreSupported;
	}
	
	/**
	 * Update the high score, but only if high scores are supported and if the
	 * score is higher than the current high score.
	 */
	@Override
	public void setHighScore(long score) {
		if (this.highScoreSupported && score > highScore) {
			this.highScore = score;
		}
	}
	
	/**
	 * Return the high score
	 */
	@Override
	public long getHighScore() {
		return this.highScore;
	}

	/**
	 * Set a new HighScoreHandler for this machine. It will also reset the high
	 * score, and (if the handler is not null), mark the machine as having high
	 * score support.
	 */


	/**
	 * Reset the current high score to 0
	 */
	public void resetHighScore() {
		this.highScore = 0;
	}
	/**
	 * Reset machine
	 */
	@Override
	public void reset(boolean hard) {
		if (!highScoreSupported) {
			for (int c = 0; c < cd.length; c++) {
				cd[c].cpu.reset();
			}
		}
	}

	/**
	 * Get a property
	 */
	@Override
	public int getProperty(int property) {
		if (property == FPS)
			return md.fps;
		if (property == ROT)
			return md.ROT;
		return -1;
	}

	/**
	 * Signal the machine a key has been pressed
	 */
	@Override
	public void keyPress(int keyCode) {
		for (int i = 0; i < md.input.length; i++) {
			md.input[i].keyPress(keyCode);
		}
	}

	/**
	 * Signal the machine a key has been released
	 */
	@Override
	public void keyRelease(int keyCode) {
		for (int i = 0; i < md.input.length; i++) {
			md.input[i].keyRelease(keyCode);
		}
	}

	/**
	 * Update InputPorts (for impulse events)
	 */
	protected void updateInput() {
		for (int i = 0; i < md.input.length; i++) {
			md.input[i].update();
		}
	}

	/**
	 * Read an InputPort
	 */
	public int readinputport(int port) {
		return md.input[port].read(0);
	}

	public int getCurrentSlice() {
		return this.currentSlice;
	}

	/**
	 * Do everything for one frame
	 */
	@Override
	public BitMap refresh(boolean render) {

		boolean rendered = false;

		int curCycle = 0;

		for (int slice = 0; slice < slicesPerFrame; slice++) { // each frame is
			// divided in
			// slices

			currentSlice = slice;

			for (int c = 0; c < cd.length; c++) { // iterate through all cpu
												  // boards,
												  // give them their timeslice
												  // and cause an interrupt whene needed.

				if (!cd[c].isAudioCpu || (soundEnabled && cd[c].isAudioCpu)) {

					int cyclesPerFrame = cd[c].frq / md.fps;
					int cyclesPerTimeSlice = cyclesPerFrame / slicesPerFrame;

					if (c == 0) {
						curCycle += cyclesPerTimeSlice;
					}

					cd[c].cpu.exec(cyclesPerTimeSlice);

					int interruptsPerFrame = cd[c].ipf;

					if (interruptsPerFrame > 0) {
						int slicesPerInterrupt = slicesPerFrame / interruptsPerFrame;

						if ((slice % slicesPerInterrupt) == 0) {
							cb[c].interrupt(cd[c].irh.irq(), true);
						}
					}
				}
			}

			if (!rendered && render && curCycle > md.getVideoBlankDuration()) {
				backBuffer = getDisplay();
				rendered = true;
			}

		}

		// UPDATE SOUND
		se.update();

		// UPDATE INPUT (for Impulse Events)
		updateInput();
		return backBuffer;
	}

	/**
	 * Returns the pixels array representing the emulated video image.
	 */
	protected BitMap getDisplay() {
		BitMap p = md.vh_screenrefresh.video_update();
		md.vh_screenrefresh.video_post_update();
		return p;
	}

	/**
	 * Creates a new CpuBoard Currently this is always a BasicCpuBoard, which
	 * is fine in most cases. Sometimes a customized CpuBoard is needed, for
	 * example if read8opc() needs to be overloaded for runtime opcode
	 * decryption.
	 */
	public CpuBoard createCpuBoard(int id) {
		return new BasicCpuBoard();
	}

	/**
	 * Standard InterruptHandler.
	 */
	public InterruptHandler irq0_line_hold() {
		return new Interrupt_switched();
	}

	/**
	 * Standard InterruptHandler.
	 */
	public InterruptHandler nmi_interrupt_switched() {
		return new NMI_interrupt_switched();
	}
	public class NMI_interrupt_switched implements InterruptHandler {
		@Override
		public int irq() {
			return nmi_interrupt_enabled ? 1 : -1;
		}
	}

	/**
	 * Standard InterruptHandler.
	 */
	public InterruptHandler interrupt_switched() {
		return new Interrupt_switched();
	}
	public class Interrupt_switched implements InterruptHandler {
		@Override
		public int irq() {
			return interrupt_enabled ? 0 : -1;
		}
	}

	/**
	 * Standard InterruptHandler.
	 */
	public WriteHandler nmi_interrupt_enable() {
		return new NMI_interrupt_enable();
	}
	public class NMI_interrupt_enable implements WriteHandler {
		@Override
		public void write(int address, int value) {
			nmi_interrupt_enabled = (value != 0);
		}
	}

	/**
	 * Standard InterruptHandler.
	 */
	public WriteHandler interrupt_enable() {
		return new Interrupt_enable();
	}
	public class Interrupt_enable implements WriteHandler {
		@Override
		public void write(int address, int value) {
			interrupt_enabled = (value != 0);
		}
	}

	/**
	 * Standard InterruptHandler.
	 */
	public InterruptHandler nmi_interrupt() {
		return new NMI_interrupt();
	}
	public class NMI_interrupt implements InterruptHandler {
		@Override
		public int irq() {
			return 1;
		}
	}

	/**
	 * Standard InterruptHandler.
	 */
	public InterruptHandler interrupt() {
		return new Interrupt();
	}
	public class Interrupt implements InterruptHandler {
		@Override
		public int irq() {
			return 0;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jef.machine.Machine#getProgress()
	 */
	@Override
	public double getProgress() {
		double cyclesPerFrame = (double) md.getCpuDriver()[0].frq / (double) md.fps;
		double cyclesPerSlice = cyclesPerFrame / slicesPerFrame;
		double curSliceCyclesLeft = cb[0].getCpu().getCyclesLeft();
		double cyclesPerFrameLeft =
			cyclesPerFrame - (((currentSlice + 1) * cyclesPerSlice) - curSliceCyclesLeft);
		return 1.0 - (cyclesPerFrameLeft / cyclesPerFrame);
	}

}