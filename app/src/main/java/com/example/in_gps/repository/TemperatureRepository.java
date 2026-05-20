package com.example.in_gps.repository;

import androidx.lifecycle.MutableLiveData;

import com.example.in_gps.api.RetrofitClient;
import com.example.in_gps.model.TemperatureModel;
import com.example.in_gps.model.TemperatureResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TemperatureRepository {

    public void fetchLatest(String deviceId, MutableLiveData<TemperatureModel> liveData) {
        RetrofitClient.getInstance().getApiService()
                .getTemperature(deviceId, 1)
                .enqueue(new Callback<TemperatureResponse>() {
                    @Override
                    public void onResponse(Call<TemperatureResponse> call, Response<TemperatureResponse> response) {
                        if (response.isSuccessful() && response.body() != null
                                && !response.body().items.isEmpty()) {
                            liveData.postValue(response.body().items.get(0));
                        }
                    }

                    @Override
                    public void onFailure(Call<TemperatureResponse> call, Throwable t) {
                        // polling will retry on next interval
                    }
                });
    }

    /**
     * 실시간 차트용 — 최근 N건 raw 로그. 1초 주기 polling에 사용.
     * /temperature?device_id=X&limit=N (items 형식 응답).
     * 서버는 created_at DESC로 주므로 호출 측에서 시간순으로 뒤집어 써야 함.
     */
    public void fetchRecent(String deviceId, int limit, MutableLiveData<List<TemperatureModel>> liveData) {
        RetrofitClient.getInstance().getApiService()
                .getTemperature(deviceId, limit)
                .enqueue(new Callback<TemperatureResponse>() {
                    @Override
                    public void onResponse(Call<TemperatureResponse> call, Response<TemperatureResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            liveData.postValue(response.body().items);
                        }
                    }

                    @Override
                    public void onFailure(Call<TemperatureResponse> call, Throwable t) {
                        // polling will retry on next interval
                    }
                });
    }

    public void fetchPeriod(String deviceId, int days, MutableLiveData<List<TemperatureModel>> liveData) {
        // 서버에서 days 기준으로 필터+집계 처리:
        //   days <= 7  → raw 데이터 (분 단위)
        //   days >  7  → 일별 평균 집계 (최대 365개)
        RetrofitClient.getInstance().getApiService()
                .getTemperatureChart(deviceId, days)
                .enqueue(new Callback<TemperatureResponse>() {
                    @Override
                    public void onResponse(Call<TemperatureResponse> call, Response<TemperatureResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            liveData.postValue(response.body().items);
                        }
                    }

                    @Override
                    public void onFailure(Call<TemperatureResponse> call, Throwable t) {
                        // silently ignore
                    }
                });
    }
}
