package stamp.app.tests;

import android.test.InstrumentationTestCase;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.Utils;
import com.google.bitcoin.crypto.DeterministicKey;
import com.google.bitcoin.crypto.HDKeyDerivation;
import com.google.bitcoin.params.MainNetParams;
import com.google.bitcoin.script.Script;
import stamp.app.MultiSigUtils;

import java.util.ArrayList;

public class MultiSigUtilsTest extends InstrumentationTestCase {

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
    private String tx = "010000000187ccb029c56b0602adbfed41a4cb60020e41aca6637325390224f13f63b972fd00000000255121039" +
            "1b674ec85dc26a19beb147c02662a4696cf18de37ee017588b65bb64e1e3d8751aeffffffff01102700000000000017a" +
            "914b378ab88141d6eaa3b684db25e4e318920e084f58700000000";
    private Transaction multiTX = null;
    private DeterministicKey dk;
    private byte[] redemptionScript;
}
