package mspacman;

public interface MsPacman {
	public int[] getPixels();
	
	public void keyPressed(int keyCode);
	
	public void keyReleased(int keyCode);
	
	public long getScore();
	
	public void stop(boolean stop);
	
	public boolean isGameOver();
}
