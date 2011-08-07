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

/**
 * @author Erik Duijs
 * 
 * CpuBoard.java */
public interface CpuBoard {

/** Initialize the CpuBoard */
	public boolean init(CpuDriver cpuDriver);

/** Get the memory */
	public int[] getMem();

/** Get the Cpu */
	public Cpu	getCpu();

/** Reset the CPU */
	public void reset(boolean hard);

/** Execute the CPU for a given number of cycles */
	public void exec(int cycles);

/** Cause an interrupt on the CPU */
	public void interrupt(int type, boolean irq);

/** Write a byte */
	public void write8(int address, int data);

/**
 * Write a byte
 * When a Cpu calls this method, a speed up can be achieved.
 */
	public void write8fast(int address, int data);

/** Read a byte */
	public int read8(int address);

/** Read an opcode byte */
	public int read8opc(int address);

/** Read a byte. Can be called by CPU to read an opcode argument */
	public int read8arg(int address);

/** Write a word */
	public void write16(int address, int data);

/**
 * Write a word
 * When a Cpu calls this method, a speed up can be achieved.
 */
	public void write16fast(int address, int data);

/** Read a word */
	public int read16(int address);

/** Read a word */
	public int read16arg(int address);

/** Write to port */
	public void out(int port, int value);

/** Read from port */
	public int in(int port);
}