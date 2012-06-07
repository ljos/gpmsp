package no.uib.bjo013.mspacman.map;

public enum GameEntity {
	MSPACMAN (16776960), 
	BLINKY (16711680), 
	PINKY (16759006), 
	INKY (65502), 
	SUE (16758855),
	BLUE (2171358), 
	SUPER_PILL (14606046),
	PILL (14606046), 
	NONE (0);
	
	private int colour;
	GameEntity(int c) {
		this.colour = c;
	}
	
	public int colour() {
		return colour;
	}
}



