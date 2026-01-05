package com.winlator.cmod;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import com.winlator.cmod.R;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.winlator.cmod.contentdialog.ContentDialog;
import com.winlator.cmod.contents.AdrenotoolsManager;
import java.util.ArrayList;

public class AdrenotoolsFragment extends Fragment {
    
    private AdrenotoolsManager adrenotoolsManager;
    private RecyclerView recyclerView;
    
    @Override 
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true); // <--- OBRIGATÓRIO PARA O BOTÃO APARECER
        this.adrenotoolsManager = new AdrenotoolsManager(getActivity());
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup layout = (ViewGroup)inflater.inflate(R.layout.adrenotools_fragment, container, false);
        recyclerView = layout.findViewById(R.id.RecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(recyclerView.getContext()));
        recyclerView.addItemDecoration(new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL));
        recyclerView.setAdapter(new DriversAdapter(adrenotoolsManager.enumarateInstalledDrivers()));
        
        // Botão original de instalar arquivo local
        View btInstallDriver = layout.findViewById(R.id.BTInstallDriver);
        btInstallDriver.setOnClickListener((v) -> {
            ContentDialog.confirm(getContext(), getString(R.string.install_drivers_message) + " " + getString(R.string.install_drivers_warning), () -> {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                getActivity().startActivityFromFragment(this, intent, MainActivity.OPEN_FILE_REQUEST_CODE);               
            });
        });
        return layout;
    }
    
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.adrenotools_gpu_drivers);
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == MainActivity.OPEN_FILE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Uri uri = data.getData();
            String driver = adrenotoolsManager.installDriver(uri);
            if (!driver.isEmpty())
                ((DriversAdapter)recyclerView.getAdapter()).addItem(driver);
        }
     }       

    // --- CÓDIGO DO MENU DE DOWNLOAD (NOVO) ---
    @Override
    public void onCreateOptionsMenu(android.view.Menu menu, android.view.MenuInflater inflater) {
        // Cria um botão de menu "Baixar" com ícone de nuvem/download
        android.view.MenuItem item = menu.add(0, 1, 0, "Baixar");
        item.setIcon(android.R.drawable.stat_sys_download); 
        item.setShowAsAction(android.view.MenuItem.SHOW_AS_ACTION_ALWAYS);
    }

@Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == 1) {
            // AGORA ABRE O GERENCIADOR DE REPOSITÓRIOS
            com.winlator.cmod.contentdialog.RepositoryManagerDialog repoDialog = new com.winlator.cmod.contentdialog.RepositoryManagerDialog(getContext());
            
            // Callback: Quando fechar tudo, recarrega a lista do fragmento
            repoDialog.setOnDismissCallback(() -> {
                if (recyclerView != null && recyclerView.getAdapter() instanceof DriversAdapter) {
                    ((DriversAdapter) recyclerView.getAdapter()).reloadList();
                }
            });
            
            repoDialog.show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    // ----------------------------------------

    private class DriversAdapter extends RecyclerView.Adapter<DriversAdapter.ViewHolder> {
        private ArrayList<String> driversList;

        public class ViewHolder extends RecyclerView.ViewHolder {
            private TextView tvName;
            private TextView tvVersion;
            private ImageButton btMenu;

            public ViewHolder(View v) {
                super(v);
                tvName = v.findViewById(R.id.TVName);
                tvVersion = v.findViewById(R.id.TVVersion);
                btMenu = v.findViewById(R.id.BTMenu);
            }
        }
        
        public DriversAdapter(ArrayList<String> driversList) {
            this.driversList = driversList;
        }

        // Método para atualizar a lista automaticamente
        public void reloadList() {
            this.driversList = adrenotoolsManager.enumarateInstalledDrivers();
            notifyDataSetChanged();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.adrenotools_list_item, viewGroup, false);
            return new ViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(ViewHolder viewHolder, final int position) {
            viewHolder.tvName.setText(adrenotoolsManager.getDriverName(driversList.get(position)));
            viewHolder.tvVersion.setText(adrenotoolsManager.getDriverVersion(driversList.get(position)));
            viewHolder.btMenu.setOnClickListener((v) -> {
                removeAtIndex(position);
            });
        }
        
        public void addItem(String item) {
            driversList.add(item);
            notifyItemInserted(getItemCount() - 1);
        }
        
        public void removeAtIndex(int index) {
            String deletedDriver = driversList.remove(index);
            adrenotoolsManager.removeDriver(deletedDriver);
            notifyItemRemoved(index);
            notifyItemRangeChanged(index, getItemCount());
        }
        
        @Override
        public int getItemCount() {
            return driversList.size();
        }
    }
}