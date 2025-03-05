package synth;

import java.io.*;
import java.math.BigInteger;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class MusicPlayer {

    public static void main(String[] args) throws IOException {
        MidiStream midiStream = new MidiStream();
        midiStream.setMusicTrack(new MusicTrack(Files.readAllBytes(Paths.get("G:\\My Drive\\RuneScape Old School Audio Archive\\MIDI Files\\Music\\800 - Noxious Awakening.mid"))), true);
        midiStream.setInitialPatch(9, 128);
        midiStream.setPcmStreamVolume(128);
        DevicePcmPlayer devicePcmPlayer = new DevicePcmPlayer();
        devicePcmPlayer.init();
        devicePcmPlayer.setStream(midiStream);
        //devicePcmPlayer.open();
        devicePcmPlayer.samples = new int[2048];
        while (true) {
            devicePcmPlayer.fill(devicePcmPlayer.samples, 256);
            devicePcmPlayer.writeToBuffer();
        }
    }

    static class VorbisBuffer {

        static final int BUFFER_INCREMENT = 256;

        static final int[] mask = {0x00000000, 0x00000001, 0x00000003,
                0x00000007, 0x0000000f, 0x0000001f, 0x0000003f, 0x0000007f, 0x000000ff,
                0x000001ff, 0x000003ff, 0x000007ff, 0x00000fff, 0x00001fff, 0x00003fff,
                0x00007fff, 0x0000ffff, 0x0001ffff, 0x0003ffff, 0x0007ffff, 0x000fffff,
                0x001fffff, 0x003fffff, 0x007fffff, 0x00ffffff, 0x01ffffff, 0x03ffffff,
                0x07ffffff, 0x0fffffff, 0x1fffffff, 0x3fffffff, 0x7fffffff, 0xffffffff};

        int ptr = 0;
        byte[] buffer = null;
        int endbit = 0;
        int endbyte = 0;
        int storage = 0;

        public void writeinit() {
            buffer = new byte[BUFFER_INCREMENT];
            ptr = 0;
            buffer[0] = (byte) '\0';
            storage = BUFFER_INCREMENT;
        }

        public void write(byte[] s) {
            for (int i = 0; i < s.length; i++) {
                if (s[i] == 0)
                    break;
                write(s[i], 8);
            }
        }

        public void read(byte[] s, int bytes) {
            int i = 0;
            while (bytes-- != 0) {
                s[i++] = (byte) (read(8));
            }
        }

        void reset() {
            ptr = 0;
            buffer[0] = (byte) '\0';
            endbit = endbyte = 0;
        }

        public void writeclear() {
            buffer = null;
        }

        public void readinit(byte[] buf, int bytes) {
            readinit(buf, 0, bytes);
        }

        public void readinit(byte[] buf, int start, int bytes) {
            ptr = start;
            buffer = buf;
            endbit = endbyte = 0;
            storage = bytes;
        }

        public void write(int value, int bits) {
            if (endbyte + 4 >= storage) {
                byte[] foo = new byte[storage + BUFFER_INCREMENT];
                System.arraycopy(buffer, 0, foo, 0, storage);
                buffer = foo;
                storage += BUFFER_INCREMENT;
            }

            value &= mask[bits];
            bits += endbit;
            buffer[ptr] |= (byte) (value << endbit);

            if (bits >= 8) {
                buffer[ptr + 1] = (byte) (value >>> (8 - endbit));
                if (bits >= 16) {
                    buffer[ptr + 2] = (byte) (value >>> (16 - endbit));
                    if (bits >= 24) {
                        buffer[ptr + 3] = (byte) (value >>> (24 - endbit));
                        if (bits >= 32) {
                            if (endbit > 0)
                                buffer[ptr + 4] = (byte) (value >>> (32 - endbit));
                            else
                                buffer[ptr + 4] = 0;
                        }
                    }
                }
            }

            endbyte += bits / 8;
            ptr += bits / 8;
            endbit = bits & 7;
        }

        public int look(int bits) {
            int ret;
            int m = mask[bits];

            bits += endbit;

            if (endbyte + 4 >= storage) {
                if (endbyte + (bits - 1) / 8 >= storage)
                    return (-1);
            }

            ret = ((buffer[ptr]) & 0xff) >>> endbit;
            if (bits > 8) {
                ret |= ((buffer[ptr + 1]) & 0xff) << (8 - endbit);
                if (bits > 16) {
                    ret |= ((buffer[ptr + 2]) & 0xff) << (16 - endbit);
                    if (bits > 24) {
                        ret |= ((buffer[ptr + 3]) & 0xff) << (24 - endbit);
                        if (bits > 32 && endbit != 0) {
                            ret |= ((buffer[ptr + 4]) & 0xff) << (32 - endbit);
                        }
                    }
                }
            }
            return (m & ret);
        }

        public int look1() {
            if (endbyte >= storage)
                return (-1);
            return ((buffer[ptr] >> endbit) & 1);
        }

        public void adv(int bits) {
            bits += endbit;
            ptr += bits / 8;
            endbyte += bits / 8;
            endbit = bits & 7;
        }

        public void adv1() {
            ++endbit;
            if (endbit > 7) {
                endbit = 0;
                ptr++;
                endbyte++;
            }
        }

        public int read(int bits) {
            int ret;
            int m = mask[bits];

            bits += endbit;

            if (endbyte + 4 >= storage) {
                ret = -1;
                if (endbyte + (bits - 1) / 8 >= storage) {
                    ptr += bits / 8;
                    endbyte += bits / 8;
                    endbit = bits & 7;
                    return (ret);
                }
            }

            ret = ((buffer[ptr]) & 0xff) >>> endbit;
            if (bits > 8) {
                ret |= ((buffer[ptr + 1]) & 0xff) << (8 - endbit);
                if (bits > 16) {
                    ret |= ((buffer[ptr + 2]) & 0xff) << (16 - endbit);
                    if (bits > 24) {
                        ret |= ((buffer[ptr + 3]) & 0xff) << (24 - endbit);
                        if (bits > 32 && endbit != 0) {
                            ret |= ((buffer[ptr + 4]) & 0xff) << (32 - endbit);
                        }
                    }
                }
            }

            ret &= m;

            ptr += bits / 8;
            endbyte += bits / 8;
            endbit = bits & 7;
            return (ret);
        }

        public int readB(int bits) {
            int ret;
            int m = 32 - bits;

            bits += endbit;

            if (endbyte + 4 >= storage) {
                /* not the main path */
                ret = -1;
                if (endbyte * 8 + bits > storage * 8) {
                    ptr += bits / 8;
                    endbyte += bits / 8;
                    endbit = bits & 7;
                    return (ret);
                }
            }

            ret = (buffer[ptr] & 0xff) << (24 + endbit);
            if (bits > 8) {
                ret |= (buffer[ptr + 1] & 0xff) << (16 + endbit);
                if (bits > 16) {
                    ret |= (buffer[ptr + 2] & 0xff) << (8 + endbit);
                    if (bits > 24) {
                        ret |= (buffer[ptr + 3] & 0xff) << (endbit);
                        if (bits > 32 && (endbit != 0))
                            ret |= (buffer[ptr + 4] & 0xff) >> (8 - endbit);
                    }
                }
            }
            ret = (ret >>> (m >> 1)) >>> ((m + 1) >> 1);

            ptr += bits / 8;
            endbyte += bits / 8;
            endbit = bits & 7;
            return (ret);
        }

        public int read1() {
            int ret;
            if (endbyte >= storage) {
                ret = -1;
                endbit++;
                if (endbit > 7) {
                    endbit = 0;
                    ptr++;
                    endbyte++;
                }
                return (ret);
            }

            ret = (buffer[ptr] >> endbit) & 1;

            endbit++;
            if (endbit > 7) {
                endbit = 0;
                ptr++;
                endbyte++;
            }
            return (ret);
        }

        public int bytes() {
            return (endbyte + (endbit + 7) / 8);
        }

        public int bits() {
            return (endbyte * 8 + endbit);
        }

        public byte[] buffer() {
            return (buffer);
        }

        public static int ilog(int v) {
            int ret = 0;
            while (v > 0) {
                ret++;
                v >>>= 1;
            }
            return (ret);
        }

        public static void report(String in) {
            System.err.println(in);
            System.exit(1);
        }
    }

    static class Util {
        static int ilog(int v) {
            int ret = 0;
            while (v != 0) {
                ret++;
                v >>>= 1;
            }
            return (ret);
        }

        static int ilog2(int v) {
            int ret = 0;
            while (v > 1) {
                ret++;
                v >>>= 1;
            }
            return (ret);
        }

        static int icount(int v) {
            int ret = 0;
            while (v != 0) {
                ret += (v & 1);
                v >>>= 1;
            }
            return (ret);
        }
    }
    // the comments are not part of vorbis_info so that vorbis_info can be
    // static storage
    static class Comment{
        static byte[] _vorbis="vorbis".getBytes();
        static byte[] _vendor="Xiphophorus libVorbis I 20000508".getBytes();

        static final int OV_EIMPL=-130;

        // unlimited user comment fields.
        public byte[][] user_comments;
        public int[] comment_lengths;
        public int comments;
        public byte[] vendor;

        public void init(){
            user_comments=null;
            comments=0;
            vendor=null;
        }

        public void add(String comment){
            add(comment.getBytes());
        }

        private void add(byte[] comment){
            byte[][] foo=new byte[comments+2][];
            if(user_comments!=null){
                System.arraycopy(user_comments, 0, foo, 0, comments);
            }
            user_comments=foo;

            int[] goo=new int[comments+2];
            if(comment_lengths!=null){
                System.arraycopy(comment_lengths, 0, goo, 0, comments);
            }
            comment_lengths=goo;

            byte[] bar=new byte[comment.length+1];
            System.arraycopy(comment, 0, bar, 0, comment.length);
            user_comments[comments]=bar;
            comment_lengths[comments]=comment.length;
            comments++;
            user_comments[comments]=null;
        }

        public void add_tag(String tag, String contents){
            if(contents==null)
                contents="";
            add(tag+"="+contents);
        }

        static boolean tagcompare(byte[] s1, byte[] s2, int n){
            int c=0;
            byte u1, u2;
            while(c<n){
                u1=s1[c];
                u2=s2[c];
                if('Z'>=u1&&u1>='A')
                    u1=(byte)(u1-'A'+'a');
                if('Z'>=u2&&u2>='A')
                    u2=(byte)(u2-'A'+'a');
                if(u1!=u2){
                    return false;
                }
                c++;
            }
            return true;
        }

        public String query(String tag){
            return query(tag, 0);
        }

        public String query(String tag, int count){
            int foo=query(tag.getBytes(), count);
            if(foo==-1)
                return null;
            byte[] comment=user_comments[foo];
            for(int i=0; i<comment_lengths[foo]; i++){
                if(comment[i]=='='){
                    return new String(comment, i+1, comment_lengths[foo]-(i+1));
                }
            }
            return null;
        }

        private int query(byte[] tag, int count){
            int i=0;
            int found=0;
            int fulltaglen=tag.length+1;
            byte[] fulltag=new byte[fulltaglen];
            System.arraycopy(tag, 0, fulltag, 0, tag.length);
            fulltag[tag.length]=(byte)'=';

            for(i=0; i<comments; i++){
                if(tagcompare(user_comments[i], fulltag, fulltaglen)){
                    if(count==found){
                        // We return a pointer to the data, not a copy
                        //return user_comments[i] + taglen + 1;
                        return i;
                    }
                    else{
                        found++;
                    }
                }
            }
            return -1;
        }

        int unpack(VorbisBuffer opb){
            int vendorlen=opb.read(32);
            if(vendorlen<0){
                clear();
                return (-1);
            }
            vendor=new byte[vendorlen+1];
            opb.read(vendor, vendorlen);
            comments=opb.read(32);
            if(comments<0){
                clear();
                return (-1);
            }
            user_comments=new byte[comments+1][];
            comment_lengths=new int[comments+1];

            for(int i=0; i<comments; i++){
                int len=opb.read(32);
                if(len<0){
                    clear();
                    return (-1);
                }
                comment_lengths[i]=len;
                user_comments[i]=new byte[len+1];
                opb.read(user_comments[i], len);
            }
            if(opb.read(1)!=1){
                clear();
                return (-1);

            }
            return (0);
        }

        int pack(VorbisBuffer opb){
            // preamble
            opb.write(0x03, 8);
            opb.write(_vorbis);

            // vendor
            opb.write(_vendor.length, 32);
            opb.write(_vendor);

            // comments
            opb.write(comments, 32);
            if(comments!=0){
                for(int i=0; i<comments; i++){
                    if(user_comments[i]!=null){
                        opb.write(comment_lengths[i], 32);
                        opb.write(user_comments[i]);
                    }
                    else{
                        opb.write(0, 32);
                    }
                }
            }
            opb.write(1, 1);
            return (0);
        }

        public int header_out(Packet op){
            VorbisBuffer opb=new VorbisBuffer();
            opb.writeinit();

            if(pack(opb)!=0)
                return OV_EIMPL;

            op.packet_base=new byte[opb.bytes()];
            op.packet=0;
            op.bytes=opb.bytes();
            System.arraycopy(opb.buffer(), 0, op.packet_base, 0, op.bytes);
            op.b_o_s=0;
            op.e_o_s=0;
            op.granulepos=0;
            return 0;
        }

        void clear(){
            for(int i=0; i<comments; i++)
                user_comments[i]=null;
            user_comments=null;
            vendor=null;
        }

        public String getVendor(){
            return new String(vendor, 0, vendor.length-1);
        }

        public String getComment(int i){
            if(comments<=i)
                return null;
            return new String(user_comments[i], 0, user_comments[i].length-1);
        }

        public String toString(){
            String foo="Vendor: "+new String(vendor, 0, vendor.length-1);
            for(int i=0; i<comments; i++){
                foo=foo+"\nComment: "
                        +new String(user_comments[i], 0, user_comments[i].length-1);
            }
            foo=foo+"\n";
            return foo;
        }
    }

    static class ByteArrayPool {
        static int ByteArrayPool_smallCount;
        static int ByteArrayPool_mediumCount;
        static int ByteArrayPool_largeCount;
        static int field4217;
        static int field4210;
        static int field4219;
        static int field4220;
        static int field4221;
        static byte[][] ByteArrayPool_small;
        static byte[][] ByteArrayPool_medium;
        static byte[][] ByteArrayPool_large;
        static byte[][] field4225;
        static ArrayList field4212;

        static {
            ByteArrayPool_smallCount = 0; // L: 13
            ByteArrayPool_mediumCount = 0; // L: 14
            ByteArrayPool_largeCount = 0; // L: 15
            field4217 = 0; // L: 16
            field4210 = 1000; // L: 17
            field4219 = 250; // L: 18
            field4220 = 100; // L: 19
            field4221 = 50; // L: 20
            ByteArrayPool_small = new byte[1000][]; // L: 21
            ByteArrayPool_medium = new byte[250][]; // L: 22
            ByteArrayPool_large = new byte[100][]; // L: 23
            field4225 = new byte[50][];
            field4212 = new ArrayList(); // L: 28
            new HashMap();
        } // L: 36

        public static float method6364(int var0) {
            var0 &= 16383; // L: 24
            return (float)(6.283185307179586D * (double)((float)var0 / 16384.0F)); // L: 25
        }

        static synchronized byte[] ByteArrayPool_getArrayBool(int var0, boolean var1) {
            byte[] var4;
            if (var0 != 100) { // L: 70
                if (var0 < 100) {
                }
            } else if (ByteArrayPool_smallCount > 0) {
                var4 = ByteArrayPool_small[--ByteArrayPool_smallCount]; // L: 71
                ByteArrayPool_small[ByteArrayPool_smallCount] = null; // L: 72
                return var4; // L: 73
            }

            if (var0 != 5000) { // L: 75
                if (var0 < 5000) {
                }
            } else if (ByteArrayPool_mediumCount > 0) {
                var4 = ByteArrayPool_medium[--ByteArrayPool_mediumCount]; // L: 76
                ByteArrayPool_medium[ByteArrayPool_mediumCount] = null; // L: 77
                return var4; // L: 78
            }

            if (var0 != 10000) { // L: 80
                if (var0 < 10000) {
                }
            } else if (ByteArrayPool_largeCount > 0) {
                var4 = ByteArrayPool_large[--ByteArrayPool_largeCount]; // L: 81
                ByteArrayPool_large[ByteArrayPool_largeCount] = null; // L: 82
                return var4; // L: 83
            }

            if (var0 != 30000) { // L: 85
                if (var0 < 30000) {
                }
            } else if (field4217 > 0) {
                var4 = field4225[--field4217]; // L: 86
                field4225[field4217] = null; // L: 87
                return var4; // L: 88
            }

            return new byte[var0]; // L: 108
        }

    }

    static class Packet {
        public byte[] packet_base;
        public int packet;
        public int bytes;
        public int b_o_s;
        public int e_o_s;

        public long granulepos;

        /**
         * sequence number for decode; the framing
         * knows where there's a hole in the data,
         * but we need coupling so that the codec
         * (which is in a seperate abstraction
         * layer) also knows about the gap
         */
        public long packetno;

    }


    static class Page {
        static int[] crc_lookup = new int[256];

        static {
            for (int i = 0; i < crc_lookup.length; i++) {
                crc_lookup[i] = crc_entry(i);
            }
        }

        static int crc_entry(int index) {
            int r = index << 24;
            for (int i = 0; i < 8; i++) {
                if ((r & 0x80000000) != 0) {
                    r = (r << 1) ^ 0x04c11db7; /* The same as the ethernet generator
               		          polynomial, although we use an
               			  unreflected alg and an init/final
               			  of 0, not 0xffffffff */
                } else {
                    r <<= 1;
                }
            }
            return (r & 0xffffffff);
        }

        public byte[] header_base;
        public int header;
        public int header_len;
        public byte[] body_base;
        public int body;
        public int body_len;

        int version() {
            return header_base[header + 4] & 0xff;
        }

        int continued() {
            return (header_base[header + 5] & 0x01);
        }

        public int bos() {
            return (header_base[header + 5] & 0x02);
        }

        public int eos() {
            return (header_base[header + 5] & 0x04);
        }

        public long granulepos() {
            long foo = header_base[header + 13] & 0xff;
            foo = (foo << 8) | (header_base[header + 12] & 0xff);
            foo = (foo << 8) | (header_base[header + 11] & 0xff);
            foo = (foo << 8) | (header_base[header + 10] & 0xff);
            foo = (foo << 8) | (header_base[header + 9] & 0xff);
            foo = (foo << 8) | (header_base[header + 8] & 0xff);
            foo = (foo << 8) | (header_base[header + 7] & 0xff);
            foo = (foo << 8) | (header_base[header + 6] & 0xff);
            return (foo);
        }

        public int serialno() {
            return (header_base[header + 14] & 0xff) | ((header_base[header + 15] & 0xff) << 8)
                    | ((header_base[header + 16] & 0xff) << 16)
                    | ((header_base[header + 17] & 0xff) << 24);
        }

        int pageno() {
            return (header_base[header + 18] & 0xff) | ((header_base[header + 19] & 0xff) << 8)
                    | ((header_base[header + 20] & 0xff) << 16)
                    | ((header_base[header + 21] & 0xff) << 24);
        }

        void checksum() {
            int crc_reg = 0;

            for (int i = 0; i < header_len; i++) {
                crc_reg = (crc_reg << 8)
                        ^ crc_lookup[((crc_reg >>> 24) & 0xff) ^ (header_base[header + i] & 0xff)];
            }
            for (int i = 0; i < body_len; i++) {
                crc_reg = (crc_reg << 8)
                        ^ crc_lookup[((crc_reg >>> 24) & 0xff) ^ (body_base[body + i] & 0xff)];
            }
            header_base[header + 22] = (byte) crc_reg;
            header_base[header + 23] = (byte) (crc_reg >>> 8);
            header_base[header + 24] = (byte) (crc_reg >>> 16);
            header_base[header + 25] = (byte) (crc_reg >>> 24);
        }

        public Page copy() {
            return copy(new Page());
        }

        public Page copy(Page p) {
            byte[] tmp = new byte[header_len];
            System.arraycopy(header_base, header, tmp, 0, header_len);
            p.header_len = header_len;
            p.header_base = tmp;
            p.header = 0;
            tmp = new byte[body_len];
            System.arraycopy(body_base, body, tmp, 0, body_len);
            p.body_len = body_len;
            p.body_base = tmp;
            p.body = 0;
            return p;
        }

    }

    static class SyncState {

        public byte[] data;
        int storage;
        int fill;
        int returned;

        int unsynced;
        int headerbytes;
        int bodybytes;

        public int clear() {
            data = null;
            return (0);
        }

        public int buffer(int size) {
            // first, clear out any space that has been previously returned
            if (returned != 0) {
                fill -= returned;
                if (fill > 0) {
                    System.arraycopy(data, returned, data, 0, fill);
                }
                returned = 0;
            }

            if (size > storage - fill) {
                // We need to extend the internal buffer
                int newsize = size + fill + 4096; // an extra page to be nice
                if (data != null) {
                    byte[] foo = new byte[newsize];
                    System.arraycopy(data, 0, foo, 0, data.length);
                    data = foo;
                } else {
                    data = new byte[newsize];
                }
                storage = newsize;
            }

            return (fill);
        }

        public int wrote(int bytes) {
            if (fill + bytes > storage)
                return (-1);
            fill += bytes;
            return (0);
        }

        // sync the stream.  This is meant to be useful for finding page
        // boundaries.
        //
        // return values for this:
        // -n) skipped n bytes
        //  0) page not ready; more data (no bytes skipped)
        //  n) page synced at current location; page length n bytes
        private Page pageseek = new Page();
        private byte[] chksum = new byte[4];

        public int pageseek(Page og) {
            int page = returned;
            int next;
            int bytes = fill - returned;

            if (headerbytes == 0) {
                int _headerbytes, i;
                if (bytes < 27)
                    return (0); // not enough for a header

                /* verify capture pattern */
                if (data[page] != 'O' || data[page + 1] != 'g' || data[page + 2] != 'g'
                        || data[page + 3] != 'S') {
                    headerbytes = 0;
                    bodybytes = 0;

                    // search for possible capture
                    next = 0;
                    for (int ii = 0; ii < bytes - 1; ii++) {
                        if (data[page + 1 + ii] == 'O') {
                            next = page + 1 + ii;
                            break;
                        }
                    }
                    //next=memchr(page+1,'O',bytes-1);
                    if (next == 0)
                        next = fill;

                    returned = next;
                    return (-(next - page));
                }
                _headerbytes = (data[page + 26] & 0xff) + 27;
                if (bytes < _headerbytes)
                    return (0); // not enough for header + seg table

                // count up body length in the segment table

                for (i = 0; i < (data[page + 26] & 0xff); i++) {
                    bodybytes += (data[page + 27 + i] & 0xff);
                }
                headerbytes = _headerbytes;
            }

            if (bodybytes + headerbytes > bytes)
                return (0);

            // The whole test page is buffered.  Verify the checksum
            synchronized (chksum) {
                // Grab the checksum bytes, set the header field to zero

                System.arraycopy(data, page + 22, chksum, 0, 4);
                data[page + 22] = 0;
                data[page + 23] = 0;
                data[page + 24] = 0;
                data[page + 25] = 0;

                // set up a temp page struct and recompute the checksum
                Page log = pageseek;
                log.header_base = data;
                log.header = page;
                log.header_len = headerbytes;

                log.body_base = data;
                log.body = page + headerbytes;
                log.body_len = bodybytes;
                log.checksum();

                // Compare
                if (chksum[0] != data[page + 22] || chksum[1] != data[page + 23]
                        || chksum[2] != data[page + 24] || chksum[3] != data[page + 25]) {
                    // D'oh.  Mismatch! Corrupt page (or miscapture and not a page at all)
                    // replace the computed checksum with the one actually read in
                    System.arraycopy(chksum, 0, data, page + 22, 4);
                    // Bad checksum. Lose sync */

                    headerbytes = 0;
                    bodybytes = 0;
                    // search for possible capture
                    next = 0;
                    for (int ii = 0; ii < bytes - 1; ii++) {
                        if (data[page + 1 + ii] == 'O') {
                            next = page + 1 + ii;
                            break;
                        }
                    }
                    //next=memchr(page+1,'O',bytes-1);
                    if (next == 0)
                        next = fill;
                    returned = next;
                    return (-(next - page));
                }
            }

            // yes, have a whole page all ready to go
            {
                page = returned;

                if (og != null) {
                    og.header_base = data;
                    og.header = page;
                    og.header_len = headerbytes;
                    og.body_base = data;
                    og.body = page + headerbytes;
                    og.body_len = bodybytes;
                }

                unsynced = 0;
                returned += (bytes = headerbytes + bodybytes);
                headerbytes = 0;
                bodybytes = 0;
                return (bytes);
            }
        }

        // sync the stream and get a page.  Keep trying until we find a page.
        // Supress 'sync errors' after reporting the first.
        //
        // return values:
        //  -1) recapture (hole in data)
        //   0) need more data
        //   1) page returned
        //
        // Returns pointers into buffered data; invalidated by next call to
        // _stream, _clear, _init, or _buffer

        public int pageout(Page og) {
            // all we need to do is verify a page at the head of the stream
            // buffer.  If it doesn't verify, we look for the next potential
            // frame

            while (true) {
                int ret = pageseek(og);
                if (ret > 0) {
                    // have a page
                    return (1);
                }
                if (ret == 0) {
                    // need more data
                    return (0);
                }

                // head did not start a synced page... skipped some bytes
                if (unsynced == 0) {
                    unsynced = 1;
                    return (-1);
                }
                // loop. keep looking
            }
        }

        // clear things to an initial state.  Good to call, eg, before seeking
        public int reset() {
            fill = 0;
            returned = 0;
            unsynced = 0;
            headerbytes = 0;
            bodybytes = 0;
            return (0);
        }

        public void init() {
        }

        public int getDataOffset() {
            return returned;
        }

        public int getBufferOffset() {
            return fill;
        }
    }

    static class StreamState {
        byte[] body_data; /* bytes from packet bodies */
        int body_storage; /* storage elements allocated */
        int body_fill; /* elements stored; fill mark */
        private int body_returned; /* elements of fill returned */

        int[] lacing_vals; /* The values that will go to the segment table */
        long[] granule_vals; /* pcm_pos values for headers. Not compact
   		   this way, but it is simple coupled to the
   		   lacing fifo */
        int lacing_storage;
        int lacing_fill;
        int lacing_packet;
        int lacing_returned;

        byte[] header = new byte[282]; /* working space for header encode */
        int header_fill;

        public int e_o_s; /* set when we have buffered the last packet in the
   	 logical bitstream */
        int b_o_s; /* set after we've written the initial page
   of a logical bitstream */
        int serialno;
        int pageno;
        long packetno; /* sequence number for decode; the framing
                      knows where there's a hole in the data,
                      but we need coupling so that the codec
                      (which is in a seperate abstraction
                      layer) also knows about the gap */
        long granulepos;

        public StreamState() {
            init();
        }

        StreamState(int serialno) {
            this();
            init(serialno);
        }

        void init() {
            body_storage = 16 * 1024;
            body_data = new byte[body_storage];
            lacing_storage = 1024;
            lacing_vals = new int[lacing_storage];
            granule_vals = new long[lacing_storage];
        }

        public void init(int serialno) {
            if (body_data == null) {
                init();
            } else {
                for (int i = 0; i < body_data.length; i++)
                    body_data[i] = 0;
                for (int i = 0; i < lacing_vals.length; i++)
                    lacing_vals[i] = 0;
                for (int i = 0; i < granule_vals.length; i++)
                    granule_vals[i] = 0;
            }
            this.serialno = serialno;
        }

        public void clear() {
            body_data = null;
            lacing_vals = null;
            granule_vals = null;
        }

        void destroy() {
            clear();
        }

        void body_expand(int needed) {
            if (body_storage <= body_fill + needed) {
                body_storage += (needed + 1024);
                byte[] foo = new byte[body_storage];
                System.arraycopy(body_data, 0, foo, 0, body_data.length);
                body_data = foo;
            }
        }

        void lacing_expand(int needed) {
            if (lacing_storage <= lacing_fill + needed) {
                lacing_storage += (needed + 32);
                int[] foo = new int[lacing_storage];
                System.arraycopy(lacing_vals, 0, foo, 0, lacing_vals.length);
                lacing_vals = foo;

                long[] bar = new long[lacing_storage];
                System.arraycopy(granule_vals, 0, bar, 0, granule_vals.length);
                granule_vals = bar;
            }
        }

        /* submit data to the internal buffer of the framing engine */
        public int packetin(Packet op) {
            int lacing_val = op.bytes / 255 + 1;

            if (body_returned != 0) {
      /* advance packet data according to the body_returned pointer. We
         had to keep it around to return a pointer into the buffer last
         call */

                body_fill -= body_returned;
                if (body_fill != 0) {
                    System.arraycopy(body_data, body_returned, body_data, 0, body_fill);
                }
                body_returned = 0;
            }

            /* make sure we have the buffer storage */
            body_expand(op.bytes);
            lacing_expand(lacing_val);

    /* Copy in the submitted packet.  Yes, the copy is a waste; this is
       the liability of overly clean abstraction for the time being.  It
       will actually be fairly easy to eliminate the extra copy in the
       future */

            System.arraycopy(op.packet_base, op.packet, body_data, body_fill, op.bytes);
            body_fill += op.bytes;

            /* Store lacing vals for this packet */
            int j;
            for (j = 0; j < lacing_val - 1; j++) {
                lacing_vals[lacing_fill + j] = 255;
                granule_vals[lacing_fill + j] = granulepos;
            }
            lacing_vals[lacing_fill + j] = (op.bytes) % 255;
            granulepos = granule_vals[lacing_fill + j] = op.granulepos;

            /* flag the first segment as the beginning of the packet */
            lacing_vals[lacing_fill] |= 0x100;

            lacing_fill += lacing_val;

            /* for the sake of completeness */
            packetno++;

            if (op.e_o_s != 0)
                e_o_s = 1;
            return (0);
        }

        public int packetout(Packet op) {

    /* The last part of decode. We have the stream broken into packet
       segments.  Now we need to group them into packets (or return the
       out of sync markers) */

            int ptr = lacing_returned;

            if (lacing_packet <= ptr) {
                return (0);
            }

            if ((lacing_vals[ptr] & 0x400) != 0) {
                /* We lost sync here; let the app know */
                lacing_returned++;

      /* we need to tell the codec there's a gap; it might need to
         handle previous packet dependencies. */
                packetno++;
                return (-1);
            }

            /* Gather the whole packet. We'll have no holes or a partial packet */
            {
                int size = lacing_vals[ptr] & 0xff;
                int bytes = 0;

                op.packet_base = body_data;
                op.packet = body_returned;
                op.e_o_s = lacing_vals[ptr] & 0x200; /* last packet of the stream? */
                op.b_o_s = lacing_vals[ptr] & 0x100; /* first packet of the stream? */
                bytes += size;

                while (size == 255) {
                    int val = lacing_vals[++ptr];
                    size = val & 0xff;
                    if ((val & 0x200) != 0)
                        op.e_o_s = 0x200;
                    bytes += size;
                }

                op.packetno = packetno;
                op.granulepos = granule_vals[ptr];
                op.bytes = bytes;

                body_returned += bytes;

                lacing_returned = ptr + 1;
            }
            packetno++;
            return (1);
        }

        // add the incoming page to the stream state; we decompose the page
        // into packet segments here as well.

        public int pagein(Page og) {
            byte[] header_base = og.header_base;
            int header = og.header;
            byte[] body_base = og.body_base;
            int body = og.body;
            int bodysize = og.body_len;
            int segptr = 0;

            int version = og.version();
            int continued = og.continued();
            int bos = og.bos();
            int eos = og.eos();
            long granulepos = og.granulepos();
            int _serialno = og.serialno();
            int _pageno = og.pageno();
            int segments = header_base[header + 26] & 0xff;

            // clean up 'returned data'
            {
                int lr = lacing_returned;
                int br = body_returned;

                // body data
                if (br != 0) {
                    body_fill -= br;
                    if (body_fill != 0) {
                        System.arraycopy(body_data, br, body_data, 0, body_fill);
                    }
                    body_returned = 0;
                }

                if (lr != 0) {
                    // segment table
                    if ((lacing_fill - lr) != 0) {
                        System.arraycopy(lacing_vals, lr, lacing_vals, 0, lacing_fill - lr);
                        System.arraycopy(granule_vals, lr, granule_vals, 0, lacing_fill - lr);
                    }
                    lacing_fill -= lr;
                    lacing_packet -= lr;
                    lacing_returned = 0;
                }
            }

            // check the serial number
            if (_serialno != serialno)
                return (-1);
            if (version > 0)
                return (-1);

            lacing_expand(segments + 1);

            // are we in sequence?
            if (_pageno != pageno) {
                int i;

                // unroll previous partial packet (if any)
                for (i = lacing_packet; i < lacing_fill; i++) {
                    body_fill -= lacing_vals[i] & 0xff;
                    //System.out.println("??");
                }
                lacing_fill = lacing_packet;

                // make a note of dropped data in segment table
                if (pageno != -1) {
                    lacing_vals[lacing_fill++] = 0x400;
                    lacing_packet++;
                }

                // are we a 'continued packet' page?  If so, we'll need to skip
                // some segments
                if (continued != 0) {
                    bos = 0;
                    for (; segptr < segments; segptr++) {
                        int val = (header_base[header + 27 + segptr] & 0xff);
                        body += val;
                        bodysize -= val;
                        if (val < 255) {
                            segptr++;
                            break;
                        }
                    }
                }
            }

            if (bodysize != 0) {
                body_expand(bodysize);
                System.arraycopy(body_base, body, body_data, body_fill, bodysize);
                body_fill += bodysize;
            }

            {
                int saved = -1;
                while (segptr < segments) {
                    int val = (header_base[header + 27 + segptr] & 0xff);
                    lacing_vals[lacing_fill] = val;
                    granule_vals[lacing_fill] = -1;

                    if (bos != 0) {
                        lacing_vals[lacing_fill] |= 0x100;
                        bos = 0;
                    }

                    if (val < 255)
                        saved = lacing_fill;

                    lacing_fill++;
                    segptr++;

                    if (val < 255)
                        lacing_packet = lacing_fill;
                }

                /* set the granulepos on the last pcmval of the last full packet */
                if (saved != -1) {
                    granule_vals[saved] = granulepos;
                }
            }

            if (eos != 0) {
                e_o_s = 1;
                if (lacing_fill > 0)
                    lacing_vals[lacing_fill - 1] |= 0x200;
            }

            pageno = _pageno + 1;
            return (0);
        }

  /* This will flush remaining packets into a page (returning nonzero),
     even if there is not enough data to trigger a flush normally
     (undersized page). If there are no packets or partial packets to
     flush, ogg_stream_flush returns 0.  Note that ogg_stream_flush will
     try to flush a normal sized page like ogg_stream_pageout; a call to
     ogg_stream_flush does not gurantee that all packets have flushed.
     Only a return value of 0 from ogg_stream_flush indicates all packet
     data is flushed into pages.

     ogg_stream_page will flush the last page in a stream even if it's
     undersized; you almost certainly want to use ogg_stream_pageout
     (and *not* ogg_stream_flush) unless you need to flush an undersized
     page in the middle of a stream for some reason. */

        public int flush(Page og) {

            int i;
            int vals = 0;
            int maxvals = (lacing_fill > 255 ? 255 : lacing_fill);
            int bytes = 0;
            int acc = 0;
            long granule_pos = granule_vals[0];

            if (maxvals == 0)
                return (0);

            /* construct a page */
            /* decide how many segments to include */

    /* If this is the initial header case, the first page must only include
       the initial header packet */
            if (b_o_s == 0) { /* 'initial header page' case */
                granule_pos = 0;
                for (vals = 0; vals < maxvals; vals++) {
                    if ((lacing_vals[vals] & 0x0ff) < 255) {
                        vals++;
                        break;
                    }
                }
            } else {
                for (vals = 0; vals < maxvals; vals++) {
                    if (acc > 4096)
                        break;
                    acc += (lacing_vals[vals] & 0x0ff);
                    granule_pos = granule_vals[vals];
                }
            }

            /* construct the header in temp storage */
            System.arraycopy("OggS".getBytes(), 0, header, 0, 4);

            /* stream structure version */
            header[4] = 0x00;

            /* continued packet flag? */
            header[5] = 0x00;
            if ((lacing_vals[0] & 0x100) == 0)
                header[5] |= 0x01;
            /* first page flag? */
            if (b_o_s == 0)
                header[5] |= 0x02;
            /* last page flag? */
            if (e_o_s != 0 && lacing_fill == vals)
                header[5] |= 0x04;
            b_o_s = 1;

            /* 64 bits of PCM position */
            for (i = 6; i < 14; i++) {
                header[i] = (byte) granule_pos;
                granule_pos >>>= 8;
            }

            /* 32 bits of stream serial number */
            {
                int _serialno = serialno;
                for (i = 14; i < 18; i++) {
                    header[i] = (byte) _serialno;
                    _serialno >>>= 8;
                }
            }

    /* 32 bits of page counter (we have both counter and page header
       because this val can roll over) */
            if (pageno == -1)
                pageno = 0; /* because someone called
                stream_reset; this would be a
                strange thing to do in an
                encode stream, but it has
                plausible uses */
            {
                int _pageno = pageno++;
                for (i = 18; i < 22; i++) {
                    header[i] = (byte) _pageno;
                    _pageno >>>= 8;
                }
            }

            /* zero for computation; filled in later */
            header[22] = 0;
            header[23] = 0;
            header[24] = 0;
            header[25] = 0;

            /* segment table */
            header[26] = (byte) vals;
            for (i = 0; i < vals; i++) {
                header[i + 27] = (byte) lacing_vals[i];
                bytes += (header[i + 27] & 0xff);
            }

            /* set pointers in the ogg_page struct */
            og.header_base = header;
            og.header = 0;
            og.header_len = header_fill = vals + 27;
            og.body_base = body_data;
            og.body = body_returned;
            og.body_len = bytes;

            /* advance the lacing data and set the body_returned pointer */

            lacing_fill -= vals;
            System.arraycopy(lacing_vals, vals, lacing_vals, 0, lacing_fill * 4);
            System.arraycopy(granule_vals, vals, granule_vals, 0, lacing_fill * 8);
            body_returned += bytes;

            /* calculate the checksum */

            og.checksum();

            /* done */
            return (1);
        }

        /* This constructs pages from buffered packet segments.  The pointers
        returned are to static buffers; do not free. The returned buffers are
        good only until the next call (using the same ogg_stream_state) */
        public int pageout(Page og) {
            if ((e_o_s != 0 && lacing_fill != 0) || /* 'were done, now flush' case */
                    body_fill - body_returned > 4096 || /* 'page nominal size' case */
                    lacing_fill >= 255 || /* 'segment table full' case */
                    (lacing_fill != 0 && b_o_s == 0)) { /* 'initial header page' case */
                return flush(og);
            }
            return 0;
        }

        public int eof() {
            return e_o_s;
        }

        public int reset() {
            body_fill = 0;
            body_returned = 0;

            lacing_fill = 0;
            lacing_packet = 0;
            lacing_returned = 0;

            header_fill = 0;

            e_o_s = 0;
            b_o_s = 0;
            pageno = -1;
            packetno = 0;
            granulepos = 0;
            return (0);
        }
    }

    static class StaticCodeBook {
        int dim; // codebook dimensions (elements per vector)
        int entries; // codebook entries
        int[] lengthlist; // codeword lengths in bits

        // mapping
        int maptype; // 0=none
        // 1=implicitly populated values from map column
        // 2=listed arbitrary values

        // The below does a linear, single monotonic sequence mapping.
        int q_min; // packed 32 bit float; quant value 0 maps to minval
        int q_delta; // packed 32 bit float; val 1 - val 0 == delta
        int q_quant; // bits: 0 < quant <= 16
        int q_sequencep; // bitflag

        // additional information for log (dB) mapping; the linear mapping
        // is assumed to actually be values in dB.  encodebias is used to
        // assign an error weight to 0 dB. We have two additional flags:
        // zeroflag indicates if entry zero is to represent -Inf dB; negflag
        // indicates if we're to represent negative linear values in a
        // mirror of the positive mapping.

        int[] quantlist; // map == 1: (int)(entries/dim) element column map
        // map == 2: list of dim*entries quantized entry vals

        StaticCodeBook() {
        }

        int pack(VorbisBuffer opb) {
            int i;
            boolean ordered = false;

            opb.write(0x564342, 24);
            opb.write(dim, 16);
            opb.write(entries, 24);

            // pack the codewords.  There are two packings; length ordered and
            // length random.  Decide between the two now.

            for (i = 1; i < entries; i++) {
                if (lengthlist[i] < lengthlist[i - 1])
                    break;
            }
            if (i == entries)
                ordered = true;

            if (ordered) {
                // length ordered.  We only need to say how many codewords of
                // each length.  The actual codewords are generated
                // deterministically

                int count = 0;
                opb.write(1, 1); // ordered
                opb.write(lengthlist[0] - 1, 5); // 1 to 32

                for (i = 1; i < entries; i++) {
                    int _this = lengthlist[i];
                    int _last = lengthlist[i - 1];
                    if (_this > _last) {
                        for (int j = _last; j < _this; j++) {
                            opb.write(i - count, Util.ilog(entries - count));
                            count = i;
                        }
                    }
                }
                opb.write(i - count, Util.ilog(entries - count));
            } else {
                // length random.  Again, we don't code the codeword itself, just
                // the length.  This time, though, we have to encode each length
                opb.write(0, 1); // unordered

                // algortihmic mapping has use for 'unused entries', which we tag
                // here.  The algorithmic mapping happens as usual, but the unused
                // entry has no codeword.
                for (i = 0; i < entries; i++) {
                    if (lengthlist[i] == 0)
                        break;
                }

                if (i == entries) {
                    opb.write(0, 1); // no unused entries
                    for (i = 0; i < entries; i++) {
                        opb.write(lengthlist[i] - 1, 5);
                    }
                } else {
                    opb.write(1, 1); // we have unused entries; thus we tag
                    for (i = 0; i < entries; i++) {
                        if (lengthlist[i] == 0) {
                            opb.write(0, 1);
                        } else {
                            opb.write(1, 1);
                            opb.write(lengthlist[i] - 1, 5);
                        }
                    }
                }
            }

            // is the entry number the desired return value, or do we have a
            // mapping? If we have a mapping, what type?
            opb.write(maptype, 4);
            switch (maptype) {
                case 0:
                    // no mapping
                    break;
                case 1:
                case 2:
                    // implicitly populated value mapping
                    // explicitly populated value mapping
                    if (quantlist == null) {
                        // no quantlist?  error
                        return (-1);
                    }

                    // values that define the dequantization
                    opb.write(q_min, 32);
                    opb.write(q_delta, 32);
                    opb.write(q_quant - 1, 4);
                    opb.write(q_sequencep, 1);

                {
                    int quantvals = 0;
                    switch (maptype) {
                        case 1:
                            // a single column of (c->entries/c->dim) quantized values for
                            // building a full value list algorithmically (square lattice)
                            quantvals = maptype1_quantvals();
                            break;
                        case 2:
                            // every value (c->entries*c->dim total) specified explicitly
                            quantvals = entries * dim;
                            break;
                    }

                    // quantized values
                    for (i = 0; i < quantvals; i++) {
                        opb.write(Math.abs(quantlist[i]), q_quant);
                    }
                }
                break;
                default:
                    // error case; we don't have any other map types now
                    return (-1);
            }
            return (0);
        }

        // unpacks a codebook from the packet buffer into the codebook struct,
        // readies the codebook auxiliary structures for decode
        int unpack(VorbisBuffer opb) {
            int i;
            //memset(s,0,sizeof(static_codebook));

            // make sure alignment is correct
            if (opb.read(24) != 0x564342) {
                //    goto _eofout;
                clear();
                return (-1);
            }

            // first the basic parameters
            dim = opb.read(16);
            entries = opb.read(24);
            if (entries == -1) {
                //    goto _eofout;
                clear();
                return (-1);
            }

            // codeword ordering.... length ordered or unordered?
            switch (opb.read(1)) {
                case 0:
                    // unordered
                    lengthlist = new int[entries];

                    // allocated but unused entries?
                    if (opb.read(1) != 0) {
                        // yes, unused entries

                        for (i = 0; i < entries; i++) {
                            if (opb.read(1) != 0) {
                                int num = opb.read(5);
                                if (num == -1) {
                                    //            goto _eofout;
                                    clear();
                                    return (-1);
                                }
                                lengthlist[i] = num + 1;
                            } else {
                                lengthlist[i] = 0;
                            }
                        }
                    } else {
                        // all entries used; no tagging
                        for (i = 0; i < entries; i++) {
                            int num = opb.read(5);
                            if (num == -1) {
                                //          goto _eofout;
                                clear();
                                return (-1);
                            }
                            lengthlist[i] = num + 1;
                        }
                    }
                    break;
                case 1:
                    // ordered
                {
                    int length = opb.read(5) + 1;
                    lengthlist = new int[entries];

                    for (i = 0; i < entries; ) {
                        int num = opb.read(Util.ilog(entries - i));
                        if (num == -1) {
                            //          goto _eofout;
                            clear();
                            return (-1);
                        }
                        for (int j = 0; j < num; j++, i++) {
                            lengthlist[i] = length;
                        }
                        length++;
                    }
                }
                break;
                default:
                    // EOF
                    return (-1);
            }

            // Do we have a mapping to unpack?
            switch ((maptype = opb.read(4))) {
                case 0:
                    // no mapping
                    break;
                case 1:
                case 2:
                    // implicitly populated value mapping
                    // explicitly populated value mapping
                    q_min = opb.read(32);
                    q_delta = opb.read(32);
                    q_quant = opb.read(4) + 1;
                    q_sequencep = opb.read(1);

                {
                    int quantvals = 0;
                    switch (maptype) {
                        case 1:
                            quantvals = maptype1_quantvals();
                            break;
                        case 2:
                            quantvals = entries * dim;
                            break;
                    }

                    // quantized values
                    quantlist = new int[quantvals];
                    for (i = 0; i < quantvals; i++) {
                        quantlist[i] = opb.read(q_quant);
                    }
                    if (quantlist[quantvals - 1] == -1) {
                        //        goto _eofout;
                        clear();
                        return (-1);
                    }
                }
                break;
                default:
                    //    goto _eofout;
                    clear();
                    return (-1);
            }
            // all set
            return (0);
            //    _errout:
            //    _eofout:
            //    vorbis_staticbook_clear(s);
            //    return(-1);
        }

        // there might be a straightforward one-line way to do the below
        // that's portable and totally safe against roundoff, but I haven't
        // thought of it.  Therefore, we opt on the side of caution
        private int maptype1_quantvals() {
            int vals = (int) (Math.floor(Math.pow(entries, 1. / dim)));

            // the above *should* be reliable, but we'll not assume that FP is
            // ever reliable when bitstream sync is at stake; verify via integer
            // means that vals really is the greatest value of dim for which
            // vals^b->bim <= b->entries
            // treat the above as an initial guess
            while (true) {
                int acc = 1;
                int acc1 = 1;
                for (int i = 0; i < dim; i++) {
                    acc *= vals;
                    acc1 *= vals + 1;
                }
                if (acc <= entries && acc1 > entries) {
                    return (vals);
                } else {
                    if (acc > entries) {
                        vals--;
                    } else {
                        vals++;
                    }
                }
            }
        }

        void clear() {
        }

        // unpack the quantized list of values for encode/decode
        // we need to deal with two map types: in map type 1, the values are
        // generated algorithmically (each column of the vector counts through
        // the values in the quant vector). in map type 2, all the values came
        // in in an explicit list.  Both value lists must be unpacked
        float[] unquantize() {

            if (maptype == 1 || maptype == 2) {
                int quantvals;
                float mindel = float32_unpack(q_min);
                float delta = float32_unpack(q_delta);
                float[] r = new float[entries * dim];

                // maptype 1 and 2 both use a quantized value vector, but
                // different sizes
                switch (maptype) {
                    case 1:
                        // most of the time, entries%dimensions == 0, but we need to be
                        // well defined.  We define that the possible vales at each
                        // scalar is values == entries/dim.  If entries%dim != 0, we'll
                        // have 'too few' values (values*dim<entries), which means that
                        // we'll have 'left over' entries; left over entries use zeroed
                        // values (and are wasted).  So don't generate codebooks like that
                        quantvals = maptype1_quantvals();
                        for (int j = 0; j < entries; j++) {
                            float last = 0.f;
                            int indexdiv = 1;
                            for (int k = 0; k < dim; k++) {
                                int index = (j / indexdiv) % quantvals;
                                float val = quantlist[index];
                                val = Math.abs(val) * delta + mindel + last;
                                if (q_sequencep != 0)
                                    last = val;
                                r[j * dim + k] = val;
                                indexdiv *= quantvals;
                            }
                        }
                        break;
                    case 2:
                        for (int j = 0; j < entries; j++) {
                            float last = 0.f;
                            for (int k = 0; k < dim; k++) {
                                float val = quantlist[j * dim + k];
                                //if((j*dim+k)==0){System.err.println(" | 0 -> "+val+" | ");}
                                val = Math.abs(val) * delta + mindel + last;
                                if (q_sequencep != 0)
                                    last = val;
                                r[j * dim + k] = val;
                                //if((j*dim+k)==0){System.err.println(" $ r[0] -> "+r[0]+" | ");}
                            }
                        }
                        //System.err.println("\nr[0]="+r[0]);
                }
                return (r);
            }
            return (null);
        }

        // 32 bit float (not IEEE; nonnormalized mantissa +
        // biased exponent) : neeeeeee eeemmmmm mmmmmmmm mmmmmmmm
        // Why not IEEE?  It's just not that important here.

        static final int VQ_FEXP = 10;
        static final int VQ_FMAN = 21;
        static final int VQ_FEXP_BIAS = 768; // bias toward values smaller than 1.

        // doesn't currently guard under/overflow
        static long float32_pack(float val) {
            int sign = 0;
            int exp;
            int mant;
            if (val < 0) {
                sign = 0x80000000;
                val = -val;
            }
            exp = (int) Math.floor(Math.log(val) / Math.log(2));
            mant = (int) Math.rint(Math.pow(val, (VQ_FMAN - 1) - exp));
            exp = (exp + VQ_FEXP_BIAS) << VQ_FMAN;
            return (sign | exp | mant);
        }

        static float float32_unpack(int val) {
            float mant = val & 0x1fffff;
            float exp = (val & 0x7fe00000) >>> VQ_FMAN;
            if ((val & 0x80000000) != 0)
                mant = -mant;
            return (ldexp(mant, ((int) exp) - (VQ_FMAN - 1) - VQ_FEXP_BIAS));
        }

        static float ldexp(float foo, int e) {
            return (float) (foo * Math.pow(2, e));
        }
    }

    // psychoacoustic setup
    static class PsyInfo {
        int athp;
        int decayp;
        int smoothp;
        int noisefitp;
        int noisefit_subblock;
        float noisefit_threshdB;

        float ath_att;

        int tonemaskp;
        float[] toneatt_125Hz = new float[5];
        float[] toneatt_250Hz = new float[5];
        float[] toneatt_500Hz = new float[5];
        float[] toneatt_1000Hz = new float[5];
        float[] toneatt_2000Hz = new float[5];
        float[] toneatt_4000Hz = new float[5];
        float[] toneatt_8000Hz = new float[5];

        int peakattp;
        float[] peakatt_125Hz = new float[5];
        float[] peakatt_250Hz = new float[5];
        float[] peakatt_500Hz = new float[5];
        float[] peakatt_1000Hz = new float[5];
        float[] peakatt_2000Hz = new float[5];
        float[] peakatt_4000Hz = new float[5];
        float[] peakatt_8000Hz = new float[5];

        int noisemaskp;
        float[] noiseatt_125Hz = new float[5];
        float[] noiseatt_250Hz = new float[5];
        float[] noiseatt_500Hz = new float[5];
        float[] noiseatt_1000Hz = new float[5];
        float[] noiseatt_2000Hz = new float[5];
        float[] noiseatt_4000Hz = new float[5];
        float[] noiseatt_8000Hz = new float[5];

        float max_curve_dB;

        float attack_coeff;
        float decay_coeff;

        void free() {
        }
    }

    static class InfoMode {
        int blockflag;
        int windowtype;
        int transformtype;
        int mapping;
    }

    static class Block {
        ///necessary stream state for linking to the framing abstraction
        float[][] pcm = new float[0][]; // this is a pointer into local storage
        VorbisBuffer opb = new VorbisBuffer();

        int lW;
        int W;
        int nW;
        int pcmend;
        int mode;

        int eofflag;
        long granulepos;
        long sequence;
        DspState vd; // For read-only access of configuration

        // bitmetrics for the frame
        int glue_bits;
        int time_bits;
        int floor_bits;
        int res_bits;

        public Block(DspState vd) {
            this.vd = vd;
            if (vd.analysisp != 0) {
                opb.writeinit();
            }
        }

        public void init(DspState vd) {
            this.vd = vd;
        }

        public int clear() {
            if (vd != null) {
                if (vd.analysisp != 0) {
                    opb.writeclear();
                }
            }
            return (0);
        }

        public int synthesis(Packet op) {
            Info vi = vd.vi;

            // first things first.  Make sure decode is ready
            opb.readinit(op.packet_base, op.packet, op.bytes);

            // Check the packet type
            if (opb.read(1) != 0) {
                // Oops.  This is not an audio data packet
                return (-1);
            }

            // read our mode and pre/post windowsize
            int _mode = opb.read(vd.modebits);
            if (_mode == -1)
                return (-1);

            mode = _mode;
            W = vi.mode_param[mode].blockflag;
            if (W != 0) {
                lW = opb.read(1);
                nW = opb.read(1);
                if (nW == -1)
                    return (-1);
            } else {
                lW = 0;
                nW = 0;
            }

            // more setup
            granulepos = op.granulepos;
            sequence = op.packetno - 3; // first block is third packet
            eofflag = op.e_o_s;

            // alloc pcm passback storage
            pcmend = vi.blocksizes[W];
            if (pcm.length < vi.channels) {
                pcm = new float[vi.channels][];
            }
            for (int i = 0; i < vi.channels; i++) {
                if (pcm[i] == null || pcm[i].length < pcmend) {
                    pcm[i] = new float[pcmend];
                } else {
                    for (int j = 0; j < pcmend; j++) {
                        pcm[i][j] = 0;
                    }
                }
            }

            // unpack_header enforces range checking
            int type = vi.map_type[vi.mode_param[mode].mapping];
            return (FuncMapping.mapping_P[type].inverse(this, vd.mode[mode]));
        }
    }
    static class Time0 extends FuncTime {
        void pack(Object i, VorbisBuffer opb){
        }

        Object unpack(Info vi, VorbisBuffer opb){
            return "";
        }

        Object look(DspState vd, InfoMode mi, Object i){
            return "";
        }

        void free_info(Object i){
        }

        void free_look(Object i){
        }

        int inverse(Block vb, Object i, float[] in, float[] out){
            return 0;
        }
    }

    static class Mapping0 extends FuncMapping {
        static int seq=0;

        void free_info(Object imap){
        };

        void free_look(Object imap){
        }

        Object look(DspState vd, InfoMode vm, Object m){
            //System.err.println("Mapping0.look");
            Info vi=vd.vi;
            Mapping0.LookMapping0 look=new Mapping0.LookMapping0();
            Mapping0.InfoMapping0 info=look.map=(Mapping0.InfoMapping0)m;
            look.mode=vm;

            look.time_look=new Object[info.submaps];
            look.floor_look=new Object[info.submaps];
            look.residue_look=new Object[info.submaps];

            look.time_func=new FuncTime[info.submaps];
            look.floor_func=new FuncFloor[info.submaps];
            look.residue_func=new FuncResidue[info.submaps];

            for(int i=0; i<info.submaps; i++){
                int timenum=info.timesubmap[i];
                int floornum=info.floorsubmap[i];
                int resnum=info.residuesubmap[i];

                look.time_func[i]= FuncTime.time_P[vi.time_type[timenum]];
                look.time_look[i]=look.time_func[i].look(vd, vm, vi.time_param[timenum]);
                look.floor_func[i]= FuncFloor.floor_P[vi.floor_type[floornum]];
                look.floor_look[i]=look.floor_func[i].look(vd, vm,
                        vi.floor_param[floornum]);
                look.residue_func[i]= FuncResidue.residue_P[vi.residue_type[resnum]];
                look.residue_look[i]=look.residue_func[i].look(vd, vm,
                        vi.residue_param[resnum]);

            }

            if(vi.psys!=0&&vd.analysisp!=0){
                // ??
            }

            look.ch=vi.channels;

            return (look);
        }

        void pack(Info vi, Object imap, VorbisBuffer opb){
            Mapping0.InfoMapping0 info=(Mapping0.InfoMapping0)imap;

    /* another 'we meant to do it this way' hack...  up to beta 4, we
       packed 4 binary zeros here to signify one submapping in use.  We
       now redefine that to mean four bitflags that indicate use of
       deeper features; bit0:submappings, bit1:coupling,
       bit2,3:reserved. This is backward compatable with all actual uses
       of the beta code. */

            if(info.submaps>1){
                opb.write(1, 1);
                opb.write(info.submaps-1, 4);
            }
            else{
                opb.write(0, 1);
            }

            if(info.coupling_steps>0){
                opb.write(1, 1);
                opb.write(info.coupling_steps-1, 8);
                for(int i=0; i<info.coupling_steps; i++){
                    opb.write(info.coupling_mag[i], Util.ilog2(vi.channels));
                    opb.write(info.coupling_ang[i], Util.ilog2(vi.channels));
                }
            }
            else{
                opb.write(0, 1);
            }

            opb.write(0, 2); /* 2,3:reserved */

            /* we don't write the channel submappings if we only have one... */
            if(info.submaps>1){
                for(int i=0; i<vi.channels; i++)
                    opb.write(info.chmuxlist[i], 4);
            }
            for(int i=0; i<info.submaps; i++){
                opb.write(info.timesubmap[i], 8);
                opb.write(info.floorsubmap[i], 8);
                opb.write(info.residuesubmap[i], 8);
            }
        }

        // also responsible for range checking
        Object unpack(Info vi, VorbisBuffer opb){
            Mapping0.InfoMapping0 info=new Mapping0.InfoMapping0();

            if(opb.read(1)!=0){
                info.submaps=opb.read(4)+1;
            }
            else{
                info.submaps=1;
            }

            if(opb.read(1)!=0){
                info.coupling_steps=opb.read(8)+1;

                for(int i=0; i<info.coupling_steps; i++){
                    int testM=info.coupling_mag[i]=opb.read(Util.ilog2(vi.channels));
                    int testA=info.coupling_ang[i]=opb.read(Util.ilog2(vi.channels));

                    if(testM<0||testA<0||testM==testA||testM>=vi.channels
                            ||testA>=vi.channels){
                        //goto err_out;
                        info.free();
                        return (null);
                    }
                }
            }

            if(opb.read(2)>0){ /* 2,3:reserved */
                info.free();
                return (null);
            }

            if(info.submaps>1){
                for(int i=0; i<vi.channels; i++){
                    info.chmuxlist[i]=opb.read(4);
                    if(info.chmuxlist[i]>=info.submaps){
                        info.free();
                        return (null);
                    }
                }
            }

            for(int i=0; i<info.submaps; i++){
                info.timesubmap[i]=opb.read(8);
                if(info.timesubmap[i]>=vi.times){
                    info.free();
                    return (null);
                }
                info.floorsubmap[i]=opb.read(8);
                if(info.floorsubmap[i]>=vi.floors){
                    info.free();
                    return (null);
                }
                info.residuesubmap[i]=opb.read(8);
                if(info.residuesubmap[i]>=vi.residues){
                    info.free();
                    return (null);
                }
            }
            return info;
        }

        float[][] pcmbundle=null;
        int[] zerobundle=null;
        int[] nonzero=null;
        Object[] floormemo=null;

        synchronized int inverse(Block vb, Object l){
            DspState vd=vb.vd;
            Info vi=vd.vi;
            Mapping0.LookMapping0 look=(Mapping0.LookMapping0)l;
            Mapping0.InfoMapping0 info=look.map;
            InfoMode mode=look.mode;
            int n=vb.pcmend=vi.blocksizes[vb.W];

            float[] window=vd.window[vb.W][vb.lW][vb.nW][mode.windowtype];
            if(pcmbundle==null||pcmbundle.length<vi.channels){
                pcmbundle=new float[vi.channels][];
                nonzero=new int[vi.channels];
                zerobundle=new int[vi.channels];
                floormemo=new Object[vi.channels];
            }

            // time domain information decode (note that applying the
            // information would have to happen later; we'll probably add a
            // function entry to the harness for that later
            // NOT IMPLEMENTED

            // recover the spectral envelope; store it in the PCM vector for now
            for(int i=0; i<vi.channels; i++){
                float[] pcm=vb.pcm[i];
                int submap=info.chmuxlist[i];

                floormemo[i]=look.floor_func[submap].inverse1(vb,
                        look.floor_look[submap], floormemo[i]);
                if(floormemo[i]!=null){
                    nonzero[i]=1;
                }
                else{
                    nonzero[i]=0;
                }
                for(int j=0; j<n/2; j++){
                    pcm[j]=0;
                }

            }

            for(int i=0; i<info.coupling_steps; i++){
                if(nonzero[info.coupling_mag[i]]!=0||nonzero[info.coupling_ang[i]]!=0){
                    nonzero[info.coupling_mag[i]]=1;
                    nonzero[info.coupling_ang[i]]=1;
                }
            }

            // recover the residue, apply directly to the spectral envelope

            for(int i=0; i<info.submaps; i++){
                int ch_in_bundle=0;
                for(int j=0; j<vi.channels; j++){
                    if(info.chmuxlist[j]==i){
                        if(nonzero[j]!=0){
                            zerobundle[ch_in_bundle]=1;
                        }
                        else{
                            zerobundle[ch_in_bundle]=0;
                        }
                        pcmbundle[ch_in_bundle++]=vb.pcm[j];
                    }
                }

                look.residue_func[i].inverse(vb, look.residue_look[i], pcmbundle,
                        zerobundle, ch_in_bundle);
            }

            for(int i=info.coupling_steps-1; i>=0; i--){
                float[] pcmM=vb.pcm[info.coupling_mag[i]];
                float[] pcmA=vb.pcm[info.coupling_ang[i]];

                for(int j=0; j<n/2; j++){
                    float mag=pcmM[j];
                    float ang=pcmA[j];

                    if(mag>0){
                        if(ang>0){
                            pcmM[j]=mag;
                            pcmA[j]=mag-ang;
                        }
                        else{
                            pcmA[j]=mag;
                            pcmM[j]=mag+ang;
                        }
                    }
                    else{
                        if(ang>0){
                            pcmM[j]=mag;
                            pcmA[j]=mag+ang;
                        }
                        else{
                            pcmA[j]=mag;
                            pcmM[j]=mag-ang;
                        }
                    }
                }
            }

            //    /* compute and apply spectral envelope */

            for(int i=0; i<vi.channels; i++){
                float[] pcm=vb.pcm[i];
                int submap=info.chmuxlist[i];
                look.floor_func[submap].inverse2(vb, look.floor_look[submap],
                        floormemo[i], pcm);
            }

            // transform the PCM data; takes PCM vector, vb; modifies PCM vector
            // only MDCT right now....

            for(int i=0; i<vi.channels; i++){
                float[] pcm=vb.pcm[i];
                //_analysis_output("out",seq+i,pcm,n/2,0,0);
                ((Mdct)vd.transform[vb.W][0]).backward(pcm, pcm);
            }

            // now apply the decoded pre-window time information
            // NOT IMPLEMENTED

            // window the data
            for(int i=0; i<vi.channels; i++){
                float[] pcm=vb.pcm[i];
                if(nonzero[i]!=0){
                    for(int j=0; j<n; j++){
                        pcm[j]*=window[j];
                    }
                }
                else{
                    for(int j=0; j<n; j++){
                        pcm[j]=0.f;
                    }
                }
            }

            // now apply the decoded post-window time information
            // NOT IMPLEMENTED
            // all done!
            return (0);
        }

        class InfoMapping0{
            int submaps; // <= 16
            int[] chmuxlist=new int[256]; // up to 256 channels in a Vorbis stream

            int[] timesubmap=new int[16]; // [mux]
            int[] floorsubmap=new int[16]; // [mux] submap to floors
            int[] residuesubmap=new int[16];// [mux] submap to residue
            int[] psysubmap=new int[16]; // [mux]; encode only

            int coupling_steps;
            int[] coupling_mag=new int[256];
            int[] coupling_ang=new int[256];

            void free(){
                chmuxlist=null;
                timesubmap=null;
                floorsubmap=null;
                residuesubmap=null;
                psysubmap=null;

                coupling_mag=null;
                coupling_ang=null;
            }
        }

        class LookMapping0{
            InfoMode mode;
            Mapping0.InfoMapping0 map;
            Object[] time_look;
            Object[] floor_look;
            Object[] floor_state;
            Object[] residue_look;
            PsyLook[] psy_look;

            FuncTime[] time_func;
            FuncFloor[] floor_func;
            FuncResidue[] residue_func;

            int ch;
            float[][] decay;
            int lastframe; // if a different mode is called, we need to
            // invalidate decay and floor state
        }

        class PsyLook{
            int n;
            PsyInfo vi;

            float[][][] tonecurves;
            float[][] peakatt;
            float[][][] noisecurves;

            float[] ath;
            int[] octave;

            void init(PsyInfo vi, int n, int rate){
            }
        }


    }

    static abstract class FuncMapping {

        public static FuncMapping[] mapping_P = {new Mapping0()};

        abstract void pack(Info info, Object imap, VorbisBuffer buffer);

        abstract Object unpack(Info info, VorbisBuffer buffer);

        abstract Object look(DspState vd, InfoMode vm, Object m);

        abstract void free_info(Object imap);

        abstract void free_look(Object imap);

        abstract int inverse(Block vd, Object lm);

    }

    abstract static class FuncTime {

        public static FuncTime[] time_P = {new Time0()};

        abstract void pack(Object i, VorbisBuffer opb);

        abstract Object unpack(Info vi, VorbisBuffer opb);

        abstract Object look(DspState vd, InfoMode vm, Object i);

        abstract void free_info(Object i);

        abstract void free_look(Object i);

        abstract int inverse(Block vb, Object i, float[] in, float[] out);
    }

    static class Info {
        static final int OV_EBADPACKET = -136;
        static final int OV_ENOTAUDIO = -135;

        private final byte[] _vorbis = "vorbis".getBytes();
        static final int VI_TIMEB = 1;
        //  static final int VI_FLOORB=1;
        static final int VI_FLOORB = 2;
        //  static final int VI_RESB=1;
        static final int VI_RESB = 3;
        static final int VI_MAPB = 1;
        static final int VI_WINDOWB = 1;

        public int version;
        public int channels;
        public int rate;

        // The below bitrate declarations are *hints*.
        // Combinations of the three values carry the following implications:
        //
        // all three set to the same value:
        // implies a fixed rate bitstream
        // only nominal set:
        // implies a VBR stream that averages the nominal bitrate.  No hard
        // upper/lower limit
        // upper and or lower set:
        // implies a VBR bitstream that obeys the bitrate limits. nominal
        // may also be set to give a nominal rate.
        // none set:
        //  the coder does not care to speculate.

        int bitrate_upper;
        int bitrate_nominal;
        int bitrate_lower;

        // Vorbis supports only short and long blocks, but allows the
        // encoder to choose the sizes

        int[] blocksizes = new int[2];

        // modes are the primary means of supporting on-the-fly different
        // blocksizes, different channel mappings (LR or mid-side),
        // different residue backends, etc.  Each mode consists of a
        // blocksize flag and a mapping (along with the mapping setup

        int modes;
        int maps;
        int times;
        int floors;
        int residues;
        int books;
        int psys; // encode only

        InfoMode[] mode_param = null;

        int[] map_type = null;
        Object[] map_param = null;

        int[] time_type = null;
        Object[] time_param = null;

        int[] floor_type = null;
        Object[] floor_param = null;

        int[] residue_type = null;
        Object[] residue_param = null;

        StaticCodeBook[] book_param = null;

        PsyInfo[] psy_param = new PsyInfo[64]; // encode only

        // for block long/sort tuning; encode only
        int envelopesa;
        float preecho_thresh;
        float preecho_clamp;

        // used by synthesis, which has a full, alloced vi
        public void init() {
            rate = 0;
        }

        public void clear() {
            for (int i = 0; i < modes; i++) {
                mode_param[i] = null;
            }
            mode_param = null;

            for (int i = 0; i < maps; i++) { // unpack does the range checking
                FuncMapping.mapping_P[map_type[i]].free_info(map_param[i]);
            }
            map_param = null;

            for (int i = 0; i < times; i++) { // unpack does the range checking
                FuncTime.time_P[time_type[i]].free_info(time_param[i]);
            }
            time_param = null;

            for (int i = 0; i < floors; i++) { // unpack does the range checking
                FuncFloor.floor_P[floor_type[i]].free_info(floor_param[i]);
            }
            floor_param = null;

            for (int i = 0; i < residues; i++) { // unpack does the range checking
                FuncResidue.residue_P[residue_type[i]].free_info(residue_param[i]);
            }
            residue_param = null;

            // the static codebooks *are* freed if you call info_clear, because
            // decode side does alloc a 'static' codebook. Calling clear on the
            // full codebook does not clear the static codebook (that's our
            // responsibility)
            for (int i = 0; i < books; i++) {
                // just in case the decoder pre-cleared to save space
                if (book_param[i] != null) {
                    book_param[i].clear();
                    book_param[i] = null;
                }
            }
            //if(vi->book_param)free(vi->book_param);
            book_param = null;

            for (int i = 0; i < psys; i++) {
                psy_param[i].free();
            }

        }

        // Header packing/unpacking
        int unpack_info(VorbisBuffer opb) {
            version = opb.read(32);
            if (version != 0)
                return (-1);

            channels = opb.read(8);
            rate = opb.read(32);

            bitrate_upper = opb.read(32);
            bitrate_nominal = opb.read(32);
            bitrate_lower = opb.read(32);

            blocksizes[0] = 1 << opb.read(4);
            blocksizes[1] = 1 << opb.read(4);

            if ((rate < 1) || (channels < 1) || (blocksizes[0] < 8) || (blocksizes[1] < blocksizes[0])
                    || (opb.read(1) != 1)) {
                clear();
                return (-1);
            }
            return (0);
        }

        // all of the real encoding details are here.  The modes, books,
        // everything
        int unpack_books(VorbisBuffer opb) {

            books = opb.read(8) + 1;

            if (book_param == null || book_param.length != books)
                book_param = new StaticCodeBook[books];
            for (int i = 0; i < books; i++) {
                book_param[i] = new StaticCodeBook();
                if (book_param[i].unpack(opb) != 0) {
                    clear();
                    return (-1);
                }
            }

            // time backend settings
            times = opb.read(6) + 1;
            if (time_type == null || time_type.length != times)
                time_type = new int[times];
            if (time_param == null || time_param.length != times)
                time_param = new Object[times];
            for (int i = 0; i < times; i++) {
                time_type[i] = opb.read(16);
                if (time_type[i] < 0 || time_type[i] >= VI_TIMEB) {
                    clear();
                    return (-1);
                }
                time_param[i] = FuncTime.time_P[time_type[i]].unpack(this, opb);
                if (time_param[i] == null) {
                    clear();
                    return (-1);
                }
            }

            // floor backend settings
            floors = opb.read(6) + 1;
            if (floor_type == null || floor_type.length != floors)
                floor_type = new int[floors];
            if (floor_param == null || floor_param.length != floors)
                floor_param = new Object[floors];

            for (int i = 0; i < floors; i++) {
                floor_type[i] = opb.read(16);
                if (floor_type[i] < 0 || floor_type[i] >= VI_FLOORB) {
                    clear();
                    return (-1);
                }

                floor_param[i] = FuncFloor.floor_P[floor_type[i]].unpack(this, opb);
                if (floor_param[i] == null) {
                    clear();
                    return (-1);
                }
            }

            // residue backend settings
            residues = opb.read(6) + 1;

            if (residue_type == null || residue_type.length != residues)
                residue_type = new int[residues];

            if (residue_param == null || residue_param.length != residues)
                residue_param = new Object[residues];

            for (int i = 0; i < residues; i++) {
                residue_type[i] = opb.read(16);
                if (residue_type[i] < 0 || residue_type[i] >= VI_RESB) {
                    clear();
                    return (-1);
                }
                residue_param[i] = FuncResidue.residue_P[residue_type[i]].unpack(this, opb);
                if (residue_param[i] == null) {
                    clear();
                    return (-1);
                }
            }

            // map backend settings
            maps = opb.read(6) + 1;
            if (map_type == null || map_type.length != maps)
                map_type = new int[maps];
            if (map_param == null || map_param.length != maps)
                map_param = new Object[maps];
            for (int i = 0; i < maps; i++) {
                map_type[i] = opb.read(16);
                if (map_type[i] < 0 || map_type[i] >= VI_MAPB) {
                    clear();
                    return (-1);
                }
                map_param[i] = FuncMapping.mapping_P[map_type[i]].unpack(this, opb);
                if (map_param[i] == null) {
                    clear();
                    return (-1);
                }
            }

            // mode settings
            modes = opb.read(6) + 1;
            if (mode_param == null || mode_param.length != modes)
                mode_param = new InfoMode[modes];
            for (int i = 0; i < modes; i++) {
                mode_param[i] = new InfoMode();
                mode_param[i].blockflag = opb.read(1);
                mode_param[i].windowtype = opb.read(16);
                mode_param[i].transformtype = opb.read(16);
                mode_param[i].mapping = opb.read(8);

                if ((mode_param[i].windowtype >= VI_WINDOWB)
                        || (mode_param[i].transformtype >= VI_WINDOWB)
                        || (mode_param[i].mapping >= maps)) {
                    clear();
                    return (-1);
                }
            }

            if (opb.read(1) != 1) {
                clear();
                return (-1);
            }

            return (0);
        }

        // The Vorbis header is in three packets; the initial small packet in
        // the first page that identifies basic parameters, a second packet
        // with bitstream comments and a third packet that holds the
        // codebook.

        public int synthesis_headerin(Comment vc, Packet op) {
            VorbisBuffer opb = new VorbisBuffer();

            if (op != null) {
                opb.readinit(op.packet_base, op.packet, op.bytes);

                // Which of the three types of header is this?
                // Also verify header-ness, vorbis
                {
                    byte[] buffer = new byte[6];
                    int packtype = opb.read(8);
                    opb.read(buffer, 6);
                    if (buffer[0] != 'v' || buffer[1] != 'o' || buffer[2] != 'r' || buffer[3] != 'b'
                            || buffer[4] != 'i' || buffer[5] != 's') {
                        // not a vorbis header
                        return (-1);
                    }
                    switch (packtype) {
                        case 0x01: // least significant *bit* is read first
                            if (op.b_o_s == 0) {
                                // Not the initial packet
                                return (-1);
                            }
                            if (rate != 0) {
                                // previously initialized info header
                                return (-1);
                            }
                            return (unpack_info(opb));
                        case 0x03: // least significant *bit* is read first
                            if (rate == 0) {
                                // um... we didn't get the initial header
                                return (-1);
                            }
                            return (vc.unpack(opb));
                        case 0x05: // least significant *bit* is read first
                            if (rate == 0 || vc.vendor == null) {
                                // um... we didn;t get the initial header or comments yet
                                return (-1);
                            }
                            return (unpack_books(opb));
                        default:
                            // Not a valid vorbis header type
                            //return(-1);
                            break;
                    }
                }
            }
            return (-1);
        }

        // pack side
        int pack_info(VorbisBuffer opb) {
            // preamble
            opb.write(0x01, 8);
            opb.write(_vorbis);

            // basic information about the stream
            opb.write(0x00, 32);
            opb.write(channels, 8);
            opb.write(rate, 32);

            opb.write(bitrate_upper, 32);
            opb.write(bitrate_nominal, 32);
            opb.write(bitrate_lower, 32);

            opb.write(Util.ilog2(blocksizes[0]), 4);
            opb.write(Util.ilog2(blocksizes[1]), 4);
            opb.write(1, 1);
            return (0);
        }

        int pack_books(VorbisBuffer opb) {
            opb.write(0x05, 8);
            opb.write(_vorbis);

            // books
            opb.write(books - 1, 8);
            for (int i = 0; i < books; i++) {
                if (book_param[i].pack(opb) != 0) {
                    //goto err_out;
                    return (-1);
                }
            }

            // times
            opb.write(times - 1, 6);
            for (int i = 0; i < times; i++) {
                opb.write(time_type[i], 16);
                FuncTime.time_P[time_type[i]].pack(this.time_param[i], opb);
            }

            // floors
            opb.write(floors - 1, 6);
            for (int i = 0; i < floors; i++) {
                opb.write(floor_type[i], 16);
                FuncFloor.floor_P[floor_type[i]].pack(floor_param[i], opb);
            }

            // residues
            opb.write(residues - 1, 6);
            for (int i = 0; i < residues; i++) {
                opb.write(residue_type[i], 16);
                FuncResidue.residue_P[residue_type[i]].pack(residue_param[i], opb);
            }

            // maps
            opb.write(maps - 1, 6);
            for (int i = 0; i < maps; i++) {
                opb.write(map_type[i], 16);
                FuncMapping.mapping_P[map_type[i]].pack(this, map_param[i], opb);
            }

            // modes
            opb.write(modes - 1, 6);
            for (int i = 0; i < modes; i++) {
                opb.write(mode_param[i].blockflag, 1);
                opb.write(mode_param[i].windowtype, 16);
                opb.write(mode_param[i].transformtype, 16);
                opb.write(mode_param[i].mapping, 8);
            }
            opb.write(1, 1);
            return (0);
        }

        public int blocksize(Packet op) {
            //codec_setup_info
            VorbisBuffer opb = new VorbisBuffer();

            int mode;

            opb.readinit(op.packet_base, op.packet, op.bytes);

            /* Check the packet type */
            if (opb.read(1) != 0) {
                /* Oops.  This is not an audio data packet */
                return (OV_ENOTAUDIO);
            }
            {
                int modebits = 0;
                int v = modes;
                while (v > 1) {
                    modebits++;
                    v >>>= 1;
                }

                /* read our mode and pre/post windowsize */
                mode = opb.read(modebits);
            }
            if (mode == -1)
                return (OV_EBADPACKET);
            return (blocksizes[mode_param[mode].blockflag]);
        }

        public String toString() {
            return "version:" + new Integer(version) + ", channels:" + new Integer(channels)
                    + ", rate:" + new Integer(rate) + ", bitrate:" + new Integer(bitrate_upper)
                    + "," + new Integer(bitrate_nominal) + "," + new Integer(bitrate_lower);
        }
    }

    static class DspState {
        static final float M_PI = 3.1415926539f;
        static final int VI_TRANSFORMB = 1;
        static final int VI_WINDOWB = 1;

        int analysisp;
        Info vi;
        int modebits;

        float[][] pcm;
        int pcm_storage;
        int pcm_current;
        int pcm_returned;

        float[] multipliers;
        int envelope_storage;
        int envelope_current;

        int eofflag;

        int lW;
        int W;
        int nW;
        int centerW;

        long granulepos;
        long sequence;

        long glue_bits;
        long time_bits;
        long floor_bits;
        long res_bits;

        // local lookup storage
        float[][][][][] window; // block, leadin, leadout, type
        Object[][] transform;
        CodeBook[] fullbooks;
        // backend lookups are tied to the mode, not the backend or naked mapping
        Object[] mode;

        // local storage, only used on the encoding side.  This way the
        // application does not need to worry about freeing some packets'
        // memory and not others'; packet storage is always tracked.
        // Cleared next call to a _dsp_ function
        byte[] header;
        byte[] header1;
        byte[] header2;

        public DspState() {
            transform = new Object[2][];
            window = new float[2][][][][];
            window[0] = new float[2][][][];
            window[0][0] = new float[2][][];
            window[0][1] = new float[2][][];
            window[0][0][0] = new float[2][];
            window[0][0][1] = new float[2][];
            window[0][1][0] = new float[2][];
            window[0][1][1] = new float[2][];
            window[1] = new float[2][][][];
            window[1][0] = new float[2][][];
            window[1][1] = new float[2][][];
            window[1][0][0] = new float[2][];
            window[1][0][1] = new float[2][];
            window[1][1][0] = new float[2][];
            window[1][1][1] = new float[2][];
        }

        static float[] window(int type, int window, int left, int right) {
            float[] ret = new float[window];
            switch (type) {
                case 0:
                    // The 'vorbis window' (window 0) is sin(sin(x)*sin(x)*2pi)
                {
                    int leftbegin = window / 4 - left / 2;
                    int rightbegin = window - window / 4 - right / 2;

                    for (int i = 0; i < left; i++) {
                        float x = (float) ((i + .5) / left * M_PI / 2.);
                        x = (float) Math.sin(x);
                        x *= x;
                        x *= M_PI / 2.;
                        x = (float) Math.sin(x);
                        ret[i + leftbegin] = x;
                    }

                    for (int i = leftbegin + left; i < rightbegin; i++) {
                        ret[i] = 1.f;
                    }

                    for (int i = 0; i < right; i++) {
                        float x = (float) ((right - i - .5) / right * M_PI / 2.);
                        x = (float) Math.sin(x);
                        x *= x;
                        x *= M_PI / 2.;
                        x = (float) Math.sin(x);
                        ret[i + rightbegin] = x;
                    }
                }
                break;
                default:
                    //free(ret);
                    return (null);
            }
            return (ret);
        }

        // Analysis side code, but directly related to blocking.  Thus it's
        // here and not in analysis.c (which is for analysis transforms only).
        // The init is here because some of it is shared

        int init(Info vi, boolean encp) {
            this.vi = vi;
            modebits = Util.ilog2(vi.modes);

            transform[0] = new Object[VI_TRANSFORMB];
            transform[1] = new Object[VI_TRANSFORMB];

            // MDCT is tranform 0

            transform[0][0] = new Mdct();
            transform[1][0] = new Mdct();
            ((Mdct) transform[0][0]).init(vi.blocksizes[0]);
            ((Mdct) transform[1][0]).init(vi.blocksizes[1]);

            window[0][0][0] = new float[VI_WINDOWB][];
            window[0][0][1] = window[0][0][0];
            window[0][1][0] = window[0][0][0];
            window[0][1][1] = window[0][0][0];
            window[1][0][0] = new float[VI_WINDOWB][];
            window[1][0][1] = new float[VI_WINDOWB][];
            window[1][1][0] = new float[VI_WINDOWB][];
            window[1][1][1] = new float[VI_WINDOWB][];

            for (int i = 0; i < VI_WINDOWB; i++) {
                window[0][0][0][i] = window(i, vi.blocksizes[0], vi.blocksizes[0] / 2,
                        vi.blocksizes[0] / 2);
                window[1][0][0][i] = window(i, vi.blocksizes[1], vi.blocksizes[0] / 2,
                        vi.blocksizes[0] / 2);
                window[1][0][1][i] = window(i, vi.blocksizes[1], vi.blocksizes[0] / 2,
                        vi.blocksizes[1] / 2);
                window[1][1][0][i] = window(i, vi.blocksizes[1], vi.blocksizes[1] / 2,
                        vi.blocksizes[0] / 2);
                window[1][1][1][i] = window(i, vi.blocksizes[1], vi.blocksizes[1] / 2,
                        vi.blocksizes[1] / 2);
            }

            fullbooks = new CodeBook[vi.books];
            for (int i = 0; i < vi.books; i++) {
                fullbooks[i] = new CodeBook();
                fullbooks[i].init_decode(vi.book_param[i]);
            }

            // initialize the storage vectors to a decent size greater than the
            // minimum

            pcm_storage = 8192; // we'll assume later that we have
            // a minimum of twice the blocksize of
            // accumulated samples in analysis
            pcm = new float[vi.channels][];
            {
                for (int i = 0; i < vi.channels; i++) {
                    pcm[i] = new float[pcm_storage];
                }
            }

            // all 1 (large block) or 0 (small block)
            // explicitly set for the sake of clarity
            lW = 0; // previous window size
            W = 0; // current window size

            // all vector indexes; multiples of samples_per_envelope_step
            centerW = vi.blocksizes[1] / 2;

            pcm_current = centerW;

            // initialize all the mapping/backend lookups
            mode = new Object[vi.modes];
            for (int i = 0; i < vi.modes; i++) {
                int mapnum = vi.mode_param[i].mapping;
                int maptype = vi.map_type[mapnum];
                mode[i] = FuncMapping.mapping_P[maptype].look(this, vi.mode_param[i],
                        vi.map_param[mapnum]);
            }
            return (0);
        }

        public int synthesis_init(Info vi) {
            init(vi, false);
            // Adjust centerW to allow an easier mechanism for determining output
            pcm_returned = centerW;
            centerW -= vi.blocksizes[W] / 4 + vi.blocksizes[lW] / 4;
            granulepos = -1;
            sequence = -1;
            return (0);
        }

        DspState(Info vi) {
            this();
            init(vi, false);
            // Adjust centerW to allow an easier mechanism for determining output
            pcm_returned = centerW;
            centerW -= vi.blocksizes[W] / 4 + vi.blocksizes[lW] / 4;
            granulepos = -1;
            sequence = -1;
        }

        // Unike in analysis, the window is only partially applied for each
        // block.  The time domain envelope is not yet handled at the point of
        // calling (as it relies on the previous block).

        public int synthesis_blockin(Block vb) {
            // Shift out any PCM/multipliers that we returned previously
            // centerW is currently the center of the last block added
            if (centerW > vi.blocksizes[1] / 2 && pcm_returned > 8192) {
                // don't shift too much; we need to have a minimum PCM buffer of
                // 1/2 long block

                int shiftPCM = centerW - vi.blocksizes[1] / 2;
                shiftPCM = (pcm_returned < shiftPCM ? pcm_returned : shiftPCM);

                pcm_current -= shiftPCM;
                centerW -= shiftPCM;
                pcm_returned -= shiftPCM;
                if (shiftPCM != 0) {
                    for (int i = 0; i < vi.channels; i++) {
                        System.arraycopy(pcm[i], shiftPCM, pcm[i], 0, pcm_current);
                    }
                }
            }

            lW = W;
            W = vb.W;
            nW = -1;

            glue_bits += vb.glue_bits;
            time_bits += vb.time_bits;
            floor_bits += vb.floor_bits;
            res_bits += vb.res_bits;

            if (sequence + 1 != vb.sequence)
                granulepos = -1; // out of sequence; lose count

            sequence = vb.sequence;

            {
                int sizeW = vi.blocksizes[W];
                int _centerW = centerW + vi.blocksizes[lW] / 4 + sizeW / 4;
                int beginW = _centerW - sizeW / 2;
                int endW = beginW + sizeW;
                int beginSl = 0;
                int endSl = 0;

                // Do we have enough PCM/mult storage for the block?
                if (endW > pcm_storage) {
                    // expand the storage
                    pcm_storage = endW + vi.blocksizes[1];
                    for (int i = 0; i < vi.channels; i++) {
                        float[] foo = new float[pcm_storage];
                        System.arraycopy(pcm[i], 0, foo, 0, pcm[i].length);
                        pcm[i] = foo;
                    }
                }

                // overlap/add PCM
                switch (W) {
                    case 0:
                        beginSl = 0;
                        endSl = vi.blocksizes[0] / 2;
                        break;
                    case 1:
                        beginSl = vi.blocksizes[1] / 4 - vi.blocksizes[lW] / 4;
                        endSl = beginSl + vi.blocksizes[lW] / 2;
                        break;
                }

                for (int j = 0; j < vi.channels; j++) {
                    int _pcm = beginW;
                    // the overlap/add section
                    int i = 0;
                    for (i = beginSl; i < endSl; i++) {
                        pcm[j][_pcm + i] += vb.pcm[j][i];
                    }
                    // the remaining section
                    for (; i < sizeW; i++) {
                        pcm[j][_pcm + i] = vb.pcm[j][i];
                    }
                }

                // track the frame number... This is for convenience, but also
                // making sure our last packet doesn't end with added padding.  If
                // the last packet is partial, the number of samples we'll have to
                // return will be past the vb->granulepos.
                //
                // This is not foolproof!  It will be confused if we begin
                // decoding at the last page after a seek or hole.  In that case,
                // we don't have a starting point to judge where the last frame
                // is.  For this reason, vorbisfile will always try to make sure
                // it reads the last two marked pages in proper sequence

                if (granulepos == -1) {
                    granulepos = vb.granulepos;
                } else {
                    granulepos += (_centerW - centerW);
                    if (vb.granulepos != -1 && granulepos != vb.granulepos) {
                        if (granulepos > vb.granulepos && vb.eofflag != 0) {
                            // partial last frame.  Strip the padding off
                            _centerW -= (granulepos - vb.granulepos);
                        }// else{ Shouldn't happen *unless* the bitstream is out of
                        // spec.  Either way, believe the bitstream }
                        granulepos = vb.granulepos;
                    }
                }

                // Update, cleanup

                centerW = _centerW;
                pcm_current = endW;
                if (vb.eofflag != 0)
                    eofflag = 1;
            }
            return (0);
        }

        // pcm==NULL indicates we just want the pending samples, no more
        public int synthesis_pcmout(float[][][] _pcm, int[] index) {
            if (pcm_returned < centerW) {
                if (_pcm != null) {
                    for (int i = 0; i < vi.channels; i++) {
                        index[i] = pcm_returned;
                    }
                    _pcm[0] = pcm;
                }
                return (centerW - pcm_returned);
            }
            return (0);
        }

        public int synthesis_read(int bytes) {
            if (bytes != 0 && pcm_returned + bytes > centerW)
                return (-1);
            pcm_returned += bytes;
            return (0);
        }

        public void clear() {
        }
    }

    /**
     * Decode an OGG file to PCM data. This class is based on the example
     * code that accompanies the Java OGG libraries (hence the lack of detailed)
     * explanation.
     *
     * @author Kevin Glass
     */
    static class OggVorbisDecoder {

        public byte[] pcmSampleData;

        public int sampleRate;

        public int loopStart;
        public int loopEnd;

        /**
         * The conversion buffer size
         */
        private int convsize = 512 * 2;
        /**
         * The buffer used to read OGG file
         */
        private final byte[] convbuffer = new byte[convsize]; // take 8k out of the data segment, not the stack

        /**
         * Create a new OGG decoder
         *
         * @param input
         */
        public OggVorbisDecoder(InputStream input) {
            try {
                getData(input);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * Get the data out of an OGG file
         *
         * @param input The input stream from which to read the OGG file
         */
        public void getData(InputStream input) throws IOException {
            // the following code come from an example in the Java OGG library.
            // Its extremely complicated and a good example of a library
            // that is potentially to low level for average users. I'd suggest
            // accepting this code as working and not thinking too hard
            // on what its actually doing

            ByteArrayOutputStream dataout = new ByteArrayOutputStream();

            SyncState oy = new SyncState(); // sync and verify incoming physical bitstream
            StreamState os = new StreamState(); // take physical pages, weld into a logical stream of packets
            Page og = new Page(); // one Ogg bitstream page.  Vorbis packets are inside
            Packet op = new Packet(); // one raw packet of data for decode

            Info vi = new Info(); // struct that stores all the static vorbis bitstream settings
            Comment vc = new Comment(); // struct that stores all the bitstream user comments
            DspState vd = new DspState(); // central working state for the packet->PCM decoder
            Block vb = new Block(vd); // local working space for packet->PCM decode

            byte[] buffer;
            int bytes = 0;

            boolean bigEndian = ByteOrder.nativeOrder().equals(ByteOrder.BIG_ENDIAN);
            // Decode setup

            oy.init(); // Now we can read pages

            while (true) { // we repeat if the bitstream is chained
                int eos = 0;

                // grab some data at the head of the stream.  We want the first page
                // (which is guaranteed to be small and only contain the Vorbis
                // stream initial header) We need the first page to get the stream
                // serialno.

                // submit a 4k block to libvorbis' Ogg layer
                int index = oy.buffer(1024);
                buffer = oy.data;
                try {
                    bytes = input.read(buffer, index, 1024);
                } catch (Exception e) {
                    throw new IOException(e.getMessage());
                }
                oy.wrote(bytes);

                // Get the first page.
                if (oy.pageout(og) != 1) {
                    // have we simply run out of data?  If so, we're done.
                    if (bytes < 1024)
                        break;

                    // error case.  Must not be Vorbis data
                    throw new IOException("Input does not appear to be an Ogg bitstream.");
                }

                // Get the serial number and set up the rest of decode.
                // serialno first; use it to set up a logical stream
                os.init(og.serialno());

                // extract the initial header from the first page and verify that the
                // Ogg bitstream is in fact Vorbis data

                // I handle the initial header first instead of just having the code
                // read all three Vorbis headers at once because reading the initial
                // header is an easy way to identify a Vorbis bitstream and it's
                // useful to see that functionality seperated out.

                vi.init();
                vc.init();
                if (os.pagein(og) < 0) {
                    // error; stream version mismatch perhaps
                    throw new IOException("Error reading first page of Ogg bitstream data.");
                }

                if (os.packetout(op) != 1) {
                    // no page? must not be vorbis
                    throw new IOException("Error reading initial header packet.");
                }

                if (vi.synthesis_headerin(vc, op) < 0) {
                    // error case; not a vorbis header
                    throw new IOException("This Ogg bitstream does not contain Vorbis audio data.");
                }

                // At this point, we're sure we're Vorbis.  We've set up the logical
                // (Ogg) bitstream decoder.  Get the comment and codebook headers and
                // set up the Vorbis decoder

                // The next two packets in order are the comment and codebook headers.
                // They're likely large and may span multiple pages.  Thus we reead
                // and submit data until we get our two pacakets, watching that no
                // pages are missing.  If a page is missing, error out; losing a
                // header page is the only place where missing data is fatal. */

                int i = 0;
                while (i < 2) {
                    while (i < 2) {

                        int result = oy.pageout(og);
                        if (result == 0)
                            break; // Need more data
                        // Don't complain about missing or corrupt data yet.  We'll
                        // catch it at the packet output phase

                        if (result == 1) {
                            os.pagein(og); // we can ignore any errors here
                            // as they'll also become apparent
                            // at packetout
                            while (i < 2) {
                                result = os.packetout(op);
                                if (result == 0)
                                    break;
                                if (result == -1) {
                                    // Uh oh; data at some point was corrupted or missing!
                                    // We can't tolerate that in a header.  Die.
                                    throw new IOException("Corrupt secondary header.  Exiting.");
                                }
                                vi.synthesis_headerin(vc, op);
                                i++;
                            }
                        }
                    }
                    // no harm in not checking before adding more
                    index = oy.buffer(1024);
                    buffer = oy.data;
                    try {
                        bytes = input.read(buffer, index, 1024);
                    } catch (Exception e) {
                        throw new IOException(e.getMessage());
                    }
                    if (bytes == 0 && i < 2) {
                        throw new IOException("End of file before finding all Vorbis headers!");
                    }
                    oy.wrote(bytes);
                }

                for (int commentIndex = 0; commentIndex < vc.comments; commentIndex++) {
                    String comment = vc.getComment(commentIndex);
                    if (comment.toLowerCase().contains("loop start=")) {
                        loopStart = Integer.parseInt(comment.toLowerCase().replace("loop start=", "").trim());
                    }
                    if (comment.toLowerCase().contains("loop end=")) {
                        loopEnd = Integer.parseInt(comment.toLowerCase().replace("loop end=", "").trim());
                    }
                    if (comment.toLowerCase().contains("sample size=")) {
                        int sampleSize = Integer.parseInt(comment.toLowerCase().replace("sample size=", "").trim());
                        if (loopEnd != sampleSize) {
                            loopEnd = sampleSize;
                        }
                    }
                    if (comment.toLowerCase().contains("sample rate=")) {
                        sampleRate = Integer.parseInt(comment.toLowerCase().replace("sample rate=", "").trim());
                    }
                }

                convsize = 512 / vi.channels;

                // OK, got and parsed all three headers. Initialize the Vorbis
                //  packet->PCM decoder.
                vd.synthesis_init(vi); // central decode state
                vb.init(vd); // local state for most of the decode
                // so multiple block decodes can
                // proceed in parallel.  We could init
                // multiple vorbis_block structures
                // for vd here

                float[][][] _pcm = new float[1][][];
                int[] _index = new int[vi.channels];
                // The rest is just a straight decode loop until end of stream
                while (eos == 0) {
                    while (eos == 0) {

                        int result = oy.pageout(og);
                        if (result == 0)
                            break; // need more data
                        if (result == -1) { // missing or corrupt data at this page position
                            System.err.println("Corrupt or missing data in bitstream; continuing...");
                        } else {
                            os.pagein(og); // can safely ignore errors at
                            // this point
                            while (true) {
                                result = os.packetout(op);

                                if (result == 0)
                                    break; // need more data
                                if (result == -1) { // missing or corrupt data at this page position
                                    // no reason to complain; already complained above
                                } else {
                                    // we have a packet.  Decode it
                                    int samples;
                                    if (vb.synthesis(op) == 0) { // test for success!
                                        vd.synthesis_blockin(vb);
                                    }

                                    // **pcm is a multichannel float vector.  In stereo, for
                                    // example, pcm[0] is left, and pcm[1] is right.  samples is
                                    // the size of each channel.  Convert the float values
                                    // (-1.<=range<=1.) to whatever PCM format and write it out

                                    while ((samples = vd.synthesis_pcmout(_pcm,
                                            _index)) > 0) {
                                        float[][] pcm = _pcm[0];
                                        int bout = (Math.min(samples, convsize));

                                        // convert floats to 16 bit signed ints (host order) and
                                        // interleave
                                        for (i = 0; i < vi.channels; i++) {
                                            int ptr = i * 2;
                                            //int ptr=i;
                                            int mono = _index[i];
                                            for (int j = 0; j < bout; j++) {
                                                int val = (int) (pcm[i][mono + j] * 32767.);
                                                //                            short val=(short)(pcm[i][mono+j]*32767.);
                                                //                            int val=(int)Math.round(pcm[i][mono+j]*32767.);
                                                // might as well guard against clipping
                                                if (val > 32767) {
                                                    val = 32767;
                                                }
                                                if (val < -32768) {
                                                    val = -32768;
                                                }
                                                if (val < 0)
                                                    val = val | 0x8000;

                                                if (bigEndian) {
                                                    convbuffer[ptr] = (byte) (val >>> 8);
                                                    convbuffer[ptr + 1] = (byte) (val);
                                                } else {
                                                    convbuffer[ptr] = (byte) (val);
                                                    convbuffer[ptr + 1] = (byte) (val >>> 8);
                                                }
                                                ptr += 2 * (vi.channels);
                                            }
                                        }

                                        dataout.write(convbuffer, 0, 2 * vi.channels * bout);

                                        vd.synthesis_read(bout); // tell libvorbis how
                                        // many samples we
                                        // actually consumed
                                    }
                                }
                            }
                            if (og.eos() != 0)
                                eos = 1;
                        }
                    }
                    if (eos == 0) {
                        index = oy.buffer(1024);
                        buffer = oy.data;
                        try {
                            bytes = input.read(buffer, index, 1024);
                        } catch (Exception e) {
                            throw new IOException();
                        }
                        oy.wrote(bytes);
                        if (bytes == 0)
                            eos = 1;
                    }
                }

                // clean up this logical bitstream; before exit we see if we're
                // followed by another [chained]

                os.clear();

                // ogg_page and ogg_packet structs always point to storage in
                // libvorbis.  They're never freed or manipulated directly

                vb.clear();
                vd.clear();
                vi.clear(); // must be called last
            }

            // OK, clean up the framer
            oy.clear();

            pcmSampleData = dataout.toByteArray();
        }
    }

    public static final class IterableNodeHashTable implements Iterable {

        int size;
        Node[] buckets;
        Node currentGet;
        Node current;
        int index;

        public IterableNodeHashTable(int var1) {
            this.index = 0; // L: 11
            this.size = var1; // L: 14
            this.buckets = new Node[var1]; // L: 15

            for (int var2 = 0; var2 < var1; ++var2) { // L: 16
                Node var3 = this.buckets[var2] = new Node(); // L: 17
                var3.previous = var3; // L: 18
                var3.next = var3; // L: 19
            }

        } // L: 21

        public Node get(long var1) {
            Node var3 = this.buckets[(int)(var1 & (long)(this.size - 1))]; // L: 24

            for (this.currentGet = var3.previous; var3 != this.currentGet; this.currentGet = this.currentGet.previous) { // L: 25 26 32
                if (this.currentGet.key == var1) { // L: 27
                    Node var4 = this.currentGet; // L: 28
                    this.currentGet = this.currentGet.previous; // L: 29
                    return var4; // L: 30
                }
            }

            this.currentGet = null; // L: 34
            return null; // L: 35
        }

        public void put(Node var1, long var2) {
            if (var1.next != null) { // L: 39
                var1.remove();
            }

            Node var4 = this.buckets[(int)(var2 & (long)(this.size - 1))]; // L: 40
            var1.next = var4.next; // L: 41
            var1.previous = var4; // L: 42
            var1.next.previous = var1; // L: 43
            var1.previous.next = var1; // L: 44
            var1.key = var2; // L: 45
        } // L: 46

        public void clear() {
            for (int var1 = 0; var1 < this.size; ++var1) { // L: 49
                Node var2 = this.buckets[var1]; // L: 50

                while (true) {
                    Node var3 = var2.previous; // L: 52
                    if (var3 == var2) { // L: 53
                        break;
                    }

                    var3.remove(); // L: 54
                }
            }

            this.currentGet = null; // L: 57
            this.current = null; // L: 58
        } // L: 59

        public Node first() {
            this.index = 0; // L: 62
            return this.next(); // L: 63
        }

        public Node next() {
            Node var1;
            if (this.index > 0 && this.buckets[this.index - 1] != this.current) { // L: 67
                var1 = this.current; // L: 68
                this.current = var1.previous; // L: 69
                return var1; // L: 70
            } else {
                do {
                    if (this.index >= this.size) { // L: 72
                        return null; // L: 79
                    }

                    var1 = this.buckets[this.index++].previous; // L: 73
                } while(var1 == this.buckets[this.index - 1]); // L: 74

                this.current = var1.previous; // L: 75
                return var1; // L: 76
            }
        }

        public Iterator iterator() {
            return new IterableNodeHashTableIterator(this); // L: 83
        }
    }

    static class IterableNodeHashTableIterator implements Iterator {

        IterableNodeHashTable hashTable;
        Node head;
        int index;
        Node last;

        public IterableNodeHashTableIterator(IterableNodeHashTable var1) {
            this.last = null; // L: 10
            this.hashTable = var1; // L: 13
            this.start(); // L: 14
        } // L: 15

        void start() {
            this.head = this.hashTable.buckets[0].previous; // L: 18
            this.index = 1; // L: 19
            this.last = null; // L: 20
        } // L: 21

        public Object next() {
            Node var1;
            if (this.hashTable.buckets[this.index - 1] != this.head) { // L: 24
                var1 = this.head; // L: 25
                this.head = var1.previous; // L: 26
                this.last = var1; // L: 27
                return var1; // L: 28
            } else {
                do {
                    if (this.index >= this.hashTable.size) { // L: 30
                        return null; // L: 38
                    }

                    var1 = this.hashTable.buckets[this.index++].previous; // L: 31
                } while(var1 == this.hashTable.buckets[this.index - 1]); // L: 32

                this.head = var1.previous; // L: 33
                this.last = var1; // L: 34
                return var1; // L: 35
            }
        }

        public boolean hasNext() {
            if (this.hashTable.buckets[this.index - 1] != this.head) { // L: 42
                return true;
            } else {
                while (this.index < this.hashTable.size) { // L: 43
                    if (this.hashTable.buckets[this.index++].previous != this.hashTable.buckets[this.index - 1]) { // L: 44
                        this.head = this.hashTable.buckets[this.index - 1].previous; // L: 45
                        return true; // L: 46
                    }

                    this.head = this.hashTable.buckets[this.index - 1]; // L: 49
                }

                return false; // L: 52
            }
        }

        public void remove() {
            if (this.last == null) { // L: 56
                throw new IllegalStateException();
            } else {
                this.last.remove(); // L: 57
                this.last = null; // L: 58
            }
        } // L: 59
    }


    static class Buffer extends Node {

        static int[] crc32Table;
        static long[] crc64Table;
        public byte[] array;
        public int offset;

        static {
            crc32Table = new int[256]; // L: 16

            int var2;
            for (int var1 = 0; var1 < 256; ++var1) { // L: 21
                int var4 = var1; // L: 22

                for (var2 = 0; var2 < 8; ++var2) { // L: 23
                    if ((var4 & 1) == 1) { // L: 24
                        var4 = var4 >>> 1 ^ -306674912;
                    } else {
                        var4 >>>= 1; // L: 25
                    }
                }

                crc32Table[var1] = var4; // L: 27
            }

            crc64Table = new long[256]; // L: 31

            for (var2 = 0; var2 < 256; ++var2) { // L: 36
                long var0 = (long) var2; // L: 37

                for (int var3 = 0; var3 < 8; ++var3) { // L: 38
                    if (1L == (var0 & 1L)) { // L: 39
                        var0 = var0 >>> 1 ^ -3932672073523589310L;
                    } else {
                        var0 >>>= 1; // L: 40
                    }
                }

                crc64Table[var2] = var0; // L: 42
            }

        } // L: 44

        public Buffer(int var1) {
            this.array = ByteArrayPool_getArray(var1); // L: 60
            this.offset = 0; // L: 61
        } // L: 62

        public Buffer() {

        }

        public static synchronized byte[] ByteArrayPool_getArray(int var0) {
            return ByteArrayPool.ByteArrayPool_getArrayBool(var0, false); // L: 112
        }

        public Buffer(byte[] var1) {
            this.array = var1; // L: 65
            this.offset = 0; // L: 66
        } // L: 67

        public void releaseArray() {
            if (this.array != null) { // L: 70
                ByteArrayPool_release(this.array);
            }

            this.array = null; // L: 71
        } // L: 72


        public static synchronized void ByteArrayPool_release(byte[] var0) {
            if (var0.length == 100 && ByteArrayPool.ByteArrayPool_smallCount < ByteArrayPool.field4210) { // L: 116
                ByteArrayPool.ByteArrayPool_small[++ByteArrayPool.ByteArrayPool_smallCount - 1] = var0; // L: 117
            } else if (var0.length == 5000 && ByteArrayPool.ByteArrayPool_mediumCount < ByteArrayPool.field4219) { // L: 120
                ByteArrayPool.ByteArrayPool_medium[++ByteArrayPool.ByteArrayPool_mediumCount - 1] = var0; // L: 121
            } else if (var0.length == 10000 && ByteArrayPool.ByteArrayPool_largeCount < ByteArrayPool.field4220) { // L: 124
                ByteArrayPool.ByteArrayPool_large[++ByteArrayPool.ByteArrayPool_largeCount - 1] = var0; // L: 125
            } else if (var0.length == 30000 && ByteArrayPool.field4217 < ByteArrayPool.field4221) { // L: 128
                ByteArrayPool.field4225[++ByteArrayPool.field4217 - 1] = var0; // L: 129
            }
        } // L: 118 122 126 130 140

        public void writeByte(int var1) {
            this.array[++this.offset - 1] = (byte) var1; // L: 75
        } // L: 76

        public void writeShort(int var1) {
            this.array[++this.offset - 1] = (byte) (var1 >> 8); // L: 79
            this.array[++this.offset - 1] = (byte) var1; // L: 80
        } // L: 81

        public void writeMedium(int var1) {
            this.array[++this.offset - 1] = (byte) (var1 >> 16); // L: 84
            this.array[++this.offset - 1] = (byte) (var1 >> 8); // L: 85
            this.array[++this.offset - 1] = (byte) var1; // L: 86
        } // L: 87

        public void writeInt(int var1) {
            this.array[++this.offset - 1] = (byte) (var1 >> 24); // L: 90
            this.array[++this.offset - 1] = (byte) (var1 >> 16); // L: 91
            this.array[++this.offset - 1] = (byte) (var1 >> 8); // L: 92
            this.array[++this.offset - 1] = (byte) var1; // L: 93
        } // L: 94

        public void writeLongMedium(long var1) {
            this.array[++this.offset - 1] = (byte) ((int) (var1 >> 40)); // L: 97
            this.array[++this.offset - 1] = (byte) ((int) (var1 >> 32)); // L: 98
            this.array[++this.offset - 1] = (byte) ((int) (var1 >> 24)); // L: 99
            this.array[++this.offset - 1] = (byte) ((int) (var1 >> 16)); // L: 100
            this.array[++this.offset - 1] = (byte) ((int) (var1 >> 8)); // L: 101
            this.array[++this.offset - 1] = (byte) ((int) var1); // L: 102
        } // L: 103

        public void writeLong(long var1) {
            this.array[++this.offset - 1] = (byte) ((int) (var1 >> 56)); // L: 106
            this.array[++this.offset - 1] = (byte) ((int) (var1 >> 48)); // L: 107
            this.array[++this.offset - 1] = (byte) ((int) (var1 >> 40)); // L: 108
            this.array[++this.offset - 1] = (byte) ((int) (var1 >> 32)); // L: 109
            this.array[++this.offset - 1] = (byte) ((int) (var1 >> 24)); // L: 110
            this.array[++this.offset - 1] = (byte) ((int) (var1 >> 16)); // L: 111
            this.array[++this.offset - 1] = (byte) ((int) (var1 >> 8)); // L: 112
            this.array[++this.offset - 1] = (byte) ((int) var1); // L: 113
        } // L: 114

        public void writeBoolean(boolean var1) {
            this.writeByte(var1 ? 1 : 0); // L: 117
        } // L: 118

        public void writeCESU8(CharSequence var1) {
            int var3 = var1.length(); // L: 142
            int var4 = 0; // L: 143

            int var5;
            for (var5 = 0; var5 < var3; ++var5) { // L: 144
                char var12 = var1.charAt(var5); // L: 145
                if (var12 <= 127) { // L: 146
                    ++var4;
                } else if (var12 <= 2047) { // L: 147
                    var4 += 2;
                } else {
                    var4 += 3; // L: 148
                }
            }

            this.array[++this.offset - 1] = 0; // L: 153
            this.writeVarInt(var4); // L: 154
            var4 = this.offset * -2117273951; // L: 155
            byte[] var6 = this.array; // L: 157
            int var7 = this.offset; // L: 158
            int var8 = var1.length(); // L: 160
            int var9 = var7; // L: 161

            for (int var10 = 0; var10 < var8; ++var10) { // L: 162
                char var11 = var1.charAt(var10); // L: 163
                if (var11 <= 127) { // L: 164
                    var6[var9++] = (byte) var11; // L: 165
                } else if (var11 <= 2047) { // L: 167
                    var6[var9++] = (byte) (192 | var11 >> 6); // L: 168
                    var6[var9++] = (byte) (128 | var11 & '?'); // L: 169
                } else {
                    var6[var9++] = (byte) (224 | var11 >> '\f'); // L: 172
                    var6[var9++] = (byte) (128 | var11 >> 6 & 63); // L: 173
                    var6[var9++] = (byte) (128 | var11 & '?'); // L: 174
                }
            }

            var5 = var9 - var7; // L: 177
            this.offset = (var5 * -2117273951 + var4) * -271291039; // L: 179
        } // L: 180

        public void writeBytes(byte[] var1, int var2, int var3) {
            for (int var4 = var2; var4 < var3 + var2; ++var4) { // L: 183
                this.array[++this.offset - 1] = var1[var4];
            }

        } // L: 184

        public void method7530(Buffer var1) {
            this.writeBytes(var1.array, 0, var1.offset); // L: 187
        } // L: 188

        public void writeLengthInt(int var1) {
            if (var1 < 0) { // L: 191
                throw new IllegalArgumentException(); // L: 192
            } else {
                this.array[this.offset - var1 - 4] = (byte) (var1 >> 24); // L: 194
                this.array[this.offset - var1 - 3] = (byte) (var1 >> 16); // L: 195
                this.array[this.offset - var1 - 2] = (byte) (var1 >> 8); // L: 196
                this.array[this.offset - var1 - 1] = (byte) var1; // L: 197
            }
        } // L: 198

        public void writeLengthShort(int var1) {
            if (var1 >= 0 && var1 <= 65535) { // L: 201
                this.array[this.offset - var1 - 2] = (byte) (var1 >> 8); // L: 204
                this.array[this.offset - var1 - 1] = (byte) var1; // L: 205
            } else {
                throw new IllegalArgumentException(); // L: 202
            }
        } // L: 206

        public void method7740(int var1) {
            if (var1 >= 0 && var1 <= 255) { // L: 209
                this.array[this.offset - var1 - 1] = (byte) var1; // L: 212
            } else {
                throw new IllegalArgumentException(); // L: 210
            }
        } // L: 213

        public void writeSmartByteShort(int var1) {
            if (var1 >= 0 && var1 < 128) { // L: 216
                this.writeByte(var1); // L: 217
            } else if (var1 >= 0 && var1 < 32768) { // L: 220
                this.writeShort(var1 + 32768); // L: 221
            } else {
                throw new IllegalArgumentException(); // L: 224
            }
        } // L: 218 222

        public void writeVarInt(int var1) {
            if ((var1 & -128) != 0) { // L: 228
                if ((var1 & -16384) != 0) { // L: 229
                    if ((var1 & -2097152) != 0) { // L: 230
                        if ((var1 & -268435456) != 0) { // L: 231
                            this.writeByte(var1 >>> 28 | 128);
                        }

                        this.writeByte(var1 >>> 21 | 128); // L: 232
                    }

                    this.writeByte(var1 >>> 14 | 128); // L: 234
                }

                this.writeByte(var1 >>> 7 | 128); // L: 236
            }

            this.writeByte(var1 & 127); // L: 238
        } // L: 239

        public int readUnsignedByte() {
            return this.array[++this.offset - 1] & 255; // L: 242
        }

        public byte readByte() {
            return this.array[++this.offset - 1]; // L: 246
        }

        public int readUnsignedShort() {
            this.offset += 2; // L: 250
            return (this.array[this.offset - 1] & 255) + ((this.array[this.offset - 2] & 255) << 8); // L: 251
        }

        public int readShort() {
            this.offset += 2; // L: 255
            int var1 = (this.array[this.offset - 1] & 255) + ((this.array[this.offset - 2] & 255) << 8); // L: 256
            if (var1 > 32767) { // L: 257
                var1 -= 65536;
            }

            return var1; // L: 258
        }

        public int readMedium() {
            this.offset += 3; // L: 262
            return ((this.array[this.offset - 3] & 255) << 16) + (this.array[this.offset - 1] & 255) + ((this.array[this.offset - 2] & 255) << 8); // L: 263
        }

        public int readInt() {
            this.offset += 4; // L: 267
            return ((this.array[this.offset - 3] & 255) << 16) + (this.array[this.offset - 1] & 255) + ((this.array[this.offset - 2] & 255) << 8) + ((this.array[this.offset - 4] & 255) << 24); // L: 268
        }

        public long readLong() {
            long var1 = (long) this.readInt() & 4294967295L; // L: 272
            long var3 = (long) this.readInt() & 4294967295L; // L: 273
            return (var1 << 32) + var3; // L: 274
        }

        public float method7570() {
            return Float.intBitsToFloat(this.readInt()); // L: 278
        }

        public boolean readBoolean() {
            return (this.readUnsignedByte() & 1) == 1; // L: 282
        }

        public String readCESU8() {
            byte var1 = this.array[++this.offset - 1]; // L: 312
            if (var1 != 0) { // L: 313
                throw new IllegalStateException("");
            } else {
                int var2 = this.readVarInt(); // L: 314
                if (var2 + this.offset > this.array.length) { // L: 315
                    throw new IllegalStateException("");
                } else {
                    byte[] var4 = this.array; // L: 317
                    int var5 = this.offset; // L: 318
                    char[] var6 = new char[var2]; // L: 320
                    int var7 = 0; // L: 321
                    int var8 = var5; // L: 322

                    int var11;
                    for (int var9 = var5 + var2; var8 < var9; var6[var7++] = (char) var11) { // L: 323 324 355
                        int var10 = var4[var8++] & 255; // L: 325
                        if (var10 < 128) { // L: 327
                            if (var10 == 0) { // L: 328
                                var11 = 65533;
                            } else {
                                var11 = var10; // L: 329
                            }
                        } else if (var10 < 192) { // L: 331
                            var11 = 65533;
                        } else if (var10 < 224) { // L: 332
                            if (var8 < var9 && (var4[var8] & 192) == 128) { // L: 333
                                var11 = (var10 & 31) << 6 | var4[var8++] & 63; // L: 334
                                if (var11 < 128) { // L: 335
                                    var11 = 65533;
                                }
                            } else {
                                var11 = 65533; // L: 337
                            }
                        } else if (var10 < 240) { // L: 339
                            if (var8 + 1 < var9 && (var4[var8] & 192) == 128 && (var4[var8 + 1] & 192) == 128) { // L: 340
                                var11 = (var10 & 15) << 12 | (var4[var8++] & 63) << 6 | var4[var8++] & 63; // L: 341
                                if (var11 < 2048) { // L: 342
                                    var11 = 65533;
                                }
                            } else {
                                var11 = 65533; // L: 344
                            }
                        } else if (var10 < 248) { // L: 346
                            if (var8 + 2 < var9 && (var4[var8] & 192) == 128 && (var4[var8 + 1] & 192) == 128 && (var4[var8 + 2] & 192) == 128) { // L: 347
                                var11 = (var10 & 7) << 18 | (var4[var8++] & 63) << 12 | (var4[var8++] & 63) << 6 | var4[var8++] & 63; // L: 348
                                if (var11 >= 65536 && var11 <= 1114111) { // L: 349
                                    var11 = 65533; // L: 350
                                } else {
                                    var11 = 65533;
                                }
                            } else {
                                var11 = 65533; // L: 352
                            }
                        } else {
                            var11 = 65533; // L: 354
                        }
                    }

                    String var3 = new String(var6, 0, var7); // L: 357
                    this.offset += var2; // L: 360
                    return var3; // L: 361
                }
            }
        }

        public void readBytes(byte[] var1, int var2, int var3) {
            for (int var4 = var2; var4 < var3 + var2; ++var4) {
                var1[var4] = this.array[++this.offset - 1]; // L: 365
            }

        } // L: 366

        public int readShortSmart() {
            int var1 = this.array[this.offset] & 255; // L: 369
            return var1 < 128 ? this.readUnsignedByte() - 64 : this.readUnsignedShort() - 49152; // L: 370 371
        }

        public int readUShortSmart() {
            int var1 = this.array[this.offset] & 255; // L: 375
            return var1 < 128 ? this.readUnsignedByte() : this.readUnsignedShort() - 32768; // L: 376 377
        }

        public int method7531() {
            int var1 = 0; // L: 381

            int var2;
            for (var2 = this.readUShortSmart(); var2 == 32767; var2 = this.readUShortSmart()) { // L: 382 383 385
                var1 += 32767; // L: 384
            }

            var1 += var2; // L: 387
            return var1; // L: 388
        }

        public int method7627() {
            return this.array[this.offset] < 0 ? this.readInt() & Integer.MAX_VALUE : this.readUnsignedShort(); // L: 392 393
        }

        public int method7532() {
            if (this.array[this.offset] < 0) { // L: 397
                return this.readInt() & Integer.MAX_VALUE;
            } else {
                int var1 = this.readUnsignedShort(); // L: 398
                return var1 == 32767 ? -1 : var1; // L: 399
            }
        }

        public int readVarInt() {
            byte var1 = this.array[++this.offset - 1]; // L: 404

            int var2;
            for (var2 = 0; var1 < 0; var1 = this.array[++this.offset - 1]) { // L: 405 406 408
                var2 = (var2 | var1 & 127) << 7; // L: 407
            }

            return var2 | var1; // L: 410
        }

        public void xteaEncryptAll(int[] var1) {
            int var2 = this.offset / 8; // L: 414
            this.offset = 0; // L: 415

            for (int var3 = 0; var3 < var2; ++var3) { // L: 416
                int var4 = this.readInt(); // L: 417
                int var5 = this.readInt(); // L: 418
                int var6 = 0; // L: 419
                int var7 = -1640531527; // L: 420

                for (int var8 = 32; var8-- > 0; var5 += var4 + (var4 << 4 ^ var4 >>> 5) ^ var1[var6 >>> 11 & 3] + var6) { // L: 421 422 425
                    var4 += var5 + (var5 << 4 ^ var5 >>> 5) ^ var6 + var1[var6 & 3]; // L: 423
                    var6 += var7; // L: 424
                }

                this.offset -= 8; // L: 427
                this.writeInt(var4); // L: 428
                this.writeInt(var5); // L: 429
            }

        } // L: 431

        public void xteaDecryptAll(int[] var1) {
            int var2 = this.offset / 8; // L: 434
            this.offset = 0; // L: 435

            for (int var3 = 0; var3 < var2; ++var3) { // L: 436
                int var4 = this.readInt(); // L: 437
                int var5 = this.readInt(); // L: 438
                int var6 = -957401312; // L: 439
                int var7 = -1640531527; // L: 440

                for (int var8 = 32; var8-- > 0; var4 -= var5 + (var5 << 4 ^ var5 >>> 5) ^ var6 + var1[var6 & 3]) { // L: 441 442 445
                    var5 -= var4 + (var4 << 4 ^ var4 >>> 5) ^ var1[var6 >>> 11 & 3] + var6; // L: 443
                    var6 -= var7; // L: 444
                }

                this.offset -= 8; // L: 447
                this.writeInt(var4); // L: 448
                this.writeInt(var5); // L: 449
            }

        } // L: 451

        public void xteaEncrypt(int[] var1, int var2, int var3) {
            int var4 = this.offset; // L: 454
            this.offset = var2; // L: 455
            int var5 = (var3 - var2) / 8; // L: 456

            for (int var6 = 0; var6 < var5; ++var6) { // L: 457
                int var7 = this.readInt(); // L: 458
                int var8 = this.readInt(); // L: 459
                int var9 = 0; // L: 460
                int var10 = -1640531527; // L: 461

                for (int var11 = 32; var11-- > 0; var8 += var7 + (var7 << 4 ^ var7 >>> 5) ^ var1[var9 >>> 11 & 3] + var9) { // L: 462 463 466
                    var7 += var8 + (var8 << 4 ^ var8 >>> 5) ^ var9 + var1[var9 & 3]; // L: 464
                    var9 += var10; // L: 465
                }

                this.offset -= 8; // L: 468
                this.writeInt(var7); // L: 469
                this.writeInt(var8); // L: 470
            }

            this.offset = var4; // L: 472
        } // L: 473

        public void xteaDecrypt(int[] var1, int var2, int var3) {
            int var4 = this.offset; // L: 476
            this.offset = var2; // L: 477
            int var5 = (var3 - var2) / 8; // L: 478

            for (int var6 = 0; var6 < var5; ++var6) { // L: 479
                int var7 = this.readInt(); // L: 480
                int var8 = this.readInt(); // L: 481
                int var9 = -957401312; // L: 482
                int var10 = -1640531527; // L: 483

                for (int var11 = 32; var11-- > 0; var7 -= var8 + (var8 << 4 ^ var8 >>> 5) ^ var9 + var1[var9 & 3]) { // L: 484 485 488
                    var8 -= var7 + (var7 << 4 ^ var7 >>> 5) ^ var1[var9 >>> 11 & 3] + var9; // L: 486
                    var9 -= var10; // L: 487
                }

                this.offset -= 8; // L: 490
                this.writeInt(var7); // L: 491
                this.writeInt(var8); // L: 492
            }

            this.offset = var4; // L: 494
        } // L: 495

        public void encryptRsa(BigInteger var1, BigInteger var2) {
            int var3 = this.offset; // L: 498
            this.offset = 0; // L: 499
            byte[] var4 = new byte[var3]; // L: 500
            this.readBytes(var4, 0, var3); // L: 501
            BigInteger var5 = new BigInteger(var4); // L: 502
            BigInteger var6 = var5.modPow(var1, var2); // L: 503
            byte[] var7 = var6.toByteArray(); // L: 504
            this.offset = 0; // L: 505
            this.writeShort(var7.length); // L: 506
            this.writeBytes(var7, 0, var7.length); // L: 507
        } // L: 508

        public void method7687(int var1) {
            this.array[++this.offset - 1] = (byte) (var1 + 128); // L: 525
        } // L: 526

        public void method7542(int var1) {
            this.array[++this.offset - 1] = (byte) (0 - var1); // L: 529
        } // L: 530

        public void method7596(int var1) {
            this.array[++this.offset - 1] = (byte) (128 - var1); // L: 533
        } // L: 534

        public int method7593() {
            return this.array[++this.offset - 1] - 128 & 255; // L: 537
        }

        public int method7545() {
            return 0 - this.array[++this.offset - 1] & 255; // L: 541
        }

        public int method7546() {
            return 128 - this.array[++this.offset - 1] & 255; // L: 545
        }

        public byte method7547() {
            return (byte) (this.array[++this.offset - 1] - 128); // L: 549
        }

        public byte method7548() {
            return (byte) (0 - this.array[++this.offset - 1]); // L: 553
        }

        public byte method7549() {
            return (byte) (128 - this.array[++this.offset - 1]); // L: 557
        }

        public void method7550(int var1) {
            this.array[++this.offset - 1] = (byte) var1; // L: 561
            this.array[++this.offset - 1] = (byte) (var1 >> 8); // L: 562
        } // L: 563

        public void method7551(int var1) {
            this.array[++this.offset - 1] = (byte) (var1 >> 8); // L: 566
            this.array[++this.offset - 1] = (byte) (var1 + 128); // L: 567
        } // L: 568

        public void method7641(int var1) {
            this.array[++this.offset - 1] = (byte) (var1 + 128); // L: 571
            this.array[++this.offset - 1] = (byte) (var1 >> 8); // L: 572
        } // L: 573

        public int method7716() {
            this.offset += 2; // L: 576
            return ((this.array[this.offset - 1] & 255) << 8) + (this.array[this.offset - 2] & 255); // L: 577
        }

        public int method7554() {
            this.offset += 2; // L: 581
            return (this.array[this.offset - 1] - 128 & 255) + ((this.array[this.offset - 2] & 255) << 8); // L: 582
        }

        public int method7576() {
            this.offset += 2; // L: 586
            return ((this.array[this.offset - 1] & 255) << 8) + (this.array[this.offset - 2] - 128 & 255); // L: 587
        }

        public int method7556() {
            this.offset += 2; // L: 591
            int var1 = ((this.array[this.offset - 1] & 255) << 8) + (this.array[this.offset - 2] & 255); // L: 592
            if (var1 > 32767) { // L: 593
                var1 -= 65536;
            }

            return var1; // L: 594
        }

        public int method7522() {
            this.offset += 2; // L: 598
            int var1 = (this.array[this.offset - 1] - 128 & 255) + ((this.array[this.offset - 2] & 255) << 8); // L: 599
            if (var1 > 32767) { // L: 600
                var1 -= 65536;
            }

            return var1; // L: 601
        }

        public int method7558() {
            this.offset += 2; // L: 605
            int var1 = ((this.array[this.offset - 1] & 255) << 8) + (this.array[this.offset - 2] - 128 & 255); // L: 606
            if (var1 > 32767) { // L: 607
                var1 -= 65536;
            }

            return var1; // L: 608
        }

        public void method7559(int var1) {
            this.array[++this.offset - 1] = (byte) var1; // L: 612
            this.array[++this.offset - 1] = (byte) (var1 >> 8); // L: 613
            this.array[++this.offset - 1] = (byte) (var1 >> 16); // L: 614
        } // L: 615

        public int method7544() {
            this.offset += 3; // L: 618
            return (this.array[this.offset - 3] & 255) + ((this.array[this.offset - 2] & 255) << 8) + ((this.array[this.offset - 1] & 255) << 16); // L: 619
        }

        public int method7561() {
            this.offset += 3; // L: 623
            return ((this.array[this.offset - 1] & 255) << 8) + ((this.array[this.offset - 3] & 255) << 16) + (this.array[this.offset - 2] & 255); // L: 624
        }

        public int method7503() {
            this.offset += 3; // L: 628
            return (this.array[this.offset - 1] & 255) + ((this.array[this.offset - 3] & 255) << 8) + ((this.array[this.offset - 2] & 255) << 16); // L: 629
        }

        public void method7563(int var1) {
            this.array[++this.offset - 1] = (byte) var1; // L: 633
            this.array[++this.offset - 1] = (byte) (var1 >> 8); // L: 634
            this.array[++this.offset - 1] = (byte) (var1 >> 16); // L: 635
            this.array[++this.offset - 1] = (byte) (var1 >> 24); // L: 636
        } // L: 637

        public void writeIntME(int var1) {
            this.array[++this.offset - 1] = (byte) (var1 >> 8); // L: 640
            this.array[++this.offset - 1] = (byte) var1; // L: 641
            this.array[++this.offset - 1] = (byte) (var1 >> 24); // L: 642
            this.array[++this.offset - 1] = (byte) (var1 >> 16); // L: 643
        } // L: 644

        public void method7565(int var1) {
            this.array[++this.offset - 1] = (byte) (var1 >> 16); // L: 647
            this.array[++this.offset - 1] = (byte) (var1 >> 24); // L: 648
            this.array[++this.offset - 1] = (byte) var1; // L: 649
            this.array[++this.offset - 1] = (byte) (var1 >> 8); // L: 650
        } // L: 651

        public int method7701() {
            this.offset += 4; // L: 654
            return (this.array[this.offset - 4] & 255) + ((this.array[this.offset - 3] & 255) << 8) + ((this.array[this.offset - 2] & 255) << 16) + ((this.array[this.offset - 1] & 255) << 24); // L: 655
        }

        public int method7567() {
            this.offset += 4; // L: 659
            return ((this.array[this.offset - 2] & 255) << 24) + ((this.array[this.offset - 4] & 255) << 8) + (this.array[this.offset - 3] & 255) + ((this.array[this.offset - 1] & 255) << 16); // L: 660
        }

        public int method7568() {
            this.offset += 4; // L: 664
            return ((this.array[this.offset - 1] & 255) << 8) + ((this.array[this.offset - 4] & 255) << 16) + (this.array[this.offset - 2] & 255) + ((this.array[this.offset - 3] & 255) << 24); // L: 665
        }

        public void method7569(byte[] var1, int var2, int var3) {
            for (int var4 = var3 + var2 - 1; var4 >= var2; --var4) {
                var1[var4] = this.array[++this.offset - 1]; // L: 669
            }

        } // L: 670

        public int readUnsignedSmart() {
            int i_2 = array[offset] & 0xff;
            return i_2 < 128 ? readUnsignedByte() - 64 : readUnsignedShort() - 49152;
        }

        public int read24BitUnsignedInteger() {
            offset += 3;
            return ((array[offset - 3] & 0xff) << 16) + (array[offset - 1] & 0xff) + ((array[offset - 2] & 0xff) << 8);
        }
    }

    /**
     * A class which handles synthesizer methods for the MIDI sequence.
     */
    static class MidiStream {

        /**
         * A table containing the loaded Sound Bank patches with their respective integer ID.
         */
        Hashtable<Integer, MusicPatch> musicPatches;

        /**
         * An integer value representing the overall volume for the output audio.
         */
        int volume;

        /**
         * An integer value representing the default tempo division amount.
         */
        int division;

        /**
         * An array of integers for MIDI Volume values.
         */
        int[] volumeControls;

        /**
         * An array of integers for MIDI Pan values.
         */
        int[] panControls;

        /**
         * An array of integers for MIDI Expression values.
         */
        int[] expressionControls;

        /**
         * An array of integers for MIDI Program Change values.
         */
        int[] programConstants;

        /**
         * An array of integers for MIDI Program Change values.
         */
        int[] patch;

        /**
         * An array of integers for MIDI Bank Select values.
         */
        int[] bankControls;

        /**
         * An array of integers for MIDI Pitch Bend values.
         */
        int[] pitchBendControls;

        /**
         * An array of integers for MIDI Modulation values.
         */
        int[] modulationControls;

        /**
         * An array of integers for MIDI Portamento time values.
         */
        int[] portamentoTimeControls;

        /**
         * An array of integers for MIDI switch on/off values to the exclusive RuneScape effects.
         */
        int[] switchControls;

        /**
         * An array of integers for MIDI Data Entry MSB values.
         */
        int[] dataEntriesMSB;

        /**
         * An array of integers for MIDI Data Entry LSB values.
         */
        int[] dataEntriesLSB;

        /**
         * An array of integers for MIDI Sample Loop Modification values (Exclusive to RuneScape).
         */
        int[] sampleLoopControls;

        /**
         * An array of integers for the MIDI ReTrigger Control values (Exclusive to RuneScape).
         */
        int[] reTriggerControls;

        /**
         * An array of integers for the MIDI ReTrigger Effect values (Exclusive to RuneScape).
         */
        int[] reTriggerEffects;

        /**
         * An array of Sound Bank patch voices that do not loop.
         */
        MusicPatchVoice[][] oneShotVoices;

        /**
         * An array of Sound Bank patch voices that do loop.
         */
        MusicPatchVoice[][] continuousVoices;

        /**
         * A long value representing the length of audio expressed in microseconds.
         */
        public long microsecondLength;

        /**
         * A long value representing the current position in the audio, expressed in microseconds.
         */
        public long microsecondPosition;

        /**
         * Another stream that aids in synthesizing the music, mixing all the active voices.
         */
        MusicPatchAudioStream patchStream;

        public boolean isLooping = true;

        public int track = 0;

        public int trackLength;

        public MidiFileReader midiFileReader;

        public MusicTrack midiTrack;

        public int sampleRate = 44100;

        /**
         * Constructs a new MidiAudioStream with default values, loading all music patches as well.
         */
        public MidiStream() {
            this.volume = 256;
            this.division = 1000000;
            this.volumeControls = new int[16];
            this.panControls = new int[16];
            this.expressionControls = new int[16];
            this.programConstants = new int[16];
            this.patch = new int[16];
            this.bankControls = new int[16];
            this.pitchBendControls = new int[16];
            this.modulationControls = new int[16];
            this.portamentoTimeControls = new int[16];
            this.switchControls = new int[16];
            this.dataEntriesMSB = new int[16];
            this.dataEntriesLSB = new int[16];
            this.sampleLoopControls = new int[16];
            this.reTriggerControls = new int[16];
            this.reTriggerEffects = new int[16];
            this.oneShotVoices = new MusicPatchVoice[16][128];
            this.continuousVoices = new MusicPatchVoice[16][128];
            this.patchStream = new MusicPatchAudioStream(this);
            this.musicPatches = new Hashtable<>();
            this.midiFileReader = new MidiFileReader();
            this.systemReset();
        }

        /**
         * A method to set the default volume level.
         *
         * @param volumeLevel An integer to represent the volume level.
         */
        public synchronized void setPcmStreamVolume(int volumeLevel) {
            this.volume = volumeLevel;
        }

        public synchronized int getVolume() {
            return this.volume;
        }

        /**
         * A method that loads one of the music patches.
         *
         * @param key The music patch ID.
         */
        public synchronized void loadMusicPatch(int key) throws FileNotFoundException {
            MusicPatch musicPatch = this.musicPatches.get(key);
            if (musicPatch == null) {
                InputStream musicPatchFile = new FileInputStream("./OSRS/patches/" + key + ".txt/");
                musicPatch = new MusicPatch(musicPatchFile);
                this.musicPatches.put(key, musicPatch);
            }

        }

        public synchronized void setMusicTrack(MusicTrack musicTrack, boolean musicLoop) {
            this.midiFileReader.clear();
            this.midiFileReader.parse(musicTrack.midi);
            this.isLooping = musicLoop;
            this.microsecondLength = 0L;
            int var3 = this.midiFileReader.trackCount();

            for (int var4 = 0; var4 < var3; ++var4) {
                this.midiFileReader.gotoTrack(var4);
                this.midiFileReader.readTrackLength(var4);
                this.midiFileReader.markTrackPosition(var4);
            }

            this.track = this.midiFileReader.getPrioritizedTrack();
            this.trackLength = this.midiFileReader.trackLengths[this.track];
            this.microsecondPosition = this.midiFileReader.method4934(this.trackLength);
        }

        protected synchronized void fill(int[] var1, int var2, int var3) throws FileNotFoundException {
            if (this.midiFileReader.isReady()) {
                int beatsPerMinute = this.midiFileReader.resolution * this.division / sampleRate;

                do {
                    long var5 = this.microsecondLength + (long) beatsPerMinute * (long) var3;
                    if (this.microsecondPosition - var5 >= 0L) {
                        this.microsecondLength = var5;
                        break;
                    }

                    int var7 = (int) ((this.microsecondPosition - this.microsecondLength + (long) beatsPerMinute - 1L) / (long) beatsPerMinute);
                    this.microsecondLength += (long) var7 * (long) beatsPerMinute;
                    this.patchStream.fill(var1, var2, var7);
                    var2 += var7;
                    var3 -= var7;
                    this.method4758();
                } while (this.midiFileReader.isReady());
            }

            this.patchStream.fill(var1, var2, var3);
        }

        void method4758() throws FileNotFoundException {
            int trackNumber = this.track;
            int trackCount = this.trackLength;

            long position;
            for (position = this.microsecondPosition; trackCount == this.trackLength; position = this.midiFileReader.method4934(trackCount)) {
                while (trackCount == this.midiFileReader.trackLengths[trackNumber]) {
                    this.midiFileReader.gotoTrack(trackNumber);
                    int midiMessage = this.midiFileReader.readMessage(trackNumber);
                    if (midiMessage == 1) {
                        this.midiFileReader.setTrackDone();
                        this.midiFileReader.markTrackPosition(trackNumber);
                        if (this.midiFileReader.isDone()) {
                            if (!this.isLooping || trackCount == 0) {
                                this.systemReset();
                                this.midiFileReader.clear();
                                return;
                            }

                            this.midiFileReader.reset(position);
                        }
                        break;
                    }

                    if ((midiMessage & 128) != 0) {
                        this.dispatchEvent(midiMessage);
                    }

                    this.midiFileReader.readTrackLength(trackNumber);
                    this.midiFileReader.markTrackPosition(trackNumber);
                }

                trackNumber = this.midiFileReader.getPrioritizedTrack();
                trackCount = this.midiFileReader.trackLengths[trackNumber];
            }

            this.track = trackNumber;
            this.trackLength = trackCount;
            this.microsecondPosition = position;
        }

        void dispatchEvent(int message) throws FileNotFoundException {
            int command = message & 240;
            int channel;
            int data1;
            int data2;
            if (command == 128) {
                channel = message & 15;
                data1 = message >> 8 & 127;
                this.noteOff(channel, data1);
            } else if (command == 144) {
                channel = message & 15;
                data1 = message >> 8 & 127;
                data2 = message >> 16 & 127;
                if (data2 > 0) {
                    this.noteOn(channel, data1, data2);
                } else {
                    this.noteOff(channel, data1);
                }

            } else if (command == 176) {
                channel = message & 15;
                data1 = message >> 8 & 127;
                data2 = message >> 16 & 127;
                if (data1 == 0) {
                    this.bankControls[channel] = (data2 << 14) + (this.bankControls[channel] & -2080769);
                }

                if (data1 == 32) {
                    this.bankControls[channel] = (data2 << 7) + (this.bankControls[channel] & -16257);
                }

                if (data1 == 1) {
                    this.modulationControls[channel] = (data2 << 7) + (this.modulationControls[channel] & -16257);
                }

                if (data1 == 33) {
                    this.modulationControls[channel] = data2 + (this.modulationControls[channel] & -128);
                }

                if (data1 == 5) {
                    this.portamentoTimeControls[channel] = (data2 << 7) + (this.portamentoTimeControls[channel] & -16257);
                }

                if (data1 == 37) {
                    this.portamentoTimeControls[channel] = data2 + (this.portamentoTimeControls[channel] & -128);
                }

                if (data1 == 7) {
                    this.volumeControls[channel] = (data2 << 7) + (this.volumeControls[channel] & -16257);
                }

                if (data1 == 39) {
                    this.volumeControls[channel] = data2 + (this.volumeControls[channel] & -128);
                }

                if (data1 == 10) {
                    this.panControls[channel] = (data2 << 7) + (this.panControls[channel] & -16257);
                }

                if (data1 == 42) {
                    this.panControls[channel] = data2 + (this.panControls[channel] & -128);
                }

                if (data1 == 11) {
                    this.expressionControls[channel] = (data2 << 7) + (this.expressionControls[channel] & -16257);
                }

                if (data1 == 43) {
                    this.expressionControls[channel] = data2 + (this.expressionControls[channel] & -128);
                }

                int[] values;
                if (data1 == 64) {
                    if (data2 >= 64) {
                        values = this.switchControls;
                        values[channel] |= 1;
                    } else {
                        values = this.switchControls;
                        values[channel] &= -2;
                    }
                }

                if (data1 == 65) {
                    if (data2 >= 64) {
                        values = this.switchControls;
                        values[channel] |= 2;
                    } else {
                        values = this.switchControls;
                        values[channel] &= -3;
                    }
                }

                if (data1 == 99) {
                    this.dataEntriesMSB[channel] = (data2 << 7) + (this.dataEntriesMSB[channel] & 127);
                }

                if (data1 == 98) {
                    this.dataEntriesMSB[channel] = (this.dataEntriesMSB[channel] & 16256) + data2;
                }

                if (data1 == 101) {
                    this.dataEntriesMSB[channel] = (data2 << 7) + (this.dataEntriesMSB[channel] & 127) + 16384;
                }

                if (data1 == 100) {
                    this.dataEntriesMSB[channel] = (this.dataEntriesMSB[channel] & 16256) + data2 + 16384;
                }

                if (data1 == 120) {
                    this.allSoundOff(channel);
                }

                if (data1 == 121) {
                    this.resetAllControllers(channel);
                }

                int dataEntry;
                if (data1 == 6) {
                    dataEntry = this.dataEntriesMSB[channel];
                    if (dataEntry == 16384) {
                        this.dataEntriesLSB[channel] = (data2 << 7) + (this.dataEntriesLSB[channel] & -16257);
                    }
                }

                if (data1 == 38) {
                    dataEntry = this.dataEntriesMSB[channel];
                    if (dataEntry == 16384) {
                        this.dataEntriesLSB[channel] = data2 + (this.dataEntriesLSB[channel] & -128);
                    }
                }

                if (data1 == 16) {
                    this.sampleLoopControls[channel] = (data2 << 7) + (this.sampleLoopControls[channel] & -16257);
                }

                if (data1 == 48) {
                    this.sampleLoopControls[channel] = data2 + (this.sampleLoopControls[channel] & -128);
                }

                if (data1 == 81) {
                    if (data2 >= 64) {
                        values = this.switchControls;
                        values[channel] |= 4;
                    } else {
                        this.setReTriggerSwitch(channel);
                        values = this.switchControls;
                        values[channel] &= -5;
                    }
                }

                if (data1 == 17) {
                    this.reTrigger(channel, (data2 << 7) + (this.reTriggerControls[channel] & -16257));
                }

                if (data1 == 49) {
                    this.reTrigger(channel, data2 + (this.reTriggerControls[channel] & -128));
                }

            } else if (command == 192) {
                channel = message & 15;
                data1 = message >> 8 & 127;
                this.programChange(channel, data1 + this.bankControls[channel]);
            } else if (command == 224) {
                channel = message & 15;
                data1 = (message >> 8 & 127) + (message >> 9 & 16256);
                this.pitchBend(channel, data1);
            } else {
                command = message & 255;
                if (command == 255) {
                    this.systemReset();
                }
            }
        }

        /**
         * A method to set the default patch for a channel, if data does not already exist in the MIDI sequence.
         *
         * @param channel The MIDI Channel number (0-15).
         * @param patch   An integer representing a Sound Bank Patch ID.
         */
        public synchronized void setInitialPatch(int channel, int patch) throws FileNotFoundException {
            this.setPatch(channel, patch);
        }

        /**
         * A method to set the patch for a channel.
         *
         * @param channel The MIDI Channel number (0-15).
         * @param patch   An integer representing a Sound Bank Patch ID.
         */
        void setPatch(int channel, int patch) throws FileNotFoundException {
            this.programConstants[channel] = patch;
            this.bankControls[channel] = patch & -128;
            this.programChange(channel, patch);
        }

        /**
         * A method to issue a program change event.
         *
         * @param channel The MIDI Channel number (0-15).
         * @param program An integer representing the program change value.
         */
        void programChange(int channel, int program) throws FileNotFoundException {
            loadMusicPatch(program);
            if (program != this.patch[channel]) {
                this.patch[channel] = program;
                for (int note = 0; note < 128; ++note) {
                    this.continuousVoices[channel][note] = null;
                }
            }

        }

        /**
         * A method to issue a note on event.
         *
         * @param channel The MIDI Channel number (0-15).
         * @param data1   The first data value, representing a note pitch.
         * @param data2   The second data value, representing the velocity of the note.
         */
        void noteOn(int channel, int data1, int data2) {
            this.noteOff(channel, data1);
            if ((this.switchControls[channel] & 2) != 0) {
                int index = 0;
                for (MusicPatchVoice musicPatchVoice = this.patchStream.musicPatchVoices.get(index); musicPatchVoice != null; musicPatchVoice = this.patchStream.musicPatchVoices.get(index++)) {
                    if (musicPatchVoice.midiChannel == channel && musicPatchVoice.releasePosition < 0) {
                        this.oneShotVoices[channel][musicPatchVoice.midiNote] = null;
                        this.oneShotVoices[channel][data1] = musicPatchVoice;
                        int currentPitch = (musicPatchVoice.portamentoOffset * musicPatchVoice.pitchShiftOffset >> 12) + musicPatchVoice.soundTransposition;
                        musicPatchVoice.soundTransposition += data1 - musicPatchVoice.midiNote << 8;
                        musicPatchVoice.pitchShiftOffset = currentPitch - musicPatchVoice.soundTransposition;
                        musicPatchVoice.portamentoOffset = 4096;
                        musicPatchVoice.midiNote = data1;
                        return;
                    }
                }
            }

            MusicPatch musicPatch = this.musicPatches.get(this.patch[channel]);
            if (musicPatch != null) {
                AudioDataSource audioDataSource = musicPatch.audioDataSources[data1];
                if (audioDataSource != null) {
                    MusicPatchVoice musicPatchVoice = new MusicPatchVoice();
                    musicPatchVoice.midiChannel = channel;
                    musicPatchVoice.patch = musicPatch;
                    musicPatchVoice.audioDataSource = audioDataSource;
                    musicPatchVoice.musicPatchEnvelope = musicPatch.musicPatchEnvelopes[data1];
                    musicPatchVoice.loopType = musicPatch.loopOffset[data1];
                    musicPatchVoice.midiNote = data1;
                    musicPatchVoice.midiNoteVolume = data2 * data2 * musicPatch.volumeOffset[data1] * musicPatch.volume + 512 >> 11;
                    musicPatchVoice.midiNotePan = musicPatch.panOffset[data1] & 255;
                    musicPatchVoice.soundTransposition = (data1 << 8) - (musicPatch.pitchOffset[data1] & 32767);
                    musicPatchVoice.decayEnvelopePosition = 0;
                    musicPatchVoice.attackEnvelopePosition = 0;
                    musicPatchVoice.positionOffset = 0;
                    musicPatchVoice.releasePosition = -1;
                    musicPatchVoice.releaseOffset = 0;
                    if (this.sampleLoopControls[channel] == 0) {
                        musicPatchVoice.stream = RawAudioStream.createSampledAudioStream(audioDataSource, this.calculatePitch(musicPatchVoice), this.calculateVolume(musicPatchVoice), this.calculatePanning(musicPatchVoice));
                    } else {
                        musicPatchVoice.stream = RawAudioStream.createSampledAudioStream(audioDataSource, this.calculatePitch(musicPatchVoice), 0, this.calculatePanning(musicPatchVoice));
                        this.modifySampleLoopStart(musicPatchVoice, musicPatch.pitchOffset[data1] < 0);
                    }

                    if (musicPatch.pitchOffset[data1] < 0) {
                        if (musicPatchVoice.stream != null && musicPatchVoice.audioDataSource.isLooping) {
                            musicPatchVoice.stream.setNumLoops(-1);
                        }
                    }

                    if (musicPatchVoice.loopType >= 0) {
                        MusicPatchVoice loopedVoice = this.continuousVoices[channel][musicPatchVoice.loopType];
                        if (loopedVoice != null && loopedVoice.releasePosition < 0) {
                            this.oneShotVoices[channel][loopedVoice.midiNote] = null;
                            loopedVoice.releasePosition = 0;
                        }

                        this.continuousVoices[channel][musicPatchVoice.loopType] = musicPatchVoice;
                    }

                    this.patchStream.musicPatchVoices.add(musicPatchVoice);
                    this.oneShotVoices[channel][data1] = musicPatchVoice;
                }
            }
        }

        /**
         * A method to modify the sample loop, a special effect exclusive to RuneScape.
         *
         * @param musicPatchVoice The synthesized sound, also called a voice.
         * @param validPitch      True if the sound is a valid note (0-127), otherwise false.
         */
        void modifySampleLoopStart(MusicPatchVoice musicPatchVoice, boolean validPitch) {
            int audioDataLength = musicPatchVoice.audioDataSource.audioData.length;
            int newLoopStart;
            if (validPitch && musicPatchVoice.audioDataSource.isLooping) {
                int newLoopStartPosition = audioDataLength + audioDataLength - musicPatchVoice.audioDataSource.loopStart;
                newLoopStart = (int) ((long) this.sampleLoopControls[musicPatchVoice.midiChannel] * (long) newLoopStartPosition >> 6);
                audioDataLength <<= 8;
                if (newLoopStart >= audioDataLength) {
                    newLoopStart = audioDataLength + audioDataLength - 1 - newLoopStart;
                    musicPatchVoice.stream.processSamplePitch();
                }
            } else {
                newLoopStart = (int) ((long) audioDataLength * (long) this.sampleLoopControls[musicPatchVoice.midiChannel] >> 6);
            }

            musicPatchVoice.stream.setNewLoopStartPosition(newLoopStart);
        }

        /**
         * A method to issue a note off event.
         *
         * @param channel The MIDI Channel number (0-15).
         * @param data1   The first data value, representing a note pitch.
         */
        void noteOff(int channel, int data1) {
            MusicPatchVoice musicPatchVoice = this.oneShotVoices[channel][data1];
            if (musicPatchVoice != null) {
                this.oneShotVoices[channel][data1] = null;
                if ((this.switchControls[channel] & 2) != 0) {
                    int index = 0;
                    for (MusicPatchVoice patchVoice = this.patchStream.musicPatchVoices.get(index); patchVoice != null; patchVoice = this.patchStream.musicPatchVoices.get(index++)) {
                        if (musicPatchVoice.midiChannel == patchVoice.midiChannel && patchVoice.releasePosition < 0 && musicPatchVoice != patchVoice) {
                            musicPatchVoice.releasePosition = 0;
                            break;
                        }
                    }
                } else {
                    musicPatchVoice.releasePosition = 0;
                }

            }
        }

        /**
         * A method to issue a pitch bend event.
         *
         * @param channel The MIDI Channel number (0-15).
         * @param data    The data value, calculated by (data1 + data2) * 128.
         */
        void pitchBend(int channel, int data) {
            this.pitchBendControls[channel] = data;
        }

        /**
         * A method to issue an all sound off event.
         *
         * @param channel The MIDI Channel number (0-15).
         */
        void allSoundOff(int channel) {
            if (this.patchStream.musicPatchVoices.size() != 0) {
                int index = 0;
                for (MusicPatchVoice musicPatchVoice = this.patchStream.musicPatchVoices.get(index); musicPatchVoice != null; musicPatchVoice = this.patchStream.musicPatchVoices.get(index++)) {
                    if (channel < 0 || musicPatchVoice.midiChannel == channel) {
                        if (musicPatchVoice.stream != null) {
                            musicPatchVoice.stream.reset(sampleRate / 100);
                            musicPatchVoice.reset();
                        }

                        if (musicPatchVoice.releasePosition < 0) {
                            this.oneShotVoices[musicPatchVoice.midiChannel][musicPatchVoice.midiNote] = null;
                        }

                        musicPatchVoice.reset();
                    }
                }
            }

        }

        /**
         * A method to issue a reset all controllers' event.
         *
         * @param channel The MIDI Channel number (0-15).
         */
        void resetAllControllers(int channel) {
            if (channel >= 0) {
                this.volumeControls[channel] = 12800;
                this.panControls[channel] = 8192;
                this.expressionControls[channel] = 16383;
                this.pitchBendControls[channel] = 8192;
                this.modulationControls[channel] = 0;
                this.portamentoTimeControls[channel] = 8192;
                this.setPortamentoSwitch(channel);
                this.setReTriggerSwitch(channel);
                this.switchControls[channel] = 0;
                this.dataEntriesMSB[channel] = 32767;
                this.dataEntriesLSB[channel] = 256;
                this.sampleLoopControls[channel] = 0;
                this.reTrigger(channel, 8192);
            } else {
                for (channel = 0; channel < 16; ++channel) {
                    this.resetAllControllers(channel);
                }

            }
        }

        /**
         * A method to issue a system reset event.
         */
        public void systemReset() {
            this.allSoundOff(-1);
            this.resetAllControllers(-1);

            int channel;
            for (channel = 0; channel < 16; ++channel) {
                this.patch[channel] = this.programConstants[channel];
            }

            for (channel = 0; channel < 16; ++channel) {
                this.bankControls[channel] = this.programConstants[channel] & -128;
            }

        }

        /**
         * A method to set the Portamento switch on/off.
         *
         * @param channel The MIDI Channel number (0-15).
         */
        void setPortamentoSwitch(int channel) {
            if ((this.switchControls[channel] & 2) != 0) {
                int index = 0;
                for (MusicPatchVoice musicPatchVoice = this.patchStream.musicPatchVoices.get(index); musicPatchVoice != null; musicPatchVoice = this.patchStream.musicPatchVoices.get(index++)) {
                    if (musicPatchVoice.midiChannel == channel && this.oneShotVoices[channel][musicPatchVoice.midiNote] == null && musicPatchVoice.releasePosition < 0) {
                        musicPatchVoice.releasePosition = 0;
                    }
                }
            }

        }

        /**
         * A method to set the ReTrigger Effect switch on/off.
         *
         * @param channel The MIDI Channel number (0-15).
         */
        void setReTriggerSwitch(int channel) {
            if ((this.switchControls[channel] & 4) != 0) {
                int index = 0;
                for (MusicPatchVoice musicPatchVoice = this.patchStream.musicPatchVoices.get(index); musicPatchVoice != null; musicPatchVoice = this.patchStream.musicPatchVoices.get(index++)) {
                    if (musicPatchVoice.midiChannel == channel) {
                        musicPatchVoice.reTriggerAmount = 0;
                    }
                }
            }

        }

        /**
         * A method to set the ReTrigger Effect values.
         *
         * @param channel The MIDI Channel number (0-15).
         * @param data    The data value.
         */
        void reTrigger(int channel, int data) {
            this.reTriggerControls[channel] = data;
            this.reTriggerEffects[channel] = (int) (2097152.0D * Math.pow(2.0D, 5.4931640625E-4D * (double) data) + 0.5D);
        }

        /**
         * A method used to calculate pitch.
         *
         * @param musicPatchVoice The synthesized sound, also called a voice.
         */
        int calculatePitch(MusicPatchVoice musicPatchVoice) {
            int shiftAmount = (musicPatchVoice.portamentoOffset * musicPatchVoice.pitchShiftOffset >> 12) + musicPatchVoice.soundTransposition;
            shiftAmount += (this.pitchBendControls[musicPatchVoice.midiChannel] - 8192) * this.dataEntriesLSB[musicPatchVoice.midiChannel] >> 12;
            MusicPatchEnvelope musicPatchEnvelope = musicPatchVoice.musicPatchEnvelope;
            int pitch;
            if (musicPatchEnvelope.vibratoDepth > 0 && (musicPatchEnvelope.vibratoSpeed > 0 || this.modulationControls[musicPatchVoice.midiChannel] > 0)) {
                pitch = musicPatchEnvelope.vibratoSpeed << 2;
                int vibratoDelay = musicPatchEnvelope.vibratoWarmUp << 1;
                if (musicPatchVoice.delayOffset < vibratoDelay) {
                    pitch = pitch * musicPatchVoice.delayOffset / vibratoDelay;
                }

                pitch += this.modulationControls[musicPatchVoice.midiChannel] >> 7;
                double frequency = Math.sin(0.01227184630308513D * (double) (musicPatchVoice.frequencyOffset & 511));
                shiftAmount += (int) (frequency * (double) pitch);
            }

            pitch = (int) ((double) (musicPatchVoice.audioDataSource.sampleRate * 256) * Math.pow(2.0D, (double) shiftAmount * 3.255208333333333E-4D) / (double) sampleRate + 0.5D);
            return Math.max(pitch, 1);
        }

        /**
         * A method used to calculate volume.
         *
         * @param musicPatchVoice The synthesized sound, also called a voice.
         */
        int calculateVolume(MusicPatchVoice musicPatchVoice) {
            MusicPatchEnvelope musicPatchEnvelope = musicPatchVoice.musicPatchEnvelope;
            int overallVolume = this.expressionControls[musicPatchVoice.midiChannel] * this.volumeControls[musicPatchVoice.midiChannel] + 4096 >> 13;
            overallVolume = overallVolume * overallVolume + 16384 >> 15;
            overallVolume = overallVolume * musicPatchVoice.midiNoteVolume + 16384 >> 15;
            overallVolume = overallVolume * this.volume + 128 >> 8;
            if (musicPatchEnvelope.fadeOut > 0) {
                overallVolume = (int) ((double) overallVolume * Math.pow(0.5D, (double) musicPatchEnvelope.fadeOut * (double) musicPatchVoice.decayEnvelopePosition * 1.953125E-5D) + 0.5D);
            }

            int position;
            int offset;
            int currentValue;
            int nextValue;
            if (musicPatchEnvelope.main != null) {
                position = musicPatchVoice.attackEnvelopePosition;
                offset = musicPatchEnvelope.main[musicPatchVoice.positionOffset + 1];
                if (musicPatchVoice.positionOffset < musicPatchEnvelope.main.length - 2) {
                    currentValue = (musicPatchEnvelope.main[musicPatchVoice.positionOffset] & 255) << 8;
                    nextValue = (musicPatchEnvelope.main[musicPatchVoice.positionOffset + 2] & 255) << 8;
                    offset += (position - currentValue) * (musicPatchEnvelope.main[musicPatchVoice.positionOffset + 3] - offset) / (nextValue - currentValue);
                }

                overallVolume = overallVolume * offset + 32 >> 6;
            }

            if (musicPatchVoice.releasePosition > 0 && musicPatchEnvelope.release != null) {
                position = musicPatchVoice.releasePosition;
                offset = musicPatchEnvelope.release[musicPatchVoice.releaseOffset + 1];
                if (musicPatchVoice.releaseOffset < musicPatchEnvelope.release.length - 2) {
                    currentValue = (musicPatchEnvelope.release[musicPatchVoice.releaseOffset] & 255) << 8;
                    nextValue = (musicPatchEnvelope.release[musicPatchVoice.releaseOffset + 2] & 255) << 8;
                    offset += (musicPatchEnvelope.release[musicPatchVoice.releaseOffset + 3] - offset) * (position - currentValue) / (nextValue - currentValue);
                }

                overallVolume = offset * overallVolume + 32 >> 6;
            }

            return overallVolume;
        }

        /**
         * A method used to calculate panning.
         *
         * @param musicPatchVoice The synthesized sound, also called a voice.
         */
        int calculatePanning(MusicPatchVoice musicPatchVoice) {
            int panValue = this.panControls[musicPatchVoice.midiChannel];
            return panValue < 8192 ? panValue * musicPatchVoice.midiNotePan + 32 >> 6 : 16384 - ((128 - musicPatchVoice.midiNotePan) * (16384 - panValue) + 32 >> 6);
        }

        /**
         * A method used to determine whether the voice is inactive or not.
         *
         * @param musicPatchVoice The synthesized sound, also called a voice.
         */
        boolean isInactive(MusicPatchVoice musicPatchVoice) {
            if (musicPatchVoice.stream == null) {
                if (musicPatchVoice.releasePosition >= 0) {
                    musicPatchVoice.reset();
                    if (musicPatchVoice.loopType > 0 && musicPatchVoice == this.continuousVoices[musicPatchVoice.midiChannel][musicPatchVoice.loopType]) {
                        this.continuousVoices[musicPatchVoice.midiChannel][musicPatchVoice.loopType] = null;
                    }
                }
                return false;
            } else {
                return true;
            }
        }

        /**
         * A method used to determine whether the voice is active or not.
         *
         * @param musicPatchVoice The synthesized sound, also called a voice.
         */
        boolean isActive(MusicPatchVoice musicPatchVoice, int[] samples, int offset, int length) {
            musicPatchVoice.samplesInMs = sampleRate / 100;
            if (musicPatchVoice.releasePosition < 0 || musicPatchVoice.stream != null && !musicPatchVoice.stream.isLoopValid()) {
                int slideAmount = musicPatchVoice.portamentoOffset;
                if (slideAmount > 0) {
                    slideAmount -= (int) (16.0D * Math.pow(2.0D, 4.921259842519685E-4D * (double) this.portamentoTimeControls[musicPatchVoice.midiChannel]) + 0.5D);
                    if (slideAmount < 0) {
                        slideAmount = 0;
                    }

                    musicPatchVoice.portamentoOffset = slideAmount;
                }

                musicPatchVoice.stream.setSampleBasePitch(this.calculatePitch(musicPatchVoice));
                MusicPatchEnvelope musicPatchEnvelope = musicPatchVoice.musicPatchEnvelope;
                boolean reachedEndOfArray = false;
                ++musicPatchVoice.delayOffset;
                musicPatchVoice.frequencyOffset += musicPatchEnvelope.vibratoDepth;
                double pitch = 5.086263020833333E-6D * (double) ((musicPatchVoice.midiNote - 60 << 8) + (musicPatchVoice.pitchShiftOffset * musicPatchVoice.portamentoOffset >> 12));
                if (musicPatchEnvelope.fadeOut > 0) {
                    if (musicPatchEnvelope.highNoteFadeOut > 0) {
                        musicPatchVoice.decayEnvelopePosition += (int) (128.0D * Math.pow(2.0D, (double) musicPatchEnvelope.highNoteFadeOut * pitch) + 0.5D);
                    } else {
                        musicPatchVoice.decayEnvelopePosition += 128;
                    }
                }

                if (musicPatchEnvelope.main != null) {
                    if (musicPatchEnvelope.mainEnvelope > 0) {
                        musicPatchVoice.attackEnvelopePosition += (int) (128.0D * Math.pow(2.0D, pitch * (double) musicPatchEnvelope.mainEnvelope) + 0.5D);
                    } else {
                        musicPatchVoice.attackEnvelopePosition += 128;
                    }

                    while (musicPatchVoice.positionOffset < musicPatchEnvelope.main.length - 2 && musicPatchVoice.attackEnvelopePosition > (musicPatchEnvelope.main[musicPatchVoice.positionOffset + 2] & 255) << 8) {
                        musicPatchVoice.positionOffset += 2;
                    }

                    if (musicPatchEnvelope.main.length - 2 == musicPatchVoice.positionOffset && musicPatchEnvelope.main[musicPatchVoice.positionOffset + 1] == 0) {
                        reachedEndOfArray = true;
                    }
                }

                if (musicPatchVoice.releasePosition >= 0 && musicPatchEnvelope.release != null && (this.switchControls[musicPatchVoice.midiChannel] & 1) == 0 && (musicPatchVoice.loopType < 0 || musicPatchVoice != this.continuousVoices[musicPatchVoice.midiChannel][musicPatchVoice.loopType])) {
                    if (musicPatchEnvelope.releaseEnvelope > 0) {
                        musicPatchVoice.releasePosition += (int) (128.0D * Math.pow(2.0D, pitch * (double) musicPatchEnvelope.releaseEnvelope) + 0.5D);
                    } else {
                        musicPatchVoice.releasePosition += 128;
                    }

                    while (musicPatchVoice.releaseOffset < musicPatchEnvelope.release.length - 2 && musicPatchVoice.releasePosition > (musicPatchEnvelope.release[musicPatchVoice.releaseOffset + 2] & 255) << 8) {
                        musicPatchVoice.releaseOffset += 2;
                    }

                    if (musicPatchEnvelope.release.length - 2 == musicPatchVoice.releaseOffset) {
                        reachedEndOfArray = true;
                    }
                }

                if (reachedEndOfArray) {
                    musicPatchVoice.stream.reset(musicPatchVoice.samplesInMs);
                    if (samples != null) {
                        musicPatchVoice.stream.fill(samples, offset, length);
                    }

                    musicPatchVoice.reset();
                    if (musicPatchVoice.releasePosition >= 0) {
                        musicPatchVoice.reset();
                        if (musicPatchVoice.loopType > 0 && musicPatchVoice == this.continuousVoices[musicPatchVoice.midiChannel][musicPatchVoice.loopType]) {
                            this.continuousVoices[musicPatchVoice.midiChannel][musicPatchVoice.loopType] = null;
                        }
                    }
                    return false;
                } else {
                    musicPatchVoice.stream.setDefaultVolumeAndPanning(musicPatchVoice.samplesInMs, this.calculateVolume(musicPatchVoice), this.calculatePanning(musicPatchVoice));
                    return true;
                }
            } else {
                musicPatchVoice.reset();
                if (musicPatchVoice.loopType > 0 && musicPatchVoice == this.continuousVoices[musicPatchVoice.midiChannel][musicPatchVoice.loopType]) {
                    this.continuousVoices[musicPatchVoice.midiChannel][musicPatchVoice.loopType] = null;
                }
                return false;
            }
        }
    }

    /**
     * A class that represents a single voice in the music, essentially all the characteristics of a note being processed.
     */
    static class MusicPatchVoice {

        /**
         * An integer value for the current MIDI channel number (0-15).
         */
        int midiChannel;

        /**
         * An envelope for the current Music Patch.
         */
        MusicPatchEnvelope musicPatchEnvelope;

        /**
         * The current Music Patch.
         */
        MusicPatch patch;

        /**
         * An Audio source.
         */
        AudioDataSource audioDataSource;

        /**
         * An integer value containing the loop type for the sample.
         */
        int loopType;

        /**
         * An integer value containing the value of the key pitch (0-127).
         */
        int midiNote;

        /**
         * An integer value representing the overall calculated volume of this sample.
         */
        int midiNoteVolume;

        /**
         * An integer value representing the overall calculated panning of this sample.
         */
        int midiNotePan;

        /**
         * An integer value representing the overall distance of this sample from the base pitch.
         */
        int soundTransposition;

        int pitchShiftOffset;

        int portamentoOffset;

        int decayEnvelopePosition;

        int attackEnvelopePosition;

        int positionOffset;

        int releasePosition;

        int releaseOffset;

        int delayOffset;

        int frequencyOffset;

        /**
         * The raw audio stream.
         */
        RawAudioStream stream;

        int samplesInMs;

        int reTriggerAmount;

        /**
         * A method that nullifies the main variables of this class.
         */
        void reset() {
            this.patch = null;
            this.audioDataSource = null;
            this.musicPatchEnvelope = null;
            this.stream = null;
        }
    }

    /**
     * A class which synthesizes the MIDI to audio with the Sound Bank patches.
     */
    static class MusicPatchAudioStream {

        /**
         * An array of Sound Bank patch voices.
         */
        ArrayList<MusicPatchVoice> musicPatchVoices;

        /**
         * The MIDI stream that this class is using.
         */
        MidiStream superStream;

        public int sampleRate = 44100;

        /**
         * Constructs a new stream from the MIDI stream.
         *
         * @param midiAudioStream The MIDI stream to be set.
         */
        MusicPatchAudioStream(MidiStream midiAudioStream) {
            this.superStream = midiAudioStream;
            this.musicPatchVoices = new ArrayList<>();
        }

        /**
         * A method that fills an array with audio samples.
         *
         * @param samples An array of integer values to be filled with audio samples.
         * @param offset  An integer representing the offset to start filling samples at.
         * @param length  An integer representing the length of audio to fill samples up to.
         */
        protected void fill(int[] samples, int offset, int length) {
            if (this.musicPatchVoices.size() != 0) {
                int index = 0;
                while (index < this.musicPatchVoices.size()) {
                    MusicPatchVoice musicPatchVoice = this.musicPatchVoices.get(index);
                    if (this.superStream.isInactive(musicPatchVoice)) {
                        int streamOffset = offset;
                        int streamLength = length;
                        do {
                            if (streamLength <= musicPatchVoice.samplesInMs) {
                                this.writeAudio(musicPatchVoice, samples, streamOffset, streamLength, streamLength + streamOffset);
                                musicPatchVoice.samplesInMs -= streamLength;
                                break;
                            }

                            this.writeAudio(musicPatchVoice, samples, streamOffset, musicPatchVoice.samplesInMs, streamLength + streamOffset);
                            streamOffset += musicPatchVoice.samplesInMs;
                            streamLength -= musicPatchVoice.samplesInMs;
                        } while (this.superStream.isActive(musicPatchVoice, samples, streamOffset, streamLength));
                    }
                    index++;
                }
            }

        }

        /**
         * A method that further handles writing the audio data.
         *
         * @param musicPatchVoice The current voice.
         * @param samples         The sample array.
         * @param offset          The position to start at.
         * @param samplesLength   The length of the samples.
         * @param size            The size of the audio to be output.
         */
        public void writeAudio(MusicPatchVoice musicPatchVoice, int[] samples, int offset, int samplesLength, int size) {
            if ((this.superStream.switchControls[musicPatchVoice.midiChannel] & 4) != 0 && musicPatchVoice.releasePosition < 0) {
                int effectAmount = this.superStream.reTriggerEffects[musicPatchVoice.midiChannel] / sampleRate;

                while (true) {
                    int length = (effectAmount + 1048575 - musicPatchVoice.reTriggerAmount) / effectAmount;
                    if (length > samplesLength) {
                        musicPatchVoice.reTriggerAmount += effectAmount * samplesLength;
                        break;
                    }

                    musicPatchVoice.stream.fill(samples, offset, length);
                    offset += length;
                    samplesLength -= length;
                    musicPatchVoice.reTriggerAmount += effectAmount * length - 1048576;
                    int finalAmount = sampleRate / 100;
                    int amount = 262144 / effectAmount;
                    if (amount < finalAmount) {
                        finalAmount = amount;
                    }

                    RawAudioStream rawAudioStream = musicPatchVoice.stream;
                    if (this.superStream.sampleLoopControls[musicPatchVoice.midiChannel] == 0) {
                        musicPatchVoice.stream = RawAudioStream.createSampledAudioStream(musicPatchVoice.audioDataSource, rawAudioStream.getSampleBasePitch(), rawAudioStream.getSampleVolume(), rawAudioStream.getSamplePanning());
                    } else {
                        musicPatchVoice.stream = RawAudioStream.createSampledAudioStream(musicPatchVoice.audioDataSource, rawAudioStream.getSampleBasePitch(), 0, rawAudioStream.getSamplePanning());
                        this.superStream.modifySampleLoopStart(musicPatchVoice, musicPatchVoice.patch.pitchOffset[musicPatchVoice.midiNote] < 0);
                        musicPatchVoice.stream.setDefaultVolume(finalAmount, rawAudioStream.getSampleVolume());
                    }

                    if (musicPatchVoice.patch.pitchOffset[musicPatchVoice.midiNote] < 0) {
                        if (musicPatchVoice.stream != null && musicPatchVoice.audioDataSource.isLooping) {
                            musicPatchVoice.stream.setNumLoops(-1);
                        }
                    }

                    rawAudioStream.reset(finalAmount);
                    rawAudioStream.fill(samples, offset, size - offset);
                }
            }

            musicPatchVoice.stream.fill(samples, offset, samplesLength);
        }

    }

    /**
     * A class which holds the information on specific parameters in each Music Patch relating to the characteristics of its sound.
     */
    static class MusicPatchEnvelope {

        /**
         * An array of bytes to help calculate the main envelope of a voice.
         */
        public byte[] main;

        /**
         * An array of bytes to help calculate the release envelope of a voice (How long the sample holds after note off).
         */
        public byte[] release;

        /**
         * A primary integer value to calculate the volume fade out factor of a voice.
         */
        public int fadeOut;

        /**
         * An integer value to calculate the main volume envelope of a voice.
         */
        public int mainEnvelope;

        /**
         * An integer value to calculate the volume release envelope of a voice.
         */
        public int releaseEnvelope;

        /**
         * A secondary integer value to calculate the volume fade out factor of higher note voices.
         */
        public int highNoteFadeOut;

        /**
         * An integer to represent the speed of the vibrato effect if used.
         */
        public int vibratoSpeed; //Vibrato Pitch

        /**
         * An integer to represent the depth of the vibrato effect if used.
         */
        public int vibratoDepth; //Vibrato Frequency

        /**
         * An integer to represent the delay of the vibrato effect if used.
         */
        public int vibratoWarmUp; //Vibrato Delay
    }

    /**
     * A utility class for computing the characteristics of the sound being written.
     */
    static class RawAudioStream {

        /**
         * The audio sample.
         */
        AudioDataSource sound;

        /**
         * An integer value representing a modifier amount for the looping sample.
         */
        int loopStartModifier;

        /**
         * An integer value representing the sample's pitch.
         */
        int samplePitch;

        /**
         * An integer value representing the sample's volume.
         */
        int sampleVolume;

        /**
         * An integer value representing the sample's panning.
         */
        int samplePan;

        /**
         * An integer value representing volume.
         */
        int volume;

        /**
         * An integer value representing the right channel's volume.
         */
        int rightChannelVolume;

        /**
         * An integer value representing the left channel's volume.
         */
        int leftChannelVolume;

        /**
         * An integer value representing the number of times a sample will loop.
         */
        int numLoops;

        /**
         * An integer value representing the sample's loop start.
         */
        int start;

        /**
         * An integer value representing the sample's loop end.
         */
        int end;

        /**
         * A boolean value representing the sample's loop status.
         */
        boolean isSampleLooping;

        /**
         * An integer value representing the current position in the stream.
         */
        int streamPosition;

        /**
         * An integer value representing the master volume of the stream.
         */
        int overallVolume;

        /**
         * An integer value representing the overall right channel volume.
         */
        int overallRightChannel;

        /**
         * An integer value representing the overall left channel volume.
         */
        int overallLeftChannel;

        boolean stereo = true;

        /**
         * Constructs a new raw audio stream.
         *
         * @param audioDataSource The audio source.
         * @param pitch           The pitch offset.
         * @param volume          The volume offset.
         * @param pan             The pan offset.
         */
        RawAudioStream(AudioDataSource audioDataSource, int pitch, int volume, int pan) {
            this.sound = audioDataSource;
            this.start = audioDataSource.loopStart;
            this.end = audioDataSource.loopEnd;
            this.isSampleLooping = false;//audioDataSource.isLooping;
            this.samplePitch = pitch;
            this.sampleVolume = volume;
            this.samplePan = pan;
            this.loopStartModifier = 0;
        }

        /**
         * A method that fills an array with audio samples.
         *
         * @param samples An array of integer values to be filled with audio samples.
         * @param offset  An integer representing the offset to start filling samples at.
         * @param length  An integer representing the length of audio to fill samples up to.
         */
        public synchronized void fill(int[] samples, int offset, int length) {
            if (this.sampleVolume != 0 || this.streamPosition != 0) {
                AudioDataSource audioDataSource = this.sound;
                int loopStart = this.start << 8;
                int loopEnd = this.end << 8;
                int sampleSize = audioDataSource.audioData.length << 8;
                int loopDifference = loopEnd - loopStart;
                if (loopDifference <= 0) {
                    this.numLoops = 0;
                }

                int position = offset;
                length += offset;
                if (this.loopStartModifier < 0) {
                    if (this.samplePitch <= 0) {
                        return;
                    }

                    this.loopStartModifier = 0;
                }

                if (this.loopStartModifier >= sampleSize) {
                    if (this.samplePitch >= 0) {
                        return;
                    }

                    this.loopStartModifier = sampleSize - 1;
                }

                if (this.numLoops < 0) {
                    if (this.isSampleLooping) {
                        if (this.samplePitch < 0) {
                            position = this.calculateBeginningOffset(samples, offset, loopStart, length, audioDataSource.audioData[this.start]);
                            if (this.loopStartModifier >= loopStart) {
                                return;
                            }

                            this.loopStartModifier = loopStart + loopStart - 1 - this.loopStartModifier;
                            this.samplePitch = -this.samplePitch;
                        }

                        while (true) {
                            position = this.calculateEndingOffset(samples, position, loopEnd, length, audioDataSource.audioData[this.end - 1]);
                            if (this.loopStartModifier < loopEnd) {
                                return;
                            }

                            this.loopStartModifier = loopEnd + loopEnd - 1 - this.loopStartModifier;
                            this.samplePitch = -this.samplePitch;
                            position = this.calculateBeginningOffset(samples, position, loopStart, length, audioDataSource.audioData[this.start]);
                            if (this.loopStartModifier >= loopStart) {
                                return;
                            }

                            this.loopStartModifier = loopStart + loopStart - 1 - this.loopStartModifier;
                            this.samplePitch = -this.samplePitch;
                        }
                    } else if (this.samplePitch < 0) {
                        while (true) {
                            position = this.calculateBeginningOffset(samples, position, loopStart, length, audioDataSource.audioData[this.end - 1]);
                            if (this.loopStartModifier >= loopStart) {
                                return;
                            }

                            this.loopStartModifier = loopEnd - 1 - (loopEnd - 1 - this.loopStartModifier) % loopDifference;
                        }
                    } else {
                        while (true) {
                            position = this.calculateEndingOffset(samples, position, loopEnd, length, audioDataSource.audioData[this.start]);
                            if (this.loopStartModifier < loopEnd) {
                                return;
                            }

                            this.loopStartModifier = loopStart + (this.loopStartModifier - loopStart) % loopDifference;
                        }
                    }
                } else {
                    if (this.numLoops > 0) {
                        if (this.isSampleLooping) {
                            loopLabel:
                            {
                                if (this.samplePitch < 0) {
                                    position = this.calculateBeginningOffset(samples, offset, loopStart, length, audioDataSource.audioData[this.start]);
                                    if (this.loopStartModifier >= loopStart) {
                                        return;
                                    }

                                    this.loopStartModifier = loopStart + loopStart - 1 - this.loopStartModifier;
                                    this.samplePitch = -this.samplePitch;
                                    if (--this.numLoops == 0) {
                                        break loopLabel;
                                    }
                                }

                                do {
                                    position = this.calculateEndingOffset(samples, position, loopEnd, length, audioDataSource.audioData[this.end - 1]);
                                    if (this.loopStartModifier < loopEnd) {
                                        return;
                                    }

                                    this.loopStartModifier = loopEnd + loopEnd - 1 - this.loopStartModifier;
                                    this.samplePitch = -this.samplePitch;
                                    if (--this.numLoops == 0) {
                                        break;
                                    }

                                    position = this.calculateBeginningOffset(samples, position, loopStart, length, audioDataSource.audioData[this.start]);
                                    if (this.loopStartModifier >= loopStart) {
                                        return;
                                    }

                                    this.loopStartModifier = loopStart + loopStart - 1 - this.loopStartModifier;
                                    this.samplePitch = -this.samplePitch;
                                } while (--this.numLoops != 0);
                            }
                        } else {
                            int loopOffset;
                            if (this.samplePitch < 0) {
                                while (true) {
                                    position = this.calculateBeginningOffset(samples, position, loopStart, length, audioDataSource.audioData[this.end - 1]);
                                    if (this.loopStartModifier >= loopStart) {
                                        return;
                                    }

                                    loopOffset = (loopEnd - 1 - this.loopStartModifier) / loopDifference;
                                    if (loopOffset >= this.numLoops) {
                                        this.loopStartModifier += loopDifference * this.numLoops;
                                        this.numLoops = 0;
                                        break;
                                    }

                                    this.loopStartModifier += loopDifference * loopOffset;
                                    this.numLoops -= loopOffset;
                                }
                            } else {
                                while (true) {
                                    position = this.calculateEndingOffset(samples, position, loopEnd, length, audioDataSource.audioData[this.start]);
                                    if (this.loopStartModifier < loopEnd) {
                                        return;
                                    }

                                    loopOffset = (this.loopStartModifier - loopStart) / loopDifference;
                                    if (loopOffset >= this.numLoops) {
                                        this.loopStartModifier -= loopDifference * this.numLoops;
                                        this.numLoops = 0;
                                        break;
                                    }

                                    this.loopStartModifier -= loopDifference * loopOffset;
                                    this.numLoops -= loopOffset;
                                }
                            }
                        }
                    }

                    if (this.samplePitch < 0) {
                        this.calculateBeginningOffset(samples, position, 0, length, 0);
                        if (this.loopStartModifier < 0) {
                            this.loopStartModifier = -1;
                        }
                    } else {
                        this.calculateEndingOffset(samples, position, sampleSize, length, 0);
                        if (this.loopStartModifier >= sampleSize) {
                            this.loopStartModifier = sampleSize;
                        }
                    }

                }
            }
        }

        public synchronized void setNumLoops(int loopCount) {
            this.numLoops = loopCount;
        }

        synchronized void muteStream() {
            this.mute(0, this.getSamplePanning());
        }

        synchronized void mute(int volume, int panning) {
            this.sampleVolume = volume;
            this.samplePan = panning;
            this.streamPosition = 0;
        }

        public synchronized int getSampleVolume() {
            return this.sampleVolume == Integer.MIN_VALUE ? 0 : this.sampleVolume;
        }

        public synchronized int getSamplePanning() {
            return this.samplePan < 0 ? -1 : this.samplePan;
        }

        public synchronized void setNewLoopStartPosition(int newLoopStart) {
            int sampleDataLength = this.sound.audioData.length << 8;
            if (newLoopStart < -1) {
                newLoopStart = -1;
            }

            if (newLoopStart > sampleDataLength) {
                newLoopStart = sampleDataLength;
            }

            this.loopStartModifier = newLoopStart;
        }

        public synchronized void processSamplePitch() {
            this.samplePitch = (this.samplePitch ^ this.samplePitch >> 31) + (this.samplePitch >>> 31);
            this.samplePitch = -this.samplePitch;
        }

        public synchronized void setDefaultVolume(int value, int volume) {
            this.setDefaultVolumeAndPanning(value, volume, this.getSamplePanning());
        }

        public synchronized void setDefaultVolumeAndPanning(int value, int volume, int pan) {
            if (value == 0) {
                this.mute(volume, pan);
            } else {
                int rightChannel = calculateRightChannel(volume, pan);
                int leftChannel = calculateLeftChannel(volume, pan);
                if (rightChannel == this.rightChannelVolume && leftChannel == this.leftChannelVolume) {
                    this.streamPosition = 0;
                } else {
                    int overallVolume = volume - this.volume;
                    if (this.volume - volume > overallVolume) {
                        overallVolume = this.volume - volume;
                    }

                    if (rightChannel - this.rightChannelVolume > overallVolume) {
                        overallVolume = rightChannel - this.rightChannelVolume;
                    }

                    if (this.rightChannelVolume - rightChannel > overallVolume) {
                        overallVolume = this.rightChannelVolume - rightChannel;
                    }

                    if (leftChannel - this.leftChannelVolume > overallVolume) {
                        overallVolume = leftChannel - this.leftChannelVolume;
                    }

                    if (this.leftChannelVolume - leftChannel > overallVolume) {
                        overallVolume = this.leftChannelVolume - leftChannel;
                    }

                    if (value > overallVolume) {
                        value = overallVolume;
                    }

                    this.streamPosition = value;
                    this.sampleVolume = volume;
                    this.samplePan = pan;
                    this.overallVolume = (volume - this.volume) / value;
                    this.overallRightChannel = (rightChannel - this.rightChannelVolume) / value;
                    this.overallLeftChannel = (leftChannel - this.leftChannelVolume) / value;
                }
            }
        }

        public synchronized void reset(int value) {
            if (value == 0) {
                this.muteStream();
            } else if (this.rightChannelVolume == 0 && this.leftChannelVolume == 0) {
                this.streamPosition = 0;
                this.sampleVolume = 0;
                this.volume = 0;
            } else {
                int invertedVolume = -this.volume;
                if (this.volume > invertedVolume) {
                    invertedVolume = this.volume;
                }

                if (-this.rightChannelVolume > invertedVolume) {
                    invertedVolume = -this.rightChannelVolume;
                }

                if (this.rightChannelVolume > invertedVolume) {
                    invertedVolume = this.rightChannelVolume;
                }

                if (-this.leftChannelVolume > invertedVolume) {
                    invertedVolume = -this.leftChannelVolume;
                }

                if (this.leftChannelVolume > invertedVolume) {
                    invertedVolume = this.leftChannelVolume;
                }

                if (value > invertedVolume) {
                    value = invertedVolume;
                }

                this.streamPosition = value;
                this.sampleVolume = Integer.MIN_VALUE;
                this.overallVolume = -this.volume / value;
                this.overallRightChannel = -this.rightChannelVolume / value;
                this.overallLeftChannel = -this.leftChannelVolume / value;
            }
        }

        public synchronized void setSampleBasePitch(int pitch) {
            if (this.samplePitch < 0) {
                this.samplePitch = -pitch;
            } else {
                this.samplePitch = pitch;
            }
        }

        public synchronized int getSampleBasePitch() {
            return this.samplePitch < 0 ? -this.samplePitch : this.samplePitch;
        }

        public boolean isLoopValid() {
            return this.loopStartModifier < 0 || this.loopStartModifier >= this.sound.audioData.length << 8;
        }

        int calculateEndingOffset(int[] samples, int offset, int endPosition, int sampleLength, int sampleLoopEnd) {
            while (true) {
                if (this.streamPosition > 0) {
                    int positionOffset = offset + this.streamPosition;
                    if (positionOffset > sampleLength) {
                        positionOffset = sampleLength;
                    }

                    this.streamPosition += offset;
                    if (this.samplePitch == 256 && (this.loopStartModifier & 255) == 0) {
                        if (stereo) {
                            offset = calculateUnmodifiedStereoOffset(this.sound.audioData, samples, this.loopStartModifier, offset, this.rightChannelVolume, this.leftChannelVolume, this.overallRightChannel, this.overallLeftChannel, positionOffset, endPosition, this);
                        } else {
                            offset = calculateUnmodifiedMonoOffset(this.sound.audioData, samples, this.loopStartModifier, offset, this.volume, this.overallVolume, positionOffset, endPosition, this);
                        }
                    } else if (stereo) {
                        offset = calculateModifiedStereoOffset(this.sound.audioData, samples, this.loopStartModifier, offset, this.rightChannelVolume, this.leftChannelVolume, this.overallRightChannel, this.overallLeftChannel, positionOffset, endPosition, this, this.samplePitch, sampleLoopEnd);
                    } else {
                        offset = calculateModifiedMonoOffset(this.sound.audioData, samples, this.loopStartModifier, offset, this.volume, this.overallVolume, positionOffset, endPosition, this, this.samplePitch, sampleLoopEnd);
                    }

                    this.streamPosition -= offset;
                    if (this.streamPosition != 0) {
                        return offset;
                    }

                    if (this.streamIsNotMuted()) {
                        continue;
                    }

                    return sampleLength;
                }

                if (this.samplePitch == 256 && (this.loopStartModifier & 255) == 0) {
                    if (stereo) {
                        return getUnmodifiedStereoEndOffset(this.sound.audioData, samples, this.loopStartModifier, offset, this.rightChannelVolume, this.leftChannelVolume, sampleLength, endPosition, this);
                    }

                    return getUnmodifiedMonoEndOffset(this.sound.audioData, samples, this.loopStartModifier, offset, this.volume, sampleLength, endPosition, this);
                }

                if (stereo) {
                    return getModifiedStereoEndOffset(this.sound.audioData, samples, this.loopStartModifier, offset, this.rightChannelVolume, this.leftChannelVolume, sampleLength, endPosition, this, this.samplePitch, sampleLoopEnd);
                }

                return getModifiedMonoEndOffset(this.sound.audioData, samples, this.loopStartModifier, offset, this.volume, sampleLength, endPosition, this, this.samplePitch, sampleLoopEnd);
            }
        }

        int calculateBeginningOffset(int[] samples, int offset, int startPosition, int sampleLength, int loopStartPosition) {
            while (true) {
                if (this.streamPosition > 0) {
                    int volumeOffset = offset + this.streamPosition;
                    if (volumeOffset > sampleLength) {
                        volumeOffset = sampleLength;
                    }

                    this.streamPosition += offset;
                    if (this.samplePitch == -256 && (this.loopStartModifier & 255) == 0) {
                        if (stereo) {
                            offset = getUnmodifiedLoopStartStereoOffset(this.sound.audioData, samples, this.loopStartModifier, offset, this.rightChannelVolume, this.leftChannelVolume, this.overallRightChannel, this.overallLeftChannel, volumeOffset, startPosition, this);
                        } else {
                            offset = getUnmodifiedLoopStartMonoOffset(this.sound.audioData, samples, this.loopStartModifier, offset, this.volume, this.overallVolume, volumeOffset, startPosition, this);
                        }
                    } else if (stereo) {
                        offset = getModifiedLoopStartStereoOffset(this.sound.audioData, samples, this.loopStartModifier, offset, this.rightChannelVolume, this.leftChannelVolume, this.overallRightChannel, this.overallLeftChannel, volumeOffset, startPosition, this, this.samplePitch, loopStartPosition);
                    } else {
                        offset = getModifiedLoopStartMonoOffset(this.sound.audioData, samples, this.loopStartModifier, offset, this.volume, this.overallVolume, volumeOffset, startPosition, this, this.samplePitch, loopStartPosition);
                    }

                    this.streamPosition -= offset;
                    if (this.streamPosition != 0) {
                        return offset;
                    }

                    if (this.streamIsNotMuted()) {
                        continue;
                    }

                    return sampleLength;
                }

                if (this.samplePitch == -256 && (this.loopStartModifier & 255) == 0) {
                    if (stereo) {
                        return getFirstUnmodifiedStereoOffset(this.sound.audioData, samples, this.loopStartModifier, offset, this.rightChannelVolume, this.leftChannelVolume, sampleLength, startPosition, this);
                    }

                    return getFirstUnmodifiedMonoOffset(this.sound.audioData, samples, this.loopStartModifier, offset, this.volume, sampleLength, startPosition, this);
                }

                if (stereo) {
                    return getFirstModifiedStereoOffset(this.sound.audioData, samples, this.loopStartModifier, offset, this.rightChannelVolume, this.leftChannelVolume, sampleLength, startPosition, this, this.samplePitch, loopStartPosition);
                }

                return getFirstModifiedMonoOffset(this.sound.audioData, samples, this.loopStartModifier, offset, this.volume, sampleLength, startPosition, this, this.samplePitch, loopStartPosition);
            }
        }

        boolean streamIsNotMuted() {
            int sampleVolume = this.sampleVolume;
            int rightVolume;
            int leftVolume;
            if (sampleVolume == Integer.MIN_VALUE) {
                leftVolume = 0;
                rightVolume = 0;
                sampleVolume = 0;
            } else {
                rightVolume = calculateRightChannel(sampleVolume, this.samplePan);
                leftVolume = calculateLeftChannel(sampleVolume, this.samplePan);
            }

            if (sampleVolume == this.volume && rightVolume == this.rightChannelVolume && leftVolume == this.leftChannelVolume) {
                if (this.sampleVolume == Integer.MIN_VALUE) {
                    this.sampleVolume = 0;
                    return false;
                } else {
                    return true;
                }
            } else {
                if (this.volume < sampleVolume) {
                    this.overallVolume = 1;
                    this.streamPosition = sampleVolume - this.volume;
                } else if (this.volume > sampleVolume) {
                    this.overallVolume = -1;
                    this.streamPosition = this.volume - sampleVolume;
                } else {
                    this.overallVolume = 0;
                }

                if (this.rightChannelVolume < rightVolume) {
                    this.overallRightChannel = 1;
                    if (this.streamPosition == 0 || this.streamPosition > rightVolume - this.rightChannelVolume) {
                        this.streamPosition = rightVolume - this.rightChannelVolume;
                    }
                } else if (this.rightChannelVolume > rightVolume) {
                    this.overallRightChannel = -1;
                    if (this.streamPosition == 0 || this.streamPosition > this.rightChannelVolume - rightVolume) {
                        this.streamPosition = this.rightChannelVolume - rightVolume;
                    }
                } else {
                    this.overallRightChannel = 0;
                }

                if (this.leftChannelVolume < leftVolume) {
                    this.overallLeftChannel = 1;
                    if (this.streamPosition == 0 || this.streamPosition > leftVolume - this.leftChannelVolume) {
                        this.streamPosition = leftVolume - this.leftChannelVolume;
                    }
                } else if (this.leftChannelVolume > leftVolume) {
                    this.overallLeftChannel = -1;
                    if (this.streamPosition == 0 || this.streamPosition > this.leftChannelVolume - leftVolume) {
                        this.streamPosition = this.leftChannelVolume - leftVolume;
                    }
                } else {
                    this.overallLeftChannel = 0;
                }

                return true;
            }
        }

        static int calculateRightChannel(int volume, int pan) {
            return pan < 0 ? volume : (int) ((double) volume * Math.sqrt((double) (16384 - pan) * 1.220703125E-4D) + 0.5D);
        }

        static int calculateLeftChannel(int volume, int pan) {
            return pan < 0 ? -volume : (int) ((double) volume * Math.sqrt((double) pan * 1.220703125E-4D) + 0.5D);
        }

        public static RawAudioStream createSampledAudioStream(AudioDataSource sound, int pitchFactor, int volumeFactor, int panFactor) {
            return sound.audioData != null && sound.audioData.length != 0 ? new RawAudioStream(sound, pitchFactor, volumeFactor, panFactor) : null;
        }

        static int getUnmodifiedMonoEndOffset(byte[] audioData, int[] samples, int loopStartModifier, int offset, int volume, int sampleLength, int endPosition, RawAudioStream rawAudioStream) {
            loopStartModifier >>= 8;
            endPosition >>= 8;
            volume <<= 2;
            int position;
            if ((position = offset + endPosition - loopStartModifier) > sampleLength) {
                position = sampleLength;
            }

            int index;
            for (position -= 3; offset < position; samples[index] += audioData[loopStartModifier++] * volume) {
                index = offset++;
                samples[index] += audioData[loopStartModifier++] * volume;
                index = offset++;
                samples[index] += audioData[loopStartModifier++] * volume;
                index = offset++;
                samples[index] += audioData[loopStartModifier++] * volume;
                index = offset++;
            }

            for (position += 3; offset < position; samples[index] += audioData[loopStartModifier++] * volume) {
                index = offset++;
            }

            rawAudioStream.loopStartModifier = loopStartModifier << 8;
            return offset;
        }

        static int getUnmodifiedStereoEndOffset(byte[] audioData, int[] samples, int loopStartModifier, int offset, int rightChannelVolume, int leftChannelVolume, int length, int endPosition, RawAudioStream rawAudioStream) {
            loopStartModifier >>= 8;
            endPosition >>= 8;
            rightChannelVolume <<= 2;
            leftChannelVolume <<= 2;
            int position;
            if ((position = offset + endPosition - loopStartModifier) > length) {
                position = length;
            }

            offset <<= 1;
            position <<= 1;

            int index;
            byte audioDataByte;
            for (position -= 6; offset < position; samples[index] += audioDataByte * leftChannelVolume) {
                audioDataByte = audioData[loopStartModifier++];
                index = offset++;
                samples[index] += audioDataByte * rightChannelVolume;
                index = offset++;
                samples[index] += audioDataByte * leftChannelVolume;
                audioDataByte = audioData[loopStartModifier++];
                index = offset++;
                samples[index] += audioDataByte * rightChannelVolume;
                index = offset++;
                samples[index] += audioDataByte * leftChannelVolume;
                audioDataByte = audioData[loopStartModifier++];
                index = offset++;
                samples[index] += audioDataByte * rightChannelVolume;
                index = offset++;
                samples[index] += audioDataByte * leftChannelVolume;
                audioDataByte = audioData[loopStartModifier++];
                index = offset++;
                samples[index] += audioDataByte * rightChannelVolume;
                index = offset++;
            }

            for (position += 6; offset < position; samples[index] += audioDataByte * leftChannelVolume) {
                audioDataByte = audioData[loopStartModifier++];
                index = offset++;
                samples[index] += audioDataByte * rightChannelVolume;
                index = offset++;
            }

            rawAudioStream.loopStartModifier = loopStartModifier << 8;
            return offset >> 1;
        }

        static int getFirstUnmodifiedMonoOffset(byte[] audioData, int[] samples, int loopStartModifier, int offset, int volume, int sampleLength, int startPosition, RawAudioStream rawAudioStream) {
            loopStartModifier >>= 8;
            startPosition >>= 8;
            volume <<= 2;
            int position;
            if ((position = offset + loopStartModifier - (startPosition - 1)) > sampleLength) {
                position = sampleLength;
            }

            int index;
            for (position -= 3; offset < position; samples[index] += audioData[loopStartModifier--] * volume) {
                index = offset++;
                samples[index] += audioData[loopStartModifier--] * volume;
                index = offset++;
                samples[index] += audioData[loopStartModifier--] * volume;
                index = offset++;
                samples[index] += audioData[loopStartModifier--] * volume;
                index = offset++;
            }

            for (position += 3; offset < position; samples[index] += audioData[loopStartModifier--] * volume) {
                index = offset++;
            }

            rawAudioStream.loopStartModifier = loopStartModifier << 8;
            return offset;
        }

        static int getFirstUnmodifiedStereoOffset(byte[] audioData, int[] samples, int loopStartModifier, int offset, int rightChannelVolume, int leftChannelVolume, int length, int startPosition, RawAudioStream rawAudioStream) {
            loopStartModifier >>= 8;
            startPosition >>= 8;
            rightChannelVolume <<= 2;
            leftChannelVolume <<= 2;
            int position;
            if ((position = loopStartModifier + offset - (startPosition - 1)) > length) {
                position = length;
            }

            offset <<= 1;
            position <<= 1;

            int index;
            byte audioDataByte;
            for (position -= 6; offset < position; samples[index] += audioDataByte * leftChannelVolume) {
                audioDataByte = audioData[loopStartModifier--];
                index = offset++;
                samples[index] += audioDataByte * rightChannelVolume;
                index = offset++;
                samples[index] += audioDataByte * leftChannelVolume;
                audioDataByte = audioData[loopStartModifier--];
                index = offset++;
                samples[index] += audioDataByte * rightChannelVolume;
                index = offset++;
                samples[index] += audioDataByte * leftChannelVolume;
                audioDataByte = audioData[loopStartModifier--];
                index = offset++;
                samples[index] += audioDataByte * rightChannelVolume;
                index = offset++;
                samples[index] += audioDataByte * leftChannelVolume;
                audioDataByte = audioData[loopStartModifier--];
                index = offset++;
                samples[index] += audioDataByte * rightChannelVolume;
                index = offset++;
            }

            for (position += 6; offset < position; samples[index] += audioDataByte * leftChannelVolume) {
                audioDataByte = audioData[loopStartModifier--];
                index = offset++;
                samples[index] += audioDataByte * rightChannelVolume;
                index = offset++;
            }

            rawAudioStream.loopStartModifier = loopStartModifier << 8;
            return offset >> 1;
        }

        static int getModifiedMonoEndOffset(byte[] audioData, int[] samples, int loopStartModifier, int offset, int volume, int length, int endPosition, RawAudioStream rawAudioStream, int pitch, int sampleLoopEnd) {
            int sampleLength;
            if (pitch == 0 || (sampleLength = offset + (pitch + (endPosition - loopStartModifier) - 257) / pitch) > length) {
                sampleLength = length;
            }

            byte audioDataByte;
            int position;
            int index;
            while (offset < sampleLength) {
                index = loopStartModifier >> 8;
                audioDataByte = audioData[index];
                position = offset++;
                samples[position] += ((audioDataByte << 8) + (audioData[index + 1] - audioDataByte) * (loopStartModifier & 255)) * volume >> 6;
                loopStartModifier += pitch;
            }

            if (pitch == 0 || (sampleLength = offset + (pitch + (endPosition - loopStartModifier) - 1) / pitch) > length) {
                sampleLength = length;
            }

            for (index = sampleLoopEnd; offset < sampleLength; loopStartModifier += pitch) {
                audioDataByte = audioData[loopStartModifier >> 8];
                position = offset++;
                samples[position] += ((audioDataByte << 8) + (index - audioDataByte) * (loopStartModifier & 255)) * volume >> 6;
            }

            rawAudioStream.loopStartModifier = loopStartModifier;
            return offset;
        }

        static int getModifiedStereoEndOffset(byte[] audioData, int[] samples, int loopStartModifier, int offset, int rightChannelVolume, int leftChannelVolume, int sampleLength, int endPosition, RawAudioStream rawAudioStream, int pitch, int sampleLoopEnd) {
            int length;
            if (pitch == 0 || (length = offset + (endPosition - loopStartModifier + pitch - 257) / pitch) > sampleLength) {
                length = sampleLength;
            }

            offset <<= 1;

            byte audioDataByte;
            int index;
            int audioLoopOffset;
            int position;
            for (length <<= 1; offset < length; loopStartModifier += pitch) {
                position = loopStartModifier >> 8;
                audioDataByte = audioData[position];
                audioLoopOffset = (audioDataByte << 8) + (loopStartModifier & 255) * (audioData[position + 1] - audioDataByte);
                index = offset++;
                samples[index] += audioLoopOffset * rightChannelVolume >> 6;
                index = offset++;
                samples[index] += audioLoopOffset * leftChannelVolume >> 6;
            }

            if (pitch == 0 || (length = (offset >> 1) + (endPosition - loopStartModifier + pitch - 1) / pitch) > sampleLength) {
                length = sampleLength;
            }

            length <<= 1;

            for (position = sampleLoopEnd; offset < length; loopStartModifier += pitch) {
                audioDataByte = audioData[loopStartModifier >> 8];
                audioLoopOffset = (audioDataByte << 8) + (position - audioDataByte) * (loopStartModifier & 255);
                index = offset++;
                samples[index] += audioLoopOffset * rightChannelVolume >> 6;
                index = offset++;
                samples[index] += audioLoopOffset * leftChannelVolume >> 6;
            }

            rawAudioStream.loopStartModifier = loopStartModifier;
            return offset >> 1;
        }

        static int getFirstModifiedMonoOffset(byte[] audioData, int[] samples, int loopStartModifier, int offset, int volume, int sampleLength, int startPosition, RawAudioStream rawAudioStream, int pitchOffset, int loopStart) {
            int position;
            if (pitchOffset == 0 || (position = offset + (pitchOffset + (startPosition + 256 - loopStartModifier)) / pitchOffset) > sampleLength) {
                position = sampleLength;
            }

            int index;
            int modifierIndex;
            while (offset < position) {
                modifierIndex = loopStartModifier >> 8;
                byte audioPositionByte = audioData[modifierIndex - 1];
                index = offset++;
                samples[index] += ((audioPositionByte << 8) + (audioData[modifierIndex] - audioPositionByte) * (loopStartModifier & 255)) * volume >> 6;
                loopStartModifier += pitchOffset;
            }

            if (pitchOffset == 0 || (position = offset + (pitchOffset + (startPosition - loopStartModifier)) / pitchOffset) > sampleLength) {
                position = sampleLength;
            }

            for (modifierIndex = pitchOffset; offset < position; loopStartModifier += modifierIndex) {
                index = offset++;
                samples[index] += ((loopStart << 8) + (audioData[loopStartModifier >> 8] - loopStart) * (loopStartModifier & 255)) * volume >> 6;
            }

            rawAudioStream.loopStartModifier = loopStartModifier;
            return offset;
        }

        static int getFirstModifiedStereoOffset(byte[] audioData, int[] samples, int loopStartModifier, int offset, int rightChannelVolume, int leftChannelVolume, int length, int startPosition, RawAudioStream rawAudioStream, int pitchOffset, int loopStartPosition) {
            int position;
            if (pitchOffset == 0 || (position = offset + (startPosition + 256 - loopStartModifier + pitchOffset) / pitchOffset) > length) {
                position = length;
            }

            offset <<= 1;

            int sampleOffset;
            int rightIndex;
            int index;
            for (position <<= 1; offset < position; loopStartModifier += pitchOffset) {
                index = loopStartModifier >> 8;
                byte audioPositionByte = audioData[index - 1];
                rightIndex = (audioData[index] - audioPositionByte) * (loopStartModifier & 255) + (audioPositionByte << 8);
                sampleOffset = offset++;
                samples[sampleOffset] += rightIndex * rightChannelVolume >> 6;
                sampleOffset = offset++;
                samples[sampleOffset] += rightIndex * leftChannelVolume >> 6;
            }

            if (pitchOffset == 0 || (position = (offset >> 1) + (startPosition - loopStartModifier + pitchOffset) / pitchOffset) > length) {
                position = length;
            }

            position <<= 1;

            for (index = loopStartPosition; offset < position; loopStartModifier += pitchOffset) {
                rightIndex = (index << 8) + (loopStartModifier & 255) * (audioData[loopStartModifier >> 8] - index);
                sampleOffset = offset++;
                samples[sampleOffset] += rightIndex * rightChannelVolume >> 6;
                sampleOffset = offset++;
                samples[sampleOffset] += rightIndex * leftChannelVolume >> 6;
            }

            rawAudioStream.loopStartModifier = loopStartModifier;
            return offset >> 1;
        }

        static int calculateUnmodifiedMonoOffset(byte[] audioData, int[] samples, int loopStartModifier, int offset, int volume, int overallVolume, int positionOffset, int endPosition, RawAudioStream rawAudioStream) {
            loopStartModifier >>= 8;
            endPosition >>= 8;
            volume <<= 2;
            overallVolume <<= 2;
            int position;
            if ((position = offset + endPosition - loopStartModifier) > positionOffset) {
                position = positionOffset;
            }

            rawAudioStream.rightChannelVolume += rawAudioStream.overallRightChannel * (position - offset);
            rawAudioStream.leftChannelVolume += rawAudioStream.overallLeftChannel * (position - offset);

            int index;
            for (position -= 3; offset < position; volume += overallVolume) {
                index = offset++;
                samples[index] += audioData[loopStartModifier++] * volume;
                volume += overallVolume;
                index = offset++;
                samples[index] += audioData[loopStartModifier++] * volume;
                volume += overallVolume;
                index = offset++;
                samples[index] += audioData[loopStartModifier++] * volume;
                volume += overallVolume;
                index = offset++;
                samples[index] += audioData[loopStartModifier++] * volume;
            }

            for (position += 3; offset < position; volume += overallVolume) {
                index = offset++;
                samples[index] += audioData[loopStartModifier++] * volume;
            }

            rawAudioStream.volume = volume >> 2;
            rawAudioStream.loopStartModifier = loopStartModifier << 8;
            return offset;
        }

        static int calculateUnmodifiedStereoOffset(byte[] audioData, int[] samples, int loopStartModifier, int offset, int rightChannelVolume, int leftChannelVolume, int overallRightChannelVolume, int overallLeftChannelVolume, int positionOffset, int endPosition, RawAudioStream rawAudioStream) {
            loopStartModifier >>= 8;
            endPosition >>= 8;
            rightChannelVolume <<= 2;
            leftChannelVolume <<= 2;
            overallRightChannelVolume <<= 2;
            overallLeftChannelVolume <<= 2;
            int position;
            if ((position = endPosition + offset - loopStartModifier) > positionOffset) {
                position = positionOffset;
            }

            rawAudioStream.volume += rawAudioStream.overallVolume * (position - offset);
            offset <<= 1;
            position <<= 1;

            byte audioDataByte;
            int index;
            for (position -= 6; offset < position; leftChannelVolume += overallLeftChannelVolume) {
                audioDataByte = audioData[loopStartModifier++];
                index = offset++;
                samples[index] += audioDataByte * rightChannelVolume;
                rightChannelVolume += overallRightChannelVolume;
                index = offset++;
                samples[index] += audioDataByte * leftChannelVolume;
                leftChannelVolume += overallLeftChannelVolume;
                audioDataByte = audioData[loopStartModifier++];
                index = offset++;
                samples[index] += audioDataByte * rightChannelVolume;
                rightChannelVolume += overallRightChannelVolume;
                index = offset++;
                samples[index] += audioDataByte * leftChannelVolume;
                leftChannelVolume += overallLeftChannelVolume;
                audioDataByte = audioData[loopStartModifier++];
                index = offset++;
                samples[index] += audioDataByte * rightChannelVolume;
                rightChannelVolume += overallRightChannelVolume;
                index = offset++;
                samples[index] += audioDataByte * leftChannelVolume;
                leftChannelVolume += overallLeftChannelVolume;
                audioDataByte = audioData[loopStartModifier++];
                index = offset++;
                samples[index] += audioDataByte * rightChannelVolume;
                rightChannelVolume += overallRightChannelVolume;
                index = offset++;
                samples[index] += audioDataByte * leftChannelVolume;
            }

            for (position += 6; offset < position; leftChannelVolume += overallLeftChannelVolume) {
                audioDataByte = audioData[loopStartModifier++];
                index = offset++;
                samples[index] += audioDataByte * rightChannelVolume;
                rightChannelVolume += overallRightChannelVolume;
                index = offset++;
                samples[index] += audioDataByte * leftChannelVolume;
            }

            rawAudioStream.rightChannelVolume = rightChannelVolume >> 2;
            rawAudioStream.leftChannelVolume = leftChannelVolume >> 2;
            rawAudioStream.loopStartModifier = loopStartModifier << 8;
            return offset >> 1;
        }

        static int getUnmodifiedLoopStartMonoOffset(byte[] audioData, int[] samples, int loopStartModifier, int offset, int volume, int overallVolume, int volumeOffset, int startPosition, RawAudioStream rawAudioStream) {
            loopStartModifier >>= 8;
            startPosition >>= 8;
            volume <<= 2;
            overallVolume <<= 2;
            int position;
            if ((position = offset + loopStartModifier - (startPosition - 1)) > volumeOffset) {
                position = volumeOffset;
            }

            rawAudioStream.rightChannelVolume += rawAudioStream.overallRightChannel * (position - offset);
            rawAudioStream.leftChannelVolume += rawAudioStream.overallLeftChannel * (position - offset);

            int index;
            for (position -= 3; offset < position; volume += overallVolume) {
                index = offset++;
                samples[index] += audioData[loopStartModifier--] * volume;
                volume += overallVolume;
                index = offset++;
                samples[index] += audioData[loopStartModifier--] * volume;
                volume += overallVolume;
                index = offset++;
                samples[index] += audioData[loopStartModifier--] * volume;
                volume += overallVolume;
                index = offset++;
                samples[index] += audioData[loopStartModifier--] * volume;
            }

            for (position += 3; offset < position; volume += overallVolume) {
                index = offset++;
                samples[index] += audioData[loopStartModifier--] * volume;
            }

            rawAudioStream.volume = volume >> 2;
            rawAudioStream.loopStartModifier = loopStartModifier << 8;
            return offset;
        }

        static int getUnmodifiedLoopStartStereoOffset(byte[] audioData, int[] samples, int loopStartModifier, int offset, int rightChannelVolume, int leftChannelVolume, int overallRightChannelVolume, int overallLeftChannelVolume, int volumeOffset, int startPosition, RawAudioStream rawAudioStream) {
            loopStartModifier >>= 8;
            startPosition >>= 8;
            rightChannelVolume <<= 2;
            leftChannelVolume <<= 2;
            overallRightChannelVolume <<= 2;
            overallLeftChannelVolume <<= 2;
            int position;
            if ((position = loopStartModifier + offset - (startPosition - 1)) > volumeOffset) {
                position = volumeOffset;
            }

            rawAudioStream.volume += rawAudioStream.overallVolume * (position - offset);
            offset <<= 1;
            position <<= 1;

            byte audioDataByte;
            int index;
            for (position -= 6; offset < position; leftChannelVolume += overallLeftChannelVolume) {
                audioDataByte = audioData[loopStartModifier--];
                index = offset++;
                samples[index] += audioDataByte * rightChannelVolume;
                rightChannelVolume += overallRightChannelVolume;
                index = offset++;
                samples[index] += audioDataByte * leftChannelVolume;
                leftChannelVolume += overallLeftChannelVolume;
                audioDataByte = audioData[loopStartModifier--];
                index = offset++;
                samples[index] += audioDataByte * rightChannelVolume;
                rightChannelVolume += overallRightChannelVolume;
                index = offset++;
                samples[index] += audioDataByte * leftChannelVolume;
                leftChannelVolume += overallLeftChannelVolume;
                audioDataByte = audioData[loopStartModifier--];
                index = offset++;
                samples[index] += audioDataByte * rightChannelVolume;
                rightChannelVolume += overallRightChannelVolume;
                index = offset++;
                samples[index] += audioDataByte * leftChannelVolume;
                leftChannelVolume += overallLeftChannelVolume;
                audioDataByte = audioData[loopStartModifier--];
                index = offset++;
                samples[index] += audioDataByte * rightChannelVolume;
                rightChannelVolume += overallRightChannelVolume;
                index = offset++;
                samples[index] += audioDataByte * leftChannelVolume;
            }

            for (position += 6; offset < position; leftChannelVolume += overallLeftChannelVolume) {
                audioDataByte = audioData[loopStartModifier--];
                index = offset++;
                samples[index] += audioDataByte * rightChannelVolume;
                rightChannelVolume += overallRightChannelVolume;
                index = offset++;
                samples[index] += audioDataByte * leftChannelVolume;
            }

            rawAudioStream.rightChannelVolume = rightChannelVolume >> 2;
            rawAudioStream.leftChannelVolume = leftChannelVolume >> 2;
            rawAudioStream.loopStartModifier = loopStartModifier << 8;
            return offset >> 1;
        }

        static int calculateModifiedMonoOffset(byte[] audioData, int[] samples, int loopStartModifier, int offset, int volume, int overallVolume, int positionOffset, int endPosition, RawAudioStream rawAudioStream, int pitch, int sampleLoopEnd) {
            rawAudioStream.rightChannelVolume -= rawAudioStream.overallRightChannel * offset;
            rawAudioStream.leftChannelVolume -= rawAudioStream.overallLeftChannel * offset;
            int position;
            if (pitch == 0 || (position = offset + (endPosition - loopStartModifier + pitch - 257) / pitch) > positionOffset) {
                position = positionOffset;
            }

            byte audioDataByte;
            int sampleIndex;
            int audioIndex;
            while (offset < position) {
                audioIndex = loopStartModifier >> 8;
                audioDataByte = audioData[audioIndex];
                sampleIndex = offset++;
                samples[sampleIndex] += ((audioDataByte << 8) + (audioData[audioIndex + 1] - audioDataByte) * (loopStartModifier & 255)) * volume >> 6;
                volume += overallVolume;
                loopStartModifier += pitch;
            }

            if (pitch == 0 || (position = offset + (endPosition - loopStartModifier + pitch - 1) / pitch) > positionOffset) {
                position = positionOffset;
            }

            for (audioIndex = sampleLoopEnd; offset < position; loopStartModifier += pitch) {
                audioDataByte = audioData[loopStartModifier >> 8];
                sampleIndex = offset++;
                samples[sampleIndex] += ((audioDataByte << 8) + (audioIndex - audioDataByte) * (loopStartModifier & 255)) * volume >> 6;
                volume += overallVolume;
            }

            rawAudioStream.rightChannelVolume += rawAudioStream.overallRightChannel * offset;
            rawAudioStream.leftChannelVolume += rawAudioStream.overallLeftChannel * offset;
            rawAudioStream.volume = volume;
            rawAudioStream.loopStartModifier = loopStartModifier;
            return offset;
        }

        static int calculateModifiedStereoOffset(byte[] audioData, int[] samples, int loopStartModifier, int offset, int rightChannelVolume, int leftChannelVolume, int overallRightChannelVolume, int overallLeftChannelVolume, int positionOffset, int endPosition, RawAudioStream rawAudioStream, int pitch, int sampleLoopEnd) {
            rawAudioStream.volume -= offset * rawAudioStream.overallVolume;
            int position;
            if (pitch == 0 || (position = offset + (endPosition - loopStartModifier + pitch - 257) / pitch) > positionOffset) {
                position = positionOffset;
            }

            offset <<= 1;

            byte audioDataByte;
            int sampleIndex;
            int audioOffset;
            int audioIndex;
            for (position <<= 1; offset < position; loopStartModifier += pitch) {
                audioIndex = loopStartModifier >> 8;
                audioDataByte = audioData[audioIndex];
                audioOffset = (audioDataByte << 8) + (loopStartModifier & 255) * (audioData[audioIndex + 1] - audioDataByte);
                sampleIndex = offset++;
                samples[sampleIndex] += audioOffset * rightChannelVolume >> 6;
                rightChannelVolume += overallRightChannelVolume;
                sampleIndex = offset++;
                samples[sampleIndex] += audioOffset * leftChannelVolume >> 6;
                leftChannelVolume += overallLeftChannelVolume;
            }

            if (pitch == 0 || (position = (offset >> 1) + (endPosition - loopStartModifier + pitch - 1) / pitch) > positionOffset) {
                position = positionOffset;
            }

            position <<= 1;

            for (audioIndex = sampleLoopEnd; offset < position; loopStartModifier += pitch) {
                audioDataByte = audioData[loopStartModifier >> 8];
                audioOffset = (audioDataByte << 8) + (audioIndex - audioDataByte) * (loopStartModifier & 255);
                sampleIndex = offset++;
                samples[sampleIndex] += audioOffset * rightChannelVolume >> 6;
                rightChannelVolume += overallRightChannelVolume;
                sampleIndex = offset++;
                samples[sampleIndex] += audioOffset * leftChannelVolume >> 6;
                leftChannelVolume += overallLeftChannelVolume;
            }

            offset >>= 1;
            rawAudioStream.volume += rawAudioStream.overallVolume * offset;
            rawAudioStream.rightChannelVolume = rightChannelVolume;
            rawAudioStream.leftChannelVolume = leftChannelVolume;
            rawAudioStream.loopStartModifier = loopStartModifier;
            return offset;
        }

        static int getModifiedLoopStartMonoOffset(byte[] audioData, int[] samples, int loopStartModifier, int offset, int volume, int overallVolume, int volumeOffset, int startPosition, RawAudioStream rawAudioStream, int pitch, int sampleLoopStart) {
            rawAudioStream.rightChannelVolume -= rawAudioStream.overallRightChannel * offset;
            rawAudioStream.leftChannelVolume -= rawAudioStream.overallLeftChannel * offset;
            int position;
            if (pitch == 0 || (position = offset + (startPosition + 256 - loopStartModifier + pitch) / pitch) > volumeOffset) {
                position = volumeOffset;
            }

            int index;
            int audioIndex;
            while (offset < position) {
                audioIndex = loopStartModifier >> 8;
                byte audioDataByte = audioData[audioIndex - 1];
                index = offset++;
                samples[index] += ((audioDataByte << 8) + (audioData[audioIndex] - audioDataByte) * (loopStartModifier & 255)) * volume >> 6;
                volume += overallVolume;
                loopStartModifier += pitch;
            }

            if (pitch == 0 || (position = offset + (startPosition - loopStartModifier + pitch) / pitch) > volumeOffset) {
                position = volumeOffset;
            }

            for (audioIndex = pitch; offset < position; loopStartModifier += audioIndex) {
                index = offset++;
                samples[index] += ((sampleLoopStart << 8) + (audioData[loopStartModifier >> 8] - sampleLoopStart) * (loopStartModifier & 255)) * volume >> 6;
                volume += overallVolume;
            }

            rawAudioStream.rightChannelVolume += rawAudioStream.overallRightChannel * offset;
            rawAudioStream.leftChannelVolume += rawAudioStream.overallLeftChannel * offset;
            rawAudioStream.volume = volume;
            rawAudioStream.loopStartModifier = loopStartModifier;
            return offset;
        }

        static int getModifiedLoopStartStereoOffset(byte[] audioData, int[] samples, int loopStartModifier, int offset, int rightChannelVolume, int leftChannelVolume, int overallRightChannelVolume, int overallLeftChannelVolume, int volumeOffset, int startPosition, RawAudioStream rawAudioStream, int pitch, int loopStartPosition) {
            rawAudioStream.volume -= offset * rawAudioStream.overallVolume;
            int volume;
            if (pitch == 0 || (volume = offset + (startPosition + 256 - loopStartModifier + pitch) / pitch) > volumeOffset) {
                volume = volumeOffset;
            }

            offset <<= 1;

            int sampleIndex;
            int audioPosition;
            int audioIndex;
            for (volume <<= 1; offset < volume; loopStartModifier += pitch) {
                audioIndex = loopStartModifier >> 8;
                byte audioDataByte = audioData[audioIndex - 1];
                audioPosition = (audioData[audioIndex] - audioDataByte) * (loopStartModifier & 255) + (audioDataByte << 8);
                sampleIndex = offset++;
                samples[sampleIndex] += audioPosition * rightChannelVolume >> 6;
                rightChannelVolume += overallRightChannelVolume;
                sampleIndex = offset++;
                samples[sampleIndex] += audioPosition * leftChannelVolume >> 6;
                leftChannelVolume += overallLeftChannelVolume;
            }

            if (pitch == 0 || (volume = (offset >> 1) + (startPosition - loopStartModifier + pitch) / pitch) > volumeOffset) {
                volume = volumeOffset;
            }

            volume <<= 1;

            for (audioIndex = loopStartPosition; offset < volume; loopStartModifier += pitch) {
                audioPosition = (audioIndex << 8) + (loopStartModifier & 255) * (audioData[loopStartModifier >> 8] - audioIndex);
                sampleIndex = offset++;
                samples[sampleIndex] += audioPosition * rightChannelVolume >> 6;
                rightChannelVolume += overallRightChannelVolume;
                sampleIndex = offset++;
                samples[sampleIndex] += audioPosition * leftChannelVolume >> 6;
                leftChannelVolume += overallLeftChannelVolume;
            }

            offset >>= 1;
            rawAudioStream.volume += rawAudioStream.overallVolume * offset;
            rawAudioStream.rightChannelVolume = rightChannelVolume;
            rawAudioStream.leftChannelVolume = leftChannelVolume;
            rawAudioStream.loopStartModifier = loopStartModifier;
            return offset;
        }
    }

    /**
     * A class which holds the data and information for the Sound Bank's individual Music Patch, essentially an instrument.
     */
    static class MusicPatch {

        /**
         * An integer value for the overall volume of this music patch.
         */
        public int volume;

        /**
         * An array of the samples used in this music patch.
         */
        public AudioDataSource[] audioDataSources;

        /**
         * An array of short values containing the precise root pitches for each sample.
         */
        public short[] pitchOffset;

        /**
         * An array of byte values containing the default volume for each sample.
         */
        public byte[] volumeOffset;

        /**
         * An array of byte values containing the default pan data for each sample.
         */
        public byte[] panOffset;

        /**
         * An array of envelope values for each sample in the music patch.
         */
        public MusicPatchEnvelope[] musicPatchEnvelopes;

        /**
         * An array of byte values containing the default loop mode for each sample.
         */
        public byte[] loopOffset;

        /**
         * A HashMap of all the available samples. Used to check for if a sample already exists, to avoid loading duplicates.
         */
        public HashMap<AudioDataSource, String> availableSources;

        /**
         * A method that reads a text file and maps the values, constructing a music patch.
         *
         * @param inputStream The input stream of a loaded .txt file resource to be read.
         */
        public MusicPatch(InputStream inputStream) {

            this.audioDataSources = new AudioDataSource[128];
            this.pitchOffset = new short[128];
            this.volumeOffset = new byte[128];
            this.panOffset = new byte[128];
            this.musicPatchEnvelopes = new MusicPatchEnvelope[128];
            this.loopOffset = new byte[128];

            this.availableSources = new HashMap<>();

            List<String> lines = new BufferedReader(new InputStreamReader(inputStream)).lines().collect(Collectors.toList());
            for (String line : lines) {
                String[] values = line.split(";");
                if (values[0].equals(values[1])) {
                    int index = Integer.parseInt(values[0]);
                    if (!availableSources.containsValue(values[2]) && !values[2].equals("-1")) {
                        this.audioDataSources[index] = new AudioDataSource(values[2]);
                        availableSources.put(this.audioDataSources[index], values[2]);
                    } else {
                        for (Map.Entry<AudioDataSource, String> entry : availableSources.entrySet()) {
                            if (entry.getValue().equals(values[2])) {
                                this.audioDataSources[index] = entry.getKey();
                            }
                        }
                    }
                    this.pitchOffset[index] = (short) Integer.parseInt(values[3]);//(short) ((((Integer.parseInt(values[3]) * 256)) + Integer.parseInt(values[4])) - Short.MIN_VALUE);
                    this.volume = Integer.parseInt(values[5]);
                    this.volumeOffset[index] = (byte) Integer.parseInt(values[6]);
                    this.panOffset[index] = (byte) Integer.parseInt(values[7]);

                    this.musicPatchEnvelopes[index] = new MusicPatchEnvelope();

                    this.musicPatchEnvelopes[index].mainEnvelope = Integer.parseInt(values[8]);
                    this.musicPatchEnvelopes[index].fadeOut = Integer.parseInt(values[9]);
                    this.musicPatchEnvelopes[index].releaseEnvelope = Integer.parseInt(values[10]);
                    this.musicPatchEnvelopes[index].highNoteFadeOut = Integer.parseInt(values[11]);
                    this.musicPatchEnvelopes[index].vibratoSpeed = Integer.parseInt(values[12]);
                    this.musicPatchEnvelopes[index].vibratoDepth = Integer.parseInt(values[13]);
                    this.musicPatchEnvelopes[index].vibratoWarmUp = Integer.parseInt(values[14]);

                    String[] envelope0 = values[15].replace("[", "").replace("]", "").replace(" ", "").split(",");
                    String[] envelope1 = values[16].replace("[", "").replace("]", "").replace(" ", "").split(",");

                    this.musicPatchEnvelopes[index].main = new byte[envelope0.length];
                    this.musicPatchEnvelopes[index].release = new byte[envelope1.length];

                    for (int stringIndex = 0; stringIndex < envelope0.length; stringIndex++) {
                        if (!envelope0[stringIndex].equals("null")) {
                            this.musicPatchEnvelopes[index].main[stringIndex] = Byte.parseByte(envelope0[stringIndex]);
                        } else {
                            this.musicPatchEnvelopes[index].main = null;
                        }
                    }

                    for (int stringIndex = 0; stringIndex < envelope1.length; stringIndex++) {
                        if (!envelope1[stringIndex].equals("null")) {
                            this.musicPatchEnvelopes[index].release[stringIndex] = Byte.parseByte(envelope1[stringIndex]);
                        } else {
                            this.musicPatchEnvelopes[index].release = null;
                        }
                    }

                    if (this.audioDataSources[index] != null) {
                        if (this.audioDataSources[index].isLooping) {
                            this.loopOffset[index] = -1;
                        } else {
                            this.loopOffset[index] = 0;
                        }
                    }
                } else {
                    for (int index = Integer.parseInt(values[0]); index < Integer.parseInt(values[1]) + 1; index++) {
                        if (!availableSources.containsValue(values[2]) && !values[2].equals("-1")) {
                            this.audioDataSources[index] = new AudioDataSource(values[2]);
                            availableSources.put(this.audioDataSources[index], values[2]);
                        } else {
                            for (Map.Entry<AudioDataSource, String> entry : availableSources.entrySet()) {
                                if (entry.getValue().equals(values[2])) {
                                    this.audioDataSources[index] = entry.getKey();
                                }
                            }
                        }
                        this.pitchOffset[index] = (short) ((((Integer.parseInt(values[3]) * 256)) + Integer.parseInt(values[4])) - Short.MIN_VALUE);
                        this.volume = Integer.parseInt(values[5]);
                        this.volumeOffset[index] = (byte) Integer.parseInt(values[6]);
                        this.panOffset[index] = (byte) Integer.parseInt(values[7]);

                        this.musicPatchEnvelopes[index] = new MusicPatchEnvelope();

                        this.musicPatchEnvelopes[index].mainEnvelope = Integer.parseInt(values[8]);
                        this.musicPatchEnvelopes[index].fadeOut = Integer.parseInt(values[9]);
                        this.musicPatchEnvelopes[index].releaseEnvelope = Integer.parseInt(values[10]);
                        this.musicPatchEnvelopes[index].highNoteFadeOut = Integer.parseInt(values[11]);
                        this.musicPatchEnvelopes[index].vibratoSpeed = Integer.parseInt(values[12]);
                        this.musicPatchEnvelopes[index].vibratoDepth = Integer.parseInt(values[13]);
                        this.musicPatchEnvelopes[index].vibratoWarmUp = Integer.parseInt(values[14]);

                        String[] envelope0 = values[15].replace("[", "").replace("]", "").replace(" ", "").split(",");
                        String[] envelope1 = values[16].replace("[", "").replace("]", "").replace(" ", "").split(",");

                        this.musicPatchEnvelopes[index].main = new byte[envelope0.length];
                        this.musicPatchEnvelopes[index].release = new byte[envelope1.length];

                        for (int stringIndex = 0; stringIndex < envelope0.length; stringIndex++) {
                            if (!envelope0[stringIndex].equals("null")) {
                                this.musicPatchEnvelopes[index].main[stringIndex] = Byte.parseByte(envelope0[stringIndex]);
                            } else {
                                this.musicPatchEnvelopes[index].main = null;
                            }
                        }

                        for (int stringIndex = 0; stringIndex < envelope1.length; stringIndex++) {
                            if (!envelope1[stringIndex].equals("null")) {
                                this.musicPatchEnvelopes[index].release[stringIndex] = Byte.parseByte(envelope1[stringIndex]);
                            } else {
                                this.musicPatchEnvelopes[index].release = null;
                            }
                        }

                        if (this.audioDataSources[index] != null) {
                            if (this.audioDataSources[index].isLooping) {
                                this.loopOffset[index] = -1;
                            } else {
                                this.loopOffset[index] = -1;
                            }
                        }
                    }
                }
            }
        }

    }

    /**
     * A class which holds the data and variables for an audio source, which can be used in music and sound effects.
     */
    static class AudioDataSource {

        /**
         * A byte array containing the raw audio in 8-bit signed little-endian format.
         */
        public byte[] audioData;

        /**
         * An integer value for the raw audio sample rate.
         */
        public int sampleRate;

        /**
         * An integer value for the sample loop start position in the raw audio.
         */
        public int loopStart;

        /**
         * An integer value for the sample loop end position in the raw audio.
         */
        public int loopEnd;

        /**
         * A boolean value to determine whether the sample should loop or not.
         */
        public boolean isLooping;

        /**
         * A method to load .ogg file resources by name, decoding them to raw 8-bit audio for use.
         *
         * @param audioName The name of the audio resource to load.
         */
        public AudioDataSource(String audioName) {
            try {
                File file = new File("OSRS.zip");
                ZipFile zipFile = new ZipFile(file);
                ZipEntry zipEntry = zipFile.getEntry(audioName + ".raw");
                InputStream inputStream = zipFile.getInputStream(zipEntry);

                byte[] data = new byte[(int) zipEntry.getSize()];
                for (int index = 0; index < data.length; index++) {
                    data[index] = (byte) inputStream.read();
                }

                try {
                    loadAudioSource(data, audioName);
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void loadAudioSource(byte[] data, String audioName) throws FileNotFoundException {
            File file = new File("OSRS Loops.txt");
            Scanner scanner = new Scanner(file);
            while (scanner.hasNextLine()) {
                String nextLine = scanner.nextLine();
                if (nextLine.split(",")[0].equals(audioName)) {
                    sampleRate = 44100;
                    loopStart = Integer.parseInt(nextLine.split(",")[2]) * 2;
                    loopEnd = Integer.parseInt(nextLine.split(",")[3]) * 2;
                    isLooping = loopEnd != 0 || loopStart != 0;
                    audioData = data;
                }
            }
        }
    }

    public static final class NodeHashTable {

        int size;
        Node[] buckets;
        Node currentGet;
        Node current;
        int index;

        public NodeHashTable(int var1) {
            this.index = 0;
            this.size = var1;
            this.buckets = new Node[var1];

            for (int var2 = 0; var2 < var1; ++var2) {
                Node var3 = this.buckets[var2] = new Node();
                var3.previous = var3;
                var3.next = var3;
            }

        }

        public Node get(long var1) {
            Node var3 = this.buckets[(int)(var1 & (long)(this.size - 1))];

            for (this.currentGet = var3.previous; var3 != this.currentGet; this.currentGet = this.currentGet.previous) {
                if (this.currentGet.key == var1) {
                    Node var4 = this.currentGet;
                    this.currentGet = this.currentGet.previous;
                    return var4;
                }
            }

            this.currentGet = null;
            return null;
        }

        public void put(Node var1, long var2) {
            if (var1.next != null) {
                var1.remove();
            }

            Node var4 = this.buckets[(int)(var2 & (long)(this.size - 1))];
            var1.next = var4.next;
            var1.previous = var4;
            var1.next.previous = var1;
            var1.previous.next = var1;
            var1.key = var2;
        }

        public Node first() {
            this.index = 0;
            return this.next();
        }

        public Node next() {
            Node var1;
            if (this.index > 0 && this.buckets[this.index - 1] != this.current) {
                var1 = this.current;
            } else {
                do {
                    if (this.index >= this.size) {
                        return null;
                    }

                    var1 = this.buckets[this.index++].previous;
                } while(var1 == this.buckets[this.index - 1]);

            }
            this.current = var1.previous;
            return var1;
        }
    }

    static class MusicTrack extends Node {

        public NodeHashTable table;
        public byte[] midi;

        MusicTrack(byte[] midiData) {
            midi = midiData;
        }

        public void readMidiTrack() {
            if (this.table == null) { // L: 274
                this.table = new NodeHashTable(16); // L: 275
                int[] var1 = new int[16]; // L: 276
                int[] var2 = new int[16]; // L: 277
                var2[9] = 128; // L: 279
                var1[9] = 128; // L: 280
                MidiFileReader var4 = new MidiFileReader(this.midi); // L: 281
                int var5 = var4.trackCount(); // L: 282

                int var6;
                for (var6 = 0; var6 < var5; ++var6) { // L: 283
                    var4.gotoTrack(var6); // L: 284
                    var4.readTrackLength(var6); // L: 285
                    var4.markTrackPosition(var6); // L: 286
                }

                label56:
                do {
                    while (true) {
                        var6 = var4.getPrioritizedTrack(); // L: 289
                        int var7 = var4.trackLengths[var6]; // L: 290

                        while (var7 == var4.trackLengths[var6]) { // L: 291
                            var4.gotoTrack(var6); // L: 292
                            int var8 = var4.readMessage(var6); // L: 293
                            if (var8 == 1) { // L: 294
                                var4.setTrackDone(); // L: 295
                                var4.markTrackPosition(var6); // L: 296
                                continue label56;
                            }

                            int var9 = var8 & 240; // L: 300
                            int var10;
                            int var11;
                            int var12;
                            if (var9 == 176) { // L: 301
                                var10 = var8 & 15; // L: 302
                                var11 = var8 >> 8 & 127; // L: 303
                                var12 = var8 >> 16 & 127; // L: 304
                                if (var11 == 0) { // L: 305
                                    var1[var10] = (var12 << 14) + (var1[var10] & -2080769);
                                }

                                if (var11 == 32) { // L: 306
                                    var1[var10] = (var1[var10] & -16257) + (var12 << 7);
                                }
                            }

                            if (var9 == 192) { // L: 308
                                var10 = var8 & 15; // L: 309
                                var11 = var8 >> 8 & 127; // L: 310
                                var2[var10] = var11 + var1[var10]; // L: 311
                            }

                            if (var9 == 144) { // L: 313
                                var10 = var8 & 15; // L: 314
                                var11 = var8 >> 8 & 127; // L: 315
                                var12 = var8 >> 16 & 127; // L: 316
                                if (var12 > 0) { // L: 317
                                    int var13 = var2[var10]; // L: 318
                                    ByteArrayNode var14 = (ByteArrayNode) this.table.get(var13); // L: 319
                                    if (var14 == null) { // L: 320
                                        var14 = new ByteArrayNode(new byte[128]); // L: 321
                                        this.table.put(var14, var13); // L: 322
                                    }

                                    var14.byteArray[var11] = 1; // L: 324
                                }
                            }

                            var4.readTrackLength(var6); // L: 327
                            var4.markTrackPosition(var6); // L: 328
                        }
                    }
                } while (!var4.isDone()); // L: 297

            }
        } // L: 331

        public void clear() {
            this.table = null; // L: 334
        } // L: 335

    }

    static class ByteArrayNode extends Node {

        public byte[] byteArray;

        public ByteArrayNode(byte[] bytes) {
            this.byteArray = bytes;
        }
    }

    static class MidiFileReader {

        static final byte[] field2963;
        Buffer buffer;
        public int resolution;
        int[] trackStarts;
        int[] trackPositions;
        public int[] trackLengths;
        int[] midiMessage;
        int division;
        long tick;

        static {
            field2963 = new byte[]{2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 0, 1, 2, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        }

        MidiFileReader(byte[] var1) {
            this.buffer = new Buffer(null);
            this.parse(var1);
        }

        public MidiFileReader() {
            this.buffer = new Buffer(null);
        }

        public void parse(byte[] data) {
            this.buffer.array = data;
            this.buffer.offset = 10;
            int var2 = this.buffer.readUnsignedShort();
            this.resolution = this.buffer.readUnsignedShort();
            this.division = 500000;
            this.trackStarts = new int[var2];

            Buffer var10000;
            int var3;
            int var5;
            for (var3 = 0; var3 < var2; var10000.offset += var5) { // L: 39
                int var4 = this.buffer.readInt(); // L: 40
                var5 = this.buffer.readInt(); // L: 41
                if (var4 == 1297379947) { // L: 42
                    this.trackStarts[var3] = this.buffer.offset; // L: 43
                    ++var3; // L: 44
                }

                var10000 = this.buffer; // L: 46
            }

            this.tick = 0L; // L: 48
            this.trackPositions = new int[var2]; // L: 49

            for (var3 = 0; var3 < var2; ++var3) { // L: 50
                this.trackPositions[var3] = this.trackStarts[var3];
            }

            this.trackLengths = new int[var2]; // L: 51
            this.midiMessage = new int[var2]; // L: 52
        } // L: 53

        public void clear() {
            this.buffer.array = null; // L: 56
            this.trackStarts = null; // L: 57
            this.trackPositions = null; // L: 58
            this.trackLengths = null; // L: 59
            this.midiMessage = null; // L: 60
        } // L: 61

        public boolean isReady() {
            return this.buffer.array != null; // L: 64
        }

        public int trackCount() {
            return this.trackPositions.length; // L: 68
        }

        public void gotoTrack(int var1) {
            this.buffer.offset = this.trackPositions[var1]; // L: 72
        } // L: 73

        public void markTrackPosition(int var1) {
            this.trackPositions[var1] = this.buffer.offset; // L: 76
        } // L: 77

        public void setTrackDone() {
            this.buffer.offset = -1; // L: 80
        } // L: 81

        public void readTrackLength(int var1) {
            int var2 = this.buffer.readVarInt(); // L: 84
            int[] var10000 = this.trackLengths;
            var10000[var1] += var2; // L: 85
        } // L: 86

        public int readMessage(int var1) {
            int var2 = this.readMessage0(var1); // L: 89
            return var2; // L: 90
        }

        int readMessage0(int var1) {
            byte var2 = this.buffer.array[this.buffer.offset]; // L: 94
            int var5;
            if (var2 < 0) { // L: 95
                var5 = var2 & 255; // L: 96
                this.midiMessage[var1] = var5; // L: 97
                ++this.buffer.offset; // L: 98
            } else {
                var5 = this.midiMessage[var1]; // L: 101
            }

            if (var5 != 240 && var5 != 247) { // L: 103
                return this.method4891(var1, var5); // L: 116
            } else {
                int var3 = this.buffer.readVarInt(); // L: 104
                if (var5 == 247 && var3 > 0) { // L: 105
                    int var4 = this.buffer.array[this.buffer.offset] & 255; // L: 106
                    if (var4 >= 241 && var4 <= 243 || var4 == 246 || var4 == 248 || var4 >= 250 && var4 <= 252 || var4 == 254) { // L: 107
                        ++this.buffer.offset; // L: 108
                        this.midiMessage[var1] = var4; // L: 109
                        return this.method4891(var1, var4); // L: 110
                    }
                }

                Buffer var10000 = this.buffer; // L: 113
                var10000.offset += var3;
                return 0; // L: 114
            }
        }

        int method4891(int var1, int var2) {
            int var4;
            if (var2 == 255) { // L: 120
                int var7 = this.buffer.readUnsignedByte(); // L: 121
                var4 = this.buffer.readVarInt(); // L: 122
                Buffer var10000;
                if (var7 == 47) { // L: 123
                    var10000 = this.buffer; // L: 124
                    var10000.offset += var4;
                    return 1; // L: 125
                } else if (var7 == 81) { // L: 127
                    int var5 = this.buffer.readMedium(); // L: 128
                    var4 -= 3; // L: 129
                    int var6 = this.trackLengths[var1]; // L: 130
                    this.tick += (long) var6 * (long) (this.division - var5); // L: 131
                    this.division = var5; // L: 132
                    var10000 = this.buffer; // L: 133
                    var10000.offset += var4;
                    return 2; // L: 134
                } else {
                    var10000 = this.buffer; // L: 136
                    var10000.offset += var4;
                    return 3; // L: 137
                }
            } else {
                byte var3 = field2963[var2 - 128]; // L: 139
                var4 = var2; // L: 140
                if (var3 >= 1) { // L: 141
                    var4 = var2 | this.buffer.readUnsignedByte() << 8;
                }

                if (var3 >= 2) { // L: 142
                    var4 |= this.buffer.readUnsignedByte() << 16;
                }

                return var4; // L: 143
            }
        }

        public long method4934(int var1) {
            return this.tick + (long) var1 * (long) this.division; // L: 147
        }

        public int getPrioritizedTrack() {
            int var1 = this.trackPositions.length; // L: 151
            int var2 = -1; // L: 152
            int var3 = Integer.MAX_VALUE; // L: 153

            for (int var4 = 0; var4 < var1; ++var4) { // L: 154
                if (this.trackPositions[var4] >= 0 && this.trackLengths[var4] < var3) { // L: 155 156
                    var2 = var4; // L: 157
                    var3 = this.trackLengths[var4]; // L: 158
                }
            }

            return var2; // L: 161
        }

        public boolean isDone() {
            if (this.trackPositions != null) {
                int var1 = this.trackPositions.length; // L: 165

                for (int var2 = 0; var2 < var1; ++var2) { // L: 166
                    if (this.trackPositions[var2] >= 0) {
                        return false;
                    }
                }

                return true; // L: 167
            }
            return false;
        }

        public void reset(long var1) {
            this.tick = var1; // L: 171
            int var3 = this.trackPositions.length; // L: 172

            for (int var4 = 0; var4 < var3; ++var4) { // L: 173
                this.trackLengths[var4] = 0; // L: 174
                this.midiMessage[var4] = 0; // L: 175
                this.buffer.offset = this.trackStarts[var4]; // L: 176
                this.readTrackLength(var4); // L: 177
                this.trackPositions[var4] = this.buffer.offset; // L: 178
            }

        } // L: 180
    }

    static class Floor0 extends FuncFloor {

        void pack(Object i, VorbisBuffer opb){
            Floor0.InfoFloor0 info=(Floor0.InfoFloor0)i;
            opb.write(info.order, 8);
            opb.write(info.rate, 16);
            opb.write(info.barkmap, 16);
            opb.write(info.ampbits, 6);
            opb.write(info.ampdB, 8);
            opb.write(info.numbooks-1, 4);
            for(int j=0; j<info.numbooks; j++)
                opb.write(info.books[j], 8);
        }

        Object unpack(Info vi, VorbisBuffer opb){
            Floor0.InfoFloor0 info=new Floor0.InfoFloor0();
            info.order=opb.read(8);
            info.rate=opb.read(16);
            info.barkmap=opb.read(16);
            info.ampbits=opb.read(6);
            info.ampdB=opb.read(8);
            info.numbooks=opb.read(4)+1;

            if((info.order<1)||(info.rate<1)||(info.barkmap<1)||(info.numbooks<1)){
                return (null);
            }

            for(int j=0; j<info.numbooks; j++){
                info.books[j]=opb.read(8);
                if(info.books[j]<0||info.books[j]>=vi.books){
                    return (null);
                }
            }
            return (info);
        }

        Object look(DspState vd, InfoMode mi, Object i){
            float scale;
            Info vi=vd.vi;
            Floor0.InfoFloor0 info=(Floor0.InfoFloor0)i;
            Floor0.LookFloor0 look=new Floor0.LookFloor0();
            look.m=info.order;
            look.n=vi.blocksizes[mi.blockflag]/2;
            look.ln=info.barkmap;
            look.vi=info;
            look.lpclook.init(look.ln, look.m);

            // we choose a scaling constant so that:
            scale=look.ln/toBARK((float)(info.rate/2.));

            // the mapping from a linear scale to a smaller bark scale is
            // straightforward.  We do *not* make sure that the linear mapping
            // does not skip bark-scale bins; the decoder simply skips them and
            // the encoder may do what it wishes in filling them.  They're
            // necessary in some mapping combinations to keep the scale spacing
            // accurate
            look.linearmap=new int[look.n];
            for(int j=0; j<look.n; j++){
                int val=(int)Math.floor(toBARK((float)((info.rate/2.)/look.n*j))*scale); // bark numbers represent band edges
                if(val>=look.ln)
                    val=look.ln; // guard against the approximation
                look.linearmap[j]=val;
            }
            return look;
        }

        static float toBARK(float f){
            return (float)(13.1*Math.atan(.00074*(f))+2.24*Math.atan((f)*(f)*1.85e-8)+1e-4*(f));
        }

        Object state(Object i){
            Floor0.EchstateFloor0 state=new Floor0.EchstateFloor0();
            Floor0.InfoFloor0 info=(Floor0.InfoFloor0)i;

            // a safe size if usually too big (dim==1)
            state.codewords=new int[info.order];
            state.curve=new float[info.barkmap];
            state.frameno=-1;
            return (state);
        }

        void free_info(Object i){
        }

        void free_look(Object i){
        }

        void free_state(Object vs){
        }

        int forward(Block vb, Object i, float[] in, float[] out, Object vs){
            return 0;
        }

        float[] lsp=null;

        int inverse(Block vb, Object i, float[] out){
            //System.err.println("Floor0.inverse "+i.getClass()+"]");
            Floor0.LookFloor0 look=(Floor0.LookFloor0)i;
            Floor0.InfoFloor0 info=look.vi;
            int ampraw=vb.opb.read(info.ampbits);
            if(ampraw>0){ // also handles the -1 out of data case
                int maxval=(1<<info.ampbits)-1;
                float amp=(float)ampraw/maxval*info.ampdB;
                int booknum=vb.opb.read(Util.ilog(info.numbooks));

                if(booknum!=-1&&booknum<info.numbooks){

                    synchronized(this){
                        if(lsp==null||lsp.length<look.m){
                            lsp=new float[look.m];
                        }
                        else{
                            for(int j=0; j<look.m; j++)
                                lsp[j]=0.f;
                        }

                        CodeBook b=vb.vd.fullbooks[info.books[booknum]];
                        float last=0.f;

                        for(int j=0; j<look.m; j++)
                            out[j]=0.0f;

                        for(int j=0; j<look.m; j+=b.dim){
                            if(b.decodevs(lsp, j, vb.opb, 1, -1)==-1){
                                for(int k=0; k<look.n; k++)
                                    out[k]=0.0f;
                                return (0);
                            }
                        }
                        for(int j=0; j<look.m;){
                            for(int k=0; k<b.dim; k++, j++)
                                lsp[j]+=last;
                            last=lsp[j-1];
                        }
                        // take the coefficients back to a spectral envelope curve
                        Lsp.lsp_to_curve(out, look.linearmap, look.n, look.ln, lsp, look.m,
                                amp, info.ampdB);

                        return (1);
                    }
                }
            }
            return (0);
        }

        Object inverse1(Block vb, Object i, Object memo){
            Floor0.LookFloor0 look=(Floor0.LookFloor0)i;
            Floor0.InfoFloor0 info=look.vi;
            float[] lsp=null;
            if(memo instanceof float[]){
                lsp=(float[])memo;
            }

            int ampraw=vb.opb.read(info.ampbits);
            if(ampraw>0){ // also handles the -1 out of data case
                int maxval=(1<<info.ampbits)-1;
                float amp=(float)ampraw/maxval*info.ampdB;
                int booknum=vb.opb.read(Util.ilog(info.numbooks));

                if(booknum!=-1&&booknum<info.numbooks){
                    CodeBook b=vb.vd.fullbooks[info.books[booknum]];
                    float last=0.f;

                    if(lsp==null||lsp.length<look.m+1){
                        lsp=new float[look.m+1];
                    }
                    else{
                        for(int j=0; j<lsp.length; j++)
                            lsp[j]=0.f;
                    }

                    for(int j=0; j<look.m; j+=b.dim){
                        if(b.decodev_set(lsp, j, vb.opb, b.dim)==-1){
                            return (null);
                        }
                    }

                    for(int j=0; j<look.m;){
                        for(int k=0; k<b.dim; k++, j++)
                            lsp[j]+=last;
                        last=lsp[j-1];
                    }
                    lsp[look.m]=amp;
                    return (lsp);
                }
            }
            return (null);
        }

        int inverse2(Block vb, Object i, Object memo, float[] out){
            Floor0.LookFloor0 look=(Floor0.LookFloor0)i;
            Floor0.InfoFloor0 info=look.vi;

            if(memo!=null){
                float[] lsp=(float[])memo;
                float amp=lsp[look.m];

                Lsp.lsp_to_curve(out, look.linearmap, look.n, look.ln, lsp, look.m, amp,
                        info.ampdB);
                return (1);
            }
            for(int j=0; j<look.n; j++){
                out[j]=0.f;
            }
            return (0);
        }

        static float fromdB(float x){
            return (float)(Math.exp((x)*.11512925));
        }

        static void lsp_to_lpc(float[] lsp, float[] lpc, int m){
            int i, j, m2=m/2;
            float[] O=new float[m2];
            float[] E=new float[m2];
            float A;
            float[] Ae=new float[m2+1];
            float[] Ao=new float[m2+1];
            float B;
            float[] Be=new float[m2];
            float[] Bo=new float[m2];
            float temp;

            // even/odd roots setup
            for(i=0; i<m2; i++){
                O[i]=(float)(-2.*Math.cos(lsp[i*2]));
                E[i]=(float)(-2.*Math.cos(lsp[i*2+1]));
            }

            // set up impulse response
            for(j=0; j<m2; j++){
                Ae[j]=0.f;
                Ao[j]=1.f;
                Be[j]=0.f;
                Bo[j]=1.f;
            }
            Ao[j]=1.f;
            Ae[j]=1.f;

            // run impulse response
            for(i=1; i<m+1; i++){
                A=B=0.f;
                for(j=0; j<m2; j++){
                    temp=O[j]*Ao[j]+Ae[j];
                    Ae[j]=Ao[j];
                    Ao[j]=A;
                    A+=temp;

                    temp=E[j]*Bo[j]+Be[j];
                    Be[j]=Bo[j];
                    Bo[j]=B;
                    B+=temp;
                }
                lpc[i-1]=(A+Ao[j]+B-Ae[j])/2;
                Ao[j]=A;
                Ae[j]=B;
            }
        }

        static void lpc_to_curve(float[] curve, float[] lpc, float amp, Floor0.LookFloor0 l,
                                 String name, int frameno){
            // l->m+1 must be less than l->ln, but guard in case we get a bad stream
            float[] lcurve=new float[Math.max(l.ln*2, l.m*2+2)];

            if(amp==0){
                for(int j=0; j<l.n; j++)
                    curve[j]=0.0f;
                return;
            }
            l.lpclook.lpc_to_curve(lcurve, lpc, amp);

            for(int i=0; i<l.n; i++)
                curve[i]=lcurve[l.linearmap[i]];
        }

        class InfoFloor0{
            int order;
            int rate;
            int barkmap;

            int ampbits;
            int ampdB;

            int numbooks; // <= 16
            int[] books=new int[16];
        }

        class LookFloor0{
            int n;
            int ln;
            int m;
            int[] linearmap;

            Floor0.InfoFloor0 vi;
            Lpc lpclook= new Lpc();
        }

        class EchstateFloor0{
            int[] codewords;
            float[] curve;
            long frameno;
            long codes;
        }
    }

    static class Drft{
        int n;
        float[] trigcache;
        int[] splitcache;

        void backward(float[] data){
            if(n==1)
                return;
            drftb1(n, data, trigcache, trigcache, n, splitcache);
        }

        void init(int n){
            this.n=n;
            trigcache=new float[3*n];
            splitcache=new int[32];
            fdrffti(n, trigcache, splitcache);
        }

        void clear(){
            if(trigcache!=null)
                trigcache=null;
            if(splitcache!=null)
                splitcache=null;
        }

        static int[] ntryh= {4, 2, 3, 5};
        static float tpi=6.28318530717958647692528676655900577f;
        static float hsqt2=.70710678118654752440084436210485f;
        static float taui=.86602540378443864676372317075293618f;
        static float taur=-.5f;
        static float sqrt2=1.4142135623730950488016887242097f;

        static void drfti1(int n, float[] wa, int index, int[] ifac){
            float arg, argh, argld, fi;
            int ntry=0, i, j=-1;
            int k1, l1, l2, ib;
            int ld, ii, ip, is, nq, nr;
            int ido, ipm, nfm1;
            int nl=n;
            int nf=0;

            int state=101;

            loop: while(true){
                switch(state){
                    case 101:
                        j++;
                        if(j<4)
                            ntry=ntryh[j];
                        else
                            ntry+=2;
                    case 104:
                        nq=nl/ntry;
                        nr=nl-ntry*nq;
                        if(nr!=0){
                            state=101;
                            break;
                        }
                        nf++;
                        ifac[nf+1]=ntry;
                        nl=nq;
                        if(ntry!=2){
                            state=107;
                            break;
                        }
                        if(nf==1){
                            state=107;
                            break;
                        }

                        for(i=1; i<nf; i++){
                            ib=nf-i+1;
                            ifac[ib+1]=ifac[ib];
                        }
                        ifac[2]=2;
                    case 107:
                        if(nl!=1){
                            state=104;
                            break;
                        }
                        ifac[0]=n;
                        ifac[1]=nf;
                        argh=tpi/n;
                        is=0;
                        nfm1=nf-1;
                        l1=1;

                        if(nfm1==0)
                            return;

                        for(k1=0; k1<nfm1; k1++){
                            ip=ifac[k1+2];
                            ld=0;
                            l2=l1*ip;
                            ido=n/l2;
                            ipm=ip-1;

                            for(j=0; j<ipm; j++){
                                ld+=l1;
                                i=is;
                                argld=(float)ld*argh;
                                fi=0.f;
                                for(ii=2; ii<ido; ii+=2){
                                    fi+=1.f;
                                    arg=fi*argld;
                                    wa[index+i++]=(float)Math.cos(arg);
                                    wa[index+i++]=(float)Math.sin(arg);
                                }
                                is+=ido;
                            }
                            l1=l2;
                        }
                        break loop;
                }
            }
        }

        static void fdrffti(int n, float[] wsave, int[] ifac){
            if(n==1)
                return;
            drfti1(n, wsave, n, ifac);
        }

        static void dradf2(int ido, int l1, float[] cc, float[] ch, float[] wa1,
                           int index){
            int i, k;
            float ti2, tr2;
            int t0, t1, t2, t3, t4, t5, t6;

            t1=0;
            t0=(t2=l1*ido);
            t3=ido<<1;
            for(k=0; k<l1; k++){
                ch[t1<<1]=cc[t1]+cc[t2];
                ch[(t1<<1)+t3-1]=cc[t1]-cc[t2];
                t1+=ido;
                t2+=ido;
            }

            if(ido<2)
                return;

            if(ido!=2){
                t1=0;
                t2=t0;
                for(k=0; k<l1; k++){
                    t3=t2;
                    t4=(t1<<1)+(ido<<1);
                    t5=t1;
                    t6=t1+t1;
                    for(i=2; i<ido; i+=2){
                        t3+=2;
                        t4-=2;
                        t5+=2;
                        t6+=2;
                        tr2=wa1[index+i-2]*cc[t3-1]+wa1[index+i-1]*cc[t3];
                        ti2=wa1[index+i-2]*cc[t3]-wa1[index+i-1]*cc[t3-1];
                        ch[t6]=cc[t5]+ti2;
                        ch[t4]=ti2-cc[t5];
                        ch[t6-1]=cc[t5-1]+tr2;
                        ch[t4-1]=cc[t5-1]-tr2;
                    }
                    t1+=ido;
                    t2+=ido;
                }
                if(ido%2==1)
                    return;
            }

            t3=(t2=(t1=ido)-1);
            t2+=t0;
            for(k=0; k<l1; k++){
                ch[t1]=-cc[t2];
                ch[t1-1]=cc[t3];
                t1+=ido<<1;
                t2+=ido;
                t3+=ido;
            }
        }

        static void dradf4(int ido, int l1, float[] cc, float[] ch, float[] wa1,
                           int index1, float[] wa2, int index2, float[] wa3, int index3){
            int i, k, t0, t1, t2, t3, t4, t5, t6;
            float ci2, ci3, ci4, cr2, cr3, cr4, ti1, ti2, ti3, ti4, tr1, tr2, tr3, tr4;
            t0=l1*ido;

            t1=t0;
            t4=t1<<1;
            t2=t1+(t1<<1);
            t3=0;

            for(k=0; k<l1; k++){
                tr1=cc[t1]+cc[t2];
                tr2=cc[t3]+cc[t4];

                ch[t5=t3<<2]=tr1+tr2;
                ch[(ido<<2)+t5-1]=tr2-tr1;
                ch[(t5+=(ido<<1))-1]=cc[t3]-cc[t4];
                ch[t5]=cc[t2]-cc[t1];

                t1+=ido;
                t2+=ido;
                t3+=ido;
                t4+=ido;
            }
            if(ido<2)
                return;

            if(ido!=2){
                t1=0;
                for(k=0; k<l1; k++){
                    t2=t1;
                    t4=t1<<2;
                    t5=(t6=ido<<1)+t4;
                    for(i=2; i<ido; i+=2){
                        t3=(t2+=2);
                        t4+=2;
                        t5-=2;

                        t3+=t0;
                        cr2=wa1[index1+i-2]*cc[t3-1]+wa1[index1+i-1]*cc[t3];
                        ci2=wa1[index1+i-2]*cc[t3]-wa1[index1+i-1]*cc[t3-1];
                        t3+=t0;
                        cr3=wa2[index2+i-2]*cc[t3-1]+wa2[index2+i-1]*cc[t3];
                        ci3=wa2[index2+i-2]*cc[t3]-wa2[index2+i-1]*cc[t3-1];
                        t3+=t0;
                        cr4=wa3[index3+i-2]*cc[t3-1]+wa3[index3+i-1]*cc[t3];
                        ci4=wa3[index3+i-2]*cc[t3]-wa3[index3+i-1]*cc[t3-1];

                        tr1=cr2+cr4;
                        tr4=cr4-cr2;
                        ti1=ci2+ci4;
                        ti4=ci2-ci4;

                        ti2=cc[t2]+ci3;
                        ti3=cc[t2]-ci3;
                        tr2=cc[t2-1]+cr3;
                        tr3=cc[t2-1]-cr3;

                        ch[t4-1]=tr1+tr2;
                        ch[t4]=ti1+ti2;

                        ch[t5-1]=tr3-ti4;
                        ch[t5]=tr4-ti3;

                        ch[t4+t6-1]=ti4+tr3;
                        ch[t4+t6]=tr4+ti3;

                        ch[t5+t6-1]=tr2-tr1;
                        ch[t5+t6]=ti1-ti2;
                    }
                    t1+=ido;
                }
                if((ido&1)!=0)
                    return;
            }

            t2=(t1=t0+ido-1)+(t0<<1);
            t3=ido<<2;
            t4=ido;
            t5=ido<<1;
            t6=ido;

            for(k=0; k<l1; k++){
                ti1=-hsqt2*(cc[t1]+cc[t2]);
                tr1=hsqt2*(cc[t1]-cc[t2]);

                ch[t4-1]=tr1+cc[t6-1];
                ch[t4+t5-1]=cc[t6-1]-tr1;

                ch[t4]=ti1-cc[t1+t0];
                ch[t4+t5]=ti1+cc[t1+t0];

                t1+=ido;
                t2+=ido;
                t4+=t3;
                t6+=ido;
            }
        }

        static void dradfg(int ido, int ip, int l1, int idl1, float[] cc, float[] c1,
                           float[] c2, float[] ch, float[] ch2, float[] wa, int index){
            int idij, ipph, i, j, k, l, ic, ik, is;
            int t0, t1, t2=0, t3, t4, t5, t6, t7, t8, t9, t10;
            float dc2, ai1, ai2, ar1, ar2, ds2;
            int nbd;
            float dcp=0, arg, dsp=0, ar1h, ar2h;
            int idp2, ipp2;

            arg=tpi/(float)ip;
            dcp=(float)Math.cos(arg);
            dsp=(float)Math.sin(arg);
            ipph=(ip+1)>>1;
            ipp2=ip;
            idp2=ido;
            nbd=(ido-1)>>1;
            t0=l1*ido;
            t10=ip*ido;

            int state=100;
            loop: while(true){
                switch(state){
                    case 101:
                        if(ido==1){
                            state=119;
                            break;
                        }
                        for(ik=0; ik<idl1; ik++)
                            ch2[ik]=c2[ik];

                        t1=0;
                        for(j=1; j<ip; j++){
                            t1+=t0;
                            t2=t1;
                            for(k=0; k<l1; k++){
                                ch[t2]=c1[t2];
                                t2+=ido;
                            }
                        }

                        is=-ido;
                        t1=0;
                        if(nbd>l1){
                            for(j=1; j<ip; j++){
                                t1+=t0;
                                is+=ido;
                                t2=-ido+t1;
                                for(k=0; k<l1; k++){
                                    idij=is-1;
                                    t2+=ido;
                                    t3=t2;
                                    for(i=2; i<ido; i+=2){
                                        idij+=2;
                                        t3+=2;
                                        ch[t3-1]=wa[index+idij-1]*c1[t3-1]+wa[index+idij]*c1[t3];
                                        ch[t3]=wa[index+idij-1]*c1[t3]-wa[index+idij]*c1[t3-1];
                                    }
                                }
                            }
                        }
                        else{

                            for(j=1; j<ip; j++){
                                is+=ido;
                                idij=is-1;
                                t1+=t0;
                                t2=t1;
                                for(i=2; i<ido; i+=2){
                                    idij+=2;
                                    t2+=2;
                                    t3=t2;
                                    for(k=0; k<l1; k++){
                                        ch[t3-1]=wa[index+idij-1]*c1[t3-1]+wa[index+idij]*c1[t3];
                                        ch[t3]=wa[index+idij-1]*c1[t3]-wa[index+idij]*c1[t3-1];
                                        t3+=ido;
                                    }
                                }
                            }
                        }

                        t1=0;
                        t2=ipp2*t0;
                        if(nbd<l1){
                            for(j=1; j<ipph; j++){
                                t1+=t0;
                                t2-=t0;
                                t3=t1;
                                t4=t2;
                                for(i=2; i<ido; i+=2){
                                    t3+=2;
                                    t4+=2;
                                    t5=t3-ido;
                                    t6=t4-ido;
                                    for(k=0; k<l1; k++){
                                        t5+=ido;
                                        t6+=ido;
                                        c1[t5-1]=ch[t5-1]+ch[t6-1];
                                        c1[t6-1]=ch[t5]-ch[t6];
                                        c1[t5]=ch[t5]+ch[t6];
                                        c1[t6]=ch[t6-1]-ch[t5-1];
                                    }
                                }
                            }
                        }
                        else{
                            for(j=1; j<ipph; j++){
                                t1+=t0;
                                t2-=t0;
                                t3=t1;
                                t4=t2;
                                for(k=0; k<l1; k++){
                                    t5=t3;
                                    t6=t4;
                                    for(i=2; i<ido; i+=2){
                                        t5+=2;
                                        t6+=2;
                                        c1[t5-1]=ch[t5-1]+ch[t6-1];
                                        c1[t6-1]=ch[t5]-ch[t6];
                                        c1[t5]=ch[t5]+ch[t6];
                                        c1[t6]=ch[t6-1]-ch[t5-1];
                                    }
                                    t3+=ido;
                                    t4+=ido;
                                }
                            }
                        }
                    case 119:
                        for(ik=0; ik<idl1; ik++)
                            c2[ik]=ch2[ik];

                        t1=0;
                        t2=ipp2*idl1;
                        for(j=1; j<ipph; j++){
                            t1+=t0;
                            t2-=t0;
                            t3=t1-ido;
                            t4=t2-ido;
                            for(k=0; k<l1; k++){
                                t3+=ido;
                                t4+=ido;
                                c1[t3]=ch[t3]+ch[t4];
                                c1[t4]=ch[t4]-ch[t3];
                            }
                        }

                        ar1=1.f;
                        ai1=0.f;
                        t1=0;
                        t2=ipp2*idl1;
                        t3=(ip-1)*idl1;
                        for(l=1; l<ipph; l++){
                            t1+=idl1;
                            t2-=idl1;
                            ar1h=dcp*ar1-dsp*ai1;
                            ai1=dcp*ai1+dsp*ar1;
                            ar1=ar1h;
                            t4=t1;
                            t5=t2;
                            t6=t3;
                            t7=idl1;

                            for(ik=0; ik<idl1; ik++){
                                ch2[t4++]=c2[ik]+ar1*c2[t7++];
                                ch2[t5++]=ai1*c2[t6++];
                            }

                            dc2=ar1;
                            ds2=ai1;
                            ar2=ar1;
                            ai2=ai1;

                            t4=idl1;
                            t5=(ipp2-1)*idl1;
                            for(j=2; j<ipph; j++){
                                t4+=idl1;
                                t5-=idl1;

                                ar2h=dc2*ar2-ds2*ai2;
                                ai2=dc2*ai2+ds2*ar2;
                                ar2=ar2h;

                                t6=t1;
                                t7=t2;
                                t8=t4;
                                t9=t5;
                                for(ik=0; ik<idl1; ik++){
                                    ch2[t6++]+=ar2*c2[t8++];
                                    ch2[t7++]+=ai2*c2[t9++];
                                }
                            }
                        }
                        t1=0;
                        for(j=1; j<ipph; j++){
                            t1+=idl1;
                            t2=t1;
                            for(ik=0; ik<idl1; ik++)
                                ch2[ik]+=c2[t2++];
                        }

                        if(ido<l1){
                            state=132;
                            break;
                        }

                        t1=0;
                        t2=0;
                        for(k=0; k<l1; k++){
                            t3=t1;
                            t4=t2;
                            for(i=0; i<ido; i++)
                                cc[t4++]=ch[t3++];
                            t1+=ido;
                            t2+=t10;
                        }
                        state=135;
                        break;

                    case 132:
                        for(i=0; i<ido; i++){
                            t1=i;
                            t2=i;
                            for(k=0; k<l1; k++){
                                cc[t2]=ch[t1];
                                t1+=ido;
                                t2+=t10;
                            }
                        }
                    case 135:
                        t1=0;
                        t2=ido<<1;
                        t3=0;
                        t4=ipp2*t0;
                        for(j=1; j<ipph; j++){
                            t1+=t2;
                            t3+=t0;
                            t4-=t0;

                            t5=t1;
                            t6=t3;
                            t7=t4;

                            for(k=0; k<l1; k++){
                                cc[t5-1]=ch[t6];
                                cc[t5]=ch[t7];
                                t5+=t10;
                                t6+=ido;
                                t7+=ido;
                            }
                        }

                        if(ido==1)
                            return;
                        if(nbd<l1){
                            state=141;
                            break;
                        }

                        t1=-ido;
                        t3=0;
                        t4=0;
                        t5=ipp2*t0;
                        for(j=1; j<ipph; j++){
                            t1+=t2;
                            t3+=t2;
                            t4+=t0;
                            t5-=t0;
                            t6=t1;
                            t7=t3;
                            t8=t4;
                            t9=t5;
                            for(k=0; k<l1; k++){
                                for(i=2; i<ido; i+=2){
                                    ic=idp2-i;
                                    cc[i+t7-1]=ch[i+t8-1]+ch[i+t9-1];
                                    cc[ic+t6-1]=ch[i+t8-1]-ch[i+t9-1];
                                    cc[i+t7]=ch[i+t8]+ch[i+t9];
                                    cc[ic+t6]=ch[i+t9]-ch[i+t8];
                                }
                                t6+=t10;
                                t7+=t10;
                                t8+=ido;
                                t9+=ido;
                            }
                        }
                        return;
                    case 141:
                        t1=-ido;
                        t3=0;
                        t4=0;
                        t5=ipp2*t0;
                        for(j=1; j<ipph; j++){
                            t1+=t2;
                            t3+=t2;
                            t4+=t0;
                            t5-=t0;
                            for(i=2; i<ido; i+=2){
                                t6=idp2+t1-i;
                                t7=i+t3;
                                t8=i+t4;
                                t9=i+t5;
                                for(k=0; k<l1; k++){
                                    cc[t7-1]=ch[t8-1]+ch[t9-1];
                                    cc[t6-1]=ch[t8-1]-ch[t9-1];
                                    cc[t7]=ch[t8]+ch[t9];
                                    cc[t6]=ch[t9]-ch[t8];
                                    t6+=t10;
                                    t7+=t10;
                                    t8+=ido;
                                    t9+=ido;
                                }
                            }
                        }
                        break loop;
                }
            }
        }

        static void drftf1(int n, float[] c, float[] ch, float[] wa, int[] ifac){
            int i, k1, l1, l2;
            int na, kh, nf;
            int ip, iw, ido, idl1, ix2, ix3;

            nf=ifac[1];
            na=1;
            l2=n;
            iw=n;

            for(k1=0; k1<nf; k1++){
                kh=nf-k1;
                ip=ifac[kh+1];
                l1=l2/ip;
                ido=n/l2;
                idl1=ido*l1;
                iw-=(ip-1)*ido;
                na=1-na;

                int state=100;
                loop: while(true){
                    switch(state){
                        case 100:
                            if(ip!=4){
                                state=102;
                                break;
                            }

                            ix2=iw+ido;
                            ix3=ix2+ido;
                            if(na!=0)
                                dradf4(ido, l1, ch, c, wa, iw-1, wa, ix2-1, wa, ix3-1);
                            else
                                dradf4(ido, l1, c, ch, wa, iw-1, wa, ix2-1, wa, ix3-1);
                            state=110;
                            break;
                        case 102:
                            if(ip!=2){
                                state=104;
                                break;
                            }
                            if(na!=0){
                                state=103;
                                break;
                            }
                            dradf2(ido, l1, c, ch, wa, iw-1);
                            state=110;
                            break;
                        case 103:
                            dradf2(ido, l1, ch, c, wa, iw-1);
                        case 104:
                            if(ido==1)
                                na=1-na;
                            if(na!=0){
                                state=109;
                                break;
                            }
                            dradfg(ido, ip, l1, idl1, c, c, c, ch, ch, wa, iw-1);
                            na=1;
                            state=110;
                            break;
                        case 109:
                            dradfg(ido, ip, l1, idl1, ch, ch, ch, c, c, wa, iw-1);
                            na=0;
                        case 110:
                            l2=l1;
                            break loop;
                    }
                }
            }
            if(na==1)
                return;
            for(i=0; i<n; i++)
                c[i]=ch[i];
        }

        static void dradb2(int ido, int l1, float[] cc, float[] ch, float[] wa1,
                           int index){
            int i, k, t0, t1, t2, t3, t4, t5, t6;
            float ti2, tr2;

            t0=l1*ido;

            t1=0;
            t2=0;
            t3=(ido<<1)-1;
            for(k=0; k<l1; k++){
                ch[t1]=cc[t2]+cc[t3+t2];
                ch[t1+t0]=cc[t2]-cc[t3+t2];
                t2=(t1+=ido)<<1;
            }

            if(ido<2)
                return;
            if(ido!=2){
                t1=0;
                t2=0;
                for(k=0; k<l1; k++){
                    t3=t1;
                    t5=(t4=t2)+(ido<<1);
                    t6=t0+t1;
                    for(i=2; i<ido; i+=2){
                        t3+=2;
                        t4+=2;
                        t5-=2;
                        t6+=2;
                        ch[t3-1]=cc[t4-1]+cc[t5-1];
                        tr2=cc[t4-1]-cc[t5-1];
                        ch[t3]=cc[t4]-cc[t5];
                        ti2=cc[t4]+cc[t5];
                        ch[t6-1]=wa1[index+i-2]*tr2-wa1[index+i-1]*ti2;
                        ch[t6]=wa1[index+i-2]*ti2+wa1[index+i-1]*tr2;
                    }
                    t2=(t1+=ido)<<1;
                }
                if((ido%2)==1)
                    return;
            }

            t1=ido-1;
            t2=ido-1;
            for(k=0; k<l1; k++){
                ch[t1]=cc[t2]+cc[t2];
                ch[t1+t0]=-(cc[t2+1]+cc[t2+1]);
                t1+=ido;
                t2+=ido<<1;
            }
        }

        static void dradb3(int ido, int l1, float[] cc, float[] ch, float[] wa1,
                           int index1, float[] wa2, int index2){
            int i, k, t0, t1, t2, t3, t4, t5, t6, t7, t8, t9, t10;
            float ci2, ci3, di2, di3, cr2, cr3, dr2, dr3, ti2, tr2;
            t0=l1*ido;

            t1=0;
            t2=t0<<1;
            t3=ido<<1;
            t4=ido+(ido<<1);
            t5=0;
            for(k=0; k<l1; k++){
                tr2=cc[t3-1]+cc[t3-1];
                cr2=cc[t5]+(taur*tr2);
                ch[t1]=cc[t5]+tr2;
                ci3=taui*(cc[t3]+cc[t3]);
                ch[t1+t0]=cr2-ci3;
                ch[t1+t2]=cr2+ci3;
                t1+=ido;
                t3+=t4;
                t5+=t4;
            }

            if(ido==1)
                return;

            t1=0;
            t3=ido<<1;
            for(k=0; k<l1; k++){
                t7=t1+(t1<<1);
                t6=(t5=t7+t3);
                t8=t1;
                t10=(t9=t1+t0)+t0;

                for(i=2; i<ido; i+=2){
                    t5+=2;
                    t6-=2;
                    t7+=2;
                    t8+=2;
                    t9+=2;
                    t10+=2;
                    tr2=cc[t5-1]+cc[t6-1];
                    cr2=cc[t7-1]+(taur*tr2);
                    ch[t8-1]=cc[t7-1]+tr2;
                    ti2=cc[t5]-cc[t6];
                    ci2=cc[t7]+(taur*ti2);
                    ch[t8]=cc[t7]+ti2;
                    cr3=taui*(cc[t5-1]-cc[t6-1]);
                    ci3=taui*(cc[t5]+cc[t6]);
                    dr2=cr2-ci3;
                    dr3=cr2+ci3;
                    di2=ci2+cr3;
                    di3=ci2-cr3;
                    ch[t9-1]=wa1[index1+i-2]*dr2-wa1[index1+i-1]*di2;
                    ch[t9]=wa1[index1+i-2]*di2+wa1[index1+i-1]*dr2;
                    ch[t10-1]=wa2[index2+i-2]*dr3-wa2[index2+i-1]*di3;
                    ch[t10]=wa2[index2+i-2]*di3+wa2[index2+i-1]*dr3;
                }
                t1+=ido;
            }
        }

        static void dradb4(int ido, int l1, float[] cc, float[] ch, float[] wa1,
                           int index1, float[] wa2, int index2, float[] wa3, int index3){
            int i, k, t0, t1, t2, t3, t4, t5, t6, t7, t8;
            float ci2, ci3, ci4, cr2, cr3, cr4, ti1, ti2, ti3, ti4, tr1, tr2, tr3, tr4;
            t0=l1*ido;

            t1=0;
            t2=ido<<2;
            t3=0;
            t6=ido<<1;
            for(k=0; k<l1; k++){
                t4=t3+t6;
                t5=t1;
                tr3=cc[t4-1]+cc[t4-1];
                tr4=cc[t4]+cc[t4];
                tr1=cc[t3]-cc[(t4+=t6)-1];
                tr2=cc[t3]+cc[t4-1];
                ch[t5]=tr2+tr3;
                ch[t5+=t0]=tr1-tr4;
                ch[t5+=t0]=tr2-tr3;
                ch[t5+=t0]=tr1+tr4;
                t1+=ido;
                t3+=t2;
            }

            if(ido<2)
                return;
            if(ido!=2){
                t1=0;
                for(k=0; k<l1; k++){
                    t5=(t4=(t3=(t2=t1<<2)+t6))+t6;
                    t7=t1;
                    for(i=2; i<ido; i+=2){
                        t2+=2;
                        t3+=2;
                        t4-=2;
                        t5-=2;
                        t7+=2;
                        ti1=cc[t2]+cc[t5];
                        ti2=cc[t2]-cc[t5];
                        ti3=cc[t3]-cc[t4];
                        tr4=cc[t3]+cc[t4];
                        tr1=cc[t2-1]-cc[t5-1];
                        tr2=cc[t2-1]+cc[t5-1];
                        ti4=cc[t3-1]-cc[t4-1];
                        tr3=cc[t3-1]+cc[t4-1];
                        ch[t7-1]=tr2+tr3;
                        cr3=tr2-tr3;
                        ch[t7]=ti2+ti3;
                        ci3=ti2-ti3;
                        cr2=tr1-tr4;
                        cr4=tr1+tr4;
                        ci2=ti1+ti4;
                        ci4=ti1-ti4;

                        ch[(t8=t7+t0)-1]=wa1[index1+i-2]*cr2-wa1[index1+i-1]*ci2;
                        ch[t8]=wa1[index1+i-2]*ci2+wa1[index1+i-1]*cr2;
                        ch[(t8+=t0)-1]=wa2[index2+i-2]*cr3-wa2[index2+i-1]*ci3;
                        ch[t8]=wa2[index2+i-2]*ci3+wa2[index2+i-1]*cr3;
                        ch[(t8+=t0)-1]=wa3[index3+i-2]*cr4-wa3[index3+i-1]*ci4;
                        ch[t8]=wa3[index3+i-2]*ci4+wa3[index3+i-1]*cr4;
                    }
                    t1+=ido;
                }
                if(ido%2==1)
                    return;
            }

            t1=ido;
            t2=ido<<2;
            t3=ido-1;
            t4=ido+(ido<<1);
            for(k=0; k<l1; k++){
                t5=t3;
                ti1=cc[t1]+cc[t4];
                ti2=cc[t4]-cc[t1];
                tr1=cc[t1-1]-cc[t4-1];
                tr2=cc[t1-1]+cc[t4-1];
                ch[t5]=tr2+tr2;
                ch[t5+=t0]=sqrt2*(tr1-ti1);
                ch[t5+=t0]=ti2+ti2;
                ch[t5+=t0]=-sqrt2*(tr1+ti1);

                t3+=ido;
                t1+=t2;
                t4+=t2;
            }
        }

        static void dradbg(int ido, int ip, int l1, int idl1, float[] cc, float[] c1,
                           float[] c2, float[] ch, float[] ch2, float[] wa, int index){

            int idij, ipph=0, i, j, k, l, ik, is, t0=0, t1, t2, t3, t4, t5, t6, t7, t8, t9, t10=0, t11, t12;
            float dc2, ai1, ai2, ar1, ar2, ds2;
            int nbd=0;
            float dcp=0, arg, dsp=0, ar1h, ar2h;
            int ipp2=0;

            int state=100;

            loop: while(true){
                switch(state){
                    case 100:
                        t10=ip*ido;
                        t0=l1*ido;
                        arg=tpi/(float)ip;
                        dcp=(float)Math.cos(arg);
                        dsp=(float)Math.sin(arg);
                        nbd=(ido-1)>>>1;
                        ipp2=ip;
                        ipph=(ip+1)>>>1;
                        if(ido<l1){
                            state=103;
                            break;
                        }
                        t1=0;
                        t2=0;
                        for(k=0; k<l1; k++){
                            t3=t1;
                            t4=t2;
                            for(i=0; i<ido; i++){
                                ch[t3]=cc[t4];
                                t3++;
                                t4++;
                            }
                            t1+=ido;
                            t2+=t10;
                        }
                        state=106;
                        break;
                    case 103:
                        t1=0;
                        for(i=0; i<ido; i++){
                            t2=t1;
                            t3=t1;
                            for(k=0; k<l1; k++){
                                ch[t2]=cc[t3];
                                t2+=ido;
                                t3+=t10;
                            }
                            t1++;
                        }
                    case 106:
                        t1=0;
                        t2=ipp2*t0;
                        t7=(t5=ido<<1);
                        for(j=1; j<ipph; j++){
                            t1+=t0;
                            t2-=t0;
                            t3=t1;
                            t4=t2;
                            t6=t5;
                            for(k=0; k<l1; k++){
                                ch[t3]=cc[t6-1]+cc[t6-1];
                                ch[t4]=cc[t6]+cc[t6];
                                t3+=ido;
                                t4+=ido;
                                t6+=t10;
                            }
                            t5+=t7;
                        }
                        if(ido==1){
                            state=116;
                            break;
                        }
                        if(nbd<l1){
                            state=112;
                            break;
                        }

                        t1=0;
                        t2=ipp2*t0;
                        t7=0;
                        for(j=1; j<ipph; j++){
                            t1+=t0;
                            t2-=t0;
                            t3=t1;
                            t4=t2;

                            t7+=(ido<<1);
                            t8=t7;
                            for(k=0; k<l1; k++){
                                t5=t3;
                                t6=t4;
                                t9=t8;
                                t11=t8;
                                for(i=2; i<ido; i+=2){
                                    t5+=2;
                                    t6+=2;
                                    t9+=2;
                                    t11-=2;
                                    ch[t5-1]=cc[t9-1]+cc[t11-1];
                                    ch[t6-1]=cc[t9-1]-cc[t11-1];
                                    ch[t5]=cc[t9]-cc[t11];
                                    ch[t6]=cc[t9]+cc[t11];
                                }
                                t3+=ido;
                                t4+=ido;
                                t8+=t10;
                            }
                        }
                        state=116;
                        break;
                    case 112:
                        t1=0;
                        t2=ipp2*t0;
                        t7=0;
                        for(j=1; j<ipph; j++){
                            t1+=t0;
                            t2-=t0;
                            t3=t1;
                            t4=t2;
                            t7+=(ido<<1);
                            t8=t7;
                            t9=t7;
                            for(i=2; i<ido; i+=2){
                                t3+=2;
                                t4+=2;
                                t8+=2;
                                t9-=2;
                                t5=t3;
                                t6=t4;
                                t11=t8;
                                t12=t9;
                                for(k=0; k<l1; k++){
                                    ch[t5-1]=cc[t11-1]+cc[t12-1];
                                    ch[t6-1]=cc[t11-1]-cc[t12-1];
                                    ch[t5]=cc[t11]-cc[t12];
                                    ch[t6]=cc[t11]+cc[t12];
                                    t5+=ido;
                                    t6+=ido;
                                    t11+=t10;
                                    t12+=t10;
                                }
                            }
                        }
                    case 116:
                        ar1=1.f;
                        ai1=0.f;
                        t1=0;
                        t9=(t2=ipp2*idl1);
                        t3=(ip-1)*idl1;
                        for(l=1; l<ipph; l++){
                            t1+=idl1;
                            t2-=idl1;

                            ar1h=dcp*ar1-dsp*ai1;
                            ai1=dcp*ai1+dsp*ar1;
                            ar1=ar1h;
                            t4=t1;
                            t5=t2;
                            t6=0;
                            t7=idl1;
                            t8=t3;
                            for(ik=0; ik<idl1; ik++){
                                c2[t4++]=ch2[t6++]+ar1*ch2[t7++];
                                c2[t5++]=ai1*ch2[t8++];
                            }
                            dc2=ar1;
                            ds2=ai1;
                            ar2=ar1;
                            ai2=ai1;

                            t6=idl1;
                            t7=t9-idl1;
                            for(j=2; j<ipph; j++){
                                t6+=idl1;
                                t7-=idl1;
                                ar2h=dc2*ar2-ds2*ai2;
                                ai2=dc2*ai2+ds2*ar2;
                                ar2=ar2h;
                                t4=t1;
                                t5=t2;
                                t11=t6;
                                t12=t7;
                                for(ik=0; ik<idl1; ik++){
                                    c2[t4++]+=ar2*ch2[t11++];
                                    c2[t5++]+=ai2*ch2[t12++];
                                }
                            }
                        }

                        t1=0;
                        for(j=1; j<ipph; j++){
                            t1+=idl1;
                            t2=t1;
                            for(ik=0; ik<idl1; ik++)
                                ch2[ik]+=ch2[t2++];
                        }

                        t1=0;
                        t2=ipp2*t0;
                        for(j=1; j<ipph; j++){
                            t1+=t0;
                            t2-=t0;
                            t3=t1;
                            t4=t2;
                            for(k=0; k<l1; k++){
                                ch[t3]=c1[t3]-c1[t4];
                                ch[t4]=c1[t3]+c1[t4];
                                t3+=ido;
                                t4+=ido;
                            }
                        }

                        if(ido==1){
                            state=132;
                            break;
                        }
                        if(nbd<l1){
                            state=128;
                            break;
                        }

                        t1=0;
                        t2=ipp2*t0;
                        for(j=1; j<ipph; j++){
                            t1+=t0;
                            t2-=t0;
                            t3=t1;
                            t4=t2;
                            for(k=0; k<l1; k++){
                                t5=t3;
                                t6=t4;
                                for(i=2; i<ido; i+=2){
                                    t5+=2;
                                    t6+=2;
                                    ch[t5-1]=c1[t5-1]-c1[t6];
                                    ch[t6-1]=c1[t5-1]+c1[t6];
                                    ch[t5]=c1[t5]+c1[t6-1];
                                    ch[t6]=c1[t5]-c1[t6-1];
                                }
                                t3+=ido;
                                t4+=ido;
                            }
                        }
                        state=132;
                        break;
                    case 128:
                        t1=0;
                        t2=ipp2*t0;
                        for(j=1; j<ipph; j++){
                            t1+=t0;
                            t2-=t0;
                            t3=t1;
                            t4=t2;
                            for(i=2; i<ido; i+=2){
                                t3+=2;
                                t4+=2;
                                t5=t3;
                                t6=t4;
                                for(k=0; k<l1; k++){
                                    ch[t5-1]=c1[t5-1]-c1[t6];
                                    ch[t6-1]=c1[t5-1]+c1[t6];
                                    ch[t5]=c1[t5]+c1[t6-1];
                                    ch[t6]=c1[t5]-c1[t6-1];
                                    t5+=ido;
                                    t6+=ido;
                                }
                            }
                        }
                    case 132:
                        if(ido==1)
                            return;

                        for(ik=0; ik<idl1; ik++)
                            c2[ik]=ch2[ik];

                        t1=0;
                        for(j=1; j<ip; j++){
                            t2=(t1+=t0);
                            for(k=0; k<l1; k++){
                                c1[t2]=ch[t2];
                                t2+=ido;
                            }
                        }

                        if(nbd>l1){
                            state=139;
                            break;
                        }

                        is=-ido-1;
                        t1=0;
                        for(j=1; j<ip; j++){
                            is+=ido;
                            t1+=t0;
                            idij=is;
                            t2=t1;
                            for(i=2; i<ido; i+=2){
                                t2+=2;
                                idij+=2;
                                t3=t2;
                                for(k=0; k<l1; k++){
                                    c1[t3-1]=wa[index+idij-1]*ch[t3-1]-wa[index+idij]*ch[t3];
                                    c1[t3]=wa[index+idij-1]*ch[t3]+wa[index+idij]*ch[t3-1];
                                    t3+=ido;
                                }
                            }
                        }
                        return;

                    case 139:
                        is=-ido-1;
                        t1=0;
                        for(j=1; j<ip; j++){
                            is+=ido;
                            t1+=t0;
                            t2=t1;
                            for(k=0; k<l1; k++){
                                idij=is;
                                t3=t2;
                                for(i=2; i<ido; i+=2){
                                    idij+=2;
                                    t3+=2;
                                    c1[t3-1]=wa[index+idij-1]*ch[t3-1]-wa[index+idij]*ch[t3];
                                    c1[t3]=wa[index+idij-1]*ch[t3]+wa[index+idij]*ch[t3-1];
                                }
                                t2+=ido;
                            }
                        }
                        break loop;
                }
            }
        }

        static void drftb1(int n, float[] c, float[] ch, float[] wa, int index,
                           int[] ifac){
            int i, k1, l1, l2=0;
            int na;
            int nf, ip=0, iw, ix2, ix3, ido=0, idl1=0;

            nf=ifac[1];
            na=0;
            l1=1;
            iw=1;

            for(k1=0; k1<nf; k1++){
                int state=100;
                loop: while(true){
                    switch(state){
                        case 100:
                            ip=ifac[k1+2];
                            l2=ip*l1;
                            ido=n/l2;
                            idl1=ido*l1;
                            if(ip!=4){
                                state=103;
                                break;
                            }
                            ix2=iw+ido;
                            ix3=ix2+ido;

                            if(na!=0)
                                dradb4(ido, l1, ch, c, wa, index+iw-1, wa, index+ix2-1, wa, index
                                        +ix3-1);
                            else
                                dradb4(ido, l1, c, ch, wa, index+iw-1, wa, index+ix2-1, wa, index
                                        +ix3-1);
                            na=1-na;
                            state=115;
                            break;
                        case 103:
                            if(ip!=2){
                                state=106;
                                break;
                            }

                            if(na!=0)
                                dradb2(ido, l1, ch, c, wa, index+iw-1);
                            else
                                dradb2(ido, l1, c, ch, wa, index+iw-1);
                            na=1-na;
                            state=115;
                            break;

                        case 106:
                            if(ip!=3){
                                state=109;
                                break;
                            }

                            ix2=iw+ido;
                            if(na!=0)
                                dradb3(ido, l1, ch, c, wa, index+iw-1, wa, index+ix2-1);
                            else
                                dradb3(ido, l1, c, ch, wa, index+iw-1, wa, index+ix2-1);
                            na=1-na;
                            state=115;
                            break;
                        case 109:
                            if(na!=0)
                                dradbg(ido, ip, l1, idl1, ch, ch, ch, c, c, wa, index+iw-1);
                            else
                                dradbg(ido, ip, l1, idl1, c, c, c, ch, ch, wa, index+iw-1);
                            if(ido==1)
                                na=1-na;

                        case 115:
                            l1=l2;
                            iw+=(ip-1)*ido;
                            break loop;
                    }
                }
            }
            if(na==0)
                return;
            for(i=0; i<n; i++)
                c[i]=ch[i];
        }
    }

    static class Lookup{
        static final int COS_LOOKUP_SZ=128;
        static final float[] COS_LOOKUP= {+1.0000000000000f, +0.9996988186962f,
                +0.9987954562052f, +0.9972904566787f, +0.9951847266722f,
                +0.9924795345987f, +0.9891765099648f, +0.9852776423889f,
                +0.9807852804032f, +0.9757021300385f, +0.9700312531945f,
                +0.9637760657954f, +0.9569403357322f, +0.9495281805930f,
                +0.9415440651830f, +0.9329927988347f, +0.9238795325113f,
                +0.9142097557035f, +0.9039892931234f, +0.8932243011955f,
                +0.8819212643484f, +0.8700869911087f, +0.8577286100003f,
                +0.8448535652497f, +0.8314696123025f, +0.8175848131516f,
                +0.8032075314806f, +0.7883464276266f, +0.7730104533627f,
                +0.7572088465065f, +0.7409511253550f, +0.7242470829515f,
                +0.7071067811865f, +0.6895405447371f, +0.6715589548470f,
                +0.6531728429538f, +0.6343932841636f, +0.6152315905806f,
                +0.5956993044924f, +0.5758081914178f, +0.5555702330196f,
                +0.5349976198871f, +0.5141027441932f, +0.4928981922298f,
                +0.4713967368260f, +0.4496113296546f, +0.4275550934303f,
                +0.4052413140050f, +0.3826834323651f, +0.3598950365350f,
                +0.3368898533922f, +0.3136817403989f, +0.2902846772545f,
                +0.2667127574749f, +0.2429801799033f, +0.2191012401569f,
                +0.1950903220161f, +0.1709618887603f, +0.1467304744554f,
                +0.1224106751992f, +0.0980171403296f, +0.0735645635997f,
                +0.0490676743274f, +0.0245412285229f, +0.0000000000000f,
                -0.0245412285229f, -0.0490676743274f, -0.0735645635997f,
                -0.0980171403296f, -0.1224106751992f, -0.1467304744554f,
                -0.1709618887603f, -0.1950903220161f, -0.2191012401569f,
                -0.2429801799033f, -0.2667127574749f, -0.2902846772545f,
                -0.3136817403989f, -0.3368898533922f, -0.3598950365350f,
                -0.3826834323651f, -0.4052413140050f, -0.4275550934303f,
                -0.4496113296546f, -0.4713967368260f, -0.4928981922298f,
                -0.5141027441932f, -0.5349976198871f, -0.5555702330196f,
                -0.5758081914178f, -0.5956993044924f, -0.6152315905806f,
                -0.6343932841636f, -0.6531728429538f, -0.6715589548470f,
                -0.6895405447371f, -0.7071067811865f, -0.7242470829515f,
                -0.7409511253550f, -0.7572088465065f, -0.7730104533627f,
                -0.7883464276266f, -0.8032075314806f, -0.8175848131516f,
                -0.8314696123025f, -0.8448535652497f, -0.8577286100003f,
                -0.8700869911087f, -0.8819212643484f, -0.8932243011955f,
                -0.9039892931234f, -0.9142097557035f, -0.9238795325113f,
                -0.9329927988347f, -0.9415440651830f, -0.9495281805930f,
                -0.9569403357322f, -0.9637760657954f, -0.9700312531945f,
                -0.9757021300385f, -0.9807852804032f, -0.9852776423889f,
                -0.9891765099648f, -0.9924795345987f, -0.9951847266722f,
                -0.9972904566787f, -0.9987954562052f, -0.9996988186962f,
                -1.0000000000000f,};

        /* interpolated lookup based cos function, domain 0 to PI only */
        static float coslook(float a){
            double d=a*(.31830989*(float)COS_LOOKUP_SZ);
            int i=(int)d;
            return COS_LOOKUP[i]+((float)(d-i))*(COS_LOOKUP[i+1]-COS_LOOKUP[i]);
        }

        static final int INVSQ_LOOKUP_SZ=32;
        static final float[] INVSQ_LOOKUP= {1.414213562373f, 1.392621247646f,
                1.371988681140f, 1.352246807566f, 1.333333333333f, 1.315191898443f,
                1.297771369046f, 1.281025230441f, 1.264911064067f, 1.249390095109f,
                1.234426799697f, 1.219988562661f, 1.206045378311f, 1.192569588000f,
                1.179535649239f, 1.166919931983f, 1.154700538379f, 1.142857142857f,
                1.131370849898f, 1.120224067222f, 1.109400392450f, 1.098884511590f,
                1.088662107904f, 1.078719779941f, 1.069044967650f, 1.059625885652f,
                1.050451462878f, 1.041511287847f, 1.032795558989f, 1.024295039463f,
                1.016001016002f, 1.007905261358f, 1.000000000000f,};

        /* interpolated 1./sqrt(p) where .5 <= p < 1. */
        static float invsqlook(float a){
            double d=a*(2.f*(float)INVSQ_LOOKUP_SZ)-(float)INVSQ_LOOKUP_SZ;
            int i=(int)d;
            return INVSQ_LOOKUP[i]+((float)(d-i))*(INVSQ_LOOKUP[i+1]-INVSQ_LOOKUP[i]);
        }

        static final int INVSQ2EXP_LOOKUP_MIN=-32;
        static final int INVSQ2EXP_LOOKUP_MAX=32;
        static final float[] INVSQ2EXP_LOOKUP= {65536.f, 46340.95001f, 32768.f,
                23170.47501f, 16384.f, 11585.2375f, 8192.f, 5792.618751f, 4096.f,
                2896.309376f, 2048.f, 1448.154688f, 1024.f, 724.0773439f, 512.f,
                362.038672f, 256.f, 181.019336f, 128.f, 90.50966799f, 64.f, 45.254834f,
                32.f, 22.627417f, 16.f, 11.3137085f, 8.f, 5.656854249f, 4.f,
                2.828427125f, 2.f, 1.414213562f, 1.f, 0.7071067812f, 0.5f, 0.3535533906f,
                0.25f, 0.1767766953f, 0.125f, 0.08838834765f, 0.0625f, 0.04419417382f,
                0.03125f, 0.02209708691f, 0.015625f, 0.01104854346f, 0.0078125f,
                0.005524271728f, 0.00390625f, 0.002762135864f, 0.001953125f,
                0.001381067932f, 0.0009765625f, 0.000690533966f, 0.00048828125f,
                0.000345266983f, 0.000244140625f, 0.0001726334915f, 0.0001220703125f,
                8.631674575e-05f, 6.103515625e-05f, 4.315837288e-05f, 3.051757812e-05f,
                2.157918644e-05f, 1.525878906e-05f,};

        /* interpolated 1./sqrt(p) where .5 <= p < 1. */
        static float invsq2explook(int a){
            return INVSQ2EXP_LOOKUP[a-INVSQ2EXP_LOOKUP_MIN];
        }

        static final int FROMdB_LOOKUP_SZ=35;
        static final int FROMdB2_LOOKUP_SZ=32;
        static final int FROMdB_SHIFT=5;
        static final int FROMdB2_SHIFT=3;
        static final int FROMdB2_MASK=31;
        static final float[] FROMdB_LOOKUP= {1.f, 0.6309573445f, 0.3981071706f,
                0.2511886432f, 0.1584893192f, 0.1f, 0.06309573445f, 0.03981071706f,
                0.02511886432f, 0.01584893192f, 0.01f, 0.006309573445f, 0.003981071706f,
                0.002511886432f, 0.001584893192f, 0.001f, 0.0006309573445f,
                0.0003981071706f, 0.0002511886432f, 0.0001584893192f, 0.0001f,
                6.309573445e-05f, 3.981071706e-05f, 2.511886432e-05f, 1.584893192e-05f,
                1e-05f, 6.309573445e-06f, 3.981071706e-06f, 2.511886432e-06f,
                1.584893192e-06f, 1e-06f, 6.309573445e-07f, 3.981071706e-07f,
                2.511886432e-07f, 1.584893192e-07f,};
        static final float[] FROMdB2_LOOKUP= {0.9928302478f, 0.9786445908f,
                0.9646616199f, 0.9508784391f, 0.9372921937f, 0.92390007f, 0.9106992942f,
                0.8976871324f, 0.8848608897f, 0.8722179097f, 0.8597555737f,
                0.8474713009f, 0.835362547f, 0.8234268041f, 0.8116616003f, 0.8000644989f,
                0.7886330981f, 0.7773650302f, 0.7662579617f, 0.755309592f, 0.7445176537f,
                0.7338799116f, 0.7233941627f, 0.7130582353f, 0.7028699885f,
                0.6928273125f, 0.6829281272f, 0.6731703824f, 0.6635520573f,
                0.6540711597f, 0.6447257262f, 0.6355138211f,};

        /* interpolated lookup based fromdB function, domain -140dB to 0dB only */
        static float fromdBlook(float a){
            int i=(int)(a*((float)(-(1<<FROMdB2_SHIFT))));
            return (i<0) ? 1.f : ((i>=(FROMdB_LOOKUP_SZ<<FROMdB_SHIFT)) ? 0.f
                    : FROMdB_LOOKUP[i>>>FROMdB_SHIFT]*FROMdB2_LOOKUP[i&FROMdB2_MASK]);
        }

    }

    static class Lsp{

        static final float M_PI=(float)(3.1415926539);

        static void lsp_to_curve(float[] curve, int[] map, int n, int ln,
                                 float[] lsp, int m, float amp, float ampoffset){
            int i;
            float wdel=M_PI/ln;
            for(i=0; i<m; i++)
                lsp[i]= Lookup.coslook(lsp[i]);
            int m2=(m/2)*2;

            i=0;
            while(i<n){
                int k=map[i];
                float p=.7071067812f;
                float q=.7071067812f;
                float w=Lookup.coslook(wdel*k);

                for(int j=0; j<m2; j+=2){
                    q*=lsp[j]-w;
                    p*=lsp[j+1]-w;
                }

                if((m&1)!=0){
                    /* odd order filter; slightly assymetric */
                    /* the last coefficient */
                    q*=lsp[m-1]-w;
                    q*=q;
                    p*=p*(1.f-w*w);
                }
                else{
                    /* even order filter; still symmetric */
                    q*=q*(1.f+w);
                    p*=p*(1.f-w);
                }

                //  q=frexp(p+q,&qexp);
                q=p+q;
                int hx=Float.floatToIntBits(q);
                int ix=0x7fffffff&hx;
                int qexp=0;

                if(ix>=0x7f800000||(ix==0)){
                    // 0,inf,nan
                }
                else{
                    if(ix<0x00800000){ // subnormal
                        q*=3.3554432000e+07; // 0x4c000000
                        hx=Float.floatToIntBits(q);
                        ix=0x7fffffff&hx;
                        qexp=-25;
                    }
                    qexp+=((ix>>>23)-126);
                    hx=(hx&0x807fffff)|0x3f000000;
                    q=Float.intBitsToFloat(hx);
                }

                q=Lookup.fromdBlook(amp*Lookup.invsqlook(q)*Lookup.invsq2explook(qexp+m)
                        -ampoffset);

                do{
                    curve[i++]*=q;
                }
                while(i<n&&map[i]==k);

            }
        }
    }


    static class Lpc{
        // en/decode lookups
        Drft fft= new Drft();;

        int ln;
        int m;

        // Autocorrelation LPC coeff generation algorithm invented by
        // N. Levinson in 1947, modified by J. Durbin in 1959.

        // Input : n elements of time doamin data
        // Output: m lpc coefficients, excitation energy

        static float lpc_from_data(float[] data, float[] lpc, int n, int m){
            float[] aut=new float[m+1];
            float error;
            int i, j;

            // autocorrelation, p+1 lag coefficients

            j=m+1;
            while(j--!=0){
                float d=0;
                for(i=j; i<n; i++)
                    d+=data[i]*data[i-j];
                aut[j]=d;
            }

            // Generate lpc coefficients from autocorr values

            error=aut[0];
    /*
    if(error==0){
      for(int k=0; k<m; k++) lpc[k]=0.0f;
      return 0;
    }
    */

            for(i=0; i<m; i++){
                float r=-aut[i+1];

                if(error==0){
                    for(int k=0; k<m; k++)
                        lpc[k]=0.0f;
                    return 0;
                }

                // Sum up this iteration's reflection coefficient; note that in
                // Vorbis we don't save it.  If anyone wants to recycle this code
                // and needs reflection coefficients, save the results of 'r' from
                // each iteration.

                for(j=0; j<i; j++)
                    r-=lpc[j]*aut[i-j];
                r/=error;

                // Update LPC coefficients and total error

                lpc[i]=r;
                for(j=0; j<i/2; j++){
                    float tmp=lpc[j];
                    lpc[j]+=r*lpc[i-1-j];
                    lpc[i-1-j]+=r*tmp;
                }
                if(i%2!=0)
                    lpc[j]+=lpc[j]*r;

                error*=1.0-r*r;
            }

            // we need the error value to know how big an impulse to hit the
            // filter with later

            return error;
        }

        // Input : n element envelope spectral curve
        // Output: m lpc coefficients, excitation energy

        float lpc_from_curve(float[] curve, float[] lpc){
            int n=ln;
            float[] work=new float[n+n];
            float fscale=(float)(.5/n);
            int i, j;

            // input is a real curve. make it complex-real
            // This mixes phase, but the LPC generation doesn't care.
            for(i=0; i<n; i++){
                work[i*2]=curve[i]*fscale;
                work[i*2+1]=0;
            }
            work[n*2-1]=curve[n-1]*fscale;

            n*=2;
            fft.backward(work);

            // The autocorrelation will not be circular.  Shift, else we lose
            // most of the power in the edges.

            for(i=0, j=n/2; i<n/2;){
                float temp=work[i];
                work[i++]=work[j];
                work[j++]=temp;
            }

            return (lpc_from_data(work, lpc, n, m));
        }

        void init(int mapped, int m){
            ln=mapped;
            this.m=m;

            // we cheat decoding the LPC spectrum via FFTs
            fft.init(mapped*2);
        }

        void clear(){
            fft.clear();
        }

        static float FAST_HYPOT(float a, float b){
            return (float)Math.sqrt((a)*(a)+(b)*(b));
        }

        // One can do this the long way by generating the transfer function in
        // the time domain and taking the forward FFT of the result.  The
        // results from direct calculation are cleaner and faster.
        //
        // This version does a linear curve generation and then later
        // interpolates the log curve from the linear curve.

        void lpc_to_curve(float[] curve, float[] lpc, float amp){

            for(int i=0; i<ln*2; i++)
                curve[i]=0.0f;

            if(amp==0)
                return;

            for(int i=0; i<m; i++){
                curve[i*2+1]=lpc[i]/(4*amp);
                curve[i*2+2]=-lpc[i]/(4*amp);
            }

            fft.backward(curve);

            {
                int l2=ln*2;
                float unit=(float)(1./amp);
                curve[0]=(float)(1./(curve[0]*2+unit));
                for(int i=1; i<ln; i++){
                    float real=(curve[i]+curve[l2-i]);
                    float imag=(curve[i]-curve[l2-i]);

                    float a=real+unit;
                    curve[i]=(float)(1.0/FAST_HYPOT(a, imag));
                }
            }
        }
    }

    static class Floor1 extends FuncFloor {

        static final int floor1_rangedb=140;
        static final int VIF_POSIT=63;

        void pack(Object i, VorbisBuffer opb){
            Floor1.InfoFloor1 info=(Floor1.InfoFloor1)i;

            int count=0;
            int rangebits;
            int maxposit=info.postlist[1];
            int maxclass=-1;

            /* save out partitions */
            opb.write(info.partitions, 5); /* only 0 to 31 legal */
            for(int j=0; j<info.partitions; j++){
                opb.write(info.partitionclass[j], 4); /* only 0 to 15 legal */
                if(maxclass<info.partitionclass[j])
                    maxclass=info.partitionclass[j];
            }

            /* save out partition classes */
            for(int j=0; j<maxclass+1; j++){
                opb.write(info.class_dim[j]-1, 3); /* 1 to 8 */
                opb.write(info.class_subs[j], 2); /* 0 to 3 */
                if(info.class_subs[j]!=0){
                    opb.write(info.class_book[j], 8);
                }
                for(int k=0; k<(1<<info.class_subs[j]); k++){
                    opb.write(info.class_subbook[j][k]+1, 8);
                }
            }

            /* save out the post list */
            opb.write(info.mult-1, 2); /* only 1,2,3,4 legal now */
            opb.write(Util.ilog2(maxposit), 4);
            rangebits= Util.ilog2(maxposit);

            for(int j=0, k=0; j<info.partitions; j++){
                count+=info.class_dim[info.partitionclass[j]];
                for(; k<count; k++){
                    opb.write(info.postlist[k+2], rangebits);
                }
            }
        }

        Object unpack(Info vi, VorbisBuffer opb){
            int count=0, maxclass=-1, rangebits;
            Floor1.InfoFloor1 info=new Floor1.InfoFloor1();

            /* read partitions */
            info.partitions=opb.read(5); /* only 0 to 31 legal */
            for(int j=0; j<info.partitions; j++){
                info.partitionclass[j]=opb.read(4); /* only 0 to 15 legal */
                if(maxclass<info.partitionclass[j])
                    maxclass=info.partitionclass[j];
            }

            /* read partition classes */
            for(int j=0; j<maxclass+1; j++){
                info.class_dim[j]=opb.read(3)+1; /* 1 to 8 */
                info.class_subs[j]=opb.read(2); /* 0,1,2,3 bits */
                if(info.class_subs[j]<0){
                    info.free();
                    return (null);
                }
                if(info.class_subs[j]!=0){
                    info.class_book[j]=opb.read(8);
                }
                if(info.class_book[j]<0||info.class_book[j]>=vi.books){
                    info.free();
                    return (null);
                }
                for(int k=0; k<(1<<info.class_subs[j]); k++){
                    info.class_subbook[j][k]=opb.read(8)-1;
                    if(info.class_subbook[j][k]<-1||info.class_subbook[j][k]>=vi.books){
                        info.free();
                        return (null);
                    }
                }
            }

            /* read the post list */
            info.mult=opb.read(2)+1; /* only 1,2,3,4 legal now */
            rangebits=opb.read(4);

            for(int j=0, k=0; j<info.partitions; j++){
                count+=info.class_dim[info.partitionclass[j]];
                for(; k<count; k++){
                    int t=info.postlist[k+2]=opb.read(rangebits);
                    if(t<0||t>=(1<<rangebits)){
                        info.free();
                        return (null);
                    }
                }
            }
            info.postlist[0]=0;
            info.postlist[1]=1<<rangebits;

            return (info);
        }

        Object look(DspState vd, InfoMode mi, Object i){
            int _n=0;

            int[] sortpointer=new int[VIF_POSIT+2];

            //    Info vi=vd.vi;

            Floor1.InfoFloor1 info=(Floor1.InfoFloor1)i;
            Floor1.LookFloor1 look=new Floor1.LookFloor1();
            look.vi=info;
            look.n=info.postlist[1];

    /* we drop each position value in-between already decoded values,
     and use linear interpolation to predict each new value past the
     edges.  The positions are read in the order of the position
     list... we precompute the bounding positions in the lookup.  Of
     course, the neighbors can change (if a position is declined), but
     this is an initial mapping */

            for(int j=0; j<info.partitions; j++){
                _n+=info.class_dim[info.partitionclass[j]];
            }
            _n+=2;
            look.posts=_n;

            /* also store a sorted position index */
            for(int j=0; j<_n; j++){
                sortpointer[j]=j;
            }
            //    qsort(sortpointer,n,sizeof(int),icomp); // !!

            int foo;
            for(int j=0; j<_n-1; j++){
                for(int k=j; k<_n; k++){
                    if(info.postlist[sortpointer[j]]>info.postlist[sortpointer[k]]){
                        foo=sortpointer[k];
                        sortpointer[k]=sortpointer[j];
                        sortpointer[j]=foo;
                    }
                }
            }

            /* points from sort order back to range number */
            for(int j=0; j<_n; j++){
                look.forward_index[j]=sortpointer[j];
            }
            /* points from range order to sorted position */
            for(int j=0; j<_n; j++){
                look.reverse_index[look.forward_index[j]]=j;
            }
            /* we actually need the post values too */
            for(int j=0; j<_n; j++){
                look.sorted_index[j]=info.postlist[look.forward_index[j]];
            }

            /* quantize values to multiplier spec */
            switch(info.mult){
                case 1: /* 1024 -> 256 */
                    look.quant_q=256;
                    break;
                case 2: /* 1024 -> 128 */
                    look.quant_q=128;
                    break;
                case 3: /* 1024 -> 86 */
                    look.quant_q=86;
                    break;
                case 4: /* 1024 -> 64 */
                    look.quant_q=64;
                    break;
                default:
                    look.quant_q=-1;
            }

    /* discover our neighbors for decode where we don't use fit flags
       (that would push the neighbors outward) */
            for(int j=0; j<_n-2; j++){
                int lo=0;
                int hi=1;
                int lx=0;
                int hx=look.n;
                int currentx=info.postlist[j+2];
                for(int k=0; k<j+2; k++){
                    int x=info.postlist[k];
                    if(x>lx&&x<currentx){
                        lo=k;
                        lx=x;
                    }
                    if(x<hx&&x>currentx){
                        hi=k;
                        hx=x;
                    }
                }
                look.loneighbor[j]=lo;
                look.hineighbor[j]=hi;
            }

            return look;
        }

        void free_info(Object i){
        }

        void free_look(Object i){
        }

        void free_state(Object vs){
        }

        int forward(Block vb, Object i, float[] in, float[] out, Object vs){
            return 0;
        }

        Object inverse1(Block vb, Object ii, Object memo){
            Floor1.LookFloor1 look=(Floor1.LookFloor1)ii;
            Floor1.InfoFloor1 info=look.vi;
            CodeBook[] books=vb.vd.fullbooks;

            /* unpack wrapped/predicted values from stream */
            if(vb.opb.read(1)==1){
                int[] fit_value=null;
                if(memo instanceof int[]){
                    fit_value=(int[])memo;
                }
                if(fit_value==null||fit_value.length<look.posts){
                    fit_value=new int[look.posts];
                }
                else{
                    for(int i=0; i<fit_value.length; i++)
                        fit_value[i]=0;
                }

                fit_value[0]=vb.opb.read(Util.ilog(look.quant_q-1));
                fit_value[1]=vb.opb.read(Util.ilog(look.quant_q-1));

                /* partition by partition */
                for(int i=0, j=2; i<info.partitions; i++){
                    int clss=info.partitionclass[i];
                    int cdim=info.class_dim[clss];
                    int csubbits=info.class_subs[clss];
                    int csub=1<<csubbits;
                    int cval=0;

                    /* decode the partition's first stage cascade value */
                    if(csubbits!=0){
                        cval=books[info.class_book[clss]].decode(vb.opb);

                        if(cval==-1){
                            return (null);
                        }
                    }

                    for(int k=0; k<cdim; k++){
                        int book=info.class_subbook[clss][cval&(csub-1)];
                        cval>>>=csubbits;
                        if(book>=0){
                            if((fit_value[j+k]=books[book].decode(vb.opb))==-1){
                                return (null);
                            }
                        }
                        else{
                            fit_value[j+k]=0;
                        }
                    }
                    j+=cdim;
                }

                /* unwrap positive values and reconsitute via linear interpolation */
                for(int i=2; i<look.posts; i++){
                    int predicted=render_point(info.postlist[look.loneighbor[i-2]],
                            info.postlist[look.hineighbor[i-2]],
                            fit_value[look.loneighbor[i-2]], fit_value[look.hineighbor[i-2]],
                            info.postlist[i]);
                    int hiroom=look.quant_q-predicted;
                    int loroom=predicted;
                    int room=(hiroom<loroom ? hiroom : loroom)<<1;
                    int val=fit_value[i];

                    if(val!=0){
                        if(val>=room){
                            if(hiroom>loroom){
                                val=val-loroom;
                            }
                            else{
                                val=-1-(val-hiroom);
                            }
                        }
                        else{
                            if((val&1)!=0){
                                val=-((val+1)>>>1);
                            }
                            else{
                                val>>=1;
                            }
                        }

                        fit_value[i]=val+predicted;
                        fit_value[look.loneighbor[i-2]]&=0x7fff;
                        fit_value[look.hineighbor[i-2]]&=0x7fff;
                    }
                    else{
                        fit_value[i]=predicted|0x8000;
                    }
                }
                return (fit_value);
            }

            return (null);
        }

        static int render_point(int x0, int x1, int y0, int y1, int x){
            y0&=0x7fff; /* mask off flag */
            y1&=0x7fff;

            {
                int dy=y1-y0;
                int adx=x1-x0;
                int ady=Math.abs(dy);
                int err=ady*(x-x0);

                int off=(int)(err/adx);
                if(dy<0)
                    return (y0-off);
                return (y0+off);
            }
        }

        int inverse2(Block vb, Object i, Object memo, float[] out){
            Floor1.LookFloor1 look=(Floor1.LookFloor1)i;
            Floor1.InfoFloor1 info=look.vi;
            int n=vb.vd.vi.blocksizes[vb.mode]/2;

            if(memo!=null){
                /* render the lines */
                int[] fit_value=(int[])memo;
                int hx=0;
                int lx=0;
                int ly=fit_value[0]*info.mult;
                for(int j=1; j<look.posts; j++){
                    int current=look.forward_index[j];
                    int hy=fit_value[current]&0x7fff;
                    if(hy==fit_value[current]){
                        hy*=info.mult;
                        hx=info.postlist[current];

                        render_line(lx, hx, ly, hy, out);

                        lx=hx;
                        ly=hy;
                    }
                }
                for(int j=hx; j<n; j++){
                    out[j]*=out[j-1]; /* be certain */
                }
                return (1);
            }
            for(int j=0; j<n; j++){
                out[j]=0.f;
            }
            return (0);
        }

        static float[] FLOOR_fromdB_LOOKUP= {1.0649863e-07F, 1.1341951e-07F,
                1.2079015e-07F, 1.2863978e-07F, 1.3699951e-07F, 1.4590251e-07F,
                1.5538408e-07F, 1.6548181e-07F, 1.7623575e-07F, 1.8768855e-07F,
                1.9988561e-07F, 2.128753e-07F, 2.2670913e-07F, 2.4144197e-07F,
                2.5713223e-07F, 2.7384213e-07F, 2.9163793e-07F, 3.1059021e-07F,
                3.3077411e-07F, 3.5226968e-07F, 3.7516214e-07F, 3.9954229e-07F,
                4.2550680e-07F, 4.5315863e-07F, 4.8260743e-07F, 5.1396998e-07F,
                5.4737065e-07F, 5.8294187e-07F, 6.2082472e-07F, 6.6116941e-07F,
                7.0413592e-07F, 7.4989464e-07F, 7.9862701e-07F, 8.5052630e-07F,
                9.0579828e-07F, 9.6466216e-07F, 1.0273513e-06F, 1.0941144e-06F,
                1.1652161e-06F, 1.2409384e-06F, 1.3215816e-06F, 1.4074654e-06F,
                1.4989305e-06F, 1.5963394e-06F, 1.7000785e-06F, 1.8105592e-06F,
                1.9282195e-06F, 2.0535261e-06F, 2.1869758e-06F, 2.3290978e-06F,
                2.4804557e-06F, 2.6416497e-06F, 2.8133190e-06F, 2.9961443e-06F,
                3.1908506e-06F, 3.3982101e-06F, 3.6190449e-06F, 3.8542308e-06F,
                4.1047004e-06F, 4.3714470e-06F, 4.6555282e-06F, 4.9580707e-06F,
                5.2802740e-06F, 5.6234160e-06F, 5.9888572e-06F, 6.3780469e-06F,
                6.7925283e-06F, 7.2339451e-06F, 7.7040476e-06F, 8.2047000e-06F,
                8.7378876e-06F, 9.3057248e-06F, 9.9104632e-06F, 1.0554501e-05F,
                1.1240392e-05F, 1.1970856e-05F, 1.2748789e-05F, 1.3577278e-05F,
                1.4459606e-05F, 1.5399272e-05F, 1.6400004e-05F, 1.7465768e-05F,
                1.8600792e-05F, 1.9809576e-05F, 2.1096914e-05F, 2.2467911e-05F,
                2.3928002e-05F, 2.5482978e-05F, 2.7139006e-05F, 2.8902651e-05F,
                3.0780908e-05F, 3.2781225e-05F, 3.4911534e-05F, 3.7180282e-05F,
                3.9596466e-05F, 4.2169667e-05F, 4.4910090e-05F, 4.7828601e-05F,
                5.0936773e-05F, 5.4246931e-05F, 5.7772202e-05F, 6.1526565e-05F,
                6.5524908e-05F, 6.9783085e-05F, 7.4317983e-05F, 7.9147585e-05F,
                8.4291040e-05F, 8.9768747e-05F, 9.5602426e-05F, 0.00010181521F,
                0.00010843174F, 0.00011547824F, 0.00012298267F, 0.00013097477F,
                0.00013948625F, 0.00014855085F, 0.00015820453F, 0.00016848555F,
                0.00017943469F, 0.00019109536F, 0.00020351382F, 0.00021673929F,
                0.00023082423F, 0.00024582449F, 0.00026179955F, 0.00027881276F,
                0.00029693158F, 0.00031622787F, 0.00033677814F, 0.00035866388F,
                0.00038197188F, 0.00040679456F, 0.00043323036F, 0.00046138411F,
                0.00049136745F, 0.00052329927F, 0.00055730621F, 0.00059352311F,
                0.00063209358F, 0.00067317058F, 0.00071691700F, 0.00076350630F,
                0.00081312324F, 0.00086596457F, 0.00092223983F, 0.00098217216F,
                0.0010459992F, 0.0011139742F, 0.0011863665F, 0.0012634633F,
                0.0013455702F, 0.0014330129F, 0.0015261382F, 0.0016253153F,
                0.0017309374F, 0.0018434235F, 0.0019632195F, 0.0020908006F,
                0.0022266726F, 0.0023713743F, 0.0025254795F, 0.0026895994F,
                0.0028643847F, 0.0030505286F, 0.0032487691F, 0.0034598925F,
                0.0036847358F, 0.0039241906F, 0.0041792066F, 0.0044507950F,
                0.0047400328F, 0.0050480668F, 0.0053761186F, 0.0057254891F,
                0.0060975636F, 0.0064938176F, 0.0069158225F, 0.0073652516F,
                0.0078438871F, 0.0083536271F, 0.0088964928F, 0.009474637F, 0.010090352F,
                0.010746080F, 0.011444421F, 0.012188144F, 0.012980198F, 0.013823725F,
                0.014722068F, 0.015678791F, 0.016697687F, 0.017782797F, 0.018938423F,
                0.020169149F, 0.021479854F, 0.022875735F, 0.024362330F, 0.025945531F,
                0.027631618F, 0.029427276F, 0.031339626F, 0.033376252F, 0.035545228F,
                0.037855157F, 0.040315199F, 0.042935108F, 0.045725273F, 0.048696758F,
                0.051861348F, 0.055231591F, 0.058820850F, 0.062643361F, 0.066714279F,
                0.071049749F, 0.075666962F, 0.080584227F, 0.085821044F, 0.091398179F,
                0.097337747F, 0.10366330F, 0.11039993F, 0.11757434F, 0.12521498F,
                0.13335215F, 0.14201813F, 0.15124727F, 0.16107617F, 0.17154380F,
                0.18269168F, 0.19456402F, 0.20720788F, 0.22067342F, 0.23501402F,
                0.25028656F, 0.26655159F, 0.28387361F, 0.30232132F, 0.32196786F,
                0.34289114F, 0.36517414F, 0.38890521F, 0.41417847F, 0.44109412F,
                0.46975890F, 0.50028648F, 0.53279791F, 0.56742212F, 0.60429640F,
                0.64356699F, 0.68538959F, 0.72993007F, 0.77736504F, 0.82788260F,
                0.88168307F, 0.9389798F, 1.F};

        static void render_line(int x0, int x1, int y0, int y1, float[] d){
            int dy=y1-y0;
            int adx=x1-x0;
            int ady=Math.abs(dy);
            int base=dy/adx;
            int sy=(dy<0 ? base-1 : base+1);
            int x=x0;
            int y=y0;
            int err=0;

            ady-=Math.abs(base*adx);

            d[x]*=FLOOR_fromdB_LOOKUP[y];
            while(++x<x1){
                err=err+ady;
                if(err>=adx){
                    err-=adx;
                    y+=sy;
                }
                else{
                    y+=base;
                }
                d[x]*=FLOOR_fromdB_LOOKUP[y];
            }
        }

        class InfoFloor1{
            static final int VIF_POSIT=63;
            static final int VIF_CLASS=16;
            static final int VIF_PARTS=31;

            int partitions; /* 0 to 31 */
            int[] partitionclass=new int[VIF_PARTS]; /* 0 to 15 */

            int[] class_dim=new int[VIF_CLASS]; /* 1 to 8 */
            int[] class_subs=new int[VIF_CLASS]; /* 0,1,2,3 (bits: 1<<n poss) */
            int[] class_book=new int[VIF_CLASS]; /* subs ^ dim entries */
            int[][] class_subbook=new int[VIF_CLASS][]; /* [VIF_CLASS][subs] */

            int mult; /* 1 2 3 or 4 */
            int[] postlist=new int[VIF_POSIT+2]; /* first two implicit */

            /* encode side analysis parameters */
            float maxover;
            float maxunder;
            float maxerr;

            int twofitminsize;
            int twofitminused;
            int twofitweight;
            float twofitatten;
            int unusedminsize;
            int unusedmin_n;

            int n;

            InfoFloor1(){
                for(int i=0; i<class_subbook.length; i++){
                    class_subbook[i]=new int[8];
                }
            }

            void free(){
                partitionclass=null;
                class_dim=null;
                class_subs=null;
                class_book=null;
                class_subbook=null;
                postlist=null;
            }

            Object copy_info(){
                Floor1.InfoFloor1 info=this;
                Floor1.InfoFloor1 ret=new Floor1.InfoFloor1();

                ret.partitions=info.partitions;
                System
                        .arraycopy(info.partitionclass, 0, ret.partitionclass, 0, VIF_PARTS);
                System.arraycopy(info.class_dim, 0, ret.class_dim, 0, VIF_CLASS);
                System.arraycopy(info.class_subs, 0, ret.class_subs, 0, VIF_CLASS);
                System.arraycopy(info.class_book, 0, ret.class_book, 0, VIF_CLASS);

                for(int j=0; j<VIF_CLASS; j++){
                    System.arraycopy(info.class_subbook[j], 0, ret.class_subbook[j], 0, 8);
                }

                ret.mult=info.mult;
                System.arraycopy(info.postlist, 0, ret.postlist, 0, VIF_POSIT+2);

                ret.maxover=info.maxover;
                ret.maxunder=info.maxunder;
                ret.maxerr=info.maxerr;

                ret.twofitminsize=info.twofitminsize;
                ret.twofitminused=info.twofitminused;
                ret.twofitweight=info.twofitweight;
                ret.twofitatten=info.twofitatten;
                ret.unusedminsize=info.unusedminsize;
                ret.unusedmin_n=info.unusedmin_n;

                ret.n=info.n;

                return (ret);
            }

        }

        class LookFloor1{
            static final int VIF_POSIT=63;

            int[] sorted_index=new int[VIF_POSIT+2];
            int[] forward_index=new int[VIF_POSIT+2];
            int[] reverse_index=new int[VIF_POSIT+2];
            int[] hineighbor=new int[VIF_POSIT];
            int[] loneighbor=new int[VIF_POSIT];
            int posts;

            int n;
            int quant_q;
            Floor1.InfoFloor1 vi;

            int phrasebits;
            int postbits;
            int frames;

            void free(){
                sorted_index=null;
                forward_index=null;
                reverse_index=null;
                hineighbor=null;
                loneighbor=null;
            }
        }

        class Lsfit_acc{
            long x0;
            long x1;

            long xa;
            long ya;
            long x2a;
            long y2a;
            long xya;
            long n;
            long an;
            long un;
            long edgey0;
            long edgey1;
        }

        class EchstateFloor1{
            int[] codewords;
            float[] curve;
            long frameno;
            long codes;
        }
    }

    abstract static class FuncFloor {

        public static FuncFloor[] floor_P = {new Floor0(), new Floor1()};

        abstract void pack(Object i, VorbisBuffer opb);

        abstract Object unpack(Info vi, VorbisBuffer opb);

        abstract Object look(DspState vd, InfoMode mi, Object i);

        abstract void free_info(Object i);

        abstract void free_look(Object i);

        abstract void free_state(Object vs);

        abstract int forward(Block vb, Object i, float[] in, float[] out, Object vs);

        abstract Object inverse1(Block vb, Object i, Object memo);

        abstract int inverse2(Block vb, Object i, Object memo, float[] out);
    }

    abstract static class FuncResidue {
        public static FuncResidue[] residue_P = {new Residue0(), new Residue1(),
                new Residue2()};

        abstract void pack(Object vr, VorbisBuffer opb);

        abstract Object unpack(Info vi, VorbisBuffer opb);

        abstract Object look(DspState vd, InfoMode vm, Object vr);

        abstract void free_info(Object i);

        abstract void free_look(Object i);

        abstract int inverse(Block vb, Object vl, float[][] in, int[] nonzero, int ch);
    }

    static class Residue0 extends FuncResidue {
        void pack(Object vr, VorbisBuffer opb) {
            Residue0.InfoResidue0 info = (Residue0.InfoResidue0) vr;
            int acc = 0;
            opb.write(info.begin, 24);
            opb.write(info.end, 24);

            opb.write(info.grouping - 1, 24); /* residue vectors to group and
          			     code with a partitioned book */
            opb.write(info.partitions - 1, 6); /* possible partition choices */
            opb.write(info.groupbook, 8); /* group huffman book */

    /* secondstages is a bitmask; as encoding progresses pass by pass, a
       bitmask of one indicates this partition class has bits to write
       this pass */
            for (int j = 0; j < info.partitions; j++) {
                int i = info.secondstages[j];
                if (Util.ilog(i) > 3) {
                    /* yes, this is a minor hack due to not thinking ahead */
                    opb.write(i, 3);
                    opb.write(1, 1);
                    opb.write(i >>> 3, 5);
                } else {
                    opb.write(i, 4); /* trailing zero */
                }
                acc += Util.icount(i);
            }
            for (int j = 0; j < acc; j++) {
                opb.write(info.booklist[j], 8);
            }
        }

        Object unpack(Info vi, VorbisBuffer opb) {
            int acc = 0;
            Residue0.InfoResidue0 info = new Residue0.InfoResidue0();
            info.begin = opb.read(24);
            info.end = opb.read(24);
            info.grouping = opb.read(24) + 1;
            info.partitions = opb.read(6) + 1;
            info.groupbook = opb.read(8);

            for (int j = 0; j < info.partitions; j++) {
                int cascade = opb.read(3);
                if (opb.read(1) != 0) {
                    cascade |= (opb.read(5) << 3);
                }
                info.secondstages[j] = cascade;
                acc += Util.icount(cascade);
            }

            for (int j = 0; j < acc; j++) {
                info.booklist[j] = opb.read(8);
            }

            if (info.groupbook >= vi.books) {
                free_info(info);
                return (null);
            }

            for (int j = 0; j < acc; j++) {
                if (info.booklist[j] >= vi.books) {
                    free_info(info);
                    return (null);
                }
            }
            return (info);
        }

        Object look(DspState vd, InfoMode vm, Object vr) {
            Residue0.InfoResidue0 info = (Residue0.InfoResidue0) vr;
            Residue0.LookResidue0 look = new LookResidue0();
            int acc = 0;
            int dim;
            int maxstage = 0;
            look.info = info;
            look.map = vm.mapping;

            look.parts = info.partitions;
            look.fullbooks = vd.fullbooks;
            look.phrasebook = vd.fullbooks[info.groupbook];

            dim = look.phrasebook.dim;

            look.partbooks = new int[look.parts][];

            for (int j = 0; j < look.parts; j++) {
                int i = info.secondstages[j];
                int stages = Util.ilog(i);
                if (stages != 0) {
                    if (stages > maxstage)
                        maxstage = stages;
                    look.partbooks[j] = new int[stages];
                    for (int k = 0; k < stages; k++) {
                        if ((i & (1 << k)) != 0) {
                            look.partbooks[j][k] = info.booklist[acc++];
                        }
                    }
                }
            }

            look.partvals = (int) Math.rint(Math.pow(look.parts, dim));
            look.stages = maxstage;
            look.decodemap = new int[look.partvals][];
            for (int j = 0; j < look.partvals; j++) {
                int val = j;
                int mult = look.partvals / look.parts;
                look.decodemap[j] = new int[dim];

                for (int k = 0; k < dim; k++) {
                    int deco = val / mult;
                    val -= deco * mult;
                    mult /= look.parts;
                    look.decodemap[j][k] = deco;
                }
            }
            return (look);
        }

        void free_info(Object i) {
        }

        void free_look(Object i) {
        }

        static int[][][] _01inverse_partword = new int[2][][]; // _01inverse is synchronized for

        // re-using partword
        synchronized static int _01inverse(Block vb, Object vl, float[][] in, int ch,
                                           int decodepart) {
            int i, j, k, l, s;
            Residue0.LookResidue0 look = (Residue0.LookResidue0) vl;
            Residue0.InfoResidue0 info = look.info;

            // move all this setup out later
            int samples_per_partition = info.grouping;
            int partitions_per_word = look.phrasebook.dim;
            int n = info.end - info.begin;

            int partvals = n / samples_per_partition;
            int partwords = (partvals + partitions_per_word - 1) / partitions_per_word;

            if (_01inverse_partword.length < ch) {
                _01inverse_partword = new int[ch][][];
            }

            for (j = 0; j < ch; j++) {
                if (_01inverse_partword[j] == null || _01inverse_partword[j].length < partwords) {
                    _01inverse_partword[j] = new int[partwords][];
                }
            }

            for (s = 0; s < look.stages; s++) {
                // each loop decodes on partition codeword containing
                // partitions_pre_word partitions
                for (i = 0, l = 0; i < partvals; l++) {
                    if (s == 0) {
                        // fetch the partition word for each channel
                        for (j = 0; j < ch; j++) {
                            int temp = look.phrasebook.decode(vb.opb);
                            if (temp == -1) {
                                return (0);
                            }
                            _01inverse_partword[j][l] = look.decodemap[temp];
                            if (_01inverse_partword[j][l] == null) {
                                return (0);
                            }
                        }
                    }

                    // now we decode residual values for the partitions
                    for (k = 0; k < partitions_per_word && i < partvals; k++, i++)
                        for (j = 0; j < ch; j++) {
                            int offset = info.begin + i * samples_per_partition;
                            int index = _01inverse_partword[j][l][k];
                            if ((info.secondstages[index] & (1 << s)) != 0) {
                                CodeBook stagebook = look.fullbooks[look.partbooks[index][s]];
                                if (stagebook != null) {
                                    if (decodepart == 0) {
                                        if (stagebook.decodevs_add(in[j], offset, vb.opb,
                                                samples_per_partition) == -1) {
                                            return (0);
                                        }
                                    } else if (decodepart == 1) {
                                        if (stagebook.decodev_add(in[j], offset, vb.opb,
                                                samples_per_partition) == -1) {
                                            return (0);
                                        }
                                    }
                                }
                            }
                        }
                }
            }
            return (0);
        }

        static int[][] _2inverse_partword = null;

        synchronized static int _2inverse(Block vb, Object vl, float[][] in, int ch) {
            int i, k, l, s;
            Residue0.LookResidue0 look = (Residue0.LookResidue0) vl;
            Residue0.InfoResidue0 info = look.info;

            // move all this setup out later
            int samples_per_partition = info.grouping;
            int partitions_per_word = look.phrasebook.dim;
            int n = info.end - info.begin;

            int partvals = n / samples_per_partition;
            int partwords = (partvals + partitions_per_word - 1) / partitions_per_word;

            if (_2inverse_partword == null || _2inverse_partword.length < partwords) {
                _2inverse_partword = new int[partwords][];
            }
            for (s = 0; s < look.stages; s++) {
                for (i = 0, l = 0; i < partvals; l++) {
                    if (s == 0) {
                        // fetch the partition word for each channel
                        int temp = look.phrasebook.decode(vb.opb);
                        if (temp == -1) {
                            return (0);
                        }
                        _2inverse_partword[l] = look.decodemap[temp];
                        if (_2inverse_partword[l] == null) {
                            return (0);
                        }
                    }

                    // now we decode residual values for the partitions
                    for (k = 0; k < partitions_per_word && i < partvals; k++, i++) {
                        int offset = info.begin + i * samples_per_partition;
                        int index = _2inverse_partword[l][k];
                        if ((info.secondstages[index] & (1 << s)) != 0) {
                            CodeBook stagebook = look.fullbooks[look.partbooks[index][s]];
                            if (stagebook != null) {
                                if (stagebook.decodevv_add(in, offset, ch, vb.opb,
                                        samples_per_partition) == -1) {
                                    return (0);
                                }
                            }
                        }
                    }
                }
            }
            return (0);
        }

        int inverse(Block vb, Object vl, float[][] in, int[] nonzero, int ch) {
            int used = 0;
            for (int i = 0; i < ch; i++) {
                if (nonzero[i] != 0) {
                    in[used++] = in[i];
                }
            }
            if (used != 0)
                return (_01inverse(vb, vl, in, used, 0));
            else
                return (0);
        }

        static class LookResidue0 {
            Residue0.InfoResidue0 info;
            int map;

            int parts;
            int stages;
            CodeBook[] fullbooks;
            CodeBook phrasebook;
            int[][] partbooks;

            int partvals;
            int[][] decodemap;

            int postbits;
            int phrasebits;
            int frames;
        }

        class InfoResidue0 {
            // block-partitioned VQ coded straight residue
            int begin;
            int end;

            // first stage (lossless partitioning)
            int grouping; // group n vectors per partition
            int partitions; // possible codebooks for a partition
            int groupbook; // huffbook for partitioning
            int[] secondstages = new int[64]; // expanded out to pointers in lookup
            int[] booklist = new int[256]; // list of second stage books

            // encode-only heuristic settings
            float[] entmax = new float[64]; // book entropy threshholds
            float[] ampmax = new float[64]; // book amp threshholds
            int[] subgrp = new int[64]; // book heuristic subgroup size
            int[] blimit = new int[64]; // subgroup position limits
        }

    }

    static class Residue1 extends Residue0 {

        int inverse(Block vb, Object vl, float[][] in, int[] nonzero, int ch) {
            int used = 0;
            for (int i = 0; i < ch; i++) {
                if (nonzero[i] != 0) {
                    in[used++] = in[i];
                }
            }
            if (used != 0) {
                return (_01inverse(vb, vl, in, used, 1));
            } else {
                return 0;
            }
        }
    }

    static class Residue2 extends Residue0 {

        int inverse(Block vb, Object vl, float[][] in, int[] nonzero, int ch) {
            int i = 0;
            for (i = 0; i < ch; i++)
                if (nonzero[i] != 0)
                    break;
            if (i == ch)
                return (0); /* no nonzero vectors */

            return (_2inverse(vb, vl, in, ch));
        }
    }

    static class CodeBook {
        int dim; // codebook dimensions (elements per vector)
        int entries; // codebook entries
        StaticCodeBook c = new StaticCodeBook();

        float[] valuelist; // list of dim*entries actual entry values
        int[] codelist; // list of bitstream codewords for each entry
        CodeBook.DecodeAux decode_tree;

        // returns the number of bits
        int encode(int a, VorbisBuffer b) {
            b.write(codelist[a], c.lengthlist[a]);
            return (c.lengthlist[a]);
        }

        // One the encode side, our vector writers are each designed for a
        // specific purpose, and the encoder is not flexible without modification:
        //
        // The LSP vector coder uses a single stage nearest-match with no
        // interleave, so no step and no error return.  This is specced by floor0
        // and doesn't change.
        //
        // Residue0 encoding interleaves, uses multiple stages, and each stage
        // peels of a specific amount of resolution from a lattice (thus we want
        // to match by threshhold, not nearest match).  Residue doesn't *have* to
        // be encoded that way, but to change it, one will need to add more
        // infrastructure on the encode side (decode side is specced and simpler)

        // floor0 LSP (single stage, non interleaved, nearest match)
        // returns entry number and *modifies a* to the quantization value
        int errorv(float[] a) {
            int best = best(a, 1);
            for (int k = 0; k < dim; k++) {
                a[k] = valuelist[best * dim + k];
            }
            return (best);
        }

        // returns the number of bits and *modifies a* to the quantization value
        int encodev(int best, float[] a, VorbisBuffer b) {
            for (int k = 0; k < dim; k++) {
                a[k] = valuelist[best * dim + k];
            }
            return (encode(best, b));
        }

        // res0 (multistage, interleave, lattice)
        // returns the number of bits and *modifies a* to the remainder value
        int encodevs(float[] a, VorbisBuffer b, int step, int addmul) {
            int best = besterror(a, step, addmul);
            return (encode(best, b));
        }

        private int[] t = new int[15]; // decodevs_add is synchronized for re-using t.

        synchronized int decodevs_add(float[] a, int offset, VorbisBuffer b, int n) {
            int step = n / dim;
            int entry;
            int i, j, o;

            if (t.length < step) {
                t = new int[step];
            }

            for (i = 0; i < step; i++) {
                entry = decode(b);
                if (entry == -1)
                    return (-1);
                t[i] = entry * dim;
            }
            for (i = 0, o = 0; i < dim; i++, o += step) {
                for (j = 0; j < step; j++) {
                    a[offset + o + j] += valuelist[t[j] + i];
                }
            }

            return (0);
        }

        int decodev_add(float[] a, int offset, VorbisBuffer b, int n) {
            int i, j, entry;
            int t;

            if (dim > 8) {
                for (i = 0; i < n; ) {
                    entry = decode(b);
                    if (entry == -1)
                        return (-1);
                    t = entry * dim;
                    for (j = 0; j < dim; ) {
                        a[offset + (i++)] += valuelist[t + (j++)];
                    }
                }
            } else {
                for (i = 0; i < n; ) {
                    entry = decode(b);
                    if (entry == -1)
                        return (-1);
                    t = entry * dim;
                    j = 0;
                    switch (dim) {
                        case 8:
                            a[offset + (i++)] += valuelist[t + (j++)];
                        case 7:
                            a[offset + (i++)] += valuelist[t + (j++)];
                        case 6:
                            a[offset + (i++)] += valuelist[t + (j++)];
                        case 5:
                            a[offset + (i++)] += valuelist[t + (j++)];
                        case 4:
                            a[offset + (i++)] += valuelist[t + (j++)];
                        case 3:
                            a[offset + (i++)] += valuelist[t + (j++)];
                        case 2:
                            a[offset + (i++)] += valuelist[t + (j++)];
                        case 1:
                            a[offset + (i++)] += valuelist[t + (j++)];
                        case 0:
                            break;
                    }
                }
            }
            return (0);
        }

        int decodev_set(float[] a, int offset, VorbisBuffer b, int n) {
            int i, j, entry;
            int t;

            for (i = 0; i < n; ) {
                entry = decode(b);
                if (entry == -1)
                    return (-1);
                t = entry * dim;
                for (j = 0; j < dim; ) {
                    a[offset + i++] = valuelist[t + (j++)];
                }
            }
            return (0);
        }

        int decodevv_add(float[][] a, int offset, int ch, VorbisBuffer b, int n) {
            int i, j, entry;
            int chptr = 0;

            for (i = offset / ch; i < (offset + n) / ch; ) {
                entry = decode(b);
                if (entry == -1)
                    return (-1);

                int t = entry * dim;
                for (j = 0; j < dim; j++) {
                    a[chptr++][i] += valuelist[t + j];
                    if (chptr == ch) {
                        chptr = 0;
                        i++;
                    }
                }
            }
            return (0);
        }

        // Decode side is specced and easier, because we don't need to find
        // matches using different criteria; we simply read and map.  There are
        // two things we need to do 'depending':
        //
        // We may need to support interleave.  We don't really, but it's
        // convenient to do it here rather than rebuild the vector later.
        //
        // Cascades may be additive or multiplicitive; this is not inherent in
        // the codebook, but set in the code using the codebook.  Like
        // interleaving, it's easiest to do it here.
        // stage==0 -> declarative (set the value)
        // stage==1 -> additive
        // stage==2 -> multiplicitive

        // returns the entry number or -1 on eof
        int decode(VorbisBuffer b) {
            int ptr = 0;
            CodeBook.DecodeAux t = decode_tree;
            int lok = b.look(t.tabn);

            if (lok >= 0) {
                ptr = t.tab[lok];
                b.adv(t.tabl[lok]);
                if (ptr <= 0) {
                    return -ptr;
                }
            }
            do {
                switch (b.read1()) {
                    case 0:
                        ptr = t.ptr0[ptr];
                        break;
                    case 1:
                        ptr = t.ptr1[ptr];
                        break;
                    case -1:
                    default:
                        return (-1);
                }
            }
            while (ptr > 0);
            return (-ptr);
        }

        // returns the entry number or -1 on eof
        int decodevs(float[] a, int index, VorbisBuffer b, int step, int addmul) {
            int entry = decode(b);
            if (entry == -1)
                return (-1);
            switch (addmul) {
                case -1:
                    for (int i = 0, o = 0; i < dim; i++, o += step)
                        a[index + o] = valuelist[entry * dim + i];
                    break;
                case 0:
                    for (int i = 0, o = 0; i < dim; i++, o += step)
                        a[index + o] += valuelist[entry * dim + i];
                    break;
                case 1:
                    for (int i = 0, o = 0; i < dim; i++, o += step)
                        a[index + o] *= valuelist[entry * dim + i];
                    break;
                default:
                    //System.err.println("CodeBook.decodeves: addmul="+addmul);
            }
            return (entry);
        }

        int best(float[] a, int step) {
            // brute force it!
            {
                int besti = -1;
                float best = 0.f;
                int e = 0;
                for (int i = 0; i < entries; i++) {
                    if (c.lengthlist[i] > 0) {
                        float _this = dist(dim, valuelist, e, a, step);
                        if (besti == -1 || _this < best) {
                            best = _this;
                            besti = i;
                        }
                    }
                    e += dim;
                }
                return (besti);
            }
        }

        // returns the entry number and *modifies a* to the remainder value
        int besterror(float[] a, int step, int addmul) {
            int best = best(a, step);
            switch (addmul) {
                case 0:
                    for (int i = 0, o = 0; i < dim; i++, o += step)
                        a[o] -= valuelist[best * dim + i];
                    break;
                case 1:
                    for (int i = 0, o = 0; i < dim; i++, o += step) {
                        float val = valuelist[best * dim + i];
                        if (val == 0) {
                            a[o] = 0;
                        } else {
                            a[o] /= val;
                        }
                    }
                    break;
            }
            return (best);
        }

        void clear() {
        }

        static float dist(int el, float[] ref, int index, float[] b, int step) {
            float acc = (float) 0.;
            for (int i = 0; i < el; i++) {
                float val = (ref[index + i] - b[i * step]);
                acc += val * val;
            }
            return (acc);
        }

        int init_decode(StaticCodeBook s) {
            c = s;
            entries = s.entries;
            dim = s.dim;
            valuelist = s.unquantize();

            decode_tree = make_decode_tree();
            if (decode_tree == null) {
                clear();
                return (-1);
            }
            return (0);
        }

        // given a list of word lengths, generate a list of codewords.  Works
        // for length ordered or unordered, always assigns the lowest valued
        // codewords first.  Extended to handle unused entries (length 0)
        static int[] make_words(int[] l, int n) {
            int[] marker = new int[33];
            int[] r = new int[n];

            for (int i = 0; i < n; i++) {
                int length = l[i];
                if (length > 0) {
                    int entry = marker[length];

                    // when we claim a node for an entry, we also claim the nodes
                    // below it (pruning off the imagined tree that may have dangled
                    // from it) as well as blocking the use of any nodes directly
                    // above for leaves

                    // update ourself
                    if (length < 32 && (entry >>> length) != 0) {
                        // error condition; the lengths must specify an overpopulated tree
                        //free(r);
                        return (null);
                    }
                    r[i] = entry;

                    // Look to see if the next shorter marker points to the node
                    // above. if so, update it and repeat.
                    {
                        for (int j = length; j > 0; j--) {
                            if ((marker[j] & 1) != 0) {
                                // have to jump branches
                                if (j == 1)
                                    marker[1]++;
                                else
                                    marker[j] = marker[j - 1] << 1;
                                break; // invariant says next upper marker would already
                                // have been moved if it was on the same path
                            }
                            marker[j]++;
                        }
                    }

                    // prune the tree; the implicit invariant says all the longer
                    // markers were dangling from our just-taken node.  Dangle them
                    // from our *new* node.
                    for (int j = length + 1; j < 33; j++) {
                        if ((marker[j] >>> 1) == entry) {
                            entry = marker[j];
                            marker[j] = marker[j - 1] << 1;
                        } else {
                            break;
                        }
                    }
                }
            }

            // bitreverse the words because our bitwise packer/unpacker is LSb
            // endian
            for (int i = 0; i < n; i++) {
                int temp = 0;
                for (int j = 0; j < l[i]; j++) {
                    temp <<= 1;
                    temp |= (r[i] >>> j) & 1;
                }
                r[i] = temp;
            }

            return (r);
        }

        // build the decode helper tree from the codewords
        CodeBook.DecodeAux make_decode_tree() {
            int top = 0;
            CodeBook.DecodeAux t = new CodeBook.DecodeAux();
            int[] ptr0 = t.ptr0 = new int[entries * 2];
            int[] ptr1 = t.ptr1 = new int[entries * 2];
            int[] codelist = make_words(c.lengthlist, c.entries);

            if (codelist == null)
                return (null);
            t.aux = entries * 2;

            for (int i = 0; i < entries; i++) {
                if (c.lengthlist[i] > 0) {
                    int ptr = 0;
                    int j;
                    for (j = 0; j < c.lengthlist[i] - 1; j++) {
                        int bit = (codelist[i] >>> j) & 1;
                        if (bit == 0) {
                            if (ptr0[ptr] == 0) {
                                ptr0[ptr] = ++top;
                            }
                            ptr = ptr0[ptr];
                        } else {
                            if (ptr1[ptr] == 0) {
                                ptr1[ptr] = ++top;
                            }
                            ptr = ptr1[ptr];
                        }
                    }

                    if (((codelist[i] >>> j) & 1) == 0) {
                        ptr0[ptr] = -i;
                    } else {
                        ptr1[ptr] = -i;
                    }

                }
            }

            t.tabn = Util.ilog(entries) - 4;

            if (t.tabn < 5)
                t.tabn = 5;
            int n = 1 << t.tabn;
            t.tab = new int[n];
            t.tabl = new int[n];
            for (int i = 0; i < n; i++) {
                int p = 0;
                int j = 0;
                for (j = 0; j < t.tabn && (p > 0 || j == 0); j++) {
                    if ((i & (1 << j)) != 0) {
                        p = ptr1[p];
                    } else {
                        p = ptr0[p];
                    }
                }
                t.tab[i] = p; // -code
                t.tabl[i] = j; // length
            }

            return (t);
        }

        class DecodeAux {
            int[] tab;
            int[] tabl;
            int tabn;

            int[] ptr0;
            int[] ptr1;
            int aux; // number of tree entries
        }
    }

    static class Node {

        public long key;
        public Node previous;
        public Node next;

        public void remove() {
            if (this.next != null) {
                this.next.previous = this.previous;
                this.previous.next = this.next;
                this.previous = null;
                this.next = null;
            }
        }

        public boolean hasNext() {
            return this.next != null;
        }
    }

    static class Mdct{

        int n;
        int log2n;

        float[] trig;
        int[] bitrev;

        float scale;

        void init(int n){
            bitrev=new int[n/4];
            trig=new float[n+n/4];

            log2n=(int)Math.rint(Math.log(n)/Math.log(2));
            this.n=n;

            int AE=0;
            int AO=1;
            int BE=AE+n/2;
            int BO=BE+1;
            int CE=BE+n/2;
            int CO=CE+1;
            // trig lookups...
            for(int i=0; i<n/4; i++){
                trig[AE+i*2]=(float)Math.cos((Math.PI/n)*(4*i));
                trig[AO+i*2]=(float)-Math.sin((Math.PI/n)*(4*i));
                trig[BE+i*2]=(float)Math.cos((Math.PI/(2*n))*(2*i+1));
                trig[BO+i*2]=(float)Math.sin((Math.PI/(2*n))*(2*i+1));
            }
            for(int i=0; i<n/8; i++){
                trig[CE+i*2]=(float)Math.cos((Math.PI/n)*(4*i+2));
                trig[CO+i*2]=(float)-Math.sin((Math.PI/n)*(4*i+2));
            }

            {
                int mask=(1<<(log2n-1))-1;
                int msb=1<<(log2n-2);
                for(int i=0; i<n/8; i++){
                    int acc=0;
                    for(int j=0; msb>>>j!=0; j++)
                        if(((msb>>>j)&i)!=0)
                            acc|=1<<j;
                    bitrev[i*2]=((~acc)&mask);
                    //	bitrev[i*2]=((~acc)&mask)-1;
                    bitrev[i*2+1]=acc;
                }
            }
            scale=4.f/n;
        }

        void clear(){
        }

        void forward(float[] in, float[] out){
        }

        float[] _x=new float[1024];
        float[] _w=new float[1024];

        synchronized void backward(float[] in, float[] out){
            if(_x.length<n/2){
                _x=new float[n/2];
            }
            if(_w.length<n/2){
                _w=new float[n/2];
            }
            float[] x=_x;
            float[] w=_w;
            int n2=n>>>1;
            int n4=n>>>2;
            int n8=n>>>3;

            // rotate + step 1
            {
                int inO=1;
                int xO=0;
                int A=n2;

                int i;
                for(i=0; i<n8; i++){
                    A-=2;
                    x[xO++]=-in[inO+2]*trig[A+1]-in[inO]*trig[A];
                    x[xO++]=in[inO]*trig[A+1]-in[inO+2]*trig[A];
                    inO+=4;
                }

                inO=n2-4;

                for(i=0; i<n8; i++){
                    A-=2;
                    x[xO++]=in[inO]*trig[A+1]+in[inO+2]*trig[A];
                    x[xO++]=in[inO]*trig[A]-in[inO+2]*trig[A+1];
                    inO-=4;
                }
            }

            float[] xxx=mdct_kernel(x, w, n, n2, n4, n8);
            int xx=0;

            // step 8

            {
                int B=n2;
                int o1=n4, o2=o1-1;
                int o3=n4+n2, o4=o3-1;

                for(int i=0; i<n4; i++){
                    float temp1=(xxx[xx]*trig[B+1]-xxx[xx+1]*trig[B]);
                    float temp2=-(xxx[xx]*trig[B]+xxx[xx+1]*trig[B+1]);

                    out[o1]=-temp1;
                    out[o2]=temp1;
                    out[o3]=temp2;
                    out[o4]=temp2;

                    o1++;
                    o2--;
                    o3++;
                    o4--;
                    xx+=2;
                    B+=2;
                }
            }
        }

        private float[] mdct_kernel(float[] x, float[] w, int n, int n2, int n4,
                                    int n8){
            // step 2

            int xA=n4;
            int xB=0;
            int w2=n4;
            int A=n2;

            for(int i=0; i<n4;){
                float x0=x[xA]-x[xB];
                float x1;
                w[w2+i]=x[xA++]+x[xB++];

                x1=x[xA]-x[xB];
                A-=4;

                w[i++]=x0*trig[A]+x1*trig[A+1];
                w[i]=x1*trig[A]-x0*trig[A+1];

                w[w2+i]=x[xA++]+x[xB++];
                i++;
            }

            // step 3

            {
                for(int i=0; i<log2n-3; i++){
                    int k0=n>>>(i+2);
                    int k1=1<<(i+3);
                    int wbase=n2-2;

                    A=0;
                    float[] temp;

                    for(int r=0; r<(k0>>>2); r++){
                        int w1=wbase;
                        w2=w1-(k0>>1);
                        float AEv=trig[A], wA;
                        float AOv=trig[A+1], wB;
                        wbase-=2;

                        k0++;
                        for(int s=0; s<(2<<i); s++){
                            wB=w[w1]-w[w2];
                            x[w1]=w[w1]+w[w2];

                            wA=w[++w1]-w[++w2];
                            x[w1]=w[w1]+w[w2];

                            x[w2]=wA*AEv-wB*AOv;
                            x[w2-1]=wB*AEv+wA*AOv;

                            w1-=k0;
                            w2-=k0;
                        }
                        k0--;
                        A+=k1;
                    }

                    temp=w;
                    w=x;
                    x=temp;
                }
            }

            // step 4, 5, 6, 7
            {
                int C=n;
                int bit=0;
                int x1=0;
                int x2=n2-1;

                for(int i=0; i<n8; i++){
                    int t1=bitrev[bit++];
                    int t2=bitrev[bit++];

                    float wA=w[t1]-w[t2+1];
                    float wB=w[t1-1]+w[t2];
                    float wC=w[t1]+w[t2+1];
                    float wD=w[t1-1]-w[t2];

                    float wACE=wA*trig[C];
                    float wBCE=wB*trig[C++];
                    float wACO=wA*trig[C];
                    float wBCO=wB*trig[C++];

                    x[x1++]=(wC+wACO+wBCE)*.5f;
                    x[x2--]=(-wD+wBCO-wACE)*.5f;
                    x[x1++]=(wD+wBCO-wACE)*.5f;
                    x[x2--]=(wC-wACO-wBCE)*.5f;
                }
            }
            return (x);
        }
    }

    /**
     * A class that connects to your own sound device in order to play audio out loud.
     */
    static class DevicePcmPlayer {

        /**
         * The Audio Format of the sound being output.
         */
        //public AudioFormat format;

        /**
         * The device being used to output sound to.
         */
        //public SourceDataLine line;

        /**
         * A byte array to write audio data samples to.
         */
        public byte[] byteSamples;

        /**
         * An integer array that is filled with data samples.
         */
        public int[] samples;

        /**
         * The stream used for sound.
         */
        public MidiStream stream;

        /**
         * An integer value determining the default sample rate for output audio.
         */
        public static int sampleRate = 44100;

        /**
         * A boolean value determining if the audio is stereo or not.
         */
        public static boolean stereo = true;

        public DataOutputStream dataOutputStream;

        /**
         * A method to set the default output audio format, as well as the empty byte array that sound will be written to later.
         */
        public void init() throws FileNotFoundException {
            //this.format = new AudioFormat((float) sampleRate, 16, stereo ? 2 : 1, true, false);
            this.byteSamples = new byte[256 << (stereo ? 2 : 1)];
            this.dataOutputStream = new DataOutputStream(new FileOutputStream("out.wav"));
        }

        /**
         * A method to get the default audio output device, open it, and start using it.
         * @throws LineUnavailableException Try to re-open the device for sound output. If not possible, an error occurs.
         */
        /*
        public void open() throws LineUnavailableException {
            try {
                DataLine.Info sourceDataLineInfo = new DataLine.Info(SourceDataLine.class, this.format, 8192);
                this.line = (SourceDataLine) AudioSystem.getLine(sourceDataLineInfo);
                this.line.open();
                this.line.start();
            } catch (LineUnavailableException lineUnavailableException) {
                if (this.line.available() != -1) {
                    this.open();
                } else {
                    this.line = null;
                    throw lineUnavailableException;
                }
            }
        }
        */

        /**
         * A method to set the default stream for the sound output.
         * @param audioStream The stream to set for playback.
         */
        public final void setStream(MidiStream audioStream) {
            this.stream = audioStream;
        }

        /**
         * A method that takes the currently set stream and fills an empty integer array with audio data.
         * @param samplesToWrite The sample integer array to write values to.
         * @param amount The amount of samples to write to the integer array.
         */
        public final void fill(int[] samplesToWrite, int amount) throws FileNotFoundException {
            Arrays.fill(samplesToWrite, 0);
            if (this.stream != null) {
                this.stream.fill(samplesToWrite, 0, amount);
            }
        }

        /**
         * A method to write audio data to the selected output sound device, which plays the sound out loud.
         */
        public void write() {
            int length = 256;
            if (DevicePcmPlayer.stereo) {
                length <<= 1;
            }

            for (int index = 0; index < length; ++index) {
                int sample = samples[index];
                if ((sample + 8388608 & -16777216) != 0) {
                    sample = 8388607 ^ sample >> 31;
                }

                this.byteSamples[index * 2] = (byte) (sample >> 8);
                this.byteSamples[index * 2 + 1] = (byte) (sample >> 16);
            }

            //this.line.write(this.byteSamples, 0, length << 1);
        }

        public void writeToBuffer() {
            int var1 = 256;
            if (stereo) {
                var1 <<= 1;
            }

            for (int var2 = 0; var2 < var1; ++var2) {
                int var3 = samples[var2];
                if ((var3 + 8388608 & -16777216) != 0) {
                    var3 = 8388607 ^ var3 >> 31;
                }

                this.byteSamples[var2 * 2] = (byte)(var3 >> 8);
                this.byteSamples[var2 * 2 + 1] = (byte)(var3 >> 16);
            }
            try {
                this.dataOutputStream.write(this.byteSamples, 0, var1 << 1);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}