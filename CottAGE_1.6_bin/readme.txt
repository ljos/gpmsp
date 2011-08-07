       C o t t A G E

Cott Arcade Generic Emulator



History:
-------

   CottAGE 1.6
   -----------
   N.B. The CottAGE versioning will now be in sync with JEmu2 from now on.
   This version is extracted from JEmu2 version 1.6.1 (http://www.gagaplay.com), which is an
   enhanced version of CottAGE: Much better rendering, better timing, better sound, online high-
   scores and more :-)
   Some components from JEmu2 were left out (The JEmu2 code itself, the 68k emulator and the CPS1
   driver).
   As of this version, java 1.1 (i.e. The MS VM) will no longer be supported.

   [JEF CHANGES & FIXES]
   * Added emulation of the YM2203 sound chip [Erik Duijs]
   * Added a sample player [Erik Duijs]
   * Fixed R register emulation of the Z80 cpu (this fixed arkanoid) [Erik Duijs]
   * Changed the sound implementation. It's now javax.sound only, which means that JEF is now
     not compatible with java 1.1 anymore, but it's only 1.3 and up. The new sound implementation
     moved the sound thread to the main thread and updates once a frame. This solved the
     infamous timing errors. [Erik Duijs]
   * Abstracted the memory mapping, and added a few alternative implementations. [Erik Duijs]
   * Added support for online high scores [Erik Duijs]
   * Many changes and fixes from the last few years I forgot to document :-) [Erik Duijs]

   [COTTAGE DRIVER CHANGES AND FIXES]
   * Fixed the arkanoid driver [Erik Duijs]
   * Fixed the gng driver [Erik Duijs]
   * Added sound to Commando, 1942, 1943, Gun Smoke, Bomb Jack, Arkanoid, and Ghosts 'n Goblins 
     [Erik Duijs]
   * Fixed the M62 Driver [Erik Duijs]
   * Many changes and fixes from the last few years I forgot to document :-) [Erik Duijs]

   [KNOWN ISSUES & TODO LIST]
   * The new sound implementation depends on precise timing, which is very difficult (if not
     impossible) to do with a pre 1.5 version of java. Sometimes this leads to audible pops and
     clicks, due to buffer underruns.
   * TODO:Add support for high precision timers of 1.5 and the unofficial 1.4 high precision timer.
   * For some reason, zoomed display modes have become extremely slow.

   CottAGE 0.13
   ------------
   
   [JEF CHANGES & FIXES]
    * Encapsulated jef.video.GfxManager
    * Replaced public int[] getPixels() in jef.machine.Machine with public BitMap getDisplay()
    * Updated jef.machine.BasicMachine according to the changes in jef.machine.Machine
    * added an update(BitMap bitmap) function to jef.video.GfxProducer
    * Changed jef.video.Vh_refresh to return a BitMap instead of an int[]

   [DRIVER CHANGES & FIXES]
    * Fixed the video offset bug in 1942 and gunsmoke.
    * Changed all video drivers according to the changed jef.video.Vh_refresh
   

   CottAGE 0.12
   ------------
   This version is a bugfix release. Version 0.11 was released too early. Sorry for the inconvenience.

   [CHANGES & FIXES]
    * Reorganized imports in all classes. [Erik Duijs]
    * Removed absolute paths in makecot.bat that caused it to fail almost everywhere. [Erik Duijs]
    * Fixed the clipping bug that affected 1943 and rocnrope where it caused an offset. [Erik Duijs]
    * Fixed the 1-pixel offset bug in scrolling games. [Erik Duijs]
    * Re-enabled mspacman. [Erik Duijs]
    * Re-enabled the old Pacman driver to use with pacman, mspacman, lizwiz, eyes, dremshpr, mrtnt
      and theglob in order to have sound working again in those games. [Erik Duijs]
    * Encapsulated jef.video.GfxProducer and changed all affected classes. [Erik Duijs]
    * Updated JEF version to 1.0 Beta 2. [Erik Duijs]
     


   CottAGE 0.11
   ------------

   [DRIVERS CHANGES]

	[NEW GAMES ADDED]

		- [M62] Add preliminary support for ldrun [Gollum]

		- [Mrjong]
			* Fixes support for mrjong [Gollum]
			* Add preliminary support for mrjong [LFE]

		- [Pacman]
			* Add support for pacplus [Gollum]
			* Add support for jumpshot [Gollum]

		- [Pooyan]
			* Fixes partially support for pooyan [Gollum]
			* Add preliminary support for pooyan [LFE]

		- [Rallyx]
			* Fixes partially support for rallyx [Gollum]
			* Add very preliminary support for rallyx [LFE]

		- [Sonson] Add support for sonson [Gollum]

		- [Troangel]
			* Fixes support for troangel [Gollum]
			* Add preliminary support for troangel [LFE]

   	[NEW CLONES ADDED]

		- [Mrjong]
			* Fixes support for crazyblk [Gollum]
			* Add preliminary support for crazyblk [LFE]

		- [Pacman]
			* Add support for puckmana, pacman, puckmod, pacmod [Gollum]
			* Add support for hangly, hangly2, newpuckx, pacheart [Gollum]
			* Add support for piranha [Gollum]
			* Add support for mspacmab, mspacpls, pacgal [Gollum]
			* Add support for crush2, crush3, mbrush, paintrlr [Gollum]
			* Add support for ponpokov [Gollum]
			* Add support for eyes2 [Gollum]

		- [Pooyan]
			* Fixes partially support for pooyans, pootan [Gollum]
			* Add preliminary support for pooyans, pootan [LFE]

		- [Sonson] Add support for sonsonj [Gollum]

	[DRIVER FIXES]

		- [1943]
			* Rename gunsmrom in gunsmoku (MAME 0.62) [Gollum]
			* Fixes defunct clipping [Erik Duijs]

		- [Bw8080] Fixes invaders rom loading [Erik Duijs]

		- [Circusc] Add preliminary sound support for circusc [Erik Duijs]

		- [Dkong] Add support for dkongo (MAME 0.62) [Gollum]

		- [Pacman]
			* Rewrite a full MAME compliant driver [Gollum]
			* Move pacman video driver to pengo video driver [Gollum]
			* Disable support for mspacman [Gollum]
			* Rename pacman into puckman [Gollum]
			* Rewrite support for eyes, lizwiz, mrtnt, pacman [Gollum]
			* Rewrite support for ponpoko [Gollum]

		- [Rocnrope] Fixes defunct clipping for rocnrope and subdrivers [Erik Duijs]

		- [Solomon] Add preliminary sound support for solomon [Erik Duijs]

   [CPU CORES CHANGES]   

	[CPU CORES FIXES]

		- [I8080] Fixes a few memory reads [Erik Duijs]

   [SOUND CORES CHANGES]

   	[NEW SOUND CORES ADDED]
		
		- [Emu2413] Add support for YM2413 sound hardware [Romain Tisserand]

   [CORE CHANGES]

	[NEW FEATURES]

		- Add support for Zoom Mode [Gollum]
		- Add support for AdvanceMAME Scale2X Effect [Gollum]
		- Add support for ROM inverting [Gollum]
		- Add support for "NO GOOD DUMP KNOWN" for roms [Gollum]

	[GUI CHANGES]

		- Add support for reset [Erik Duijs]

	[OPTIMIZATIONS]

		- Optimize TV Mode Effect [Gollum]
		- Optimize all bitmap functions [Erik Duijs/Sandy McArthur]
		- Add optimizations when sound disabled [Erik Duijs]

	[MAIN CHANGES]

		- Fixes global variables for videoram and driver information [Erik Duijs]
		- Fixes sound initialization and setup [Erik Duijs]
		- Add a lot of comments to the whole code [Erik Duijs]
		- Add preliminary support for driver imports [Gollum]
		- Add support for a few new MAME constants [Gollum]
		- Move driver code (MAME specific) to CottAGE package [Erik Duijs]
		- Move Rom loader code to its own JEF class [Erik Duijs]
		- Move GUI console code to its own JEF class [Erik Duijs]
		- Move throttle and frameskip code to its own JEF class [Erik Duijs]
		- Add versioning support for JEF package [Erik Duijs]
		- Restructure the whole source code to separate JEF and CottAGE [Erik Duijs]
		- Fixes clipping support for draw gfx [Gollum]
		- Fixes palette support [Erik Duijs]
		- Fixes rom loading and debugging [Erik Duijs]
		- Add IRQ constant names to all machine drivers [Erik Duijs]
		- Add support for IRQ lines [Erik Duijs]
		- Add support for sound cpu flag [Erik Duijs]
		- Add hardware reset support [Erik Duijs]

--------------------------------------------------------------------------------------------

   CottAGE 0.10
   ------------

   [DRIVERS CHANGES]

	[NEW GAMES ADDED]

		- [1942] Add preliminary support for 1942 [Gollum]

		- [Bankp] Add support for bankp [Gollum]

		- [Circusc] Add support for circusc [Erik Duijs/Gollum]

		- [Gberet]
			* Add support for mrgoemon [Gollum]
			* Add preliminary support for gberet [Erik Duijs]

		- [Mario] Add support for mario [Gollum]

		- [Pingpong] Add support for pingpong [Gollum]

		- [Safarir] Add preliminary support for safarir [Gollum]

   	[NEW CLONES ADDED]

		- [1942] Add support for 1942a, 1942b [Gollum]

		- [1943] Add support for gunsmrom, gunsmoka, gunsmokj [Gollum]

		- [Arkanoid] Add preliminary support for arkangc, arkatayt, arkbloc2 [Gollum]

		- [Bublbobl] Add support for boblbobl, sboblbob, tokiob [Gollum]

		- [Circusc] Add support for circusc2, circuscc, circusce [Gollum]

		- [Gberet] Add support for rushatck [Gollum]

		- [Mario] Add support for mariojp, masao [Gollum]

	[DRIVER FIXES]

		- [Bankp] Add sound support for bankp [Erik Duijs]

		- [Bublbobl]
			* Fixes palette bugs for boblbobl, sboblbob, tokiob [Erik Duijs]
			* Fixes support for boblbobl, sboblbob [Gollum]

		- [Circusc] Fixes support for circusc [Gollum]

		- [Galaga]
			* Fixes support for galaga [Erik Duijs]
			* Add video support for galaga [Erik Duijs]
			* Add sound support for galaga [Erik Duijs]

		- [Gberet]
			* Fixes a palette bug [Gollum]
			* Add sound support for gberet and all subdrivers [Erik Duijs]
			* Fixes support for gberet [Erik Duijs]

		- [Hexa] Add preliminary sound support for hexa [Erik Duijs]

		- [Pacman]
			* Add sound support for dremshpr [Erik Duijs]
			* Fixes support for dremshpr, theglob (unmapped memory) [Erik Duijs]

		- [Pingpong] Add sound support for pingpong [Erik Duijs]

   [CPU CORES CHANGES]   

	[CPU CORES FIXES]

		- [Z80]
			* Completely rewrite the whole code for faster performance on Sun [Erik Duijs]
			* Fixes PUSH BC [Erik Duijs]
			* Remove PC from fetch-decode loop [Erik Duijs]
			* Fixes LD D,HX / LD D,LX / LD D,HY / LD D,LY [Erik Duijs]

   [SOUND CORES CHANGES]

   	[NEW SOUND CORES ADDED]
		
		- [SN76496] Add support for SN76496 sound hardware [Erik Duijs]

   	[SOUND CORES FIXES]

		- [AY8910] Finish support for the AY-8910 sound hardware [Erik Duijs]
		
		- [Namco] Fixes mixing routines (better sound) [Gollum]

		- [SN76496] Fixes support for sun.audio [Gollum]

   [CORE CHANGES]

	[NEW FEATURES]

		- Add new keyboard definitions for player two [Gollum]
		- Add support for cocktail mode [Gollum]
		- Add support for two players mode [Gollum]
		- Add CRC support for roms loading [Erik Duijs]
		- Add preliminary support for .inp (record inputs) files [Gollum]
		- Greatly improved sound quality for sun.audio [Gollum]
		- Add tags support for cpus [Erik Duijs]
   		- Add a very useful tracing log for Z80 cpu [Erik Duijs] 

	[GUI CHANGES]

		- Add new keyboard definitions [Gollum]
		- Catch all exceptions for throttling code [Erik Duijs]

	[OPTIMIZATIONS]

		- Optimize semi alpha blending display [Gollum]
		- Optimize 16bit memory accesses for basic cpuboard [Erik Duijs]

	[MAIN CHANGES]

		- Fixes 180 degrees rotated display [Gollum]
		- Fixes clipping bugs in drawgfx functions [Gollum]
		- Fixes copyscrollbitmap functions in some cases [Gollum]
		- Change cpuboards to always use memory maps (Galaga) [Erik Duijs]
		- Completely restructure the whole source code [Erik Duijs]

--------------------------------------------------------------------------------------------

   CottAGE 0.05
   ------------

   [DRIVERS CHANGES]

	[NEW GAMES ADDED]

		- [1943] Add support for 1943, 1943kai, gunsmoke [Erik Duijs]

		- [Dkong] Add support for dkong, dkongjr, dkong3, radarscp [Erik Duijs]	

		- [Hexa] Add support for hexa [Gollum]	

		- [Hyperspt] Add support for hyperspt, roadf [Erik Duijs]

		- [M79amb] Add support for m79amb [Gollum]

		- [Rocnrope] Add support for rocnrope [Erik Duijs]

	[NEW CLONES ADDED]

		- [1943]
			* Fixes support for 1943j [Gollum]
			* Add support for 1943j [Erik Duijs]

		- [Dkong]
			* Add support for dkong3j [Gollum]
			* Add support for dkongjrj, dkngjnrj, dkongjrb, dkngjnrb [Gollum]
			* Add support for dkongjp, dkongjo, dkongjo1 [Gollum]

		- [Hyperspt] Add support for hpolym84, roadf2 [Erik Duijs]

		- [Rocnrope]
			* Fixes support for rocnropk [Gollum]
			* Add support for rocnropk [Erik Duijs]

	[DRIVER FIXES]

		- [1943] Merge gunsmoke driver (MAME todo list) [Erik Duijs]
	
		- [Dkong] Fixes support for dkongjr [Gollum]

		- [Minivadr]
			* Add a CottAGE optimized version (default) [Gollum]
			* Add a 95% straight MAME video driver port [Gollum]

   [CPU CORES CHANGES]   

	[CPU CORES FIXES]

		- [CPU6809]
			* Fixes two indexed addressing modes [Erik Duijs]
			* Fixes interrupt/CWAI [Erik Duijs]
			* Add support for encrypted opcodes [Erik Duijs]

   [SOUND CORES CHANGES]

   	[NEW SOUND CORES ADDED]
		
		- [AY8910] Start preliminary AY-8910 sound hardware [Erik Duijs]

   [CORE CHANGES]

	[NEW FEATURES]

   		- Start cpu debugger support [S.C.Wong] 

	[GUI CHANGES]

		- Add maximum speed detection and display [Gollum]
		- Change default keys [Gollum]
		- Disable simultaneous pause/ROM loading/FPS display [Gollum]
		- Improve ROM loading with nicer display [Gollum]
		- Improve pause support with nicer display [Erik Duijs]
		- Add a nice GFX display for FPS [Erik Duijs]

	[OPTIMIZATIONS]

		- Optimize bitmap drawing functions by 15% [Gollum]
		- Optimize memory banks support [Gollum]
		- Optimize default read/write memory handlers [Gollum]
		- Optimize default read/write port handlers [Gollum]

	[MAIN CHANGES]

		- Add new GFX scrolling features [Erik Duijs]
		- Add support for init handlers [Gollum]
		- Add support for memory banks [Erik Duijs]

--------------------------------------------------------------------------------------------

   CottAGE 0.04 - 22/06/2002
   -------------------------

   [DRIVERS CHANGES]

	[NEW GAMES ADDED]

		- [Dotrikun] Add support for dotrikun [Gollum]

		- [Galaga] Add preliminary support for galaga [Erik Duijs]

   		- [Galaxian] Add support for warofbug [Erik Duijs/Gollum]

   		- [Minivadr] Add support for minivadr [Gollum]

		- [News] Add support for news [Gollum]

		- [Pacman] Add support for vanvan, dremshpr [Erik Duijs]

   		- [Yiear] Add support for yiear [Erik Duijs]

   	[NEW CLONES ADDED]

 		- [Blktiger] Add support for bktigerb, blkdrgon, blkdrgnb [Gollum]

	   	- [Bombjack] Add support for bombjac2 [Gollum]

		- [Bw8080] Add support for invrvnge, invrvnga [Gollum]

		- [Dotrikun] Add support for dotriku2 [Gollum]

		- [Galaxian] Add support for galaxiaj [Erik Duijs]

   		- [Yiear] Add support for yiear2 [Gollum]

	[DRIVER FIXES]

		- [Blktiger]
			* Fixes wrong colors [Erik Duijs]
			* Find the slowdown bug ! [Gollum]

	   	- [Bombjack]
			* Fixes video bugs [Erik Duijs]
			* Rewrite driver using new MAME-like architecture [Gollum]

		- [Bw8080]
			* Remove VIDEO_MODIFIES_PALETTE flag [Gollum]
			* Add non-merged roms support [Gollum]
			* Fixes invrvnge support [Gollum]

		- [Dotrikun] Fixes support for dotrikun, dotriku2 [Gollum]

		- [Galaxian]
			* Fixes galaxiaj support [Gollum]
			* Fixes scramblb support (R register) [Erik Duijs]
			* Rewrite scramblb roms loading [Erik Duijs]

		- [Gyruss] Fixes some bugs [Erik Duijs]

		- [Solomon]
			* Fixes wrong colors [Erik Duijs]
			* Rewrite driver using new MAME-like architecture [Gollum]

   		- [Yiear]
	   		* Rewrite driver using new MAME-like architecture [Gollum]
			* Fixes support for yiear [Gollum]

   [CPU CORES CHANGES]   
		
	[NEW CPU CORES ADDED]

		- [CPU6809] Add cpu 6809 emulation core [S.C.Wong]

	[CPU CORES FIXES]

		- [CPU6809] Fixes 6809 compilation errors [Arnon Cardoso]
		- [Z80]
			* Fixes stack pointer support [Erik Duijs/Gollum]
			* Optimize interrupts handling [Erik Duijs]
			* Optimize main loop [Erik Duijs]
			* Rename some variables [Erik Duijs]
			* Add preliminary support for R register [Erik Duijs]

   [SOUND CORES CHANGES]

   	[SOUND CORES FIXES]
		
		- [Namco] Add javax.sound support [Erik Duijs]

   [CORE CHANGES]

	[NEW FEATURES]

		- Add generation of HTML games, menu and index pages (Info tool) [Gollum]
		- Add list generation of supported games (Info tool) [Gollum]
		- Add list generation of game names and descriptions (Info tool) [Gollum]
		- Add list generation of supported games, year, manufacturer (Info tool) [Gollum]
		- Add list generation of drivers source files (Info tool) [Gollum]
		- Add -noclones option (Info tool) to not list alternate versions [Gollum]
		- Add a command-line usage Info tool [Gollum]
		- Add support javax.sound [Erik Duijs]

	[GUI CHANGES]

		- Add configurable sound buffer for applet (only javax.sound) [Erik Duijs]
		- Add configurable sampling rate for applet (only javax.sound) [Erik Duijs]
		- F9 key increases frameskip and disable auto-frameskip [Erik Duijs]
		- F8 key decreases frameskip and enable auto-frameskip when 0 [Erik Duijs]
		- Improve again throttling and auto-frameskip support [Arnon Cardoso]
		- Improve pause support with a nice filtering and message [Gollum]
		- Add auto-frameskip support [Arnon Cardoso]
	   	- Fixes 50% scanlines in x2 mode [Gollum]
   		- Fixes antialiasing in x2 mode [Gollum]
	   	- Add frame skipping support (up to 12 levels) [Gollum]
	   	- Add a gfx rom loader to inform user of the loading process [Gollum]

	[HTML/DOC CHANGES]

		- Rewrite readme.txt [Gollum]
	   	- Improve HTML pages with correct game names [Gollum]
   		- Improve HTML pages with gfx background and no border [Gollum]

	[OPTIMIZATIONS]

		- Optimize again driver selection [Gollum]
	   	- Optimize some bitmap functions [Gollum]
   		- Optimize java gfx rendering [Gollum]
	   	- Optimize driver selection [Gollum]
		- Optimize x2 mode [Gollum]

	[MAIN CHANGES]

		- Add tilemap stuff [Gollum]
		- Remove color codes caching system (less memory usage) [Erik Duijs]
		- Fixes VIDEO_MODIFIES_PALETTE handling [Erik Duijs]
		- Add video_post_update function [Erik Duijs]
		- Improve MAME-like driver architecture [Gollum]
		- Fixes sound code (not denied access anymore in Sun JRE 1.4) [Erik Duijs]
		- Fixes a gfx bug for 180 or 270 rotated games [Erik Duijs]
		- Add generic driver selection [Gollum]
   		- Rewrite sound support in CottAGE [Erik Duijs]
		- Start rewriting inputport features [Erik Duijs]	
	   	- Add support for a more MAME-like driver architecture [Gollum]
   		- Add support for auto-loading of non-merged roms [Gollum]
   		- Add support for continued ROMs [Gollum]
	   	- Fixes system exit on errors in rom loader [Gollum]
   		- Fixes constants in rotation code [Gollum]
	   	- Fixes reset of driver cpus (invrvnge) [Gollum]
		- Add new basic driver constants [Erik Duijs]
		- Reduce memory usage of Gfx Manager's caching system [Erik Duijs]
   		- Rewrite tile caching [Erik Duijs]
   		- Separate video constants from other constants [Erik Duijs]

--------------------------------------------------------------------------------------------

   CottAGE 0.03 - 15/05/2002
   -------------------------

   [DRIVERS CHANGES]

	[NEW GAMES ADDED]

   		- [Galaxian] Add support for galaxian, scramblb [Erik Duijs]

   	[NEW CLONES ADDED]

   		- [Bw8080]
			* Add support for alieninv, cosmicmo, jspecter, jspectr2 [Gollum]
			* Add support for spaceatt, invaderl, spceking, spcewars [Gollum]
			* Add support for spacewr3, superinv, sinvemag, sinvzen [Gollum]

	[DRIVER FIXES]

   		- [Bw8080]
			* Fixes spaceat2, boothill [Gollum]
   			* Rewrite driver selection [Gollum]
   			* Resync some subdrivers with MAME 0.60s [Gollum]

   		- [Pacman] Add sound support to all subdrivers [Erik Duijs]

   [SOUND CORES CHANGES]

   	[NEW SOUND CORES ADDED]
		
		- [Namco] Add namco sound hardware [Erik Duijs]

   [CPU CORES CHANGES]

	[CPU CORES FIXES]

   		- [Z80] Removed localization of PC in fetch-decode loop. [Erik Duijs]

   [CORE CHANGES]

	[NEW FEATURES]

   		- Add sound support in CottAGE [Erik Duijs]

	[GUI CHANGES]

   		- Rewrite throttling code [Arnon Cardoso]
	   	- Start writing new throttling code [Erik Duijs/Arnon Cardoso]
   		- Add sound enable/disable option [Erik Duijs]

	[HTML/DOC CHANGES]

   		- Improve HTML pages with games with sound [Gollum]
   		- Improve HTML pages with sorted games [Gollum]
   		- Improve HTML pages with clones and games [Gollum]

	[MAIN CHANGES]

   		- Improve bitmap facilities [Erik Duijs]
   		- Fixes XY scrolling playfield [Erik Duijs]
   		- Add support for video granularity [Erik Duijs]
   		- Fixes error messages in zip file support [Erik Duijs]
   		- Rename all drivers according to MAME 0.60s [Gollum]
   		- Add soundchip support [Erik Duijs]
   		- Resync directories with MAME 0.60s [Gollum]

--------------------------------------------------------------------------------------------

   CottAGE 0.02 - 10/05/2002
   -------------------------

   [DRIVERS CHANGES]

	[DRIVER FIXES]

   		- [Bombjack] Fixes driver information [Erik Duijs/Gollum]

   [CPU CORES CHANGES]

	[CPU CORES FIXES]

   		- [Z80] Add INI/IND opcodes [Erik Duijs]

   [CORE CHANGES]

	[GUI CHANGES]

   		- Add a weird trick for throttling on Win9X/Win2K [Erik Duijs]

	[HTML/DOC CHANGES]

   		- Fixes HTML pages [Erik Duijs/Gollum]
	   	- Fixes compatibility tests [Erik Duijs]

	[MAIN CHANGES]

		- Fixes release package with roms directory [Gollum]
   		- Fixes source package compilation [Arnon Cardoso]

--------------------------------------------------------------------------------------------

   CottAGE 0.01 - 07/05/2002
   -------------------------

   [DRIVERS CHANGES]

	[DRIVER FIXES]

   		- [Bw8080]
			* Fixes sicv, sisv, sisv2, sitv [Gollum]
   			* Resync some subdrivers with MAME 0.58s [Gollum]

   [CORE CHANGES]

	[HTML/DOC CHANGES]

		- Redo some compatibility tests [Gollum]
   		- Improve HTML pages [Gollum]

--------------------------------------------------------------------------------------------

   CottAGE 0.00 - UNRELEASED
   -------------------------

   - All CottAGE design [Erik Duijs]
   - JEF library [Erik Duijs]
   - All drivers work [Erik Duijs]
   - Drivers fixes [Gollum]
   - Romloader fixes [Gollum]




Usage and Distribution License:
------------------------------

I. Purpose:
----------
   CottAGE is strictly a non-profit project. Its main purpose is to be a reference
   to the inner workings of the emulated arcade machines. This is done for
   educational purposes and to prevent many historical games from sinking into
   oblivion once the hardware they run on stops working. Of course to preserve
   the games, you must also be able to actually play them; you can consider
   that a nice side effect.
   It is not our intention to infringe on any copyrights or patents on the
   original games. All of CottAGE's source code is either our own or freely
   available. To operate, the emulator requires images of the original ROMs
   from the arcade machines, which must be provided by the user. No portion of
   the original ROM codes are included in the executable.

II. Cost:
--------
   CottAGE is free. Its source code is free. Selling either is not allowed.

III. ROM Images:
---------------
   ROM images are copyrighted material. Most of them cannot be distributed
   freely. Distribution of CottAGE on the same physical medium as illegal copies
   of ROM images is strictly forbidden.
   You are not allowed to distribute CottAGE in any form if you sell, advertise,
   or publicize illegal CD-ROMs or other media containing ROM images. This
   restriction applies even if you don't make money, directly or indirectly,
   from those activities. You are allowed to make ROMs and CottAGE available for
   download on the same website, but only if you warn users about the ROMs's
   copyright status, and make it clear that users must not download ROMs unless
   they are legally entitled to do so.

IV. Source Code Distribution:
----------------------------
   If you distribute the jar file (java classes) version of CottAGE, you should also
   distribute the source code. If you can't do that, you must provide a link
   to a site where the source can be obtained.

V. Distribution Integrity:
-------------------------
   This chapter applies to the official CottAGE distribution. See below for
   limitations on the distribution of derivative works.
   CottAGE must be distributed only in the original archives. You are not allowed
   to distribute a modified version, nor to remove and/or add files to the
   archive.

VI. Reuse of Source Code:
------------------------
   The source code cannot be used in a commercial product without the written
   authorization of the authors. Use in non-commercial products is allowed, and
   indeed encouraged. If you use portions of the CottAGE source code in your
   program, however, you must make the full source code freely available as
   well.
   Usage of the _information_ contained in the source code is free for any use.

VII. Derivative Works:
---------------------
   Derivative works are allowed, however, these works are discouraged. 
   CottAGE is a continuously evolving project while CottAGE try to maintain the 
   same level of compatibility
   and features. It is in your best interests to submit your contributions
   (drivers or new code for the Jef library) to the CottAGE development team,
   so they may be integrated into the main distribution.



How to Contact Us
-----------------

The official CottAGE homepage is http://cottage.emuunlim.com/
You can always find the latest release there as well as a page
to try some ROMs.



Acknowledgments
---------------

Huge thanks to the whole MAME team for their incredible work.
Thanks to all members of the Java-Emu Team for help, fixes and testing.
