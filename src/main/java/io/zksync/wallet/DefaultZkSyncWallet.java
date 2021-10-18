package io.zksync.wallet;

import io.reactivex.annotations.Nullable;
import io.zksync.domain.TimeRange;
import io.zksync.domain.auth.ChangePubKeyOnchain;
import io.zksync.domain.auth.ChangePubKeyVariant;
import io.zksync.domain.auth.Toggle2FA;
import io.zksync.domain.fee.TransactionFee;
import io.zksync.domain.state.AccountState;
import io.zksync.domain.swap.Order;
import io.zksync.domain.token.NFT;
import io.zksync.domain.token.Token;
import io.zksync.domain.token.TokenId;
import io.zksync.domain.token.Tokens;
import io.zksync.domain.transaction.*;
import io.zksync.ethereum.DefaultEthereumProvider;
import io.zksync.ethereum.EthereumProvider;
import io.zksync.ethereum.wrappers.ZkSync;
import io.zksync.exception.ZkSyncException;
import io.zksync.provider.DefaultProvider;
import io.zksync.provider.Provider;
import io.zksync.signer.EthSignature;
import io.zksync.signer.EthSigner;
import io.zksync.signer.ZkSigner;
import io.zksync.transport.ZkSyncTransport;
import lombok.Getter;
import lombok.SneakyThrows;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.web3j.protocol.Web3j;
import org.web3j.tuples.generated.Tuple2;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.utils.Strings;

public class DefaultZkSyncWallet<A extends ChangePubKeyVariant, S extends EthSigner<A>> implements ZkSyncWallet {

    private S ethSigner;
    private ZkSigner zkSigner;

    @Getter
    private Provider provider;

    private Integer accountId;

    private String pubKeyHash;

    DefaultZkSyncWallet(S ethSigner, ZkSigner zkSigner, Provider provider) {
        this.ethSigner = ethSigner;
        this.zkSigner = zkSigner;

        this.provider = provider;

        this.accountId = null;
        this.pubKeyHash = null;
    }

    public static <A extends ChangePubKeyVariant, S extends EthSigner<A>> DefaultZkSyncWallet<A, S> build(S ethSigner, ZkSigner zkSigner, ZkSyncTransport transport) {
        return new DefaultZkSyncWallet<>(ethSigner, zkSigner, new DefaultProvider(transport));
    }

    public static <A extends ChangePubKeyVariant, S extends EthSigner<A>> DefaultZkSyncWallet<A, S> build(S ethSigner, ZkSigner zkSigner, Provider provider) {
        return new DefaultZkSyncWallet<>(ethSigner, zkSigner, provider);
    }

    @Override
    public String setSigningKey(TransactionFee fee, Integer nonce, boolean onchainAuth, TimeRange timeRange) {

        if (isSigningKeySet()) {
            throw new ZkSyncException("Current signing key is already set");
        }

        final Integer nonceToUse = nonce == null ? getNonce() : nonce;

        if (onchainAuth) {
            final SignedTransaction<ChangePubKey<ChangePubKeyOnchain>> signedTx = buildSignedChangePubKeyTxOnchain(fee, nonceToUse, timeRange);
            return submitSignedTransaction(signedTx.getTransaction(), null, false);
        } else {
            final SignedTransaction<ChangePubKey<A>> signedTx = buildSignedChangePubKeyTx(fee, nonceToUse, timeRange);
            return submitSignedTransaction(signedTx.getTransaction(), null, false);
        }
    }

    @Override
    public String syncTransfer(String to, BigInteger amount, TransactionFee fee, Integer nonce, TimeRange timeRange) {

        final Integer nonceToUse = nonce == null ? getNonce() : nonce;

        final SignedTransaction<Transfer> signedTransfer = buildSignedTransferTx(to, fee.getFeeToken(), amount,
                fee.getFee(), nonceToUse, timeRange);

        return submitSignedTransaction(signedTransfer.getTransaction(), signedTransfer.getEthereumSignature());
    }

    @Override
    public String syncWithdraw(String ethAddress, BigInteger amount, TransactionFee fee, Integer nonce,
            boolean fastProcessing, TimeRange timeRange) {
        final Integer nonceToUse = nonce == null ? getNonce() : nonce;

        final SignedTransaction<Withdraw> signedWithdraw = buildSignedWithdrawTx(ethAddress, fee.getFeeToken(), amount,
                fee.getFee(), nonceToUse, timeRange);

        return submitSignedTransaction(signedWithdraw.getTransaction(), signedWithdraw.getEthereumSignature()[0],
                fastProcessing);
    }

    @Override
    public String syncForcedExit(String target, TransactionFee fee, Integer nonce, TimeRange timeRange) {
        final Integer nonceToUse = nonce == null ? getNonce() : nonce;

        final SignedTransaction<ForcedExit> signedForcedExit = buildSignedForcedExitTx(target, fee.getFeeToken(),
                fee.getFee(), nonceToUse, timeRange);

        return submitSignedTransaction(signedForcedExit.getTransaction(), signedForcedExit.getEthereumSignature());
    }

    @Override
    public String syncMintNFT(String recipient, String contentHash, TransactionFee fee, Integer nonce) {
        final Integer nonceToUse = nonce == null ? getNonce() : nonce;

        final SignedTransaction<MintNFT> signedMintNFT = buildSignedMintNFTTx(recipient, contentHash, fee.getFeeToken(),
                fee.getFee(), nonceToUse);

        return submitSignedTransaction(signedMintNFT.getTransaction(), signedMintNFT.getEthereumSignature());
    }

    @Override
    public String syncWithdrawNFT(String to, NFT token, TransactionFee fee, Integer nonce, TimeRange timeRange) {
        final Integer nonceToUse = nonce == null ? getNonce() : nonce;

        final SignedTransaction<WithdrawNFT> signedWithdrawNFT = buildSignedWithdrawNFTTx(to, token, fee.getFeeToken(), fee.getFee(), nonceToUse, timeRange);

        return submitSignedTransaction(signedWithdrawNFT.getTransaction(), signedWithdrawNFT.getEthereumSignature());
    }

    @SneakyThrows
    @Override
    public List<String> syncTransferNFT(String to, NFT token, TransactionFee fee, Integer nonce, TimeRange timeRange) {
        final Integer nonceToUse = nonce == null ? getNonce() : nonce;
        final Tokens tokens = provider.getTokens();

        final Token feeToken = tokens.getTokenBySymbol(fee.getFeeToken()) != null ?
                tokens.getTokenBySymbol(fee.getFeeToken()) : tokens.getTokenByAddress(fee.getFeeToken());

        final Transfer transferNft = Transfer
                .builder()
                .accountId(this.getAccountId())
                .from(ethSigner.getAddress())
                .to(to)
                .token(token.getId())
                .tokenId(token)
                .amount(BigInteger.ONE)
                .nonce(nonceToUse)
                .fee(BigInteger.ZERO.toString())
                .timeRange(timeRange)
                .build();
        final Transfer transferFee = Transfer
                .builder()
                .accountId(this.getAccountId())
                .from(ethSigner.getAddress())
                .to(ethSigner.getAddress())
                .token(feeToken.getId())
                .tokenId(feeToken)
                .amount(BigInteger.ZERO)
                .nonce(nonceToUse + 1)
                .fee(fee.getFee().toString())
                .timeRange(timeRange)
                .build();
        EthSignature ethSignature = ethSigner.signBatch(Arrays.asList(transferNft, transferFee), nonceToUse, feeToken, fee.getFee()).get();
        return submitSignedBatch(Arrays.asList(
            zkSigner.signTransfer(transferNft),
            zkSigner.signTransfer(transferFee)
        ), ethSignature);
    }

    @Override
    public String syncSwap(Order order1, Order order2, BigInteger amount1, BigInteger amount2, TransactionFee fee, Integer nonce) {
        final Integer nonceToUse = nonce == null ? getNonce() : nonce;

        final SignedTransaction<Swap> signedSwap = buildSignedSwapTx(order1, order2, amount1, amount2, fee.getFeeToken(), fee.getFee(), nonceToUse);

        return submitSignedTransaction(signedSwap.getTransaction(), signedSwap.getEthereumSignature()[0], order1.getEthereumSignature(), order2.getEthereumSignature());
    }

    @Override
    @SneakyThrows
    public <T extends TokenId> Order buildSignedOrder(String recipient, T sell, T buy, Tuple2<BigInteger, BigInteger> ratio, BigInteger amount, Integer nonce, TimeRange timeRange) {
        final Integer nonceToUse = nonce == null ? getNonce() : nonce;

        Order order = Order.builder()
            .accountId(this.getAccountId())
            .amount(amount)
            .recipientAddress(recipient)
            .tokenSell(sell.getId())
            .tokenBuy(buy.getId())
            .ratio(ratio)
            .nonce(nonceToUse)
            .timeRange(timeRange)
            .build();
        final EthSignature ethSignature = ethSigner.signOrder(order, sell, buy).get();
        order.setEthereumSignature(ethSignature);

        return zkSigner.signOrder(order);
    }

    @Override
    @SneakyThrows
    public <T extends TokenId> Order buildSignedLimitOrder(String recipient, T sell, T buy, Tuple2<BigInteger, BigInteger> ratio,
            Integer nonce, TimeRange timeRange) {
        return this.buildSignedOrder(recipient, sell, buy, ratio, BigInteger.ZERO, nonce, timeRange);
    }

    @Override
    public AccountState getState() {
        return provider.getState(ethSigner.getAddress());
    }

    @Override
    public boolean isSigningKeySet() {
        return Objects.equals(this.getPubKeyHash(), this.zkSigner.getPublicKeyHash());
    }

    @Override
    public Integer getAccountId() {
        if (this.accountId == null) {
            this.loadAccountInfo();
        }

        return this.accountId;
    }

    @Override
    public String getPubKeyHash() {
        if (this.pubKeyHash == null) {
            this.loadAccountInfo();
        }

        return this.pubKeyHash;
    }

    @Override
    public EthereumProvider createEthereumProvider(Web3j web3j, ContractGasProvider contractGasProvider) {
        String contractAddress = this.provider.contractAddress().getMainContract();
        ZkSync contract = ZkSync.load(contractAddress, web3j, this.ethSigner.getTransactionManager(), contractGasProvider);
        DefaultEthereumProvider ethereum = new DefaultEthereumProvider(web3j, this.ethSigner, contract);
        return ethereum;
    }

    @Override
    public String getAddress() {
        return this.ethSigner.getAddress();
    }

    @Override
    public Tokens getTokens() {
        return this.provider.getTokens();
    }

    @Override
    public boolean enable2FA() {
        final Long timestamp = System.currentTimeMillis();
        final Integer accountId = this.getAccountId();

        final EthSignature ethSignature = ethSigner.signToggle(true, timestamp).join();

        final Toggle2FA toggle2Fa = new Toggle2FA(
            true,
            accountId,
            timestamp,
            ethSignature,
            null
        );

        return provider.toggle2FA(toggle2Fa);
    }

    @Override
    public boolean disable2FA(@Nullable String pubKeyHash) {
        final Long timestamp = System.currentTimeMillis();
        final Integer accountId = this.getAccountId();

        final EthSignature ethSignature = (
            Strings.isEmpty(pubKeyHash) ?
                ethSigner.signToggle(false, timestamp) :
                ethSigner.signToggle(false, timestamp, pubKeyHash)
        ).join();

        final Toggle2FA toggle2Fa = new Toggle2FA(
            false,
            accountId,
            timestamp,
            ethSignature,
            pubKeyHash
        );

        return provider.toggle2FA(toggle2Fa);
    }

    @SneakyThrows
    private SignedTransaction<ChangePubKey<A>> buildSignedChangePubKeyTx(TransactionFee fee, Integer nonce,
        TimeRange timeRange) {
        if (zkSigner == null) {
            throw new Error("ZKSync signer is required for current pubkey calculation.");
        }

        final Token token = provider.getTokens().getToken(fee.getFeeToken());

        final ChangePubKey<A> changePubKey = ChangePubKey
            .<A>builder()
            .accountId(this.getAccountId())
            .account(ethSigner.getAddress())
            .newPkHash(zkSigner.getPublicKeyHash())
            .nonce(nonce).feeToken(token.getId())
            .fee(fee.getFee().toString())
            .timeRange(timeRange)
            .build();
        final ChangePubKey<A> changePubKeyAuth = ethSigner.signAuth(changePubKey).get();
        EthSignature ethSignature = ethSigner.signTransaction(changePubKey, nonce, token, fee.getFee()).get();

        return new SignedTransaction<>(zkSigner.signChangePubKey(changePubKeyAuth), ethSignature);
    }

    @SneakyThrows
    private SignedTransaction<ChangePubKey<ChangePubKeyOnchain>> buildSignedChangePubKeyTxOnchain(TransactionFee fee, Integer nonce,
        TimeRange timeRange) {
        if (zkSigner == null) {
            throw new Error("ZKSync signer is required for current pubkey calculation.");
        }

        final Token token = provider.getTokens().getToken(fee.getFeeToken());

        final ChangePubKey<ChangePubKeyOnchain> changePubKey = ChangePubKey
            .<ChangePubKeyOnchain>builder()
            .accountId(this.getAccountId())
            .account(ethSigner.getAddress())
            .newPkHash(zkSigner.getPublicKeyHash())
            .nonce(nonce).feeToken(token.getId())
            .fee(fee.getFee().toString())
            .ethAuthData(new ChangePubKeyOnchain())
            .timeRange(timeRange)
            .build();


        return new SignedTransaction<>(zkSigner.signChangePubKey(changePubKey));
    }

    @SneakyThrows
    private SignedTransaction<Transfer> buildSignedTransferTx(String to,
                                                              String tokenIdentifier,
                                                              BigInteger amount,
                                                              BigInteger fee,
                                                              Integer nonce,
                                                              TimeRange timeRange) {
        if (zkSigner == null) {
            throw new Error("ZKSync signer is required for current pubkey calculation.");
        }

        final Tokens tokens = provider.getTokens();

        final Token token = tokens.getTokenBySymbol(tokenIdentifier) != null ?
                tokens.getTokenBySymbol(tokenIdentifier) : tokens.getTokenByAddress(tokenIdentifier);

        final Transfer transfer = Transfer
                .builder()
                .accountId(this.getAccountId())
                .from(ethSigner.getAddress())
                .to(to)
                .token(token.getId())
                .amount(amount)
                .nonce(nonce)
                .fee(fee.toString())
                .timeRange(timeRange)
                .build();

        final EthSignature ethSignature = ethSigner.signTransaction(transfer, nonce, token, fee).get();

        return new SignedTransaction<>(zkSigner.signTransfer(transfer), ethSignature);
    }

    @SneakyThrows
    private SignedTransaction<Withdraw> buildSignedWithdrawTx(String to, String tokenIdentifier, BigInteger amount, BigInteger fee, Integer nonce, TimeRange timeRange) {
        if (zkSigner == null) {
            throw new Error("ZKSync signer is required for current pubkey calculation.");
        }

        final Tokens tokens = provider.getTokens();

        final Token token = tokens.getTokenBySymbol(tokenIdentifier) != null ?
                tokens.getTokenBySymbol(tokenIdentifier) : tokens.getTokenByAddress(tokenIdentifier);

        final Withdraw withdraw = Withdraw
                .builder()
                .accountId(this.getAccountId())
                .from(ethSigner.getAddress())
                .to(to)
                .token(token.getId())
                .amount(amount)
                .nonce(nonce)
                .fee(fee.toString())
                .timeRange(timeRange)
                .build();

        final EthSignature ethSignature = ethSigner.signTransaction(withdraw, nonce, token, fee).get();

        return new SignedTransaction<>(zkSigner.signWithdraw(withdraw), ethSignature);
    }

    @SneakyThrows
    private SignedTransaction<ForcedExit> buildSignedForcedExitTx(String target,
                                                                String tokenIdentifier,
                                                                BigInteger fee,
                                                                Integer nonce,
                                                                TimeRange timeRange) {
        if (zkSigner == null) {
            throw new Error("ZKSync signer is required for current pubkey calculation.");
        }

        final Tokens tokens = provider.getTokens();

        final Token token = tokens.getToken(tokenIdentifier);

        final ForcedExit forcedExit = ForcedExit
                .builder()
                .initiatorAccountId(this.getAccountId())
                .target(target)
                .token(token.getId())
                .nonce(nonce)
                .fee(fee.toString())
                .timeRange(timeRange)
                .build();
        
        final EthSignature ethSignature = ethSigner.signTransaction(forcedExit, nonce, token, fee).get();

        return new SignedTransaction<>(zkSigner.signForcedExit(forcedExit), ethSignature);
    }

    @SneakyThrows
    private SignedTransaction<MintNFT> buildSignedMintNFTTx(String to, String contentHash, String tokenIdentifier, BigInteger fee, Integer nonce) {
        if (zkSigner == null) {
            throw new Error("ZKSync signer is required for current pubkey calculation.");
        }

        final Tokens tokens = provider.getTokens();

        final Token token = tokens.getToken(tokenIdentifier);

        final MintNFT mintNft = MintNFT
            .builder()
            .creatorId(this.getAccountId())
            .creatorAddress(ethSigner.getAddress())
            .contentHash(contentHash)
            .recipient(to)
            .fee(fee.toString())
            .feeToken(token.getId())
            .nonce(nonce)
            .build();

        final EthSignature ethSignature = ethSigner.signTransaction(mintNft, nonce, token, fee).get();

        return new SignedTransaction<>(zkSigner.signMintNFT(mintNft), ethSignature);
    }

    @SneakyThrows
    private SignedTransaction<WithdrawNFT> buildSignedWithdrawNFTTx(String to, NFT token, String tokenIdentifier, BigInteger fee, Integer nonce, TimeRange timeRange) {
        if (zkSigner == null) {
            throw new Error("ZKSync signer is required for current pubkey calculation.");
        }

        final Tokens tokens = provider.getTokens();

        final Token feeToken = tokens.getToken(tokenIdentifier);

        final WithdrawNFT withdrawNFT = WithdrawNFT
            .builder()
            .accountId(this.getAccountId())
            .from(ethSigner.getAddress())
            .to(to)
            .token(token.getId())
            .nonce(nonce)
            .fee(fee.toString())
            .feeToken(feeToken.getId())
            .timeRange(timeRange)
            .build();

        final EthSignature ethSignature = ethSigner.signTransaction(withdrawNFT, nonce, feeToken, fee).get();

        return new SignedTransaction<>(zkSigner.signWithdrawNFT(withdrawNFT), ethSignature);
    }

    @SneakyThrows
    private SignedTransaction<Swap> buildSignedSwapTx(Order order1, Order order2, BigInteger amount1, BigInteger amount2, String tokenIdentifier, BigInteger fee, Integer nonce) {
        if (zkSigner == null) {
            throw new Error("ZKSync signer is required for current pubkey calculation.");
        }

        final Tokens tokens = provider.getTokens();

        final Token feeToken = tokens.getToken(tokenIdentifier);

        final Swap swap = Swap.builder()
            .orders(new Tuple2<>(order1, order2))
            .submitterAddress(this.ethSigner.getAddress())
            .submitterId(this.getAccountId())
            .amounts(new Tuple2<>(amount1, amount2))
            .nonce(nonce)
            .fee(fee.toString())
            .feeToken(feeToken.getId())
            .build();

        final EthSignature ethSignature = ethSigner.signTransaction(swap, nonce, feeToken, fee).get();

        return new SignedTransaction<>(zkSigner.signSwap(swap), ethSignature);
    }

    private String submitSignedTransaction(ZkSyncTransaction signedTransaction,
                                         EthSignature ethereumSignature,
                                         boolean fastProcessing) {
        return provider.submitTx(signedTransaction, ethereumSignature, fastProcessing);
    }

    private String submitSignedTransaction(ZkSyncTransaction signedTransaction,
                                         EthSignature ...ethereumSignature) {
        if (ethereumSignature == null || ethereumSignature.length == 0) {
            return provider.submitTx(signedTransaction, null, false);
        } else if (ethereumSignature.length == 1) {
            return provider.submitTx(signedTransaction, ethereumSignature[0], false);
        } else {
            return provider.submitTx(signedTransaction, ethereumSignature);
        }
    }

    private List<String> submitSignedBatch(List<ZkSyncTransaction> transactions, EthSignature ethereumSignature) {
        return provider.submitTxBatch(
            transactions.stream().map(tx -> Pair.of(tx, (EthSignature) null)).collect(Collectors.toList()),
            ethereumSignature
        );
    }

    private Integer getNonce() {
        return getState().getCommitted().getNonce();
    }

    private void loadAccountInfo() {
        final AccountState state = getState();

        this.accountId = state.getId();
        this.pubKeyHash = state.getCommitted().getPubKeyHash();
    }

    @Override
    public <T extends ZkSyncTransaction> String submitTransaction(SignedTransaction<T> transaction) {
        return submitSignedTransaction(transaction.getTransaction(), transaction.getEthereumSignature());
    }
}
