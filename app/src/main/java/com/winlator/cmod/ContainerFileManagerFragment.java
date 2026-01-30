package com.winlator.cmod;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import com.winlator.cmod.BaseFileManagerFragment;
import com.winlator.cmod.container.Container;
import com.winlator.cmod.container.FileInfo;
import com.winlator.cmod.contentdialog.ContentDialog;
import com.winlator.cmod.contentdialog.FileInfoDialog;
import com.winlator.cmod.core.AppUtils;
import com.winlator.cmod.core.Callback;
import com.winlator.cmod.core.FileUtils;
import com.winlator.cmod.core.StringUtils;
import com.winlator.cmod.core.WineUtils;
import com.winlator.cmod.win32.MSIcon;
import com.winlator.cmod.core.MSLink;
import com.winlator.cmod.win32.PEParser;
import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.Collections;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.FileWriter;
import com.winlator.cmod.xenvironment.ImageFs;
import android.util.Log;

/* loaded from: classes.dex */
public class ContainerFileManagerFragment extends BaseFileManagerFragment<FileInfo> {
    private Container container;
    private final int containerId;
    private String startPath;

    public ContainerFileManagerFragment(int containerId) {
        this(containerId, null);
    }

    public ContainerFileManagerFragment(int containerId, String startPath) {
        this.containerId = containerId;
        this.startPath = startPath;
    }

    @Override // com.winlator.BaseFileManagerFragment, androidx.fragment.app.Fragment
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        container = this.manager.getContainerById(containerId);
        viewStyle = BaseFileManagerFragment.ViewStyle.valueOf(this.preferences.getString("container_file_manager_view_style", "GRID"));
        String str = this.startPath;
        if (str != null) {
            setCurrentWorkingPath(WineUtils.unixToDOSPath(str, container));
            this.startPath = null;
        }
    }

    @Override // com.winlator.BaseFileManagerFragment
    public void refreshContent() {
        super.refreshContent();
        FileInfo parent = !folderStack.isEmpty() ? (FileInfo) folderStack.peek() : null;
        ArrayList<FileInfo> files = loadFiles(container, parent);
        this.recyclerView.setAdapter(new FileInfoAdapter(files));
        this.emptyTextView.setVisibility(files.isEmpty() ? 0 : 8);
        updateActionBarTitle();
    }

    @Override // androidx.fragment.app.Fragment
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.container_file_manager_menu, menu);
        refreshViewStyleMenuItem(menu.findItem(R.id.menu_item_view_style));
    }

    public ArrayList<FileInfo> loadFiles(Container container, FileInfo parent) {
        ArrayList<FileInfo> fileInfos = new ArrayList<>();
        if (parent != null) {
            return parent.list();
        }
        String rootPath = container.getRootDir().getPath();
        fileInfos.add(new FileInfo(container, "C:", rootPath + "/.wine/drive_c", FileInfo.Type.DRIVE));
        for (String[] drive : container.drivesIterator()) {
            fileInfos.add(new FileInfo(container, drive[0]+":",drive[1], FileInfo.Type.DRIVE));
        }
        File userDir = container.getUserDir();
        File documentsDir = new File(userDir, "Documents");
        String name = documentsDir.getName();
        String path = documentsDir.getPath();
        FileInfo.Type fileInfoType = FileInfo.Type.DIRECTORY;
        fileInfos.add(new FileInfo(container, name, path, fileInfoType));
        Collections.sort(fileInfos);
        return fileInfos;
    }

    private void createFolder() {
        clearClipboard();
        if (this.folderStack.isEmpty()) {
            return;
        }
        ContentDialog.prompt(getContext(), R.string.new_folder, null, new Callback() { 
            @Override // com.winlator.core.Callback
            public final void call(Object obj) {
                File file = new File(((FileInfo) folderStack.peek()).toFile(), (String) obj);
                if (file.isDirectory()) {
                    AppUtils.showToast(getContext(), R.string.there_already_file_with_that_name);
                } else {
                    file.mkdir();
                    refreshContent();
                }
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    private void instantiateClipboard(FileInfo file, boolean cutMode) {
        clearClipboard();
        this.clipboard = new BaseFileManagerFragment.Clipboard(new File[]{new File(file.path)}, cutMode);
        this.pasteButton.setVisibility(0);
    }

    /* JADX INFO: Access modifiers changed from: private */
    private void addDesktop(FileInfo file) throws Exception {
        
        Context context = getContext();
         try {
            String displayName = FileUtils.getBasename(file.name);
            String unixPath = file.toFile().getAbsolutePath();
            File shortcutsDir = container.getDesktopDir();
            ImageFs imageFs = ImageFs.find(context);
            if (!shortcutsDir.exists()) shortcutsDir.mkdirs();
            File desktopFile = new File(shortcutsDir, displayName + ".desktop");
            Log.d("unixPath",unixPath);
            try (PrintWriter writer = new PrintWriter(new FileWriter(desktopFile))) {
                writer.println("[Desktop Entry]");
                writer.println("Name=" + displayName);
                writer.println("Exec=env WINEPREFIX=" + "\"" + imageFs.wineprefix + "\"" + " wine " + unixPath);
                writer.println("Type=Application");
                writer.println("container_id:" + container.id);
                writer.close();
            }
            AppUtils.showToast(context, R.string.file_added_to_desktop);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }

    @Override // com.winlator.BaseFileManagerFragment
    protected void pasteFiles() {
        if (folderStack.isEmpty()) {
            clearClipboard();
            AppUtils.showToast(getContext(), R.string.you_cannot_paste_files_here);
        } else {
            this.clipboard.targetDir = ((FileInfo) folderStack.peek()).toFile();
            super.pasteFiles();
        }
    }

    @Override // androidx.fragment.app.Fragment
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        int itemId = menuItem.getItemId();
        switch (itemId) {
            case R.id.menu_item_home /* 2131296730 */:
                folderStack.clear();
                refreshContent();
                return true;
            case R.id.menu_item_new_folder /* 2131296738 */:
                createFolder();
                return true;
            case R.id.menu_item_view_style /* 2131296751 */:
                BaseFileManagerFragment.ViewStyle viewStyle2 = BaseFileManagerFragment.ViewStyle.GRID;
                if (viewStyle == viewStyle2) {
                    viewStyle = BaseFileManagerFragment.ViewStyle.LIST;
                }else{
                    viewStyle = viewStyle2;
                }
                setViewStyle(viewStyle);
                this.preferences.edit().putString("container_file_manager_view_style", viewStyle.name()).apply();
                refreshViewStyleMenuItem(menuItem);
                return true;
            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }

    private void setCurrentWorkingPath(String dosPath) {
        String[] names = StringUtils.removeEndSlash(dosPath).split("\\\\");
        String basePath = "";
        folderStack.clear();
        for (String name : names) {
            if (!name.isEmpty()) {
                String dosPath2 = WineUtils.dosToUnixPath(basePath + name, container);
                if (basePath.isEmpty() && name.matches("[A-Za-z]:")) {
                    folderStack.push(new FileInfo(container, name, dosPath2, FileInfo.Type.DRIVE));
                } else {
                    folderStack.push(new FileInfo(container, dosPath2, FileInfo.Type.DIRECTORY));
                }
                basePath = basePath + name + "\\";
            }
        }
        updateActionBarTitle();
    }

    private String getCurrentWorkingPath() {
        if (!folderStack.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < folderStack.size(); i++) {
                if (i > 0) {
                    sb.append("\\");
                }
                sb.append(((FileInfo) folderStack.elementAt(i)).getDisplayName());
            }
            if (folderStack.size() == 1) {
                sb.append("\\");
            }
            return sb.toString();
        }
        return "";
    }

    private void updateActionBarTitle() {
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        ActionBar actionBar = activity.getSupportActionBar();
        if (!folderStack.isEmpty()) {
            actionBar.setHomeAsUpIndicator(R.drawable.icon_action_bar_back);
            actionBar.setTitle(getCurrentWorkingPath());
        } else {
            actionBar.setHomeAsUpIndicator(R.drawable.icon_action_bar_menu);
            actionBar.setTitle(getHomeTitle());
        }
    }

    @Override // com.winlator.BaseFileManagerFragment
    protected String getHomeTitle() {
        return container.getName();
    }

    /* JADX INFO: Access modifiers changed from: private */
    private class LoadIconTask {
        private boolean canceled;
        private final Executor executor;
        private final WeakReference<ImageView> imageViewWeakRef;

        private LoadIconTask( ImageView imageView) {
            executor = Executors.newSingleThreadExecutor();
            canceled = false;
            imageViewWeakRef = new WeakReference<>(imageView);
        }

        public void loadAsync(final FileInfo file) {
            executor.execute(new Runnable() { 
                @Override // java.lang.Runnable
                public final void run() {
                    final Object icon = getIconForFile(file);
                    final ImageView imageView = imageViewWeakRef.get();
                    if (imageView != null && !canceled) {
                        imageView.post(new Runnable() {
                            @Override // java.lang.Runnable
                            public final void run() {
                                 if (canceled) {
                                    return;
                                 }
                                 if (icon instanceof Bitmap) {
                                    imageView.setImageBitmap((Bitmap) icon);
                                 } else {
                                    imageView.setImageResource(((Integer) icon).intValue());
                                 }
                            }
                        });
                    }
                }
            });
        }
        public void cancel() {
            this.canceled = true;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    private class FileInfoAdapter extends RecyclerView.Adapter<FileInfoAdapter.ViewHolder> {
        private final List<FileInfo> data;

        private class ViewHolder extends RecyclerView.ViewHolder {
            private final ImageView imageView;
            private final ImageView menuButton;
            private final ImageView runButton;
            private final TextView subtitle;
            private final TextView title;

            private ViewHolder(View view) {
                super(view);
                this.imageView = (ImageView) view.findViewById(R.id.ImageView);
                this.title = (TextView) view.findViewById(R.id.TVTitle);
                this.subtitle = (TextView) view.findViewById(R.id.TVSubtitle);
                this.runButton = (ImageView) view.findViewById(R.id.BTRun);
                this.menuButton = (ImageView) view.findViewById(R.id.BTMenu);
            }
        }

        public FileInfoAdapter(List<FileInfo> data) {
            this.data = data;
        }

        @Override // androidx.recyclerview.widget.RecyclerView.Adapter
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            int resource = viewStyle == BaseFileManagerFragment.ViewStyle.LIST ? R.layout.file_list_item : R.layout.file_grid_item;
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(resource, parent, false));
        }

        @Override // androidx.recyclerview.widget.RecyclerView.Adapter
        public void onBindViewHolder(ViewHolder holder, int position) {
            Context context = getContext();
            final FileInfo item = this.data.get(position);
            FileInfo.Type type = item.type;
            if (item.type == FileInfo.Type.DRIVE) {
                String driveText = getContext().getString(R.string.drive);
                holder.title.setText(driveText + " (" + item.name + ")");
            } else {
                MSLink.Options linkInfo = item.getLinkinfo();
                if (linkInfo != null && linkInfo.isDirectory) {
                    type = FileInfo.Type.DIRECTORY;
                }
                holder.title.setText(item.getDisplayName());
            }
            holder.subtitle.setVisibility(8);
            holder.runButton.setImageResource(R.drawable.icon_open);
            if (type == FileInfo.Type.DIRECTORY && !folderStack.isEmpty()) {
                holder.subtitle.setText(item.getItemCount() + " " + context.getString(R.string.items));
                holder.subtitle.setVisibility(0);
            } else if (type == FileInfo.Type.FILE) {
                holder.runButton.setImageResource(R.drawable.icon_run);
                holder.subtitle.setText(StringUtils.formatBytes(item.getSize()));
                holder.subtitle.setVisibility(0);
            }
            if (type == FileInfo.Type.FILE) {
                holder.imageView.setImageResource(R.drawable.container_file);
                LoadIconTask loadIconTask = (LoadIconTask) holder.imageView.getTag();
                if (loadIconTask != null) {
                    loadIconTask.cancel();
                }
                LoadIconTask loadIconTask2 = new LoadIconTask(holder.imageView);
                loadIconTask2.loadAsync(item);
                holder.imageView.setTag(loadIconTask2);
            } else {
                holder.imageView.setImageResource(((Integer) getIconForFile(item)).intValue());
            }
            holder.imageView.setOnClickListener(new View.OnClickListener() { 
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    openFile(item);
                }
            });
            holder.runButton.setOnClickListener(new View.OnClickListener() { 
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    openFile(item);
                }
            });
            holder.menuButton.setOnClickListener(new View.OnClickListener() { 
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    final Context context = getContext();
                    PopupMenu listItemMenu = new PopupMenu(context, view);
                    if (Build.VERSION.SDK_INT >= 29) {
                        listItemMenu.setForceShowIcon(true);
                    }
                    listItemMenu.inflate(R.menu.file_manager_popup_menu);
                    Menu menu = listItemMenu.getMenu();
                    menu.findItem(R.id.menu_item_settings).setVisible(false);
                    if (folderStack.isEmpty()) {
                        menu.findItem(R.id.menu_item_cut).setVisible(false);
                        menu.findItem(R.id.menu_item_remove).setVisible(false);
                        menu.findItem(R.id.menu_item_rename).setVisible(false);
                        menu.findItem(R.id.menu_item_add_desktop).setVisible(false);
                    } else if (!FileUtils.getExtension(item.path).equals("exe")) {
                        menu.findItem(R.id.menu_item_add_desktop).setVisible(false);
                    }
                    listItemMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() { 
                        @Override // android.widget.PopupMenu.OnMenuItemClickListener
                        public final boolean onMenuItemClick(MenuItem menuItem) {
                            int itemId = menuItem.getItemId();
                            switch (itemId) {
                                case R.id.menu_item_add_desktop /* 2131296718 */:
                                    try{
                                        addDesktop(item);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    return true;
                                case R.id.menu_item_copy /* 2131296721 */:
                                case R.id.menu_item_cut /* 2131296722 */:
                                    instantiateClipboard(item, itemId == R.id.menu_item_cut);
                                    return true;
                                case R.id.menu_item_info /* 2131296731 */:
                                    new FileInfoDialog(context, item, container).show();
                                    return true;
                                case R.id.menu_item_remove /* 2131296743 */:
                                    clearClipboard();
                                    ContentDialog.confirm(context, R.string.do_you_want_to_remove_this_file, new Runnable() { 
                                        @Override // java.lang.Runnable
                                        public final void run() {
                                            removeFile(item.toFile());
                                        }
                                    });
                                    return true;
                                case R.id.menu_item_rename /* 2131296744 */:
                                    clearClipboard();
                                    ContentDialog.prompt(context, R.string.rename, item.name, new Callback() { 
                                        @Override // com.winlator.core.Callback
                                        public final void call(Object obj) {
                                            item.renameTo((String) obj);
                                            refreshContent();
                                        }
                                    });
                                    return true;
                                default:
                                    return true;
                            }
                        }
                    });
                    listItemMenu.show();
                }
            });
        }

        @Override // androidx.recyclerview.widget.RecyclerView.Adapter
        public final int getItemCount() {
            return this.data.size();
        }

        private void openFile(FileInfo file) {
            Activity activity = getActivity();
            MSLink.Options linkInfo = file.getLinkinfo();
            boolean isFile = true;
            if (linkInfo == null ? file.type != FileInfo.Type.FILE : linkInfo.isDirectory) {
                isFile = false;
            }
            if (isFile) {
                Intent intent = new Intent(activity, (Class<?>) XServerDisplayActivity.class);
                intent.putExtra("container_id", container.id);
                intent.putExtra("exec_path", file.path);
                activity.startActivity(intent);
                return;
            }
            folderStack.push(file);
            refreshContent();
        }
    }

    private Object getIconForFile(FileInfo file) {
        Bitmap bitmap;
        FileInfo.Type type = file.type;
        FileInfo.Type type2 = FileInfo.Type.DIRECTORY;
        Integer numValueOf = Integer.valueOf(R.drawable.container_folder);
        if (type == type2) {
            Context context = getContext();
            if (file.path.endsWith("xuser/" + context.getString(R.string.documents))) {
                return Integer.valueOf(R.drawable.container_folder_documents);
            }
            return numValueOf;
        }
        if (type == FileInfo.Type.DRIVE) {
            return Integer.valueOf(R.drawable.container_drive);
        }
        String extension = FileUtils.getExtension(file.path);
        switch (extension) {
            case "exe":
                Bitmap bitmap2 = PEParser.extractIcon(file.toFile());
                return bitmap2 != null ? bitmap2 : Integer.valueOf(R.drawable.container_file_window);
            case "bat":
                return Integer.valueOf(R.drawable.container_file_window);
            case "ico":
                Bitmap bitmap3 = MSIcon.decodeFile(file.toFile());
                if (bitmap3 != null) {
                    return bitmap3;
                }
                break;
            case "dll":
                return Integer.valueOf(R.drawable.container_file_library);
            case "lnk":
                MSLink.Options linkInfo = file.getLinkinfo();
                if (linkInfo != null) {
                    if (linkInfo.isDirectory) {
                        return numValueOf;
                    }
                    String targetPath = linkInfo.iconLocation;
                    if (targetPath == null) {
                        targetPath = linkInfo.targetPath;
                    }
                    String targetPath2 = WineUtils.dosToUnixPath(targetPath, container);
                    if (targetPath2.endsWith(".ico")) {
                        bitmap = MSIcon.decodeFile(new File(targetPath2));
                    } else {
                        bitmap = PEParser.extractIcon(new File(targetPath2), linkInfo.iconIndex);
                    }
                    if (bitmap != null) {
                        return bitmap;
                    }
                }
                return Integer.valueOf(R.drawable.container_file_link);
        }
        return Integer.valueOf(R.drawable.container_file);
    }
}