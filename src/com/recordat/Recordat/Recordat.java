package com.recordat.Recordat;


import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.Session.AccessType;

public class Recordat extends Activity {
    final static private String TAG = "RECORDAT_MAIN"; 

    final static private String APP_KEY = "dxbiimfb1fidfq4";
    final static private String APP_SECRET = "a1q12wkacl0etuq";

    final static private AccessType ACCESS_TYPE = AccessType.APP_FOLDER;

    final static private String DROPBOX_KEY_KEY = "com.recordat.Recordat.tokenkey";
    final static private String DROPBOX_SECRET_KEY = "com.recordat.Recordat.secretkey";

    private SharedPreferences sharedPreferences;

    private DropboxAPI<AndroidAuthSession> dBApi;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        sharedPreferences = this.getSharedPreferences(
                "com.recordat.Recordat", Context.MODE_PRIVATE);
        initDropboxApi();
    }

    private void initDropboxApi() {
        AppKeyPair appKeys = new AppKeyPair(APP_KEY, APP_SECRET);
        AndroidAuthSession session = new AndroidAuthSession(appKeys, ACCESS_TYPE);
        dBApi = new DropboxAPI<AndroidAuthSession>(session);

        AccessTokenPair accessTokenPair = getStoredKeys();
        if (accessTokenPair != null) {
            dBApi.getSession().startAuthentication(Recordat.this);
        }
        dBApi.getSession().setAccessTokenPair(accessTokenPair);
    }

    protected void onResume() {
        super.onResume();

        // ...

        if (dBApi.getSession().authenticationSuccessful()) {
            try {
                // MANDATORY call to complete auth.
                // Sets the access token on the session
                dBApi.getSession().finishAuthentication();

                AccessTokenPair tokens = dBApi.getSession().getAccessTokenPair();

                // Provide your own storeKeys to persist the access token pair
                // A typical way to store tokens is using SharedPreferences
                storeKeys(tokens.key, tokens.secret);
            } catch (IllegalStateException e) {
                Log.i("DbAuthLog", "Error authenticating", e);
            }
        }

        // ...
    }

    private void storeKeys(String key, String secret) {
        sharedPreferences.edit().putString(DROPBOX_KEY_KEY, key);
        sharedPreferences.edit().putString(DROPBOX_SECRET_KEY, secret).commit();
    }

    private AccessTokenPair getStoredKeys() {
        String key = sharedPreferences.getString(DROPBOX_KEY_KEY, null);
        String secret = sharedPreferences.getString(DROPBOX_SECRET_KEY, null);
        if (key == null || secret == null) {
            return null;
        }
        return new AccessTokenPair(key, secret);
    }
}
