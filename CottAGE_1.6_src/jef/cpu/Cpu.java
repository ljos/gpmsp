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

package jef.cpu;

/**
 * Interface class for CPU emulators.
 * 
 * @author Erik Duijs
 * 
 *         Cpu.java
 */
public interface Cpu {

	public static final boolean TRACE = false;

	/** Initialize the CPU */
	public boolean init(jef.cpuboard.CpuBoard ram, int debug);

	/** Trigger an interrupt */
	public void interrupt(int type, boolean irq);

	/** Reset the CPU */
	public void reset();

	/** Execute the CPU for a given amount of Cycles */
	public void exec(int cycles);

	/** Return the currently executed instruction */
	public long getInstruction();

	/** Set a specific property of the CPU */
	public void setProperty(int property, int value);

	/** Set debug mode */
	public void setDebug(int debug);

	/** Get the debug mode */
	public int getDebug();

	/** Tag the CPU */
	public void setTag(String tag);

	/** Get the tag */
	public String getTag();

	public int getCyclesLeft();

	/** Interrupt type */
	public static final int INTERRUPT_TYPE_IRQ = 0;
	/** Interrupt type */
	public static final int INTERRUPT_TYPE_NMI = 1;
	/** Interrupt type */
	public static final int INTERRUPT_TYPE_FIRQ = 2;
	/** Interrupt type */
	public static final int INTERRUPT_TYPE_IGNORE = -1;

	/** Property type */
	public static final int PROPERTY_Z80_IRQ_VECTOR = 0;
}