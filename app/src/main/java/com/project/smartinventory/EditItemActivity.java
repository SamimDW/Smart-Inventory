package com.project.smartinventory;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.project.smartinventory.viewmodel.InventoryViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * An Activity for creating a new inventory item or editing an existing one.
 * <p>
 * This screen provides a form to input or modify details of an {@link InventoryItem},
 * such as its name, description, category, quantity, and price. It operates in two modes:
 * "add mode" and "edit mode".
 * </p>
 * <ul>
 *     <li>
 *         <b>Add Mode:</b> Activated when the activity is started without an existing item.
 *         The form is blank, and a new item is created upon submission.
 *     </li>
 *     <li>
 *         <b>Edit Mode:</b> Activated by passing an {@link InventoryItem} object via an Intent
 *         with the key {@link #EXTRA_ITEM}. The form is pre-populated with the item's data,
 *         and changes are saved to the existing item upon submission.
 *     </li>
 * </ul>
 * <p>
 * The activity uses {@link InventoryViewModel} to interact with the underlying data repository,
 * ensuring that data operations are lifecycle-aware and survive configuration changes.
 * The form includes validation for required fields like name, quantity, and price.
 * </p>
 *
 * @see InventoryItem
 * @see InventoryViewModel
 */
public class EditItemActivity extends AppCompatActivity {

    public static final String EXTRA_ITEM = "extra_item";

    private InventoryViewModel viewModel;
    private InventoryItem existingItem;
    private boolean isEditMode;

    // UI
    private MaterialToolbar topAppBar;
    private TextInputLayout itemNameInputLayout;
    private TextInputLayout itemDescriptionInputLayout;
    private TextInputLayout categoryInputLayout;
    private TextInputLayout quantityInputLayout;
    private TextInputLayout priceInputLayout;
    private TextInputLayout minStockInputLayout;
    private TextInputLayout dateAddedInputLayout;

    private TextInputEditText itemNameEditText;
    private TextInputEditText itemDescriptionEditText;
    private AutoCompleteTextView categoryAutoComplete;
    private TextInputEditText quantityEditText;
    private TextInputEditText priceEditText;
    private TextInputEditText minStockEditText;
    private TextInputEditText dateAddedEditText;

    private MaterialButton cancelButton;
    private MaterialButton addItemButton;

    private ArrayAdapter<String> categoryAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_item);

        // ViewModel
        viewModel = new ViewModelProvider(this).get(InventoryViewModel.class);

        // Read existing item if provided
        existingItem = (InventoryItem) getIntent().getSerializableExtra(EXTRA_ITEM);
        isEditMode = (existingItem != null);

        bindViews();
        setupToolbar();
        setupCategoryDropdown();
        bindExistingDataIfNeeded();
        setupButtons();
    }

    private void bindViews() {
        topAppBar = findViewById(R.id.topAppBar);

        itemNameInputLayout        = findViewById(R.id.itemNameInputLayout);
        itemDescriptionInputLayout = findViewById(R.id.itemDescriptionInputLayout);
        categoryInputLayout        = findViewById(R.id.categoryInputLayout);
        quantityInputLayout        = findViewById(R.id.quantityInputLayout);
        priceInputLayout           = findViewById(R.id.priceInputLayout);
        minStockInputLayout        = findViewById(R.id.minStockInputLayout);
        dateAddedInputLayout       = findViewById(R.id.dateAddedInputLayout);

        itemNameEditText        = findViewById(R.id.itemNameEditText);
        itemDescriptionEditText = findViewById(R.id.itemDescriptionEditText);
        categoryAutoComplete    = findViewById(R.id.categoryAutoComplete);
        quantityEditText        = findViewById(R.id.quantityEditText);
        priceEditText           = findViewById(R.id.priceEditText);
        minStockEditText        = findViewById(R.id.minStockEditText);
        dateAddedEditText       = findViewById(R.id.dateAddedEditText);

        cancelButton   = findViewById(R.id.cancelButton);
        addItemButton  = findViewById(R.id.addItemButton);
    }

    private void setupToolbar() {
        // Use toolbar navigation as back
        topAppBar.setNavigationOnClickListener(v -> onBackPressed());

        if (isEditMode) {
            topAppBar.setTitle(R.string.edit_item_title);
            addItemButton.setText(R.string.save_changes);
        } else {
            topAppBar.setTitle(R.string.add_new_item_title);
            addItemButton.setText(R.string.add_item);
        }
    }

    private void setupCategoryDropdown() {
        categoryAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                new ArrayList<>()
        );
        categoryAutoComplete.setAdapter(categoryAdapter);

        // Populate from current categories in ViewModel
        viewModel.categories.observe(this, cats -> {
            List<String> safe = (cats != null) ? cats : new ArrayList<>();
            categoryAdapter.clear();
            categoryAdapter.addAll(safe);
            categoryAdapter.notifyDataSetChanged();
        });
    }

    private void bindExistingDataIfNeeded() {
        if (isEditMode) {
            itemNameEditText.setText(existingItem.getName());
            itemDescriptionEditText.setText(existingItem.getDescription());
            categoryAutoComplete.setText(existingItem.getCategory(), false);
            quantityEditText.setText(String.valueOf(existingItem.getQuantity()));
            priceEditText.setText(String.valueOf(existingItem.getPrice()));

            int threshold = existingItem.getLowStockThreshold();
            if (threshold > 0) {
                minStockEditText.setText(String.valueOf(threshold));
            }

            // If you later add dateAdded to the model, bind it here.
            // For now, leave this blank or show a placeholder.
        } else {
            // New item: set today's date as a hint value (visual only for now)
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    .format(new Date());
            dateAddedEditText.setText(today);
        }
    }

    private void setupButtons() {
        cancelButton.setOnClickListener(v -> finish());

        addItemButton.setOnClickListener(v -> {
            if (!validateForm()) {
                return;
            }

            String name        = itemNameEditText.getText().toString().trim();
            String description = itemDescriptionEditText.getText().toString().trim();
            String category    = categoryAutoComplete.getText().toString().trim();
            int quantity       = parseIntSafe(quantityEditText.getText());
            double price       = parseDoubleSafe(priceEditText.getText());
            int minStock       = parseIntSafe(minStockEditText.getText());

            if (minStock <= 0) {
                // Fallback threshold if user leaves it empty or invalid
                minStock = 5;
            }

            if (isEditMode) {
                existingItem.setName(name);
                existingItem.setDescription(description);
                existingItem.setCategory(category);
                existingItem.setQuantity(quantity);
                existingItem.setPrice(price);
                existingItem.setLowStockThreshold(minStock);

                viewModel.update(existingItem);
            } else {
                InventoryItem newItem = new InventoryItem();
                newItem.setName(name);
                newItem.setDescription(description);
                newItem.setCategory(category);
                newItem.setQuantity(quantity);
                newItem.setPrice(price);
                newItem.setLowStockThreshold(minStock);

                viewModel.add(newItem);
            }

            // For a production app, you would wait for success.
            // For this project, close immediately and let InventoryActivity refresh.
            finish();
        });
    }

    private boolean validateForm() {
        clearErrors();

        boolean valid = true;

        String name     = text(itemNameEditText);
        String quantity = text(quantityEditText);
        String price    = text(priceEditText);

        if (TextUtils.isEmpty(name)) {
            itemNameInputLayout.setError(getString(R.string.error_name_required));
            valid = false;
        }

        if (TextUtils.isEmpty(quantity)) {
            quantityInputLayout.setError(getString(R.string.error_quantity_required));
            valid = false;
        }

        if (TextUtils.isEmpty(price)) {
            priceInputLayout.setError(getString(R.string.error_price_required));
            valid = false;
        }

        int q = parseIntSafe(quantityEditText.getText());
        if (q < 0) {
            quantityInputLayout.setError(getString(R.string.error_quantity_negative));
            valid = false;
        }

        double p = parseDoubleSafe(priceEditText.getText());
        if (p < 0) {
            priceInputLayout.setError(getString(R.string.error_price_negative));
            valid = false;
        }

        return valid;
    }

    private void clearErrors() {
        itemNameInputLayout.setError(null);
        itemDescriptionInputLayout.setError(null);
        categoryInputLayout.setError(null);
        quantityInputLayout.setError(null);
        priceInputLayout.setError(null);
        minStockInputLayout.setError(null);
    }

    private String text(TextInputEditText editText) {
        return editText.getText() != null
                ? editText.getText().toString().trim()
                : "";
    }

    private int parseIntSafe(CharSequence s) {
        try {
            String raw = s != null ? s.toString().replace(",", "").trim() : "";
            if (raw.isEmpty()) return 0;
            return Integer.parseInt(raw);
        } catch (Exception e) {
            return 0;
        }
    }

    private double parseDoubleSafe(CharSequence s) {
        try {
            String raw = s != null ? s.toString().replace(",", "").trim() : "";
            if (raw.isEmpty()) return 0.0;
            return Double.parseDouble(raw);
        } catch (Exception e) {
            return 0.0;
        }
    }
}
