package com.example.ecopesa.ui.medir;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class MedirViewModel extends ViewModel {

    private final MutableLiveData<String> texto;

    public MedirViewModel() {
        texto = new MutableLiveData<>();
        texto.setValue("0 kg");
    }

    public LiveData<String> getTexto() {
        return texto;
    }
}
