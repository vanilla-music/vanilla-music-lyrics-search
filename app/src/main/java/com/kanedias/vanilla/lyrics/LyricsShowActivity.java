/*
 * Copyright (C) 2016-2017 Oleg `Kanedias` Chernovskiy <adonai@xaker.ru>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.kanedias.vanilla.lyrics;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.provider.DocumentFile;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.kanedias.vanilla.plugins.DialogActivity;
import com.kanedias.vanilla.plugins.PluginConstants;
import com.kanedias.vanilla.plugins.PluginUtils;
import com.kanedias.vanilla.plugins.saf.SafRequestActivity;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static com.kanedias.vanilla.plugins.PluginConstants.*;
import static com.kanedias.vanilla.lyrics.PluginService.pluginInstalled;
import static com.kanedias.vanilla.plugins.PluginUtils.checkAndRequestPermissions;
import static com.kanedias.vanilla.plugins.saf.SafUtils.findInDocumentTree;
import static com.kanedias.vanilla.plugins.saf.SafUtils.isSafNeeded;

/**
 * Main activity of Lyrics Search plugin. This will be presented as a dialog to the user
 * if one chooses it as the requested plugin.
 * <p/>
 *
 * @see PluginService service that launches this
 *
 * @author Oleg Chernovskiy
 */
public class LyricsShowActivity extends DialogActivity {

    private SharedPreferences mPrefs;

    private TextView mLyricsText;
    private ViewSwitcher mSwitcher;
    private Button mOkButton, mWriteButton;

    private LyricsEngine mEngine = new LyricsWikiEngine();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lyrics_show);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        mSwitcher = (ViewSwitcher) findViewById(R.id.loading_switcher);
        mLyricsText = (TextView) findViewById(R.id.lyrics_text);
        mWriteButton = (Button) findViewById(R.id.write_button);
        mOkButton = (Button) findViewById(R.id.ok_button);

        setupUI();
        handlePassedIntent(true); // called in onCreate to be shown only once
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = new MenuInflater(this);
        inflater.inflate(R.menu.lyrics_options, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            switch (item.getItemId()) {
                case R.id.reload_option:
                    // show only when loading is complete
                    item.setVisible(mSwitcher.getDisplayedChild() == 1);
                    continue;
                default:
                    break;
            }
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.reload_option:
                // show loading circle
                mSwitcher.setDisplayedChild(0);
                handlePassedIntent(false);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void handlePassedIntent(boolean useLocal) {
        // check if this is an answer from tag plugin
        if (useLocal && TextUtils.equals(getIntent().getStringExtra(EXTRA_PARAM_P2P), P2P_READ_TAG)) {
            // already checked this string in service, no need in additional checks
            String lyrics = getIntent().getStringArrayExtra(EXTRA_PARAM_P2P_VAL)[0];
            showFetchedLyrics(lyrics);
            return;
        }

        // try to load from *.lrc file nearby
        if (useLocal && loadFromFile()) {
            return;
        }


        // we didn't receive lyrics from tag plugin, try to retrieve it via lyrics engine
        new LyricsFetcher().execute(getIntent());
    }

    /**
     * Try to load lyrics tag from companion *.lrc file nearby
     * @return true if lyrics was loaded from file, false otherwise
     */
    private boolean loadFromFile() {
        if (!PluginUtils.havePermissions(this, WRITE_EXTERNAL_STORAGE)) {
            return false;
        }

        Uri fileUri = getIntent().getParcelableExtra(EXTRA_PARAM_URI);
        File media = new File(fileUri.getPath());
        String lyricsFileName = lyricsForFile(media);
        File lyricsFile = new File(media.getParentFile(), lyricsFileName);
        if (!lyricsFile.exists()) {
            return false;
        }

        try {
            String lyricsText = LyricsWikiEngine.readIt(new FileInputStream(lyricsFile));
            showFetchedLyrics(lyricsText);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Failed to read lyrics text from file!", e);
            return false;
        }
        return true;
    }

    /**
     * Stop spinning animation and show lyrics for the song.
     * Write button wil lbe active after that as lyrics will be available for persisting.
     * @param lyrics retrieved song lyrics
     */
    private void showFetchedLyrics(String lyrics) {
        if (TextUtils.isEmpty(lyrics)) {
            // nothing found
            mWriteButton.setEnabled(false);
        } else {
            // some lyrics was extracted
            mWriteButton.setEnabled(true);
        }
        mLyricsText.setText(lyrics);
        mSwitcher.setDisplayedChild(1);
        invalidateOptionsMenu();
    }

    /**
     * Initialize UI elements with handlers and action listeners
     */
    private void setupUI() {
        mLyricsText.setMovementMethod(new ScrollingMovementMethod());
        mWriteButton.setOnClickListener(new SelectWriteAction());
        mOkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    /**
     * External lyrics fetcher (using network). Operates asynchronously, notifies dialog when finishes.
     * On no result (no such lyrics, couldn't fetch etc.) shows toast about this, on success updates dialog text.
     */
    private class LyricsFetcher extends AsyncTask<Intent, Void, String> {

        @Override
        protected String doInBackground(Intent... params) {
            String title = getIntent().getStringExtra(EXTRA_PARAM_SONG_TITLE);
            String artist = getIntent().getStringExtra(EXTRA_PARAM_SONG_ARTIST);
            return mEngine.getLyrics(artist, title);
        }

        @Override
        protected void onPostExecute(String lyrics) {
            if (TextUtils.isEmpty(lyrics)) {
                // no lyrics - show excuse
                Toast.makeText(LyricsShowActivity.this, R.string.lyrics_not_found, Toast.LENGTH_SHORT).show();
            }

            showFetchedLyrics(lyrics);
        }
    }

    /**
     * CLick listener for P2P integration, sends intent to write retrieved lyrics to local file tag or to
     * lyrics file
     */
    private class SelectWriteAction implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            List<String> actions = new ArrayList<>();
            actions.add(getString(R.string.write_to_lrc));

            // if tag editor is installed, show `write to tag` button
            if (pluginInstalled(LyricsShowActivity.this, PluginService.PLUGIN_TAG_EDIT_PKG)) {
                actions.add(getString(R.string.write_to_tag));
            }

            new AlertDialog.Builder(LyricsShowActivity.this)
                    .setItems(actions.toArray(new CharSequence[0]), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which) {
                                case 0: // to lyrics file
                                    // onResume will fire both on first launch and on return from permission request
                                    if (!checkAndRequestPermissions(LyricsShowActivity.this, WRITE_EXTERNAL_STORAGE)) {
                                        return;
                                    }

                                    persistAsLrcFile();
                                    break;
                                case 1: // to media file tag
                                    writeToFileTag();
                                    break;
                            }
                        }
                    }).create().show();
        }
    }

    /**
     * Write lyrics as a *.lrc file - selects SAF/File routine based on target access.
     * Resulting file should be placed in the same directory as media file but with *.lrc extension instead.
     */
    private void persistAsLrcFile() {
        Uri fileUri = getIntent().getParcelableExtra(EXTRA_PARAM_URI);
        if (fileUri == null) {
            // wrong intent passed?
            return;
        }

        File mediaFile = new File(fileUri.getPath());
        if (!mediaFile.exists()) {
            // file deleted while launching intent or player db is not refreshed
            return;
        }

        String lrcFilename = lyricsForFile(mediaFile);
        File lrcTarget = new File(mediaFile.getParent(), lrcFilename);
        byte[] data = mLyricsText.getText().toString().getBytes(Charset.forName("UTF-8"));
        if (isSafNeeded(mediaFile)) {
            if (mPrefs.contains(PREF_SDCARD_URI)) {
                // we already got the permission!
                writeThroughSaf(data, mediaFile, lrcTarget.getName());
                return;
            }

            // request SAF permissions in SAF activity
            Intent safIntent = new Intent(this, SafRequestActivity.class);
            safIntent.putExtra(PluginConstants.EXTRA_PARAM_PLUGIN_APP, getApplicationInfo());
            safIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            safIntent.putExtras(getIntent());
            startActivity(safIntent);
            // it will pass us URI back after the work is done
        } else {
            writeThroughFile(data, mediaFile, lrcTarget);
        }
    }

    /**
     * Retrieves companion name for lyrics file from media passed
     * @param mediaFile - original media file that the lyrics was requested for
     * @return string representing name with extension for lyrics companion file
     */
    @NonNull
    private static String lyricsForFile(File mediaFile) {
        String mfName = mediaFile.getName();
        return mfName.indexOf(".") > 0
                ? mfName.substring(0, mfName.lastIndexOf(".")) + ".lrc"
                : mfName + ".lrc";
    }

    /**
     * Write lyrics to *.lrc file through SAF framework - the only way to do it in Android > 4.4 when working with SD card
     */
    private void writeThroughSaf(byte[] data, File original, String name) {
        DocumentFile originalRef;
        if (mPrefs.contains(PREF_SDCARD_URI)) {
            // no sorcery can allow you to gain URI to the document representing file you've been provided with
            // you have to find it again now using Document API

            // /storage/volume/Music/some.mp3 will become [storage, volume, music, some.mp3]
            List<String> pathSegments = new ArrayList<>(Arrays.asList(original.getAbsolutePath().split("/")));
            Uri allowedSdRoot = Uri.parse(mPrefs.getString(PREF_SDCARD_URI, ""));
            originalRef = findInDocumentTree(DocumentFile.fromTreeUri(this, allowedSdRoot), pathSegments);
        } else {
            // user will click the button again
            return;
        }

        if (originalRef == null) {
            // nothing selected or invalid file?
            Toast.makeText(this, R.string.saf_nothing_selected, Toast.LENGTH_LONG).show();
            return;
        }

        DocumentFile folderJpgRef = originalRef.getParentFile().createFile("image/*", name);
        try {
            ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(folderJpgRef.getUri(), "rw");
            if (pfd == null) {
                // should not happen
                Log.e(LOG_TAG, "SAF provided incorrect URI!" + folderJpgRef.getUri());
                return;
            }

            FileOutputStream fos = new FileOutputStream(pfd.getFileDescriptor());
            fos.write(data);
            fos.close();

            // rescan original file
            Toast.makeText(this, R.string.file_written_successfully, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.saf_write_error) + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            Log.e(LOG_TAG, "Failed to write to file descriptor provided by SAF!", e);
        }
    }

    /**
     * Write to *.lrc file through file-based API
     * @param data - data to write
     * @param original - original media file that was requested by user
     * @param target - target file for writing metadata into
     */
    private void writeThroughFile(byte[] data, File  original, File target) {
        try {
            FileOutputStream fos = new FileOutputStream(target);
            fos.write(data);
            fos.close();

            Toast.makeText(this, R.string.file_written_successfully, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.error_writing_file) + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            Log.e(LOG_TAG, "Failed to write to file descriptor provided by SAF!", e);
        }
    }

    /**
     * Write to the song tag using Tag Editor Plugin
     */
    private void writeToFileTag() {
        String lyrics = mLyricsText.getText().toString();
        Intent request = new Intent(ACTION_LAUNCH_PLUGIN);
        request.setPackage(PluginService.PLUGIN_TAG_EDIT_PKG);
        request.putExtra(EXTRA_PARAM_URI, getIntent().getParcelableExtra(EXTRA_PARAM_URI));
        request.putExtra(EXTRA_PARAM_PLUGIN_APP, getApplicationInfo());
        request.putExtra(EXTRA_PARAM_P2P, P2P_WRITE_TAG);
        request.putExtra(EXTRA_PARAM_P2P_KEY, new String[]{"LYRICS"}); // tag name
        request.putExtra(EXTRA_PARAM_P2P_VAL, new String[]{lyrics}); // tag value
        startService(request);
    }

}