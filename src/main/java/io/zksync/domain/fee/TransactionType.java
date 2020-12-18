package io.zksync.domain.fee;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public enum TransactionType {

    WITHDRAW("Withdraw"),

    TRANSFER("Transfer"),

    FAST_WITHDRAW("FastWithdraw"),

    CHANGE_PUB_KEY("ChangePubKey"),

    CHANGE_PUB_KEY_ONCHAIN_AUTH("ChangePubKey"),

    FORCED_EXIT("Withdraw");

    private String feeIdentifier;

    TransactionType(String feeIdentifier) {
        this.feeIdentifier = feeIdentifier;
    }

    public String getFeeIdentifier() {
        return feeIdentifier;
    }

    public Object getRaw() {
        switch (this) {
            case CHANGE_PUB_KEY:
                return buildChangePubKey(false);
            case CHANGE_PUB_KEY_ONCHAIN_AUTH:
                return buildChangePubKey(true);
            default:
                return this.getFeeIdentifier();
        }
    }

    private Object buildChangePubKey(boolean onchainPubkeyAuth) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();
        ObjectNode child = mapper.createObjectNode();
        child.put("onchainPubkeyAuth", onchainPubkeyAuth);
        root.set(this.getFeeIdentifier(), child);
        return root;
    }

}
