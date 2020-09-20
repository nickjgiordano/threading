import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Worker implements Runnable {

    private final int id;  // Unique worker ID.
    
    private final JobStack jobStack;  // Reference to the job stack.
    private final ResourceStack resourceStack;  // Reference to the resource stack.
    
    private Job job;  // Job being processed.
    private Resource[] resources;  // Resources being used for job being processed.
    
    private boolean busy;  // Indicates the status of the worker. True when they are working (executing jobs) and false when there are no more jobs left to execute.
    
    private final Map<Integer, ArrayList<Integer>> jobsCompleted;  // The job record of the worker. Stores each job's ID and the IDs of the resources used for each job.
    // Constructor.
    public Worker(int theId, JobStack theJobStack, ResourceStack theResourceStack) {
        id = theId;
        jobStack = theJobStack;
        resourceStack = theResourceStack;
        job = null;
        busy = true;
        jobsCompleted = new TreeMap<>();
    }

    private void log(String msg) {
        System.out.println("Worker "+id+": "+msg); 
    }
    
    private void report(int jobID) throws MalformedURLException,IOException {
        String url = "http://www.scm.keele.ac.uk/staff/stan/app_notify.php?job=" + job.getId() + "&worker=" + id;
        
        URL objUrl = new URL(url);
        HttpURLConnection conUrl = (HttpURLConnection) objUrl.openConnection();
        conUrl.setRequestMethod("GET");
        
        try ( BufferedReader brInput = new BufferedReader( new InputStreamReader( conUrl.getInputStream() ) ) ) {
            String sLine;
            StringBuffer sbResponse = new StringBuffer();
            while( ( sLine = brInput.readLine() ) != null ) {
                sbResponse.append(sLine);
                System.err.println(sbResponse);
            }
        }
    }
    
    @Override
    public void run() {
        log("started");
        busy = false;
        while(jobStack.getSize() != 0 && busy == false) {
            busy = true;
            job = jobStack.pop();
            resources = resourceStack.pop( job.getResourceRequirement() );
            
            
            try {Thread.sleep( job.getTimeToComplete() );}
            catch(InterruptedException e) {System.out.println( e.toString() );}
            ArrayList<Integer> list = new ArrayList<>();
            
            
            
            for (Resource resource : resources) {list.add( resource.getId() );}
            log("completed job " + job.getId() + ".");
            jobsCompleted.put(job.getId(), list);
            resourceStack.push(resources);
            
            
            
            try {report( job.getId() );}
            catch (IOException ex) {Logger.getLogger(Worker.class.getName()).log(Level.SEVERE, null, ex);}
            busy = false;
        }
    }
    
    public void printJobs() {
        String sResources = "";
        for( Map.Entry< Integer, ArrayList<Integer> > entry : jobsCompleted.entrySet() ) {
            sResources = sResources + "\n     Job " + entry.getKey() + ", Resources: " + entry.getValue();
        } log(sResources);
    }
}