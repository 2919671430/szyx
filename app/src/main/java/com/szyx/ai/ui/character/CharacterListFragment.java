package com.szyx.ai.ui.character;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.szyx.ai.data.db.entity.CharacterEntity;
import com.szyx.ai.ui.MainActivity;
import com.szyx.ai.ui.session.SessionListFragment;
import com.szyx.ai.viewmodel.CharacterViewModel;

public class CharacterListFragment extends Fragment {

    private CharacterViewModel viewModel;
    private CharacterAdapter adapter;
    private TextView emptyText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_character_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        emptyText = view.findViewById(R.id.emptyText);
        RecyclerView recyclerView = view.findViewById(R.id.characterRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new CharacterAdapter(
            character -> {
                // 进入会话列表
                SessionListFragment fragment = new SessionListFragment();
                Bundle args = new Bundle();
                args.putLong("characterId", character.id);
                fragment.setArguments(args);
                ((MainActivity) requireActivity()).navigateTo(fragment);
            },
            (character, anchorView) -> {
                PopupMenu popup = new PopupMenu(requireContext(), anchorView);
                popup.getMenu().add("编辑");
                popup.getMenu().add("删除");
                popup.setOnMenuItemClickListener(item -> {
                    if ("编辑".equals(item.getTitle().toString())) {
                        CharacterEditFragment fragment = new CharacterEditFragment();
                        Bundle args = new Bundle();
                        args.putLong("characterId", character.id);
                        fragment.setArguments(args);
                        ((MainActivity) requireActivity()).navigateTo(fragment);
                        return true;
                    } else if ("删除".equals(item.getTitle().toString())) {
                        new MaterialAlertDialogBuilder(requireContext())
                                .setTitle("删除角色")
                                .setMessage(R.string.delete_character_confirm)
                                .setPositiveButton(R.string.delete, (d, w) -> viewModel.delete(character))
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

        FloatingActionButton fab = view.findViewById(R.id.fabAdd);
        fab.setOnClickListener(v -> {
            ((MainActivity) requireActivity()).navigateTo(new CharacterEditFragment());
        });

        // 设置按钮
        View btnSettings = view.findViewById(R.id.btnSettings);
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> {
                ((MainActivity) requireActivity()).navigateTo(new com.szyx.ai.ui.settings.SettingsFragment());
            });
        }

        // 日志按钮
        View btnLog = view.findViewById(R.id.btnLog);
        if (btnLog != null) {
            btnLog.setOnClickListener(v -> {
                ((MainActivity) requireActivity()).navigateTo(new com.szyx.ai.ui.log.LogViewerFragment());
            });
        }

        // 使用手册按钮
        View btnHelp = view.findViewById(R.id.btnHelp);
        if (btnHelp != null) {
            btnHelp.setOnClickListener(v -> {
                ((MainActivity) requireActivity()).navigateTo(new com.szyx.ai.ui.help.HelpFragment());
            });
        }

        viewModel = new ViewModelProvider(this).get(CharacterViewModel.class);
        viewModel.getAllCharacters().observe(getViewLifecycleOwner(), characters -> {
            adapter.submitList(characters);
            emptyText.setVisibility(characters == null || characters.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }
}
