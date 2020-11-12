package im.argent.zksync.domain.fee;

public enum TransactionType {

    WITHDRAW("Withdraw"),

    TRANSFER("Transfer"),

    FAST_WITHDRAW("FastWithdraw"),

    CHANGE_PUB_KEY("ChangePubKey"),

    FORCED_EXIT("Withdraw");

    private String feeIdentifier;

    TransactionType(String feeIdentifier) {
        this.feeIdentifier = feeIdentifier;
    }

    public String getFeeIdentifier() {
        return feeIdentifier;
    }

}
