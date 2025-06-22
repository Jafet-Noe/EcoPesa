package com.example.ecopesa.ui.configuracion;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.ecopesa.databinding.FragmentConfiguracionBinding;

public class ConfiguracionFragment extends Fragment {

    public static final float DEFAULT_FACTOR = 0.006872454162414f;
    private FragmentConfiguracionBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentConfiguracionBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        SharedPreferences prefs = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE);
        float factor = prefs.getFloat("factor", DEFAULT_FACTOR);
        binding.editarFactor.setText(String.valueOf(factor));

        binding.botonGuardarFactor.setOnClickListener(v -> {
            try {
                float nuevo = Float.parseFloat(binding.editarFactor.getText().toString().trim());
                prefs.edit().putFloat("factor", nuevo).apply();
                Toast.makeText(getContext(), "Guardado", Toast.LENGTH_SHORT).show();
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Valor inválido", Toast.LENGTH_SHORT).show();
            }
        });

        binding.botonResetear.setOnClickListener(v -> {
            prefs.edit().putFloat("factor", DEFAULT_FACTOR).apply();
            binding.editarFactor.setText(String.valueOf(DEFAULT_FACTOR));
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
