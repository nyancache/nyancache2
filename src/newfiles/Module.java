package newfiles;

import java.awt.Desktop;
import java.awt.Label;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;


import newfiles.Entry.Type;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.io.*;

import com.gargoylesoftware.htmlunit.WebClient;

/*
 * Ziele für Version 1.1.3:
 * TODO Für jedes Zeichen der PDF wird ihr genauer Ort und ihre Ausmaße ermittelt
 * TODO Anhand der Orte der Zeichen wird die PDF in Blöcke unterteilt
 * TODO Anhand dieser Daten Verbesserung der Elsevier-Erkennung
 */
public class Module {

  //Dateinamen
  static String FOLDER = "newfiles/";
  static String NONDUPLICATES = FOLDER + "nonDuplicates";
  static String DIRECTORIES = FOLDER + "directories";
  static String DATABASE = FOLDER + "database";
  static String JOURNALS = FOLDER + "journals";
  static String FLUIDDUPLICATES = FOLDER + "fluidDuplicates";
  static boolean IGNORE_FLUIDDUPLICATES = true;
  
  
  public static String getFileEnding(File file){
    String[] temp;
    return (temp = file.getAbsolutePath().split("\\."))[temp.length-1].toLowerCase();
  }
  
  //ï¿½ffnet die Haupt-Datenbank von Endnote
  public static void openEndnote(){
    File endnote = new File("DB\\Literatur.enl");
    try{
      if(endnote.canWrite())
        Desktop.getDesktop().open(endnote);
      else
        Desktop.getDesktop().open(new File("BiB-DB\\Neuerfasste Literatur.enl"));
    }catch(Exception ioe){
       System.out.println(ioe);
     }
  }
 
  
  public static void openEndnote2(){
    try{
      Desktop.getDesktop().open(new File("BiB-DB\\Neuerfasste Literatur.enl"));
    }catch(Exception ioe){
      System.out.println(ioe);
    }
  }
  
  
  public static void open(File f){
    try{
      Desktop.getDesktop().open(f);
        }catch(Exception ioe){
          System.out.println(ioe);
        }
  }
  
  //ï¿½ffnet eine Datei mit Standard-Programm, das in Windows eingestellt ist
  public static void open(LinkedList<File> list, int index){
    open(list.get(index));
  }
  
  //ï¿½ffnet eine Datei, aber nur dann, wenn der Typ passt
  public static void open(LinkedList<File> list, int index, String[] types){
        String end;
        boolean match; //Rï¿½ckgabewert, ob der Typ passt
        
        File f = list.get(index); //Ermittle die mit dem Index korrespondierende Datei
        
        if(types.length == 0){ //Werden keine Typen ï¿½bergeben, werden alle Dateitypen geï¿½ffnet
          match = true;
        }else{
          end = getFileEnding(f);
          match = ArrayUtils.contains(types, end.toLowerCase());
        }
        
        if(match){
          open(f);
        }
  }
  
  //ï¿½ffnet eine Serie an Dateien mittels open()
  public static void open(LinkedList<File> list, int from, int to){
    for(int i = from; i <= to; i++){
      open(list, i);
    }
  }
  
  //ï¿½ffnet eine Serie an Dateien mittels open(), Bedingung: Dateiendung
  public static void open(LinkedList<File> list, int from, int to, String[] types){
    for(int i = from; i <= to; i++){
      open(list, i, types);
    }
  }
  
  //ï¿½ffnet alle Dateien
  public static void open(LinkedList<File> list){
    int length = list.size();
    
    for(int i = 0; i < length; i++){
      open(list,i);
    }
    
  }
  
  //ï¿½ffnet alle Dateien, Bedingung: Endung
  public static void open(LinkedList<File> list, String[] types){
    int length = list.size();
    
    for(int i = 0; i < length; i++){
      open(list,i, types);
    }
    
  }
  
  @SuppressWarnings("unchecked")
  public static ArrayList<ArrayList<Integer>> openNonDuplicates(){
    ArrayList<ArrayList<Integer>> ret = new ArrayList<ArrayList<Integer>>();
    
    try{
    ObjectInputStream stream = new ObjectInputStream(new FileInputStream(NONDUPLICATES));
    ret = (ArrayList<ArrayList<Integer>>) stream.readObject();
    stream.close();
    }catch(Exception ioe){
      
    }
    
    return ret;
  }
  
  public static void saveDir(LinkedList<File> list){
    
    try{
      ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(DIRECTORIES));
      out.writeObject(list);
      out.close();
    }catch(Exception ioe){
      System.out.println(ioe);
    }
    
  }
  
  public static void saveEntries(LinkedList<Entry> entryList){
    try{
      ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(DATABASE));
      out.writeObject(entryList);
      out.close();
      
      //Lösche die fluidDuplicates-Datei, da diese nun obsolet ist
      (new File(FLUIDDUPLICATES)).delete();
      
    }catch(Exception ioe){
      System.out.println(ioe);
    }
  }
  
  @SuppressWarnings("unchecked")
  public static LinkedList<Entry> readEntries() throws IOException{
    File f;
    LinkedList<Entry> ret = new LinkedList<Entry>();
    if(!((f = new File(DATABASE)).exists()) || (f.isDirectory())){
      return ret;
    }
    
    try{
      ObjectInputStream in = new ObjectInputStream(new FileInputStream(DATABASE));
      ret = (LinkedList<Entry>) in.readObject();
      in.close();
    }catch(Exception ioe){
        System.out.println(ioe);
    	throw new IOException();
    }
    
    return ret;
  }
  
  //Wrapper fï¿½r rekursive Funktion getPDFList
  public static LinkedList<Entry> getPDFList(String dir){
    LinkedList<Entry> ret = new LinkedList<Entry>();
    
    ret = getPDFList(dir, ret);
    
    return ret;
  }

  //Ermittelt alle PDF-Dateien in einem Ordner
  //Ziel: Alle PDF-Dateien im Erledigt-Ordner erfassen
  //Zu jeder PDF-Datei werden mit dem gleichen Index weitere Informationen gespeichert
  private static LinkedList<Entry> getPDFList(String dir, LinkedList<Entry> ret){
    File folder = new File(dir);
    File currfile; 
    File[] listOfFiles = folder.listFiles();
    
    int size = listOfFiles.length;
    
    for(int i = 0; i < size; i++){
      
      //Speiche die aktuelle Datei in currfile zwischen
      currfile = listOfFiles[i];
      //Handelt es sich um einen Ordner, wird die Prozedur rekursiv angewendet
      if(currfile.isDirectory()){
        ret = getPDFList(currfile.getAbsolutePath(), ret);
      }else if(currfile.isFile() && getFileEnding(currfile).equals("pdf")){ //Handelt es sich um eine pdf-Datei? Dann zur Liste hinzufï¿½gen
        ret.add(new Entry(currfile));
      }
    }
    
    return ret;
  }
  
  
  //Diese Datei liest die fluidDuplicates-Datei aus und gibt zurück, welche Dateien schonmal auf Duplikate überprüft wurden
  public static ArrayList<PathDuplicatesSet> getFluidDuplicates(){
    ArrayList<PathDuplicatesSet> ret = new ArrayList<PathDuplicatesSet>();
    
    if(!IGNORE_FLUIDDUPLICATES){ //Wurde die Konstante IGNORE_FLUIDDUPLICATES auf true gesetzt, wird dieser Code ignoriert
      File fluidDuplicatesFile = new File(FLUIDDUPLICATES);
      Object obj;
      PathDuplicatesSet pds = new PathDuplicatesSet();
      
      try{
        FileInputStream fis = new FileInputStream(fluidDuplicatesFile);
        ObjectInputStream ois = new ObjectInputStream(fis);
        
        while(true){
          try{
            
            obj = ois.readObject();
            if(obj instanceof PathDuplicatesSet){
              pds = (PathDuplicatesSet) obj;
              ret.add(pds);
            }
            
          }catch(Exception ioe){
            break;
          }
        }
        
        fis.close();
        ois.close();
        
      }catch(Exception ioe){
        System.out.println("Fehler bei Ausführung von getfluidduplicates");
      }
      
      fluidDuplicatesFile = null;
    }
    return ret;
  }
  
  public static void saveFluidDuplicates(ArrayList<ArrayList<Integer>> duplicates, LinkedList<File> finallist){
    //Speichert die gefundenen Duplicate in der Form (path,duplicates) of (String,ArrayList<Integer>)
    //Hierbei ist path = Dateipfad + lastModified als long
    int size = finallist.size();
    File fluidDuplicatesFile = new File(FLUIDDUPLICATES);
    PathDuplicatesSet pds;
    File currFile;
    
    
    try{
      
      FileOutputStream fos = new FileOutputStream(fluidDuplicatesFile);
      ObjectOutputStream oos = new ObjectOutputStream(fos);
      for(int i = 0; i < size; i++){
        currFile = finallist.get(i);
        pds = new PathDuplicatesSet();
        pds.path = currFile.getName() + currFile.lastModified();
        pds.duplicates = duplicates.get(i);
        oos.writeObject(pds);
      }
      
      oos.flush();
      
      fos.close();
      oos.close();
      
    }catch(Exception ioe){
      System.out.println("Fehler bei Ausführung von savefluidduplicates");
    }
    
    fluidDuplicatesFile = null;
  }
  
  @SuppressWarnings("unchecked")
  public static Map<ArrayList<Integer>,String> getNewDoublicates (Entry newEntry, LinkedList<Entry> fullList, InfoStream is, boolean online, boolean tryRename){
    WebClient wc = new WebClient();
    PDFUtil pdfUtil = new PDFUtil(Module.readJournalList(), wc, is);
    System.out.println("getNewDoublicates start: " + newEntry.getPDF().getName());
    ArrayList<Integer> ret = new ArrayList<Integer>();
    Map<ArrayList<Integer>,String> retMap = new HashMap<>();
    Map<String,String> endnoteExport = new HashMap<>();
    int len = fullList.size(); //Grï¿½ï¿½e der kompletten Eintrags-Liste
    long size = newEntry.getSize();
    String pdfTitle = ""; //Titel des Artikels, der in der PDF-Datei gespeichert ist
    File thisPDF;
    ArrayList<String> authors = new ArrayList<String>();
    int year = 0;
    String articleTitle = "";
    String newPdfTitle = "";
    String endnoteOut = "";
    String firstLine = "";
    boolean doRename = false;
    File newFile;
    Map<String,Map<String,String>> pdfParseResult = new HashMap<>();
    ArrayList<String> endnoteExportLines = new ArrayList<String>();
    Entry e; //temporäre Variable für verglichenen Eintrag
    
    if(len == 0) return retMap; //Wenn die EntryList leer ist, findet keine Duplikat-Kontrolle statt
    
    thisPDF = newEntry.getPDF();
    for(int i = 0; i < len; i++){
      e = fullList.get(i);
      //Heuristik 1: Grï¿½ï¿½e der PDF-Datei
      if(size != 0 && e.getSize() != 0){ //Ist ï¿½berhaupt eine Grï¿½ï¿½e gespeichert?
        //wenn ja, sind die Dateien gleich groï¿½?
          try{
            if(size == e.getSize() && FileUtils.contentEquals(thisPDF, e.getPDF())){
              ret.add(i);
            }
          }catch(Exception ioe){
            System.out.println(ioe);
          }
      }
    }
    
    try{
      pdfParseResult = pdfUtil.getTitle(newEntry.getPDF(), is, online);
      if(pdfParseResult.size() == 1){
        if(pdfParseResult.keySet().toArray()[0] instanceof String){
          pdfTitle = (String) pdfParseResult.keySet().toArray()[0];
        }else{
          System.out.println("Fehler in der pdfUtil-Erkennung: Der Key ist kein String.");
        }
        if( pdfParseResult.values().toArray()[0] instanceof Map<?,?>){
          endnoteExport = (Map<String,String>) pdfParseResult.values().toArray()[0];
        }else{
          System.out.println("Fehler in der pdfUtil-Erkennung: Der Value ist keine Map<?,?>");
        }
      }else{
        System.out.println("Fehler in der pdfUtil-Erkennung: Die Rückgabe hat mehr als einen Eintrag.");
      }
    }catch(Exception ioe){
      System.out.println("Diese Datei konnte wegen eines unbekannten Fehlers nicht analysiert werden.");
      ioe.printStackTrace();
      retMap.put(ret, "");
      return retMap;
    }
    
    try{
      is.print("Untersuchte PDF-Datei:\t" + newEntry.getPDF().getName() + "\n");
      is.print("Gefundener Titel:\t" + pdfTitle + "\n\n\n");
    }catch(Exception ioe){System.out.println(ioe);}
      
      //Wir haben nun ein boolean uneq, das zurï¿½ckgibt, ob die PDF-Dateien identisch sind
    ArrayList<Double> confList = new ArrayList<Double>();
    ArrayList<String> stringList = new ArrayList<String>();
    for(int i = 0; i < len; i++){
      e = fullList.get(i);
      confList.add(e.cmpTitle(pdfTitle));
      stringList.add(e.getTitle());
    }
    
    Object[] confList2 = confList.toArray();
    confList.sort(new Comparator<Double>(){
    @Override
    public int compare(Double d1, Double d2){
      return (-1) * Double.compare(d1, d2);
      }
    }
  );
    System.out.println("This maximum degree of conformity has been found: " + confList.get(0));
    System.out.println("This is the title of the entry most likely to be a doublicate: " + stringList.get(ArrayUtils.indexOf(confList2, confList.get(0))));
    for(int i = 0; i < len; i++){
      e = fullList.get(i);
      //Heuristik 2: Titel der Studie
      if(e.cmpTitle(pdfTitle) >= 0.95 && !pdfTitle.equals("") && !e.getTitle().equals("")){ //Ã„hneln sich die Titel?
        ret.add(i);
        System.out.println("This file has been detected as doublicate.");
        break;
      }
      
    }
    System.out.println("getNewDoublicates end: " + newEntry.getPDF().getName() + "\n\n");
    
    //Ermittle Endnote-Daten aus Bibsonomy
    if(endnoteExport.size() > 0){ //Wurde ein Ergebnis gefunden? Dann verarbeiten, wenn gewünscht.
      if(tryRename){
        doRename = true;
      }
      //Schreibe Endnote-Daten in Textdatei
      try{
        for(String key: endnoteExport.keySet()){
          endnoteExportLines.add(key + " " + endnoteExport.get(key));
        }
        for(String line:endnoteExportLines){
          //Bevor eine Teile hinzugefügt wird, wird überprüft, ob sie mit einem % beginnt. Nur dann wird sie übernommen
          //Ansonsten wird die Importdatei ungültig.
          
          //Da immer erst ab %0 gelesen wird, muss unbedingt damit begonnen werden.
          if(line.startsWith("%0")){
            firstLine = line + "\n";
          }
          if(line.startsWith("%")){
            endnoteOut += line + "\n";
          }
          
          if(line.startsWith("%A")){ //Das %A signalisiert den Beginn der Autorenzeile
            line = StringUtils.remove(line, "%A");
            for (String author : StringUtils.split(line, ";&")){ //Trenne die unterschiedlichen Autoren voneinander, indem nach ; und & separiert wird
              authors.add(StringUtils.split(author,',')[0].trim().toLowerCase()); //Nun wird nur der Text vor dem Komma übernommen, da dies der Nachname ist
            }
          }else if(line.startsWith("%D")){ //Lese Jahreszahl aus
            line = StringUtils.remove(line, "%D");
            line = line.trim();
            line = StringUtils.substring(line, 0, 4);
            try{
              year = Integer.parseInt(line);
            }catch(Exception ioe){
              doRename = false;
            }
          }else if(line.startsWith("%T")){
            line = StringUtils.remove(line, "%T");
            line = line.trim();
            line = StringUtils.strip(line, ".");
            articleTitle = line;
          }
        }
        
        //endnoteOut wird aus der ersten Zeile und dem Rest zusammengesetzt
        endnoteOut = firstLine + endnoteOut;
        
        //Erstelle den neuen PDF-Dateinamen anhand der ausgelesenen Informationen
        if(authors.size() > 3){
          newPdfTitle = authors.get(0).trim() + " et al_" + year + "_" + articleTitle + ".pdf";
        }else if(authors.size() == 3){
          newPdfTitle = authors.get(0).trim() + "_" + authors.get(1).trim() + "_" + authors.get(2).trim() + "_" + year + "_" + articleTitle + ".pdf";
        }else if(authors.size() == 2){
          newPdfTitle = authors.get(0).trim() + "_" + authors.get(1).trim() + "_" + year + "_" + articleTitle + ".pdf";
        }else if(authors.size() == 1){
          newPdfTitle = authors.get(0).trim() + "_" + year + "_" + articleTitle + ".pdf";
        }else{
          doRename = false;
        }
        
        //Zeichen entfernen, die nicht in Dateinamen geschrieben werden können
        newPdfTitle = StringUtils.remove(newPdfTitle, ':');
        newPdfTitle = StringUtils.remove(newPdfTitle, '<');
        newPdfTitle = StringUtils.remove(newPdfTitle, '>');
        newPdfTitle = StringUtils.remove(newPdfTitle, '"');
        newPdfTitle = StringUtils.remove(newPdfTitle, '\\');
        newPdfTitle = StringUtils.remove(newPdfTitle, '/');
        newPdfTitle = StringUtils.remove(newPdfTitle, '|');
        newPdfTitle = StringUtils.remove(newPdfTitle, '*');
        newPdfTitle = StringUtils.remove(newPdfTitle, '?');
        newPdfTitle = StringUtils.remove(newPdfTitle, ',');
        newPdfTitle = StringUtils.remove(newPdfTitle, '@');
        newPdfTitle = newPdfTitle.toLowerCase();
        
        if(doRename){
          //Wenn der Titel zu lang ist, wird er abgekürzt, damit das Dateisystem nicht zerschossen wird
          if(newPdfTitle.length() > 150){
            newPdfTitle = StringUtils.toEncodedString(StringUtils.left(newPdfTitle, 146).getBytes(),Charsets.UTF_8) + ".pdf";
          }
          System.out.println("Der folgende PDF-Dateiname wird vorgeschlagen: " + newPdfTitle);
          //Nenne Datei um
          try{
            newFile = new File(StringUtils.substringBeforeLast(newEntry.getPDF().getAbsolutePath(),"\\") + "\\" + newPdfTitle);
            System.out.println("newEntry.getPDF().getAbsolutePath(): " + newEntry.getPDF().getAbsolutePath());
            System.out.println("newfile.getAbsolutePath(): " + newFile.getAbsolutePath());
            newEntry.getPDF().renameTo(newFile);
          }catch(Exception ioe){
            ioe.printStackTrace();
          }
          
        }else{
          System.out.println("Für diese Datei wird keine Veränderung des PDF-Dateinamens vorgeschlagen.");
        }
      }catch(Exception ioe){
        ioe.printStackTrace();
      }
    }
    newEntry = null;
    retMap.put(ret, endnoteOut);
    System.out.println(endnoteOut);
    return retMap;
  }
  
  public static String endnoteMapToString(Map<String,String> map){
    String ret = "";
    for (Map.Entry<String, String> entry : map.entrySet()) {
      ret += entry.getKey() + " " + entry.getValue() + "\n";
    }
    
    return ret;
  }
  
  
  
  public static ArrayList<ArrayList<Integer>> getDuplicates(LinkedList<Entry> pre_entryList, Label status, ArrayList<ArrayList<Integer>> nonDuplicates){
    int len = pre_entryList.size();
    long lastSize = -1; //Merk-Variable fï¿½r vorherige Dateigrï¿½ï¿½e
    long thisSize = -1; //Merk-Variable fï¿½r momentane Dateigrï¿½ï¿½e
    int lastCounter = -1; //Merk-Variable fï¿½r den vorherigen Index der 2-Dimensionalen Liste
    boolean first = true; //Kontroll-Boolean, ob es sich um die erste Gefundene Widerholung mit einer bestimmten Dateigrï¿½ï¿½e ist
    Entry e, e2; //Temporï¿½rer Eintrag
    ArrayList<ArrayList<Integer>> ret = new ArrayList<ArrayList<Integer>>();
    ArrayList<Integer> tempList = new ArrayList<Integer>();
    ArrayList<Integer> tempList2 = new ArrayList<Integer>();
    ArrayList<Entry> entryList = new ArrayList<Entry>();
    
    //Forme die entryList in eine ArrayList um, damit schneller gesucht werden kann
    for(int i = 0; i < len; i++){
      pre_entryList.get(i).setID(i);
      entryList.add(pre_entryList.get(i));
    }
    
    System.out.println(nonDuplicates.toString());
    Collections.sort(entryList, new CompFileSize());
    for(int i = 0; i < len; i++){ //Alle Eintrï¿½ge der entryList durchlaufen
      if(i%10 == 0){
      status.setText(i + " von " + len + " (" + Math.round(Math.floor(((double) i/ (double) len)*100.0)) + "%)");
      System.out.println(i);
      }
      e = entryList.get(i);
      //Heuristik 1: Grï¿½ï¿½e der PDF-Datei
      if ((thisSize = e.getSize()) == lastSize && thisSize != 0 && e.cmpAuthor(entryList.get(i-1).getAuthor()) > 0){ //Besteht eine Widerholung und ist eine PDF-Datei vorhanden?
        //Weiterhin wird das Dublikat nur dann eingetragen, wenn irgendein Autor ï¿½bereinstimmt
        if(first){//Handelt es sich um die erste Widerholung mit der Grï¿½ï¿½e?
          first = false;
          
          tempList = new ArrayList<Integer>();
          tempList2 = new ArrayList<Integer>();
          //Ist der aktuelle Eintrag schon in der nonDuplicates enthalten?
          tempList.add(entryList.get(i-1).getRefID());
          tempList.add(e.getRefID());
          
          tempList2.add(e.getRefID());
          tempList2.add(entryList.get(i-1).getRefID());
          
          
          //Eintrag wird nur gespeichert, wenn er nicht in der nonDuplicates enthalten ist
          System.out.println(tempList.toString() + nonDuplicates.toString());
          if(!nonDuplicates.contains(tempList) && !nonDuplicates.contains(tempList2)){
            lastCounter++;
            ret.add(new ArrayList<Integer>());
            ret.get(lastCounter).add(entryList.get(i-1).getRefID());
            ret.get(lastCounter).add(e.getRefID());
            System.out.println("FOUND BY PDF");
          }else{
            System.out.println("Non-Duplicate found");
          }
        }else{
          ret.get(lastCounter).add(i);
          System.out.println("FOUND BY PDF AGAIN");
        }//end if
      }else{//Besteht keine Widerholung?
        first = true; //first wird fï¿½r die nï¿½chste Wiederholung wieder auf true gesetzt
        
        //Separater Abgleich mit allen anderen Eintrï¿½gen
        for(int j = i+1; j < len; j++){
          //Heuristik 2: Identischer Titel und Typ identisch, auï¿½erdem Jahreszahl identisch
          //In diesem Fall wird nur dann keine ï¿½bereinstimmung festgestellt, wenn kein Autor ï¿½bereinstimmt
          if(e.getType() == (e2 = entryList.get(j)).getType()){
            if(e.getYear() == 0 || e2.getYear() == 0 || (e.getYear() == e2.getYear())){
              if(e.cmpTitle(e2.getTitle()) >= 0.85){
                if(e.cmpAuthor(e2.getAuthor()) > 0){
                  
                  tempList = new ArrayList<Integer>();
                  tempList2 = new ArrayList<Integer>();
                  //Ist das aktuelle Paar schon in der nonDuplicates-Liste enthalten?
                  tempList.add(e.getRefID());
                  tempList.add(e2.getRefID());
                  
                  tempList2.add(e2.getRefID());
                  tempList2.add(e.getRefID());
                  //Eintrag wird nur dann hinzugefügt, wenn er nicht in der nonDuplicates-Liste enthalten ist
                  System.out.println(tempList.toString() + nonDuplicates.toString());
                  if(!nonDuplicates.contains(tempList) && !nonDuplicates.contains(tempList2)){
                    lastCounter++;
                    ret.add(new ArrayList<Integer>());
                    ret.get(lastCounter).add(e.getRefID());
                    ret.get(lastCounter).add(e2.getRefID());
                    System.out.print("FOUND BY ALGORITHM");
                    System.out.println(e2.getTitle() + "\t" + e2.getAuthor());
                  }else{
                    System.out.println("Non-Duplicate found");
                  }
                }
              }
            }
          }
        }
        
      }//end if
      lastSize = thisSize;
      
    }//end for
    
    //Im Moment ist in ret der Index der Internen Dublikate-Liste gespeichert
    //Forme dies in die Entry-ID um
    System.out.println(ret);/*
    len = ret.size();
    for(int i = 0; i < len; i++){
      len2 = ret.get(i).size();
      
      for(int j = 0; j < len2; j++){
        System.out.println(entryList.get(ret.get(i).get(j)).getTitle());
        ret.get(i).set(j, entryList.get(ret.get(i).get(j)).getID());
      }
    }*/

    return ret;
  }
  
  public static void saveNonDoublicates(ArrayList<ArrayList<Integer>> doublList){
    File outFile = new File(NONDUPLICATES);

    try{
      ObjectOutputStream stream = new ObjectOutputStream(new FileOutputStream(outFile));
      System.out.println("\n\n\ndoublList:" + doublList); 
      stream.writeObject(doublList);
      stream.close();
    }catch(Exception ioe){
      
    }
  }
  
  public static ArrayList<String> readBibTex(String path) throws FileNotFoundException, IOException{
    ArrayList<String> ret = new ArrayList<String>();
    String line; //Variable fï¿½r Ausgelesene Zeilen
    String curr = ""; //Variable fï¿½r gegenwï¿½rtig eingelesenen Eintrag
    //Erstelle einen BufferedReader zum Auslesen der BibTex-Datei
    File BibFile = new File(path);
    FileReader fr = new FileReader(BibFile);
    BufferedReader br = new BufferedReader(fr);
    
    //Lese Inhalt der BibTex-Datei aus, jeder Eintrag bekommt einen neuen ArrayList-Index
    while((line = br.readLine()) != null){
      if(line.startsWith("@")){ //Handelt es sich um einen neuen Eintrag? Dann neuen Eintrag in ArrayList hinzufï¿½gen
        
        //Wenn zuvor etwas erfasst wurde, wird diser Eintrag in die ArrayList eingetragen
        if(!curr.equals("")){
          ret.add(curr);
          curr = "";
        }
        
        curr = line + "\n";
      }else{//Wird kein neuer Eintrag begonnen?
        
        //Hat die Zeile ï¿½berhaupt Inhalt?
        if(!line.equals("")){ //Wenn ja: zu curr hinzufï¿½gen, inklusive Zeilenumbruch
          curr += line + "\n";
        }//end if
      }//end if
    }//end while
    
    //Auch der letzte Eintrag muss noch hinzugefï¿½gt werden
    ret.add(curr);
    
    
    //Schlieï¿½e den BufferedReader
    br.close();
    
    return ret;
  }
  
  public static Entry parseEntry(String bibTex){
    Entry ret = new Entry();
    String[] lines; //Array fï¿½r die Zeilen des Eintrags
    int size; //int fï¿½r die Zeilenanzahl
    int size2; //int fï¿½r die Zeichenanzahl
    int last = 0; //int fï¿½r die letzte in der vorherigen Schleife behandelte Zeile
    String firstword = ""; //String fï¿½r die Art des eingelesenen Eintrags
    boolean paren = false; //Boolean, ob im Moment eine Klammer geï¿½ffnet ist
    String content = ""; //String fï¿½r den momentan eingelesenen Inhalt
    
    //Erkenne die erste Zeile (beginnt mit einem @) und lese den Eintragstyp aus
    lines = bibTex.split("\n");
    size = lines.length;
    
    for(int i = 0; i<size;i++){
      if(lines[i].trim().startsWith("@")){
        switch(lines[i].charAt(1)){
        case 'i':
          if(lines[i].charAt(3) == 'p'){
            ret.setType(Type.INPROCEEDINGS);
          }else{
            ret.setType(Type.INBOOK);
          }
          break;
        case 'b':
          ret.setType(Type.BOOK);
          break;
        case 't':
          ret.setType(Type.TECHREPORT);
          break;
        case 'a':
          ret.setType(Type.ARTICLE);
          break;
        case 'm':
          ret.setType(Type.MISC);
          break;
        case 'p':
          ret.setType(Type.PHDTHESIS);
          break;
        default:
          ret.setType(Type.INDEF);
        } //end switch
        last = i;
        break;
      }//end if
      
    }//end for
    
    for(int i = last+1; i < size; i++){
      
      
      size2 = lines[i].length(); //Zeichenanzahl der Zeile
      
      //Weiterlesen des bisherigen Inhaltes, wenn zu Beginn der Zeile noch eine Klammer offen ist
      if(paren == true){
        
        for(int j = 0; j < size2; j++){
          
          if(lines[i].charAt(j) == '}'){
            paren = false;
            break;
          }
          
          content = content + lines[i].charAt(j);
        }
        
      }
      
      else if(lines[i].trim().toLowerCase().startsWith("author")){
        firstword  = "author";
        
      }
      
      else if(lines[i].trim().toLowerCase().startsWith("title")){
        firstword = "title";
      }
      
      else if(lines[i].trim().toLowerCase().startsWith("journal")){
        firstword = "journal";
      }
      
      else if(lines[i].trim().toLowerCase().startsWith("booktitle")){
        firstword = "booktitle";
      }
      
      else if(lines[i].trim().toLowerCase().startsWith("publisher")){
        firstword = "publisher";
      }
      
      else if(lines[i].trim().toLowerCase().startsWith("editor")){
        firstword = "editor";
      }
      
      else if(lines[i].trim().toLowerCase().startsWith("pages")){
        firstword = "pages";
      }
      
      else if(lines[i].trim().toLowerCase().startsWith("year")){
        firstword = "year";
      }
      
      else if (lines[i].trim().toLowerCase().startsWith("pdf")){
        firstword = "pdf";
      }
      
      else if (lines[i].trim().toLowerCase().startsWith("endnote")){
        firstword = "endnote";
      }
      
      //Durchlaufe alle Zeichen, bis eine geï¿½ffnete Klammer gefunden wird, dann lese aus bis eine geschlossene Klammer gefunden wird
      for(int j = 0; j < size2; j++){
        
        if(lines[i].charAt(j) == '{'){
          paren = true;
          continue;
        }
        
        if(lines[i].charAt(j) == '}'){
          paren = false;
          break;
        }
        
        if(paren == true){
          content = content + lines[i].charAt(j);
        }
        
      }
      //Eintragen des eingelesenen Inhaltes
      if(paren == false){
        if(firstword.equals("author")){
          ret.setAuthor(content);
        }else if(firstword.equals("title")){
          ret.setTitle(content);
        }else if(firstword.equals("journal")){
          ret.setJournal(content);
        }else if(firstword.equals("booktitle")){
          ret.setBooktitle(content);
        }else if(firstword.equals("publisher")){
          ret.setPublisher(content);
        }else if(firstword.equals("pages")){
          ret.setPages(content);
        }else if(firstword.equals("year")){
          ret.setYear(content);
        }else if(firstword.equals("editor")){
          ret.setEditor(content);
        }else if(firstword.equals("pdf")){
          ret.setPDF(content);
        }else if(firstword.equals("endnote")){
          ret.setRefID(Integer.parseInt(content));
        }
        
        firstword = "";
        content = "";
      }
      
    }
    
    return ret;
  }
  
  public static void saveJournalList(LinkedList<Entry> entryList){
    ArrayList<String> journals = new ArrayList<String>();
    String thisJournal;
    for(Entry e: entryList){
      thisJournal = e.getJournal();
      if(!thisJournal.isEmpty() && !journals.contains(thisJournal)){
        journals.add(e.getJournal().toLowerCase().replaceAll(" ", ""));
        System.out.println("Journal has been added: " + e.getJournal());
      }
    }
    
    
    try{
      ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(JOURNALS));
      out.writeObject(journals);
      out.close();
    }catch(Exception ioe){
      System.out.println(ioe);
    }
  }
  
  @SuppressWarnings("unchecked")
  public static ArrayList<String> readJournalList(){
    ArrayList<String> ret = new ArrayList<String>();
    
    try{
      ObjectInputStream in = new ObjectInputStream(new FileInputStream(JOURNALS));
      ret = (ArrayList<String>) in.readObject();
      in.close();
    }catch(Exception ioe){
      System.out.println(ioe);
    }
    return ret;
  }
 
  public static Entry getEntryByRefID(int id, LinkedList<Entry> entryList){
    Entry e = new Entry();
    
    for(Entry entry: entryList){
      if(entry.getRefID() == id){
        e = entry;
        break;
      }
    }
    
    return e;
  }
   
  public static class PathDuplicatesSet implements Serializable{
    private static final long serialVersionUID = 1;
    String path;
    ArrayList<Integer> duplicates;
    
    public String toString(){
      return "[" + path + "," + duplicates.toString() + "]";
    }
  }
  
}

