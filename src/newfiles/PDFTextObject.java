package newfiles;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.fontbox.cmap.CMap;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSFloat;
import org.apache.pdfbox.cos.COSInteger;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.util.PDFOperator;

public class PDFTextObject {
  ArrayList<Object> attributes = new ArrayList<Object>();
  String text = "";
  boolean nexttf = false;
  boolean nexttm = false;
  Map<String,PDFont> fonts = new HashMap<>();
  private PDFTextStyle objStyle = new PDFTextStyle();
  PDFont currFont = null;
  
  public PDFTextStyle getObjStyle(){
    return this.objStyle;
  }
  
  public class PDFTextStyle {
    private double tf = 0; //Font size
    private PDFont font = null; //Font
    private double[] tm = {0,0,0,0,0,0};
    
    public void setTf(double _tf){
      this.tf = _tf;
    }
    
    public void setFont(PDFont _font){
      this.font = _font;
    }
    
    public void setTm(double[] _tm){
      this.tm = _tm;
    }
    
    public double getTf(){
      return this.tf;
    }
    
    public PDFont getFont(){
      return this.font;
    }
    
    public double[] getTm(){
      return this.tm;
    }
    
    public double getTm(int index){
      return this.tm[index];
    }
    
    public boolean isTmZero(){
      boolean ret = true;
      for(double t : getTm()){
        ret = ret && (t == 0);
      }
      
      return ret;
    }
    
    public double getSize(){
      if(isTmZero())      //Wenn keine Transformationsmatrix gesetzt ist, wird das Ergebnis rein aus der Tf geschlossen
        return getTf();
      else
        return getTf()*getTm(3);
    }
    
    public PDFTextStyle copy(){
      PDFTextStyle copy = new PDFTextStyle();
      copy.setTf(this.getTf());
      copy.setTm(this.getTm());
      copy.setFont(this.getFont());
      
      return copy;
    }
  }
  
  PDFTextObject(Map<String,PDFont> fts){
    this.fonts = fts;
  }
  
  PDFTextObject(PDFTextObject obj){
    //Kopiere alle Einträge aus obj.fonts
    for(Map.Entry<String, PDFont> tFontSet : obj.fonts.entrySet()){
      this.fonts.put(tFontSet.getKey(), tFontSet.getValue());
    }
    this.objStyle = obj.getObjStyle().copy();
    this.currFont = obj.currFont;
  }
  
  public void add(Object attr){
    attributes.add(attr);
  }
  
  public String getText(){
    return text;
  }

  public void setFonts(Map<String,PDFont> _fonts){
    this.fonts = _fonts;
  }
  
  public String parseCOSString(COSString thisString, String text){
    if(this.currFont == null || this.currFont.getToUnicodeCMap() == null){
      text += thisString.getString();
    }else{
      text += parseUnicodeCMap(this.currFont.getToUnicodeCMap(),thisString.getString());
    }
    return text;
  }
  
  public void procText() throws IOException{
    String op;
    ArrayList<Object> flushObjects = new ArrayList<Object>();
    COSName fontName;
    
    for(Object attr : attributes){
      if(attr instanceof PDFOperator){
        op = ((PDFOperator) attr).getOperation();
        if(op.equals("Tf")){ //{selectfont} Set text font and size
          System.out.println("Für Tf wurde folgende Größe gefunden: " + flushObjects.size());
          for(Object obj : flushObjects){
            System.out.println("\t" + obj.toString());
          }
          
          if(flushObjects.size() == 2 && flushObjects.get(0) instanceof COSName && (flushObjects.get(1) instanceof COSInteger || flushObjects.get(1) instanceof COSFloat)){
            fontName = ((COSName) flushObjects.get(0));
            System.out.println("Der folgende fontName wurde gefunden: " + fontName);
            this.currFont = this.fonts.get(fontName.getName()); //Wenn kein Font vorhanden ist, ist currFont = null
            System.out.println("Es wurde folgender Font zugewiesen: " + this.currFont);
            
            this.objStyle.setTf(getCOSnumericValue(flushObjects.get(1)));
            
            
          }
        }else if (op.equals("Tm")){ //Set text matrix and text line matrix
          if(flushObjects.size() == 6){
                double[] tmtemp = {  getCOSnumericValue(flushObjects.get(0)),
                    getCOSnumericValue(flushObjects.get(1)),
                    getCOSnumericValue(flushObjects.get(2)),
                    getCOSnumericValue(flushObjects.get(3)),
                    getCOSnumericValue(flushObjects.get(4)),
                    getCOSnumericValue(flushObjects.get(5))};
                this.objStyle.setTm(tmtemp);
          }
        }else if (op.equals("Td") || op.equals("TD")){
        	//Wenn ein Zeilenumbruch stattfindet, wird im Text ein ' ' ergänzt.
        	if(flushObjects.size() == 2 && flushObjects.get(1) instanceof COSFloat && (((COSFloat) flushObjects.get(1)).doubleValue() != 0)){
        		text += " ";
        	}
        }else if(op.equals("TJ")){ //Show text, allowing individual glyph positioning
          //Vor einem TJ wird der Text und die Positionierung mittels eines COSArray dargestellt
          if(flushObjects.size() == 1 && flushObjects.get(0) instanceof COSArray){
            COSArray thisArray = (COSArray) flushObjects.get(0);
            for(COSBase thisObject : thisArray){
              if(thisObject instanceof COSInteger){ //Es handelt sich bei dem Element zum eine Verschiebungsangabe
                //Da die Werte subtrahiert werden, stehen negative Werte für Verschiebungen nach rechts
                //Wenn eine Verschiebung kleiner als -150 ist, wird dies als Leerzeichen gewertet
                if(((COSInteger) thisObject).intValue() < -150){
                  text += " ";
                }
              }else if(thisObject instanceof COSString){ //Es handelt sich bei dem Element um einen String
                text = parseCOSString((COSString) thisObject, text);
              }
            }
          }
        }else if(op.equals("Tj")){ //Show text
          if(flushObjects.size() == 1 && flushObjects.get(0) instanceof COSString){
            text = parseCOSString((COSString) flushObjects.get(0), text);
          }
        }
        flushObjects = new ArrayList<Object>();
      }else{
        flushObjects.add(attr);
      }
    }
  }
  
  protected String parseUnicodeCMap(CMap map, String input){
    String output = "";
    byte[] inbytes = input.getBytes();
    
    /* Es wird überprüft, welche Codierungsform vorliegt.
     * Je nachdem, wird die entsprechende Funktion aufgerufen.
     */
    if(map.hasTwoByteMappings()){
      for(int i = 0; (i+1) < inbytes.length; i = i+2){
        output = output + map.lookup(input.getBytes(), i, 2);
      }
    }else if(map.hasOneByteMappings()){
      for(int i = 0; i < inbytes.length; i++){
        output = output + map.lookup(input.getBytes(), i, 1);
      }
    }else if(map.hasCIDMappings()){
      for(int i = 0; i < inbytes.length; i++){
        output = output + map.lookupCID(input.getBytes(), i, 1);
      }
    }
    return output;
  }
  
  public boolean isCOSnumeric(Object o){
	  return (o instanceof COSFloat || o instanceof COSInteger);
  }
  
  public static double getCOSnumericValue(Object o) throws IOException{
	  if(o instanceof COSFloat){
		  return ((COSFloat) o).doubleValue();
	  }else if(o instanceof COSInteger){
		  return ((COSInteger) o).doubleValue();
	  }else{
		  throw (new IOException());
	  }
  }
}
