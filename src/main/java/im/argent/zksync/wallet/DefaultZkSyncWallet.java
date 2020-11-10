package im.argent.zksync.wallet;

import im.argent.zksync.domain.fee.TransactionFee;
import im.argent.zksync.domain.fee.TransactionFeeDetails;
import im.argent.zksync.domain.fee.TransactionFeeRequest;
import im.argent.zksync.domain.fee.TransactionType;
import im.argent.zksync.domain.state.AccountState;
import im.argent.zksync.domain.token.Token;
import im.argent.zksync.domain.token.Tokens;
import im.argent.zksync.domain.transaction.ChangePubKey;
import im.argent.zksync.domain.transaction.Transfer;
import im.argent.zksync.domain.transaction.Withdraw;
import im.argent.zksync.domain.transaction.ZkSyncTransaction;
import im.argent.zksync.provider.DefaultProvider;
import im.argent.zksync.provider.Provider;
import im.argent.zksync.signer.EthSignature;
import im.argent.zksync.signer.EthSigner;
import im.argent.zksync.signer.ZkSigner;
import im.argent.zksync.transport.ZkSyncTransport;
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

        //TODO better nonce logic (see js sdk)
        //TODO better fee logic

        final SignedTransaction<ChangePubKey> signedTx = buildSignedChangePubKeyTx(fee, nonce, onchainAuth);

        if (pubKeyHash.equals(signedTx.getTransaction().getNewPkHash())) {
            throw new RuntimeException("Current signing key is already set");
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

        return new SignedTransaction(zkSigner.signChangePubKey(changePubKey), ethSignature);
    }

    private SignedTransaction<Transfer> buildSignedTransferTx(String to, String tokenIdentifier, BigInteger amount, BigInteger fee, Integer nonce) {
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

    private String submitSignedTransaction(ZkSyncTransaction signedTransaction,
                                         EthSignature ethereumSignature,
                                         boolean fastProcessing) {
        return provider.submitTx(signedTransaction, ethereumSignature, fastProcessing);
    }

    private Integer getNonce() {
        return getState().getCommitted().getNonce();
    }
}
