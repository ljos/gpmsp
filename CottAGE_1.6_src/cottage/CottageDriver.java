package cottage;

import java.net.URL;

import jef.machine.Machine;
import cottage.drivers.Pacman;
import cottage.mame.Driver;

public class CottageDriver {
	
	public static String[][] Supported_Games_List = {
		{"mspacman","Pacman",  ""}, // MSPACMAN!!!
		{"",        ""} /* End of array */
	};

	/* Uncomment following drivers to compile and enable them */
	public void Supported_Drivers_List() {
		new Pacman();
	}

	/* Sort game list for further dichotomic searching */
	public void sortGames()
	{
		int i,j,len;
		String aux;
		len = Supported_Games_List.length;
		/* Bubble-sort */
		for(i=0; i<len; i++) {
			for(j=i+1; j<len; j++) {
				if (Supported_Games_List[j][0].compareTo(Supported_Games_List[i][0])<0) {
					/* Swap entries */
					aux = Supported_Games_List[j][0];
					Supported_Games_List[j][0] = Supported_Games_List[i][0];
					Supported_Games_List[i][0] = aux;
					aux = Supported_Games_List[j][1];
					Supported_Games_List[j][1] = Supported_Games_List[i][1];
					Supported_Games_List[i][1] = aux;
				}
			}
		}
	}

	/* Lookup for a game name in the game list */
	public int lookupGame(String name) {
		int min = 0;
		int middle, oldmiddle;
		int max = Supported_Games_List.length;
		int res;

		/* Dichotomic search */
		middle = (max+min)>>1;
		do {
			res = Supported_Games_List[middle][0].compareTo(name);
			if (res == 0) {
				return middle;
			} else if (res < 0) {
				min = middle;
			} else if (res > 0) {
				max = middle;
			}
			oldmiddle = middle;
			middle = (max+min)>>1;
		} while (middle != oldmiddle);

		return -1;
	}

	/* Returns a driver selected by name */
	public Machine getMachine(URL base_URL, String driver_name) {

		Driver d = null;

	//	System.out.print("Trying to select driver '" + driver_name + "'...");

		/* Sort games before dichotomic search */
		sortGames();

		/* Dichotomic search of the game name */
		int res = lookupGame(driver_name);
		/* Game found ? */
		if (res != -1) {
			try {
				d = (Driver)Class.forName("cottage.drivers."+Supported_Games_List[res][1]).newInstance();
			} catch (Exception e) {
				System.out.println("Bad!");
				System.out.println("ERROR : '" + Supported_Games_List[res][1] + "' driver does not exist!");
				for(;;);
			}
		/* Game not found */
		} else {
			System.out.println("Bad!");
			System.out.println("ERROR : '" + driver_name + "' is not supported!");
			for(;;);
		}

//		System.out.println("Ok!");

		return d.getMachine(base_URL, driver_name);
	}
}
