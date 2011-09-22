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
 * IOReadPort.java */
public class IOReadPort implements ReadHandler {

	private int			size;
	private ReadHandler	readMap[];
	private UndefinedRead  defread  = new UndefinedRead();
	static final boolean debug = false;

	public IOReadPort() {
		this.size = 0x100;
		this.readMap = new ReadHandler[size];
		set(0, size-1, defread);
	}

	public IOReadPort(int size) {
		this.size = size;
		this.readMap = new ReadHandler[size];
		set(0, size-1, defread);
	}

	public void set(int from, int until, ReadHandler memRead) {
		for (int i = from; i <= until; i++) {
			this.readMap[i] = memRead;
		}
	}

	public int getSize() {
		return size;
	}

	public ReadHandler[] get() {
		return readMap;
	}

	public class UndefinedRead implements ReadHandler {
		@Override
		public int read(int address) {
			if (debug) System.out.println("Undefined Read at " + Integer.toHexString(address));
			return 0;
		}
	}

    @Override
	public int read(int address) {
        return readMap[address].read(address);
    }

}