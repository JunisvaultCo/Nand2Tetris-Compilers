package hackvm;
import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import javax.swing.event.*;
import java.io.*;
import java.util.*;
/**
 *
 * @author jul
 */
public class HackVM extends JFrame implements ActionListener
{
  JTextField JTF;
  JButton JB;
  JTextField errorField;
  
  FileWriter fw;
  BufferedWriter bw;
  
  int staticCountt=16;
  int tempCountt=5;
  int line=0;
  
  String function="";
  String name="";
  int countt=1;
  
  HackVM()
  {
    super("HackVM");
    JTF= new JTextField("Enter name here");
    JB= new JButton("Assemble~!");
    JB.addActionListener(this);
    
    errorField= new JTextField("tasty test");
    errorField.setEditable(false);
    errorField.setForeground(Color.RED);
    
    setLayout(new BoxLayout(getContentPane(),BoxLayout.Y_AXIS));
    add(JTF);
    add(JB);
    add(errorField);
    pack();
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setLocationRelativeTo(null);
    
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
    HackVM HV = new HackVM();
    HV.setVisible(true);
  }
  
  private void stackCommand(String[] words) throws Exception
  {
    boolean isC=words[1].equals("constant");
    boolean isS=words[1].equals("static");
    boolean isT=words[1].equals("temp");
    boolean isP=words[1].equals("pointer");
    int num;
    try
    {
      num=Integer.valueOf(words[2]);
    }
    catch(Exception e)
    {
      errorField.setText(words[2]+"is not an unsigned integer");
      e.printStackTrace();
      return;
    }
    if(words[0].equals("push"))
    {
      if(isC)
      {
        bw.write(
          "@"+num+"\n"+
          "D=A\n"+
          "@SP\n" +
          "A=M\n" +
          "M=D\n" +
          "@SP\n" +
          "M=M+1\n");
        line+=7;
      }
      else if(isS)
      {
        bw.write(
          "@"+name+"."+num+"\n" +
          "D=M\n" +
          "@SP\n" +
          "A=M\n" +
          "M=D\n" +
          "@SP\n" +
          "M=M+1\n"
        );
        line+=7;
      }
      else if(isP)
      {
        String th="@THIS";
        if(num==1)
          th="@THAT";
        bw.write(
          th+"\n" +
          "D=M\n" +
          "@SP\n" +
          "A=M\n" +
          "M=D\n" +
          "@SP\n" +
          "M=M+1\n"
        );
        line+=7;
      }
      else if(isT)
      {
        bw.write(
          "@"+(5+num)+"\n" +
          "D=M\n" +
          "@SP\n" +
          "A=M\n" +
          "M=D\n" +
          "@SP\n" +
          "M=M+1\n"
        );
        line+=7;
      }
      else
      {
        String segment="@LCL";
        if(words[1].equals("argument"))
          segment="@ARG";
        else if(words[1].equals("this"))
          segment="@THIS";
        else if(words[1].equals("that"))
          segment="@THAT";
        bw.write(
          "@"+num+"\n" +
          "D=A\n" +
          segment+"\n" +
          "A=M+D\n" +
          "D=M\n" +
          "@SP\n" +
          "A=M\n" +
          "M=D\n" +
          "@SP\n" +
          "M=M+1\n"
        );
        line+=10;
      }
    }
    else if(words[0].equals("pop"))
    {
      if(isC)
      {
        errorField.setText("Cannot pop constant!");
        return;
      }
      else if(isS)
      {
        bw.write(
          "@SP\n" +
          "M=M-1\n" +
          "A=M\n" +
          "D=M\n" +
          "@"+name+"."+num+"\n" +
          "M=D\n"
        );
        line+=6;
      }
      else if(isP)
      {
        String th="@THIS";
        if(num==1)
          th="@THAT";
        bw.write(
          "@SP\n" +
          "M=M-1\n" +
          "A=M\n" +
          "D=M\n" +
          ""+th+"\n" +
          "M=D\n"
        );
        line+=6;
      }
      else if(isT)
      {
        bw.write(
          "@SP\n" +
          "M=M-1\n" +
          "A=M\n" +
          "D=M\n" +
          "@"+(num+5)+"\n" +
          "M=D\n"
        );
        line+=6;
      }
      else
      {
        String segment="@LCL";
        if(words[1].equals("argument"))
          segment="@ARG";
        else if(words[1].equals("this"))
          segment="@THIS";
        else if(words[1].equals("that"))
          segment="@THAT";
        bw.write(
          segment+"\n" +
          "D=M\n" +
          "@"+num+ "\n" +
          "D=D+A\n" +
          "@SP\n" +
          "A=M\n" +
          "M=D\n" +
          "@SP\n" +
          "A=M-1\n" +
          "D=M\n" +
          "A=A+1\n" +
          "A=M\n" +
          "M=D\n" +
          "@SP\n" +
          "M=M-1\n"
        );
        line+=15;
      }
    }
  }
  
  private void arithmeticCommand(String[] words) throws Exception
  {
    //returns integer
    if(words[0].equals("add"))//x+y
    {
      bw.write(
              "@SP\n" +
              "M=M-1\n" +
              "A=M-1\n" +
              "D=M\n" +
              "A=A+1\n" +
              "D=D+M\n" +
              "@SP\n" +
              "A=M-1\n" +
              "M=D\n"
              );
      line+=9;
    }
    else if(words[0].equals("sub"))//x-y
    {
      bw.write(
              "@SP\n" +
              "M=M-1\n" +
              "A=M-1\n" +
              "D=M\n" +
              "A=A+1\n" +
              "D=D-M\n" +
              "@SP\n" +
              "A=M-1\n" +
              "M=D\n"
              );
      line+=9;
    }
    else if(words[0].equals("neg"))//-y
    {
      bw.write(
        "@SP\n" +
        "A=M-1\n" +
        "M=-M\n"
      );
      line+=3;
    }
    //returns boolean
    else if(words[0].equals("eq"))//x==y
    {
      bw.write(
        "@SP\n" +
        "M=M-1\n" +
        "A=M-1\n" +
        "D=M\n" +
        "A=A+1\n" +
        "D=D-M\n" +
        "@"+(line+11)+"\n" +
        "D;JEQ\n" +
        "D=0\n" +
        "@"+(line+12)+"\n" +
        "0;JMP\n" +
        "D=-1\n" +
        "@SP\n" +
        "A=M-1\n" +
        "M=D\n"
      );
      line+=15;
    }
    else if(words[0].equals("gt"))//x>y
    {
      bw.write(
        "@SP\n" +
        "M=M-1\n" +
        "A=M-1\n" +
        "D=M\n" +
        "A=A+1\n" +
        "D=D-M\n" +
        "@"+(line+11)+"\n" +
        "D;JGT\n" +
        "D=0\n" +
        "@"+(line+12)+"\n" +
        "0;JMP\n" +
        "D=-1\n" +
        "@SP\n" +
        "A=M-1\n" +
        "M=D\n"
      );
      line+=15;
    }
    else if(words[0].equals("lt"))//x<y
    {
      bw.write(
        "@SP\n" +
        "M=M-1\n" +
        "A=M-1\n" +
        "D=M\n" +
        "A=A+1\n" +
        "D=D-M\n" +
        "@"+(line+11)+"\n" +
        "D;JLT\n" +
        "D=0\n" +
        "@"+(line+12)+"\n" +
        "0;JMP\n" +
        "D=-1\n" +
        "@SP\n" +
        "A=M-1\n" +
        "M=D\n"
      );
      line+=15;
    }
    else if(words[0].equals("and"))//x and y
    {
      bw.write(
              "@SP\n" +
              "M=M-1\n" +
              "A=M-1\n" +
              "D=M\n" +
              "A=A+1\n" +
              "D=D&M\n" +
              "@SP\n" +
              "A=M-1\n" +
              "M=D\n"
              );
      line+=9;
    }
    else if(words[0].equals("or"))//x or y
    {
      bw.write(
              "@SP\n" +
              "M=M-1\n" +
              "A=M-1\n" +
              "D=M\n" +
              "A=A+1\n" +
              "D=D|M\n" +
              "@SP\n" +
              "A=M-1\n" +
              "M=D\n"
              );
      line+=9;
    }
    else if(words[0].equals("not"))//not y
    {
      bw.write(
        "@SP\n" +
        "A=M-1\n" +
        "M=!M\n"
      );
      line+=3;
    }
  }
  
  private void branchCommand(String[] words) throws Exception
  {
    if(words[0].equals("label"))
      bw.write("("+function+"$"+words[1] +")\n");
    else if(words[0].equals("goto"))
    {
      bw.write(
        "@"+function+"$"+words[1]+"\n"+
        "0;JMP\n"
      );
      line+=2;
    }
    else if(words[0].equals("if-goto"))
    {
      bw.write(
        "@SP\n" +
        "A=M-1\n" +
        "D=M\n" +
        "@SP\n" +
        "M=M-1\n"+
        "@"+words[1]+"\n"+
        "D;JNE\n"
      );
      line+=7;
    }
  }
  
  private void functionCommand(String[] words) throws Exception
  {
    if(words[0].equals("function"))
    {
      //save function point
      function=words[1];
      countt=1;
      bw.write(
        "("+words[1] +")\n"
      );
      try
      {
        //set LCL
        int num=Integer.valueOf(words[2]);
        bw.write(
          "@SP\n" +
          "D=M\n" +
          "@LCL\n" +
          "M=D\n"
        );
        line+=4;
        //set local variables to 0
        for(int i=0;i<num;i++)
        {
          bw.write(
            "@SP\n" +
            "A=M\n" +
            "M=0\n" +
            "@SP\n" +
            "M=M+1\n");
          line+=5;
        }
      }
      catch(NumberFormatException e)
      {
        errorField.setText("Found non-integer in word 3 line "+line);
        e.printStackTrace();
        return;
      }
    }
    else if(words[0].equals("call"))
    {
      try
      {
        //ARG precalc
        int num= Integer.valueOf(words[2]);
        //save return address
        bw.write(
          "@"+function+"$ret."+countt + "\n"+
          "D=A\n" +
          "@SP\n" +
          "A=M\n" +
          "M=D\n" +
          "@SP\n" +
          "M=M+1\n"
        );
        line+=7;
        num++;
        //save caller frame
        String[] frame={"@LCL\n","@ARG\n","@THIS\n","@THAT\n"};
        for(int i=0;i<4;i++)
        {
          bw.write(
            frame[i]+
            "D=M\n" +
            "@SP\n" +
            "A=M\n" +
            "M=D\n" +
            "@SP\n" +
            "M=M+1\n"
          );
          line+=7;
          num++;
        }
        //set ARG
        
        bw.write(
          "@SP\n" +
          "D=M\n" +
          "@"+num+"\n" +
          "D=D-A\n" +
          "@ARG\n" +
          "M=D\n"
        );
        line+=6;
        //JUMP!
        bw.write(
          "@"+words[1]+"\n"+
          "0;JMP\n"+
          "("+function+"$ret."+countt + ")\n"
        );
        line+=2;
        countt++;
      }
      catch(NumberFormatException e)
      {
        errorField.setText("Found non-integer in word 3 line "+line);
        e.printStackTrace();
        return;
      }
    }
    else if(words[0].equals("return"))
    {
      //set R13 with return address value
      bw.write(
        "@5\n" +
        "D=A\n" +
        "@LCL\n" +
        "A=M-D\n" +
        "D=M\n" +
        "@R13\n" +
        "M=D\n"
      );
      line+=6;
      //set ARG 0 to return value
      bw.write(
        "@SP\n" +
        "A=M-1\n" +
        "D=M\n" +
        "@ARG\n" +
        "A=M\n" +
        "M=D\n"
      );
      line+=6;
      //sets SP to ARG+1
      bw.write(
        "@ARG\n" +
        "D=M\n" +
        "@SP\n" +
        "M=D+1\n"
      );
      line+=4;
      //restores the caller segments
      String[] segments={"@THAT\n","@THIS\n","@ARG\n"};
      for(int i=0;i<3;i++)
      {
        bw.write(
          "@LCL\n" +
          "M=M-1\n" +
          "A=M\n"+
          "D=M\n"+
          segments[i] +
          "M=D\n"
        );
        line+=6;
      }
      //sets LCL
      bw.write(
        "@LCL\n" +
        "M=M-1\n" +
        "A=M\n" +
        "D=M\n" +
        "@LCL\n" +
        "M=D\n"
      );
      line+=6;
      //jumps to the return address
      bw.write(
        "@R13\n" +
        "A=M\n"+
        "0;JMP\n"
      );
      line+=3;
    }
  }
  
  private void readFile( String fileName)
  {
    
    try
    {
      File f = new File(fileName);
      Scanner scann=new Scanner(f);
      while(scann.hasNextLine())
      {
        String s= scann.nextLine();
        if(s.length() ==0) continue;
        if(s.split("/").length==0) continue;
        s=s.split("/")[0].trim();
        if(s.length() ==0) continue;
        String[] words= s.split(" ");
        try
        {
          if(words[0].equals("call")||words[0].equals("function")||words[0].equals("return"))
            functionCommand(words);
          else if(words.length>2)
            stackCommand(words);
          else if(words.length>1)
            branchCommand(words);
          else
            arithmeticCommand(words);
        }
        catch(Exception e)
        {
          errorField.setText("Couldn't write to output file!");
          return;
        }
      }
    }
    catch(Exception e)
    {
      errorField.setText("Can't find file: "+fileName);
      e.printStackTrace();
    }
  }
  
  public void actionPerformed(ActionEvent AE)
  {
    line=0;
    errorField.setText("Assembling~!");
    name = JTF.getText();
    boolean isDir=!(name.endsWith(".vm"));
    name=name.split("(.vm)")[0];
    String fileName="";
    boolean bootStrap=true;
    try
    {
      fw= new FileWriter(new File(name+".asm"));
      bw= new BufferedWriter(fw);
      //bootstrap
      //set SP 256
      if(bootStrap)
      {
        bw.write(
          "@256\n" +
          "D=A\n" +
          "@SP\n" +
          "M=D\n"
        );
        line+=4;
        String call="call Sys.init 0";
        String[] callW=call.split(" ");
        functionCommand(callW);
        bw.write("@"+(line)+"\n0;JMP\n");
      }
      if(!isDir)
      {
        fileName=name+".vm";
        readFile(fileName);
      }
      else
      {
        File folder= new File(name);
        for (final File fileEntry : folder.listFiles())
          if (fileEntry.getName().endsWith(".vm"))
          {
            name=fileEntry.getName().split("(.vm)")[0];
            readFile(fileEntry.getPath());
          }
      }
      if(!bootStrap)
        bw.write("@"+(line)+"\n0;JMP");
      bw.close();
      errorField.setText("Done!");
    }
    catch (Exception e)
    {
      errorField.setText("Couldn't write to output file!");
      return;
    }
  }
  
}
