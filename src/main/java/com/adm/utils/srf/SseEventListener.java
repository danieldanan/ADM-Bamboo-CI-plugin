/*
 *
 *
 * Certain versions of software and/or documents ("Material") accessible here may contain branding from
 * Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.  As of September 1, 2017,
 * the Material is now offered by Micro Focus, a separately owned and operated company.  Any reference to the HP
 * and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE
 * marks are the property of their respective owners.
 * __________________________________________________________________
 *
 * MIT License
 *
 * (c) Copyright 2012-2018 Micro Focus or one of its affiliates.
 *
 * The only warranties for products and services of Micro Focus and its affiliates
 * and licensors ("Micro Focus") are set forth in the express warranty statements
 * accompanying such products and services. Nothing herein should be construed as
 * constituting an additional warranty. Micro Focus shall not be liable for technical
 * or editorial errors or omissions contained herein.
 * The information contained herein is subject to change without notice.
 */

package com.adm.utils.srf;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.launchdarkly.eventsource.EventHandler;
import com.launchdarkly.eventsource.MessageEvent;
import net.sf.json.JSONObject;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

public class SseEventListener extends Observable implements EventHandler {

    private BuildLogger buildLogger;
    private final String delim;
    private ArrayList<Observer> observers;

    public SseEventListener(BuildLogger buildLogger) {
        this.buildLogger = buildLogger;
        this.delim = "#########################################################################\r\n";
        this.observers = new ArrayList<>();
    }

    public synchronized void addObserver(Observer o) {
        this.observers.add(o);
    }

    public synchronized void deleteObserver(Observer o) {
        this.observers.remove(o);
    }

    @Override
    public void notifyObservers(Object arg) {
        for (Observer observer : this.observers) {
            observer.update(this, arg);
        }
    }

    @Override
    public void onOpen() {
    }

    @Override
    public void onClosed() {
    }

    @Override
    public void onMessage(String eventName, MessageEvent messageEvent) throws Exception {
        String data = messageEvent.getData();
        if(data == null || data.isEmpty())
            return;

        try {
            JSONObject obj = JSONObject.fromObject(data);

            switch (eventName) {
                case "test-run-count":
                    return;
                case "test-run-started":
                    this.testRunStartedHandler(obj, eventName);
                    break;
                case "script-run-started":
                    this.scriptRunStartedHandler();
                    break;
                case "script-step-created":
                    this.scriptStepCreatedHandler(obj, eventName);
                    break;
                case "script-run-ended":
                    this.scriptRunEndedHandler();
                    break;
                case "test-run-ended":
                    this.testRunEndedHandler(obj, eventName);
                    break;

                default:
                    return;
            }

            buildLogger.addBuildLogEntry(delim);
            buildLogger.addBuildLogEntry(obj.toString(2));
            buildLogger.addBuildLogEntry("\r\n");
        }
        catch (Exception e){
            buildLogger.addErrorLogEntry(e.getMessage());
        }
    }

    @Override
    public void onComment(String s) {
    }

    @Override
    public void onError(Throwable throwable) {
        buildLogger.addErrorLogEntry("SseEventListener got error:" + throwable.getMessage());
        throwable.printStackTrace();
    }

    private void testRunStartedHandler(JSONObject obj, String eventName) {
        buildLogger.addBuildLogEntry(delim);
        obj.discard("runningCount");
        JSONObject o1 = JSONObject.fromObject(obj.get("testRun"));
        String str = String.format("%1s %2s Status: %3s\r\n",
                o1.get("name"),
                eventName,
                o1.get("status"));
        this.notifyObservers(new SrfSseEventNotification(SrfSseEventNotification.SrfTestRunEvents.TEST_RUN_START, o1.getString("id"), null));
        buildLogger.addBuildLogEntry(str);
    }

    private void testRunEndedHandler(JSONObject obj, String eventName) {
        buildLogger.addBuildLogEntry(delim);
        obj.discard("runningCount");
        JSONObject o1 = JSONObject.fromObject(obj.get("testRun"));
        String testRunId = o1.getString("id");
        o1.discard("id");
        o1.discard("tags");
        o1.discard("user");
        o1.discard("additionalData");
        obj.discard("testRun");

        JSONObject o2 = JSONObject.fromObject(o1.get("test"));
        o1.discard("test");
        obj.put("testRun", o1);
        obj.put("environments", o2.get("environments"));
        obj.put("scripts", o2.get("scripts"));

        String str = String.format("%1s %2s Status: %3s\r\n",
                o1.get("name"),
                eventName,
                o1.get("status")
        );
        String testRunStatus = o1.get("status").toString();
        buildLogger.addBuildLogEntry(str);
        this.notifyObservers(new SrfSseEventNotification(SrfSseEventNotification.SrfTestRunEvents.TEST_RUN_END, testRunId, testRunStatus));
    }

    private void scriptStepCreatedHandler(JSONObject obj, String eventName) {
        String status = obj.getString("status");
        if(status.compareTo("running") == 0)
            return;

        buildLogger.addBuildLogEntry(delim);
        String str = String.format("%1s Status: %2s\r\n",
                eventName,
                obj.get("status")
        );
        buildLogger.addBuildLogEntry(str);
        obj.discard("id");
        obj.discard("scriptRun");
        obj.discard("snapshot");
    }

    private void scriptRunStartedHandler() {
        buildLogger.addBuildLogEntry(delim);;
    }

    private void scriptRunEndedHandler() {
        buildLogger.addBuildLogEntry(delim);;
    }
}




