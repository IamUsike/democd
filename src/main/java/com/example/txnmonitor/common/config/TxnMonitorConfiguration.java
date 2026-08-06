package com.example.txnmonitor.common.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(TxnMonitorProperties.class)
public class TxnMonitorConfiguration {
}
