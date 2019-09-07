package org.android.onviflibrary;

public interface IOnvifDeviceRequestHandler {

    void onRequest(OnvifDevice device,
                   IHttpRequester<OnvifRequest, OnvifResponse> httpRequester) throws Exception;

}
