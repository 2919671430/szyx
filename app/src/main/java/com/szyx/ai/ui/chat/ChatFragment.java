package com.szyx.ai.ui.chat;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.szyx.ai.R;
import com.szyx.ai.data.db.entity.MessageEntity;
import com.szyx.ai.data.db.entity.NumericalValueEntity;
import com.szyx.ai.viewmodel.ChatViewModel;

import java.io.File;
import java.util.List;

public class ChatFragment extends Fragment {

    private ChatViewModel viewModel;
    private ChatAdapter adapter;
    private EditText messageInput;
    private ImageView btnSend;
    private LinearLayout typingIndicator;
    private RecyclerView recyclerView;
    private TextView textModelIndicator;
    private TextView textNumericalValues;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        long sessionId = getArguments() != null ? getArguments().getLong("sessionId") : 0;
        if (sessionId <= 0) {
            Toast.makeText(requireContext(), "无效的会话", Toast.LENGTH_SHORT).show();
            return;
        }

        // Initialize views
        recyclerView = view.findViewById(R.id.messagesRecyclerView);
        messageInput = view.findViewById(R.id.messageInput);
        btnSend = view.findViewById(R.id.btnSend);
        typingIndicator = view.findViewById(R.id.typingIndicator);
        TextView characterNameText = view.findViewById(R.id.characterNameText);
        ImageView avatarImageView = view.findViewById(R.id.avatarImageView);
        textModelIndicator = view.findViewById(R.id.textModelIndicator);
        textNumericalValues = view.findViewById(R.id.textNumericalValues);

        // Setup RecyclerView
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);

        adapter = new ChatAdapter();
        adapter.setOnChoiceClickListener(choiceText -> {
            messageInput.setText("");
            adapter.clearStreamingText();
            viewModel.sendMessage(choiceText);
        });
        adapter.setOnMessageActionListener(new ChatAdapter.OnMessageActionListener() {
            @Override
            public void onMessageLongClick(MessageEntity message) {
                String[] actions;
                if ("user".equals(message.role)) {
                    actions = new String[]{"复制", "编辑并重发", "删除"};
                } else if ("assistant".equals(message.role)) {
                    actions = new String[]{"复制", "重新生成", "删除"};
                } else {
                    actions = new String[]{"复制"};
                }

                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("消息操作")
                        .setItems(actions, (dialog, which) -> {
                            switch (which) {
                                case 0: // 复制
                                    android.content.ClipboardManager clipboard =
                                            (android.content.ClipboardManager) requireContext()
                                                    .getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("message", message.content));
                                    Toast.makeText(requireContext(), "已复制到剪贴板", Toast.LENGTH_SHORT).show();
                                    break;
                                case 1: // 编辑并重发 / 重新生成
                                    if ("user".equals(message.role)) {
                                        messageInput.setText(message.content);
                                        messageInput.setSelection(message.content.length());
                                        viewModel.deleteMessage(message);
                                    } else {
                                        viewModel.deleteMessage(message);
                                        viewModel.regenerateLastResponse();
                                    }
                                    break;
                                case 2: // 删除
                                    viewModel.deleteMessage(message);
                                    Toast.makeText(requireContext(), "消息已删除", Toast.LENGTH_SHORT).show();
                                    break;
                            }
                        })
                        .show();
            }
        });
        recyclerView.setAdapter(adapter);

        // ViewModel
        viewModel = new ViewModelProvider(this).get(ChatViewModel.class);
        viewModel.setSession(sessionId);

        // Observe character info
        viewModel.getCharacterName().observe(getViewLifecycleOwner(), name -> {
            if (name != null && !name.isEmpty()) {
                characterNameText.setText(name);
            }
        });
        viewModel.getCharacterAvatarPath().observe(getViewLifecycleOwner(), avatarPath -> {
            if (avatarPath != null && new File(avatarPath).exists()) {
                Glide.with(avatarImageView).load(avatarPath).circleCrop().into(avatarImageView);
            }
            adapter.setCharacterAvatarPath(avatarPath);
        });

        // Observe messages
        viewModel.getMessages().observe(getViewLifecycleOwner(), messages -> {
            if (messages != null) {
                adapter.submitList(messages);
                if (!messages.isEmpty()) {
                    recyclerView.scrollToPosition(messages.size() - 1);
                }
            }
        });

        // Observe generation state
        viewModel.getIsGenerating().observe(getViewLifecycleOwner(), generating -> {
            if (generating) {
                typingIndicator.setVisibility(View.VISIBLE);
            } else {
                typingIndicator.setVisibility(View.GONE);
            }
            btnSend.setEnabled(!generating);
        });

        // Observe streaming text
        viewModel.getStreamingText().observe(getViewLifecycleOwner(), text -> {
            if (text != null && !text.isEmpty()) {
                adapter.showStreamingText(text);
                recyclerView.scrollToPosition(adapter.getItemCount() - 1);
            } else {
                adapter.clearStreamingText();
            }
        });

        // Observe errors
        viewModel.getErrorText().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
            }
        });

        // Observe numerical values
        viewModel.getNumericalValues().observe(getViewLifecycleOwner(), values -> {
            if (values != null && !values.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (NumericalValueEntity v : values) {
                    if (sb.length() > 0) sb.append(" | ");
                    sb.append(v.name).append(": ").append(v.currentValue);
                    if (v.maxValue > 0) sb.append("/").append(v.maxValue);
                }
                textNumericalValues.setText(sb.toString());
                textNumericalValues.setVisibility(View.VISIBLE);
            } else {
                textNumericalValues.setVisibility(View.GONE);
            }
        });

        // Click numerical bar to show change history
        textNumericalValues.setOnClickListener(v -> {
            List<String> history = viewModel.getNumericalHistory().getValue();
            if (history != null && !history.isEmpty()) {
                StringBuilder msg = new StringBuilder("最近一次数值变化：\n");
                for (String h : history) {
                    msg.append("• ").append(h).append("\n");
                }
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("数值变化记录")
                        .setMessage(msg.toString())
                        .setPositiveButton("确定", null)
                        .show();
            } else {
                Toast.makeText(requireContext(), "暂无数值变化记录", Toast.LENGTH_SHORT).show();
            }
        });

        // Model switch on indicator click
        textModelIndicator.setOnClickListener(v -> showModelSwitchDialog());

        // Observe current model name
        viewModel.getCurrentModelName().observe(getViewLifecycleOwner(), name -> {
            if (name != null) {
                textModelIndicator.setText(name);
            }
        });

        // Send button
        btnSend.setOnClickListener(v -> sendMessage());
    }

    private void showModelSwitchDialog() {
        String[] modelCodes = {"xiaomi", "deepseek", "custom"};
        String[] modelNames = {"小米 mimo-v2.5", "DeepSeek V4 Flash", "自定义模型"};

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("切换模型")
                .setItems(modelNames, (dialog, which) -> {
                    viewModel.switchModel(modelCodes[which]);
                    Toast.makeText(requireContext(), "已切换到: " + modelNames[which], Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void sendMessage() {
        String text = messageInput.getText().toString().trim();
        if (text.isEmpty()) return;

        messageInput.setText("");
        adapter.clearStreamingText();
        viewModel.sendMessage(text);
    }
}
