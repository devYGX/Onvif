package org.android.onviflibrary.finder;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class OnvifFinderTest {
    private static final String TAG = "OnvifFinderTest";

    @Test
    public void find() throws Exception {

        // 通过组播的形式去获取
        OnvifFinder onvifFinder = new OnvifFinder();
        // OnvifFinder onvifFinder = new OnvifFinder("255.255.255.255");

        // 通过组播的形式去获取, 只获取一个
        // OnvifFinder onvifFinder = new OnvifFinder("239.255.255.250", 1);

        // 指定IP去hu获取, 只获取一个
        // OnvifFindeonvifFinder = new OnvifFinder(/*"239.255.255.250"*/"192.168.1.87", 1);

        // 指定IP去获取, 只获取一个,并且指定超时时间未30秒
        // OnvifFinder onvifFinder = new OnvifFinder(/*"239.255.255.250"*/"192.168.1.87", 1, 30 * 1000);

        // 同步获取, 如果获取过程发生异常, 则会抛出异常
        List<OnvifDiscoverer> onvifDiscovererList = onvifFinder.find();
        Log.e(TAG, "find: "+onvifDiscovererList.size());
        for (OnvifDiscoverer onvifDiscoverer : onvifDiscovererList) {
            Log.e(TAG, "find: "+onvifDiscoverer);
        }
     /* // 同步获取, 参可以传入一个集合进行进行保存;
        List<OnvifDiscoverer> reusableList = new ArrayList<>();
        // onvifDiscovererList就是reusableList
        List<OnvifDiscoverer> onvifDiscovererList = onvifFinder.find(reusableList);

        // 通过回调获取
        onvifFinder.find(new OnvifFinderCallback() {
            @Override
            public void onFindStart() {

            }

            @Override
            public void onFindDiscoverer(OnvifDiscoverer discoverer, List<OnvifDiscoverer> discovererList) {

            }

            @Override
            public void onFindThrowable(Throwable t) {

            }

            @Override
            public void onFindEnd(List<OnvifDiscoverer> discovererList) {

            }
        });


        // 指定回调的回调线程
        Handler subscribeHandler = new Handler(Looper.getMainLooper());
        onvifFinder.find(subscribeHandler, new OnvifFinderCallback() {
            @Override
            public void onFindStart() {

            }

            @Override
            public void onFindDiscoverer(OnvifDiscoverer discoverer, List<OnvifDiscoverer> discovererList) {

            }

            @Override
            public void onFindThrowable(Throwable t) {

            }

            @Override
            public void onFindEnd(List<OnvifDiscoverer> discovererList) {

            }
        });

        // 复用集合
        onvifFinder.find(new ArrayList<>(), new OnvifFinderCallback() {
            @Override
            public void onFindStart() {

            }

            @Override
            public void onFindDiscoverer(OnvifDiscoverer discoverer, List<OnvifDiscoverer> discovererList) {

            }

            @Override
            public void onFindThrowable(Throwable t) {

            }

            @Override
            public void onFindEnd(List<OnvifDiscoverer> discovererList) {

            }
        });

        // 复用集合, 指定回调的线程,
        onvifFinder.find(new ArrayList<>(), new Handler(Looper.getMainLooper()), new OnvifFinderCallback() {
            @Override
            public void onFindStart() {

            }

            @Override
            public void onFindDiscoverer(OnvifDiscoverer discoverer, List<OnvifDiscoverer> discovererList) {

            }

            @Override
            public void onFindThrowable(Throwable t) {

            }

            @Override
            public void onFindEnd(List<OnvifDiscoverer> discovererList) {

            }
        });*/
    }
}