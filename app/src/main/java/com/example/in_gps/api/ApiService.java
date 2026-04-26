package com.example.in_gps.api;

import com.example.in_gps.model.DeviceLogResponse;
import com.example.in_gps.model.DeviceModelResponse;
import com.example.in_gps.model.TemperatureResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    @GET("devices")
    Call<DeviceModelResponse>getDevices();

    @GET("temperature")
    Call<TemperatureResponse> getTemperature(
            @Query("device_id") String deviceId,
            @Query("limit") int limit
    );

    @GET("temperature/chart")
    Call<TemperatureResponse> getTemperatureChart(
            @Query("device_id") String deviceId,
            @Query("days") int days
    );

    @GET("devices/{device_id}/logs")
    Call<DeviceLogResponse> getDeviceLogs(
            @Path("device_id") String deviceId,
            @Query("limit") int limit
    );
}
