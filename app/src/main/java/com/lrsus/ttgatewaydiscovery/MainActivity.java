/* Copyright 2017 Long Range Systems, LLC
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions
 *    and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of
 *    conditions and the following disclaimer in the documentation and/or other materials provided
 *    with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to
 *    endorse or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.lrsus.ttgatewaydiscovery;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.HashSet;

public class MainActivity extends AppCompatActivity {

    private String TAG = "GatewayDiscovery";
    private ListView gatewayList;
    // Gateway will be advertising with type "_tracker-http._tcp"
    private String SERVICE_TYPE = "_tracker-http._tcp";
    // DiscoveryListener will look for advertisements. It is not possible to get advertiser's
    // host information unless passing service info to resolve listener.
    private NsdManager.DiscoveryListener mDiscoveryListener;

    // ResolveListener will resolve services that are discovered from discovery listener.
    private NsdManager.ResolveListener mResolveListener;
    // Manager is a service that manages listeners related to network discovery.
    private NsdManager mNsdManager;
    // Vector of NsdServiceInfo will hold all the gateways discovered.
    private HashSet<String> mServices = new HashSet<>();
    // Adapter for list view.
    private ArrayAdapter<String> adapter;

    class GatewayResolveListener implements NsdManager.ResolveListener {

        @Override
        public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
            // Called when the resolve fails.  Use the error code to debug.
            Log.e(TAG, "Resolve failed" + errorCode);
        }

        @Override
        public void onServiceResolved(NsdServiceInfo serviceInfo) {
            final String hostAddress = serviceInfo.getHost().getHostAddress();

            // To prevent duplicate items, we'll add it to set.
            if (!mServices.contains(hostAddress)) {
                Log.e(TAG, "Resolve Succeeded. " + serviceInfo);

                if (serviceInfo.getServiceType().equals(SERVICE_TYPE)) {
                    Log.d(TAG, "Same IP.");
                    return;
                }

                // Add to set
                mServices.add(hostAddress);

                // Update UI ListView with host address.
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter.add(hostAddress);
                    }
                });
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
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        gatewayList.setAdapter(adapter);
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
