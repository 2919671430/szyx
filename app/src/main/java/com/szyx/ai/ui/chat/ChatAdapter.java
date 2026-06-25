package com.szyx.ai.ui.chat;

import android.graphics.Color;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private static final Pattern CHOICE_PATTERN = Pattern.compile("【([^】]+?)】");

    public interface OnMessageActionListener {
        void onMessageLongClick(MessageEntity message);
    }

    public interface OnChoiceClickListener {
        void onChoiceClick(String choiceText);
    }

    private OnMessageActionListener listener;
    private OnChoiceClickListener choiceListener;
    private String streamingText = null;
    private String characterAvatarPath = null;

    public ChatAdapter() {
        super(DIFF_CALLBACK);
    }

    public void setOnMessageActionListener(OnMessageActionListener listener) {
        this.listener = listener;
    }

    public void setOnChoiceClickListener(OnChoiceClickListener listener) {
        this.choiceListener = listener;
    }

    public void setCharacterAvatarPath(String path) {
        this.characterAvatarPath = path;
    }

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
                avh.choicesContainer.setVisibility(View.GONE);
                avh.choicesContainer.removeAllViews();
            } else {
                MessageEntity msg = getItem(position);
                String content = msg.content;

                List<String> choices = extractChoices(content);
                if (!choices.isEmpty() && position == getLastAssistantPosition()) {
                    String displayText = stripChoices(content);
                    avh.messageText.setText(displayText);
                    setupChoices(avh.choicesContainer, choices);
                } else {
                    avh.messageText.setText(content);
                    avh.choicesContainer.setVisibility(View.GONE);
                    avh.choicesContainer.removeAllViews();
                }
            }
            if (characterAvatarPath != null && new File(characterAvatarPath).exists()) {
                Glide.with(avh.avatarImage)
                        .load(characterAvatarPath)
                        .placeholder(R.drawable.ic_default_avatar)
                        .error(R.drawable.ic_default_avatar)
                        .circleCrop()
                        .into(avh.avatarImage);
            } else {
                Glide.with(avh.avatarImage).clear(avh.avatarImage);
                avh.avatarImage.setImageResource(R.drawable.ic_default_avatar);
            }
            avh.avatarImage.setVisibility(View.VISIBLE);
        } else if (holder instanceof SummaryViewHolder) {
            MessageEntity msg = getItem(position);
            ((SummaryViewHolder) holder).messageText.setText(msg.content);
        }

        if (streamingText == null || position < super.getItemCount()) {
            holder.itemView.setOnLongClickListener(v -> {
                if (listener != null && position < super.getItemCount()) {
                    listener.onMessageLongClick(getItem(position));
                }
                return true;
            });
        }
    }

    private int getLastAssistantPosition() {
        for (int i = super.getItemCount() - 1; i >= 0; i--) {
            if ("assistant".equals(getItem(i).role)) return i;
        }
        return -1;
    }

    private List<String> extractChoices(String text) {
        List<String> choices = new ArrayList<>();
        Matcher matcher = CHOICE_PATTERN.matcher(text);
        while (matcher.find()) {
            choices.add(matcher.group(1).trim());
        }
        return choices;
    }

    private String stripChoices(String text) {
        return text.replaceAll("【[^】]+?】", "").trim();
    }

    private void setupChoices(LinearLayout container, List<String> choices) {
        container.removeAllViews();
        container.setVisibility(View.VISIBLE);

        for (String choice : choices) {
            TextView chip = new TextView(container.getContext());
            chip.setText(choice);
            chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            chip.setTextColor(Color.parseColor("#FF00E5FF"));
            chip.setBackgroundResource(R.drawable.bg_choice_chip);
            chip.setGravity(Gravity.CENTER);
            int hPad = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12,
                    container.getContext().getResources().getDisplayMetrics());
            int vPad = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8,
                    container.getContext().getResources().getDisplayMetrics());
            chip.setPadding(hPad, vPad, hPad, vPad);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3,
                    container.getContext().getResources().getDisplayMetrics());
            lp.setMargins(0, margin, 0, margin);
            chip.setLayoutParams(lp);

            chip.setOnClickListener(v -> {
                if (choiceListener != null) {
                    choiceListener.onChoiceClick(choice);
                }
            });

            container.addView(chip);
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
        LinearLayout choicesContainer;
        AssistantViewHolder(View v) {
            super(v);
            messageText = v.findViewById(R.id.messageText);
            avatarImage = v.findViewById(R.id.avatarImage);
            choicesContainer = v.findViewById(R.id.choicesContainer);
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
