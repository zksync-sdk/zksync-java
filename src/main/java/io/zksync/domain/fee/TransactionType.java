package io.zksync.domain.fee;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

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

    public Object getRaw() {
        if (this == TransactionType.CHANGE_PUB_KEY) {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode root = mapper.createObjectNode();
            ObjectNode child = mapper.createObjectNode();
            child.put("onchainPubkeyAuth", false);
            root.set(this.getFeeIdentifier(), child);
            return root;
        } else {
            return this.getFeeIdentifier();
        }
    }

}
