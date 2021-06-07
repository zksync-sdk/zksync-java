package io.zksync.domain.transaction;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import org.web3j.tuples.generated.Tuple2;

import io.zksync.domain.Signature;
import io.zksync.domain.swap.Order;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Swap implements ZkSyncTransaction {
    
    private final String type = "Swap";

    private Integer submitterId;

    private String submitterAddress;

    private Integer nonce;

    @JsonIgnore
    private Tuple2<Order, Order> orders;

    @JsonIgnore
    private Tuple2<BigInteger, BigInteger> amounts;

    private String fee;

    private Integer feeToken;

    private Signature signature;

    @JsonGetter("orders")
    public List<Order> getOrdersJson() {
        return Arrays.asList(orders.component1(), orders.component2());
    }

    @JsonGetter("amounts")
    public List<BigInteger> getAmountsJson() {
        return Arrays.asList(amounts.component1(), amounts.component2());
    }

    @JsonIgnore
    public BigInteger getFeeInteger() {
        return new BigInteger(fee);
    }

}
