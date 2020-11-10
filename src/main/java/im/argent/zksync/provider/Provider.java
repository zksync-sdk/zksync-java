package im.argent.zksync.provider;

import im.argent.zksync.domain.fee.TransactionFeeDetails;
import im.argent.zksync.domain.fee.TransactionFeeRequest;
import im.argent.zksync.domain.state.AccountState;
import im.argent.zksync.domain.token.Tokens;
import im.argent.zksync.domain.transaction.ZkSyncTransaction;
import im.argent.zksync.signer.EthSignature;

public interface Provider {

    AccountState getState(String accountAddress);

    TransactionFeeDetails getTransactionFee(TransactionFeeRequest feeRequest);

    Tokens getTokens();

    String submitTx(ZkSyncTransaction tx, EthSignature ethereumSignature, boolean fastProcessing);
}
