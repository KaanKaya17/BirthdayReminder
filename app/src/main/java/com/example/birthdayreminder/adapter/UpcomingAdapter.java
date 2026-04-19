package com.example.birthdayreminder.adapter;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.birthdayreminder.R;
import com.example.birthdayreminder.data.Birthday;
import com.example.birthdayreminder.databinding.ItemUpcomingBirthdayBinding;
import com.example.birthdayreminder.util.BirthdayUtils;

import java.util.ArrayList;
import java.util.List;

public class UpcomingAdapter extends RecyclerView.Adapter<UpcomingAdapter.ViewHolder> {

    private List<Birthday> birthdays = new ArrayList<>();
    private OnUpcomingClickListener listener;

    public interface OnUpcomingClickListener {
        void onUpcomingClick(Birthday birthday);
    }

    public UpcomingAdapter(OnUpcomingClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemUpcomingBirthdayBinding binding = ItemUpcomingBirthdayBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Birthday birthday = birthdays.get(position);
        holder.bind(birthday);
    }

    @Override
    public int getItemCount() {
        return birthdays.size();
    }

    public void setBirthdays(List<Birthday> birthdays) {
        this.birthdays = birthdays;
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemUpcomingBirthdayBinding binding;

        ViewHolder(ItemUpcomingBirthdayBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            binding.getRoot().setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onUpcomingClick(birthdays.get(pos));
                }
            });
        }

        void bind(Birthday birthday) {
            binding.tvName.setText(birthday.getName());
            binding.tvDate.setText(BirthdayUtils.formatDate(birthday.getDay(), birthday.getMonth(), 0));
            binding.tvInitials.setText(BirthdayUtils.getInitials(birthday.getName()));

            int color = BirthdayUtils.getAvatarColor(birthday.getName());
            GradientDrawable avatarDrawable = new GradientDrawable();
            avatarDrawable.setShape(GradientDrawable.OVAL);
            avatarDrawable.setColor(color);
            binding.avatarBackground.setBackground(avatarDrawable);

            long days = BirthdayUtils.daysUntilNextBirthday(birthday.getDay(), birthday.getMonth());
            binding.tvDaysRemaining.setText(BirthdayUtils.formatDaysRemaining(days));
        }
    }
}
