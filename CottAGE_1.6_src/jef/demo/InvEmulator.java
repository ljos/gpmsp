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

package jef.demo;

import java.net.URL;

import jef.machine.BasicMachine;
import jef.machine.Machine;
import jef.machine.MachineDriver;
import jef.map.MemoryReadAddress;
import jef.map.MemoryWriteAddress;
import jef.map.InputPort;
import jef.map.IOReadPort;
import jef.map.IOWritePort;
import jef.map.ReadHandler;
import jef.map.WriteHandler;
import jef.map.InterruptHandler;
import jef.video.BitMap;
import jef.video.Vh_refresh;
import jef.video.BitMapImpl;
import jef.util.RomLoader;
import jef.cpuboard.CpuDriver;
import jef.cpu.I8080;
import jef.cpu.Cpu;

/**
 * This class is an emulator of the classic Space Invaders arcade game's
 * hardware from the late 70s.
 * It is meant as an example of how to create an emulater using
 * the JEF (Java Emulation Framework) package.
 * All info about the Space Invaders hardware is derived from the MAME source
 * which is obtainable from www.mame.net.
 *
 * @author Erik Duijs
 */
public class InvEmulator implements InterruptHandler,
									Vh_refresh,
									WriteHandler{

    /** URL is needed to load the ROM images */
    URL base_URL;

	/** Reference to the Machine object */
	Machine m = new BasicMachine();

	/** The real machine's screen width. */
	static final int SCREEN_WIDTH = 224;

	/** The real machine's screen height. */
	static final int SCREEN_HEIGHT = 256;

	/** The number of frames per second (FPS) that should be drawn. */
	static final int SCREEN_FPS = 60;

	/** The machine runs on a 2Mhz Cpu (an Intel I8080). */
	static final int CPU_CLOCK_SPEED = 2000000;

	/** The machine invokes 2 interrupts every frame. */
	static final int CPU_IRQ_PER_FRAME = 2;

	/** The InputPorts emulate joysticks and dipswitches etc. */
	InputPort[] input;

	/** JEF provides for multiple memory regions, so a 2-dimensional array
	 *  is needed for the int[] array representing the memory. */
	int[][] memory = new int[1][0x10000]; // 1x 64kb memory

	/** Used to count the interrupts */
	int count = 0;

	/** Used in the custom bit shift hardware emulation */
	int shift_amount = 0;
	/** Used in the custom bit shift hardware emulation */
	int shift_data1 = 0;
	/** Used in the custom bit shift hardware emulation */
	int shift_data2 = 0;

	/** Reference to the pixel buffer (the video back buffer) */
	int[] pixels;
	/** BitMap representing the back buffer */
	BitMapImpl bitmap;

	/** Used in video emulation. It's the palette of black and white colors. */
	int[]	pal = { 0x000000, 0xffffff };
	/** Used in video emulation to translate a written byte to pixels. */
	int[][] gfx = new int[256][8];

	/**
	 * This is called by the main applet Invader.java and
	 * initializes the emulator.
	 * It returns the Machine object.
	 * 	 * @param base_URL	 * @param w	 * @param h	 * @return Machine	 */
	public Machine getMachine(URL base_URL, int w, int h) {

		// URL is needed to load the ROMs
		this.base_URL = base_URL;

		// Create the BitMap representing the video back buffer.
		this.bitmap = new BitMapImpl(w,h);
		// Create a reference to the pixels for ease.
		this.pixels = bitmap.getPixels();

		// Initialize the translation table used to translate
		// bytes written to video ram, to pixel colors.
		for (int i = 0; i < 256; i++) {
			gfx[i][7] = pal[ i >> 7];
			gfx[i][6] = pal[(i &  64) >> 6];
			gfx[i][5] = pal[(i &  32) >> 5];
			gfx[i][4] = pal[(i &  16) >> 4];
			gfx[i][3] = pal[(i &   8) >> 3];
			gfx[i][2] = pal[(i &   4) >> 2];
			gfx[i][1] = pal[(i &   2) >> 1];
			gfx[i][0] = pal[(i &   1)];
		}

		// Load the ROMs
		loadROMs();

		// Initialize InputPorts
		input = createInputPorts();

		// Create the MachineDriver (used for generating the Machine)
		MachineDriver md = new MachineDriver(createCpuDriver(),
											 SCREEN_WIDTH,
											 SCREEN_HEIGHT,
											 (Vh_refresh)this);
		// Set the frames per second
		md.setFPS(SCREEN_FPS);

		// Set the input ports
		md.setInputPorts(input);

		// Initialize Machine by generating it from the MachineDriver
		m.init(md);
		return m;
	}

	/**
	 * Returns CpuDriver(s). Every Cpu needs a CpuDriver which
	 * defines how the Cpu interacts with the rest of the machine.
	 * This means memory maps, port maps, interrupts and such.
	 * In this case there's just one CpuDriver because Space Invaders
	 * only has one Cpu (an intel I8080 @ 2MHz).
	 * 	 * @return CpuDriver[]	 */
	public CpuDriver[] createCpuDriver() {
		CpuDriver[] cpuDriver = new CpuDriver[1];
		cpuDriver[0] = new CpuDriver( (Cpu) new I8080(),
										CPU_CLOCK_SPEED,
										createMemoryReadAddress(),
										createMemoryWriteAddress(),
										createIOReadPort(),
										createIOWritePort(),
										(InterruptHandler)this,
										CPU_IRQ_PER_FRAME);
		return cpuDriver;
	}

	/**
	 * This is the implementation of this class' InterruptHandler interface.
	 * Space invaders's Cpu is interrupted 2 times per frame.
	 * Every 1st is an IRQ, the 2nd an NMI (Non Maskable Interrupt).
	 * 	 * @see jef.map.InterruptHandler#irq()	 */
	public int irq() {
		count++;

		if ( (count & 1) == 1 )
			return jef.cpu.Cpu.INTERRUPT_TYPE_IRQ;  /* IRQ */
		else
			return jef.cpu.Cpu.INTERRUPT_TYPE_NMI;  /* NMI */
	}

	/**
	 * Creates the memory map for reading.
	 * 	 * @return MemoryReadAddress	 */
	private MemoryReadAddress createMemoryReadAddress() {
		MemoryReadAddress mra = new MemoryReadAddress(memory[0]);
		mra.setMR( 0x0000, 0x1fff, MemoryReadAddress.MRA_ROM );
		mra.setMR( 0x2000, 0x3fff, MemoryReadAddress.MRA_RAM );
		mra.setMR( 0x4000, 0x63ff, MemoryReadAddress.MRA_ROM );
		return mra;
	}

	/**
	 * Creates the memory map for writing.
	 * 	 * @return MemoryWriteAddress	 */
	private MemoryWriteAddress createMemoryWriteAddress() {
		MemoryWriteAddress mwa = new MemoryWriteAddress(memory[0]);
		mwa.setMW( 0x0000, 0x1fff, MemoryWriteAddress.MWA_ROM );
		mwa.setMW( 0x2000, 0x23ff, MemoryWriteAddress.MWA_RAM );
		mwa.set( 0x2400, 0x3fff, (WriteHandler)this ); // Video RAM
		mwa.setMW( 0x4000, 0x63ff, MemoryWriteAddress.MWA_ROM );
		return mwa;
	}

	/**
	 * Creates an I/O port map for reading.
	 * 	 * @return IOReadPort	 */
	private IOReadPort createIOReadPort() {
		IOReadPort ior = new IOReadPort();
		ior.set( 0x01, 0x01, (ReadHandler)input[0] );
		ior.set( 0x02, 0x02, (ReadHandler)input[1] );
		ior.set( 0x03, 0x03, new ShiftDataR() );
		return ior;
	}

	/**
	 * Creates an I/O port map for writing.
	 * 	 * @return IOWritePort	 */
	private IOWritePort createIOWritePort() {
		IOWritePort	iow = new IOWritePort();
		iow.set( 0x02,	0x02, new ShiftAmountW() );
		iow.set( 0x04,	0x04, new ShiftDataW() );
		return iow;
	}

	/**
	 * Creates input ports.
	 * These are mapped to 2 ports in createIOReadPort().
	 * An InputPort is a port which reads from the keyboard and uses that
	 * to emulate joysticks, dip switches or other controllers.
	 * 	 * @return InputPort[]	 */
	private InputPort[] createInputPorts() {

		InputPort[] port = new InputPort[2];
		port[0] = new InputPort();
		port[1] = new InputPort();

		/* InputPort[0] */
		port[0].setBit( 0x01, InputPort.IP_ACTIVE_LOW,  InputPort.IPT_COIN1 );
		port[0].setBit( 0x02, InputPort.IP_ACTIVE_HIGH, InputPort.IPT_START2 );
		port[0].setBit( 0x04, InputPort.IP_ACTIVE_HIGH, InputPort.IPT_START1 );
		port[0].setBit( 0x10, InputPort.IP_ACTIVE_HIGH, InputPort.IPT_BUTTON1 );
		port[0].setBit( 0x20, InputPort.IP_ACTIVE_HIGH, InputPort.IPT_JOYSTICK_LEFT  | InputPort.IPF_2WAY );
		port[0].setBit( 0x40, InputPort.IP_ACTIVE_HIGH, InputPort.IPT_JOYSTICK_RIGHT | InputPort.IPF_2WAY );
		port[0].setBit( 0x80, InputPort.IP_ACTIVE_LOW,  InputPort.IPT_UNKNOWN );

		/* Dip switch 0 (InputPort[1] */
		port[1].setDipName( 0x03, 0x00, "Lives" );
		port[1].setDipSetting(    0x00, "3" );
		port[1].setDipSetting(    0x01, "4" );
		port[1].setDipSetting(    0x02, "5" );
		port[1].setDipSetting(    0x03, "6" );
		port[1].setBit( 0x04, InputPort.IP_ACTIVE_HIGH, InputPort.IPT_TILT );
		port[1].setDipName( 0x08, 0x00, "Bonus Live" );
		port[1].setDipSetting(    0x08, "1000" );
		port[1].setDipSetting(    0x00, "1500" );
		port[1].setBit( 0x10, InputPort.IP_ACTIVE_HIGH, InputPort.IPT_BUTTON1 | InputPort.IPF_PLAYER2 );
		port[1].setBit( 0x20, InputPort.IP_ACTIVE_HIGH, InputPort.IPT_JOYSTICK_LEFT  | InputPort.IPF_PLAYER2 );
		port[1].setBit( 0x40, InputPort.IP_ACTIVE_HIGH, InputPort.IPT_JOYSTICK_RIGHT | InputPort.IPF_PLAYER2 );
		port[1].setDipName( 0x80, 0x00, "Coport Info" );
		port[1].setDipSetting(    0x80, "Off" );
		port[1].setDipSetting(    0x00, "on" );

		return port;
	}

	/**
	 * This class emulates Space Invader's custom bit shift hardware.	 */
	public class ShiftDataR implements ReadHandler {
		public int read(int port) {
			return (((((shift_data1 << 8) | shift_data2) << (shift_amount & 0x07)) >> 8) & 0xff);
		}
	}

	/**
	 * This class emulates Space Invader's custom bit shift hardware.
	 */
	public class ShiftDataW implements WriteHandler {
		public void write(int port, int data) {
			shift_data2 = shift_data1;
			shift_data1 = data;
		}
	}

	/**
	 * This class emulates Space Invader's custom bit shift hardware.
	 */
	public class ShiftAmountW implements WriteHandler {
		public void write(int port, int data) {
			shift_amount = data;
		}
	}

	/**
	 * Loads the zipped ROM images to memory.	 */
	private void loadROMs() {
		RomLoader romLoader = new RomLoader();
		romLoader.setZip( "invaders" );

		romLoader.setMemory(memory[0]);
		romLoader.loadROM( "invaders.h", 0x0000, 0x0800, 0x734f5ad8 );
		romLoader.loadROM( "invaders.g", 0x0800, 0x0800, 0x6bfaca4a );
		romLoader.loadROM( "invaders.f", 0x1000, 0x0800, 0x0ccead96 );
		romLoader.loadROM( "invaders.e", 0x1800, 0x0800, 0x14e538b0 );

		romLoader.loadZip(base_URL);
	}

	/**
	 * This is the implementation of this class' WriteHandler interface.
	 * Here, every write to video ram is captured and the pixels that
	 * are changed because of this write, are directly emulated and written
	 * to the pixel buffer.
	 * 	 * @see jef.map.WriteHandler#write(int, int)	 */
	public void write(int address, int data) {
		memory[0][address] = data;
		int x, y;
		int i = 0x1bff - (address - 0x2400);
		x = (i & 0x1f) * 8;
		y = 223 - (i >> 5);

		for (int n = 0; n < 8; n++) {
			i = (x + n) * 224 + y;
			pixels[i] = pal[gfx[data][7 - n] & 1];
		}
	}

	/**
	 * This is the implementation of this class' Vh_refresh interface.
	 * This is called after every frame and returns the BitMap
	 * representing the back buffer.
	 * 	 * @see jef.video.Vh_refresh#video_update()	 */
	public BitMap video_update() {
		return this.bitmap;
	}

	/**
	 * This is the implementation of this class' Vh_refresh interface.
	 * This is called after every frame and returns the BitMap
	 * representing the back buffer.
	 * 	 * @see jef.video.Vh_refresh#video_post_update()	 */
	public void video_post_update() {}

}