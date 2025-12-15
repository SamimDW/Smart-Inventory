package com.project.smartinventory.repository;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.project.smartinventory.InventoryItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Repository class for managing inventory data in Firebase Firestore.
 * This class handles all Create, Read, Update, and Delete (CRUD) operations
 * for {@link InventoryItem} objects associated with the currently authenticated user.
 *
 * <p>It abstracts the underlying Firebase implementation, providing a clean API
 * for the ViewModel to interact with the data layer. Operations are asynchronous
 * and use a callback mechanism ({@link InventoryCallback}) to return results or errors.
 *
 * <p>Each user's inventory is stored in a dedicated sub-collection under their
 * unique user ID in Firestore: `/users/{userId}/inventory/{itemId}`.
 */
public class InventoryRepository {

    private static final String TAG = "InventoryRepository";

    private final FirebaseAuth auth;
    private final FirebaseFirestore db;

    public interface InventoryCallback<T> {
        void onSuccess(T result);
        void onError(String errorCode);
    }

    public InventoryRepository() {
        this.auth = FirebaseAuth.getInstance();
        this.db   = FirebaseFirestore.getInstance();
    }

    /** Helper: inventory collection for the current user. */
    private CollectionReference inventoryCollection() {
        if (auth.getCurrentUser() == null) {
            throw new IllegalStateException("No logged-in user for inventory");
        }
        String uid = auth.getCurrentUser().getUid();
        return db.collection("users")
                .document(uid)
                .collection("inventory");
    }

    /** Add a new inventory item. Returns the new documentId in callback. */
    public void addItem(@NonNull InventoryItem item, @NonNull InventoryCallback<String> callback) {
        inventoryCollection()
                .add(item)
                .addOnSuccessListener(docRef -> {
                    String docId = docRef.getId();
                    Log.d(TAG, "addItem success, id=" + docId);
                    callback.onSuccess(docId);
                })
                .addOnFailureListener(e -> {
                    Log.d(TAG, "addItem FAILED: " + e);
                    callback.onError("ADD_FAILED");
                });
    }

    /** Update an existing item using its documentId. */
    public void updateItem(@NonNull InventoryItem item, @NonNull InventoryCallback<Void> callback) {
        String docId = item.getDocumentId();
        if (docId == null || docId.trim().isEmpty()) {
            callback.onError("MISSING_ID");
            return;
        }

        inventoryCollection()
                .document(docId)
                .set(item)
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "updateItem success, id=" + docId);
                    callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    Log.d(TAG, "updateItem FAILED: " + e);
                    callback.onError("UPDATE_FAILED");
                });
    }

    /** Delete an item by documentId. */
    public void deleteItem(@NonNull String documentId, @NonNull InventoryCallback<Void> callback) {
        inventoryCollection()
                .document(documentId)
                .delete()
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "deleteItem success, id=" + documentId);
                    callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    Log.d(TAG, "deleteItem FAILED: " + e);
                    callback.onError("DELETE_FAILED");
                });
    }

    /** Load all items for the current user. */
    public void getAllItems(@NonNull InventoryCallback<List<InventoryItem>> callback) {
        inventoryCollection()
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<InventoryItem> list = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot) {
                        InventoryItem item = doc.toObject(InventoryItem.class);
                        if (item != null) {
                            item.setDocumentId(doc.getId());
                            list.add(item);
                        }
                    }
                    Log.d(TAG, "getAllItems loaded " + list.size() + " items");
                    callback.onSuccess(list);
                })
                .addOnFailureListener(e -> {
                    Log.d(TAG, "getAllItems FAILED: " + e);
                    callback.onError("LOAD_FAILED");
                });
    }
}
