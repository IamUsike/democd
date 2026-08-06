package com.example.txnmonitor.api;

public class GraphPointResponse {

	private String label;
	private long value;

	public GraphPointResponse() {
	}

	public GraphPointResponse(String label, long value) {
		this.label = label;
		this.value = value;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public long getValue() {
		return value;
	}

	public void setValue(long value) {
		this.value = value;
	}
}
