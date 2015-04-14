package newfiles;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;








import org.apache.commons.io.Charsets;
import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.io.RandomAccessBuffer;
import org.apache.pdfbox.pdfparser.NonSequentialPDFParser;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.util.PDFOperator;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;

public class PDFUtil{
  
  ArrayList<String> journals;
  WebClient webClient;
  InfoStream is;
  
  PDFUtil(ArrayList<String> j, WebClient wc, InfoStream i){
    journals = j;
    webClient = wc;
    is = i;
  }
  
  private enum Source{
    ELSEVIER, JSTOR, BIBSONOMY, DEFAULT
  }
  
  @SuppressWarnings("unchecked")
public Map<String,Map<String,String>> getTitle(File input, InfoStream is, boolean online) throws Exception{
    String articleTitle = "";
    boolean parsingText = false;
    Map<String,PDFont> fonts = new HashMap<>();
    PDFTextObject textobj = new PDFTextObject(fonts);
    ArrayList<PDFTextObject> textlist = new ArrayList<PDFTextObject>();
    int sizesDepth = 0;
    Map<String,String> endnoteExport = new HashMap<>();
    Source source = Source.DEFAULT;
    Map<String,Map<String,String>> ret = new HashMap<>();
    PDFont titleFont = null;
    String articleTitleOld;
    PDDocument document = new PDDocument();
    document.close();
    PDPage page;
    String fullText = "";
    
    try{
	    try{
	        //Ein Überblick über die Struktur von PDF-Dateien liefert der PDF-Standard von Adobe und die Auswertung von PDF-Dateien mittels Preflight
	        
	        //Schritt 1: Erstelle einen NonSequentialPDFParser, der die Crossreference-Tables der pdf auflöst
	        NonSequentialPDFParser rawParser = new NonSequentialPDFParser(input, new RandomAccessBuffer());
	        rawParser.parse();
	        
	        //Schritt 2: Lese aus diesem Parser die erste Seite aus
	        document = rawParser.getPDDocument();  
	        ArrayList<PDPage> allPages = new ArrayList<>();
	        if(document.getDocumentCatalog().getAllPages() instanceof List<?>){
	        	allPages.addAll(document.getDocumentCatalog().getAllPages());
	        }
	        page = (PDPage) allPages.get(0);
	    }catch(Exception ioe){
	    	is.error(ioe.getStackTrace());
	    	ret.put("",endnoteExport);
	    	return ret;
	    }

      //Schritt 3: Lese die mit der ersten Seite verbundenen Fonts aus
      fonts = page.getResources().getFonts();
      textobj.setFonts(fonts);
      System.out.println("Es wurden für die erste Seite " + fonts.size() + " Fonts geladen.");
      
      //Schritt 4: Erstelle einen Stream für die Daten, nachdem der Filter entfernt wurde
      PDStream contents = page.getContents();
      PDFStreamParser parser = new PDFStreamParser(contents.getStream().getUnfilteredStream(), new RandomAccessBuffer());
      parser.parse();
      
      //Schritt 5: Lese die Tokens aus
      List<Object> tokens = parser.getTokens();
      System.out.println("Es wurden für dieses Dokument " + tokens.size() + " Tokens gefunden.");
      for(Object thisToken : tokens){
    	System.out.println(thisToken);
        if(thisToken instanceof PDFOperator){
          String op = ((PDFOperator) thisToken).getOperation();
          if(op.equals("BT")){ //BT ist der Operator, der für den Anfang eines Textblocks signalisiert
            parsingText = true;
            textobj = new PDFTextObject(textobj);
          }else if(op.equals("ET")){ //ET ist der Operator, der das Ende eines Textblocks signalisiert
            parsingText = false;
            textobj.procText();
            textlist.add(textobj);
            System.out.println("Text: " + textobj.getText()
                   + "\t\t@ " + textobj.getObjStyle().getSize() + "\n\n");
            textobj = new PDFTextObject(textobj);
          }else if(op.equals("Tj") || op.equals("TJ")){
            //Am Ende der Textanzeige wird die Verarbeitung dieses Abschnitts beendet
            textobj.add(thisToken);
            textobj.procText();
            textlist.add(textobj);
            System.out.println("Text: " + textobj.getText()
                 + "\t\t@ " + textobj.getObjStyle().getSize() + "\n\n");
            textobj = new PDFTextObject(textobj);
          }else if(parsingText){
            textobj.add(thisToken);
          }
        }else if(parsingText){
          textobj.add(thisToken);
        }
      }
      textlist.add(textobj);
      System.out.println("Text: " + textobj.getText()
      + "\t\t@" + textobj.getObjStyle().getSize());
      parser.clearResources();
    } catch (Exception e) {   
      e.printStackTrace();
    
    }
    
    //Ermittlung einer Ordnung der im Text vorkommenden Textgrößen
    ArrayList<Double> textSizes = new ArrayList<Double>();
    for(PDFTextObject thisText : textlist){
      textSizes.add(thisText.getObjStyle().getSize());
    }
    textSizes.sort(new Comparator<Double>(){
            @Override
            public int compare(Double d1, Double d2){
              return (-1) * Double.compare(d1, d2);
            }
          }
    );
    
    System.out.println("Folgende Ordnung der Textgrößen wurde ermittelt:");
    for(Double size : textSizes){
      System.out.println(size);
    }
    //Ein gefundener Titel wird abgelehnt, wenn die Länge zu klein ist oder wenn es sich um einen Journalnamen handelt.
    do{
    	System.out.println("Durchlauf mit sizesDepth = " + sizesDepth  + " und textsize = " + textSizes.get(sizesDepth));
      articleTitle = "";
      //Ermittlung des Texts mit der Maximalen gewünschten Textgröße
      //Außerdem wird abgebrochen, wenn die Schriftgröße innerhalb der gleichen Textgröße gewechselt wird
      if(textSizes.size() > sizesDepth){
        for(PDFTextObject thisText : textlist){
          if(thisText.getObjStyle().getSize() == textSizes.get(sizesDepth)){
        	  //Ist bereits eine Schriftart festgestellt?
        	  if(titleFont == null){ //Wenn nicht, jetzt machen
        		  titleFont = thisText.currFont;
        	  }else{//Wenn schon: Wurde die Schriftart geändert? Wenn ja, abbrechen!
        		  if(titleFont != thisText.currFont){
        			  break;
        		  }
        	  }
            articleTitle += thisText.getText();
          }
        }
        System.out.println("Analysierter Text: " + articleTitle);
      }
      sizesDepth++;
      titleFont = null;
    }while(textSizes.size() > sizesDepth && (articleTitle.length() < 7 || journals.contains(articleTitle.toLowerCase().replaceAll(" ", ""))));
    
    //Ist Online-Suche aktiviert?
    if(online){
      //Bei Elsevier-Titeln wird eine Suche über Sciencedirect durchgeführt
      //Die Notwendigen Daten (Journalname, Volume, Seiten) stehen direkt am Anfang des Artikels
      
      //Schritt 1: Ist irgendwo im Text die Zeichenfolge "elsevier.com" enthalten? Dann handelt es sich um einen elsevier-Artikel
      source = Source.BIBSONOMY;
      for(PDFTextObject thistext : textlist){
        if(thistext.getText().contains("elsevier.com")){
          source = Source.ELSEVIER;
          break;
        }else if(thistext.getText().contains("links.jstor.org")){
          source = Source.JSTOR;
        }
      }
      
      switch(source){
      case ELSEVIER:
    	  try{
        endnoteExport = parseElsevier(textlist);
    	  }catch(Exception ioe){
    		  is.error(ioe.getStackTrace());
    		  source = Source.BIBSONOMY;
    	  }
        if(!isComplete(endnoteExport)){
        	endnoteExport = completeData(endnoteExport, articleTitle, fullText, document);
        }
        articleTitle = endnoteExport.get("%T");
        break;
      case JSTOR:
    	  try{
          endnoteExport = parseJStor(textlist);
    	  }catch(Exception ioe){
    		  is.error(ioe.getStackTrace());
    		  source = Source.BIBSONOMY;
    	  }
    	  if(!isComplete(endnoteExport)){
          	endnoteExport = completeData(endnoteExport, articleTitle, fullText, document);
          }
          articleTitle = endnoteExport.get("%T");
          
          break;
      case BIBSONOMY:
    	  try{
        endnoteExport = parseBibsonomy(articleTitle);
    	  }catch(IOException ioe){ //Die Bibsonomy-Suche ist nicht gelungen. Titel wird nun gegen Wörterbuch getestet
        	is.warning("Für \"" + articleTitle + "\" ist kein Artikel gefunden wurden.\n");
        	articleTitleOld = articleTitle; //Merke ursprünglich ausgelenen Titel, falls dieser Versuch fehlschlägt
        	ArrayList<String> dict = readDictionary("newfiles/dict/english.txt");
        	articleTitle = dictionaryOnly(articleTitle, dict);
        	is.warning("Suche stattdessen nach \"" + articleTitle + "\".");
        	try{
        	endnoteExport = parseBibsonomy(articleTitle);
        	}catch(IOException ioe2){
        		articleTitle = endnoteExport.get("%T");
        		//Auch die neue Suche war nicht erfolgreich.
        		//Es wird wieder zum ursprünglich gelesenen Titel zurückgekehrt
        		articleTitle = articleTitleOld;
        	}
        }
        
        break;
      case DEFAULT:
        break;
      default:
        break;
      }
    }
    
    //Text im PDF in einen String übertragen
    for(PDFTextObject pto : textlist){
    	fullText += pto.getText();
    }
    
    //Überprüfe, ob die erhaltenen Daten richtig sind
    if(!checkResults(endnoteExport, fullText, document)){
    	//Wenn nein, gebe dies aus und lösche den endnoteExport
    	endnoteExport = new HashMap<>();
    	is.print("Die gefundenen Ergebnisse wurden aus qualitativen Gründen verworfen.\n");
    }
    
    ret.put(articleTitle, endnoteExport);
    return ret;
}
  
  //Gibt einen Endnote-Export graphisch aus
  public void printResults(Map<String, String> endnoteExport){
	for(Map.Entry<String,String> item : endnoteExport.entrySet()){
			System.out.println(item.getKey() + " - " + item.getValue());
	}  
  }
  
  public Map<String, String> completeData(Map<String, String> endnoteExport,
		String articleTitle, String fullText, PDDocument document) {
	Map<String, String> ret = new HashMap<>();
	Map<String,String> parseBib;
	String usingTitle;
	String[] checkKeys = {"%T","%A","%P","%0","%V","%N","%Y","%B"};
	
	System.out.println("Vervollständigung der Daten wird begonnen");
	System.out.println("Im Moment liegen folgende Daten vor:");
	printResults(endnoteExport);
	
	//Entscheide, anhand welchen Titels eine Bibsonomy-Abfrage durchgeführt wird
	usingTitle = (endnoteExport.getOrDefault("%T", "").equals("")) ? articleTitle : endnoteExport.get("%T");
	
	try{
	parseBib = parseBibsonomy(usingTitle);
	}catch(Exception ioe){
		//Unter dem Titel wurde nichts gefunden. 
		//Suche nach Namen der Autoren + %B
		usingTitle = (endnoteExport.getOrDefault("%A", "").equals("")
				&& endnoteExport.getOrDefault("%B", "").equals(""))  ? 
						endnoteExport.get("%A").replace(';', ' ').replace('&', ' ') + " " + endnoteExport.get("%B") :
							null;
		try{
			if(usingTitle != null){
				parseBib = parseBibsonomy(usingTitle);
			}else{
				throw new IOException();
			}
		}catch(Exception ioe2){
			return endnoteExport;
		}
	}
	
	//Sind die über Bibsonomy erlangten Daten akzeptabel?
	if(checkResults(parseBib,fullText,document)){
		for(String key : checkKeys){
			if(!endnoteExport.containsKey(key) || endnoteExport.get(key).trim().equals("")){
				ret.put(key, parseBib.getOrDefault(key, ""));
			}else{
				ret.put(key, endnoteExport.get(key));
			}
		}
	}else{ //Sonst: Ursprungsdaten zurückgeben
		ret = endnoteExport;
	}
	
	System.out.println("Vervollständigung der Daten ist beendet.");
	System.out.println("Es liegen nun folgende Daten vor:");
	printResults(ret);
	
	return ret;
}

/*
   * checkResults überprüft, ob die Informationen, die über den Artikel gefunden worden,
   * mit den Informationen, die aus der PDF erkennbar sind, übereinstimmen.
   * 	- Passt die Seitenzahl?
   * 	- Befindet sich der Titel im Dokument?
   * 	- Befinden sich die Namen der Autoren im Dokument?
   */
  public boolean checkResults(Map<String, String> endnoteExport,
		String fullText, PDDocument document) {
	  
	//Heuristik: Anhand der PDF-Informationen wird ein Score ermittelt.
	//Ist dieser Score über 5, gilt die PDF als übereinstimmend
	  
	  
	int score = 0;
	
	//Variablen für die Seitenzahl
	int pagesNum = 0;
	int exportPagesNum = 0;
	String pagesExport;
	int pageStart = 0;
	int pageEnd = 0;
	int thisNum;
	boolean parseEnd = false;
	
	//Variablen für den Titel
	String exportTitle = "";
	double confirmScore = 0.0;
	
	//Variablen für die Autor*innen
	String exportAuthors = "";
	String[] arrayAuthors;
	ArrayList<String> names = new ArrayList<>();
	double nameScore = 0.0;
	
	//fullText wird zu lowerCase geändert
	fullText = fullText.toLowerCase();
	
	//Überprüfung der Seitenzahl
	
	//Ermittle die Anzahl der Seiten im PDF-Dokument
	pagesNum = document.getNumberOfPages();
	
	//Ermittle die Anzahl der Seiten laut endnoteExport
	pagesExport = endnoteExport.get("%P");
	
	if(pagesExport != null){
		for(char c : pagesExport.toCharArray()){
			if(CharUtils.isAsciiNumeric(c)){
				thisNum = CharUtils.toIntValue(c);
				
				if(!parseEnd){ //Wird im Moment die Start-Seitenzahl ausgelesen?
					if(pageStart == 0){
						//Wurde noch keine Zahl für pageStart eingelesen, wird jetzt die erste Zimmer gesetzt
						pageStart = thisNum;
					}else{ //Ansonsten wird die Ziffer angefügt
						pageStart = pageStart * 10 + thisNum;
					}
				}else{
					if(pageEnd == 0){
						//Wurde noch keine Zahl für pageStart eingelesen, wird jetzt die erste Zimmer gesetzt
						pageEnd = thisNum;
					}else{ //Ansonsten wird die Ziffer angefügt
						pageEnd = pageEnd * 10 + thisNum;
					}
				}
			}else{
				if(pageStart != 0){
					parseEnd = true;
				}
			}
		}
		
		//Berechnung der Seitenanzahl
		exportPagesNum = pageEnd - pageStart + 1;
		
		if(pagesNum == exportPagesNum){
			//Wenn die Seitenzahlen übereinstimmen, gibt es vier Punkte
			score += 4;
		}else if(pagesNum == 0 || exportPagesNum == 0){
			//Keine Punkte, wenn eine der Zahlen 0 ist
		}else if(Math.abs(pagesNum - exportPagesNum) == 1){
			//Wenn die Seitenzahl um genau 1 abweicht, gibt es zwei Punkte
			score += 2;
		}
		else if(Math.abs((pagesNum / exportPagesNum) - 1) < 0.05){
			//Wenn die Richtige Seitenzahl um höchstens 5% abweicht, gibt es einen Punkt
			score += 1;
		}
	}else{
		//Wenn keine Seitenangaben im Endnote-Export vorhanden sind, werden zwei Punkte vergeben
		score += 2;
	}
	
	//Überprüfung des Titels
	
	//Einlesen des Titels aus EndnoteExport
	exportTitle = endnoteExport.get("%T");
	
	if(exportTitle != null){
		
		//Wenn der Titel komplett im Text enthalten ist, gibt es drei
		if(fullText.contains(exportTitle.toLowerCase())){
			confirmScore = 3;
			score += confirmScore;
		}else{
			for(String thisWord : exportTitle.split(" ")){ //Die Wörter, die aus dem Titel im Text sind, werden gezählt
				if(fullText.contains(thisWord.toLowerCase())){
					confirmScore += 1.0;
				}
			}
			
			//Danach wird durch die Anzahl der Wörter im Titel geteilt
			confirmScore /= exportTitle.split(" ").length;
			
			//Nun enthält confirmScore die Übereinstimmung in Prozent
			//Es wird hoch 1.5 genommen, dann mit zwei Multipliziert, dann gerundet
			confirmScore = Math.pow(confirmScore, 1.5);
			confirmScore *= 2;
			confirmScore = Math.round(confirmScore);
			
			//Dieser Wert (maximal 2) wird dem Score hinzugefügt
			score += confirmScore;
		}
	}
	
	//Überprüfung der Autor*innen-Namen
	exportAuthors = endnoteExport.get("%A");
	
	if(exportAuthors != null){
		//Ermittlung der Autor*innen als Array
		arrayAuthors = StringUtils.split(exportAuthors, "&;");
		
		//Füllen der names-Liste
		for(String author : arrayAuthors){
			for(String name : author.split(",")){
				names.add(name);
			}
		}
		
		//Nach allen Namen wird im Text gesucht
		for(String name : names){
			//Wenn ein Name gefunden wird, wird der nameScore um 1 erhöht
			if(fullText.contains(name.toLowerCase())){
				nameScore++;
			}
		}
		
		//nameScore wird zum Prozentualanteil der gefundenen Namen transformiert
		nameScore /= names.size();
		
		//Wenn es zwei oder weniger Namen gibt, sind für Namen maximal 2 Punkte möglich
		if(names.size() <= 2){
			nameScore *= 2;
		}else if(names.size() <= 4){ //Wenn es vier oder weniger Namen gibt, sind für Namen max. 3 Punkte möglich
			nameScore *= 3;
		}else{ //Sonst sind maximal vier Punkte möglich
			nameScore *= 4;
		}
		
		nameScore = Math.round(nameScore);
		score += nameScore; //Gerundeter Wert wird zum Score hinzugefügt
	}
	
	//Die Qualitätsauswertung ist jetzt vollständig
	System.out.println("\nGesamt-Score: " + score);
	System.out.println("Seiten-Score: " + (score-confirmScore-nameScore));
	System.out.println("Page-Start: " + pageStart);
	System.out.println("Page-End: " + pageEnd);
	System.out.println("PDF-Seiten: " + pagesNum);
	System.out.println("Endnote-Seiten: " + exportPagesNum);
	System.out.println("Titel-Score: " + confirmScore);
	System.out.println("Name-Score: " + nameScore + "\n");
	
	//Die Testung ist erfolgreich, wenn es mindestens 5 Punkte gibt
	return (score >= 5);
}
  

public Map<String,String> parseJStor(ArrayList<PDFTextObject> textlist) throws Exception{
    Map<String,String> ret = new HashMap<>();
    String jstorLink = "";
    String tempString;
    int charCounter = -1;
    URL jstorURL;
    int startIndex = 0,
      endIndex = 0;
    String risDoc = "";
    String jsCode = "";
    
    webClient.getCookieManager().setCookiesEnabled(true); //Aktiviere Cookies, damit JStor funktioniert
    //Ermitteln des Links zum JStor-Dokument
    for(PDFTextObject thisText : textlist){
      if(thisText.getText().contains("http://links") || charCounter != -1){
        tempString = thisText.getText();
        
        if(charCounter == -1){
          charCounter = tempString.indexOf("http://");
        }
        
        while(charCounter != -1){
          try{
            jstorLink += tempString.charAt(charCounter++);
            if(tempString.charAt(charCounter) == ' '){
              charCounter = -1;
            }
          }catch(IndexOutOfBoundsException ioe){
            charCounter = -1;
          }
        }
      }
    }
    
    //Aufrufen der JStor-Seite
      jstorURL = new URL(jstorLink);
      webClient.getCookies(jstorURL);
      Page page = webClient.getPage(jstorURL);
        WebResponse response = page.getWebResponse();
        String content = response.getContentAsString();
      System.out.println("Es wurde soeben folgende Seite geöffnet: " + jstorURL.toString());
      
      System.out.println(content);
        if(content.contains("class=\"export-citation\"")){ //In der nächsten Zeile ist der Link zum Abruf der Zitationsdaten
          content= content.substring(content.indexOf("class=\"export-citation\""));
          startIndex = StringUtils.indexOf(content, "<a href=\"") + 9;
          endIndex = StringUtils.indexOf(content, "\">", startIndex);
          jstorURL = new URL("http://jstor.org" + content.substring(startIndex, endIndex)); //jstorURL enthält jetzt die URL zur Seite, in der man die Zitationsform wählen kann
        }
      
      //Öffne Stream für den neuen Inhalt von jstorURL
      page = webClient.getPage(jstorURL);
      response = page.getWebResponse();
      content = response.getContentAsString();
      
      if(content.contains("userAction=export&format=refman")){
        System.out.println("Der gesuchte Code userAction=export&format=refman wurde gefunden. Es wird nun der Link extrahiert.");
        //Schneide Text so ab, dass das erste Anführungszeichen das des Links ist
        content = content.substring(content.indexOf("sumbitToAction")-250);
        
        startIndex = StringUtils.indexOf(content, "sumbitToAction");
        endIndex = StringUtils.indexOf(content,"\');",startIndex);
        jsCode = content.substring(startIndex, endIndex+3);
      }

      risDoc = ((HtmlPage) page).executeJavaScript(jsCode).getNewPage().getWebResponse().getContentAsString("UTF-8");
      ret = RIStoEndNote(risDoc);
    
    return ret;
  }
  
  public Map<String,String> parseBibsonomy(String articleTitle) throws IOException{
    String endnoteText = "";
    Map<String,String> endnoteItems = new HashMap<>();
    
    //Leerzeichen und Sonderzeichen korrigieren
    articleTitle = StringUtils.replace(articleTitle, "+", "%2B");
      articleTitle = StringUtils.replace(articleTitle, " ", "+");
      articleTitle = StringUtils.replace(articleTitle, ":", "%3A");
      articleTitle = StringUtils.replace(articleTitle, ";", "%3B");
      articleTitle = StringUtils.replace(articleTitle, "?", "%3F");
      articleTitle = StringUtils.replace(articleTitle, "@", "%40");
      articleTitle = StringUtils.replace(articleTitle, ",", "%2C");
       
         String html = "";
        URL oracle = new URL("http://www.bibsonomy.org/search/" + articleTitle);
        System.out.println("Accessing " + "http://www.bibsonomy.org/search/" + articleTitle);
            BufferedReader in = new BufferedReader(
            new InputStreamReader(oracle.openStream(), Charsets.UTF_8));

            String inputLine;
            while ((inputLine = in.readLine()) != null){
                html += inputLine;
            }
            in.close();
            int io = StringUtils.indexOf(html, "/layout/endnote/bibtex/");
            String endnote_http = "";
            while(html.charAt(io) != '"'){
              endnote_http += html.charAt(io);
              io++;
            }
            URL exportURL = new URL("http://www.bibsonomy.org" + endnote_http);
            System.out.println("Downloading " + "http://www.bibsonomy.org" + endnote_http);
         in = new BufferedReader(
                 new InputStreamReader(exportURL.openStream(), Charsets.UTF_8));

                 while ((inputLine = in.readLine()) != null){
                     endnoteText += inputLine + "\n";
                 }
                 in.close();
                 endnoteItems = textToMap(endnoteText);
       
       
       
       return endnoteItems;
  }
  
  static public Map<String,String> textToMap(String text){
    Map<String,String> ret = new HashMap<>();
    String[] lines = StringUtils.split(text, '\n');
    String command = "";
    String content = "";

    for(String line : lines){
      line.trim();
      if(line.length() > 3){ //Ist sowohl Befehl, als auch Inhalt vorhanden?
        command = line.substring(0, 2);
        content = line.substring(3);
        content.trim();
      
        ret.put(command, content);
      }
    }
    return ret;
  }
  
  enum currParsing{
    JOURNAL, VOLUME, YEAR, PAGES_FROM, PAGES_TO, QUIT
  }
  
  enum elsevierLayout{
	  OLD_LAYOUT, NEW_LAYOUT
  }
  
  public Map<String,String> parseElsevier(ArrayList<PDFTextObject> textlist) throws Exception{
    String journalParam = "";
    String volumeParam = "";
    String pagesParam = "";
    String journalName = "";
    String yearName = "";
    String fullText = "";
    String workingText = "";
    URL url;
    String searchURL = "";
    int formStart = 0;
    int formEnd = 0;
    int stringPointer = 0;
    String html = "";
    char thisChar;
    String paramName = "";
    String paramValue = "";
    Map<String,String> params = new HashMap<>();
    String risDocument = "";
    Map<String,String> endnoteExport = new HashMap<>();
    Set<String> keySet;
    String inputLine;
    BufferedReader in;
    String key, value;
    HtmlPage page;
    HtmlSubmitInput exportBtn;
    Page pg;
    currParsing parser = currParsing.JOURNAL;
    elsevierLayout layout;
    
    //Es muss überprüft werden, ob es sich um ein altes oder neues Layout von Elsevier-Dokumenten handelt
    
    //Ist der Text "Journal homepage" vorhanden? Dann handelt es sich um das neue Layout
    for(PDFTextObject thistext : textlist){
    	fullText += thistext.getText();
    }
    
    if(fullText.toLowerCase().contains("journal homepage")){
    	layout = elsevierLayout.NEW_LAYOUT;
    }else{
    	layout = elsevierLayout.OLD_LAYOUT;
    }
    
    
    if(layout == elsevierLayout.NEW_LAYOUT){
    	//Im neuen Layout beginnen die Metainformationen direkt am Anfang
    	workingText = fullText;
    }else if(layout == elsevierLayout.OLD_LAYOUT){
    	//Im alten Layout beginnen die Metainformationen nach dem ELSEVIER-Logo
    	workingText = fullText.substring(fullText.indexOf("ELSEVIER ")+9);
    }
    System.out.println(workingText);
    //Schritt 2: Ermittlung von Journalnamen, Volume, Pages
      for(char c : workingText.toCharArray()){
        if(parser == currParsing.QUIT){
          break;
        }
        
        switch(parser){
        case JOURNAL:
          if(CharUtils.isAsciiNumeric(c)){
            parser = currParsing.VOLUME; //Auf Volume-Erkennung wird umgestellt, wenn eine Zahl gefunden wird
            volumeParam += c;
          }else{
            journalParam += c;
          }
          break;
        case VOLUME:
          if(c == '('){
            parser = currParsing.YEAR; //Auf Year-Erkennung wird umgestellt, wenn eine geöffnete Klammer gefunden wird
          }else{
            volumeParam += c;
          }
          break;
        case YEAR:
          if(c == ')'){  //Auf Pages-Erkennung wird umgestellt, wenn die geöffnete Klammer wieder geschlossen wird
            parser = currParsing.PAGES_FROM;
          }else{
        	  if(CharUtils.isAsciiNumeric(c)){
        		  yearName += c;
        	  }
          }
          break;
        case PAGES_FROM:
          if(c== '-'){
            pagesParam += c;
            parser = currParsing.PAGES_TO;
          }else{
            pagesParam += c;
          }
          break;
        case PAGES_TO:
          if(CharUtils.isAsciiNumeric(c) || c == ' '){
            pagesParam += c;
          }else{
            parser = currParsing.QUIT;
          }
          break;
        default:
          break;
        }
      }
    //Deklariere Variablen, die Daten für Entnote darstellen, sollten sie nicht in der RIS-Datei sein
    journalName = journalParam.trim();
    
    journalParam = journalParam.trim().replace(' ', '+');
    volumeParam = volumeParam.trim();
    pagesParam= pagesParam.trim();
    System.out.println("Die Analyse findet anhand folgender Daten statt: \nJournalname: " + journalParam + "\nVolume: " + volumeParam + "\nPages: " + pagesParam);
    //Schritt 3: Ermittlung der Variablen, die für eine gültige GET-Abfrage nötig sind aus dem HTML-Quelltext
    url = new URL("http://www.sciencedirect.com");
    System.out.println("Accessing " + "http://www.sciencedirect.com");
      in = new BufferedReader(
        new InputStreamReader(url.openStream(), Charsets.UTF_8));

        while ((inputLine = in.readLine()) != null){
            html += inputLine;
        }
        in.close();
        formStart = StringUtils.indexOf(html, "<form id=\"quickSearch\" name=\"qkSrch\" method=\"get\" target=\"_top\" action=\"/science\">");
        html = html.substring(formStart);
        formEnd = StringUtils.indexOf(html, "</form>");
        html = html.substring(0, formEnd); //Alles vor dem zu verarbeitendem Formular wird abgeschnitten
        while((stringPointer = StringUtils.indexOf(html, "<input type=\"hidden\" name=\"")) != -1){
           stringPointer += 27; //führt stringPointer zum Beginn des Parameter-Namens
           while((thisChar = html.charAt(stringPointer)) != '"'){
             paramName += thisChar;
             stringPointer++;
           }
           stringPointer += 9; //Führe stringPointer zum Beginn des Parameter-Values
           while((thisChar = html.charAt(stringPointer)) != '"'){
             paramValue += thisChar;
             stringPointer++;
           }
           params.put(paramName, paramValue); //Füge gefundene Werte zu den Parametern hinzu
              
           //Lösche temporäre Variablen
           paramName = "";
           paramValue = "";
           
           //Entferne bereits behandelten Code aus html
           html = html.substring(stringPointer);
         }
            
         searchURL = "http://www.sciencedirect.com/science?_ob=QuickSearchURL&_method=submitForm&_acct=" + params.get("_acct") + "&searchtype=a&_origin=home&_zone=qSearch&md5=" + params.get("md5") + "&";
         //Ermittlung der URL zur Suche
         //Ausgabe von params, zu Testzwecken
         for (Map.Entry<String, String> entry : params.entrySet()) {
           key = entry.getKey();
           value = entry.getValue();
           System.out.println("Es wurde der Parameter (" + key + "," + value + ") gefunden");
         }
         //Zur URL noch die Daten über Journal, Volume, Seiten hinzufügen
         searchURL += "qs_title=" + journalParam + "&qs_vol=" + volumeParam + "&qs_pages=" + pagesParam;
         System.out.println(searchURL);
         
        page = this.webClient.getPage(searchURL); //Öffne Seite
        exportBtn = page.getHtmlElementById("export_button"); //Rufe Link auf, der zum RIS-Dokument führt
        pg = exportBtn.click();
        risDocument = pg.getWebResponse().getContentAsString("UTF-8");
        System.out.println("Es wurde das folgende RIS-Dokument geladen: \n" + risDocument);
        endnoteExport = RIStoEndNote(risDocument);
        
        /* Es wird jetzt überprüft, ob folgende Informationen vorhanden sind, wenn nicht, können sie ergänzt werden,
         * da sie bereits bekannt sind:
         * - Journal
         * - Volume
         * - Year
         * - Pages-From
         * - Pages-To
         */
        
        keySet = endnoteExport.keySet();
        
        //Ist Journal ('%B') enthalten?
        if(!keySet.contains("%B")){
        	endnoteExport.put("%B", journalName);
        }
        
        //Ist Year ('%D') enthalten?
        if(!keySet.contains("%D")){
        	endnoteExport.put("%D", yearName);
        }
        
        //Ist Volume ('%V') enthalten?
        if(!keySet.contains("%V")){
        	endnoteExport.put("%V",volumeParam);
        }
        
        //Sind die Pages('%P') enthalten?
        if(!keySet.contains("%P")){
        	endnoteExport.put("%P",pagesParam);
        }
        
        return endnoteExport;
  }
  
  public Map<String,String> RIStoEndNote(String ris){
    String[] lines = StringUtils.split(ris, '\n');
    String command = ""; //Befehl in der RIS-Zeile
    String content = ""; //Inhalt in der RIS-Zeile
    String newCommand = ""; //Befehl in der Endnote-Zeile
    String newContent = ""; //Inhalt in der Endnote-Zeile
    ArrayList<String> authors = new ArrayList<String>();
    ArrayList<String> editors = new ArrayList<String>();
    String pageStart = "";
    String pageEnd = "";
    int size = 0;
    Map<String,String> lineContents = new HashMap<>();
    
    for(String line : lines){
      newCommand = "";
      newContent = "";
      
      //System.out.println("RIStoEndNote: Folgende Zeile wird verarbeitet: " + line);
      //Die Zeile muss mindestens 6 Zeichen haben, sonst ist sie ungültig
      if(line.length() < 2){
        continue;
      }
      
      command = line.substring(0, 2).trim(); //Lese die ersten zwei Zeichen aus, sie stellen den Befehl dar
      if(line.length() >= 7){
        content = line.substring(6).trim(); //Ab dem siebten Zeichen wird der Inhalt eingelesen
      }
      
      System.out.println("RIStoEndNote: Command: " + command + ", Content: " + content);
      
      //Jetzt werden alle im RIS-Format möglichen Befehle durchgegangen
      if(command.equals("TY")){        //Type of reference (must be the first tag)
        newCommand = "%0";
        if(content.equals("ABST")){      //Abstract
          newContent = "Generic";
        }else if(content.equals("ADVS")){  //Audiovisual Material
          newContent = "Audiovisual Material";
        }else if(content.equals("ART")){  //Art work
          newContent = "Artwork";
        }else if(content.equals("BILL")){  //Bill/Resolution
          newContent = "Bill";
        }else if(content.equals("BOOK")){  //Book, Whole
          newContent = "Book";
        }else if(content.equals("CASE")){  //Case
          newContent = "Case";
        }else if(content.equals("CHAP")){  //Book Chapter
          newContent = "Book Section";
        }else if(content.equals("COMP")){  //Computer program
          newContent = "Computer Program";
        }else if(content.equals("CONF")){  //Conference proceeding
          newContent = "Conference Proceedings";
        }else if(content.equals("CTLG")){  //Catalog
          newContent = "Generic";
        }else if(content.equals("DATA")){  //Data file
          newContent = "Dataset";
        }else if(content.equals("ELEC")){  //Electronic Citation
          newContent = "Generic";
        }else if(content.equals("GEN")){  //Generic
          newContent = "Generic";
        }else if(content.equals("HEAR")){  //Hearing
          newContent = "Hearing";
        }else if(content.equals("ICOMM")){  //Internet Communication
          newContent = "Personal Communication";
        }else if(content.equals("INPR")){  //In press
          newContent = "Generic";
        }else if(content.equals("JFULL")){  //Journal (full)
          newContent = "Edited Book";
        }else if(content.equals("JOUR")){  //Journal
          newContent = "Journal Article";
        }else if(content.equals("MAP")){  //Map
          newContent = "Map";
        }else if(content.equals("MGZN")){  //Magazine Article
          newContent = "Magazine Article";
        }else if(content.equals("MPCT")){  //Motion picture
          newContent = "Film or Broadcast";
        }else if(content.equals("MUSIC")){  //Music score
          newContent = "Music";
        }else if(content.equals("NEWS")){  //Newspaper
          newContent = "Newspaper Article";
        }else if(content.equals("PAMP")){  //Pamphlet
          newContent = "Pamphlet";
        }else if(content.equals("PAT")){  //Patent
          newContent = "Patent";
        }else if(content.equals("PCOMM")){  //Personal communication
          newContent = "Personal Communication";
        }else if(content.equals("RPRT")){  //Report
          newContent = "Report";
        }else if(content.equals("SER")){  //Serial (Book, Monograph)
          newContent = "Serial";
        }else if(content.equals("SLIDE")){  //Slide
          newContent = "Generic";
        }else if(content.equals("SOUND")){  //Sound recording
          newContent = "Music";
        }else if(content.equals("STAT")){  //Statute
          newContent = "Statute";
        }else if(content.equals("THES")){  //Thesis/Dissertation
          newContent = "Thesis";
        }else if(content.equals("UNBILL")){  //Unenacted bill/resolution
          newContent = "Bill";
        }else if(content.equals("UNPB")){  //Unpublished work
          newContent = "Unpublished Work";
        }else if(content.equals("VIDEO")){  //Video recording
          newContent = "Audiovisual Material";
        }
      }else if(command.equals("ID")){      //Reference ID
        continue;
      }else if(command.equals("T1")){      //Primary title
        newCommand = "%T";
        newContent = content;
      }else if(command.equals("TI")){      //Book title
        newCommand = "%T";
        newContent = content;
      }else if(command.equals("CT")){      //Title of unpublished reference
        newCommand = "%T";
        newContent = content;
      }else if(command.equals("A1")){      //Primary author
        authors.add(content);
        continue;
      }else if(command.equals("A2")){      //Secondary author
        editors.add(content);
        continue;
      }else if(command.equals("AU")){      //Author (syntax. Last name, First name, Suffix)
        authors.add(content);
        continue;
      }else if(command.equals("A4")){      //Translator (same syntax)
        continue;
      }else if(command.equals("Y1")){      //Primary date
        continue;
      }else if(command.equals("PY")){      //Publication year (YYYY//MM/DD)
        newCommand = "%D";
        newContent = content.substring(0, 4);
      }else if(command.equals("N1")){      //Notes
        newCommand = "%Z";
        newContent = content;
      }else if(command.equals("KW")){      //Keywords (each keyword must be on separate line preceded KW -)
        continue;
      }else if(command.equals("RP")){      //Reprint status (IN FILE, NOT IN FILE, ON REQUEST, (MM/DD/YY)
        continue;
      }else if(command.equals("SP")){      //Start page number
        pageStart = content;
      }else if(command.equals("EP")){      //Ending page number
        pageEnd = content;
      }else if(command.equals("JF")){      //Periodical full name
        newCommand = "%B";
        newContent = content;
      }else if(command.equals("J0")){      //Periodical standard abbreviation
        newCommand = "%B";
        newContent = content;
      }else if(command.equals("JA")){      //Periodical in which article was published
        newCommand = "%B";
        newContent = content;
      }else if(command.equals("J1")){      //Periodical name - User abbreviation 1
        continue;
      }else if(command.equals("J2")){      //Periodical name - User abbreviation 2
        continue;
      }else if(command.equals("VL")){      //Volume number
        newCommand = "%V";
        newContent = content;
      }else if(command.equals("IS")){      //Issue number
        newCommand = "%N";
        newContent = content;
      }else if(command.equals("T2")){      //Title secondary
        newCommand = "%B";
        newContent = content;
      }else if(command.equals("CY")){      //City of Publication
        newCommand = "%C";
        newContent = content;
      }else if(command.equals("PB")){      //Publisher
        newCommand = "%I";
        newContent = content;
      }else if(command.equals("T3")){      //Title series
        continue;
      }else if(command.equals("N2")){      //Abstract
        newCommand = "%X";
        newContent = content;
      }else if(command.equals("SN")){      //ISSN/ISBN
        newCommand = "%@";
        newContent = content;
      }else if(command.equals("AV")){      //Availability
        continue;
      }else if(command.equals("M1")){      //Misc. 1
        continue;
      }else if(command.equals("M3")){      //Misc. 3
        continue;
      }else if(command.equals("AD")){      //Address
        continue;
      }else if(command.equals("UR")){      //Web/URL
        newCommand = "%U";
        newContent = content;
      }else if(command.equals("L1")){      //Link to PDF
        newCommand = "%>";
        newContent = content;
      }else if(command.equals("L2")){      //Link to Full-Text
        continue;
      }else if(command.equals("L3")){      //Related records
        continue;
      }else if(command.equals("L4")){      //Images
        continue;
      }else if(command.equals("ER")){      //End of Reference (must be last tag)
        break;
      }else{
        continue;
        //Handelt es sich um einen Befehl, der nicht verarbeitet werden kann, wird diese Zeile übersprungen
      }
      System.out.println("newCommand: " + newCommand + ", newContent: " + newContent);
      
      lineContents.putIfAbsent(newCommand, newContent); //Die neue Zeile wird in die Einträge-Map geschrieben
      
    }
    //Am Ende müssen noch die Autoren in der für Endnote typischen Syntax eingefügt werden
    size = authors.size();
    System.out.println("authors.size() = " + size);
    if(size > 0){
      System.out.println("authors.get(0)=" + authors.get(0));
      newContent = authors.get(0);
      for(int i = 1; i < size-1; i++){
        newContent += "; " + authors.get(i);
      }
      if(size > 1){
        newContent += "& " + authors.get(size-1);
      }
      System.out.println("Autoreneintrag: command: %A content: " + newContent);
      lineContents.putIfAbsent("%A", newContent);
    }
    
    //Auch die Editoren müssen noch eingefügt werden (beides jeweils nur, wenn auch vorhanden)
    size = editors.size();
    if(size > 0){
      newContent = editors.get(0);
      for(int i = 1; i < size-1; i++){
        newContent += "; " + editors.get(i);
      }
      if(size > 1){
        newContent += "& " + editors.get(size-1);
      }
      
      lineContents.putIfAbsent("%E", newContent);
    }
    //pageStart und pageEnd müssen angegeben werden, wenn vorhanden
    if(pageStart != "" && pageEnd != ""){
      lineContents.putIfAbsent("%P", pageStart + " - " + pageEnd);
    }
    
    return lineContents;
  }
  
  //Entfernt aus einem String alle Wörter, die nicht in einem .txt-Wörterbuch vorhanden sind
  public ArrayList<String> readDictionary(String fileName){
	  ArrayList<String> ret = new ArrayList<String>();
	  String line = "";
	  try{
		  FileReader fr = new FileReader(new File(fileName));
		  BufferedReader bfr = new BufferedReader(fr);
		  while((line = bfr.readLine()) != null){
			  ret.add(line);
		  }
		  bfr.close();
	  }catch(FileNotFoundException fnfe){
		  try{
			  is.error(fnfe.getStackTrace());
		  }catch(Exception ioe){}
	  }catch(IOException ioe){
		  try{
			  is.error(ioe.getStackTrace());
		  }catch(Exception ioe2){}
	  }
	  
	  return ret;
  }
  //Case-Insensitive
  public String dictionaryOnly(String articleTitle, ArrayList<String> dict){
	  String ret = "";
	  String[] words;
	  
	  words = StringUtils.split(articleTitle, " -:,.?!\"()&+*'#~");
	  
	  for(String word : words){
		  //Ist das Word im Wörterbuch enthalten?
		  if(dict.contains(word.toLowerCase().trim())){
			  ret += word + " ";
		  }else if(word.toUpperCase() == word){
			  //Ist das Wort in Blockschrift? Dann könnte es ein Fachbegriff sein.
			  ret += word + " ";
		  }
	  }
	  
	  return ret;
  }
  
  //Überprüft, ob die ermittelten Endnote-Export-Daten komplett sind
  public boolean isComplete(Map<String,String> endnoteExport){
	  boolean ret = true;
	  
	  if (!endnoteExport.containsKey("%T") || endnoteExport.get("%T").trim().equals("")){ //Titel
		  ret = false;
	  }else if(!endnoteExport.containsKey("%B") || endnoteExport.get("%B").trim().equals("")){ //Journalname, etc.
		  ret = false;
	  }else if(!endnoteExport.containsKey("%A") || endnoteExport.get("%A").trim().equals("")){ //Autor*innen
		  ret = false;
	  }else if(!endnoteExport.containsKey("%P") || endnoteExport.get("%P").trim().equals("")){ //Seiten
		  ret = false;
	  }else if(!endnoteExport.containsKey("%Y") || endnoteExport.get("%Y").trim().equals("")){ //Jahr
		  ret = false;
	  }else if(!endnoteExport.containsKey("%0") || endnoteExport.get("%0").trim().equals("")){ //Dokumenttyp
		  ret = false;
	  }else{
		  if(endnoteExport.get("%0").trim().toLowerCase().equals("journal article")){
			  if(!endnoteExport.containsKey("%V") || endnoteExport.get("%V").trim().equals("")){ //Volume
				  ret = false;
			  }else if(!endnoteExport.containsKey("%N") || endnoteExport.get("%N").trim().equals("")){ //Issue
				  ret = false;
			  }
		  }
	  }
	  
	  return ret;
  }
}