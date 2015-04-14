package newfiles;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.awt.Button;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.Checkbox;
import java.awt.FileDialog;
import java.awt.Font;
import java.awt.Label;
import java.awt.SystemTray;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.TrayIcon;
import java.io.File;  
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

/*
 * Ziele für Version 1.1.3:
 * TODO Erstellung der Graphischen Analyse des Texts
 * TODO Alle Constanten werden als Interface ausgelagert
 */

public class Main {
	
public static final String VERSION = "1.1.3";
  public static final int BUTTON_START = 24;
  public static final int BUTTON_HEIGHT = 24;
  public static final int BUTTON_WIDTH = 300;
  public static final int STOPPER_HEIGHT = 10;
  public static final int STATUS_HEIGHT = 25;
  public static final int PROGRAM_WIDTH = 900;
  public static final int WINDOWBAR_HEIGHT = 25;
  public static final int WINDOWBAR_WIDTH = 2;
  public static final ImageIcon ICON = new ImageIcon("BiB-DB/newfiles_icon.png");
  public static final Color BG_COLOR = new Color(240, 240, 240);
  public static final String USER_AGENT = "generic";

  LinkedList<File> finallist = new LinkedList<File>();
  LinkedList<File> readlist = new LinkedList<File>();
  LinkedList<File> currlist = new LinkedList<File>();
  LinkedList<Entry> entryList = new LinkedList<Entry>();
  ArrayList<ArrayList<Integer>> doublicates = new ArrayList<ArrayList<Integer>>(); // 2-Dimensionales
                                            // Array
                                            // fï¿½r
                                            // DublikatMainThread());
  LinkedList<File> newFiles = new LinkedList<File>();
  LinkedList<File> currentFiles = new LinkedList<File>();

  boolean doublicatesChecked = false;
  boolean endnoteReadOnly = false;
  InfoStream is;
  MainWindow window = new MainWindow(this);
  ExtraWindow newFilesWindow = new ExtraWindow();
  ExtraWindow infoWindow = new ExtraWindow();
  ExtraWindow alternatingWindow = new ExtraWindow();
  ExtraWindow pdfAnalysisWindow = new ExtraWindow();
  
  Label status = new Label();
  Label eLabel0 = new Label();
  Label eLabel1 = new Label();
  BaseButton button0 = new BaseButton("Neue Dateien anzeigen");
  BaseButton button1 = new BaseButton("Speichern der aktuellen Datei-Liste");
  BaseButton button2 = new BaseButton("Wiederholung des Suchdurchlaufs");
  BaseButton buttonD = new BaseButton("Analyse der PDF-Dateien");
  BaseButton button3 = new BaseButton("Interne Duplikate Suchen");
  BaseButton buttonA = new BaseButton("Nicht-Duplikate speichern");
  BaseButton button4 = new BaseButton("Aufrufen der neuen PDF-Dateien");
  BaseButton button5 = new BaseButton("Endnote öffnen");
  BaseButton button6 = new BaseButton("Zweit-Datenbank auf Endnote öffnen");
  BaseButton button7 = new BaseButton("Repräsentation der Endnote-DB aktualisieren");
  BaseButton button9 = new BaseButton("Erfassen-Ordner korrigieren");
  BaseButton buttonB = new BaseButton("Alternierende Zahlen generieren");
  BaseButton buttonC = new BaseButton("Hintergrund-OCR aktivieren");
  KillButton button8 = new KillButton("Beenden des Programms", window, is);

  JScrollPane infoScroll;
  JTextPane infoField = new JTextPane();

  // Felder für die Generierung von alternierenden Zahlen
  TextField fieldLeft = new TextField();
  TextField fieldRight = new TextField();
  TextArea output = new TextArea("", 0, 0, TextArea.SCROLLBARS_VERTICAL_ONLY);

  String statusCache = "";
  Diagnostics diag;
  
  Threads threadMgr = new Threads(this);
  
  Thread threadEndnote = new Thread(threadMgr.newCheckEndnote());
  Thread threadNewFiles = new Thread(threadMgr.newCheckNewFiles());
  Thread threadRunDx = new Thread(threadMgr.newRunDx());
  
  //Deklaration von Icon und SystemTray
  SystemTray st = SystemTray.getSystemTray();
  TrayIcon trayIcon = new TrayIcon(ICON.getImage(), "So running. Much Nyan Cache. Wow.");
  
  Document thisDoc;

  // Erstelle eine Liste mit allen Dateien, die in der directories-Datei
  // gespeichert sind
  @SuppressWarnings("unchecked")
  private static LinkedList<File> getReadlist() {
    LinkedList<File> readlist = new LinkedList<File>();

    try {
      ObjectInputStream in = new ObjectInputStream(new FileInputStream(
          Module.DIRECTORIES));
      Object obj = in.readObject();
      if (obj instanceof LinkedList) {
        readlist = (LinkedList<File>) obj;
      } // end if
      in.close();
    } catch (Exception ioe) {
      System.out.print(ioe);
    } // end try-catch

    return readlist;
  }

  // Erstelle eine Liste mit allen Dateien, die im Moment existieren
  private static LinkedList<File> getCurrList() {
    return search(System.getProperty("user.dir"));
  }

  // Erstelle eine Liste mit allen Dateien, die neu sind und nicht in der
  // directories enthalten sind
  private static LinkedList<File> getFinalList(LinkedList<File> currlist,
      LinkedList<File> readlist) {
    Collection<File> col = new LinkedList<File>(readlist);
    currlist.removeAll(col);
    return currlist;
  }

  // Bereite die Informationen so vor, dass eine Zusammenfassung der
  // Suchergebnisse im Fenster gezeigt werden können
  public void prepInfo(LinkedList<File> finallist,
      ArrayList<ArrayList<Integer>> doublicates,
      LinkedList<Entry> entryList) {
    // MÃ¶glicher Fehler: Wenn die IDs innerhalb der Listen nicht
    // Ã¼bereinstimmen, gibt es Probleme (also die Indezes der doublicates
    // sich auf andere EintrÃ¤ge beziehen als die der entryList
    int size = finallist.size();
    int dubnum = 0;

    System.out.println("doublicates.size() = " + doublicates.size());
    System.out.println("finallist.size() = " + finallist.size());

    if(doublicatesChecked == true){
      if (doublicates.size() == size) {
        for (int i = 0; i < size; i++) {
          if (doublicates.get(i).size() != 0) { // Sind Dublikate fÃ¼r
            // diesen Eintrag
            // gefunden?
            dubnum++;
          }
        }// end for
      }
    }
    printInfo(size, dubnum);
  }

  @SuppressWarnings("unchecked")
  private ArrayList<ArrayList<Integer>> getDuplicates(boolean online, PrintWriter pw, boolean tryRename) {
    ArrayList<ArrayList<Integer>> ret = new ArrayList<ArrayList<Integer>>();
    Map<ArrayList<Integer>,String> retMap = new HashMap<>();
    ArrayList<Module.PathDuplicatesSet> knownDuplicates = new ArrayList<Module.PathDuplicatesSet>();
    int size;
    File thisFile;
    String pathString;
    ArrayList <String> pwStrings = new ArrayList<String>();
    boolean duplicateFound;
    
    buttonD.setEnabled(false);
    // Aktuelle fluidDuplicates-Liste wird aufgerufen
    knownDuplicates = Module.getFluidDuplicates();
    size = finallist.size();
    for (int i = 0; i < size; i++) {
      setStatus("Suche nach Duplikaten... (" + (i + 1) + " von " + size
          + ")");
      thisFile = finallist.get(i);
      pathString = thisFile.getName() + thisFile.lastModified();

      // Überprüfe, ob für diese Datei schon nach Duplikaten gesucht
      // wurde, wenn ja, rufe diese auf
      duplicateFound = false;
      for (Module.PathDuplicatesSet thisDuplicate : knownDuplicates) {
        if (thisDuplicate.path.equals(pathString)) {
          System.out
              .println("Übereinstimmung gefunden! Duplikat muss nicht kontrolliert werden.");
          duplicateFound = true;
          ret.add(thisDuplicate.duplicates);
        }
      }

      // Wenn für diese Datei noch nicht nach Duplikaten gesucht wurde,
      // wird dies nun gemacht
      if (!duplicateFound) {
        retMap = (Module.getNewDoublicates(new Entry(thisFile),
            entryList, is, online, tryRename));
        ret.add((ArrayList<Integer>)retMap.keySet().toArray()[0]);
        pwStrings.add((String)retMap.values().toArray()[0]);
      }

      buttonD.setEnabled(true);
    }

    if(online){
      for(String thisString : pwStrings){
        pw.print(thisString + "\n\n");
      }
    }
    // Neue fluidDuplicates-Liste wird gespeichert
    Module.saveFluidDuplicates(ret, finallist);
    return ret;
  }

  public void main(String[] args) {
    Date today = new Date();
    Date lastMod = new Date((new File(Module.DATABASE)).lastModified());
    Calendar calToday = Calendar.getInstance();
    Calendar calLastMod = Calendar.getInstance();
    SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy hh:mm");

    calToday.setTime(today);
    calLastMod.setTime(lastMod);
    try {
      is = new InfoStream("output.html");
    } catch (Exception ioe) {
      System.out.println("Es konnte kein Output-Log erstellt werden");
    }
    // Ermittle Liste mit aktuellen Dateien
    
    init();

    // Befinden wir uns in der ersten Woche des Monats?
    if (calToday.get(Calendar.DAY_OF_MONTH) <= 7) {
      // Wurde das DB-Abbild diesen Monat schon erneuert? Wenn nein,
      // Hinweis.
      if (calLastMod.get(Calendar.MONTH) != calToday.get(Calendar.MONTH)) {
        showInfo("In diesem Monat wurde das DB-Abbild noch nicht erneuert.\nDas letzte Update war am "
            + dateFormat.format(lastMod)
            + ".\n\nBitte denkt auch an den NEWSLETTER und an die RÜCKGABEERINNERUNGEN.");
      }
    }
    try{
    	diag = new Diagnostics(this);
        threadRunDx.start();
    }catch(Exception ioe){
    	try{
    		ioe.printStackTrace();
    	is.error(ioe.getStackTrace());
    	}catch(Exception ioe2){
    		
    	}
    }
    threadNewFiles.start();

    threadEndnote.start();
    
    // Lese gegenwÃ¤rtige Literatur.txt ein
    try{
      is.print("Gegenwärtige Endnote-Datenbank wird eingelesen.\n");
    }catch(Exception ioe){
      ioe.printStackTrace();
    }
    try{
    entryList = Module.readEntries();
    buttonD.setEnabled(true);
      is.print("Die Endnote-Datenbank wurde erfolgreich eingelesen.\n\n");
    }catch(Exception ioe){
      ioe.printStackTrace();
    }
    // Aktiviere den Button, durch den die Datenbank auf interne Dublikate
    // untersucht werden kann
    button3.setEnabled(true);

  }// end function

  // Wrapper fï¿½r die rekursive Funktion unten
  public static LinkedList<File> search(String target) {
    LinkedList<File> working = new LinkedList<File>();

    working = search(target, working);
    return working;
  }

  // Rekursive Funktion
  public static LinkedList<File> search(String target,
      LinkedList<File> working) { // Lese alle von hier ausgehenden
                    // Dateien aus und speichere sie in die
                    // LinkedList
    File folder = new File(target);
    File[] listOfFiles = folder.listFiles();
    LinkedList<File> ll = new LinkedList<File>();

    ll.addAll(Arrays.asList(listOfFiles));

    for (int i = 0; i < listOfFiles.length; i++) {
      if (listOfFiles[i].isDirectory()
          && (listOfFiles[i].getName().startsWith("cache_") || listOfFiles[i]
              .getName().toLowerCase().startsWith("dringend"))) { // Gehe
                                        // alle
                                        // gefundenen
                                        // Ordner
                                        // durch
        working = search(listOfFiles[i].getAbsolutePath(), working);
      } else if (listOfFiles[i].isFile()
          && StringUtils.endsWith(listOfFiles[i].getAbsolutePath()
              .toLowerCase(), ".pdf")) {
        working.add(listOfFiles[i]);
      } // end if
    } // end for

    return working;
  }

  public void openLists() {
    currlist = getCurrList();
    readlist = getReadlist();
    finallist = getFinalList(currlist, readlist);
    enableNewFiles(finallist, currlist);
  }

  // Deaktiviert die Buttons, merkt sich die vorherigen Einstellungen
  public boolean[] disable() {
    boolean[] ret = { button0.isEnabled(), button1.isEnabled(),
        button2.isEnabled(), button3.isEnabled(), button4.isEnabled(),
        button5.isEnabled(), button6.isEnabled(), button7.isEnabled(),
        button8.isEnabled(), button9.isEnabled(), buttonA.isEnabled(),
        buttonB.isEnabled(), buttonC.isEnabled(), buttonD.isEnabled()};
    button0.setEnabled(false);
    button1.setEnabled(false);
    button2.setEnabled(false);
    button3.setEnabled(false);
    button4.setEnabled(false);
    button5.setEnabled(false);
    button6.setEnabled(false);
    button7.setEnabled(false);
    button8.setEnabled(false);
    button9.setEnabled(false);
    buttonA.setEnabled(false);
    buttonB.setEnabled(false);
    buttonC.setEnabled(false);
    buttonD.setEnabled(false);
    return ret;
  }

  // Aktiviert die Buttons wieder anhand der vorherigen Einstellungen
  public void enable(boolean[] cache) {
    button0.setEnabled(cache[0]);
    button1.setEnabled(cache[1]);
    button2.setEnabled(cache[2]);
    button3.setEnabled(cache[3]);
    button4.setEnabled(cache[4]);
    button5.setEnabled(cache[5]);
    button6.setEnabled(cache[6]);
    button7.setEnabled(cache[7]);
    button8.setEnabled(cache[8]);
    button9.setEnabled(cache[9]);
    buttonA.setEnabled(cache[10]);
    buttonB.setEnabled(cache[11]);
    buttonC.setEnabled(cache[12]);
    buttonD.setEnabled(cache[13]);
  }

  public void exception(Exception ioe) {
    fatalException(ioe.toString());
  }

  public void fatalException(String message) {
    // Hier sollte ein Fehlerfenster geÃ¶ffnet werden
    System.exit(1);
  }

  // Gebe eine Zusammenfassung der Suchergebnisse aus
  public void printInfo(int size, int dubnum) {
    if(doublicatesChecked){
      setStatus("Neue Dateien: " + size
          + "                     Davon vielleicht Duplikate: " + dubnum);
    }else{
      setStatus("Neue Dateien: " + size);
    }

  }

  // Aktiviere die Buttons nachdem nach neuen Dateien gesucht wurde
  public void enableNewFiles(LinkedList<File> finallist,
      LinkedList<File> currlist) {
    button0.setEnabled(true);
    button1.setEnabled(true);
    button4.setEnabled(true);
    newFiles = finallist;
    currentFiles = currlist;
  }

  // Aktiviere die Buttons, nachdem auf Dublikate kontrolliert wurde
  public void enableNewFiles(LinkedList<File> finallist,
      ArrayList<ArrayList<Integer>> doubllist, LinkedList<Entry> elist) {
    button0.setEnabled(true);
    button1.setEnabled(true);
    button2.setEnabled(true);
    button4.setEnabled(true);
    newFiles = finallist;
    doublicates = doubllist;
    entryList = elist;
  }

  // Initialisiere die grafische Oberfläche
  public void init() {
	trayIcon.addMouseListener(new MouseListener(){

		@Override
		public void mouseClicked(MouseEvent arg0) {
			window.setVisible(true);
		}

		@Override
		public void mouseEntered(MouseEvent arg0) {
			
		}

		@Override
		public void mouseExited(MouseEvent arg0) {
			
		}

		@Override
		public void mousePressed(MouseEvent arg0) {
			
		}

		@Override
		public void mouseReleased(MouseEvent arg0) {
			
		}
		
	});  
	  
    infoScroll = new JScrollPane(infoField);

    window.setLayout(/* new GridLayout(13,1) */null);
    window.setBackground(BG_COLOR);
    window.setIconImage(ICON.getImage());
    window.add(button0);
    window.add(button1);
    window.add(button4);
    window.add(button2);
    window.add(eLabel0);
    window.add(button5);
    window.add(button6);
    window.add(button7);
    window.add(button3);
    window.add(button9);
    window.add(eLabel1);
    window.add(button8);
    window.add(buttonA);
    window.add(buttonB);
    window.add(buttonC);
    window.add(buttonD);
    // window.add(infoField);
    window.add(infoScroll);

    button0.setEnabled(false);
    button1.setEnabled(false);
    button2.setEnabled(false);
    button3.setEnabled(false);
    button4.setEnabled(false);
    button5.setEnabled(true);
    button6.setEnabled(true);
    button7.setEnabled(true);
    button8.setEnabled(true);
    button9.setEnabled(true);
    buttonA.setEnabled(true);
    buttonB.setEnabled(true);
    buttonC.setEnabled(true);
    buttonD.setEnabled(false);

    button0.setFocusable(false);
    button1.setFocusable(false);
    button2.setFocusable(false);
    button3.setFocusable(false);
    button4.setFocusable(false);
    button5.setFocusable(false);
    button6.setFocusable(false);
    button7.setFocusable(false);
    button8.setFocusable(false);
    button9.setFocusable(false);
    buttonA.setFocusable(false);
    buttonB.setFocusable(false);
    buttonC.setFocusable(false);
    buttonD.setFocusable(false);

    button0.addActionListener(new NewFilesButtonListener());
    button1.addActionListener(new SaveNewFilesButtonListener());
    button2.addActionListener(new RedoFilesButtonListener());
    button3.addActionListener(new IntDoublicatesButtonListener());
    button5.addActionListener(new EndnoteButtonListener());
    button6.addActionListener(new Endnote2ButtonListener());
    button7.addActionListener(new EndnoteRepReadButtonListener());
    button4.addActionListener(new OpenPdfButtonListener());
    button9.addActionListener(new CorrectErfassenListener());
    buttonA.addActionListener(new NonDuplicatesListener());
    buttonB.addActionListener(new AlternatingListener());
    buttonC.addActionListener(new BackgroundOCRListener());
    buttonD.addActionListener(new PDFAnalysisListener());

    button0.setBounds(0, BUTTON_START, BUTTON_WIDTH, BUTTON_HEIGHT);
    button1.setBounds(0, BUTTON_START + BUTTON_HEIGHT, BUTTON_WIDTH,
        BUTTON_HEIGHT);
    button4.setBounds(0, BUTTON_START + 2 * BUTTON_HEIGHT, BUTTON_WIDTH,
        BUTTON_HEIGHT);
    button2.setBounds(0, BUTTON_START + 3 * BUTTON_HEIGHT, BUTTON_WIDTH,
        BUTTON_HEIGHT);
    buttonD.setBounds(0, BUTTON_START + 4 * BUTTON_HEIGHT, BUTTON_WIDTH,
        BUTTON_HEIGHT);
    
    button5.setBounds(0, BUTTON_START + 5 * BUTTON_HEIGHT + STOPPER_HEIGHT,
        BUTTON_WIDTH, BUTTON_HEIGHT);
    button6.setBounds(0, BUTTON_START + 6 * BUTTON_HEIGHT + STOPPER_HEIGHT,
        BUTTON_WIDTH, BUTTON_HEIGHT);
    button7.setBounds(0, BUTTON_START + 7 * BUTTON_HEIGHT + STOPPER_HEIGHT,
        BUTTON_WIDTH, BUTTON_HEIGHT);
    button3.setBounds(0, BUTTON_START + 8 * BUTTON_HEIGHT + STOPPER_HEIGHT,
        BUTTON_WIDTH, BUTTON_HEIGHT);
    buttonA.setBounds(0, BUTTON_START + 9 * BUTTON_HEIGHT + STOPPER_HEIGHT,
        BUTTON_WIDTH, BUTTON_HEIGHT);
    button9.setBounds(0, BUTTON_START + 10 * BUTTON_HEIGHT + STOPPER_HEIGHT,
        BUTTON_WIDTH, BUTTON_HEIGHT);
    buttonB.setBounds(0,
        BUTTON_START + 11 * BUTTON_HEIGHT + STOPPER_HEIGHT,
        BUTTON_WIDTH, BUTTON_HEIGHT);
    // buttonC.setBounds(0, BUTTON_START + 11*BUTTON_HEIGHT +
    // STOPPER_HEIGHT, PROGRAM_WIDTH, BUTTON_HEIGHT);

    button8.setBounds(0, BUTTON_START + 12 * BUTTON_HEIGHT + 2
        * STOPPER_HEIGHT, BUTTON_WIDTH, BUTTON_HEIGHT);

    status.setBounds(0, BUTTON_START + 13 * BUTTON_HEIGHT + 2
        * STOPPER_HEIGHT, PROGRAM_WIDTH, STATUS_HEIGHT);
    // infoField.setBounds(BUTTON_WIDTH, BUTTON_START,
    // PROGRAM_WIDTH-BUTTON_WIDTH, 12*BUTTON_HEIGHT + 2*STOPPER_HEIGHT);
    infoScroll.setBounds(BUTTON_WIDTH, BUTTON_START, PROGRAM_WIDTH
        - BUTTON_WIDTH, 13 * BUTTON_HEIGHT + 2 * STOPPER_HEIGHT);
    thisDoc = infoField.getDocument();
    infoField.setFont(new Font("Helvetica", Font.PLAIN, 12));
    is.addDocument(thisDoc);
    try {
      is.printBold("Nyan Cache " + VERSION + "\n\n");
      SimpleAttributeSet bold = new SimpleAttributeSet();
      StyleConstants.setBold(bold, true);
    } catch (Exception ioe) {
      System.out.println(ioe);
    }
    window.setTitle("Nyan Cache "); // Fenstertitel setzen
    window.setSize(PROGRAM_WIDTH, BUTTON_START + 13 * BUTTON_HEIGHT + 2
        * STOPPER_HEIGHT + STATUS_HEIGHT); // Fenstergrï¿½ï¿½e
                          // einstellen
    status.setAlignment(Label.CENTER);
    window.setResizable(false);
    window.add(status);
    window.setVisible(true); // Fenster (inkl. Inhalt) sichtbar machen
    
    try{
    	//Icon auf System-Tray anzeigen
    st.add(trayIcon);
    trayIcon.setImageAutoSize(true);
    }catch(Exception ioe){
    	try{
    	is.error("Es kann nicht auf das System-Tray zugegriffen werden.");
    	}catch(Exception ioe2){}
    	System.out.println(ioe);
    }
  }

  // Setze den Text des Statuslabels
  public void setStatus(String text) {
    status.setText(text);
  }

  // Setze den Text des Statuslabels, der sich nach einer halben Sekunde
  // wieder zurücksetzt
  public void quickStatus(String text) {
    String currText = status.getText();
    status.setText(text);
    try {
      Thread.sleep(500);
    } catch (Exception ioe) {
    }
    status.setText(currText);
  }

  // Zeigt einen Hinweis im Hinweisfenster an
  public void showInfo(String text) {
    infoWindow.removeAll(); // Entfernt alle alten Elemente aus dem Fenster,
                // damit die neuen Elemente angezeigt werden
                // können

    TextArea info = new TextArea(text, 0, 0, TextArea.SCROLLBARS_NONE);
    CloseButton okButton = new CloseButton("OK", infoWindow);

    infoWindow.setIconImage(ICON.getImage());
    infoWindow.setSize(300, 200);
    infoWindow.setLayout(null);
    infoWindow.setTitle("Hinweis");
    infoWindow.setResizable(false);
    infoWindow.setBackground(new Color(240, 240, 240));
    info.setBackground(new Color(240, 240, 240));
    info.setBounds(WINDOWBAR_WIDTH, WINDOWBAR_HEIGHT, 300, 140);
    info.setEditable(false);
    okButton.setBounds(WINDOWBAR_WIDTH, WINDOWBAR_HEIGHT + 140,
        300 - WINDOWBAR_WIDTH, 60 - WINDOWBAR_HEIGHT - WINDOWBAR_WIDTH);
    infoWindow.add(info);
    infoWindow.add(okButton);


    infoWindow.setVisible(true);
  }

  class RedoFilesButtonListener implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
      doublicatesChecked = false;
      boolean[] cache = disable();

      button2.setEnabled(false);

      openLists();

      setStatus("Ordner werden nach neuen Dateien untersucht.");
      prepInfo(finallist, doublicates, entryList);
      enableNewFiles(finallist, doublicates, entryList);

      System.out.println("newFiles.size() = " + newFiles.size());
      System.out.println("finallist.size() = " + finallist.size());
      System.out.println("doublicates.size() = " + doublicates.size());
      button2.setEnabled(true);
      enable(cache);

    }
  }

  class EndnoteButtonListener implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
      File endnote = new File("DB\\Literatur.enl");

      try {
        ((BaseButton) e.getSource()).setEnabled(false);
        ((BaseButton) e.getSource()).setText("Wird geöffnet");
        Desktop.getDesktop().open(endnote);
        ((BaseButton) e.getSource()).setEnabled(true);
        ((BaseButton) e.getSource()).setText("Öffne Endnote-Datenbank");
      } catch (Exception ioe) {
        System.out.println(ioe);
      }

    }

  }

  class Endnote2ButtonListener implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
      File endnote = new File("BiB-DB\\Neuerfasste Literatur.enl");

      try {
        ((BaseButton) e.getSource()).setEnabled(false);
        ((BaseButton) e.getSource()).setText("Wird geöffnet");
        Desktop.getDesktop().open(endnote);
        ((BaseButton) e.getSource()).setEnabled(true);
        ((BaseButton) e.getSource())
            .setText("Zweit-Datenbank auf Endnote öffnen");
      } catch (Exception ioe) {
        System.out.println(ioe);
      }

    }

  }

  class NewFilesButtonListener implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
      int size;
      TextArea fileArea = new TextArea("", 30, 100,
          TextArea.SCROLLBARS_VERTICAL_ONLY);
      String areaText;

      newFilesWindow.removeAll(); // Leert das Fenster, damit der
                    // erneuerte Textbereich angezeigt wird
      System.out
          .println("NewFilesButtonListener.actionPerformed wird ausgeführt");
      System.out.println("newFiles.size() = " + newFiles.size());
      if (!doublicatesChecked || doublicates.isEmpty()) { // Wenn zum
                                // Zeitpunkt der
                                // Fensterï¿½ffnung
                                // noch keine
                                // Dublikate
                                // festgestellt
                                // wurden, dann
                                // diesen Teil
                                // ausfï¿½hren
        System.out.println("Codeteil für keine Funde wird ausgeführt.");
        areaText = "Folgende neue Dateien wurden gefunden:\n(Bislang keine Duplikate gefunden)\n\n";

        for (File file : newFiles) {
          areaText += "  * "
              + file.getAbsolutePath().substring(
                  file.getAbsolutePath().indexOf("cache"))
              + "\n";
        }

      } else { // Ist die Dublikatkontrolle schon abgeschlossen? Wenn ja,
            // kï¿½nnen diese auch angezeigt werden
        System.out
            .println("Codeteil für gefundene Duplikate wird ausgeführt.");
        areaText = "Folgende neue Dateien wurden gefunden:\n\n";
        for (int i = 0; i < newFiles.size(); i++) {
          areaText += ("* " + newFiles
              .get(i)
              .getAbsolutePath()
              .substring(
                  newFiles.get(i).getAbsolutePath()
                      .indexOf("cache")))
              + "\n";

          if (doublicates.get(i).size() != 0) { // Sind Dublikate fÃ¼r
                              // diesen Eintrag
                              // gefunden?
            size = doublicates.get(i).size();
            areaText += "WARNUNG: Für vorherige Datei bestehen möglicherweise folgende Duplikate, Anzahl "
                + size + "\n";
            for (int j = 0; j < size; j++) {
              areaText += entryList
                  .get(doublicates.get(i).get(j)) + "\n";
            }
          }
        }// end for

      }

      fileArea.setText(areaText);
      fileArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
      fileArea.setEditable(false);

      newFilesWindow.setTitle("Neue Dateien");
      newFilesWindow.setIconImage(ICON.getImage());
      newFilesWindow.add(fileArea);
      newFilesWindow.pack();
      newFilesWindow.setVisible(true);
    }
  }

  class SaveNewFilesButtonListener implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent ae) {
      BaseButton bt = (BaseButton) ae.getSource();
      String btLabel = bt.getText();

      bt.setText("Liste wird gespeichert...");
      bt.setEnabled(false);
      Module.saveDir(currentFiles);
      bt.setEnabled(true);
      bt.setText(btLabel);
    }
  }

  class EndnoteRepReadButtonListener implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent ae) {
      boolean[] cache = disable();
      String statusCache;
      String path;
      ArrayList<String> bibTex = new ArrayList<String>();
      int len = 0;

      FileDialog fd = new FileDialog(window);
      fd.setTitle("Bitte BibTex-Datei mit den Endnote-Einträgen öffnen");
      fd.setVisible(true);
      fd.setDirectory(System.getProperty("user.dir"));
      path = fd.getFile();
      if (path == null) { // Wurde im Dialog auf abbrechen gedrÃ¼ckt?
        quickStatus("Vorgang abgebrochen.");
        enable(cache);
      } else {
        try {
          if (fd.getFiles().length == 1) {
            path = fd.getFiles()[0].getAbsolutePath();
            statusCache = status.getText();
            setStatus("Bitte warten.");
            bibTex = Module.readBibTex(path);
            setStatus("Datei gefunden. Wird verarbeitet.");
            entryList = new LinkedList<Entry>();
            len = bibTex.size();

            for (int i = 0; i < len; i++) {
              setStatus(i + 1 + " von " + len);
              entryList.add(Module.parseEntry(bibTex.get(i)));
            }

            setStatus("Einträge werden gespeichert");
            Module.saveEntries(entryList);

            setStatus("Neue Journale werden erfasst und gespeichert");
            Module.saveJournalList(entryList);

            enable(cache);
            setStatus(statusCache);
          }
        } catch (Exception ioe) {
          System.out.println(ioe);
          System.exit(1);
        }
      }
    }

  }

  class OpenPdfButtonListener implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent ae) {
      Module.open(finallist, new String[] { "pdf" });
    }

  }

  class IntDoublicatesButtonListener implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent ae) {
      ArrayList<ArrayList<Integer>> intDoublicates = new ArrayList<ArrayList<Integer>>();
      ArrayList<ArrayList<Integer>> nonDuplicates = new ArrayList<ArrayList<Integer>>();
      Entry thisEntry;
      String statusCache = status.getText();
      String output = "";
      String path;
      File outFile;

      setStatus("Suche nach Duplikaten wird gestartet...");

      // Einlesen der Nicht-Duplikate-Liste

      nonDuplicates = Module.openNonDuplicates();
      intDoublicates = Module.getDuplicates(entryList, status,
          nonDuplicates);
      System.out.println("Dublikatsuche abgeschlossen");
      System.out.println(intDoublicates.size() + " Einträge gefunden");
      for (ArrayList<Integer> doubl : intDoublicates) {
        output += ("---------------------------------------------\n");
        for (Integer doublIndex : doubl) {
          thisEntry = Module.getEntryByRefID(doublIndex, entryList);
          output += thisEntry.toString();
        }
      }

      setStatus(statusCache);
      System.out.println(intDoublicates);
      JFileChooser chooser = new JFileChooser();
      chooser.setDialogTitle("Wohin soll das Ergebnisprotokoll gespeichert werden?");
      chooser.setDialogType(JFileChooser.SAVE_DIALOG);
      chooser.setCurrentDirectory(new File("./"));
      chooser.showOpenDialog(window);
      path = chooser.getSelectedFile().getAbsolutePath();

      if (path != null) {
        outFile = new File(path);

        try {
          FileOutputStream fos = new FileOutputStream(outFile);
          OutputStreamWriter osw = new OutputStreamWriter(fos);
          osw.write(output);
          osw.close();
          fos.close();
        } catch (Exception ioe) {
          System.out.println(ioe);
        }
      }

    }

  }

  class CorrectErfassenListener implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent ae) {
      // Ermittle den Ordner, aus dem gelesen werden soll
      String dir = "";
      JFileChooser chooser = new JFileChooser();
      chooser.setDialogTitle("Bitte PDF-Ordner angeben");
      chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
      chooser.setCurrentDirectory(new File("./"));
      int returnVal = chooser.showOpenDialog(window);
      if (returnVal == JFileChooser.APPROVE_OPTION) {
        dir = chooser.getSelectedFile().getAbsolutePath();
      }

      // Schritt 1: Lese die Namen aller Ordner im
      // DB/Literatur.Data/PDF-Ordner ein
      String[] arrayOfFiles = new File(dir).list();
      String[] erfassen = new File("Erfassen/Erledigt").list();
      File copyFiles = new File("Erfassen/Erledigt_neu"); // In diesem
                                // Ordner werden
                                // zu
                                // korrigierende
                                // Dateien
                                // eingetragen
      copyFiles.mkdir();
      String statusCache = status.getText();
      setStatus("PDF-Dateien werden überprüft");
      System.out.println("PDF-Dateien werden überprüft");
      // Schritt 2: Gehe alle Ordner durch
      for (String file : arrayOfFiles) {
        System.out.println(file);
        // Sind zwei _ enthalten? Dann ist der PDF-Name korrekt
        // formatiert
        if (StringUtils.countMatches(file, "_") >= 2) {
          File subfolder = new File(dir + "\\" + file);
          System.out.println("\n\n");
          System.out.println("Überprüfe Ordner"
              + subfolder.getAbsolutePath());
          File[] pdfs = subfolder.listFiles();
          // Ist im Ordner eine Datei enthalten?
          if (pdfs.length == 1) {

            // Ist die Datei noch nicht im Erfassen-Ordner?
            if (!ArrayUtils.contains(erfassen, pdfs[0].getName())) {
              System.out
                  .println("[!!!!!] Folgende Datei wurde nicht im Erfassen-Ordner gefunden: "
                      + pdfs[0].getName());

              try {
                // Kopiere Datei nach erfassen_neu
                FileUtils.copyFileToDirectory(pdfs[0],
                    copyFiles, true);
                System.out.println(pdfs[0].getName());
              } catch (Exception ioe) {
                System.out.println(ioe);
              }
            }

          }
        }
      }

      setStatus(statusCache);
      quickStatus("Neue PDFs wurden nach Erledigt_neu kopiert.");
      System.out.println("Fertig.");
    }

  }

  class NonDuplicatesListener implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent ae) {
      ArrayList<ArrayList<Integer>> intDuplicates = new ArrayList<ArrayList<Integer>>();
      String statusCache = status.getText();

      // Schritt 1: Erfasse die aktuellen Duplikate
      setStatus("Suche nach Duplikaten wird gestartet...");
      intDuplicates = Module.getDuplicates(entryList, status,
          new ArrayList<ArrayList<Integer>>());
      System.out.println("\n\n\nintDuplicates:" + intDuplicates);
      // Schritt 2: Speichere die aktuellen Duplikate als
      // "Nicht-Duplikate"
      setStatus("Liste wird gespeichert.");
      Module.saveNonDoublicates(intDuplicates);

      setStatus(statusCache);
      quickStatus("Nicht-Duplikate wurden gespeichert.");
    }
  }

  class AlternatingListener implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent ae) {
      Label labLeft = new Label("Beginn linke Seiten:");
      Label labRight = new Label("Beginn rechte Seiten:");
      BaseButton generate = new BaseButton("Zahlen generieren");
      CloseButton close = new CloseButton("Schließen", alternatingWindow);

      labLeft.setBounds(WINDOWBAR_WIDTH, WINDOWBAR_HEIGHT + 0, 150, 20);
      labRight.setBounds(WINDOWBAR_WIDTH, WINDOWBAR_HEIGHT + 20, 150, 20);
      fieldLeft.setBounds(WINDOWBAR_WIDTH + 150, WINDOWBAR_HEIGHT + 0,
          30, 20);
      fieldRight.setBounds(WINDOWBAR_WIDTH + 150, WINDOWBAR_HEIGHT + 20,
          30, 20);
      output.setBounds(WINDOWBAR_WIDTH + 0, WINDOWBAR_HEIGHT + 40, 180,
          60);
      generate.setBounds(WINDOWBAR_WIDTH, WINDOWBAR_HEIGHT + 100, 180,
          BUTTON_HEIGHT);
      close.setBounds(WINDOWBAR_WIDTH, WINDOWBAR_HEIGHT + 100
          + BUTTON_HEIGHT, 180, BUTTON_HEIGHT);

      generate.addActionListener(new GenerateListener());

      alternatingWindow.removeAll();
      alternatingWindow.setResizable(false);
      alternatingWindow.setTitle("Zahlengenerator");
      alternatingWindow.setIconImage(ICON.getImage());
      alternatingWindow.setLayout(null);
      alternatingWindow.setBackground(BG_COLOR);

      alternatingWindow.add(labLeft);
      alternatingWindow.add(labRight);
      alternatingWindow.add(fieldLeft);
      alternatingWindow.add(fieldRight);
      alternatingWindow.add(output);
      alternatingWindow.add(generate);
      alternatingWindow.add(close);

      alternatingWindow.setSize(180 + WINDOWBAR_WIDTH + 2,
          WINDOWBAR_HEIGHT + 100 + 2 * BUTTON_HEIGHT);
      alternatingWindow.setVisible(true);
    }
  }

  class GenerateListener implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent ae) {
      int left;
      int right;
      int diff = 0;

      try {
        left = Integer.parseInt(fieldLeft.getText());
        right = Integer.parseInt(fieldRight.getText());
      } catch (Exception ioe) {
        output.setText("Eingaben fehlerhaft.");
        return;
      }

      String ret = "";

      // Fall 1: Es wurden in der PDF-Datei zuerst die rechten, dann die
      // linken Dateien angefügt
      if (left > right) {
        diff = left - right;
      } else { // Fall 2: Es wurden in der PDF-Datei zuerst die linken,
            // dann die rechten Dateien angefügt
        diff = right - left;
      }

      // Es werden immer zunächst die linken Seiten angefügt
      for (int i = 0; i < diff - 1; i++) {
        ret += (left + i) + "," + (right + i) + ",";
      }

      ret += (left + diff - 1) + "," + (right + diff - 1); // Letze Seiten
                                  // werden
                                  // separat
                                  // angefügt,
                                  // damit am
                                  // Ende kein
                                  // Komma
                                  // steht

      output.setText(ret);
    }

  }

  class PDFAnalysisListener implements ActionListener {
    
    Checkbox question1 = new Checkbox("Online-Analyse aktiv", true);
    Label question2 = new Label("Name der Zieldatei");
    Checkbox question3 = new Checkbox("Umbenennen der PDFs", true);
    BaseButton ok = new BaseButton("Start");
    CloseButton close = new CloseButton("Abbrechen",pdfAnalysisWindow);
    BaseButton getLoc = new BaseButton("...");
    TextField saveFile = new TextField();
    String savePath = "";
    
    boolean question3memo = true;
    
    @Override
    public void actionPerformed(ActionEvent ae){
      //Zeige ein Fenster an, in dem die Analyse-Einstellungen gewählt werden können
      
      pdfAnalysisWindow.setBounds(100, 100, 250, 165);

      pdfAnalysisWindow.setTitle("Einstellungen");
      pdfAnalysisWindow.setResizable(false);
      pdfAnalysisWindow.add(question1);
      pdfAnalysisWindow.add(question2);
      pdfAnalysisWindow.add(question3);
      pdfAnalysisWindow.add(ok);
      pdfAnalysisWindow.add(close);
      pdfAnalysisWindow.add(getLoc);
      pdfAnalysisWindow.add(saveFile);
      
      question1.setBounds(WINDOWBAR_WIDTH + 5, 30, 200, 15);
      question2.setBounds(WINDOWBAR_WIDTH + 5, 60, 200, 15);
      
      saveFile.setBounds(WINDOWBAR_WIDTH + 5, 80, 200, 25);
      getLoc.setBounds(WINDOWBAR_WIDTH + 205, 80, 20, 25);
      
      question3.setBounds(WINDOWBAR_WIDTH + 5, 110, 200, 15);
      
      ok.setBounds(WINDOWBAR_WIDTH + 100, 130, 50, 25);
      close.setBounds(WINDOWBAR_WIDTH + 155, 130, 80, 25);
      
      question1.addItemListener(new question1Listener());
      getLoc.addActionListener(new GetLocationListener());
      ok.addActionListener(new StartButtonListener());
      pdfAnalysisWindow.setLayout(null);
      pdfAnalysisWindow.setVisible(true);
    }
    
    /* question1Listener achtet darauf, dass die Folgefragen nur dann
     * benatwortet werden können, wenn das erste Item aktiviert ist.
     */
    class question1Listener implements ItemListener {

      @Override
      public void itemStateChanged(ItemEvent arg0) {
        if(arg0.getStateChange() == ItemEvent.DESELECTED){
          question3memo = question3.getState();
          
          question2.setEnabled(false);
          question3.setEnabled(false);
          question3.setState(false);
          getLoc.setEnabled(false);
          saveFile.setEnabled(false);
        }else if(arg0.getStateChange() == ItemEvent.SELECTED){
          question2.setEnabled(true);
          question3.setEnabled(true);
          getLoc.setEnabled(true);
          saveFile.setEnabled(true);
          question3.setState(question3memo);
        }
      }
      
    }
    
    class GetLocationListener implements ActionListener{

      @Override
      public void actionPerformed(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Wohin sollen die Endnote-Importdateien gespeichert werden?");
        chooser.setDialogType(JFileChooser.SAVE_DIALOG);
        chooser.setCurrentDirectory(new File("./"));
        chooser.showOpenDialog(window);
        try{
          savePath = chooser.getSelectedFile().getAbsolutePath();
        }catch(Exception ioe){
          savePath = "";
        }
        saveFile.setText(savePath);
      }
      
    }
    
    class StartButtonListener implements ActionListener {

      @Override
      public void actionPerformed(ActionEvent e) {
        //Überprüfe, ob die im saveFile-Feld angegebene Adresse gültig ist
        String path = saveFile.getText();
        File pathFile = new File(path);
        PrintWriter pw;
        if(question1.getState()){
          try{
            //Soll eine Online-Suche stattfinden?
              FileUtils.touch(pathFile);
              pw = new PrintWriter(new OutputStreamWriter(FileUtils.openOutputStream(pathFile),StandardCharsets.UTF_8));
              setStatus("Duplikat-Suche wird begonnen.");
              pdfAnalysisWindow.setVisible(false);
              try{
            	  doublicates = getDuplicates(question1.getState(), pw, question3.getState());
              }catch(Exception ioe){
            	  is.error("Fehler bei der Suche nach Duplikaten.\n");
            	  is.error(ioe.getStackTrace());
              }
              
              setStatus("Duplikate-Suche abgeschlossen");
              pw.flush();
              pw.close();
   
            
         }catch(Exception ioe){
            try{
              is.error("An die gewünschte Adresse kann nicht geschrieben werden. Daher findet kein Analyse-Vorgang statt.\n");
              is.error(ioe.getStackTrace());
            }catch(Exception ioe2){
              System.out.println(ioe);
            }
         }
        }else{
         setStatus("Duplikat-Suche wird begonnen.");
         pdfAnalysisWindow.setVisible(false);
         doublicates = getDuplicates(question1.getState(), null, question3.getState());
         setStatus("Duplikate-Suche abgeschlossen");        
        } 
        
        doublicatesChecked = true;
        
        prepInfo(finallist, doublicates, entryList);
        enableNewFiles(finallist, doublicates, entryList);
        
        pdfAnalysisWindow.dispose();
      }
    }
  }
  
  class BackgroundOCRListener implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent ae) {
      /*
       * File imageFile = new File("newfiles/eurotext.tif"); Tesseract1
       * instance = new Tesseract1(); // JNA Direct Mapping
       * 
       * try { String result = instance.doOCR(imageFile);
       * //System.out.println(result); } catch (Exception e) {
       * System.err.println(e.getMessage()); }
       */
    }
  }

}