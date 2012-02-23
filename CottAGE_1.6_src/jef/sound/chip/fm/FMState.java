/*
 * Created on 14-aug-2005
 * 
 * To change the template for this generated file go to Window - Preferences -
 * Java - Code Generation - Code and Comments
 */
package jef.sound.chip.fm;

/**
 * @author Erik Duijs
 * 
 * To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Generation - Code and Comments
 */
public class FMState implements FMConstants {
	public static final int FM_TIMER_INTERVAL = 0;

	/* OPN/OPM common state */
	public int index; /* chip index (number of chip) */
	public int clock; /* master clock (Hz) */
	public int rate; /* sampling rate (Hz) */
	public double freqbase; /* frequency base */
	public double TimerBase; /* Timer base time */
	public int address; /* address register */
	public int irq; /* interrupt level */
	public int irqmask; /* irq mask */
	public int status; /* status flag */
	public long mode; /* mode CSM / 3SLOT */
	public int TA; /* timer a */
	public int TAC; /* timer a counter */
	public int TB; /* timer b */
	public int TBC; /* timer b counter */
	/* speedup customize */
	/* local time tables */
	public int[][] DT_TABLE = new int[8][32]; /* DeTune tables */
	public int[] AR_TABLE = new int[94]; /* Atttack rate tables */
	public int[] DR_TABLE = new int[94]; /* Decay rate tables */
	/* Extention Timer and IRQ handler */
	public FMTimerHandler Timer_Handler;
	public FMIRQHandler IRQ_Handler;
	/* timer model single / interval */
	public int timermodel;
	
	public FMState (FMIRQHandler ih) {
		this.IRQ_Handler = ih;
	}

	/* OPN/OPM Mode Register Write */
	public void FMSetMode(int n, int v) {
		/* b7 = CSM MODE */
		/* b6 = 3 slot mode */
		/* b5 = reset b */
		/* b4 = reset a */
		/* b3 = timer enable b */
		/* b2 = timer enable a */
		/* b1 = load b */
		/* b0 = load a */
		mode = v;

		/* reset Timer b flag */
		if ((v & 0x20)!=0)
			FM_STATUS_RESET(0x02);
		/* reset Timer a flag */
		if ((v & 0x10)!=0)
			FM_STATUS_RESET(0x01);
		/* load b */
		if ((v & 0x02) != 0) {
			if (TBC == 0) {
				TBC = (256 - TB) << 4;
				/* External timer handler */
				if (Timer_Handler != null) {
					Timer_Handler.n = n;
					Timer_Handler.c = 1;
					Timer_Handler.cnt = TBC;
					Timer_Handler.stepTime = TimerBase;
				}

			}
		} else if (timermodel == FM_TIMER_INTERVAL) { /* stop interbval timer */
			if (TBC != 0) {
				TBC = 0;
				if (Timer_Handler != null) {
					Timer_Handler.n = n;
					Timer_Handler.c = 1;
					Timer_Handler.cnt = 0;
					Timer_Handler.stepTime = TimerBase;
				}
			}
		}
		/* load a */
		if ((v & 0x01) != 0) {
			if (TAC == 0) {
				TAC = (1024 - TA);
				/* External timer handler */
				if (Timer_Handler != null) {
					Timer_Handler.n = n;
					Timer_Handler.c = 0;
					Timer_Handler.cnt = TAC;
					Timer_Handler.stepTime = TimerBase;
				}

			}
		} else if (timermodel == FM_TIMER_INTERVAL) { /* stop interbval timer */
			if (TAC != 0) {
				TAC = 0;
				if (Timer_Handler != null) {
					Timer_Handler.n = n;
					Timer_Handler.c = 0;
					Timer_Handler.cnt = 0;
					Timer_Handler.stepTime = TimerBase;
				}
			}
		}
	}

	/* status set and IRQ handling */
	public void FM_STATUS_SET(int flag) {
		/* set status flag */
		status |= flag;
		//System.out.println("FM_STATUS_SET, status = " + status);
		if (!(irq != 0) && ((status & irqmask) != 0)) {
			irq = 1;
			/* callback user interrupt handler (IRQ is OFF to ON) */
			if (IRQ_Handler != null) {
				IRQ_Handler.irq(index, 1);
			}
		}
	}

	/* status reset and IRQ handling */
	public void FM_STATUS_RESET(int flag) {
		/* reset status flag */
		status &= ~flag;
		//System.out.println("FM_STATUS_RESET, status = " + status);
		if ((irq != 0) && !((status & irqmask) != 0)) {
			irq = 0;
			/* callback user interrupt handler (IRQ is ON to OFF) */
			if (IRQ_Handler != null) {
				IRQ_Handler.irq(index, 0);
			}
		}
	}

	/* IRQ mask set */
	public void FM_IRQMASK_SET(int flag) {
		irqmask = flag;
		System.out.println("FM_IRQMASK_SET, irqmask = " + irqmask);
		/* IRQ handling check */
		FM_STATUS_SET(0);
		FM_STATUS_RESET(0);
	}
	
	/* ----- internal timer mode , update timer */
	/* ---------- calcrate timer A ---------- */
	public void INTERNAL_TIMER_A(FMChan CSM_CH)					
	{													
		if( this.TAC != 0 &&  (this.Timer_Handler==null) )		
		if( (this.TAC -= this.freqbase*4096) <= 0 )	
		{											
			TimerAOver();						
			/* CSM mode total level latch and auto key on */	
			if( (this.mode & 0x80) != 0 )					
				CSM_CH.CSMKeyControll();			
		}											
	}
	/* ---------- calcrate timer B ---------- */
	public void INTERNAL_TIMER_B(int step)						
	{														
		if( this.TBC != 0 && (this.Timer_Handler==null) )				
		if( (this.TBC -= this.freqbase*4096*step) <= 0 )	
		TimerBOver();							
	}
	
	/* Timer A Overflow */
	public void TimerAOver()
	{
		/* status set if enabled */
		if((this.mode & 0x04)!=0) FM_STATUS_SET(0x01);
		/* clear or reload the counter */
		if (this.timermodel == FM_TIMER_INTERVAL)
		{
			this.TAC = (1024-this.TA);
			//if (this.Timer_Handler != 0) (this.Timer_Handler)(this.index,0,(double)this.TAC,this.TimerBase);
		}
		else this.TAC = 0;
	}
	/* Timer B Overflow */
	public void TimerBOver()
	{
		/* status set if enabled */
		if((this.mode & 0x08)!=0) FM_STATUS_SET(0x02);
		/* clear or reload the counter */
		if (this.timermodel == FM_TIMER_INTERVAL)
		{
			this.TBC = ( 256-this.TB)<<4;
			//if (this.Timer_Handler) (this.Timer_Handler)(this.index,1,(double)this.TBC,this.TimerBase);
		}
		else this.TBC = 0;
	}
	public void init_timetables( int[] DTTABLE , int ARRATE , int DRRATE )
	{
		int i,d;
		double rate;

		/* DeTune table */
		for (d = 0;d <= 3;d++){
			for (i = 0;i <= 31;i++){
				rate = DTTABLE[d*32 + i] * this.freqbase * FREQ_RATE;
				this.DT_TABLE[d][i]   =  (int) rate;
				this.DT_TABLE[d+4][i] = (int) -rate;
			}
		}
		/* make Attack & Decay tables */
		for (i = 0;i < 4;i++) this.AR_TABLE[i] = this.DR_TABLE[i] = 0;
		for (i = 4;i < 64;i++){
			rate  = this.freqbase;						/* frequency rate */
			if( i < 60 ) rate *= 1.0+(i&3)*0.25;		/* b0-1 : x1 , x1.25 , x1.5 , x1.75 */
			rate *= 1<<((i>>2)-1);						/* b2-5 : shift bit */
			rate *= (EG_ENT<<ENV_BITS);
			this.AR_TABLE[i] = (int) (rate / ARRATE);
			this.DR_TABLE[i] = (int) (rate / DRRATE);
		}
		this.AR_TABLE[62] = EG_AED-1;
		this.AR_TABLE[63] = EG_AED-1;
		for (i = 64;i < 94 ;i++){	/* make for overflow area */
			this.AR_TABLE[i] = this.AR_TABLE[63];
			this.DR_TABLE[i] = this.DR_TABLE[63];
		}

	}
}
