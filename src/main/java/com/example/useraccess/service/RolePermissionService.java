package com.example.useraccess.service;

import java.util.List;

public interface RolePermissionService {

    List<String> getPermissionsByRoles(List<String> roles);

    int getPermissionCountByRoles(List<String> roles);

    String buildRolesCacheKey(List<String> roles);
}