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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Broadcast receiver used for retrieving query intents
 *
 * @see PluginConstants
 * @see PluginService
 */
public class PluginQueryBroadcastReceiver extends BroadcastReceiver {

    /**
     * Just starts the service. We need a broadcast receiver for this, as ordinary intents
     * are targeted-only. Only query intents will come here, as "plugin launch" intents are targeted
     * and thus don't need a broadcast receiver to reach service.
     * @param context context this receiver operates in
     * @param intent incoming query intent
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(PluginConstants.LOG_TAG, "Received query intent!");
        switch (intent.getAction()) {
            case PluginConstants.ACTION_REQUEST_PLUGIN_PARAMS:
                intent.setClass(context, PluginService.class);
                context.startService(intent);
                return;
            default:
                Log.e(PluginConstants.LOG_TAG, "Unknown intent received by receiver! Action" + intent.getAction());
        }
    }
}
