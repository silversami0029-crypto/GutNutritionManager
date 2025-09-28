package com.example.gutnutritionmanager;



import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

public class NutritionViewModelFactory implements ViewModelProvider.Factory {
    private final Application application;

    public NutritionViewModelFactory(Application application) {
        this.application = application;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(NutritionViewModel.class)) {
            return (T) new NutritionViewModel(application);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}