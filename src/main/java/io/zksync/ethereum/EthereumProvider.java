package io.zksync.ethereum;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Transfer;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.StaticGasProvider;
import org.web3j.utils.Convert.Unit;

import io.zksync.domain.token.Token;
import io.zksync.ethereum.wrappers.ERC20;
import io.zksync.ethereum.wrappers.ZkSync;
import io.zksync.signer.EthSigner;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class EthereumProvider {

    private static final BigInteger DEFAULT_THRESHOLD = BigInteger.valueOf(2).pow(255);
    private static final ContractGasProvider DEFAULT_GAS_PROVIDER = new StaticGasProvider(BigInteger.ZERO,
            BigInteger.ZERO);

    private final Web3j web3j;
    private final EthSigner ethSigner;
    private final ZkSync contract;

    public CompletableFuture<TransactionReceipt> approveDeposits(Token token, Optional<BigInteger> limit) {
        ERC20 tokenContract = ERC20.load(token.getAddress(), this.web3j, this.ethSigner.getCredentials(), this.contract.getGasProvider());
        return tokenContract.approve(this.contractAddress(), limit.orElse(DEFAULT_THRESHOLD)).sendAsync();
    }

    public CompletableFuture<TransactionReceipt> transfer(Token token, BigInteger amount, String to) {
        if (token.isETH()) {
            Transfer transfer = new Transfer(web3j, contract.getTransactionManager());
            return transfer.sendFunds(to, new BigDecimal(amount), Unit.WEI).sendAsync();
        } else {
            ERC20 tokenContract = ERC20.load(token.getAddress(), this.web3j, this.ethSigner.getCredentials(), this.contract.getGasProvider());
            return tokenContract.transfer(to, amount).sendAsync();
        }
    }

    public CompletableFuture<TransactionReceipt> deposit(Token token, BigInteger amount, String userAddress) {
        if (token.isETH()) {
            return contract.depositETH(userAddress, amount).sendAsync();
        } else {
            return contract.depositERC20(token.getAddress(), amount, userAddress).sendAsync();
        }
    }

    public CompletableFuture<TransactionReceipt> fullExit(Token token, Integer accountId) {
        return contract.fullExit(BigInteger.valueOf(accountId), token.getAddress()).sendAsync();
    }

    public CompletableFuture<Boolean> isDepositApproved(Token token, Optional<BigInteger> threshold) {
        ERC20 tokenContract = ERC20.load(token.getAddress(), this.web3j, this.ethSigner.getCredentials(), DEFAULT_GAS_PROVIDER);
        return tokenContract.allowance(this.ethSigner.getAddress(), this.contractAddress())
            .sendAsync()
            .thenApply(allowance -> {
                return allowance.compareTo(threshold.orElse(DEFAULT_THRESHOLD)) >= 0;
            });
    }

    public CompletableFuture<BigInteger> getBalance() {
        return web3j.ethGetBalance(this.ethSigner.getAddress(), DefaultBlockParameterName.LATEST)
            .sendAsync()
            .thenApply(EthGetBalance::getBalance);
    }

    public CompletableFuture<BigInteger> getNonce() {
        return web3j.ethGetTransactionCount(this.ethSigner.getAddress(), DefaultBlockParameterName.PENDING)
            .sendAsync()
            .thenApply(EthGetTransactionCount::getTransactionCount);
    }

    public String contractAddress() {
        return this.contract.getContractAddress();
    }
    
}
