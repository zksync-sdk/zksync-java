package io.zksync.domain;

import java.math.BigInteger;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;
import org.web3j.tuples.generated.Tuple2;

import io.zksync.domain.auth.ChangePubKeyAuthType;
import io.zksync.domain.auth.ChangePubKeyVariant;
import io.zksync.domain.fee.TransactionFee;
import io.zksync.domain.fee.TransactionFeeBatchRequest;
import io.zksync.domain.fee.TransactionFeeDetails;
import io.zksync.domain.fee.TransactionFeeRequest;
import io.zksync.domain.fee.TransactionType;
import io.zksync.domain.swap.Order;
import io.zksync.domain.token.NFT;
import io.zksync.domain.token.Token;
import io.zksync.domain.token.TokenId;
import io.zksync.domain.token.Tokens;
import io.zksync.domain.transaction.ChangePubKey;
import io.zksync.domain.transaction.ForcedExit;
import io.zksync.domain.transaction.MintNFT;
import io.zksync.domain.transaction.Swap;
import io.zksync.domain.transaction.Transfer;
import io.zksync.domain.transaction.Withdraw;
import io.zksync.domain.transaction.WithdrawNFT;
import io.zksync.wallet.ZkASyncWallet;
import io.zksync.wallet.ZkSyncWallet;
import lombok.Getter;

@Getter
public class TransactionBuildHelper {

    private Tokens tokens;

    private Supplier<CompletableFuture<Integer>> accountId;
    private Supplier<CompletableFuture<Integer>> nonce;
    private Function<TransactionFeeRequest, CompletableFuture<TransactionFeeDetails>> transactionFee;
    private Function<TransactionFeeBatchRequest, CompletableFuture<TransactionFeeDetails>> transactionFeeBatch;
    private Supplier<String> address;

    public TransactionBuildHelper(ZkASyncWallet asyncWallet, Tokens tokens) {
        this.tokens = tokens;

        this.accountId = asyncWallet::getAccountId;
        this.nonce = asyncWallet::getNonce;
        this.transactionFee = asyncWallet.getProvider()::getTransactionFee;
        this.transactionFeeBatch = asyncWallet.getProvider()::getTransactionFee;
        this.address = asyncWallet::getAddress;
    }

    public TransactionBuildHelper(ZkSyncWallet wallet, Tokens tokens) {
        this.tokens = tokens;

        this.accountId = () -> CompletableFuture.completedFuture(wallet.getAccountId());
        this.nonce = () -> CompletableFuture.completedFuture(wallet.getState().getCommitted().getNonce());
        this.transactionFee = (request) -> CompletableFuture.completedFuture(wallet.getProvider().getTransactionFee(request));
        this.transactionFeeBatch = (request) -> CompletableFuture.completedFuture(wallet.getProvider().getTransactionFee(request));
        this.address = wallet::getAddress;
    }

    public Token getToken(String tokenIdentifier) {
        final Token token = tokens.getTokenBySymbol(tokenIdentifier) != null ?
                tokens.getTokenBySymbol(tokenIdentifier) : tokens.getTokenByAddress(tokenIdentifier);

        return token;
    }

    /**
     * Build changePubKey transaction
     * 
     * @param <A> - Authentication variant type
     * @param pubKeyHash - Public key hash in ZkSync format
     * @param fee - Fee amount for paying the transaction
     * @param nonce - Nonce value
     * @param timeRange - Timerange of validity of the transcation
     * @return - Unsigned transaction object
     */
    public <A extends ChangePubKeyVariant> CompletableFuture<ChangePubKey<A>> changePubKey(String pubKeyHash, TransactionFee fee, Integer nonce, TimeRange timeRange) {
        final String tokenIdentifier = fee.getFeeToken();

        return CompletableFuture.supplyAsync(() -> {
            final ChangePubKey<A> changePubKey = ChangePubKey
                .<A>builder()
                .accountId(accountId.get().join())
                .account(address.get())
                .newPkHash(pubKeyHash)
                .nonce(nonce == null ? this.nonce.get().join() : nonce)
                .feeToken(getToken(tokenIdentifier).getId())
                .fee(fee.getFee().toString())
                .timeRange(timeRange == null ? new TimeRange() : timeRange)
                .build();
            return changePubKey;
        });
    }

    /**
     * Build changePubKey transaction (With default time range)
     * 
     * @param <A> - Authentication variant type
     * @param pubKeyHash - Public key hash in ZkSync format
     * @param fee - Fee amount for paying the transaction
     * @param nonce - Nonce value
     * @return - Unsigned transaction object
     */
    public <A extends ChangePubKeyVariant> CompletableFuture<ChangePubKey<A>> changePubKey(String pubKeyHash, TransactionFee fee, Integer nonce) {
        return changePubKey(pubKeyHash, fee, nonce, null);
    }

    /**
     * Build changePubKey transaction (With default time range and load nonce from network)
     * 
     * @param <A> - Authentication variant type
     * @param pubKeyHash - Public key hash in ZkSync format
     * @param fee - Fee amount for paying the transaction
     * @return - Unsigned transaction object
     */
    public <A extends ChangePubKeyVariant> CompletableFuture<ChangePubKey<A>> changePubKey(String pubKeyHash, TransactionFee fee) {
        return changePubKey(pubKeyHash, fee, null, null);
    }

    /**
     * Build changePubKey transaction (With default time range and load nonce from network)
     * This function will estimate the fee for the transaction
     * 
     * @param <A> - Authentication variant type
     * @param <T> - Token type
     * @param pubKeyHash - Public key hash in ZkSync format
     * @param tokenId - Fundible token id
     * @param auth - Authentication variant object
     * @return - Unsigned transaction object
     */
    public <A extends ChangePubKeyVariant, T extends TokenId> CompletableFuture<ChangePubKey<A>> changePubKey(String pubKeyHash, T tokenId, A auth) {
        TransactionType transactionType;
        if (auth.getType() == ChangePubKeyAuthType.CREATE2) {
            transactionType = TransactionType.CHANGE_PUB_KEY_CREATE2;
        } else if (auth.getType() == ChangePubKeyAuthType.ECDSA) {
            transactionType = TransactionType.CHANGE_PUB_KEY_ECDSA;
        } else if (auth.getType() == ChangePubKeyAuthType.Onchain) {
            transactionType = TransactionType.CHANGE_PUB_KEY_ONCHAIN;
        } else {
            throw new IllegalArgumentException("Unknown authentication type");
        }
        TransactionFeeRequest feeRequest = TransactionFeeRequest.builder()
                .transactionType(transactionType)
                .address(address.get())
                .tokenIdentifier(tokenId.getSymbol())
                .build();
        return transactionFee.apply(feeRequest)
            .thenApply(feeTotal -> {
                TransactionFee fee = new TransactionFee(tokenId.getSymbol(), feeTotal.getTotalFeeInteger());
                ChangePubKey<A> changePubKey = this.<A>changePubKey(pubKeyHash, fee, null, null).join();
                changePubKey.setEthAuthData(auth);
                return changePubKey;
            });
    }

    /*
     * Transfer transaction
     */

    /**
     * Build transfer transaction
     * 
     * @param to - Ethereum address of the receiver of the funds
     * @param amount - Amount of the funds to be transferred
     * @param fee - Fee amount for paying the transaction
     * @param nonce - Nonce value
     * @param timeRange - Timerange of validity of the transcation
     * @return - Unsigned transaction object
     */
    public CompletableFuture<Transfer> transfer(String to, BigInteger amount, TransactionFee fee, Integer nonce, TimeRange timeRange) {
        final String tokenIdentifier = fee.getFeeToken();

        return CompletableFuture.supplyAsync(() -> {
            final Transfer transfer = Transfer
                .builder()
                .accountId(accountId.get().join())
                .from(address.get())
                .to(to)
                .token(getToken(tokenIdentifier).getId())
                .amount(amount)
                .nonce(nonce == null ? this.nonce.get().join() : nonce)
                .fee(fee.getFee().toString())
                .timeRange(timeRange == null ? new TimeRange() : timeRange)
                .build();
            return transfer;
        });
    }

    /**
     * Build transfer transaction (With default time range)
     * 
     * @param to - Ethereum address of the receiver of the funds
     * @param amount - Amount of the funds to be transferred
     * @param fee - Fee amount for paying the transaction
     * @param nonce - Nonce value
     * @return - Unsigned transaction object
     */
    public CompletableFuture<Transfer> transfer(String to, BigInteger amount, TransactionFee fee, Integer nonce) {
        return transfer(to, amount, fee, nonce, null);
    }

    /**
     * Build transfer transaction (With default time range and load nonce from network)
     * 
     * @param to - Ethereum address of the receiver of the funds
     * @param amount - Amount of the funds to be transferred
     * @param fee - Fee amount for paying the transaction
     * @return - Unsigned transaction object
     */
    public CompletableFuture<Transfer> transfer(String to, BigInteger amount, TransactionFee fee) {
        return transfer(to, amount, fee, null, null);
    }

    /**
     * Build transfer transaction (With default time range and load nonce from network)
     * This function will estimate the fee for the transaction
     * 
     * @param <T> - Token type
     * @param to - Ethereum address of the receiver of the funds
     * @param amount - Amount of the funds to be transferred
     * @param tokenId - Fundible token id
     * @return - Unsigned transaction object
     */
    public <T extends TokenId> CompletableFuture<Transfer> transfer(String to, BigInteger amount, T tokenId) {
        TransactionFeeRequest feeRequest = TransactionFeeRequest.builder()
                .transactionType(TransactionType.TRANSFER)
                .address(to)
                .tokenIdentifier(tokenId.getSymbol())
                .build();
        return transactionFee.apply(feeRequest)
            .thenApply(feeTotal -> {
                TransactionFee fee = new TransactionFee(tokenId.getSymbol(), feeTotal.getTotalFeeInteger());
                return transfer(to, amount, fee, null, null).join();
            });
    }

    /*
     * Withdraw transaction
     */

    /**
     * Build withdraw transaction
     * 
     * @param to - Ethereum address of the receiver of the funds
     * @param amount - Amount of the funds to be withdrawn
     * @param fee - Fee amount for paying the transaction
     * @param nonce - Nonce value
     * @param timeRange - Timerange of validity of the transcation
     * @return - Unsigned transaction object
     */
    public CompletableFuture<Withdraw> withdraw(String to, BigInteger amount, TransactionFee fee, Integer nonce, TimeRange timeRange) {
        final String tokenIdentifier = fee.getFeeToken();

        return CompletableFuture.supplyAsync(() -> {
            final Withdraw withdraw = Withdraw
                .builder()
                .accountId(accountId.get().join())
                .from(address.get())
                .to(to)
                .token(getToken(tokenIdentifier).getId())
                .amount(amount)
                .nonce(nonce == null ? this.nonce.get().join() : nonce)
                .fee(fee.getFee().toString())
                .timeRange(timeRange == null ? new TimeRange() : timeRange)
                .build();
            return withdraw;
        });
    }

    /**
     * Build withdraw transaction (With default time range)
     * 
     * @param to - Ethereum address of the receiver of the funds
     * @param amount - Amount of the funds to be withdrawn
     * @param fee - Fee amount for paying the transaction
     * @param nonce - Nonce value
     * @return - Unsigned transaction object
     */
    public CompletableFuture<Withdraw> withdraw(String to, BigInteger amount, TransactionFee fee, Integer nonce) {
        return withdraw(to, amount, fee, nonce, null);
    }

    /**
     * Build withdraw transaction (With default time range and load nonce from network)
     * 
     * @param to - Ethereum address of the receiver of the funds
     * @param amount - Amount of the funds to be withdrawn
     * @param fee - Fee amount for paying the transaction
     * @return - Unsigned transaction object
     */
    public CompletableFuture<Withdraw> withdraw(String to, BigInteger amount, TransactionFee fee) {
        return withdraw(to, amount, fee, null, null);
    }

    /**
     * Build withdraw transaction (With default time range and load nonce from network)
     * This function will estimate the fee for the transaction
     * 
     * @param <T> - Token type
     * @param to - Ethereum address of the receiver of the funds
     * @param amount - Amount of the funds to be transferred
     * @param tokenId - Fundible token id
     * @param fastProcessing - Mark the transaction should be executed as fast as possible
     * @return - Unsigned transaction object
     */
    public <T extends TokenId> CompletableFuture<Withdraw> withdraw(String to, BigInteger amount, T tokenId, boolean fastProcessing) {
        TransactionFeeRequest feeRequest = TransactionFeeRequest.builder()
                .transactionType(fastProcessing ? TransactionType.FAST_WITHDRAW : TransactionType.WITHDRAW)
                .address(to)
                .tokenIdentifier(tokenId.getSymbol())
                .build();
        return transactionFee.apply(feeRequest)
            .thenApply(feeTotal -> {
                TransactionFee fee = new TransactionFee(tokenId.getSymbol(), feeTotal.getTotalFeeInteger());
                return withdraw(to, amount, fee, null, null).join();
            });
    }

    /*
     * ForcedExit transaction
     */

    /**
     * Build forced exit transaction
     * 
     * @param to - Ethereum address of the receiver of the funds
     * @param fee - Fee amount for paying the transaction
     * @param nonce - Nonce value
     * @param timeRange - Timerange of validity of the transcation
     * @return - Unsigned transaction object
     */
    public CompletableFuture<ForcedExit> forcedExit(String to, TransactionFee fee, Integer nonce, TimeRange timeRange) {
        final String tokenIdentifier = fee.getFeeToken();

        return CompletableFuture.supplyAsync(() -> {
            final ForcedExit forcedExit = ForcedExit
                .builder()
                .initiatorAccountId(accountId.get().join())
                .target(to)
                .token(getToken(tokenIdentifier).getId())
                .nonce(nonce == null ? this.nonce.get().join() : nonce)
                .fee(fee.getFee().toString())
                .timeRange(timeRange == null ? new TimeRange() : timeRange)
                .build();
            return forcedExit;
        });
    }

    /**
     * Build forced exit transaction (With default time range)
     * 
     * @param to - Ethereum address of the receiver of the funds
     * @param fee - Fee amount for paying the transaction
     * @param nonce - Nonce value
     * @return - Unsigned transaction object
     */
    public CompletableFuture<ForcedExit> forcedExit(String to, TransactionFee fee, Integer nonce) {
        return forcedExit(to, fee, nonce, null);
    }

    /**
     * Build forced exit transaction (With default time range and load nonce from network)
     * 
     * @param to - Ethereum address of the receiver of the funds
     * @param fee - Fee amount for paying the transaction
     * @return - Unsigned transaction object
     */
    public CompletableFuture<ForcedExit> forcedExit(String to, TransactionFee fee) {
        return forcedExit(to, fee, null, null);
    }

    /**
     * Build forced exit transaction (With default time range and load nonce from network)
     * This function will estimate the fee for the transaction
     * 
     * @param <T> - Token type
     * @param to - Ethereum address of the receiver of the funds
     * @param tokenId - Fundible token id
     * @return - Unsigned transaction object
     */
    public <T extends TokenId> CompletableFuture<ForcedExit> forcedExit(String to, T tokenId) {
        TransactionFeeRequest feeRequest = TransactionFeeRequest.builder()
                .transactionType(TransactionType.FORCED_EXIT)
                .address(address.get())
                .tokenIdentifier(tokenId.getSymbol())
                .build();
        return transactionFee.apply(feeRequest)
            .thenApply(feeTotal -> {
                TransactionFee fee = new TransactionFee(tokenId.getSymbol(), feeTotal.getTotalFeeInteger());
                return forcedExit(to, fee, null, null).join();
            });
    }

    /*
     * MintNFT transaction
     */

    /**
     * Build mint NFT transaction
     * 
     * @param recipient - Ethereum address of the receiver of the NFT
     * @param contentHash - Hash for creation Non-fundible token
     * @param fee - Fee amount for paying the transaction
     * @param nonce - Nonce value
     * @return - Unsigned transaction object
     */
    public CompletableFuture<MintNFT> mintNFT(String recipient, String contentHash, TransactionFee fee, Integer nonce) {
        final String tokenIdentifier = fee.getFeeToken();

        return CompletableFuture.supplyAsync(() -> {
            final MintNFT mintNft = MintNFT
                .builder()
                .creatorId(accountId.get().join())
                .creatorAddress(address.get())
                .contentHash(contentHash)
                .recipient(recipient)
                .fee(fee.getFee().toString())
                .feeToken(getToken(tokenIdentifier).getId())
                .nonce(nonce == null ? this.nonce.get().join() : nonce)
                .build();
            return mintNft;
        });
    }

    /**
     * Build mint NFT transaction (Load nonce from network)
     * 
     * @param recipient - Ethereum address of the receiver of the NFT
     * @param contentHash - Hash for creation Non-fundible token
     * @param fee - Fee amount for paying the transaction
     * @return - Unsigned transaction object
     */
    public CompletableFuture<MintNFT> mintNFT(String recipient, String contentHash, TransactionFee fee) {
        return mintNFT(recipient, contentHash, fee, null);
    }

    /**
     * Build mint NFT transaction (Load nonce from network)
     * This function will estimate the fee for the transaction
     * 
     * @param <T> - Token type
     * @param recipient - Ethereum address of the receiver of the NFT
     * @param contentHash - Hash for creation Non-fundible token
     * @param tokenId - Fundible token id
     * @return - Unsigned transaction object
     */
    public <T extends TokenId> CompletableFuture<MintNFT> mintNFT(String recipient, String contentHash, T tokenId) {
        TransactionFeeRequest feeRequest = TransactionFeeRequest.builder()
                .transactionType(TransactionType.MINT_NFT)
                .address(address.get())
                .tokenIdentifier(tokenId.getSymbol())
                .build();
        return transactionFee.apply(feeRequest)
            .thenApply(feeTotal -> {
                TransactionFee fee = new TransactionFee(tokenId.getSymbol(), feeTotal.getTotalFeeInteger());
                return mintNFT(recipient, contentHash, fee, null).join();
            });
    }

    /*
     * WithdrawNFT transaction
     */

    /**
     * Build withdraw NFT transaction
     * 
     * @param to - Ethereum address of the receiver of the NFT
     * @param token - Existing Non-fundible token
     * @param fee - Fee amount for paying the transaction
     * @param nonce - Nonce value
     * @param timeRange - Timerange of validity of the transcation
     * @return - Unsigned transaction object
     */
    public CompletableFuture<WithdrawNFT> withdrawNFT(String to, NFT token, TransactionFee fee, Integer nonce, TimeRange timeRange) {
        final String tokenIdentifier = fee.getFeeToken();

        return CompletableFuture.supplyAsync(() -> {
            final WithdrawNFT withdrawNFT = WithdrawNFT
                .builder()
                .accountId(accountId.get().join())
                .from(address.get())
                .to(to)
                .feeToken(getToken(tokenIdentifier).getId())
                .nonce(nonce == null ? this.nonce.get().join() : nonce)
                .timeRange(timeRange == null ? new TimeRange() : timeRange)
                .fee(fee.getFee().toString())
                .timeRange(timeRange)
                .build();
            return withdrawNFT;
        });
    }

    /**
     * Build withdraw NFT transaction (With default time range)
     * 
     * @param to - Ethereum address of the receiver of the NFT
     * @param token - Existing Non-fundible token
     * @param fee - Fee amount for paying the transaction
     * @param nonce - Nonce value
     * @return - Unsigned transaction object
     */
    public CompletableFuture<WithdrawNFT> withdrawNFT(String to, NFT token, TransactionFee fee, Integer nonce) {
        return withdrawNFT(to, token, fee, nonce, null);
    }

    /**
     * Build withdraw NFT transaction (With default time range and load nonce from network)
     * 
     * @param to - Ethereum address of the receiver of the NFT
     * @param token - Existing Non-fundible token
     * @param fee - Fee amount for paying the transaction
     * @return - Unsigned transaction object
     */
    public CompletableFuture<WithdrawNFT> withdrawNFT(String to, NFT token, TransactionFee fee) {
        return withdrawNFT(to, token, fee, null, null);
    }

    /**
     * Build withdraw NFT transaction (With default time range and load nonce from network)
     * This function will estimate the fee for the transaction
     * 
     * @param <T> - Token type
     * @param to - Ethereum address of the receiver of the NFT
     * @param token - Existing Non-fundible token
     * @param tokenId - Fundible token id
     * @param fastProcessing - Mark the transaction should be executed as fast as possible
     * @return - Unsigned transaction object
     */
    public <T extends TokenId> CompletableFuture<WithdrawNFT> withdrawNFT(String to, NFT token, T tokenId, boolean fastProcessing) {
        TransactionFeeRequest feeRequest = TransactionFeeRequest.builder()
                .transactionType(fastProcessing ? TransactionType.FAST_WITHDRAW_NFT : TransactionType.WITHDRAW_NFT)
                .address(address.get())
                .tokenIdentifier(tokenId.getSymbol())
                .build();
        return transactionFee.apply(feeRequest)
            .thenApply(feeTotal -> {
                TransactionFee fee = new TransactionFee(tokenId.getSymbol(), feeTotal.getTotalFeeInteger());
                return withdrawNFT(to, token, fee, null, null).join();
            });
    }

    /*
     * TransferNFT transaction
     */

    /**
     * Build transfer NFT transaction
     * 
     * @param to - Ethereum address of the receiver of the NFT
     * @param token - Existing Non-fundible token
     * @param fee - Fee amount for paying the transaction
     * @param nonce - Nonce value
     * @param timeRange - Timerange of validity of the transcation
     * @return - Unsigned transaction objects
     */
    public CompletableFuture<Tuple2<Transfer, Transfer>> transferNFT(String to, NFT token, TransactionFee fee, Integer nonce, TimeRange timeRange) {
        final String tokenIdentifier = fee.getFeeToken();

        return CompletableFuture.supplyAsync(() -> {
            final Integer accountId = this.accountId.get().join();
            final String address = this.address.get();
            final Token feeToken = getToken(tokenIdentifier);
            final Integer nonceToUse = nonce == null ? this.nonce.get().join() : nonce;
            final TimeRange timeRangeToUse = timeRange == null ? new TimeRange() : timeRange;
            final Transfer transferNft = Transfer
                .builder()
                .accountId(accountId)
                .from(address)
                .to(to)
                .token(token.getId())
                .tokenId(token)
                .amount(BigInteger.ONE)
                .nonce(nonceToUse)
                .fee(BigInteger.ZERO.toString())
                .timeRange(timeRangeToUse)
                .build();
            final Transfer transferFee = Transfer
                .builder()
                .accountId(accountId)
                .from(address)
                .to(address)
                .token(feeToken.getId())
                .tokenId(feeToken)
                .amount(BigInteger.ZERO)
                .nonce(nonceToUse + 1)
                .fee(fee.getFee().toString())
                .timeRange(timeRangeToUse)
                .build();
            return new Tuple2<>(transferNft, transferFee);
        });
    }

    /**
     * Build transfer NFT transaction (With default time range)
     * 
     * @param to - Ethereum address of the receiver of the NFT
     * @param token - Existing Non-fundible token
     * @param fee - Fee amount for paying the transaction
     * @param nonce - Nonce value
     * @return - Unsigned transaction objects
     */
    public CompletableFuture<Tuple2<Transfer, Transfer>> transferNFT(String to, NFT token, TransactionFee fee, Integer nonce) {
        return transferNFT(to, token, fee, nonce, null);
    }

    /**
     * Build transfer NFT transaction (With default time range and load nonce from network)
     * 
     * @param to - Ethereum address of the receiver of the NFT
     * @param token - Existing Non-fundible token
     * @param fee - Fee amount for paying the transaction
     * @return - Unsigned transaction objects
     */
    public CompletableFuture<Tuple2<Transfer, Transfer>> transferNFT(String to, NFT token, TransactionFee fee) {
        return transferNFT(to, token, fee, null, null);
    }

    /**
     * Build transfer NFT transaction (With default time range and load nonce from network)
     * This function will estimate the fee for the transaction
     * 
     * @param <T> - Token type
     * @param to - Ethereum address of the receiver of the NFT
     * @param token - Existing Non-fundible token
     * @param tokenId - Fundible token id
     * @return - Unsigned transaction objects
     */
    public <T extends TokenId> CompletableFuture<Tuple2<Transfer, Transfer>> transferNFT(String to, NFT token, T tokenId) {
        TransactionFeeBatchRequest feeRequest = TransactionFeeBatchRequest.builder()
                .transactionType(Pair.of(TransactionType.TRANSFER, to))
                .transactionType(Pair.of(TransactionType.TRANSFER, address.get()))
                .tokenIdentifier(tokenId.getSymbol())
                .build();
        return transactionFeeBatch.apply(feeRequest)
            .thenApply(feeTotal -> {
                TransactionFee fee = new TransactionFee(tokenId.getSymbol(), feeTotal.getTotalFeeInteger());
                return transferNFT(to, token, fee, null, null).join();
            });
    }

    /*
     * Swap transaction
     */

    /**
     * Build swap transaction
     * 
     * @param order1 - Signed order
     * @param order2 - Signed order
     * @param amount1 - Amount funds to be swapped
     * @param amount2 - Amount funds to be swapped
     * @param fee - Fee amount for paying the transaction
     * @param nonce - Nonce value
     * @return - Unsigned transaction object
     */
    public CompletableFuture<Swap> swap(Order order1, Order order2, BigInteger amount1, BigInteger amount2, TransactionFee fee, Integer nonce) {
        final String tokenIdentifier = fee.getFeeToken();

        return CompletableFuture.supplyAsync(() -> {
            final Swap swap = Swap.builder()
                .orders(new Tuple2<>(order1, order2))
                .submitterAddress(address.get())
                .submitterId(accountId.get().join())
                .amounts(new Tuple2<>(amount1, amount2))
                .nonce(nonce == null ? this.nonce.get().join() : nonce)
                .fee(fee.getFee().toString())
                .feeToken(getToken(tokenIdentifier).getId())
                .build();
            return swap;
        });
    }

    /**
     * Build swap transaction (Load nonce from network)
     * 
     * @param order1 - Signed order
     * @param order2 - Signed order
     * @param amount1 - Amount funds to be swapped
     * @param amount2 - Amount funds to be swapped
     * @param fee - Fee amount for paying the transaction
     * @return - Unsigned transaction object
     */
    public CompletableFuture<Swap> swap(Order order1, Order order2, BigInteger amount1, BigInteger amount2, TransactionFee fee) {
        return swap(order1, order2, amount1, amount2, fee, null);
    }

    /**
     * Build swap transaction (Load nonce from network)
     * This function will estimate the fee for the transaction
     * 
     * @param <T> - Token type
     * @param order1 - Signed order
     * @param order2 - Signed order
     * @param amount1 - Amount funds to be swapped
     * @param amount2 - Amount funds to be swapped
     * @param tokenId - Fundible token id
     * @return - Unsigned transaction object
     */
    public <T extends TokenId> CompletableFuture<Swap> swap(Order order1, Order order2, BigInteger amount1, BigInteger amount2, T tokenId) {
        TransactionFeeBatchRequest feeRequest = TransactionFeeBatchRequest.builder()
                .transactionType(Pair.of(TransactionType.SWAP, address.get()))
                .tokenIdentifier(tokenId.getSymbol())
                .build();
        return transactionFeeBatch.apply(feeRequest)
            .thenApply(feeTotal -> {
                TransactionFee fee = new TransactionFee(tokenId.getSymbol(), feeTotal.getTotalFeeInteger());
                return swap(order1, order2, amount1, amount2, fee, null).join();
            });
    }

    /*
     * Order transaction
     */

    /**
     * Build order component of the swap transaction
     * 
     * @param recipient - Ethereum address of the receiver of the funds
     * @param sell - Token to sell
     * @param buy - Token to buy
     * @param ratio - Swap ratio
     * @param amount - Amount to swap
     * @param nonce - Nonce value
     * @param timeRange - Timerange of validity of the order
     * @return - Unsigned order object
     */
    public <T extends TokenId> CompletableFuture<Order> order(String recipient, T sell, T buy, Tuple2<BigInteger, BigInteger> ratio, BigInteger amount, Integer nonce, TimeRange timeRange) {
        return CompletableFuture.supplyAsync(() -> {
            Order order = Order.builder()
                .accountId(accountId.get().join())
                .amount(amount)
                .recipientAddress(recipient)
                .tokenSell(sell.getId())
                .tokenBuy(buy.getId())
                .ratio(ratio)
                .nonce(nonce == null ? this.nonce.get().join() : nonce)
                .timeRange(timeRange == null ? new TimeRange() : timeRange)
                .build();
            return order;
        });
    }

    /**
     * Build order component of the swap transaction (With default time range)
     * 
     * @param recipient - Ethereum address of the receiver of the funds
     * @param sell - Token to sell
     * @param buy - Token to buy
     * @param ratio - Swap ratio
     * @param amount - Amount to swap
     * @param nonce - Nonce value
     * @return - Unsigned order object
     */
    public <T extends TokenId> CompletableFuture<Order> order(String recipient, T sell, T buy, Tuple2<BigInteger, BigInteger> ratio, BigInteger amount, Integer nonce) {
        return order(recipient, sell, buy, ratio, amount, nonce, null);
    }

    /**
     * Build order component of the swap transaction (With default time range and load nonce from network)
     * 
     * @param recipient - Ethereum address of the receiver of the funds
     * @param sell - Token to sell
     * @param buy - Token to buy
     * @param ratio - Swap ratio
     * @param amount - Amount to swap
     * @return - Unsigned order object
     */
    public <T extends TokenId> CompletableFuture<Order> order(String recipient, T sell, T buy, Tuple2<BigInteger, BigInteger> ratio, BigInteger amount) {
        return order(recipient, sell, buy, ratio, amount, null, null);
    }

    /*
     * Limit Order transaction
     */

    /**
     * Build limit order component of the swap transaction
     * 
     * @param recipient - Ethereum address of the receiver of the funds
     * @param sell - Token to sell
     * @param buy - Token to buy
     * @param ratio - Swap ratio
     * @param nonce - Nonce value
     * @param timeRange - Timerange of validity of the order
     * @return - Unsigned order object
     */
    public <T extends TokenId> CompletableFuture<Order> limitOrder(String recipient, T sell, T buy, Tuple2<BigInteger, BigInteger> ratio, Integer nonce, TimeRange timeRange) {
        return order(recipient, sell, buy, ratio, BigInteger.ZERO, nonce, timeRange);
    }

    /**
     * Build order component of the swap transaction (With default time range)
     * 
     * @param recipient - Ethereum address of the receiver of the funds
     * @param sell - Token to sell
     * @param buy - Token to buy
     * @param ratio - Swap ratio
     * @param nonce - Nonce value
     * @return - Unsigned order object
     */
    public <T extends TokenId> CompletableFuture<Order> limitOrder(String recipient, T sell, T buy, Tuple2<BigInteger, BigInteger> ratio, Integer nonce) {
        return limitOrder(recipient, sell, buy, ratio, nonce, null);
    }

    /**
     * Build order component of the swap transaction (With default time range and load nonce from network)
     * 
     * @param recipient - Ethereum address of the receiver of the funds
     * @param sell - Token to sell
     * @param buy - Token to buy
     * @param ratio - Swap ratio
     * @return - Unsigned order object
     */
    public <T extends TokenId> CompletableFuture<Order> limitOrder(String recipient, T sell, T buy, Tuple2<BigInteger, BigInteger> ratio) {
        return limitOrder(recipient, sell, buy, ratio, null, null);
    }
    
}
