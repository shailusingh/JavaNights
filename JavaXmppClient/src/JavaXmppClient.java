
public class JavaXmppClient implements Runnable {

	public static String host = "host";
	public static int port = 5222;
	public static String service = "hostname";
	public static String user = null;
	public static String pwd = null;

	public void run() {

		try {

			XmppManager xmppManager = XmppManager.getInstance();
			xmppManager.setHost(host);
			xmppManager.setPort(port);
			xmppManager.setService(host);
			xmppManager.setUsername(user);
			xmppManager.setPassword(pwd);

			System.out.println("XmppManager going to start.");

			xmppManager.init();

			Thread.currentThread().sleep(1000);

		} catch (Exception e) {
			e.printStackTrace();
			System.out
					.println("Connection Initalization with xmpp server failed with exception msg -"
							+ e.getMessage());

		}

	}

	public static void main(String a[]) {
		host = a[0];
		service = a[0]; // host and service are same
		user = a[1];
		pwd = a[2];

		if (null == a[0] || null == a[1] || null == pwd) {
			System.out
					.println("USAGE: java JavaXmppClient <host> <user> <pwd>");
			System.exit(-1);
		}

		System.out.println("---THIS BEHAVE AS A LISTINING  USER (" + user
				+ ")---");

		JavaXmppClient xmppMobileClient = new JavaXmppClient();
		Thread thread = new Thread(xmppMobileClient);
		thread.start();
	}
}
