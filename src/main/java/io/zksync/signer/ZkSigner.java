package io.zksync.signer;

import io.zksync.domain.ChainId;
import io.zksync.domain.Signature;
import io.zksync.domain.auth.ChangePubKeyVariant;
import io.zksync.domain.swap.Order;
import io.zksync.domain.transaction.ChangePubKey;
import io.zksync.domain.transaction.ForcedExit;
import io.zksync.domain.transaction.MintNFT;
import io.zksync.domain.transaction.Swap;
import io.zksync.domain.transaction.Transfer;
import io.zksync.domain.transaction.Withdraw;
import io.zksync.domain.transaction.WithdrawNFT;
import io.zksync.exception.ZkSyncException;
import io.zksync.exception.ZkSyncIncorrectCredentialsException;
import io.zksync.sdk.zkscrypto.lib.ZksCrypto;
import io.zksync.sdk.zkscrypto.lib.entity.ZksPackedPublicKey;
import io.zksync.sdk.zkscrypto.lib.entity.ZksPrivateKey;
import io.zksync.sdk.zkscrypto.lib.entity.ZksSignature;
import io.zksync.sdk.zkscrypto.lib.exception.ZksMusigTooLongException;
import io.zksync.sdk.zkscrypto.lib.exception.ZksSeedTooShortException;
import io.zksync.signer.EthSignature.SignatureType;

import org.apache.commons.lang.ArrayUtils;
import org.web3j.utils.Numeric;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static io.zksync.signer.SigningUtils.*;

public class ZkSigner {

    private static final ZksCrypto crypto = ZksCrypto.load();
    
    public static final String MESSAGE = "Access zkSync account.\n\nOnly sign this message for a trusted client!";

    public static final Integer TRANSACTION_VERSION = 0x01;

    private final ZksPrivateKey privateKey;

    private final ZksPackedPublicKey publicKey;

    private final String publicKeyHash;

    private ZkSigner(ZksPrivateKey privateKey) {
        this.privateKey = privateKey;

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

    public static ZkSigner fromRawPrivateKey(byte[] rawPrivateKey) {
        ZksPrivateKey privateKey = new ZksPrivateKey.ByReference();
        privateKey.data = rawPrivateKey;

        return new ZkSigner(privateKey);
    }

    public static ZkSigner fromEthSigner(EthSigner<?> ethSigner, ChainId chainId) {
        String message = MESSAGE;
        if (chainId != ChainId.Mainnet) {
            message = String.format("%s\nChain ID: %d.", MESSAGE, chainId.getId());
        }
        EthSignature signature = ethSigner.signMessage(message.getBytes(), true).join();
        if (signature.getType() != SignatureType.EthereumSignature) {
            throw new ZkSyncIncorrectCredentialsException("Invalid signature type: " + signature.getType());
        }

        return fromSeed(Numeric.hexStringToByteArray(signature.getSignature()));

    }

    public Signature sign(byte[] message) {
        try {
            final byte[] signature = crypto.signMessage(privateKey, message).getData();

            return Signature
                    .builder()
                    .pubKey(Numeric.toHexStringNoPrefix(publicKey.getData()))
                    .signature(Numeric.toHexString(signature).substring(2))
                    .build();
        } catch (ZksMusigTooLongException e) {
            throw new ZkSyncException(e);
        }
    }

    public boolean verify(byte[] message, Signature signature) {
        ZksPackedPublicKey zksPublicKey = new ZksPackedPublicKey.ByReference();
        zksPublicKey.data = Numeric.hexStringToByteArray(signature.getPubKey());
        ZksSignature zksSignature = new ZksSignature.ByReference();
        zksSignature.data = Numeric.hexStringToByteArray(signature.getSignature());

        return crypto.verifySignature(zksPublicKey, zksSignature, message);
    }

    public String getPublicKeyHash() {
        return "sync:" + publicKeyHash;
    }

    public String getPublicKey() {
        return Numeric.toHexString(publicKey.getData());
    }

    public <T extends ChangePubKeyVariant> ChangePubKey<T> signChangePubKey(ChangePubKey<T> changePubKey) {
        try {
            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputStream.write(0xff - 0x07);
            outputStream.write(TRANSACTION_VERSION);
            outputStream.write(accountIdToBytes(changePubKey.getAccountId()));
            outputStream.write(addressToBytes(changePubKey.getAccount()));
            outputStream.write(addressToBytes(changePubKey.getNewPkHash()));
            outputStream.write(tokenIdToBytes(changePubKey.getFeeToken()));
            outputStream.write(feeToBytes(changePubKey.getFeeInteger()));
            outputStream.write(nonceToBytes(changePubKey.getNonce()));
            outputStream.write(numberToBytesBE(changePubKey.getTimeRange().getValidFrom(), 8));
            outputStream.write(numberToBytesBE(changePubKey.getTimeRange().getValidUntil(), 8));

            byte[] message = outputStream.toByteArray();

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
            outputStream.write(0xff - 0x05);
            outputStream.write(TRANSACTION_VERSION);
            outputStream.write(accountIdToBytes(transfer.getAccountId()));
            outputStream.write(addressToBytes(transfer.getFrom()));
            outputStream.write(addressToBytes(transfer.getTo()));
            outputStream.write(tokenIdToBytes(transfer.getToken()));
            outputStream.write(amountPackedToBytes(transfer.getAmount()));
            outputStream.write(feeToBytes(transfer.getFeeInteger()));
            outputStream.write(nonceToBytes(transfer.getNonce()));
            outputStream.write(numberToBytesBE(transfer.getTimeRange().getValidFrom(), 8));
            outputStream.write(numberToBytesBE(transfer.getTimeRange().getValidUntil(), 8));

            byte[] message = outputStream.toByteArray();

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
            outputStream.write(0xff - 0x03);
            outputStream.write(TRANSACTION_VERSION);
            outputStream.write(accountIdToBytes(withdraw.getAccountId()));
            outputStream.write(addressToBytes(withdraw.getFrom()));
            outputStream.write(addressToBytes(withdraw.getTo()));
            outputStream.write(tokenIdToBytes(withdraw.getToken()));
            outputStream.write(amountFullToBytes(withdraw.getAmount()));
            outputStream.write(feeToBytes(withdraw.getFeeInteger()));
            outputStream.write(nonceToBytes(withdraw.getNonce()));
            outputStream.write(numberToBytesBE(withdraw.getTimeRange().getValidFrom(), 8));
            outputStream.write(numberToBytesBE(withdraw.getTimeRange().getValidUntil(), 8));

            byte[] message = outputStream.toByteArray();

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
            outputStream.write(0xff - 0x08);
            outputStream.write(TRANSACTION_VERSION);
            outputStream.write(accountIdToBytes(forcedExit.getInitiatorAccountId()));
            outputStream.write(addressToBytes(forcedExit.getTarget()));
            outputStream.write(tokenIdToBytes(forcedExit.getToken()));
            outputStream.write(feeToBytes(forcedExit.getFeeInteger()));
            outputStream.write(nonceToBytes(forcedExit.getNonce()));
            outputStream.write(numberToBytesBE(forcedExit.getTimeRange().getValidFrom(), 8));
            outputStream.write(numberToBytesBE(forcedExit.getTimeRange().getValidUntil(), 8));

            byte[] message = outputStream.toByteArray();

            final Signature signature = sign(message);

            forcedExit.setSignature(signature);

            return forcedExit;
        } catch (IOException e) {
            throw new ZkSyncException(e);
        }
    }

    public MintNFT signMintNFT(MintNFT mintNFT) {
        try{
            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputStream.write(0xff - 0x09);
            outputStream.write(TRANSACTION_VERSION);
            outputStream.write(accountIdToBytes(mintNFT.getCreatorId()));
            outputStream.write(addressToBytes(mintNFT.getCreatorAddress()));
            outputStream.write(Numeric.hexStringToByteArray(mintNFT.getContentHash()));
            outputStream.write(addressToBytes(mintNFT.getRecipient()));
            outputStream.write(tokenIdToBytes(mintNFT.getFeeToken()));
            outputStream.write(feeToBytes(mintNFT.getFeeInteger()));
            outputStream.write(nonceToBytes(mintNFT.getNonce()));

            byte[] message = outputStream.toByteArray();

            final Signature signature = sign(message);

            mintNFT.setSignature(signature);

            return mintNFT;
        } catch (IOException e) {
            throw new ZkSyncException(e);
        }
    }

    public WithdrawNFT signWithdrawNFT(WithdrawNFT withdrawNFT) {
        try {
            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputStream.write(0xff - 0x0a);
            outputStream.write(TRANSACTION_VERSION);
            outputStream.write(accountIdToBytes(withdrawNFT.getAccountId()));
            outputStream.write(addressToBytes(withdrawNFT.getFrom()));
            outputStream.write(addressToBytes(withdrawNFT.getTo()));
            outputStream.write(tokenIdToBytes(withdrawNFT.getToken()));
            outputStream.write(tokenIdToBytes(withdrawNFT.getFeeToken()));
            outputStream.write(feeToBytes(withdrawNFT.getFeeInteger()));
            outputStream.write(nonceToBytes(withdrawNFT.getNonce()));
            outputStream.write(numberToBytesBE(withdrawNFT.getTimeRange().getValidFrom(), 8));
            outputStream.write(numberToBytesBE(withdrawNFT.getTimeRange().getValidUntil(), 8));

            byte[] message = outputStream.toByteArray();

            final Signature signature = sign(message);

            withdrawNFT.setSignature(signature);

            return withdrawNFT;
        } catch (IOException e) {
            throw new ZkSyncException(e);
        }
    }

    public Swap signSwap(Swap swap) {
        try {
            final byte[] order1 = getOrderBytes(swap.getOrders().component1());
            final byte[] order2 = getOrderBytes(swap.getOrders().component2());
            final byte[] ordersHash = crypto.rescueHashOrders(ArrayUtils.addAll(order1, order2)).getData();
            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputStream.write(0xff - 0x0b);
            outputStream.write(TRANSACTION_VERSION);
            outputStream.write(accountIdToBytes(swap.getSubmitterId()));
            outputStream.write(addressToBytes(swap.getSubmitterAddress()));
            outputStream.write(nonceToBytes(swap.getNonce()));
            outputStream.write(ordersHash);
            outputStream.write(tokenIdToBytes(swap.getFeeToken()));
            outputStream.write(feeToBytes(swap.getFeeInteger()));
            outputStream.write(amountPackedToBytes(swap.getAmounts().component1()));
            outputStream.write(amountPackedToBytes(swap.getAmounts().component2()));

            byte[] message = outputStream.toByteArray();

            final Signature signature = sign(message);

            swap.setSignature(signature);

            return swap;
        } catch (IOException e) {
            throw new ZkSyncException(e);
        }
    }

    public Order signOrder(Order order) {
        byte[] message = getOrderBytes(order);

        final Signature signature = sign(message);

        order.setSignature(signature);

        return order;
    }

    public byte[] getOrderBytes(Order order) {
        try {
            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputStream.write(0x6f); // ASCII 'o' in hex for (o)rder
            outputStream.write(TRANSACTION_VERSION);
            outputStream.write(accountIdToBytes(order.getAccountId()));
            outputStream.write(addressToBytes(order.getRecipientAddress()));
            outputStream.write(nonceToBytes(order.getNonce()));
            outputStream.write(tokenIdToBytes(order.getTokenSell()));
            outputStream.write(tokenIdToBytes(order.getTokenBuy()));
            outputStream.write(bigIntToBytesBE(order.getRatio().component1(), 15));
            outputStream.write(bigIntToBytesBE(order.getRatio().component2(), 15));
            outputStream.write(amountPackedToBytes(order.getAmount()));
            outputStream.write(numberToBytesBE(order.getTimeRange().getValidFrom(), 8));
            outputStream.write(numberToBytesBE(order.getTimeRange().getValidUntil(), 8));

            byte[] message = outputStream.toByteArray();

            return message;
        } catch (IOException e) {
            throw new ZkSyncException(e);
        }
    }
}
