package com.example.useraccess.dto;

import java.io.Serializable;

public class SummaryResponse implements Serializable {

	private static final long serialVersionUID = -6545756706784071321L;
	private String userId;
	private String env;
	private int permissionCount;

	public SummaryResponse() {
	}

	public SummaryResponse(String userId, String env, int permissionCount) {
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