package io.zksync.domain.fee;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public enum TransactionType {

    WITHDRAW("Withdraw"),

    TRANSFER("Transfer"),

    FAST_WITHDRAW("FastWithdraw"),

    CHANGE_PUB_KEY_ONCHAIN("Onchain"),
    CHANGE_PUB_KEY_ECDSA("ECDSA"),
    CHANGE_PUB_KEY_CREATE2("CREATE2"),

    LEGACY_CHANGE_PUB_KEY("ChangePubKey"),

    LEGACY_CHANGE_PUB_KEY_ONCHAIN_AUTH("ChangePubKey"),

    FORCED_EXIT("Withdraw"),

    SWAP("Swap"),

    MINT_NFT("MintNFT"),

    WITHDRAW_NFT("WithdrawNFT"),

    FAST_WITHDRAW_NFT("FastWithdrawNFT");

    private String feeIdentifier;

    TransactionType(String feeIdentifier) {
        this.feeIdentifier = feeIdentifier;
    }

    public String getFeeIdentifier() {
        return feeIdentifier;
    }

    public Object getRaw() {
        switch (this) {
            case LEGACY_CHANGE_PUB_KEY:
                return buildChangePubKeyLegacy(false);
            case LEGACY_CHANGE_PUB_KEY_ONCHAIN_AUTH:
                return buildChangePubKeyLegacy(true);
            case CHANGE_PUB_KEY_ECDSA:
            case CHANGE_PUB_KEY_CREATE2:
            case CHANGE_PUB_KEY_ONCHAIN:
                return buildChangePubKey(this.getFeeIdentifier());
            default:
                return this.getFeeIdentifier();
        }
    }

    private Object buildChangePubKeyLegacy(boolean onchainPubkeyAuth) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();
        ObjectNode child = mapper.createObjectNode();
        child.put("onchainPubkeyAuth", onchainPubkeyAuth);
        root.set(this.getFeeIdentifier(), child);
        return root;
    }

    private Object buildChangePubKey(String authType) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();
        root.put("ChangePubKey", authType);
        return root;
    }

}
