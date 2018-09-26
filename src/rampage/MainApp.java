/**INOG
 * @author abba5aghaei
 */
package rampage;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class MainApp extends Application {
	private Stage stage;
	private Scene scene;
	private AnchorPane root;
	private FXMLLoader loader;
	private Socket socket;
	private DataInputStream in;
	private DataOutputStream out;
	private Thread client;
	private Thread nc;
	private Enumeration<InetAddress> net_address;
	private InetAddress address;
	private boolean is_connect;
	private boolean checker_on_loop = true;
	private boolean has_network = false;
	private String[] userInformation;
	private boolean logined;
	private boolean type;
	private boolean hamburgerPressed;
	private List<String> userStatus;
	private List<String> myContacts;
	@FXML
	private Button send;
	@FXML
	private Label waiting;
	@FXML
	private Label status;
	@FXML
	private Label username;
	@FXML
	private Label contactName;
	@FXML
	private ProgressIndicator waiter;
	@FXML
	private TextArea chat;
	@FXML
	private TextField pm;
	@FXML
	private TextField port;
	@FXML
	private TextField host;
	@FXML
	private TextField usernameField;
	@FXML
	private PasswordField passwordField;
	@FXML
	private PasswordField passwordConfirmField;
	@FXML
	private TextField emailField;
	@FXML
	private ToggleButton signlog;
	@FXML
	private ListView<String> users;
	@FXML
	private Hyperlink connect;
	@FXML
	private Hyperlink disconnect;
	@FXML
	private AnchorPane accountPane;

	@Override
	public void start(Stage primaryStage) {
		try {
			loader = new FXMLLoader();
			loader.setLocation(getClass().getResource("Graphic.fxml"));
			Image icon = new Image(getClass().getResourceAsStream("rampage.png"));
			root = (AnchorPane) loader.load();
			scene = new Scene(root);
			stage = primaryStage;
			stage.setScene(scene);
			stage.setResizable(false);
			stage.setTitle("Rampage");
			stage.getIcons().add(icon);
			stage.centerOnScreen();
			stage.show();
			is_connect = false;
			stage.addEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST, new EventHandler<WindowEvent>() {
				@Override
				public void handle(WindowEvent we) {
					exit();
				}
			});
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}

	public static void main(String[] args) {
		launch(args);
	}

	public void initialize() {
		logined = false;
		type = true;
		hamburgerPressed = false;
		userStatus = new ArrayList<String>();
		myContacts = new ArrayList<String>();
		try {
			File file = new File("config.abs");
			if (file.exists()) {
				BufferedReader reader = new BufferedReader(new FileReader(file));
				userInformation = new String[3];
				for (int i = 0; i < 3; i++)
					userInformation[i] = reader.readLine();
				logined = true;
				username.setText(userInformation[0]);
				String contact;
				while ((contact = reader.readLine()) != null)
					myContacts.add(contact);
				for (String element : myContacts)
					users.getItems().add(element);
				reader.close();
			}
		} catch (IOException e) {
		}
		if (logined) {
			connect.setDisable(false);
		} else {
			accountPane.setVisible(true);
			connect.setDisable(true);
		}
		check();
		checkThread();
	}

	public void getListOfUsers() {
		Thread connector = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					URL url = new URL("http://localhost:8080/RampageUsers/users");
					Map<String, Object> params = new LinkedHashMap<>();
					params.put("value", "1");
					StringBuilder postData = new StringBuilder();
					for (Map.Entry<String, Object> param : params.entrySet()) {
						if (postData.length() != 0)
							postData.append('&');
						postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
						postData.append('=');
						postData.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
					}
					byte[] postDataBytes = postData.toString().getBytes("UTF-8");
					HttpURLConnection conn = (HttpURLConnection) url.openConnection();
					conn.setRequestMethod("POST");
					conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
					conn.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
					conn.setDoOutput(true);
					conn.getOutputStream().write(postDataBytes);
					BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
					String packet = "";
					users.getItems().clear();
					myContacts.clear();
					userStatus.clear();
					while ((packet = in.readLine()) != null) {
						myContacts.add(packet.substring(0, packet.length() - 2));
						userStatus.add(packet.substring(packet.length() - 1, packet.length()));
					}
					Platform.runLater(new Runnable() {
						@Override
						public void run() {
							for (int i = 0; i < myContacts.size(); i++) {
								if (userStatus.get(i).equals("0"))
									users.getItems().add(myContacts.get(i));
								else
									users.getItems().add(myContacts.get(i) + " - online");
							}
						}
					});
				} catch (Exception e) {
					Platform.runLater(new Runnable() {
						@Override
						public void run() {
							status.setText("اتصال اینترنت خود را بررسی کنید");
							playAnimation();
						}
					});
				}
			}
		});
		connector.start();
	}

	public void connect() {
		String Host = host.getText();
		int PORT = Integer.parseInt(port.getText());
		String un = username.getText();
		port.setDisable(true);
		host.setDisable(true);
		connect.setDisable(true);
		is_connect = true;
		client = new Thread(new Runnable() {
			@Override
			public synchronized void run() {
				try {
					socket = new Socket(Host, PORT);
					changeView();
					in = new DataInputStream(socket.getInputStream());
					out = new DataOutputStream(socket.getOutputStream());
					out.writeUTF(un);
					String msg = "";
					while (true) {
						msg = in.readUTF();
						chat.appendText("- " + msg + "\r\n");
					}
				} catch (Exception e) {
					System.out.print(e.getMessage());
				}
			}
		});
		client.start();
	}

	public void sendMessage() {
		String message = pm.getText();
		try {
			out.writeUTF(contactName + ":" + message);
			pm.setText("");
			chat.appendText("+" + message + "\r\n");
		} catch (Exception e) {
			status.setText("خطا در اتصال");
			playAnimation();
		}
	}

	protected void changeView() {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				send.setDisable(false);
				chat.setDisable(false);
				pm.setDisable(false);
				disconnect.setDisable(false);
				waiter.setVisible(false);
				waiting.setText("Connected");
			}
		});
	}

	private void playAnimation() {
		FadeTransition ft = new FadeTransition();
		ft.setNode(status);
		ft.setFromValue(100);
		ft.setToValue(0);
		ft.setCycleCount(6);
		ft.play();
	}

	public void disconnect() {
		chat.setDisable(true);
		pm.setDisable(true);
		send.setDisable(true);
		try {
			if (in != null)
				in.close();
			if (out != null)
				out.close();
			if (socket != null)
				if (!socket.isClosed())
					socket.close();
			is_connect = false;
			port.setDisable(false);
			host.setDisable(false);
			connect.setDisable(false);
			disconnect.setDisable(true);
			status.setText("ارتباط قطع شد");
			playAnimation();
		} catch (Exception e) {
			status.setText("خطا در قطع ارتباط");
			playAnimation();
		}
	}

	public void exit() {
		if (is_connect)
			disconnect();
		if (nc != null)
			if (nc.isAlive())
				nc.stop();
		try {
			if (userInformation != null) {
				PrintWriter writer = new PrintWriter(new File("config.abs"));
				for (String element : userInformation)
					writer.println(element);
				for (String element : myContacts)
					writer.println(element);
				writer.close();
			}
		} catch (IOException e) {
		}
		System.exit(0);
	}

	public void check() {
		nc = new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					if (haveNet()) {
						Platform.runLater(new Runnable() {
							@Override
							public void run() {
								waiter.setVisible(false);
								waiting.setText("Connected");
							}
						});
						has_network = true;
						System.out.print("ok");
					}
				}
			}
		});
		nc.start();
	}

	public void checkThread() {
		Thread tc = new Thread(new Runnable() {
			@Override
			public void run() {
				checker_on_loop = true;
				while (checker_on_loop) {
					if (has_network) {
						nc.suspend();
						checker_on_loop = false;
						System.out.print("Thread Closed");
					} else {
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			}
		});
		tc.start();
	}

	private boolean haveNet() {
		try {
			Enumeration<NetworkInterface> net = NetworkInterface.getNetworkInterfaces();
			while (net.hasMoreElements()) {
				net_address = net.nextElement().getInetAddresses();
				while (net_address.hasMoreElements()) {
					address = net_address.nextElement();
					if (!address.isAnyLocalAddress() && !address.isLoopbackAddress() && !address.isSiteLocalAddress())
						if (!address.getHostName().equals(address.getHostAddress()))
							return true;
				}
			}
		} catch (SocketException e) {
			e.printStackTrace();
		}
		return false;
	}

	public void handleEnter(KeyEvent ke) {
		if (ke.getCode().equals(KeyCode.ENTER))
			sendMessage();
	}

	public void handleHamburger() {
		if (hamburgerPressed) {
			accountPane.setVisible(true);
			hamburgerPressed = false;
		} else {
			accountPane.setVisible(false);
			hamburgerPressed = true;
		}
	}

	public void handleOk() {
		if (type == true)
			signup();
		else
			login();
	}

	public void handleCancel() {
		accountPane.setVisible(false);
	}

	public void handleSinglog() {
		if (type == false) {
			signlog.setText("Signup");
			passwordConfirmField.setDisable(false);
			emailField.setDisable(false);
			type = true;
		} else {
			signlog.setText("Login");
			passwordConfirmField.setDisable(true);
			emailField.setDisable(true);
			type = false;
		}
	}

	public void handleList() {
		if (users.getSelectionModel().getSelectedIndex() >= 0)
			contactName.setText(myContacts.get(users.getSelectionModel().getSelectedIndex()));
	}

	public void signup() {
		if (isValidValue()) {
			Thread signuper = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						URL url = new URL("http://localhost:8080/RampageUsers/signup");
						Map<String, Object> params = new LinkedHashMap<>();
						params.put("username", usernameField.getText());
						params.put("password", passwordField.getText());
						params.put("email", emailField.getText());
						StringBuilder postData = new StringBuilder();
						for (Map.Entry<String, Object> param : params.entrySet()) {
							if (postData.length() != 0)
								postData.append('&');
							postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
							postData.append('=');
							postData.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
						}
						byte[] postDataBytes = postData.toString().getBytes("UTF-8");
						HttpURLConnection conn = (HttpURLConnection) url.openConnection();
						conn.setRequestMethod("POST");
						conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
						conn.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
						conn.setDoOutput(true);
						conn.getOutputStream().write(postDataBytes);
						BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
						String packet = in.readLine();
						if (packet.charAt(0) == '1') {
							Platform.runLater(new Runnable() {
								@Override
								public void run() {
									logined = true;
									status.setText(":)");
									playAnimation();
									accountPane.setVisible(false);
									connect.setDisable(false);
									userInformation = new String[3];
									userInformation[0] = usernameField.getText();
									userInformation[1] = passwordField.getText();
									userInformation[2] = emailField.getText();
									username.setText(usernameField.getText());
								}
							});
							getListOfUsers();
						} else if (packet.charAt(0) == '0') {
							Platform.runLater(new Runnable() {
								@Override
								public void run() {
									logined = false;
									status.setText(":(");
									playAnimation();
								}
							});
						} else {
							Platform.runLater(new Runnable() {
								@Override
								public void run() {
									logined = false;
									status.setText("خطا: " + packet);
									playAnimation();
								}
							});
						}
					} catch (Exception e) {
						Platform.runLater(new Runnable() {
							@Override
							public void run() {
								System.out.println(e.getMessage());
								status.setText("خطا در اتصال");
								playAnimation();
							}
						});
					}
				}
			});
			signuper.start();
		} else {
			status.setText("پسوورد یا نام کاربری اشتباه است");
			playAnimation();
		}
	}

	private boolean isValidValue() {
		if (passwordField.getText().equals(passwordConfirmField.getText()))
			return true;
		return false;
	}

	public void login() {
		Thread loginer = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					URL url = new URL("http://localhost:8080/RampageUsers/login");
					Map<String, Object> params = new LinkedHashMap<>();
					params.put("username", usernameField.getText());
					params.put("password", passwordField.getText());
					StringBuilder postData = new StringBuilder();
					for (Map.Entry<String, Object> param : params.entrySet()) {
						if (postData.length() != 0)
							postData.append('&');
						postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
						postData.append('=');
						postData.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
					}
					byte[] postDataBytes = postData.toString().getBytes("UTF-8");
					HttpURLConnection conn = (HttpURLConnection) url.openConnection();
					conn.setRequestMethod("POST");
					conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
					conn.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
					conn.setDoOutput(true);
					conn.getOutputStream().write(postDataBytes);
					BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
					String packet = in.readLine();
					if (packet.charAt(0) == '1') {
						Platform.runLater(new Runnable() {
							@Override
							public void run() {
								logined = true;
								status.setText("وارد شدید");
								playAnimation();
								accountPane.setVisible(false);
								connect.setDisable(false);
								username.setText(usernameField.getText());
								getListOfUsers();
							}
						});
					} else if (packet.charAt(0) == '2') {
						Platform.runLater(new Runnable() {
							@Override
							public void run() {
								logined = false;
								status.setText(":)");
								playAnimation();
							}
						});
					} else {
						Platform.runLater(new Runnable() {
							@Override
							public void run() {
								logined = false;
								status.setText("پسوورد یا نام کاربری اشتباه است");
								playAnimation();
							}
						});
					}
				} catch (Exception e) {
					Platform.runLater(new Runnable() {
						@Override
						public void run() {
							System.out.println(e.getMessage());
							status.setText("خطا در اتصال");
							playAnimation();
						}
					});
				}
			}
		});
		loginer.start();
	}
}
