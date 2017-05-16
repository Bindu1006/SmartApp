package com.spectrum.smartapp.BeanObjects;

/**
 * Created by shrutib on 5/16/17.
 */

public class EnergyBean {

    private String device_ip;

    private String device_name;

    private String usage;

    private String Status;

    public String getDevice_ip() {
        return device_ip;
    }

    public void setDevice_ip(String device_ip) {
        this.device_ip = device_ip;
    }

    public String getDevice_name() {
        return device_name;
    }

    public void setDevice_name(String device_name) {
        this.device_name = device_name;
    }

    public String getUsage() {
        return usage;
    }

    public void setUsage(String usage) {
        this.usage = usage;
    }

    public String getStatus() {
        return Status;
    }

    public void setStatus(String status) {
        Status = status;
    }

    @Override
    public String toString() {
        return "EnergyBean{" +
                "device_ip='" + device_ip + '\'' +
                ", device_name='" + device_name + '\'' +
                ", usage='" + usage + '\'' +
                ", Status='" + Status + '\'' +
                '}';
    }
}
