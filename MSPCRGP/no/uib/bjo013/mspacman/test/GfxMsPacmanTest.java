package no.uib.bjo013.mspacman.test;

import java.awt.BorderLayout;
import java.awt.Point;
import java.util.List;

import javax.swing.JFrame;

import jef.video.BitMap;
import no.uib.bjo013.mspacman.Game;
import no.uib.bjo013.mspacman.GfxMsPacman;

public class GfxMsPacmanTest {

	public static void main(String[] args) throws Exception {
		BitMap bm;
		Game g = new Game(true);
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
					Point target = g.getMap().getPills().get(0);
					g.setTarget(target);
				} catch (Exception e) {
				}
				bm = g.update();
				List<Point> path = g.getPath();
				for (Point p : path) {
					bm.setPixelFast(p.x, p.y, 65280);
				}
				c.setBitmap(bm);
				synchronized (c) {
					c.notify();
				}
				synchronized (c) {
					c.notify();
				}
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
