package com.example.useraccess.dto;

import java.io.Serializable;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request for fetching effective permissions for a user")
public class PermissionRequest implements Serializable {
	private static final long serialVersionUID = 5404915247435182589L;

	@Schema(example = "PROD")
	private String env;

	@Schema(example = "sandeep.g")
	private String userId;

	@Schema(example = "[\"BO_APPEALS_READ\", \"BO_DOCS_VIEW\", \"BO_ADMIN_SUPPORT\"]")
	private List<String> roles;

	public PermissionRequest() {
	}

	public String getEnv() {
		return env;
	}

	public void setEnv(String env) {
		this.env = env;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public List<String> getRoles() {
		return roles;
	}

	public void setRoles(List<String> roles) {
		this.roles = roles;
	}
}