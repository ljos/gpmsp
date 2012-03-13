package no.uib.bjo013.mspacman;

import java.awt.event.KeyEvent;
import java.util.concurrent.CountDownLatch;

import jef.video.GfxProducer;

public class GUIMsPacman extends GfxProducer implements MsPacman {
	private static final long serialVersionUID = 8867847222040282809L;

	/** TXT stuff **/
	public static GUIMsPacman main;

	private final CountDownLatch[] signal;
	private Object lock;
	private Thread parent;
	
	private boolean stop = false;
	
	private Game g = new Game(false);

	public GUIMsPacman(CountDownLatch[] signal, Object lock, Thread parent) {
		this.signal = signal;
		this.lock = lock;
		this.parent = parent;
	}

	@Override
	public void main(int w, int h) {
		update(g.initialize());
		update(g.start());
		
		int latch = 0;
		signal[latch].countDown();
		++latch;

		while (shouldContinue()) { // running game
			synchronized (lock) {
				try {
					lock.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				update(g.update());
				while(!parent.getState().equals(Thread.State.WAITING));
				lock.notify();
			}
			if (g.isGameOver() && shouldContinue()) {
				update(g.start());
				signal[latch].countDown();
				++latch;
			}
		}
		for (CountDownLatch l : signal) {
			l.countDown();
		}
	}

	@Override
	protected void processKeyEvent(KeyEvent e) {
		switch (e.getID()) {
		case KeyEvent.KEY_PRESSED:
			g.keyPressed(e.getKeyCode());
			break;
		case KeyEvent.KEY_RELEASED:
			g.keyReleased(e.getKeyCode());
			break;
		}
	}

	@Override
	public int[] getPixels() {
		return g.getPixels();
	}

	@Override
	public int getPixel(int x, int y) {
		return g.getPixel(x, y);
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
		// score = 0x43f7, highscore = 0x43ed
	}

	@Override
	public synchronized void stopMSP() {
		this.stop = true;
		g.setThrottle(false);	
	}
	
	@Override
	public synchronized boolean shouldContinue() {
		return !stop;
	}

	@Override
	public boolean isGameOver() {
		return g.isGameOver();
	}
}
