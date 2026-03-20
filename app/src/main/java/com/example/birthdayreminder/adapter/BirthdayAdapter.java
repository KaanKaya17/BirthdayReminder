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
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_birthday, parent, false);
        return new ViewHolder(view);
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
        private final View avatarBackground;
        private final TextView tvInitials;
        private final TextView tvName;
        private final TextView tvDate;
        private final TextView tvDaysRemaining;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            avatarBackground = itemView.findViewById(R.id.avatarBackground);
            tvInitials = itemView.findViewById(R.id.tvInitials);
            tvName = itemView.findViewById(R.id.tvName);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvDaysRemaining = itemView.findViewById(R.id.tvDaysRemaining);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onBirthdayClick(birthdays.get(pos));
                }
            });
        }

        void bind(Birthday birthday) {
            tvName.setText(birthday.getName());
            tvDate.setText(BirthdayUtils.formatDate(birthday.getDay(), birthday.getMonth(), birthday.getYear()));
            tvInitials.setText(BirthdayUtils.getInitials(birthday.getName()));

            // Avatar color
            int color = BirthdayUtils.getAvatarColor(birthday.getName());
            GradientDrawable avatarDrawable = new GradientDrawable();
            avatarDrawable.setShape(GradientDrawable.OVAL);
            avatarDrawable.setColor(color);
            avatarBackground.setBackground(avatarDrawable);

            // Days remaining badge
            long days = BirthdayUtils.daysUntilNextBirthday(birthday.getDay(), birthday.getMonth());
            tvDaysRemaining.setText(BirthdayUtils.formatDaysRemaining(days));

            // Badge color based on proximity
            GradientDrawable badgeDrawable = new GradientDrawable();
            badgeDrawable.setCornerRadius(100f);
            if (days == 0) {
                badgeDrawable.setColor(itemView.getContext().getColor(R.color.today_badge));
            } else if (days <= 7) {
                badgeDrawable.setColor(itemView.getContext().getColor(R.color.secondary));
            } else if (days <= 30) {
                badgeDrawable.setColor(itemView.getContext().getColor(R.color.soon_badge));
            } else {
                badgeDrawable.setColor(itemView.getContext().getColor(R.color.later_badge));
            }
            tvDaysRemaining.setBackground(badgeDrawable);
        }
    }
}
