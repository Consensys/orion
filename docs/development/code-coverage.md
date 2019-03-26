## Code coverage

We use the jacoco test coverage plugin, which generates coverage data whenever tests are run.

To run the report:
```
./gradlew test jacocoTestReport
```

The report is available at `build/reports/jacoco/test/html/index.html`