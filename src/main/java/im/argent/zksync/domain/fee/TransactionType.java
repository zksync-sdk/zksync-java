package im.argent.zksync.domain.fee;

public enum TransactionType {

    WITHDRAW("Withdraw"),

    TRANSFER("Transfer"),

    FAST_WITHDRAW("FastWithdraw"),

    CHANGE_PUB_KEY("ChangePubKey");

    private String identifier;

    TransactionType(String identifier) {
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }

}
