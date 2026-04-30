package com.winlator.cmod.core;

import android.app.Activity;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.preference.PreferenceManager;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.winlator.cmod.R;

public class PreloaderDialog {
    private final Activity activity;
    private Dialog dialog;

    public PreloaderDialog(Activity activity) {
        this.activity = activity;
    }

    private void create() {
        if (dialog != null) return;
        dialog = new Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setContentView(R.layout.preloader_dialog);
        applyTheme();

        Window window = dialog.getWindow();
        if (window != null) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        }
    }

    public synchronized void show(int textResId) {
        if (isShowing()) return;
        close();
        if (dialog == null) create();
        ((TextView)dialog.findViewById(R.id.TextView)).setText(textResId);
        boolean transparentMode = textResId == R.string.starting_up || textResId == R.string.shutdown;
        applyTheme(transparentMode);
        CircularProgressIndicator progressBar = dialog.findViewById(R.id.CircularProgressIndicator);
        TextView tvProgress = dialog.findViewById(R.id.TVProgress);
        TextView tvProgressValue = dialog.findViewById(R.id.TVProgressValue);
        if (progressBar != null) {
            int trackColor = transparentMode ? android.graphics.Color.TRANSPARENT : android.graphics.Color.parseColor("#5f5f5f");
            progressBar.setTrackColor(trackColor);
            progressBar.setTrackThickness(transparentMode ? 4 : 7);
            progressBar.setIndicatorSize(transparentMode ? 44 : 72);
            applyIndicatorMode(progressBar, true);
            progressBar.setProgress(0);
        }
        if (tvProgress != null) tvProgress.setText("");
        if (tvProgressValue != null) tvProgressValue.setText("");
        dialog.show();
    }

    public void showOnUiThread(final int textResId) {
        activity.runOnUiThread(() -> show(textResId));
    }

    public synchronized void close() {
        try {
            if (dialog != null) {
                dialog.dismiss();
            }
        }
        catch (Exception e) {}
    }

    public void closeOnUiThread() {
        activity.runOnUiThread(this::close);
    }

    public boolean isShowing() {
        return dialog != null && dialog.isShowing();
    }

    public void setProgress(int progress) {
        if (dialog == null) return;
        CircularProgressIndicator progressBar = dialog.findViewById(R.id.CircularProgressIndicator);
        TextView tvProgress = dialog.findViewById(R.id.TVProgress);
        TextView tvProgressValue = dialog.findViewById(R.id.TVProgressValue);
        if (progressBar != null) {
            applyIndicatorMode(progressBar, false);
            progressBar.setMax(100);
            progressBar.setProgress(Math.max(0, Math.min(progress, 100)));
        }
        if (tvProgress != null) tvProgress.setText(progress + "%");
        if (tvProgressValue != null) tvProgressValue.setText("");
    }

    public void setProgress(int progress, long downloadedBytes, long totalBytes) {
        if (dialog == null) return;
        CircularProgressIndicator progressBar = dialog.findViewById(R.id.CircularProgressIndicator);
        TextView tvProgress = dialog.findViewById(R.id.TVProgress);
        TextView tvProgressValue = dialog.findViewById(R.id.TVProgressValue);
        if (progressBar != null) {
            applyIndicatorMode(progressBar, false);
            progressBar.setMax(100);
            progressBar.setProgress(Math.max(0, Math.min(progress, 100)));
        }
        if (tvProgress != null) tvProgress.setText(progress + "%");
        if (tvProgressValue != null) {
            float downloadedMB = downloadedBytes / (1024f * 1024f);
            float totalMB = totalBytes > 0 ? totalBytes / (1024f * 1024f) : 0f;
            if (totalMB > 0f) tvProgressValue.setText(String.format(java.util.Locale.US, "%d%%  (%.1f / %.1f MB)", progress, downloadedMB, totalMB));
            else tvProgressValue.setText(String.format(java.util.Locale.US, "%d%%  (%.1f MB)", progress, downloadedMB));
        }
    }

    public void setText(int textResId) {
        if (dialog == null) return;
        TextView textView = dialog.findViewById(R.id.TextView);
        if (textView != null) textView.setText(textResId);
    }

    public void setIndeterminate(boolean indeterminate) {
        if (dialog == null) return;
        CircularProgressIndicator progressBar = dialog.findViewById(R.id.CircularProgressIndicator);
        TextView tvProgress = dialog.findViewById(R.id.TVProgress);
        if (progressBar != null) applyIndicatorMode(progressBar, indeterminate);
        if (indeterminate && tvProgress != null) tvProgress.setText("");
    }

    private void applyIndicatorMode(CircularProgressIndicator progressBar, boolean indeterminate) {
        if (progressBar.isIndeterminate() == indeterminate) return;
        boolean wasVisible = progressBar.getVisibility() == android.view.View.VISIBLE;
        if (wasVisible) progressBar.setVisibility(android.view.View.INVISIBLE);
        progressBar.setIndeterminate(indeterminate);
        if (wasVisible) progressBar.setVisibility(android.view.View.VISIBLE);
    }

    private void applyTheme() {
        applyTheme(false);
    }

    private void applyTheme(boolean transparentStartupMode) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
        boolean isDarkMode = sharedPreferences.getBoolean("dark_mode", true);
        LinearLayout container = dialog.findViewById(R.id.LLPreloaderContainer);
        TextView textView = dialog.findViewById(R.id.TextView);
        TextView progressText = dialog.findViewById(R.id.TVProgress);
        TextView progressValue = dialog.findViewById(R.id.TVProgressValue);
        if (container != null) {
            if (transparentStartupMode) {
                container.setBackgroundResource(android.R.color.transparent);
            } else {
                container.setBackgroundResource(isDarkMode ? R.drawable.content_dialog_background_dark : R.drawable.content_dialog_background);
            }
        }
        int textColor = activity.getResources().getColor(isDarkMode ? android.R.color.white : R.color.colorPrimaryDark, activity.getTheme());
        if (textView != null) textView.setTextColor(textColor);
        if (progressText != null) progressText.setTextColor(textColor);
        if (progressValue != null) progressValue.setTextColor(textColor);
    }
}
