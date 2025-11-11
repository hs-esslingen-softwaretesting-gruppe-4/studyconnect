## Exercise 5

### Task 5.1


### Task 5.2
Implementation of the scenarios and steps located under /backend/test/java/de/softwaretesting/studyconnect/steps/*Steps.java

### Task 5.3

**Question: What do you think makes more sense when to execute the BDD test: execute always together with your unit test cases or shall BDD/acceptance tests be treated differently, e.g., execute less frequent?**

As unit tests only test isolated classes or functions, they execute really fast, often within just a few milliseconds. For this reason, unit tests can be run at any time in the development/deployment process, for example before every push and merge/request, but also locally before any commit.

Behaviour driven development tests on the other hand involve testing multiple moduls of the app and have a broader test scope in general, and are therefore slowler and more prone to errors. They are used to validate that a feature is implemented.  
Therefore it makes sense to run those tests in a CI/CD pipeline, for example on a pull-request (this makes the most sense when working on feature-branches), or on demand by a developer or QA at any given time to verify that a feature is completely implemented.

Acceptance Test verify that the software meets the requirements and is ready for production. This requires an even broader test scope than BDD tests, making them more ressource and time consuming. Therefore, these tests should be run before release, for example on a pull-request to main/master in a CI/CD pipeline.


