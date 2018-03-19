package www.frain.com.androidtvproject;
import java.util.HashMap;
import java.util.Map;


import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
/**
 * Created by diy on 2018/3/16.
 */

public class SerieExtTool {

    /*设置网页抓取响应时间*/
    private static final int TIMEOUT = 10000;

    public static Map<String, Object> getSerieExtDetail(String testUrl) throws Exception{
        /*用來封裝要保存的参数*/
        Map<String, Object> map = new HashMap<String, Object>();

        /*取得车系参数配置页面文档*/
       // Document document = Jsoup.connect(url).timeout(TIMEOUT).get();
        Connection connect = Jsoup.connect("https:"+testUrl).timeout(TIMEOUT);
        Map<String, String> header = new HashMap<String, String>();
        header.put(":authority", "ww4.hanjutv.com");
        header.put(":method", "GET");
        header.put(":path", testUrl.substring(testUrl.indexOf("/index.php?")));
        header.put(":scheme", "https");
        header.put("accept", "application/json, text/javascript, */*; q=0.01");
        header.put("accept-encoding", "gzip, deflate, br");
        header.put("accept-language", "zh-CN,zh;q=0.9");
        header.put("cache-control", "no-cache");
        header.put("accept", "ww4.hanjutv.com");
        Connection connection = connect.data(header);
        Document document = connection.get();
        /*取得script下面的JS变量*/
        Elements e = document.getElementsByTag("script").eq(6);

        /*循环遍历script下面的JS变量*/
        for (Element element : e) {

            /*取得JS变量数组*/
            String[] data = element.data().toString().split("var");

            /*取得单个JS变量*/
            for(String variable : data){

                /*过滤variable为空的数据*/
                if(variable.contains("=")){

                    /*取到满足条件的JS变量*/
                    if(variable.contains("option") || variable.contains("config")
                            || variable.contains("color") || variable.contains("innerColor")){

                        String[]  kvp = variable.split("=");

                        /*取得JS变量存入map*/
                        if(!map.containsKey(kvp[0].trim()))
                            map.put(kvp[0].trim(), kvp[1].trim().substring(0, kvp[1].trim().length()-1).toString());
                    }
                }
            }
        }
        return map;
    }

}