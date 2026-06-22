package com.szyx.ai.ui.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.szyx.ai.R;
import com.google.android.material.textfield.TextInputEditText;

public class SettingsFragment extends Fragment {

    private SharedPreferences prefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        prefs = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE);

        Slider sliderCtxSize = view.findViewById(R.id.sliderCtxSize);
        Slider sliderThreads = view.findViewById(R.id.sliderThreads);
        Slider sliderTemperature = view.findViewById(R.id.sliderTemperature);
        TextView textCtxSize = view.findViewById(R.id.textCtxSize);
        TextView textThreads = view.findViewById(R.id.textThreads);
        TextView textTemperature = view.findViewById(R.id.textTemperature);
        TextView textCpuInfo = view.findViewById(R.id.textCpuInfo);
        Button btnSave = view.findViewById(R.id.btnSaveSettings);

        // Xiaomi API controls
        SwitchMaterial switchXiaomiApi = view.findViewById(R.id.switchXiaomiApi);
        TextInputEditText editApiKey = view.findViewById(R.id.editApiKey);
        TextView textApiStatus = view.findViewById(R.id.textApiStatus);

        // DeepSeek API controls
        SwitchMaterial switchDeepSeekApi = view.findViewById(R.id.switchDeepSeekApi);
        TextInputEditText editDeepSeekApiKey = view.findViewById(R.id.editDeepSeekApiKey);
        TextView textDeepSeekApiStatus = view.findViewById(R.id.textDeepSeekApiStatus);

        // Custom API controls
        SwitchMaterial switchCustomApi = view.findViewById(R.id.switchCustomApi);
        TextInputEditText editCustomApiUrl = view.findViewById(R.id.editCustomApiUrl);
        TextInputEditText editCustomModelName = view.findViewById(R.id.editCustomModelName);
        TextInputEditText editCustomApiKey = view.findViewById(R.id.editCustomApiKey);
        TextView textCustomApiStatus = view.findViewById(R.id.textCustomApiStatus);

        // Load Xiaomi API settings
        boolean useXiaomiApi = prefs.getBoolean("use_xiaomi_api", false);
        String apiKey = prefs.getString("xiaomi_api_key", "");
        switchXiaomiApi.setChecked(useXiaomiApi);
        editApiKey.setText(apiKey);
        updateApiStatus(textApiStatus, useXiaomiApi);
        switchXiaomiApi.setOnCheckedChangeListener((buttonView, isChecked) ->
                updateApiStatus(textApiStatus, isChecked));

        // Load DeepSeek API settings
        boolean useDeepSeekApi = prefs.getBoolean("use_deepseek_api", false);
        String deepSeekApiKey = prefs.getString("deepseek_api_key", "");
        switchDeepSeekApi.setChecked(useDeepSeekApi);
        editDeepSeekApiKey.setText(deepSeekApiKey);
        updateDeepSeekApiStatus(textDeepSeekApiStatus, useDeepSeekApi);
        switchDeepSeekApi.setOnCheckedChangeListener((buttonView, isChecked) ->
                updateDeepSeekApiStatus(textDeepSeekApiStatus, isChecked));

        // Load Custom API settings
        boolean useCustomApi = prefs.getBoolean("use_custom_api", false);
        String customApiUrl = prefs.getString("custom_api_url", "");
        String customModelName = prefs.getString("custom_model_name", "");
        String customApiKey = prefs.getString("custom_api_key", "");
        switchCustomApi.setChecked(useCustomApi);
        editCustomApiUrl.setText(customApiUrl);
        editCustomModelName.setText(customModelName);
        editCustomApiKey.setText(customApiKey);
        updateCustomApiStatus(textCustomApiStatus, useCustomApi);
        switchCustomApi.setOnCheckedChangeListener((buttonView, isChecked) ->
                updateCustomApiStatus(textCustomApiStatus, isChecked));

        // Show CPU core count
        int cpuCores = Runtime.getRuntime().availableProcessors();
        textCpuInfo.setText("CPU 线程数 (设备有 " + cpuCores + " 个核心，推荐 6)");

        // Device info
        TextView textDeviceInfo = view.findViewById(R.id.textDeviceInfo);
        Runtime rt = Runtime.getRuntime();
        long maxMemMB = rt.maxMemory() / 1024 / 1024;
        textDeviceInfo.setText("可用内存: " + maxMemMB + " MB | CPU: " + cpuCores + " 核");

        sliderCtxSize.setValue(prefs.getInt("n_ctx", 8192));
        sliderThreads.setValue(prefs.getInt("n_threads", 6));
        sliderTemperature.setValue(prefs.getFloat("temperature", 0.7f));

        textCtxSize.setText(String.valueOf((int) sliderCtxSize.getValue()));
        textThreads.setText(String.valueOf((int) sliderThreads.getValue()));
        textTemperature.setText(String.valueOf(sliderTemperature.getValue()));

        sliderCtxSize.addOnChangeListener((slider, value, fromUser) ->
                textCtxSize.setText(String.valueOf((int) value)));
        sliderThreads.addOnChangeListener((slider, value, fromUser) ->
                textThreads.setText(String.valueOf((int) value)));
        sliderTemperature.addOnChangeListener((slider, value, fromUser) ->
                textTemperature.setText(String.format("%.1f", value)));

        btnSave.setOnClickListener(v -> {
            prefs.edit()
                    .putBoolean("use_xiaomi_api", switchXiaomiApi.isChecked())
                    .putString("xiaomi_api_key", editApiKey.getText().toString().trim())
                    .putBoolean("use_deepseek_api", switchDeepSeekApi.isChecked())
                    .putString("deepseek_api_key", editDeepSeekApiKey.getText().toString().trim())
                    .putBoolean("use_custom_api", switchCustomApi.isChecked())
                    .putString("custom_api_url", editCustomApiUrl.getText().toString().trim())
                    .putString("custom_model_name", editCustomModelName.getText().toString().trim())
                    .putString("custom_api_key", editCustomApiKey.getText().toString().trim())
                    .putInt("n_ctx", (int) sliderCtxSize.getValue())
                    .putInt("n_threads", (int) sliderThreads.getValue())
                    .putFloat("temperature", sliderTemperature.getValue())
                    .apply();
            Toast.makeText(requireContext(), "设置已保存", Toast.LENGTH_SHORT).show();
        });
    }

    private void updateApiStatus(TextView statusView, boolean enabled) {
        statusView.setText(enabled ? "已启用：优先使用小米模型 API" : "关闭：未启用");
    }

    private void updateDeepSeekApiStatus(TextView statusView, boolean enabled) {
        statusView.setText(enabled ? "已启用：可在聊天中切换 DeepSeek" : "关闭：未启用 DeepSeek");
    }

    private void updateCustomApiStatus(TextView statusView, boolean enabled) {
        statusView.setText(enabled ? "已启用：可在聊天中切换自定义模型" : "关闭：未启用自定义模型");
    }
}
