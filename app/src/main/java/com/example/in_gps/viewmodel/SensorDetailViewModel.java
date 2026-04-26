package com.example.in_gps.viewmodel;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.in_gps.model.TemperatureModel;
import com.example.in_gps.repository.TemperatureRepository;

import java.util.List;

public class SensorDetailViewModel extends ViewModel {

    private static final long POLL_INTERVAL_MS = 5000;

    private final String deviceId;
    private final MutableLiveData<TemperatureModel> temperatureData = new MutableLiveData<>();
    private final MutableLiveData<List<TemperatureModel>> periodData = new MutableLiveData<>();
    private final TemperatureRepository repository = new TemperatureRepository();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            repository.fetchLatest(deviceId, temperatureData);
            handler.postDelayed(this, POLL_INTERVAL_MS);
        }
    };

    public SensorDetailViewModel(String deviceId) {
        this.deviceId = deviceId;
        handler.post(pollRunnable);
    }

    public LiveData<TemperatureModel> getTemperatureData() {
        return temperatureData;
    }

    public LiveData<List<TemperatureModel>> getPeriodData() {
        return periodData;
    }

    public void loadPeriod(int days) {
        repository.fetchPeriod(deviceId, days, periodData);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        handler.removeCallbacksAndMessages(null);
    }

    public static class Factory implements ViewModelProvider.Factory {
        private final String deviceId;

        public Factory(String deviceId) {
            this.deviceId = deviceId;
        }

        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return (T) new SensorDetailViewModel(deviceId);
        }
    }
}
