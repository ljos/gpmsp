package jef.video;

import jef.machine.MachineDriver;
import jef.video.BitMap;

public class BasicVideoEmulator implements VideoEmulator,
											Eof_callback,
											Vh_start,
											Vh_stop,
											Vh_refresh,
											Vh_convert_color_proms
{
	protected BitMap bitmap;

	protected BitMap getDisplay() {
		return this.bitmap;
	}

	public void eof_callback() {}
	public void palette_init() {}
	public BitMap video_update() { return this.bitmap; }
	public void video_post_update() {}
	public int vh_start() { return 0; }
	public void vh_stop() {}
	public void init(MachineDriver md) {}
}
