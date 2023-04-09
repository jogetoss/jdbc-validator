package org.joget.marketplace;

import java.io.IOException;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.model.FormValidator;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import java.util.Properties;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.dbcp2.BasicDataSourceFactory;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.service.AppPluginUtil;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.DynamicDataSourceManager;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.SecurityUtil;
import org.joget.workflow.model.service.WorkflowUserManager;
import org.joget.workflow.util.WorkflowUtil;
import org.joget.plugin.base.PluginWebSupport;
import org.json.JSONException;
import org.json.JSONObject;

public class JdbcValidator extends FormValidator implements PluginWebSupport {

    private final static String MESSAGE_PATH = "message/JdbcValidator";

    @Override
    public boolean validate(Element element, FormData data, String[] values) {
        boolean result = true;
        String id = FormUtil.getElementParameterName(element);
        String mandatory = (String) getProperty("mandatory");

        if (isEmptyValues(values)) {
            if ("true".equals(mandatory)) {
                result = false;
                data.addFormError(id, AppPluginUtil.getMessage("org.joget.marketplace.jdbcvalidator.missingValue", getClassName(), MESSAGE_PATH));
            }
        } else {
            Connection con = null;
            DataSource ds = null;
            String message = (String) getProperty("message");
            String query = getPropertyString("query");

            PreparedStatement pstmt = null;
            try {
                ds = createDataSource();
                con = ds.getConnection();
                for (String val : values) {
                    List<String> placeHolders = getPlaceHolders(query);
                    Map<Integer, String> placeHolderMap = new HashMap<>();
                    for (int i = 0; i < placeHolders.size(); i++) {
                        if (placeHolders.get(i).startsWith("{") && placeHolders.get(i).endsWith("}")) {
                            String value = getFieldValue(placeHolders.get(i), data);
                            query = query.replace(placeHolders.get(i), "?");
                            placeHolderMap.put(i, value);
                        } else if (placeHolders.get(i).startsWith("?")) {
                            placeHolderMap.put(i, val);
                        }
                    }
                    pstmt = con.prepareStatement(query);
                    int pstmtIndex = 0;
                    for (Map.Entry<Integer, String> entry : placeHolderMap.entrySet()) {
                        int mapKey = entry.getKey();
                        String mapValue = entry.getValue();
                        pstmt.setObject(pstmtIndex + 1, mapValue);
                        pstmtIndex++;
                    }
                    ResultSet rs = pstmt.executeQuery();
                    if (rs.next()) {
                        result = false;
                        break;
                    }
                }
            } catch (Exception e) {
                LogUtil.error(getClassName(), e, "");
            } finally {
                try {
                    if (pstmt != null) {
                        pstmt.close();
                    }
                    if (con != null) {
                        con.close();
                    }
                    if (ds != null && ds instanceof BasicDataSource) {
                        ((BasicDataSource) ds).close();
                    }
                } catch (Exception e) {
                    LogUtil.error(getClassName(), e, "");
                }
            }

            if (!result) {
                if (message == null || message.isEmpty()) {
                    data.addFormError(id, AppPluginUtil.getMessage("org.joget.marketplace.jdbcvalidator.duplicateValueMessage", getClassName(), MESSAGE_PATH));
                }
                data.addFormError(id, message);
            }

        }
        return result;
    }

    public List<String> getPlaceHolders(String query) {
        List<String> placeholders = new ArrayList<>();
        StringBuilder currentPlaceholder = new StringBuilder();
        for (int i = 0; i < query.length(); i++) {
            char c = query.charAt(i);
            if (c == '?') {
                placeholders.add("?");
            } else if (c == '{') {
                currentPlaceholder = new StringBuilder();
                while (query.charAt(i) != '}') {
                    currentPlaceholder.append(query.charAt(i));
                    i++;
                }
                currentPlaceholder.append(query.charAt(i));
                placeholders.add(currentPlaceholder.toString());
            }
        }
        return placeholders;
    }

    public String getFieldValue(String fieldPlaceholder, FormData data) {
        String fieldId = fieldPlaceholder;
        if (fieldPlaceholder != null && !fieldPlaceholder.isEmpty()) {
            if (fieldPlaceholder.startsWith("{") && fieldPlaceholder.endsWith("}")) {
                fieldId = fieldPlaceholder.substring(1, fieldPlaceholder.length() - 1);
            }
        }
        return data.getRequestParameter(fieldId) != null ? data.getRequestParameter(fieldId) : fieldPlaceholder;
    }

    protected DataSource createDataSource() throws Exception {
        DataSource ds = null;
        String datasource = getPropertyString("jdbcDatasource");
        if ("default".equals(datasource)) {
            // use current datasource
            ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        } else {
            // use custom datasource
            Properties dsProps = new Properties();
            dsProps.put("driverClassName", getPropertyString("jdbcDriver"));
            dsProps.put("url", getPropertyString("jdbcUrl"));
            dsProps.put("username", getPropertyString("jdbcUser"));
            dsProps.put("password", getPropertyString("jdbcPassword"));
            ds = BasicDataSourceFactory.createDataSource(dsProps);
        }
        return ds;
    }

    protected boolean isEmptyValues(String[] values) {
        boolean result = false;
        if (values == null || values.length == 0) {
            result = true;
        } else {
            for (String val : values) {
                if (val == null || val.trim().length() == 0) {
                    result = true;
                    break;
                }
            }
        }
        return result;
    }

    @Override
    public String getName() {
        return "JDBC Validator";
    }

    @Override
    public String getVersion() {
        return "7.0.0";
    }

    @Override
    public String getDescription() {
        return "JDBC Form Validator";
    }

    @Override
    public String getLabel() {
        return "JDBC Validator";
    }

    @Override
    public String getClassName() {
        return this.getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClass().getName(), "/properties/jdbcValidator.json", null, true, MESSAGE_PATH);
    }

    public void webService(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        //Limit the API for admin usage only
        boolean isAdmin = WorkflowUtil.isCurrentUserInRole(WorkflowUserManager.ROLE_ADMIN);
        if (!isAdmin) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String action = request.getParameter("action");
        if ("testConnection".equals(action)) {
            String message = "";
            Connection conn = null;

            AppDefinition appDef = AppUtil.getCurrentAppDefinition();

            String jdbcDriver = AppUtil.processHashVariable(request.getParameter("jdbcDriver"), null, null, null, appDef);
            String jdbcUrl = AppUtil.processHashVariable(request.getParameter("jdbcUrl"), null, null, null, appDef);
            String jdbcUser = AppUtil.processHashVariable(request.getParameter("jdbcUser"), null, null, null, appDef);
            String jdbcPassword = AppUtil.processHashVariable(SecurityUtil.decrypt(request.getParameter("jdbcPassword")), null, null, null, appDef);

            Properties dsProps = new Properties();
            dsProps.put("driverClassName", jdbcDriver);
            dsProps.put("url", jdbcUrl);
            dsProps.put("username", jdbcUser);
            dsProps.put("password", jdbcPassword);

            try ( BasicDataSource ds = BasicDataSourceFactory.createDataSource(dsProps)) {

                conn = ds.getConnection();

                message = AppPluginUtil.getMessage("datalist.jdbcDataListBinder.connectionOk", getClassName(), null);
            } catch (Exception e) {
                LogUtil.error(getClassName(), e, "Test Connection error");
                message = AppPluginUtil.getMessage("datalist.jdbcDataListBinder.connectionFail", getClassName(), null) + "\n" + e.getLocalizedMessage();
            } finally {
                try {
                    if (conn != null && !conn.isClosed()) {
                        conn.close();
                    }
                } catch (SQLException e) {
                    LogUtil.error(DynamicDataSourceManager.class.getName(), e, "");
                }
            }
            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.accumulate("message", message);
                jsonObject.write(response.getWriter());
            } catch (IOException | JSONException e) {
                //ignore
            }
        } else {
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
        }
    }

}
