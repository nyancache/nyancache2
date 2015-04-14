package newfiles;
import java.util.Comparator;


public class CompFileSize implements Comparator<Entry>{

	private int signum(long i){
		if(i == 0) return 0;
		if(i > 0) return 1; else return -1;
	}
	
	public int compare(Entry f1, Entry f2){
		return signum(f1.getSize() - f2.getSize());
	}
	
}
