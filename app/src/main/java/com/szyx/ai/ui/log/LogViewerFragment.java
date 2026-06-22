package com.szyx.ai.ui.log;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.szyx.ai.R;
import com.szyx.ai.util.AppLog;

public class LogViewerFragment extends Fragment {

    private TextView logText;
    private ScrollView scrollView;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean autoScroll = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_log_viewer, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        logText = view.findViewById(R.id.logText);
        scrollView = view.findViewById(R.id.scrollView);

        view.findViewById(R.id.btnClear).setOnClickListener(v -> {
            AppLog.clear();
            refreshLog();
            Toast.makeText(requireContext(), "日志已清除", Toast.LENGTH_SHORT).show();
        });

        view.findViewById(R.id.btnRefresh).setOnClickListener(v -> refreshLog());

        view.findViewById(R.id.btnCopy).setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) requireContext()
                    .getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText("app_log", AppLog.getAll()));
            Toast.makeText(requireContext(), "日志已复制到剪贴板", Toast.LENGTH_SHORT).show();
        });

        scrollView.getViewTreeObserver().addOnScrollChangedListener(() -> {
            View child = scrollView.getChildAt(0);
            if (child != null) {
                int diff = child.getBottom() - (scrollView.getHeight() + scrollView.getScrollY());
                autoScroll = diff < 50;
            }
        });

        refreshLog();
        startAutoRefresh();
    }

    private void refreshLog() {
        logText.setText(AppLog.getAll());
        if (autoScroll) {
            scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
        }
    }

    private void startAutoRefresh() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isResumed()) {
                    refreshLog();
                    handler.postDelayed(this, 1000);
                }
            }
        }, 1000);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacksAndMessages(null);
    }
}
