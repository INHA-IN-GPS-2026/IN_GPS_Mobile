package com.example.in_gps;

import com.google.gson.annotations.SerializedName;

public class DeviceModel {
    @SerializedName("device_id")
    public String deviceId;
    @SerializedName("equipment_id")
    public String equipmentId;
    @SerializedName("status")
    public String status;
    @SerializedName("installed_on")
    public String installedOn;
    @SerializedName("last_seen_at")
    public String lastSeenAt;
    @SerializedName("created_at")
    public String createdAt;
    @SerializedName("updated_at")
    public String updatedAt;
}