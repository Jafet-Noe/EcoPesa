package com.example.ecopesa.ui.informacion;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.ecopesa.databinding.FragmentInformacionBinding;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class InformacionFragment extends Fragment {

    private FragmentInformacionBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        InformacionViewModel informacionViewModel =
                new ViewModelProvider(this).get(InformacionViewModel.class);

        binding = FragmentInformacionBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textoInformacion;
        informacionViewModel.getTexto().observe(getViewLifecycleOwner(), textView::setText);

        informacionViewModel.cargarDatos(leerYaml("informacion.yml"));
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private Map<String, String> leerYaml(String nombre) {
        Map<String, String> datos = new HashMap<>();
        try {
            InputStream is = requireContext().getAssets().open(nombre);
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String linea;
            while ((linea = br.readLine()) != null) {
                String[] partes = linea.split(":");
                if (partes.length >= 2) {
                    datos.put(partes[0].trim(), linea.substring(linea.indexOf(":") + 1).trim());
                }
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return datos;
    }
}
