/***************************************************************************

  vidhrdw.c

  Functions to emulate the video hardware of the machine.

***************************************************************************/

package cottage.vidhrdw;

import cottage.mame.MAMEVideo;
import jef.map.*;
import jef.machine.*;
import jef.video.*;

public class Arkanoid extends MAMEVideo implements VideoEmulator,Vh_refresh,Vh_start,Vh_stop,Vh_convert_color_proms {

static int gfxbank,palettebank;
public int arkanoid_paddle_select;

static boolean flip_screen_x,flip_screen_y;
static boolean change = false;

public void init(MachineDriver md) {
	super.init_bis(md);
	super.init(md);
}

public WriteHandler arkanoid_d008_w() { return new Arkanoid_d008_w(); }
public class Arkanoid_d008_w implements WriteHandler {
	public void write(int address, int data) {
		/* bits 0 and 1 flip X and Y, I don't know which is which */
		flip_screen_x = ((data & 0x01) != 0);
		flip_screen_y = ((data & 0x02) != 0);
		//flip_screen_x_set(data & 0x01);
		//flip_screen_y_set(data & 0x02);

		/* bit 2 selects the input paddle */
		arkanoid_paddle_select = data & 0x04;

		/* bit 3 is coin lockout (but not the service coin) */
		//coin_lockout_w(0, !(data & 0x08));
		//coin_lockout_w(1, !(data & 0x08));

		/* bit 4 is unknown */

		/* bits 5 and 6 control gfx bank and palette bank. They are used together */
		/* so I don't know which is which. */
		int aux;

		change = false;

		aux = (data & 0x20) >> 5;
		if (gfxbank != aux) {
			gfxbank = aux;
			change = true;
		}
		//set_vh_global_attribute(&gfxbank, (data & 0x20) >> 5);
		aux = (data & 0x40) >> 6;
		if (palettebank != aux) {
			palettebank = aux;
			change = true;
		}
		//set_vh_global_attribute(&palettebank, (data & 0x40) >> 6);

		/* bit 7 is unknown */
	}
}


/***************************************************************************

  Draw the game screen in the given mame_bitmap.
  Do NOT call osd_update_display() from this function, it will be called by
  the main emulation engine.

***************************************************************************/
public BitMap video_update() {
	int offs;

	//if (change)
	//{
	//	for(int i=0; i<videoram_size; i++)
	//		dirtybuffer[i] = true;
	//}
/*	if (get_vh_global_attribute_changed())
	{
		memset(dirtybuffer,1,videoram_size);
	}*/

	/* for every character in the Video RAM, check if it has been modified */
	/* since last time and update it accordingly. */
	for (offs = videoram_size - 2;offs >= 0;offs -= 2)
	{
		int offs2;

		offs2 = offs/2;
		//if (dirtybuffer[offs] || dirtybuffer[offs+1])
		//{
			int sx,sy,code;


			dirtybuffer[offs] = false;
			dirtybuffer[offs + 1] = false;

			sx = offs2 % 32;
			sy = offs2 / 32;

			if (flip_screen_x) sx = 31 - sx;
			if (flip_screen_y) sy = 31 - sy;

			code = RAM[videoram + offs + 1] + ((RAM[videoram + offs] & 0x07) << 8) + 2048 * gfxbank;
			drawgfx(tmpbitmap,Machine_gfx[0],
					code,
					((RAM[videoram + offs] & 0xf8) >> 3) + 32 * palettebank,
					flip_screen_x,flip_screen_y,
					8*sx,8*sy,
					GfxManager.TRANSPARENCY_NONE,0);
		//}
	}


	/* copy the temporary bitmap to the screen */
	copybitmap(bitmap,tmpbitmap,0,0,0,0,Machine_visible_area,GfxManager.TRANSPARENCY_NONE,0);

	/* Draw the sprites. */
	for (offs = 0;offs < spriteram_size;offs += 4)
	{
		int sx,sy,code;


		sx = RAM[spriteram + offs];
		sy = 248 - RAM[spriteram + offs + 1];
		if (flip_screen_x) sx = 248 - sx;
		if (flip_screen_y) sy = 248 - sy;

		code = RAM[spriteram + offs + 3] + ((RAM[spriteram + offs + 2] & 0x03) << 8) + 1024 * gfxbank;
		drawgfx(bitmap,Machine_gfx[0],
				2 * code,
				((RAM[spriteram + offs + 2] & 0xf8) >> 3) + 32 * palettebank,
				flip_screen_x,flip_screen_y,
				sx,sy + (flip_screen_y ? 8 : -8),
				GfxManager.TRANSPARENCY_PEN,0);
		drawgfx(bitmap,Machine_gfx[0],
				2 * code + 1,
				((RAM[spriteram + offs + 2] & 0xf8) >> 3) + 32 * palettebank,
				flip_screen_x,flip_screen_y,
				sx,sy,
				GfxManager.TRANSPARENCY_PEN,0);
	}
	return bitmap;
}

}