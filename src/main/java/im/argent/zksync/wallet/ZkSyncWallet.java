package im.argent.zksync.wallet;

import im.argent.zksync.domain.fee.TransactionFee;
import im.argent.zksync.domain.fee.TransactionFeeDetails;
import im.argent.zksync.domain.fee.TransactionFeeRequest;
import im.argent.zksync.domain.fee.TransactionType;
import im.argent.zksync.domain.state.AccountState;
import im.argent.zksync.provider.Provider;
import im.argent.zksync.signer.EthSigner;
import im.argent.zksync.signer.ZkSigner;
import im.argent.zksync.transport.ZkSyncTransport;

import java.math.BigInteger;

public interface ZkSyncWallet {

    static DefaultZkSyncWallet build(EthSigner ethSigner, ZkSigner zkSigner, ZkSyncTransport transport) {
        return new DefaultZkSyncWallet(ethSigner, zkSigner, transport);
    }

    String setSigningKey(TransactionFee fee, Integer nonce, boolean onchainAuth);

    String syncTransfer(String to, String tokenIdentifier, BigInteger amount, BigInteger fee, Integer nonce);

    String syncWithdraw(String ethAddress,
                        String tokenIdentifier,
                        BigInteger amount,
                        BigInteger fee,
                        Integer nonce,
                        boolean fastProcessing);

    String syncForcedExit(String target, String tokenIdentifier, BigInteger fee, Integer nonce);

    AccountState getState();

    TransactionFeeDetails getTransactionFee(TransactionType type, String tokenIdentifier);

    TransactionFeeDetails getTransactionFee(TransactionType type, String address, String tokenIdentifier);

    Provider getProvider();
}

