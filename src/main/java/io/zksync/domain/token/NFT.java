package io.zksync.domain.token;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

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
        return new BigDecimal(amount)
            .setScale(1)
            .divide(BigDecimal.ONE, RoundingMode.DOWN);
    }
}
