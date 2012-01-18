package no.uib.bjo013.mspacman;

public interface MsPacman extends Runnable {
	public int[] getPixels();
	
	public int getPixel(int x, int y);
	
	public void keyPressed(int keyCode);
	
	public void keyReleased(int keyCode);
	
	public long getScore();
	
	public void stop(boolean stop);
	
	public boolean isGameOver();
	
	public boolean checkForGhostRight(int x, int y);
	
	public boolean checkForGhostLeft(int x, int y);
	
	public boolean checkForWallY(int x, int y);
	
	public boolean checkForGhostUp(int x, int y);
	
	public boolean checkForGhostDown(int x, int y);
	
	public boolean containsGhost(int ghost, int x, int y);
	
	public int[] getMsPacman();
	
	public int[] getGhost(int ghost);
	
	public int relativeDistance(int entity, int item);

	boolean checkForWallX(int x, int y);
	
}
