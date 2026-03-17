package com.example.useraccess.repository;
import java.util.List;

public interface PermissionRepositoryCustom {

    int getEffectivePermissionCount(String userId, List<String> roles);

    List<String> getEffectivePermissions(String userId, List<String> roles);
}