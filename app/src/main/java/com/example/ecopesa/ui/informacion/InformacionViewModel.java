package com.example.ecopesa.ui.informacion;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class InformacionViewModel extends ViewModel {

    private final MutableLiveData<String> texto;

    public InformacionViewModel() {
        texto = new MutableLiveData<>();
        texto.setValue("");
    }

    public LiveData<String> getTexto() {
        return texto;
    }

    public void cargarDatos(java.util.Map<String, String> datos) {
        StringBuilder sb = new StringBuilder();
        for (java.util.Map.Entry<String, String> e : datos.entrySet()) {
            sb.append(e.getKey()).append(": ").append(e.getValue()).append("\n");
        }
        texto.setValue(sb.toString());
    }
}
