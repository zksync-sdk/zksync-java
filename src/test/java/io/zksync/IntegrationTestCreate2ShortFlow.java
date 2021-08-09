package io.zksync;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;
import org.web3j.utils.Convert.Unit;

import io.zksync.domain.ChainId;
import io.zksync.domain.TransactionBuildHelper;
import io.zksync.domain.auth.ChangePubKeyCREATE2;
import io.zksync.domain.token.Token;
import io.zksync.domain.transaction.ChangePubKey;
import io.zksync.domain.transaction.TransactionDetails;
import io.zksync.domain.transaction.Transfer;
import io.zksync.signer.Create2EthSigner;
import io.zksync.signer.EthSignature;
import io.zksync.signer.ZkSigner;
import io.zksync.transport.ZkTransactionStatus;
import io.zksync.transport.receipt.ZkSyncPollingTransactionReceiptProcessor;
import io.zksync.transport.receipt.ZkSyncTransactionReceiptProcessor;
import io.zksync.wallet.SignedTransaction;
import io.zksync.wallet.ZkASyncWallet;
import io.zksync.provider.AsyncProvider;

@Ignore
public class IntegrationTestCreate2ShortFlow {

    private static final Token ETHEREUM_COIN = Token.createETH();

    private ZkASyncWallet wallet;

    private ChangePubKeyCREATE2 create2Data;
    private Create2EthSigner ethSigner;
    private ZkSigner zkSigner;

    private ZkSyncTransactionReceiptProcessor receiptProcessor;

    @Before
    public void setup() {
        Web3j web3j = Web3j.build(new HttpService("{{ethereum_web3_rpc_url}}"));
        zkSigner = ZkSigner.fromSeed(Numeric.toBytesPadded(BigInteger.ZERO, 32));
        create2Data = new ChangePubKeyCREATE2("0x5C69bEe701ef814a2B6a3EDD4B1652CB9cc5aA6f", Numeric.toHexStringWithPrefixZeroPadded(BigInteger.ZERO, 64), "0x96e8ac4277198ff8b6f785478aa9a39f403cb768dd02cbee326c3e7da348845f");
        ethSigner = Create2EthSigner.fromData(web3j, zkSigner, create2Data);

        wallet = ZkASyncWallet.build(ethSigner, zkSigner, AsyncProvider.defaultProvider(ChainId.Rinkeby));
        receiptProcessor = new ZkSyncPollingTransactionReceiptProcessor(wallet);
    }

    @Test
    public void setupPublicKey() throws InterruptedException, ExecutionException, TimeoutException {
        TransactionBuildHelper helper = new TransactionBuildHelper(this.wallet, this.wallet.getTokens().join());
        ChangePubKey<ChangePubKeyCREATE2> changePubKey = helper.changePubKey(zkSigner.getPublicKeyHash(), ETHEREUM_COIN, create2Data).join();
        changePubKey = ethSigner.signAuth(changePubKey).join();
        EthSignature ethSignature = ethSigner.signTransaction(changePubKey, changePubKey.getNonce(), ETHEREUM_COIN, changePubKey.getFeeInteger()).join();
        SignedTransaction<ChangePubKey<ChangePubKeyCREATE2>> transaction = new SignedTransaction<>(zkSigner.signChangePubKey(changePubKey), ethSignature);
        String hash = wallet.submitTransaction(transaction).join();

        System.out.println(hash);

        TransactionDetails receipt = receiptProcessor.waitForTransaction(hash, ZkTransactionStatus.COMMITED).get(30, TimeUnit.SECONDS);
        assertNotNull(receipt);
        assertTrue(receipt.getExecuted());
        assertTrue(receipt.getSuccess());
    }

    @Test
    public void transferFunds() throws InterruptedException, ExecutionException, TimeoutException {
        TransactionBuildHelper helper = new TransactionBuildHelper(this.wallet, this.wallet.getTokens().join());
        Transfer transfer = helper.transfer(ethSigner.getAddress(), Convert.toWei(BigDecimal.valueOf(100000), Unit.GWEI).toBigInteger(), ETHEREUM_COIN).join();
        EthSignature ethSignature = ethSigner.signTransaction(transfer, transfer.getNonce(), ETHEREUM_COIN, transfer.getFeeInteger()).join();
        SignedTransaction<Transfer> transaction = new SignedTransaction<>(zkSigner.signTransfer(transfer), ethSignature);
        String hash = wallet.submitTransaction(transaction).join();

        System.out.println(hash);

        TransactionDetails receipt = receiptProcessor.waitForTransaction(hash, ZkTransactionStatus.COMMITED).get(30, TimeUnit.SECONDS);
        assertNotNull(receipt);
        assertTrue(receipt.getExecuted());
        assertTrue(receipt.getSuccess());
    }
}
