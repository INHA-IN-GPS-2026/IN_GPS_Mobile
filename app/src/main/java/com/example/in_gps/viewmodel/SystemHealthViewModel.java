package com.example.in_gps.viewmodel;

import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.in_gps.repository.DeviceRepository;

public class SystemHealthViewModel extends ViewModel {

    private static final long POLL_INTERVAL_MS = 30_000;

    public static class DeviceStats {
        public final int normal;
        public final int warning;
        public final int critical;
        public final int total;

        public DeviceStats(int normal, int warning, int critical) {
            this.normal = normal;
            this.warning = warning;
            this.critical = critical;
            this.total = normal + warning + critical;
        }
    }

    private final MutableLiveData<DeviceStats> deviceStats = new MutableLiveData<>();
    private final DeviceRepository repository = new DeviceRepository();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            repository.fetchDevices(devices -> {
                int normal = 0, warning = 0, critical = 0;
                for (com.example.in_gps.model.DeviceModel d : devices) {
                    if ("normal".equalsIgnoreCase(d.status)) normal++;
                    else if ("warning".equalsIgnoreCase(d.status)) warning++;
                    else if ("critical".equalsIgnoreCase(d.status)) critical++;
                }
                deviceStats.postValue(new DeviceStats(normal, warning, critical));
            });
            handler.postDelayed(this, POLL_INTERVAL_MS);
        }
    };

    public SystemHealthViewModel() {
        handler.post(pollRunnable);
    }

    public LiveData<DeviceStats> getDeviceStats() {
        return deviceStats;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        handler.removeCallbacksAndMessages(null);
    }
}
