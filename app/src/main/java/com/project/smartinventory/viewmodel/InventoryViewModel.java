package com.project.smartinventory.viewmodel;

import android.app.Application;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.project.smartinventory.InventoryItem;
import com.project.smartinventory.database.DatabaseHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * InventoryViewModel
 *
 * Owns inventory data for the UI layer:
 *  - Exposes a list of {@link InventoryItem}s via LiveData.
 *  - Computes and exposes summary stats (total count, low-stock count).
 *  - Performs all DB I/O off the main thread and posts results back.
 *
 * Design notes:
 *  - Single source of truth is the DB (reads after each write).
 *  - Uses a single-thread executor to serialize DB operations.
 *  - UI should observe {@link #items}, {@link #totalCount}, and {@link #lowStockCount}.
 */
public class InventoryViewModel extends AndroidViewModel {

    // ---- Dependencies ----
    private final DatabaseHelper db;

    // Serialize DB work; avoids concurrent writes on simple setups
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    // ---- State (backing fields) ----
    private final MutableLiveData<List<InventoryItem>> _items = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Integer> _totalCount       = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> _lowStockCount    = new MutableLiveData<>(0);

    // ---- Public LiveData (immutable) ----
    public final LiveData<List<InventoryItem>> items   = _items;
    public final LiveData<Integer> totalCount          = _totalCount;
    public final LiveData<Integer> lowStockCount       = _lowStockCount;

    public InventoryViewModel(@NonNull Application app) {
        super(app);
        db = new DatabaseHelper(app);
        refresh(); // initial load
    }

    /**
     * Recalculates and posts the summary stats for the provided list.
     * Must be called on a background thread; results are posted to observers.
     */
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

    /**
     * Loads the full inventory from the database and updates LiveData.
     * Safe to call from the UI thread; work is dispatched to {@link #io}.
     */
    @MainThread
    public void refresh() {
        io.execute(() -> {
            List<InventoryItem> data = db.getAllItems();
            _items.postValue(data);
            recalcStats(data);
        });
    }

    /**
     * Adds a new item, then reloads list + stats.
     * (Simple approach: write-then-read to keep state consistent.)
     */
    @MainThread
    public void add(InventoryItem it) {
        io.execute(() -> {
            db.addItem(it);
            List<InventoryItem> data = db.getAllItems();
            _items.postValue(data);
            recalcStats(data);
        });
    }

    /**
     * Updates an existing item, then reloads list + stats.
     */
    @MainThread
    public void update(InventoryItem it) {
        io.execute(() -> {
            db.updateItem(it);
            List<InventoryItem> data = db.getAllItems();
            _items.postValue(data);
            recalcStats(data);
        });
    }

    /**
     * Deletes an item by id, then reloads list + stats.
     */
    @MainThread
    public void delete(int id) {
        io.execute(() -> {
            db.deleteItem(id);
            List<InventoryItem> data = db.getAllItems();
            _items.postValue(data);
            recalcStats(data);
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        io.shutdown(); // best-effort shutdown; ViewModel is going away
    }
}
