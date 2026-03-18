package com.example.useraccess.dto;

import java.io.Serializable;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request for fetching permissions for a single AD role")
public class RolePermissionRequest implements Serializable{

	private static final long serialVersionUID = -7307451484432973669L;

	@Schema(example = "PROD")
	private String env;

	@Schema(example = "BO_DOCS_VIEW")
	private String roleName;

	public RolePermissionRequest() {
	}

	public String getEnv() {
		return env;
	}

	public void setEnv(String env) {
		this.env = env;
	}

	public String getRoleName() {
		return roleName;
	}

	public void setRoleName(String roleName) {
		this.roleName = roleName;
	}
}