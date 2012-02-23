/*
 * Created on 14-aug-2005
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package jef.sound.chip.fm;

/**
 * @author Erik Duijs
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public interface FMConstants {
	/* some globals */
	public static final int TYPE_SSG    = 0x01;    /* SSG support          */
	public static final int TYPE_OPN    = 0x02;    /* OPN device           */
	public static final int TYPE_LFOPAN = 0x04;    /* OPN type LFO and PAN */
	public static final int TYPE_6CH    = 0x08;    /* FM 6CH / 3CH         */
	public static final int TYPE_DAC    = 0x10;    /* YM2612's DAC device  */
	public static final int TYPE_ADPCM  = 0x20;    /* two ADPCM unit       */
	
	/* slot number */
	public static final int  SLOT1 = 0;
	public static final int  SLOT2 = 2;
	public static final int  SLOT3 = 1;
	public static final int  SLOT4 = 3;
	
	/* sinwave entries */
	/* used static memory = SIN_ENT * 4 (byte) */
	public static final int SIN_ENT = 2048;

	/* output level entries (envelope,sinwave) */
	/* envelope counter lower bits */
	public static final int ENV_BITS = 16;
	/* envelope output entries */
	public static final int EG_ENT   = 4096;

	/* envelope counter position */
	public static final int EG_AST   = 0;					/* start of Attack phase */
	public static final int EG_AED   = (EG_ENT<<ENV_BITS);	/* end   of Attack phase */
	public static final int EG_DST   = EG_AED	;			/* start of Decay/Sustain/Release phase */
	public static final int EG_DED   = (EG_DST+((EG_ENT-1)<<ENV_BITS));	/* end   of Decay/Sustain/Release phase */
	public static final int EG_OFF   = EG_DED;				/* off */
	//#if FM_SEG_SUPPORT
	public static final int EG_UST   = ((2*EG_ENT)<<ENV_BITS);  /* start of SEG UPSISE */
	public static final int EG_UED   = ((3*EG_ENT)<<ENV_BITS);  /* end of SEG UPSISE */
	//#endif
	
	public static final double EG_STEP = (96.0/EG_ENT); /* OPL is 0.1875 dB step  */

	/* LFO table entries */
	public static final int VIB_ENT = 512;
	public static final int VIB_SHIFT = (32-9);
	public static final int AMS_ENT = 512;
	public static final int AMS_SHIFT = (32-9);

	public static final int VIB_RATE = 256;
	
	public static final int FM_EG_RELEASE = 0;
	public static final int FM_EG_SR = 1;
	public static final int FM_EG_DR = 2;
	public static final int FM_EG_AR = 3;

	public static final int FM_EG_SSG_DR = 4;
	public static final int FM_EG_SSG_SR = 5;
	public static final int FM_EG_SSG_AR = 6;
	
	public static final int FM_OUTPUT_BIT = 16;
	/* -------------------- preliminary define section --------------------- */
	/* attack/decay rate time rate */
	public static final int OPM_ARRATE    =  399128;
	public static final int OPM_DRRATE    = 5514396;
	/* It is not checked , because I haven't YM2203 rate */
	public static final int OPN_ARRATE  = OPM_ARRATE;
	public static final int OPN_DRRATE  = OPM_DRRATE;

	/* PG output cut off level : 78dB(14bit)? */
	public static final int PG_CUT_OFF = ((int)(78.0/EG_STEP));
	/* EG output cut off level : 68dB? */
	public static final int EG_CUT_OFF = ((int)(68.0/EG_STEP));

	public static final int FREQ_BITS = 24;		/* frequency turn          */

	/* PG counter is 21bits @oct.7 */
	public static final int FREQ_RATE   = (1<<(FREQ_BITS-21));
	public static final int TL_BITS    = (FREQ_BITS+2);
	/* OPbit = 14(13+sign) : TL_BITS+1(sign) / output = 16bit */
	public static final int TL_SHIFT = (TL_BITS+1-(14-16));

	/* output final shift */
	public static final int FM_OUTSB  = (TL_SHIFT-FM_OUTPUT_BIT);
	public static final int FM_MAXOUT = ((1<<(TL_SHIFT-1))-1);
	public static final int FM_MINOUT = (-(1<<(TL_SHIFT-1)));
	
	/* bit0 = Right enable , bit1 = Left enable */
	public static final int OUTD_RIGHT  = 1;
	public static final int OUTD_LEFT   = 2;
	public static final int OUTD_CENTER = 3;
	
	public static final int  TL_MAX = (PG_CUT_OFF+EG_CUT_OFF+1);
	

	public static final int  LFO_RATE = 0x10000;
	
	
}
