package mspacman.bot;

import mspacmanr.NUIMsPacman;

public class NUIMsPacmanBot extends AbstrMsPacmanBot {
	
	public NUIMsPacmanBot(String code, int tries) {
		super(code, tries);
	}

	@Override
	public void run() {
		game = new NUIMsPacman();
		gamethread = new Thread(game);
		gamethread.start();
		
		this.logic();
	}
}
