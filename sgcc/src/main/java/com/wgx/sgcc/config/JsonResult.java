package com.wgx.sgcc.config;


import java.io.UnsupportedEncodingException;

public  class JsonResult {

    private  int error_code = 0;

    private  String error_msg ="success";

    private   Object data;

    public static JsonResult error(int i, String str, Object o) {
        JsonResult jsonResult =new JsonResult();
        jsonResult.setError_code(i);
        jsonResult.setError_msg(str);
        jsonResult.setData(o);
        return jsonResult;
    }


    public  int getError_code() {
        return error_code;
    }

    public  void setError_code(int error_code) {
        this.error_code = error_code;
    }

    public  String getError_msg() {
        try {
            error_msg=new String(error_msg.getBytes(),"UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return error_msg;
    }

    public  void setError_msg(String error_msg) {
        this.error_msg = error_msg;
    }

    public  Object getData() {
        return data;
    }

    public  void setData(Object data) {
        this.data = data;
    }

    public static JsonResult returnSuccess(String msg,Object data){
        JsonResult jsonResult = new JsonResult();
        jsonResult.setError_msg(msg);
        jsonResult.setData(data);
        return jsonResult;
    }

}
