package modelviewer.rs.buffer;

import lombok.SneakyThrows;

import java.util.Arrays;
import java.util.LinkedList;


public final class Buffer {

    @SneakyThrows(Exception.class)
    public static Buffer create() {
        synchronized (BUFFER_CACHE) {
            Buffer buffer = null;
            if (cache_indices > 0) {
                cache_indices--;

                buffer = (Buffer) BUFFER_CACHE.pop();
            }
            if (buffer != null) {
                buffer.pos = 0;
                return buffer;
            }
        }
        Buffer buffer = new Buffer();
        buffer.pos = 0;
        buffer.payload = new byte[5000];
        return buffer;
    }

    private Buffer() {
    }

    @SneakyThrows(Exception.class)
    public Buffer(byte[] payload) {
        this.payload = payload;
        this.pos = 0;
    }

    @SneakyThrows(Exception.class)
    public Buffer(int length, byte initialValue) {
        this.payload = new byte[length];
        Arrays.fill(payload, initialValue);
    }

    @SneakyThrows(Exception.class)
    public void skip(int amount) {
        this.pos += amount;
    }

    @SneakyThrows(Exception.class)
    public void setPosition(int pos) {
        this.pos = pos;
    }

    @SneakyThrows(Exception.class)
    public int getSmartShortMinusOne() {
        final int i_118_ = this.payload[pos++] & 0xFF;
        if (i_118_ < 128) {
            return (this.getSignedByte() & 0xFF) - 1;
        }
        return (this.getShort() & 0xFFFF) - 32769;
    }

    /**
     * Reads a smart value from the buffer.
     *
     * @return the read smart value.
     */
    @SneakyThrows(Exception.class)
    public int readSmart() {
        int value = payload[pos] & 0xff;
        if (value < 128) {
            return getUnsignedByte();
        }
        return getUnsignedShort() - 32768;
    }


    /**
     * Reads a smart value from the buffer (supports -1).
     *
     * @return the read smart value.
     */
    @SneakyThrows(Exception.class)
    public int readSmartNS() {
        return readSmart() - 1;
    }


    /**
     * Writes a smart value to the buffer.
     *
     * @param value the value to write.
     */
    @SneakyThrows(Exception.class)
    public void writeSmart(int value) {
        if (value >= 128) {
            writeShort(value + 32768);
        } else {
            write_byte(value);
        }
    }


    /**
     * Reads an unsigned smart value from the buffer.
     *
     * @return the read unsigned smart value.
     */
    @SneakyThrows(Exception.class)
    public int readUnsignedSmart() {
        int value = payload[pos] & 0xff;
        if (value < 128) {
            return getUnsignedByte() - 64;
        }
        return getUnsignedShort() - 49152;
    }


    /**
     * Writes an unsigned smart value to the buffer.
     *
     * @param value the value to write.
     */
    @SneakyThrows(Exception.class)
    public void writeUnsignedSmart(int value) {
        if (value < 64 && value >= -64) {
            write_byte(value + 64);
            return;
        }
        if (value < 16384 && value >= -16384) {
            writeShort(value + 49152);
        } else {
            System.out.println("Error psmart out of range: " + value);
        }
    }


    /**
     * Reads a smart words from the buffer.
     *
     * @return the read smart value.
     */
    @SneakyThrows(Exception.class)
    public int readSmartWords() {
        int value = 0;
        int incr;
        for (incr = readSmart(); incr == 32767; incr = readSmart()) {
            value += 32767;
        }
        value += incr;
        return value;
    }


    /**
     * Writes a smart words to the buffer.
     *
     * @param value the smart value to write.
     */
    @SneakyThrows(Exception.class)
    public void writeSmartWords(int value) {
        while (value > 32767) {
            writeSmart(value);
            value -= 32767;
        }
        if (value > 0) {
            writeSmart(value);
        }
    }


    /**
     * Reads a big smart value from the buffer.
     *
     * @return the read smart value.
     */
    @SneakyThrows(Exception.class)
    public int readBigSmart() {
        if (payload[pos] < 0) {
            return getInt() & 0x7fffffff;
        }
        return getUnsignedShort();
    }


    /**
     * Reads a big smart value from the buffer (termination value
     * <code>-1</code> supported).
     *
     * @return the read smart value.
     */
    @SneakyThrows(Exception.class)
    public int readBigSmartNS() {
        if (payload[pos] < 0) {
            return getInt() & 0x7fffffff;
        }
        int value = getUnsignedShort();
        if (value == 32767) {
            return -1;
        }
        return value;
    }


    /**
     * Writes a big smart value to the buffer.
     *
     * @param value the value to write.
     */
    @SneakyThrows(Exception.class)
    public void writeBigSmart(int value) {
        if (value > 32767) {
            writeInt(value - Integer.MAX_VALUE - 1);
        } else {
            writeShort(value >= 0 ? value : 32767);
        }
    }

    @SneakyThrows(Exception.class)
    public int get_smart_b() {
        int baseVal = 0;
        int lastVal = 0;
        while ((lastVal = getUnsignedSmart()) == 32767) {
            baseVal += 32767;
        }
        return baseVal + lastVal;
    }

    @SneakyThrows(Exception.class)
    public String getNewString() {
        int i = this.pos;
        while (this.payload[this.pos++] != 0)
            ;
        return new String(this.payload, i, pos - i - 1);
    }

    @SneakyThrows(Exception.class)
    public void write_byte(int value) {
        this.payload[this.pos++] = (byte) value;
    }

    @SneakyThrows(Exception.class)
    public void writeBoolean(boolean value) {
        this.payload[this.pos++] = (byte) (value ? 1 : 0);
    }

    @SneakyThrows(Exception.class)
    public void writeShort(int value) {
        this.payload[this.pos++] = (byte) (value >> 8);
        this.payload[this.pos++] = (byte) value;
    }

    @SneakyThrows(Exception.class)
    public void writeTriByte(int value) {
        this.payload[this.pos++] = (byte) (value >> 16);
        this.payload[this.pos++] = (byte) (value >> 8);
        this.payload[this.pos++] = (byte) value;
    }

    @SneakyThrows(Exception.class)
    public void writeInt(int value) {
        this.payload[this.pos++] = (byte) (value >> 24);
        this.payload[this.pos++] = (byte) (value >> 16);
        this.payload[this.pos++] = (byte) (value >> 8);
        this.payload[this.pos++] = (byte) value;
    }

    @SneakyThrows(Exception.class)
    public void writeLEInt(int value) {
        this.payload[this.pos++] = (byte) value;
        this.payload[this.pos++] = (byte) (value >> 8);
        this.payload[this.pos++] = (byte) (value >> 16);
        this.payload[this.pos++] = (byte) (value >> 24);
    }

    @SneakyThrows(Exception.class)
    public void writeString(String text) {
        System.arraycopy(text.getBytes(), 0, this.payload, this.pos, text.length());
        this.pos += text.length();
        this.payload[this.pos++] = 10;
    }

    @SneakyThrows(Exception.class)
    public void put_bytes(byte data[], int offset, int length) {
        for (int index = length; index < length + offset; index++)
            this.payload[this.pos++] = data[index];
    }

    @SneakyThrows(Exception.class)
    public void put_length(int length) {
        this.payload[this.pos - length - 1] = (byte) length;
    }

    @SneakyThrows(Exception.class)
    public int getUnsignedByte() {
        return this.payload[this.pos++] & 0xff;
    }

    @SneakyThrows(Exception.class)
    public byte getSignedByte() {
        return this.payload[this.pos++];
    }

    @SneakyThrows(Exception.class)
    public int getUnsignedShort() {
        this.pos += 2;
        return ((this.payload[this.pos - 2] & 0xff) << 8) + (this.payload[this.pos - 1] & 0xff);
    }

    @SneakyThrows(Exception.class)
    public int getSignedShort() {
        this.pos += 2;
        int value = ((this.payload[this.pos - 2] & 0xff) << 8) + (this.payload[this.pos - 1] & 0xff);
        if (value > 32767)
            value -= 0x10000;

        return value;
    }

    @SneakyThrows(Exception.class)
    public int getShort() {
        this.pos += 2;
        int value = ((this.payload[this.pos - 2] & 0xff) << 8) + (this.payload[this.pos - 1] & 0xff);

        if (value > 60000)
            value = -65535 + value;
        return value;
    }

    @SneakyThrows(Exception.class)
    public int get24BitInt() {
        this.pos += 3;
        return ((this.payload[this.pos - 3] & 0xff) << 16) + ((this.payload[this.pos - 2] & 0xff) << 8) + (this.payload[this.pos - 1] & 0xff);
    }

    @SneakyThrows(Exception.class)
    public int getInt() {
        this.pos += 4;
        return ((this.payload[this.pos - 4] & 0xff) << 24) + ((this.payload[this.pos - 3] & 0xff) << 16) + ((this.payload[this.pos - 2] & 0xff) << 8) + (this.payload[this.pos - 1] & 0xff);
    }

    @SneakyThrows(Exception.class)
    public long getLong() {
        long msi = (long) this.getInt() & 0xffffffffL;
        long lsi = (long) this.getInt() & 0xffffffffL;
        return (msi << 32) + lsi;
    }

    @SneakyThrows(Exception.class)
    public String getString() {

        int index = this.pos;
        while (this.payload[this.pos++] != 10)
            ;
        return new String(this.payload, index, this.pos - index - 1);
    }

    private static final char[] CHARACTERS = new char[]{
            '\u20ac', '\u0000', '\u201a', '\u0192', '\u201e', '\u2026',
            '\u2020', '\u2021', '\u02c6', '\u2030', '\u0160', '\u2039',
            '\u0152', '\u0000', '\u017d', '\u0000', '\u0000', '\u2018',
            '\u2019', '\u201c', '\u201d', '\u2022', '\u2013', '\u2014',
            '\u02dc', '\u2122', '\u0161', '\u203a', '\u0153', '\u0000',
            '\u017e', '\u0178'
    };

    @SneakyThrows(Exception.class)
    public String getStringOSRS() {
        StringBuilder sb = new StringBuilder();

        for (; ; ) {
            int ch = this.getUnsignedByte();

            if (ch == 0) {
                break;
            }

            if (ch >= 128 && ch < 160) {
                char var7 = CHARACTERS[ch - 128];
                if (0 == var7) {
                    var7 = '?';
                }

                ch = var7;
            }

            sb.append((char) ch);
        }
        return sb.toString();
    }

    @SneakyThrows(Exception.class)
    public byte[] getStringBytes() {
        int index = this.pos;
        while (this.payload[this.pos++] != 10)
            ;
        byte[] data = new byte[this.pos - index - 1];
        System.arraycopy(this.payload, index, data, 0, this.pos - 1 - index);
        return data;
    }

    @SneakyThrows(Exception.class)
    public void getBytes(int offset, int length, byte[] data) {
        for (int index = length; index < length + offset; index++)
            data[index] = this.payload[this.pos++];
    }

    @SneakyThrows(Exception.class)
    public void initBitAccess() {
        this.bit_pos = this.pos * 8;
    }

    @SneakyThrows(Exception.class)
    public int getBits(int amount) {
        int byte_offset = this.bit_pos >> 3;
        int bit_offset = 8 - (this.bit_pos & 7);
        int value = 0;
        this.bit_pos += amount;
        for (; amount > bit_offset; bit_offset = 8) {
            value += (this.payload[byte_offset++] & BIT_MASKS[bit_offset]) << amount - bit_offset;
            amount -= bit_offset;
        }
        if (amount == bit_offset)
            value += this.payload[byte_offset] & BIT_MASKS[bit_offset];
        else
            value += this.payload[byte_offset] >> bit_offset - amount & BIT_MASKS[amount];

        return value;
    }

    @SneakyThrows(Exception.class)
    public void finishBitAccess() {
        this.pos = (this.bit_pos + 7) / 8;
    }

    @SneakyThrows(Exception.class)
    public int getSignedSmart() {
        int value = this.payload[this.pos] & 0xff;
        if (value < 128) {
            return this.getUnsignedByte() - 64;
        } else {
            return this.getUnsignedShort() - 49152;
        }
    }

    @SneakyThrows(Exception.class)
    public int getUnsignedSmart() {
        int value = this.payload[this.pos] & 0xff;
        if (value < 128) {
            return this.getUnsignedByte();
        } else {
            return this.getUnsignedShort() - 32768;
        }
    }

    @SneakyThrows(Exception.class)
    public void putAddedByte(int value) {
        this.payload[this.pos++] = (byte) (value + 128);
    }

    public void putNegatedByte(int value) {
        this.payload[this.pos++] = (byte) (-value);
    }

    public void putSubtractedByte(int value) {
        this.payload[this.pos++] = (byte) (128 - value);
    }

    public int getAddedByte() {
        return this.payload[this.pos++] - 128 & 0xff;
    }

    public int getNegatedByte() {
        return -this.payload[this.pos++] & 0xff;
    }

    public int getSubtractedByte() {
        return 128 - this.payload[this.pos++] & 0xff;
    }

    public byte getSignedAddedByte() {
        return (byte) (this.payload[this.pos++] - 128);
    }

    public byte getSignedNegatedByte() {
        return (byte) -this.payload[this.pos++];
    }

    public byte getSignedSubtractedByte() {
        return (byte) (128 - this.payload[this.pos++]);
    }

    public void putLEShortDuplicate(int value) {
        this.payload[this.pos++] = (byte) value;
        this.payload[this.pos++] = (byte) (value >> 8);
    }

    public void putShortAdded(int value) {
        this.payload[this.pos++] = (byte) (value >> 8);
        this.payload[this.pos++] = (byte) (value + 128);
    }

    public void putLEShortAdded(int value) {
        this.payload[this.pos++] = (byte) (value + 128);
        this.payload[this.pos++] = (byte) (value >> 8);
    }

    public int method549() {//TODO
        this.pos += 2;
        return ((this.payload[this.pos - 1] & 0xff) << 8) + (this.payload[this.pos - 2] & 0xff);
    }

    public int method550() {//TODO
        this.pos += 2;
        return ((this.payload[this.pos - 2] & 0xff) << 8) + (this.payload[this.pos - 1] - 128 & 0xff);
    }

    public int get_little_short() {
        this.pos += 2;
        return ((this.payload[this.pos - 1] & 0xff) << 8) + (this.payload[this.pos - 2] - 128 & 0xff);
    }

    public int method552() {//TODO
        this.pos += 2;
        int value = ((this.payload[this.pos - 1] & 0xff) << 8) + (this.payload[this.pos - 2] & 0xff);

        if (value > 32767)
            value -= 0x10000;
        return value;
    }

    public int method553() {//TODO
        this.pos += 2;
        int value = ((this.payload[this.pos - 1] & 0xff) << 8) + (this.payload[this.pos - 2] - 128 & 0xff);
        if (value > 32767)
            value -= 0x10000;

        return value;
    }

    public int method555() {//TODO
        this.pos += 4;
        return ((this.payload[this.pos - 2] & 0xff) << 24)
                + ((this.payload[this.pos - 1] & 0xff) << 16)
                + ((this.payload[this.pos - 4] & 0xff) << 8)
                + (this.payload[this.pos - 3] & 0xff);
    }

    public int method556() {//TODO
        this.pos += 4;
        return ((this.payload[this.pos - 3] & 0xff) << 24)
                + ((this.payload[this.pos - 4] & 0xff) << 16)
                + ((this.payload[this.pos - 1] & 0xff) << 8)
                + (this.payload[this.pos - 2] & 0xff);
    }

    public int method557() {//TODO
        this.pos += 4;
        return ((this.payload[this.pos - 2] & 255) << 8)
                + ((this.payload[this.pos - 4] & 255) << 24)
                + ((this.payload[this.pos - 3] & 255) << 16)
                + (this.payload[this.pos - 1] & 255);
    }


    public void putReverseData(byte[] data, int length, int offset) {
        for (int index = (length + offset) - 1; index >= length; index--)
            this.payload[this.pos++] = (byte) (data[index] + 128);

    }

    public void getReverseData(byte[] data, int offset, int length) {
        for (int index = (length + offset) - 1; index >= length; index--)
            data[index] = this.payload[this.pos++];

    }

    public byte[] payload;
    public int pos;

    public int bit_pos;
    private static final int[] BIT_MASKS = {
            0, 1, 3, 7, 15, 31, 63, 127, 255,
            511, 1023, 2047, 4095, 8191, 16383, 32767, 65535, 0x1ffff, 0x3ffff,
            0x7ffff, 0xfffff, 0x1fffff, 0x3fffff, 0x7fffff, 0xffffff,
            0x1ffffff, 0x3ffffff, 0x7ffffff, 0xfffffff, 0x1fffffff, 0x3fffffff,
            0x7fffffff, -1
    };

    private static int cache_indices;
    private static final LinkedList BUFFER_CACHE = new LinkedList();
}
