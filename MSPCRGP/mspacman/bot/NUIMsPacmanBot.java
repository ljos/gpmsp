package mspacman.bot;

import mspacman.NUIMsPacman;

public class NUIMsPacmanBot implements IMsPacmanBot {
	
	private NUIMsPacman game;
	private Thread gamethread;
	private String id;
	private int tries;
	
	public NUIMsPacmanBot(String id) {
		this.id = id;
	}

	@Override
	public void run() {
		game = new NUIMsPacman();
		gamethread = new Thread(game);
		gamethread.start();
		
		while(gamethread.isAlive()) {
			if(game.isGameOver()) {
				tries--;
				if(tries == 0) {
					this.stop(true);
				}
			}
		
			this.logic();
		}
	}
	
	private void logic() {
		//BOTLOGIC HERE
	}

	@Override
	public long getScore() {
		return game.getScore();
	}

	@Override
	public void setTries(int tries) {
		this.tries = tries;
	}

	@Override
	public void stop(boolean stop) {
		game.stop(true);
	}


	@Override
	public String getID() {
		return id;
	}

}
