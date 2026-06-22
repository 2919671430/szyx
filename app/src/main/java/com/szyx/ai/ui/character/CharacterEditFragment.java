package com.szyx.ai.ui.character;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.szyx.ai.R;
import com.szyx.ai.data.db.entity.CharacterEntity;
import com.szyx.ai.data.db.entity.WorldBookEntity;
import com.szyx.ai.data.repository.ChatRepository;
import com.szyx.ai.util.JsonUtils;
import com.szyx.ai.util.TemplateManager;
import com.szyx.ai.viewmodel.CharacterViewModel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CharacterEditFragment extends Fragment {

    private CharacterViewModel viewModel;
    private CharacterEntity editingCharacter;
    private long characterId = -1;

    private EditText editName, editPersonality, editSystemPrompt, editDescription,
            editFirstMessage, editTags, editWorldSetting, editOutputStyle,
            editSupremeDirective;
    private Slider sliderConstraint, sliderSTMRounds;
    private TextView textConstraint, textSTMRounds;
    private SwitchMaterial switchLTM, switchWorldBook, switchNumerical;

    private EditText editWorldBookInput, editWorldBookPriority;
    private TextView textWorldBookCount;
    private ChatRepository chatRepo;
    private ExecutorService executor;
    private ImageView avatarPreview;
    private String avatarPath;

    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        saveAvatarFromUri(uri);
                    }
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_character_edit, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        editName = view.findViewById(R.id.editName);
        avatarPreview = view.findViewById(R.id.avatarPreview);
        editPersonality = view.findViewById(R.id.editPersonality);
        editSystemPrompt = view.findViewById(R.id.editSystemPrompt);
        editDescription = view.findViewById(R.id.editDescription);
        editFirstMessage = view.findViewById(R.id.editFirstMessage);
        editTags = view.findViewById(R.id.editTags);
        editWorldSetting = view.findViewById(R.id.editWorldSetting);
        editOutputStyle = view.findViewById(R.id.editOutputStyle);
        editSupremeDirective = view.findViewById(R.id.editSupremeDirective);

        sliderConstraint = view.findViewById(R.id.sliderConstraint);
        textConstraint = view.findViewById(R.id.textConstraint);
        sliderSTMRounds = view.findViewById(R.id.sliderSTMRounds);
        textSTMRounds = view.findViewById(R.id.textSTMRounds);

        switchLTM = view.findViewById(R.id.switchLTM);
        switchWorldBook = view.findViewById(R.id.switchWorldBook);
        switchNumerical = view.findViewById(R.id.switchNumerical);

        // World book management
        chatRepo = new ChatRepository(requireActivity().getApplication());
        executor = Executors.newSingleThreadExecutor();
        editWorldBookInput = view.findViewById(R.id.editWorldBookInput);
        editWorldBookPriority = view.findViewById(R.id.editWorldBookPriority);
        textWorldBookCount = view.findViewById(R.id.textWorldBookCount);
        View btnAddWorldBook = view.findViewById(R.id.btnAddWorldBook);
        View btnManageWorldBook = view.findViewById(R.id.btnManageWorldBook);

        btnAddWorldBook.setOnClickListener(v -> addWorldBookEntry());
        btnManageWorldBook.setOnClickListener(v -> showWorldBookList());

        View btnExportWorldBook = view.findViewById(R.id.btnExportWorldBook);
        View btnImportWorldBook = view.findViewById(R.id.btnImportWorldBook);
        btnExportWorldBook.setOnClickListener(v -> exportWorldBook());
        btnImportWorldBook.setOnClickListener(v -> importWorldBook());

        if (characterId > 0) {
            loadWorldBookCount();
        }

        // Constraint level labels
        String[] constraintLabels = {"宽松", "标准", "严格"};
        sliderConstraint.addOnChangeListener((slider, value, fromUser) ->
                textConstraint.setText(constraintLabels[(int) value]));

        sliderSTMRounds.addOnChangeListener((slider, value, fromUser) ->
                textSTMRounds.setText((int) value + " 轮"));

        viewModel = new ViewModelProvider(this).get(CharacterViewModel.class);

        if (getArguments() != null) {
            characterId = getArguments().getLong("characterId", -1);
        }

        if (characterId > 0) {
            viewModel.getCharacterById(characterId).observe(getViewLifecycleOwner(), character -> {
                if (character != null && editingCharacter == null) {
                    editingCharacter = character;
                    populateFields(character);
                }
            });
        }

        Button btnSave = view.findViewById(R.id.btnSave);
        btnSave.setOnClickListener(v -> saveCharacter());

        Button btnImport = view.findViewById(R.id.btnImport);
        btnImport.setOnClickListener(v -> importCharacter());

        Button btnExport = view.findViewById(R.id.btnExport);
        btnExport.setOnClickListener(v -> exportCharacter());

        Button btnTemplate = view.findViewById(R.id.btnTemplate);
        btnTemplate.setOnClickListener(v -> showTemplateDialog());

        // Avatar picker
        View btnPickAvatar = view.findViewById(R.id.btnPickAvatar);
        btnPickAvatar.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    private void populateFields(CharacterEntity c) {
        editName.setText(c.name);
        editPersonality.setText(c.personality);
        editSystemPrompt.setText(c.systemPrompt);
        editDescription.setText(c.description);
        editFirstMessage.setText(c.firstMessage);
        editTags.setText(c.tags);
        editWorldSetting.setText(c.worldSetting);
        editOutputStyle.setText(c.outputStyle);
        editSupremeDirective.setText(c.supremeDirective);

        if (c.avatarPath != null && !c.avatarPath.isEmpty()) {
            avatarPath = c.avatarPath;
            Glide.with(avatarPreview).load(new File(c.avatarPath)).circleCrop().into(avatarPreview);
        }

        sliderConstraint.setValue(c.constraintLevel);
        String[] constraintLabels = {"宽松", "标准", "严格"};
        textConstraint.setText(constraintLabels[c.constraintLevel]);

        sliderSTMRounds.setValue(c.stmRounds > 0 ? c.stmRounds : 20);
        textSTMRounds.setText((c.stmRounds > 0 ? c.stmRounds : 20) + " 轮");

        switchLTM.setChecked(c.ltmEnabled);
        switchWorldBook.setChecked(c.worldBookEnabled);
        switchNumerical.setChecked(c.numericalEnabled);
    }

    private void saveCharacter() {
        String name = editName.getText().toString().trim();
        String systemPrompt = editSystemPrompt.getText().toString().trim();

        // All fields are optional except name (free text mode)
        if (name.isEmpty()) {
            editName.setError("请输入角色名称");
            return;
        }

        CharacterEntity character = editingCharacter != null ? editingCharacter : new CharacterEntity();
        character.name = name;
        character.personality = editPersonality.getText().toString().trim();
        character.systemPrompt = systemPrompt;
        character.description = editDescription.getText().toString().trim();
        character.firstMessage = editFirstMessage.getText().toString().trim();
        character.tags = editTags.getText().toString().trim();
        character.worldSetting = editWorldSetting.getText().toString().trim();
        character.outputStyle = editOutputStyle.getText().toString().trim();
        character.supremeDirective = editSupremeDirective.getText().toString().trim();
        character.constraintLevel = (int) sliderConstraint.getValue();
        character.stmRounds = (int) sliderSTMRounds.getValue();
        character.ltmEnabled = switchLTM.isChecked();
        character.worldBookEnabled = switchWorldBook.isChecked();
        character.numericalEnabled = switchNumerical.isChecked();
        character.updatedAt = System.currentTimeMillis();
        if (avatarPath != null) {
            character.avatarPath = avatarPath;
        }

        if (editingCharacter == null) {
            character.createdAt = System.currentTimeMillis();
            viewModel.insert(character);
        } else {
            viewModel.update(character);
        }

        Toast.makeText(requireContext(), "角色已保存", Toast.LENGTH_SHORT).show();
        getParentFragmentManager().popBackStack();
    }

    private void showTemplateDialog() {
        java.util.List<TemplateManager.Template> templates = TemplateManager.getTemplates();
        String[] names = new String[templates.size()];
        for (int i = 0; i < templates.size(); i++) {
            names[i] = templates.get(i).name + " - " + templates.get(i).description;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("选择模板")
                .setItems(names, (dialog, which) -> {
                    TemplateManager.Template template = templates.get(which);
                    editName.setText(template.name);
                    editSystemPrompt.setText(template.systemPrompt);
                    editPersonality.setText(template.personality);
                    editWorldSetting.setText(template.worldSetting);
                    editOutputStyle.setText(template.outputStyle);
                    editDescription.setText(template.description);
                    Toast.makeText(requireContext(), "已套用模板: " + template.name, Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void importCharacter() {
        android.content.ClipboardManager clipboard =
                (android.content.ClipboardManager) requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
        if (clipboard.hasPrimaryClip() && clipboard.getPrimaryClip() != null) {
            String json = clipboard.getPrimaryClip().getItemAt(0).getText().toString();
            try {
                CharacterEntity imported = JsonUtils.importCharacter(json);
                editName.setText(imported.name);
                editPersonality.setText(imported.personality);
                editSystemPrompt.setText(imported.systemPrompt);
                editDescription.setText(imported.description);
                editFirstMessage.setText(imported.firstMessage);
                editTags.setText(imported.tags);
                if (imported.worldSetting != null) editWorldSetting.setText(imported.worldSetting);
                if (imported.outputStyle != null) editOutputStyle.setText(imported.outputStyle);
                Toast.makeText(requireContext(), "角色已导入", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(requireContext(), "导入失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void exportCharacter() {
        String name = editName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "请先填写角色名称", Toast.LENGTH_SHORT).show();
            return;
        }

        CharacterEntity temp = new CharacterEntity();
        temp.name = name;
        temp.personality = editPersonality.getText().toString().trim();
        temp.systemPrompt = editSystemPrompt.getText().toString().trim();
        temp.description = editDescription.getText().toString().trim();
        temp.firstMessage = editFirstMessage.getText().toString().trim();
        temp.tags = editTags.getText().toString().trim();
        temp.worldSetting = editWorldSetting.getText().toString().trim();
        temp.outputStyle = editOutputStyle.getText().toString().trim();
        temp.createdAt = System.currentTimeMillis();

        String json = JsonUtils.exportCharacter(temp);
        if (json != null) {
            android.content.ClipboardManager clipboard =
                    (android.content.ClipboardManager) requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("character", json));
            Toast.makeText(requireContext(), "JSON 已复制到剪贴板", Toast.LENGTH_SHORT).show();
        }
    }

    private void addWorldBookEntry() {
        if (characterId <= 0) {
            Toast.makeText(requireContext(), "请先保存角色后再添加世界书条目", Toast.LENGTH_SHORT).show();
            return;
        }

        String input = editWorldBookInput.getText().toString().trim();
        if (input.isEmpty() || !input.contains("|")) {
            Toast.makeText(requireContext(), "请输入格式：关键词1,关键词2 | 内容", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] parts = input.split("\\|", 2);
        String keywords = parts[0].trim();
        String content = parts[1].trim();
        int priority = 5;
        try {
            priority = Integer.parseInt(editWorldBookPriority.getText().toString().trim());
            priority = Math.max(1, Math.min(10, priority));
        } catch (NumberFormatException ignored) {}

        final int finalPriority = priority;
        executor.execute(() -> {
            WorldBookEntity entry = new WorldBookEntity();
            entry.characterId = characterId;
            entry.keywords = keywords;
            entry.content = content;
            entry.priority = finalPriority;
            entry.enabled = true;
            entry.createdAt = System.currentTimeMillis();
            chatRepo.insertWorldBookEntry(entry);

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    editWorldBookInput.setText("");
                    editWorldBookPriority.setText("5");
                    loadWorldBookCount();
                    Toast.makeText(requireContext(), "世界书条目已添加", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void saveAvatarFromUri(Uri uri) {
        executor.execute(() -> {
            try {
                File avatarDir = new File(requireContext().getFilesDir(), "avatars");
                if (!avatarDir.exists()) avatarDir.mkdirs();
                File avatarFile = new File(avatarDir, "avatar_" + System.currentTimeMillis() + ".jpg");

                InputStream is = requireContext().getContentResolver().openInputStream(uri);
                FileOutputStream fos = new FileOutputStream(avatarFile);
                byte[] buf = new byte[4096];
                int len;
                while ((len = is.read(buf)) > 0) {
                    fos.write(buf, 0, len);
                }
                fos.close();
                is.close();

                avatarPath = avatarFile.getAbsolutePath();
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Glide.with(avatarPreview).load(avatarFile).circleCrop().into(avatarPreview);
                        Toast.makeText(requireContext(), "头像已设置", Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), "头像设置失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void loadWorldBookCount() {
        if (characterId <= 0) return;
        executor.execute(() -> {
            List<WorldBookEntity> entries = chatRepo.getAllWorldBookEntries(characterId);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    textWorldBookCount.setText("已有条目：" + entries.size());
                });
            }
        });
    }

    private void showWorldBookList() {
        if (characterId <= 0) {
            Toast.makeText(requireContext(), "请先保存角色", Toast.LENGTH_SHORT).show();
            return;
        }

        executor.execute(() -> {
            List<WorldBookEntity> entries = chatRepo.getAllWorldBookEntries(characterId);
            if (getActivity() == null) return;

            getActivity().runOnUiThread(() -> {
                if (entries.isEmpty()) {
                    Toast.makeText(requireContext(), "暂无世界书条目", Toast.LENGTH_SHORT).show();
                    return;
                }

                String[] items = new String[entries.size()];
                for (int i = 0; i < entries.size(); i++) {
                    WorldBookEntity e = entries.get(i);
                    String preview = e.content.length() > 30 ? e.content.substring(0, 30) + "..." : e.content;
                    items[i] = "[" + e.priority + "] " + e.keywords + " → " + preview;
                }

                new AlertDialog.Builder(requireContext())
                        .setTitle("世界书条目（点击删除）")
                        .setItems(items, (dialog, which) -> {
                            WorldBookEntity selected = entries.get(which);
                            new AlertDialog.Builder(requireContext())
                                    .setTitle("删除条目？")
                                    .setMessage("关键词：" + selected.keywords + "\n内容：" + selected.content)
                                    .setPositiveButton("删除", (d, w) -> {
                                        executor.execute(() -> {
                                            chatRepo.deleteWorldBookEntry(selected);
                                            if (getActivity() != null) {
                                                getActivity().runOnUiThread(this::loadWorldBookCount);
                                            }
                                        });
                                    })
                                    .setNegativeButton("取消", null)
                                    .show();
                        })
                        .show();
            });
        });
    }

    private void exportWorldBook() {
        if (characterId <= 0) {
            Toast.makeText(requireContext(), "请先保存角色", Toast.LENGTH_SHORT).show();
            return;
        }
        executor.execute(() -> {
            List<WorldBookEntity> entries = chatRepo.getAllWorldBookEntries(characterId);
            if (entries.isEmpty()) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), "暂无世界书条目", Toast.LENGTH_SHORT).show());
                }
                return;
            }
            try {
                org.json.JSONArray arr = new org.json.JSONArray();
                for (WorldBookEntity e : entries) {
                    org.json.JSONObject obj = new org.json.JSONObject();
                    obj.put("keywords", e.keywords);
                    obj.put("content", e.content);
                    obj.put("priority", e.priority);
                    arr.put(obj);
                }
                String json = arr.toString(2);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        android.content.ClipboardManager clipboard =
                                (android.content.ClipboardManager) requireContext()
                                        .getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("worldbook", json));
                        Toast.makeText(requireContext(), "世界书已导出到剪贴板（" + entries.size() + "条）", Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), "导出失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void importWorldBook() {
        if (characterId <= 0) {
            Toast.makeText(requireContext(), "请先保存角色", Toast.LENGTH_SHORT).show();
            return;
        }
        android.content.ClipboardManager clipboard =
                (android.content.ClipboardManager) requireContext()
                        .getSystemService(android.content.Context.CLIPBOARD_SERVICE);
        if (!clipboard.hasPrimaryClip() || clipboard.getPrimaryClip() == null) {
            Toast.makeText(requireContext(), "剪贴板为空", Toast.LENGTH_SHORT).show();
            return;
        }
        String json = clipboard.getPrimaryClip().getItemAt(0).getText().toString();
        executor.execute(() -> {
            try {
                org.json.JSONArray arr = new org.json.JSONArray(json);
                int count = 0;
                for (int i = 0; i < arr.length(); i++) {
                    org.json.JSONObject obj = arr.getJSONObject(i);
                    WorldBookEntity entry = new WorldBookEntity();
                    entry.characterId = characterId;
                    entry.keywords = obj.optString("keywords", "");
                    entry.content = obj.optString("content", "");
                    entry.priority = obj.optInt("priority", 5);
                    entry.enabled = true;
                    entry.createdAt = System.currentTimeMillis();
                    if (!entry.keywords.isEmpty() && !entry.content.isEmpty()) {
                        chatRepo.insertWorldBookEntry(entry);
                        count++;
                    }
                }
                final int imported = count;
                final List<WorldBookEntity> entries = chatRepo.getAllWorldBookEntries(characterId);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        textWorldBookCount.setText("已有条目：" + entries.size());
                        Toast.makeText(requireContext(), "已导入 " + imported + " 条世界书条目", Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), "导入失败：格式不正确", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }
}
