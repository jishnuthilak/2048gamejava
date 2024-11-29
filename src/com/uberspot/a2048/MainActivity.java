package com.uberspot.a2048;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import java.util.Locale;

import de.cketti.changelog.dialog.DialogChangeLog;

public class MainActivity extends Activity {

    private static final String MAIN_ACTIVITY_TAG = "2048_MainActivity";

    private WebView mWebView;
    private long mLastBackPress;
    private static final long mBackPressThreshold = 3500;
    private static final String IS_FULLSCREEN_PREF = "is_fullscreen_pref";
    private long mLastTouch;
    private static final long mTouchThreshold = 2000;
    private Toast pressBackToast;

    @SuppressLint({"SetJavaScriptEnabled", "ShowToast", "ClickableViewAccessibility"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Don't show an action bar or title
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // Enable hardware acceleration
        getWindow().setFlags(LayoutParams.FLAG_HARDWARE_ACCELERATED,
                LayoutParams.FLAG_HARDWARE_ACCELERATED);

        // Apply previous setting about showing status bar or not
        applyFullScreen(isFullScreen());

        // Check if screen rotation is locked in settings
        boolean isOrientationEnabled = false;
        try {
            isOrientationEnabled = Settings.System.getInt(getContentResolver(),
                    Settings.System.ACCELEROMETER_ROTATION) == 1;
        } catch (SettingNotFoundException e) {
            Log.d(MAIN_ACTIVITY_TAG, "Settings could not be loaded");
        }

        // If rotation isn't locked and it's a LARGE screen then add orientation changes based on sensor
        int screenLayout = getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK;
        if (((screenLayout == Configuration.SCREENLAYOUT_SIZE_LARGE)
                || (screenLayout == Configuration.SCREENLAYOUT_SIZE_XLARGE))
                && isOrientationEnabled) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        }

        setContentView(R.layout.activity_main);

        DialogChangeLog changeLog = DialogChangeLog.newInstance(this);
        if (changeLog.isFirstRun()) {
            changeLog.getLogDialog().show();
        }

        // Load WebView with game
        mWebView = findViewById(R.id.mainWebView);
        WebSettings settings = mWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);

        // If there is a previous instance, restore it in the WebView
        if (savedInstanceState != null) {
            mWebView.restoreState(savedInstanceState);
        } else {
            // Load WebView with the game from assets
            String url = "file:///android_asset/2048/index.html?lang=" + Locale.getDefault().getLanguage();
            Log.d(MAIN_ACTIVITY_TAG, "Loading URL: " + url);
            mWebView.loadUrl(url);
        }

        Toast.makeText(getApplication(), R.string.toggle_fullscreen, Toast.LENGTH_SHORT).show();

        // Set fullscreen toggle on WebView LongClick
        mWebView.setOnTouchListener((v, event) -> {
            long currentTime = System.currentTimeMillis();
            if ((event.getAction() == MotionEvent.ACTION_UP)
                    && (Math.abs(currentTime - mLastTouch) > mTouchThreshold)) {
                boolean toggledFullScreen = !isFullScreen();
                saveFullScreen(toggledFullScreen);
                applyFullScreen(toggledFullScreen);
            } else if (event.getAction() == MotionEvent.ACTION_DOWN) {
                mLastTouch = currentTime;
            }
            return false;
        });

        pressBackToast = Toast.makeText(getApplicationContext(), R.string.press_back_again_to_exit,
                Toast.LENGTH_SHORT);
    }

    @Override
    protected void onResume() {
        super.onResume();
        String url = "file:///android_asset/2048/index.html?lang=" + Locale.getDefault().getLanguage();
        Log.d(MAIN_ACTIVITY_TAG, "Resuming and reloading URL: " + url);
        mWebView.loadUrl(url);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        mWebView.saveState(outState);
    }

    private void saveFullScreen(boolean isFullScreen) {
        Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putBoolean(IS_FULLSCREEN_PREF, isFullScreen);
        editor.apply();
    }

    private boolean isFullScreen() {
        return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(IS_FULLSCREEN_PREF, true);
    }

    private void applyFullScreen(boolean isFullScreen) {
        if (isFullScreen) {
            getWindow().clearFlags(LayoutParams.FLAG_FULLSCREEN);
        } else {
            getWindow().setFlags(LayoutParams.FLAG_FULLSCREEN, LayoutParams.FLAG_FULLSCREEN);
        }
    }

    @Override
    public void onBackPressed() {
        long currentTime = System.currentTimeMillis();
        if (Math.abs(currentTime - mLastBackPress) > mBackPressThreshold) {
            pressBackToast.show();
            mLastBackPress = currentTime;
        } else {
            pressBackToast.cancel();
            super.onBackPressed();
        }
    }
}
