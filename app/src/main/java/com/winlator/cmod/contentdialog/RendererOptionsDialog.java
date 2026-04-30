package com.winlator.cmod.contentdialog;

import android.content.Context;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;

import com.winlator.cmod.R;

public class RendererOptionsDialog extends ContentDialog {

    private final boolean isNativeMode;

    private void setGroupVisibility(int id, int vis) {
        View v = findViewById(id);
        if (v != null) v.setVisibility(vis);
    }

    public interface Config {
        boolean getRendererNative();
        void setRendererNative(boolean v);

        String getRendererPresentMode();
        void setRendererPresentMode(String v);

        String getRendererDriverId();
        void setRendererDriverId(String v);

        int getRendererFilterMode();
        void setRendererFilterMode(int v);

        int getRendererRefreshRateLimit();
        void setRendererRefreshRateLimit(int v);

        boolean getRendererSwapRB();
        void setRendererSwapRB(boolean v);
    }

    private static final String[] PRESENT_MODE_IDS    = {"fifo", "mailbox"};
    private static final String[] PRESENT_MODE_LABELS = {
        "Fifo",
        "Mailbox"
    };

    private static final String[] FILTER_LABELS = {
        "Bilinear",
        "Nearest neighbor"
    };
    private static final int[] REFRESH_RATE_VALUES = {60, 0};
    private static final String[] REFRESH_RATE_LABELS = {"60 Hz", "Device Refresh Rate"};

    public RendererOptionsDialog(View anchorView, Config config, boolean isNativeMode) {
        super(anchorView.getContext(), R.layout.renderer_options_dialog);
        this.isNativeMode = isNativeMode;
        setTitle("Renderer Options");
        setIcon(R.drawable.icon_monitor);

        Context ctx = anchorView.getContext();

        Spinner  spPresent = findViewById(R.id.SPRendererPresentMode);
        Spinner  spFilter  = findViewById(R.id.SPRendererFilter);
        Spinner  spRefresh = findViewById(R.id.SPRendererRefreshRate);
        CheckBox cbSwapRB  = findViewById(R.id.CBRendererSwapRB);

        setGroupVisibility(R.id.GroupDriver,  View.GONE);
        setGroupVisibility(R.id.GroupFilter,  View.VISIBLE);

        spPresent.setAdapter(new ArrayAdapter<>(ctx,
            android.R.layout.simple_spinner_dropdown_item, PRESENT_MODE_LABELS));
        int pmSel = 0;
        String curPm = config.getRendererPresentMode();
        for (int i = 0; i < PRESENT_MODE_IDS.length; i++) {
            if (PRESENT_MODE_IDS[i].equals(curPm)) { pmSel = i; break; }
        }
        spPresent.setSelection(pmSel);

        String forcedDriverId = "";

        spFilter.setAdapter(new ArrayAdapter<>(ctx,
            android.R.layout.simple_spinner_dropdown_item, FILTER_LABELS));
        int filterSel = config.getRendererFilterMode();
        if (filterSel < 0 || filterSel >= FILTER_LABELS.length) filterSel = 0;
        spFilter.setSelection(filterSel);

        spRefresh.setAdapter(new ArrayAdapter<>(ctx,
            android.R.layout.simple_spinner_dropdown_item, REFRESH_RATE_LABELS));
        int rrSel = 0;
        int currentRefresh = config.getRendererRefreshRateLimit();
        for (int i = 0; i < REFRESH_RATE_VALUES.length; i++) {
            if (REFRESH_RATE_VALUES[i] == currentRefresh) { rrSel = i; break; }
        }
        spRefresh.setSelection(rrSel);
        cbSwapRB.setChecked(config.getRendererSwapRB());

        setOnConfirmCallback(() -> {
            config.setRendererPresentMode(PRESENT_MODE_IDS[spPresent.getSelectedItemPosition()]);
            config.setRendererDriverId(forcedDriverId);
            config.setRendererFilterMode(spFilter.getSelectedItemPosition());
            config.setRendererRefreshRateLimit(REFRESH_RATE_VALUES[spRefresh.getSelectedItemPosition()]);
            config.setRendererSwapRB(cbSwapRB.isChecked());
        });
    }

    public static int toVkPresentMode(String mode) {
        if (mode == null) return 2;
        switch (mode) {
            case "mailbox":       return 1;
            default:              return 2;
        }
    }
}
