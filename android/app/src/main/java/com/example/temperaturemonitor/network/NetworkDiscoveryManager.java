package com.example.temperaturemonitor.network;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.temperaturemonitor.model.DiscoveredDevice;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class NetworkDiscoveryManager {
    private static final String TAG = "NetDiscoveryManager";
    private NsdManager nsdManager;
    private NsdManager.DiscoveryListener discoveryListener;
    private NsdManager.ResolveListener resolveListener;
    private String serviceTypeToDiscover;
    private DiscoveryCallback callback;
    private Context context;
    private Handler mainHandler;

    private List<NsdServiceInfo> servicesToResolve = new CopyOnWriteArrayList<>();
    private boolean isResolving = false;

    public interface DiscoveryCallback {
        void onDeviceFound(DiscoveredDevice device);
        void onDeviceLost(DiscoveredDevice device);
        void onDiscoveryStarted();
        void onDiscoveryFailed(String reason);
        void onDiscoveryStopped();
        void onResolutionFailed(String serviceName, int errorCode);
    }

    public NetworkDiscoveryManager(Context context) {
        this.context = context;
        this.nsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void startDiscovery(String serviceType, DiscoveryCallback callback) {
        if (this.discoveryListener != null) {
            Log.d(TAG, "Discovery already active or not properly stopped. Stopping previous.");
            stopDiscovery();
        }
        this.serviceTypeToDiscover = serviceType;
        this.callback = callback;
        this.servicesToResolve.clear();

        initializeResolveListener();
        initializeDiscoveryListener();

        if (nsdManager != null) {
            Log.d(TAG, "Starting discovery for type: " + serviceType);
            nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, discoveryListener);
            if (callback != null) {
                mainHandler.post(callback::onDiscoveryStarted);
            }
        } else {
            Log.e(TAG, "NsdManager is null. Cannot start discovery.");
            if (callback != null) {
                mainHandler.post(() -> callback.onDiscoveryFailed("NsdManager not available"));
            }
        }
    }

    public void stopDiscovery() {
        Log.d(TAG, "Stopping discovery...");
        if (nsdManager != null && discoveryListener != null) {
            try {
                nsdManager.stopServiceDiscovery(discoveryListener);
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "Error stopping service discovery: " + e.getMessage());
            }
        }
        discoveryListener = null;
        resolveListener = null;
        servicesToResolve.clear();
        isResolving = false;
        if (callback != null) {
            mainHandler.post(callback::onDiscoveryStopped);
        }
    }

    private void initializeDiscoveryListener() {
        discoveryListener = new NsdManager.DiscoveryListener() {
            @Override
            public void onDiscoveryStarted(String regType) {
                Log.d(TAG, "Service discovery started: " + regType);
            }

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                Log.d(TAG, "Service found: " + service.getServiceName() + " of type " + service.getServiceType());
                if (service.getServiceType().equals(serviceTypeToDiscover) || (service.getServiceType() + ".").equals(serviceTypeToDiscover)) {
                    servicesToResolve.add(service);
                    resolveNextService();
                } else {
                    Log.w(TAG, "Found service of different type: " + service.getServiceType());
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo service) {
                Log.e(TAG, "Service lost: " + service.getServiceName());
                if (callback != null) {
                    DiscoveredDevice lostDevice = new DiscoveredDevice(service.getServiceName(), service.getServiceType(), null, 0);
                    mainHandler.post(() -> callback.onDeviceLost(lostDevice));
                }
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.i(TAG, "Discovery stopped: " + serviceType);
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery start failed: Error code: " + errorCode);
                if (nsdManager != null) nsdManager.stopServiceDiscovery(this);
                if (callback != null) {
                    mainHandler.post(() -> callback.onDiscoveryFailed("Start discovery failed with code: " + errorCode));
                }
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery stop failed: Error code: " + errorCode);
            }
        };
    }

    private synchronized void resolveNextService() {
        if (isResolving || servicesToResolve.isEmpty()) {
            return;
        }
        NsdServiceInfo serviceToResolve = servicesToResolve.remove(0);
        if (serviceToResolve != null) {
            isResolving = true;
            Log.d(TAG, "Attempting to resolve: " + serviceToResolve.getServiceName());
            nsdManager.resolveService(serviceToResolve, resolveListener);
        }
    }

    private void initializeResolveListener() {
        resolveListener = new NsdManager.ResolveListener() {
            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.e(TAG, "Resolve failed for " + serviceInfo.getServiceName() + " Error code: " + errorCode);
                if (callback != null) {
                    mainHandler.post(() -> callback.onResolutionFailed(serviceInfo.getServiceName(), errorCode));
                }
                isResolving = false;
                resolveNextService();
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                Log.i(TAG, "Service resolved: " + serviceInfo.getServiceName());
                Log.i(TAG, "Host: " + serviceInfo.getHost() + ", Port: " + serviceInfo.getPort());

                if (callback != null) {
                    String hostAddress = serviceInfo.getHost() != null ? serviceInfo.getHost().getHostAddress() : null;
                    DiscoveredDevice device = new DiscoveredDevice(
                            serviceInfo.getServiceName(),
                            serviceInfo.getServiceType(),
                            hostAddress,
                            serviceInfo.getPort()
                    );
                    mainHandler.post(() -> callback.onDeviceFound(device));
                }
                isResolving = false;
                resolveNextService();
            }
        };
    }
}
