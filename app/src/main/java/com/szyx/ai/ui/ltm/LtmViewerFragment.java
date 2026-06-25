package com.szyx.ai.ui.ltm;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.szyx.ai.R;
import com.szyx.ai.data.db.entity.LongTermMemoryEntity;
import com.szyx.ai.data.repository.ChatRepository;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LtmViewerFragment extends Fragment {

    private ChatRepository repo;
    private ExecutorService executor;
    private long sessionId;
    private LtmAdapter adapter;
    private TextView emptyText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ltm_viewer, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionId = getArguments() != null ? getArguments().getLong("sessionId", 0) : 0;
        if (sessionId <= 0) return;

        repo = new ChatRepository(requireActivity().getApplication());
        executor = Executors.newSingleThreadExecutor();

        emptyText = view.findViewById(R.id.emptyText);
        RecyclerView recyclerView = view.findViewById(R.id.ltmRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new LtmAdapter(memory -> {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("删除记忆")
                    .setMessage("确定删除这条记忆？")
                    .setPositiveButton("删除", (d, w) -> {
                        repo.deleteLTM(memory);
                        loadMemories(null);
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });
        recyclerView.setAdapter(adapter);

        ChipGroup chipGroup = view.findViewById(R.id.chipGroup);
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                loadMemories(null);
                return;
            }
            int id = checkedIds.get(0);
            if (id == R.id.chipAll) loadMemories(null);
            else if (id == R.id.chipPlot) loadMemories("plot");
            else if (id == R.id.chipAffection) loadMemories("affection");
            else if (id == R.id.chipChoice) loadMemories("choice");
            else if (id == R.id.chipEvent) loadMemories("event");
        });

        loadMemories(null);
    }

    private void loadMemories(@Nullable String tag) {
        executor.execute(() -> {
            List<LongTermMemoryEntity> memories;
            if (tag == null) {
                memories = repo.getLTMsForSession(sessionId);
            } else {
                memories = repo.getLTMsByTag(sessionId, tag);
            }
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    adapter.submitList(memories);
                    emptyText.setVisibility(memories.isEmpty() ? View.VISIBLE : View.GONE);
                });
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (executor != null) executor.shutdownNow();
    }

    private static class LtmAdapter extends androidx.recyclerview.widget.ListAdapter<LongTermMemoryEntity, LtmAdapter.VH> {

        interface OnDeleteListener {
            void onDelete(LongTermMemoryEntity memory);
        }

        private final OnDeleteListener deleteListener;
        private static final SimpleDateFormat SDF = new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault());

        LtmAdapter(OnDeleteListener deleteListener) {
            super(new androidx.recyclerview.widget.DiffUtil.ItemCallback<LongTermMemoryEntity>() {
                @Override
                public boolean areItemsTheSame(@NonNull LongTermMemoryEntity o, @NonNull LongTermMemoryEntity n) {
                    return o.id == n.id;
                }

                @Override
                public boolean areContentsTheSame(@NonNull LongTermMemoryEntity o, @NonNull LongTermMemoryEntity n) {
                    return o.content.equals(n.content) && o.importance == n.importance;
                }
            });
            this.deleteListener = deleteListener;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ltm, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            LongTermMemoryEntity mem = getItem(position);
            holder.tagText.setText(getTagDisplayName(mem.tag));
            holder.importanceText.setText("重要性: " + mem.importance + "/10");
            holder.roundText.setText("第" + mem.roundNumber + "轮");
            holder.contentText.setText(mem.content);
            holder.itemView.setOnLongClickListener(v -> {
                if (deleteListener != null) deleteListener.onDelete(mem);
                return true;
            });
        }

        private String getTagDisplayName(String tag) {
            switch (tag) {
                case "plot": return "剧情";
                case "affection": return "好感";
                case "choice": return "抉择";
                case "event": return "事件";
                default: return tag;
            }
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView tagText, importanceText, roundText, contentText;
            VH(View v) {
                super(v);
                tagText = v.findViewById(R.id.tagText);
                importanceText = v.findViewById(R.id.importanceText);
                roundText = v.findViewById(R.id.roundText);
                contentText = v.findViewById(R.id.contentText);
            }
        }
    }
}
