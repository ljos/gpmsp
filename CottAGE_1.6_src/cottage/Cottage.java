package cottage;

import java.awt.AWTEvent;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.net.URL;

import jef.machine.Machine;
import jef.video.GfxProducer;
import jef.util.Throttle;

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

/** reference to the driver **/
    Machine m;
    Throttle t;

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
                if(t.getFrameSkip() > 0) {
                    t.setFrameSkip(t.getFrameSkip()-1);
                    t.enableAutoFrameSkip(false);
                } else {
                    t.enableAutoFrameSkip(true);
                }
                break;

            case KeyEvent.VK_8:
                if(t.isAutoFrameSkip()) {
                    t.enableAutoFrameSkip(false);
                    t.setFrameSkip(0);
                } else if(t.getFrameSkip() < 12) {
                    t.setFrameSkip(t.getFrameSkip()+1);
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

    public void main(int w, int h) {
        String driver = "";
        
        System.out.println("CottAGE version " + VERSION + " using JEF version " + jef.Version.VERSION + ".");

        try {
            base_URL = new URL("file://localhost/Users/bjarte/Documents/workspace/mspacman/bin/");
        } catch(Exception e) {
		}
        try {
            driver   = "mspacman";
        } catch(Exception e) {
        }
        try {
            doubled  = getParameter("DOUBLE").equals("Yes");
        } catch(Exception e) {
        }
        try {
            scale2x  = getParameter("SCALE2X").equals("Yes");
        } catch(Exception e) {
        }
        try {
            sound    = false;
        } catch(Exception e) {
        }

        int sLineBuf = getPar("LINEBUFFER"); if(sLineBuf == -1) sLineBuf = 4096;
        jef.util.Config.SOUND_BUFFER_SIZE = sLineBuf;

        int sSampFrq = getPar("SAMPLINGRATE"); if(sSampFrq == -1) sSampFrq = 22050;
        jef.util.Config.SOUND_SAMPLING_FREQ = sSampFrq;

        main = this;

        CottageDriver d = new CottageDriver();

        /* prepare TXT stuff */
        jef.video.Console.init(w,h,this);
        pixel = new int[w*h];
        update(pixel);

        showTXT = true;
        m = d.getMachine(base_URL, driver);
        showTXT = false;

        pixel = null;
        jef.video.Console.init(w,h,this);

        m.setSound(sound);

        if(!doubled && !scale2x)
            pixel = m.refresh(true).getPixels();
        else
            pixel = new int[m.refresh(true).getPixels().length * 4];

        enableEvents(AWTEvent.KEY_EVENT_MASK);

        requestFocus();

        System.out.println("Running...");

        t = new Throttle(m.getProperty(Machine.FPS));

        while(true) {
			if(!paused) {
				update(m.refresh(true));
			}
			t.throttle();
        }
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

    public void postPaint(Graphics g) {
        if (paused) {
			String text = "GAME PAUSED";
			jef.video.Console.drawTextLine(g, getWidth()/2 - 6 * text.length()/2, getHeight()/2 + 6, text);
        } else if (showFPS) {
            StringBuffer buf = new StringBuffer();
            String fs = Integer.toString(t.getFrameSkip());
            String afs = Float.toString(t.getAverageFPS());
            if(t.isAutoFrameSkip()) fs = "AUTO(" + fs + ")";
            buf.append(t.getFPS()).append("/").append(t.getTargetFPS()).append("/").append(afs);
            buf.append("  thr:").append(t.isEnabled());
            buf.append("  sl:").append(t.getSleep());
            buf.append("  fs:").append(fs);
			jef.video.Console.drawTextLine(g, 1, 12, buf.toString());
        } else if (showTXT) {
			jef.video.Console.drawText(g);
        }
    }

    /**
     * Get a numeric parameter from the HTML page holding the applet.
     * If the parameter is not (correctly) defined in the HTML page, -1 is returned.
     */
    private final int getPar(String parStr) {
        int returnValue = -1;
        try {
            parStr = getParameter(parStr);
            if(parStr != null) {
                returnValue = Integer.parseInt(parStr);
            }
        } catch(Exception e) {
        }
        return returnValue;
    }
}
