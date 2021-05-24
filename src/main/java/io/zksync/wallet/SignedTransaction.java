package io.zksync.wallet;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import org.apache.commons.lang3.tuple.Pair;

import io.zksync.domain.transaction.ZkSyncTransaction;
import io.zksync.signer.EthSignature;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class SignedTransaction<T extends ZkSyncTransaction> {

    @JsonIgnore
    T transaction;

    @JsonIgnore
    EthSignature ethereumSignature;

    @JsonGetter
    public T getTx() {
        return transaction;
    }

    @JsonGetter
    public EthSignature getSignature() {
        return ethereumSignature;
    }

    public static <T extends ZkSyncTransaction> SignedTransaction<T> fromPair(Pair<T, EthSignature> tx) {
        return new SignedTransaction<T>(tx.getLeft(), tx.getRight());
    }
}
