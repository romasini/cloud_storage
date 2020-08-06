public class Network {

    private String serverName;
    private int serverPort;

    public Network(String serverName, int serverPort) {
        this.serverName = serverName;
        this.serverPort = serverPort;
    }

    public Network() {
        //по возможности загружать настройки из файла типа properties
        this("localhost",8189);
    }
}
