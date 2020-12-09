package io.zksync.signer;

import io.zksync.domain.ChainId;
import io.zksync.domain.Signature;
import io.zksync.domain.transaction.ChangePubKey;
import io.zksync.domain.transaction.ForcedExit;
import io.zksync.domain.transaction.Transfer;
import io.zksync.domain.transaction.Withdraw;
import io.zksync.exception.ZkSyncException;
import io.zksync.exception.ZkSyncIncorrectCredentialsException;
import io.zksync.sdk.zkscrypto.lib.ZksCrypto;
import io.zksync.sdk.zkscrypto.lib.entity.ZksPackedPublicKey;
import io.zksync.sdk.zkscrypto.lib.entity.ZksPrivateKey;
import io.zksync.sdk.zkscrypto.lib.exception.ZksMusigTooLongException;
import io.zksync.sdk.zkscrypto.lib.exception.ZksSeedTooShortException;
import io.zksync.signer.EthSignature.SignatureType;

import org.web3j.utils.Numeric;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SignatureException;

import static io.zksync.signer.SigningUtils.*;

public class ZkSigner {

    private static final ZksCrypto crypto = ZksCrypto.load();
    
    public static final String MESSAGE = "Access zkSync account.\n\nOnly sign this message for a trusted client!";

    private final ZksPrivateKey privateKey;

    private final ZksPackedPublicKey publicKey;

    private final String publicKeyHash;

    private ZkSigner(ZksPrivateKey privateKey) {
        this.privateKey = privateKey;

        System.out.println("Private key: " + Numeric.toHexString(privateKey.getData()));

        // Generate public key from private key
        publicKey = crypto.getPublicKey(privateKey);

        // Generate hash from public key
        publicKeyHash = Numeric.toHexStringNoPrefix(crypto.getPublicKeyHash(publicKey).getData());
    }

    public static ZkSigner fromSeed(byte[] seed) {
        try {
            // Generate private key from seed
            ZksPrivateKey privateKey = crypto.generatePrivateKey(seed);

            return new ZkSigner(privateKey);
        } catch (ZksSeedTooShortException e) {
            throw new ZkSyncException(e);
        }
    }

    public static ZkSigner fromRawPrivateKey(byte[] rawPrivateKey){
        try {
            // Generate private key from seed
            ZksPrivateKey privateKey = crypto.generatePrivateKey(rawPrivateKey);
            privateKey.data = rawPrivateKey;

            return new ZkSigner(privateKey);
        } catch (ZksSeedTooShortException e) {
            throw new ZkSyncException(e);
        }
    }

    public static ZkSigner fromEthSigner(EthSigner ethSigner, ChainId chainId) {
        String message;
        if (chainId == ChainId.Mainnet) {
            message = MESSAGE;
        } else {
            message = String.format("%s\nChain ID: %d.", MESSAGE, chainId.getId());
        }
        EthSignature signature = ethSigner.signMessage(message);
        if (signature.getType() != SignatureType.EthereumSignature) {
            throw new ZkSyncIncorrectCredentialsException("Invalid signature type: " + signature.getType());
        }
        try {
            if (!ethSigner.verifySignature(signature, message)) {
                throw new ZkSyncIncorrectCredentialsException("Failed to verify signature: " + signature.getSignature());
            }
        } catch (SignatureException e) {
            throw new ZkSyncIncorrectCredentialsException("Failed to verify signature: " + signature.getSignature(), e);
        }

        return fromSeed(Numeric.hexStringToByteArray(signature.getSignature()));

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

    public ForcedExit signForcedExit(ForcedExit forcedExit) {
        try {
            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputStream.write(0x08);
            outputStream.write(accountIdToBytes(forcedExit.getInitiatorAccountId()));
            outputStream.write(addressToBytes(forcedExit.getTarget()));
            outputStream.write(tokenIdToBytes(forcedExit.getToken()));
            outputStream.write(feeToBytes(forcedExit.getFeeInteger()));
            outputStream.write(nonceToBytes(forcedExit.getNonce()));

            byte[] message = outputStream.toByteArray();

            final Signature signature = sign(message);

            forcedExit.setSignature(signature);

            return forcedExit;
        } catch (IOException e) {
            throw new ZkSyncException(e);
        }
    }
}
