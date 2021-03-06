/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jppf.application.template;

import java.util.List;
import org.jppf.client.*;
import org.jppf.node.protocol.Task;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * This is a template JPPF application runner.
 * It is fully commented and is designed to be used as a starting point
 * to write an application using JPPF.
 * @author Laurent Cohen
 */
public class TemplateApplicationRunner {
  /**
   * The JPPF client, handles all communications with the server.
   * It is recommended to only use one JPPF client per JVM, so it
   * should generally be created and used as a singleton.
   */
  private static JPPFClient jppfClient =  null;
  private static TemplateApplicationRunner instance = null;
  private Map<String, JPPFJob> resultsMap = null;
  
  public static synchronized TemplateApplicationRunner getInstance()
  {
		if(instance == null)
		{
			instance = new TemplateApplicationRunner();
		}  
		
		return instance;
  }

  private TemplateApplicationRunner()
  {
    jppfClient = new JPPFClient();
    resultsMap = new HashMap<String, JPPFJob>();
  }

  /**
   * Create a JPPF job that can be submitted for execution.
   * @return an instance of the {@link org.jppf.client.JPPFJob JPPFJob} class.
   * @throws Exception if an error occurs while creating the job or adding tasks.
   */
  public synchronized JPPFJob createJob() throws Exception 
  {
    // create a JPPF job
    JPPFJob job = new JPPFJob();

    // give this job a readable unique id that we can use to monitor and manage it.
    job.setName("Test Job");

    // add a task to the job.
    job.add(new TemplateJPPFTask());

    // add more tasks here ...
    
    return job;
  }
  
    /**
   * Create a test JPPF job that can be submitted for execution.
   * @return an instance of the {@link org.jppf.client.JPPFJob JPPFJob} class.
   * @throws Exception if an error occurs while creating the job or adding tasks.
   */
  public synchronized JPPFJob createTestJob(String message, int waitTime, int numTasks) throws Exception 
  {
    // create a JPPF job
    JPPFJob job = new JPPFJob();

    // give this job a readable unique id that we can use to monitor and manage it.
    job.setName("Test Job");


    for(int i = 0; i < numTasks; i++)
    {
      job.add(new TemplateJPPFTask(message, waitTime));
    }    
    
    return job;
  }
  
   /**
   * Create a test JPPF job that can be submitted for execution.
   * @return an instance of the {@link org.jppf.client.JPPFJob JPPFJob} class.
   * @throws Exception if an error occurs while creating the job or adding tasks.
   */
  public synchronized JPPFJob createRandomizedTestJob(String message, int maxWaitTime, int numTasks) throws Exception 
  {
    // create a JPPF job
    JPPFJob job = new JPPFJob();

    // give this job a readable unique id that we can use to monitor and manage it.
    job.setName("Randomized Test Job");


    for(int i = 0; i < numTasks; i++)
    {
      job.add(new RandomizedJPPFTask(message, maxWaitTime));
    }    
    
    return job;
  }
  
  /**
  * Attempts to cancel the job with the specified ID.  
  *
  * @Returns true if the job was cancelled, false if not.
  *
  */
  public synchronized boolean cancelJob(String jobID)
  {
    boolean wasSuccessful = false;
    
    try
    {
      wasSuccessful = jppfClient.cancelJob(jobID);
    }
    catch(Exception e)
    {
      return false;    
    }
	
	 return wasSuccessful;
  }

  /**
   * Execute a job in blocking mode. The application will be blocked until the job
   * execution is complete.
   * @param job the JPPF job to execute.
   * @throws Exception if an error occurs while executing the job.
   */
  public synchronized void executeBlockingJob(final JPPFJob job) throws Exception {
    // set the job in blocking mode.
    job.setBlocking(true);

    // Submit the job and wait until the results are returned.
    // The results are returned as a list of JPPFTask instances,
    // in the same order as the one in which the tasks were initially added the job.
    List<Task<?>> results = jppfClient.submitJob(job);

    // process the results
    processExecutionResults(results);
  }

  /**
   * Execute a job in non-blocking mode. The application has the responsibility
   * for handling the notification of job completion and collecting the results.
   * @param job the JPPF job to execute.
   * @throws Exception if an error occurs while executing the job.
   */
  public synchronized void executeNonBlockingJob(final JPPFJob job) throws Exception {
    // set the job in non-blocking (or asynchronous) mode.
    job.setBlocking(false);

    // this call returns immediately. We will use the collector at a later time
    // to obtain the execution results asynchronously
    JPPFResultCollector collector = submitNonBlockingJob(job);

    // We should store the result collector so that website users can query it for job info.
    // When the job is finished (fail or succeed) the website should delete the listener and store
    // any archival info to a database so we don't run out of RAM.

    resultsMap.put(job.getUuid(), job);
  }

  /**
   * Execute a job in non-blocking mode. The application has the responsibility
   * for handling the notification of job completion and collecting the results.
   * @param job the JPPF job to execute.
   * @return a JPPFResultCollector used to obtain the execution results at a later time.
   * @throws Exception if an error occurs while executing the job.
   */
  public synchronized JPPFResultCollector submitNonBlockingJob(final JPPFJob job) throws Exception {
    // set the job in non-blocking (or asynchronous) mode.
    job.setBlocking(false);

    // We need to be notified of when the job execution has completed.
    // To this effect, we define an instance of the TaskResultListener interface,
    // which we will register with the job.
    // Here, we use an instance of JPPFResultCollector, conveniently provided by the JPPF API.
    // JPPFResultCollector implements TaskResultListener and has a constructor that takes
    // the number of tasks in the job as a parameter.
    JPPFResultCollector collector = new JPPFResultCollector(job);
    job.setResultListener(collector);

    // Submit the job. This call returns immediately without waiting for the execution of
    // the job to complete. As a consequence, the object returned for a non-blocking job is
    // always null. Note that we are calling the exact same method as in the blocking case.
    jppfClient.submitJob(job);

    // finally return the result collector, so it can be used to collect the exeuction results
    // at a time of our chosing. The collector can also be obtained at any time by calling 
    // (JPPFResultCollector) job.getResultListener()
    return collector;
  }

  /**
   * Process the execution results of each submitted task. 
   * @param results the tasks results after execution on the grid.
   */
  public synchronized void processExecutionResults(final List<Task<?>> results) 
  {
    // process the results
    for (Task<?> task: results) {
      // if the task execution resulted in an exception
      if (task.getThrowable() != null) {
        // process the exception here ...
      }
      else {
        // process the result here ...
      }
    }
  }
  
  /*
  * This isn't thread-safe and should be deleted
  */
  public synchronized JPPFResultCollector getResultsForJob(String jobID)
  {
		return (JPPFResultCollector) resultsMap.get(jobID).getResultListener();
  }
  
  /*
	* @Returns the number of tasks in the specified job
	*/
	public synchronized int getTotalTasks(String jobID)
	{
		return resultsMap.get(jobID).getJobTasks().size();
	}

	/*
	* @Returns the number of tasks still pending for the specified job
	*/
	public synchronized int getNumCompleteTasks(String jobID)
	{
		return resultsMap.get(jobID).getResults().size();
	}

  /*
  * Returns a list of the statuses of all tasks in the specified job.
  * Statuses can be: "EXECUTING", "FAILED", or "COMPLETE".
  *
  * Please note that these statuses are different than those returned by getJobStatus()
  * in that they do not correspond to any JPPF enum value and are determined
  * by seeing whether each task threw an exception or has results yet.
  *
  * @Returns a list of the statuses of all tasks in the specified job.
  */
  public synchronized String[] getTaskStatuses(String jobID)
  {    
    int numTasks = resultsMap.get(jobID).getJobTasks().size();
    JobResults results = resultsMap.get(jobID).getResults();
    String[] statuses = new String[numTasks];

    for(int i = 0; i < numTasks; i++)
    {
      if(!results.hasResult(i))
      {
        statuses[i] = "EXECUTING";
        continue;
      }

      Task t = results.getResultTask(i);

      if(t.getThrowable() != null)
      {
        statuses[i] = "FAILED";
        continue;
      }

      if((String)t.getResult() != null)
      {
        statuses[i] = "COMPLETE";
        continue;
      }

      // this should never happen
      statuses[i] = "EXECUTING";
    }

    return (String[])statuses;
  }
}
