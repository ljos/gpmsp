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

package jef.util;

/**
 * @author Erik Duijs, Julien Freilat
 * 
 * Utility class to load ROMS into memory
 */
import java.io.*;
import java.net.URL;
import java.util.zip.*;

public final class RomLoader {

// static final String FILE_SEPERATOR = System.getProperty("file.separator");
 static final String FILE_SEPERATOR = "/";
 private final int MAXLIST = 256;

 private int romlist_useram[][] = new int[MAXLIST][];     /* real memory to use to store the ROM file*/
 private String romlist_names[] = new String[MAXLIST];    /* ROM filename to use */
 private int romlist_storeat[] = new int[MAXLIST];        /* offset in real memory where to load the ROM file */
 private int romlist_length[] = new int[MAXLIST];         /* length of the ROM file to load */
 private int romlist_orglength[] = new int[MAXLIST];      /* length backup */
 private int romlist_crc[] = new int[MAXLIST];            /* CRC of the ROM file */
 private boolean romlist_loaded[] = new boolean[MAXLIST]; /* is this ROM file already loaded ? */
 private boolean romlist_cont[] = new boolean[MAXLIST];   /* is this ROM file is a continued ROM ? */
 private int romlist_skip[] = new int[MAXLIST];           /* bytes to skip for this ROM file */
 private int romlist_usetmp[][] = new int[MAXLIST][];     /* temporary memory to use to store the ROM file */
 private boolean romlist_invert[] = new boolean[MAXLIST]; /* is this ROM file is to be inverted ? */
 private int romlist_index = 0;
 private int load_count = 0;

 private boolean noLoad = false;
 private boolean invert = false;

 private int ram[];
 private String zip;
 private String parent_zip = null;
 private String sub = "";

 public final void noLoading() {
	noLoad = true;
 }

 public final void setMemory(int mem[]) {
 	this.ram = mem;
 }

 public final void setInvert(boolean inv) {
	this.invert = inv;
 }

/* Select ZIP filename to use eventually */
 public final void setZip(String zip) {
	 this.zip = "roms" + FILE_SEPERATOR + zip + ".zip";
//	 romlist_index = 0;
 }

/* Select parent ZIP filename to use eventually */
 public final void setParentZip(String parent) {
	 if (parent == null)
		this.parent_zip = null;
	 else
	 	this.parent_zip = "roms" + FILE_SEPERATOR + parent + ".zip";
//	 romlist_index = 0;
 }

/* Select ZIP filename to use eventually (with subdirectory) */
 public final void setZip(String zip, String sub) {
	 this.zip = "roms" + FILE_SEPERATOR + zip + ".zip";
	 this.sub = sub + FILE_SEPERATOR;
//	 romlist_index = 0;
 }

/* Request to read a file to memory */
 public final void loadROM(String FName, int StoreAt, int Length) {
	 loadROM(FName, StoreAt, Length, 0);
 }

/* Request to read a file to memory (with CRC check) */
 public final void loadROM(String FName, int StoreAt, int Length, int crc) {
	String name;

	/* Store needed info */
	this.romlist_useram[romlist_index] = this.ram;
	name = sub + FName;
	this.romlist_names[romlist_index] = name.toLowerCase();
	this.romlist_storeat[romlist_index] = StoreAt;
	this.romlist_length[romlist_index] = Length;
	this.romlist_orglength[romlist_index] = Length;
	this.romlist_crc[romlist_index] = crc;
//	System.out.println(Integer.toHexString(crc) + "," + Integer.toHexString(romlist_crc[romlist_index]));
	this.romlist_loaded[romlist_index] = false;
	this.romlist_cont[romlist_index] = false;
	this.romlist_usetmp[romlist_index] = null;
	this.romlist_skip[romlist_index] = 1;
	this.romlist_invert[romlist_index] = invert;

	romlist_index++;
 }

/* Request to read a file to memory (with CRC check) */
 public final void loadROM(String FName, int StoreAt, int Length, int crc, int skip) {
	String name;

	/* Store needed info */
	this.romlist_useram[romlist_index] = this.ram;
	name = sub + FName;
	this.romlist_names[romlist_index] = name.toLowerCase();
	this.romlist_storeat[romlist_index] = StoreAt;
	this.romlist_length[romlist_index] = Length;
	this.romlist_orglength[romlist_index] = Length;
	this.romlist_crc[romlist_index] = crc;
//	System.out.println(Integer.toHexString(crc) + "," + Integer.toHexString(romlist_crc[romlist_index]));
	this.romlist_loaded[romlist_index] = false;
	this.romlist_cont[romlist_index] = false;
	this.romlist_usetmp[romlist_index] = null;
	this.romlist_skip[romlist_index] = 1+skip;
	this.romlist_invert[romlist_index] = invert;

	romlist_index++;
 }

/* Request to read a file to memory (with CRC check) */
 public final void continueROM(int StoreAt, int Length) {
	String name;
	int i;

	/* Store needed info */
	this.romlist_useram[romlist_index] = this.ram;
	this.romlist_names[romlist_index] = null;
	this.romlist_storeat[romlist_index] = StoreAt;
	this.romlist_length[romlist_index] = Length;
	this.romlist_orglength[romlist_index] = Length;
	this.romlist_crc[romlist_index] = 0;
	this.romlist_loaded[romlist_index] = true;
	this.romlist_cont[romlist_index] = true;
	this.romlist_usetmp[romlist_index] = null;
	this.romlist_invert[romlist_index] = invert;

	/* Update length of parent ROM to continue */
	i = romlist_index-1;
	while (this.romlist_cont[i])
		i--;
	this.romlist_length[i] += Length;

	romlist_index++;
	/* Do not count continued ROM entries */
	load_count++;
 }

/* Dispatch the memory contents read into real memory */
 private final void storeMemory() {
	int i,j,org;
	int ofs;
	int cur_ofs;
	int skip;

	i = 0;
	while (i<this.romlist_index) {
		/* Is next ROM a continued ROM ? */
		if ((i+1<this.romlist_index) && this.romlist_cont[i+1]) {
			/* Keep number for the original ROM */
			org = i;
			/* Store the original ROM contents */
			ofs = this.romlist_storeat[i];
			if (this.romlist_invert[i])
				for (j=0; j<this.romlist_orglength[i]; j++)
					this.romlist_useram[i][ofs++] = (this.romlist_usetmp[i][j]^0xff)&0xff;
			else
				for (j=0; j<this.romlist_orglength[i]; j++)
					this.romlist_useram[i][ofs++] = this.romlist_usetmp[i][j];
			cur_ofs = this.romlist_orglength[i];
			/* Store all continued ROMs */
			i+=1;
			while ((i<this.romlist_index) && this.romlist_cont[i]) {
				ofs = this.romlist_storeat[i];
				if (this.romlist_invert[org])
					for (j=0; j<this.romlist_length[i]; j++)
						this.romlist_useram[i][ofs++] = (this.romlist_usetmp[org][cur_ofs++]^0xff)&0xff;
				else
					for (j=0; j<this.romlist_length[i]; j++)
						this.romlist_useram[i][ofs++] = this.romlist_usetmp[org][cur_ofs++];
				i+=1;
			}
			/* Free temporary buffer */
			this.romlist_usetmp[org] = null;
		/* Normal ROM */
		} else {
			ofs = this.romlist_storeat[i];
			skip = this.romlist_skip[i];
			if (this.romlist_invert[i])
				for (j=0; j<this.romlist_length[i]; j++) {
					this.romlist_useram[i][ofs] = (this.romlist_usetmp[i][j]^0xff)&0xff;
					ofs += skip;
				}
			else
				for (j=0; j<this.romlist_length[i]; j++) {
					this.romlist_useram[i][ofs] = this.romlist_usetmp[i][j];
					ofs += skip;
				}
			/* Free temporary buffer */
			this.romlist_usetmp[i] = null;
			i+=1;
		}
	}
 }

/* Read files from either disk or web */
 public final void loadZip(URL rootURL) {
	 if (noLoad)
		return;

	 if (rootURL == null)
		 loadFromDisk();
	 else {
		 /* Load parent zip if available */
		 if (parent_zip != null)
			loadFromWeb(rootURL,true);
		 /* Load zip */
		 loadFromWeb(rootURL,false);
		 storeMemory();
	 }

	 /* Reinitialize counters */
	 romlist_index = 0;
       load_count = 0;
 }

/* Read files to memory from disk */
 private final void loadFromDisk() {
	DataInputStream in;
	int i;
	int ofs;

	try {
		for(i=0; i<this.romlist_index; i++) {
			System.out.print("Loading " + this.romlist_names[i] + "(" + Integer.toHexString(this.romlist_crc[i]) + ")...");
			in = new DataInputStream(new FileInputStream(this.romlist_names[i]));
			try {
				ofs = this.romlist_storeat[i];
				while(true) {
					this.romlist_useram[i][ofs++] = in.readUnsignedByte();
				}
			} catch (EOFException e) {
				System.out.println("");
				System.out.println("ERROR loading ROM:");
				System.out.println(e);
		 		System.exit(0);
			}
			in.close();
			in = null;
			System.out.println("Ok!");
		}
	} catch (IOException e) {
		System.out.println("");
		System.out.println("ERROR loading ROM:");
		System.out.println(e);
 		System.exit(0);
	}
 }

/* Read files to memory from web (with Zip support) */
 private final void loadFromWeb(URL baseURL, boolean bParent) {
	 URL romURL = null;
	 InputStream is = null;
	 ZipInputStream zis = null;
	 ZipEntry ze;
	 byte[] buffer;
	 int i,j;
	 String zipName;
	 int length;
	 int nreadbytes;
	 int nbytes;
	 int toreadbytes;

	 try {
		 if (bParent)
		 	romURL = new URL (baseURL, parent_zip);
		 else
		 	romURL = new URL (baseURL, zip);
		 is = romURL.openStream();
	 } catch (Exception e) {
		System.out.println("ERROR loading ROM: error opening stream");
		System.out.println(e);
 		for(;;);
	 }

	 if (is == null) {
         	System.out.println("ERROR loading ROM: null InputStream");
		for(;;);
	 }

	 zis = new ZipInputStream(is);

	 ze=null;
	 for(;;) {
		try {
	   		ze = zis.getNextEntry();
		} catch (Exception e) {
	      	System.out.println("ERROR loading ROM: error getting zip entry");
			System.out.println(e);
         	for(;;);
		}

	   	if (ze == null) {
			if ((load_count != this.romlist_index) && (!bParent)) {
				for(i=0; i<this.romlist_index; i++) {
					if (this.romlist_loaded[i] == false) {
						System.out.println(this.romlist_names[i] + "(" + Integer.toHexString(this.romlist_crc[i]) + ") not found !");
						try {
							jef.video.Console.TXT[jef.video.Console.cTXT]=this.romlist_names[i] + "(" + Integer.toHexString(this.romlist_crc[i]) + ") not found !";
							jef.video.Console.update();
							jef.video.Console.cTXT = (jef.video.Console.cTXT+1)%jef.video.Console.nTXT;
						} catch (Exception e) {}
					}
				}
	        	for(;;);
			}
			return;
		}

		zipName = ze.getName().toLowerCase();
	   	//System.out.println("ZE : " + ze.getName() + "," + Long.toHexString(ze.getCrc()));
		for(i=0; i<this.romlist_index; i++) {
			//if ((!this.romlist_loaded[i]) && (ze.getCrc() == (long)this.romlist_crc[i]) ) {
			if ((!this.romlist_loaded[i]) && ((this.romlist_crc[i]==0) || (Long.toHexString(ze.getCrc()).equals(Integer.toHexString(this.romlist_crc[i])))) ) {
			//if ((!this.romlist_loaded[i]) && (ze.getName().equals(this.romlist_names[i]))) {
				if (this.romlist_crc[i] == 0)
					System.out.print("Loading " + this.romlist_names[i] + "(NO GOOD DUMP KNOWN)...");
				else
					System.out.print("Loading " + this.romlist_names[i] + "(" + Integer.toHexString(this.romlist_crc[i]) + ")...");
				try {
					jef.video.Console.TXT[jef.video.Console.cTXT]="Loading " + this.romlist_names[i] + "...";
					jef.video.Console.update();
				} catch (Exception e) {}

				length = this.romlist_length[i];

				buffer = new byte[length];

			  	nbytes = length;
				toreadbytes = length;

		 		try {
					while (toreadbytes>0) {
						nreadbytes = zis.read(buffer,nbytes - toreadbytes, toreadbytes);
						if (nreadbytes == -1)
							break;
						toreadbytes -= nreadbytes;
				  	}
			    } catch (Exception ignored){
					System.out.println("ERROR loading ROM: invalid ZIP file");
				    	for(;;);
			    }

				this.romlist_usetmp[i] = new int[length];
	 			for (j=0; j<length; j++)
		 			this.romlist_usetmp[i][j] = (buffer[j] + 256) & 0xff;

				buffer = null;

				this.romlist_loaded[i] = true;
				load_count++;

				System.out.println("Ok!");
				try {
					jef.video.Console.TXT[jef.video.Console.cTXT]+="Ok!";
					jef.video.Console.update();
					jef.video.Console.cTXT = (jef.video.Console.cTXT+1)%jef.video.Console.nTXT;
				} catch (Exception e) {}

				break;
			}
		}
	}
 }

/* Read a file to memory from a URL
 public final void loadROMFromURL(URL rootURL, String FName, int StoreAt, int Length) throws IOException {
	byte buffer[] = new byte [Length];
	int readbytes=0;
	try {
		InputStream is;
		URL romURL = new URL (rootURL, FName);
		is = romURL.openStream();
		BufferedInputStream bis = new BufferedInputStream (is, Length);
		int n=Length;
		int toRead = Length;
		while ( toRead > 0 ) {
			int nRead = bis.read( buffer, n-toRead, toRead );
			toRead -= nRead;
			readbytes += nRead;
		}
		for (int i=0;i<Length;i++) {ram[StoreAt+i]=(buffer[i]+256)&0xff;}
		is.close();
		System.out.println("Ok!");
	}
	catch (Exception e) {
		System.out.println ("ERROR");
		System.exit(0);
	}
 }*/
};


