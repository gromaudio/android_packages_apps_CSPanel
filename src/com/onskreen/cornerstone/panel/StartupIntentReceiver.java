package com.onskreen.cornerstone.panel;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

public class StartupIntentReceiver extends BroadcastReceiver {
    static final String TAG = "CSPanel SIR";

	@Override
	public void onReceive(Context context, Intent intent) {
		//Check that Cornerstone should start on boot
		SharedPreferences settings = context.getSharedPreferences(CSSettings.CS_PREFS, Context.MODE_MULTI_PROCESS);
        // Save preference for CSSettings app
        if (!settings.contains("startup")) {
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean("startup", context.getResources().getBoolean(R.bool.startup));
            editor.commit();

            Log.v(TAG, "Default preferences saved on BOOT");
        }
		if (settings.getBoolean("startup", context.getResources().getBoolean(R.bool.startup))) {
			//Set what activity should launch after boot completes
			Intent startupBootIntent = new Intent(context, CSPanel.class);
			startupBootIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(startupBootIntent);
		}
		
		Log.v(TAG, "StartupIntentReceiver intent received");
	}
}
