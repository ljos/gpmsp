/*
 * Created on 14-aug-2005
 * 
 * To change the template for this generated file go to Window - Preferences -
 * Java - Code Generation - Code and Comments
 */
package jef.sound.chip.fm;

import jef.sound.chip.YM2203;
import jef.util.INT32;

/**
 * @author Erik Duijs
 * 
 * To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Generation - Code and Comments
 */
public class FMChan implements FMConstants {
	public FMSlot[] SLOT = new FMSlot[4];
	public int PAN; 						/* PAN :NONE,LEFT,RIGHT or CENTER */
	public int ALGO; 						/* Algorythm */
	public int FB; 						/* shift count of self feed back */
	public int[] op1_out = new int[2]; 	/* op1 output for beedback */
	/* Algorythm (connection) */
	public INT32 connect1; 					/* pointer of SLOT1 output */
	public INT32 connect2; 					/* pointer of SLOT2 output */
	public INT32 connect3; 					/* pointer of SLOT3 output */
	public INT32 connect4; 					/* pointer of SLOT4 output */
	/* LFO */
	public int pms; 						/* PMS depth level of channel */
	public long ams; 						/* AMS depth level of channel */
	/* Phase Generator */
	public int fc; 						/* fnum,blk :adjusted to sampling rate */
	public int fn_h; 						/* freq latch : */
	public int kcode; 						/* key code : */
	
	public FMChan() {
		for (int i = 0; i < 4; i++) {
			SLOT[i] = new FMSlot();
		}
	}
	/* ---------- calcrate one of channel ---------- */
	public void FM_CALC_CH()
	{
		int eg_out1,eg_out2,eg_out3,eg_out4;  //envelope output

		/* Phase Generator */
		//#if FM_LFO_SUPPORT
		//INT32 pms = lfo_pmd * this.pms / LFO_RATE;
		//if(pms)
		//{
		//	pg_in1 = (this.SLOT[SLOT1].Cnt += this.SLOT[SLOT1].Incr + (INT32)(pms * this.SLOT[SLOT1].Incr) / PMS_RATE);
		//	pg_in2 = (this.SLOT[SLOT2].Cnt += this.SLOT[SLOT2].Incr + (INT32)(pms * this.SLOT[SLOT2].Incr) / PMS_RATE);
		//	pg_in3 = (this.SLOT[SLOT3].Cnt += this.SLOT[SLOT3].Incr + (INT32)(pms * this.SLOT[SLOT3].Incr) / PMS_RATE);
		//	pg_in4 = (this.SLOT[SLOT4].Cnt += this.SLOT[SLOT4].Incr + (INT32)(pms * this.SLOT[SLOT4].Incr) / PMS_RATE);
		//}
		//else
		//	#endif
		{
			YM2203.pg_in1.value = (int) (this.SLOT[SLOT1].Cnt += this.SLOT[SLOT1].Incr);
			YM2203.pg_in2.value = (int) (this.SLOT[SLOT2].Cnt += this.SLOT[SLOT2].Incr);
			YM2203.pg_in3.value = (int) (this.SLOT[SLOT3].Cnt += this.SLOT[SLOT3].Incr);
			YM2203.pg_in4.value = (int) (this.SLOT[SLOT4].Cnt += this.SLOT[SLOT4].Incr);
		}

		/* Envelope Generator */
		eg_out1 = this.SLOT[SLOT1].FM_CALC_EG();
		eg_out2 = this.SLOT[SLOT2].FM_CALC_EG();
		eg_out3 = this.SLOT[SLOT2].FM_CALC_EG();
		eg_out4 = this.SLOT[SLOT4].FM_CALC_EG();

		/* Connection */
		if( eg_out1 < EG_CUT_OFF )	/* SLOT 1 */
		{
			if( this.FB != 0){
				/* with self feed back */
				YM2203.pg_in1.value += (this.op1_out[0]+this.op1_out[1])>>this.FB;
				this.op1_out[1] = this.op1_out[0];
			}
			this.op1_out[0] = OP_OUT(YM2203.pg_in1.value,eg_out1);
			/* output slot1 */
			if( this.connect1 == null)
			{
				/* algorythm 5  */
				YM2203.pg_in2.value += this.op1_out[0];
				YM2203.pg_in3.value += this.op1_out[0];
				YM2203.pg_in4.value += this.op1_out[0];
			}else{
				/* other algorythm */
				this.connect1.value += this.op1_out[0];
			}
		}
		if( eg_out2 < EG_CUT_OFF )	/* SLOT 2 */
			this.connect2.value += OP_OUT(YM2203.pg_in2.value,eg_out2);
		if( eg_out3 < EG_CUT_OFF )	/* SLOT 3 */
			this.connect3.value += OP_OUT(YM2203.pg_in3.value,eg_out3);
		if( eg_out4 < EG_CUT_OFF )	/* SLOT 4 */
			this.connect4.value += OP_OUT(YM2203.pg_in4.value,eg_out4);
	}
	
	/**
	 * @param pg
	 * @param eg
	 * @return
	 */
	private int OP_OUT(int PG, int EG) {
		//return YM2203.SIN_TABLE[ (PG / (0x1000000 / SIN_ENT)) & (SIN_ENT-1) ][EG];
		return YM2203.SIN_TABLE( (PG / (0x1000000 / SIN_ENT)) & (SIN_ENT-1), EG);
	}

	/* ---------- frequency counter  ---------- */
	public void CALC_FCOUNT()
	{
		if( this.SLOT[SLOT1].Incr==-1){
			int fc = this.fc;
			int kc = this.kcode;
			this.SLOT[SLOT1].CALC_FCSLOT(fc , kc );
			this.SLOT[SLOT2].CALC_FCSLOT(fc , kc );
			this.SLOT[SLOT3].CALC_FCSLOT(fc , kc );
			this.SLOT[SLOT4].CALC_FCSLOT(fc , kc );
		}
	}
	
	/* ----- key on of SLOT ----- */
	private boolean FM_KEY_IS(FMSlot SLOT) {
		return (SLOT.eg_next!=FM_EG_RELEASE);
	}
			
	public void FM_KEYON(int s )
	{
		FMSlot slot = SLOT[s];
		if( !FM_KEY_IS(slot) )
		{
			/* restart Phage Generator */
			slot.Cnt = 0;
			/* phase . Attack */
			//#if FM_SEG_SUPPORT
			if( (slot.SEG&8)!=0 ) slot.eg_next = FM_EG_SSG_AR;
			else
				//#endif
				slot.eg_next = FM_EG_AR;
			slot.evs     = slot.evsa;
			//#if 0
			/* convert decay count to attack count */
			/* --- This caused the problem by credit sound of paper boy. --- */
			//SLOT.evc = EG_AST + DRAR_TABLE[ENV_CURVE[SLOT.evc>>ENV_BITS]];/* + SLOT.evs;*/
			//#else
				/* reset attack counter */
			slot.evc = EG_AST;
			//#endif
			slot.eve = EG_AED;
		}
	}
	/* ----- key off of SLOT ----- */
	public void FM_KEYOFF(int s )
	{
		FMSlot slot = SLOT[s];
		if( FM_KEY_IS(slot) )
		{
			/* if Attack phase then adjust envelope counter */
			if( slot.evc < EG_DST )
				slot.evc = (YM2203.ENV_CURVE[slot.evc>>ENV_BITS]<<ENV_BITS) + EG_DST;
			/* phase . Release */
			slot.eg_next = FM_EG_RELEASE;
			slot.eve     = EG_DED;
			slot.evs     = slot.evsr;
		}
	}
	/**
	 * 
	 */
	public void CSMKeyControll() {
		/* int ksl = KSL[this.kcode]; */
		/* all key off */
		FM_KEYOFF(SLOT1);
		FM_KEYOFF(SLOT2);
		FM_KEYOFF(SLOT3);
		FM_KEYOFF(SLOT4);
		/* total level latch */
		this.SLOT[SLOT1].TLL = this.SLOT[SLOT1].TL /*+ ksl*/;
		this.SLOT[SLOT2].TLL = this.SLOT[SLOT2].TL /*+ ksl*/;
		this.SLOT[SLOT3].TLL = this.SLOT[SLOT3].TL /*+ ksl*/;
		this.SLOT[SLOT4].TLL = this.SLOT[SLOT4].TL /*+ ksl*/;
		/* all key on */
		FM_KEYON(SLOT1);
		FM_KEYON(SLOT2);
		FM_KEYON(SLOT3);
		FM_KEYON(SLOT4);		
	}
}
