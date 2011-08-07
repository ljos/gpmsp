package cottage.vidhrdw;

import jef.machine.MachineDriver;
import jef.map.ReadHandler;
import jef.map.WriteHandler;
import jef.video.BitMap;
import jef.video.GfxManager;
import jef.video.Vh_convert_color_proms;
import jef.video.Vh_refresh;
import jef.video.Vh_start;
import jef.video.Vh_stop;
import jef.video.VideoEmulator;

import cottage.mame.MAMEVideo;

public class Safarir extends MAMEVideo implements VideoEmulator,Vh_refresh,Vh_start,Vh_stop,Vh_convert_color_proms {

public int[] Fsafarir_ram1 = {0};
public int[] Fsafarir_ram2 = {0};
public int[] Fsafarir_ram_size = {0};
int safarir_ram1, safarir_ram2;
int safarir_ram_size;

int safarir_ram;

static int safarir_scroll;

	public void init(MachineDriver md) {
		super.init_bis(md);
		super.init(md);
		safarir_ram1 = Fsafarir_ram1[0];
		safarir_ram2 = Fsafarir_ram2[0];
		safarir_ram_size = Fsafarir_ram_size[0];
		System.out.println("RAM1="+safarir_ram1);
		System.out.println("RAM2="+safarir_ram2);
		System.out.println("RAMSIZE="+safarir_ram_size);
	}

public WriteHandler safarir_ram_w() { return new Safarir_ram_w(); }
class Safarir_ram_w implements WriteHandler {
	public void write(int address, int data) {
//		System.out.println("["+address+"]="+data);
		RAM[address + safarir_ram] = data;
	}
}

public ReadHandler safarir_ram_r() { return new Safarir_ram_r(); }
class Safarir_ram_r implements ReadHandler {
	public int read(int address) {
//		System.out.println("DATA=["+address+"]="+RAM[address + safarir_ram]);
		return RAM[address + safarir_ram];
	}
}

public WriteHandler safarir_scroll_w() { return new Safarir_scroll_w(); }
class Safarir_scroll_w implements WriteHandler {
	public void write(int address, int data) {
		safarir_scroll = data;
	}
}

public WriteHandler safarir_ram_bank_w() { return new Safarir_ram_bank_w(); }
public class Safarir_ram_bank_w implements WriteHandler {
	public void write(int address, int data) {
		safarir_ram = (data!=0) ? 0 : safarir_ram2 - safarir_ram1;
	}
}

public BitMap video_update()
{
	int offs;
	int j;

	for (offs = safarir_ram_size/2 - 1;offs >= 0;offs--)
	{
		int sx,sy;
		int code;


		sx = offs % 32;
		sy = offs / 32;

		code = RAM[safarir_ram1 + safarir_ram + offs + safarir_ram_size/2];

		if (code!=0)
			System.out.println("CODE1="+code);

		drawgfx(bitmap,Machine_gfx[0],
				code & 0x7f,
				code >> 7,
				0,0,
				(8*sx - safarir_scroll) & 0xff,8*sy,
				Machine_visible_area,GfxManager.TRANSPARENCY_NONE,0);
	}


	/* draw the frontmost playfield. They are characters, but draw them as sprites */

	for (offs = safarir_ram_size/2 - 1;offs >= 0;offs--)
	{
		int sx,sy,transparency;
		int code;


		sx = offs % 32;
		sy = offs / 32;

		code = RAM[safarir_ram1 + safarir_ram + offs];

		if (code!=0)
			System.out.println("CODE2="+code);

		transparency = (sx >= 3) ? GfxManager.TRANSPARENCY_PEN : GfxManager.TRANSPARENCY_NONE;


		drawgfx(bitmap,Machine_gfx[1],
				code & 0x7f,
				code >> 7,
				0,0,
				8*sx,8*sy,
				Machine_visible_area,transparency,0);
	}
	return bitmap;
}


/*static int colortable_source[] =
{
	0x00, 0x01,
	0x00, 0x02,
};*/

public void palette_init() {
	palette_set_color(2,0x00,0x00,0x00); /* black */
	palette_set_color(1,0x80,0x80,0x80); /* gray */
	palette_set_color(0,0xff,0xff,0xff); /* white */

//	memcpy(colortable,colortable_source,sizeof(colortable_source));
}

}