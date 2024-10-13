import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.io.IOException;
import java.util.LinkedList;

/*
 * SenderThread monitors send queues and sends packets whenever there are packets in the queues
 */
public class SenderThread extends Thread {
	private final DatagramSocket sock;
	private final LinkedList<GUDPEndPoint> senderList;
	private boolean runFlag = true;
	private boolean debug = true;
	private GUDPSocket.drop senderDrop;

	public SenderThread(DatagramSocket sock, LinkedList<GUDPEndPoint> senderList, GUDPSocket.drop senderDrop) {
		this.sock = sock;
		this.senderList = senderList;
		this.senderDrop = senderDrop;
	}

	public void stopSenderThread() {
		this.runFlag = false;
	}

	@Override
	public void run() {

		synchronized (senderList) {
			if (senderList.isEmpty()) {
				try {
					senderList.wait();
				} catch (InterruptedException e) {
				}
			}
		}

		while (runFlag) {
			synchronized (senderList) {
				for (GUDPEndPoint endPoint : senderList) {
					FSMSender(endPoint);
					if (endPoint.getFinished()) {
						if (endPoint.isEmptyQueue()) {
							senderList.remove(endPoint);
						}
					}
				}
				boolean allEmpty = true;
				for (GUDPEndPoint endPoint : senderList) {
					if (!endPoint.isEmptyQueue()) {
						allEmpty = false;
						break;
					}
				}
				if (senderList.isEmpty()) {
					senderList.notifyAll();
				} else if (allEmpty) {
					try {
						senderList.wait();
					} catch (InterruptedException e) {
						System.out.println("Sender thread interrupted");
					}
				}
				if (!runFlag) {
					senderList.notifyAll();
				}
			}
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	/* public void run() */

	public void FSMSender(GUDPEndPoint endPoint) {
		/*
		 * FSM for GBN Sender
		 * INIT: Do nothing. Progress to WAIT.
		 * WAIT: If the queue is not empty, progress to SEND. Otherwise, notify other
		 * threads
		 * SEND: Send packets while the window is not full.
		 * Move to WAIT after iterating through the senderList
		 * RCV: The actual packet reception is done by the ReceiverThread. Here you deal
		 * with the timer.
		 * Also progress to SEND since you probably have more sending window available.
		 * TIMEOUT: Resend packets and restart the timer.
		 * If maximum retransmission, terminate the SendThread, which should
		 * trigger the program to terminate (assuming you monitor it in send and finish
		 * methods).
		 *
		 * NOTE: You do not need to synchronize senderList in this method since it
		 * should have already been synchronized
		 * by other methods that called this method.
		 * /*
		 * DatagramPacket udppacket;`
		 * switch (endPoint.getState()) {
		 * case INIT:
		 * case WAIT:
		 * case SEND:
		 * case RCV:
		 * case TIMEOUT:
		 * case DEFAULT:
		 * }
		 */
		switch (endPoint.getState()) {
			case INIT:
				endPoint.setState(GUDPEndPoint.endPointState.WAIT);
				break;

			case WAIT:
				if (!endPoint.isEmptyQueue()) {
					endPoint.setState(GUDPEndPoint.endPointState.SEND);
				} else {
					synchronized (senderList) {
						senderList.notifyAll();
					}
				}
				break;

			case SEND:
				while (endPoint.getNextseqnum() < endPoint.getBase() + endPoint.getWindowSize()
						&& endPoint.getNextseqnum() <= endPoint.getLast()) {
					GUDPPacket packet = endPoint.getPacket(endPoint.getNextseqnum());
					try {
						DatagramPacket udppacket = packet.pack();
						sock.send(udppacket);
					} catch (IOException e) {
						e.printStackTrace();
					}
					if (endPoint.getBase() == endPoint.getNextseqnum()) {
						endPoint.startTimer();
					}
					endPoint.setNextseqnum(endPoint.getNextseqnum() + 1);
				}
				endPoint.setState(GUDPEndPoint.endPointState.WAIT);
				break;

			case TIMEOUT:
				if (endPoint.getRetry() >= GUDPEndPoint.MAX_RETRY) {
					stopSenderThread();
					senderList.notifyAll();
					break;
				} else {
					endPoint.startTimer();
					endPoint.setRetry(endPoint.getRetry() + 1);
					for (int i = endPoint.getBase(); i < endPoint.getNextseqnum(); i++) {
						GUDPPacket packet = endPoint.getPacket(i);
						try {
							DatagramPacket udppacket = packet.pack();
							sock.send(udppacket);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
				endPoint.setState(GUDPEndPoint.endPointState.WAIT);
				break;

			case RCV:
				if (endPoint.getBase() == endPoint.getNextseqnum()) {
					endPoint.stopTimer();
					endPoint.setRetry(0);
				} else {
					endPoint.stopTimer();
					endPoint.startTimer();
				}
				endPoint.setState(GUDPEndPoint.endPointState.SEND);
				break;

			default:
				break;
		}
	}

} /* public void FSMSender(GUDPEndPoint endPoint) */

/* public class SenderThread */
