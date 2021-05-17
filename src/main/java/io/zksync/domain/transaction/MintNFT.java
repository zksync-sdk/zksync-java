package io.zksync.domain.transaction;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.zksync.domain.Signature;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MintNFT implements ZkSyncTransaction {
    
    private final String type = "MintNFT";

    private Integer creatorId;

    private String creatorAddress;

    private String contentHash;

    private String recipient;

    private String fee;

    private Integer feeToken;

    private Integer nonce;

    private Signature signature;

    @JsonIgnore
    public BigInteger getFeeInteger() {
        return new BigInteger(fee);
    }
}
