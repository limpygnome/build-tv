package com.limpygnome.buildtv.daemon.dashboard.dashboard;

import org.apache.commons.lang3.text.StrSubstitutor;
import org.json.simple.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * The JIRA implementation of a dashboards provider, which can build public and private URLs for a Jira dashboards.
 *
 * This will use the Atlassian Wallboard plugin to display a dashboards.
 *
 * JSON parameter configuration:
 * - user: the Jira user able to access the dashboards
 * - pass: the pass for the Jira user
 * - url: the base URL of the Jira instance
 * - dashboards: the ID of the dashboards
 */
public class JiraDashboardProvider extends DashboardProvider
{
    private static final String JIRA_URL = "${url}/login.jsp?os_username=${user}&os_password=${pass}&os_destination=plugins/servlet/Wallboard/?dashboardId=${dashboard}";
    private static final String JIRA_PUBLIC_URL = "${url}/plugins/servlet/Wallboard/?dashboardId=${dashboard}";

    private String url;
    private String publicUrl;

    @Override
    public void loadParams(JSONObject root)
    {
        super.loadParams(root);

        // Prepare to build URLs
        Map<String, String> values = new HashMap<>();
        values.put("user", (String) root.get("user"));
        values.put("pass", (String) root.get("pass"));
        values.put("url", (String) root.get("url"));
        values.put("dashboard", (String) root.get("dashboard"));

        // Build URLs
        StrSubstitutor substitutor = new StrSubstitutor(values);

        url = substitutor.replace(JIRA_URL);
        publicUrl = substitutor.replace(JIRA_PUBLIC_URL);
    }

    @Override
    public String fetchUrl()
    {
        return url;
    }

    @Override
    public String fetchPublicUrl()
    {
        return publicUrl;
    }
}
