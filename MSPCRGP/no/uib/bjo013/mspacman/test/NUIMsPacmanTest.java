package no.uib.bjo013.mspacman.test;

import java.awt.event.KeyEvent;
import java.util.concurrent.CountDownLatch;

import no.uib.bjo013.mspacman.NUIMsPacman;


public class NUIMsPacmanTest {
	public static void main(String args[]) throws InterruptedException {
		CountDownLatch[] signal = {new CountDownLatch(1), new CountDownLatch(1), new CountDownLatch(1)};
		NUIMsPacman g = new NUIMsPacman(signal);
		Thread t = new Thread(g);
		t.start();
		signal[0].await();
		signal[1].await();
		
		signal[2].await();
		
		
		
		synchronized(g) {
			g.stopMSP();
		}
		Thread.sleep(1000);
		System.out.println(t.isAlive());
	}
}
