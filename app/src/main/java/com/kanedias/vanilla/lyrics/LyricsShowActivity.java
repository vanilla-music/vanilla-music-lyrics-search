/*
 * Copyright (C) 2016 Oleg Chernovskiy <adonai@xaker.ru>
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

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import static com.kanedias.vanilla.lyrics.PluginConstants.*;
import static com.kanedias.vanilla.lyrics.PluginService.pluginInstalled;

/**
 * Main activity of Lyrics Search plugin. This will be presented as a dialog to the user
 * if one chooses it as the requested plugin.
 * <p/>
 *
 * @see PluginService service that launches this
 *
 * @author Oleg Chernovskiy
 */
public class LyricsShowActivity extends Activity {

    private TextView mLyricsText;
    private ViewSwitcher mSwitcher;
    private Button mOkButton, mWriteFileButton;

    private LyricsEngine mEngine = new LyricsWikiEngine();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lyrics_show);

        mSwitcher = (ViewSwitcher) findViewById(R.id.loading_switcher);
        mLyricsText = (TextView) findViewById(R.id.lyrics_text);
        mWriteFileButton = (Button) findViewById(R.id.write_file_button);
        mOkButton = (Button) findViewById(R.id.ok_button);

        setupUI();
        handlePassedIntent(); // called in onCreate to be shown only once
    }

    private void handlePassedIntent() {
        // check if this is an answer from tag plugin
        if (TextUtils.equals(getIntent().getStringExtra(EXTRA_PARAM_P2P), P2P_READ_TAG)) {
            // already checked this string in service, no need in additional checks
            String lyrics = getIntent().getStringArrayExtra(EXTRA_PARAM_P2P_VAL)[0];
            showFetchedLyrics(lyrics);
            return;
        }

        // if tag editor is installed, show `write to tag` button
        if (pluginInstalled(this, PluginService.PLUGIN_TAG_EDIT_PKG)) {
            mWriteFileButton.setVisibility(View.VISIBLE);
            mWriteFileButton.setOnClickListener(new LyricsShowActivity.LyricsToTagSender());
        }

        // we didn't receive lyrics from tag plugin, try to retrieve it via lyrics engine
        new LyricsFetcher().execute(getIntent());
    }

    private void showFetchedLyrics(String lyrics) {
        mLyricsText.setText(lyrics);
        mWriteFileButton.setEnabled(true);
        mSwitcher.showNext();
    }

    /**
     * Initialize UI elements with handlers and action listeners
     */
    private void setupUI() {
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
            if(TextUtils.isEmpty(lyrics)) {
                // no lyrics - show excuse
                finish();
                Toast.makeText(LyricsShowActivity.this, R.string.lyrics_not_found, Toast.LENGTH_SHORT).show();
                return;
            }

            showFetchedLyrics(lyrics);
        }
    }

    /**
     * CLick listener for P2P integration, sends intent to write retrieved lyrics to local file tag
     */
    private class LyricsToTagSender implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            String lyrics = mLyricsText.getText().toString();
            Intent request = new Intent(ACTION_LAUNCH_PLUGIN);
            request.setPackage(PluginService.PLUGIN_TAG_EDIT_PKG);
            request.putExtra(EXTRA_PARAM_URI, getIntent().getParcelableExtra(EXTRA_PARAM_URI));
            request.putExtra(EXTRA_PARAM_PLUGIN_APP, getApplicationInfo());
            request.putExtra(EXTRA_PARAM_P2P, P2P_WRITE_TAG);
            request.putExtra(EXTRA_PARAM_P2P_KEY, new String[]{"LYRICS"}); // tag name
            request.putExtra(EXTRA_PARAM_P2P_VAL, new String[]{lyrics}); // tag value
            startService(request);
            mWriteFileButton.setVisibility(View.GONE);
        }
    }
}
