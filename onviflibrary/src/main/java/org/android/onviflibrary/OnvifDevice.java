package org.android.onviflibrary;

import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;

import org.android.onviflibrary.impls.GetCapabilitiesStrategy;
import org.android.onviflibrary.requester.DefaultOnvifRequester;

import java.util.ArrayList;
import java.util.List;


/**
 * Onvif设备;
 * 通过构造器去创建 {@link Builder}
 * 如果登录成功, 则会返回一个OnvifDevice实例;
 * 登录不成功则抛出错误;
 * 密码或账号错误时可能是抛出AuthenticateException
 *
 * @see org.android.onviflibrary.OnvifDeviceTest2
 * @author YGX
 */
public class OnvifDevice {
    private static final String TAG = "OnvifDevice";
    public static final int DEFAULT_PORT = 80;
    public static final String DEFAULT_FORMAT_URL = "http://%s:%d/onvif/device_service";

    private String getServicesUrl;
    private String getCapabilitiesUrl;
    private String getDevicesInformationUrl;
    private String getProfilesUrl;
    private String getStreamUriUrl;

    private String manufacturerName;
    private String modelName;
    private String fwVersion;
    private String serialNumber;
    private String hwID;

    private String username;
    private String password;
    private String host;
    private int port = DEFAULT_PORT;

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    private List<MediaProfile> mediaProfiles = new ArrayList<>();

    public String getGetCapabilitiesUrl() {
        return getCapabilitiesUrl;
    }

    public void setGetCapabilitiesUrl(String getCapabilitiesUrl) {
        this.getCapabilitiesUrl = getCapabilitiesUrl;
    }

    public String getGetServicesUrl() {
        return getServicesUrl;
    }

    public void setGetServicesUrl(String getServicesUrl) {
        this.getServicesUrl = getServicesUrl;
    }

    public String getGetDevicesInformationUrl() {
        return getDevicesInformationUrl;
    }

    public void setGetDevicesInformationUrl(String getDevicesInformationUrl) {
        this.getDevicesInformationUrl = getDevicesInformationUrl;
    }

    public String getGetProfilesUrl() {
        return getProfilesUrl;
    }

    public void setGetProfilesUrl(String getProfilesUrl) {
        this.getProfilesUrl = getProfilesUrl;
    }

    public String getGetStreamUriUrl() {
        return getStreamUriUrl;
    }

    public void setGetStreamUriUrl(String getStreamUriUrl) {
        this.getStreamUriUrl = getStreamUriUrl;
    }

    public void setManufacturerName(String manufacturerName) {
        this.manufacturerName = manufacturerName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public void setFwVersion(String fwVersion) {
        this.fwVersion = fwVersion;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public void setHwID(String hwID) {
        this.hwID = hwID;
    }

    private OnvifDevice() {
    }

    public String getManufacturerName() {
        return manufacturerName;
    }

    public String getModelName() {
        return modelName;
    }

    public String getFwVersion() {
        return fwVersion;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public String getHwID() {
        return hwID;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getHost() {
        return host;
    }

    public List<MediaProfile> getMediaProfiles() {
        return mediaProfiles;
    }


    public static class Builder {

        private final OnvifDevice onvifDevice;
        private IHttpRequester<OnvifRequest, OnvifResponse> requester;
        private IOnvifDeviceLoginStrategy loginHandler;

        public Builder() {
            onvifDevice = new OnvifDevice();
        }

        public Builder username(String username) {
            onvifDevice.username = username;
            return this;
        }

        public Builder password(String password) {
            onvifDevice.password = password;
            return this;
        }

        public Builder host(String host) {
            onvifDevice.host = host;
            return this;
        }

        public Builder httpRequester(@Nullable IHttpRequester<OnvifRequest, OnvifResponse> requester) {
            this.requester = requester;
            return this;
        }

        public Builder loginStrategy(@Nullable IOnvifDeviceLoginStrategy loginHandler) {
            this.loginHandler = loginHandler;
            return this;
        }

        @WorkerThread
        public OnvifDevice login() throws Exception {

            if (isEmptys(onvifDevice.host)) {
                throw new IllegalArgumentException("host can not be null");
            }
            if (isEmptys(onvifDevice.username)) {
                throw new IllegalArgumentException("username can not be null");
            }
            if (isEmptys(onvifDevice.password)) {
                throw new IllegalArgumentException("password can not be null");
            }


            if (requester == null) requester = new DefaultOnvifRequester();
            if (loginHandler == null) loginHandler = new GetCapabilitiesStrategy();

            requester.syncStart();
            try {
                loginHandler.onRequest(onvifDevice, requester);
                return onvifDevice;
            } finally {
                requester.syncStop();
            }
        }

        private boolean isEmptys(String... strArray) {
            for (String s : strArray) {
                if (s == null || s.length() == 0) return true;
            }
            return false;
        }
    }

    @Override
    public String toString() {
        StringBuilder mediaProfilesString = new StringBuilder();
        for (MediaProfile mediaProfile : mediaProfiles) {
            mediaProfilesString.append("[").append(mediaProfile).append("]");
        }
        return "OnvifDevice{" +
                "manufacturerName='" + manufacturerName + '\'' +
                ", modelName='" + modelName + '\'' +
                ", fwVersion='" + fwVersion + '\'' +
                ", serialNumber='" + serialNumber + '\'' +
                ", hwID='" + hwID + '\'' +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", host='" + host + '\'' +
                ", mediaProfiles=" + mediaProfilesString +
                '}';
    }
}
