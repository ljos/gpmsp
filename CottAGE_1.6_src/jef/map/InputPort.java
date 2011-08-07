/*
 * 
 * Java Emulation Framework
 * 
 * This library contains a framework for creating emulation software.
 * 
 * Copyright (C) 2002 Erik Duijs (erikduijs@yahoo.com)
 * 
 * Contributors: - Julien Freilat - Arnon Goncalves Cardoso - S.C. Wong -
 * Romain Tisserand - David Raingeard
 * 
 * 
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or (at your
 * option) any later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *  
 */

package jef.map;

import java.awt.event.KeyEvent;


/**
 * @author Erik Duijs
 * 
 * This class implements a ReadHandler for handling input events. TO DO: Make
 * inputs configurable instead of hardcoded MAME input map which has to be done
 * in a cottage.mame class.
 */
public class InputPort implements ReadHandler {

	public static final int IP_ACTIVE_HIGH = 0x00;
	public static final int IP_ACTIVE_LOW = 0xff;
	public static final int IPT_UNKNOWN = 0;
	public static final int IPT_UNUSED = 0;
	public static final int IPT_COIN1 = 1;
	public static final int IPT_COIN2 = 2;
	public static final int IPT_COIN3 = 2;
	public static final int IPT_START1 = 3;
	public static final int IPT_START2 = 4;
	public static final int IPT_START3 = 4;
	public static final int IPT_JOYSTICK_LEFT = 5;
	public static final int IPT_JOYSTICK_RIGHT = 6;
	public static final int IPT_JOYSTICK_UP = 7;
	public static final int IPT_JOYSTICK_DOWN = 8;
	public static final int IPT_BUTTON1 = 9;
	public static final int IPT_BUTTON2 = 10;
	public static final int IPT_BUTTON3 = 11;
	public static final int IPT_BUTTON4 = 12;
    public static final int IPT_BUTTON5 = 13;
    public static final int IPT_BUTTON6 = 14;
	public static final int IPT_TILT = 15;
	public static final int IPF_2WAY = 0; // not implemented
	public static final int IPF_4WAY = 0; // not implemented
	public static final int IPF_8WAY = 0; // not implemented
	public static final int IPF_COCKTAIL = 128;
	public static final int IPF_TOGGLE = 2048;
	public static final int IPF_PLAYER1 = 0;
	public static final int IPF_PLAYER2 = 128;
	public static final int IPF_PLAYER3 = 256; // not implemented
	public static final int IPF_PLAYER4 = 384; // not implemented

	// analog
	public static final int IPT_PADDLE			= 1;
	public static final int IPT_DIAL			= 2;
	public static final int IPF_REVERSE		= 256;
	public static final int IPT_AD_STICK_X 	= 3;
	public static final int IPT_AD_STICK_Y 	= 4;
	
	private int value = 0;

	private boolean analog = false;
	private boolean reverse = false;
	private int center = 0x7f;
	private int type = 0;
	private int[] bit = { 1, 2, 4, 8, 16, 32, 64, 128 };

	private int[] bitMask = new int[512];
	private int[] activityType = new int[512];

	private boolean[] impulse = { false, false, false, false, false, false, false, false };
	private int[] impulseActivityType = { 0, 0, 0, 0, 0, 0, 0, 0 };
	private int[] impulseInputType = { 0, 0, 0, 0, 0, 0, 0, 0 };
	private int[] impulseCounter = { -1, -1, -1, -1, -1, -1, -1, -1 };
	private int[] impulseDelay = { 0, 0, 0, 0, 0, 0, 0, 0 };

	/**
	 * Handle key press event.
	 * 
	 * @param keyCode
	 */
	public void keyPress(int keyCode) {

		switch (keyCode) {

			case KeyEvent.VK_5 :
				value =
					(activityType[IPT_COIN1] == IP_ACTIVE_HIGH)
						? value | (0xff & bitMask[IPT_COIN1])
						: value & ~bitMask[IPT_COIN1];
				break;

			case KeyEvent.VK_6 :
				value =
					(activityType[IPT_COIN2] == IP_ACTIVE_HIGH)
						? value | (0xff & bitMask[IPT_COIN1])
						: value & ~bitMask[IPT_COIN1];
				break;

			case KeyEvent.VK_1 :
				value =
					(activityType[IPT_START1] == IP_ACTIVE_HIGH)
						? value | (0xff & bitMask[IPT_START1])
						: value & ~bitMask[IPT_START1];
				break;

			case KeyEvent.VK_2 :
				value =
					(activityType[IPT_START2] == IP_ACTIVE_HIGH)
						? value | (0xff & bitMask[IPT_START2])
						: value & ~bitMask[IPT_START2];
				break;

			case KeyEvent.VK_LEFT :
				if (!analog) {
					value =
						(activityType[IPT_JOYSTICK_LEFT] == IP_ACTIVE_HIGH)
							? value | (0xff & bitMask[IPT_JOYSTICK_LEFT])
							: value & ~bitMask[IPT_JOYSTICK_LEFT];
				} else {
					switch (type) {
						case IPT_PADDLE :
						case IPT_AD_STICK_X :
							value = reverse ? 255 : 0;
					}
				}
				break;

			case KeyEvent.VK_D :
				if (!analog) {
					value =
						(activityType[IPT_JOYSTICK_LEFT | IPF_PLAYER2] == IP_ACTIVE_HIGH)
							? value | (0xff & bitMask[IPT_JOYSTICK_LEFT | IPF_PLAYER2])
							: value & ~bitMask[IPT_JOYSTICK_LEFT | IPF_PLAYER2];
				} else {
					switch (type) {
						case IPT_PADDLE :
							value = reverse ? 255 : 0;
							break;
					}
				}
				break;

			case KeyEvent.VK_RIGHT :
				if (!analog) {
					value =
						(activityType[IPT_JOYSTICK_RIGHT] == IP_ACTIVE_HIGH)
							? value | (0xff & bitMask[IPT_JOYSTICK_RIGHT])
							: value & ~bitMask[IPT_JOYSTICK_RIGHT];
				} else {
					switch (type) {
						case IPT_PADDLE :
						case IPT_AD_STICK_X :
							value = reverse ? 0 : 255;
					}
				}
				break;

			case KeyEvent.VK_G :
				if (!analog) {
					value =
						(activityType[IPT_JOYSTICK_RIGHT | IPF_PLAYER2] == IP_ACTIVE_HIGH)
							? value | (0xff & bitMask[IPT_JOYSTICK_RIGHT | IPF_PLAYER2])
							: value & ~bitMask[IPT_JOYSTICK_RIGHT | IPF_PLAYER2];
				} else {
					switch (type) {
						case IPT_PADDLE :
							value = reverse ? 0 : 255;
							break;
					}
				}
				break;

			case KeyEvent.VK_UP :
				if (!analog) {
					value =
						(activityType[IPT_JOYSTICK_UP] == IP_ACTIVE_HIGH)
							? value | (0xff & bitMask[IPT_JOYSTICK_UP])
							: value & ~bitMask[IPT_JOYSTICK_UP];
				} else {
					switch (type) {
						case IPT_PADDLE :
						case IPT_AD_STICK_Y :
							value = reverse ? 255 : 0;
					}

				}
				break;

			case KeyEvent.VK_R :
				value =
					(activityType[IPT_JOYSTICK_UP | IPF_PLAYER2] == IP_ACTIVE_HIGH)
						? value | (0xff & bitMask[IPT_JOYSTICK_UP | IPF_PLAYER2])
						: value & ~bitMask[IPT_JOYSTICK_UP | IPF_PLAYER2];
				break;

			case KeyEvent.VK_DOWN :
				if (!analog) {
					value =
						(activityType[IPT_JOYSTICK_DOWN] == IP_ACTIVE_HIGH)
							? value | (0xff & bitMask[IPT_JOYSTICK_DOWN])
							: value & ~bitMask[IPT_JOYSTICK_DOWN];
				} else {
					switch (type) {
						case IPT_PADDLE :
						case IPT_AD_STICK_Y :
							value = reverse ? 0 : 255;
					}

				}
				break;

			case KeyEvent.VK_F :
				value =
					(activityType[IPT_JOYSTICK_DOWN | IPF_PLAYER2] == IP_ACTIVE_HIGH)
						? value | (0xff & bitMask[IPT_JOYSTICK_DOWN | IPF_PLAYER2])
						: value & ~bitMask[IPT_JOYSTICK_DOWN | IPF_PLAYER2];
				break;

			case KeyEvent.VK_CONTROL :
				value =
					(activityType[IPT_BUTTON1] == IP_ACTIVE_HIGH)
						? value | (0xff & bitMask[IPT_BUTTON1])
						: value & ~bitMask[IPT_BUTTON1];
				break;

			case KeyEvent.VK_A :
				value =
					(activityType[IPT_BUTTON1 | IPF_PLAYER2] == IP_ACTIVE_HIGH)
						? value | (0xff & bitMask[IPT_BUTTON1 | IPF_PLAYER2])
						: value & ~bitMask[IPT_BUTTON1 | IPF_PLAYER2];
				break;

			case KeyEvent.VK_SPACE :
				value =
					(activityType[IPT_BUTTON2] == IP_ACTIVE_HIGH)
						? value | (0xff & bitMask[IPT_BUTTON2])
						: value & ~bitMask[IPT_BUTTON2];
				break;

			case KeyEvent.VK_S :
				value =
					(activityType[IPT_BUTTON2 | IPF_PLAYER2] == IP_ACTIVE_HIGH)
						? value | (0xff & bitMask[IPT_BUTTON2 | IPF_PLAYER2])
						: value & ~bitMask[IPT_BUTTON2 | IPF_PLAYER2];
				break;

			case KeyEvent.VK_Z :
				value =
					(activityType[IPT_BUTTON3] == IP_ACTIVE_HIGH)
						? value | (0xff & bitMask[IPT_BUTTON3])
						: value & ~bitMask[IPT_BUTTON3];
				break;

			case KeyEvent.VK_Q :
				value =
					(activityType[IPT_BUTTON3 | IPF_PLAYER2] == IP_ACTIVE_HIGH)
						? value | (0xff & bitMask[IPT_BUTTON3 | IPF_PLAYER2])
						: value & ~bitMask[IPT_BUTTON3 | IPF_PLAYER2];
				break;

			case KeyEvent.VK_X :
				value =
					(activityType[IPT_BUTTON4] == IP_ACTIVE_HIGH)
						? value | (0xff & bitMask[IPT_BUTTON4])
						: value & ~bitMask[IPT_BUTTON4];
				break;

			case KeyEvent.VK_W :
				value =
					(activityType[IPT_BUTTON4 | IPF_PLAYER2] == IP_ACTIVE_HIGH)
						? value | (0xff & bitMask[IPT_BUTTON4 | IPF_PLAYER2])
						: value & ~bitMask[IPT_BUTTON4 | IPF_PLAYER2];
				break;

            case KeyEvent.VK_C :
                value =
                    (activityType[IPT_BUTTON5] == IP_ACTIVE_HIGH)
                        ? value | (0xff & bitMask[IPT_BUTTON5])
                        : value & ~bitMask[IPT_BUTTON5];
                break;

            case KeyEvent.VK_V :
                value =
                    (activityType[IPT_BUTTON6] == IP_ACTIVE_HIGH)
                        ? value | (0xff & bitMask[IPT_BUTTON6])
                        : value & ~bitMask[IPT_BUTTON6];
                break;

        }

	}

	/**
	 * Handle key release event.
	 * 
	 * @param keyCode
	 */
	public void keyRelease(int keyCode) {

		switch (keyCode) {

			case KeyEvent.VK_5 :
				value =
					(activityType[IPT_COIN1] == IP_ACTIVE_LOW)
						? value | (0xff & bitMask[IPT_COIN1])
						: value & ~bitMask[IPT_COIN1];
				break;

			case KeyEvent.VK_6 :
				value =
					(activityType[IPT_COIN2] == IP_ACTIVE_LOW)
						? value | (0xff & bitMask[IPT_COIN1])
						: value & ~bitMask[IPT_COIN1];
				break;

			case KeyEvent.VK_1 :
				value =
					(activityType[IPT_START1] == IP_ACTIVE_LOW)
						? value | (0xff & bitMask[IPT_START1])
						: value & ~bitMask[IPT_START1];
				break;

			case KeyEvent.VK_2 :
				value =
					(activityType[IPT_START2] == IP_ACTIVE_LOW)
						? value | (0xff & bitMask[IPT_START2])
						: value & ~bitMask[IPT_START2];
				break;

			case KeyEvent.VK_LEFT :
				if (!analog) {
					value =
						(activityType[IPT_JOYSTICK_LEFT] == IP_ACTIVE_LOW)
							? value | (0xff & bitMask[IPT_JOYSTICK_LEFT])
							: value & ~bitMask[IPT_JOYSTICK_LEFT];
				} else {
					value = center;
				}
				break;

			case KeyEvent.VK_D :
				if (!analog) {
					value =
						(activityType[IPT_JOYSTICK_LEFT | IPF_PLAYER2] == IP_ACTIVE_LOW)
							? value | (0xff & bitMask[IPT_JOYSTICK_LEFT | IPF_PLAYER2])
							: value & ~bitMask[IPT_JOYSTICK_LEFT | IPF_PLAYER2];
				} else {
					switch (type) {
						case IPT_PADDLE :
							value = center;
							break;
					}
				}
				break;

			case KeyEvent.VK_RIGHT :
				if (!analog) {
					value =
						(activityType[IPT_JOYSTICK_RIGHT] == IP_ACTIVE_LOW)
							? value | (0xff & bitMask[IPT_JOYSTICK_RIGHT])
							: value & ~bitMask[IPT_JOYSTICK_RIGHT];
				} else {
					value = center;
				}
				break;

			case KeyEvent.VK_G :
				if (!analog) {
					value =
						(activityType[IPT_JOYSTICK_RIGHT | IPF_PLAYER2] == IP_ACTIVE_LOW)
							? value | (0xff & bitMask[IPT_JOYSTICK_RIGHT | IPF_PLAYER2])
							: value & ~bitMask[IPT_JOYSTICK_RIGHT | IPF_PLAYER2];
				} else {
					switch (type) {
						case IPT_PADDLE :
							value = center;
							break;
					}
				}
				break;

			case KeyEvent.VK_UP :
				if (!analog) {
					value =
						(activityType[IPT_JOYSTICK_UP] == IP_ACTIVE_LOW)
							? value | (0xff & bitMask[IPT_JOYSTICK_UP])
							: value & ~bitMask[IPT_JOYSTICK_UP];
				} else {
					value = center;
				}
				break;

			case KeyEvent.VK_R :
				value =
					(activityType[IPT_JOYSTICK_UP | IPF_PLAYER2] == IP_ACTIVE_LOW)
						? value | (0xff & bitMask[IPT_JOYSTICK_UP | IPF_PLAYER2])
						: value & ~bitMask[IPT_JOYSTICK_UP | IPF_PLAYER2];
				break;

			case KeyEvent.VK_DOWN :
				if (!analog) {
					value =
						(activityType[IPT_JOYSTICK_DOWN] == IP_ACTIVE_LOW)
							? value | (0xff & bitMask[IPT_JOYSTICK_DOWN])
							: value & ~bitMask[IPT_JOYSTICK_DOWN];
				} else {
					value = center;
				}
				break;

			case KeyEvent.VK_F :
				value =
					(activityType[IPT_JOYSTICK_DOWN | IPF_PLAYER2] == IP_ACTIVE_LOW)
						? value | (0xff & bitMask[IPT_JOYSTICK_DOWN | IPF_PLAYER2])
						: value & ~bitMask[IPT_JOYSTICK_DOWN | IPF_PLAYER2];
				break;

			case KeyEvent.VK_CONTROL :
				value =
					(activityType[IPT_BUTTON1] == IP_ACTIVE_LOW)
						? value | (0xff & bitMask[IPT_BUTTON1])
						: value & ~bitMask[IPT_BUTTON1];
				break;

			case KeyEvent.VK_A :
				value =
					(activityType[IPT_BUTTON1 | IPF_PLAYER2] == IP_ACTIVE_LOW)
						? value | (0xff & bitMask[IPT_BUTTON1 | IPF_PLAYER2])
						: value & ~bitMask[IPT_BUTTON1 | IPF_PLAYER2];
				break;

			case KeyEvent.VK_SPACE :
				value =
					(activityType[IPT_BUTTON2] == IP_ACTIVE_LOW)
						? value | (0xff & bitMask[IPT_BUTTON2])
						: value & ~bitMask[IPT_BUTTON2];
				break;

			case KeyEvent.VK_S :
				value =
					(activityType[IPT_BUTTON2 | IPF_PLAYER2] == IP_ACTIVE_LOW)
						? value | (0xff & bitMask[IPT_BUTTON2 | IPF_PLAYER2])
						: value & ~bitMask[IPT_BUTTON2 | IPF_PLAYER2];
				break;

			case KeyEvent.VK_Z :
				value =
					(activityType[IPT_BUTTON3] == IP_ACTIVE_LOW)
						? value | (0xff & bitMask[IPT_BUTTON3])
						: value & ~bitMask[IPT_BUTTON3];
				break;

			case KeyEvent.VK_Q :
				value =
					(activityType[IPT_BUTTON3 | IPF_PLAYER2] == IP_ACTIVE_LOW)
						? value | (0xff & bitMask[IPT_BUTTON3 | IPF_PLAYER2])
						: value & ~bitMask[IPT_BUTTON3 | IPF_PLAYER2];
				break;

			case KeyEvent.VK_X :
				value =
					(activityType[IPT_BUTTON4] == IP_ACTIVE_LOW)
						? value | (0xff & bitMask[IPT_BUTTON4])
						: value & ~bitMask[IPT_BUTTON4];
				break;

			case KeyEvent.VK_W :
				value =
					(activityType[IPT_BUTTON4 | IPF_PLAYER2] == IP_ACTIVE_LOW)
						? value | (0xff & bitMask[IPT_BUTTON4 | IPF_PLAYER2])
						: value & ~bitMask[IPT_BUTTON4 | IPF_PLAYER2];
				break;
                
            case KeyEvent.VK_C :
                value =
                    (activityType[IPT_BUTTON5] == IP_ACTIVE_LOW)
                        ? value | (0xff & bitMask[IPT_BUTTON5])
                        : value & ~bitMask[IPT_BUTTON5];
                break;
                
            case KeyEvent.VK_V :
                value =
                    (activityType[IPT_BUTTON4] == IP_ACTIVE_LOW)
                        ? value | (0xff & bitMask[IPT_BUTTON6])
                        : value & ~bitMask[IPT_BUTTON6];
                break;
		}
	}

	/**
	 * Get the port value.
	 * 
	 * @see jef.map.ReadHandler#read(int)
	 */
	public int read(int port) {
		if (!analog) {
			return value;
		} else {
		//	value = (value + Mouse.getDX());
			
		//	return (value / 2) & 0xff;
			return 0;
		}
	}

	/**
	 * Write a value to the port.
	 * 
	 * @param data
	 */
	public void write(int data) {
		this.value = data;
	}

	/**
	 * Map a bit.
	 * 
	 * @param bitMask
	 * @param activityType
	 * @param inputType
	 */
	public void setBit(int bitMask, int activityType, int inputType) {
		this.bitMask[inputType] = bitMask;
		this.activityType[inputType] = activityType;
		value = (activityType == IP_ACTIVE_LOW) ? value | (0xff & bitMask) : value & ~bitMask;
	}

	/**
	 * Map a bit to an impulse event.
	 * 
	 * @param bitMask
	 * @param activityType
	 * @param inputType
	 * @param frames
	 */
	public void setBitImpulse(int bitMask, int activityType, int inputType, int frames) {
		this.bitMask[inputType] = bitMask;
		this.activityType[inputType] = activityType;
		this.impulseActivityType[bitSet(bitMask)] = activityType;
		this.impulse[bitSet(bitMask)] = true;
		this.impulseInputType[bitSet(bitMask)] = inputType;
		this.impulseDelay[bitSet(bitMask)] = frames;
		value = (activityType == IP_ACTIVE_LOW) ? value | (0xff & bitMask) : value & ~bitMask;
	}

	/**
	 * Update impulse events. Call this every frame.
	 */
	public void update() {
		for (int i = 0; i < 8; i++) {
			if (impulse[i]) {
				if (impulseCounter[i] > 0)
					impulseCounter[i]--;
				else {
					int bitMask = 1 << i;
					value =
						(impulseActivityType[i] == IP_ACTIVE_LOW)
							? value & ~bitMask
							: value | (0xff & bitMask);
				}
			}
		}
	}

	/**
	 * Map a name and a default setting to a dip switch.
	 * 
	 * @param bitMask
	 * @param defSetting
	 * @param name
	 */
	public void setDipName(int bitMask, int defSetting, String name) {
		value = (value & ~bitMask) | defSetting;
		// TO DO: the rest of this
	}

	/**
	 * Set a dipswitch to a specified setting.
	 * 
	 * @param setting
	 * @param name
	 */
	public void setDipSetting(int setting, String name) {
		// TO DO
	}

	/**
	 * Set a service switch.
	 * 
	 * @param bitMask
	 * @param activityType
	 */
	public void setService(int bitMask, int activityType) {
		value = (activityType == IP_ACTIVE_LOW) ? value | (0xff & bitMask) : value & ~bitMask;
	}

	/**
	 * Set analog input.
	 * 
	 * @param bitMask
	 * @param center
	 * @param type
	 * @param a
	 * @param b
	 * @param c
	 * @param d
	 */
	public void setAnalog(int bitMask, int center, int type, int a, int b, int c, int d) {
		this.analog = true;
		this.type = type & 31;
		this.reverse = (type & IPF_REVERSE) != 0;
		this.center = this.value = center;
		// TO DO
	}

	/**
	 * Get the bitnumber that is set.
	 * 
	 * @param value
	 * @return int
	 */
	private int bitSet(int value) {
		for (int i = 0; i < 8; i++) {
			if (bit[i] == value)
				return i;
		}
		return 0;
	}
}