package io.zksync.domain.token;

import java.math.BigDecimal;
import java.math.BigInteger;

public interface TokenId {

    Integer getId();

    String getSymbol();

    BigDecimal intoDecimal(BigInteger amount);
    
}
