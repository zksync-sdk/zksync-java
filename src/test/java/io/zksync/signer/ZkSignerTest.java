package io.zksync.signer;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.Test;
import org.web3j.utils.Numeric;

import io.zksync.domain.ChainId;
import io.zksync.domain.Signature;

public class ZkSignerTest {

    private static final String PRIVATE_KEY = "0x000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f";
    private static final byte[] SEED = Numeric.hexStringToByteArray(PRIVATE_KEY);
    private static final byte[] MESSAGE = Numeric.hexStringToByteArray("0x000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f202122232425262728292a2b2c2d2e2f303132333435363738393a3b3c3d3e3f");
    private static final String PUBKEY = "0x17f3708f5e2b2c39c640def0cf0010fd9dd9219650e389114ea9da47f5874184";
    private static final String PUBKEY_HASH = "sync:4f3015a1d2b93239f9510d8bc2cf49376a78a08e";
    private static final String PUBKEY_HASH_ETH = "sync:18e8446d7748f2de52b28345bdbc76160e6b35eb";
    private static final String PUBKEY_HASH_RAW = "sync:45ef0ae7362eb021ae2d9ac251a3ee434f37ed73";
    private static final String SIGNATURE = "5462c3083d92b832d540c9068eed0a0450520f6dd2e4ab169de1a46585b394a4292896a2ebca3c0378378963a6bc1710b64c573598e73de3a33d6cec2f5d7403";

    @Test
    public void testCreationFromSeed() {
        ZkSigner signer = ZkSigner.fromSeed(SEED);
        assertEquals(signer.getPublicKey(), PUBKEY);
    }

    @Test
    public void testCreationFromEthSigner() {
        DefaultEthSigner ethSigner = DefaultEthSigner.fromRawPrivateKey(PRIVATE_KEY);
        ZkSigner signer = ZkSigner.fromEthSigner(ethSigner, ChainId.Mainnet);

        assertEquals(signer.getPublicKeyHash(), PUBKEY_HASH_ETH);
    }

    @Test
    public void testCreationFromRawPrivateKey() throws Exception {
        ZkSigner signer = ZkSigner.fromRawPrivateKey(Numeric.hexStringToByteArray(PRIVATE_KEY));

        assertEquals(signer.getPublicKeyHash(), PUBKEY_HASH_RAW);
    }

    @Test
    public void testSigningMessage() {
        ZkSigner signer = ZkSigner.fromSeed(SEED);
        Signature sign = signer.sign(MESSAGE);

        assertEquals(sign.getSignature(), SIGNATURE);
    }

    @Test
    public void testPublicKeyGeneration() {
        ZkSigner signer = ZkSigner.fromSeed(SEED);
        String publicKey = signer.getPublicKey();

        assertEquals(publicKey, PUBKEY);
    }

    @Test
    public void testPublicKeyHashGeneration() {
        ZkSigner signer = ZkSigner.fromSeed(SEED);
        String pubKeyHash = signer.getPublicKeyHash();

        assertEquals(pubKeyHash, PUBKEY_HASH);
    }
    
}
