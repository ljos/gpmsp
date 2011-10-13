package mspacman.test;

import java.awt.BorderLayout;

import javax.swing.JFrame;

import mspacmanr.GUIMsPacman;

public class GUIMsPacmanTest {

	public static void main(String[] args) throws Exception {

		GUIMsPacman[] cs = new GUIMsPacman[5];
		for (int i = 0; i < cs.length; ++i) {
			GUIMsPacman c = new GUIMsPacman();
			c.setSize(224, 288 + 22); // I think the + 22 is because of the top bar.
			cs[i] = c;
		}

		for (int i = 0; i < 5; ++i) {
			JFrame app = new JFrame("Ms. Pacman " + i);
			app.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			app.setSize(224, 288 + 22);
			app.setLocation(300 * i, 0);
			app.getContentPane().add(cs[i], BorderLayout.CENTER);
			app.setVisible(true);
		}

		for (GUIMsPacman c : cs) {
			new Thread(c).start();
		}
	}
}
