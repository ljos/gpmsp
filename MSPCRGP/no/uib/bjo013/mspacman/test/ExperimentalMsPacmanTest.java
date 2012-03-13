package no.uib.bjo013.mspacman.test;

import java.awt.BorderLayout;
import java.util.concurrent.CountDownLatch;

import javax.swing.JFrame;

import no.uib.bjo013.mspacman.ExperimentalMsPacman;

public class ExperimentalMsPacmanTest {

	public static void main(String[] args) throws Exception {
		CountDownLatch[] signal = { 
				new CountDownLatch(1),
				new CountDownLatch(1),
				new CountDownLatch(1),
				new CountDownLatch(1),
				new CountDownLatch(1),
				new CountDownLatch(1),
				new CountDownLatch(1),
				new CountDownLatch(1),
				new CountDownLatch(1) };
		ExperimentalMsPacman c = new ExperimentalMsPacman(signal);
		c.setSize(224, 288 + 22); // I think the + 22 is because of the top bar.

		JFrame app = new JFrame("Ms. Pacman");
		app.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		app.setSize(224, 288 + 22);
		app.setLocation(240, 0);
		app.getContentPane().add(c, BorderLayout.CENTER);
		app.setVisible(true);
		Thread t = new Thread(c);
		t.start();

		for(CountDownLatch s : signal) {
			s.await();
		}

		synchronized (c) {
			c.stopMSP();
		}
		t.join();
		System.out.println(t.isAlive());
		app.dispose();// */
	}
}
