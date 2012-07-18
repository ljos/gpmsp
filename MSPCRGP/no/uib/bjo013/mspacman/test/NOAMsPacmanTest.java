package no.uib.bjo013.mspacman.test;

import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.event.KeyEvent;

import javax.swing.JFrame;

import jef.video.BitMap;
import no.uib.bjo013.mspacman.NOAGame;
import no.uib.bjo013.mspacman.GfxMsPacman;

public class NOAMsPacmanTest {

	public static void main(String[] args) throws Exception {
		BitMap bm;
		NOAGame g = new NOAGame(false, 30000);
		bm = g.initialize();
		GfxMsPacman c = new GfxMsPacman(bm);
		c.setSize(224, 288 + 22); // I think the + 22 is because of the top bar.

		JFrame app = new JFrame("Ms. Pacman");
		app.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		app.setSize(224, 288 + 22);
		app.setLocation(240, 0);
		app.getContentPane().add(c, BorderLayout.CENTER);
		app.setVisible(true);
		Thread t = new Thread(c);
		t.start();
		for (int i = 0; i < 10; ++i) {
			bm = g.start();
			c.setBitmap(bm);
			while (!g.isGameOver()) {
				synchronized (c) {
					c.notify();
				}
				
				g.setDirection(g.getMap().directionOf(5));
				
				bm = g.update();
				c.setBitmap(bm);
			}
		}
		c.stop();
		synchronized (c) {
			c.notify();
		}
		t.join();
		app.dispose();
	}
}
