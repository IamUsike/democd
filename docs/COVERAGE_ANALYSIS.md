# Code Coverage Analysis - Transaction Monitoring & Alerts Dashboard

## Current Status

**Project**: Spring Boot 3.4.4 + Java 21  
**Test Framework**: JUnit 5 + Mockito  
**Database**: H2 (in-memory for tests)  

## Coverage Challenges

### JaCoCo Compatibility Issue
JaCoCo 0.8.11 (the latest stable version at time of writing) has issues with Java 21's class file format (version 69). When instrumentation is enabled, it fails to instrument:
- Java 21 system library classes (java.sql.*, java.base modules)
- Dynamically generated classes (Mockito-generated mocks using ByteBuddy)

This makes traditional JaCoCo-based coverage analysis difficult for Java 21 projects.

## Recommended Solutions

### 1. **IDE-Level Coverage (Recommended for Now)**
Your IDE (IntelliJ IDEA) has built-in coverage analysis:
- **Run → Run with Coverage** (or `Ctrl+Alt+F10`)
- View coverage directly in the IDE without external tools
- Supports Java 21 natively
- No configuration required

### 2. **Future: JaCoCo 1.0.x (When Released)**
JaCoCo is working on full Java 21 support. Once released:
```xml
<!-- Update pom.xml when JaCoCo 1.0.0+ is available -->
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>1.0.0</version>
    <!-- ... configuration ... -->
</plugin>
```

### 3. **Alternative: OpenClover**
OpenClover supports Java 21 and provides similar coverage features:
```xml
<plugin>
    <groupId>org.openclover</groupId>
    <artifactId>clover-maven-plugin</artifactId>
    <version>4.5.0</version>
</plugin>
```

## Test Suites Available

### Unit Tests (No Integration Context)
Run: `mvn test -Dtest=*Test`

**Test Files**:
- `src/test/java/com/example/txnmonitor/rule/AmountThresholdRuleTest.java`
- `src/test/java/com/example/txnmonitor/rule/RuleEngineTest.java`
- `src/test/java/com/example/txnmonitor/transaction/TransactionServiceTest.java`
- `src/test/java/com/example/txnmonitor/alert/AlertServiceTest.java`

### Integration Tests (With Spring Context)
Run: `mvn test -Dtest=*ControllerTest`

**Test Files**:
- `src/test/java/com/example/txnmonitor/api/TransactionControllerTest.java`
- `src/test/java/com/example/txnmonitor/api/AlertControllerTest.java`

## Quick Coverage Check Steps

### In IntelliJ IDEA
1. Open any test class (e.g., `AmountThresholdRuleTest`)
2. Right-click → **Run 'TestClassName' with Coverage**
3. View results in the **Coverage** tool window
4. Drill down into specific files or packages for details

### Via Command Line (No Coverage Tool)
```bash
# Just run tests to verify they pass
cd C:\Users\Administrator\IdeaProjects\democd
.\mvnw test

# View test report in browser
start target/site/surefire-report.html
```

## Coverage Targets (Best Practices)

For this MVP project:
- **Rule Engine**: 90%+ (critical business logic)
- **Services**: 80%+ (business operations)
- **Controllers**: 70%+ (API contracts)
- **Repositories**: 40%+ (data access, mostly mocked in tests)
- **Utilities**: 60%+

## Files to Prioritize

1. **High Priority** (Core Logic)
   - `Rule.java` and implementations
   - `RuleEngine.java`
   - `AlertService.java`
   - `TransactionService.java`

2. **Medium Priority** (API Integration)
   - `AlertController.java`
   - `TransactionController.java`

3. **Lower Priority** (Data Access)
   - Repository implementations (mocked in tests)
   - DTOs and mappers

## Next Steps

1. **Run coverage in IDE** to get visibility
2. **Monitor test results** as features are added
3. **When JaCoCo 1.0.x releases**, integrate it into CI/CD pipeline
4. **Document coverage thresholds** in the project README

---

**Last Updated**: August 5, 2026  
**Java Version**: 21  
**Framework**: Spring Boot 3.4.4

