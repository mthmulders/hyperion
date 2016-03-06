# Hyperion
Hyperion (Υπερίων) was probably the god of observation in ancient Greece.
Fast-forward to 2015, Hyperion is a system that observes a ['Smart Meter'](https://en.wikipedia.org/wiki/Smart_meter) system.
Using Hyperion you can read your smart meter from your computer or Raspberry Pi.
You will need a way to connect your smart meter to the computer.
The smart meter has a P1 port, which is in fact RJ11.
You can connect to the serial port or the USB-port as long as your operating system supports reading from it.
On a Raspberry Pi, using a RJ11-to-USB cable, the serial port will become visible on `/dev/ttyUSB0`.

## Architecture
Hyperion consists of two main parts: the 'meter agent' and the 'core'.

### Meter Agent
The meter agent is the part that connects to the Smart Meter and tries to understand the packets (telegrams) it sends.
It then submits those to the core.

### Core
The core is the part that periodically stores meter readings and makes them available using a simple REST API.

## About the code
Hyperion is written in Scala using the Akka-framework.

Build status: [![Circle CI](https://circleci.com/gh/mthmulders/hyperion/tree/master.svg?style=svg)](https://circleci.com/gh/mthmulders/hyperion/tree/master)

Code quality: [![Codacy Badge](https://api.codacy.com/project/badge/grade/13a2d2b6f4fc43c1bdcd7f5c0306bd4f)](https://www.codacy.com/app/m-th-mulders/hyperion)

# Getting Hyperion
Pre-built Debian packages can be downloaded from [CircleCI](https://circleci.com/gh/mthmulders/hyperion).
Click on the latest green build and move to the 'Artifacts' tab.
The packages are located in the `deb` folder.

# License
Hyperion is licensed under the MIT License. See the `LICENSE` file for details.