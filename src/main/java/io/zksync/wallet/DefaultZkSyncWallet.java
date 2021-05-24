package io.zksync.wallet;

import io.zksync.domain.TimeRange;
import io.zksync.domain.auth.ChangePubKeyECDSA;
import io.zksync.domain.auth.ChangePubKeyOnchain;
import io.zksync.domain.fee.TransactionFee;
import io.zksync.domain.state.AccountState;
import io.zksync.domain.token.NFT;
import io.zksync.domain.token.Token;
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
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.web3j.protocol.Web3j;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.utils.Numeric;

public class DefaultZkSyncWallet implements ZkSyncWallet {

    private EthSigner ethSigner;
    private ZkSigner zkSigner;

    @Getter
    private Provider provider;

    private Integer accountId;

    private String pubKeyHash;

    DefaultZkSyncWallet(EthSigner ethSigner, ZkSigner zkSigner, Provider provider) {
        this.ethSigner = ethSigner;
        this.zkSigner = zkSigner;

        this.provider = provider;

        final AccountState state = getState();

        this.accountId = state.getId();
        this.pubKeyHash = state.getCommitted().getPubKeyHash();
    }

    public static DefaultZkSyncWallet build(EthSigner ethSigner, ZkSigner zkSigner, ZkSyncTransport transport) {
        return new DefaultZkSyncWallet(ethSigner, zkSigner, new DefaultProvider(transport));
    }

    public static DefaultZkSyncWallet build(EthSigner ethSigner, ZkSigner zkSigner, Provider provider) {
        return new DefaultZkSyncWallet(ethSigner, zkSigner, provider);
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
            final SignedTransaction<ChangePubKey<ChangePubKeyECDSA>> signedTx = buildSignedChangePubKeyTxSigned(fee, nonceToUse, timeRange);
            return submitSignedTransaction(signedTx.getTransaction(), null, false);
        }
    }

    @Override
    public String syncTransfer(String to, BigInteger amount, TransactionFee fee, Integer nonce, TimeRange timeRange) {

        final Integer nonceToUse = nonce == null ? getNonce() : nonce;

        final SignedTransaction<Transfer> signedTransfer = buildSignedTransferTx(to, fee.getFeeToken(), amount,
                fee.getFee(), nonceToUse, timeRange);

        return submitSignedTransaction(signedTransfer.getTransaction(), signedTransfer.getEthereumSignature(), false);
    }

    @Override
    public String syncWithdraw(String ethAddress, BigInteger amount, TransactionFee fee, Integer nonce,
            boolean fastProcessing, TimeRange timeRange) {
        final Integer nonceToUse = nonce == null ? getNonce() : nonce;

        final SignedTransaction<Withdraw> signedWithdraw = buildSignedWithdrawTx(ethAddress, fee.getFeeToken(), amount,
                fee.getFee(), nonceToUse, timeRange);

        return submitSignedTransaction(signedWithdraw.getTransaction(), signedWithdraw.getEthereumSignature(),
                fastProcessing);
    }

    @Override
    public String syncForcedExit(String target, TransactionFee fee, Integer nonce, TimeRange timeRange) {
        final Integer nonceToUse = nonce == null ? getNonce() : nonce;

        final SignedTransaction<ForcedExit> signedForcedExit = buildSignedForcedExitTx(target, fee.getFeeToken(),
                fee.getFee(), nonceToUse, timeRange);

        return submitSignedTransaction(signedForcedExit.getTransaction(), signedForcedExit.getEthereumSignature(),
                false);
    }

    @Override
    public String syncMintNFT(String recipient, String contentHash, TransactionFee fee, Integer nonce) {
        final Integer nonceToUse = nonce == null ? getNonce() : nonce;

        final SignedTransaction<MintNFT> signedMintNFT = buildSignedMintNFTTx(recipient, contentHash, fee.getFeeToken(),
                fee.getFee(), nonceToUse);

        return submitSignedTransaction(signedMintNFT.getTransaction(), signedMintNFT.getEthereumSignature(),
                false);
    }

    @Override
    public String syncWithdrawNFT(String to, NFT token, TransactionFee fee, Integer nonce, TimeRange timeRange) {
        final Integer nonceToUse = nonce == null ? getNonce() : nonce;

        final SignedTransaction<WithdrawNFT> signedWithdrawNFT = buildSignedWithdrawNFTTx(to, token, fee.getFeeToken(), fee.getFee(), nonceToUse, timeRange);

        return submitSignedTransaction(signedWithdrawNFT.getTransaction(), signedWithdrawNFT.getEthereumSignature(), false);
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
                .accountId(accountId)
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
                .accountId(accountId)
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
    public String syncSwap(TransactionFee fee, Integer nonce) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AccountState getState() {
        return provider.getState(ethSigner.getAddress());
    }

    @Override
    public boolean isSigningKeySet() {
        return this.pubKeyHash.equals(this.zkSigner.getPublicKeyHash());
    }

    @SneakyThrows
    private SignedTransaction<ChangePubKey<ChangePubKeyECDSA>> buildSignedChangePubKeyTxSigned(TransactionFee fee, Integer nonce,
            TimeRange timeRange) {
        if (zkSigner == null) {
            throw new Error("ZKSync signer is required for current pubkey calculation.");
        }

        final Token token = provider.getTokens().getToken(fee.getFeeToken());

        final ChangePubKey<ChangePubKeyECDSA> changePubKey = ChangePubKey
            .<ChangePubKeyECDSA>builder()
            .accountId(accountId)
            .account(ethSigner.getAddress())
            .newPkHash(zkSigner.getPublicKeyHash())
            .nonce(nonce).feeToken(token.getId())
            .fee(fee.getFee().toString())
            .timeRange(timeRange)
            .build();


        ChangePubKeyECDSA auth = new ChangePubKeyECDSA(null,
                    Numeric.toHexString(Numeric.toBytesPadded(BigInteger.ZERO, 32)));
        changePubKey.setEthAuthData(auth);
        EthSignature ethSignature = ethSigner.signTransaction(changePubKey, nonce, token, fee.getFee()).get();
        auth.setEthSignature(ethSignature.getSignature());


        return new SignedTransaction<>(zkSigner.signChangePubKey(changePubKey), ethSignature);
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
                .accountId(accountId)
                .account(ethSigner.getAddress())
                .newPkHash(zkSigner.getPublicKeyHash())
                .nonce(nonce)
                .feeToken(token.getId())
                .fee(fee.getFee().toString())
                .ethAuthData(new ChangePubKeyOnchain())
                .timeRange(timeRange)
                .build();

        return new SignedTransaction<>(zkSigner.signChangePubKey(changePubKey), null);
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
                .accountId(accountId)
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
                .accountId(accountId)
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
                .initiatorAccountId(accountId)
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
            .creatorId(accountId)
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
            .accountId(accountId)
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

    private String submitSignedTransaction(ZkSyncTransaction signedTransaction,
                                         EthSignature ethereumSignature,
                                         boolean fastProcessing) {
        return provider.submitTx(signedTransaction, ethereumSignature, fastProcessing);
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

    @Override
    public EthereumProvider createEthereumProvider(Web3j web3j, ContractGasProvider contractGasProvider) {
        String contractAddress = this.provider.contractAddress().getMainContract();
        ZkSync contract = ZkSync.load(contractAddress, web3j, this.ethSigner.getTransactionManager(), contractGasProvider);
        DefaultEthereumProvider ethereum = new DefaultEthereumProvider(web3j, this.ethSigner, contract);
        return ethereum;
    }
}
