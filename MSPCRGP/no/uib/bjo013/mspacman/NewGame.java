package no.uib.bjo013.mspacman;

import java.awt.event.KeyEvent;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jef.machine.Machine;
import jef.util.Throttle;
import jef.video.BitMap;
import no.uib.bjo013.mspacman.map.NewGameMap;
import no.uib.bjo013.mspacman.map.Node;
import cottage.CottageDriver;
import cottage.machine.Pacman;

public class NewGame {
	private Pacman m;
	private Throttle t;
	private BitMap bitmap;

	private NewGameMap gm;
	private List<Node> path;
	private Node target;
	private Map<Node, Double> adjustments = new HashMap<Node, Double>();

	public NewGame() {
		init(false);
	}

	public NewGame(boolean throttle) {
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
		gm = new NewGameMap(bitmap);
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
			gm = new NewGameMap(bitmap);
			this.waitForReadyMessageDissapear();
		}
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
				path = gm.calculatePath(target, adjustments);
				Iterator<Node> ph = path.iterator();
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
	public void moveTowards(Node p) {
		Node m = gm.getMsPacman();
		int mx = (m.p.x > 220 && p.p.x < 15) ? -1 : m.p.x;
		int my = m.p.y;
		int px = (p.p.x > 220 && m.p.x < 15) ? -1 : p.p.x;
		int py = p.p.y;
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

	public List<Node> getPath() {
		return path;
	}

	public void setTarget(Node target) {
		this.target = target;
	}

	public void adjustScores(Map<Node, Double> ps) {
		adjustments.putAll(ps);
	}

	public void adjustScore(Node p, Double score) {
		if (adjustments.containsKey(p)) {
			score += adjustments.get(p);
		}
		adjustments.put(p, score);
	}

	public void adjustCircle(Node origin, int radius, double value) {
		Set<Node> ns = new HashSet<Node>(origin.getNeighbors());
		for(int i = 0; i < radius; i++) {
			Iterator<Node> iter = ns.iterator();
			while(iter.hasNext()) {
				ns.addAll(iter.next().getNeighbors());
			}
		}
		for(Node n : ns) {
			adjustments.put(n, value);
		}
	}

	public NewGameMap getMap() {
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
