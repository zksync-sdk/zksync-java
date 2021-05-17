package io.zksync.wallet;

import org.junit.Before;
import org.junit.Test;

import io.zksync.domain.ChainId;
import io.zksync.domain.Signature;
import io.zksync.domain.TimeRange;
import io.zksync.domain.auth.ChangePubKeyOnchain;
import io.zksync.domain.fee.TransactionFee;
import io.zksync.domain.state.AccountState;
import io.zksync.domain.state.DepositingBalance;
import io.zksync.domain.state.DepositingState;
import io.zksync.domain.state.State;
import io.zksync.domain.token.NFT;
import io.zksync.domain.token.Token;
import io.zksync.domain.token.Tokens;
import io.zksync.domain.transaction.ChangePubKey;
import io.zksync.domain.transaction.ForcedExit;
import io.zksync.domain.transaction.MintNFT;
import io.zksync.domain.transaction.Transfer;
import io.zksync.domain.transaction.Withdraw;
import io.zksync.domain.transaction.WithdrawNFT;
import io.zksync.provider.Provider;
import io.zksync.signer.EthSignature;
import io.zksync.signer.EthSigner;
import io.zksync.signer.DefaultEthSigner;
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
        ethSigner = DefaultEthSigner.fromRawPrivateKey(ETH_PRIVATE_KEY);
        zkSigner = ZkSigner.fromEthSigner(ethSigner, ChainId.Mainnet);

        Provider provider = mock(Provider.class);
        when(provider.getState(anyString())).thenReturn(defaultAccountState(44));

        wallet = ZkSyncWallet.build(ethSigner, zkSigner, provider);
    }

    @Test
    public void testSetSigningKey() {
        Provider provider = mock(Provider.class);
        when(provider.getState(anyString())).thenReturn(defaultAccountState(55));
        ZkSyncWallet wallet = ZkSyncWallet.build(ethSigner, zkSigner, provider);
        Token token = defaultToken();

        when(provider.getTokens()).thenReturn(new Tokens(Collections.singletonMap(token.getAddress(), token)));
        when(provider.submitTx(defaultZkSyncTransaction_ChangePubKey(), null, false))
                .thenReturn("success:hash");

        String response = wallet.setSigningKey(defaultTransactionFee(1000000000), 13, true, new TimeRange(0, 4294967295L));

        assertNotNull(response);
        assertEquals(response, "success:hash");
    }

    @Test
    public void testSyncTransfer() {
        Provider provider = wallet.getProvider();
        Token token = defaultToken();
        EthSignature ethSignature = new EthSignature(SignatureType.EthereumSignature,
                "0x4684a8f03c5da84676ff4eae89984f20057ce288b3a072605cbf93ef4bcc8a021306b13a88c6d3adc68347f4b68b1cbdf967861005e934afa50ce2e0c5bced791b");
        when(provider.getTokens()).thenReturn(new Tokens(Collections.singletonMap(token.getAddress(), token)));
        when(provider.submitTx(defaultZkSyncTransaction_Transfer(), ethSignature, false)).thenReturn("success:hash");
        String response = wallet.syncTransfer("0x19aa2ed8712072e918632259780e587698ef58df",
                BigInteger.valueOf(1000000000000L), defaultTransactionFee(1000000), 12, new TimeRange(0, 4294967295L));
        assertNotNull(response);
        assertEquals(response, "success:hash");
    }

    @Test
    public void testSyncWithdraw() {
        Provider provider = wallet.getProvider();
        Token token = defaultToken();
        EthSignature ethSignature = new EthSignature(SignatureType.EthereumSignature, "0xa87d458c96f2b78c8b615c7703540d5af0c0b5266b12dbd648d8f6824958ed907f40cae683fa77e7a8a5780381cae30a94acf67f880ed30483c5a8480816fc9d1c");
        when(provider.getTokens()).thenReturn(new Tokens(Collections.singletonMap(token.getAddress(), token)));
        when(provider.submitTx(defaultZkSyncTransaction_Withdraw(), ethSignature, false)).thenReturn("success:hash");
        String response = wallet.syncWithdraw(
            "0x19aa2ed8712072e918632259780e587698ef58df",
            BigInteger.valueOf(1000000000000L),
            defaultTransactionFee(1000000),
            12,
            false,
            new TimeRange(0, 4294967295L)
        );
        assertNotNull(response);
        assertEquals(response, "success:hash");
    }

    @Test
    public void testSyncMintNFT() {
        Provider provider = wallet.getProvider();
        Token token = defaultToken();
        EthSignature ethSignature = new EthSignature(SignatureType.EthereumSignature, "0xac4f8b1ad65ea143dd2a940c72dd778ba3e07ee766355ed237a89a0b7e925fe76ead0a04e23db1cc1593399ee69faeb31b2e7e0c6fbec70d5061d6fbc431d64a1b");
        when(provider.getTokens()).thenReturn(new Tokens(Collections.singletonMap(token.getAddress(), token)));
        when(provider.submitTx(defaultZkSyncTransaction_MintNFT(), ethSignature, false)).thenReturn("success:hash");
        String response = wallet.syncMintNFT(
            "0x19aa2ed8712072e918632259780e587698ef58df",
            "0x0000000000000000000000000000000000000000000000000000000000000123",
            defaultTransactionFee(1000000),
            12
        );
        assertNotNull(response);
        assertEquals(response, "success:hash");
    }

    @Test
    public void testSyncWithdrawNFT() {
        Provider provider = wallet.getProvider();
        Token token = defaultToken();
        NFT nft = new NFT(100000, "NFT-100000", "0x19aa2ed8712072e918632259780e587698ef58df", "0x0000000000000000000000000000000000000000000000000000000000000123");
        EthSignature ethSignature = new EthSignature(SignatureType.EthereumSignature, "0x4a50341da6d2b1f0b64a4e37f753c02c43623e89cb0a291026c37fdcc723da9665453ce622f4dd6237bd98430ef0d75755694b1968f3b2d0ea8598f8bc43accf1b");
        when(provider.getTokens()).thenReturn(new Tokens(Collections.singletonMap(token.getAddress(), token)));
        when(provider.submitTx(defaultZkSyncTransaction_WithdrawNFT(), ethSignature, false)).thenReturn("success:hash");
        String response = wallet.syncWithdrawNFT(
            "0x19aa2ed8712072e918632259780e587698ef58df",
            nft,
            defaultTransactionFee(1000000),
            12,
            new TimeRange(0, 4294967295L)
        );
        assertNotNull(response);
        assertEquals(response, "success:hash");
    }

    @Test
    public void testSyncForceExit() {
        Provider provider = wallet.getProvider();
        Token token = defaultToken();
        EthSignature ethSignature = new EthSignature(SignatureType.EthereumSignature, "0x4db4eaa3ca3c1b750bc95361847c7dcda5bcc08644f5a80590c604d728f5a01f52bc767a15e8d6fc8293c3ac46f8fbb3ae4aa4bd3db7db1b0ec8959e63b1861e1c");
        when(provider.getTokens()).thenReturn(new Tokens(Collections.singletonMap(token.getAddress(), token)));
        when(provider.submitTx(defaultZkSyncTransaction_ForceExit(), ethSignature, false)).thenReturn("success:hash");
        String response = wallet.syncForcedExit(
            "0x19aa2ed8712072e918632259780e587698ef58df",
            defaultTransactionFee(1000000),
            12,
            new TimeRange(0, 4294967295L)
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
            Collections.singletonMap("ETH", BigInteger.TEN.toString()),
            Collections.emptyMap()
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

    private ChangePubKey<ChangePubKeyOnchain> defaultZkSyncTransaction_ChangePubKey() {
        ChangePubKey<ChangePubKeyOnchain> tx = new ChangePubKey<>(
            55,
            "0xede35562d3555e61120a151b3c8e8e91d83a378a",
            "sync:18e8446d7748f2de52b28345bdbc76160e6b35eb",
            0,
            "1000000000",
            13,
            Signature.builder()
                .pubKey("40771354dc314593e071eaf4d0f42ccb1fad6c7006c57464feeb7ab5872b7490")
                .signature("31a6be992eeb311623eb466a49d54cb1e5b3d44e7ccc27d55f82969fe04824aa92107fefa6b0a2d7a07581ace7f6366a5904176fae4aadec24d75d3d76028500")
                .build(),
            new ChangePubKeyOnchain(),
            new TimeRange(0, 4294967295L)
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
                .signature("50a9b498ffb54a24ba77fca2d9a72f4d906464d14c73c8f3b4a457e9149ba0885c6de37706ced49ae8401fb59000d4bcf9f37bcdaeab20a87476c3e08088b702")
                .build(),
            new TimeRange(0, 4294967295L)
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
                .signature("5c3304c8d1a8917580c9a3f8edb9d8698cbe9e6e084af93c13ac3564fa052588b93830785b3d0f60a1a193ec4fff61f81b95f0d16bf128ee21a6ceb09ef88602")
                .build(),
            null,
            new TimeRange(0, 4294967295L)
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
                .signature("3e2866bb00f892170cc3592d48aec7eb4afba75bdd0a530780fa1dcbdf857d07d75deb774142a93e3d1ca3be29e614e50892b95702b6461f86ddf78b9ab11a01")
                .build(),
            new TimeRange(0, 4294967295L)
        );
        return tx;
    }

    private MintNFT defaultZkSyncTransaction_MintNFT() {
        MintNFT tx = new MintNFT(
            44,
            "0xede35562d3555e61120a151b3c8e8e91d83a378a",
            "0x0000000000000000000000000000000000000000000000000000000000000123",
            "0x19aa2ed8712072e918632259780e587698ef58df",
            "1000000",
            0,
            12,
            Signature.builder()
                .pubKey("40771354dc314593e071eaf4d0f42ccb1fad6c7006c57464feeb7ab5872b7490")
                .signature("8c119b01ff8ae75ba5aabaa4ad480690e6a56d6e99d430ecac3bc3beacbaba28b3740cb20574d130281874fc70daaab884ee8e03a510e9ca9c1c677a2412cf03")
                .build()
        );
        return tx;
    }

    private WithdrawNFT defaultZkSyncTransaction_WithdrawNFT() {
        WithdrawNFT tx = new WithdrawNFT(
            44,
            "0xede35562d3555e61120a151b3c8e8e91d83a378a",
            "0x19aa2ed8712072e918632259780e587698ef58df",
            100000,
            0,
            "1000000",
            12,
            Signature.builder()
                .pubKey("40771354dc314593e071eaf4d0f42ccb1fad6c7006c57464feeb7ab5872b7490")
                .signature("9d94324425f23d09bf76df52e520e8da4561718057eb29fe6d760945be986b8e3a1955d9c02cf415558f533b7d9573564798db9586cc5ba1fdc44f711e455e03")
                .build(),
            new TimeRange(0, 4294967295L)
        );
        return tx;
    }
    
}
