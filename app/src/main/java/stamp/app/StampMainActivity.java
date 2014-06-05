package stamp.app;

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
        if (id == R.id.action_settings) {
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
            if(params.length < 3)
                return;


            String cmd = params[0];
            //String service = params[1];
            String post_back = params[2];

            Log.w("INFO", post_back);

            if(cmd.equals("mpk")) {

                processMPKRequest(params, post_back);

            } else if(cmd.equals("sign")) {

                // Sign a TX.
                processSignRequest(params, post_back);
            }
        }
        catch(Exception e) {

            Toast toast = Toast.makeText(getApplicationContext(),
                    e.toString(), Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    private void processSignRequest(final String[] params, final String post_back)
        throws Exception {

        final DeterministicKey ekprv = getHDWalletDeterministicKey();

        final AsyncHttpClient client = new AsyncHttpClient();
        final RequestParams rp = new RequestParams();

        for(int i = 3; (i + 1) < params.length; i+= 2) {
            rp.put(params[i], params[i + 1]);
        }


        Log.w("INFO", rp.toString());

        client.get(post_back, rp, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(String response) {
                Transaction tx = new Transaction(MainNetParams.get(), Hex.decode(response));

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
                        }
                        @Override
                        public void onFailure(int statusCode, Header[] headers,
                                              byte[] responseBody, Throwable error) {

                            Toast toast = Toast.makeText(getApplicationContext(),
                                    "" + statusCode, Toast.LENGTH_SHORT);
                            toast.show();
                        }
                    });

                } catch (Exception e) {
                    // TODO figure out what to do with it.
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

    private DeterministicKey getHDWalletDeterministicKey() throws Exception {
        String[] seed = getWalletSeed().split(" ");
        MnemonicCode mc = new MnemonicCode();
        byte[] rnd = mc.toEntropy(Arrays.asList(seed));

        return HDKeyDerivation.createMasterPrivateKey(rnd);
    }

    private void processMPKRequest(String[] params, String post_back) throws Exception {

        DeterministicKey ekprv = getHDWalletDeterministicKey();

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
}
