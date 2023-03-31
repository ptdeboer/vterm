/*
 * (C) 2005 - 2012 Virtual Laboratory for eScience (VL-e).
 * (C) 2012 - 2015 Netherlands eScience Center.
 * (C) 2005 - 2023 Piter.NL
 *     See LICENSE.txt for details.
 */
//---
package nl.piter.vterm.emulator;

public class VTxCharDefs {

    /* Relevant VT100/VT102/VT10X Control Characters (octal)
     * 000 = NUL (fill character)
     * 001 = SOH (Start of heading)
     * 002 = STX (Start of text)
     * 003 = ETX (Can be selected half-duplex turnaround char)
     * 004 = EOT (Can be turnaround or disconnect char, if turn, then DLE-EOT=disc.)
     * 005 = ENQ (Transmits answerback message)
     * 006 = ACK
     * 007 = BEL (Generates bell tone)
     * 010 = BS  (Moves cursor left)
     * 011 = HT  (Moves cursor to next tab)
     * 012 = LF  (Linefeed or New line operation)
     * 013 = VT  (Processed as LF)
     * 014 = FF  (Processed as LF, can be selected turnaround char)
     * 015 = CR  (Moves cursor to left margin, can be turnaround char)
     * 016 = SO  (Selects G1 charset)
     * 017 = SI  (Selects G0 charset)
     * 020 = DLE (Data Link Exchage)
     * 021 = DC1 (XON, causes terminal to continue transmit)
     * 023 = DC3 (XOFF, causes terminal to stop transmitting)
     * 030 = CAN (Cancels escape sequence)
     * 032 = SUB (Processed as CAN)
     * 033 = ESC (Processed as sequence indicator)
     */

    final public static char CTRL_NUL = 0x00; // Ignored on input; not stored in buffer
    final public static char CTRL_SOH = 0x01;
    final public static char CTRL_STX = 0x02;
    final public static char CTRL_ETX = 0x03; // CTRL-C:
    final public static char CTRL_EOT = 0x04; // CTRL-D: End Of Transission ?
    final public static char CTRL_ENQ = 0x05; // CTRL_E Transmit ANSWERBACK message
    final public static char CTRL_ACK = 0x06;
    final public static char CTRL_BEL = 0x07; // CTRL-G: BEEEEEEEEEEEEEEEEP
    final public static char CTRL_BS = 0x08; // CTRL-H: Backspace
    final public static char CTRL_HT = 0x09; // CTRL-I: next stabstop
    final public static char CTRL_LF = 0x0a; // CTRL-J: line feed/new line (depends on line feed mode)
    final public static char CTRL_VT = 0x0b; // CTRL-K/LF : line feed/new line
    final public static char CTRL_FF = 0x0c; // CTRL-L/LF : line feed/new line
    final public static char CTRL_CR = 0x0d; // CTRL-M/CR : Carriage Return
    final public static char CTRL_SO = 0x0e; // CTRL-N: G1 character set
    final public static char CTRL_SI = 0x0f; // CTRL-O: G0 character set
    final public static char CTRL_DLE = 0x10;
    final public static char CTRL_XON = 0x11; // DC1: CTRL-Q: XON (only XON/XOFF are allowed)
    final public static char CTRL_DC2 = 0x12; // DC2
    final public static char CTRL_XOFF = 0x13;// DC3: CTRL-S: XOFF (turn off XON/XOFF mode)
    final public static char CTRL_DC4 = 0x14; // DC4:
    final public static char CTRL_CAN = 0x18; // Abort CTRL sequence, output ERROR Char
    final public static char CTRL_SUB = 0x1a; // Same as CAN
    final public static char CTRL_ESC = 0x1b; // New Escape Sequence  (aborts previous)
    final public static char CTRL_DEL = 0x7f; // Alternate BS: plot 7 holes in punchcard (01111111)
    // 8 bit characters:
    final public static char IND_8bit = 0x84; // Index (ESC D)
    final public static char NEL_8bit = 0x85; // Next Line  = ESC E
    final public static char HTS_8bit = 0x88; // Tab Set ESC H
    final public static char RI_8bit = 0x8d;
    final public static char SS2_8bit = 0x8e;
    final public static char SS3_8bit = 0x8f;
    final public static char DCS_8bit = 0x90;
    final public static char SPA_8bit = 0x96;
    final public static char EPA_8bit = 0x97;
    final public static char SOS_8bit = 0x98;
    final public static char DECID_8bit = 0x9a;
    final public static char CSI_8bit = 0x9b;
    final public static char ST_8bit = 0x9c;
    final public static char OSC_8bit = 0x9d;
    final public static char PM_8bit = 0x9e;
    final public static char APC_8bit = 0x9f;

    // Prefix characters sequences:
    final public static String CTRL_CSI_PREFIX = CTRL_ESC + "[";
    final public static String CTRL_OSC_PREFIX = CTRL_ESC + "]";
    final public static String CTRL_DEC_PRIVATE_PREFIX = CTRL_ESC + "[?";
    final public static String CTRL_SECONDARY_DA_PREFIX = CTRL_ESC + "[>";

}
