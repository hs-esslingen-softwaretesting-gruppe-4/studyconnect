## Exercise 9.1 & 9.2
Documentation regarding the workflows can be found [here](../documentation.md#ci-pipeline). <br>

## Exercise 9.3
SonarQube integration is handled in the [Build and Test Backend (push/pr main)](../../.github/workflows/test-and-analyze-backend.yml) workflow.
As some of our team members already had the SonarQube extension installed in VS-code, there were only few issues regarding the code.
However SonarQube flagged a significantly lower test coverage than our setup with jacoco, so we added some tests, especially regarding the SecurityConfig in the profile `prod` for production.
SonarQube did not like the security setup for the `test` and `dev` setup, these issues had to be manually reviewed and marked as solved.
