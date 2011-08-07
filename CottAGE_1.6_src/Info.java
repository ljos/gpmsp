import cottage.*;
import cottage.mame.MAMEDriver;
import jef.machine.*;
import jef.video.*;

import java.io.*;

public class Info {

	static boolean bListClones = true;

	static void showUsage() {
		System.out.println("Usage: java Info [game] [options]");
		System.out.println("Options:");
		System.out.println("");
		System.out.println("-list / -ls                    List all supported games");
		System.out.println("-listfull / -ll                List all game names and descriptions");
		System.out.println("-listgames                     List all supported games, year, manufacturer");
		System.out.println("-listsourcefile                List all drivers source files");
		System.out.println("-gamelist                      Generate the gamelist file");
		System.out.println("-noclones                      Do not list alternate versions");
		System.out.println("-genhtml                       Generate HTML pages for all supported games");
		System.out.println("-showusage / -su               Show this help");
	}

	static void list() {
		int i,j;
		CottageDriver d = new CottageDriver();
		int len = d.Supported_Games_List.length - 1;
		String aux;

		System.out.println("");
		System.out.println("CottAGE currently supports the following games:");
		System.out.println("");
		for(i=0; i<len; i++) {
			aux = d.Supported_Games_List[i][0];
			for(j=d.Supported_Games_List[i][0].length(); j<8; j++)
				aux += " ";
			System.out.print(aux);
			if (((i+1)%8) == 0)
				System.out.println("");
			else
				System.out.print("  ");
		}
		if ((i%8) != 0)
			System.out.println("");
		System.out.println("");
		System.out.println("Total ROM sets supported: " + len);
	}

	static void listsourcefile() {
		int i,j;
		CottageDriver d = new CottageDriver();
		int len = d.Supported_Games_List.length - 1;
		String aux;

		for(i=0; i<len; i++) {
			aux = d.Supported_Games_List[i][0];
			for(j=d.Supported_Games_List[i][0].length(); j<9; j++)
				aux += " ";
			System.out.print(aux);
			System.out.print("cottage/drivers/"+d.Supported_Games_List[i][1]);
			System.out.println("");
		}
	}

	static void listfull() {
		int i,j;
		CottageDriver d = new CottageDriver();
		MAMEDriver dr = null;
		BasicMachine m = null;
		int len = d.Supported_Games_List.length - 1;
		String aux;

		System.out.println("Name:     Description:");
		for(i=0; i<len; i++) {
			aux = d.Supported_Games_List[i][0];
			for(j=d.Supported_Games_List[i][0].length(); j<10; j++)
				aux += " ";
			try {
				dr = (MAMEDriver)Class.forName("cottage.drivers."+d.Supported_Games_List[i][1]).newInstance();
			} catch (Exception e) {
				System.out.println("ERROR : '" + d.Supported_Games_List[i][1] + "' driver does not exist!");
				System.exit(1);
			}
			m = (BasicMachine)dr.getMachineInfo(d.Supported_Games_List[i][0]);
			if (bListClones || (dr.driver_clone == null))
				System.out.println(aux + "\"" + dr.driver_name + "\"");
		}
	}

	static void listgames() {
		int i,j;
		CottageDriver d = new CottageDriver();
		MAMEDriver dr = null;
		BasicMachine m = null;
		int len = d.Supported_Games_List.length - 1;
		String aux;
		String date;

		for(i=0; i<len; i++) {
			try {
				dr = (MAMEDriver)Class.forName("cottage.drivers."+d.Supported_Games_List[i][1]).newInstance();
			} catch (Exception e) {
				System.out.println("ERROR : '" + d.Supported_Games_List[i][1] + "' driver does not exist!");
				System.exit(1);
			}
			m = (BasicMachine)dr.getMachineInfo(d.Supported_Games_List[i][0]);
			date = dr.driver_date;
			if (date.compareTo("0")==0)
				date = "????";
			aux = dr.driver_prod;
			for(j=dr.driver_prod.length(); j<39; j++)
				aux += " ";
			if (bListClones || (dr.driver_clone == null))
				System.out.println(date + " " + aux + dr.driver_name);
		}
	}

	static void gamelist() {
		int i,j;
		CottageDriver d = new CottageDriver();
		MAMEDriver dr = null;
		BasicMachine m = null;
		int len = d.Supported_Games_List.length - 1;
		String aux1;
		String aux2;

		System.out.println("This is the complete list of games supported by CottAGE " + Cottage.VERSION + " (" + Cottage.RELEASE_DATE + ").");
		System.out.println("");
		System.out.println("This list is generated automatically and is not 100% accurate.");
		System.out.println("Please let us know of any errors so we can correct them.");
		System.out.println("");
		System.out.println("Here are the meanings of the columns:");
		System.out.println("");
		System.out.println("Internal Name");
		System.out.println("=============");
		System.out.println("  This is the unique name that must be used when running the game from a");
		System.out.println("  command line.");
		System.out.println("");
		System.out.println("  Note: Each game's ROM set must be placed in the roms path, in a .zip file.");
		System.out.println("");
		System.out.println("+-------------------------------------------------------------+----------+");
		System.out.println("|                                                             | Internal |");
		System.out.println("| Game Name                                                   |   Name   |");
		System.out.println("+-------------------------------------------------------------+----------+");

		for(i=0; i<len; i++) {
			try {
				dr = (MAMEDriver)Class.forName("cottage.drivers."+d.Supported_Games_List[i][1]).newInstance();
			} catch (Exception e) {
				System.out.println("ERROR : '" + d.Supported_Games_List[i][1] + "' driver does not exist!");
				System.exit(1);
			}
			m = (BasicMachine)dr.getMachineInfo(d.Supported_Games_List[i][0]);
			aux1 = dr.driver_name;
			for(j=dr.driver_name.length(); j<59; j++)
				aux1 += " ";
			aux2 = d.Supported_Games_List[i][0];
			for(j=d.Supported_Games_List[i][0].length(); j<8; j++)
				aux2 += " ";

			if (bListClones || (dr.driver_clone == null))
				System.out.println("| " + aux1 + " | " + aux2 + " |");
		}

		System.out.println("+-------------------------------------------------------------+----------+");

	}

	static void genhtml() {
		FileOutputStream file;
		PrintStream p;

		int i,j;
		CottageDriver d = new CottageDriver();
		MAMEDriver dr = null;
		BasicMachine m = null;
		int len = d.Supported_Games_List.length - 1;
		String driver;
		int width = 0;
		int height = 0;
		int count_clones = 0;
		String[] names = new String[len];
		String[] orgnames = new String[len];
		String[] comments = new String[len];
		String[] clones = new String[len];
		boolean[] sound = new boolean[len];

		int iname = 0;

		/* Build index.htm */
		try {
			file = new FileOutputStream("index.htm");
			p = new PrintStream( file );
			p.println ("<HTML>");
			p.println ("");
			p.println ("<HEAD>");
			p.println ("<META HTTP-EQUIV=\"Content-Type\" CONTENT=\"text/html; charset=iso-8859-1\">");
			p.println("<TITLE>CottAGE</TITLE>");
			p.println ("</HEAD>");
			p.println ("<FRAMESET COLS=\"310,*\" FRAMEBORDER=NO BORDER=0 FRAMESPACING=0>");
			p.println ("<FRAME SRC=\"menu.htm\" NAME=\"menu\" MARGINWIDTH=1 FRAMEBORDER=NO BORDER=0 MARGINHEIGHT=0 SCROLLING=YES NORESIZE>");
			p.println ("<FRAME SRC=\"invaders.htm\" NAME=\"cottage\" MARGINWIDTH=0 MARGINHEIGHT=0>");
			p.println ("</FRAMESET>");
			p.println ("<NOFRAMES>");
			p.println ("<BODY BACKGROUND=\"cottage.jpg\">");
			p.println ("Viewing this page requires a browser capable of displaying frames.");
			p.println ("</BODY>");
			p.println ("</NOFRAMES>");
			p.println ("</HTML>");
			p.close();
		} catch (Exception e) { System.err.println ("Error writing to file"); }

		/* Build htm pages for all supported games */
		for(i=0; i<len; i++) {
			try {
				dr = (MAMEDriver)Class.forName("cottage.drivers."+d.Supported_Games_List[i][1]).newInstance();
			} catch (Exception e) {
				System.out.println("ERROR : '" + d.Supported_Games_List[i][1] + "' driver does not exist!");
				System.exit(1);
			}
			driver = d.Supported_Games_List[i][0];
			m = (BasicMachine)dr.getMachineInfo(driver);

			names[iname] = dr.driver_name;
			orgnames[iname] = d.Supported_Games_List[i][0];
			comments[iname] = d.Supported_Games_List[i][2];
			clones[iname] = dr.driver_clone;
			sound[iname] = dr.driver_sound;
			iname++;
			if (dr.driver_clone != null)
				count_clones++;

			switch(m.md.ROT) {
			case GfxManager.ROT0:
			case GfxManager.ROT180:
				width = ((m.md.visible[1]+1) - m.md.visible[0]);
				height = ((m.md.visible[3]+1) - m.md.visible[2]);
				break;
			case GfxManager.ROT90:
			case GfxManager.ROT270:
				height = ((m.md.visible[1]+1) - m.md.visible[0]);
				width = ((m.md.visible[3]+1) - m.md.visible[2]);
				break;
			}

			try {
				file = new FileOutputStream(driver + ".htm");
				p = new PrintStream( file );
				p.println ("<HTML>");
				p.println ("");
				p.println ("<HEAD>");
				p.println ("<META HTTP-EQUIV=\"Content-Type\" CONTENT=\"text/html; charset=iso-8859-1\">");
				p.println("<TITLE>CottAGE</TITLE>");
				p.println ("</HEAD>");
				p.println ("<BODY BACKGROUND=\"cottage.jpg\">");
				p.println ("<TABLE WIDTH=\"100%\" HEIGHT=\"100%\">");
				p.println ("<TR>");
				p.println ("<TD WIDTH=\"100%\" VALIGN=MIDDLE ALIGN=CENTER>");
				p.println ("<BR>");
				p.println ("<APPLET CODE=\"cottage.Cottage.class\" ARCHIVE=\"Cottage.jar\" WIDTH=\""+width+"\" HEIGHT=\""+height+"\" DRIVER=\""+driver+"\">");
				p.println ("Your browser does not support Java...");
				p.println ("</APPLET>");
				p.println ("</TD>");
				p.println ("</TR>");
				p.println ("</TABLE>");
				p.println ("</BODY>");
				p.println ("</HTML>");
				p.close();
			} catch (Exception e) { System.err.println ("Error writing to file"); }
		}

		/* Build menu.htm */
		try {
			file = new FileOutputStream("menu.htm");
			p = new PrintStream( file );
			p.println ("<HTML>");
			p.println ("<HEAD>");
			p.println ("<META HTTP-EQUIV=\"Content-Type\" CONTENT=\"text/html; charset=iso-8859-1\">");
			p.println ("<TITLE>CottAGE</TITLE>");
			p.println ("</HEAD>");
			p.println ("<BODY BGCOLOR=WHITE TEXT=\"#000000\" LINK=\"#000000\" VLINK=\"#000000\" ALINK=\"#000000\">");
			p.println ("<P ALIGN=CENTER>");
			p.println ("<FONT COLOR=\"#FFBB00\" SIZE=\"4\" FACE=\"Verdana\">");
			p.println ("<B>CottAGE " + Cottage.VERSION + "</B>");
			p.println ("</FONT>");
			p.println ("</P>");
			p.println ("<FONT FACE=\"Tahoma\" size=\"2\"><B>" + len + " ROM sets</B></FONT><BR>");
			p.println ("<FONT FACE=\"Tahoma\" size=\"2\"><B>" + (len - count_clones) + " Unique games</B></FONT><BR>");
			p.println ("<FONT FACE=\"Tahoma\" size=\"2\" COLOR=GRAY><B>" + count_clones + " Clones</B></FONT><BR>");
			p.println ("<BR>");
			p.println ("<TABLE BORDER=1 CELLSPACING=0 CELLPADDING=0>");
			p.println ("<TH BGCOLOR=LIGHTGREEN>");
			p.println ("<FONT SIZE=\"1\" FACE=\"Tahoma\">KEYS</FONT>");
			p.println ("</TH>");
			p.println ("<TR><TD>&nbsp;");
			p.println ("<FONT SIZE=\"1\" FACE=\"Tahoma\"><B>1</B> - Player 1 Start</FONT>");
			p.println ("</TD></TR>");
			p.println ("<TR><TD>&nbsp;");
			p.println ("<FONT SIZE=\"1\" FACE=\"Tahoma\"><B>2</B> - Player 2 Start</FONT>");
			p.println ("</TD></TR>");
			p.println ("<TR><TD>&nbsp;");
			p.println ("<FONT SIZE=\"1\" FACE=\"Tahoma\"><B>5</B> - Insert Coin Player 1</FONT>");
			p.println ("</TD></TR>");
			p.println ("<TR><TD>&nbsp;");
			p.println ("<FONT SIZE=\"1\" FACE=\"Tahoma\"><B>6</B> - Insert Coin Player 2</FONT>");
			p.println ("</TD></TR>");
			p.println ("<TR><TD>&nbsp;");
			p.println ("<FONT SIZE=\"1\" FACE=\"Tahoma\"><B>UP/DOWN/LEFT/RIGHT</B> - P1 Pad</FONT>");
			p.println ("</TD></TR>");
			p.println ("<TR><TD>&nbsp;");
			p.println ("<FONT SIZE=\"1\" FACE=\"Tahoma\"><B>R/F/D/G</B> - P2 Pad</FONT>");
			p.println ("</TD></TR>");
			p.println ("<TR><TD>&nbsp;");
			p.println ("<FONT SIZE=\"1\" FACE=\"Tahoma\"><B>CTRL/SPACE/Z/X</B> - P1 Buttons</FONT>");
			p.println ("</TD></TR>");
			p.println ("<TR><TD>&nbsp;");
			p.println ("<FONT SIZE=\"1\" FACE=\"Tahoma\"><B>A/S/Q/W</B> - P2 Buttons</FONT>");
			p.println ("</TD></TR>");
			p.println ("<TR><TD>&nbsp;");
			p.println ("<FONT SIZE=\"1\" FACE=\"Tahoma\"><B>P</B> - Pause Emulation</FONT>");
			p.println ("</TD></TR>");
			p.println ("<TR><TD>&nbsp;");
			p.println ("<FONT SIZE=\"1\" FACE=\"Tahoma\"><B>F8</B> - Decrease Frame Skipping<BR>Enable Auto Frame Skipping When 0</FONT>");
			p.println ("</TD></TR>");
			p.println ("<TR><TD>&nbsp;");
			p.println ("<FONT SIZE=\"1\" FACE=\"Tahoma\"><B>F9</B> - Increase Frame Skipping<BR>Disable Auto Frame Skipping</FONT>");
			p.println ("</TD></TR>");
			p.println ("<TR><TD>&nbsp;");
			p.println ("<FONT SIZE=\"1\" FACE=\"Tahoma\"><B>F10</B> - Toggle Speed Throttling</FONT>");
			p.println ("</TD></TR>");
			p.println ("<TR><TD>&nbsp;");
			p.println ("<FONT SIZE=\"1\" FACE=\"Tahoma\"><B>F11</B> - Toggle Speed Display</FONT>");
			p.println ("</TD></TR>");
			p.println ("<TR><TD>&nbsp;");
			p.println ("<FONT SIZE=\"1\" FACE=\"Tahoma\"><B>F12</B> - Toggle Sound</FONT>");
			p.println ("</TD></TR>");
			p.println ("</TABLE>");
			p.println ("<FONT COLOR=RED FACE=\"Tahoma\" SIZE=\"1\"><B>N.B:</B> keep in mind that this emulator is still in beta stages!</FONT><BR>");
			p.println ("<BR>");
			p.println ("<HR WIDTH=\"100%\" SIZE=\"1\">");
			p.println ("<BR>");
			p.println ("<TABLE BORDER=\"1\" BORDERCOLOR=\"#EFEFEF\" CELLPADDING=\"0\" CELLSPACING=\"0\" WIDTH=\"100%\">");

			/* Sort things */
			String saux;
			boolean baux;

			/* Fake sorting by prefixing clones with parent driver */
			for(i=0; i<len; i++) {
				if (clones[i] != null) {
					j = 0;
					saux = null;
					while((j<len)&&(saux == null)) {
						if (clones[i] == orgnames[j])
							saux = names[j];
						j++;
					}
					names[i] = saux+"@"+names[i];
				}
			}

			/* Bubble-sort */
			for(i=0; i<len; i++) {
				for(j=i+1; j<len; j++) {
					if (names[j].compareTo(names[i])<0) {
						/* Swap entries */
						saux = names[j]; names[j] = names[i]; names[i] = saux;
						saux = orgnames[j]; orgnames[j] = orgnames[i]; orgnames[i] = saux;
						saux = comments[j]; comments[j] = comments[i]; comments[i] = saux;
						saux = clones[j]; clones[j] = clones[i]; clones[i] = saux;
						baux = sound[j]; sound[j] = sound[i]; sound[i] = baux;
					}
				}
			}

			/* Unfake sorting by removing the parent driver prefix */
			for(i=0; i<len; i++) {
				if (clones[i] != null)
					names[i] = names[i].substring(names[i].indexOf('@')+1);
			}

			for(i=0; i<len; i++) {
				p.println ("<TR>");
				p.println ("<TD WIDTH=\"65%\">");
				if (clones[i] != null)
					p.println("&nbsp;&nbsp;");
				p.println ("<A HREF=\"" + orgnames[i] + ".htm\" TARGET=\"cottage\">");
				if (clones[i] != null)
					p.println ("<FONT SIZE=\"1\" FACE=\"Tahoma\" COLOR=GRAY>");
				else
					p.println ("<FONT SIZE=\"1\" FACE=\"Tahoma\">");
				p.println ("<STRONG>" + names[i] + " </STRONG>");
				p.println ("</FONT>");
				p.println ("</A>");
				if (sound[i])
					p.println (" <IMG SRC=\"sound.gif\" ALIGN=ABSMIDDLE>");
				p.println ("</TD>");
      	  		p.println ("<TD WIDTH=\"35%\">");
				p.println ("<FONT COLOR=\"#0000FF\" SIZE=\"1\" FACE=\"Tahoma\">");
				p.println (comments[i]);
				p.println ("</FONT>");
				p.println ("</TD>");
				p.println ("</TR>");
			}

			p.println ("</TABLE>");
			p.println ("<BR>");
			p.println ("<HR WIDTH=\"100%\" SIZE=\"1\">");
			p.println ("<FONT COLOR=RED FACE=\"Tahoma\" SIZE=\"1\">");
			p.println ("<B>WARNING!</B><BR>");
			p.println ("Some ROMs available on this site are copyrighted. We strongly discourage you to try to download them. Using ROMs that you do not own is illegal!</FONT><BR>");
			p.println ("</BODY>");
			p.println ("</HTML>");
			p.close();
		} catch (Exception e) { System.err.println ("Error writing to file"); }

	}

	/* undocumented function to generate html pages for the Official CottAGE site */
	static void genhtmlweb()
	{
		FileOutputStream file = null;
		PrintStream p;

		int i,j;
		CottageDriver d = new CottageDriver();
		MAMEDriver dr = null;
		BasicMachine m = null;
		int len = d.Supported_Games_List.length - 1;
		String driver;
		int width = 0;
		int height = 0;
		int count_clones = 0;
		String[] names = new String[len];
		String[] selnames = new String[len];
		int[] selindexes = new int[len];
		int[] newdate = new int[len];
		String[] dates = new String[len];
		String[] prods = new String[len];
		String[] orgnames = new String[len];
		String[] comments = new String[len];
		String[] clones = new String[len];
		boolean[] sound = new boolean[len];
		char letter;

		int iname = 0;

		/* Build htm pages for all supported games */
		for(i=0; i<len; i++) {
			try {
				dr = (MAMEDriver)Class.forName("cottage.drivers."+d.Supported_Games_List[i][1]).newInstance();
			} catch (Exception e) {
				System.out.println("ERROR : '" + d.Supported_Games_List[i][1] + "' driver does not exist!");
				System.exit(1);
			}
			driver = d.Supported_Games_List[i][0];
			m = (BasicMachine)dr.getMachineInfo(driver);

			letter = dr.driver_name.charAt(0);
			if ((letter>='a')&&(letter<='z'))
				dr.driver_name = (char)(letter-32)+dr.driver_name.substring(1);

			letter = dr.driver_prod.charAt(0);
			if ((letter>='a')&&(letter<='z'))
				dr.driver_prod = (char)(letter-32)+dr.driver_prod.substring(1);

			if (dr.driver_prod.compareTo("Bootleg") == 0)
				dr.driver_prod = " bootleg";

			if (dr.driver_date.compareTo("0") == 0)
				dr.driver_date = "19??";

			names[iname] = dr.driver_name;
			dates[iname] = dr.driver_date;
			prods[iname] = dr.driver_prod;
			orgnames[iname] = d.Supported_Games_List[i][0];
			comments[iname] = d.Supported_Games_List[i][2];
			clones[iname] = dr.driver_clone;
			sound[iname] = dr.driver_sound;
			iname++;
			if (dr.driver_clone != null)
				count_clones++;

			switch(m.md.ROT) {
			case GfxManager.ROT0:
			case GfxManager.ROT180:
				width = ((m.md.visible[1]+1) - m.md.visible[0]);
				height = ((m.md.visible[3]+1) - m.md.visible[2]);
				break;
			case GfxManager.ROT90:
			case GfxManager.ROT270:
				height = ((m.md.visible[1]+1) - m.md.visible[0]);
				width = ((m.md.visible[3]+1) - m.md.visible[2]);
				break;
			}

			try {

				for(int mode=0; mode<4; mode++) {
				
				switch(mode) {
				case 0: file = new FileOutputStream(driver + ".htm"); break; // NORMAL
				case 1: file = new FileOutputStream(driver + "_d.htm"); break; // 2X SIZE
				case 2: file = new FileOutputStream(driver + "_t.htm"); break; // TV MODE
				case 3: file = new FileOutputStream(driver + "_s.htm"); break; // SCALE2X EFFECT
				}

				p = new PrintStream( file );

				p.println ("<HTML>");
				p.println ("");
				p.println ("<HEAD>");
				p.println ("<META HTTP-EQUIV=\"Content-Type\" CONTENT=\"text/html; charset=iso-8859-1\">");
				p.println ("<TITLE>CottAGE</TITLE>");
				p.println ("<SCRIPT LANGUAGE=\"JavaScript\">");
				p.println ("<!-- Begin");
				p.println ("function addbookmark()");
				p.println ("{");
				p.println ("bookmarkurl=\"http://cottage.emuunlim.com/cottage/" + driver + ".htm\"");
				p.println ("bookmarktitle=\"CottAGE - " + dr.driver_name + "\"");
				p.println ("if (document.all)");
				p.println ("window.external.AddFavorite(bookmarkurl,bookmarktitle)");
				p.println ("}");
				p.println ("//  End -->");
				p.println ("</SCRIPT>");
				p.println ("</HEAD>");
				p.println ("<BODY BACKGROUND=\"cottage.gif\" MARGINWIDTH=0 MARGINHEIGHT=0 MARGINLEFT=0 MARGINTOP=0>");
				p.println ("");
				p.println ("<TABLE BORDER=1 BORDERCOLOR=BLACK CELLSPACING=0 WIDTH=75% ALIGN=CENTER>");
				p.println ("<TR>");
				p.println ("<TD ALIGN=CENTER BGCOLOR=WHITE>");
				p.println ("<FONT COLOR=RED SIZE=2 FACE=\"Tahoma\">");
				p.println ("<B>" + dr.driver_name + "</B>");
				p.println ("</FONT>");
				p.println ("<FONT COLOR=BLACK SIZE=1 FACE=\"Tahoma\">");
				p.println ("© " + dr.driver_date + " " + dr.driver_prod);
				p.println ("</FONT>");
				p.println ("</TD>");
				p.println ("</TR>");
				p.println ("</TABLE>");
				p.println ("");
				p.println ("<BR>");
				p.println ("");

				p.println ("<TABLE WIDTH=100% BORDER=0>");
				p.println ("<TR>");
				p.println ("<TD ALIGN=LEFT>");
				p.println ("<TABLE BORDER=1 BORDERCOLOR=BLACK CELLSPACING=0>");
				p.println ("<TH BGCOLOR=LIGHTGREEN>");
				p.println ("<FONT SIZE=1 FACE=\"Tahoma\">KEYS</FONT>");
				p.println ("</TH>");
				p.println ("<TR>");
				p.println ("<TD BGCOLOR=WHITE>");
				p.println ("<FONT SIZE=1 FACE=\"Tahoma\">");
				p.println ("<B>1</B> - P1 Start<BR>");
				p.println ("<B>2</B> - P2 Start<BR>");
				p.println ("<B>5</B> - Insert Coin P1<BR>");
				p.println ("<B>6</B> - Insert Coin P2<BR>");
				p.println ("<B>UP/DOWN/LEFT/RIGHT</B> - P1 Pad<BR>");
				p.println ("<B>R/F/D/G</B> - P2 Pad<BR>");
				p.println ("<B>CTRL/SPACE/Z/X</B> - P1 Buttons<BR>");
				p.println ("<B>A/S/Q/W</B> - P2 Buttons<BR>");
				p.println ("<B>P</B> - Pause Emulation<BR>");
				p.println ("<B>F8</B> - Decrease FrameSkip<BR>");
				p.println ("<B>F9</B> - Increase FrameSkip<BR>");
				p.println ("<B>F10</B> - Toggle Speed Throttle<BR>");
				p.println ("<B>F11</B> - Toggle Speed Display<BR>");
				p.println ("<B>F12</B> - Toggle Sound<BR>");
				p.println ("<BR>");
				p.println ("</FONT>");
				p.println ("</TD>");
				p.println ("</TR>");

				if (d.Supported_Games_List[i][2] != "")
				{
					p.println ("<TH BGCOLOR=LIGHTGREEN>");
					p.println ("<FONT SIZE=1 FACE=\"Tahoma\">BUGS</FONT>");
					p.println ("</TH>");
					p.println ("<TR>");
					p.println ("<TD ALIGN=CENTER BGCOLOR=WHITE>");
					p.println ("<FONT SIZE=1 FACE=\"Tahoma\">");
					p.println (d.Supported_Games_List[i][2]);
					p.println ("<BR><BR>");
					p.println ("</FONT>");
					p.println ("</TD>");
					p.println ("</TR>");
				}

				p.println ("<TH BGCOLOR=LIGHTGREEN>");
				p.println ("<FONT SIZE=1 FACE=\"Tahoma\">MENU</FONT>");
				p.println ("</TH>");
				p.println ("<TR>");
				p.println ("<TD ALIGN=CENTER BGCOLOR=WHITE>");
				p.println ("<FONT SIZE=1 FACE=\"Tahoma\">");

				if (mode==0)
					p.println ("<B>=&gt; NORMAL &lt;=</B><BR>");
				else
					p.println ("<A HREF=\""+driver + ".htm\"><B>NORMAL</B></A><BR>");

				if (mode==1)
					p.println ("<B>=&gt; 2X SIZE &lt;=</B><BR>");
				else
					p.println ("<A HREF=\""+driver + "_d.htm\"><B>2X SIZE</B></A><BR>");

				if (mode==2)
					p.println ("<B>=&gt; TV MODE [SLOW] &lt;=</B><BR>");
				else
					p.println ("<A HREF=\""+driver + "_t.htm\"><B>TV MODE [SLOW]</B></A><BR>");

				if (mode==3)
					p.println ("<B>=&gt; SCALE2X [SLOW] &lt;=</B><BR>");
				else
					p.println ("<A HREF=\""+driver + "_s.htm\"><B>SCALE2X [SLOW]</B></A><BR>");

				p.println ("<HR WIDTH=50%>");
				p.println ("<A HREF=\"Javascript:addbookmark()\"><B>BOOKMARK GAME</B></A><BR>");
				p.println ("<A HREF=\"JavaScript:window.close()\"><B>CLOSE WINDOW</B></A><BR>");
				p.println ("</FONT>");
				p.println ("<BR>");
				p.println ("<IMG SRC=\"http://cottage.emuunlim.com/cgi-bin/count.pl\">");
				p.println ("</TD>");
				p.println ("</TR>");

				p.println ("</TABLE>");
				p.println ("</TD>");
				p.println ("<TD ALIGN=\"LEFT\">");
				p.println ("<TABLE BORDER=0 CELLSPACING=0 CELLPADDING=0>");
				p.println ("<TR>");
				p.println ("<TD>");

				if (mode==0) {
					p.println ("<APPLET CODE=\"cottage.Cottage.class\" ARCHIVE=\"Cottage.jar\" WIDTH=\""+width+"\" HEIGHT=\""+height+"\" DRIVER=\""+driver+"\">");
				}

				if (mode==1) {
					int n = height%256;
					if (n!=0)
						n = height + 256 - n;
					else
						n = height;
					p.println ("<APPLET CODE=\"cottage.Cottage.class\" ARCHIVE=\"Cottage.jar\" WIDTH=\""+width*2+"\" HEIGHT=\""+n*2+"\" DRIVER=\""+driver+"\">");
					p.println ("<PARAM NAME=\"ZOOM\" VALUE=\"Yes\">");
					p.println ("<PARAM NAME=\"REALHEIGHT\" VALUE=\""+height*2+"\">");
				}
				
				if (mode==2) {
					p.println ("<APPLET CODE=\"cottage.Cottage.class\" ARCHIVE=\"Cottage.jar\" WIDTH=\""+width*2+"\" HEIGHT=\""+height*2+"\" DRIVER=\""+driver+"\">");
					p.println ("<PARAM NAME=\"DOUBLE\" VALUE=\"Yes\">");
				}

				if (mode==3) {
					p.println ("<APPLET CODE=\"cottage.Cottage.class\" ARCHIVE=\"Cottage.jar\" WIDTH=\""+width*2+"\" HEIGHT=\""+height*2+"\" DRIVER=\""+driver+"\">");
					p.println ("<PARAM NAME=\"SCALE2X\" VALUE=\"Yes\">");
				}

				p.println ("Your browser does not support Java...");
				p.println ("</APPLET>");
				p.println ("</TD>");
				p.println ("</TR>");
				p.println ("</TABLE>");
				p.println ("</TD>");
				p.println ("</TR>");
				p.println ("</TABLE>");
				p.println ("</BODY>");
				p.println ("</HTML>");

				p.close();

				}

			} catch (Exception e) { System.err.println ("Error writing to file"); }
		}

		String saux;
		boolean baux;
		int iaux;
		int[] count = new int[33];
		String common;
		int tcount;
		int page;
		int next;
		int cmp;
		int lenc;

		/* Build screens.htm */

		/* Bubble-sort */
		for(i=0; i<len; i++) {
			for(j=i+1; j<len; j++) {
				if (names[j].compareTo(names[i])<0) {
					/* Swap entries */
					saux = names[j]; names[j] = names[i]; names[i] = saux;
					saux = dates[j]; dates[j] = dates[i]; dates[i] = saux;
					saux = prods[j]; prods[j] = prods[i]; prods[i] = saux;
					saux = orgnames[j]; orgnames[j] = orgnames[i]; orgnames[i] = saux;
					saux = comments[j]; comments[j] = comments[i]; comments[i] = saux;
					saux = clones[j]; clones[j] = clones[i]; clones[i] = saux;
					baux = sound[j]; sound[j] = sound[i]; sound[i] = baux;
				}
			}
		}

		for(i=0; i<len; i++) {
			letter = names[i].charAt(0);
			if ((letter < 'A') || (letter > 'Z'))
				count[0]++;
			else
				count[letter+1 - 'A']++;
		}

		page = 0;
		tcount = 0;
		common = "";
		for(i=0; i<27; i++) {
			if (i == 0)
				letter = '#' ;
			else
				letter = (char)('A'+i-1);

			if (count[i] > 0) {
				common += "<FONT FACE=\"Verdana\" COLOR=GREEN SIZE=+1>";
				if (page == 0)
					common += "<A HREF=\"screens.htm";
				else
					common += "<A HREF=\"screens" + page + ".htm";
				if (i == 0)
					common += "#letterx\">";
				else
					common += "#letter" + letter + "\">";
			} else
				common += "<FONT FACE=\"Verdana\" COLOR=GRAY SIZE=+1>";

			common += letter;

			if (count[i] > 0)
				common += "</A></FONT> ";
			else
				common += " </FONT>";

			tcount += count[i];
			if (tcount >= 8) {
				page+=tcount/8;
				tcount = tcount%8;
			}

		}

		j=0;
		i=0;
		while (i<len) {

		try {
			if (j>0)
				file = new FileOutputStream("screens" + j + ".htm");
			else
				file = new FileOutputStream("screens.htm");
			p = new PrintStream( file );
			p.println ("<HTML>");
			p.println ("");
			p.println ("<HEAD>");
			p.println ("<SCRIPT LANGUAGE=\"JavaScript\">");
			p.println ("<!-- Begin");
			p.println ("function cottage(gameName)");
			p.println ("{");
			p.println ("window.open(gameName,null,\"'scrollbar=no,menubar=no,toolbar=no,personalbars=no,scrollbar=no,status=no,fullscreen=yes\");");
			p.println ("}");
			p.println ("//  End -->");
			p.println ("</SCRIPT>");
			p.println ("");
			p.println ("<META HTTP-EQUIV=\"Content-Type\" CONTENT=\"text/html; charset=iso-8859-1\">");
			p.println ("<TITLE>CottAGE</TITLE>");
			p.println ("</HEAD>");
			p.println ("<BODY BACKGROUND=\"scanline.gif\" TEXT=\"#FFFFFF\" LINK=\"#FFAA00\" VLINK=\"#BB7700\" MARGINWIDTH=0 MARGINHEIGHT=0>");
			p.println ("");
			p.println ("<FONT FACE=\"Verdana\" COLOR=GREEN SIZE=+2><B>CHOOSE A GAME</B></FONT>");
			p.println ("<BR>");
			p.println ("<FONT FACE=\"Verdana\" COLOR=GREEN SIZE=+1>");
			p.println ("All ");
			p.println ("<A HREF=\"screens_bug.htm\">Buggy</A> ");
			p.println ("<A HREF=\"screens_clon.htm\">Clones</A> ");
			p.println ("<A HREF=\"screens_prod.htm\">Manufacturer</A> ");
			p.println ("<A HREF=\"screens_org.htm\">Originals</A> ");
			p.println ("<A HREF=\"screens_work.htm\">Working</A> ");
			p.println ("<A HREF=\"screens_year.htm\">Year</A> ");
			p.println ("</FONT>");
			p.println ("<BR>");
			p.println (common);
			p.println ("<BR><BR>");
			p.println ("");
			p.println ("<TABLE WIDTH=100% BORDER=1 CELLSPACING=0 CELLPADDING=0>");

			p.println ("</TABLE>");
			p.println ("<BR><BR>");
			p.println ("<FONT FACE=\"Verdana\" COLOR=GREEN SIZE=+1>");
			letter = names[i].charAt(0);
			if ((letter < 'A') || (letter > 'Z'))
				p.println ("<A NAME=\"letterx\">");
			else
				p.println ("<A NAME=\"letter" + letter +"\">");
			p.println (letter);
			p.println ("</A>");
			p.println ("</FONT>");
			p.println ("<BR><BR>");
			p.println ("<TABLE WIDTH=100% BORDER=1 CELLSPACING=0 CELLPADDING=0>");

			int row = 0;
			int k = 0;
			while((i<len)&&(k<8)) {
				letter = names[i].charAt(0);
				if ((k>0) && (letter != names[i-1].charAt(0))) {
					if (row == 0)
						p.println ("</TR>");
					p.println ("</TABLE>");
					p.println ("<BR><BR>");
					p.println ("<FONT FACE=\"Verdana\" COLOR=GREEN SIZE=+1>");
					if ((letter < 'A') || (letter > 'Z'))
						p.println ("<A NAME=\"letterx\">");
					else
						p.println ("<A NAME=\"letter" + letter +"\">");
					p.println (letter);
					p.println ("</A>");
					p.println ("</FONT>");
					p.println ("<BR><BR>");
					p.println ("<TABLE WIDTH=100% BORDER=1 CELLSPACING=0 CELLPADDING=0>");
					row = 0;
				}

				if (row == 0)
					p.println ("<TR>");

				p.println ("<TD ALIGN=CENTER VALIGN=MIDDLE>");
				p.println ("<A HREF=\"javascript:cottage('cottage/" + orgnames[i] + ".htm')\">");
				p.println ("<IMG SRC=\"" + orgnames[i] + ".gif\">");
				p.println ("</A><BR>");
				p.println (names[i] + "<BR>");
				p.println ("© " + dates[i] + " " + prods[i]);

				if (clones[i] != null) {
					p.println ("<BR>");
					p.println ("<B>GAME CLONE</B>");
				}

				if (sound[i]) {
					p.println("<BR>");
					p.println("<IMG SRC=\"sound.gif\">");
				}

				p.println ("</TD>");

				if (row == 1) {
					p.println ("</TR>");
					row = 0;
				} else
					row = 1;

				i++; k++;
			}

			if (row == 1)
				p.println ("</TR>");

			p.println ("</TABLE>");

			p.println ("<P ALIGN=CENTER>");
			p.println ("<FONT FACE=\"Verdana\" COLOR=GREEN SIZE=+1>");

			if (j>0) {
				if (j == 1)
					p.println ("<A HREF=\"screens.htm\">PREV 8</A>");
				else
					p.println ("<A HREF=\"screens" + (j-1) + ".htm\">PREV 8</A>");
			}

			next = len - i;
			if (next > 0) {
				next = (next < 8) ? next : 8;
				p.println ("<A HREF=\"screens" + (j+1) + ".htm\">NEXT " + next + "</A>");
			}

			p.println ("</FONT>");
			p.println ("</P>");

			p.println ("");
			p.println ("<P ALIGN=CENTER><IMG SRC=\"http://cottage.emuunlim.com/cgi-bin/count.pl\"></P>");
			p.println ("");
			p.println ("</BODY>");
			p.println ("</HTML>");
			p.close();

		} catch (Exception e) { System.err.println ("Error writing to file"); }

		j++;

		}


		/* Build screens_clon.htm */

		lenc = 0;
		for(i=0; i<len; i++) {
			if (clones[i] != null) {
				selnames[lenc] = names[i];
				selindexes[lenc++] = i;
			}
		}

		/* Bubble-sort */
		for(i=0; i<lenc; i++) {
			for(j=i+1; j<lenc; j++) {
				if (selnames[j].compareTo(selnames[i])<0) {
					/* Swap entries */
					saux = selnames[j]; selnames[j] = selnames[i]; selnames[i] = saux;
					iaux = selindexes[j]; selindexes[j] = selindexes[i]; selindexes[i] = iaux;
				}
			}
		}

		for(i=0; i<lenc; i++) {
			letter = selnames[i].charAt(0);
			if ((letter < 'A') || (letter > 'Z'))
				count[0]++;
			else
				count[letter+1 - 'A']++;
		}

		page = 0;
		tcount = 0;
		common = "";
		for(i=0; i<27; i++) {
			if (i == 0)
				letter = '#' ;
			else
				letter = (char)('A'+i-1);

			if (count[i] > 0) {
				common += "<FONT FACE=\"Verdana\" COLOR=GREEN SIZE=+1>";
				if (page == 0)
					common += "<A HREF=\"screens_clon.htm";
				else
					common += "<A HREF=\"screens_clon" + page + ".htm";
				if (i == 0)
					common += "#letterx\">";
				else
					common += "#letter" + letter + "\">";
			} else
				common += "<FONT FACE=\"Verdana\" COLOR=GRAY SIZE=+1>";

			common += letter;

			if (count[i] > 0)
				common += "</A></FONT> ";
			else
				common += " </FONT>";

			tcount += count[i];
			if (tcount >= 8) {
				page+=tcount/8;
				tcount = tcount%8;
			}

		}

		j=0;
		i=0;
		while (i<lenc) {

		try {
			if (j>0)
				file = new FileOutputStream("screens_clon" + j + ".htm");
			else
				file = new FileOutputStream("screens_clon.htm");
			p = new PrintStream( file );
			p.println ("<HTML>");
			p.println ("");
			p.println ("<HEAD>");
			p.println ("<SCRIPT LANGUAGE=\"JavaScript\">");
			p.println ("<!-- Begin");
			p.println ("function cottage(gameName)");
			p.println ("{");
			p.println ("window.open(gameName,null,\"'scrollbar=no,menubar=no,toolbar=no,personalbars=no,scrollbar=no,status=no,fullscreen=yes\");");
			p.println ("}");
			p.println ("//  End -->");
			p.println ("</SCRIPT>");
			p.println ("");
			p.println ("<META HTTP-EQUIV=\"Content-Type\" CONTENT=\"text/html; charset=iso-8859-1\">");
			p.println ("<TITLE>CottAGE</TITLE>");
			p.println ("</HEAD>");
			p.println ("<BODY BACKGROUND=\"scanline.gif\" TEXT=\"#FFFFFF\" LINK=\"#FFAA00\" VLINK=\"#BB7700\" MARGINWIDTH=0 MARGINHEIGHT=0>");
			p.println ("");
			p.println ("<FONT FACE=\"Verdana\" COLOR=GREEN SIZE=+2><B>CHOOSE A GAME</B></FONT>");
			p.println ("<BR>");
			p.println ("<FONT FACE=\"Verdana\" COLOR=GREEN SIZE=+1>");
			p.println ("<A HREF=\"screens.htm\">All</A> ");
			p.println ("<A HREF=\"screens_bug.htm\">Buggy</A> ");
			p.println ("Clones ");
			p.println ("<A HREF=\"screens_prod.htm\">Manufacturer</A> ");
			p.println ("<A HREF=\"screens_org.htm\">Originals</A> ");
			p.println ("<A HREF=\"screens_work.htm\">Working</A> ");
			p.println ("<A HREF=\"screens_year.htm\">Year</A> ");
			p.println ("</FONT>");
			p.println ("<BR>");
			p.println (common);
			p.println ("<BR><BR>");
			p.println ("");
			p.println ("<TABLE WIDTH=100% BORDER=1 CELLSPACING=0 CELLPADDING=0>");

			p.println ("</TABLE>");
			p.println ("<BR><BR>");
			p.println ("<FONT FACE=\"Verdana\" COLOR=GREEN SIZE=+1>");
			letter = selnames[i].charAt(0);
			if ((letter < 'A') || (letter > 'Z'))
				p.println ("<A NAME=\"letterx\">");
			else
				p.println ("<A NAME=\"letter" + letter +"\">");
			p.println (letter);
			p.println ("</A>");
			p.println ("</FONT>");
			p.println ("<BR><BR>");
			p.println ("<TABLE WIDTH=100% BORDER=1 CELLSPACING=0 CELLPADDING=0>");

			int row = 0;
			int k = 0;
			while((i<lenc)&&(k<8)) {
				letter = selnames[i].charAt(0);
				if ((k>0) && (letter != selnames[i-1].charAt(0))) {
					if (row == 0)
						p.println ("</TR>");
					p.println ("</TABLE>");
					p.println ("<BR><BR>");
					p.println ("<FONT FACE=\"Verdana\" COLOR=GREEN SIZE=+1>");
					if ((letter < 'A') || (letter > 'Z'))
						p.println ("<A NAME=\"letterx\">");
					else
						p.println ("<A NAME=\"letter" + letter +"\">");
					p.println (letter);
					p.println ("</A>");
					p.println ("</FONT>");
					p.println ("<BR><BR>");
					p.println ("<TABLE WIDTH=100% BORDER=1 CELLSPACING=0 CELLPADDING=0>");
					row = 0;
				}

				if (row == 0)
					p.println ("<TR>");

				p.println ("<TD ALIGN=CENTER VALIGN=MIDDLE>");
				p.println ("<A HREF=\"javascript:cottage('cottage/" + orgnames[selindexes[i]] + ".htm')\">");
				p.println ("<IMG SRC=\"" + orgnames[selindexes[i]] + ".gif\">");
				p.println ("</A><BR>");
				p.println (selnames[i] + "<BR>");
				p.println ("© " + dates[selindexes[i]] + " " + prods[selindexes[i]]);

				if (sound[selindexes[i]]) {
					p.println("<BR>");
					p.println("<IMG SRC=\"sound.gif\">");
				}

				p.println ("</TD>");

				if (row == 1) {
					p.println ("</TR>");
					row = 0;
				} else
					row = 1;

				i++; k++;
			}

			if (row == 1)
				p.println ("</TR>");

			p.println ("</TABLE>");

			p.println ("<P ALIGN=CENTER>");
			p.println ("<FONT FACE=\"Verdana\" COLOR=GREEN SIZE=+1>");

			if (j>0) {
				if (j == 1)
					p.println ("<A HREF=\"screens_clon.htm\">PREV 8</A>");
				else
					p.println ("<A HREF=\"screens_clon" + (j-1) + ".htm\">PREV 8</A>");
			}

			next = lenc - i;
			if (next > 0) {
				next = (next < 8) ? next : 8;
				p.println ("<A HREF=\"screens_clon" + (j+1) + ".htm\">NEXT " + next + "</A>");
			}

			p.println ("</FONT>");
			p.println ("</P>");

			p.println ("");
			p.println ("<P ALIGN=CENTER><IMG SRC=\"http://cottage.emuunlim.com/cgi-bin/count.pl\"></P>");
			p.println ("");
			p.println ("</BODY>");
			p.println ("</HTML>");
			p.close();

		} catch (Exception e) { System.err.println ("Error writing to file"); }

		j++;

		}


		/* Build screens_bug.htm */

		lenc = 0;
		for(i=0; i<len; i++) {
			if (comments[i] != "") {
				selnames[lenc] = names[i];
				selindexes[lenc++] = i;
			}
		}

		/* Bubble-sort */
		for(i=0; i<lenc; i++) {
			for(j=i+1; j<lenc; j++) {
				if (selnames[j].compareTo(selnames[i])<0) {
					/* Swap entries */
					saux = selnames[j]; selnames[j] = selnames[i]; selnames[i] = saux;
					iaux = selindexes[j]; selindexes[j] = selindexes[i]; selindexes[i] = iaux;
				}
			}
		}

		for(i=0; i<lenc; i++) {
			letter = selnames[i].charAt(0);
			if ((letter < 'A') || (letter > 'Z'))
				count[0]++;
			else
				count[letter+1 - 'A']++;
		}

		page = 0;
		tcount = 0;
		common = "";
		for(i=0; i<27; i++) {
			if (i == 0)
				letter = '#' ;
			else
				letter = (char)('A'+i-1);

			if (count[i] > 0) {
				common += "<FONT FACE=\"Verdana\" COLOR=GREEN SIZE=+1>";
				if (page == 0)
					common += "<A HREF=\"screens_bug.htm";
				else
					common += "<A HREF=\"screens_bug" + page + ".htm";
				if (i == 0)
					common += "#letterx\">";
				else
					common += "#letter" + letter + "\">";
			} else
				common += "<FONT FACE=\"Verdana\" COLOR=GRAY SIZE=+1>";

			common += letter;

			if (count[i] > 0)
				common += "</A></FONT> ";
			else
				common += " </FONT>";

			tcount += count[i];
			if (tcount >= 8) {
				page+=tcount/8;
				tcount = tcount%8;
			}

		}

		j=0;
		i=0;
		while (i<lenc) {

		try {
			if (j>0)
				file = new FileOutputStream("screens_bug" + j + ".htm");
			else
				file = new FileOutputStream("screens_bug.htm");
			p = new PrintStream( file );
			p.println ("<HTML>");
			p.println ("");
			p.println ("<HEAD>");
			p.println ("<SCRIPT LANGUAGE=\"JavaScript\">");
			p.println ("<!-- Begin");
			p.println ("function cottage(gameName)");
			p.println ("{");
			p.println ("window.open(gameName,null,\"'scrollbar=no,menubar=no,toolbar=no,personalbars=no,scrollbar=no,status=no,fullscreen=yes\");");
			p.println ("}");
			p.println ("//  End -->");
			p.println ("</SCRIPT>");
			p.println ("");
			p.println ("<META HTTP-EQUIV=\"Content-Type\" CONTENT=\"text/html; charset=iso-8859-1\">");
			p.println ("<TITLE>CottAGE</TITLE>");
			p.println ("</HEAD>");
			p.println ("<BODY BACKGROUND=\"scanline.gif\" TEXT=\"#FFFFFF\" LINK=\"#FFAA00\" VLINK=\"#BB7700\" MARGINWIDTH=0 MARGINHEIGHT=0>");
			p.println ("");
			p.println ("<FONT FACE=\"Verdana\" COLOR=GREEN SIZE=+2><B>CHOOSE A GAME</B></FONT>");
			p.println ("<BR>");
			p.println ("<FONT FACE=\"Verdana\" COLOR=GREEN SIZE=+1>");
			p.println ("<A HREF=\"screens.htm\">All</A> ");
			p.println ("Buggy ");
			p.println ("<A HREF=\"screens_clon.htm\">Clones</A> ");
			p.println ("<A HREF=\"screens_prod.htm\">Manufacturer</A> ");
			p.println ("<A HREF=\"screens_org.htm\">Originals</A> ");
			p.println ("<A HREF=\"screens_work.htm\">Working</A> ");
			p.println ("<A HREF=\"screens_year.htm\">Year</A> ");
			p.println ("</FONT>");
			p.println ("<BR>");
			p.println (common);
			p.println ("<BR><BR>");
			p.println ("");
			p.println ("<TABLE WIDTH=100% BORDER=1 CELLSPACING=0 CELLPADDING=0>");

			p.println ("</TABLE>");
			p.println ("<BR><BR>");
			p.println ("<FONT FACE=\"Verdana\" COLOR=GREEN SIZE=+1>");
			letter = selnames[i].charAt(0);
			if ((letter < 'A') || (letter > 'Z'))
				p.println ("<A NAME=\"letterx\">");
			else
				p.println ("<A NAME=\"letter" + letter +"\">");
			p.println (letter);
			p.println ("</A>");
			p.println ("</FONT>");
			p.println ("<BR><BR>");
			p.println ("<TABLE WIDTH=100% BORDER=1 CELLSPACING=0 CELLPADDING=0>");

			int row = 0;
			int k = 0;
			while((i<lenc)&&(k<8)) {
				letter = selnames[i].charAt(0);
				if ((k>0) && (letter != selnames[i-1].charAt(0))) {
					if (row == 0)
						p.println ("</TR>");
					p.println ("</TABLE>");
					p.println ("<BR><BR>");
					p.println ("<FONT FACE=\"Verdana\" COLOR=GREEN SIZE=+1>");
					if ((letter < 'A') || (letter > 'Z'))
						p.println ("<A NAME=\"letterx\">");
					else
						p.println ("<A NAME=\"letter" + letter +"\">");
					p.println (letter);
					p.println ("</A>");
					p.println ("</FONT>");
					p.println ("<BR><BR>");
					p.println ("<TABLE WIDTH=100% BORDER=1 CELLSPACING=0 CELLPADDING=0>");
					row = 0;
				}

				if (row == 0)
					p.println ("<TR>");

				p.println ("<TD ALIGN=CENTER VALIGN=MIDDLE>");
				p.println ("<A HREF=\"javascript:cottage('cottage/" + orgnames[selindexes[i]] + ".htm')\">");
				p.println ("<IMG SRC=\"" + orgnames[selindexes[i]] + ".gif\">");
				p.println ("</A><BR>");
				p.println (selnames[i] + "<BR>");
				p.println ("© " + dates[selindexes[i]] + " " + prods[selindexes[i]]);

				if (clones[selindexes[i]] != null) {
					p.println ("<BR>");
					p.println ("<B>GAME CLONE</B>");
				}

				if (sound[selindexes[i]]) {
					p.println("<BR>");
					p.println("<IMG SRC=\"sound.gif\">");
				}

				if (comments[selindexes[i]] != "") {
					p.println ("<BR>");
					p.println ("<B>" + comments[selindexes[i]] + "</B>");
				}

				p.println ("</TD>");

				if (row == 1) {
					p.println ("</TR>");
					row = 0;
				} else
					row = 1;

				i++; k++;
			}

			if (row == 1)
				p.println ("</TR>");

			p.println ("</TABLE>");

			p.println ("<P ALIGN=CENTER>");
			p.println ("<FONT FACE=\"Verdana\" COLOR=GREEN SIZE=+1>");

			if (j>0) {
				if (j == 1)
					p.println ("<A HREF=\"screens_bug.htm\">PREV 8</A>");
				else
					p.println ("<A HREF=\"screens_bug" + (j-1) + ".htm\">PREV 8</A>");
			}

			next = lenc - i;
			if (next > 0) {
				next = (next < 8) ? next : 8;
				p.println ("<A HREF=\"screens_bug" + (j+1) + ".htm\">NEXT " + next + "</A>");
			}

			p.println ("</FONT>");
			p.println ("</P>");

			p.println ("");
			p.println ("<P ALIGN=CENTER><IMG SRC=\"http://cottage.emuunlim.com/cgi-bin/count.pl\"></P>");
			p.println ("");
			p.println ("</BODY>");
			p.println ("</HTML>");
			p.close();

		} catch (Exception e) { System.err.println ("Error writing to file"); }

		j++;

		}


		/* Build screens_prod.htm */

		for(i=0; i<27; i++)
			count[i]=0;

		/* Bubble-sort */
		for(i=0; i<len; i++) {
			for(j=i+1; j<len; j++) {
				cmp = prods[j].compareTo(prods[i]);
				if ((cmp<0) || ((cmp==0) && (names[j].compareTo(names[i])<0))) {
					/* Swap entries */
					saux = names[j]; names[j] = names[i]; names[i] = saux;
					saux = dates[j]; dates[j] = dates[i]; dates[i] = saux;
					saux = prods[j]; prods[j] = prods[i]; prods[i] = saux;
					saux = orgnames[j]; orgnames[j] = orgnames[i]; orgnames[i] = saux;
					saux = comments[j]; comments[j] = comments[i]; comments[i] = saux;
					saux = clones[j]; clones[j] = clones[i]; clones[i] = saux;
					baux = sound[j]; sound[j] = sound[i]; sound[i] = baux;
				}
			}
		}

		for(i=0; i<len; i++) {
			letter = prods[i].charAt(0);
			if ((letter < 'A') || (letter > 'Z'))
				count[0]++;
			else
				count[letter+1 - 'A']++;
		}

		page = 0;
		tcount = 0;
		common = "";
		for(i=0; i<27; i++) {
			if (i == 0)
				letter = '#' ;
			else
				letter = (char)('A'+i-1);

			if (count[i] > 0) {
				common += "<FONT FACE=\"Verdana\" COLOR=GREEN SIZE=+1>";
				if (page == 0)
					common += "<A HREF=\"screens_prod.htm";
				else
					common += "<A HREF=\"screens_prod" + page + ".htm";
				if (i == 0)
					common += "#letterx\">";
				else
					common += "#letter" + letter + "\">";
			} else
				common += "<FONT FACE=\"Verdana\" COLOR=GRAY SIZE=+1>";

			common += letter;

			if (count[i] > 0)
				common += "</A></FONT> ";
			else
				common += " </FONT>";

			tcount += count[i];
			if (tcount >= 8) {
				page+=tcount/8;
				tcount = tcount%8;
			}

		}

		j=0;
		i=0;
		while (i<len) {

		try {
			if (j>0)
				file = new FileOutputStream("screens_prod" + j + ".htm");
			else
				file = new FileOutputStream("screens_prod.htm");
			p = new PrintStream( file );
			p.println ("<HTML>");
			p.println ("");
			p.println ("<HEAD>");
			p.println ("<SCRIPT LANGUAGE=\"JavaScript\">");
			p.println ("<!-- Begin");
			p.println ("function cottage(gameName)");
			p.println ("{");
			p.println ("window.open(gameName,null,\"'scrollbar=no,menubar=no,toolbar=no,personalbars=no,scrollbar=no,status=no,fullscreen=yes\");");
			p.println ("}");
			p.println ("//  End -->");
			p.println ("</SCRIPT>");
			p.println ("");
			p.println ("<META HTTP-EQUIV=\"Content-Type\" CONTENT=\"text/html; charset=iso-8859-1\">");
			p.println ("<TITLE>CottAGE</TITLE>");
			p.println ("</HEAD>");
			p.println ("<BODY BACKGROUND=\"scanline.gif\" TEXT=\"#FFFFFF\" LINK=\"#FFAA00\" VLINK=\"#BB7700\" MARGINWIDTH=0 MARGINHEIGHT=0>");
			p.println ("");
			p.println ("<FONT FACE=\"Verdana\" COLOR=GREEN SIZE=+2><B>CHOOSE A GAME</B></FONT>");
			p.println ("<BR>");
			p.println ("<FONT FACE=\"Verdana\" COLOR=GREEN SIZE=+1>");
			p.println ("<A HREF=\"screens.htm\">All</A> ");
			p.println ("<A HREF=\"screens_bug.htm\">Buggy</A> ");
			p.println ("<A HREF=\"screens_clon.htm\">Clones</A> ");
			p.println ("Manufacturer ");
			p.println ("<A HREF=\"screens_org.htm\">Originals</A> ");
			p.println ("<A HREF=\"screens_work.htm\">Working</A> ");
			p.println ("<A HREF=\"screens_year.htm\">Year</A> ");
			p.println ("</FONT>");
			p.println ("<BR>");
			p.println (common);
			p.println ("<BR><BR>");
			p.println ("");
			p.println ("<TABLE WIDTH=100% BORDER=1 CELLSPACING=0 CELLPADDING=0>");

			p.println ("</TABLE>");
			p.println ("<BR><BR>");
			p.println ("<FONT FACE=\"Verdana\" COLOR=GREEN SIZE=+1>");
			letter = prods[i].charAt(0);
			if ((letter < 'A') || (letter > 'Z'))
				p.println ("<A NAME=\"letterx\">");
			else
				p.println ("<A NAME=\"letter" + letter +"\">");
			p.println (letter);
			p.println ("</A>");
			if (letter != ' ')
				p.println ("<BR><BR>");
			p.println (prods[i]);
			p.println ("</FONT>");
			p.println ("<BR><BR>");
			p.println ("<TABLE WIDTH=100% BORDER=1 CELLSPACING=0 CELLPADDING=0>");

			int row = 0;
			int k = 0;
			while((i<len)&&(k<8)) {
				letter = prods[i].charAt(0);
				if (k>0) {
					if (letter != prods[i-1].charAt(0)) {
						if (row == 0)
							p.println ("</TR>");
						p.println ("</TABLE>");
						p.println ("<BR><BR>");
						p.println ("<FONT FACE=\"Verdana\" COLOR=GREEN SIZE=+1>");
						if ((letter < 'A') || (letter > 'Z'))
							p.println ("<A NAME=\"letterx\">");
						else
							p.println ("<A NAME=\"letter" + letter +"\">");
						p.println (letter);
						p.println ("</A>");
						if (letter != ' ')
							p.println ("<BR><BR>");
						p.println (prods[i]);
						p.println ("</FONT>");
						p.println ("<BR><BR>");
						p.println ("<TABLE WIDTH=100% BORDER=1 CELLSPACING=0 CELLPADDING=0>");
						row = 0;
					} else if (prods[i].compareTo(prods[i-1]) != 0) {
						if (row == 0)
							p.println ("</TR>");
						p.println ("</TABLE>");
						p.println ("<BR>");
						p.println ("<FONT FACE=\"Verdana\" COLOR=GREEN SIZE=+1>");
						p.println (prods[i]);
						p.println ("</FONT>");
						p.println ("<BR><BR>");
						p.println ("<TABLE WIDTH=100% BORDER=1 CELLSPACING=0 CELLPADDING=0>");
						row = 0;
					}
				}

				if (row == 0)
					p.println ("<TR>");

				p.println ("<TD ALIGN=CENTER VALIGN=MIDDLE>");
				p.println ("<A HREF=\"javascript:cottage('cottage/" + orgnames[i] + ".htm')\">");
				p.println ("<IMG SRC=\"" + orgnames[i] + ".gif\">");
				p.println ("</A><BR>");
				p.println (names[i] + "<BR>");
				p.println ("© " + dates[i]);

				if (clones[i] != null) {
					p.println ("<BR>");
					p.println ("<B>GAME CLONE</B>");
				}

				if (sound[i]) {
					p.println("<BR>");
					p.println("<IMG SRC=\"sound.gif\">");
				}

				p.println ("</TD>");

				if (row == 1) {
					p.println ("</TR>");
					row = 0;
				} else
					row = 1;

				i++; k++;
			}

			if (row == 1)
				p.println ("</TR>");

			p.println ("</TABLE>");

			p.println ("<P ALIGN=CENTER>");
			p.println ("<FONT FACE=\"Verdana\" COLOR=GREEN SIZE=+1>");

			if (j>0) {
				if (j == 1)
					p.println ("<A HREF=\"screens_prod.htm\">PREV 8</A>");
				else
					p.println ("<A HREF=\"screens_prod" + (j-1) + ".htm\">PREV 8</A>");
			}

			next = len - i;
			if (next > 0) {
				next = (next < 8) ? next : 8;
				p.println ("<A HREF=\"screens_prod" + (j+1) + ".htm\">NEXT " + next + "</A>");
			}

			p.println ("</FONT>");
			p.println ("</P>");

			p.println ("");
			p.println ("<P ALIGN=CENTER><IMG SRC=\"http://cottage.emuunlim.com/cgi-bin/count.pl\"></P>");
			p.println ("");
			p.println ("</BODY>");
			p.println ("</HTML>");
			p.close();

		} catch (Exception e) { System.err.println ("Error writing to file"); }

		j++;

		}


		/* Build screens_org.htm */

		lenc = 0;
		for(i=0; i<len; i++) {
			if (clones[i] == null) {
				selnames[lenc] = names[i];
				selindexes[lenc++] = i;
			}
		}

		/* Bubble-sort */
		for(i=0; i<lenc; i++) {
			for(j=i+1; j<lenc; j++) {
				if (selnames[j].compareTo(selnames[i])<0) {
					/* Swap entries */
					saux = selnames[j]; selnames[j] = selnames[i]; selnames[i] = saux;
					iaux = selindexes[j]; selindexes[j] = selindexes[i]; selindexes[i] = iaux;
				}
			}
		}

		for(i=0; i<lenc; i++) {
			letter = selnames[i].charAt(0);
			if ((letter < 'A') || (letter > 'Z'))
				count[0]++;
			else
				count[letter+1 - 'A']++;
		}

		page = 0;
		tcount = 0;
		common = "";
		for(i=0; i<27; i++) {
			if (i == 0)
				letter = '#' ;
			else
				letter = (char)('A'+i-1);

			if (count[i] > 0) {
				common += "<FONT FACE=\"Verdana\" COLOR=GREEN SIZE=+1>";
				if (page == 0)
					common += "<A HREF=\"screens_org.htm";
				else
					common += "<A HREF=\"screens_org" + page + ".htm";
				if (i == 0)
					common += "#letterx\">";
				else
					common += "#letter" + letter + "\">";
			} else
				common += "<FONT FACE=\"Verdana\" COLOR=GRAY SIZE=+1>";

			common += letter;

			if (count[i] > 0)
				common += "</A></FONT> ";
			else
				common += " </FONT>";

			tcount += count[i];
			if (tcount >= 8) {
				page+=tcount/8;
				tcount = tcount%8;
			}

		}

		j=0;
		i=0;
		while (i<lenc) {

		try {
			if (j>0)
				file = new FileOutputStream("screens_org" + j + ".htm");
			else
				file = new FileOutputStream("screens_org.htm");
			p = new PrintStream( file );
			p.println ("<HTML>");
			p.println ("");
			p.println ("<HEAD>");
			p.println ("<SCRIPT LANGUAGE=\"JavaScript\">");
			p.println ("<!-- Begin");
			p.println ("function cottage(gameName)");
			p.println ("{");
			p.println ("window.open(gameName,null,\"'scrollbar=no,menubar=no,toolbar=no,personalbars=no,scrollbar=no,status=no,fullscreen=yes\");");
			p.println ("}");
			p.println ("//  End -->");
			p.println ("</SCRIPT>");
			p.println ("");
			p.println ("<META HTTP-EQUIV=\"Content-Type\" CONTENT=\"text/html; charset=iso-8859-1\">");
			p.println ("<TITLE>CottAGE</TITLE>");
			p.println ("</HEAD>");
			p.println ("<BODY BACKGROUND=\"scanline.gif\" TEXT=\"#FFFFFF\" LINK=\"#FFAA00\" VLINK=\"#BB7700\" MARGINWIDTH=0 MARGINHEIGHT=0>");
			p.println ("");
			p.println ("<FONT FACE=\"Verdana\" COLOR=GREEN SIZE=+2><B>CHOOSE A GAME</B></FONT>");
			p.println ("<BR>");
			p.println ("<FONT FACE=\"Verdana\" COLOR=GREEN SIZE=+1>");
			p.println ("<A HREF=\"screens.htm\">All</A> ");
			p.println ("<A HREF=\"screens_bug.htm\">Buggy</A> ");
			p.println ("<A HREF=\"screens_clon.htm\">Clones</A> ");
			p.println ("<A HREF=\"screens_prod.htm\">Manufacturer</A> ");
			p.println ("Originals ");
			p.println ("<A HREF=\"screens_work.htm\">Working</A> ");
			p.println ("<A HREF=\"screens_year.htm\">Year</A> ");
			p.println ("</FONT>");
			p.println ("<BR>");
			p.println (common);
			p.println ("<BR><BR>");
			p.println ("");
			p.println ("<TABLE WIDTH=100% BORDER=1 CELLSPACING=0 CELLPADDING=0>");

			p.println ("</TABLE>");
			p.println ("<BR><BR>");
			p.println ("<FONT FACE=\"Verdana\" COLOR=GREEN SIZE=+1>");
			letter = selnames[i].charAt(0);
			if ((letter < 'A') || (letter > 'Z'))
				p.println ("<A NAME=\"letterx\">");
			else
				p.println ("<A NAME=\"letter" + letter +"\">");
			p.println (letter);
			p.println ("</A>");
			p.println ("</FONT>");
			p.println ("<BR><BR>");
			p.println ("<TABLE WIDTH=100% BORDER=1 CELLSPACING=0 CELLPADDING=0>");

			int row = 0;
			int k = 0;
			while((i<lenc)&&(k<8)) {
				letter = selnames[i].charAt(0);
				if ((k>0) && (letter != selnames[i-1].charAt(0))) {
					if (row == 0)
						p.println ("</TR>");
					p.println ("</TABLE>");
					p.println ("<BR><BR>");
					p.println ("<FONT FACE=\"Verdana\" COLOR=GREEN SIZE=+1>");
					if ((letter < 'A') || (letter > 'Z'))
						p.println ("<A NAME=\"letterx\">");
					else
						p.println ("<A NAME=\"letter" + letter +"\">");
					p.println (letter);
					p.println ("</A>");
					p.println ("</FONT>");
					p.println ("<BR><BR>");
					p.println ("<TABLE WIDTH=100% BORDER=1 CELLSPACING=0 CELLPADDING=0>");
					row = 0;
				}

				if (row == 0)
					p.println ("<TR>");

				p.println ("<TD ALIGN=CENTER VALIGN=MIDDLE>");
				p.println ("<A HREF=\"javascript:cottage('cottage/" + orgnames[selindexes[i]] + ".htm')\">");
				p.println ("<IMG SRC=\"" + orgnames[selindexes[i]] + ".gif\">");
				p.println ("</A><BR>");
				p.println (selnames[i] + "<BR>");
				p.println ("© " + dates[selindexes[i]] + " " + prods[selindexes[i]]);

				if (sound[selindexes[i]]) {
					p.println("<BR>");
					p.println("<IMG SRC=\"sound.gif\">");
				}

				p.println ("</TD>");

				if (row == 1) {
					p.println ("</TR>");
					row = 0;
				} else
					row = 1;

				i++; k++;
			}

			if (row == 1)
				p.println ("</TR>");

			p.println ("</TABLE>");

			p.println ("<P ALIGN=CENTER>");
			p.println ("<FONT FACE=\"Verdana\" COLOR=GREEN SIZE=+1>");

			if (j>0) {
				if (j == 1)
					p.println ("<A HREF=\"screens_org.htm\">PREV 8</A>");
				else
					p.println ("<A HREF=\"screens_org" + (j-1) + ".htm\">PREV 8</A>");
			}

			next = lenc - i;
			if (next > 0) {
				next = (next < 8) ? next : 8;
				p.println ("<A HREF=\"screens_org" + (j+1) + ".htm\">NEXT " + next + "</A>");
			}

			p.println ("</FONT>");
			p.println ("</P>");

			p.println ("");
			p.println ("<P ALIGN=CENTER><IMG SRC=\"http://cottage.emuunlim.com/cgi-bin/count.pl\"></P>");
			p.println ("");
			p.println ("</BODY>");
			p.println ("</HTML>");
			p.close();

		} catch (Exception e) { System.err.println ("Error writing to file"); }

		j++;

		}


		/* Build screens_work.htm */

		lenc = 0;
		for(i=0; i<len; i++) {
			if (comments[i] == "") {
				selnames[lenc] = names[i];
				selindexes[lenc++] = i;
			}
		}

		/* Bubble-sort */
		for(i=0; i<lenc; i++) {
			for(j=i+1; j<lenc; j++) {
				if (selnames[j].compareTo(selnames[i])<0) {
					/* Swap entries */
					saux = selnames[j]; selnames[j] = selnames[i]; selnames[i] = saux;
					iaux = selindexes[j]; selindexes[j] = selindexes[i]; selindexes[i] = iaux;
				}
			}
		}

		for(i=0; i<lenc; i++) {
			letter = selnames[i].charAt(0);
			if ((letter < 'A') || (letter > 'Z'))
				count[0]++;
			else
				count[letter+1 - 'A']++;
		}

		page = 0;
		tcount = 0;
		common = "";
		for(i=0; i<27; i++) {
			if (i == 0)
				letter = '#' ;
			else
				letter = (char)('A'+i-1);

			if (count[i] > 0) {
				common += "<FONT FACE=\"Verdana\" COLOR=GREEN SIZE=+1>";
				if (page == 0)
					common += "<A HREF=\"screens_work.htm";
				else
					common += "<A HREF=\"screens_work" + page + ".htm";
				if (i == 0)
					common += "#letterx\">";
				else
					common += "#letter" + letter + "\">";
			} else
				common += "<FONT FACE=\"Verdana\" COLOR=GRAY SIZE=+1>";

			common += letter;

			if (count[i] > 0)
				common += "</A></FONT> ";
			else
				common += " </FONT>";

			tcount += count[i];
			if (tcount >= 8) {
				page+=tcount/8;
				tcount = tcount%8;
			}

		}

		j=0;
		i=0;
		while (i<lenc) {

		try {
			if (j>0)
				file = new FileOutputStream("screens_work" + j + ".htm");
			else
				file = new FileOutputStream("screens_work.htm");
			p = new PrintStream( file );
			p.println ("<HTML>");
			p.println ("");
			p.println ("<HEAD>");
			p.println ("<SCRIPT LANGUAGE=\"JavaScript\">");
			p.println ("<!-- Begin");
			p.println ("function cottage(gameName)");
			p.println ("{");
			p.println ("window.open(gameName,null,\"'scrollbar=no,menubar=no,toolbar=no,personalbars=no,scrollbar=no,status=no,fullscreen=yes\");");
			p.println ("}");
			p.println ("//  End -->");
			p.println ("</SCRIPT>");
			p.println ("");
			p.println ("<META HTTP-EQUIV=\"Content-Type\" CONTENT=\"text/html; charset=iso-8859-1\">");
			p.println ("<TITLE>CottAGE</TITLE>");
			p.println ("</HEAD>");
			p.println ("<BODY BACKGROUND=\"scanline.gif\" TEXT=\"#FFFFFF\" LINK=\"#FFAA00\" VLINK=\"#BB7700\" MARGINWIDTH=0 MARGINHEIGHT=0>");
			p.println ("");
			p.println ("<FONT FACE=\"Verdana\" COLOR=GREEN SIZE=+2><B>CHOOSE A GAME</B></FONT>");
			p.println ("<BR>");
			p.println ("<FONT FACE=\"Verdana\" COLOR=GREEN SIZE=+1>");
			p.println ("<A HREF=\"screens.htm\">All</A> ");
			p.println ("<A HREF=\"screens_bug.htm\">Buggy</A> ");
			p.println ("<A HREF=\"screens_clon.htm\">Clones</A> ");
			p.println ("<A HREF=\"screens_prod.htm\">Manufacturer</A> ");
			p.println ("<A HREF=\"screens_org.htm\">Originals</A> ");
			p.println ("Working ");
			p.println ("<A HREF=\"screens_year.htm\">Year</A> ");
			p.println ("</FONT>");
			p.println ("<BR>");
			p.println (common);
			p.println ("<BR><BR>");
			p.println ("");
			p.println ("<TABLE WIDTH=100% BORDER=1 CELLSPACING=0 CELLPADDING=0>");

			p.println ("</TABLE>");
			p.println ("<BR><BR>");
			p.println ("<FONT FACE=\"Verdana\" COLOR=GREEN SIZE=+1>");
			letter = selnames[i].charAt(0);
			if ((letter < 'A') || (letter > 'Z'))
				p.println ("<A NAME=\"letterx\">");
			else
				p.println ("<A NAME=\"letter" + letter +"\">");
			p.println (letter);
			p.println ("</A>");
			p.println ("</FONT>");
			p.println ("<BR><BR>");
			p.println ("<TABLE WIDTH=100% BORDER=1 CELLSPACING=0 CELLPADDING=0>");

			int row = 0;
			int k = 0;
			while((i<lenc)&&(k<8)) {
				letter = selnames[i].charAt(0);
				if ((k>0) && (letter != selnames[i-1].charAt(0))) {
					if (row == 0)
						p.println ("</TR>");
					p.println ("</TABLE>");
					p.println ("<BR><BR>");
					p.println ("<FONT FACE=\"Verdana\" COLOR=GREEN SIZE=+1>");
					if ((letter < 'A') || (letter > 'Z'))
						p.println ("<A NAME=\"letterx\">");
					else
						p.println ("<A NAME=\"letter" + letter +"\">");
					p.println (letter);
					p.println ("</A>");
					p.println ("</FONT>");
					p.println ("<BR><BR>");
					p.println ("<TABLE WIDTH=100% BORDER=1 CELLSPACING=0 CELLPADDING=0>");
					row = 0;
				}

				if (row == 0)
					p.println ("<TR>");

				p.println ("<TD ALIGN=CENTER VALIGN=MIDDLE>");
				p.println ("<A HREF=\"javascript:cottage('cottage/" + orgnames[selindexes[i]] + ".htm')\">");
				p.println ("<IMG SRC=\"" + orgnames[selindexes[i]] + ".gif\">");
				p.println ("</A><BR>");
				p.println (selnames[i] + "<BR>");
				p.println ("© " + dates[selindexes[i]] + " " + prods[selindexes[i]]);

				if (clones[selindexes[i]] != null) {
					p.println ("<BR>");
					p.println ("<B>GAME CLONE</B>");
				}

				if (sound[selindexes[i]]) {
					p.println("<BR>");
					p.println("<IMG SRC=\"sound.gif\">");
				}

				p.println ("</TD>");

				if (row == 1) {
					p.println ("</TR>");
					row = 0;
				} else
					row = 1;

				i++; k++;
			}

			if (row == 1)
				p.println ("</TR>");

			p.println ("</TABLE>");

			p.println ("<P ALIGN=CENTER>");
			p.println ("<FONT FACE=\"Verdana\" COLOR=GREEN SIZE=+1>");

			if (j>0) {
				if (j == 1)
					p.println ("<A HREF=\"screens_work.htm\">PREV 8</A>");
				else
					p.println ("<A HREF=\"screens_work" + (j-1) + ".htm\">PREV 8</A>");
			}

			next = lenc - i;
			if (next > 0) {
				next = (next < 8) ? next : 8;
				p.println ("<A HREF=\"screens_work" + (j+1) + ".htm\">NEXT " + next + "</A>");
			}

			p.println ("</FONT>");
			p.println ("</P>");

			p.println ("");
			p.println ("<P ALIGN=CENTER><IMG SRC=\"http://cottage.emuunlim.com/cgi-bin/count.pl\"></P>");
			p.println ("");
			p.println ("</BODY>");
			p.println ("</HTML>");
			p.close();

		} catch (Exception e) { System.err.println ("Error writing to file"); }

		j++;

		}


		/* Build screens_year.htm */

		for(i=0; i<33; i++)
			count[i]=0;

		/* Bubble-sort */
		for(i=0; i<len; i++) {
			for(j=i+1; j<len; j++) {
				if (names[j].compareTo(names[i])<0) {
					/* Swap entries */
					saux = names[j]; names[j] = names[i]; names[i] = saux;
					saux = dates[j]; dates[j] = dates[i]; dates[i] = saux;
					saux = prods[j]; prods[j] = prods[i]; prods[i] = saux;
					saux = orgnames[j]; orgnames[j] = orgnames[i]; orgnames[i] = saux;
					saux = comments[j]; comments[j] = comments[i]; comments[i] = saux;
					saux = clones[j]; clones[j] = clones[i]; clones[i] = saux;
					baux = sound[j]; sound[j] = sound[i]; sound[i] = baux;
				}
			}
		}

		for (i=0; i<len; i++)
			if (dates[i].compareTo("19??") == 0)
				dates[i] = "1900";

		/* Bubble-sort */
		for(i=0; i<len; i++) {
			for(j=i+1; j<len; j++) {
				cmp = dates[j].compareTo(dates[i]);
				if ((cmp<0) || ((cmp==0) && (names[j].compareTo(names[i])<0))) {
					/* Swap entries */
					saux = names[j]; names[j] = names[i]; names[i] = saux;
					saux = dates[j]; dates[j] = dates[i]; dates[i] = saux;
					saux = prods[j]; prods[j] = prods[i]; prods[i] = saux;
					saux = orgnames[j]; orgnames[j] = orgnames[i]; orgnames[i] = saux;
					saux = comments[j]; comments[j] = comments[i]; comments[i] = saux;
					saux = clones[j]; clones[j] = clones[i]; clones[i] = saux;
					baux = sound[j]; sound[j] = sound[i]; sound[i] = baux;
				}
			}
		}

		int datcalc;
		for(i=0; i<len; i++) {
			if (dates[i].compareTo("1900") == 0) {
				dates[i] = "19??";
				newdate[i] = 0;
				count[0]++;
			} else {
				datcalc = (dates[i].charAt(0)-'0')*1000;
				datcalc += (dates[i].charAt(1)-'0')*100;
				datcalc += (dates[i].charAt(2)-'0')*10;
				datcalc += (dates[i].charAt(3)-'0');
				newdate[i] = datcalc;
				count[datcalc+1 - 1970]++;
			}
		}

		page = 0;
		tcount = 0;
		common = "";
		String letterx = "";
		String lettery;
		for(i=0; i<33; i++) {
			if (i == 0)
				lettery = "Unknown";
			else {
				letterx = ""+(1970+i-1);
				int year = 70+i-1;
				if (year<100)
					lettery = "" + year;
				else
					lettery = "0" + (year-100);
			}

			if (count[i] > 0) {
				common += "<FONT FACE=\"Verdana\" COLOR=GREEN SIZE=+1>";
				if (page == 0)
					common += "<A HREF=\"screens_year.htm";
				else
					common += "<A HREF=\"screens_year" + page + ".htm";
				if (i == 0)
					common += "#letterx\">";
				else
					common += "#letter" + letterx + "\">";
			} else
				common += "<FONT FACE=\"Verdana\" COLOR=GRAY SIZE=+1>";

			common += lettery;

			if (count[i] > 0)
				common += "</A></FONT> ";
			else
				common += " </FONT>";

			tcount += count[i];
			if (tcount >= 8) {
				page+=tcount/8;
				tcount = tcount%8;
			}

		}

		j=0;
		i=0;
		while (i<len) {

		try {
			if (j>0)
				file = new FileOutputStream("screens_year" + j + ".htm");
			else
				file = new FileOutputStream("screens_year.htm");
			p = new PrintStream( file );
			p.println ("<HTML>");
			p.println ("");
			p.println ("<HEAD>");
			p.println ("<SCRIPT LANGUAGE=\"JavaScript\">");
			p.println ("<!-- Begin");
			p.println ("function cottage(gameName)");
			p.println ("{");
			p.println ("window.open(gameName,null,\"'scrollbar=no,menubar=no,toolbar=no,personalbars=no,scrollbar=no,status=no,fullscreen=yes\");");
			p.println ("}");
			p.println ("//  End -->");
			p.println ("</SCRIPT>");
			p.println ("");
			p.println ("<META HTTP-EQUIV=\"Content-Type\" CONTENT=\"text/html; charset=iso-8859-1\">");
			p.println ("<TITLE>CottAGE</TITLE>");
			p.println ("</HEAD>");
			p.println ("<BODY BACKGROUND=\"scanline.gif\" TEXT=\"#FFFFFF\" LINK=\"#FFAA00\" VLINK=\"#BB7700\" MARGINWIDTH=0 MARGINHEIGHT=0>");
			p.println ("");
			p.println ("<FONT FACE=\"Verdana\" COLOR=GREEN SIZE=+2><B>CHOOSE A GAME</B></FONT>");
			p.println ("<BR>");
			p.println ("<FONT FACE=\"Verdana\" COLOR=GREEN SIZE=+1>");
			p.println ("<A HREF=\"screens.htm\">All</A> ");
			p.println ("<A HREF=\"screens_bug.htm\">Buggy</A> ");
			p.println ("<A HREF=\"screens_clon.htm\">Clones</A> ");
			p.println ("<A HREF=\"screens_prod.htm\">Manufacturer</A> ");
			p.println ("<A HREF=\"screens_org.htm\">Originals</A> ");
			p.println ("<A HREF=\"screens_work.htm\">Working</A> ");
			p.println ("Year ");
			p.println ("</FONT>");
			p.println ("<BR>");
			p.println (common);
			p.println ("<BR><BR>");
			p.println ("");
			p.println ("<TABLE WIDTH=100% BORDER=1 CELLSPACING=0 CELLPADDING=0>");

			p.println ("</TABLE>");
			p.println ("<BR><BR>");
			p.println ("<FONT FACE=\"Verdana\" COLOR=GREEN SIZE=+1>");

			if (newdate[i] == 0) {
				p.println ("<A NAME=\"letterx\">");
				p.println ("Unknown");
			} else {
				p.println ("<A NAME=\"letter" + newdate[i] +"\">");
				p.println (newdate[i]);
			}
			p.println ("</A>");
			p.println ("</FONT>");
			p.println ("<BR><BR>");
			p.println ("<TABLE WIDTH=100% BORDER=1 CELLSPACING=0 CELLPADDING=0>");

			int row = 0;
			int k = 0;
			while((i<len)&&(k<8)) {
				if ((k>0) && (newdate[i] != newdate[i-1])) {
					if (row == 0)
						p.println ("</TR>");
					p.println ("</TABLE>");
					p.println ("<BR><BR>");
					p.println ("<FONT FACE=\"Verdana\" COLOR=GREEN SIZE=+1>");
					if (newdate[i] == 0) {
						p.println ("<A NAME=\"letterx\">");
						p.println ("Unknown");
					} else {
						p.println ("<A NAME=\"letter" + newdate[i] +"\">");
						p.println (newdate[i]);
					}
					p.println ("</A>");
					p.println ("</FONT>");
					p.println ("<BR><BR>");
					p.println ("<TABLE WIDTH=100% BORDER=1 CELLSPACING=0 CELLPADDING=0>");
					row = 0;
				}

				if (row == 0)
					p.println ("<TR>");

				p.println ("<TD ALIGN=CENTER VALIGN=MIDDLE>");
				p.println ("<A HREF=\"javascript:cottage('cottage/" + orgnames[i] + ".htm')\">");
				p.println ("<IMG SRC=\"" + orgnames[i] + ".gif\">");
				p.println ("</A><BR>");
				p.println (names[i] + "<BR>");
				p.println ("© " + prods[i]);

				if (clones[i] != null) {
					p.println ("<BR>");
					p.println ("<B>GAME CLONE</B>");
				}

				if (sound[i]) {
					p.println("<BR>");
					p.println("<IMG SRC=\"sound.gif\">");
				}

				p.println ("</TD>");

				if (row == 1) {
					p.println ("</TR>");
					row = 0;
				} else
					row = 1;

				i++; k++;
			}

			if (row == 1)
				p.println ("</TR>");

			p.println ("</TABLE>");

			p.println ("<P ALIGN=CENTER>");
			p.println ("<FONT FACE=\"Verdana\" COLOR=GREEN SIZE=+1>");

			if (j>0) {
				if (j == 1)
					p.println ("<A HREF=\"screens_year.htm\">PREV 8</A>");
				else
					p.println ("<A HREF=\"screens_year" + (j-1) + ".htm\">PREV 8</A>");
			}

			next = len - i;
			if (next > 0) {
				next = (next < 8) ? next : 8;
				p.println ("<A HREF=\"screens_year" + (j+1) + ".htm\">NEXT " + next + "</A>");
			}

			p.println ("</FONT>");
			p.println ("</P>");

			p.println ("");
			p.println ("<P ALIGN=CENTER><IMG SRC=\"http://cottage.emuunlim.com/cgi-bin/count.pl\"></P>");
			p.println ("");
			p.println ("</BODY>");
			p.println ("</HTML>");
			p.close();

		} catch (Exception e) { System.err.println ("Error writing to file"); }

		j++;

		}


	}

	public static void main(String args[])
	{
		if (args.length == 2) {
			if (args[0].compareTo("-noclones") == 0) {
				String aux;
				aux = args[0]; args[0] = args[1]; args[1] = aux;
				bListClones = false;
			} else if (args[1].compareTo("-noclones") == 0) {
				bListClones = false;
			}
		}

		if (args.length > 0) {
			if ((args[0].compareTo("-su") == 0) || (args[0].compareTo("-showusage") == 0))
				showUsage();
			else if ((args[0].compareTo("-ls") == 0) || (args[0].compareTo("-list") == 0))
				list();
			else if (args[0].compareTo("-listsourcefile") == 0)
				listsourcefile();
			else if ((args[0].compareTo("-ll") == 0) || (args[0].compareTo("-listfull") == 0))
				listfull();
			else if (args[0].compareTo("-listgames") == 0)
				listgames();
			else if (args[0].compareTo("-gamelist") == 0)
				gamelist();
			else if (args[0].compareTo("-genhtml") == 0)
				genhtml();
			else if (args[0].compareTo("-genhtmlweb") == 0)
				genhtmlweb();
			else
				showUsage();
		} else
			showUsage();
	}

}