In this subproject you'll find a helper object, `hyperion.TestCli`.
You can use it to run/debug Hyperion without connecting it to a physical meter.

Start it with

    sbt \
      -Dconfig.file=app/src/test/resources/environment.conf \
      testApp/"runMain hyperion.TestCli"

Every time you press `[enter]`, a random telegram is emitted.
Stop it with `[ctrl]-c`.