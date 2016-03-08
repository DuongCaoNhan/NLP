import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.regex.Pattern;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.time.*;
import edu.stanford.nlp.util.CoreMap;

class Event{
	Date start;
	Date end;
	
	public Event(Date start, Date end){
		this.start = start;
		this.end = end;
	}
	
	public void printEvent(){
		System.out.println("Start Time: " + start + " -- " + "End Time: " + end );
	}
}

class FileDecision {

	// @param args is path to file

	String fileName = new String();
	static List<String> listSentences = new ArrayList<String>();

	public FileDecision(String content) {
		initializes(content);
		readFile();
	}

	public void initializes(String fileName) {
		Date date = new Date();
		this.fileName = fileName;
	}

	public void readFile() {
		try {
			BufferedReader inReader = new BufferedReader(new FileReader(fileName));
			int c;
			while ((c = inReader.read()) != -1) {
				StringBuilder sentence = new StringBuilder();
				while (true) {
					char character = (char) c;
					if (character == '!' || character == '?' || character == '.' || c == -1) {
						break;
					}
					if (character == '\n') {
						character = ' ';
					}
					sentence.append(character);
					c = inReader.read();
				}
				listSentences.add(sentence.toString());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

class SplitEvent{
	private Properties props;
	private AnnotationPipeline pipeline;
	// build pipeline bang cach add cac annotator -> de xu li cac annotation
	// roi dua object muon annotate vao -> lay ve object da annotated

	private String fileName;
	private List<Event> listEvents = new ArrayList<Event>();
	private String today = new String();
	
	public SplitEvent(String fileName){
		this.fileName = fileName;
		
		props = new Properties();
		pipeline = new AnnotationPipeline();
		
		pipeline.addAnnotator(new TokenizerAnnotator(true));
		pipeline.addAnnotator(new TimeAnnotator("sutime", props));
		
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
		Date date = new Date();
		today = dateFormat.format(date).toString();
	}
	
	public void process(){
		FileDecision fileDecision = new FileDecision(fileName);
		
		for (String text : FileDecision.listSentences) {
			text = text.toLowerCase();
			if(text.matches("(.*)from(.*)to(.*)on(.*)") || text.matches("from(.*)to(.*)on(.*)") || text.matches("from(.*)to(.*)") || text.matches("(.*)from(.*)to(.*)")){
				editTextWithFromTo(text);
			}
			else{
				Annotation annotation = new Annotation(text);
				annotation.set(CoreAnnotations.DocDateAnnotation.class, this.today);
				pipeline.annotate(annotation);// Run the pipeline on an input annotation
					
				System.out.println(annotation.get(CoreAnnotations.TextAnnotation.class));
				List<CoreMap> timexAnnsAll = annotation.get(TimeAnnotations.TimexAnnotations.class);
				for (CoreMap cm : timexAnnsAll) {
					String timeExpress = cm.get(TimeExpression.Annotation.class).getTemporal().getTimexValue();
					System.out.println(timeExpress);
					if(timeExpress.contains("W")){
						// For case has week only
						processWithWeek(timeExpress);
						continue;
					}
					else{
						// For case doesn't have week
						processWithOutWeek(timeExpress);
					}
				}
			}
		}
	}
	
	public void editTextWithFromTo(String content){
		Pattern pattern = Pattern.compile("from");
		List<String> resultList = new ArrayList<String>();
		String[] splitList = pattern.split(content);
		
		for(String element : splitList){
			if(element.charAt(0) == ' '){
				element = new StringBuilder(element).deleteCharAt(0).toString();
				if(element.contains("to")){
					if(element.contains("on")){
						int indexOfOn = element.indexOf("on");
						String dayAfterOn = new String();
						for(int i = indexOfOn ; i < element.length() ; i++){
							if(element.charAt(i) == ' '){
								int index = i + 1;
								while((element.charAt(index) >= 'A' && element.charAt(index) <= 'Z') || (element.charAt(index) >= 'a' && element.charAt(index) <= 'z')){
									index++;
								}
								dayAfterOn = element.substring(i + 1,index);
								break;
							}
						}
						int insertPosition = 0;
						for(int i = 0 ; i < element.length() ; i++){
							if(element.charAt(i) == ' '){
								insertPosition = i;
								break;
							}
						}
						if(insertPosition != 0){
							StringBuilder strB = new StringBuilder(element);
							strB.insert(insertPosition, " on " + dayAfterOn);
							element = strB.toString();
						}
					}
				}
			}
			Annotation annotation = new Annotation(element);
			annotation.set(CoreAnnotations.DocDateAnnotation.class, this.today);
			pipeline.annotate(annotation);// Run the pipeline on an input annotation
				
			List<CoreMap> timexAnnsAll = annotation.get(TimeAnnotations.TimexAnnotations.class);
			String timeExpress = new String();
			String startDate = new String();
			String endDate = new String();
			
			for (CoreMap cm : timexAnnsAll) {
				String subTimeExpress = cm.get(TimeExpression.Annotation.class).getTemporal().getTimexValue();
				timeExpress += subTimeExpress + " ";
			}
			for(int i = 0 ; i < timeExpress.length() ; i++){
				if(timeExpress.charAt(i) == ' '){
					startDate = timeExpress.substring(0, i);
					endDate = timeExpress.substring(i + 1, timeExpress.length() - 1);
					break;
				}
			}
			if(startDate.length() > 0 && endDate.length() > 0){
				processWithOutWeek(startDate, endDate);
			}
		}
	}
	
	public void processWithWeek(String startDate, String endDate){
		
	}
	
	public void processWithOutWeek(String startDate, String endDate){
		if(startDate.contains("T")){
			try {
				Date startDateOfEvent = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm").parse(startDate);
				Date endDateOfEvent = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm").parse(endDate);	
				Event event = new Event(startDateOfEvent, endDateOfEvent);
				
				listEvents.add(event);
			} 
			catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else{
			try {
				Date startDateOfEvent = new SimpleDateFormat("yyyy-MM-dd").parse(startDate);
				Date endDateOfEvent = new SimpleDateFormat("yyyy-MM-dd").parse(endDate);	
				Event event = new Event(startDateOfEvent, endDateOfEvent);
				listEvents.add(event);
			} 
			catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	}
	
	public void processWithWeek(String timeExpress){
		if(!timeExpress.contains("WXX")){
			int startIndexForWeek = timeExpress.indexOf("W");
			int endIndexForWeek = 0;
			startIndexForWeek++;
			int year = 0, week = 0;
			endIndexForWeek = startIndexForWeek + 2;
			String weekNum = timeExpress.substring(startIndexForWeek, endIndexForWeek);
			week = Integer.parseInt(weekNum);
			week++;
			for (int i = 0; i < timeExpress.length(); i++) {
				if (timeExpress.charAt(i) == '-') {
					year = Integer.parseInt(timeExpress.substring(0, i));
					break;
				}
			}
			Calendar cld = Calendar.getInstance();
			cld.set(Calendar.YEAR, year);
			cld.set(Calendar.WEEK_OF_YEAR, week);
			cld.setFirstDayOfWeek(Calendar.MONDAY);
			
			if(!timeExpress.contains("WE")){
				cld.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
				Date startDate = cld.getTime();
				cld.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
				Date endDate = cld.getTime();
			
				Event event = new Event(startDate, endDate);
				listEvents.add(event);
			}
			else{
				cld.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
				Date startDate = cld.getTime();
				cld.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
				Date endDate = cld.getTime();
			
				Event event = new Event(startDate, endDate);
				listEvents.add(event);
			}
		}
	}
	
	public void processWithOutWeek(String timeExpress){
		if(!timeExpress.contains("T")){
			try {
				Date startDate = new SimpleDateFormat("yyyy-MM-dd hh:mm").parse(timeExpress + " 07:00");
				Date endDate = new SimpleDateFormat("yyyy-MM-dd hh:mm").parse(timeExpress + " 23:00");	
				Event event = new Event(startDate, endDate);
				listEvents.add(event);
			} 
			catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else{
			
		}
	}
	
	public String getFileName(){
		return this.fileName;
	}
	
	public String getToday(){
		return this.today;
	}
	
	public void printListEvent(){
		for(Event e : listEvents){
			e.printEvent();
		}
	}
}

public class SUTimeWithEvent{
    public static void main( String[] args ){
    	if(args.length == 0){
    		System.err.println("Pass <file_name> in commandline argument");
    		return ;
    	}
    	SplitEvent splitEvent = new SplitEvent(args[0]);
    	splitEvent.process();
    	splitEvent.printListEvent();
    }
}
