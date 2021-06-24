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

    public Token getToken(String tokenIdentifier) {
        final Token token = tokens.getTokenBySymbol(tokenIdentifier) != null ?
                tokens.getTokenBySymbol(tokenIdentifier) : tokens.getTokenByAddress(tokenIdentifier);

        return token;
    }

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

    public <A extends ChangePubKeyVariant> CompletableFuture<ChangePubKey<A>> changePubKey(String pubKeyHash, TransactionFee fee, Integer nonce) {
        return changePubKey(pubKeyHash, fee, nonce, null);
    }

    public <A extends ChangePubKeyVariant> CompletableFuture<ChangePubKey<A>> changePubKey(String pubKeyHash, TransactionFee fee) {
        return changePubKey(pubKeyHash, fee, null, null);
    }

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
                return this.<A>changePubKey(pubKeyHash, fee, null, null).join();
            });
    }

    /*
     * Transfer transaction
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

    public CompletableFuture<Transfer> transfer(String to, BigInteger amount, TransactionFee fee, Integer nonce) {
        return transfer(to, amount, fee, nonce, null);
    }

    public CompletableFuture<Transfer> transfer(String to, BigInteger amount, TransactionFee fee) {
        return transfer(to, amount, fee, null, null);
    }

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

    public CompletableFuture<Withdraw> withdraw(String to, BigInteger amount, TransactionFee fee, Integer nonce) {
        return withdraw(to, amount, fee, nonce, null);
    }

    public CompletableFuture<Withdraw> withdraw(String to, BigInteger amount, TransactionFee fee) {
        return withdraw(to, amount, fee, null, null);
    }

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

    public CompletableFuture<ForcedExit> forcedExit(String to, TransactionFee fee, Integer nonce) {
        return forcedExit(to, fee, nonce, null);
    }

    public CompletableFuture<ForcedExit> forcedExit(String to, TransactionFee fee) {
        return forcedExit(to, fee, null, null);
    }

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

    public CompletableFuture<MintNFT> mintNFT(String recipient, String contentHash, TransactionFee fee) {
        return mintNFT(recipient, contentHash, fee, null);
    }

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

    public CompletableFuture<WithdrawNFT> withdrawNFT(String to, NFT token, TransactionFee fee, Integer nonce) {
        return withdrawNFT(to, token, fee, nonce, null);
    }

    public CompletableFuture<WithdrawNFT> withdrawNFT(String to, NFT token, TransactionFee fee) {
        return withdrawNFT(to, token, fee, null, null);
    }

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

    public CompletableFuture<Tuple2<Transfer, Transfer>> transferNFT(String to, NFT token, TransactionFee fee, Integer nonce) {
        return transferNFT(to, token, fee, nonce, null);
    }

    public CompletableFuture<Tuple2<Transfer, Transfer>> transferNFT(String to, NFT token, TransactionFee fee) {
        return transferNFT(to, token, fee, null, null);
    }

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

    public CompletableFuture<Swap> swap(Order order1, Order order2, BigInteger amount1, BigInteger amount2, TransactionFee fee) {
        return swap(order1, order2, amount1, amount2, fee, null);
    }

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

    public CompletableFuture<Order> order(String recipient, Token sell, Token buy, Tuple2<BigInteger, BigInteger> ratio, BigInteger amount, Integer nonce, TimeRange timeRange) {
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

    public CompletableFuture<Order> order(String recipient, Token sell, Token buy, Tuple2<BigInteger, BigInteger> ratio, BigInteger amount, Integer nonce) {
        return order(recipient, sell, buy, ratio, amount, nonce, null);
    }

    public CompletableFuture<Order> order(String recipient, Token sell, Token buy, Tuple2<BigInteger, BigInteger> ratio, BigInteger amount) {
        return order(recipient, sell, buy, ratio, amount, null, null);
    }

    /*
     * Limit Order transaction
     */

    public CompletableFuture<Order> limitOrder(String recipient, Token sell, Token buy, Tuple2<BigInteger, BigInteger> ratio, Integer nonce, TimeRange timeRange) {
        return order(recipient, sell, buy, ratio, BigInteger.ZERO, nonce, timeRange);
    }

    public CompletableFuture<Order> limitOrder(String recipient, Token sell, Token buy, Tuple2<BigInteger, BigInteger> ratio, Integer nonce) {
        return limitOrder(recipient, sell, buy, ratio, nonce, null);
    }

    public CompletableFuture<Order> limitOrder(String recipient, Token sell, Token buy, Tuple2<BigInteger, BigInteger> ratio) {
        return limitOrder(recipient, sell, buy, ratio, null, null);
    }
    
}
