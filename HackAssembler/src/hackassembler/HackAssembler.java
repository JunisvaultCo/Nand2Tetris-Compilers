/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hackassembler;
import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import javax.swing.event.*;
import java.util.*;
import java.io.*;

/**
 *
 * @author jul
 */
public class HackAssembler extends JFrame implements ActionListener
{
    JTextField JTF;
    JButton JB;
    JTextField errorField;
    
    String[] jumps=
    {
      "",
      "JGT",
      "JEQ",
      "JGE",
      "JLT",
      "JNE",
      "JLE",
      "JMP"
    };
    
    String[] nx=
    {
      "1","-1","A","!A","-A","D+1","A+1","A-1","D-A","D|A",
      "1+D","1+A","-1+A","-A+D","A|D"
    };
    String[] ny=
    {
      "1", "D","!D","-D","D+1","A+1","D-1","A-D","D|A",
      "1+D","1+A","-1+D","-D+A","A|D"
    };
    
    int pos=16;
    
    HashMap<String,Integer> hash= new HashMap<String,Integer>();
    HashMap<String,Integer> countt= new HashMap<String,Integer>();
    
    HackAssembler()
    {
      super("HackAssembler");
      
      JTF= new JTextField("File Name Here");
      JB= new JButton("Assemble~!");
      JB.addActionListener(this);
      
      errorField= new JTextField();
      errorField.setEditable(false);
      errorField.setForeground(Color.RED);
      errorField.setText("tasty test");
      
      setLayout(new BoxLayout(getContentPane(),BoxLayout.Y_AXIS));
      
      add(JTF);
      add(JB);
      add(errorField);
      
      pack();
      setLocationRelativeTo(null);
      setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      
      hash.put("SP",0);
      hash.put("LCL",1);
      hash.put("ARG",2);
      hash.put("THIS",3);
      hash.put("THAT",4);
      for(int i=0;i<16;i++)
        hash.put("R"+i, i);
      hash.put("SCREEN",16384);
      hash.put("KEYBOARD",24576);
    }
    
    public static void main(String[] args)
    {
      javax.swing.SwingUtilities.invokeLater(
       ()->
       {
         GUI();
       }
      );
    }
    private static void GUI()
    {
      HackAssembler HA= new HackAssembler();
      HA.setVisible(true);
    }
    
    private int find(String s,String[] arr)
    {
      for(int i=0;i<arr.length;i++)
        if(arr[i].equals(s))
          return i;
      return -1;
    }
    
    private int getComp(String s)
    {
      int code=0;
      if(s.contains("M"))//a
        code+=4096;
      s=s.replace("M", "A");
      if(!s.contains("D"))//zx
        code+=2048;
      if(find(s,nx)!=-1)//nx
        code+=1024;
      if(!s.contains("A"))//zy
        code+=512;
      if(find(s,ny)!=-1)//ny
        code+=256;
      if(s.contains("+")||s.contains("-")||s.equals("1")||s.equals("0"))//f
        code+=128;
      if((s.contains("-")||s.contains("!")||s.contains("1")||s.contains("|"))&& !s.contains("-1"))//no
        code+=64;
      return code;
    }
    
    public void actionPerformed(ActionEvent e)
    {
      try
      {
        int line=0;
        File f=new File(JTF.getText()+".asm");
        Scanner scann= new Scanner(f);
        FileWriter fw= new FileWriter(new File(JTF.getText()+".hack"));
        BufferedWriter bw = new BufferedWriter(fw);
        while(scann.hasNextLine())
        {
          String s=scann.nextLine();
          s=s.split("/.")[0].trim();
          if(s.length()==0||s.charAt(0)=='/') continue;
          if(s.charAt(0)=='(')
          {
            String[] words=s.split("[()]");
            if(hash.containsKey(words[1])&&countt.containsKey(words[1]))
            {
              errorField.setText("Label at line "+line+" already defined!");
              return;
            }
            else
            {
              hash.put(words[1], line);
              countt.put(words[1],1);
              
            }
          }
          else line++;
        }
        scann.close();
        scann= new Scanner(f);
        line=0;
        while(scann.hasNextLine())
        {
          int code=0;
          String s=scann.nextLine();
          s=s.split("/.")[0].trim();
          if(s.length()==0||s.charAt(0)=='/'||s.charAt(0)=='(') continue;
          boolean isA=(s.charAt(0)=='@');
          if(!isA)
          {
            code+=57344;
            String[] words=s.split("[=;]");
            if(s.contains(";"))
              code+=find(words[words.length-1],jumps);
            if(s.contains("="))
            {
              if(words[0].contains("M"))
                code+=8;
              if(words[0].contains("D"))
                code+=16;
              if(words[0].contains("A"))
                code+=32;
            }
            if(!s.contains("="))
              code+=getComp(words[0]);
            else
              code+=getComp(words[1]);
          }
          else
          {
            System.out.println("here Damn it");
            String[] words= s.split("@");
            try
            {
              if(Integer.valueOf(words[1])<0)
              {
                errorField.setText("Address at line "+line+" is a negative number!");
                return;
              }
              code+=Integer.valueOf(words[1]);
            }
            catch(Exception exx)
            {
              if(hash.containsKey(words[1]))
                code+=hash.get(words[1]);
              else
              {
                hash.put(words[1], pos);
                code+=pos;
                pos++;
              }
            }
          }
          for(int i=32768;i>=1;i=i/2)
          {
            bw.write(code/i+'0');
            code=code%i;
          }
          bw.write('\n');
          line++;
        }
        bw.close();
        errorField.setText("DONE!");
      }
      catch(Exception ex)
      {
        ex.printStackTrace();
        errorField.setText("Cannot find file!");
      }
    }
    
}
