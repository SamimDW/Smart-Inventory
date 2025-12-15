package com.project.smartinventory.viewmodel;

import android.app.Application;
import android.content.SharedPreferences;

import androidx.annotation.MainThread;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.project.smartinventory.auth.AuthRepository;

/**
 * LoginViewModel
 *
 * Login:    username + password
 * Register: username + email + password
 *
 * Uses AuthRepository for all Firebase work.
 * ViewModel only:
 *  - validates input
 *  - manages LiveData state
 *  - updates SharedPreferences
 */
public class LoginViewModel extends AndroidViewModel {

    private final AuthRepository authRepository;
    private final SharedPreferences prefs;

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> _isSuccess = new MutableLiveData<>(false);
    private final MutableLiveData<String>  _error     = new MutableLiveData<>(null);

    public final LiveData<Boolean> isLoading = _isLoading;
    public final LiveData<Boolean> isSuccess = _isSuccess;
    public final LiveData<String>  error     = _error;

    public LoginViewModel(Application app) {
        super(app);

        this.authRepository = new AuthRepository();
        this.prefs = app.getSharedPreferences("MyAppPrefs", Application.MODE_PRIVATE);

        boolean alreadyLoggedIn =
                authRepository.hasCurrentUser() ||
                        prefs.getBoolean("isLoggedIn", false);

        if (alreadyLoggedIn) {
            _isSuccess.setValue(true);
        }
    }

    // ------------ LOGIN: username + password ------------

    @MainThread
    public void login(String username, String password) {
        _error.setValue(null);

        if (isValidUsername(username) || isValidPassword(password)) {
            _error.setValue("LOGIN_FAILED");
            return;
        }

        _isLoading.setValue(true);

        authRepository.login(username, password, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess() {
                prefs.edit().putBoolean("isLoggedIn", true).apply();
                _isLoading.postValue(false);
                _isSuccess.postValue(true);
            }

            @Override
            public void onError(String errorCode) {
                _isLoading.postValue(false);
                _error.postValue(errorCode);
            }
        });
    }

    // ------------ REGISTER: username + email + password ------------

    @MainThread
    public void register(String username, String email, String password) {
        _error.setValue(null);

        if (isValidUsername(username) || !isValidEmail(email) || isValidPassword(password)) {
            _error.setValue("REGISTER_FAILED");
            return;
        }

        _isLoading.setValue(true);

        authRepository.register(username, email, password, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess() {
                prefs.edit().putBoolean("isLoggedIn", true).apply();
                _isLoading.postValue(false);
                _isSuccess.postValue(true);
            }

            @Override
            public void onError(String errorCode) {
                _isLoading.postValue(false);
                _error.postValue(errorCode);
            }
        });
    }

    public void clearError() {
        _error.setValue(null);
    }

    @MainThread
    public void logout() {
        authRepository.logout();
        prefs.edit().putBoolean("isLoggedIn", false).apply();
        _isSuccess.setValue(false);
    }

    // ---------- Validation ----------

    private boolean isValidUsername(String username) {
        if (username == null) return true;
        String u = username.trim();
        if (u.isEmpty()) return true;
        return u.contains(" ") || u.contains("@");
    }

    private boolean isValidEmail(String email) {
        if (email == null) return false;
        String e = email.trim();
        if (e.isEmpty()) return false;
        return e.contains("@") && e.contains(".");
    }

    private boolean isValidPassword(String password) {
        if (password == null) return true;
        String p = password.trim();
        if (p.isEmpty()) return true;
        return p.length() < 6;
    }
}
