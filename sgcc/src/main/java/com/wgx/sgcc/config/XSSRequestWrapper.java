package com.gjdw.stserver.config;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringEscapeUtils;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Pattern;

public class XSSRequestWrapper extends HttpServletRequestWrapper {

    public XSSRequestWrapper(HttpServletRequest request) {
        super(request);
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        Map<String, String[]> request_map = super.getParameterMap();
        Iterator iterator = request_map.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry me = (Map.Entry) iterator.next();
            // System.out.println(me.getKey()+":");
            String[] values = (String[]) me.getValue();
            for (int i = 0; i < values.length; i++) {
                values[i] = cleanXSS(values[i]);
            }
        }
        return request_map;
    }


    @Override
    public String[] getParameterValues(String parameter) {
        String[] values = super.getParameterValues(parameter);
        if (values == null) {
            return null;
        }
        int count = values.length;
        String[] encodedValues = new String[count];
        for (int i = 0; i < count; i++) {
            encodedValues[i] = cleanXSS(values[i]);
        }
        return encodedValues;
    }

    @Override
    public String getParameter(String parameter) {
        String value = super.getParameter(parameter);
        if (value != null) {
            return cleanXSS(value);
        }
        return null;
    }

    @Override
    public String getHeader(String name) {
        String value = super.getHeader(name);
        if (value == null)
            return null;
        return cleanXSS(value);
    }

    private static String cleanXSS(String value) {
//        value = value.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
//        value = value.replaceAll("%3C", "&lt;").replaceAll("%3E", "&gt;");
//        value = value.replaceAll("\\(", "&#40;").replaceAll("\\)", "&#41;");
//        value = value.replaceAll("%28", "&#40;").replaceAll("%29", "&#41;");
//        value = value.replaceAll("'", "&#x27;").replaceAll("`","&#x60;");
//        value = value.replaceAll("&", "&amp;").replaceAll("</", "&#x2F;");
//        value = value.replaceAll("eval\\((.*)\\)", "");
//        value = value.replaceAll("[\\\"\\\'][\\s]*javascript:(.*)[\\\"\\\']", "&#40;");
//        value = value.replaceAll("script", "&#40;");
//        value = value.replaceAll("(\\[window|location)*", "&0x02;");
//        value = value.replaceAll("(window\\.location|window\\.|\\.location|document\\.cookie|document\\.|alert\\(.*?\\)|window\\.open\\()*", "&0x02;");
        String result = StringEscapeUtils.escapeHtml4(value.toString());
        result = result.replace("&quot;", "\"");
        return result;
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        final ByteArrayInputStream bais = new ByteArrayInputStream(inputHandlers(super.getInputStream()).getBytes());

        return new ServletInputStream() {

            @Override
            public int read() throws IOException {
                return bais.read();
            }

            @Override
            public boolean isFinished() {
                return false;
            }

            @Override
            public boolean isReady() {
                return false;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
            }
        };
    }

    public String inputHandlers(ServletInputStream servletInputStream) {
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(servletInputStream, Charset.forName("UTF-8")));
            String line = "";
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return CheckHtml(sb.toString());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (servletInputStream != null) {
                try {
                    servletInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    private static String CheckHtml(String str) {
        JSONObject jsonObject = JSONObject.parseObject(str);
        Map<Object, Object> param = jsonObject.toJavaObject(Map.class);
        for (Map.Entry<Object, Object> entry : param.entrySet()) {
            Object value1 = entry.getValue();
            String result = StringEscapeUtils.escapeHtml4(value1.toString());
            result = result.replace("&quot;", "\"");
            entry.setValue(result);

//            if (value1.getClass().equals(String.class)) {
//                entry.setValue(StringEscapeUtils.escapeHtml3(value1.toString()));
//            } else if (value1.getClass().equals(List.class)) {
//                List<String> value1_list = (List) value1;
//                for (String item : value1_list) {
//
//                }
//            }
//            if (value1.getClass().equals(JSONObject.class)) {
//
//            } else {
//
//            }
        }
        return param.toString();
    }

}
