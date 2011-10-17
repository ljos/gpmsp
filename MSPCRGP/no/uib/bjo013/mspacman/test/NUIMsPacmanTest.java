package no.uib.bjo013.mspacman.test;

import java.awt.event.KeyEvent;

import no.uib.bjo013.mspacman.NUIMsPacman;


public class NUIMsPacmanTest {
	public static void main(String args[]) throws InterruptedException {
		NUIMsPacman g = new NUIMsPacman();
		Thread t = new Thread(g);
		t.start();
		Thread.sleep(6000);
		g.keyPressed(KeyEvent.VK_5);
		System.out.println("####5#####");
		Thread.sleep(500);
		g.keyReleased(KeyEvent.VK_5);
		System.out.println("@@@@release 5@@@@");
		Thread.sleep(500);
		System.out.println("@@@@@1@@@@@");
		g.keyPressed(KeyEvent.VK_1);
		Thread.sleep(100);
		g.keyReleased(KeyEvent.VK_1);
		System.out.println("@@@@@release 1@@@@");
		Thread.sleep(200);
		System.out.println("@@@@@LEFT@@@@@@@");
		g.keyPressed(KeyEvent.VK_LEFT);
		for (int i = 0; i < 30; ++i) {
			Thread.sleep(1000);
			System.out.println(i);
		}
		System.out.println(g.getScore() + " " + t.isAlive());
		g.stop(true);
		Thread.sleep(1000);
		System.out.println(t.isAlive());
	}
}
