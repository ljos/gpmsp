/***************************************************************************

"Kabuki" Z80 encryption


The "Kabuki" is a custom Z80 module which runs encrypted code. The encryption
key is stored in some battery-backed RAM, therefore the chip has the annoying
habit of stopping working every few years, when the battery dies.
Check at the bottom of this text to see a list of all the known games which
use this chip.


How it works:
The base operation is a bit swap which affects couples of adjacent bits.
Each of the 4 couples may or may not be swapped, depending on the address of
the byte and on whether it is an opcode or data.
The decryption consists of these steps:
- bitswap
- ROL
- bitswap
- XOR with a key
- ROL
- bitswap
- ROL
- bitswap

To know how to apply the bit swap, take the address of the byte to decode and:
- if the byte is an opcode, add addr_key to the address
- if the byte is data, XOR the address with 1FC0, add 1, and then add addr_key
You'll get a 16-bit word. The first two bitswaps depend on bits 0-7 of that
word, while the second two on bits 8-15. When a bit in the word is 1, swap the
two bits, oherwise don't. The exact couple of bits affected depends on the
game and is identified in this file with two keys: swap_key1 and swap_key2
(which are just permutations of the numbers 0-7, not full 32-bit integers).


Key space size:
- swap_key1  8! = 40320
- swap_key2  8! = 40320
- addr_key   2^16 = 65536
- xor_key    2^8 = 256
- total      2.7274 * 10^16


Weaknesses:
- 0x00 and 0xff, having all the bits set to the same value, are not affected
  by bit permutations after the XOR. Therefore, their encryption is the same
  regardless of the high 8 bits of the address, and of the value of
  swap_key2. If there is a long stream of 0x00 or 0xff in the original data,
  this can be used to find by brute force all the candidates for swap_key1,
  xor_key, and for the low 8 bits of addr_key. This is a serious weakness
  which dramatically reduces the security of the encryption.
- A 0x00 is always encrypted as a byte with as many 1s as xor_key; a 0xff is
  always encrypted as a byte with as many 0s as xor_key has 1s. So you just
  need to know one 0x00 or 0xff in the unencrypted data to know how many 1s
  there are in xor_key.
- Once you have restricted the range for swap_key1 and you know the number of
  1s in the xor_key, you can easily use known plaintext attacks and brute
  force to find the remaining keys. Long strings like THIS GAME IS FOR USE IN
  and ABCDEFGHIJKLMNOPQRSTUVWXYZ can be found by comparing the number of 1s
  in the clear and encrypted data, taking xor_key into account. When you have
  found where the string is, use brute force to reduce the key space.


Known games:
                                         swap_key1 swap_key2 addr_key xor_key
Mahjong Gakuen 2 Gakuen-chou no Fukushuu 76543210  01234567    aa55     a5
Poker Ladies                              "    "    "    "      ""      ""
Dokaben                                   "    "    "    "      ""      ""
Dokaben 2                                             unknown
Pang / Buster Bros / Pomping World       01234567  76543210    6548     24
Capcom Baseball                           "    "    "    "      ""      ""
Capcom World                             04152637  40516273    5751     43
Adventure Quiz 2 Hatena ? no Dai-Bouken  45670123  45670123    5751     43
Super Pang                               45670123  45670123    5852     43
Super Buster Bros                        45670123  45670123    2130     12
Super Marukin-Ban                        54321076  54321076    4854     4f
Quiz Tonosama no Yabou                   12345670  12345670    1111     11
Ashita Tenki ni Naare                                 unknown
Quiz Sangokushi                          23456701  23456701    1828     18
Block Block                              02461357  64207531    0002     01

Warriors of Fate                         01234567  54163072    5151     51
Cadillacs and Dinosaurs                  76543210  24601357    4343     43
Punisher                                 67452103  75316024    2222     22
Slam Masters                             54321076  65432107    3131     19

***************************************************************************/

package cottage.machine;

import jef.cpuboard.BasicDecryptingCpuBoard;
import jef.cpuboard.CpuBoard;
import jef.machine.BasicMachine;
import jef.machine.Machine;
import cottage.mame.MAMEConstants;
import cottage.mame.MAMEDriver;

public class Kabuki extends BasicMachine implements Machine {

	public Kabuki(MAMEDriver m) {
		this.m = m;
	}
	public MAMEDriver m;

	public CpuBoard createCpuBoard(int id) {
		return new BasicDecryptingCpuBoard(m.memory_region_length(m.REGION_CPU1)/2);
	}



	public void mgakuen2_decode() {
		mitchell_decode(0x76543210, 0x01234567, 0xaa55, 0xa5);
	}
	public void pang_decode() {
		mitchell_decode(0x01234567, 0x76543210, 0x6548, 0x24);
	}
	public void cworld_decode() {
		mitchell_decode(0x04152637, 0x40516273, 0x5751, 0x43);
	}
	public void hatena_decode() {
		mitchell_decode(0x45670123, 0x45670123, 0x5751, 0x43);
	}
	public void spang_decode() {
		mitchell_decode(0x45670123, 0x45670123, 0x5852, 0x43);
	}
	public void sbbros_decode() {
		mitchell_decode(0x45670123, 0x45670123, 0x2130, 0x12);
	}
	public void marukin_decode() {
		mitchell_decode(0x54321076, 0x54321076, 0x4854, 0x4f);
	}
	public void qtono1_decode() {
		mitchell_decode(0x12345670, 0x12345670, 0x1111, 0x11);
	}
	public void qsangoku_decode() {
		mitchell_decode(0x23456701, 0x23456701, 0x1828, 0x18);
	}
	public void block_decode() {
		mitchell_decode(0x02461357, 0x64207531, 0x0002, 0x01);
	}

	void wof_decode() {
		cps1_decode(0x01234567, 0x54163072, 0x5151, 0x51);
	}
	void dino_decode() {
		cps1_decode(0x76543210, 0x24601357, 0x4343, 0x43);
	}
	void punisher_decode() {
		cps1_decode(0x67452103, 0x75316024, 0x2222, 0x22);
	}
	void slammast_decode() {
		cps1_decode(0x54321076, 0x65432107, 0x3131, 0x19);
	}

	static int bitswap1(int src, int key, int select) {
		if ((select & (1 << ((key >> 0) & 7))) != 0)
			src = (src & 0xfc) | ((src & 0x01) << 1) | ((src & 0x02) >> 1);
		if ((select & (1 << ((key >> 4) & 7))) != 0)
			src = (src & 0xf3) | ((src & 0x04) << 1) | ((src & 0x08) >> 1);
		if ((select & (1 << ((key >> 8) & 7))) != 0)
			src = (src & 0xcf) | ((src & 0x10) << 1) | ((src & 0x20) >> 1);
		if ((select & (1 << ((key >> 12) & 7))) != 0)
			src = (src & 0x3f) | ((src & 0x40) << 1) | ((src & 0x80) >> 1);

		return src;
	}

	static int bitswap2(int src, int key, int select) {
		if ((select & (1 << ((key >> 12) & 7))) != 0)
			src = (src & 0xfc) | ((src & 0x01) << 1) | ((src & 0x02) >> 1);
		if ((select & (1 << ((key >> 8) & 7))) != 0)
			src = (src & 0xf3) | ((src & 0x04) << 1) | ((src & 0x08) >> 1);
		if ((select & (1 << ((key >> 4) & 7))) != 0)
			src = (src & 0xcf) | ((src & 0x10) << 1) | ((src & 0x20) >> 1);
		if ((select & (1 << ((key >> 0) & 7))) != 0)
			src = (src & 0x3f) | ((src & 0x40) << 1) | ((src & 0x80) >> 1);

		return src;
	}

	static int bytedecode(int src, int swap_key1, int swap_key2, int xor_key, int select) {
		src = bitswap1(src, swap_key1 & 0xffff, select & 0xff);
		src = ((src & 0x7f) << 1) | ((src & 0x80) >> 7);
		src = bitswap2(src, swap_key1 >> 16, select & 0xff);
		src ^= xor_key;
		src = ((src & 0x7f) << 1) | ((src & 0x80) >> 7);
		src = bitswap2(src, swap_key2 & 0xffff, select >> 8);
		src = ((src & 0x7f) << 1) | ((src & 0x80) >> 7);
		src = bitswap1(src, swap_key2 >> 16, select >> 8);
		return src;
	}

	void kabuki_decode(int[] src, int src_offs, int dst_op,int dest_data,
			int base_addr,int length,int swap_key1,int swap_key2,int addr_key,int xor_key)
	{
		int A;
		int select;

		for (A = 0;A < length;A++)
		{
			/* decode opcodes */
			select = (A + base_addr) + addr_key;
			src[dst_op + A] = bytedecode(src[src_offs + A],swap_key1,swap_key2,xor_key,select);

			/* decode data */
			select = ((A + base_addr) ^ 0x1fc0) + addr_key + 1;
			src[dest_data + A] = bytedecode(src[src_offs + A],swap_key1,swap_key2,xor_key,select);
		}
		
	}
	
	void mitchell_decode(int swap_key1, int swap_key2, int addr_key, int xor_key) {
		int i;
		int[] rom = m.memory_region(MAMEConstants.REGION_CPU1);
		int diff = m.memory_region_length(MAMEConstants.REGION_CPU1) / 2;

	    kabuki_decode(rom, 0, diff,0,  0x0000,0x8000, swap_key1,swap_key2,addr_key,xor_key);
		for (i = 0x10000; i < diff; i += 0x4000)
			kabuki_decode(rom, i,i+diff,i,0x8000,0x4000, swap_key1,swap_key2,addr_key,xor_key);
	}

	void cps1_decode(int swap_key1, int swap_key2, int addr_key, int xor_key) {
		/*
		 * int []rom = m.memory_region(MAMEConstants.REGION_CPU2); int []backup =
		 * m.memory_region(MAMEConstants.REGION_USER1); int diff =
		 * m.memory_region_length(MAMEConstants.REGION_CPU2) / 2;
		 *  // the main CPU can read the ROM and checksum it to verify that it
		 * hasn't been // replaced with a decrypted one. if (backup!=null)
		 * memcpy(backup,rom,0x8000);
		 * 
		 * memory_set_opcode_base(1,rom+diff);
		 */
	}
}