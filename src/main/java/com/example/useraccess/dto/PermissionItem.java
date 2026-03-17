package com.example.useraccess.dto;

import java.io.Serializable;

public class PermissionItem implements Serializable{
	private static final long serialVersionUID = -2153352187903442073L;
	private String name;
	private String source;
	private String riskLevel;

	public PermissionItem() {
	}

	public PermissionItem(String name, String source, String riskLevel) {
		this.name = name;
		this.source = source;
		this.riskLevel = riskLevel;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getRiskLevel() {
		return riskLevel;
	}

	public void setRiskLevel(String riskLevel) {
		this.riskLevel = riskLevel;
	}
}