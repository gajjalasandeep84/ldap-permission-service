package com.example.useraccess.dto;

import java.io.Serializable;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request for fetching user permissions")
public class PermissionRequest implements Serializable {

	private static final long serialVersionUID = 5404915247435182589L;

	@Schema(example = "SIT_AUTO_RO_2")
	private String userId;

	@Schema(example = "SIT")
	private String env;

	@Schema(example = "[\"ROLE_CSR_II\", \"PlanAdmin\"]")
	private List<String> roles;

	public PermissionRequest() {
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

	public List<String> getRoles() {
		return roles;
	}

	public void setRoles(List<String> roles) {
		this.roles = roles;
	}
}