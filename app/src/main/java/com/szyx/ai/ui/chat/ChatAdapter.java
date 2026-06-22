package com.szyx.ai.ui.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.szyx.ai.R;
import com.szyx.ai.data.db.entity.MessageEntity;

public class ChatAdapter extends ListAdapter<MessageEntity, RecyclerView.ViewHolder> {

    private static final int TYPE_USER = 0;
    private static final int TYPE_ASSISTANT = 1;
    private static final int TYPE_SUMMARY = 2;
    private static final int TYPE_STREAMING = 3;

    public interface OnMessageActionListener {
        void onMessageLongClick(MessageEntity message);
    }

    private OnMessageActionListener listener;
    private String streamingText = null;
    private String characterAvatarPath = null;

    public ChatAdapter() {
        super(DIFF_CALLBACK);
    }

    public void setOnMessageActionListener(OnMessageActionListener listener) {
        this.listener = listener;
    }

    public void setCharacterAvatarPath(String path) {
        this.characterAvatarPath = path;
    }

    /**
     * Show streaming text as a temporary assistant message at the end of the list.
     */
    public void showStreamingText(String text) {
        int oldCount = getItemCount();
        this.streamingText = text;
        int newCount = getItemCount();
        if (oldCount == newCount) {
            notifyItemChanged(newCount - 1, "streaming");
        } else {
            notifyItemInserted(newCount - 1);
        }
    }

    /**
     * Clear streaming text when generation completes.
     */
    public void clearStreamingText() {
        int oldCount = getItemCount();
        this.streamingText = null;
        int newCount = getItemCount();
        if (oldCount > newCount) {
            notifyItemRemoved(oldCount - 1);
        }
    }

    @Override
    public int getItemCount() {
        int base = super.getItemCount();
        return streamingText != null ? base + 1 : base;
    }

    @Override
    public int getItemViewType(int position) {
        if (streamingText != null && position == super.getItemCount()) {
            return TYPE_STREAMING;
        }
        MessageEntity msg = getItem(position);
        if ("summary".equals(msg.role)) return TYPE_SUMMARY;
        if ("user".equals(msg.role)) return TYPE_USER;
        return TYPE_ASSISTANT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case TYPE_USER:
                return new UserViewHolder(inflater.inflate(R.layout.item_message_user, parent, false));
            case TYPE_SUMMARY:
                return new SummaryViewHolder(inflater.inflate(R.layout.item_message_summary, parent, false));
            case TYPE_STREAMING:
            case TYPE_ASSISTANT:
            default:
                return new AssistantViewHolder(inflater.inflate(R.layout.item_message_assistant, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position,
                                 @NonNull List<Object> payloads) {
        if (!payloads.isEmpty() && streamingText != null
                && holder instanceof AssistantViewHolder
                && position == super.getItemCount()) {
            ((AssistantViewHolder) holder).messageText.setText(streamingText);
            return;
        }
        onBindViewHolder(holder, position);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof UserViewHolder) {
            MessageEntity msg = getItem(position);
            ((UserViewHolder) holder).messageText.setText(msg.content);
        } else if (holder instanceof AssistantViewHolder) {
            AssistantViewHolder avh = (AssistantViewHolder) holder;
            if (streamingText != null && position == super.getItemCount()) {
                avh.messageText.setText(streamingText);
            } else {
                MessageEntity msg = getItem(position);
                avh.messageText.setText(msg.content);
            }
            // Load character avatar
            if (characterAvatarPath != null && new File(characterAvatarPath).exists()) {
                Glide.with(avh.avatarImage)
                        .load(characterAvatarPath)
                        .circleCrop()
                        .into(avh.avatarImage);
                avh.avatarImage.setVisibility(View.VISIBLE);
            } else {
                avh.avatarImage.setVisibility(View.VISIBLE);
            }
        } else if (holder instanceof SummaryViewHolder) {
            MessageEntity msg = getItem(position);
            ((SummaryViewHolder) holder).messageText.setText(msg.content);
        }

        // Long click for non-streaming items
        if (streamingText == null || position < super.getItemCount()) {
            holder.itemView.setOnLongClickListener(v -> {
                if (listener != null && position < super.getItemCount()) {
                    listener.onMessageLongClick(getItem(position));
                }
                return true;
            });
        }
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        UserViewHolder(View v) {
            super(v);
            messageText = v.findViewById(R.id.messageText);
        }
    }

    static class AssistantViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        ImageView avatarImage;
        AssistantViewHolder(View v) {
            super(v);
            messageText = v.findViewById(R.id.messageText);
            avatarImage = v.findViewById(R.id.avatarImage);
        }
    }

    static class SummaryViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        SummaryViewHolder(View v) {
            super(v);
            messageText = v.findViewById(R.id.messageText);
        }
    }

    private static final DiffUtil.ItemCallback<MessageEntity> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<MessageEntity>() {
                @Override
                public boolean areItemsTheSame(@NonNull MessageEntity old, @NonNull MessageEntity n) {
                    return old.id == n.id;
                }

                @Override
                public boolean areContentsTheSame(@NonNull MessageEntity old, @NonNull MessageEntity n) {
                    return old.content.equals(n.content) &&
                            old.isHidden == n.isHidden &&
                            old.role.equals(n.role);
                }
            };
}
