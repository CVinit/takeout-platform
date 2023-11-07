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
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;


@Component
@Slf4j
public class MapUtil {
    @Autowired
    private MapProperties mapProperties;

    /*public void getSn() throws Exception {


        Map params = new LinkedHashMap<String, String>();
        params.put("address", "北京市海淀区上地十街10号");
        params.put("output", "json");
        params.put("ak", ak);
        params.put("callback", "showLocation");

        params.put("sn", caculateSn());

        requestGetSN(apiUrl, params);
    }*/

    /**
     * 选择了ak，使用SN校验：
     * 根据您选择的AK已为您生成调用代码
     * 检测您当前的AK设置了sn检验，本示例中已为您生成sn计算代码
     * @param
     * @param
     * @throws Exception
     */
    /*public void requestGetSN(String strUrl, Map<String, String> param) throws Exception {
        if (strUrl == null || strUrl.length() <= 0 || param == null || param.size() <= 0) {
            return;
        }

        StringBuffer queryString = new StringBuffer();
        queryString.append(strUrl);
        for (Map.Entry<?, ?> pair : param.entrySet()) {
            queryString.append(pair.getKey() + "=");
            //    第一种方式使用的 jdk 自带的转码方式  第二种方式使用的 spring 的转码方法 两种均可
            //    queryString.append(URLEncoder.encode((String) pair.getValue(), "UTF-8").replace("+", "%20") + "&");
            queryString.append(UriUtils.encode((String) pair.getValue(), "UTF-8") + "&");
        }

        if (queryString.length() > 0) {
            queryString.deleteCharAt(queryString.length() - 1);
        }

        java.net.URL url = new URL(queryString.toString());
        System.out.println(queryString.toString());
        URLConnection httpConnection = (HttpURLConnection) url.openConnection();
        httpConnection.connect();

        InputStreamReader isr = new InputStreamReader(httpConnection.getInputStream());
        BufferedReader reader = new BufferedReader(isr);
        StringBuffer buffer = new StringBuffer();
        String line;
        while ((line = reader.readLine()) != null) {
            buffer.append(line);
        }
        reader.close();
        isr.close();
        System.out.println("SN: " + buffer.toString());
    }*/

    public String getLocation(String address) {
        Map<String, String> paramsMap = new LinkedHashMap<>();
        paramsMap.put("address", address);
        paramsMap.put("output", "json");
        paramsMap.put("ak", mapProperties.getAk());
//        paramsMap.put("callback", "showLocation");

        String uri = null;
        try {
            uri = getUri(paramsMap, mapProperties.getLocateApiUri());
        } catch (Exception e) {
            throw new MapException("地址信息处理错误" + e);
        }

//        uri = URLEncoder.encode(uri, "UTF-8");

        if (mapProperties.getApiDomain() == null || mapProperties.getApiDomain().length() <= 0 ) {
            throw new MapException("apiDomain错误！");
        }

        String url = mapProperties.getApiDomain() + uri;
//        url = URLEncoder.encode(url,"UTF-8");

        String response = HttpClientUtil.doGet(url, null);

        JSONObject jsonObject = JSONObject.parseObject(response);
        jsonObject = jsonObject.getJSONObject("result").getJSONObject("location");

        String res = jsonObject.getString("lat") + "," + jsonObject.getString("lng");

        return res;

    }

    public Long getDistance(String origin,String destination){
        try {

            Map<String, String> paramsMap = new LinkedHashMap<>();
            paramsMap.put("origin", origin);
            paramsMap.put("destination", destination);
            paramsMap.put("riding_type","1"); //电动车骑行
            paramsMap.put("ak", mapProperties.getAk());
            String currentTimestamp =  String.valueOf(System.currentTimeMillis());
            paramsMap.put("timestamp", currentTimestamp);

            String uri = getUri(paramsMap, mapProperties.getDirectApiUri());

            String url = mapProperties.getApiDomain() + uri;
//            url = URLEncoder.encode(url,"UTF-8");

            String response = HttpClientUtil.doGet(url, null);

            JSONObject jsonObject = JSONObject.parseObject(response);
            JSONArray jsonArray = jsonObject.getJSONObject("result").getJSONArray("routes");
            if (jsonArray == null ){
                throw new MapException("无配送路线");
            }
            Long res = 0L;
            for (int i = 0;i < jsonArray.size();i++){
                JSONObject json = jsonArray.getJSONObject(i);
                Long distance = json.getLong("distance");
                res = res > distance ? res : distance;
            }
            return res;


        } catch (Exception e) {
            throw new MapException("获取位置信息错误：" + e.toString());
        }
    }

    private String getUri(Map<String, String> paramsMap, String apiUri) throws UnsupportedEncodingException,
            NoSuchAlgorithmException {

        // 计算sn跟参数对出现顺序有关，get请求请使用LinkedHashMap保存<key,value>，该方法根据key的插入顺序排序；post请使用TreeMap保存<key,value>，该方法会自动将key按照字母a-z顺序排序。
        // 所以get请求可自定义参数顺序（sn参数必须在最后）发送请求，但是post请求必须按照字母a-z顺序填充body（sn参数必须在最后）。
        // 以get请求为例：http://api.map.baidu.com/geocoder/v2/?address=百度大厦&output=json&ak=yourak，paramsMap中先放入address，再放output，然后放ak，放入顺序必须跟get请求中对应参数的出现顺序保持一致。
//        Map paramsMap = new LinkedHashMap<String, String>();
//        paramsMap.put("address", "北京市海淀区上地十街10号");
//        paramsMap.put("output", "json");
//        paramsMap.put("ak", ak);
//        paramsMap.put("callback", "showLocation");


        // 调用下面的toQueryString方法，对LinkedHashMap内所有value作utf8编码，拼接返回结果address=%E7%99%BE%E5%BA%A6%E5%A4%A7%E5%8E%A6&output=json&ak=yourak
        String paramsStr = toQueryString(paramsMap);

        if (apiUri == null || apiUri.length() <= 0){
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
