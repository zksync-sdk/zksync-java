package im.argent.zksync.signer;

import im.argent.zksync.domain.token.Token;
import org.web3j.utils.Numeric;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;

public class SigningUtils {

    private static final int MAX_NUMBER_OF_TOKENS = 128;

    private static final int MAX_NUMBER_OF_ACCOUNTS = Double.valueOf(Math.pow(2, 24)).intValue();

    private static final int AMOUNT_EXPONENT_BIT_WIDTH = 5;
    private static final int AMOUNT_MANTISSA_BIT_WIDTH = 35;
    private static final int FEE_EXPONENT_BIT_WIDTH = 5;
    private static final int FEE_MANTISSA_BIT_WIDTH = 11;

    public static String getChangePubKeyMessage(String pubKeyHash, Integer nonce, Integer accountId) {
        final String pubKeyHashStripped = pubKeyHash.replace("sync:", "").toLowerCase();

        return String.format("Register zkSync pubkey:\n\n" +
                        "%s\n" +
                        "nonce: %s\n" +
                        "account id: %s\n\n" +
                        "Only sign this message for a trusted client!",
                pubKeyHashStripped,
                Numeric.toHexString(nonceToBytes(nonce)),
                Numeric.toHexString(accountIdToBytes(accountId)));
    }

    public static String getTransferMessage(String to,
                                            Integer accountId,
                                            Integer nonce,
                                            BigInteger amount,
                                            Token token,
                                            BigInteger fee) {
        return String.format(
                "Transfer %s %s\n" +
                        "To: %s\n" +
                        "Nonce: %s\n" +
                        "Fee: %s %s\n" +
                        "Account Id: %s",
                token.formatToken(amount),
                token.getSymbol(),
                to.toLowerCase(),
                nonce,
                token.formatToken(fee),
                token.getSymbol(),
                accountId);
    }

    public static String getWithdrawMessage(String to,
                                            Integer accountId,
                                            Integer nonce,
                                            BigInteger amount,
                                            Token token,
                                            BigInteger fee) {

        return String.format(
                "Withdraw %s %s\n" +
                        "To: %s\n" +
                        "Nonce: %s\n" +
                        "Fee: %s %s\n" +
                        "Account Id: %s",
                token.formatToken(amount),
                token.getSymbol(),
                to.toLowerCase(),
                nonce,
                token.formatToken(fee),
                token.getSymbol(),
                accountId);
    }

    public static byte[] accountIdToBytes(Integer accountId) {

        if (accountId > MAX_NUMBER_OF_ACCOUNTS) {
            throw new RuntimeException("Account number too large");
        }

        return intToByteArrayBE(accountId, 4);
    }

    public static byte[] addressToBytes(String address) {
        final String prefixlessAddress = removeAddressPrefix(address);

        final byte[] addressBytes = Numeric.hexStringToByteArray(prefixlessAddress);

        if (addressBytes.length != 20) {
            throw new RuntimeException("Address must be 20 bytes long");
        }

        return addressBytes;
    }

    public static byte[] tokenIdToBytes(Integer tokenId) {
        if (tokenId < 0) {
            throw new RuntimeException("Negative tokenId");
        }

        if (tokenId >= MAX_NUMBER_OF_TOKENS) {
            throw new Error("TokenId is too big");
        }

        return intToByteArrayBE(tokenId, 2);
    }

    public static byte[] feeToBytes(BigInteger fee) {
        return packFeeChecked(fee);
    }

    public static byte[] amountPackedToBytes(BigInteger amount) {
        return packAmountChecked(amount);
    }

    public static byte[] amountFullToBytes(BigInteger amount) {
        try {
            final byte[] amountBytes = amount.toByteArray();

            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputStream.write(new byte[16 - amountBytes.length]);
            outputStream.write(amountBytes);

            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public static byte[] nonceToBytes(Integer nonce) {
        if (nonce < 0) {
            throw new Error("Negative nonce");
        }

        return numberToBytesBE(nonce, 4);
    }

    private static byte[] packFeeChecked(BigInteger fee) {
        if (!closestPackableTransactionFee(fee).toString().equals(fee.toString())) {
            throw new Error("Fee Amount is not packable");
        }

        return packFee(fee);
    }

    private static byte[] packAmountChecked(BigInteger amount) {
        if (!closestPackableTransactionAmount(amount).toString().equals(amount.toString())) {
            throw new Error("Amount is not packable");
        }

        return packAmount(amount);
    }

    private static BigInteger closestPackableTransactionFee(BigInteger fee) {
        final byte[] packedFee = packFee(fee);
        printBytes("packed fee: ", packedFee);
        return decimalByteArrayToInteger(packedFee, FEE_EXPONENT_BIT_WIDTH, FEE_MANTISSA_BIT_WIDTH, 10);
    }

    private static BigInteger closestPackableTransactionAmount(BigInteger amount) {
        final byte[] packedAmount = packAmount(amount);
        printBytes("packed amount: ", packedAmount);
        return decimalByteArrayToInteger(packedAmount, AMOUNT_EXPONENT_BIT_WIDTH, AMOUNT_MANTISSA_BIT_WIDTH, 10);
    }

    private static byte[] packFee(BigInteger fee) {
        return reverseBits(integerToDecimalByteArray(fee, FEE_EXPONENT_BIT_WIDTH, FEE_MANTISSA_BIT_WIDTH, 10));
    }

    private static byte[] packAmount(BigInteger amount) {
        return reverseBits(integerToDecimalByteArray(amount, AMOUNT_EXPONENT_BIT_WIDTH, AMOUNT_MANTISSA_BIT_WIDTH, 10));
    }

    private static byte[] integerToDecimalByteArray(BigInteger value,
                                                    int expBits,
                                                    int mantissaBits,
                                                    int expBase) {
        final BigInteger maxExponent = BigInteger.valueOf(10).pow(
                BigInteger.valueOf(2).pow(expBits).subtract(BigInteger.ONE).intValue());

        final BigInteger maxMantissa = BigInteger.valueOf(2).pow(mantissaBits).subtract(BigInteger.ONE);

        if (value.compareTo(maxMantissa.multiply(maxExponent)) > 0) {
            throw new RuntimeException("Integer is too big");
        }

        int exponent = 0;
        BigInteger mantissa = value;

        while (mantissa.compareTo(maxMantissa) > 0) {
            mantissa =  mantissa.divide(BigInteger.valueOf(expBase));
            exponent++;
        }
        System.out.println("Mantissa: " + mantissa);
        System.out.println("Mantissa intValue: " + mantissa.longValue());
        System.out.println("Exponent: " + exponent);

        final Bits exponentBitSet = numberToBitsLE(Long.valueOf(exponent), expBits);
        final Bits mantissaBitSet = numberToBitsLE(mantissa.longValue(), mantissaBits);

        System.out.println("exponent bits: " + exponentBitSet);
        System.out.println("mantissa bits: " + mantissaBitSet);

        final Bits reversed = combineBitSets(exponentBitSet, mantissaBitSet).reverse();

        System.out.println("reversed: " + reversed);
        printBytes("bits into bytes BE reversed", reverseBytes(bitsIntoBytesInBEOrder(reversed)));

        return reverseBits(bitsIntoBytesInBEOrder(reversed));
    }

    private static BigInteger decimalByteArrayToInteger(byte[] decimalBytes,
                                                    int expBits,
                                                    int mantissaBits,
                                                    int expBase) {
        if (decimalBytes.length * 8 != mantissaBits + expBits) {
            throw new RuntimeException("Decimal unpacking, incorrect input length");
        }

        final Bits bits = bytesToBitsBE(decimalBytes).reverse();

        BigInteger exponent = BigInteger.ZERO;
        BigInteger expPow2 = BigInteger.ONE;

        for (int i = 0; i < expBits; i++) {
            if (bits.get(i)) {
                exponent = exponent.add(expPow2);
            }
            expPow2 = expPow2.multiply(BigInteger.valueOf(2));
        }
        exponent = BigInteger.valueOf(expBase).pow(exponent.intValue());

        BigInteger mantissa = BigInteger.ZERO;
        BigInteger mantissaPow2 = BigInteger.ONE;

        for (int i = expBits; i < expBits + mantissaBits; i++) {
            if (bits.get(i)) {
                mantissa = mantissa.add(mantissaPow2);
            }
            mantissaPow2 = mantissaPow2.multiply(BigInteger.valueOf(2));
        }

        return exponent.multiply(mantissa);
    }

    private static Bits bytesToBitsBE(byte[] bytes) {
        final Bits bitSet = new Bits(bytes.length * 8);

        for (int i = 0; i < bytes.length; i++) {
            final byte aByte = bytes[i];

            if ((aByte & 0x80) != 0) bitSet.set(i * 8);
            if ((aByte & 0x40) != 0) bitSet.set(i * 8 + 1);
            if ((aByte & 0x20) != 0) bitSet.set(i * 8 + 2);
            if ((aByte & 0x10) != 0) bitSet.set(i * 8 + 3);
            if ((aByte & 0x08) != 0) bitSet.set(i * 8 + 4);
            if ((aByte & 0x04) != 0) bitSet.set(i * 8 + 5);
            if ((aByte & 0x02) != 0) bitSet.set(i * 8 + 6);
            if ((aByte & 0x01) != 0) bitSet.set(i * 8 + 7);
        }

        return bitSet;
    }

    private static byte[] bitsIntoBytesInBEOrder(Bits bits) {
        if (bits.size() % 8 != 0) {
            throw new RuntimeException("Wrong number of bits to pack");
        }

        int numBytes = bits.size() / 8;
        byte[] resultBytes = new byte[numBytes];

        for (int currentByte = 0; currentByte < numBytes; currentByte++) {
            int value = 0;

            if (bits.get(currentByte * 8)) {
                value |= 0x80;
            }

            if (bits.get(currentByte * 8 + 1)) {
                value |= 0x40;
            }

            if (bits.get(currentByte * 8 + 2)) {
                value |= 0x20;
            }

            if (bits.get(currentByte * 8 + 3)) {
                value |= 0x10;
            }

            if (bits.get(currentByte * 8 + 4)) {
                value |= 0x08;
            }

            if (bits.get(currentByte * 8 + 5)) {
                value |= 0x04;
            }

            if (bits.get(currentByte * 8 + 6)) {
                value |= 0x02;
            }

            if (bits.get(currentByte * 8 + 7)) {
                value |= 0x01;
            }

            resultBytes[currentByte] = Integer.valueOf(value).byteValue();
        }

        printBytes("bitsIntoBytesInBEOrder: ", resultBytes);

        return resultBytes;
    }

    private static Bits combineBitSets(Bits bitSetA, Bits bitSetB) {
        final int size = bitSetA.size() + bitSetB.size();
        final Bits bitSet = new Bits(size);

        int index = 0;

        while (index < bitSetA.size()) {
            if (bitSetA.get(index)) {
                bitSet.set(index);
            }
            index++;
        }

        int bCount = 0;
        while (bCount < bitSetB.size()) {
            if (bitSetB.get(bCount)) {
                bitSet.set(index);
            }
            index++;
            bCount++;
        }

        return bitSet;
    }

    private static Bits numberToBitsLE(long number, int numBits) {
        System.out.println(number);
        final Bits bitSet = new Bits(numBits);
        bitSet.size();

        for (int i = 0; i < numBits; i++) {
            final long bit = number & 1;

            System.out.println(number + " % " + 1 + " = " + bit);
            if (bit == 1) {
                bitSet.set(i);
            }
            number /= 2;
        }

        return bitSet;
    }

    private static byte[] numberToBytesBE(int number, int numBytes) {
        final byte[] result = new byte[numBytes];

        for (int i = numBytes - 1; i >= 0; i--) {
            result[i] = Integer.valueOf(number & 0xff).byteValue();
            number >>= 8;
        }

        return result;
    }

    private static byte[] reverseBits(byte[] toReverse) {
        final byte[] reversedBytes = reverseBytes(toReverse);
        final byte[] reversedBits = new byte[toReverse.length];


        for (int i = 0; i < reversedBytes.length; i++) {
            byte aByte = reversedBytes[i];

            aByte = Integer.valueOf(((aByte & 0xf0) >> 4) | ((aByte & 0x0f) << 4)).byteValue();
            aByte = Integer.valueOf(((aByte & 0xcc) >> 2) | ((aByte & 0x33) << 2)).byteValue();
            aByte = Integer.valueOf(((aByte & 0xaa) >> 1) | ((aByte & 0x55) << 1)).byteValue();

            reversedBits[i] = aByte;
        }

        return reversedBits;
    }

    private static byte[] reverseBytes(byte[] toReverse) {
        final byte[] reversed = new byte[toReverse.length];

        for (int i = 0; i < toReverse.length; i++) {
            reversed[i] = toReverse[toReverse.length - (i + 1)];
        }

        return reversed;
    }

    private static String removeAddressPrefix(String address) {
        if (address.startsWith("0x")) {
            return address.substring(2);
        }

        if (address.startsWith("sync")) {
            return address.substring(5);
        }

        throw new RuntimeException("ETH address must start with '0x' and PubKeyHash must start with 'sync:'");
    }

    private static byte[] intToByteArrayBE(int value, int size) {
        final byte[] result = new byte[size];

        for (int i = size - 1; i >= 0; i--) {
            result[i] = Integer.valueOf(value & 0xff).byteValue();
            value >>=8;
        }

        return result;
    }

    private static void printBytes(String prefix, byte[] toPrint) {
        final StringBuilder builder = new StringBuilder();

        for (byte aByte : toPrint) {
            builder.append(Byte.toUnsignedInt(aByte) + ", ");
        }

        System.out.println(prefix + builder.toString());
    }
}
