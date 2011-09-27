package mspacman.bot;

import java.awt.BorderLayout;

import javax.swing.JFrame;

import mspacman.GUIMsPacman;

public class GUIMsPacmanBot extends AbstrMsPacmanBot {
	
	public GUIMsPacmanBot(String code, int tries) {
		super(code, tries);
	}
	
	@Override
	public void run() {
		game = new GUIMsPacman();
		((GUIMsPacman)game).setSize(224, 288 + 22);
		
		JFrame app = new JFrame("Ms. Pacman");
		app.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		app.setSize(224, 288 + 22);
		app.setLocation(300, 0);
		app.getContentPane().add((GUIMsPacman)game, BorderLayout.CENTER);
		app.setVisible(true);
		
		this.logic();
	}
}
