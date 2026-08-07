package com.example.txnmonitor.rule;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ensures {@code rule_configs} has the four default rules when Flyway is off
 * ({@code spring.flyway.enabled=false} + {@code ddl-auto=update} creates an empty table).
 *
 * <p>Matches the seed rows in {@code V3__create_rule_configs.sql}. Idempotent:
 * existing rows are left alone; missing types are inserted; live rule beans are
 * re-applied from DB after seeding.
 */
@Component
@Order(100)
public class RuleConfigSeeder implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(RuleConfigSeeder.class);

	private final RuleConfigRepository ruleConfigRepository;
	private final List<ConfigurableRule> configurableRules;

	public RuleConfigSeeder(
			RuleConfigRepository ruleConfigRepository,
			List<ConfigurableRule> configurableRules) {
		this.ruleConfigRepository = ruleConfigRepository;
		this.configurableRules = configurableRules;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		int inserted = 0;
		inserted += seedIfAbsent(amountThreshold());
		inserted += seedIfAbsent(velocity());
		inserted += seedIfAbsent(newPayee());
		inserted += seedIfAbsent(dailyLimit());

		for (ConfigurableRule rule : configurableRules) {
			ruleConfigRepository.findById(rule.ruleType()).ifPresent(rule::applyConfig);
		}

		if (inserted > 0) {
			log.info("Seeded {} missing rule_configs row(s) (Flyway seed fallback)", inserted);
		} else {
			log.debug("rule_configs already populated ({} rows)", ruleConfigRepository.count());
		}
	}

	private int seedIfAbsent(RuleConfig config) {
		if (ruleConfigRepository.existsById(config.getRuleType())) {
			return 0;
		}
		ruleConfigRepository.save(config);
		return 1;
	}

	private static RuleConfig amountThreshold() {
		RuleConfig c = new RuleConfig();
		c.setRuleType("AMOUNT_THRESHOLD");
		c.setName("Amount Threshold Rule");
		c.setDescription(
				"Fires when a single transaction amount exceeds the configured threshold. Used to detect unusually large individual transfers.");
		c.setEnabled(true);
		c.setAmountThreshold(new BigDecimal("10000.00"));
		c.setUpdatedAt(LocalDateTime.now());
		return c;
	}

	private static RuleConfig velocity() {
		RuleConfig c = new RuleConfig();
		c.setRuleType("VELOCITY");
		c.setName("Velocity Rule");
		c.setDescription(
				"Fires when an account exceeds the maximum number of transactions within a rolling time window. Detects rapid bursts of activity.");
		c.setEnabled(true);
		c.setVelocityMaxTransactions(5);
		c.setVelocityWindowMinutes(10);
		c.setUpdatedAt(LocalDateTime.now());
		return c;
	}

	private static RuleConfig newPayee() {
		RuleConfig c = new RuleConfig();
		c.setRuleType("NEW_PAYEE");
		c.setName("New Payee Rule");
		c.setDescription(
				"Fires when an account sends money to a payee it has never transacted with before. Flags first-contact transfers for review.");
		c.setEnabled(true);
		c.setUpdatedAt(LocalDateTime.now());
		return c;
	}

	private static RuleConfig dailyLimit() {
		RuleConfig c = new RuleConfig();
		c.setRuleType("DAILY_LIMIT");
		c.setName("Daily Limit Rule");
		c.setDescription(
				"Fires when the cumulative DEBIT amount for an account on a calendar day exceeds the configured limit. Catches high-volume daily outflow.");
		c.setEnabled(true);
		c.setDailyLimit(new BigDecimal("50000.00"));
		c.setUpdatedAt(LocalDateTime.now());
		return c;
	}
}
