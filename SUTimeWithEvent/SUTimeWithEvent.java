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
import java.util.regex.Matcher;
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

    private List<String> matchesListForFromTo = new ArrayList<String>();
    private List<String> matchesListForBeforeAfter = new ArrayList<String>();
	
	public SplitEvent(String fileName){
		this.fileName = fileName;
		
		props = new Properties();
		pipeline = new AnnotationPipeline();
		
		pipeline.addAnnotator(new TokenizerAnnotator(true));
		pipeline.addAnnotator(new TimeAnnotator("sutime", props));
		
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
		Date date = new Date();
		today = dateFormat.format(date).toString();


        matchesListForBeforeAfter.add("(.*)(before\\s([0-9])(am))");
        matchesListForBeforeAfter.add("(.*)(before\\s([0-9])(pm))");
        matchesListForBeforeAfter.add("(.*)(before\\s([0-9])([0-9])(am))");
        matchesListForBeforeAfter.add("(.*)(before\\s([0-9])([0-9])(pm))");

        matchesListForBeforeAfter.add("(.*)(after\\s([0-9])(am))");
        matchesListForBeforeAfter.add("(.*)(after\\s([0-9])(pm))");
        matchesListForBeforeAfter.add("(.*)(after\\s([0-9])([0-9])(am))");
        matchesListForBeforeAfter.add("(.*)(after\\s([0-9])([0-9])(pm))");

        matchesListForFromTo.add("(.*)from(.*)to(.*)on(.*)");
        matchesListForFromTo.add("(.*)from(.*)to(.*)");
	}
	
	public void process(){
		FileDecision fileDecision = new FileDecision(fileName);
		
		for (String text : FileDecision.listSentences) {
			text = text.toLowerCase();
            int count = 0;

			for(String ele : matchesListForFromTo){
                if(text.matches(ele)){
                    count++;
                    editTextWithFromTo(text);
                    break;
                }
            }
            for(String ele : matchesListForBeforeAfter){
                if(text.matches(ele + "((.*))")){
                    count++;
                    editTextWithBeforeAfter(text);
                    break;
                }
            }
			if(count == 0){
				Annotation annotation = new Annotation(text);
				annotation.set(CoreAnnotations.DocDateAnnotation.class, this.today);
				pipeline.annotate(annotation);// Run the pipeline on an input annotation

				List<CoreMap> timexAnnsAll = annotation.get(TimeAnnotations.TimexAnnotations.class);
				for (CoreMap cm : timexAnnsAll) {
					String timeExpress = cm.get(TimeExpression.Annotation.class).getTemporal().getTimexValue();
					if(timeExpress.contains("W")){
						// For case has week only
						processWithWeek(timeExpress);
						continue;
					}
					else{
						// For case doesn't have week
						processWithOutWeek(timeExpress,-1);
					}
				}
			}
		}
	}
	
	public void editTextWithFromTo(String content){
		Pattern pattern = Pattern.compile("from");
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
									if(index == element.length()){
										break;
									}
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
					if(i < timeExpress.length() - 1){
						endDate = timeExpress.substring(i + 1, timeExpress.length() - 1);
					}
					else{
						endDate = timeExpress.substring(i, timeExpress.length() - 1);
					}
					break;
				}
			}
			if(startDate.length() > 0){
				if(!startDate.contains("W") && !endDate.contains("W")){
					if(endDate.length() != 0){
						processWithOutWeek(startDate, endDate);
					}
					else{
						processWithOutWeek(startDate, startDate);
					}
				}
				else{
					if(endDate.length() != 0){
						processWithWeek(startDate, endDate);
					}
					else{
						processWithWeek(startDate, startDate);
					}
				}
			}
		}
	}

    public void makeEventWithAfterBefore(String text){

            Annotation annotation = new Annotation(text);
            annotation.set(CoreAnnotations.DocDateAnnotation.class, this.today);
            pipeline.annotate(annotation);// Run the pipeline on an input annotation

            List<CoreMap> timexAnnsAll = annotation.get(TimeAnnotations.TimexAnnotations.class);
            for(CoreMap cm : timexAnnsAll){
                String timeExpress = cm.get(TimeExpression.Annotation.class).getTemporal().getTimexValue();
                int indexOfHourPath = timeExpress.indexOf("T");
                String hourPath = timeExpress.substring(indexOfHourPath + 1, indexOfHourPath + 3);
                if(text.contains("before")){
                    if(Integer.parseInt(hourPath) <= 7){
                        makeEventWithSimpleDateFormat("yyyy-MM-dd'T'hh:mm", timeExpress, timeExpress);
                    }
                    else{
                        StringBuilder strB = new StringBuilder(timeExpress);
                        strB.replace(indexOfHourPath + 1, indexOfHourPath + 6, "07:00");
                        String newExpress = strB.toString();
                        makeEventWithSimpleDateFormat("yyyy-MM-dd'T'hh:mm", newExpress, timeExpress);
                    }
                }
                else{
                    if(Integer.parseInt(hourPath) >= 23){
                        makeEventWithSimpleDateFormat("yyyy-MM-dd'T'hh:mm", timeExpress, timeExpress);
                    }
                    else{
                        StringBuilder strB = new StringBuilder(timeExpress);
                        strB.replace(indexOfHourPath + 1, indexOfHourPath + 6, "23:00");
                        String newExpress = strB.toString();
                        makeEventWithSimpleDateFormat("yyyy-MM-dd'T'hh:mm", timeExpress, newExpress);
                    }
                }
            }
    }

    public void editTextWithBeforeAfter(String text){
        List<String> outputList = new ArrayList<String>();

        if(text.contains("before")){
            String outputListAfterSplitByBefore[] = text.split("before");
            for(String str : outputListAfterSplitByBefore){
                str = "before" + str;
                if(str.contains("after")){
                    String outputListAfterSplitByAfter[] = str.split("after");
                    for(String element : outputListAfterSplitByAfter){
                        if(!element.contains("before")){
                            element = "after" + element;
                        }
                        outputList.add(element);
                    }
                }
                else{
                    outputList.add(str);
                }
            }
        }
        else if(text.contains("after")){
            String outputListAfterSplitByBefore[] = text.split("after");
            for(String str : outputListAfterSplitByBefore){
                str = "after" + str;
                if(str.contains("before")){
                    String outputListAfterSplitByAfter[] = str.split("before");
                    for(String element : outputListAfterSplitByAfter){
                        if(!element.contains("after")){
                            element = "before" + element;
                            outputList.add(element);
                        }
                    }
                }
                else{
                    outputList.add(str);
                }
            }
        }

        for(String str : outputList){
            makeEventWithAfterBefore(str);
        }
    }
	
	public Date makeDateWithWeek(String dateString){
		int startIndexForWeek = dateString.indexOf("W");
		int endIndexForWeek = 0;
		startIndexForWeek++;
		
		int year = 0, week = 0;
		endIndexForWeek = startIndexForWeek + 2;
		String weekNum = dateString.substring(startIndexForWeek, endIndexForWeek);
		week = Integer.parseInt(weekNum);
		week++;
		for (int i = 0; i < dateString.length(); i++) {
			if (dateString.charAt(i) == '-') {
				year = Integer.parseInt(dateString.substring(0, i));
				break;
			}
		}
		Calendar cld = Calendar.getInstance();
		cld.set(Calendar.YEAR, year);
		cld.set(Calendar.WEEK_OF_YEAR, week);

		if(dateString.contains("WE")){
			cld.set(Calendar.DAY_OF_WEEK, 7);
		}
		
		Date date = cld.getTime();
		return date;
	}
	
	public void processWithWeek(String startDate, String endDate){
		Date startDateOfEvent = makeDateWithWeek(startDate);
		Date endDateOfEvent = makeDateWithWeek(endDate);
		makeAndAddEvent(startDateOfEvent, endDateOfEvent);
	}
	
	public void processWithOutWeek(String startDate, String endDate){
		if(startDate.contains("T")){
            makeEventWithSimpleDateFormat("yyyy-MM-dd'T'hh:mm", startDate, endDate);
		}
		else{
            makeEventWithSimpleDateFormat("yyyy-MM-dd", startDate, endDate);
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
			
			if(!timeExpress.contains("WE")){
				for(int i = 1 ; i <= 7 ; i++){
                    Date startDate = makeCustomCalendar(year, week, i, 00, 00, 07, Calendar.MONDAY).getTime();
                    Date endDate = makeCustomCalendar(year, week, i, 00, 00, 23, Calendar.MONDAY).getTime();
                    makeAndAddEvent(startDate, endDate);
                }
			}
			else{
				for(int i = 1 ; i <= 7 ; i++){
					if(i == 1 || i == 7){
						Date startDate = makeCustomCalendar(year, week, i, 00, 00, 07, Calendar.MONDAY).getTime();
						Date endDate = makeCustomCalendar(year, week, i, 00, 00, 23, Calendar.MONDAY).getTime();
				        makeAndAddEvent(startDate, endDate);
					}
				}
			}
		}
	}

    public Calendar makeCustomCalendar(int year, int week, int day_of_week, int minute, int second, int hour, int first_day_of_week){
        Calendar cld = Calendar.getInstance();

        cld.set(Calendar.YEAR, year);
        cld.set(Calendar.WEEK_OF_YEAR, week);

        cld.setFirstDayOfWeek(first_day_of_week);
        cld.set(Calendar.DAY_OF_WEEK,day_of_week);
        cld.set(Calendar.MINUTE, minute);
        cld.set(Calendar.SECOND, second);

        cld.set(Calendar.HOUR_OF_DAY, hour);

        return cld;
    }

    public void makeEventWithSimpleDateFormat(String format, String startDate, String endDate){
        Date startDateOfEvent = makeDateWithSimpleDateFormat(format, startDate);
        Date endDateOfEvent = makeDateWithSimpleDateFormat(format, endDate);
        makeAndAddEvent(startDateOfEvent, endDateOfEvent);
    }

    public Date makeDateWithSimpleDateFormat(String format, String timeExpress){
        try {
            return new SimpleDateFormat(format).parse(timeExpress);
        }
        catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void makeAndAddEvent(Date startDate, Date endDate){
        Event event = new Event(startDate, endDate);
        listEvents.add(event);
    }
	
	public void processWithOutWeek(String timeExpress,int flagTimeMode){
		if(!timeExpress.contains("T")){
            makeEventWithSimpleDateFormat("yyyy-MM-dd hh:mm", timeExpress + " 07:00", timeExpress + " 23:00");
        }
		else{
			if(!timeExpress.contains("MO") && !timeExpress.contains("AF") && !timeExpress.contains("EV")){
                int index = timeExpress.indexOf("T");
                String newExpress = timeExpress.substring(0, index + 1);
                if(flagTimeMode == -1){
                    newExpress += "07:00";
                    makeEventWithSimpleDateFormat("yyyy-MM-dd'T'hh:mm", newExpress, timeExpress);
                }
                else{
                    newExpress += "23:00";
                    makeEventWithSimpleDateFormat("yyyy-MM-dd'T'hh:mm", timeExpress, newExpress);
                }
			}
			else{
				if(timeExpress.contains("MO")){
                    makeEventWithSimpleDateFormat("yyyy-MM-dd'TMO'hh:mm", timeExpress + "07:00", timeExpress + "11:00");
				}
				else if(timeExpress.contains("AF")){
                    makeEventWithSimpleDateFormat("yyyy-MM-dd'TAF'hh:mm", timeExpress + "12:00", timeExpress + "18:00");
				}
				else if(timeExpress.contains("EV")){
                    makeEventWithSimpleDateFormat("yyyy-MM-dd'TEV'hh:mm", timeExpress + "19:00", timeExpress + "23:00");
				}
			}
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
