package com.example.temperaturemonitor;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.temperaturemonitor.adapter.LocationAdapter;
import com.example.temperaturemonitor.model.DiscoveredDevice;
import com.example.temperaturemonitor.viewmodel.MainViewModel;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private MainViewModel viewModel;
    private LocationAdapter locationAdapter;

    private TextView textViewSelectedLocation;
    private TextView textViewCurrentTempValue;
    private TextView textViewMeanTempValue;
    private EditText editTextLocationId;
    private Button buttonDeleteLocation;
    private Button buttonScanLocal;
    private RecyclerView recyclerViewLocations;
    private ProgressBar progressBar;

    private Handler refreshHandler = new Handler(Looper.getMainLooper());
    private Runnable refreshRunnable;
    private static final long REFRESH_INTERVAL_MS = 30000;

    private String currentSelectedLocation = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textViewSelectedLocation = findViewById(R.id.textViewSelectedLocation);
        textViewCurrentTempValue = findViewById(R.id.textViewCurrentTempValue);
        textViewMeanTempValue = findViewById(R.id.textViewMeanTempValue);
        editTextLocationId = findViewById(R.id.editTextLocationId);
        buttonDeleteLocation = findViewById(R.id.buttonDeleteLocation);
        buttonScanLocal = findViewById(R.id.buttonScanLocal);
        recyclerViewLocations = findViewById(R.id.recyclerViewLocations);
        progressBar = findViewById(R.id.progressBar);

        recyclerViewLocations.setLayoutManager(new LinearLayoutManager(this));
        locationAdapter = new LocationAdapter(
                location -> {
                    Log.d(TAG, "Location clicked: " + location);
                    currentSelectedLocation = location;
                    editTextLocationId.setText(location);
                    textViewSelectedLocation.setText("Місце: " + location + "\n(завантаження...)");
                    textViewCurrentTempValue.setText(getString(R.string.current_temp_placeholder_loading));
                    textViewMeanTempValue.setText(getString(R.string.mean_temp_placeholder_loading));
                    viewModel.fetchHistory(location);
                    showLoading(true);
                },
                location -> {
                    Log.d(TAG, "Location long clicked: " + location);
                    showDeleteConfirmationDialog(location);
                }
        );
        recyclerViewLocations.setAdapter(locationAdapter);

        buttonDeleteLocation.setOnClickListener(v -> {
            String locationToDelete = editTextLocationId.getText().toString().trim();
            if (!locationToDelete.isEmpty()) {
                showDeleteConfirmationDialog(locationToDelete);
            } else {
                Toast.makeText(this, "Введіть ID локації для видалення", Toast.LENGTH_SHORT).show();
            }
        });

        buttonScanLocal.setOnClickListener(v -> {
            if (viewModel.isDiscovering().getValue() != null && viewModel.isDiscovering().getValue()) {
                viewModel.stopLocalDeviceDiscovery();
            } else {
                viewModel.startLocalDeviceDiscovery();
            }
        });

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        observeViewModel();
        viewModel.fetchLocations();
        showLoading(true);
        textViewSelectedLocation.setText(getString(R.string.location_not_selected_multiline));
        textViewCurrentTempValue.setText(getString(R.string.current_temp_placeholder));
        textViewMeanTempValue.setText(getString(R.string.mean_temp_placeholder));
        setupPeriodicRefresh();
    }

    private void observeViewModel() {
        viewModel.getLocations().observe(this, locations -> {
            showLoading(false);
            Log.d(TAG, "Locations LiveData updated. Count: " + (locations != null ? locations.size() : "null"));
            if (locations != null) {
                locationAdapter.submitList(new ArrayList<>(locations));
                if (currentSelectedLocation == null && !locations.isEmpty()) {
                    currentSelectedLocation = locations.get(0);
                    Log.d(TAG, "Auto-selecting first location: " + currentSelectedLocation);
                    editTextLocationId.setText(currentSelectedLocation);
                    textViewSelectedLocation.setText("Місце: " + currentSelectedLocation + "\n(завантаження...)");
                    viewModel.fetchHistory(currentSelectedLocation);
                    showLoading(true);
                } else if (currentSelectedLocation != null && !locations.contains(currentSelectedLocation)) {
                    Log.d(TAG, "Currently selected location " + currentSelectedLocation + " was removed or is no longer valid.");
                    currentSelectedLocation = null;
                    editTextLocationId.setText("");
                    textViewSelectedLocation.setText(getString(R.string.location_not_selected_multiline));
                    textViewCurrentTempValue.setText(getString(R.string.current_temp_placeholder));
                    textViewMeanTempValue.setText(getString(R.string.mean_temp_placeholder));
                } else if (locations.isEmpty()) {
                    currentSelectedLocation = null;
                    editTextLocationId.setText("");
                    textViewSelectedLocation.setText(getString(R.string.location_not_selected_multiline_empty_list));
                    textViewCurrentTempValue.setText(getString(R.string.current_temp_placeholder));
                    textViewMeanTempValue.setText(getString(R.string.mean_temp_placeholder));
                }
            }
        });

        viewModel.getCurrentDataDisplay().observe(this, dataPair -> {
            showLoading(false);
            if (dataPair != null && currentSelectedLocation != null) {
                Log.d(TAG, "CurrentDataDisplay LiveData updated: Temp=" + dataPair.first + ", Time=" + dataPair.second + " for location " + currentSelectedLocation);
                textViewSelectedLocation.setText("Місце: " + currentSelectedLocation + "\nОстанній: " + dataPair.second);
                textViewCurrentTempValue.setText("Поточна:\n" + dataPair.first);
            } else if (currentSelectedLocation != null) {
                textViewSelectedLocation.setText("Місце: " + currentSelectedLocation + "\n(помилка даних)");
                textViewCurrentTempValue.setText(getString(R.string.current_temp_placeholder_error));
            }
        });

        viewModel.getMeanTemperatureToday().observe(this, meanTemp -> {
            Log.d(TAG, "MeanTemperatureToday LiveData updated: " + meanTemp);
            if (meanTemp != null && currentSelectedLocation != null) {
                textViewMeanTempValue.setText("Середня за сьогодні:\n" + meanTemp);
            } else if (currentSelectedLocation != null) {
                textViewMeanTempValue.setText(getString(R.string.mean_temp_placeholder_no_data));
            }
        });

        viewModel.getDiscoveredDevices().observe(this, discoveredDevices -> {
            if (discoveredDevices != null && !discoveredDevices.isEmpty()) {
                Log.d(TAG, "Discovered devices updated: " + discoveredDevices.size());
                showDiscoveredDevicesDialog(discoveredDevices);
            } else if (viewModel.isDiscovering().getValue() != null && viewModel.isDiscovering().getValue()){
                Log.d(TAG, "Discovery active, but no devices found yet.");
            }
        });

        viewModel.isDiscovering().observe(this, isDiscovering -> {
            if (isDiscovering) {
                buttonScanLocal.setText("Зупинити пошук");
                showLoading(true);
            } else {
                buttonScanLocal.setText("Шукати локальні ESP");
                showLoading(false);
            }
        });
    }

    private void showDiscoveredDevicesDialog(List<DiscoveredDevice> devices) {
        if (devices == null || devices.isEmpty()) {
            if (viewModel.isDiscovering().getValue() != null && !viewModel.isDiscovering().getValue()) {
                Toast.makeText(this, "Локальні пристрої не знайдено.", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        List<String> deviceNames = new ArrayList<>();
        for (DiscoveredDevice device : devices) {
            deviceNames.add(device.getServiceName() + " (" + device.getHostAddress() + ")");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, deviceNames);

        new AlertDialog.Builder(this)
                .setTitle("Знайдені локальні датчики")
                .setAdapter(adapter, (dialog, which) -> {
                    DiscoveredDevice selected = devices.get(which);
                    Log.d(TAG, "User selected discovered device: " + selected.getServiceName());
                    currentSelectedLocation = selected.getServiceName();
                    editTextLocationId.setText(currentSelectedLocation);
                    textViewSelectedLocation.setText("Місце: " + currentSelectedLocation + "\n(завантаження...)");
                    viewModel.fetchHistory(currentSelectedLocation);
                    showLoading(true);
                    Toast.makeText(MainActivity.this, "Вибрано: " + selected.getServiceName(), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Закрити", null)
                .setOnDismissListener(dialog -> {
                    if (viewModel.isDiscovering().getValue() != null && viewModel.isDiscovering().getValue()) {
                    }
                })
                .show();
    }

    private void showDeleteConfirmationDialog(String location) {
        new AlertDialog.Builder(this)
                .setTitle("Підтвердження видалення")
                .setMessage("Видалити локацію '" + location + "' та всі її дані?")
                .setPositiveButton("Так", (dialog, which) -> {
                    Log.d(TAG, "Deletion confirmed for: " + location);
                    showLoading(true);
                    viewModel.deleteLocation(location, (success, message) -> {
                        Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                        if (success) {
                            if (location.equals(currentSelectedLocation)) {
                                currentSelectedLocation = null;
                                editTextLocationId.setText("");
                            }
                        } else {
                            showLoading(false);
                        }
                    });
                })
                .setNegativeButton("Ні", null)
                .show();
    }

    private void showLoading(boolean isLoading) {
        Log.d(TAG, "showLoading called with: " + isLoading);
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    private void setupPeriodicRefresh() {
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                if (currentSelectedLocation != null && !currentSelectedLocation.isEmpty()) {
                    Log.d(TAG, "Periodic refresh triggered for: " + currentSelectedLocation);
                    viewModel.fetchHistory(currentSelectedLocation);
                } else if (viewModel.getLocations().getValue() == null || viewModel.getLocations().getValue().isEmpty()){
                    Log.d(TAG, "Periodic refresh: No location selected or list empty, fetching locations list.");
                    viewModel.fetchLocations();
                }
                refreshHandler.postDelayed(this, REFRESH_INTERVAL_MS);
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Starting periodic refresh.");
        refreshHandler.postDelayed(refreshRunnable, 1000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: Stopping periodic refresh.");
        refreshHandler.removeCallbacks(refreshRunnable);
        if (viewModel.isDiscovering().getValue() != null && viewModel.isDiscovering().getValue()) {
            viewModel.stopLocalDeviceDiscovery();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: Ensuring discovery is stopped.");
        if (viewModel.isDiscovering().getValue() != null && viewModel.isDiscovering().getValue()) {
            viewModel.stopLocalDeviceDiscovery();
        }
    }
}
