package io.zksync.signer;

import java.math.BigInteger;
import java.security.SignatureException;
import java.util.concurrent.CompletableFuture;

import org.web3j.tx.TransactionManager;

import io.zksync.domain.auth.ChangePubKeyVariant;
import io.zksync.domain.token.Token;

public interface EthSigner {
    
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

    /**
     * Sign ZkSync `ChangePubKey` operation message
     * 
     * @param pubKeyHash - ZkSync public key hash
     * @param nonce - Nonce of the account
     * @param accountId - Id of the account in ZkSync
     * @return Signature object
     */
    CompletableFuture<EthSignature> signChangePubKey(String pubKeyHash, Integer nonce, Integer accountId, ChangePubKeyVariant changePubKeyVariant);

    /**
     * Sign ZkSync `Transfer` operation message
     * 
     * @param to - Address of receiver
     * @param accountId - Id of the sender account in ZkSync
     * @param nonce - Nonce of the sender account
     * @param amount - Amount tokens to transfer
     * @param token - Token object supported by ZkSync
     * @param fee - Cost of transaction in ZkSync network
     * @return Signature object
     */
    CompletableFuture<EthSignature> signTransfer(String to, Integer accountId, Integer nonce, BigInteger amount, Token token, BigInteger fee);

    /**
     * Sign ZkSync `Withdraw` operation message
     * 
     * @param to - Address of receiver
     * @param accountId - Id of the account in ZkSync
     * @param nonce - Nonce of the account
     * @param amount - Amount tokens to withdraw
     * @param token - Token object supported by ZkSync
     * @param fee - Cost of transaction in ZkSync network
     * @return Signature object
     */
    CompletableFuture<EthSignature> signWithdraw(String to, Integer accountId, Integer nonce, BigInteger amount, Token token, BigInteger fee);

    /**
     * Sign ZkSync `ForcedExit` operation message
     * 
     * @param to - Address of receiver
     * @param nonce - Nonce of the account
     * @param token - Token object supported by ZkSync
     * @param fee - Cost of transaction in ZkSync network
     * @return Signature object
     */
    CompletableFuture<EthSignature> signForcedExit(String to, Integer nonce, Token token, BigInteger fee);

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
