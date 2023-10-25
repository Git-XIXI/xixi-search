package com;

import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.model.Picture;
import com.model.entity.Post;
import com.service.PostService;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SpringBootTest
public class CrawlerTest {

    @Resource
    PostService postService;
    @Test
    void testFetchPassage() {

        String json = "{\"current\": 1, \"pageSize\": 8, \"sortField\":\"createTime\", \"sortOrder\": \"descend\", \"category\": \"文章\",\"reviewStatus\": 1}";
        String url = "https://www.code-nav.cn/api/post/search/page/vo";
        String result = HttpRequest
                .post(url)
                .body(json)
                .execute()
                .body();
        //System.out.println(result);

        Map<String, Object> map = JSONUtil.toBean(result, Map.class);
        JSONObject data =  (JSONObject)map.get("data");
        JSONArray records = (JSONArray) data.get("records");
        List<Post> postList = new ArrayList<>();
        for (Object record : records) {
            JSONObject  tempRecord = (JSONObject) record;
            Post post = new Post();
            post.setTitle(tempRecord.getStr("title"));
            post.setContent(tempRecord.getStr("content"));
            JSONArray tags = (JSONArray) tempRecord.get("tags");
            List<String> tagList = tags.toList(String.class);
            post.setTags(JSONUtil.toJsonStr(tagList));
            post.setUserId(1L);
            postList.add(post);
        }
        //System.out.println(postList);
        //数据入库
        boolean b = postService.saveBatch(postList);
        Assertions.assertTrue(b);
    }


    @Test
    void testFetchPicture() throws IOException {
        int current = 1;
        String url = String.format("https://cn.bing.com/images/search?q=头像&form=HDRSC2&first=%s",current);
        Document doc = Jsoup.connect(url).get();
        Elements elements = doc.select(".iuscp.isv");

        List<Picture> pictureList = new ArrayList<>();
        for (Element element : elements) {
            //取图片地址
            String  m = element.select(".iusc").get(0).attr("m");

            Map<String,String> map = JSONUtil.toBean(m, Map.class);
            String murl = map.get("murl");
            System.out.println(murl);
            //取标题
            String title = element.select(".inflnk").get(0).attr("aria-label");
            System.out.println(title);

            Picture picture = new Picture();
            picture.setUrl(murl);
            picture.setTitle(title);
            pictureList.add(picture);
        }

        
//        Elements newsHeadlines = doc.select("#mp-itn b a");
//        for (Element headline : newsHeadlines) {
//
//        }
    }
}