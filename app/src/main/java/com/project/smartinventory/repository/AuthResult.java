package com.project.smartinventory.repository;


public sealed interface AuthResult permits AuthResult.Success, AuthResult.Error {
    final class Success implements AuthResult {}
    final class Error implements AuthResult {
        public enum Code { USER_NOT_FOUND, USERNAME_TAKEN, CREATE_FAILED, UNKNOWN }
        public final Code code;
        public Error(Code code) { this.code = code; }
    }
}

