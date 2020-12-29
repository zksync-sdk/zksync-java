package io.zksync.signer;

import io.zksync.domain.token.Token;
import io.zksync.exception.ZkSyncException;
import org.web3j.crypto.Bip32ECKeyPair;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.MnemonicUtils;
import org.web3j.crypto.Sign;
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

    private DefaultEthSigner(Credentials credentials) {

        System.out.println(Numeric.toHexString(credentials.getEcKeyPair().getPrivateKey().toByteArray()));
        this.credentials = credentials;
    }

    public static DefaultEthSigner fromMnemonic(String mnemonic) {
        return new DefaultEthSigner(generateCredentialsFromMnemonic(mnemonic, 0));
    }

    public static DefaultEthSigner fromMnemonic(String mnemonic, int accountIndex) {
        return new DefaultEthSigner(generateCredentialsFromMnemonic(mnemonic, accountIndex));
    }

    public static DefaultEthSigner fromRawPrivateKey(String rawPrivateKey) {
        return new DefaultEthSigner(Credentials.create(rawPrivateKey));
    }

    public String getAddress() {
        return credentials.getAddress();
    }

    public Credentials getCredentials() {
        return this.credentials;
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
        int[] derivationPath = {44 | Bip32ECKeyPair.HARDENED_BIT, 60 | Bip32ECKeyPair.HARDENED_BIT, 0 | Bip32ECKeyPair.HARDENED_BIT, 0,accountIndex};

        // Generate a BIP32 master keypair from the mnemonic phrase
        Bip32ECKeyPair masterKeypair = Bip32ECKeyPair.generateKeyPair(
                MnemonicUtils.generateSeed(mnemonic, ""));

        // Derive the keypair using the derivation path
        Bip32ECKeyPair  derivedKeyPair = Bip32ECKeyPair.deriveKeyPair(masterKeypair, derivationPath);

        // Load the wallet for the derived keypair
        return Credentials.create(derivedKeyPair);
    }
}
