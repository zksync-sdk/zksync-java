package io.zksync.domain;

import lombok.Getter;

@Getter
public enum ChainId {
    /// Ethereum Mainnet.
    Mainnet(1),
    /// Ethereum Rinkeby testnet.
    Rinkeby(4),
    /// Ethereum Ropsten testnet.
    Ropsten(3),
    /// Self-hosted Ethereum & zkSync networks.
    Localhost(9);

    private long id;

    private ChainId(long id) {
        this.id = id;
    }
}
