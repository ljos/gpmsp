package no.uib.bjo013.mspacman;

import java.awt.Point;
import java.awt.event.KeyEvent;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import jef.machine.Machine;
import jef.util.Throttle;
import jef.video.BitMap;
import no.uib.bjo013.mspacman.map.GameMap;
import cottage.CottageDriver;
import cottage.machine.Pacman;

public class Game {
	private Pacman m;
	private Throttle t;
	private BitMap bitmap;

	private GameMap gm;
	private List<Point> path;
	private Point target;
	private Map<Point, Double> adjustments = new HashMap<Point, Double>();

	public Game() {
		init(false);
	}

	public Game(boolean throttle) {
		init(throttle);
	}

	private void init(boolean throttle) {
		CottageDriver d = new CottageDriver();

		String driver = "mspacman";

		URL base_URL = null;
		try {
			base_URL = new URL(String.format("file://localhost/%s/.mspacman/",
					System.getProperty("user.home")));
		} catch (Exception e) {
		}

		m = (Pacman) d.getMachine(base_URL, driver);
		bitmap = m.refresh(true);
		t = new Throttle(m.getProperty(Machine.FPS));
		t.enable(throttle);
	}

	public BitMap initialize() {
		for (int i = 3; i > 0;) {
			bitmap = m.refresh(true);
			if (((cottage.machine.Pacman) m).md.getREGION_CPU()[0x43F8] == 0) {
				--i;
			} else if (i < 3) {
				++i;
			}
		}
		return bitmap;
	}

	public BitMap waitForReadyMessageAppear() {
		Runnable sendKeys = new Runnable() {
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
		};
		Thread th = new Thread(sendKeys);
		for (;;) {
			if (!th.isAlive()) {
				th = new Thread(sendKeys);
				th.start();
			}
			bitmap = m.refresh(true);
			if (((cottage.machine.Pacman) m).md.getREGION_CPU()[0x4252] == 82) {
				break;
			}
		}
		try {
			th.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return bitmap;
	}

	public BitMap waitForReadyMessageDissapear() {
		for (;;) {
			bitmap = m.refresh(true);
			if (((cottage.machine.Pacman) m).md.getREGION_CPU()[0x4252] == 64) {
				break;
			}
		}
		return bitmap;
	}

	public BitMap start() {
		this.waitForReadyMessageAppear();
		gm = new GameMap(bitmap);
		this.waitForReadyMessageDissapear();
		return bitmap;
	}

	/*
	 * MOVE TOWARDS IS DONE ON THE THIRD ELEMENT IN THE LIST. THE REASON FOR
	 * THIS IS THAT THE FIRST ELEMENT IS MSP AND THAT THE SECOND IS TOO CLOSE
	 * AND MSP WILL THROW A FIT IF WE GO FOR SECOND.
	 */
	private long nochange = System.currentTimeMillis();
	private long lastscore = 0;
	public BitMap update() {
		bitmap = m.refresh(true);
		if (((cottage.machine.Pacman) m).md.getREGION_CPU()[0x4252] == 82) {
			nochange = System.currentTimeMillis();
			gm = new GameMap(bitmap);
			this.waitForReadyMessageDissapear();
		}
		System.out.println("SCORE " + lastscore + " " + (System.currentTimeMillis() - nochange));
		if ((System.currentTimeMillis() - nochange) > 10000) {
			gm.update(bitmap);
			return bitmap;
		} else {
			if (lastscore != getScore()) {
				lastscore = getScore();
				nochange = System.currentTimeMillis();
			}
			
			gm.update(bitmap);
			try {
				gm.resetSearch();
				target = gm.adjustScores(adjustments);
				path = gm.calculatePath(target);
				Iterator<Point> ph = path.iterator();
				ph.next();
				ph.next();
				this.moveTowards(ph.next());
				t.throttle();
			} catch (Exception e) {
			}
			adjustments.clear();
			return bitmap;
		}
	}

	/*
	 * ORDER IS VERY IMPORTANT FOR THE IF ELSE TREE
	 * 
	 * THE REASON FOR THE +-1X IS THAT SOMETIMES MSP IS IN THE WRONG POSITION.
	 * THIS IS THE FAULT OF THE EMULATOR OR SOMETHING. IT WORKS NOW ATLEAST.
	 * 
	 * THE -1 RETURN MAKES SURE IT CONTINUES IN THE CORRECT DIRECTION WHEN IT
	 * TRIES TO MOVE THROUGH PORTAL.
	 */
	public void moveTowards(Point p) {
		Point m = gm.getMsPacman();
		int mx = (m.x > 220 && p.x < 15) ? -1 : m.x;
		int my = m.y;
		int px = (p.x > 220 && m.x < 15) ? -1 : p.x;
		int py = p.y;
		if (py < my && px <= mx && px >= mx - 1) {
			this.keyPressed(KeyEvent.VK_UP);
		} else if (py > my && px <= mx + 1 && px >= mx - 1) {
			this.keyPressed(KeyEvent.VK_DOWN);
		} else if (px < mx) {
			this.keyPressed(KeyEvent.VK_LEFT);
		} else if (px > mx) {
			this.keyPressed(KeyEvent.VK_RIGHT);
		}
	}

	public List<Point> getPath() {
		return path;
	}

	public void setTarget(Point target) {
		this.target = target;
	}

	public void adjustScores(Map<Point, Double> ps) {
		adjustments.putAll(ps);
	}

	public void adjustScore(Point p, Double score) {
		if (adjustments.containsKey(p)) {
			score += adjustments.get(p);
		}
		adjustments.put(p, score);
	}

	public void adjustCircle(Point origin, int radius, double value) {
		try {
			for (int i = -radius; i < radius; ++i) {
				for (int j = -radius; j < radius; ++j) {
					Point p = new Point(origin.x + i, origin.y + j);
					if (gm.validPoint(p)) {
						adjustments.put(p, value);
					}
				}
			}
		} catch (Exception e) {
		}
	}

	public GameMap getMap() {
		return gm;
	}

	public void keyPressed(int code) {
		switch (code) {
		case KeyEvent.VK_P:
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
		// SCORE = 0x43f7, HIGH SCORE = 0x43ed
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

	public boolean isGameOver() {
		if (((cottage.machine.Pacman) m).md.getREGION_CPU()[0x403B] == 67) {
			nochange=0;
			return true;
		}
		return false;
	}

	public int[] getPixels() {
		return bitmap.getPixels();
	}

	public int getPixel(int x, int y) {
		return bitmap.getPixel(x, y);
	}

	public void setThrottle(boolean enable) {
		t.enable(enable);
	}
}
