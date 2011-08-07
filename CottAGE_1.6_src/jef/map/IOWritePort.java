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

package jef.map;

/**
 * @author Erik Duijs
 * 
 * IOWritePort.java */
public class IOWritePort implements WriteHandler {

	private int			size;
	private WriteHandler	writeMap[];
	private UndefinedWrite  defwrite  = new UndefinedWrite();
	static final boolean debug = false;

	public IOWritePort() {
		this.size = 0x100;
		this.writeMap = new WriteHandler[size];
		set(0, size-1, (WriteHandler)defwrite);
	}

	public IOWritePort(int size) {
		this.size = size;
		this.writeMap = new WriteHandler[size];
		set(0, size-1, (WriteHandler)defwrite);
	}

	public void set(int from, int until, WriteHandler memWrite) {
		for (int i = from; i <= until; i++) {
			this.writeMap[i] = memWrite;
		}
	}

	public int getSize() {
		return size;
	}

	/*public WriteHandler[] get() {
		return writeMap;
	}*/
    public void write(int address, int data) {
        writeMap[address].write(address, data);
    }

	public class UndefinedWrite implements WriteHandler {
		public void write(int address, int value) {
			if (debug) System.out.println("Undefined Write at " + Integer.toHexString(address) + ", value : " + Integer.toHexString(value));
		}
	}
}