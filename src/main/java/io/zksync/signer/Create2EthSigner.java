package io.zksync.signer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SignatureException;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import org.bouncycastle.util.Arrays;
import org.web3j.crypto.Hash;
import org.web3j.protocol.Web3j;
import org.web3j.tx.ReadonlyTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.utils.Numeric;

import io.zksync.domain.auth.ChangePubKeyCREATE2;
import io.zksync.domain.swap.Order;
import io.zksync.domain.token.Token;
import io.zksync.domain.token.TokenId;
import io.zksync.domain.transaction.ChangePubKey;
import io.zksync.domain.transaction.ZkSyncTransaction;
import io.zksync.ethereum.wrappers.IEIP1271;
import io.zksync.exception.ZkSyncException;

public class Create2EthSigner implements EthSigner<ChangePubKeyCREATE2> {
    private final TransactionManager transactionManager;
    private final IEIP1271 validator;

    private ChangePubKeyCREATE2 authData;
    private String address;

    private Create2EthSigner(String zkSyncAddress, String address, ChangePubKeyCREATE2 create2Data, TransactionManager transactionManager, IEIP1271 validator) {
        this.transactionManager = transactionManager;
        this.authData = create2Data;
        this.address = address;
        this.validator = validator;
    }

    public static Create2EthSigner fromRawPrivateKey(Web3j web3j, String zkSyncAddress, ChangePubKeyCREATE2 create2Data) {
        final byte[] pubKeyHashStripped = Numeric.hexStringToByteArray(zkSyncAddress.replace("sync:", "").toLowerCase());
        final byte[] arg = Numeric.hexStringToByteArray(create2Data.getSaltArg());

        final byte[] salt = generateSalt(arg, pubKeyHashStripped);
        final String address = generateAddress(create2Data.getCreatorAddress(), salt, Numeric.hexStringToByteArray(create2Data.getCodeHash()));
        final TransactionManager transactionManager = new ReadonlyTransactionManager(web3j, create2Data.getCreatorAddress());
        return new Create2EthSigner(zkSyncAddress, address, create2Data, transactionManager, IEIP1271.load(address, web3j, transactionManager, null));
    }

    @Override
    public String getAddress() {
        return address;
    }

    @Override
    public TransactionManager getTransactionManager() {
        return this.transactionManager;
    }

    @Override
    public CompletableFuture<ChangePubKey<ChangePubKeyCREATE2>> signAuth(
            ChangePubKey<ChangePubKeyCREATE2> changePubKey) {
        changePubKey.setEthAuthData(authData);
        return CompletableFuture.completedFuture(changePubKey);
    }

    @Override
    public <T extends ZkSyncTransaction> CompletableFuture<EthSignature> signTransaction(T transaction, Integer nonce,
            Token token, BigInteger fee) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public <T extends TokenId> CompletableFuture<EthSignature> signOrder(Order order, T tokenSell, T tokenBuy) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public <T extends ZkSyncTransaction> CompletableFuture<EthSignature> signBatch(Collection<T> transactions,
            Integer nonce, Token token, BigInteger fee) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<EthSignature> signMessage(byte[] message) {
        return CompletableFuture.completedFuture(new EthSignature(EthSignature.SignatureType.EIP1271Signature, Numeric.toHexStringWithPrefix(BigInteger.ZERO)));
    }

    @Override
    public CompletableFuture<EthSignature> signMessage(byte[] message, boolean addPrefix) {
        return CompletableFuture.completedFuture(new EthSignature(EthSignature.SignatureType.EIP1271Signature, Numeric.toHexStringWithPrefix(BigInteger.ZERO)));
    }

    @Override
    public CompletableFuture<Boolean> verifySignature(EthSignature signature, byte[] message)
            throws SignatureException {
        return this.verifySignature(signature, message, true);
    }

    @Override
    public CompletableFuture<Boolean> verifySignature(EthSignature signature, byte[] message, boolean prefixed)
            throws SignatureException {
        byte[] messageHash = prefixed ? EthSigner.getEthereumMessageHash(message) : Hash.sha3(message);
        return this.validator.isValidSignature(messageHash, Numeric.hexStringToByteArray(signature.getSignature()))
            .sendAsync()
            .thenApply((result) -> Arrays.areEqual(result, EIP1271_SUCCESS_VALUE));
    }

    private static byte[] generateSalt(byte[] saltArg, byte[] pubKeyHash) {
        try {
            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            outputStream.write(saltArg);
            outputStream.write(pubKeyHash);

            byte[] data = outputStream.toByteArray();

            byte[] hash = Hash.sha3(data);

            return hash;
        } catch (IOException e) {
            throw new ZkSyncException(e);
        }
    }

    private static String generateAddress(String creatorAddress, byte[] salt, byte[] codeHash) {
        try {
            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            outputStream.write(0xff);
            outputStream.write(Numeric.hexStringToByteArray(creatorAddress));
            outputStream.write(salt);
            outputStream.write(codeHash);

            byte[] data = outputStream.toByteArray();

            byte[] hash = Hash.sha3(data);
            byte[] address = Arrays.copyOfRange(hash, 12, hash.length);

            return Numeric.toHexString(address);
        } catch (IOException e) {
            throw new ZkSyncException(e);
        }
    }
    
}
