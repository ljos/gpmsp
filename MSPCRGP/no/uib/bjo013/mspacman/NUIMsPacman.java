package no.uib.bjo013.mspacman;

import java.awt.event.KeyEvent;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.CountDownLatch;

import jef.machine.Machine;
import jef.util.Throttle;
import cottage.CottageDriver;
import cottage.machine.Pacman;

public class NUIMsPacman implements MsPacman {
	private int[] pixel;
	private Pacman m;
	private Throttle t;
	private boolean stop = false;

	private final CountDownLatch[] signal;

	public NUIMsPacman(CountDownLatch[] signal) {
		this.signal = signal;
	}

	@Override
	public void run() {
		URL base_URL = null;
		try {
			base_URL = new URL(String.format("file://localhost/%s/.mspacman/",
					System.getProperty("user.home")));
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

		String driver = "mspacman";
		boolean sound = false;

		int sLineBuf = 4096;
		jef.util.Config.SOUND_BUFFER_SIZE = sLineBuf;

		int sSampFrq = 22050;
		jef.util.Config.SOUND_SAMPLING_FREQ = sSampFrq;

		CottageDriver d = new CottageDriver();

		m = (Pacman) d.getMachine(base_URL, driver);
		m.setSound(sound);

		pixel = new int[m.refresh(true).getPixels().length];

		pixel = m.refresh(true).getPixels();

		t = new Throttle(m.getProperty(Machine.FPS));
		t.enable(true);

		int i = 3;
		while (i > 0) { //finding if the game is at start screen.
			m.refresh(true);
			t.throttle();
			if (((cottage.machine.Pacman) m).md.getREGION_CPU()[0x43F8] == 0) {
				--i;
			} else if (i < 3) {
				++i;
			}
		}

		Thread tj = new Thread(new SendKeys()); //sending keys for starting the game
		tj.start();
		
		i = 3;
		while (i > 0) { //waiting for ready message to appear
			m.refresh(true);
			t.throttle();
			if (((cottage.machine.Pacman) m).md.getREGION_CPU()[0x4252] == 82) {
				--i;
			} else if (i < 3) {
				++i;
			}
		}

		for (;;) { // waiting for ready message to disappear
			m.refresh(true);
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
		
		int sevenseven = 0;
		while (!stop && sevenseven < 10) { //running game
			m.refresh(true);
			t.throttle();
			if (this.isGameOver() && !stop) {
				i = 3;
				while (i > 0) { //finding if the game is past ended screen
					m.refresh(true);
					t.throttle();
					if (((cottage.machine.Pacman) m).md.getREGION_CPU()[0x4252] != 77) {
						--i;
					} else if (i < 3) {
						++i;
					}
				}
				Thread th = new Thread(new SendKeys());
				th.start();
				
				i = 3;
				while (i > 0) { //waiting for ready message to appear
					m.refresh(true);
					t.throttle();
					if (((cottage.machine.Pacman) m).md.getREGION_CPU()[0x4252] == 82) {
						--i;
					} else if (i < 3) {
						++i;
					}
				}

				for (;;) { // waiting for ready message to disappear
					m.refresh(true);
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
			if (((cottage.machine.Pacman) m).md.getREGION_CPU()[0x4252] == 77) {
				++sevenseven;
			} else if (sevenseven > 0) {
				--sevenseven;
			}
		}
		for(CountDownLatch l : signal) {
			l.countDown();
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
				&& getPixel(x + 14, y + 6) != ghost 
				&& getPixel(x + 14, y + 7) == ghost);
	}

	@Override
	public int[] getMsPacman() {
		for (int y = 27; y < 260; ++y) {
			for (int x = 0; x < 216; ++x) {
				if ((getPixel(x + 10, y + 5) == 16776960
						&& getPixel(x + 11, y + 3) == 2171358
						&& getPixel(x + 12, y + 4) == 2171358
						&& getPixel(x + 11, y + 4) == 16711680 && getPixel(
						x + 12, y + 3) == 16711680)
						|| (getPixel(x + 5, y + 5) == 16776960
								&& getPixel(x + 4, y + 3) == 2171358
								&& getPixel(x + 3, y + 4) == 2171358
								&& getPixel(x + 4, y + 4) == 16711680 
								&& getPixel(x + 3, y + 3) == 16711680)
						|| (getPixel(x + 5, y + 10) == 16776960
								&& getPixel(x + 4, y + 12) == 2171358
								&& getPixel(x + 3, y + 11) == 2171358
								&& getPixel(x + 4, y + 11) == 16711680 
								&& getPixel(x + 3, y + 12) == 16711680)) {
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
				if (getPixel(x + 5, y + 1) != ghost
						&& getPixel(x + 6, y + 1) == ghost
						&& getPixel(x + 9, y + 1) == ghost
						&& getPixel(x + 10, y + 1) != ghost
						&& getPixel(x + 1, y + 6) != ghost
						&& getPixel(x + 1, y + 7) == ghost
						&& getPixel(x + 14, y + 6) != ghost
						&& getPixel(x + 14, y + 7) == ghost) {
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
	public void keyPressed(int keyCode) {
		m.keyPress(keyCode);
	}

	@Override
	public void keyReleased(int keyCode) {
		m.keyRelease(keyCode);
	}

	@Override
	public long getScore() {
		return getScore(0x43f7, m.md.getREGION_CPU());
		// score = 0x43f7, highscore = 0x43ed
	}

	private long getScore(int offset, int[] mem) {
		final int ZERO_CHAR = 0x00;
		final int BLANK_CHAR = 0x40;

		long score = 0;

		// calculate the score
		for (int i = 0; i < 7; i++) {
			int c = mem[offset + i];
			if (c == 0x00 || c == BLANK_CHAR)
				c = ZERO_CHAR;
			c -= ZERO_CHAR;
			score += (c * Math.pow(10, i));
		}

		return (score > 9999999) ? 0 : score;
	}

	public synchronized void stopMSP() {
		this.stop = true;
	}

	@Override
	public boolean isGameOver() {
		return ((Pacman) m).md.getREGION_CPU()[0x403B] == 67;
	}
	
	public boolean stopq() {
		return stop;
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
