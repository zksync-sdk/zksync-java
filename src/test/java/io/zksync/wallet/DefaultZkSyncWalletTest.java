package io.zksync.wallet;

import org.junit.Before;
import org.junit.Test;

import io.zksync.domain.ChainId;
import io.zksync.domain.Signature;
import io.zksync.domain.fee.TransactionFee;
import io.zksync.domain.state.AccountState;
import io.zksync.domain.state.DepositingBalance;
import io.zksync.domain.state.DepositingState;
import io.zksync.domain.state.State;
import io.zksync.domain.token.Token;
import io.zksync.domain.token.Tokens;
import io.zksync.domain.transaction.ChangePubKey;
import io.zksync.domain.transaction.ForcedExit;
import io.zksync.domain.transaction.Transfer;
import io.zksync.domain.transaction.Withdraw;
import io.zksync.provider.Provider;
import io.zksync.signer.EthSignature;
import io.zksync.signer.EthSigner;
import io.zksync.signer.ZkSigner;
import io.zksync.signer.EthSignature.SignatureType;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigInteger;
import java.util.Collections;

public class DefaultZkSyncWalletTest {

    private static final String ETH_PRIVATE_KEY = "0x000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f";


    private ZkSyncWallet wallet;
    private EthSigner ethSigner;
    private ZkSigner zkSigner;

    @Before
    public void setUp() {
        ethSigner = EthSigner.fromRawPrivateKey(ETH_PRIVATE_KEY);
        zkSigner = ZkSigner.fromEthSigner(ethSigner, ChainId.Mainnet);

        Provider provider = mock(Provider.class);
        when(provider.getState(anyString())).thenReturn(defaultAccountState(44));

        wallet = ZkSyncWallet
                .build(ethSigner, zkSigner, provider);
    }

    @Test
    public void testSetSigningKey() {
        Provider provider = mock(Provider.class);
        when(provider.getState(anyString())).thenReturn(defaultAccountState(55));
        ZkSyncWallet wallet = ZkSyncWallet
                .build(ethSigner, zkSigner, provider);
        Token token = defaultToken();
        EthSignature ethSignature = new EthSignature(SignatureType.EthereumSignature, "0xe062aca0dd8438174f424a26f3dd528ca9bd98366b2dafd6c6735eeaccd9e787245ac7dbbe2a37e3a74f168e723c5a2c613de25795a056bc81ff4c8d4106e56f1c");
        
        when(provider.getTokens()).thenReturn(new Tokens(Collections.singletonMap(token.getAddress(), token)));
        when(provider.submitTx(defaultZkSyncTransaction_ChangePubKey(), ethSignature, false)).thenReturn("success:hash");
        
        String response = wallet.setSigningKey(defaultTransactionFee(1000000000), 13, false);
        
        assertNotNull(response);
        assertEquals(response, "success:hash");
    }

    @Test
    public void testSyncTransfer() {
        Provider provider = wallet.getProvider();
        Token token = defaultToken();
        EthSignature ethSignature = new EthSignature(SignatureType.EthereumSignature, "0x6f7e631024b648e8d3984f84aa14d4f1b1013191042ef51b6443e3f25b075a0346988ab824687041ce699a91ed6e20bedff7c730aac3d8c7a111dd408c1862e41c");
        when(provider.getTokens()).thenReturn(new Tokens(Collections.singletonMap(token.getAddress(), token)));
        when(provider.submitTx(defaultZkSyncTransaction_Transfer(), ethSignature, false)).thenReturn("success:hash");
        String response = wallet.syncTransfer(
            "0x19aa2ed8712072e918632259780e587698ef58df",
            BigInteger.valueOf(1000000000000L),
            defaultTransactionFee(1000000),
            12
        );
        assertNotNull(response);
        assertEquals(response, "success:hash");
    }

    @Test
    public void testSyncWithdraw() {
        Provider provider = wallet.getProvider();
        Token token = defaultToken();
        EthSignature ethSignature = new EthSignature(SignatureType.EthereumSignature, "0xaa6ea9d9b06457c2652f80707b7ab35ba3b5b4ef593624773d00660dd5f9174215b327be358c9bd2ae539ae5220d47033d252506119a46cd898b42ae2bb366891c");
        when(provider.getTokens()).thenReturn(new Tokens(Collections.singletonMap(token.getAddress(), token)));
        when(provider.submitTx(defaultZkSyncTransaction_Withdraw(), ethSignature, false)).thenReturn("success:hash");
        String response = wallet.syncWithdraw(
            "0x19aa2ed8712072e918632259780e587698ef58df",
            BigInteger.valueOf(1000000000000L),
            defaultTransactionFee(1000000),
            12,
            false
        );
        assertNotNull(response);
        assertEquals(response, "success:hash");
    }

    @Test
    public void testSyncForceExit() {
        Provider provider = wallet.getProvider();
        Token token = defaultToken();
        when(provider.getTokens()).thenReturn(new Tokens(Collections.singletonMap(token.getAddress(), token)));
        when(provider.submitTx(defaultZkSyncTransaction_ForceExit(), null, false)).thenReturn("success:hash");
        String response = wallet.syncForcedExit(
            "0x19aa2ed8712072e918632259780e587698ef58df",
            defaultTransactionFee(1000000),
            12
        );
        assertNotNull(response);
        assertEquals(response, "success:hash");
    }

    @Test
    public void testGetState() {
        Provider provider = spy(wallet.getProvider());
        when(provider.getState(anyString())).thenReturn(defaultAccountState(44));
        assertEquals(wallet.getState(), defaultAccountState(44));
    }

    private AccountState defaultAccountState(Integer accountId) {
        AccountState account = new AccountState(
            "0xede35562d3555e61120a151b3c8e8e91d83a378a",
            accountId,
            defaultDepositingState(),
            defaultState(),
            defaultState()
        );
        return account;
    }

    private DepositingState defaultDepositingState() {
        DepositingState depositing = new DepositingState(
            Collections.singletonMap("ETH", new DepositingBalance(BigInteger.TEN.toString(), BigInteger.valueOf(12345)))
        );
        return depositing;
    }

    private State defaultState() {
        State state = new State(
            Integer.MAX_VALUE,
            "17f3708f5e2b2c39c640def0cf0010fd9dd9219650e389114ea9da47f5874184",
            Collections.singletonMap("ETH", BigInteger.TEN.toString())
        );
        return state;
    }

    private Token defaultToken() {
        Token token = new Token(
            0,
            "0x0000000000000000000000000000000000000000",
            "ETH",
            0 // INFO: only for testing purpose. 1:1
        );
        return token;
    }

    private TransactionFee defaultTransactionFee(long amount) {
        TransactionFee fee = new TransactionFee(
            "0x0000000000000000000000000000000000000000",
            BigInteger.valueOf(amount)
        );
        return fee;
    }

    private ChangePubKey defaultZkSyncTransaction_ChangePubKey() {
        ChangePubKey tx = new ChangePubKey(
            55,
            "0xede35562d3555e61120a151b3c8e8e91d83a378a",
            "sync:18e8446d7748f2de52b28345bdbc76160e6b35eb",
            0,
            "1000000000",
            13,
            Signature.builder()
                .pubKey("40771354dc314593e071eaf4d0f42ccb1fad6c7006c57464feeb7ab5872b7490")
                .signature("74a6c035f3471b3e441ff60b792d4ea74f69acb1d91682fc657594d5d1add50beb9e7450d7f782ecadfe45ad872c5e7a8da4ad3dadcd58a08534df45f1617e05")
                .build(),
            "0xe062aca0dd8438174f424a26f3dd528ca9bd98366b2dafd6c6735eeaccd9e787245ac7dbbe2a37e3a74f168e723c5a2c613de25795a056bc81ff4c8d4106e56f1c"
        );
        return tx;
    }

    private ForcedExit defaultZkSyncTransaction_ForceExit() {
        ForcedExit tx = new ForcedExit(
            44,
            "0x19aa2ed8712072e918632259780e587698ef58df",
            0,
            "1000000",
            12,
            Signature.builder()
                .pubKey("40771354dc314593e071eaf4d0f42ccb1fad6c7006c57464feeb7ab5872b7490")
                .signature("62fa1d2f56e1d9a422fbf689cc27ff9da6a33ee9add6d47d4afdf4657b0a93146da6f720e06e2b3894b7ab645eb07d3cd1576710157848f5cb04809e0e2f3a04")
                .build()
        );
        return tx;
    }

    private Transfer defaultZkSyncTransaction_Transfer() {
        Transfer tx = new Transfer(
            44,
            "0xede35562d3555e61120a151b3c8e8e91d83a378a",
            "0x19aa2ed8712072e918632259780e587698ef58df",
            0,
            BigInteger.valueOf(1000000000000L),
            "1000000",
            12,
            Signature.builder()
                .pubKey("40771354dc314593e071eaf4d0f42ccb1fad6c7006c57464feeb7ab5872b7490")
                .signature("3025a85f9f953f67f7f60cdc97fc77496afb5c83ed622eff27dc5f2a51d8f189496df563aa3eb1274b2254649a4c572f920b8b90105d0eb14ea16a4e789ed303")
                .build()
        );
        return tx;
    }

    private Withdraw defaultZkSyncTransaction_Withdraw() {
        Withdraw tx = new Withdraw(
            44,
            "0xede35562d3555e61120a151b3c8e8e91d83a378a",
            "0x19aa2ed8712072e918632259780e587698ef58df",
            0,
            BigInteger.valueOf(1000000000000L),
            "1000000",
            12,
            Signature.builder()
                .pubKey("40771354dc314593e071eaf4d0f42ccb1fad6c7006c57464feeb7ab5872b7490")
                .signature("f1837e30472ae13f7ddd09163de86e73e864e068bc252f83bed2e17f1671b90b276a6008ba5d25a65f8c81236982d47512adaef8bb0da922ffa226957a939b02")
                .build()
        );
        return tx;
    }
    
}
