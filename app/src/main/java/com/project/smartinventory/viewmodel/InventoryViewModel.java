package com.project.smartinventory.viewmodel;

import android.app.Application;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.project.smartinventory.InventoryItem;
import com.project.smartinventory.repository.InventoryRepository;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * InventoryViewModel
 *
 * Owns inventory data for the UI layer:
 *  - Exposes a list of {@link InventoryItem}s via LiveData.
 *  - Computes and exposes summary stats (total count, low-stock count).
 *  - Loads and mutates data through InventoryRepository (Firestore).
 *
 * Design notes:
 *  - Single source of truth is Firestore (reload after each write).
 *  - Uses a single-thread executor only for local filtering work.
 *  - UI should observe {@link #items}, {@link #totalCount},
 *    {@link #lowStockCount}, and optionally {@link #isLoading}, {@link #error}.
 */
public class InventoryViewModel extends AndroidViewModel {

    private final InventoryRepository repo;
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    // Full list from Firestore
    private final MutableLiveData<List<InventoryItem>> _allItems =
            new MutableLiveData<>(new ArrayList<>());

    // Visible (filtered) list
    private final MutableLiveData<List<InventoryItem>> _items =
            new MutableLiveData<>(new ArrayList<>());

    private final MutableLiveData<Integer> _totalCount    = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> _lowStockCount = new MutableLiveData<>(0);

    // list of distinct categories for current inventory
    private final MutableLiveData<List<String>> _categories =
            new MutableLiveData<>(new ArrayList<>());

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String>  _error     = new MutableLiveData<>(null);

    // ---- Public LiveData ----
    public final LiveData<List<InventoryItem>> items      = _items;
    public final LiveData<Integer>             totalCount = _totalCount;
    public final LiveData<Integer>             lowStockCount = _lowStockCount;
    public final LiveData<List<String>>        categories = _categories;
    public final LiveData<Boolean>             isLoading  = _isLoading;
    public final LiveData<String>              error      = _error;

    // ---- Filter state ----
    private String  currentQuery    = "";
    private String  currentCategory = null; // null or "ALL" = no category filter
    private boolean onlyLowStock    = false;

    public InventoryViewModel(@NonNull Application app) {
        super(app);
        repo = new InventoryRepository();
        refresh();
    }

    // ---- Public API ----

    @MainThread
    public void refresh() {
        _error.setValue(null);
        _isLoading.setValue(true);
        reloadFromRepo();
    }

    @MainThread
    public void add(InventoryItem it) {
        _error.setValue(null);
        _isLoading.setValue(true);

        repo.addItem(it, new InventoryRepository.InventoryCallback<String>() {
            @Override
            public void onSuccess(String docId) {
                // assign generated docId to the item if you care
                it.setDocumentId(docId);
                reloadFromRepo();
            }

            @Override
            public void onError(String errorCode) {
                _isLoading.postValue(false);
                _error.postValue(errorCode);
            }
        });
    }

    @MainThread
    public void update(InventoryItem it) {
        _error.setValue(null);
        _isLoading.setValue(true);

        repo.updateItem(it, new InventoryRepository.InventoryCallback<Void>() {
            @Override
            public void onSuccess(Void unused) {
                reloadFromRepo();
            }

            @Override
            public void onError(String errorCode) {
                _isLoading.postValue(false);
                _error.postValue(errorCode);
            }
        });
    }

    @MainThread
    public void delete(String documentId) {
        _error.setValue(null);
        _isLoading.setValue(true);

        repo.deleteItem(documentId, new InventoryRepository.InventoryCallback<Void>() {
            @Override
            public void onSuccess(Void unused) {
                reloadFromRepo();
            }

            @Override
            public void onError(String errorCode) {
                _isLoading.postValue(false);
                _error.postValue(errorCode);
            }
        });
    }

    // ---- Filter setters (called from UI) ----

    @MainThread
    public void setSearchQuery(String query) {
        currentQuery = (query != null) ? query.trim().toLowerCase() : "";
        applyFiltersAsync();
    }

    @MainThread
    public void setCategoryFilter(String category) {
        if (category == null || category.trim().isEmpty() || "ALL".equalsIgnoreCase(category)) {
            currentCategory = null;
        } else {
            currentCategory = category.trim().toLowerCase();
        }
        applyFiltersAsync();
    }

    @MainThread
    public void setOnlyLowStock(boolean onlyLow) {
        onlyLowStock = onlyLow;
        applyFiltersAsync();
    }

    // ---- Internal data loading ----

    /** Load all items from Firestore via repository. */
    private void reloadFromRepo() {
        repo.getAllItems(new InventoryRepository.InventoryCallback<List<InventoryItem>>() {
            @Override
            public void onSuccess(List<InventoryItem> result) {
                _isLoading.postValue(false);
                _allItems.postValue(result);
                applyFiltersAndPost(result);
            }

            @Override
            public void onError(String errorCode) {
                _isLoading.postValue(false);
                _error.postValue(errorCode);
                // keep old _allItems; no update
            }
        });
    }

    // ---- Internal filtering ----

    /** Runs filter on a background thread using the latest _allItems value. */
    private void applyFiltersAsync() {
        io.execute(() -> {
            List<InventoryItem> source = _allItems.getValue();
            if (source == null) source = new ArrayList<>();
            applyFiltersAndPost(source);
        });
    }

    /** Applies search + filters to source list and posts to LiveData. */
    private void applyFiltersAndPost(List<InventoryItem> source) {
        if (source == null) source = new ArrayList<>();

        List<InventoryItem> result = new ArrayList<>();

        String q   = currentQuery;
        String cat = currentCategory;
        boolean onlyLow = onlyLowStock;

        for (InventoryItem it : source) {
            if (it == null) continue;

            // Low stock filter
            if (onlyLow && !it.isLowStock()) {
                continue;
            }

            // Category filter
            if (cat != null) {
                String itemCat = it.getCategory() != null
                        ? it.getCategory().toLowerCase()
                        : "";
                if (!itemCat.equals(cat)) {
                    continue;
                }
            }

            // Text search filter (name, description, category)
            if (q != null && !q.isEmpty()) {
                String name  = safeLower(it.getName());
                String desc  = safeLower(it.getDescription());
                String c     = safeLower(it.getCategory());
                if (!name.contains(q) && !desc.contains(q) && !c.contains(q)) {
                    continue;
                }
            }

            result.add(it);
        }

        _items.postValue(result);
        recalcStats(result);

        // recalc categories based on source (all items)
        LinkedHashSet<String> cats = new LinkedHashSet<>();
        for (InventoryItem it : source) {
            if (it == null) continue;
            String c = it.getCategory();
            if (c == null) continue;
            c = c.trim();
            if (!c.isEmpty()) {
                cats.add(c);
            }
        }
        _categories.postValue(new ArrayList<>(cats));
    }

    // ---- Filter getters ----
    public String getCurrentCategoryFilter() {
        return currentCategory;
    }

    public boolean isOnlyLowStockFilterEnabled() {
        return onlyLowStock;
    }

    private String safeLower(String s) {
        return s != null ? s.toLowerCase() : "";
    }

    private void recalcStats(List<InventoryItem> list) {
        int total = (list != null) ? list.size() : 0;
        int low = 0;
        if (list != null) {
            for (InventoryItem it : list) {
                if (it != null && it.isLowStock()) low++;
            }
        }
        _totalCount.postValue(total);
        _lowStockCount.postValue(low);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        io.shutdown();
    }
}
