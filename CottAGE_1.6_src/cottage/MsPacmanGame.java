package cottage;

import java.awt.event.KeyEvent;
import java.net.MalformedURLException;
import java.net.URL;

import jef.machine.Machine;
import jef.util.Throttle;

public class MsPacmanGame implements Runnable {
	private static String VERSION = "0.01a";
	private int[] pixel;
	private Machine m;
	private Throttle t;
	private boolean stop = false;
	
	
	@Override
	public void run() {
		String driver = "";
		System.out.println("Version " + VERSION);
		
		URL base_URL = null;
		try {
			base_URL = new URL("file://localhost/Users/bjarte/Documents/workspace/mspacman/bin/");
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

		m = d.getMachine(base_URL, driver);
		m.setSound(sound);

		pixel = new int[m.refresh(true).getPixels().length];

		pixel = m.refresh(true).getPixels();

		t = new Throttle(m.getProperty(Machine.FPS));

		while (!stop) {
			pixel = m.refresh(true).getPixels();
			t.throttle();
		}
	}
	
	public int[] getPixels() {
		return pixel;
	}
	
	public void keyPressed(int keyCode) {
		m.keyPress(keyCode);
	}
	
	public void keyReleased(int keyCode) {
		m.keyRelease(keyCode);
	}
	
	public long getScore() {
		return getScore(0x43ed, ((cottage.machine.Pacman)m).md.getREGION_CPU());
	}
	
	private long getScore(int offset, int[] mem) {
		final int ZERO_CHAR = 0x00;
		final int BLANK_CHAR = 0x40;
		
		long score = 0;
		
		// calculate the score
		for (int i = 0; i < 7; i++) {
			int c = mem[offset + i];
			if (c == 0x00 || c == BLANK_CHAR) c = ZERO_CHAR;
			c -= ZERO_CHAR;
			score += (c * Math.pow(10, i));
		}
		
		return (score > 9999999) ? 0 : score;
	}
	
	public void stop(boolean stop) {
		this.stop = stop;
	}
	
	public static void main(String args[]) throws InterruptedException {
		MsPacmanGame g = new MsPacmanGame();
		Thread t = new Thread(g);
		t.start();
		Thread.sleep(6000);
		g.keyPressed(KeyEvent.VK_5);
		System.out.println("####5#####");
		Thread.sleep(500);
		g.keyReleased(KeyEvent.VK_5);
		System.out.println("@@@@release 5@@@@");
		Thread.sleep(500);
		System.out.println("@@@@@1@@@@@");
		g.keyPressed(KeyEvent.VK_1);
		Thread.sleep(100);
		g.keyReleased(KeyEvent.VK_1);
		System.out.println("@@@@@release 1@@@@");
		Thread.sleep(200);
		System.out.println("@@@@@LEFT@@@@@@@");
		g.keyPressed(KeyEvent.VK_LEFT);
		for(int i = 0; i < 30; ++i) {
			Thread.sleep(1000);
			System.out.println(i);
		}
		System.out.println(g.getScore() + " " + t.isAlive());
		g.stop(true);
		Thread.sleep(1000);
		System.out.println(t.isAlive());
		
	}
}
