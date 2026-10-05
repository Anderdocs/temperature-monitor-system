package com.example.temperaturemonitor.viewmodel;

import android.app.Application;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.temperaturemonitor.model.DiscoveredDevice;
import com.example.temperaturemonitor.model.TemperatureMeasurement;
import com.example.temperaturemonitor.network.NetworkDiscoveryManager;
import com.example.temperaturemonitor.network.TemperatureApiService;
import com.example.temperaturemonitor.network.RetrofitClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainViewModel extends AndroidViewModel {
    private static final String TAG = "MainViewModel";

    private TemperatureApiService apiService;
    private NetworkDiscoveryManager networkDiscoveryManager;

    private MutableLiveData<List<String>> _locations = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<String>> getLocations() { return _locations; }

    private MutableLiveData<Pair<String, String>> _currentDataDisplay = new MutableLiveData<>();
    public LiveData<Pair<String, String>> getCurrentDataDisplay() { return _currentDataDisplay; }

    private MutableLiveData<String> _meanTemperatureToday = new MutableLiveData<>();
    public LiveData<String> getMeanTemperatureToday() { return _meanTemperatureToday; }

    private MutableLiveData<List<DiscoveredDevice>> _discoveredDevices = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<DiscoveredDevice>> getDiscoveredDevices() { return _discoveredDevices; }

    private MutableLiveData<Boolean> _isDiscovering = new MutableLiveData<>(false);
    public LiveData<Boolean> isDiscovering() { return _isDiscovering; }

    private static final DateTimeFormatter SERVER_TIMESTAMP_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter DISPLAY_TIME_ONLY_FORMATTER = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault());

    public static final String SERVICE_TYPE = "_esp-temp-sensor._tcp";
    private Set<DiscoveredDevice> uniqueDiscoveredDevicesSet = new HashSet<>();

    public MainViewModel(@NonNull Application application) {
        super(application);
        apiService = RetrofitClient.getApiService();
        networkDiscoveryManager = new NetworkDiscoveryManager(application.getApplicationContext());
        _currentDataDisplay.setValue(new Pair<>("-- °C", "(немає даних)"));
        _meanTemperatureToday.setValue("-- °C");
    }

    public void startLocalDeviceDiscovery() {
        if (_isDiscovering.getValue() != null && _isDiscovering.getValue()) {
            Log.i(TAG, "Пошук вже запущено.");
            return;
        }
        Log.d(TAG, "Starting local device discovery...");
        uniqueDiscoveredDevicesSet.clear();
        _discoveredDevices.postValue(new ArrayList<>(uniqueDiscoveredDevicesSet));
        _isDiscovering.postValue(true);

        networkDiscoveryManager.startDiscovery(SERVICE_TYPE, new NetworkDiscoveryManager.DiscoveryCallback() {
            @Override
            public void onDeviceFound(DiscoveredDevice device) {
                Log.i(TAG, "ViewModel: Device Found - " + device.getServiceName() + " at " + device.getHostAddress());
                if (device.getHostAddress() != null && !device.getHostAddress().isEmpty()) {
                    boolean added = uniqueDiscoveredDevicesSet.add(device);
                    if (added) {
                        _discoveredDevices.postValue(new ArrayList<>(uniqueDiscoveredDevicesSet));
                    }
                }
            }

            @Override
            public void onDeviceLost(DiscoveredDevice device) {
                Log.i(TAG, "ViewModel: Device Lost - " + device.getServiceName());
                boolean removed = uniqueDiscoveredDevicesSet.remove(device);
                if (removed) {
                    _discoveredDevices.postValue(new ArrayList<>(uniqueDiscoveredDevicesSet));
                }
            }

            @Override
            public void onDiscoveryStarted() {
                Log.d(TAG, "ViewModel: Discovery Started.");
            }

            @Override
            public void onDiscoveryFailed(String reason) {
                Log.e(TAG, "ViewModel: Discovery Failed - " + reason);
                _isDiscovering.postValue(false);
            }

            @Override
            public void onDiscoveryStopped() {
                Log.d(TAG, "ViewModel: Discovery Stopped.");
                _isDiscovering.postValue(false);
            }
            @Override
            public void onResolutionFailed(String serviceName, int errorCode) {
                Log.e(TAG, "ViewModel: Resolution failed for " + serviceName + ", code: " + errorCode);
            }
        });
    }

    public void stopLocalDeviceDiscovery() {
        Log.d(TAG, "Stopping local device discovery...");
        networkDiscoveryManager.stopDiscovery();
        _isDiscovering.postValue(false);
    }

    public void fetchLocations() {
        Log.d(TAG, "Fetching locations...");
        apiService.getAllLocations().enqueue(new Callback<List<String>>() {
            @Override
            public void onResponse(Call<List<String>> call, Response<List<String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Locations fetched: " + response.body().size());
                    _locations.postValue(response.body());
                    if (response.body().isEmpty()){
                        Log.i(TAG, "Список локацій порожній.");
                    }
                } else {
                    Log.e(TAG, "Error fetching locations: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<String>> call, Throwable t) {
                Log.e(TAG, "Network error fetching locations: " + t.getMessage());
                t.printStackTrace();
            }
        });
    }

    public void fetchHistory(String location) {
        if (location == null || location.isEmpty()) {
            Log.w(TAG, "fetchHistory called with null or empty location");
            _currentDataDisplay.postValue(new Pair<>("-- °C", "(локація не вибрана)"));
            _meanTemperatureToday.postValue("-- °C");
            return;
        }
        Log.d(TAG, "Fetching history for: " + location);
        apiService.getTemperatureHistory(location).enqueue(new Callback<List<TemperatureMeasurement>>() {
            @Override
            public void onResponse(Call<List<TemperatureMeasurement>> call, Response<List<TemperatureMeasurement>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "History fetched for " + location + ": " + response.body().size() + " records");
                    processAndDisplayLatest(response.body(), location);
                } else {
                    Log.e(TAG, "Error fetching history for " + location + ": " + response.code());
                    _currentDataDisplay.postValue(new Pair<>("-- °C", "(немає даних)"));
                    _meanTemperatureToday.postValue("-- °C");
                }
            }

            @Override
            public void onFailure(Call<List<TemperatureMeasurement>> call, Throwable t) {
                Log.e(TAG, "Network error fetching history for " + location + ": " + t.getMessage());
                _currentDataDisplay.postValue(new Pair<>("-- °C", "(помилка мережі)"));
                _meanTemperatureToday.postValue("-- °C");
                t.printStackTrace();
            }
        });
    }

    private void processAndDisplayLatest(List<TemperatureMeasurement> history, String location) {
        if (history == null || history.isEmpty()) {
            Log.d(TAG, "No history data for " + location);
            _currentDataDisplay.postValue(new Pair<>("-- °C", "(немає даних)"));
            _meanTemperatureToday.postValue("-- °C");
            return;
        }

        TemperatureMeasurement latestRecord = null;
        LocalDateTime latestTimestamp = null;
        double sumTemperatureToday = 0;
        int countToday = 0;
        LocalDate today = LocalDate.now();

        for (TemperatureMeasurement record : history) {
            if (record.getTimestamp() == null || record.getTemperature() == null) continue;
            try {
                LocalDateTime recordTimestamp = LocalDateTime.parse(record.getTimestamp(), SERVER_TIMESTAMP_FORMATTER);
                if (latestTimestamp == null || recordTimestamp.isAfter(latestTimestamp)) {
                    latestTimestamp = recordTimestamp;
                    latestRecord = record;
                }
                if (recordTimestamp.toLocalDate().equals(today)) {
                    sumTemperatureToday += record.getTemperature();
                    countToday++;
                }
            } catch (DateTimeParseException e) {
                Log.e(TAG, "Error parsing date from history: " + record.getTimestamp(), e);
            }
        }

        if (latestRecord != null && latestTimestamp != null && latestRecord.getTemperature() != null) {
            String tempText = String.format(Locale.US, "%.1f°C", latestRecord.getTemperature());
            String timeText = latestTimestamp.format(DISPLAY_TIME_ONLY_FORMATTER);
            _currentDataDisplay.postValue(new Pair<>(tempText, timeText));
            Log.d(TAG, "Latest data for " + location + ": " + tempText + " at " + timeText);
        } else {
            Log.d(TAG, "No valid latest record found for " + location);
            _currentDataDisplay.postValue(new Pair<>("-- °C", "(дані некоректні)"));
        }

        if (countToday > 0) {
            double avgTemp = sumTemperatureToday / countToday;
            _meanTemperatureToday.postValue(String.format(Locale.US, "%.1f°C", avgTemp));
            Log.d(TAG, "Mean temp for " + location + " today: " + avgTemp);
        } else {
            _meanTemperatureToday.postValue("-- °C");
            Log.d(TAG, "No data for " + location + " today to calculate mean temp.");
        }
        Log.i(TAG, "Дані для " + location + " оновлено (без Toast).");
    }

    public void deleteLocation(String location, final DeletionCallback callback) {
        if (location == null || location.isEmpty()) {
            if (callback != null) callback.onResult(false, "Назва локації не може бути порожньою.");
            return;
        }
        Log.d(TAG, "Deleting location: " + location);
        apiService.deleteLocation(location).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Location deleted successfully: " + location);
                    fetchLocations();
                    if (callback != null) callback.onResult(true, "Локація '" + location + "' видалена");
                } else {
                    Log.e(TAG, "Error deleting location " + location + ": " + response.code());
                    if (callback != null) callback.onResult(false, "Помилка видалення: " + response.code());
                }
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "Network error deleting location " + location + ": " + t.getMessage());
                if (callback != null) callback.onResult(false, "Помилка мережі при видаленні: " + t.getMessage());
                t.printStackTrace();
            }
        });
    }
    public interface DeletionCallback {
        void onResult(boolean success, String message);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        Log.d(TAG, "ViewModel onCleared. Stopping discovery.");
        stopLocalDeviceDiscovery();
    }
}
