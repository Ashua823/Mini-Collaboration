package main.java.com.collaboration.domain;

public class Response {
    //状态码
    private int code;
    //提示信息
    private String message;
    //返回的数据
    private Object data;
    //响应时间
    private long timestamp;

    public Response(int code, String message, Object data, long timestamp) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = timestamp;
    }

    public Response() {
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "Response{" +
                "code=" + code +
                ", message='" + message + '\'' +
                ", data=" + data +
                ", timestamp=" + timestamp +
                '}';
    }

    public static Response success(Object data){
        //创建 Response 对象
        Response response = new Response();
        //code 设为 200
        response.setCode(200);
        //message 设为 "success"
        response.setMessage("success");
        //data 设为传入的参数
        response.setData(data);
        //timestamp 设为当前时间
        response.setTimestamp(System.currentTimeMillis());
        return response;
    }

    public static Response error(String message) {
        //创建 Response 对象
        Response response = new Response();
        //code 设为 400
        response.setCode(400);
        //message 设为传入的错误信息
        response.setMessage(message);
        //data 设为 null
        response.setData(null);
        //timestamp 设为当前时间
        response.setTimestamp(System.currentTimeMillis());
        return response;
    }

}
