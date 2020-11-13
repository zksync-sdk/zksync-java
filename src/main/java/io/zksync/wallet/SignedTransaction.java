package io.zksync.wallet;

import io.zksync.domain.transaction.ZkSyncTransaction;
import io.zksync.signer.EthSignature;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class SignedTransaction<T extends ZkSyncTransaction> {

    T transaction;

    EthSignature ethereumSignature;
}
