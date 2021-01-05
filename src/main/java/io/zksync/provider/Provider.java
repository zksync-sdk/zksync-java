package io.zksync.provider;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import io.zksync.domain.ChainId;
import io.zksync.domain.contract.ContractAddress;
import io.zksync.domain.fee.TransactionFeeBatchRequest;
import io.zksync.domain.fee.TransactionFeeDetails;
import io.zksync.domain.fee.TransactionFeeRequest;
import io.zksync.domain.operation.EthOpInfo;
import io.zksync.domain.state.AccountState;
import io.zksync.domain.token.Token;
import io.zksync.domain.token.Tokens;
import io.zksync.domain.transaction.TransactionDetails;
import io.zksync.domain.transaction.ZkSyncTransaction;
import io.zksync.signer.EthSignature;
import io.zksync.transport.HttpTransport;

public interface Provider {

    /**
     * Get current state of the account
     * 
     * @param accountAddress - Address of the account in hex format
     * @return AccountState that represent state of the account
     */
    AccountState getState(String accountAddress);

    /**
     * Get fee of transaction that means cost of transaction execution in ZkSync network
     * 
     * @param feeRequest - Transaction information for estimation fee
     * @return Details of fee amount for transaction execution
     */
    TransactionFeeDetails getTransactionFee(TransactionFeeRequest feeRequest);

    /**
     * Get fee of batch of transaction that means cost of transaction batch execution in ZkSync network
     * 
     * @param feeRequest - Transaction batch information for estimation fee
     * @return Details of fee amount for transaction execution
     */
    TransactionFeeDetails getTransactionFee(TransactionFeeBatchRequest feeRequest);

    /**
     * Get list of tokens supproted by ZkSync network
     * 
     * @return Token information
     */
    Tokens getTokens();

    /**
     * Get the token price in USD known to server
     * 
     * @param token - Token details object
     * @return current token price
     */
    BigDecimal getTokenPrice(Token token);

    /**
     * Submit signed transaction to ZkSync network
     * 
     * @param tx - Signed transaction object
     * @param ethereumSignature - Signature of Ethereum account as 2-FA authorization
     * @param fastProcessing - Mark the transaction should be executed as soon as possible
     * @return Hash of the sent transaction in ZkSync network
     */
    String submitTx(ZkSyncTransaction tx, EthSignature ethereumSignature, boolean fastProcessing);

    /**
     * Submit signed transaction to ZkSync network
     * 
     * @param tx - Signed transaction object
     * @param fastProcessing - Mark the transaction should be executed as soon as possible
     * @return Hash of the sent transaction in ZkSync network
     */
    String submitTx(ZkSyncTransaction tx, boolean fastProcessing);

    /**
     * Submit signed transaction batch to ZkSync network
     * 
     * @param txs- List of signed transaction objects
     * @param ethereumSignature - Signature of Ethereum account as 2-FA authorization
     * @return List of hashes of the sent transactions in ZkSync network
     */
    List<String> submitTxBatch(List<Pair<ZkSyncTransaction, EthSignature>> txs, EthSignature ethereumSignature);

    /**
     * Submit signed transaction batch to ZkSync network
     * 
     * @param txs - List of signed transaction objects
     * @return List of hashes of the sent transactions in ZkSync network
     */
    List<String> submitTxBatch(List<Pair<ZkSyncTransaction, EthSignature>> txs);

    /**
     * Get details of the transaction in ZkSync network by hash
     * 
     * @param txHash - Hash of the transaction in format: `sync-tx:[hex]`
     * @return Details of the transaction
     */
    TransactionDetails getTransactionDetails(String txHash);

    /**
     * Get address of ZkSync contract deployed in Ethereum network
     * 
     * @return Address of contract in hex
     */
    ContractAddress contractAddress();

    /**
     * Get information for Priority Operation by id
     * 
     * @param priorityOperationId - Identifier of Priority Operation
     * @return Information of Priority Operation
     */
    EthOpInfo getEthOpInfo(Integer priorityOperationId);

    /**
     * Get the amount of confirmations on Ethereum required for Priority Operation to be processed
     * 
     * @return Amount of confirmations
     */
    BigInteger getConfirmationsForEthOpAmount();

    /**
     * Get hash of the transaction in Ethereum network by hash of the transaction sent in ZkSync
     * 
     * @param zkSyncWithdrawalHash - Hash of the transaction in format: `sync-tx:[hex]`
     * @return Hash of the sent transaction in Ethereum network
     */
    String getEthTransactionForWithdrawal(String zkSyncWithdrawalHash);

    /**
     * Create default ZkSync provider by given chain id
     * 
     * @param chainId - Chain id supproted by ZkSync network
     * @return ZkSync provider object
     */
    static Provider defaultProvider(ChainId chainId) {
        HttpTransport transport = null;
        switch (chainId) {
            case Mainnet: transport = new HttpTransport("https://api.zksync.io/jsrpc"); break;
            case Rinkeby: transport = new HttpTransport("https://rinkeby-api.zksync.io/jsrpc"); break;
            case Ropsten: transport = new HttpTransport("https://ropsten-api.zksync.io/jsrpc"); break;
            case Localhost: transport = new HttpTransport("http://127.0.0.1:3030"); break;
        }
        return new DefaultProvider(transport);
    }

}
