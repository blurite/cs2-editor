package buffer;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public final class ByteBuffer {
    private int position;
    private final byte[] buffer;
    private static final char[] unicodeTable = {'€', '\000', '‚', 'ƒ', '„', '…', '†', '‡', 'ˆ', '‰', 'Š', '‹', 'Œ',
            '\000', 'Ž', '\000', '\000', '‘', '’', '“', '”', '•', '–', '—', '˜', '™', 'š', '›', 'œ', '\000', 'ž', 'Ÿ'};
    private static final int xteaDelta = -1640531527;
    private static final int xteaRounds = 32;

    public ByteBuffer() {
        this(new byte[5000], 0);
    }

    public ByteBuffer(byte[] data) {
        this(data, 0);
    }

    public ByteBuffer(int capacity) {
        this(new byte[capacity], 0);
    }

    public ByteBuffer(byte[] data, int offset) {
        this.buffer = data;
        this.position = offset;
    }

    public void setPosition(int position) {
        if (position < 0 || position >= this.buffer.length) {
            throw new IllegalArgumentException();
        }
        this.position = position;
    }

    public int limit() {
        return buffer.length;
    }

    public int getPosition() {
        return position;
    }

    public int remaining() {
        return buffer.length - position;
    }

    public byte[] toArray(int offset, int length) {
        byte[] bf = new byte[length - offset];
        if (length >= 0) System.arraycopy(this.buffer, offset, bf, 0, length);
        return bf;
    }

    public byte[] getBuffer() {
        return this.buffer;
    }

    public byte readByte() {
        return this.buffer[this.position++];
    }

    public int readUnsignedByte() {
        return this.buffer[this.position++] & 255;
    }

    public void writeByte(int v) {
        this.buffer[this.position++] = (byte) v;
    }

    public void readBytes(byte[] buffer, int offset, int length) {
        for (int pos = offset; pos < length + offset; pos++) {
            buffer[pos] = this.buffer[this.position++];
        }
    }

    public void writeBytes(byte[] buffer, int offset, int length) {
        for (int pos = offset; pos < offset + length; pos++) {
            this.buffer[this.position++] = buffer[pos];
        }
    }

    public int readUnsignedSmart() {
        final int value = this.buffer[this.position] & 255;
        if (value < 128) {
            return readUnsignedByte();
        } else {
            return readUnsignedShort() - 32768;
        }
    }

    public int readUnsignedSmartSub() {
        final int value = this.buffer[this.position] & 255;
        if (value < 128) {
            return readUnsignedByte() - 1;
        } else {
            return readUnsignedShort() - 32769;
        }
    }

    public int readSmart() {
        final int value = this.buffer[this.position] & 255;
        if (value < 128) {
            return readUnsignedByte() - 64;
        }
        return readUnsignedShort() - 49152;
    }

    public void writeSmart(int v) {
        if (v >= 0 && v < 128) {
            this.writeByte(v);
        } else if (v >= 0 && v < 32768) {
            this.writeShort(v + 32768);
        } else {
            throw new IllegalArgumentException();
        }
    }

    public int readShort() {
        this.position += 2;
        int value = ((this.buffer[this.position - 1] & 255) + (this.buffer[this.position - 2] << 8 & 65280));
        if (value > 32767) {
            value -= 65536;
        }
        return value;
    }

    public int readUnsignedShort() {
        this.position += 2;
        return ((this.buffer[this.position - 2] << 8 & 65280) + (this.buffer[this.position - 1] & 255));
    }

    public void writeShort(int s) {
        this.buffer[this.position++] = (byte) (s >> 8);
        this.buffer[this.position++] = (byte) s;
    }

    public int readUnsignedMedInt() {
        this.position += 3;
        return ((this.buffer[this.position - 2] << 8 & 65280) + ((this.buffer[this.position - 3] & 255) << 16) + (this.buffer[this.position - 1] & 255));
    }

    public int readMedium() {
        return (readUnsignedByte() << 16) + (readUnsignedByte() << 8) + readUnsignedByte();
    }

    public int readSignedMedInt() {
        this.position += 3;
        int value =
                ((this.buffer[this.position - 1] & 255) + (this.buffer[this.position - 2] << 8 & 65280) + (this.buffer[this.position - 3] << 16 & 16711680));
        if (value > 8388607) {
            value -= 16777216;
        }
        return value;
    }

    public void writeMedium(int value) {
        this.buffer[this.position++] = (byte) (value >> 16);
        this.buffer[this.position++] = (byte) (value >> 8);
        this.buffer[this.position++] = (byte) (value & 255);
    }

    public int readInt() {
        this.position += 4;
        return ((this.buffer[this.position - 1] & 255) + ((this.buffer[this.position - 3] & 255) << 16) + ((this.buffer[this.position - 4] << 24 & ~16777215) + (this.buffer[this.position - 2] << 8 & 65280)));
    }

    public void writeInt(int value) {
        this.buffer[this.position++] = (byte) (value >> 24);
        this.buffer[this.position++] = (byte) (value >> 16);
        this.buffer[this.position++] = (byte) (value >> 8);
        this.buffer[this.position++] = (byte) value;
    }

    public long read5Byte() {
        long v0 = (long) readUnsignedByte() & 4294967295L;
        long v1 = (long) readInt() & 4294967295L;
        return v1 + (v0 << 32);
    }

    public long readLong() {
        long v0 = (long) readInt() & 4294967295L;
        long v1 = (long) readInt() & 4294967295L;
        return (v0 << 32) + v1;
    }

    public void writeLong(long v) {
        this.buffer[this.position++] = (byte) (int) (v >> 56);
        this.buffer[this.position++] = (byte) (int) (v >> 48);
        this.buffer[this.position++] = (byte) (int) (v >> 40);
        this.buffer[this.position++] = (byte) (int) (v >> 32);
        this.buffer[this.position++] = (byte) (int) (v >> 24);
        this.buffer[this.position++] = (byte) (int) (v >> 16);
        this.buffer[this.position++] = (byte) (int) (v >> 8);
        this.buffer[this.position++] = (byte) (int) v;
    }

    public int readUnsignedIntSmart() {
        int peek = buffer[position];
        return (peek & 0x80) == 0 ? readUnsignedShort() : (readInt() & 0x7FFFFFFF);
    }

    public ByteBuffer writeUnsignedIntSmart(int v) {
        if (v >= 0 && v <= 0x7FFF) writeShort(v);
        else if (v >= 0 && v <= 0x7FFFFFFF) writeInt(0x80000000 | v);
        else throw new IllegalArgumentException("for " + v);
        return this;
    }
    /*
    public fun ByteBuf.readUnsignedIntSmart(): Int {
    val peek = getUnsignedByte(readerIndex()).toInt()
    return if ((peek and 0x80) == 0) {
        readUnsignedShort()
    } else {
        readInt() and 0x7FFFFFFF
    }
}

public fun ByteBuf.writeUnsignedIntSmart(v: Int): ByteBuf {
    when (v) {
        in 0..0x7FFF -> writeShort(v)
        in 0..0x7FFFFFFF -> writeInt(0x80000000.toInt() or v)
        else -> throw IllegalArgumentException()
    }

    return this
}
     */

    public int readSmartInt() {
/*        if (buffer[position] >= 0) {
            return readUnsignedShort() & 32767;
        }
        return readInt() & 2147483647;*/
        return readUnsignedIntSmart();
    }

    public int readBigSmart() {
        if (buffer[position] < 0) {
            return readInt() & Integer.MAX_VALUE;
        } else {
            final int value = readUnsignedShort();
            return value == 32767 ? -1 : value;
        }
    }

    public int readHugeSmart() {
        int value = 0;
        int var2;
        for (var2 = readUnsignedSmart(); var2 == 32767; var2 = readUnsignedSmart()) {
            value += 32767;
        }
        value += var2;
        return value;
    }

    public void writeSmart32(final int value) {
        writeUnsignedIntSmart(value);
/*        if (value < 0) throw new RuntimeException("Value too big.");
        if (value <= 32767) {
            writeShort(value);
        } else {
            writeInt(value);
        }*/
    }

    public void writeBigSmart(int value) {
        if (value >= Short.MAX_VALUE) {
            writeInt(value - Integer.MAX_VALUE - 1);
        } else {
            writeShort(value >= 0 ? value : 32767);
        }
    }

    public void writeHugeSmart(int value) {
        for (int i = (value / 32767) - 1; i >= 0; i--) {
            writeSmart(32767);
        }
        writeSmart(value % 32767);
    }

    public String readNullString() {
        if (this.buffer[this.position] == 0) {
            this.position++;
            return null;
        }
        return readString();
    }

    public String readVersionedString() {
        return this.readVersionedString((byte) 0);
    }

    public String readVersionedString(byte versionNumber) {
        byte vNumber = this.buffer[this.position++];
        if (vNumber != versionNumber) throw new IllegalStateException("Bad string version number!");
        int pos = this.position;
        while (this.buffer[this.position++] != 0) {
            /* empty */
        }
        int strLen = this.position - pos - 1;
        if (strLen == 0) return "";
        return decodeString(buffer, pos, strLen);
    }

    public void writeVersionedString(String str) {
        this.writeVersionedString(str, (byte) 0);
    }

    public void writeVersionedString(String str, byte version) {
        int nullIdx = str.indexOf('\000');
        if (nullIdx >= 0) throw new IllegalArgumentException("NUL character at " + nullIdx + "!");
        this.buffer[this.position++] = version;
        this.position += encodeString(buffer, this.position, str, 0, str.length());
        this.buffer[this.position++] = (byte) 0;
    }

    public String readString() {
        int pos = this.position;
        while (this.buffer[this.position++] != 0) {
            /* empty */
        }
        int strlen = this.position - pos - 1;
        if (strlen == 0) return "";
        return decodeString(buffer, pos, strlen);
    }

    public void writeString(String string) {
        int n = string.indexOf('\000');
        if (n >= 0) throw new IllegalArgumentException("NUL character at " + n + "!");
        this.position += encodeString(buffer, this.position, string, 0, string.length());
        this.buffer[this.position++] = (byte) 0;
    }

    public int readSum() {
        int sum = 0;
        int incr = readUnsignedSmart();
        while (incr == 32767) {
            incr = readUnsignedSmart();
            sum += 32767;
        }
        sum += incr;
        return sum;
    }

    public int readVarSeized() {
        int f = this.buffer[this.position++];
        int sum = 0;
        for (; f < 0; f = this.buffer[this.position++]) sum = (sum | f & 127) << 7;
        return sum | f;
    }

    public void writeVarSeized(int val) {
        if ((val & ~127) != 0) {
            if ((val & ~16383) != 0) {
                if ((val & ~2097151) != 0) {
                    if ((val & ~268435455) != 0) this.writeByte(val >>> 28 | 128);
                    this.writeByte((val | 269102108) >>> 21);
                }
                this.writeByte(val >>> 14 | 128);
            }
            this.writeByte((val | 16417) >>> 7);
        }
        this.writeByte(val & 127);
    }

    public long readDynamic(int numBytes) throws IllegalArgumentException {
        if (--numBytes < 0 || numBytes > 7) throw new IllegalArgumentException();
        long value = 0L;
        for (int bitsLeft = numBytes * 8; bitsLeft >= 0; bitsLeft -= 8)
            value |= ((long) this.buffer[this.position++] & 255L) << bitsLeft;
        return value;
    }

    public void writeDynamic(int numBytes, long value) {
        if (--numBytes < 0 || numBytes > 7) throw new IllegalArgumentException();
        for (int bitsLeft = numBytes * 8; bitsLeft >= 0; bitsLeft -= 8)
            this.buffer[this.position++] = (byte) (int) (value >> bitsLeft);
    }

    public void encryptXTEA(int[] keys, int offset, int length) {
        int originalPosition = this.position;
        this.position = offset;
        int numCycles = (length - offset) / 8;
        for (int cycle = 0; cycle < numCycles; cycle++) {
            int v0 = readInt();
            int v1 = readInt();
            int sum = 0;
            int numRounds = xteaRounds;
            while (numRounds-- > 0) {
                v0 += (sum + keys[sum & 3] ^ (v1 << 4 ^ v1 >>> 5) + v1);
                sum += xteaDelta;
                v1 += (sum + keys[(sum & 7471) >>> 11] ^ v0 + (v0 << 4 ^ v0 >>> 5));
            }
            this.position -= 8;
            writeInt(v0);
            writeInt(v1);
        }
        this.position = originalPosition;
    }

    public void xteaDecrypt(int[] keys, int offset, int length) {
        int originalPosition = this.position;
        this.position = offset;
        int numCycles = (length - offset) / 8;
        for (int cycle = 0; cycle < numCycles; cycle++) {
            int v0 = readInt();
            int v1 = readInt();
            int numRounds = xteaRounds;
            int sum = xteaDelta * numRounds;
            while (numRounds-- > 0) {
                v1 -= (sum + keys[(sum & 6510) >>> 11] ^ (v0 << 4 ^ v0 >>> 5) + v0);
                sum -= xteaDelta;
                v0 -= keys[sum & 3] + sum ^ v1 + (v1 >>> 5 ^ v1 << 4);
            }
            this.position -= 8;
            this.writeInt(v0);
            this.writeInt(v1);
        }
        this.position = originalPosition;
    }

    static String decodeString(byte[] buffer, int offset, int strLen) {
        char[] strBuffer = new char[strLen];
        int write = 0;
        for (int dc = 0; dc < strLen; dc++) {
            int data = buffer[dc + offset] & 255;
            if (data == 0) continue;
            if (data >= 128 && data < 160) {
                char uni = unicodeTable[data - 128];
                if (uni == 0) uni = '?';
                strBuffer[write++] = uni;
                continue;
            }
            strBuffer[write++] = (char) data;
        }
        return new String(strBuffer, 0, write);
    }

    public char readJagexChar() {
        int b = readUnsignedByte();
        if (b >= 127 && b < 160) {
            char curChar = unicodeTable[b - 128];
            if (curChar == 0) {
                curChar = 63;
            }
            b = curChar;
        }
        return (char) b;
    }

    public static int encodeString(byte[] buffer, int bufferOffset, String str, int strOffset, int strLen) {
        int charsToEncode = strLen - strOffset;
        for (int cc = 0; cc < charsToEncode; cc++) {
            char c = str.charAt(cc + strOffset);
            if ((c > 0 && c < 128) || (c >= 160 && c <= 255)) {
                buffer[bufferOffset + cc] = (byte) c;
                continue;
            }
            switch (c) {
                case '€':
                    buffer[bufferOffset + cc] = -128;
                    break;
                case '‚':
                    buffer[bufferOffset + cc] = -126;
                    break;
                case 'ƒ':
                    buffer[bufferOffset + cc] = -125;
                    break;
                case '„':
                    buffer[bufferOffset + cc] = -124;
                    break;
                case '…':
                    buffer[bufferOffset + cc] = -123;
                    break;
                case '†':
                    buffer[bufferOffset + cc] = -122;
                    break;
                case '‡':
                    buffer[bufferOffset + cc] = -121;
                    break;
                case 'ˆ':
                    buffer[bufferOffset + cc] = -120;
                    break;
                case '‰':
                    buffer[bufferOffset + cc] = -119;
                    break;
                case 'Š':
                    buffer[bufferOffset + cc] = -118;
                    break;
                case '‹':
                    buffer[bufferOffset + cc] = -117;
                    break;
                case 'Œ':
                    buffer[bufferOffset + cc] = -116;
                    break;
                case 'Ž':
                    buffer[bufferOffset + cc] = -114;
                    break;
                case '‘':
                    buffer[bufferOffset + cc] = -111;
                    break;
                case '’':
                    buffer[bufferOffset + cc] = -110;
                    break;
                case '“':
                    buffer[bufferOffset + cc] = -109;
                    break;
                case '”':
                    buffer[bufferOffset + cc] = -108;
                    break;
                case '•':
                    buffer[bufferOffset + cc] = -107;
                    break;
                case '–':
                    buffer[bufferOffset + cc] = -106;
                    break;
                case '—':
                    buffer[bufferOffset + cc] = -105;
                    break;
                case '˜':
                    buffer[bufferOffset + cc] = -104;
                    break;
                case '™':
                    buffer[bufferOffset + cc] = -103;
                    break;
                case 'š':
                    buffer[bufferOffset + cc] = -102;
                    break;
                case '›':
                    buffer[bufferOffset + cc] = -101;
                    break;
                case 'œ':
                    buffer[bufferOffset + cc] = -100;
                    break;
                case 'ž':
                    buffer[bufferOffset + cc] = -98;
                    break;
                case 'Ÿ':
                    buffer[bufferOffset + cc] = -97;
                    break;
                default:
                    buffer[bufferOffset + cc] = (byte) '?';
                    break;
            }
        }
        return charsToEncode;
    }

    public Int2ObjectMap<Object> readParameters() {
        final int length = readUnsignedByte();
        final Int2ObjectMap<Object> parameters = new Int2ObjectOpenHashMap<>(length);
        for (int index = 0; index < length; index++) {
            final boolean stringInstance = readUnsignedByte() == 1;
            final int key = readMedium();
            Object value = stringInstance ? readString() : readInt();
            parameters.put(key, value);
        }
        return parameters;
    }

    public void writeParameters(final Int2ObjectMap<Object> parameters) {
        writeByte(parameters.size());
        for (final Int2ObjectMap.Entry<Object> parameter : parameters.int2ObjectEntrySet()) {
            final int key = parameter.getIntKey();
            final Object value = parameter.getValue();
            writeByte(value instanceof String ? 1 : 0);
            writeMedium(key);
            if (value instanceof String) {
                writeString((String) value);
            } else {
                writeInt(((Number) value).intValue());
            }
        }
    }

    public int readVarInt2() {
        int value = 0;
        int bits = 0;

        int read;
        do {
            read = this.readUnsignedByte();
            value |= (read & 127) << bits;
            bits += 7;
        } while(read > 127);

        return value;
    }

}