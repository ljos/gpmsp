package cottage.mame;

import java.net.URL;
import jef.machine.Machine;

/**
 * A Driver defines all characteristics of the emulated machine.
 * A Machine object is created in this class.
 */
public interface Driver {

/**
 * Initialize the driver.
 * The filename of the actual game is passed in case this driver supports more
 * than one game.
 * Returns the Machine Object
 */
	public Machine 	getMachine(URL url, String fname);

/**
 * Set a property of the driver, like a rendering method for example,
 * or sound volume. Like anything.
 */
	public void 	setProperty(int property, int value);

/**
 * Get a property from the driver.
 */
	public int 		getProperty(int property);

	public String	getDriverInfo();
	public String	getGameInfo();


}