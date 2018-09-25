/*
 * Copyright (C) 2016-2018 Oleg `Kanedias` Chernovskiy <adonai@xaker.ru>
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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.kanedias.vanilla.plugins.PluginConstants;

import static com.kanedias.vanilla.plugins.PluginConstants.*;

/**
 * Broadcast receiver used for retrieving query intents
 *
 * @see PluginConstants
 */
public class PluginQueryBroadcastReceiver extends BroadcastReceiver {

    /**
     * Just answer with plugin parameters. We need a broadcast receiver for this, as ordinary intents
     * are targeted-only. Only query intents will come here, as "plugin launch" intents are targeted
     * and thus don't need a broadcast receiver to reach activity.
     *
     * @param context context this receiver operates in
     * @param intent  incoming query intent
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(PluginConstants.LOG_TAG, "Received query intent!");
        if (intent.getAction() == null)
            return;

        switch (intent.getAction()) {
            case PluginConstants.ACTION_REQUEST_PLUGIN_PARAMS:
                handleRequestPluginParams(context, intent);
                return;
            default:
                Log.e(PluginConstants.LOG_TAG, "Unknown intent received by receiver! Action" + intent.getAction());
        }
    }

    /**
     * Sends plugin info back to Vanilla Music service.
     *
     * @param intent intent from player
     */
    private void handleRequestPluginParams(Context ctx, Intent intent) {
        Intent answer = new Intent(ACTION_HANDLE_PLUGIN_PARAMS);
        answer.setPackage(intent.getPackage());
        answer.putExtra(EXTRA_PARAM_PLUGIN_NAME, ctx.getString(R.string.lyrics_search));
        answer.putExtra(EXTRA_PARAM_PLUGIN_APP, ctx.getApplicationInfo());
        answer.putExtra(EXTRA_PARAM_PLUGIN_DESC, ctx.getString(R.string.plugin_desc));
        ctx.sendBroadcast(answer);
    }
}
