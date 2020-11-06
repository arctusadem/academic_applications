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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;



public class Master {

    private static String ip= "172.16.2.72";
    private static int port=9090;
    private static long time=0;
    private static int d=1;
    private static String slavesFile = "slaves.txt";
    private static String logFile = "logMaster.log";
    
    private static long timeStampMASTER = 0;
    
    private static ArrayList<String> slaves = new ArrayList();
    private static ArrayList<String> messages = new ArrayList();
    
    
    public Master(){}
    
    
    public static void main(String[] args) {
        Master master = new Master();
        try {
            master.main();
        } catch (Exception ex) {
            Logger.getLogger(Master.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    
    
    public void main() throws Exception {
        Master master = new Master();
        master.openFile();
        
        timeStampMASTER = System.currentTimeMillis();
        master.askSlavesHours();
        master.average();
        

    }
    
    private String clientUDP(byte[] sendCommand, int length, String ipSlave, int portSlave) throws Exception {
        InetAddress IPAddress1 = InetAddress.getByName(ip);
        DatagramSocket clientSocket = new DatagramSocket(port,IPAddress1);

        long timeOutCount = 0;
        long timeOutMax = 1;
        String modifiedSentence = "";
        boolean synAck = true; 
        while(true){
            InetAddress IPAddress = InetAddress.getByName(ipSlave);
            byte[] sendData = new byte[1024];
            byte[] receiveData = new byte[1024];
            
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, portSlave);
            
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            if(synAck){
                sendData = sendCommand;
                sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, portSlave);
                clientSocket.send(sendPacket);
                receivePacket = new DatagramPacket(receiveData, receiveData.length);
                
                try{
                    clientSocket.receive(receivePacket);
                }catch(Exception e){
                    System.out.println("Host not responded. Trying again");
                    if(timeOutCount == timeOutMax){
                        System.out.println("Max range of timeout reached. Aborting conection..");
                        clientSocket.close();
                        return "";
                    }
                    timeOutCount++;
                    continue;
                }

                modifiedSentence = new String(receivePacket.getData());
                clientSocket.close();
                break;
            }

        }
        return modifiedSentence;

    }
    
    
    public void askSlavesHours() throws Exception{
        byte sendCommand[] = new byte[1024];
        sendCommand = "/getHour".getBytes();
        
        for(String slave : slaves){
            int port = Integer.parseInt( slave.split(":")[1] );
            String ip = slave.split(":")[0];
            String receiveTime = clientUDP(sendCommand, sendCommand.length, "172.16.2.72", port);
            if(!receiveTime.trim().equals("")){
                long receiveTimeLong = Long.parseLong(receiveTime.trim());
                String calendar = this.getFormatCalendar(receiveTimeLong);
                String compound = ip + ":" + port + ":" + receiveTime.trim() + ":"+ calendar;
                messages.add(compound);
                
                writeLogFile("Get slave hours: "+compound);
                System.out.println("Get slave hours: "+compound);
            }
        }
        
    }
 

    private void average(){
       
        long diffValue = 0;
        long divisionCount = 1;
        
        int i=0; int count = this.messages.size();
        for(String messageIP : this.messages){
            

            long value = getMinutesCalendar(this.timeStampMASTER) - getMinutesCalendar(Long.parseLong( messageIP.split(":")[2].trim() ) ) ;
            String compound = messageIP.split(":")[0].trim() + ":" + messageIP.split(":")[1].trim() + ":" + messageIP.split(":")[2].trim() + ":" + messageIP.split(":")[3].trim() +":" + messageIP.split(":")[4].trim() +":" + messageIP.split(":")[5].trim() + "; Diferenca="+value;
            
            if( Math.abs(value) <= this.d ){
                diffValue -= value;
                divisionCount++;
            
            }else{
                compound = messageIP.split(":")[0].trim() + ":" + messageIP.split(":")[1].trim() + ":" + messageIP.split(":")[2].trim() + ":" +  messageIP.split(":")[3].trim() +":" + messageIP.split(":")[4].trim() +":" + messageIP.split(":")[5].trim() + "; Diferenca="+value +" Falha";
            }
            System.out.println("Diferenca: "+compound);
            writeLogFile("Diferenca: "+compound);
            messages.set(i++,compound);
            if(i==count) break;
        }
        long avg = diffValue/divisionCount;
        System.out.println("MASTER: "+this.timeStampMASTER + ":"+getFormatCalendar(this.timeStampMASTER));
        writeLogFile("MASTER: "+this.timeStampMASTER + ":"+getFormatCalendar(this.timeStampMASTER));
        System.out.println("Media D: "+avg);
        writeLogFile("Media D: "+avg);
        
        for(String messageIP: this.messages){
            long newSeconds = 0, newMinutes=0;
            String compound = "";
            try{
                if( messageIP.split("Diferenca=")[1].split(" ")[1].equals("Falha")  ){
                    long value = Long.parseLong( messageIP.split("Diferenca=")[0].split(" ")[0].trim());
                    value = value * 60000;
                    if( value >= 0 ){
                        newMinutes = Long.parseLong(messageIP.split(":")[2].trim()) + value;
                    }else{
                        newMinutes = Long.parseLong(messageIP.split(":")[2].trim()) - value;
                    }
                }
                
            }catch(Exception e){
                if( !(messageIP.split("Diferenca=")[1].split(" ")[1].isEmpty()) ){
                    long value = Long.parseLong( messageIP.split("Diferenca=")[0].trim());
                    avg = avg * 60000;
                    if( value >= 0 ){
                        newMinutes = Long.parseLong(messageIP.split(":")[2].trim()) + avg;
                    }else{
                        newMinutes = Long.parseLong(messageIP.split(":")[2].trim()) - avg;
                    }
                }
            }
            
            String ip = messageIP.split(":")[0];
            String port = messageIP.split(":")[1];

            compound = ip + ":" + port + ":" + newMinutes + ":" + getFormatCalendar(newMinutes);
            
            writeLogFile("SET NEW HOUR: "+compound);
            System.out.println("New hour: "+compound);
        }
        this.messages.clear();
        
    }
    
    private void setNewTimeStamp(String ip, String port, String timeStamp, String calendar, String hourStr, String minutesStr, String secondsStr){
        int year = Integer.parseInt( calendar.split("/")[0]);
        int month = Integer.parseInt( calendar.split("/")[1]);
        int day = Integer.parseInt( calendar.split("/")[2].split(" ")[0]);
        int hour = Integer.parseInt(hourStr);
        int minutes = Integer.parseInt(minutesStr);
        int seconds = Integer.parseInt(secondsStr);
        long newTimeStamp = returnTimeMillis(year, month, day, hour,minutes,seconds);
        
        byte[] send = new byte[1024];
        send = String.format("/setNewHour "+newTimeStamp).getBytes();
        
        writeLogFile("Novo tempo: "+ip+":"+port+" "+getFormatCalendar(newTimeStamp));
        
        try {
            clientUDP(send, send.length, ip, Integer.parseInt(port) );
        } catch (Exception ex) {
            Logger.getLogger(Master.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
    }
   
    private long getSecondsCalendar(long timeStamp){
        long seconds =0;
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timeStamp);
        seconds = calendar.get(Calendar.SECOND);
        return seconds;
    }
    
    private long getMinutesCalendar(long timeStamp){
        long minutes =0;
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timeStamp);
        minutes = calendar.get(Calendar.MINUTE);
        return minutes;
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
    
    private void writeLogFile(String compound){
        BufferedWriter writer= null;
        try{
            writer = new BufferedWriter(new FileWriter(new File(logFile), true));
            writer.append(getFormatCalendar(System.currentTimeMillis()) + ": "+compound + "\n");

        }catch(Exception e){
            System.out.println("E");
        }finally{
            try {
                writer.close();
            } catch (IOException ex) {
                Logger.getLogger(Master.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }
    
    private void openFile(){
        File file = new File(slavesFile);
        BufferedReader reader = null;
        try{
            reader = new BufferedReader(new FileReader(file));
            String text = null;
            
            while((text = reader.readLine()) != null){
                slaves.add(text);
            }
            
        }catch(Exception e){
            System.out.println("E");
        }finally{
            try {
                reader.close();
            } catch (IOException ex) {
                Logger.getLogger(Master.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
    }
    
    private long returnTimeMillis(int year, int mounth, int day, int hours, int minutes, int seconds){
        Calendar cal = Calendar.getInstance();
        cal.set(year, mounth, day, hours, minutes, seconds);
        return cal.getTimeInMillis();
    }
    
    public void setMasterParameters(String ipMaster, int portMaster, long currentTime, int d_average, String endereco_file, String output) {
        ip = ipMaster;
        port = portMaster;
        time = currentTime;
        d = d_average;
        slavesFile = endereco_file;
        logFile = output;
        
    }
    
}