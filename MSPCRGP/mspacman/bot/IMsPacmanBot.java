package mspacman.bot;

public interface IMsPacmanBot extends Runnable {
	public long getScore();
	public void setTries(int tries);
	public void stop(boolean stop);
	
	public String getID();
}
