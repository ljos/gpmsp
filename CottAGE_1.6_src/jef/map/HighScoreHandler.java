/*
 * Created on Aug 3, 2005
 * by edy
 */
package jef.map;

import jef.machine.Machine;

/**
 * @author edy
 * 
 * Generic High score counter
 */
public class HighScoreHandler implements WriteHandler {

    WriteHandler writeHandler;

    Machine m;

    int[] memory;

    int blank_char, zero_char;

    int initial_high_score;

    int start_addr;

    int offset;

    int last_addr;

    // select this value for blank_char if the scores are stored in nibbles instead of bytes.
    public static final int CHAR_NIBBLES = 0xffffff;

    boolean save_highscore = false;
    int delay;

    /**
     * @param passThrough The 'real' memory handler for this memory area
     * @param m Reference to the Machine
     * @param memory Reference to the memory
     * @param blank_char The byte value for a blank character (use CHAR_NIBBLES if digits are stored in nibbles).
     * @param zero_char The byte value for a zero character
     * @param initial_high_score The initial high score at boot-up
     * @param start_addr The start address of the high score
     * @param offset The offset between bytes
     * @param last_addr The last address of the high score
     * @param delay A delay before high scores are calculated. Used to skip init sequences like in pacman.
     */
    public HighScoreHandler(WriteHandler passThrough, Machine m, int[] memory, int blank_char, int zero_char,
            int initial_high_score, int start_addr, int offset, int last_addr, int delay) {
        super();
        this.writeHandler = passThrough;
        this.m = m;
        this.memory = memory;
        this.blank_char = blank_char;
        this.zero_char = zero_char;
        this.initial_high_score = initial_high_score;
        this.start_addr = start_addr;
        this.offset = offset;
        this.last_addr = last_addr;

        m.setHighScoreSupported(true);
        m.setHighScore(0);
    }

    /*
     * (non-Javadoc)
     * 
     * @see jef.map.WriteHandler#write(int, int)
     */
    public void write(int address, int data) {
        if (memory[address] != data) {

            writeHandler.write(address, data);

            // for not counting init sequences (like pacman)
            if (save_highscore) {
                m.setHighScore(calcScore());
            } else {
                if (delay > 0) {
                    delay--;
                } else {
                    if (calcScore() == initial_high_score) {
                        System.out.println("Triggered highscore saving.");
                        save_highscore = true;
                    }
                }
            }
        }

    }

    /**
     * Calculate the score from the emulated memory contents.
     * 
     * @return long The score
     */
    private long calcScore() {

        long score = 0;

        int pow = 0;
        if (blank_char == CHAR_NIBBLES) {
            for (int i = last_addr; i >= start_addr; i -= offset) {
                int c = memory[i];
                
                score += ((c & 0x0f) * Math.pow(10, pow++));
                score += ((c >> 4) * Math.pow(10, pow++));
            }
        } else {
            // calculate the score
            for (int i = last_addr; i >= start_addr; i -= offset) {
                int c = memory[i];
                if (c == 0x00 || c == blank_char)
                    c = zero_char;
                c -= zero_char;
                score += (c * Math.pow(10, pow++));
            }
        }

        if (score >= Math.pow(10, pow) || score == initial_high_score)
            score = 0;

        return score;
    }

}
