package newfiles;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.InvalidPropertiesFormatException;
import java.util.List;
import java.util.Map;

import javax.swing.text.BadLocationException;

import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.exception.NoDataException;
import org.apache.commons.math3.exception.NotPositiveException;
import org.apache.commons.math3.exception.NotStrictlyPositiveException;
import org.apache.commons.math3.exception.NullArgumentException;
import org.apache.commons.math3.exception.NumberIsTooSmallException;
import org.apache.commons.math3.exception.OutOfRangeException;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.MatrixDimensionMismatchException;
import org.apache.commons.math3.linear.NonSquareMatrixException;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealMatrixChangingVisitor;
import org.apache.commons.math3.linear.RealMatrixPreservingVisitor;
import org.apache.commons.math3.linear.RealVector;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSFloat;
import org.apache.pdfbox.cos.COSInteger;
import org.apache.pdfbox.cos.COSNumber;
import org.apache.pdfbox.cos.COSObject;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.io.RandomAccessBuffer;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.util.PDFOperator;

public class PDFGfxAnalytics {
	
	/*PDFGfxAnalytics bietet Funktionen an, anhand derer die Orte der Zeichen in der
	 *	PDF-Datei erkannt werden.
	*/
	
	private PDDocument document;
	private InfoStream is;
	
	//TEXT STATE PARAMETERS
	private TMatrix t_m; //Text matrix
	private TMatrix t_lm; //Text line matrix
	private boolean t_k; //Text knockout
	/*
	 * Unlike other text state parameters, there is no specific operator
	 * for setting this parameter; it can be set only through the
	 * TK entry in a graphics state parameter dictionary by using the gs operator
	 */
	private double t_rise; //Text Rise
	private double t_l; //Leading
	private double t_h; //Horizontal scaling
	private double t_w; //Word spacing (Applies only to the space character, ASCII 32)
	private double t_c; //Character spacing
	private int t_mode; //Text rendering mode
	private PDFont t_f; //Text font
	private double t_fs; //Text font size
	
	
	//KONSTANTEN FÜR TEXT RENDERING MODE
	final static int FILL = 0;
	final static int STROKE = 1;
	final static int FILLSTROKE = 2;
	final static int INVISIBLE = 3;
	final static int FILLCLIP = 4;
	final static int STROKECLIP = 5;
	final static int FILLSTROKECLIP = 6;
	final static int CLIP = 7;
	
	//Funktionen zum Abrufen und Bearbeiten der text state parameters
	protected void setTextMatrix(TMatrix tmatrix){
		this.t_m = tmatrix;
	}
	
	protected void setTextLineMatrix(TMatrix tlmatrix){
		this.t_lm = tlmatrix;
	}
	
	protected void setTextKnockout(boolean knockout){
		this.t_k = knockout;
	}
	
	protected void setTextRise(double rise){
		this.t_rise = rise;
	}
	
	protected void setLeading(double leading){
		this.t_l = leading;
	}
	
	protected void setHorizontalScaling(double hscaling){
		this.t_h = hscaling / 100;
	}
	
	protected void setWordSpacing(double wspacing){
		this.t_w = wspacing;
	}
	
	protected void setCharacterSpacing(double cspacing){
		this.t_c = cspacing;
	}
	
	protected void setTextRenderingMode(int mode){
		this.t_mode = mode;
	}
	
	protected void setFont(PDFont font){
		this.t_f = font;
	}
	
	protected void setFontSize(double fontSize){
		this.t_fs = fontSize;
	}
	
	protected TMatrix getTextMatrix(){
		return this.t_m;
	}
	
	protected TMatrix getTextLineMatrix(){
		return this.t_lm;
	}
	
	protected boolean getTextKnockout(){
		return this.t_k;
	}
	
	protected double getTextRise(){
		return this.t_rise;
	}
	
	protected double getLeading(){
		return this.t_l;
	}
	
	protected double getHorizontalScaling(){
		return this.t_h;
	}
	
	protected double getWordSpacing(){
		return this.t_w;
	}
	
	protected double getCharacterSpacing(){
		return this.t_c;
	}
	
	protected int getTextRenderingMode(){
		return this.t_mode;
	}
	
	protected PDFont getFont(){
		return this.t_f;
	}
	
	protected double getFontSize(){
		return this.t_fs;
	}
	
	PDFGfxAnalytics(PDDocument d, InfoStream i){
		this.document = d;
		this.is = i;
		
		//Default-Werte außerhalb der Text state parameters setzen (diese werden durch BT gesetzt)
		
	}
	
	
	public class Glyph{
		private double x1;		//X-Koordinate oben links
		private double x2;		//X-Koordinate oben rechts
		private double y1;		//Y-Koordinate oben links
		private double y2;		//Y-Koordinate oben rechts
		private int page;
		
		public double getX1(){
			return this.x1;
		}
		
		public double getY1(){
			return this.y1;
		}
		
		public double getX2(){
			return this.x2;
		}
		
		public double getY2(){
			return this.y2;
		}
		
		public int getPageNum(){
			return this.page;
		}
		
	}
	
	public class TMatrix extends Array2DRowRealMatrix{
		private static final long serialVersionUID = 1L;

		TMatrix(){
			super(3,3);
			this.setToIdentity();
		}
		
		TMatrix(COSNumber[] input) throws InvalidPropertiesFormatException{
			this();
			
			double a,b,c,d,e,f;
			
			if(input.length == 6){
				try{
					a = PDFTextObject.getCOSnumericValue(input[0]);
					b = PDFTextObject.getCOSnumericValue(input[1]);
					c = PDFTextObject.getCOSnumericValue(input[2]);
					d = PDFTextObject.getCOSnumericValue(input[3]);
					e = PDFTextObject.getCOSnumericValue(input[4]);
					f = PDFTextObject.getCOSnumericValue(input[5]);
					
					this.updateMatrix(a,b,0,
							c,d,0,
							e,f,1);
					
				}catch(IOException ioe){
					throw new InvalidPropertiesFormatException("Matrix konnte nicht erstellt werden, da"
							+ " unter den Daten ungültige Werte waren.");
				}
			}else{
				throw new InvalidPropertiesFormatException("Matrix konnte nicht erstellt werden,"
						+ " da keine sechs Werte übergeben worden.");
			}
		}
		
		TMatrix(double v00, double v01, double v02,
				double v10, double v11, double v12,
				double v20, double v21, double v22){
			this();
			this.updateMatrix( v00,  v01,  v02,
					 v10,  v11,  v12,
					 v20,  v21,  v22);
		}
		
		public void updateMatrix(double v00, double v01, double v02,
				double v10, double v11, double v12,
				double v20, double v21, double v22){
			
			//Erste Zeile
			this.setEntry(0,0,v00);
			this.setEntry(0,1,v01);
			this.setEntry(0,2,v02);
			
			//Zweite Zeile
			this.setEntry(1,0,v10);
			this.setEntry(1,1,v11);
			this.setEntry(1,2,v12);
			
			//Dritte Zeile
			this.setEntry(2,0,v20);
			this.setEntry(2,1,v21);
			this.setEntry(2,2,v22);
		}

		public void updateMatrix(double a, double b, double c, double d, double e, double f){
			this.updateMatrix(a,b,0,
					c,d,0,
					e,f,1);
		}
		
		public void makeTranslationMatrix(double t_x, double t_y){
			this.updateMatrix(1, 0, 0, 1, t_x, t_y);
		}
		
		public void makeScalingMatrix(double s_x, double s_y){
			this.updateMatrix(s_x, 0, 0, s_y, 0, 0);
		}
		
		public void makeRotationMatrix(double theta){
			this.updateMatrix(Math.cos(theta), Math.sin(theta), -Math.sin(theta), Math.cos(theta),0,0);
		}
		
		public void makeSkewMatrix(double alpha, double beta){
			this.updateMatrix(1, Math.tan(alpha), Math.tan(beta), 1, 0, 0);
		}
		
		//Macht die Matrix zur Identitätsmatrix
		public void setToIdentity(){
			this.updateMatrix(1,0,0,
						 0,1,0,
						 0,0,1);
		}
	}
	
	public class Point extends Array2DRowRealMatrix{
		private static final long serialVersionUID = 1L;
		
		Point(double x, double y){
			super(1,3);
			update(x,y);
		}
		
		public void update(double x, double y){
			this.setEntry(0, 0, x);
			this.setEntry(0, 1, y);
			this.setEntry(0, 2, 1);
		}
		
		public void setX(double x){
			this.setEntry(0,0,x);
		}
		
		public void setY(double y){
			this.setEntry(0,1,y);
		}
		
		public double getX(){
			return this.getEntry(0, 0);
		}
		
		public double getY(){
			return this.getEntry(0, 1);
		}
	}
	
	//Diese Funktion liest alle Zeichen aus dem PDF-Dokument aus und ermittelt deren Koordinaten
	
	@SuppressWarnings("unchecked")
	public void parse(){
		Object tempObj;
		ArrayList<PDPage> pages = new ArrayList<>();
		
		tempObj = document.getDocumentCatalog().getAllPages();
		
		if(tempObj instanceof List<?>){
			pages.addAll((List<PDPage>)tempObj);
		}
		
		for(PDPage page : pages){
			try{
				parsePage(page, pages.indexOf(page));
			}catch(IOException ioe){
				try{
					is.error(ioe.getStackTrace());
				}catch(BadLocationException ble){ ble.printStackTrace();	
				}catch(IOException ioe2) {ioe2.printStackTrace();}
			}
		}
	}
	
	
	//Erledigt das Auslesen der Informationen aus einer Seite
	enum Operators{
		BT, cm, d0, d1, ET, T_STAR, Td, TD, Tj, TJ, Tm, _HYPHEN, _QMARK, Tc, Tw, Tz, TL, Tf, Tr, Ts
	}
	
	public Operators stringToOpEnum(String op){
		if(op.equals("BT")){
			return Operators.BT;
		}else if(op.equals("cm")){
			return Operators.cm;
		}else if(op.equals("d0")){
			return Operators.d0;
		}else if(op.equals("d1")){
			return Operators.d1;
		}else if(op.equals("ET")){
			return Operators.ET;
		}else if(op.equals("T*")){
			return Operators.T_STAR;
		}else if(op.equals("Td")){
			return Operators.Td;
		}else if(op.equals("TD")){
			return Operators.TD;
		}else if(op.equals("Tj")){
			return Operators.Tj;
		}else if(op.equals("TJ")){
			return Operators.TJ;
		}else if(op.equals("Tm")){
			return Operators.Tm;
		}else if(op.equals("'")){
			return Operators._HYPHEN;
		}else if(op.equals("\"")){
			return Operators._QMARK;
		}else if(op.equals("Tc")){
			return Operators.Tc;
		}else if(op.equals("Tw")){
			return Operators.Tw;
		}else if(op.equals("Tz")){
			return Operators.Tz;
		}else if(op.equals("TL")){
			return Operators.TL;
		}else if(op.equals("Tf")){
			return Operators.Tf;
		}else if(op.equals("Tr")){
			return Operators.Tr;
		}else if(op.equals("Ts")){
			return Operators.Ts;
		}else return null;
	}
	
	public void setTextDefaults(){
		this.setTextMatrix(new TMatrix()); //Identitätsmatrix
		this.setCharacterSpacing(0);
		this.setWordSpacing(0);
		this.setHorizontalScaling(1);
		this.setLeading(0);
		this.setTextRenderingMode(FILL);
		this.setTextRenderingMode(0);
		this.setTextKnockout(true);
	}
	
	public void parsePage(PDPage page, int pageNum) throws IOException{
		List<Object> tokens;
		Map<String,PDFont> fonts = new HashMap<>();
		ArrayList<Object> flushObjects = new ArrayList<>();
		
		PDStream contents = page.getContents();
	    PDFStreamParser parser = new PDFStreamParser(contents.getStream().getUnfilteredStream(), new RandomAccessBuffer());
	    parser.parse();
	    
	    tokens = parser.getTokens();
	    
	    //Mit der Seite verbundene Fonts laden
	    fonts = page.getResources().getFonts();
	    
	    /* Relevante PDF-Codes
	     * BT - Begin text object
	     * cm - Concatenate matrix to current transformation matrix
	     * d0 - Set glyph width in Type 3 font
	     * d1 - Set glyph width and bounding box in Type 3 font
	     * ET - End text object
	     * T* - Move to start of next text line
	     * Td - Move text position
	     * TD - Move text position and set leading
	     * Tj - Show text
	     * TJ - Show text, allowing individual glyph positioning
	     * Tm - Set text matrix and text line matrix
	     * '  - Move to next line and show text
	     * " - Set word and character spacing, move to next line, and show text
	     * 
	     * TEXT STATE OPERATORS
	     * Tc - Set character spacing
	     * Tw - Set word spacing
	     * Tz - Set horizontal text scaling
	     * TL - Set text leading
	     * Tf - Set text font and size
	     * Tr - Set text rendering mode
	     * Ts - Set text rise
	     */
	    
	    //Gehe alle Tokens durch
	    for(Object token : tokens){
	    	if(token instanceof PDFOperator){
	    		switch(getValOp(((PDFOperator) token).getOperation(), flushObjects)){
	    		case BT: //Begin text object
	    			//Standardwerte setzen
	    			//font und text size: keine default values
	    			setTextDefaults();
	    			break;
	    		case cm: //Concatenate matrix to current transformation matrix
	    			break;
	    		case d0: //Set glyph width in Type 3 font
	    			break;
	    		case d1: //Set glyph width and bounding box in Type 3 width
	    			break;
	    		case ET: // End text object
	    			/*nothing*/
	    			break;
	    		case T_STAR: //Move to start of next text line
	    			break;
	    		case Td: //Move text position
	    			break;
	    		case TD: //Move text position and set leading
	    			break;
	    		case Tj: //Show text
	    			break;
	    		case TJ: //Show text, allowing individual glyph positioning
	    			break;
	    		case Tm: //Set text matrix and text line matrix
	    			break;
	    		case _HYPHEN: //Move to next line and show text
	    			break;
	    		case _QMARK: //Set word and character spacing, move to next line, and show text
	    			break;
	    		case Tc: //Set character spacing
	    			setCharacterSpacing(((COSNumber) flushObjects.get(0)).doubleValue());
	    			break;
	    		case Tw: //Set word spacing
	    			setWordSpacing(((COSNumber) flushObjects.get(0)).doubleValue());
	    			break;
	    		case Tz: //Set horizontal text scaling
	    			setHorizontalScaling(((COSNumber) flushObjects.get(0)).doubleValue());
	    			break;
	    		case TL: //Set text leading
	    			setLeading(((COSNumber) flushObjects.get(0)).doubleValue());
	    			break;
	    		case Tf: //Set text font and size
	    			setFont(fonts.get((COSString) flushObjects.get(0)));
	    			setFontSize(((COSNumber) flushObjects.get(1)).doubleValue());
	    			break;
	    		case Tr: //Set text rendering mode
	    			setTextRenderingMode(((COSInteger) flushObjects.get(0)).intValue());
	    			break;
	    		case Ts: //Set text rise
	    			setTextRise(((COSNumber) flushObjects.get(0)).doubleValue());
	    			break;
	    		default:
	    			break;
	    				
	    		}
	    		flushObjects = new ArrayList<>(); //FlushObjects wird für den nächsten Operator geleert
	    	}else{
	    		flushObjects.add(token);
	    	}
	    }
	    
	}
	
	private ClassList nl(){
		return new ClassList();
	}
	
	//Diese Klasse wird lediglich benötigt, um die Deklarationen der Operations zu erleichtern
	class ClassList extends ArrayList<Class<?>>{
		private static final long serialVersionUID = 1L;

		ClassList(){
			super();
		}
		
		public ClassList a(Class<?> c){
			this.add(c);
			return this;
		}
		
	}
	
	//Diese Funktion überprüft für einen aktuellen Operator und die flushObjects, ob
	//sie den Regeln für einen bestimmten Operator entsprechen
	protected Operators getValOp(String givenOp, ArrayList<Object> flushObjects){
		Operators ret = null;
		Map<String, ClassList> var = new HashMap<>();
		ClassList toCheck = new ClassList();
		
		var.put("BT", nl()); //keine Operanden
		var.put("cm", nl().a(COSNumber.class).a(COSNumber.class).a(COSNumber.class)
					.a(COSNumber.class).a(COSNumber.class).a(COSNumber.class));
		var.put("d0", nl().a(COSNumber.class).a(COSNumber.class));
		var.put("d1", nl().a(COSNumber.class).a(COSNumber.class).a(COSNumber.class)
					.a(COSNumber.class).a(COSNumber.class).a(COSNumber.class));
		var.put("ET", nl()); //keine Operanden
		var.put("T*", nl()); //keine Operanden
		var.put("Td", nl().a(COSNumber.class).a(COSNumber.class));
		var.put("TD", nl().a(COSNumber.class).a(COSNumber.class));
		var.put("Tj", nl().a(COSString.class));
		var.put("TJ",nl().a(COSArray.class)); //Das Array kann Strings und Zahlenwerte enthalten
		var.put("Tm", nl().a(COSNumber.class).a(COSNumber.class).a(COSNumber.class)
					.a(COSNumber.class).a(COSNumber.class).a(COSNumber.class));
		var.put("'", nl().a(COSString.class));
		var.put("\"", nl().a(COSNumber.class).a(COSNumber.class).a(COSString.class));
		var.put("Tc", nl().a(COSNumber.class));
		var.put("Tw", nl().a(COSNumber.class));
		var.put("Tz", nl().a(COSNumber.class));
		var.put("TL", nl().a(COSNumber.class));
		var.put("Tf", nl().a(COSString.class).a(COSNumber.class));
		var.put("Tr", nl().a(COSInteger.class));
		var.put("Ts", nl().a(COSNumber.class));

		if(areAllInstances(flushObjects, var.get(givenOp))){
			return stringToOpEnum(givenOp);
		}else{
			return null;
		}
	}

	public boolean areAllInstances(ArrayList<Object> flushObjects, ClassList toCheck){
		
		boolean ret = true;
		
		if(toCheck == null || flushObjects.size() == toCheck.size()){
			
			for(int i = 0; i < flushObjects.size(); i++){
				ret = ret && toCheck.get(i).isInstance(flushObjects.get(i));
			}
			
		}else{
			return false;
		}
		
		return ret;
	}
	
}
