package server;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Slave {

    
    private static String ip = "";
    private static int port = 9876;
    private static long timeStamp = System.currentTimeMillis() - (5 * 60000);
    private static String output = "logSlave.log";
    
    public Slave(){}
    
    
    public static void main(String[] args) {
        Slave slave = new Slave();
        try {
            slave.main();
        } catch (Exception ex) {
            Logger.getLogger(Slave.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    

    public void main() throws Exception{
        System.out.println("Iniciado. Escutando ...");
        DatagramSocket serverSocket = new DatagramSocket(port);
        byte[] receiveData = new byte[1024];
        byte[] sendData = new byte[1024];
        while (true) {
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            serverSocket.receive(receivePacket);

            InetAddress IPAddress = receivePacket.getAddress();
            int port = receivePacket.getPort();

            String sentence = new String(receivePacket.getData());
            System.out.println("RECEIVED: " + IPAddress + ":" + port + ":" + sentence.trim());
            writeLogFile("RECEIVED: " + IPAddress + ":" + port + ":" + sentence.trim());
            
            if (sentence.trim().equals("/getHour")) {
                long time = System.currentTimeMillis();
                sentence = String.valueOf(time);
                //sentence = String.valueOf(timeStamp);
                String compound = IPAddress + ":" + port + ":" + sentence;
                System.out.println("SENT: "+compound);
                
                String capitalizedSentence = sentence.toUpperCase();
                sendData = capitalizedSentence.getBytes();
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
                serverSocket.send(sendPacket);
                
                writeLogFile("SENT command \"/localHour\": "+compound);
                
            }else if(sentence.startsWith("/setNewHour")){
                this.timeStamp = Long.parseLong(sentence.split(" ")[1]);
                
                String compound = IPAddress + ":" + port + ": Hours updated" + timeStamp + " - "+getFormatCalendar(timeStamp);
                
                sendData = compound.getBytes();
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
                serverSocket.send(sendPacket);
                
                
                writeLogFile("SENT: "+compound);
                
            }else if(sentence.trim().equals("SYN")){
                sentence = "ACK";
                String compound = IPAddress + ":" + port + ":" + sentence;
                System.out.println("SENT: "+compound);
                
                String capitalizedSentence = sentence.toUpperCase();
                sendData = capitalizedSentence.getBytes();
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
                serverSocket.send(sendPacket);
                
                writeLogFile("SENT: "+compound);
            }

        }

    }
    
    
    private void writeLogFile(String compound){
        //File file = new File(output);
        BufferedWriter writer= null;
        try{
            writer = new BufferedWriter(new FileWriter(new File(output), true));
            writer.append(getFormatCalendar(System.currentTimeMillis()) + ": "+compound + "\n");
            
        }catch(Exception e){
            System.out.println("E");
        }finally{
            try {
                writer.close();
            } catch (IOException ex) {
                Logger.getLogger(Slave.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
    }
    
    
    public String getFormatCalendar(long timeStamp){
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timeStamp);

        int mYear = calendar.get(Calendar.YEAR);
        int mMonth = calendar.get(Calendar.MONTH);
        int mDay = calendar.get(Calendar.DAY_OF_MONTH);
        int mHours = calendar.get(Calendar.HOUR);
        int mMin = calendar.get(Calendar.MINUTE);
        int mSec = calendar.get(Calendar.SECOND);
        int mMilisec = calendar.get(Calendar.MILLISECOND);
        String message = mYear + "/" + mMonth + "/" + mDay + " " + mHours + ":"+ mMin + ":" + mSec;
        
        return message;
        
    }
    
    private long returnTimeMillis(int year, int mounth, int day, int hours, int minutes, int seconds){
        Calendar cal = Calendar.getInstance();
        cal.set(year, mounth, day, hours, minutes, seconds);
        return cal.getTimeInMillis();
    }
    
    public void setVariables(String ip, int port, long timeStamp, String output){
        this.ip = ip;
        this.port = port;
        this.timeStamp = timeStamp;
        this.output = output;
    }
    
    
}
