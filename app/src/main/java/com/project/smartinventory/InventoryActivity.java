package com.project.smartinventory;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Spinner;
import android.widget.CheckBox;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.project.smartinventory.viewmodel.InventoryViewModel;
import com.project.smartinventory.viewmodel.LoginViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages the main inventory screen.
 *
 * <p>This activity is the central hub for viewing and managing inventory items.
 * It displays a list of items using a {@link RecyclerView}, provides summary counts
 * (total items, low stock items), and includes functionality for searching and filtering the list.
 *
 * <p><b>Key Responsibilities:</b>
 * <ul>
 *     <li>Observes {@link InventoryViewModel} to display the list of inventory items and summary data.</li>
 *     <li>Handles user interactions for adding new items (via a floating action button that launches
 *         {@link EditItemActivity}), and editing or deleting existing items through the list adapter.</li>
 *     <li>Provides search functionality to filter items by name.</li>
 *     <li>Offers a filter dialog to narrow down the list by category or low stock status.</li>
 *     <li>Manages the top app bar, which includes actions for navigation (e.g., to {@link NotificationActivity})
 *         and user session management (logout).</li>
 * </ul>
 *
 * @see InventoryViewModel
 * @see InventoryAdapter
 * @see EditItemActivity
 */
public class InventoryActivity extends AppCompatActivity {

    // ---- ViewModels ----
    private InventoryViewModel viewModel;
    private LoginViewModel loginViewModel;

    // ---- UI ----
    private RecyclerView recyclerView;
    private FloatingActionButton fab;
    private InventoryAdapter adapter;

    // ---- Filter ----
    private List<String> currentCategories = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);

        // ---- Toolbar ----
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        int colorOnPrimary = MaterialColors.getColor(toolbar,
                com.google.android.material.R.attr.colorOnPrimary
        );

        Drawable overflow = toolbar.getOverflowIcon();
        if (overflow != null) {
            overflow.setTint(colorOnPrimary);
        }

        // ---- Recycler + FAB ----
        recyclerView = findViewById(R.id.recyclerView);
        fab = findViewById(R.id.fabAdd);

        // ---- ViewModels ----
        viewModel = new ViewModelProvider(this).get(InventoryViewModel.class);
        loginViewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        viewModel.categories.observe(this, cats -> {
            currentCategories = (cats != null) ? cats : new ArrayList<>();
        });

        // Adapter with row-level actions
        adapter = new InventoryAdapter(new ArrayList<>(), new InventoryAdapter.OnItemClickListener() {
            @Override
            public void onEditClick(InventoryItem item, int position) {
                Intent intent = new Intent(InventoryActivity.this, EditItemActivity.class);
                intent.putExtra(EditItemActivity.EXTRA_ITEM, item);
                startActivity(intent);
            }

            @Override
            public void onDeleteClick(InventoryItem item, int position) {
                String docId = item.getDocumentId();
                if (docId != null && !docId.trim().isEmpty()) {
                    viewModel.delete(docId);
                }
            }
        });


        recyclerView.setLayoutManager(new GridLayoutManager(this, 1));
        recyclerView.setAdapter(adapter);

        // Summary counters (bound to ViewModel)
        TextView totalItemsCount = findViewById(R.id.totalItemsCount);
        TextView lowStockCount   = findViewById(R.id.lowStockCount);

        // Observe data -> update list
        viewModel.items.observe(this, items -> adapter.updateItems(items));

        // Observe counters -> update summary UI
        viewModel.totalCount.observe(this, n -> totalItemsCount.setText(String.valueOf(n)));
        viewModel.lowStockCount.observe(this, n -> lowStockCount.setText(String.valueOf(n)));

        // Add and edit items
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(this, EditItemActivity.class);
            startActivity(intent);
        });


        // ---- Search ----
        TextInputEditText searchEditText = findViewById(R.id.searchEditText);
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // no-op
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.setSearchQuery(s != null ? s.toString() : "");
            }

            @Override
            public void afterTextChanged(Editable s) {
                // no-op
            }
        });

        // ---- Filter ----
        MaterialButton filterButton = findViewById(R.id.filterButton);
        filterButton.setOnClickListener(v -> showFilterDialog());
    }

    // refresh inventory list on resume to display update after add or edit
    @Override
    protected void onResume() {
        super.onResume();
        viewModel.refresh();
    }

    // ---- App Bar Menu ----

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.inventory_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        final int id = item.getItemId();

        if (id == R.id.action_notifications) {
            startActivity(new Intent(this, NotificationActivity.class));
            return true;

        } else if (id == R.id.action_settings) {
            // Placeholder for future SettingsActivity
            return true;

        } else if (id == R.id.action_logout) {
            new AlertDialog.Builder(this)
                    .setTitle("Logout")  // TODO move to strings.xml
                    .setMessage("Are you sure you want to log out?")
                    .setPositiveButton("Logout", (d, w) -> {
                        loginViewModel.logout();
                        startActivity(new Intent(this, LoginActivity.class));
                        finish();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // ---- Dialogs ----


    /**
     * Displays a dialog for filtering the inventory list.
     *
     * <p>The dialog allows users to filter by a specific category and/or show only items
     * that are low in stock. The category spinner is populated with all unique categories
     * currently present in the inventory, plus an "All categories" option.
     *
     * <ul>
     *   <li><b>Apply:</b> Applies the selected category and low stock filters to the list by
     *       updating the {@link InventoryViewModel}.</li>
     *   <li><b>Clear:</b> Resets all filters, showing all items.</li>
     *   <li><b>Cancel:</b> Closes the dialog without changing the current filters.</li>
     * </ul>
     */
    private void showFilterDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_filter, null);

        Spinner categorySpinner   = dialogView.findViewById(R.id.categorySpinner);
        CheckBox lowStockCheckBox = dialogView.findViewById(R.id.lowStockCheckBox);

        List<String> options = new ArrayList<>();
        options.add("All categories");
        options.addAll(currentCategories);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                options
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);

        // Preselect current filters from ViewModel
        String currentCat = viewModel.getCurrentCategoryFilter();
        AtomicBoolean onlyLow   = new AtomicBoolean(viewModel.isOnlyLowStockFilterEnabled());

        // Default to "All categories"
        int selectionIndex = 0;

        if (currentCat != null) {
            // options[0] = "All categories", real categories start at index 1
            for (int i = 1; i < options.size(); i++) {
                if (options.get(i) != null &&
                        options.get(i).equalsIgnoreCase(currentCat)) {
                    selectionIndex = i;
                    break;
                }
            }
        }

        categorySpinner.setSelection(selectionIndex);
        lowStockCheckBox.setChecked(onlyLow.get());

        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("Apply", (d, w) -> {
                    String selected = (String) categorySpinner.getSelectedItem();
                    onlyLow.set(lowStockCheckBox.isChecked());

                    if ("All categories".equals(selected)) {
                        viewModel.setCategoryFilter(null);
                    } else {
                        viewModel.setCategoryFilter(selected);
                    }
                    viewModel.setOnlyLowStock(onlyLow.get());
                })
                .setNeutralButton("Clear", (d, w) -> {
                    viewModel.setCategoryFilter(null);
                    viewModel.setOnlyLowStock(false);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
