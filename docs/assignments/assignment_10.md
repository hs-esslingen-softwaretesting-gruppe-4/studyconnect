## 10.3 Automated API Test Cases

- Implementation can be found [here](../../backend/src/test/java/de/softwaretesting/studyconnect/controllers/)
- Documentation regarding the API Tests can be found [here](../documentation.md#automated-api-tests)


## 10.5 CI Integration

The automated API-tests run during the `verify phase` of the application and are therefore executed without further configuration in the [Quick Test Backend](../../.github/workflows/test-backend.yml) and the [Build and Test Backend (push/pr main)](../../.github/workflows/test-and-analyze-backend.yml) workflows.
