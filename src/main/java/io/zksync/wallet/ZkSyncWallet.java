package io.zksync.wallet;

import io.zksync.domain.TimeRange;
import io.zksync.domain.fee.TransactionFee;
import io.zksync.domain.state.AccountState;
import io.zksync.domain.token.NFT;
import io.zksync.ethereum.EthereumProvider;
import io.zksync.provider.DefaultProvider;
import io.zksync.provider.Provider;
import io.zksync.signer.EthSigner;
import io.zksync.signer.ZkSigner;
import io.zksync.transport.ZkSyncTransport;

import java.math.BigInteger;
import java.util.List;

import org.web3j.protocol.Web3j;
import org.web3j.tx.gas.ContractGasProvider;

public interface ZkSyncWallet {

    static DefaultZkSyncWallet build(EthSigner ethSigner, ZkSigner zkSigner, ZkSyncTransport transport) {
        return new DefaultZkSyncWallet(ethSigner, zkSigner, new DefaultProvider(transport));
    }

    static DefaultZkSyncWallet build(EthSigner ethSigner, ZkSigner zkSigner, Provider provider) {
        return new DefaultZkSyncWallet(ethSigner, zkSigner, provider);
    }

    String setSigningKey(TransactionFee fee, Integer nonce, boolean onchainAuth, TimeRange timeRange);

    String syncTransfer(String to, BigInteger amount, TransactionFee fee, Integer nonce, TimeRange timeRange);

    String syncWithdraw(String ethAddress,
                        BigInteger amount,
                        TransactionFee fee,
                        Integer nonce,
                        boolean fastProcessing,
                        TimeRange timeRange);

    String syncForcedExit(String target, TransactionFee fee, Integer nonce, TimeRange timeRange);

    String syncMintNFT(String recipient, String contentHash, TransactionFee fee, Integer nonce);

    String syncWithdrawNFT(String to, NFT token, TransactionFee fee, Integer nonce, TimeRange timeRange);

    List<String> syncTransferNFT(String to, NFT token, TransactionFee fee, Integer nonce, TimeRange timeRange);

    String syncSwap(TransactionFee fee, Integer nonce);

    boolean isSigningKeySet();

    AccountState getState();

    Provider getProvider();

    EthereumProvider createEthereumProvider(Web3j web3j, ContractGasProvider contractGasProvider);
}

