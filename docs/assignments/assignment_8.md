## Assignment 8

### Exercise 8.1

Documentation of the checkstyle setup can be found in the [documentation](../documentation.md#checkstyle). <br>
All further information about the checkstyle configuration can be found [here](../checkstyle-configuration.md)

#### Opinion about Linters
Linters can be very useful in larger software projects. They can be used to enforce common naming conventions in projects, ensure consistent file formatting across different IDEs and developers working on the project. They can also help new developers with learning common conventions regarding a programming language and programming in general.


On the other hand, linters have to be configured carefully, at least when they are able to fail the build, as otherwise frustrations can arise when a build keeps failing for minor reasons like a file not ending with a new line.

 Linters can also easily flag false positives, for example the [StudyconnectApplication class](../../backend/src/main/java/de/softwaretesting/studyconnect/StudyconnectApplication.java) gets flagged for a HideUtilityClassConstructor warning as checkstyle recognizes the class as a helper class which shouldn't have a public constructor. Excluding such errors from showing up can be tedious and not worth the time.

 On first execution linters tend to produce a lot of errors and warnings when they are not set up in the beginning of a project. In our project checkstyle generated over 230 errors and warnings, with most of them being simple formatting inconsistencies. For this reason, we configured spotless as a formatter, running spotless in the validation phase before checkstyle to drastically reduce those warnings.

 In a nutshell, Linters can be very useful, but for our smaller Studyconnect project we are sceptical whether checkstyle will be very useful and worth the time to set it up with an adapted configuration for the project. For the most part, those errors are marked by tools/extensions like sonarqube in the IDE as well, and together with an auto-formatter like spotless in the CI or pre-commit-hook provide the same result. Also, the generated reports by checkstyle have to be read periodically, which we think is likely to get skipped when in a rush to finish a feature or assignment.

 ### Exercise 8.2
 Documentation of the JaCoCo setup can be found in the [documentation](../documentation.md#jacoco). <br>
All further information about the JaCoCo configuration can be found [here](../jacoco-configuration.md)

### Opinion about JaCoCo
JaCoCo is a powerful tool for ensuring sufficient test coverage in a project and for identifying which parts of the code are still untested.

When integrating coverage checks, it is important to keep in mind that certain components such as DTOs, mappers, or other simple data structures that typically do not require dedicated tests, should be excluded from the coverage scope. This ensures accurate and meaningful results rather than artificially lowering the overall coverage.

In our opinion, JaCoCo is an excellent tool for gaining insight into how much of the codebase is covered by the existing tests.
However, if used without proper configuration, it can lead to overtesting. For example: writing unnecessary tests solely to satisfy coverage thresholds instead of focusing on meaningful test scenarios.
