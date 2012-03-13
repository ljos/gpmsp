package no.uib.bjo013.mspacman;

import java.util.concurrent.CountDownLatch;

public class NUIMsPacman implements MsPacman {
	private Game g = new Game(false);
	
	private boolean stop = false;

	private final CountDownLatch[] signal;
	private final Object lock;
	private Thread parent;

	public NUIMsPacman(CountDownLatch[] signal, Object lock, Thread parent) {
		this.signal = signal;
		this.lock = lock;
		this.parent = parent;
	}

	@Override
	public void run() {
		g.initialize();
		g.startGame();
		
		int latch = 0;
		signal[latch].countDown();
		++latch;
		while (this.shouldContinue()) { //running game
			synchronized (lock) {
				try {
					lock.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				g.update();
				while(!parent.getState().equals(Thread.State.WAITING));
				lock.notify();
			}
			
			if (g.isGameOver() && this.shouldContinue()) {
				g.startGame();
				signal[latch].countDown();
				++latch;
			}
		}
		for(CountDownLatch l : signal) {
			l.countDown();
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
		if(entityatxy != 16776960) { //ghosts
			switch(dir) {
			case 0: //left
				for (int i = x; i > 0; --i) {
					if (checkForWallX(i, y)) {
						break;
					}
					if (containsMsPacman(i, y)) {
						return true;
					}
				}
				break;
			case 1: //up
				for (int i = y; i > 0; --i) {
					if (checkForWallY(x, i)) {
						break;
					}
					if (containsMsPacman(x, i)) {
						return true;
					}
				}
				break;
			case 2: //right
				for (int i = x; i < 224; ++i) {
					if (checkForWallX(i, y)) {
						break;
					}
					if (containsMsPacman(x + i, y)) {
						return true;
					}
				}
				break;
			case 3: //down
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
		} else { //mspacman
			switch(dir) {
			case 0: //left
				return checkForGhostLeft(x, y);
			case 1: //up
				return checkForGhostUp(x, y);
			case 2: //right
				return checkForGhostRight(x, y);
			case 3: //down
				return checkForGhostDown(x, y);
			}
		}
		return false;
	}

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

	public boolean checkForWallY(int x, int y) {
		int[] walls = { 4700382, 2171358, 65280, 4700311, 16758935, 14606046 };
		for (int wall : walls) {
			if (getPixel(x + 2, y) == wall) {
				return true;
			}
		}
		return false;
	}

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
	
	public boolean containsMsPacman(int x, int y) {
		return (getPixel(x + 10, y + 5) == 16776960
				&& getPixel(x + 11, y + 3) == 2171358
				&& getPixel(x + 12, y + 4) == 2171358
				&& getPixel(x + 11, y + 4) == 16711680 
				&& getPixel(x + 12, y + 3) == 16711680)
				|| (getPixel(x + 5, y + 5) == 16776960
				        && getPixel(x + 4, y + 3) == 2171358
						&& getPixel(x + 3, y + 4) == 2171358
						&& getPixel(x + 4, y + 4) == 16711680 
						&& getPixel(x + 3, y + 3) == 16711680)
				|| (getPixel(x + 5, y + 10) == 16776960
						&& getPixel(x + 4, y + 12) == 2171358
						&& getPixel(x + 3, y + 11) == 2171358
						&& getPixel(x + 4, y + 11) == 16711680 
						&& getPixel(x + 3, y + 12) == 16711680);
	}

	public int[] getMsPacman() {
		for (int y = 27; y < 260; ++y) {
			for (int x = 0; x < 216; ++x) {
				if (containsMsPacman(x , y)) {
					return new int[] { x, y };
				}
			}
		}
		return new int[] { -1, -1 };
	}

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
}
