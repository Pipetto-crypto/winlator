package com.winlator.cmod.core;

import android.app.Activity;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.preference.PreferenceManager;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.winlator.cmod.R;
import com.winlator.cmod.math.Mathf;

public class DownloadProgressDialog {
    private final Activity activity;
    private Dialog dialog;

    public DownloadProgressDialog(Activity activity) {
        this.activity = activity;
    }

    private void create() {
        if (dialog != null) return;
        dialog = new Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setContentView(R.layout.download_progress_dialog);
        applyTheme();

        Window window = dialog.getWindow();
        if (window != null) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        }
    }

    private void applyTheme() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
        boolean isDarkMode = sharedPreferences.getBoolean("dark_mode", true);
        LinearLayout container = dialog.findViewById(R.id.LLDownloadContainer);
        TextView title = dialog.findViewById(R.id.TextView);
        TextView progress = dialog.findViewById(R.id.TVProgress);
        if (container != null) {
            container.setBackgroundResource(isDarkMode ? R.drawable.content_dialog_background_dark : R.drawable.preloader_background);
        }
        int textColor = activity.getResources().getColor(isDarkMode ? android.R.color.white : R.color.colorPrimaryDark, activity.getTheme());
        if (title != null) title.setTextColor(textColor);
        if (progress != null) progress.setTextColor(textColor);
    }

    public void show() {
        show(null);
    }

    public void show(int textResId) {
        show(textResId, null);
    }

    public void show(Runnable onCancelCallback) {
        show(0, onCancelCallback);
    }

    public void show(int textResId, final Runnable onCancelCallback) {
        if (isShowing()) return;
        close();
        if (dialog == null) create();

        if (textResId > 0) ((TextView)dialog.findViewById(R.id.TextView)).setText(textResId);

        setProgress(0);
        if (onCancelCallback != null) {
            dialog.findViewById(R.id.BTCancel).setOnClickListener((v) -> onCancelCallback.run());
            dialog.findViewById(R.id.LLBottomBar).setVisibility(View.VISIBLE);
        }
        dialog.show();
    }

    public void setProgress(int progress) {
        if (dialog == null) return;
        progress = Mathf.clamp(progress, 0, 100);
        ((CircularProgressIndicator)dialog.findViewById(R.id.CircularProgressIndicator)).setProgress(progress);
        ((TextView)dialog.findViewById(R.id.TVProgress)).setText(progress+"%");
    }

    public void close() {
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
}
