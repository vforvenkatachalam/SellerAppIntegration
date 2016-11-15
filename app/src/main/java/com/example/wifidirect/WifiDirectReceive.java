package com.example.wifidirect;

import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import com.example.R;
import com.example.wifidirect.Adapter.WifiAdapter;
import com.example.wifidirect.BroadcastReceiver.WifiDirectBroadcastReceiver;
import com.example.wifidirect.Task.DataServerAsyncTask;

public class WifiDirectReceive extends AppCompatActivity implements View.OnClickListener {


    TextView txtView;

    RecyclerView mRecyclerView;
    WifiAdapter mAdapter;
    Button btnRefresh;

    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private BroadcastReceiver mReceiver;
    private IntentFilter mFilter;
    private WifiP2pInfo info;


    private DataServerAsyncTask mDataTask;

    // For peers information.
    private List<HashMap<String, String>> peersshow = new ArrayList();


    // All the peers.
    private List peers = new ArrayList();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.wifi_direct_client);

        initView();
        initIntentFilter();
        initReceiver();
        initEvents();

        discoverPeers();
    }

    private void initView() {

        txtView = (TextView) findViewById(R.id.txtReceived);
        btnRefresh = (Button)findViewById(R.id.btnRefresh);
        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerview);
        mAdapter = new WifiAdapter(peersshow);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this.getApplicationContext()));
    }

    private void initIntentFilter() {

        mFilter = new IntentFilter();
        mFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mFilter.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);
        mFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
    }

    private void initReceiver() {

        mManager = (WifiP2pManager) getSystemService(WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, Looper.myLooper(), null);

        WifiP2pManager.PeerListListener mPeerListListerner = new WifiP2pManager.PeerListListener() {

            @Override
            public void onPeersAvailable(WifiP2pDeviceList peersList) {

                peers.clear();
                peersshow.clear();

                Collection<WifiP2pDevice> aList = peersList.getDeviceList();
                peers.addAll(aList);

                for (int i = 0; i < aList.size(); i++) {
                    WifiP2pDevice a = (WifiP2pDevice) peers.get(i);
                    HashMap<String, String> map = new HashMap<String, String>();
                    map.put("name", a.deviceName);
                    map.put("address", a.deviceAddress);
                    peersshow.add(map);
                }

                mAdapter = new WifiAdapter(peersshow);
                mRecyclerView.setAdapter(mAdapter);
                mRecyclerView.setLayoutManager(new LinearLayoutManager(WifiDirectReceive.this));

                mAdapter.SetOnItemClickListener(new WifiAdapter.OnItemClickListener() {

                    @Override
                    public void OnItemClick(View view, int position) {

                        createConnect(peersshow.get(position).get("address"), peersshow.get(position).get("name"));
                    }

                    @Override
                    public void OnItemLongClick(View view, int position) {

                    }
                });
            }
        };

        WifiP2pManager.ConnectionInfoListener mInfoListener = new WifiP2pManager.ConnectionInfoListener() {

            @Override
            public void onConnectionInfoAvailable(WifiP2pInfo minfo) {

                Log.i("bizzmark", "InfoAvailable is on");
                Toast.makeText(getApplicationContext(),"ConnectionInfoListener onConnectionInfoAvailable.",Toast.LENGTH_SHORT).show();

                info = minfo;

                if (info.groupFormed && info.isGroupOwner) {

                    Toast.makeText(getApplicationContext(),"WifiP2pManager.ConnectionInfoListener onConnectionInfoAvailable: Group owner.",Toast.LENGTH_SHORT).show();
                    Log.i("bizzmark", "Receive server start.");

                    mDataTask = new DataServerAsyncTask(WifiDirectReceive.this, txtView);
                    mDataTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }

            }
        };

        mReceiver = new WifiDirectBroadcastReceiver(mManager, mChannel, this, mPeerListListerner, mInfoListener);
    }

    private void createConnect(String address, final String name) {

        //WifiP2pDevice device;
        WifiP2pConfig config = new WifiP2pConfig();
        Log.i("bizzmark", address);

        config.deviceAddress = address;

        config.wps.setup = WpsInfo.PBC;
        Log.i("bizzmark", "MAC IS " + address);

        // Seller app so group owner.
        config.groupOwnerIntent = 15;

        mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {

                Toast.makeText(getApplicationContext(),"WifiP2pManager.connect success.",Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int reason) {

                Toast.makeText(getApplicationContext(),"WifiP2pManager.connect failure reason: " + reason,Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initEvents() {

        btnRefresh.setOnClickListener(this);

        discoverPeers();

        mAdapter.SetOnItemClickListener(new WifiAdapter.OnItemClickListener() {

            @Override
            public void OnItemClick(View view, int position) {

                createConnect(peersshow.get(position).get("address"), peersshow.get(position).get("name"));
            }

            @Override
            public void OnItemLongClick(View view, int position) {

            }
        });
    }

    private void discoverPeers() {

        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                Toast.makeText(getApplicationContext(),"WifiP2pManager.discoverPeers success.",Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int reason) {
                Toast.makeText(getApplicationContext(),"WifiP2pManager.discoverPeers failure. Reason: " + reason,Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void StopConnect() {

        // SetButtonGone();
        mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {

            }

            @Override
            public void onFailure(int reason) {

            }
        });
    }

    @Override
    protected void onResume() {

        super.onResume();
        registerReceiver(mReceiver, mFilter);
    }

    @Override
    public void onPause() {

        super.onPause();
        Log.i("bizzmark", "on pause.");
        unregisterReceiver(mReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        StopConnect();
    }

    public void ResetReceiver() {

        unregisterReceiver(mReceiver);
        registerReceiver(mReceiver, mFilter);

    }

    @Override
    public void onClick(View view) {
        ResetReceiver();
    }
}