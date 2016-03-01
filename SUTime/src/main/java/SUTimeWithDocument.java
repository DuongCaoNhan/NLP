import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.time.*;
import edu.stanford.nlp.util.CoreMap;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.ArrayList;
import java.util.Properties;

import com.sun.org.apache.xerces.internal.impl.xpath.regex.ParseException;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

class SUTimeTool{

	//@param args is path to file

	String fileName = new String();
	static String standardTime = new String();
	static List<String> listSentences = new ArrayList<String>();

	public SUTimeTool(String content){
		initializes(content);
		readFile();
	}

	public void initializes(String fileName){
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
		Date date = new Date();
		this.fileName = fileName;	
		standardTime = dateFormat.format(date).toString();
	}

	public void readFile(){
		try{
			BufferedReader inReader = new BufferedReader(new FileReader(fileName));
			int c;
			while((c = inReader.read()) != -1) {
				StringBuilder sentence = new StringBuilder();
				while(true){
					char character = (char) c;
					if(character == '!' || character == '?' || character == '.' || c == -1){
						break;
					}
					if(character == '\n'){
						character = ' ';
					}
					sentence.append(character);
					c = inReader.read();
				}
				listSentences.add(sentence.toString());
			}
		}
		catch(IOException e){
			e.printStackTrace();
		}
	}
}

public class SUTimeWithDocument {

	static List<Date> listDates = new ArrayList<Date>();

	public static void main(String[] args) {
		Properties props = new Properties();
		AnnotationPipeline pipeline = new AnnotationPipeline();
		// build pipeline bang cach add cac annotator -> de xu li cac annotation
		// roi dua object muon annotate vao -> lay ve object da annotated
		
		pipeline.addAnnotator(new TokenizerAnnotator(true));
		pipeline.addAnnotator(new TimeAnnotator("sutime", props));
		
		SUTimeTool tool = new SUTimeTool(args[0]);

		for(String text : SUTimeTool.listSentences){
			Annotation annotation = new Annotation(text);
			annotation.set(CoreAnnotations.DocDateAnnotation.class, SUTimeTool.standardTime);
			pipeline.annotate(annotation);// Run the pipeline on an input annotation.
			System.out.println(annotation.get(CoreAnnotations.TextAnnotation.class));
			List<CoreMap> timexAnnsAll = annotation.get(TimeAnnotations.TimexAnnotations.class);
			for(CoreMap cm : timexAnnsAll){
				String pattern = cm.get(TimeExpression.Annotation.class).getTemporal().toString();
				try {
					setFormatAndMakeDate(pattern);
				} 
				catch (java.text.ParseException e) {
					e.printStackTrace();
				}
			}
		}

		for(Date d : listDates){
			System.out.println(d);
		}
	}
	
	public static void setFormatAndMakeDate(String pattern) throws java.text.ParseException{
		if(pattern.contains("T")){
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm", Locale.ENGLISH);
			Date date = format.parse(pattern.replace('T', ' '));
			listDates.add(date);
		}
		else{
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
			Date date = format.parse(pattern);
			listDates.add(date);
		}
	}
}