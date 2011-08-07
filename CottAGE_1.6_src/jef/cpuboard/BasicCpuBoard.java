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

package jef.cpuboard;

import jef.cpu.Cpu;
import jef.map.ReadHandler;
import jef.map.ReadMap;
import jef.map.WriteHandler;
import jef.map.InterruptHandler;
import jef.map.WriteMap;

/**
 * Reference implementation of the CpuBoard interface.
 * 
 * @author Erik Duijs
 * 
 * BasicCpuBoard.java */
public class BasicCpuBoard implements CpuBoard {

	public Cpu		cpu;
	public int		frq;
	//public ReadHandler	readMap[];
	//public WriteHandler	writeMap[];
	//public ReadHandler	portInMap[];
	//public WriteHandler	portOutMap[];
    private ReadMap mra;
    private WriteMap mwa;
    private ReadHandler ior;
    private WriteHandler iow;
    
	public InterruptHandler	irqHandler;
	public int		ipf;

	public int		mem[];


/** Initialize the CpuBoard */
	public boolean init(CpuDriver cpuDriver) {

		this.cpu = cpuDriver.cpu;
		this.frq = cpuDriver.frq;
		this.mra = cpuDriver.mra;
		this.mwa = cpuDriver.mwa;
		this.ior = cpuDriver.ior;
		this.iow = cpuDriver.iow;
		this.mem = cpuDriver.mra.getMemory();
		this.irqHandler = cpuDriver.irh;
		this.ipf = cpuDriver.ipf;

		this.cpu.init(this, 0);

		return true;
	}

/** Get the memory */
	public int[] getMem() {
		return mem;
	}

/** Get the CPU */
	public Cpu getCpu() {
		return cpu;
	}

/** Reset the CPU */
	public void reset(boolean hard) {
		cpu.reset();
		if (hard) {
			for (int i = 0; i < mwa.getSize(); i++) {
				mwa.write(i, 0);
			}
		}
	}

/** Execute the CPU for a given number of cycles */
	public void exec(int cycles) {
		cpu.exec(cycles);
	}

/** Cause an interrupt on the CPU */
	public void interrupt(int type, boolean irq) {
		cpu.interrupt(type, irq);
	}

/** Write a byte */
	public void write8(int address, int data) {
        mwa.write(address, data);
		//writeMap[address].write(address, data);
	}

/** Write a byte */
	public void write8fast(int address, int data) {
        mwa.write(address, data);
		//writeMap[address].write(address, data);
	}

/** Read a byte */
	public int read8(int address) {
        return mra.read(address);
		//return readMap[address].read(address);
	}

/** Read an opcode byte */
	public int read8opc(int address) {
        return mra.read(address);
        //return readMap[address].read(address);
	}

/** Read a byte */
	public int read8arg(int address) {
        return mra.read(address);
        //return readMap[address].read(address);
	}

/** Write a word */
	public void write16(int address, int data) {
		mwa.write(address++, data & 0xff);
		mwa.write(address, data>>8);
        //writeMap[address].write(address++, data & 0xff);
        //writeMap[address].write(address, data>>8);
	}

/** Write a word */
	public void write16fast(int address, int data) {
        mwa.write(address++, data & 0xff);
        mwa.write(address, data>>8);
        //writeMap[address].write(address++, data & 0xff);
        //writeMap[address].write(address, data>>8);
	}

/** Read a word */
	public int read16(int address) {
		return mra.read(address++) | (mra.read(address) << 8);
        //return readMap[address].read(address++) | (readMap[address].read(address) << 8);
	}

/** Read a word */
	public int read16arg(int address) {
        return mra.read(address++) | (mra.read(address) << 8);
        //return readMap[address].read(address++) | (readMap[address].read(address) << 8);
	}


/** Write to port */
	public void out(int port, int value) {
		//System.out.println(Integer.toHexString(port) + " - " + portOutMap[port].toString());
		//portOutMap[port].write(port, value);
        iow.write(port, value);
	}

/** Read from port */
	public int in(int port) {
		//return portInMap[port].read(port);
        return ior.read(port);
	}

}