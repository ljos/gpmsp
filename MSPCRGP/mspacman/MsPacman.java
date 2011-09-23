package mspacman;

public interface MsPacman extends Runnable {
	public int[] getPixels();
	
	public void keyPressed(int keyCode);
	
	public void keyReleased(int keyCode);
	
	public long getScore();
	
	public void stop(boolean stop);
	
	public boolean isGameOver();
}
