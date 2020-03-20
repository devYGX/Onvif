package org.android.onvif;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import org.android.ffmpeg.OnvifPlayer;
import org.android.ffmpeg.OnvifPlayerObserver;
import org.android.ffmpeg.OnvifStreamCallback;
import org.android.onviflibrary.MediaProfile;
import org.android.onviflibrary.OnvifDevice;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class OnvifDisplayActivity extends AppCompatActivity implements SurfaceHolder.Callback, OnvifStreamCallback, RadioGroup.OnCheckedChangeListener, OnvifPlayerObserver {
    private static final String TAG = "OnvifDisplayActivity";
    public static final String HOST = "host";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    private SurfaceView mSurfaceView;
    private String onvifHost;
    private String onvifUsername;
    private String onvifPassword;
    private RadioGroup radioGroup;
    private OnvifPlayer onvifPlayer;
    private Surface holderSurface;
    private MediaProfile mediaProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onvif_display);

        mSurfaceView = findViewById(R.id.surfaceView);
        radioGroup = findViewById(R.id.radioGroup);
        radioGroup.setOnCheckedChangeListener(this);
        mSurfaceView.getHolder().addCallback(this);
        onvifPlayer = new OnvifPlayer();
        onvifPlayer.setFrameCallback(this);
        onvifPlayer.setPlayerObserver(this);

        initData();
    }

    private void initData() {
        onvifHost = getIntent().getStringExtra(HOST);
        onvifUsername = getIntent().getStringExtra(USERNAME);
        onvifPassword = getIntent().getStringExtra(PASSWORD);

        if (TextUtils.isEmpty(onvifHost) || TextUtils.isEmpty(onvifUsername) || TextUtils.isEmpty(onvifPassword)) {
            return;
        }

        OnvifDevice.Builder builder = new OnvifDevice.Builder().host(onvifHost)
                .username(onvifUsername)
                .password(onvifPassword);
        Disposable disposable = Observable.just(builder)
                .map(new Function<OnvifDevice.Builder, OnvifDevice>() {
                    @Override
                    public OnvifDevice apply(OnvifDevice.Builder builder) throws Exception {
                        return builder.login();
                    }
                })
                .doOnNext(new Consumer<OnvifDevice>() {
                    @Override
                    public void accept(OnvifDevice onvifDevice) throws Exception {
                        List<MediaProfile> mediaProfiles = onvifDevice.getMediaProfiles();
                        String onvifMediaStreamUri = mediaProfiles.get(0).getOnvifMediaStreamUri();
                        onvifPlayer.setPath(onvifMediaStreamUri);
                        onvifPlayer.prepare();
                        onvifPlayer.start();
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<OnvifDevice>() {
                    @Override
                    public void accept(OnvifDevice onvifDevice) throws Exception {
                        List<MediaProfile> mediaProfiles = onvifDevice.getMediaProfiles();
                        RadioButton defaultSelect = null;
                        for (int i = 0; i < mediaProfiles.size(); i++) {
                            MediaProfile mediaProfile = mediaProfiles.get(i);
                            RadioButton radioButton = new RadioButton(radioGroup.getContext());
                            radioButton.setTag(mediaProfile);
                            // radioButton.setId(i);
                            MediaProfile.VideoEncoderConfiguration configuration = mediaProfile.getVideoEncoderConfiguration();
                            MediaProfile.VideoResolution resolution = configuration.getResolution();
                            radioButton.setText((resolution.getWidth() + "x" + resolution.getHeight()).concat("\n")
                                    .concat(mediaProfile.getMediaStreamUri()));
                            radioButton.setTextColor(Color.WHITE);

                            RadioGroup.LayoutParams params = new RadioGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                            params.topMargin = 25;
                            params.leftMargin = 25;
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                radioButton.setButtonTintList(ColorStateList.valueOf(0xFFFFFFFF));
                            }
                            radioGroup.addView(radioButton, params);
                            if (i == 0) {
                                defaultSelect = radioButton;
                            }
                        }
                        defaultSelect.setChecked(true);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        throwable.printStackTrace();
                    }
                });
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        holderSurface = holder.getSurface();
        onvifPlayer.setSurface(holderSurface);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        holderSurface = null;
        if (onvifPlayer != null) {
            onvifPlayer.setSurface(null);
        }
    }

    @Override
    public void onFrame(byte[] buf, int width, int height) {
        //  Log.d(TAG, "onFrame: " + Thread.currentThread().getName() + ", " + buf.length + ", " + width + ", " + height);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        onvifPlayer.release();
    }

    boolean handle;

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {

        RadioButton child = (RadioButton) group.findViewById(checkedId);
        Object tag = child.getTag();
        mediaProfile = tag instanceof MediaProfile ? ((MediaProfile) tag) : null;
        Log.d(TAG, "onCheckedChanged: " + checkedId + ", " + mediaProfile + ", " + child);
        if (mediaProfile == null) {
            return;
        }

        if (handle) return;
        handle = true;

        Disposable subscribe = Observable.just(mediaProfile)
                .doOnNext(new Consumer<MediaProfile>() {
                    @Override
                    public void accept(MediaProfile mediaProfile) throws Exception {
                        onvifPlayer.reset();
                        onvifPlayer.setPath(mediaProfile.getOnvifMediaStreamUri());
                        onvifPlayer.prepare();
                        onvifPlayer.start();
                    }
                }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<MediaProfile>() {
                    @Override
                    public void accept(MediaProfile mediaProfile) throws Exception {
                        handle = false;
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        handle = false;
                    }
                });
    }

    @Override
    public void onPrepared(OnvifPlayer player, int width, int height) {
        Log.d(TAG, "onPrepared: " + Thread.currentThread().getName() + ", " + width + ", " + height);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) mSurfaceView.getLayoutParams();
                params.dimensionRatio = "W," + height + ":" + width;
                mSurfaceView.setLayoutParams(params);
            }
        });
    }

    @Override
    public void onPlayDisconnect() {
        Log.d(TAG, "onPlayDisconnect: ");
    }

    @Override
    public void onPlayFinish() {
        Log.d(TAG, "onPlayFinish: ");
    }
}
