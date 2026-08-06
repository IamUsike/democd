package com.example.txnmonitor.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "txnmonitor")
public class TxnMonitorProperties {

	private EvaluationMode evaluation = new EvaluationMode();
	private RuleEvaluationCache ruleEvaluation = new RuleEvaluationCache();

	public EvaluationMode getEvaluation() {
		return evaluation;
	}

	public void setEvaluation(EvaluationMode evaluation) {
		this.evaluation = evaluation;
	}

	public RuleEvaluationCache getRuleEvaluation() {
		return ruleEvaluation;
	}

	public void setRuleEvaluation(RuleEvaluationCache ruleEvaluation) {
		this.ruleEvaluation = ruleEvaluation;
	}

	public boolean isAsyncEvaluation() {
		return evaluation.isAsync();
	}

	public static class EvaluationMode {

		private Mode mode = Mode.sync;

		public Mode getMode() {
			return mode;
		}

		public void setMode(Mode mode) {
			this.mode = mode;
		}

		public boolean isAsync() {
			return mode == Mode.async;
		}
	}

	public static class RuleEvaluationCache {

		private boolean enabled;

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}
	}

	public enum Mode {
		sync,
		async
	}
}
