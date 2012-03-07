package no.uib.bjo013.mspacman;

import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.net.URL;
import java.util.concurrent.CountDownLatch;

import jef.machine.Machine;
import jef.util.Throttle;
import jef.video.GfxProducer;
import cottage.CottageDriver;
import cottage.machine.Pacman;

public class GUIMsPacman extends GfxProducer implements MsPacman {
	private static final long serialVersionUID = 8867847222040282809L;

	/** pixel buffer **/
	public static int pixel[];

	/** TXT stuff **/
	public static GUIMsPacman main;

	/** booleans **/
	boolean pause = false;
	boolean stop = false;

	/** reference to the driver **/
	private Pacman m;
	Throttle t;

	private final CountDownLatch[] signal;
	private final Object lock;
	private Thread parent;

	public GUIMsPacman(CountDownLatch[] signal, Object lock, Thread parent) {
		this.signal = signal;
		this.lock = lock;
		this.parent = parent;
	}

	@Override
	public void main(int w, int h) {
		String driver = "mspacman";

		URL base_URL = null;
		try {
			base_URL = new URL(String.format("file://localhost/%s/.mspacman/",
					System.getProperty("user.home")));
		} catch (Exception e) {
		}

		main = this;

		CottageDriver d = new CottageDriver();

		/* prepare TXT stuff */
		jef.video.Console.init(w, h, this);
		pixel = new int[w * h];
		update(pixel);

		m = (Pacman) d.getMachine(base_URL, driver);

		jef.video.Console.init(w, h, this);

		pixel = new int[w * h];
		update(pixel);

		pixel = new int[m.refresh(true).getPixels().length];

		pixel = m.refresh(true).getPixels();

		t = new Throttle(m.getProperty(Machine.FPS));
		t.enable(false);

		int i = 3;
		while (i > 0) { // finding if the game is at start screen.
			update(m.refresh(true));
			t.throttle();
			if (((cottage.machine.Pacman) m).md.getREGION_CPU()[0x43F8] == 0) {
				--i;
			} else if (i < 3) {
				++i;
			}
		}

		Thread tj = new Thread(new SendKeys()); // sending keys for starting the
												// game
		tj.start();

		for (long j = 0;; ++j) { // waiting for ready message to appear
			update(m.refresh(true));
			t.throttle();
			if (((cottage.machine.Pacman) m).md.getREGION_CPU()[0x4252] == 82) {
				break;
			}
			if (j % 30 == 0) {
				new Thread(new SendKeys()).start();
			}
		}

		for (;;) { // waiting for ready message to disappear
			update(m.refresh(true));
			t.throttle();
			if (((cottage.machine.Pacman) m).md.getREGION_CPU()[0x4252] == 64) {
				break;
			}
		}

		try {
			tj.join();
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}

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
				for (int j = 0; j < 5; j++) {
					update(m.refresh(true));
					t.throttle();
				}
				while(!parent.getState().equals(Thread.State.WAITING));
				lock.notify();
			}
			t.throttle();
			if (this.isGameOver() && shouldContinue()) {
				for (;;) { // finding if the game is past ended screen
					update(m.refresh(true));
					t.throttle();
					if (((cottage.machine.Pacman) m).md.getREGION_CPU()[0x4252] != 77) {
						break;
					}
				}
				Thread th = new Thread(new SendKeys());
				th.start();

				for (long j = 0;; ++j) { // waiting for ready message to appear
					update(m.refresh(true));
					t.throttle();
					if (((cottage.machine.Pacman) m).md.getREGION_CPU()[0x4252] == 82) {
						break;
					}
					if (j % 50 == 0) {
						new Thread(new SendKeys()).start();
					}
				}

				for (;;) { // waiting for ready message to disappear
					update(m.refresh(true));
					t.throttle();
					if (((cottage.machine.Pacman) m).md.getREGION_CPU()[0x4252] == 64) {
						break;
					}
				}

				try {
					th.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
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
		switch (code) {
		case KeyEvent.VK_P:
			pause = !pause;
			break;
		case KeyEvent.VK_UP:
			m.writeInput(254);
			break;
		case KeyEvent.VK_LEFT:
			m.writeInput(253);
			break;
		case KeyEvent.VK_RIGHT:
			m.writeInput(251);
			break;
		case KeyEvent.VK_DOWN:
			m.writeInput(247);
			break;
		default:
			m.keyPress(code);
			break;
		}
	}

	@Override
	public void keyReleased(int code) {
		switch (code) {
		case KeyEvent.VK_UP:
		case KeyEvent.VK_LEFT:
		case KeyEvent.VK_RIGHT:
		case KeyEvent.VK_DOWN:
			// m.writeInput(255);
			break;
		default:
			m.keyRelease(code);
			break;
		}
	}

	public long getScore() {
		return getScore(0x43f7, ((cottage.machine.Pacman) m).md.getREGION_CPU());
	}

	private long getScore(int offset, int[] mem) {
		final int ZERO_CHAR = 0x00;
		final int BLANK_CHAR = 0x40;

		long score = 0;

		// calculate the score
		for (int i = 0; i < 7; i++) {
			int c = mem[offset + i];
			if (c == 0x00 || c == BLANK_CHAR) {
				c = ZERO_CHAR;
			}
			c -= ZERO_CHAR;
			score += (c * Math.pow(10, i));
		}

		return (score > 9999999) ? 0 : score;
	}

	@Override
	public boolean isGameOver() {
		return ((cottage.machine.Pacman) m).md.getREGION_CPU()[0x403B] == 67;
	}

	@Override
	public void postPaint(Graphics g) {
		if (pause) {
		}
	}

	public int[] getEntity(int colour) {
		if (colour == 16776960) {
			return getMsPacman();
		} else {
			int[] ghosts = { 16711680, 16759006, 65502, 16758855 };
			for (int ghost : ghosts) {
				if (colour == ghost) {
					return getGhost(ghost);
				}
			}
		}
		return new int[] { -1, -1 };
	}

	@Override
	public int[] getPixels() {
		return pixel;
	}

	@Override
	public int getPixel(int x, int y) {
		return (x >= 0 && x < 224 && y >= 0 && y < 288) ? pixel[x + y * 224]
				: -1;
	}

	@Override
	public boolean checkForWallX(int x, int y) {
		int[] walls = { 4700382, 2171358, 65280, 4700311, 16758935, 14606046 };
		for (int wall : walls) {
			if (getPixel(x, y + 10) == wall) {
				return true;
			}
		}
		return false;
	}

	public boolean checkForEntity(int entityatxy, int dir, int x, int y) {
		if (entityatxy != 16776960) { // ghosts
			switch (dir) {
			case 0: // left
				for (int i = x; i > 0; --i) {
					if (checkForWallX(i, y)) {
						break;
					}
					if (containsMsPacman(i, y)) {
						return true;
					}
				}
				break;
			case 1: // up
				for (int i = y; i > 0; --i) {
					if (checkForWallY(x, i)) {
						break;
					}
					if (containsMsPacman(x, i)) {
						return true;
					}
				}
				break;
			case 2: // right
				for (int i = x; i < 224; ++i) {
					if (checkForWallX(i, y)) {
						break;
					}
					if (containsMsPacman(x + i, y)) {
						return true;
					}
				}
				break;
			case 3: // down
				for (int i = y; i < 288; ++i) {
					if (checkForWallY(x, i)) {
						break;
					}
					if (containsMsPacman(x, i)) {
						return true;
					}
				}
				break;
			}
		} else { // mspacman
			switch (dir) {
			case 0: // left
				return checkForGhostLeft(x, y);
			case 1: // up
				return checkForGhostUp(x, y);
			case 2: // right
				return checkForGhostRight(x, y);
			case 3: // down
				return checkForGhostDown(x, y);
			}
		}
		return false;
	}

	@Override
	public boolean checkForGhostRight(int x, int y) {
		int[] ghosts = { 16711680, 16759006, 65502, 16758855 };

		for (int ghost : ghosts) {
			for (int i = x; i < 224; ++i) {
				if (checkForWallX(i, y)) {
					break;
				}
				if (containsGhost(ghost, x + i, y)) {
					return true;
				}
			}
		}

		return false;
	}

	@Override
	public boolean checkForGhostLeft(int x, int y) {
		int[] ghosts = { 16711680, 16759006, 65502, 16758855 };

		for (int ghost : ghosts) {
			for (int i = x; i > 0; --i) {
				if (checkForWallX(i, y)) {
					break;
				}
				if (containsGhost(ghost, i, y)) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public boolean checkForWallY(int x, int y) {
		int[] walls = { 4700382, 2171358, 65280, 4700311, 16758935, 14606046 };
		for (int wall : walls) {
			if (getPixel(x + 2, y) == wall) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean checkForGhostUp(int x, int y) {
		int[] ghosts = { 16711680, 16759006, 65502, 16758855 };

		for (int ghost : ghosts) {
			for (int i = y; i > 0; --i) {
				if (checkForWallY(x, i)) {
					break;
				}
				if (containsGhost(ghost, x, i)) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public boolean checkForGhostDown(int x, int y) {
		int[] ghosts = { 16711680, 16759006, 65502, 16758855 };

		for (int ghost : ghosts) {
			for (int i = y; i < 288; ++i) {
				if (checkForWallY(x, i)) {
					break;
				}
				if (containsGhost(ghost, x, i)) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public boolean containsGhost(int ghost, int x, int y) {
		return (getPixel(x + 5, y + 1) != ghost
				&& getPixel(x + 6, y + 1) == ghost
				&& getPixel(x + 9, y + 1) == ghost
				&& getPixel(x + 10, y + 1) != ghost
				&& getPixel(x + 1, y + 6) != ghost
				&& getPixel(x + 1, y + 7) == ghost
				&& getPixel(x + 14, y + 6) != ghost && getPixel(x + 14, y + 7) == ghost);
	}

	public boolean containsMsPacman(int x, int y) {
		return (getPixel(x + 10, y + 5) == 16776960
				&& getPixel(x + 11, y + 3) == 2171358
				&& getPixel(x + 12, y + 4) == 2171358
				&& getPixel(x + 11, y + 4) == 16711680 && getPixel(x + 12,
				y + 3) == 16711680)
				|| (getPixel(x + 5, y + 5) == 16776960
						&& getPixel(x + 4, y + 3) == 2171358
						&& getPixel(x + 3, y + 4) == 2171358
						&& getPixel(x + 4, y + 4) == 16711680 && getPixel(
						x + 3, y + 3) == 16711680)
				|| (getPixel(x + 5, y + 10) == 16776960
						&& getPixel(x + 4, y + 12) == 2171358
						&& getPixel(x + 3, y + 11) == 2171358
						&& getPixel(x + 4, y + 11) == 16711680 && getPixel(
						x + 3, y + 12) == 16711680);
	}

	@Override
	public int[] getMsPacman() {
		for (int y = 27; y < 260; ++y) {
			for (int x = 0; x < 216; ++x) {
				if (containsMsPacman(x, y)) {
					return new int[] { x, y };
				}
			}
		}
		return new int[] { -1, -1 };
	}

	@Override
	public int[] getGhost(int ghost) {
		for (int y = 27; y < 253; ++y) {
			for (int x = 0; x < 216; ++x) {
				if (containsGhost(ghost, x, y)) {
					return new int[] { x, y };
				}
			}
		}

		return new int[] { -1, -1 };
	}

	@Override
	public int relativeDistance(int entity, int item) {
		int[] ent1 = new int[2];
		int[] ent2 = new int[2];

		if (entity == 16776960) {
			ent1 = getMsPacman();
		} else {
			ent1 = getGhost(entity);
		}

		if (item == 16776960) {
			ent2 = getMsPacman();
		} else if (item == 14606046) { // pill
			ent2 = findClostestPill(ent1[0], ent1[1]);
		} else {
			ent2 = getGhost(item);
		}

		double distance = Math.sqrt(Math.pow((ent1[0] - ent2[0]), 2)
				+ Math.pow((ent1[1] - ent2[1]), 2));

		return (int) Math.floor(distance);
	}

	private boolean checkForPill(int x, int y) {
		return getPixel(x, y) == 0 && getPixel(x + 3, y) == 0
				&& getPixel(x, y + 3) == 0 && getPixel(x + 3, y + 3) == 0
				&& getPixel(x + 1, y + 1) == 14606046
				&& getPixel(x + 1, y + 2) == 14606046
				&& getPixel(x + 2, y + 1) == 14606046
				&& getPixel(x + 2, y + 2) == 14606046;
	}

	private int[] findClostestPill(int x, int y) {
		int xs = x;
		int ys = y;

		for (int d = 1; d < 16 * 5; d++) {
			for (int i = 0; i < d + 1; i++) {
				int x1 = xs - d + i < 3 ? 3 : xs - d + 1;
				int y1 = ys - i < 29 ? 29 : ys - 1;

				if (checkForPill(x1, y1)) {
					return new int[] { x1, y1 };
				}

				int x2 = xs + d - i < 3 ? 3 : xs + d - i;
				int y2 = ys + i < 29 ? 29 : ys + i;

				if (checkForPill(x2, y2)) {
					return new int[] { x2, y2 };
				}
			}

			for (int i = 1; i < d; i++) {
				int x1 = xs - i < 3 ? 3 : xs - i;
				int y1 = ys + d - i < 29 ? 29 : ys + d - i;

				if (checkForPill(x1, y1)) {
					return new int[] { x1, y1 };
				}

				int x2 = xs + d - i < 3 ? 3 : xs + d - i;
				int y2 = ys - i < 29 ? 29 : ys - i;

				if (checkForPill(x2, y2)) {
					return new int[] { x2, y2 };
				}
			}
		}

		return new int[] { -1, -1 };
	}

	@Override
	protected void processKeyEvent(KeyEvent e) {
		int code = e.getKeyCode();
		switch (e.getID()) {

		case KeyEvent.KEY_PRESSED:
			switch (code) {

			case KeyEvent.VK_P:
				pause = !pause;
				break;

			case KeyEvent.VK_ESCAPE:
				m.reset(false);
				break;

			case KeyEvent.VK_7:
				if (t.getFrameSkip() > 0) {
					t.setFrameSkip(t.getFrameSkip() - 1);
					t.enableAutoFrameSkip(false);
				} else {
					t.enableAutoFrameSkip(true);
				}
				break;

			case KeyEvent.VK_8:
				if (t.isAutoFrameSkip()) {
					t.enableAutoFrameSkip(false);
					t.setFrameSkip(0);
				} else if (t.getFrameSkip() < 12) {
					t.setFrameSkip(t.getFrameSkip() + 1);
					t.enableAutoFrameSkip(false);
				}
				break;

			case KeyEvent.VK_9:
				t.enable(!t.isEnabled());
				break;

			default: // 255=nothing, 254=up, 253=left, 251=right, 247=down
				switch (code) {
				case KeyEvent.VK_UP:
					m.writeInput(254);
					break;
				case KeyEvent.VK_LEFT:
					m.writeInput(253);
					break;
				case KeyEvent.VK_RIGHT:
					m.writeInput(251);
					break;
				case KeyEvent.VK_DOWN:
					m.writeInput(247);
					break;
				default:
					m.keyPress(code);
					break;
				}
				break;
			}
			break;

		case KeyEvent.KEY_RELEASED:
			switch (code) {
			case KeyEvent.VK_UP:
			case KeyEvent.VK_LEFT:
			case KeyEvent.VK_RIGHT:
			case KeyEvent.VK_DOWN:
				// m.writeInput(255);
				break;
			default:
				m.keyRelease(code);
				break;
			}
			break;
		}
	}

	public synchronized void stopMSP() {
		this.stop = true;
		t.enable(false);
	}

	public synchronized boolean shouldContinue() {
		return !stop;
	}

	private class SendKeys implements Runnable {
		@Override
		public void run() {
			try {
				Thread.sleep(100);
				m.keyPress(KeyEvent.VK_5);
				Thread.sleep(100);
				m.keyRelease(KeyEvent.VK_5);
				Thread.sleep(100);
				m.keyPress(KeyEvent.VK_1);
				Thread.sleep(100);
				m.keyRelease(KeyEvent.VK_1);
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
