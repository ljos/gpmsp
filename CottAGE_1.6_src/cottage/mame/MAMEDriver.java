package cottage.mame;

import java.net.URL;

import jef.util.RomLoader;
import cottage.mame.Driver;
import jef.cpu.*;
import jef.cpuboard.CpuDriver;
import jef.map.*;
import jef.machine.Machine;
import jef.machine.MachineDriver;
import jef.video.Eof_callback;
import jef.video.GfxLayout;
import jef.video.GfxDecodeInfo;
import jef.video.Vh_start;
import jef.video.Vh_convert_color_proms;
import jef.video.Vh_refresh;
import jef.video.VideoEmulator;
import jef.sound.SoundChipEmulator;

/**
 * Helper class for porting MAME drivers to CottAGE.
 */
public abstract class MAMEDriver implements Driver, MAMEConstants {

	public MachineDriver md;

	/* Driver info */
	public String driver_name;
	public String driver_prod;
	public String driver_date;
	public String driver_clone;
	public boolean driver_sound;
	public boolean bInfo = false;

	public URL base_URL;

	protected RomLoader romLoader = new RomLoader();
	public int[] properties = new int[0x10];

	private int[][] REGIONS = new int[21][];
	private int cur_region = 0;

	private InputPort[] inps = new InputPort[10];
	private int inp_count = 0;
	protected ReadHandler input_port_0_r;
	protected ReadHandler input_port_1_r;
	protected ReadHandler input_port_2_r;
	protected ReadHandler input_port_3_r;
	protected ReadHandler input_port_4_r;
	protected ReadHandler input_port_5_r;
	protected ReadHandler input_port_6_r;
	protected ReadHandler input_port_7_r;
	protected ReadHandler input_port_8_r;
	protected ReadHandler input_port_9_r;

	protected final ReadHandler soundlatch_r = new Soundlatch_r();
	protected final WriteHandler soundlatch_w = new Soundlatch_w();
	
	private int soundLatch = 0;

	private MemoryReadAddress[] mra = new MemoryReadAddress[8];
	private int mra_count = 0;

	private MemoryWriteAddress[] mwa = new MemoryWriteAddress[8];
	private int mwa_count = 0;

	private IOReadPort[] ior = new IOReadPort[8];
	private int ior_count = 0;

	private IOWritePort[] iow = new IOWritePort[8];
	private int iow_count = 0;

	private VideoEmulator videoEmulator;

	private GfxDecodeInfo[] gdis = new GfxDecodeInfo[8];
	private int gdi_count = 0;

	private CpuDriver[] cpus = new CpuDriver[8];
	private int cpu_count = 0;

	private SoundChipEmulator[] soundChips = new SoundChipEmulator[8];
	private int snd_count = 0;

//	private String GAMEmanufacturer = "";
//	private String GAMEname = "";

	private String DriverName = "";

	private int Fvideoram = -1;
	private int Fvideoram_size = -1;
	private int Fcolorram = -1;
	private int Fspriteram = -1;
	private int Fspriteram_size = -1;
	private int Fspriteram_2 = -1;
	private int Fspriteram_2_size = -1;
	private int Fspriteram_3 = -1;
	private int Fspriteram_3_size = -1;
	private int Fpaletteram = -1;
	private int Fpaletteram_2 = -1;

	protected static final VoidFunction NOP = new NoFunction();
	protected static final GfxDecodeInfo[] NO_GFX_DECODE_INFO = null;
	protected static final SoundChipEmulator[] noSound = null;

	protected class Soundlatch_r implements ReadHandler {
		@Override
		public int read(int offset) {
			//System.out.println("Soundlatch_r :" + soundLatch);
			return soundLatch;
		}
	}

	protected boolean bModify = false;
	protected int cpu_num = -1;

	protected int RGN_FRAC(int num, int den) {
		return (0x80000000 | (((num) & 0x0f) << 27) | (((den) & 0x0f) << 23));
	}

	private class Soundlatch_w implements WriteHandler {
		@Override
		public void write(int address, int data) {
			//System.out.println("Soundlatch_w :" + data);
			soundLatch = data;
		}
		
	}
	protected void soundlatch_w(int offset, int data) {
        //System.out.println("Soundlatch_w :" + data);
		this.soundLatch = data;
	}

	protected int getSoundLatch() {
		return soundLatch;
	}

	protected void cpu_set_irq_line(int cpu, int irqType, int irqTriggerType) {
		cpus[cpu].cpu.interrupt(irqType, true);
	}
	
	protected void cpu_cause_interrupt(int cpu, int irqType) {
		cpus[cpu].cpu.interrupt(irqType, true);
	}

	public Machine getMachineInfo(String fname) {
		romLoader.noLoading();
		bInfo = true;
		return getMachine(null, fname);
	}

	@Override
	public Machine getMachine(URL url, String fname) {
		base_URL = url;
		DriverName = fname;

		cur_region = 0;
		inp_count = 0;
		gdi_count = 0;
		cpu_count = 0;

		for (int i = 0; i < 10; i++)
			inps[i] = new InputPort();
		input_port_0_r = inps[0];
		input_port_1_r = inps[1];
		input_port_2_r = inps[2];
		input_port_3_r = inps[3];
		input_port_4_r = inps[4];
		input_port_5_r = inps[5];
		input_port_6_r = inps[6];
		input_port_7_r = inps[7];
		input_port_8_r = inps[8];
		input_port_9_r = inps[9];
		return null;
	}

	@Override
	public int getProperty(int property) {
		return properties[property];
	}

	@Override
	public void setProperty(int property, int value) {
		properties[property] = value;
	}

	@Override
	public String getDriverInfo() {
		return "AbstractDriver";
	}

	@Override
	public String getGameInfo() {
		return "AbstractDriver supports nothing at all ;o)";
	}

	protected void cpu_setbank(int b, int address) {
		mra[b - 1].setBankAddress(b, address);
	}

	protected void Helpers(int name, int valname, int size, int valsize) {
		switch (name) {
			case videoram :
				Fvideoram = valname;
				break;
			case colorram :
				Fcolorram = valname;
				break;
			case spriteram :
				Fspriteram = valname;
				break;
			case spriteram_2 :
				Fspriteram_2 = valname;
				break;
			case spriteram_3 :
				Fspriteram_3 = valname;
				break;
			case paletteram :
				Fpaletteram = valname;
				break;
			case paletteram_2 :
				Fpaletteram_2 = valname;
				break;
		}
		switch (size) {
			case videoram_size :
				Fvideoram_size = valsize;
				break;
			case spriteram_size :
				Fspriteram_size = valsize;
				break;
			case spriteram_2_size :
				Fspriteram_2_size = valsize;
				break;
			case spriteram_3_size :
				Fspriteram_3_size = valsize;
				break;
		}
	}
	protected void	install_mem_read_handler(int cpu, int from, int until, ReadHandler memRead) {
		mra[cpu].set(from, until, memRead);
	}
	protected void MR_START() {
		mra[mra_count++] = new MemoryReadAddress(REGIONS[cur_region - 1]);
	}

	protected void MR_ADD(int from, int until, ReadHandler memRead) {
		mra[mra_count - 1].set(from, until, memRead);
	}

	protected void MR_START(int from, int until, ReadHandler memRead) {
		MR_START();
		MR_ADD(from, until, memRead);
	}

	protected void MR_ADD(int from, int until, int type) {
		mra[mra_count - 1].setMR(from, until, type);
	}

	protected void MR_START(int from, int until, int type) {
		MR_START();
		MR_ADD(from, until, type);
	}

	protected void MW_START() {
		mwa[mwa_count++] = new MemoryWriteAddress(REGIONS[cur_region - 1]);
	}

	protected void MW_ADD(int from, int until, WriteHandler memWrite) {
		mwa[mwa_count - 1].set(from, until, memWrite);
	}

	protected void MW_START(int from, int until, WriteHandler memWrite) {
		MW_START();
		MW_ADD(from, until, memWrite);
	}

	protected void MW_ADD(int from, int until, WriteHandler memWrite, int helper, int helper_size) {
		mwa[mwa_count - 1].set(from, until, memWrite);
		Helpers(helper, from, helper_size, until + 1 - from);
	}

	protected void MW_START(int from, int until, WriteHandler memWrite, int helper, int helper_size) {
		MW_START();
		MW_ADD(from, until, memWrite, helper, helper_size);
	}

	protected void MW_ADD(int from, int until, WriteHandler memWrite, int helper) {
		mwa[mwa_count - 1].set(from, until, memWrite);
		Helpers(helper, from, -1, 0);
	}

	protected void MW_ADD(int from, int until, WriteHandler memWrite, int[] helper) {
		mwa[mwa_count - 1].set(from, until, memWrite);
		helper[0] = from;
	}

	protected void MW_ADD(int from, int until, int type, int[] helper, int[] helper_size) {
		mwa[mwa_count - 1].setMW(from, until, type);
		helper[0] = from;
		helper_size[0] = until + 1 - from;
	}

	protected void MW_ADD(
		int from,
		int until,
		WriteHandler memWrite,
		int[] helper,
		int[] helper_size) {
		mwa[mwa_count - 1].set(from, until, memWrite);
		helper[0] = from;
		helper_size[0] = until + 1 - from;
	}

	protected void MW_START(int from, int until, WriteHandler memWrite, int helper) {
		MW_START();
		MW_ADD(from, until, memWrite, helper);
	}

	protected void MW_ADD(int from, int until, int type) {
		mwa[mwa_count - 1].setMW(from, until, type);
	}

	protected void MW_START(int from, int until, int type) {
		MW_START();
		MW_ADD(from, until, type);
	}

	protected void MW_ADD(int from, int until, int type, int helper) {
		mwa[mwa_count - 1].setMW(from, until, type);
		Helpers(helper, from, -1, 0);
	}

	protected void MW_ADD(int from, int until, int type, int[] helper) {
		mwa[mwa_count - 1].setMW(from, until, type);
		helper[0] = from;
	}

	protected void MW_START(int from, int until, int type, int helper) {
		MW_START();
		MW_ADD(from, until, type, helper);
	}

	protected void MW_ADD(int from, int until, int type, int helper, int helper_size) {
		mwa[mwa_count - 1].setMW(from, until, type);
		Helpers(helper, from, helper_size, until + 1 - from);
	}

	protected void MW_START(int from, int until, int type, int helper, int helper_size) {
		MW_START();
		MW_ADD(from, until, type, helper, helper_size);
	}

	protected void PR_START() {
		ior[ior_count] = null;
		ior[ior_count++] = new IOReadPort();
	}

	protected void PR_ADD(int from, int until, ReadHandler memRead) {
		if (ior[ior_count - 1]!=null) ior[ior_count - 1].set(from, until, memRead);
	}

	protected void PR_START(int from, int until, ReadHandler memRead) {
		PR_START();
		PR_ADD(from, until, memRead);
	}

	protected void PW_START() {
		iow[iow_count] = null;
		iow[iow_count++] = new IOWritePort();
	}

	protected void PW_ADD(int from, int until, WriteHandler memWrite) {
		if (iow[iow_count-1]!=null) iow[iow_count - 1].set(from, until, memWrite);
	}

	protected void PW_START(int from, int until, WriteHandler memWrite) {
		PW_START();
		PW_ADD(from, until, memWrite);
	}

	protected void GDI_ADD(int mem, int offset, GfxLayout gfx, int colorOffset, int numberOfColors) {
		gdis[gdi_count] = new GfxDecodeInfo(REGIONS[mem], offset, gfx, colorOffset, numberOfColors);
		gdi_count++;
	}

	protected void GDI_ADD(int mem, int offset, int[][] gfx, int colorOffset, int numberOfColors) {
		gdis[gdi_count] = new GfxDecodeInfo(REGIONS[mem], offset, gfx, colorOffset, numberOfColors);
		gdi_count++;
	}

	protected void GDI_ADD(int eofarray) {
	}

	protected void MDRV_CPU_ADD(Cpu cpu, int frq) {
		ior[cpu_count] = new IOReadPort();
		iow[cpu_count] = new IOWritePort();

		cpus[cpu_count] =
			new CpuDriver(cpu, frq, null, null, ior[cpu_count], iow[cpu_count], null, 0);
		cpu_count++;

		cur_region++;
	}
	
	protected void MDRV_VIDEO_EOF(Eof_callback eof_callback) {
		mdalloc();
		md.eof_callback = eof_callback;
	}
	
	protected void MDRV_CPU_ADD(int type, int frq) {
		Cpu cpu = null;

		switch (type) {
			case Z80 :
				cpu = new Z80();
				break;
			case I8080 :
				cpu = new I8080();
				break;
			case M6809 :
				cpu = new M6809();
				break;
			/*case M68000 :
				cpu = new M68k();
				break;*/
		}

		MDRV_CPU_ADD(cpu, frq);
	}

	protected void MDRV_CPU_ADD_TAG(String tag, int type, int frq) {
		Cpu cpu = null;

		switch (type) {
			case 0 :
				cpu = new Z80();
				break;
			case 1 :
				cpu = new I8080();
				break;
			case 2 :
				cpu = new M6809();
				break;
			/*case M68000 :
				cpu = new M68k();
				break;*/
		}

		cpu.setTag(tag);
		MDRV_CPU_ADD(cpu, frq);
	}

	protected void MDRV_CPU_MODIFY(String tag) {
		int i;

		bModify = false;
		cpu_num = -1;
		i = 0;
		while ((cpu_num == -1) && (i < cpu_count)) {
			if (cpus[i].cpu.getTag().compareTo(tag) == 0)
				cpu_num = i;
		}
		if (cpu_num != -1)
			bModify = true;
	}

	protected void MDRV_CPU_FLAGS(int flags) {
		cpus[cpu_count - 1].isAudioCpu = ((flags & CPU_AUDIO_CPU) != 0);
	}

	protected void MDRV_SOUND_ADD(SoundChipEmulator soundChip) {
		soundChips[snd_count] = soundChip;
		snd_count++;
	}

	protected void MDRV_CPU_MEMORY(boolean mread, boolean mwrite) {
		cpus[cpu_count - 1].mra = mra[cpu_count - 1];
		cpus[cpu_count - 1].mwa = mwa[cpu_count - 1];
	}

	protected void MDRV_CPU_PORTS(boolean ioread, boolean iowrite) {
		cpus[cpu_count - 1].ior = ior[ior_count - 1];
		cpus[cpu_count - 1].iow = iow[iow_count - 1];
	}

	protected void MDRV_CPU_PORTS(int ioread, boolean iowrite) {
		cpus[cpu_count - 1].iow = iow[iow_count - 1];
	}

	protected void MDRV_CPU_PORTS(boolean ioread, int iowrite) {
		cpus[cpu_count - 1].ior = ior[ior_count - 1];
	}

	protected void MDRV_CPU_VBLANK_INT(InterruptHandler irh, int ipf) {
		if (bModify) {
			cpus[cpu_num].irh = irh;
			cpus[cpu_num].ipf = ipf;
		} else {
			cpus[cpu_count - 1].irh = irh;
			cpus[cpu_count - 1].ipf = ipf;
		}
	}

	protected void setVideoEmulator(VideoEmulator ve) {
		this.videoEmulator = ve;
	}

	protected void mdalloc() {
		if (md == null) {

			CpuDriver[] cpuDriver = new CpuDriver[cpu_count];
			for (int i = 0; i < cpu_count; i++)
				cpuDriver[i] = cpus[i];

			SoundChipEmulator[] sce = new SoundChipEmulator[snd_count];
			for (int i = 0; i < snd_count; i++)
				sce[i] = soundChips[i];

			if (!bInfo) {
				System.out.println("cpus :" + cpuDriver.length);
				System.out.println("sndchips :" + sce.length);
			}

			md =
				new MachineDriver(
					cpuDriver,
					0,
					0,
					0,
					NOP,
					0,
					0,
					null,
					null,
					0,
					0,
					null,
					null,
					0,
					null,
					null,
					null,
					null,
					sce);

			/* Update RAM addresses */
			if (Fvideoram != -1)
				cottage.mame.MAMEVideo.videoram = Fvideoram;
			if (Fcolorram != -1)
				cottage.mame.MAMEVideo.colorram = Fcolorram;
			if (Fspriteram != -1)
				cottage.mame.MAMEVideo.spriteram = Fspriteram;
			if (Fspriteram_2 != -1)
				cottage.mame.MAMEVideo.spriteram_2 = Fspriteram_2;
			if (Fspriteram_3 != -1)
				cottage.mame.MAMEVideo.spriteram_3 = Fspriteram_3;
			if (Fpaletteram != -1)
				cottage.mame.MAMEVideo.paletteram = Fpaletteram;
			if (Fpaletteram_2 != -1)
				cottage.mame.MAMEVideo.paletteram_2 = Fpaletteram_2;

			/* Update RAM sizes */
			if (Fvideoram_size != -1)
				cottage.mame.MAMEVideo.videoram_size = Fvideoram_size;
			if (Fspriteram_size != -1)
				cottage.mame.MAMEVideo.spriteram_size = Fspriteram_size;
			if (Fspriteram_2_size != -1)
				cottage.mame.MAMEVideo.spriteram_2_size = Fspriteram_2_size;
			if (Fspriteram_3_size != -1)
				cottage.mame.MAMEVideo.spriteram_3_size = Fspriteram_3_size;

			md.REGIONS = REGIONS;
		}
	}

	protected void MDRV_IMPORT_FROM(boolean imp) {
	}

	protected void MDRV_INTERLEAVE(int spf) {
		mdalloc();
		md.spf = spf;
	}

	protected void MDRV_FRAMES_PER_SECOND(int fps) {
		mdalloc();
		md.fps = fps;
	}

	protected void MDRV_FRAMES_PER_SECOND(double fps) {
		mdalloc();
		md.fps = (int) fps;
	}

	protected void MDRV_VBLANK_DURATION(int vbd) {
		mdalloc();
		md.vbd = vbd;
	}

	protected void MDRV_VIDEO_ATTRIBUTES(int videoFlags) {
		mdalloc();
		md.videoFlags = videoFlags;
	}

	protected void MDRV_SCREEN_SIZE(int w, int h) {
		mdalloc();
		md.w = w;
		md.h = h;
	}

	protected void MDRV_VISIBLE_AREA(int x1, int y1, int x2, int y2) {
		mdalloc();
		int[] visible = new int[4];
		visible[0] = x1;
		visible[1] = y1;
		visible[2] = x2;
		visible[3] = y2;
		md.visible = visible;
	}

	protected void MDRV_GFXDECODE(boolean gfxdec) {
		mdalloc();
		GfxDecodeInfo[] gfx = new GfxDecodeInfo[gdi_count];
		for (int i = 0; i < gdi_count; i++)
			gfx[i] = gdis[i];
		md.gfx = gfx;
	}

	protected void MDRV_PALETTE_LENGTH(int pal) {
		mdalloc();
		md.pal = pal;
		md.col = pal;
	}

	protected void MDRV_COLORTABLE_LENGTH(int col) {
		mdalloc();
		md.col = col;
	}

	protected void MDRV_PALETTE_INIT(Vh_convert_color_proms initProms) {
		mdalloc();
		md.initProms = initProms;
	}

	protected void MDRV_VIDEO_START(Vh_start vh_start) {
		mdalloc();
		md.vh_start = vh_start;
	}

	protected void MDRV_VIDEO_UPDATE(Vh_refresh vh_screenrefresh) {
		mdalloc();
		md.vh_screenrefresh = vh_screenrefresh;
	}

	protected String DEF_STR2(int number) {
		return DEF_STR[number];
	}

	protected void PORT_START() {
		inp_count++;
	}

	protected void PORT_ANALOG(int bitMask, int center, int type, int a, int b, int c, int d) {
		inps[inp_count - 1].setAnalog(bitMask, center, type, a, b, c, d);
	}

	protected void PORT_BIT(int bitMask, int activityType, int inputType) {
		inps[inp_count - 1].setBit(bitMask, activityType, inputType);
	}

	protected void PORT_BITX(int bitMask, int defSetting, int c, String name, int d, int e) {
		inps[inp_count - 1].setDipName(bitMask, defSetting, name);
	}

	protected void PORT_BIT_IMPULSE(int bitMask, int activityType, int inputType, int frames) {
		inps[inp_count - 1].setBitImpulse(bitMask, activityType, inputType, frames);
	}

	protected void PORT_DIPNAME(int bitMask, int defSetting, String name) {
		inps[inp_count - 1].setDipName(bitMask, defSetting, name);
	}

	protected void PORT_DIPSETTING(int setting, String name) {
		inps[inp_count - 1].setDipSetting(setting, name);
	}

	protected void PORT_SERVICE(int bitMask, int activityType) {
		inps[inp_count - 1].setService(bitMask, activityType);
	}

	protected void ROM_LOAD(String name, int offset, int length, int crc) {
		romLoader.loadROM(name, offset, length, crc);
	}

	protected void ROM_LOAD16_BYTE(String name, int offset, int length, int crc) {
		romLoader.loadROM(name, offset, length, crc, 1);
	}

	protected void ROM_CONTINUE(int offset, int length) {
		romLoader.continueROM(offset, length);
	}

	protected void ROM_REGION(int size, int mem, int options) {
		if (REGIONS[mem] == null)
			REGIONS[mem] = new int[size];
		romLoader.setMemory(REGIONS[mem]);
		romLoader.setInvert((options & ROMREGION_INVERT) != 0);
	}

	public int[] memory_region(int region) {
		return REGIONS[region];
	}

	public int memory_region_length(int region) {
		return REGIONS[region].length;
	}

	protected int BADCRC(int crc) {
		return crc;
	}

	protected void GAME(
		int year,
		boolean roms,
		InputPort[] inp,
		boolean ini,
		int rot,
		String man,
		String nam) {
		if (!bInfo) {
			System.out.println("Starting...");
			System.out.println(nam);
			System.out.println(Integer.toString(year) + " - " + man);
			md.ROT = rot;
			md.input = inp;
			md.ve = this.videoEmulator;
		} else {
			this.bInfo = true;
			md.info = bInfo;
			this.driver_date = Integer.toString(year);
			this.driver_prod = man;
			this.driver_name = nam;
			this.driver_clone = null;
			this.driver_sound = (md.soundChips != noSound) ? true : false;
			md.ROT = rot;
		}
	}

	protected void GAME(
		int year,
		boolean roms,
		String parent,
		InputPort[] inp,
		boolean ini,
		int rot,
		String man,
		String nam) {
		if (!bInfo) {
			System.out.println("Starting...");
			System.out.println(nam);
			System.out.println(Integer.toString(year) + " - " + man);
			md.ROT = rot;
			md.input = inp;
			md.ve = this.videoEmulator;
		} else {
			this.bInfo = true;
			md.info = bInfo;
			this.driver_date = Integer.toString(year);
			this.driver_prod = man;
			this.driver_name = nam;
			this.driver_clone = parent;
			this.driver_sound = (md.soundChips != noSound) ? true : false;
			md.ROT = rot;
		}
	}

	protected void GAME(
		int year,
		boolean roms,
		int parent,
		boolean mdrv,
		boolean inp,
		boolean ini,
		int rot,
		String man,
		String nam) {
		GAME(Integer.toString(year), roms, null, mdrv, inp, ini, rot, man, nam);
	}

	protected void GAME(
		int year,
		boolean roms,
		int parent,
		boolean mdrv,
		boolean inp,
		int ini,
		int rot,
		String man,
		String nam) {
		GAME(Integer.toString(year), roms, null, mdrv, inp, true, rot, man, nam);
	}

	protected void GAME(
		int year,
		boolean roms,
		int parent,
		boolean mdrv,
		boolean inp,
		InitHandler ini,
		int rot,
		String man,
		String nam) {
		GAME(Integer.toString(year), roms, null, mdrv, inp, ini, rot, man, nam);
	}

	protected void GAME(
		String year,
		boolean roms,
		int parent,
		boolean mdrv,
		boolean inp,
		boolean ini,
		int rot,
		String man,
		String nam) {
		GAME(year, roms, null, mdrv, inp, ini, rot, man, nam);
	}

	protected void GAME(
		String year,
		boolean roms,
		int parent,
		boolean mdrv,
		boolean inp,
		int ini,
		int rot,
		String man,
		String nam) {
		GAME(year, roms, null, mdrv, inp, true, rot, man, nam);
	}

	protected void GAME(
		int year,
		boolean roms,
		String parent,
		boolean mdrv,
		boolean inp,
		boolean ini,
		int rot,
		String man,
		String nam) {
		GAME(Integer.toString(year), roms, parent, mdrv, inp, ini, rot, man, nam);
	}

	protected void GAME(
		int year,
		boolean roms,
		String parent,
		boolean mdrv,
		boolean inp,
		int ini,
		int rot,
		String man,
		String nam) {
		GAME(Integer.toString(year), roms, parent, mdrv, inp, true, rot, man, nam);
	}

	protected void GAME(
		int year,
		boolean roms,
		String parent,
		boolean mdrv,
		boolean inp,
		InitHandler ini,
		int rot,
		String man,
		String nam) {
		GAME(Integer.toString(year), roms, parent, mdrv, inp, ini, rot, man, nam);
	}

	protected void GAME(
		String year,
		boolean roms,
		String parent,
		boolean mdrv,
		boolean inp,
		int ini,
		int rot,
		String man,
		String nam) {
		GAME(year, roms, parent, mdrv, inp, true, rot, man, nam);
	}

	protected void GAME(
		String year,
		boolean roms,
		String parent,
		boolean mdrv,
		boolean inp,
		InitHandler ini,
		int rot,
		String man,
		String nam) {
		GAME(year, roms, parent, mdrv, inp, true, rot, man, nam);
		ini.init();
	}

	protected void GAME(
		String year,
		boolean roms,
		String parent,
		boolean mdrv,
		boolean inp,
		boolean ini,
		int rot,
		String man,
		String nam) {
		int i;

		if (!bInfo) {
			/* Load ROMs */
			romLoader.setZip(DriverName);
			romLoader.setParentZip(parent);
			romLoader.loadZip(base_URL);

			System.out.println("Starting...");
			System.out.println(nam);
			System.out.println(year + " - " + man);
			md.ROT = rot;

			InputPort[] in = new InputPort[inp_count];
			/* Add selected ports */
			for (i = 0; i < inp_count; i++)
				in[i] = inps[i];
			/* Free other ports */
			for (i = inp_count; i < 10; i++)
				inps[i] = null;
			md.input = in;
			md.ve = this.videoEmulator;

		} else {
			this.bInfo = true;
			md.info = bInfo;
			this.driver_date = year;
			this.driver_prod = man;
			this.driver_name = nam;
			this.driver_clone = parent;
			this.driver_sound = (snd_count > 0) ? true : false;
			md.ROT = rot;
		}
	}
}