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

	@Override
	public void eof_callback() {}
	@Override
	public void palette_init() {}
	@Override
	public BitMap video_update() { return this.bitmap; }
	@Override
	public void video_post_update() {}
	@Override
	public int vh_start() { return 0; }
	@Override
	public void vh_stop() {}
	@Override
	public void init(MachineDriver md) {}
}
