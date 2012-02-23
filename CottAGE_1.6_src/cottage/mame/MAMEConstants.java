package cottage.mame;

import jef.map.MemoryWriteAddress;
import jef.video.VideoConstants;

/**
 * Interface with constants as used in the MAME source.
 */
public interface MAMEConstants extends VideoConstants {

	// Memory region codes
	static final int REGION_CPU1  = 0;
	static final int REGION_CPU2  = 1;
	static final int REGION_CPU3  = 2;
	static final int REGION_CPU4  = 3;
	static final int REGION_CPU5  = 4;
	static final int REGION_CPU6  = 5;
	static final int REGION_CPU7  = 6;
	static final int REGION_CPU8  = 7;
	static final int REGION_GFX1  = 8;
	static final int REGION_GFX2  = 9;
	static final int REGION_GFX3  = 10;
	static final int REGION_GFX4  = 11;
	static final int REGION_GFX5  = 12;
	static final int REGION_GFX6  = 13;
	static final int REGION_GFX7  = 14;
	static final int REGION_GFX8  = 15;
	static final int REGION_PROMS = 16;
	static final int REGION_SOUND1= 17;
	static final int REGION_SOUND2= 18;
	static final int REGION_SOUND3= 19;
	static final int REGION_SOUND4= 20;

	static final int ROMREGION_DISPOSE = 1;
	static final int ROMREGION_INVERT  = 2;

	static final int IRQ_LINE_IRQ  	= jef.cpu.Cpu.INTERRUPT_TYPE_IRQ;
	static final int IRQ_LINE_NMI  	= jef.cpu.Cpu.INTERRUPT_TYPE_NMI;
	static final int IRQ_LINE_FIRQ 	= jef.cpu.Cpu.INTERRUPT_TYPE_FIRQ;

	static final int CPU_AUDIO_CPU	= 1;

	static final int PULSE_LINE		= 1;

	// Memory Read handler IDs
	static final int MRA_RAM 	= 0;
	static final int videoram_r = 0;
	static final int colorram_r = 0;
	static final int MRA_ROM 	= 1;
	static final int MRA_NOP 	= 2;
	static final int MRA_BANK1 	= 3;
	static final int MRA_BANK2 	= 4;
	static final int MRA_BANK3 	= 5;
	static final int MRA_BANK4 	= 6;
	static final int MRA_BANK5 	= 7;
	static final int MRA_BANK6 	= 8;
	static final int MRA_BANK7 	= 9;
	static final int MRA_BANK8 	= 10;

	// Memory Write handler IDs
	static final int MWA_RAM = MemoryWriteAddress.MWA_RAM;
	static final int MWA_ROM = MemoryWriteAddress.MWA_ROM;
	static final int MWA_NOP = MemoryWriteAddress.MWA_NOP;
	static final int MWA_BANK1 = MemoryWriteAddress.MWA_BANK1;
	static final int MWA_BANK2 = MemoryWriteAddress.MWA_BANK2;
	static final int MWA_BANK3 = MemoryWriteAddress.MWA_BANK3;
	static final int MWA_BANK4 = MemoryWriteAddress.MWA_BANK4;
	static final int MWA_BANK5 = MemoryWriteAddress.MWA_BANK5;
	static final int MWA_BANK6 = MemoryWriteAddress.MWA_BANK6;
	static final int MWA_BANK7 = MemoryWriteAddress.MWA_BANK7;
	static final int MWA_BANK8 = MemoryWriteAddress.MWA_BANK8;
	
	// VBlank durations
	static final int DEFAULT_60HZ_VBLANK_DURATION = 0;
	static final int DEFAULT_30HZ_VBLANK_DURATION = 0;
	// If you use IPT_VBLANK, you need a duration different from 0.
	static final int DEFAULT_REAL_60HZ_VBLANK_DURATION = 2500;
	static final int DEFAULT_REAL_30HZ_VBLANK_DURATION = 2500;

	// CPU codes
	static final int   Z80 = 0;
	static final int I8080 = 1;
	static final int M6809 = 2;
	static final int M68000 = 3;
	
	// RAM codes
	static final int videoram     = 0;
	static final int colorram     = 1;
	static final int spriteram    = 2;
	static final int spriteram_2  = 3;
	static final int spriteram_3  = 4;
	static final int paletteram   = 5;
	static final int paletteram_2 = 6;

	// RAM size codes
	static final int videoram_size    = 0;
	static final int spriteram_size   = 1;
	static final int spriteram_2_size = 2;
	static final int spriteram_3_size = 3;

	// Input
	static final int IP_ACTIVE_HIGH 		= 0x00;
	static final int IP_ACTIVE_LOW		= 0xff;
	static final int IPT_UNKNOWN			= 0;
	static final int IPT_UNUSED			= 0;
	static final int IPT_COIN1			= 1;
	static final int IPT_COIN2			= 2;
	static final int IPT_COIN3			= 2;
	static final int IPT_COIN4		  	= 2;
	static final int IPT_START1		  	= 3;
	static final int IPT_START2		  	= 4;
	static final int IPT_START3		  	= 4;
	static final int IPT_START4		  	= 4;
	static final int IPT_JOYSTICK_LEFT  	= 5;
	static final int IPT_JOYSTICK_RIGHT 	= 6;
	static final int IPT_JOYSTICK_UP		= 7;
	static final int IPT_JOYSTICK_DOWN  	= 8;
	static final int IPT_BUTTON1			= 9;
	static final int IPT_BUTTON2			= 10;
	static final int IPT_BUTTON3			= 11;
	static final int IPT_BUTTON4			= 12;
    static final int IPT_BUTTON5          = 13;
    static final int IPT_BUTTON6          = 14;
	static final int IPT_TILT			    = 15;
	static final int IPT_SPECIAL			= 16;
	static final int IPT_SERVICE			= 17;
	static final int IPT_SERVICE1		    = 18;
	static final int KEYCODE_F2			= 19;
	static final int IPT_VBLANK       	= 20;
	static final int IPT_DIPSWITCH_SETTING= 21;
	static final int IPT_DIPSWITCH_NAME 	= 22;
	static final int IP_KEY_NONE			= 0;
	static final int IP_JOY_NONE			= 0;
	static final int IPF_CHEAT            = 0;  // not implemented
	static final int IPF_2WAY			= 0;	// not implemented
	static final int IPF_4WAY			= 0;	// not implemented
	static final int IPF_8WAY			= 0;	// not implemented
	static final int IPF_COCKTAIL		= 128;
	static final int IPF_TOGGLE			= 64;
	static final int IPF_PLAYER1			= 0;
	static final int IPF_PLAYER2			= 128;
	static final int IPF_PLAYER3			= 256;	// not implemented
	static final int IPF_PLAYER4			= 384;	// not implemented
	// analog
	static final int IPT_PADDLE			= 1;
	static final int IPT_DIAL			= 2;
	static final int IPF_REVERSE		= 256;
	static final int IPT_AD_STICK_X 	= 3;
	static final int IPT_AD_STICK_Y 	= 4;
	
	// Dipswitch settings
	static String DEF_STR[] = {
		"Unknown",
		"On",
		"Off",
		"Lives",
		"Bonus Live",
		"Coinage",
		"5 Coins - 1 Credit",
		"4 Coins - 1 Credit",
		"3 Coins - 1 Credit",
		"2 Coins - 1 Credit",
		"1 Coin - 1 Credit",
		"1 Coin - 2 Credits",
		"1 Coin - 3 Credits",
		"1 Coin - 4 Credits",
		"1 Coin - 5 Credits",
		"1 Coin - 6 Credits",
		"1 Coin - 7 Credits",
		"2 Coins - 2 Credits",
		"2 Coins - 3 Credits",
		"2 Coins - 4 Credits",
		"2 Coins - 5 Credits",
		"3 Coins - 2 Credits",
		"3 Coins - 3 Credits",
		"3 Coins - 4 Credits",
		"3 Coins - 5 Credits",
		"4 Coins - 2 Credits",
		"4 Coins - 3 Credits",
		"4 Coins - 4 Credits",
		"4 Coins - 5 Credits",
		"5 Coins - 2 Credits",
		"5 Coins - 3 Credits",
		"5 Coins - 4 Credits",
		"5 Coins - 5 Credits",
		"Cabinet",
		"Upright",
		"Cocktail",
		"Demo Sounds",
		"Free Play",
		"Difficulty",
		"Coinage A",
		"Coinage B",
		"Flip Screen",
		"Yes",
		"No",
		"Unused",
		"Service Mode",
		"6 Coins - 1 Credit",
		"1 Coin - 8 Credits",
		"7 Coins - 1 Credit",
	};

	static final int Unknown = 0;
	static final int On = 1;
	static final int Off = 2;
	static final int Lives = 3;
	static final int Bonus_Life = 4;
	static final int Coinage = 5;
	static final int _5C_1C = 6;
	static final int _4C_1C = 7;
	static final int _3C_1C = 8;
	static final int _2C_1C = 9;
	static final int _1C_1C = 10;
	static final int _1C_2C = 11;
	static final int _1C_3C = 12;
	static final int _1C_4C = 13;
	static final int _1C_5C = 14;
	static final int _1C_6C = 15;
	static final int _1C_7C = 16;
	static final int _2C_2C = 17;
	static final int _2C_3C = 18;
	static final int _2C_4C = 19;
	static final int _2C_5C = 20;
	static final int _3C_2C = 21;
	static final int _3C_3C = 22;
	static final int _3C_4C = 23;
	static final int _3C_5C = 24;
	static final int _4C_2C = 25;
	static final int _4C_3C = 26;
	static final int _4C_4C = 27;
	static final int _4C_5C = 28;
	static final int _5C_2C = 29;
	static final int _5C_3C = 30;
	static final int _5C_4C = 31;
	static final int _5C_5C = 32;
	static final int Cabinet = 33;
	static final int Upright = 34;
	static final int Cocktail = 35;
	static final int Demo_Sounds = 36;
	static final int Free_Play = 37;
	static final int Difficulty = 38;
	static final int Coin_A = 39;
	static final int Coin_B = 40;
	static final int Flip_Screen = 41;
	static final int Yes = 42;
	static final int No = 43;
	static final int Unused = 44;
	static final int Service_Mode = 45;
	static final int _6C_1C = 46;
	static final int _1C_8C = 47;
	static final int _7C_1C = 48;
}