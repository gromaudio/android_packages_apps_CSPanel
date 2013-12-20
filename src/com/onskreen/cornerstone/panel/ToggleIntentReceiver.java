package com.onskreen.cornerstone.panel;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ToggleIntentReceiver extends BroadcastReceiver {
    static final String TAG = "CSPanel TIR";

	@Override
	public void onReceive(Context context, Intent intent) {
        if (CSPanel.isCornestoneStarted) {
            CSPanel.togglePanel();
        }

        Log.v(TAG, "Intent received");
	}
}
