package com.lrsus.ttgatewaydiscovery;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;

import java.util.HashSet;

public class MainActivity extends AppCompatActivity {

    private String TAG = "GatewayDiscovery";
    private ListView gatewayList;
    // Gateway will be advertising with type "_tracker-http._tcp"
    private String SERVICE_TYPE = "_tracker-http._tcp";
    // DiscoveryListener will look for advertisements
    private NsdManager.DiscoveryListener mDiscoveryListener;

    // ResolveListener will resolve services that are discovered from discovery listener.
    private NsdManager.ResolveListener mResolveListener;
    // Manager is a service that manages listeners related to network discovery.
    private NsdManager mNsdManager;
    // Vector of NsdServiceInfo will hold all the gateways discovered.
    private HashSet<String> mServices = new HashSet<>();

    class GatewayResolveListener implements NsdManager.ResolveListener {

        @Override
        public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
            // Called when the resolve fails.  Use the error code to debug.
            Log.e(TAG, "Resolve failed" + errorCode);
        }

        @Override
        public void onServiceResolved(NsdServiceInfo serviceInfo) {
            String hostAddress = serviceInfo.getHost().getHostAddress();
            Log.i(TAG, "Found address " + hostAddress);

            if (!mServices.contains(hostAddress)) {
                Log.e(TAG, "Resolve Succeeded. " + serviceInfo);

                if (serviceInfo.getServiceType().equals(SERVICE_TYPE)) {
                    Log.d(TAG, "Same IP.");
                    return;
                }

                mServices.add(serviceInfo.getHost().getHostAddress());
            }
        }
    };

    class GatewayDiscoveryListener implements NsdManager.DiscoveryListener {

        @Override
        public void onDiscoveryStarted(String regType) {
            Log.d(TAG, "Service discovery started.");
        }

        @Override
        public void onServiceFound(NsdServiceInfo service) {
            // Check if advertisement matches tag.
            if (!service.getServiceType().equals(SERVICE_TYPE)) {;
                mNsdManager.resolveService(service, mResolveListener);
            }
        }

        @Override
        public void onServiceLost(NsdServiceInfo service) {
            Log.e(TAG, "Service lost: " + service.getServiceName());
        }

        @Override
        public void onDiscoveryStopped(String serviceType) {
            Log.i(TAG, "Discovery stopped: " + serviceType);
            mDiscoveryListener = null;
        }

        @Override
        public void onStartDiscoveryFailed(String serviceType, int errorCode) {
            Log.e(TAG, "Discovery start failed - " + serviceType + " code: " + errorCode);
            mDiscoveryListener = null;
        }

        @Override
        public void onStopDiscoveryFailed(String serviceType, int errorCode) {
            Log.e(TAG, "Discovery stop failed - " + serviceType + " code: " + errorCode);
            mDiscoveryListener = null;
        }
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        gatewayList = (ListView) findViewById(R.id.gateway_addresses);
        mNsdManager = (NsdManager) getSystemService(Context.NSD_SERVICE);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (mResolveListener == null) {
            mResolveListener = new GatewayResolveListener();
        }

        if (mDiscoveryListener == null) {
            mDiscoveryListener = new GatewayDiscoveryListener();

            // Give listener to NSD system service
            mNsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mResolveListener == null) {
            mResolveListener = new GatewayResolveListener();
        }

        if (mDiscoveryListener == null) {
            mDiscoveryListener = new GatewayDiscoveryListener();

            // Give listener to NSD system service
            mNsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
        }
    }

    @Override
    protected void onPause() {
        // Stop discovery since we don't want it to be running in the background.

        if (mDiscoveryListener != null) {
            mNsdManager.stopServiceDiscovery(mDiscoveryListener);
            mDiscoveryListener = null;
        }

        super.onPause();
    }

    @Override
    protected void onStop() {
        if (mDiscoveryListener != null) {
            mNsdManager.stopServiceDiscovery(mDiscoveryListener);
            mDiscoveryListener = null;
        }

        super.onStop();
    }
}
