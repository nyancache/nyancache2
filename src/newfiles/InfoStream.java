package newfiles;

import java.awt.Color;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.html.HTMLEditorKit;

public class InfoStream extends FileWriter{
  
  String streamFilename;
  Document textDocument;
  JTextPane pane;
  Element root;
  boolean textOut;
  HTMLEditorKit kit;
  public static SimpleAttributeSet RED = getRed();
  public static SimpleAttributeSet YELLOW = getYellow();
  public static SimpleAttributeSet BOLD = getBold();
  
  InfoStream(String streamFilename) throws IOException{
    super(streamFilename);
    textOut = false;
  }

  public void addDocument(Document doc){
    this.textDocument = doc;
    textOut = true;
  }
  
  public void addPane(JTextPane p){
    this.pane = p;
  }
  
  public void print(String output) throws BadLocationException, IOException{
    this.write(output + "\n");
    if(textOut)
      this.textDocument.insertString(textDocument.getLength(), output, new SimpleAttributeSet());
  }
  
  public void printBold(String output) throws BadLocationException, IOException{
    this.write(output + "\n");
    if(textOut)
      this.textDocument.insertString(textDocument.getLength(), output, BOLD);
  }
  
  public void warning(String output) throws BadLocationException, IOException{
    this.write("WARNUNG: " + output + "\n");
    if(textOut)
      this.textDocument.insertString(textDocument.getLength(), "WARNUNG: " + output + "\n\n", YELLOW);
  }
  
  public void error(String output) throws BadLocationException, IOException{
    this.write("FEHLER: " + output + "\n");
    if(textOut)
      this.textDocument.insertString(textDocument.getLength(), "Fehler: " + output, RED);
  }
  
  public void error(StackTraceElement[] ste) throws BadLocationException, IOException{
	  String output = "";
	  for(StackTraceElement thisSte : ste){
		  output += "Fehler in Datei " + thisSte.getFileName() + "\n"
				  + "in Klasse " + thisSte.getClassName() + "\n"
				  + "in Methode " + thisSte.getMethodName() + "\n"
				  + "in Zeile " + thisSte.getLineNumber() + "\n\n";
	  }
	    this.write("FEHLER: " + output + "\n");
	    if(textOut)
	      this.textDocument.insertString(textDocument.getLength(), "Fehler: " + output, RED);
  }
  
  private static SimpleAttributeSet getRed(){
    SimpleAttributeSet red = new SimpleAttributeSet();
    StyleConstants.setForeground(red, Color.RED);
    return red;
  }
  
  private static SimpleAttributeSet getYellow(){
    SimpleAttributeSet yellow = new SimpleAttributeSet();
    StyleConstants.setForeground(yellow,Color.getHSBColor((float) 0.15, (float)1.0, (float)0.7));
    return yellow;
  }
  
  private static SimpleAttributeSet getBold(){
    SimpleAttributeSet bold = new SimpleAttributeSet();
    StyleConstants.setBold(bold, true);
    return bold;
  }
  
  public void close() throws IOException{
    super.flush();
    super.close();
  }
}
