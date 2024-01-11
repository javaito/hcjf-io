package org.hcjf.io.net.proxy;

public class Main {

    public static void main(String[] args) {
        TcpProxy proxy = new TcpProxy(6555);
        proxy.start();
    }

}
