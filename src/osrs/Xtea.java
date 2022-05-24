package osrs;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class Xtea {

    private static final int GOLDEN_RATIO = 0x9E3779B9;

    private static final int ROUNDS = 32;

    private final int[] keys;

    public Xtea(int[] keys)
    {
        this.keys = keys;
    }

    public byte[] decrypt(byte[] data) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(data, 0, data.length);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(byteArrayOutputStream);
        int numBlocks = data.length / 8;
        for (int block = 0; block < numBlocks; ++block)
        {
            int v0 = byteBuffer.getInt();
            int v1 = byteBuffer.getInt();
            int sum = GOLDEN_RATIO * ROUNDS;
            for (int i = 0; i < ROUNDS; ++i)
            {
                v1 -= (((v0 << 4) ^ (v0 >>> 5)) + v0) ^ (sum + keys[(sum >>> 11) & 3]);
                sum -= GOLDEN_RATIO;
                v0 -= (((v1 << 4) ^ (v1 >>> 5)) + v1) ^ (sum + keys[sum & 3]);
            }
            //out.writeInt(v0);
            //out.writeInt(v1);
        }
        byteBuffer.flip();
        try {
            out.write(byteBuffer.array());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return byteArrayOutputStream.toByteArray();
    }
}
