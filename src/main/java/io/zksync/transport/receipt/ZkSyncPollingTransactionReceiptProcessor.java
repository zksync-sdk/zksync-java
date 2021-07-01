package io.zksync.transport.receipt;

import java.util.concurrent.CompletableFuture;

import io.zksync.domain.transaction.TransactionDetails;
import io.zksync.exception.ZkSyncException;
import io.zksync.provider.AsyncProvider;
import io.zksync.transport.ZkTransactionStatus;
import io.zksync.wallet.ZkASyncWallet;

public class ZkSyncPollingTransactionReceiptProcessor extends ZkSyncTransactionReceiptProcessor {

    protected final long sleepDuration;
    protected final int attempts;

    public ZkSyncPollingTransactionReceiptProcessor(AsyncProvider provider, long sleepDuration, int attempts) {
        super(provider);

        this.sleepDuration = sleepDuration;
        this.attempts = attempts;
    }

    public ZkSyncPollingTransactionReceiptProcessor(ZkASyncWallet wallet, long sleepDuration, int attempts) {
        this(wallet.getProvider(), sleepDuration, attempts);
    }

    public ZkSyncPollingTransactionReceiptProcessor(AsyncProvider provider) {
        this(provider, 100, Integer.MAX_VALUE);
    }

    public ZkSyncPollingTransactionReceiptProcessor(ZkASyncWallet wallet) {
        this(wallet.getProvider());
    }

    @Override
    public CompletableFuture<TransactionDetails> waitForTransaction(String hash, ZkTransactionStatus status) {
        final CompletableFuture<TransactionDetails> result = CompletableFuture.supplyAsync(() -> {
            TransactionDetails details = this.getTransactionDetails(hash).join();
            for (int i = 0; i < attempts; i++) {
                if (!details.getExecuted()) {
                    try {
                        Thread.sleep(sleepDuration);
                    } catch (InterruptedException e) {
                        throw new ZkSyncException(e);
                    }

                    details = this.getTransactionDetails(hash).join();
                } else {
                    switch (status) {
                        case SENT:
                        if (details.getBlock() != null) {
                            return details;
                        }
                        break;
                        case COMMITED:
                        if (details.getBlock() != null && details.getBlock().getCommitted()) {
                            return details;
                        }
                        break;
                        case VERIFIED:
                        if (details.getBlock() != null && details.getBlock().getVerified()) {
                            return details;
                        }
                        break;
                    }
                }
            }

            throw new ZkSyncException(
                    "Transaction was not generated after "
                            + ((sleepDuration * attempts) / 1000
                                    + " seconds for transaction: "
                                    + hash));
        });

        return result;
    }

}
