package io.zksync.wallet;

import io.zksync.domain.TimeRange;
import io.zksync.domain.auth.ChangePubKeyVariant;
import io.zksync.domain.fee.TransactionFee;
import io.zksync.domain.state.AccountState;
import io.zksync.domain.swap.Order;
import io.zksync.domain.token.NFT;
import io.zksync.domain.token.TokenId;
import io.zksync.domain.token.Tokens;
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

    /**
     * Send set signing key transaction
     * 
     * @param fee - Fee amount for paying the transaction
     * @param nonce - Nonce value
     * @param onchainAuth - Use authentication onchain
     * @param timeRange - Timerange of validity of the transcation
     * @return - Hash of the sent transaction in hex string
     */
    String setSigningKey(@NotNull TransactionFee fee, @Nullable Integer nonce, @NotNull boolean onchainAuth, @Nullable TimeRange timeRange);

    /**
     * Send transfer coins (or tokens) transaction
     * 
     * @param to - Ethereum address of the receiver of the funds
     * @param amount - Amount of the funds to be transferred
     * @param fee - Fee amount for paying the transaction
     * @param nonce - Nonce value
     * @param timeRange - Timerange of validity of the transcation
     * @return - Hash of the sent transaction in hex string
     */
    String syncTransfer(@NotNull String to, @NotNull BigInteger amount, @NotNull TransactionFee fee, @Nullable Integer nonce, @Nullable TimeRange timeRange);

    /**
     * Send withdraw coins (or tokens) transaction
     * Given funds amount will be withdrawn to the wallet on Ethereum L1 network
     * 
     * @param ethAddress - Ethereum address of the receiver of the funds
     * @param amount - Amount of the funds to be withdrawn
     * @param fee - Fee amount for paying the transaction
     * @param nonce - Nonce value
     * @param fastProcessing - Increase speed of the execution
     * @param timeRange - Timerange of validity of the transcation
     * @return - Hash of the sent transaction in hex string
     */
    String syncWithdraw(@NotNull String ethAddress,
                        @NotNull BigInteger amount,
                        @NotNull TransactionFee fee,
                        @Nullable Integer nonce,
                        @NotNull boolean fastProcessing,
                        @Nullable TimeRange timeRange);

    /**
     * Send forced exit transaction
     * 
     * @param target - Ethereum address of the receiver of the funds
     * @param fee - Fee amount for paying the transaction
     * @param nonce - Nonce value
     * @param timeRange - Timerange of validity of the transcation
     * @return - Hash of the sent transaction in hex string
     */
    String syncForcedExit(@NotNull String target, @NotNull TransactionFee fee, @Nullable Integer nonce, @Nullable TimeRange timeRange);

    /**
     * Send mint NFT transaction
     * 
     * @param recipient - Ethereum address of the receiver of the NFT
     * @param contentHash - Hash for creation Non-fundible token
     * @param fee - Fee amount for paying the transaction
     * @param nonce - Nonce value
     * @return - Hash of the sent transaction in hex string
     */
    String syncMintNFT(@NotNull String recipient, @NotNull String contentHash, @NotNull TransactionFee fee, @Nullable Integer nonce);

    /**
     * Send withdraw NFT transaction
     * NFT will be withdrawn to the wallet in Ethereum L1 network
     * 
     * @param to - Ethereum address of the receiver of the NFT
     * @param token - Existing Non-fundible token
     * @param fee - Fee amount for paying the transaction
     * @param nonce - Nonce value
     * @param timeRange - Timerange of validity of the transcation
     * @return - Hash of the sent transaction in hex string
     */
    String syncWithdrawNFT(@NotNull String to, @NotNull NFT token, @NotNull TransactionFee fee, @Nullable Integer nonce, @Nullable TimeRange timeRange);

    /**
     * Send transfer NFT transaction
     * 
     * @param to - Ethereum address of the receiver of the NFT
     * @param token - Existing Non-fundible token
     * @param fee - Fee amount for paying the transaction
     * @param nonce - Nonce value
     * @param timeRange - Timerange of validity of the transcation
     * @return - List of 2 hashes of the sent transactions in hex string
     */
    List<String> syncTransferNFT(@NotNull String to, @NotNull NFT token, @NotNull TransactionFee fee, @Nullable Integer nonce, @Nullable TimeRange timeRange);

    /**
     * Send swap transaction
     * 
     * @param order1 - Signed order
     * @param order2 - Signed order
     * @param amount1 - Amount funds to be swapped
     * @param amount2 - Amount funds to be swapped
     * @param fee - Fee amount for paying the transaction
     * @param nonce - Nonce value
     * @return - Hash of the sent transaction in hex string
     */
    String syncSwap(@NotNull Order order1, @NotNull Order order2, @NotNull BigInteger amount1, @NotNull BigInteger amount2, @NotNull TransactionFee fee, @Nullable Integer nonce);

    /**
     * Build swap order
     * 
     * @param recipient - Ethereum address of the receiver of the funds
     * @param sell - Token to sell
     * @param buy - Token to buy
     * @param ratio - Swap ratio
     * @param amount - Amount to swap
     * @param nonce - Nonce value
     * @param timeRange - Timerange of validity of the order
     * @return - Signed order object
     */
    <T extends TokenId> Order buildSignedOrder(@NotNull String recipient, @NotNull T sell, @NotNull T buy, @NotNull Tuple2<BigInteger, BigInteger> ratio, @NotNull BigInteger amount, @Nullable Integer nonce, @Nullable TimeRange timeRange);

    /**
     * Build swap limit order
     * 
     * @param recipient - Ethereum address of the receiver of the funds
     * @param sell - Token to sell
     * @param buy - Token to buy
     * @param ratio - Swap ratio
     * @param nonce - Nonce value
     * @param timeRange - Timerange of validity of the order
     * @return - Signed order object
     */
    <T extends TokenId> Order buildSignedLimitOrder(@NotNull String recipient, @NotNull T sell, @NotNull T buy, @NotNull Tuple2<BigInteger, BigInteger> ratio, @Nullable Integer nonce, @Nullable TimeRange timeRange);

    /**
     * Submit signed transaction to ZkSync network
     * 
     * @param <T> - ZkSyncTransaction transaction type
     * @param transaction - Prepared signed transaction
     * @return - Hash of the sent transaction in hex string
     */
    <T extends ZkSyncTransaction> String submitTransaction(SignedTransaction<T> transaction);

    /**
     * Check if wallet public key hash is set
     * 
     * @return - True if pubkey hash is set otherwise false
     */
    boolean isSigningKeySet();

    /**
     * Get current account state
     * 
     * @return - State object
     */
    AccountState getState();

    /**
     * Get low level ZkSync API provider
     * 
     * @return - Provider
     */
    Provider getProvider();

    /**
     * Get wallet public key hash
     * 
     * @return - Pubkey hash in ZkSync format
     */
    String getPubKeyHash();

    /**
     * Get id of the account within current ZkSync network
     * 
     * @return - Account Id
     */
    Integer getAccountId();

    /**
     * Get current wallet address
     * 
     * @return - Wallet address in hex string
     */
    String getAddress();

    /**
     * Get list of the supported tokens by current ZkSync network
     * 
     * @return - Token list object
     */
    Tokens getTokens();

    /**
     * Send request to enable 2-Factor authentication
     * 
     * @return true if successful, false otherwise
     */
    boolean enable2FA();

    /**
     * Send request to disable 2-Factor authentication
     * 
     * @param pubKeyHash - ZkSync public key hash of the account
     * @return true if successful, false otherwise
     */
    boolean disable2FA(@Nullable String pubKeyHash);

    EthereumProvider createEthereumProvider(Web3j web3j, ContractGasProvider contractGasProvider);
}

