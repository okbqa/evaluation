package org.aksw.okbqa.okbqaevaluation;

import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.aksw.okbqa.utils.PostService;

/**
 * Evaluation for OKBQA. Assumes - input = "{ "string" = "someString",
 * "language" = "someLanguage" } als JSONObject - output is a set of strings als
 * JSONArray
 */
public class Evaluation {

    public static void main(String args[]) {
        String language = "en";
        String service = null;
        String benchmarkFile = "resources/qald5.json";
        if (args.length == 0) {
            System.out.println("Config error. Give name of service als argument");
            System.exit(1);
        }
        if (args.length > 0) {
            service = args[0]; 
        }
        if (args.length > 1) {
            language = args[1]; 
        }
        if (args.length > 2) {
            benchmarkFile = args[2]; 
        }
        Map<Integer, Map<String, Set<String>>> benchmark = readBenchmarkFromFile(benchmarkFile,
                language);
        System.out.println("Benchmark contains "+benchmark.keySet().size()+" questions.");
        Map<Integer, Map<String, Double>> result = evaluate(service, benchmark, language);
        System.out.println("\n"+toCSV(result));
    }

    public static String toCSV(Map<Integer, Map<String, Double>> result) {
        String header = "id,precision,recall,f-measure,true positives,false negatives,false positives";
        String line="";
        for(Integer id:result.keySet())
        {
            line = line + id;            
            line = line +","+result.get(id).get("precision");
            line = line +","+result.get(id).get("recall");
            line = line +","+result.get(id).get("f-measure");
            line = line +","+result.get(id).get("truePositives");
            line = line +","+result.get(id).get("falseNegatives");
            line = line +","+result.get(id).get("falsePositives");
            line = line + "\n";
        }
        return line;
    }

    /**
     * Reads a benchmark from a JSON string
     *
     * @param data String from which the benchmark object is to be read
     * @param lang Language supported by the framework
     * @return Benchmark object that maps id to query string in the supported
     * language and the set of expected answers
     */
    public static Map<Integer, Map<String, Set<String>>> readBenchmark(String data, String lang) {
        Map<Integer, Map<String, Set<String>>> benchmark = new HashMap<>();
        JSONParser parser = new JSONParser();
        try {
            JSONObject input = (JSONObject) parser.parse(data);
            JSONArray questions = (JSONArray) input.get("questions");
            String q;
            Set<String> uris;
            for (int i = 0; i < questions.size(); i++) {
                JSONObject question = (JSONObject) questions.get(i);
                Integer index = Integer.parseInt(question.get("id").toString());
                JSONArray body = ((JSONArray) question.get("body"));
                q = null;
                uris = null;
                for (int j = 0; j < body.size(); j++) {
                    if (((JSONObject) body.get(j)).get("language").equals("en")) {
                        q = ((JSONObject) body.get(j)).get("string").toString();
                        break;
                    }
                }
                if (q != null) {
                    JSONArray answers = ((JSONArray) question.get("answers"));
                    uris = new HashSet<>();
                    for (int j = 0; j < answers.size(); j++) {
                        uris.add(((JSONObject) answers.get(j)).get("string").toString());
                    }
                }
                Map<String, Set<String>> qMap = new HashMap<>();
                qMap.put(q, uris);
                benchmark.put(index, qMap);
            }
            System.out.println("Read benchmark with " + benchmark.keySet().size() + " questions.");
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
        return benchmark;
    }

    /**
     * Reads a benchmark from a given file
     *
     * @param file File from which the benchmark is to be read
     * @param lang Language supported by the QA framework
     * @return Benchmark object
     */
    public static Map<Integer, Map<String, Set<String>>> readBenchmarkFromFile(String file, String lang) {
        String data = "";
        try {
            BufferedReader in = new BufferedReader(new FileReader(new File(file)));
            String s = in.readLine();
            while (s != null) {
                data = data + s + "\n";
                s = in.readLine();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return readBenchmark(data, lang);
    }

    /**
     * Gets results for a query from a given endpoint
     *
     * @param restInterface Endpoint name
     * @param question String to ask
     * @return Set of strings as answer
     */
    public static Set<String> getResults(String restInterface, String question, String language) {
        Set<String> s = new HashSet<>();

        //get results
        try {
            JSONArray answer = (JSONArray)(new JSONParser()).parse(PostService.callPostService(restInterface,
                    "{\"string\":\"" + question + "\", \"language\":\"" + language + "\"/\"en\"}"));
            for (int i = 0; i < answer.size(); i++) {
                s.add(answer.get(i).toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return s;
    }

    public static Map<Integer, Map<String, Double>> evaluate(String restInterface, Map<Integer, Map<String, Set<String>>> benchmark, String language) {
        //true positive, true negative, false positive
        double tp, fn, fp;
        Map<Integer, Map<String, Double>> result = new HashMap<>();
        for (Integer id : benchmark.keySet()) {
            tp = 0;
            fn = 0;
            fp = 0;
            String question = benchmark.get(id).keySet().iterator().next();
            Set<String> referenceAnswers = benchmark.get(id).get(question);
            Set<String> systemAnswers = getResults(restInterface, question, language);
            for (String answer : systemAnswers) {
                if (referenceAnswers.contains(answer)) {
                    //true positives
                    tp++;
                } else {
                    //false positives
                    fp++;
                }
            }
            //false negatives
            fn = referenceAnswers.size() - tp;
            Map<String, Double> evalMap = new HashMap<>();
            if (tp == 0) {
                evalMap.put("precision", 0d);
                evalMap.put("recall", 0d);
                evalMap.put("f-measure", 0d);
            } else {
                evalMap.put("precision", tp / (tp + fp));
                evalMap.put("recall", tp / (tp + fn));
                evalMap.put("f-measure", 2 * evalMap.get("precision") * evalMap.get("recall") / (evalMap.get("precision") + evalMap.get("recall")));
            }
            evalMap.put("truePositives", tp);
            evalMap.put("falsePositives", fp);
            evalMap.put("falseNegatives", fn);
            result.put(id, evalMap);
        }
        return result;
    }

    
}
