/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.aksw.okbqa.utils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.aksw.okbqa.okbqaevaluation.Evaluation;

/**
 *
 * @author ngonga
 */
public class PostService {
    /** Calls a service which understands JSON and gets a String back, which is 
     * return
     * @param urlString URL of the service to call
     * @param inputText Input JSON
     * @return String Output of service
     * @throws ParseException
     * @throws IOException
     * @throws org.json.simple.parser.ParseException 
     */
    public static String callPostService(String urlString, String inputText)
            throws ParseException, IOException, org.json.simple.parser.ParseException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setUseCaches(false);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Content-Length", String.valueOf(inputText.length()));

        DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
        wr.writeBytes(inputText);
        wr.flush();

        InputStream inputStream = connection.getInputStream();
        InputStreamReader in = new InputStreamReader(inputStream);
        BufferedReader reader = new BufferedReader(in);

        StringBuilder sb = new StringBuilder();
        while (reader.ready()) {
            sb.append(reader.readLine());
        }

        wr.close();
        reader.close();
        connection.disconnect();

        return sb.toString();
    }
    
    public void test() {
        String s = "{\n"
                + "    \"question\": \"Who did that and wo?\",\n"
                + "    \"slots\": [\n"
                + "        {\n"
                + "            \"s\": \"?x\",\n"
                + "            \"p\": \"verbalization\",\n"
                + "            \"o\": \"Free University of Berlin\"\n"
                + "        },\n"
                + "        {\n"
                + "            \"s\": \"?y\",\n"
                + "            \"p\": \"verbalization\",\n"
                + "            \"o\": \"University of Leipzig\"\n"
                + "        },\n"
                + "        {\n"
                + "            \"s\": \"?y\",\n"
                + "            \"p\": \"is\",\n"
                + "            \"o\": \"rdf:Resource|rdfs:Literal\"\n"
                + "        },\n"
                + "        {\n"
                + "            \"s\": \"?z\",\n"
                + "            \"p\": \"verbalization\",\n"
                + "            \"o\": \"connected\"\n"
                + "        },\n"
                + "        {\n"
                + "            \"s\": \"?z\",\n"
                + "            \"p\": \"is\",\n"
                + "            \"o\": \"rdf:Property|rdfs:Literal\"\n"
                + "        }\n"
                + "    ]\n"
                + "}";
        try {
            System.out.println(PostService.callPostService("http://110.45.246.131:2357/agdistis/disambiguate", s));
            //Map<Integer, Map<String, Set<String>>> map = readBenchmarkfromFile("resources/qald5.json", "en");
            //System.out.println(evaluate("", map));
        } catch (Exception ex) {
            Logger.getLogger(Evaluation.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
