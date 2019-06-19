package space.alula.tapebot;

/*
DFPWM1a implementation in Java
operates on 8-bit signed PCM data and little-endian DFPWM data
by Ben "GreaseMonkey" Russell, 2013, 2016 - Public Domain
NOTE, len is in bytes relative to DFPWM (len*8 PCM bytes)
also the main() function takes unsigned 8-bit data and converts it to suit
*/

public class DFPWM {
    private final int respInc;
    private final int respDec;
    private final int respPrec;
    private final int lpfStrength;
    private final boolean dfpwmOld;

    private int response = 0;
    private int level = 0;
    private boolean lastbit = false;

    private int flastlevel = 0;
    private int lpflevel = 0;


    public DFPWM(boolean newdfpwm) {
        dfpwmOld = !newdfpwm;
        if (newdfpwm) {
            respInc = 1;
            respDec = 1;
            respPrec = 10;
            lpfStrength = 140;
        } else {
            respInc = 7;
            respDec = 20;
            respPrec = 8;
            lpfStrength = 100;
        }
    }

    private void ctxUpdate(boolean curbit) {
        int target = (curbit ? 127 : -128);
        int nlevel = (level + ((response * (target - level)
                + (1 << (respPrec - 1))) >> respPrec));
        if (nlevel == level && level != target)
            nlevel += (curbit ? 1 : -1);

        int rtarget;
        int rdelta;

        if (curbit == lastbit) {
            rtarget = (1 << respPrec) - 1;
            rdelta = respInc;
        } else {
            rtarget = 0;
            rdelta = respDec;
        }

        int nresponse = response + (dfpwmOld ? ((rdelta * (rtarget - response) + 128) >> 8) : 0);
        if (nresponse == response && response != rtarget)
            nresponse += (curbit == lastbit ? 1 : -1);

        if (respPrec > 8 && nresponse < (2 << (respPrec - 8))) {
            nresponse = (2 << (respPrec - 8));
        }

        response = nresponse;
        lastbit = curbit;
        level = nlevel;
    }

    public void decompress(byte[] dest, byte[] src, int destoffs, int srcoffs, int len) {
        for (int i = 0; i < len; i++) {
            byte d = src[srcoffs++];
            for (int j = 0; j < 8; j++) {
                // apply context
                boolean currBit = ((d & 1) != 0);
                boolean lastBit = this.lastbit;
                ctxUpdate(currBit);
                d >>= 1;

                // apply noise shaping
                int blevel = (byte) (currBit == lastBit
                        ? level
                        : ((flastlevel + level + 1) >> 1));
                flastlevel = level;

                // apply low-pass filter
                lpflevel += ((lpfStrength * (blevel - lpflevel) + 0x80) >> 8);
                dest[destoffs++] = (byte) (lpflevel);
            }
        }
    }

    public void compress(byte[] dest, byte[] src, int destoffs, int srcoffs, int len) {
        for (int i = 0; i < len; i++) {
            int d = 0;
            for (int j = 0; j < 8; j++) {
                int inlevel = src[srcoffs++];
                boolean curbit = (inlevel > level || (inlevel == level && level == 127));
                d = (curbit ? (d >> 1) + 128 : d >> 1);
                ctxUpdate(curbit);
            }
            dest[destoffs++] = (byte) d;
        }
    }
}
