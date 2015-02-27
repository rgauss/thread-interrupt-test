package com.example.interrupt;

import java.util.Date;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Simple test showing thread interrupt behavior
 * 
 * @author rgauss
 */
public class ThreadInterruptTest {
    
    private static final long WORKER_PROCESS_TIME_MS = 1000;
    
    @Test
    public void testWithoutInterrupt()
    {
        testThreadBehavior("withoutInterrupt", false);
    }
    
    @Test
    public void testWithInterrupt()
    {
        testThreadBehavior("withInterrupt", true);
    }
    
    protected void testThreadBehavior(String id, boolean interruptWorker)
    {
        System.out.println("main thread started");
        
        SomeApp someApp = new SomeApp(id, interruptWorker);
        Thread parentThread = someApp.startParentProcess();
        
        // Stop the parent thread halfway through expected worker process time
        try {
            Thread.sleep(WORKER_PROCESS_TIME_MS / 2);
        } catch (InterruptedException e) {
            System.out.println("main thread interrupted");
        }
        parentThread.interrupt();
        
        // Wait to check worker process time
        try {
            Thread.sleep(WORKER_PROCESS_TIME_MS * 2);
        } catch (InterruptedException e) {
            System.out.println("main thread interrupted");
        }
        
        long time = someApp.getTimeTakenForWorkerToStop();
        if (interruptWorker)
        {
            assertTrue("someApp[" + id + "] workerThread should have been stopped",
                    time != 0 && time < WORKER_PROCESS_TIME_MS);
        }
        else
        {
            assertTrue("someApp[" + id + "] workerThread should have been allowed to finish",
                    time != 0 && time >= WORKER_PROCESS_TIME_MS);
        }
    }

    public class SomeApp
    {
        private String id;
        private boolean interruptWorker;
        private long start;
        private long timeTakenForWorkerToStop;
        
        public SomeApp(String id, boolean interruptWorker)
        {
            this.id = id;
            this.interruptWorker = interruptWorker;
            start = new Date().getTime();
        }
        
        public long getTimeTakenForWorkerToStop() {
            return timeTakenForWorkerToStop;
        }

        public Thread startParentProcess()
        {
            Thread parentProcess = new Thread()
            {
                public void run() {
                    Thread workerThread = startWorkerProcess();
                    try {
                        workerThread.join();
                    } catch (InterruptedException e) {
                        long timeInterrupted = new Date().getTime() - start;
                        System.out.println("someApp[" + id + "] parentThread interrupted at " + timeInterrupted + "ms");
                        if (interruptWorker)
                        {
                            workerThread.interrupt();
                        }
                    }
                }
            };
            parentProcess.start();
            System.out.println("someApp[" + id + "] parentThread started");
            return parentProcess;
        }
        
        public Thread startWorkerProcess()
        {
            Thread workerProcess = new Thread()
            {
                public void run() {
                    try {
                        Thread.sleep(WORKER_PROCESS_TIME_MS);
                        long timeTaken = new Date().getTime() - start;
                        System.out.println("someApp[" + id + "] workerThread "
                                + "finished work at " + timeTaken + "ms <-- "
                                        + "The parent did not interrupt the worker");
                    } catch (InterruptedException e) {
                        // OK, we'll stop
                        long timeInterrupted = new Date().getTime() - start;
                        System.out.println("someApp[" + id + "] workerThread "
                                + "interrupted at " + timeInterrupted + "ms <-- "
                                        + "The parent interrupted the worker");
                    }
                    timeTakenForWorkerToStop = new Date().getTime() - start;
                }
            };
            workerProcess.start();
            System.out.println("someApp[" + id + "] workerThread started");
            return workerProcess;
        }
    }
}
