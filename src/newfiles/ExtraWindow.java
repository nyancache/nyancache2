package newfiles;

import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class ExtraWindow extends Frame{
	private static final long serialVersionUID = 1L;

	ExtraWindow(){
		super();
		this.addWindowListener(new WindowAdapter(){
			@Override
			public void windowClosing(WindowEvent e) {
				e.getWindow().dispose();
			}
		});
	}
}
