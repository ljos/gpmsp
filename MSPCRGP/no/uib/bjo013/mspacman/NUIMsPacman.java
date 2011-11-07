package no.uib.bjo013.mspacman;

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
	
	private final CountDownLatch signal;
	
	public NUIMsPacman(CountDownLatch signal) {
		this.signal = signal;
	}
	
	public NUIMsPacman() {
		this.signal = new CountDownLatch(1);
	}

	@Override
	public void run() {
		String driver = "";
		
		System.out.println("pacman started " + Thread.currentThread().getId());

		URL base_URL = null;
		try {
			base_URL = new URL(
					String.format("file://localhost/%s/.mspacman/", 
							System.getProperty("user.home")));
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

		driver = "mspacman";
		boolean sound = false;

		int sLineBuf = 4096;
		jef.util.Config.SOUND_BUFFER_SIZE = sLineBuf;

		int sSampFrq = 22050;
		jef.util.Config.SOUND_SAMPLING_FREQ = sSampFrq;

		CottageDriver d = new CottageDriver();

		m = (Pacman)d.getMachine(base_URL, driver);
		m.setSound(sound);

		pixel = new int[m.refresh(true).getPixels().length];

		pixel = m.refresh(true).getPixels();
		
		
		t = new Throttle(m.getProperty(Machine.FPS));

		
		
		while (!stop) {
			pixel = m.refresh(true).getPixels();
			t.throttle();
		}
		System.out.println("pacman stopped " + Thread.currentThread().getId());
	}

	@Override
	public int[] getPixels() {
		return pixel;
	}
	
	@Override
	public int getPixel(int x, int y) {
		return (x>=0 && x<224 && y>=0 && y<288) ? pixel[x + y * 224] : -1;
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
		return getScore(0x43ed, m.md.getREGION_CPU());
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

	public void stop(boolean stop) {
		this.stop = stop;
	}

	@Override
	public boolean isGameOver() {
		return ((Pacman) m).md.getREGION_CPU()[0x403B] == 67;
	}
}
