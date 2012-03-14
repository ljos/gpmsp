package no.uib.bjo013.mspacman.map;

import java.awt.Point;

public enum GameEntity {
	MSPACMAN (16776960), 
	BLINKY (16711680), 
	PINKY (16759006), 
	INKY (65502), 
	SUE (16758855), 
	PILL (14606046), 
	SUPER_PILL (14606046),
	BLUE (2171358), 
	NONE (0);
	
	private int colour;
	GameEntity(int c) {
		this.colour = c;
	}
	
	public int colour() {
		return colour;
	}
}



