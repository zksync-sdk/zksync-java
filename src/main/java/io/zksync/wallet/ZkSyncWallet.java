package io.zksync.wallet;

import io.zksync.domain.TimeRange;
import io.zksync.domain.auth.ChangePubKeyVariant;
import io.zksync.domain.fee.TransactionFee;
import io.zksync.domain.state.AccountState;
import io.zksync.domain.swap.Order;
import io.zksync.domain.token.NFT;
import io.zksync.domain.token.Token;
import io.zksync.domain.transaction.ZkSyncTransaction;
import io.zksync.ethereum.EthereumProvider;
import io.zksync.provider.DefaultProvider;
import io.zksync.provider.Provider;
import io.zksync.signer.EthSigner;
import io.zksync.signer.ZkSigner;
import io.zksync.transport.ZkSyncTransport;

import java.math.BigInteger;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.web3j.protocol.Web3j;
import org.web3j.tuples.generated.Tuple2;
import org.web3j.tx.gas.ContractGasProvider;

public interface ZkSyncWallet {

    public static <A extends ChangePubKeyVariant, T extends EthSigner<A>> DefaultZkSyncWallet<A, T> build(T ethSigner, ZkSigner zkSigner, ZkSyncTransport transport) {
        return new DefaultZkSyncWallet<>(ethSigner, zkSigner, new DefaultProvider(transport));
    }

    public static <A extends ChangePubKeyVariant, T extends EthSigner<A>> DefaultZkSyncWallet<A, T> build(T ethSigner, ZkSigner zkSigner, Provider provider) {
        return new DefaultZkSyncWallet<>(ethSigner, zkSigner, provider);
    }

    String setSigningKey(@NotNull TransactionFee fee, @Nullable Integer nonce, @NotNull boolean onchainAuth, @Nullable TimeRange timeRange);

    String syncTransfer(@NotNull String to, @NotNull BigInteger amount, @NotNull TransactionFee fee, @Nullable Integer nonce, @Nullable TimeRange timeRange);

    String syncWithdraw(@NotNull String ethAddress,
                        @NotNull BigInteger amount,
                        @NotNull TransactionFee fee,
                        @Nullable Integer nonce,
                        @NotNull boolean fastProcessing,
                        @Nullable TimeRange timeRange);

    String syncForcedExit(@NotNull String target, @NotNull TransactionFee fee, @Nullable Integer nonce, @Nullable TimeRange timeRange);

    String syncMintNFT(@NotNull String recipient, @NotNull String contentHash, @NotNull TransactionFee fee, @Nullable Integer nonce);

    String syncWithdrawNFT(@NotNull String to, @NotNull NFT token, @NotNull TransactionFee fee, @Nullable Integer nonce, @Nullable TimeRange timeRange);

    List<String> syncTransferNFT(@NotNull String to, @NotNull NFT token, @NotNull TransactionFee fee, @Nullable Integer nonce, @Nullable TimeRange timeRange);

    String syncSwap(@NotNull Order order1, @NotNull Order order2, @NotNull BigInteger amount1, @NotNull BigInteger amount2, @NotNull TransactionFee fee, @Nullable Integer nonce);

    Order buildSignedOrder(@NotNull String recipient, @NotNull Token sell, @NotNull Token buy, @NotNull Tuple2<BigInteger, BigInteger> ratio, @NotNull BigInteger amount, @Nullable Integer nonce, @Nullable TimeRange timeRange);

    Order buildSignedLimitOrder(@NotNull String recipient, @NotNull Token sell, @NotNull Token buy, @NotNull Tuple2<BigInteger, BigInteger> ratio, @Nullable Integer nonce, @Nullable TimeRange timeRange);

    <T extends ZkSyncTransaction> String submitTransaction(SignedTransaction<T> transaction);

    boolean isSigningKeySet();

    AccountState getState();

    Provider getProvider();

    String getPubKeyHash();

    Integer getAccountId();

    String getAddress();

    EthereumProvider createEthereumProvider(Web3j web3j, ContractGasProvider contractGasProvider);
}

