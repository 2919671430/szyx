package com.szyx.ai.viewmodel;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.szyx.ai.util.FileUtils;

import java.io.File;

public class DownloadViewModel extends AndroidViewModel {

    private final MutableLiveData<Boolean> modelExists = new MutableLiveData<>(false);

    public DownloadViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<Boolean> getModelExists() { return modelExists; }

    public boolean checkModelsExist() {
        boolean exists = FileUtils.isAllModelsReady(getApplication());
        modelExists.postValue(exists);
        return exists;
    }

    public String getSavedModelPath() {
        SharedPreferences prefs = getApplication().getSharedPreferences("settings", Context.MODE_PRIVATE);
        return prefs.getString("llm_model_path", "");
    }

    public void saveModelPath(String path) {
        SharedPreferences prefs = getApplication().getSharedPreferences("settings", Context.MODE_PRIVATE);
        prefs.edit().putString("llm_model_path", path).apply();
    }
}
