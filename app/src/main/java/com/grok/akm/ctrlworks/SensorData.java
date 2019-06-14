package com.grok.akm.ctrlworks;

public class SensorData {

    private float yaw;
    private float pitch;
    private float azimuth;

    public SensorData(){

    }

    public float getYaw() {
        return yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public float getAzimuth() {
        return azimuth;
    }

    public void setAzimuth(float azimuth) {
        this.azimuth = azimuth;
    }

    @Override
    public String toString() {
        return "SensorData{" +
                "yaw=" + yaw +
                ", pitch=" + pitch +
                ", azimuth=" + azimuth +
                '}';
    }
}
