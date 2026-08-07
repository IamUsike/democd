package com.example.txnmonitor.rule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

@ExtendWith(MockitoExtension.class)
class RuleConfigSeederTest {

	@Mock
	private RuleConfigRepository ruleConfigRepository;

	@Mock
	private ConfigurableRule amountRule;

	@Mock
	private ConfigurableRule velocityRule;

	private RuleConfigSeeder seeder;

	@BeforeEach
	void setUp() {
		when(amountRule.ruleType()).thenReturn("AMOUNT_THRESHOLD");
		when(velocityRule.ruleType()).thenReturn("VELOCITY");
		seeder = new RuleConfigSeeder(ruleConfigRepository, List.of(amountRule, velocityRule));
	}

	@Test
	void seedsAllFourWhenTableEmpty() {
		when(ruleConfigRepository.existsById(any())).thenReturn(false);
		when(ruleConfigRepository.findById("AMOUNT_THRESHOLD")).thenReturn(Optional.empty());
		when(ruleConfigRepository.findById("VELOCITY")).thenReturn(Optional.empty());
		when(ruleConfigRepository.save(any(RuleConfig.class))).thenAnswer(inv -> inv.getArgument(0));

		seeder.run(new DefaultApplicationArguments());

		ArgumentCaptor<RuleConfig> captor = ArgumentCaptor.forClass(RuleConfig.class);
		verify(ruleConfigRepository, org.mockito.Mockito.times(4)).save(captor.capture());
		assertEquals(4, captor.getAllValues().size());
		assertTrue(captor.getAllValues().stream().anyMatch(c -> "AMOUNT_THRESHOLD".equals(c.getRuleType())));
		assertTrue(captor.getAllValues().stream().anyMatch(c -> "VELOCITY".equals(c.getRuleType())));
		assertTrue(captor.getAllValues().stream().anyMatch(c -> "NEW_PAYEE".equals(c.getRuleType())));
		assertTrue(captor.getAllValues().stream().anyMatch(c -> "DAILY_LIMIT".equals(c.getRuleType())));
	}

	@Test
	void doesNotOverwriteExistingRows() {
		when(ruleConfigRepository.existsById(any())).thenReturn(true);
		when(ruleConfigRepository.findById("AMOUNT_THRESHOLD")).thenReturn(Optional.empty());
		when(ruleConfigRepository.findById("VELOCITY")).thenReturn(Optional.empty());
		when(ruleConfigRepository.count()).thenReturn(4L);

		seeder.run(new DefaultApplicationArguments());

		verify(ruleConfigRepository, never()).save(any());
	}

	@Test
	void reappliesConfigToLiveBeans() {
		RuleConfig stored = new RuleConfig();
		stored.setRuleType("AMOUNT_THRESHOLD");
		stored.setEnabled(false);
		stored.setName("Amount Threshold Rule");
		stored.setDescription("desc");

		when(ruleConfigRepository.existsById(any())).thenReturn(true);
		when(ruleConfigRepository.findById("AMOUNT_THRESHOLD")).thenReturn(Optional.of(stored));
		when(ruleConfigRepository.findById("VELOCITY")).thenReturn(Optional.empty());
		when(ruleConfigRepository.count()).thenReturn(4L);

		seeder.run(new DefaultApplicationArguments());

		verify(amountRule).applyConfig(stored);
		verify(velocityRule, never()).applyConfig(any());
	}
}
