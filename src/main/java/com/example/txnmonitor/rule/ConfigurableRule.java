package com.example.txnmonitor.rule;

/**
 * Extended contract for rules that can be enabled/disabled and reconfigured at runtime
 * via the operator UI without restarting the application.
 *
 * <p>Implementing this interface alongside {@link Rule} lets {@link RuleEngine} honour the
 * enabled flag and lets {@link RuleConfigService} apply DB-sourced configuration without
 * coupling to concrete rule types (no switch statement needed).
 *
 * <p>Adding a new configurable rule = one new class implementing {@code ConfigurableRule} —
 * do not change this interface, {@link Rule}, or {@link RuleEngine}.
 */
public interface ConfigurableRule extends Rule {

    /** Unique rule type identifier, matching the {@code rule_type} primary key in rule_configs. */
    String ruleType();

    /** Whether this rule should currently fire during {@link RuleEngine} evaluation. */
    boolean isEnabled();

    /**
     * Apply the given DB-sourced configuration to this rule instance.
     * Called both at startup (from {@link RuleEngineConfig}) and at runtime
     * (from {@link RuleConfigService}) when an operator saves changes.
     *
     * <p>Implementations must be thread-safe: use {@code volatile} fields or
     * synchronised setters so in-flight evaluations see consistent state.
     *
     * @param config the persisted config row for this rule type
     */
    void applyConfig(RuleConfig config);
}
