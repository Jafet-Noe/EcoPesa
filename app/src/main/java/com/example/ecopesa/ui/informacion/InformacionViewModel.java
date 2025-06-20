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

    public void cargarDatos(java.util.Map<String, Object> datos) {
        StringBuilder sb = new StringBuilder();
        for (java.util.Map.Entry<String, Object> e : datos.entrySet()) {
            Object val = e.getValue();
            if (val instanceof java.util.List) {
                sb.append(e.getKey()).append(":\n");
                for (Object item : (java.util.List<?>) val) {
                    sb.append(" \u2022 ").append(item).append("\n");
                }
            } else {
                sb.append(e.getKey()).append(": ").append(val).append("\n");
            }
            sb.append("\n");
        }
        texto.setValue(sb.toString().trim());
    }
}
