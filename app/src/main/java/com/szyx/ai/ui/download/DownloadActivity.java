package com.szyx.ai.ui.download;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.szyx.ai.R;
import com.szyx.ai.ui.MainActivity;
import com.szyx.ai.viewmodel.DownloadViewModel;

import java.io.File;

public class DownloadActivity extends AppCompatActivity {

    private DownloadViewModel viewModel;
    private TextInputEditText etModelPath;

    private final ActivityResultLauncher<Intent> storagePermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (Environment.isExternalStorageManager()) {
                        Toast.makeText(this, "权限已授予", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "需要存储权限才能访问模型文件", Toast.LENGTH_LONG).show();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download);

        viewModel = new ViewModelProvider(this).get(DownloadViewModel.class);

        if (viewModel.checkModelsExist()) {
            goToMain();
            return;
        }

        etModelPath = findViewById(R.id.etModelPath);
        Button btnConfirm = findViewById(R.id.btnConfirm);
        Button btnSkip = findViewById(R.id.btnSkip);
        Button btnPermission = findViewById(R.id.btnPermission);

        // Restore saved path
        String savedPath = viewModel.getSavedModelPath();
        if (!savedPath.isEmpty()) {
            etModelPath.setText(savedPath);
        }

        // Permission button
        btnPermission.setOnClickListener(v -> requestStoragePermission());

        btnConfirm.setOnClickListener(v -> {
            String path = etModelPath.getText() != null ? etModelPath.getText().toString().trim() : "";
            if (path.isEmpty()) {
                etModelPath.setError("请输入模型文件路径");
                return;
            }

            try {
                File modelFile = new File(path);
                if (!modelFile.exists()) {
                    etModelPath.setError("文件未找到，请检查路径并授予存储权限");
                    return;
                }
                if (modelFile.length() < 100 * 1024 * 1024) {
                    etModelPath.setError("文件太小，不是有效的模型文件");
                    return;
                }
            } catch (SecurityException e) {
                etModelPath.setError("无权限访问此路径，请先授予存储权限");
                return;
            }

            viewModel.saveModelPath(path);
            Toast.makeText(this, "模型路径已保存", Toast.LENGTH_SHORT).show();
            goToMain();
        });

        btnSkip.setOnClickListener(v -> goToMain());
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                storagePermissionLauncher.launch(intent);
            } else {
                Toast.makeText(this, "权限已授予", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void goToMain() {
        try {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "启动应用失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }
}
