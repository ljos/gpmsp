package no.uib.bjo013.mspacman;

import java.awt.AWTEvent;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.net.URL;
import java.util.concurrent.CountDownLatch;

import jef.machine.Machine;
import jef.util.Throttle;
import jef.video.GfxProducer;
import cottage.CottageDriver;

public class GUIMsPacman extends GfxProducer implements MsPacman {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8867847222040282809L;

	/** pixel buffer **/
	public static int pixel[];

	/** TXT stuff **/
	public static GUIMsPacman main;

	URL base_URL;

	/** booleans **/
	boolean showFPS = false;
	boolean showTXT = false;
	boolean paused = false;

	boolean doubled = false;
	boolean scale2x = false;
	boolean scanlines = false;
	boolean sound = true;

	boolean stop = false;

	/** reference to the driver **/
	Machine m;
	Throttle t;
	
	private final CountDownLatch signal;
	
	public GUIMsPacman(CountDownLatch signal) {
		this.signal = signal;
	}
	
	public GUIMsPacman() {
		this.signal = new CountDownLatch(1);
	}

	@Override
	protected void processKeyEvent(KeyEvent e) {
		int code = e.getKeyCode();
		switch (e.getID()) {

		case KeyEvent.KEY_PRESSED:
			switch (code) {

			case KeyEvent.VK_P:
				paused = !paused;
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

			case KeyEvent.VK_0:
				showFPS = !showFPS;
				break;

			default:
				m.keyPress(code);
				break;
			}
			break;

		case KeyEvent.KEY_RELEASED:
			m.keyRelease(code);
			break;
		}
	}

	@Override
	public void main(int w, int h) {
		String driver = "";

		try {
			base_URL = new URL(
					String.format("file://localhost/%s/.mspacman/", 
							System.getProperty("user.home")));
		} catch (Exception e) {
		}
		try {
			driver = "mspacman";
		} catch (Exception e) {
		}
		try {
			doubled = getParameter("DOUBLE").equals("Yes");
		} catch (Exception e) {
		}
		try {
			scale2x = getParameter("SCALE2X").equals("Yes");
		} catch (Exception e) {
		}
		try {
			sound = false;
		} catch (Exception e) {
		}

		int sLineBuf = getPar("LINEBUFFER");
		if (sLineBuf == -1)
			sLineBuf = 4096;
		jef.util.Config.SOUND_BUFFER_SIZE = sLineBuf;

		int sSampFrq = getPar("SAMPLINGRATE");
		if (sSampFrq == -1)
			sSampFrq = 22050;
		jef.util.Config.SOUND_SAMPLING_FREQ = sSampFrq;

		main = this;

		CottageDriver d = new CottageDriver();

		/* prepare TXT stuff */
		jef.video.Console.init(w, h, this);
		pixel = new int[w * h];
		update(pixel);

		showTXT = true;
		m = d.getMachine(base_URL, driver);
		showTXT = false;

		pixel = null;
		jef.video.Console.init(w, h, this);

		m.setSound(sound);

		if (!doubled && !scale2x) {
			pixel = m.refresh(true).getPixels();
		} else {
			pixel = new int[m.refresh(true).getPixels().length * 4];
		}

		enableEvents(AWTEvent.KEY_EVENT_MASK);

		requestFocus();

		System.out.println("Running...");

		t = new Throttle(m.getProperty(Machine.FPS));


		while (!stop) {
			if (!paused) {
				update(m.refresh(true));
			}
			t.throttle();
		}
	}

	public int[] getPixels() {
		return pixel;
	}
	
	@Override
	public int getPixel(int x, int y) {
		return (x>=0 && x<224 && y>=0 && y<288) ? pixel[x + y * 224] : -1;
	}

	public void keyPressed(int keyCode) {
		m.keyPress(keyCode);
	}

	public void keyReleased(int keyCode) {
		m.keyRelease(keyCode);
	}

	public long getScore() {
		return getScore(0x43ed, ((cottage.machine.Pacman) m).md.getREGION_CPU());
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
	
	public boolean gameOver() {
		return ((cottage.machine.Pacman)m).md.getREGION_CPU()[0x403B] == 67;
	}

	public void stop(boolean stop) {
		this.stop = stop;
	}
	
	@Override
	public boolean isGameOver() {
		return ((cottage.machine.Pacman) m).md.getREGION_CPU()[0x403B] == 67;
	}

	@Override
	public void postPaint(Graphics g) {
		if (paused) {
			String text = "GAME PAUSED";
			jef.video.Console.drawTextLine(g,
					getWidth() / 2 - 6 * text.length() / 2,
					getHeight() / 2 + 6, text);
		} else if (showFPS) {
			StringBuffer buf = new StringBuffer();
			String fs = Integer.toString(t.getFrameSkip());
			String afs = Float.toString(t.getAverageFPS());
			if (t.isAutoFrameSkip())
				fs = "AUTO(" + fs + ")";
			buf.append(t.getFPS()).append("/").append(t.getTargetFPS())
					.append("/").append(afs);
			buf.append("  thr:").append(t.isEnabled());
			buf.append("  sl:").append(t.getSleep());
			buf.append("  fs:").append(fs);
			jef.video.Console.drawTextLine(g, 1, 12, buf.toString());
		} else if (showTXT) {
			jef.video.Console.drawText(g);
		}
	}

	/**
	 * Get a numeric parameter from the HTML page holding the applet. If the
	 * parameter is not (correctly) defined in the HTML page, -1 is returned.
	 */
	private final int getPar(String parStr) {
		int returnValue = -1;
		try {
			parStr = getParameter(parStr);
			if (parStr != null) {
				returnValue = Integer.parseInt(parStr);
			}
		} catch (Exception e) {
		}
		return returnValue;
	}
}
