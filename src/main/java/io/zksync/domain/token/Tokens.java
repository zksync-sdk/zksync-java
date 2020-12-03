package io.zksync.domain.token;

import io.zksync.exception.ZkSyncException;
import lombok.*;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;

@Getter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class Tokens {

    Map<String, Token> tokens = new HashMap<String, Token>();

    @JsonAnySetter
    public void setUnrecognizedFields(String key, Token value) {
        this.tokens.put(key, value);
    }

    public Token getTokenBySymbol(String symbol) {
        return tokens.get(symbol);
    }

    public Token getTokenByAddress(String tokenAddress) {
        return tokens
                .values()
                .stream()
                .filter(token -> token.getAddress().equals(tokenAddress))
                .findFirst()
                .orElseThrow(() -> new ZkSyncException("No token with address " + tokenAddress));
    }

    public Token getToken(String tokenIdentifier) {

        return getTokenBySymbol(tokenIdentifier) != null ?
                getTokenBySymbol(tokenIdentifier) : getTokenByAddress(tokenIdentifier);
    }
}
