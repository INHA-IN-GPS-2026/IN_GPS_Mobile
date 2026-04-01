package com.example.in_gps.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.NumberPicker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.in_gps.R;
import com.example.in_gps.viewmodel.SettingsViewModel;
import com.google.android.material.chip.Chip;

public class SettingsFragment extends Fragment {

    private SettingsViewModel viewModel;
    private NumberPicker pickerHours;
    private NumberPicker pickerMinutes;
    private NumberPicker pickerSeconds;
    private Chip chipCurrentInterval;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        pickerHours = view.findViewById(R.id.picker_hours);
        pickerMinutes = view.findViewById(R.id.picker_minutes);
        pickerSeconds = view.findViewById(R.id.picker_seconds);
        chipCurrentInterval = view.findViewById(R.id.chip_current_interval);

        pickerHours.setMinValue(0);
        pickerHours.setMaxValue(23);
        pickerMinutes.setMinValue(0);
        pickerMinutes.setMaxValue(59);
        pickerSeconds.setMinValue(0);
        pickerSeconds.setMaxValue(59);

        viewModel = new ViewModelProvider(this).get(SettingsViewModel.class);

        viewModel.getHours().observe(getViewLifecycleOwner(), h -> pickerHours.setValue(h));
        viewModel.getMinutes().observe(getViewLifecycleOwner(), m -> pickerMinutes.setValue(m));
        viewModel.getSeconds().observe(getViewLifecycleOwner(), s -> pickerSeconds.setValue(s));
        viewModel.getIntervalLabel().observe(getViewLifecycleOwner(), label ->
                chipCurrentInterval.setText(label));

        view.findViewById(R.id.btn_save_interval).setOnClickListener(v ->
                viewModel.saveInterval(
                        pickerHours.getValue(),
                        pickerMinutes.getValue(),
                        pickerSeconds.getValue()
                )
        );
    }
}
