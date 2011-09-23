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


import jef.cpuboard.CpuDriver;
import jef.map.InputPort;
import jef.map.NoFunction;
import jef.map.VoidFunction;
import jef.sound.SoundChipEmulator;
import jef.video.Eof_callback;
import jef.video.GfxDecodeInfo;
import jef.video.Vh_convert_color_proms;
import jef.video.Vh_refresh;
import jef.video.Vh_start;
import jef.video.Vh_stop;
import jef.video.VideoEmulator;

/**
 * @author Erik Duijs
 * 
 * MachineDriver.java
 * 
 * The MachineDriver holds all needed global properties
 * of the emulated machine. This class is passed to a Machine
 * at initialization to construct a Machine based on the
 * MachineDriver's properties.
 */
public class MachineDriver {

	/** Reference to the CpuDriver(s) */
	public CpuDriver[]   cpuDriver;

	/** Frames per second */
	public int 			fps = 60;

	/** VBlank duration */
	public int 			vbd = 0;

	/** Time slices per frame */
	public int			spf = 1;

	/** Width of the display */
	public int			w;

	/** Height of the display */
	public int			h;

	/** Screen rotation */
	public int	  		ROT = 0;

	/** Flags to set for video */
	public int			videoFlags = 0;

	/** The visible area of the display */
	public int[]	visible;

	/** Reference to the GfxDecoderInfo(s) */
	public GfxDecodeInfo[] gfx;

	/** Total Colors */
	public int			pal = 1;

	/** */
	public int			col = 1;

	/** Machine information */
	public boolean		info = false;

	/** Initializer function */
	public VoidFunction init = new NoFunction();

	/** For video emulation initialization */
	public VideoEmulator ve = new NoVE();
	public class NoVE implements VideoEmulator {
		@Override
		public void init(MachineDriver mDr) {}
	}

	/** Function to convert the color proms */
	public Vh_convert_color_proms initProms = new NoProms();
	public class NoProms implements Vh_convert_color_proms {
		@Override
		public void palette_init() {}
	}

	/** Function called at end of frame */
	public Eof_callback	eof_callback = new NoEof();
	public class NoEof implements Eof_callback {
		@Override
		public void eof_callback() {}
	}

	/** Function called to initialize video emulation */
	public Vh_start		vh_start = new NoStart();
	public class NoStart implements Vh_start {
		@Override
		public int vh_start() { return 0; }
	}

	/** Function called to end video emulation */
	public Vh_stop		vh_stop = new NoStop();
	public class NoStop implements Vh_stop {
		@Override
		public void vh_stop() {}
	}

	/** Function called at every redraw of the emulated screen */
	public Vh_refresh 	vh_screenrefresh;

	/** Reference to the SoundChipEmulator(s) */
	public SoundChipEmulator[] soundChips;

	/** Reference to the InputPort(s) */
	public InputPort[] input;

	/** Reference to the memory region(s) */
	public int[][] REGIONS;

	/** Reference to the Machine */
	public Machine mach = null;

	/** Constructor */
	public MachineDriver(	CpuDriver[] cpuDriver,
							int fps,
							int vbd,
							int spf,
							VoidFunction init,
							int w,
							int h,
							int[]visible,
							GfxDecodeInfo[] gfx,
							int pal, int col,
							VideoEmulator ve,
							Vh_convert_color_proms initProms,
							int videoFlags,
							Eof_callback eof_callback,
							Vh_start vh_start,
							Vh_stop vh_stop,
							Vh_refresh vh_screenrefresh,
							SoundChipEmulator[] sound) {

		this.cpuDriver	= cpuDriver;
		this.fps	 	= fps;
		this.vbd		= vbd;
		this.spf		= spf;
		this.init		= init;
		this.w			= w;
		this.h			= h;
		this.visible	= visible;
		this.gfx		= gfx;
		this.pal		= pal;
		this.col		= col;
		this.initProms	= initProms;
		this.videoFlags	= videoFlags;
		this.eof_callback = eof_callback;
		this.vh_start	= vh_start;
		this.vh_stop	= vh_stop;
		this.vh_screenrefresh = vh_screenrefresh;
		this.soundChips = sound;
		this.ve 		= ve;
	}

	/** Constructor (DEPRECATED) */
	public MachineDriver(	CpuDriver[] cpuDriver,
							int fps,
							int vbd,
							int spf,
							VoidFunction init,
							int w,
							int h,
							int[]visible,
							GfxDecodeInfo[] gfx,
							int pal, int col,
							Vh_convert_color_proms initProms,
							int videoFlags,
							Eof_callback eof_callback,
							Vh_start vh_start,
							Vh_stop vh_stop,
							Vh_refresh vh_screenrefresh,
							SoundChipEmulator[] sound) {

		this.cpuDriver	= cpuDriver;
		this.fps	 	= fps;
		this.vbd		= vbd;
		this.spf		= spf;
		this.init		= init;
		this.w			= w;
		this.h			= h;
		this.visible	= visible;
		this.gfx		= gfx;
		this.pal		= pal;
		this.col		= col;
		this.initProms	= initProms;
		this.videoFlags	= videoFlags;
		this.eof_callback = eof_callback;
		this.vh_start	= vh_start;
		this.vh_stop	= vh_stop;
		this.vh_screenrefresh = vh_screenrefresh;
		this.soundChips = sound;
	}

	public MachineDriver(CpuDriver[] cpuDriver,
						 int w, int h,
						 Vh_refresh vh) {
		this.cpuDriver = cpuDriver;
		this.w = w;
		this.h = h;
		this.vh_screenrefresh = vh;
		this.visible = new int[4];
		visible[0] = 0;
		visible[1] = 0;
		visible[2] = w-1;
		visible[3] = h-1;
	}



	// Getters and setters
	public void setCpuDriver(CpuDriver[] cpuDriver) {
		this.cpuDriver = cpuDriver;
	}

	public CpuDriver[] getCpuDriver() {
		return this.cpuDriver;
	}

	public void setFPS(int fps) {
		this.fps = fps;
	}

	public int getFPS() {
		return this.fps;
	}

	public void setVideoBlankDuration(int vbd) {
		this.vbd = vbd;
	}

	public int getVideoBlankDuration() {
		return this.vbd;
	}

	public void setTimeSlicesPerFrame(int spf) {
		this.spf = spf;
	}

	public int getTimeSlicesPerFrame() {
		return this.spf;
	}

	public void setVideoWidth(int w) {
		this.w = w;
	}

	public int getVideoWidth() {
		return this.w;
	}

	public void setVideoHeight(int h) {
		this.h = h;
	}

	public int getVideoHeight() {
		return this.h;
	}

	public void setVisibleArea(int[] visible) {
		this.visible = visible;
	}

	public int[] getVisibleArea() {
		return this.visible;
	}

	public void setGfxDecodeInfo(GfxDecodeInfo[] gfx) {
		this.gfx = gfx;
	}

	public GfxDecodeInfo[] getGfxDecodeInfo() {
		return this.gfx;
	}

	public void setPaletteLength(int pal) {
		this.pal = pal;
	}

	public int getPaletteLength() {
		return this.pal;
	}

	public void setColorTableLength(int col) {
		this.col = col;
	}

	public int getColorTableLength() {
		return this.col;
	}

	public void setInitializer(VoidFunction init) {
		this.init = init;
	}

	public VoidFunction getInitializer() {
		return this.init;
	}

	public void setVideoEmulator(VideoEmulator ve) {
		this.ve = ve;
	}

	public VideoEmulator getVideoEmulator() {
		return this.ve;
	}

	public void setEofCallBack(Eof_callback eof) {
		this.eof_callback = eof;
	}

	public Eof_callback getEofCallBack() {
		return this.eof_callback;
	}

	public void setVhStart(Vh_start s) {
		this.vh_start = s;
	}

	public Vh_start getVhStart() {
		return this.vh_start;
	}

	public void setVhStop(Vh_stop s) {
		this.vh_stop = s;
	}

	public Vh_stop getVhStop() {
		return this.vh_stop;
	}

	public void setVhRefresh(Vh_refresh r) {
		this.vh_screenrefresh = r;
	}

	public Vh_refresh getVhRefresh() {
		return this.vh_screenrefresh;
	}

	public void setSoundChips(SoundChipEmulator[] soundChips) {
		this.soundChips = soundChips;
	}

	public SoundChipEmulator[] getSoundChips() {
		return this.soundChips;
	}

	public void setInputPorts(InputPort[] p) {
		this.input = p;
	}

	public InputPort[] getInputPorts() {
		return this.input;
	}

	public void setMemoryRegions(int[][] r) {
		this.REGIONS = r;
	}

	public int[][] getMemoryRegions() {
		return this.REGIONS;
	}

	public void setMachine(Machine m) {
		this.mach = m;
	}

	public Machine getMachine() {
		return this.mach;
	}
	
	public int[] getREGION_CPU() {
		return ((cottage.vidhrdw.Pacman)ve).getREGION_CPU();
	}


}