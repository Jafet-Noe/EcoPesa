package com.example.ecopesa.ui.medir;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.content.SharedPreferences;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.ecopesa.databinding.FragmentMedirBinding;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MedirFragment extends Fragment {

    private FragmentMedirBinding binding;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private String ipDispositivo;
    private String nombreDispositivo;
    private static final float DEFAULT_FACTOR = 0.006872454162414f;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        MedirViewModel medirViewModel =
                new ViewModelProvider(this).get(MedirViewModel.class);

        binding = FragmentMedirBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView valueView = binding.valorProgreso;
        medirViewModel.getTexto().observe(getViewLifecycleOwner(), valueView::setText);

        SharedPreferences prefs = requireContext().getSharedPreferences("devices", Context.MODE_PRIVATE);
        if (getArguments() != null) {
            ipDispositivo = getArguments().getString("ip");
            prefs.edit().putString("selected_device", ipDispositivo).apply();
        } else {
            ipDispositivo = prefs.getString("selected_device", null);
        }
        nombreDispositivo = prefs.getString(ipDispositivo, ipDispositivo);
        requireActivity().setTitle(nombreDispositivo);

        binding.botonMedir.setAllCaps(false);
        binding.botonMedir.setOnClickListener(v -> solicitarMedida());
        actualizarValor();
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        executor.shutdownNow();
        binding = null;
    }

    private void actualizarValor() {
        if (ipDispositivo == null) return;
        executor.execute(() -> {
            try {
                URL url = new URL("http://" + ipDispositivo + ":8080/");
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");
                BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String numero = br.readLine();
                br.close();
                requireActivity().runOnUiThread(() -> mostrarNumero(numero));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void solicitarMedida() {
        if (ipDispositivo == null) {
            Toast.makeText(getContext(), "Sin dispositivo", Toast.LENGTH_SHORT).show();
            return;
        }
        executor.execute(() -> {
            try {
                URL url = new URL("http://" + ipDispositivo + ":8080/");
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");
                con.getInputStream().close();
                actualizarValor();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void mostrarNumero(String numero) {
        SharedPreferences prefs = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE);
        double factor = prefs.getFloat("factor", DEFAULT_FACTOR);
        try {
            double valor = Double.parseDouble(numero.trim());
            double resultado = valor * factor;
            int progreso = (int)resultado/1000;
            String texto = null;
            if(resultado < 1000){
                texto = String.format("%.2f g", resultado);
            }else{
                texto = String.format("%.2f Kg", resultado/(double)1000);
            }
            binding.valorProgreso.setText(texto);
            binding.progresoMedir.setProgressCompat((int)progreso, true);
        } catch (NumberFormatException e) {
            binding.valorProgreso.setText("Error con numero "+numero);
        }
    }
}
