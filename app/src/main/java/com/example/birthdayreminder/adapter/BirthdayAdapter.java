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
import com.example.birthdayreminder.databinding.ItemBirthdayBinding;
import com.example.birthdayreminder.util.BirthdayUtils;

import java.util.ArrayList;
import java.util.List;

public class BirthdayAdapter extends RecyclerView.Adapter<BirthdayAdapter.ViewHolder> {

    private List<Birthday> birthdays = new ArrayList<>();
    private OnBirthdayClickListener listener;

    public interface OnBirthdayClickListener {
        void onBirthdayClick(Birthday birthday);
    }

    public BirthdayAdapter(OnBirthdayClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemBirthdayBinding binding = ItemBirthdayBinding.inflate(
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

    public Birthday getBirthdayAt(int position) {
        return birthdays.get(position);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemBirthdayBinding binding;

        ViewHolder(ItemBirthdayBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            binding.getRoot().setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onBirthdayClick(birthdays.get(pos));
                }
            });
        }

        void bind(Birthday birthday) {
            binding.tvName.setText(birthday.getName());
            binding.tvDate.setText(BirthdayUtils.formatDate(birthday.getDay(), birthday.getMonth(), birthday.getYear()));
            binding.tvInitials.setText(BirthdayUtils.getInitials(birthday.getName()));

            // Avatar color
            int color = BirthdayUtils.getAvatarColor(birthday.getName());
            GradientDrawable avatarDrawable = new GradientDrawable();
            avatarDrawable.setShape(GradientDrawable.OVAL);
            avatarDrawable.setColor(color);
            binding.avatarBackground.setBackground(avatarDrawable);

            // Days remaining badge
            long days = BirthdayUtils.daysUntilNextBirthday(birthday.getDay(), birthday.getMonth());
            binding.tvDaysRemaining.setText(BirthdayUtils.formatDaysRemaining(days));

            // Badge color based on proximity
            GradientDrawable badgeDrawable = new GradientDrawable();
            badgeDrawable.setCornerRadius(100f);
            if (days == 0) {
                badgeDrawable.setColor(binding.getRoot().getContext().getColor(R.color.today_badge));
            } else if (days <= 7) {
                badgeDrawable.setColor(binding.getRoot().getContext().getColor(R.color.secondary));
            } else if (days <= 30) {
                badgeDrawable.setColor(binding.getRoot().getContext().getColor(R.color.soon_badge));
            } else {
                badgeDrawable.setColor(binding.getRoot().getContext().getColor(R.color.later_badge));
            }
            binding.tvDaysRemaining.setBackground(badgeDrawable);
        }
    }
}
