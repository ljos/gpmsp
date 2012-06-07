package no.uib.bjo013.mspacman.test;

import java.awt.BorderLayout;

import javax.swing.JFrame;

import jef.video.BitMap;
import no.uib.bjo013.mspacman.GfxMsPacman;
import no.uib.bjo013.mspacman.NewGame;
import no.uib.bjo013.mspacman.map.Node;

public class NewGfxMsPacmanTest {

	public static void main(String[] args) throws Exception {
		Thread.sleep(5000);
		BitMap bm;
		NewGame g = new NewGame(false);
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
				try {
					for(Node p : g.getMap().getGhosts()) {
						if (p != null) {
							g.adjustCircle(p, 20, Double.MAX_VALUE);	
						}
					}
				} catch (Exception e) {
				}
				
				bm = g.update();
				for(Node q : g.getPath()) {
					bm.setPixel(q.p.x, q.p.y, 65280);
				}
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
