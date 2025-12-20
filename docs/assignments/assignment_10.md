## 10.3 Automated API Test Cases

- Implementation can be found [here](../../backend/src/test/java/de/softwaretesting/studyconnect/controllers/)
- Documentation regarding the API Tests can be found [here](../documentation.md#automated-api-tests)


## 10.5 CI Integration

The automated API tests (controller unit tests during the Maven `test` phase and integration tests during the `verify` phase) are all executed by the standard `mvn verify` lifecycle and are therefore run without further configuration in the [Quick Test Backend](../../.github/workflows/test-backend.yml) and the [Build and Test Backend (push/pr main)](../../.github/workflows/test-and-analyze-backend.yml) workflows.
