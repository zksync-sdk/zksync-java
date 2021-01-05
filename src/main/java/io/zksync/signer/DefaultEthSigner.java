package io.zksync.signer;

import io.zksync.domain.token.Token;
import io.zksync.ethereum.transaction.NoOpTransactionManager;
import io.zksync.exception.ZkSyncException;
import org.web3j.crypto.Bip32ECKeyPair;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.MnemonicUtils;
import org.web3j.crypto.Sign;
import org.web3j.protocol.Web3j;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.utils.Numeric;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

import static io.zksync.signer.SigningUtils.*;

public class DefaultEthSigner implements EthSigner {

    private final Credentials credentials;
    private final TransactionManager transactionManager;

    private DefaultEthSigner(TransactionManager transactionManager, Credentials credentials) {
        this.credentials = credentials;
        this.transactionManager = transactionManager;
    }

    public static EthSigner fromMnemonic(String mnemonic) {
        Credentials credentials = generateCredentialsFromMnemonic(mnemonic, 0);
        return new DefaultEthSigner(new NoOpTransactionManager(credentials), credentials);
    }

    public static EthSigner fromMnemonic(String mnemonic, int accountIndex) {
        Credentials credentials = generateCredentialsFromMnemonic(mnemonic, accountIndex);
        return new DefaultEthSigner(new NoOpTransactionManager(credentials), credentials);
    }

    public static EthSigner fromRawPrivateKey(String rawPrivateKey) {
        Credentials credentials = Credentials.create(rawPrivateKey);
        return new DefaultEthSigner(new NoOpTransactionManager(credentials), credentials);
    }

    public static EthSigner fromMnemonic(Web3j web3j, String mnemonic) {
        Credentials credentials = generateCredentialsFromMnemonic(mnemonic, 0);
        return new DefaultEthSigner(new RawTransactionManager(web3j, credentials), credentials);
    }

    public static EthSigner fromMnemonic(Web3j web3j, String mnemonic, int accountIndex) {
        Credentials credentials = generateCredentialsFromMnemonic(mnemonic, accountIndex);
        return new DefaultEthSigner(new RawTransactionManager(web3j, credentials), credentials);
    }

    public static EthSigner fromRawPrivateKey(Web3j web3j, String rawPrivateKey) {
        Credentials credentials = Credentials.create(rawPrivateKey);
        return new DefaultEthSigner(new RawTransactionManager(web3j, credentials), credentials);
    }

    public String getAddress() {
        return transactionManager.getFromAddress();
    }

    public TransactionManager getTransactionManager() {
        return this.transactionManager;
    }

    public CompletableFuture<EthSignature> signChangePubKey(String pubKeyHash, Integer nonce, Integer accountId) {
        return signMessage(getChangePubKeyMessage(pubKeyHash, nonce, accountId));
    }

    public CompletableFuture<EthSignature> signTransfer(String to, Integer accountId, Integer nonce, BigInteger amount, Token token,
            BigInteger fee) {
        return signMessage(getTransferMessage(to, accountId, nonce, amount, token, fee));
    }

    public CompletableFuture<EthSignature> signWithdraw(String to, Integer accountId, Integer nonce, BigInteger amount, Token token,
            BigInteger fee) {
        return signMessage(getWithdrawMessage(to, accountId, nonce, amount, token, fee));
    }

    public CompletableFuture<EthSignature> signMessage(String message) {
        return signMessage(message, true);
    }

    public CompletableFuture<EthSignature> signMessage(String message, boolean addPrefix) {
        Sign.SignatureData sig = addPrefix ?
            Sign.signPrefixedMessage(message.getBytes(), credentials.getEcKeyPair()) :
            Sign.signMessage(message.getBytes(), credentials.getEcKeyPair());

        final ByteArrayOutputStream output = new ByteArrayOutputStream();

        try {
            output.write(sig.getR());
            output.write(sig.getS());
            output.write(sig.getV());
        } catch (IOException e) {
            throw new ZkSyncException("Error when creating ETH signature", e);
        }

        final String signature = Numeric.toHexString(output.toByteArray());

        return CompletableFuture.completedFuture(EthSignature.builder().signature(signature).type(EthSignature.SignatureType.EthereumSignature).build());
    }

    public CompletableFuture<Boolean> verifySignature(EthSignature signature, String message) throws SignatureException {
        return verifySignature(signature, message, true);
    }

    public CompletableFuture<Boolean> verifySignature(EthSignature signature, String message, boolean prefixed) throws SignatureException {
        byte[] sig = Numeric.hexStringToByteArray(signature.getSignature());
        Sign.SignatureData signatureData = new Sign.SignatureData(
            Arrays.copyOfRange(sig, 64, 65),
            Arrays.copyOfRange(sig, 0, 32),
            Arrays.copyOfRange(sig, 32, 64)
        );
        BigInteger publicKey = prefixed ?
            Sign.signedPrefixedMessageToKey(message.getBytes(), signatureData) :
            Sign.signedMessageToKey(message.getBytes(), signatureData);
        return CompletableFuture.completedFuture(credentials.getEcKeyPair().getPublicKey().equals(publicKey));
    }

    private static Credentials generateCredentialsFromMnemonic(String mnemonic, int accountIndex) {
        //m/44'/60'/0'/0 derivation path
        int[] derivationPath = {44 | Bip32ECKeyPair.HARDENED_BIT, 60 | Bip32ECKeyPair.HARDENED_BIT, 0 | Bip32ECKeyPair.HARDENED_BIT, 0, accountIndex};

        // Generate a BIP32 master keypair from the mnemonic phrase
        Bip32ECKeyPair masterKeypair = Bip32ECKeyPair.generateKeyPair(
                MnemonicUtils.generateSeed(mnemonic, ""));

        // Derive the keypair using the derivation path
        Bip32ECKeyPair  derivedKeyPair = Bip32ECKeyPair.deriveKeyPair(masterKeypair, derivationPath);

        // Load the wallet for the derived keypair
        return Credentials.create(derivedKeyPair);
    }
}
