package cottage;

import java.awt.AWTEvent;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.net.URL;

import jef.machine.Machine;
import jef.util.Throttle;
import jef.video.BitMap;
import jef.video.GfxProducer;

public class Cottage extends GfxProducer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8867847222040282809L;

	public static final String VERSION = "1.0 beta";
	public static final String RELEASE_DATE = "20-10-2005";

	/** pixel buffer **/
	public static int pixel[];

	/** TXT stuff **/
	public static Cottage main;

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

	@Override
	protected void processKeyEvent(KeyEvent e) {
		int code = e.getKeyCode();
		switch (e.getID()) {

		case KeyEvent.KEY_PRESSED:
			switch (code) {
			case KeyEvent.VK_SPACE:
				break;
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

	@Override
	public void main(int w, int h) {
		String driver = "";

		System.out.println("CottAGE version " + VERSION + " using JEF version "
				+ jef.Version.VERSION + ".");

		try {
			base_URL = new URL(
					"file://localhost/Users/bjarte/Documents/workspace/mspacman/bin/");
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

		pixel = m.refresh(true).getPixels();

		enableEvents(AWTEvent.KEY_EVENT_MASK);

		requestFocus();

		System.out.println("Running...");

		t = new Throttle(m.getProperty(Machine.FPS));
		t.enable(false);

		BitMap bm = m.refresh(true);
		while (!stop) {
			if (!paused) {
				m.refresh(true);
				update(bm);
			}
			t.throttle();
		}
	}

	public int getPixel(int x, int y) {
		return (x >= 0 && x < 224 && y >= 0 && y < 288) ? pixel[x + y * 224]
				: -1;
	}

	@Override
	public void postPaint(Graphics g) {
		if (paused) {
			// String text = "GAME PAUSED";
			// jef.video.Console.drawTextLine(g,
			// getWidth() / 2 - 6 * text.length() / 2,
			// getHeight() / 2 + 6, text);
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
}
