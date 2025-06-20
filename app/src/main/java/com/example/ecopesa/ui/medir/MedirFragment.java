package com.example.ecopesa.ui.medir;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.ecopesa.databinding.FragmentMedirBinding;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class MedirFragment extends Fragment {

    private FragmentMedirBinding binding;
    private SimpleHttpServer servidor;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        MedirViewModel medirViewModel =
                new ViewModelProvider(this).get(MedirViewModel.class);

        binding = FragmentMedirBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textoMedir;
        medirViewModel.getTexto().observe(getViewLifecycleOwner(), textView::setText);

        servidor = new SimpleHttpServer(numero ->
                requireActivity().runOnUiThread(() -> {
                    binding.textoMedir.setText(numero + " kg");
                    try {
                        int valor = Integer.parseInt(numero.trim());
                        binding.progresoMedir.setProgress(valor, true);
                    } catch (NumberFormatException ignored) {
                    }
                }));
        servidor.start();
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (servidor != null) {
            servidor.detener();
        }
        binding = null;
    }

    private static class SimpleHttpServer extends Thread {
        private boolean activo = true;
        private ServerSocket socket;
        private final OnNumeroListener listener;

        interface OnNumeroListener {
            void onNumero(String numero);
        }

        SimpleHttpServer(OnNumeroListener listener) {
            this.listener = listener;
        }

        void detener() {
            activo = false;
            try {
                if (socket != null) socket.close();
            } catch (IOException ignored) {}
        }

        @Override
        public void run() {
            try {
                socket = new ServerSocket(8080);
                while (activo) {
                    Socket cliente = socket.accept();
                    BufferedReader br = new BufferedReader(new InputStreamReader(cliente.getInputStream()));
                    String linea;
                    int longitud = 0;
                    while ((linea = br.readLine()) != null && !linea.isEmpty()) {
                        if (linea.startsWith("Content-Length:")) {
                            longitud = Integer.parseInt(linea.substring(15).trim());
                        }
                    }
                    char[] cuerpo = new char[longitud];
                    if (longitud > 0) {
                        br.read(cuerpo, 0, longitud);
                        String numero = new String(cuerpo).trim();
                        listener.onNumero(numero);
                    }
                    PrintWriter pw = new PrintWriter(cliente.getOutputStream());
                    pw.print("HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\n\r\nOK");
                    pw.flush();
                    cliente.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
