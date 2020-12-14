package io.zksync.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.lang3.tuple.Pair;

import io.zksync.domain.contract.ContractAddress;
import io.zksync.domain.fee.TransactionFeeDetails;
import io.zksync.domain.fee.TransactionFeeRequest;
import io.zksync.domain.fee.TransactionType;
import io.zksync.domain.operation.EthOpInfo;
import io.zksync.domain.state.AccountState;
import io.zksync.domain.token.Tokens;
import io.zksync.domain.transaction.TransactionDetails;
import io.zksync.domain.transaction.ZkSyncTransaction;
import io.zksync.exception.ZkSyncException;
import io.zksync.signer.EthSignature;
import io.zksync.transport.ZkSyncTransport;
import io.zksync.transport.response.ZksAccountState;
import io.zksync.transport.response.ZksContractAddress;
import io.zksync.transport.response.ZksEthOpInfo;
import io.zksync.transport.response.ZksSentTransaction;
import io.zksync.transport.response.ZksSentTransactionBatch;
import io.zksync.transport.response.ZksTokens;
import io.zksync.transport.response.ZksTransactionDetails;
import io.zksync.transport.response.ZksTransactionFeeDetails;
import lombok.AllArgsConstructor;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@AllArgsConstructor
public class DefaultProvider implements Provider {

    private ZkSyncTransport transport;

    @Override
    public AccountState getState(String accountAddress) {

        final AccountState response = transport.send("account_info", Collections.singletonList(accountAddress),
                ZksAccountState.class);

        return response;
    }

    @Override
    public TransactionFeeDetails getTransactionFee(TransactionFeeRequest feeRequest) {
        try {

            Object transactionType = feeRequest.getTransactionType().getFeeIdentifier();

            if (feeRequest.getTransactionType() == TransactionType.CHANGE_PUB_KEY) {
                transactionType = new ObjectMapper().readValue("{ \"ChangePubKey\": { \"onchainPubkeyAuth\": false }}",
                        JsonNode.class);
            }

            return transport.send("get_tx_fee",
                    Arrays.asList(transactionType, feeRequest.getAddress(), feeRequest.getTokenIdentifier()),
                   ZksTransactionFeeDetails.class);

        } catch (IOException e) {
            throw new ZkSyncException(e);
        }
    }

    @Override
    public Tokens getTokens() {
        final Tokens response = transport.send("tokens", Collections.emptyList(), ZksTokens.class);

        return response;
    }

    @Override
    public String submitTx(ZkSyncTransaction tx, EthSignature ethereumSignature, boolean fastProcessing) {
        final String responseBody = transport.send("tx_submit", Arrays.asList(tx, ethereumSignature, fastProcessing),
                ZksSentTransaction.class);

        return responseBody;
    }

    public List<String> submitTxBatch(List<Pair<ZkSyncTransaction, EthSignature>> txs, EthSignature ethereumSignature) {
        final List<String> responseBody = transport.send("submit_txs_batch", Arrays.asList(txs, ethereumSignature), ZksSentTransactionBatch.class);

        return responseBody;
    }

    @Override
    public ContractAddress contractAddress() {
        final ContractAddress contractAddress = transport.send("contract_address", Collections.emptyList(), ZksContractAddress.class);

        return contractAddress;
    }

    @Override
    public TransactionDetails getTransactionDetails(String txHash) {
        final TransactionDetails response = transport.send("tx_info", Collections.singletonList(txHash),
                ZksTransactionDetails.class);

        return response;
    }

    @Override
    public EthOpInfo getEthOpInfo(Integer priority) {
        final EthOpInfo response = transport.send("ethop_info", Collections.singletonList(priority),
                ZksEthOpInfo.class);

        return response;
    }
}