package com.winlator.cmod.contentdialog;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.winlator.cmod.R;
import com.winlator.cmod.container.Container;
import com.winlator.cmod.container.FileInfo;
import com.winlator.cmod.core.AppUtils;
import com.winlator.cmod.core.Callback;
import com.winlator.cmod.core.FileUtils;
import com.winlator.cmod.core.StringUtils;
import com.winlator.cmod.core.UnitUtils;
import com.winlator.cmod.core.WineUtils;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;

/* loaded from: classes.dex */
public class FileInfoDialog extends ContentDialog {
    public FileInfoDialog(Context context, FileInfo file, Container container) {
        super(context, R.layout.file_info_dialog);
        String formattedType;
        ArrayList<String[]> lines;
        findViewById(R.id.BTCancel).setVisibility(8);
        setTitle(R.string.information);
        setIcon(R.drawable.icon_info);
        String formattedType2 = "";
        switch (file.type) {
            case FILE:
                formattedType2 = context.getString(R.string.file);
                break;
            case DIRECTORY:
                formattedType2 = context.getString(R.string.folder);
                break;
            case DRIVE:
                formattedType2 = context.getString(R.string.drive);
                break;
        }
        ArrayList<String[]> lines2 = new ArrayList<>();
        int i = 0;
        lines2.add(new String[]{context.getString(R.string.type), formattedType2});
        if (file.type == FileInfo.Type.DIRECTORY) {
            lines2.add(new String[]{context.getString(R.string.contains), file.getItemCount() + " " + context.getString(R.string.items)});
        }
        if (file.type == FileInfo.Type.FILE) {
            lines2.add(new String[]{context.getString(R.string.size), StringUtils.formatBytes(file.getSize())});
        } else {
            lines2.add(new String[]{context.getString(R.string.size), "?"});
        }
        if (file.type != FileInfo.Type.DRIVE) {
            lines2.add(new String[]{context.getString(R.string.location), WineUtils.unixToDOSPath(FileUtils.getDirname(file.path), container)});
        }
        Date date = new Date(file.toFile().lastModified());
        String modified = DateFormat.getDateTimeInstance(3, 3).format(date);
        lines2.add(new String[]{context.getString(R.string.modified), modified});
        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.LLContent);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, -2);
        params.width = AppUtils.getPreferredDialogWidth(context);
        linearLayout.setLayoutParams(params);
        ViewGroup.LayoutParams params2 = new LinearLayout.LayoutParams(-2, -2);
        int paddingRight = (int) UnitUtils.dpToPx(8.0f);
        Iterator<String[]> it = lines2.iterator();
        while (it.hasNext()) {
            String[] columns = it.next();
            LinearLayout row = new LinearLayout(context);
            row.setLayoutParams(params2);
            row.setOrientation(i);
            TextView tvLabel = new TextView(context);
            tvLabel.setLayoutParams(params2);
            tvLabel.setPadding(0, 0, paddingRight, 0);
            tvLabel.setTextSize(1, 16.0f);
            tvLabel.setText(columns[0] + ":");
            tvLabel.setTypeface(tvLabel.getTypeface(), 1);
            row.addView(tvLabel);
            final TextView tvValue = new TextView(context);
            tvValue.setLayoutParams(params2);
            tvValue.setTextSize(1, 16.0f);
            tvValue.setMaxLines(1);
            tvValue.setText(columns[1]);
            if (!columns[1].equals("?")) {
                formattedType = formattedType2;
                lines = lines2;
            } else {
                final AtomicLong lastTime = new AtomicLong();
                final AtomicLong totalSize = new AtomicLong();
                formattedType = formattedType2;
                lines = lines2;
                FileUtils.getSizeAsync(file.toFile(), (size) -> {
                    totalSize.addAndGet(size.longValue());
                    long currTime = System.currentTimeMillis();
                    int elapsedTime = (int) (currTime - lastTime.get());
                    if (lastTime.get() == 0 || elapsedTime > 30) {
                        tvValue.post(new Runnable() { 
                            @Override // java.lang.Runnable
                            public final void run() {
                                tvValue.setText(StringUtils.formatBytes(totalSize.get()));
                            }
                        });
                        lastTime.set(currTime);
                    }
                });
            }
            row.addView(tvValue);
            linearLayout.addView(row);
            i = 0;
            formattedType2 = formattedType;
            lines2 = lines;
        }
    }
}