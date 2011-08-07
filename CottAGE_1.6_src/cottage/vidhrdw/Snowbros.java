/*
 * Created on Sep 7, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package cottage.vidhrdw;
import jef.machine.MachineDriver;
import jef.map.MemoryMap;
import jef.map.ReadHandler;
import jef.map.WriteHandler;
import jef.video.BitMap;
import jef.video.BitMapImpl;
import jef.video.GfxManager;
import jef.video.Vh_convert_color_proms;
import jef.video.Vh_refresh;
import jef.video.Vh_start;
import jef.video.Vh_stop;
import jef.video.VideoEmulator;

import cottage.mame.MAMEVideo;

/**
 * @author Erik Duijs
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class Snowbros 	extends MAMEVideo
implements VideoEmulator, Vh_refresh, Vh_start, Vh_stop, Vh_convert_color_proms {

	public WriteHandler paletteram_xBBBBBGGGGGRRRRR_word_w(int paletteram) { return new Paletteram_xBBBBBGGGGGRRRRR_word_w(paletteram); }
	public class Paletteram_xBBBBBGGGGGRRRRR_word_w implements WriteHandler {
		int palram;
		public Paletteram_xBBBBBGGGGGRRRRR_word_w(int pram) {
			palram = pram;
            
		}
		public void write(int address, int data) {
            paletteRAM[address&0x1ff] = data;
			changecolor_xBBBBBGGGGGRRRRR(address>>1, (paletteRAM[address&0xfffffe] << 8) | paletteRAM[(address&0xfffffe)+1]);
		}
	}
    
    int[] paletteRAM = new int[0x200];
    
    
	
	private BitMapImpl charLayer;



    private MemoryMap memMap;

	private int READ_WORD(int a) {
		a &= 0xfffffe;
        return (memMap.read(a++) << 8) | memMap.read(a);
	}
	private void WRITE_WORD(int a, int value) {
		a &= 0xfffffe;
        memMap.write(a++, value >> 8);
        memMap.write(a  , value & 0xff);
	}
	private int COMBINE_WORD(int addr, int val2) {
		if ((addr & 1) != 0) {
			return (memMap.read(addr & 0xfffffe) << 8) | val2;
		} else {
			return (val2 << 8) | (memMap.read(addr + 1));
		}
	}

	/**
	 * @return
	 */
	public boolean snowbros() {
		return true;
	}
	public void init(MachineDriver md) {
		super.init(md);
		charLayer = new BitMapImpl(backBuffer.getWidth(), backBuffer.getHeight());
	}
    
    public void setRegions(MemoryMap memMap) {
        this.memMap = memMap;
    }

	public BitMap video_update() {
		int x=0,y=0,offs;


		//palette_recalc ();
		/* no need to check the return code since we redraw everything each frame */


		/*
		 * Sprite Tile Format
		 * ------------------
		 *
		 * Byte(s) | Bit(s)   | Use
		 * --------+-76543210-+----------------
		 *  0-5	| -------- | ?
		 *	6	| -------- | ?
		 *	7	| xxxx.... | Palette Bank
		 *	7	| .......x | XPos - Sign Bit
		 *	9	| xxxxxxxx | XPos
		 *	7	| ......x. | YPos - Sign Bit
		 *	B	| xxxxxxxx | YPos
		 *	7	| .....x.. | Use Relative offsets
		 *	C	| -------- | ?
		 *	D	| xxxxxxxx | Sprite Number (low 8 bits)
		 *	E	| -------- | ?
		 *	F	| ....xxxx | Sprite Number (high 4 bits)
		 *	F	| x....... | Flip Sprite Y-Axis
		 *	F	| .x...... | Flip Sprite X-Axis
		 */

		/* This clears & redraws the entire screen each pass */

		//fillbitmap(bitmap,Machine->gfx[0]->colortable[0],&Machine->drv->visible_area);
		for (int i = 0; i < bitmap.getPixels().length; i++) {
			bitmap.getPixels()[i] = 0;
		}

		for (offs = 0;offs < 0x1e00; offs += 16)
		{
			int sx = READ_WORD(0x700000+8+offs) & 0xff;
			int sy = READ_WORD(0x700000+0x0a+offs) & 0xff;
			int tilecolour = READ_WORD(0x700000+6+offs);

			if ((tilecolour & 1)!=0) sx = -1 - (sx ^ 0xff);

			if ((tilecolour & 2)!=0) sy = -1 - (sy ^ 0xff);

			if ((tilecolour & 4)!=0)
			{
				x += sx;
				y += sy;
			}
			else
			{
				x = sx;
				y = sy;
			}

			if (x > 511) x &= 0x1ff;
			if (y > 511) y &= 0x1ff;

			if ((x>-16) && (y>0) && (x<256) && (y<240))
			{
				int attr = READ_WORD(0x700000 + 0x0e + offs);
				int tile = ((attr & 0x0f) << 8) + (READ_WORD(0x700000 + 0x0c+offs) & 0xff);

				drawgfx(bitmap,0,
						tile,
						(tilecolour & 0xf0) >> 4,
						attr & 0x80, attr & 0x40,
						x,y,
						GfxManager.TRANSPARENCY_PEN,0);
			}
		}
		return bitmap;
	}
    /**
     * @param i
     * @return
     */
    public ReadHandler paletteram_word_r(int i) {
        return new PaletteRAM_R();
    }
    
    public class PaletteRAM_R implements ReadHandler {
        public int read(int address) {
            return paletteRAM[address&0x1ff];
        }
        
    }
}

