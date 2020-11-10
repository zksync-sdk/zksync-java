package im.argent.zksync.signer;

import sun.security.util.ArrayUtil;

import java.util.Arrays;

public class Bits {

    boolean[] bits;

    public Bits(int size) {
        bits = new boolean[size];
    }

    public Bits(boolean[] bits) {
        this.bits = bits;
    }

    public boolean get(int i) {
        return bits[i];
    }

    public void set(int index) {
        bits[index] = true;
    }

    public void set(int index, boolean value) {
        bits[index] = value;
    }

    public void set(int index, int value) {
        bits[index] = value == 0 ? false : true;
    }

    public int size() {
        return bits.length;
    }

    public Bits reverse() {
        boolean[] reversed = new boolean[bits.length];
        int bitsLength = bits.length;

        for (int i = 0; i < bitsLength; i++) {
            reversed[i] = bits[bitsLength - 1 - i];
        }

        return new Bits(reversed);
    }

    public String toString() {
        final StringBuilder builder = new StringBuilder();
        for (boolean bit : bits) {
            builder.append(bit ? 1 : 0);
            builder.append(", ");
        }
        return builder.toString();
    }
}
