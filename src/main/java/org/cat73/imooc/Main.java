package org.cat73.imooc;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.nutz.http.Http;
import org.nutz.http.Response;
import org.nutz.json.Json;
import org.nutz.lang.Streams;
import org.nutz.log.Log;
import org.nutz.log.Logs;

public class Main {
    private static final Pattern MID_PATTERN = Pattern.compile(
            "<a href='/video/(\\d+)' class=\"J-media-item\">\\s+<i class=\"icon-video type\"></i>\\s+(\\d+?-\\d+? .+?)\\s+");
    private static final Pattern VNAME_PATTERN = Pattern.compile("<h2 class=\"l\">(.+?)</h2>");
    private static final Log log = Logs.getLog(Main.class);

    private static File saveFolder;

    private static class MovieInfo {
        String name;
        String id;

        public MovieInfo(String name, String id) {
            this.name = name;
            this.id = id;
        }
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            return;
        }
        String vid = args[0];

        // 获取视频列表
        log.infof("获取视频列表: %s", vid);
        log.info("----------------");

        // 下载网页
        Response response = Http.get("http://www.imooc.com/learn/" + vid);
        String html = response.getContent();

        // 获取课程名
        Matcher matcher = VNAME_PATTERN.matcher(html);
        matcher.find();
        String vname = matcher.group(1);

        // 获取 mids
        List<MovieInfo> movies = new ArrayList<>();
        matcher = MID_PATTERN.matcher(html);
        while (matcher.find()) {
            movies.add(new MovieInfo(matcher.group(2), matcher.group(1)));
        }
        movies.remove(0);

        // 遍历视频列表并下载
        log.infof("开始下载课程视频: %s", vname);
        log.info("----------------");

        saveFolder = new File("immoc-download/" + vid + "." + vname);
        saveFolder.mkdirs();
        movies.forEach(Main::downloadMovie);

        log.info("下载完成.");
    }

    @SuppressWarnings("unchecked")
    private static void downloadMovie(MovieInfo movieInfo) {
        try {
            log.infof("解析视频地址: %s", movieInfo.name);

            Response response = Http
                    .get("http://www.imooc.com/course/ajaxmediainfo/?mid=" + movieInfo.id);
            Map<?, ?> obj = (Map<?, ?>) Json.fromJson(response.getContent());
            obj = (Map<?, ?>) obj.get("data");
            obj = (Map<?, ?>) obj.get("result");
            List<String> mpaths = (List<String>) obj.get("mpath");
            String mpath = mpaths.get(mpaths.size() - 1);
            // TODO 正则匹配后缀名，不要写死 mp4
            File file = new File(saveFolder, movieInfo.name + ".mp4");
            file.delete();

            log.infof("开始下载: %s", movieInfo.name);
            log.infof("视频地址: %s", mpath);
            log.infof("保存路径: %s", file.getAbsolutePath());

            response = Http.get(mpath);

            Streams.writeAndClose(new FileOutputStream(file), response.getStream());

            log.infof("下载视频 %s 完成.", movieInfo.name);
            log.info("----------------");
        } catch (Exception e) {
            log.errorf("下载视频 %s 失败", movieInfo.name);
        }
    }
}
