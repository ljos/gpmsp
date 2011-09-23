package jef.video;

import java.awt.Color;
import java.awt.Graphics;

public class Console {
	public static String TXT[];
	public static int nTXT;
	public static int cTXT=0;

	public static GfxProducer gp;


	public static void init(int w, int h, GfxProducer gfx) {
        cTXT = 0;
        nTXT = h/11;
        TXT = new String[nTXT];
        for(int iTXT=0; iTXT<nTXT; iTXT++)
            TXT[iTXT] = null;
		gp = gfx;
	}

    public static void drawTextLine(Graphics g, int x, int y, String text) {
      	g.setColor(Color.blue);
      	g.drawString(text, x + 1, y + 1);
      	g.setColor(Color.white);
      	g.drawString(text, x, y);
	}

	public static void drawText(Graphics g) {
		for(int iTXT=0; iTXT<nTXT; iTXT++) {
			if(TXT[iTXT]!=null) {
				drawTextLine(g, 1, 11*(iTXT+1), TXT[iTXT]);
			}
		}
	}

	public static void update() {
		if (gp != null)	gp.update();
	}

}