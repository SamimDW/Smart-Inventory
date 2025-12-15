package com.project.smartinventory;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * InventoryAdapter
 * <p>
 * RecyclerView adapter for displaying {@link InventoryItem} rows.
 * <p>
 * Design highlights:
 * - Uses DiffUtil to efficiently update the list (insert/move/change with animations).
 * - Supports partial binds via payloads (only rebinds changed fields).
 * - Exposes simple callbacks for edit/delete actions and optional data-change hooks
 *   to update empty states and counters in the hosting screen.
 * <p>
 * Firestore note:
 * - Identity is based on documentId (String) instead of a local DB integer id.
 */
public class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.InventoryViewHolder> {

    private List<InventoryItem> inventoryItems;
    private final OnItemClickListener listener;
    private OnDataChangedListener dataChangedListener;

    /** Row-level actions surfaced to the host (Activity/Fragment). */
    public interface OnItemClickListener {
        void onEditClick(InventoryItem item, int position);
        void onDeleteClick(InventoryItem item, int position);
    }

    /**
     * Optional hooks to inform the host about list-level state (empty, counts).
     * Useful for toggling empty views or updating summary chips.
     */
    public interface OnDataChangedListener {
        void onEmpty(boolean isEmpty);
        void onCountsChanged(int totalCount, int lowStockCount);
    }

    /** Register optional data change hooks. Safe to call anytime. */
    public void setOnDataChangedListener(OnDataChangedListener l) {
        this.dataChangedListener = l;
        notifyDataChangedHooks();
    }

    /**
     * Construct the adapter.
     *
     * @param inventoryItems initial items (can be null; will be treated as empty)
     * @param listener row-level action listener (edit/delete)
     */
    public InventoryAdapter(List<InventoryItem> inventoryItems, OnItemClickListener listener) {
        this.inventoryItems = inventoryItems != null ? inventoryItems : new ArrayList<>();
        this.listener = listener;
        // No stable IDs here; Firestore uses String documentIds and the list is driven by DiffUtil.
    }

    @NonNull
    @Override
    public InventoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_inventory_row, parent, false);
        return new InventoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InventoryViewHolder holder, int position) {
        InventoryItem item = inventoryItems.get(position);
        holder.bind(item, listener);
    }

    /**
     * Partial rebinds using payloads: avoids rebinding unchanged views (better perf).
     * Falls back to full bind when payloads are empty.
     */
    @Override
    public void onBindViewHolder(@NonNull InventoryViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads);
            return;
        }
        Bundle p = (Bundle) payloads.get(0);
        InventoryItem item = inventoryItems.get(position);
        holder.bindPartial(item, p);
    }

    @Override
    public int getItemCount() {
        return inventoryItems != null ? inventoryItems.size() : 0;
    }

    // ---------- Public API ----------

    /**
     * Replace the current list with a new list using DiffUtil for efficient updates.
     * Preserves animations and only rebinds changed rows/fields.
     */
    public void updateItems(List<InventoryItem> newItems) {
        if (newItems == null) newItems = new ArrayList<>();

        final List<InventoryItem> oldList = new ArrayList<>(this.inventoryItems);
        final List<InventoryItem> newList = new ArrayList<>(newItems);

        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override public int getOldListSize() { return oldList.size(); }
            @Override public int getNewListSize() { return newList.size(); }

            @Override
            public boolean areItemsTheSame(int oldPos, int newPos) {
                InventoryItem o = oldList.get(oldPos);
                InventoryItem n = newList.get(newPos);
                String oldId = o.getDocumentId();
                String newId = n.getDocumentId();

                if (oldId == null || newId == null) {
                    // Fallback: if both are null, fall back to object equality
                    return o == n;
                }
                return oldId.equals(newId);
            }


            @Override
            public boolean areContentsTheSame(int oldPos, int newPos) {
                InventoryItem o = oldList.get(oldPos);
                InventoryItem n = newList.get(newPos);
                return eq(o.getName(), n.getName())
                        && eq(o.getDescription(), n.getDescription())
                        && eq(o.getCategory(), n.getCategory())
                        && o.getQuantity() == n.getQuantity()
                        && Double.compare(o.getPrice(), n.getPrice()) == 0
                        && o.isLowStock() == n.isLowStock();
            }

            @Override
            public Object getChangePayload(int oldPos, int newPos) {
                InventoryItem o = oldList.get(oldPos);
                InventoryItem n = newList.get(newPos);
                Bundle p = new Bundle();
                if (!eq(o.getName(), n.getName())) p.putBoolean("name", true);
                if (!eq(o.getDescription(), n.getDescription())) p.putBoolean("desc", true);
                if (!eq(o.getCategory(), n.getCategory())) p.putBoolean("cat", true);
                if (o.getQuantity() != n.getQuantity()
                        || o.isLowStock() != n.isLowStock()) {
                    p.putBoolean("qty", true);
                }
                if (Double.compare(o.getPrice(), n.getPrice()) != 0) {
                    p.putBoolean("price", true);
                }
                return p.isEmpty() ? null : p;
            }
        });

        this.inventoryItems = newList;
        diff.dispatchUpdatesTo(this);
        notifyDataChangedHooks();
    }

    /** Add a single item by producing a new list and delegating to {@link #updateItems(List)}. */
    public void addItem(InventoryItem item) {
        List<InventoryItem> copy = new ArrayList<>(inventoryItems);
        copy.add(item);
        updateItems(copy);
    }

    /** Remove an item by Firestore documentId (no-op if id not found). */
    public void removeByDocumentId(String documentId) {
        if (documentId == null) return;
        List<InventoryItem> copy = new ArrayList<>(inventoryItems);
        for (int i = 0; i < copy.size(); i++) {
            String id = copy.get(i).getDocumentId();
            if (documentId.equals(id)) {
                copy.remove(i);
                break;
            }
        }
        updateItems(copy);
    }

    /**
     * Replace an existing item with matching documentId; if not present, append it.
     * Handy for upserts from dialogs or network responses.
     */
    public void replaceOrAdd(InventoryItem updated) {
        String updatedId = updated.getDocumentId();
        List<InventoryItem> copy = new ArrayList<>(inventoryItems);
        boolean found = false;

        if (updatedId != null) {
            for (int i = 0; i < copy.size(); i++) {
                String existingId = copy.get(i).getDocumentId();
                if (updatedId.equals(existingId)) {
                    copy.set(i, updated);
                    found = true;
                    break;
                }
            }
        }

        if (!found) copy.add(updated);
        updateItems(copy);
    }

    // ---------- Helpers ----------

    private static boolean eq(Object a, Object b) {
        return Objects.equals(a, b);
    }

    /** Computes and emits aggregate list state to the optional dataChangedListener. */
    private void notifyDataChangedHooks() {
        if (dataChangedListener == null) return;
        int total = inventoryItems.size();
        int low = 0;
        for (InventoryItem it : inventoryItems) {
            if (it.isLowStock()) low++;
        }
        dataChangedListener.onCountsChanged(total, low);
        dataChangedListener.onEmpty(total == 0);
    }

    // ---------- ViewHolder ----------

    static class InventoryViewHolder extends RecyclerView.ViewHolder {
        private final TextView itemNameText;
        private final TextView itemDescriptionText;
        private final TextView categoryText;
        private final TextView quantityText;
        private final TextView priceText;
        private final TextView lowStockIndicator;
        private final ImageButton editButton;
        private final ImageButton deleteButton;

        InventoryViewHolder(@NonNull View itemView) {
            super(itemView);
            itemNameText        = itemView.findViewById(R.id.itemNameText);
            itemDescriptionText = itemView.findViewById(R.id.itemDescriptionText);
            categoryText        = itemView.findViewById(R.id.categoryText);
            quantityText        = itemView.findViewById(R.id.quantityText);
            priceText           = itemView.findViewById(R.id.priceText);
            lowStockIndicator   = itemView.findViewById(R.id.lowStockIndicator);
            editButton          = itemView.findViewById(R.id.editButton);
            deleteButton        = itemView.findViewById(R.id.deleteButton);
        }

        void bind(InventoryItem item, OnItemClickListener listener) {
            itemNameText.setText(item.getName());
            itemDescriptionText.setText(item.getDescription());
            categoryText.setText(item.getCategory());
            quantityText.setText(String.valueOf(item.getQuantity()));

            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
            priceText.setText(currencyFormat.format(item.getPrice()));

            lowStockIndicator.setVisibility(item.isLowStock() ? View.VISIBLE : View.GONE);

            editButton.setOnClickListener(v -> {
                if (listener != null) {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) listener.onEditClick(item, pos);
                }
            });
            deleteButton.setOnClickListener(v -> {
                if (listener != null) {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) listener.onDeleteClick(item, pos);
                }
            });
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) listener.onEditClick(item, pos);
                }
            });
        }

        void bindPartial(InventoryItem item, Bundle payload) {
            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);

            if (payload.getBoolean("name",  false)) {
                itemNameText.setText(item.getName());
            }
            if (payload.getBoolean("desc",  false)) {
                itemDescriptionText.setText(item.getDescription());
            }
            if (payload.getBoolean("cat",   false)) {
                categoryText.setText(item.getCategory());
            }
            if (payload.getBoolean("qty",   false)) {
                quantityText.setText(String.valueOf(item.getQuantity()));
                lowStockIndicator.setVisibility(item.isLowStock() ? View.VISIBLE : View.GONE);
            }
            if (payload.getBoolean("price", false)) {
                priceText.setText(currencyFormat.format(item.getPrice()));
            }
        }
    }
}
