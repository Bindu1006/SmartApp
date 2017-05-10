package com.spectrum.smartapp.BeanObjects;

import android.os.Parcel;
import android.os.Parcelable;

import com.spectrum.smartapp.ConfigureDeviceActivity;
import com.spectrum.smartapp.NetworkDiscovery.NetInfo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by shrutib on 4/29/17.
 */

public class DeviceBean implements Serializable {

    public static final String EXTRA = ConfigureDeviceActivity.PKG + ".extra";
//    public static final String EXTRA_POSITION = ActivityMain.PKG + ".extra_position";
//    public static final String EXTRA_HOST = ActivityMain.PKG + ".extra_host";
//    public static final String EXTRA_TIMEOUT = ActivityMain.PKG + ".network.extra_timeout";
//    public static final String EXTRA_HOSTNAME = ActivityMain.PKG + ".extra_hostname";
//    public static final String EXTRA_BANNERS = ActivityMain.PKG + ".extra_banners";
//    public static final String EXTRA_PORTSO = ActivityMain.PKG + ".extra_ports_o";
//    public static final String EXTRA_PORTSC = ActivityMain.PKG + ".extra_ports_c";
//    public static final String EXTRA_SERVICES = ActivityMain.PKG + ".extra_services";
    public static final int TYPE_GATEWAY = 0;
    public static final int TYPE_COMPUTER = 1;

    public int deviceType = TYPE_COMPUTER;
    public int isAlive = 1;
    public int position = 0;
    public int responseTime = 0; // ms
    public String ipAddress = null;
    public String hostname = null;
    public String hardwareAddress = NetInfo.NOMAC;
    public String nicVendor = "Unknown";
    public String os = "Unknown";
    public HashMap<Integer, String> services = null;
    public HashMap<Integer, String> banners = null;
    public ArrayList<Integer> portsOpen = null;
    public ArrayList<Integer> portsClosed = null;


    private String deviceIpAddress;

    private String vendor;

    private String deviceName;

    private String deviceStatus;

    public String getDeviceIpAddress() {
        return deviceIpAddress;
    }

    public void setDeviceIpAddress(String deviceIpAddress) {
        this.deviceIpAddress = deviceIpAddress;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getDeviceStatus() {
        return deviceStatus;
    }

    public void setDeviceStatus(String deviceStatus) {
        this.deviceStatus = deviceStatus;
    }

    public DeviceBean() {
        // New object
    }

    public DeviceBean(Parcel in) {
        // Object from parcel
        readFromParcel(in);
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(deviceType);
        dest.writeInt(isAlive);
        dest.writeString(ipAddress);
        dest.writeString(hostname);
        dest.writeString(hardwareAddress);
        dest.writeString(nicVendor);
        dest.writeString(os);
        dest.writeInt(responseTime);
        dest.writeInt(position);
        dest.writeMap(services);
        dest.writeMap(banners);
        dest.writeList(portsOpen);
        dest.writeList(portsClosed);
    }

    @SuppressWarnings("unchecked")
    private void readFromParcel(Parcel in) {
        deviceType = in.readInt();
        isAlive = in.readInt();
        ipAddress = in.readString();
        hostname = in.readString();
        hardwareAddress = in.readString();
        nicVendor = in.readString();
        os = in.readString();
        responseTime = in.readInt();
        position = in.readInt();
        services = in.readHashMap(null);
        banners = in.readHashMap(null);
        portsOpen = in.readArrayList(Integer.class.getClassLoader());
        portsClosed = in.readArrayList(Integer.class.getClassLoader());
    }

    @SuppressWarnings("unchecked")
    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public DeviceBean createFromParcel(Parcel in) {
            return new DeviceBean(in);
        }

        public DeviceBean[] newArray(int size) {
            return new DeviceBean[size];
        }
    };

    @Override
    public String toString() {
        return "DeviceBean{" +
                "deviceStatus='" + deviceStatus + '\'' +
                ", deviceIpAddress='" + deviceIpAddress + '\'' +
                ", vendor='" + vendor + '\'' +
                ", deviceName='" + deviceName + '\'' +
                '}';
    }
}
