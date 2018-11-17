package com.lmyun.socket.server;

import com.lmyun.socket.SocketUtils;
import com.lmyun.socket.packages.JsonPackage;
import com.lmyun.socket.packages.request.rsa.RequestRsaKeyPackage;
import com.lmyun.socket.packages.request.rsa.license.LicenseCheckPackage;
import com.lmyun.socket.thread.SocketListenThread;
import com.lmyun.socket.thread.SocketSendThread;
import com.lmyun.utils.RSAUtils;
import org.json.simple.JSONObject;

import java.util.Map;

public class ServerDealer extends Thread {
    private final SocketListenThread listener;
    private final SocketSendThread sender;
    private String privateKey;

    public ServerDealer(SocketListenThread listener, SocketSendThread sender) {
        this.listener = listener;
        this.sender = sender;
    }

    @Override
    public void run() {
        while (true) {
            while (this.listener.getMsg().size() != 0) {
                try {
                    byte[] msg = this.listener.getMsg().take();
                    int msgType = SocketUtils.Byte2Int(SocketUtils.toObjects(SocketUtils.subBytes(msg, 0, 4)));
                    msg = SocketUtils.subBytes(msg, 4, msg.length - 4);
                    this.deal(msgType, msg);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void deal(int msgType, byte[] msg) {
        switch (msgType) {
            case 2:
                this.log("收到心跳包");
                //this.sender.addMsg(new HeartPackage());
                break;
            case 1001:
                this.log("收到公钥请求");
                RequestRsaKeyPackage requestRsaKeyPackage = new RequestRsaKeyPackage();
                Map<String, String> keys = RSAUtils.createKeys(1024);
                requestRsaKeyPackage.setPublicKey(keys.get("publicKey")).contentUpdate();
                this.privateKey = keys.get("privateKey");
                this.sender.addMsg(requestRsaKeyPackage);
                this.log("公钥已发送");
                break;
            case 2001:
                LicenseCheckPackage licenseCheckPackage = new LicenseCheckPackage(new String(msg));
                licenseCheckPackage.setPrivateKey(this.privateKey);
                JsonPackage decode = licenseCheckPackage.decode();
                Map<String, Object> data = decode.getData();
                System.out.println(decode.getJson().toJSONString());
                this.log("授权验证：[" + data.get("licenseType") + "]" + data.get("license"));

        }
    }

    public void log(String log) {
        System.out.println("【" + this.sender.getSocket().getRemoteSocketAddress().toString() + "】" + log);
    }
}
