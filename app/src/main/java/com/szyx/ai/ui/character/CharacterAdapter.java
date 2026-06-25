package com.szyx.ai.ui.character;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.szyx.ai.R;
import com.szyx.ai.data.db.entity.CharacterEntity;

import java.io.File;

public class CharacterAdapter extends ListAdapter<CharacterEntity, CharacterAdapter.ViewHolder> {

    public interface OnCharacterClickListener {
        void onClick(CharacterEntity character);
    }

    public interface OnCharacterLongClickListener {
        void onLongClick(CharacterEntity character, View anchorView);
    }

    private final OnCharacterClickListener clickListener;
    private final OnCharacterLongClickListener longClickListener;

    public CharacterAdapter(OnCharacterClickListener clickListener,
                            OnCharacterLongClickListener longClickListener) {
        super(DIFF_CALLBACK);
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_character, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CharacterEntity character = getItem(position);
        holder.nameText.setText(character.name);
        holder.personalityText.setText(
                character.personality != null && !character.personality.isEmpty()
                        ? character.personality : character.description != null ? character.description : "");

        if (character.avatarPath != null && new File(character.avatarPath).exists()) {
            Glide.with(holder.avatarImage)
                    .load(character.avatarPath)
                    .placeholder(R.drawable.ic_default_avatar)
                    .error(R.drawable.ic_default_avatar)
                    .circleCrop()
                    .into(holder.avatarImage);
        } else {
            Glide.with(holder.avatarImage).clear(holder.avatarImage);
            holder.avatarImage.setImageResource(R.drawable.ic_default_avatar);
        }

        holder.itemView.setOnClickListener(v -> clickListener.onClick(character));
        holder.btnMore.setOnClickListener(v -> longClickListener.onLongClick(character, v));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView avatarImage;
        TextView nameText;
        TextView personalityText;
        ImageButton btnMore;

        ViewHolder(View itemView) {
            super(itemView);
            avatarImage = itemView.findViewById(R.id.avatarImage);
            nameText = itemView.findViewById(R.id.nameText);
            personalityText = itemView.findViewById(R.id.personalityText);
            btnMore = itemView.findViewById(R.id.btnMore);
        }
    }

    private static final DiffUtil.ItemCallback<CharacterEntity> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<CharacterEntity>() {
                @Override
                public boolean areItemsTheSame(@NonNull CharacterEntity old, @NonNull CharacterEntity n) {
                    return old.id == n.id;
                }

                @Override
                public boolean areContentsTheSame(@NonNull CharacterEntity old, @NonNull CharacterEntity n) {
                    return old.name.equals(n.name) &&
                            (old.personality == null ? n.personality == null : old.personality.equals(n.personality)) &&
                            old.updatedAt == n.updatedAt;
                }
            };
}
