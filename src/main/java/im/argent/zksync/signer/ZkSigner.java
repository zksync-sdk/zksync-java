package im.argent.zksync.signer;

import im.argent.zksync.domain.Signature;
import im.argent.zksync.domain.transaction.ChangePubKey;
import im.argent.zksync.domain.transaction.Transfer;
import im.argent.zksync.domain.transaction.Withdraw;
import im.argent.zksync.exception.ZkSyncException;
import io.zksync.sdk.zkscrypto.lib.ZksCrypto;
import io.zksync.sdk.zkscrypto.lib.entity.ZksPackedPublicKey;
import io.zksync.sdk.zkscrypto.lib.entity.ZksPrivateKey;
import io.zksync.sdk.zkscrypto.lib.exception.ZksMusigTooLongException;
import io.zksync.sdk.zkscrypto.lib.exception.ZksSeedTooShortException;
import org.web3j.utils.Numeric;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static im.argent.zksync.signer.SigningUtils.*;

public class ZkSigner {

    private final ZksCrypto crypto;

    private final ZksPrivateKey privateKey;

    private final ZksPackedPublicKey publicKey;

    private final String publicKeyHash;

    private ZkSigner(ZksPrivateKey privateKey, ZksCrypto crypto) {
        this.crypto = crypto;
        this.privateKey = privateKey;

        System.out.println("Private key: " + Numeric.toHexString(privateKey.getData()));

        // Generate public key from private key
        publicKey = crypto.getPublicKey(privateKey);

        // Generate hash from public key
        publicKeyHash = Numeric.toHexStringNoPrefix(crypto.getPublicKeyHash(publicKey).getData());
    }

    public static ZkSigner fromSeed(byte[] seed) {

        // Load native library
        ZksCrypto crypto = ZksCrypto.load();

        try {

            // Generate private key from seed
            ZksPrivateKey privateKey = crypto.generatePrivateKey(seed);

            return new ZkSigner(privateKey, crypto);
        } catch (ZksSeedTooShortException e) {
            throw new ZkSyncException(e);
        }
    }

    public Signature sign(byte[] message) {
        try {
            final byte[] signature = crypto.signMessage(privateKey, message).getData();

            System.out.println("PK: " + Numeric.toHexString(privateKey.getData())
                    + "\nMessage: " + Numeric.toHexString(message)
                    + "\nSignature: " + Numeric.toHexString(signature).substring(2));

            return Signature
                    .builder()
                    .pubKey(Numeric.toHexStringNoPrefix(publicKey.getData()))
                    .signature(Numeric.toHexString(signature).substring(2))
                    .build();
        } catch (ZksMusigTooLongException e) {
            throw new ZkSyncException(e);
        }
    }

    public String getPublicKeyHash() {
        return "sync:" + publicKeyHash;
    }

    public String getPublicKey() {
        return Numeric.toHexString(publicKey.getData());
    }

    public ChangePubKey signChangePubKey(ChangePubKey changePubKey) {
        try {
            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputStream.write(0x07);
            outputStream.write(accountIdToBytes(changePubKey.getAccountId()));
            outputStream.write(addressToBytes(changePubKey.getAccount()));
            outputStream.write(addressToBytes(changePubKey.getNewPkHash()));
            outputStream.write(tokenIdToBytes(changePubKey.getFeeToken()));
            outputStream.write(feeToBytes(changePubKey.getFeeInteger()));
            outputStream.write(nonceToBytes(changePubKey.getNonce()));

            byte[] message = outputStream.toByteArray();

            System.out.println("Message to sign: " + Numeric.toHexString(message));

            final Signature signature = sign(message);

            changePubKey.setSignature(signature);

            return changePubKey;
        } catch (IOException e) {
            throw new ZkSyncException(e);
        }
    }

    public Transfer signTransfer(Transfer transfer) {
        try {
            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputStream.write(0x05);
            outputStream.write(accountIdToBytes(transfer.getAccountId()));
            outputStream.write(addressToBytes(transfer.getFrom()));
            outputStream.write(addressToBytes(transfer.getTo()));
            outputStream.write(tokenIdToBytes(transfer.getToken()));
            outputStream.write(amountPackedToBytes(transfer.getAmount()));
            outputStream.write(feeToBytes(transfer.getFeeInteger()));
            outputStream.write(nonceToBytes(transfer.getNonce()));

            byte[] message = outputStream.toByteArray();

            System.out.println("Amount: " + Numeric.toHexString(amountPackedToBytes(transfer.getAmount())));
            System.out.println("Fee: " + Numeric.toHexString(feeToBytes(transfer.getFeeInteger())));
            System.out.println("Message to sign: " + Numeric.toHexString(message));

            final Signature signature = sign(message);

            transfer.setSignature(signature);

            return transfer;
        } catch (IOException e) {
            throw new ZkSyncException(e);
        }
    }

    public Withdraw signWithdraw(Withdraw withdraw) {
        try {
            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputStream.write(0x03);
            outputStream.write(accountIdToBytes(withdraw.getAccountId()));
            outputStream.write(addressToBytes(withdraw.getFrom()));
            outputStream.write(addressToBytes(withdraw.getTo()));
            outputStream.write(tokenIdToBytes(withdraw.getToken()));
            outputStream.write(amountFullToBytes(withdraw.getAmount()));
            outputStream.write(feeToBytes(withdraw.getFeeInteger()));
            outputStream.write(nonceToBytes(withdraw.getNonce()));

            byte[] message = outputStream.toByteArray();

            System.out.println("Amount: " + Numeric.toHexString(amountPackedToBytes(withdraw.getAmount())));
            System.out.println("Fee: " + Numeric.toHexString(feeToBytes(withdraw.getFeeInteger())));
            System.out.println("Message to sign: " + Numeric.toHexString(message));

            final Signature signature = sign(message);

            withdraw.setSignature(signature);

            return withdraw;
        } catch (IOException e) {
            throw new ZkSyncException(e);
        }
    }
}
