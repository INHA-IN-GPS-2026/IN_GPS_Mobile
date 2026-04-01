package com.example.in_gps.repository;

import androidx.lifecycle.MutableLiveData;

import com.example.in_gps.api.RetrofitClient;
import com.example.in_gps.model.TemperatureModel;
import com.example.in_gps.model.TemperatureResponse;

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
}
