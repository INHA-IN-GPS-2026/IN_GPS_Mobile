package com.example.in_gps.model;

import com.google.gson.annotations.SerializedName;

public class TemperatureModel {
    @SerializedName("id")
    public long id;

    @SerializedName("device_id")
    public String deviceId;

    // Thermistor (NTC) °C
    @SerializedName("temp1")
    public float temp1;

    @SerializedName("temp2")
    public float temp2;

    // ADXL335 RMS [mg] — server v4 (angle_* → rms_* rename)
    @SerializedName("rms_x")
    public Integer rmsX;

    @SerializedName("rms_y")
    public Integer rmsY;

    @SerializedName("rms_z")
    public Integer rmsZ;

    // 레거시 — 구 server (v3 이전) 응답 호환. 신규 코드는 사용 X.
    @SerializedName("angle_x")
    public Float angleX;

    @SerializedName("angle_y")
    public Float angleY;

    @SerializedName("angle_z")
    public Float angleZ;

    @SerializedName("event")
    public String event;

    @SerializedName("created_at")
    public String createdAt;

    // 집계 응답 전용 (aggregated=true 일 때만 값 존재)
    @SerializedName("temp1_max")
    public Float temp1Max;

    @SerializedName("temp2_max")
    public Float temp2Max;

    @SerializedName("temp1_min")
    public Float temp1Min;

    @SerializedName("temp2_min")
    public Float temp2Min;
}
