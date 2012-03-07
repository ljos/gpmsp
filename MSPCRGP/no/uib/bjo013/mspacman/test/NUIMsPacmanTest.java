package no.uib.bjo013.mspacman.test;
import java.util.concurrent.CountDownLatch;

import no.uib.bjo013.mspacman.NUIMsPacman;

public class NUIMsPacmanTest {
	public static void main(String args[]) throws InterruptedException {
		/*CountDownLatch[] signal = {new CountDownLatch(1), new CountDownLatch(1), new CountDownLatch(1)};
		NUIMsPacman g = new NUIMsPacman(signal, new Object());
		Thread t = new Thread(g);
		t.start();
		System.out.println("before await");
		signal[0].await();
		System.out.println("before await");
		signal[1].await();
		System.out.println("before await");
		
		signal[2].await();
		
		
		
		synchronized(g) {
			g.stopMSP();
		}
		Thread.sleep(1000);
		System.out.println(t.isAlive());*/
	}
}
