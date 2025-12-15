package com.project.smartinventory;

import com.google.firebase.firestore.Exclude;

/// InventoryItem
/// Data model for an inventory entry used with Firestore.
/// Identity:
///  - documentId: Firestore document ID (not a local SQL primary key).
/// Fields:
///  - name:        Human-readable name of the item.
///  - description: Basic description or notes about the item.
///  - category:    Logical grouping (e.g., "Electronics", "Clothing").
///  - quantity:    Number of units currently in stock.
///  - price:       Cost per unit.
/// Utility:
///  - [#isLowStock()] flags items at or below a simple stock threshold.
public class InventoryItem implements java.io.Serializable {

    // ---- Identity ----
    @Exclude
    private String documentId;   // Firestore doc ID

    // ---- Fields ----
    private String name;
    private String description;
    private String category;
    private int quantity;
    private double price;
    private int lowStockThreshold;

    // ---- Constructors ----

    /**
     * No-arg constructor required for Firestore toObject().
     */
    public InventoryItem() {
    }

    public InventoryItem(String documentId,
                         String name,
                         String description,
                         String category,
                         int quantity,
                         double price,
                         int lowStockThreshold) {
        this.documentId = documentId;
        this.name = name;
        this.description = description;
        this.category = category;
        this.quantity = quantity;
        this.price = price;
        this.lowStockThreshold = lowStockThreshold;
    }

    // ---- Identity accessors ----

    /**
     * Firestore document ID.
     */
    @Exclude
    public String getDocumentId() {
        return documentId;
    }

    @Exclude
    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    /**
     * Backward-compatible alias for old code that used getId()/setId().
     * Prefer getDocumentId()/setDocumentId() in new code.
     */
    @Exclude
    public String getId() {
        return documentId;
    }

    @Exclude
    public void setId(String id) {
        this.documentId = id;
    }

    // ---- Field accessors ----

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    // ---- Utility ----

    public int getLowStockThreshold() {
        return lowStockThreshold;
    }

    public void setLowStockThreshold(int lowStockThreshold) {
        this.lowStockThreshold = lowStockThreshold;
    }

    public boolean isLowStock() {
        int threshold = lowStockThreshold > 0 ? lowStockThreshold : 5; // fallback
        return quantity <= threshold;
    }
}
