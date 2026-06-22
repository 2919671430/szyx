package com.szyx.ai.ui.session;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.szyx.ai.R;
import com.szyx.ai.data.db.entity.ChatSessionEntity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SessionAdapter extends ListAdapter<ChatSessionEntity, SessionAdapter.ViewHolder> {

    public interface OnSessionClickListener {
        void onClick(ChatSessionEntity session);
    }

    public interface OnSessionLongClickListener {
        void onLongClick(ChatSessionEntity session, View anchorView);
    }

    private final OnSessionClickListener clickListener;
    private final OnSessionLongClickListener longClickListener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    public SessionAdapter(OnSessionClickListener clickListener,
                          OnSessionLongClickListener longClickListener) {
        super(DIFF_CALLBACK);
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_session, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatSessionEntity session = getItem(position);
        holder.sessionNameText.setText(session.sessionName);
        holder.messageCountText.setText(session.messageCount + " messages");
        holder.lastMessageTimeText.setText(dateFormat.format(new Date(session.lastMessageAt)));

        holder.itemView.setOnClickListener(v -> clickListener.onClick(session));
        holder.itemView.setOnLongClickListener(v -> {
            longClickListener.onLongClick(session, v);
            return true;
        });
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView sessionNameText;
        TextView messageCountText;
        TextView lastMessageTimeText;

        ViewHolder(View itemView) {
            super(itemView);
            sessionNameText = itemView.findViewById(R.id.sessionNameText);
            messageCountText = itemView.findViewById(R.id.messageCountText);
            lastMessageTimeText = itemView.findViewById(R.id.lastMessageTimeText);
        }
    }

    private static final DiffUtil.ItemCallback<ChatSessionEntity> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<ChatSessionEntity>() {
                @Override
                public boolean areItemsTheSame(@NonNull ChatSessionEntity old, @NonNull ChatSessionEntity n) {
                    return old.id == n.id;
                }

                @Override
                public boolean areContentsTheSame(@NonNull ChatSessionEntity old, @NonNull ChatSessionEntity n) {
                    return old.sessionName.equals(n.sessionName) &&
                            old.messageCount == n.messageCount &&
                            old.lastMessageAt == n.lastMessageAt;
                }
            };
}
