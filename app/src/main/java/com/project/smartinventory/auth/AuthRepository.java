package com.project.smartinventory.auth;

import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * AuthRepository
 *
 * Handles all Firebase Auth + Firestore logic:
 *  - login with email + password
 *  - register with username + email + password
 *
 * Firestore layout:
 *   usernames/{username} : { uid, email }
 *   users/{uid}          : { username, email }
 */
public class AuthRepository {

    private static final String TAG = "AuthRepository";

    private final FirebaseAuth auth;
    private final FirebaseFirestore db;

    public interface AuthCallback {
        void onSuccess();
        void onError(String errorCode);
    }

    public AuthRepository() {
        this.auth = FirebaseAuth.getInstance();
        this.db   = FirebaseFirestore.getInstance();
    }

    public boolean hasCurrentUser() {
        return auth.getCurrentUser() != null;
    }

    public void logout() {
        auth.signOut();
    }

    // ------------ LOGIN: email + password ------------
    public void login(String email, String password, AuthCallback callback) {
        String trimmedEmail = email.trim();

        auth.signInWithEmailAndPassword(trimmedEmail, password)
                .addOnCompleteListener(authTask -> {
                    if (authTask.isSuccessful()) {
                        callback.onSuccess();
                    } else {
                        Exception e = authTask.getException();

                        if (e instanceof FirebaseAuthInvalidUserException) {
                            callback.onError("USER_NOT_FOUND");
                        } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
                            callback.onError("WRONG_PASSWORD");
                        } else {
                            callback.onError("LOGIN_FAILED");
                        }
                    }
                });
    }

    // ------------ REGISTER: username + email + password ------------
    public void register(String username, String email, String password, AuthCallback callback) {
        String trimmedUsername = username.trim();
        String trimmedEmail    = email.trim();

        // 1) Create Firebase Auth user with email
        auth.createUserWithEmailAndPassword(trimmedEmail, password)
                .addOnCompleteListener((Task<AuthResult> createTask) -> {

                    if (!createTask.isSuccessful()) {
                        Exception e = createTask.getException();
                        log("createUserWithEmailAndPassword FAILED: " + e);

                        if (e instanceof FirebaseAuthUserCollisionException) {
                            callback.onError("EMAIL_IN_USE");
                        } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
                            callback.onError("INVALID_EMAIL");
                        } else {
                            callback.onError("REGISTER_FAILED");
                        }
                        return;
                    }

                    if (createTask.getResult() == null || createTask.getResult().getUser() == null) {
                        log("createUserWithEmailAndPassword returned null user");
                        callback.onError("REGISTER_FAILED");
                        return;
                    }

                    String uid = createTask.getResult().getUser().getUid();
                    log("Firebase user created, uid=" + uid);

                    // 2) Write user profile document (username is just profile data now)
                    DocumentReference userRef = db.collection("users").document(uid);
                    UserDoc userDoc = new UserDoc(trimmedUsername, trimmedEmail);

                    userRef.set(userDoc)
                            .addOnSuccessListener(unused -> {
                                log("users/" + uid + " written OK");
                                callback.onSuccess();
                            })
                            .addOnFailureListener(e -> {
                                log("write user doc FAILED: " + e);

                                //If Firestore write fails, delete the Auth user
                                if (auth.getCurrentUser() != null) {
                                    auth.getCurrentUser().delete()
                                            .addOnCompleteListener(t -> log("cleanup delete user result=" + t.isSuccessful()));
                                }

                                callback.onError("REGISTER_FAILED");
                            });
                });
    }


    // ---------- Firestore POJOs ----------
    public static class UserDoc {
        public String username;
        public String email;

        public UserDoc(String username, String email) {
            this.username = username;
            this.email = email;
        }
    }

    private void log(String msg) {
        Log.d(TAG, msg);
    }
}
