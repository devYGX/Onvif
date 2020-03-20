package org.android.onviflibrary.impls;

import android.text.TextUtils;
import android.util.Log;

import org.android.onviflibrary.IHttpRequester;
import org.android.onviflibrary.IOnvifDeviceLoginStrategy;
import org.android.onviflibrary.MediaProfile;
import org.android.onviflibrary.OnvifDevice;
import org.android.onviflibrary.OnvifRequest;
import org.android.onviflibrary.OnvifResponse;
import org.android.onviflibrary.finder.OnvifDiscoverer;
import org.android.onviflibrary.finder.OnvifFinder;
import org.android.onviflibrary.req.GetDeviceInformationOnvifRequest;
import org.android.onviflibrary.req.GetProfilesOnvifRequest;
import org.android.onviflibrary.req.GetServiceOnvifRequest;
import org.android.onviflibrary.req.GetStreamUriOnvifRequest;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.List;
import java.util.Locale;

public class GetServicesStrategy implements IOnvifDeviceLoginStrategy {
    private static final String TAG = "DefaultOnvifDeviceReque";

    @Override
    public void onRequest(OnvifDevice device,
                          IHttpRequester<OnvifRequest, OnvifResponse> httpRequester) throws Exception {

        // GetCapabilities
        getServices(device, httpRequester);

        // GetDeviceInformation
        getDeviceInformation(device, httpRequester);

        // GetProfile
        getProfile(device, httpRequester);

        // GetStreamUri
        for (MediaProfile profile : device.getMediaProfiles()) {
            getMediaProfileStreamUrl(device, profile, httpRequester);
            profile.setStreamType(
                    getMediaProfileStreamType(profile.getName(),
                            profile.getToken(),
                            profile.getMediaStreamUri()));
            profile.setOnvifMediaStreamUri(
                    profile.getMediaStreamUri().replace("rtsp://",
                            "rtsp://" + device.getUsername() + ":" + device.getPassword() + "@"));
        }

    }

    private void getServices(OnvifDevice device, IHttpRequester<OnvifRequest, OnvifResponse> httpRequester) throws Exception {
        // getUrl; headers; contentType;
        String getGetServicesUrl = TextUtils.isEmpty(device.getGetServicesUrl()) ?
                String.format(Locale.CHINA, OnvifDevice.DEFAULT_FORMAT_URL, device.getHost(), device.getPort()) : device.getGetServicesUrl();
        GetServiceOnvifRequest request =
                new GetServiceOnvifRequest(getGetServicesUrl);
        OnvifResponse response = httpRequester.accept(request);


        int code = response.getCode();
        if (code != 200) {
            OnvifFinder onvifFinder = new OnvifFinder(device.getHost(), 1);
            List<OnvifDiscoverer> onvifDiscovererList = onvifFinder.find();
            if (onvifDiscovererList.size() == 0) {
                throw new IllegalStateException("No Host " + device.getHost() + " Onvif Device");
            }
            OnvifDiscoverer onvifDiscoverer = onvifDiscovererList.get(0);
            URL url = new URL(onvifDiscoverer.getAddress());
            int port = url.getPort();
            device.setPort(port);
            request =
                    new GetServiceOnvifRequest(onvifDiscoverer.getAddress());
            response = httpRequester.accept(request);
        }

        if (response.getCode() != 200) {
            throw new IllegalStateException("Get Onvif Services Fail With Code " + response.getCode() + ": " + response.getResponseMessage());
        }
        parseGetServicesResponse(response, device);
    }

    private void parseGetServicesResponse(OnvifResponse response, OnvifDevice device) throws XmlPullParserException, IOException {
        XmlPullParserFactory parserFactory = XmlPullParserFactory.newInstance();
        parserFactory.setNamespaceAware(true);
        XmlPullParser xmlPullParser = parserFactory.newPullParser();
        xmlPullParser.setInput(new StringReader(response.getMessage()));
        int eventType = xmlPullParser.getEventType();

        /*
        <tds:Service>
        <tds:Namespace>http://www.onvif.org/ver10/media/wsdl</tds:Namespace>
        <tds:XAddr>http://192.168.1.86:2020/onvif/service</tds:XAddr>
        <tds:Version>
          <tt:Major>2</tt:Major>
          <tt:Minor>20</tt:Minor>
        </tds:Version>
      </tds:Service>
         */

        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && xmlPullParser.getName().equals("Service")) {
                eventType = xmlPullParser.next();
                String namespace = null;
                String xAddr = null;
                while (!(eventType == XmlPullParser.END_TAG && xmlPullParser.getName().equals("Service"))) {
                    if (eventType == XmlPullParser.START_TAG) {
                        String name = xmlPullParser.getName();
                        if (name.equals("Namespace")) {
                            xmlPullParser.next();
                            namespace = xmlPullParser.getText();
                        } else if (name.equals("XAddr")) {
                            xmlPullParser.next();
                            xAddr = xmlPullParser.getText();
                        }
                        if (!TextUtils.isEmpty(namespace) && !TextUtils.isEmpty(xAddr)) {
                            namespace = namespace.trim();
                            xAddr = xAddr.trim();
                            break;
                        }
                    }
                    eventType = xmlPullParser.next();
                }
                if (GetProfilesOnvifRequest.NAMESPACE.equals(namespace)) {
                    device.setGetStreamUriUrl(xAddr);
                    device.setGetProfilesUrl(xAddr);
                    return;
                }
            }
            eventType = xmlPullParser.next();

        }
    }

    /**
     * if the key array containts keyword 'sub', then return {@link MediaProfile#SUBSTREAM}
     * if the key arraycontaints keyword 'main', then return {@link MediaProfile#MAINSTREAM}
     * otherwise return {@link MediaProfile#UNKNOWN}
     *
     * @param key media profile name, token, and mediaStreamUri
     * @return MediaProfile StreamType,
     */
    private int getMediaProfileStreamType(String... key) {
        for (String s : key) {
            if (!TextUtils.isEmpty(s)) {
                String lowerCaseStr = s.toLowerCase();
                if (lowerCaseStr.contains("sub")) return MediaProfile.SUBSTREAM;
                else if (lowerCaseStr.contains("main")) return MediaProfile.MAINSTREAM;
            }
        }
        return MediaProfile.UNKNOWN;
    }

    private void getMediaProfileStreamUrl(OnvifDevice device, MediaProfile profile, IHttpRequester<OnvifRequest, OnvifResponse> httpRequester) throws Exception {
        String getProfileUrl = TextUtils.isEmpty(device.getGetStreamUriUrl()) ?
                String.format(Locale.CHINA, OnvifDevice.DEFAULT_FORMAT_URL, device.getHost(), device.getPort()) : device.getGetStreamUriUrl();
        GetStreamUriOnvifRequest req = new GetStreamUriOnvifRequest(
                getProfileUrl, device.getUsername(), device.getPassword(), false, profile.getToken());
        OnvifResponse onvifResponse = doRequest(req, httpRequester);
        parseGetStreamUri(profile, onvifResponse);
    }

    private void parseGetStreamUri(MediaProfile mediaProfile, OnvifResponse getStreamURIResponse) throws XmlPullParserException, IOException {
        XmlPullParserFactory parserFactory = XmlPullParserFactory.newInstance();
        parserFactory.setNamespaceAware(true);
        XmlPullParser xmlPullParser = parserFactory.newPullParser();
        xmlPullParser.setInput(new StringReader(getStreamURIResponse.getMessage()));
        int eventType = xmlPullParser.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && "Uri".equals(xmlPullParser.getName())) {
                xmlPullParser.next();
                mediaProfile.setMediaStreamUri(xmlPullParser.getText());
                break;
            }
            eventType = xmlPullParser.next();
        }
    }

    private void getProfile(OnvifDevice device, IHttpRequester<OnvifRequest, OnvifResponse> httpRequester) throws Exception {
        String getProfileUrl = TextUtils.isEmpty(device.getGetProfilesUrl()) ?
                String.format(Locale.CHINA, OnvifDevice.DEFAULT_FORMAT_URL, device.getHost(), device.getPort()) : device.getGetProfilesUrl();
        GetProfilesOnvifRequest request = new GetProfilesOnvifRequest(getProfileUrl, device.getUsername(), device.getPassword(), false);
        parseGetProfile(device, doRequest(request, httpRequester));
    }

    private void parseGetProfile(OnvifDevice onvifDevice, OnvifResponse getProfilesResponse) throws Exception {
        List<MediaProfile> mediaProfiles = onvifDevice.getMediaProfiles();
        mediaProfiles.clear();
        XmlPullParserFactory parserFactory = XmlPullParserFactory.newInstance();
        parserFactory.setNamespaceAware(true);
        XmlPullParser xmlPullParser = parserFactory.newPullParser();
        xmlPullParser.setInput(new StringReader(getProfilesResponse.getMessage()));
        int eventType = xmlPullParser.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                String tagName = xmlPullParser.getName();
                if ("Profiles".equalsIgnoreCase(tagName)) {
                    String token = xmlPullParser.getAttributeValue(null, "token");
                    MediaProfile mediaProfile = new MediaProfile();
                    eventType = xmlPullParser.nextTag();
                    if ("Name".equalsIgnoreCase(xmlPullParser.getName())) {
                        xmlPullParser.next();
                        String name = xmlPullParser.getText();
                        mediaProfile.setName(name);
                    }
                    mediaProfiles.add(mediaProfile);
                    mediaProfile.setToken(token);
                    // 读取Profiles节点内容
                    while (!(eventType == XmlPullParser.END_TAG && "Profiles".equalsIgnoreCase(xmlPullParser.getName()))) {
                        if (eventType == XmlPullParser.START_TAG) {
                            if ("VideoEncoderConfiguration".equals(xmlPullParser.getName())) {
                                MediaProfile.VideoEncoderConfiguration videoEncoder = new MediaProfile.VideoEncoderConfiguration();
                                while (!(eventType == XmlPullParser.END_TAG && "VideoEncoderConfiguration".equals(xmlPullParser.getName())) && eventType != XmlPullParser.END_DOCUMENT) {
                                    if (eventType == XmlPullParser.START_TAG) {
                                        String startTagName = xmlPullParser.getName();
                                        switch (startTagName) {
                                            case "VideoEncoderConfiguration":
                                                String videoEncoderToken = xmlPullParser.getAttributeValue(null, "token");
                                                videoEncoder.setToken(videoEncoderToken);
                                                xmlPullParser.nextTag();
                                                if ("Name".equalsIgnoreCase(xmlPullParser.getName())) {
                                                    xmlPullParser.next();
                                                    String videoEncoderName = xmlPullParser.getText();
                                                    videoEncoder.setName(videoEncoderName);
                                                }
                                                break;
                                            case "Encoding":
                                                xmlPullParser.next();
                                                String videoEncoderEncoding = xmlPullParser.getText();
                                                videoEncoder.setEnoding(videoEncoderEncoding);
                                                break;
                                            case "Resolution":
                                                eventType = xmlPullParser.nextTag();
                                                while (eventType != XmlPullParser.END_DOCUMENT
                                                        && !(eventType == XmlPullParser.END_TAG && "Resolution".equalsIgnoreCase(xmlPullParser.getName()))) {

                                                    if (eventType == XmlPullParser.START_TAG) {
                                                        String resolutionName = xmlPullParser.getName();
                                                        if (TextUtils.isEmpty(resolutionName))
                                                            continue;
                                                        switch (resolutionName.toLowerCase()) {
                                                            case "width":
                                                                xmlPullParser.next();
                                                                String strWidth = xmlPullParser.getText();
                                                                MediaProfile.VideoResolution resolution = videoEncoder.getResolution();
                                                                if (resolution == null) {
                                                                    resolution = new MediaProfile.VideoResolution();
                                                                    videoEncoder.setResolution(resolution);
                                                                }
                                                                try {
                                                                    resolution.setWidth(Integer.parseInt(strWidth.trim()));
                                                                } catch (Exception e) {
                                                                }
                                                                break;
                                                            case "height":
                                                                xmlPullParser.next();
                                                                String strHeight = xmlPullParser.getText();
                                                                MediaProfile.VideoResolution resolutionH = videoEncoder.getResolution();
                                                                if (resolutionH == null) {
                                                                    resolutionH = new MediaProfile.VideoResolution();
                                                                    videoEncoder.setResolution(resolutionH);
                                                                }
                                                                try {
                                                                    resolutionH.setHeight(Integer.parseInt(strHeight.trim()));
                                                                } catch (Exception e) {
                                                                }
                                                                break;
                                                        }
                                                    }
                                                    eventType = xmlPullParser.next();
                                                }
                                                break;
                                            case "RateControl":
                                                while (eventType != XmlPullParser.END_DOCUMENT
                                                        && !(eventType == XmlPullParser.END_TAG && "RateControl".equalsIgnoreCase(xmlPullParser.getName()))) {

                                                    if (eventType == XmlPullParser.START_TAG) {
                                                        String resolutionName = xmlPullParser.getName();
                                                        if (!TextUtils.isEmpty(resolutionName)) {
                                                            switch (resolutionName.toLowerCase()) {
                                                                case "frameratelimit":
                                                                    xmlPullParser.next();
                                                                    String strFrameRateLimit = xmlPullParser.getText();
                                                                    MediaProfile.VideoRateControl videoRateControl = videoEncoder.getVideoRateControl();
                                                                    if (videoRateControl == null) {
                                                                        videoRateControl = new MediaProfile.VideoRateControl();
                                                                        videoEncoder.setVideoRateControl(videoRateControl);
                                                                    }
                                                                    try {
                                                                        videoRateControl.setFrameRateLimit(Integer.parseInt(strFrameRateLimit.trim()));
                                                                    } catch (Exception e) {
                                                                    }
                                                                    break;
                                                                // case "EncodingInterval":
                                                                case "encodinginterval":
                                                                    xmlPullParser.next();
                                                                    String strEncodingInterval = xmlPullParser.getText();
                                                                    MediaProfile.VideoRateControl videoRateControlInterval = videoEncoder.getVideoRateControl();
                                                                    if (videoRateControlInterval == null) {
                                                                        videoRateControlInterval = new MediaProfile.VideoRateControl();
                                                                        videoEncoder.setVideoRateControl(videoRateControlInterval);
                                                                    }
                                                                    try {
                                                                        videoRateControlInterval.setEncodingInterval(Integer.parseInt(strEncodingInterval.trim()));
                                                                    } catch (Exception e) {
                                                                    }
                                                                    break;
                                                                case "bitratelimit":
                                                                    xmlPullParser.next();
                                                                    String strBitrateLimit = xmlPullParser.getText();
                                                                    MediaProfile.VideoRateControl videoRateControlBitrate = videoEncoder.getVideoRateControl();
                                                                    if (videoRateControlBitrate == null) {
                                                                        videoRateControlBitrate = new MediaProfile.VideoRateControl();
                                                                        videoEncoder.setVideoRateControl(videoRateControlBitrate);
                                                                    }
                                                                    try {
                                                                        videoRateControlBitrate.setBitrateLimit(Integer.parseInt(strBitrateLimit.trim()));
                                                                    } catch (Exception e) {
                                                                    }
                                                                    break;
                                                            }
                                                        }
                                                    }
                                                    eventType = xmlPullParser.next();
                                                }
                                                break;
                                        }
                                    }
                                    eventType = xmlPullParser.next();
                                }
                                mediaProfile.setVideoEncoderConfiguration(videoEncoder);
                            }
                        }
                        eventType = xmlPullParser.next();
                    }
                }
            }
            eventType = xmlPullParser.next();
        }
    }

    private OnvifResponse doRequest(OnvifRequest request, IHttpRequester<OnvifRequest, OnvifResponse> httpRequester) throws Exception {
        Log.d(TAG, "doRequest: " + request.getUrl() + ", " + request.getContent());
        OnvifResponse onvifResponse = httpRequester.accept(request);
        int code = onvifResponse.getCode();
        /*Log.d(TAG, "doRequest: 1 " + request.getUrl()
                + "\n" + request.getContent()
                + "\n" + onvifResponse.getCode()
                + "\n" + onvifResponse.getResponseMessage()
                + "\n" + onvifResponse.getMessage());*/
        // 需要Digest验证
        if (code == 401) {
            String authenticate = onvifResponse.header("WWW-Authenticate");
            request.appendHeaders("Authorization", request.getDigestHeader(authenticate));
            onvifResponse = httpRequester.accept(request);
        } else if (code != 200) {
            request.setWs_UsernameTokenContent(true);
            onvifResponse = httpRequester.accept(request);
        }
       /* Log.d(TAG, "doRequest: 2 " + request.getUrl()
                + "\n" + request.getContent()
                + "\n" + onvifResponse.getCode()
                + "\n" + onvifResponse.getResponseMessage()
                + "\n" + onvifResponse.getMessage());*/
        if (onvifResponse.getCode() != 200) {
            throw new IllegalStateException("Get " + request.getClass().getSimpleName() + " Fail With Code " + onvifResponse.getCode() + ": " + onvifResponse.getResponseMessage());
        }

        return onvifResponse;
    }

    private void getDeviceInformation(OnvifDevice device, IHttpRequester<OnvifRequest, OnvifResponse> httpRequester) throws Exception {
        String getDeviceInformationUrl = TextUtils.isEmpty(device.getGetDevicesInformationUrl()) ?
                String.format(Locale.CHINA, OnvifDevice.DEFAULT_FORMAT_URL, device.getHost(), device.getPort()) : device.getGetDevicesInformationUrl();
        GetDeviceInformationOnvifRequest request = new GetDeviceInformationOnvifRequest(getDeviceInformationUrl, device.getUsername(), device.getPassword(), false);


        parseGetDeviceInfomation(device, doRequest(request, httpRequester));

    }

    private void parseGetDeviceInfomation(OnvifDevice onvifDevice, OnvifResponse getDeviceInformationResponse) throws Exception {
        XmlPullParserFactory parserFactory = XmlPullParserFactory.newInstance();
        parserFactory.setNamespaceAware(true);
        XmlPullParser xmlPullParser = parserFactory.newPullParser();
        xmlPullParser.setInput(new StringReader(getDeviceInformationResponse.getMessage()));
        int eventType = xmlPullParser.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                String name = xmlPullParser.getName();
                switch (name) {
                    case "Manufacturer":
                        xmlPullParser.next();
                        onvifDevice.setManufacturerName(xmlPullParser.getText());
                        break;
                    case "FirmwareVersion":
                        xmlPullParser.next();
                        onvifDevice.setFwVersion(xmlPullParser.getText());
                        break;
                    case "Model":
                        xmlPullParser.next();
                        onvifDevice.setModelName(xmlPullParser.getText());
                        break;
                    case "SerialNumber":
                        xmlPullParser.next();
                        onvifDevice.setSerialNumber(xmlPullParser.getText());
                        break;
                    case "HardwareId":
                        xmlPullParser.next();
                        onvifDevice.setHwID(xmlPullParser.getText());
                        break;
                }
            }

            eventType = xmlPullParser.next();

        }
    }
}
