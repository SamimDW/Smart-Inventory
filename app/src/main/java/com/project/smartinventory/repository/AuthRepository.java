// AuthRepository.java
package com.project.smartinventory.repository;

import android.content.Context;

import com.project.smartinventory.database.DatabaseHelper;

/**
 * AuthRepository
 *
 * Repository layer responsible for handling authentication logic.
 * In a real-world application this would communicate with:
 *  - A remote API (via Retrofit/OkHttp, etc.),
 *  - A local database (e.g., Room) for cached credentials,
 *  - Or a combination of both.
 *
 * For now this implementation is a simple stub that validates against
 * hardcoded credentials.
 */
// AuthRepository.java
public class AuthRepository {
    private final DatabaseHelper db;
    public AuthRepository(Context ctx) { this.db = new DatabaseHelper(ctx); }

    public AuthResult login(String username, String password) {
        boolean ok = db.userExists(username, password);
        return ok ? new AuthResult.Success()
                : new AuthResult.Error(AuthResult.Error.Code.USER_NOT_FOUND);
    }

    public AuthResult register(String username, String password) {
        if (db.usernameTaken(username))
            return new AuthResult.Error(AuthResult.Error.Code.USERNAME_TAKEN);
        boolean created = db.registerUser(username, password);
        return created ? new AuthResult.Success()
                : new AuthResult.Error(AuthResult.Error.Code.CREATE_FAILED);
    }
}
