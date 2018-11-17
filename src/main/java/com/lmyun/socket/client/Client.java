package com.lmyun.socket.client;

import com.lmyun.socket.SocketUtils;
import com.lmyun.socket.client.thread.SocketHearter;
import com.lmyun.socket.packages.request.rsa.RequestRsaKeyPackage;
import com.lmyun.socket.packages.request.rsa.license.LicenseCheckPackage;
import com.lmyun.socket.thread.SocketListenThread;
import com.lmyun.socket.thread.SocketSendThread;
import lombok.Getter;

import java.io.IOException;
import java.net.Socket;
import java.util.UUID;

public class Client {
    private Socket socket;
    private String host = "localhost";
    private int port = 9999;

    private SocketSendThread sender;
    private SocketListenThread listener;
    @Getter
    private String publicKey;

    public Client() throws IOException {
        socket = new Socket(host, port);
        System.out.println("客户端启动!!!");
    }

    public static void main(String[] args) throws Exception {
        new Client().talk();
    }

    private void talk() throws Exception {
        this.sender = new SocketSendThread(this.socket);
        this.listener = new SocketListenThread(this.socket);
        this.sender.start();
        this.listener.start();
        new SocketHearter(this.sender).start();//心跳开始
        //////////////////////////////////////////////////////////////////////////////
        this.sender.addMsg(new RequestRsaKeyPackage().setPublicKey(String.valueOf(UUID.randomUUID())).contentUpdate());
        while (true) {
            while (this.listener.getMsg().size() != 0) {
                byte[] msg = this.listener.getMsg().take();
                int msgType = SocketUtils.Byte2Int(SocketUtils.toObjects(SocketUtils.subBytes(msg, 0, 4)));
                msg = SocketUtils.subBytes(msg, 4, msg.length - 4);
                this.deal(msgType, msg);
            }
            Thread.sleep(100);
        }
    }

    public void deal(int msgType, byte[] msg) {

        switch (msgType) {
            case 2:
                System.out.println("收到来自服务端的心跳包");
                break;
            case 1001:
                RequestRsaKeyPackage requestRsaKeyPackage = new RequestRsaKeyPackage(new String(msg));
                this.publicKey = requestRsaKeyPackage.getPublicKey();
                System.out.println("收到来自服务端的公钥" + this.publicKey);
                LicenseCheckPackage licenseCheckPackage = new LicenseCheckPackage();
                licenseCheckPackage.setLicense("aaaaa");
                licenseCheckPackage.setLicenseType("test");
                licenseCheckPackage.setPublicKey(this.getPublicKey());
                this.sender.addMsg(licenseCheckPackage.contentUpdate());
                break;
        }
    }
}
