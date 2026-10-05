package com.example.temperaturemonitor.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.example.temperaturemonitor.R;

public class LocationAdapter extends ListAdapter<String, LocationAdapter.LocationViewHolder> {
    private final OnLocationClickListener clickListener;
    private final OnLocationLongClickListener longClickListener;

    public LocationAdapter(@NonNull OnLocationClickListener clickListener, @NonNull OnLocationLongClickListener longClickListener) {
        super(DIFF_CALLBACK);
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
    }

    @NonNull
    @Override
    public LocationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_location, parent, false);
        return new LocationViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull LocationViewHolder holder, int position) {
        String currentLocation = getItem(position);
        holder.bind(currentLocation, clickListener, longClickListener);
    }

    static class LocationViewHolder extends RecyclerView.ViewHolder {
        private final TextView locationNameTextView;

        public LocationViewHolder(@NonNull View itemView) {
            super(itemView);
            locationNameTextView = itemView.findViewById(R.id.textViewItemLocationName);
        }

        public void bind(final String location, final OnLocationClickListener clickListener, final OnLocationLongClickListener longClickListener) {
            locationNameTextView.setText(location);
            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onLocationClick(location);
                }
            });
            itemView.setOnLongClickListener(v -> {
                if (longClickListener != null) {
                    longClickListener.onLocationLongClick(location);
                    return true;
                }
                return false;
            });
        }
    }

    public interface OnLocationClickListener {
        void onLocationClick(String location);
    }
    public interface OnLocationLongClickListener {
        void onLocationLongClick(String location);
    }

    private static final DiffUtil.ItemCallback<String> DIFF_CALLBACK = new DiffUtil.ItemCallback<String>() {
        @Override
        public boolean areItemsTheSame(@NonNull String oldItem, @NonNull String newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areContentsTheSame(@NonNull String oldItem, @NonNull String newItem) {
            return oldItem.equals(newItem);
        }
    };
}