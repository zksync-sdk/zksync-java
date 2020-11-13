package io.zksync.provider;

import io.zksync.domain.fee.TransactionFeeDetails;
import io.zksync.domain.fee.TransactionFeeRequest;
import io.zksync.domain.state.AccountState;
import io.zksync.domain.token.Tokens;
import io.zksync.domain.transaction.ZkSyncTransaction;
import io.zksync.signer.EthSignature;

public interface Provider {

    AccountState getState(String accountAddress);

    TransactionFeeDetails getTransactionFee(TransactionFeeRequest feeRequest);

    Tokens getTokens();

    String submitTx(ZkSyncTransaction tx, EthSignature ethereumSignature, boolean fastProcessing);
}
