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
import jef.map.IOReadPort;
import jef.map.IOWritePort;
import jef.map.InterruptHandler;
import jef.map.ReadMap;
import jef.map.WriteMap;

/**
 * @author Erik Duijs
 * 
 *         CpuDriver defines the properties of how a CPU interfaces with the
 *         hardware. Mainly CPU type, memory maps and interrupt behaviour.
 * 
 *         CpuDriver.java
 */
public class CpuDriver {

	/** The CPU */
	public Cpu cpu;

	/** The CPU's clock speed */
	public int frq;

	/** The memory map for reading */
	public ReadMap mra;

	/** The memory map for writing */
	public WriteMap mwa;

	/** The map for reading ports */
	public IOReadPort ior;

	/** The map for writing to ports */
	public IOWritePort iow;

	/** The InterruptHandler */
	public InterruptHandler irh;

	/** Interrupts per frame */
	public int ipf;

	/** If isAudioCpu is true, this Cpu can be disabled if sound is disabled. */
	public boolean isAudioCpu = false;

	/** Constructor */
	public CpuDriver(Cpu cpu, int frq, ReadMap mra, WriteMap mwa,
			IOReadPort ior, IOWritePort iow, InterruptHandler irh, int ipf) {
		this.cpu = cpu;
		this.frq = frq;
		this.mra = mra;
		this.mwa = mwa;
		this.ior = ior;
		this.iow = iow;
		this.irh = irh;
		this.ipf = ipf;
	}
}