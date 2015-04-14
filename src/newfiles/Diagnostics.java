package newfiles;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.swing.text.BadLocationException;


public class Diagnostics {
	InfoStream is;
	Main main;
	
	boolean readNonduplicates = true;
	boolean writeNonduplicates = true;
	boolean readDatabase = true;
	boolean writeDatabase = true;
	boolean readJournals = true;
	boolean writeJournals = true;
	boolean readFluidduplicates = true;
	boolean writeFluidduplicates = true;
	boolean accWebDB = true; //Indiziert, ob die Web-Literatur-Datenbanken erreichbar sind.
	
	boolean firstRun = true; //Handelt es sich um den ersten Diagnosedurchlauf?
	
	Diagnostics(Main main2){
		this.is = main2.is;
		this.main = main2;
	}
	
	public void dx() throws IOException, BadLocationException{
		  String message = "";
		  if (!(new File(Module.FOLDER)).exists()){
			  message += "Der Ordner " + Module.FOLDER  + "wurde nicht gefunden."
			  		  + "Es wird dringend empfohlen, diesen anzulegen, bevor das Program fortfährt.\n\n";
			  readNonduplicates = false;
			  writeNonduplicates = false;
			  readDatabase = false;
			  writeDatabase = false;
			  readJournals = false;
			  writeJournals = false;
			  readFluidduplicates = false;
			  writeFluidduplicates = false;
		  }
		  
		  if (!(new File(Module.NONDUPLICATES)).exists()){
			  message += "Die Datei " + Module.NONDUPLICATES + " wurde nicht gefunden.\n"
					  + "Dies führt zu folgender Einschränkung des Programms:\n"
					  + "Wird nach bestehenden internen Duplikaten gesucht, können bereits "
					  + "als Nicht-Duplikate identifizierte Dateien nicht ignoriert werden.\n\n";
			  readNonduplicates = false;
			  writeNonduplicates = false;
		  }else{
			  if(!canReadNow(new File(Module.NONDUPLICATES))){
			  message += "Auf die Datei " + Module.NONDUPLICATES + " besteht kein Lesezugriff.\n"
					  + "Dies führt zu folgender Einschränkung des Programms:\n"
					  + "Wird nach bestehenden internen Duplikaten gesucht, können bereits "
					  + "als Nicht-Duplikate identifizierte Dateien nicht ignoriert werden.\n\n";
			  readNonduplicates = false;
			  }
			  if(!canWriteNow(new File(Module.NONDUPLICATES))){
			  message += "Auf die Datei " + Module.NONDUPLICATES + " besteht kein Schreibzugriff.\n"
					  + "Dies führt zu folgender Einschränkung des Programms:\n"
					  + "Die Funktion, bereits als interne Duplikate erkannte Dateien zu speichern"
					  + ", besteht nicht.\n\n";
			  writeNonduplicates = false;
			  }
		  }

		  if(!(new File(Module.DATABASE)).exists()){
			  message += "Die Datei " + Module.DATABASE + " wurde nicht gefunden.\n"
					  + "Dies führt zu folgender Einschränkung des Programms:\n"
					  + "Es kann nicht auf die Endnote-Datenbank zugegriffen werden.\n\n";
			  readDatabase = false;
			  writeDatabase = false;
		  }else{
			  if(!canReadNow(new File(Module.DATABASE))){
			  message += "Auf die Datei " + Module.DATABASE + " besteht kein Lesezugriff.\n"
					  + "Dies führt zu folgender Einschränkung des Programms:\n"
					  + "Es kann nicht auf die Endnote-Datenbank zugegriffen werden.\n\n";
			  readDatabase = false;
			  }
		  if(!canWriteNow(new File(Module.DATABASE))){
			  message += "Auf die Datei " + Module.DATABASE + " besteht kein Schreibzugriff.\n"
					  + "Dies führt zu folgender Einschränkung des Programms:\n"
					  + "Die Endnote-Datenbank kann nicht aktualisiert werden.\n\n";
			  writeDatabase = false;
		  	}
		  }

		  if(!(new File(Module.JOURNALS)).exists()){
			  message += "Die Datei " + Module.JOURNALS + " wurde nicht gefunden.\n"
					  + "Dies führt zu folgender Einschränkung des Programms:\n"
					  + "Einschränkungen bei der Qualität der ausgelesenen Artikel-Titel.\n\n";
			  readJournals = false;
			  writeJournals = false;
		  }else{
			  if(!canReadNow(new File(Module.JOURNALS))){
			  message += "Auf die Datei " + Module.JOURNALS + " besteht kein Lesezugriff.\n"
					  + "Dies führt zu folgender Einschränkung des Programms:\n"
					  + "Einschränkungen bei der Qualität der ausgelesenen Artikel-Titel.\n\n";
			  readJournals = false;
			  }
			  if(!canWriteNow(new File(Module.JOURNALS))){
			  message += "Auf die Datei " + Module.JOURNALS + " besteht kein Schreibzugriff.\n"
					  + "Dies führt zu folgender Einschränkung des Programms:\n"
					  + "Bei der Aktualsierung der Endnote-Datenbank können keine neuen Journale gespeichert werden.\n\n";
			  writeJournals = false;
			  }
		  }

		  if(!(new File(Module.FLUIDDUPLICATES)).exists()){
			  message += "Die Datei " + Module.FLUIDDUPLICATES + " wurde nicht gefunden."
					  + "Dies führt zu folgender Einschränkung des Programms:\n"
					  + "PDF-Dateien, bei denen bereits überprüft wurde, ob es sich um Duplikate handelt, "
					  + "werden bei der Analyse erneut untersucht.";
			  readFluidduplicates = false;
			  writeFluidduplicates = false;
		  }else{
			  if(!canReadNow(new File(Module.FLUIDDUPLICATES))){
			  message += "Auf die Datei " + Module.FLUIDDUPLICATES + " besteht kein Lesezugriff."
					  + "Dies führt zu folgender Einschränkung des Programms:\n"
					  + "PDF-Dateien, bei denen bereits überprüft wurde, ob es sich um Duplikate handelt, "
					  + "werden bei der Analyse erneut untersucht.";
			  readFluidduplicates = false;
			  }
			  if(!canWriteNow(new File(Module.FLUIDDUPLICATES))){
				  message += "Auf die Datei " + Module.FLUIDDUPLICATES + " besteht kein Schreibzugriff."
						  + "Dies führt zu folgender Einschränkung des Programms:\n"
						  + "PDF-Dateien, bei denen bereits überprüft wurde, ob es sich um Duplikate handelt, "
						  + "werden in folgenden Analysen erneut untersucht.";
				  writeFluidduplicates = false;
			  }
		  }
		  
		  if(Module.IGNORE_FLUIDDUPLICATES){
			  message += "Im Quellcode ist das IGNORE_FLUIDDUPLICATES-Flag gesetzt.\n"
					  + "Dies führt dazu, dass bei jeder PDF-Analyse Duplikate erneut untersucht werden.";
		  }

		  if(!message.equals("") && firstRun){
				 is.warning(message);
		  }
		  
		  if(!readNonduplicates){
			  //Ausrufezeichen bei "Interne Duplikate ermitteln"
			  main.button3.makeAlert();
		  }
		  
		  if(!writeNonduplicates){
			  main.buttonA.makeAlert();
		  }
		  
		  if(!readDatabase){
			  main.buttonD.makeAlert();
			  main.button3.makeAlert();
			  main.buttonA.makeAlert();
			  main.button9.makeAlert();
		  }
		  
		  if(!writeDatabase){
			  main.button7.makeAlert();
		  }
		  
		  if(!readJournals){
			  main.buttonD.makeAlert();
		  }
		  
		  if(!writeJournals){
			  main.button7.makeAlert();
		  }
		  
		  if(!readFluidduplicates || !writeFluidduplicates){
			  main.buttonD.makeAlert();
		  }

		  if(readDatabase && readJournals && readFluidduplicates && writeFluidduplicates){
			  main.buttonD.removeAlert();
		  }
		  
		  if(writeNonduplicates && readDatabase){
			  main.buttonA.removeAlert();
		  }
		  
		  if(readNonduplicates && readDatabase){
			  main.button3.removeAlert();
		  }
		  
		  if(readDatabase){
			  main.button9.removeAlert();
		  }
		  
		  if(writeDatabase && writeJournals){
			  main.button7.removeAlert();
		  }

		  message = "";
		  //Überprüfe, ob online-Zugriff besteht
		  try{
			  if(!InetAddress.getByName("jstor.org").isReachable(10000)){
			  }
		  }catch(UnknownHostException uhe){
			  message += "Der Server auf http://www.jstor.org ist nicht erreichbar.\n"
					  + "Dies führt zu folgender Einschränkung des Programms:\n"
					  + "Die Qualität der PDF-Online-Analyse ist eingeschränkt.\n\n";
			  accWebDB = false;
			  uhe.printStackTrace();
		  }
		  
		try{
			  if(!InetAddress.getByName("sciencedirect.com").isReachable(10000)){
			  }
		  }catch(UnknownHostException uhe){
			  message += "Der Server auf http://www.sciencedirect.com ist nicht erreichbar.\n"
					  + "Dies führt zu folgender Einschränkung des Programms:\n"
					  + "Die Qualität der PDF-Online-Analyse ist eingeschränkt.\n\n";	
			  accWebDB = false;
			  uhe.printStackTrace();
		  }
			  
		try{  
		  if(!InetAddress.getByName("bibsonomy.org").isReachable(10000)){
		  }
		}catch(UnknownHostException uhe){
			 message += "Der Server auf http://www.bibsonomy.org ist nicht erreichbar.\n"
					  + "Dies führt zu folgender Einschränkung des Programms:\n"
					  + "Die Qualität der PDF-Online-Analyse ist eingeschränkt.\n\n";
			  accWebDB = false;
			  uhe.printStackTrace();
		}
		
		  if(!message.equals("") && firstRun){
				 is.warning(message);
		  }
		  
		  firstRun = false;
		  
		  
	  }
	
	  public static boolean canReadNow(File file){
		  FileInputStream fis;
		  try{
			  fis = new FileInputStream(file);
			  fis.close();
			  return true;
		  }catch(IOException ioe){
			  return false;
		  }
	  }
	  
	  public static boolean canWriteNow(File file){
		  FileOutputStream fos;
		  try{
			  fos = new FileOutputStream(file, true);
			  fos.close();
			  return true;
		  }catch(IOException ioe){
			  return false;
		  }
	  }
}
