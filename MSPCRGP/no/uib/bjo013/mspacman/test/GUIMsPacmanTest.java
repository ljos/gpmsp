package no.uib.bjo013.mspacman.test;

import java.awt.BorderLayout;
import java.util.concurrent.CountDownLatch;

import javax.swing.JFrame;

import no.uib.bjo013.mspacman.GUIMsPacman;

public class GUIMsPacmanTest {

	public static void main(String[] args) throws Exception {
		CountDownLatch[] signal = { new CountDownLatch(1),
				new CountDownLatch(1), new CountDownLatch(1) };
		GUIMsPacman c = new GUIMsPacman(signal, new Object(), Thread.currentThread());
		c.setSize(224, 288 + 22); // I think the + 22 is because of the top bar.

		JFrame app = new JFrame("Ms. Pacman");
		app.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		app.setSize(224, 288 + 22);
		app.setLocation(240, 0);
		app.getContentPane().add(c, BorderLayout.CENTER);
		app.setVisible(true);
		Thread t = new Thread(c);
		t.start();

		System.out.println("before await");
		signal[0].await();
		System.out.println("before await");
		signal[1].await();
		System.out.println("before await");

		signal[2].await();

		synchronized (c) {
			c.stopMSP();
		}
		t.join();
		System.out.println(t.isAlive()); // */
	}
}
