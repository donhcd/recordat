package com.recordat.Recordat;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.*;
import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.Session.AccessType;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;

public class Recordat extends Activity {
    final static private String LOG_TAG = "RECORDAT_MAIN";

    final static private String APP_KEY = "dxbiimfb1fidfq4";
    final static private String APP_SECRET = "a1q12wkacl0etuq";

    final static private AccessType ACCESS_TYPE = AccessType.APP_FOLDER;

    final static private String DROPBOX_KEY_KEY = "com.recordat.Recordat.tokenkey";
    final static private String DROPBOX_SECRET_KEY = "com.recordat.Recordat.secretkey";

    private static String appDirectoryPath;

    private SharedPreferences sharedPreferences;

    private DropboxAPI<AndroidAuthSession> dBApi;

    private boolean startRecording = true;

    private Button recordButton = null;
    private MediaRecorder recorder = null;

    private Button playButton = null;
    private MediaPlayer player = null;

    private String newFileName;
    private String fileName;
    private long startTime;
    private ListView addedBkmkListView;

    private ArrayAdapter recordBkmkAdapter;
    private Button addBookmarkButton;
    private Button renameAudioButton;
    private ArrayList<Bookmark> currentBookmarks;
    private ListView playBkmkListView;
    private ArrayList<Bookmark> playingFileBookmarks;
    private ArrayAdapter playAdapter;

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
        appDirectoryPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/recordat/";
        new File(appDirectoryPath).mkdirs();
    }

    private void initPlayerInterface() {
        final LinearLayout linearLayout = (LinearLayout)findViewById(R.id.stuff);
        recordButton = (Button)findViewById(R.id.recordbutton);
        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onRecord(startRecording);
                if (startRecording) {
                    recordButton.setText("Stop recording");
                } else {
                    recordButton.setText("Start recording");
                }
                startRecording = !startRecording;
            }
        });
        playButton = (Button)findViewById(R.id.playbutton);
        playButton.setOnClickListener(new View.OnClickListener() {
            private boolean startPlaying = true;
            public void onClick(View v) {
                if (startPlaying) {
                    onPlay();
                } else {
                    stopPlaying();
                    playButton.setText("Start playing");
                }
                startPlaying = !startPlaying;
            }
        });
        addBookmarkButton = (Button)findViewById(R.id.addbookmark);
        addBookmarkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(Recordat.this, "poopoopoo", Toast.LENGTH_LONG).show();
                recordBkmkAdapter.add(new Bookmark(new Date().getTime() - startTime));
            }
        });
        addedBkmkListView = (ListView)findViewById(R.id.addedbookmarklist);
        addedBkmkListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, final int i, long l) {
                final Bookmark bookmark = (Bookmark) recordBkmkAdapter.getItem(i);

                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(Recordat.this);
                alertDialogBuilder.setTitle("Rename bookmark");
                alertDialogBuilder.setMessage("New name");
                final EditText input = new EditText(Recordat.this);
                alertDialogBuilder.setView(input);
                alertDialogBuilder.setPositiveButton("Rename", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        recordBkmkAdapter.remove(recordBkmkAdapter.getItem(i));
                        bookmark.setText(String.valueOf(input.getText()));
                        recordBkmkAdapter.insert(bookmark, i);
                    }
                });
                alertDialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Canceled.
                    }
                });
                alertDialogBuilder.show();
            }
        });

        playBkmkListView = (ListView)findViewById(R.id.playbookmarklist);
        playBkmkListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, final int i, long l) {
                final Bookmark bookmark = (Bookmark) playAdapter.getItem(i);
                // TODO is maybe the wrong time
                player.seekTo((int) bookmark.getTime());
            }
        });

        renameAudioButton = (Button)findViewById(R.id.renameaudio);
        renameAudioButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(Recordat.this);
                alertDialogBuilder.setTitle("Rename audio recording");
                alertDialogBuilder.setMessage("Recording name");
                final EditText input = new EditText(Recordat.this);
                alertDialogBuilder.setView(input);
                alertDialogBuilder.setPositiveButton("Rename", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        newFileName = String.valueOf(input.getText());
                    }
                });
                alertDialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Canceled.
                    }
                });
                alertDialogBuilder.show();
            }
        });
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
    }

    private void onRecord(boolean start) {
        if (start) {
            startRecording();
            currentBookmarks = new ArrayList<Bookmark>();
            recordBkmkAdapter = new ArrayAdapter(
                    this, android.R.layout.simple_list_item_1, currentBookmarks);
            addedBkmkListView.setAdapter(recordBkmkAdapter);
        } else {
            stopRecording();
        }
    }

    private class AddToDropboxTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... strings) {
            String fileName = strings[0];
            FileInputStream inputStream = null;
            try {
                File file = new File(appDirectoryPath+fileName);
                inputStream = new FileInputStream(file);
                DropboxAPI.Entry newEntry = dBApi.putFile(
                        fileName, inputStream, file.length(), null, null);
                Log.i("DbExampleLog", "The uploaded file's rev is: " + newEntry.rev);
            } catch (FileNotFoundException e) {
                Log.e("DbExampleLog", "File not found.");
            } catch (DropboxException e) {
                Log.e("DbExampleLog", "Something went wrong while uploading.");
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {}
                }
            }
            return null;
        }
    }

    private void onPlay() {
        AlertDialog.Builder builder = new AlertDialog.Builder(Recordat.this);
        FilenameFilter filter = new FilenameFilter() {
            public boolean accept(File dir, String filename) {
                File sel = new File(dir, filename);
                return filename.contains(".mp4") || sel.isDirectory();
            }
        };
        final String[] recordingList = new File(appDirectoryPath).list(filter);
        final String[] theFile = {""};
        builder.setTitle("Choose recording");
        builder.setItems(recordingList, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                theFile[0] = recordingList[which];
                startPlaying(appDirectoryPath + theFile[0]);
                playButton.setText("Stop playing");
            }
        });
        builder.show();
    }

    private void startPlaying(String fileName) {
        String bkmksFile = fileName;
        bkmksFile = bkmksFile.substring(0, bkmksFile.length()-4)+"_bmks.json";
        playingFileBookmarks = getBookmarksFromFile(bkmksFile);
        playAdapter = new ArrayAdapter(
                this, android.R.layout.simple_list_item_1, playingFileBookmarks);
        playBkmkListView.setAdapter(playAdapter);
        playBkmkListView.setVisibility(View.VISIBLE);
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
        playBkmkListView.setVisibility(View.GONE);
    }

    private void startRecording() {
        fileName = new Date().getTime() + "";
        newFileName = null;
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        recorder.setOutputFile(appDirectoryPath + fileName + ".mp4");
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

        try {
            recorder.prepare();
            recorder.start();
            startTime = new Date().getTime();
            findViewById(R.id.recordingoptions).setVisibility(View.VISIBLE);
            findViewById(R.id.addedbookmarklist).setVisibility(View.VISIBLE);
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
            Toast.makeText(this, "fuck, we are not prepared", Toast.LENGTH_LONG).show();
        }
    }

    private void stopRecording() {
        recordButton.setText("Start recording");
//        startRecording = true;
        recorder.stop();
        recorder.release();
        recorder = null;
        findViewById(R.id.recordingoptions).setVisibility(View.GONE);
        findViewById(R.id.addedbookmarklist).setVisibility(View.GONE);
        if (newFileName != null) {
            File from = new File(appDirectoryPath+fileName+".mp4");
            File to = new File(appDirectoryPath+newFileName+".mp4");
            from.renameTo(to);
        } else {
            newFileName = fileName;
        }

        String newFileInfoName = newFileName + "_bmks";
        try {
            FileWriter file = new FileWriter(appDirectoryPath+newFileInfoName+".json");
            Toast.makeText(this, new Gson().toJson(currentBookmarks), Toast.LENGTH_LONG).show();
            file.write(new Gson().toJson(currentBookmarks));
            file.flush();
            file.close();

        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        currentBookmarks = null;

        new AddToDropboxTask().execute(newFileName.concat(".mp4"));
        new AddToDropboxTask().execute(newFileInfoName.concat(".json"));
    }

    private String getBookmarksJSON(ArrayList<Bookmark> currentBookmarks) {
        return new Gson().toJson(currentBookmarks);
    }

    private ArrayList<Bookmark> getBookmarksFromFile(String filePath) {
        BufferedReader br = null;
        String file = "";

        try {
            String sCurrentLine;
            Log.e(LOG_TAG, "in getBookmarksFromFile, reading "+filePath);
            br = new BufferedReader(new FileReader(filePath));
            Log.e(LOG_TAG, "made br");
            while ((sCurrentLine = br.readLine()) != null) {
                file = file.concat(sCurrentLine);
            }
            Log.e(LOG_TAG, "file read: "+file);
        } catch (IOException e) {
            Log.e(LOG_TAG, e.getMessage());
            return null;
        } finally {
            try {
                if (br != null)br.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        Type collectionType = new TypeToken<ArrayList<Bookmark>>(){}.getType();
        return new Gson().fromJson(file, collectionType);
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
            stopRecording();
        }

        if (player != null) {
            player.release();
            player = null;
        }
    }
}
