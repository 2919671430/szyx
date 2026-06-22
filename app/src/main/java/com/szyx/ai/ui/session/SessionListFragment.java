package com.szyx.ai.ui.session;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.szyx.ai.R;
import com.szyx.ai.data.db.entity.ChatSessionEntity;
import com.szyx.ai.ui.MainActivity;
import com.szyx.ai.ui.chat.ChatFragment;
import com.szyx.ai.viewmodel.SessionViewModel;

public class SessionListFragment extends Fragment {

    private SessionViewModel viewModel;
    private SessionAdapter adapter;
    private long characterId;
    private TextView emptyText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_session_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        characterId = getArguments() != null ? getArguments().getLong("characterId") : 0;

        emptyText = view.findViewById(R.id.emptyText);
        RecyclerView recyclerView = view.findViewById(R.id.sessionRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new SessionAdapter(
            session -> {
                ChatFragment fragment = new ChatFragment();
                Bundle args = new Bundle();
                args.putLong("sessionId", session.id);
                fragment.setArguments(args);
                ((MainActivity) requireActivity()).navigateTo(fragment);
            },
            (session, anchorView) -> {
                PopupMenu popup = new PopupMenu(requireContext(), anchorView);
                popup.getMenu().add("重命名");
                popup.getMenu().add("复制");
                popup.getMenu().add("删除");
                popup.setOnMenuItemClickListener(item -> {
                    String title = item.getTitle().toString();
                    switch (title) {
                        case "重命名":
                            showRenameDialog(session);
                            return true;
                        case "复制":
                            viewModel.duplicateSession(session.id, session.sessionName + " (copy)");
                            return true;
                        case "删除":
                            new MaterialAlertDialogBuilder(requireContext())
                                    .setTitle("删除存档")
                                    .setMessage(R.string.delete_session_confirm)
                                    .setPositiveButton(R.string.delete, (d, w) -> viewModel.deleteSession(session))
                                    .setNegativeButton(R.string.cancel, null)
                                    .show();
                            return true;
                    }
                    return false;
                });
                popup.show();
            }
        );
        recyclerView.setAdapter(adapter);

        FloatingActionButton fab = view.findViewById(R.id.fabNewSession);
        fab.setOnClickListener(v -> showNewSessionDialog());

        viewModel = new ViewModelProvider(this).get(SessionViewModel.class);
        viewModel.getSessions(characterId).observe(getViewLifecycleOwner(), sessions -> {
            adapter.submitList(sessions);
            emptyText.setVisibility(sessions == null || sessions.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    private void showNewSessionDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_new_session, null);
        EditText nameInput = dialogView.findViewById(R.id.editSessionName);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("新建存档")
                .setView(dialogView)
                .setPositiveButton("创建", (dialog, which) -> {
                    String name = nameInput.getText().toString().trim();
                    if (name.isEmpty()) name = "Session " + System.currentTimeMillis();
                    viewModel.createSession(characterId, name);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showRenameDialog(ChatSessionEntity session) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_new_session, null);
        EditText nameInput = dialogView.findViewById(R.id.editSessionName);
        nameInput.setText(session.sessionName);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("重命名存档")
                .setView(dialogView)
                .setPositiveButton("重命名", (dialog, which) -> {
                    String name = nameInput.getText().toString().trim();
                    if (!name.isEmpty()) {
                        session.sessionName = name;
                        viewModel.update(session);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
