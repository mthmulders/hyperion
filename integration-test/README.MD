# Hyperion integration tests
These tests are written using [Rest Assured](https://github.com/rest-assured/rest-assured).
Test data is injected using an SQL script (found in `../scripts/database`).

To run the tests locally, issue

    sbt \
        -Dconfig.file=app/src/test/resources/application.conf \
        integrationTest/test
