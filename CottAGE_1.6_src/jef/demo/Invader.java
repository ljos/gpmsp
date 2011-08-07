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

import java.awt.event.KeyEvent;
import java.awt.*;
import java.net.URL;

import jef.video.GfxProducer;
import jef.machine.Machine;

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
 public class Invader extends GfxProducer {

/** reference to the Machine **/
    Machine m;

/** Here the real emulation takes place */
	InvEmulator im;

/** URL to the origin of the applet */
	URL base_URL;

/**
 * Send KeyEvents to the Machine object
 *  * @see java.awt.Component#processKeyEvent(KeyEvent) */
    protected void processKeyEvent(KeyEvent e) {
        int code = e.getKeyCode();
        switch (e.getID()) {

        case KeyEvent.KEY_PRESSED:
			m.keyPress(code);
            break;

        case KeyEvent.KEY_RELEASED:
            m.keyRelease(code);
            break;
        }
    }

	/**
	 * The main method
	 * 	 * @see jef.video.GfxProducer#main(int, int)
	 * @param w The width of the applet.
	 * @param h The height of the applet.	 */
    public void main(int w, int h) {
        try {
            base_URL = getDocumentBase();
        } catch(Exception e) {
		}

        // initialize the emulator and get a reference to it's Machine object.
        im = new InvEmulator();
        m = im.getMachine(base_URL, w, h);

        // enable keyboard input
        enableEvents(AWTEvent.KEY_EVENT_MASK);
        requestFocus();

        // Throttle to 60 fps
        jef.util.Throttle.init(60, getThread());

        // main loop
        while(true) {
			// do everything for one frame
			m.refresh(true);

			// get the back buffer if it's not skipped
			//if(!jef.util.Throttle.skipFrame()) update(m.getDisplay().getPixels());

			// slow down to the machine's original speed
			jef.util.Throttle.throttle();
        }
    }

}
