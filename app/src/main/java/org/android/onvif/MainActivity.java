package org.android.onvif;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import org.android.onviflibrary.OnvifDevice;
import org.android.onviflibrary.finder.OnvifDiscoverer;
import org.android.onviflibrary.finder.OnvifFinder;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, OnItemClickListener {
    private static final String TAG = "MainActivity";
    private EditText etHost;
    private EditText etUsername;
    private EditText etPassword;
    private RecyclerView mRecyclerView;
    private SimpleAdapter<OnvifDiscoverer> simpleAdapter;
    private ArrayList<OnvifDiscoverer> onvifDiscoverers;
    private int permissionRequestCode = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();

        checkPermission();
    }

    private void checkPermission() {
        String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE};
        List<String> permissionList = null;
        for (String s : permissions) {
            int i = ContextCompat.checkSelfPermission(getApplicationContext(), s);
            if (i != PackageManager.PERMISSION_GRANTED) {
                if (permissionList == null) permissionList = new ArrayList<>();
                permissionList.add(s);
            }
        }

        if (permissionList != null && permissionList.size() > 0) {
            String[] toArray = permissionList.toArray(new String[0]);
            ActivityCompat.requestPermissions(this,
                    toArray, permissionRequestCode);
        }
    }

    private void initView() {
        findViewById(R.id.btnPlay).setOnClickListener(this);
        etHost = findViewById(R.id.etHost);
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        mRecyclerView = findViewById(R.id.recyclerView);

        onvifDiscoverers = new ArrayList<>();
        simpleAdapter = new SimpleAdapter<>(onvifDiscoverers, new Fun<OnvifDiscoverer, String>() {
            @Override
            public String apply(OnvifDiscoverer onvifDiscoverer) {
                return onvifDiscoverer.getHost();
            }
        });
        simpleAdapter.setOnItemClickListener(this);
        mRecyclerView.setAdapter(simpleAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.menu_scan:
                doScanOnvifDevices();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void doScanOnvifDevices() {

        OnvifFinder onvifFinder = new OnvifFinder();
        onvifDiscoverers.clear();
        Observable.just(onvifFinder)
                .map(new Function<OnvifFinder, List<OnvifDiscoverer>>() {
                    @Override
                    public List<OnvifDiscoverer> apply(OnvifFinder onvifFinder) throws Exception {
                        return onvifFinder.find();
                    }
                }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<List<OnvifDiscoverer>>() {
                    @Override
                    public void accept(List<OnvifDiscoverer> list) throws Exception {
                        onvifDiscoverers.addAll(list);
                        simpleAdapter.notifyDataSetChanged();
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        throwable.printStackTrace();
                        Toast.makeText(MainActivity.this,
                                "exception: " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onClick(View v) {
        String host = etHost.getText().toString().trim();
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        OnvifDevice.Builder builder = new OnvifDevice.Builder()
                .host(host)
                .username(username)
                .password(password);
        v.setEnabled(false);
        Observable.just(builder)
                .map(new Function<OnvifDevice.Builder, OnvifDevice>() {
                    @Override
                    public OnvifDevice apply(OnvifDevice.Builder builder) throws Exception {
                        return builder.login();
                    }
                }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<OnvifDevice>() {
                    @Override
                    public void accept(OnvifDevice onvifDevice) throws Exception {
                        Log.d(TAG, "accept: " + onvifDevice.toString());
                        Intent intent = new Intent(MainActivity.this, OnvifDisplayActivity.class);
                        intent.putExtra(OnvifDisplayActivity.HOST, onvifDevice.getHost());
                        intent.putExtra(OnvifDisplayActivity.USERNAME, onvifDevice.getUsername());
                        intent.putExtra(OnvifDisplayActivity.PASSWORD, onvifDevice.getPassword());
                        startActivity(intent);
                        v.setEnabled(true);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        throwable.printStackTrace();
                        v.setEnabled(true);
                    }
                });
    }


    @Override
    public void onItemClickListener(RecyclerView view, int position) {
        OnvifDiscoverer discoverer = onvifDiscoverers.get(position);
        etHost.setText(discoverer.getHost());
        etUsername.setText("");
        etPassword.setText("");
    }
}
