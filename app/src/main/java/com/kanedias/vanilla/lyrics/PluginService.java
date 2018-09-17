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

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import com.kanedias.vanilla.plugins.PluginConstants;

import java.util.List;

import static com.kanedias.vanilla.plugins.PluginConstants.ACTION_HANDLE_PLUGIN_PARAMS;
import static com.kanedias.vanilla.plugins.PluginConstants.ACTION_LAUNCH_PLUGIN;
import static com.kanedias.vanilla.plugins.PluginConstants.ACTION_REQUEST_PLUGIN_PARAMS;
import static com.kanedias.vanilla.plugins.PluginConstants.ACTION_WAKE_PLUGIN;
import static com.kanedias.vanilla.plugins.PluginConstants.EXTRA_PARAM_P2P;
import static com.kanedias.vanilla.plugins.PluginConstants.EXTRA_PARAM_P2P_KEY;
import static com.kanedias.vanilla.plugins.PluginConstants.EXTRA_PARAM_P2P_VAL;
import static com.kanedias.vanilla.plugins.PluginConstants.EXTRA_PARAM_PLUGIN_APP;
import static com.kanedias.vanilla.plugins.PluginConstants.EXTRA_PARAM_PLUGIN_DESC;
import static com.kanedias.vanilla.plugins.PluginConstants.EXTRA_PARAM_PLUGIN_NAME;
import static com.kanedias.vanilla.plugins.PluginConstants.EXTRA_PARAM_URI;
import static com.kanedias.vanilla.plugins.PluginConstants.LOG_TAG;
import static com.kanedias.vanilla.plugins.PluginConstants.P2P_READ_TAG;

/**
 * Main service of Plugin system.
 * This service must be able to handle ACTION_WAKE_PLUGIN, ACTION_REQUEST_PLUGIN_PARAMS and ACTION_LAUNCH_PLUGIN
 * intents coming from VanillaMusic.
 * <p/>
 * Casual conversation looks like this:
 * <pre>
 *     VanillaMusic                                 Plugin
 *          |                                         |
 *          |       ACTION_WAKE_PLUGIN broadcast      |
 *          |---------------------------------------->| (plugin init if just installed)
 *          |                                         |
 *          | ACTION_REQUEST_PLUGIN_PARAMS broadcast  |
 *          |---------------------------------------->| (this is handled by BroadcastReceiver first)
 *          |                                         |
 *          |      ACTION_HANDLE_PLUGIN_PARAMS        |
 *          |<----------------------------------------| (plugin answer with name and desc)
 *          |                                         |
 *          |           ACTION_LAUNCH_PLUGIN          |
 *          |---------------------------------------->| (plugin is allowed to show window)
 * </pre>
 *
 * @see PluginConstants
 * @see LyricsShowActivity
 *
 * @author Oleg Chernovskiy
 */
public class PluginService extends Service {

    public static final String PLUGIN_TAG_EDIT_PKG = "com.kanedias.vanilla.audiotag";

    private Intent mOriginalIntent;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return START_NOT_STICKY;
        }

        final String action = intent.getAction();
        switch (action) {
            case ACTION_WAKE_PLUGIN:
                Log.i(LOG_TAG, "Plugin enabled!");
                break;
            case ACTION_REQUEST_PLUGIN_PARAMS:
                handleRequestPluginParams(intent);
                break;
            case ACTION_LAUNCH_PLUGIN:
                handleLaunchPlugin(intent);
                break;
            default:
                Log.e(LOG_TAG, "Unknown intent action received!" + action);
        }
        return START_NOT_STICKY;
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static boolean pluginInstalled(Context ctx, String pkgName) {
        List<ResolveInfo> resolved = ctx.getPackageManager().queryBroadcastReceivers(new Intent(ACTION_REQUEST_PLUGIN_PARAMS), 0);
        for (ResolveInfo pkg : resolved) {
            if (TextUtils.equals(pkg.activityInfo.packageName, pkgName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Sends plugin info back to Vanilla Music service.
     * @param intent intent from player
     */
    private void handleRequestPluginParams(Intent intent) {
        Intent answer = new Intent(ACTION_HANDLE_PLUGIN_PARAMS);
        answer.setPackage(intent.getPackage());
        answer.putExtra(EXTRA_PARAM_PLUGIN_NAME, getString(R.string.lyrics_search));
        answer.putExtra(EXTRA_PARAM_PLUGIN_APP, getApplicationInfo());
        answer.putExtra(EXTRA_PARAM_PLUGIN_DESC, getString(R.string.plugin_desc));
        sendBroadcast(answer);
    }

    private void handleLaunchPlugin(Intent intent) {
        if (!intent.hasExtra(EXTRA_PARAM_P2P) && pluginInstalled(this, PLUGIN_TAG_EDIT_PKG)) {
            // it's user-requested, try to retrieve lyrics from the tag first
            mOriginalIntent = intent;
            Intent readLyrics = new Intent(ACTION_LAUNCH_PLUGIN);
            readLyrics.setPackage(PluginService.PLUGIN_TAG_EDIT_PKG);
            readLyrics.putExtra(EXTRA_PARAM_URI, (Bundle) intent.getParcelableExtra(EXTRA_PARAM_URI));
            readLyrics.putExtra(EXTRA_PARAM_PLUGIN_APP, getApplicationInfo());
            readLyrics.putExtra(EXTRA_PARAM_P2P, P2P_READ_TAG);
            readLyrics.putExtra(EXTRA_PARAM_P2P_KEY, new String[]{"LYRICS"}); // tag name
            startService(readLyrics);
            return;
        }

        if (intent.hasExtra(EXTRA_PARAM_P2P)) {
            handleP2pIntent(intent);
            return;
        }

        Intent dialogIntent = new Intent(this, LyricsShowActivity.class);
        dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        dialogIntent.putExtras(intent);
        startActivity(dialogIntent);
    }

    /**
     * This plugin also has P2P functionality with others.
     * <br/>
     * Tag plugin - Uses provided field retrieval interface for LYRICS tag:
     * <p/>
     * <pre>
     *     Lyrics Plugin                               Tag Editor Plugin
     *          |                                         |
     *          |       P2P intent with lyrics request    |
     *          |---------------------------------------->| (LP also stores original intent)
     *          |                                         |
     *          |       P2P intent with lyrics response   |
     *          |<----------------------------------------| (can be empty if no embedded lyrics found)
     *          |                                         |
     *
     *     At this point lyrics plugin starts activity with either
     *     extras from lyrics response (if found) or with original intent
     * </pre>
     *
     * @param intent p2p intent that should be handled
     */
    private void handleP2pIntent(Intent intent) {
        switch (intent.getStringExtra(EXTRA_PARAM_P2P)) {
            case P2P_READ_TAG: // this is a reply on our request for lyrics tag
                Intent dialogIntent = new Intent(this, LyricsShowActivity.class);
                dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                dialogIntent.putExtras(mOriginalIntent);

                String[] fields = intent.getStringArrayExtra(EXTRA_PARAM_P2P_VAL);
                if (fields.length > 0 && !TextUtils.isEmpty(fields[0])) {
                    // start activity with retrieved lyrics
                    dialogIntent.putExtras(intent);
                }
                startActivity(dialogIntent);
                break;
        }
    }
}
