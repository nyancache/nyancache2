package newfiles;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.InvalidPropertiesFormatException;
import java.util.List;
import java.util.Map;

import javax.swing.text.BadLocationException;

import org.apache.commons.lang3.StringUtils;
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
import org.apache.fontbox.cmap.CMap;
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
	public	Gfx gfx;

	//TEXT STATE PARAMETERS
	private TMatrix t_m; //Text matrix
	private TMatrix t_lm; //Text line matrix
	private TMatrix ctm; //transformation matrix
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
	
	protected void setTransformatoinMatrix(TMatrix ctmatrix){
		this.ctm = ctmatrix;
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
	
	protected TMatrix getTransformationMatrix(){
		return this.ctm;
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
		
		ctm = new TMatrix(); //Transformationsmatrix wird auf die Einheitsmatrix gesetzt
	}
	
	
	public class TMatrix extends Array2DRowRealMatrix{
		private static final long serialVersionUID = 1L;

		TMatrix(){
			super(3,3);
			this.setToIdentity();
		}
		
		TMatrix(RealMatrix m){
			this();
			this.setColumn(0, m.getColumn(0));
			this.setColumn(1, m.getColumn(1));
			this.setColumn(2, m.getColumn(2));
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
		
		TMatrix(double a, double b, double c, double d, double e, double f){
			this();
			this.updateMatrix(a,b,c,d,e,f);
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
		
		public void transform(Array2DRowRealMatrix mat){ //Multipliziert den Punkt mit der Matrix
			this.update(mat.getEntry(0, 0)*getX()+mat.getEntry(1,0)*getY()+mat.getEntry(2,0),
					mat.getEntry(0, 1)*getX()+mat.getEntry(1,1)*getY()+mat.getEntry(2,1));
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
	    gfx = new Gfx(page.getMediaBox().getWidth(), page.getMediaBox().getHeight());
	    
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
	    		//Dank getValOp wird in Operator nur dann zurückgemeldet, wenn die Übergebenen Parameter passend sind
	    		switch(getValOp(((PDFOperator) token).getOperation(), flushObjects)){
	    		case BT: //Begin text object
	    			//Standardwerte setzen
	    			//font und text size: keine default values
	    			setTextDefaults();
	    			break;
	    		case cm: //Concatenate matrix to current transformation matrix
	    			 //Es werden 6 COSNumber-Werte übertragen
	    			op_cm((COSNumber) flushObjects.get(0),
	    					(COSNumber) flushObjects.get(1),
	    					(COSNumber) flushObjects.get(2),
	    					(COSNumber) flushObjects.get(3),
	    					(COSNumber) flushObjects.get(4),
	    					(COSNumber) flushObjects.get(5));
	    			break;
	    		case d0: //Set glyph width in Type 3 font
	    			break;
	    		case d1: //Set glyph width and bounding box in Type 3 width
	    			break;
	    		case ET: // End text object
	    			/*nothing*/
	    			break;
	    		case T_STAR: //Move to start of next text line
	    			/*
	    			 * T* =
	    			 * 		0 T_l Td (wobei T_l = leading)
	    			 */
	    			op_Td(COSInteger.ZERO, new COSFloat((float) getLeading()));
	    			break;
	    		case Td: //Move text position
	    			op_Td((COSNumber) flushObjects.get(0), (COSNumber) flushObjects.get(1));
	    			break;
	    		case TD: //Move text position and set leading
	    			/*
	    			 * t_x t_y TD =
	    			 * 		-t_y TL
	    			 * 		t_x t_y Td
	    			 */
	    			op_TL(new COSFloat(((COSNumber) flushObjects.get(0)).floatValue()*(-1)));
	    			op_Td((COSNumber) flushObjects.get(0), (COSNumber) flushObjects.get(1));
	    			break;
	    		case Tj: //Show text
	    			op_Tj((COSString) flushObjects.get(0));
	    			break;
	    		case TJ: //Show text, allowing individual glyph positioning
	    			break;
	    		case Tm: //Set text matrix and text line matrix
	    			op_Tm((COSNumber) flushObjects.get(0),
	    					(COSNumber) flushObjects.get(1),
	    					(COSNumber) flushObjects.get(2),
	    					(COSNumber) flushObjects.get(3),
	    					(COSNumber) flushObjects.get(4),
	    					(COSNumber) flushObjects.get(5));
	    			break;
	    		case _HYPHEN: //Move to next line and show text
	    			break;
	    		case _QMARK: //Set word and character spacing, move to next line, and show text
	    			break;
	    		case Tc: //Set character spacing
	    			op_Tc((COSNumber) flushObjects.get(0));
	    			break;
	    		case Tw: //Set word spacing
	    			op_Tw((COSNumber) flushObjects.get(0));
	    			break;
	    		case Tz: //Set horizontal text scaling
	    			op_Tz((COSNumber) flushObjects.get(0));
	    			break;
	    		case TL: //Set text leading
	    			op_TL((COSNumber) flushObjects.get(0));
	    			break;
	    		case Tf: //Set text font and size
	    			op_Tf(fonts.get((COSString) flushObjects.get(0)), ((COSNumber) flushObjects.get(1)));
	    			break;
	    		case Tr: //Set text rendering mode
	    			op_Tr((COSInteger) flushObjects.get(0));
	    			break;
	    		case Ts: //Set text rise
	    			op_Ts((COSNumber) flushObjects.get(0));
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
	
	  protected String parseUnicodeCMap(CMap map, byte[] input){ //Formt Unicode-Bytes in einen String um
		    String output = "";
		    
		    /* Es wird überprüft, welche Codierungsform vorliegt.
		     * Je nachdem, wird die entsprechende Funktion aufgerufen.
		     */
		    if(map.hasTwoByteMappings()){
		      for(int i = 0; (i+1) < input.length; i = i+2){
		        output = output + map.lookup(input, i, 2);
		      }
		    }else if(map.hasOneByteMappings()){
		      for(int i = 0; i < input.length; i++){
		        output = output + map.lookup(input, i, 1);
		      }
		    }else if(map.hasCIDMappings()){
		      for(int i = 0; i < input.length; i++){
		        output = output + map.lookupCID(input, i, 1);
		      }
		    }
		    return output;
	  }
	
	private void op_Tj(COSString cosString) throws IOException{
		PDFont currFont = getFont(); //die Schriftart, die für den String gültig ist
		byte[] stringBytes = cosString.getBytes();
		String textString;
		double stringWidth;
		Point 	p1, p2, p3, p4; //4 Punkte des Text-Rechtecks. Im Uhrzeigersinn von oben links
		TMatrix translation = new TMatrix();
		
		//Schritt 1: Umformung der bytes zu einem String
		
		if (currFont.getToUnicodeCMap() == null){ //Der Text ist in einer ASCII-Schrift gehalten
			//Jedem Byte im String ist ein Glyph zugeordnet
			textString = cosString.toString();
		}else{
			textString = parseUnicodeCMap(currFont.getToUnicodeCMap(), stringBytes);
		}
		
		//Schritt 2: Ermittlung der Breite des Strings vor Textmatrix
		
		//Ermittle die reguläre Breite des Strings in dem Font
		stringWidth = currFont.getStringWidth(textString);
		
		//Füllt zwischen jedes Zeichenpaar ein CharacterSpacing
		stringWidth += getCharacterSpacing()*(textString.length()-1);
		
		//Handelt es sich um einen simple font oder einen single byte font? Dann word spacing berücksichtigen
		if(currFont.getToUnicodeCMap() == null && !currFont.getToUnicodeCMap().hasTwoByteMappings()){
			for(byte glyph : stringBytes){
				if (glyph == 32){
					stringWidth += getWordSpacing();
				}
			}
		}
		
		//Berücksichtigung der horizontalen Skalierung
		stringWidth *= (getHorizontalScaling()/100);
		
		//Das Rechteck wird nun im unskalierten Text Space ermittelt
		p1 = new Point(0,getTextRise()); //oben Links
		p2 = new Point(stringWidth, -getTextRise()); //oben Rechts
		p3 = new Point(stringWidth, getFontSize()-getTextRise()); //unten Rechts
		p4 = new Point(0, getFontSize()-getTextRise());
		
		//Nun wird das Rechteck durch die Textmatrix skaliert
		p1.transform(getTextMatrix());
		p2.transform(getTextMatrix());
		p3.transform(getTextMatrix());
		p4.transform(getTextMatrix());
		
		//Zuletzt wird das Rechteck durch die Transformationsmatrix in den User Space transformiert
		p1.transform(getTransformationMatrix());
		p2.transform(getTransformationMatrix());
		p3.transform(getTransformationMatrix());
		p4.transform(getTransformationMatrix());
		
		//Füge das Rechteck der Gfx hinzu
		gfx.add(new Rectangle(p1,p2,p3,p4));
		
		//Aktualisiere die Textmatrix, indem um die Breite des Strings nach rechts verschoben wird
		translation.makeTranslationMatrix(stringWidth, 0);
		getTextMatrix().preMultiply(translation);
	}

	private void op_Ts(COSNumber cosNumber) { //Set text rise
		setTextRise(cosNumber.doubleValue());
	}

	private void op_Tr(COSInteger cosInteger) { //Set text rendering mode
		setTextRenderingMode(cosInteger.intValue());
	}

	private void op_Tf(PDFont pdFont, COSNumber cosNumber) { //Set text font and size
		setFont(pdFont);
		setFontSize(cosNumber.doubleValue());
	}

	private void op_TL(COSNumber cosNumber) { //Set text leading
		setLeading(cosNumber.doubleValue());
	}

	private void op_Tz(COSNumber cosNumber) { //set horizontal text scaling
		setHorizontalScaling(cosNumber.doubleValue());
	}

	private void op_Tw(COSNumber cosNumber) { //Set word spacing
		setWordSpacing(cosNumber.doubleValue());
	}

	private void op_Tc(COSNumber cosNumber) { //Set character spacing
		setCharacterSpacing(cosNumber.doubleValue());
	}

	public void op_Td(COSNumber t_x, COSNumber t_y){ //move text position
		//Der erste übertragene Wert ist x, der zweite übertragene Wert ist y
		//Mit diesen Werten wird eine Translationsmatrix erstellt
		TMatrix tempMatrix = new TMatrix();
		tempMatrix.makeTranslationMatrix(t_x.doubleValue(),
				t_y.doubleValue());
		//Die neue text line matrix wird berechnet, indem die Translationsmatrix mit
		//der alten text line matrix multipliziert wird
		setTextLineMatrix(new TMatrix(tempMatrix.multiply(getTextLineMatrix())));
		
		//text line matrix und text matrix werden gleich gesetzt
		setTextMatrix(new TMatrix(getTextLineMatrix().copy()));
	}
	
	public void op_cm(COSNumber a, COSNumber b, COSNumber c, COSNumber d, COSNumber e, COSNumber f){
		getTransformationMatrix().updateMatrix(a.doubleValue(),
				b.doubleValue(),
				c.doubleValue(),
				d.doubleValue(),
				e.doubleValue(),
				f.doubleValue());
	}
	
	public void op_Tm(COSNumber a, COSNumber b, COSNumber c, COSNumber d, COSNumber e, COSNumber f){
		getTextMatrix().updateMatrix(a.doubleValue(),
				b.doubleValue(),
				c.doubleValue(),
				d.doubleValue(),
				e.doubleValue(),
				f.doubleValue());
		//Setze text line matrix und text matrix gleich
		setTextLineMatrix(new TMatrix(getTextMatrix().copy()));
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
		Map<String, ClassList> var = new HashMap<>();
		
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

	class Rectangle{
		Point p1,p2,p3,p4;
		
		Rectangle(Point u1, Point u2, Point u3, Point u4){
			p1 = u1;
			p2 = u2;
			p3 = u3;
			p4 = u4;
		}
		
		void setUpperLeft(Point p){
			p1 = p;
		}
		
		void setUpperRight(Point p){
			p2 = p;
		}
		
		void setLowerRight(Point p){
			p3 = p;
		}
		
		void setLowerLeft(Point p){
			p4 = p;
		}
		
		Point getUpperLeft(){
			return p1;
		}
		
		Point getUpperRight(){
			return p2;
		}
		
		Point getLowerRight(){
			return p3;
		}
		
		Point getLowerLeft(){
			return p4;
		}
	}
	
	class Gfx{
		double height, width;
		ArrayList<Rectangle> rect;
		
		Gfx(double h, double w){
			height = h;
			width = w;
			rect = new ArrayList<>();
		}
		
		void add(Rectangle r){
			rect.add(r);
		}
	}
}
