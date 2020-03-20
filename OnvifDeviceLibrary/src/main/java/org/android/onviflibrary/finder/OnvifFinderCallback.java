package org.android.onviflibrary.finder;

import java.util.List;

public interface OnvifFinderCallback {

    void onFindStart();

    void onFindDiscoverer(OnvifDiscoverer discoverer, List<OnvifDiscoverer> discovererList);

    void onFindThrowable(Throwable t);

    void onFindEnd(List<OnvifDiscoverer> discovererList);
}
