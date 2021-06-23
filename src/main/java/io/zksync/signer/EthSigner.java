package io.zksync.signer;

import java.math.BigInteger;
import java.security.SignatureException;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import org.web3j.tx.TransactionManager;

import io.zksync.domain.auth.ChangePubKeyVariant;
import io.zksync.domain.swap.Order;
import io.zksync.domain.token.Token;
import io.zksync.domain.transaction.ChangePubKey;
import io.zksync.domain.transaction.ZkSyncTransaction;

public interface EthSigner<A extends ChangePubKeyVariant> {
    
    /**
     * Get wallet address
     * 
     * @return Address in hex string
     */
    String getAddress();

    /**
     * Get internal transaction manager. @see org.web3j.tx.TransactionManager
     * 
     * @return TransactionManager object that implements signing and sending transactions
     */
    TransactionManager getTransactionManager();

    CompletableFuture<ChangePubKey<A>> signAuth(ChangePubKey<A> changePubKey);

    /**
     * Sign `ZkSync` type operation message
     * 
     * @param <T> - ZkSyncTransaction transaction type
     * @param transaction - Prepared transaction
     * @param nonce - Nonce of the account
     * @param token - Token object supported by ZkSync
     * @param fee - Cost of transaction in ZkSync network
     * @return Signature object
     */
    <T extends ZkSyncTransaction> CompletableFuture<EthSignature> signTransaction(T transaction, Integer nonce, Token token, BigInteger fee);

    /**
     * Sign `Order` message
     * 
     * @param order - Prepared order
     * @param tokenSell - Token object supported by ZkSync
     * @param tokenBuy - Token object supported by ZkSync
     * @return Signature object
     */
    CompletableFuture<EthSignature> signOrder(Order order, Token tokenSell, Token tokenBuy);

    /**
     * Sign batch of `ZkSync` type operation messages
     * 
     * @param <T> - ZkSyncTransaction transaction type
     * @param transactions - List of the prepared transactions
     * @param nonce - Nonce of the account
     * @param token - Token object supported by ZkSync
     * @param fee - Cost of transaction in ZkSync network
     * @return Signature object
     */
    <T extends ZkSyncTransaction> CompletableFuture<EthSignature> signBatch(Collection<T> transactions, Integer nonce, Token token, BigInteger fee);

    /**
     * Sign raw message
     * 
     * @param message - Message to sign
     * @return Signature object
     */
    CompletableFuture<EthSignature> signMessage(byte[] message);

    /**
     * Sign raw message
     * 
     * @param message - Message to sign
     * @param addPrefix - If true then add secure prefix (https://eips.ethereum.org/EIPS/eip-712)
     * @return
     */
    CompletableFuture<EthSignature> signMessage(byte[] message, boolean addPrefix);

    /**
     * Verify signature with raw message
     * 
     * @param signature - Signature object
     * @param message - Message to verify
     * @return true on verification success
     * @throws SignatureException If the public key could not be recovered or if there was a
     *     signature format error.
     */
    CompletableFuture<Boolean> verifySignature(EthSignature signature, byte[] message) throws SignatureException;

    /**
     * Verify signature with raw message
     * 
     * @param signature - Signature object
     * @param message - Message to verify
     * @param prefixed - If true then add secure prefix (https://eips.ethereum.org/EIPS/eip-712)
     * @return true on verification success
     * @throws SignatureException If the public key could not be recovered or if there was a
     *     signature format error.
     */
    CompletableFuture<Boolean> verifySignature(EthSignature signature, byte[] message, boolean prefixed) throws SignatureException;

}
