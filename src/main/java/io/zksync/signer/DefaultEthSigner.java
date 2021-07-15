package io.zksync.signer;

import io.zksync.domain.auth.ChangePubKeyECDSA;
import io.zksync.domain.swap.Order;
import io.zksync.domain.token.Token;
import io.zksync.domain.token.TokenId;
import io.zksync.domain.transaction.*;
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
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static io.zksync.signer.SigningUtils.*;

public class DefaultEthSigner implements EthSigner<ChangePubKeyECDSA> {

    private final Credentials credentials;
    private final TransactionManager transactionManager;

    private DefaultEthSigner(TransactionManager transactionManager, Credentials credentials) {
        this.credentials = credentials;
        this.transactionManager = transactionManager;
    }

    public static DefaultEthSigner fromMnemonic(String mnemonic) {
        Credentials credentials = generateCredentialsFromMnemonic(mnemonic, 0);
        return new DefaultEthSigner(new NoOpTransactionManager(credentials), credentials);
    }

    public static DefaultEthSigner fromMnemonic(String mnemonic, int accountIndex) {
        Credentials credentials = generateCredentialsFromMnemonic(mnemonic, accountIndex);
        return new DefaultEthSigner(new NoOpTransactionManager(credentials), credentials);
    }

    public static DefaultEthSigner fromRawPrivateKey(String rawPrivateKey) {
        Credentials credentials = Credentials.create(rawPrivateKey);
        return new DefaultEthSigner(new NoOpTransactionManager(credentials), credentials);
    }

    public static DefaultEthSigner fromMnemonic(Web3j web3j, String mnemonic) {
        Credentials credentials = generateCredentialsFromMnemonic(mnemonic, 0);
        return new DefaultEthSigner(new RawTransactionManager(web3j, credentials), credentials);
    }

    public static DefaultEthSigner fromMnemonic(Web3j web3j, String mnemonic, int accountIndex) {
        Credentials credentials = generateCredentialsFromMnemonic(mnemonic, accountIndex);
        return new DefaultEthSigner(new RawTransactionManager(web3j, credentials), credentials);
    }

    public static DefaultEthSigner fromRawPrivateKey(Web3j web3j, String rawPrivateKey) {
        Credentials credentials = Credentials.create(rawPrivateKey);
        return new DefaultEthSigner(new RawTransactionManager(web3j, credentials), credentials);
    }

    public String getAddress() {
        return transactionManager.getFromAddress();
    }

    public TransactionManager getTransactionManager() {
        return this.transactionManager;
    }

    @Override
    public CompletableFuture<ChangePubKey<ChangePubKeyECDSA>> signAuth(ChangePubKey<ChangePubKeyECDSA> changePubKey) {
        ChangePubKeyECDSA auth = new ChangePubKeyECDSA(null, Numeric.toHexString(Numeric.toBytesPadded(BigInteger.ZERO, 32)));
        return signMessage(getChangePubKeyData(changePubKey.getNewPkHash(), changePubKey.getNonce(), changePubKey.getAccountId(), auth))
            .thenApply(sig -> {
                auth.setEthSignature(sig.getSignature());
                changePubKey.setEthAuthData(auth);
                return changePubKey;
            });
    }

    public <T extends ZkSyncTransaction> CompletableFuture<EthSignature> signTransaction(T tx, Integer nonce, Token token, BigInteger fee) {
        switch (tx.getType()) {
            case "ChangePubKey":
                ChangePubKey<?> changePubKey = (ChangePubKey<?>) tx;
                return signMessage(getChangePubKeyData(changePubKey.getNewPkHash(), nonce, changePubKey.getAccountId(), changePubKey.getEthAuthData()));
            case "ForcedExit":
                ForcedExit forcedExit = (ForcedExit) tx;
                return signMessage(String.join("\n", getForcedExitMessagePart(forcedExit.getTarget(), token, fee), getNonceMessagePart(nonce)).getBytes());
            case "MintNFT":
                MintNFT mintNFT = (MintNFT) tx;
                return signMessage(String.join("\n", getMintNFTMessagePart(mintNFT.getContentHash(), mintNFT.getRecipient(), token, fee), getNonceMessagePart(nonce)).getBytes());
            case "Transfer":
                Transfer transfer = (Transfer) tx;
                TokenId tokenId = transfer.getTokenId() != null ? transfer.getTokenId() : token;
                return signMessage(String.join("\n", getTransferMessagePart(transfer.getTo(), transfer.getAccountId(), transfer.getAmount(), tokenId, new BigInteger(transfer.getFee())), getNonceMessagePart(nonce)).getBytes());
            case "Withdraw":
                Withdraw withdraw = (Withdraw) tx;
                return signMessage(String.join("\n", getWithdrawMessagePart(withdraw.getTo(), withdraw.getAccountId(), withdraw.getAmount(), token, fee), getNonceMessagePart(nonce)).getBytes());
            case "WithdrawNFT":
                WithdrawNFT withdrawNft = (WithdrawNFT) tx;
                return signMessage(String.join("\n", getWithdrawNFTMessagePart(withdrawNft.getTo(), withdrawNft.getToken(), token, fee), getNonceMessagePart(nonce)).getBytes());
            case "Swap":
                return signMessage(String.join("\n", getSwapMessagePart(token, fee), getNonceMessagePart(nonce)).getBytes());
            default: throw new IllegalArgumentException(String.format("Transaction type {} is not supported yet", tx.getType()));
        }
    }

    @Override
    public <T extends TokenId> CompletableFuture<EthSignature> signOrder(Order order, T tokenSell, T tokenBuy) {
        String message = getOrderMessagePart(order.getRecipientAddress(), order.getAmount(), tokenSell, tokenBuy, order.getRatio(), order.getNonce());

        return signMessage(message.getBytes());
    }

    public <T extends ZkSyncTransaction> CompletableFuture<EthSignature> signBatch(Collection<T> transactions, Integer nonce, Token token, BigInteger fee) {
        String message = transactions.stream()
            .map(tx -> {
                switch (tx.getType()) {
                    case "ForcedExit":
                        ForcedExit forcedExit = (ForcedExit) tx;
                        return getForcedExitMessagePart(forcedExit.getTarget(), token, fee);
                    case "MintNFT":
                        MintNFT mintNFT = (MintNFT) tx;
                        return getMintNFTMessagePart(mintNFT.getRecipient(), mintNFT.getContentHash(), token, fee);
                    case "Transfer":
                        Transfer transfer = (Transfer) tx;
                        TokenId tokenId = transfer.getTokenId() != null ? transfer.getTokenId() : token;
                        return getTransferMessagePart(transfer.getTo(), transfer.getAccountId(), transfer.getAmount(), tokenId, new BigInteger(transfer.getFee()));
                    case "Withdraw":
                        Withdraw withdraw = (Withdraw) tx;
                        return getWithdrawMessagePart(withdraw.getTo(), withdraw.getAccountId(), withdraw.getAmount(), token, fee);
                    case "WithdrawNFT":
                        WithdrawNFT withdrawNft = (WithdrawNFT) tx;
                        return getWithdrawNFTMessagePart(withdrawNft.getTo(), withdrawNft.getToken(), token, fee);
                    case "Swap":
                        return getSwapMessagePart(token, fee);
                    default: throw new IllegalArgumentException(String.format("Transaction type {} is not supported by batch", tx.getType()));
                }
            })
            .collect(Collectors.joining("\n"));
        String result = String.join("\n", message, getNonceMessagePart(nonce));
        return signMessage(result.getBytes());
    }

    public CompletableFuture<EthSignature> signMessage(byte[] message) {
        return signMessage(message, true);
    }

    public CompletableFuture<EthSignature> signMessage(byte[] message, boolean addPrefix) {
        Sign.SignatureData sig = addPrefix ?
            Sign.signPrefixedMessage(message, credentials.getEcKeyPair()) :
            Sign.signMessage(message, credentials.getEcKeyPair());

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

    public CompletableFuture<Boolean> verifySignature(EthSignature signature, byte[] message) throws SignatureException {
        return verifySignature(signature, message, true);
    }

    public CompletableFuture<Boolean> verifySignature(EthSignature signature, byte[] message, boolean prefixed) throws SignatureException {
        byte[] sig = Numeric.hexStringToByteArray(signature.getSignature());
        Sign.SignatureData signatureData = new Sign.SignatureData(
            Arrays.copyOfRange(sig, 64, 65),
            Arrays.copyOfRange(sig, 0, 32),
            Arrays.copyOfRange(sig, 32, 64)
        );
        BigInteger publicKey = prefixed ?
            Sign.signedPrefixedMessageToKey(message, signatureData) :
            Sign.signedMessageToKey(message, signatureData);
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
