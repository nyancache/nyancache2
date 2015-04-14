package newfiles;

import java.io.File;
import java.io.Serializable;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

public class Entry implements Serializable{

  private static final long serialVersionUID = 2;
  
  private int      ID = 0;
  private int      refID = 0;
  private File    pdfFile; //Mit dem Eintrag verbundene PDF-Datei
  private long    size = 0; //Gr��e der verbundenen PDF-Datei
  private Type    type = Type.INDEF;
  private String[]  author = {""}; //Es werden mehrere Autoren separat gespeichert
  private String    title = "";
  private String    journal = "";
  private String    booktitle = "";
  private String    publisher = "";
  private String[]  editor = {""}; //Es werden mehrere Editoren separat gespeichert
  private  int      year = 0;
  private  int[]    pages= {0,0}; //Enth�lt zwei Eintr�ge, Beginn und Ende
  
  enum Type{
    INBOOK,
    TECHREPORT,
    ARTICLE,
    BOOK,
    MISC,
    INPROCEEDINGS,
    PHDTHESIS,
    INDEF
  }
  
  
  Entry(){
    
  }
  
  Entry(String path){
    this.pdfFile = new File(path);
    this.size = this.pdfFile.length();
  }
  
  Entry(File PDF){
    this.pdfFile = PDF;
    this.size = PDF.length();
  }
  
  //Auslesefunktionen
  
  public int getRefID(){
    return this.refID;
  }
  
  public int getID(){
    return this.ID;
  }
  
  public File getPDF(){
    return this.pdfFile;
  }
  
  public long getSize(){
    return this.size;
  }
  
  public Type getType(){
    return this.type;
  }

  public String[] getAuthor(){
    return this.author;
  }
  
  public String getTitle(){
    return this.title;
  }
  
  public String getJournal(){
    return this.journal;
  }
  
  public String getBooktitle(){
    return this.booktitle;
  }
  
  public String getPublisher(){
    return this.publisher;
  }
  
  
  public String[] getEditor(){
    return this.editor;
  }
  
  public int getYear(){
    return this.year;
  }
  
  public int[] getPages(){
    return this.pages;
  }
  
  public boolean checkPDF(){ //Überprüft, ob tatsächlich eine PDF-Datei verbunden ist
    String[] s;
    if(this.getSize() == 0) return false;
    if((s = StringUtils.split(this.getPDF().getName(),'.'))[s.length-1].toLowerCase().equals("pdf")) return true;
    else return false;
  }
  //Einlesefunktionen
  public void setID(int i){
    this.ID = i;
  }
  
  public void setPDF(String intPath){
    //"internal-pdf:/" entfernen
    intPath = StringUtils.removeStart(intPath, "internal-pdf:/");
    
    //Datei aufrufen
    this.pdfFile = new File("DB/Literatur.Data/PDF" + intPath);
    this.size = this.pdfFile.length();
  }
  
  public void setType(Type t){
    this.type = t;
  }
  
  public void setAuthor(String a){
    int size;
    
    this.author = a.split("(and)");
    size = this.author.length;
    
    //Entferne Freizeichen am Anfang und Ende der Autoreneintr�ge
    for(int i = 0; i < size; i++){
      this.author[i] = this.author[i].trim();
    }
    
  }

  public void setRefID(int refID){
    this.refID = refID;
  }
  
  public void setTitle(String t){
    this.title = t;
  }
  
  public void setJournal(String j){
    this.journal = j;
  }
  
  public void setBooktitle(String t){
    this.booktitle = t;
  }
  
  public void setPublisher(String p){
    this.publisher = p;
  }
  
  public void setEditor(String e){
    int size;
    
    this.editor = e.split("(and)");
    size = this.editor.length;
    
    //Entferne Freizeichen am Anfang und Ende der Autoreneintr�ge
    for(int i = 0; i < size; i++){
      this.editor[i] = this.editor[i].trim();
    }
  }
  
  public void setYear(String y){
    
    //Wenn der Jahreseintrag keine Zahl ist, wird nichts eingetragen
    try{
      this.year = Integer.parseInt(y);
    }catch(Exception e){
      
    }
    
  }
  
  public void setPages(String p){
    String[] sPages = p.split("-");
    
    //Seiten werden nicht eingetragen, wenn der Eintrag nicht das Format "Anfang - Ende" hat
    if(sPages.length == 2){
      try{
        this.pages[0] = Integer.parseInt(sPages[0].trim());
        this.pages[1] = Integer.parseInt(sPages[1].trim());
      }catch(Exception e){
        
      }
    }
  }
  
  //Vergleichsfunktionen
  public double cmpTitle(String cTitle){
    String title = this.getTitle();
    int len1, len2; //L�nge der Strings
    int lowerLength; //L�nge des k�rzeren Strings
    int levenshtein; //Levenshtein-Entfernung
    
    //Bei allen Titeln werden Sonderzeichen inkl. Leerzeichen entfernt, LowerCase wird eingestellt
    title = title.toLowerCase().replaceAll(" ", "").replaceAll(":", "").replaceAll("-", "");
    cTitle = cTitle.toLowerCase().replaceAll(" ", "").replaceAll(":", "").replaceAll("-", "");
    
    //levenshtein-Entfernung: Anzahl der Ver�nderungen in einem String, um den einen in den anderen zu �berf�hren
    levenshtein = StringUtils.getLevenshteinDistance(title, cTitle);
    //System.out.println(title + " VS " + cTitle + " LEVENSHTEIN " + levenshtein);
    lowerLength = ((len1 = title.length()) > (len2 = cTitle.length())) ? len1 : len2;
    
    //Wenn einer der beiden Strings leer ist (was echt schlecht w�re), wird automatisch gleich gesetzt
    if(lowerLength == 0){
      return 1.0;
    }
    
    //Der R�ckgabewert ist das Verh�ltnis der levenshtein-Entfernung zur L�nge des k�rzeren Strings
    return 1.0 - ((double) levenshtein / (double) lowerLength);
  }
  
  public double cmpAuthor(String[] cAuthor){
    String[] author = this.getAuthor();
    String[] tName; //Tempor�re Namensliste
    int  len1, len2; //Variablen f�r Array-L�ngen
    int hit = 0; //Z�hler f�r �bereinstimmende Eintr�ge
    //Alle Autorennamen werden Standardisiert (LowerCase, Trim, Vorname nur erster Buchstabe)
    len1 = author.length;
    len2 = cAuthor.length;
    
    //Standardisierung von Autorenliste 1
    for(int i=0; i < len1; i++){
      author[i] = author[i].trim().toLowerCase().replace(" ", ""); //Alle Leerzeichen entfernen
      
      //K�rze den Vornamen ab, falls es sich um eine Vornamen+Nachnamen-Kombination handelt
      if(StringUtils.countMatches(author[i], ",") == 1){
        tName = author[i].split(",");  
        tName[1] = tName[1].trim().toCharArray()[0] + "";
        author[i] = tName[0] + "," + tName[1];
      }
      
    }
    
    //Standardisierung von Autorenliste 2, dabei Vergleich mit Liste 1
    for(int i=0; i < len2; i++){
      cAuthor[i] = cAuthor[i].trim().toLowerCase().replace(" ", ""); //Alle Leerzeichen entfernen
      tName = cAuthor[i].split(",");
      
      //K�rze den Vornamen ab, falls es sich um eine Vornamen+Nachnamen-Kombination handelt
      if(StringUtils.countMatches(cAuthor[i], ",") == 1){
        tName = cAuthor[i].split(",");  
        tName[1] = tName[1].trim().toCharArray()[0] + "";
        cAuthor[i] = tName[0] + "," + tName[1];
      }
      
      System.out.println(ArrayUtils.toString(author));
      System.out.println(cAuthor[i]);
      if(ArrayUtils.contains(author, cAuthor[i])) {hit++;}
    }
    
    //len1 wird gleichgesetzt mit der Gr��e des l�ngeren Arrays
    len1 = (len1 > len2)? len1 : len2;
    
    System.out.println("len1: " + len1 + "\tlen2: " + len2);
    //Wenn eine der beiden Autorenlisten leer ist, wird �bereinstimmung zur�ckgegeben
    if(len1 == 0 || len2 == 0 || (len1 == 1 && author[0].equals("") || (len2 == 1 && cAuthor[0].equals("")))){
      return 1.0;
    }
    //Zur�ckgegeben wird das Verh�ltnis aus �bereinstimmungen und der L�nge des l�ngeren Arrays
    return (double) hit/(double) len1;
  }
  
  //Textausgabefunktion
  
  public String toString(){
    String ret = "Typ: ";
    if(this.type == Type.INBOOK){
      ret += "INBOOK";
    }else if(this.type == Type.TECHREPORT){
      ret += "TECHREPORT";
    }else if(this.type == Type.ARTICLE){
      ret += "ARTICLE";
    }else if(this.type == Type.BOOK){
      ret += "BOOK";
    }else if(this.type == Type.MISC){
      ret += "MISC";
    }else if(this.type == Type.INPROCEEDINGS){
      ret += "INPROCEEDINGS";
    }else if(this.type == Type.PHDTHESIS){
      ret += "PHDTHESIS";
    }else if(this.type == Type.INDEF){
      ret += "nicht definiert";
    }
    
    ret += "\n";
    ret += "Referenz-ID:" + this.refID + "\n";
    ret += "Autoren: " + ArrayUtils.toString(this.author) + "\n";
    ret += "Titel: " + this.title + "\n";
    ret += "Journal: " + this.journal + "\n";
    ret += "Buchtitel: " + this.booktitle + "\n";
    ret += "Publisher: " + this.publisher + "\n";
    ret += "Editoren: " + ArrayUtils.toString(this.editor) + "\n";
    ret += "Jahr: " + this.year + "\n";
    ret += "Seiten: " + this.pages[0] + " - " + this.pages[1] + "\n";
    ret += "PDF-Groesse: " + this.size + "\n\n";
    
    return ret;
  }
}
