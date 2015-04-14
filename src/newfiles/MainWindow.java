package newfiles;

import java.awt.Frame;
import java.awt.TrayIcon.MessageType;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class MainWindow extends Frame{
	private static final long serialVersionUID = 1L;

	MainWindow(Main main){
		super();
		this.addWindowListener(new WindowAdapter(){
			@Override
			public void windowClosing(WindowEvent e) {
				e.getWindow().setVisible(false);
				main.trayIcon.displayMessage("Nyan Cache", "Nyan Cache läuft weiter. Auf Tray-Icon klicken um Fenster zu öffnen.", MessageType.INFO);
			}
		});
	}
}
