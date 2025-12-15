package com.project.smartinventory;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.project.smartinventory.viewmodel.InventoryViewModel;
import com.project.smartinventory.viewmodel.LoginViewModel;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

/**
 * InventoryActivity
 *
 * Displays the inventory list, summary counts, and provides CRUD affordances
 * (add/edit/delete) via a dialog. Also hosts top-app-bar actions (notifications,
 * settings, logout) through a menu.
 *
 * Responsibilities:
 *  - Initialize RecyclerView + adapter and observe {@link InventoryViewModel} for data.
 *  - Show “Add/Edit item” dialog and persist changes through the ViewModel.
 *  - Handle top app bar actions (navigate to NotificationActivity, confirm logout, etc.).
 *
 * Notes:
 *  - Edge-to-edge is intentionally disabled here for simplicity and predictable layout.
 *  - All user-facing strings should live in strings.xml (marked below with TODO).
 */
public class InventoryActivity extends AppCompatActivity {

    // ---- ViewModels ----
    private InventoryViewModel viewModel;
    private LoginViewModel loginViewModel;

    // ---- UI ----
    private RecyclerView recyclerView;
    private FloatingActionButton fab;
    private InventoryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Simpler, non edge-to-edge layout to avoid inset handling
        setContentView(R.layout.activity_inventory);

        // ---- Toolbar ----
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // ---- Recycler + FAB ----
        recyclerView = findViewById(R.id.recyclerView);
        fab = findViewById(R.id.fabAdd);

        // ---- ViewModels ----
        viewModel = new ViewModelProvider(this).get(InventoryViewModel.class);
        loginViewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        // Adapter with row-level actions
        adapter = new InventoryAdapter(new ArrayList<>(), new InventoryAdapter.OnItemClickListener() {
            @Override public void onEditClick(InventoryItem item, int position) { showEditDialog(item); }
            @Override public void onDeleteClick(InventoryItem item, int position) { viewModel.delete(item.getId()); }
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

        // Add new item
        fab.setOnClickListener(v -> showAddDialog());
    }

    // ---- App Bar Menu ----

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // NOTE: ensure this file name matches your res/menu XML (menu file is "inventory_menu.xml" here)
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
            // TODO strings.xml: screen title
            // TODO: If/when you add a SettingsActivity, start it here.
            // startActivity(new Intent(this, SettingsActivity.class));
            return true;

        } else if (id == R.id.action_logout) {
            // Confirm before clearing auth and returning to login
            new AlertDialog.Builder(this)
                    .setTitle("Logout")  // TODO strings.xml
                    .setMessage("Are you sure you want to log out?") // TODO strings.xml
                    .setPositiveButton("Logout", (d, w) -> {          // TODO strings.xml
                        loginViewModel.logout();
                        startActivity(new Intent(this, LoginActivity.class));
                        finish();
                    })
                    .setNegativeButton("Cancel", null) // TODO strings.xml
                    .show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // ---- Dialogs ----

    /** Convenience wrapper for Add flow. */
    private void showAddDialog() {
        showEditOrAddDialog(null);
    }

    /** Convenience wrapper for Edit flow. */
    private void showEditDialog(InventoryItem item) {
        showEditOrAddDialog(item);
    }

    /**
     * Builds and shows the Add/Edit dialog. If {@code existing} is null, this acts as “Add”.
     * Otherwise, fields are pre-populated and “Save” updates the existing item.
     */
    private void showEditOrAddDialog(InventoryItem existing) {
        final View dialog = getLayoutInflater().inflate(R.layout.dialog_item, null);

        final TextInputEditText name  = dialog.findViewById(R.id.inputName);
        final TextInputEditText desc  = dialog.findViewById(R.id.inputDesc);
        final TextInputEditText cat   = dialog.findViewById(R.id.inputCategory);
        final TextInputEditText qty   = dialog.findViewById(R.id.inputQuantity);
        final TextInputEditText price = dialog.findViewById(R.id.inputPrice);

        // Pre-fill for edit
        if (existing != null) {
            name.setText(existing.getName());
            desc.setText(existing.getDescription());
            cat.setText(existing.getCategory());
            qty.setText(String.valueOf(existing.getQuantity()));
            // Display price using a readable number format; parsing will normalize it
            price.setText(NumberFormat.getNumberInstance(Locale.US).format(existing.getPrice()));
        }

        new AlertDialog.Builder(this)
                .setTitle(existing == null ? "Add Item" : "Edit Item") // TODO strings.xml
                .setView(dialog)
                .setPositiveButton(existing == null ? "Add" : "Save", (d, w) -> { // TODO strings.xml
                    // Extract and normalize inputs
                    final String n  = String.valueOf(name.getText()).trim();
                    final String de = String.valueOf(desc.getText()).trim();
                    final String ca = String.valueOf(cat.getText()).trim();
                    final int q     = parseIntSafe(String.valueOf(qty.getText()).trim());
                    final double pr = parseDoubleSafe(String.valueOf(price.getText()).trim());

                    if (existing == null) {
                        // New item (id=0; real id assigned by DB/repo)
                        InventoryItem it = new InventoryItem(0, n, de, ca, q, pr);
                        viewModel.add(it);
                    } else {
                        // Update existing
                        existing.setName(n);
                        existing.setDescription(de);
                        existing.setCategory(ca);
                        existing.setQuantity(q);
                        existing.setPrice(pr);
                        viewModel.update(existing);
                    }
                })
                .setNegativeButton("Cancel", null) // TODO strings.xml
                .show();
    }

    // ---- Parsing helpers ----

    /** Parses an int from a possibly formatted string (commas allowed). Returns def on failure. */
    private int parseIntSafe(String s) {
        try {
            return Integer.parseInt(s.replaceAll(",", ""));
        } catch (Exception e) {
            return 0;
        }
    }

    /** Parses a double from a possibly formatted string (commas allowed). Returns def on failure. */
    private double parseDoubleSafe(String s) {
        try {
            return Double.parseDouble(s.replaceAll(",", ""));
        } catch (Exception e) {
            return 0.0;
        }
    }
}
