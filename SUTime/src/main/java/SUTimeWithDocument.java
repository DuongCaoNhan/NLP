import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.time.*;
import edu.stanford.nlp.util.CoreMap;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Properties;

import com.sun.org.apache.xerces.internal.impl.xpath.regex.ParseException;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

class FileDecision {

	// @param args is path to file

	String fileName = new String();
	static String today = new String();
	static List<String> listSentences = new ArrayList<String>();

	public FileDecision(String content) {
		initializes(content);
		readFile();
	}

	public void initializes(String fileName) {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
		Date date = new Date();
		this.fileName = fileName;
		today = dateFormat.format(date).toString();
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

public class SUTimeWithDocument {

	static List<Date> listDates = new ArrayList<Date>();

	public static void main(String[] args) {
		Properties props = new Properties();
		AnnotationPipeline pipeline = new AnnotationPipeline();
		// build pipeline bang cach add cac annotator -> de xu li cac annotation
		// roi dua object muon annotate vao -> lay ve object da annotated

		pipeline.addAnnotator(new TokenizerAnnotator(true));
		pipeline.addAnnotator(new TimeAnnotator("sutime", props));

		FileDecision decision = new FileDecision(args[0]);

		for (String text : FileDecision.listSentences) {
			Annotation annotation = new Annotation(text);
			if (text.toLowerCase().contains("tomorrow") | text.toLowerCase().contains("yesterday"))
				annotation.set(CoreAnnotations.DocDateAnnotation.class, FileDecision.today);
			else {
				Calendar calendar = Calendar.getInstance();
				calendar.set(Calendar.DAY_OF_WEEK, 5);
				DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
				annotation.set(CoreAnnotations.DocDateAnnotation.class, dateFormat.format(calendar.getTime()));
			}

			pipeline.annotate(annotation);// Run the pipeline on an input
											// annotation.
			System.out.println(annotation.get(CoreAnnotations.TextAnnotation.class));
			List<CoreMap> timexAnnsAll = annotation.get(TimeAnnotations.TimexAnnotations.class);
			for (CoreMap cm : timexAnnsAll) {
				String timeExpress = cm.get(TimeExpression.Annotation.class).getTemporal().getTimexValue();
				System.out.println(timeExpress);
				try {
					listDates.add(convertTimexToDate(timeExpress));
				} catch (java.text.ParseException e) {
					e.printStackTrace();
				}
			}
		}

		for (Date d : listDates) {
			System.out.println(d);
		}
	}

	public static void setFormatAndMakeDate(String timeExpress) throws java.text.ParseException {
		String timePart = new String();
		String datePart = new String();

		if (timeExpress.contains("T")) {
			int index = timeExpress.indexOf("T");
			timePart = timeExpress.substring(index + 1);
		}
		if (!timeExpress.contains("WXX") && timeExpress.contains("W")) {
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
			if (timePart.length() != 0) {
				cld.set(Calendar.HOUR, Integer.parseInt(timePart.substring(0, 2)));
				cld.set(Calendar.MINUTE, Integer.parseInt(timePart.substring(3, 5)));
			}
			Date date = cld.getTime();
			listDates.add(date);
		} else {
			if (!timeExpress.contains("XXXX")) {
				int countCrossSymbol = 0;
				for (int i = 0; i < timeExpress.length(); i++) {
					if (timeExpress.charAt(i) == '-') {
						if (countCrossSymbol < 2) {
							countCrossSymbol++;
						}
					}
					if (timeExpress.charAt(i) != '-' && (timeExpress.charAt(i) < '0' || timeExpress.charAt(i) > '9')) {
						datePart = timeExpress.substring(0, i - 1);
						break;
					}
					if (countCrossSymbol == 2) {
						datePart = timeExpress.substring(0, i + 3);
						break;
					}
				}
				DateFormat format;
				String dateOutput = datePart;
				if (timePart.length() != 0) {
					dateOutput += " " + timePart;
					format = new SimpleDateFormat("yyyy-MM-dd hh:mm");
					Date date = format.parse(dateOutput);
					listDates.add(date);
				} else {
					format = new SimpleDateFormat("yyyy-MM-dd");
					Date date = format.parse(dateOutput);
					listDates.add(date);
				}
			}
		}
	}

	public static Date convertTimexToDate(String timexValue) throws java.text.ParseException {
		SimpleDateFormat simpleDateFormat1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
		SimpleDateFormat simpleDateFormat2 = new SimpleDateFormat("yyyy-MM-dd");
		Date date = null;
		try {
			if (timexValue.contains("T"))
				date = simpleDateFormat1.parse(timexValue);
			else
				date = simpleDateFormat2.parse(timexValue);
		} catch (ParseException e) {

			e.printStackTrace();
		}
		return date;
	}
}