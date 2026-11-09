package com.winlator.cmod.contentdialog;

import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.Spinner;
import com.winlator.cmod.R;

import android.content.Context;
import com.winlator.cmod.core.AppUtils;
import com.winlator.cmod.core.KeyValueSet;
import com.winlator.cmod.core.StringUtils;

public class DisplayXConfigDialog extends ContentDialog {
    public static String DEFAULT_CONFIG = "backend=x11" + ",performanceMode=1" + ",surfaceFormat=rgba8" + ",presentRR=0" + ",presentMode=auto";
    private Context context;
    
    public DisplayXConfigDialog(View anchor) {
        super(anchor.getContext(), R.layout.displayx_config_dialog);
        context = anchor.getContext();
        setIcon(R.drawable.icon_settings);
        setTitle("DisplayX " + context.getString(R.string.configuration));
        
        final Spinner sSurfaceFormat = findViewById(R.id.SSurfaceFormat);
        final Spinner sBackend = findViewById(R.id.SBackend);
        final Spinner sPresentMode = findViewById(R.id.SPresentMode);
        final CheckBox cbEnablePerfMode = findViewById(R.id.CBEnablePerfMode);
        final CheckBox cbSyncRR = findViewById(R.id.CBPresentRR);
        
        String tag = anchor.getTag().toString();
        KeyValueSet config = parseConfig(anchor.getTag());
        
        AppUtils.setSpinnerSelectionFromIdentifier(sSurfaceFormat, config.get("surfaceFormat"));
        
        String backend = StringUtils.parseIdentifier(config.get("backend"));
        AppUtils.setSpinnerSelectionFromIdentifier(sBackend, backend);
        AppUtils.setSpinnerSelectionFromIdentifier(sPresentMode, config.get("presentMode"));
        
        if (backend.equals("displayx")) {
            sPresentMode.setEnabled(true);
            sPresentMode.setVisibility(View.VISIBLE);
        }    
        else {
            sPresentMode.setEnabled(false);
            sPresentMode.setVisibility(View.GONE);
        }     
        
        sBackend.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedItem = StringUtils.parseIdentifier(sBackend.getSelectedItem().toString());
                if (selectedItem.equals("displayx")) {
                    sPresentMode.setEnabled(true);
                    sPresentMode.setVisibility(View.VISIBLE);
                }    
                else {
                    sPresentMode.setEnabled(false);
                    sPresentMode.setVisibility(View.GONE);
                 }   
                    
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        
        cbEnablePerfMode.setChecked(config.get("performanceMode").equals("1") ? true : false);
        cbSyncRR.setChecked(config.get("presentRR").equals("1") ? true : false);
        
        setOnConfirmCallback(() -> {
            config.put("surfaceFormat", sSurfaceFormat.getSelectedItem().toString());
            config.put("backend", StringUtils.parseIdentifier(sBackend.getSelectedItem().toString()));
            config.put("presentMode", sPresentMode.getSelectedItem().toString());
            config.put("performanceMode", cbEnablePerfMode.isChecked() ? "1" : "0");
            config.put("presentRR", cbSyncRR.isChecked() ? "1" : "0");
            anchor.setTag(config.toString());
        });
    }
    
    public static KeyValueSet parseConfig(Object config) {
        String data = config != null && !config.toString().isEmpty() ? config.toString() :  DEFAULT_CONFIG;
        return new KeyValueSet(data);
    }
}
