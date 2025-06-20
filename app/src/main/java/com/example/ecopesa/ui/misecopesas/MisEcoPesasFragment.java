package com.example.ecopesa.ui.misecopesas;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

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

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMisEcopesasBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        binding.botonBuscar.setOnClickListener(v -> buscarDispositivos());
        return root;
    }

    private void buscarDispositivos() {
        binding.contenedorDispositivos.removeAllViews();
        dispositivos.clear();
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
                                dispositivos.put(ip, kg);
                                requireActivity().runOnUiThread(() -> agregarBoton(ip, kg));
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

    private void agregarBoton(String ip, String kg) {
        Button btn = new Button(requireContext());
        btn.setText(kg + " kg");
        btn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_ecopesa, 0, 0, 0);
        btn.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString("ip", ip);
            Navigation.findNavController(v).navigate(R.id.nav_medir, args);
        });
        btn.setOnLongClickListener(v -> {
            ((LinearLayout) v.getParent()).removeView(v);
            dispositivos.remove(ip);
            return true;
        });
        binding.contenedorDispositivos.addView(btn);
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
