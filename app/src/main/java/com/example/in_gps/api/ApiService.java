package com.example.in_gps.api;

import com.example.in_gps.model.AvailableDatesResponse;
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

    /** 기간(시작~종료) 조회. 날짜는 YYYY-MM-DD. */
    @GET("temperature/chart")
    Call<TemperatureResponse> getTemperatureChartRange(
            @Query("device_id") String deviceId,
            @Query("start") String start,
            @Query("end") String end
    );

    /** 데이터가 존재하는 날짜 목록(캘린더 표시용). */
    @GET("temperature/dates")
    Call<AvailableDatesResponse> getAvailableDates(
            @Query("device_id") String deviceId
    );

    @GET("devices/{device_id}/logs")
    Call<DeviceLogResponse> getDeviceLogs(
            @Path("device_id") String deviceId,
            @Query("limit") int limit
    );
}
