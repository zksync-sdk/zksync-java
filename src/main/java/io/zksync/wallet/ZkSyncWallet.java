package io.zksync.wallet;

import io.zksync.domain.fee.TransactionFee;
import io.zksync.domain.fee.TransactionFeeDetails;
import io.zksync.domain.fee.TransactionType;
import io.zksync.domain.state.AccountState;
import io.zksync.ethereum.EthereumProvider;
import io.zksync.provider.DefaultProvider;
import io.zksync.provider.Provider;
import io.zksync.signer.EthSigner;
import io.zksync.signer.ZkSigner;
import io.zksync.transport.ZkSyncTransport;

import java.math.BigInteger;

import org.web3j.protocol.Web3j;
import org.web3j.tx.gas.ContractGasProvider;

public interface ZkSyncWallet {

    static DefaultZkSyncWallet build(EthSigner ethSigner, ZkSigner zkSigner, ZkSyncTransport transport) {
        return new DefaultZkSyncWallet(ethSigner, zkSigner, new DefaultProvider(transport));
    }

    static DefaultZkSyncWallet build(EthSigner ethSigner, ZkSigner zkSigner, Provider provider) {
        return new DefaultZkSyncWallet(ethSigner, zkSigner, provider);
    }

    String setSigningKey(TransactionFee fee, Integer nonce, boolean onchainAuth);

    String syncTransfer(String to, BigInteger amount, TransactionFee fee, Integer nonce);

    String syncWithdraw(String ethAddress,
                        BigInteger amount,
                        TransactionFee fee,
                        Integer nonce,
                        boolean fastProcessing);

    String syncForcedExit(String target, TransactionFee fee, Integer nonce);

    boolean isSingingKeySet();

    AccountState getState();

    TransactionFeeDetails getTransactionFee(TransactionType type, String tokenIdentifier);

    TransactionFeeDetails getTransactionFee(TransactionType type, String address, String tokenIdentifier);

    Provider getProvider();

    EthereumProvider createEthereumProvider(Web3j web3j, ContractGasProvider contractGasProvider);
}

