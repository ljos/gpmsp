package cottage;

import java.net.URL;

import jef.machine.Machine;
import cottage.drivers.Pacman;
import cottage.mame.Driver;

public class CottageDriver {

	/* Returns a driver selected by name */
	public Machine getMachine(URL base_URL, String driver_name) {

		Driver d = null;

		try {
			d = (Driver) Class.forName("cottage.drivers.Pacman").newInstance();
		} catch (Exception e) {
			System.out.println("Bad!");
			System.out.println("ERROR : driver does not exist!");
		}

		return d.getMachine(base_URL, driver_name);
	}
}
