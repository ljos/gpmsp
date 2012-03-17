package no.uib.bjo013.mspacman;

import jef.video.BitMap;
import jef.video.GfxProducer;

public class GfxMsPacman extends GfxProducer {
	public static final long serialVersionUID = 7496968761560424439L;
	
	private BitMap bitmap;
	private boolean stop = false;
	
	public GfxMsPacman(BitMap bitmap) {
		this.bitmap = bitmap;
	}
	
	@Override
	public void main(int w, int h) {
		while(!stop) {
			synchronized (this) {
				try {
					this.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			update(bitmap);
		}
	}
	
	public synchronized void stop() {
		stop=true;
	}
	
	public synchronized void setBitmap(BitMap bitmap) {
		this.bitmap = bitmap;
	}
}
