# Documentation of the P1 interface.

Note that lots of information in this file is based on the ["P1 Companion Standard"](http://files.domoticaforum.eu/uploads/Smartmetering/DSMR%20v4.0%20final%20P1.pdf).
Other devices, such as gas, water or thermal warmth metering devices, can be connected to the metering system.
There are four channels available for such devices.

Once the so-called "P1 port" is connected and supplied with power, the electricity metering system will start to send data readouts every ten seconds in a "P1 telegram".

## P1 Telegram structure
Data inside the P1 Telegram is layed out according to the following structure:

    /X X X 5 Identification CR LF CR LF Data ! CRC CR LF

Or, formatted:

    /X X X 5 Identification

    Data
    !CRC

Here, the CRC is a checksum value over the preceding characters in the data message.

Each "data object" (indicated with `Data` above) conforms to this structure:

    OBIS Reference(value)

## OBIS References
The OBIS Reference describes what kind of data this line describes; between brackets is the actual value for that data object.

|OBIS Reference|Description                                                                                                                                      |
|-------------:|-------------------------------------------------------------------------------------------------------------------------------------------------|
|0-0:96.1.1    |Serial number for the electricity metering system, a.k.a. "equipment identifier"                                                                 |
|1-0:1.8.1     |Actual meter reading for electricity delivered under "low tariff", in 0,001 kWh                                                                  |
|1-0:1.8.2     |Actual meter reading for electricity delivered under "normal tariff", in 0,001 kWh                                                               |
|1-0:2.8.1     |Actual meter reading for electricity produced under "low tariff", in 0,001 kWh                                                                   |
|1-0:2.8.2     |Actual meter reading for electricity produced under "normal tariff", in 0,001 kWh                                                                |
|0-0:96.14.0   |Tariff indication (`0001` for _low tariff_, `0002` for _normal tariff_)                                                                          |
|1-0:1.7.0     |Current electricity power consumed, in W                                                                                                         |
|1-0:2.7.0     |Current electricity power produced, in W                                                                                                         |
|0-0:17.0.0    |Electricity threshold in kW (not sure yet what this means)                                                                                       |
|0-0:96.3.10   |Electricity switch position (not sure yet what this means)                                                                                       |
|0-0:96.13.1   |Reserved for sending text messages codes (8 numeric positions)                                                                                   |
|0-0:96.13.0   |Reserved for sending text messages codes (1024 character positions)                                                                              |
|0-n:24.1.0    |Type of other device on channel `n`. `3` appears to be a gas metering system                                                                     |
|0-n:96.1.0    |Serial number of the connected device on channel `n`, a.k.a. "equipment identifier"                                                              |
|0-n:24.2.1    |When type is `3`: Last hourly value (temperature converted) of gas delivered to client in m3, including decimal values and including capture time|
|0-n:24.4.0    |When type is `3`: If present, position of the valve (`1` for _on_, other options are _off_ or _released_)                                        |

## Checksum
The checksum is to be interpreted as an integer value, in other words it should be prefixed with `0x`.
This checksum is calculated as a CRC16 over the entire message: everything from `/` to (and probably including) `!`.

