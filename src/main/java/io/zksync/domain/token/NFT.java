package io.zksync.domain.token;

import java.math.BigDecimal;
import java.math.BigInteger;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NFT implements TokenId {
    
    private Integer id;

    private String symbol;

    private String creatorId;

    private String contentHash;

    @Override
    public BigDecimal intoDecimal(BigInteger amount) {
        return new BigDecimal(amount);
    }
}
