package io.promagent.log.spring;


import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.XmlUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.concurrent.TimeUnit;


@Component
@Slf4j
public class DownloadUtils {

    @Autowired
    private AgentConfig agentConfig;

    private OkHttpClient okHttpClient = new OkHttpClient();

    public String readDocFirstTag(String mavenMetaData, String TagName) {
        return XmlUtil.parseXml(mavenMetaData)
                .getDocumentElement()
                .getElementsByTagName(TagName)
                .item(0)
                .getTextContent();
    }

    public String readMavenMeta(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Basic "+agentConfig.getToken())
                .build();

        return okHttpClient
                .newCall(request)
                .execute()
                .body()
                .string();
    }

    public void downJar() throws IOException {
        File destination = new File(agentConfig.getAgentDir(), FileUtil.getName(agentConfig.getRemoteDownloadUrl()));
        if (!destination.exists()) {
            log.info("下载文件:" + agentConfig.getRemoteDownloadUrl());
            long startTime = System.currentTimeMillis();
            Request request = new Request.Builder()
                    .url(agentConfig.getRemoteDownloadUrl())
                    .addHeader("Connection", "close")
                    .addHeader("Authorization", "Basic "+agentConfig.getToken())
                    .build();

            InputStream source = okHttpClient.newCall(request).execute().body().byteStream();
            FileUtils.copyInputStreamToFile(source, destination);
            String downloadTime = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTime) + " Sec";
            agentConfig.setDownloadTime(downloadTime);
        } else {
            agentConfig.setDownloadTime("cache");
        }
        String agentPath = destination.getAbsolutePath();
        agentConfig.setJarFile(agentPath);
    }

    public void updateJar() throws IOException {

        String mavenMeta = readMavenMeta(agentConfig.getMetadataUrl());
        String version = readDocFirstTag(mavenMeta, agentConfig.getMetadataTag());
        String jarVersion = version;
        if (StringUtils.isEmpty(agentConfig.getReleaseUrl())) {
            String metadataVersionUrl = MessageFormat.format(agentConfig.getMetadataVersionUrl(), version);
            agentConfig.setMetadataVersionUrl(metadataVersionUrl);
            mavenMeta = readMavenMeta(metadataVersionUrl);
            jarVersion = readDocFirstTag(mavenMeta, agentConfig.getMetadataVersionTag());
        }
        String remoteDownLoadUrl = MessageFormat.format(agentConfig.getRemoteDownloadUrl(), version, jarVersion);
        agentConfig.setRemoteDownloadUrl(remoteDownLoadUrl);
        downJar();
    }
}
