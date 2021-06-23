package io.zksync.ethereum;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Transfer;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.StaticGasProvider;
import org.web3j.utils.Numeric;
import org.web3j.utils.Convert.Unit;

import io.zksync.domain.token.Token;
import io.zksync.ethereum.wrappers.ERC20;
import io.zksync.ethereum.wrappers.ZkSync;
import io.zksync.signer.EthSigner;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DefaultEthereumProvider implements EthereumProvider {

    private static final BigInteger MAX_APPROVE_AMOUNT = BigInteger.valueOf(2).pow(256).subtract(BigInteger.ONE);
    private static final BigInteger DEFAULT_THRESHOLD = BigInteger.valueOf(2).pow(255);
    private static final ContractGasProvider DEFAULT_GAS_PROVIDER = new StaticGasProvider(BigInteger.ZERO,
            BigInteger.ZERO);

    private final Web3j web3j;
    private final EthSigner<?> ethSigner;
    private final ZkSync contract;

    @Override
    public CompletableFuture<TransactionReceipt> approveDeposits(Token token, Optional<BigInteger> limit) {
        ERC20 tokenContract = ERC20.load(token.getAddress(), this.web3j, this.ethSigner.getTransactionManager(),
                this.contract.getGasProvider());
        return tokenContract.approve(this.contractAddress(), limit.orElse(MAX_APPROVE_AMOUNT)).sendAsync();
    }

    @Override
    public CompletableFuture<TransactionReceipt> transfer(Token token, BigInteger amount, String to) {
        if (token.isETH()) {
            Transfer transfer = new Transfer(web3j, contract.getTransactionManager());
            return transfer.sendFunds(to, new BigDecimal(amount), Unit.WEI).sendAsync();
        } else {
            ERC20 tokenContract = ERC20.load(token.getAddress(), this.web3j, this.ethSigner.getTransactionManager(),
                    this.contract.getGasProvider());
            return tokenContract.transfer(to, amount).sendAsync();
        }
    }

    @Override
    public CompletableFuture<TransactionReceipt> deposit(Token token, BigInteger amount, String userAddress) {
        if (token.isETH()) {
            return contract.depositETH(userAddress, amount).sendAsync();
        } else {
            return contract.depositERC20(token.getAddress(), amount, userAddress).sendAsync();
        }
    }

    @Override
    public CompletableFuture<TransactionReceipt> withdraw(Token token, BigInteger amount) {
        if (token.isETH()) {
            return contract.withdrawETH(amount).sendAsync();
        } else {
            return contract.withdrawERC20(token.getAddress(), amount).sendAsync();
        }
    }

    @Override
    public CompletableFuture<TransactionReceipt> fullExit(Token token, Integer accountId) {
        return contract.requestFullExit(BigInteger.valueOf(accountId), token.getAddress()).sendAsync();
    }

    @Override
    public CompletableFuture<TransactionReceipt> setAuthPubkeyHash(String pubKeyhash, BigInteger nonce) {
        return contract.setAuthPubkeyHash(Numeric.hexStringToByteArray(pubKeyhash), nonce).sendAsync();
    }

    @Override
    public CompletableFuture<Boolean> isDepositApproved(Token token, Optional<BigInteger> threshold) {
        ERC20 tokenContract = ERC20.load(token.getAddress(), this.web3j, this.ethSigner.getTransactionManager(),
                DEFAULT_GAS_PROVIDER);
        return tokenContract.allowance(this.ethSigner.getAddress(), this.contractAddress()).sendAsync()
                .thenApply(allowance -> {
                    return allowance.compareTo(threshold.orElse(DEFAULT_THRESHOLD)) >= 0;
                });
    }

    @Override
    public CompletableFuture<Boolean> isOnChainAuthPubkeyHashSet(BigInteger nonce) {
        return contract.authFacts(ethSigner.getAddress(), nonce).sendAsync().thenApply(publicKeyHash -> {
            return !Arrays.equals(publicKeyHash, Bytes32.DEFAULT.getValue());
        });
    }

    @Override
    public CompletableFuture<BigInteger> getBalance() {
        return web3j.ethGetBalance(this.ethSigner.getAddress(), DefaultBlockParameterName.LATEST).sendAsync()
                .thenApply(EthGetBalance::getBalance);
    }

    @Override
    public CompletableFuture<BigInteger> getNonce() {
        return web3j.ethGetTransactionCount(this.ethSigner.getAddress(), DefaultBlockParameterName.PENDING).sendAsync()
                .thenApply(EthGetTransactionCount::getTransactionCount);
    }

    @Override
    public String contractAddress() {
        return this.contract.getContractAddress();
    }
    
}
