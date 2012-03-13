package no.uib.bjo013.mspacman;

import java.awt.event.KeyEvent;
import java.net.URL;

import jef.machine.Machine;
import jef.util.Throttle;
import jef.video.BitMap;
import cottage.CottageDriver;
import cottage.machine.Pacman;

public class Game {
	/** reference to the driver **/
	private Pacman m;
	private Throttle t;
	private BitMap bitmap;

	public Game(boolean throttle) {
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
		for (int i = 3; i > 0;) { // finding if the game is at start screen.
			update();
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
		for (;;) { // waiting for ready message to appear
			if (!th.isAlive()) {
				th = new Thread(sendKeys);
				th.start();
			}
			update();
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
		for (;;) { // waiting for ready message to disappear
			update();
			if (((cottage.machine.Pacman) m).md.getREGION_CPU()[0x4252] == 64) {
				break;
			}
		}
		return bitmap;
	}
	
	public BitMap startGame() {
		this.waitForReadyMessageAppear();
		this.waitForReadyMessageDissapear();
		return bitmap;
	}

	public BitMap update() {
		bitmap = m.refresh(true);
		t.throttle();
		return bitmap;
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
		return ((cottage.machine.Pacman) m).md.getREGION_CPU()[0x403B] == 67;
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
