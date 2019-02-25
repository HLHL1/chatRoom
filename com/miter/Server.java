
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class ServerThread implements Runnable{

    //获取的客户端Socket
    Socket client = null;
    //获取的服务器ServerSocket
    ServerSocket server = null;
    //获取的客户端IP
    String ip = null;
    //获取的客户端端口
    int port = 0;
    //组合客户端的ip和端口字符串得到uid字符串
    String uid = null;

    //静态ArrayList存储所有uid，uid由ip和端口字符串拼接而成
    static ArrayList<String> uid_arr = new ArrayList<String>();//主要用于打印更新上线的客户端---可以去掉，用Map打印也可以
    //静态HashMap存储所有uid, ServerThread对象组成的对
    static Map<String, ServerThread> cm = new ConcurrentHashMap<>();

    public ServerThread(Socket client, ServerSocket server,String ip, int port) {
        this.client = client;
        this.server = server;
        this.ip = ip;
        this.port = port;
        this.uid = ip+":"+port;
        
    }

    @Override
    public void run() {

        //将当前客户端uid存入的ArrayList
        uid_arr.add(uid);
        //将当前服务线程存入Map中
        cm.put(uid,this);

        //显示时间格式
        SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        //终端打印客户端IP和端口
      //  System.out.println("Client connected: " + uid);

        try {
            //获取客户端的输入输出流
            InputStream in=client.getInputStream();//客户向我输入
            OutputStream out=client.getOutputStream();//我给客户返回

            //向客户端输入连接成功的信息
            String welcom = sdf.format(new Date())+"\n成功连接服务器...\n服务器IP: " + server.getInetAddress().getLocalHost().getHostAddress() + ", 端口: 6666\n客户端IP: " + ip + ", 端口: " + port + "\n";
            out.write(welcom.getBytes());

            //广播更新在线名单
            updateOnlineList(out);


            //准备缓冲区
            byte[] buf = new byte[1024];
            int len = 0;

            //持续监听并转发客户端消息
            while(true){

                //获取客户端给服务器发送的信息
                len=in.read(buf);
                String msg=new String(buf,0,len);
             //   System.out.println(msg);

                //消息类型：退出或者聊天
                String type=msg.substring(0,msg.indexOf('/'));
                //聊天内容：空或者聊天内容
                String chat=msg.substring(msg.indexOf('/')+1);

                //根据消息类型分别处理
                //如果退出
                if(type.equals("Exit")){

                    //更新ArrayList和Map
                    uid_arr.remove(uid_arr.indexOf(this.uid));
                    cm.remove(this.uid);
                    //广播更新在线名单
                    updateOnlineList(out);
                    break;
                }
                //如果聊天
                else if(type.equals("Chat")) {

                    //提取收信者信息
                    String[] receiveArr = chat.substring(0, chat.indexOf('/')).split(",");
                    //提取聊天内容
                    String word = chat.substring(chat.indexOf('/') + 1);
                    System.out.println(word);
                    //向收信者广播发出聊天信息
                    chatOnlineList(out, uid, receiveArr, word);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void chatOnlineList(OutputStream out, String uid, String[] receiveArr, String word)throws Exception {

        for(String tmp:receiveArr){
            out=cm.get(tmp).client.getOutputStream();
            //发送聊天信息
            out.write(("Chat/" + uid + "/" + word).getBytes());
        }
    }

    private void updateOnlineList(OutputStream out) throws Exception {

            for (String tmp : uid_arr) {
                //获取广播收听者的输出流
                out = cm.get(tmp).client.getOutputStream();
                //将当前在线名单以逗号为分割组合成长字符串一次传送
                StringBuilder sb = new StringBuilder("OnlineListUpdate/");
                for (String member : uid_arr) {
                    sb.append(member);
                    //以逗号分隔uid，除了最后一个
                    if (uid_arr.indexOf(member) != uid_arr.size() - 1)
                        sb.append(",");
                }
                //向每个客户端输入更新在线的名单
                out.write(sb.toString().getBytes());
            }

    }

}
public class Server {

    public static void main(String[] args) throws Exception{

        //建立服务器
        ServerSocket server=new ServerSocket(6666);
        //提示服务端建立成功
        //socket.getInetAddress()返回InetAddress对象包含远程计算机的IP地址。
        // InetAddress.getHostAddress()返回String对象与该地址的文本表示。
      //  System.out.println("Server online...."+server.getInetAddress().getLocalHost().getHostAddress()+","+6666);

        while(true) {
            //接收客户端Socket
            Socket client = server.accept();
            //提取客户端Ip
            String ip=client.getInetAddress().getHostAddress();
            //提取客户端端口号
            int port=client.getPort();
            //建立新的服务器线程, 向该线程提供服务器ServerSocket，客户端Socket，客户端IP和端口
            new Thread(new ServerThread(client, server, ip, port)).start();
        }
    }
}
