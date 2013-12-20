package com.onskreen.cornerstone.panel;

import android.app.Activity;
import android.app.ProgressDialog;
import android.app.ActivityManagerNative;
import android.app.ICornerstoneManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.Toast;
 
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

public class CSPanel extends Activity {
    static final String TAG = "CSPanel";

    static final boolean DEBUG = true;

    /**
     * Author: Onskreen
     * Date: 11/01/2012
     *
     * ICS Upgrade - the screen becoming active causes onResume to get triggered
     * so now need a safety check to avoid cs apps from getting relaunched.
     */
    static boolean isCornestoneStarted = false;

    /**
     * Author: Onskreen
     * Date: 13/07/2011
     *
     * Shared preference file name constant
     */
    static final String CS_PREFS = "CSPrefs";

    /**Static for now, really should be dynamically managed**/
    String[] mCornerstoneApps;
    String mCSLauncherPkgName;
    String mCSLauncherClassName;

    enum Cornerstone_State {
		OPEN,
		CLOSED
    }
    static Cornerstone_State csState = Cornerstone_State.OPEN; // Begins in expanded state

    /**
     * Author: Onskreen
     * Date: 25/02/2011
     *
     * Cornerstone controls
     */
    private ImageButton csAppHeader1;

    /**
     * Author: Onskreen
     * Date: 25/02/2011
     *
     * Handler to update the UI controls in UI thread
     */
    private Handler mHandler;

    /**
     * Author: Onskreen
     * Date: 25/02/2011
     *
     * CornerstoneManager callback method to be notified by WMS whenever user changes the focus
     */
    private final ICornerstoneManager.Stub mBinder = new ICornerstoneManager.Stub() {
		public void onCornerstonePanelFocusChanged(String pkgName, boolean focus, int index){
			Message msg = mHandler.obtainMessage();
			msg.obj = pkgName;
			msg.what = index;
			mHandler.sendMessage(msg);
		}
	};

    private PackageManager mPkgManager;


    /**Activity Event Handlers**/

    @Override
    public void onDestroy() {
        super.onDestroy();
        isCornestoneStarted = false;
    }

    @Override
    public void onResume() {
        super.onResume();
        /**Start the cornerstone apps once the cornerstone itself is visible**/

        if(!isCornestoneStarted) {
	        /**
	         * Author: Onskreen
	         * Date: 06/07/2011
	         *
	         * Launches Cornerstone apps as AsyncTask and shows progress dialog to
	         * the user.
	         */
	         startCornerstoneApps();
	         isCornestoneStarted = true;
        }
    }

    /**
     * Author: Onskreen
     * Date: 04/10/2011
     *
     * Disbales BACK key for CSPanel app
     */
    @Override
    public void onBackPressed() {
        return;
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /**
         * Author: Onskreen
         * Date: 13/07/2011
         *
         * Creates the Sharedpreference if it doesn't exist and store the
         * cs panel packages if they don't exist. It also populates the
         * mCornerstoneApps list which can be used across the class.
         */
        SharedPreferences settings = getSharedPreferences(CS_PREFS, 4);
        String panel0 = settings.getString("panel0", null);
        if(panel0 == null){
            SharedPreferences.Editor editor = settings.edit();
            editor.putString("panel0", "com.onskreen.cornerstone.camera");
            // Commit the edits!
            editor.commit();
            panel0 = "com.onskreen.cornerstone.camera";
        }

        mCornerstoneApps = new String[1];
        mCornerstoneApps[0] =  panel0;

        /**
         * Author: Onskreen
         * Date: 20/07/2011
         *
         * populate the CLauncher package and class names
         * from the cornerstone.xml
         */
        if(mCSLauncherPkgName == null && mCSLauncherClassName == null){
            XmlResourceParser xpp = null;
            try {
                Resources res = this.getResources();
                xpp = res.getXml(com.android.internal.R.xml.cornerstone);
                xpp.next();
                int eventType = xpp.getEventType();
                String tag;
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if(eventType == XmlPullParser.START_DOCUMENT) {
                    } else if(eventType == XmlPullParser.START_TAG) {
                         tag = xpp.getName();
                         if(tag.equals("launcher")) {
                             xpp.next();
                             tag = xpp.getName();
                             if(tag.equals("pkg")){
                                xpp.next();
                                mCSLauncherPkgName = xpp.getText();
                                xpp.next();
                             }
                             xpp.next();
                             tag = xpp.getName();
                             if(tag.equals("class")){
                                xpp.next();
                                mCSLauncherClassName = xpp.getText();
                                xpp.next();
                             }
                             break;
                         }
                    }
                    eventType = xpp.next();
                }
                xpp.close();
             } catch (XmlPullParserException e) {
                  e.printStackTrace();
                  xpp.close();
             } catch (IOException e) {
                 e.printStackTrace();
                 xpp.close();
             }
        }

        /**
         * Author: Onskreen
         * Date: 19/04/2011
         *
         * sets the contentview with appropriate layout file as per the current
         * configuration.
         */
        Configuration config = getApplicationContext().getResources().getConfiguration();
        if (config != null){
            int orientation = config.orientation;
            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                // Portrait
                setContentView(R.layout.portrait_main);
            } else if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                // Landscape
                setContentView(R.layout.landscape_main);
            }
        }

        /**
         * Author: Onskreen
         * Date: 25/02/2011
         *
         * Initialize the UI controls at startup of the app
         */
        initializeControls();

       /**
        * Author: Onskreen
        * Date: 25/02/2011
        *
        * Update the UI controls in UI thread when WMS notifies the cornerstone app
        */
		mHandler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
                int index = msg.what;

                switch (index) {
                    case 0:
                        setCornerstoneAppControls(0);
                        break;
                    default:
                        setCornerstoneAppControls(-1);
                        break;
                }
		}
	};

       /**
        * Author: Onskreen
        * Date: 28/02/2011
        *
        * Registers the ICornerstoneManager interface with AMS, so that AMS can notify conerstone app
        * whenever user changes the focus
        */
		try{
			ActivityManagerNative.getDefault().setCornerstoneManager(mBinder);
		} catch (RemoteException e){
			e.printStackTrace();
		}
    }

    /**
     * Author: Onskreen
     * Date: 19/04/2011
     *
     * In AndroidManifest.xml file, we've set the android:configChanges=android:configChanges="orientation|keyboardHidden"
     * flag so that Framework doesn't restart this activity whenever orientation changes. Framework notifies the activities
     * by onConfigurationChanged notification method.
     *
     * To turn off the orientation handling commet out this method as well as comment out the android:configChanges in manifest
     * file.
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        String header1 = (String)csAppHeader1.getTag();

        super.onConfigurationChanged(newConfig);
        if (newConfig != null){
            int orientation = newConfig.orientation;
            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                // Portrait
                setContentView(R.layout.portrait_main);
            } else if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                // Landscape
                setContentView(R.layout.landscape_main);
            }

            initializeControls();

            switch(csState) {
                case OPEN:
    				if(header1 != null && header1.equals("focused")){
    				    setCornerstoneAppControls(0);
    				} else {
    				    setCornerstoneAppControls(-1);
    				}
    				break;
                case CLOSED:
    				setCornerstoneAppControls(-1);
    				break;
            }
        }
    }

    /**
     * Author: Onskreen
     * Date: 20/04/2011
     *
     * Utility method to initialize the cs panel controls.
     */
    private void initializeControls() {
		csAppHeader1 = (ImageButton) findViewById(R.id.cs_app_header1);
		OnClickListener appHeader1Listener = new OnClickListener(){
		    @Override
		    public void onClick(View v) {
				handleFocus(v);
		    }
		};
		csAppHeader1.setOnClickListener(appHeader1Listener);

		mPkgManager = getApplicationContext().getPackageManager();
    }

	public void launchApp(View view) {
		int index = -1;

	   /**
		* Author: Onskreen
		* Date: 08/03/2011
		*
		* Launches the CSLauncher app to show the list of installed apps in CSPanel view.
		*/
		Intent intent = new Intent();
		ComponentName cp = new ComponentName(mCSLauncherPkgName, mCSLauncherClassName);
		intent.setComponent(cp);
		ActivityLauncher aLauncher = new ActivityLauncher(intent, index);
		aLauncher.launch();
	}

    public static void togglePanel() {
        if (csState == Cornerstone_State.OPEN) {
            try {
                ActivityManagerNative.getDefault().setCornerstoneState(false);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            csState = Cornerstone_State.CLOSED;
        } else if (csState == Cornerstone_State.CLOSED) {
            try {
                ActivityManagerNative.getDefault().setCornerstoneState(true);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            csState = Cornerstone_State.OPEN;
        }
        Log.v(TAG, "Cornerstone panels toggled with status " + csState);
    }

	/**
	 * Author: Onskreen
	 * Date: 12/04/2011
	 *
	 * Handles the focus based on the user input
	 */
	public void handleFocus(View view) {
		int index = -1;
		switch(view.getId()){
			case R.id.cs_app_header1:
				index = 0;
				break;
		}
		try {
			ActivityManagerNative.getDefault().setCornerstoneFocusedApp(index);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Author: Onskreen
	 * Date: 28/02/2011
	 *
	 * A utility method to update the cornerstone app controls based on the requested
	 * panel index.
	 */
	private void setCornerstoneAppControls(int panelIndex){
	  switch(panelIndex){
		case 0:
			csAppHeader1.setTag("focused");
			break;
		default:
			csAppHeader1.setTag("unfocused");
			break;
		}
	}

	private void startCornerstoneApps() {
		if(DEBUG) {
			Log.v(TAG, "Starting original CS Apps: " + mCornerstoneApps);
		}

		Context context = getApplicationContext();
		int duration = Toast.LENGTH_SHORT;

		/**
		 * Author: Onskreen
		 * Date: 06/04/2011
		 *
		 * Added safety checks in case intent doesn't actually exist on device. Display toast for now for debugging purposes,
		 * not intended to be final UI.
		 */
		for(int i=0; i<mCornerstoneApps.length; i++) {
		    Intent currIntent = mPkgManager.getLaunchIntentForPackage(mCornerstoneApps[i]);
		    if(currIntent == null) {
		        /**
			     * Author: Onskreen
			     * Date: 11/01/2011
			     *
			     * Start CS Launcher if the default startup cs apps don't exist
			     * on device or framework not able to find the intent.
			     */
				Intent intent = new Intent();
				ComponentName cp = new ComponentName(mCSLauncherPkgName, mCSLauncherClassName);
				intent.setComponent(cp);
				ActivityLauncher aLauncher = new ActivityLauncher(intent, i);
				aLauncher.launch();
			} else {
			    Toast toast = Toast.makeText(context, "Cornerstone Launching. Please wait...", duration);
	            toast.show();
				ActivityLauncher aLauncher = new ActivityLauncher(currIntent, i);
				aLauncher.launch();
	        }
	    }
	}

	/**
	 * Author: Onskreen
	 * Date: 06/07/2011
	 *
	 * Launches Cornerstone apps and displays the progress dialog till the
	 * Cornerstone apps launched and visible to the user.
	 */
	 private class launchCornerstoneApps extends AsyncTask<Void,Void,Void> {

	    private final ProgressDialog dialog = new ProgressDialog(CSPanel.this);
	    private final long sleepTime = 1500;

	    @Override
        protected void onPreExecute() {
	        dialog.setMessage("Loading. Please wait...");
	        dialog.show();
	    }

	    @Override
        protected Void doInBackground(Void... params) {
	        try{
	            startCornerstoneApps();
	            Thread.sleep(sleepTime);
	        } catch (InterruptedException e) {
	            dialog.dismiss();
	        }
	        return null;
	    }

	    @Override
        protected void onPostExecute(Void unused) {
	        dialog.dismiss();
	    }
	}

	/**
	 * Author: Onskreen
	 * Date: 02/05/2011
	 *
	 * Launch intents in separate thread to avoid ANR crashing the CS.
	 *
	 */
	private class ActivityLauncher {
		private final Intent intent;
		private final int index;

		public ActivityLauncher(Intent intent, int index) {
			this.intent = intent;
			this.index = index;
			if(DEBUG) {
				Log.v(TAG, "ActivityLauncher - Preparing to Launch: " + intent + " at Index: " + index);
			}
		}

		public void launch() {
			new Thread(new Runnable() {
		        @Override
                public void run() {
					try {
						if(DEBUG) {
							Log.v(TAG, "ActivityLauncher - Launching: " + intent);
						}
						ActivityManagerNative.getDefault().startCornerstoneApp(intent, index);
					} catch (RemoteException e) {
						e.printStackTrace();
					}
				}
			}).start();
		}
	}
}
