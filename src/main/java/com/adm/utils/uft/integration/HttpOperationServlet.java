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

package com.adm.utils.uft.integration;


import org.apache.commons.io.IOUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.Boolean;
import java.lang.Exception;
import java.lang.String;

/**
 * Created with IntelliJ IDEA.
 * User: yanghanx
 * Date: 4/22/16
 * Time: 9:18 AM
 */
public class HttpOperationServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }


    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        //mobile center
        String serverUrl = req.getParameter("mcServerURLInput");
        String userName = req.getParameter("mcUserNameInput");
        String password = req.getParameter("mcPasswordInput");

        //proxy
        String useProxy = req.getParameter("useProxy");
        String useAuthentication = req.getParameter("useAuthentication");
        String proxyAddress = req.getParameter("proxyAddress");
        String proxyUserName = req.getParameter("proxyUserName");
        String proxyPassword = req.getParameter("proxyPassword");

        boolean isProxy = Boolean.valueOf(useProxy);
        boolean isAuthentication = Boolean.valueOf(useAuthentication);

        String msg = "";
        JobOperation operation = null;

        if (CommonUtils.doCheck(serverUrl, userName, password)) {

            try {
                //1:no proxy 2:config proxy, but proxy info is null 3:config proxy, no auth 4: config proxy config auth, but auth null   5:cinfig proxy,auth
                if (isProxy) {
                    if (proxyAddress != null && proxyAddress != "") {

                        if (isAuthentication) {

                            if (proxyUserName != null && proxyUserName != "" && proxyPassword != null && proxyPassword != "") {
                                operation = new JobOperation(serverUrl, userName, password, proxyAddress, proxyUserName, proxyPassword);//5
                                msg = send(operation,req);
                            } else {
                                msg = "{\"myErrorCode\":\"4\"}"; //4
                            }

                        } else {
                            operation = new JobOperation(serverUrl, userName, password, proxyAddress, null, null);//3
                            msg = send(operation,req);
                        }

                    } else {
                        msg = "{\"myErrorCode\":\"2\"}";   //2
                    }

                } else {
                    operation = new JobOperation(serverUrl, userName, password, null, null, null);//1
                    msg = send(operation,req);
                }


            } catch (Exception e) {

            }

        } else {
            msg = "{\"myErrorCode\":\"0\"}";//0  userName password or url null
        }


        writeJSON(resp, msg);

    }

    private String send(JobOperation operation,HttpServletRequest req) throws Exception{
        String method = req.getParameter("method");
        String msg = "";
        if ("createTempJob".equals(method)) {
            msg = operation.createTempJob();
        }else if("getJobJSONData".equals(method)){
            String jobUUID = req.getParameter("jobUUID");
            msg = operation.getJobJSONData(jobUUID);
        }
        return msg;
    }


    private void writeJSON(HttpServletResponse resp, String info) throws ServletException, IOException {
        PrintWriter writer = null;
        try {

            writer = resp.getWriter();

            writer.write(info);

            writer.flush();
        } finally {
            IOUtils.closeQuietly(writer);
        }
    }


}
