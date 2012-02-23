/*
 * Created on 14-aug-2005
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package jef.sound.chip.fm;

import jef.sound.chip.YM2203;

/**
 * @author Erik Duijs
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class FMSlot implements FMConstants {
	public int[] DT;				/* detune          :DT_TABLE[DT]       */
	public int DT2;			/* multiple,Detune2:(DT2<<4)|ML for OPM*/
	public int TL;				/* total level     :TL << 8            */
	public int KSR;			/* key scale rate  :3-KSR              */
	public int[] AR;				/* attack rate     :&AR_TABLE[AR<<1]   */
	public int AR_pointer;
	public int[] DR;				/* decay rate      :&DR_TABLE[DR<<1]   */
	public int DR_pointer;
	public int[] SR;				/* sustin rate     :&DR_TABLE[SR<<1]   */
	public int SR_pointer;
	public int SL;				/* sustin level    :SL_TABLE[SL]       */
	public int[] RR;				/* release rate    :&DR_TABLE[RR<<2+2] */
	public int RR_pointer;
	public int SEG;			/* SSG EG type     :SSGEG              */
	public int ksr;			/* key scale rate  :kcode>>(3-KSR)     */
	public long mul;			/* multiple        :ML_TABLE[ML]       */
	/* Phase Generator */
	public long Cnt;			/* frequency count :                   */
	public long Incr;			/* frequency step  :                   */
	/* Envelope Generator */
	//void (*eg_next)(struct fm_slot *SLOT);	/* pointer of phase handler */
	public int eg_next;
	public int evc;			/* envelope counter                    */
	public int eve;			/* envelope counter end point          */
	public int evs;			/* envelope counter step               */
	public int evsa;			/* envelope step for Attack            */
	public int evsd;			/* envelope step for Decay             */
	public int evss;			/* envelope step for Sustain           */
	public int evsr;			/* envelope step for Release           */
	public int TLL;			/* adjusted TotalLevel                 */
	/* LFO */
	public int amon;			/* AMS enable flag              */
	public long ams;			/* AMS depth level of this SLOT */
	
	public int FM_CALC_EG()						
	{													
		if( (this.evc += this.evs) >= this.eve) 		
			this.egNext(this.eg_next);						
		int OUT = this.TLL+YM2203.ENV_CURVE[this.evc>>ENV_BITS];	
		if(this.ams != 0)									
		OUT += (this.ams* YM2203.lfo_amd/LFO_RATE);		
		return OUT;
	}

	/* ---------- frequency counter for operater update ---------- */
	public void CALC_FCSLOT(long fc , int kc )
	{
		int ksr;

		/* frequency step counter */
		/* this.Incr= (fc+this.DT[kc])*this.mul; */
		this.Incr= fc*this.mul + this.DT[kc];
		ksr = kc >> this.KSR;
		if( this.ksr != ksr )
		{
			this.ksr = ksr;
			/* attack , decay rate recalcration */
			this.evsa = this.AR[AR_pointer + ksr];
			this.evsd = this.DR[DR_pointer + ksr];
			this.evss = this.SR[SR_pointer + ksr];
			this.evsr = this.RR[RR_pointer + ksr];
		}
		this.TLL = this.TL /* + KSL[kc]*/;
	}
	
	
	public void egNext(int i) {
		switch (i) {
		case FM_EG_RELEASE :
			evc = EG_OFF;
			eve = EG_OFF+1;
			evs = 0;
			break;
		case FM_EG_SR :
			evc = EG_OFF;
			eve = EG_OFF+1;
			evs = 0;
			break;
		case FM_EG_DR :
			eg_next = FM_EG_SR;
			evc = SL;
			eve = EG_DED;
			evs = evss;
			break;
		case FM_EG_AR :
			/* next DR */
			eg_next = FM_EG_DR;
			evc = EG_DST;
			eve = SL;
			evs = evsd;
			break;
		case FM_EG_SSG_DR :
			if( (SEG&2)!=0){
				/* reverce */
				eg_next = FM_EG_SSG_SR;
				evc = SL + (EG_UST - EG_DST);
				eve = EG_UED;
				evs = evss;
			}else{
				/* again */
				evc = EG_DST;
			}
			/* hold */
			if( (SEG&1)!=0) evs = 0;		
			break;
		case FM_EG_SSG_SR :
			if( (SEG&2)!=0){
				/* reverce  */
				eg_next = FM_EG_SSG_DR;
				evc = EG_DST;
				eve = EG_DED;
				evs = evsd;
			}else{
				/* again */
				evc = SL + (EG_UST - EG_DST);
			}
			/* hold check */
			if( (SEG&1)!=0) evs = 0;
			break;
		case FM_EG_SSG_AR :
			if( (SEG&4)!=0){	/* start direction */
				/* next SSG-SR (upside start ) */
				eg_next = FM_EG_SSG_SR;
				evc = SL + (EG_UST - EG_DST);
				eve = EG_UED;
				evs = evss;
			}else{
				/* next SSG-DR (downside start ) */
				eg_next = FM_EG_SSG_DR;
				evc = EG_DST;
				eve = EG_DED;
				evs = evsd;
			}
			break;
		}
	}
}
