package com.project.smartinventory;

import java.io.Serializable;

/**
 * InventoryItem
 *
 * A simple data model representing an item in the inventory.
 * This class is {@link Serializable}, making it suitable for passing
 * between Android components (e.g., via Intents or Bundles).
 *
 * Fields:
 *  - id:        Unique identifier for the item (typically database primary key).
 *  - name:      Human-readable name of the item.
 *  - description: Detailed explanation or notes about the item.
 *  - category:  Logical grouping (e.g., "Electronics", "Clothing").
 *  - quantity:  Number of units currently in stock.
 *  - price:     Cost per unit.
 *
 * Utility:
 *  - Includes {@link #isLowStock()} to flag items below a stock threshold.
 */
public class InventoryItem implements Serializable {

    // ---- Fields ----
    private int id;
    private String name;
    private String description;
    private String category;
    private int quantity;
    private double price;

    // ---- Constructors ----

    /** Default no-arg constructor (needed for frameworks like Firebase, Room, etc.) */
    public InventoryItem() {}

    /**
     * Full constructor to quickly create an item.
     *
     * @param id          unique identifier
     * @param name        item name
     * @param description item description
     * @param category    category grouping
     * @param quantity    stock level
     * @param price       unit price
     */
    public InventoryItem(int id, String name, String description,
                         String category, int quantity, double price) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.quantity = quantity;
        this.price = price;
    }

    // ---- Getters / Setters ----

    /** @return unique identifier */
    public int getId() { return id; }
    /** @param id unique identifier */
    public void setId(int id) { this.id = id; }

    /** @return item name */
    public String getName() { return name; }
    /** @param name item name */
    public void setName(String name) { this.name = name; }

    /** @return item description */
    public String getDescription() { return description; }
    /** @param description detailed description */
    public void setDescription(String description) { this.description = description; }

    /** @return category */
    public String getCategory() { return category; }
    /** @param category logical grouping (e.g., Electronics) */
    public void setCategory(String category) { this.category = category; }

    /** @return stock quantity */
    public int getQuantity() { return quantity; }
    /** @param quantity stock quantity */
    public void setQuantity(int quantity) { this.quantity = quantity; }

    /** @return unit price */
    public double getPrice() { return price; }
    /** @param price cost per unit */
    public void setPrice(double price) { this.price = price; }

    // ---- Utility Methods ----

    /**
     * Determines if this item is considered "low stock".
     *
     * @return true if quantity is less than or equal to 5, otherwise false
     * (threshold can be adjusted as needed).
     */
    public boolean isLowStock() {
        return quantity <= 5;
    }
}
