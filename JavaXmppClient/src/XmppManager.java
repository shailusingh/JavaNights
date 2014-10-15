import java.util.Date;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.ping.PingFailedListener;
import org.jivesoftware.smack.ping.packet.Ping;
import org.jivesoftware.smack.proxy.ProxyInfo;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.ping.PingManager;

/**
 * This class will be used as manager to interact with any Xmpp server 
 * Some methods can be invoked on this instance
 *
 */

public class XmppManager implements ConnectionListener, PacketListener, PingFailedListener, MessageListener {

	private static ConnectionConfiguration config;
	private static XMPPConnection connection;

	private static XmppManager xmppManager;

	private static String username;
	private static String password;

	private static ChatManager chatManager;

	private String host;
	private int port;
	private String service;

	/*
	 * Initialize the required static classes
	 */
	static {
		try {
			Class.forName("org.jivesoftware.smack.ReconnectionManager");
			Class.forName(ServiceDiscoveryManager.class.getName());
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/*
	 * Singleton
	 */
	public static XmppManager getInstance() {
		if (null == xmppManager) {
			xmppManager = new XmppManager();
		}
		return xmppManager;
	}

	/*
	 * This will do all the initialization
	 */
	private void initalize() {
		try {
			
			host = getHost();
			port = getPort();
			service = getService();

			System.out.println("Connecting to server host[" + host + "] post[" + port + "]");

			if (null == config) {
				config = new ConnectionConfiguration(host, port, service, ProxyInfo.forDefaultProxy());

				config.setReconnectionAllowed(true);
				config.setSASLAuthenticationEnabled(true);
				config.setSecurityMode(SecurityMode.disabled);

			}

			if (null == connection) 
				connection = new XMPPConnection(config);

			SASLAuthentication.supportSASLMechanism("PLAIN", 0);

			if (!connection.isConnected())
				connection.connect();

			Thread.currentThread().sleep(1000 * 2);

			System.out.println("Connected with server [" + host + "]: " + connection.isConnected());

			connection.addConnectionListener(this);

			chatManager = connection.getChatManager();

			if (!connection.isAuthenticated()) {
				System.out.println("Connected but Not authenticated, so try to login with ID:" + username);

				connection.login(username, password);

				Thread.currentThread().sleep(1000 * 2);

				System.out.println("Success logged in with ID [" + username + "] ");

			}

			Presence presence = new Presence(Presence.Type.available);
			connection.sendPacket(presence);

			System.out.println("Presence sent.");

			connection.addPacketListener(this, new PacketTypeFilter(Packet.class));

			System.out.println("All done connected ok...");

			try {

				//tweak it as per your need
				PingManager.getInstanceFor(connection).ping(host, 1000 * 60 * 1);

				//Thread.currentThread().sleep(1000 * 60 * 1);

			} catch (Exception e) {
				e.printStackTrace();
			}

		} catch (XMPPException xe) {
			System.out.println("XMPPException - " + xe.getMessage());

		} catch (Exception e) {
			System.out.println("Exception - " + e.getMessage());

		}

	}

	/*
	 * this will register itself to the host
	 */
	public void selfRegister() throws XMPPException, Exception {
		if (null == connection) {
			initalize();
		}

		if (connection != null && connection.isConnected()) {

			System.out.println("Connected.");

			connection.getAccountManager().createAccount(username, password);

			Thread.currentThread().sleep(1000 * 2);
			System.out.println("User registered with Id - " + username);
		}

	}

	// initalization
	public void init() throws XMPPException {
		initalize();
	}

	// connection
	public void closeConnection() throws XMPPException {
		if (connection != null && connection.isConnected()) {
			connection.disconnect();
			System.out.println(String.format("User disconnected."));
		}
	}

	// sendmessage
	public void sendMessage(String loginID, String message) throws XMPPException {

		if (null == connection || !connection.isConnected() || !connection.isAuthenticated()) {
			System.out.println("Server not rechable or not connected, trying to reconnect.");
			initalize();
		}

		if (null != chatManager) {
			Chat chat = chatManager.createChat(loginID + "@" + service, this);

			chat.sendMessage(message);

			System.out.println("Mesage Sent [" + message + "]");

		} else {
			System.out.println("Cannot send message to user [" + loginID + "]");
		}

	}

	// send broadcast message to all
	public void sendBroadcastMessage(String message) throws XMPPException {

		if (null != connection && connection.isAuthenticated()) {
			Message msg = new Message();
			msg.setTo(service + "/announce/all");
			msg.setBody(message);
			connection.sendPacket(msg);
			System.out.println("Broadcast message sent - " + message);
		} else {
			System.out.println("Cannot broadcast message to all.");
		}

	}

	// here we get the stanzas
	@Override
	public void processPacket(Packet packet) {
		try {
			if (null != packet) {

				System.out.println("RCVD XML - " + packet.toXML());

				// do a ping back to the server
				if (packet instanceof Ping) {
					Ping ping = new Ping();
					ping.setTo(packet.getFrom());
					ping.setType(Type.RESULT);
					connection.sendPacket(ping);
					// send the presence also
					Presence presence = new Presence(Presence.Type.available);
					connection.sendPacket(presence);

				}

				// this is the message that we are waiting for
				if (packet instanceof Message) {
					Message message = (Message) packet;
					if (null != message) {
						String body = message.getBody();
						String from = message.getFrom();
						System.out.println("RCVD MSG -  " + body);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// In case ping failed
	@Override
	public void pingFailed() {
		System.out.println("Ping to server failed at time : " + new Date());
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}

	@Override
	public void processMessage(Chat chat, Message packet) {
		try {
			if (null != packet) {
				if (packet instanceof Message) {
					Message message = (Message) packet;
					if (null != message) {
						String body = message.getBody();
						String from = message.getFrom();
						System.out.println("MSG from messagelistener -  " + body);

					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	public void connectionClosed() {
		try {
			System.out.println("ConnectionClosed.");
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	public void connectionClosedOnError(Exception arg0) {
		try {
			System.out.println("ConnectionClosedOnError - " + arg0.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	public void reconnectingIn(int arg0) {
		try {
			System.out.println("ReconnectingIn in [" + arg0 + "] secs");
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	public void reconnectionFailed(Exception arg0) {
		try {
			System.out.println("ReconnectionFailed - " + arg0.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	public void reconnectionSuccessful() {
		try {
			System.out.println("ReconnectionSuccessful.");
			// reconnected, so authenticate it again
			if (!connection.isAuthenticated())
				connection.login(username, password);

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public void setService(String service) {
		this.service = service;
	}

	public static String getUsername() {
		return username;
	}

	public static void setUsername(String username) {
		XmppManager.username = username;
	}

	public static String getPassword() {
		return password;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getService() {
		return service;
	}

	public static void setPassword(String password) {
		XmppManager.password = password;
	}

}