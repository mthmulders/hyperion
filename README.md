# Status
I do no longer actively maintain this code base.
I have archived it; this means you can still inspect it and check it out.
Take care: the ideas on which I have initially built this project may now be outdated.

The code in this repo is NOT production grade anymore.
Its primary purpose has been learning Scala and Akka concepts.
I have been running it in production myself, but I had additional security measures in place back then.

You can in no way hold me liable for damage caused directly or indirectly by using this code; see also the [license](#license).

# Hyperion
Hyperion (Υπερίων) was probably the [god of observation](https://en.wikipedia.org/wiki/Hyperion_%28mythology%29) in ancient Greece.
Fast-forward to the present, Hyperion is a system that observes a ['Smart Meter'](https://en.wikipedia.org/wiki/Smart_meter) system.
Using Hyperion you can read your smart meter from your computer or Raspberry Pi.
To do so, you need a connection between your smart meter to the computer.
The smart meter has a P1 port, which is in fact an RJ11 connector.
You can connect to the serial port or the USB-port as long as your operating system supports reading from it.
On a Raspberry Pi, using a RJ11-to-USB cable, the serial port will become visible on `/dev/ttyUSB0`.

## Design
Hyperion is built as an [actor system](https://en.wikipedia.org/wiki/Actor_model) and implemented with [Akka](http://akka.io/).
The main actors are:
1. The `MeterAgent` creates the `IO(Serial)` extension (from [akka-serial](https://github.com/jodersky/akka-serial));
it starts it and reacts to the messages it sends.
1. When a `Serial.Received` comes in, the `MeterAgent` converts its payload to an UTF-8 string and sends it to the `Collecting` actor.
1. The `Collecting` that collects pieces of data coming in until it thinks there is complete P1 Telegram.
It then parses the content of its buffer using the `P1Parser` (not an actor).
If that succeeds, it sends the telegram using `TelegramReceived` to the `MessageDistributor`.
1. Actors that want to do something with the data subscribe themselves for incoming telegrams by sending `RegisterReceiver` to the `MessageDistributor`.
All actors that are subscribed will receive the `TelegramRecived` messages that the `MessageDistributor` receives from the `Collecting` actor.

There are also some actors that process data from the smart meter:
1. The `RecentHistoryActor` keeps a limited buffer of received telegrams.
Using that buffer, it can report on the last 30 minutes (for example) of energy usage.
Useful for seeing how a certain device influences energy usage.
1. The `DailyHistoryActor` will schedule itself to sleep when it starts, and awake at a fixed time once a day.
It will then wait for one `TelegramRecived`, log some metrics in a database and go to sleep again.
Useful for generating daily/monthly/yearly reports.
1. The `ActualValuesHandlerActor` is created once a WebSocket is connected at its endpoint.
From then on, it will pass on all `TelegramRecived` to that WebSocket.
Useful for having a "life" view on the energy meter.

## Web App
There is also a web front-end to Hyperion.
It is maintained in a [separate Git repository](https://github.com/mthmulders/hyperion-web).

## About the code
Hyperion is written in Scala using the Akka-framework.

[![Circle CI](https://circleci.com/gh/mthmulders/hyperion/tree/master.svg?style=svg)](https://circleci.com/gh/mthmulders/hyperion/tree/master)
[![SonarCloud quality gate](https://sonarcloud.io/api/project_badges/measure?project=mthmulders_hyperion&metric=alert_status)](https://sonarcloud.io/dashboard?id=mthmulders_hyperion)
[![SonarCloud vulnerability count](https://sonarcloud.io/api/project_badges/measure?project=mthmulders_hyperion&metric=vulnerabilities)](https://sonarcloud.io/dashboard?id=mthmulders_hyperion)
[![SonarCloud technical debt](https://sonarcloud.io/api/project_badges/measure?project=mthmulders_hyperion&metric=sqale_index)](https://sonarcloud.io/dashboard?id=mthmulders_hyperion)
[![Mutation testing badge](https://badge.stryker-mutator.io/github.com/mthmulders/hyperion/master)](https://dashboard.stryker-mutator.io/reports/github.com/mthmulders/hyperion/master)
[![Scala Steward badge](https://img.shields.io/badge/Scala_Steward-helping-blue.svg?style=flat&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAQCAMAAAARSr4IAAAAVFBMVEUAAACHjojlOy5NWlrKzcYRKjGFjIbp293YycuLa3pYY2LSqql4f3pCUFTgSjNodYRmcXUsPD/NTTbjRS+2jomhgnzNc223cGvZS0HaSD0XLjbaSjElhIr+AAAAAXRSTlMAQObYZgAAAHlJREFUCNdNyosOwyAIhWHAQS1Vt7a77/3fcxxdmv0xwmckutAR1nkm4ggbyEcg/wWmlGLDAA3oL50xi6fk5ffZ3E2E3QfZDCcCN2YtbEWZt+Drc6u6rlqv7Uk0LdKqqr5rk2UCRXOk0vmQKGfc94nOJyQjouF9H/wCc9gECEYfONoAAAAASUVORK5CYII=)](https://scala-steward.org)


# Getting Hyperion
Pre-built Debian packages can be downloaded from [CircleCI](https://circleci.com/gh/mthmulders/hyperion).
Click on the latest green build and move to the 'Artifacts' tab.
The packages are located in the `deb` folder.

# Hyperion integration tests
The integration tests are written using [Rest Assured](https://github.com/rest-assured/rest-assured) and stored in `./src/it/`.
Test data is injected using an SQL script (found in `./scripts/database`).

To run the tests locally, issue

    sbt \
        -Dconfig.file=app/src/test/resources/environment.conf \
        integrationTest/test


# License
Hyperion is licensed under the MIT License. See the `LICENSE` file for details.