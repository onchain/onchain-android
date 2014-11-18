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

        sigListJson = MultiSigUtils.signSignatureList(sigListJson, cwTX, child.toECKey());

        String tx = "010000000132448fc909b395d7f9e393d830c56b80bb0609d9977be173f1fdff03c515153900000000dd0048304402204b324d61d9fab51dc5d8775b5186c06cb1fc87629c2563315fe0bcaa007c12fb022067775db856f4b08893dc514ccacebc8521d67e2cd0d6be550b040df572a7399e0101493046022100f6309c26186de8fbc020df9e7aee13b90ce1b9f195f7638c36e36b079189c17e022100bd14a491fb4916dee84d73ae1ed867dd3e3fa80f1e2e6dc507d3cfcb90348f1e014c47522102fd89e243d38f4e24237eaac4cd3a6873ce45aa4036ec0c7b79a4d4ac0fefebc42102388085f9b6bbfb1c353b2664cf1857ff6d11c3f93b0635a31204bcbbb9e0403d52aeffffffff02a0860100000000001976a914ef7aeda00f357959ad628405753b41cfb778bde188ac0a5700000000000017a9143b04977278415be296acc65f83084341871ef9f08700000000";
        Transaction signedTX = new Transaction(MainNetParams.get(), Hex.decode(tx));

        // Run a TX through sign and see if it keeps all the sigs.
        Transaction afterTX = MultiSigUtils.signMultiSig(signedTX, new ECKey());

        String hex = Utils.bytesToHexString(afterTX.bitcoinSerialize());

        assertEquals(tx, hex);
    }

    public void testJustFuckingWork() throws Exception {
        String tx = "010000000268b24b5bfbaca07b471b967bdc737a1bf85670c72e5cc2db2fe3cbdae1da900c01000000c9524104f38a0124afe10f06cad3d4cbf9159f63443a63d4219d9316a411901348b4ccff517a812ba2578ef97bf8d0cd1a18d5f1de0a697529186c26e51ffb895a1c9e51410498ef09c13a496507999e6b08cbebc059f4751c94929388108e421c93bf7520216eabdfca6216b579e48c7a830e09e7343a277e59236be72e920a5a9bd021d2ae410476c3b254aec505f7aefa5ba172d85f4df6a03bba905a89775dadee5a07e283f9035d13572f8a345b66052111b20c75a106750bcac946f3c24a3355ba9e65e94453aeffffffffdefa8d2c540fc2f0de38ba3833da5ba3970b7ed815afeccff1dd2efeeb41ca5100000000c9524104f38a0124afe10f06cad3d4cbf9159f63443a63d4219d9316a411901348b4ccff517a812ba2578ef97bf8d0cd1a18d5f1de0a697529186c26e51ffb895a1c9e51410498ef09c13a496507999e6b08cbebc059f4751c94929388108e421c93bf7520216eabdfca6216b579e48c7a830e09e7343a277e59236be72e920a5a9bd021d2ae410476c3b254aec505f7aefa5ba172d85f4df6a03bba905a89775dadee5a07e283f9035d13572f8a345b66052111b20c75a106750bcac946f3c24a3355ba9e65e94453aeffffffff02c0d40100000000001976a9141f2fbe5334b1d291a0e0e03cdb3546516742dd5588ac580a00000000000017a9140dd0db5bc8c7158da03c65c34e39e3b54f0f89cf8700000000";

        String sigList = "[{\"04f38a0124afe10f06cad3d4cbf9159f63443a63d4219d9316a411901348b4ccff517a812ba2578ef97bf8d0cd1a18d5f1de0a697529186c26e51ffb895a1c9e51\":{\"hash\":\"0ec3270b78f1ba0118d198288cc2d26f003922ff2e38e0594386f2c1b2e8f787\"},\"0498ef09c13a496507999e6b08cbebc059f4751c94929388108e421c93bf7520216eabdfca6216b579e48c7a830e09e7343a277e59236be72e920a5a9bd021d2ae\":{\"hash\":\"0ec3270b78f1ba0118d198288cc2d26f003922ff2e38e0594386f2c1b2e8f787\"},\"0476c3b254aec505f7aefa5ba172d85f4df6a03bba905a89775dadee5a07e283f9035d13572f8a345b66052111b20c75a106750bcac946f3c24a3355ba9e65e944\":{\"hash\":\"0ec3270b78f1ba0118d198288cc2d26f003922ff2e38e0594386f2c1b2e8f787\"}},{\"04f38a0124afe10f06cad3d4cbf9159f63443a63d4219d9316a411901348b4ccff517a812ba2578ef97bf8d0cd1a18d5f1de0a697529186c26e51ffb895a1c9e51\":{\"hash\":\"985f495b1e9b9670bc77d25a2f7a47eb4d7dae3f4d2aa4637294791eda05d98d\"},\"0498ef09c13a496507999e6b08cbebc059f4751c94929388108e421c93bf7520216eabdfca6216b579e48c7a830e09e7343a277e59236be72e920a5a9bd021d2ae\":{\"hash\":\"985f495b1e9b9670bc77d25a2f7a47eb4d7dae3f4d2aa4637294791eda05d98d\"},\"0476c3b254aec505f7aefa5ba172d85f4df6a03bba905a89775dadee5a07e283f9035d13572f8a345b66052111b20c75a106750bcac946f3c24a3355ba9e65e944\":{\"hash\":\"985f495b1e9b9670bc77d25a2f7a47eb4d7dae3f4d2aa4637294791eda05d98d\"}}]";


        String privkeya = "5KAovUBbq3uBUQBPPr6RABJVnh4fy6E49dbQjqhwE8HEoCDTA19";
        ECKey key1 = new DumpedPrivateKey(MainNetParams.get(), privkeya).getKey();

        assertEquals(key1.toAddress(MainNetParams.get()).toString(), "1MWr2FY4XLfEzZ7PQPELNwFkog83vwh6a1");

        String privkeyb = "5JefEur75YYjxHJjmJDaTRAL8hY8GWvLxTwHn11HZQWwcySKfrn";
        ECKey key2 = new DumpedPrivateKey(MainNetParams.get(), privkeyb).getKey();

        assertEquals(key2.toAddress(MainNetParams.get()).toString(), "1CZn88sLyLNe6zwJsPLYkj9DTsHXVWi3TU");

        Transaction cwTX = new Transaction(MainNetParams.get(), Hex.decode(tx));
        sigList = MultiSigUtils.signSignatureList(sigList, cwTX, key1);
        sigList = MultiSigUtils.signSignatureList(sigList, cwTX, key2);
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
