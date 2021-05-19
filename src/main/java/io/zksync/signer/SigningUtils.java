package io.zksync.signer;

import io.zksync.domain.auth.ChangePubKeyVariant;
import io.zksync.domain.token.Token;
import io.zksync.domain.token.TokenId;
import io.zksync.exception.ZkSyncException;
import lombok.SneakyThrows;

import org.web3j.utils.Numeric;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.NumberFormat;
import java.util.Locale;

public class SigningUtils {

    private static final long MAX_NUMBER_OF_TOKENS = Long.MAX_VALUE;

    private static final int MAX_NUMBER_OF_ACCOUNTS = Double.valueOf(Math.pow(2, 24)).intValue();

    private static final int AMOUNT_EXPONENT_BIT_WIDTH = 5;
    private static final int AMOUNT_MANTISSA_BIT_WIDTH = 35;
    private static final int FEE_EXPONENT_BIT_WIDTH = 5;
    private static final int FEE_MANTISSA_BIT_WIDTH = 11;

    public static String getChangePubKeyMessagePart(String pubKeyHash, Token token, BigInteger fee) {
        final String pubKeyHashStripped = pubKeyHash.replace("sync:", "").toLowerCase();

        String result = String.format("Set signing key: %s", pubKeyHashStripped);
        if (fee.compareTo(BigInteger.ZERO) > 0) {
            result += String.format("\nFee: %s %s", format(token.intoDecimal(fee)), token.getSymbol());
        }
        return result;
    }

    @SneakyThrows
    public static byte[] getChangePubKeyData(String pubKeyHash, Integer nonce, Integer accountId, ChangePubKeyVariant changePubKeyVariant) {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(addressToBytes(pubKeyHash));
        outputStream.write(nonceToBytes(nonce));
        outputStream.write(accountIdToBytes(accountId));
        outputStream.write(changePubKeyVariant.getBytes());

        return outputStream.toByteArray();
    }

    public static String getTransferMessagePart(String to,
                                            Integer accountId,
                                            BigInteger amount,
                                            TokenId token,
                                            BigInteger fee) {
        String result = !amount.equals(BigInteger.ZERO) ? String.format("Transfer %s %s to: %s", format(token.intoDecimal(amount)), token.getSymbol(), to.toLowerCase()) : "";
        if (fee.compareTo(BigInteger.ZERO) > 0) {
            result += String.format("%sFee: %s %s", result.isEmpty() ? "" : "\n", format(token.intoDecimal(fee)), token.getSymbol());
        }
        return result;
    }

    public static String getWithdrawMessagePart(String to,
                                            Integer accountId,
                                            BigInteger amount,
                                            Token token,
                                            BigInteger fee) {

        String result = String.format("Withdraw %s %s to: %s", format(token.intoDecimal(amount)), token.getSymbol(), to.toLowerCase());
        if (fee.compareTo(BigInteger.ZERO) > 0) {
            result += String.format("\nFee: %s %s", format(token.intoDecimal(fee)), token.getSymbol());
        }
        return result;
    }

    public static String getForcedExitMessagePart(String to, Token token, BigInteger fee) {
        String result = String.format("ForcedExit %s to: %s", token.getSymbol(), to.toLowerCase());
        if (fee.compareTo(BigInteger.ZERO) > 0) {
            result += String.format("\nFee: %s %s", format(token.intoDecimal(fee)), token.getSymbol());
        }
        return result;
    }

    public static String getMintNFTMessagePart(String contentHash, String recipient, Token token, BigInteger fee) {
        String result = String.format("MintNFT %s for: %s", contentHash, recipient.toLowerCase());
        if (fee.compareTo(BigInteger.ZERO) > 0) {
            result += String.format("\nFee: %s %s", format(token.intoDecimal(fee)), token.getSymbol());
        }
        return result;
    }

    public static String getWithdrawNFTMessagePart(String to,
                                            Integer tokenId,
                                            Token token,
                                            BigInteger fee) {

        String result = String.format("WithdrawNFT %d to: %s", tokenId, to.toLowerCase());
        if (fee.compareTo(BigInteger.ZERO) > 0) {
            result += String.format("\nFee: %s %s", format(token.intoDecimal(fee)), token.getSymbol());
        }
        return result;
    }

    public static String getSwapMessagePart(Token token, BigInteger fee) {
        String result = String.format("Swap fee: %s %s", format(token.intoDecimal(fee)), token.getSymbol());
        return result;
    }

    public static String getNonceMessagePart(Integer nonce) {
        return String.format("Nonce: %s", nonce);
    }

    public static String format(BigDecimal amount) {
        NumberFormat format = NumberFormat.getNumberInstance(Locale.ROOT);
        format.setMinimumFractionDigits(1);
        format.setMaximumFractionDigits(18);
        format.setGroupingUsed(false);
        return format.format(amount);
    }

    public static byte[] numberToBytesBE(long number, int numBytes) {
        final byte[] result = new byte[numBytes];

        for (int i = numBytes - 1; i >= 0; i--) {
            result[i] = Long.valueOf(number & 0xff).byteValue();
            number >>= 8;
        }

        return result;
    }

    public static byte[] bigIntToBytesBE(BigInteger number, int numBytes) {
        final byte[] result = new byte[numBytes];

        for (int i = numBytes - 1; i >= 0; i--) {
            result[i] = number.and(BigInteger.valueOf(0xff)).byteValue();
            number = number.shiftRight(8);
        }

        return result;
    }

    public static byte[] accountIdToBytes(Integer accountId) {

        if (accountId > MAX_NUMBER_OF_ACCOUNTS) {
            throw new ZkSyncException("Account number too large");
        }

        return numberToBytesBE(accountId, 4);
    }

    public static byte[] addressToBytes(String address) {
        final String prefixlessAddress = removeAddressPrefix(address);

        final byte[] addressBytes = Numeric.hexStringToByteArray(prefixlessAddress);

        if (addressBytes.length != 20) {
            throw new ZkSyncException("Address must be 20 bytes long");
        }

        return addressBytes;
    }

    public static byte[] tokenIdToBytes(Integer tokenId) {
        if (tokenId < 0) {
            throw new ZkSyncException("Negative tokenId");
        }

        if (tokenId >= MAX_NUMBER_OF_TOKENS) {
            throw new Error("TokenId is too big");
        }

        return numberToBytesBE(tokenId, 4);
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
            throw new ZkSyncException(e);
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
        return decimalByteArrayToInteger(packedFee, FEE_EXPONENT_BIT_WIDTH, FEE_MANTISSA_BIT_WIDTH, 10);
    }

    private static BigInteger closestPackableTransactionAmount(BigInteger amount) {
        final byte[] packedAmount = packAmount(amount);
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
            throw new ZkSyncException("Integer is too big");
        }

        int exponent = 0;
        BigInteger mantissa = value;

        while (mantissa.compareTo(maxMantissa) > 0) {
            mantissa =  mantissa.divide(BigInteger.valueOf(expBase));
            exponent++;
        }

        final Bits exponentBitSet = numberToBitsLE(Long.valueOf(exponent), expBits);
        final Bits mantissaBitSet = numberToBitsLE(mantissa.longValue(), mantissaBits);

        final Bits reversed = combineBitSets(exponentBitSet, mantissaBitSet).reverse();

        return reverseBits(bitsIntoBytesInBEOrder(reversed));
    }

    private static BigInteger decimalByteArrayToInteger(byte[] decimalBytes,
                                                    int expBits,
                                                    int mantissaBits,
                                                    int expBase) {
        if (decimalBytes.length * 8 != mantissaBits + expBits) {
            throw new ZkSyncException("Decimal unpacking, incorrect input length");
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
            throw new ZkSyncException("Wrong number of bits to pack");
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
        final Bits bitSet = new Bits(numBits);
        bitSet.size();

        for (int i = 0; i < numBits; i++) {
            final long bit = number & 1;

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

        throw new ZkSyncException("ETH address must start with '0x' and PubKeyHash must start with 'sync:'");
    }
}
