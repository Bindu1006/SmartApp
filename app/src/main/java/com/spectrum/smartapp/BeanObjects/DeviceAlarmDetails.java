package com.spectrum.smartapp.BeanObjects;

/**
 * Created by shrutib on 5/8/17.
 */

public class DeviceAlarmDetails {

    private String deviceName;

    private String deviceIP;

    private String deviceStatus;

    private String alarmTime;

    private String repeatStatus;

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getDeviceIP() {
        return deviceIP;
    }

    public void setDeviceIP(String deviceIP) {
        this.deviceIP = deviceIP;
    }

    public String getDeviceStatus() {
        return deviceStatus;
    }

    public void setDeviceStatus(String deviceStatus) {
        this.deviceStatus = deviceStatus;
    }

    public String getAlarmTime() {
        return alarmTime;
    }

    public void setAlarmTime(String alarmTime) {
        this.alarmTime = alarmTime;
    }

    public String getRepeatStatus() {
        return repeatStatus;
    }

    public void setRepeatStatus(String repeatStatus) {
        this.repeatStatus = repeatStatus;
    }

    @Override
    public String toString() {
        return "DeviceAlarmDetails{" +
                "deviceName='" + deviceName + '\'' +
                ", deviceIP='" + deviceIP + '\'' +
                ", deviceStatus='" + deviceStatus + '\'' +
                ", alarmTime='" + alarmTime + '\'' +
                ", repeatStatus='" + repeatStatus + '\'' +
                '}';
    }
}
