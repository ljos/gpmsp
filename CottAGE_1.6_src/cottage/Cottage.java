package cottage;

import java.awt.AWTEvent;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.net.MalformedURLException;
import java.net.URL;

import jef.machine.Machine;
//import jef.video.BitMap;
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
                if(Throttle.getFrameSkip() > 0) {
                    Throttle.setFrameSkip(Throttle.getFrameSkip()-1);
                    Throttle.enableAutoFrameSkip(false);
                } else {
                    Throttle.enableAutoFrameSkip(true);
                }
                break;

            case KeyEvent.VK_8:
                if(Throttle.isAutoFrameSkip()) {
                    Throttle.enableAutoFrameSkip(false);
                    Throttle.setFrameSkip(0);
                } else if(Throttle.getFrameSkip() < 12) {
                    Throttle.setFrameSkip(Throttle.getFrameSkip()+1);
                    Throttle.enableAutoFrameSkip(false);
                }
                break;

            case KeyEvent.VK_9:
                Throttle.enable(!Throttle.isEnabled());
                break;

            case KeyEvent.VK_0:
                showFPS = !showFPS;
                break;

            /*case KeyEvent.VK_F12:
                sound = !sound;
                m.setSound(sound);
                break;*/

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
    
    public static void main(String args[]) throws MalformedURLException {
    	 String driver = ""; 
         System.out.println("CottAGE version " + VERSION + " using JEF version " + jef.Version.VERSION + ".");
         URL base_URL = new URL("file://localhost/Users/bjarte/Documents/workspace/mspacman/bin/");
         driver   = "mspacman";
         boolean sound    = false;

         int sLineBuf = 4096;
         jef.util.Config.SOUND_BUFFER_SIZE = sLineBuf;

         int sSampFrq = 22050;
         jef.util.Config.SOUND_SAMPLING_FREQ = sSampFrq;

         CottageDriver d = new CottageDriver();

         Machine m = d.getMachine(base_URL, driver);
         m.setSound(sound);

 	  	pixel = new int[m.refresh(true).getPixels().length * 4];

        pixel = m.refresh(true).getPixels();

        Throttle.init(m.getProperty(Machine.FPS));

         while(true) {
        	m.refresh(true);
 			Throttle.throttle();
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

	  	pixel = new int[m.refresh(true).getPixels().length * 4];

        if(!doubled && !scale2x)
            pixel = m.refresh(true).getPixels();
        else
            pixel = new int[m.refresh(true).getPixels().length * 4];

        enableEvents(AWTEvent.KEY_EVENT_MASK);

        requestFocus();

        System.out.println("Running...");

        Throttle.init(m.getProperty(Machine.FPS), getThread());

        while(true) {
			if(!paused) {
				update(m.refresh(true));
			}
			Throttle.throttle();
        }
    }

 /*   private final int dimColor(int col) {
		return (col>>1) & 0x7F7F7F;
    } */

/*    private int[] getDisplay(BitMap bm) {
    	int[] pix = bm.getPixels();
        if(!doubled && !scale2x) {
            if(paused) {
                for(int ofs=0; ofs<pix.length; ofs++)
                    pix[ofs] = dimColor(pix[ofs]);
            } else if(showFPS) {
                for(int ofs=0; ofs<getWidth() * 16; ofs++)
		        	pix[ofs] = dimColor(pix[ofs]);
			}

            return pix;
        } else {
			if (doubled) pixel = bm.getScaledBitMap(2, BitMap.SCALE_MODE_TV ).getPixels();
			else if (scale2x) pixel = bm.getScaledBitMap(2, BitMap.SCALE_MODE_SCALE2X ).getPixels();
		}

        if(paused) {
            for(int ofs=0; ofs<pixel.length; ofs++)
            	pixel[ofs] = dimColor(pixel[ofs]);
        } else if(showFPS) {
        	for(int offs=0; offs<getWidth() * 16; offs++)
				pixel[offs] = dimColor(pixel[offs]);
		}
		
        return pixel;
    } */

    public void postPaint(Graphics g) {
        if (paused) {
			String text = "GAME PAUSED";
			jef.video.Console.drawTextLine(g, getWidth()/2 - 6 * text.length()/2, getHeight()/2 + 6, text);
        } else if (showFPS) {
            StringBuffer buf = new StringBuffer();
            String fs = Integer.toString(Throttle.getFrameSkip());
            String afs = Float.toString(Throttle.getAverageFPS());
            if(Throttle.isAutoFrameSkip()) fs = "AUTO(" + fs + ")";
            buf.append(Throttle.getFPS()).append("/").append(Throttle.getTargetFPS()).append("/").append(afs);
            buf.append("  thr:").append(Throttle.isEnabled());
            buf.append("  sl:").append(Throttle.getSleep());
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
