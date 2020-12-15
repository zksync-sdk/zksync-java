package io.zksync.provider;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

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

public interface Provider {

    AccountState getState(String accountAddress);

    TransactionFeeDetails getTransactionFee(TransactionFeeRequest feeRequest);

    TransactionFeeDetails getTransactionFee(TransactionFeeBatchRequest feeRequest);

    Tokens getTokens();

    BigDecimal getTokenPrice(Token token);

    String submitTx(ZkSyncTransaction tx, EthSignature ethereumSignature, boolean fastProcessing);

    List<String> submitTxBatch(List<Pair<ZkSyncTransaction, EthSignature>> txs, EthSignature ethereumSignature);

    TransactionDetails getTransactionDetails(String txHash);

    ContractAddress contractAddress();

    EthOpInfo getEthOpInfo(Integer priority);

    BigInteger getConfirmationsForEthOpAmount();
}
