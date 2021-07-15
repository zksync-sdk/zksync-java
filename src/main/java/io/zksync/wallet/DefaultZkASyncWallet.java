package io.zksync.wallet;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.web3j.protocol.Web3j;
import org.web3j.tuples.generated.Tuple2;
import org.web3j.tx.gas.ContractGasProvider;

import io.zksync.domain.TimeRange;
import io.zksync.domain.TransactionBuildHelper;
import io.zksync.domain.auth.ChangePubKeyOnchain;
import io.zksync.domain.auth.ChangePubKeyVariant;
import io.zksync.domain.fee.TransactionFee;
import io.zksync.domain.state.AccountState;
import io.zksync.domain.swap.Order;
import io.zksync.domain.token.NFT;
import io.zksync.domain.token.TokenId;
import io.zksync.domain.token.Tokens;
import io.zksync.domain.transaction.ChangePubKey;
import io.zksync.domain.transaction.Transfer;
import io.zksync.domain.transaction.ZkSyncTransaction;
import io.zksync.ethereum.DefaultEthereumProvider;
import io.zksync.ethereum.EthereumProvider;
import io.zksync.ethereum.wrappers.ZkSync;
import io.zksync.provider.AsyncProvider;
import io.zksync.signer.EthSignature;
import io.zksync.signer.EthSigner;
import io.zksync.signer.ZkSigner;

public class DefaultZkASyncWallet<A extends ChangePubKeyVariant, S extends EthSigner<A>> implements ZkASyncWallet {

    private final TransactionBuildHelper helper;

    private S ethSigner;
    private ZkSigner zkSigner;
    private AsyncProvider provider;

    private Integer accountId;

    private String pubKeyHash;

    DefaultZkASyncWallet(S ethSigner, ZkSigner zkSigner, AsyncProvider provider) {
        this.ethSigner = ethSigner;
        this.zkSigner = zkSigner;

        this.provider = provider;

        this.accountId = null;
        this.pubKeyHash = null;

        this.helper = new TransactionBuildHelper(this, this.getTokens().join());
    }

    @Override
    public CompletableFuture<String> setSigningKey(TransactionFee fee, Integer nonce, boolean onchainAuth,
            TimeRange timeRange) {
        if (onchainAuth) {
            return this.helper.<ChangePubKeyOnchain>changePubKey(this.getPubKeyHash().join(), fee, nonce, timeRange)
                .thenApply(changePubKey -> {
                    zkSigner.signChangePubKey(changePubKey);

                    return this.submitSignedTransaction(changePubKey).join();
                });
        } else {
            return this.helper.<A>changePubKey(this.getPubKeyHash().join(), fee, nonce, timeRange)
                .thenApply(changePubKey -> {
                    final ChangePubKey<A> changePubKeyAuth = ethSigner.signAuth(changePubKey).join();
                    final EthSignature ethSignature = ethSigner.signTransaction(changePubKey, nonce, this.helper.getToken(fee.getFeeToken()), fee.getFee()).join();
                    zkSigner.signChangePubKey(changePubKeyAuth);

                    return this.submitSignedTransaction(changePubKeyAuth, ethSignature).join();
                });
        }
    }

    @Override
    public CompletableFuture<String> syncTransfer(String to, BigInteger amount, TransactionFee fee, Integer nonce,
            TimeRange timeRange) {
            return this.helper.transfer(to, amount, fee, nonce, timeRange)
                .thenApply(transfer -> {
                    final EthSignature ethSignature = ethSigner.signTransaction(transfer, nonce, this.helper.getToken(fee.getFeeToken()), fee.getFee()).join();
                    zkSigner.signTransfer(transfer);

                    return this.submitSignedTransaction(transfer, ethSignature).join();
                });
    }

    @Override
    public CompletableFuture<String> syncWithdraw(String ethAddress, BigInteger amount, TransactionFee fee,
            Integer nonce, boolean fastProcessing, TimeRange timeRange) {
            return this.helper.withdraw(ethAddress, amount, fee, nonce, timeRange)
                .thenApply(withdraw -> {
                    final EthSignature ethSignature = ethSigner.signTransaction(withdraw, nonce, this.helper.getToken(fee.getFeeToken()), fee.getFee()).join();
                    zkSigner.signWithdraw(withdraw);

                    return this.submitSignedTransaction(withdraw, ethSignature, fastProcessing).join();
                });
    }

    @Override
    public CompletableFuture<String> syncForcedExit(String target, TransactionFee fee, Integer nonce,
            TimeRange timeRange) {
            return this.helper.forcedExit(target, fee, nonce, timeRange)
                .thenApply(forcedExit -> {
                    final EthSignature ethSignature = ethSigner.signTransaction(forcedExit, nonce, this.helper.getToken(fee.getFeeToken()), fee.getFee()).join();
                    zkSigner.signForcedExit(forcedExit);

                    return this.submitSignedTransaction(forcedExit, ethSignature).join();
                });
    }

    @Override
    public CompletableFuture<String> syncMintNFT(String recipient, String contentHash, TransactionFee fee,
            Integer nonce) {
            return this.helper.mintNFT(recipient, contentHash, fee, nonce)
                .thenApply(mintNft -> {
                    final EthSignature ethSignature = ethSigner.signTransaction(mintNft, nonce, this.helper.getToken(fee.getFeeToken()), fee.getFee()).join();
                    zkSigner.signMintNFT(mintNft);

                    return this.submitSignedTransaction(mintNft, ethSignature).join();
                });
    }

    @Override
    public CompletableFuture<String> syncWithdrawNFT(String to, NFT token, TransactionFee fee, Integer nonce,
            TimeRange timeRange) {
        return this.helper.withdrawNFT(to, token, fee, nonce, timeRange)
                .thenApply(withdrawNft -> {
                    final EthSignature ethSignature = ethSigner.signTransaction(withdrawNft, nonce, this.helper.getToken(fee.getFeeToken()), fee.getFee()).join();
                    zkSigner.signWithdrawNFT(withdrawNft);

                    return this.submitSignedTransaction(withdrawNft, ethSignature).join();
                });
    }

    @Override
    public CompletableFuture<List<String>> syncTransferNFT(String to, NFT token, TransactionFee fee, Integer nonce,
            TimeRange timeRange) {
        return this.helper.transferNFT(to, token, fee, nonce, timeRange)
                .thenApply(transferNft -> {
                    final Transfer nft = transferNft.component1();
                    final Transfer fees = transferNft.component2();
                    EthSignature ethSignature = ethSigner.signBatch(Arrays.asList(nft, fees), nft.getNonce(), this.helper.getToken(fee.getFeeToken()), fee.getFee()).join();
                    return submitSignedBatch(Arrays.asList(
                        zkSigner.signTransfer(nft),
                        zkSigner.signTransfer(fees)
                    ), ethSignature).join();
                });
    }

    @Override
    public CompletableFuture<String> syncSwap(Order order1, Order order2, BigInteger amount1, BigInteger amount2,
            TransactionFee fee, Integer nonce) {
        return this.helper.swap(order1, order2, amount1, amount2, fee, nonce)
                .thenApply(swap -> {
                    final EthSignature ethSignature = ethSigner.signTransaction(swap, nonce, this.helper.getToken(fee.getFeeToken()), fee.getFee()).join();
                    zkSigner.signSwap(swap);

                    return this.submitSignedTransaction(swap, ethSignature).join();
                });
    }

    @Override
    public <T extends TokenId> CompletableFuture<Order> buildSignedOrder(String recipient, T sell, T buy,
            Tuple2<BigInteger, BigInteger> ratio, BigInteger amount, Integer nonce, TimeRange timeRange) {
        return this.helper.order(recipient, sell, buy, ratio, amount, nonce, timeRange)
                .thenApply(order -> {
                    final EthSignature ethSignature = ethSigner.signOrder(order, sell, buy).join();
                    order.setEthereumSignature(ethSignature);

                    return zkSigner.signOrder(order);
                });
    }

    @Override
    public <T extends TokenId> CompletableFuture<Order> buildSignedLimitOrder(String recipient, T sell, T buy,
            Tuple2<BigInteger, BigInteger> ratio, Integer nonce, TimeRange timeRange) {
        return this.helper.limitOrder(recipient, sell, buy, ratio, nonce, timeRange)
                .thenApply(order -> {
                    final EthSignature ethSignature = ethSigner.signOrder(order, sell, buy).join();
                    order.setEthereumSignature(ethSignature);

                    return zkSigner.signOrder(order);
                });
    }

    @Override
    public CompletableFuture<Boolean> isSigningKeySet() {
        return this.getPubKeyHash()
            .thenApply(pubKey -> Objects.equals(pubKey, this.zkSigner.getPublicKeyHash()));
    }

    @Override
    public CompletableFuture<AccountState> getState() {
        return this.provider.getState(this.getAddress());
    }

    @Override
    public AsyncProvider getProvider() {
        return this.provider;
    }

    @Override
    public CompletableFuture<String> getPubKeyHash() {
        if (this.pubKeyHash == null) {
            return getState()
                .thenApply(this::setAccountInfo)
                .thenApply(state -> state.getCommitted().getPubKeyHash());
        } else {
            return CompletableFuture.completedFuture(this.pubKeyHash);
        }
    }

    @Override
    public CompletableFuture<Integer> getAccountId() {
        if (this.accountId == null) {
            return getState()
                .thenApply(this::setAccountInfo)
                .thenApply(AccountState::getId);
        } else {
            return CompletableFuture.completedFuture(this.accountId);
        }
    }

    @Override
    public CompletableFuture<Tokens> getTokens() {
        return this.provider.getTokens();
    }

    @Override
    public <T extends ZkSyncTransaction> CompletableFuture<String> submitTransaction(SignedTransaction<T> transaction) {
        return submitSignedTransaction(transaction.getTransaction(), transaction.getEthereumSignature());
    }

    @Override
    public String getAddress() {
        return this.ethSigner.getAddress();
    }

    @Override
    public CompletableFuture<Integer> getNonce() {
        return getState()
            .thenApply(state -> state.getCommitted().getNonce());
    }

    @Override
    public EthereumProvider createEthereumProvider(Web3j web3j, ContractGasProvider contractGasProvider) {
        String contractAddress = this.provider.contractAddress().join().getMainContract();
        ZkSync contract = ZkSync.load(contractAddress, web3j, this.ethSigner.getTransactionManager(), contractGasProvider);
        DefaultEthereumProvider ethereum = new DefaultEthereumProvider(web3j, this.ethSigner, contract);
        return ethereum;
    }

    private CompletableFuture<String> submitSignedTransaction(ZkSyncTransaction signedTransaction,
                                         EthSignature ethereumSignature,
                                         boolean fastProcessing) {
        return provider.submitTx(signedTransaction, ethereumSignature, fastProcessing);
    }

    private CompletableFuture<String> submitSignedTransaction(ZkSyncTransaction signedTransaction,
                                         EthSignature ...ethereumSignature) {
        if (ethereumSignature == null || ethereumSignature.length == 0) {
            return provider.submitTx(signedTransaction, null, false);
        } else if (ethereumSignature.length == 1) {
            return provider.submitTx(signedTransaction, ethereumSignature[0], false);
        } else {
            return provider.submitTx(signedTransaction, ethereumSignature);
        }
    }

    private CompletableFuture<List<String>> submitSignedBatch(List<ZkSyncTransaction> transactions, EthSignature ethereumSignature) {
        return provider.submitTxBatch(
            transactions.stream().map(tx -> Pair.of(tx, (EthSignature) null)).collect(Collectors.toList()),
            ethereumSignature
        );
    }

    private AccountState setAccountInfo(AccountState state) {
        this.accountId = state.getId();
        this.pubKeyHash = state.getCommitted().getPubKeyHash();

        return state;
    }
    
}
