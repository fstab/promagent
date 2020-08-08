package io.promagent.spring;


import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.XmlUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.text.MessageFormat;


@Component
@Slf4j
public class DownloadUtils {

    @Autowired
    private AgentConfig agentConfig;

    private OkHttpClient okHttpClient = new OkHttpClient();

    private String authorization;

    @PostConstruct
    public void init() {
        String auth = agentConfig.getMvmName() + ":" + agentConfig.getMvmPass();
        this.authorization = "Basic " + new String(Base64.encodeBase64(auth.getBytes(Charset.forName("US-ASCII"))));
    }

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
                .addHeader("Authorization", authorization)
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
            log.info("下载文件:"+agentConfig.getRemoteDownloadUrl());
            Request request = new Request.Builder()
                    .url(agentConfig.getRemoteDownloadUrl())
                    .addHeader("Connection", "close")
                    .addHeader("Authorization", authorization)
                    .build();

            InputStream source = okHttpClient.newCall(request).execute().body().byteStream();
            FileUtils.copyInputStreamToFile(source, destination);
            log.info("保存文件:"+destination.getAbsolutePath());
        }
        String agentPath = destination.getAbsolutePath();
        agentConfig.setJarFile(agentPath);
    }

    public void updateJar() throws IOException {

        String mavenMeta = readMavenMeta(agentConfig.getMetadataUrl());
        String version = readDocFirstTag(mavenMeta, "latest");

        String metadataVersionUrl = MessageFormat.format(agentConfig.getMetadataVersionUrl(), version);
        agentConfig.setMetadataVersionUrl(metadataVersionUrl);

        mavenMeta = readMavenMeta(metadataVersionUrl);
        String jarVersion = readDocFirstTag(mavenMeta, "value");

        String remoteDownLoadUrl = MessageFormat.format(agentConfig.getRemoteDownloadUrl(), version, jarVersion);
        agentConfig.setRemoteDownloadUrl(remoteDownLoadUrl);
        downJar();
    }
}
