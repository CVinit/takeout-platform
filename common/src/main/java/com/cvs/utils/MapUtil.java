package com.cvs.utils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.cvs.exception.MapException;
import com.cvs.properties.MapProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;


@Component
@Slf4j
public class MapUtil {
    @Autowired
    private MapProperties mapProperties;

    public String getLocation(String address) {
        Map<String, String> paramsMap = new LinkedHashMap<>();
        paramsMap.put("address", address);
        paramsMap.put("output", "json");
        paramsMap.put("ak", mapProperties.getAk());

        String uri = "";
        try {
            uri = getUri(paramsMap, mapProperties.getLocateApiUri());
        } catch (UnsupportedEncodingException e) {
            log.error("地址信息编码处理错误");
            throw new MapException("地址信息编码处理错误，请联系管理员");
        }

        String url = mapProperties.getApiDomain() + uri;

        String response = HttpClientUtil.doGet(url, null);

        JSONObject jsonObject = JSONObject.parseObject(response);

        Integer status = jsonObject.getInteger("status");
        if (!status.equals(0)) {
            if (status.equals(1)) {
                log.error("api服务器内部错误");
            } else if (status.equals(2)) {
                log.error("请求参数非法");
            } else if (status.equals(3)) {
                log.error("权限校验失败");
            } else if (status.equals(4)) {
                log.error("配额校验失败，请检查当日配额是否充足");
            } else if (status.equals(5)) {
                log.error("ak不存在或者非法，未传入ak参数或ak已被删除");
            } else if (status.equals(101)) {
                log.error("AK参数不存在");
            } else if (status.equals(102)) {
                log.error("不通过白名单或者安全码不对");
            } else if (status.equals(240)) {
                log.error("APP 服务被禁用，请进入API控制台为AK勾选对应服务");
            }
            throw new MapException("api响应异常");
        }

        jsonObject = jsonObject.getJSONObject("result").getJSONObject("location");

        String res = jsonObject.getString("lat") + "," + jsonObject.getString("lng");

        return res;

    }

    public Long getDistance(String origin, String destination) {

        Map<String, String> paramsMap = new LinkedHashMap<>();
        paramsMap.put("origin", origin);
        paramsMap.put("destination", destination);
        paramsMap.put("riding_type", "1"); //电动车骑行
        paramsMap.put("ak", mapProperties.getAk());
        String currentTimestamp = String.valueOf(System.currentTimeMillis());
        paramsMap.put("timestamp", currentTimestamp);

        String uri = null;
        try {
            uri = getUri(paramsMap, mapProperties.getDirectApiUri());
        } catch (UnsupportedEncodingException e) {
            log.error("地址信息编码处理错误");
            throw new MapException("地址信息编码处理错误，请联系管理员");
        }

        String url = mapProperties.getApiDomain() + uri;
//            url = URLEncoder.encode(url,"UTF-8");

        String response = HttpClientUtil.doGet(url, null);

        JSONObject jsonObject = JSONObject.parseObject(response);
        Integer status = jsonObject.getInteger("status");
        if (!status.equals(0)) {
            if (status.equals(1)) {
                log.error("api服务器内部错误");
            } else if (status.equals(2)) {
                log.error("请求参数非法");
            } else if (status.equals(7)) {
                log.error("无返回结果");
            }else if (status.equals(2001)) {
                log.error("无骑行路线");
            }
            throw new MapException("api响应异常");
        }
        JSONArray jsonArray = jsonObject.getJSONObject("result").getJSONArray("routes");
        if (jsonArray == null || jsonArray.size() <= 0) {
            throw new MapException("无可配送路线，无法为您配送订单");
        }
        Long res = 0L;
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject json = jsonArray.getJSONObject(i);
            Long distance = json.getLong("distance");
            res = res > distance ? res : distance;
        }
        return res;

    }

    private String getUri(Map<String, String> paramsMap, String apiUri) throws UnsupportedEncodingException {

        // 计算sn跟参数对出现顺序有关，get请求请使用LinkedHashMap保存<key,value>，该方法根据key的插入顺序排序；post请使用TreeMap保存<key,value>，该方法会自动将key按照字母a-z顺序排序。
        // 所以get请求可自定义参数顺序（sn参数必须在最后）发送请求，但是post请求必须按照字母a-z顺序填充body（sn参数必须在最后）。
        // 以get请求为例：http://api.map.baidu.com/geocoder/v2/?address=百度大厦&output=json&ak=yourak，paramsMap中先放入address，再放output，然后放ak，放入顺序必须跟get请求中对应参数的出现顺序保持一致。

        // 调用下面的toQueryString方法，对LinkedHashMap内所有value作utf8编码，拼接返回结果address=%E7%99%BE%E5%BA%A6%E5%A4%A7%E5%8E%A6&output=json&ak=yourak
        String paramsStr = toQueryString(paramsMap);

        if (apiUri == null || apiUri.length() <= 0) {
            throw new MapException("apiUri错误！");
        }
        // 对paramsStr前面拼接上/geocoder/v3/?，后面直接拼接yoursk得到/geocoder/v3/?address=%E7%99%BE%E5%BA%A6%E5%A4%A7%E5%8E%A6&output=json&ak=yourakyoursk
        String wholeStr = new String(apiUri + paramsStr + mapProperties.getSk());

//        System.out.println(wholeStr);
        // 对上面wholeStr再作utf8编码
        String tempStr = URLEncoder.encode(wholeStr, "UTF-8");

        // 调用下面的MD5方法得到最后的sn签名
        String sn = MD5(tempStr);
//        System.out.println(sn);

        String uriWithSN = apiUri + paramsStr + "&sn=" + sn;
        return uriWithSN;
    }

    // 对Map内所有value作utf8编码，拼接返回结果
    private String toQueryString(Map<?, ?> data)
            throws UnsupportedEncodingException {
        StringBuffer queryString = new StringBuffer();
        for (Map.Entry<?, ?> pair : data.entrySet()) {
            queryString.append(pair.getKey() + "=");
            //    第一种方式使用的 jdk 自带的转码方式  第二种方式使用的 spring 的转码方法 两种均可
            queryString.append(URLEncoder.encode((String) pair.getValue(), "UTF-8").replace("+", "%20") + "&");
//            queryString.append(UriUtils.encode((String) pair.getValue(), "UTF-8") + "&");
        }
        if (queryString.length() > 0) {
            queryString.deleteCharAt(queryString.length() - 1);
        }
        return queryString.toString();
    }

    // 来自stackoverflow的MD5计算方法，调用了MessageDigest库函数，并把byte数组结果转换成16进制
    private String MD5(String md5) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest
                    .getInstance("MD5");
            byte[] array = md.digest(md5.getBytes());
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < array.length; ++i) {
                sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100)
                        .substring(1, 3));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
        }
        return null;
    }


}
