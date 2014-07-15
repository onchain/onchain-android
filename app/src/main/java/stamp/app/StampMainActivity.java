package stamp.app;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceActivity;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.Utils;
import com.google.bitcoin.crypto.DeterministicHierarchy;
import com.google.bitcoin.crypto.DeterministicKey;
import com.google.bitcoin.crypto.HDKeyDerivation;
import com.google.bitcoin.crypto.MnemonicCode;
import com.google.bitcoin.crypto.MnemonicException;
import com.google.bitcoin.params.MainNetParams;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.apache.http.Header;
import org.spongycastle.util.encoders.Hex;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class StampMainActivity extends ActionBarActivity implements View.OnClickListener {

    Button scanButton;

    static final int SCAN_QR_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stamp_main);

        scanButton = (Button) findViewById(R.id.scan_button);
        scanButton.setOnClickListener(this);
    }

    private String getWalletSeed() {
        String walletSeed = getSharedPreferences("bip39", MODE_PRIVATE)
                .getString("wallet-seed", "");

        if(walletSeed.equals("")) {

            SecureRandom sr = new SecureRandom();
            byte[] rndBytes = new byte[32];
            sr.nextBytes(rndBytes);

            try {
                MnemonicCode mc = new MnemonicCode();
                List<String> mn = mc.toMnemonic(rndBytes);
                StringBuilder seed = new StringBuilder();
                for(String s : mn) {
                    seed.append(s);
                    seed.append(" ");
                }

                walletSeed = seed.toString().trim();

                SharedPreferences.Editor e = getSharedPreferences("bip39", MODE_PRIVATE).edit();
                e.putString("wallet-seed", seed.toString().trim());
                e.commit();
            } catch(Exception e) {
                throw new RuntimeException(e);
            }
        }
        return walletSeed;
    }

    private void setWalletSeed(String seed) {
        SharedPreferences.Editor e = getSharedPreferences("bip39", MODE_PRIVATE).edit();
        e.putString("wallet-seed", seed.toString().trim());
        e.commit();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.stamp_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_showseed) {
            new AlertDialog.Builder(this).setTitle(R.string.action_showseed)
                .setMessage(getWalletSeed()).setNeutralButton("Close", null).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View view) {
        Intent intent = new Intent(this, ScanQRCodeActivity.class);
        startActivityForResult(intent, SCAN_QR_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == SCAN_QR_CODE) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {

                Toast toast = Toast.makeText(getApplicationContext(),
                        data.getStringExtra("result"), Toast.LENGTH_SHORT);
                toast.show();

                // Do something with the contact here (bigger example below)
                processQRRequest(data.getStringExtra("result"));
            }
        }
    }

    private void processQRRequest(String data) {

        try {

            String[] params = data.split("\\|");
            if(params.length < 3) {
                // It's not a command, perhaps it's BIP39 seed.
                params = data.split(" ");
                if(params.length == 24)
                    processNewWalletSeed(data);
                return;
            }


            String cmd = params[0];
            String service = params[1];
            String post_back = params[2];

            int crc = crc16(service);

            Log.w("INFO", post_back);

            if(cmd.equals("mpk")) {

                processMPKRequest(params, post_back, crc);

            } else if(cmd.equals("sign")) {

                // Sign a TX.
                processSignRequest(params, post_back, crc);
            } else if(cmd.equals("pubkey")) {

                // Sign a TX.
                processPubKeyRequest(params, post_back, crc);
            }
        }
        catch(Exception e) {

            Toast toast = Toast.makeText(getApplicationContext(),
                    e.toString(), Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    private void processNewWalletSeed(final String data) {

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        setWalletSeed(data);
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(data).setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener)
                .setTitle("Do you want to change your BIP39 seed ?").show();

    }

    private void processSignRequest(final String[] params, final String post_back, int index)
        throws Exception {

        final DeterministicKey ekprv = getHDWalletDeterministicKey(index);

        final AsyncHttpClient client = new AsyncHttpClient();
        final RequestParams rp = new RequestParams();

        for(int i = 3; (i + 1) < params.length; i+= 2) {
            rp.put(params[i], params[i + 1]);
        }


        Log.w("INFO", rp.toString());

        client.get(post_back, rp, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(String response) {

                Log.w("INFO", response);

                Transaction tx = new Transaction(MainNetParams.get(), Hex.decode(response));

                if(tx.getOutputs().size() == 0) {

                    String txShort = response.substring(0, 40);
                    Toast toast = Toast.makeText(getApplicationContext(),
                            "Invalid TX ? (" + txShort + ")", Toast.LENGTH_SHORT);
                    toast.show();
                    return;
                }

                try {

                    tx = MultiSigUtils.signMultiSig(tx, ekprv.toECKey());

                    rp.put("tx", Utils.bytesToHexString(tx.bitcoinSerialize()));

                    // POST it back.

                    client.post(post_back, rp, new AsyncHttpResponseHandler() {
                        @Override
                        public void onSuccess(String response) {

                            Toast toast = Toast.makeText(getApplicationContext(),
                                    response, Toast.LENGTH_SHORT);
                            toast.show();


                            Log.w("INFO", "SUCCESS " + response);
                        }
                        @Override
                        public void onFailure(int statusCode, Header[] headers,
                                              byte[] responseBody, Throwable error) {

                            Toast toast = Toast.makeText(getApplicationContext(),
                                    "" + statusCode, Toast.LENGTH_SHORT);
                            toast.show();


                            Log.w("INFO", "FAILURE " + statusCode);
                        }
                    });

                } catch (Exception e) {
                    Log.w("ERROR", e.getMessage());

                    Toast toast = Toast.makeText(getApplicationContext(),
                            e.getMessage(), Toast.LENGTH_SHORT);
                    toast.show();
                    return;
                }

                Toast toast = Toast.makeText(getApplicationContext(),
                        tx.getOutputs().get(0).toString(), Toast.LENGTH_SHORT);
                toast.show();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers,
                                  byte[] responseBody, Throwable error) {

                Toast toast = Toast.makeText(getApplicationContext(),
                        "" + statusCode, Toast.LENGTH_SHORT);
                toast.show();
            }
        });
    }

    private DeterministicKey getHDWalletDeterministicKey(int index) throws Exception {
        String[] seed = getWalletSeed().split(" ");
        MnemonicCode mc = new MnemonicCode();
        byte[] rnd = mc.toEntropy(Arrays.asList(seed));

        DeterministicKey dk = HDKeyDerivation.createMasterPrivateKey(rnd);

        return HDKeyDerivation.deriveChildKey(dk, index);
    }

    private void processPubKeyRequest(String[] params, String post_back, int index)
            throws Exception {

        DeterministicKey ekprv = getHDWalletDeterministicKey(index);

        Toast toast = Toast.makeText(getApplicationContext(),
                bytesToHex(ekprv.getPubKeyBytes()), Toast.LENGTH_SHORT);
        toast.show();

        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams rp = new RequestParams();
        rp.put("pubkey", bytesToHex(ekprv.getPubKeyBytes()));


        for(int i = 3; (i + 1) < params.length; i+= 2) {
            rp.put(params[i], params[i + 1]);
        }


        Log.w("INFO", rp.toString());

        client.post(post_back, rp, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(String response) {

                Toast toast = Toast.makeText(getApplicationContext(),
                        response, Toast.LENGTH_SHORT);
                toast.show();
            }
            @Override
            public void onFailure(int statusCode, Header[] headers,
                                  byte[] responseBody, Throwable error) {

                Toast toast = Toast.makeText(getApplicationContext(),
                        "" + statusCode, Toast.LENGTH_SHORT);
                toast.show();
            }
        });
    }

    private void processMPKRequest(String[] params, String post_back, int index) throws Exception {

        DeterministicKey ekprv = getHDWalletDeterministicKey(index);

        Toast toast = Toast.makeText(getApplicationContext(),
                ekprv.serializePubB58(), Toast.LENGTH_SHORT);
        toast.show();

        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams rp = new RequestParams();
        rp.put("mpk", ekprv.serializePubB58());


        for(int i = 3; (i + 1) < params.length; i+= 2) {
            rp.put(params[i], params[i + 1]);
        }


        Log.w("INFO", rp.toString());

        client.post(post_back, rp, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(String response) {

                Toast toast = Toast.makeText(getApplicationContext(),
                        response, Toast.LENGTH_SHORT);
                toast.show();
                Log.w("INFO", "SUCCESS " + response);
            }
            @Override
            public void onFailure(int statusCode, Header[] headers,
                                  byte[] responseBody, Throwable error) {

                Toast toast = Toast.makeText(getApplicationContext(),
                        "" + statusCode, Toast.LENGTH_SHORT);
                toast.show();
                Log.w("ERROR", "" + bytesToHex(responseBody));

                Log.w("INFO", "FAILURE " + statusCode);
            }
        });
    }

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static int crc16(String serviceName) {
        int crc = 0xFFFF;          // initial value
        int polynomial = 0x1021;   // 0001 0000 0010 0001  (0, 5, 12)

        byte[] bytes = serviceName.getBytes();

        for (byte b : bytes) {
            for (int i = 0; i < 8; i++) {
                boolean bit = ((b   >> (7-i) & 1) == 1);
                boolean c15 = ((crc >> 15    & 1) == 1);
                crc <<= 1;
                if (c15 ^ bit) crc ^= polynomial;
            }
        }

        crc &= 0xffff;
        return crc;
    }
}
