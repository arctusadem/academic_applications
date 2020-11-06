package server;

import java.util.Calendar;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;


//INICIAR PRIMEIRO SLAVE E DEPOIS MASTER

public class Server {

    
    
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String param ="";
        System.out.println("Aperte: (-m) para Master ou (-s) para Slave:");
        System.out.print("Parametro: "); param = sc.next();

        
        if(param.equals("-m")){
            String ipMaster = "192.168.2.72";
            int portMaster = 9090;
            
            String calendarFormat = "2018/4/27 23:55:40";
            int year =    Integer.parseInt( calendarFormat.trim().split("/")[0] );
            int mounth =  Integer.parseInt( calendarFormat.trim().split("/")[1] );
            int day =     Integer.parseInt( calendarFormat.trim().split("/")[2].split(" ")[0] );
            int hours =   Integer.parseInt( calendarFormat.trim().split(" ")[1].split(":")[0] );
            int minutes = Integer.parseInt( calendarFormat.trim().split(" ")[1].split(":")[1] );
            int seconds = Integer.parseInt( calendarFormat.trim().split(" ")[1].split(":")[2] );
            
            long currentTime = returnTimeMillis(year, mounth, day, hours, minutes, seconds);
            
            int d = 10;
            
            String endereco = "slaves.txt";
            
            String output = "logMaster.log";
            
            Master Master = new Master();
            Master.setMasterParameters(ipMaster,portMaster,currentTime,d,endereco,output);
            try {
                Master.main();
            } catch (Exception ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        }else if(param.equals("-s")){
            Slave Slave = new Slave();
            String ipSlave = "192.168.2.72";
            int portSlave = 9876;
            
            String calendarFormat = "2018/4/27 23:46:12";
            int year =    Integer.parseInt( calendarFormat.trim().split("/")[0] );
            int mounth =  Integer.parseInt( calendarFormat.trim().split("/")[1] );
            int day =     Integer.parseInt( calendarFormat.trim().split("/")[2].split(" ")[0] );
            int hours =   Integer.parseInt( calendarFormat.trim().split(" ")[1].split(":")[0] );
            int minutes = Integer.parseInt( calendarFormat.trim().split(" ")[1].split(":")[1] );
            int seconds = Integer.parseInt( calendarFormat.trim().split(" ")[1].split(":")[2] );
            
            long timeStamp = returnTimeMillis(year, mounth, day, hours, minutes, seconds);
            String logfile = "logSlave.log";
            
            Slave.setVariables(ipSlave, portSlave, timeStamp, logfile);
            try {
                Slave.main();
            } catch (Exception ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }
        }else{
            System.out.println("Favor inserir um comando correto");
        }
        
        
        
    }
    
    private static long returnTimeMillis(int year, int mounth, int day, int hours, int minutes, int seconds){
        Calendar cal = Calendar.getInstance();
        cal.set(year, mounth, day, hours, minutes, seconds);
        return cal.getTimeInMillis();
    }
    
}
