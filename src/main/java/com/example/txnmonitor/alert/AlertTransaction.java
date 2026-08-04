package com.example.txnmonitor.alert;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "alert_transactions")
@IdClass(AlertTransaction.AlertTransactionId.class)
public class AlertTransaction {

	@Id
	@Column(name = "alert_id", nullable = false)
	private Long alertId;

	@Id
	@Column(name = "transaction_id", nullable = false)
	private Long transactionId;

	public AlertTransaction() {
	}

	public AlertTransaction(Long alertId, Long transactionId) {
		this.alertId = alertId;
		this.transactionId = transactionId;
	}

	public Long getAlertId() {
		return alertId;
	}

	public void setAlertId(Long alertId) {
		this.alertId = alertId;
	}

	public Long getTransactionId() {
		return transactionId;
	}

	public void setTransactionId(Long transactionId) {
		this.transactionId = transactionId;
	}

	public static class AlertTransactionId implements Serializable {

		private Long alertId;
		private Long transactionId;

		public AlertTransactionId() {
		}

		public AlertTransactionId(Long alertId, Long transactionId) {
			this.alertId = alertId;
			this.transactionId = transactionId;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			AlertTransactionId that = (AlertTransactionId) o;
			return Objects.equals(alertId, that.alertId) && Objects.equals(transactionId, that.transactionId);
		}

		@Override
		public int hashCode() {
			return Objects.hash(alertId, transactionId);
		}
	}
}
