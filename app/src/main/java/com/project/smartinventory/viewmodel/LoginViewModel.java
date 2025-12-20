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
    public final MutableLiveData<String> emailFieldError = new MutableLiveData<>(null);
    public final MutableLiveData<String> passwordFieldError = new MutableLiveData<>(null);
    public final MutableLiveData<String> usernameFieldError = new MutableLiveData<>(null);


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

    // ------------ LOGIN: email + password ------------

    @MainThread
    public void login(String email, String password) {
        _error.setValue(null);
        emailFieldError.setValue(null);
        passwordFieldError.setValue(null);

        if (!isValidEmail(email)) {
            emailFieldError.setValue("Enter a valid email");
            return;
        }

        if (password == null || password.isEmpty()) {
            passwordFieldError.setValue("Password is required");
            return;
        }

        _isLoading.setValue(true);

        authRepository.login(email, password, new AuthRepository.AuthCallback() {
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
        usernameFieldError.setValue(null);
        emailFieldError.setValue(null);
        passwordFieldError.setValue(null);

        if (!isValidDisplayUsername(username)) {
            usernameFieldError.setValue("Use 3–20 chars: letters, numbers, _ or -");
            return;
        }

        if (!isValidEmail(email)) {
            emailFieldError.setValue("Enter a valid email");
            return;
        }

        String pwErr = passwordRuleError(password);
        if (pwErr != null) {
            passwordFieldError.setValue(pwErr);
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

    //
    private String passwordRuleError(String password) {
        if (password == null || password.isEmpty()) return "Password is required";
        if (password.length() < 8) return "Use at least 8 characters";

        boolean hasLetter  = password.matches(".*[A-Za-z].*");
        boolean hasNumber  = password.matches(".*\\d.*");
        boolean hasSpecial = password.matches(".*[!@#$%^&*].*");

        if (!hasLetter)  return "Add at least 1 letter";
        if (!hasNumber)  return "Add at least 1 number";
        if (!hasSpecial) return "Add 1 special: !@#$%^&*";

        return null;
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

    private boolean isValidDisplayUsername(String username) {
        if (username == null) return false;
        String u = username.trim();
        if (u.isEmpty()) return false;
        // allow letters/numbers/underscore/dash, 3-20 chars
        return u.matches("^[A-Za-z0-9_\\-]{3,20}$");
    }

    private boolean isValidEmail(String email) {
        if (email == null) return false;
        String e = email.trim();
        if (e.isEmpty()) return false;
        return android.util.Patterns.EMAIL_ADDRESS.matcher(e).matches();
    }

    private boolean isValidPassword(String password) {
        if (password == null) return false;
        if (password.isEmpty()) return false;
        return password.length() >= 8;
    }
}
