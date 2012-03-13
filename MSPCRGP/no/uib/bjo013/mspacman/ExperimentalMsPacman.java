package no.uib.bjo013.mspacman;

import java.awt.Point;
import java.awt.event.KeyEvent;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.CountDownLatch;

import jef.video.BitMap;
import jef.video.GfxProducer;
import no.uib.bjo013.mspacman.map.GameMap;

public class ExperimentalMsPacman extends GfxProducer implements MsPacman {
	private static final long serialVersionUID = 8867847222040282809L;

	public static ExperimentalMsPacman main;

	/** booleans **/
	boolean stop = false;
	
	private Game g = new Game(true);
	private GameMap gm;

	private final CountDownLatch[] signal;
	private final Object lock;
	private Thread parent;

	public ExperimentalMsPacman(CountDownLatch[] signal, Object lock, Thread parent) {
		this.signal = signal;
		this.lock = lock;
		this.parent = parent;
	}

	@Override
	public void main(int w, int h) {
		update(g.initialize());
		BitMap bm = g.waitForReadyMessageAppear();
		update(bm);
		gm = new GameMap(bm);
		update(g.waitForReadyMessageDissapear());
		int latch = 0;
		signal[latch].countDown();
		++latch;

		while (shouldContinue()) { // running game
			synchronized (lock) {
			//	try {
			//		lock.wait();
			//	} catch (InterruptedException e) {
			//		e.printStackTrace();
			//	}
				bm = g.update();
				gm.update(bm);
				try {
					Iterator<Point> pi = gm.getSuperPills().iterator();
					//pi.next();
					Point blinky = pi.next();
				for(Point p : gm.calculatePath(blinky)) {
					bm.setPixelFast(p.x, p.y, 65280);
				}
				} catch (NoSuchElementException e) {}
				update(bm);
					
				
			//	while (!parent.getState().equals(Thread.State.WAITING));
			//	lock.notify();
			}
			if (this.isGameOver() && shouldContinue()) {
				update(g.initialize());
				bm = g.waitForReadyMessageAppear();
				update(bm);
				gm = new GameMap(bm);
				update(g.waitForReadyMessageDissapear());
				signal[latch].countDown();
				++latch;
			}
		}
		for (CountDownLatch l : signal) {
			l.countDown();
		}
	}

	@Override
	public void keyPressed(int code) {
		g.keyPressed(code);
	}

	@Override
	public void keyReleased(int code) {
		g.keyReleased(code);
	}

	@Override
	public long getScore() {
		return g.getScore();
	}

	@Override
	public boolean isGameOver() {
		return g.isGameOver();
	}
	
	@Override
	public int getPixel(int x, int y) {
		return g.getPixel(x, y);
	}
	

	@Override
	protected void processKeyEvent(KeyEvent e) {
		int code = e.getKeyCode();
		switch (e.getID()) {
		case KeyEvent.KEY_PRESSED:
			g.keyPressed(code);
			break;
		case KeyEvent.KEY_RELEASED:
			g.keyReleased(code);
			break;
		}
	}

	@Override
	public synchronized void stopMSP() {
		this.stop = true;
		g.setThrottle(true);
	}

	@Override
	public synchronized boolean shouldContinue() {
		return !stop;
	}

	@Override
	public int[] getPixels() {
		return g.getPixels();
	}
}
