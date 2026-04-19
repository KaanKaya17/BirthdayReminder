package com.example.birthdayreminder;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.birthdayreminder.adapter.BirthdayAdapter;
import com.example.birthdayreminder.databinding.ActivityMainBinding;
import com.example.birthdayreminder.adapter.UpcomingAdapter;
import com.example.birthdayreminder.data.Birthday;
import com.example.birthdayreminder.util.BirthdayUtils;
import com.example.birthdayreminder.viewmodel.BirthdayViewModel;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private BirthdayViewModel viewModel;
    private BirthdayAdapter birthdayAdapter;
    private UpcomingAdapter upcomingAdapter;

    private ActivityResultLauncher<Intent> addEditLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupViewModel();
        setupRecyclerViews();
        setupSearch();
        setupFab();
        setupSwipeToDelete();
        setupAddEditLauncher();
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(BirthdayViewModel.class);
    }

    private void setupRecyclerViews() {
        // Main list
        birthdayAdapter = new BirthdayAdapter(birthday -> {
            Intent intent = new Intent(this, BirthdayDetailActivity.class);
            intent.putExtra("birthday_id", birthday.getId());
            startActivity(intent);
        });
        binding.birthdayRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.birthdayRecyclerView.setAdapter(birthdayAdapter);

        // Upcoming horizontal list
        upcomingAdapter = new UpcomingAdapter(birthday -> {
            Intent intent = new Intent(this, BirthdayDetailActivity.class);
            intent.putExtra("birthday_id", birthday.getId());
            startActivity(intent);
        });
        binding.upcomingRecyclerView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.upcomingRecyclerView.setAdapter(upcomingAdapter);

        // Observe data
        viewModel.getSearchResults().observe(this, this::updateUI);
    }

    private void updateUI(List<Birthday> birthdays) {
        if (birthdays == null || birthdays.isEmpty()) {
            binding.emptyState.setVisibility(View.VISIBLE);
            binding.birthdayRecyclerView.setVisibility(View.GONE);
            binding.upcomingSection.setVisibility(View.GONE);
            binding.allBirthdaysTitle.setVisibility(View.GONE);
        } else {
            binding.emptyState.setVisibility(View.GONE);
            binding.birthdayRecyclerView.setVisibility(View.VISIBLE);
            binding.allBirthdaysTitle.setVisibility(View.VISIBLE);

            // Sort by days until next birthday
            List<Birthday> sorted = new ArrayList<>(birthdays);
            sorted.sort(Comparator.comparingLong(b ->
                    BirthdayUtils.daysUntilNextBirthday(b.getDay(), b.getMonth())));

            birthdayAdapter.setBirthdays(sorted);

            // Upcoming: only those within 30 days
            List<Birthday> upcoming = sorted.stream()
                    .filter(b -> BirthdayUtils.daysUntilNextBirthday(b.getDay(), b.getMonth()) <= 30)
                    .collect(Collectors.toList());

            if (!upcoming.isEmpty()) {
                binding.upcomingSection.setVisibility(View.VISIBLE);
                upcomingAdapter.setBirthdays(upcoming);
            } else {
                binding.upcomingSection.setVisibility(View.GONE);
            }
        }
    }

    private void setupSearch() {
        binding.searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.setSearchQuery(s.toString().trim());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupFab() {
        binding.fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddEditBirthdayActivity.class);
            addEditLauncher.launch(intent);
        });
    }

    private void setupAddEditLauncher() {
        addEditLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // Data refreshes automatically via LiveData
                }
        );
    }

    private void setupSwipeToDelete() {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                                  RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Birthday deletedBirthday = birthdayAdapter.getBirthdayAt(position);

                viewModel.delete(deletedBirthday);

                Snackbar.make(binding.birthdayRecyclerView,
                                getString(R.string.birthday_deleted), Snackbar.LENGTH_LONG)
                        .setAction(getString(R.string.undo), v -> {
                            viewModel.insert(deletedBirthday, null);
                        })
                        .setActionTextColor(getColor(R.color.secondary))
                        .show();
            }
        }).attachToRecyclerView(binding.birthdayRecyclerView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Force refresh by re-setting search query
        String currentQuery = binding.searchEditText.getText() != null ? binding.searchEditText.getText().toString() : "";
        viewModel.setSearchQuery(currentQuery);
    }
}