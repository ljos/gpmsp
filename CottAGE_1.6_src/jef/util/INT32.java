/*
 * Created on 14-aug-2005
 */
package jef.util;

/**
 * @author Erik Duijs
 * 
 *         Utility class for porting C INT32 types which are used by reference.
 */
public class INT32 {
	public int value;

	/**
	 * Limit the value by min and max bounds.
	 * 
	 * @param min
	 * @param max
	 */
	public void limit(int min, int max) {
		if (value > max)
			value = max;
		else if (value < min)
			value = min;
	}

}
