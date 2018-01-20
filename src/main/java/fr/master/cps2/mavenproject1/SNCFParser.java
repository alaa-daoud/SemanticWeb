package fr.master.cps2.mavenproject1;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author daoud
 */
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

import org.apache.jena.util.FileManager;
import org.apache.jena.vocabulary.RDF;

import org.apache.jena.vocabulary.RDFS;

/**
 *
 * @author daoud
 */
public class SNCFParser {

    static Resource station_class;
    static Resource route_class;
    static Resource trip_class;
    static Resource step_class;
    static Resource service_class;
    static Resource calender_class;
    static Resource calender_dates_class;
    static Resource Monday,Tuesday,Wednesday,Thursday,Friday,Saturday,Sunday;
    static Property geoLat;
    static Property geolong;
    static Property next;
    static Property stopSequence;
    static String nameSpace;
    static Property depart_time;
    static Property arrive_time;
    static Property station;
    static Property belongsToRoute;
    static Property belongsToTrip;
    static Property ofService;
    static Property regularDays;
    static Property exceptionDay;
    static Property availableOn;
    static Property startDate;
    static Property endDate;
    static Property exceptionDate;
    static Property exceptionType;
    static String resourcePatern = "/Resources";
    static String stopsPatern = "/stops#";
    static String routesPatern = "/routes#";
    static String tripsPatern = "/trips#";
    static String stepsPatern = "/steps#";
    static String servicesPatern = "/services#";
    static String calendersPatern = "/calenders#";
    static String calenderDatesPatern = "/calender_dates#";
    static String proprtiesPatern = "/properties/";
    static String routsFile = "C:\\Users\\daoud\\OneDrive\\Documents\\export-ter-gtfs-last\\routes.txt";
    static String stopsFile = "C:\\Users\\daoud\\OneDrive\\Documents\\export-ter-gtfs-last\\stops.txt";
    static String tripsFile = "C:\\Users\\daoud\\OneDrive\\Documents\\export-ter-gtfs-last\\trips.txt";
    static String stoptimesFile = "C:\\Users\\daoud\\OneDrive\\Documents\\export-ter-gtfs-last\\stop_times.txt";
    static String calenderFile = "C:\\Users\\daoud\\OneDrive\\Documents\\export-ter-gtfs-last\\calendar.txt";
    static String calenderDatesFile = "C:\\Users\\daoud\\OneDrive\\Documents\\export-ter-gtfs-last\\calendar_dates.txt";
    static String W3Time;
    public static final String _LANG_FR = "fr"; // The language of the station names
    public static final String _UTF_8 = "UTF-8"; // Character encoding

    public static void main(String[] args) {

        // create 
        //createRDF();
        // read
        //readRDF("http://dbpedia.org/resource/Human/");
        //read from csv file
        
        Model graph = init(args);
        handleStops(stopsFile, graph);
        handleRoutes(routsFile, graph);
        handleTrips(tripsFile, graph);
        hanldeTimes(stoptimesFile, graph);
        handleCalender(graph, calenderFile);
        
        handleCalenderExceptions(graph, calenderDatesFile);
        try {
            graph.write(new FileOutputStream("outputRDF.ttl"), "Turtle");
        } catch (FileNotFoundException ex) {
            Logger.getLogger(SNCFParser.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }

    public static Model init(String[] args) {
        Model graph = ModelFactory.createDefaultModel();
        String geo = "http://www.w3.org/2003/01/geo/wgs84_pos#";
        nameSpace = "http://www.example.com/GTFS";
        W3Time="https://www.w3.org/TR/owl-time/#time:";
        graph.setNsPrefix("geo", geo);
        graph.setNsPrefix("ex", nameSpace);
        station_class = graph.createResource(nameSpace + "#station");
        route_class = graph.createResource(nameSpace + "#route");
        step_class = graph.createResource(nameSpace + "#trip_step");
        trip_class = graph.createResource(nameSpace + "#trip");
        service_class = graph.createResource(nameSpace + "#service");
        calender_class = graph.createResource(nameSpace + "#calender");
        calender_dates_class = graph.createResource(nameSpace + "#calender_date");
        geoLat = graph.createProperty(geo, "lat");
        geolong = graph.createProperty(geo, "long");
        next = graph.createProperty(nameSpace + proprtiesPatern, "next");
        stopSequence = graph.createProperty(nameSpace + proprtiesPatern, "trip_stop_sequence");
        depart_time = graph.createProperty(nameSpace + proprtiesPatern, "depart_time");
        arrive_time = graph.createProperty(nameSpace + proprtiesPatern, "arrive_time");
        station = graph.createProperty(nameSpace + proprtiesPatern, "station");
        belongsToRoute = graph.createProperty(nameSpace + proprtiesPatern, "belongs_to_route");
        belongsToTrip = graph.createProperty(nameSpace + proprtiesPatern, "belongs_to_trip");
        regularDays = graph.createProperty(nameSpace + proprtiesPatern, "regular_days");
        exceptionDay = graph.createProperty(nameSpace + proprtiesPatern, "exception_day");
        availableOn= graph.createProperty(nameSpace + proprtiesPatern, "available_on");
        startDate= graph.createProperty(nameSpace + proprtiesPatern, "start_date");
        endDate= graph.createProperty(nameSpace + proprtiesPatern, "end_date");
        exceptionDate= graph.createProperty(nameSpace + proprtiesPatern, "exception_date");
        exceptionType= graph.createProperty(nameSpace + proprtiesPatern, "exception_type");
        Monday=graph.createResource(W3Time+"Monday");
        Tuesday=graph.createResource(W3Time+"Tuesday");
        Wednesday=graph.createResource(W3Time+"Wednesday");
        Thursday=graph.createResource(W3Time+"Thursday");
        Friday=graph.createResource(W3Time+"Friday");
        Saturday=graph.createResource(W3Time+"Saturday");
        Sunday=graph.createResource(W3Time+"Sunday");
        ofService=graph.createProperty(nameSpace + proprtiesPatern, "of_service");
        return graph;
    }

    public static void handleStops(String file, Model graph) {
        String csvFile = file;
        BufferedReader br = null;
        String line = "";
        String cvsSplitBy = ",";
        try {

            br = new BufferedReader(new FileReader(csvFile));

            //get property names
            String props = br.readLine();
            String[] csvprops = props.split(cvsSplitBy);
            Property[] prs = new Property[csvprops.length];
            for (int i = 0; i < csvprops.length; i++) {
                prs[i] = graph.createProperty(nameSpace + proprtiesPatern, csvprops[i]);

            }
            int ln = 1;
            //read csv data
            while ((line = br.readLine()) != null) {
                ln++;
                // use comma as separator
                String[] csvLine = line.split(cvsSplitBy);

                Resource current = graph.createResource(nameSpace + resourcePatern + stopsPatern + handleSpaces(csvLine[0]));
                current.addProperty(RDF.type, station_class);

                for (int i = 1; i < csvLine.length; i++) {
                    if (csvLine[i].length() != 0 && i < csvprops.length) {
                        if (csvprops[i].equalsIgnoreCase("stop_name")) {
                            current.addProperty(RDFS.label, csvLine[i], _LANG_FR);
                        } else if (csvprops[i].equalsIgnoreCase("stop_lat")) {
                            current.addProperty(geoLat, csvLine[i], XSDDatatype.XSDdecimal);
                        } else if (csvprops[i].equalsIgnoreCase("stop_lon")) {
                            current.addProperty(geolong, csvLine[i], XSDDatatype.XSDdecimal);
                        } else if (csvprops[i].equalsIgnoreCase("parent_station")) {
                            current.addProperty(prs[i], graph.getResource(nameSpace + resourcePatern + stopsPatern + csvLine[i]));
                        } else if (csvprops[i].contains("date")) {
                            current.addProperty(prs[i], handleDate(csvLine[i]), XSDDatatype.XSDdate);
                        } else {

                            current.addProperty(prs[i], csvLine[i]);
                        }
                    }

                }

            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

    }

    public static void handleRoutes(String file, Model graph) {
        String csvFile = file;
        BufferedReader br = null;
        String line = "";
        String cvsSplitBy = ",";
        try {
            br = new BufferedReader(new FileReader(csvFile));
            //get property names
            String props = br.readLine();
            String[] csvprops = props.split(cvsSplitBy);
            Property[] prs = new Property[csvprops.length];

            //read csv data
            while ((line = br.readLine()) != null) {
                // use comma as separator
                String[] csvLine = line.split(cvsSplitBy);
                Resource current = graph.createResource(nameSpace + resourcePatern + routesPatern + handleSpaces(csvLine[0]));
                current.addProperty(RDF.type, route_class);
                for (int i = 0; i < csvLine.length; i++) {
                    if (csvLine[i].length() != 0) {

                        if (csvprops[i].equalsIgnoreCase("route_long_name")) {
                            current.addProperty(RDFS.label, csvLine[i], _LANG_FR);
                        }
                    }
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void handleTrips(String file, Model graph) {
        String csvFile = file;
        BufferedReader br = null;
        String line = "";
        String cvsSplitBy = ",";
        try {
            br = new BufferedReader(new FileReader(csvFile));
            //get property names
            String props = br.readLine();
            String[] csvprops = props.split(cvsSplitBy);
            Property[] prs = new Property[csvprops.length];

            //read csv data
            while ((line = br.readLine()) != null) {
                // use comma as separator
                String[] csvLine = line.split(cvsSplitBy);
                Resource current = graph.createResource(nameSpace + resourcePatern + tripsPatern + handleSpaces(csvLine[2]));
                current.addProperty(RDF.type, trip_class);
                for (int i = 0; i < csvLine.length; i++) {
                    if (csvLine[i].length() != 0) {
                        if (csvprops[i].equalsIgnoreCase("route_id")) {
                            current.addProperty(belongsToRoute, graph.getResource(nameSpace + resourcePatern + routesPatern + handleSpaces(csvLine[i])));
                        }
                        if (csvprops[i].equalsIgnoreCase("trip_headsign")) {
                            current.addProperty(RDFS.label, csvLine[i], _LANG_FR);
                        }
                        if (csvprops[i].equalsIgnoreCase("service_id")) {
                            
                            Resource serv = graph.getResource(nameSpace + resourcePatern + servicesPatern + csvLine[i]);
                            if(!graph.containsResource(serv));
                                serv.addProperty(RDF.type, service_class);
                            current.addProperty(ofService, serv);
                        }

                    }
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static Model hanldeTimes(String file, Model graph) {
        String csvFile = file;
        BufferedReader br = null;
        String line = "";
        String cvsSplitBy = ",";
        int trip_cullomn = 0;
        int seq_cullomn = 4;
        try {
            br = new BufferedReader(new FileReader(csvFile));
            //get property names
            String props = br.readLine();
            String[] csvprops = props.split(cvsSplitBy);
            Property[] prs = new Property[csvprops.length];
            for (int i = 0; i < csvprops.length; i++) {
                prs[i] = graph.createProperty(nameSpace + proprtiesPatern, csvprops[i]);
                if (csvprops[i].equalsIgnoreCase("trip_id")) {
                    trip_cullomn = i;
                }
                if (csvprops[i].equalsIgnoreCase("stop_sequence")) {
                    seq_cullomn = i;
                }
            }
            //read csv data

            while ((line = br.readLine()) != null) {
// trip_id,arrival_time,departure_time,stop_id,stop_sequence,stop_headsign,pickup_type,drop_off_type,shape_dist_traveled
                // use comma as separator
                String[] csvLine = line.split(cvsSplitBy);
                int seq = Integer.parseInt(csvLine[seq_cullomn]);
                handleSequence(graph, csvLine[trip_cullomn], seq, csvLine, csvprops);

            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return graph;
        }

    }

    public static Resource handleSequence(Model graph, String trip, int sequence, String[] csvLine, String[] csvprops) {
        Resource res = graph.getResource(nameSpace + resourcePatern + stepsPatern + handleSpaces(trip + "_seq_" + sequence));
        Resource tr = graph.getResource(nameSpace + resourcePatern + tripsPatern + trip);
        if (!graph.containsResource(res)) {
            res = graph.createResource(nameSpace + resourcePatern + stepsPatern + handleSpaces(trip + "_seq_" + sequence));
            if (sequence > 0) {
                Resource currResource = res;
                Resource c = handleSequence(graph, trip, sequence - 1, new String[0], new String[0]);
                c.addProperty(next, currResource);
                currResource = c;
            }
        }
        res.addProperty(RDF.type, step_class);
        res.addProperty(belongsToTrip, tr);
        for (int i = 1; i < csvLine.length; i++) {
            if (csvLine[i].length() != 0) {
                if (csvprops[i].equalsIgnoreCase("arrival_time")) {
                    res.addProperty(arrive_time, csvLine[i], XSDDatatype.XSDtime);
                }
                if (csvprops[i].equalsIgnoreCase("departure_time")) {
                    res.addProperty(depart_time, csvLine[i], XSDDatatype.XSDtime);
                }
                if (csvprops[i].equalsIgnoreCase("stop_id")) {
                    res.addProperty(station, graph.getResource(nameSpace + resourcePatern + stopsPatern + handleSpaces(csvLine[i])));
                }
            }
        }

        tr.addProperty(stopSequence, graph.getResource(nameSpace + resourcePatern + stepsPatern + handleSpaces(trip + "_seq_" + 0)));

        return res;
    }

    public static void handleCalender(Model graph, String calenderFile) {
        String csvFile = calenderFile;
        BufferedReader br = null;
        String line = "";
        String cvsSplitBy = ",";
        
        try {
            br = new BufferedReader(new FileReader(csvFile));
            //get property names
            String props = br.readLine();
            String[] csvprops = props.split(cvsSplitBy);
            Property[] prs = new Property[csvprops.length];

            //read csv data
            while ((line = br.readLine()) != null) {
                
//service_id,monday,tuesday,wednesday,thursday,friday,saturday,sunday,start_date,end_date
                // use comma as separator
                String[] csvLine = line.split(cvsSplitBy);
                Resource current = graph.createResource(nameSpace + resourcePatern + calendersPatern + handleSpaces(csvLine[0]));
                current.addProperty(RDF.type, calender_class);
                current.addProperty(RDFS.label, "ca_"+csvLine[0]);
                for (int i = 0; i < csvprops.length; i++) {
                    prs[i] = graph.createProperty(nameSpace + proprtiesPatern, csvprops[i]);
                    if (csvprops[i].equalsIgnoreCase("service_id")) {
                        Resource curService=graph.getResource(nameSpace + resourcePatern + servicesPatern + handleSpaces(csvLine[i]));
                        if(!graph.containsResource(curService));
                                curService.addProperty(RDF.type, service_class);
                        curService.addProperty(regularDays, current);
                    }
                    else if(csvprops[i].equalsIgnoreCase("start_date")){
                       current.addProperty(startDate, handleDate(csvLine[i]),XSDDatatype.XSDdate);
                    }
                    else if(csvprops[i].equalsIgnoreCase("end_date")){
                       current.addProperty(endDate, handleDate(csvLine[i]),XSDDatatype.XSDdate);
                    }
                    else {
                        if(csvLine[i].contains("1"))
                        {
                            String Day=csvprops[i].substring(1);
                            Day=(csvprops[i].charAt(0)+"").toUpperCase()+Day;
                            current.addProperty(availableOn, graph.getResource(W3Time+Day));
                        }
                    }
                }
                

            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
    }
public static void handleCalenderExceptions(Model graph, String calenderdatesFile) {
        String csvFile = calenderdatesFile;
        BufferedReader br = null;
        String line = "";
        String cvsSplitBy = ",";
        
        try {
            br = new BufferedReader(new FileReader(csvFile));
            //get property names
            String props = br.readLine();
            String[] csvprops = props.split(cvsSplitBy);
            //Property[] prs = new Property[csvprops.length];

            //read csv data
            while ((line = br.readLine()) != null) {
                
//service_id,date,exception_type
                // use comma as separator
                String[] csvLine = line.split(cvsSplitBy);
                Resource current = graph.createResource(nameSpace + resourcePatern + calenderDatesPatern + handleSpaces(csvLine[0]+"_"+csvLine[1]));
                current.addProperty(RDF.type, calender_dates_class);
                current.addProperty(RDFS.label, "ex_"+csvLine[0]);
                for (int i = 0; i < csvprops.length; i++) {
                    //prs[i] = graph.createProperty(nameSpace + proprtiesPatern, csvprops[i]);
                    if (csvprops[i].equalsIgnoreCase("service_id")) {
                        Resource curService=graph.getResource(nameSpace + resourcePatern + servicesPatern + handleSpaces(csvLine[i]));
                        if(!graph.containsResource(curService));
                                curService.addProperty(RDF.type, service_class);
                        curService.addProperty(exceptionDay, current);
                    }
                    
                    else if(csvprops[i].equalsIgnoreCase("date")){
                       
                        String d=handleDate(csvLine[i]);
                        current.addProperty(exceptionDate, d,XSDDatatype.XSDdate);
                    }
                   
                    else {
                        if(csvLine[i].contains("1"))
                        {
                            current.addLiteral(exceptionType, "Add");
                        }
                        else
                            current.addLiteral(exceptionType, "Remove");
                    }
                }
                

            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
    }
    public static Model readRDF(String inputFileName) {
        // create an empty model
        Model graphM = ModelFactory.createDefaultModel();

        // use the FileManager to find the input file
        InputStream in = FileManager.get().open(inputFileName);
        if (in == null) {
            throw new IllegalArgumentException(
                    "File: " + inputFileName + " not found");
        }

        // read the RDF/XML file
        graphM.read(in, null, "TTL");

        // write it to standard out
        graphM.write(System.out, "Turtle");
        return graphM;
    }

    static String handleSpaces(String in) {
        String r = in.replaceAll(" ", "_");
        try {
            r = URLEncoder.encode(r, _UTF_8);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(SNCFParser.class.getName()).log(Level.SEVERE, null, ex);
        }
        return r;

    }

    static String handleDate(String date) {
        return date.substring(0, 4) + "-" + date.substring(4, 6) + "-" + date.substring(6, 8);
    }

    /*                     else if(true){
                            current.addLiteral(geoLat, ResourceFactory.createTypedLiteral("2012-03-11", XSDDatatype.XSDdate));
                        }   
     */
}

