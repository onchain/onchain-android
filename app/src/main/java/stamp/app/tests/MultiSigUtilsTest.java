package stamp.app.tests;

import android.test.InstrumentationTestCase;
import com.google.bitcoin.core.DumpedPrivateKey;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.Utils;
import com.google.bitcoin.crypto.DeterministicKey;
import com.google.bitcoin.crypto.HDKeyDerivation;
import com.google.bitcoin.crypto.MnemonicCode;
import com.google.bitcoin.params.MainNetParams;
import com.google.bitcoin.script.Script;
import org.spongycastle.util.encoders.Hex;
import stamp.app.MultiSigUtils;

import java.util.ArrayList;
import java.util.Arrays;

public class MultiSigUtilsTest extends InstrumentationTestCase {

    public void testCorrectKeys() throws Exception {
        assertEquals(dk.serializePubB58(), "xpub661MyMwAqRbcFhCvdhTAfpEEDV58oqDvv65YNHC686NNs4KbH8YZQJWVmrfbve7aAVHzxw8bKFxA7MLeDK6BbLfkE3bqkvHLPgaGHHtYGeY");
    }

    public void testWhatsMyKey2() throws Exception {

        String[] seed = "short kiwi mixed tunnel prosper marine mechanic nest fossil tag range north trigger twist hidden enlist sauce tiger wagon cat south bottom virus load".split(" ");
        MnemonicCode mc = new MnemonicCode();
        byte[] rnd = mc.toEntropy(Arrays.asList(seed));

        DeterministicKey dk = HDKeyDerivation.createMasterPrivateKey(rnd);
        DeterministicKey child = HDKeyDerivation.deriveChildKey(dk, 62235);

        assertEquals(child.serializePrivB58(), "hello);");
    }

    public void testSignMultiSig() throws Exception {

        ECKey privateKey = HDKeyDerivation.deriveChildKey(dk, 2).toECKey();

        Transaction txRaw = MultiSigUtils.signMultiSig(multiTX, privateKey);

        String txString = Utils.bytesToHexString(txRaw.bitcoinSerialize());

        assertFalse(txString.equals(tx));
    }

    public void testDeriveKeysByPaths() throws Exception {

        ArrayList<ECKey> keys = MultiSigUtils.deriveKeysFromPath("m/2", dk);

        assertEquals(keys.size(), 1);

        DeterministicKey node = HDKeyDerivation.deriveChildKey(dk, 2);

        assertEquals(node.toECKey().toString(), keys.get(0).toString());
    }

    public void testSignTXByPaths() throws Exception {

        ECKey privateKey = HDKeyDerivation.deriveChildKey(dk, 2).toECKey();

        Transaction txRaw = MultiSigUtils.signMultiSig(multiTX, privateKey);

        Transaction txRaw2 = MultiSigUtils.signMultiSigFromPath(multiTX, dk, "m/2");

        assertEquals(txRaw.getHashAsString(), txRaw2.getHashAsString());

        String txString = Utils.bytesToHexString(txRaw.bitcoinSerialize());
    }

    public void testSignPassPhraseTXPaths() throws Exception {

        Transaction cwTX = new Transaction(MainNetParams.get(), Hex.decode(tx2));

        String cwTXString = Utils.bytesToHexString(cwTX.bitcoinSerialize());
        assertEquals(cwTXString, tx2);

        String privkey = "5JFZuDkLgbEXK4CUEiXyyz4fUqzAsQ5QUqufdJy8MoLA9S1RdNX";
        ECKey handyKey = new DumpedPrivateKey(MainNetParams.get(), privkey).getKey();


        String addr = "11o51X3ciSjoLWFN3sbg3yzCM8RSuD2q9";
        assertEquals(addr, handyKey.toAddress(MainNetParams.get()).toString());

        // Sign and verify.
        Transaction txRaw = MultiSigUtils.signMultiSig(cwTX, handyKey);
        String txString = Utils.bytesToHexString(txRaw.bitcoinSerialize());
        assertEquals(txString, tx2);
    }

    public static void testSignSignatureList() throws Exception {

        String[] seed = "short kiwi mixed tunnel prosper marine mechanic nest fossil tag range north trigger twist hidden enlist sauce tiger wagon cat south bottom virus load".split(" ");
        MnemonicCode mc = new MnemonicCode();
        byte[] rnd = mc.toEntropy(Arrays.asList(seed));

        DeterministicKey dk = HDKeyDerivation.createMasterPrivateKey(rnd);
        DeterministicKey child = HDKeyDerivation.deriveChildKey(dk, MultiSigUtils.crc16("onchain.io"));

        Transaction cwTX = new Transaction(MainNetParams.get(), Hex.decode(sigListTX));

        sigListJson = MultiSigUtils.signSignatureList(sigListJson, cwTX, child);

        throw new Exception(sigListJson);

    }

    @Override
    protected void setUp() throws Exception {

        // Free Bitcoins !!! I sent this address some money :)
        // All zeros generates address 3J3ySBaH15W66GCBRZPY2XyQLtwYQqtQSJ
        byte[] rndBytes = new byte[32];

        dk = HDKeyDerivation.createMasterPrivateKey(rndBytes);

        // Create a redemption script based on our 1 of 1 multi sig.
        DeterministicKey node = HDKeyDerivation.deriveChildKey(dk, 2);
        ArrayList<ECKey> keys = new ArrayList<ECKey>();
        keys.add(node.toECKey());
        redemptionScript = Script.createMultiSigOutputScript(1, keys);
        Script s = new Script(redemptionScript);

        multiTX = new Transaction(MainNetParams.get(), Utils.parseAsHexOrBase58(tx));
    }


    // TX created with https://rawgit.com/OutCast3k/bitcoin-multisig/master/index.html
    private String tx1 = "010000000187ccb029c56b0602adbfed41a4cb60020e41aca6637325390224f13f63b972fd00000000" +
            "2551210391b674ec85dc26a19beb147c02662a4696cf18de37ee017588b65bb64e1e3d8751aeffffffff01102700000" +
            "000000017a914b378ab88141d6eaa3b684db25e4e318920e084f58700000000";

    // TX generated by a JSfiddle.
    private String tx = "010000000187ccb029c56b0602adbfed41a4cb60020e41aca6637325390224f13f63b972fd000000002" +
            "551210391b674ec85dc26a19beb147c02662a4696cf18de37ee017588b65bb64e1e3d8751aeffffffff01409c000000" +
            "00000017a914a7cd6fbb008d8de20be48f932dca9a4ccce357c08700000000";

    // TX generated on carbon wallet unit tests
    private String tx2 = "01000000010e202bd73c787fbaaad3090c14810747c705c5ae5cf64d13f4e94a8a0a64a66700000000" +
            "9200483045022015d6180c4b4ec67528a92af9c4c67e18caaecfddf39fad12470927352ac76345022100c20847a2269" +
            "e639a2c8bc4be6252ad93bfc92d5ad8295a161b7369b5f160130801475221036c4b74c7fd3bc6d3f7330854e3001524" +
            "33c23765c5368780c2a3eaee099d526921035b26a3467cc261f288338ffeec343054c410f8526b1f0a645405b429afd" +
            "87e2352aeffffffff0210270000000000001976a91404d075b3f501deeef5565143282b6cfe8fad5e9488ac80380100" +
            "0000000017a914381cf81ebd8af84ce9c0539c72b6b6aa00ee31fe8700000000";

    private static String sigListJson =  "[{\"02fd89e243d38f4e24237eaac4cd3a6873ce45aa4036ec0c7b79a4d4ac0fefebc4\":{\"hash\":\"fddf486c62c0c89eb9d8bd054e6bffb504fdf70239e315074676d9f65c49bd1b\"},\"0396e42d3c584da0300ee44dcbaee0eccaa0e6ae2264fdd2554af6d2953f95bf99\":{\"hash\":\"fddf486c62c0c89eb9d8bd054e6bffb504fdf70239e315074676d9f65c49bd1b\",\"sig\":\"304502200a2bff6a4da53e3376c36d943dc9d43addc18b667f5892411e55ccaea8b3b779022100f4f2a1c121e75cd80137b2c38fdf90f4da634ab6c159005d530eb9cfe3e93f60\"}},{\"02fd89e243d38f4e24237eaac4cd3a6873ce45aa4036ec0c7b79a4d4ac0fefebc4\":{\"hash\":\"2fb8960eecf2fe2a9268953cb564e27b669e570fc39c7b100f9706e6339ffdf5\"},\"034000cea8f9cbaf88095d3ef539ee438e3cefea9ed9585e2e182b45496f071a83\":{\"hash\":\"2fb8960eecf2fe2a9268953cb564e27b669e570fc39c7b100f9706e6339ffdf5\",\"sig\":\"3044022079dda685df2d0294d076b52b42fc298d2dc7a1300b93bec3216470bdc2619af2022028f2c97aa412933d515e82596b45b57c33463be63812cb60acad981f9505a021\"}}]";

    private static String sigListTX = "01000000029fd77c01b4f81f142e7e066eb9abeb4952ec5fdea51036acbb22b5ffeb57fd5f0100000047522102fd89e243d38f4e24237eaac4cd3a6873ce45aa4036ec0c7b79a4d4ac0fefebc4210396e42d3c584da0300ee44dcbaee0eccaa0e6ae2264fdd2554af6d2953f95bf9952aeffffffffc0161c6d62ac75f36bf95fcb2a8222f2274e86c2dcaec3434a0b6b6e0a6b60800000000047522102fd89e243d38f4e24237eaac4cd3a6873ce45aa4036ec0c7b79a4d4ac0fefebc421034000cea8f9cbaf88095d3ef539ee438e3cefea9ed9585e2e182b45496f071a8352aeffffffff0156050000000000001976a91404d075b3f501deeef5565143282b6cfe8fad5e9488ac00000000";
    private Transaction multiTX = null;
    private DeterministicKey dk;
    private byte[] redemptionScript;
}
