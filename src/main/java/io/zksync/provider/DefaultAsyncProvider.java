package io.zksync.provider;

import org.apache.commons.lang3.tuple.Pair;

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
import io.zksync.transport.ZkSyncTransport;
import io.zksync.transport.response.ZksAccountState;
import io.zksync.transport.response.ZksContractAddress;
import io.zksync.transport.response.ZksEthOpInfo;
import io.zksync.transport.response.ZksGetConfirmationsForEthOpAmount;
import io.zksync.transport.response.ZksSentTransaction;
import io.zksync.transport.response.ZksSentTransactionBatch;
import io.zksync.transport.response.ZksTokenPrice;
import io.zksync.transport.response.ZksTokens;
import io.zksync.transport.response.ZksTransactionDetails;
import io.zksync.transport.response.ZksTransactionFeeDetails;
import io.zksync.wallet.SignedTransaction;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@AllArgsConstructor
public class DefaultAsyncProvider implements AsyncProvider {
    private ZkSyncTransport transport;

    @Override
    public CompletableFuture<AccountState> getState(String accountAddress) {

        final CompletableFuture<AccountState> response = transport.sendAsync("account_info", Collections.singletonList(accountAddress),
                ZksAccountState.class);

        return response;
    }

    @Override
    public CompletableFuture<TransactionFeeDetails> getTransactionFee(TransactionFeeRequest feeRequest) {
        final CompletableFuture<TransactionFeeDetails> response = transport.sendAsync("get_tx_fee",
                Arrays.asList(feeRequest.getTransactionType().getRaw(), feeRequest.getAddress(),
                        feeRequest.getTokenIdentifier()),
                ZksTransactionFeeDetails.class);

        return response;
    }

    @Override
    public CompletableFuture<TransactionFeeDetails> getTransactionFee(TransactionFeeBatchRequest feeRequest) {
        final CompletableFuture<TransactionFeeDetails> response = transport.sendAsync("get_txs_batch_fee_in_wei",
                Arrays.asList(feeRequest.getTransactionTypesRaw(), feeRequest.getAddresses(),
                        feeRequest.getTokenIdentifier()),
                ZksTransactionFeeDetails.class);

        return response;
    }

    @Override
    public CompletableFuture<Tokens> getTokens() {
        final CompletableFuture<Tokens> response = transport.sendAsync("tokens", Collections.emptyList(), ZksTokens.class);

        return response;
    }

    @Override
    public CompletableFuture<BigDecimal> getTokenPrice(Token token) {
        final CompletableFuture<BigDecimal> response = transport.sendAsync("get_token_price", Collections.singletonList(token.getSymbol()),
                ZksTokenPrice.class);

        return response;
    }

    @Override
    public CompletableFuture<String> submitTx(ZkSyncTransaction tx, EthSignature ethereumSignature, boolean fastProcessing) {
        final CompletableFuture<String> responseBody = transport.sendAsync("tx_submit", Arrays.asList(tx, ethereumSignature, fastProcessing),
                ZksSentTransaction.class);

        return responseBody;
    }

    @Override
    public CompletableFuture<String> submitTx(ZkSyncTransaction tx, boolean fastProcessing) {
        return submitTx(tx, null, fastProcessing);
    }

    @Override
    public CompletableFuture<String> submitTx(ZkSyncTransaction tx, EthSignature... ethereumSignature) {
        final CompletableFuture<String> responseBody = transport.sendAsync("tx_submit", Arrays.asList(tx, ethereumSignature),
                ZksSentTransaction.class);

        return responseBody;
    }

    @Override
    public CompletableFuture<List<String>> submitTxBatch(List<Pair<ZkSyncTransaction, EthSignature>> txs, EthSignature ethereumSignature) {
        final CompletableFuture<List<String>> responseBody = transport.sendAsync("submit_txs_batch", Arrays.asList(txs.stream().map(SignedTransaction::fromPair).collect(Collectors.toList()), ethereumSignature),
                ZksSentTransactionBatch.class);

        return responseBody;
    }

    @Override
    public CompletableFuture<List<String>> submitTxBatch(List<Pair<ZkSyncTransaction, EthSignature>> txs) {
        return submitTxBatch(txs, null);
    }

    @Override
    public CompletableFuture<ContractAddress> contractAddress() {
        final CompletableFuture<ContractAddress> contractAddress = transport.sendAsync("contract_address", Collections.emptyList(),
                ZksContractAddress.class);

        return contractAddress;
    }

    @Override
    public CompletableFuture<TransactionDetails> getTransactionDetails(String txHash) {
        final CompletableFuture<TransactionDetails> response = transport.sendAsync("tx_info", Collections.singletonList(txHash),
                ZksTransactionDetails.class);

        return response;
    }

    @Override
    public CompletableFuture<EthOpInfo> getEthOpInfo(Integer priority) {
        final CompletableFuture<EthOpInfo> response = transport.sendAsync("ethop_info", Collections.singletonList(priority),
                ZksEthOpInfo.class);

        return response;
    }

    @Override
    public CompletableFuture<BigInteger> getConfirmationsForEthOpAmount() {
        final CompletableFuture<BigInteger> response = transport.sendAsync("get_confirmations_for_eth_op_amount", Collections.emptyList(),
                ZksGetConfirmationsForEthOpAmount.class);

        return response;
    }

    @Override
    public CompletableFuture<String> getEthTransactionForWithdrawal(String zkSyncWithdrawalHash) {
        final CompletableFuture<String> response = transport.sendAsync("get_eth_tx_for_withdrawal", Collections.singletonList(zkSyncWithdrawalHash),
                ZksSentTransaction.class);

        return response;
    }
}
