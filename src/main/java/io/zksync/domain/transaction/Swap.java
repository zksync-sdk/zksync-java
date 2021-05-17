package io.zksync.domain.transaction;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.web3j.tuples.generated.Tuple2;

import io.zksync.domain.Signature;
import io.zksync.domain.swap.Order;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Swap implements ZkSyncTransaction {
    
    private final String type = "Swap";

    private Integer submitterId;

    private String submitterAddress;

    private Integer nonce;

    private Tuple2<Order, Order> orders;

    private Tuple2<BigInteger, BigInteger> amounts;

    private String fee;

    private Integer feeToken;

    private Signature signature;

    @JsonIgnore
    public BigInteger getFeeInteger() {
        return new BigInteger(fee);
    }

}
