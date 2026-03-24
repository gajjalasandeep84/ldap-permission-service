package com.example.useraccess.dto;

public class PermissionCountResponse {

	private String userId;

	private String env;

	private int permissionCount;

	public PermissionCountResponse() {

	}

	public PermissionCountResponse(String userId, String env, int permissionCount) {

		this.userId = userId;

		this.env = env;

		this.permissionCount = permissionCount;

	}

	public String getUserId() {

		return userId;

	}

	public void setUserId(String userId) {

		this.userId = userId;

	}

	public String getEnv() {

		return env;

	}

	public void setEnv(String env) {

		this.env = env;

	}

	public int getPermissionCount() {

		return permissionCount;

	}

	public void setPermissionCount(int permissionCount) {

		this.permissionCount = permissionCount;

	}

}
