/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jackcompiler;

import java.io.*;
import java.util.*;
import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
/**
 *
 * @author jul
 */

class Instruction
{
  int type;
  String word;
  static final int KEYWORD=0;
  static final int SYMBOL=1;
  static final int INTEGER=2;
  static final int STRING=3;
  static final int IDENTIFIER=4;
  void set(int t,String w)
  {
    type=t;
    word=w;
  }
}

class Variable
{
  String name;
  String type;
  int category;
  int index;
  static int STATIC=0;
  static int FIELD=1;
  static int ARGUMENT=2;
  static int LOCAL=3;
}

class Parser
{
  static String keywords[]={
    "class" , "constructor" , "function" , "method" ,
    "field" , "static" , "var" , "int" , "char" , "boolean" ,
    "void" , "true" , "false" , "null" , "this" , "let" , "do" ,
    "if" , "else" , "while" , "return"
  };
  static char symbols[]={
    '{','}','(',')','[',']','.',',',';','+','-','*','/','&','|','<','>','=','~'
  };
  
  static String chars=" !\"#$%&'()*+,-./"
          + "0123456789"
          + ":;<=>?@"
          + "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
          + "[/]^_`"
          + "abcdefghijklmnopqrstuvwxyz"
          + "{|}~";
  
  static Scanner scann;
  static BufferedWriter bw;
  static String line;
  static Instruction inst;
  static int spaces;
  
  static HashMap<String,Variable> classVars = new HashMap<String,Variable>();
  static HashMap<String,Variable> localVars = new HashMap<String,Variable>();
  
  static String className;
  static String functionName;
  static String functionType;
  static int staticVarCount;
  static int fieldVarCount;
  static int localVarCount;
  static int argumentVarCount;
  static boolean isDefinition;
  static int labelCount;
  
  static void getNextToken() throws Exception
  {
    boolean isMultComment=false;
    do
    {
      line=line.trim();
      while(!line.isEmpty())
      {
        line=line.trim();
        //comments
        if(line.startsWith("//"))
        {
          line="";
          continue;
        }
        //multi-line comment
        else if(line.startsWith("/*")||isMultComment)
        {
          isMultComment=!line.contains("*/");
          if(!isMultComment)
            line=line.substring(line.indexOf("*/")+2);
          else
            line="";
          continue;
        }
        //keywords
        for(int i=0;i<keywords.length;i++)
          if(line.startsWith(keywords[i])
          &&!Character.isAlphabetic(line.charAt(keywords[i].length())))
          {
            inst.set(Instruction.KEYWORD,keywords[i]);
            line=line.substring(keywords[i].length());
            return;
          }
        //symbols
        for(int i=0;i<symbols.length;i++)
          if(line.charAt(0)==symbols[i])
          {
            String symbol=symbols[i]+"";
            inst.set(Instruction.SYMBOL,symbol);
            line=line.substring(1);
            return;
          }
        //integer constant
        int num=0;
        int countt=0;
        boolean ok=false;
        for(int i=0;line.charAt(i)>='0'&&line.charAt(i)<='9'&&i<line.length();i++)
        {
          num=num*10;
          num+=line.charAt(i)-'0';
          ok=true;
          countt++;
        }
        if(ok)
        {
          inst.set(Instruction.INTEGER,num+"");
          line=line.substring(countt);
          return;
        }
        //string constant
        if(line.charAt(0)=='"')
        {
          String s= line.substring(1,line.indexOf('"',1));
          inst.set(Instruction.STRING, s);
          line=line.substring(line.indexOf('"',1)+1);
          return;
        }
        //identifier
        String identifier=line.split("[{}\\(\\)\\[\\]\\.,;\\+\\-\\*\\/&\\|<>=~]")[0].split(" ")[0];
        inst.set(Instruction.IDENTIFIER, identifier);
        line=line.substring(identifier.length());
        return;
      }
      line= scann.nextLine().trim();
    }while(scann.hasNextLine());
    return;
  }
  static void write(String tag,String extra, boolean singleLine) throws Exception
  {
    for(int i=0;i<spaces;i++)
      bw.write(' ');
    if(!singleLine)
    {
      bw.write("<");
      bw.write(tag);
      bw.write(">\n");
    }
    else
    {
      if(extra.equals("<"))
        extra="&lt;";
      else if(extra.equals(">"))
        extra="&gt;";
      else if(extra.equals("\""))
        extra="&quot;";
      else if(extra.equals("&"))
        extra="&amp;";
      bw.write("<");
      bw.write(tag);
      bw.write("> ");
      bw.write(extra);
      bw.write(" </");
      bw.write(tag);
      bw.write(">\n");
    }
  }
  
  static void compileBasic(boolean getNew) throws Exception
  {
    if(getNew)
      getNextToken();
  }
  
  static void pushVar(String name) throws Exception
  {
    Variable var;
    String type;
    if(localVars.containsKey(name))
      var = localVars.get(name);
    else if(classVars.containsKey(name))
      var = classVars.get(name);
    else
      throw new Exception("Couldn't find variable with name "+name);
    if(var.category==Variable.ARGUMENT)
      type= "argument ";
    else if (var.category==Variable.LOCAL)
      type= "local ";
    else if (var.category==Variable.STATIC)
      type= "static ";
    else
      type= "this ";
    bw.write("push "+ type+var.index + "\n");
  }
  static void popVar(String name) throws Exception
  {
    Variable var;
    String type;
    if(localVars.containsKey(name))
      var = localVars.get(name);
    else if(classVars.containsKey(name))
      var = classVars.get(name);
    else
      throw new Exception("Couldn't find variable with name "+name);
    if(var.category==Variable.ARGUMENT)
      type= "argument ";
    else if (var.category==Variable.LOCAL)
      type= "local ";
    else if (var.category==Variable.STATIC)
      type= "static ";
    else
      type= "this ";
    bw.write("pop "+ type+var.index + "\n");
  }
  //done
  static void compileSubroutineCall(String functionName,boolean getNew, boolean writeIdent) throws Exception
  {
    if(getNew)
      getNextToken();
    
    if(writeIdent)
      if(inst.type!=Instruction.IDENTIFIER)
        throw new Exception("Subroutine call to "+functionName+ " must start with subroutine name");
    
    String name= inst.word;
    if(!writeIdent)
      name=functionName;
    compileBasic(writeIdent);//write . or (
    int countt=0;
    if(inst.word.equals("("))
    {
      name = className + "." +name;
      if(functionType.equals("method")||functionType.equals("constructor"))
      {
        bw.write("push pointer 0\n");
        countt++;
      }
      countt+=compileExpressionList();
      if(!inst.word.equals(")"))
        throw new Exception("argument list must end with )");
    }
    else if(inst.word.equals("."))
    {
      isDefinition=true;
      compileBasic(true);//get function name;
      if(inst.type!=Instruction.IDENTIFIER)
        throw new Exception("Subroutine call to "+functionName+ " must start with subroutine name, instead is "+inst.word);
      if(localVars.containsKey(name))
      {
        pushVar(name);
        name=localVars.get(name).type;
        countt++;
      }
      else if(classVars.containsKey(name))
      {
        pushVar(name);
        name=classVars.get(name).type;
        countt++;
      }
      name=name+"."+inst.word;
      
      compileBasic(true);//get (
      if(!inst.word.equals("("))
        throw new Exception("argument list must begin with (");
      countt+=compileExpressionList();
      if(!inst.word.equals(")"))
        throw new Exception("argument list must end with )");
    }
    else
      throw new Exception("Expected . or (");
    bw.write("call "+name+" "+countt+"\n");
  }
  //done
  static int compileExpressionList() throws Exception
  {
    getNextToken();
    if(inst.word.equals(")"))
      return 0;
    compileExpression(false);
    int countt=1;
    while(true)
    {
      if(inst.word.equals(","))
      {
        compileBasic(false);//write the ,
        compileExpression(true);
        countt++;
      }
      else
        break;
    }
    return countt;
  }
  //done
  static void compileTerm(boolean getNew) throws Exception
  {
    if(getNew)
      getNextToken();
    if(inst.type!=Instruction.IDENTIFIER&&inst.type!=Instruction.SYMBOL)
    {
      //write the keyword,integer or string
      //if integer
      if(inst.type==Instruction.INTEGER)
        bw.write("push constant "+inst.word+ "\n");
      //if keyword
      else if(inst.type==Instruction.KEYWORD)
      {
        if(inst.word.equals("this"))
          bw.write("push pointer 0\n");
        else if(inst.word.equals("null")||inst.word.equals("false"))
          bw.write("push constant 0\n");
        else if(inst.word.equals("true"))
        {
          bw.write("push constant 1\n");
          bw.write("neg\n");
        }
        else
          throw new Exception("Can't use keyword "+inst.word+" in a term!");
      }
      //if string
      else
      {
        bw.write("push constant " +inst.word.length()+ "\n");
        bw.write("call String.new 1\n");
        for(int i=0;i<inst.word.length();i++)
        {
          int charCode= chars.indexOf(inst.word.charAt(i))+32;
          bw.write("push constant "+charCode+ "\n");
          bw.write("call String.appendChar 2\n");
        }
      }
      getNextToken();
    }
    else if(inst.type==Instruction.IDENTIFIER)
    {
      compileBasic(false);//write the identifier
      String name = inst.word;
      getNextToken();
      if(inst.word.equals("(")||inst.word.equals("."))
      {
        compileSubroutineCall(name,false,false);
        getNextToken();
        return;
      }
      pushVar(name);
      if(inst.word.equals("["))
      {
        compileExpression(true);
        if(!inst.word.equals("]"))
          throw new Exception("Expected ]");
        bw.write("add\n");
        bw.write("pop pointer 1\n");
        bw.write("push that 0\n");
        getNextToken();
      }
    }
    else if(inst.word.equals("("))
    {
      compileExpression(true);
      if(!inst.word.equals(")"))
        throw new Exception("Expected ) at end of term");
      getNextToken();
    }
    else if("-~".contains(inst.word))
    {
      String symbol= inst.word;
      compileTerm(true);
      if(symbol.equals("-"))
        bw.write("neg\n");
      else
        bw.write("not\n");
    }
  }
  //done
  static void compileExpression(boolean getNew) throws Exception
  {
    compileTerm(getNew);
    while(true)
    {
      if(inst.word.length()==0 || !"+-*/&|<>=".contains(inst.word))
        break;
      String symbol= inst.word;
      compileTerm(true);
      //+-&|<>= easy cases
      if("+".equals(symbol))
        bw.write("add\n");
      else if("-".equals(symbol))
        bw.write("sub\n");
      else if("&".equals(symbol))
        bw.write("and\n");
      else if("|".equals(symbol))
        bw.write("or\n");
      else if("<".equals(symbol))
        bw.write("lt\n");
      else if(">".equals(symbol))
        bw.write("gt\n");
      else if("=".equals(symbol))
        bw.write("eq\n");
      //* and / call OS functions
      else if("*".equals(symbol))
        bw.write("call Math.multiply 2\n");
      else if("/".equals(symbol))
        bw.write("call Math.divide 2\n");
      else
        throw new Exception("Couldn't find symbol "+symbol);
    }
  }
  
  //done
  static void compileReturnStatement() throws Exception
  {
    getNextToken();
    if(!inst.word.equals(";"))
      compileExpression(false);
    if(!inst.word.equals(";"))
      throw new Exception("Expected ; at end of return statement");
    bw.write("return\n");
  }
  //done
  static void compileDoStatement() throws Exception
  {
    compileSubroutineCall(null,true,true);
    compileBasic(true);//write ;
    bw.write("pop temp 1\n");
    if(!inst.word.equals(";"))
      throw new Exception("Expected ; at end of do statement");
  }
  //done
  static void compileWhileStatement() throws Exception
  {
    bw.write("label L"+labelCount+"\n");
    compileBasic(true);//write the (
    if(!inst.word.equals("("))
      throw new Exception("Expected ( after while");
    compileExpression(true);
    
    if(!inst.word.equals(")"))
      throw new Exception("Expected ) after while cond");
    compileBasic(true);//write the {
    bw.write("not\n");
    bw.write("if-goto L"+(labelCount+1)+"\n");
    int lab= labelCount;
    labelCount+=2;
    
    if(!inst.word.equals("{"))
      throw new Exception("Expected { before statements block");
    compileStatements(true);
    
    if(!inst.word.equals("}"))
      throw new Exception("Expected } after statements block");
    bw.write("goto L"+lab+"\n");
    bw.write("label L"+(lab+1)+"\n");
  }
  //done
  static void compileIfStatement() throws Exception
  {
    compileBasic(true);//write the (
    if(!inst.word.equals("("))
      throw new Exception("Expected ( after if");
    compileExpression(true);
    
    if(!inst.word.equals(")"))
      throw new Exception("Expected ) after if cond");
    bw.write("not\n");
    bw.write("if-goto L"+labelCount+ "\n");
    int lab=labelCount;
    labelCount+=2;
    
    compileBasic(true);//write the {
    if(!inst.word.equals("{"))
      throw new Exception("Expected { before statements block");
    
    compileStatements(true);
    
    if(!inst.word.equals("}"))
      throw new Exception("Expected } after statements block");
    bw.write("goto L"+(lab+1)+"\n");
    bw.write("label L"+lab+"\n");
    
    //else part
    getNextToken();
    if(inst.word.equals("else"))
    {
      compileBasic(true);//write the {
      if(!inst.word.equals("{"))
        throw new Exception("Expected { before statements block");
      
      compileStatements(true);
      
      if(!inst.word.equals("}"))
        throw new Exception("Expected } after statements block");
      getNextToken();
    }
    bw.write("label L"+(lab+1)+"\n");
  }
  //done
  static void compileLetStatement() throws Exception
  {
    compileBasic(true);//write the identifier
    if(inst.type!=Instruction.IDENTIFIER)
      throw new Exception("Must specify variable");
    
    boolean isArrayEntry=false;
    String name= inst.word;
    getNextToken();
    if(inst.word.equals("["))
    {
      pushVar(name);
      isArrayEntry=true;
      compileExpression(true);
      if(!inst.word.equals("]"))
        throw new Exception("Expected ]");
      bw.write("add\n");
      getNextToken();
    }
    if(!inst.word.equals("="))
      throw new Exception("Expected =");
    
    compileExpression(true);
    
    if(!inst.word.equals(";"))
      throw new Exception("Expected ;");
    bw.write("pop temp 0\n");
    if(isArrayEntry)
      bw.write("pop pointer 1\n");
    bw.write("push temp 0\n");
    if(isArrayEntry)
      bw.write("pop that 0\n");
    else
      popVar(name);
  }
  //done
  static void compileStatements(boolean getNew) throws Exception
  {
    if(getNew)
      getNextToken();
    while(true)
    {
      if(inst.word.equals("let"))
        compileLetStatement();
      else if(inst.word.equals("if"))
      {
        compileIfStatement();
        continue;
      }
      else if(inst.word.equals("while"))
        compileWhileStatement();
      else if(inst.word.equals("do"))
        compileDoStatement();
      else if(inst.word.equals("return"))
        compileReturnStatement();
      else break;
      getNextToken();
    }
  }
  
  //done
  static boolean compileVarDec() throws Exception
  {
    getNextToken();
    if(inst.word.equals("var"))
    {
      isDefinition=true;
      Variable var = new Variable();
      var.index=localVarCount;
      localVarCount++;
      var.category = Variable.LOCAL;
      
      
      isDefinition=true;
      compileBasic(true);//get the type part
      if(inst.type!=Instruction.IDENTIFIER
      &&!inst.word.equals("int")&&!inst.word.equals("boolean")
      &&!inst.word.equals("char"))
        throw new Exception("Must have int, char, boolean or identifier as type");
      var.type=inst.word;
      
      compileBasic(true);//get the name;
      if(inst.type!=Instruction.IDENTIFIER)
        throw new Exception("Variable name must start with _ or letter");
      var.name = inst.word;
      localVars.put(var.name, var);
      
      while(true)
      {
        Variable extra = new Variable();
        getNextToken();
        if(!inst.word.equals(","))
          break;
        else
        {
          compileBasic(false);//get the ,
          isDefinition=true;
          compileBasic(true);//identifier
          
          extra.name=inst.word;
          extra.category= Variable.LOCAL;
          extra.type=var.type;
          extra.index=localVarCount;
          localVarCount++;
          localVars.put(extra.name, extra);
          
          if(inst.type!=Instruction.IDENTIFIER)
            throw new Exception("Variable name must start with _ or letter");
        }
      }
      if(!inst.word.equals(";"))
        throw new Exception("field/static must end with ;");
      
      return true;
    }
    return false;
  }
  //done
  static void compileSubroutineBody() throws Exception
  {
    compileBasic(true);//get {
    if(!inst.word.equals("{"))
      throw new Exception("Subroutine body must start with {");
    while(compileVarDec());
    
    bw.write("function "+className+"."+functionName+" "+localVarCount+"\n");
    if(functionType.equals("constructor"))
    {
      bw.write("push constant "+fieldVarCount+"\n");
      bw.write("call Memory.alloc 1\n");
      bw.write("pop pointer 0\n");
    }
    else if(functionType.equals("method"))
    {
      bw.write("push argument 0\n");
      bw.write("pop pointer 0\n");
    }
    compileStatements(false);
    if(!inst.word.equals("}"))
      throw new Exception("Subroutine body must end with }");
  }
  //done
  static void compileParameterList() throws Exception
  {
    getNextToken();
    
    if(inst.type!=Instruction.IDENTIFIER
    &&!inst.word.equals("int")&&!inst.word.equals("boolean")
    &&!inst.word.equals("char"))
      return;
    Variable var = new Variable();
    var.category=Variable.ARGUMENT;
    
    isDefinition=true;
    compileBasic(false);//get first parameter type
    var.type=inst.word;
    isDefinition=true;
    compileBasic(true);//get first parameter name
    var.name=inst.word;
    
    var.index=argumentVarCount;
    argumentVarCount++;
    localVars.put(var.name, var);
    
    if(inst.type!=Instruction.IDENTIFIER)
      throw new Exception("Variable name must start with _ or letter");
    getNextToken();
    while(inst.word.equals(","))
    {
      Variable extra = new Variable();
      extra.category=var.category;
      
      isDefinition=true;
      compileBasic(true);//get second parameter type
      
      if(inst.type!=Instruction.IDENTIFIER
      &&!inst.word.equals("int")&&!inst.word.equals("boolean")
      &&!inst.word.equals("char"))
        throw new Exception("Must have int,char,boolean or identifier as type");
      extra.type=inst.word;
      
      isDefinition=true;
      compileBasic(true);//get second parameter name
      if(inst.type!=Instruction.IDENTIFIER)
        throw new Exception("Variable name must start with _ or letter");
      extra.name=inst.word;
      
      extra.index=argumentVarCount;
      argumentVarCount++;
      
      localVars.put(extra.name, extra);
      
      getNextToken();
    }
    return;
  }
  //done
  static boolean compileSubroutineDec(boolean getNew) throws Exception
  {
    if(getNew) getNextToken();
    if(inst.word.equals("constructor")||inst.word.equals("function")||inst.word.equals("method"))
    {
      localVarCount=0;
      argumentVarCount=0;
      localVars.clear();
      functionType=inst.word;
      
      isDefinition=true;
      compileBasic(true);//get the type part
      
      if(inst.type!=Instruction.IDENTIFIER
      &&!inst.word.equals("int")&&!inst.word.equals("boolean")
      &&!inst.word.equals("char")&&!inst.word.equals("void"))
        throw new Exception("Must have int,char,boolean,void or identifier as return type in signature");
      if(functionType.equals("method"))
      {
        Variable var = new Variable();
        var.category = Variable.ARGUMENT;
        var.index= 0;
        var.type=inst.word;
        var.name="this";
        argumentVarCount++;
        localVars.put(var.name, var);
      }
      
      isDefinition=true;
      compileBasic(true);//get the name;
      functionName=inst.word;
      if(inst.type!=Instruction.IDENTIFIER)
        throw new Exception("Function name must start with _ or letter");
      
      compileBasic(true);//get the (
      if(!inst.word.equals("("))
        throw new Exception("parameter list must start with (");
      compileParameterList();//get the parameter list
      
      if(!inst.word.equals(")"))
        throw new Exception("parameter list must end with )");
      compileSubroutineBody();
      
      return true;
    }
    return false;
  }
  //done
  static boolean compileClassVarDec() throws Exception
  {
    getNextToken();
    if(inst.word.equals("static")||inst.word.equals("field"))
    {
      Variable var = new Variable();
      
      if(inst.word.equals("static"))
        var.category=Variable.STATIC;
      else if(inst.word.equals("field"))
        var.category=Variable.FIELD;
      
      isDefinition=true;
      compileBasic(true);//get the type part
      if(inst.type!=Instruction.IDENTIFIER
      &&!inst.word.equals("int")&&!inst.word.equals("boolean")
      &&!inst.word.equals("char"))
        throw new Exception("Must have int,char,boolean or identifier as type");
      var.type=inst.word;
      
      isDefinition=true;
      compileBasic(true);//get the name;
      if(inst.type!=Instruction.IDENTIFIER)
        throw new Exception("Variable name must start with _ or letter");
      var.name=inst.word;
      if(var.category==Variable.STATIC)
      {
        var.index=staticVarCount;
        staticVarCount++;
      }
      else
      {
        var.index=fieldVarCount;
        fieldVarCount++;
      }
      
      classVars.put(var.name,var);
      
      while(true)
      {
        Variable extra = new Variable();
        getNextToken();
        if(!inst.word.equals(","))
          break;
        else
        {
          isDefinition=true;
          compileBasic(true);//write identifier
          if(inst.type!=Instruction.IDENTIFIER)
            throw new Exception("Variable name must start with _ or letter");
          extra.name=inst.word;
          extra.category=var.category;
          extra.type=var.type;
          if(var.category==Variable.STATIC)
          {
            extra.index=staticVarCount;
            staticVarCount++;
          }
          else
          {
            extra.index=fieldVarCount;
            fieldVarCount++;
          }
          
          classVars.put(extra.name,extra);
        }
      }
      if(!inst.word.equals(";"))
        throw new Exception("field/static must end with ;");
      
      return true;
    }
    return false;
  }
  //done
  static void compileClass() throws Exception
  {
    fieldVarCount=0;
    staticVarCount=0;
    classVars.clear();
    isDefinition=true;
    
    compileBasic(true);//get identifier
    if(inst.type!=Instruction.IDENTIFIER)
      throw new Exception("Class must have name");
    className=inst.word;
    
    compileBasic(true);//get {
    if(!inst.word.equals("{"))
      throw new Exception("Class must have name and be followed by {");
    while(compileClassVarDec());
    boolean ok= compileSubroutineDec(false);
    while(ok&&compileSubroutineDec(true));
    compileBasic(false);
    if(!inst.word.equals("}"))
      throw new Exception("Class must end with }");
  }
  
  //done
  static void compile(String file,String name) throws Exception
  {
    FileWriter fw= new FileWriter(new File(name+".vm"));
    bw= new BufferedWriter(fw);
    scann = new Scanner(new File(file));
    inst= new Instruction();
    line="";
    getNextToken();
    if(inst.word.equals("class"))
      compileClass();     
    bw.close();
  }
}

public class JackCompiler extends JFrame implements ActionListener
{
  JTextField JTF;
  JButton JB;
  JTextField errorField;
  
  String name;
  
  FileWriter fw;
  BufferedWriter bw;
  
  JackCompiler()
  {
    super("JackCompiler (Tokenizer for now)");
    setLayout(new BoxLayout(getContentPane(),BoxLayout.Y_AXIS));
    JTF = new JTextField("Insert file name/dir here");
    JB=new JButton("Compile~!");
    JB.addActionListener(this);
    errorField= new JTextField("tasty test~");
    errorField.setForeground(Color.RED);
    errorField.setEditable(false);
    add(JTF);
    add(JB);
    add(errorField);
    pack();
    setLocationRelativeTo(null);
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
  }
  
  public static void main(String[] args)
  {
    SwingUtilities.invokeLater(
      ()->
      {
        GUI();
      }
    );
  }
  
  private static void GUI()
  {
    JackCompiler JC= new JackCompiler();
    JC.setVisible(true);
  }
  
  public void actionPerformed(ActionEvent AE)
  {
    errorField.setText("Compiling~!");
    name = JTF.getText();
    boolean isDir = !(name.endsWith(".jack"));
    name = name.split("(.jack)")[0];
    String fileName="";
    try
    {
      try
      {
        if(!isDir)
        {
          fileName=name+".jack";
          Parser.compile(fileName,name);
        }
        else
        {
          File folder= new File(name);
          for (final File fileEntry : folder.listFiles())
            if (fileEntry.getName().endsWith(".jack"))
            {
              name=fileEntry.getName().split("(.jack)")[0];
              Parser.compile(fileEntry.getPath(),name);
            }
        }
        errorField.setText("Done!");
      }
      catch(IOException ee)
      {
        errorField.setText("Couldn't read file "+name);
        ee.printStackTrace();
      }
      catch(Exception ee)
      {
        ee.printStackTrace();
        errorField.setText(ee.getMessage());
        System.out.println(ee.getMessage());
        Parser.bw.close();
      }
    }
    catch (Exception e)
    {
      errorField.setText("Cannot write in file!");
      e.printStackTrace();
    }
  }
}