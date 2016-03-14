This document lists some of the known issues that may be around in Hyperion.

- It seems that when the Core disappears (crashes), the Meter Agent is not able to find it back.
Restarting the Meter Agent resolves this.
Probably caused by the fact that the `StateTimeout` is enqueued for later delivery.
- The Meter Agent does not (yet) verify the checksum that is part of each P1 telegram.
According to the DSMR spec, it should be CRC16 checksum.