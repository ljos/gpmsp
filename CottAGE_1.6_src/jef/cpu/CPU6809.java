/*

Java Emulation Framework

This library contains a framework for creating emulation software.

Copyright (C) 2002 Erik Duijs (erikduijs@yahoo.com)

Contributors:
- Julien Freilat
- Arnon Goncalves Cardoso
- S.C. Wong
- Romain Tisserand
- David Raingeard


This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

*/

package jef.cpu;

import jef.cpuboard.CpuBoard;

//**********************************************************************
//* CPU6809 - MC6809 Emulator Version 1.0
//* By S.C.W. Jan 2001
//*
//* Verion 1.0 By S.C.W. Oct 2001.
//* This version is written for JEmu
//*
//* Notes:
//* 1, The arguments driver ram are neglected excepted in setram().
//* ram is set by this method and stored as an object private variable
//*
//* 2. There are two hardware IRQ (except DMA and NMI). The extra FIRQ
//* is not in interface cpu
//*
//* Erik Duijs May 2002
//* - implemented jef.cpu.Cpu interface and removed JEmu interface functions.
//*
//* S.C.W. Jan 2002
//* Version 1.0 - finished
//* 1. opcode results compared with MAME, esp. mul and daa
//* 2. CC changed to 8 booleans
//* 3. registers using  ints
//* 4. abandoned microprograms, the dirty trick of function pointer
//*    use a huge switch-case
//* 5. sync and cwai implemented and tested
//*    (controlled by waitState)
//* 6. irq, firq and nmi implemented
//*    (controlled by interruptState)
//* 7. CPU synchronized - current at 1.25MHz (800ns/cycle)
//* 8. tracer/disassembler encapsulated
//* 9. memory I/O encapsulated in driver
//*
//* Notes:
//* This will be the final version for some time. Only bugs will be fixed,
//* but no functionality will be added nor optimization done.
//* The current version is general, it can be used with J6809 and JEmu.
//*
//**********************************************************************

/**
 * Interpreting MC6809 CPU emulator.
 * 
 * @author S.C. Wong
 * 
 * CPU6809.java */
public class CPU6809 implements CPU6809Const, Cpu
{
	int traceinstructions = 0;

//**********************************************************************
// Variables
//
//**********************************************************************
  /* registers package sim6809.accessible */
  public boolean flagE, flagF, flagH, flagI;     // 1-bit flags
  public boolean flagN, flagZ, flagV, flagC;     // 1-bit flags
  public int a = 0, b = 0, dp = 0;                 // 8-bit registers
  public int s = 0, u = 0, x = 0, y = 0, pc = 0;  // 16-bit registers
  public int ir = 0;                              // Instruction register
  public int ea = 0;                              // effective address

  public int waitState = RUNNING_STATE;           // wait state

  public int lastPc, opPc;

  public int breakpoint = 0xffff;

  private int curInstr=0;
  /* internal variables */
  public int page;              // page # for 16-bit opcodes
  public int addrMode;          // addressing modes
  private boolean useEa = false; // is an effective address used

  private int tCycle; // number of cycles elapsed in this frame

  private CpuBoard cb; // cb

  public int interruptState; // the mix of the IRQ control lines.

  private String tag;

//**********************************************************************
// Constructor
//
//**********************************************************************
  public CPU6809()
  {
  }

  /** Reset the state (registers) of the CPU.
   *  only affect dp, ir, ea, pc and cc
   */

  public final void reset()
  {
    dp = ir = ea = 0;

    flagI = true; // disable IRQ
    flagF = true; // disable FIRQ

    pc = (readByte(RESET_VECTOR_HI) << 8) |
          readByte(RESET_VECTOR_LO);

    page = 1; useEa = false;
    addrMode = INHERENT;
    interruptState = NO_IRQ;
    tCycle = 0;
  }

//**********************************************************************
// Implementation of CottAGE Cpu.java interface methods
//
//**********************************************************************
  public final boolean init(CpuBoard cb, int debug)
  {	this.cb = cb;
	return true;
  }

  public final void interrupt(int type, boolean irq)
  {	switch (type)
	{	case INTERRUPT_TYPE_IRQ:
		  irq();
		  break;
		case INTERRUPT_TYPE_NMI:
		  nmi();
		  break;
		case INTERRUPT_TYPE_FIRQ:
		  firq();
		  break;
	  }
  }

  public final long getInstruction()
  {	// most instructions are 1-byte long,
    // except those in page 2 and 3 are 2-byte.
    // an int can hold them anyway
    // format: the return value: b3b2b1b0, b3 the MSB and b0 the LSB
    // b3 and b2 always equal to zero;
    // b1 = 0, 0x10 or 0x11 depending on page;
    // b0 = ir.int curInstr = ir;
    if (page == 2) curInstr |= 0x1000;
    else if (page == 3) curInstr |= 0x1100;
    return (long)curInstr;
  }

  public final void setProperty(int p, int v)
  {
  }

  public final void setDebug(int d)
  {
  }

  public final int getDebug()
  { return 0;
  }

  public void	setTag(String tag)
  { this.tag = tag;
  }

  public String	getTag()
  { return this.tag;
  }

  // 6809 has 2 irqs: IRQ and FIRQ on top of NMI
  // Interrupt is set default as IRQ
  /**
   * Sending interrupt requests
   */
  public void irq()
  { interruptState |= SEND_IRQ;
    if (waitState == CWAI_STATE && !flagI) waitState = RUNNING_STATE;
    if (waitState == SYNC_STATE) waitState = RUNNING_STATE;
  }

  public void nmi()
  { interruptState |= SEND_NMI;
    if (waitState == CWAI_STATE) waitState = RUNNING_STATE;
    if (waitState == SYNC_STATE) waitState = RUNNING_STATE;
  }

  public void firq()
  { interruptState |= SEND_FIRQ;
    if (waitState == CWAI_STATE && !flagF) waitState = RUNNING_STATE;
    if (waitState == SYNC_STATE) waitState = RUNNING_STATE;
  }

  /**
   * Execute a number of clock cycles
   */
  public void exec(int cycles)
  {
    tCycle = cycles;

    if ((interruptState & SEND_NMI) != 0)
    { runNmi();
    }

    if ((interruptState & SEND_IRQ) != 0 && !flagI)
    { runIrq();
    }

    if ((interruptState & SEND_FIRQ) != 0 && !flagF)
    { runFirq();
    }
    interruptState = NO_IRQ;

    while (tCycle > 0 && pc <= breakpoint)
    { execInstr();
    }
  }

  // return value = cycles had taken up
  public void execInstr()
  {

    lastPc = pc; // DEBUG

    ir = cb.read8opc(pc++);

	/* TAKE OUT FROM HERE!!! */

/*
    //if (lastPc == 0x6307) traceinstructions = 999999;
    //if (lastPc == 0x6436) traceinstructions = 999999;
    //if (lastPc == 0x62dd) traceinstructions = 999999;
    //if (lastPc == 0x6000) traceinstructions = 999999;
    //if (lastPc == 0x6436) traceinstructions = 999999;
    //if (lastPc == 0x61f1) traceinstructions = 999999;
    if (lastPc == 0x70a0) traceinstructions = 999999;


    if (traceinstructions > 0)  {
    	System.out.print(Integer.toHexString(lastPc)+","+Integer.toHexString(ir)+"-"+Integer.toHexString(cb.read8(pc))+","+Integer.toHexString(cb.read8(pc+1)));
    	System.out.print(" : S=" + Integer.toHexString(s));
    	System.out.print(" : A=" + Integer.toHexString(a));
    	System.out.print(" : B=" + Integer.toHexString(b));
    	System.out.print(" : X=" + Integer.toHexString(x));
    	System.out.print(" : Y=" + Integer.toHexString(y));
    	System.out.print(" : DP=" + Integer.toHexString(dp));
    	System.out.print(" : U=" + Integer.toHexString(u));
     	System.out.println(" : Cycles=" + tCycle);
   		traceinstructions--;
	}*/

	/* TAKE oUT UNTIL HERE!!! */

    tCycle -= decode(); // instrCycle set here
    execSingleInstr(ir & 0xff);
  }

//**********************************************************************
// Memory I/O
//
//**********************************************************************

  /** Read a byte from the memory */
  public final int readByte(int _ea)
  { return cb.read8(_ea & 0xffff);
  }

  /** Read 2 bytes from the memory */
  public final int read2Bytes(int _ea)
  { int temp = cb.read8(_ea & 0xffff) << 8;
    temp = temp | cb.read8((_ea & 0xffff) + 1);
    return temp;
  }

  /** Write a byte to the memory */
  public final void writeByte(int _ea, int _b)
  { cb.write8(_ea & 0xffff, _b & 0xff);
  }

  /** Write 2 bytes to the memory */
  public final void write2Bytes(int _ea, int _b)
  { cb.write8(_ea & 0xffff, (_b >>> 8) & 0xff);
    cb.write8((_ea & 0xffff) + 1, _b & 0xff);
  }

  /** Fetch the byte pointed by pc */
  private final int fetch()
  { return cb.read8(pc++);
  }

  /** Fetch two bytes pointed by pc, MSB first, then LSB. */
  private final int fetch2()
  {
    int temp = cb.read8(pc++) << 8;
    temp = temp | cb.read8(pc++);
    return temp;
  }


//**********************************************************************
// unsigned to signed in int
// undefined if _unsigned is out of range
//
//**********************************************************************
  private int to2C5Bit(int _unsigned) //5-bit 2's complement
  { if (_unsigned > 0xf)
      return _unsigned | 0xffffffe0;
    else
      return _unsigned;
  }

  private int to2C8Bit(int _unsigned) //8-bit 2's complement
  { if (_unsigned > 0x7f)
      return _unsigned | 0xffffff00;
    else
      return _unsigned;
  }

  private int to2C16Bit(int _unsigned) //16-bit 2's complement
  { if (_unsigned > 0x7fff)
      return _unsigned | 0xffff0000;
    else
      return _unsigned;
  }

//**********************************************************************
// Interrupt micro-programs: NMI, IRQ and FIRQ
//
//**********************************************************************
  public void runIrq()
  { flagE = true;  // E = 1

    //System.out.println("I1 ,S=" + Integer.toHexString(s));

    if ( (interruptState & CWAI_STATE) == 0) // if CWAI has not been called
      pull_push8BitBase(false, true, 0xff); // PSHS X,Y,D,U,DP,PC,CC
    else
      interruptState &= ~CWAI_STATE;		// clear CWAI state

    //System.out.println("I2 ,S=" + Integer.toHexString(s));

    flagF = true;
    flagI = true;

    //PC = [$FFF8:$FFF9]
    pc = (readByte(IRQ_VECTOR_HI) << 8) |
          readByte(IRQ_VECTOR_LO);
  }

  public void runFirq()
  { if (waitState == RUNNING_STATE) flagE = false;  // E = 0
    else pull_push8BitBase(false, true, 0x81); // PSHS PC,CC

    flagF = true;
    flagI = true;

    //PC = [$FFF6:$FFF7]
    pc = (readByte(FIRQ_VECTOR_HI) << 8) |
          readByte(FIRQ_VECTOR_LO);
  }

  public void runNmi()
  { waitState = RUNNING_STATE;

    flagE = true; // E = 1
    if (waitState != CWAI_STATE) // if CWAI has not been called
      pull_push8BitBase(false, true, 0xff); // PSHS X,Y,D,U,DP,PC,CC

    flagF = true;
    flagI = true;

    //PC = [$FFFC:$FFFD]
    pc = (readByte(NMI_VECTOR_HI) << 8) |
          readByte(NMI_VECTOR_LO);
  }

//**********************************************************************
// CC setter/getter and D setter/getter
//
//**********************************************************************

  // _val: true = 1; false = 0.
  private final void setCC(int _val)
  { flagC = (_val & C_MASK) != 0;
    flagV = (_val & V_MASK) != 0;
    flagZ = (_val & Z_MASK) != 0;
    flagN = (_val & N_MASK) != 0;
    flagI = (_val & I_MASK) != 0;
    flagH = (_val & H_MASK) != 0;
    flagF = (_val & F_MASK) != 0;
    flagE = (_val & E_MASK) != 0;
  }

  /* package */ final int cc()
  { int cc = 0;
    if (flagC) cc |= C_MASK;
    if (flagV) cc |= V_MASK;
    if (flagZ) cc |= Z_MASK;
    if (flagN) cc |= N_MASK;
    if (flagI) cc |= I_MASK;
    if (flagH) cc |= H_MASK;
    if (flagF) cc |= F_MASK;
    if (flagE) cc |= E_MASK;
    return cc;
  }

  private final int d() //it combines A and B to get register D
  { return (a << 8) | b;
  }

  private final void setD(int _r) //it combines A and B to set register D
  { a = _r >>> 8;
    b = _r & 0xff;
  }

//**********************************************************************
// decode() Decide the addressing modes
//
//**********************************************************************
  /** Set the addressing mode and page. */
  /* this works with AddrModeArray, but ain't no good.
     need further testing, see if it's really faster than the current decoder
     a better test shows it's faster and easier to maintain so use it back
     (v 1.0)

  */
  public int decode()
  { if (ir == 0x10)
      { page = 2;
        ir = cb.read8opc(pc++);
      }
      else if (ir == 0x11)
      { page = 3;
        ir = cb.read8opc(pc++);
      }
      else page = 1;

    int instrCycle = baseCycles[page-1][ir & 0xff];
    addrMode = addrModeArray[ir & 0xff];


    opPc = pc;

    if (addrMode == INHERENT || addrMode == RELATIVE ||
        addrMode == IMMEDIATE) useEa = false;
    else instrCycle += calcEA();

    return instrCycle;
  }

//**********************************************************************
// calcEA() Effective Address Calculation
// according to the addressing mode
//
//**********************************************************************
  private final int calcEA()
  { useEa = true; //set useEa
    int postbyte;
    int rr;
    int reg;
    boolean isIndirect = false;

    int extraCycle = 0;

    switch(addrMode)
    { case EXTENDED:
        ea = fetch2();
        break;
      case DIRECT:
        ea = (dp << 8) | fetch();
        break;
      case INDEXED: //the indexed mode
      { postbyte = fetch();
        rr = (postbyte & 0x60) >>> 5; //the register field
        if (rr == 0) reg = x;
        else if (rr == 1) reg = y;
        else if (rr == 2) reg = u;
        else reg = s;

        if (getBit(postbyte, 7) == 0) //take the 5-bit offset
        { ea = reg + to2C5Bit(postbyte & 0x1f);
        }
        else
        { int lsn = (postbyte & 0x0f); // least significant nibble
          isIndirect = ((postbyte & 0x10) != 0);

          switch(lsn)
          { case 0: // auto post-inc by 1
              ea = reg++;
              tCycle -= 2;
              break;
            case 1: // auto post-inc by 2
              ea = reg;
              reg += 2;
              tCycle -= 3;
              break;
            case 2: // auto pre-dec by 1
              ea = --reg;
              tCycle -= 2;
              break;
            case 3: // auto pre-dec by 2
              reg -= 2;
              ea = reg;
              tCycle -= 3;
              break;
            case 4: // zero offset
              ea = reg;
              // tCycle -= 0; no extra tCycle
              break;
            case 5: // b offset
              ea = reg + to2C8Bit(b);
              tCycle -= 1;
              break;
            case 6: // a offset
              ea = reg + to2C8Bit(a);
              tCycle -= 1;
              break;
            case 8: // 8-bit offset indexed
              ea = reg + to2C8Bit(fetch());
              tCycle -= 1;
              break;
            case 9: // 16-bit offset indexed
              ea = reg + to2C16Bit(fetch2());
              tCycle -= 4;
              break;
            case 11: // d offset
              ea = reg + to2C16Bit(d());
              tCycle -= 4;
              break;
            case 12: // PC + 8-bit
              ea = to2C8Bit(fetch()) + pc;
              tCycle -= 1;
              break;
            case 13: // PC + 16-bit
              ea = to2C16Bit(fetch2()) + pc;
              tCycle -= 5;
              break;
            case 15: // extended indirect
              ea = fetch2();
              tCycle -= 5;
              break;
          }

          if (lsn <= 3) // update auto inc/decrement
          { if (rr == 0) x = reg;
            else if (rr == 1) y = reg;
            else if (rr == 2) u = reg;
            else s = reg;
          }

          if (isIndirect) // indirect address
          { ea = read2Bytes(ea);
            tCycle -= 3; // extra 3 cycles for indirect indexed
          }
        }
      }
    }
    return extraCycle;
  }

  //**********************************************************************
  // Base Micro Programs listed in alphabetical order
  //
  //
  //**********************************************************************
  private boolean isNeg8Bit(int _r)
  { return ((_r & 0x80) != 0);
  }

  private boolean isNeg16Bit(int _r)
  { return ((_r & 0x8000) != 0);
  }

  private int getBit(int _r, int _bit)
  { if ((_r & BIT_MASK[_bit]) == 0) return 0;
    else return 1;
  }

  // done - checked(Nov 2001)
  public int adc8BitBase(int _r)
  { int operand;
    if (!useEa) operand = fetch();
    else operand = readByte(ea);

    int t;
    int cVal = flagC? 1 : 0;

    t = (_r & 0x0f) + (operand & 0x0f) + cVal;
    flagH = (t & 0x10) != 0; // Half carry
    t = (_r & 0x7f) + (operand & 0x7f) + cVal;
    flagV = (t & 0x80) != 0; // Overflow
    t = (_r & 0xff) + (operand & 0xff) + cVal;
    flagC = (t & 0x100) != 0; // Bit 7 Carry out
    _r = t & 0xff;

    flagV = flagV ^ flagC;
    flagN = isNeg8Bit(_r);
    flagZ = _r == 0;

    return _r;
  }

  // done - checked(Nov 2001)
  public int add8BitBase(int _r)
  { int operand;
    if (!useEa) operand = fetch();
    else operand = readByte(ea);

    int t;
    t = (_r & 0x0f) + (operand & 0x0f);
    flagH = (t & 0x10) != 0; // Half carry
    t = (_r & 0x7f) + (operand & 0x7f);
    flagV = (t & 0x80) != 0; // Overflow
    t = (_r & 0xff) + (operand & 0xff);
    flagC = (t & 0x100) != 0; // Bit 7 Carry out
    _r = t & 0xff;

    flagV = flagV ^ flagC; // V = V XOR C for 2C
    flagN = isNeg8Bit(_r);
    flagZ = _r == 0;

    return _r;
  }

  // done - checked(Nov 2001)
  public int and8BitBase(int _r)
  { int operand;
    if (!useEa) operand = fetch();
    else operand = readByte(ea);

    _r = _r & operand;
    flagV = false;
    flagN = isNeg8Bit(_r);
    flagZ = _r == 0;

    return _r;
  }

  // done - checked(Nov 2001)
  public int asl8BitBase(int _r)
  { flagC = getBit(_r, 7) != 0;
    flagV = getBit(_r, 7) != getBit(_r, 6); // bit 7 xor bit 6
    _r = (_r << 1) & 0xff;
    flagN = isNeg8Bit(_r);
    flagZ = _r == 0;
    return _r;
  }

  // done - checked(Nov 2001)
  public int asr8BitBase(int _r)
  { flagC = getBit(_r, 0) != 0;

    _r = (_r >> 1);

    // manually turn on the sign bit coz _r is a 4-byte int
    if (getBit(_r, 6) != 0) _r |= 0x80;
    flagN = isNeg8Bit(_r);
    flagZ = _r == 0;

    return _r;
  }

  // done - checked(Nov 2001)
  public void branchBase()
  {
    int offset;
    if (page == 1)
    { offset = to2C8Bit(fetch());
      pc += offset;
    }
    else // page == 2, long branch
    { offset = to2C16Bit(fetch2());

      pc += offset;
      tCycle--; // an extra cycle if long branch taken
      // exceptions LBRA, LBRN and LBSR. LBRA is offset in its microprog method.
      // LBRN and LBSR won't call branchBase().
    }


  }

  // done - checked(Nov 2001)
  public int clr8BitBase()
  { flagN = false;
    flagZ = true;
    flagV = false;
    flagC = false;
    return 0;
  }

  // done - checked(Nov 2001)
  public void cmp8BitBase(int _r)
  { int operand;
    if (!useEa) operand = fetch();
    else operand = readByte(ea);

    int t = _r - operand;
    flagV = ((_r^operand^t^(t>>1))&0x80) != 0; // Overflow
    flagC = (t & 0x100) != 0; // Bit 8 borrow

    // set N, Z
    _r = t & 0xff;
    flagN = isNeg8Bit(_r);
    flagZ = _r == 0;
  }

  // done - checked(Nov 2001)
  public void cmp16BitBase(int _r)
  { int operand2;
    if (!useEa) operand2 = fetch2();
    else operand2 = read2Bytes(ea);

    int t = _r - operand2;
    flagV = ((_r^operand2^t^(t>>1))&0x8000) != 0; // Overflow
    flagC = (t & 0x10000) != 0; // Bit 8 borrow

    // set N, Z
    _r = t & 0xffff;
    flagN = isNeg16Bit(_r);
    flagZ = _r == 0;
  }

  // done - checked(Nov 2001)
  public int com8BitBase(int _r)
  { flagV = false;
    flagC = true;
    _r = ~_r & 0xff;
    flagN = isNeg8Bit(_r);
    flagZ = _r == 0;
    return _r;
  }

  // done - checked(Nov 2001)
  public int dec8BitBase(int _r)
  { flagV = _r == 0x80;
    _r = (_r - 1) & 0xff;
    flagN = isNeg8Bit(_r);
    flagZ = _r == 0;
    return _r;
  }

  // done - checked(Nov 2001)
  public int eor8BitBase(int _r)
  { int operand;
    if (!useEa) operand = fetch();
    else operand = readByte(ea);

    _r = (_r ^ operand) & 0xff;

    flagV = false;
    flagN = isNeg8Bit(_r);
    flagZ = _r == 0;
    return _r;
  }

  // done - checked(Nov 2001)
  public void exg_tfr8BitBase(boolean _isExg, int _r) //_r post byte
  { int b1 = (_r & 0x70) >>> 4;
    int b2 = _r & 0x07;

    int r1 = 0, r2 = 0;
    int temp;

    if (b1 == 0) r1 = a;
    else if (b1 == 1) r1 = b;
    else if (b1 == 2) r1 = cc();
    else /* b1 = 3 */ r1 = dp;

    if (b2 == 0) r2 = a;
    else if (b2 == 1) r2 = b;
    else if (b2 == 2) r2 = cc();
    else /* b2 = 3 */ r2 = dp;

    if (_isExg)
    { temp = r1;
      r1 = r2;
      r2 = temp;

      if (b1 == 0) a = r1;
      else if (b1 == 1) b = r1;
      else if (b1 == 2) setCC(r1);
      else /* b1 = 3 */ dp = r1;

    }
    else // trf
    { r2 = r1;
    }

    if (b2 == 0) a = r2;
    else if (b2 == 1) b = r2;
    else if (b2 == 2) setCC(r2);
    else /* b2 = 3 */ dp = r2;
  }

  // done - checked(Nov 2001)
  public void exg_tfr16BitBase(boolean _isExg, int _r)
  { int b1 = (_r & 0x70) >>> 4;
    int b2 = _r & 0x07;

    int r1, r2;
    int temp;

    if (b1 == 0) r1 = d();
    else if (b1 == 1) r1 = x;
    else if (b1 == 2) r1 = y;
    else if (b1 == 3) r1 = u;
    else if (b1 == 4) r1 = s;
    else /* b1 = 5 */ r1 = pc;

    if (b2 == 0) r2 = d();
    else if (b2 == 1) r2 = x;
    else if (b2 == 2) r2 = y;
    else if (b2 == 3) r2 = u;
    else if (b2 == 4) r2 = s;
    else /* b2 = 5 */ r2 = pc;

    if (_isExg)
    { temp = r1;
      r1 = r2;
      r2 = temp;
    }
    else // trf
    { r2 = r1;
    }

    if (b1 == 0) setD(r1);
    else if (b1 == 1) x = r1;
    else if (b1 == 2) y = r1;
    else if (b1 == 3) u = r1;
    else if (b1 == 4) s = r1;
    else /* b1 = 5 */ pc = r1;

    if (b2 == 0) setD(r2);
    else if (b2 == 1) x = r2;
    else if (b2 == 2) y = r2;
    else if (b2 == 3) u = r2;
    else if (b2 == 4) s = r2;
    else /* b2 = 5 */ pc = r2;
  }

  // done - checked(Nov 2001)
  public int inc8BitBase(int _r)
  { flagV = _r == 0x7f;
    _r = (_r + 1) & 0xff;
    flagN = isNeg8Bit(_r);
    flagZ = _r == 0;
    return _r;
  }

  // done - checked(Nov 2001)
  public int ld8BitBase()
  { int operand;
    flagV = false;
    if (!useEa) operand = fetch();
    else operand = readByte(ea);
    flagN = isNeg8Bit(operand);
    flagZ = operand == 0;
    return operand;
  }

  // done - checked(Nov 2001)
  public int ld16BitBase()
  { int operand2;
    flagV = false;
    if (!useEa) operand2 = fetch2();
    else operand2 = read2Bytes(ea);
    flagN = isNeg16Bit(operand2);
    flagZ = operand2 == 0;

    return operand2;
  }

  // done - checked(Nov 2001)
  public int lea16BitBase(int _r)
  { return ea & 0xffff;
  }

  // done - checked(Nov 2001)
  public int lsr8BitBase(int _r)
  { flagC = getBit(_r, 0) != 0;
    _r = _r >>> 1;
    flagN = false;
    flagZ = _r == 0;
    return _r;
  }

  // done - checked(Nov 2001)
  public int neg8BitBase(int _r)
  { flagV = _r == 0x80;
    flagC = _r != 0;
    _r = -_r & 0xff;
    flagN = isNeg8Bit(_r);
    flagZ = _r == 0;
    return _r;
  }

  // done - checked(Nov 2001)
  public int or8BitBase(int _r)
  { int operand;
    if (!useEa) operand = fetch();
    else operand = readByte(ea);

    _r = _r | operand;
    flagV = false;
    flagN = isNeg8Bit(_r);
    flagZ = _r == 0;

    return _r;
  }

  // done - checked(Nov 2001)
  public void pull_push8BitBase(boolean _isPull, boolean _isS, int _b)
  { int stack, stack2;
    int postByte = _b;

    if (_isS)
    { stack2 = u;
      stack = s;
    }
    else
    { stack2 = s;
      stack = u;
    }

    if (_isPull) // pulling
    { if (getBit(postByte, 0) != 0)
      { setCC(readByte(stack++));
        tCycle--;
      }
      if (getBit(postByte, 1) != 0)
      { a = readByte(stack++);
        tCycle--;
      }
      if (getBit(postByte, 2) != 0)
      { b = readByte(stack++);
        tCycle--;
      }
      if (getBit(postByte, 3) != 0)
      { dp = readByte(stack++);
        tCycle--;
      }
      if (getBit(postByte, 4) != 0)
      { x = readByte(stack & 0xffff) << 8;
        x = x | readByte((stack & 0xffff) + 1);
        stack += 2;
        tCycle -= 2;
      }
      if (getBit( postByte, 5) != 0)
      { y = read2Bytes(stack);
        stack += 2;
        tCycle -= 2;
      }
      if (getBit( postByte, 6) != 0)
      { stack2 = read2Bytes(stack);
        stack += 2;
        if (_isS) u = stack2;
        else s = stack2;
        tCycle -= 2;
      }
      if (getBit( postByte, 7) != 0)
      { pc = read2Bytes(stack);
        stack += 2;
        tCycle -= 2;
      }
    }
    else // pushing
    { if (getBit(postByte,  7) != 0)
      { stack -=2;
        write2Bytes(stack, pc);
        tCycle -= 2;
      }
      if (getBit(postByte,  6) != 0)
      { stack -=2;
        write2Bytes(stack, stack2);
        tCycle -= 2;
      }
      if (getBit(postByte,  5) != 0)
      { stack -=2;
        write2Bytes(stack, y);
        tCycle -= 2;
      }
      if (getBit(postByte,  4) != 0)
      { stack -=2;
        write2Bytes(stack, x);
        tCycle -= 2;
      }
      if (getBit(postByte,  3) != 0)
      { writeByte(--stack, dp);
        tCycle--;
      }
      if (getBit(postByte,  2) != 0)
      { writeByte(--stack, b);
        tCycle--;
      }
      if (getBit(postByte,  1) != 0)
      { writeByte(--stack, a);
        tCycle--;
      }
      if (getBit(postByte,  0) != 0)
      { writeByte(--stack, cc());
        tCycle--;
      }
    }
    if (_isS)
      s = stack & 0xffff;
    else
      u = stack & 0xffff;
  }

  // done - checked(Nov 2001)
  public int rol8BitBase(int _r)
  { flagV = (getBit(_r, 6) != getBit(_r, 7));

    boolean lastC = flagC;
    flagC = getBit(_r, 7) != 0;
    _r = (_r << 1) & 0xff;
    if (lastC) _r |= 0x01;
    else _r &= 0xfe;

    flagN = isNeg8Bit(_r);
    flagZ = _r == 0;
    return _r;
  }

  // done - checked(Nov 2001)
  public int ror8BitBase(int _r)
  { boolean lastC = flagC;

    flagC = getBit(_r, 0) != 0;
    _r = (_r >>> 1) & 0xff;
    if (lastC) _r |= 0x80;
    else _r &= 0x7f;

    flagN = isNeg8Bit(_r);
    flagZ = _r == 0;
    return _r;
  }

  // done - checked(Nov 2001)
  public int sub8BitBase(int _r)
  { int operand;
    if (!useEa) operand = fetch();
    else operand = readByte(ea);

    int t = _r - operand;
    flagV = ((_r^operand^t^(t>>1))&0x80) != 0; // Overflow
    flagC = (t & 0x100) != 0; // Bit 8 borrow

    // set N, Z
    _r = t & 0xff;
    flagN = isNeg8Bit(_r);
    flagZ = _r == 0;

    return _r;
  }



  // done - checked(Nov 2001)
  public int sbc8BitBase(int _r)
  { int operand;
    if (!useEa) operand = fetch();
    else operand = readByte(ea);

    int t;
    if (flagC) t = _r - operand - 1;
    else t = _r - operand;
    flagV = ((_r^operand^t^(t>>1))&0x80) != 0; // Overflow
    flagC = (t & 0x100) != 0; // Bit 8 borrow

    // set N, Z
    _r = t & 0xff;
    flagN = isNeg8Bit(_r);
    flagZ = _r == 0;

    return _r;
  }

  // done - checked(Nov 2001)
  public void st8BitBase(int _r)
  { if (useEa)
    { writeByte(ea, _r);
    }
    else
    { writeByte(pc++, _r);
    }

    flagV = false;
    flagN = isNeg8Bit(_r);
    flagZ = _r == 0;
  }

  // done - checked(Nov 2001)
  public void st16BitBase(int _r)
  { if (useEa)
    { write2Bytes(ea, _r);
    }
    else
    { write2Bytes(pc, _r);
      pc += 2;
    }
    flagV = false;
    flagN = isNeg16Bit(_r);
    flagZ = _r == 0;
  }

  // done - checked(Nov 2001)
  public void tst8BitBase(int _r)
  { flagV = false;
    flagN = isNeg8Bit(_r);
    flagZ = _r == 0;
  }

  //**********************************************************************
  // Instruction chooser and instructions
  // a few codes implemented directly in this section
  //
  //**********************************************************************
  private void execSingleInstr(int _opcode)
  { switch(_opcode)
    { case 0x0: neg(); break;
      case 0x3: com(); break;
      case 0x4: lsr(); break;
      case 0x6: ror(); break;
      case 0x7: asr(); break;
      case 0x8: asl(); break;
      case 0x9: rol(); break;
      case 0xa: dec(); break;
      case 0xc: inc(); break;
      case 0xd: tst(); break;
      case 0xe: jmp(); break;
      case 0xf: clr(); break;
      case 0x12: nop(); break;
      case 0x13: sync(); break;
      case 0x16: lbra(); break;
      case 0x17: lbsr(); break;
      case 0x19: daa(); break;
      case 0x1a: orcc(); break;
      case 0x1c: andcc(); break;
      case 0x1d: sex(); break;
      case 0x1e: exg(); break;
      case 0x1f: tfr(); break;
      case 0x20: bra(); break;
      case 0x21: xbrn(); break;
      case 0x22: xbhi(); break;
      case 0x23: xbls(); break;
      case 0x24: xbcc(); break;
      case 0x25: xbcs(); break;
      case 0x26: xbne(); break;
      case 0x27: xbeq(); break;
      case 0x28: xbvc(); break;
      case 0x29: xbvs(); break;
      case 0x2a: xbpl(); break;
      case 0x2b: xbmi(); break;
      case 0x2c: xbge(); break;
      case 0x2d: xblt(); break;
      case 0x2e: xbgt(); break;
      case 0x2f: xble(); break;
      case 0x30: leax(); break;
      case 0x31: leay(); break;
      case 0x32: leas(); break;
      case 0x33: leau(); break;
      case 0x34: pshs(); break;
      case 0x35: puls(); break;
      case 0x36: pshu(); break;
      case 0x37: pulu(); break;
      case 0x39: rts(); break;
      case 0x3a: abx(); break;
      case 0x3b: rti(); break;
      case 0x3c: cwai(); break;
      case 0x3d: mul(); break;
      case 0x3f: swi(); break;
      case 0x40: nega(); break;
      case 0x43: coma(); break;
      case 0x44: lsra(); break;
      case 0x46: rora(); break;
      case 0x47: asra(); break;
      case 0x48: asla(); break;
      case 0x49: rola(); break;
      case 0x4a: deca(); break;
      case 0x4c: inca(); break;
      case 0x4d: tsta(); break;
      case 0x4f: clra(); break;
      case 0x50: negb(); break;
      case 0x53: comb(); break;
      case 0x54: lsrb(); break;
      case 0x56: rorb(); break;
      case 0x57: asrb(); break;
      case 0x58: aslb(); break;
      case 0x59: rolb(); break;
      case 0x5a: decb(); break;
      case 0x5c: incb(); break;
      case 0x5d: tstb(); break;
      case 0x5f: clrb(); break;
      case 0x60: neg(); break;
      case 0x63: com(); break;
      case 0x64: lsr(); break;
      case 0x66: ror(); break;
      case 0x67: asr(); break;
      case 0x68: asl(); break;
      case 0x69: rol(); break;
      case 0x6a: dec(); break;
      case 0x6c: inc(); break;
      case 0x6d: tst(); break;
      case 0x6e: jmp(); break;
      case 0x6f: clr(); break;
      case 0x70: neg(); break;
      case 0x73: com(); break;
      case 0x74: lsr(); break;
      case 0x76: ror(); break;
      case 0x77: asr(); break;
      case 0x78: asl(); break;
      case 0x79: rol(); break;
      case 0x7a: dec(); break;
      case 0x7c: inc(); break;
      case 0x7d: tst(); break;
      case 0x7e: jmp(); break;
      case 0x7f: clr(); break;
      case 0x80: suba(); break;
      case 0x81: cmpa(); break;
      case 0x82: sbca(); break;
      case 0x83: subd_cmpd_cmpu(); break;
      case 0x84: anda(); break;
      case 0x85: bita(); break;
      case 0x86: lda(); break;
      case 0x88: eora(); break;
      case 0x89: adca(); break;
      case 0x8a: ora(); break;
      case 0x8b: adda(); break;
      case 0x8c: cmpx_y_s(); break;
      case 0x8d: bsr(); break;
      case 0x8e: ldx_y(); break;
      case 0x90: suba(); break;
      case 0x91: cmpa(); break;
      case 0x92: sbca(); break;
      case 0x93: subd_cmpd_cmpu(); break;
      case 0x94: anda(); break;
      case 0x95: bita(); break;
      case 0x96: lda(); break;
      case 0x97: sta(); break;
      case 0x98: eora(); break;
      case 0x99: adca(); break;
      case 0x9a: ora(); break;
      case 0x9b: adda(); break;
      case 0x9c: cmpx_y_s(); break;
      case 0x9d: jsr(); break;
      case 0x9e: ldx_y(); break;
      case 0x9f: stx_y(); break;
      case 0xa0: suba(); break;
      case 0xa1: cmpa(); break;
      case 0xa2: sbca(); break;
      case 0xa3: subd_cmpd_cmpu(); break;
      case 0xa4: anda(); break;
      case 0xa5: bita(); break;
      case 0xa6: lda(); break;
      case 0xa7: sta(); break;
      case 0xa8: eora(); break;
      case 0xa9: adca(); break;
      case 0xaa: ora(); break;
      case 0xab: adda(); break;
      case 0xac: cmpx_y_s(); break;
      case 0xad: jsr(); break;
      case 0xae: ldx_y(); break;
      case 0xaf: stx_y(); break;
      case 0xb0: suba(); break;
      case 0xb1: cmpa(); break;
      case 0xb2: sbca(); break;
      case 0xb3: subd_cmpd_cmpu(); break;
      case 0xb4: anda(); break;
      case 0xb5: bita(); break;
      case 0xb6: lda(); break;
      case 0xb7: sta(); break;
      case 0xb8: eora(); break;
      case 0xb9: adca(); break;
      case 0xba: ora(); break;
      case 0xbb: adda(); break;
      case 0xbc: cmpx_y_s(); break;
      case 0xbd: jsr(); break;
      case 0xbe: ldx_y(); break;
      case 0xbf: stx_y(); break;
      case 0xc0: subb(); break;
      case 0xc1: cmpb(); break;
      case 0xc2: sbcb(); break;
      case 0xc3: addd(); break;
      case 0xc4: andb(); break;
      case 0xc5: bitb(); break;
      case 0xc6: ldb(); break;
      case 0xc8: eorb(); break;
      case 0xc9: adcb(); break;
      case 0xca: orb(); break;
      case 0xcb: addb(); break;
      case 0xcc: ldd(); break;
      case 0xce: ldu_s(); break;
      case 0xd0: subb(); break;
      case 0xd1: cmpb(); break;
      case 0xd2: sbcb(); break;
      case 0xd3: addd(); break;
      case 0xd4: andb(); break;
      case 0xd5: bitb(); break;
      case 0xd6: ldb(); break;
      case 0xd7: stb(); break;
      case 0xd8: eorb(); break;
      case 0xd9: adcb(); break;
      case 0xda: orb(); break;
      case 0xdb: addb(); break;
      case 0xdc: ldd(); break;
      case 0xdd: std(); break;
      case 0xde: ldu_s(); break;
      case 0xdf: stu_s(); break;
      case 0xe0: subb(); break;
      case 0xe1: cmpb(); break;
      case 0xe2: sbcb(); break;
      case 0xe3: addd(); break;
      case 0xe4: andb(); break;
      case 0xe5: bitb(); break;
      case 0xe6: ldb(); break;
      case 0xe7: stb(); break;
      case 0xe8: eorb(); break;
      case 0xe9: adcb(); break;
      case 0xea: orb(); break;
      case 0xeb: addb(); break;
      case 0xec: ldd(); break;
      case 0xed: std(); break;
      case 0xee: ldu_s(); break;
      case 0xef: stu_s(); break;
      case 0xf0: subb(); break;
      case 0xf1: cmpb(); break;
      case 0xf2: sbcb(); break;
      case 0xf3: addd(); break;
      case 0xf4: andb(); break;
      case 0xf5: bitb(); break;
      case 0xf6: ldb(); break;
      case 0xf7: stb(); break;
      case 0xf8: eorb(); break;
      case 0xf9: adcb(); break;
      case 0xfa: orb(); break;
      case 0xfb: addb(); break;
      case 0xfc: ldd(); break;
      case 0xfd: std(); break;
      case 0xfe: ldu_s(); break;
      case 0xff: stu_s(); break;
      default: /* invalid instruction ! */
    }
  }


  private void abx()
  { x = (x + b) & 0xffff; // unsigned addition
  }

  private void adca()
  { a = adc8BitBase(a); }

  private void adcb()
  { b = adc8BitBase(b); }

  private void adda()
  { a = add8BitBase(a); }

  private void addb()
  { b = add8BitBase(b); }

  private void addd()
  { int t;
    int operand2;
    if (useEa)
      operand2 = read2Bytes(ea);
    else
      operand2 = fetch2();
    int d = d();

    t = (d & 0x7fff) + (operand2 & 0x7fff);
    flagV = getBit(t, 15) != 0; // Overflow

    t = (d & 0xffff) + (operand2 & 0xffff);
    flagC = getBit(t, 16) != 0; // Bit 16 Carry out

    flagV = flagV ^ flagC;
    d = (t & 0xffff);
    flagN = isNeg16Bit(d);
    flagZ = d == 0;
    setD(d);
  }
  private void anda()
  { a = and8BitBase(a); }

  private void andb()
  { b = and8BitBase(b); }

  private void andcc()
  { setCC(cc() & fetch()); }

  private void asla()
  { a = asl8BitBase(a); }

  private void aslb()
  { b = asl8BitBase(b); }

  private void asl()
  { int operand;
    if (!useEa)
    { operand = readByte(pc);
      writeByte(pc++, asl8BitBase(operand));
    }
    else
    { operand = readByte(ea);
      writeByte(ea, asl8BitBase(operand));
    }
  }

  private void asra()
  { a = asr8BitBase(a); }

  private void asrb()
  { b = asr8BitBase(b); }

  private void asr()
  { int operand;
    if (!useEa)
    { operand = readByte(pc);
      writeByte(pc++, asr8BitBase(operand));
    }
    else
    { operand = readByte(ea);
      writeByte(ea, asr8BitBase(operand));
    }
  }
  private void xbcc()
  { if (!flagC) branchBase();
    else
    { if (page == 1) pc++;
      else pc += 2;
    }
  }

  private void xbcs()
  { if (flagC) branchBase();
    else
    { if (page == 1) pc++;
      else pc += 2;
    }
  }
  private void xbeq()
  { if (flagZ) branchBase();
    else
    { if (page == 1) pc++;
      else pc += 2;
    }
  }
  private void xbge()
  { if (flagN ^ flagV)
    { if (page == 1) pc++;
      else pc += 2;
    }
    else branchBase();
  }
  private void xbgt()
  { if (flagZ || (flagN ^ flagV))
    { if (page == 1) pc++;
      else pc += 2;
    }
    else branchBase();
  }
  private void xbhi()
  { if (flagC | flagZ)
    { if (page == 1) pc++;
      else pc += 2;
    }
    else branchBase();
  }
  private void bita()
  { and8BitBase(a); } // just update the flags, discard the outcome
  private void bitb()
  { and8BitBase(b); } // just update the flags, discard the outcome
  private void xble()
  { if (flagZ || (flagN ^ flagV))
      branchBase();
    else
    { if (page == 1) pc++;
      else pc += 2;
    }
  }
  private void xbls()
  { if (flagC || flagZ) branchBase();
    else
    { if (page == 1) pc++;
      else pc += 2;
    }
  }
  private void xblt()
  { if (flagN ^ flagV) branchBase();
    else
    { if (page == 1) pc++;
      else pc += 2;
    }
  }

  private void xbmi()
  { if (flagN) branchBase();
    else
    { if (page == 1) pc++;
      else pc += 2;
    }
  }
  private void xbne()
  { if (!flagZ) branchBase();
    else
    { if (page == 1) pc++;
      else pc += 2;
    }
  }
  private void xbpl()
  { if (!flagN) branchBase();
    else
    { if (page == 1) pc++;
      else pc += 2;
    }
  }
  private void bra()
  { branchBase();
  }
  private void lbra()
  { // artificially change page to 2 coz it's an exception:
    // a long branch with single byte op code
    page = 2;
    branchBase();
    tCycle++; // exception: offset the extra cycle in branchBase()
  }
  private void xbrn()
  { if (page == 1) pc++;
    else pc += 2;
  }
  private void bsr()
  { // can't use branchBase()
    int operand = fetch();
    pull_push8BitBase(false, true, 0x80); // tCycle -= 2;
    tCycle += 2;
    pc += to2C8Bit(operand);
  }
  private void lbsr()
  { // artificially change it coz it's an exception:
    // a long branch with single byte op code

    page = 2;
    int operand2 = fetch2();
    pull_push8BitBase(false, true, 0x80); // tCycle -= 2;
    tCycle += 2;
    pc += to2C16Bit(operand2);
  }
  private void xbvc()
  { if (!flagV) branchBase();
    else
    { if (page == 1) pc++;
      else pc += 2;
    }
  }
  private void xbvs()
  { if (flagV) branchBase();
    else
    { if (page == 1) pc++;
      else pc += 2;
    }
  }
  private void clra()
  { a = clr8BitBase(); }
  private void clrb()
  { b = clr8BitBase(); }
  private void clr()
  { if (!useEa)
    { writeByte(pc++, clr8BitBase());
    }
    else
    { writeByte(ea, clr8BitBase());
    }
  }
  private void cmpa()
  { cmp8BitBase(a); }
  private void cmpb()
  { cmp8BitBase(b); }
  private void cmpx_y_s()
  { if (page == 1)
    { cmp16BitBase(x);
    }
    else if (page == 2)
    { cmp16BitBase(y);
    }
    else
    { cmp16BitBase(s);
    }
  }
  private void coma()
  { a = com8BitBase(a); }
  private void comb()
  { b = com8BitBase(b); }
  private void com()
  { int operand;
    if (!useEa)
    { operand = readByte(pc);
      writeByte(pc++, com8BitBase(operand));
    }
    else
    { operand = readByte(ea);
      writeByte(ea, com8BitBase(operand));
    }
  }

  // done - not checked
  private void cwai()
  { int tempCC = cc() & fetch(); // and the following byte with CC
    tempCC |= E_MASK; // set E
    setCC(tempCC);
    pull_push8BitBase(false, true, 0xff); //push everything in S
    waitState = CWAI_STATE;

    interruptState |= CWAI_STATE;

    tCycle = 0;
  }

  // done - checked(Jan 2002)
  private void daa()
  { int lsn = (a & 0x0f), msn = (a & 0xf0);
    int temp = 0, cf = 0;

    if ((lsn > 9) || flagH) cf |= 0x06;
    if ((msn > 0x80 && lsn > 0x09) || flagC || msn > 0x90) cf |= 0x60;

    temp = cf + a;
    flagC = ((temp & 0x100) != 0) || flagC;
    a = temp & 0xff;
    flagZ = a == 0;
    flagN = isNeg8Bit(a);
  }
  private void deca()
  { a = dec8BitBase(a); }
  private void decb()
  { b = dec8BitBase(b); }
  private void dec()
  { int operand;
    if (!useEa)
    { operand = readByte(pc);
      writeByte(pc++, dec8BitBase(operand));
    }
    else
    { operand = readByte(ea);
      writeByte(ea, dec8BitBase(operand));
    }
  }
  private void eora()
  { a = eor8BitBase(a); }
  private void eorb()
  { b= eor8BitBase(b); }
  private void exg()
  { int operand = fetch();
    if (getBit(operand, 7) == 0) //check bit 7
      exg_tfr16BitBase(true, operand);
    else
      exg_tfr8BitBase(true, operand);
  }
  private void inca()
  { a = inc8BitBase(a); }
  private void incb()
  { b = inc8BitBase(b); }
  private void inc()
  { int operand;
    if (!useEa)
    { operand = readByte(pc);
      writeByte(pc++, inc8BitBase(operand));
    }
    else
    { operand = readByte(ea);
      writeByte(ea, inc8BitBase(operand));
    }
  }
  private void jmp()
  { pc = ea;
  }
  private void jsr()
  { pull_push8BitBase(false, true, 0x80); //push pc in system stack
    tCycle += 2;
    pc = ea;
  }
  private void lda()
  { a = ld8BitBase(); }
  private void ldb()
  { b = ld8BitBase(); }
  private void ldd()
  {
    setD(ld16BitBase());
  }
  private void ldx_y()
  { if (page == 1)
      x = ld16BitBase();
    else
      y = ld16BitBase();
  }
  private void ldu_s()
  { if (page == 1)
      u = ld16BitBase();
    else
      s = ld16BitBase();
  }
  private void leax()
  { x = lea16BitBase(x);
    flagZ = x == 0;
  }
  private void leay()
  { y = lea16BitBase(y);
    flagZ = y == 0;
  }
  private void leas()
  { s = lea16BitBase(s); }
  private void leau()
  { u = lea16BitBase(u); }
  private void lsra()
  { a = lsr8BitBase(a); }
  private void lsrb()
  { b = lsr8BitBase(b); }
  private void lsr()
  { int operand;
    if (!useEa)
    { operand = readByte(pc);
      writeByte(pc++, lsr8BitBase(operand));
    }
    else
    { operand = readByte(ea);
      writeByte(ea, lsr8BitBase(operand));
    }
  }

  // done - checked(Jan 2002)
  private void mul()
  { flagZ = a == 0 || b == 0;
    flagC = (((a & 0xff) * (b & 0xff)) & 0x80) != 0;
    setD(((a & 0xff) * (b & 0xff)) & 0xffff);
  }
  private void nega()
  { a = neg8BitBase(a); }
  private void negb()
  { b = neg8BitBase(b); }
  private void neg()
  { int operand;
    if (!useEa)
    { operand = readByte(pc);
      writeByte(pc++, neg8BitBase(operand));
    }
    else
    { operand = readByte(ea);
      writeByte(ea, neg8BitBase(operand));
    }
  }
  private void nop()
  { /* nothing */
  }
  private void ora()
  { a = or8BitBase(a); }
  private void orb()
  { b = or8BitBase(b); }
  private void orcc()
  { setCC(cc() | fetch()); }
  private void pshs()
  { pull_push8BitBase(false, true, fetch());
  }
  private void pshu()
  { pull_push8BitBase(false, false, fetch());
  }
  private void puls()
  { pull_push8BitBase(true, true, fetch());
  }
  private void pulu()
  { pull_push8BitBase(true, false, fetch());
  }
  private void rora()
  { a = ror8BitBase(a); }
  private void rorb()
  { b = ror8BitBase(b); }
  private void ror()
  { int operand;
    if (!useEa)
    { operand = readByte(pc);
      writeByte(pc++, ror8BitBase(operand));
    }
    else
    { operand = readByte(ea);
      writeByte(ea, ror8BitBase(operand));
    }
  }
  private void rola()
  { a = rol8BitBase(a); }
  private void rolb()
  { b = rol8BitBase(b); }
  private void rol()
  { int operand;
    if (!useEa)
    { operand = readByte(pc);
      writeByte(pc++, rol8BitBase(operand));
    }
    else
    { operand = readByte(ea);
      writeByte(ea, rol8BitBase(operand));
    }
  }
  private void rti()
  { pull_push8BitBase(true, true, 0x1); // get CC first, tCycle--;
    tCycle++;

    if (flagE)
    { pull_push8BitBase(true, true, 0xfe); //get the rest, tCycle -= 11;
      tCycle -= 4; // extra = 4: -11 - 4 = -15
    }
    else
    { pull_push8BitBase(true, true, 0x80); //recover PC only, tCycle -= 9;
      tCycle += 3; // extra = 3: -9 + 3 = -6
    }
  }
  private void rts()
  { pull_push8BitBase(true, true, 0x80); //pull pc back from system stack
    tCycle += 2; // offsets pull_push8BitBase()
  }
  private void sbca()
  { a = sbc8BitBase(a); }
  private void sbcb()
  { b = sbc8BitBase(b); }
  private void sex()
  { if (isNeg8Bit(b))
       a = 0xff;
    else a = 0;

    flagV = false;
    flagN = isNeg8Bit(b);
    flagZ = b == 0;
  }
  private void sta()
  { st8BitBase(a); }
  private void stb()
  { st8BitBase(b); }
  private void std()
  { st16BitBase(d()); }
  private void stx_y()
  { if (page == 1) st16BitBase(x);
    else st16BitBase(y);
  }
  private void stu_s()
  { if (page == 1) st16BitBase(u);
    else st16BitBase(s);
  }
  private void suba()
  { a = sub8BitBase(a); }

  private void subb()
  { b = sub8BitBase(b); }


  private void subd_cmpd_cmpu()
  { if (page == 1) //subd
    { int d = d();
      int operand2;
      if (!useEa) operand2 = fetch2();
      else operand2 = read2Bytes(ea);

      int t = d - operand2;
      flagV = ((d ^ operand2 ^ t ^ (t >> 1)) & 0x8000) != 0; // Overflow
      flagC = (t & 0x10000) != 0; // Bit 8 borrow

      // set N, Z
      d = t & 0xffff;
      flagN = isNeg16Bit(d);
      flagZ = d == 0;
      setD(d);
    }
    else if (page == 2)
    { cmp16BitBase(d());
    }
    else
    { cmp16BitBase(u);
    }
  }
  private void swi()
  { flagE = true;
    pull_push8BitBase(false, true, 0xff); //push everything in S
    tCycle += 12;
    flagI = true;
    flagF = true;
    pc = (readByte(SWI_VECTOR_HI) << 8) |
              readByte(SWI_VECTOR_LO);

  }
  private void swi2()
  { flagE = true;
    pull_push8BitBase(false, true, 0xff); //push everything in S
    tCycle += 12;
    pc = (readByte(SWI2_VECTOR_HI) << 8) |
              readByte(SWI2_VECTOR_LO);
  }
  private void swi3()
  { flagE = true;
    pull_push8BitBase(false, true, 0xff); //push everything in S
    tCycle += 12;
    pc = (readByte(SWI3_VECTOR_HI) << 8) |
              readByte(SWI3_VECTOR_LO);
  }

  // done - not checked
  private void sync()
  { waitState = SYNC_STATE;
    tCycle = 0;
  }
  private void tfr()
  { int operand = fetch();
    if (getBit(operand, 7) == 0) //check bit 7 (bit 3 of the most significant nibble)
      exg_tfr16BitBase(false, operand);
    else
      exg_tfr8BitBase(false, operand);
  }
  private void tsta()
  { tst8BitBase(a); }
  private void tstb()
  { tst8BitBase(b); }
  private void tst()
  { int operand;
    if (!useEa)
    { operand = fetch();
      tst8BitBase(operand);
    }
    else
    { operand = readByte(ea);
      tst8BitBase(operand);
    }
  }

/* (non-Javadoc)
 * @see jef.cpu.Cpu#getCyclesLeft()
 */
public int getCyclesLeft() {
	// TODO Auto-generated method stub
	return 0;
}
}
