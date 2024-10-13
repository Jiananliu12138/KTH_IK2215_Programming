# KTH_IK2215_Programming
Overview
---
Guaranteed UDP (GUDP) enables reliable transport over UDP. It is based on the Go-Back-N (GBN) protocol.

The protocol uses automatic repeat request (ARQ), leveraging acknowledgements and timeouts to ensure reliability.

Sliding window flow control allows multiple packets to be in flight simultaneously, enabling asynchronous communication.

Unlike TCP, GUDP is not connection-oriented.

Main tasks
--------------
Part 1: GUDPSocket.java

1. Implement core functionalities, including send(), receive(), finish(), and close() methods.
2. Assume SenderThread.java and ReceiverThread.java are already implemented as described in the
comments in the files.

Part 2: SenderThread.java
1. Implement the send thread that monitors send queues and sends packets based on the GBN protocol
when there are packets in the queues
2. Assume GUDPSocket.java and ReceiverThread.java are implemented as described in the comments in
the files
3. ReceiverThread.java handles ACK for the GBN sender, and it also calls FSMSender after it removes all ACKed
packet from the send buffer

Test
---
You can use VSSend and VSRecv to test various situations that occur during the transmission process to verify whether the program you have written is correct.
