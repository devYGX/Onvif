package org.android.onviflibrary;

public interface IOnvifDeviceLoginStrategy {

    void onRequest(OnvifDevice device,
                   IHttpRequester<OnvifRequest, OnvifResponse> httpRequester) throws Exception;

}
