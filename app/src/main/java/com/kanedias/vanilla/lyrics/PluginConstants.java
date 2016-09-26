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

/**
 * This class constants should be synchronized with VanillaMusic <code>PluginUtils</code> class
 */
public class PluginConstants {

    private PluginConstants() {
    }

    // these actions are for passing between main player and plugins
    static final String ACTION_REQUEST_PLUGIN_PARAMS = "ch.blinkenlights.android.vanilla.action.REQUEST_PLUGIN_PARAMS"; // broadcast
    static final String ACTION_HANDLE_PLUGIN_PARAMS = "ch.blinkenlights.android.vanilla.action.HANDLE_PLUGIN_PARAMS"; // answer
    static final String ACTION_WAKE_PLUGIN = "ch.blinkenlights.android.vanilla.action.WAKE_PLUGIN"; // targeted for each found
    static final String ACTION_LAUNCH_PLUGIN = "ch.blinkenlights.android.vanilla.action.LAUNCH_PLUGIN"; // targeted at selected by user

    // these are used by plugins to describe themselves
    static final String EXTRA_PARAM_PLUGIN_NAME = "ch.blinkenlights.android.vanilla.extra.PLUGIN_NAME";
    static final String EXTRA_PARAM_PLUGIN_APP = "ch.blinkenlights.android.vanilla.extra.PLUGIN_APP";
    static final String EXTRA_PARAM_PLUGIN_DESC = "ch.blinkenlights.android.vanilla.extra.PLUGIN_DESC";

    // this is passed to plugin when it is selected by user
    static final String EXTRA_PARAM_FILE_PATH = "ch.blinkenlights.android.vanilla.extra.FILE_PATH";
    static final String EXTRA_PARAM_SONG_TITLE = "ch.blinkenlights.android.vanilla.extra.SONG_TITLE";
    static final String EXTRA_PARAM_SONG_ALBUM = "ch.blinkenlights.android.vanilla.extra.SONG_ALBUM";
    static final String EXTRA_PARAM_SONG_ARTIST = "ch.blinkenlights.android.vanilla.extra.SONG_ARTIST";

    // plugin-to-plugin extras (pass EXTRA_PARAM_PLUGIN_APP too to know whom to answer)
    static final String EXTRA_PARAM_P2P = "ch.blinkenlights.android.vanilla.extra.P2P"; // marker
    static final String EXTRA_PARAM_P2P_KEY = "ch.blinkenlights.android.vanilla.extra.P2P_KEY";
    static final String EXTRA_PARAM_P2P_VAL = "ch.blinkenlights.android.vanilla.extra.P2P_VALUE";

    // related to tag editor
    static final String P2P_WRITE_TAG = "WRITE_TAG";
    static final String P2P_READ_TAG = "READ_TAG";

    static final String LOG_TAG = "Vanilla:LyricsPlugin";

    static final String VANILLA_PACKAGE_NAME = "ch.blinkenlights.android.vanilla";
    static final String VANILLA_SERVICE_NAME = ".PlaybackService";
}
