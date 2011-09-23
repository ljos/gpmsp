package mspacman.bot;

import java.awt.BorderLayout;

import javax.swing.JFrame;

import mspacman.GUIMsPacman;

public class GUIMsPacmanBot implements IMsPacmanBot {

	private GUIMsPacman game;
	private Thread gamethread;
	private int tries;
	private String id;
	
	public GUIMsPacmanBot(String id) {
		this.id = id;
	}
	
	@Override
	public void run() {
		game = new GUIMsPacman();
		game.setSize(224, 288 + 22);
		
		JFrame app = new JFrame("Ms. Pacman");
		app.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		app.setSize(224, 288 + 22);
		app.setLocation(300, 0);
		app.getContentPane().add(game, BorderLayout.CENTER);
		app.setVisible(true);
		
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
