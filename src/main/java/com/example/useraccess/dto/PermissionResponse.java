package com.example.useraccess.dto;

import java.io.Serializable;
import java.util.List;

public class PermissionResponse implements Serializable {

	private static final long serialVersionUID = -7242094619393395257L;
	private String userId;
	private String env;
	private int permissionCount;
	private List<PermissionItem> permissions;

	public PermissionResponse() {
	}

	public PermissionResponse(String userId, String env, int permissionCount, List<PermissionItem> permissions) {
		this.userId = userId;
		this.env = env;
		this.permissionCount = permissionCount;
		this.permissions = permissions;
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

	public List<PermissionItem> getPermissions() {
		return permissions;
	}

	public void setPermissions(List<PermissionItem> permissions) {
		this.permissions = permissions;
	}
}