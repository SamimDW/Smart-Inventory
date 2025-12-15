package com.project.smartinventory.auth;

import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * AuthRepository
 *
 * Handles all Firebase Auth + Firestore logic:
 *  - login with username + password (mapped to email)
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

    // ------------ LOGIN: username + password ------------

    public void login(String username, String password, AuthCallback callback) {
        String trimmedUsername = username.trim();
        DocumentReference usernameRef = db.collection("usernames").document(trimmedUsername);

        usernameRef.get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                log("username lookup FAILED: " + task.getException());
                callback.onError("LOGIN_FAILED");
                return;
            }

            DocumentSnapshot doc = task.getResult();
            if (doc == null || !doc.exists()) {
                log("username not found: " + trimmedUsername);
                callback.onError("USER_NOT_FOUND");
                return;
            }

            String email = doc.getString("email");
            if (email == null || email.trim().isEmpty()) {
                log("email missing in username doc for " + trimmedUsername);
                callback.onError("LOGIN_FAILED");
                return;
            }

            log("username mapped to email=" + email);

            auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener((Task<AuthResult> authTask) -> {

                        if (authTask.isSuccessful()) {
                            log("signInWithEmailAndPassword success");
                            callback.onSuccess();
                        } else {
                            Exception e = authTask.getException();
                            log("signInWithEmailAndPassword FAILED: " + e);

                            if (e instanceof FirebaseAuthInvalidUserException) {
                                callback.onError("USER_NOT_FOUND");
                            } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
                                callback.onError("WRONG_PASSWORD");
                            } else {
                                callback.onError("LOGIN_FAILED");
                            }
                        }
                    });
        });
    }

    // ------------ REGISTER: username + email + password ------------

    public void register(String username, String email, String password, AuthCallback callback) {
        String trimmedUsername = username.trim();
        String trimmedEmail    = email.trim();

        // 1) Check if username already exists
        DocumentReference usernameRef = db.collection("usernames").document(trimmedUsername);
        usernameRef.get().addOnCompleteListener(usernameTask -> {
            if (!usernameTask.isSuccessful()) {
                log("username check FAILED: " + usernameTask.getException());
                callback.onError("REGISTER_FAILED");
                return;
            }

            if (usernameTask.getResult() != null && usernameTask.getResult().exists()) {
                log("username already taken: " + trimmedUsername);
                callback.onError("USERNAME_TAKEN");
                return;
            }

            // 2) Create Firebase Auth user with email
            auth.createUserWithEmailAndPassword(trimmedEmail, password)
                    .addOnCompleteListener((Task<AuthResult> createTask) -> {
                        if (!createTask.isSuccessful()) {
                            Exception e = createTask.getException();
                            log("createUserWithEmailAndPassword FAILED: " + e);

                            if (e instanceof FirebaseAuthUserCollisionException) {
                                callback.onError("EMAIL_IN_USE");
                            } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
                                callback.onError("REGISTER_FAILED");
                            } else {
                                callback.onError("REGISTER_FAILED");
                            }
                            return;
                        }

                        String uid = createTask.getResult().getUser().getUid();
                        log("Firebase user created, uid=" + uid);

                        // 3) Write user document
                        DocumentReference userRef = db.collection("users").document(uid);
                        UserDoc userDoc = new UserDoc(trimmedUsername, trimmedEmail);

                        userRef.set(userDoc)
                                .addOnSuccessListener(unused -> {
                                    log("users/" + uid + " written OK");

                                    // 4) Write username index
                                    UsernameDoc usernameDoc = new UsernameDoc(uid, trimmedEmail);
                                    usernameRef.set(usernameDoc)
                                            .addOnSuccessListener(unused2 -> {
                                                log("usernames/" + trimmedUsername + " written OK");
                                                callback.onSuccess();
                                            })
                                            .addOnFailureListener(e -> {
                                                log("write username index FAILED: " + e);
                                                callback.onError("REGISTER_FAILED");
                                            });
                                })
                                .addOnFailureListener(e -> {
                                    log("write user doc FAILED: " + e);
                                    callback.onError("REGISTER_FAILED");
                                });
                    });
        });
    }

    // ---------- Firestore POJOs ----------

    public static class UserDoc {
        public String username;
        public String email;

        public UserDoc() { }

        public UserDoc(String username, String email) {
            this.username = username;
            this.email = email;
        }
    }

    public static class UsernameDoc {
        public String uid;
        public String email;

        public UsernameDoc() { }

        public UsernameDoc(String uid, String email) {
            this.uid = uid;
            this.email = email;
        }
    }

    private void log(String msg) {
        Log.d(TAG, msg);
    }
}
