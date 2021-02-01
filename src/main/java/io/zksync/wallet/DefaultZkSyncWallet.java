package io.zksync.wallet;

import io.zksync.domain.fee.TransactionFee;
import io.zksync.domain.fee.TransactionFeeDetails;
import io.zksync.domain.fee.TransactionFeeRequest;
import io.zksync.domain.fee.TransactionType;
import io.zksync.domain.state.AccountState;
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

import org.web3j.protocol.Web3j;
import org.web3j.tx.gas.ContractGasProvider;

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
    public String setSigningKey(TransactionFee fee, Integer nonce, boolean onchainAuth) {

        if (isSigningKeySet()) {
            throw new ZkSyncException("Current signing key is already set");
        }

        final Integer nonceToUse = nonce == null ? getNonce() : nonce;

        final SignedTransaction<ChangePubKey> signedTx = buildSignedChangePubKeyTx(fee, nonceToUse, onchainAuth);

        return submitSignedTransaction(signedTx.getTransaction(), signedTx.getEthereumSignature(), false);
    }

    @Override
    public String syncTransfer(String to, BigInteger amount, TransactionFee fee, Integer nonce) {

        final Integer nonceToUse = nonce == null ? getNonce() : nonce;

        final SignedTransaction<Transfer> signedTransfer = buildSignedTransferTx(to , fee.getFeeToken(), amount, fee.getFee(), nonceToUse);

        return submitSignedTransaction(signedTransfer.getTransaction(), signedTransfer.getEthereumSignature(), false);
    }

    @Override
    public String syncWithdraw(String ethAddress,
                               BigInteger amount,
                               TransactionFee fee,
                               Integer nonce,
                               boolean fastProcessing) {
        final Integer nonceToUse = nonce == null ? getNonce() : nonce;

        final SignedTransaction<Withdraw> signedWithdraw =
                buildSignedWithdrawTx(ethAddress, fee.getFeeToken(), amount, fee.getFee(), nonceToUse);

        return submitSignedTransaction(
                signedWithdraw.getTransaction(), signedWithdraw.getEthereumSignature(), fastProcessing);
    }

    @Override
    public String syncForcedExit(String target, TransactionFee fee, Integer nonce) {
        final Integer nonceToUse = nonce == null ? getNonce() : nonce;

        final SignedTransaction<ForcedExit> signedForcedExit =
                buildSignedForcedExitTx(target, fee.getFeeToken(), fee.getFee(), nonceToUse);

        return submitSignedTransaction(
                signedForcedExit.getTransaction(), signedForcedExit.getEthereumSignature(), false);
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
    private SignedTransaction<ChangePubKey> buildSignedChangePubKeyTx(TransactionFee fee, Integer nonce, boolean onchainAuth) {
        if (zkSigner == null) {
            throw new Error("ZKSync signer is required for current pubkey calculation.");
        }

        final Token token = provider.getTokens().getToken(fee.getFeeToken());

        final ChangePubKey changePubKey = ChangePubKey
                .builder()
                .accountId(accountId)
                .account(ethSigner.getAddress())
                .newPkHash(zkSigner.getPublicKeyHash())
                .nonce(nonce)
                .feeToken(token.getId())
                .fee(fee.getFee().toString())
                .build();

        EthSignature ethSignature = null;

        if (!onchainAuth) {
            ethSignature = ethSigner.signChangePubKey(
                    zkSigner.getPublicKeyHash(), nonce, accountId).get();

            changePubKey.setEthSignature(ethSignature.getSignature());
        }

        return new SignedTransaction<>(zkSigner.signChangePubKey(changePubKey), ethSignature);
    }

    @SneakyThrows
    private SignedTransaction<Transfer> buildSignedTransferTx(String to,
                                                              String tokenIdentifier,
                                                              BigInteger amount,
                                                              BigInteger fee,
                                                              Integer nonce) {
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
                .build();

        final EthSignature ethSignature = ethSigner.signTransfer(
                to, accountId, nonce, amount, token, fee).get();

        return new SignedTransaction<>(zkSigner.signTransfer(transfer), ethSignature);
    }

    @SneakyThrows
    private SignedTransaction<Withdraw> buildSignedWithdrawTx(String to, String tokenIdentifier, BigInteger amount, BigInteger fee, Integer nonce) {
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
                .build();

        final EthSignature ethSignature = ethSigner.signWithdraw(
                to, accountId, nonce, amount, provider.getTokens().getToken(tokenIdentifier), fee).get();

        return new SignedTransaction<>(zkSigner.signWithdraw(withdraw), ethSignature);
    }

    private SignedTransaction<ForcedExit> buildSignedForcedExitTx(String target,
                                                                String tokenIdentifier,
                                                                BigInteger fee,
                                                                Integer nonce) {
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
                .build();

        return new SignedTransaction<>(zkSigner.signForcedExit(forcedExit), null);
    }

    private String submitSignedTransaction(ZkSyncTransaction signedTransaction,
                                         EthSignature ethereumSignature,
                                         boolean fastProcessing) {
        return provider.submitTx(signedTransaction, ethereumSignature, fastProcessing);
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
