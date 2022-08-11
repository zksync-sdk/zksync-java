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
    /// Optimism Goerli Testnet
    Goerli(420),
    /// Ethereum Sepolia Testnet
    Sepolia(11155111),

    /// Self-hosted Ethereum & zkSync networks.
    Localhost(9);

    private final long id;

    ChainId(long id) {
        this.id = id;
    }
}
