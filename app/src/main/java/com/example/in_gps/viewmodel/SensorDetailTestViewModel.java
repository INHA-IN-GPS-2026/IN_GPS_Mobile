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

/**
 * SensorDetailTestFragment 용 ViewModel.
 *
 *  - ESP는 1초마다 mfg_data 광고 → STM32 게이트웨이가 MQTT publish → 서버 INSERT
 *  - 모바일은 1초 주기로 /temperature?device_id=X&limit={WINDOW_SIZE} 호출
 *  - 두 차트(온도, RMS)는 받은 같은 List를 공유해 sliding window로 갱신
 */
public class SensorDetailTestViewModel extends ViewModel {

    /** 차트에 표시할 sliding window 크기 (= 직전 N초). */
    public static final int WINDOW_SIZE = 60;

    /** polling 주기. 너무 작으면 서버 부하, 너무 크면 실시간 느낌 떨어짐. */
    private static final long POLL_INTERVAL_MS = 1000L;

    private final String deviceId;
    private final TemperatureRepository repository = new TemperatureRepository();
    private final Handler handler = new Handler(Looper.getMainLooper());

    /** 최근 WINDOW_SIZE 건의 raw 로그. 차트가 관찰. */
    private final MutableLiveData<List<TemperatureModel>> recentData = new MutableLiveData<>();

    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            repository.fetchRecent(deviceId, WINDOW_SIZE, recentData);
            handler.postDelayed(this, POLL_INTERVAL_MS);
        }
    };

    public SensorDetailTestViewModel(String deviceId) {
        this.deviceId = deviceId;
        handler.post(pollRunnable);
    }

    public LiveData<List<TemperatureModel>> getRecentData() {
        return recentData;
    }

    public String getDeviceId() {
        return deviceId;
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
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return (T) new SensorDetailTestViewModel(deviceId);
        }
    }
}
