## 10.1 API Documentation

- The OpenAPI documetation can be found [here](../../backend/openapi.yaml)
- by following the instructions in the documentation found [here](../documentation.md#starten-des-backends)
- Ensure the environment variable *SPRING_PROFILES_ACTIVE* is set to **dev** in the `.env` file derived from [this example](../../backend/.env.example), if it is not already the default. This file also defines the **PORT** used by the application (default: **8080**)

### Accessing the backend

- Once the backend is running, it can be accessed via a web browser at: [Weblink](http://localhost:8080/api/) *Note:* Browsers only send GET requests. For more advanced testing, use tools such as **Thunder Client** (VS Code extension) or **curl** from the command line
- After the `/api/` prefix, you can append any available endpoint path. For example,
`/api/users` returns a list of users (or an empty list if none exist).
- All available endpoints and request/response details are defined in the OpenAPI documentation.

## 10.3 Automated API Test Cases

- Implementation can be found [here](../../backend/src/test/java/de/softwaretesting/studyconnect/controllers/)
- Documentation regarding the API Tests can be found [here](../documentation.md#automated-api-tests)


## 10.5 CI Integration

The automated API tests (controller unit tests during the Maven `test` phase and integration tests during the `verify` phase) are all executed by the standard `mvn verify` lifecycle and are therefore run without further configuration in the [Quick Test Backend](../../.github/workflows/test-backend.yml) and the [Build and Test Backend (push/pr main)](../../.github/workflows/test-and-analyze-backend.yml) workflows.


## 10.2 Manual API Testing

Use the template in [assignment_10_2_manual_api_testing.md](./assignment_10_2_manual_api_testing.md) to document your manual tests (tool used, successful request, invalid request, and short summary with screenshots or CLI logs).
