
package newfiles;

import java.awt.TrayIcon;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class Threads {
  
  Main main;
  
  Threads(Main pointerMain){
    this.main = pointerMain;
  }
  
  public CheckEndnote newCheckEndnote(){
    CheckEndnote ret = new CheckEndnote();
    
    return ret;
  }
  
  public CheckNewFiles newCheckNewFiles(){
    CheckNewFiles ret = new CheckNewFiles();
    
    return ret;
  }
  
  public RunDx newRunDx(){
	  RunDx ret = new RunDx();
	  
	  return ret;
  }
  
  public class CheckEndnote implements Runnable {
    
    @Override
    public void run() {
      while (true) {
        try {
          FileOutputStream fos = new FileOutputStream(new File(
              "DB\\Literatur.enl"), true);
          fos.close();
          if (main.endnoteReadOnly == true) { // Ist die Endnote-Datei zum
                          // ersten Mal schreibfähig?
            main.trayIcon.displayMessage("Endnote", "Es wurde Schreibzugriff auf die Endnote-Haupt-DB gestattet.", TrayIcon.MessageType.INFO);
          }
          main.button5.removeAlert();
          main.endnoteReadOnly = false;
        } catch (IOException e) {
        	main.button5.makeAlert();
          main.endnoteReadOnly = true;
        }
        try {
          System.out.println("Endnote überprüft.");
          Thread.sleep(1000 * 60 * 5); // Stelle 5-Minuten-Abstände
                          // ein
        } catch (Exception e) {
          break;
        }
      }
    }
  }

  public class CheckNewFiles implements Runnable {
    @Override
    public void run() {
      int oldSize = 0;
      boolean first = true;
      
      while (true) {
        main.button2.setEnabled(false); // Deaktiviere
                      // "Neue Dateien überprüfen"-Button
        // Lese directories-Datei aus
        main.openLists();

        main.prepInfo(main.finallist, main.doublicates, main.entryList);
        main.enableNewFiles(main.finallist, main.doublicates, main.entryList);
        if (!first && oldSize < main.finallist.size()) {
          main.trayIcon.displayMessage("Neue Dateien","Es wurden neue Dateien in den Cache-Ordnern gefunden", TrayIcon.MessageType.INFO);
          main.doublicatesChecked = false;
        }
        oldSize = main.finallist.size();
        first = false;
        main.button2.setEnabled(true); // Aktiviere Button wieder
        try {
          Thread.sleep(1000 * 60 * 1); // Stelle 1-Minuten-Abstände
                          // ein
        } catch (Exception e) {
          break;
        }

      }

    }
  }
  
  public class RunDx implements Runnable{
	@Override
	public void run() {
		while(true){
			try{
				main.diag.dx();
				Thread.sleep(1000 * 60);
			}catch(Exception ioe){
				try{
					main.is.error(ioe.getStackTrace());
				}catch(Exception ioe2){
					
				}
				break;
			}
		}
	}
	  
  }
}
