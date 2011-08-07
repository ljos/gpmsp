package cottage;

import java.net.URL;
import jef.machine.Machine;
import cottage.drivers.*;
import cottage.mame.Driver;

public class CottageDriver {

	/* Add new supported games here (game_name, driver_name, comments) */
	public static String[][] Supported_Games_List = {
		/* Bw8080 drivers */
//		{"280zzzap","Bw8080",  "Bad Input/Crashes"},
//		{"alieninv","Bw8080",  ""},
//		{"boothill","Bw8080",  "Crashes"},
//		{"checkmat","Bw8080",  ""},
//		{"cosmicmo","Bw8080",  ""},
//		{"earthinv","Bw8080",  ""},
//		{"gunfight","Bw8080",  ""},
//		{"invaderl","Bw8080",  ""},
//		{"invaders","Bw8080",  ""},
//		{"invadpt2","Bw8080",  ""},
//		{"invdpt2m","Bw8080",  ""},
//		{"invrvnga","Bw8080",  "Input Crashes"},
//		{"invrvnge","Bw8080",  "Input Crashes"},
//		{"jspecter","Bw8080",  ""},
//		{"jspectr2","Bw8080",  ""},
//		{"lagunar", "Bw8080",  "Bad Input/Crashes"},
//		{"m4",      "Bw8080",  ""},
//		{"maze",    "Bw8080",  ""},
//		{"phantom2","Bw8080",  ""},
//		{"seawolf", "Bw8080",  ""},
//		{"sinvemag","Bw8080",  ""},
//		{"sinvzen", "Bw8080",  ""},
//		{"spaceatt","Bw8080",  ""},
//		{"spaceat2","Bw8080",  ""},
//		{"spceking","Bw8080",  ""},
//		{"spcenctr","Bw8080",  ""},
//		{"spcewars","Bw8080",  ""},
//		{"spacewr3","Bw8080",  ""},
//		{"sicv",    "Bw8080",  ""},
//		{"sisv",    "Bw8080",  ""},
//		{"sisv2",   "Bw8080",  ""},
//		{"sitv",    "Bw8080",  "Crashes"},
//		{"superinv","Bw8080",  "Crashes"},

//		{"puckman", "Pacman_",  ""},
//		{"puckmana","Pacman_",  ""},
//		{"pacman",  "Pacman",  ""},
		{"mspacman","Pacman",  ""}, // MSPACMAN!!!
//		{"puckmod", "Pacman_",  ""},
//		{"pacmod",  "Pacman_",  ""},
//		{"hangly",  "Pacman_",  ""},
//		{"hangly2", "Pacman_",  ""},
//		{"newpuckx","Pacman_",  ""},
//		{"pacheart","Pacman_",  ""},
//		{"piranha", "Pacman_",  ""},
//		{"pacplus", "Pacman_",  ""},
//		{"mspacmab","Pacman_",  ""},
//		{"mspacpls","Pacman_",  ""},
//		{"pacgal",  "Pacman_",  ""},
//		{"crush2",  "Pacman_",  ""},
//		{"crush3",  "Pacman_",  ""},
//		{"mbrush",  "Pacman_",  ""},
//		{"paintrlr","Pacman_",  ""},
//		{"ponpoko", "Pacman",  ""},
//		{"ponpokov","Pacman_",  ""},
//		{"eyes",    "Pacman",  ""},
//		{"eyes2",   "Pacman_",  ""},
//		{"mrtnt",   "Pacman",  "Big glitches"},
//		{"lizwiz",  "Pacman",  ""},
//		{"jumpshot","Pacman_",  ""},

		/* Pacman drivers */
		//{"eyes",    "Pacman",  ""},
		//{"lizwiz",  "Pacman",  ""},
		//{"mrtnt",   "Pacman",  "Big glitches"},
		//DISABLED{"mspacman","Pacman",  ""},
		//{"pacman",  "Pacman",  ""},
		//{"ponpoko", "Pacman",  ""},
//		{"theglob", "Pacman",  "Game reboots"},
//		{"vanvan",  "Pacman",  ""},
//		{"dremshpr","Pacman",  ""},

		/* Galaxian drivers */
//		{"galaxian","Galaxian",""},
//		{"galaxiaj","Galaxian",""},
//		{"scramblb","Galaxian",""},
//		{"warofbug","Galaxian",""},

		/* Gyruss drivers */
//		{"gyruss",  "Gyruss",  "Glitches"},

		/* Solomon drivers */
//		{"solomon", "Solomon", ""},

		/* Bombjack drivers */
//		{"bombjack","Bombjack",""},
//		{"bombjac2","Bombjack",""},

		/* Black Tiger drivers */
//		{"blktiger","Blktiger","Small sprite lag"},
//		{"bktigerb","Blktiger","Small sprite lag"},
//		{"blkdrgon","Blktiger","Small sprite lag"},
//		{"blkdrgnb","Blktiger","Small sprite lag"},


		/* Yie Ar Kung-Fu drivers */
//		{"yiear",   "Yiear",   ""},
//		{"yiear2",  "Yiear",   ""},

		/* Mini Invaders drivers */
//		{"minivadr","Minivadr",""},

		/* Dottori Kun drivers */
//		{"dotrikun","Dotrikun",""},
//		{"dotriku2","Dotrikun",""},

		/* Galaga drivers */
//		{"galaga",  "Galaga",  "GameOver crashes"},

		/* News drivers */
//		{"news",    "News",    ""},

		/* News drivers */
//		{"hexa",    "Hexa",    "No dipswitches"},

		/* News drivers */
//		{"m79amb",  "M79amb",  ""},

		/* Donkey Kong drivers */
//		{"dkong",   "Dkong",   ""},
//		{"dkongo",  "Dkong",   ""},
//		{"dkongjp", "Dkong",   ""},
//		{"dkongjo", "Dkong",   ""},
//		{"dkongjo1","Dkong",   ""},
//		{"dkongjr", "Dkong",   ""},
//		{"dkongjrj","Dkong",   ""},
//		{"dkngjnrj","Dkong",   ""},
//		{"dkongjrb","Dkong",   ""},
//		{"dkngjnrb","Dkong",   ""},
//		{"dkong3",  "Dkong",   ""},
//		{"dkong3j", "Dkong",   ""},
//		{"radarscp","Dkong",   ""},

		/* 1943 drivers */
//		{"1943",	"_1943",   ""},
//		{"1943j",	"_1943",   ""},
//		{"1943kai",	"_1943",   ""},
//		{"gunsmoke","_1943",   ""},
//		{"gunsmokj","_1943",   ""},
//		{"gunsmoku","_1943",   ""},
//		{"gunsmoka","_1943",   ""},

		/* Roc 'n rope drivers */
//		{"rocnrope","Rocnrope",""},
//		{"rocnropk","Rocnrope",""},

		/* Hyper Sports drivers */
//		{"hyperspt","Hyperspt","Buggy scores/controls"},
//		{"hpolym84","Hyperspt","Buggy scores/controls"},
//		{"roadf",   "Hyperspt",""},
//		{"roadf2",  "Hyperspt",""},

		/* Safari Rally drivers */
//  	{"safarir", "Safarir", "Not working"},

		/* Bank Panic drivers */
//		{"bankp",   "Bankp",   ""},

		/* Circus Charlie drivers */
//		{"circusc", "Circusc", ""},
//		{"circusc2","Circusc", ""},
//		{"circuscc","Circusc", ""},
//		{"circusce","Circusc", ""},

		/* Green Beret drivers */
//		{"gberet",  "Gberet",  ""},
//		{"rushatck","Gberet",  ""},
//		{"mrgoemon","Gberet",  ""},

		/* Bubble Bobble drivers */
//		{"boblbobl","Bublbobl",""},
//		{"sboblbob","Bublbobl",""},
//		{"tokiob",  "Bublbobl",""},

		/* 1942 drivers */
//		{"1942",	"_1942",   ""},
//		{"1942a",	"_1942",   ""},
//		{"1942b",	"_1942",   ""},

		/* Pingpong drivers */
//		{"pingpong","Pingpong",""},

		/* Arkanoid drivers */
//		{"arkatayt","Arkanoid","Not working"},
//		{"arkbloc2","Arkanoid","Not working"},
//		{"arkangc", "Arkanoid","Not working"},

		/* Mario drivers */
//		{"mario",   "Mario",   ""},
//		{"mariojp", "Mario",   ""},
//		{"masao",   "Mario",   ""},

		/* Tropical Angel drivers */
//		{"troangel","Troangel",""},

		/* Mr. Jong drivers */
//		{"mrjong",  "Mrjong",  ""},
//		{"crazyblk","Mrjong",  ""},

		/* Pooyan drivers */
//		{"pooyan",  "Pooyan",  "Sprite bugs"},
//		{"pooyans", "Pooyan",  "Sprite bugs"},
//		{"pootan",  "Pooyan",  "Sprite bugs"},

		/* Sonson drivers */
//		{"sonson",  "Sonson",  ""},
//		{"sonsonj", "Sonson",  ""},

		/* M62 drivers */
//		{"ldrun",   "M62",     ""},

		/* Commando drivers */
//		{"commando","Commando","No sound"},

		/* Mitchell drivers */
//		{"pang",    "Mitchell","Preliminary"},
//		{"pangb",   "Mitchell","Preliminary"},

		/* Ghost'n Goblins drivers */
//		{"gng",     "Gng",     "Preliminary"},
//		{"gnga",    "Gng",     "Preliminary"},
//		{"gngt",    "Gng",     "Preliminary"},
//		{"makaimur","Gng",     "Preliminary"},
//		{"makaimuc","Gng",     "Preliminary"},
//		{"makaimug","Gng",     "Preliminary"},
//		{"diamond", "Gng",     "Preliminary"},

		/* Jr. Pac-Man drivers */
//		{"jrpacman","Jrpacman","Preliminary"},

		/* 4 En Raya drivers */
//		{"4enraya", "_4enraya","Preliminary"},
		
//		{"sf2", "Cps1", ""},
		{"",        ""} /* End of array */
	};

	/* Uncomment following drivers to compile and enable them */
	public void Supported_Drivers_List() {
//		new Bw8080();
//		new Pacman_();
		new Pacman();
//		new Galaxian();
//		new Gyruss();
//		new Solomon();
//		new Bombjack();
//		new Blktiger();
//		new Yiear();
//		new Minivadr();
//		new Dotrikun();
//		new Galaga();
//		new News();
//		new Hexa();
//		new M79amb();
//		new Dkong();
//		new _1943();
//		new Rocnrope();
//		new Hyperspt();
//		new Safarir();
//		new Bankp();
//		new Circusc();
//		new Gberet();
//		new Bublbobl();
//		new _1942();
//		new Pingpong();
//		new Arkanoid();
//		new Mario();
//		new Troangel();
//		new Mrjong();
		//new Rallyx();
//		new Pooyan();
//		new Sonson();
//		new M62();
//		new Commando();
//		new Mitchell();
//		new _4enraya();
//		new Gng();
//		new Mitchell();
//		new Jrpacman();
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

		System.out.print("Trying to select driver '" + driver_name + "'...");

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

		System.out.println("Ok!");

		return d.getMachine(base_URL, driver_name);
	}
}
