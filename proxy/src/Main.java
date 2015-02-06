import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.concurrent.atomic.AtomicBoolean;

public class Main implements Runnable {
    private final byte[] START_SEQ = new byte[] { 0, Byte.MIN_VALUE, Byte.MIN_VALUE, Byte.MIN_VALUE, 0 };
    private ServerSocket serverSocket;
    private final FileOutputStream out;
    private final AtomicBoolean rx = new AtomicBoolean(), tx = new AtomicBoolean();
    private byte mark;

    private final JSpinner spinner = new JSpinner();

    public void setMark(byte mark) {
        if (this.mark != mark) {
            System.err.println("Mark " + this.mark + " -> " + mark);
            this.mark = mark;
        }
    }

    public Main() throws IOException {
        this.serverSocket = new ServerSocket(9999);
        this.out = new FileOutputStream("file.dat", true);
    }

    public void close() throws IOException {
        this.out.close();
        serverSocket.close();
        serverSocket = null;
    }

    public static void main(String[] args) throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException, KeyManagementException, InterruptedException {
//        KeyStore ks = KeyStore.getInstance("JKS");
//        ks.load(new FileInputStream("/home/mgruszecki/mitm/server.jks"), "password".toCharArray());
//        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
//        kmf.init(ks, "password".toCharArray());
//        SSLContext sc = SSLContext.getInstance("TLS");
//        sc.init(kmf.getKeyManagers(), null, null);
//        SSLServerSocketFactory ssf = sc.getServerSocketFactory();
//        SSLServerSocket sslserversocket = (SSLServerSocket) ssf.createServerSocket(9999);
        Main main = new Main();
        main.start();


    }

    private void start() {
        Thread main = new Thread(this, "main-loop");
        main.setDaemon(true);
        main.start();
        JFrame frame = new JFrame("Eve proxy");
        JPanel mainPanel = new JPanel();
        frame.getContentPane().add(mainPanel);
        mainPanel.add(new IndicatorPanel(rx, Color.GREEN, "RX"));
        mainPanel.add(new IndicatorPanel(tx, Color.RED, "TX"));
        mainPanel.add(spinner);
        mainPanel.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {  }
            @Override
            public void mousePressed(MouseEvent e) {
                Number number = (Number) spinner.getValue();
                setMark(number.byteValue());
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                setMark((byte) 0);
            }
            @Override
            public void mouseEntered(MouseEvent e) {   }
            @Override
            public void mouseExited(MouseEvent e) {    }
        });
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setSize(new Dimension(200, 200));
        frame.setVisible(true);
    }

    @Override
    public void run() {
        while(serverSocket != null) {
            try {
                Socket socket = serverSocket.accept();
                System.err.println("Connection from " + socket.getInetAddress());
                System.err.println("Connecting to eve server");
                Socket eveSocket = new Socket("evex", 26000);
                Thread thread;
                thread = new Thread(new Copier(socket.getInputStream(), eveSocket.getOutputStream(), tx, (byte)1));
                thread.setName("1 - local -> eve server");
                thread.setDaemon(true);
                thread.start();
                thread = new Thread(new Copier(eveSocket.getInputStream(), socket.getOutputStream(), rx, (byte)2));
                thread.setName("2 - eve server -> local");
                thread.setDaemon(true);
                thread.start();
                thread.join();
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                // do nothing
            }
        }
    }

    class Copier implements Runnable {
        private final InputStream inputStream;
        private final OutputStream outputStream;
        private final AtomicBoolean indicator;
        private final byte code;

        Copier(InputStream inputStream, OutputStream outputStream, AtomicBoolean indicator, byte code) {
            this.inputStream = inputStream;
            this.outputStream = outputStream;
            this.indicator = indicator;
            this.code = code;
        }

        @Override
        public void run() {
            System.err.println("Starting " + code);
            final byte[] buffer = new byte[4096];
            int r;
            try {
                while (true) {
                    while ((r = inputStream.read(buffer)) >= 0) {
                        indicator.set(true);
                        outputStream.write(buffer, 0, r);
                        outputStream.flush();
                        save(code, buffer, r);
                    }
                    Thread.sleep(1);
                }
            } catch (Exception ex) {
                System.err.println("Error " + Thread.currentThread().getName() + "\t" + ex);
                ex.printStackTrace();
            }
        }
    }

    private void save(byte code, byte[] buffer, int len) throws IOException {
        synchronized (out) {
            out.write(START_SEQ);
            out.write(new byte[] {code, mark});
            out.write(len);
            out.write(buffer, 0, len);
            out.flush();
        }
    }
}



