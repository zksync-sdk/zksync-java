package io.zksync.domain.state;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.zksync.domain.token.NFT;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class State {

    private Integer nonce;

    private String pubKeyHash;

    private Map<String, String> balances;

    private Map<String, NFT> nfts;

    private Map<String, NFT> mintedNfts;
}
