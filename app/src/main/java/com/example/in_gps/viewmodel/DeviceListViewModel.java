package com.example.in_gps.viewmodel;

import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.in_gps.model.DeviceModel;
import com.example.in_gps.repository.DeviceRepository;

import java.util.List;

public class DeviceListViewModel extends ViewModel {

    private static final long POLL_INTERVAL_MS = 30_000;

    private final MutableLiveData<List<DeviceModel>> devices = new MutableLiveData<>();
    private final DeviceRepository repository = new DeviceRepository();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            repository.fetchDevices(result -> devices.postValue(result));
            handler.postDelayed(this, POLL_INTERVAL_MS);
        }
    };

    public DeviceListViewModel() {
        handler.post(pollRunnable);
    }

    public LiveData<List<DeviceModel>> getDevices() {
        return devices;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        handler.removeCallbacksAndMessages(null);
    }
}
