package redis.clients.util;

/**
 * @Author chenchen
 * @Date 2021/11/8 4:51 下午
 * @Description 定制crc32非加密哈希算法,兼容python crc32 实现
 **/
public class CRC32Hashing implements Hashing {

    private static final long[] crc32Table = new long[256];

    //每个桶的crc多项式值
    static {
        long crcValue;
        for (int i = 0; i < 256; i++) {
            crcValue = i;
            for (int j = 0; j < 8; j++) {
                //奇偶性校验
                if ((crcValue & 1) == 1) {
                    //模2<=>右移1位
                    crcValue = crcValue >> 1;
                    //和 1110 1101 1011 1000 1000 0011 0010 0000 进行异或
                    crcValue = 0x00000000edb88320L ^ crcValue;
                } else {
                    //模2<=>右移1位
                    crcValue = crcValue >> 1;
                }
            }
            crc32Table[i] = crcValue;
        }
    }


    @Override
    public long hash(String key) {
        return hash(SafeEncoder.encode(key));
    }

    @Override
    public long hash(byte[] key) {
        //value的初始值为2^32
        long value = 0x00000000ffffffffL;
        for (byte b : key) {
            //先对value 进行异或 再进行与运算
            int index = (int) ((value ^ b) & 0xff);
            // 算出来的桶对应下标 与 value右移 一个字节位的数据进行异或
            value = crc32Table[index] ^ (value >> 8);
        }
        // key 所有比特位 遍历完毕后 与 2^32进行异或
        value = value ^ 0x00000000ffffffffL;
        return value;
    }

    public static void main(String[] args) {
        System.out.println(new CRC32Hashing().hash("hb_v_1ab3a5de7d2c66c4643ed1d9eb659f9a"));
    }
}
