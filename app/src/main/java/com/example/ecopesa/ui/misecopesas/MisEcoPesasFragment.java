package com.example.ecopesa.ui.misecopesas;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.EditText;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.ecopesa.R;
import com.example.ecopesa.databinding.FragmentMisEcopesasBinding;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.HashMap;
import java.util.Map;

public class MisEcoPesasFragment extends Fragment {

    private FragmentMisEcopesasBinding binding;
    private Thread searchThread;
    private final Map<String, String> dispositivos = new HashMap<>();
    private String multicastIp = "239.0.0.1";
    private SharedPreferences prefs;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMisEcopesasBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        prefs = requireContext().getSharedPreferences("devices", Context.MODE_PRIVATE);

        binding.botonBuscar.setOnClickListener(v -> buscarDispositivos());

        cargarDispositivosGuardados();
        return root;
    }

    private void buscarDispositivos() {
        binding.contenedorDispositivos.removeAllViews();
        dispositivos.clear();
        cargarDispositivosGuardados();
        if (searchThread != null) {
            searchThread.interrupt();
        }
        searchThread = new Thread(() -> {
            try {
                byte[] buf = new byte[256];
                InetAddress group = InetAddress.getByName(multicastIp);
                MulticastSocket socket = new MulticastSocket(12345);
                socket.joinGroup(group);
                long start = System.currentTimeMillis();
                while (System.currentTimeMillis() - start < 5000) {
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    socket.receive(packet);
                    String msg = new String(packet.getData(), 0, packet.getLength()).trim();
                    if (msg.startsWith("EcoPesa")) {
                        String[] parts = msg.substring(7).split("-");
                        if (parts.length >= 2) {
                            String kg = parts[0].trim();
                            String ip = parts[1].trim();
                            if (!dispositivos.containsKey(ip)) {
                                String defaultName = "EcoPesa" + kg;
                                if (!prefs.contains(ip)) {
                                    prefs.edit().putString(ip, defaultName).apply();
                                }
                                String name = prefs.getString(ip, defaultName);
                                dispositivos.put(ip, name);
                                requireActivity().runOnUiThread(() -> agregarBoton(ip, name));
                            }
                        }
                    }
                }
                socket.leaveGroup(group);
                socket.close();
            } catch (IOException ignored) {
            }
        });
        searchThread.start();
    }

    private void cargarDispositivosGuardados() {
        for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
            if ("selected_device".equals(entry.getKey())) continue;
            String ip = entry.getKey();
            String name = entry.getValue().toString();
            dispositivos.put(ip, name);
            agregarBoton(ip, name);
        }
    }

    private void agregarBoton(String ip, String nombre) {
        Button btn = new Button(requireContext());
        btn.setText(nombre);
        btn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_ecopesa, 0, 0, 0);
        btn.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString("ip", ip);
            prefs.edit().putString("selected_device", ip).apply();
            Navigation.findNavController(v).navigate(R.id.nav_medir, args);
        });
        btn.setOnLongClickListener(v -> {
            mostrarOpcionesDispositivo(ip, btn);
            return true;
        });
        binding.contenedorDispositivos.addView(btn);
    }

    private void mostrarOpcionesDispositivo(String ip, Button btn) {
        CharSequence[] opciones = {getString(R.string.rename), getString(R.string.delete)};
        new AlertDialog.Builder(requireContext())
                .setItems(opciones, (dialog, which) -> {
                    if (which == 0) {
                        EditText input = new EditText(requireContext());
                        input.setHint(R.string.device_name_hint);
                        input.setText(dispositivos.get(ip));
                        new AlertDialog.Builder(requireContext())
                                .setTitle(R.string.rename)
                                .setView(input)
                                .setPositiveButton(android.R.string.ok, (d, w) -> {
                                    String nuevo = input.getText().toString().trim();
                                    if (nuevo.isEmpty()) nuevo = ip;
                                    dispositivos.put(ip, nuevo);
                                    prefs.edit().putString(ip, nuevo).apply();
                                    btn.setText(nuevo);
                                })
                                .setNegativeButton(android.R.string.cancel, null)
                                .show();
                    } else if (which == 1) {
                        ((LinearLayout) btn.getParent()).removeView(btn);
                        dispositivos.remove(ip);
                        prefs.edit().remove(ip).apply();
                        if (ip.equals(prefs.getString("selected_device", null))) {
                            prefs.edit().remove("selected_device").apply();
                        }
                    }
                })
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (searchThread != null) {
            searchThread.interrupt();
        }
        binding = null;
    }
}
