package com.szyx.ai.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

/**
 * Background worker for running condensation on sessions.
 * Currently unused - condensation runs inline in ChatViewModel.
 * Reserved for future use if background condensation is needed.
 */
public class CondensationWorker extends Worker {

    public static final String KEY_SESSION_ID = "session_id";

    public CondensationWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        // Condensation is currently handled inline in ChatViewModel.sendMessage()
        // This worker is reserved for future background condensation if needed
        return Result.success();
    }
}
