package io.zksync.wallet;

import io.zksync.domain.fee.TransactionFee;
import io.zksync.domain.fee.TransactionFeeDetails;
import io.zksync.domain.fee.TransactionFeeRequest;
import io.zksync.domain.fee.TransactionType;
import io.zksync.domain.state.AccountState;
import io.zksync.domain.token.Token;
import io.zksync.domain.token.Tokens;
import io.zksync.domain.transaction.*;
import io.zksync.exception.ZkSyncException;
import io.zksync.provider.DefaultProvider;
import io.zksync.provider.Provider;
import io.zksync.signer.EthSignature;
import io.zksync.signer.EthSigner;
import io.zksync.signer.ZkSigner;
import io.zksync.transport.ZkSyncTransport;
import lombok.Getter;

import java.math.BigInteger;

public class DefaultZkSyncWallet implements ZkSyncWallet {

    private EthSigner ethSigner;
    private ZkSigner zkSigner;

    @Getter
    private Provider provider;

    private Integer accountId;

    private String pubKeyHash;

    DefaultZkSyncWallet(EthSigner ethSigner, ZkSigner zkSigner, ZkSyncTransport transport) {
        this.ethSigner = ethSigner;
        this.zkSigner = zkSigner;

        this.provider = new DefaultProvider(transport);

        final AccountState state = getState();

        this.accountId = state.getId();
        this.pubKeyHash = state.getCommitted().getPubKeyHash();
    }

    public static DefaultZkSyncWallet build(EthSigner ethSigner, ZkSigner zkSigner, ZkSyncTransport transport) {
        return new DefaultZkSyncWallet(ethSigner, zkSigner, transport);
    }

    @Override
    public String setSigningKey(TransactionFee fee, Integer nonce, boolean onchainAuth) {

        final Integer nonceToUse = nonce == null ? getNonce() : nonce;

        final SignedTransaction<ChangePubKey> signedTx = buildSignedChangePubKeyTx(fee, nonceToUse, onchainAuth);

        if (pubKeyHash.equals(signedTx.getTransaction().getNewPkHash())) {
            throw new ZkSyncException("Current signing key is already set");
        }

        return submitSignedTransaction(signedTx.getTransaction(), signedTx.getEthereumSignature(), false);
    }

    @Override
    public String syncTransfer(String to, String tokenIdentifier, BigInteger amount, BigInteger fee, Integer nonce) {

        final Integer nonceToUse = nonce == null ? getNonce() : nonce;

        final SignedTransaction<Transfer> signedTransfer = buildSignedTransferTx(to , tokenIdentifier, amount, fee, nonceToUse);

        return submitSignedTransaction(signedTransfer.getTransaction(), signedTransfer.getEthereumSignature(), false);
    }

    @Override
    public String syncWithdraw(String ethAddress,
                               String tokenIdentifier,
                               BigInteger amount,
                               BigInteger fee,
                               Integer nonce,
                               boolean fastProcessing) {
        final Integer nonceToUse = nonce == null ? getNonce() : nonce;
        final TransactionType txType = fastProcessing ? TransactionType.FAST_WITHDRAW : TransactionType.WITHDRAW;
        final BigInteger feeToUse = fee == null ?
                getTransactionFee(txType, ethAddress, tokenIdentifier).getTotalFeeInteger() : fee;

        final SignedTransaction<Withdraw> signedWithdraw =
                buildSignedWithdrawTx(ethAddress, tokenIdentifier, amount, feeToUse, nonceToUse);

        return submitSignedTransaction(
                signedWithdraw.getTransaction(), signedWithdraw.getEthereumSignature(), fastProcessing);
    }

    @Override
    public String syncForcedExit(String target, String tokenIdentifier, BigInteger fee, Integer nonce) {
        final Integer nonceToUse = nonce == null ? getNonce() : nonce;

        final BigInteger feeToUse = fee == null ?
                getTransactionFee(TransactionType.FORCED_EXIT, target, tokenIdentifier).getTotalFeeInteger() : fee;

        final SignedTransaction<ForcedExit> signedForcedExit =
                buildSignedForcedExitTx(target, tokenIdentifier, feeToUse, nonceToUse);

        return submitSignedTransaction(
                signedForcedExit.getTransaction(), signedForcedExit.getEthereumSignature(), false);
    }

    @Override
    public AccountState getState() {
        return provider.getState(ethSigner.getAddress());
    }

    @Override
    public TransactionFeeDetails getTransactionFee(TransactionType type, String tokenIdentifier) {
        return getTransactionFee(type, ethSigner.getAddress(), tokenIdentifier);
    }

    @Override
    public TransactionFeeDetails getTransactionFee(TransactionType type, String address, String tokenIdentifier) {
        return provider.getTransactionFee(TransactionFeeRequest
                .builder()
                .transactionType(type)
                .address(address)
                .tokenIdentifier(tokenIdentifier)
                .build());
    }

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
                    zkSigner.getPublicKeyHash(), nonce, accountId);

            changePubKey.setEthSignature(ethSignature.getSignature());
        }

        return new SignedTransaction<>(zkSigner.signChangePubKey(changePubKey), ethSignature);
    }

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
                to, accountId, nonce, amount, provider.getTokens().getToken(tokenIdentifier), fee);

        return new SignedTransaction<>(zkSigner.signTransfer(transfer), ethSignature);
    }

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
                to, accountId, nonce, amount, provider.getTokens().getToken(tokenIdentifier), fee);

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
}
