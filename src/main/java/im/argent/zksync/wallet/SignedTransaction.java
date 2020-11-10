package im.argent.zksync.wallet;

import im.argent.zksync.domain.transaction.ZkSyncTransaction;
import im.argent.zksync.signer.EthSignature;
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
