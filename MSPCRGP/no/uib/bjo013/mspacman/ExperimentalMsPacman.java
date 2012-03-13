package no.uib.bjo013.mspacman;

import java.awt.Point;
import java.awt.event.KeyEvent;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;

import jef.video.GfxProducer;

public class ExperimentalMsPacman extends GfxProducer implements MsPacman {
	private static final long serialVersionUID = 8867847222040282809L;

	public static ExperimentalMsPacman main;

	/** booleans **/
	boolean stop = false;

	private Game game = new Game(true);

	private final CountDownLatch[] signal;

	public ExperimentalMsPacman(CountDownLatch[] signal) {
		this.signal = signal;
	}

	@Override
	public void main(int w, int h) {
		update(game.initialize());
		update(game.start());
		int latch = 0;
		signal[latch].countDown();
		++latch;

		while (shouldContinue()) { // running game
			Iterator<Point> nodes = game.getMap().getSuperPills().iterator();
			if (!nodes.hasNext()) {
				nodes = game.getMap().getPills().iterator();
			}
			game.setTarget(nodes.next());
			update(game.update());

		}
		if (this.isGameOver() && shouldContinue()) {
			game.start();
			signal[latch].countDown();
			++latch;
		}
		for (CountDownLatch l : signal) {
			l.countDown();
		}
	}

	@Override
	public void keyPressed(int code) {
		game.keyPressed(code);
	}

	@Override
	public void keyReleased(int code) {
		game.keyReleased(code);
	}

	@Override
	public long getScore() {
		return game.getScore();
	}

	@Override
	public boolean isGameOver() {
		return game.isGameOver();
	}

	@Override
	public int getPixel(int x, int y) {
		return game.getPixel(x, y);
	}

	@Override
	protected void processKeyEvent(KeyEvent e) {
		int code = e.getKeyCode();
		switch (e.getID()) {
		case KeyEvent.KEY_PRESSED:
			game.keyPressed(code);
			break;
		case KeyEvent.KEY_RELEASED:
			game.keyReleased(code);
			break;
		}
	}

	@Override
	public synchronized void stopMSP() {
		this.stop = true;
		game.setThrottle(true);
	}

	@Override
	public synchronized boolean shouldContinue() {
		return !stop;
	}

	@Override
	public int[] getPixels() {
		return game.getPixels();
	}
}
