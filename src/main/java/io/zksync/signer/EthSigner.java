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

import static io.zksync.signer.SigningUtils.*;

public class EthSigner {

    private final Credentials credentials;

    private EthSigner(Credentials credentials) {

        System.out.println(Numeric.toHexString(credentials.getEcKeyPair().getPrivateKey().toByteArray()));
        this.credentials = credentials;
    }


    public static EthSigner fromMnemonic(String mnemonic) {
        return new EthSigner(generateCredentialsFromMnemonic(mnemonic, 0));
    }

    public static EthSigner fromMnemonic(String mnemonic, int accountIndex) {
        return new EthSigner(generateCredentialsFromMnemonic(mnemonic, accountIndex));
    }

    public String getAddress() {
        return credentials.getAddress();
    }

    public Credentials getCredentials() {
        return this.credentials;
    }

    public EthSignature signChangePubKey(String pubKeyHash, Integer nonce, Integer accountId) {
        return signMessage(getChangePubKeyMessage(pubKeyHash, nonce, accountId));
    }

    public EthSignature signTransfer(String to,
                                     Integer accountId,
                                     Integer nonce,
                                     BigInteger amount,
                                     Token token,
                                     BigInteger fee) {
        return signMessage(getTransferMessage(to, accountId, nonce, amount, token, fee));
    }

    public EthSignature signWithdraw(String to,
                                     Integer accountId,
                                     Integer nonce,
                                     BigInteger amount,
                                     Token token,
                                     BigInteger fee) {
        return signMessage(getWithdrawMessage(to, accountId, nonce, amount, token, fee));
    }

    public EthSignature signMessage(String message) {
        System.out.println("Eth message: " + message);

        Sign.SignatureData sig = Sign.signPrefixedMessage(message.getBytes(), credentials.getEcKeyPair());

        final ByteArrayOutputStream output = new ByteArrayOutputStream();

        try {
            output.write(sig.getR());
            output.write(sig.getS());
            output.write(sig.getV());
        } catch (IOException e) {
            throw new ZkSyncException("Error when creating ETH signature", e);
        }

        final String signature = Numeric.toHexString(output.toByteArray());

        return EthSignature
                .builder()
                .signature(signature)
                .type("EthereumSignature")
                .build();
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
