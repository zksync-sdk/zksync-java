package io.zksync.wallet;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.web3j.protocol.Web3j;
import org.web3j.tuples.generated.Tuple2;
import org.web3j.tx.gas.ContractGasProvider;

import io.reactivex.annotations.Nullable;
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
import io.zksync.provider.AsyncProvider;
import io.zksync.provider.DefaultAsyncProvider;
import io.zksync.signer.EthSigner;
import io.zksync.signer.ZkSigner;
import io.zksync.transport.ZkSyncTransport;

public interface ZkASyncWallet {

    public static <A extends ChangePubKeyVariant, T extends EthSigner<A>> DefaultZkASyncWallet<A, T> build(T ethSigner, ZkSigner zkSigner, ZkSyncTransport transport) {
        return new DefaultZkASyncWallet<>(ethSigner, zkSigner, new DefaultAsyncProvider(transport));
    }

    public static <A extends ChangePubKeyVariant, T extends EthSigner<A>> DefaultZkASyncWallet<A, T> build(T ethSigner, ZkSigner zkSigner, AsyncProvider provider) {
        return new DefaultZkASyncWallet<>(ethSigner, zkSigner, provider);
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
    CompletableFuture<String> setSigningKey(TransactionFee fee, Integer nonce, boolean onchainAuth, TimeRange timeRange);

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
    CompletableFuture<String> syncTransfer(String to, BigInteger amount, TransactionFee fee, Integer nonce, TimeRange timeRange);

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
    CompletableFuture<String> syncWithdraw(String ethAddress,
                        BigInteger amount,
                        TransactionFee fee,
                        Integer nonce,
                        boolean fastProcessing,
                        TimeRange timeRange);

    /**
     * Send forced exit transaction
     * 
     * @param target - Ethereum address of the receiver of the funds
     * @param fee - Fee amount for paying the transaction
     * @param nonce - Nonce value
     * @param timeRange - Timerange of validity of the transcation
     * @return - Hash of the sent transaction in hex string
     */
    CompletableFuture<String> syncForcedExit(String target, TransactionFee fee, Integer nonce, TimeRange timeRange);

    /**
     * Send mint NFT transaction
     * 
     * @param recipient - Ethereum address of the receiver of the NFT
     * @param contentHash - Hash for creation Non-fundible token
     * @param fee - Fee amount for paying the transaction
     * @param nonce - Nonce value
     * @return - Hash of the sent transaction in hex string
     */
    CompletableFuture<String> syncMintNFT(String recipient, String contentHash, TransactionFee fee, Integer nonce);

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
    CompletableFuture<String> syncWithdrawNFT(String to, NFT token, TransactionFee fee, Integer nonce, TimeRange timeRange);

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
    CompletableFuture<List<String>> syncTransferNFT(String to, NFT token, TransactionFee fee, Integer nonce, TimeRange timeRange);

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
    CompletableFuture<String> syncSwap(Order order1, Order order2, BigInteger amount1, BigInteger amount2, TransactionFee fee, Integer nonce);

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
    <T extends TokenId> CompletableFuture<Order> buildSignedOrder(String recipient, T sell, T buy, Tuple2<BigInteger, BigInteger> ratio, BigInteger amount, Integer nonce, TimeRange timeRange);

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
    <T extends TokenId> CompletableFuture<Order> buildSignedLimitOrder(String recipient, T sell, T buy, Tuple2<BigInteger, BigInteger> ratio, Integer nonce, TimeRange timeRange);

    /**
     * Submit signed transaction to ZkSync network
     * 
     * @param <T> - ZkSyncTransaction transaction type
     * @param transaction - Prepared signed transaction
     * @return - Hash of the sent transaction in hex string
     */
    <T extends ZkSyncTransaction> CompletableFuture<String> submitTransaction(SignedTransaction<T> transaction);

    /**
     * Check if wallet public key hash is set
     * 
     * @return - True if pubkey hash is set otherwise false
     */
    CompletableFuture<Boolean> isSigningKeySet();

    /**
     * Get current account state
     * 
     * @return - State object
     */
    CompletableFuture<AccountState> getState();

    /**
     * Get low level ZkSync API provider
     * 
     * @return - Provider
     */
    AsyncProvider getProvider();

    /**
     * Get wallet public key hash
     * 
     * @return - Pubkey hash in ZkSync format
     */
    CompletableFuture<String> getPubKeyHash();

    /**
     * Get id of the account within current ZkSync network
     * 
     * @return - Account Id
     */
    CompletableFuture<Integer> getAccountId();

    /**
     * Get latest commited nonce value of the account
     * 
     * @return - Nonce
     */
    CompletableFuture<Integer> getNonce();

    /**
     * Get list of the supported tokens by current ZkSync network
     * 
     * @return - Token list object
     */
    CompletableFuture<Tokens> getTokens();

    /**
     * Get current wallet address
     * 
     * @return - Wallet address in hex string
     */
    String getAddress();

    /**
     * Send request to enable 2-Factor authentication
     * 
     * @return true if successful, false otherwise
     */
    CompletableFuture<Boolean> enable2FA();

    /**
     * Send request to disable 2-Factor authentication
     * 
     * @param pubKeyHash - ZkSync public key hash of the account
     * @return true if successful, false otherwise
     */
    CompletableFuture<Boolean> disable2FA(@Nullable String pubKeyHash);

    EthereumProvider createEthereumProvider(Web3j web3j, ContractGasProvider contractGasProvider);
}
