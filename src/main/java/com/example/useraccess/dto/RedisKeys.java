package com.example.useraccess.dto;

public final class RedisKeys {

	private RedisKeys() {
	}

	public static String userSummary(String userId) {
		return "user:" + userId.toLowerCase() + ":summary";
	}

	public static String userPermissions(String userId) {
		return "user:" + userId.toLowerCase() + ":permissions";
	}

	public static String rolePermissions(String roleName) {
		return "role:" + roleName.toLowerCase() + ":permissions";
	}
}