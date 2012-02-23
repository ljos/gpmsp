/*
 * Created on 14-aug-2005
 * 
 * To change the template for this generated file go to Window - Preferences -
 * Java - Code Generation - Code and Comments
 */
package jef.sound.chip;

import jef.map.ReadHandler;
import jef.map.WriteHandler;
import jef.sound.SoundChip;
import jef.sound.SoundChipEmulator;
import jef.sound.chip.fm.FMChan;
import jef.sound.chip.fm.FMConstants;
import jef.sound.chip.fm.FMIRQHandler;
import jef.sound.chip.fm.FMOpn;
import jef.sound.chip.fm.FMState;
import jef.sound.chip.fm.FMTimerHandler;
import jef.util.INT32;

/**
 * @author Erik Duijs
 * 
 * To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Generation - Code and Comments
 */
public class YM2203 extends SoundChip implements SoundChipEmulator, FMConstants {

	private int YM2203NumChips;
	private int baseClock;
	
	private YM2203_f[] FM2203;
	public static AY8910 SSG; 
	private FMTimerHandler timeHandler;
	private FMIRQHandler irqHandler;
	
	private static int[] TL_TABLE;
	
	/* runtime work */
	public static INT32[] out_ch = new INT32[4];		/* channel output NONE,LEFT,RIGHT or CENTER */
	public static INT32 pg_in1 = new INT32();
	public static INT32 pg_in2 = new INT32();
	public static INT32 pg_in3 = new INT32();
	public static INT32 pg_in4 = new INT32();	/* PG input of SLOTs */
	
	private static int[] SIN_TABLE = new int[SIN_ENT];
	public static int[] ENV_CURVE = new int[3*EG_ENT+1];
	public static int[] DRAR_TABLE = new int[EG_ENT];
	
	private static FMState  State;			/* basic status */
	private static FMChan[]  cch = new FMChan[8];			/* pointer of FM channels */
	
	public static long lfo_amd;
	

	public static int SIN_TABLE(int i, int j) {
		return TL_TABLE[SIN_TABLE[i] + j];
	}
	
	/* here's the virtual YM2203(OPN) */
	private final class YM2203_f {
		FMOpn OPN = new FMOpn(irqHandler); /* OPN state */
		FMChan[] CH = new FMChan[3]; /* channel state */
		public YM2203_f() {
			for (int i = 0; i < 3; i++) {
				CH[i] = new FMChan();
			}
		}
	};

	/**
	 * Constructor
	 */
	public YM2203(int numChips, int clock, FMTimerHandler th, FMIRQHandler ih) {
		this.YM2203NumChips = numChips;
		this.baseClock = clock;
		this.timeHandler = th;
		this.irqHandler = ih;
		SSG = new AY8910(numChips, clock);
		for (int i = 0; i < 4; i++) {
			out_ch[i] = new INT32();
		}
	}
	
	@Override
	public void init(boolean useJavaxSound, int sampRate, int buflen, int fps) {
		super.init(useJavaxSound, sampRate, buflen, fps);
		
		SSG.init(useJavaxSound, sampRate, buflen, fps);

		YM2203Init(YM2203NumChips, baseClock, sampRate, timeHandler, irqHandler);
	}
	/* ---------- Initialize YM2203 emulator(s) ---------- */
	/* 'num' is the number of virtual YM2203's to allocate */
	/* 'rate' is sampling rate and 'bufsiz' is the size of the */
	/* buffer that should be updated at each interval */
	private void YM2203Init(
		int num,
		int clock,
		int rate,
		FMTimerHandler TimerHandler,
		FMIRQHandler IRQHandler) {

		YM2203NumChips = num;

		FMInitTable();

		FM2203 = new YM2203_f[YM2203NumChips];
		for (int i = 0; i < YM2203NumChips; i++) {
			FM2203[i] = new YM2203_f();
			FM2203[i].OPN.ST.index = i;
			FM2203[i].OPN.type = FMOpn.TYPE_YM2203;
			FM2203[i].OPN.P_CH = FM2203[i].CH;
			FM2203[i].OPN.ST.clock = clock;
			FM2203[i].OPN.ST.rate = rate;
			/* FM2203[i].OPN.ST.irq = 0; */
			/* FM2203[i].OPN.ST.satus = 0; */
			FM2203[i].OPN.ST.timermodel = FMState.FM_TIMER_INTERVAL;
			/* Extend handler */
			FM2203[i].OPN.ST.Timer_Handler = TimerHandler;
			FM2203[i].OPN.ST.IRQ_Handler = IRQHandler;
			SSG.AY8910_init("AY-3-8910", i, baseClock, 50, super.getSampFreq(), 0, 0, 0, 0);
			SSG.build_mixer_table(i);
			YM2203ResetChip(i);
		}
	}

	/**
	 * @param i
	 */
	private void YM2203ResetChip(int num) {
		int i;
		FMOpn OPN = (FM2203[num].OPN);

		/* Reset Priscaler */
		OPN.setPris(6 * 12, 6 * 12, 4); /* 1/6 , 1/4 */
		/* reset SSG section */
		SSG.AY8910_reset(OPN.ST.index);
		/* status clear */
		OPN.ST.FM_IRQMASK_SET(0x03);
		OPN.OPNWriteMode(0x27, 0x30); /* mode 0 , timer reset */
		reset_channel(OPN.ST, FM2203[num].CH, 3);
		/* reset OPerator paramater */
		for (i = 0xb6; i >= 0xb4; i--)
			OPN.OPNWriteReg(i, 0xc0); /* PAN RESET */
		for (i = 0xb2; i >= 0x30; i--)
			OPN.OPNWriteReg(i, 0);
		for (i = 0x26; i >= 0x20; i--)
			OPN.OPNWriteReg(i, 0);
	}

	/**
	 * @param state
	 * @param chans
	 * @param i
	 */
	private void reset_channel(FMState ST, FMChan[] CH, int chan) {
			int c,s;

			ST.mode   = 0;	/* normal mode */
			ST.FM_STATUS_RESET(0xff);
			ST.TA     = 0;
			ST.TAC    = 0;
			ST.TB     = 0;
			ST.TBC    = 0;

			for( c = 0 ; c < chan ; c++ )
			{
				CH[c].fc = 0;
				CH[c].PAN = OUTD_CENTER;
				for(s = 0 ; s < 4 ; s++ )
				{
					CH[c].SLOT[s].SEG = 0;
					CH[c].SLOT[s].eg_next= FM_EG_RELEASE;
					CH[c].SLOT[s].evc = EG_OFF;
					CH[c].SLOT[s].eve = EG_OFF+1;
					CH[c].SLOT[s].evs = 0;
				}
			}
	}
	/**
	 * @param ch
	 */
	public static void setup_connection(FMChan CH) {
		INT32 carrier = out_ch[CH.PAN]; /* NONE,LEFT,RIGHT or CENTER */

		switch( CH.ALGO ){
		case 0:
			/*  PG---S1---S2---S3---S4---OUT */
			CH.connect1 = pg_in2;
			CH.connect2 = pg_in3;
			CH.connect3 = pg_in4;
			break;
		case 1:
			/*  PG---S1-+-S3---S4---OUT */
			/*  PG---S2-+               */
			CH.connect1 = pg_in3;
			CH.connect2 = pg_in3;
			CH.connect3 = pg_in4;
			break;
		case 2:
			/* PG---S1------+-S4---OUT */
			/* PG---S2---S3-+          */
			CH.connect1 = pg_in4;
			CH.connect2 = pg_in3;
			CH.connect3 = pg_in4;
			break;
		case 3:
			/* PG---S1---S2-+-S4---OUT */
			/* PG---S3------+          */
			CH.connect1 = pg_in2;
			CH.connect2 = pg_in4;
			CH.connect3 = pg_in4;
			break;
		case 4:
			/* PG---S1---S2-+--OUT */
			/* PG---S3---S4-+      */
			CH.connect1 = pg_in2;
			CH.connect2 = carrier;
			CH.connect3 = pg_in4;
			break;
		case 5:
			/*         +-S2-+     */
			/* PG---S1-+-S3-+-OUT */
			/*         +-S4-+     */
			CH.connect1 = null;	/* special case */
			CH.connect2 = carrier;
			CH.connect3 = carrier;
			break;
		case 6:
			/* PG---S1---S2-+     */
			/* PG--------S3-+-OUT */
			/* PG--------S4-+     */
			CH.connect1 = pg_in2;
			CH.connect2 = carrier;
			CH.connect3 = carrier;
			break;
		case 7:
			/* PG---S1-+     */
			/* PG---S2-+-OUT */
			/* PG---S3-+     */
			/* PG---S4-+     */
			CH.connect1 = carrier;
			CH.connect2 = carrier;
			CH.connect3 = carrier;
		}
		CH.connect4 = carrier;
	}
	/**
	 *  
	 */
	private void FMInitTable() {
		int s,t;
		double rate;
		int i,j;
		double pom;

		/* allocate total level table plus+minus section */
		TL_TABLE = new int[2*TL_MAX];
		/* make total level table */
		for (t = 0;t < TL_MAX ;t++){
			if(t >= PG_CUT_OFF)
				rate = 0;	/* under cut off area */
			else
				rate = ((1<<TL_BITS)-1)/Math.pow(10,EG_STEP*t/20);	/* dB . voltage */
			TL_TABLE[       t] =  (int)rate;
			TL_TABLE[TL_MAX+t] = -TL_TABLE[t];
			/*		Log(LOG_INF,"TotalLevel(%3d) = %x\n",t,TL_TABLE[t]);*/
		}

		/* make sinwave table (total level offet) */

		for (s = 1;s <= SIN_ENT/4;s++){
			pom = Math.sin(2.0*Math.PI*s/SIN_ENT); /* sin   */
			pom = 20*log10(1/pom);	     /* . decibel */
			j = (int) (pom / EG_STEP);    /* TL_TABLE steps */
			/* cut off check */
			if(j > PG_CUT_OFF)
				j = PG_CUT_OFF;
			/* degree 0   -  90    , degree 180 -  90 : plus section */
			SIN_TABLE[          s] = SIN_TABLE[SIN_ENT/2-s] = j;
			/* degree 180 - 270    , degree 360 - 270 : minus section */
			SIN_TABLE[SIN_ENT/2+s] = SIN_TABLE[SIN_ENT  -s] = TL_MAX+j;
			/* Log(LOG_INF,"sin(%3d) = %f:%f db\n",s,pom,(double)j * EG_STEP); */
		}
		/* degree 0 = degree 180                   = off */
		SIN_TABLE[0] = SIN_TABLE[SIN_ENT/2]        = PG_CUT_OFF;

		/* envelope counter . envelope output table */
		for (i=0; i<EG_ENT; i++)
		{
			/* ATTACK curve */
			/* !!!!! preliminary !!!!! */
			pom = Math.pow( ((double)(EG_ENT-1-i)/EG_ENT) , 8 ) * EG_ENT;
			/* if( pom >= EG_ENT ) pom = EG_ENT-1; */
			ENV_CURVE[i] = (int)pom;
			/* DECAY ,RELEASE curve */
			ENV_CURVE[(EG_DST>>ENV_BITS)+i]= i;
			//#if FM_SEG_SUPPORT
			/* DECAY UPSIDE (SSG ENV) */
			ENV_CURVE[(EG_UST>>ENV_BITS)+i]= EG_ENT-1-i;
			//#endif
		}
		/* off */
		ENV_CURVE[EG_OFF>>ENV_BITS]= EG_ENT-1;

		/* decay to reattack envelope converttable */
		j = EG_ENT-1;
		for (i=0; i<EG_ENT; i++)
		{
			while( (j!=0) && (ENV_CURVE[j] < i) ) j--;
			DRAR_TABLE[i] = j<<ENV_BITS;
			/* Log(LOG_INF,"DR %06X = %06X,AR=%06X\n",i,DRAR_TABLE[i],ENV_CURVE[DRAR_TABLE[i]>>ENV_BITS] ); */
		}

	}
	
	/* ---------- update one of chip ----------- */
	private void YM2203UpdateOne(int num, int length)
	{
		YM2203_f F2203 = (FM2203[num]);
		FMOpn OPN =   (FM2203[num].OPN);
		int i;
		State = F2203.OPN.ST;
		cch[0]   = F2203.CH[0];
		cch[1]   = F2203.CH[1];
		cch[2]   = F2203.CH[2];
		//#if FM_LFO_SUPPORT
		/* LFO */
		//lfo_amd = lfo_pmd = 0;
		//#endif
		/* frequency counter channel A */
		cch[0].CALC_FCOUNT();
		/* frequency counter channel B */
		cch[1].CALC_FCOUNT();
		/* frequency counter channel C */
		if( (State.mode & 0xc0) != 0 ){
			/* 3SLOT MODE */
			if( cch[2].SLOT[SLOT1].Incr==-1){
				/* 3 slot mode */
				cch[2].SLOT[SLOT1].CALC_FCSLOT(OPN.SL3.fc[1] , OPN.SL3.kcode[1] );
				cch[2].SLOT[SLOT2].CALC_FCSLOT(OPN.SL3.fc[2] , OPN.SL3.kcode[2] );
				cch[2].SLOT[SLOT3].CALC_FCSLOT(OPN.SL3.fc[0] , OPN.SL3.kcode[0] );
				cch[2].SLOT[SLOT4].CALC_FCSLOT(cch[2].fc , cch[2].kcode );
			}
		} else  cch[2].CALC_FCOUNT();

		for( i=0; i < length ; i++ )
		{
			/*            channel A         channel B         channel C      */
			out_ch[OUTD_CENTER].value = 0;
			/* calcrate FM */
			for( int _ch=0 ; _ch <= 2 ; _ch++)
				cch[_ch].FM_CALC_CH();
			/* limit check */
			//Limit( out_ch[OUTD_CENTER] , FM_MAXOUT, FM_MINOUT );
			out_ch[OUTD_CENTER].limit(FM_MINOUT,FM_MAXOUT);
			/* store to sound buffer */
			writeLinBuffer(i, readLinBuffer(i) + ((out_ch[OUTD_CENTER].value >> FM_OUTSB)/YM2203NumChips)/2);
			//buf[i] = out_ch[OUTD_CENTER].value >> FM_OUTSB;
			/* timer controll */
			State.INTERNAL_TIMER_A(cch[2] );
		}
		State.INTERNAL_TIMER_B(length);
	}
	
	/* ---------- YM2203 I/O interface ---------- */
	int YM2203Write(int n,int a,int v)
	{
		FMOpn OPN = FM2203[n].OPN;

		if ( (a & 1) == 0 )
		{	/* address port */
			OPN.ST.address = v & 0xff;
			/* Write register to SSG emurator */
			if( v < 16 ) SSG.AY8910Write(n,0,v);
			switch(OPN.ST.address)
			{
			case 0x2d:	/* divider sel */
				OPN.setPris( 6*12, 6*12 ,4); /* OPN 1/6 , SSG 1/4 */
				break;
			case 0x2e:	/* divider sel */
				OPN.setPris( 3*12, 3*12,2); /* OPN 1/3 , SSG 1/2 */
				break;
			case 0x2f:	/* divider sel */
				OPN.setPris( 2*12, 2*12,1); /* OPN 1/2 , SSG 1/1 */
				break;
			}
		}
		else
		{	/* data port */
			int addr = OPN.ST.address;
			switch( addr & 0xf0 )
			{
			case 0x00:	/* 0x00-0x0f : SSG section */
				/* Write data to SSG emurator */
				SSG.AY8910Write(n,a,v);
				break;
			case 0x20:	/* 0x20-0x2f : Mode section */
				YM2203UpdateReq(n);
				/* write register */
				OPN.OPNWriteMode(addr,v);
				break;
			default:	/* 0x30-0xff : OPN section */
				YM2203UpdateReq(n);
				/* write register */
				OPN.OPNWriteReg(addr,v);
			}
		}
		return OPN.ST.irq;
	}
	
	int YM2203Read(int n,int a)
	{
		YM2203_f F2203 = (FM2203[n]);
		int addr = F2203.OPN.ST.address;
		int ret = 0;

		if( (a&1)==0 )
		{	/* status port */
			ret = F2203.OPN.ST.status;
            //System.out.println("readstatus " + n);
		}
		else
		{	/* data port (ONLY SSG) */
			if( addr < 16 ) {
                
                ret = SSG.AY8910Read(n);
                //System.out.println("readdata " + n);
            }
		}
        //System.out.println("ret=" + ret);
		return ret;
	}
	
	/**
	 * @param n
	 */
	private void YM2203UpdateReq(int n) {
		// TODO Auto-generated method stub
	}

	/**
	 * @param d
	 * @return
	 */
	private double log10(double n) {
		return Math.log(n) / Math.log(10);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jef.sound.SoundChip#writeBuffer()
	 */
	@Override
	public void writeBuffer() {
		SSG.writeBuffer();
		SSG.update();
		clearBuffer();
		int length = super.getBufferLength();
		//System.out.print("~");
		for (int i = 0; i < YM2203NumChips; i++) {
			YM2203UpdateOne(i,length);
		}
		
	}
	
	public ReadHandler ym2203_status_port_0_r() { return new YM2203_status_port_r(0); }
	public ReadHandler ym2203_status_port_1_r() { return new YM2203_status_port_r(1); }
	public ReadHandler ym2203_status_port_2_r() { return new YM2203_status_port_r(2); }
	public ReadHandler ym2203_status_port_3_r() { return new YM2203_status_port_r(3); }
	public ReadHandler ym2203_status_port_4_r() { return new YM2203_status_port_r(4); }
	public class YM2203_status_port_r implements ReadHandler {
		int c;
		public YM2203_status_port_r(int context) {
			c = context;
		}
		@Override
		public int read(int address) {
			int i = YM2203Read(c,0);
			//System.out.println("YM2203_status_port_r" + c + " : " + i);
			return i;
		}
	}
	
	public ReadHandler ym2203_read_port_0_r() { return new YM2203_read_port_r(0); }
	public ReadHandler ym2203_read_port_1_r() { return new YM2203_read_port_r(1); }
	public ReadHandler ym2203_read_port_2_r() { return new YM2203_read_port_r(2); }
	public ReadHandler ym2203_read_port_3_r() { return new YM2203_read_port_r(3); }
	public ReadHandler ym2203_read_port_4_r() { return new YM2203_read_port_r(4); }
	public class YM2203_read_port_r implements ReadHandler {
		int c;
		public YM2203_read_port_r(int context) {
			c = context;
		}
		@Override
		public int read(int address) {
			int i = YM2203Read(c,1);
			//System.out.println("YM2203_read_port_r" + c + " : " + i);
			return i;
		}
	}
	
	public WriteHandler ym2203_control_port_0_w() { return new YM2203_control_port_w(0); }
	public WriteHandler ym2203_control_port_1_w() { return new YM2203_control_port_w(1); }
	public WriteHandler ym2203_control_port_2_w() { return new YM2203_control_port_w(2); }
	public WriteHandler ym2203_control_port_3_w() { return new YM2203_control_port_w(3); }
	public WriteHandler ym2203_control_port_4_w() { return new YM2203_control_port_w(4); }
	public class YM2203_control_port_w implements WriteHandler {
		int c;
		public YM2203_control_port_w(int context) {
			c = context;
		}
		@Override
		public void write(int address, int data) {
			//System.out.println("YM2203_control_port_w" + c + " : " + data);
			YM2203Write(c,0,data);
		}
	}

	public WriteHandler ym2203_write_port_0_w() { return new YM2203_write_port_w(0); }
	public WriteHandler ym2203_write_port_1_w() { return new YM2203_write_port_w(1); }
	public WriteHandler ym2203_write_port_2_w() { return new YM2203_write_port_w(2); }
	public WriteHandler ym2203_write_port_3_w() { return new YM2203_write_port_w(3); }
	public WriteHandler ym2203_write_port_4_w() { return new YM2203_write_port_w(4); }
	public class YM2203_write_port_w implements WriteHandler {
		int c;
		public YM2203_write_port_w(int context) {
			c = context;
		}
		@Override
		public void write(int address, int data) {
			//System.out.println("YM2203_write_port_w" + c + " : " + data);
			YM2203Write(c,1,data);
		}
	}
}
