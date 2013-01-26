package com.recordat.Recordat;


import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.Session.AccessType;

import java.io.IOException;

public class Recordat extends Activity {
    final static private String LOG_TAG = "RECORDAT_MAIN";

    final static private String APP_KEY = "dxbiimfb1fidfq4";
    final static private String APP_SECRET = "a1q12wkacl0etuq";

    final static private AccessType ACCESS_TYPE = AccessType.APP_FOLDER;

    final static private String DROPBOX_KEY_KEY = "com.recordat.Recordat.tokenkey";
    final static private String DROPBOX_SECRET_KEY = "com.recordat.Recordat.secretkey";

    private static String fileName = null;

    private SharedPreferences sharedPreferences;

    private DropboxAPI<AndroidAuthSession> dBApi;

    private RecordButton recordButton = null;
    private MediaRecorder recorder = null;

    private PlayButton playButton = null;
    private MediaPlayer player = null;

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
        initPlayerInterface();
        fileName = Environment.getExternalStorageDirectory().getAbsolutePath() + "/audiorecordtest.3gp";
    }

    private void initPlayerInterface() {
        LinearLayout linearLayout = new LinearLayout(this);
        recordButton = new RecordButton(this);
        linearLayout.addView(recordButton,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        0));
        playButton = new PlayButton(this);
        linearLayout.addView(playButton,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        0));
        setContentView(linearLayout);
    }

    private void initDropboxApi() {
        AppKeyPair appKeys = new AppKeyPair(APP_KEY, APP_SECRET);
        AndroidAuthSession session = new AndroidAuthSession(appKeys, ACCESS_TYPE);
        dBApi = new DropboxAPI<AndroidAuthSession>(session);

        AccessTokenPair accessTokenPair = getStoredKeys();
        if (accessTokenPair == null) {
            dBApi.getSession().startAuthentication(Recordat.this);
        } else {
            dBApi.getSession().setAccessTokenPair(accessTokenPair);
        }
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

    private void onRecord(boolean start) {
        if (start) {
            startRecording();
        } else {
            stopRecording();
        }
    }

    private void onPlay(boolean start) {
        if (start) {
            startPlaying();
        } else {
            stopPlaying();
        }
    }

    private void startPlaying() {
        player = new MediaPlayer();
        try {
            player.setDataSource(fileName);
            player.prepare();
            player.start();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }
    }

    private void stopPlaying() {
        player.release();
        player = null;
    }

    private void startRecording() {
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        recorder.setOutputFile(fileName);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

        try {
            recorder.prepare();
            recorder.start();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
            Toast.makeText(this, "fuck, we are not prepared", Toast.LENGTH_LONG).show();
        }
    }

    private void stopRecording() {
        recorder.stop();
        recorder.release();
        recorder = null;
    }

    private void storeKeys(String key, String secret) {
        sharedPreferences.edit().putString(DROPBOX_KEY_KEY, key).commit();
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

    @Override
    public void onPause() {
        super.onPause();
        if (recorder != null) {
            recorder.release();
            recorder = null;
        }

        if (player != null) {
            player.release();
            player = null;
        }
    }

    class RecordButton extends Button {
        boolean startRecording = true;

        OnClickListener clicker = new OnClickListener() {
            public void onClick(View v) {
                onRecord(startRecording);
                if (startRecording) {
                    setText("Stop recording");
                } else {
                    setText("Start recording");
                }
                startRecording = !startRecording;
            }
        };

        public RecordButton(Context ctx) {
            super(ctx);
            setText("Start recording");
            setOnClickListener(clicker);
        }
    }

    class PlayButton extends Button {
        boolean startPlaying = true;

        OnClickListener clicker = new OnClickListener() {
            public void onClick(View v) {
                onPlay(startPlaying);
                if (startPlaying) {
                    setText("Stop playing");
                } else {
                    setText("Start playing");
                }
                startPlaying = !startPlaying;
            }
        };

        public PlayButton(Context ctx) {
            super(ctx);
            setText("Start playing");
            setOnClickListener(clicker);
        }
    }
}
