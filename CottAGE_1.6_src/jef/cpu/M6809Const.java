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

//**********************************************************************
//* Constants for CPU6809 v1.0
//* By S.C.W. Jan 2002
//*
//* Updates
//* 23-may-'02 : Removed some unused constants and made all
//*				 constants static final. [Erik Duijs]
//**********************************************************************

/**
 * Constants for M6809.java
 * 
 * @author S.C. Wong
 * 
 * M6809Const.java */
public interface M6809Const
{

  // addressing modes
  static final int IMMEDIATE = 0, DIRECT   = 1, INDEXED  = 2;
  static final int EXTENDED  = 3, INHERENT = 4, RELATIVE = 5;

  // irq Vector Table
  static final int RESET_VECTOR_HI    = 0xfffe;
  static final int RESET_VECTOR_LO    = 0xffff;
  static final int NMI_VECTOR_HI      = 0xfffc;
  static final int NMI_VECTOR_LO      = 0xfffd;
  static final int SWI_VECTOR_HI      = 0xfffa;
  static final int SWI_VECTOR_LO      = 0xfffb;
  static final int IRQ_VECTOR_HI      = 0xfff8;
  static final int IRQ_VECTOR_LO      = 0xfff9;
  static final int FIRQ_VECTOR_HI     = 0xfff6;
  static final int FIRQ_VECTOR_LO     = 0xfff7;
  static final int SWI2_VECTOR_HI     = 0xfff4;
  static final int SWI2_VECTOR_LO     = 0xfff5;
  static final int SWI3_VECTOR_HI     = 0xfff2;
  static final int SWI3_VECTOR_LO     = 0xfff3;

  // reserved
  static final int RESERVED_VECTOR_HI = 0xfff0;
  static final int RESERVED_VECTOR_LO = 0xfff1;


  // for addr mode look up.
  // 9 = invalid instruction
  static final int[] addrModeArray=
  { 1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1, //0
    9,9,4,4,9,9,5,5,9,4,0,9,0,4,0,0, //1
    5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5, //2
    2,2,2,2,0,0,0,0,4,4,4,4,4,4,4,4, //3
    4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4, //4
    4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4, //5
    2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2, //6
    3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3, //7
    0,0,0,0,0,0,0,0,0,0,0,0,0,5,0,9, //8
    1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1, //9
    2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2, //a
    3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3, //b
    0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0, //c
    1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1, //d
    2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2, //e
    3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3, //f
  //0 1 2 3 4 5 6 7 8 9 a b c d e f
  };

  /* base # CPU cycles
   * indices are the instruction opcode
   * illegal instructions have ~=1
   * special attention:
   * CWAI(opcode=3C), PULs and PSHs(opcode=34-37),
   * RTI(opcode=3B), SYNC(opcode=13),
   * and long branches
   */
  static final int[][] baseCycles =
      { // page 1
        { 6, 1, 1, 6, 6, 1, 6, 6, 6, 6, 6, 1, 6, 6, 3, 6,   //0
          1, 1, 2, 2, 1, 1, 5, 9, 1, 2, 3, 1, 3, 2, 8, 7,   //1
          3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3,   //2
          4, 4, 4, 4, 5, 5, 5, 5, 1, 5, 3, 6,21,11, 1,19,   //3
          2, 1, 1, 2, 2, 1, 2, 2, 2, 2, 2, 1, 2, 2, 1, 2,   //4
          2, 1, 1, 2, 2, 1, 2, 2, 2, 2, 2, 1, 2, 2, 1, 2,   //5
          6, 1, 1, 6, 6, 1, 6, 6, 6, 6, 6, 1, 6, 6, 3, 6,   //6
          7, 1, 1, 7, 7, 1, 7, 7, 7, 7, 7, 1, 7, 7, 3, 7,   //7
          2, 2, 2, 4, 2, 2, 2, 1, 2, 2, 2, 2, 4, 7, 3, 1,   //8
          4, 4, 4, 6, 4, 4, 4, 4, 4, 4, 4, 4, 6, 7, 5, 5,   //9
          4, 4, 4, 6, 4, 4, 4, 4, 4, 4, 4, 4, 6, 7, 5, 5,   //a
          5, 5, 5, 7, 5, 5, 5, 5, 5, 5, 5, 5, 7, 8, 6, 6,   //b
          2, 2, 2, 4, 2, 2, 2, 1, 2, 2, 2, 2, 3, 1, 3, 1,   //c
          4, 4, 4, 6, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5,   //d
          4, 4, 4, 6, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5,   //e
          5, 5, 5, 7, 5, 5, 5, 5, 5, 5, 5, 5, 6, 6, 6, 6 }, //f
       // 0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f

        // page 2
        { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,   //0
          1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,   //1
          1, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,   //2
          1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,20,   //3
          1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,   //4
          1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,   //5
          1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,   //6
          1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,   //7
          1, 1, 1, 5, 1, 1, 1, 1, 1, 1, 1, 1, 5, 1, 4, 1,   //8
          1, 1, 1, 7, 1, 1, 1, 1, 1, 1, 1, 1, 7, 1, 6, 6,   //9
          1, 1, 1, 7, 1, 1, 1, 1, 1, 1, 1, 1, 7, 1, 6, 6,   //a
          1, 1, 1, 8, 1, 1, 1, 1, 1, 1, 1, 1, 8, 1, 7, 7,   //b
          1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 4, 1,   //c
          1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 6, 6,   //d
          1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 6, 6,   //e
          1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 7, 7 }, //f
       // 0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f

        // page 3
        { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,   //0
          1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,   //1
          1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,   //2
          1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,20,   //3
          1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,   //4
          1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,   //5
          1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,   //6
          1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,   //7
          1, 1, 1, 5, 1, 1, 1, 1, 1, 1, 1, 1, 5, 1, 1, 1,   //8
          1, 1, 1, 7, 1, 1, 1, 1, 1, 1, 1, 1, 7, 1, 1, 1,   //9
          1, 1, 1, 7, 1, 1, 1, 1, 1, 1, 1, 1, 7, 1, 1, 1,   //a
          1, 1, 1, 8, 1, 1, 1, 1, 1, 1, 1, 1, 8, 1, 1, 1,   //b
          1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,   //c
          1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,   //d
          1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,   //e
          1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}}; //f
       // 0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f

  // Bit-masks
  static final int[] BIT_MASK =
  { 0x0001,
    0x0002,
    0x0004,
    0x0008,
    0x0010,
    0x0020,
    0x0040,
    0x0080,
    0x0100,
    0x0200,
    0x0400,
    0x0800,
    0x1000,
    0x2000,
    0x4000,
    0x8000,
    0x10000, // these four for the carry/borrow bit
    0x20000,
    0x40000,
    0x80000
  };

  // CC constants
  static final int C_MASK = BIT_MASK[0];      //00000001binary
  static final int V_MASK = BIT_MASK[1];      //00000010binary
  static final int Z_MASK = BIT_MASK[2];      //00000100binary
  static final int N_MASK = BIT_MASK[3];      //00001000binary
  static final int I_MASK = BIT_MASK[4];      //00010000binary
  static final int H_MASK = BIT_MASK[5];      //00100000binary
  static final int F_MASK = BIT_MASK[6];      //01000000binary
  static final int E_MASK = BIT_MASK[7];      //10000000binary


  // irq states
  static final int NO_IRQ   = 0; //00000000binary
  static final int SEND_NMI  = 1; //00000001binary
  static final int SEND_IRQ  = 2; //00000010binary
  static final int SEND_FIRQ = 4; //00000100binary
  // wait states - Jan 2002, cwai- or sync-related
  static final int CWAI_STATE = 8;
  static final int SYNC_STATE = 16;
  static final int LDS_STATE  = 32;
  static final int RUNNING_STATE = 0;

}
