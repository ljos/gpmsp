package jef.video;

import jef.machine.MachineDriver;

/**
 * Interface class.
 * 
 * @author Erik Duijs
 * 
 *         VideoEmulator.java
 */
public interface VideoEmulator extends VideoConstants {

	public void init(MachineDriver md);
}