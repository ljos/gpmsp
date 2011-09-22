/*
 * Created on 14-aug-2005
 * 
 * To change the template for this generated file go to Window - Preferences -
 * Java - Code Generation - Code and Comments
 */
package jef.sound.chip.fm;

import jef.sound.chip.YM2203;

/* OPN/A/B common state */

public class FMOpn implements FMConstants {
	public static final int TYPE_YM2203 = 0;

//	private static final double freq_table[] = { 3.98, 5.56, 6.02, 6.37, 6.88, 9.63, 48.1, 72.2 };
	
	public int type; /* chip type */
	public FMState ST; /* general state */
	public FM3Slot SL3 = new FM3Slot(); /* 3 slot mode state */
	public FMChan[] P_CH; /* pointer of CH */
	public long[] FN_TABLE = new long[2048]; /* fnumber . increment counter */
	/* LFO */
	public long LFOCnt;
	public long LFOIncr;
	public long[] LFO_FREQ = new long[8]; /* LFO FREQ table */
	public int[] LFO_wave = new int[512];

	/* sustain lebel table (3db per step) */
	/* 0 - 15: 0, 3, 6, 9,12,15,18,21,24,27,30,33,36,39,42,93 (dB)*/
	private static final int SC(double db)  {
		return (int)((db*((3/EG_STEP)*(1<<ENV_BITS)))+EG_DST);
	}
	static final int[] SL_TABLE = {
			SC( 0),SC( 1),SC( 2),SC(3 ),SC(4 ),SC(5 ),SC(6 ),SC( 7),
			SC( 8),SC( 9),SC(10),SC(11),SC(12),SC(13),SC(14),SC(31)
	};
	
	/* multiple table */
	//#define ML(n) (n*2);
	private static final int ML(double d) {
		return (int)(d * 2);
	}	
	private static final int[] MUL_TABLE = {
			/* 1/2, 1, 2, 3, 4, 5, 6, 7, 8, 9,10,11,12,13,14,15 */
			ML(0.50),ML( 1.00),ML( 2.00),ML( 3.00),ML( 4.00),ML( 5.00),ML( 6.00),ML( 7.00),
			ML(8.00),ML( 9.00),ML(10.00),ML(11.00),ML(12.00),ML(13.00),ML(14.00),ML(15.00),
			/* DT2=1 *SQL(2)   */
			ML(0.71),ML( 1.41),ML( 2.82),ML( 4.24),ML( 5.65),ML( 7.07),ML( 8.46),ML( 9.89),
			ML(11.30),ML(12.72),ML(14.10),ML(15.55),ML(16.96),ML(18.37),ML(19.78),ML(21.20),
			/* DT2=2 *SQL(2.5) */
			ML( 0.78),ML( 1.57),ML( 3.14),ML( 4.71),ML( 6.28),ML( 7.85),ML( 9.42),ML(10.99),
			ML(12.56),ML(14.13),ML(15.70),ML(17.27),ML(18.84),ML(20.41),ML(21.98),ML(23.55),
			/* DT2=3 *SQL(3)   */
			ML( 0.87),ML( 1.73),ML( 3.46),ML( 5.19),ML( 6.92),ML( 8.65),ML(10.38),ML(12.11),
			ML(13.84),ML(15.57),ML(17.30),ML(19.03),ML(20.76),ML(22.49),ML(24.22),ML(25.95)
	};
	
	private static final int[] OPN_FKTABLE={0,0,0,0,0,0,0,1,2,3,3,3,3,3,3,3};
	
	private static final int[] OPN_DTTABLE={
			/* this table is YM2151 and YM2612 data */
			/* FD=0 */
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			/* FD=1 */
			0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2,
			2, 3, 3, 3, 4, 4, 4, 5, 5, 6, 6, 7, 8, 8, 8, 8,
			/* FD=2 */
			1, 1, 1, 1, 2, 2, 2, 2, 2, 3, 3, 3, 4, 4, 4, 5,
			5, 6, 6, 7, 8, 8, 9,10,11,12,13,14,16,16,16,16,
			/* FD=3 */
			2, 2, 2, 2, 2, 3, 3, 3, 4, 4, 4, 5, 5, 6, 6, 7,
			8 , 8, 9,10,11,12,13,14,16,17,19,20,22,22,22,22
	};
	/* Dummy table of Attack / Decay rate ( use when rate == 0 ) */
	private static final int[] RATE_0 =
		{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};

	public FMOpn(FMIRQHandler ih) {
		this.ST = new FMState(ih); /* general state */
	}
	
	//#undef ML	
	/* ---------- write a OPN mode register 0x20-0x2f ---------- */
	public void OPNWriteMode(int r, int v)
	{
		int c;
		FMChan CH;

		switch(r){
		case 0x21:	/* Test */
			break;
			//#if FM_LFO_SUPPORT
		case 0x22:	/* LFO FREQ (YM2608/YM2612) */
			//if( (type & TYPE_LFOPAN) != 0 )
			//{
			//	LFOIncr = (v&0x08) ? LFO_FREQ[v&7] : 0;
			//	cur_chip = NULL;
			//}
			break;
			//#endif
		case 0x24:	/* timer A High 8*/
			ST.TA = (ST.TA & 0x03)|(v<<2);
			break;
		case 0x25:	/* timer A Low 2*/
			ST.TA = (ST.TA & 0x3fc)|(v&3);
			break;
		case 0x26:	/* timer B */
			ST.TB = v;
			break;
		case 0x27:	/* mode , timer controll */
			ST.FMSetMode(ST.index,v );
			break;
		case 0x28:	/* key on / off */
			c = v&0x03;
			if( c == 3 ) break;
			if( ((v&0x04)!=0) && ((type & TYPE_6CH)!=0) ) c+=3;
			CH = P_CH[c];
			/* csm mode */
			if( c == 2 && ((ST.mode & 0x80)!=0) ) break;
			if((v&0x10)!=0) CH.FM_KEYON(SLOT1); else CH.FM_KEYOFF(SLOT1);
			if((v&0x20)!=0) CH.FM_KEYON(SLOT2); else CH.FM_KEYOFF(SLOT2);
			if((v&0x40)!=0) CH.FM_KEYON(SLOT3); else CH.FM_KEYOFF(SLOT3);
			if((v&0x80)!=0) CH.FM_KEYON(SLOT4); else CH.FM_KEYOFF(SLOT4);
			/*		Log(LOG_INF,"OPN %d:%d : KEY %02X\n",n,c,v&0xf0);*/
			break;
		}
	}

	/* set detune & multiple */
	private void set_det_mul(FMState ST,FMChan CH,FMSlot SLOT,int v)
	{
		SLOT.mul = MUL_TABLE[v&0x0f];
		SLOT.DT  = ST.DT_TABLE[(v>>4)&7];
		CH.SLOT[SLOT1].Incr=-1;
	}
	/* set total level */
	private void set_tl(FMChan CH,FMSlot SLOT , int v, boolean csmflag)
	{
		v &= 0x7f;
		v = (v<<7)|v; /* 7bit . 14bit */
		SLOT.TL = (v*EG_ENT)>>14;
		if( !csmflag )
		{	/* not CSM latch total level */
			SLOT.TLL = SLOT.TL /* + KSL[CH.kcode] */;
		}
	}	
	/* set attack rate & key scale  */
	private  void set_ar_ksr(FMChan CH,FMSlot SLOT,int v,int[] ar_table)
	{
		SLOT.KSR  = 3-(v>>6);
		v&=0x1f;
		if (v!=0) {
			SLOT.AR   = ar_table;
			SLOT.AR_pointer = v << 1;
		} else {
			SLOT.AR = RATE_0;
			SLOT.AR_pointer = 0;
		}
		SLOT.evsa = SLOT.AR[SLOT.ksr];
		if( SLOT.eg_next == FM_EG_AR ) SLOT.evs = SLOT.evsa;
		CH.SLOT[SLOT1].Incr=-1;
	}
	/* set decay rate */
	private  void set_dr(FMSlot SLOT,int v,int[] dr_table)
	{
		v&=0x1f;
		if (v!=0) {
			SLOT.DR   = dr_table;
			SLOT.DR_pointer = v << 1;
		} else {
			SLOT.DR = RATE_0;
			SLOT.DR_pointer = 0;
		}
		SLOT.evsd = SLOT.DR[SLOT.ksr];
		if( SLOT.eg_next == FM_EG_DR ) SLOT.evs = SLOT.evsd;
	}
	/* set sustain rate */
	private  void set_sr(FMSlot SLOT,int v,int[] dr_table)
	{
		v&=0x1f;
		if (v!=0) {
			SLOT.SR   = dr_table;
			SLOT.SR_pointer = v << 1;
		} else {
			SLOT.SR = RATE_0;
			SLOT.SR_pointer = 0;
		}
		SLOT.evss = SLOT.SR[SLOT.ksr];
		if( SLOT.eg_next == FM_EG_SR ) SLOT.evs = SLOT.evss;
	}
	/* set release rate */
	private  void set_sl_rr(FMSlot SLOT,int v,int[] dr_table)
	{
		SLOT.SL = SL_TABLE[(v>>4)];
		SLOT.RR_pointer = ((v&0x0f)<<2)|2;
		SLOT.RR = dr_table;
		SLOT.evsr = SLOT.RR[SLOT.ksr];
		if( SLOT.eg_next == FM_EG_RELEASE ) SLOT.evs = SLOT.evsr;
	}
	
	private int OPN_CHAN(int N){
		return (N&3);
	}
	private int OPN_SLOT(int N) {
		return ((N>>2)&3);
	}

	/* ---------- write a OPN register (0x30-0xff) ---------- */
	public void OPNWriteReg(int r, int v)
	{
		int c;
		FMChan CH;
		FMSlot SLOT;

		/* 0x30 - 0xff */
		if( (c = OPN_CHAN(r)) == 3 ) return; /* 0xX3,0xX7,0xXB,0xXF */
		if( (r >= 0x100) /* && (type & TYPE_6CH) */ ) c+=3;
		CH = P_CH[c];

		SLOT = (CH.SLOT[OPN_SLOT(r)]);
		switch( r & 0xf0 ) {
		case 0x30:	/* DET , MUL */
			set_det_mul(ST,CH,SLOT,v);
			break;
		case 0x40:	/* TL */
			set_tl(CH,SLOT,v,(c == 2) && ((ST.mode & 0x80)!=0) );
			break;
		case 0x50:	/* KS, AR */
			set_ar_ksr(CH,SLOT,v,ST.AR_TABLE);
			break;
		case 0x60:	/*     DR */
			/* bit7 = AMS_ON ENABLE(YM2612) */
			set_dr(SLOT,v,ST.DR_TABLE);
			//#if FM_LFO_SUPPORT
			//if( type & TYPE_LFOPAN)
			//{
			//	SLOT.amon = v>>7;
			//	SLOT.ams = CH.ams * SLOT.amon;
			//}
			//#endif
			break;
		case 0x70:	/*     SR */
			set_sr(SLOT,v,ST.DR_TABLE);
			break;
		case 0x80:	/* SL, RR */
			set_sl_rr(SLOT,v,ST.DR_TABLE);
			break;
		case 0x90:	/* SSG-EG */
			//#if !FM_SEG_SUPPORT
			//if(v&0x08) Log(LOG_ERR,"OPN %d,%d,%d :SSG-TYPE envelope selected (not supported )\n",ST.index,c,OPN_SLOT(r));
			//#endif
			SLOT.SEG = v&0x0f;
			break;
		case 0xa0:
			switch( OPN_SLOT(r) ){
			case 0:		/* 0xa0-0xa2 : FNUM1 */
			{
				int fn  = (( (CH.fn_h)&7)<<8) + v;
				int blk = CH.fn_h>>3;
				/* make keyscale code */
				CH.kcode = (blk<<2)|OPN_FKTABLE[(fn>>7)];
				/* make basic increment counter 32bit = 1 cycle */
				CH.fc = (int) (FN_TABLE[fn]>>(7-blk));
				CH.SLOT[SLOT1].Incr=-1;
			}
			break;
			case 1:		/* 0xa4-0xa6 : FNUM2,BLK */
				CH.fn_h = v&0x3f;
				break;
			case 2:		/* 0xa8-0xaa : 3CH FNUM1 */
				if( r < 0x100)
				{
					int fn  = ((SL3.fn_h[c]&7)<<8) + v;
					int blk = SL3.fn_h[c]>>3;
					/* make keyscale code */
					SL3.kcode[c]= (blk<<2)|OPN_FKTABLE[(fn>>7)];
					/* make basic increment counter 32bit = 1 cycle */
					SL3.fc[c] = FN_TABLE[fn]>>(7-blk);
					(P_CH)[2].SLOT[SLOT1].Incr=-1;
				}
				break;
			case 3:		/* 0xac-0xae : 3CH FNUM2,BLK */
				if( r < 0x100)
					SL3.fn_h[c] = v&0x3f;
				break;
			}
			break;
		case 0xb0:
			switch( OPN_SLOT(r) ){
			case 0:		/* 0xb0-0xb2 : FB,ALGO */
			{
				int feedback = (v>>3)&7;
				CH.ALGO = v&7;
				CH.FB   = (feedback!=0) ? 8+1 - feedback : 0;
				YM2203.setup_connection( CH );
			}
			break;
			case 1:		/* 0xb4-0xb6 : L , R , AMS , PMS (YM2612/YM2608) */
				if( (type & TYPE_LFOPAN)!=0)
				{
					//#if FM_LFO_SUPPORT
					/* b0-2 PMS */
					/* 0,3.4,6.7,10,14,20,40,80(cent) */
					//static const double pmd_table[8]={0,3.4,6.7,10,14,20,40,80};
					//static const int amd_table[4]={0/EG_STEP,1.4/EG_STEP,5.9/EG_STEP,11.8/EG_STEP };
					//CH.pms = (1.5/1200.0)*pmd_table[(v>>4) & 0x07] * PMS_RATE;
					/* b4-5 AMS */
					/* 0 , 1.4 , 5.9 , 11.8(dB) */
					//CH.ams = amd_table[(v>>4) & 0x03];
					//CH.SLOT[SLOT1].ams = CH.ams * CH.SLOT[SLOT1].amon;
					//CH.SLOT[SLOT2].ams = CH.ams * CH.SLOT[SLOT2].amon;
					//CH.SLOT[SLOT3].ams = CH.ams * CH.SLOT[SLOT3].amon;
					//CH.SLOT[SLOT4].ams = CH.ams * CH.SLOT[SLOT4].amon;
					//#endif
					/* PAN */
					CH.PAN = (v>>6)&0x03; /* PAN : b6 = R , b7 = L */
					YM2203.setup_connection( CH );
					/* Log(LOG_INF,"OPN %d,%d : PAN %d\n",n,c,CH.PAN);*/
				}
				break;
			}
			break;
		}
	}
	

	

	public void setPris(int pris, int TimerPris, int SSGpris) {
		int i;

		/* frequency base */
		ST.freqbase = (ST.rate != 0) ? ((double) ST.clock / ST.rate) / pris : 0;
		/* Timer base time */
		ST.TimerBase = 1.0 / ((double) ST.clock / (double) TimerPris);
		/* SSG part priscaler set */
		if (SSGpris != 0)
			YM2203.SSG.AY8910_set_clock(ST.index, ST.clock * 2 / SSGpris);
		/* make time tables */
		ST.init_timetables(OPN_DTTABLE, OPN_ARRATE, OPN_DRRATE);
		/* make fnumber . increment counter table */
		for (i = 0; i < 2048; i++) {
			/* it is freq table for octave 7 */
			/* opn freq counter = 20bit */
			FN_TABLE[i] = (long) (i * ST.freqbase * FREQ_RATE * (1 << 7) / 2);
		}
		//#if FM_LFO_SUPPORT
		/* LFO wave table */
		//for (i = 0; i < LFO_ENT; i++) {
		//	LFO_wave[i] =
		//		i < LFO_ENT / 2
		//			? i * LFO_RATE / (LFO_ENT / 2)
		//			: (LFO_ENT - i) * LFO_RATE / (LFO_ENT / 2);
		//}
		/* LFO freq. table */ //{
			/* 3.98Hz,5.56Hz,6.02Hz,6.37Hz,6.88Hz,9.63Hz,48.1Hz,72.2Hz @ 8MHz */
//
	//		for (i = 0; i < 8; i++) {
	//			LFO_FREQ[i] =
	//				(ST.rate != 0)
	//					? ((double) LFO_ENT
	//						* (1 << LFO_SHIFT)
	//						/ (ST.rate
	//							/ freq_table[i]
	//							* (ST.freqbase * ST.rate / (8000000.0 / 144))))
	//					: 0;
//
//			}
//		}
		//#endif
		/* Log(LOG_INF,"OPN %d set priscaler %d\n",ST.index,pris); */
	}

}
