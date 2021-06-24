package io.zksync.transport.receipt;

import java.util.concurrent.CompletableFuture;

import io.zksync.domain.transaction.TransactionDetails;
import io.zksync.provider.AsyncProvider;
import io.zksync.transport.ZkTransactionStatus;
import lombok.Getter;

public abstract class ZkSyncTransactionReceiptProcessor {

    @Getter
    private final AsyncProvider provider;

    public ZkSyncTransactionReceiptProcessor(AsyncProvider provider) {
        this.provider = provider;
    }

    public abstract CompletableFuture<TransactionDetails> waitForTransaction(String hash, ZkTransactionStatus status);

    public CompletableFuture<TransactionDetails> getTransactionDetails(String txHash) {
        return this.provider.getTransactionDetails(txHash);
    }

}
