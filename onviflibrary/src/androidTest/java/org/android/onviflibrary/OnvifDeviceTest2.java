package org.android.onviflibrary;

import android.util.Log;

import org.android.onviflibrary.impls.GetCapabilitiesRequestHandler;
import org.android.onviflibrary.impls.GetServicesRequestHandler;
import org.android.onviflibrary.requester.DefaultOnvifRequester;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@RunWith(Parameterized.class)
public class OnvifDeviceTest2 {

    private static final String TAG = "OnvifDeviceTest";
    private final Account onvifAccount;

    static class Account {
        String host;
        String username;
        String password;

        public Account(String host, String username, String password) {
            this.host = host;
            this.username = username;
            this.password = password;
        }
    }

    @Parameterized.Parameters
    public static Collection<Account> source() {
        List<Account> list = new ArrayList<>();

        // TP-LINK, 严格要求密码
        list.add(new Account("10.0.0.40", "admin", "1234567890"));

        // 火力牛, 不限密码
        list.add(new Account("192.168.1.60", "admin", "1234567890"));

        // 大华, 不限密码
        list.add(new Account("10.0.0.39", "admin", "1234567890"));

        // 华安, 不限密码
        list.add(new Account("10.0.0.201", "admin", "1234567890"));

        // 海康, 限制密码登录
        list.add(new Account("10.0.0.57", "admin", "test12345678Q"));

        // Network Digital Video, 不限密码
        list.add(new Account("192.168.1.88", "admin", "test12345678Q"));

        return list;
    }

    public OnvifDeviceTest2(Account onvifAccount) {
        this.onvifAccount = onvifAccount;
    }

       @Test
       public void getServicesLogin() throws Exception {
           OnvifDevice device = new OnvifDevice.Builder()
                   .host(onvifAccount.host)
                   .username(onvifAccount.username)
                   .password(onvifAccount.password)
                   .httpRequester(new DefaultOnvifRequester())
                   .requestHandler(new GetServicesRequestHandler())
                   .login();
           Log.e(TAG, "getServicesLogin: " + device);
       }
    @Test
    public void getCapabilitiesLogin() throws Exception {
        OnvifDevice device = new OnvifDevice.Builder()
                .host(onvifAccount.host)
                .username(onvifAccount.username)
                .password(onvifAccount.password)
                .httpRequester(new DefaultOnvifRequester())
                .requestHandler(new GetCapabilitiesRequestHandler())
                .login();

        String fwVersion = device.getFwVersion();
        String hwID = device.getHwID();
        String manufacturerName = device.getManufacturerName();
        String modelName = device.getModelName();
        String serialNumber = device.getSerialNumber();

        List<MediaProfile> mediaProfiles = device.getMediaProfiles();
        for (MediaProfile mediaProfile : mediaProfiles) {
            String mediaStreamUri = mediaProfile.getMediaStreamUri();
            String onvifMediaStreamUri = mediaProfile.getOnvifMediaStreamUri();
            MediaProfile.VideoEncoderConfiguration videoEncoderConfiguration = mediaProfile.getVideoEncoderConfiguration();
            String enoding = videoEncoderConfiguration.getEnoding();
            MediaProfile.VideoResolution resolution = videoEncoderConfiguration.getResolution();
            int width = resolution.getWidth();
            int height = resolution.getHeight();
            MediaProfile.VideoRateControl videoRateControl = videoEncoderConfiguration.getVideoRateControl();
            int bitrateLimit = videoRateControl.getBitrateLimit();
            int encodingInterval = videoRateControl.getEncodingInterval();
            int frameRateLimit = videoRateControl.getFrameRateLimit();
        }
        Log.e(TAG, "getCapabilitiesLogin: " + device);
    }
}