package com.kanedias.vanilla.lyrics;

import android.net.Uri;
import android.util.Log;

import com.kanedias.vanilla.plugins.PluginUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

/**
 * @author Kanedias
 * <p>
 * Created on 29.11.20
 */
public class GeniusApiEngine implements LyricsEngine {

    private static final String GENIUS_API_HOST = "api.genius.com";
    private static final String GENIUS_MAIN_URL = "https://genius.com";

    private static final String GENIUS_API_TOKEN = BuildConfig.GENIUS_API_TOKEN;
    private static final String TAG = GeniusApiEngine.class.getSimpleName();

    @Override
    public String getLyrics(String artistName, String songTitle) {
        try {

            JSONObject searchReply = makeApiCall(artistName, songTitle);
            if (searchReply == null) { // no URL in API answer or no correct answer at all
                return null;
            }

            // get first match from list
            JSONArray hits = searchReply.getJSONObject("response").getJSONArray("hits");
            if (hits == null || hits.length() == 0) {
                // no hits
                return null;
            }

            for (int i = 0; i < hits.length(); ++i) {
                JSONObject song = hits.getJSONObject(i);
                if (!song.getString("type").equals("song")) {
                    // not a song, skip
                    continue;
                }

                String lyricsUrl = song.getJSONObject("result").getString("path");
                return parseFullLyricsPage(lyricsUrl);
            }

            return null;

        } catch (IOException e) {
            Log.w(TAG, "Couldn't connect to lyrics wiki REST endpoints", e);
            return null;
        } catch (JSONException e) {
            Log.w(TAG, "Couldn't transform API answer to JSON entity", e);
            return null;
        }
    }

    private String parseFullLyricsPage(String lyricsUrl) throws IOException {
        if (lyricsUrl == null)
            return null;

        HttpsURLConnection pageGet = null;
        try {
            pageGet = (HttpsURLConnection) new URL(GENIUS_MAIN_URL + lyricsUrl).openConnection();
            pageGet.setReadTimeout(10_000);
            pageGet.setConnectTimeout(15_000);

            pageGet.connect();
            int response = pageGet.getResponseCode();
            if (response != HttpsURLConnection.HTTP_OK) {
                // redirects are handled internally, this is clearly an error
                return null;
            }

            InputStream is = pageGet.getInputStream();
            Document page = Jsoup.parse(is, "UTF-8", GENIUS_MAIN_URL);
            Element lyrics = page.select("div.lyrics p").first();

            if (lyrics == null) {
                // page format changed
                return null;
            }

            StringBuilder builder = new StringBuilder();
            for (Node curr : lyrics.childNodes()) {
                if (curr instanceof Element && ((Element) curr).tagName().equals("br")) {
                    builder.append("\n");
                } else if (curr instanceof Element) {
                    builder.append(((Element) curr).text());
                } else if (curr instanceof TextNode) {
                    builder.append(((TextNode) curr).text());
                }
            }

            return builder.toString();
        } finally {
            if (pageGet != null) {
                pageGet.disconnect();
            }
        }
    }

    /**
     * First call
     */
    private JSONObject makeApiCall(String artistName, String songTitle) throws IOException, JSONException {
        HttpsURLConnection apiCall = null;
        try {
            // build query
            Uri link = new Uri.Builder()
                    .scheme("https")
                    .authority(GENIUS_API_HOST)
                    .path("search")
                    .appendQueryParameter("q", artistName + " " + songTitle)
                    .build();

            // construct an http request
            apiCall = (HttpsURLConnection) new URL(link.toString()).openConnection();
            apiCall.setReadTimeout(10_000);
            apiCall.setConnectTimeout(15_000);

            apiCall.setRequestProperty("Authorization", "Bearer " + GENIUS_API_TOKEN);

            // execute
            apiCall.connect();
            int response = apiCall.getResponseCode();
            if (response != HttpsURLConnection.HTTP_OK) {
                // redirects are handled internally, this is clearly an error
                return null;
            }

            InputStream is = apiCall.getInputStream();
            String reply = new String(PluginUtils.readFully(is), "UTF-8");
            return new JSONObject(reply);
        } finally {
            if (apiCall != null) {
                apiCall.disconnect();
            }
        }
    }
}
