package com.project.smartinventory.viewmodel;

import android.app.Application;
import android.content.SharedPreferences;

import androidx.annotation.MainThread;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.project.smartinventory.database.DatabaseHelper;
import com.project.smartinventory.repository.AuthRepository;
import com.project.smartinventory.repository.AuthResult;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * LoginViewModel
 *
 * Handles login/registration state and persists a simple "logged in" flag.
 * Exposes immutable LiveData for the UI:
 *  - isLoading: true while an auth request is in flight
 *  - isSuccess: true when the user is authenticated
 *  - error:     a user-facing message describing the last error (null if none)
 *
 * Implementation details:
 *  - Uses a single-threaded Executor for database I/O work off the main thread.
 *  - Persists auth status in SharedPreferences (simple demo storage).
 *  - Posts results back to LiveData on the main thread via postValue().
 *
 * Notes:
 *  - ViewModels should avoid hardcoding user-facing strings. Consider emitting error codes
 *    (e.g., enum) and mapping to localized strings in the UI layer. For now we keep strings
 *    here for simplicity, but the UI should ideally provide the text.
 *  - Prefer DataStore over SharedPreferences for modern apps.
 */
public class LoginViewModel extends AndroidViewModel {

    // ---- Dependencies ----
    private final DatabaseHelper db;
    private final SharedPreferences prefs;

    // Single background thread for I/O (DB queries, etc.)
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    // ---- Backing state (mutable, internal) ----
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> _isSuccess = new MutableLiveData<>(false);
    private final MutableLiveData<String>  _error     = new MutableLiveData<>(null);

    // ---- Public, immutable LiveData ----
    public final LiveData<Boolean> isLoading = _isLoading;
    public final LiveData<Boolean> isSuccess = _isSuccess;
    public final LiveData<String>  error     = _error;

    public LoginViewModel(Application app) {
        super(app);
        db = new DatabaseHelper(app);
        prefs = app.getSharedPreferences("MyAppPrefs", Application.MODE_PRIVATE);

        // If already logged in, reflect that immediately so UI can navigate.
        if (prefs.getBoolean("isLoggedIn", false)) {
            _isSuccess.setValue(true);
        }
    }

    /**
     * Attempts to authenticate with the given credentials.
     * Work is dispatched to the I/O executor; results are posted to LiveData.
     */

    private final AuthRepository repo = new AuthRepository(getApplication());
    public void login(String u, String p) {
        _isLoading.setValue(true);
        _error.setValue(null); // optional: clears any previous error; UI must ignore null
        io.execute(() -> {
            AuthResult r = repo.login(u, p);
            if (r instanceof AuthResult.Success) {
                prefs.edit().putBoolean("isLoggedIn", true).apply();
                _isSuccess.postValue(true);
                // DO NOT touch _error here
            } else if (r instanceof AuthResult.Error) {
                _error.postValue(((AuthResult.Error) r).code.name()); // e.g., "USER_NOT_FOUND"
            }
            _isLoading.postValue(false);
        });
    }



    /**
     * Registers a new account and logs in on success.
     * Validation (e.g., empty username/password) should be handled by the UI before calling.
     */
    @MainThread
    public void register(String username, String password) {
        _isLoading.setValue(true);
        io.execute(() -> {
            if (db.usernameTaken(username)) {
                // TODO: Emit a code and map to localized string in UI.
                _error.postValue("Username already taken.");
                _isLoading.postValue(false);
                return;
            }
            boolean created = db.registerUser(username, password);
            if (created) {
                prefs.edit().putBoolean("isLoggedIn", true).apply();
                _isSuccess.postValue(true);
                _error.postValue(null);
            } else {
                _error.postValue("Could not create account.");
            }
            _isLoading.postValue(false);
        });
    }

    public void clearError() { _error.setValue(null); }


    /**
     * Clears local "logged in" state.
     * Caller should handle navigation back to LoginActivity.
     */
    @MainThread
    public void logout() {
        prefs.edit().putBoolean("isLoggedIn", false).apply();
        _isSuccess.setValue(false);
    }
}
