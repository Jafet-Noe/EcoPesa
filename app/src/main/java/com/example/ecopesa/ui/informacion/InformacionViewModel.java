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
            sb.append(e.getKey()).append(": ");
            Object val = e.getValue();
            if (val instanceof java.util.List) {
                java.util.List<?> lista = (java.util.List<?>) val;
                for (Object item : lista) {
                    sb.append("\n  - ").append(item);
                }
                if (!lista.isEmpty()) {
                    sb.append("\nÚltimo alumno: ").append(lista.get(lista.size() - 1));
                }
                sb.append("\n");
            } else {
                sb.append(val).append("\n");
            }
        }
        texto.setValue(sb.toString());
    }
}
